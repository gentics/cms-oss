package com.gentics.contentnode.rest.model.response.migration;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * 
 * Response to requests for fetching jobitems. A MigrationJobItemsResponse object contains a list of objects that have been handled by the migration job.
 * 
 * @author johannes2
 * 
 */
@XmlRootElement
public class MigrationJobItemsResponse extends GenericResponse {

	private int jobId;

	/**
	 * Constructor used by JAXB
	 */
	public MigrationJobItemsResponse() {}

	/**
	 * Create an empty MigrationJobItemsResponse
	 * 
	 * @param message
	 * @param responseInfo
	 */
	public MigrationJobItemsResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * The job items
	 */
	private List<MigrationJobLogEntryItem> jobItems = new ArrayList<MigrationJobLogEntryItem>();

	public int getJobId() {
		return jobId;
	}

	/**
	 * Sets the job id for this job items reponse
	 * 
	 * @param jobId
	 */
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	/**
	 * Returns the list of {@link MigrationJobLogEntryItem}
	 * 
	 * @return
	 */
	public List<MigrationJobLogEntryItem> getJobItems() {
		return jobItems;
	}

	/**
	 * Sets the list of {@link MigrationJobLogEntryItem}
	 * 
	 * @param jobItems
	 */
	public void setJobItems(List<MigrationJobLogEntryItem> jobItems) {
		this.jobItems = jobItems;
	}

}
