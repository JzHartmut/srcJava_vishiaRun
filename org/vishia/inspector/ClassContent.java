package org.vishia.inspector;

import java.io.UnsupportedEncodingException;
import org.vishia.bridgeC.MemSegmJc;
import org.vishia.bridgeC.OS_TimeStamp;
//import org.vishia.byteData.Field_Jc;
import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.inspector.SearchElement.SearchTrc;
import org.vishia.reflect.ClassJc;
import org.vishia.reflect.FieldJc;
import org.vishia.reflect.MemAccessArrayDebugJc;
import org.vishia.reflect.ModifierJc;
import org.vishia.util.Java4C;

/**Implements the commands to get fields and values from data and sets values. 
 * @author Hartmut Schorrig
 *
 *
 *
 */
public final class ClassContent implements CmdConsumer_ifc
{

  /**Version, history and license.
   * <ul>
   * <li>2013-12-07 Hartmut requfix: Check of the Index for getValueByIndex: Don't start from 0, because a reseted target 
   *   starts by 0 always. Use 16 bit of seconds, about 18 h. Only a new restart in exactly 65536 seconds are faulty. 
   *   If the index is invalid, the {@link InspcDataExchangeAccess#kInvalidIndex} is returned for this value.
   *   The requester should remove that index. It is implemented in {@link org.vishia.inspectorAccessor.InspcVariable}.
   * <li>2013-01-10 Hartmut bugfix: If the path fails in 
   *   {@link #getSetValueByPath(org.vishia.communication.InspcDataExchangeAccess.Inspcitem, org.vishia.communication.InspcDataExchangeAccess.InspcSetValue, org.vishia.communication.InspcDataExchangeAccess.InspcDatagram, String, int)},
   *   It should be returned a message anyway. If nothing is returned, the calling doesn't know that problem 
   *   and the whole telegram may not be sent. It would cause a timeout.
   * <li>2012-04-08 Hartmut new: Support of GetValueByIdent
   * <li>2011-02-00 Hartmut created, converted from the C implementation.
   * <li>2006-00-00 Hartmut created for C/C++
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

  
  /**The Object from which the user-given path starts to search. 
   * See {@link SearchElement#searchObject(String, Object, FieldJc[], int[])}.
   * @java2c=simpleRef. */
  private Object rootObj;

  /**Association to produce the answer of a request. It is possible to send more as one answer telegram. 
   * @java2c=simpleRef. */
	private AnswerComm_ifc answerComm;
	
	/**Yet only a placeholder, used in the C-implementation. @java2c=simpleRef.*/
	public MemAccessArrayDebugJc debugRemoteAccess;

	/**A debug helper to visit the search activity on access to any reflection element. 
	 * @java2c=simpleRef, embeddedArrayElements,simpleArray. */
	//public final SearchTrc[] debugSearchTrc;
	
  /**Current number while preparing a answer datagram. */
  private int nrofAnswerBytes = 0; 
  
  ///**Max number as parameter of call. */
  //private int maxNrofAnswerBytes;
  
  ///**Reference to the answer stored in class.  
  // * @java2c=simpleRef. */
  //private InspcDataExchangeAccess.Datagram answerP;
  
  /**Access element for {@link ByteDataAccess} to the answer Item. 
   * It is reused whenever an info item is to be added. */
  private final InspcDataExchangeAccess.Inspcitem answerItem = new InspcDataExchangeAccess.Inspcitem();
  
  /**Buffer to prepare a array information in the answer of a telegram. */
  private final StringBuilder uArray = new StringBuilder(64);
	
  /**Buffer to prepare the value in the answer of a telegram. */
  private final StringBuilder uValue = new StringBuilder(160);
	
  /**Buffer to prepare the answer in the answer of a telegram. */
  private final StringBuilder uAnswer = new StringBuilder(200);
  
  /** java2c=simpleRef. */
  final InspcDataInfo test = new InspcDataInfo();
  
  /**Array of registered data access info items. @java2c = embeddedArrayElements. */
  final InspcDataInfo[] registeredDataAccess = new InspcDataInfo[1024];
  
  public ClassContent()
  { debugRemoteAccess = MemAccessArrayDebugJc.getSingleton();
    for(int ii=0; ii<registeredDataAccess.length; ++ii){
      registeredDataAccess[ii] = new InspcDataInfo();
    }
  	//TODO: Java2C-problem because different array annotations ... 
  	 //TODO: searchTrc should be used an onw instanc with idx!!!
  	//debugSearchTrc = SearchElement.searchTrc;  //init with that static instance.
	}
	
  
  /**Sets the Object which is the root for all data.
   * @param rootObj
   */
  public final void setRootObject(Object rootObj)
  {
		this.rootObj = rootObj;
	}
	
		
	/**sets all aggregations which are unknown on constuctor. */
	@Override public final void setAnswerComm(AnswerComm_ifc answerComm)
	{ this.answerComm = answerComm;
	}

