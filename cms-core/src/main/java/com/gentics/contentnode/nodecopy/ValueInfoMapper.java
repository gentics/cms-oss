/*
 * @author norbert
 * @date 04.02.2008
 * @version $Id: ValueInfoMapper.java,v 1.6 2009-12-16 16:12:13 herbert Exp $
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

import com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.ReferenceDescriptor;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType.ParameterType;
import com.gentics.lib.db.DB;

/**
 * Reference descriptor for values for parts of type 11 (pagetag) or 20 (templatetag), linking to the selected page or template
 */
public class ValueInfoMapper extends AbstractReferenceDescriptor implements ReferenceDescriptor {
	protected Map<Object, Integer> typeMap = new HashMap<Object, Integer>();

	protected Table[] targets;

	protected Integer[] targetTypes = new Integer[] {
		ObjectHelper.T_PAGE, ObjectHelper.T_FILE, ObjectHelper.T_FOLDER, ObjectHelper.T_TEMPLATETAG,
		ObjectHelper.T_CONTENTTAG};

	protected static Map<Integer, Integer> types = new HashMap<Integer, Integer>();

	protected static Map<Integer, Integer[]> reverseTypes = new HashMap<Integer, Integer[]>();

	static {
		types.put(new Integer(11), ObjectHelper.T_PAGE);
		types.put(new Integer(20), ObjectHelper.T_TEMPLATE);

		reverseTypes.put(ObjectHelper.T_PAGE, new Integer[] { new Integer(11)});
		reverseTypes.put(ObjectHelper.T_TEMPLATE, new Integer[] { new Integer(20)});
	}

	/**
	 * @param sourceTable
	 * @param tables
	 * @param reference
	 */
	public ValueInfoMapper(Table sourceTable, Tables tables, Reference reference) {
		super(sourceTable, tables, reference);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#init(java.sql.Connection,
	 *      com.gentics.jaxb.JAXBreferenceType.ParameterType[])
	 */
	public void init(Connection conn, ParameterType[] parameter) throws StructureCopyException {
		Statement st = null;
		ResultSet res = null;
		List<Table> targetTables = new Vector<Table>();

		try {
			st = conn.createStatement();
			res = st.executeQuery("SELECT id, type_id FROM part WHERE type_id IN (11, 20)");
			while (res.next()) {
				Integer type = res.getInt("type_id");
				Object id = res.getObject("id");

				typeMap.put(id, types.get(type));
			}

			for (int i = 0; i < targetTypes.length; i++) {
				Table t = tables.getTable(ObjectHelper.typeToTable(targetTypes[i]));

				if (t != null && !targetTables.contains(t)) {
					targetTables.add(t);
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
		Integer[] types = (Integer[]) reverseTypes.get(object.getSourceTable());

		if (types != null) {
			StringBuffer sqlClause = new StringBuffer("part.type_id in (");
			Object[] params = new Object[types.length + 1];
			boolean first = true;

			for (int i = 0; i < types.length; i++) {
				if (first) {
					first = false;
				} else {
					sqlClause.append(", ");
				}
				sqlClause.append("?");
				params[i + 1] = types;
			}
			sqlClause.append(") and value.info = ?");
			params[params.length - 1] = object.getId();

			return sourceTable.getObjects(copy, conn, "value LEFT JOIN part ON value.part_id = part.id", sqlClause.toString(), params, allObjects,
					getLinkColumn(), object);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor#getTargetTable(com.gentics.contentnode.dbcopy.DBObject)
	 */
	public Table getTargetTable(StructureCopy copy, DBObject object) throws StructureCopyException {
		Table targetTable = getReferencedObjectTable(object);

		if (targetTable != null) {
			return targetTable;
		}
		Object partId = object.getColValue("part_id");

		if (partId instanceof Integer) {
			Integer type = (Integer) typeMap.get(partId);

			return tables.getTable(ObjectHelper.typeToTable(type));
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
		return new String[] { "info", "part_id"};
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.AbstractReferenceDescriptor#getLinkColumn()
	 */
	public String getLinkColumn() {
		return "info";
	}
}
