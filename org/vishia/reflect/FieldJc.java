package org.vishia.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import org.vishia.bridgeC.MemSegmJc;
import org.vishia.util.Java4C;

/**This class extends the capability of the Java standard class java/lang/reflect/Field.
 * There are some intension to do so. The most important is the compatibility to reflections 
 * in C Language:
 * <br><br>
 * The C language provides a more width variation of referencing and array addressing.
 * In the C-reflection, the access to array elements is part of the FieldJc-methods.
 * All get/setType-methods get an index or more as one index to address an array. So the get/set
 * is executed in the reflection shell. In opposite in Java some operations are need to detect
 * the type, size and dimension of an array to set elements.
 * <br><br>
 * A second approach is the access to a external hardware used especially at C-level. The reflection
 * information for the second hardware may be contained in a host environment, in C. There the
 * reflection processing is executed. The real hardware is attached only with simple questions
 * for root instance, type idents, memory content. They are simple 4-byte-accesses which can be done
 * via a small port for example a Dual-Port-RAM oder CAN-Bus-communication or via Ethernet too.
 * At the real target (second) hardware only some arrays with offsets and types in integer-format
 * are need, and simple algorithm.
 * <br><br>
 * A further approach for C-reflection is working without Object-based structures.
 * <br><br>
 * Another approach is providing reflection information in Java for non-Java platforms. The reflection
 * information are generated from C-header and provides as byte-image. That information
 * can be used
 * <ul>
 * <li>In Java to access data which are given as byte-image form a C-platform.
 * <li>For access to another hardware (second CPU).
 * </ul> 
 * <br><br>
 * It is adequate the functionality of the CRuntimeJavalike-class FieldJc, but for Java application.
 * The following enhancements in comparison with java.lang.reflect.Field are done:
 * <ul>
 * <li>The general strategy to give any instance is: Supply the instance in form 
 *   of a {@link MemSegmJc}-reference instead of a java.lang.Object-reference. There are two reasons:
 *   <ul>
 *   <li>In the C-implementation, non-Object-base structures are supported too. An ObjectJc*-pointer
 *     can't be used in all cases.
 *   <li>To support access to an external hardware, an additional information (segment) is necessary
 *     always proper to the instance reference.
 *   </ul>
 *   The type {@link MemSegmJc} is deployed in C with a segment information and a void*-pointer,
 *   where in Java it is a Object-reference for normal reflection usage, the segment information
 *   and an int-address for access to extern memory with special Interprocesscomm-Mechanism such as
 *   UDP-Telegrams, serial data transmission or access via JNI.   
 * <li>
 *   All get and set-Methods to access field contents have an int... Argument for indexing an array.
 * </ul>
 * 
 * @author Hartmut Schorrig
 *
 */
@SuppressWarnings("unchecked")
public class FieldJc
{
  
  /**Version, history and license.
   * <ul>
   * <li>2012-08-23 Hartmut {@link #getFloat(MemSegmJc, int...)} now supports non-static arrays too. Experience.
   * <li>2008-00-00 Hartmut created
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:<br>
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
   *    but doesn't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you intent to use this source without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  public static final int version = 20120823;

  /**That annotation is used to detect fix-size-arrays. In Java all arrays are dynamically,
   * but to test the reflection access in Java adequate to C static arrays should be emulated. */
  static final Class<Java4C.FixArraySize> annotationJava2CFixArraySize;
  
  /**Initializer-Block for the {@link #annotationJava2CFixArraySize}. */
  static {
    Class<Java4C.FixArraySize> af;
    try{
      af = (Class<Java4C.FixArraySize>) Class.forName("org.vishia.util.Java4C$FixArraySize");
    }catch(ClassNotFoundException exc){ 
      af = null;
    }
    annotationJava2CFixArraySize = af;
  }

  
  /**Aggregation to the original field of reflection from Java. It may be null if the reflections
   * don't come from Java. */
  private final Field field;
  
  /**Name of the field. It is the same like field.getName(), but set if field == null. */
  private final String name; 
  
