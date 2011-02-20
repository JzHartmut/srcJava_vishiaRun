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
package org.vishia.byteData;

import org.vishia.util.StringFormatter;

public abstract class ByteDataAccessDbg extends ByteDataAccess
{
	/**Use especially for test, only used in toString(). 
   * @java2c=instanceType:"StringFormatter". It is never a derived class.*/
  StringFormatter toStringformatter = null;


  /**This method is escpecially usefull to debug in eclipse. 
   * It shows the first bytes of head, the position of child and the first bytes of the child.
   */
  public String toString()
  { //NOTE: do not create a new object in every call, it is uneffective.
    if(data==null){ return "no data"; }
    else
    { if(toStringformatter == null){ toStringformatter = new StringFormatter(); }
      else { toStringformatter.reset(); }
      toStringformatter.addint(idxBegin, "33331").add("..").addint(idxFirstChild,"333331").add(":");
      int nrofBytes = idxFirstChild - idxBegin;
      if(nrofBytes > 16){ nrofBytes = 16; }
      if(nrofBytes <0){ nrofBytes = 4; }
      if(idxBegin + nrofBytes > data.length){ nrofBytes = data.length - idxBegin; }  
      toStringformatter.addHexLine(data, idxBegin, nrofBytes, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
      toStringformatter.add(" child ").addint(idxCurrentChild,"-3331").add("..").addint(idxCurrentChildEnd,"-33331").add(":");
      if(idxCurrentChild >= idxBegin)
      { 
        nrofBytes = idxCurrentChildEnd - idxCurrentChild;
        if(nrofBytes > 16){ nrofBytes = 16; }
        if(nrofBytes <0){ nrofBytes = 4; }
        if(idxCurrentChild + nrofBytes > data.length){ nrofBytes = data.length - idxBegin; }  
        toStringformatter.addHexLine(data, idxCurrentChild, nrofBytes, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
      }
      final String ret = toStringformatter.toString();
      return ret;
    }  
  }
	
}
