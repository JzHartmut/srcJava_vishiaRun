package org.vishia.inspectorAccessor;

import org.vishia.byteData.ByteDataAccess;
import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.Address_InterProcessComm_Socket;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessCommFactory;
import org.vishia.communication.InterProcessCommFactoryAccessor;
import org.vishia.inspector.*;

public class InspectorAccessor
{
	
	private GenerateOrder orderGenerator = new GenerateOrder();
	
	private final byte[] txBuffer = new byte[1400];

	private final CheckerRxTelg checkerRxTelg = new CheckerRxTelg();
	
	/**If true, then a TelgHead is prepared already and some more info can be taken into the telegram.
	 * If false then the txBuffer is unused yet.
	 */
	private boolean bFillTelg = false;
	
	
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
	
	String sOwnIpAddr = "UDP:127.0.0.1:60099";
	
	String sTargetIpAddr = "UDP:127.0.0.1:60080";
	
	Address_InterProcessComm targetAddr = new Address_InterProcessComm_Socket(sTargetIpAddr);
	
	int nSeqNumber = 0; 
	
	int nEncryption = 0;
	
	
	
	final InterProcessComm ipc;
	
	
	InspectorAccessor()
	{
		
		txAccess.assignEmpty(txBuffer);
    //The factory should be loaded already. Then the instance is able to get. Loaded before!
		InterProcessCommFactory ipcFactory = InterProcessCommFactoryAccessor.getInstance();
		ipc = ipcFactory.create (sOwnIpAddr);
		ipc.open(null, true);
		int ipcOk = ipc.checkConnection();
		if(ipcOk == 0){
		  receiveThread.start();   //start it after ipc is ok.
		}
	}

	
	
	/**Checks whether the head of the datagram should be created and filled. Does that if necessary.
	 * Elsewhere if the datagram hasn't place for the new info, it will be sent and a new head
	 * will be created. 
	 * @param zBytesInfo
	 */
	private void checkSendAndFillHead(int zBytesInfo)
	{ if(!bFillTelg){
	    txAccess.setHead(nEntrant, nSeqNumber, nEncryption);
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
	boolean checkIsSent()
	{
	  return bIsSentTelg;
	}
	
	
	
	/**Adds the info block to send 'get value by path'
	 * @param sPathInTarget
	 * @return The order number.
	 */
	int cmdGetValueByPath(String sPathInTarget)
	{
	  checkSendAndFillHead(sPathInTarget.length() + 8 + 4);
    Datagrams.CmdGetValue infoGetValue = new Datagrams.CmdGetValue();
    txAccess.addChild(infoGetValue);
    int order = orderGenerator.getNewOrder();
    infoGetValue.set(sPathInTarget, order);
    return order;
	}
	
	
	
	void send()
	{
    int lengthDatagram = txAccess.getLength();
    txAccess.setLengthDatagram(lengthDatagram);
    //send the telegram:
    checkerRxTelg.setAwait(nSeqNumber);
    ipc.send(txBuffer, lengthDatagram, targetAddr);
	  bFillTelg = false;
	  bIsSentTelg = true;
	}
	
	
	
	InspcDataExchangeAccess.Datagram  awaitAnswer(int timeout){ return checkerRxTelg.waitForAnswer(timeout); }
	
	
	
	
  
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
        checkerRxTelg.applyReceivedTelg(rxBuffer, result[0]);
      } else {
        System.out.append("receive error");
      }
    }//while
  }
  
  
  
	
	void stop(){}
}
