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
package org.vishia.communication;

/**This class is used as interface and as singleton instance access for factory of any InterProcessComm.
 * @author Hartmut Schorrig
 *
 */
public interface InterProcessCommFactory
{

  
  
  
  public Address_InterProcessComm createAddress(String protocolAndOwnAddr, int nPort);
  //{ return singleton.createAddress(protocolAndOwnAddr, nPort);
  //}
  
  public Address_InterProcessComm createAddress(String protocolAndOwnAddr);
  //{ return singleton.createAddress(protocolAndOwnAddr, -1);
  //}
  
	/**Creates a InterProcessComm from a parameter String. The type depends on this String.
   * For example:
   * <ul>
   * <li>"UDP:192.16.35.3:1234" for UDP via socket. Don't write spaces, set the port after ':'
   * </ul> 
   * @param protocolAndOwnAddr A string which determines the kind of communication and the own address (slot).
   *                           It depends on the underlying system which kind of communication are supported
   * @return null or an instance maybe with opened communication.
   */
  public InterProcessComm create(String protocolAndOwnAddr, int nPort);
  //{ return singleton.create(protocolAndOwnAddr, nPort);
  //}
  
  public InterProcessComm create(String protocolAndOwnAddr);
  //{ return singleton.create(protocolAndOwnAddr, -1);
  //}

  InterProcessComm create(Address_InterProcessComm addr);
  
  
}
