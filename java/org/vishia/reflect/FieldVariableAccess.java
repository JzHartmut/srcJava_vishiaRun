package org.vishia.reflect;

import java.lang.reflect.Field;

/**Deprecated use {@link FieldJcVariableAccess}
 *  @author Hartmut Schorrig
 * @Deprecated use {@link FieldJcVariableAccess}
 */
public class FieldVariableAccess extends FieldJcVariableAccess{ 
  
   public FieldVariableAccess(Object instance, Field theField) {
     super(instance, theField);
   }
 
}