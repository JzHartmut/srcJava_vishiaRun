package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.msgDispatch.LogMessage;


/**This interface is used to execute anything if any info block is received in a telegram.
 * @author Hartmut Schorrig
 *
 */
public interface InspcAccessExecRxOrder_ifc
{

  void execInspcRxOrder(InspcDataExchangeAccess.Reflitem info, long time, LogMessage log, int identLog);
  
  /**It is called after evaluating the sequence of answer telegrams. 
   * Especially if more as one answer item is expected, it determines the end of answers.
   * @param order The order of request.
   */
  void finitTelg(int order);
  
}
