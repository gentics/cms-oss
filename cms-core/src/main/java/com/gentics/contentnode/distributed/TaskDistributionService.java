package com.gentics.contentnode.distributed;

import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Interface for implementations that support distribution of tasks (among different CMS instances)
 */
public interface TaskDistributionService {
	/**
	 * Check whether task execution is allowed on this CMS instance
	 * @return true if task execution is allowed, false if task execution is forbidden, null if it cannot be determined
	 */
	Boolean isTaskExecutionAllowed();

	/**
	 * Call the given task
	 * @param <T> type of the return value
	 * @param task task
	 * @param single true to run the task on a single instance only, false to run on all instances
	 * @return pair consisting of a flag, whether the task was run by this implementation and optionally the return value
	 * @throws InterruptedException
	 * @throws Exception
	 */
	<T> Pair<Boolean, T> call(Callable<T> task, boolean single) throws InterruptedException, Exception;

	/**
	 * Execute the given task on optionally available other instances
	 * @param task task
	 */
	void executeOther(Runnable task);
}
