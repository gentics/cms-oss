package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.NormalReference;
import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.StructureCopy.ObjectKey;

/**
 * Reference template_folder -> template. The reference will restrict fetching foreign objects to folders that are not deleted
 */
public class TemplateFolderReference extends NormalReference {

	public TemplateFolderReference(Table sourceTable, Tables tables, Reference reference) {
		super(sourceTable, tables.getTable("template"), "template_id", reference);
	}

	@Override
	public List<DBObject> getLinkingObjects(StructureCopy copy, Connection conn, DBObject object, Map<ObjectKey, DBObject> allObjects)
			throws StructureCopyException {
		return sourceTable.getObjects(copy, conn, "template_folder, folder",
				"template_folder.folder_id = folder.id AND template_folder.template_id = ? AND folder.deleted = 0", new Object[] { object.getId() },
				allObjects, getLinkColumn(), object);
	}
}