	@Override public int executeMonitorCmd(InspcDataExchangeAccess.Inspcitem cmd, InspcDataExchangeAccess.InspcDatagram answer, int maxNrofAnswerBytes) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{ /**Switch to the cmd execution. */
		int nOrder = cmd.getOrder();
		int nCmd = cmd.getCmd();
		nrofAnswerBytes = InspcDataExchangeAccess.InspcDatagram.sizeofHead;
		switch(nCmd){
		case InspcDataExchangeAccess.Inspcitem.kGetFields:
			cmdGetFields(cmd, answer, maxNrofAnswerBytes);
			break;
		case InspcDataExchangeAccess.Inspcitem.kGetValueByPath:
			cmdGetValueByPath(cmd, answer, maxNrofAnswerBytes);
			break;
		case InspcDataExchangeAccess.Inspcitem.kSetValueByPath:
			cmdSetValueByPath(cmd, answer, maxNrofAnswerBytes);
			break;
		case InspcDataExchangeAccess.Inspcitem.kGetAddressByPath:
      cmdGetAddressByPath(cmd, answer, maxNrofAnswerBytes);
      break;
    case InspcDataExchangeAccess.Inspcitem.kRegisterRepeat:
      cmdRegisterRepeat(cmd, answer, maxNrofAnswerBytes);
      break;
    case InspcDataExchangeAccess.Inspcitem.kGetValueByIndex:
      cmdGetValueByIndex(cmd, answer, maxNrofAnswerBytes);
      break;
    default:
    { /**Unknown command - answer is: kFailedCommand.
       * think over another variant: return 0 to use delegation pattern ...
       */
    	nrofAnswerBytes += InspcDataExchangeAccess.Inspcitem.sizeofHead;
      answer.addChild(answerItem);
      answerItem.setInfoHead(InspcDataExchangeAccess.Inspcitem.sizeofHead
      	, InspcDataExchangeAccess.Inspcitem.kFailedCommand, nOrder);
    }   
		}//switch
		return 0; //nrofAnswerBytes;
	}
	
	
	
