package org.vishia.inspcPC;

import java.io.IOException;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InspcDataExchangeAccess.Inspcitem;
import org.vishia.msgDispatch.LogMessage;


/**Simple class which sets only "ok" or a value on receive.
 * @author Hartmut Schorrig
 *
 */
public class InspcRxOk implements InspcAccessExecRxOrder_ifc
{
  long timeReceived;
  boolean bOk;
  boolean bAnswer;
  
  String sAnswer;
  
  public InspcRxOk() {
    timeReceived = -1;
  }
  
  @Override public void execInspcRxOrder(Inspcitem info, long time, LogMessage log, int identLog)
  {
    timeReceived = time;
    bOk = false; //default, set true
    bAnswer = true;
    int cmd = info.getCmd();
    switch(cmd){
      case InspcDataExchangeAccess.Inspcitem.kFailedCommand: sAnswer = "failed command"; break;
      case InspcDataExchangeAccess.Inspcitem.kFailedHandle: sAnswer = "failed handle"; break;
      case InspcDataExchangeAccess.Inspcitem.kFailedPath: sAnswer = "failed datapath"; break;
      case InspcDataExchangeAccess.Inspcitem.kFailedValue: sAnswer = "failed value"; break;
      case InspcDataExchangeAccess.Inspcitem.kFailedRegisterRepeat: sAnswer = "failed register handle"; break;
      default: sAnswer = "ok"; bOk = true;
    }//switch
  }

  @Override public Runnable callbackOnAnswer()
  { return null;  //no callback.
  }
  
  
  public void clear(){
    timeReceived = -1;
    bOk = false;
    bAnswer = false;
    sAnswer = null;
  }


  public String await(int maxMillisec, Appendable out) {
    long time = System.currentTimeMillis();
    long timeLast = time + maxMillisec;
    while(!bAnswer && (System.currentTimeMillis() - timeLast) < 0) {
      try { Thread.sleep(200); } catch (InterruptedException e) { }
    }
    String ret;
    if(!bAnswer) ret = "faulty communication";
    else ret = sAnswer;  //typical null on bOk
    clear();
    if(out !=null) try { out.append(ret).append('\n'); } catch(IOException exc){ System.err.println("InspcRxOk - IOException"); }
    return ret;
  }
}
