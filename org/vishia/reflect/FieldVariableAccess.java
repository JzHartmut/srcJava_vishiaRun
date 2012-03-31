package org.vishia.reflect;


import java.lang.reflect.Field;

import org.vishia.byteData.VariableAccess_ifc;

/**This class supports the access to a Java variable with the interface {@link VariableAccess_ifc}.
 * It is proper for the GUI-access.
 * @author Hartmut Schorrig
 *
 */
public class FieldVariableAccess implements VariableAccess_ifc
{

	/**The field in the given instance. */
	final java.lang.reflect.Field theField;
	
	/**The instance where the field is member of. */
	final Object instance;
	
	
	public FieldVariableAccess(Object instance, Field theField)
	{ theField.setAccessible(true);
		this.theField = theField;
		this.instance = instance;
	}

	@Override	public int getInt(int ...ixArray)
	{ int value ; 
		try{ value = theField.getInt(instance);}
		catch(Exception exc){ throw new IllegalArgumentException(exc); }
		return value;
	}

	public long getLong(int ...ixArray)
	{ long value ; 
		try{ value = theField.getLong(instance);}
		catch(Exception exc){ throw new IllegalArgumentException(exc); }
		return value;
	}

	@Override	public float getFloat(int ...ixArray)
	{ float value ; 
		try{ value = theField.getFloat(instance);}
		catch(Exception exc){ throw new IllegalArgumentException(exc); }
		return value;
	}

	@Override
	public float setFloat(float value, int... ixArray)
	{
		// TODO Auto-generated method stub
		return value;
	}
	
	@Override	public double getDouble(int ...ixArray)
	{ double value ; 
		try{ value = theField.getDouble(instance);}
		catch(Exception exc){ throw new IllegalArgumentException(exc); }
		return value;
	}

	@Override
	public double setDouble(double value, int... ixArray)
	{
		// TODO Auto-generated method stub
		return value;
	}
	
	@Override	public String getString(int ...ixArray)
	{ String sValue ;
	  Class<?> type = theField.getType();
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
    Class<?> type = theField.getType();
    String sType = type.getName();
    if(type.isPrimitive()){
      return sType.charAt(0);
    } else {
      throw new IllegalArgumentException("does war nich vereinbart, mach'n mer nich.");
      //return '.'; //TODO
    }
  }
  
  @Override public int getDimension(int dimension){
    return 0; //TODO
  }
  

  


}
