/*
 * @author norbert
 * @date 25.02.2008
 * @version $Id: ValueDatasourceMapper.java,v 1.1 2008-02-26 10:39:33 norbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;

/**
 * Implementation for the reference descriptor value -&gt; datasource
 */
public class ValueDatasourceMapper extends AbstractValueDSMapper {

	/**
	 * target tables
	 */
	protected Table[] targetTables;

	/**
	 * reference columns
	 */
	protected final static String[] REFCOL = new String[] { "value_ref"};

	/**
	 * Create an instance of this reference descriptor
	 * @param sourceTable source table
	 * @param tables tables
	 * @param reference reference
	 */
	public ValueDatasourceMapper(Table sourceTable, Tables tables, Reference reference) {
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
		return isDatasourceValue(object) ? targetTables[0] : null;
	}
}
