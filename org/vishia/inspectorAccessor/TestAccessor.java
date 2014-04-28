package org.vishia.inspectorAccessor;

import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InspcDataExchangeAccess.Inspcitem;
import org.vishia.inspector.InspcTelgInfoSet;
import org.vishia.msgDispatch.LogMessage;

public class TestAccessor
{

  InspcCommPort targetCommPort = new InspcCommPort();
  
  InspcTargetAccessor inspcAccessor;
  
  
  /**The main method is only for test.
   * @param args
   */
  public static void main(String[] args)
  {
    
    //This class is loaded yet. It has only static members. 
    //The static member instance of the baseclass InterProcessCommFactoryAccessor is set.
    //For C-compiling it is adequate a static linking.
    new org.vishia.communication.InterProcessCommFactorySocket();
    TestAccessor main = new TestAccessor();
    main.execute();
  }
  
  
  void execute()
  {
    String sIpOwn = "UDP:0.0.0.0:60099";
    String sIpTarget = "UDP:127.0.0.1:60080";
    
    String sPathInTarget = "workingThread.data.yCos.";
    
    Address_InterProcessComm addrTarget = targetCommPort.createTargetAddr(sIpTarget);
    inspcAccessor = new InspcTargetAccessor(targetCommPort, addrTarget);

    //String sPathInTarget2 = "_DSP_.data1.bitField.bits-bit11."; 
    if(targetCommPort.open(sIpOwn))
    {
      inspcAccessor.setTargetAddr(sIpTarget); 
      while(true){
        int order = inspcAccessor.cmdGetValueByPath(sPathInTarget, testExec);    
        //inspcAccessor.rxEval.setExpectedOrder(order, null);
        inspcAccessor.cmdFinit();
        InspcDataExchangeAccess.InspcDatagram[] answer = inspcAccessor.awaitAnswer(1000);
        if(answer !=null){
          long time = System.currentTimeMillis();
          String sError = inspcAccessor.evaluate(answer[0], testExec, time, null, 0);
        }
        try{ Thread.sleep(300);} catch(InterruptedException exc){}
      }
      //targetCommPort.close();
    }
    

    
  }


  
  InspcAccessExecRxOrder_ifc testExec = new InspcAccessExecRxOrder_ifc()
  {

    @Override public void execInspcRxOrder(Inspcitem info, long time, LogMessage log, int identLog)
    {
      int order = info.getOrder();
      int cmd = info.getCmd();
      try{
        if(cmd == InspcDataExchangeAccess.Inspcitem.kAnswerValue){
          float value = InspcTargetAccessor.valueFloatFromRxValue(info, InspcTargetAccessor.getInspcTypeFromRxValue(info));
          System.out.println("" + value);
        }
      } catch(Exception exc){
        stop();
      }
    }

    @Override public Runnable callbackOnAnswer(){return null; }  //empty

  };
  
  
  
  void stop(){}
  
}
