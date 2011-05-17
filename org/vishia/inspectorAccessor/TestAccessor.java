package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InspcDataExchangeAccess.Info;
import org.vishia.inspector.Datagrams;

public class TestAccessor
{

  InspcAccessor inspcAccessor = new InspcAccessor();
  
  InspcAccessEvaluatorRxTelg inspcRxEval = new InspcAccessEvaluatorRxTelg();
  
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
    while(true){
      int order = inspcAccessor.cmdGetValueByPath("_DSP_.data1.bitField.bits-bit11.");    
      inspcRxEval.setExpectedOrder(order, null);
      inspcAccessor.send();
      InspcDataExchangeAccess.Datagram answer = inspcAccessor.awaitAnswer(1000);
      String sError = inspcRxEval.evaluate(answer, testExec);
      stop();
      try{ Thread.sleep(1000);} catch(InterruptedException exc){}
    }

    
  }


  
  InspcAccessExecRxOrder testExec = new InspcAccessExecRxOrder()
  {

    @Override public void execInspcRxOrder(Info info)
    {
      int order = info.getOrder();
      int cmd = info.getCmd();
      try{
        if(cmd == InspcDataExchangeAccess.Info.kAnswerValue){
          float value = InspcAccessEvaluatorRxTelg.valueFloatFromRxValue(info);
          System.out.println("" + value);
        }
      } catch(Exception exc){
        stop();
      }
    }
    
  };
  
  
  
  void stop(){}
  
}
