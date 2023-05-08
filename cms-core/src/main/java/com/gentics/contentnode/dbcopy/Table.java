/*
 * @author norbert
 * @date 03.10.2006
 * @version $Id: Table.java,v 1.17 2010-09-28 17:01:31 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.dbcopy.StructureCopy.ObjectKey;
import com.gentics.contentnode.dbcopy.jaxb.JAXBPropertiesType.PropertyType;
import com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType;
import com.gentics.contentnode.dbcopy.jaxb.impl.JAXBreferenceTypeImpl;
import com.gentics.contentnode.dbcopy.jaxb.impl.JAXBtableTypeImpl;
import com.gentics.lib.etc.StringUtils;

/**
 * Implementation of a table configuration
 */
public class Table extends JAXBtableTypeImpl {

	/**
	 * list of foreign references (tables linking to this table) where foreigndeepcopy is "true"
	 */
	protected List<Reference> foreignReferences = new Vector<Reference>();

	/**
	 * list of *all* foreign references (tables linking to this table)
	 */
	protected List<Reference> allForeignReferences = new Vector<Reference>();

	/**
	 * names of the columns that hold links to other tables
	 */
	protected String[] referenceColumns;

	/**
	 * comma separated list of fields to select from the table
	 */
	protected String selectedFields;

	/**
	 * names of all columns in the table
	 */
	protected String[] allColumns;

	/**
	 * names of all columns, that can be copied (not uuid and udate)
	 */
	protected String[] allCopyColumns;

	/**
	 * command to copy an object in a normal table, needs the id as bind value
	 */
	protected String copyCommand;

	/**
	 * command to insert an oject in a normal table, needs all data columns as bind value
	 */
	protected String insertCommand;

	/**
	 * SQL Statement to get the global ID for the given localid (or localid and localid2)
	 */
	protected String selectGlobalIdSQL;

	/**
	 * SQL Statement to get the local ID (or pair of local ids) for the given global id
	 */
	protected String selectLocalIdSQL;

	/**
	 * names of the link columns in cross tables
	 */
	protected String[] crossTableId;

	/**
	 * configured object modificator (may be null)
	 */
	protected ObjectModificator[] modificators;

	/**
	 * @see #getReferenceColumns()
	 */
	protected String[] dbReferenceColumns;

	/**
	 * names of all data columns
	 */
	protected String[] allDataColumns;

	/**
	 * all columns in the table, but the id column
	 */
	protected String[] allButIdColumn;

	/**
	 * properties map
	 */
	protected Map<String, String> properties;

	/**
	 * Map containing configured default values.
	 * Default values can be configured as property [columnname]_default
	 */
	protected Map<String, Object> defaultValues = new HashMap<String, Object>();

	/**
	 * Map of reference descriptors. Keys are the reference column names, values
	 * are lists of reference descriptors
	 */
	protected Map<String, List<ReferenceDescriptor>> referenceDescriptorMap = new HashMap<String, List<ReferenceDescriptor>>();

	/**
	 * list holding the names of columns to completely omit
	 */
	protected List<String> omitColumns = new Vector<String>();

	/**
	 * Get the list of reference descriptors that need the given reference
	 * column
	 * @param referenceColumn name of the reference column
	 * @param createIfNotFound create the list, if not found
	 * @return list of reference descriptors, or null if not found and createIfNotFound is false
	 */
	protected List<ReferenceDescriptor> getReferenceDescriptors(String referenceColumn, boolean createIfNotFound) {
		List<ReferenceDescriptor> referenceDescriptors = referenceDescriptorMap.get(referenceColumn);

		if (referenceDescriptors == null && createIfNotFound) {
			referenceDescriptors = new Vector<ReferenceDescriptor>();
			referenceDescriptorMap.put(referenceColumn, referenceDescriptors);
		}

		return referenceDescriptors;
	}

