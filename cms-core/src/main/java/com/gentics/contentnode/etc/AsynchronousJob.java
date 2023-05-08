/*
 * @author jan
 * @date Aug 29, 2008
 * @version $Id: AsynchronousJob.java,v 1.1 2008-10-16 15:09:58 jan Exp $
 */
package com.gentics.contentnode.etc;

import com.gentics.contentnode.render.RenderResult;

/**
 * Interface for asynchronous jobs, that are processed in instances of the {@link AsynchronousWorker}.
 */
public interface AsynchronousJob {

	/**
	 * Process the job.
	 * @param renderResult render result that can be used to logging (e.g. for JobGroups)
	 * @return number work units, this job was worth. Should be 1 for normal jobs or the number of grouped jobs for JobGroups.
	 * @throws Exception
	 */
	public int process(RenderResult renderResult) throws Exception;

	/**
	 * Get the job description, which is used for logging
	 * @return job description
	 */
	public String getDescription();

	/**
	 * Return true, if the job shall be logged (and counted for the statistics), false if not
	 * @return true for logged jobs, false for 'dummy' jobs
	 */
	public boolean isLogged();
}
