package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;

public class CheckerRxTelg
{

  /**True then awaits a telg.
   * It is set before a telegram is sent.
   */
  boolean awaitTelg;
  
  /**True if wait is called already. */
  boolean bWaiting;
  
  int awaitSeqNumber;
  
  boolean received;
  
  final InspcDataExchangeAccess.Datagram rxTelg = new InspcDataExchangeAccess.Datagram();
  
  /**Set awaiting status. Called before a telegram is sent.
   * @param seqNumber The awaiting sequence number, it is the same as the sent sequence number
   */
  void setAwait(int seqNumber)
  {
    this.awaitSeqNumber = seqNumber;
    this.awaitTelg = true;
    this.received = false;
  }
  
  
  InspcDataExchangeAccess.Datagram waitForAnswer(int timeout)
  { boolean bAnswer;
    synchronized(this){
      if(received){
        bAnswer = true;
      } else {
        try{ wait(timeout); } catch(InterruptedException exc){}
        bAnswer = received;
      }
    }
    return bAnswer ? rxTelg : null;
  }
  
  
  /**Called in the rx-trhead
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
