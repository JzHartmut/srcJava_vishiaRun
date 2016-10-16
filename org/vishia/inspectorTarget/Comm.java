package org.vishia.inspectorTarget;

import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessCommFactory;
import org.vishia.communication.InterProcessCommFactoryAccessor;
import org.vishia.util.Assert;
import org.vishia.util.Java4C;

public class Comm implements Runnable
{
  /**Version and history
   * <ul>
   * <li>2011-11-17 Hartmut new {@link #shutdown()} to end communication thread.
   * <li>2011-01-00 Hartmut Created from C-Sources, then re-translated to C and testet with Java2C
   * </ul>
   */
  public static final int version = 0x20111118; 

  /**@java2c=simpleRef. */
	private final CmdExecuter cmdExecuter;
	
	/**State of function.
	 * <ul>
	 * <li>o: opened
	 * <li>e: error (open error)
	 * <li>x: exit
	 * <li>z: is exited.
	 * </ul>
	 * 
	 */
	private char state;
	
	private boolean bEnablePrintfOnComm;
	
	/**@java2c=simpleRef. */
	private final InterProcessComm ipc;
	
	private int ctErrorTelg;
	
	/**@java2c=simpleRef. */
	private final Thread thread;
	
	/**@java2c=simpleArray. */
	private final int[] nrofBytesReceived = new int[1];
	
	/**Use a static receive buffer. It is important for C-applications. */
	@Java4C.SimpleArray
	private final byte[] data_rxBuffer = new byte[1500];
	
	/**For C: store the reference and length of the SimpleArray in the next structure. */
	@Java4C.PtrVal private final byte[] rxBuffer =  data_rxBuffer;
	
	/**@java2c=simpleRef. */
	private final Address_InterProcessComm myAnswerAddress;
	
