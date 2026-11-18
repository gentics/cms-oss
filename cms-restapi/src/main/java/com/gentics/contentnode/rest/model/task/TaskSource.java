package com.gentics.contentnode.rest.model.task;

/**
 * Source of a task
 */
public enum TaskSource {
	/**
	 * Task created by a user
	 */
	manual(false),

	/**
	 * Task created by a background job
	 */
	job(false),

	/**
	 * Task created by GenAIx for a session, e.g. "ready to publish"
	 */
	genaix(false),

	/**
	 * Virtual task for a page in the publish queue
	 */
	publishQueue(true),

	/**
	 * Virtual task for a page with broken links
	 */
	linkChecker(true);

	private final boolean virtual;

	TaskSource(boolean virtual) {
		this.virtual = virtual;
	}

	/**
	 * Whether tasks of this source are virtual
	 * @return true for virtual
	 */
	public boolean isVirtual() {
		return virtual;
	}
}
