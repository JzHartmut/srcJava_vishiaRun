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
package org.vishia.bridgeC;

import java.util.Collection;


//import java.util.concurrent.ConcurrentLinkedQueue;
/**This version of ConcurrentLinkedQueue inherites the original java.util.concurrent.ConcurrentLinkedQueue 
 * without any other extensions or overload methods,
 * but it has special constructors, which are oriented to a C-language implementation for embedded systemss
 * with a static memory allocation. In Java the special constructor functions were dummies.
 */
public class ConcurrentLinkedQueue<EntryType> extends java.util.concurrent.ConcurrentLinkedQueue<EntryType>
{
  private static final long serialVersionUID = 8639030493898499627L;

  /**Same as original:
   * Creates a <tt>ConcurrentLinkedQueue</tt> that is initially empty.
   */
  public ConcurrentLinkedQueue() {}

  /**Same as original:
   * Creates a <tt>ConcurrentLinkedQueue</tt>
   * initially containing the elements of the given collection,
   * added in traversal order of the collection's iterator.
   * @param c the collection of elements to initially contain
   * @throws NullPointerException if the specified collection or any
   *         of its elements are null
   */
  public ConcurrentLinkedQueue(Collection<? extends EntryType> c) 
  { super(c);
  }

  /**Creates an empty LinkedQueue,
   * but with memory to save nodes.
   * Additional for C-Using in embedded systems. 
   */
  public ConcurrentLinkedQueue(MemC memNodes)
  { //ignore the sharing in Java implementation!
    super();
  }
  
  /**Additional for C-Using in embedded systems: Creates an empty LinkedQueue,
   * but with shared nodes with another LinkedQueue
   */
  public ConcurrentLinkedQueue(ConcurrentLinkedQueue<EntryType> srcNodeShare)
  { //ignore the sharing in Java implementation!
    super();
  }
  
}
