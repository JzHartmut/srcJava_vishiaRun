package org.vishia.msgDispatch;

import java.util.TimeZone;

import org.vishia.util.FileSystem;

/**This class replaces the System.out and System.err with 2 inputs which creates
 * a LogMessage and dispatch it with the Message Dispatcher.
 * Without any other effort both outputs will be dispatch to the originally
 * System.out and System.err output, but with additional time stamp and identification number.
 * <br><br>
 * The messages can be redirected and prevented using the {@link MsgDispatcher} capability.
 * 
 * @author Hartmut Schorrig
 *
 */
public class MsgDispatchSystemOutErr {

  private static final int ixMsgOutputStdOut = 0;
  private static final int ixMsgOutputStdErr = 1;
  private static final int ixMsgOutputFile = 2;
  
  
  final MsgPrintStream printOut, printErr;
  
  public final MsgDispatcher msgDispatcher;

  LogMessageFile fileOutput;
  
  TimeZone timeZoneForFile = TimeZone.getTimeZone("GMT");
  
  LogMessageStream cmdlineOut, cmdlineErr;
  
  static MsgDispatchSystemOutErr singleton;
  
  MsgDispatchSystemOutErr(String msgFiles){
    cmdlineOut = new LogMessageStream(System.out);
    cmdlineErr = new LogMessageStream(System.err);
    MsgDispatcher msgDispatcher = new MsgDispatcher(1000, 100, 3, 0);
    this.msgDispatcher = msgDispatcher;
    
    printOut = new MsgPrintStream(msgDispatcher, 10000, 5000, 20);
    printErr = new MsgPrintStream(msgDispatcher, 50000, 5000, 20);
    int secondsToClose = -10; //any 10 seconds, the file will be closed, and re-openened at least after 10 seconds.
    int hoursPerFile = -3600; //This is 3600 seconds.
    try{ FileSystem.mkDirPath("D:/DATA/msg/");}
    catch(java.io.IOException exc){ System.out.println("can't create D:/DATA/msg/"); }
    msgDispatcher.setOutputRoutine(ixMsgOutputStdOut, "stdout", false, cmdlineOut);
    msgDispatcher.setOutputRoutine(ixMsgOutputStdErr, "stderr", false, cmdlineErr);
    msgDispatcher.setOutputRange(0, 49999, 1<<ixMsgOutputStdOut, MsgDispatcher.mSet, 0);
    msgDispatcher.setOutputRange(50000, Integer.MAX_VALUE, 1<<ixMsgOutputStdErr, MsgDispatcher.mSet, 0);
    if(msgFiles !=null){
      fileOutput = new LogMessageFile(msgFiles, secondsToClose, hoursPerFile, null
        , timeZoneForFile, msgDispatcher.getSharedFreeEntries());
    
      msgDispatcher.setOutputRoutine(ixMsgOutputFile, "File", false, fileOutput);
    }
    System.setOut(printOut.getPrintStreamLog());
    System.setErr(printErr.getPrintStreamLog());
  }
  
  
  public static MsgDispatchSystemOutErr create(String msgFiles){
    singleton = new MsgDispatchSystemOutErr(msgFiles);
    return singleton;
  }
}
