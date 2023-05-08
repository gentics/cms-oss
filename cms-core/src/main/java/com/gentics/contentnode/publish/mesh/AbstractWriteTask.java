package com.gentics.contentnode.publish.mesh;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.publish.mesh.MeshPublisher.MeshProject;

/**
 * Abstract implementation of write tasks
 */
abstract class AbstractWriteTask {
	/**
	 * Object type
	 */
	protected int objType;

	/**
	 * Object ID
	 */
	protected int objId;

	/**
	 * Object description
	 */
	protected String description;

	/**
	 * Current project
	 */
	protected MeshProject project;

	/**
	 * Target project
	 */
	protected MeshProject targetProject;

	/**
	 * Node ID
	 */
	protected int nodeId;

	/**
	 * Mesh UUID
	 */
	protected String uuid;

	/**
	 * Instance of the {@link MeshPublisher}
	 */
	protected MeshPublisher publisher;

	/**
	 * Flag to mark whether the finished task should be reported to the publish queue
	 */
	protected boolean reportToPublishQueue;

	/**
	 * Perform this write task
	 * @throws NodeException
	 */
	public abstract void perform() throws NodeException;

	/**
	 * Report publishing of an object
	 */
	public abstract void reportDone();
}
