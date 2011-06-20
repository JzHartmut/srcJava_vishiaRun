package org.vishia.inspector;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.reflect.ClassJc;

/**It is an extra class additonal to {@link InspcDataExchangeAccess.Info}
 * because the last one is used in a embedded target system translated with Java2C.
 * Not necessary code shouldn't lade it.
 * @author Hartmut Schorrig
 *
 */
public class InspcTelgInfoSet extends InspcDataExchangeAccess.Info
{
  public void setCmdGetValueByPath(String path, int order)
  {
    int zPath = path.length();
    int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Info.kGetValueByPath, order);
  }
  
  /**Adds the info block to send 'get value by path'
   * @param path
   * @param value The value as long-image, it may be a double, float, int etc.
   * @param typeofValue The type of the value, use {@link InspcDataExchangeAccess#kScalarTypes}
   *                    + {@link ClassJc#REFLECTION_double} etc.
   * @return The order number. 0 if the cmd can't be created.
   */
  public void setCmdSetValueByPath(String path, long value, int typeofValue, int order)
  {
    int zPath = path.length();
    int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    InspcDataExchangeAccess.SetValue accessSetValue = new InspcDataExchangeAccess.SetValue(); 
    addChild(accessSetValue);
    accessSetValue.setLong(value);
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Info.kSetValueByPath, order);
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
    int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    InspcDataExchangeAccess.SetValue accessSetValue = new InspcDataExchangeAccess.SetValue(); 
    addChild(accessSetValue);
    accessSetValue.setFloat(value);
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Info.kSetValueByPath, order);
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
    int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    InspcDataExchangeAccess.SetValue accessSetValue = new InspcDataExchangeAccess.SetValue(); 
    addChild(accessSetValue);
    accessSetValue.setDouble(value);
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Info.kSetValueByPath, order);
  }
  
  public void setCmdGetAddressByPath(String path, int order)
  {
    int zPath = path.length();
    int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Info.kGetAddressByPath, order);
  }
  
	
}
