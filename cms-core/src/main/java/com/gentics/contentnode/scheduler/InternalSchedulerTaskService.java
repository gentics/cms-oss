package com.gentics.contentnode.scheduler;

import java.util.List;

/**
 * Interface for services for {@link InternalSchedulerTask}s
 */
public interface InternalSchedulerTaskService {
	/**
	 * Get the list of provided tasks
	 * @return task list
	 */
	List<InternalSchedulerTask> tasks();
}