  /**The type of the field. */
  public final ClassJc type;
  
  final char cPrimitveType;

  
  /**The modifier contains some more informations as java.reflect.field.
   * It is copied from {@link #field}, some more bits are added, see {@link ModifierJc}.
   */
  private int modifier;
  
  /**The size of a static array. Note: The array size may be changed in Java. Only a final array
   * isn't changed. If null then it isn't an array. */
  final int[] staticArraySize;
  
  /**All annotations from {@link #field}. */
  final Annotation[] annotations;
  
    
  /**Helper class containing a template method to get the last array and the last index
   * for indexed access.
   * @param <ArrayType> The type like int[] or short[] 
   */
  class GetArray<ArrayType>
  {
    /**Gets the last array
     * @param ixP Returns the last given index in the variable argument list ix, or 0 if not given.
     * @param obj The instance where this field is member of.
     * @param ix  variable number of given indices for the dimension. If the number of indices
     *            is less then the number of dimension, missed indices are used as 0.
     * @return The last array-reference with the proper type.
     *         To access data the returnValue[ixP[0]] should be used.
     */
    ArrayType getLastArray(int[] ixP, MemSegmJc obj, int ...ix)
    { Object data = null;  //it is the Object of an array-head
      try{ data = field.get(obj.obj());  //the element itself. It is an int[], or float[] or such.
       } catch(IllegalAccessException exc){
         throw new RuntimeException(exc);
       }
      int ix1 = 0;  //index of the dimension
      for(int ixx=0; ixx < staticArraySize.length; ++ixx){  //iterate over dimension
        if(ix.length >= ixx){ ix1 = ix[ixx]; } else { ix1 = 0; }  //if an index isn't given, use 0
        if(ixx < staticArraySize.length -1){
          data = ((Object[])data)[ix1];       //further dimensions: data is the array inside
        } 
      }
      ixP[0] = ix1;
      return (ArrayType)data;  //cast to the array type.
    }
  }

  
  
  
  /**Helper class containing a template method to get and set a value in an array or container
   * for indexed access.
   * @param <ElementType> The type like int[] or short[] 
   */
  abstract class GetSetContainerElement<ElementType, ArrayType>
  {
    /**Helper class containing a template method to get the last array and the last index
     * for indexed access.
     * @param <ArrayType> The type like int[] or short[] 
     */
    class GetArray
    {
      /**Gets the last array
       * @param ixP Returns the last given index in the variable argument list ix, or 0 if not given.
       * @param obj The instance where this field is member of.
       * @param ix  variable number of given indices for the dimension. If the number of indices
       *            is less then the number of dimension, missed indices are used as 0.
       * @return The last array-reference with the proper type.
       *         To access data the returnValue[ixP[0]] should be used.
       */
      ArrayType getLastArray(int[] ixP, MemSegmJc obj, int ...ix)
      { Object data = null;  //it is the Object of an array-head
        try{ data = field.get(obj.obj());  //the element itself. It is an int[], or float[] or such.
         } catch(IllegalAccessException exc){
           throw new RuntimeException(exc);
         }
        int ix1 = 0;  //index of the dimension
        for(int ixx=0; ixx < staticArraySize.length; ++ixx){  //iterate over dimension
          if(ix.length >= ixx){ ix1 = ix[ixx]; } else { ix1 = 0; }  //if an index isn't given, use 0
          if(ixx < staticArraySize.length -1){
            data = ((Object[])data)[ix1];       //further dimensions: data is the array inside
          } 
        }
        ixP[0] = ix1;
        return (ArrayType)data;  //cast to the array type.
      }
    }

    /**Instance containing the template-access method.
     * 
     */
    GetArray getTypedArray = new GetArray();

    
    
