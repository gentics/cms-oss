package com.gentics.contentnode.task;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.task.TaskSource;

/**
 * Provider of the virtual tasks of one source, registered in <code>META-INF/services</code>
 */
public interface TaskProviderService {
	/**
	 * Source of the tasks this provider computes
	 * @return a virtual source
	 */
	TaskSource source();

	/**
	 * Get the tasks the current user may see
	 * @return list of tasks
	 * @throws NodeException
	 */
	List<ResolvableUserTaskModel> tasks() throws NodeException;
}
