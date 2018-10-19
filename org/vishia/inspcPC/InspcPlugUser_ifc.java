package org.vishia.inspcPC;

import org.vishia.inspcPC.accTarget.InspcTargetAccessor;

/**This is a callback or plug interface to inform a plugged instance about some things from the {@link InspcMng}.
 * It is especially used and implemented for the InspcGui.
 * @author Hartmut Schorrig
 *
 */
public interface InspcPlugUser_ifc// extends GralPlugUser_ifc
{
  
  /**One of the state which should be shown in the application. */
  enum TargetState{ inactive, idle, waitReceive, receive, openError}; 
  
  /**Show the state of target communication. */
  void showStateInfo(String key, TargetState state, int count, float[] cycle_timeout);
  
  void setInspcComm(InspcAccess_ifc inspcMng);
  
  
  /**Registers a target after opening the communication.
   * @param targetAcc
   */
  void registerTarget(String name, String sAddr, InspcTargetAccessor targetAcc);
  
  /**This method is called periodically on start of requesting data all widgets in visible windows.
   * It is possible to get some special data here.
   * @param ident Any identification depending of the caller. It should be understand by the user algorithm.
   */
  void requData(int ident);
  
  void isSent(int seqnr);
  
}
