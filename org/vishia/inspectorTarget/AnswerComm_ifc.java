package org.vishia.inspectorTarget;

public interface AnswerComm_ifc
{
	/**Sends an answer. 
	 * @param nrofAnswerBytesPart Number of bytes of a part, which is prepared new 
	 *                            and not returned from executeMonitorCmd_CmdConsumer_ifcInspc(...) yet.
	 *                            This parameter is meanfull if a telegram will be sent 
	 *                            while executeMonitorCmd_CmdConsumer_ifcInspc(...) is running.
	 *                            If the last telegramm will be sent, it should be 0.
	 * @param bLastTelg true than the bit for last telg in the sequence counter is set.
	 *                  false than the last-telg-bit isn't set and a new telg will be prepared.
	 * @return The yet maximal number of answer bytes.
	 *         <0 than an error occur. the preparation of data should be aborted.
	 */ 
	int txAnswer(int nrofAnswerBytesPart, boolean bLastTelg);
	
}
