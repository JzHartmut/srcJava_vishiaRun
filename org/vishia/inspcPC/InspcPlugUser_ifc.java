package org.vishia.inspcPC;


/**This is a callback or plug interface to inform a plugged instance about some things from the {@link InspcMng}.
 * @author Hartmut Schorrig
 *
 */
public interface InspcPlugUser_ifc// extends GralPlugUser_ifc
{
  
  /**One of the state which should be shown in the application. */
  enum TargetState{ inactive, idle, waitReceive, receive}; 
  
  /**Show the state of target communication. */
  void showStateInfo(String key, TargetState state, int count, float[] cycle_timeout);
  
  void setInspcComm(InspcAccess_ifc inspcMng);
  
  /**This method is called periodically on start of requesting data all widgets in visible windows.
   * It is possible to get some special data here.
   * @param ident Any identification depending of the caller. It should be understand by the user algorithm.
   */
  void requData(int ident);
  
  void isSent(int seqnr);
  
}
