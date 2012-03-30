package org.vishia.inspectorAccessor;

import org.vishia.byteData.VariableAccess_ifc;
import org.vishia.communication.InspcDataExchangeAccess;

public class InspcVariable implements VariableAccess_ifc
{

  final InspcMng varMng;
  
  /**This class joins the GUI-Widget with the inspector communication info block.
   * It is created for any Widget one time if need and used for the communication after that. 
   * The routine {@link #execInspcRxOrder(Info)} is used to show the received values.
   */
  class VariableRxAction implements InspcAccessExecRxOrder_ifc
  {
     /**This method is called for any info block in the received telegram from target,
     * if this implementing instance is stored on the order.
     * It prepares the value presentation.
     * @see org.vishia.inspectorAccessor.InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Info)
     */
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Info info)
    {
      String sShow;
      int order = info.getOrder();
      int cmd = info.getCmd();
      //if(widgd instanceof GralLed){
        
      //}
      if(cmd == InspcDataExchangeAccess.Info.kAnswerValue){
        InspcVariable.this.value = InspcAccessEvaluatorRxTelg.valueFloatFromRxValue(info);
        varMng.variableIsReceived(InspcVariable.this);
      }
    }
  }
 

  final VariableRxAction rxAction = new VariableRxAction();
  
  /**The path of the variable in the target system. */
  String sPath;
  
  long timeRequested;
  
  
  float value;
  
  
  InspcVariable(InspcMng mng, String sPath){
    this.varMng = mng;
    this.sPath = sPath;
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
    timeRequested = System.currentTimeMillis();
    return value;
  }

  @Override
  public int getInt(int... ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getString(int ixArray)
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
  public String setString(String value, int ixArray)
  {
    // TODO Auto-generated method stub
    return null;
  }
  
}
