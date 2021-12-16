package org.vishia.inspector.helper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.XmlNodeSimple;


public class GenReflectProAgentExplorerXml
{

	SimpleXmlOutputter xmlOut;
	
	
	XmlNode xmlTop;
	
  //List<File> files = new LinkedList<File>();	
	
  
  File dir;
  
  File[] files;

  
	private FilenameFilter filter = new FilenameFilter(){
		@Override public boolean accept(File dir, String name)
		{return name.endsWith(".rag");
		}
	};

  

  
	public static void main(String[] args)
	{
		GenReflectProAgentExplorerXml main = new GenReflectProAgentExplorerXml();
		main.execute(args[0]);
	}
	
	
	void execute(String sStartDir)
	{
		dir = new File(sStartDir);
		xmlTop = new XmlNodeSimple<Object>("AgentExplorerStructure");
		xmlTop.setAttribute("version", "1.0");
		try{ getInDirectory(dir, xmlTop);
		} catch(XmlException exc){
			exc.printStackTrace(System.out);
		}
		xmlOut = new SimpleXmlOutputter();
		try{
			OutputStreamWriter writer = new java.io.FileWriter("rp_agent_explorer.xml");
		  xmlOut.write(writer, xmlTop);
		  writer.close();
		} catch(IOException exc){
			System.out.println("Problem writing file");
		}
	}
	
	
	
	void getInDirectory(File dir, XmlNode xmlDir) throws XmlException
	{
		files = dir.listFiles();
		for(File file: files){
			if(file.isDirectory()){
				XmlNode xmlDir1 = new XmlNodeSimple<Object>("label");
				xmlDir1.setAttribute("name", file.getName());
				xmlDir1.setAttribute("expanded", "no");
				xmlDir.addContent(xmlDir1);
				getInDirectory(file, xmlDir1);
			} else if(file.getName().endsWith(".rag")){
				//rag file found
				XmlNode xmlFile = new XmlNodeSimple<Object>("agent");
				xmlFile.setAttribute("filename", file.getAbsolutePath());
				xmlFile.setAttribute("type", "1");
				xmlFile.setAttribute("state", "1");
				xmlDir.addContent(xmlFile);
			}
		}
		
	}
}