	/**
	 * Check whether the reference value in the given column is really needed for the data
	 * @param res result set holding the complete information
	 * @param referenceColumnName name of the reference column
	 * @return true if at least one reference descriptor actually needs the information, false if none
	 * @throws SQLException
	 */
	public boolean isReferenceValueNeeded(ResultSet res, String referenceColumnName) throws SQLException {
		// get all reference descriptors which need the reference column
		List<ReferenceDescriptor> referenceDescriptors = getReferenceDescriptors(referenceColumnName, false);

		if (referenceDescriptors == null) {
			// no reference descriptors found, so this is an id column (which is always needed)
			return true;
		} else {
			// ask all reference descriptors, whether they need the value for the given resultset
			for (ReferenceDescriptor referenceDesc : referenceDescriptors) {
				if (referenceDesc.isReferenceValueNeeded(res, referenceColumnName)) {
					// found a reference descriptor needing the value
					return true;
				}
			}

			// no reference descriptor needed the value
			return false;
		}
	}

	/**
	 * Check the consistency and initialize all class members
	 * @param copy copy configuration
	 * @param tables tables configuration
	 * @return true when the table is consistently coonfigured, false if not
	 * @throws SQLException
	 */
	public boolean checkConsistency(StructureCopy copy, Tables tables) throws Exception {
		Connection conn = copy.getConnection();

		// read the tables properties into a map
		properties = new HashMap<String, String>();
		if (isSetProperties() && getProperties().isSetPropertyList()) {
			PropertyType[] propertyList = getProperties().getPropertyList();

			for (int i = 0; i < propertyList.length; i++) {
				String id = propertyList[i].getId();

				properties.put(id, propertyList[i].getValue());

				// check whether the property is probably a default value
				if (id.endsWith("_default")) {
					defaultValues.put(id.substring(0, id.length() - 8), propertyList[i].getValue());
				}
			}
		}

		// check the columns to omit
		String[] omitCols = StringUtils.splitString(getProperty("omitcolumns"), ",");

		for (int i = 0; i < omitCols.length; i++) {
			omitColumns.add(omitCols[i]);
		}

		// check whether the given table exists
		ResultSet columns = conn.getMetaData().getColumns(null, null, getName(), null);

		JAXBreferenceType[] ref = isSetReferences() ? getReferences().getRef() : new JAXBreferenceTypeImpl[0];
		Map<String, Boolean> checkedReferences = new HashMap<String, Boolean>();

		for (int i = 0; i < ref.length; i++) {
			if (ref[i].isSetTarget()) {
				checkedReferences.put(ref[i].getCol(), Boolean.FALSE);
			}
		}

		List<String> allCols = new Vector<String>();
		boolean idColumnChecked = isCrossTable();

		crossTableId = isCrossTable() ? getIdcol().trim().split("\\s*,\\s*") : new String[0];
		for (int i = 0; i < crossTableId.length; i++) {
			checkedReferences.put(crossTableId[i], Boolean.FALSE);
		}

		boolean checksOk = true;

		while (columns.next()) {
			// omit columns if configured to do so
			if (omitColumns.contains(columns.getString("COLUMN_NAME"))) {
				continue;
			}

			allCols.add(columns.getString("COLUMN_NAME"));
			if (!isCrossTable() && getIdcol().equals(columns.getString("COLUMN_NAME"))) {
				idColumnChecked = true;
			} else {
				for (int i = 0; i < ref.length; i++) {
					if (ref[i].isSetTarget() && ((Reference) ref[i]).getCol().equals(columns.getString("COLUMN_NAME"))) {
						checkedReferences.put(ref[i].getCol(), Boolean.TRUE);
					}
				}
				for (int i = 0; i < crossTableId.length; i++) {
					if (crossTableId[i].equals(columns.getString("COLUMN_NAME"))) {
						checkedReferences.put(crossTableId[i], Boolean.TRUE);
					}
				}
			}
		}
		allColumns = (String[]) allCols.toArray(new String[allCols.size()]);
		List<String> allCopyCols = new ArrayList<String>(allCols);
		allCopyCols.remove("uuid");
		allCopyCols.remove("udate");
		allCopyColumns = allCopyCols.toArray(new String[allCopyCols.size()]);

		if (!idColumnChecked) {
			System.err.println("Could not find id column {" + getIdcol() + "} for table {" + getName() + "}");
			checksOk = false;
		}

		for (Map.Entry<String, Boolean> entry : checkedReferences.entrySet()) {
			if (!entry.getValue().booleanValue()) {
				System.err.println("Could not find column {" + entry.getKey() + "}");
				checksOk = false;
			}
		}

		// finally check all references
		List<String> refCols = new Vector<String>();

		for (int i = 0; i < ref.length; i++) {
			Reference reference = (Reference) ref[i];

			checksOk &= reference.checkConsistency(conn, this, tables);
			ReferenceDescriptor referenceDescriptor = reference.getReferenceDescriptor(conn);

			if (referenceDescriptor != null) {
				String[] tempCols = referenceDescriptor.getReferenceColumns();

				for (int j = 0; j < tempCols.length; j++) {
					if (!refCols.contains(tempCols[j])) {
						refCols.add(tempCols[j]);
					}

					// also build map of reference descriptors and reference
					// columns
					List<ReferenceDescriptor> refDescriptors = getReferenceDescriptors(tempCols[j], true);

					if (!refDescriptors.contains(referenceDescriptor)) {
						refDescriptors.add(referenceDescriptor);
					}
				}
			}
		}
		for (int i = 0; i < crossTableId.length; i++) {
			if (!refCols.contains(crossTableId[i])) {
				refCols.add(crossTableId[i]);
			}
		}
		// and also the id fields for the
		referenceColumns = (String[]) refCols.toArray(new String[refCols.size()]);

		boolean fieldset = false;

		if (!isCrossTable()) {
			selectedFields = "`" + getName() + "`." + getIdcol();
			fieldset = true;
		} else {
			selectedFields = "";
		}
		for (int i = 0; i < referenceColumns.length; i++) {
			selectedFields += (fieldset ? ", " : "") + "`" + getName() + "`." + referenceColumns[i];
			fieldset = true;
		}

		// pre-generate the "select all fields but id" command part
		if (checksOk && !isCrossTable()) {
			allButIdColumn = new String[allColumns.length - 1];
			int index = 0;

			for (int i = 0; i < allColumns.length; i++) {
				if (!getIdcol().equals(allColumns[i])) {
					allButIdColumn[index++] = allColumns[i];
				}
			}
			List<String> copyFields = new ArrayList<String>();
			copyFields.addAll(Arrays.asList(allButIdColumn));
			copyFields.remove("uuid");
			copyFields.remove("udate");
			copyCommand = "INSERT INTO `" + getName() + "`(" + StringUtils.merge(copyFields.toArray(), ", ") + ") SELECT "
					+ StringUtils.merge(copyFields.toArray(), ", ", "`" + getName() + "`.", null) + " FROM `" + getName() + "` WHERE " + getIdcol() + " = ?";
			// generate insert command here
			insertCommand = "INSERT INTO `" + getName() + "` (" + StringUtils.merge(allButIdColumn, ",") + ") VALUES ("
					+ StringUtils.repeat("?", allButIdColumn.length, ",") + ")";
		} else if (checksOk && isCrossTable()) {
			List<String> allButIdCols = new Vector<String>();

			allButIdCols.addAll(allCols);
			for (int i = 0; i < crossTableId.length; i++) {
				// do not remove crosstable id fields that are no references
				if (getReferenceDescriptor(copy, crossTableId[i]) != null) {
					allButIdCols.remove(crossTableId[i]);
				}
			}

			allButIdColumn = (String[]) allButIdCols.toArray(new String[allButIdCols.size()]);
		}

		// create the SQL statements to select the global id or local id's
		StringBuilder sql = new StringBuilder("SELECT ");
		if (isCrossTable()) {
			sql.append(getIdcol());
		} else {
			sql.append("id");
		}
		sql.append(" FROM `").append(getName()).append("` WHERE uuid = ?");
		selectLocalIdSQL = sql.toString();
		sql = new StringBuilder("SELECT uuid FROM `").append(getName()).append("` WHERE ");
		if (isCrossTable()) {
			for (int i = 0; i < crossTableId.length; i++) {
				if (i > 0) {
					sql.append(" AND ");
				}
				sql.append(crossTableId[i]).append(" = ?");
			}
		} else {
			sql.append("id = ?");
		}

		// check the modificators (if set)
		if (isSetModificators()) {
			String[] modificatorClassNames = getModificators().getModificator();

			modificators = new ObjectModificator[modificatorClassNames.length];
			for (int i = 0; i < modificatorClassNames.length; i++) {
				// load the class
				try {
					Class<?> modificatorClass = Class.forName(modificatorClassNames[i]);

					if (!ObjectModificator.class.isAssignableFrom(modificatorClass)) {
						throw new Exception("Class " + modificatorClassNames[i] + " does not implement " + ObjectModificator.class.getName());
					}

					modificators[i] = (ObjectModificator) modificatorClass.newInstance();
				} catch (Exception ex) {
					System.err.println("Error while checking modificator class " + modificatorClassNames[i] + ": " + ex.getLocalizedMessage());
					checksOk = false;
				}
			}
		}

		// collect all data columns
		List<String> allDataCols = new Vector<String>();

		allDataCols.addAll(allCols);
		allDataCols.removeAll(refCols);
		allDataCols.remove(getIdcol());
		// TODO remove cross table id columns?
		allDataColumns = (String[]) allDataCols.toArray(new String[allDataCols.size()]);

		// setup the dbReferenceColumns from the selected fields
		String[] fields = selectedFields.split(",");

		dbReferenceColumns = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			int dotIndex = fields[i].indexOf('.');

			dbReferenceColumns[i] = dotIndex >= 0 ? fields[i].substring(dotIndex + 1) : fields[i];
			dbReferenceColumns[i] = dbReferenceColumns[i].trim();
		}

