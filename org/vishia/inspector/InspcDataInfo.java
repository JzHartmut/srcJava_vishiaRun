package org.vishia.inspector;

import java.lang.reflect.Field;

import org.vishia.bridgeC.MemSegmJc;
import org.vishia.reflect.FieldJc;

/**This class contains the description to access one variable in memory
 * to get a value not by path but by a ident. The structure stores the type, memory address
 * and some access info. In Java the memory address is the Object reference where the Field is located.
 * In C it is adapted in the same kind to support Java2C-translation.
 * @author Hartmut Schorrig
 *
 */
public class InspcDataInfo
{

  /**Version, history and license
   * <ul>
   * <li>2012-04-07 Hartmut created, converted from the C implementation.
   * <li>2006-00-00 Hartmut created for C/C++
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

  
  /** Timeout of using.*/
  //int timeout_millisec;   
    
  /**timestamp where it was used lastly. 
   * The timestamp may be any unit of a target system. It is wrapping around. Use the difference
   * between currentTime - lastUsed to test which element is the oldest one.
   * If this element is ==0 then it is unused. don't set =0.
   * A millisecond wraps in about 3 weeks. 
   */
  int lastUsed;
  
  MemSegmJc addr;
  
  /**Address and Segment of the value. */
  FieldJc addrValue;

  /**Nr of bytes to read and transfer. */
  byte sizeofValue;

  byte dummy;
  
  /**The type of the value, to send in telegram, see kScalarTypes_DataExchangeCmd_OBM. 
   * If 0, than the entry is free.
   */
  byte typeValue;
  
  /**The kind of order:
   * 'm': build min, max and mid
   * 'r': Record
   * 
   * 
   */ 
  byte kindofOrder;

  /**If it is a recording order, size of the buffer. */
  short lengthData;  
  
  /**The indendificator should be sent from request to safety the correctness of request. */
  short check;  
  //OS_ValuePtr theObject;
      
  
}
