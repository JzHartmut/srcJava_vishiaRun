package org.vishia.curves;

import java.io.File;
import java.io.IOException;

import org.vishia.util.Timeshort;

/**This interface can be used for writing curves into files from any application. Implementors are 'exporter' for that data.
 * This interface is used in {@link org.vishia.gral.base.GralCurveView} but it is useful for other applications too.
 * 
 * It is possible to load a implementor of that interface by string (using Class.forName(name).newInstance()).
 * It means that this interface should supply all required access methods.
 * @author Hartmut Schorrig
 *
 */
public interface WriteCurve_ifc {
  
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


  /**Sets the output file. The file may be opened in this routine or opened in {@link #writeCurveStart(int)}.
   * This routine should be the first one to call. An currently usage of the same instance is closed
   * with this call. 
   * 
   * @param file The file for output.
   */
  void setFile(File file);
  
  /**Sets information for one track. This routine should be called more as one time one for each track.
   * It should be called firstly after {@link #setFile(File)}.
   * 
   * @param nrofTracks
   * @param ixTrack
   * @param sPath
   * @param sName
   */
  void setTrackInfo(int nrofTracks, int ixTrack, String sPath, String sName, String sColor, float scale7div, float mid, float line0);
  
  void writeCurveTimestamp(Timeshort timeshortabs);
  
  /**Opens the file and write head information. The {@link #setTrackInfo(int, int, String, String)}
   * should be called already for all tracks. It means, all information which may need in the head are given
   * on calling this method.
   * 
   * @param timeshort
   * @throws IOException
   */
  void writeCurveStart(int timeshort) throws IOException ;
  
  void writeCurveRecord(int timeshort, float[] values) throws IOException ;
  
  void writeCurveFinish() throws IOException ;
  
  void writeCurveError(String msg) throws IOException ;
  
}