		return checksOk;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.jaxb.JAXBtableType#getIdcol()
	 */
	public String getIdcol() {
		String idCol = super.getIdcol();

		return idCol != null ? idCol : "id";
	}

	/**
	 * Check whether this is a cross table
	 * @return true for cross tables, false for others
	 */
	public boolean isCrossTable() {
		return "cross".equals(getType());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "table [" + getId() + "]";
	}

	/**
	 * Add the foreign reference to the list
	 * @param reference foreign reference
	 */
	public void addForeignReference(Reference reference) {
		if (reference == null) {
			return;
		}
		// only add to the list, when foreigndeepcopy is "true"
		if (!reference.isSetForeigndeepcopy() || reference.isForeigndeepcopy(true)) {
			foreignReferences.add(reference);
		}
		allForeignReferences.add(reference);
	}

	/**
	 * Get all foreign references
	 * @return list of all foreign references
	 */
	public List<Reference> getAllForeignReferences() {
		return allForeignReferences;
	}

	/**
	 * Get the list of foreign references (as instances of {@link Reference}), that have foreigndeepcopy "true"
	 * @return list of foreign references
	 */
	public List<Reference> getDeepCopyForeignReferences() {
		return foreignReferences;
	}

	/**
	 * Get an object from the table by id
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param id id of the object
	 * @param allObjects map of all objects (new objects will be put here)
	 * @param getFromDB true whether the object shall be fetched from the
	 *        database if not yet contained in allObjects, false if not
	 * @param referenceName name of the reference that causes this object to be
	 *        copied
	 * @param referencingObject referencing object that causes this object to be
	 *        copied
	 * @return the object or null if it does not exist
	 * @throws StructureCopyException
	 */
	public DBObject getObjectByID(StructureCopy copy, Connection conn, Object id,
			Map<StructureCopy.ObjectKey, DBObject> allObjects, boolean getFromDB, String referenceName, DBObject referencingObject) throws StructureCopyException {
		return getObjectByID(copy, conn, id, allObjects, getFromDB, referenceName, referencingObject, true);
	}

