package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.perm.DeletePermType;
import com.gentics.contentnode.factory.perm.EditPermType;
import com.gentics.contentnode.factory.perm.ViewPermType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.perm.PermType;

/**
 * Interface for objects, that are contained in folders
 */
public interface NodeObjectInFolder extends NodeObject {
	/**
	 * Get the folder of the object.
	 * @return The folder of the object.
	 * @throws NodeException
	 */
	Folder getFolder() throws NodeException;

	/**
	 * Gets the folder ID of a object.
	 * @return The folder ID of the object.
	 * @throws NodeException
	 */
	Integer getFolderId() throws NodeException;

	/**
	 * Set the folder id of the object
	 * @param folderId new folder id
	 * @throws NodeException
	 */
	void setFolderId(Integer folderId) throws NodeException, ReadOnlyException;

	/**
	 * Get the node owning this object (which is the node owning the object's folder)
	 * @return node
	 * @throws NodeException
	 */
	default Node getOwningNode() throws NodeException {
		try (NoMcTrx noMc = new NoMcTrx()) {
			return getFolder().getOwningNode();
		}
	}

	@Override
	default NodeObject getParentObject() throws NodeException {
		return getFolder();
	}

	/**
	 * Check whether the current user is allowed to view this object
	 * @return permission flag
	 * @throws NodeException
	 */
	default boolean canView() throws NodeException {
		return canView(TransactionManager.getCurrentTransaction().getPermHandler());
	}

	/**
	 * Check whether the perm handler allows to view this object
	 * @param permHandler permission handler for checking
	 * @return permission flag
	 * @throws NodeException
	 */
	default boolean canView(PermHandler permHandler) throws NodeException {
		ViewPermType annotation = getObjectInfo().getObjectClass().getAnnotation(ViewPermType.class);
		if (annotation == null) {
			return true;
		}

		Folder folder = null;
		try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE); ChannelTrx trx = new ChannelTrx()) {
			folder = getFolder().getMaster();
		}
		if (folder == null) {
			return false;
		} else {
			if (!permHandler.canView(folder)) {
				return false;
			}

			return permHandler.checkPermissionBit(Folder.TYPE_FOLDER, folder.getId(), annotation.value().getBit(),
					getTType(), getRoleCheckId(), getRoleBit(annotation.value()));
		}
	}

	/**
	 * Check whether the current user is allowed to edit this object
	 * @return permission flag
	 * @throws NodeException
	 */
	default boolean canEdit() throws NodeException {
		return canEdit(TransactionManager.getCurrentTransaction().getPermHandler());
	}

	/**
	 * Check whether the permission handler allows to edit this object
	 * @param permHandler permission handler to check
	 * @return permission flag
	 * @throws NodeException
	 */
	default boolean canEdit(PermHandler permHandler) throws NodeException {
		if (!canView(permHandler)) {
			return false;
		}

		EditPermType annotation = getObjectInfo().getObjectClass().getAnnotation(EditPermType.class);
		if (annotation == null) {
			return true;
		}
		Folder folder = null;
		try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE); ChannelTrx trx = new ChannelTrx()) {
			folder = getFolder().getMaster();
		}
		if (folder == null) {
			return false;
		} else {
			return permHandler.checkPermissionBit(Folder.TYPE_FOLDER, folder.getId(), annotation.value().getBit(),
					getTType(), getRoleCheckId(), getRoleBit(annotation.value()));
		}
	}

	/**
	 * Check whether the current user is allowed to delete this object
	 * @return permission flag
	 * @throws NodeException
	 */
	default boolean canDelete() throws NodeException {
		return canDelete(TransactionManager.getCurrentTransaction().getPermHandler());
	}

	/**
	 * Check whether the permission handler allows to delete this object
	 * @param permHandler permission handler to check
	 * @return permission flag
	 * @throws NodeException
	 */
	default boolean canDelete(PermHandler permHandler) throws NodeException {
		if (!canView(permHandler)) {
			return false;
		}

		DeletePermType annotation = getObjectInfo().getObjectClass().getAnnotation(DeletePermType.class);
		if (annotation == null) {
			return true;
		}
		Folder folder = null;
		try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE); ChannelTrx trx = new ChannelTrx()) {
			folder = getFolder().getMaster();
		}
		if (folder == null) {
			return false;
		} else {
			return permHandler.checkPermissionBit(Folder.TYPE_FOLDER, folder.getId(), annotation.value().getBit(),
					getTType(), getRoleCheckId(), getRoleBit(annotation.value()));
		}
	}

	/**
	 * Get the role bit of the permType, which is used for this object (-1 if no role bit is used)
	 * @param permType perm type
	 * @return role bit (may be -1)
	 */
	int getRoleBit(PermType permType);

	/**
	 * Get the ID of the entry, for which the role bit is checked (-1 if no role checking is done)
	 * @return ID
	 */
	int getRoleCheckId();

	/**
	 * Inform the object, that the inheritance setting of the folder changed
	 * @throws NodeException
	 */
	void folderInheritanceChanged() throws NodeException;
}
