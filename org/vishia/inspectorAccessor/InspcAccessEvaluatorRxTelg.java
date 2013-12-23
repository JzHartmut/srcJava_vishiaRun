package org.vishia.inspectorAccessor;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.reflect.ClassJc;
import org.vishia.util.Assert;

/**This class helps to evaluate any telegram.
 * @author Hartmut Schorrig
 *
 */
public class InspcAccessEvaluatorRxTelg
{
  
  private static class OrderWithTime
  {
    final int order;
    final long time;
    final InspcAccessExecRxOrder_ifc exec;
    
    public OrderWithTime(int order, long time, InspcAccessExecRxOrder_ifc exec)
    { this.order = order;
      this.time = time;
      this.exec = exec;
    }
    
  }
  

  final Map<Integer, OrderWithTime> ordersExpected = new TreeMap<Integer, OrderWithTime>();
  
  OrderWithTime orderGetFields;
  
  final Deque<OrderWithTime> listTimedOrders = new LinkedList<OrderWithTime>();
  
  /**Reused instance to evaluate any info blocks.
   * 
   */
  InspcDataExchangeAccess.Reflitem infoAccess = new InspcDataExchangeAccess.Reflitem();
  
  
  public InspcAccessEvaluatorRxTelg()
  {
    
  }
  
  
  /**Sets a expected order in the index of orders.
   * @param order The unique order number.
   * @param exec The execution for the answer. 
   */
  public void setExpectedOrder(int order, InspcAccessExecRxOrder_ifc exec)
  {
    OrderWithTime timedOrder = new OrderWithTime(order, System.currentTimeMillis(), exec);
    //listTimedOrders.addFirst(timedOrder);
    ordersExpected.put(order, timedOrder);
  }
  
  
  /**Clean up the order list.
   * @param timeOld The time before that the orders are old. 
   * @return number of deleted orders.
   */
  public int checkAndRemoveOldOrders(long timeOld)
  {
    int removedOrders = 0;
    boolean bRepeatBecauseModification;
    do{
      bRepeatBecauseModification = false;
      //if(ordersExpected.size() > 10){
        Set<Map.Entry<Integer, OrderWithTime>> list = ordersExpected.entrySet();
        try{
          Iterator<Map.Entry<Integer, OrderWithTime>> iter = list.iterator();
          while(iter.hasNext()){
            Map.Entry<Integer, OrderWithTime> entry = iter.next();
            OrderWithTime timedOrder = entry.getValue();
            if(timedOrder.time < timeOld){
              iter.remove();
              removedOrders +=1;
            }
          }
        } catch(Exception exc){ //Repeat it, the list ist modified
          bRepeatBecauseModification = true;
        }
      //}
    } while(bRepeatBecauseModification);
      
    return removedOrders;
  }
  
  
  
  /**Evaluates a received telegram.
   * @param telgHead The telegram
   * @param executer if given, than the {@link InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Reflitem)}
   *        -method is called for any info block.<br>
   *        If null, then the order is searched like given with {@link #setExpectedOrder(int, InspcAccessExecRxOrder_ifc)}
   *        and that special routine is executed.
   * @return null if no error, if not null then it is an error description. 
   */
  public String evaluate(InspcDataExchangeAccess.ReflDatagram telgHead, InspcAccessExecRxOrder_ifc executer, long time, LogMessage log, int identLog)
  { String sError = null;
    int currentPos = InspcDataExchangeAccess.ReflDatagram.sizeofHead;
    //for(InspcDataExchangeAccess.ReflDatagram telgHead: telgHeads){
      int nrofBytesTelgInHead = telgHead.getLengthDatagram();
      int nrofBytesTelg = telgHead.getLength();  //length from ByteDataAccess-management.
      //telgHead.assertNotExpandable();
      while(sError == null && currentPos + InspcDataExchangeAccess.Reflitem.sizeofHead <= nrofBytesTelgInHead){
        telgHead.addChild(infoAccess);
        int nrofBytesInfo = infoAccess.getLenInfo();
        if(nrofBytesInfo <8){
          Assert.stop();
        } else {
          if(nrofBytesTelg < currentPos + nrofBytesInfo){
            sError = "to less bytes in telg at " + currentPos + ": " 
                   + nrofBytesInfo + " / " + (nrofBytesTelg - currentPos);
          } else {
            if(executer !=null){
              executer.execInspcRxOrder(infoAccess, time, log, identLog);
            } else {
              int order = infoAccess.getOrder();
              int cmd = infoAccess.getCmd();
              OrderWithTime timedOrder = ordersExpected.remove(order);
              if(cmd == InspcDataExchangeAccess.Reflitem.kAnswerFieldMethod){
                //special case: The same order number is used for more items in the same sequence number.
                if(timedOrder !=null){
                  orderGetFields = timedOrder;
                } else if(orderGetFields !=null && orderGetFields.order == order) {
                  timedOrder = orderGetFields;
                }
              }
              if(timedOrder !=null){
                //remove timed order
                InspcAccessExecRxOrder_ifc orderExec = timedOrder.exec;
                if(orderExec !=null){
                  orderExec.execInspcRxOrder(infoAccess, time, log, identLog);
                } else {
                  stop();  //should not 
                }
              }
              //
              //search the order whether it is expected:
            }
            infoAccess.setLengthElement(nrofBytesInfo);
            //telgHead.setLengthCurrentChildElement(nrofBytesInfo); //to add the next.
            currentPos += nrofBytesInfo;  //the same as stored in telgHead-access
            
          }
        }
      }
    //}
    return sError;
  }
  
  
  
