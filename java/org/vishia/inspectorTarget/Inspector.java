package org.vishia.inspectorTarget;

import org.vishia.bridgeC.MemSegmJc;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessCommFactory;
import org.vishia.communication.InterProcessComm_SocketImpl;
import org.vishia.reflect.ClassJc;

/**This is the main class for the Inspector for Java and C.
 * The inspector helps inspect data via reflection. It is a service. 
 * It uses an {@link InterProcessComm} implementation for communication with any client.
 * 
 * To use the Inspector in Java you should insert the following lines on start of an Java program:
 * <pre>
 * class MyTestClass {
 * 
 *   void startRoutine() {
 *     //Instantiate the InterprocessComm implementation for sockets
 *     new org.vishia.communication.InterProcessCommFactorySocket();
 *     //Instance for Inspector service:
 *     Inspector inspc = new Inspector("UDP:0.0.0.0:60094");
 *     inspc.start(this);
 *   }
 *   
 *   void terminateRoutine() {
 *     Inspector.get().shutdown(this);
 *   }
 *   
 *   
 *   ......
 * </pre>
 * <ul>
 * <li>The {@link InterProcessComm_SocketImpl} instantiates a singleton referred with {@link InterProcessCommFactory#getInstance()}
 *   for socket communication. This is recommended on PC platforms.
 * <li>The {@link #start(Object)} routine initiates the Inspector service with the given instance as root instance.
 * <li>The {@link #shutdown()} is necessary especially if a Java frame still runs but the application with the Inspector should be terminate.
 *   It closes the communication, close the socket. If the whole Java application is end as process on the operation system, the communication is closed from the system already. 
 * <li>That is all. Now the socket communication can access via UDP with the given IP and Port.
 * </ul> 
 *     
 * @author Hartmut Schorrig
 *
 */
//tag::class_Inspector_Head[]
public class Inspector
{
//end::class_Inspector_Head[]
  
  /**Version and history
   * <ul>
   * <li>2015-08-05 Hartmut new {@link #get()} to get the first instance which may be a singleton.
   * <li>2015-08-05 Hartmut chg {@link #classContent} is public to access its methods. 
   * <li>2011-11-17 Hartmut new {@link #shutdown()} to end communication thread.
   * <li>2011-01-00 Hartmut Created from C-Sources, then re-translated to C and testet with Java2C
   * </ul>
   */
  public static final String version = "2015-08-05"; 

//tag::class_Inspector_Data[]
  private static Inspector singleton; 
  
	/**The sub module ClassContent should be accessible from outside to offer methods of it in the application itself.
	 * There are only a few public ones.
	 */
	public final ClassContent classContent = new ClassContent();
	
	/**The main cmd executer. There may be more as one {@link CmdConsumer_ifc} */
	private final CmdExecuter cmdExecuter = new CmdExecuter(classContent);
	
	/**The communication class. @java2c=embedded Type:Comm. */
	private final Comm comm;
//end::class_Inspector_Data[]
	
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
  
  /**Start the execution. */
  public void start(ClassJc rootClazz, MemSegmJc rootAddr)
  {
    classContent.setRootObject(rootClazz, rootAddr);
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