    /**Gets a value from a container-field.
     * @param obj The instance containing the field.
     * @param ix indices, may be empty
     * @return The value of the template-type.
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public ElementType getValue(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
      ElementType value = null;
      if(staticArraySize !=null && staticArraySize.length >0){
        int[] ixP = new int[1];
        ArrayType array = getTypedArray.getLastArray(ixP, obj, ix);
        value = getValue(array,ixP[0]);
      } else {
        //value = 0; //field.getInt(obj.obj());
      }
      return value;
    }
    /**Gets a value from a container-field.
     * @param obj The instance containing the field.
     * @param ix indices, may be empty
     * @return The value of the template-type.
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public ElementType setValue(ElementType value, MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
      if(staticArraySize !=null && staticArraySize.length >0){
        int[] ixP = new int[1];
        ArrayType array = getTypedArray.getLastArray(ixP, obj, ix);
        setValue(array, value, ixP[0]);
      } else {
        //value = 0; //field.getInt(obj.obj());
      }
      return value;
    }
    
    abstract ElementType getValue(ArrayType array, int ix);
    
    abstract void setValue(ArrayType array, ElementType element, int ix);
    
  }
  
  
  
  
  /**Creates based on a given field.
   * @param field Any field of reflection. */
  public FieldJc(Field field)
  {
    this.field = field;
    field.setAccessible(true);
    modifier = field.getModifiers();
    this.name = field.getName();
    Class fieldType = field.getType();
    String sTypeName = fieldType.getName();
    if(fieldType.isPrimitive()){
      modifier |= ModifierJc.mPrimitiv;  //designate on field that it is a primitive, like in C-reflection. 
    }
    annotations = field.getAnnotations();
    
    for(Annotation annotation: annotations){
      Class<? extends Annotation> aClazz = annotation.annotationType();
      String name = aClazz.getName();
    }
    //check container types List and Map
    Class[] typeInterfaces = fieldType.getInterfaces();
    if(typeInterfaces !=null){
      for(Class typeInterface: typeInterfaces){
        String sNameifc = typeInterface.getName();
        if(sNameifc.equals("java.util.List")){
          modifier |= ModifierJc.kLinkedListJc;
        } else if(sNameifc.equals("Map")){
          modifier |= ModifierJc.kMapJc;
        }  
      }
    }
    
    if(sTypeName.equals("java.util.List")){
      modifier |= ModifierJc.kLinkedListJc;
    } else if(sTypeName.equals("java.util.Map")){
      modifier |= ModifierJc.kMapJc;
    }  
    
    
    
    if(sTypeName.charAt(0)== '['){
      //An array
      char typeChar =0;
      int ixArray = 0;
      try{ 
        while( (typeChar = sTypeName.charAt(ixArray)) == '['){
          ixArray +=1;
        } //count depths of array.
        this.cPrimitveType = typeChar;
        
        switch(typeChar){
        case 'B': type = ClassJc.primitive("byte"); break;
        case 'S': type = ClassJc.primitive("short"); break;
        case 'I': type = ClassJc.primitive("int"); break;
        case 'J': type = ClassJc.primitive("long"); break;
        case 'L': type = ClassJc.primitive("long"); break;  //it is wrong. J is correct.
        case 'F': type = ClassJc.primitive("float"); break;
        case 'D': type = ClassJc.primitive("double"); break;
        case 'Z': type = ClassJc.primitive("boolean"); break;
        case 'C': type = ClassJc.primitive("char"); break;
        default:
          throw new IllegalArgumentException("FieldJc-Exception; unexpected typeChar;" + typeChar);
        }//switch
      }  catch(Exception exc){
        throw new RuntimeException(exc);
      }
      Java4C.FixArraySize atFixArraySize = field.getAnnotation(annotationJava2CFixArraySize);
      if(atFixArraySize !=null){
        //it is designated as a fix-size-array for C-translation.
        //Therefore it es presented as a fix-size-array in Java too.
        //If the real array size doesn't match to the annotation-given size, an exception is thrown.
          int size = atFixArraySize.value();
          
          modifier |= ModifierJc.kStaticArray;
          staticArraySize = new int[ixArray];
          staticArraySize[0] = size;
  
      } else {
        //a non-final array in Java is like an ObjectArray
        modifier |= ModifierJc.kObjectArrayJc;
        //type = ClassJc.fromClass(fieldType);
        //this.cPrimitveType = '\0';
        staticArraySize = null;
      }
    } else {
      //not an array field
      type = ClassJc.fromClass(fieldType);
      if(fieldType.isPrimitive()){
        String name = fieldType.getName();
        if(name.equals("byte")){ this.cPrimitveType = 'B'; }
        else if(name.equals("short")){ this.cPrimitveType = 'S'; }
        else if(name.equals("int")){ this.cPrimitveType = 'I'; }
        else if(name.equals("long")){ this.cPrimitveType = 'L'; }
        else if(name.equals("float")){ this.cPrimitveType = 'F'; }
        else if(name.equals("double")){ this.cPrimitveType = 'D'; }
        else if(name.equals("boolean")){ this.cPrimitveType = 'Z'; }
        else if(name.equals("char")){ this.cPrimitveType = 'C'; }
        else { this.cPrimitveType = '\0';}
      } else {
        this.cPrimitveType = '\0';
      }
      staticArraySize = null;
    }
  }

  
  