	private final int cmdGetFields(InspcDataExchangeAccess.Inspcitem cmd, InspcDataExchangeAccess.InspcDatagram answer, int maxNrofAnswerBytes) 
	{
	  //this.maxNrofAnswerBytes = maxNrofAnswerBytes;
	  //this.answerP = answer;
	  int ixFieldStart;
	  /**@java2c=nonPersistent.  */
		String sVariablePath;
    //FieldJc const* field = null;
    //ObjectJc* obj = null; 
    ClassJc clazz;
    
    nrofAnswerBytes = InspcDataExchangeAccess.InspcDatagram.sizeofHead; 
    boolean bQuestCollectionSize;
    int idxCollectionQuest;
    int nCmd;
    
    nCmd = cmd.getCmd();
    int nrofBytesCmd = cmd.getLenInfo();
    try{ sVariablePath = cmd.getChildString(nrofBytesCmd-8); }
    catch(UnsupportedEncodingException exc){ sVariablePath = ""; }
    ixFieldStart = 0;
    /**Check whether its a question to collection size: */  
    idxCollectionQuest = sVariablePath.indexOf("<?>");
    if(idxCollectionQuest < 0) { idxCollectionQuest = sVariablePath.indexOf("[?]"); }
    if( idxCollectionQuest >=0)
    { bQuestCollectionSize = true;
      sVariablePath = sVariablePath.substring( 0, idxCollectionQuest);
    }
    else
    { bQuestCollectionSize = false;
    }
    
    try{
    	FieldJc field;
      final MemSegmJc memObj = new MemSegmJc();  //an Stack instance in C because it is an embedded type.
      int memSegment = 0;
      boolean found;
      int modifiers;
      if(sVariablePath.length() == 0 || sVariablePath.equals("."))
      { /**root path: */
        found = true; 
        clazz = ClassJc.getClass(rootObj);  //the main class itself contains some pointer yet.
        field = null;
        bQuestCollectionSize = false;  //not at root level
        modifiers = 0;
        memObj.setAddrSegm(rootObj, 0);
      } else {
        /**not the root path, search the obj started from static_cast<ObjectJc*>(this) //targets[0]: */
      	int idx;
        /**@java2c=stackInstance, simpleArray.  */
    	  final int[] idxP = new int[1];
    	  /**@java2c=stackInstance, simpleArray.  */
    	  final FieldJc[] fieldP = new FieldJc[1];
      	/**Search the field in its object, the referenced instance of the field is requested: */
    	  memObj.set(SearchElement.searchObject(sVariablePath, rootObj, fieldP, idxP));
        idx = idxP[0];
        field = fieldP[0];
        found = memObj.obj() != null;
        if(found)
        { /**Field is found. */
        	modifiers = field.getModifiers();
	        if(ModifierJc.isCollection(modifiers) && (idx < 0 || (ModifierJc.isStaticArray(modifiers) 
	        	&& idx >= field.getStaticArraySize())))
	        { bQuestCollectionSize = true;
	          clazz = null;
	        } else { /**normal Object: */
	        	try{
	        		/**Gets the real class of the field. @java2c=stackInstance, simpleArray. */
	        	  final ClassJc[] retClazz = new ClassJc[1];
              field.getObjAndClass(memObj, retClazz, idx);  //index -1: if it is a container, no class is returned
              bQuestCollectionSize = false;  //the clazz may be null
              clazz = retClazz[0];
              found = (clazz != null);       //getFields with non acknowledge answer if it is a null-reference. 
           } catch(RuntimeException exc){
	        		clazz = null;
              bQuestCollectionSize = true;  //Exception, no clazz info, but it may be a collection size quest.
           }
	        }
        } else { //The requested field isn't found, faulty path 
        	clazz = null;
        	modifiers = 0;
        }
        
      }
      if( found)
      { /**The field describes the last found field, the obj is the associated instance 
         * which contains the field, 
         * obj is typeof getDeclaringClass_FieldJc().
         * get the clazz associated to the pointered obj, if there is a reference.
         */
      	int nOrderNr =cmd.getOrder();
        if(bQuestCollectionSize)
        { //the size of an container is requested:
        	int nSize = field.getArraylength(memObj);  //obj is the object where field is member of.
	        boolean hasSubstructure = (modifiers & ModifierJc.mPrimitiv) ==0; 
	        uAnswer.setLength(0);
	        uAnswer.append('[').append(nSize).append("]:");
	        String name = field.getName();
	        uAnswer.append(name);
	        if(hasSubstructure)
  		    { uAnswer.append("...");
          }
	        //the GUI will expand it to some nodes, one per element.
	        int lengthAnswer = uAnswer.length();
	        int lengthAnswer4 = (lengthAnswer +3)/4 *4;
	        if(lengthAnswer4 > lengthAnswer) 
	        { uAnswer.append("\0\0\0".substring(0,lengthAnswer4 - lengthAnswer)); //fill rest with 0
	        }
	        //adds the answer to the telegram:
          answer.addChild(answerItem);
          /**@java2c=nonPersistent,toStringNonPersist. */
          String sAnswer = uAnswer.toString();
          answerItem.addChildString(sAnswer);  //Note: first add the string, then set the head because the setInfoHead adjusts the length of head child.
          answerItem.setInfoHead(lengthAnswer4 + InspcDataExchangeAccess.Inspcitem.sizeofHead
          	, InspcDataExchangeAccess.Inspcitem.kAnswerFieldMethod, nOrderNr);
          //  
	      } else if(clazz != null) {
          //not a question to collection size, but real clazz found:
	      	//show the fields:
	      	if(memObj.obj() !=null && MemSegmJc.segment(memObj)==0){
        		/**Check whether an outer class exists. */
          	ClassJc outerObj = clazz.getEnclosingClass();
        		if(outerObj !=null){
        			evaluateFieldGetFields(answer, "_outer", outerObj, 0, 0, nOrderNr, maxNrofAnswerBytes);
        		}
          }
          /**Gets the fields of the real class of the found reference-field. 
           * @java2c=embeddedArrayElements. */
        	FieldJc[] fields = clazz.getDeclaredFields();
          if (fields != null)
          { 
          	int ii = ixFieldStart;
            if(ii< 0) { ii = 0; } 
            for(int ixField = 0; ixField < fields.length; ++ixField){
              /**Generates one entry per field in the answer telegram. */
            	evaluateFieldGetFields(answer, fields[ixField], nOrderNr, maxNrofAnswerBytes);
            }
          }
        }
      }
      
    } catch(Exception exc){
    	/**Unexpected ...*/
    	System.out.println("ClassContent-getFields - unexpected:");
    	exc.printStackTrace();
    }
		return 0;
	}	
	
	
	
