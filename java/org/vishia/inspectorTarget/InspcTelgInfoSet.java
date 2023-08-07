package org.vishia.inspectorTarget;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.reflect.ClassJc;

/**It is an extra class additonal to {@link InspcDataExchangeAccess.Inspcitem}
 * because the last one is used in a embedded target system translated with Java2C.
 * Not necessary code shouldn't lade it.
 * @author Hartmut Schorrig
 *
 */
public class InspcTelgInfoSet extends InspcDataExchangeAccess.Inspcitem
{
  /**Version, history and license.
   * <ul>
   * <li>2015-01-27 Hartmut: bugfix in telegram length, rest chars for 4-Byte-alignment. Problem only on end of telegram. 
   * If 0 rest chars were need 4 are used, but the telegram byte buffer was to short for that. But in special situations.
   * <li>2012-04-08 Hartmut new: Support of GetValueByIdent
   * <li>2011-05-00 Hartmut created
   * </ul>
   * <br><br>
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
   */
  public static final int version = 20150127;

  
  public void setCmdGetFields(String path, int order)
  {
    int zPath = path.length();
    int restChars = (- zPath) & 0x3;  //complete to a 4-aligned length
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Inspcitem.kGetFields, order);
  }
  
  public static int lengthCmdGetFields(int pathLength){
    int restChars = (- pathLength) & 0x3;  //complete to a 4-aligned length
    int l = InspcDataExchangeAccess.Inspcitem.sizeofHead+ pathLength + restChars;
    return l;
  }

  
  public void setCmdGetValueByPath(String path, int order)
  {
    int zPath = path.length();
    int restChars = (- zPath) & 0x3;  //complete to a 4-aligned length
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Inspcitem.kGetValueByPath, order);
  }
  
  public static int lengthCmdGetValueByPath(int pathLength){
    int restChars = (- pathLength) & 0x3;  //complete to a 4-aligned length
    int l = InspcDataExchangeAccess.Inspcitem.sizeofHead+ pathLength + restChars;
   return l;
  }

  
  /**Adds the info block to send 'get value by path'
   * @param path
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   * @deprecated see {@link org.vishia.inspcPC.accTarget.InspcTargetAccessor#cmdSetValueByPath(String, double)}
   */
  @Deprecated
  public void xxxsetCmdSetValueByPath(String path, long value, int typeofValue, int order)
  {
    int zPath = path.length();
    int restChars = (- zPath) & 0x3;  //complete to a 4-aligned length
    InspcDataExchangeAccess.InspcSetValue accessSetValue = new InspcDataExchangeAccess.InspcSetValue(); 
    addChild(accessSetValue);
    accessSetValue.setLong(value);
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Inspcitem.kSetValueByPath, order);
  }
  
  /**Adds the info block to send 'get value by path'
   * @param path
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public void setCmdSetValueByPath(String path, int value, int order)
  {
    int zPath = path.length();
    int restChars = (- zPath) & 0x3;  //complete to a 4-aligned length
    InspcDataExchangeAccess.InspcSetValue accessSetValue = new InspcDataExchangeAccess.InspcSetValue(); 
    addChild(accessSetValue);
    accessSetValue.setInt(value);
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Inspcitem.kSetValueByPath, order);
  }
  
  /**Adds the info block to send 'get value by path'
   * @param path
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public void setCmdSetValueByPath(String path, float value, int order)
  {
    int zPath = path.length();
    int restChars = (- zPath) & 0x3;  //complete to a 4-aligned length
    InspcDataExchangeAccess.InspcSetValue accessSetValue = new InspcDataExchangeAccess.InspcSetValue(); 
    addChild(accessSetValue);
    accessSetValue.setFloat(value);
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Inspcitem.kSetValueByPath, order);
  }
  
  /**Adds the info block to send 'get value by path'
   * @param path
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public void setCmdSetValueByPath(String path, double value, int order)
  {
    int zPath = path.length();
    int restChars = (- zPath) & 0x3;  //complete to a 4-aligned length
    InspcDataExchangeAccess.InspcSetValue accessSetValue = new InspcDataExchangeAccess.InspcSetValue(); 
    addChild(accessSetValue);
    accessSetValue.setDouble(value);
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Inspcitem.kSetValueByPath, order);
  }
  
  
  public static int lengthCmdSetValueByPath(int pathLength){
    int restChars = (- pathLength) & 0x3;  //complete to a 4-aligned length
    int l = InspcDataExchangeAccess.InspcSetValue.sizeofElement + pathLength  + restChars;
    return l;
  }
  
  
  public void setCmdGetAddressByPath(String path, int order)
  {
    int zPath = path.length();
    int restChars = (- zPath) & 0x3;  //complete to a 4-aligned length
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Inspcitem.kGetAddressByPath, order);
  }
  
  public static int lengthCmdGetAddressByPath(int pathLength){
    int restChars = (- pathLength) & 0x3;  //complete to a 4-aligned length
    int l = InspcDataExchangeAccess.Inspcitem.sizeofHead+ pathLength + restChars;
    return l;
  }
  
	
}
