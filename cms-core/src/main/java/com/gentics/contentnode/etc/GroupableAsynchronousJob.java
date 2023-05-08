package com.gentics.contentnode.etc;


/**
 * Interface for groupable asynchronous jobs
 */
public interface GroupableAsynchronousJob extends AsynchronousJob {
	/**
	 * Get a JobGroup instance, if this job is groupable
	 * @param <T>
	 * @return JobGroup instance, if this job is groupble, null if not
	 */
	public <T extends GroupableAsynchronousJob> JobGroup<T> getGroup();
}
