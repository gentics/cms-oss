/*
 * @author jan
 * @date Oct 21, 2008
 * @version $Id: MulticonnectionTransaction.java,v 1.3 2008-12-09 12:00:52 jan Exp $
 */
package com.gentics.contentnode.factory;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.AsynchronousJob;
import com.gentics.contentnode.publish.PublishThreadInfo;

/**
 * Interface for multiconnection transaction (instances are used for multithreaded publishing)
 */
public interface MulticonnectionTransaction extends Transaction {

	/**
	 * Get the List of PublishThreadInfo objects of all threads where the PublishThread info was set
	 * (either explicitly with setPublishThreadInfo() or implicitly initialized by calling getPublishThreadInfo()).
	 * @return List of PublishThreadInfo objects or an empty list (but never null)
	 */
	public List<PublishThreadInfo> getPublishThreadInfos();

	/**
	 * Return the PublishThreadInfo (timing informations) of the current thread.
	 * If the PublishThreadInfo was not set for current thread a new PublishThreadInfo will be initialized and set for this object.
	 * @return
	 */
	public PublishThreadInfo getPublishThreadInfo();

	public void setPublishThreadInfo(PublishThreadInfo publishThreadInfo);

	/**
	 * Waits until all asynchronous jobs are finished
	 * @throws NodeException
	 */
	public void waitForAsynchronousJobs() throws NodeException;

	/**
	 * Adds a job to the asynchronous job queue. The job will not be executed immediately and has to wait in the queue.
	 * This function return instantly.    
	 * @param job
	 * @throws NodeException
	 */
	public void addAsynchronousJob(AsynchronousJob job) throws NodeException;
}
