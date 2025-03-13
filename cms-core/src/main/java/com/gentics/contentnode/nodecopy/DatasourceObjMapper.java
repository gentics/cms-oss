/*
 * @author norbert
 * @date 04.10.2006
 * @version $Id: DatasourceObjMapper.java,v 1.7 2008-02-26 10:39:33 norbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.ReferenceDescriptor;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBReferenceType.Parameter;
import com.gentics.contentnode.object.Overview;
import com.gentics.lib.db.DB;

/**
 * @author norbert
 */
public class DatasourceObjMapper extends AbstractReferenceDescriptor implements ReferenceDescriptor {

	protected Map<Object, Integer> dsMap = new HashMap<Object, Integer>();

	protected Table[] targets;

	/**
	 * Create an instance of this ReferenceDescriptor
	 * @param sourceTable source table
	 * @param tables tables
	 * @param reference reference
	 */
	public DatasourceObjMapper(Table sourceTable, Tables tables, Reference reference) {
		super(sourceTable, tables, reference);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#init(java.sql.Connection,
	 *      com.gentics.jaxb.JAXBreferenceType.ParameterType[])
	 */
	public void init(Connection conn, Parameter[] parameter) throws StructureCopyException {
		Statement st = null;
		ResultSet res = null;
		List<Table> targetTables = new Vector<Table>();

		try {
			st = conn.createStatement();
			res = st.executeQuery("SELECT id, o_type, is_folder FROM ds");
			while (res.next()) {
				int selectionType = res.getInt("is_folder");

				switch (selectionType) {
				case Overview.SELECTIONTYPE_FOLDER: {
					// folders are selected
					dsMap.put(res.getObject("id"), new Integer(10002));
					Table t = tables.getTable(ObjectHelper.typeToTable(new Integer(10002)));

					if (t != null && !targetTables.contains(t)) {
						targetTables.add(t);
					}
					break;
				}

				case Overview.SELECTIONTYPE_SINGLE: {
					// the overview objects are selected directly
					dsMap.put(res.getObject("id"), ObjectTransformer.getInteger(res.getObject("o_type"), null));
					Table t = tables.getTable(ObjectHelper.typeToTable(new Integer(res.getInt("o_type"))));

					if (t != null && !targetTables.contains(t)) {
						targetTables.add(t);
					}
					break;
				}

				default:
					// no objects are selected (but fetched from the current folder)
					break;
				}
			}

			targets = (Table[]) targetTables.toArray(new Table[targetTables.size()]);
		} catch (SQLException e) {
			throw new StructureCopyException(e);
		} finally {
			DB.close(res);
			DB.close(st);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#getLinkingObjects(com.gentics.StructureCopy,
	 *      java.sql.Connection, com.gentics.DBObject, java.util.Map)
	 */
	public List<DBObject> getLinkingObjects(StructureCopy copy, Connection conn, DBObject object,
			Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException {
		Integer[] types = ObjectHelper.tableToTypes(object.getSourceTable().getName());

		if (types.length != 0) {

			StringBuffer whereClause = new StringBuffer("ds.o_type IN (");
			boolean first = true;

			for (int i = 0; i < types.length; i++) {
				if (first) {
					first = false;
				} else {
					whereClause.append(",");
				}
				whereClause.append("?");
			}
			whereClause.append(") AND ds_obj.o_id = ?");

			Object[] params = new Object[types.length + 1];

			System.arraycopy(types, 0, params, 0, types.length);
			params[params.length - 1] = object.getId();
			return sourceTable.getObjects(copy, conn, "ds_obj LEFT JOIN ds ON ds_obj.ds_id = ds.id", whereClause.toString(), params, allObjects, getLinkColumn(),
					object);
		} else {
			return null;
		}
	}

	/**
	 * Get the target table for the link of the object
	 * @param object object
	 * @return target table or null
	 */
	public Table getTargetTable(StructureCopy copy, DBObject object) throws StructureCopyException {
		Table targetTable = getReferencedObjectTable(object);

		if (targetTable != null) {
			return targetTable;
		}
		Object type = dsMap.get(object.getColValue("ds_id"));

		if (type instanceof Integer) {
			return tables.getTable(ObjectHelper.typeToTable((Integer) type));
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#getPossibleTargets()
	 */
	public Table[] getPossibleTargets() {
		return targets;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#getReferenceColumns()
	 */
	public String[] getReferenceColumns() {
		return new String[] { "o_id"};
	}

	/* (non-Javadoc)
	 * @see com.gentics.AbstractReferenceDescriptor#getLinkColumn()
	 */
	public String getLinkColumn() {
		return "o_id";
	}
}
