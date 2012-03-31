package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InspcDataExchangeAccess.Info;
import org.vishia.inspector.InspcTelgInfoSet;

public class TestAccessor
{

  InspcAccessor inspcAccessor = new InspcAccessor(new InspcAccessEvaluatorRxTelg());
  
  
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
    //String sPathInTarget2 = "_DSP_.data1.bitField.bits-bit11."; 
    if(inspcAccessor.open(sIpOwn))
    {
      inspcAccessor.setTargetAddr(sIpTarget); 
      while(true){
        int order = inspcAccessor.cmdGetValueByPath(sPathInTarget);    
        inspcAccessor.rxEval.setExpectedOrder(order, null);
        inspcAccessor.send();
        InspcDataExchangeAccess.Datagram[] answer = inspcAccessor.awaitAnswer(1000);
        if(answer !=null){
          String sError = inspcAccessor.rxEval.evaluate(answer, testExec);
        }
        try{ Thread.sleep(300);} catch(InterruptedException exc){}
      }
    }
    

    
  }


  
  InspcAccessExecRxOrder_ifc testExec = new InspcAccessExecRxOrder_ifc()
  {

    @Override public void execInspcRxOrder(Info info)
    {
      int order = info.getOrder();
      int cmd = info.getCmd();
      try{
        if(cmd == InspcDataExchangeAccess.Info.kAnswerValue){
          float value = InspcAccessEvaluatorRxTelg.valueFloatFromRxValue(info, InspcAccessEvaluatorRxTelg.getInspcTypeFromRxValue(info));
          System.out.println("" + value);
        }
      } catch(Exception exc){
        stop();
      }
    }
    
  };
  
  
  
  void stop(){}
  
}