	/**Creates the communication for the inspector.
	 * The InterProcessComm interface implementation is got depending on
	 * <ul><li>the ownAddrIpc-string
	 * <li>the existing InterProcessComm-Implementation, which analyzes the address-string.
	 * <ul>
	 * It means, the communication is not determined from this implementation, it depends
	 * on the parameter of the ownAddrIpc and the possibilities. 
	 * @param ownAddrIpc The address String
	 * @param cmdExecuter aggregation of executer of all commands.
	 */
	public Comm(String ownAddrIpc, CmdExecuter cmdExecuter)
	{ this.cmdExecuter = cmdExecuter;
	  /**use the existent factory, it is determined by linker or classLoader, which it is.
	   * @java2c=dynamic-call. */
		InterProcessCommFactory ipcFactory = InterProcessCommFactory.getInstance();
		/**The interProcessCommunication, depends from the factory and the own Address.
		 * It is not socket anyway.
		 * @java2c=dynamic-call.  */
		InterProcessComm ipcMtbl = ipcFactory.create (ownAddrIpc);
		//create the correct type for the sender-adress.
		myAnswerAddress = ipcMtbl.createAddress();  //empty address for receiving and send back
		//create the receive-thread
		thread = new Thread(this, "Inspc");
		//set it to class ref.
		this.ipc = ipcMtbl;
	}
	
	
	public final boolean openComm(boolean blocking) {
    int ok;
    /**@java2c=dynamic-call. */
    InterProcessComm ipcMtbl = ipc; 
    //------------------------------------------->Open the communication:
    ok = ipcMtbl.open(null, blocking);
		state = (ok >=0 ? 'o' : 'e');
    if(bEnablePrintfOnComm)
    { //only for debug:
    	if(ok >=0)
      { System.out.print("\nopen OBMA-Communication ok\n"); 
      }      
      else
      { System.out.format("\nopen OBMA-Communication error: %d\n", -ok);
      }
    }
    return ok >=0;  //true if InterProcessComm.open() successfully
  }



	
	final public void start(){
		thread.start();
	}
	
	
	@Override public void run()
	{ while(state != 'x'){
		  openComm(true);
		  if(state == 'o'){
		  	receiveAndExecute();
		  } else {
		  	state = 'E';
		  	while(state == 'E'){
	        try{ Thread.sleep(1000); } catch(InterruptedException exc){}
	        //check state after a waiting time, repeat open.
	      }
		  }
	  }
	  synchronized(this){
  	  state = 'z';
  	  notify();
	  }
		
	}
	
	
	private final void receiveAndExecute()
	{
		/**The interface for answer: It is implemented in cmdExecuter. @java2c=dynamic-call. */
  	@Java4C.DynamicCall final AnswerComm_ifc answerCommMtbl = this.cmdExecuter; 
  	/**@java2c=dynamic-call. */
  	@Java4C.DynamicCall final CmdExecuter cmdExecuterMtbl = this.cmdExecuter; 
  	/**@java2c=dynamic-call. */
  	@Java4C.DynamicCall final InterProcessComm ipcMtbl = ipc;  //java2c: build Mtbl
    while(state !='x'){  //x to terminate
	    //chgData_TestData_Inspc(ythis->testInspc);   //only for test.
      this.nrofBytesReceived[0] = 0; //expected the nrof available data
    	//
      //----> receiveData(...)
      //
      state = 'r';  //receive
	  	try{
        ipcMtbl.receiveData(this.nrofBytesReceived, this.rxBuffer, this.myAnswerAddress);
        if(state !='x'){
    	  	if(nrofBytesReceived[0] <0){ //error situation
            //it is possible that a send request has failed because the destination port is not
          	//able to reach any more. Therefore wait a moment and listen new
          	state = 'e';  //prevent send
            //wait a moment! If it is a permanent error, a loop can be occur. Prevent it. Let other threads work.
          	try{ Thread.sleep(50); } catch(InterruptedException exc){}
          	state = 'r';
            //
            //cmdExecuterMtbl.executeCmd(rxBuffer, nrofBytesReceived[0]);
          } else {
          	cmdExecuterMtbl.executeCmd(rxBuffer, nrofBytesReceived[0]);      
    	      //unnecessary because usage receiveData: ipcMtbl.freeData(rxBuffer);
    	    }
        }
	  	} catch(Exception exc){
	  	  /** @java2c=toStringNonPersist, StringBuilderInStack=100. */
	      CharSequence msg = Assert.exceptionInfo("org.vishia.inspector.Comm - unexpected Exception; ", exc, 0, 7);
	  	  System.err.println(msg);
	  	  exc.printStackTrace( System.err);
	  	}
	  }//while state !='x'
	}

  
  /**Sends the answer telg to the sender of the received telegram. 
   * If the receiving process fails, the answer isn't send. This situation can occur only 
   * if the preparation of the answer runs in another thread.
   * @param bufferAnswerData The bytes to send.
   * @param nrofBytesAnswer number of bytes to send.
   * @return number of bytes sent or a negative value on error.
   */
  public final int sendAnswer(@Java4C.PtrVal byte[] bufferAnswerData, int nrofBytesAnswer)  
  { int nrofSentBytes;
  	@Java4C.DynamicCall InterProcessComm ipcMtbl = ipc;  //java2c: build Mtbl-pointer.
		nrofSentBytes = ipcMtbl.send(bufferAnswerData, nrofBytesAnswer, myAnswerAddress);
		if(nrofSentBytes < 0)
		{ if(bEnablePrintfOnComm){
			  //only for debug.
				System.out.print("\nError InterProcessComm "); 
			}
			nrofSentBytes = -2;
		}
		/*
    */
		if(bEnablePrintfOnComm){ System.out.print("<"); }
		return nrofSentBytes;
  }

  
  /**Shutdown the communication, close the thread. This routine should be called 
   * either on shutdown of the whole system or on closing the inspector functionality.
   * The inspector functionality can be restarted calling {@link #start(Object)}.
   */
  public void shutdown(){
    state = 'x';
    @Java4C.DynamicCall InterProcessComm ipcMtbl = ipc; 
    ipcMtbl.close();  //breaks waiting in receive socket
    //waits till the receive thread is finished.
    while(state !='z'){
      synchronized(this){
        try{ wait(100); } catch(InterruptedException exc){}
      }
    }
  }

  
}
