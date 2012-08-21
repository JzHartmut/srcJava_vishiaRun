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
package org.vishia.msgDispatch;

import java.util.Arrays;

import org.vishia.bridgeC.ConcurrentLinkedQueue;
import org.vishia.bridgeC.MemC;
import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.VaArgBuffer;
import org.vishia.bridgeC.Va_list;

/**This is the core of the message dispatcher. It dispatches only. 
 * The dispatch table maybe filled with a simplest algorithm. 
 * This class is able to use in a simple enviroment.
 * 
 * @author Hartmut Schorrig
 *
 */
public class MsgDispatcherCore 
{

  /**version, history and license:
   * <ul>
   * <li>2012-08-22 Hartmut new {@link #setMsgTextConverter(MsgText_ifc)}. It provides a possibility to work with a
   *   translation from ident numbers to text with an extra module, it is optional.
   * <li>2012-06-15 Hartmut created as separation from the MsgDispatcher because it may necessary to deploy 
   *   in a small C environment.
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

  /**If this bit is set in the bitmask for dispatching, the dispatching should be done 
   * in the dispatcher Thread. In the calling thread the message is stored in a queue. */
  public final static int mDispatchInDispatcherThread = 0x80000000;

  /**If this bit is set in the bitmask for dispatching, the dispatching should only be done 
   * in the calling thread. It is possible that also the bit {@link mDispatchInDispatcherThread}
   * is set, if there is more as one destination. */
  public final static int mDispatchInCallingThread =    0x40000000;

  /**Only this bits are used to indicate the destination via some Bits*/
  public final static int mDispatchBits =               0x3FFFFFFF;
  
  /**Number of Bits in {@link mDispatchWithBits}, it is the number of destinations dispached via bit mask. */
  protected final int nrofMixedOutputs;
  
  /**Calculated mask of bits which are able to mix. */
  public final int mDstMixedOutputs;
   
  /**Calculated mask of bits which are one index. */
  public final int mDstOneOutput;
  
  
  
  /**Mask for dispatch the message to console directly in the calling thread. 
   * It is like system.out.println(...) respectively printf in C.
   * The console output is a fix part of the Message dispatcher.
   */
  public final static int mConsole = 0x01;
  
  /**queued Console output, it is a fix part of the Message dispatcher. */
  public final static int mConsoleQueued = 0x02;
  
  /**Used for argument mode from {@link #setOutputRange(int, int, int, int, int)} to add an output.
   * The other set outputs aren't change. 
   */
  public final static int mAdd = 0xcadd;
  
  /**Used for argument mode from {@link #setOutputRange(int, int, int, int, int)} to set an output.
   * Outputs before are removed. 
   */
  public final static int mSet = 0xc5ed;
  
  /**Used for argument mode from {@link #setOutputRange(int, int, int, int, int)} to remove an output.
   * All other outputs aren't change.
   */
  public final static int mRemove = 0xcde1;
  
  
  
  /**Stores all data of a message if the message is queued here. @java2c=noObject. */  
  public static final class Entry
  {

    /**Bit31 is set if the state is coming, 0 if it is going. */
    public int ident;
     
    /**The bits of destination dispatching are ascertained already before it is taken in the queue. */
    public int dst;
  
    /**The output and format controlling text. In C it should be a reference to a persistent,
     * typical constant String. For a simple system the text can be left empty, using a null-reference. 
     * @java2c=zeroTermString. 
     */
    public String text;
    
  
    /**The time stamp of the message. It is detected before the message is queued. */
    public final OS_TimeStamp timestamp = new OS_TimeStamp();

    /**Values from variable argument list. This is a special structure 
     * because the Implementation in C is much other than in Java. 
     * In Java it is simple: All arguments are of type Object, and the variable argument list
     * is a Object[]. The memory of all arguments are handled in garbage collection. It is really simple.
     * But in C there are some problems:
     * <ul><li>Type of arguments
     * <li>Location of arguments: Simple numeric values are in stack (only call by value is possible)
     *     but strings (const char*) may be non persistent. No safety memory management is present.
     * </ul>
     * Therefore in C language some things should be done additionally. See the special C implementation
     * in VaArgList.c. 
     */
    public final VaArgBuffer values = new VaArgBuffer(11);  //embedded in C
    
    //final Values values = new Values();  //NOTE: 16*4 = 64 byte per entry. NOTE: only a struct is taken with *values as whole.

    public static int _sizeof(){ return 1; } //it is a dummy in Java.
    
  }
  
