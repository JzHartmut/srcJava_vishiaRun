/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.communication;


import java.io.IOException;
import java.text.ParseException;

import org.vishia.util.StringFormatter;

import org.vishia.mainCmd.*;

/** Mainclass for the application ....<br/>
    This is a sample and a example. The user should do the followed to use the class as a template:
    <ul><li>copy this file and cmdLineSampleExec.java under the requested name and package</li>
        <li>correct the package line</li>
        <li>replace 'CmdLineSample' with the user's main class name</li>
        <li>replace 'CmdLineSampleExec' with the user's first level application class name</li>
        <li>look on all :TODO: -location and change/complete the source there</li>
    </ul>
*/

public class SocketTester
{

  static class CmdArgs {
    /**Cmdline-argument, set on -i option. Inetaddress.*/
    String sDestinationInetAddress = "localhost";
    
    int nDestinationPort = 1002;
    int nReceivePort = 1001;

    /**Cmdline-argument, set on -o option. Outputfile to output something. :TODO: its a example.*/
    String sFileOut = null;

    
    String sOwnAddress = "UDP:127.0.0.1:0xeac0";

  }
  
  
  
  
  class UDPdebugTransmitter extends Thread
  {
    byte[] bufferInput = new byte[100];
    
    @Override
    public void run()
    { int nChars = 0;
      
      if(false)
      { bufferInput[0]=0x58;
        bufferInput[1]=0x79;
        int nSentBytesTest = comm.send(bufferInput, 2, dstAddress);
      }  
          
    
      while(nChars >=0)
      { try{ nChars = System.in.read(bufferInput);}
        catch (IOException e)
        { nChars = -1;
        }
        if(nChars > 0)
        {
          int nSentBytes = comm.send(bufferInput, nChars, dstAddress);
          main.writeInfoln("sent " + nSentBytes + " bytes:");
          { int length = nChars;
            int idx = 0;
            while(length > 0)
            { int nBytes = length > 16 ? 16 : length;
              StringFormatter output = new StringFormatter(120);
              output.addHexLine(bufferInput, idx, nBytes, StringFormatter.k1);
              main.writeInfoln(output.toString());
              try{ output.close();} catch(IOException exc) {}
              length -= nBytes;
              idx += nBytes;
            }
          }
          if(nSentBytes < 0)
          { main.writeError("sent error: "+ comm.translateErrorMsg(nSentBytes));
          }
        
        }
      }
    }
  }
  
  
  
  class UDPdebugReceiver extends Thread
  {
  
    boolean bAbort = false;  
  
    /** helper class to evaluate dataInput*/
    //private Element_CodeGraphicsData traceInput = new Element_CodeGraphicsData();
  
  
    UDPdebugReceiver(String sFileOut)
    { 
    }
  
  
  
    @Override
    public void run()
    { int[] receivedBytes = new int[1];
      while(!bAbort)
      { 
        byte[] data = comm.receive(receivedBytes, senderAddress);
        if(receivedBytes[0] < 0)
        {
          main.writeError("receive Error socket:" + comm.translateErrorMsg(receivedBytes[0]));
          bAbort = true; //kill the task.
        }
        else
        { int length = receivedBytes[0];
          int idx = 0;
          main.writeInfoln("received " + length + "bytes from ");
          while(length > 0)
          { int nBytes = length > 64 ? 64 : length;
            StringFormatter output = new StringFormatter(120);
            output.addHexLine(data, idx, nBytes, StringFormatter.k4right);
            main.writeInfoln(output.toString());
            try{ output.close();} catch(IOException exc) {}
            length -= nBytes;
            idx += nBytes;
          }
          
        }
      }
    }


 }

  
  final CmdArgs args;
  
  
  final Report report;

  /** The Interprocess communication. */
  InterProcessComm comm;
  
  Address_InterProcessComm dstAddress;
  
  Address_InterProcessComm ownAddress;
  
  
  Address_InterProcessComm senderAddress;
  
  SocketTester(CmdArgs args, Report report, MainCmd_ifc main){
    this.report = report;
    this.args = args;
    this.main = main;
  }

  
  
  /** Unified access to main as MainCmd_ifc*/
  final MainCmd_ifc main;
  
  /*---------------------------------------------------------------------------------------------*/
  /** main started from java
   * 
   * -i192.168.1.67 -p5555 -rUDP:192.168.1.69:20320
   * 
   * 
   * */
  public static void main(String [] args) { 
    CmdLine cmd = new CmdLine(args);
  boolean bOk = true;
  int error;
  try{ cmd.parseArguments(); }
  catch(Exception exception)
  { cmd.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
    cmd.exit();
  }
  //main.testStringBufferFormat();
    SocketTester main = new SocketTester(cmd.args, cmd, cmd);
    new InterProcessCommFactorySocket();  //initializes the static member of InterProcessCommFactoryAccessor.
    main.execute();
    cmd.exit();
  }

  

