package org.vishia.inspector;

public class Inspector
{

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
	
}
