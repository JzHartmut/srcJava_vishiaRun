package org.vishia.curves;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.vishia.util.Timeshort;

public class WriteCurveCsv  implements WriteCurve_ifc{

  /**Version, history and copyright/copyleft.
   * <ul>
   * <li>2012-10-12 created. Firstly used for {@link org.vishia.guiInspc.InspcCurveView} and {@link org.vishia.gral.base.GralCurveView}.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:<br>
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
   *    but doesn't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you intent to use this source without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  public static final int version = 20121012;


  File fOut;
  
  Writer out;

  Timeshort timeshortabs;
  
  String[] sColumn;
  
  SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
  
  StringBuilder uLine = new StringBuilder(5000);
  
  @Override public void setTrackInfo(int nrofTracks, int ixTrack, String sPath, String sName){
    if(sColumn == null){
      sColumn = new String[nrofTracks];
    }
    sColumn[ixTrack] = sPath;
  }
  
  public WriteCurveCsv() {
  }
  
  
  @Override public void setFile(File fOut){
    this.fOut = fOut;
  }
  
  @Override public void writeCurveError(String msg) throws IOException {
    if(out !=null){
      out.close();
    }
    System.err.println("WriteCurveCsv error -" + msg + ";");
  }

  @Override public void writeCurveFinish() throws IOException {
    out.close();
  }

  @Override public void writeCurveRecord(int timeshort, float[] values) throws IOException {
    long time = timeshortabs.absTimeshort(timeshort);
    Date date = new Date(time);
    uLine.setLength(0);
    String sDate = dateFormat.format(date);
    uLine.append(sDate).append(";  ");
    for(int ix = 0; ix < values.length; ++ix){
      uLine.append(values[ix]).append(";  ");
    }
    uLine.append("\r\n");
    out.append(uLine);
  }

  @Override public void writeCurveStart(int timeshort) throws IOException {
    if(out !=null){
      out.close();
    }
    out = new FileWriter(fOut);
    uLine.setLength(0);
    uLine.append("time; ");
    for(String sColumn1: sColumn){
      if(sColumn1 == null){
        sColumn1 = "?";
      }
      uLine.append(sColumn1).append(";  ");
    }
    out.append(uLine);
  }

  @Override public void writeCurveTimestamp(Timeshort timeshortabs) {
    this.timeshortabs = timeshortabs;
    
  }
  
  
  
  

  
}
