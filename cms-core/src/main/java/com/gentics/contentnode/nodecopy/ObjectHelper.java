/*
 * @author norbert
 * @date 04.10.2006
 * @version $Id: ObjectHelper.java,v 1.7 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.dbcopy.CopyController;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.ExcludedObject;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.lib.db.DB;
import com.gentics.lib.etc.StringUtils;

/**
 * @author norbert
 *
 */
public final class ObjectHelper {
	protected static Map<Integer, String> tableMap = new HashMap<Integer, String>();

	public final static Integer T_NODE = new Integer(10001);
	public final static Integer T_FOLDER = new Integer(10002);
	public final static Integer T_TEMPLATE = new Integer(10006);
	public final static Integer T_PAGE = new Integer(10007);
	public final static Integer T_FILE = new Integer(10008);
	public final static Integer T_IMAGE = new Integer(10011);
	public final static Integer T_CONTENTTAG = new Integer(10111);
	public final static Integer T_TEMPLATETAG = new Integer(10112);

	static {
		tableMap.put(T_NODE, "foldernode");
		tableMap.put(T_FOLDER, "folder");
		tableMap.put(T_TEMPLATE, "template");
		tableMap.put(T_PAGE, "page");
		tableMap.put(T_FILE, "contentfile");
		tableMap.put(T_IMAGE, "contentimagefile");
		tableMap.put(T_CONTENTTAG, "contenttag");
		tableMap.put(T_TEMPLATETAG, "templatetag");
	}

	public static String typeToTable(Integer type) {
		return (String) tableMap.get(type);
	}

	public static Integer[] tableToTypes(String tableName) {
		List<Integer> types = new Vector<Integer>();

		for (Map.Entry<Integer, String> entry : tableMap.entrySet()) {
			if (entry.getValue().equals(tableName)) {
				types.add(entry.getKey());
			}
		}

		return (Integer[]) types.toArray(new Integer[types.size()]);
	}

	/**
	 * Get objects from the node db
	 * @param copy copy configuration
	 * @param table table from which to fetch the objects
	 * @param restriction restriction string (might be null or empty for no
	 *        further restriction)
	 * @param params parameters to be used in the restriction
	 * @param allObjects map of all already fetched objects
	 * @param referenceName name of the reference which caused the objects to be
	 *        fetched (fetched objects will have the reference with this name
	 *        set to the given object)
	 * @param referencedObject referenced object which caused the objects to be
	 *        fetched
	 * @return list of fetched objects
	 * @throws StructureCopyException in case of errors
	 */
	public static List<DBObject> getObjectsFromNodeDB(StructureCopy copy, Table table, String fromClause,
			String restriction, Object[] params, Map<StructureCopy.ObjectKey, DBObject> allObjects, String referenceName,
			DBObject referencedObject) throws StructureCopyException {
		try {
			StringBuffer sql = new StringBuffer();

			sql.append("SELECT ").append(table.getSelectedFields());
			sql.append(" FROM ").append(fromClause);

			boolean restrictionSet = false;

			if (table.isSetRestrict()) {
				sql.append(" WHERE (");
				sql.append(copy.resolveProperties(table.getRestrict()));
				sql.append(")");
				restrictionSet = true;
			}
			if (restriction != null && restriction.length() > 0) {
				sql.append(restrictionSet ? " AND " : " WHERE ");
				sql.append("(");
				sql.append(copy.resolveProperties(restriction));
				sql.append(")");
			}

			PreparedStatement st = null;
			ResultSet res = null;
			List<DBObject> fetchedObjects = new Vector<DBObject>();

			try {
				st = copy.getConnection().prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

				int paramCounter = 1;

				if (params != null) {
					for (int i = 0; i < params.length; i++) {
						st.setObject(paramCounter++, params[i]);
					}
				}
				res = st.executeQuery();
				while (res.next()) {
					Object id = table.getId(res);
					StructureCopy.ObjectKey key = StructureCopy.ObjectKey.getObjectKey(table, id);

					// check whether the object was already fetched
					if (!allObjects.containsKey(key)) {
						// check whether the object shall be excluded
						switch (copy.getCopyController().isExcluded(copy, table, id)) {
						case CopyController.EXCLUSION_NULL:
							// excluded (replaced with null)
							continue;

						case CopyController.EXCLUSION_NOTED:
							// excluded (replaced with placeholder)
							DBObject excludedObject = new ExcludedObject(table,
									ObjectHelper.resolveObjectName(copy.getConnection(), copy, table, ObjectTransformer.getInt(id, -1)));

							excludedObject.setOriginalId(id);
							allObjects.put(key, excludedObject);
							break;

						case CopyController.EXCLUSION_NO:
						default:
							// not excluded
							// table.updateReferenceColumns(res.getMetaData());
							DBObject object = new DBObject(table, id, res, referencedObject, referenceName, false);

							allObjects.put(key, object);

							// immediately set the object reference for the object
							if (referenceName != null && referencedObject != null) {
								object.setReference(referenceName, referencedObject);
							}

							fetchedObjects.add(object);

							table.getObjectLinks(copy, copy.getConnection(), allObjects, object, true);
						}

					} else {
						fetchedObjects.add(allObjects.get(key));
					}
				}
			} finally {
				DB.close(res);
				DB.close(st);
			}

			return fetchedObjects;
		} catch (SQLException e) {
			throw new StructureCopyException("Error while getting objects", e);
		}
	}

