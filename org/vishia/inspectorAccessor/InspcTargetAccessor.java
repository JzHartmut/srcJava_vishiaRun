package org.vishia.inspectorAccessor;


import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


import org.vishia.byteData.ByteDataAccess;
import org.vishia.byteData.ByteDataAccessSimple;
import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.inspector.InspcTelgInfoSet;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.reflect.ClassJc;

/**An instance of this class accesses one target device via InterProcessCommunication, usual Ethernet-Sockets.
 * This class gets an opened {@link InterProcessComm} from its {@link InspcCommPort} aggregate 
 * and creates a receiving thread. 
 * Any send requests are invoked from the environment. A send request usual contains
 * a reference to a {@link InspcAccessExecRxOrder_ifc}. 
 * Thats {@link InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Reflitem, long, LogMessage, int)} 
 * is executed if the response item is received. With the order-number-concept 
 * see {@link InspcDataExchangeAccess.Reflitem#getOrder()} the request and the response are associated together.
 * <br><br>
 * The following sequence shows handling in one thread for cyclic communication. Env is one thread
 * which invokes this sequence diagram cyclically.
 * <pre>        
 * Env                       this                 txAccess      ipc        commPort
 *  |--isReady()-------------->|                      |          |            |
 *  |--cmdGetSet...----------->|--->addChild--------->|          |            |
 *  |     ...                  |       ...            |          |            |
 *  |--cmdGetSet...----------->|--->addChild--------->|          |            |
 *  |                          |                      |          |<-receive()-|
 *  |-->cmdFinit()------------>|-setLengthDatagram()->|          |            |
 *  |                  [bTaskPending = true]
 *  |                          |---send(txBuffer)--------------->|            |
 *  ~                          ~                                 ~            ~
 *  |                          |                                 |..receive..>|
 *  |                          |<----evaluateRxTelg(datagram)-----------------|  
 *  |<-execInspcRxOrder(item)--|                                 |
 *  |     ...                  |                                 |
 *  |<-execInspcRxOrder(item)--|---send(txBuffer)--------------->|            |
 *  ~                          ~                                 ~            ~
 *  |                          |                                 |..receive..>|
 *  |                          |<----evaluateRxTelg(datagram)-----------------|  
 *  |<-execInspcRxOrder(item)--|
 *  |     ...                  |                               
 *  |<-execInspcRxOrder(item)--|  
 *  |                      [on last datagram:    ]
 *  |                      [bTaskPending = false;]
 *  |                          |
 *  ~                          ~
 *  |--isReady()-------------->|   //the next task
 *                               
 *  </pre>
 * First {@link #isReady(long)} should be invoked. This routine checks whether the communication hangs. 
 * If that routine returns false, a new cmd can't be sent. This is because the target has not answered. 
 * After a dedicated timeout after the last request this {@link #isReady(long)} routines clears all ressources
 * and returns true. Then a new request is accepted. A target may be absent, the communication hangs therefore,
 * but later it may be present already. If one telegram was lost, it is the same behavior. Generally telegrams
 * are not repeated, because usual new values should be gotten after new requests.
 * The {@link #isReady(long)} routine can be invoked before any cmd..., if it is ready, it is a lightweight routine.
 * <br><br>
 * The cmdGetSet routines are:
 * <ul>
 * <li>{@link #cmdGetValueByPath(String, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdRegisterByPath(String, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdGetValueByIdent(int, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdGetAddressByPath(String)}
 * <li>{@link #cmdSetValueByPath(String, double)}
 * <li>{@link #cmdSetValueByPath(String, float)}
 * <li>{@link #cmdSetValueByPath(String, long, int)}
 * </ul>
 * The routines organizes the datagram. If one datagram is full, the next instance is used. There are 10 instances
 * of {@link TxBuffer}.
 * <br><br>
 * At least the user should invoke {@link #cmdFinit()}. That command completes the only one or the last datagram
 * and sends the first datagram to the target. With this invocation the {@link #isReady(long)} returns false
 * because the variable {@link #bTaskPending} is set to true.
 * <br><br>
 * The target should response with the answer with the same sequence number to associate answer and request. 
 * The {@link #commPort} ({@link InspcCommPort}) gets the received telegram.
 * On comparison of the sender address in the datagram or on comparison of the entrant in the 
 * datagram's head {@link InspcDataExchangeAccess.ReflDatagram#getEntrant()}.
 * The answer from target can consist of more as one datagram. Any correct answer telegram invokes 
 * {@link #evaluateRxTelg(byte[], int)}. 
 * The answer contains some {@link InspcDataExchangeAccess.Reflitem}. All of them has its order number. 
 * With the order number the associated {@link InspcAccessExecRxOrder_ifc
 * #execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Reflitem, long, LogMessage, int)}
 * is invoked as callback in the users space. This is organized in the aggregated {@link #rxEval} instance.   
 * <br><br>
 * If the last answer datagram of a request is gotten, the next datagram  of this task will be sent to the target. The target
 * will be attacked only by one telegram in one time, all one after another. That is because a target may have
 * less network resources (a cheap processor etc.). After the last receive datagram of the last send datagram
 * the semaphore {@link #bTaskPending} is set to false, a next {@link #isReady(long)} returns true 
 * for the next task of requests. 
 * <br><br>
 * On receive the {@link #commPort} ({@link InspcCommPort}) gets the received telegram.
 * On comparison of the sender address in the datagram or on comparison of the entrant in the 
 * datagram's head {@link InspcDataExchangeAccess.ReflDatagram#getEntrant()}.
 * <br><br>
 * <b>Requests in multithreading</b><br>
 * The routines {@link #cmdGetValueByPath(String, InspcAccessExecRxOrder_ifc)} etc. are not threadsafe.
 * That is because the organization of a datagram content should be controlled by one hand. Typically this 
 * only one thread runs cyclically because it requests currently data.
 * <br>
 * To support requests in more as one thread two strategies are supported:
 * <ul>
 * <li>A thread can assemble its own datagram and then add to the cyclic communication thread.
 * <li>A thread can invoke simple request items which are stored in a {@link ConcurrentLinkedQueue} and 
 *   regarded on next cyclic call.
 * </ul>
 * In both cases the {@link InspcAccessExecRxOrder_ifc
 * #execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Reflitem, long, LogMessage, int)}
 * is invoked by the receiver's thread called back in the user's space. This routines can be used to notify
 * any waiting thread or any short action can be done in the callback, especially request of some more cmd...    
 * @author Hartmut Schorrig
 *
 */
