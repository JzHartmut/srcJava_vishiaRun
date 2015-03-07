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

import org.vishia.bridgeC.MemC;
import org.vishia.byteData.ByteDataAccessBase;
import org.vishia.reflect.ClassJc;
import org.vishia.util.Java4C;
import org.vishia.util.StringFormatter;
/**This class supports preparing data for the Inspector-datagram-definition.
 * @author Hartmut Schorrig
 *
 */
public final class InspcDataExchangeAccess
{
  
  
  /**Version, history and license.
   * <ul>
   * <li>2013-12-08 Hartmut chg: Rename Datagram to {@link InspcDatagram} and Info to {@link Inspcitem}, better to associate.
   * <li>2013-12-08 Hartmut new: {@link InspcSetValueData} 
   * <li>2013-12-07 Hartmut chg:
   *   <ul>
   *   <li>{@link InspcDatagram#setHeadRequest(int, int, int)} with answerNr = 0 and {@link InspcDatagram#setHeadAnswer(int, int, int)}
   *     with previous behavior, answerNr = 1 initial. A request telegram sends the answerNr = 0 up to now.
   *   <li>Some {@link InspcDatagram#knrofBytes} etc. now private. Any application uses the access methods.
   *   <li>new {@link InspcDatagram#getAnswerNr()} and {@link InspcDatagram#lastAnswer()}  
   *   <li>new {@link Inspcitem#kSetvaluedata}, {@link Inspcitem#kAnswervaluedata} as a new kind of request.
   *   <li>new {@link #kInvalidIndex} to distinguish an index error from a value error.
   *   </ul> 
   * <li>2012-04-09 Hartmut new: Some enhancements, especially {@link Inspcitem#kGetValueByIndex} 
   *   The {@link Inspcitem#setInfoHead(int, int, int)} and {@link Inspcitem#setLength(int)} now adjusts the
   *   length of the element and parent in the {@link ByteDataAccessBase} data with the same.
   *   Set this information at end of filling variable data in an Info item, then all is correct.
   * <li>2011-06-21 Hartmut new {@link InspcSetValue} completed with set-methods. 
   *     Note: Because this class is used in Java2C for C-Programming, the short methods should be designated
   *     to use macros while translation Java2C.
   * <li>2011-01-01 Hartmut Translated to Java
   * <li>2005 Hartmut created for C-programming
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
   */
  public static final int version = 20120409;
  
  
  
  /**Preparing the header of a datagram.
   * 
   */
  public final static class InspcDatagram extends ByteDataAccessBase
  {
    private static final int knrofBytes = 0;
    private static final int knEntrant = 2;  //2
    private static final int kencryption = 4; //4
    private static final int kseqnr = 8;   //8
    private static final int kanswerNr =12;      //12
    private static final int kspare13 =13;      //12
    private static final int kspare14 =14;      //12
    public static final int sizeofHead = 16;
    
    public InspcDatagram(@Java4C.PtrVal byte[] buffer)
    { super(sizeofHead);
      assign(buffer, -1, 0);
      super.setBigEndian(true);
    }
    
    public InspcDatagram()
    { super(sizeofHead);
      setBigEndian(true);
    }
    
    
    /**Assigns a datagram.
     * @param data The data of the datagram.
     * @param length number of received data or buffer length for transmit.
     */
    @Java4C.Inline public final void assignDatagram(@Java4C.PtrVal byte[] data, int length){
      super.assign(data, length, 0);
    }
    
    
    @Java4C.Inline public final void setLengthDatagram(int length){ setInt16(0, length); }
    
    @Java4C.Inline public final int getLengthDatagram(){ return getInt16(0); }
    
    /**Sets the head for an answer telegram. Sets the answer number to 1. 
     * Therefore it is for the first answer. All following answers uses {@link #incrAnswerNr()}
     * and {@link #markAnswerNrLast()} to change the answer nr.
     * @param entrant
     * @param seqNr
     * @param encryption
     */
    public final void setHeadRequest(int entrant, int seqNr, int encryption){
      setInt16(knrofBytes, sizeofHead);
      setInt16(knEntrant, entrant);
      setInt32(kseqnr,seqNr);
      setInt8(kanswerNr, 0x0);
      setInt8(kspare13, 0x0);
      setInt16(kspare14, 0x0);
      //int encryption = (int)(((0x10000 * Math.random())-0x8000) * 0x10000);
      setInt32(kencryption, encryption);
    }
    
