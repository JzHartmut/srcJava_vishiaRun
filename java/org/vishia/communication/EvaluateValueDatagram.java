package org.vishia.communication;

import java.util.Map;
import java.util.TreeMap;

import org.vishia.byteData.ByteDataSymbolicAccess;
import org.vishia.byteData.VariableAccess_ifc;

/**This class helps to evaluate values in a datagram (usual float)
 * with a time stamp, usable for {@link org.vishia.guiInspc.InspcCurveViewApp}
 * See also OamShowValues
 * @author Hartmut Schorrig
 * @since 2024-02-27
 *
 */
public class EvaluateValueDatagram {

  
  /**This index (map) contains all variables, both from {@link #accessOamVariable} as well as content of {@link #evalValues}. */
  final Map<String, VariableAccess_ifc> idxAllVariables = new TreeMap<String, VariableAccess_ifc>();

  
  /**Index (fast access) of all variable which are sent from the automation device (contained in the cfg-file). */
  protected final ByteDataSymbolicAccess accessOamVariable;

  /**The access to received data for the timestamp as milliseconds after a base year.
   * It is not null if that variable is contained in the received data description
   * See {@link #readVariableCfg()}.
   */
  VariableAccess_ifc varTimeMilliSecFromBaseyear;
  
  protected int timeShort;
  
  protected int clearDataCt = 0;
  
  long timeMilliSecFromBaseyear;

  
  boolean dataValid;
  
  public EvaluateValueDatagram ( ) {
    this.accessOamVariable = new ByteDataSymbolicAccess(this.idxAllVariables); //access to variables in a byte[]
  }

  
  public float[] readFloatFromDatagram (byte[] binData, int nrofBytes, int from) {
    this.accessOamVariable.assignData(binData, nrofBytes, from, System.currentTimeMillis());
    this.accessOamVariable.dataAccess.setBigEndian(true);
    this.dataValid = true;
    //long timeAbs = this.accessOamVariable.dataAccess.getLongVal(0x0, 8);
    int timeShortAdd = 0; //this.accessOamVariable.dataAccess.getIntVal(0x8, 4);
    int timeShort = this.accessOamVariable.dataAccess.getIntVal(0x4, 4);
    int timeDiff = timeShort - this.timeShort;
    this.timeShort = timeShort;
    if(timeDiff <0) {
      this.clearDataCt +=1;                                // two times back timeShort is clear Data
      return null;
    } else {
      this.clearDataCt = 0;
    }
    this.accessOamVariable.setTimeShort(timeShort, timeShortAdd);
    //this.timeMilliSecFromBaseyear = timeAbs + (timeShort - timeShortAdd);
    if(this.varTimeMilliSecFromBaseyear !=null){
      //read the time stamp from the record:
      this.timeMilliSecFromBaseyear = this.varTimeMilliSecFromBaseyear.getInt();
    } else {
      //this.timeMilliSecFromBaseyear = System.currentTimeMillis();
    }
    int zFloat = (nrofBytes -8 )/4;
    float[] ret = new float[zFloat];
    for(int ix = 0; ix < zFloat; ++ix) {
      ret[ix] = this.accessOamVariable.dataAccess.getFloatVal(4*ix+8);
    }
    
    return ret;
  }

  
  public int getTimeShort () { return this.timeShort; }
  
  public boolean clearData () { return this.clearDataCt >=2; }
  
}
