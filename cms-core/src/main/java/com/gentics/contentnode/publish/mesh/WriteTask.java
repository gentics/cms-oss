package com.gentics.contentnode.publish.mesh;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.PublishAction;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
//import com.gentics.contentnode.publish.mesh.MeshPublisher.MeshNodeTracker;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeResponse;

import io.reactivex.Single;
import io.reactivex.functions.Function;

/**
 * Implementation of a task to write a node into Mesh
 */
class WriteTask extends AbstractWriteTask {
	/**
	 * Schema name
	 */
	protected String schema;

	/**
	 * Mesh UUID of the parent
	 */
	protected String parentUuid;

	/**
	 * Local ID of the parent folder
	 */
	protected int folderId;

	/**
	 * Language
	 */
	protected String language;

	/**
	 * Optional alternative mesh languages
	 */
	protected Set<String> alternativeMeshLanguages;

	/**
	 * Flag to mark existing objects
	 */
	protected boolean exists;

	/**
	 * Fields of the Mesh Node
	 */
	protected FieldMap fields;

//	/**
//	 * Tracker to mark when an object was written
//	 */
//	protected MeshNodeTracker tracker;

	/**
	 * Optional list of post save operations (e.g. for uploading binary data)
	 */
	protected List<Function<NodeResponse, Single<NodeResponse>>> postSave;

	/**
	 * Optional postponed tagmap entries
	 */
	protected Map<TagmapEntryRenderer, Object> postponed;

	/**
	 * Optional roles to set
	 */
	protected Collection<String> roles;

	/**
	 * Flag to mark write tasks, which are postponable
	 */
	protected boolean postponable = true;

	/**
	 * Perform this write task
	 * @throws NodeException
	 */
	@Override
	public void perform(boolean withSemaphore) throws NodeException {
		publisher.save(this, withSemaphore, null);
	}

	/**
	 * Report publishing of an object
	 */
	@Override
	public void reportDone() {
		MeshPublisher.logger.debug(String.format("Set %d.%d to be done", objType, objId));
		if (publisher.controller.publishProcess) {
			if (reportToPublishQueue) {
				try {
					PublishQueue.reportPublishActionDone(MeshPublisher.normalizeObjType(objType), objId, nodeId, PublishAction.WRITE_CR);
				} catch (NodeException e) {
				}
			}

			if (publisher.publishInfo != null) {
				switch (objType) {
				case Folder.TYPE_FOLDER:
					MBeanRegistry.getPublisherInfo().publishedFolder(nodeId);
					publisher.publishInfo.folderRendered();
					break;
				case File.TYPE_FILE:
				case ImageFile.TYPE_IMAGE:
					MBeanRegistry.getPublisherInfo().publishedFile(nodeId);
					publisher.publishInfo.fileRendered();
					break;
				default:
					break;
				}
			}
		}
	}

	@Override
	public String toString() {
		if (!exists) {
			return String.format("Create %s as %s (uuid %s)", description, schema, uuid);
		} else {
			return String.format("Update %s as %s (uuid %s)", description, schema, uuid);
		}
	}
}