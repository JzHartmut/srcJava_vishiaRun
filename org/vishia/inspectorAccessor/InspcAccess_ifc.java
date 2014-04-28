package org.vishia.inspectorAccessor;

public interface InspcAccess_ifc
{
  /**Version, history and license.
   * <ul>
   * <li>2014-04-24 Hartmut created: Methods from {@link InspcTargetAccessor}
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-04-30";

  
  public int cmdGetFields(String sPathInTarget, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  /**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created.
   */
  public int cmdGetValueByPath(String sPathInTarget, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  
  /**Adds the info block to send 'register by path'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created because the telegram is full.
   */
  public int cmdRegisterByPath(String sPathInTarget, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  
  /**Adds the info block to send 'register by path'
   * @param sPathInTarget
   * @return The order number. 0 if the cmd can't be created because the telegram is full.
   */
  public boolean cmdGetValueByIdent(int ident, InspcAccessExecRxOrder_ifc actionOnRx);
  

  /**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public void cmdSetValueByPath(String sPathInTarget, long value, int typeofValue, InspcAccessExecRxOrder_ifc actionOnRx);
  
  /**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created because the telgram is full.
   */
  public int cmdSetValueByPath(String sPathInTarget, int value);
  
  
  /**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created because the telgram is full.
   */
  public void cmdSetValueByPath(String sPathInTarget, float value, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  /**Adds the info block to send 'get value by path'
   * @param sPathInTarget
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public void cmdSetValueByPath(String sPathInTarget, double value, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  /**Adds the info block to send 'get value by path'
   * @param sPath Either the path inside the target or the path with target identification,
   *   depends on implementation. Maybe possible with alias.
   * @param actionOnRx it will be added to a list with its order. Executed in the receive thread.
   * @throws IllegalArgumentException if too many requests are done without answer. 
   *   Hint: Write all activities in try-catch with a common error log.  
   */
  public void cmdGetAddressByPath(String sPath, InspcAccessExecRxOrder_ifc actionOnRx);





  

}