 	private final void evaluateFieldGetFields(InspcDataExchangeAccess.InspcDatagram answer, FieldJc field, int orderNr, int maxNrofAnswerBytes)
 	{
 		//FieldJc field = new FieldJc(fieldP);   //regard container types
 		String name = field.getName();
 	  ClassJc typeField = field.getType();
 	  int modifiers = field.getModifiers();
 	  int staticArraySize = field.getStaticArraySize();
 	  evaluateFieldGetFields(answer, name, typeField, modifiers, staticArraySize, orderNr, maxNrofAnswerBytes);
 	} 
 	  
 	
 	
 	
 	private final void evaluateFieldGetFields(InspcDataExchangeAccess.InspcDatagram answer, 
 	  String name, ClassJc typeField, int modifiers
 		,int  staticArraySize, int orderNr, int maxNrofAnswerBytes
 	)
  {
 	  int modifContainertype = modifiers & ModifierJc.m_Containertype;
 	  boolean hasSubstructure = (modifiers & ModifierJc.mPrimitiv) ==0
     || (modifContainertype !=0); 
    String type = typeField == null ? "unknown" : typeField.getName();
		int lengthName = name.length(); //length_StringJc(&name);
		int lengthType = type.length();
		
		uArray.setLength(0);
		if(modifContainertype == ModifierJc.kUML_LinkedList)
    { uArray.append("[?]:LinkedList"); 
    }
    else if(modifContainertype == ModifierJc.kUML_ArrayList)
    { uArray.append("[?]:ArrayList"); 
    }
    else if(modifContainertype == ModifierJc.kStaticArray)
    { uArray.append('[').append(staticArraySize).append(']'); 
    }
    else if(modifContainertype != 0){
      uArray.append("[?]:TODO-containerType");
    }
		{ uValue.setLength(0);
			int lengthArray = uArray.length();
	    int lengthValue = uValue.length();
	    /**calculate the length of the answer before writing. */
	    int lengthAnswer = InspcDataExchangeAccess.Inspcitem.sizeofHead + lengthName + 1 + lengthType 
	                     + lengthArray + lengthValue + (hasSubstructure ? 3:0);
	    int lengthAnswer4 = (lengthAnswer+3)/4 *4;  //aufgerundet durch 4 teilbar.
	    if((nrofAnswerBytes + lengthAnswer4) > maxNrofAnswerBytes){
	    	/**The information doesn't fit in the datagram: Send the last one and clear it. 
	    	 * @java2c=dynamic-call. */
	    	@Java4C.DynamicCall final AnswerComm_ifc answerCommMtbl = answerComm;  //concession to Java2C_ build Mtbl-reference
	    	answerCommMtbl.txAnswer(nrofAnswerBytes, false); 
	    	nrofAnswerBytes = InspcDataExchangeAccess.InspcDatagram.sizeofHead; 
	      answer.removeChildren();
	      answer.incrAnswerNr();
	    }
      { uAnswer.setLength(0);
      	uAnswer.append(name);
        uAnswer.append(uArray);
        uAnswer.append(':');
        uAnswer.append(type);
        if(lengthValue > 0)
        { uAnswer.append('=');
          uAnswer.append(uValue);
        }
        if(hasSubstructure)
        { //answerItem->data [answerPos] = '*';
          //answerPos +=1;
        	uAnswer.append("...");
        }
        assert(uAnswer.length() + InspcDataExchangeAccess.Inspcitem.sizeofHead == lengthAnswer);  //should be the same.
        if(lengthAnswer4 > lengthAnswer) 
        { uAnswer.append("\0\0\0".substring(0,lengthAnswer4 - lengthAnswer)); //fill rest with 0
        }
        answer.addChild(answerItem);
        /**sAnswer contains one entry for the telegram. Builds a String to add,
         * but in concession to Java2c: @java2c=nonPersistent,toStringNonPersist. */
        String sAnswerAdd = uAnswer.toString();
	    	answerItem.addChildString(sAnswerAdd);
        //Prepare the answer item for this field:
        answerItem.setInfoHead(lengthAnswer4, InspcDataExchangeAccess.Inspcitem.kAnswerFieldMethod, orderNr);
        nrofAnswerBytes += lengthAnswer4;
      }
		}

 	}
  
 	
 	
 	
	private final int cmdGetValueByPath(InspcDataExchangeAccess.Inspcitem cmd
		, InspcDataExchangeAccess.InspcDatagram answer, int maxNrofAnswerBytes) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{
		int nrofBytesCmd = cmd.getLenInfo();
		/**@java2c=nonPersistent.  */
		String sVariablePath = cmd.getChildString(nrofBytesCmd - InspcDataExchangeAccess.Inspcitem.sizeofHead);
		getSetValueByPath(cmd, null, answer, sVariablePath, maxNrofAnswerBytes);
		return 0;
	}
	
