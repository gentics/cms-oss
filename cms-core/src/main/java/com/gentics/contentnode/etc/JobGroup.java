package com.gentics.contentnode.etc;

import java.util.ArrayList;
import java.util.List;

import com.gentics.contentnode.publish.Publisher;
import com.gentics.contentnode.render.RenderResult;

/**
 * Abstract class for a job group. A job group is an instance of
 * {@link AsynchronousJob}, that groups other jobs and performs them (when
 * {@link #process(RenderResult)} is called on the group).
 * 
 * Implementations can implement {@link #preProcess(RenderResult)} and
 * {@link #postProcess(RenderResult)} to perform tasks before and/or after the
 * group of jobs is processed.
 * 
 * @param <T> Job implementation class
 */
public abstract class JobGroup<T extends GroupableAsynchronousJob> implements AsynchronousJob {
	/**
	 * List of jobs grouped in this job
	 */
	protected List<T> jobQueue = new ArrayList<T>();

	/**
	 * Create a new group for the given job. The group will already contain the job
	 * @param job job
	 */
	public JobGroup(T job) {
		addAsynchronousJob(job);
	}

	/**
	 * Add a job to the group
	 * @param job job
	 */
	public void addAsynchronousJob(T job) {
		synchronized (jobQueue) {
			jobQueue.add(job);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.AsynchronousJob#process()
	 */
	public int process(RenderResult renderResult) throws Exception {
		preProcess(renderResult);
		// process all grouped jobs
		int loggedJobs = 0;
		for (T job : jobQueue) {
			long start = System.currentTimeMillis();
			int workUnits = processJob(renderResult, job);
			long duration = System.currentTimeMillis() - start;

			if (job.isLogged()) {
				loggedJobs += workUnits;
				if (renderResult != null) {
					renderResult.info(Publisher.class, Thread.currentThread().getName() + " processed '" + job.getDescription() + "' in " + duration + " ms");
				}
			}
		}
		postProcess(renderResult);
		return loggedJobs;
	}

	/**
	 * Get the maximum number of jobs, that will be grouped together
	 * The default is 100, but this can be overwritten in specific JobGroup implementations
	 * @return maximum number of jobs
	 */
	protected int jobLimit() {
		return 100;
	}

	/**
	 * Check whether the group is full (and shall be processed now)
	 * @return true if the group is full
	 */
	public boolean isFull() {
		return jobQueue.size() >= jobLimit(); 
	}

	/**
	 * Get the size of the JobQueue
	 * @return size of the JobQueue
	 */
	public int getQueueSize() {
		return jobQueue.size();
	}

	/**
	 * Check whether the given job is groupable by this group
	 * @param job job
	 * @return true if the job is groupable, false if not
	 */
	public boolean isGroupable(GroupableAsynchronousJob job) {
		if (job == null) {
			return false;
		}
		try {
			@SuppressWarnings({ "unchecked", "unused" })
			T ownJob = (T)job;
			return true;
		} catch (ClassCastException e) {
			return false;
		}
	}

	/**
	 * Do pre-processing before processing the jobs in the group
	 * @param renderResult render result for logging (may be null)
	 * @throws Exception
	 */
	protected abstract void preProcess(RenderResult renderResult) throws Exception;

	/**
	 * Do post-processing after processing the jobs in the group
	 * @param renderResult render result for logging (may be null)
	 * @throws Exception
	 */
	protected abstract void postProcess(RenderResult renderResult) throws Exception;

	/**
	 * Process the given job
	 * The default implementation will just call {@link T#process(RenderResult)} for the job and return the result.
	 * @param renderResult render result
	 * @param job job to process
	 * @return number of work items, the processed job was worth
	 * @throws Exception
	 */
	protected int processJob(RenderResult renderResult, T job) throws Exception {
		return job.process(renderResult);
	}
}
