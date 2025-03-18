/*
 * @author norbert
 * @date 03.10.2006
 * @version $Id: Tables.java,v 1.10 2010-09-28 17:01:31 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gentics.contentnode.dbcopy.jaxb.JAXBTableType;

/**
 * Tables implementation
 */
public class Tables extends com.gentics.contentnode.dbcopy.jaxb.Tables {

	/**
	 * map of tables, keys are the names, values are the Table objects
	 */
	protected Map<String, Table> tablesMap;

	/**
	 * constant for the number of steps display in the "progress bar"
	 */
	protected final static int PROGRESS_STEPS = 10;

	protected final static Map<String, Map<String, String>> FULL_REFERENCE_NAMES = new HashMap<String, Map<String, String>>();

	/**
	 * Check the consistency
	 * @param copy copy configuration
	 * @return true when everything is consistent, false if not
	 * @throws Exception
	 */
	public boolean checkConsistency(StructureCopy copy) throws Exception {
		// first generate the tables map
		boolean checksOk = true;

		tablesMap = new HashMap<String, Table>();
		JAXBTableType[] tables = getTable();

		for (int i = 0; i < tables.length; i++) {
			if (tablesMap.containsKey(tables[i].getId())) {
				// duplicate table definition foun
				System.err.println("Duplicate definition for table {" + tables[i].getId() + "} found");
				checksOk = false;
			} else {
				// put the table into the map
				tablesMap.put(tables[i].getId(), (Table) tables[i]);
			}
		}

		// we found duplicate table definitions, so stop here
		if (!checksOk) {
			return false;
		}

		// now check every table for consistency
		for (int i = 0; i < tables.length; i++) {
			if (tables[i] instanceof Table) {
				checksOk &= ((Table) tables[i]).checkConsistency(copy, this);
			}
		}

		// finally check whether the roottable really exists
		if (getTable(getRoottable()) == null) {
			System.err.println("Could not find root table {" + getRoottable() + "}");
			checksOk = false;
		}

		return checksOk;
	}

	/**
	 * Get the table by name
	 * @param tableName table name
	 * @return table or null if no table found with the name
	 */
	public Table getTable(String tableName) {
		if (tablesMap == null || tableName == null) {
			return null;
		}
		return (Table) tablesMap.get(tableName);
	}

	/**
	 * Get all tables
	 * @return all tables
	 */
	public Table[] getTables() {
		Collection<Table> tableValues = tablesMap.values();

		return (Table[]) tableValues.toArray(new Table[tableValues.size()]);
	}

	/**
	 * Get the table with the given property set to the given value
	 * @param propertyName name of the property
	 * @param propertyValue searched value
	 * @return (first) table with the property set accordignly or null if no table found
	 */
	public Table getTableWithProperty(String propertyName, String propertyValue) {
		if (propertyName == null || propertyValue == null) {
			return null;
		}
		for (Table table : tablesMap.values()) {
			if (propertyValue.equals(table.getProperty(propertyName))) {
				return table;
			}
		}

		return null;
	}

	/**
	 * Get the object structure
	 * @param copy copy configuration
	 * @param conn database connection
	 * @return map of objects that hold references to other objects
	 * @throws StructureCopyException
	 */
	public Map<StructureCopy.ObjectKey, DBObject> getObjectStructure(StructureCopy copy, Connection conn) throws StructureCopyException {
		return getObjectStructure(copy, conn, false);
	}

	/**
	 * Get the object structure
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param verbose true for verbose
	 * @return map of objects that hold references to other objects
	 * @throws StructureCopyException
	 */
	public Map<StructureCopy.ObjectKey, DBObject> getObjectStructure(StructureCopy copy, Connection conn, boolean verbose) throws StructureCopyException {
		if (verbose) {
			System.out.println("Getting objects");
		}

		// Get all objects to copy in the first run, which will fetch all objects which are:
		// - root objects
		// - linked from already fetched objects with deepcopy = true
		// - linking to already fetched objects with foreigndeepcopy = true
		Map<StructureCopy.ObjectKey, DBObject> allObjects = new LinkedHashMap<StructureCopy.ObjectKey, DBObject>();
		Map<StructureCopy.ObjectKey, DBObject> mainObjects = new LinkedHashMap<StructureCopy.ObjectKey, DBObject>();

		copy.getCopyController().getObjectStructure(copy, allObjects, mainObjects);

		if (verbose) {
			System.out.print("Updating links");
		}

		int objectNumber = mainObjects.size();
		int objectBlock = 0;
		int objectCount = 0;

		// now do the second run. In this run, only links with deepcopy = false to also fetched objects are updated.
		for (DBObject object : mainObjects.values()) {
			object.getSourceTable().getObjectLinks(copy, conn, allObjects, object, false);
			objectCount++;
			if (verbose && objectCount * PROGRESS_STEPS / objectNumber != objectBlock) {
				System.out.print(".");
				objectBlock = objectCount * PROGRESS_STEPS / objectNumber;
			}
		}

		if (verbose) {
			System.out.println();
		}

		return mainObjects;
	}

	/**
	 * Copy the object structure, update all links
	 * @param copy copy configuration
	 * @param objectStructure object structure to copy
	 * @param conn database connection
	 * @param verbose true for verbose mode
	 * @throws StructureCopyException
	 */
	public void copyStructure(StructureCopy copy, Map<StructureCopy.ObjectKey, DBObject> objectStructure, Connection conn, boolean verbose) throws StructureCopyException {
		copy.getCopyController().beginCopyObjects(copy, objectStructure);
		copy.getCopyController().copyObjectStructure(copy, objectStructure);
		copy.getCopyController().finishCopyObjects(copy, objectStructure);
	}

	/**
	 * Construct the full (unique) reference name for the reference to the given table
	 * @param tableName name of the table
	 * @param referenceName name of the reference
	 * @return full name of the reference
	 */
	public static synchronized String getFullReferenceName(String tableName, String referenceName) {
		Map<String, String> referenceNames = null;

		if (!FULL_REFERENCE_NAMES.containsKey(tableName)) {
			referenceNames = new HashMap<String, String>();
			FULL_REFERENCE_NAMES.put(tableName, referenceNames);
		} else {
			referenceNames = FULL_REFERENCE_NAMES.get(tableName);
		}

		if (referenceNames.containsKey(referenceName)) {
			return (String) referenceNames.get(referenceName);
		} else {
			String fullReferenceName = tableName + "." + referenceName;

			referenceNames.put(referenceName, fullReferenceName);
			return fullReferenceName;
		}
	}
}
