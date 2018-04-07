package org.vishia.inspcPC.mng;

import java.util.Map;
import java.util.TreeMap;

import org.vishia.byteData.VariableContainer_ifc;

/**This class contains all data which describes a field formally used or not used for access yet.
   * If the field should be used for access the {@link #var} should be gotten respectively created.
   * A field is created as answer of 'cmdGetFields' from a target. In that time the field is not accessed already,
   * only its name and type is known. Only this information are stored here.
   * A field can be created for a known type of struct without access to the target too.
   * A field is more lightweight than a variable.
   */
  public class InspcFieldOfStruct {
    
    
    public final InspcStruct struct;
    
    public final String nameShow;
    
    public final String identifier;
    
    public final String type;
    
    public final boolean hasSubstruct;
    
    public final int nrofArrayElements;
    
    //private final InspcStruct substruct;
    
    /**Maybe null if the field was not read until now. Not all fields creates variables. */
    InspcVariable var;
    
    static Map<String, String> idxPrimitiveTypes;
    
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
    public InspcFieldOfStruct(InspcStruct parent, String nameShow, String identifier, String type, int nrofArrayElements){
      this.struct = parent;
      this.nameShow = nameShow;
      this.identifier = identifier;
      this.type = type;
      this.nrofArrayElements = nrofArrayElements;
      this.hasSubstruct = hasSubstruct();
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
    public InspcFieldOfStruct(InspcVariable var, String type, int nrofArrayElements){
      this.struct = var.getOrCreateStructForNonLeafVariables();
      this.identifier = var.ds.sName;
      this.nameShow = identifier;
      this.type = type;
      this.var = var;
      this.nrofArrayElements = nrofArrayElements;
      this.hasSubstruct = hasSubstruct();
    }
    
    
   boolean hasSubstruct() {
     if(type == null){
       return true;  //a parent node
     }
     if(nrofArrayElements >1) return true;
     if(idxPrimitiveTypes == null) {
       idxPrimitiveTypes = new TreeMap<String, String>();
       idxPrimitiveTypes.put("int", "");
       idxPrimitiveTypes.put("float", "");
       idxPrimitiveTypes.put("int16", "");
       idxPrimitiveTypes.put("String", "");
       idxPrimitiveTypes.put("void", "");
     }
     return idxPrimitiveTypes.get(type) == null;
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
      //if identifier is only an index, append without '.'
      String sPathVar = sParentPath + (identifier.charAt(0)== '[' ? "" : (sParentPath.endsWith(":") ? "" : '.')) + identifier;
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
  
  
  /**Returns the existing variable, do not create a new one, returns null if this field has not a variable yet. */
  public InspcVariable variableExisting(){ return var; }
  
  //public InspcStruct substruct(){ return substruct; }
}
  
