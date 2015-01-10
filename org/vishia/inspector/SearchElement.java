package org.vishia.inspector;

import java.lang.reflect.Field;

import org.vishia.bridgeC.MemSegmJc;
import org.vishia.reflect.ClassJc;
import org.vishia.reflect.FieldJc;

/**It is a wrapper around the java.lang.Class with additional functionality.
 * @author Hartmut Schorrig 
 */
public class SearchElement
{

	
	/**Only for debugging: Element to store the trace while searching any element.
	 * It is use-able if the algorithm itself are debugged.
	 * @java2c=noObject. 
	 */
	public static class SearchTrc
	{ 
		final MemSegmJc objWhereFieldIsFound = new MemSegmJc(); 
	  /**@java2c=simpleRef. It is const because ClassJc is a const type.*/
		ClassJc clazzWhereFieldIsFound; //
		/**@java2c=simpleRef.  It is const because ClassJc is a const type.*/
		FieldJc field; 
		/**@java2c=simpleRef. */
		ClassJc typeOfField; 
	  final MemSegmJc objOfField = new MemSegmJc(); 
	} 
	
	/**Only for debugging: Stores the trace while searching any element.
	 * It is use-able if the algorithm itself are debugged.
	 * @java2c=embeddedArrayElements,simpleArray. 
	 */
	public static final SearchTrc[] searchTrc  = new SearchTrc[16]; //only debug
	
  static{
  	/**@xxjava2c=initembeddedElement. */
  	for(int ii=0; ii<searchTrc.length; ++ii){ searchTrc[ii] = new SearchTrc();}
	}
	
	
	
	/**Searches a Field in a Object with given path. It is the core routine to access data
	 * with the inspector with the given path.
	 * @param sPath String given path to any Field in an Object.
	 * @param startObj The root Object for the path.
	 * @param retField The field of the found Object will stored at retField[0]. 
	 *          @pjava2c=simpleVariableRef.
	 * @param retIdx If the sPath contains a index written like [12] at end, then
	 *               the index is returned here. Elsewhere -1 is returned in retIdx[0].
	 *        @pjava2c=simpleVariableRef.
	 * @return The address and segment of the Object, which contains the retField[0].
	 */
	public static MemSegmJc searchObject(final String sPath, final Object startObj, final FieldJc[] retField, final int[] retIdx)
	{
		final MemSegmJc currentObj = new MemSegmJc(); //default if nothing is found
	  String sName = null;
	  String sElement = null;
	  ClassJc clazz = ClassJc.getClass(startObj);
	  /**@xx java2c=stackInstance */
	  final MemSegmJc nextObj = new MemSegmJc(startObj,0);  //the source Object for the next access
	  FieldJc field = null;
	  int idx = -1;
	  int posSep;
	  int posStart = 0;
	  int idxSearchTrc = 0;
    try{
    	do{
    		int posEnd;
        posSep = sPath.indexOf('.', posStart);  //may be <0 if no '.' is found
        posEnd = posSep >= 0 ? posSep : sPath.length();
        if(posEnd > posStart)  //should be always, only not on '.' on end.
        { //next loop to search:
          int posBracket;
          currentObj.set(nextObj);
          idx = -1;
          sElement = sPath.substring(posStart, posEnd);
          posBracket = sElement.indexOf('[');
          { int posAngleBracket = sElement.indexOf('<');
	          if(posAngleBracket >=0)
	          { posBracket = posAngleBracket;  //may be also <0
	          }
	          if(posBracket >=0){
	            int posBracketEnd = sElement.indexOf(posAngleBracket >=0 ? '>' : ']', posBracket + 1 );
	            if(posBracketEnd <0) { posBracketEnd = sElement.length(); } //if the ] is missing in the actual context
	            //get index:
	            idx = Integer.parseInt(sElement.substring(posBracket +1, posBracketEnd));
	            sName = sElement.substring(0, posBracket);
	          } else {
	          	sName = sElement;
	          }
	          sElement = null; //clear_StringJc(&sElement);
	          if(sName.equals("super")){
	            field = clazz.getSuperField();
	          } else {
	            field = clazz.getDeclaredField(sName);
	          }
	          sName = null; //clear_StringJc(&sName);
	
	          if(  field != null    //instead exception. See getDeclaredField_ClassJc(). 
	            && (  posSep >0                             //a separator is given,
	               && sPath.length() > (posSep+1) //but at least 1 char after separator!
	            )  ){
	            //a next loop may be necessary because a . is found:
	            //access to the element because it is the base of next loop.
	            //currentObj is the object where the field is searched. 
	            searchTrc[idxSearchTrc].objWhereFieldIsFound.set(currentObj); 
	            searchTrc[idxSearchTrc].clazzWhereFieldIsFound = clazz;
	            searchTrc[idxSearchTrc].field = field;
	            /**@java2c=stackInstance, simpleArray. */
	        	  final ClassJc[] retClazz = new ClassJc[1];
              //access to the field, which is current yet.
	        	  //The real class of the referenced instance is gotten too: retClazz.
	        	  nextObj.set(field.getObjAndClass(currentObj, retClazz, idx));
	            clazz = retClazz[0];
              searchTrc[idxSearchTrc].typeOfField = clazz;
	            searchTrc[idxSearchTrc].objOfField.set(nextObj);
	            if(++idxSearchTrc >= searchTrc.length){ 
	              //prevent overflow. Its only a debug helper. The first entries are relevant.
	            	idxSearchTrc = searchTrc.length-1; 
	            }  
	            //nextObj is the object where the field is member of, 
	            //nextObj is getted started from currentObj, +via offset in field, and access than. 
	            //nextObj may be null, than exit the loop.
	          
	          } else {} //the obj and field are found
	          posStart = posSep + 1;
	        }
        }
    	}while(field != null && posSep > 0 && nextObj.obj() != null);
    } catch(NoSuchFieldException exc){
    	currentObj.setNull();
    	field = null;
    	idx= -1;
    } catch(Exception exc){
    	currentObj.setNull();
    	field = null;
    	idx= -1;
    }
    retField[0] = field;
    retIdx[0] = idx;
    return currentObj;		
	}
	
}
