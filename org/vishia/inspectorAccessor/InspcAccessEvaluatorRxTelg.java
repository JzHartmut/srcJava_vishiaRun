package org.vishia.inspectorAccessor;

import java.util.Map;
import java.util.TreeMap;

import org.vishia.communication.InspcDataExchangeAccess;

/**This class helps to evaluate any telegram.
 * @author Hartmut Schorrig
 *
 */
public class InspcAccessEvaluatorRxTelg
{

  final Map<Integer, InspcAccessExecRxOrder> ordersExpected = new TreeMap<Integer, InspcAccessExecRxOrder>();
  
  
  /**Reused instance to evaluate any info blocks.
   * 
   */
  InspcDataExchangeAccess.Info infoAccess = new InspcDataExchangeAccess.Info();
  
  
  public InspcAccessEvaluatorRxTelg()
  {
    
  }
  
  
  void setExpectedOrder(int order, InspcAccessExecRxOrder exec)
  {
    ordersExpected.put(order, exec);
  }
  
  
  /**Evaluates a received telegram.
   * @param telgHead The telegram
   * @param executer if given, than the {@link InspcAccessExecRxOrder#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Info)}
   *        -method is called for any info block.<br>
   *        If null, then the order is searched like given with {@link #setExpectedOrder(int, InspcAccessExecRxOrder)}
   *        and that special routine is executed.
   * @return null if no error, if not null then it is an error description. 
   */
  String evaluate(InspcDataExchangeAccess.Datagram telgHead, InspcAccessExecRxOrder executer)
  { String sError = null;
    int currentPos = InspcDataExchangeAccess.Datagram.sizeofHead;
    int nrofBytesTelgInHead = telgHead.getLengthDatagram();
    int nrofBytesTelg = telgHead.getLength();  //length from ByteDataAccess-management.
    while(sError == null && currentPos + InspcDataExchangeAccess.Info.sizeofHead <= nrofBytesTelg){
      telgHead.addChild(infoAccess);
      int nrofBytesInfo = infoAccess.getLength();
      if(nrofBytesTelg < currentPos + nrofBytesInfo){
        sError = "to less bytes in telg at " + currentPos + ": " 
               + nrofBytesInfo + " / " + (nrofBytesTelg - currentPos);
      } else {
        if(executer !=null){
          executer.execInspcRxOrder(infoAccess);
        } else {
          int cmd = infoAccess.getCmd();
          int order = infoAccess.getOrder();
          //
          //search the order whether it is expected:
        }
        telgHead.setLengthCurrentChildElement(nrofBytesInfo); //to add the next.
        currentPos += nrofBytesInfo;  //the same as stored in telgHead-access
        
      }
    }
    return sError;
  }
  
  
  
  public static float valueFloatFromRxValue(InspcDataExchangeAccess.Info info)
  {
    float ret = 0;
    int type = (int)info.getChildInteger(1);
    if(type >= InspcDataExchangeAccess.kScalarTypes){
      switch(type - InspcDataExchangeAccess.kScalarTypes){
        case org.vishia.reflect.ClassJc.REFLECTION_char16: ret = (float)(char)info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_char8: ret = (float)(char)info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_double: ret = (float)info.getChildDouble(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_float: ret = (float)info.getChildFloat(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int8: ret = (float)info.getChildInteger(-1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int16: ret = (float)info.getChildInteger(-2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int32: ret = (float)info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int64: ret = (float)info.getChildInteger(-8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int: ret = (float)info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint8: ret = (float)info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint16: ret = (float)info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint32: ret = (float)info.getChildInteger(4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint64: ret = (float)info.getChildInteger(8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint: ret = (float)info.getChildInteger(4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_boolean: ret = info.getChildInteger(1) == 0 ? 0.0f: 1.0f; break;
      }      
    } else if(type <= InspcDataExchangeAccess.maxNrOfChars){
      try{
        String sValue = info.getChildString(type);
        ret = Float.parseFloat(sValue);
      } catch(Exception exc){ ret = 0; }
    }

    return ret;
  }
  
  
  
  
  
}
