package org.vishia.inspcPC.accTarget;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.vishia.byteData.ByteDataAccessBase;
import org.vishia.byteData.ByteDataAccessSimple;
import org.vishia.byteData.VariableAccessArray_ifc;
import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessComm_SocketImpl;
import org.vishia.event.EventCmdtypeWithBackEvent;
import org.vishia.event.EventTimeout;
import org.vishia.event.EventTimerThread;
import org.vishia.inspcPC.InspcAccessExecRxOrder_ifc;
import org.vishia.inspcPC.InspcAccess_ifc;
import org.vishia.inspcPC.InspcPlugUser_ifc;
import org.vishia.inspcPC.InspcTargetAccessData;
import org.vishia.inspcPC.mng.InspcMng;
import org.vishia.inspcPC.mng.InspcStruct;
import org.vishia.inspcPC.mng.InspcVariable;
import org.vishia.inspectorTarget.InspcTelgInfoSet;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.reflect.ClassJc;
import org.vishia.states.StateMachine;
import org.vishia.states.StateSimple;
import org.vishia.util.Assert;
import org.vishia.util.Debugutil;

/**An instance of this class accesses one target device via InterProcessCommunication, usual Ethernet-Sockets.
 * This class gets an opened {@link InterProcessComm} from its {@link InspcCommPort} aggregate. 
 * The InspcCommPort has created a receiving thread.<br> 
 * <img src="../../../../img/InspcTargetAccessor_omd.png"><br>Object model diagram:
 * <ul>
 * <li>The {@link InspcMng} knows one or more InspcTargetAccessor-instances, indicated by its name. 
 *   The name of the target access is the first part of the data path to a target: <code>"name:path.in.target"</code>
 * <li>Any of that InspcTargetAccessor handles the datagrams to and from one target.
 * <li>More as one InspcTargetAccessor or all one can handle their communication via only one instance of {@link InspcCommPort}.
 * <li>The InspcCommPort associates an {@link InterProcessComm} interface which is implemented by the {@link InterProcessComm_SocketImpl}
 *   or maybe for a serial communication or Dual-Port-RAM or other. It means the communication connection to a target can be done
 *   in several ways independent of the mission of this class. 
 * </ul>  
 * See also: <li><a href="../../../../../../Inspc/html/InspcComm.html">.../vishia/Inspc/InspcComm.html</href> or 
 * <a href="http://www.vishia.org/Inspc/html/InspcComm.html">www.vishia.org/Inspc/InspcComm.html</href>
 * <br><br>
 * A send requests are invoked from the environment, it is the {@link InspcMng}. 
 * <ul>
 * <li>Firstly {@link #isOrSetReady(long)} is invoked to see whether a communication task is pending yet.
 * <li>If a communication is pending for this target, new requests should be deferred.
 * <li>If the communication hangs, {@link #isOrSetReady(long)} clears the communication to this target and returns true
 *   if the given timeout is expired: It is possible to unplug a target - it does not answer - and plug again later. 
 *   The target does not repeat the lost answers. But it is ready for new communication. The timeout should be a proper time for the target.
 *   If it is to less, the target may prepare the answer yet in the moment and it is disturbed hectically. The timeout should be acceptable
 *   for usage. About 5 seconds may be a proper value.
 * <li>If the communication is not pending, {@link #isOrSetReady(long)} returns true.
 * <li>Then some requests can be prepared by invocation of the methods of the {@link InspcAccess_ifc} <code>cmdGet/Set</code>.... 
 *   With them one or more datagrams are prepared. 
 * <li>The requests contain a callback reference to  a {@link InspcAccessExecRxOrder_ifc}. 
 * Thats {@link InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem, long, LogMessage, int)} 
 * is executed if the response item is received. With the order-number-concept 
 * see {@link InspcDataExchangeAccess.Inspcitem#getOrder()} the request and the response are associated together.  
 * <li>At least {@link #cmdFinit()} should be invoked. That completes the one or last datagram and forces sending to the target.   
 * </ul>
 * The following sequence shows that. This sequence is executed in one thread for cyclic communication. 
 * <pre>        
 * InspcMng                  this                 txAccess   commPort     ipc
 *  |                          |                                 |            |
 *  ~                          ~                                 ~            ~
 *  +procComm():               |                                 |            |
 *  +--isOrSetReady(...)------>|                      |          |            | 
 *  +--cmdGetSet...----------->|--->addChild--------->|          |            | 
 *  +--isOrSetReady(...)------>|                      |          |            | 
 *  +--cmdGetSet...----------->|--->addChild--------->|          |            | 
 *  +--cmdGetSet...----------->|--->addChild--------->|          |            | 
 *  +                          |--completeDatagram()->|          |            | 
 *  +--cmdGetSet...----------->|--->addChild--------->|          |            | 
 *  +--cmdGetSet...----------->|--->addChild--------->|          |            | 
 *  +                          |                      |          |            | 
 *  +-->cmdFinit()------------>|--completeDatagram()->|          |            | 
 *  |                  [bTaskPending = true]                     |            | 
 *  |                          |---send(txBuffer)--------------->|--send----->| 
 *  |                          |                                 |            | 
 *  ~                          ~                                 ~            ~ 
 * </pre>
 * Therewith up to 10 datagrams are assembled. Only the first datagram is sent yet. The target may answer:
 * <ul>
 * <li>An answer may need more as one answer datagram. 
 *  The received datagrams are checked whether they have the correct sequence and answer number. Only then
 *  they are stored in {@link #_tdata} {@link TelgData#rxDatagram}. This is done in {@link #evaluateRxTelg(byte[], int)}.
 * <li>The answer datagrams don't need to receive in the correct order. There are filled some bits {@link TelgData#rxBitsAnswerNr}. 
 *  An datagram which is received twice is ignored as twice.
 * <li>The last answer datagram is marked with {@link InspcDataExchangeAccess.InspcDatagram#lastAnswer()}, a special bit. 
 *  With them is can be recognized that all datagrams are completely received.
 * </ul>
 * <pre> 
 *                           this                              commPort      ipc
 *                             |                                 |            !
 *                             ~                                 ~            ~<<==
 *                             |                                 !            !   "
 *                             !                                 !<..receive..!   "
 *                 [rxDatagram]!<---evaluateRxTelg(datagram)-----!            !   "
 *                 [rxDatagram]!<---evaluateRxTelg(datagram)-----!            !   "
 *                             !                                 !            !   "
 *                             ~                                 ~            ~   "
 * </pre>
 * <ul>
 * <li>The cyclic {@link InspcMng#procComm()} invokes {@link #evaluateRxTelgInspcThread()} cyclically. This routine checks 
 *   whether all datagrams of one request datagram are received. It invokes 
 *   {@link #evaluateOneDatagram(org.vishia.communication.InspcDataExchangeAccess.InspcDatagram, InspcAccessExecRxOrder_ifc, long, LogMessage, int, DebugTxRx, int)}
 *   internally for each of the received datagrams.
 * <li>The data of the datagram are evaluated using the {@link InspcDataExchangeAccess.Inspcitem} for each item.
 *   That is derived from {@link ByteDataAccessBase} which converts the byte data in the datagram to user specifics.
 * <li>For each item the {@link InspcDataExchangeAccess.Inspcitem#getOrder()} is read. 
 *  With them and the stored {@link InspcAccessExecRxOrder_ifc} is searched in the {@link #ordersExpected} index.
 * <li>The {@link InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem, long, LogMessage, int)}
 *  is invoked with the received datagram item. That is the <b>callback to the users level</b> with the received information.     
 * <li>If the datagram is evaluated, the routine {@link #evaluateRxTelgInspcThread()} looks whether a next datagram with assembled requests
 *   are given furthermore. Then it sends is to the target.  
 * <li>With a next {@link #send()} this sequence is repeated.
 * </ul>
 * <pre>
 * InspcMng                  this                                                 "
 *  |                          |                                                  "
 *  ~                          ~                                                  "
 *  +procComm():               |                                                  "
 *  +-evalRxTelgInspcThread()->|[rxDatagram]
 *  +                          |-evaluateOneDatagram()-+            Requestor     "              
 *  +                          +<----------------------+                  |       "         
 *  +                          +----execInspcRxOrder(datagramitem, ...)-->|       "                          
 *  +                          +----execInspcRxOrder(datagramitem, ...)-->|       "                          
 *  +                          +----execInspcRxOrder(datagramitem, ...)-->|       "                          
 *  +                          +----execInspcRxOrder(datagramitem, ...)-->|       "                          
 *  +                          +                                          |       "
 *  +                          +                                                  "
 *  +                          +                               commPort      ipc  "
 *  +                          +                                 |            |   "
 *  +                          +---send(txBuffer)--------------->|...........>|   "
 *  +<.........................+                                 |            |   "
 *  |                          |                                 |            |   "
 *  |                          |                                 |            |   "
 *  ~                          ~                                 ~            ~====
 * </pre>
 * The communication is still pending yet. Next requests from user level are deferred till this repetition
 * of {@link #send()} and {@link #evaluateRxTelgInspcThread()} is currently.
 * <br><br>
 * If all datagrams are sent and all answers are received: 
 *  <pre>
 * InspcMng                  this                                                 "
 *  |                          |                                                  "
 *  ~                          ~                                 ~            ~
 *  |                          |[on last datagram:    ]
 *  |                          |[bTaskPending = false;]
 *  |                          |
 *  ~                          ~
 *  |--isOrSetReady(...)------>|   //the next communication cycle
 *  |                             
 *  </pre>
 * The cmdGetSet routines are:
 * <ul>
 * <li>{@link #cmdGetFields(String, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdGetValueByPath(String, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdRegisterHandle(String, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdGetValueByHandle(int, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdGetAddressByPath(String, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdSetValueByPath(String, double, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdSetValueByPath(String, float, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdSetValueByPath(String, int, InspcAccessExecRxOrder_ifc)}
 * <li>{@link #cmdSetValueByPath(String, long, int, InspcAccessExecRxOrder_ifc)}
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
 * datagram's head {@link InspcDataExchangeAccess.InspcDatagram#getEntrant()}.
 * The answer from target can consist of more as one datagram. Any correct answer telegram invokes 
 * {@link #evaluateRxTelg(byte[], int)}. 
 * The answer contains some {@link InspcDataExchangeAccess.Inspcitem}. All of them has its order number. 
 * With the order number the associated {@link InspcAccessExecRxOrder_ifc
 * #execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem, long, LogMessage, int)}
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
 * datagram's head {@link InspcDataExchangeAccess.InspcDatagram#getEntrant()}.
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
 * #execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem, long, LogMessage, int)}
 * is invoked by the receiver's thread called back in the user's space. This routines can be used to notify
 * any waiting thread or any short action can be done in the callback, especially request of some more cmd...    
 * @author Hartmut Schorrig
 *
 */