	private final int cmdSetValueByPath(InspcDataExchangeAccess.Inspcitem cmd
		, InspcDataExchangeAccess.InspcDatagram answer, int maxNrofAnswerBytes) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{
		int nrofBytesCmd = cmd.getLenInfo();
		/**@java2c=stackInstance. */
		InspcDataExchangeAccess.InspcSetValue setValue = new InspcDataExchangeAccess.InspcSetValue();
		cmd.addChild(setValue);
		int nrofBytesPath = nrofBytesCmd - InspcDataExchangeAccess.Inspcitem.sizeofHead 
		                                 - InspcDataExchangeAccess.InspcSetValue.sizeofElement;
		/**@java2c=nonPersistent.  */
		String sVariablePath = cmd.getChildString(nrofBytesPath);
		getSetValueByPath(cmd, setValue, answer, sVariablePath, maxNrofAnswerBytes);
		return 0;
	}
	

	
	private final int getSetValueByPath(
		InspcDataExchangeAccess.Inspcitem cmd
		, InspcDataExchangeAccess.InspcSetValue accSetValue
		, InspcDataExchangeAccess.InspcDatagram answer
		, String sVariablePath
		, int maxNrofAnswerBytes) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{
		int nOrderNr =cmd.getOrder();
		/**@java2c=nonPersistent.  */
		FieldJc theField = null;
		/**@java2c=stackInstance, simpleArray.  */
	  final FieldJc[] theFieldP = new FieldJc[1];
		final MemSegmJc theObject = new MemSegmJc();
    /**@java2c=nonPersistent.  */
		int memSegment = 0;
    int idxOutput = 0;
    int maxIdxOutput = 1200; //note: yet a full telegramm can be used.      
    try{
    	int idx;
    	/**@java2c=stackInstance, simpleArray.  */
  	  final int[] idxP = new int[1];
      theObject.set(SearchElement.searchObject(sVariablePath, rootObj, theFieldP, idxP));
      theField = theFieldP[0];
      idx = idxP[0];
      if(theObject.obj() != null && theField !=null)
      { 
        answer.addChild(answerItem);
        getSetValue(theField, idx, theObject, accSetValue, maxNrofAnswerBytes);
        int nBytesItem = answerItem.getLength();
        answerItem.setInfoHead(nBytesItem, InspcDataExchangeAccess.Inspcitem.kAnswerValue, nOrderNr);
      } else {
        //Info failed value to return. Note: If nothing is returned, the calling doesn't know that problem 
        //and the whole telegram may not be sent. It would cause a timeout. Bugfix on 2013-01-10
        answer.addChild(answerItem);
        int nBytesItem = answerItem.getLength();
        answerItem.setInfoHead(nBytesItem, InspcDataExchangeAccess.Inspcitem.kFailedPath, nOrderNr);
      }
    }catch(Exception exc){
    	/**Unexpected ...*/
    	System.out.println("ClassContent-getValueByPath - unexpected:");
    	exc.printStackTrace();
    }
    
		
	  return 0;
	}	
	
	
	
