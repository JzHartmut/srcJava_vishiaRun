/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.communication;

import java.util.Arrays;

import org.vishia.byteData.ByteDataAccess;

/**This class supports preparing data for the Inspector-datagram-definition.
 * @author Hartmut Schorrig
 *
 */
public class InspcDataExchangeAccess
{

	
	
	/**Preparing the header of a datagram.
	 * 
	 */
	public final static class Datagram extends ByteDataAccess
	{
		public static final int knrofBytes = 0;
		public static final int knEntrant = 2;  //2
		public static final int kencryption = 4; //4
		public static final int kseqnr = 8;   //8
		public static final int kanswerNr =12;      //12
		public static final int sizeofHead = 16;

		public Datagram(byte[] buffer)
		{ this();
			assignEmpty(buffer);
			setBigEndian(true);
		}
		
		public Datagram()
		{ super();
			setBigEndian(true);
		}
		
		@Override protected final void specifyEmptyDefaultData()
		{
			Arrays.fill(data, 0, sizeofHead, (byte)0);
		}

		@Override protected final int specifyLengthElement() throws IllegalArgumentException
		{ return 0;
		}

		@Override public final int specifyLengthElementHead()
		{ return sizeofHead;
		}
		
		public final void setLengthDatagram(int length){ setInt16(0, length); }
		
		public final int getLengthDatagram(){ return getInt16(0); }
		
		public final void setHead(int entrant, int seqNr, int encryption){
			setInt16(knrofBytes, sizeofHead);
			setInt16(knEntrant, entrant);
			setInt32(kseqnr,seqNr);
			setInt16(kanswerNr, 0x1);
			//int encryption = (int)(((0x10000 * Math.random())-0x8000) * 0x10000);
			setInt32(kencryption, encryption);
		}
		
		public final void setEntrant(int nr){ setInt16(knEntrant, nr); }
		
		public final int getEntrant(){ return getInt16(knEntrant); }
		
		public final int getEncryption(){ return getInt32(kencryption); }
		
    public final void setSeqnr(int nr){ setInt32(kseqnr, nr); }
		
		public final int getSeqnr(){ return getInt32(kseqnr); }
		
		/**Mark the datagram as last answer. */
		public final void markAnswerNrLast()
		{ int nr = getInt8(kanswerNr);
		  nr |= 0x80;
		  setInt8(kanswerNr, nr);
		}
		
		/**Increments the number for the answer datagram. */
		public final void incrAnswerNr()
		{ int nr = getInt8(kanswerNr);
	  	nr = (nr & 0x7f) +1;
	  	assert((nr & 0x80) ==0);
	  	setInt8(kanswerNr, nr);
		}
	}
	
	
	
	
	/**This is the header of an information entry.
	 * An information entry contains this header and may be some childs. 
	 * The childs may be simple integer or String childs getting and setting
	 * with the methodes to add
	 * {@link ByteDataAccess#addChildInteger(int, long)} or {@link ByteDataAccess#addChildString(String)}. 
	 * and the methods to get
	 * {@link ByteDataAccess#getChildInteger(int)} or {@link ByteDataAccess#getChildString(int)}.
	 * The childs may be described by a named-here class, forex {@link SetValue}
	 * <br><br>
	 * The structure of an information entry may be described with XML, where the XML is only
	 * a medium to show the structures, for example:
	 * <pre>
	 * <Info bytes="16" order="345"><StringValue length="7">Example</StringValue></Info>   
	 * </pre>
	 * In this case 8 Bytes are added after the head. The length stored in the head is 16. 
	 * The <StringValue...> consists of a length byte, following by ASCII-character.
	 */
	public static class Info extends ByteDataAccess
	{

		public final static int sizeofHead = 8;

		/** Aufforderung zur Rückgabe einer Liste aller Attribute und Assoziationen des adressierten Objektes.
    
		    Im Cmd wird der PATH des Objektes übergeben. Das geschieht in einer Struktur DataExchangeString_OBM.
		    
		    ,  Cmd:
		    ,  +------head-----------+-int32-+---------string------------------+
		    ,  |kGetFields           | ix    | PATH mit Punkt am Ende             |
		    ,  +---------------------+-------+-----------------------------+
		    
		    Dabei wird mit dem ,,head.index,, ein Startindex übergeben. Dieser soll bei der ersten Abfrage =0 sein.
		    
		    Das positive Antworttelegramm enthält eine Liste von Items mit einzelnen Attributen und Assoziationen:
		    
		    ,  Answer:
		    ,  +------head-----------+-----string--+-------head----------+-----string--
		    ,  |kAnswerFieldMethod   | Name:Typ    |kAnswerFieldMethod   | Name:Typ ...
		    ,  +---------------------+-------------+---------------------+-------------
		
		    Der Aufbau des Strings ist bei ,,kAnswerFieldMethod,, beschrieben.
		    
		    Wenn nicht alle Attribute und Assoziationen in ein Antworttelegramm passen, dann wird als letztes Item gesendet:
		
		    ,  Answer:
		    ,  ----+------head--------------------+---data--+
		    ,      |kAnswerFurtherFieldsMethods   |  ix
		    ,  ----+------------------------------+---------=
		
		    Der Index ist hierbei derjenige Index, der in einer weiteren Anforderung mit kGetFields 
		    als ,,head.index,, angegeben werden soll. Damit werden ab diesem Index weitere Attribute und Methoden abgefragt.
		 */
		public final static int kGetFields = 0x10;
		