  /** Executes the cmd-line-application. The functionality application class is created inside this method
  independent of the command line invoke.
*/
void execute()
{ 
  /** The execution class knows the SocketTester Main class in form of the MainCmd super class
      to hold the contact to the command line execution.
  */
  //comm = InterProcessCommFactoryAccessor.getInstance().create("UDP:0.0.0.0", nReceivePort);
  
  InterProcessCommFactory ipcFactory = InterProcessCommFactory.getInstance();
  comm = ipcFactory.create(args.sOwnAddress); //It creates and opens the UDP-Port.
  
  
  //dstAddress    = InterProcessCommFactory.makeRemoteAddress(sDestinationInetAddress, nDestinationPort);
  dstAddress    = comm.createAddress("127.0.0.1", args.nDestinationPort);
  senderAddress = comm.createAddress();
  ownAddress    = comm.createAddress(args.sOwnAddress);
  
  int error = comm.open(ownAddress, true); //InterProcessComm.receiverShouldbeBlocking);
  if(error <0)
  { main.writeError("Problem at open socket" + comm.translateErrorMsg(error));
  }
  else      
  {
    UDPdebugReceiver rxExec = new UDPdebugReceiver(args.sFileOut);
    UDPdebugTransmitter txExec = new UDPdebugTransmitter();

    rxExec.start();
    System.out.println("SocketTester receives from " + args.sOwnAddress);
    try
    { //rxExec.start();
      //txExec.start();
      int nrWait = 0;
      while(true)
      { System.out.print("+");
        synchronized(this)
        { wait(10000);
        }
      }
    }
    catch(Exception exception)
    { //catch the last level of error. No error is reported direct on command line!
      main.report("Uncatched Exception on main level:", exception);
      main.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
    }
  }  
//note: exit the command line application in static main()
}



  
  
  
  
  
  private static class CmdLine extends MainCmd {
    /*---------------------------------------------------------------------------------------------*/
    /*::TODO:: for every argument of command line at least one variable must be existed.
      The variable is to set in testArgument.
      It is possible that one variable can used in several command line arguments.
    */

    
    CmdArgs args = new CmdArgs();
    

    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the main class.
        The command line arguments are parsed here. After them the execute class is created as composition of SocketTester.
    */
    CmdLine(String[] args)
    { super(args);
      //:TODO: user, add your help info!
      //super.addHelpInfo(getAboutInfo());
      super.addAboutInfo("UDP-debug-receiver");
      super.addAboutInfo("made by HSchorrig, 2006-04-01");
      super.addHelpInfo("param: -aIADR -pPORT -oOUT");
      super.addHelpInfo("-iIADR  The destination internetAdress, default is localhost");
      super.addHelpInfo("-pPORT  The destination port, default is 1002");
      super.addHelpInfo("-rPORT  The own port, default is UDP:127.0.0.1.0xeac0");
      super.addHelpInfo("-oOUT   File to write the received data.");
      super.addStandardHelpInfo();

    }


    
    
    
    
    void testStringBufferFormat()
    { byte[] data = new byte[16];
      for(int i=0; i<data.length; i++){ data[i] = (byte)(i);}
      StringFormatter output = new StringFormatter(100);
      output.addHexLine(data, 0, 16, StringFormatter.k4right);
      try{ output.close();} catch(IOException exc) {}
      super.reportln(Report.info, output.toString());
    }









    /*---------------------------------------------------------------------------------------------*/
    /** Tests one argument. This method is invoked from parseArgument. It is abstract in the superclass MainCmd
        and must be overwritten from the user.
        :TODO: user, test and evaluate the content of the argument string
        or test the number of the argument and evaluate the content in dependence of the number.

        @param argc String of the actual parsed argument from cmd line
        @param nArg number of the argument in order of the command line, the first argument is number 1.
        @return true is okay,
                false if the argument doesn't match. The parseArgument method in MainCmd throws an exception,
                the application should be aborted.
    */
    @Override
    public boolean testArgument(String arg, int nArg) {
      boolean bOk = true;  //set to false if the argc is not passed

      if(arg.startsWith("-i"))      
      { args.sDestinationInetAddress = getArgument(2);
      }
      else if(arg.startsWith("-p"))
      { String sValue = getArgument(2);
        try
        { Integer val = Integer.decode(sValue);
        args.nDestinationPort = val.intValue();
        }
        catch(NumberFormatException exception)
        { writeError("Parameter -p is not a valid integer:" + sValue);
        }
      }
      else if(arg.startsWith("-r"))
      { args.sOwnAddress = getArgument(2);
        
      }
      else if(arg.startsWith("-o"))
      { args.sFileOut = getArgument(2);
      }
      else bOk=false;

      return bOk;
    }

    /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
     * and the application is terminated. The user should overwrite this method if the call without comand line arguments
     * is meaningfull.
     * @throws ParseException 
     *
     */
    @Override
    protected void callWithoutArguments() throws ParseException
    { //:TODO: overwrite with empty method - if the calling without arguments
      //having equal rights than the calling with arguments - no special action.
      //super.callWithoutArguments();  //it needn't be overwritten if it is unnecessary
    }




    /*---------------------------------------------------------------------------------------------*/
    /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       :TODO: the user only should determine the specific checks, this is a sample.
       @return true if successfull, false if failed.
    */
    @Override
    protected boolean checkArguments()
    { boolean bOk = true;

      //if(sFileOut == null)           { writeWarning("argument -o no outputfile is given, use default"); sFileOut = "out.txt";}
      //else if(sFileOut.length()==0)  { bOk = false; writeError("argument -o without content"); }

      if(!bOk) setExitErrorLevel(exitWithArgumentError);

      return bOk;

   }

    
    
    
    

  }
  
}







