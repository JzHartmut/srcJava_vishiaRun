package org.vishia.inspectorTarget.example;

import java.util.LinkedList;
import java.util.List;

import org.vishia.communication.InterProcessCommFactoryAccessor;
import org.vishia.inspectorTarget.Inspector;
import org.vishia.util.Java4C;

public class ExampleInspector
{
	static class Data
	{ int x,y;
		short s1, s2;
		float f1, f2;
		double d1, d2;
		String t1, t2;
	  Data(int val){ 
	  	x = val; y = val +10;
	  	t1 = "testString"; t2 = "y=" + y; 
	  }
	}
	
	private final WorkingThread workingThread = new WorkingThread();
	
	private final Data data = new Data(123);
	
  private boolean run;
  
	int intVal;
  
  float floatVal;
  
  @Java4C.FixArraySize(20) 
  final int[] intArray = new int[20];
	
  private int[] dynamicIntArray;
  
  private List<Data> list = new LinkedList<Data>();
  
  double[] dArray = new double[10];
  
  short[] sArray = new short[16];
  
  int sizeList;
  
  private final Inspector inspector = new Inspector("UDP:192.168.1.69:20320");  //60092");
	
	public final static void main(String[] args)
	{
		//This class is loaded yet. It has only static members. 
		//The static member instance of the baseclass InterProcessCommFactoryAccessor is set.
		//For C-compiling it is adequate a static linking.
		new org.vishia.communication.InterProcessCommFactorySocket();
		ExampleInspector main = new ExampleInspector();
		main.setExampleData();
		main.execute();
	}
	
	void setExampleData()
	{ int ix;
		dynamicIntArray = new int[5];
		sizeList = 5;
		for(ix = 0; ix < dynamicIntArray.length; ++ix){
			dynamicIntArray[ix] = ix + 100;
			list.add(new Data(200+ix));
		}
	}
	
	ExampleInspector()
	{
		workingThread.start();
		inspector.start(this);
	}
	
	
	void execute()
	{
	  run = true;
	  while(run)
	  {
	  	try{ Thread.sleep(10); }
	  	catch(InterruptedException exc){
	  	}
	  	int sizeListLast = list.size();
	  	data.d2 = data.d1 * 0.6666667;
	  	if(sizeListLast != sizeList){
	  		while(sizeListLast < sizeList){
	  			list.add(new Data(sizeListLast));
	  			sizeListLast +=1;
	  		}
	  		while(sizeListLast > sizeList){
	  			list.remove(--sizeListLast);
	  		}
	  			
	  	}
	  }
	}
	
}
