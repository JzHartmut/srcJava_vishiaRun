package org.vishia.inspector;

import org.vishia.communication.InspcDataExchangeAccess;

public class Datagrams
{

	
	public static class CmdGetValue extends InspcDataExchangeAccess.Info
	{
		
		public void set(String path, int order)
		{
			int zPath = path.length();
			int restChars = 4 - (zPath & 0x3);  //complete to a 4-aligned length
			addChildString(path);
			if(restChars >0) { addChildInteger(restChars, 0); }
			int zInfo = getLength();
			this.setInfoHead(zInfo, InspcDataExchangeAccess.Info.kGetValueByPath, order);
			
		}
		
	}
	
	
	
	
}
