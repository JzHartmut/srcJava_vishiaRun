package org.vishia.msgDispatch;

import java.io.File;

import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.InterProcessComm;
import org.vishia.util.FileSystem;



/**This class receives messages via a simple file transfer.
 * The file contains some message items in binary format. A file with changed timestamp
 * is recognized as a new message packet. The source should open, write and close the files with messages
 * in an lower time cycle than this class reads the file.
 * <br>
 * It is able to use for an connection which can transfer files but not free ethernet telegrams.
 * The class implements the InterProcessComm interface because it is used as any form of InterProcessComm
 * independent of its implementation via file transfer.
 * 
 * @author Hartmut Schorrig
 *
 */
public class MsgRecvCommFile implements InterProcessComm
{

  /**The only one file which is checked. */
  private final File fileMsgBin;
  
  long lastTimestamp = 0;
  
  
  
  public MsgRecvCommFile(String file){
    fileMsgBin = new File(file);
  }
  
  
  @Override
  public int abortReceive() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int capacityToSendWithoutBlocking(int nrofBytesToSend) {
    throw new RuntimeException("unexpected call, not supported here.");
  }

  @Override
  public int checkConnection() {
    return fileMsgBin.exists()? 0 : -1;
  }

  @Override
  public int close() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int dataAvailable() {
    if(fileMsgBin.exists() && fileMsgBin.lastModified() != lastTimestamp){
      return (int)fileMsgBin.length();
    } else {
      return 0;
    }
  }

  @Override
  public boolean equals(Address_InterProcessComm address1p,
      Address_InterProcessComm address2p) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public int flush() {
    return 0;
  }

  @Override
  public void freeData(byte[] data) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Address_InterProcessComm getOwnAddress() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public byte[] getSendBuffer(int len) {
    throw new RuntimeException("unexpected call, not supported here.");
  }

  @Override public int open(Address_InterProcessComm ownAddress, boolean shouldBlock) {
    if(shouldBlock){
      return -1; //not supported.
    } else {
      return 0;
    }
  }

  @Override
  public byte[] receive(int[] result, Address_InterProcessComm sender) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public byte[] receiveData(int[] nrofBytes, byte[] buffer,  Address_InterProcessComm sender) 
  { 
    long fileTime = fileMsgBin.lastModified();
    if(fileTime != lastTimestamp){
      lastTimestamp = fileTime;
      if(buffer == null){
        int lengthFile = (int)fileMsgBin.length();
        buffer = new byte[lengthFile];
      }
      if((nrofBytes[0] = FileSystem.readBinFile(fileMsgBin, buffer)) <0){
        //error
        nrofBytes[0] = -2;
      }
    } else {
      nrofBytes[0] = 0;
      buffer = null;  //return null;
    }
    return buffer;
  }

  @Override
  public int send(byte[] data, int nBytes, Address_InterProcessComm addressee) {
    throw new RuntimeException("unexpected call, not supported here.");
  }

  @Override
  public String translateErrorMsg(int nError) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override public Address_InterProcessComm createAddress()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override public Address_InterProcessComm createAddress(int p1, int p2)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override public Address_InterProcessComm createAddress(String p1, int p2)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override public Address_InterProcessComm createAddress(String address)
  {
    // TODO Auto-generated method stub
    return null;
  }

}
