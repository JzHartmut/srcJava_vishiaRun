package org.vishia.msgDispatch;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.bridgeC.Va_list;
import org.vishia.mainCmd.Report;
import org.vishia.zbnf.ZbnfJavaOutput;

/**This class holds all configuarion informations about messages. */
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

  
  public static class MsgConfigItem
  {
    public String text;
    
    public int identNr;
    
    public String dst;
    
    private char type_;
    
    public void set_type(String src){ type_=src.charAt(0); }
  }
  
  
  public static class MsgConfigZbnf
  { public final List<MsgConfigItem> item = new LinkedList<MsgConfigItem>();
  }
  
  
  /**Index over all ident numbers. */
  private final Map<Integer, MsgConfigItem> indexIdentNr = new TreeMap<Integer, MsgConfigItem>(); 
  
  
  public MsgConfig(Report log, String sPathZbnf)
  {
    ZbnfJavaOutput parser = new ZbnfJavaOutput(log);
    MsgConfigZbnf rootParseResult = new MsgConfigZbnf();
    File fileConfig = new File(sPathZbnf + "/msg.cfg");
    File fileSyntax = new File(sPathZbnf + "/msgCfg.zbnf");
    String sError = parser.parseFileAndFillJavaObject(MsgConfigZbnf.class, rootParseResult, fileConfig, fileSyntax);
    if(sError != null){
      log.writeError(sError);
    } else {
      //success parsing
      for(MsgConfigItem item: rootParseResult.item){
        indexIdentNr.put(item.identNr, item);
      }
      log.writeInfoln("message-config file "+ fileConfig.getAbsolutePath() + " red, " + indexIdentNr.size() + " entries.");
    }
  }
  
  
  public boolean setMsgDispaching(MsgDispatcher msgDispatcher, String chnChars){

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
    System.err.println("MsgReceiver - test message; test");
    return true;
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
