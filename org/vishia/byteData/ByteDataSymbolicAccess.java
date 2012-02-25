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
package org.vishia.byteData;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.bridgeC.IllegalArgumentExceptionJc;
import org.vishia.byteData.RawDataAccess;
import org.vishia.mainCmd.Report;
import org.vishia.zbnf.ZbnfJavaOutput;


public class ByteDataSymbolicAccess {

	/**Version, able to read as hex yyyymmdd.
	 * Changes:
	 * <ul>
	 * <li>2010-12-03 Hartmut fnChg: The syntax is defined internally, see {@link #syntaxSymbolicDescrFile}.
	 * </ul>
	 * <ul>
	 * <li>new: new functionality, downward compatibility.
	 * <li>fnChg: Change of functionality, no changing of formal syntax, it may be influencing the functions of user,
	 *            but mostly in a positive kind. 
	 * <li>chg: Change of functionality, it should be checked syntactically, re-compilation necessary.
	 * <li>adap: No changing of own functionality, but adapted to a used changed module.
	 * <li>corr: correction of a bug, it should be a good thing.
	 * <li>bug123: correction of a tracked bug.
	 * <li>nice: Only a nice correction, without changing of functionality, without changing of syntax.
	 * <li>descr: Change of description of elements.
	 * </ul> 
	 */
	public final static int versionStamp = 0x20101203;
	
	/**An instance is created and filled from ZBNF-parser using reflection.
	 */
	public class Variable implements VariableAccess_ifc
	{
		public final ByteDataSymbolicAccess bytes;
		
		/**The data path. */
		public String name;
		
		/**A simple read-able identifier */
		public String nameShow;
		
		private char typeChar;
		
		public char getTypeChar(){ return typeChar; }
		
		public void set_typeChar(String src){ typeChar = src.charAt(0); }
		
		public int bytePos;
		
		public int bitMask;
		
		public int XXXnrofBytes;
		
		public int nrofArrayElements;

		public Variable(ByteDataSymbolicAccess bytes)
		{
			super();
			this.bytes = bytes;
		}