  public FieldJc(Class clazz, String name){
    this.type = ClassJc.fromClass(clazz);
    this.field = null;
    this.name = name;
    this.staticArraySize = null;
    this.cPrimitveType = '\0';
    this.annotations = null;
  }
  
  
  public FieldJc(ClassJc clazz, String name){
    this.type = clazz;
    this.field = null;
    this.name = name;
    this.staticArraySize = null;
    this.cPrimitveType = '\0';
    this.annotations = null;
  }
  
  
  
  public String getName(){ return name; }
  
  
  public ClassJc getType(){ return type; }
  
  
  /**Returns the type char. For primitives it is B S I L F D Z C, 
   * for non-primitives it returns 0.
   * @return 0 if non primitive, else typechar.
   */
  public char getTypeChar(){ return cPrimitveType; }
  
  public int getModifiers(){ return modifier; }
  
  
  public int getStaticArraySize(){ return staticArraySize !=null && staticArraySize.length>0 ? staticArraySize[0] : 0; }
  
  
  
  public int getArraylength(MemSegmJc instance) throws IllegalArgumentException, IllegalAccessException
  { /* it is assumed, that the obj matches to the Field ythis, it means, the Field ythis
     is getted from a object of exactly this type or from the obj itself.
     This is tested by original Java - set- or get- operations.
     But it is not tested here to optimize calculation time. If it is a part of a
     Java2C-translation, the test should be done on Java-level, if it is a directly
     programmed in C or C++ part, the user should be programmed carefully, how it is
     ordinary in C or C++.
    */
   int length;
   int bitModifiers;
   int bitContainerType;
   final MemSegmJc adr = new MemSegmJc();  //an Stack instance in C because it is an embedded type.
   bitModifiers = getModifiers();
   bitContainerType = bitModifiers & ModifierJc.m_Containertype;
   //gets the address of the container without index
   //adr = getContainerAddress(instance, null, null); //memory location of the field inside obj
   if(MemSegmJc.segment(instance) ==0){
     adr.set(getContainer(instance)); //reference to the instance; 
   }
   Object obj = adr.obj();
   if(obj != null && MemSegmJc.segment(adr)==0){
     switch(bitContainerType){
     case ModifierJc.kLinkedListJc:
     { length = ((List)obj).size();
     } break; 
     case ModifierJc.kMapJc:
     { length = ((Map)obj).size();;
     } break; 
     case ModifierJc.kUML_LinkedList:
     { length = getArraylengthUML_LinkedList(adr);
     } break; 
     case ModifierJc.kUML_ArrayList:
     { length = getArraylengthUML_ArrayList(adr);
     } break; 
     case ModifierJc.kUML_Map:
     { length = getArraylengthUML_Map(adr);
     } break; 
     case ModifierJc.kObjectArrayJc:
     { Object[] array = (Object[])(adr.obj());  //it is known that it is an ObjectArrayJc
       length = array.length;
     } break; 
     case ModifierJc.kStaticArray:
     { //memory location of the field inside obj
       //MemSegmJc adr = getMemoryAddress_FieldJc(ythis,instance, null, null); 
       length = getStaticArraySize();
       if(  (bitModifiers & ModifierJc.mAddressing) == ModifierJc.kReference) {
         /**Only if it is an reference, check the reference values from back. Show only non-null references. */
         length = 0; //getRealLengthStaticArray_MemAccessJc(adr, length);
       }
     } break;
     default:
     { //assumed, it is a reference:
       //THROW_s0(NoSuchFieldException, "not an array or container field, field=", (int32)(ythis));
       length = 0;  //dereferenced at the given position.
     }
     }//switch
   }
   else
   { length = 0; //because container is not exitsting
   }
   return length;
  }  
  
  
  
  
  /**Returns the real Class and the referenced Object appropriate to the field in the given Object.
   * The field should be a reference field, not a primitive. 
   * It means, this.getType().isPrimitive() should return false.
   * @param instanceM The reference to the Object, where the field is located in. It may be located
   *        in a extern hardware.
   * @param retClazz The really type of retObj, not only the this.getType().
   * @param idx Given index if the field is an array-field.
   * @return The object which is referenced. It may be null contain a null-reference.
   *        The referenced object may be located in a extern hardware, though instanceM is locally.
   *        A special reference is responsible to referencing a external hardware.
   */
  public MemSegmJc getObjAndClass(MemSegmJc instanceM, ClassJc[] retClazz, int ...idx)
  { ClassJc clazz = null;
    Object retObj = null;
    Object currObject = instanceM.obj();
    //Class<?> typeField = field.getType();
    String nameType = type.getName();
    if(nameType.startsWith("["))
    { /**its an Object-Array */
      Object[] objArray = (Object[])instanceM.obj();
      retObj = objArray[idx[0]];     //an indexOutOfBoundsException may be occured.
      clazz = ClassJc.getClass(retObj);
    }
    else if(name.equals("super")){
      retObj = instanceM.obj();  //same instance
      clazz = type;  //the super type.
    }
    else
    {
      int modifier = field.getModifiers();
      field.setAccessible(true);
      try{ 
        retObj = field.get(currObject);
        if(retObj !=null){
          clazz = ClassJc.getClass(retObj);
        } else {
          clazz = null;
        }
      } catch(IllegalAccessException exc){ 
        retObj = null;
        clazz = null;
      }
      //if(Modifier.)
    }
    retClazz[0] = clazz;  
    return new MemSegmJc(retObj, 0);
  }
  
  
  
  
  public byte getByte(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    return field.getByte(obj.obj());
  }
  
  
  public short getShort(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    return field.getShort(obj.obj());
  }
  
