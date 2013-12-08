package org.vishia.inspector;

import java.io.UnsupportedEncodingException;

import org.vishia.communication.InspcDataExchangeAccess;
import org.vishia.communication.InterProcessComm;
import org.vishia.util.Java4C;

public class CmdExecuter implements AnswerComm_ifc
{

	/**@ java2c=simpleRef. */
	final InspcDataExchangeAccess.ReflDatagram datagramCmd = new InspcDataExchangeAccess.ReflDatagram(); 
	
	/**@ java2c=simpleRef. */
	final InspcDataExchangeAccess.Reflitem infoCmd = new InspcDataExchangeAccess.Reflitem();  
	
	/**@java2c=simpleRef. */
	private final CmdConsumer_ifc cmdConsumer;
	
	private final int maxNrofAnswerBytes = 1400;
  
  private int nrofBytesAnswer;
  
  private int nrofSentBytes; 
  
  private int ctFailedTelgPart;
  
  /**@java2c=simpleRef. */
	private Comm comm;
  
  private final byte[] bufferAnswerData = new byte[1500]; 
  
  private final InspcDataExchangeAccess.ReflDatagram myAnswerData = new InspcDataExchangeAccess.ReflDatagram(bufferAnswerData); 
  
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


	boolean executeCmd(byte[] buffer, int nrofBytesReceived) 
	{
  	datagramCmd.assignData(buffer, nrofBytesReceived);
    int nEntrant = datagramCmd.getEntrant();
    boolean bOk = true;
    int nrofBytesProcessed;
    int nrofBytesTelg;
    int partLength;
    int maxNrofBytesAnswerPart;
    int nrofBytesAnswerPart;
    nrofBytesAnswer = 0;
    /**@java2c=dynamic-call. */
  	@Java4C.DynamicCall final CmdConsumer_ifc cmdConsumerMtbl = cmdConsumer;
  	myAnswerData.removeChildren();
    //String test = myAnswerData.toString();
    if(nEntrant < 0){
      //a negative number: It is an entrant, the telegram has the common head.
      nrofBytesTelg = datagramCmd.getLengthDatagram();
      nrofBytesProcessed = datagramCmd.sizeofHead;
      useTelgHead = true;
      //
      //prepare the answer telg:
      int seqNr = datagramCmd.getSeqnr();
      int encryption = datagramCmd.getEncryption();
      myAnswerData.setHeadAnswer(nEntrant, seqNr, encryption);
      nrofBytesAnswer = InspcDataExchangeAccess.ReflDatagram.sizeofHead;
      while(bOk && nrofBytesTelg >= (nrofBytesProcessed + InspcDataExchangeAccess.Reflitem.sizeofHead)){
        //The next telg Part will be found after the processed part.
      	datagramCmd.addChild(infoCmd);
      	partLength = infoCmd.getLenInfo();
      	if(  partLength >= InspcDataExchangeAccess.Reflitem.sizeofHead
      		&& partLength <= (nrofBytesTelg - nrofBytesProcessed)){
      	  //valid head data.
          boolean lastPart = (nrofBytesProcessed + partLength) == nrofBytesTelg;
          maxNrofBytesAnswerPart = maxNrofAnswerBytes - nrofBytesAnswer;
          //execute:
          try{ 
          	nrofBytesAnswerPart = cmdConsumerMtbl.executeMonitorCmd(infoCmd, myAnswerData, maxNrofBytesAnswerPart);
          } catch(IllegalArgumentException exc){
          	nrofBytesAnswerPart =0; //TODO send a nack
          }catch(UnsupportedEncodingException exc){
          	nrofBytesAnswerPart =0; //TODO send a nack
          }
          
      	} else { //invalid head data
      	  bOk = false;
          ctFailedTelgPart +=1;
        
      	}
        nrofBytesProcessed += partLength;
      }
      int nrofAnswer = myAnswerData.getLengthTotal();
      if(nrofAnswer > InspcDataExchangeAccess.ReflDatagram.sizeofHead){
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
      infoCmd.assignData(buffer, nrofBytesReceived);
      infoCmd.setBigEndian(true);
      maxNrofBytesAnswerPart = 1400;
      try{ nrofBytesAnswerPart = cmdConsumerMtbl.executeMonitorCmd(infoCmd, myAnswerData, maxNrofBytesAnswerPart);
      } catch(IllegalArgumentException exc){
      	nrofBytesAnswerPart =0; //TODO send a nack
      }catch(UnsupportedEncodingException exc){
      	nrofBytesAnswerPart =0; //TODO send a nack
      }
      int nrofAnswer = myAnswerData.getLengthTotal();
      if(nrofAnswer > InspcDataExchangeAccess.ReflDatagram.sizeofHead){
      	//more as the head:
      	txAnswer(nrofAnswer, true);
      }
    }
    return bOk;
  }

	
  /**Send the current answer datagram as answer.
   * The length of the datagram is set to the head using {@link InspcDataExchangeAccess.ReflDatagram#setLengthDatagram(int)}
   * 
   * @see org.vishia.inspector.AnswerComm_ifc#txAnswer(int, boolean)
   */
  @Override public int txAnswer(int nrofAnswerBytesPart, boolean bLastTelg)
	//int txAnswer_AnswerComm_Inspc(AnswerComm_Inspc* ythis, int nrofAnswerBytesPart, bool bLastTelg) 
	{
	  int ret;
	  if(useTelgHead){
	  	myAnswerData.setLengthDatagram(nrofAnswerBytesPart);
	  }
	  //ythis->answer.nrofSentBytes = txAnswerRawData_Comm_Inspc(ythis, &ythis->answer.myAnswerData, ythis->answer.nrofAnswerBytes, &ythis->myAnswerAddress);
		nrofBytesAnswer = nrofAnswerBytesPart;
	  if(  !useTelgHead                                 //the older form without head
	    || nrofBytesAnswer > InspcDataExchangeAccess.ReflDatagram.sizeofHead  //more data as the head only.
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
				myAnswerData.incrAnswerNr();
				nrofBytesAnswer = InspcDataExchangeAccess.ReflDatagram.sizeofHead;
        ret = InspcDataExchangeAccess.ReflDatagram.sizeofHead - nrofBytesAnswer;
			}
	    
	  } else {
	  	//nothing to sent.
	    ret = InspcDataExchangeAccess.ReflDatagram.sizeofHead - nrofBytesAnswer;
		}
		return ret;
	}
	
	
	
}
