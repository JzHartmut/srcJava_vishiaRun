package org.vishia.inspector;

public class Inspector
{
  
  /**Version and history
   * <ul>
   * <li>2011-11-17 Hartmut new {@link #shutdown()} to end communication thread.
   * <li>2011-01-00 Hartmut Created from C-Sources, then re-translated to C and testet with Java2C
   * </ul>
   */
  public static final int version = 0x20111118; 

	private final ClassContent classContent = new ClassContent();
	
	/**The main cmd executer. There may be more as one {@link CmdConsumer_ifc} */
	private final CmdExecuter cmdExecuter = new CmdExecuter(classContent);
	
	/**The communication class. @java2c=embedded Type:Comm. */
	private final Comm comm;
	
	/**
	 * @param commOwnAddr forex "UDP:0.0.0.0:60078"
	 */
	public Inspector(String commOwnAddr)
	{ 
		comm = new Comm(commOwnAddr, cmdExecuter);
		cmdExecuter.completeConstruction(comm);
		classContent.setAnswerComm(cmdExecuter);
	}
	
	/**Start the execution. */
	public void start(Object rootObj)
	{
		classContent.setRootObject(rootObj);
		comm.start();
		
	}
	
	/**Shutdown the communication, close the thread. This routine should be called 
	 * either on shutdown of the whole system or on closing the inspector functionality.
	 * The inspector functionality can be restarted calling {@link #start(Object)}.
	 * 
	 */
	public void shutdown(){
	  comm.shutdown();
	}
	
}