@SuppressWarnings("synthetic-access") 
public class InspcTargetAccessor implements InspcAccess_ifc
{
	
  /**The version history and license of this class.
   * <ul>
   * <li>2015-10-29 Hartmut chg: {@link #isOrSetReady(long)} now checks the timeout by its own {@link #cycle_timeout}.
   *   The last one can be changed in the {@link #setStateToUser(InspcPlugUser_ifc)} which acts with the graphical user interface.
   *   Therewith the timeout can be changed in the GUI especially for debugging situations.
   * <li>2015-08-08 Hartmut chg: getValueByHandle now works. Some changes. 
   * <li>2015-06-24 Hartmut chg: Writing debug info for all tx and rx items is able to disable. 
   * <li>2015-06-21 Hartmut chg: Now getFields set the {@link GetFieldsData#bGetFieldsPending} and prohibits further telegrams
   *   via {@link #isOrSetReady(long)} false-return: Reason: The target does not regard a new telegram
   *   for further get-value requests if the telegram has less free space. The target crashes instead. 
   *   On getFields more as one answer telegram is regarded. But the rest of the telegram for getValueByPath does not work. 
   *   An older non fixed target should be accepted because it runs in field - no update possibility!   
   * <li>2015-05-28 Hartmut new {@link #addUserTxOrder(Runnable)} not only for the whole {@link InspcMng}.
   * <li>2015-05-28 Hartmut bugfix length of item was faulty for #cmdSetValueByPath(...). 
   *   Check length in {@link InspcTelgInfoSet#lengthCmdSetValueByPath(int)}. For all commands.
   * <li>2015-05-20 Hartmut new {@link DebugTxRx}, data in sub classes 
   * <li>2015-03-20 Hartmut {@link #requestFields(InspcStruct, Runnable)} redesigned 
   * <li>2014-10-12 Hartmut State machine included, tested but not active used. 
   * <li>2014-09-21 Hartmut TODO use a state machine .
   * <li>2014-01-08 Hartmut chg: {@link #cmdSetValueByPath(String, int, InspcAccessExecRxOrder_ifc)},
   *   {@link #valueStringFromRxValue(org.vishia.communication.InspcDataExchangeAccess.Inspcitem, int)}
   * <li>2014-01-08 Hartmut chg: Now change of time regime. The request thread writes only data 
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
  final static String version = "2015-08-08";

  private static class DebugTxRx{ 
    //int[] posTelgItems = new int[200];
    int nrofBytesDatagramReceived;
    int nrofBytesDatagramInHead;
    /**The last send tx telegram. */
    InspcDataExchangeAccess.InspcDatagram txTelgHead; ////
    ArrayList<InspcDataExchangeAccess.Inspcitem> txItems;
    
    /**All received answerNr telegrams for the last send tx telegram. The received telegrams for older tx telegrams are removed. */
    InspcDataExchangeAccess.InspcDatagram[] rxTelgHead = new InspcDataExchangeAccess.InspcDatagram[20]; ////
    
    /**All received items for the rx telegrams of one tx telegram. */
    ArrayList<InspcDataExchangeAccess.Inspcitem>[] rxItems = new ArrayList[20];  //for up to 20 received telegrams.
    
    
    @SuppressWarnings("unchecked") 
    DebugTxRx() {
      txItems = new ArrayList<InspcDataExchangeAccess.Inspcitem>();
      for(int ix = 0; ix < rxItems.length; ++ix){
        rxItems[ix] = new ArrayList<InspcDataExchangeAccess.Inspcitem>();
      }
    }
    
    
    /**Removes all rx data, so that the garbage collector can garbage it.
     * Called on #isOrSetReady if ready.
     */
    void clearAll() {
      if(txTelgHead != null){ txTelgHead.removeChildren(); }
      txItems.clear();
      clearRx();
     }

