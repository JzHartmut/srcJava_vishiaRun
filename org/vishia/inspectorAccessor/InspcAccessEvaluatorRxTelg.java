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
    int currentPos = telgHead.getLength();  //length from ByteDataAccess-management.
    assert(currentPos == InspcDataExchangeAccess.Datagram.sizeofHead);
    int nrofBytesTelg = telgHead.getLengthDatagram();
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
      }
    }
    return sError;
  }
  
  
}
