/*
 * @author norbert
 * @date 25.02.2008
 * @version $Id: PartDatasourceMapper.java,v 1.2 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBReferenceType.Parameter;

/**
 * Implementation of the reference descriptor for the reference part -&gt;
 * datasource, for parts of type (29, 30 or 32).
 */
public class PartDatasourceMapper extends AbstractReferenceDescriptor {

	/**
	 * target tables
	 */
	protected Table[] targetTables;

	/**
	 * reference columns
	 */
	protected final static String[] REFCOL = new String[] { "info_int", "type_id"};

	/**
	 * Create instance of this reference descriptor
	 * @param sourceTable source table
	 * @param tables tables configuration
	 * @param reference reference
	 */
	public PartDatasourceMapper(Table sourceTable, Tables tables, Reference reference) {
		super(sourceTable, tables, reference);
		targetTables = new Table[] { tables.getTable("datasource")};
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getLinkColumn()
	 */
	public String getLinkColumn() {
		return REFCOL[0];
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getLinkingObjects(com.gentics.contentnode.dbcopy.StructureCopy, java.sql.Connection, com.gentics.contentnode.dbcopy.DBObject, java.util.Map)
	 */
	public List<DBObject> getLinkingObjects(StructureCopy copy, Connection conn, DBObject object,
			Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException {
		return Collections.emptyList();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getPossibleTargets()
	 */
	public Table[] getPossibleTargets() {
		return targetTables;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getReferenceColumns()
	 */
	public String[] getReferenceColumns() {
		return REFCOL;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getTargetTable(com.gentics.contentnode.dbcopy.StructureCopy, com.gentics.contentnode.dbcopy.DBObject)
	 */
	public Table getTargetTable(StructureCopy copy, DBObject object) throws StructureCopyException {
		Table targetTable = getReferencedObjectTable(object);

		if (targetTable != null) {
			return targetTable;
		}
		int type = ObjectTransformer.getInt(object.getColValue("type_id"), -1);

		switch (type) {
		case 29:
		case 30:
		case 32:
			return targetTables[0];

		default:
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#init(java.sql.Connection, com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType.ParameterType[])
	 */
	public void init(Connection conn, Parameter[] parameter) throws StructureCopyException {// nothing to do here
	}
}
