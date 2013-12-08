package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.msgDispatch.LogMessage;

/**Interface for a method, which evaluates the answer telegrams from a target.
 * @author Hartmut Schorrig
 *
 */
public interface InspcAccessExecAnswerTelg_ifc
{
  void execInspcRxTelg(InspcDataExchangeAccess.ReflDatagram[] telgs, LogMessage log, int identLog);
}