    /**Removes all rx data, so that the garbage collector can garbage it.
     * Called on #isOrSetReady if ready.
     */
    void clearRx() {
      for(int ix = 0; ix < rxItems.length; ++ix){
        if(rxTelgHead[ix] != null){ 
          rxTelgHead[ix].removeChildren(); 
          rxTelgHead[ix] = null;
        }
        rxItems[ix].clear(); //the children items
      }
      nrofBytesDatagramInHead = -1;
      nrofBytesDatagramReceived = -1;

    }
  }

  
  /**This class contains all data for one datagram to send.
   * @author hartmut
   *
   */
  private class TxBuffer
  {
    byte[] buffer;
    
    TxBuffer(){}
    int nSeq;
    int nrofBytesTelg;
    //AtomicInteger stateOfTxTelg = new AtomicInteger();
    boolean lastTelg;
  }
  
  private static class OrderWithTime
  {
    final int order;
    final long time;
    final InspcAccessExecRxOrder_ifc exec;
    
    public OrderWithTime(int order, long time, InspcAccessExecRxOrder_ifc exec)
    { this.order = order;
      this.time = time;
      this.exec = exec;
    }
    
  }
  

  
  private static class TelgData
  {
    boolean bDebugTelg = true;
    
    /**Position of all received items.
     * 
     */
    DebugTxRx[] debugTxRx = new DebugTxRx[10];
    
    /**The current used sequence number. It is never 0 if it is used the first time. */
    int nSeqNumber = 600;

    /**The sequence number for that datagram which is send and await to receive.
     * Note that the target can send more as one answer telegram for one request.
     * All answers has the same sequence number, but they have different answer numbers. 
     */
    int nSeqNumberTxRx;

    private final TxBuffer[] tx = new TxBuffer[10];
    
    /**Index for {@link #txBuffer} for the currently filled buffer. */
    private int txixFill = 0;
    
    /**Index for {@link #txBuffer} for the buffer which should be send if it is full. */
    private int txixSend = 0;

    /**The start answer number is 1 for newer datagrams but 0 for older ones. If an answer with 0 is received one time, 
     * it is set to 0 for that target for ever because it is an old one.
     */
    //int startAnswerNr = 1;
    /**The old target starts the answers from 0 and has a step by 2. It is detect because the first answer as {@link InspcDataExchangeAccess.InspcDatagram#getAnswerNr()} ==0*/
    boolean oldTarget;
    
    /**Up to 64 answer numbers which are received already for the request.
     * A twice received answer is not evaluated twice. Note that in network communication
     * any telegram can received more as one time with the same content.
     */
    long rxBitsAnswerNr;

    long rxBitsAnswerMask;

    /**Up to 128 receive buffers for all datagrams for this sequence number, there may be more as one answer per request. */
    InspcDataExchangeAccess.InspcDatagram[] rxDatagram = new InspcDataExchangeAccess.InspcDatagram[128];

    /**Number of received datagrams for 1 sequence. */
    int rxNrofDatagramsForOneSend; 
    
    
    final InspcDataExchangeAccess.InspcDatagram accTxTelgStatic = new InspcDataExchangeAccess.InspcDatagram();
    
    final InspcTelgInfoSet accTxItemStatic = new InspcTelgInfoSet();
    
    final InspcDataExchangeAccess.InspcDatagram accRxTelgStatic = new InspcDataExchangeAccess.InspcDatagram();
    
    final InspcDataExchangeAccess.Inspcitem accRxItemStatic = new InspcDataExchangeAccess.Inspcitem();
  }
  
  TelgData _tdata = new TelgData();
  
  private static class GetFieldsData
  {

    /**If not null then cmdGetFields will be invoked in the next {@link InspcTargetAccessor#cmdFinit()} invocation.
     * After them this field will be set to null again. */
    InspcTargetAccessData requFields;
    
    /**Action on receiving fields from target, only valid for the requFields. */
    InspcAccessExecRxOrder_ifc rxActionGetFields;
    
    /**If not null then this runnable will be called on end of requestFields. */
    Runnable runOnResponseFields;
    
    /**The last order for Get Fields.
     * Special case: Only this telegram-info has more as one answer.
     * Only one request 'get fields' should be send in one time.
     * This order will be set on the first answer 'get fields' and remain for more answers.
     * The next request 'get fields' will be received with the first field with another order,
     * which is stored here etc.
     */
    OrderWithTime orderGetFields;

    private boolean bGetFieldsPending;
    
  } GetFieldsData getFieldsData = new GetFieldsData();
  
  
  /**Identifier especially for debugging. */
  final String name;
  
  /**Free String to write, for debug stop. */
  String dbgNameStopTx;

  /**If true then writes a log of all send and received telegrams. */
  LogMessage logTelg;
  
  boolean bWriteDebugSystemOut;  //if(logTelg !=null && bWriteDebugSystemOut)
  
  int identLogTelg;
  
	private final InspcAccessGenerateOrder orderGenerator = new InspcAccessGenerateOrder();
	
	
  
 //private final InspcAccessCheckerRxTelg checkerRxTelg = new InspcAccessCheckerRxTelg();
	
  /**Map of all orders which are send as request.
   * 
   */
  protected final Map<Integer, OrderWithTime> ordersExpected = new TreeMap<Integer, OrderWithTime>();
  
  
  final Deque<OrderWithTime> listTimedOrders = new LinkedList<OrderWithTime>();
  
  /**Some orders from any application which should be run in the {@link #inspcThread}. */
  private final ConcurrentLinkedQueue<Runnable> userTxOrders = new ConcurrentLinkedQueue<Runnable>();
  

  
  /**Map of all Variables which are gotten by handle.
   * 
   */
  final Map<Integer, InspcVariable> XXXvariablesByHandle = new TreeMap<Integer, InspcVariable>();
  
  
  /**Reused instance to evaluate any info blocks.
   * 
   */
  //InspcDataExchangeAccess.Inspcitem infoAccessRx = new InspcDataExchangeAccess.Inspcitem();
  
  

  
  public final Map<Integer, Runnable> callbacksOnAnswer = new TreeMap<Integer, Runnable>();
  

  char state = 'R';
  

  
  final States states;
  
  
  private final StateSimple stateIdle, stateWaitAnswer;
  
  enum Cmd{ fill, send, lastAnswer};
  
  class Ev extends EventCmdtypeWithBackEvent<Cmd, Ev>{ Ev(Cmd cmd){ super(cmd); } };
  
  Ev evFill = new Ev(Cmd.fill);
  
  Ev evSend = new Ev(Cmd.send);
  
  Ev evLastAnswer = new Ev(Cmd.lastAnswer);
  

  
  
  class States extends StateMachine
  {
    
    States(EventTimerThread thread){ super("InspcTargetAccessor", thread); }
    
    
    class StateInactive extends StateSimple {
      final boolean isDefault = true;

      Trans addRequest_Filling(EventObject ev, Trans trans)
      {
        if(trans ==null) return new Trans(StateFilling.class);
        if(ev == evFill){
          trans.retTrans = mTransit | mEventConsumed;
          trans.doExit();
          trans.doEntry(ev); 
        } 
        return trans;
      }
    
      
    };
    
    
    class StateIdle extends StateSimple {
   
      @Override public int entry(EventObject ev){
        //stateMachine.timeout(System.currentTimeMillis() + 10000);
        return 0;
      }
      
      Timeout timeout = new Timeout(10000, StateInactive.class);
      
      Trans addRequest_Filling = new Trans(StateFilling.class);
    
      @Override protected Trans checkTrans(EventObject ev){
        if(ev instanceof EventTimeout) return timeout;
        else if(ev == evFill) return addRequest_Filling;
        else return null;
      }

    };
    
    
    class StateFilling extends StateSimple {
      //StateSimple stateFilling = new StateSimple(states, "filling"){

      Trans addRequest = new Trans(StateFilling.class); //remain in state
  
      Trans shouldSend_WaitReceive = new Trans(StateWaitReceive.class);
    
      @Override protected Trans checkTrans(EventObject ev){
        if(ev == evFill) return addRequest;
        else if(ev == evSend) return shouldSend_WaitReceive;
        else return null;
      }
      
    };
    
    
    
    class StateWaitReceive extends StateSimple {
      //StateSimple stateSending = new StateSimple(states, "sending"){

      @Override public int entry(EventObject ev) {
        timeSend = System.currentTimeMillis(); 
        return 0;
      }
      
      Trans lastAnswer_WaitReceive(EventObject ev, Trans trans)
      {
        if(trans ==null) return new Trans(StateIdle.class);
        if(ev == evLastAnswer){
          trans.retTrans = mTransit | mEventConsumed;
          trans.doExit();
          trans.doEntry(ev); 
        } 
        return trans;
      }
      
    };
    
    
    
    class StateReceive extends StateSimple {
    //StateSimple stateWaitAnswer = new StateSimple(states, "waitAnswer"){

      Trans lastAnswer_Idle = new Trans(StateIdle.class);
    
      Trans notLastAnswer_WaitReceive = new Trans(StateReceive.class);
      
    };
    
    
 
    
    
    
    
  }
  
  
  //States states1 = new States();
  
  
  //final StateTop states = new StateTop(null);
  
  
  
  
  
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
  
	long timeSend, timeReceive, dtimeReceive, dtimeWeakup;
	
	/**Set to true while a telegram is evaluating in the received thread.
	 * Then {@link #isOrSetReady(long)} returns false though the time is expired.  
	 * This is a helper for debugging firstly. Don't send new telegrams while debugging.
	 */
	boolean bRunInRxThread;
	
	private InspcDataExchangeAccess.InspcDatagram txAccess;
	
	
	/**Number of idents to get values per ident. It determines the length of an info block,
	 * see {@link #dataInfoDataGetValueByIdent}. An info block has to be no longer than a UDP-telegram.
	 */
	private final static int zIdent4GetValueByIdent = 300;  //1200 Byte
	
	private int ixIdent5GetValueByIdent;
	
	/**A info element to get values by ident It contains a {@link InspcDataExchangeAccess.Inspcitem} head
	 * and then 4-byte-idents for data. The same order of data are given in the array
	 */
	private final byte[] dataInfoDataGetValueByIdent = new byte[InspcDataExchangeAccess.Inspcitem.sizeofHead + 4 * zIdent4GetValueByIdent]; 
	
	
	//ByteBuffer acc4ValueByIdent = new ByteBuffer();
	
	/**Managing instance of {@link ByteDataAccessBase} for {@link #dataInfoDataGetValueByIdent}. */
	private final ByteDataAccessBase accInfoDataGetValueByIdent;
	
	private final InspcAccessExecRxOrder_ifc[] actionRx4GetValueByHandle = new InspcAccessExecRxOrder_ifc[zIdent4GetValueByIdent]; 
	//InspcVariable[] variable4GetValueByIdent = new InspcVariable[zIdent4GetValueByIdent];
	
	/**The entrant is the sub-consumer of a telegram on the device with given IP.
	 * A negative number is need because compatibility with oder telegram structures.
	 * The correct number depends on the device. It is a user parameter of connection.
	 */
	int nEntrant = -1;
	
	final Address_InterProcessComm targetAddr;
	
	int nEncryption = 0;
	
  private final InspcCommPort commPort;	
	
  
  final float[] cycle_timeout = new float[2];
  
  
  public InspcTargetAccessor(String name, InspcCommPort commPort, Address_InterProcessComm targetAddr, EventTimerThread threadEvents)
  { this.name = name;
    this.commPort = commPort;
    this.targetAddr = targetAddr;
    cycle_timeout[0] = 0.1f;
    cycle_timeout[1] = 5.0f;
    accInfoDataGetValueByIdent = new ByteDataAccessBase(0);
    accInfoDataGetValueByIdent.setBigEndian(true);
    accInfoDataGetValueByIdent.assignClear(dataInfoDataGetValueByIdent);
    //this.infoAccess = new InspcTelgInfoSet();
    for(int ix = 0; ix < _tdata.tx.length; ++ix){
      _tdata.tx[ix] = new TxBuffer();
      _tdata.tx[ix].buffer = new byte[1400];
      //tx[ix].stateOfTxTelg.set(0);
    }
    for(int ix1 = 0; ix1 < _tdata.debugTxRx.length; ++ix1) {
        DebugTxRx debugData = _tdata.debugTxRx[ix1] = new DebugTxRx(); //only initially.
        //Arrays.fill(debugData.posTelgItems, -1);  
        debugData.clearAll();
    }

    commPort.registerTargetAccessor(this);
    states = new States(threadEvents);	
    stateIdle = states.getState(States.StateIdle.class);
    stateWaitAnswer = states.getState(States.StateWaitReceive.class);
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
	
	
  /**Adds any program snippet which is executed while preparing the telegram for data request from target.
   * After execution the order will be removed.
   * @param order the program snippet.
   */
  public void addUserTxOrder(Runnable order)
  {
    userTxOrders.add(order);
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
  { if(evFill.occupy(null, false)) {
      states.processEvent(evFill);    //set state to fill
    } else; //don't send the event, it is in queue already from last prepareTelg invocation.
    if(!states.isInState(States.StateFilling.class)){
      stop();
    }
    if(_tdata.txixFill >= _tdata.tx.length ){ //check more as one datagram
      System.err.println("InspcTargetAccessor - Too many telegram requests;");
      return false;
    }
    if(bFillTelg && (txAccess.getLengthTotal() + lengthNewInfo) > _tdata.tx[_tdata.txixFill].buffer.length){
      //the next requested info does not fit in the current telg. 
      //Therefore send the telg but only if the other telgs are received already!
      completeDatagram(false);
      if((_tdata.txixFill) >= _tdata.tx.length){
        return false;   //not enough space for communication. 
      }
    }
    if(!bFillTelg){
      //assert(tx[ixTxFill].stateOfTxTelg.compareAndSet(0, 'f'));
      //prepare a new telegram to send with ByteDataAccess, new head.
      Arrays.fill(_tdata.tx[_tdata.txixFill].buffer, (byte)0);
      if(_tdata.bDebugTelg) {
        txAccess = new InspcDataExchangeAccess.InspcDatagram();
        _tdata.debugTxRx[_tdata.txixFill].txTelgHead = txAccess;   //register in debug.
      } else {
        txAccess = _tdata.accTxTelgStatic;
      }
      txAccess.assignClear(_tdata.tx[_tdata.txixFill].buffer);
      if(++_tdata.nSeqNumber == 0){ _tdata.nSeqNumber = 1; }
      txAccess.setHeadRequest(nEntrant, _tdata.nSeqNumber, nEncryption);
      bFillTelg = true;
      if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - Prepare head; " + _tdata.txixFill + "; seq=" + _tdata.nSeqNumber);
      int nRestBytes = _tdata.tx[_tdata.txixFill].buffer.length - txAccess.getLengthHead();
      return true;
    }
    int lengthDatagram = txAccess.getLengthTotal();
    
    int nRestBytes = _tdata.tx[_tdata.txixFill].buffer.length - lengthDatagram;
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
	
	
	/**Checks readiness of communication cycle. Returns true if a communication cycle is not pending (finished).
	 * It is in state {@link States.StateIdle} or {@link States.StateFilling}
	 * Returns false if not all answer telegrams were received from the last request.
	 * It is in all other states of {@link States}. 
	 * If the time of the last send request was before timeLastTxWaitFor 
	 * then it is assumed that the communication was faulty. Therefore all older pending requests are removed.
	 * @param timeCurrent The current time. It is compared with the time of the last transmit telegram which's answer is expected,
	 *   and the timeout. If the timeout is expired, older requests are removed and this routine returns true. 
	 */
	public boolean isOrSetReady(long timeCurrent){ 
	  boolean bReady;
	  if(!bTaskPending.get()){
	    bReady = !getFieldsData.bGetFieldsPending; //true;
	  }
	  else {
	    long timeLastTxWaitFor = timeCurrent - (long)(1000 * cycle_timeout[1]);
	    long dtimeExpired = timeLastTxWaitFor - timeSend;  //timeExpired is the timeLast - wait time 
	    if(  timeSend ==0  //a telegram was never sent, faulty bTaskPending 
	      || dtimeExpired >=0 && !bRunInRxThread
	      ) { //not ready for a longer time: 
	      //forgot a pending request:
          //if(logTelg !=null) 
        System.err.println("InspcTargetAccessor.isReady - recover after timeout target; " + toString());
        bRequestWhileTaskPending = false;
        synchronized(this){
          Debugutil.stop();  //possibility to wait in debug
	      }
        setReady();
	      bReady = true;  //is setted ready.
	    } else {
	      if(!bRequestWhileTaskPending){ //it is the first one
	        bRequestWhileTaskPending = true;
	        if(logTelg !=null) System.err.println("InspcTargetAccessor.isReady - not ready for requests; " + toString());
	      }
	      bReady = false; //not ready
	    }
	  }
	  if(bReady) {
	  }
	  return bReady;
	}
	
	
	/**Sets the communication cycle of ready (idle) state,
	 * called either inside {@link #isOrSetReady(long)} on time expired or if the last rx telegram was processed successfully.
	 */
	private void setReady()
	{
    for(int ixRx = 0; ixRx < _tdata.rxNrofDatagramsForOneSend; ++ixRx) {
      _tdata.rxDatagram [ixRx] = null;  //garbage it.
    }
    _tdata.rxNrofDatagramsForOneSend = 0;
    _tdata.rxBitsAnswerNr = _tdata.rxBitsAnswerMask = 0;
    state = 'R';
    _tdata.txixSend = 0;
    _tdata.txixFill = 0;   //if the last one was received, the tx-buffer is free for new requests. 
    _tdata.nSeqNumberTxRx = 0;
    getFieldsData.bGetFieldsPending = false;
    bFillTelg = false;
    bShouldSend = false;
    bIsSentTelg = false;
    //bSendPending.set(false);
    ordersExpected.clear();  //after long waiting their is not any expected.
    for(DebugTxRx debugData1: _tdata.debugTxRx) {
      debugData1.clearAll();
    }
    bTaskPending.set(false);
	}
	
	
	
  /**Sets an expected order in the index of orders and registers a callback after all telegrams was received if given.
   * @param order The unique order number.
   * @param exec The execution for the answer. 
   */
  public void setExpectedOrder(int order, InspcAccessExecRxOrder_ifc actionOnRx)
  {
    if(actionOnRx !=null){
      OrderWithTime timedOrder = new OrderWithTime(order, System.currentTimeMillis(), actionOnRx);
      //listTimedOrders.addFirst(timedOrder);
      ordersExpected.put(new Integer(order), timedOrder);
      Runnable action = actionOnRx.callbackOnAnswer();
      if(action !=null){
        callbacksOnAnswer.put(new Integer(action.hashCode()), action);  //register the same action only one time.
      }
    }
  }
  
  
  
  
  /**Set the request for all fields of the given variable. This method is invoked from outer (GUI) 
   * to force {@link #cmdGetFields(String, InspcAccessExecRxOrder_ifc)} in the inspector thread.
   * @param data The variable
   * @param rxActionGetFields Action on gotten fields
   * @param runOnReceive Action should be run if all fields are received, if all datagrams are received for that communication cycle.
   */
  public void requestFields(InspcTargetAccessData data, InspcAccessExecRxOrder_ifc rxActionGetFields, Runnable runOnReceive){
    getFieldsData.rxActionGetFields = rxActionGetFields;
    getFieldsData.runOnResponseFields = runOnReceive;
    getFieldsData.requFields = data;  //this is the atomic signal to execute cmdGetFields
  }

  

  InspcTelgInfoSet newTxitem(){
    if(_tdata.bDebugTelg) {
      InspcTelgInfoSet item = new InspcTelgInfoSet();
      //register it in the debug struct
      _tdata.debugTxRx[_tdata.txixFill].txItems.add(item);
      return item;
    } else {
      _tdata.accRxItemStatic.detach();  //if it was used.
      return _tdata.accTxItemStatic;
    }
  }
  

  /**Adds the info block to send 'get fields by path'
   * @param sPathInTarget The path in the target with the target specific rules.
   * @param actionOnRx this action will be executed on receiving the item.
   * @return The order number. 0 if the cmd can't be created.
   */
  @Override public int cmdGetFields(String sPathInTarget, InspcAccessExecRxOrder_ifc actionOnRx)
  { int order;
    //states.applyEvent(evFill);
    //if(states.isInState(stateFilling))
      stop();
    int lengthItem = InspcTelgInfoSet.lengthCmdGetFields(sPathInTarget.length());
    if(prepareTelg(lengthItem)) {
      //InspcTelgInfoSet infoGetValue = new InspcTelgInfoSet();
      InspcTelgInfoSet infoAccess = newTxitem();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdGetFields(sPathInTarget, order);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg+idLogGetFields, "add cmdGetFields %s, order = %d", sPathInTarget, new Integer(order)); 
      }
      getFieldsData.orderGetFields = new OrderWithTime(order, System.currentTimeMillis(), actionOnRx);
      
      //setExpectedOrder(order, actionOnRx);
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
  public int cmdGetValueByPath(String sPathInTarget, InspcAccessExecRxOrder_ifc actionOnRx)
  { int order = 5;
    int lengthItem = InspcTelgInfoSet.lengthCmdGetValueByPath(sPathInTarget.length());
    if(prepareTelg(lengthItem)) {
      //InspcTelgInfoSet infoGetValue = new InspcTelgInfoSet();
      InspcTelgInfoSet infoAccess = newTxitem();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdGetValueByPath(sPathInTarget, order);
      if(sPathInTarget.equals("_DSP_.ccs_1P.ccs_IB_priv.ictrl.pire_p.out.YD.")) {
        System.out.println("InspcTargetAccessor.cmdGetValueByPath - check, " +  sPathInTarget + ", " + infoAccess.toString());
        ///Debugutil.stop();
      }  

      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg+idLogGetValueByPath, "add cmdGetValueByPath %s, order = %d", sPathInTarget, new Integer(order)); 
      }
      if(sPathInTarget.contains("xWCP_1"))
        Debugutil.stop();
      setExpectedOrder(order, actionOnRx);
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
  public int cmdRegisterHandle(String sPathInTarget, InspcAccessExecRxOrder_ifc actionOnRx)
  { final int order;
    final int zPath = sPathInTarget.length();
    final int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    prepareTelg(InspcDataExchangeAccess.Inspcitem.sizeofHead + zPath + restChars);
    order = orderGenerator.getNewOrder();
    setExpectedOrder(order, actionOnRx);
    InspcTelgInfoSet infoAccess = newTxitem();
    txAccess.addChild(infoAccess);  
    infoAccess.addChildString(sPathInTarget);
    if(restChars >0) { infoAccess.addChildInteger(restChars, 0); }
    final int zInfo = infoAccess.getLength();
    infoAccess.setInfoHead(zInfo, InspcDataExchangeAccess.Inspcitem.kRegisterHandle, order);
    //
    if(logTelg !=null){ 
      logTelg.sendMsg(identLogTelg+idLogRegisterByPath, "add registerByPath %s, order = %d", sPathInTarget, new Integer(order)); 
    }
    return order;
  }
  
  
  
  /**Adds the info block to send 'get value by ident'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created because the telegram is full.
   */
  @Override public boolean cmdGetValueByHandle(int ident, InspcAccessExecRxOrder_ifc action)
  { if(ixIdent5GetValueByIdent >= actionRx4GetValueByHandle.length){
      return false;
      //  txCmdGetValueByIdent();
    }
    actionRx4GetValueByHandle[ixIdent5GetValueByIdent] = action;
    accInfoDataGetValueByIdent.addChildInteger(4, ident);
    ixIdent5GetValueByIdent +=1;
    return true;
  }
  
  
  /**
   * @return true if the telegram is sent.
   */
  boolean txCmdGetValueByIdent() {
    if(ixIdent5GetValueByIdent > 0 && isOrSetReady(System.currentTimeMillis())){
      int lengthInfo = accInfoDataGetValueByIdent.getLength();
      //It prepares the telg head.
      if(prepareTelg(lengthInfo)){  //It finishes an existing telegram if there is not enough space for the idents-info
        int order = 0xabcdef01; //not neccessary. //orderGenerator.getNewOrder();
        //setExpectedOrder(order, actionRx4ValueByIdent);
        InspcTelgInfoSet infoAccess = newTxitem();
        txAccess.addChild(infoAccess);
        int posInTelg = infoAccess.getPositionInBuffer() + InspcDataExchangeAccess.Inspcitem.sizeofHead;
        System.arraycopy(dataInfoDataGetValueByIdent, 0, infoAccess.getData(), posInTelg, 4 * ixIdent5GetValueByIdent);
        infoAccess.setInfoHead(lengthInfo + InspcDataExchangeAccess.Inspcitem.sizeofHead, InspcDataExchangeAccess.Inspcitem.kGetValueByHandle, order);
        ixIdent5GetValueByIdent = 0;
        accInfoDataGetValueByIdent.assignClear(dataInfoDataGetValueByIdent);
        return true;
      } else {
        return false; //any problem, tegegram can't be sent.
      }
    } else return false; //no data
  }
  
  
  
  
  final void execRx4ValueByHandle(InspcDataExchangeAccess.Inspcitem inspcitem, long time, LogMessage log, int identLog){
    //int lenInfo = info.getLength();
    InspcDataExchangeAccess.InspcAnswerValueByHandle answerItem = new InspcDataExchangeAccess.InspcAnswerValueByHandle(inspcitem);
    final int ixValStart = answerItem.getIxHandleFrom();  //first index of variable in this answer item
    final int ixValTo = answerItem.getIxHandleTo();  //first index of variable in this answer item
    int ixVal = ixValStart;  //the suggested index of the action, proper to tx
    while(ixVal <= ixValTo){  //at least one byte in info, 
      InspcAccessExecRxOrder_ifc action = actionRx4GetValueByHandle[ixVal];
      ixVal +=1;
      if(action !=null){
        action.execInspcRxOrder(answerItem, time, log, identLog);
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
  @Override public void cmdSetValueByPath(String sPathInTarget, long value, int typeofValue, InspcAccessExecRxOrder_ifc actionOnRx)
  { int order;
    int zPath = sPathInTarget.length();
    int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    int zInfo = InspcDataExchangeAccess.Inspcitem.sizeofHead + InspcDataExchangeAccess.InspcSetValue.sizeofElement + zPath + restChars; 
    if(prepareTelg(zInfo )){
      InspcTelgInfoSet infoAccess = newTxitem();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      //infoAccess.setCmdSetValueByPath(sPathInTarget, value, typeofValue, order);
      InspcDataExchangeAccess.InspcSetValue accessSetValue = new InspcDataExchangeAccess.InspcSetValue(); 
      infoAccess.addChild(accessSetValue);
      accessSetValue.setLong(value);
      infoAccess.addChildString(sPathInTarget);
      if(restChars >0) { infoAccess.addChildInteger(restChars, 0); }
      assert(infoAccess.getLength() == zInfo);  //check length after add children. 
      infoAccess.setInfoHead(zInfo, InspcDataExchangeAccess.Inspcitem.kSetValueByPath, order);
      setExpectedOrder(order, actionOnRx);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg+idLogSetValueByPath, "add cmdSetValueByPath %s, order = %d, value=%08X, type=%d", sPathInTarget, new Integer(order), new Long(value), new Integer(typeofValue)); 
      }
    } else {
      //too much info blocks
      order = 0;
    }
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
      order = 0; cmdSetValueByPath(sPathInTarget, value, typeofValue, exec);    
      if(order !=0){ //save the order to the action. It is taken on receive.
        this.setExpectedOrder(order, exec);
      } else {
        //XXXsendAndAwaitAnswer();  //calls execInspcRxOrder as callback.
        sent = true;
      }
    } while(order == 0);  
    return sent;
  }
  
  
  @Override public void cmdSetValueByPath(VariableAccessArray_ifc var, String value){
    assert(var instanceof InspcVariable);
    InspcVariable var1 = (InspcVariable)var;
    String valueTrimmed = value.trim();
    assert(var1.ds.targetAccessor == this);
    try{
        switch(var.getType()){
          case 'D':
          case 'F': {
            double val = Double.parseDouble(valueTrimmed); 
            cmdSetValueByPath(var1.ds.sPathInTarget, val, null); 
          } break;
          case 'S':
          case 'B':
          case 'I': {
            int val;
            if(valueTrimmed.startsWith("0x")){
              val = Integer.parseInt(valueTrimmed.substring(2),16); 
            } else {
              val = Integer.parseInt(valueTrimmed); 
            }
            cmdSetValueByPath(var1.ds.sPathInTarget, val, null); 
          } break;
          case 's': {  //empty yet
            
          } break;
        }
      } catch(Exception exc){
        //usual number format exception
        System.err.println("InspcTargetAccessor.cmdSetValueByPath - exception" + exc.getMessage() + "; ");
      }
 
  }
  

  
  
  /**Adds the info block to send 'set value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created because the telgram is full.
   */
  public int cmdSetValueByPath(String sPathInTarget, int value)
  { int order;
    int lengthItem = InspcTelgInfoSet.lengthCmdSetValueByPath(sPathInTarget.length());
    if(prepareTelg(lengthItem)) {
      InspcTelgInfoSet infoAccess = newTxitem();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdSetValueByPath(sPathInTarget, value, order);
    } else {
      //too much info blocks
      order = 0;
    }
    return order;
  }
  
  
  /**Adds the info block to send 'set value by path'
   * @param sPathInTarget
   * @param value The value as int value, 32 bit
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created because the telgram is full.
   */
  public void cmdSetValueByPath(String sPathInTarget, int value, InspcAccessExecRxOrder_ifc actionOnRx)
  { int order;
    int lengthItem = InspcTelgInfoSet.lengthCmdSetValueByPath(sPathInTarget.length());
    if(prepareTelg(lengthItem)) {
      InspcTelgInfoSet infoAccess = newTxitem();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdSetValueByPath(sPathInTarget, value, order);
      setExpectedOrder(order, actionOnRx);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg+idLogSetValueByPath, "add cmdSetValueByPath %s, order = %d, value=%d", sPathInTarget, new Integer(order), new Integer(value)); 
      }
    }   
  }
  
  
  /**Adds the info block to send 'set value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created because the telgram is full.
   */
  public void cmdSetValueByPath(String sPathInTarget, float value, InspcAccessExecRxOrder_ifc actionOnRx)
  { int order;
    int lengthItem = InspcTelgInfoSet.lengthCmdSetValueByPath(sPathInTarget.length());
    if(prepareTelg(lengthItem)) {
      InspcTelgInfoSet infoAccess = newTxitem();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdSetValueByPath(sPathInTarget, value, order);
      setExpectedOrder(order, actionOnRx);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg+idLogSetValueByPath, "add cmdSetValueByPath %s, order = %d, value=%f", sPathInTarget, new Integer(order), new Float(value)); 
      }
    }   
  }
  
  
  /**Adds the info block to send 'set value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public void cmdSetValueByPath(String sPathInTarget, double value, InspcAccessExecRxOrder_ifc actionOnRx)
  { int order;
    int lengthItem = InspcTelgInfoSet.lengthCmdSetValueByPath(sPathInTarget.length());
    if(prepareTelg(lengthItem)) {
      InspcTelgInfoSet infoAccess = newTxitem();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdSetValueByPath(sPathInTarget, value, order);
      setExpectedOrder(order, actionOnRx);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg+idLogSetValueByPath, "add cmdSetValueByPath %s, order = %d, value=%f", sPathInTarget, new Integer(order), new Double(value)); 
      }
    }
  }
  
  
  /**Adds the info block to send 'get address by path'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created.
   * @since 2014-04-28 new form
   */
  @Override public boolean cmdGetAddressByPath(String sPathInTarget, InspcAccessExecRxOrder_ifc actionOnRx)
  { int order;
    int lengthItem = InspcTelgInfoSet.lengthCmdGetAddressByPath(sPathInTarget.length());
    if(prepareTelg(lengthItem)) {
      //InspcTelgInfoSet infoGetValue = new InspcTelgInfoSet();
      InspcTelgInfoSet infoAccess = newTxitem();
      txAccess.addChild(infoAccess);
      order = orderGenerator.getNewOrder();
      infoAccess.setCmdGetAddressByPath(sPathInTarget, order);
      setExpectedOrder(order, actionOnRx);
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg + idLogGetAddress, "add cmdGetAddressByPath %s, order = %d", sPathInTarget, new Integer(order)); 
      }
    } else {
      //too much telegrams
      throw new IllegalArgumentException("InspcTargetAccessor - too much telegrams;");
    }
    return true;
  }
  
  
  
  /**This routine have to be called after the last cmd in one thread. It sends the last telg
   * or initialized the send for previous telgs if the answer is not gotten yet.
   * @return true if anything to send, false if no cmd was given.
   */
  public boolean cmdFinit(){
    states.processEvent(evSend);
    if(!bTaskPending.get()) {
      txCmdGetValueByIdent();
      if(bFillTelg || _tdata.txixFill >0){
        if(bFillTelg) {
          completeDatagram(true);
        }
        bTaskPending.set(true);
        send();   //send the first telegramm now
        return true;
      }
      else return false;
    } 
    else return false;  //nothing to do
  }
  
  
  public void setStateToUser(InspcPlugUser_ifc user) {
    if(user !=null){
      InspcPlugUser_ifc.TargetState stateUser;
      if(bTaskPending.get()){ stateUser = InspcPlugUser_ifc.TargetState.waitReceive; }
      else { stateUser = InspcPlugUser_ifc.TargetState.idle; }
      user.showStateInfo(name, stateUser, _tdata.nSeqNumber, cycle_timeout);
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
  
  
  
  /**Completes this datagram with all head information.
   */
  private void completeDatagram(boolean lastTelg){
    assert(bFillTelg);
    int lengthDatagram = txAccess.getLength();
    if(_tdata.txixFill < _tdata.tx.length) {
      assert(lengthDatagram <= _tdata.tx[_tdata.txixFill].buffer.length);
      _tdata.tx[_tdata.txixFill].nrofBytesTelg = lengthDatagram;
      txAccess.setLengthDatagram(lengthDatagram);
      _tdata.tx[_tdata.txixFill].nSeq = _tdata.nSeqNumber;
      _tdata.tx[_tdata.txixFill].lastTelg = lastTelg;
      bFillTelg = false;
      state = 's';
      if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - complete Datagram; " + lastTelg + "; " + _tdata.txixFill + "; seqnr " + _tdata.nSeqNumber);
      _tdata.txixFill +=1;
    }
  }
  
  
  
  
	/**Sends the prepared telegram.
	 * @return
	 */
  private void send()
  {
    //send the telegram:
    //checkerRxTelg.setAwait(nSeqNumber);  
    if(name.equals(dbgNameStopTx)) {
      Debugutil.stop();
    }
    timeSend = System.currentTimeMillis();
    _tdata.nSeqNumberTxRx = _tdata.tx[_tdata.txixSend].nSeq;
    //bSendPending = true;
    bIsSentTelg = true;
    int lengthDatagram = _tdata.tx[_tdata.txixSend].nrofBytesTelg;
    //tx[ixTxSend].nrofBytesTelg = -1; //is sent
    if(lengthDatagram >0) {
      int ok = commPort.send(this, _tdata.tx[_tdata.txixSend].buffer, lengthDatagram);
      synchronized(this){
        if(ok == 96)
          Debugutil.stop();
      }
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg +idLogTx, "send telg ix=%d, length= %d, ok = %d, seqn=%d", new Integer(_tdata.txixSend), new Integer(lengthDatagram)
        , new Integer(ok), new Integer(_tdata.nSeqNumberTxRx)); 
      }
    } else {
      Debugutil.stop();
      System.out.println("InspcTargetAccessor.send() - target does not response, too many telegr, " + targetAddr);
    }
    bShouldSend = false; 
  }
	
	
	
  /**This routine is called from the received thread if a telegram with the sender of this target
   * was received.
   * <br><br>
   * A communication cycle consists of maybe more as one answer for one request telegram
   * and maybe more as one request telegram. This routine checks whether all answers for the pending
   * request telegram were received. Only if all telegrams where received (especially the last one)
   * then the maybe next request telegram is sent.
   * <br><br>
   * If all telegrams of the communication cycle are received, the flag {@link #bTaskPending} is set to false 
   * and the {@link #isReady(long)} method will return true.
   * <br><br>
   * If any answer is missed, especially because the communication is disturbed or the device is faulty,
   * then the {@link #isReady(long)} returns false till the given timeout has expired.
   * @param rxBuffer
   * @param rxLength
   */
  public void evaluateRxTelg(byte[] rxBuffer, int rxLength){
    bRunInRxThread = true;
    try{ 
      timeReceive = System.currentTimeMillis();
      dtimeReceive = timeReceive - timeSend;       
      if(logTelg !=null){ 
        logTelg.sendMsg(identLogTelg+idLogRx, "recv telg after %d ms", new Long(dtimeReceive)); 
      }
      long time = System.currentTimeMillis();
      final InspcDataExchangeAccess.InspcDatagram accessRxTelg = new InspcDataExchangeAccess.InspcDatagram();
      accessRxTelg.assign(rxBuffer, rxLength);
      int rxSeqnr = accessRxTelg.getSeqnr();
      //int rxAnswerNr = accessRxTelg.getAnswerNr();
      if(rxSeqnr == this._tdata.nSeqNumberTxRx){
        //the correct answer
        int nAnswer = accessRxTelg.getAnswerNr() - 1;  
        if(nAnswer == -1) { //has sent 0, old Target
          nAnswer = 0;
          _tdata.oldTarget = true;  //target has sent 0, old target, store for ever.
        } else if(_tdata.oldTarget) {
          nAnswer = (nAnswer +1) >>1;  //add 1 because it starts with 0, shift because 2. telegram has 2 etc.
        }
        int bitAnswer = 1 << (nAnswer & 0x3f);
        if((_tdata.rxBitsAnswerNr & bitAnswer) ==0) { //this answer is not processed yet?
          //a new answer, not processed
          _tdata.rxBitsAnswerNr |= bitAnswer;
          //
          _tdata.rxDatagram[nAnswer] = accessRxTelg;  //store the telegram to evaluate in the inspector thread. 
          //
          if(accessRxTelg.lastAnswer()) {
            _tdata.rxNrofDatagramsForOneSend = nAnswer +1;  //signal for evaluating in InspcMng-thread
            _tdata.rxBitsAnswerMask = (1 << _tdata.rxNrofDatagramsForOneSend) -1;  //for all this bits an anwer should be received.
            if(_tdata.rxBitsAnswerMask != _tdata.rxBitsAnswerNr) {
              Debugutil.stop();  //last telegram received before antecedent ones.
            }
            //All received telegrams for this send telegram will be evaluated together if bitsAnswerMask == bitsAnswerNrRx
            //ttt states.processEvent(evLastAnswer);
            if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - Rcv last answer; " + rxSeqnr);
            if(logTelg !=null){ 
              logTelg.sendMsg(identLogTelg+idLogRxLast, "recv ok last telg seqn=%d nAnswer=%d after %d ms", new Integer(rxSeqnr), new Integer(nAnswer), new Long(dtimeReceive)); 
            }
            /*ttt
            if((debugRxTelgIx +=1) > debugRxTelg.length){
              debugRxTelgIx = debugRxTelg.length -1;   //prevent exception for too many telegs.
              System.err.println("InspcTargetAccessor - too many telg seq");
            }
            // 
            txNextAfterRcvOrSetReady();  //see isReady
            */
            //
          } else {
            //sendPending: It is not the last answer, remain true
            if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - Rcv answer; " + nAnswer + "; seqn=" + rxSeqnr);
            if(logTelg !=null){ 
              logTelg.sendMsg(identLogTelg+idLogRx, "recv ok not last telg seqn=%d nAnswer=%d after %d ms", new Integer(rxSeqnr), new Integer(nAnswer), new Long(dtimeReceive)); 
            }
          }
        } else {
          //duplicate answer for this sequence:
          if(logTelg !=null || bWriteDebugSystemOut) System.out.println("InspcTargetAccessor - faulty answer in sequence, received= " + nAnswer + ", expected=0x" + Long.toHexString(_tdata.rxBitsAnswerNr));
          //faulty seqnr
          if(logTelg !=null){ 
            logTelg.sendMsg(identLogTelg+idLogRxRepeat, "recv repeated telg seqn=%d nAnswer=%d after %d ms", new Integer(rxSeqnr), new Integer(nAnswer), new Long(dtimeReceive)); 
          }
        }
      } else {
        //faulty seqnr ?sendPending: is false, unexpected rx
        if(logTelg !=null || bWriteDebugSystemOut) System.out.println("InspcTargetAccessor - unexpected seqnr, received=" + rxSeqnr + ", expected=" + _tdata.nSeqNumberTxRx);
        if(logTelg !=null){ 
          logTelg.sendMsg(identLogTelg+idLogFailedSeq, "recv failed seqn=%d after %d ms", new Integer(rxSeqnr), new Long(dtimeReceive)); 
        }
  
      }
    } finally {
      bRunInRxThread = false;
    }
  }
  
  
  
  /**This routine is invoked cyclically in the inspector thread. It checks whether telegrams are received.
   * 
   */
  public void evaluateRxTelgInspcThread(){
    long time = System.currentTimeMillis();
    if(_tdata.rxBitsAnswerNr >0 && _tdata.rxBitsAnswerNr == _tdata.rxBitsAnswerMask) { //only somewhat to do if received telegrams exists.
      //all telegrams from one send telegram are received. Usual it is only one telegram.
      try { 
        for(int ixRx = 0; ixRx < _tdata.rxNrofDatagramsForOneSend; ++ixRx) {
          DebugTxRx dbgRx = _tdata.bDebugTelg ? _tdata.debugTxRx [_tdata.txixSend] : null;
          evaluateOneDatagram(_tdata.rxDatagram[ixRx], null, time, logTelg, identLogTelg +idLogRxItem, dbgRx, ixRx);  ////
          //evaluateRx1TelgInspcThread(rxDatagram[ixRx]);
        }
      } catch(Exception exc) {
        CharSequence text = Assert.exceptionInfo("InspcTargetAccessor - Exception while evaluating ", exc, 0, 20);
        System.err.append(text);
      }
      states.processEvent(evLastAnswer);
      // 
      //
      //after evaluating the answer, send a next telegram for this communication cycle:
      //check whether it is the last telegram:
      //
      //boolean wasLastTxTelg = tlg.tx[tlg.ixTxSend].lastTelg;  //the ixTxSend is the index of send still.
      for(int ixRx = 0; ixRx < _tdata.rxNrofDatagramsForOneSend; ++ixRx) {
        _tdata.rxDatagram [ixRx] = null;  //garbage it.
      }
      //debugData ... not cleared here, cleared for txData lastly.
      _tdata.rxNrofDatagramsForOneSend = 0;
      _tdata.rxBitsAnswerNr = _tdata.rxBitsAnswerMask = 0;
      if(++_tdata.txixSend < _tdata.txixFill){
        if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - Send next telg; seqnr=" + _tdata.tx[_tdata.txixSend].nSeq + "; ixTxSend= " + _tdata.txixSend);
        //Note: sendPending remain set. For this next telegram.
        send();
      } else { //if(wasLastTxTelg){  //last is reached.
        lastTelg();  //action after receiving the last telegram from the last send telegram.
                     //invoke callbacksOnAnswer if stored. 
        setReady();    
        //Now new send telegrams can be prepared. Firstly it would check whether there are 
        bRequestWhileTaskPending = false;
        if(logTelg !=null && bWriteDebugSystemOut) System.out.println("InspcTargetAccessor.Test - All received; ");
      }  
    }
  }

  
  /**This routine checks whether the communication is in its tx request gathering state: {@link #isOrSetReady(long)}
   * returns true. Then given {@link #addUserTxOrder(Runnable)} are invoked, which fills the tx telegram usual.
   * This routine is only invoked in the {@link org.vishia.inspcPV.mng.InspcMng#procComm()} routine cyclically for any target.
   * It is not intent to invoke by an application.
   * 
   */
  public void checkExecuteSendUserOrder(){
    if(isOrSetReady(System.currentTimeMillis())) {
      Runnable userTxOrder;
      if(getFieldsData.requFields !=null){
        System.out.println("InspcTargetAccessor - send getFields;");
        String path = this.getFieldsData.requFields.sPathInTarget; //getTargetFromPath(this.requestedFields.path()); 
        //InspcTargetAccessor targetAccessor = requestedFields.targetAccessor();
        callbacksOnAnswer.put(new Integer(getFieldsData.runOnResponseFields.hashCode()), getFieldsData.runOnResponseFields);  //register the same action only one time.
        cmdGetFields(path, this.getFieldsData.rxActionGetFields);
        getFieldsData.bGetFieldsPending = true;  //prevents request some more, isOrSetReady returns false after them.
        this.getFieldsData.requFields = null;
      }
      //
      while( (userTxOrder = userTxOrders.poll()) !=null){
        userTxOrder.run(); //maybe add some more requests to the current telegram.
      }
    }    
  }
  
  
  
  
  
  
	/**Waits for answer from the target.
	 * This method can be called in any users thread. Typically it is the thread, which has send the telegram
	 * to the target. 
	 * 
	 * @param timeout for waiting.
	 * @return null on timeout, the answer datagrams elsewhere.
	 * @since 2014-04-28 commented as deprecated
	 * @deprecated The communication handles several requests. It is not proper that one await is programmed.
	 *   The answer will be gotten for any information unit in the telegrams. 
	 *   See {@link InspcAccessExecRxOrder_ifc}-argument of any {@link #cmdGetValueByPath(String, InspcAccessExecRxOrder_ifc)}...
	 *   routine.
	 */
	@Deprecated
  public InspcDataExchangeAccess.InspcDatagram[] awaitAnswer(int timeout)
	{ //InspcDataExchangeAccess.ReflDatagram[] answerTelgs = checkerRxTelg.waitForAnswer(timeout); 
  	long time = System.currentTimeMillis();
    dtimeWeakup = time - timeSend;
    return null; //answerTelgs;
  }
	
	
	
	
  
  /**Clean up the order list.
   * @param timeOld The time before that the orders are old. 
   * @return positive: number of removed orders. negative: number of found orders, whereby nothing is removed. 
   *   Usual 0 if no orders are pending. If >0, some orders are wrong, if <0, the communication is slow.
   */
  public int checkAndRemoveOldOrders(long timeOld)
  {
    int foundOrders = 0;
    int removedOrders = 0;
    boolean bRepeatBecauseModification;
    do{
      bRepeatBecauseModification = false;
      //if(ordersExpected.size() > 10){
        Set<Map.Entry<Integer, OrderWithTime>> list = ordersExpected.entrySet();
        try{
          Iterator<Map.Entry<Integer, OrderWithTime>> iter = list.iterator();
          while(iter.hasNext()){
            foundOrders +=1;
            Map.Entry<Integer, OrderWithTime> entry = iter.next();
            OrderWithTime timedOrder = entry.getValue();
            if(timedOrder.time < timeOld){
              iter.remove();
              removedOrders +=1;
            }
          }
        } catch(Exception exc){ //Repeat it, the list ist modified
          bRepeatBecauseModification = true;
        }
      //}
    } while(bRepeatBecauseModification);
      
    return removedOrders > 0 ? removedOrders : -foundOrders;
  }
  
  
  
  /**Evaluates a received telegram.
   * @param telgHead The telegram
   * @param executer if given, than the {@link InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem)}
   *        -method is called for any info block.<br>
   *        If null, then the order is searched like given with {@link #setExpectedOrder(int, InspcAccessExecRxOrder_ifc)}
   *        and that special routine is executed.
   * @return null if no error, if not null then it is an error description. 
   * @since 2015-08 it does not return an error but it writes to System.err on failure. It continues if possible thow an item may be faulty.
   */
  public void evaluateOneDatagram(InspcDataExchangeAccess.InspcDatagram telgHead
      , InspcAccessExecRxOrder_ifc executer, long time, LogMessage log, int identLog, DebugTxRx dbgRx, int dbgixAnswer)
  { String sError = null;
    //int currentPos = InspcDataExchangeAccess.InspcDatagram.sizeofHead;
    //for(InspcDataExchangeAccess.ReflDatagram telgHead: telgHeads){
    int nrofBytesDatagramReceived = telgHead.getLengthTotal();
    int nrofBytesDatagramInHead = telgHead.getLengthDatagram();
    if(dbgRx !=null) {
      dbgRx.nrofBytesDatagramInHead = nrofBytesDatagramInHead;          
      dbgRx.nrofBytesDatagramReceived = nrofBytesDatagramReceived;
      assert(dbgRx.nrofBytesDatagramInHead == dbgRx.nrofBytesDatagramReceived); 
      dbgRx.rxTelgHead[dbgixAnswer] = telgHead;
    }
    if(dbgRx.nrofBytesDatagramInHead != dbgRx.nrofBytesDatagramReceived) {
      System.err.println("ERROR InscTargetAccessor.evaluateOneDatagram - mismatch between nrofBytesDatagramReceived, nrofBytesDatagramInHead, " + nrofBytesDatagramReceived+ " != " + nrofBytesDatagramInHead);
      return;
    }
    //int nrofBytesTelg = telgHead.getLength();  //length from ByteDataAccess-management.
    //telgHead.assertNotExpandable();
    while(sError == null && telgHead.sufficingBytesForNextChild(InspcDataExchangeAccess.Inspcitem.sizeofHead)){
      ////
      final InspcDataExchangeAccess.Inspcitem infoAccessRx;
      if(dbgRx ==null) {
        _tdata.accRxItemStatic.detach();
        infoAccessRx = _tdata.accRxItemStatic;
      } else {
        infoAccessRx = new InspcDataExchangeAccess.Inspcitem();
        dbgRx.rxItems[dbgixAnswer].add(infoAccessRx); 
      }
      telgHead.addChild(infoAccessRx);
      int nrofBytesInfo = infoAccessRx.getLenInfo();
      int posInData = infoAccessRx.getPositionInBuffer();
      if(!infoAccessRx.checkLengthElement(nrofBytesInfo)) {
        System.err.println("ERROR InscTargetAccessor.evaluateOneDatagram - mismatch between nrofBytesInfo, nrofBytesDatagramInHead, " + nrofBytesDatagramReceived+ " != " + nrofBytesDatagramInHead);
        return;
      }
      if((posInData + nrofBytesInfo) > nrofBytesDatagramInHead) {
        System.err.println("ERROR InscTargetAccessor.evaluateOneDatagram - mismatch between nrofBytesInfo, nrofBytesDatagramInHead, " + nrofBytesDatagramReceived+ " != " + nrofBytesDatagramInHead);
        return;
      }
      infoAccessRx.setLengthElement(nrofBytesInfo);  //if an exception throws or evaluation of infoAccess is aborted, is known length.
      if(executer !=null){
        //deprecated and unused.
        executer.execInspcRxOrder(infoAccessRx, time, log, identLog);
      } else {
        //that is correct:
        int cmd = infoAccessRx.getCmd();
        if(cmd == InspcDataExchangeAccess.Inspcitem.kAnswerFieldMethod){
          //special case: The same order number is used for more items in the same sequence number.
          //callback the proper request for get fileds to any InspcVariable.
          getFieldsData.orderGetFields.exec.execInspcRxOrder(infoAccessRx, time, log, identLog);
        }
        else if(cmd == InspcDataExchangeAccess.Inspcitem.kAnswerValueByHandle || cmd == InspcDataExchangeAccess.Inspcitem.kFailedHandle){
          ////
          execRx4ValueByHandle(infoAccessRx, time, log, identLog);
        }
        else {
          //
          int order = infoAccessRx.getOrder();
          OrderWithTime timedOrder = ordersExpected.remove(order);
          if(timedOrder !=null){
            //remove timed order
            InspcAccessExecRxOrder_ifc orderExec = timedOrder.exec;
            if(orderExec !=null){
              orderExec.execInspcRxOrder(infoAccessRx, time, log, identLog);
            } else {
              stop();  //should not 
            }
          }
        }
        //
        //search the order whether it is expected:
      }
      infoAccessRx.setLengthElement(nrofBytesInfo);
      //telgHead.setLengthCurrentChildElement(nrofBytesInfo); //to add the next.
      //currentPos += nrofBytesInfo;  //the same as stored in telgHead-access
      
    }
    //return sError;
  }
  
  
  
  public static float valueFloatFromRxValue(InspcDataExchangeAccess.Inspcitem info, int type)
  {
    float ret = 0;
    if(type >= InspcDataExchangeAccess.kScalarTypes){
      switch(type - InspcDataExchangeAccess.kScalarTypes){
        case org.vishia.reflect.ClassJc.REFLECTION_char16: ret = (char)info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_char8: ret = (char)info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_double: ret = (float)info.getChildDouble(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_float: ret = info.getChildFloat(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int8: ret = info.getChildInteger(-1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int16: ret = info.getChildInteger(-2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int32: ret = info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int64: ret = info.getChildInteger(-8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int: ret = info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint8: ret = info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint16: ret = info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint32: ret = info.getChildInteger(4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint64: ret = info.getChildInteger(8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint: ret = info.getChildInteger(4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_boolean: ret = info.getChildInteger(1) == 0 ? 0.0f: 1.0f; break;
      }      
    } else if(type <= InspcDataExchangeAccess.maxNrOfChars){
      try{
        String sValue = info.getChildString(type);
        ret = Float.parseFloat(sValue);
      } catch(Exception exc){ ret = 0; }
    }

    return ret;
  }
  
  
  
  

  public static int valueIntFromRxValue(InspcDataExchangeAccess.Inspcitem info, int type)
  {
    int ret = 0;
    if(type >= InspcDataExchangeAccess.kScalarTypes){
      switch(type - InspcDataExchangeAccess.kScalarTypes){
        case ClassJc.REFLECTION_char16: ret = (char)info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_char8:  ret = (char)info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_double: ret = (int)info.getChildDouble(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_float:  ret = (int)info.getChildFloat(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int8:   ret = (int)info.getChildInteger(-1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int16:  ret = (int)info.getChildInteger(-2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int32:  ret = (int)info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int64:  ret = (int)info.getChildInteger(-8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int:    ret = (int)info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint8:  ret = (int)info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint16: ret = (int)info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint32: ret = (int)info.getChildInteger(4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint64: ret = (int)info.getChildInteger(8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint:   ret = (int)info.getChildInteger(4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_boolean:ret = info.getChildInteger(1) == 0 ? 0: 1; break;
      }      
    } else if(type == InspcDataExchangeAccess.kReferenceAddr){
      ret = (int)info.getChildInteger(4);
    } else if(type <= InspcDataExchangeAccess.maxNrOfChars){
      try{
        String sValue = info.getChildString(type);
        ret = Integer.parseInt(sValue);
      } catch(Exception exc){ ret = 0; }
    }

    return ret;
  }
  
  
  public static String valueStringFromRxValue(InspcDataExchangeAccess.Inspcitem info, int nBytesString)
  {
    String value = null; 
    value = info.getChildString(nBytesString);
    return value;  
  }
  
  
  
  /**Gets the reflection type of the received information.
   * 
   * @param info
   * @return The known character Z, C, D, F, B, S, I, J for the scalar types, 'c' for character array (String)
   */
  public static short getInspcTypeFromRxValue(InspcDataExchangeAccess.Inspcitem info)
  {
    char ret = 0;
    short type = (short)info.getChildInt(1);
    return type;
  }
    
    /**Gets the type of the received information.
   * 
   * @param info
   * @return The known character Z, C, D, F, B, S, I, J for the scalar types, 'c' for character array (String)
   */
  public static char getTypeFromInspcType(int type)
  {
    final char ret;
    if(type >= InspcDataExchangeAccess.kScalarTypes){
      switch(type - InspcDataExchangeAccess.kScalarTypes){
        case ClassJc.REFLECTION_char16: ret = 'S'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_char8:  ret = 'C'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_double: ret = 'D'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_float:  ret = 'F'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int8:   ret = 'B'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int16:  ret = 'S'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int32:  ret = 'I'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int64:  ret = 'J'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int:    ret = 'I'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint8:  ret = 'B'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint16: ret = 'S'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint32: ret = 'I'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint64: ret = 'J'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint:   ret = 'I'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_boolean:ret = 'Z'; break;
        default: ret = '?';
      }      
    } else if(type == InspcDataExchangeAccess.kReferenceAddr){
      ret = 'I';
    } else if(type == InspcDataExchangeAccess.kLengthAndString){
        ret = 'c';
    } else if(type <= InspcDataExchangeAccess.maxNrOfChars){
        ret = 'c';
    } else {
      ret = '?'; //error
    }
    return ret;
  }
  
  
  public int getStateInfo() {
    if(stateIdle.isInState()) return 1;
    else if(stateWaitAnswer.isInState()) return 2;
    else return 0;
  }
  
  
  /**Executes after the last answer telegram of this sequence was received. It checks all entries 
   * in {@link #callbacksOnAnswer} and executes it.
   * 
   */
  void lastTelg(){
    Set<Map.Entry<Integer, Runnable>> list = callbacksOnAnswer.entrySet();
    try{
      Iterator<Map.Entry<Integer, Runnable>> iter = list.iterator();
      while(iter.hasNext()){
        Runnable run = iter.next().getValue();
        run.run();   
      }
    } catch(Exception exc){
      System.err.println("InspcTargetAccessor - lastTelg wrong callback; " + exc.getMessage());
    }
    getFieldsData.bGetFieldsPending = false;  //maybe was true.
    callbacksOnAnswer.clear();
  }
  
  

	
	
	
	@Override public String toString(){
	  
	  return name + ": " + targetAddr.toString() + ":" + state;
	}
	
	@Override public InspcTargetAccessData getTargetAccessFromPath(String sDataPath, boolean strict)
  { throw new RuntimeException("only valid for the manager.");
  }

  
  
  InspcAccessExecRxOrder_ifc XXXactionRx4ValueByIdent = new InspcAccessExecRxOrder_ifc(){
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Inspcitem info, long time, LogMessage log, int identLog)
    { execRx4ValueByHandle(info, time, log, identLog);
    }
    @Override public Runnable callbackOnAnswer(){return null; }  //empty
  };
	
	void stop(){}


}
