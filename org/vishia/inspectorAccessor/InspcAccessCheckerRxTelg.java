package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;

/**This class checks whether a received telegram from a target is expected or not.
 * @author Hartmut Schorrig
 *
 */
public class InspcAccessCheckerRxTelg
{

  /**True then awaits a telg.
   * It is set before a telegram is sent.
   */
  boolean awaitTelg;
  
  /**True if wait is called already. */
  boolean bWaiting;
  
  private int awaitSeqNumber;
  
  /**True if a awaited telegram was received. */
  private boolean received;
  
  final InspcDataExchangeAccess.Datagram rxTelg = new InspcDataExchangeAccess.Datagram();
  
  /**Set awaiting an answer with given sequence number. This method should be called before a telegram is sent.
   * If the answer telegram is received quickly after send, that informations should be present already.
   * @param seqNumber The awaiting sequence number, it is the same as the sent sequence number
   */
  void setAwait(int seqNumber)
  {
    this.awaitSeqNumber = seqNumber;
    this.awaitTelg = true;  //info for applyReceivedTelg, anything is awaited
    this.received = false;  //set before send, it can't be received yet.
  }
  
  
  /**Wait for an answer telegram. This routine can be called after sent or in an extra thread,
   * which processes only received telegrams. 
   * @param timeout
   * @return null if timeout, elsewhere the answer telegram with given head and content.
   */
  InspcDataExchangeAccess.Datagram waitForAnswer(int timeout)
  { boolean bAnswer;
    synchronized(this){
      if(received){  //received already before this method is called:
        bAnswer = true;
      } else { //not received, then wait.
        try{ wait(timeout); } catch(InterruptedException exc){}
        bAnswer = received;
      }
    }
    return bAnswer ? rxTelg : null;
  }
  
  
  /**Called in the rx-trhead to apply any received telegram.
   * The Rx-thread is an independent thread, which is in receive loop anytime.
   * @param rxBuffer
   * @param zBuffer
   */
  void applyReceivedTelg(byte[] rxBuffer, int zBuffer)
  {
    if(awaitTelg){
      rxTelg.assignData(rxBuffer, zBuffer);
      int rxSeqnr = rxTelg.getSeqnr();
      if(rxSeqnr == awaitSeqNumber){
        //TODO check if the answer should consist of more as one telg
        synchronized(this){
          if(bWaiting){
            notify();
            bWaiting = false;
          }
          received = true;
        }
      }
    }
  }
  
  
  

}
