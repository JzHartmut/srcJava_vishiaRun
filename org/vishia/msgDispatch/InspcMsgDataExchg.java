package org.vishia.msgDispatch;

import org.vishia.byteData.ByteDataAccess;


/**This class describes the data structure for messages in the Inspector datagram
 * @author Hartmut Schorrig
 *
 */
public class InspcMsgDataExchg extends ByteDataAccess{

  private static final int kLength = 0;  //2
  private static final int kSequ = 1;   //8
  private static final int kModeTypes = 2; //4
  private static final int kIdent = 4;  //2
  private static final int kTime = 8;
  private static final int kValues =16;      //12

  /**Returns 2 Bytes for any value from bit 1,0 to bit 15,14.
   * Bit coding:
   * <ul>
   * <li>00 Integer value 4 Bytes
   * <li>01 Short Text, 8 Byte
   * <li>10 Double value, 8 Byte
   * <li>11 Float value, 4 Bytes
   * </ul>
   * For example, 0x00ff means, there are 4 float values. 0x0005 means, there are 16 Bytes text (2 words a 8 chars). 
   */
  public final int getModeTypes(){ return getInt16(kModeTypes); }

  
  
  @Override
  protected void specifyEmptyDefaultData() {
    setInt8(kLength, kValues);
    
  }

  @Override
  protected int specifyLengthElement() throws IllegalArgumentException {
    // TODO Auto-generated method stub
    return getInt8(kLength);
  }

  @Override
  public int specifyLengthElementHead() {
    // TODO Auto-generated method stub
    return kValues;
  }

}
