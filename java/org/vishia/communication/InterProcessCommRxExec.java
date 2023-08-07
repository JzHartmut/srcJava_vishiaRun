package org.vishia.communication;

import org.vishia.util.Java4C;

/**This is a sample class for implementing the {@link InterProcessCommRx_ifc} especially for usage in C.
 * It is translated Java2C.
 * 
 * @author Hartmut Schorrig
 *
 */
public class InterProcessCommRxExec extends InterProcessCommRx_ifc
{

  @Override public void execRxData(@Java4C.PtrVal byte[] buffer, int nrofBytesReceived, Address_InterProcessComm sender)
  {
    System.out.println("Hello world - implementing of InterProcessCommRx_ifc");
    
  }
  
}