public class InspcTargetAccessor 
{
	
  /**The version history and license of this class.
   * <ul>
   * <li>2012-04-08 Hartmut chg: Now change of time regime. The request thread writes only data 
   *   and invokes the first send. All other communication is done in the receive thread. 
   *   The goal is: The target system should gotten only one telegram at one time, all telegrams one after another.
   * <li>2012-04-08 Hartmut new: Support of GetValueByIdent
   * <li>2012-04-05 Hartmut new: Use {@link LogMessage to test telegram traffic}
   * <li>2012-04-02 Hartmut new: {@link #sendAndPrepareCmdSetValueByPath(String, long, int, InspcAccessExecRxOrder_ifc)}.
   *   The concept is: provide the {@link InspcAccessExecRxOrder_ifc} with the send request.
   *   It should be implement for all requests in this form. But the awaiting of answer doesn't may the best way.
   *   Problem evaluating the answer telg in the rx thread or in the tx thread. TODO documentation.
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

  
  private class TxBuffer{
    byte[] buffer;
    
    TxBuffer(){}
    int nSeq;
    int nrofBytesTelg;
    //AtomicInteger stateOfTxTelg = new AtomicInteger();
    boolean lastTelg;
  }
  
  
  /**If true then writes a log of all send and received telegrams. */
  LogMessage logTelg;
  
  boolean bWriteDebugSystemOut;  //if(logTelg !=null && bWriteDebugSystemOut)
  
  int identLogTelg;
  
	private final InspcAccessGenerateOrder orderGenerator = new InspcAccessGenerateOrder();
	
	private final TxBuffer[] tx = new TxBuffer[10];

	/**Index for {@link #txBuffer} for the currently filled buffer. */
	private int ixTxFill = 0;

  /**Index for {@link #txBuffer} for the buffer which should be send if it is full. */
  private int ixTxSend = 0;
  
 //private final InspcAccessCheckerRxTelg checkerRxTelg = new InspcAccessCheckerRxTelg();
	
	/**A Reflitem set instance. */
	private final InspcTelgInfoSet infoAccess;
	
