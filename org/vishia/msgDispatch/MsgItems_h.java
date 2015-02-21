
package org.vishia.msgDispatch ;

import org.vishia.byteData.*;


public class MsgItems_h
{

  /**version, history and license:
   * <ul>
   * <li>
   * <li>2010-08-00 Hartmut created 
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
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
   * 
   * 
   */
  public final static int version = 20120822;



  public static class MsgItem extends ByteDataAccessBase
  {
    
    protected static final int kSizevalues = 4;
    

    /**Index of the data element*/
    public static final int
      kIdxtimestamp = 0
      , kIdxtimeMillisec = 0 + 4
      , kIdxmode_typeVal = 0 + 4 + 2
      , kIdxident = 0 + 4 + 2 + 2
      , kIdxvalues = 0 + 4 + 2 + 2 + 4
      , kIdxAfterLast = 0 + 4 + 2 + 2 + 4 + 4 * 4;
    ; /*xsl: all Data from struct in headerfile converted to position indices */

    
    
    

    
      
      
      
    /** Constructs the data management class*/
    public MsgItem()
    { super(kIdxAfterLast);
    }

    /** Constructs as a child inside another ByteDataAccess*/
    public MsgItem(ByteDataAccessBase parent, int idxChildInParent)
    { super(kIdxAfterLast);
      try{ assignAt(idxChildInParent, parent); }
      catch(IllegalArgumentException exc)
      { //it won't be have any exception because specifyLengthElement() inside this class is the only source for it.
      }
    }
    

    //@Override
    protected void specifyEmptyDefaultData()
    {
      for(int ii=ixBegin; ii < ixEnd; ii++)
      { super.data[ii] = 0;
      }
    }


        
	    public void set_timestamp(int val)
	    { //type of struct-attribut is int32
	      setInt32(kIdxtimestamp, val);
	    }
	      
      public int get_timestamp()
      { //type of struct-attribut is int32
        return getInt32(kIdxtimestamp);
      }
        
	    public void set_timeMillisec(short val)
	    { //type of struct-attribut is int16
	      setInt16(kIdxtimeMillisec, val);
	    }
	      
      public short get_timeMillisec()
      { //type of struct-attribut is int16
        return getInt16(kIdxtimeMillisec);
      }
        
	    public void set_mode_typeVal(short val)
	    { //type of struct-attribut is int16
	      setInt16(kIdxmode_typeVal, val);
	    }
	      
      public short get_mode_typeVal()
      { //type of struct-attribut is int16
        return getInt16(kIdxmode_typeVal);
      }
        
	    public void set_ident(int val)
	    { //type of struct-attribut is int32
	      setInt32(kIdxident, val);
	    }
	      
      public int get_ident()
      { //type of struct-attribut is int32
        return getInt32(kIdxident);
      }
        
	    public void set_values(int val, int idx)
	    { //type of struct-attribut is int32
	      setInt32(kIdxvalues, idx, 4, val);
	    }
	      
      public int get_values(int idx)
      { //type of struct-attribut is int32
        return getInt32(kIdxvalues, idx, 4);
      }
        
      public final static int size_values = 4;
          
      
      
  }




  public static class MsgItems extends ByteDataAccessBase
  {
    
    protected static final int kSizemsgItems = 20;
    

    /**Index of the data element*/
    public static final int
      kIdxfileHead = 32
      , kIdxnrofMsg = 32 
      , kIdxdummy = 32 + 2
      , kIdxmsgItems = 32 + 2 + 2
      , kIdxAfterLast =32 + 2 + 2 + MsgItem.kIdxAfterLast * 20;
    ; /*xsl: all Data from struct in headerfile converted to position indices */

    
    
    

    
      
      
      
    /** Constructs the data management class*/
    public MsgItems()
    { super(kIdxAfterLast);
    }

    /** Constructs as a child inside another ByteDataAccess*/
    public MsgItems(ByteDataAccessBase parent, int idxChildInParent)
    { super(kIdxAfterLast);
      try{ assignAt(idxChildInParent, parent); }
      catch(IllegalArgumentException exc)
      { //it won't be have any exception because specifyLengthElement() inside this class is the only source for it.
      }
    }
    



 
    //@Override
    protected void specifyEmptyDefaultData()
    {
      for(int ii=ixBegin; ii < ixEnd; ii++)
      { super.data[ii] = 0;
      }
    }

        
        
	      //note: method to set fileHead, not able to generate.
	        
      //note: method to set fileHead, not able to generate.
        
	    public void set_nrofMsg(short val)
	    { //type of struct-attribut is int16
	      setInt16(kIdxnrofMsg, val);
	    }
	      
      public short get_nrofMsg()
      { //type of struct-attribut is int16
        return getInt16(kIdxnrofMsg);
      }
        
	    public void set_dummy(short val)
	    { //type of struct-attribut is int16
	      setInt16(kIdxdummy, val);
	    }
	      
      public short get_dummy()
      { //type of struct-attribut is int16
        return getInt16(kIdxdummy);
      }
        
    public final MsgItem msgItems = new MsgItem(); //(this, kIdxmsgItems);  //embedded structure
        
	      //note: method to set msgItems, not able to generate.
	        
      //note: method to set msgItems, not able to generate.
        
      /**Because the method has fix childs, the assignDataToFixChilds method is overridden to apply to all fix childs. */
      protected void assignDataToFixChildren() throws IllegalArgumentException
      {
      
        //NOTE: use super.data etc to prevent false using of a local element data. super is ByteDataAccess.    
        int length = super.ixEnd - kIdxmsgItems;
        msgItems.assign(super.data, length, super.ixBegin + kIdxmsgItems);  //embedded structure
        msgItems.setBigEndian(super.bBigEndian);
      }

      /**Because the method has fix childs, the setBigEndian method is overridden to apply the endian to all fix childs. */
      public void setBigEndianItem(boolean val)
      { super.setBigEndian(val);
      
          
        msgItems.setBigEndian(val);  //embedded structure
          
      }
      
  }


}
