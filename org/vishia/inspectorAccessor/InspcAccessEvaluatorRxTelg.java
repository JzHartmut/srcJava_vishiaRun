package org.vishia.inspectorAccessor;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.vishia.communication.InspcDataExchangeAccess;

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
  
  final Deque<OrderWithTime> listTimedOrders = new LinkedList<OrderWithTime>();
  
  /**Reused instance to evaluate any info blocks.
   * 
   */
  InspcDataExchangeAccess.Info infoAccess = new InspcDataExchangeAccess.Info();
  
  
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
   * @param executer if given, than the {@link InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Info)}
   *        -method is called for any info block.<br>
   *        If null, then the order is searched like given with {@link #setExpectedOrder(int, InspcAccessExecRxOrder_ifc)}
   *        and that special routine is executed.
   * @return null if no error, if not null then it is an error description. 
   */
  public String evaluate(InspcDataExchangeAccess.Datagram[] telgHeads, InspcAccessExecRxOrder_ifc executer)
  { String sError = null;
    int currentPos = InspcDataExchangeAccess.Datagram.sizeofHead;
    for(InspcDataExchangeAccess.Datagram telgHead: telgHeads){
      int nrofBytesTelgInHead = telgHead.getLengthDatagram();
      int nrofBytesTelg = telgHead.getLength();  //length from ByteDataAccess-management.
      while(sError == null && currentPos + InspcDataExchangeAccess.Info.sizeofHead <= nrofBytesTelg){
        telgHead.addChild(infoAccess);
        int nrofBytesInfo = infoAccess.getLenInfo();
        if(nrofBytesTelg < currentPos + nrofBytesInfo){
          sError = "to less bytes in telg at " + currentPos + ": " 
                 + nrofBytesInfo + " / " + (nrofBytesTelg - currentPos);
        } else {
          if(executer !=null){
            executer.execInspcRxOrder(infoAccess);
          } else {
            int cmd = infoAccess.getCmd();
            int order = infoAccess.getOrder();
            OrderWithTime timedOrder = ordersExpected.remove(order);
            if(timedOrder !=null){
              //remove timed order
              InspcAccessExecRxOrder_ifc orderExec = timedOrder.exec;
              orderExec.execInspcRxOrder(infoAccess);
            }
            //
            //search the order whether it is expected:
          }
          telgHead.setLengthCurrentChildElement(nrofBytesInfo); //to add the next.
          currentPos += nrofBytesInfo;  //the same as stored in telgHead-access
          
        }
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
  
  
  
  
  public static int valueIntFromRxValue(InspcDataExchangeAccess.Info info)
  {
    int ret = 0;
    int type = (int)info.getChildInteger(1);
    if(type >= InspcDataExchangeAccess.kScalarTypes){
      switch(type - InspcDataExchangeAccess.kScalarTypes){
        case org.vishia.reflect.ClassJc.REFLECTION_char16: ret = (char)info.getChildInteger(2); break;
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
    } else if(type <= InspcDataExchangeAccess.maxNrOfChars){
      try{
        String sValue = info.getChildString(type);
        ret = Integer.parseInt(sValue);
      } catch(Exception exc){ ret = 0; }
    }

    return ret;
  }
  
  
  
  
  
}
