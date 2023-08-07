package org.vishia.inspectorTarget;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.util.Java4C;

public class CmdExecuter implements AnswerComm_ifc
{
  /**Version, history and license.
   * <ul>
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
  public static final String version = "2015-08-05";


	/**@ java2c=simpleRef. */
	final InspcDataExchangeAccess.InspcDatagram datagramCmd = new InspcDataExchangeAccess.InspcDatagram(); 
	
	/**@ java2c=simpleRef. */
	final InspcDataExchangeAccess.Inspcitem infoCmd = new InspcDataExchangeAccess.Inspcitem();  
	
	/**@java2c=simpleRef. */
	private final CmdConsumer_ifc cmdConsumer;
	
	private final int maxNrofAnswerBytes = 1400;
  
  private int nrofBytesAnswer;
  
  private int nrofSentBytes; 
  
  private int ctFailedTelgPart;
  
  /**@java2c=simpleRef. */
	private Comm comm;
  
  /**Buffer for the answer telegram. It should be less then the max length of an UDP telegram.
   * 
   */
  @Java4C.SimpleArray
  private final byte[] data_bufferAnswerData = new byte[1400]; 
  
  /**This reference is used to refer the answer buffer. It is for C usage with the PtrVal type which contains the address and the size
   * in one struct. */
  @Java4C.PtrVal
  private final byte[] bufferAnswerData = data_bufferAnswerData; 
  
  
  
  private final InspcDataExchangeAccess.InspcDatagram myAnswerData = new InspcDataExchangeAccess.InspcDatagram(bufferAnswerData); 
  
  /**true than the myAnswerdata is of type DataExchangeTelg_Inspc, 
   * false: older form: without head.
   */
  private boolean useTelgHead;
  
	
	
  public CmdExecuter(CmdConsumer_ifc commandConsumer){
    this.cmdConsumer = commandConsumer;
  }
  
  public void completeConstruction(Comm comm)
  {
  	this.comm = comm;	
  }


