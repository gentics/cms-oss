package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.perm.PublishPermType;
import com.gentics.contentnode.perm.PermHandler;

/**
 * Interface for objects, which can be published and are contained in folders
 */
public interface PublishableNodeObjectInFolder extends PublishableNodeObject, NodeObjectInFolder {
	/**
	 * Check whether the current user is allowed to publish this object (this includes permission to take the object offline)
	 * @return permission flag
	 * @throws NodeException
	 */
	default boolean canPublish() throws NodeException {
		return canPublish(TransactionManager.getCurrentTransaction().getPermHandler());
	}

	/**
	 * Check whether the permission handler allows to publish this object (this includes permission to take the object offline)
	 * @return permission flag
	 * @throws NodeException
	 */
	default boolean canPublish(PermHandler permHandler) throws NodeException {
		if (!canView(permHandler)) {
			return false;
		}

		PublishPermType annotation = getObjectInfo().getObjectClass().getAnnotation(PublishPermType.class);
		if (annotation == null) {
			return true;
		}

		Folder folder = null;
		try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
			folder = getFolder().getMaster();
		}
		if (folder == null) {
			return false;
		} else {
			return permHandler.checkPermissionBit(Folder.TYPE_FOLDER, folder.getId(), annotation.value().getBit(), getTType(), getRoleCheckId(), getRoleBit(annotation.value()));
		}
	}
}
