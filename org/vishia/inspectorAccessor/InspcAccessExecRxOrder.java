package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;


/**This interface is used to execute anything if any info block is received in a telegram.
 * @author Hartmut Schorrig
 *
 */
public interface InspcAccessExecRxOrder
{

  void execInspcRxOrder(InspcDataExchangeAccess.Info info);
  
}
