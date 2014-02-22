package org.vishia.inspectorAccessor;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.bridgeC.MemSegmJc;
import org.vishia.byteData.VariableAccessArray_ifc;
import org.vishia.byteData.VariableAccessWithBitmask;
import org.vishia.byteData.VariableAccessWithIdx;
import org.vishia.byteData.VariableAccess_ifc;
import org.vishia.byteData.VariableContainer_ifc;
import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessComm_SocketImpl;
import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.inspector.SearchElement;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.reflect.FieldJc;
import org.vishia.reflect.FieldJcVariableAccess;
import org.vishia.reflect.FieldVariableAccess;
import org.vishia.util.Assert;
import org.vishia.util.CompleteConstructionAndStart;
import org.vishia.util.StringFunctions;
import org.vishia.util.ThreadRun;

/**This class supports the communication via the inspector reflex access. 
 * It is a {@link VariableContainer_ifc}. It means any application can handle with variable. 
 * If a variable is requested, a time stamp is written their (see {@link InspcVariable#timeRequested})
 * and therefore this variable is requested via its {@link InspcVariable#sPathInTarget} from the associated
 * target device.
 * <br><br>
 * This class supports free communication with Inspector reflex access outside the {@link VariableAccess_ifc}
 * thinking too. Especially via {@link #addUserOrder(Runnable)} some code snippets can be placed in the
 * communication thread of this class.
 * <br><br>
 * This class starts an onw thread for the send requests to the target device. That thread opens the
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
 * The {@link #inspcThread} respectively the called method {@link #runInspcThread()} runs in a loop,
 * if the communication was opened till {@link #close()} is called. In this loop all requested variables
 * were handled with a request-value telegram and all {@link #addUserOrder(Runnable)} are processed.
 * <br><br>
 * In the {@link #runInspcThread()} all known variables from {@link #idxAllVars} are processed, see {@link #getVariable(String)}. 
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
public class InspcMng implements CompleteConstructionAndStart, VariableContainer_ifc, Closeable
{

  /**Version, history and license.
   * <ul>
   * <li>2013-01-10 Hartmut bugfix: If a variable can't be requested in {@link #requestValueByPath(String, InspcAccessExecRxOrder_ifc)} because
   *   the telegram is full, the same variable should be requested repeatedly in the next telegram. It was forgotten.
   * <li>2012-06-09 Hartmut new: Now it knows java-internal variable too, the path for the argument of {@link #getVariable(String)}
   *   should start with "java:" then the variable is searched internally. TODO now it isn't tested well, the start instance
   *   should be given by constructor, because it should be either the start instance of the whole application or a special
   *   data area.
   * <li>2012-04-17 Hartmut new: {@link #bUseGetValueByIndex}: Access via getValuePerPath for downward compatibility with target device.
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
   * <li>2011-06-30 Hartmut new: {@link #sendAndPrepareCmdSetValueByPath(String, long, int, InspcAccessExecRxOrder_ifc)}:
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
  
  private final InspcPlugUser_ifc user;

  /**This method is called if variable are received.
   * 
   */
  Runnable callbackOnRxData;
  
  
  /**If true then writes a log of all send and received telegrams. */
  LogMessage logTelg;
  
  boolean bWriteDebugSystemOut;  //if(logTelg !=null && bWriteDebugSystemOut)
  
  int identLogTelg;
  
  /**This container holds all variables which are created. */
  Map<String, VariableAccess_ifc> idxAllVars = new TreeMap<String, VariableAccess_ifc>();
  
  
  /**This container holds all variables which are created. */
  Map<String, InspcStruct> idxAllStruct = new TreeMap<String, InspcStruct>();
  
  /**If not null then cmdGetFields should be invoked. */
  InspcStruct requestedFields;
  
  /**This container holds that variables which are currently used for communication. */
  Map<String, InspcVariable> XXXidxVarsInAccess = new TreeMap<String, InspcVariable>();
  
  /**This index should be empty if all requested variables are received.
   * If a variable is requested, it is registered here. If the answer is received, it is deleted.
   * If all variable values have its response, it is empty then.
   * On timeout this index is deleted too.
   */
  Map<String, InspcVariable> idxRequestedVarFromTarget = new TreeMap<String, InspcVariable>();
  
  /**Instance for the inspector access to the target. */
  //public final InspcTargetAccessor inspcAccessor;
  
  /**Own address string for the communication. */
  private final String sOwnIpcAddr;
  
  /**The target ipc-address for Interprocess-Communication with the target.
   * It is a string, which determines the kind of communication.
   * For example "UDP:0.0.0.0:60099" to create a socket port for UDP-communication.
   */
  private final Map<String, String> indexTargetIpcAddr;

  
  private final Map<String, InspcTargetAccessor> indexTargetAccessor;
  
  private final List<InspcTargetAccessor> listTargetAccessor;
  
  private Map<String, String> indexFaultDevice;
  
  boolean retryDisabledVariable;

  long millisecTimeoutOrders = 5000;
  
  long timeLastRemoveOrders;

  
  String sIpTarget;
  
  final boolean bUseGetValueByIndex;

  boolean bUserCalled;

  /**Some orders from any application which should be run in the {@link #inspcThread}. */
  public ConcurrentLinkedQueue<Runnable> userOrders = new ConcurrentLinkedQueue<Runnable>();
  
  /**The time when all the receiving is finished or had its timeout.
   * 
   */
  long XXXtimeReceived;
  
  protected final InspcCommPort commPort; 
  

  
  /**The Event callback routine which is invoked if all 
   * 
   */
  final EventConsumer XXXcallback = new EventConsumer(){
    @Override public int processEvent(Event ev)
    { callbackOnRxData(ev); 
      return 1;
    }
    @Override public String toString(){ return "InspcMng - callback rxdata"; }

  };
  
  /**The thread which calls the {@link #callbackOnRxData} method to show all received data. */
  //Thread inspcThread = new Thread("InspcMng"){ @Override public void run() { runInspcThread(); } };

  
  

  /**True if the {@link #inspcThread} is running. If set to false, the thread ends. */
  //boolean bThreadRuns;
  
  /**True if the {@link #inspcThread} is in wait for data. */
  //boolean bThreadWaits;
  
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
    this.indexTargetAccessor = new TreeMap<String, InspcTargetAccessor>();
    this.listTargetAccessor = new LinkedList<InspcTargetAccessor>();
    this.sOwnIpcAddr = sOwnIpcAddr;
    this.bUseGetValueByIndex = bUseGetValueByIndex;
    this.user = user;
    if(user !=null){
      user.setInspcComm(this);
    }

  }
  
  
  @Override public void completeConstruction(){}
  
  @Override public void startupThreads(){
    threadReqFromTarget.start();
  }

  
  
  
  @Override public void setCallbackOnReceivedData(Runnable callback){
    this.callbackOnRxData = callback;
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
  
  
  /**Adds any program snippet which is executed while preparing the telegram for data request from target.
   * @param order the program snippet.
   */
  public void addUserOrder(Runnable order)
  {
    userOrders.add(order);
  }
  
  
  public void requestFields(InspcStruct struct){
    requestedFields = struct;
  }

  
  @Override public VariableAccess_ifc getVariable(final String sDataPathP)
  { int posIndex = sDataPathP.lastIndexOf('.');
    final String sDataPathOfWidget;
    char cc;
    int mask, bit;
    if(posIndex > 0 && sDataPathP.length() > posIndex+1 && (cc = sDataPathP.charAt(posIndex +1)) >='0' && cc <='9'){
      //it is a bit designation:
      if(sDataPathP.charAt(posIndex -1) =='.'){ //a double .. for bit space like 12..8
        int posIndex1 = sDataPathP.lastIndexOf('.', posIndex -2);
        int bitStart = StringFunctions.parseIntRadixBack(sDataPathP, posIndex1-1, posIndex1-1, 10, null);
        int[] zParsed = new int[1];
        int bitEnd = StringFunctions.parseIntRadix(sDataPathP, posIndex+1, 2, 10, zParsed);
        posIndex = posIndex1 - zParsed[0];
        if(bitStart >= bitEnd){
          bit = bitEnd;
          mask = (1 << (bitStart - bitEnd +1)) -1;
        } else {
          bit = bitStart;
          mask = (1 << (bitEnd - bitStart +1)) -1;
        }
      } else {
        bit = StringFunctions.parseIntRadix(sDataPathP, posIndex+1, 2, 10, null);
        mask = 1;
      }
      sDataPathOfWidget = sDataPathP.substring(0, posIndex);
    } else {
      mask = -1;
      bit = 0;
      sDataPathOfWidget = sDataPathP;
    }
    VariableAccess_ifc var = idxAllVars.get(sDataPathOfWidget);
    if(var == null){
      if(sDataPathOfWidget.startsWith("java:")){
        FieldJc[] field = new FieldJc[0];
        int[] ix = new int[0];
        String sPathJava = sDataPathOfWidget.substring(5);
        MemSegmJc addr = SearchElement.searchObject(sPathJava, this, field, ix);
        var = new FieldJcVariableAccess(this, field[0]);
      } else {
        PathStructAccessor path1 = getTargetFromPath(sDataPathOfWidget);
        if(path1 !=null && path1.accessor !=null){
          var = new InspcVariable(this, path1.accessor, path1.itsStruct, path1.sPathInTarget, path1.sName);
        } else {
          System.err.println("InspcMng - Variable target unknown; " + sDataPathOfWidget); 
        }
      }
      if(var == null){
        Assert.stop();
        //TODO use dummy to prevent new requesting
      } else {
        idxAllVars.put(sDataPathOfWidget, var);
      }
    }
    if(mask == -1){ return var; }
    else { return new VariableAccessWithBitmask(var, bit, mask); }
  }
 
  
  
  /**This routine requests all variables from its target devices, 
   * which were requested itself after last call of refresh.
   * The answer of the target-request will invoke 
   * {@link InspcVariable.VariableRxAction#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Info)}
   * with the info block of the telegram for each variable.
   * If all variables are received, the callback routine 
   * @returns true if at least one variable was requested, false if nothing is requested.
   * @see org.vishia.byteData.VariableContainer_ifc#refreshValues()
   */
  protected boolean procComm(){
    boolean bRequest = false;
    bUserCalled = false;
    long timeCurr = System.currentTimeMillis();
    idxRequestedVarFromTarget.clear();  //clear it, only new requests are pending then.
    //System.out.println("InspcMng.ProcComm - step;");
    int nrofVarsReq = 0;
    int nrofVarsAll = 0;
    if(requestedFields !=null){
      requestedFields.fields.clear();
      PathStructAccessor path1 = getTargetFromPath(requestedFields.path()); 
      //InspcTargetAccessor targetAccessor = requestedFields.targetAccessor();
      path1.accessor.cmdGetFields(path1.sPathInTarget, requestedFields.rxActionGetFields);
      requestedFields = null;
    }
    for(Map.Entry<String,VariableAccess_ifc> entryVar: idxAllVars.entrySet()){
      VariableAccess_ifc var = entryVar.getValue();
      nrofVarsAll +=1;
      if(var instanceof InspcVariable){
        InspcVariable varInspc = (InspcVariable)var;
        if(   var.isRequestedValue(retryDisabledVariable) ){  //handle only variable from Inspector access
          if(varInspc.sPathInTarget.startsWith("#"))
            Assert.stop();
          bRequest = true;
          if(varInspc.targetAccessor.isReady(timeCurr)){
            nrofVarsReq +=1;
            varInspc.requestValueFromTarget(timeCurr, retryDisabledVariable);
          } else {
            //The variable is not able to get, remove the request.
            //The request will be repeat if the variable is newly requested.
            var.requestValue(0, null);
          }
          
        }
      }
    }
    Runnable userOrder;
    while( (userOrder = userOrders.poll()) !=null){
      userOrder.run(); //maybe add some more requests to the current telegram.
    }
    if(nrofVarsReq >0){
      //System.out.println("InspcMng.procComm - variables requested; " + nrofVarsReq + "; all=" + nrofVarsAll);
    }
    for(InspcTargetAccessor inspcAccessor: listTargetAccessor){
      inspcAccessor.cmdFinit();     
    }
    
    if(user !=null){
      user.isSent(0);
    }
    
    long time = System.currentTimeMillis();
    if(time >= timeLastRemoveOrders + millisecTimeoutOrders){
      timeLastRemoveOrders = time;
      for(InspcTargetAccessor inspcAccessor: listTargetAccessor){
        int removedOrders = inspcAccessor.rxEval.checkAndRemoveOldOrders(time - timeLastRemoveOrders);
        if(removedOrders >0){
          System.err.println("InspcMng - Communication problem, removed Orders; " + removedOrders);
        }
      }
    }

    return bRequest;
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
        inspcAccessor.rxEval.setExpectedOrder(order, commAction);
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
  

  
  /**Splits and replaces a given data path in a variable GUI field to the target accessor,
   * It uses {@link #indexTargetAccessor} to get the target accessor instance.
   * It creates or gets and references {@link InspcStruct} with all parents for this path.
   *  
   * @param sDataPath The user given data path
   * @param retDataPath
   * @return
   */
  public PathStructAccessor getTargetFromPath(String sDataPath){
    final InspcTargetAccessor accessor;
    final String sPathInTarget;
    final String sName;
    InspcStruct itsStruct;
    
    int posSepDevice = sDataPath.indexOf(':');
    if(posSepDevice >0){
      String sDevice = sDataPath.substring(0, posSepDevice);
      accessor = indexTargetAccessor.get(sDevice);
      if(accessor == null){
        errorDevice(sDevice);
      }
      int posName = sDataPath.lastIndexOf('.');
      if(posName >0){
        String sStructPath = sDataPath.substring(/*posSepDevice +1*/ 0, posName);
        itsStruct = idxAllStruct.get(sStructPath);
        if(itsStruct == null){
          InspcStruct parent = getOrCreateParentStruct(sStructPath, accessor);
          itsStruct = new InspcStruct(sStructPath, accessor, parent);
          idxAllStruct.put(sStructPath, itsStruct);
        }
        sName = sDataPath.substring(posName +1);
      } else {
        sName = sDataPath.substring(posSepDevice +1);
        itsStruct = null;
      }
      sPathInTarget = sDataPath.substring(posSepDevice +1);
      return new PathStructAccessor(accessor, sPathInTarget, sName, itsStruct);
      
    } else {
      return null;
    }

  }
  

  
  /**Gets or creates the parent for the given path. The parent is referenced in {@link #idxAllStruct}.
   * @param sPathChild
   * @return null if it is the root.
   */
  private InspcStruct getOrCreateParentStruct(String sPathChild, InspcTargetAccessor accessor){
    if(sPathChild.endsWith(":")){
      return null;
    } else {
      int posLastDot = sPathChild.lastIndexOf('.');
      if(posLastDot <0) {
        posLastDot = sPathChild.indexOf(':') +1;
      }
      final String sPath = sPathChild.substring(0, posLastDot);
      InspcStruct ret = idxAllStruct.get(sPath);
      if(ret == null){
        InspcStruct parent = getOrCreateParentStruct(sPath, accessor);
        ret = new InspcStruct(sPath, accessor, parent);
        idxAllStruct.put(sPath, ret);
      }
      return ret;
    }
  }
  
  
  
  public InspcVariable getOrCreateVariable(InspcStruct struct, InspcStruct.FieldOfStruct field){
    InspcVariable var = field.variable();
    if(var == null){
      String sPathVar = struct.path() + '.' + field.name;
      var = (InspcVariable)getVariable(sPathVar);
      if(var !=null){
        field.setVariable(var);
      }
    }
    return var; 
  }
  

  
  public void cmdSetValueOfField(InspcStruct struct, InspcStruct.FieldOfStruct field, String value){
    InspcVariable var = getOrCreateVariable(struct, field);
    if(var !=null){
      switch(var.cType){
        case 'D':
        case 'F': {
          double val = Double.parseDouble(value); 
          var.targetAccessor.cmdSetValueByPath(var.sPathInTarget, val); 
        } break;
        case 'S':
        case 'B':
        case 'I': {
          int val = Integer.parseInt(value); 
          var.targetAccessor.cmdSetValueByPath(var.sPathInTarget, val); 
        } break;
        case 's': {  //empty yet
          
        } break;
      }
    }

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
  
  
  
  /*package private*/ void variableIsReceived(InspcVariable var){
    idxRequestedVarFromTarget.remove(var.sPathInTarget);
    if(idxRequestedVarFromTarget.isEmpty()){ //all variables are received:
      threadReqFromTarget.forceStep(false);
    }
  }

  
  
  
  
  /**This routine will be invoked if all data are received from the target.
   * @param ev
   */
  void callbackOnRxData(Event ev){
    
  }
  

  void openComm(){
    commPort.open(sOwnIpcAddr);
    for(Map.Entry<String, String> e : indexTargetIpcAddr.entrySet()){
      Address_InterProcessComm addrTarget = commPort.createTargetAddr(e.getValue());
      InspcTargetAccessor accessor = new InspcTargetAccessor(commPort, addrTarget, new InspcAccessEvaluatorRxTelg());
      indexTargetAccessor.put(e.getKey(), accessor);
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
     * <li>then calls {@link #callbackOnRxData(Event)} to show the values.
     * <li>then loops.
     * </ul>
     */
    @Override public final int step(int cycletime, int cycletimelast, int calctimelast, long timesecondsAbs){
      //bThreadRuns = true;
      if(callbackOnRxData !=null){
        callbackOnRxData.run();         //show the received values.
        //the next requests for variables will be set.
        //It may be the same, it may be other.
      }
  
      bAllReceived = false;
        procComm();
        if(!bAllReceived){
          stop();
        }
        //timeReceived = System.currentTimeMillis();  //all requests after this time calls new variables.
      return -1; //!bAllReceived;
    }//step
  }; //step  
  
  @Override public void close() throws IOException
  { //bThreadRuns = false;
    threadReqFromTarget.close();
    commPort.close();
  }
  
  void stop(){}
  
  public static class PathStructAccessor{
    public final InspcTargetAccessor accessor;
    public final String sPathInTarget;
    public final String sName;
    public final InspcStruct itsStruct;
    
    public PathStructAccessor(InspcTargetAccessor accessor, String sPathInTarget, String sName, InspcStruct itsStruct)
    { this.accessor = accessor;
      this.sPathInTarget = sPathInTarget;
      this.sName = sName;
      this.itsStruct = itsStruct;
    }
    
  }
  
}
