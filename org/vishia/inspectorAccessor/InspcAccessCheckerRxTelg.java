package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.msgDispatch.LogMessage;

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
  
  /**This aggregation is null if the answer telegram should not evaluated in the receiving thread.
   * If is should evaluated in the receiving thread, this is the method to evaluate. */
  private InspcAccessExecAnswerTelg_ifc executerAnswer;
  
  /**True if wait is called already. */
  private boolean bWaiting;
  
  private int awaitSeqNumber;
  
  /**True if a awaited telegram was received. */
  private boolean received;
  
  final InspcDataExchangeAccess.ReflDatagram rxTelg = new InspcDataExchangeAccess.ReflDatagram();

  /**Accumulator for all answer telegrams. Usual it is only 1 telegram. But up to the max number may be received.
   * The communication supports only 1 send telegram, but with more as one info blocks.
   * The answer telegram can contain 1 or more info blocks. The answers may be longer as the requests.
   * Therefore more as one answer telegrams can be received.
   */
  private InspcDataExchangeAccess.ReflDatagram[] answerTelgs; 
  

  public InspcAccessCheckerRxTelg()
  {
  }
  
  void setExecuterAnswer(InspcAccessExecAnswerTelg_ifc executerAnswer)
  { this.executerAnswer = executerAnswer;
  }
  
  
  public boolean hasAnwer(){ return received; }
  
  
  /**Set awaiting an answer with given sequence number. This method should be called before a telegram is sent.
   * If the answer telegram is received quickly after send, that informations should be present already.
   * @param seqNumber The awaiting sequence number, it is the same as the sent sequence number
   * @param executerAnswer if not null, then the method of this interface will be called in the receiver thread
   *        if all answer telegrams are received. 
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
  InspcDataExchangeAccess.ReflDatagram[] waitForAnswer(int timeout)
  { boolean bAnswer;
    synchronized(this){
      if(received){  //received already before this method is called:
        bAnswer = true;
      } else { //not received, then wait.
        bWaiting = true;
        try{ wait(timeout); } catch(InterruptedException exc){}
        bAnswer = received;
      }
    }
    if(bAnswer){
      InspcDataExchangeAccess.ReflDatagram[] answers = new InspcDataExchangeAccess.ReflDatagram[1];
      answers[0] = answerTelgs[0];
      return answers;
    } else {
      return null;
    }
  }
  
  
  /**Called in the rx-trhead to apply any received telegram.
   * The Rx-thread is an independent thread, which is in receive loop anytime.
   * @param rxBuffer
   * @param zBuffer
   */
  void applyReceivedTelg(byte[] rxBuffer, int zBuffer, LogMessage log, int identLog)
  {
    if(awaitTelg){
      rxTelg.assignData(rxBuffer, zBuffer);
      int rxSeqnr = rxTelg.getSeqnr();
      if(rxSeqnr == awaitSeqNumber){
        //TODO check if the answer should consist of more as one telg
        answerTelgs = new InspcDataExchangeAccess.ReflDatagram[1];
        answerTelgs[0] = rxTelg;
        if(executerAnswer !=null){
          if(bWaiting){
            throw new RuntimeException("Software error. awaitSeqNumber() with an executer was called. "
              + "Than waitForAnswer() must not be called!"); 
          }
          executerAnswer.execInspcRxTelg(answerTelgs, log, identLog);
        } else {
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
  
  
  

}
