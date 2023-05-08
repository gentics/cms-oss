package com.gentics.contentnode.publish.mesh;

/**
 * Management Bean interface for MeshPublisher information
 */
public interface MeshPublishControllerInfoMBean {
	/**
	 * Get the taskqueue size
	 * @return size of the task queue
	 */
	int getWriteTaskQueueSize();

	/**
	 * Get the number of postponed tasks
	 * @return number of postponed tasks
	 */
	int getPostponedWriteTasks();

	/**
	 * Get number of remaining render tasks
	 * @return number of remaining tasks
	 */
	int getRemainingRenderTasks();

	/**
	 * Get number of total render tasks
	 * @return number of total tasks
	 */
	int getTotalRenderTasks();

	/**
	 * Get the state of the publish controller
	 * @return state
	 */
	String getState();
}
