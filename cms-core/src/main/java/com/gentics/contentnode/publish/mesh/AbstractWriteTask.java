package com.gentics.contentnode.publish.mesh;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
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
	 * Optional roles to set
	 */
	protected Collection<String> roles;

	/**
	 * Perform this write task
	 * @throws NodeException
	 */
	public void perform() throws NodeException {
		perform(true);
	}

	/**
	 * Perform this write task
	 * @param withSemaphore whether the acquire a semaphore
	 * @throws NodeException
	 */
	public abstract void perform(boolean withSemaphore) throws NodeException;

	/**
	 * Report publishing of an object
	 */
	public abstract void reportDone();

	/**
	 * Get the node object of this write task
	 * @return node object
	 * @throws NodeException
	 */
	public NodeObject getNodeObject() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			return t.getObject(t.getClass(objType), objId);
		}
	}

	/**
	 * Get the language variant of the node object in the given language (or null)
	 * @param language language
	 * @return language variant or null
	 * @throws NodeException
	 */
	public NodeObject getLanguageVariant(String language) throws NodeException {
		NodeObject nodeObject = getNodeObject();
		if (nodeObject == null) {
			return null;
		}
		if (nodeObject instanceof Page) {
			Page page = (Page) nodeObject;
			try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
				return page.getLanguageVariant(language);
			}
		} else if (nodeObject instanceof Folder) {
			// folders will always exist in english
			if (StringUtils.equals(language, "en")) {
				return nodeObject;
			} else {
				// check for alternative languages
				if (MeshPublisher.getAlternativeMeshLanguages(nodeObject).contains(language)) {
					return nodeObject;
				} else {
					return null;
				}
			}
		} else {
			return null;
		}
	}
}