	/**
	 * Get an object from the table by id
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param id id of the object
	 * @param allObjects map of all objects (new objects will be put here)
	 * @param getFromDB true whether the object shall be fetched from the
	 *        database if not yet contained in allObjects, false if not
	 * @param referenceName name of the reference that causes this object to be
	 *        copied
	 * @param referencingObject referencing object that causes this object to be
	 *        copied
	 * @param getObjectLinks true, when all object links of the object shall be resolved, false if not
	 * @return the object or null if it does not exist
	 * @throws StructureCopyException
	 */
	public DBObject getObjectByID(StructureCopy copy, Connection conn, Object id,
			Map<StructureCopy.ObjectKey, DBObject> allObjects, boolean getFromDB, String referenceName, DBObject referencingObject, boolean getObjectLinks) throws StructureCopyException {
		if (isCrossTable() || id == null || "0".equals(id.toString())) {
			// cross tables have no id
			return null;
		}

		// first check whether the object already exists
		StructureCopy.ObjectKey key = StructureCopy.ObjectKey.getObjectKey(this, id);

		// if the id is an objectkey, get the real id
		if (id instanceof ObjectKey) {
			id = ((ObjectKey) id).getId();
		}

		if (!allObjects.containsKey(key) && getFromDB) {
			DBObject object = copy.getCopyController().getObjectByID(copy, this, id, referenceName, referencingObject, true);

			if (object != null) {
				allObjects.put(key, object);
				if (getObjectLinks) {
					getObjectLinks(copy, conn, allObjects, object, true);
				}
				return object;
			} else {
				return null;
			}
		} else {
			DBObject object = (DBObject) allObjects.get(key);

			if (object == null && !getFromDB) {
				// get the reference
				if (referencingObject != null && referenceName != null) {
					ReferenceDescriptor referenceDescriptor = referencingObject.getSourceTable().getReferenceDescriptor(copy, referenceName);

					copy.getCopyController().handleUnsatisfiedLink(copy, referencingObject, referenceDescriptor, this, id);
					referenceDescriptor.handleUnsatisfiedReference(copy, referencingObject, this, id);
				}
			}
			if (object != null && !object.isReferencesFilled() && getObjectLinks) {
				getObjectLinks(copy, conn, allObjects, object, true);
			}
			return object;
		}
	}