    /**Sets the head for an request telegram. Sets the answer number to 0. 
     * @param entrant
     * @param seqNr
     * @param encryption
     */
    public final void setHeadAnswer(int entrant, int seqNr, int encryption){
      setInt16(knrofBytes, sizeofHead);
      setInt16(knEntrant, entrant);
      setInt32(kseqnr,seqNr);
      setInt8(kanswerNr, 0x1);
      setInt8(kspare13, 0x0);
      setInt16(kspare14, 0x0);
      //int encryption = (int)(((0x10000 * Math.random())-0x8000) * 0x10000);
      setInt32(kencryption, encryption);
    }
    
    @Java4C.Inline public final void setEntrant(int nr){ setInt16(knEntrant, nr); }
    
    @Java4C.Inline public final int getEntrant(){ return getInt16(knEntrant); }
    
    @Java4C.Inline public final int getEncryption(){ return getInt32(kencryption); }
    
    @Java4C.Inline public final void setSeqnr(int nr){ setInt32(kseqnr, nr); }
    
    @Java4C.Inline public final int getSeqnr(){ return getInt32(kseqnr); }
    
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
    
    /**Gets the number of the answer datagram. 
     * The last datagramm is mask with the bit */
    @Java4C.Inline public final int getAnswerNr()
    { return getInt8(kanswerNr) & 0x7f;
    }
    
    /**Gets the information about the last answer datagram. */
    @Java4C.Inline public final boolean lastAnswer()
    { return (getInt8(kanswerNr) & 0x80) == 0x80;
    }
  }
  
  
  
  
  /**This is the header of an information entry.
   * <pre>
   * Inspcitem::= <@0+2#?SIZE> <@2+2#?cmd> <@4+4#?order> .
   * </pre>
   * An information entry contains this header and may be some childs. 
   * The childs may be simple integer or String childs getting and setting
   * with the methodes to add
   * {@link ByteDataAccessBase#addChildInteger(int, long)} or {@link ByteDataAccessBase#addChildString(String)}. 
   * and the methods to get
   * {@link ByteDataAccessBase#getChildInteger(int)} or {@link ByteDataAccessBase#getChildString(int)}.
   * The childs may be described by a named-here class, forex {@link InspcSetValue}
   * <br><br>
   * The structure of an information entry may be described with XML, where the XML is only
   * a medium to show the structures, for example:
   * <pre>
   * <Info bytes="16" order="345"><StringValue length="7">Example</StringValue></Info>   
   * </pre>
   * In this case 8 Bytes are added after the head. The length stored in the head is 16. 
   * The <StringValue...> consists of a length byte, following by ASCII-character.
   */
  public static class Inspcitem extends ByteDataAccessBase
  {
    private final static int kbyteOrder = 4;
    public final static int sizeofHead = 8;
    
    /** Aufforderung zur Rueckgabe einer Liste aller Attribute und Assoziationen des adressierten Objektes.
    
		    Im Cmd wird der PATH des Objektes uebergeben. Das geschieht in einer Struktur DataExchangeString_OBM.
		    
		    ,  Cmd:
		    ,  +------head------------+---------string--------------+
		    ,  |kGetFields            | PATH mit Punkt am Ende      |
		    ,  +----------------------+-----------------------------+
		    
		    Dabei wird mit dem ,,head.index,, ein Startindex uebergeben. Dieser soll bei der ersten Abfrage =0 sein.
		    
		    Das positive Antworttelegramm enthaelt eine Liste von Items mit einzelnen Attributen und Assoziationen:
		    
		    ,  Answer:
		    ,  +------head-----------+-----string--+-------head----------+-----string--
		    ,  |kAnswerFieldMethod   | Name:Typ    |kAnswerFieldMethod   | Name:Typ ...
		    ,  +---------------------+-------------+---------------------+-------------
		
		    Der Aufbau des Strings ist bei ,,kAnswerFieldMethod,, beschrieben.
     */
    public final static int kGetFields = 0x10;
    
    
    /**Antwort auf Aufforderung zur Rueckgabe einer Liste von Attributen, Assoziationen oder Methoden.
    Das Antwort-Item enthaelt einen Eintrag f�r ein Element, Type DataExchangeString_OBM.
    Die Antwort auf kGetFields oder kGetMethods besteht aus mehreren Items je nach der Anzahl der vorhandenen Elemente.
    Gegebenenfalls ist die Antwort auch auf mehrere Telegramme verteilt.

    Die Zeichenkette fuer ein Item aus zwei Teilen, Typ und Name, getrennt mit einem Zeichen ':'.
    Der angegebenen Typ entspricht dem Typ der Assoziationsreferenz, nicht dem Typ des tatsaechlich assoziierten Objektes,
    wenn es sich nicht um einen einfachen Typ handelt.

    Wenn eine Methode uebergeben wird, dann werden die Aufrufargument-Typen wie eine formale Argumentliste in C angegeben.
    Beispiel:
    , returnType:methodName(arg1Typ,arg2Typ)

    Der Index im Head der Antwort zaehlt die uebergebenen Informationen.
     */
    
