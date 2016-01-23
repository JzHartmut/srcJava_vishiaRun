package org.vishia.inspcPC.mng;

import org.vishia.byteData.VariableAccessArray_ifc;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.inspcPC.InspcAccessExecRxOrder_ifc;
import org.vishia.inspcPC.InspcAccess_ifc;
import org.vishia.inspcPC.InspcTargetAccessData;
import org.vishia.reflect.ClassJc;

/**This class stores commands to any target from any application.
 * Used for example for {@link InspcMng#cmdSetValueByPath(String, float, org.vishia.inspcPC.InspcAccessExecRxOrder_ifc)}
 * The preparation of the telegram is only done in one thread. Therefore the command is stored here.
 * 
 * @author Hartmut Schorrig
 *
 */
//package private:
class InspcCmdStore implements InspcAccess_ifc
{
  final InspcMng inspc;
  
  String sDataPath;
  
  int handle;
  
  /**The command like defined in {@link InspcDataExchangeAccess.Inspcitem} */
  int cmd;
  
  /**A value is stored in a long format, as float or double bit image though it is a double or float value.
   * The {@link #typeofValue} contains the type. That is the convention of communication.
   */
  long value; 
  
  String values;
  
  int typeofValue;
  
  InspcAccessExecRxOrder_ifc actionOnRx;

  
  
  public InspcCmdStore(InspcMng inspc)
  {
    this.inspc = inspc;
  }

  @Override public int cmdGetFields(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx)
  { this.sDataPath = sDataPath;
    this.cmd = InspcDataExchangeAccess.Inspcitem.kGetFields;
    this.actionOnRx = actionOnRx;
    return 0;
  }

  @Override public int cmdGetValueByPath(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx)
  {
    this.sDataPath = sDataPath;
    this.cmd = InspcDataExchangeAccess.Inspcitem.kGetValueByPath;
    this.actionOnRx = actionOnRx;
    return 0;
  }

  @Override public int cmdRegisterHandle(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx)
  {
    this.sDataPath = sDataPath;
    this.cmd = InspcDataExchangeAccess.Inspcitem.kRegisterHandle;
    this.actionOnRx = actionOnRx;
    return 0;
  }

  @Override public boolean cmdGetValueByHandle(int ident, InspcAccessExecRxOrder_ifc actionOnRx)
  {
    this.sDataPath = null;
    this.handle = ident;
    this.cmd = InspcDataExchangeAccess.Inspcitem.kGetValueByHandle;
    this.actionOnRx = actionOnRx;
    return false;
  }

  @Override public void cmdSetValueByPath(String sDataPath, long value, int typeofValue, InspcAccessExecRxOrder_ifc actionOnRx)
  {
    this.sDataPath = sDataPath;
    this.typeofValue = typeofValue;
    this.value = value;
    this.cmd = InspcDataExchangeAccess.Inspcitem.kSetValueByPath;
    this.actionOnRx = actionOnRx;
  }

  @Override public int cmdSetValueByPath(String sDataPath, int value)
  { cmdSetValueByPath(sDataPath, value, InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_int32, null);
    return 0;
  }

  @Override public void cmdSetValueByPath(VariableAccessArray_ifc var, String value)
  { throw new RuntimeException("TODO");
  }

  @Override public void cmdSetValueByPath(String sDataPath, float value, InspcAccessExecRxOrder_ifc actionOnRx)
  {
    this.sDataPath = sDataPath;
    this.typeofValue = InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_float;
    this.value = Float.floatToRawIntBits(value);
    this.cmd = InspcDataExchangeAccess.Inspcitem.kSetValueByPath;
    this.actionOnRx = actionOnRx;
  }

  @Override public void cmdSetValueByPath(String sDataPath, double value,
      InspcAccessExecRxOrder_ifc actionOnRx)
  {
    this.sDataPath = sDataPath;
    this.typeofValue = InspcDataExchangeAccess.kScalarTypes + ClassJc.REFLECTION_double;
    this.value = Double.doubleToRawLongBits(value);
    this.cmd = InspcDataExchangeAccess.Inspcitem.kSetValueByPath;
    this.actionOnRx = actionOnRx;
  }

  @Override public boolean cmdGetAddressByPath(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx)
  {
    this.sDataPath = sDataPath;
    this.cmd = InspcDataExchangeAccess.Inspcitem.kGetAddressByPath;
    this.actionOnRx = actionOnRx;
    return true;
  }

  @Override public boolean isOrSetReady(long timeCurrent)
  { throw new IllegalArgumentException("Not implemented here.");
  }

  @Override public void addUserTxOrder(Runnable order)
  { throw new IllegalArgumentException("Not implemented here.");
  }

  @Override public void requestFields(InspcTargetAccessData data,
      InspcAccessExecRxOrder_ifc rxActionGetFields, Runnable runOnReceive)
  { throw new IllegalArgumentException("TODO.");
  }

  @Override public InspcTargetAccessData getTargetAccessFromPath(String sDataPath, boolean strict)
  { throw new IllegalArgumentException("Not implemented here.");
  }

  
  
  /**Executes the stored command in this thread.
   * 
   */
  void exec()
  {
    InspcTargetAccessData acc = inspc.getTargetAccessFromPath(sDataPath, true);
    if(acc !=null) {
      switch(cmd){
        case InspcDataExchangeAccess.Inspcitem.kGetFields:
          acc.targetAccessor.cmdGetFields(acc.sPathInTarget, actionOnRx);
          break;
        case InspcDataExchangeAccess.Inspcitem.kGetValueByPath:
          acc.targetAccessor.cmdGetValueByPath(acc.sPathInTarget, actionOnRx);
          break;
        case InspcDataExchangeAccess.Inspcitem.kSetValueByPath:
          acc.targetAccessor.cmdSetValueByPath(acc.sPathInTarget, value, typeofValue, actionOnRx);  //all value types till int32
          break;
        case InspcDataExchangeAccess.Inspcitem.kGetAddressByPath:
          acc.targetAccessor.cmdGetAddressByPath(acc.sPathInTarget, actionOnRx);
          break;
        case InspcDataExchangeAccess.Inspcitem.kRegisterHandle:
          acc.targetAccessor.cmdRegisterHandle(acc.sPathInTarget, actionOnRx);
          break;
        case InspcDataExchangeAccess.Inspcitem.kGetValueByHandle:
          acc.targetAccessor.cmdGetValueByHandle(handle, actionOnRx);
          break;
        default:
        { //Unknown command - answer is: kFailedCommand.
          System.err.println("InscCmdStore: internal cmd error");
        }   
      }//switch
      
    }
  }
  
  
 }
