package org.vishia.inspectorAccessor;

import java.util.Map;

import org.vishia.byteData.VariableAccess_ifc;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.msgDispatch.LogMessage;

public class InspcVariable implements VariableAccess_ifc
{
  
  /**Version, history and license.
   * <ul>
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
  public static final int version = 20120422;



  final InspcMng varMng;
  
  /**This class supplies the method to set the variable value from a received info block. 
   */
  class VariableRxAction implements InspcAccessExecRxOrder_ifc
  {
     /**This method is called for any info block in the received telegram from target,
     * if this implementing instance is stored on the order.
     * It prepares the value presentation.
     * @see org.vishia.inspectorAccessor.InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Info)
     */
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Info info, LogMessage log, int identLog)
    {
      //String sShow;
      //int order = info.getOrder();
      int cmd = info.getCmd();
      //if(widgd instanceof GralLed){
        
      //}
      switch(cmd){
        case InspcDataExchangeAccess.Info.kAnswerRegisterRepeat: {
          int ident = (int)info.getChildInteger(4);
          InspcVariable.this.idTarget = ident;
        } //no break, use next case too!
        //$FALL-THROUGH$
        case InspcDataExchangeAccess.Info.kAnswerValueByIndex:  //same handling, though only one of some values are gotten.
        case InspcDataExchangeAccess.Info.kAnswerValue: {
          int typeInspc = InspcAccessEvaluatorRxTelg.getInspcTypeFromRxValue(info);
          InspcVariable.this.cType = InspcAccessEvaluatorRxTelg.getTypeFromInspcType(typeInspc);
          if("BSI".indexOf(cType) >=0){
            valueI = InspcAccessEvaluatorRxTelg.valueIntFromRxValue(info, typeInspc);
            valueF = valueI;
          } else { 
            valueF = InspcAccessEvaluatorRxTelg.valueFloatFromRxValue(info, typeInspc);
            valueI = (int)valueF;
          }
          if(log !=null){
            log.sendMsg(identLog, "InspcVariable - receive; variable=%s, type=%c, val = %8X = %d = %f", sPath, cType, valueI, valueI, valueF);
          }
          varMng.variableIsReceived(InspcVariable.this);
        } break;
      }//switch
    }
  }

  
  
  /*package private*/ final VariableRxAction rxAction = new VariableRxAction();
  
  /**The path of the variable in the target system. */
  String sPath;

  InspcAccessor targetAccessor;
  
  /**If not -1 then it is the identification of the variable in the target device.
   * The the value can be gotten calling getValueByIdent().
   */
  int idTarget = -1;
  
  /**Timestamp in milliseconds after 1970 when the variable was requested. 
   * A value may be gotten only if a new request is pending. */
  long timeRequested;
  
  long timeRefreshed;
  
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
   * @param sPath The access path.
   */
  InspcVariable(InspcMng mng, String sPath){
    this.varMng = mng;
    this.sPath = sPath;
  }
  
  
  boolean requestValueFromTarget(long timeCurrent)  
  { //check whether the widget has an comm action already. 
    //First time a widgets gets its WidgetCommAction. Then for ever the action is kept.
    if(idTarget !=-1){
      targetAccessor.cmdGetValueByIdent(this.idTarget, this.rxAction);
      return false;
    } else if(idTarget ==-2 || !varMng.bUseGetValueByIndex){
      //get by ident is not supported:
      String sPathComm = this.sPath  + ".";
      Map<String, InspcVariable> idx = varMng.idxRequestedVarFromTarget; 
      idx.put(this.sPath, this);
      return varMng.requestValueByPath(sPathComm, this.rxAction);
    } else {
      //register the variable in the target system:
      String sPathComm = this.sPath  + ".";
      Map<String, InspcVariable> idx = varMng.idxRequestedVarFromTarget; 
      idx.put(this.sPath, this);
      targetAccessor = varMng.registerByPath(sPathComm, this.rxAction);
      return false;  //the value will be gotten in next request.
    }
  }
  
  
  

  
  @Override
  public double getDouble(int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float getFloat(int... ixArray)
  {
    return valueF;
  }

  @Override
  public int getInt(int... ixArray)
  {
    return valueI;
  }

  @Override
  public long getLong(int... ixArray)
  {
    return valueI;
  }

  @Override
  public String getString(int ...ixArray)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double setDouble(double value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float setFloat(float value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int setInt(int value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long setLong(long value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String setString(String value, int ...ixArray)
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  
  
  @Override public char getType(){ return cType; } 
  
  @Override public int getDimension(int dimension){
    return 0; //no dimension
  }


  @Override public long getLastRefreshTime(){ return timeRefreshed; }

  @Override public void requestValue(long time){ this.timeRequested = time; }
  
  @Override public String toString(){ return " Variable(" + sPath + ") "; }

}
