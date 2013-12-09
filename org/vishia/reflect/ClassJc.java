package org.vishia.reflect;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.bridgeC.MemSegmJc;

public final class ClassJc
{
  
  /**Version, history and license
   * <ul>
   * <li>2012-04-08 Hartmut new {@link #nrofBytesScalarTypes}, some correction parallel 
   *   to CRuntimeJavalike/.../Jc/ReflectionJc.h,c
   * <li>2009 Hartmut created as adapter for C-programming
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
  public static final int version = 20120409;

  /**Index of all known reflection-classes valid for this application. If any reflection class
   * is gotten, it is known here for all following accesses. Especially the instances for the fields
   * of the class {@link #indexNameFields} shouldn't be created newly if the instance of ClassJc is stored. 
   */
  private final static Map<String, ClassJc> allClasses = new TreeMap<String, ClassJc>(); 
  
  /**Designation of type especially in a embedded system in C-programming.
   * The types identifications are defined in the range from
   * 0 to 0x1f. The limitation is because the {@link org.vishia.communication.InspcDataExchangeAccess}
   * uses designation from 0..0xc7 for Strings with upto 200 character
   * and uses this types in 1 byte with added {@link org.vishia.communication.InspcDataExchangeAccess#kScalarTypes} = 0xe0
   * <br>
   * The type void is used especially for a void* pointer. 
   */
  public static final int REFLECTION_void  =              0x01; 
  
  /**Designation of the integer types with defined bit width. In C signed and unsigned are differenced.
   * To process an unsigned uint32 in Java, a long is necessary usually.
   */
  public static final int REFLECTION_int64  =             0x02; 
  public static final int REFLECTION_uint64 =             0x03;
  public static final int REFLECTION_int32  =             0x04;
  public static final int REFLECTION_uint32 =             0x05;
  public static final int REFLECTION_int16  =             0x06;
  public static final int REFLECTION_uint16 =             0x07;
  public static final int REFLECTION_int8   =             0x08;
  public static final int REFLECTION_uint8  =             0x09;
  /**Don't use this type identification, because it is not defined whether an int variable is
   * 16 or 32 bit. Instead use {@link #REFLECTION_int32} etc.
   * @deprecated
   */
  @Deprecated
  public static final int REFLECTION_int    =             0x0a
                        , REFLECTION_uint   =             0x0b;
  
  /**Designation of the floating types. The bit definition of float should be the same standard
   * like Java, it is usual for C too.
   */
  public static final int REFLECTION_float  =             0x0c,
                          REFLECTION_double =             0x0d;
  
  /**A character in C or in byte structures has 8 bit usually. Therefore the number of bytes of a
   * type designated with REFLECTION_char8 is 1. To convert it to a Java char,
   * the encoding should be regarded. A 16-bit-char should be an UTF16. It has 2 bytes.
   */
  public static final int REFLECTION_char8 =              0x0e,
                          REFLECTION_char16 =             0x0f;
  
  /**TODO what is a String in C. Should be a StringJc with usual 8 bytes.
   * 
   */
  public static final int REFLECTION_String =             0x10;
  
  /**This type identification should not interpret for a byte or integer image in C language. 
   * It is not defined whether an boolean variable is 1, 8, 16 or 32 bit.
   * The boolean presentation is platform-depending. 
   * Maybe its better to use {@link #REFLECTION_int32} etc.
   */
  public static final int REFLECTION_boolean=             0x16;
  
  /**If this designation is used, the bit position and the number of bits are given
   * in a special field.
   */
  public static final int REFLECTION_bitfield =           0x17;
  
  /**Array contains the number of byte which were used if the scalar types with designation 0..0x17
   * are stored in a byte structure. 
   */
  public static final int[] nrofBytesScalarTypes = { 0, 0, 8, 8, 4, 4, 2, 2, 1, 1, 4, 4
    , 4, 8  //float, double
    , 1, 2  //char8, char16
    , 8     //StringJc?
    , 0,0,0,0,0  //0x11..0x15
    , 1     //boolean represented with 1 byte
    , 4     //bitfield supplied with 4 bytes
  };

