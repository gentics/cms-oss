/*
 * @author norbert
 * @date 28.01.2008
 * @version $Id: DBCopyController.java,v 1.9 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.dbcopy.jaxb.JAXBtableType;
import com.gentics.contentnode.nodecopy.AbstractCopyController;
import com.gentics.contentnode.nodecopy.ObjectHelper;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.FileUtils;
import com.gentics.contentnode.publish.FileUtilsImpl;
import com.gentics.lib.db.DB;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Specific implementation of the {@link CopyController} that copies records of
 * a database.
 */
public class DBCopyController extends AbstractCopyController {

	/**
	 * root work phase
	 */
	protected IWorkPhase rootWorkPhase;

	private boolean dbFileContentInDB;

	protected static NodeLogger logger = NodeLogger.getNodeLogger(DBCopyController.class);

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#setRootWorkPhase(com.gentics.lib.etc.IWorkPhase)
	 */
	public void setRootWorkPhase(IWorkPhase rootWorkPhase) {
		this.rootWorkPhase = rootWorkPhase;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#copyObject(com.gentics.contentnode.dbcopy.StructureCopy,
	 *      com.gentics.contentnode.dbcopy.DBObject)
	 */
	public int copyObject(StructureCopy copy, DBObject object, boolean firstRun) throws StructureCopyException {
		Table table = object.getSourceTable();

		if (firstRun && table.isCrossTable()) {
			// don't copy crosstable entries in the first run
			return OBJECT_IGNORED;
		} else if (!firstRun && !table.isCrossTable()) {
			// only copy crosstable entries in the second run
			return OBJECT_IGNORED;
		}
		if (table.isCrossTable()) {
			// copy the cross table object (with all new object links)
			PreparedStatement st = null;

			try {
				StringBuffer copyCmd = new StringBuffer();

				copyCmd.append("INSERT INTO `" + table.getName() + "` (" + StringUtils.merge(table.allCopyColumns, ", ") + ") SELECT ");
				Object[] params = new Object[object.referencedObjects.size() + table.crossTableId.length];
				int paramCounter = 0;
				boolean first = true;

				for (int i = 0; i < table.allCopyColumns.length; i++) {
					if (first) {
						first = false;
					} else {
						copyCmd.append(", ");
					}
					if (object.referencedObjects.containsKey(table.allCopyColumns[i])) {
						// TODO eventually check whether the referenced object
						// has a new id
						copyCmd.append("?");
						params[paramCounter++] = ((DBObject) object.referencedObjects.get(table.allCopyColumns[i])).newId;
					} else {
						// special hackish fix for problem when copying nodes
						// with templates, when the original template was linked
						// to a folder outside that node
						// without this fix, the copied template would also be linked to the foreign folder (which was not copied)
						if ("template_folder".equals(table.getName()) && "yes".equals(copy.properties.getProperty("copytemplate", "no"))) {
							return OBJECT_IGNORED;
						}
						copyCmd.append(table.allCopyColumns[i]);
					}
				}
				copyCmd.append(" FROM `").append(table.getName()).append('`');
				first = true;
				for (int i = 0; i < table.crossTableId.length; i++) {
					if (first) {
						copyCmd.append(" WHERE ");
						first = false;
					} else {
						copyCmd.append(" AND ");
					}
					copyCmd.append(table.crossTableId[i]).append(" = ?");
					params[paramCounter++] = object.getColValue(table.crossTableId[i]);
				}

				st = copy.getConnection().prepareStatement(copyCmd.toString());
				for (int i = 0; i < params.length; i++) {
					st.setObject(i + 1, params[i]);
				}
				st.execute();
			} catch (SQLException e) {
				System.out.println("Error while trying to copy values of table {" + table.getId() + "}");
				throw new StructureCopyException(e);
			} finally {
				if (st != null) {
					try {
						st.close();
					} catch (SQLException ignored) {}
				}
			}
		} else {
			// copy the object
			PreparedStatement st = null;
			ResultSet keys = null;

			try {
				st = copy.getConnection().prepareStatement(table.copyCommand, PreparedStatement.RETURN_GENERATED_KEYS);
				st.setObject(1, object.originalId);
				st.execute();
				keys = st.getGeneratedKeys();
				keys.next();
				object.newId = keys.getObject(1);

				// FIXME: ugly hack (as ugly as in import/export) to copy files.
				if ("contentfile".equals(table.getName())) {
					if (!dbFileContentInDB) {
						String filepath = copy.resolveProperties("${filepath}");
						File dbFile = new File(filepath, object.getOriginalId() + ".bin");

						if (dbFile.exists()) {
							FileUtils fileUtils = new FileUtilsImpl();

							try {
								fileUtils.createCopy(dbFile, new File(filepath, object.getId() + ".bin"));
							} catch (IOException e) {
								throw new StructureCopyException("Error while copying contentfile {" + object.getOriginalId() + "}", e);
							}
						} else {
							logger.fatal("Unable to find dbfile for contentfile with id {" + object.getOriginalId() + "} - tried to find in {" + filepath + "}");
						}
					} else {
						// file is stored in DB
						PreparedStatement stmt2 = null;

						try {
							stmt2 = copy.getConnection().prepareStatement(
									"INSERT INTO contentfiledata (contentfile_id, binarycontent) SELECT ?, binarycontent FROM contentfiledata WHERE contentfile_id = ?");
							stmt2.setObject(1, object.getId());
							stmt2.setObject(2, object.getOriginalId());
							int r = stmt2.executeUpdate();

							if (r != 1) {
								logger.error(
										"Error while trying to copy binary data in database - insert returned wrong number of affected rows: expected:{1} actual:{"
												+ r + "} originalId:{" + object.getOriginalId() + "} newId:{" + object.getId() + "}");
							}
						} finally {
							DB.close(stmt2);
						}
					}
				}
			} catch (SQLException e) {
				throw new StructureCopyException(e);
			} finally {
				if (keys != null) {
					try {
						keys.close();
					} catch (SQLException ignored) {}
				}
				if (st != null) {
					try {
						st.close();
					} catch (SQLException ignored) {}
				}
			}
		}

		return OBJECT_CREATED;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#getObjectStructure(com.gentics.contentnode.dbcopy.StructureCopy)
	 */
	public void getObjectStructure(StructureCopy copy,
			Map<StructureCopy.ObjectKey, DBObject> allObjects,
			Map<StructureCopy.ObjectKey, DBObject> mainObjects) throws StructureCopyException {
		Table rootTable = getRootTable(copy);

		if (rootTable == null) {
			throw new StructureCopyException("Cannot copy db structure, must have a roottable configured!");
		}

		// get the objects, beginning with those from the root table
		rootTable.getObjects(copy, copy.getConnection(), null, null, mainObjects, null, null);
		allObjects.putAll(mainObjects);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#getObjects(com.gentics.contentnode.dbcopy.StructureCopy,
	 *      com.gentics.contentnode.dbcopy.Table, java.lang.String,
	 *      java.lang.Object[], java.util.Map, java.lang.String,
	 *      com.gentics.contentnode.dbcopy.DBObject)
	 */
	public List<DBObject> getObjects(StructureCopy copy, Table table, String fromClause,
			String restriction, Object[] params, Map<StructureCopy.ObjectKey, DBObject> allObjects, String referenceName,
			DBObject referencedObject) throws StructureCopyException {
		return ObjectHelper.getObjectsFromNodeDB(copy, table, fromClause, restriction, params, allObjects, referenceName, referencedObject);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#updateObjectLinks(com.gentics.contentnode.dbcopy.StructureCopy,
	 *      com.gentics.contentnode.dbcopy.DBObject)
	 */
	public void updateObjectLinks(StructureCopy copy, DBObject object) throws StructureCopyException {
		// check whether the object is already copied
		if (object.newId == null) {
			throw new StructureCopyException("Cannot update object links for object " + object + ": object is not yet copied");
		}

		// no references -> nothing to do
		if (object.referencedObjects.isEmpty()) {
			return;
		}

		Table table = object.getSourceTable();

		// generate the update command
		StringBuffer updateCommand = new StringBuffer();
		Object[] params = new Object[object.referencedObjects.size() + 1];

		updateCommand.append("UPDATE `").append(table.getName()).append('`');
		boolean first = true;
		int paramsCounter = 0;

		for (Map.Entry<String, DBObject> entry : object.referencedObjects.entrySet()) {
			String colId = entry.getKey().toString();
			DBObject referencedObject = (DBObject) entry.getValue();

			// check whether all referenced objects already have a new id
			if (referencedObject.newId != null) {
				if (first) {
					first = false;
					updateCommand.append(" SET ");
				} else {
					updateCommand.append(", ");
				}
				updateCommand.append(colId).append(" = ?");
				params[paramsCounter++] = referencedObject.newId;
			} else {
				throw new StructureCopyException(
						"Cannot update object links for object " + object + ": referenced object " + referencedObject + " (column " + colId + ") is not yet copied.");
			}
		}
		updateCommand.append(" WHERE " + table.getIdcol() + " = ?");
		params[paramsCounter++] = object.newId;

		// update the object links
		PreparedStatement st = null;

		try {
			st = copy.getConnection().prepareStatement(updateCommand.toString());
			for (int i = 0; i < params.length; i++) {
				st.setObject(i + 1, params[i]);
			}
			st.execute();
		} catch (SQLException e) {
			throw new StructureCopyException(e);
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (SQLException ignored) {}
			}
		}
	}

