package com.gentics.contentnode.rest.model.response.migration;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A migration job entry contains information about an executed migration job. Infomation like the job status and the count of objects that have been handled are stored
 * in MigrationJobEntry objects.
 * 
 * @author johannes2
 * 
 */
public class MigrationJobEntry implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5199098960227659809L;

	private int jobId;
	private int status;
	private int jobType;
	private String timestamp;
	private String logName;
	private long handledObjects;

	@JsonIgnore
	String config;

	public MigrationJobEntry() {}

	/**
	 * Creates a new job log entry object
	 * 
	 * @param jobId
	 * @param jobType
	 * @param status
	 * @param timestamp
	 * @param config
	 */
	public MigrationJobEntry(int jobId, int jobType, int status, String timestamp, String config, String logName, long handledObjects) {
		this.jobId = jobId;
		this.jobType = jobType;
		this.status = status;
		this.timestamp = timestamp;
		this.config = config;
		this.logName = logName;
		this.handledObjects = handledObjects;
	}

	/**
	 * Returns how many objects have been handled by the migration job
	 * 
	 * @return
	 */
	public long getHandledObjects() {
		return handledObjects;
	}

	/**
	 * Sets the number of objects that have been handled by the migration job
	 * 
	 * @param handledObjects
	 */
	public void setHandledObjects(long handledObjects) {
		this.handledObjects = handledObjects;
	}

	/**
	 * Returns the log filename for the migration job
	 * 
	 * @return
	 */
	public String getLogName() {
		return logName;
	}

	/**
	 * Sets the name of the migration log
	 * 
	 * @param logName
	 */
	public void setLogName(String logName) {
		this.logName = logName;
	}

	public int getJobId() {
		return jobId;
	}

	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	/**
	 * Returns the status of the executed migration job
	 * 
	 * @return
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the status of the migration job
	 * 
	 * @param status
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Returns the timestamp for this job entry
	 * 
	 * @return
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets the timestamp for this job entry
	 * 
	 * @param timestamp
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Returns the job type. The job type is used to distinguished between a tag type migration and a template type migration
	 * 
	 * @return
	 */
	public int getJobType() {
		return jobType;
	}

	/**
	 * Sets the job type id.
	 * 
	 * @param jobType
	 */
	public void setJobType(int jobType) {
		this.jobType = jobType;
	}

	/**
	 * Returns the configuration in form of a serialized json string for the migration job
	 * 
	 * @return
	 */
	@JsonIgnore
	public String getConfig() {
		return config;
	}

	/**
	 * Sets the migration json string that represents the migration configuration
	 * 
	 * @param config
	 */
	@JsonIgnore
	public void setConfig(String config) {
		this.config = config;
	}

}
