package org.vishia.inspcPC.mng;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.byteData.VariableAccessArray_ifc;
import org.vishia.byteData.VariableAccessWithBitmask;
import org.vishia.byteData.VariableAccess_ifc;
import org.vishia.byteData.VariableContainer_ifc;
import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessComm_SocketImpl;
import org.vishia.event.EventTimerThread;
import org.vishia.inspcPC.InspcAccessExecRxOrder_ifc;
import org.vishia.inspcPC.InspcAccess_ifc;
import org.vishia.inspcPC.InspcPlugUser_ifc;
import org.vishia.inspcPC.InspcRxOk;
import org.vishia.inspcPC.InspcTargetAccessData;
import org.vishia.inspcPC.accTarget.InspcCommPort;
import org.vishia.inspcPC.accTarget.InspcTargetAccessor;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.reflect.ClassJc;
import org.vishia.util.Assert;
import org.vishia.util.CompleteConstructionAndStart;
import org.vishia.util.ReplaceAlias_ifc;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringFunctions_C;
import org.vishia.util.ThreadRun;

/**This class supports the communication via the inspector for example with reflection access. 
 * <img src="../../../../img/InspcMng.png"><br>Object model diagram
 * <br><br>
 * This class is a {@link VariableContainer_ifc}. It means the application can handle with variables, which's values
 * are present in a target devices accessed via reflex access.
 * <br><br>
 * A Variable can be gotten via {@link VariableContainer_ifc#getVariable(String)} using this instance.
 * To request an actual value for a variable {@link VariableAccess_ifc#requestValue()} 
 * or {@link VariableAccess_ifc#requestValue(long, Runnable)} shoud be called. Therewith the time stamp 
 * is written in the variable (see {@link InspcVariable#timeRequested})
 * and that variable is requested via its {@link InspcVariable#ds} from the associated
 * target device.
 * <br><br>
 * This class supports free communication with Inspector reflex access outside the {@link VariableAccess_ifc}
 * thinking too. Especially via {@link #addUserOrder(Runnable)} some code snippet can be placed in the
 * communication thread of this class.
 * <br><br>
 * This class starts an own thread {@link #startupThreads()} for the send requests to the target device. 
 * That thread opens the
 * inspector reflex access communication via {@link InspcTargetAccessor#open(String)} which may use an
 * {@link org.vishia.communication.InterProcessComm} instance.
 * <br><br>
 * The references to all known variables of the user are hold in an indexed list {@link #idxAllVars} sorted by name.
 * The variable reference of type {@link VariableAccess_ifc} stores the access path to the target if necessary and the 
 * actual value. The actual value may be read from a target device or not in the last time. A variable can be registered
 * to the manager calling {@link #getVariable(String)}. That routine build the correct instance for the variable access,
 * that is either {@link InspcVariable} or an {@link org.vishia.reflect.FieldJcVariableAccess} for java-internal variables.
 * The variable will be wrapped in an {@link org.vishia.byteData.VariableAccessWithIdx}. The variable knows the 
 * {@link InspcTargetAccessor} and can get values from the target.
 * <br><br>
 * <b>Communication principle</b>:
 * The {@link #threadReqFromTarget} calls method {@link #procComm()} cyclically,
 * if the communication was opened till {@link #close()} is called. In this loop all requested variables
 * were handled with a request-value telegram and all {@link #addUserOrder(Runnable)} are processed.
 * <br><br>
 * In the {@link #procComm()} all known variables from {@link #idxAllVars} are processed, see {@link #getVariable(String)}. 
 * But only if a value from a variable was requested in the last time, a new value is requested from a target device.
 * With that requests one send telegram is built. If the telegram is filled, it will be send.
 * Then the answer of the target device is awaiting, see {@link InspcTargetAccessor#sendAndAwaitAnswer()}. 
 * In this time the loop is blocked till a timeout is occurred while waiting of the embedded device's answer.
 * Normally the embedded device or other target should answer in a few milliseconds.
 * <br><br>
 * The receiving of telegrams is executing in an extra receive Thread, see {@link InspcTargetAccessor#receiveThread}.
 * The receive thread handles receiving from the one port of communication, which is opened with the 
 * {@link InterProcessComm} instance which usual is a {@link InterProcessComm_SocketImpl}. 
 * It is possible to send from more as this thread, or it is possible to receive some special telegrams which 
 * are not requested. That does the receive thread.
 * <br><br>
 * The receive thread knows the sequence numbers of a sent telegram, which is response by an answer telegram.
 * This sequence numbers are TODO
 * 
 *  
 * The {@link InspcTargetAccessor} supports the execution of any action in the received thread too,
 * but this class calls {@link InspcTargetAccessor#awaitAnswer(int)} in its send thread to force notifying
 * of this class if the correct answer telegram is received.  
 * <br>
 * TODO it seems better to execute the answer in the receive thread, because some user requests can be executed
 * in that kind too. It isn't necessary to call a {@link InspcAccessEvaluatorRxTelg#evaluate(org.vishia.communication.InspcDataExchangeAccess.Datagram[], InspcAccessExecRxOrder_ifc)}
 * in the sending thread. But the sending thread should be inform about completely receiving and execution
 * before the next telegram will be send to the same device. That is because the device should not be 
 * flood and trash with too many telegrams. A embedded target may have a limited IP stack!
 * Sending a next telegram to the same device without an answer should be taken only after a suitable
 * timeout. But another device can be requested in that time. 
 * <br>
 * But the receiving of telegrams is executing in an extra receive Thread, see {@link InspcTargetAccessor#receiveThread}.
 * The {@link InspcTargetAccessor} supports the execution of any action in the received thread too,
 * but this class calls {@link InspcTargetAccessor#awaitAnswer(int)} in its send thread to force notifying
 * of this class if the correct answer telegram is received.  
 * <br>
 * If the telegram is received, all variables are filled with the received values and the  
 * @author Hartmut Schorrig
 *
 */
