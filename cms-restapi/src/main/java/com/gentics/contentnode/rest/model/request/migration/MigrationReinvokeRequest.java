package com.gentics.contentnode.rest.model.request.migration;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Tag Type Migration Reinvoke Request. This request can be used to reinvoke a migration job.
 * 
 * @author johannes2
 * 
 */
@XmlRootElement
public class MigrationReinvokeRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -632905949462479024L;

	/**
	 * The type of objects to be migrated
	 */
	private String type;

	/**
	 * The object id for the object that should be migrated again
	 */
	private Integer objectId;

	/**
	 * The job Id of the job that previously tried to migrate this object
	 */
	private Integer jobId;

	/**
	 * Default Constructor needed for jaxb
	 */
	public MigrationReinvokeRequest() {}

	/**
	 * Return the type of this migration request
	 * 
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the migration job type for this request
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the id of the object that should be re-invoked.
	 * 
	 * @return
	 */
	public Integer getObjectId() {
		return objectId;
	}

	/**
	 * Set the object id for the request
	 * 
	 * @param objectId
	 */
	public void setObjectId(Integer objectId) {
		this.objectId = objectId;
	}

	/**
	 * Returns the job id for this request
	 * 
	 * @return
	 */
	public Integer getJobId() {
		return jobId;
	}

	/**
	 * Set the job id that should be used for reinvocation
	 * 
	 * @param jobId
	 */
	public void setJobId(Integer jobId) {
		this.jobId = jobId;
	}

}
