package com.gentics.contentnode.rest.model.response.migration;

/**
 * Class that represents a migration job item within the database log
 * 
 * @author johannes2
 * 
 */
public class MigrationJobLogEntryItem {

	int jobId;
	int objectId;
	int objectType;
	int status;

	public MigrationJobLogEntryItem() {}

	/**
	 * Create a new job log entry item
	 * 
	 * @param jobId
	 * @param objectId
	 * @param objectType
	 * @param status
	 */
	public MigrationJobLogEntryItem(int jobId, int objectId, int objectType, int status) {
		this.jobId = jobId;
		this.objectId = objectId;
		this.objectType = objectType;
		this.status = status;
	}

	/**
	 * Returns the job id
	 * 
	 * @return
	 */
	public int getJobId() {
		return jobId;
	}

	/**
	 * Sets the job id
	 * 
	 * @param jobId
	 */
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	/**
	 * Returns the object id for this item
	 * 
	 * @return
	 */
	public int getObjectId() {
		return objectId;
	}

	/**
	 * Sets the object id for this item
	 * 
	 * @param objectId
	 */
	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}

	/**
	 * Returns the object type for this item. Generic Content.Node type values are being used. (e.g. 10007 for pages..)
	 * 
	 * @return
	 */
	public int getObjectType() {
		return objectType;
	}

	/**
	 * Sets the object type for this object item
	 * 
	 * @param objectType
	 */
	public void setObjectType(int objectType) {
		this.objectType = objectType;
	}

	/**
	 * Returns the migration status of this object item. This reflects how the object was handled within the migration job.
	 * 
	 * @return
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the migration status of this object item.
	 * 
	 * @param status
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return String.format("Job %d, Object %d.%d, Status %d", jobId, objectType, objectId, status);
	}
}
