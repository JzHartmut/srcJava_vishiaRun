package org.vishia.inspcPC.mng;

import java.text.ParseException;

import org.vishia.bridgeC.ConcurrentLinkedQueue;
import org.vishia.bridgeC.IllegalArgumentExceptionJc;
import org.vishia.byteData.VariableAccessArray_ifc;
import org.vishia.byteData.VariableAccess_ifc;
import org.vishia.byteData.VariableContainer_ifc;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.inspcPC.InspcAccessExecRxOrder_ifc;
import org.vishia.inspcPC.InspcTargetAccessData;
import org.vishia.inspcPC.accTarget.InspcTargetAccessor;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.reflect.ClassJc;
import org.vishia.util.Debugutil;
import org.vishia.util.StringPartScan;

/**This class presents a variable, which is accessed by a {@link InspcTargetAccessor}. It mirrors a variable in the target.
 * <br><br>
 * <b>Get a variable</b>:<br>
 * Use the {@link VariableContainer_ifc#getVariable(String)} which is provided with the {@link InspcMng} to access an already existing
 * variable or create a new one mirror for a variable. The String parameter should be the access path to the target. 
 * The access path can start with <br>
 * <code>alias:...path...</code>: <br>
 * The alias will be translated using the given {@link InspcMng#complete_ReplaceAlias_ifc(org.vishia.util.ReplaceAlias_ifc)}.
 * <br><br>
 * After replacing the alias or without alias the path should start with <br>
 * <code>target:...path...</code><br>
 * The target identifier should be one of the names given by construction of the {@link InspcMng#InspcMng(String, java.util.Map, int, boolean, org.vishia.inspcPC.InspcPlugUser_ifc)}.
 *  
 * <br><br>
 * <b>Access a variable</b>:<br>
 * <ul>
 * <li>One can invoke {@link #requestValue()} or {@link #requestValue(long, Runnable)} to request the communication with the target. 
 * This is done for example for graphical representation in {@link org.vishia.gral.base.GralWidget#requestNewValueForVariable(long)} 
 * <li>The {@link InspcMng#procComm()} routine checks all variables via {@link #isRequestedValue(boolean)}. 
 * That call sets an internal flag {@link #isRequested} to prevent more as one request to the target. 
 * It sends the request to the target in the routine {@link #requestValueFromTarget(long, boolean)}
 * either via {@link InspcTargetAccessor#cmdGetValueByPath(String, InspcAccessExecRxOrder_ifc)}
 * or {@link InspcTargetAccessor#cmdGetValueByHandle(int, InspcAccessExecRxOrder_ifc)} with more variable in one datagram and all requests in up to 10 datagrams.
 * <li>The target may answer (if connected) and invoke the callback routine of the request. This is done in the same thread as the request
 * on start of {@link InspcMng#procComm()}. The answer was expected in another thread (communication thread) but the answer datagrams are stored in the 
 * {@link InspcTargetAccessor.TelgData} instance to evaluate in the procComm. The answer sets the {@link #timeRefreshed}.
 * <li>It is possible to check whether the variable is updated by invocation of {@link #getLastRefreshTime()}. With them it is possible 
 * to set fields to show the value to gray if the variable was not refreshed a longer time.
 * <li>The invocation of {@link #getFloat()} etc. returns the stored value independently whether it was refreshed or not.
 * </ul>  
 * @author Hartmut Schorrig
 *
 */
