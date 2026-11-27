package com.gentics.contentnode.rest.model.task;

/**
 * Action the current user may perform on a task
 */
public enum TaskAction {
	/**
	 * Open the object or the GenAIx session
	 */
	open,

	/**
	 * View the published page ({@code details.liveUrl})
	 */
	view,

	/**
	 * Approve the publish queue request ({@code POST /page/pubqueue/approve})
	 */
	approve,

	/**
	 * Hand the page back for revision ({@code POST /page/assign})
	 */
	assignBack,

	/**
	 * Set the status to {@link TaskStatus#done}
	 */
	complete,

	/**
	 * Set the status to {@link TaskStatus#dismissed}
	 */
	dismiss,

	/**
	 * Change title, description, due date or recipient
	 */
	edit,

	/**
	 * Delete the task
	 */
	delete,

	/**
	 * Start a new GenAIx session with the task's objects as context
	 */
	startSession,

	/**
	 * Take over a group task ("Assign to me")
	 */
	claim,

	/**
	 * Give a claimed group task back to its group
	 */
	unclaim
}
