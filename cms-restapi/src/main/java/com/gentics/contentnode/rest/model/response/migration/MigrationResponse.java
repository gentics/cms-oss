package com.gentics.contentnode.rest.model.response.migration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * The {@link MigrationResponse} is returned when a migration job is invoked or reinvoked. It contains the jobId and message information.
 * 
 * @author johannes2
 * 
 */
@XmlRootElement
public class MigrationResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1476069977488051411L;

	/**
	 * Id of the tag type migration job
	 */
	private int jobId;

	/**
	 * List of messages displayed to the user
	 */
	private List<Message> messages = new ArrayList<Message>();

	/**
	 * Response responseInfo for this response
	 */
	private ResponseInfo responseInfo;

	/**
	 * Constructor used by JAXB
	 */
	public MigrationResponse() {}

	/**
	 * Create an instance of the response with response info
	 * 
	 * @param responseInfo
	 *            response info
	 */
	public MigrationResponse(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
	}

	/**
	 * Create an instance of the response with a single message and response info
	 * 
	 * @param message
	 *            message
	 * @param responseInfo
	 *            response info
	 */
	public MigrationResponse(Message message, ResponseInfo responseInfo) {
		messages.add(message);
		this.responseInfo = responseInfo;
	}

	/**
	 * Create an instance of the response with a list of messages and response info
	 * 
	 * @param message
	 *            message
	 * @param responseInfo
	 *            response info
	 */
	public MigrationResponse(List<Message> messages, ResponseInfo responseInfo) {
		this.messages = messages;
		this.responseInfo = responseInfo;
	}

	/**
	 * Return the list of messages
	 */
	public List<Message> getMessages() {
		return messages;
	}

	/**
	 * Add a new message to the list of messages
	 */
	public void addMessage(Message message) {
		if (messages == null) {
			messages = new LinkedList<Message>();
		}
		messages.add(message);
	}

	/**
	 * Set the list of messages
	 */
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	/**
	 * Return the reponse info
	 */
	public ResponseInfo getResponseInfo() {
		return responseInfo;
	}

	/**
	 * Set the reponse info
	 */
	public void setResponseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
	}

	/**
	 * Returns the job id for this reponse
	 * 
	 * @return
	 */
	public int getJobId() {
		return jobId;
	}

	/**
	 * Set the job id for this reponse
	 * 
	 * @param jobId
	 */
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}
}