    public final static int kAnswerFieldMethod = 0x14;
    
    public final static int kRegisterRepeat = 0x23;
    
    public final static int kAnswerRegisterRepeat = 0x123;
    
    public final static int kFailedRegisterRepeat = 0x124;
    
    public final static int kGetValueByIndex = 0x25;
    
    /**
     * <pre>
        ,  answer:
        ,  +------head-8---------+-int-4-+-int-4-+---n-------+-int-4-+---n-------+
        ,  |kAnswerValueByIndex  | ixVal | types | values    | types | values    |
        ,  +---------------------+-------+-------+-----------+-------+-----------+
     * <ul>
     * <li>First byte after head is the start index of variable for answer. It is 0 for the first answer
     *   info block. If the info block cannot contain all answers, a second info block in a second telegram
     *   will be send which starts with its start index.
     * <li>After them a block with up to 4 values follows. The first word of that block ///
     *   contains 4 types of values, see {@link InspcDataExchangeAccess#kScalarTypes}. Especially the 
     */
    public final static int kAnswerValueByIndex = 0x125;
    
    public final static int kAnswerValue = 0x26;
    
    public final static int kFailedValue = 0x27;
    
    public final static int kGetValueByPath = 0x30;
    
    public final static int kGetAddressByPath = 0x32;
    
    public final static int kSetValueByPath = 0x35;
    
    /**Sets a string value.
     * <pre>
     * < inspcitem> <@+2#?strlen> <@+strlen$?value> <@a4> <@strlen..SIZE$?path> <@a4>
     * </pre>
     * @since 2013-12-24
     */
    public final static int kSetStringByPath = 0x36;
    
    /**Request to get all messages.
    ,  Cmd:<pre>
    ,  +------head-----------+
    ,  |kGetMs               |
    ,  +---------------------+
    </pre>
    ,  Answer:<pre>
    ,  +------head-----------+---------+---------+---------+---------+
    ,  |kAnswerMsg           | Msg     | Msg     | Msg     | Msg     |
    ,  +---------------------+---------+---------+---------+---------+

     * Any Message has 16 to 80 Bytes. The structure of a message is described with {@link org.vishia.msgDispatch.InspcMsgDataExchg}.
     * This structure should be used as child. All values of the message are children of that child.
     */
    public final static int kGetMsg = 0x40;
    public final static int kAnswerMsg = 0x140;
    
    /**Remove gotten messages. Any message contains a sequence number. The answer of {@link #kGetMsg} 
     * contains all messages in a proper sequence order from..to. This Telegram removes the messages from..to sequence.
    ,  Cmd:<pre>
    ,  +------head-----------+--int-----+--int-----+
    ,  |kRemoveMsg           | from seq | to seq   |
    ,  +---------------------+----------+----------+
    </pre>
    ,  Answer:<pre>
    ,  +------head-----------+
    ,  |kAnswerRemoveMsg     |
    ,  +---------------------+
  
     */
    public final static int kRemoveMsg = 0x41;
    public final static int kAnswerRemoveMsgOk = 0x141;
    public final static int kAnswerRemoveMsgNok = 0x241;
    
    
    
    /**This item sets a value with a given position:
     * <pre>
     * <@8+4#?position> <@12+4#?length> <@16..SIZE#?bitImageValue>
     * </pre>
     * The position may be a memory address which is known in the client proper to the target
     * or it may be a relative offset in any target's data. The type of data is target-specific. 
     */
    public final static int kSetvaluedata = 0x50, kAnswervaluedata = 0x150;
    
    public final static int kFailedPath = 0xFe;
    
    public final static int kNoRessource = 0xFd;
    
    
    public final static int kFailedCommand = 0xFF;
    
    
    
    //public static final int kSpecialValueStart = 0x7000, kSpecialValueLast = 0x7fff;
    
    public Inspcitem(int sizeData){
      super(sizeofHead, sizeData);
    }
    
