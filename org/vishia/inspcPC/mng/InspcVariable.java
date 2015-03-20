package org.vishia.inspcPC.mng;

import java.text.ParseException;
import java.util.Map;

import org.vishia.bridgeC.ConcurrentLinkedQueue;
import org.vishia.bridgeC.IllegalArgumentExceptionJc;
import org.vishia.byteData.VariableAccessArray_ifc;
import org.vishia.byteData.VariableAccess_ifc;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.inspcPC.accTarget.InspcAccessExecRxOrder_ifc;
import org.vishia.inspcPC.accTarget.InspcTargetAccessData;
import org.vishia.inspcPC.accTarget.InspcTargetAccessor;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartScan;

/**This class presents a variable, which is accessed by a {@link InspcTargetAccessor} to get or set its value.
 * The value of the variable is presented with the {@link VariableAccessArray_ifc}. 
 * 
 * @author Hartmut Schorrig
 *
 */
public class InspcVariable implements VariableAccess_ifc
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-12-07 Hartmut new: {@link #sValueToTarget} is set on {@link #setString(String)}. Then the new content
   *   is sent to target to change it there. It is done in the {@link #requestValueFromTarget(long, boolean)} 
   *   because this routine is called if the field is shown. Only if it is shown the change can be done.
   * <li>2013-12-07 Hartmut new: {@link #itsStruct} 
   * <li>2013-12-07 Hartmut chg: In {@link VariableRxAction}: Answer from target with info.cmd = {@link InspcDataExchangeAccess.Inspcitem#kFailedPath} 
   *   disables this variable from data communication. TODO enable with user action if the target was changed (recompiled, restarted etc).
   * <li>2013-12-07 Hartmut chg: In {@link VariableRxAction}: Answer from target with variable type designation = {@link InspcDataExchangeAccess#kInvalidIndex}
   *   The requester should remove that index. Then a new {@link InspcTargetAccessor#cmdRegisterByPath(String, InspcAccessExecRxOrder_ifc)}
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
   * 
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
     * @see org.vishia.inspcPC.accTarget.InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem)
     */
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Inspcitem info, long time, LogMessage log, int identLog)
    {
      //String sShow;
      //int order = info.getOrder();
      int cmd = info.getCmd();
      //if(widgd instanceof GralLed){
        
      //}
      switch(cmd){
        case InspcDataExchangeAccess.Inspcitem.kAnswerRegisterRepeat: {
          int ident = (int)info.getChildInteger(4);
          InspcVariable.this.handleTarget = ident;
          InspcVariable.this.modeTarget = ModeHandleVariable.kTargetUseByHandle;
        } //no break, use next case too!
        //$FALL-THROUGH$
        case InspcDataExchangeAccess.Inspcitem.kAnswerValueByIndex:  //same handling, though only one of some values are gotten.
        case InspcDataExchangeAccess.Inspcitem.kAnswerValue: {
          int typeInspc = InspcTargetAccessor.getInspcTypeFromRxValue(info);
          InspcVariable.this.cType = InspcTargetAccessor.getTypeFromInspcType(typeInspc);
          if(typeInspc == InspcDataExchangeAccess.kTypeNoValue || typeInspc == InspcDataExchangeAccess.kInvalidIndex){
            modeTarget = ModeHandleVariable.kTargetNotSet;  //try again.
          }
          else if(cType == 'c'){ //character String
            valueS = InspcTargetAccessor.valueStringFromRxValue(info, typeInspc);  
          }
          else if("BSI".indexOf(cType) >=0){
            valueI = InspcTargetAccessor.valueIntFromRxValue(info, typeInspc);
            valueF = valueI;
          } else { 
            valueF = InspcTargetAccessor.valueFloatFromRxValue(info, typeInspc);
            valueI = (int)valueF;
          }
          if(log !=null){
            log.sendMsg(identLog, "InspcVariable - receive; variable=%s, type=%c, val = %8X = %d = %f", ds.sPathInTarget, cType, valueI, valueI, valueF);
          }
          varMng.variableIsReceived(InspcVariable.this);
          timeRefreshed = time;
          Runnable runReceived;
          while((runReceived = runOnRecv.poll())!=null){
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

  /**The structure were this variable is member of. Not null if this is not the root variable in a device. */
  public final InspcVariable parent;
  
  /*package private*/ final VariableRxAction rxAction = new VariableRxAction();
  
  public final InspcTargetAccessData ds;
  
  /**It is is a structure, it maybe not null if it is requested.
   * null for a leaf variable, null if the structure was not requested till now.
   * See {@link #struct()}. It creates.
   */
  private InspcStruct itsStruct;

  
  /**Special designations as value of {@link #idTarget} 
   */
  protected final static int kIdTargetUndefined = -1, kIdTargetDisabled = -3; 
  
  /**Special designations as value of {@link #idTarget} 
   */
  protected final static int kIdTargetUsePerPath = -2; 
  
  enum ModeHandleVariable {
    kTargetNotSet, kIdTargetDisabled, kIdTargetUsePerPath, kTargetUseByHandle
  }
  
  ModeHandleVariable modeTarget = ModeHandleVariable.kTargetNotSet;
  
  /**If >=0 then it is the identification of the variable in the target device.
   * if <0 then see {@link #kIdTargetDisabled} etc.
   * The the value can be gotten calling getValueByIdent().
   */
  int handleTarget;
  
  /**Timestamp in milliseconds after 1970 when the variable was requested. 
   * A value may be gotten only if a new request is pending. */
  long timeRequested;
  
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
  public boolean requestValueFromTarget(long timeCurrent, boolean retryDisabledVariable)  
  { //check whether the widget has an comm action already. 
    //First time a widgets gets its WidgetCommAction. Then for ever the action is kept.
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
          ds.targetAccessor.cmdSetValueByPath(sPathComm, value, null);
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
              ds.targetAccessor.cmdSetValueByPath(sPathComm, value, null);
            }
          } catch(ParseException exc){
            
          }
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
        return ds.targetAccessor.cmdGetValueByIdent(this.handleTarget, this.rxAction);
      } else {
        //register the variable in the target system:
        String sPathComm = this.ds.sPathInTarget  + ".";
        if(sPathComm.charAt(0) != '#'){
          Map<String, InspcVariable> idx = varMng.idxRequestedVarFromTarget; 
          idx.put(this.ds.sPathInTarget, this);
          return  ds.targetAccessor.cmdRegisterByPath(sPathComm, this.rxAction) !=0;
        }
      }
    } else if(modeTarget == ModeHandleVariable.kIdTargetDisabled){
      if(retryDisabledVariable && this.ds.sPathInTarget.charAt(0) != '#'){
        modeTarget = ModeHandleVariable.kTargetNotSet;  //in the next step: register or get by path
      }
      return true;  //true because the variable is handled.
    } else {
      //get by handle is not supported:
      String sPathComm = this.ds.sPathInTarget  + ".";
      if(sPathComm.charAt(0) != '#'){
        modeTarget = ModeHandleVariable.kIdTargetUsePerPath;
        Map<String, InspcVariable> idx = varMng.idxRequestedVarFromTarget; 
        idx.put(this.ds.sPathInTarget, this);
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
  public InspcStruct struct() { 
    if(itsStruct == null) {
      itsStruct = new InspcStruct(this, ds.sParentPath);
    }
    return itsStruct; 
  }
  
  
  @Override public void setRefreshed(long time){ timeRefreshed = time; }

  @Override public long getLastRefreshTime(){ return timeRefreshed; }

  @Override public void requestValue(long time){ this.timeRequested = time; }
  
  @Override public void requestValue(long time, Runnable run)
  {
    this.timeRequested = time;
    if(run !=null){
      int catastrophicCount = 10;
      while(this.runOnRecv.remove(run)){  //prevent multiple add 
        if(--catastrophicCount <0){ throw new IllegalArgumentExceptionJc("InspcVariable - requestValue catastrophicalCount", run.hashCode()); }
      }
      boolean offerOk = this.runOnRecv.offer(run);
      if(!offerOk){ throw new IllegalArgumentExceptionJc("InspcVariable - requestValue run cannot be added", run.hashCode()); }
    }
  }
  

  
  
  @Override public boolean isRequestedValue(boolean retryFaultyVariables){
    if(timeRequested == 0) return false;  //never requested
    if(modeTarget == ModeHandleVariable.kIdTargetDisabled && !retryFaultyVariables) 
      return false;
    long timeNew = timeRequested - timeRefreshed;
    return timeNew >0;
  }
  
  @Override public boolean isRefreshed(){ return timeRefreshed != 0 && (timeRefreshed - timeRequested ) >=0; }

  
  
  @Override public String toString(){ 
    StringBuilder u = new StringBuilder();
    u.append("Variable: ").append(ds.sDataPath);
    u.append(" m=").append(modeTarget)
      .append(" h=").append(Integer.toHexString(handleTarget));
    return u.toString();
  }


}