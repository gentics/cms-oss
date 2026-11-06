package com.gentics.contentnode.tests.utils;


import static org.junit.Assert.fail;

import java.util.Arrays;

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
import com.gentics.contentnode.rest.model.request.DisinheritRequest;
import com.gentics.contentnode.rest.model.request.FolderMoveRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.response.DisinheritResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LocalizationsResponse;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.DisinheritRestUtil;
import com.gentics.testutils.GenericTestUtils;

/**
 * Enum for tested object types.
 * Contain methods for basic operations (create, localize, delete from wastebin, restore from wastebin, ...)
 */
public enum MCTestedType {
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
	 * Disinherit the object from the channel
	 * @param object object to disinherit
	 * @param channel channel
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse disinherit(LocalizableNodeObject<?> object, Node channel) throws NodeException {
		DisinheritRequest request = new DisinheritRequest();
		request.setDisinherit(Arrays.asList(channel.getId()));
		return setInheritance(object, request);
	}

	/**
	 * Reinherit the object into the channel
	 * @param object object to reinherit
	 * @param channel channel
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse reinherit(LocalizableNodeObject<?> object, Node channel) throws NodeException {
		DisinheritRequest request = new DisinheritRequest();
		request.setReinherit(Arrays.asList(channel.getId()));
		return setInheritance(object, request);
	}

	/**
	 * Exclude object from inheritance
	 * @param object object to exclude
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse exclude(LocalizableNodeObject<?> object) throws NodeException {
		DisinheritRequest request = new DisinheritRequest();
		request.setExclude(true);
		return setInheritance(object, request);
	}

	/**
	 * Include object into inheritance
	 * @param object object to include
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse include(LocalizableNodeObject<?> object) throws NodeException {
		DisinheritRequest request = new DisinheritRequest();
		request.setExclude(false);
		return setInheritance(object, request);
	}

	/**
	 * Set the object to be disinherited by default
	 * @param object object
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse disinheritDefault(LocalizableNodeObject<?> object) throws NodeException {
		DisinheritRequest request = new DisinheritRequest();
		request.setDisinheritDefault(true);
		return setInheritance(object, request);
	}

	/**
	 * Set the object to be inherited by default
	 * @param object object
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse reinheritDefault(LocalizableNodeObject<?> object) throws NodeException {
		DisinheritRequest request = new DisinheritRequest();
		request.setDisinheritDefault(false);
		return setInheritance(object, request);
	}

	/**
	 * Get the localizations for the object with given ID
	 * @param objectId object ID
	 * @return localizations response
	 * @throws NodeException
	 */
	public LocalizationsResponse localizations(int objectId) throws NodeException {
		switch (this) {
		case file:
			return new MCFileResourceImpl().localizations(objectId);
		case folder:
			return new MCFolderResourceImpl().localizations(objectId);
		case image:
			return new MCImageResourceImpl().localizations(objectId);
		case page:
			return new MCPageResourceImpl().localizations(objectId, true);
		default:
			fail("Cannot get localizations for object of unknown type " + this);
			return null;
		}
	}

	/**
	 * Get the localizations for the given object
	 * @param object object
	 * @return localizations response
	 * @throws NodeException
	 */
	public LocalizationsResponse localizations(LocalizableNodeObject<?> object) throws NodeException {
		return localizations(object.getId());
	}

	/**
	 * Get the inheritance settings of the object
	 * @param object object
	 * @return disinherit response
	 * @throws NodeException
	 */
	public DisinheritResponse getInheritance(LocalizableNodeObject<?> object) throws NodeException {
		switch(this) {
		case file:
			return DisinheritRestUtil.get(File.class, Integer.toString(object.getId()));
		case folder:
			return DisinheritRestUtil.get(Folder.class, Integer.toString(object.getId()));
		case image:
			return DisinheritRestUtil.get(ImageFile.class, Integer.toString(object.getId()));
		case page:
			return DisinheritRestUtil.get(Page.class, Integer.toString(object.getId()));
		default:
			return null;
		}
	}

	/**
	 * Set the inheritance for the object
	 * @param object object
	 * @param request inheritance request
	 * @return response
	 * @throws NodeException
	 */
	protected GenericResponse setInheritance(LocalizableNodeObject<?> object, DisinheritRequest request) throws NodeException {
		switch(this) {
		case file:
			return DisinheritRestUtil.set(File.class, Integer.toString(object.getId()), request, false);
		case folder:
			return DisinheritRestUtil.set(Folder.class, Integer.toString(object.getId()), request, false);
		case image:
			return DisinheritRestUtil.set(ImageFile.class, Integer.toString(object.getId()), request, false);
		case page:
			return DisinheritRestUtil.set(Page.class, Integer.toString(object.getId()), request, false);
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