    public Inspcitem(){
      super(sizeofHead);
    }
    
    
    /**Sets the head data and sets the length of the ByteDataAccess-element.
     * @param length The length in head, the length of the info element
     * @param cmd The cmd of the info element
     * @param order The order number to assign the answer.
     */
    public final void setInfoHead(final int length, final int cmd, final int order)
    { setInt16(0, length); 
    setInt16(2, cmd); 
    setInt32(kbyteOrder, order);
    int lengthInfo = length >= sizeofHead ? length : sizeofHead;
    setLengthElement(lengthInfo);  //adjust the length in the access.
    }
    
    @Java4C.Inline public final void setLength(int length)
    { setInt16(0, length); 
    setLengthElement(length);
    }
    
    @Java4C.Inline public final void setCmd(int cmd)
    { setInt16(2, cmd); 
    }
    
    /**Returns the cmd in a Reflitem. The cmd is coded see {@link #kFailedCommand}, {@link #kAnswerFieldMethod} etc.
     * @return
     */
    @Java4C.Inline public final int getCmd(){ return getInt16(2); }
    
    @Java4C.Inline public final int getLenInfo(){ return getInt16(0); }
    
    /**Gets the order number of the info block. A sending info is set with the
     * {@link #setInfoHead(int, int, int)} with any order identification number which is unified for the target
     *   in a proper time. The received info returns the same order ident.   
     * @return The order from this Info block.
     */
    @Java4C.Inline public final int getOrder(){ return getInt32(kbyteOrder); }
    
    
    @Java4C.Exclude
    @Override public void infoFormattedAppend(StringFormatter u) {
      String cmd;
      int cmd1 = getCmd();
      int z = getLenInfo();
      int s = -1;  //start of string
      switch( cmd1 ) {
        case kGetFields:    cmd = "getValueByPath "; s = 8; break;
        case kGetValueByPath:    cmd = "getValueByPath "; s = 8; break;
        case kGetAddressByPath:  cmd = "getAddressByPath ";  s = 8; break;
        case kSetValueByPath:    cmd = "setValueByPath "; break;
        case kSetStringByPath:   cmd = "setStringByPath "; break;
        case kFailedPath:        cmd = "failedPath "; break;
        case kNoRessource:       cmd = "noRessource "; break;
        case kFailedCommand:     cmd = "failedCmd "; break;
        case kAnswerValue:       cmd = "answerValue "; break;
        case kGetMsg:            cmd = "getMsg "; break;
        default: cmd = Integer.toHexString(cmd1);
      }
      String path = s >0 ? getString(s, z-s) : "";
      u.addint(ixBegin, "33331")
      .add("..").addint(ixBegin + sizeHead,"333331")
      .add("..").addint(ixChildEnd,"333331")
      .add(bExpand ? '+' : ':').addint(ixEnd,"333331").add(":");
      u.addHexLine(data, ixBegin, sizeofHead, StringFormatter.k4left).add(": ");
      u.add(cmd).add(path);
    }
    
    
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
  
  
  /**This type identification designates that the value is not available.
   */
  public static final int kTypeNoValue = 0xde;
  
  
  /**This type identification designates that the index to access by index is invalid.
   */
  public static final int kInvalidIndex = 0xdd;
  
  
  
  /**Scalar types started with 0xe0,
   * see {@link org.vishia.reflect.ClassJc#REFLECTION_int32} etc.
   * 
   */
  public static final int kScalarTypes = 0xe0;
  
  
  //}
  
  /**ReflItem which contains a value.
   * <pre>
   * InspcSetValue::= <@0+6#?pwd> <@7+1#?type> [<@8+4 empty> <@12+4#?long> | ...].
   * </pre>
   * 
   */
  @Java4C.ExtendsOnlyMethods
  public final static class InspcSetValue extends ByteDataAccessBase{
    
    public final static int sizeofElement = 16;
    
    private final static int kType = 7;
    
    public InspcSetValue(){
      super(sizeofElement);
      setBigEndian(true);
    }
    
    /**Gets a password for access control.
     * @return The password.
     */
    @Java4C.Inline public final long getPwd(){ return _getLong(0, 6); }
    
    
    @Java4C.Inline public final void setPwd(int pwd){ _setLong(0, 6, pwd); }
    
    @Java4C.Inline public final byte getType(){ return (byte) _getLong(7, 1); }
    
    @Java4C.Inline public final byte getByte(){ return (byte)_getLong(15, -1);} 
    
    @Java4C.Inline public final short getShort(){ return (short)_getLong(14, -2);} 
    
    /**A long value is provided in the bytes 8..15 in Big endian.
     * If only a int value will be used, it were found in the bit 12..15.
     * @return The int value.
     */
    @Java4C.Inline public final int getInt(){ return (int)_getLong(12, -4); }
    