  /**This class contains some test-counts for debugging. It is a own class because structuring of attributes. 
   * @xxxjava2c=noObject.  //NOTE: ctor without ObjectJc not implemented yet.
   */
  protected static final class TestCnt
  {
    int noOutput;
    int tomuchMsgPerThread;
  }
  
  /**This class contains all infomations for a output. There is an array of this type in MsgDispatcher. 
   * @java2c=noObject.
   */
  protected static final class Output
  {
    /**Short name of the destination, used for {@link #setOutputRange } or {@link #setOutputFromString }
     * @xxxjava2c=simpleArray.
     */
    String name;
    
    /**The output interface. */
    LogMessage outputIfc;

    /**true if this output is processed in the dispatcher thread, 
     * false if the output is called immediately in the calling thread.
     */ 
    boolean dstInDispatcherThread;
  
    
  }
  
  final TestCnt testCnt = new TestCnt();
  
  /**List of messages to process in the dispatcher thread.
   * @java2c=noGC.
   */
  final ConcurrentLinkedQueue<Entry> listOrders;
  
  /**List of entries for messages to use. For C usage it is a List with a fix size.
   * The ConcurrentLinkedQueue may be implemented in a simple way if only a simplest system is used.
   * @java2c=noGC.
   */
  final ConcurrentLinkedQueue<Entry> freeOrders;
  
  /**List of idents, its current length. */ 
  protected int actNrofListIdents;

  /**List of idents, a array with lengthListIdents elements.
   * @java2c=noGC.
   */
  protected int[] listIdents;  //NOTE: It is a pointer to an array struct.

  /**List of destination bits for the idents.
   * @java2c=noGC.
   */
  protected int[] listBitDst;  //NOTE: It is a pointer to an array struct.

  /**up to 30 destinations for output.
   * @java2c=noGC,embeddedArrayElements.
   */
  protected Output[] outputs;

  
  /**Converter from the ident number to a text. Maybe null, then unused.
   * See {@link #setMsgTextConverter(MsgText_ifc)}
   */
  protected MsgText_ifc msgText;
  
  
  /**Initializes the instance.
   * @param maxQueue The static limited maximal size of the queue to store messages from user threads
   *        to dispatch in the dispatcher thread. If you call the dispatching in dispatcher thread
   *        cyclicly in 100-ms-steps, and you have worst case no more as 200 messages in this time,
   *        200 is okay.
   * @param nrofMixedOutputs
   */
  MsgDispatcherCore(int maxQueue, int nrofMixedOutputs){
    this.nrofMixedOutputs = nrofMixedOutputs;
    if(nrofMixedOutputs < 0 || nrofMixedOutputs > 28) throw new IllegalArgumentException("max. nrofMixedOutputs");
    this.mDstMixedOutputs = (1<<nrofMixedOutputs) -1;
    this.mDstOneOutput = mDispatchBits & ~mDstMixedOutputs;

    /**A queue in C without dynamically memory management should have a pool of nodes.
     * The nodes are allocated one time and assigned to freeOrders.
     * The other queues shares the nodes with freeOrders.
     * The ConcurrentLinkedQueue isn't from java.util.concurrent, it is a wrapper around that.
     */
    final MemC mNodes = MemC.alloc((maxQueue +2) * Entry._sizeof());
    this.freeOrders = new ConcurrentLinkedQueue<Entry>(mNodes);
    this.listOrders = new ConcurrentLinkedQueue<Entry>(this.freeOrders);

  }
  
  
  public void setMsgTextConverter(MsgText_ifc converter){
    msgText = converter;
  }
  
  
  
  /**Searches and returns the bits where a message is dispatch to.
   * The return value describes what to do with the message.
   * @param ident The message identificator
   * @return 0 if the message should not be dispatched, else some bits or number, see {@link #mDstMixedOutputs} etc.
   */
  public final int searchDispatchBits(int ident)
  { int bitDst;
    if(ident < 0)
    { /**a negative ident means: going state. The absolute value is to dispatch! */ 
      ident = -ident;
    }
    int idx = Arrays.binarySearch(listIdents, 0, actNrofListIdents, ident);
    if(idx < 0) idx = -idx -2;  //example: nr between idx=2 and 3 returns -4, converted to 2
    if(idx < 0) idx = 0;        //if nr before idx = 0, use properties of msg nr=0
    bitDst = listBitDst[idx];     
    return bitDst;
  }

  
  