public class InspcVariable implements VariableAccessArray_ifc
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-12-07 Hartmut new: Bit {@link #isRequested} to prevent request twice, in experience.
   * <li>2013-12-07 Hartmut new: {@link #sValueToTarget} is set on {@link #setString(String)}. Then the new content
   *   is sent to target to change it there. It is done in the {@link #requestValueFromTarget(long, boolean)} 
   *   because this routine is called if the field is shown. Only if it is shown the change can be done.
   * <li>2013-12-07 Hartmut new: {@link #itsStruct} 
   * <li>2013-12-07 Hartmut chg: In {@link VariableRxAction}: Answer from target with info.cmd = {@link InspcDataExchangeAccess.Inspcitem#kFailedPath} 
   *   disables this variable from data communication. TODO enable with user action if the target was changed (recompiled, restarted etc).
   * <li>2013-12-07 Hartmut chg: In {@link VariableRxAction}: Answer from target with variable type designation = {@link InspcDataExchangeAccess#kInvalidHandle}
   *   The requester should remove that index. Then a new {@link InspcTargetAccessor#cmdRegisterHandle(String, InspcAccessExecRxOrder_ifc)}
   *   is forced to get a valid index.
   * <li>2013-12-07 Hartmut chg: {@link #requestValueFromTarget(long)}: If the path starts with '#', it is not requested.       
   * <li>2013-01-10 Hartmut bugfix: If a variable can't be requested in {@link #requestValueFromTarget(long)} because
   *   the telegram is full, the same variable should be requested repeatedly in the next telegram. It was forgotten.
   * <li>2012-09-24 Hartmut new {@link #getLong(int...)} and {@link #setLong(long, int...)} not implemented, only formal 
   * <li>2012-04-22 Hartmut adapt: {@link #requestValue(long)} etc. from {@link VariableAccess_ifc}.
   * <li>2012-04-17 Hartmut new: Access via getValuePerPath for downward compatibility with target device.
   * <li>2012-04-08 Hartmut new: Support of GetValueByIdent
   * <li>2012-03-31 Hartmut created. See {@link InspcMng#version}. 
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
  public static final int version = 20150127;

  final InspcMng varMng;
  
  /**This class supplies the method to set the variable value from a received info block. 
   */
  class VariableRxAction implements InspcAccessExecRxOrder_ifc
  {
     /**This method is called for any info block in the received telegram from target,
     * if this implementing instance is stored on the order.
     * It prepares the value presentation.
     * @see org.vishia.inspcPC.InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem)
     */
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Inspcitem info, long time, LogMessage log, int identLog)
    {
      //String sShow;
      //int order = info.getOrder();
      int cmd = info.getCmd();
      //if(widgd instanceof GralLed){
      int lenItem = info.getLenInfo();  
      //}
      switch(cmd){
        case InspcDataExchangeAccess.Inspcitem.kAnswerRegisterHandle: {
          int handle = (int)info.getChildInteger(4);
          InspcVariable.this.handleTarget = handle;
          InspcVariable.this.typeTarget = (short)info.getChildInt(1);  //The value follows, first byte is the type.
          InspcVariable.this.cType = InspcTargetAccessor.getTypeFromInspcType(typeTarget);
          InspcVariable.this.modeTarget = ModeHandleVariable.kTargetUseByHandle;
        } break;
        
        case InspcDataExchangeAccess.Inspcitem.kAnswerValueByHandle: { //same handling, though only one of some values are gotten.
          InspcDataExchangeAccess.InspcAnswerValueByHandle accValuesByHandle = new InspcDataExchangeAccess.InspcAnswerValueByHandle(info);
          int ixHandle1 = accValuesByHandle.getIxHandleFrom();
          int ixHandle2 = accValuesByHandle.getIxHandleTo();
          for(int ixHandle = ixHandle1; ixHandle < ixHandle2; ++ixHandle) {
            //check the size and type of any answer value:
          }
        } break;
        case InspcDataExchangeAccess.Inspcitem.kFailedHandle: { //same handling, though only one of some values are gotten.
          InspcDataExchangeAccess.InspcAnswerValueByHandle accValuesByHandle = new InspcDataExchangeAccess.InspcAnswerValueByHandle(info);
          int ixHandle1 = accValuesByHandle.getIxHandleFrom();
          int ixHandle2 = accValuesByHandle.getIxHandleTo();
          for(int ixHandle = ixHandle1; ixHandle < ixHandle2; ++ixHandle) {
            //check the size and type of any answer value:
          }
        } break;
        case InspcDataExchangeAccess.Inspcitem.kAnswerValue: {
          InspcVariable.this.typeTarget = InspcTargetAccessor.getInspcTypeFromRxValue(info);
          InspcVariable.this.cType = InspcTargetAccessor.getTypeFromInspcType(typeTarget);
          if(typeTarget == InspcDataExchangeAccess.kTypeNoValue || typeTarget == InspcDataExchangeAccess.kInvalidHandle){
            modeTarget = ModeHandleVariable.kTargetNotSet;  //try again.
          }
          else if(cType == 'c'){ //character String
            valueS = InspcTargetAccessor.valueStringFromRxValue(info, typeTarget);  
          }
          else if("BSI".indexOf(cType) >=0){
            valueI = InspcTargetAccessor.valueIntFromRxValue(info, typeTarget);
            valueF = valueI;
          } else { 
            valueF = InspcTargetAccessor.valueFloatFromRxValue(info, typeTarget);
            valueI = (int)valueF;
          }
          if(log !=null){
            log.sendMsg(identLog, "InspcVariable - receive; variable=%s, type=%c, val = %8X = %d = %f", ds.sPathInTarget, cType, valueI, valueI, valueF);
          }
          timeRefreshed = time;
          Runnable runReceived;
          while((runReceived = runOnRecv.poll())!=null){
            System.out.println("InspcVariable.execInspcRxOrder - runOnRecv, " + ds.sDataPath);
            runReceived.run();
          }
        } break;
        case InspcDataExchangeAccess.Inspcitem.kFailedPath:{
          System.err.println("InspcAccessEvaluatorRxTelg - failed path; " + ds.sPathInTarget);
          modeTarget = ModeHandleVariable.kIdTargetDisabled;
        } break;
        default: {
          System.err.println("InspcAccessEvaluatorRxTelg - unknown answer ident; " + ds.sPathInTarget);
        } break;
      }//switch
    }
    
    @Override public Runnable callbackOnAnswer(){return null; }  //empty

  }


  
  /**This class supplies the method to set the variable value from a received info block. 
   */
  @SuppressWarnings("synthetic-access") 
  class ActionRxByHandle implements InspcAccessExecRxOrder_ifc
  {
     /**This method is called If a answer value by handle was received and the ixHandle has referred this variable.
     * It prepares the value presentation.
     * @see org.vishia.inspcPC.InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem)
     */
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Inspcitem accAnswerItem, long time, LogMessage log, int identLog)
    {
      setValueFormAnswerTelgByHandle(accAnswerItem, time);
    }

    @Override public Runnable callbackOnAnswer(){return null; }  //empty
 } 
 
  private final ActionRxByHandle actionRxByHandle = new ActionRxByHandle();
  
  
  
  
  /**The structure were this variable is member of. Not null if this is not the root variable in a device. */
  public final InspcVariable parent;
  
  /*package private*/ final VariableRxAction rxAction = new VariableRxAction();
  
  public final InspcTargetAccessData ds;
  
  /**It is is a structure, it maybe not null if it is requested.
   * null for a leaf variable, null if the structure was not requested till now.
   * See {@link #getOrCreateStructForNonLeafVariables()}. It creates.
   */
  private InspcStruct itsStruct;

  
  /**Special designations as value of {@link #idTarget} 
   */
  protected final static int kIdTargetUndefined = -1, kIdTargetDisabled = -3; 
  
  /**Special designations as value of {@link #idTarget} 
   */
  protected final static int kIdTargetUsePerPath = -2; 
  
  enum ModeHandleVariable {
    /**The Variable is not used yet. */
    kTargetNotSet, 
    /**The variable has not responsed and it is set to disable yet. */
    kIdTargetDisabled, 
    
    kIdTargetUsePerPath, 
    
    /**The variable has a valid handle. {@link #handleTarget} should hava a proper value.
     * The cmdRequestByHandle is used. */
    kTargetUseByHandle, 
    /**The variable was requested with cmdRequestHandle. It has not answered till now. wait for answer. */
    kTargetHandleRequested
  }
  
  ModeHandleVariable modeTarget = ModeHandleVariable.kTargetNotSet;
  
  /**If >=0 then it is the handle of the variable in the target device.
   * if <0 then see {@link #kIdTargetDisabled} etc.
   * The the value can be gotten calling getValueByHandle().
   */
  int handleTarget;
  
  /**The type information which is returned from the target by registerHandle. */
  short typeTarget;
  
  /**Timestamp in milliseconds after 1970 when the variable was requested. 
   * A value may be gotten only if a new request is pending. */
  long timeRequested;
  
  /**If true this variable is requested already by the {@link InspcMng}. The answer should be awaited. */ 
  boolean isRequested;
  
  long timeRefreshed;
  
  private final ConcurrentLinkedQueue<Runnable> runOnRecv = new ConcurrentLinkedQueue<Runnable>();
  
  /**The value from the target device. */
  float valueF;

  /**The value from the target device. */
  int valueI;
  
  /**The value from the target device. */
  String valueS;
  
  /**The type depends from the type in the target device. It is set if any answer is gotten. 
   * 'c' for character array. */
  char cType = 'F';
  
  /**The value of the variable was set from {@link VariableAccess_ifc}. It means it should be sent to target.*/
  String sValueToTarget;
  
  /**Creates a variable. A variable is an entity, which will be gotten with one access to the 
   * target device. It may be a String or a short static array too.
   * 
   * @param mng
   * @param sPathInTarget The access path.
   */
  InspcVariable(InspcMng mng, InspcVariable parent, InspcTargetAccessData data){
    this.varMng = mng;
    this.ds = data;
    this.parent = parent;
    //if(data.itsStruct !=null) {
    //  data.itsStruct.registerVariable(this);
    //}
  }
  
  
  /**Notes the request for this variable in the request telegram to the target.
   * @param retryDisabledVariable true then retry a disabled variable, see {@link #kIdTargetDisabled}
   * @return order if the datagram item is set. 0 if the datagram is full. -1 if there is nothing to send.
   */
  //package private
  boolean requestValueFromTarget(long timeCurrent, boolean retryDisabledVariable)  
  { //check whether the widget has an comm action already. 
    //First time a widgets gets its WidgetCommAction. Then for ever the action is kept.
    if(ds.sDataPath.equals("Sim94:simTime.timeShort")) {
      //System.out.println("InspcTargetAccessor.cmdGetValueByPath - check1, " +  ds.sDataPath);
      ///Debugutil.stop();
    }  

    if(sValueToTarget !=null) { //thread safety: atomic operation set the reference.
      String sPathComm = this.ds.sPathInTarget  + ".";
      try{
        if("BSI".indexOf(cType) >=0){
          final int value;
          if(sValueToTarget.startsWith("0x")){
            value = Integer.parseInt(sValueToTarget.substring(2),16);
          } else {
            value = Integer.parseInt(sValueToTarget);
          }
          ds.targetAccessor.cmdSetInt32ByPath(sPathComm, value, null);
        } else if(cType == 'F') {
          sValueToTarget = sValueToTarget.replace(',','.');  //use decimal point instead german colon 
          StringPartScan spValue = new StringPartScan(sValueToTarget);
          spValue.setIgnoreWhitespaces(true);
          try{ 
            if(spValue.scanFloatNumber().scanOk()) {
              double value = spValue.getLastScannedFloatNumber(); //Float.parseFloat(sValueToTarget);
              if(spValue.scan("k").scanOk()){
                value *= 1000.0f;
              } else if(spValue.scan("M").scanOk()){
                value *= 1000000.0f;
              }
              ds.targetAccessor.cmdSetDoubleByPath(sPathComm, value, null);
            }
          } catch(ParseException exc){
            
          }
          spValue.close();
        } else {
          System.out.println("InspcVariable - faulty type for setValue; " + cType);
        }
      } catch(NumberFormatException exc) {
        System.err.println("InspcVariable - faulty value for setValue; " + sValueToTarget);
      }
      sValueToTarget = null;
      return true;
    }
    if(varMng.bUseGetValueByHandle){
      if(modeTarget == ModeHandleVariable.kTargetUseByHandle){
        return ds.targetAccessor.cmdGetValueByHandle(this.handleTarget, this.actionRxByHandle);
      } else if(modeTarget != ModeHandleVariable.kTargetHandleRequested){
        //register the variable in the target system:
        modeTarget = ModeHandleVariable.kTargetHandleRequested;
        String sPathComm = this.ds.sPathInTarget  + ".";
        if(sPathComm.charAt(0) != '#'){
          return  ds.targetAccessor.cmdRegisterHandle(sPathComm, this.rxAction) !=0;
        }
      }
    } else if(modeTarget == ModeHandleVariable.kIdTargetDisabled){
      if(retryDisabledVariable && this.ds.sPathInTarget.length() >0 && this.ds.sPathInTarget.charAt(0) != '#'){
        modeTarget = ModeHandleVariable.kTargetNotSet;  //in the next step: register or get by path
      }
      return true;  //true because the variable is handled.
    } else {
      //get by handle is not supported:
      String sPathComm = this.ds.sPathInTarget  + ".";
      if(sPathComm.charAt(0) != '#'){
        modeTarget = ModeHandleVariable.kIdTargetUsePerPath;
        return ds.targetAccessor.cmdGetValueByPath(sPathComm, this.rxAction) !=0;
        //return varMng.requestValueByPath(sPathComm, this.rxAction);
      } else { 
        modeTarget = ModeHandleVariable.kIdTargetDisabled;
        return true;  //variable is handled.
      }
    }
    return true;   //true because the variable is handled.
  }
  
  
  

  
  @Override
  public double getDouble()
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float getFloat()
  {
    return valueF;
  }

  @Override
  public int getInt()
  {
    return valueI;
  }

  @Override
  public long getLong()
  {
    return valueI;
  }

  @Override public String getString() 
  { if(valueS !=null) { return valueS; }
    else switch(cType) {
      case 'F': case 'D': return Float.toString(valueF);
      default: return Integer.toString(valueI); 
    }//switch 
  }

  @Override
  public double setDouble(double value)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float setFloat(float value)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int setInt(int value)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long setLong(long value)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String setString(String value)
  { sValueToTarget = value.trim();   //it will be sent if it is set, thread safe by setting one atomic reference.
    //System.out.println("TODO InscVariable.setString(); " + value);
    return value;
  }
  
  
  
  @Override public char getType(){ return cType; } 
  
  
  
  /**Creates an {@link InspcStruct} if it is not created till now, returns it.
   * This method should only be called for variable which are not leaf variables in the target device.
   * If it is created for a leaf variable, the filling of the struct fails so that the structure has no fields. 
   * 
   * @return Instance for the structure information of this variable.
   */
  public InspcStruct getOrCreateStructForNonLeafVariables() { 
    if(itsStruct == null) {
      itsStruct = new InspcStruct(this, ds.sParentPath);
    }
    return itsStruct; 
  }
  
  
  /**Gets the struct for a node (non leafe variable).
   * If it is a leaf variable, this method returns null because the struct is only given for a node in the tree.
   * It is possible that this method returns null on a non-leaf variable, if the struct is not defined till now.
   * If it is guaranteed that this is a non-leaf-variable, invoke {@link #getOrCreateStructForNonLeafVariables()}.
   * @return null on a leaf-variable, the defined struct (maybe null) for a non-leaf variable.
   */
  public InspcStruct getStruct(){ return itsStruct; }
  
  @Override public void setRefreshed(long time){ timeRefreshed = time; }

  @Override public long getLastRefreshTime(){ return timeRefreshed; }

  /**Request with the current time. This is more simple for usage in JZcmd. */
  public void requestValue(){ requestValue(System.currentTimeMillis(), null); }
  
  @Override public void requestValue(long time){ requestValue(time, null); }
 
  @Override public void requestValue(long time, Runnable run)
  { ///
    if(time == 0) this.timeRequested = System.currentTimeMillis();
    long timeLast = time - timeRequested;
    if(  this.timeRequested == 0 //never requested, request now.
      || (this.timeRefreshed !=0 && (this.timeRefreshed - this.timeRequested) >= 0)  //already refreshed 
      || (timeLast) >= 5000  //requested for a longer time.
      ) {
      //request newly only if it was requested 
      this.timeRequested = time;   //request it!
      this.isRequested = false;    //it should be requested, but it is not requested yet.
    }
    if(run !=null){
      int catastrophicCount = 10;
      while(this.runOnRecv.remove(run)){  //prevent multiple add 
        if(--catastrophicCount <0){ throw new IllegalArgumentExceptionJc("InspcVariable - requestValue catastrophicalCount", run.hashCode()); }
      }
      boolean offerOk = this.runOnRecv.offer(run);
      if(ds.sDataPath.equals("CCS:_DSP_.ccs_1P.ccs_IB_priv.ictrl.pire_p.out.YD")) {
        System.out.println("InspcVariable.execInspcRxOrder - requestValue, " + (this.timeRequested - System.currentTimeMillis())/1000.0f + ", " + ds.sDataPath);
        Debugutil.stop();
      }  
      if(!offerOk){ throw new IllegalArgumentExceptionJc("InspcVariable - requestValue run cannot be added", run.hashCode()); }
    }
  }
  
  
  
  
  @Override public boolean isRequestedValue(long timeEarlyRequested, boolean retryFaultyVariables){
    if(ds.sDataPath.equals("Sim94:simTime.timeShort"))
      Debugutil.stop();
    if(timeRequested == 0 /* || isRequested*/) return false;  //never requested
    //NOTE isRequested: Don't request twice if there is not an answer ? Then timeRefreshed is not set or increased.
    //It is not proper to continue a request.
    if(modeTarget == ModeHandleVariable.kIdTargetDisabled && !retryFaultyVariables) 
      return false;
    long timeNew = timeRefreshed == 0 ? 1 : timeRequested - timeRefreshed;  //in newer time requested.
    long timeReq1 = timeRequested - timeEarlyRequested;
    boolean bReq = timeNew > 0 && timeReq1 >=0;
    if(bReq) {
      //System.out.println("InspcVariable.isRequestedValue, " + this);
      isRequested = true;
    }
    return bReq;  //for latest 5 seconds requested
  }
  
  @Override public boolean isRefreshed(){ 
    long timeAnswer = timeRefreshed - timeRequested;
    boolean isRefreshed = timeRefreshed != 0 && (timeAnswer ) >=0;
    if(isRefreshed) 
      Debugutil.stop();
    return isRefreshed; 
  }

  
  
  @Override public String toString(){ 
    StringBuilder u = new StringBuilder();
    u.append("Variable: ");
    if(ds !=null){ u.append(ds.sDataPath); }
    u.append(" m=").append(modeTarget)
      .append(" h=").append(Integer.toHexString(handleTarget));
    return u.toString();
  }


  @Override public int getInt(int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override public int setInt(int value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override public long getLong(int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override public long setLong(long value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override public float getFloat(int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override public float setFloat(float value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override public double getDouble(int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override public double setDouble(double value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }


  @Override public String getString(int... ixArray)
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override public String setString(String value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override public int getDimension(int dimension)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  
  private void setValueFormAnswerTelgByHandle(InspcDataExchangeAccess.Inspcitem accAnswerItem, long time)
  {
    int cmd = accAnswerItem.getCmd();
    if(cmd == InspcDataExchangeAccess.Inspcitem.kFailedHandle) {
      handleTarget = 0;  //request a new one.
      modeTarget = ModeHandleVariable.kTargetNotSet;
    } else {
      if(typeTarget <= InspcDataExchangeAccess.kLengthAndString){
        int nrofChars = accAnswerItem.getChildInt(1);
        valueS = accAnswerItem.getChildString(nrofChars); 
      } else {
        switch(typeTarget){
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_int   : valueI = accAnswerItem.getChildInt(-4); valueF = valueI; break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_int64 : { long val = accAnswerItem.getChildInt(-8); valueI = (int) val; valueF = val; } break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_int32 : valueI = accAnswerItem.getChildInt(-4); valueF = valueI; break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_int16 : valueI = accAnswerItem.getChildInt(-2); valueF = valueI; break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_int8  : valueI = accAnswerItem.getChildInt(-1); valueF = valueI; break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_uint  : {long val = accAnswerItem.getChildInt(4); valueI = (int)val; valueF = val; } break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_uint64: {long val = accAnswerItem.getChildInt(4); valueI = (int)val; valueF = val < 0 ? (float)val + (65536.0F * 65536.0F * 65536.0F * 32768.0F) : (float)val ; } break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_uint32: {long val = accAnswerItem.getChildInt(4); valueI = (int)val; valueF = val; } break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_uint16: valueI = accAnswerItem.getChildInt(2); valueF = valueI; break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_uint8 : valueI = accAnswerItem.getChildInt(1); valueF = valueI; break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_float : valueF = accAnswerItem.getChildFloat(); valueI = (int)valueF; break;
          case InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_double: { double val = accAnswerItem.getChildDouble(); valueI = (int)val; valueF = (float)val; } break;
          default: System.err.println("Error InspcVariable.setValueFormAnswerTelgByHandle - faulty type");
        }
      }
      timeRefreshed = time;
    }
          
  }

}