  /**Composition of the Java-original reflection. It is is null, this instance describes 
   * reflection-data outside of the java-scope, especially for a second respectively
   * remote CPU. */
  private final Class<?> clazz;
  
  /**All fields of this class. */
  private Map<String, FieldJc> indexNameFields;
  
  private FieldJc[] allFields;
  
  private final int modifier;
  
  private final String name;
  
  private ClassJc(Class<?> clazz)
  { this.clazz = clazz;
    modifier = clazz.getModifiers();
    name = clazz.getName();
    allClasses.put(name, this);
    
  }
  
  
  
  void fillAllFields(){
    if(indexNameFields == null){
      Field[] fields = clazz.getDeclaredFields();
      //create the requested aggregations:
      indexNameFields = new TreeMap<String, FieldJc>();
      Class<?> superClazz = clazz.getSuperclass();
      int ix;
      if(false && superClazz !=null){  //wrong idea, field is not existent. TODO evaluate superclass in ClassContent.getFields.
        allFields = new FieldJc[fields.length+1];
        FieldJc fieldJc = new FieldJc(superClazz, "super");
        indexNameFields.put("super", fieldJc);
        ix=0; //last used index
        allFields[ix] = fieldJc;
      } else {
        allFields = new FieldJc[fields.length];
        ix=-1; //last used index, preincremented
      }
      for(Field field: fields){
        String name = field.getName();
        FieldJc fieldJc = new FieldJc(field);
        indexNameFields.put(name, fieldJc);
        allFields[++ix] = fieldJc;
      }
    }
  }
  
  private ClassJc(String name, int modifier)
  {
    this.name = name;
    this.modifier = modifier;
    this.clazz = null;
    allClasses.put(name, this);
  }
  
  
  public static ClassJc getClass(Object obj){ 
    Class<?> clazz = obj.getClass();
    String sName = clazz.getName();
    
    ClassJc ret = allClasses.get(sName);
    if(ret == null){
      MemSegmJc objM = new MemSegmJc(obj, 0);
      ret = new ClassJc(clazz);
    }
    return ret;
  }
  
  
  public static ClassJc fromClass(Class<?> clazz)
  {
    String className = clazz.getName();
    ClassJc clazzJc = allClasses.get(className);
    if(clazzJc == null){
      clazzJc = new ClassJc(clazz);
    }
    return clazzJc;
  }
  
  
  /**Creates or gets the Class with the given name. */
  public static ClassJc forName(String className)
  {
    ClassJc clazzJc = allClasses.get(className);
    if(clazzJc == null){
      //Class clazz = Class.forName(className);
    }
    return clazzJc;
  }
  
  
  
  /**Creates or gets the primitive class with the given Name. */
  public static ClassJc primitive(String className)
  {
    ClassJc clazzJc = allClasses.get(className);
    if(clazzJc == null){
      clazzJc = new ClassJc(className, ModifierJc.mPrimitiv);
    }
    return clazzJc;
  }
  
  
  
  
  public String getName(){ return name; }
  
  public FieldJc[] getDeclaredFields(){
    fillAllFields();
    return allFields;
  }
 
  public FieldJc getDeclaredField(String name) 
  throws NoSuchFieldException
  {
    fillAllFields();
    FieldJc field = indexNameFields.get(name);
    if(field ==null) throw new NoSuchFieldException(name);
    return field;
  }
  
  public boolean isPrimitive(){ return (modifier & ModifierJc.mPrimitiv) != 0 || clazz != null && clazz.isPrimitive(); }
  
  
  /**Returns the basic Class like Field.getClass()
   * @return
   */
  public Class<?> getClazz(){ return clazz; }
  
  
  public ClassJc getEnclosingClass(){
    return null; //TODO
  }
  
}
