package org.vishia.inspectorAccessor;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.inspector.Datagrams;

public class TestAccessor
{

  InspectorAccessor inspcAccessor = new InspectorAccessor();
  
  
  
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
    int order = inspcAccessor.cmdGetValueByPath("_DSP_.data1.bitField.bits-bit11.");    
    inspcAccessor.send();
    InspcDataExchangeAccess.Datagram answer = inspcAccessor.awaitAnswer(0);
    stop();
    try{ Thread.sleep(1000);} catch(InterruptedException exc){}
    

    
  }

  
  void stop(){}
  
}
