package org.vishia.inspectorAccessor;

import java.util.Map;

import org.vishia.bridgeC.ConcurrentLinkedQueue;
import org.vishia.bridgeC.IllegalArgumentExceptionJc;
import org.vishia.byteData.VariableAccessArray_ifc;
import org.vishia.byteData.VariableAccess_ifc;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.msgDispatch.LogMessage;

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
  public static final int version = 20131224;

  final InspcStruct itsStruct;

  final InspcMng varMng;
  
  /**This class supplies the method to set the variable value from a received info block. 
   */
  class VariableRxAction implements InspcAccessExecRxOrder_ifc
  {
     /**This method is called for any info block in the received telegram from target,
     * if this implementing instance is stored on the order.
     * It prepares the value presentation.
     * @see org.vishia.inspectorAccessor.InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem)
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
          InspcVariable.this.idTarget = ident;
        } //no break, use next case too!
        //$FALL-THROUGH$
        case InspcDataExchangeAccess.Inspcitem.kAnswerValueByIndex:  //same handling, though only one of some values are gotten.
        case InspcDataExchangeAccess.Inspcitem.kAnswerValue: {
          int typeInspc = InspcAccessEvaluatorRxTelg.getInspcTypeFromRxValue(info);
          InspcVariable.this.cType = InspcAccessEvaluatorRxTelg.getTypeFromInspcType(typeInspc);
          if(typeInspc == InspcDataExchangeAccess.kTypeNoValue || typeInspc == InspcDataExchangeAccess.kInvalidIndex){
            idTarget = 0;  //try again.
          }
          else if("BSI".indexOf(cType) >=0){
            valueI = InspcAccessEvaluatorRxTelg.valueIntFromRxValue(info, typeInspc);
            valueF = valueI;
          } else { 
            valueF = InspcAccessEvaluatorRxTelg.valueFloatFromRxValue(info, typeInspc);
            valueI = (int)valueF;
          }
          if(log !=null){
            log.sendMsg(identLog, "InspcVariable - receive; variable=%s, type=%c, val = %8X = %d = %f", sPathInTarget, cType, valueI, valueI, valueF);
          }
          varMng.variableIsReceived(InspcVariable.this);
          timeRefreshed = time;
          Runnable runReceived;
          while((runReceived = runOnRecv.poll())!=null){
            runReceived.run();
          }
        } break;
        case InspcDataExchangeAccess.Inspcitem.kFailedPath:{
          System.err.println("InspcAccessEvaluatorRxTelg - failed path; " + sPathInTarget);
          idTarget = kIdTargetDisabled;
        } break;
        
      }//switch
    }
    
    @Override public void finitTelg(int order){}  //empty

  }

  
  
  /*package private*/ final VariableRxAction rxAction = new VariableRxAction();
  
  /**The path and name of the variable in the target system. */
  final String sPathInTarget, sName;

  final InspcTargetAccessor targetAccessor;
  
  /**Special designations as value of {@link #idTarget} 
   */
  protected final static int kIdTargetUndefined = -1, kIdTargetDisabled = -3; 
  
  /**Special designations as value of {@link #idTarget} 
   */
  protected final static int kIdTargetUsePerPath = -2; 
  
  /**If >=0 then it is the identification of the variable in the target device.
   * if <0 then see {@link #kIdTargetDisabled} etc.
   * The the value can be gotten calling getValueByIdent().
   */
  int idTarget = kIdTargetUndefined;
  
  /**Timestamp in milliseconds after 1970 when the variable was requested. 
   * A value may be gotten only if a new request is pending. */
  long timeRequested;
  
  long timeRefreshed;
  
  private final ConcurrentLinkedQueue<Runnable> runOnRecv = new ConcurrentLinkedQueue<Runnable>();
  
  /**The value from the target device. */
  float valueF;

  /**The value from the target device. */
  int valueI;
  
  /**The type depends from the type in the target device. It is set if any answer is gotten. 
   * 'c' for character array. */
  char cType = 'F';
  
  /**Creates a variable. A variable is an entity, which will be gotten with one access to the 
   * target device. It may be a String or a short static array too.
   * 
   * @param mng
   * @param sPathInTarget The access path.
   */
  InspcVariable(InspcMng mng, InspcTargetAccessor targetAccessor, InspcStruct itsStruct, String sDataPath, String sName){
    this.varMng = mng;
    this.itsStruct = itsStruct;
    this.targetAccessor = targetAccessor;
    this.sPathInTarget = sDataPath;
    this.sName = sName;
    itsStruct.registerVariable(this);
  }
  
  
  /**Notes the request for this variable in the request telegram to the target.
   * @param retryDisabledVariable true then retry a disabled variable, see {@link #kIdTargetDisabled}
   * @return order if the datagram item is set. 0 if the datagram is full. -1 if there is nothing to send.
   */
  public boolean requestValueFromTarget(long timeCurrent, boolean retryDisabledVariable)  
  { //check whether the widget has an comm action already. 
    //First time a widgets gets its WidgetCommAction. Then for ever the action is kept.
    if(idTarget >= 1 && varMng.bUseGetValueByIndex){
      return targetAccessor.cmdGetValueByIdent(this.idTarget, this.rxAction);
    } else if(idTarget == kIdTargetDisabled){
      if(retryDisabledVariable){
        idTarget = kIdTargetUndefined;  //in the next step: register or get by path
      }
      return true;  //true because the variable is handled.
    } else if(idTarget == kIdTargetUsePerPath || !varMng.bUseGetValueByIndex){
      //get by ident is not supported:
      String sPathComm = this.sPathInTarget  + ".";
      if(sPathComm.charAt(0) != '#'){
        Map<String, InspcVariable> idx = varMng.idxRequestedVarFromTarget; 
        idx.put(this.sPathInTarget, this);
        return targetAccessor.cmdGetValueByPath(sPathComm, this.rxAction) !=0;
        //return varMng.requestValueByPath(sPathComm, this.rxAction);
      } else return true;  //variable is handled.
    } else {
      //register the variable in the target system:
      String sPathComm = this.sPathInTarget  + ".";
      if(sPathComm.charAt(0) != '#'){
        Map<String, InspcVariable> idx = varMng.idxRequestedVarFromTarget; 
        idx.put(this.sPathInTarget, this);
        return  targetAccessor.cmdRegisterByPath(sPathComm, this.rxAction) !=0;
      }
      return true;   //true because the variable is handled.
    }
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

  @Override
  public String getString()
  {
    // TODO Auto-generated method stub
    return null;
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
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  
  
  @Override public char getType(){ return cType; } 
  
  
  
  public InspcStruct struct() { return itsStruct; }
  
  
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
    if(idTarget == kIdTargetDisabled && !retryFaultyVariables) return false;
    long timeNew = timeRequested - timeRefreshed;
    return timeNew >0;
  }
  

  
  
  @Override public String toString(){ return " Variable(" + sPathInTarget + ") "; }


}