    /**A long value is provided in the bytes 8..15 in Big endian.
     * @return The long value.
     */
    @Java4C.Inline public final long getLong(){ return _getLong(8, -8); }
    
    /**A float value is provided in the bytes 8..11 in Big endian.
     * @return The float value.
     */
    @Java4C.Inline public final float getFloat(){ return getFloat(8); }
    
    @Java4C.Inline public final double getDouble(){ return getDouble(8); }
    
    /**Sets a byte value. */
    @Java4C.Inline public final void setBool(int value)
    { clearData(); _setLong(kType,1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_boolean);  _setLong(15, 1, value);} 
    
    /**Sets a byte value. */
    @Java4C.Inline public final void setByte(int value)
    { clearData(); _setLong(kType,1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int8);  _setLong(15, 1, value);} 
    
    /**Sets a short value. */
    @Java4C.Inline public final void setShort(int value)
    { clearData(); _setLong(kType,1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16);  _setLong(14, 2, value);} 
    
    /**Sets a int32 value. */
    @Java4C.Inline public final void setInt(int value)
    { clearData(); _setLong(kType,1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int32); _setLong(12, 4, value);} 
    
    /**Sets a long value (int64). */
    @Java4C.Inline public final void setLong(long value)
    { clearData(); _setLong(kType,1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int64);  _setLong(8, 8, value);} 
    
    /**Sets a float value. */
    @Java4C.Inline public final void setFloat(float value)
    { clearData(); _setLong(kType,1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_float);  setFloat(12, value);} 
    
    /**Sets a float value given by a int image. */
    @Java4C.Inline public final void setFloatIntImage(int value)
    { clearData(); _setLong(kType,1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_float);  _setLong(12, 4, value);} 
    
    /**Sets a double value. */
    @Java4C.Inline public final void setDouble(double value)
    { clearData(); _setLong(kType,1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_double);  setDouble(8, value);} 
    
    
    
  }//class SetValue
  
  
  
  
  /**An item to set values with an index.
   * <pre>
   * InspcSetValueData::= <@0+8 Inspcitem> <@8+4#?address> <@12+4#?position> <@16 ReflSetValue>
   * </pre>
   * uses @{@link Inspcitem}, {@link InspcSetValue}
   */
  @Java4C.ExtendsOnlyMethods
  public final static class InspcSetValueData extends Inspcitem {
    
    public final static int sizeofElement = 32;
    
    public InspcSetValueData(){
      super(sizeofElement);
      setBigEndian(true);
    }
    
    
    
    
    
    
    
    
    @Java4C.Inline public final void setAddress(int address){ _setLong(8, 4, address); }
    
    @Java4C.Inline public final void setPosition(int position){ _setLong(12, 4, position); }
    
    public final void setBool(int value){ 
      @Java4C.StackInstance InspcSetValue setValue = new InspcSetValue();
      this.addChildAt(16, setValue);
      setValue.setBool((byte)value);
    }
    
    public final void setShort(int value){ 
      @Java4C.StackInstance InspcSetValue setValue = new InspcSetValue();
      this.addChildAt(16, setValue);
      setValue.setShort((short)value);
    }
    
    public final void setByte(int value){ 
      @Java4C.StackInstance InspcSetValue setValue = new InspcSetValue();
      this.addChildAt(16, setValue);
      setValue.setByte((byte)value);
    }
    
    public final void setInt(int value){ 
      @Java4C.StackInstance InspcSetValue setValue = new InspcSetValue();
      this.addChildAt(16, setValue);
      setValue.setInt(value);
    }
    
    public final void setFloat(float value){ 
      @Java4C.StackInstance InspcSetValue setValue = new InspcSetValue();
      this.addChildAt(16, setValue);
      setValue.setFloat(value);
    }
    
    public final void setFloatIntImage(int value){ 
      @Java4C.StackInstance InspcSetValue setValue = new InspcSetValue();
      this.addChildAt(16, setValue);
      setValue.setFloatIntImage(value);
    }
    
    public final void setDouble(double value){ 
      @Java4C.StackInstance InspcSetValue setValue = new InspcSetValue();
      this.addChildAt(16, setValue);
      setValue.setDouble(value);
    }
    
    public final void setLong(long value){ 
      @Java4C.StackInstance InspcSetValue setValue = new InspcSetValue();
      this.addChildAt(16, setValue);
      setValue.setLong(value);
    }
    
    @Java4C.Inline public final void setHead(int order){
      super.setInfoHead(sizeofElement, InspcDataExchangeAccess.Inspcitem.kSetvaluedata, order);
    }
    
  }
  
  
  
  
}
