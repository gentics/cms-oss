package com.gentics.contentnode.jmx;

/**
 * Interface for PublishWorkerInfo Management Bean
 */
public interface PublishWorkerInfoMBean {

	/**
	 * Get the current page ID
	 * @return current page ID
	 */
	String getCurrentPageId();

	/**
	 * Get the current duration
	 * @return current duration
	 */
	long getCurrentDuration();

	/**
	 * Get the average duration
	 * @return average duration
	 */
	long getAverageDuration();

	/**
	 * Get the number of pages done
	 * @return number of pages done
	 */
	long getPagesDone();
}
