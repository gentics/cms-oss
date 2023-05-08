/*
 * @author johannes2
 * @date 16.10.2008
 * @version $Id: IPublishThreadInfo.java,v 1.2 2010-01-11 11:04:33 norbert Exp $
 */
package com.gentics.contentnode.publish;

/**
 * Interface that contains methods to access specific information about publisher threads
 * Currently NOT used.
 * @author johannes2
 */
public interface IPublishThreadInfo {

	/**
	 * Returns a specific thread id to identify this thread info
	 * @return
	 */
	String getThreadID();

	/**
	 * Returns the amount of pages that were processed by this thread
	 * @return
	 */
	long getNPagesDone();

	/**
	 * Returns the time that this thread has spend with waiting
	 * @return
	 */
	long getWaitingTime();

	/**
	 * Returns the time that this thread has spend with working
	 * @return
	 */
	long getWorkingTime();

	/**
	 * Returns the time hat this thread has spend with waiting for the
	 * (readable) db connection
	 * @return
	 */
	long getWaitingDbTime();

	/**
	 * Returns the current cpu usage of this thread
	 * @return current cpu usage in percent eg. 0.52f
	 */
	float getCurrentCpuUsage();
}
