package org.vishia.reflect;

/**This class stores a log of accesses to an external hardware.
 * It is implemented in C-language in its own way. This definition may be only a placeholder
 * for the Java2C-translated parts, or it can be used in the organization of remote-hardware-access
 * programmed in Java. 
 * @author Hartmut Schorrig
 *
 */
final public class MemAccessArrayDebugJc
{
  int ix; 
  
  MemAccessDebugJc[] item = new MemAccessDebugJc[20];
	
  
  static MemAccessArrayDebugJc singleton = new MemAccessArrayDebugJc();
  
  /**Returns the instance if there is only one istance. Note: Implemented in C-language.
   * @return null if it isn't a singleton.
   */
  public final static MemAccessArrayDebugJc getSingleton(){ return singleton; }
  
	/**Contains one item to log the access to a remote hardware.
	 */
	public static final class MemAccessDebugJc
	{
	  /**The cmd to the external CPU.*/ 
	  int cmd;

	  /**The value for the address to the external CPU, casted to int32.*/ 
	  int address;

	  /**The value for the input to the external CPU.*/ 
	  int input;

	  /**The value which is returned from external CPU.*/
	  int output;

	
	}
}
