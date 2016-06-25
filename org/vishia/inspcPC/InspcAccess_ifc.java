package org.vishia.inspcPC;

import org.vishia.byteData.VariableAccessArray_ifc;


/**This is the interface to use the Inspector to access any target. This interface is used both 
 * from the central {@link org.vishia.inspcPc.mng.InspcMng} to access all known targets
 * and for the {@link org.vishia.inspcPc.accTarget.InspcTargetAccessor} to access a specific target.
 * 
 * @author Hartmut Schorrig
 *
 */
public interface InspcAccess_ifc
{
  /**Version, history and license.
   * <ul>
   * <li>2014-04-24 Hartmut Methods should all return boolean for success or not, for different reasons.
   *   It is fatal if a command was not sent and it is not known. Implemented only on {@link #cmdGetAddressByPath(String, InspcAccessExecRxOrder_ifc)} yet. 
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
   * <li> But the LPGL is not appropriate for a whole software product,
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

  
  /**Some adding values for the message number for the log system. */ 
  static int idLogGetValueByPath=0, idLogGetValueByIdent=1, idLogGetAddress=2, idLogGetFields=4
    , idLogRegisterByPath=5, idLogSetValueByPath=6, idLogGetOther=9
    , idLogTx=10, idLogRx=11, idLogRxLast=12, idLogRxNotlast=13
    , idLogRxItem = 14, idLogFailedSeq=17, idLogRxRepeat=18, idLogRxError=19;  
  
  /**Some adding values for the message number for the log system. */ 
  static int idLogRcvGetValueByPath=20, idLogRcvGetValueByIdent=21, idLogRcvGetAddress=22, idLogRcvGetFields=24
  , idLogRcvRegisterByPath=25, idLogRcvSetValueByPath=26, idLogRcvGetOther=29;

  
  public int cmdGetFields(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  /**Adds the info block to send 'get value by path'
   * @param sDataPath
   * @return The order number. 0 if the cmd can't be created.
   */
  public int cmdGetValueByPath(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  
  /**Adds the info block to send 'register by path'
   * @param sDataPath
   * @return The order number. 0 if the cmd can't be created because the telegram is full.
   */
  public int cmdRegisterHandle(String sDataPath, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  
  /**Adds the info block to send 'register by path'
   * @param sDataPath
   * @return The order number. 0 if the cmd can't be created because the telegram is full.
   */
  public boolean cmdGetValueByHandle(int ident, InspcAccessExecRxOrder_ifc actionOnRx);
  

  /**Adds the info block to send 'get value by path'
   * @param sDataPath
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public void cmdSetValueByPath(String sDataPath, long value, int typeofValue, InspcAccessExecRxOrder_ifc actionOnRx);
  
  /**Adds the info block to send 'set value by path'
   * @param sDataPath
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created because the telgram is full.
   */
  //public int cmdSetValueByPath(String sDataPath, int value);
  
  
  void cmdSetStringByPath(VariableAccessArray_ifc var, String value);

  
  /**Adds the info block to send 'get value by path'
   * @param sDataPath
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created because the telgram is full.
   */
  public void cmdSetInt32ByPath(String sDataPath, int value, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  /**Adds the info block to send 'get value by path'
   * @param sDataPath
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created because the telgram is full.
   */
  public void cmdSetFloatByPath(String sDataPath, float value, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  /**Adds the info block to send 'get value by path'
   * @param sDataPath
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public void cmdSetDoubleByPath(String sDataPath, double value, InspcAccessExecRxOrder_ifc actionOnRx);
  
  
  /**Adds the info block to send 'get value by path'
   * @param sPath Either the path inside the target or the path with target identification,
   *   depends on implementation. Maybe possible with alias.
   * @param actionOnRx it will be added to a list with its order. Executed in the receive thread.
   * @throws IllegalArgumentException if too many requests are done without answer. 
   *   Hint: Write all activities in try-catch with a common error log.  
   */
  public boolean cmdGetAddressByPath(String sPath, InspcAccessExecRxOrder_ifc actionOnRx);




/**Checks readiness of communication cycle. Returns true if a communication cycle is not pending (finished).
   * It is in state {@link States.StateIdle} or {@link States.StateFilling}
   * Returns false if not all answer telegrams were received from the last request.
   * It is in all other states of {@link States}. 
   * If the time of the last send request was before timeLastTxWaitFor 
   * then it is assumed that the communication was faulty. Therefore all older pending requests are removed.
   * 
   * Note: On InspcMng it return false;
   * @param timeCurrent The current time. It is compared with the time of the last transmit telegram which's answer is expected,
   *   and the timeout. If the timeout is expired, older requests are removed and this routine returns true. 
   *   
   */
  boolean isOrSetReady(long timeCurrent); 
  
  
  
  /**Adds any program snippet which is executed while preparing the telegram for data request from target.
   * After execution the order will be removed.
   * @param order the program snippet.
   */
  void addUserTxOrder(Runnable order);


  /**Set the request for all fields of the given variable. This method is invoked from outer (GUI) 
   * to force {@link #cmdGetFields(String, InspcAccessExecRxOrder_ifc)} in the inspector thread.
   * @param data The variable
   * @param rxActionGetFields Action on gotten fields
   * @param runOnReceive Action should be run if all fields are received, if all datagrams are received for that communication cycle.
   */
  void requestFields(InspcTargetAccessData data, InspcAccessExecRxOrder_ifc rxActionGetFields, Runnable runOnReceive);

  
  
    /**Splits a given full data path with device:datapath maybe with alias:datapath in the device, path, name and returns a struct. 
   * It uses {@link #indexTargetAccessor} to get the target accessor instance.
   * It uses {@link #idxAllStruct} to get the existing {@link InspcStruct} for the variable
   *  
   * @param sDataPath The user given data path maybe with alias, necessary with target.
   *   An alias is written in form "alias:rest.of.path". A device is written "device:rest.of.path".
   *   The distinction between alias and device is done with checking whether the charsequence before :
   *   is detected as alias.
   * @param strict true then throws an error on faulty device, if false then returns null if faulty. 
   * @return structure which contains the device, path, name.
   */
  InspcTargetAccessData getTargetAccessFromPath(String sDataPath, boolean strict);


}
