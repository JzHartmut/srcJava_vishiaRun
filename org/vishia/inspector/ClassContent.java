package org.vishia.inspector;

import java.io.UnsupportedEncodingException;
import org.vishia.bridgeC.MemSegmJc;
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
  
  /**Max number as parameter of call. */
  private int maxNrofAnswerBytes;
  
  /** @java2c=simpleRef. */
  private InspcDataExchangeAccess.Datagram answer;
  
  private final InspcDataExchangeAccess.Info answerItem = new InspcDataExchangeAccess.Info();
  
  /**Buffer to prepare a array information in the answer of a telegram. */
  private final StringBuilder uArray = new StringBuilder(64);
	
  /**Buffer to prepare the value in the answer of a telegram. */
  private final StringBuilder uValue = new StringBuilder(160);
	
  /**Buffer to prepare the answer in the answer of a telegram. */
  private final StringBuilder uAnswer = new StringBuilder(200);
  
  public ClassContent()
  { debugRemoteAccess = MemAccessArrayDebugJc.getSingleton();
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

	@Override public int executeMonitorCmd(InspcDataExchangeAccess.Info cmd, InspcDataExchangeAccess.Datagram answer, int maxNrofAnswerBytes) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{ /**Switch to the cmd execution. */
		int nOrder = cmd.getOrder();
		int nCmd = cmd.getCmd();
		nrofAnswerBytes = InspcDataExchangeAccess.Datagram.sizeofHead;
		switch(nCmd){
		case InspcDataExchangeAccess.Info.kGetFields:
			cmdGetFields(cmd, answer, maxNrofAnswerBytes);
			break;
		case InspcDataExchangeAccess.Info.kGetValueByPath:
			cmdGetValueByPath(cmd, answer, maxNrofAnswerBytes);
			break;
		case InspcDataExchangeAccess.Info.kSetValueByPath:
			cmdSetValueByPath(cmd, answer, maxNrofAnswerBytes);
			break;
		case InspcDataExchangeAccess.Info.kGetAddressByPath:
			cmdGetAddressByPath(cmd, answer, maxNrofAnswerBytes);
			break;
		default:
    { /**Unknown command - answer is: kFailedCommand.
       * think over another variant: return 0 to use delegation pattern ...
       */
    	nrofAnswerBytes += InspcDataExchangeAccess.Info.sizeofHead;
      answer.addChild(answerItem);
      answerItem.setInfoHead(InspcDataExchangeAccess.Info.sizeofHead
      	, InspcDataExchangeAccess.Info.kFailedCommand, nOrder);
    }   
		}//switch
		return 0; //nrofAnswerBytes;
	}
	
	
	
	private final int cmdGetFields(InspcDataExchangeAccess.Info cmd, InspcDataExchangeAccess.Datagram answer, int maxNrofAnswerBytes) 
	{
	  this.maxNrofAnswerBytes = maxNrofAnswerBytes;
	  this.answer = answer;
	  int ixFieldStart;
	  /**@java2c=nonPersistent.  */
		String sVariablePath;
    //FieldJc const* field = null;
    //ObjectJc* obj = null; 
    ClassJc clazz;
    
    nrofAnswerBytes = InspcDataExchangeAccess.Datagram.sizeofHead; 
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
          answerItem.setInfoHead(lengthAnswer4 + InspcDataExchangeAccess.Info.sizeofHead
          	, InspcDataExchangeAccess.Info.kAnswerFieldMethod, nOrderNr);
          /**@java2c=nonPersistent,toStringNonPersist. */
          String sAnswer = uAnswer.toString();
          answerItem.addChildString(sAnswer);
          //  
	      } else if(clazz != null) {
          //not a question to collection size, but real clazz found:
	      	//show the fields:
	      	if(memObj.obj() !=null && MemSegmJc.segment(memObj)==0){
        		/**Check whether an outer class exists. */
          	ClassJc outerObj = clazz.getEnclosingClass();
        		if(outerObj !=null){
        			evaluateFieldGetFields("_outer", outerObj, 0, 0, nOrderNr);
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
            	evaluateFieldGetFields(fields[ixField], nOrderNr);
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
	
	
	
 	private final void evaluateFieldGetFields(FieldJc field, int orderNr)
 	{
 		//FieldJc field = new FieldJc(fieldP);   //regard container types
 		String name = field.getName();
 	  ClassJc typeField = field.getType();
 	  int modifiers = field.getModifiers();
 	  int staticArraySize = field.getStaticArraySize();
 	  evaluateFieldGetFields(name, typeField, modifiers, staticArraySize, orderNr);
 	} 
 	  
 	
 	
 	
 	private final void evaluateFieldGetFields(String name, ClassJc typeField, int modifiers
 		,int  staticArraySize, int orderNr
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
	    int lengthAnswer = InspcDataExchangeAccess.Info.sizeofHead + lengthName + 1 + lengthType 
	                     + lengthArray + lengthValue + (hasSubstructure ? 3:0);
	    int lengthAnswer4 = (lengthAnswer+3)/4 *4;  //aufgerundet durch 4 teilbar.
	    if((nrofAnswerBytes + lengthAnswer4) > maxNrofAnswerBytes){
	    	/**The information doesn't fit in the datagram: Send the last one and clear it. 
	    	 * @java2c=dynamic-call. */
	    	@Java4C.DynamicCall final AnswerComm_ifc answerCommMtbl = answerComm;  //concession to Java2C_ build Mtbl-reference
	    	answerCommMtbl.txAnswer(nrofAnswerBytes, false); 
	    	nrofAnswerBytes = InspcDataExchangeAccess.Datagram.sizeofHead; 
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
        assert(uAnswer.length() + InspcDataExchangeAccess.Info.sizeofHead == lengthAnswer);  //should be the same.
        if(lengthAnswer4 > lengthAnswer) 
        { uAnswer.append("\0\0\0".substring(0,lengthAnswer4 - lengthAnswer)); //fill rest with 0
        }
        answer.addChild(answerItem);
        /**sAnswer contains one entry for the telegram. Builds a String to add,
         * but in concession to Java2c: @java2c=nonPersistent,toStringNonPersist. */
        String sAnswerAdd = uAnswer.toString();
	    	answerItem.addChildString(sAnswerAdd);
        //Prepare the answer item for this field:
        answerItem.setInfoHead(lengthAnswer4, InspcDataExchangeAccess.Info.kAnswerFieldMethod, orderNr);
        nrofAnswerBytes += lengthAnswer4;
      }
		}

 	}
  
 	
 	
 	
	private final int cmdGetValueByPath(InspcDataExchangeAccess.Info cmd
		, InspcDataExchangeAccess.Datagram answer, int maxNrofAnswerBytes) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{
		int nrofBytesCmd = cmd.getLenInfo();
		/**@java2c=nonPersistent.  */
		String sVariablePath = cmd.getChildString(nrofBytesCmd - InspcDataExchangeAccess.Info.sizeofHead);
		getSetValueByPath(cmd, null, answer, sVariablePath);
		return 0;
	}
	
	private final int cmdSetValueByPath(InspcDataExchangeAccess.Info cmd
		, InspcDataExchangeAccess.Datagram answer, int maxNrofAnswerBytes) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{
		int nrofBytesCmd = cmd.getLenInfo();
		/**@java2c=stackInstance. */
		InspcDataExchangeAccess.SetValue setValue = new InspcDataExchangeAccess.SetValue();
		cmd.addChild(setValue);
		int nrofBytesPath = nrofBytesCmd - InspcDataExchangeAccess.Info.sizeofHead 
		                                 - InspcDataExchangeAccess.SetValue.sizeofElement;
		/**@java2c=nonPersistent.  */
		String sVariablePath = cmd.getChildString(nrofBytesPath);
		getSetValueByPath(cmd, setValue, answer, sVariablePath);
		return 0;
	}
	

	
	private final int getSetValueByPath(
		InspcDataExchangeAccess.Info cmd
		, InspcDataExchangeAccess.SetValue accSetValue
		, InspcDataExchangeAccess.Datagram answer
		, String sVariablePath) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{
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
      if(theObject.obj() != null && theField !=null)
      { 
      	ClassJc type = theField.getType();
        int modifier = theField.getModifiers();
        //add the answer-item-head. The rest is specific for types.
        answer.addChild(answerItem);
        answerItem.setInfoHead(0, InspcDataExchangeAccess.Info.kAnswerValue, nOrderNr);
      	
        if(type.isPrimitive()){
        	String sType = type.getName();
        	char cType = sType.charAt(0);
        	switch(cType){
        	case 'v':   //void, handle as int
        	case 'i': { //int
        		int value;
        		if(accSetValue !=null){
        			int setValue = accSetValue.getInt();  //the value to set
        			value = theField.setInt(theObject, setValue, idx);
        		} else {
        		  value = theField.getInt(theObject, idx);
        		}
        		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int32);  //Set the number of char-bytes in 1 byte
        		answerItem.addChildInteger(4, value); 
        		sValue = null;
        	} break;
        	case 'c': { //int
        		char value;
        		if(accSetValue !=null){
        			char setValue = (char)accSetValue.getByte();  //the value to set
        			value = theField.setChar(theObject, setValue, idx);
        		} else {
        		  value = theField.getChar(theObject, idx);
        		}
        		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16);  //Set the number of char-bytes in 1 byte
        		answerItem.addChildInteger(2, (short)value); 
        		sValue = null;
        	} break;
        	case 's': {  //short
        		int value;
        		if(accSetValue !=null){
        			short setValue = accSetValue.getShort();  //the value to set
        			value = theField.setShort(theObject, setValue, idx);
        		} else {
        		  value = theField.getShort(theObject, idx);
        		}
        		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16);  //Set the number of char-bytes in 1 byte
        		answerItem.addChildInteger(2, value); 
        		sValue = null;
        	} break;
        	case 'l': {  //long, it is int64
        		long value = theField.getInt64(theObject, idx);
        		//TODO long won't supported by ReflectPro, use int
        		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int32); //64);  //Set the number of char-bytes in 1 byte
        		answerItem.addChildInteger(4, value);  //8 
        		sValue = null;
        	} break;
        	case 'f': {  //float
        		float valuef;
        		if(accSetValue !=null){
        			float setValue = (float)accSetValue.getDouble();  //the value to set
        			valuef = theField.setFloat(theObject, setValue, idx);
        		} else {
        		  valuef = theField.getFloat(theObject, idx);
        		}
        		int value = Float.floatToRawIntBits(valuef);
        		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_float);  //Set the number of char-bytes in 1 byte
        		answerItem.addChildInteger(4, value); 
        		sValue = null;
        	} break;
        	case 'd': {  //double  TODO 'd' 
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
          		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_float);  //Set the number of char-bytes in 1 byte
          		answerItem.addChildInteger(4, value); 
          	} else {
	        		long value = Double.doubleToLongBits(fvalue);
	        		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_double);  //Set the number of char-bytes in 1 byte
	        		answerItem.addChildInteger(8, value); 
        		}
	        	sValue = null;
        	} break;
        	case 'b': switch(sType.charAt(1)){//boolean or byte or bitField
        		case 'o': { //boolean
        			boolean value;
          		if(accSetValue !=null){
          			boolean setValue = accSetValue.getShort() !=0;  //the value to set
          			value = theField.setBoolean(theObject, setValue, idx);
          		} else {
          		  value = theField.getBoolean(theObject, idx);
          		}
          		int value1 = value ? 1 : 0;
          		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16);  //Set the number of char-bytes in 1 byte
          		answerItem.addChildInteger(2, value1); 
          		sValue = null;
          	} break;
          	case 'y': { //byte
          		short value = theField.getByte(theObject, idx);
          		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16);  //Set the number of char-bytes in 1 byte
          		answerItem.addChildInteger(2, value); 
          		sValue = null;
          	} break;
          	case 'i': { //bitfield
          		short value;
          		if(accSetValue !=null){
          			short setValue = accSetValue.getShort();  //the value to set
          			value = theField.setBitfield(theObject, setValue, idx);
          		} else {
          		  value = theField.getBitfield(theObject, idx);
          		}
          		answerItem.addChildInteger(1, InspcDataExchangeAccess.kScalarTypes+ClassJc.REFLECTION_int16);  //Set the number of char-bytes in 1 byte
          		answerItem.addChildInteger(2, value); 
          		sValue = null;
          	} break;
          	default: {
          		sValue = "?unknownPrimitiveType?";
          	}
          } break;
        	default: {
        		sValue = "?unknownPrimType?";
        	}
        	}//switch
        } else { //it is a complex type, not a numeric.
        	sValue = theField.getString(theObject, idx);
        }
        if(sValue !=null){
        	int zValue = sValue.length();
        	if(zValue > InspcDataExchangeAccess.maxNrOfChars){
        		zValue = InspcDataExchangeAccess.maxNrOfChars;
        		sValue = sValue.substring(0, zValue);
        	}
        	int zInfo = zValue+1+InspcDataExchangeAccess.Info.sizeofHead;
        	answerItem.addChildInteger(1, zValue);  //Set the number of char-bytes in 1 byte
        	answerItem.addChildString(sValue);  //Set the character String after them.
        }
        answerItem.setLength(answerItem.getLength());  //the length of the answerItems in byte.
        /*
        MemSegmJc adr;
        if(idx < 0 || ModifierJc.isStaticEmbeddedArray(modifier)){
          adr = getMemoryAddress_FieldJc(theField,theObject, false, idx);
        }
        */
         
      }
    }catch(Exception exc){
    	/**Unexpected ...*/
    	System.out.println("ClassContent-getValueByPath - unexpected:");
    	exc.printStackTrace();
    }
    
		
	  return 0;
	}	
	
	
	
	private final int cmdGetAddressByPath(InspcDataExchangeAccess.Info cmd
		, InspcDataExchangeAccess.Datagram answer, int maxNrofAnswerBytes) 
	throws IllegalArgumentException, UnsupportedEncodingException 
	{
		int nrofBytesCmd = cmd.getLenInfo();
		int nrofBytesPath = nrofBytesCmd - InspcDataExchangeAccess.Info.sizeofHead;
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
      answerItem.setInfoHead(0, InspcDataExchangeAccess.Info.kAnswerValue, nOrderNr);
    	if(theObject.obj() != null && theField !=null)
      { 
        int addr = theField.getMemoryIdent(theObject, idxP[0]);
        answerItem.addChildInteger(1, InspcDataExchangeAccess.kReferenceAddr);  //Set the number of char-bytes in 1 byte
    		answerItem.addChildInteger(4, addr); 
    		
      } else {
      	answerItem.setCmd(InspcDataExchangeAccess.Info.kFailedValue);
      }
      answerItem.setLength(answerItem.getLength());  //the length of the answerItems in byte.
      
    }catch(Exception exc){
    	/**Unexpected ...*/
    	System.out.println("ClassContent-getValueByPath - unexpected:");
    	exc.printStackTrace();
    }
	
		return 0;
	}
	

	
	
	
	
	
	final void stop(){}
	
}