  /**Executes the given command received with this datagram
   * @param buffer contains the datagram
   * @param nrofBytesReceived
   * @return true if ok, false if nok
   */
  boolean executeCmd(@Java4C.PtrVal byte[] buffer, int nrofBytesReceived) 
  {
  	datagramCmd.assignDatagram(buffer, nrofBytesReceived);
    int nEntrant = datagramCmd.getEntrant();
    boolean bOk = true;
    //int nrofBytesProcessed;
    int nrofBytesTelg;
    int partLength;
    int maxNrofBytesAnswerPart;
    nrofBytesAnswer = 0;
    /**@java2c=dynamic-call. */
  	@Java4C.DynamicCall final CmdConsumer_ifc cmdConsumerMtbl = cmdConsumer;
  	myAnswerData.removeChildren();
  	Arrays.fill(bufferAnswerData, 0, bufferAnswerData.length, (byte)0);
    //String test = myAnswerData.toString();
    if(nEntrant < 0){
      //a negative number: It is an entrant, the telegram has the common head.
      nrofBytesTelg = datagramCmd.getLengthDatagram();
      int nrofBytesAccess = datagramCmd.getLengthTotal();
      assert(nrofBytesTelg == nrofBytesReceived);
      assert(nrofBytesTelg == nrofBytesAccess);
      //nrofBytesProcessed = datagramCmd.sizeofHead;
      useTelgHead = true;
      //
      //prepare the answer telg:
      int seqNr = datagramCmd.getSeqnr();
      int encryption = datagramCmd.getEncryption();
      myAnswerData.setHeadAnswer(nEntrant, seqNr, encryption);
      nrofBytesAnswer = InspcDataExchangeAccess.InspcDatagram.sizeofHead;
      while(bOk && datagramCmd.sufficingBytesForNextChild(InspcDataExchangeAccess.Inspcitem.sizeofHead) ) { //nrofBytesTelg >= (nrofBytesProcessed + InspcDataExchangeAccess.Inspcitem.sizeofHead)){
        //The next telg Part will be found after the processed part.
      	datagramCmd.addChild(infoCmd);
      	partLength = infoCmd.getLenInfo();
      	if(  partLength >= InspcDataExchangeAccess.Inspcitem.sizeofHead
      		&& infoCmd.checkLengthElement(partLength)) { //partLength <= (nrofBytesTelg - nrofBytesProcessed)){
      	  //valid head data.
          infoCmd.setLengthElement(partLength);  //this child has the given length.
          //boolean lastPart = (nrofBytesProcessed + partLength) == nrofBytesTelg;
          maxNrofBytesAnswerPart = maxNrofAnswerBytes - nrofBytesAnswer;
          //execute:
          try{ 
          	cmdConsumerMtbl.executeMonitorCmd(infoCmd, myAnswerData, maxNrofBytesAnswerPart);
          } catch(IllegalArgumentException exc){
          	System.err.println("CmdExecuter - Exception");
            //TODO send a nack
          }catch(UnsupportedEncodingException exc){
            System.err.println("CmdExecuter - Exception2");
            //TODO send a nack
          }
          
      	} else { //invalid head data
      	  bOk = false;
          ctFailedTelgPart +=1;
        
      	}
        //nrofBytesProcessed += partLength;
      }
      int nrofAnswer = myAnswerData.getLengthTotal();
      if(nrofAnswer > InspcDataExchangeAccess.InspcDatagram.sizeofHead){
      	//more as the head:
      	txAnswer(nrofAnswer, true);
      }

    } else {
      //a positive number: The telegram hasn't the commmon head ,,DataExchangeTelgHead_Inspc,,, it is one command.
      //It is the old style of communication, exclusively used until 2010-0216.
      useTelgHead = false;
      //dummy head with 2 empty information units.
      myAnswerData.setHeadAnswer(0, 0x080000, 0);
      myAnswerData.setLengthDatagram(8);
      infoCmd.assign(buffer, nrofBytesReceived);
      infoCmd.setBigEndian(true);
      maxNrofBytesAnswerPart = 1400;
      try{ cmdConsumerMtbl.executeMonitorCmd(infoCmd, myAnswerData, maxNrofBytesAnswerPart);
      } catch(IllegalArgumentException exc){
      	//TODO send a nack
      }catch(UnsupportedEncodingException exc){
      	//TODO send a nack
      }
      int nrofAnswer = myAnswerData.getLengthTotal();
      if(nrofAnswer > InspcDataExchangeAccess.InspcDatagram.sizeofHead){
      	//more as the head:
      	txAnswer(nrofAnswer, true);
      }
    }
    return bOk;
  }

	
  /**Send the current answer datagram as answer. Firstly the {@link InspcDataExchangeAccess.InspcDatagram#incrAnswerNr()}
   * was invoked, therewith an answer starts with 1. That increment is important for more as one answer datagrams. 
   * The head is initialized only one time with the data from the request telegram, the answerNr is incremented always. 
   * The length of the datagram is set to the head using {@link InspcDataExchangeAccess.InspcDatagram#setLengthDatagram(int)}
   * 
   * @see org.vishia.inspectorTarget.AnswerComm_ifc#txAnswer(int, boolean)
   */
  @Override public int txAnswer(int nrofAnswerBytesPart, boolean bLastTelg)
	//int txAnswer_AnswerComm_Inspc(AnswerComm_Inspc* ythis, int nrofAnswerBytesPart, bool bLastTelg) 
	{
	  int ret;
	  if(useTelgHead){
      myAnswerData.incrAnswerNr();  //start answer from 1
	  	myAnswerData.setLengthDatagram(nrofAnswerBytesPart);
	  }
	  //ythis->answer.nrofSentBytes = txAnswerRawData_Comm_Inspc(ythis, &ythis->answer.myAnswerData, ythis->answer.nrofAnswerBytes, &ythis->myAnswerAddress);
		nrofBytesAnswer = nrofAnswerBytesPart;
	  if(  !useTelgHead                                 //the older form without head
	    || nrofBytesAnswer > InspcDataExchangeAccess.InspcDatagram.sizeofHead  //more data as the head only.
	    ){
	    if(bLastTelg && useTelgHead) { 
	    	myAnswerData.markAnswerNrLast(); //mark as last telg
	    }
	    //send.
	    comm.sendAnswer(bufferAnswerData, nrofBytesAnswer);
    	//
	    if(bLastTelg){
				ret = 0;
			} else {
				//prepare the next telg:
				nrofBytesAnswer = InspcDataExchangeAccess.InspcDatagram.sizeofHead;
        ret = InspcDataExchangeAccess.InspcDatagram.sizeofHead - nrofBytesAnswer;
			}
	    
	  } else {
	  	//nothing to sent.
	    ret = InspcDataExchangeAccess.InspcDatagram.sizeofHead - nrofBytesAnswer;
		}
		return ret;
	}
	
	
	
}
