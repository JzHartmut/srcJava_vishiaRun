package org.vishia.inspectorAccessor;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.msgDispatch.LogMessage;

/**This class presents a structure in a target system with some fields and methods
 * that is a parent of a {@link InspcVariable}.
 * 
 * @author Hartmut Schorrig
 *
 */
public final class InspcStruct
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-12-20 Hartmut created. 
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
  public static final int version = 20131224;

  
  public final static class Field {
    
    public final String name;
    
    public final String type;
    
    InspcVariable var;
    
    Field(String name, String type){
      this.name = name;
      this.type = type;
    }
    
    public void setVariable(InspcVariable var){ this.var = var; }
    
    public InspcVariable variable(){ return var; }
  }
  
  
  private final String path;
  
  private final InspcTargetAccessor targetAccessor;
  
  private final InspcStruct parent;
  
  /*package private*/ final VariableRxAction rxActionGetFields = new VariableRxAction();
  

  
  List<Field> fields = new ArrayList<Field>();
  
  boolean bRequFields;
  
  boolean bUpdated;
  
  /**All variables which are associated to the fields of this struct. Not all fields have a variable on startup,
   * only on access a variable will be created. That variable is member of the {@link InspcMng#idxAllVars} too. */
  Map<String, InspcVariable> vars = new TreeMap<String, InspcVariable>();
  
  
  /**Callback for any request. */
  Runnable callback;
  
  InspcStruct(String path, InspcTargetAccessor targetAccessor, InspcStruct parent){
    this.path = path;
    this.targetAccessor = targetAccessor; 
    this.parent = parent;
  }

  public String path(){ return path; }
  
  public InspcStruct parent(){ return parent; }
  
  public InspcTargetAccessor targetAccessor(){ return targetAccessor; }
  
  /**Invoked only in {@link InspcVariable#InspcVariable(InspcMng, InspcTargetAccessor, InspcStruct, String)}
   * for a new variable.
   * @param var
   */
  void registerVariable(InspcVariable var){
    vars.put(var.sName, var);
  }
  
  
  public void requestFields(Runnable callbackP){ bRequFields = true; this.callback = callbackP; }
  
  boolean isRequestFields(){ if(bRequFields) { bRequFields = false; return true;} else { return false; } }
  
  
  public boolean isUpdated(){ return bUpdated; }
  
  public Iterable<Field> fieldIter(){ return fields; }
  
  
  void rxActionGetFields(InspcDataExchangeAccess.Reflitem info, long time){
    //String sShow;
    //int order = info.getOrder();
    int cmd = info.getCmd();
    //if(widgd instanceof GralLed){
      
    //}
    switch(cmd){
      case InspcDataExchangeAccess.Reflitem.kAnswerFieldMethod: {
        int zString = info.getLenInfo() - 8;
        try{ 
          String sField = info.getChildString(zString); 
          int posSep = sField.indexOf(':');
          int posTypeEnd = sField.indexOf("...");
          String name = sField.substring(0, posSep);
          String type = sField.substring(posSep+1, posTypeEnd > posSep ? posTypeEnd : sField.length());
          Field field = new Field(name, type);
          fields.add(field);
          bUpdated = true;
          //build all variables   
        } catch(UnsupportedEncodingException exc){
          System.err.println("InspcStruct - unexpected UnsupportedEncodingException while getFields;" +  path);
        }
      } break;
      case InspcDataExchangeAccess.Reflitem.kFailedPath:{
        System.err.println("InspcAccessEvaluatorRxTelg - failed path; " + path);
        fields.clear();
      } break;
      
    }//switch
    
    
    
  }

  
  @Override public String toString(){ return path; }
  
  /**This class supplies the method to set the variable value from a received info block. 
   */
  class VariableRxAction implements InspcAccessExecRxOrder_ifc
  {
     /**This method is called for any info block in the received telegram from target,
     * if this implementing instance is stored on the order.
     * It prepares the value presentation.
     * @see org.vishia.inspectorAccessor.InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Reflitem)
     */
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Reflitem info, long time, LogMessage log, int identLog)
    { rxActionGetFields(info, time);
    }
    
    @Override public void finitTelg(int order){
      bRequFields = false;
      if(callback !=null){
        callback.run();
        callback = null;
      }
    }
  }

  
  

  
}