public class InspcMng implements CompleteConstructionAndStart, VariableContainer_ifc, Closeable, InspcAccess_ifc
{

  /**Version, history and license.
   * <ul>
   * <li>2015-06-21 Hartmut new. invokes {@link InspcTargetAccessor#setStateToUser(InspcPlugUser_ifc)}. 
   * <li>2015-06-02 Hartmut The addUserOrder(Runnable) method is removed from here. It is replaced by 
   *   the {@link InspcTargetAccessor#addUserTxOrder(Runnable)}. Any target has its own timing. Only the target
   *   access can determine when to send to a target. Some gardening furthermore.
   * <li>2015-03-20 Hartmut requestFields redesigned. now managed by the {@link InspcTargetAccessor} 
   * <li>2013-01-10 Hartmut bugfix: If a variable can't be requested in {@link #requestValueByPath(String, InspcAccessExecRxOrder_ifc)} because
   *   the telegram is full, the same variable should be requested repeatedly in the next telegram. It was forgotten.
   * <li>2012-06-09 Hartmut new: Now it knows java-internal variable too, the path for the argument of {@link #getVariable(String)}
   *   should start with "java:" then the variable is searched internally. TODO now it isn't tested well, the start instance
   *   should be given by constructor, because it should be either the start instance of the whole application or a special
   *   data area.
   * <li>2012-04-17 Hartmut new: {@link #bUseGetValueByHandle}: Access via getValuePerPath for downward compatibility with target device.
   * <li>2012-04-08 Hartmut new: Support of GetValueByIdent, catch of some exception, 
   * <li>2012-04-05 Hartmut new: Use {@link LogMessage to test telegram trafic}
   * <li>2012-04-02 Hartmut all functionality from org.vishia.guiInspc.InspcGuiComm now here,
   *   the org.vishia.guiInspc.InspcGuiComm is deleted now.
   * <li>2012-03-31 Hartmut created. Most of code are gotten from org.vishia.guiInspc.InspcGuiComm,
   *   which has experience since about 1 year. But the concept is changed. This source uses
   *   the {@link VariableAccess_ifc} concept which has experience in the org.vishia.guiViewCfg package.
   *   Both concepts to access variable are merged yet. The GUI to show values from a target system
   *   now has only one interface concept to access values, independent of their communication concept.
   *   This class supports the communication via the inspector reflex access. 
   * <li>2012-03-31 next versions from older org.vishia.guiInspc.InspcGuiComm:  
   * <li>2011-06-30 Hartmut new: nolink: sendAndPrepareCmdSetValueByPath(String, long, int, InspcAccessExecRxOrder_ifc)
   *   2014-04 new #send
   *     It is the first method which organizes that info blocks can be created one after another
   *     without regarding the telegram length. It simplifies the usage.
   * <li>2011-06-30 Hartmut improved: {@link WidgetCommAction#execInspcRxOrder(Info)} for formatted output   
   * <li>2011-05-17 execInspcRxOrder() int32AngleDegree etc. to present a angle in degrees
   * <li>2011-05-01 Hartmut: Created
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
   * 
   */
  public static final int version = 0x20131211;

  
  /**The request of values from the target is organized in a cyclic thread. The thread sends
   * the request and await the answer.
   */
  private final ThreadRun threadReqFromTarget;
  
  
  /**Thread which manages timers and creates time events. */
  //final EventTimerMng threadTimer = new EventTimerMng("timerEv");
  