  /**Sends a message. See interface.  
   * @param identNumber 
   * @param creationTime
   * @param text The identifier text @pjava2c=zeroTermString.
   * @param typeArgs Type chars, ZCBSIJFD for boolean, char, byte, short, int, long, float double. 
   *        @java2c=zeroTermString.
   * @param args see interface
   */
  public final boolean sendMsgVaList(int identNumber, final OS_TimeStamp creationTime, String text, final Va_list args)
  {
    // TODO Auto-generated method stub
    int dstBits = searchDispatchBits(identNumber);
    if(dstBits != 0)
    { final int dstBitsForDispatcherThread;
      if((dstBits & mDispatchInCallingThread) != 0)
      { /**dispatch in this calling thread: */
        dstBitsForDispatcherThread = dispatchMsg(dstBits, false, identNumber, creationTime, text, args);
      }
      else
      { /**No destinations are to use in calling thread. */
        dstBitsForDispatcherThread = dstBits;
      }
      //if((dstBits & mDispatchInDispatcherThread) != 0)
      if(dstBitsForDispatcherThread != 0)
      { /**store in queue, dispatch in a common thread of the message dispatcher:
         * To support realtime systems, all data are static. The list freeOrders contains entries,
         * get it from there and use it. No 'new' is necessary here. 
         */
        Entry entry = freeOrders.poll();  //get a new Entry from static data store
        if(entry == null)
        { /**queue overflow, no entries available. The message can't be displayed
           * This is the payment to won't use 'new'. A problem of overwork of data. 
           */
          //TODO test if it is a important message
          System.out.println("**************** NO ENTRIES");
        }
        else
        { /**write the informations to the entry, store it. */
          entry.dst = dstBitsForDispatcherThread;
          entry.ident = identNumber;
          entry.text = text;
          entry.timestamp.set(creationTime);
          entry.values.copyFrom(text, args);
          listOrders.offer(entry);
        }
      }
    }
    return true;
  }
  

  
  /**Dispatches a message. This routine is called either in the calling thread of the message
   * or in the dispatcher thread. 
   * @param dstBits Destination identificator. If the bit {@link mDispatchInDispatcherThread} is set,
   *        the dispatching should be done only for a destination if the destination is valid
   *        for the dispatcher thread. 
   *        Elsewhere if the bit is 0, the dispatching should be done only for a destination 
   *        if the destination is valid for the calling thread.
   * @param bDispatchInDispatcherThread true if this method is called in dispatcher thread,
   *        false if called in calling thread. This param is compared with {@link Output#dstInDispatcherThread},
   *        only if it is equal with them, the message is outputted.
   * @param identNumber identification of the message.
   * @param creationTime
   * @param text The identifier text @pjava2c=zeroTermString.
   * @param args @pjava2c=nonPersistent.
   * @return 0 if all destinations are processed, elsewhere dstBits with bits of non-processed dst.
   */
  protected final int dispatchMsg(int dstBits, boolean bDispatchInDispatcherThread
      , int identNumber, final OS_TimeStamp creationTime, String text, final Va_list args)
  { //final boolean bDispatchInDispatcherThread = (dstBits & mDispatchInDispatcherThread)!=0;
    //assert, that dstBits is positive, because >>=1 and 0-test fails elsewhere.
    //The highest Bit has an extra meaning, also extract above.
    dstBits &= mDispatchBits;  
    int bitTest = 0x1;
    int idst = 0;
    if(msgText !=null){
      String sTextCfg = msgText.getMsgText(identNumber);
      if(sTextCfg !=null){
        text = sTextCfg;   //replace the input text if a new one is found.
      }
    }
    while(dstBits != 0 && bitTest < mDispatchBits) //abort if no bits are set anymore.
    { if(  (dstBits & bitTest)!=0 
        && ( ( outputs[idst].dstInDispatcherThread &&  bDispatchInDispatcherThread)  //dispatch in the requested thread
           ||(!outputs[idst].dstInDispatcherThread && !bDispatchInDispatcherThread)
           )
        )
      { LogMessage out = outputs[idst].outputIfc;
        if(out != null)
        { boolean sent = out.sendMsgVaList(identNumber, creationTime, text, args);
          if(sent)
          { dstBits &= ~bitTest; //if sent, reset the associated bit.
          }
        }
        else
        { dstBits &= ~bitTest; //reset the associated bit, send isn't possible
          testCnt.noOutput +=1;
        }
      }
      bitTest <<=1;
      idst += 1;
    }
    return dstBits;
  }


  
  
  


}