  public int setShort(MemSegmJc objM, short valSet, int... ix) throws IllegalArgumentException, IllegalAccessException {
    short value;
    if(staticArraySize !=null && staticArraySize.length >0){
      value = shortContainer.setValue(valSet, objM, ix);
   } else {
      field.setShort(objM.obj(), valSet);
      value = valSet;
    }
    return value;
  }
  
  
  
  
  public int xxxgetInt(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    int value;
    if(staticArraySize !=null && staticArraySize.length >0){
      Object data = field.get(obj.obj());  //the element itself. It is an int[], or float[] or such.
      
      int ix1 = 0;
      for(int ixx=0; ixx < staticArraySize.length; ++ixx){
        if(ix.length >= ixx){ ix1 = ix[ixx]; } else { ix1 = 0; }
        if(ixx < staticArraySize.length -1){
          data = ((Object[])data)[ix1];
        } 
      }
      value = ((int[])data)[ix1];
    } else {
      value = field.getInt(obj.obj());
    }
    return value;
  }
  
  
  
  public int getInt(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    int value;
    if(staticArraySize !=null && staticArraySize.length >0){
      value = intContainer.getValue(obj, ix);
    } else {
      value = field.getInt(obj.obj());
    }
    return value;
  }
  
  
  
  public long getLong(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    long value;
    if(staticArraySize !=null && staticArraySize.length >0){
      value = longContainer.getValue(obj, ix);
    } else {
      value = field.getLong(obj.obj());
    }
    return value;
  }
  
  
  