	/**Sets the value if accSetValue is not null, fills the {@link #answerItem} with the read value.
	 * @param theField describes the field to access
	 * @param idx with this index
	 * @param theObject in this object
	 * @param accSetValue null or given set value.
	 * @param maxNrofAnswerBytes 
	 * @return true if this information has space in the current telgram, false if not.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private boolean getSetValue(final FieldJc theField, int idx, final MemSegmJc theObject
	   , InspcDataExchangeAccess.InspcSetValue accSetValue
	   , int maxNrofAnswerBytes
	) throws IllegalArgumentException, IllegalAccessException
	{
	  ClassJc type = theField.getType();
    int modifier = theField.getModifiers();
    String sValue = null;
    boolean bOk;
    int actLenTelg = answerItem.getLengthTotal();
    int restLen = maxNrofAnswerBytes - actLenTelg;
    int nType;
    //add the answer-item-head. The rest is specific for types.
    if(type.isPrimitive()){
      String sType = type.getName();
      char cType = sType.charAt(0);
      switch(cType){
      case 'v':   //void, handle as int
      case 'i': { //int
        bOk = restLen >=5;
        if(bOk){
          int value;
          if(accSetValue !=null){
            int setValue = accSetValue.getInt();  //the value to set
            value = theField.setInt(theObject, setValue, idx);
          } else {
            value = theField.getInt(theObject, idx);
          }
          nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int32;
          answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
          answerItem.addChildInteger(4, value); 
          sValue = null;
        }
      } break;
      case 'c': { //int
        bOk = restLen >=3;
        if(bOk){
          char value;
          if(accSetValue !=null){
            char setValue = (char)accSetValue.getByte();  //the value to set
            value = theField.setChar(theObject, setValue, idx);
          } else {
            value = theField.getChar(theObject, idx);
          }
          nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16;
          answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
          answerItem.addChildInteger(2, (short)value); 
          sValue = null;
        }
      } break;
      case 's': {  //short
        bOk = restLen >=3;
        if(bOk){
          int value;
          if(accSetValue !=null){
            short setValue = accSetValue.getShort();  //the value to set
            value = theField.setShort(theObject, setValue, idx);
          } else {
            value = theField.getShort(theObject, idx);
          }
          nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16;
          answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
          answerItem.addChildInteger(2, value); 
          sValue = null;
        }
      } break;
      case 'l': {  //long, it is int64
        bOk = restLen >=5;  //TODO 9
        if(bOk){
          long value = theField.getInt64(theObject, idx);
          //TODO long won't supported by ReflectPro, use int
          nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int32;
          answerItem.addChildInteger(1, nType); //64);  //Set the number of char-bytes in 1 byte
          answerItem.addChildInteger(4, value);  //8 
          sValue = null;
        }
      } break;
      case 'f': {  //float
        bOk = restLen >=5;
        if(bOk){
          float valuef;
          if(accSetValue !=null){
            float setValue = (float)accSetValue.getDouble();  //the value to set
            valuef = theField.setFloat(theObject, setValue, idx);
          } else {
            valuef = theField.getFloat(theObject, idx);
          }
          int value = Float.floatToRawIntBits(valuef);
          nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_float;
          answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
          answerItem.addChildInteger(4, value); 
          sValue = null;
        }
      } break;
      case 'd': {  //double  TODO 'd' 
        bOk = restLen >=9;
        if(bOk){
          double fvalue;
          if(accSetValue !=null){
            double setValue = accSetValue.getDouble();  //the value to set
            fvalue = theField.setDouble(theObject, setValue, idx);
          } else {
            fvalue = theField.getDouble(theObject, idx);
          }
          boolean fixme = true;
          if(fixme){
            int value = Float.floatToRawIntBits((float)fvalue);  //send as float, Problem of ReflectPro
            nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_float;
            answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
            answerItem.addChildInteger(4, value); 
          } else {
            long value = Double.doubleToLongBits(fvalue);
            nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_double;
            answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
            answerItem.addChildInteger(8, value); 
          }
          sValue = null;
        }
      } break;
      case 'b': switch(sType.charAt(1)){//boolean or byte or bitField
        case 'o': { //boolean
          bOk = restLen >=3;
          if(bOk){
            boolean value;
            if(accSetValue !=null){
              boolean setValue = accSetValue.getShort() !=0;  //the value to set
              value = theField.setBoolean(theObject, setValue, idx);
            } else {
              value = theField.getBoolean(theObject, idx);
            }
            int value1 = value ? 1 : 0;
            nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16;
            answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
            answerItem.addChildInteger(2, value1); 
            sValue = null;
          }
        } break;
        case 'y': { //byte
          bOk = restLen >=3;
          if(bOk){
            short value = theField.getByte(theObject, idx);
            nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16;
            answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
            answerItem.addChildInteger(2, value); 
            sValue = null;
          }
        } break;
        case 'i': { //bitfield
          bOk = restLen >=3;
          if(bOk){
            short value;
            if(accSetValue !=null){
              short setValue = accSetValue.getShort();  //the value to set
              value = theField.setBitfield(theObject, setValue, idx);
            } else {
              value = theField.getBitfield(theObject, idx);
            }
            nType = InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16;
            answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
            answerItem.addChildInteger(2, value); 
            sValue = null;
          }
        } break;
        default: {
          bOk = restLen >=1;
          if(bOk){
            nType = InspcDataExchangeAccess.kTypeNoValue;
            answerItem.addChildInteger(1, nType);  
            sValue = null; //"?unknownPrimitiveType?";
          }
        }
      } break;
      default: {
        bOk = restLen >=1;
        if(bOk){
          nType = InspcDataExchangeAccess.kTypeNoValue;
          answerItem.addChildInteger(1, nType);  
          sValue = null; //"?unknownPrimType?";
        }
      }
      }//switch
    } else { //it is a complex type, not a numeric.
      sValue = theField.getString(theObject, idx);
      bOk = true;
    }
    if(sValue !=null){
      int zValue = sValue.length();
      if(zValue > InspcDataExchangeAccess.maxNrOfChars){
        zValue = InspcDataExchangeAccess.maxNrOfChars;
        sValue = sValue.substring(0, zValue);
      }
      bOk = restLen >= zValue +1;
      if(bOk){
        int zInfo = zValue+1+InspcDataExchangeAccess.Inspcitem.sizeofHead;
        nType = zValue;
        answerItem.addChildInteger(1, nType);  //Set the number of char-bytes in 1 byte
        answerItem.addChildString(sValue);  //Set the character String after them.
      }
    }
    /*
    MemSegmJc adr;
    if(idx < 0 || ModifierJc.isStaticEmbeddedArray(modifier)){
      adr = getMemoryAddress_FieldJc(theField,theObject, false, idx);
    }
    */
    return bOk;
	}
	
	
	
	private final int cmdGetAddressByPath(InspcDataExchangeAccess.Inspcitem cmd
		, InspcDataExchangeAccess.InspcDatagram answer, int maxNrofAnswerBytes) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{
		int nrofBytesCmd = cmd.getLenInfo();
		int nrofBytesPath = nrofBytesCmd - InspcDataExchangeAccess.Inspcitem.sizeofHead;
		/**@java2c=nonPersistent.  */
		String sVariablePath = cmd.getChildString(nrofBytesPath);
		int nOrderNr =cmd.getOrder();
		/**@java2c=nonPersistent.  */
		FieldJc theField = null;
		/**@java2c=stackInstance, simpleArray.  */
	  final FieldJc[] theFieldP = new FieldJc[1];
		final MemSegmJc theObject = new MemSegmJc();
    /**@java2c=nonPersistent.  */
		String sValue;
		int memSegment = 0;
    int idxOutput = 0;
    int maxIdxOutput = 1200; //note: yet a full telegramm can be used.      
    try{
    	int idx;
    	/**@java2c=stackInstance, simpleArray.  */
  	  final int[] idxP = new int[1];
      theObject.set(SearchElement.searchObject(sVariablePath, rootObj, theFieldP, idxP));
      theField = theFieldP[0];
      idx = idxP[0];
      answer.addChild(answerItem);
      answerItem.setInfoHead(0, InspcDataExchangeAccess.Inspcitem.kAnswerValue, nOrderNr);
    	if(theObject.obj() != null && theField !=null)
      { 
        int addr = theField.getMemoryIdent(theObject, idxP[0]);
        answerItem.addChildInteger(1, InspcDataExchangeAccess.kReferenceAddr);  //Set the number of char-bytes in 1 byte
    		answerItem.addChildInteger(4, addr); 
    		
      } else {
      	answerItem.setCmd(InspcDataExchangeAccess.Inspcitem.kFailedValue);
      }
      answerItem.setLength(answerItem.getLength());  //the length of the answerItems in byte.
      
    }catch(Exception exc){
    	/**Unexpected ...*/
    	System.out.println("ClassContent-getValueByPath - unexpected:");
    	exc.printStackTrace();
    }
	
		return 0;
	}
	

	
	int cmdRegisterRepeat(InspcDataExchangeAccess.Inspcitem cmd
    , InspcDataExchangeAccess.InspcDatagram answer, int maxNrofAnswerBytes) 
  throws IllegalArgumentException, UnsupportedEncodingException 
	{
    int nrofBytesCmd = cmd.getLenInfo();
    int nrofBytesPath = nrofBytesCmd - InspcDataExchangeAccess.Inspcitem.sizeofHead;
    /**@java2c=nonPersistent.  */
    String sVariablePath = cmd.getChildString(nrofBytesPath);
    int nOrderNr =cmd.getOrder();
    /**@java2c=nonPersistent.  */
    FieldJc theField = null;
    /**@java2c=stackInstance, simpleArray.  */
    final FieldJc[] theFieldP = new FieldJc[1];
    final MemSegmJc theObject = new MemSegmJc();
    /**@java2c=nonPersistent.  */
    String sValue;
    int memSegment = 0;
    int idxOutput = 0;
    int maxIdxOutput = 1200; //note: yet a full telegramm can be used.      
    try{
      int idx;
      /**@java2c=stackInstance, simpleArray.  */
      final int[] idxP = new int[1];
      theObject.set(SearchElement.searchObject(sVariablePath, rootObj, theFieldP, idxP));
      theField = theFieldP[0];
      idx = idxP[0];
      answer.addChild(answerItem);
      answerItem.setInfoHead(0, InspcDataExchangeAccess.Inspcitem.kAnswerRegisterRepeat, nOrderNr);
      if(theObject.obj() != null && theField !=null)
      { 
        ClassJc type = theField.getType();
        int modifier = theField.getModifiers();
        int addr = theField.getMemoryIdent(theObject, idxP[0]);
      
        int ixReg = 0;
        InspcDataInfo freeOrder = null;
        int currentTime = OS_TimeStamp.os_getSeconds();
        /**Search a free position in the static array: */
        int ixRegLast = 0;
        int diffLast = 0;
        while(freeOrder == null && ixReg < registeredDataAccess.length)
        { InspcDataInfo order = registeredDataAccess[ixReg];
          int lastUsed = currentTime - order.lastUsed;
          if( (lastUsed) > 3600 ){
            freeOrder = order;
          }
          else { 
            if(lastUsed > diffLast){
              diffLast = lastUsed;
              ixRegLast = ixReg;
            }
            ixReg +=1;
          }
        }
        if(freeOrder == null)
        { //no registerItem found which is older than 1 our:
          ixReg = ixRegLast;  //get the oldest one.
          freeOrder = registeredDataAccess[ixReg];
        }
        freeOrder.lastUsed = currentTime;
        freeOrder.addrValue = theField;
        freeOrder.addr.set(theObject);
        //don't start by 0 on reset of the target! date-20131208
        //- freeOrder.check +=1; //change it to detect old requests at same index.
        freeOrder.check = (short)(currentTime);
        int ixAnswer = ixReg | (freeOrder.check <<12);
        answerItem.addChildInteger(4, ixAnswer); 
        getSetValue(theField, idx, theObject, null, maxNrofAnswerBytes);
        answerItem.setLength(answerItem.getLength());  //the length of the answerItems in byte.
      } else { 
        answerItem.setCmd(InspcDataExchangeAccess.Inspcitem.kFailedPath);
      }
      answerItem.setLength(answerItem.getLength());  //the length of the answerItems in byte.
      
    } catch(Exception exc){
      /**Unexpected ...*/
      System.out.println("ClassContent-getValueByPath - unexpected:");
      exc.printStackTrace();
    }
  
    return 0;
  }


	
	int cmdGetValueByIndex(InspcDataExchangeAccess.Inspcitem cmd
    , InspcDataExchangeAccess.InspcDatagram answer, int maxNrofAnswerBytes) 
  throws IllegalArgumentException, UnsupportedEncodingException
  {
    int nrofVariable = (cmd.getLenInfo() - InspcDataExchangeAccess.Inspcitem.sizeofHead) / 4; 
    //cyclTime_MinMaxTime_Fwc(timeValueRepeat);
    try
    { int idxOutput = 0;
      int idx;
      boolean bOk = true;
      int nOrderNr =cmd.getOrder();
      answer.addChild(answerItem);  ///
      answerItem.addChildInteger(4, 0);  //TODO more as one telg.
      while(cmd.sufficingBytesForNextChild(4)){
      //for(idx = 0; bOk && idx < nrofVariable; idx++)
        int idxReq = (int)cmd.getChildInteger(4); //getInt16BigEndian(&input->ixAccess[idx].ix);  //the requestions of objmoni access
        int idxDataAccess = idxReq & 0x0fff;
        int check = (idxReq >> 12) & 0xfffff;
        InspcDataInfo order = registeredDataAccess[idxDataAccess];
        //TODO check if it matches in space of telegram!
        if(check == order.check && order.addrValue !=null){
          getSetValue(order.addrValue, 0, order.addr, null, maxNrofAnswerBytes);
        } else {
          //The ident is faulty. Any ident request should have its answer.
          answerItem.addChildInteger(1, InspcDataExchangeAccess.kInvalidIndex);
        }
      }//while
      int nBytesItem = answerItem.getLength();
      answerItem.setInfoHead(nBytesItem, InspcDataExchangeAccess.Inspcitem.kAnswerValueByIndex, nOrderNr);
    } catch(IllegalAccessException exc){
      System.err.println("ClassContent - cmdGetValueByIndex; Unexpected IllegalAccessException");
    }
    return 0;
  }








	
	final void stop(){}
	
}
