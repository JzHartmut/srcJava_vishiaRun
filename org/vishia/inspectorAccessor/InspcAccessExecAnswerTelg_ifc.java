package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;

/**Interface for a method, which evaluates the answer telegrams from a target.
 * @author Hartmut Schorrig
 *
 */
public interface InspcAccessExecAnswerTelg_ifc
{
  void execInspcRxTelg(InspcDataExchangeAccess.Datagram[] telgs);
}
