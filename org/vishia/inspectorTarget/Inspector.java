package org.vishia.inspectorTarget;

public class Inspector
{
  
  /**Version and history
   * <ul>
   * <li>2015-08-05 Hartmut new {@link #get()} to get the first instance which may be a singleton.
   * <li>2015-08-05 Hartmut chg {@link #classContent} is public to access its methods.
   * <li>2011-11-17 Hartmut new {@link #shutdown()} to end communication thread.
   * <li>2011-01-00 Hartmut Created from C-Sources, then re-translated to C and testet with Java2C
   * </ul>
   */
  public static final String version = "2015-08-05"; 

  private static Inspector singleton; 
  
	/**The sub module ClassContent should be accessible from outside to offer methods of it in the application itself.
	 * There are only a few public ones.
	 */
	public final ClassContent classContent = new ClassContent();
	
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
		if(singleton == null){
		  singleton = this;
		}
	}
	
	
	/**Returns the first instance of the Inspector in this application. Usual only one instance is used,
	 * then it is a singleton. More as one instance is possible, then the first instance is the singleton returned here.
	 * If the Inspector is not created yet, this method returns null. 
	 * 
	 * @return
	 */
	public static Inspector get(){
	  return singleton;
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
