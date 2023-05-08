package com.gentics.contentnode.jmx;

/**
 * Management Bean interface for publish handler information
 */
public interface PublishHandlerInfoMBean {

	/**
	 * Get the name
	 * @return name
	 */
	String getName();

	/**
	 * Get the implementation class
	 * @return implementation class
	 */
	String getJavaClass();

	/**
	 * Get number of created objects
	 * @return number of created objects
	 */
	long getCreated();

	/**
	 * Get number of updated objects
	 * @return number of updated objects
	 */
	long getUpdated();

	/**
	 * Get number of deleted objects
	 * @return number of deleted objects
	 */
	long getDeleted();

	/**
	 * Average time for create calls
	 * @return average time for create calls
	 */
	long getAvgCreateTime();

	/**
	 * Average time for update calls
	 * @return average time for update calls
	 */
	long getAvgUpdateTime();

	/**
	 * Average time for delete calls
	 * @return average time for delete calls
	 */
	long getAvgDeleteTime();

	/**
	 * Get the current status
	 * @return current status
	 */
	String getStatus();
}
