package org.vishia.curves;

import java.io.File;
import java.io.IOException;

public interface WriteCurve_ifc {
  
  void setFile(File file);
  
  void setTrackInfo(int nrofTracks, int ixTrack, String sPath, String sName);
  
  void writeCurveStart(int timeshort) throws IOException ;
  
  void writeCurveTimestamp(int timeshort, long timeAbs, float millisec7short);
  
  void writeCurveRecord(int timeshort, float[] values) throws IOException ;
  
  void writeCurveFinish() throws IOException ;
  
  void writeCurveError(String msg) throws IOException ;
  
}
