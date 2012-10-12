package org.vishia.curves;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**This class writes values from curves in a binary file with head.
 * @author Hartmut Schorrig
 *
 */
public class WriteCurveBinary implements WriteCurve_ifc{

  
  File fOut;
  
  OutputStream out;

  @Override public void setTrackInfo(int nrofTracks, int ixTrack, String sPath, String sName){
    
  }
  
  public WriteCurveBinary() {
  }
  
  
  public void setFile(File fOut){
    this.fOut = fOut;
  }
  
  @Override
  public void writeCurveError(String msg) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeCurveFinish() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeCurveRecord(int timeshort, float[] values) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeCurveStart(int timeshort) throws IOException {
    // TODO Auto-generated method stub
    if(out !=null){
      out.close();
    }
    out = new FileOutputStream(fOut);
    
  }

  @Override
  public void writeCurveTimestamp(int timeshort, long timeAbs,
      float millisec7short) {
    // TODO Auto-generated method stub
    
  }
  
  
  
  
}
