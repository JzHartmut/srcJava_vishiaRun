package org.vishia.inspector;

import org.vishia.communication.InspcDataExchangeAccess;

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
  
  public void setCmdSetValueByPath(long value, String path, int order)
  {
    int zPath = path.length();
    int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
    addChildInteger(8, value);
    addChildString(path);
    if(restChars >0) { addChildInteger(restChars, 0); }
    int zInfo = getLength();
    this.setInfoHead(zInfo, InspcDataExchangeAccess.Info.kGetValueByPath, order);
    
  }
  
	
}
