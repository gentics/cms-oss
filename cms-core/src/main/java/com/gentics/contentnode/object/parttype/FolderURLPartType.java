/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: FolderURLPartType.java,v 1.2 2007-04-30 07:34:49 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 25 - URL (folder)
 */
public class FolderURLPartType extends UrlPartType {

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public FolderURLPartType(Value value) throws NodeException {
		super(value, UrlPartType.TARGET_FOLDER);
	}

	@Override
	public int getInternal() {
		// folder urls are always "internal"
		return 1;
	}

	/**
	 * Get the target folder
	 * @return target folder (or null)
	 * @throws NodeException
	 */
	public Folder getTargetFolder() throws NodeException {
		return (Folder) getTarget();
	}

	/**
	 * Set the target folder
	 * @param folder target folder (may be null to unset)
	 * @throws NodeException
	 */
	public void setTargetFolder(Folder folder) throws NodeException {
		Value value = getValueObject();

		// if a folder is set, we replace it by its master
		if (folder != null) {
			folder = folder.getMaster();
		}

		// set the folder id
		value.setValueRef(folder == null ? 0 : ObjectTransformer.getInt(folder.getId(), 0));
	}

	@Override
	public Type getPropertyType() {
		return Type.FOLDER;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		NodeObject target = getTarget();

		if (target != null) {
			property.setFolderId(target.getId());
			Node node = getNode();
			if (node != null) {
				property.setNodeId(node.getId());
			}
		}
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		setTargetFolder(t.getObject(Folder.class, property.getFolderId(), -1, false));
		setNode(t.getObject(Node.class, property.getNodeId()));
	}
}
