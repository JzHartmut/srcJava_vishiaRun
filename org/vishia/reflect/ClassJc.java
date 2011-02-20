/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.reflect;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.bridgeC.MemSegmJc;

public final class ClassJc
{
	
	/**Index of all known reflection-classes valid for this application. If any reflection class
	 * is gotten, it is known here for all following accesses. Especially the instances for the fields
	 * of the class {@link #indexNameFields} shouldn't be created newly if the instance of ClassJc is stored. 
	 */
	private final static Map<String, ClassJc> allClasses = new TreeMap<String, ClassJc>(); 
	
  public static final int REFLECTION_void  =              0x01; 
  public static final int REFLECTION_int64  =             0x02; 
  public static final int REFLECTION_uint64 =             0x03;
  public static final int REFLECTION_int32  =             0x04;
  public static final int REFLECTION_uint32 =             0x05;
  public static final int REFLECTION_int16  =             0x06;
  public static final int REFLECTION_uint16 =             0x07;
  public static final int REFLECTION_int8   =             0x08;
  public static final int REFLECTION_uint8  =             0x09;
  public static final int REFLECTION_int    =             0x0a;
  public static final int REFLECTION_uint   =             0x0b;
  public static final int REFLECTION_float  =             0x0c;
  public static final int REFLECTION_double =             0x0d;
  public static final int REFLECTION_char8 =              0x0e;
  public static final int REFLECTION_char16 =             0x0f;
  public static final int REFLECTION_String =             0x10;
  public static final int REFLECTION_boolean=             0x16;
  public static final int REFLECTION_bitfield =           0x17;

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
      allFields = new FieldJc[fields.length];
      int ix=-1;
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
  
  
  public ClassJc getEnclosingClass(){
  	return null; //TODO
  }
  
}