  /**Instance to evaluate received telegrams. It is possible that a derived instance is used! */
  public final InspcAccessEvaluatorRxTelg rxEval;
  
  
  final InspcDataExchangeAccess.ReflDatagram accessRxTelg = new InspcDataExchangeAccess.ReflDatagram();


  char state = 'R';
  
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
  
  
  /**True if it is sent and not all answers are received yet. */
  //private final AtomicBoolean bSendPending = new AtomicBoolean();
  
  
  /**Set if the {@link #cmdFinit()} is given. set false on last received answer for the task. */
  private final AtomicBoolean bTaskPending = new AtomicBoolean();
  
  
  private boolean bRequestWhileTaskPending;
  
  /**Set if it is sent and all answers are received. It is for the current {@link #nSeqNumber}. */
  private boolean bHasAnswered;
  
  /**Set to true if the answer is missing after timeout.
   * Then a new send request should be done after a longer time only.
   */
  private boolean bNoAnswer = false;
  
	long timeSend, timeReceive, dtimeReceive, dtimeWeakup;
	
	private final InspcDataExchangeAccess.ReflDatagram txAccess = new InspcDataExchangeAccess.ReflDatagram();
	
	
	/**Number of idents to get values per ident. It determines the length of an info block,
	 * see {@link #dataInfoDataGetValueByIdent}. An info block has to be no longer than a UDP-telegram.
	 */
	private final static int zIdent4GetValueByIdent = 320;
	
	private int ixIdent5GetValueByIdent;
	
	/**A info element to get values by ident It contains a {@link InspcDataExchangeAccess.Reflitem} head
	 * and then 4-byte-idents for data. The same order of data are given in the array
	 */
	private final byte[] dataInfoDataGetValueByIdent = new byte[InspcDataExchangeAccess.Reflitem.sizeofHead + 4 * zIdent4GetValueByIdent]; 
	
	
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
	
	String XXXsTargetIpAddr;
	
	final Address_InterProcessComm targetAddr;
	
	/**The current used sequence number. It is never 0 if it is used the first time. */
	int nSeqNumber = 600; 
	
	/**The sequence number for that datagram which is send and await to receive.
	 * Note that the target can send more as one answer telegram for one request.
	 * All answers has the same sequence number, but they have different answer numbers. 
	 */
	int nSeqNumberTxRx;
	
	/**Up to 128 answer numbers which are received already for the request.
	 * A twice received answer is not evaluated twice. Note that in network communication
	 * any telegram can received more as one time with the same content.
	 */
	int[] bitsAnswerNrRx = new int[8];
	
	int nEncryption = 0;
	
  private final InspcCommPort commPort;	
	
	
	public InspcTargetAccessor(InspcCommPort commPort, Address_InterProcessComm targetAddr, InspcAccessEvaluatorRxTelg inspcRxEval)
	{ this.commPort = commPort;
	  this.targetAddr = targetAddr;
		this.rxEval = inspcRxEval;
		this.infoAccess = new InspcTelgInfoSet();
    for(int ix = 0; ix < tx.length; ++ix){
      tx[ix] = new TxBuffer();
      tx[ix].buffer = new byte[1400];
      //tx[ix].stateOfTxTelg.set(0);
    }
		commPort.registerTargetAccessor(this);
		
    //The factory should be loaded already. Then the instance is able to get. Loaded before!
	}

	
	/**Sets the target address for the next telegram assembling and the next send()-invocation.
	 * If a telegram is pending, it will be sent. A new telegram is prepared.
	 * @param sTargetIpAddr
	 */
	@Deprecated
	public void setTargetAddr(String sTargetIpAddr)
	{ //TODO check and send, prepare new head.
	  //this.sTargetIpAddr = sTargetIpAddr;
	  //this.targetAddr = new Address_InterProcessComm_Socket(sTargetIpAddr);
    //commPort.registerTargetAccessor(this);
	  assert(false);
	}
	
	
	
