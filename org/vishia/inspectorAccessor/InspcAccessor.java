package org.vishia.inspectorAccessor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.vishia.byteData.ByteDataAccess;
import org.vishia.byteData.ByteDataAccessSimple;
import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.Address_InterProcessComm_Socket;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessCommFactory;
import org.vishia.communication.InterProcessCommFactoryAccessor;
import org.vishia.communication.InspcDataExchangeAccess.Info;
import org.vishia.inspector.InspcTelgInfoSet;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.reflect.ClassJc;

/**An instance of this class accesses any target device via InterProcessCommunication, usual Ethernet-Sockets.
 * This class creates opens the {@link InterProcessComm} and creates a receiving thread. 
 * Any send requests are invoked from the environment. A send request usual contains
 * a reference to a {@link InspcAccessExecRxOrder_ifc}. 
 * Thats {@link InspcAccessExecRxOrder_ifc#execInspcRxOrder(Info, LogMessage, int)} is executed
 * if the response info block is received. With the order-number-concept see {@link InspcDataExchangeAccess.Info#getOrder()} 
 * the request and the response are associated together.
 *  
 * @author Hartmut Schorrig
 *
 */
public class InspcAccessor implements Closeable
{
	
  /**The versionm history and license of this class:
   * <ul>
   * <li>2012-04-08 Hartmut new: Support of GetValueByIdent
   * <li>2012-04-05 Hartmut new: Use {@link LogMessage to test telegram trafic}
   * <li>2012-04-02 Hartmut new: {@link #sendAndPrepareCmdSetValueByPath(String, long, int, InspcAccessExecRxOrder_ifc)}.
   *   The concept is: provide the {@link InspcAccessExecRxOrder_ifc} with the send request.
   *   It should be implement for all requests in this form. But the awaiting of answer doesn't may the best way.
   *   Problem evaluating the answer telg in the rx thread or in the tx thread. TODO.
   * <li>2011-06-19 Hartmut new; {@link #shouldSend()} and {@link #isFilledTxTelg()} able to call outside.
   *     It improves the handling with info blocks in a telegram.
   * <li>2011-05 Hartmut created
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  final static int version = 20120406;

  /**If true then writes a log of all send and received telegrams. */
  LogMessage logTelg;
  
  int identLogTelg;
  
	private InspcAccessGenerateOrder orderGenerator = new InspcAccessGenerateOrder();
	
	private final byte[] txBuffer = new byte[1400];

	private final InspcAccessCheckerRxTelg checkerRxTelg = new InspcAccessCheckerRxTelg();
	
	private final InspcTelgInfoSet infoAccess;
	
  /**Instance to evaluate received telegrams. It is possible that a derived instance is used! */
  public final InspcAccessEvaluatorRxTelg rxEval;
  
	/**If true, then a TelgHead is prepared already and some more info can be taken into the telegram.
	 * If false then the txBuffer is unused yet.
	 */
	private boolean bFillTelg = false;
	
	/**If true then the current prepared tx telegram should be send.
	 * This bit is set, if a info block can't be placed in the current telegram.
	 */
  private boolean bShouldSend = false;
	
	/**True if a second datagram is prepared and sent yet.
   * 
   */
  private boolean bIsSentTelg = false;
  
	long timeSend, dtimeReceive, dtimeWeakup;
	
	private final InspcDataExchangeAccess.Datagram txAccess = new InspcDataExchangeAccess.Datagram();
	
	
	/**Number of idents to get values per ident. It determines the length of an info block,
	 * see {@link #dataInfoDataGetValueByIdent}. An info block has to be no longer than a UDP-telegram.
	 */
	private final static int zIdent4GetValueByIdent = 320;
	
	private int ixIdent5GetValueByIdent;
	
	/**A info element to get values by ident It contains a {@link InspcDataExchangeAccess.Info} head
	 * and then 4-byte-idents for data. The same order of data are given in the array
	 */
	private final byte[] dataInfoDataGetValueByIdent = new byte[InspcDataExchangeAccess.Info.sizeofHead + 4 * zIdent4GetValueByIdent]; 
	
	
	//ByteBuffer acc4ValueByIdent = new ByteBuffer();
	