  public int setInt(MemSegmJc objM, int valSet, int... ix) throws IllegalArgumentException, IllegalAccessException {
    int value;
    if(staticArraySize !=null && staticArraySize.length >0){
      value = intContainer.setValue(valSet, objM, ix);
   } else {
      field.setInt(objM.obj(), valSet);
      value = valSet;
    }
    return value;
  }
  
  
  
  public char getChar(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    char value;
    if(staticArraySize !=null && staticArraySize.length >0){
      value = charContainer.getValue(obj, ix);
    } else {
      value = field.getChar(obj.obj());
    }
    return value;
  }
  
  
  
  public char setChar(MemSegmJc objM, char valSet, int... ix) throws IllegalArgumentException, IllegalAccessException {
    char value;
    if(staticArraySize !=null && staticArraySize.length >0){
      value = charContainer.setValue(valSet, objM, ix);
   } else {
      field.setChar(objM.obj(), valSet);
      value = valSet;
    }
    return value;
  }
  
  
  
  
  /**Helper contains the method to access an int[]-array. */
  private final GetSetContainerElement<Integer, int[]> intContainer = new GetSetContainerElement<Integer, int[]>()
  { @Override Integer getValue(int[] array, int ix)
    { return array[ix];
    }

    @Override void setValue(int[] array, Integer element, int ix)
    { array[ix] = element;
    }

  };
  
  
  
  /**Helper contains the method to access an int[]-array. */
  private final GetSetContainerElement<Long, long[]> longContainer = new GetSetContainerElement<Long, long[]>()
  { @Override Long getValue(long[] array, int ix)
    { return array[ix];
    }

    @Override void setValue(long[] array, Long element, int ix)
    { array[ix] = element;
    }

  };
  
  /**Helper contains the method to access an int[]-array. */
  private final GetSetContainerElement<Short, short[]> shortContainer = new GetSetContainerElement<Short, short[]>()
  {  @Override Short getValue(short[] array, int ix)
    { return array[ix];
    }

    @Override void setValue(short[] array, Short element, int ix)
    { array[ix] = element;
    }

  };
  
  /**Helper contains the method to access an float[]-array. */
  private final GetSetContainerElement<Float, float[]> floatContainer = new GetSetContainerElement<Float, float[]>()
  {  @Override Float getValue(float[] array, int ix)
    { return array[ix];
    }

    @Override void setValue(float[] array, Float element, int ix)
    { array[ix] = element;
    }

  };
  
  /**Helper contains the method to access an float[]-array. */
  private final GetSetContainerElement<Double, double[]> doubleContainer = new GetSetContainerElement<Double, double[]>()
  {  @Override Double getValue(double[] array, int ix)
    { return array[ix];
    }

    @Override void setValue(double[] array, Double element, int ix)
    { array[ix] = element;
    }

  };
  
  /**Helper contains the method to access an int[]-array. */
  private final GetSetContainerElement<Character, char[]> charContainer = new GetSetContainerElement<Character, char[]>(){

    @Override Character getValue(char[] array, int ix)
    { return array[ix];
    }

    @Override void setValue(char[] array, Character element, int ix)
    { array[ix] = element;
    }

  };
  
  /**Helper contains the method to access an int[]-array. */
  private final GetArray<short[]> getShortArray = new GetArray<short[]>();
  
  /**Helper contains the method to access an int[]-array. */
  private final GetArray<byte[]> getByteArray = new GetArray<byte[]>();
  
  /**Helper contains the method to access an int[]-array. */
  private final GetArray<long[]> getLongArray = new GetArray<long[]>();
  
