package org.vishia.inspectorTarget.example;

/**This is a class which does anything to display it with the reflectPro-target. 
 * It creates an own thread and calculates some sine-curve-values.
 * @author Hartmut Schorrig.
 *
 */
public class WorkingThread
{
	
	public static class Data
	{
		/**Increase of a angle in the step time. It determines the frequency of output signals.
		 * 
		 */
		int dw = 20000;
		
		/**An turning angle in the step time. The angles value represents the float-range -Math.PI to Math.PI
		 */
		int ww;
	
		/**The angle converted to float. */
		float wFloat;
		
		/**Some sine-frequency-values. */
		float ySin, ySin2, ySin3, yCos;
		
		/**Addition of sinus of 1, 2x and 3x frequence.
		 * 
		 */
		float yOut1;
	}		
	
	private final Data data = new Data();
	
	/**The thread functionality.
	 * 
	 */
	private Runnable theThreadRun = new Runnable()
	{ @Override public void run()
		{
		  theThreadMng.bRun = true;
		  while(theThreadMng.bRun){
		  	try{ Thread.sleep(10);} catch(InterruptedException exc){} 
		  	for(int ii=0; ii<100; ++ii){
		  		//runs 100 times the step routine, it is adequate a step-time of 100 us
		  		step();
		  	}
		  }
		}
	};

	private class ThreadMng
	{
		/**True while it runs, set false to terminate the thread.
		 */
		boolean bRun;
		
		private final Thread thread = new Thread(theThreadRun, "WorkingThread");
	}
	
	ThreadMng theThreadMng = new ThreadMng();
	
	WorkingThread()
	{
		
	}
	
	
	/**Start of the thread, should be called initially if the application is built completely. */
	void start(){ theThreadMng.thread.start(); }
	
	/**Terminate the thread for ever, should be called on termination of the application.
	 */
	void terminate(){ theThreadMng.bRun = false; }
	
	final void step()
	{
		data.ww += data.dw;   //increasing of angle. Runs wrapping over PI
		//convert the integer-angle to a float value in range -Pi..PI
		float ww = (float)(data.ww * Math.PI / 0x80000000L); //Note use 0x8..Long because it should be positive!
		if(data.ww >= Math.PI){ 
			data.ww -= 2*Math.PI;  //runs in range -Pi .. +PI 
		}
		data.wFloat = ww;
		//Note: Java knows only the double versions of trigonometric functions.
		//In the embedded control a float is used often (cheaper processors).
		//TODO: org.vishia.bridgeC.Math should be used instead java.lang.Math for float operations
		data.ySin = (float)Math.sin(ww);
		data.ySin2 = (float)Math.sin((float)(2*ww + Math.PI/2));
		data.ySin3 = (float)Math.sin((float)(3*ww));
		data.yCos = (float)Math.cos(ww);
		data.yOut1 = data.ySin + data.ySin2; // + data.ySin3;
		
	}
	
}