	/**
	 * Get the root table
	 * @param copy copy configuration
	 * @return root table or null
	 */
	protected Table getRootTable(StructureCopy copy) {
		JAXBtableType[] tables = copy.getTables().getTable();

		for (int i = 0; i < tables.length; i++) {
			if (ObjectTransformer.getBoolean(((Table) tables[i]).getProperty("roottable"), false)) {
				return (Table) tables[i];
			}
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#finishCopy(com.gentics.contentnode.dbcopy.StructureCopy)
	 */
	public void finishCopy(StructureCopy copy) throws StructureCopyException {}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#startCopy(com.gentics.contentnode.dbcopy.StructureCopy)
	 */
	public void startCopy(StructureCopy copy) throws StructureCopyException {
		dbFileContentInDB = ObjectTransformer.getBoolean(copy.resolveProperties("${dbFileContentInDB}"), false);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#handleErrors(com.gentics.contentnode.dbcopy.StructureCopy, java.lang.Exception)
	 */
	public void handleErrors(StructureCopy copy, Exception e) throws StructureCopyException {}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#handleUnsatisfiedLink(com.gentics.contentnode.dbcopy.StructureCopy, com.gentics.contentnode.dbcopy.DBObject, com.gentics.contentnode.dbcopy.ReferenceDescriptor, com.gentics.contentnode.dbcopy.Table, java.lang.Object)
	 */
	public void handleUnsatisfiedLink(StructureCopy copy, DBObject object,
			ReferenceDescriptor reference, Table table, Object id) throws StructureCopyException {}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#getObjectByID(com.gentics.contentnode.dbcopy.StructureCopy, com.gentics.contentnode.dbcopy.Table, java.lang.Object, java.lang.String, com.gentics.contentnode.dbcopy.DBObject)
	 */
	public DBObject getObjectByID(StructureCopy copy, Table table, Object id,
			String referenceName, DBObject referencingObject, boolean checkForExclusions) throws StructureCopyException {
		return ObjectHelper.getObjectFromNodeDBByID(copy, table, id, referenceName, referencingObject, checkForExclusions);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#beginCopyObjects(com.gentics.contentnode.dbcopy.StructureCopy)
	 */
	public void beginCopyObjects(StructureCopy copy, Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException {}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#finishCopyObjects(com.gentics.contentnode.dbcopy.StructureCopy)
	 */
	public void finishCopyObjects(StructureCopy copy, Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException {
		// create page versions for all copied pages
		for (DBObject obj : allObjects.values()) {
			if (ObjectTransformer.getInt(obj.getSourceTable().getProperty("ttype"), -1) == Page.TYPE_PAGE) {
				createPageVersion(copy, obj);
			}
		}
	}

	public void postCommit(StructureCopy copy) {// nothing to do..
	}
}
