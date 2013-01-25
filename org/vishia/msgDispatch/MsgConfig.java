package org.vishia.msgDispatch;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.bridgeC.Va_list;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.zbnf.ZbnfJavaOutput;

/**This class holds all configuration information about messages, especially the message text associated to a ident number. 
 * */
public class MsgConfig implements MsgText_ifc
{

  /**version, history and license:
   * <ul>
   * <li>
   * <li>2010-08-00 Hartmut created 
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
  public final static int version = 20120822;

  
  /**One item for each message. 
   * From Zbnf: This class is used as setting class for Zbnf2Java, therefore all is public. The identifiers have to be used
   * as semantic in the parser script.
   *
   */
  public static class MsgConfigItem
  {
    /**The message text can contain format specifier for the additional values. */
    public String text;
    
    /**The message ident.*/
    public int identNr;
    
    /**Some chars which can specify the destination (output) for the message. */
    public String dst;
    
    private char type_;
    
    public void set_type(String src){ type_=src.charAt(0); }
  }
  
  
  /**From Zbnf: This class is used as setting class for Zbnf2Java, therefore all is public. The identifiers have to be used
   * as semantic in the parser script.
   */
  public static class MsgConfigZbnf
  { public final List<MsgConfigItem> item = new LinkedList<MsgConfigItem>();
  }
  
  
  /**Index over all ident numbers. */
  private final Map<Integer, MsgConfigItem> indexIdentNr = new TreeMap<Integer, MsgConfigItem>(); 
  
  
  /**
   * @param log
   * @param sPathCfg
   */
  public MsgConfig()
  {
  }
  
  
  
  /**Reads the configuration from a file with given syntax.An example for such a syntax file is:
   * <pre>
MsgConfig::= { <item> } \e.

item::= <#?identNr>  <!.?type> <*|\t|\ \ ?dst> <*|\r|\n|\t|\ \ ?text>.
   * </pre>
   * Any line is designated with the semantic 'line'. A line is build by the shown syntax elements. 
   * The semantic have to be used like shown. The semantic identifier are given by the element names of the classes 
   * {@link MsgConfigZbnf} and {@link MsgConfigItem}. The syntax can be another one.
   *  
   * @param fileConfig
   * @param fileSyntax
   * @param log Output while parsing.
   * @return null if successfully, an error hint on error.
   */
  public String readConfig(File fileConfig, File fileSyntax, MainCmdLogging_ifc log){
    ZbnfJavaOutput parser = new ZbnfJavaOutput(log);
    MsgConfigZbnf rootParseResult = new MsgConfigZbnf();
    String sError = parser.parseFileAndFillJavaObject(MsgConfigZbnf.class, rootParseResult, fileConfig, fileSyntax);
    if(sError == null){
      //success parsing
      for(MsgConfigItem item: rootParseResult.item){
        indexIdentNr.put(item.identNr, item);
      }
    }
    return sError;
  }
  

  public int getNrofItems(){ return indexIdentNr.size(); }
  
  
  /**Sets the dispatching of all captured messages.
   * @param msgDispatcher
   * @param chnChars The characters which are associated to dstBits 0x0001, 0x0002 etc in
   *   {@link MsgDispatcher#setOutputRange(int, int, int, int, int)} in respect to the characters
   *   stored in the message config dst field. For example "df" if the dstBit 0x0001 is associated to the display
   *   and the dstBit 0x0002 is associated to an output file and "d" means "Display" and "f" means "File" in the config text.
   * @return The last message ident number which was used by this configuration.
   */
  public int setMsgDispaching(MsgDispatcher msgDispatcher, String chnChars){

    String dstMsg = "";
    int firstIdent = 0, lastIdent = -1;
    for(Map.Entry<Integer,MsgConfig.MsgConfigItem> entry: indexIdentNr.entrySet()){
      MsgConfig.MsgConfigItem item = entry.getValue();
      if(dstMsg.equals(item.dst)){
        lastIdent = item.identNr;
      } else {
        //a new dst, process the last one.
        if(lastIdent >=0){
          setRange(msgDispatcher, dstMsg, firstIdent, lastIdent, chnChars);
        }
        //for next dispatching range: 
        firstIdent = lastIdent = item.identNr;
        dstMsg = item.dst;
      }
    }
    setRange(msgDispatcher, dstMsg, firstIdent, lastIdent, chnChars);  //for the last block.
    //prevent the output of all other messages ???
    //do not so! //setRange(msgDispatcher, "", lastIdent, Integer.MAX_VALUE, chnChars);  //for the last block.
    System.err.println("MsgConfig - test message; test");
    return lastIdent;
  }
  
  
  private void setRange(MsgDispatcher msgDispatcher, String dstMsg, int firstIdent, int lastIdent, String chnChars){
    int dstBits = 0;
    for(int ixChn = 0; ixChn < chnChars.length(); ++ixChn){
      char chnChar = chnChars.charAt(ixChn);
      if(dstMsg.indexOf(chnChar)>=0){ dstBits |= (1<<ixChn); }  //output to file
    }
    msgDispatcher.setOutputRange(firstIdent, lastIdent, dstBits, MsgDispatcher.mSet, 3);
  }

  
  
  @Override public String getMsgText(int ident){
    MsgConfigItem item;
    if(ident < 0) item = indexIdentNr.get(-ident);
    else item = indexIdentNr.get(ident);
    return item !=null ? item.text : null;
  }
  
  
}
