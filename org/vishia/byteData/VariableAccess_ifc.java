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

/**This interface describes the access to any form of variable.
 * A variable may be a field gotten with reflection, see implementation {@link org.vishia.reflect.FieldVariableAccess}.
 * A variable may be a entity in a byte-datagram, see {@link org.vishia.byteData.ByteDataSymbolicAccess.Variable}.
 * The quality of a variable and the kind of access is described in the implementation.
 * The user can access to a substantial variable with this interface.
 * <br><br>
 * It is possible to have one list or index with variables which are found in several data storage types,
 * for example in Java-data using {@link org.vishia.reflect.FieldVariableAccess} or in data from files.
 * use Container <code>List<VariableAccess_ifc></code> or <code>Map<String,VariableAccess_ifc></code> 
 * 
 * @author Hartmut Schorrig
 *
 */
public interface VariableAccess_ifc
{

	/**Gets a integer-type value from this variable. The variable contains the information, 
	 * whether it is long, short etc. If the variable contains a long value greater as the integer range,
	 * an IllegalArgumentException may be thrown or not, it depends on the implementation.
	 * @param ixArray unused if it isn't an indexed variable.
	 * @return the value.
	 */
	int getInt(int ...ixArray);
	
	/**Sets the value into the variable. If the variable is of byte or short type and the value is not able
	 * to present, an IllegalArgumentException may be thrown or not, it depends on the implementation.
	 * @param value The value given as int.
	 * @param ixArray unused if it isn't an indexed variable.
	 * @return The value really set (maybe more imprecise).
	 */
	int setInt(int value, int ...ixArray);
	
	/**Gets the value from this variable. If the variable is in another format than float, 
	 * a conversion to be will be done.
	 * @param ixArray unused if it isn't an indexed variable.
	 * @return the value.
	 */
	float getFloat(int ...ixArray);
	
	/**Sets the value from this variable. 
	 * @param ixArray unused if it isn't an indexed variable.
	 * @return the value.
	 */
	float setFloat(float value, int ...ixArray);
	
	
	/**Gets the value from this variable. If the variable is in another format than double, 
	 * a conversion to be will be done.
	 * @param ixArray unused if it isn't an indexed variable.
	 * @return the value.
	 */
	double getDouble(int ...ixArray);
	
	/**Sets the value from this variable. If the variable is from float type, and the range (exponent)
	 * is able to present in float, the value will be stored in float with truncation of digits.
	 * @param ixArray unused if it isn't an indexed variable.
	 * @return the value.
	 */
	double setDouble(double value, int ...ixArray);
	
	
	/**Gets the value from this variable. If the variable is numerical, it is converted to a proper representation.
	 * @param ixArray unused if it isn't an indexed variable.
	 * @return the value.
	 */
	String getString(int ixArray);
	
	/**Sets the value into the variable
	 * @param value The value given as String.
	 * @param ixArray unused if it isn't an indexed variable.
	 * @return The value really set (maybe shortened).
	 */
	String setString(String value, int ixArray);
	
	
	
}
