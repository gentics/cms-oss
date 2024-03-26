package com.gentics.contentnode.distributed;

import java.util.concurrent.Callable;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.contentnode.etc.ServiceLoaderUtil;

/**
 * Utility class which helps in the task distribution
 */
public class DistributionUtil {
	/**
	 * Service loader
	 */
	protected static ServiceLoaderUtil<TaskDistributionService> taskDistributionServiceLoader = ServiceLoaderUtil
			.load(TaskDistributionService.class);

	/**
	 * Determine whether execution of a task, that should not be run in parallel is allowed
	 * @return true if execution is allowed
	 */
	public static boolean isTaskExecutionAllowed() {
		return StreamSupport.stream(taskDistributionServiceLoader.spliterator(), false)
				.map(TaskDistributionService::isTaskExecutionAllowed).filter(flag -> flag != null).findFirst().orElse(true);
	}

	/**
	 * Call the given task in a way that it is only run once (not in parallel)
	 * @param <T> type of the return value
	 * @param task task
	 * @return return value
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public static <T> T call(Callable<T> task) throws InterruptedException, Exception {
		return call(task, true);
	}

	/**
	 * Call the given task
	 * @param <T> type of the return value
	 * @param task task
	 * @param single true if task must not run in parallel, false it it should run on every member of a distributed system
	 * @return return value
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public static <T> T call(Callable<T> task, boolean single) throws InterruptedException, Exception {
		for (TaskDistributionService service : taskDistributionServiceLoader) {
			Pair<Boolean, T> resultPair = service.call(task, single);
			if (resultPair.getLeft()) {
				return resultPair.getRight();
			}
		}

		return task.call();
	}

	/**
	 * Execute the given task on other members of a distributed system
	 * @param task task
	 */
	public static void executeOther(Runnable task) {
		for (TaskDistributionService service : taskDistributionServiceLoader) {
			service.executeOther(task);
		}
	}
}