  /**Helper contains the method to access an int[]-array. */
  private final GetArray<float[]> getFloatArray = new GetArray<float[]>();
  
  /**Helper contains the method to access an int[]-array. */
  private final GetArray<double[]> getDoubleArray = new GetArray<double[]>();
  
  /**Helper contains the method to access an int[]-array. */
  private final GetArray<Object[]> getObjectArray = new GetArray<Object[]>();
  
  /**Helper contains the method to access an int[]-array. */
  private final GetArray<String[]> getStringArray = new GetArray<String[]>();
  
  
  
  
  public long getInt64(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    return field.getLong(obj.obj());
  }
  
  
  public float getFloat(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    float value;
    if(staticArraySize !=null && staticArraySize.length >0){
      value = floatContainer.getValue(obj, ix);
    } else if((modifier & ModifierJc.kObjectArrayJc) !=0) {
      Object array = field.get(obj.obj());
      float[] fArray = (float[]) array;
      int ix0 = ix[0];
        
      if(ix0 >=0 && ix0 < fArray.length ){
        value = fArray[ix0];
      } else {
        value = -99999.99999f;
      }
    } else {
      value = field.getFloat(obj.obj());
    }
    return value;
  }
  
  public float setFloat(MemSegmJc objM, float valSet, int... ix) throws IllegalArgumentException, IllegalAccessException 
  {
    float value;
    if(staticArraySize !=null && staticArraySize.length >0){
      value = floatContainer.setValue(valSet, objM, ix);
    } else if((modifier & ModifierJc.kObjectArrayJc) !=0) {
      Object array = field.get(objM.obj());
      float[] fArray = (float[]) array;
      int ix0 = ix[0];
        
      if(ix0 >=0 && ix0 < fArray.length ){
        value = fArray[ix0] = valSet;
      } else {
        value = -99999.99999f;
      }
    } else {
      field.setFloat(objM.obj(), valSet);
      value = valSet;
    }
    return value;
  }
  
  
  

  public double getDouble(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    return field.getDouble(obj.obj());
  }
  

  public double setDouble(MemSegmJc objM, double valSet, int... ix) throws IllegalArgumentException, IllegalAccessException 
  {
    double value;
    if(staticArraySize !=null && staticArraySize.length >0){
      value = doubleContainer.setValue(valSet, objM, ix);
    } else {
      field.setDouble(objM.obj(), valSet);
      value = valSet;
    }
    return value;
  }
  
  
  
  
  public boolean getBoolean(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    return field.getBoolean(obj.obj());
  }
 
