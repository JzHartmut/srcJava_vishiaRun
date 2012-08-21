package org.vishia.msgDispatch;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.mainCmd.Report;
import org.vishia.msgDispatch.LogMessage;

/**This class receives the messages from the target device, dispatch it per ident and writes it 
 * in the actual list and in some files.
 * The message is dispatched using {@link MsgDispatcher}.
 * 
 * @author Hartmut Schorrig
 *
 */
public class MsgReceiver 
{

  /**Version, history and license.
   * <ul>
   * <li>2010-06-00 Hartmut created
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
   * If you are indent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  final static int version = 20120822;

  
  final MsgConfig msgConfig;
  
  
  private class MsgItem
  {
    String time;
    int ident;
    char state;
    String text;
  }
  
  private final Report console;
  
  /**true if the messages will be displayed. Only then it will be got from receiver. */
  private boolean bActivated = false;
  
  private boolean bListToBottom = false;
  
  List<MsgItem> msgOfDay = new LinkedList<MsgItem>();
  
  MsgRecvComm comm = new MsgRecvComm();
  
  /**The instance for dispatching the messages. */
  private final LogMessage msgDispatcher;
  
  final MsgItems_h.MsgItems recvData;
  
  final byte[] recvDataBuffer;
  
  final int[] nrofBytesReceived = new int[1];
  
  MsgItems_h.MsgItem msgItem = new MsgItems_h.MsgItem(); 
  
  //JScrollPane guiNotify;
  //Component table;

  
  
  public MsgReceiver(Report console, LogMessage msgDispatcher, MsgConfig msgConfig)
  { this.console = console;
    this.msgConfig = msgConfig;
    comm.open(null, false);
    recvData = new MsgItems_h.MsgItems();
    recvDataBuffer = new byte[MsgItems_h.MsgItems.kIdxAfterLast];
    recvData.assignEmpty(recvDataBuffer);
    recvData.setBigEndian(true);
  
    this.msgDispatcher = msgDispatcher;
    //check all entries in the configuration to configure the MsgDispatcher:
  }
  
  
  
  
  
  
  /**Now start work. */
  public void start(){
    bActivated = true;
  }
  
  
  
  private void storeMsgOfDay(long absTime, int ident, Object... values)
  {
 /*
    MsgItem msgItem = new MsgItem();
    Date date = new Date(absTime);
    msgItem.time = dateFormat.format(date);
    msgItem.ident = ident < 0 ? -ident : ident;
    //The configuration for this msg ident.
    */
    //String formatText  = msgConfig.getMsgText(ident);
    //if(formatText == null){
      //no config for the ident found.
      String formatText = "unknown message";
    //}
    /*
    try{ msgItem.text = String.format(localization, formatText,values);
    
    } catch(IllegalFormatConversionException exc){
      msgItem.text = "error in text format: " + formatText;
    }catch(IllegalFormatPrecisionException exc){
      msgItem.text = "error-precision in text format: " + formatText;
    }
    msgItem.state = ident<0? '-' : '+';  //going/coming
    msgOfDay.add(msgItem);
   */
    //String sInfoLine = msgItem.time + '\t' + msgItem.ident + '\t' + msgItem.state + '\t' + msgItem.text;
    //guiAccess.insertInfo("msgOfDay", Integer.MAX_VALUE, sInfoLine);
    while(msgOfDay.size() > 200){
      msgOfDay.remove(0);
    }
    msgDispatcher.sendMsgTime(ident, new OS_TimeStamp(absTime), formatText, values);
    
    
  }
  
  
/*
  void OldstoreMsgOfDay(long absTime, int ident, Object... values)
  {
    MsgItem msgItem = new MsgItem();
    Date date = new Date(absTime);
    msgItem.time = dateFormat.format(date);
    msgItem.ident = ident < 0 ? -ident : ident;
    //The configuration for this msg ident.
    MsgConfig.MsgConfigItem cfgItem = msgConfig.indexIdentNr.get(msgItem.ident);
    String formatText;
    
    if(cfgItem != null){
      formatText = cfgItem.text; //msgConfig"Format %f %d";
    } else {
      //no config for the ident found.
      formatText = "unknown message";
    }
    try{ msgItem.text = String.format(localization, formatText,values);
    
    } catch(IllegalFormatConversionException exc){
      msgItem.text = "error in text format: " + formatText;
    }catch(IllegalFormatPrecisionException exc){
      msgItem.text = "error-precision in text format: " + formatText;
    }
    msgItem.state = ident<0? '-' : '+';  //going/coming
    msgOfDay.add(msgItem);
    //String sInfoLine = msgItem.time + '\t' + msgItem.ident + '\t' + msgItem.state + '\t' + msgItem.text;
    //guiAccess.insertInfo("msgOfDay", Integer.MAX_VALUE, sInfoLine);
    while(msgOfDay.size() > 200){
      msgOfDay.remove(0);
    }
    msgDispatcher.sendMsgTime(ident, new OS_TimeStamp(absTime), formatText, values);
    
    
  }
  */
  
  void test()
  { Date currTime = new Date();
    long currMillisec = currTime.getTime();
    storeMsgOfDay(currMillisec, 1, 3.4, 34);
  }
  
  
  /**This method should be called in a applications thread cyclically. It tests whether new datagram with messages is received.
   * Note: The method does not block, because the {@link #comm}.{@link org.vishia.communication.InterProcessComm#open(org.vishia.communication.Address_InterProcessComm, boolean)} 
   * should be called for non blocking mode!
   * 
   */
  public void testAndReceive()
  {
    if(bActivated){
      //fileOutput.flush();
      comm.receiveData(nrofBytesReceived, recvDataBuffer, null);
      if(nrofBytesReceived[0] > 0){
        if(nrofBytesReceived[0] < MsgItems_h.MsgItems.kIdxAfterLast){
          console.writeError("msgReceiver: to less bytes: " + nrofBytesReceived[0]);
        } else {
          int nrofMsg = recvData.get_nrofMsg();
          try{
            for(int ii = 0; ii < nrofMsg; ii++){
              msgItem.assignAtIndex(MsgItems_h.MsgItems.kIdxmsgItems
                  + ii * MsgItems_h.MsgItem.kIdxAfterLast                 
                  , recvData);
              int timestamp = msgItem.get_timestamp();  //UDT
              short timeMillisec = msgItem.get_timeMillisec();
              short mode_typeVal = msgItem.get_mode_typeVal();
              long timeMillisecUTC = (long)(timestamp)* 1000 + timeMillisec;
              
              int ident = msgItem.get_ident();
              int value1 = msgItem.get_values(0);  //maybe float image
              int value2 = msgItem.get_values(1);  //maybe float image
              int value3 = msgItem.get_values(2);  //maybe float image
              int value4 = msgItem.get_values(3);  //maybe float image
    
              Object[] values = new Object[4];
              short typeValue = mode_typeVal;
              for(int ixV=0; ixV < values.length; ++ixV){
                int value = msgItem.get_values(ixV);
                switch(typeValue & 3){
                case 0: values[ixV] = new Integer(value); break;
                case 1:
                case 2:
                case 3: values[ixV] = new Float(Float.intBitsToFloat(value)); break;
                }
                typeValue >>=2;
              }
              
              storeMsgOfDay(timeMillisecUTC, ident, values);

              bListToBottom = true;              
              
            }
          } catch(IllegalArgumentException exc){ throw new RuntimeException("unexpected"); }
        }
      }
    }
  }
  
  
  

  
}
