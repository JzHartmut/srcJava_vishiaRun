package org.vishia.inspector;

import java.io.UnsupportedEncodingException;

import org.vishia.communication.InspcDataExchangeAccess;

public interface CmdConsumer_ifc
{
	/**Executes a command, writes the answer in the answer datagram.
	 * <br><br>
	 * Rules vor answer:
	 * <br><br>
	 * The answer-Datagram is given with prepared head. The only one value is the current datagram length
	 * which should be set to the head before it is send. But this action is done in the method
	 * {@link AnswerComm_ifc#txAnswer(int, boolean)}. 
	 * The user should add its {@link InspcDataExchangeAccess.Inspcitem}-elements for the answer there.
	 * Only if the datagram is full and a next info have to be add, the current datagram should be send.
	 * Therefore the routine {@link AnswerComm_ifc#txAnswer(int, boolean)} should be called
	 * with the current number of bytes and <code>false</code> as 2. parameter. A non-filled datagram
	 * is not to be sent. It will be send after the last invocation of a command consumer from the
	 * activator of this method. In this way more as one answer can be given in one ore more datagrams,
	 * while the command datagram may contain more as one command too. 
	 * 
	 * @param cmd The command. It is one information unit only, not a full datagram, because the command
	 *            is containing in a information unit. A datagram may contain more as one command.
	 * @param answer answer datagram with prepared head, maybe with already content. 
	 *               The answer information units are to be add here. The datagram is to be send,
	 *               if it is full.
	 * @param maxNrofAnswerBytes The maximum of bytes inclusive head in the datagram.
	 * @return 0
	 */
	int executeMonitorCmd(InspcDataExchangeAccess.Inspcitem cmd, InspcDataExchangeAccess.InspcDatagram answer, int maxNrofAnswerBytes)
	throws IllegalArgumentException, UnsupportedEncodingException 
	;

	/**Sets the aggregation for the answer.
	 * @param answerComm
	 */
	void setAnswerComm(AnswerComm_ifc answerComm);
	
}
