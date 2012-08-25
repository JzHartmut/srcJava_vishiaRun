package org.vishia.reflect;


import java.lang.reflect.Field;

import org.vishia.bridgeC.MemSegmJc;
import org.vishia.byteData.VariableAccess_ifc;

/**This class supports the access to a Java variable with the interface {@link VariableAccess_ifc}.
 * It is proper for the GUI-access.
 * @author Hartmut Schorrig
 *
 */
public class FieldVariableAccess implements VariableAccess_ifc
{

  /**Version, history and license.
   * <ul>
   * <li>2012-04-22 Hartmut adapt: {@link #requestValue(long)} etc. from {@link VariableAccess_ifc}.
   * <li>2010-06-00 Hartmut created. See {@link InspcMng#version}. 
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
   */
  public static final int version = 20120422;

  /**The field in the given instance. */
  //final java.lang.reflect.Field theField;
  
  final FieldJc theField;
  
  
  long timeLastRefreshed;
  
  long timeRequestRefresh;
  
  /**The instance where the field is member of. */
  //final Object instance;
  
  MemSegmJc instance;
  
  /**If 0 then it is a scalar.
   * 
   */
  int nrofArrayElements;
  
  public FieldVariableAccess(Object instance, Field theField)
  { theField.setAccessible(true);
    /*
    int modifiers = theField.getModifiers();
    Class<?> clazz = theField.getType();
    String sTypeName = clazz.getName();
    int posBracket= sTypeName.indexOf('[');
    if(posBracket > 0){
      theField.get
      nrofArrayElements = 1; //instance.length;
    }
    */
    this.theField = new FieldJc(theField);
    this.instance = new MemSegmJc(instance, 0);
  }

  @Override  public int getInt(int ...ixArray)
  { int value ; 
    try{ value = theField.getInt(instance, ixArray);}
    catch(Exception exc){ throw new IllegalArgumentException(exc); }
    return value;
  }

  public long getLong(int ...ixArray)
  { long value ; 
    try{ value = theField.getLong(instance, ixArray);}
    catch(Exception exc){ throw new IllegalArgumentException(exc); }
    return value;
  }

  @Override  public float getFloat(int ...ixArray)
  { float value ; 
    try{ value = theField.getFloat(instance, ixArray);}
    catch(Exception exc){ throw new IllegalArgumentException(exc); }
    return value;
  }

  @Override
  public float setFloat(float value, int... ixArray)
  { try{ value = theField.setFloat(instance, value, ixArray);}
    catch(Exception exc){ throw new IllegalArgumentException(exc); }
    return value;
  }
  
  @Override  public double getDouble(int ...ixArray)
  { double value ; 
    try{ value = theField.getDouble(instance, ixArray);}
    catch(Exception exc){ throw new IllegalArgumentException(exc); }
    return value;
  }

  @Override
  public double setDouble(double value, int... ixArray)
  {
    // TODO Auto-generated method stub
    return value;
  }
  
  @Override  public String getString(int ...ixArray)
  { String sValue ;
    ClassJc typeJc = theField.getType();
    Class<?> type = typeJc.getClazz();
    try{ 
      if(type.isPrimitive()){
        String sType = type.getName();
        char typeChar = sType.charAt(0);
        if("bsi".indexOf(typeChar) >=0){
          int value1 = getInt(ixArray);
          sValue = Integer.toString(value1);
        } else if("l".indexOf(typeChar) >=0){
          long value1 = getLong(ixArray);
          sValue = Long.toString(value1);
        } else if("fd".indexOf(typeChar) >=0){
          double value = getDouble(ixArray);
          final String sFormat;
          if(value < 1.0 && value > -1.0){ sFormat = "%1.5f"; }
          else if(value < 100.0F && value > -100.0F){ sFormat = "% 2.3f"; }
          else if(value < 10000.0F && value > -10000.0F){ sFormat = "% 4.1f"; }
          else if(value < 100000.0F && value > -100000.0F){ value = value / 1000.0F; sFormat = "% 2.3f k"; }
          else { sFormat = "%3.3g"; }
          sValue = String.format(sFormat, value); //Double.toString(value1);
        } else {
          sValue = "TT";
        }
      } else { //any Object
        Object ref = theField.get(instance);
        sValue = ref == null ? "null" : ref.toString();
      }
    } catch(Exception exc){ throw new IllegalArgumentException(exc); }
    return sValue;
}

  @Override
  public int setInt(int value, int ...ixArray)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String setString(String value, int ...ixArray)
  {
    // TODO Auto-generated method stub
    return null;
  }

  
  @Override public char getType(){ 
    return theField.getTypeChar();
    /*
    
    ClassJc typeJc = theField.getType();
    Class<?> type = typeJc.getClazz();
    String sType = type.getName();
    if(type.isPrimitive()){
      return sType.charAt(0);
    } else {
      throw new IllegalArgumentException("does war nich vereinbart, mach'n mer nich.");
      //return '.'; //TODO
    }
    */
  }
  
  @Override public int getDimension(int dimension){
    return 0; //TODO
  }

  @Override public void requestValue(long timeRequested){ timeRequestRefresh = timeRequested; }

  @Override public long getLastRefreshTime(){ return timeLastRefreshed; }

  
  public void setRefreshed(long time){ timeLastRefreshed = time; }

  public long getTimeRequestRefresh(){ return timeRequestRefresh; }

  
  

}
