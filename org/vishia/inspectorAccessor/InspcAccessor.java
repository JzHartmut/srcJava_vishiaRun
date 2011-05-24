package org.vishia.inspectorAccessor;

import org.vishia.byteData.ByteDataAccess;
import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.Address_InterProcessComm_Socket;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessCommFactory;
import org.vishia.communication.InterProcessCommFactoryAccessor;
import org.vishia.inspector.*;

public class InspcAccessor
{
	
	private InspcAccessGenerateOrder orderGenerator = new InspcAccessGenerateOrder();
	
	private final byte[] txBuffer = new byte[1400];

	private final InspcAccessCheckerRxTelg checkerRxTelg = new InspcAccessCheckerRxTelg();
	
	/**If true, then a TelgHead is prepared already and some more info can be taken into the telegram.
	 * If false then the txBuffer is unused yet.
	 */
	private boolean bFillTelg = false;
	
	long timeSend, dtimeReceive, dtimeWeakup;
	
	/**True if a second datagram is prepared and sent yet.
	 * 
	 */
	private boolean bIsSentTelg = false;
	
	private final InspcDataExchangeAccess.Datagram txAccess = new InspcDataExchangeAccess.Datagram();
	
	/**The entrant is the sub-consumer of a telegram on the device with given IP.
	 * A negative number is need because compatibility with oder telegram structures.
	 * The correct number depends on the device. It is a user parameter of connection.
	 */
	int nEntrant = -1;
	
	String sOwnIpAddr;
	
	String sTargetIpAddr;
	
	Address_InterProcessComm targetAddr;
	
	int nSeqNumber = 0; 
	
	int nEncryption = 0;
	
	
	
	private InterProcessComm ipc;
	
	
	public InspcAccessor()
	{
		
		txAccess.assignEmpty(txBuffer);
    //The factory should be loaded already. Then the instance is able to get. Loaded before!
	}

	
	public boolean open(String sOwnIpAddr)
	{
	  this.sOwnIpAddr = sOwnIpAddr;
    InterProcessCommFactory ipcFactory = InterProcessCommFactoryAccessor.getInstance();
    ipc = ipcFactory.create (sOwnIpAddr);
    ipc.open(null, true);
    int ipcOk = ipc.checkConnection();
    if(ipcOk == 0){
      receiveThread.start();   //start it after ipc is ok.
    }
	  return ipcOk == 0;
	}
	
	
	/**Sets the target address for the next telegram assembling and the next send()-invocation.
	 * If a telegram is pending, it will be sent. A new telegram is prepared.
	 * @param sTargetIpAddr
	 */
	public void setTargetAddr(String sTargetIpAddr)
	{ //TODO check and send, prepare new head.
	  this.sTargetIpAddr = sTargetIpAddr;
	  this.targetAddr = new Address_InterProcessComm_Socket(sTargetIpAddr);
	}
	
	/**Checks whether the head of the datagram should be created and filled. Does that if necessary.
	 * Elsewhere if the datagram hasn't place for the new info, it will be sent and a new head
	 * will be created. 
	 * @param zBytesInfo
	 */
	private void checkSendAndFillHead(int zBytesInfo)
	{ if(!bFillTelg){
	    txAccess.assignEmpty(txBuffer);
	    txAccess.setHead(nEntrant, ++nSeqNumber, nEncryption);
	    bFillTelg = true;
	  } else {
	    int lengthDatagram = txAccess.getLength();
	    if(lengthDatagram + zBytesInfo > txBuffer.length){
	      send();
	      assert(!bFillTelg);
	      txAccess.setHead(nEntrant, nSeqNumber, nEncryption);
	      bFillTelg = true;
	    }
	  }
	}
	
	/**Returns true if enough information blocks are given, so that a telegram was sent already.
	 * A second telegram may be started to prepare with the last call. 
	 * But it should be waited for the answer firstly.  
	 * @return true if a telegram is sent but the answer isn't received yet.
	 */
	public boolean checkIsSent()
	{
	  return bIsSentTelg;
	}
	
	
	
	/**Adds the info block to send 'get value by path'
	 * @param sPathInTarget
	 * @return The order number. 0 if the cmd can't be created.
	 */
	public int cmdGetValueByPath(String sPathInTarget)
	{ int order;
	  checkSendAndFillHead(sPathInTarget.length() + 8 + 4);
	  int sizeTelgLast = txAccess.getLengthTotal();
	  if(sizeTelgLast + Datagrams.CmdGetValue.sizeofHead + sPathInTarget.length() + 3 < txBuffer.length){
      Datagrams.CmdGetValue infoGetValue = new Datagrams.CmdGetValue();
      txAccess.addChild(infoGetValue);
      order = orderGenerator.getNewOrder();
      infoGetValue.set(sPathInTarget, order);
	  } else {
	    //too much info blocks
	    order = 0;
	  }
    return order;
	}
	
	
	
	public void send()
	{
    int lengthDatagram = txAccess.getLength();
    txAccess.setLengthDatagram(lengthDatagram);
    //send the telegram:
    checkerRxTelg.setAwait(nSeqNumber);
    timeSend = System.currentTimeMillis();
    ipc.send(txBuffer, lengthDatagram, targetAddr);
	  bFillTelg = false;
	  bIsSentTelg = true;
	}
	
	
	
	/**Sets a executer instance for the answer telegrams from the target.
	 * The method {@link InspcAccessExecAnswerTelg_ifc#execInspcRxTelg(org.vishia.communication.InspcDataExchangeAccess.Datagram[], int)}
	 * will be invoked if all answer telegrams are received.
	 * If this method is set, the answer execution of the application is done in the receivers thread.
	 * In that case the method {@link #awaitAnswer(int)} must not use.
	 * @param executerAnswer
	 */
	public  void setExecuterAnswer(InspcAccessExecAnswerTelg_ifc executerAnswer)
  { checkerRxTelg.setExecuterAnswer(executerAnswer);
  }
  

	
	
	/**Waits for answer from the target.
	 * This method can be called in any users thread. Typically it is the thread, which has send the telegram
	 * to the target. The alternate method is {@link #setExecuterAnswer(InspcAccessExecAnswerTelg_ifc)}.
	 * 
	 * @param timeout for waiting.
	 * @return null on timeout, the answer datagrams elsewhere.
	 */
	public InspcDataExchangeAccess.Datagram[]  awaitAnswer(int timeout)
	{ InspcDataExchangeAccess.Datagram[] answerTelgs = checkerRxTelg.waitForAnswer(timeout); 
  	long time = System.currentTimeMillis();
    dtimeWeakup = time - timeSend;
    return answerTelgs;
  }
	
	
	
	
  
  Runnable receiveRun = new Runnable()
  { @Override public void run()
    { receiveFromTarget();
    }
  };
  
  /**A receive thread should be used anyway if a socket receiving or other receiving is given.
   * Because: Anytime any telegram can be received, the receiver buffer should be cleared,
   * also if the telegram is unexpected.
   */
  Thread receiveThread = new Thread(receiveRun, "inspcRxThread");
  
  
  
  void receiveFromTarget()
  {
    int[] result = new int[1];
    while(true){
      byte[] rxBuffer = ipc.receive(result, targetAddr);
      if(result[0]>0){
        long time = System.currentTimeMillis();
        dtimeReceive = time - timeSend;
        checkerRxTelg.applyReceivedTelg(rxBuffer, result[0]);
      } else {
        System.out.append("receive error");
      }
    }//while
  }
  
  
  
	
	void stop(){}
}
