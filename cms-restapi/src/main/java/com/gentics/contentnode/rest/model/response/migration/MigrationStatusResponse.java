package com.gentics.contentnode.rest.model.response.migration;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response to request to check status of tag type migration
 * 
 * @author Taylor
 * 
 */
@XmlRootElement
public class MigrationStatusResponse extends GenericResponse {
	/**
	 * Serial Version UI
	 */
	private static final long serialVersionUID = 6099693253726411221L;

	/**
	 * The lastest job information
	 */
	private MigrationJobEntry latestJob;

	/**
	 * The percent of the migration job that is complete
	 */
	private Integer percentComplete;

	/**
	 * Status of the tag type migration job
	 */
	private int status;

	/**
	 * ID of the tag type migration job
	 */
	private int jobId;

	/**
	 * Constructor used by JAXB
	 */
	public MigrationStatusResponse() {}

	/**
	 * Create an instance of the response with single message and response info
	 * 
	 * @param message
	 *            message
	 * @param responseInfo
	 *            response info
	 */
	public MigrationStatusResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Returns the completion status for the job that is currently being executed
	 * 
	 * @return
	 */
	public Integer getPercentComplete() {
		return percentComplete;
	}

	/**
	 * Set the progress completion information for this reponse
	 * 
	 * @param percentComplete
	 */
	public void setPercentComplete(Integer percentComplete) {
		this.percentComplete = percentComplete;
	}

	/**
	 * Returns the status for the job that is currently being executed
	 * 
	 * @return
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the status for this response
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Returns the jobId for the job which is currently being executed
	 * 
	 * @return
	 */
	public int getJobId() {
		return jobId;
	}

	/**
	 * Sets the job id for this response
	 * 
	 * @param jobId
	 */
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	/**
	 * Returns the lastest job information
	 * 
	 * @return
	 */
	public MigrationJobEntry getLatestJob() {
		return this.latestJob;
	}

	/**
	 * Sets the lastest job information for this response.
	 * 
	 * @param latestJob
	 */
	public void setLatestJob(MigrationJobEntry latestJob) {
		this.latestJob = latestJob;

	}
}
