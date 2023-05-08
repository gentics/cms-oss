/*
 * @author norbert
 * @date 03.10.2006
 * @version $Id: ReferenceDescriptor.java,v 1.6 2008-11-10 10:54:29 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType.ParameterType;
import com.gentics.contentnode.nodecopy.ObjectHelper;

/**
 * Interface for reference descriptor. Implementing classes provide information
 * and methods for reference links between tables.
 */
public interface ReferenceDescriptor {

	/**
	 * copytype true (referenced objects shall be copied)
	 */
	public final static int COPYTYPE_TRUE = 0;

	/**
	 * copytype false (referenced objects shall not be copied)
	 */
	public final static int COPYTYPE_FALSE = 1;

	/**
	 * copytype ask (referenced objects shall possibly be copied)
	 */
	public final static int COPYTYPE_ASK = 2;

	/**
	 * Get linking objects (objects linking to the given object via this
	 * reference). The linking objects are read from the database (if not yet
	 * done) and linked together.
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param object base object
	 * @param allObjects map of all objects already fetched from the database
	 * @return array of objects linking to the given object
	 * @throws StructureCopyException
	 */
	List<DBObject> getLinkingObjects(StructureCopy copy, Connection conn, DBObject object, Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException;

	/**
	 * Get the linked object
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param object base object
	 * @param allObjects map of all objects already fetched from the database
	 * @param getFromDB true when linked objects shall be fetched from the
	 *        database (if not yet done), false when only objects already
	 *        contained in allObjects shall be linked.
	 * @return the linked object or null
	 * @throws StructureCopyException
	 */
	DBObject getLinkedObject(StructureCopy copy, Connection conn, DBObject object,
			Map<StructureCopy.ObjectKey, DBObject> allObjects, boolean getFromDB) throws StructureCopyException;

	/**
	 * Initialize this reference descriptor
	 * @param conn database connection
	 * @param parameter parameters for this reference descriptor
	 * @throws StructureCopyException
	 */
	void init(Connection conn, ParameterType[] parameter) throws StructureCopyException;

	/**
	 * Get all possible targets for this reference descriptor
	 * @return array of all possible target tables
	 */
	Table[] getPossibleTargets();

	/**
	 * Get the names of the columns defining this reference
	 * @return list of names
	 */
	String[] getReferenceColumns();

	/**
	 * Check whether the reference value of the given object is really needed
	 * @param res result set holding the data
	 * @param referenceColumnName name of the reference column
	 * @return true when the reference value is needed, false if not
	 * @throws SQLException
	 */
	boolean isReferenceValueNeeded(ResultSet res, String referenceColumnName) throws SQLException;

	/**
	 * Update this reference for the given source object. The ttype must be one
	 * of the constants defined in {@link ObjectHelper} and may be null if not
	 * known or not available.
	 * @param sourceObject source object (the one holding the reference)
	 * @param targetTable target table (table of the referenced object)
	 * @param ttype ttype
	 * @param targetId target id (id of the referenced object)
	 * @throws StructureCopyException
	 */
	void updateReference(DBObject sourceObject, Table targetTable, Integer ttype, Object targetId) throws StructureCopyException;

	/**
	 * Get the column name holding the id of the referenced object (which is also taken as reference name)
	 * @return link column
	 */
	String getLinkColumn();

	/**
	 * Get the source table of the reference
	 * @return source table
	 */
	Table getSourceTable();

	/**
	 * Check whether the object has another object linked by this reference
	 * @param copy copy configuration
	 * @param sourceObject source object
	 * @return true when an object is linked, false if not
	 * @throws StructureCopyException
	 */
	boolean isObjectLinked(StructureCopy copy, DBObject sourceObject) throws StructureCopyException;

	/**
	 * Get the target table for the reference stored in the given object
	 * @param copy copy configuration
	 * @param object object holding the reference
	 * @return target table or null
	 * @throws StructureCopyException
	 */
	public Table getTargetTable(StructureCopy copy, DBObject object) throws StructureCopyException;

	/**
	 * Get the reference copy type. Returns one of {@link #COPYTYPE_TRUE},
	 * {@link #COPYTYPE_FALSE} or {@link #COPYTYPE_ASK}.
	 * @return copytype of this reference
	 */
	public int getReferenceType();

	/**
	 * Get the foreign reference copy type. Returns one of {@link #COPYTYPE_TRUE},
	 * {@link #COPYTYPE_FALSE} or {@link #COPYTYPE_ASK}.
	 * @return foreign copytype of this reference
	 */
	public int getForeignReferenceType();

	/**
	 * Handle an unsatisfied reference
	 * @param copy structure copy
	 * @param object object holding the reference
	 * @param table target table
	 * @param id target id
	 * @throws StructureCopyException
	 */
	default void handleUnsatisfiedReference(StructureCopy copy, DBObject object, Table table, Object id) throws StructureCopyException {
		// empty default implementation
	}
}