	/**Managing instance of {@link ByteDataAccess} for {@link #dataInfoDataGetValueByIdent}. */
	private final ByteDataAccess accInfoDataGetValueByIdent = new ByteDataAccessSimple(dataInfoDataGetValueByIdent, true);
	
	private final InspcAccessExecRxOrder_ifc[] actionRx4GetValueByIdent = new InspcAccessExecRxOrder_ifc[zIdent4GetValueByIdent]; 
	//InspcVariable[] variable4GetValueByIdent = new InspcVariable[zIdent4GetValueByIdent];
	
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
	
	
	public InspcAccessor(InspcAccessEvaluatorRxTelg inspcRxEval)
	{
		this.rxEval = inspcRxEval;
		txAccess.assignEmpty(txBuffer);
		this.infoAccess = new InspcTelgInfoSet();
		
    //The factory should be loaded already. Then the instance is able to get. Loaded before!
	}

	
	public boolean open(String sOwnIpAddrP)
	{
	  this.sOwnIpAddr = sOwnIpAddrP;
    InterProcessCommFactory ipcFactory = InterProcessCommFactoryAccessor.getInstance();
    ipc = ipcFactory.create (sOwnIpAddrP);
    int ipcOk = ipc.open(null, true);
    if(ipcOk >=0){  ipcOk = ipc.checkConnection(); }
    if(ipcOk == 0){
    }
    if(ipcOk < 0){
      System.out.println("Problem can't open socket: " + sOwnIpAddrP); 
    } else {
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
	
	
	
	/**Switch on or off the log functionality.
	 * @param logP The log output. null then switch of the log.
	 * @param ident The ident number in log for send, next number for receive.
	 */
	public void setLog(LogMessage log, int ident){
	  this.logTelg = log;
	  this.identLogTelg = ident;
	}
	
	
  /**Checks whether the head of the datagram should be created and filled. Does that if necessary.
   * Elsewhere if the datagram hasn't place for the new info, it will be sent and a new head
   * will be created. 
   * @param zBytesInfo
   */
  private void XXXcheckSendAndFillHead(int zBytesInfo)
  { if(!bFillTelg){
      txAccess.assignEmpty(txBuffer);
      if(++nSeqNumber == 0){ nSeqNumber = 1; }
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
  
  
  /**Checks whether the head of the datagram should be created and the telegram has place for the current data.
   * This routine is called at begin of all cmd...() routines of this class. 
   * <ul>
   * <li>Creates the head if if necessary.
   * <li>Sets {@link #bFillTelg}, able to query with {@link isFilledTxTelg()}.
   * <li>Checks whether the requested bytes are able to fill in the telegram.
   * <li>Sets {@link #bShouldSend} able to query with {@link shouldSend()} if the info doesn't fit in the telegram.
   * </ul>
   * @param zBytesInfo Number of bytes to add to the telegram.
   * @return true if the number of bytes are able to place in the current telegram.
   *         If this routine returns false, the info can't be place in this telegram.
   *         It should be send firstly. The current order should be processed after them.
   */
  private boolean checkAndFillHead(int zBytesInfo)
  { if(!bFillTelg){
      txAccess.assignEmpty(txBuffer);
      if(++nSeqNumber == 0){ nSeqNumber = 1; }
      txAccess.setHead(nEntrant, nSeqNumber, nEncryption);
      bFillTelg = true;
      assert(zBytesInfo + txAccess.getLengthTotal() <= txBuffer.length);  //1 info block should match in size!
      return true;
    } else {
      int lengthDatagram = txAccess.getLengthTotal();
      bShouldSend =  (lengthDatagram + zBytesInfo) >= txBuffer.length;
      return !bShouldSend;
    }
  }
  

  
  /**Checks whether the head of the datagram should be created and the telegram has place for the current data.
   * This routine is called at begin of all cmd...() routines of this class. 
   * <ul>
   * <li>Creates the head if if necessary.
   * <li>Sets {@link #bFillTelg}, able to query with {@link isFilledTxTelg()}.
   * <li>Checks whether the requested bytes are able to fill in the telegram.
   * <li>Sets {@link #bShouldSend} able to query with {@link shouldSend()} if the info doesn't fit in the telegram.
   * </ul>
   * @param zBytesInfo Number of bytes to add to the telegram.
   * @return true if the number of bytes are able to place in the current telegram.
   *         If this routine returns false, the info can't be place in this telegram.
   *         It should be send firstly. The current order should be processed after them.
   */
  private int prepareTelg(int lengthNewInfo)
  { if(bFillTelg && (txAccess.getLengthTotal() + lengthNewInfo) > txBuffer.length){
      //the next requested info does not fit in the current telg. 
      //Therefore send the current telg, wait for answer.
      sendAndAwaitAnswer();
    }
    if(!bFillTelg){
      txAccess.assignEmpty(txBuffer);
      if(++nSeqNumber == 0){ nSeqNumber = 1; }
      txAccess.setHead(nEntrant, nSeqNumber, nEncryption);
      bFillTelg = true;
      return txBuffer.length - txAccess.getLengthHead();
    }
    int lengthDatagram = txAccess.getLengthTotal();
    
    return txBuffer.length - lengthDatagram;
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
    if(checkAndFillHead(InspcTelgInfoSet.sizeofHead + sPathInTarget.length() + 3 )){
      //InspcTelgInfoSet infoGetValue = new InspcTelgInfoSet();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdGetValueByPath(sPathInTarget, order);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg, "send cmdGetValueByPath %s, order = %d", sPathInTarget, order); 
      }
    } else {
      //too much info blocks
      order = 0;
    }
    return order;
  }
  
  
  
  /**Adds the info block to send 'register by path'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created because the telegram is full.
   */
  public int cmdRegisterByPath(String sPathInTarget, InspcAccessExecRxOrder_ifc actionOnRx)
  { final int order;
    final int zPath = sPathInTarget.length();
    final int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    prepareTelg(zPath + restChars);
    order = orderGenerator.getNewOrder();
    rxEval.setExpectedOrder(order, actionOnRx);
    txAccess.addChild(infoAccess);  
    infoAccess.addChildString(sPathInTarget);
    if(restChars >0) { infoAccess.addChildInteger(restChars, 0); }
    final int zInfo = infoAccess.getLength();
    infoAccess.setInfoHead(zInfo, InspcDataExchangeAccess.Info.kRegisterRepeat, order);
    //
    if(logTelg !=null){ 
      logTelg.sendMsg(identLogTelg, "send registerByPath %s, order = %d", sPathInTarget, order); 
    }
    return order;
  }
  
  
  
  /**Adds the info block to send 'register by path'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created because the telegram is full.
   */
  public void cmdGetValueByIdent(int ident, InspcAccessExecRxOrder_ifc actionOnRx)
  { if(ixIdent5GetValueByIdent >= actionRx4GetValueByIdent.length){
      txCmdGetValueByIdent();
    }
    actionRx4GetValueByIdent[ixIdent5GetValueByIdent] = actionOnRx;
    accInfoDataGetValueByIdent.addChildInteger(4, ident);
    ixIdent5GetValueByIdent +=1;
  }
  
  
  /**
   * @return true if the telegram is sent.
   */
  boolean txCmdGetValueByIdent() {
    if(ixIdent5GetValueByIdent > 0){
      int lengthInfo = accInfoDataGetValueByIdent.getLength();
      //It prepares the telg head.
      prepareTelg(lengthInfo);  //It sends an existing telegram if there is not enough space for the idents-info
      int order = orderGenerator.getNewOrder();
      rxEval.setExpectedOrder(order, actionRx4ValueByIdent);
      txAccess.addChild(infoAccess);
      int posInTelg = infoAccess.getPositionInBuffer() + InspcDataExchangeAccess.Info.sizeofHead;
      System.arraycopy(dataInfoDataGetValueByIdent, 0, infoAccess.getData(), posInTelg, 4 * ixIdent5GetValueByIdent);
      infoAccess.setInfoHead(lengthInfo + InspcDataExchangeAccess.Info.sizeofHead, InspcDataExchangeAccess.Info.kGetValueByIndex, order);
      ixIdent5GetValueByIdent = 0;
      accInfoDataGetValueByIdent.assignEmpty(dataInfoDataGetValueByIdent);
      sendAndAwaitAnswer();
      return true;
    } else return false;
  }
  
  private final void execRx4ValueByIdent(Info info, LogMessage log, int identLog){
    //int lenInfo = info.getLength();
    int ixVal = (int)info.getChildInteger(4);
    while(info.sufficingBytesForNextChild(1)){  //at least one byte in info, 
      InspcAccessExecRxOrder_ifc action = actionRx4GetValueByIdent[ixVal];
      ixVal +=1;
      if(action !=null){
        action.execInspcRxOrder(info, log, identLog);
      }
    }
    //System.out.println("execRx4ValueByIdent");
    stop();
  }
  

  
  /**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public int cmdSetValueByPath(String sPathInTarget, long value, int typeofValue)
  { int order;
    int zPath = sPathInTarget.length();
    int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    int zInfo = Info.sizeofHead + InspcDataExchangeAccess.SetValue.sizeofElement + zPath + restChars; 
    if(checkAndFillHead(zInfo )){
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg, "send cmdSetValueByPath %s, order = %d, value=%8X, type=%d", sPathInTarget, order, value, typeofValue); 
      }
      //infoAccess.setCmdSetValueByPath(sPathInTarget, value, typeofValue, order);
      InspcDataExchangeAccess.SetValue accessSetValue = new InspcDataExchangeAccess.SetValue(); 
      infoAccess.addChild(accessSetValue);
      accessSetValue.setLong(value);
      infoAccess.addChildString(sPathInTarget);
      if(restChars >0) { infoAccess.addChildInteger(restChars, 0); }
      assert(infoAccess.getLength() == zInfo);  //check length after add children. 
      infoAccess.setInfoHead(zInfo, InspcDataExchangeAccess.Info.kSetValueByPath, order);
    } else {
      //too much info blocks
      order = 0;
    }
    return order;
  }
  
  
  /**Sends the given telegram if the requested command doesn't fit in the telegram yet,
   * prepares the given command as info in the given or a new one telegram and registers the exec on answer.
   * <br><br>
   * After the last such request {@link #sendAndAwaitAnswer()} have to be called
   * to send at least the last request. 
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @param exec The routine to execute on answer.
   * @return true if a new telegram was created. It is an info only.
   * 
   */
  public boolean sendAndPrepareCmdSetValueByPath(String sPathInTarget, long value, int typeofValue, InspcAccessExecRxOrder_ifc exec)
  { int order;
    boolean sent = false;
    do {
      order = cmdSetValueByPath(sPathInTarget, value, typeofValue);    
      if(order !=0){ //save the order to the action. It is taken on receive.
        this.rxEval.setExpectedOrder(order, exec);
      } else {
        sendAndAwaitAnswer();  //calls execInspcRxOrder as callback.
        sent = true;
      }
    } while(order == 0);  
    return sent;
  }
  
  
/**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public int cmdSetValueByPath(String sPathInTarget, float value)
  { int order;
    if(checkAndFillHead(InspcTelgInfoSet.sizeofHead + 8 + sPathInTarget.length() + 3 )){
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdSetValueByPath(sPathInTarget, value, order);
    } else {
      //too much info blocks
      order = 0;
    }
    return order;
  }
  
  
  /**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public int cmdSetValueByPath(String sPathInTarget, double value)
  { int order;
    if(checkAndFillHead(InspcTelgInfoSet.sizeofHead + 8 + sPathInTarget.length() + 3 )){
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdSetValueByPath(sPathInTarget, value, order);
    } else {
      //too much info blocks
      order = 0;
    }
    return order;
  }
  
  
  /**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created.
   */
  public int cmdGetAddressByPath(String sPathInTarget)
  { int order;
    if(checkAndFillHead(InspcTelgInfoSet.sizeofHead + sPathInTarget.length() + 3 )){
      //InspcTelgInfoSet infoGetValue = new InspcTelgInfoSet();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdGetAddressByPath(sPathInTarget, order);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg, "send cmdGetAddressByPath %s, order = %d", sPathInTarget, order); 
      }
    } else {
      //too much info blocks
      order = 0;
    }
    return order;
  }
  
  
  /**Returns whether a tx telegram is filled with any info blocks.
   * The user can check it. The user needn't store itself whether any call of cmd...() is done.
   * It helps in management info blocks.
   * @return true if any call of cmd...() was done and the send() was not called.
   */
  public boolean isFilledTxTelg(){ return bFillTelg; }
  
  /**Returns true if any cmd..() call doesn't fit in the current telegram, therefore the tx telegram
   * should be send firstly.
   */
  public boolean shouldSend(){ return bShouldSend; }
  
  
	/**Sends the prepared telegram.
	 * @return
	 */
	public int send()
	{
	  assert(bFillTelg);
    int lengthDatagram = txAccess.getLength();
    txAccess.setLengthDatagram(lengthDatagram);
    //send the telegram:
    checkerRxTelg.setAwait(nSeqNumber);
    timeSend = System.currentTimeMillis();
    int ok = ipc.send(txBuffer, lengthDatagram, targetAddr);
    if(logTelg !=null){ 
      logTelg.sendMsg(identLogTelg +1, "send telg length= %s, ok = %d", lengthDatagram, ok); 
    }
    bFillTelg = false;
	  bIsSentTelg = true;
	  bShouldSend = false;
	  return nSeqNumber;
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
  

  void sendAndAwaitAnswer()
  { send();
    InspcDataExchangeAccess.Datagram[] answerTelgs = awaitAnswer(2000);
    if(answerTelgs !=null){
      rxEval.evaluate(answerTelgs, null, logTelg, identLogTelg + 5); //executerAnswerInfo);  //executer on any info block.
    } else {
      System.err.println("InspcAccessor - sendAndAwaitAnswer; no communication" );
    }
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
  
  boolean bRun, bFinish, bWaitFinish;
  
  /**Closes the thread for receive
   * @see java.io.Closeable#close()
   */
  @Override public void close() throws IOException
  { if(bRun){ //on error bRun is false
      bRun = false;
      ipc.close();
      synchronized(receiveRun){
        while( !bFinish){ try{ receiveRun.wait(); } catch(InterruptedException exc){}}
      }
    }
  }

  
  
  void receiveFromTarget()
  { bRun = true;
    int[] result = new int[1];
    while(bRun){
      byte[] rxBuffer = ipc.receive(result, targetAddr);
      if(result[0]>0){
        long time = System.currentTimeMillis();
        dtimeReceive = time - timeSend;
        if(logTelg !=null){ 
          logTelg.sendMsg(identLogTelg+3, "recv telg after %d ms", dtimeReceive); 
        }
        checkerRxTelg.applyReceivedTelg(rxBuffer, result[0], logTelg, identLogTelg +4);
      } else {
        System.out.append("receive error");
      }
    }//while
    
    synchronized(receiveRun){ 
      bFinish = true;     //NOTE set in synchronized state, because it should wait for
      receiveRun.notify(); 
    }
  }
  
  
  InspcAccessExecRxOrder_ifc actionRx4ValueByIdent = new InspcAccessExecRxOrder_ifc(){
    @Override public void execInspcRxOrder(Info info, LogMessage log, int identLog)
    { execRx4ValueByIdent(info, log, identLog);
    }
  };
	
	void stop(){}


}