	/**
	 * Get all objects of this table
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param restriction restriction for objects to fetch
	 * @param params parameters for the restriction
	 * @param allObjects map of all objects
	 * @param referenceName name of the reference that causes this object to be
	 *        copied
	 * @param referencedObject referencing object that causes this object to be
	 *        copied
	 * @return list of objects
	 * @throws StructureCopyException
	 */
	public List<DBObject> getObjects(StructureCopy copy, Connection conn, String restriction,
			Object[] params, Map<StructureCopy.ObjectKey, DBObject> allObjects, String referenceName, DBObject referencedObject) throws StructureCopyException {
		return getObjects(copy, conn, getName(), restriction, params, allObjects, referenceName, referencedObject);
	}

	/**
	 * Get the object's id from the given resultset
	 * @param res resultset pointing to the current row
	 * @return id of the object
	 * @throws SQLException
	 */
	public Object getId(ResultSet res) throws SQLException {
		if (!isCrossTable()) {
			return res.getObject(getIdcol());
		} else {
			Object[] key = new Object[crossTableId.length];

			for (int i = 0; i < crossTableId.length; i++) {
				key[i] = res.getObject(crossTableId[i]);
			}

			return new MultiReferenceKey(key);
		}
	}

	/**
	 * Get all objects of this table, with possibility to make joins
	 * @param copy copy configuration
	 * @param conn database conneciton
	 * @param fromClause from clause (may contain joins)
	 * @param restriction restriction for the fetched objects (may contain
	 *        columns from joined tables)
	 * @param params parameters for the restriction
	 * @param allObjects map of all objects
	 * @param referenceName name of the reference that causes this object to be
	 *        copied
	 * @param referencedObject referencing object that causes this object to be
	 *        copied
	 * @return list of objects
	 * @throws StructureCopyException
	 */
	public List<DBObject> getObjects(StructureCopy copy, Connection conn, String fromClause,
			String restriction, Object[] params, Map<StructureCopy.ObjectKey, DBObject> allObjects, String referenceName,
			DBObject referencedObject) throws StructureCopyException {
		List<DBObject> objects = copy.getCopyController().getObjects(copy, this, fromClause, restriction, params, allObjects, referenceName, referencedObject);

		// add the objects to the map (if not done so before)
		for (DBObject object : objects) {
			checkInterrupted();
			StructureCopy.ObjectKey key = StructureCopy.ObjectKey.getObjectKey(this, object.getId());

			if (!allObjects.containsKey(key)) {
				allObjects.put(key, object);
			}
		}
		return objects;
	}

