package org.vishia.msgDispatch;

import java.util.TimeZone;

import org.vishia.util.FileSystem;

public class MsgDispatchSystemOutErr {

  private static final int ixMsgOutputFile = 1;
  
  private static final int ixMsgOutputGuiList = 0;
  

  final MsgPrintStream printOutput;
  
  public final MsgDispatcher msgDispatcher;

  final LogMessageFile fileOutput;
  
  TimeZone timeZoneForFile = TimeZone.getTimeZone("GMT");
  
  LogMessageStream cmdlineOut, cmdlineErr;
  
  static MsgDispatchSystemOutErr singleton;
  
  MsgDispatchSystemOutErr(){
    cmdlineOut = new LogMessageStream(System.out);
    cmdlineErr = new LogMessageStream(System.err);
    MsgDispatcher msgDispatcher = new MsgDispatcher(1000, 100, 3, 0);
    this.msgDispatcher = msgDispatcher;
    
    printOutput = new MsgPrintStream(msgDispatcher);
    int secondsToClose = -10; //any 10 seconds, the file will be closed, and re-openened at least after 10 seconds.
    int hoursPerFile = -3600; //This is 3600 seconds.
    try{ FileSystem.mkDirPath("D:/DATA/msg/");}
    catch(java.io.IOException exc){ System.out.println("can't create D:/DATA/msg/"); }
    fileOutput = new LogMessageFile("D:/DATA/msg/log$yyyy-MMM-dd-HH_mm$.log", secondsToClose, hoursPerFile, null
        , timeZoneForFile, msgDispatcher.getSharedFreeEntries());
    msgDispatcher.setOutputRoutine(ixMsgOutputFile, "File", false, fileOutput);
    msgDispatcher.setOutputRoutine(ixMsgOutputGuiList, "GUI-List", false, cmdlineOut);
    System.setOut(printOutput.getPrintStreamLog());
    System.setErr(printOutput.getPrintStreamLog());
  }
  
  
  public static MsgDispatchSystemOutErr create(){
    singleton = new MsgDispatchSystemOutErr();
    return singleton;
  }
}
