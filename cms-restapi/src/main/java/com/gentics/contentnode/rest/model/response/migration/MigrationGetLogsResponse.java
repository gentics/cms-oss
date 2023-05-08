package com.gentics.contentnode.rest.model.response.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response to request to retrieve all logs for tag type migration
 * 
 * @author Taylor
 * 
 */
@XmlRootElement
public class MigrationGetLogsResponse extends GenericResponse {

	/**
	 * List of log filenames
	 */
	private Map<String, String> logFilenames = new HashMap<String, String>();

	/**
	 * List of job entries
	 */
	private List<MigrationJobEntry> jobEntries = new ArrayList<MigrationJobEntry>();
	
	/**
	 * Constructor used by JAXB
	 */
	public MigrationGetLogsResponse() {}

	/**
	 * Create an instance of the response with single message and response info
	 * 
	 * @param message
	 *            message
	 * @param responseInfo
	 *            response info
	 */
	public MigrationGetLogsResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	public Map<String, String> getLogFilenames() {
		return logFilenames;
	}

	/**
	 * Sets the map of log filenames
	 * @param logFilenames
	 */
	public void setLogFilenames(Map<String, String> logFilenames) {
		this.logFilenames = logFilenames;
	}

	/**
	 * Returns the list of job migration job entries
	 * @return
	 */
	public List<MigrationJobEntry> getJobEntries() {
		return jobEntries;
	}

	/**
	 * Sets the list of migration job entries fot his reponse.
	 * @param jobEntries
	 */
	public void setJobEntries(List<MigrationJobEntry> jobEntries) {
		this.jobEntries = jobEntries;
	}

}
