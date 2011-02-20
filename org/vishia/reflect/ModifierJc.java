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
package org.vishia.reflect;

public class ModifierJc
{

	
	public final static int mPrimitiv =             0x000f0000;
	
	public final static int m_Containertype =       0x00f00000;
	public final static int kLinkedListJc =         0x00100000;
	public final static int kObjectArrayJc =        0x00200000;
	public final static int kArrayListJc =          0x00300000;
	public final static int kMapJc =                0x00400000;
	public final static int kUML_LinkedList =       0x00500000;
	public final static int kUML_Map =              0x00600000;
	public final static int kUML_ArrayList =        0x00700000;
	public final static int kStaticArray =          0x00800000;

	public final static int mAddressing =           0x03000000;
	public final static int kEmbedded =             0x01000000;
	public final static int kReference =            0x02000000;
	
	public final static int mContainerinstance =    0x30000000;
	
	public final static int kEmbeddedContainer =    0x10000000;
	public final static int kReferencedContainer =  0x20000000;
	public final static int kEnhancedRefContainer = 0x30000000;
	
	
	public static boolean isCollection(int val){ return (val & m_Containertype) !=0; }
	
	
	public static boolean isStaticArray(int val){ return (val & m_Containertype) == kStaticArray; }
	
	public static boolean isStaticEmbeddedArray(int val){ return (val & (m_Containertype|mContainerinstance)) == (kStaticArray|kEmbeddedContainer ); }
	
}