	/**Switch on or off the log functionality.
	 * @param logP The log output. null then switch of the log.
	 * @param ident The ident number in log for send, next number for receive.
	 */
	public void setLog(LogMessage log, int ident){
	  this.logTelg = log;
	  this.identLogTelg = ident;
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
  private boolean prepareTelg(int lengthNewInfo)
  { bHasAnswered = false;
    if(ixTxFill >= tx.length ){
      System.err.println("InspcTargetAccessor - Too many telegram requests;");
      return false;
    }
    if(bFillTelg && (txAccess.getLengthTotal() + lengthNewInfo) > tx[ixTxFill].buffer.length){
      //the next requested info does not fit in the current telg. 
      //Therefore send the telg but only if the other telgs are received already!
      completeDatagramAndMaybeSend(false);
      if((ixTxFill) >= tx.length){
        return false;   //not enough space for communication. 
      }
    }
    if(!bFillTelg){
      //assert(tx[ixTxFill].stateOfTxTelg.compareAndSet(0, 'f'));
      txAccess.assignEmpty(tx[ixTxFill].buffer);
      if(++nSeqNumber == 0){ nSeqNumber = 1; }
      txAccess.setHeadRequest(nEntrant, nSeqNumber, nEncryption);
      bFillTelg = true;
      if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - Prepare head; " + ixTxFill + "; seq=" + nSeqNumber);
      int nRestBytes = tx[ixTxFill].buffer.length - txAccess.getLengthHead();
      return true;
    }
    int lengthDatagram = txAccess.getLengthTotal();
    
    int nRestBytes = tx[ixTxFill].buffer.length - lengthDatagram;
    return true;
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
	
	
	/**Returns true if one time an answer was missing after the timeout wait time. 
	 * A new request should be sent after a longer time. But it should be sent because the target
	 * may be reconnected. It is possible to send only after a manual command.
	 */
	public boolean isReady(long time){ 
	  if(!bTaskPending.get()) return true;
	  else {
	    if((time - timeSend) > 3000){
	      //forgot a pending request:
	      bTaskPending.set(false);
	      bRequestWhileTaskPending = false;
	      ixTxFill = 0;
	      ixTxSend = 0;
	      state = 'R';
	      bFillTelg = false;
	      bShouldSend = false;
	      bIsSentTelg = false;
	      //bSendPending.set(false);
	      bHasAnswered = false;
	      bNoAnswer = true;
	      rxEval.ordersExpected.clear();  //after long waiting their is not any expected.
	      return true;
	    } else {
	      if(!bRequestWhileTaskPending){ //it is the first one
	        bRequestWhileTaskPending = true;
          System.err.println("InspcTargetAccessor.isReady - not ready for requests; " + toString());
	      }
	      return false;
	    }
	  }
	}
	
	
  /**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created.
   */
  public int cmdGetValueByPath(String sPathInTarget, InspcAccessExecRxOrder_ifc actionOnRx)
  { int order;
    if(prepareTelg(InspcDataExchangeAccess.Reflitem.sizeofHead + sPathInTarget.length() + 3 )){
      //InspcTelgInfoSet infoGetValue = new InspcTelgInfoSet();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdGetValueByPath(sPathInTarget, order);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg, "send cmdGetValueByPath %s, order = %d", sPathInTarget, new Integer(order)); 
      }
      rxEval.setExpectedOrder(order, actionOnRx);
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
    infoAccess.setInfoHead(zInfo, InspcDataExchangeAccess.Reflitem.kRegisterRepeat, order);
    //
    if(logTelg !=null){ 
      logTelg.sendMsg(identLogTelg, "send registerByPath %s, order = %d", sPathInTarget, new Integer(order)); 
    }
    return order;
  }
  
  
  
  /**Adds the info block to send 'register by path'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created because the telegram is full.
   */
  public boolean cmdGetValueByIdent(int ident, InspcAccessExecRxOrder_ifc actionOnRx)
  { if(ixIdent5GetValueByIdent >= actionRx4GetValueByIdent.length){
      return false;
      //  txCmdGetValueByIdent();
    }
    actionRx4GetValueByIdent[ixIdent5GetValueByIdent] = actionOnRx;
    accInfoDataGetValueByIdent.addChildInteger(4, ident);
    ixIdent5GetValueByIdent +=1;
    return true;
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
      int posInTelg = infoAccess.getPositionInBuffer() + InspcDataExchangeAccess.Reflitem.sizeofHead;
      System.arraycopy(dataInfoDataGetValueByIdent, 0, infoAccess.getData(), posInTelg, 4 * ixIdent5GetValueByIdent);
      infoAccess.setInfoHead(lengthInfo + InspcDataExchangeAccess.Reflitem.sizeofHead, InspcDataExchangeAccess.Reflitem.kGetValueByIndex, order);
      ixIdent5GetValueByIdent = 0;
      accInfoDataGetValueByIdent.assignEmpty(dataInfoDataGetValueByIdent);
      return true;
    } else return false;
  }
  
  final void execRx4ValueByIdent(InspcDataExchangeAccess.Reflitem info, long time, LogMessage log, int identLog){
    //int lenInfo = info.getLength();
    int ixVal = (int)info.getChildInteger(4);
    while(info.sufficingBytesForNextChild(1)){  //at least one byte in info, 
      InspcAccessExecRxOrder_ifc action = actionRx4GetValueByIdent[ixVal];
      ixVal +=1;
      if(action !=null){
        action.execInspcRxOrder(info, time, log, identLog);
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
    int zInfo = InspcDataExchangeAccess.Reflitem.sizeofHead + InspcDataExchangeAccess.ReflSetValue.sizeofElement + zPath + restChars; 
    if(prepareTelg(zInfo )){
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg, "send cmdSetValueByPath %s, order = %d, value=%8X, type=%d", sPathInTarget
            , new Integer(order), new Long(value), new Integer(typeofValue)); 
      }
      //infoAccess.setCmdSetValueByPath(sPathInTarget, value, typeofValue, order);
      InspcDataExchangeAccess.ReflSetValue accessSetValue = new InspcDataExchangeAccess.ReflSetValue(); 
      infoAccess.addChild(accessSetValue);
      accessSetValue.setLong(value);
      infoAccess.addChildString(sPathInTarget);
      if(restChars >0) { infoAccess.addChildInteger(restChars, 0); }
      assert(infoAccess.getLength() == zInfo);  //check length after add children. 
      infoAccess.setInfoHead(zInfo, InspcDataExchangeAccess.Reflitem.kSetValueByPath, order);
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
  @Deprecated
  public boolean sendAndPrepareCmdSetValueByPath(String sPathInTarget, long value, int typeofValue, InspcAccessExecRxOrder_ifc exec)
  { int order;
    boolean sent = false;
    do {
      order = cmdSetValueByPath(sPathInTarget, value, typeofValue);    
      if(order !=0){ //save the order to the action. It is taken on receive.
        this.rxEval.setExpectedOrder(order, exec);
      } else {
        //XXXsendAndAwaitAnswer();  //calls execInspcRxOrder as callback.
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
   * @return The order number. 0 if the cmd can't be created because the telgram is full.
   */
  public int cmdSetValueByPath(String sPathInTarget, float value)
  { int order;
    if(prepareTelg(InspcDataExchangeAccess.Reflitem.sizeofHead + 8 + sPathInTarget.length() + 3 )){
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
    if(prepareTelg(InspcDataExchangeAccess.Reflitem.sizeofHead + 8 + sPathInTarget.length() + 3 )){
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
    if(prepareTelg(InspcDataExchangeAccess.Reflitem.sizeofHead + sPathInTarget.length() + 3 )){
      //InspcTelgInfoSet infoGetValue = new InspcTelgInfoSet();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdGetAddressByPath(sPathInTarget, order);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg, "send cmdGetAddressByPath %s, order = %d", sPathInTarget, new Integer(order)); 
      }
    } else {
      //too much info blocks
      order = 0;
    }
    return order;
  }
  
  
  /**This routine have to be called after the last cmd in one thread. It sends the last telg
   * or initialized the send for previous telgs if the answer is not gotten yet.
   * @return true if anything to send, false if no cmd was given.
   */
  public boolean cmdFinit(){
    if(bFillTelg){
      bTaskPending.set(true);
      completeDatagramAndMaybeSend(true);
      send();   //send the first telegramm now
      return true;
    } else {
      return false;  //nothing to do
    }
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
  
  
  
  /**Completes this datagram. Send it if it is the first one or all others has received already.
   */
  private void completeDatagramAndMaybeSend(boolean lastTelg){
    assert(bFillTelg);
    int lengthDatagram = txAccess.getLength();
    tx[ixTxFill].nrofBytesTelg = lengthDatagram;
    txAccess.setLengthDatagram(lengthDatagram);
    tx[ixTxFill].nSeq = nSeqNumber;
    tx[ixTxFill].lastTelg = lastTelg;
    bFillTelg = false;
    state = 's';
    if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - complete Datagram; " + lastTelg + "; " + ixTxFill + "; seqnr " + nSeqNumber);
    ixTxFill +=1;
  }
  
  
  
  
	/**Sends the prepared telegram.
	 * @return
	 */
	private void send()
	{
    //send the telegram:
    //checkerRxTelg.setAwait(nSeqNumber);
    timeSend = System.currentTimeMillis();
    nSeqNumberTxRx = tx[ixTxSend].nSeq;
    //bSendPending = true;
    bIsSentTelg = true;
    int lengthDatagram = tx[ixTxSend].nrofBytesTelg;
    int ok = commPort.send(this, tx[ixTxSend].buffer, lengthDatagram);
    if(logTelg !=null){ 
      logTelg.sendMsg(identLogTelg +1, "send telg length= %s, ok = %d", new Integer(lengthDatagram), new Integer(ok)); 
    }
    bShouldSend = false;
	}
	
	
	
  void evaluateRxTelg(byte[] rxBuffer, int rxLength){
    timeReceive = System.currentTimeMillis();
    dtimeReceive = timeReceive - timeSend;
    if(logTelg !=null){ 
      logTelg.sendMsg(identLogTelg+3, "recv telg after %d ms", new Long(dtimeReceive)); 
    }
    long time = System.currentTimeMillis();
    accessRxTelg.assignData(rxBuffer, rxLength);
    int rxSeqnr = accessRxTelg.getSeqnr();
    //int rxAnswerNr = accessRxTelg.getAnswerNr();
    if(rxSeqnr == this.nSeqNumberTxRx){
      //the correct answer
      int nAnswer = accessRxTelg.getAnswerNr();
      int bitAnswer = 1 << (nAnswer & 0xf);
      int ixAnswer = nAnswer >> 4;
      if((bitsAnswerNrRx[ixAnswer] & bitAnswer) ==0){
        bitsAnswerNrRx[ixAnswer] |= bitAnswer;
        rxEval.evaluate(accessRxTelg, null, time, null, 0);
        if(accessRxTelg.lastAnswer()){
          //bSendPending = false;
          if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - Rcv last answer; " + rxSeqnr);
          Arrays.fill(bitsAnswerNrRx, 0);
          boolean wasLastTxTelg = tx[ixTxSend].lastTelg;  //the ixTxSend is the index of send still.
          ixTxSend +=1;  //next send slot
          if(ixTxSend < ixTxFill){
            if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - Send next telg; seqnr=" + tx[ixTxSend].nSeq + "; ixTxSend= " + ixTxSend);
            //Note: sendPending remain set. For this next telegram.
            send();
          } else if(wasLastTxTelg){  //last is reached.
            bTaskPending.set(false);  //Note, any other telg of this step can be follow.
            bRequestWhileTaskPending = false;
            state = 'R';
            ixTxSend = 0;
            ixTxFill = 0;   //if the last one was received, the tx-buffer is free for new requests. 
            nSeqNumberTxRx = 0;
            bHasAnswered = true;
            if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - All received; ");
          }
        } else {
          //sendPending: It is not the last answer, remain true
          if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - Rcv answer; " + nAnswer + "; seqn=" + rxSeqnr);
        }
      } else {
        //sendPending: rx is not an anwer, ignored, remain true
        if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor - faulty seqnr; " + rxSeqnr + "; expected " + this.nSeqNumberTxRx);
        //faulty seqnr
      }
    } else {
      //sendPending: is false, unexpected rx
      if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor - unexpected rx; ");
    }
  }
  
  

	
	/**Waits for answer from the target.
	 * This method can be called in any users thread. Typically it is the thread, which has send the telegram
	 * to the target. The alternate method is {@link #setExecuterAnswer(InspcAccessExecAnswerTelg_ifc)}.
	 * 
	 * @param timeout for waiting.
	 * @return null on timeout, the answer datagrams elsewhere.
	 */
	public InspcDataExchangeAccess.ReflDatagram[] awaitAnswer(int timeout)
	{ //InspcDataExchangeAccess.ReflDatagram[] answerTelgs = checkerRxTelg.waitForAnswer(timeout); 
  	long time = System.currentTimeMillis();
    dtimeWeakup = time - timeSend;
    return null; //answerTelgs;
  }
	
	
	
	@Override public String toString(){
	  
	  return targetAddr.toString() + ":" + state;
	}
	
	
  
  
  InspcAccessExecRxOrder_ifc actionRx4ValueByIdent = new InspcAccessExecRxOrder_ifc(){
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Reflitem info, long time, LogMessage log, int identLog)
    { execRx4ValueByIdent(info, time, log, identLog);
    }
  };
	
	void stop(){}


}
