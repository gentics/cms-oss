/*
 * @author norbert
 * @date 03.10.2006
 * @version $Id: NormalReference.java,v 1.6 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType.ParameterType;

/**
 * Normal reference from one table to another, the link is defined by exactly
 * one column and always links to the same table
 */
public class NormalReference extends AbstractReferenceDescriptor implements ReferenceDescriptor {

	/**
	 * target of the reference
	 */
	protected Table targetTable;

	/**
	 * referencing column
	 */
	protected String linkColumn;

	/**
	 * Create instance of the normal reference
	 * @param sourceTable source table
	 * @param targetTable target table
	 * @param linkColumn referencing column
	 * @param reference reference
	 */
	public NormalReference(Table sourceTable, Table targetTable, String linkColumn, Reference reference) {
		super(sourceTable, null, reference);
		this.sourceTable = sourceTable;
		this.targetTable = targetTable;
		this.linkColumn = linkColumn;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#init(java.sql.Connection)
	 */
	public void init(Connection conn, ParameterType[] parameter) throws StructureCopyException {// nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#getLinkingObjects(java.sql.Connection,
	 *      com.gentics.DBObject)
	 */
	public List<DBObject> getLinkingObjects(StructureCopy copy, Connection conn, DBObject object,
			Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException {
		return sourceTable.getObjects(copy, conn, "`" + sourceTable.getName() + "`." + linkColumn + " = ?", new Object[] { object.getId()}, allObjects, getLinkColumn(),
				object);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#getPossibleTargets()
	 */
	public Table[] getPossibleTargets() {
		return new Table[] { targetTable};
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.ReferenceDescriptor#getReferenceColumns()
	 */
	public String[] getReferenceColumns() {
		return new String[] { linkColumn};
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.AbstractReferenceDescriptor#getLinkColumn()
	 */
	public String getLinkColumn() {
		return linkColumn;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#isObjectLinked(com.gentics.contentnode.dbcopy.StructureCopy, com.gentics.contentnode.dbcopy.DBObject)
	 */
	public boolean isObjectLinked(StructureCopy copy, DBObject sourceObject) throws StructureCopyException {
		// an object is linked, when the link column holds a positive number
		return isValidReference(sourceObject.getColValue(linkColumn));
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor#getTargetTable(com.gentics.contentnode.dbcopy.DBObject)
	 */
	public Table getTargetTable(StructureCopy copy, DBObject object) throws StructureCopyException {
		return targetTable;
	}
}
