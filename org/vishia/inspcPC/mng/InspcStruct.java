package org.vishia.inspcPC.mng;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.byteData.VariableAccessWithIdx;
import org.vishia.byteData.VariableContainer_ifc;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.inspcPC.accTarget.InspcAccessExecRxOrder_ifc;
import org.vishia.inspcPC.accTarget.InspcTargetAccessor;
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
   * <li>2015-03-20 Hartmut {@link #varOfStruct} 
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

  
  /**This class contains all data which describes a field formally used or not used for access yet.
   * If the field should be used for access the {@link #var} should be gotten respectively created.
   * A field is created as answer of 'cmdGetFields' from a target. In that time the field is not accessed already,
   * only its name and type is known. Only this information are stored here.
   * A field can be created for a known type of struct without access to the target too.
   * A field is more lightweight than a variable.
   */
  public final static class FieldOfStruct {
    
    
    public final InspcStruct struct;
    
    public final String nameShow;
    
    public final String identifier;
    
    public final String type;
    
    public final boolean hasSubstruct;
    
    public final int nrofArrayElements;
    
    //private final InspcStruct substruct;
    
    /**Maybe null if the field was not read until now. Not all fields creates variables. */
    InspcVariable var;
    
    /**Creates a field in a given parent struct. This ctor is used after response to 'getFields' 
     * or if the name of fields are known, especially for standard types.
     * @param parent The struct which contains this field.
     * @param nameShow The name to show in the table.
     * @param identifier The name to use for creating a variable.
     * @param type The type to show in table.
     * @param nrofArrayElements >0 then it is an array type.
     * @param hasSubstruct true then it describes a variable which is a struct. 
     *   The {@link InspcStruct} instance of this field which contains the sub fields will be referenced
     *   if an variable was created with {@link #var}. {@link InspcVariable#itsStruct}. 
     */
    public FieldOfStruct(InspcStruct parent, String nameShow, String identifier, String type, int nrofArrayElements, boolean hasSubstruct){
      this.struct = parent;
      this.nameShow = nameShow;
      this.identifier = identifier;
      this.type = type;
      this.nrofArrayElements = nrofArrayElements;
      this.hasSubstruct = hasSubstruct;
    }
    
    
    /**Creates a field for a given variable. This ctor is used if a field is necessary where a variable is given.
     * TODO store the one time created field in the variable if the variable exists already.
     * @param var
     * @param type The type to show in table.
     * @param nrofArrayElements >0 then it is an array type.
     * @param hasSubstruct true then it describes a variable which is a struct. 
     *   The {@link InspcStruct} instance of this field which contains the sub fields will be referenced
     *   if an variable was created with {@link #var}. {@link InspcVariable#itsStruct}. 
     */
    public FieldOfStruct(InspcVariable var, String type, int nrofArrayElements, boolean hasSubstruct){
      this.struct = var.struct();
      this.identifier = var.ds.sName;
      this.nameShow = identifier;
      this.type = type;
      this.var = var;
      this.nrofArrayElements = nrofArrayElements;
      this.hasSubstruct = hasSubstruct;
    }
    
    
    /**Returns the variable which is assigned to the given field. If the field has not a variable 
     * then the variable will be created with the path of the parent (struct) variable and the name of the field.
     * The concept is: create variable only if they are necessary. It is possible to view a structure 
     * but don't have variable for all fields. This method creates the variable if it is necessary.
     * 
     * @param param the variable of the structure, same as {@link InspcStruct#varOfStruct}.
     * @param container the container where all variables can be gotten with given path. It is the {@link InspcMng}
     * 
     * @return The variable associated to the field which allows communication with the target.
     */
    public InspcVariable variable(InspcVariable parent, VariableContainer_ifc container) {
      if(var == null){
        String sParentPath = parent.ds.sDataPath;
        String sPathVar = sParentPath + (sParentPath.endsWith(":") ? "" : '.') + identifier;
        var = (InspcVariable)container.getVariable(sPathVar);
        
        if(nrofArrayElements >0) {
        /*
          int[] idx = new int[1];
          idx[0] = nrofArrayElements;
          var = new VariableAccessWithIdx(var, idx);
          */
        }
      }
      return var; 
    }
    
    //public InspcStruct substruct(){ return substruct; }
  }
  

  private InspcVariable varOfStruct;

  
  
  private final String path;
  
  //private final InspcTargetAccessor targetAccessor;
  
  //private final InspcStruct parent;
  
  /**Action on receiving getFields from target for each field for this structure variable.
   * This instance invokes the {@link #rxActionGetFields(org.vishia.communication.InspcDataExchangeAccess.Inspcitem, long)}
   * which fills the {@link #fields}.
   */
  public final VariableRxAction rxActionGetFields = new VariableRxAction();
  

  
  List<FieldOfStruct> fields = new ArrayList<FieldOfStruct>();
  
  boolean bRequFields;
  
  boolean bUpdated;
  
  /**All variables which are associated to the fields of this struct. Not all fields have a variable on startup,
   * only on access a variable will be created. That variable is member of the {@link InspcMng#idxAllVars} too. */
  //Map<String, InspcVariable> vars = new TreeMap<String, InspcVariable>();
  
  
  /**Callback for any request. */
  Runnable callback;
  
  InspcStruct(InspcVariable varOfStruct, String path){
    this.path = path;
    //this.targetAccessor = targetAccessor; 
    //this.parent = varOfStruct == null ? null: varOfStruct.struct();
    this.varOfStruct = varOfStruct;
  }

  public String path(){ return path; }
  
  //public InspcStruct parent(){ return parent; }
  
  
  public InspcVariable varOfStruct(InspcMng mng) {
    if(varOfStruct ==null) {
      varOfStruct = (InspcVariable)mng.getVariable(path);
    }
    return varOfStruct; 
  }
  
  
  public void requestFields(Runnable callbackP){ bRequFields = true; this.callback = callbackP; }
  
  public void requestFields(){ 
    fields.clear();
    bUpdated = false;
    bRequFields = true; 
  }
  
  boolean isRequestFields(){ if(bRequFields) { bRequFields = false; return true;} else { return false; } }
  
  
  public boolean isUpdated(){ return bUpdated; }
  
  public Iterable<FieldOfStruct> fieldIter(){ return fields; }
  
  
  void rxActionGetFields(InspcDataExchangeAccess.Inspcitem info, long time, LogMessage log, int identLog){
    //String sShow;
    //int order = info.getOrder();
    int cmd = info.getCmd();
    //if(widgd instanceof GralLed){
      
    //}
    switch(cmd){
      case InspcDataExchangeAccess.Inspcitem.kAnswerFieldMethod: {
        int zString = info.getLenInfo() - 8;
        String sField = info.getChildString(zString); 
        if(log !=null) {
          log.sendMsg(identLog + InspcTargetAccessor.idLogGetFields, "recv getFields %s", sField); 
        }
        int posSepNameType = sField.indexOf(':');
        int posTypeEnd = sField.indexOf("...");
        String nameShow = sField.substring(0, posSepNameType);
        int posSepContainer = nameShow.indexOf('[');
        String name;
        int nrofArrayElements;
        if(posSepContainer >=0) {
          String identContainer = nameShow.substring(posSepContainer+1, nameShow.length()-1);  //without ending ]
          if(identContainer.equals("?")){
            nrofArrayElements = -1;
          } else {
            try{ nrofArrayElements = Integer.parseInt(identContainer); }
            catch(NumberFormatException exc){ nrofArrayElements = 9; }
          }
          name = nameShow.substring(0, posSepContainer);
        } else {
          name = nameShow;
          nrofArrayElements = 0;
        }
        String type = sField.substring(posSepNameType+1, posTypeEnd > posSepNameType ? posTypeEnd : sField.length());
        FieldOfStruct field = new FieldOfStruct(this, nameShow, name, type, nrofArrayElements, posTypeEnd >0);
        fields.add(field);
        bUpdated = true;
        //build all variables   
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
     * @see org.vishia.inspcPC.accTarget.InspcAccessExecRxOrder_ifc#execInspcRxOrder(org.vishia.communication.InspcDataExchangeAccess.Inspcitem)
     */
    @Override public void execInspcRxOrder(InspcDataExchangeAccess.Inspcitem info, long time, LogMessage log, int identLog)
    { rxActionGetFields(info, time, log, identLog);
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
