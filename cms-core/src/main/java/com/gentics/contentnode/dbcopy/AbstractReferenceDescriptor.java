/*
 * @author norbert
 * @date 05.10.2006
 * @version $Id: AbstractReferenceDescriptor.java,v 1.7 2008-11-10 10:54:29 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.gentics.lib.etc.StringUtils;

/**
 * Abstract implementation for reference descriptors
 */
abstract public class AbstractReferenceDescriptor implements ReferenceDescriptor {

	/**
	 * source table
	 */
	protected Table sourceTable;

	/**
	 * all tables
	 */
	protected Tables tables;

	/**
	 * the reference
	 */
	protected Reference reference;

	/**
	 * copytype
	 */
	protected int referenceCopyType;

	/**
	 * foreign copytype
	 */
	protected int referenceForeignCopyType;

	/**
	 * Create an instance of this ReferenceDescriptor
	 * @param sourceTable source table
	 * @param tables tables
	 * @param reference reference
	 */
	public AbstractReferenceDescriptor(Table sourceTable, Tables tables, Reference reference) {
		this.sourceTable = sourceTable;
		this.tables = tables;
		this.reference = reference;
		String deepCopy = this.reference.getDeepcopy().value();

		if ("false".equals(deepCopy)) {
			referenceCopyType = COPYTYPE_FALSE;
		} else if ("ask".equals(deepCopy)) {
			referenceCopyType = COPYTYPE_ASK;
		} else {
			referenceCopyType = COPYTYPE_TRUE;
		}
		String foreigndeepcopy = this.reference.getForeigndeepcopy().value();

		if ("false".equals(foreigndeepcopy)) {
			referenceForeignCopyType = COPYTYPE_FALSE;
		} else if ("ask".equals(foreigndeepcopy)) {
			referenceForeignCopyType = COPYTYPE_ASK;
		} else {
			referenceForeignCopyType = COPYTYPE_TRUE;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#updateReference(com.gentics.contentnode.dbcopy.DBObject,
	 *      com.gentics.contentnode.dbcopy.Table, java.lang.Integer,
	 *      java.lang.Object)
	 */
	public void updateReference(DBObject sourceObject, Table targetTable, Integer ttype, Object targetId) throws StructureCopyException {
		// check whether the object is from the sourcetable
		if (sourceObject == null) {
			return;
		}
		if (!sourceTable.equals(sourceObject.getSourceTable())) {
			throw new StructureCopyException("Could not update reference for object, the source table is different");
		}
		sourceObject.setColValue(getLinkColumn(), targetId);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getSourceTable()
	 */
	public Table getSourceTable() {
		return sourceTable;
	}

	/**
	 * Check whether the given object has an object referenced
	 * @param copy copy configuration
	 * @param sourceObject object
	 * @return true when the given object has the reference set, false if not
	 */
	public boolean isObjectLinked(StructureCopy copy, DBObject sourceObject) throws StructureCopyException {
		return sourceObject.getColValue(getLinkColumn()) instanceof StructureCopy.ObjectKey
				|| (getTargetTable(copy, sourceObject) != null && isValidReference(sourceObject.getColValue(getLinkColumn())));
	}

	/**
	 * Check whether the given object is a valid object reference
	 * @param object object
	 * @return true for valid object references, false for empty references
	 */
	protected boolean isValidReference(Object object) {
		return object != null && !"0".equals(object.toString());
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getLinkedObject(com.gentics.contentnode.dbcopy.StructureCopy, java.sql.Connection, com.gentics.contentnode.dbcopy.DBObject, java.util.Map, boolean)
	 */
	public DBObject getLinkedObject(StructureCopy copy, Connection conn, DBObject object,
			Map<StructureCopy.ObjectKey, DBObject> allObjects, boolean getFromDB) throws StructureCopyException {
		DBObject linkedObject = object.getReference(getLinkColumn());

		if (linkedObject == null) {
			Table targetTable = getTargetTable(copy, object);

			if (targetTable != null) {
				linkedObject = targetTable.getObjectByID(copy, conn, object.getColValue(getLinkColumn()), allObjects, getFromDB, getLinkColumn(), object);
				if (linkedObject != null) {
					object.setReference(getLinkColumn(), linkedObject);
				}
			}
		}

		return linkedObject;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		Table[] possibleTargets = getPossibleTargets();
		StringBuffer string = new StringBuffer();

		string.append("Reference ").append(sourceTable.getName()).append(".").append(getLinkColumn()).append(" -> ");
		string.append(StringUtils.merge(possibleTargets, ", ", "(", ")"));
		return string.toString();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getReferenceType()
	 */
	public int getReferenceType() {
		return referenceCopyType;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getForeignReferenceType()
	 */
	public int getForeignReferenceType() {
		return referenceForeignCopyType;
	}

	/**
	 * Helper method to get the target table of the referenced object (when set
	 * as instance of {@link StructureCopy.ObjectKey})
	 * @param object object
	 * @return target table if set, or null
	 */
	protected Table getReferencedObjectTable(DBObject object) {
		Object colValue = object.getColValue(getLinkColumn());

		if (colValue instanceof StructureCopy.ObjectKey) {
			return ((StructureCopy.ObjectKey) colValue).getTable();
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#isReferenceValueNeeded(java.sql.ResultSet, java.lang.String)
	 */
	public boolean isReferenceValueNeeded(ResultSet res, String referenceColumnName) throws SQLException {
		// the default implementation returns true (all reference values will be needed)
		return true;
	}
}
