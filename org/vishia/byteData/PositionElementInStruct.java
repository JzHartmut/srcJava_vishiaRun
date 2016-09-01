package org.vishia.byteData;

/**This class helps to count positions in structures for Boolean bits, bytes. 
 * It is able to use for conversion of structure descriptions from C-Headers and from SCL (S7-Programming, Simatic).
 * */
public class PositionElementInStruct
{
	/**Positions of elements within the Variable of the FB, DB or UDT.
	 * There are set to the start position if the {@link #generateOneSclFile(String, VariablenBlock)}-
	 * routine is started. Then there are incremented depending of the parse result of the variables
	 * in the SCL-File, see {@link #incrPosInOriginalDb(ZbnfVariable, TypeConversion)}.
	 */
	private int ixByte, ixBit;
  
	
	public int getIxByte(){ return ixByte; }
	
	public int getIxBit(){ return ixBit; }
	
	public int getMaskBit(){ return 1 << ixBit; }
	
  public void setStartPos(int pos)
  { ixByte = pos;
    ixBit = 0;
  }
	
  
  /**Adjusts the position if the next element has a size, which requests a new align.
   * @param sizeForAlign The size of the next element. 
   */
  public void adjustNonBool(int sizeForAlign)
  {
  	if(sizeForAlign >0 && ixBit > 0){
			//increment now boolean address, because it follows a non-boolean type.
			this.ixByte +=1;
			this.ixBit = 0;
		}
		if(sizeForAlign >=2){
			if((this.ixByte & 1) == 1){
				this.ixByte +=1;    //2- and 4-Byte-Parameter uses odd addresses.
			}
		}
	      	
  }
  
  
  //type.nrofBytes
  
  public void incrPosInOriginalDb(int arrayStartIx, int arrayEndIx, int nrofBytesElement)
  {
  	final int nrofElement;
		if(arrayStartIx != 0 || arrayEndIx != 0){
			nrofElement = arrayEndIx - arrayStartIx +1;
			assert(nrofElement >0);
		} else {
			nrofElement = 1;
		}
  	if(nrofBytesElement >0){  //not a boolean
      ixByte += nrofBytesElement * nrofElement;
    } else { //a boolean.
    	ixBit += nrofElement;
    	while(ixBit >=8 && ixByte < 999999){
    		ixByte +=1; 
    		ixBit -= 8;
    	}
    	assert(ixByte < 999999); //not catastrophic while abort.
    }
  }
  

}
