package com.gentics.contentnode.tests.utils;

import static org.junit.Assert.fail;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.FolderMoveRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.testutils.GenericTestUtils;

/**
 * Enum for tested object types.
 * Contain methods for basic operations (create, localize, delete from wastebin, restore from wastebin, ...)
 */
public enum TestedType {
	folder,
	page,
	file,
	image;

	/**
	 * Create an instance of the tested type
	 * @param folder where to create the object
	 * @param template template used to create pages
	 * @return instance
	 * @throws NodeException
	 */
	public LocalizableNodeObject<?> create(Folder folder, Template template) throws NodeException {
		return create(folder, null, template, null);
	}

	/**
	 * Create an instance of the tested type
	 * @param folder where to create the object
	 * @param name object name. If null, the object will get a default name
	 * @param template template used to create pages
	 * @return instance
	 * @throws NodeException
	 */
	public LocalizableNodeObject<?> create(Folder folder, String name, Template template) throws NodeException {
		return create(folder, name, template, null);
	}

	/**
	 * Create an instance of the tested type
	 * @param folder where to create the object
	 * @param template template used to create pages
	 * @param channel channel or null to create in the master node
	 * @return instance
	 * @throws NodeException
	 */
	public LocalizableNodeObject<?> create(Folder folder, String name, Template template, Node channel) throws NodeException {
		switch (this) {
		case folder:
			return ContentNodeTestDataUtils.createFolder(folder, ObjectTransformer.getString(name, "Folder"), channel);
		case page:
			return ContentNodeTestDataUtils.createPage(folder, template, ObjectTransformer.getString(name, "Page"), channel);
		case file:
			return ContentNodeTestDataUtils.createFile(folder, ObjectTransformer.getString(name, "testfile.txt"), "Contents".getBytes(), channel);
		case image:
			return ContentNodeTestDataUtils.createFile(folder, ObjectTransformer.getString(name, "blume.jpg"), GenericTestUtils.getPictureResource("blume.jpg"), channel);
		default:
			fail("Cannot generate object of unknown type " + this);
			return null;
		}
	}

	/**
	 * Localize the object in the given channel
	 * @param object object (must be created via {@link #create(Folder, String)} of this type)
	 * @param channel channel
	 * @return localized copy
	 * @throws NodeException
	 */
	public LocalizableNodeObject<?> localize(LocalizableNodeObject<?> object, Node channel) throws NodeException {
		switch (this) {
		case folder:
			return ContentNodeTestDataUtils.localize((Folder)object, channel);
		case page:
			return ContentNodeTestDataUtils.localize((Page)object, channel);
		case file:
		case image:
			return ContentNodeTestDataUtils.localize((File)object, channel);
		default:
			fail("Cannot localized object of unknown type " + this);
			return null;
		}
	}

	/**
	 * Delete the given object from the wastebin
	 * @param object object
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse deleteFromWastebin(NodeObject object) throws NodeException {
		switch (this) {
		case folder:
			return new FolderResourceImpl().deleteFromWastebin(object.getId().toString(), 0);
		case page:
			return new PageResourceImpl().deleteFromWastebin(object.getId().toString(), 0);
		case file:
			return new FileResourceImpl().deleteFromWastebin(object.getId().toString(), 0);
		case image:
			return new ImageResourceImpl().deleteFromWastebin(object.getId().toString(), 0);
		default:
			return null;
		}
	}

	/**
	 * Restore the given object from the wastebin
	 * @param object object
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse restoreFromWastebin(NodeObject object) throws NodeException {
		switch (this) {
		case folder:
			return new FolderResourceImpl().restoreFromWastebin(object.getId().toString(), 0);
		case page:
			return new PageResourceImpl().restoreFromWastebin(object.getId().toString(), 0);
		case file:
			return new FileResourceImpl().restoreFromWastebin(object.getId().toString(), 0);
		case image:
			return new ImageResourceImpl().restoreFromWastebin(object.getId().toString(), 0);
		default:
			return null;
		}
	}

	/**
	 * Publish the object, if it is a page
	 * @param object object to publish
	 * @return object
	 * @throws NodeException
	 */
	public LocalizableNodeObject<?> publish(LocalizableNodeObject<?> object) throws NodeException {
		if (this == page && object instanceof Page) {
			return ContentNodeTestDataUtils.update((Page)object, p -> p.publish());
		} else {
			return object;
		}
	}

	/**
	 * Move the object into the target folder
	 * @param object object to move
	 * @param targetFolder target folder
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse move(LocalizableNodeObject<?> object, Folder targetFolder) throws NodeException {
		return move(object, targetFolder, null);
	}

	/**
	 * Move the object into the target folder
	 * @param object object to move
	 * @param targetFolder target folder
	 * @param targetNode optional target not to move into a channel
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse move(LocalizableNodeObject<?> object, Folder targetFolder, Node targetNode) throws NodeException {
		ObjectMoveRequest request = new ObjectMoveRequest();
		request.setFolderId(targetFolder.getId());
		if (targetNode != null) {
			request.setNodeId(targetNode.getId());
		}
		switch (this) {
		case file:
			return new FileResourceImpl().move(Integer.toString(object.getId()), request);
		case folder:
			try {
				FolderMoveRequest folderRequest = new FolderMoveRequest();
				folderRequest.setFolderId(targetFolder.getId());
				if (targetNode != null) {
					folderRequest.setNodeId(targetNode.getId());
				}
				return new FolderResourceImpl().move(Integer.toString(object.getId()), folderRequest);
			} catch (Exception e) {
				throw new NodeException(e);
			}
		case image:
			return new ImageResourceImpl().move(Integer.toString(object.getId()), request);
		case page:
			return new PageResourceImpl().move(Integer.toString(object.getId()), request);
		default:
			return null;
		}
	}

	/**
	 * Get the object class
	 * @return object class
	 */
	public Class<? extends NodeObject> getClazz() {
		switch (this) {
		case folder:
			return Folder.class;
		case page:
			return Page.class;
		case file:
			return File.class;
		case image:
			return ImageFile.class;
		default:
			return null;
		}
	}
}
