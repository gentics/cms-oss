package com.gentics.contentnode.rest.model.response.migration;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response to request to retrieve log for a tag type migration
 * 
 * @author Taylor
 * 
 */
@XmlRootElement
public class MigrationGetLogResponse extends GenericResponse {

	/**
	 * Contents of log file
	 */
	private String logContents;
	
	/**
	 * Constructor used by JAXB
	 */
	public MigrationGetLogResponse() {}

	/**
	 * Create an instance of the response with single message and response info
	 * 
	 * @param message
	 *            message
	 * @param responseInfo
	 *            response info
	 */
	public MigrationGetLogResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Returns the log file content
	 * @return
	 */
	public String getLogContents() {
		return logContents;
	}

	/**
	 * Set the contents of the log
	 * @param logContents
	 */
	public void setLogContents(String logContents) {
		this.logContents = logContents;
	}

}
