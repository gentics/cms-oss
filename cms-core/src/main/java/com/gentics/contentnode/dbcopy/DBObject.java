/*
 * @author norbert
 * @date 03.10.2006
 * @version $Id: DBObject.java,v 1.12 2010-09-28 17:01:31 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * Implementation class of objects to copy
 */
public class DBObject implements Resolvable {

	protected Object originalId;

	/**
	 * new id, null if not yet copied
	 */
	protected Object newId;

	/**
	 * the update timestamp of this object (if known)
	 */
	protected int udate = -1;

	/**
	 * map of referenced objects, keys are the column names, values are the
	 * referenced objects (instances of {@link DBObject}, or null if empty
	 */
	protected Map<String, DBObject> referencedObjects = new HashMap<String, DBObject>(2);

	/**
	 * map of lists of referencing objects, keys are the table.reference names
	 * of the foreign references, values are lists of objects or null if empty
	 */
	protected Map<String, List<DBObject>> referencingObjects = null;

	/**
	 * source table
	 */
	protected Table sourceTable;

	/**
	 * reference values - stored in an array. the order is defined by
	 * {@link Table#getReferenceColumns()}
	 */
	protected Object[] referenceValuesArray;

	/**
	 * The object linking to or linked from this object that caused this object
	 * to be copied
	 */
	protected DBObject creationCauseObject;

	/**
	 * name of the link that caused this object to be copied
	 */
	protected String creationCauseLink;

	/**
	 * map of properties
	 */
	private Map<String, Object> properties = null;

	/**
	 * flag to mark whether the references have been filled for the object
	 */
	private boolean referencesFilled = false;

	/**
	 * display name of the object (may be null for objects that have no display name, or for deleted objects)
	 */
	private String name = null;

	/**
	 * Create an (empty) instance of the object
	 * @param sourceTable source table
	 * @param name display name of the object or null if the object has no display name
	 */
	public DBObject(Table sourceTable, String name) {
		this.sourceTable = sourceTable;
		this.name = name;
		referenceValuesArray = new Object[sourceTable.getReferenceColumns().length];
	}

	/**
	 * Create an instance of the object with the given id
	 * @param sourceTable source table
	 * @param id id of the object
	 * @param res result set holding the data
	 * @param creationCauseObject
	 * @param creationCauseLink
	 * @param udate true when the resultset contains the udate, false if not
	 */
	public DBObject(Table sourceTable, Object id, ResultSet res, DBObject creationCauseObject,
			String creationCauseLink, boolean udate) throws SQLException {
		this.creationCauseObject = creationCauseObject;
		this.creationCauseLink = creationCauseLink;
		this.sourceTable = sourceTable;
		originalId = id;
		ResultSetMetaData meta = res.getMetaData();
		int colCount = meta.getColumnCount();

		referenceValuesArray = new Object[colCount];
		for (int i = 1; i <= colCount; i++) {
			String colName = meta.getColumnName(i);

			if (udate && "udate".equalsIgnoreCase(colName)) {
				continue;
			}
			// only set the value of the reference column, if the value is really needed
			referenceValuesArray[getIndexForColumn(colName)] = sourceTable.isReferenceValueNeeded(res, colName) ? res.getObject(colName) : null;
		}
		if (udate) {
			this.udate = res.getInt("udate");
			if (this.udate == 0) {
				this.udate = -1;
			}
		}
	}

	/**
	 * Get the current id of the object (original or new if already copied)
	 * @return id of the object
	 */
	public Object getId() {
		return newId == null ? originalId : newId;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("object ").append(originalId).append(" -> ").append(newId).append(" of ").append(sourceTable);
		return buffer.toString();
	}

	/**
	 * Set the given object reference
	 * @param colId column id
	 * @param referencedObject referenced object
	 */
	public void setReference(String colId, DBObject referencedObject) {
		referencedObjects.put(colId, referencedObject);
	}

	/**
	 * Set a foreign reference (list of objects referencing this object)
	 * @param referenceDescriptor descriptor of the foreign reference
	 * @param referenceList list of referencing objects
	 */
	public void setForeignReference(ReferenceDescriptor referenceDescriptor, List<DBObject> referenceList) {
		if (referenceDescriptor == null) {
			return;
		}
		if (referencingObjects == null) {
			referencingObjects = new HashMap<String, List<DBObject>>(2);
		}
		String tableName = referenceDescriptor.getSourceTable().getId();
		String referenceName = referenceDescriptor.getLinkColumn();

		// add to the map of referencing objects
		referencingObjects.put(Tables.getFullReferenceName(tableName, referenceName), referenceList);
	}

	/**
	 * Get the list of foreign referencing objects or null if not set
	 * @param referenceDescriptor descriptor of the foreign reference
	 * @return list of objects or null
	 */
	public List<DBObject> getForeignReference(ReferenceDescriptor referenceDescriptor) {
		if (referenceDescriptor == null) {
			return null;
		}
		String tableName = referenceDescriptor.getSourceTable().getId();
		String referenceName = referenceDescriptor.getLinkColumn();

		return referencingObjects == null ? null : referencingObjects.get(Tables.getFullReferenceName(tableName, referenceName));
	}

