package com.gentics.contentnode.tests.utils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.Creator;

/**
 * Enum for tested inheritance situations
 */
public enum TestedInheritance {
	/**
	 * Object is created in master and inherited in channel
	 */
	inherited,

	/**
	 * Object is created in master and localized in channel
	 */
	localized,

	/**
	 * Object is created in channel
	 */
	local;

	/**
	 * Create objects of the given type according to this inheritance situation.
	 * The objects will be created in the root folder of the given node/channel.
	 * The method will return the master object and channel object as array. If
	 * any of the objects is not created, the entry will be null.
	 * Pages will not be published.
	 * @param type object type
	 * @param master master node
	 * @param channel channel node
	 * @return array of created objects
	 * @throws NodeException
	 */
	public LocalizableNodeObject<?>[] create(MCTestedType type, Node master, Node channel) throws NodeException {
		return create(type, master.getFolder(), channel);
	}

	/**
	 * Create objects of the given type according to this inheritance situation.
	 * The objects will be created in the given folder.
	 * The method will return the master object and channel object as array. If
	 * any of the objects is not created, the entry will be null.
	 * Pages will not be published.
	 * @param type object type
	 * @param folder folder
	 * @param channel channel node
	 * @return array of created objects
	 * @throws NodeException
	 */
	public LocalizableNodeObject<?>[] create(MCTestedType type, Folder folder, Node channel) throws NodeException {
		LocalizableNodeObject<?>[] objects = new LocalizableNodeObject<?>[2];
		Template template = null;
		if (type == MCTestedType.page) {
			template = Creator.createTemplate("Test template", "", folder);
		}
		switch (this) {
		case inherited:
			objects[0] = type.create(folder, template);
			break;
		case localized:
			objects[0] = type.create(folder, template);
			objects[1] = type.localize(objects[0], channel);
			break;
		case local:
			objects[1] = type.create(folder, null, template, channel);
			break;
		}
		return objects;
	}

	/**
	 * Variant of {@link #create(MCTestedType, Node, Node)} that will publish pages (if pages are created)
	 * @param type object type
	 * @param master master node
	 * @param channel channel node
	 * @return array of created objects
	 * @throws NodeException
	 */
	public LocalizableNodeObject<?>[] createPublished(MCTestedType type, Node master, Node channel) throws NodeException {
		return createPublished(type, master.getFolder(), channel);
	}

	/**
	 * Variant of {@link #create(MCTestedType, Folder, Node)} that will publish pages (if pages are created)
	 * @param type object type
	 * @param folder folder
	 * @param channel channel node
	 * @return array of created objects
	 * @throws NodeException
	 */
	public LocalizableNodeObject<?>[] createPublished(MCTestedType type, Folder folder, Node channel) throws NodeException {
		LocalizableNodeObject<?>[] objects = create(type, folder, channel);
		Transaction t = TransactionManager.getCurrentTransaction();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof Page) {
				t.getObject(Page.class, objects[i].getId(), true).publish();
			}
		}
		return objects;
	}

	/**
	 * Check whether a container (folder) with this inheritance can contain children with the given childInheritance
	 * @param childInheritance child inheritance
	 * @return true iff folder can contain the requested children
	 */
	public boolean canContain(TestedInheritance childInheritance) {
		switch (this) {
		case local:
			return childInheritance == local;
		case inherited:
		case localized:
		default:
			return true;
		}
	}
}
