package org.vishia.inspectorAccessor;

import org.vishia.byteData.ByteDataAccess;
import org.vishia.communication.Address_InterProcessComm;
import org.vishia.communication.Address_InterProcessComm_Socket;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.communication.InterProcessCommFactory;
import org.vishia.communication.InterProcessCommFactoryAccessor;
import org.vishia.inspector.*;

public class InspectorAccessor
{
	
	GenerateOrder orderGenerator = new GenerateOrder();
	
	final byte[] txBuffer = new byte[1400];

	final InspcDataExchangeAccess.Datagram txAccess = new InspcDataExchangeAccess.Datagram();
	
	/**The entrant is the sub-consumer of a telegram on the device with given IP.
	 * A negative number is need because compatibility with oder telegram structures.
	 * The correct number depends on the device. It is a user parameter of connection.
	 */
	int nEntrant = -1;
	
	String sOwnIpAddr = "UDP:127.0.0.1:60099";
	
	String sTargetIpAddr = "UDP:127.0.0.1:60080";
	
	Address_InterProcessComm targetAddr = new Address_InterProcessComm_Socket(sTargetIpAddr);
	
	int nSeqNumber = 0; 
	
	int nEncryption = 0;
	
	final InterProcessComm ipc;
	
	
	int cmdGetValueByPath(String sPath){
		return 0;
	}
	
	InspectorAccessor()
	{
		
		txAccess.assignEmpty(txBuffer);
    //The factory should be loaded already. Then the instance is able to get. Loaded before!
		InterProcessCommFactory ipcFactory = InterProcessCommFactoryAccessor.getInstance();
		ipc = ipcFactory.create (sOwnIpAddr);
		ipc.open(null, true);
		int ipcOk = ipc.checkConnection();
		if(ipcOk == 0){
		  receiveThread.start();   //start it after ipc is ok.
		}
	}
	
	
	
	
	Runnable testRun = new Runnable()
	{
		@Override public void run()
		{
			//Build a telegram
	    txAccess.setHead(nEntrant, nSeqNumber, nEncryption);
	    Datagrams.CmdGetValue infoGetValue = new Datagrams.CmdGetValue();
	    txAccess.addChild(infoGetValue);
			int order = orderGenerator.getNewOrder();
			infoGetValue.set("_DSP_.data1.bitfield.bits-bit11", order);
			int lengthDatagram = txAccess.getLength();
			txAccess.setLengthDatagram(order);
			//send the telegram:
			ipc.send(txBuffer, lengthDatagram, targetAddr);
			
			try{ infoGetValue.wait(1000); } catch(InterruptedException exc){}
			
			try{ Thread.sleep(1000);} catch(InterruptedException exc){}
			
		}
	};
	
	
	
	
	Runnable receiveRun = new Runnable()
	{	@Override public void run()
		{ receiveFromTarget();
		}
	};
	
	Thread receiveThread = new Thread(receiveRun, "inspcRxThread");
	
	
	
	void receiveFromTarget()
	{
		int[] result = new int[1];
		byte[] rxBuffer = ipc.receive(result, targetAddr);
    stop();
	}
	
	
	/**The main method is only for test.
	 * @param args
	 */
	public static void main(String[] args)
	{
		
	  //This class is loaded yet. It has only static members. 
		//The static member instance of the baseclass InterProcessCommFactoryAccessor is set.
		//For C-compiling it is adequate a static linking.
		new org.vishia.communication.InterProcessCommFactorySocket();
		InspectorAccessor main = new InspectorAccessor();
		main.testRun.run();
	}
	
	
	void stop(){}
}