  /**Thread which manages the queue of all events of state machines. */
  final EventTimerThread threadEvent = new EventTimerThread("events");
  
  private final InspcPlugUser_ifc user;

  ReplaceAlias_ifc replacerAlias;
  
  /**This method is called if variable are received.
   * 
   */
  Runnable callbackOnRxData;
  
  
  Runnable XXXcallbackShowingTargetCommState;
  
  /**If true then writes a log of all send and received telegrams. */
  LogMessage logTelg;
  
  boolean bWriteDebugSystemOut;  //if(logTelg !=null && bWriteDebugSystemOut)
  
  int identLogTelg;
  
  /**This container holds all variables which are created. */
  Map<String, InspcVariable> idxAllVars = new TreeMap<String, InspcVariable>();
  
  
  /**This container holds all structures which are created. */
  Map<String, InspcStruct> idxAllStruct = new TreeMap<String, InspcStruct>();
  
  /**Own address string for the communication. */
  private final String sOwnIpcAddr;
  
  /**The target ipc-address for Interprocess-Communication with the target.
   * It is a string, which determines the kind of communication.
   * For example "UDP:0.0.0.0:60099" to create a socket port for UDP-communication.
   */
  private final Map<String, String> indexTargetIpcAddr;

  
  private final Map<String, InspcTargetAccessor> indexTargetAccessor = new TreeMap<String, InspcTargetAccessor>();
  
  private final List<InspcTargetAccessor> listTargetAccessor = new ArrayList<InspcTargetAccessor>();
  
  private Map<String, String> indexFaultDevice;
  
  private ConcurrentLinkedQueue<InspcCmdStore> cmdQueue = new ConcurrentLinkedQueue<>();
  
  boolean retryDisabledVariable;

  long millisecTimeoutOrders = 5000;
  
  long timeLastRemoveOrders;

  
  String sIpTarget;
  
  boolean bUseGetValueByHandle;

  boolean bUserCalled;

  protected final InspcCommPort commPort; 
  

  
  /**Set from {@link #variableIsReceived(InspcVariable)} if all variables are received.
   * Tested from {@link #inspcThread} whether all are received.
   */
  boolean bAllReceived;
  
  public InspcMng(String sOwnIpcAddr, Map<String, String> indexTargetIpcAddr, boolean bUseGetValueByIndex, InspcPlugUser_ifc user){
    this.threadReqFromTarget = new ThreadRun("InspcMng", step, 100);
    this.commPort = new InspcCommPort();  //maybe more as one
    //maybe more as one
    //this.inspcAccessor = new InspcTargetAccessor(commPort, new InspcAccessEvaluatorRxTelg());
    this.indexTargetIpcAddr = indexTargetIpcAddr;
    this.sOwnIpcAddr = sOwnIpcAddr;
    this.bUseGetValueByHandle = bUseGetValueByIndex;
    this.user = user;
    if(user !=null){
      user.setInspcComm(this);
    }

  }
  
  
  @Override public void completeConstruction(){}
  
  
  public void complete_ReplaceAlias_ifc(ReplaceAlias_ifc replacerAliasArg){ this.replacerAlias = replacerAliasArg; }
  
