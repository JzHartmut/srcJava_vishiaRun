package org.vishia.inspcPC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.vishia.cmd.JZtxtcmdExecuter;
import org.vishia.cmd.JZtxtcmdScript;
import org.vishia.communication.InterProcessCommFactorySocket;
import org.vishia.inspcPC.mng.InspcMng;
import org.vishia.jztxtcmd.JZtxtcmd;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.ReplaceAlias_ifc;

/**
 * This class contains a main-routine to execute inspector commands in a command
 * line. The inspector command is read from a textual file.
 * 
 * @author Hartmut Schorrig
 *
 */
public class InspCmd
{

  /**Version, history and license.
   * <ul>
   * <li>2017-07-02 Hartmut new: gardening, second usage: invokes "execute" instead main with inspc as argument.
   *   It is better able to document. 
   * <li>2016 Hartmut: created
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  public final static String version = "2017-07-02"; 

  
  
  
  
  /** Aggregation to the Console implementation class. */
  final MainCmd_ifc console;

  public static void main(String[] sArgs)
  {
    try { // for unexpected exceptions
      int exitlevel = smain(sArgs);
      System.exit(exitlevel);
    } catch (Exception exc) {
      // catch the last level of error. No error is reported direct on command
      // line!
      System.err.println("InspCmd - script exception; " + exc.getMessage());
      exc.printStackTrace(System.err);
      System.exit(MainCmdLogging_ifc.exitWithErrors);
    }
  }

  /**
   * Invocation from another java program without exit the JVM
   * 
   * @param sArgs
   *          same like {@link #main(String[])}
   * @return the exit level 0 - successful 1..6 see
   *         {@link MainCmdLogging_ifc#exitWithArgumentError} etc.
   * @throws IllegalAccessException
   */
  public static int smain(String[] sArgs) throws ScriptException, IllegalAccessException
  {
    String sRet = null;
    Args args = new Args();
    CmdLine mainCmdLine = new CmdLine(args, sArgs); // the instance to parse
                                                    // arguments and others.
    mainCmdLine.setReportLevel(0); // over-write if argument given. Don't use a
                                   // report.txt by default.
    try {
      mainCmdLine.parseArguments();
    } catch (Exception exception) {
      sRet = "InspcCmd - Argument error ;" + exception.getMessage();
      mainCmdLine.report(sRet, exception);
      mainCmdLine.setExitErrorLevel(MainCmdLogging_ifc.exitWithArgumentError);
    }
    if (args.sFileScript == null) {
      mainCmdLine.writeHelpInfo(null);
    } else {
      if (sRet == null) {
        InspCmd main = new InspCmd(mainCmdLine);
        Writer out = null;
        File fOut = args.sFileTextOut == null ? null : new File(args.sFileTextOut);
        try {
          out = args.sFileTextOut == null ? null : new FileWriter(fOut);
          main.execute(args, out);
        } catch (IOException e) {
          throw new ScriptException(e);
        }
        if (out != null) {
          try {
            out.close();
          } catch (IOException exc) {
            throw new RuntimeException(exc);
          }
        }

        main.console.writeInfoln("SUCCESS");

      }
    }
    return mainCmdLine.getExitErrorLevel();
  }

  public static class Args
  {

    /**
     * path to the script file for the generation or execution script of JZcmd.
     */
    public String sFileScript;

    /**
     * path to the text output file which is generated by JZcmd. May be null,
     * then no text output.
     */
    public String sFileTextOut;

    public String sScriptCheck;

    public File fileTestXml;

    public int cycletime = 100;

  }

  /**
   * The organization class for command line invocation.
   */
  private static class CmdLine extends MainCmd
  {

    public final Args argData;

    protected final MainCmd.Argument[] argList = {
        new MainCmd.Argument("", ":pathTo/input.script", new MainCmd.SetArgument()
        {
          @Override
          public boolean setArgument(String val)
          {
            argData.sFileScript = val;
            return true;
          }
        }), new MainCmd.Argument("-t", ":OUTEXT pathTo text-File for output", new MainCmd.SetArgument()
        {
          @Override
          public boolean setArgument(String val)
          {
            argData.sFileTextOut = val;
            return true;
          }
        }) };