  public static float valueFloatFromRxValue(InspcDataExchangeAccess.Reflitem info, int type)
  {
    float ret = 0;
    if(type >= InspcDataExchangeAccess.kScalarTypes){
      switch(type - InspcDataExchangeAccess.kScalarTypes){
        case org.vishia.reflect.ClassJc.REFLECTION_char16: ret = (char)info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_char8: ret = (char)info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_double: ret = (float)info.getChildDouble(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_float: ret = info.getChildFloat(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int8: ret = info.getChildInteger(-1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int16: ret = info.getChildInteger(-2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int32: ret = info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int64: ret = info.getChildInteger(-8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int: ret = info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint8: ret = info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint16: ret = info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint32: ret = info.getChildInteger(4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint64: ret = info.getChildInteger(8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint: ret = info.getChildInteger(4); break;
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
  
  
  
  

  public static int valueIntFromRxValue(InspcDataExchangeAccess.Reflitem info, int type)
  {
    int ret = 0;
    if(type >= InspcDataExchangeAccess.kScalarTypes){
      switch(type - InspcDataExchangeAccess.kScalarTypes){
        case ClassJc.REFLECTION_char16: ret = (char)info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_char8:  ret = (char)info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_double: ret = (int)info.getChildDouble(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_float:  ret = (int)info.getChildFloat(); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int8:   ret = (int)info.getChildInteger(-1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int16:  ret = (int)info.getChildInteger(-2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int32:  ret = (int)info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int64:  ret = (int)info.getChildInteger(-8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_int:    ret = (int)info.getChildInteger(-4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint8:  ret = (int)info.getChildInteger(1); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint16: ret = (int)info.getChildInteger(2); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint32: ret = (int)info.getChildInteger(4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint64: ret = (int)info.getChildInteger(8); break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint:   ret = (int)info.getChildInteger(4); break;
        case org.vishia.reflect.ClassJc.REFLECTION_boolean:ret = info.getChildInteger(1) == 0 ? 0: 1; break;
      }      
    } else if(type == InspcDataExchangeAccess.kReferenceAddr){
      ret = (int)info.getChildInteger(4);
    } else if(type <= InspcDataExchangeAccess.maxNrOfChars){
      try{
        String sValue = info.getChildString(type);
        ret = Integer.parseInt(sValue);
      } catch(Exception exc){ ret = 0; }
    }

    return ret;
  }
  
  
  /**Gets the reflection type of the received information.
   * 
   * @param info
   * @return The known character Z, C, D, F, B, S, I, J for the scalar types, 'c' for character array (String)
   */
  public static int getInspcTypeFromRxValue(InspcDataExchangeAccess.Reflitem info)
  {
    char ret = 0;
    int type = (int)info.getChildInteger(1);
    return type;
  }
    
    /**Gets the type of the received information.
   * 
   * @param info
   * @return The known character Z, C, D, F, B, S, I, J for the scalar types, 'c' for character array (String)
   */
  public static char getTypeFromInspcType(int type)
  {
    final char ret;
    if(type >= InspcDataExchangeAccess.kScalarTypes){
      switch(type - InspcDataExchangeAccess.kScalarTypes){
        case ClassJc.REFLECTION_char16: ret = 'S'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_char8:  ret = 'C'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_double: ret = 'D'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_float:  ret = 'F'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int8:   ret = 'B'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int16:  ret = 'S'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int32:  ret = 'I'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int64:  ret = 'J'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_int:    ret = 'I'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint8:  ret = 'B'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint16: ret = 'S'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint32: ret = 'I'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint64: ret = 'J'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_uint:   ret = 'I'; break;
        case org.vishia.reflect.ClassJc.REFLECTION_boolean:ret = 'Z'; break;
        default: ret = '?';
      }      
    } else if(type == InspcDataExchangeAccess.kReferenceAddr){
      ret = 'I';
    } else if(type <= InspcDataExchangeAccess.maxNrOfChars){
        ret = 'c';
    } else {
      ret = '?'; //error
    }
    return ret;
  }
  
  
  void lastTelg(){
    if(orderGetFields != null){
      orderGetFields.exec.finitTelg(orderGetFields.order);
    }
  }
  
  
  void stop(){}
  
  
}