	/**
	 * Get the object links for the given object
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param allObjects map of all objects
	 * @param object object to update
	 * @param firstRun true when this is the first run, false for the second run
	 * @throws SQLException
	 */
	public void getObjectLinks(StructureCopy copy, Connection conn, Map<StructureCopy.ObjectKey, DBObject> allObjects,
			DBObject object, boolean firstRun) throws StructureCopyException {
		object.setReferencesFilled(true);

		if (object instanceof ExcludedObject) {
			// do not get no linked objects for excluded objects
			return;
		}

		ReferenceRestrictor referenceRestrictor = copy.getCopyController().getCurrentReferenceRestrictor();

		// follow the references and get referenced objects
		if (isSetReferences()) {
			JAXBreferenceType[] ref = getReferences().getRef();

			for (int i = 0; i < ref.length; i++) {
				checkInterrupted();
				// only follow this link when deepcopy is true (default) or we
				// are in the second run
				if (!firstRun || !ref[i].isSetDeepcopy() || "true".equals(ref[i].getDeepcopy())) {
					ReferenceDescriptor referenceDescriptor = ((Reference) ref[i]).getReferenceDescriptor(conn);

					if (referenceDescriptor != null
							&& (referenceRestrictor == null || !referenceRestrictor.isReferenceRestricted(copy, referenceDescriptor, object, false))) {
						referenceDescriptor.getLinkedObject(copy, conn, object, allObjects, firstRun);
					}
				}
			}
		}

		// in the first run, we also fetch foreign linking objects
		if (firstRun) {
			// follow the foreign references and get foreign references objects
			for (Reference ref : foreignReferences) {
				checkInterrupted();
				ReferenceDescriptor referenceDescriptor = ref.getReferenceDescriptor(conn);

				if (referenceDescriptor != null
						&& (referenceRestrictor == null || !referenceRestrictor.isReferenceRestricted(copy, referenceDescriptor, object, true))) {
					List<DBObject> referencingObjects = referenceDescriptor.getLinkingObjects(copy, conn, object, allObjects);

					object.setForeignReference(referenceDescriptor, referencingObjects);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getId().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof Table) {
			return ((Table) obj).getId().equals(getId());
		} else {
			return false;
		}
	}

	/**
	 * Get all columns of the table
	 * @return array of all columns
	 */
	public String[] getAllColumns() {
		return allColumns;
	}

	/**
	 * Copy the object in the database, generate and set the new id. Only update
	 * the object links when the table is a cross table
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param object object to copy
	 * @param firstRun true for the first run (non-crosstables), false for the second run (crosstables)
	 * @throws StructureCopyException
	 */
	public void copyObject(StructureCopy copy, Connection conn, DBObject object, boolean firstRun) throws StructureCopyException {
		if (!object.getSourceTable().equals(this)) {
			return;
		}
		int actionTaken = copy.getCopyController().copyObject(copy, object, firstRun);

		// when object modificators are defined, let it modify the object now
		if (modificators != null) {
			for (int i = 0; i < modificators.length; i++) {
				checkInterrupted();
				modificators[i].modifyObject(copy, conn, object, actionTaken);
			}
		}
	}

	/**
	 * Update the object links for the given object, which already has to be
	 * copied before. This is only done for normal objects (no cross table
	 * objects).
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param object object to update
	 * @throws SQLException
	 */
	public void updateObjectLinks(StructureCopy copy, Connection conn, DBObject object) throws StructureCopyException {
		if (!object.getSourceTable().equals(this) || isCrossTable()) {
			return;
		}

		copy.getCopyController().updateObjectLinks(copy, object);
	}

	/**
	 * Generates a where statement for the given object.
	 * Adds the values into the 'values' list.
	 */
	public String generateWhereStatement(DBObject obj, List<Object> values) {
		StringBuffer buf = new StringBuffer(" ");

		if (isCrossTable()) {
			String[] ids = getCrossTableId();

			for (int i = 0; i < ids.length; i++) {
				if (i > 0) {
					buf.append(" AND ");
				}
				buf.append(ids[i]).append(" = ? ");
			}
			MultiReferenceKey key = (MultiReferenceKey) obj.getId();

			values.addAll(Arrays.asList(key.getReferences()));
		} else {
			buf.append(getIdcol()).append(" = ? ");
			values.add(obj.getId());
		}
		return buf.toString();
	}

	/**
	 * Class for object keys that contain of more than ony column. Reimplements
	 * methods {@link #hashCode()} and {@link #equals(Object)} such that objects
	 * with the same series of column values are assumed to be equal.
	 */
	public static class MultiReferenceKey {

		/**
		 * column values
		 */
		protected Object[] references;

		/**
		 * Create an instance of the key
		 * @param references column values
		 */
		public MultiReferenceKey(Object[] references) {
			this.references = references;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			int hash = 0;

			for (int i = 0; i < references.length; i++) {
				hash += references[i] != null ? references[i].hashCode() : 0;
			}

			return hash;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof MultiReferenceKey) {
				return Arrays.equals(references, ((MultiReferenceKey) obj).references);
			} else {
				return false;
			}
		}

		/**
		 * Get the references (parts of the key)
		 * @return object array of references
		 */
		public Object[] getReferences() {
			return references;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return StringUtils.merge(references, ",");
		}
	}

	// /**
	// * Updates the names of the "reference columns"
	// * @param meta
	// * @throws SQLException
	// * @see #getReferenceColumns()
	// */
	// public void updateReferenceColumns(ResultSetMetaData meta) throws SQLException {
	// int colCount = meta.getColumnCount();
	// String[] refCols = new String[colCount];
	// for(int i = 1 ; i <= colCount ; i++) {
	// refCols[i-1] = meta.getColumnName(i);
	// }
	// this.dbReferenceColumns = refCols;
	// }

	/**
	 * returns the names of the reference columns in a defined order
	 * which is not changed during one execution.
	 *
	 * It contains all necessary columns which {@link DBObject}s have to store.
	 */
	public String[] getReferenceColumns() {
		return this.dbReferenceColumns;
	}

	/**
	 * Get the reference descriptor of the first reference found that is targeted to the given table
	 * @param copy copy configuration
	 * @param tableName name of the target table
	 * @return reference descriptor or null if none found
	 * @throws StructureCopyException
	 */
	public ReferenceDescriptor getReferenceDescriptorToTable(StructureCopy copy,
			String tableName) throws StructureCopyException {
		if (tableName == null) {
			return null;
		}

		if (isSetReferences()) {
			JAXBreferenceType[] references = getReferences().getRef();

			for (int i = 0; i < references.length; i++) {
				Reference reference = (Reference) references[i];

				if (tableName.equals(reference.getTarget())) {
					return reference.getReferenceDescriptor(copy.getConnection());
				}
			}
		}
		return null;
	}

	/**
	 * Get the reference descriptor with the given name
	 * @param copy copy configuration
	 * @param name name of the reference (which is actually the name of the column holding the referenced id)
	 * @return reference descriptor or null if none found
	 * @throws StructureCopyException
	 */
	public ReferenceDescriptor getReferenceDescriptor(StructureCopy copy, String name) throws StructureCopyException {
		if (name == null) {
			return null;
		}
		if (isSetReferences()) {
			JAXBreferenceType[] references = getReferences().getRef();

			for (int i = 0; i < references.length; i++) {
				Reference reference = (Reference) references[i];

				if (name.equals(reference.getReferenceDescriptor(copy.getConnection()).getLinkColumn())) {
					return reference.getReferenceDescriptor(copy.getConnection());
				}
			}
		}
		return null;
	}

	/**
	 * Get the table property
	 * @param key property key
	 * @return value or null
	 */
	public String getProperty(String key) {
		if (properties == null) {
			properties = new HashMap<String, String>();
			if (isSetProperties() && getProperties().isSetPropertyList()) {
				PropertyType[] propertyList = getProperties().getPropertyList();

				for (int i = 0; i < propertyList.length; i++) {
					properties.put(propertyList[i].getId(), propertyList[i].getValue());
				}
			}
		}
		return ObjectTransformer.getString(properties.get(key), null);
	}

	/**
	 * Get the selected fields
	 * @return selected fields
	 */
	public String getSelectedFields() {
		return selectedFields;
	}

	/**
	 * Get all data columns
	 * @return data columns
	 */
	public String[] getAllDataColumns() {
		return allDataColumns;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.jaxb.impl.JAXBtableTypeImpl#getId()
	 */
	public String getId() {
		if (isSetId()) {
			return super.getId();
		} else {
			// the id defaults to the name
			return getName();
		}
	}

	/**
	 * Get the insert command
	 * @return insert command
	 */
	public String getInsertCommand() {
		return insertCommand;
	}

	/**
	 * Get the SQL Statement for selecting the global ID
	 * @return SQL Statement
	 */
	public String getSelectGlobalIdSQL() {
		return selectGlobalIdSQL;
	}

	/**
	 * Get the SQL Statement for selecting the local ID
	 * @return SQL Statement
	 */
	public String getSelectLocalIdSQL() {
		return selectLocalIdSQL;
	}

	/**
	 * Get all but the id column
	 * @return all columns but the id column
	 */
	public String[] getAllButIdColumn() {
		return allButIdColumn;
	}

	/**
	 * Get names of the id fields in a cross table
	 * @return names of the id fields
	 */
	public String[] getCrossTableId() {
		return crossTableId;
	}

	/**
	 * Method to get the value for the given column from the given dataMap, considering configured default values.
	 * @param dataMap data map containing the column values
	 * @param columnName column name
	 * @return either the value found in the dataMap, or the default value (if any). null otherwise
	 */
	public Object getColumnValue(Map<String, Object> dataMap, String columnName) {
		if (dataMap.containsKey(columnName)) {
			return dataMap.get(columnName);
		} else if (defaultValues.containsKey(columnName)) {
			return defaultValues.get(columnName);
		} else {
			return null;
		}
	}

	/**
	 * Check interrupted status of thread (set when user cancels operation).
	 * @throws StructureCopyInterruptedException if thread has been interrupted
	 */
	protected static void checkInterrupted() throws StructureCopyInterruptedException {
		if (Thread.interrupted()) {
			throw new StructureCopyInterruptedException();
		}
	}
}