	/**
	 * Get a single object by its id.
	 * @param copy copy configuration
	 * @param table table of the object
	 * @param id object id
	 * @param referenceName name of the reference
	 * @param referencingObject referencing object
	 * @param checkForExclusions whether exclusions shall be checked or not
	 * @return the object or null if not found
	 * @throws StructureCopyException
	 */
	public static DBObject getObjectFromNodeDBByID(StructureCopy copy, Table table, Object id,
			String referenceName, DBObject referencingObject, boolean checkForExclusions) throws StructureCopyException {
		// do not get the object when it is excluded
		if (checkForExclusions) {
			switch (copy.getCopyController().isExcluded(copy, table, id)) {
			case CopyController.EXCLUSION_NULL:
				return null;

			case CopyController.EXCLUSION_NOTED:
				DBObject excludedObject = new ExcludedObject(table,
						ObjectHelper.resolveObjectName(copy.getConnection(), copy, table, ObjectTransformer.getInt(id, -1)));

				excludedObject.setOriginalId(id);
				return excludedObject;
			}
		}

		// fetch the single object here
		StringBuffer sql = new StringBuffer();
		List<Object> sqlData = new Vector<Object>();

		sql.append("SELECT ").append(table.getSelectedFields());
		sql.append(" FROM `").append(table.getName());
		sql.append("` WHERE ");
		sql.append("(");
		if (table.isCrossTable()) {
			String[] crossTableId = table.getCrossTableId();

			sql.append(StringUtils.merge(crossTableId, " AND ", "`" + table.getName() + "`.", " = ?"));
			if (id instanceof Table.MultiReferenceKey) {
				Table.MultiReferenceKey key = (Table.MultiReferenceKey) id;
				Object[] references = key.getReferences();

				for (int i = 0; i < references.length; i++) {
					sqlData.add(references[i]);
				}
			} else {
				sqlData.add(id);
			}
		} else {
			sql.append('`').append(table.getName()).append("`.").append(table.getIdcol());
			sql.append(" = ?");
			sqlData.add(id);
		}
		sql.append(")");
		if (table.isSetRestrict()) {
			sql.append(" AND (");
			sql.append(copy.resolveProperties(table.getRestrict()));
			sql.append(")");
		}

		PreparedStatement st = null;
		ResultSet res = null;

		try {
			Connection conn = copy.getConnection();

			st = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

			int paramCounter = 1;

			for (Object data : sqlData) {
				st.setObject(paramCounter++, data);
			}

			res = st.executeQuery();
			if (res.next()) {
				DBObject object = new DBObject(table, id, res, referencingObject, referenceName, false);

				return object;
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new StructureCopyException(e);
		} finally {
			DB.close(res);
			DB.close(st);
		}
	}

	/**
	 * Resolve names from local objects
	 * @param conn
	 * @param copy
	 * @param table the associated table
	 * @param localId the object's local id
	 * @return name of the object, null if not found
	 * @throws StructureCopyException
	 */
	public static String resolveObjectName(Connection conn, StructureCopy copy,
			String tableName, int localId) throws StructureCopyException {
		return resolveObjectName(conn, copy, copy.getTables().getTable(tableName), localId);
	}

	/**
	 * Resolve names from local objects
	 * @param conn
	 * @param copy
	 * @param tableName the associated table
	 * @param localId the object's local id
	 * @return name of the object, null if not found
	 * @throws StructureCopyException
	 */
	public static String resolveObjectName(Connection conn,
			StructureCopy copy, Table table, int localId) throws StructureCopyException {
		if (table == null) {
			return null;
		}

		PreparedStatement st = null;
		ResultSet res = null;

		if ("objtagdef".equals(table.getId())) {
			try {
				st = conn.prepareStatement("SELECT id FROM objprop WHERE objtag_id = ? LIMIT 1");
				st.setInt(1, localId);
				res = st.executeQuery();

				if (res.next()) {
					return resolveObjectName(conn, copy, "objprop", res.getInt("id"));
				} else {
					DB.close(res);
					DB.close(st);
					st = conn.prepareStatement("SELECT name FROM objtag WHERE id = ?");
					st.setInt(1, localId);
					res = st.executeQuery();

					if (res.next()) {
						return res.getString("name");
					}
				}
			} catch (SQLException e) {
				throw new StructureCopyException("could not resolve object name from tablecolum {" + table.getName() + "} for id {" + localId + "}", e);
			} finally {
				DB.close(res);
				DB.close(st);
			}
		}

		// try name colum first
		String nameColumn = table.getProperty("namecolumn");

		if (nameColumn != null) {
			try {
				st = conn.prepareStatement("SELECT " + nameColumn + " FROM `" + table.getName() + "` WHERE id = ?");
				st.setInt(1, localId);
				res = st.executeQuery();
				while (res.next()) {
					return res.getString(nameColumn);
				}
			} catch (SQLException e) {
				throw new StructureCopyException(
						"could not resolve object name from tablecolum {" + table.getName() + "}.{" + nameColumn + "} for id {" + localId + "}", e);
			} finally {
				DB.close(res);
				DB.close(st);
			}
		}

		// try name reference
		String nameReference = table.getProperty("namereference");

		if (nameReference != null) {
			Integer languageId = ObjectTransformer.getInteger(copy.resolveProperties("${language}"), new Integer(1));

			try {
				st = conn.prepareStatement(
						"SELECT du.value " + "FROM `" + table.getName() + "` LEFT JOIN dicuser du " + "ON du.output_id = `" + table.getName() + "`." + nameReference + " "
						+ "WHERE `" + table.getName() + "`.id = ? AND du.language_id = ?");
				st.setInt(1, localId);
				st.setObject(2, languageId);
				res = st.executeQuery();
				while (res.next()) {
					return res.getString("value");
				}
			} catch (SQLException e) {
				throw new StructureCopyException(
						"could not resolve object name from dicuser table" + " referenced via tablecolum {" + table.getName() + "}.{" + nameReference + "} for id {"
						+ localId + "}",
						e);
			} finally {
				DB.close(res);
				DB.close(st);
			}
		}

		// no name found... sorry.
		return null;
	}
}
