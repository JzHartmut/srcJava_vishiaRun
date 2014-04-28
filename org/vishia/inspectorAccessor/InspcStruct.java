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

  
  public final static class FieldOfStruct {
    
    public final String name;
    
    public final String type;
    
    public final boolean hasSubstruct;
    
    private final InspcStruct substruct;
    
    InspcVariable var;
    
    FieldOfStruct(InspcStruct parent, String name, String type, boolean hasSubstruct){
      this.name = name;
      this.type = type;
      this.hasSubstruct = hasSubstruct;
      if(hasSubstruct){
        String pathsub;
        String pathParent = parent.path();
        if(pathParent.endsWith(":")){
          pathsub = pathParent + name; 
        } else {
          pathsub = pathParent + "." + name;
        }
        this.substruct = new InspcStruct(pathsub, parent.targetAccessor(), parent );
      } else {
        this.substruct = null;
      }
    }
    
    public void setVariable(InspcVariable var){ this.var = var; }
    
    public InspcVariable variable(){ return var; }
    
    public InspcStruct substruct(){ return substruct; }
  }
  
  
  private final String path;
  
  private final InspcTargetAccessor targetAccessor;
  
  private final InspcStruct parent;
  
  /*package private*/ final VariableRxAction rxActionGetFields = new VariableRxAction();
  

  
  List<FieldOfStruct> fields = new ArrayList<FieldOfStruct>();
  
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
  
  public Iterable<FieldOfStruct> fieldIter(){ return fields; }
  
  
  void rxActionGetFields(InspcDataExchangeAccess.Inspcitem info, long time){
    //String sShow;
    //int order = info.getOrder();
    int cmd = info.getCmd();
    //if(widgd instanceof GralLed){
      
    //}
    switch(cmd){
      case InspcDataExchangeAccess.Inspcitem.kAnswerFieldMethod: {
        int zString = info.getLenInfo() - 8;
        try{ 
          String sField = info.getChildString(zString); 
          int posSep = sField.indexOf(':');
          int posTypeEnd = sField.indexOf("...");
          String name = sField.substring(0, posSep);
          String type = sField.substring(posSep+1, posTypeEnd > posSep ? posTypeEnd : sField.length());
          FieldOfStruct field = new FieldOfStruct(this, name, type, posTypeEnd >0);
          fields.add(field);
          bUpdated = true;
          //build all variables   
        } catch(UnsupportedEncodingException exc){
          System.err.println("InspcStruct - unexpected UnsupportedEncodingException while getFields;" +  path);
        }
      } break;
      case InspcDataExchangeAccess.Inspcitem.kFailedPath:{
        System.err.println("InspcAccessEvaluatorRxTelg - failed path; " + path);
        fields.clear();
      } break;
      
    }//switch
    
    
    
  }

  
  @Override public String toString(){ return path; }
  
  /**This class supplies the method to set the variable value from a received info block. 
   */
  class VariableRxAction implements InspcAccessExecRxOrder_ifc, Runnable
  {
     /**This method is called for any info block in the received telegram from target,
     * if this implementing instance is stored on the order.
     * It prepares the value presentation.
     * @see org.vishia.inspectorAccessor.InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem)
     */
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Inspcitem info, long time, LogMessage log, int identLog)
    { rxActionGetFields(info, time);
    }
    
    @Override public Runnable callbackOnAnswer(){ return this; }
    
    @Override public void run(){   
      bRequFields = false;
      if(callback !=null){
        callback.run();
        callback = null;
      }
    }
    
  }

  
  

  
}