    protected CmdLine(Args argData, String[] sCmdlineArgs)
    {
      super(sCmdlineArgs);
      this.argData = argData;
      super.addAboutInfo("Execution of Inspspector-Commands from a file");
      super.addAboutInfo("made by HSchorrig, Version 1.0, 2016-01-06");
      super.addHelpInfo("args SCRIPTFILE [-t:OUTEXT]");
      super.addArgument(argList);
      super.addHelpInfo("==Standard arguments of MainCmd==");
      super.addStandardHelpInfo();
    }

    @Override
    protected void callWithoutArguments()
    { // overwrite with empty method - it is admissible.
    }

    @Override
    protected boolean checkArguments()
    {
      if (argData.sFileScript == null)
        return false;
      else
        return true;
    }

  }

  ReplaceAlias_ifc replAlias = new InspcReplAlias();

  public InspCmd(MainCmd_ifc console)
  {
    super();
    this.console = console;
  }

  void execute(Args args, Writer out)
      throws IOException, ScriptException, IllegalAccessException, IllegalArgumentException
  {

    // String sRet = null;
    // File fScriptCheck = args.sScriptCheck == null ? null : new
    // File(args.sScriptCheck);

    JZtxtcmdScript script = JZtxtcmd.translateAndSetGenCtrl(new File(args.sFileScript), console);

    JZtxtcmdExecuter executer = new JZtxtcmdExecuter(console);
    executer.initialize(script, false, null);
    // create variables as argument for exexSub:
    Map<String, DataAccess.Variable<Object>> vars; // = new
                                                   // IndexMultiTable<String,
                                                   // DataAccess.Variable<Object>>(IndexMultiTable.providerString);
    // define String ownIp
    // DataAccess.createOrReplaceVariable(vars, "ownIp", 'S', null, false);
    // define Map targets
    Map<String, DataAccess.Variable<Object>> idx1TargetIpcAddr; // = new
                                                                // IndexMultiTable<String,
                                                                // DataAccess.Variable<Object>>(IndexMultiTable.providerString);
    // DataAccess.createOrReplaceVariable(vars, "targets", 'M',
    // idx1TargetIpcAddr, false);
    //
    // invoke the sub routine args
    //
    JZtxtcmdScript.JZcmdClass class1 = script.getClass("Args");
    if (class1 == null)
      throw new IllegalArgumentException("Script class \"Args\" not found in \"" + args.sFileScript + "\"");
    JZtxtcmdExecuter.ExecuteLevel level = executer.execute_Scriptclass(class1);
    vars = level.localVariables;
    String sOwnIpcAddr = vars.get("ownIp").value().toString();
    idx1TargetIpcAddr = (Map<String, DataAccess.Variable<Object>>) vars.get("targets").value();
    Map<String, String> idxTargetIpcAddr = new IndexMultiTable<String, String>(IndexMultiTable.providerString);

    for (Map.Entry<String, DataAccess.Variable<Object>> entry : idx1TargetIpcAddr.entrySet()) {
      idxTargetIpcAddr.put(entry.getKey(), entry.getValue().value().toString());
    }
    // replAlias.addDataReplace(idxTargetIpcAddr);
    new InterProcessCommFactorySocket();
    InspcMng inspcMng = new InspcMng(sOwnIpcAddr, idxTargetIpcAddr, args.cycletime, false, null);
    inspcMng.complete_ReplaceAlias_ifc(replAlias);
    inspcMng.startupThreads();
    // create devices:

    // executer.initialize(script, false, null);
    // try{
    // executer.setScriptVariable("inspc", 'O', inspcMng, true);
    // } //catch(IllegalAccessException exc) {
    // TODO
    // }
    List<DataAccess.Variable<Object>> var = new LinkedList<DataAccess.Variable<Object>>();
    var.add(new DataAccess.Variable<Object>('O', "inspc", inspcMng));
    // executer.executeScriptLevel(script, null);
    executer.execSub(script, "execute", var, false, out, null); // (null, false,
                                                                // true, out,
                                                                // var, null);
    // JZcmd.execute(executer, fileScript, out, console.currdir(), true,
    // fScriptCheck, console);
    inspcMng.close();

  }

}
