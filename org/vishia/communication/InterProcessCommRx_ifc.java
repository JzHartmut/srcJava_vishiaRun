package org.vishia.communication;

import org.vishia.util.Java4C;

public interface InterProcessCommRx_ifc
{

  void execRxData(@Java4C.PtrVal byte[] buffer, int nrofBytesReceived);
  
}
