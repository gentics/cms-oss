/*
 * @author norbert
 * @date 05.10.2006
 * @version $Id: ValueMapper.java,v 1.9 2009-12-16 16:12:13 herbert Exp $
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
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.ReferenceDescriptor;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBReferenceType.Parameter;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.lib.db.DB;

/**
 * @author norbert
 */
public class ValueMapper extends AbstractReferenceDescriptor implements ReferenceDescriptor {
	protected Map<Object, Integer> typeMap = new HashMap<Object, Integer>();

	protected Map<Object, Integer> typeIdMap = new HashMap<Object, Integer>();

	protected Table[] targets;

	protected Integer[] targetTypes = new Integer[] {
		ObjectHelper.T_PAGE, ObjectHelper.T_FILE, ObjectHelper.T_IMAGE, ObjectHelper.T_FOLDER,
		ObjectHelper.T_TEMPLATETAG, ObjectHelper.T_CONTENTTAG};

	protected static Map<Integer, Integer> types = new HashMap<Integer, Integer>();

	protected static Map<Integer, Integer[]> reverseTypes = new HashMap<Integer, Integer[]>();

	static {
		types.put(new Integer(4), ObjectHelper.T_PAGE);
		types.put(new Integer(6), ObjectHelper.T_IMAGE);
		types.put(new Integer(8), ObjectHelper.T_FILE);
		types.put(new Integer(11), ObjectHelper.T_CONTENTTAG);
		types.put(new Integer(14), ObjectHelper.T_FILE);
		types.put(new Integer(20), ObjectHelper.T_TEMPLATETAG);
		types.put(new Integer(25), ObjectHelper.T_FOLDER);

		reverseTypes.put(ObjectHelper.T_PAGE, new Integer[] { new Integer(4)});
		reverseTypes.put(ObjectHelper.T_CONTENTTAG, new Integer[] { new Integer(11)});
		reverseTypes.put(ObjectHelper.T_FILE, new Integer[] { new Integer(8), new Integer(14)});
		reverseTypes.put(ObjectHelper.T_IMAGE, new Integer[] { new Integer(6)});
		reverseTypes.put(ObjectHelper.T_TEMPLATETAG, new Integer[] { new Integer(20)});
		reverseTypes.put(ObjectHelper.T_FOLDER, new Integer[] { new Integer(25)});
	}

	/**
	 * Create an instance of this ReferenceDescriptor
	 * @param sourceTable source table
	 * @param tables tables
	 * @param reference reference
	 */
	public ValueMapper(Table sourceTable, Tables tables, Reference reference) {
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
			res = st.executeQuery("SELECT id, type_id FROM part WHERE type_id IN (4, 6, 8, 11, 14, 20, 25)");
			while (res.next()) {
				Integer type = res.getInt("type_id");
				Object id = res.getObject("id");

				typeMap.put(id, types.get(type));
				typeIdMap.put(id, type);
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

	/**
	 * Check whether the part with given id is of type Tag (Page) or Tag (Template)
	 * @param partId part id
	 * @return true for Tag PartTypes, false for other
	 */
	protected boolean isTagPartType(Object partId) {
		int typeId = ObjectTransformer.getInt(typeIdMap.get(partId), -1);

		return typeId == 11 || typeId == 20;
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
			sqlClause.append(") and value.value_ref = ?");
			params[params.length - 1] = object.getId();

			return sourceTable.getObjects(copy, conn, "value LEFT JOIN part ON value.part_id = part.id", sqlClause.toString(), params, allObjects,
					getLinkColumn(), object);
		}
		return null;
	}

	/**
	 * Get the target table for the reference from the given object
	 * @param object object
	 * @return target table (or null)
	 */
	public Table getTargetTable(StructureCopy copy, DBObject object) throws StructureCopyException {
		Table targetTable = getReferencedObjectTable(object);

		if (targetTable != null) {
			return targetTable;
		}
		Object partId = object.getColValue("part_id");

		if (partId instanceof Integer) {
			Integer type = (Integer) typeMap.get(partId);
			Table target = tables.getTable(ObjectHelper.typeToTable(type));

			// for pagetags, the link might go to a templatetag
			if (isTagPartType(partId) && "t".equals(object.getColValue("value_text"))) {
				target = tables.getTable(ObjectHelper.typeToTable(ObjectHelper.T_TEMPLATETAG));
			}

			return target;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor#isReferenceValueNeeded(java.sql.ResultSet, java.lang.String)
	 */
	public boolean isReferenceValueNeeded(ResultSet res, String referenceColumnName) throws SQLException {
		// special implementation for value_text
		if ("value_text".equals(referenceColumnName)) {
			Object partId = res.getObject("part_id");

			if (partId instanceof Integer) {
				// get the part type
				if (isTagPartType(partId)) {
					return true;
				} else {
					// all other types do not need the value_text as reference
					// information
					return false;
				}
			} else {
				// we found no part_id, so the value_text will not be needed as reference information
				return false;
			}
		} else {
			// all other reference columns are needed according to the default implementation
			return super.isReferenceValueNeeded(res, referenceColumnName);
		}
	}

	// /*
	// * (non-Javadoc)
	// * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#isObjectLinked(com.gentics.contentnode.dbcopy.StructureCopy,
	// *      com.gentics.contentnode.dbcopy.DBObject)
	// */
	// public boolean isObjectLinked(StructureCopy copy, DBObject sourceObject) throws StructureCopyException {
	//
	// return false;
	// }

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
		return new String[] { "value_ref", "part_id", "value_text"};
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.AbstractReferenceDescriptor#getLinkColumn()
	 */
	public String getLinkColumn() {
		return "value_ref";
	}

	@Override
	public void handleUnsatisfiedReference(StructureCopy copy, DBObject object, Table table, Object id) throws StructureCopyException {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			Table targetTable = getTargetTable(copy, object);
			if (targetTable != null) {
				String targetTableName = targetTable.getName();
				Class<? extends NodeObject> clazz = null;
	
				if ("contentfile".equals(targetTableName)) {
					clazz = File.class;
				} else if ("page".equals(targetTableName)) {
					clazz = Page.class;
				} else if ("folder".equals(targetTableName)) {
					clazz = Folder.class;
				}
	
				if (clazz != null && t.getObject(clazz, ObjectTransformer.getInt(id, 0)) == null) {
					unsetReference(object, table, id);
				}
			}
		} catch (NodeException e) {
			throw new StructureCopyException(e);
		}
	}

	/**
	 * Update the reference to 0
	 * @param object object
	 * @param table target table
	 * @param id target id
	 */
	protected void unsetReference(DBObject object, Table table, Object id) {
		DBObject dummy = new DBObject(table, "");
		dummy.setOriginalId(id);
		dummy.setNewId(0);
		object.setReference(getLinkColumn(), dummy);
	}
}
