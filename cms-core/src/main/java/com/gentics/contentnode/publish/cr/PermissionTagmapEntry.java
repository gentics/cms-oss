package com.gentics.contentnode.publish.cr;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.log.NodeLogger;

/**
 * Special implementation of the Tagmap Entry for publishing permission
 * information
 */
public class PermissionTagmapEntry extends DummyTagmapEntry {
	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ContentMap.class);

	/**
	 * Permission bit
	 */
	protected int permissionBit;

	/**
	 * Create an instance of the permission tagmap entry
	 * @param tagname tagname
	 * @param mapname mapname
	 * @param attributeType attribute type
	 * @param objectType object type
	 * @param permissionBit permission bit
	 */
	public PermissionTagmapEntry(String tagname, String mapname, int attributeType,
			int objectType, int permissionBit) {
		super(objectType, tagname, mapname, attributeType, 0);
		this.permissionBit = permissionBit;
	}

	@Override
	public Object transformValue(Object value, BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer) {
		Integer folderId = ObjectTransformer.getInteger(value, null);

		// garbage in, garbage out
		if (folderId == null) {
			return null;
		}

		try {
			// add the dependency on the property "permissions" of the
			// folder
			Transaction t = TransactionManager.getCurrentTransaction();
			RenderType renderType = t.getRenderType();
			Folder folder = t.getObject(Folder.class, folderId);

			if (folder == null) {
				throw new NodeException("Folder " + folderId + " not found");
			}
			folder = folder.getMaster();

			renderType.addDependency(folder, "permissions");

			// transform the folder id in a list of
			// group id's with permission to perform an
			// action on the folder
			return PermHandler.getGroupsWithPermissionBit(Folder.TYPE_FOLDER, folder.getId(), permissionBit);
		} catch (NodeException e) {
			logger.error("Error while adding permission attribute {" + mapname + "} into contentrepository: ", e);
			return null;
		}
	}
}
