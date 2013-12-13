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
  
}
