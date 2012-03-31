package org.vishia.inspectorAccessor;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.vishia.byteData.VariableAccessWithIdx;
import org.vishia.byteData.VariableAccess_ifc;
import org.vishia.byteData.VariableContainer_ifc;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.util.CompleteConstructionAndStart;
import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.StringFunctions;

/**This class supports the communication via the inspector reflex access. 

 * @author Hartmut Schorrig
 *
 */
public class InspcMng implements CompleteConstructionAndStart, VariableContainer_ifc
{

  /**Version, history and license
   * <ul>
   * <li>2013-03-31 Hartmut created. Most of code are gotten from org.vishia.guiInspc.InspcGuiComm,
   *   which has experience since about 1 year. But the concept is changed. This source uses
   *   the {@link VariableAccess_ifc} concept which has experience in the org.vishia.guiViewCfg package.
   *   Both concepts to access variable are merged yet. The GUI to show values from a target system
   *   now has only one interface concept to access values, independent of their communication concept.
   *   This class supports the communication via the inspector reflex access. 
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
  public static final int version = 20120331;

  
  /**This method is called if variable are received.
   * 
   */
  Runnable callbackOnRxData;
  
  /**This container holds all variables which are created. */
  Map<String, InspcVariable> idxAllVars = new TreeMap<String, InspcVariable>();
  
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

  String sIpTarget;

  
  /**The time when all the receiving is finished or had its timeout.
   * 
   */
  long timeReceived;
  
  
  /**The Event callback routine which is invoked if all 
   * 
   */
  final EventConsumer callback = new EventConsumer(){
    @Override public boolean processEvent(Event ev)
    { callbackOnRxData(ev); 
      return true;
    }
  };
  
  /**The thread which calls the {@link #callbackOnRxData} method to show all received data. */
  Thread inspcThread = new Thread("InspcMng"){ @Override public void run() { runInspcThread(); } };

  
  final Event callbackOnRx = new Event(this, callback);
  

  /**True if the {@link #inspcThread} is running. If set to false, the thread ends. */
  boolean bThreadRuns;
  
  /**True if the {@link #inspcThread} is in wait for data. */
  boolean bThreadWaits;
  
  /**Set from {@link #variableIsReceived(InspcVariable)} if all variables are received.
   * Tested from {@link #inspcThread} whether all are received.
   */
  boolean bAllReceived;
  
  public InspcMng(String sOwnIpcAddr, Map<String, String> indexTargetIpcAddr){
    this.inspcAccessor = new InspcAccessor(new InspcAccessEvaluatorRxTelg());
    this.indexTargetIpcAddr = indexTargetIpcAddr;
    this.sOwnIpcAddr = sOwnIpcAddr;
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
  
  
  @Override public VariableAccessWithIdx getVariable(final String sDataPathP)
  { int posIndex = sDataPathP.lastIndexOf('.');
    final String sDataPathVariable;
    char cc;
    int mask, bit;
    if(posIndex > 0 && sDataPathP.length() > posIndex && (cc = sDataPathP.charAt(posIndex +1)) >='0' && cc <='9'){
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
    InspcVariable var = idxAllVars.get(sDataPathVariable);
    if(var == null){
      var = new InspcVariable(this, sDataPathVariable);
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
  private boolean requestValuesFromTarget(){
    boolean bRequest = false;
    idxRequestedVarFromTarget.clear();  //clear it, only new requests are pending then.
    for(Map.Entry<String,InspcVariable> entryVar: idxAllVars.entrySet()){
      InspcVariable var = entryVar.getValue();
      if(var.timeRequested >= timeReceived){  //only requests communication if the variable was requested:
        bRequest = true;
        getValueFromTarget(var);
      }
    }
    if(bRequest){
      if(inspcAccessor.isFilledTxTelg()){
        sendAndAwaitAnswer();
      }

    }
    return bRequest;
  }

  
  void getValueFromTarget(InspcVariable var)  
  { //check whether the widget has an comm action already. 
    //First time a widgets gets its WidgetCommAction. Then for ever the action is kept.
    String sPathComm = var.sPath  + ".";
    idxRequestedVarFromTarget.put(var.sPath, var);
    getValueByPath(sPathComm, var.rxAction);
  }
  
  
  
  void getValueByPath(String sDataPath, InspcAccessExecRxOrder_ifc commAction){
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
      /*  TODO need?
      if(user !=null && !bUserCalled){  //call only one time per procComm()
        user.requData(0);
        bUserCalled = true;
      }
      */
      //
      //create the send command to target.
      int order = inspcAccessor.cmdGetValueByPath(sDataPath);    
      if(order !=0){
        //save the order to the action. It is taken on receive.
        inspcAccessor.rxEval.setExpectedOrder(order, commAction);
      } else {
        sendAndAwaitAnswer();  //calls execInspcRxOrder as callback.
        //sent = true;
      } 
    }    
  }
  

  
  String translateDeviceToAddrIp(String sDevice)
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

  
  
  void sendAndAwaitAnswer()
  { inspcAccessor.send();
    InspcDataExchangeAccess.Datagram[] answerTelgs = inspcAccessor.awaitAnswer(2000);
    if(answerTelgs !=null){
      inspcAccessor.rxEval.evaluate(answerTelgs, null); //executerAnswerInfo);  //executer on any info block.
    } else {
      System.err.println("no communication");
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
      if(requestValuesFromTarget()){
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
    }
  }
  
  
  void stop(){}
  
}