	/**
	 * Add the given object to the list of referencing objects
	 * @param referenceDescriptor descriptor of the foreign reference
	 * @param object object referencing this object
	 */
	public void addForeignReference(ReferenceDescriptor referenceDescriptor, DBObject object) {
		if (referenceDescriptor == null) {
			return;
		}
		String tableName = referenceDescriptor.getSourceTable().getId();
		String referenceName = referenceDescriptor.getLinkColumn();
		String fullReferenceName = Tables.getFullReferenceName(tableName, referenceName);

		if (referencingObjects == null) {
			referencingObjects = new HashMap<String, List<DBObject>>(2);
		}
		List<DBObject> foreignObjects = referencingObjects.get(fullReferenceName);

		if (foreignObjects == null) {
			foreignObjects = new Vector<DBObject>(2);
			referencingObjects.put(fullReferenceName, foreignObjects);
		}

		if (!foreignObjects.contains(object)) {
			try {
				foreignObjects.add(object);
			} catch (UnsupportedOperationException e) {
				// when adding to the list is not supported, we create a new (modifiable) list and use that
				foreignObjects = new Vector<DBObject>(foreignObjects);
				foreignObjects.add(object);
				referencingObjects.put(fullReferenceName, foreignObjects);
			}
		}
	}

	/**
	 * Get the referenced object
	 * @param colId name of the link column
	 * @return referenced object
	 */
	public DBObject getReference(String colId) {
		return (DBObject) referencedObjects.get(colId);
	}

	/**
	 * Get the column value
	 * @param colName column name
	 * @return value or null
	 */
	public Object getColValue(String colName) {
		return referenceValuesArray[getIndexForColumn(colName)];
	}

	/**
	 * Set a column value
	 * @param colName column name
	 * @param value new value
	 */
	public void setColValue(String colName, Object value) {
		referenceValuesArray[getIndexForColumn(colName)] = value;
	}

	/**
	 * Get the source table
	 * @return source table
	 */
	public Table getSourceTable() {
		return sourceTable;
	}

	/**
	 * Dump this object (together with all references) into the print stream
	 * @param print where to dump to
	 */
	public void dump(PrintStream print) {
		print.println(toString());
		for (Map.Entry<String, DBObject> entry : referencedObjects.entrySet()) {
			print.println("\t" + entry.getKey() + " -> " + entry.getValue());
		}
	}

	/**
	 * Check whether this object references the given object
	 * @param object object
	 * @return true when this object references the given object, false if not
	 */
	public boolean references(DBObject object) {
		return referencedObjects.containsValue(object);
	}

	/**
	 * Get all linked objects
	 * @return collection of linked objects
	 */
	public Collection<DBObject> getLinkedObjects() {
		return referencedObjects.values();
	}

	/**
	 * Get the name of the link that caused this object to be copied
	 * @return name of the link
	 */
	public String getCreationCauseLink() {
		return creationCauseLink;
	}

	/**
	 * Get the object linked from or linking to this object which caused this
	 * object to be copied
	 * @return linked object
	 */
	public DBObject getCreationCauseObject() {
		return creationCauseObject;
	}

	public int getIndexForColumn(String colName) {
		String[] cols = sourceTable.getReferenceColumns();

		for (int i = 0; i < cols.length; i++) {
			if (colName.equals(cols[i])) {
				return i;
			}
		}
		throw new RuntimeException("Error while searching for index for column {" + colName + "} in {" + sourceTable.getId() + "}");
	}

	/**
	 * Get the new id of the object (may be null)
	 * @return new id of the object or null
	 */
	public Object getNewId() {
		return newId;
	}

	/**
	 * Set the new id
	 * @param newId
	 */
	public void setNewId(Object newId) {
		this.newId = newId;
	}

	/**
	 * Get the original id
	 * @return the original id
	 */
	public Object getOriginalId() {
		return originalId;
	}

	/**
	 * Set the original id
	 * @param originalId original objectid
	 */
	public void setOriginalId(Object originalId) {
		this.originalId = originalId;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		return getColValue(key);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	public void setUdate(int udate) {
		this.udate = udate;
	}
    
	public int getUdate() {
		return udate;
	}

	/**
	 * Get the meta property with the given key
	 * @param key key
	 * @return value (null if not set)
	 */
	public Object getMetaProperty(String key) {
		return properties == null ? null : properties.get(key);
	}

	/**
	 * Set the given meta property
	 * @param key key
	 * @param value value
	 */
	public void setMetaProperty(String key, Object value) {
		if (properties == null) {
			properties = new HashMap<String, Object>(2);
		}
		properties.put(key, value);
	}

	/**
	 * Check whether the references for this object have been filled
	 * @return true when the references for this object have all been filled, false if not
	 */
	public boolean isReferencesFilled() {
		return referencesFilled;
	}

	/**
	 * Set the flag to mark whether all references have been filled
	 * @param referencesFilled true when the references have been filled, false if not
	 */
	public void setReferencesFilled(boolean referencesFilled) {
		this.referencesFilled = referencesFilled;
	}

	/**
	 * Get the display name of the object or null if it has no name set
	 * @return display name of the object (may be null)
	 */
	public String getName() {
		return name;
	}
}