  /* (non-Javadoc)
   * @see org.vishia.util.CompleteConstructionAndStart#startupThreads()
   */
  @Override public void startupThreads(){
    threadReqFromTarget.start();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) { }
  }

  public void setmodeRetryDisabledVariables(boolean retry){ retryDisabledVariable = retry; }
  
  public void setmodeGetValueByIndex(boolean byIndex){ bUseGetValueByHandle = byIndex; }
  
  
  @Override public void setCallbackOnReceivedData(Runnable callback){
    this.callbackOnRxData = callback;
  }
  
  
  
  public void XXXsetCallbackShowingState(Runnable callback) {
    this.XXXcallbackShowingTargetCommState = callback;
  }

  /*
  @Override public VariableAccess_ifc getVariable(final String nameP, int[] index)
  { int posIndex = nameP.indexOf('[');
    final String name;
    if(posIndex > 0){
      name = nameP.substring(posIndex);
      try{ 
        int posIndex2 = nameP.indexOf(']');
        String sIndex = nameP.substring(posIndex +1, posIndex2); 
        int nIndex = Integer.parseInt(sIndex);
        index[0] = nIndex;
      } catch(Exception exc){
        System.err.println("InspcMng.getVariable-fault Index; " + name);
      }
    } else {
      name = nameP;
    }
    InspcVariable var = idxAllVars.get(name);
    if(var == null){
      var = new InspcVariable(this, name);
      idxAllVars.put(name, var);
    }
    return var;
  }
   */
  
  
  
  /**Enables or disables the logging of communication activity.
   * @param log null then log is off. Any log output.
   * @param ident base identification for the messages.
   */
  public void setLogForTargetComm(LogMessage log, int ident){
    this.logTelg = log;
    this.identLogTelg = ident;
    for(InspcTargetAccessor target: listTargetAccessor){
      target.setLog(log, ident);
    }
  }
  
  
  
  /* (non-Javadoc)
   * @see org.vishia.byteData.VariableContainer_ifc#getVariable(java.lang.String)
   */
  @Override public VariableAccess_ifc getVariable(final String sDataPath)
  { int posIndex = sDataPath.lastIndexOf('.');
  final String sDataPathOfWidget;
  char cc;
  int mask, bit;
  if(posIndex > 0 && sDataPath.length() > posIndex+1 && (cc = sDataPath.charAt(posIndex +1)) >='0' && cc <='9'){
    //it is a bit designation:
    if(sDataPath.charAt(posIndex -1) =='.'){ //a double .. for bit space like 12..8
      int posIndex1 = sDataPath.lastIndexOf('.', posIndex -2);
      int bitStart = StringFunctions_C.parseIntRadixBack(sDataPath, posIndex1-1, posIndex1-1, 10, null);
      int[] zParsed = new int[1];
      int bitEnd = StringFunctions_C.parseIntRadix(sDataPath, posIndex+1, 2, 10, zParsed);
      posIndex = posIndex1 - zParsed[0];
      if(bitStart >= bitEnd){
        bit = bitEnd;
        mask = (1 << (bitStart - bitEnd +1)) -1;
      } else {
        bit = bitStart;
        mask = (1 << (bitEnd - bitStart +1)) -1;
      }
    } else {
      bit = StringFunctions_C.parseIntRadix(sDataPath, posIndex+1, 2, 10, null);
      mask = 1;
    }
    sDataPathOfWidget = sDataPath.substring(0, posIndex);
  } else {
    mask = -1;
    bit = 0;
    sDataPathOfWidget = sDataPath;
  }
  InspcVariable var = getVariable(sDataPathOfWidget, 0);
  if(mask == -1){ return var; }
  else { return new VariableAccessWithBitmask(var, bit, mask); }
}
  
  
  private InspcVariable getVariable(final String sDataPath, int recurs)
  { if(recurs > 100) throw new IllegalArgumentException("too many recursion");
    InspcVariable var = idxAllVars.get(sDataPath);
    if(var == null){
      InspcTargetAccessData acc = getTargetAccessFromPath(sDataPath, false);
      //InspcVarPathStructAcc path1 = getTargetFromPath(sDataPathOfWidget);
      if(acc !=null && acc.targetAccessor !=null){
        InspcVariable parent = acc.sParentPath ==null ? null : getVariable(acc.sParentPath, recurs +1);
        var = new InspcVariable(this, parent, acc);
        idxAllVars.put(sDataPath, var);
      } else {
        System.err.println("InspcMng - Variable target unknown; " + sDataPath); 
        //TODO use dummy to prevent new requesting
      }
    }
    return var;
  }
 
  
  /**Returns a variable which's access to the target is established.
   * Returns null if either the path is faulty (faulty target ident) or the communication was not established in the given time.
   * If a variable is returned, the data path is correct in the target. The last current value can be gotten via
   * {@link VariableAccess_ifc#getFloat()} etc. If a new value is need, invoke {@link VariableAccess_ifc#requestValue()}.
   * 
   * @param sDataPath Path to the target:data.path.
   * @param maxtimeSeconds waiting time for establishing the target communication.
   * @return A living variable or null.
   */
  public VariableAccess_ifc accVariable(final String sDataPath, int maxtimeMilliseconds)
  {
    VariableAccess_ifc var = getVariable(sDataPath);
    if(var == null) return null;
    long timeStart = System.currentTimeMillis();
    long time = timeStart - 3000; //firstly request
    do {
      long time1 = System.currentTimeMillis();
      if(time1 - time > 2000) {   //request newly after longer time.
        var.requestValue(time1);
        time = time1;
      }
      try { Thread.sleep(200); } catch (InterruptedException e) { }
    } while(!var.isRefreshed() && (time - timeStart) < maxtimeMilliseconds);  //check in cycle of 200 ms, the request will be send in 100 ms-cycle.
    if( (time - timeStart) >= maxtimeMilliseconds) return null;  //communication was not established
    else return var;
  }
  
  public InspcRxOk create_InspcRxOk(){ return new InspcRxOk(); }
  
  
  
  /**This routine requests all values from its target devices, 
   * for the variables which were requested itself after last call of refresh.
   * The answer of the target-request will invoke 
   * {@link InspcVariable.VariableRxAction#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem, long, LogMessage, int)}
   * with the info block of the telegram for each variable.
   * If all variables are received, the callback routine 
   * @returns true if at least one variable was requested, false if nothing is requested.
   * @see org.vishia.byteData.VariableContainer_ifc#refreshValues()
   */
  protected void procComm(){
    boolean bRequest = false;
    bUserCalled = false;
    //System.out.println("InspcMng.ProcComm - step;");
    long timeCurr = System.currentTimeMillis();
    //check received telegrams
    for(InspcTargetAccessor inspcAccessor: listTargetAccessor){
      //evaluate all targets generally independent of the state of receiving 
      //because several targets have its own behavior.
      inspcAccessor.evaluateRxTelgInspcThread(); //it sets isReady()
      
    }
    /*
    if(callbackShowingTargetCommState !=null) {
      callbackShowingTargetCommState.run();
    }
    */
    if(callbackOnRxData !=null){
      callbackOnRxData.run();         //show the received values.
      //In the callback the next requests for variables will be set to show in the GUI.
      //It may be the same, it may be other.
    }
    //
    //
    //All received telegrams may be evaluated, this is the only one position where a target communication may be in idle state
    //because after them some new variables were requested.
    //Therefore show the state here:
    for(InspcTargetAccessor inspcAccessor: listTargetAccessor){
      //show the state to the user plug ifc.
      inspcAccessor.setStateToUser(user);
    }
    //
    //Assemble the transmit telegrams for requesting or set values.
    //
    //first check requesting queues.
    //
    InspcCmdStore cmd;
    while( (cmd = cmdQueue.poll()) !=null) {
      cmd.exec();
    }
    //
    //Some requests may be stored in the targetAccessors:
    //
    for(InspcTargetAccessor inspcAccessor: listTargetAccessor){
      //invokes userTxOrders and cmdGetFields but only if isOrSetReady() of this target.
      inspcAccessor.checkExecuteSendUserOrder(); //invokes getFields if requested.     
    }
    //
    //Check all variables, some maybe requested. Then send the request value:
    //
    int nrofVarsReq = 0;
    @SuppressWarnings("unused") int nrofVarsAll = 0;
    for(Map.Entry<String,InspcVariable> entryVar: idxAllVars.entrySet()){ //check all variables of the system.
      VariableAccess_ifc var = entryVar.getValue();
      nrofVarsAll +=1;
      if(var instanceof InspcVariable){
        InspcVariable varInspc = (InspcVariable)var;
        if(varInspc.ds.sPathInTarget.equals("this$0.inspcMng"))
          Assert.stop();
        if(   var.isRequestedValue(retryDisabledVariable) ){              //but handle only variable which are requested
          nrofVarsReq +=1;
          int dtimeRequested = (int)(timeCurr - varInspc.timeRequested);
          if(varInspc.ds.sDataPath.equals("CCS:_DSP_.ccs_1P.ccs_IB_priv.ictrl.pire_p.out.YD")) {
            System.out.println("InspcMng.procComm - test request InspcVar, "  + dtimeRequested/1000.0f + ", "+  varInspc.ds.sDataPath);
            ///Debugutil.stop();
          }  
          if(dtimeRequested > 10000){
            System.out.println("InspcMng.procComm - remove old request - device may not ready, " + varInspc.ds.sDataPath);
            //The variable is not able to get, remove the request.
            //The request will be repeat if the variable is newly requested.
            var.requestValue(0);  //old request set to 0
          }
          if(varInspc.ds.sPathInTarget.startsWith("#"))
            Assert.stop();
          bRequest = true;
          if(varInspc.ds.targetAccessor.isOrSetReady(timeCurr)){ //check whether the device is ready.
            if(varInspc.ds.sDataPath.equals("CCS:_DSP_.ccs_1P.ccs_IB_priv.ictrl.pire_p.out.YD")) {
              System.out.println("InspcMng.procComm - exec request InspcVar, "  + dtimeRequested/1000.0f + ", "+  varInspc.ds.sDataPath);
              ///Debugutil.stop();
            }  
            varInspc.requestValueFromTarget(timeCurr, retryDisabledVariable);
          } else {
            //It is usual that the target is slower than the request. Let the request active.
            //The request will be removed if it is older than the expired time. 
            //it is strong faulty: var.requestValue(0, null);   //remove this request.
          }
          
        }
      }
    }
    if(nrofVarsReq >0){
      //System.out.println("InspcMng.procComm - variables requested; " + nrofVarsReq + "; all=" + nrofVarsAll);
    }
    
    for(InspcTargetAccessor inspcAccessor: listTargetAccessor){
      inspcAccessor.cmdFinit();  //finit tx telegrams and send the first if in this state.     
    }
    if(user !=null){
      user.isSent(0);
    }
    
    long time = System.currentTimeMillis();
    if(time >= timeLastRemoveOrders + millisecTimeoutOrders){
      timeLastRemoveOrders = time;
      for(InspcTargetAccessor inspcAccessor: listTargetAccessor){
        int removedOrders = inspcAccessor.checkAndRemoveOldOrders(time - timeLastRemoveOrders);
        if(removedOrders >0){
          System.err.println("InspcMng - Communication problem, removed Orders; " + removedOrders);
        }
      }
    }
  }




  
  
  
  
  
  /**Requests the value from the target.
   * @param sDataPath
   * @param commAction
   * @return true then the telegram has not space. The value is not requested. It should be repeated.
  boolean requestValueByPath(String sDataPath, InspcAccessExecRxOrder_ifc commAction){
    boolean bRepeattheRequest = false;
    int posSepDevice = sDataPath.indexOf(':');
    if(posSepDevice >0){
      String sDevice = sDataPath.substring(0, posSepDevice);
      String sIpTargetNew = translateDeviceToAddrIp(sDevice); ///
      if(sIpTargetNew == null){
        errorDevice(sDevice);
      } else {
        if(sIpTarget == null){
          sIpTarget = sIpTargetNew;
          inspcAccessor.setTargetAddr(sIpTarget);
        }
      }
      sDataPath = sDataPath.substring(posSepDevice +1);
    }
    //
    if(sIpTarget !=null){
      if(user !=null && !bUserCalled){  //call only one time per procComm()
        user.requData(0);
        bUserCalled = true;
      }
      //
      //create the send command to target.
      int order = inspcAccessor.cmdGetValueByPath(sDataPath);    
      if(order !=0){
        //save the order to the action. It is taken on receive.
        inspcAccessor.setExpectedOrder(order, commAction);
        bRepeattheRequest = true;
      } else {
        inspcAccessor.sendAndAwaitAnswer();  //calls execInspcRxOrder as callback.
        //sent = true;
      } 
    }    
    return bRepeattheRequest;
  }
   */
  

  
  /**Prepares the info block to register a variable on the target device.
   * @param sDataPath The data path on target
   * @param actionOnRx receiving action, executing with the response info.
  InspcTargetAccessor registerByPath(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx){
    int posSepDevice = sDataPath.indexOf(':');
    if(posSepDevice >0){
      String sDevice = sDataPath.substring(0, posSepDevice);
      String sIpTargetNew = translateDeviceToAddrIp(sDevice); ///
      if(sIpTargetNew == null){
        errorDevice(sDevice);
      } else {
        if(sIpTarget == null){
          sIpTarget = sIpTargetNew;
          inspcAccessor.setTargetAddr(sIpTarget);
        }
      }
      sDataPath = sDataPath.substring(posSepDevice +1);
    }
    //
    if(sIpTarget !=null){
      if(user !=null && !bUserCalled){  //call only one time per procComm()
        user.requData(0);
        bUserCalled = true;
      }
      //
      //create the send command to target.
      inspcAccessor.cmdRegisterByPath(sDataPath, actionOnRx);    
    }    
    return inspcAccessor;
  }
   */
  

  
  
  /**Splits a given full data path with device:datapath maybe with alias:datapath in the device, path, name and returns a struct. 
   * It uses {@link #indexTargetAccessor} to get the target accessor instance.
   * It uses {@link #idxAllStruct} to get the existing {@link InspcStruct} for the variable
   *  
   * @param sDataPath The user given data path maybe with alias, necessary with target.
   *   An alias is written in form "alias:rest.of.path". A device is written "device:rest.of.path".
   *   The distinction between alias and device is done with checking whether the charsequence before :
   *   is detected as alias.
   * @param strict true then throws an error on faulty device, if false then returns null if faulty. 
   * @return structure which contains the device, path, name.
   */
  public InspcTargetAccessData getTargetAccessFromPath(String sDataPath, boolean strict){
    final InspcTargetAccessor accessor;
    final String sPathInTarget;
    final String sName;
    
    String pathRepl = replacerAlias == null ? sDataPath : replacerAlias.replaceDataPathPrefix(sDataPath);
    String sPathWithTarget = pathRepl !=null ? pathRepl : sDataPath;    //with or without replacement.
    int posSepDevice = sPathWithTarget.indexOf(':');
    if(posSepDevice >0){
      String sDevice = sPathWithTarget.substring(0, posSepDevice);
      accessor = indexTargetAccessor.get(sDevice);
      if(accessor == null){
        errorDevice(sDevice);
      }
      int posName = sPathWithTarget.lastIndexOf('.');
      String sStructPath;
      if(posName >0){
        sStructPath = sPathWithTarget.substring(/*posSepDevice +1*/ 0, posName);
        sName = sPathWithTarget.substring(posName +1);
      } else if(sPathWithTarget.length() > posSepDevice+1) {
        sStructPath = sPathWithTarget.substring(0, posSepDevice+1);
        sName = sPathWithTarget.substring(posSepDevice +1);
      } else {
        sStructPath = null;  //no parent. 
        sName = "";          //selects the root in the target.
      }
      sPathInTarget = sPathWithTarget.substring(posSepDevice +1);
      return new InspcTargetAccessData(accessor, sDataPath, sPathInTarget, sStructPath, sName);
    } else {
      if(strict) throw new IllegalArgumentException("path should have the form \"device:internalPath\" or \"alias:subpath\", given: " + sDataPath);
      else return null;
    }

  }
  

  
  public int getStateOfTargetComm(int ixTarget) {
    if(ixTarget < listTargetAccessor.size()) {
      InspcTargetAccessor target = listTargetAccessor.get(ixTarget);
      return target.getStateInfo();
    } else return 0;
  }
  
  
  
  

  
   
  public String translateDeviceToAddrIp(String sDevice)
  {
    String ret = indexTargetIpcAddr.get(sDevice);
    return ret;
  }
  
  void errorDevice(String sDevice){
    if(indexFaultDevice ==null){ indexFaultDevice = new TreeMap<String, String>(); }
    if(indexFaultDevice.get(sDevice) == null){
      //write the error message only one time!
      indexFaultDevice.put(sDevice, sDevice);
      System.err.println("InspcMng - errorDevice; unknown device key: " + sDevice);
    }
  }
  
  
  
  
  
  
  /**This routine will be invoked if all data are received from the target.
   * @param ev
   */
  void callbackOnRxData(EventObject ev){
    
  }
  

  void openComm(){
    commPort.open(sOwnIpcAddr);
    for(Map.Entry<String, String> e : indexTargetIpcAddr.entrySet()){
      String name = e.getKey();
      Address_InterProcessComm addrTarget = commPort.createTargetAddr(e.getValue());
      InspcTargetAccessor accessor = new InspcTargetAccessor(name, commPort, addrTarget, threadEvent);
      indexTargetAccessor.put(name, accessor);
      listTargetAccessor.add(accessor);
    }
  }
  
  
  
  private final ThreadRun.Step step = new ThreadRun.Step(){
    
  
    @Override public final int start(int cycletime){
      openComm();
      return -1;
    }

    
    
    /**This routine is the thread routine of {@link #inspcThread} called in run one time.
     * It runs in a loop till {@link #bThreadRuns} is set to false.
     * <ul>
     * <li>It waits some milliseconds, 
     * <li>then requests values from target
     * <li>the waits for receiving all values or for a maximal time.
     * <li>then calls {@link #callbackOnRxData(EventCmdPingPongType)} to show the values.
     * <li>then loops.
     * </ul>
     */
    @Override public final int step(int cycletime, int cycletimelast, int calctimelast, long timesecondsAbs){
      //bThreadRuns = true;
      bAllReceived = false;
        InspcMng.this.procComm();  //Core-Communication-method.
        if(!bAllReceived){
          stop();
        }
        //timeReceived = System.currentTimeMillis();  //all requests after this time calls new variables.
      return -1; //!bAllReceived;
    }//step
  }; //step  
  
  @Override public void close() throws IOException
  { //bThreadRuns = false;
    while(cmdQueue.size()>0){
      try { Thread.sleep(500); } catch (InterruptedException e) { }
    }
    try { Thread.sleep(1500); } catch (InterruptedException e) { }
    threadReqFromTarget.close();
    commPort.close();
    threadEvent.close();
  }
  
  void stop(){}
  
  @Override
  public int cmdGetFields(String sPathInTarget,
      InspcAccessExecRxOrder_ifc actionOnRx)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override
  public boolean cmdGetValueByIdent(int ident,
      InspcAccessExecRxOrder_ifc actionOnRx)
  {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public int cmdGetValueByPath(String sPathInTarget,
      InspcAccessExecRxOrder_ifc actionOnRx)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override
  public int cmdRegisterByPath(String sPathInTarget,
      InspcAccessExecRxOrder_ifc actionOnRx)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override
  public boolean cmdGetAddressByPath(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx)
  { InspcTargetAccessData acc = getTargetAccessFromPath(sDataPath, true);
    acc.targetAccessor.cmdGetAddressByPath(acc.sPathInTarget, actionOnRx);
    return true;
  }


  @Override
  public void cmdSetValueByPath(String sDataPath, long value, int typeofValue, InspcAccessExecRxOrder_ifc actionOnRx)
  { //check which thread: The inspector thread itself:
    if(false) {
      InspcTargetAccessData acc = getTargetAccessFromPath(sDataPath, true);
      acc.targetAccessor.cmdSetValueByPath(acc.sPathInTarget, value, typeofValue, actionOnRx);
    } else {
      InspcCmdStore cmd = new InspcCmdStore(this);
      cmd.cmdSetValueByPath(sDataPath, value, typeofValue, actionOnRx);
      cmdQueue.offer(cmd);
    
    }
  }


  
  public void cmdSetInt32ByPath(String sDataPath, int value, InspcAccessExecRxOrder_ifc actionOnRx) 
  { cmdSetValueByPath(sDataPath, value, InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_int32, actionOnRx);
  }
  
  public void cmdSetFloatByPath(String sDataPath, float value, InspcAccessExecRxOrder_ifc actionOnRx) 
  { //NOTE: The target need a double, old compatibility necessary!
    cmdSetValueByPath(sDataPath, Double.doubleToRawLongBits(value), InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_double, actionOnRx);
  }
  
  
  public void cmdSetDoubleByPath(String sDataPath, double value, InspcAccessExecRxOrder_ifc actionOnRx) 
  { cmdSetValueByPath(sDataPath, Double.doubleToRawLongBits(value), InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_double, actionOnRx);
  }
  
  
  /**Command to set a value. It is queued and executed in the tx thread.
   * @see org.vishia.inspcPC.InspcAccess_ifc#cmdSetValueByPath(java.lang.String, int)
   */
  @Override
  public int cmdSetValueByPath(String sDataPath, int value)
  { InspcCmdStore cmd = new InspcCmdStore(this);
    cmd.cmdSetValueByPath(sDataPath, value);
    cmdQueue.offer(cmd);
    return 0;
  }


  @Override public void cmdSetValueByPath(VariableAccessArray_ifc var, String value)
  { InspcCmdStore cmd = new InspcCmdStore(this);
    cmd.cmdSetValueByPath(var, value);
    cmdQueue.offer(cmd);
    throw new RuntimeException("only valid for a defined target.");
  }

  
  @Override
  public void cmdSetValueByPath(String sDataPath, float value, InspcAccessExecRxOrder_ifc actionOnRx)
  { InspcCmdStore cmd = new InspcCmdStore(this);
    cmd.cmdSetValueByPath(sDataPath, value, actionOnRx);
    cmdQueue.offer(cmd);
  }


  @Override
  public void cmdSetValueByPath(String sDataPath, double value, InspcAccessExecRxOrder_ifc actionOnRx)
  { InspcCmdStore cmd = new InspcCmdStore(this);
    cmd.cmdSetValueByPath(sDataPath, value, actionOnRx);
    cmdQueue.offer(cmd);
  }
  
  
  @Override public boolean isOrSetReady(long timeCurrent) { return false; }
  
  @Override public void addUserTxOrder(Runnable order) 
  { throw new RuntimeException("only valid for a defined target.");
  }
  
  @Override public void requestFields(InspcTargetAccessData data, InspcAccessExecRxOrder_ifc rxActionGetFields, Runnable runOnReceive)
  { throw new RuntimeException("only valid for a defined target.");
  }

}