		@Override public int getInt(int ...ixArray)
		{ int value = 0;
			int nrofBytes = getNrofBytes();
			int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes;  //sizeof(int) is 4.
			if(bytePos > data.length-nrofBytes) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes);
			switch(this.typeChar){
			case 'D': value = (int)dataAccess.getDoubleVal(bytePos); break;
			case 'F': value = (int)dataAccess.getFloatVal(bytePos); break;
			case 'I': case 'J': case 'S': case 'B': value = dataAccess.getIntVal(bytePos, -nrofBytes); break;
			case 'Z': value = (dataAccess.getIntVal(bytePos, 1) & this.bitMask) == 0 ? 0: 1; break;
			default: new IllegalArgumentExceptionJc("fault type, expected: int, found: ", typeChar);
			}//switch
			return value;
		}
		
		
		@Override public int setInt(int value, int ...ixArray)
		{ int nrofBytes = getNrofBytes();
			int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes;  //sizeof(int) is 4.
			if(bytePos > data.length-nrofBytes) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes);
			switch(typeChar){
			case 'D': dataAccess.setDoubleVal(bytePos, value); break;
			case 'F': dataAccess.setFloatVal(bytePos, value); break;
			case 'I': case 'J': case 'S': case 'B': dataAccess.setIntVal(bytePos, nrofBytes, value); break;
			case 'Z': {
				byte byteVal = (byte)dataAccess.getIntVal(bytePos, 1);
				if(value == 0){ byteVal &= ~bitMask; }
				else { byteVal |= bitMask; }  //TODO use ixArray
				dataAccess.setIntVal(bytePos, 1, byteVal);  //rewrite
			}break;
			//TODO case 'Z': (dataAccess.setIntVal(bytePos, 1) & variable.bitMask) == 0 ? 0: 1; break;
			default: new IllegalArgumentException("fault type, expected: float, found: " + typeChar);
			}//switch
			return value;
		}

		@Override
		public String getString(int ixArray)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String setString(String value, int ixArray)
		{
			// TODO Auto-generated method stub
			return null;
		}

		//@Override
		public float getFloat(int... ixArray)
		{ float value = 0.0f;
			int nrofBytes = getNrofBytes();
			int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes;  //sizeof(int) is 4.
			if(bytePos > data.length-nrofBytes) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes);
			switch(typeChar){
			case 'D': value = (float)dataAccess.getDoubleVal(bytePos); break;
			case 'F': value = dataAccess.getFloatVal(bytePos); break;
			case 'I': case 'J': case 'S': case 'B': value = dataAccess.getIntVal(bytePos, -nrofBytes); break;
			default: new IllegalArgumentException("fault type, expected: float, found: " + typeChar);
			}//switch
			return value;
		}

		//@Override 
		public float setFloat(float value, int ...ixArray)
		{ int nrofBytes = getNrofBytes();
			int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes;  //sizeof(int) is 4.
			if(bytePos > data.length-nrofBytes) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes);
			switch(typeChar){
			case 'D': dataAccess.setDoubleVal(bytePos, value); break;
			case 'F': dataAccess.setFloatVal(bytePos, value); break;
			case 'I': case 'J': case 'S': case 'B': 
				//TODO check whether the value matches
				dataAccess.setIntVal(bytePos, nrofBytes, (int)value); break;
			//TODO case 'Z': (dataAccess.setIntVal(bytePos, 1) & variable.bitMask) == 0 ? 0: 1; break;
			default: new IllegalArgumentException("fault type, expected: float, found: " + typeChar);
			}//switch
			return value;
		}

		@Override
		public double getDouble(int... ixArray)
		{ double value = 0.0;
			int nrofBytes = getNrofBytes();
			int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes;  //sizeof(int) is 4.
			if(bytePos > data.length-nrofBytes) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes);
			switch(typeChar){
			case 'D': value = dataAccess.getDoubleVal(bytePos); break;
			case 'F': value = dataAccess.getFloatVal(bytePos); break;
			case 'I': case 'J': case 'S': case 'B': value = dataAccess.getIntVal(bytePos, -nrofBytes); break;
			default: new IllegalArgumentException("fault type, expected: float, found: " + typeChar);
			}//switch
			return value;
		}

		@Override
		public double setDouble(double value, int... ixArray)
		{
			// TODO Auto-generated method stub
			return value;
		}
		
		private int getNrofBytes()
		{ int nrofBytes = 0;  //for unexpected types: nrofBytes may be unused.
			switch(typeChar){
			case 'D': nrofBytes = 8; break;
			case 'F': nrofBytes = 4; break;
			case 'J': nrofBytes = 8; break;
			case 'I': nrofBytes = 4; break;
			case 'S': nrofBytes = 2; break;
			case 'B': nrofBytes = 1; break;
			}
			return nrofBytes;
			
		}

		
		
	}
	
	public class ZbnfResult
	{ private List<Variable> variable1 = new LinkedList<Variable>();
	  //public Variable new_variable(){ return new Variable(); }

		/**New instance for ZBNF-component: <variable> */
		public Variable new_variable(){
	  	return new Variable(ByteDataSymbolicAccess.this);
	  }
	
		/**Set from ZBNF-component: <variable> */
		public void add_variable(Variable item){
	  	variable1.add(item);
	  }
		
	
	}

	
	private final Report log;
	
	private final Map<String, ByteDataSymbolicAccess.Variable> indexVariable = new TreeMap<String, ByteDataSymbolicAccess.Variable>();
	
	private byte[] data;
	
	int ixStartData;
	
	int nrofData;
	
	/**This syntax describes the oam-variable-config-file.
	 * For example it is <pre>
	 * binMirror/loadLock: Z @0.0x2;  //a boolean value named "binMirror/loadLock" in byte 0 masked with 0x2
   * param/capbModulId.capId: B @734 +1 *96;  //an array of bytes started from address 734, 96 * 1 byte
	 * </pre> 
   */
	private static final String syntaxSymbolicDescrFile =
		"OamConfig::= ==[OamVariables|AllVariableFromDB]== { <variable> ; } \\e. "
	+ "variable::= <$/\\.?name> : <!.?typeChar> @<#?bytePos>[\\.0x<#x?bitMask>] [ + <#?nrofBytes> [ * <#?nrofArrayElements>]]."
	;
	
	private RawDataAccess dataAccess = new RawDataAccess();
	
	public ByteDataSymbolicAccess(Report log)
	{ this.log = log;
	}
	
	public int readVariableCfg(String sFileCfg)
	{ 
		ZbnfJavaOutput parser = new ZbnfJavaOutput(log);
		ZbnfResult rootParseResult = new ZbnfResult();
		File fileConfig = new File(sFileCfg);
		//File fileSyntax = new File("exe/oamVar.zbnf");
		String sError = parser.parseFileAndFillJavaObject(rootParseResult.getClass(), rootParseResult, fileConfig, syntaxSymbolicDescrFile);
	  if(sError != null){
	  	log.writeError(sError);
	  } else {
	  	//success parsing
	  	for(Variable item: rootParseResult.variable1){
	  		indexVariable.put(item.name, item);
	  	}
		}
		return indexVariable.size();
	}
	
	public void assignData(byte[] data)
	{
		assignData(data, data.length, 0);
	}
	
	public void assignData(byte[] data, int length, int from)
  { this.data = data;
    this.ixStartData = from;
    this.nrofData = length;
    assert( (from + length) <= data.length);
    try{	dataAccess.assignData(data, length, from);
		} catch (IllegalArgumentException exc) { }
		dataAccess.setBigEndian(true);
  }
	
  
  public int lengthData(){ return data ==null ? 0 : data.length; }
  
  /**Searches a variable by name and returns it.
   * A variable is a description of the byte position. length, type in a byte[]-Array.
   * @param name The name 
   * @return null if not found.
   */
  public Variable getVariable(String name)
  { return indexVariable.get(name);
  }
	
	public double getDouble(String name)
	{ Variable variable = indexVariable.get(name);
	  if(variable == null) throw new IllegalArgumentException("not found:" + name);
	  return getDouble(variable, -1);
  }
	
	public double getDouble(Variable variable, int ixArray){ return variable.getDouble(ixArray); }
	
	
	/**Get a float value from byte-area.
	 * @param name The name of the registered variable, maybe with "[ix]" where ix is a number.
	 * @return The value
	 */
	public float getFloat(String name)
	{ final int[] ixArrayA = new int[1];
	  final String sPathVariable = ByteDataSymbolicAccess.separateIndex(name, ixArrayA);
	  Variable variable = indexVariable.get(sPathVariable);
	  if(variable == null) return 9.999999F; //throw new IllegalArgumentException("not found:" + name);
	  return variable.getFloat(ixArrayA);
  }
	
	
	public float getFloat(Variable variable){ return getFloat(variable, -1); }
	
	
	public float getFloat(Variable variable, int ixArray){ return variable.getFloat(ixArray); }
	
	
	
	
	public int getInt(String name)
	{ Variable variable = indexVariable.get(name);
	  if(variable == null) throw new IllegalArgumentException("not found:" + name);
	  return getInt(variable, -1);
  }
	
	public int getInt(Variable variable, int ixArray){ return variable.getInt(ixArray); }
	
	
	public boolean getBool(String name)
	{ Variable variable = indexVariable.get(name);
	  if(variable == null) throw new IllegalArgumentException("not found:" + name);
	  return getBool(variable, -1);
  }
	
	public boolean getBool(Variable variable, int ixArray)
	{ boolean value = false;
		int bytePos = ixArray < 0 ? variable.bytePos : variable.bytePos + (ixArray >>3);  //sizeof(bool) is 1/8.
		if(bytePos > data.length-4) throw new IllegalArgumentException("file to short: " + data.length + ", necessary: " + bytePos);
		switch(variable.typeChar){
		case 'I': value = 0 != dataAccess.getIntVal(bytePos, 4); break;
		case 'J': value = 0 != dataAccess.getIntVal(bytePos, 8); break;
		case 'S': value = 0 != dataAccess.getIntVal(bytePos, 2); break;
		case 'B': value = 0 != dataAccess.getIntVal(bytePos, 1); break;
		case 'Z': {
			byte byteVal = (byte)dataAccess.getIntVal(bytePos, 1);
			value = (byteVal & variable.bitMask) !=0;  //TODO use ixArray
		}break;
		default: new IllegalArgumentException("fault type, expected: float, found: " + variable.typeChar);
		}//switch
		return value;
	}
	

	
	
	public void setFloat(String name, float value)
	{ Variable variable = indexVariable.get(name);
	  if(variable == null) throw new IllegalArgumentException("not found:" + name);
	  variable.setFloat(value, -1);
  }
	
	public void setFloat(Variable variable, int ixArray, float value){ variable.setFloat(value, ixArray); }
	
	
	public void setInt(String name, int value)
	{ Variable variable = indexVariable.get(name);
	  if(variable == null) throw new IllegalArgumentException("not found:" + name);
	  setInt(variable, -1, value);
  }
	
	public void setInt(Variable variable, int ixArray, int value){ variable.setInt(value, ixArray); }
	
	
	/**Separates an string-given index from a path.
	 * The index may contain in the sPathValue at end in form "[index]",
	 * where index is numerical, maybe hexa starting with "0x"
	 * @param sPathValue The path
	 * @param ix An array with the necessary number of  element to store the index. It may be null
	 *           or shorter then the number of indices. Then the indeces are not stored.
	 * @return The sPathValue without the index.
	 */
	public static String separateIndex(String sPathValue, int[] ix)
	{
		int posArray = sPathValue.indexOf('[');
		final String sPathVariable = posArray <0 ? sPathValue : sPathValue.substring(0, posArray);
		final int ixArray;
		if(posArray >=0){
			int posArrayEnd = sPathValue.indexOf(']', posArray+1); 
			String sIxArray = sPathValue.substring(posArray+1, posArrayEnd); 
			
			//TODO detect 0x, copy from ...
  		ixArray = Integer.parseInt(sIxArray);
		} else {
			ixArray = -1; //no array access.
		}
    if(ix != null){
    	ix[0] = ixArray;
    }
		return sPathVariable;
	}

	
	
}
