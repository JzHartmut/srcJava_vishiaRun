package org.vishia.inspectorAccessor;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.bridgeC.MemSegmJc;
import org.vishia.byteData.VariableAccessWithIdx;
import org.vishia.byteData.VariableAccess_ifc;
import org.vishia.byteData.VariableContainer_ifc;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessComm_SocketImpl;
import org.vishia.inspector.SearchElement;
import org.vishia.reflect.FieldJc;
import org.vishia.reflect.FieldJcVariableAccess;
import org.vishia.reflect.FieldVariableAccess;
import org.vishia.util.CompleteConstructionAndStart;
import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.StringFunctions;

/**This class supports the communication via the inspector reflex access. 
 * It is a {@link VariableContainer_ifc}. It means any application can handle with variable. 
 * If a variable is requested, a time stamp is written their (see {@link InspcVariable#timeRequested})
 * and therefore this variable is requested via its {@link InspcVariable#sPath} from the associated
 * target device.
 * <br><br>
 * This class supports free communication with Inspector reflex access outside the {@link VariableAccess_ifc}
 * thinking too. Especially via {@link #addUserOrder(Runnable)} some code snippets can be placed in the
 * communication thread of this class.
 * <br><br>
 * This class starts an onw thread for the send requests to the target device. That thread opens the
 * inspector reflex access communication via {@link InspcAccessor#open(String)} which may use an
 * {@link org.vishia.communication.InterProcessComm} instance.
 * <br><br>
 * The references to all known variables of the user are hold in an indexed list {@link #idxAllVars} sorted by name.
 * The variable reference of type {@link VariableAccess_ifc} stores the access path to the target if necessary and the 
 * actual value. The actual value may be read from a target device or not in the last time. A variable can be registered
 * to the manager calling {@link #getVariable(String)}. That routine build the correct instance for the variable access,
 * that is either {@link InspcVariable} or an {@link org.vishia.reflect.FieldJcVariableAccess} for java-internal variables.
 * The variable will be wrapped in an {@link org.vishia.byteData.VariableAccessWithIdx}. The variable knows the 
 * {@link InspcAccessor} and can get values from the target.
 * <br><br>
 * <b>Communication principle</b>:
 * The {@link #inspcThread} respectively the called method {@link #runInspcThread()} runs in a loop,
 * if the communication was opened till {@link #close()} is called. In this loop all requested variables
 * were handled with a request-value telegram and all {@link #addUserOrder(Runnable)} are processed.
 * <br><br>
 * In the {@link #runInspcThread()} all known variables from {@link #idxAllVars} are processed, see {@link #getVariable(String)}. 
 * But only if a value from a variable was requested in the last time, a new value is requested from a target device.
 * With that requests one send telegram is built. If the telegram is filled, it will be send.
 * Then the answer of the target device is awaiting, see {@link InspcAccessor#sendAndAwaitAnswer()}. 
 * In this time the loop is blocked till a timeout is occurred while waiting of the embedded device's answer.
 * Normally the embedded device or other target should answer in a few milliseconds.
 * <br><br>
 * The receiving of telegrams is executing in an extra receive Thread, see {@link InspcAccessor#receiveThread}.
 * The receive thread handles receiving from the one port of communication, which is opened with the 
 * {@link InterProcessComm} instance which usual is a {@link InterProcessComm_SocketImpl}. 
 * It is possible to send from more as this thread, or it is possible to receive some special telegrams which 
 * are not requested. That does the receive thread.
 * <br><br>
 * The receive thread knows the sequence numbers of a sent telegram, which is response by an answer telegram.
 * This sequence numbers are TODO
 * 
 *  
 * The {@link InspcAccessor} supports the execution of any action in the received thread too,
 * but this class calls {@link InspcAccessor#awaitAnswer(int)} in its send thread to force notifying
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
 * But the receiving of telegrams is executing in an extra receive Thread, see {@link InspcAccessor#receiveThread}.
 * The {@link InspcAccessor} supports the execution of any action in the received thread too,
 * but this class calls {@link InspcAccessor#awaitAnswer(int)} in its send thread to force notifying
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
  public static final int version = 20120417;

  private final InspcPlugUser_ifc user;

  /**This method is called if variable are received.
   * 
   */
  Runnable callbackOnRxData;
  
  /**This container holds all variables which are created. */
  Map<String, VariableAccess_ifc> idxAllVars = new TreeMap<String, VariableAccess_ifc>();
  
  /**This container holds that variables which are currently used for communication. */
  Map<String, InspcVariable> idxVarsInAccess = new TreeMap<String, InspcVariable>();
  
  /**This index should be empty if all requested variables are received.
   * If a variable is requested, it is registered here. If the answer is received, it is deleted.
   * If all variable values have its response, it is empty then.
   * On timeout this index is deleted too.
   */
  Map<String, InspcVariable> idxRequestedVarFromTarget = new TreeMap<String, InspcVariable>();
  
  /**Instance for the inspector access to the target. */
  public final InspcAccessor inspcAccessor;
  
  /**Own address string for the communication. */
  private final String sOwnIpcAddr;
  
  /**The target ipc-address for Interprocess-Communication with the target.
   * It is a string, which determines the kind of communication.
   * For example "UDP:0.0.0.0:60099" to create a socket port for UDP-communication.
   */
  private final Map<String, String> indexTargetIpcAddr;

  private Map<String, String> indexFaultDevice;

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
  long timeReceived;
  
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
  Thread inspcThread = new Thread("InspcMng"){ @Override public void run() { runInspcThread(); } };

  
  

  /**True if the {@link #inspcThread} is running. If set to false, the thread ends. */
  boolean bThreadRuns;
  
  /**True if the {@link #inspcThread} is in wait for data. */
  boolean bThreadWaits;
  
  /**Set from {@link #variableIsReceived(InspcVariable)} if all variables are received.
   * Tested from {@link #inspcThread} whether all are received.
   */
  boolean bAllReceived;
  
  public InspcMng(String sOwnIpcAddr, Map<String, String> indexTargetIpcAddr, boolean bUseGetValueByIndex, InspcPlugUser_ifc user){
    this.inspcAccessor = new InspcAccessor(new InspcAccessEvaluatorRxTelg());
    this.indexTargetIpcAddr = indexTargetIpcAddr;
    this.sOwnIpcAddr = sOwnIpcAddr;
    this.bUseGetValueByIndex = bUseGetValueByIndex;
    this.user = user;
    if(user !=null){
      user.setInspcComm(this);
    }

  }
  
  
  @Override public void completeConstruction(){}
  
  @Override public void startupThreads(){
    inspcThread.start();
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
  
  
  /**Adds any program snippet which is executed while preparing the telegram for data request from target.
   * @param order the program snippet.
   */
  public void addUserOrder(Runnable order)
  {
    userOrders.add(order);
  }
  

  
  @Override public VariableAccessWithIdx getVariable(final String sDataPathP)
  { int posIndex = sDataPathP.lastIndexOf('.');
    final String sDataPathVariable;
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
      sDataPathVariable = sDataPathP.substring(0, posIndex);
    } else {
      mask = -1;
      bit = 0;
      sDataPathVariable = sDataPathP;
    }
    VariableAccess_ifc var = idxAllVars.get(sDataPathVariable);
    if(var == null){
      if(sDataPathVariable.startsWith("java:")){
        FieldJc[] field = new FieldJc[0];
        int[] ix = new int[0];
        String sPathJava = sDataPathVariable.substring(5);
        MemSegmJc addr = SearchElement.searchObject(sPathJava, this, field, ix);
        var = new FieldJcVariableAccess(this, field[0]);
      } else {
        var = new InspcVariable(this, sDataPathVariable);
      }
      if(var == null){
        //TODO use dummy to prevent new requesting
      }
      idxAllVars.put(sDataPathVariable, var);
    }
    return new VariableAccessWithIdx(var, null, bit, mask);
  }
 
  
  
  /**This routine requests all variables from the target device, 
   * which were requested itself after last call of refresh.
   * The answer of the target-request will invoke 
   * {@link InspcVariable.VariableRxAction#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Info)}
   * with the info block of the telegram for each variable.
   * If all variables are received, the callback routine 
   * @returns true if at least one variable was requested, false if nothing is requested.
   * @see org.vishia.byteData.VariableContainer_ifc#refreshValues()
   */
  private boolean procComm(){
    boolean bRequest = false;
    bUserCalled = false;
    long timeCurr = System.currentTimeMillis();
    idxRequestedVarFromTarget.clear();  //clear it, only new requests are pending then.
    for(Map.Entry<String,VariableAccess_ifc> entryVar: idxAllVars.entrySet()){
      VariableAccess_ifc var = entryVar.getValue();
      if(var instanceof InspcVariable){
        InspcVariable varInspc = (InspcVariable)var;
        if(varInspc.timeRequested >= timeReceived){  //only requests communication if the variable was requested:
          bRequest = true;
          if(!varInspc.requestValueFromTarget(timeCurr)){
            //the value can't be requested. The Telegram is sent and the answer or a timeout is gotten.
            //Start with the same request in the next telegram, yet.
            boolean bOk = varInspc.requestValueFromTarget(timeCurr);
            if(!bOk){
              System.out.println("InspcMng.procComm - nok; ");
            }
            //assert(bOk);
          }
        }
      }
    }
    Runnable userOrder;
    while( (userOrder = userOrders.poll()) !=null){
      userOrder.run(); //maybe add some more requests to the current telegram.
    }
    if( !inspcAccessor.txCmdGetValueByIdent()  //calls sendAndAwaitAnswer internally 
      && inspcAccessor.isFilledTxTelg()){       //only call if necessary
        inspcAccessor.sendAndAwaitAnswer();     
    }
    
    if(user !=null){
      user.isSent(0);
    }
    
    long time = System.currentTimeMillis();
    if(time >= timeLastRemoveOrders + millisecTimeoutOrders){
      timeLastRemoveOrders = time;
      int removedOrders = inspcAccessor.rxEval.checkAndRemoveOldOrders(time - timeLastRemoveOrders);
      if(removedOrders >0){
        System.err.println("InspcMng - Communication problem, removed Orders; " + removedOrders);
      }
    }

    return bRequest;
  }




  
  
  
  
  
  /**Requests the value from the target.
   * @param sDataPath
   * @param commAction
   * @return true then the telegram has not space. The value is not requested. It should be repeated.
   */
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
  

  
  /**Prepares the info block to register a variable on the target device.
   * @param sDataPath The data path on target
   * @param actionOnRx receiving action, executing with the response info.
   */
  InspcAccessor registerByPath(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx){
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
    idxRequestedVarFromTarget.remove(var.sPath);
    if(idxRequestedVarFromTarget.isEmpty()){ //all variables are received:
      synchronized(this){
        bAllReceived = true;
        if(bThreadWaits){ notify(); }
      }
    }
  }

  
  
  
  
  /**This routine will be invoked if all data are received from the target.
   * @param ev
   */
  void callbackOnRxData(Event ev){
    
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
  final void runInspcThread(){
    bThreadRuns = true;
    inspcAccessor.open(sOwnIpcAddr);

    while(bThreadRuns){
      //delay some milliseconds before requests new values.
      synchronized(this){ try{ wait(100); } catch(InterruptedException exc){} }
      //now requests.
      bAllReceived = false;
      try{
        if(procComm()){
          synchronized(this){
            if(!bAllReceived){ //NOTE: its possible that 
              bThreadWaits = true;
              try{ wait(100); } catch(InterruptedException exc){}
              bThreadWaits = false;
            }
          }
        }
        if(!bAllReceived){
          stop();
        }
        timeReceived = System.currentTimeMillis();  //all requests after this time calls new variables.
        if(callbackOnRxData !=null){
          callbackOnRxData.run();         //show the received values.
          //the next requests for variables will be set.
          //It may be the same, it may be other.
        }
      }catch(Exception exc){
        System.err.println("InspcMng - runInspcThread - unexpected Exception; " + exc.getMessage());
        exc.printStackTrace(System.err);
      }
    }
  }
  
  
  @Override public void close() throws IOException
  { bThreadRuns = false;
    inspcAccessor.close();
  }
  
  void stop(){}
  
}
