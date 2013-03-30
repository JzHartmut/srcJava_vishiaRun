package org.vishia.msgDispatch;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.zbnf.ZbnfJavaOutput;

/**See MsgConfig.
 * @author Hartmut
 * @deprecated use MsgConfig. This uses ZbnfParser, but it may be not necessary for it.
 */
public class MsgConfigZbnf extends MsgConfig
{
  
  final ZbnfMsgConfigItem zbnfItem = new ZbnfMsgConfigItem();
  
  
  final List<MsgConfigItem> listItems = new LinkedList<MsgConfigItem>();
  
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
    String sError = parser.parseFileAndFillJavaObject(this.getClass(), this, fileConfig, fileSyntax);
    if(sError == null){
      //success parsing
      for(MsgConfigItem item: listItems){
        indexIdentNr.put(item.identNr, item);
      }
    }
    return sError;
  }
  

  
  public ZbnfMsgConfigItem new_item(){ zbnfItem.item = new MsgConfigItem(); return zbnfItem; }
  
  public void add_item(ZbnfMsgConfigItem val){ listItems.add(val.item); }
  
  
  
  public static class ZbnfMsgConfigItem{
    public boolean onlyIdent;
    MsgConfigItem item;
    
    /**From Zbnf: sets the text. If [$$<?onyIdent>] was found, the text does not contain the identText-part.
     * If a identText is parsed before and {@link #onlyIdent} was not set, the identText is the start text of the text itself.*/
    public void set_text(String val){ 
      item.text = onlyIdent || item.identText == null ? val : item.identText + val; 
    }
    
    public void set_identText(String val){ item.identText = val; }
    
    
    public void set_identNr(long val){ item.identNr = (int)val; }
    
    
    public void set_identNrLast(long val){ item.identNrLast = (int)val; }
    
    
    public void set_dst(String val){ item.dst = val; }
    
    
    public void set_type(String val){ item.type_ = val.charAt(0); }
  }
  
}
