/*
 * @author norbert
 * @date 04.10.2006
 * @version $Id: ObjectMapper.java,v 1.8 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
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
import com.gentics.contentnode.dbcopy.jaxb.JAXBReferenceType.Parameter;

/**
 * @author norbert
 */
public class ObjectMapper extends AbstractReferenceDescriptor implements ReferenceDescriptor {
	protected Map<Integer, Table> targets = new HashMap<Integer, Table>();

	protected String idColumn = "o_id";

	protected String typeColumn = "o_type";

	/**
	 * Create an instance of this ReferenceDescriptor
	 * @param sourceTable source table
	 * @param tables tables
	 * @param reference reference
	 */
	public ObjectMapper(Table sourceTable, Tables tables, Reference reference) {
		super(sourceTable, tables, reference);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#getLinkingObjects(com.gentics.StructureCopy,
	 *      java.sql.Connection, com.gentics.DBObject, java.util.Map)
	 */
	public List<DBObject> getLinkingObjects(StructureCopy copy, Connection conn, DBObject object,
			Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException {
		List<DBObject> objects = new Vector<DBObject>();
		Table targetTable = object.getSourceTable();

		for (Map.Entry<Integer, Table> entry : targets.entrySet()) {
			if (targetTable.equals(entry.getValue())) {
				objects.addAll(
						sourceTable.getObjects(copy, conn, sourceTable.getName() + "." + idColumn + " = ? && " + sourceTable.getName() + "." + typeColumn + " = ?",
						new Object[] { object.getId(), entry.getKey()}, allObjects, getLinkColumn(), object));
                
				// perm entries for nodes need to be copied to folders too...
				if ("perm".equals(sourceTable.getName()) && entry.getKey().equals(ObjectHelper.T_NODE)) {
					objects.addAll(
							sourceTable.getObjects(copy, conn, sourceTable.getName() + "." + idColumn + " = ? && " + sourceTable.getName() + "." + typeColumn + " = ?",
							new Object[] { object.getId(), ObjectHelper.T_FOLDER}, allObjects, getLinkColumn(), object));
				}
			}
		}
		return objects;
	}

	/**
	 * Get the target table for the reference in the given object
	 * @param object object
	 * @return target table or null
	 */
	public Table getTargetTable(StructureCopy copy, DBObject object) throws StructureCopyException {
		Table targetTable = getReferencedObjectTable(object);

		if (targetTable != null) {
			return targetTable;
		}
		Object type = object.getColValue(typeColumn);

		return (Table) targets.get(type);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#init(java.sql.Connection)
	 */
	public void init(Connection conn, Parameter[] parameter) throws StructureCopyException {
		for (int i = 0; i < parameter.length; i++) {
			if ("idcol".equals(parameter[i].getId())) {
				idColumn = parameter[i].getValue();
			} else if ("typecol".equals(parameter[i].getId())) {
				typeColumn = parameter[i].getValue();
			}
		}

		// node
		Table nodeTable = tables.getTable(ObjectHelper.typeToTable(ObjectHelper.T_NODE));

		if (nodeTable != null) {
			targets.put(ObjectHelper.T_NODE, nodeTable);
		}

		// folder
		Table folderTable = tables.getTable(ObjectHelper.typeToTable(ObjectHelper.T_FOLDER));

		if (folderTable != null) {
			targets.put(ObjectHelper.T_FOLDER, folderTable);
		}

		// template
		Table templateTable = tables.getTable(ObjectHelper.typeToTable(ObjectHelper.T_TEMPLATE));

		if (templateTable != null) {
			targets.put(ObjectHelper.T_TEMPLATE, templateTable);
		}

		// page
		Table pageTable = tables.getTable(ObjectHelper.typeToTable(ObjectHelper.T_PAGE));

		if (pageTable != null) {
			targets.put(ObjectHelper.T_PAGE, pageTable);
		}

		// file
		Table fileTable = tables.getTable(ObjectHelper.typeToTable(ObjectHelper.T_FILE));

		if (fileTable != null) {
			targets.put(ObjectHelper.T_FILE, fileTable);
		}

		// image
		Table imageTable = tables.getTable(ObjectHelper.typeToTable(ObjectHelper.T_IMAGE));

		if (imageTable != null) {
			targets.put(ObjectHelper.T_IMAGE, imageTable);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#getPossibleTargets()
	 */
	public Table[] getPossibleTargets() {
		return (Table[]) targets.values().toArray(new Table[targets.values().size()]);
	}

	/* (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#getReferenceColumns()
	 */
	public String[] getReferenceColumns() {
		return new String[] { idColumn, typeColumn};
	}

	/* (non-Javadoc)
	 * @see com.gentics.AbstractReferenceDescriptor#getLinkColumn()
	 */
	public String getLinkColumn() {
		return idColumn;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor#updateReference(com.gentics.contentnode.dbcopy.DBObject, com.gentics.contentnode.dbcopy.Table, java.lang.Integer, java.lang.Object)
	 */
	public void updateReference(DBObject sourceObject, Table targetTable, Integer ttype, Object targetId) throws StructureCopyException {
		super.updateReference(sourceObject, targetTable, ttype, targetId);
		sourceObject.setColValue(typeColumn, ttype);
	}
}
