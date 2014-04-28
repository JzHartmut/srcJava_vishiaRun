package org.vishia.inspectorAccessor;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessCommFactory;
import org.vishia.communication.InterProcessCommFactoryAccessor;

/**This class is one communication port for a target communication.
 * <ul>
 * <li>A communication port is associated with one instance of {@link InterProcessComm}.
 * <li>A communication port can handle more as one devices. It associates a Map of {@link #targetAccessors}.
 * <li>A communication port contains the {@link #receiveThread} and the {@link #receiveRun} routine.
 * </ul>
 * @author Hartmut Schorrig
 *
 */
public class InspcCommPort implements Closeable
{
  private InterProcessComm ipc;
  
  String sOwnIpAddr;
  
  /**All access instances which uses this port. The key is the targetAddr.toString().
   * 
   */
  Map<String, InspcTargetAccessor> targetAccessors = new TreeMap<String, InspcTargetAccessor>();
  
  Runnable receiveRun = new Runnable()
  { @Override public void run()
    { receiveFromTarget();
    }
  };
  

  /**A receive thread should be used anyway if a socket receiving or other receiving is given.
   * Because: Anytime any telegram can be received, the receiver buffer should be cleared,
   * also if the telegram is unexpected.
   */
  Thread receiveThread = new Thread(receiveRun, "inspcRxThread");
  
  boolean bRun, bFinish, bWaitFinish;
  
  Address_InterProcessComm targetSenderAddr;
  

  public InspcCommPort(){
    
  }
  
  
  /**Register an target access instance which uses this port.
   * An instance can be registered more as one time, especially for any usage.
   * This routine checks whether it is registered already, it registers one time.
   * The registration is necessary for association of an answer telegram for this port.
   * @param accessor
   */
  void registerTargetAccessor(InspcTargetAccessor accessor){
    String key = accessor.targetAddr.toString();
    if(targetAccessors.get(key) ==null){
      targetAccessors.put(key, accessor);
    }
  }
  
  
  
  public Address_InterProcessComm createTargetAddr(String sAddr){
    return ipc.createAddress(sAddr);
  }
  
  
  public boolean open(String sOwnIpAddrP)
  {
    this.sOwnIpAddr = sOwnIpAddrP;
    InterProcessCommFactory ipcFactory = InterProcessCommFactoryAccessor.getInstance();
    ipc = ipcFactory.create (sOwnIpAddrP);
    targetSenderAddr = ipc.createAddress();
    int ipcOk = ipc.open(null, true);
    if(ipcOk >=0){  ipcOk = ipc.checkConnection(); }
    if(ipcOk == 0){
    }
    if(ipcOk < 0){
      System.out.println("Problem can't open socket: " + sOwnIpAddrP); 
    } else {
      receiveThread.start();   //start it after ipc is ok.
    }
    return ipcOk == 0;
  }
  

  /**Closes the thread for receive
   * @see java.io.Closeable#close()
   */
  @Override public void close() throws IOException
  { if(bRun){ //on error bRun is false
      bRun = false;
      ipc.close();
      synchronized(receiveRun){
        while( !bFinish){ try{ receiveRun.wait(); } catch(InterruptedException exc){}}
      }
    }
  }

  
  
  int send(InspcTargetAccessor targerAccessor, byte[] txBuffer, int lengthDatagram) {
    
    return ipc.send(txBuffer, lengthDatagram, targerAccessor.targetAddr);
  }
  
  
  
  
  void receiveFromTarget()
  { bRun = true;
    int[] result = new int[1];
    while(bRun){
      byte[] rxBuffer = ipc.receive(result, targetSenderAddr);
      if(result[0]>0){
        String keyTargetAccessor = targetSenderAddr.toString();
        InspcTargetAccessor targetAccessor = targetAccessors.get(keyTargetAccessor);
        if(targetAccessor !=null){
          targetAccessor.evaluateRxTelg(rxBuffer, result[0]);
        } else {
          System.out.append("InspcCommPort - receive from unknown target; "+ keyTargetAccessor);
        }
      } else {
        System.out.append("InspcCommPort - receive error");
      }
    }//while
    
    synchronized(receiveRun){ 
      bFinish = true;     //NOTE set in synchronized state, because it should wait for
      receiveRun.notify(); 
    }
  }
  
  

  
  
}