  /**Gets the value from a bitField. A bitField is deployed in C/C++ only. In Java it may be possible
   * to get a value from a bitField from a extern C-programmed module (TODO). 
   * @param obj
   * @param ix
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public short getBitfield(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    return 0;
  }
 
  /**Gets the value from a bitField. A bitField is deployed in C/C++ only. In Java it may be possible
   * to get a value from a bitField from a extern C-programmed module (TODO). 
   * @param obj
   * @param ix
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public short setBitfield(MemSegmJc obj, short value, int... ix) throws IllegalArgumentException, IllegalAccessException {
    return 0;
  }
 
  /**Gets a String representation of the content of this FieldJc in the given instance.
   * <ul>
   * <li>If the field is a Object-compatible type (a reference) and the reference is not null, then the Object.toString()-method
   * is invoked with this reference. 
   * <li>If the reference is null, than "null" is returned.
   * <li>If the field is a primitive type, the String representation of its content is returned.
   * <li>For C-usage: If the field references a non-Object-instance, "@addr" is returned, 
   *   where addr is the memory address of the referenced instance (the reference value). 
   * </ul>
   * @param instance The instance where this FieldJc is member of.
   * @param ix 0 or more indices if the Field is an array or container.
   * @return A String which represents the content of the field.
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public String getString(MemSegmJc instance, int... ix) throws IllegalArgumentException, IllegalAccessException {
    Object obj1 = (this.field == null) ? instance.obj() : field.get(instance.obj());  //regard super field.
    final String sValue;
    if(obj1 == null){ sValue = "null"; }
    else { sValue = obj1.toString(); }
    return sValue;
  }
  
  
  public MemSegmJc get(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    Object refObj = field.get(obj.obj());
    MemSegmJc retMem = new MemSegmJc(refObj, 0);
    return retMem;
  }
  
  
  public int getMemoryIdent(MemSegmJc obj, int... ix)
  {
    return obj.obj().hashCode();
    
  }
  
  
  public MemSegmJc getContainer(MemSegmJc obj, int... ix) throws IllegalArgumentException, IllegalAccessException {
    Object refObj = field.get(obj.obj());
    MemSegmJc retMem = new MemSegmJc(refObj, 0);
    return retMem;
  }
  
  
  
  /**Sets the integer adequate Field.setInt(obj, value), but with one or more indices.
   * @param data
   * @param val
   * @param ix
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   */
  /*
  public int xxxsetInt(MemSegmJc objM, int val, int ... ix) throws IllegalArgumentException, IllegalAccessException
  { Object obj = objM.obj();
    String sTypename = type.getName();
    int ixArray = 0;
    if(sTypename.charAt(0)!='['){
      //trivial, not an array, simple field.
      field.setInt(obj, val);   //use the setInt()-method. It does all, inclusive test and exception.
    } else {
      //it is an array type. Access to the correct element!
      Object data = field.get(obj);  //the element itself. It is an int[], or float[] or such.
      //if(sTypename.equals("[I")){ ((int[])data)[ix[0]] = val; }
      while(sTypename.charAt(++ixArray) == '['){
        //if it is a deeper array, get the element.
        data = ((Object[])data)[ix[ixArray-1]];
      } //count depths of array.
      //now data is a simple int[] or float[] or such, but not a deeper array.
      char typechar = sTypename.charAt(ixArray);  //int, etc.
      switch(typechar){
      case 'I': ((int[])data)[ix[ixArray-1]] = val; break;  //simple access.
      case 'J': ((int[])data)[ix[ixArray-1]] = val; break;  //simple access.
      default: throw new IllegalArgumentException("Field is not of int-type.");
      }
    } 
    return val;
  }
  */

  /**Sets the integer adequate Field.setInt(obj, value), but with one or more indices.
   * @param data
   * @param val
   * @param ix
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   */
  public boolean setBoolean(MemSegmJc objM, boolean val, int ... ix) throws IllegalArgumentException, IllegalAccessException
  { Object obj = objM.obj();
    String sTypename = type.getName();
    int ixArray = 0;
    if(sTypename.charAt(0)!='['){
      //trivial, not an array, simple field.
      field.setBoolean(obj, val);   //use the setInt()-method. It does all, inclusive test and exception.
    } else {
      //it is an array type. Access to the correct element!
      Object data = field.get(obj);  //the element itself. It is an int[], or float[] or such.
      //if(sTypename.equals("[I")){ ((int[])data)[ix[0]] = val; }
      while(sTypename.charAt(++ixArray) == '['){
        //if it is a deeper array, get the element.
        data = ((Object[])data)[ix[ixArray-1]];
      } //count depths of array.
      //now data is a simple int[] or float[] or such, but not a deeper array.
      char typechar = sTypename.charAt(ixArray);  //int, etc.
      switch(typechar){
      case 'Z': ((boolean[])data)[ix[ixArray-1]] = val; break;  //simple access.
      default: throw new IllegalArgumentException("Array-Element is not of boolean-type.");
      }
    } 
    return val;
  }
  

  int getArraylengthUML_LinkedList(MemSegmJc instance){ return 0; }
  
  int getArraylengthUML_ArrayList(MemSegmJc instance){ return 0; }
  
  int getArraylengthUML_Map(MemSegmJc instance){ return 0; }
  
  
  //public Class<?> getType(){ return field.getClass(); }
}
