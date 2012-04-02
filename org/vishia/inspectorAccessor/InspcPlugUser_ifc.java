package org.vishia.inspectorAccessor;


public interface InspcPlugUser_ifc// extends GralPlugUser_ifc
{
  
  //void XXXsetInspcComm(InspcGuiComm inspcCommP);
  
  void setInspcComm(InspcMng inspcMng);
  
  /**This method is called periodically on start of requesting data all widgets in visible windows.
   * It is possible to get some special data here.
   * @param ident Any identification depending of the caller. It should be understand by the user algorithm.
   */
  void requData(int ident);
  
  void isSent(int seqnr);
  
}
