package org.vishia.curves;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WriteCurveCsv  implements WriteCurve_ifc{

  
  File fOut;
  
  Writer out;

  int absTimeshort;
  
  long absTime;
  
  float millisec7timeshort;
  
  SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
  
  StringBuilder uLine = new StringBuilder(5000);
  
  @Override public void setTrackInfo(int nrofTracks, int ixTrack, String sPath, String sName){
    
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
    long time = (long)((timeshort - this.absTimeshort) * millisec7timeshort) + absTime;
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
    
  }

  @Override public void writeCurveTimestamp(int timeshort, long timeAbs, float millisec7short) {
    this.absTimeshort = timeshort;
    this.absTime = timeAbs;
    this.millisec7timeshort = millisec7short;
    
  }
  
  
  
  

  
}