		/**@deprecated */
		public final static int kGetFieldsFurther = 0x12; 

		
		/**Antwort auf Aufforderung zur Rückgabe einer Liste von Attributen, Assoziationen oder Methoden.
    Das Antwort-Item enthält einen Eintrag für ein Element, Type DataExchangeString_OBM.
    Die Antwort auf kGetFields oder kGetMethods besteht aus mehreren Items je nach der Anzahl der vorhandenen Elemente.
    Gegebenenfalls ist die Antwort auch auf mehrere Telegramme verteilt.

    Die Zeichenkette für ein Item aus zwei Teilen, Typ und Name, getrennt mit einem Zeichen ':'.
    Der angegebenen Typ entspricht dem Typ der Assoziationsreferenz, nicht dem Typ des tatsächlich assoziierten Objektes,
    wenn es sich nicht um einen einfachen Typ handelt.

    Wenn eine Methode übergeben wird, dann werden die Aufrufargument-Typen wie eine formale Argumentliste in C angegeben.
    Beispiel:
    , returnType:methodName(arg1Typ,arg2Typ)

    Der Index im Head der Antwort zählt die übergebenen Informationen.
  */

		public final static int kAnswerFieldMethod = 0x14;

		public final static int kRegisterRepeat = 0x23;
		
		public final static int kAnswerRegisterRepeat = 0x123;
		
		public final static int kFailedRegisterRepeat = 0x124;
		
		public final static int kGetValueByIndex = 0x25;
		
		public final static int kAnswerValue = 0x26;
		
		public final static int kFailedValue = 0x27;
		
		public final static int kGetValueByPath = 0x30;
		
		public final static int kGetAddressByPath = 0x32;
		
		public final static int kSetValueByPath = 0x35;
		
		
		
		
		public final static int kFailedCommand = 0xFF;

		public static final int kReference = 0xdf;
		
		@Override protected final void specifyEmptyDefaultData()
		{
		}

		@Override protected final int specifyLengthElement() throws IllegalArgumentException
		{ return 0;
		}

		@Override public final int specifyLengthElementHead()
		{ return sizeofHead;
		}
		
		public final void setInfoHead(int length, int cmd, int order)
		{ setInt16(0, length); 
		  setInt16(2, cmd); 
		  setInt32(4, order); 
		}
		
		public final void setLength(int length)
		{ setInt16(0, length); 
		}
		
		public final void setCmd(int cmd)
		{ setInt16(2, cmd); 
		}
		
		public final int getCmd(){ return getInt16(2); }
		
		public final int getLenInfo(){ return getInt16(0); }
		
		public final int getOrder(){ return getInt32(4); }
		
			
	}
	
	
	//public interface ValueTypes
	//{
		/**Values between 0..199 determines the length of string.
		 * A String item contains maximal 200 Bytes. */
		public static final int maxNrOfChars = 0xc8;

		/**A reference is the memory-address of an element in C-language
		 * or a significant numeric Identifier of an object (instance) in Java.
		 */
		public static final int kReferenceAddr = 0xdf;
		
		/**Scalar types started with 0xe0,
		 * see {@link org.vishia.byteData.}
		 * 
		 */
		public static final int kScalarTypes = 0xe0;
		
		
	//}
	
/**Datagram from accessor to Target to set a value.
 * It is a derived Info. (?)		
 */
public final static class SetValue extends ByteDataAccess{
	
	public final static int sizeofElement = 16;

	public SetValue(){
		setBigEndian(true);
	}
	
	/**Gets a password for access control.
	 * @return The first password.
	 */
	public int getPwdLo(){ return (int)_getLong(0, -4); }
	
	/**Gets a password for access control.
	 * @return The second password.
	 */
	public int getPwdHi(){ return (int)_getLong(4, -4); }
	
	public void setPwdLo(int pwd){ _setLong(0, -4, pwd); }
	
	public void setPwdHi(int pwd){ _setLong(4, -4, pwd); }

	public byte getByte(){ return (byte)_getLong(15, -1);} 
	
	public short getShort(){ return (short)_getLong(14, -2);} 
	
	/**A long value is provided in the bytes 8..15 in Big endian.
	 * If only a int value will be used, it were found in the bit 12..15.
	 * @return The int value.
	 */
	public int getInt(){ return (int)_getLong(12, -4); }
	
	/**A long value is provided in the bytes 8..15 in Big endian.
	 * @return The long value.
	 */
	public long getLong(){ return _getLong(8, -8); }
	
	/**A float value is provided in the bytes 8..11 in Big endian.
	 * @return The float value.
	 */
	public float getFloat(){ return getFloat(8); }
	
	public double getDouble(){ return getDouble(8); }

  /**Sets a byte value. */
  public void setByte(byte value){ _setLong(15, 1, value);} 
  
  /**Sets a byte value. */
  public void setShort(short value){ _setLong(14, 2, value);} 
  
  /**Sets a byte value. */
  public void setInt(int value){ _setLong(12, 4, value);} 
  
  /**Sets a byte value. */
  public void setLong(long value){ _setLong(8, 8, value);} 
  
  /**Sets a byte value. */
  public void setFloat(float value){ setFloat(8, value);} 
  
  /**Sets a byte value. */
  public void setDouble(double value){ setDouble(8, value);} 
  
	@Override protected void specifyEmptyDefaultData()
	{
	}

	@Override protected int specifyLengthElement() throws IllegalArgumentException
	{ return sizeofElement;
	}

	@Override public int specifyLengthElementHead()
	{ return sizeofElement;
	}
	
	
}//class SetValue
		
	
}
