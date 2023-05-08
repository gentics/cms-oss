package com.gentics.lib.datasource;

import java.io.File;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.auth.GenticsUser;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceInfo;
import com.gentics.api.lib.datasource.DatasourceModificationException;
import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.datasource.WriteableVersioningDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.FilesystemAttributeValue;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.content.PreparedBatchStatement;
import com.gentics.lib.content.ResolvableGenticsContentObject;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.db.StringLengthManipulator;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * @author haymo
 * @version $Id: CNWriteableDatasource.java,v 1.14 2005/07/27 13:00:12 laurin
 *          Exp $
 */
public class CNWriteableDatasource extends CNDatasource implements
		WriteableVersioningDatasource {

	/**
	 * Insert statement for table contentattribute including all available columns
	 */
	protected final static String BATCH_INSERT = "INSERT INTO {tablename} (contentid, name, sortorder, value_text, value_bin, value_int, value_blob, value_clob, value_long, value_double, value_date) VALUES ("
			+ StringUtils.repeat("?", 11, ",") + ")";

	/**
	 * Types of the parameters in {@link #BATCH_INSERT}
	 */
	protected final static List<Integer> BATCH_INSERT_TYPES = Arrays.asList(Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.BLOB, Types.INTEGER,
			Types.BLOB, Types.LONGVARCHAR, Types.BIGINT, Types.DOUBLE, Types.TIMESTAMP);

	/**
	 * Update statement for table contentattribute including all available columns
	 */
	protected final static String BATCH_UPDATE = "UPDATE {tablename} SET value_text = ?, value_bin = ?, value_int = ?, value_blob = ?, value_clob = ?, value_long = ?, value_double = ?, value_date = ? WHERE contentid = ? AND name = ?";

	/**
	 * Types of the parameters in {@link #BATCH_UPDATE}
	 */
	protected final static List<Integer> BATCH_UPDATE_TYPES = Arrays.asList(Types.VARCHAR, Types.BLOB, Types.INTEGER, Types.BLOB, Types.LONGVARCHAR, Types.BIGINT,
			Types.DOUBLE, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR);

	/**
	 * Delete statement for table contentattribute
	 */
	protected final static String BATCH_DELETE = "DELETE FROM {tablename} WHERE contentid = ? AND name = ?";

	/**
	 * Types of the parameters in {@link #BATCH_DELETE}
	 */
	protected final static List<Integer> BATCH_DELETE_TYPES = Arrays.asList(Types.VARCHAR, Types.VARCHAR);

	/**
	 * Map containing the indices for attributes to be used in {@link #BATCH_INSERT}
	 */
	protected final static Map<String, Integer> BATCH_INSERT_COLUMNS = new HashMap<String, Integer>();

	/**
	 * Map containing the indices for attributes to be used in {@link #BATCH_UPDATE}
	 */
	protected final static Map<String, Integer> BATCH_UPDATE_COLUMNS = new HashMap<String, Integer>();

	/**
	 * Map containing the indices for attributes to be used in {@link #BATCH_DELETE}
	 */
	protected final static Map<String, Integer> BATCH_DELETE_COLUMNS = new HashMap<String, Integer>();

	/**
	 * Vector holding protected attribute names. Those attribute names will not
	 * be written when creating a new object
	 */
	private final static Vector<String> PROTECTED_ATTRIBUTES = new Vector<String>();
    
	private boolean allowSettingObjType = false;

	/**
	 * when this flag is set, the datasource will write the last updatetimestamp in the contentstatus table on every insert/update/delete
	 */
	private boolean setUpdatetimestampOnWrite = false;

	/**
	 * when this flag is set, the datasource will automatically repair the
	 * id_counter column in the contentobject table, every time at least one
	 * object was inserted with a given contentid. If the flag is not set, the
	 * method {@link #repairIdCounters(List)} can be used to repair the
	 * id_counter (CRSync and publish process do this).
	 */
	private boolean repairIDCounterOnInsert = true;

	static {
		PROTECTED_ATTRIBUTES.add("contentid");
		PROTECTED_ATTRIBUTES.add("obj_type");
		PROTECTED_ATTRIBUTES.add("obj_id");
		PROTECTED_ATTRIBUTES.add("updatetimestamp");

		BATCH_INSERT_COLUMNS.put("contentid", 0);
		BATCH_INSERT_COLUMNS.put("name", 1);
		BATCH_INSERT_COLUMNS.put("sortorder", 2);
		BATCH_INSERT_COLUMNS.put("value_text", 3);
		BATCH_INSERT_COLUMNS.put("value_bin", 4);
		BATCH_INSERT_COLUMNS.put("value_int", 5);
		BATCH_INSERT_COLUMNS.put("value_blob", 6);
		BATCH_INSERT_COLUMNS.put("value_clob", 7);
		BATCH_INSERT_COLUMNS.put("value_long", 8);
		BATCH_INSERT_COLUMNS.put("value_double", 9);
		BATCH_INSERT_COLUMNS.put("value_date", 10);

		BATCH_UPDATE_COLUMNS.put("value_text", 0);
		BATCH_UPDATE_COLUMNS.put("value_bin", 1);
		BATCH_UPDATE_COLUMNS.put("value_int", 2);
		BATCH_UPDATE_COLUMNS.put("value_blob", 3);
		BATCH_UPDATE_COLUMNS.put("value_clob", 4);
		BATCH_UPDATE_COLUMNS.put("value_long", 5);
		BATCH_UPDATE_COLUMNS.put("value_double", 6);
		BATCH_UPDATE_COLUMNS.put("value_date", 7);
		BATCH_UPDATE_COLUMNS.put("contentid", 8);
		BATCH_UPDATE_COLUMNS.put("name", 9);

		BATCH_DELETE_COLUMNS.put("contentid", 0);
		BATCH_DELETE_COLUMNS.put("name", 1);
	}

	public CNWriteableDatasource(String id, HandlePool handle, Map parameters) {
		super(id, handle, parameters);
		if (parameters != null) {
			allowSettingObjType = ObjectTransformer.getBoolean(parameters.get("allow_setting_obj_type"), false);
			setUpdatetimestampOnWrite = ObjectTransformer.getBoolean(parameters.get("setUpdatetimestampOnWrite"), setUpdatetimestampOnWrite);
		}
	}

	/**
	 * Set the flag {@link #repairIDCounterOnInsert}
	 * @param repairIDCounterOnInsert true when the id_counter shall be repaired
	 *        on every insert, false if not
	 */
	public void setRepairIDCounterOnInsert(boolean repairIDCounterOnInsert) {
		this.repairIDCounterOnInsert = repairIDCounterOnInsert;
	}

	public boolean canWrite() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#store(com.gentics.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo store(DatasourceRecordSet rst) throws DatasourceException {
		return store(rst, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#store(com.gentics.lib.datasource.DatasourceRecordSet,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo store(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_STORE);

			// split the record set into two, ony for updating, one for
			// inserting
			DatasourceRecordSet toInsert = new CNDatasourceRecordSet(this);
			DatasourceRecordSet toUpdate = new CNDatasourceRecordSet(this);

			for (Iterator i = rst.iterator(); i.hasNext();) {
				DatasourceRow row = (DatasourceRow) i.next();

				GenticsContentObject cnObj = (GenticsContentObject) row.toObject();

				if (cnObj == null) {
					continue;
				}

				// TODO when the datasource of the cnObj is not this datasource,
				// we clone the object
				if (cnObj.getDatasource() != this) {
					cnObj = cloneObject(cnObj);
					row = new CNDatasourceRow(cnObj);
				}
				if (checkExistence(cnObj)) {
					toUpdate.addRow(row);
				} else {
					toInsert.addRow(row);
				}
			}
			DefaultDatasourceInfo info = new DefaultDatasourceInfo();
			CNDatasourceRecordSet infoRecordSet = new CNDatasourceRecordSet(this);

			info.setDatasourceRecordSet(infoRecordSet);

			// do the inserts
			if (toInsert.size() > 0) {
				DatasourceInfo insertInfo = insert(toInsert, user);

				if (insertInfo != null) {
					Collection insertSet = insertInfo.getAffectedRecords();

					for (Iterator i = insertSet.iterator(); i.hasNext();) {
						infoRecordSet.addRow((DatasourceRow) i.next());
					}
				}
			}
			// do the updates
			if (toUpdate.size() > 0) {
				DatasourceInfo updateInfo = update(toUpdate, user);

				if (updateInfo != null) {
					Collection updateSet = updateInfo.getAffectedRecords();

					for (Iterator i = updateSet.iterator(); i.hasNext();) {
						infoRecordSet.addRow((DatasourceRow) i.next());
					}
				}
			}

			return info;
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_CN_STORE);
		}
	}

	/**
	 * Checks if a given content object already exists.
	 * @param cnObj the cn object to check
	 * @return true if object exists.
	 * @throws DatasourceException
	 */
	private boolean checkExistence(GenticsContentObject cnObj) throws DatasourceException {
		// for existance checking, we always have to check the current
		// version of the object
		GenticsContentObject currentVersion = null;

		if (cnObj.isCurrentVersion()) {
			currentVersion = cnObj;
		} else if (!GenticsContentFactory.isTemporary(cnObj.getContentId())) {
			try {
				currentVersion = GenticsContentFactory.createContentObject(cnObj.getContentId(), this, true);
			} catch (Exception e) {
				throw new DatasourceException(e);
			}
		}
		boolean isExisting = currentVersion != null && currentVersion.exists();

		return isExisting;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#store(java.util.Collection)
	 */
	public DatasourceInfo store(Collection objects) throws DatasourceException {
		return store(objects, null);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#store(java.util.Collection, com.gentics.lib.user.GenticsUser)
	 */
	public DatasourceInfo store(Collection objects, GenticsUser user) throws DatasourceException {
		DatasourceRecordSet set = new CNDatasourceRecordSet(this);

		for (Iterator iter = objects.iterator(); iter.hasNext();) {
			Object element = iter.next();

			// convert the object into a CNDatasourceRow
			CNDatasourceRow row = getCNDatasourceRow(element);

			if (row != null) {
				// add the row
				set.addRow(row);
			} else {
				// could not add the row, so print a warning
				if (element == null) {
					logger.warn("Found null in the collection of objects to store, ignoring.");
				} else {
					logger.warn("Found object of class {" + element.getClass().getName() + "}, which cannot be stored. Ignoring.");
				}
			}
		}

		return store(set, user);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#update(java.util.Collection)
	 */
	public DatasourceInfo update(Collection objects) throws DatasourceException {
		return update(objects, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#update(com.gentics.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo update(DatasourceRecordSet rst) throws DatasourceException {
		return update(rst, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#insert(java.util.Collection)
	 */
	public DatasourceInfo insert(Collection objects) throws DatasourceException {
		return insert(objects, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#insert(com.gentics.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo insert(DatasourceRecordSet rst) throws DatasourceException {
		return insert(rst, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#delete(java.util.Collection)
	 */
	public DatasourceInfo delete(Collection objects) throws DatasourceException {
		return delete(objects, null);
	}
    
	public DatasourceInfo delete(DatasourceFilter filter) throws DatasourceException {
		return delete(getResult(filter, null));
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#delete(com.gentics.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo delete(DatasourceRecordSet rst) throws DatasourceException {
		return delete(rst, null);
	}

	/**
	 * update the given single value attribute in the database
	 * @param originalObject object for which the attribute has to be updated
	 * @param attribute attribute to update
	 * @return true when the attribute really was changed, false if not
	 */
	protected boolean updateSinglevalueAttribute(GenticsContentObject originalObject,
			GenticsContentAttribute attribute) throws CMSUnavailableException,
				NodeIllegalArgumentException, SQLException {
		boolean attributeChanged = false;

		attribute.resetIterator();
		Object newValue = null;

		if (attribute.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_BLOB) {
			newValue = attribute.getNextBinaryValue();
		} else {
            
			// truncate the value
			if (attribute.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_TEXT) {
				String stringValue = attribute.getNextValue();

				newValue = truncateAttributeValue(stringValue, attribute.getParent().getContentId(), attribute.getAttributeName(),
						getHandle().getDBHandle().getStringLengthManipulator());
			} else {
				newValue = attribute.getNextObjectValue();
			}
		}

		// get the original attribute (if original object exists)
		GenticsContentAttribute originalAttribute = originalObject != null ? originalObject.getAttribute(attribute.getAttributeName()) : null;

		if (newValue != null && !"".equals(newValue)) {
			// check whether the attribute is in the database
			if (originalAttribute != null && originalAttribute.isSet() && (originalAttribute.getValues() == null || originalAttribute.getValues().size() == 1)) {
				// attribute exists in the database -> update it
				// get the original attribute value
				Object originalValue = null;

				if (originalAttribute.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_BLOB) {
					originalValue = originalAttribute.getNextBinaryValue();
				} else {
					originalValue = originalAttribute.getNextObjectValue();
				}

				if (!newValue.equals(originalValue)) {
					if (logger.isDebugEnabled()) {
						logger.debug(
								"updating singlevalue attribute '" + attribute.getAttributeName() + "' for object '" + attribute.getParent().getContentId() + "'");
					}
					if (logger.isWarnEnabled()) {
						// do check for existing attribute values
						SimpleResultProcessor resProc = new SimpleResultProcessor();

						DB.query(getHandle().getDBHandle(),
								"select count(*) existing_values from " + getHandle().getDBHandle().getContentAttributeName() + " where contentid = ? and name = ?",
								new String[] { attribute.getParent().getContentId(), attribute.getAttributeName()}, resProc);
						Iterator iter = resProc.iterator();

						if (iter.hasNext()) {
							SimpleResultRow row = (SimpleResultRow) iter.next();
							int existing = row.getInt("existing_values");

							if (existing != 1) {
								logger.warn(
										"Found {" + existing + "} existing values for " + getHandle().getDBHandle().getContentAttributeName() + " {"
										+ attribute.getAttributeName() + "} contentid {" + attribute.getParent().getContentId()
										+ "} where 1 was expected. This will result in database inconsistency!");
							}
						}
					}

					List params = new Vector();

					params.add(newValue);
					params.add(attribute.getParent().getContentId());
					params.add(attribute.getAttributeName());
					String sql = "UPDATE " + getHandle().getDBHandle().getContentAttributeName() + " set " + getDataTypeColumn(attribute.getAttributeName())
							+ " = ? WHERE contentid = ? AND name = ?";
					int modifiedRows = DB.update(getHandle().getDBHandle(), sql, params.toArray(), null);

					if (modifiedRows != 1) {
						logger.error(
								"Tried updating a single value attribut, but {" + modifiedRows
								+ "} rows were affected. deleting all attributes of that name to insert it.");
						insertSinglevalueAttribute(attribute, newValue);
					}
					attributeChanged = true;
				}
			} else {
				insertSinglevalueAttribute(attribute, newValue);
				attributeChanged = true;
			}
		} else {
			if (originalAttribute != null && originalAttribute.isSet()) {
				if (logger.isDebugEnabled()) {
					logger.debug(
							"removing singlevalue attribute '" + attribute.getAttributeName() + "' for object '" + attribute.getParent().getContentId() + "'");
				}
				// attribute exists in the database -> remove it
				String sql = "DELETE FROM " + getHandle().getDBHandle().getContentAttributeName() + " WHERE contentid = ? and name = ?";

				DB.update(getHandle().getDBHandle(), sql, new Object[] {
					attribute.getParent().getContentId(), attribute.getAttributeName()}, null);
				attributeChanged = true;
			}
		}

		if (attributeChanged) {
			// ensure that the attribute is removed from the cache since it is changed
			try {
				clearQueryCache();
				GenticsContentFactory.uncacheAttribute(this, attribute.getParent(), attribute);
			} catch (PortalCacheException e) {
				logger.warn("Error while removing attribute from cache", e);
			}
		}

		return attributeChanged;
	}

	private void insertSinglevalueAttribute(GenticsContentAttribute attribute, Object newValue) throws SQLException, CMSUnavailableException {
		// cleanup old entries which may be left over/invalid references 
		String sql = "DELETE FROM " + getHandle().getDBHandle().getContentAttributeName() + " WHERE contentid = ? and name = ?";

		DB.update(getHandle().getDBHandle(), sql, new Object[] {
			attribute.getParent().getContentId(), attribute.getAttributeName()}, null);

		// insert the attribute into the database
		if (logger.isDebugEnabled()) {
			logger.debug("inserting singlevalue attribute '" + attribute.getAttributeName() + "' for object '" + attribute.getParent().getContentId() + "'");
		}

		sql = "INSERT INTO " + getHandle().getDBHandle().getContentAttributeName() + " (" + getDataTypeColumn(attribute.getAttributeName())
				+ ", contentid, name) values (?, ?, ?)";
		DB.update(getHandle().getDBHandle(), sql, new Object[] { newValue, attribute.getParent().getContentId(), attribute.getAttributeName()}, null);
	}

	/**
	 * update the given single value attribute in the database
	 * @param originalObject object for which the attribute has to be updated
	 * @param attribute attribute to update
	 * @param insert prepared batch insert statement
	 * @param update prepared batch update statement
	 * @param delete prepared batch delete statement
	 * @return true when the attribute really was changed, false if not
	 */
	protected boolean updateSinglevalueAttribute(GenticsContentObject originalObject,
			GenticsContentAttribute attribute, PreparedBatchStatement insert, PreparedBatchStatement update, PreparedBatchStatement delete) throws CMSUnavailableException,
				NodeIllegalArgumentException, SQLException {
		boolean attributeChanged = false;

		attribute.resetIterator();
		Object newValue = null;

		if (attribute.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_BLOB) {
			newValue = attribute.getNextBinaryValue();
		} else {
            
			// truncate the value
			if (attribute.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_TEXT) {
				String stringValue = attribute.getNextValue();

				newValue = truncateAttributeValue(stringValue, attribute.getParent().getContentId(), attribute.getAttributeName(),
						getHandle().getDBHandle().getStringLengthManipulator());
			} else {
				newValue = attribute.getNextObjectValue();
			}
		}

		// get the original attribute (if original object exists)
		GenticsContentAttribute originalAttribute = originalObject != null ? originalObject.getAttribute(attribute.getAttributeName()) : null;

		if (newValue != null && !"".equals(newValue)) {
			// check whether the attribute is in the database
			if (originalAttribute != null && originalAttribute.isSet() && (originalAttribute.getValues() == null || originalAttribute.getValues().size() == 1)) {
				// attribute exists in the database -> update it
				// get the original attribute value
				Object originalValue = null;

				if (originalAttribute.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_BLOB) {
					originalValue = originalAttribute.getNextBinaryValue();
				} else {
					originalValue = originalAttribute.getNextObjectValue();
				}

				if (!newValue.equals(originalValue)) {
					if (logger.isDebugEnabled()) {
						logger.debug(
								"updating singlevalue attribute '" + attribute.getAttributeName() + "' for object '" + attribute.getParent().getContentId() + "'");
					}

					Map<String, Object> data = new HashMap<>();
					data.put(getDataTypeColumn(attribute.getAttributeName()), newValue);
					data.put("contentid", attribute.getParent().getContentId());
					data.put("name", attribute.getAttributeName());
					update.add(data);

					attributeChanged = true;
				}
			} else if (originalAttribute != null && originalAttribute.isSet()) {
				// original attribute had more than one value
				insertSinglevalueAttribute(attribute, newValue, insert, delete, true);
				attributeChanged = true;
			} else {
				insertSinglevalueAttribute(attribute, newValue, insert, delete, false);
				attributeChanged = true;
			}
		} else {
			if (originalAttribute != null && originalAttribute.isSet()) {
				if (logger.isDebugEnabled()) {
					logger.debug(
							"removing singlevalue attribute '" + attribute.getAttributeName() + "' for object '" + attribute.getParent().getContentId() + "'");
				}

				// attribute exists in the database -> remove it
				Map<String, Object> data = new HashMap<>();
				data.put("contentid", attribute.getParent().getContentId());
				data.put("name", attribute.getAttributeName());
				delete.add(data);

				attributeChanged = true;
			}
		}

		if (attributeChanged) {
			// ensure that the attribute is removed from the cache since it is changed
			try {
				clearQueryCache();
				GenticsContentFactory.uncacheAttribute(this, attribute.getParent(), attribute);
			} catch (PortalCacheException e) {
				logger.warn("Error while removing attribute from cache", e);
			}
		}

		return attributeChanged;
	}

	private void insertSinglevalueAttribute(GenticsContentAttribute attribute, Object newValue, PreparedBatchStatement insert, PreparedBatchStatement delete, boolean cleanFirst) throws SQLException, CMSUnavailableException {
		if (logger.isDebugEnabled()) {
			logger.debug("inserting singlevalue attribute '" + attribute.getAttributeName() + "' for object '" + attribute.getParent().getContentId() + "'");
		}

		Map<String, Object> data = new HashMap<>();
		data.put("contentid", attribute.getParent().getContentId());
		data.put("name", attribute.getAttributeName());

		if (cleanFirst) {
			// cleanup old entries which may be left over/invalid references 
			delete.add(data);
		}

		data.put(getDataTypeColumn(attribute.getAttributeName()), newValue);
		insert.add(data);
	}

	/**
	 * Update the given foreign link attribute objects in the database
	 * @param originalObject object for which the attribute has to be updated
	 * @param attribute attribute to update
	 * @param user user performing the action
	 * @return true when the attribute really was changed, false if not
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 * @throws SQLException
	 */
	protected boolean updateForeignLinkedObjects(GenticsContentObject originalObject,
			GenticsContentObject cnObj, GenticsContentAttribute attribute, GenticsUser user) throws CMSUnavailableException,
				NodeIllegalArgumentException, SQLException, DatasourceException {
		boolean attributeChanged = false;

		// get the objects to insert or update
		Object value = cnObj.get(attribute.getAttributeName());

		// get all objects that were there, but are there no longer
		if (originalObject != null) {
			Object originalValue = originalObject.get(attribute.getAttributeName());

			if (originalValue instanceof Collection) {
				Collection originalCol = (Collection) originalValue;

				if (value instanceof Collection) {
					originalCol.removeAll((Collection) value);
				}

				// remove all objects that are no longer there
				if (originalCol.size() > 0) {
					delete(originalCol, user);
				}
			}
		}

		if (value instanceof Collection) {
			Collection valueCol = (Collection) value;

			for (Iterator iter = valueCol.iterator(); iter.hasNext();) {
				Object element = (Object) iter.next();

				if (element instanceof GenticsContentObject) {
					GenticsContentObject foreignObject = (GenticsContentObject) element;

					// ensure the objects are correctly linked
					try {
						foreignObject.setProperty(attribute.getForeignLinkAttribute(), cnObj.getContentId());
					} catch (InsufficientPrivilegesException e) {// ignored, must not happen
					}
					foreignObject.setVersionTimestamp(cnObj.getVersionTimestamp());
				}
			}

			// store the foreign linked objects
			store(valueCol, user);
		}

		return attributeChanged;
	}

	/**
	 * update the given multi value attribute in the database
	 * @param originalObject object for which the attribute has to be updated
	 * @param attribute attribute to update
	 * @return true when the attribute really was changed, false if not
	 */
	protected boolean updateMultivalueAttribute(GenticsContentObject originalObject,
			GenticsContentAttribute attribute) throws CMSUnavailableException,
				NodeIllegalArgumentException, SQLException {
		boolean attributeChanged = false;
		DBHandle dbHandle = getHandle().getDBHandle();
		String contentId = attribute.getParent().getContentId();
		String name = attribute.getAttributeName();
		String dataTypeColumn = getDataTypeColumn(name);

		attribute.resetIterator();
		// get all existing values of the attribute
		GenticsContentAttribute originalAttribute = originalObject != null ? originalObject.getAttribute(attribute.getAttributeName()) : null;

		// prepare the original values and new values (both as strings)
		List originalValues = new Vector();

		if (originalAttribute != null) {
			for (Iterator i = originalAttribute.getValues().iterator(); i.hasNext();) {
				Object o = i.next();

				if (o != null) {
					originalValues.add(originalAttribute.normalizeValue(o));
				}
			}
		}
        
		// truncate the values if needed
		boolean needsTruncateCheck = attribute.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_TEXT;
        
		List newValues = new Vector();

		for (Iterator i = attribute.getValues().iterator(); i.hasNext();) {
			Object o = i.next();

			if (o != null) {
				Object val = originalAttribute.normalizeValue(o);

				if (needsTruncateCheck) {
					String stringValue = ObjectTransformer.getString(val, null);

					val = truncateAttributeValue(stringValue, contentId, name, dbHandle.getStringLengthManipulator());
				}
				if (val != null) {
					newValues.add(val);
				}
			}
		}

		// check now whether the sortorder needs to be fixed
		if (originalAttribute.needsSortorderFixed()) {
			// the sortorder needs to be fixed. we do this by a complete
			// delete/insert for all values of the attribute
			logger.warn(
					"Need to fix invalid sortorder for multivalue attribute {" + originalAttribute.getAttributeName() + "} of object {"
					+ originalObject.getContentId() + "}");
			// remove all currently stored values
			DB.update(dbHandle, "DELETE FROM " + getHandle().getDBHandle().getContentAttributeName() + " WHERE contentid = ? AND name = ?",
					new Object[] { contentId, name}, null);

			// and store all new values

			int sortorder = 0;
			StringBuffer toInsertSql = new StringBuffer();

			toInsertSql.append("INSERT INTO " + getHandle().getDBHandle().getContentAttributeName() + " (");
			toInsertSql.append(getDataTypeColumn(attribute.getAttributeName()));
			toInsertSql.append(", contentid, name, sortorder) values");

			int toInsertSize = newValues.size();
			List params = new Vector();

			toInsertSql.append(" (?,?,?,?)");
			for (int i = 0; i < toInsertSize; ++i) {
				params.clear();
				params.add(newValues.get(i));
				params.add(contentId);
				params.add(name);
				params.add(new Integer(++sortorder));
				DB.update(getHandle().getDBHandle(), toInsertSql.toString(), params.toArray(), null);
			}
			attributeChanged = true;
		} else {

			// make diff between the existing and new attributes

			Iterator origIter = originalValues.iterator();
			Iterator newIter = newValues.iterator();

			List toUpdate = new Vector();
			List toDelete = new Vector();
			List toInsert = new Vector();

			int sortorder = 0;

			while (origIter.hasNext() && newIter.hasNext()) {
				sortorder++;
				Object origValue = origIter.next();
				Object newValue = newIter.next();

				if (!newValue.equals(origValue)) {
					toUpdate.add(new Object[] { origValue, newValue, new Integer(sortorder)});
				}
			}

			while (origIter.hasNext()) {
				toDelete.add(origIter.next());
			}

			while (newIter.hasNext()) {
				toInsert.add(newIter.next());
			}

			// update the values to update
			if (toUpdate.size() > 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("updating " + toUpdate.size() + " values from multivalue attribute {" + name + "} for object {" + contentId + "}");
				}
				StringBuffer updateSQL = new StringBuffer();

				updateSQL.append("UPDATE " + getHandle().getDBHandle().getContentAttributeName() + " SET ").append(dataTypeColumn).append(" = ? WHERE contentid = ? AND name = ? AND sortorder = ? AND ").append(
						DatatypeHelper.getDBSpecificComparisonStatement(DB.getDatabaseProductName(dbHandle), dataTypeColumn, attribute.getRealAttributeType()));
				String updateSQLString = updateSQL.toString();

				// prepare another version of the update statement (for updating empty values)
				updateSQL = new StringBuffer();
				updateSQL.append("UPDATE " + getHandle().getDBHandle().getContentAttributeName() + " SET ").append(dataTypeColumn).append(" = ? WHERE contentid = ? AND name = ? AND sortorder = ? AND (").append(
						DatatypeHelper.getDBSpecificComparisonStatement(DB.getDatabaseProductName(dbHandle), dataTypeColumn, attribute.getRealAttributeType()));
				updateSQL.append(" OR ").append(dataTypeColumn).append(" = '' OR ").append(dataTypeColumn).append(" IS NULL)");
				String updateSQLStringForEmpty = updateSQL.toString();

				for (Iterator iter = toUpdate.iterator(); iter.hasNext();) {
					Object[] values = (Object[]) iter.next();

					if (ObjectTransformer.isEmpty(values[0])) {
						DB.update(dbHandle, updateSQLStringForEmpty, new Object[] { values[1], contentId, name, values[2], values[0]});
					} else {
						DB.update(dbHandle, updateSQLString, new Object[] { values[1], contentId, name, values[2], values[0]});
					}
					attributeChanged = true;
				}
			}
        
			// delete the values to delete
			if (toDelete.size() > 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("removing " + toDelete.size() + " values from multivalue attribute {" + name + "} for object {" + contentId + "}");
				}
				StringBuffer toDeleteSql = new StringBuffer();

				toDeleteSql.append("DELETE FROM " + getHandle().getDBHandle().getContentAttributeName() + " WHERE contentid = ? AND name = ? AND sortorder > ?");
        
				DB.update(getHandle().getDBHandle(), toDeleteSql.toString(), new Object[] {
					contentId, name, new Integer(sortorder)}, null);
				attributeChanged = true;
			}
        
			// insert the values to insert
			if (toInsert.size() > 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("adding " + toInsert.size() + " values to multivalue attribute {" + name + "} for object {" + contentId + "}");
				}
				StringBuffer toInsertSql = new StringBuffer();

				toInsertSql.append("INSERT INTO " + getHandle().getDBHandle().getContentAttributeName() + " (");
				toInsertSql.append(getDataTypeColumn(attribute.getAttributeName()));
				toInsertSql.append(", contentid, name, sortorder) values");
        
				int toInsertSize = toInsert.size();
				List params = new Vector();
        
				toInsertSql.append(" (?,?,?,?)");
				for (int i = 0; i < toInsertSize; ++i) {
					params.clear();
					params.add(toInsert.get(i));
					params.add(contentId);
					params.add(name);
					params.add(new Integer(++sortorder));
					DB.update(getHandle().getDBHandle(), toInsertSql.toString(), params.toArray(), null);
					attributeChanged = true;
				}
			}
		}

		if (attributeChanged) {
			// ensure that the attribute is removed from the cache since it is changed
			try {
				clearQueryCache();
				GenticsContentFactory.uncacheAttribute(this, attribute.getParent(), attribute);
			} catch (PortalCacheException e) {
				logger.warn("Error while removing attribute from cache", e);
			}
		}

		return attributeChanged;
	}

	/**
	 * Insert the given attribute into the filesystem
	 * @param attribute attribute
	 * @throws DatasourceException
	 */
	protected void insertFilesystemAttribute(GenticsContentAttribute attribute) throws DatasourceException {
		GenticsContentObject parent = attribute.getParent();
		String basePath = getAttributePath();

		if (ObjectTransformer.isEmpty(basePath)) {
			throw new DatasourceException(
					"Error while saving attribute '" + attribute.getAttributeName() + "' for object '" + parent + "' into filesystem: basepath is empty");
		}

		// iterate over the values (might be streams)
		List<FilesystemAttributeValue> values = attribute.getFSValues();
		int sortOrder = 0;

		for (FilesystemAttributeValue fsValue : values) {
			sortOrder++;
			try {
				fsValue.saveData(getHandle().getDBHandle(), basePath, attribute, attribute.isMultivalue() ? sortOrder : 0);
			} catch (CMSUnavailableException e) {
				throw new DatasourceException(e);
			}
		}
	}

	/**
	 * Update the existing filesystem attribute
	 * @param originalObject original object
	 * @param attribute attribute
	 * @return true if the attribute was changed, false if not
	 * @throws DatasourceException
	 * @throws NodeIllegalArgumentException 
	 * @throws CMSUnavailableException 
	 */
	protected boolean updateFilesystemAttribute(
			GenticsContentObject originalObject,
			GenticsContentAttribute attribute) throws DatasourceException, CMSUnavailableException, NodeIllegalArgumentException {
		boolean changed = false;
		GenticsContentObject parent = attribute.getParent();
		String basePath = getAttributePath();

		if (ObjectTransformer.isEmpty(basePath)) {
			throw new DatasourceException(
					"Error while saving attribute '" + attribute.getAttributeName() + "' for object '" + parent + "' into filesystem: basepath is empty");
		}

		// get all existing values of the attribute
		GenticsContentAttribute originalAttribute = originalObject != null ? originalObject.getAttribute(attribute.getAttributeName()) : null;
		List<FilesystemAttributeValue> originalValues = null;

		if (originalAttribute != null) {
			originalValues = originalAttribute.getFSValues();
		} else {
			originalValues = Collections.emptyList();
		}

		List<FilesystemAttributeValue> fsValues = attribute.getFSValues();

		// iterate over the original values
		DBHandle dbHandle = getHandle().getDBHandle();

		int ovCounter = 0;

		for (; ovCounter < originalValues.size(); ++ovCounter) {
			if (ovCounter < fsValues.size()) {
				// update the given value
				originalValues.get(ovCounter).setData(fsValues.get(ovCounter).getData(), fsValues.get(ovCounter).getMD5(),
						fsValues.get(ovCounter).getLength());
				changed |= originalValues.get(ovCounter).saveData(dbHandle, basePath, attribute, attribute.isMultivalue() ? ovCounter + 1 : 0);
			} else {
				originalValues.get(ovCounter).deleteData(dbHandle, basePath, originalAttribute, attribute.isMultivalue() ? ovCounter + 1 : 0);
				changed = true;
			}
		}

		// insert new values
		while (ovCounter < fsValues.size()) {
			fsValues.get(ovCounter).saveData(dbHandle, basePath, attribute, attribute.isMultivalue() ? ovCounter + 1 : 0);
			ovCounter++;
			changed = true;
		}

		return changed;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#update(java.util.Collection, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo update(Collection objects, GenticsUser user) throws DatasourceException {
		return update(objects, user, false);
	}

	/**
	 * Udpate the collection of objects
	 * @param objects collection of objects to update
	 * @param user user making the update
	 * @param omitUpdateChecks true when update checks shall be omitted (will repair eventually missing versioning information)
	 * @return info about updated objects
	 * @throws DatasourceException
	 */
	public DatasourceInfo update(Collection objects, GenticsUser user, boolean omitUpdateChecks) throws DatasourceException {
		DatasourceRecordSet set = new CNDatasourceRecordSet(this);

		for (Iterator iter = objects.iterator(); iter.hasNext();) {
			Object element = iter.next();

			// convert the object into a CNDatasourceRow
			CNDatasourceRow row = getCNDatasourceRow(element);

			if (row != null) {
				// add the row
				set.addRow(row);
			} else {
				// could not add the row, so print a warning
				if (element == null) {
					logger.warn("Found null in the collection of objects to update, ignoring.");
				} else {
					logger.warn("Found object of class {" + element.getClass().getName() + "}, which cannot be updated. Ignoring.");
				}
			}
		}

		return update(set, user, omitUpdateChecks);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#update(com.gentics.api.lib.datasource.DatasourceRecordSet, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo update(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		return update(rst, user, false);
	}

	/**
	 * Update the objects from the given recordset
	 * @param rst recordset
	 * @param user user making the update
	 * @param omitUpdateChecks true to omit update check and write the data
	 *        (incl. versioning information) in any case
	 * @return info of updated objects
	 * @throws DatasourceException
	 */
	public DatasourceInfo update(DatasourceRecordSet rst, GenticsUser user, boolean omitUpdateChecks) throws DatasourceException {
		int updateCount = 0;
		CNDatasourceRecordSet updatedRecords = new CNDatasourceRecordSet(this);

		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_UPDATE);

			if (user == null && isVersioning() && logger.isWarnEnabled()) {
				logger.warn("Updating objects without user!");
			}

			validateExistingObjects(rst, true);

			try {
				DBHandle dbHandle = getHandle().getDBHandle();

				// prepare batch statements
				PreparedBatchStatement insert = new PreparedBatchStatement(
						BATCH_INSERT.replace("{tablename}", dbHandle.getContentAttributeName()), BATCH_INSERT_TYPES,
						BATCH_INSERT_COLUMNS);
				PreparedBatchStatement update = new PreparedBatchStatement(
						BATCH_UPDATE.replace("{tablename}", dbHandle.getContentAttributeName()), BATCH_UPDATE_TYPES,
						BATCH_UPDATE_COLUMNS);
				PreparedBatchStatement delete = new PreparedBatchStatement(
						BATCH_DELETE.replace("{tablename}", dbHandle.getContentAttributeName()), BATCH_DELETE_TYPES,
						BATCH_DELETE_COLUMNS);

				// TODO throw exceptions?!
				Iterator it = rst.iterator();

				if (rst.size() == 0) {
					return null;
				}

				// DatasourceRow row0 = rst.getRow(0);
				// GenticsContentObjectImpl object = (GenticsContentObjectImpl)
				// row0.toObject();
				// this.setAttributeNames(object.getAccessedAttributeNames());

				StringBuffer sql = new StringBuffer(500);
				String[] attr;
				ArrayList params = new ArrayList();
				HashMap map = new HashMap();

				int obj_id = 0;
				int obj_type = 0;
				Long timestamp = new Long((long) System.currentTimeMillis() / 1000);

				// attr = this.getAttributeNames();
				// if ( attr == null ) {
				// throw new IllegalStateException("No Attributes to update.");
				// }

				while (it.hasNext()) {
					DatasourceRow row = (DatasourceRow) it.next();

					if (row == null) {
						throw new DatasourceException("A Row is not a DatasourceRow Object.");
					}

					GenticsContentObject cnObj = (GenticsContentObject) row.toObject();

					if (cnObj == null) {
						continue;
					}

					// set the attribute names
					if (omitUpdateChecks) {
						// when update checks shall be omitted, get all set attributes
						setAttributeNames(cnObj.getAccessedAttributeNames(true));
					} else {
						// otherwise get only the modified attributes
						setAttributeNames(cnObj.getModifiedAttributeNames());
					}
					attr = this.getAttributeNames();

					// clear
					sql.delete(0, sql.length());
					params.clear();

					obj_id = cnObj.getObjectId();
					obj_type = cnObj.getObjectType();

					// Set Contentobject correctly
					String contentid = obj_type + "." + obj_id;

					cnObj.setObjectId(obj_id);

					// clear buffers
					map.clear();

					// get the original object
					GenticsContentObject originalObject = GenticsContentFactory.createContentObject(contentid, this); // TODO: use

					// objects
					// versiontimestamp
					// here?
					// prefill the original object with all attributes that may
					// be
					// changed
					try {
						// since the prefilled attributes are only direct
						// attributes, we eventually need to escaped the .
						// (dots) in attribute names
						String[] escapedAttr = null;

						if (attr != null) {
							escapedAttr = new String[attr.length];
							for (int i = 0; i < attr.length; i++) {
								escapedAttr[i] = attr[i].replaceAll("\\.", "\\\\\\.");
							}
						}
						GenticsContentFactory.prefillContentObjects(this, Collections.singletonList(originalObject), escapedAttr, -1, true); // TODO:
						// use
						// objects
						// versiontimestamp
						// here?
					} catch (Exception ex) {
						NodeLogger.getLogger(getClass()).warn("cannot prefill original object while updating", ex);
					}

					// check whether the object was really changed
					boolean objectChanged = false;

					// update attributes for content
					for (int i = 0; i < attr.length; i++) {
						String name = attr[i];
						boolean attributeChanged = false;
						String attstring = null;

						// get the current object attribute
						GenticsContentAttribute attribute = cnObj.getAttribute(name);

						if (attribute.isFilesystem()) {
							attributeChanged = updateFilesystemAttribute(originalObject, attribute);
						} else if (attribute.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
							attributeChanged = updateForeignLinkedObjects(originalObject, cnObj, attribute, user);
						} else if (attribute.isMultivalue()) {
							attributeChanged = updateMultivalueAttribute(originalObject, attribute);
						} else {
							if (isVersioning()) {
								attributeChanged = updateSinglevalueAttribute(originalObject, attribute);
							} else {
								attributeChanged = updateSinglevalueAttribute(originalObject, attribute, insert, update, delete);
							}
						}

						if (attributeChanged && getOptimizedColName(name) != null) {
							map.put(getOptimizedColName(name), row.getObject(name));
						}

						objectChanged |= attributeChanged;
						// create a version of the attribute (if datasource
						// supports
						// versioning)
						if (attribute.getAttributeType() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
							if (!cnObj.isCurrentVersion()) {
								// when this is not a current version, create
								// the version
								// event if not changed (it may be changed in
								// the timeline)
								createAttributeVersion(contentid, cnObj.getObjectType(), attribute.getAttributeName(), cnObj.getVersionTimestamp(),
										timestamp.intValue(), user != null ? user.getId() : "", true, cnObj.isFutureVersion());
							} else if (attributeChanged || omitUpdateChecks) {
								// for the current version, just create a
								// version when the
								// attribute really was changed (or update checks are disabled)
								createAttributeVersion(contentid, cnObj.getObjectType(), attribute.getAttributeName(), timestamp.intValue(),
										timestamp.intValue(), user != null ? user.getId() : "", false, false);
							}
						}
					}

					// remove protected attributes from the list of attributes
					// to update
					if (!allowSettingObjType) {
						map.remove("obj_type");
					}
					map.remove("contentid");
					map.remove("obj_id");
					map.remove("updatetimestamp");
					if (map.size() > 0) {
						// clear
						sql.delete(0, sql.length());
						params.clear();

						// insert content
						sql.append("UPDATE " + dbHandle.getContentMapName() + " SET ");

						Iterator oit = map.entrySet().iterator();

						// update previously prepared optimized values
						boolean first = true;

						while (oit.hasNext()) {
							if (!first) {
								sql.append(',');
							} else {
								first = false;
							}
							Map.Entry e = (Map.Entry) oit.next();

							sql.append(e.getKey());
							sql.append(" = ? ");
							addQuickColumnParam(params, e, dbHandle.getStringLengthManipulator());
						}
						//
						// // HM 2005-06-03: fix the upate for motherid,
						// // mother_obj_id,
						// // mother_obj_type
						// // update content
						//
						// // update contentid of mother object
						// if (cnObj.getMotherContentId() != null) {
						// if (!first) {
						// sql.append(',');
						// } else {
						// first = false;
						// }
						// sql.append("motherid = ? ");
						// params.add(cnObj.getMotherContentId());
						// }
						//
						// // update mother object id
						// if (!first) {
						// sql.append(',');
						// } else {
						// first = false;
						// }
						// sql.append("mother_obj_id = ?");
						// params.add(new Integer(cnObj.getMotherObjectId()));
						//
						// // update mother object type
						// if (!first) {
						// sql.append(',');
						// } else {
						// first = false;
						// }
						// sql.append("mother_obj_type = ?");
						// params.add(new Integer(cnObj.getMotherObjectType()));

						sql.append(" WHERE contentid = ?");
						params.add(obj_type + "." + obj_id);

						if (logger.isDebugEnabled()) {
							logger.debug(sql.toString());
						}

						DB.update(dbHandle, sql.toString(), params.toArray());
						objectChanged = true;
					}

					// check whether the motherid was changed
					boolean motherIdChanged = false;

					if (!StringUtils.isEqual(cnObj.getMotherContentId(), originalObject.getMotherContentId())) {
						motherIdChanged = true;
						objectChanged = true;
					}

					if (objectChanged) {
						updateCount++;
						// update the updatetimestamp only when something really
						// was
						// changed
						// clear
						sql.delete(0, sql.length());
						params.clear();

						sql.append(" UPDATE " + getHandle().getDBHandle().getContentMapName() + " SET ");
						sql.append(" updatetimestamp = ? ");
						if (motherIdChanged) {
							sql.append(", motherid = ?, mother_obj_type = ?, mother_obj_id = ?");
						}
						sql.append(" WHERE contentid = ? ");

						// when the object has a custom updatetimestamp set, we
						// use it, otherwise we set the current timestamp
						params.add(cnObj.getCustomUpdatetimestamp() != -1 ? new Long(cnObj.getCustomUpdatetimestamp()) : timestamp);
						if (motherIdChanged) {
							params.add(cnObj.getMotherContentId());
							params.add(new Integer(cnObj.getMotherObjectType()));
							params.add(new Integer(cnObj.getMotherObjectId()));
						}
						params.add(contentid); // contentid

						DB.update(getHandle().getDBHandle(), sql.toString(), params.toArray());

						// remove the object from the cache
						try {
							clearQueryCache();
							GenticsContentFactory.uncacheObject(this, cnObj);
						} catch (PortalCacheException e) {
							logger.warn("Error while removing object from cache", e);
						}

						updatedRecords.addRow(row);
					}

					// create a version (if datasource supports versioning)
					if (!cnObj.isCurrentVersion()) {
						createObjectVersion(contentid, cnObj.getObjectType(), cnObj.getVersionTimestamp(), timestamp.intValue(),
								user != null ? user.getId() : "", true, cnObj.isFutureVersion());
					} else {
						createObjectVersion(contentid, cnObj.getObjectType(), timestamp.intValue(), timestamp.intValue(), user != null ? user.getId() : "",
								false, false);
					}
				}

				// execute batched statements
				delete.execute(dbHandle);
				insert.execute(dbHandle);
				update.execute(dbHandle);

				return new DefaultDatasourceInfo(updatedRecords);
			} catch (Exception e) {
				throw new DatasourceException("update failed", e);
			}
		} finally {
			if (updateCount > 0) {
				setLastUpdateIfConfigured();
			}
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_CN_UPDATE);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#delete(java.util.Collection,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo delete(Collection objects, GenticsUser user) throws DatasourceException {
		DatasourceRecordSet set = new CNDatasourceRecordSet(this);

		for (Iterator iter = objects.iterator(); iter.hasNext();) {
			Object element = iter.next();

			// convert the object into a CNDatasourceRow
			CNDatasourceRow row = getCNDatasourceRow(element);

			if (row != null) {
				// add the row
				set.addRow(row);
			} else {
				// could not add the row, so print a warning
				if (element == null) {
					logger.warn("Found null in the collection of objects to delete, ignoring.");
				} else {
					logger.warn("Found object of class {" + element.getClass().getName() + "}, which cannot be deleted. Ignoring.");
				}
			}
		}
		return delete(set, user);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#delete(com.gentics.lib.datasource.DatasourceRecordSet,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo delete(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {

		int deleted = 0;
        
		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_DELETE);

			if (user == null && isVersioning() && logger.isWarnEnabled()) {
				logger.warn("Deleting objects without user!");
			}
			try {
				if (rst == null) {
					return new DefaultDatasourceInfo(null);
				}

				String sqlcm = "DELETE FROM " + getHandle().getDBHandle().getContentMapName() + " WHERE contentid = ?"; 
				String sqlca = "DELETE FROM " + getHandle().getDBHandle().getContentAttributeName() + " where contentid = ?";
				Iterator it = rst.iterator();
				String contentid;
				ArrayList params = new ArrayList();

				int timestamp = (int) (System.currentTimeMillis() / 1000);

				while (it.hasNext()) {
					DatasourceRow row = (DatasourceRow) it.next();

					// reset parameters for each datasourcerow RRE 2004.10.27
					params = new ArrayList();
					if (row != null) {
						GenticsContentObject cnObj = (GenticsContentObject) row.toObject();

						if (cnObj == null) {
							continue;
						}

						// TODO successfull delete check - see below and update
						// @
						// .insert() as well
						contentid = cnObj.getContentId();
						if (contentid != null) {
							params.add(contentid);
							int status = DB.update(getHandle().getDBHandle(), sqlcm, params.toArray(), null);

							// add number of affected objects
							deleted += status;
                            
							int statusca = DB.update(getHandle().getDBHandle(), sqlca, params.toArray(), null);

							// if no object deleted but some leftover attributes found, count this as well.
							if (status == 0 && statusca > 0) {
								deleted += 1;
							}

							// remove all value files
							File[] valueFiles = GenticsContentFactory.getValueFiles(getAttributePath(), contentid);

							for (File file : valueFiles) {
								DB.removeFileOnCommit(getHandle().getDBHandle(), file);
							}
						}

						// create a version (if datasource supports versioning)
						// TODO: how to support future deletions?
						createFullVersion(contentid, cnObj.getObjectType(), timestamp, timestamp, user != null ? user.getId() : "", false,
								cnObj.isFutureVersion());

						// remove the object from the cache
						try {
							clearQueryCache();
							GenticsContentFactory.uncacheObject(this, cnObj);
						} catch (PortalCacheException e) {
							logger.warn("Error while removing object from cache", e);
						}
					}
				}
			} catch (Exception e) {
				throw new DatasourceException("delete failed", e);
			}
		} finally {
			if (deleted > 0) {
				setLastUpdateIfConfigured();
			}
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_CN_DELETE);
		}
        
		return new NumDatasourceInfo(deleted);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#insert(java.util.Collection,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo insert(Collection objects, GenticsUser user) throws DatasourceException {
		DatasourceRecordSet set = new CNDatasourceRecordSet(this);

		for (Iterator iter = objects.iterator(); iter.hasNext();) {
			Object element = iter.next();

			// convert the object into a CNDatasourceRow
			CNDatasourceRow row = getCNDatasourceRow(element);

			if (row != null) {
				// add the row
				set.addRow(row);
			} else {
				// could not add the row, so print a warning
				if (element == null) {
					logger.warn("Found null in the collection of objects to insert, ignoring.");
				} else {
					logger.warn("Found object of class {" + element.getClass().getName() + "}, which cannot be inserted. Ignoring.");
				}
			}
		}

		return insert(set, user);
	}
    
	/**
	 * Validates that all objects in datasource record set either exists, or not (depending on 'existing')
	 * @param rst the datasource record set to verify
	 * @param existing true if all objects have to exist, false if all object must not exist.
	 * @throws DatasourceModificationException if any object's exists() method returns the wrong value.
	 */
	private void validateExistingObjects(DatasourceRecordSet rst, boolean existing) throws DatasourceModificationException, DatasourceException {
		for (Iterator i = rst.iterator(); i.hasNext();) {
			DatasourceRow row = (DatasourceRow) i.next();

			if (row == null) {
				// None of our business
				continue;
			}
			GenticsContentObject cnObj = (GenticsContentObject) row.toObject();

			if (cnObj == null) {
				continue;
			}
			if (checkExistence(cnObj) != existing) {
				throw new DatasourceModificationException(row, "A given Content Object does " + (existing ? "NOT " : "") + " exist.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#insert(com.gentics.lib.datasource.DatasourceRecordSet,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo insert(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		DefaultDatasourceInfo info = new DefaultDatasourceInfo();

		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_INSERT);

			if (user == null && isVersioning() && logger.isWarnEnabled()) {
				logger.warn("Inserting objects without user!");
			}
            
			validateExistingObjects(rst, false);


			try {
				if (rst == null) {
					return null;
				}
				DBHandle dbHandle = getHandle().getDBHandle();

				StringBuffer sql = new StringBuffer(500);
				// create the info object
				CNDatasourceRecordSet infoRecordSet = new CNDatasourceRecordSet(this);

				info.setDatasourceRecordSet(infoRecordSet);

				Iterator it = rst.iterator();

				if (it == null) {
					return null;
				}

				String[] attr;
				ArrayList params = new ArrayList();
				Map<String, Object> insertData = new HashMap<>();
				HashMap map = new HashMap();

				int obj_id = 0;
				int obj_type = 0;
				Long timestamp = new Long((long) System.currentTimeMillis() / 1000);

				attr = this.getAttributeNames();
				// if ( attr == null ) {
				// throw new IllegalStateException("No Attributes to update.");
				// }

				// we collect all objtypes for which objects were inserted with
				// given contentid (for later repairing the id_counters)
				List objTypesInsertedWithContentid = new Vector();

				// add obj_type, contentid to the adttribute list
				while (it.hasNext()) {
					PreparedBatchStatement insert = new PreparedBatchStatement(
							BATCH_INSERT.replace("{tablename}", dbHandle.getContentAttributeName()), BATCH_INSERT_TYPES,
							BATCH_INSERT_COLUMNS);

					DatasourceRow row = (DatasourceRow) it.next();

					if (row == null) {
						throw new DatasourceException("A Row is not a DatasourceRow Object.");
					}

					GenticsContentObject cnObj = (GenticsContentObject) row.toObject();

					if (cnObj == null) {
						continue;
					}

					attr = cnObj.getModifiedAttributeNames();

					// clear
					sql.delete(0, sql.length());
					params.clear();

					// insert content
					sql.append(
							"INSERT INTO " + dbHandle.getContentMapName() + " (contentid, obj_id, obj_type,"
							+ " motherid, mother_obj_id, mother_obj_type, updatetimestamp" + ") VALUES (?, ?, ?, ?, ?, ?, ?)");

					obj_id = cnObj.getObjectId();
					obj_type = cnObj.getObjectType();

					// TODO generate id if not available
					if (obj_id == 0) {
						obj_id = generateId(obj_type);
					} else {
						Integer objTypeO = new Integer(obj_type);

						if (!objTypesInsertedWithContentid.contains(objTypeO)) {
							objTypesInsertedWithContentid.add(objTypeO);
						}
					}

					// Set Contentobject correctly
					String contentid = obj_type + "." + obj_id;

					cnObj.setObjectId(obj_id);

					// TODO check if type is set
					params.add(contentid); // contentid
					params.add(new Integer(obj_id)); // obj_id
					params.add(new Integer(obj_type)); // obj_type
					params.add(cnObj.getMotherContentId());
					params.add(new Integer(cnObj.getMotherObjectId()));
					params.add(new Integer(cnObj.getMotherObjectType()));
					// when the object has a custom updatetimestamp set, we use
					// it, otherwise we set the current timestamp
					params.add(cnObj.getCustomUpdatetimestamp() != -1 ? new Long(cnObj.getCustomUpdatetimestamp()) : timestamp);

					if (logger.isDebugEnabled()) {
						logger.debug(sql.toString());
					}

					DB.update(getHandle().getDBHandle(), sql.toString(), params.toArray());

					// insert the recordset into the info object
					infoRecordSet.addRow(new CNDatasourceRow(cnObj));

					// clear buffers
					map.clear();

					// insert attributes for content
					for (int i = 0; i < attr.length; i++) {
						String name = attr[i];

						// get the current object attribute
						GenticsContentAttribute attribute = cnObj.getAttribute(name);

						attribute.resetIterator();

						if (attribute.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
							// TODO implement storing of foreign objects here
							Object value = cnObj.get(attribute.getAttributeName());

							if (value instanceof Collection) {
								Collection valueCol = (Collection) value;

								for (Iterator iter = valueCol.iterator(); iter.hasNext();) {
									Object element = (Object) iter.next();

									if (element instanceof GenticsContentObject) {
										GenticsContentObject foreignObject = (GenticsContentObject) element;

										// ensure the objects are correctly
										// linked
										foreignObject.setProperty(attribute.getForeignLinkAttribute(), cnObj.getContentId());
										foreignObject.setVersionTimestamp(cnObj.getVersionTimestamp());
									}
								}

								// store the foreign linked objects
								store(valueCol, user);
							}
							continue;
						} else if (attribute.isFilesystem()) {
							insertFilesystemAttribute(attribute);
						} else if (attribute.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_BLOB) {
							byte[] binaryData = null;
							int sortorder = 1;

							while ((binaryData = attribute.getNextBinaryValue()) != null) {
								if (isVersioning()) {
									// Clear buffers
									sql.delete(0, sql.length());
									params.clear();

									sql.append("INSERT INTO " + getHandle().getDBHandle().getContentAttributeName() + " (contentid, name, ");
									if (attribute.isMultivalue()) {
										// sortorder is only stored for multivalue
										// attributes
										sql.append("sortorder, ");
									}
									sql.append(getDataTypeColumn(name));
									sql.append(") VALUES (?, ?, ").append(attribute.isMultivalue() ? "?, " : "").append("?)");
									params.add(cnObj.getContentId()); // contentid
									params.add(name); // name
									if (attribute.isMultivalue()) {
										// sortorder is only stored for multivalue
										// attributes
										params.add(new Integer(sortorder++)); // sortorder
									}
									params.add(binaryData); // value
									// params.add( new
									// String(ObjectTransformer.getString(cnObj.getAttribute(
									// name ),""))); // value

									// prepare optimized parameters
									// optimizedParams.add( getOptimizedColName(
									// name ) );
									if (getOptimizedColName(name) != null) {
										map.put(getOptimizedColName(name), row.getObject(name));
									}

									if (logger.isDebugEnabled()) {
										logger.debug(sql.toString());
									}

									DB.update(getHandle().getDBHandle(), sql.toString(), params.toArray());
								} else {
									insertData.clear();
									insertData.put("contentid", cnObj.getContentId());
									insertData.put("name", name);
									insertData.put(getDataTypeColumn(name), binaryData);

									if (attribute.isMultivalue()) {
										// sortorder is only stored for multivalue
										// attributes
										insertData.put("sortorder", sortorder++);
									}

									if (getOptimizedColName(name) != null) {
										map.put(getOptimizedColName(name), row.getObject(name));
									}

									insert.add(insertData);
								}
							}
						} else {
							Object attValue = null;
							int sortorder = 1;

							while ((attValue = attribute.getNextObjectValue()) != null) {
								if (isVersioning()) {
									// Clear buffers
									sql.delete(0, sql.length());
									params.clear();

									sql.append("INSERT INTO " + getHandle().getDBHandle().getContentAttributeName() + " (contentid, name, ");
									if (attribute.isMultivalue()) {
										// sortorder is only stored for multivalue
										// attributes
										sql.append("sortorder, ");
									}
									sql.append(getDataTypeColumn(name));
									sql.append(") VALUES (?, ?, ").append(attribute.isMultivalue() ? "?, " : "").append("?)");
									params.add(cnObj.getContentId()); // contentid
									params.add(name); // name
									if (attribute.isMultivalue()) {
										params.add(new Integer(sortorder++)); // sortorder
									}
	                                
									// truncate the text if needed
									if (attribute.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_TEXT) {
										// attValue = truncateAttributeValue(attribute, dbHandle.getStringLengthManipulator());
										attValue = truncateAttributeValue(ObjectTransformer.getString(attValue, null), contentid, name,
												dbHandle.getStringLengthManipulator());
									}
	                                
									params.add(attValue); // value
									// params.add( new
									// String(ObjectTransformer.getString(cnObj.getAttribute(
									// name ),""))); // value

									// prepare optimized parameters
									// optimizedParams.add( getOptimizedColName(
									// name ) );
									if (getOptimizedColName(name) != null) {
										map.put(getOptimizedColName(name), row.getObject(name));
									}

									if (logger.isDebugEnabled()) {
										logger.debug(sql.toString());
									}

									try {
										DB.update(dbHandle, sql.toString(), params.toArray());
									} catch (Exception e) {
										throw new DatasourceException(
												"Error while inserting attribute {" + name + "} for object {" + cnObj.getContentId() + "} with value {" + attValue + "}",
												e);
									}
								} else {
									insertData.clear();
									insertData.put("contentid", cnObj.getContentId());
									insertData.put("name", name);
									if (attribute.isMultivalue()) {
										insertData.put("sortorder", sortorder++);
									}

									// truncate the text if needed
									if (attribute.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_TEXT) {
										// attValue = truncateAttributeValue(attribute, dbHandle.getStringLengthManipulator());
										attValue = truncateAttributeValue(ObjectTransformer.getString(attValue, null), contentid, name,
												dbHandle.getStringLengthManipulator());
									}
									insertData.put(getDataTypeColumn(name), attValue);

									if (getOptimizedColName(name) != null) {
										map.put(getOptimizedColName(name), row.getObject(name));
									}

									insert.add(insertData);
								}
							}
						}
					}

					if (map.size() > 0) {
						// clear
						sql.delete(0, sql.length());
						params.clear();

						// insert content
						sql.append("UPDATE " + dbHandle.getContentMapName() + " SET ");

						Iterator oit = map.entrySet().iterator();

						// update previously prepared optimized values
						boolean first = true;

						while (oit.hasNext()) {
							if (!first) {
								sql.append(',');
							} else {
								first = false;
							}
							Map.Entry e = (Map.Entry) oit.next();

							sql.append(e.getKey());
							sql.append(" = ? ");
							addQuickColumnParam(params, e, dbHandle.getStringLengthManipulator());
						}

						sql.append(" WHERE contentid = ?");
						params.add(obj_type + "." + obj_id);

						if (logger.isDebugEnabled()) {
							logger.debug(sql.toString());
						}

						DB.update(dbHandle, sql.toString(), params.toArray());
					}

					// create a version (if datasource supports versioning)
					if (!cnObj.isCurrentVersion()) {
						createFullVersion(contentid, cnObj.getObjectType(), cnObj.getVersionTimestamp(), timestamp.intValue(), user != null ? user.getId() : "",
								true, cnObj.isFutureVersion());
					} else {
						createFullVersion(contentid, cnObj.getObjectType(), timestamp.intValue(), timestamp.intValue(), user != null ? user.getId() : "", false,
								false);
					}

					insert.execute(dbHandle);
				}

				// when at least one record was inserted, clear the query cache
				if (info.getAffectedRecordCount() > 0) {
					if (repairIDCounterOnInsert && !objTypesInsertedWithContentid.isEmpty()) {
						repairIdCounters(objTypesInsertedWithContentid);
					}

					clearQueryCache();
				}

				return info;
			} catch (DatasourceException e) {
				throw e;
			} catch (Exception e) {
				throw new DatasourceException("insert failed", e);
			}
		} finally {
			if (info.getAffectedRecordCount() > 0) {
				setLastUpdateIfConfigured();
			}
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_CN_INSERT);
		}
	}

	private void addQuickColumnParam(ArrayList params, Map.Entry e, StringLengthManipulator manipulator) throws CMSUnavailableException {
		Object val = e.getValue();

		if (val == null) {
			params.add(null);
		} else if (!(val instanceof GenticsContentAttribute)) {
			// This can never happen imho
			logger.fatal("Value is no content attribute. {" + val.getClass().getName() + "}");
			params.add(null);
		} else {
			GenticsContentAttribute contentattr = (GenticsContentAttribute) val;

			if (contentattr.isMultivalue()) {
				logger.error("Attribute is multivalue but marked as optimized !! {" + contentattr.getAttributeName() + "}");
				params.add(null);
			} else {
				// now check if we have a text(short) attr. and truncate if needed.
				if (contentattr.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_TEXT) {
					String truncated = truncateAttributeValue(contentattr.getNextValue(), contentattr.getParent().getContentId(), contentattr.getAttributeName(),
							manipulator);

					params.add(truncated);
				} else {
					params.add(contentattr.getNextObjectValue());
				}
			}
		}
	}
    
	/**
	 * Overloaded version of truncateAttributeValue for you convinience.
	 * @param attribute Attribute which should be truncated. Note that if this is a multivalue attribute you will only get the truncated result of the next value.
	 * @param manipulator The StringLengthManipulator which is used to determine the length of the string and truncates the value.
	 * @return The value truncated to 255 (according to the length by the specified manipulator).
	 */
	private String truncateAttributeValue(GenticsContentAttribute attribute,
			StringLengthManipulator manipulator) {
		String value;

		try {
			value = attribute.getNextValue();
		} catch (CMSUnavailableException e) {
			return null;
		}
		return truncateAttributeValue(value, attribute.getParent().getContentId(), attribute.getAttributeName(), manipulator);

	}
    
	/**
	 * Truncate the given value with the specified manipulator and produce a logerror.
	 * @param value The value that should be truncated to fit in the database.
	 * @param contentId The contentid of the parent object. This is used just for logging information. Can be null, however this is strongly discouraged since we want to produce usefull feedback messages to the use. 
	 * @param attributName The name of the attribute. Same rules apply as to the argument contentId.
	 * @param manipulator The StringLengthManipulator which is used to determine the length of the string and truncates the value.
	 * @return The value truncated to 255 (according to the length by the specified manipulator).
	 */
	private String truncateAttributeValue(String value, String contentId, String attributName, StringLengthManipulator manipulator) {
		if (value != null) {
			if (manipulator.getLength(value) > 255) {
				logger.error(
						"The value of the attribute {" + attributName + "} in the object {" + contentId
						+ "} is too long for the specified datatype and will be truncated.");
				return manipulator.truncate(value, 255);
			}
			return value;
		}
		return null;
	}
    
	private int generateId(int objType) throws SQLException, CMSUnavailableException {
		// TODO: change implementation to NOT reuse content ids (using counter
		// table), make creation of contentids threadsafe
		if (objType == 0) {
			throw new SQLException("generateId: objType may not be 0");
		}

		SimpleResultProcessor rs = new SimpleResultProcessor();

		// first check for the id_counter in contentobject
		// TODO: do not make the check on every method invocation but store the
		// info somewhere
		if (DB.fieldExists(getHandle().getDBHandle(), getHandle().getDBHandle().getContentObjectName(), "id_counter")) {
			// encapsulate increasing counter and getting new value into a
			// synchronized block
			synchronized (CNWriteableDatasource.class) {
				// update the id_counter and afterwards select the new value. do
				// this in a transaction to overcome synchronisation problems
				String updateSQL = "UPDATE " + getHandle().getDBHandle().getContentObjectName() + " set id_counter = id_counter + 1 WHERE type = ?";
				String selectSQL = "SELECT id_counter FROM " + getHandle().getDBHandle().getContentObjectName() + " WHERE type = ?";
				Object[] params = new Object[] { new Integer(objType)};

				// do the update and select in a transaction
				DB.queryBatch(getHandle().getDBHandle(), new String[] { updateSQL, selectSQL}, new Object[][] { params, params}, rs, null, true, true);

				if (rs == null) {
					return 0;
				}
				SimpleResultRow row = rs.getRow(1);

				if (row == null) {
					return 0;
				}

				return row.getInt("id_counter");
			}

		} else {
			logger.error("Caution: This datasource does not use id_counter for generation of obj_id's. " + "It is possible that contentid's are not unique!");
			DB.query(getHandle().getDBHandle(), "SELECT max(obj_id) maxid FROM " + getHandle().getDBHandle().getContentMapName() + " WHERE obj_type = ?",
					new Integer(objType), rs);
			if (rs == null) {
				return 0;
			}

			Iterator it = rs.iterator();

			if (it.hasNext()) {
				SimpleResultRow row = (SimpleResultRow) it.next();

				if (row == null) {
					return 0;
				}

				int newId = row.getInt("maxid") + 1;

				return newId;
			}
		}

		return 0;
	}

	/**
	 * create a version for the object with the given contentid
	 * @param contentId contentid of the object to version
	 * @param objectType objecttype
	 * @param timestamp of the version
	 * @param currentTimestamp current timestamp (different from timestamp when
	 *        a future or past version is created)
	 * @param userId id of the user who creates the version
	 * @param futureOrPastChange true if the version is a future or past change,
	 *        false for a current change
	 * @param autoupdate true when the autoupdate flag shall be set for future
	 *        changes
	 * @return true when an object has been versioned, false if not
	 */
	private boolean createObjectVersion(String contentId, int objectType, int timestamp, int currentTimestamp,
			String userId, boolean futureOrPastChange, boolean autoupdate) {
		if (isVersioning()) {
			// when the tableversion objects are not yet created, do this now
			boolean versionCreated = false;

			try {
				initTableVersioning();

				// check whether the objecttype is excluded from versioning and create a unique version if yes
				if (DatatypeHelper.isObjecttypeExcludeVersioning(getHandle().getDBHandle(), objectType)) {
					contentMapVersion.createUniqueVersion(new Object[] { contentId}, userId);
				} else {
					versionCreated |= contentMapVersion.createVersion(new Object[] { contentId }, timestamp, userId, futureOrPastChange, autoupdate);
					// versionCreated |=
					// contentAttributeVersion.createVersion(contentid, timestamp,
					// userId, futureOrPastChange);

					// if this is a futureOrPastChange, restore the current version
					if (futureOrPastChange) {
						contentMapVersion.restoreVersion(contentId, currentTimestamp);
						// contentAttributeVersion.restoreVersion(contentid,
						// currentTimestamp);
					}
				}
			} catch (Exception ex) {
				logger.error("Error while creating object version", ex);
			}

			return versionCreated;
		} else {
			return false;
		}
	}

	/**
	 * create a version for the object and attributes with the given contentid
	 * @param contentId contentid of the object to version
	 * @param objectType object type
	 * @param timestamp of the version
	 * @param currentTimestamp current timestamp (different from timestamp when
	 *        a future or past version is created)
	 * @param userId id of the user who creates the version
	 * @param futureOrPastChange true if the version is a future or past change,
	 *        false for a current change
	 * @param autoupdate true when the autoupdate flag shall be set for future
	 *        changes
	 * @return true when an object has been versioned, false if not
	 */
	private boolean createFullVersion(String contentId, int objectType, int timestamp, int currentTimestamp,
			String userId, boolean futureOrPastChange, boolean autoupdate) {
		if (isVersioning()) {
			// when the tableversion objects are not yet created, do this now
			boolean versionCreated = false;

			try {
				initTableVersioning();

				if (DatatypeHelper.isObjecttypeExcludeVersioning(getHandle().getDBHandle(), objectType)) {
					contentMapVersion.createUniqueVersion(new Object[] { contentId}, userId);
					allContentAttributeVersion.createUniqueVersion(new Object[] { contentId}, userId);
				} else {
					versionCreated |= contentMapVersion.createVersion(new Object[] { contentId}, timestamp, userId, futureOrPastChange, autoupdate);
					versionCreated |= allContentAttributeVersion.createVersion(new Object[] { contentId}, timestamp, userId, futureOrPastChange, autoupdate);

					// find all attributes that are excluded from versioning and
					// create unique versions for those
					String[] excludedAttributes = DatatypeHelper.getNonVersioningAttributes(getHandle().getDBHandle(), objectType);

					for (int i = 0; i < excludedAttributes.length; ++i) {
						contentAttributeVersion.createUniqueVersion(new Object[] { contentId, excludedAttributes[i]}, userId);
					}

					// if this is a futureOrPastChange, restore the current
					// version
					if (futureOrPastChange) {
						contentMapVersion.restoreVersion(contentId, currentTimestamp);
						allContentAttributeVersion.restoreVersion(contentId, currentTimestamp);
					}
				}
			} catch (Exception ex) {
				logger.error("Error while creating full version", ex);
			}

			return versionCreated;
		} else {
			return false;
		}
	}

	/**
	 * create a version of the given attribute for a contentobject
	 * @param contentId contentid of the object
	 * @param objectType object type
	 * @param attributeName name of the attribute to version
	 * @param timestamp of the version
	 * @param currentTimestamp current timestamp (different from timestamp when
	 *        a future or past version is created)
	 * @param userId id of the user who creates the version
	 * @param futureOrPastChange true if the version is a future or past change,
	 *        false for a current change
	 * @param autoupdate true when the autoupdate flag shall be set for future
	 *        changes
	 * @return true when the attribute has been versioned, false if not
	 */
	private boolean createAttributeVersion(String contentId, int objectType, String attributeName,
			int timestamp, int currentTimestamp, String userId, boolean futureOrPastChange,
			boolean autoupdate) {
		if (isVersioning()) {
			// when the tableversion objects are not yet created, do this now

			Object[] idParams = new Object[] { contentId, attributeName };

			boolean versionCreated = false;

			try {
				initTableVersioning();

				// check whether the attribute is excluded from versioning
				if (DatatypeHelper.isAttributeExcludeVersioning(getHandle().getDBHandle(), objectType, attributeName)
						|| DatatypeHelper.isObjecttypeExcludeVersioning(getHandle().getDBHandle(), objectType)) {
					contentAttributeVersion.createUniqueVersion(idParams, userId);
				} else {
					versionCreated |= contentAttributeVersion.createVersion(idParams, timestamp, userId, futureOrPastChange, autoupdate);

					// if this is a futureOrPastChange, restore the current version
					if (futureOrPastChange) {
						contentAttributeVersion.restoreVersion(idParams, currentTimestamp);
					}
				}
			} catch (Exception ex) {
				logger.error("Error while creating attribute version", ex);
			}

			return versionCreated;
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableVersioningDatasource#updateDueFutureChanges()
	 */
	public boolean updateDueFutureChanges() {
		logger.info("updateDueFutureChanges()");
		if (isVersioning()) {
			try {
				initTableVersioning();
				Integer timestamp = new Integer((int) (System.currentTimeMillis() / 1000));
				SimpleResultProcessor result = new SimpleResultProcessor();

				// update the contentattributes
				DB.query(getHandle().getDBHandle(),
						"SELECT contentid FROM " + getHandle().getDBHandle().getContentAttributeName()
						+ "_nodeversion WHERE nodeversiontimestamp <= ? AND nodeversion_autoupdate = ?",
						new Object[] { timestamp, Boolean.TRUE },
						result);
				for (Iterator iter = result.iterator(); iter.hasNext();) {
					SimpleResultRow row = (SimpleResultRow) iter.next();

					allContentAttributeVersion.restoreVersion(row.getString("contentid"), timestamp.intValue());
				}
				// reset all autoupdate-flags for old versions
				DB.update(getHandle().getDBHandle(),
						"UPDATE " + getHandle().getDBHandle().getContentAttributeName()
						+ "_nodeversion SET nodeversion_autoupdate = ? WHERE nodeversiontimestamp <= ? AND nodeversion_autoupdate = ?",
						new Object[] { Boolean.FALSE, timestamp, Boolean.TRUE },
						null);

				// update the contentmap
				DB.query(getHandle().getDBHandle(),
						"SELECT contentid FROM " + getHandle().getDBHandle().getContentMapName()
						+ "_nodeversion WHERE nodeversiontimestamp <= ? AND nodeversion_autoupdate = ?",
						new Object[] { timestamp, Boolean.TRUE },
						result);
				for (Iterator iter = result.iterator(); iter.hasNext();) {
					SimpleResultRow row = (SimpleResultRow) iter.next();

					contentMapVersion.restoreVersion(row.getString("contentid"), timestamp.intValue());
				}
				// reset all autoupdate-flags for old versions
				DB.update(getHandle().getDBHandle(),
						"UPDATE " + getHandle().getDBHandle().getContentMapName()
						+ "_nodeversion SET nodeversion_autoupdate = ? WHERE nodeversiontimestamp <= ? AND nodeversion_autoupdate = ?",
						new Object[] { Boolean.FALSE, timestamp, Boolean.TRUE },
						null);
			} catch (Exception ex) {
				logger.error("error while updating future changes", ex);
			}
			return false;
		} else {
			return false;
		}
	}

	/**
	 * Internal method to clone a GenticsContentObject (which was loaded from another datasource)
	 * This method only clones accessed attributes. Make sure all attributes you want to
	 *   clone are accessed before calling this method.
	 * @param object object to clone
	 * @return cloned object
	 */
	protected GenticsContentObject cloneObject(GenticsContentObject object) throws DatasourceException {
		try {
			if (object.exists()) {
				GenticsContentObject clonedObject = GenticsContentFactory.createContentObject(object.getContentId(), this, object.getVersionTimestamp(), false);

				// this is important to initialize the object
				clonedObject.exists();
				// only the accessed attributes names are cloned to avoid prefetching of LOBs in the CRSync.
				// the prefetching code will skip LOBs, and so, if all attributes are prefetched, all attributes
				// except for LOBs will have been accessed.
				String[] attributeNames = object.getAccessedAttributeNames(false);

				for (int i = 0; i < attributeNames.length; i++) {
					String name = attributeNames[i];

					// when the attribute is streamable for both objects, we just set the streams
					if (clonedObject.isStreamable(name) && object.isStreamable(name)) {
						CNDatasource sourceDS = (CNDatasource) object.getDatasource();
						String basePath = sourceDS.getAttributePath();
						List<FilesystemAttributeValue> oldValues = object.getAttribute(name).getFSValues();
						List<FilesystemAttributeValue> newValues = new Vector<FilesystemAttributeValue>(oldValues.size());

						for (FilesystemAttributeValue value : oldValues) {
							if (!ObjectTransformer.isEmpty(value.getStoragePath())) {
								FilesystemAttributeValue data = new FilesystemAttributeValue();

								data.setData(new File(basePath, value.getStoragePath()), value.getMD5(), value.getLength());
								newValues.add(data);
							} else {
								newValues.add(null);
							}
						}
						if (newValues.size() == 0) {
							clonedObject.setProperty(name, null);
						} else if (newValues.size() == 1) {
							clonedObject.setProperty(name, newValues.get(0));
						} else {
							clonedObject.setProperty(name, newValues);
						}
					} else {
						clonedObject.setProperty(name, object.get(name));
					}
				}
				// also update the mother object
				clonedObject.setMotherContentId(object.getMotherContentId());
				// ... and the custom updatetimestamp (if set)
				if (object.getCustomUpdatetimestamp() != -1) {
					clonedObject.setCustomUpdatetimestamp(object.getCustomUpdatetimestamp());
				}
				return clonedObject;
			} else {
				// TODO should we clone not existent objects?
				return object;
			}
		} catch (Exception e) {
			throw new DatasourceException("Error while cloning object", e);
		}
	}

	/**
	 * Version of {@link #create(Map, int)} with option to check existance
	 * @param objectParameters map of object parameters
	 * @param versionTimestamp versiontimestamp (-1 for current version)
	 * @param checkExistance true when existance of object shall be checked, false if not
	 * @return object
	 * @throws DatasourceException
	 */
	public Changeable create(Map objectParameters, int versionTimestamp, boolean checkExistance) throws DatasourceException {
		// first check whether the objectparameters contain at least the objecttype
		if (objectParameters == null || (!objectParameters.containsKey("obj_type") && !objectParameters.containsKey("contentid"))) {
			throw new DatasourceException("Cannot create object with neither obj_type nor contentid");
		}

		int objecttype = ObjectTransformer.getInt(objectParameters.get("obj_type"), -1);
		String contentid = ObjectTransformer.getString(objectParameters.get("contentid"), null);

		// the objecttype must be a number
		try {

			GenticsContentObject object = null;

			if (!StringUtils.isEmpty(contentid) && !GenticsContentFactory.isTemporary(contentid)) {
				object = GenticsContentFactory.createContentObject(contentid, this, versionTimestamp, checkExistance);
			} else {
				if (!StringUtils.isEmpty(contentid)) {
					object = GenticsContentFactory.createContentObject(objecttype, this, versionTimestamp, contentid);
				} else {
					object = GenticsContentFactory.createContentObject(objecttype, this, versionTimestamp, null);
				}
			}
			// create the object
			// set additional attributes to the object
			if (object != null) {
				for (Iterator iter = objectParameters.entrySet().iterator(); iter.hasNext();) {
					Map.Entry element = (Map.Entry) iter.next();

					if (PROTECTED_ATTRIBUTES.contains(element.getKey())) {
						// do not write protected attributes
						continue;
					}
					object.setAttribute(element.getKey().toString(), element.getValue());
				}
			}
			return object;
		} catch (Exception ex) {
			throw new DatasourceException("Error while creating object with obj_type {" + objecttype + "} and contentid {" + contentid + "}", ex);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.VersioningDatasource#create(java.util.Map, int)
	 */
	public Changeable create(Map objectParameters, int versionTimestamp) throws DatasourceException {
		return create(objectParameters, versionTimestamp, true);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#create(java.util.Map)
	 */
	public Changeable create(Map objectParameters) throws DatasourceException {
		return create(objectParameters, -1);
	}

	/**
	 * Set the last update timestamp (if the contentstatus table exists)
	 * @param timestamp timestamp
	 * @return true when the timestamp was successfully set, false if no contentstatus table exists
	 * @throws DatasourceException when an error occurred
	 */
	public boolean setLastUpdate(long timestamp) throws DatasourceException {
		try {
			DBHandle handle = getHandle().getDBHandle();
			if (DB.tableExists(handle, handle.getContentStatusName())) {
				setContentStatus("lastupdate", (int) timestamp);
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			throw new DatasourceException("Error while setting last update timestamp", e);
		}
	}

	/**
	 * Set the last update timestamp to the current time
	 * @return true when the timestamp was successfully set, false if no contentstatus table exists
	 * @throws DatasourceException when an error occurred
	 */
	public boolean setLastUpdate() throws DatasourceException {
		return setLastUpdate(System.currentTimeMillis() / 1000);
	}
    
	/**
	 * Returns true if it is allowed to write the attribute obj_type - VERY VERY hackish, don't use !
	 */
	public boolean getAllowSettingObjType() {
		return allowSettingObjType;
	}

	/**
	 * Set the updatetimestamp (after insert/update/delete), if configured
	 * @return true when the updatetimestamp was set, false if not
	 * @throws DatasourceException
	 */
	protected boolean setLastUpdateIfConfigured() throws DatasourceException {
		if (setUpdatetimestampOnWrite) {
			return setLastUpdate();
		} else {
			return false;
		}
	}

	/**
	 * Repair the id counter for the given list of objtypes (or for all
	 * objtypes, if list is empty or null)
	 * @param objTypesToRepair list of objtypes to repair, may be empty or null
	 *        for ALL objtypes
	 * @throws SQLException
	 */
	public void repairIdCounters(List objTypesToRepair) throws SQLException, CMSUnavailableException {
		DBHandle dbHandle = getHandle().getDBHandle();

		if (DB.fieldExists(dbHandle, dbHandle.getContentObjectName(), "id_counter")) {
			String repairStatement = "UPDATE " + dbHandle.getContentObjectName() + " SET id_counter = (SELECT max(obj_id) FROM " + dbHandle.getContentMapName()
					+ " WHERE type = obj_type) WHERE type in (SELECT distinct obj_type FROM " + dbHandle.getContentMapName() + ")";

			if (!ObjectTransformer.isEmpty(objTypesToRepair)) {
				repairStatement += " AND type in (" + StringUtils.repeat("?", objTypesToRepair.size(), ",") + ")";
			}
			DB.update(dbHandle, repairStatement,
					ObjectTransformer.isEmpty(objTypesToRepair) ? null : (Object[]) objTypesToRepair.toArray(new Object[objTypesToRepair.size()]));
		}
	}

	/**
	 * Get a CNDatasourceRow containing a GenticsContentObject that represents
	 * the given object. If the object is a {@link Map},
	 * a {@link GenticsContentObject} containing all the data from the original
	 * object is created.
	 * @param object object to convert into a CNDatasourceRow
	 * @return CNDatasourceRow or null if the object cannot be converted
	 * @throws DatasourceException if the object is a Map, but cannot be
	 *         converted
	 */
	protected CNDatasourceRow getCNDatasourceRow(Object object) throws DatasourceException {
		if (object instanceof GenticsContentObject) {
			return new CNDatasourceRow((GenticsContentObject) object);
		} else if (object instanceof ResolvableGenticsContentObject) {
			return new CNDatasourceRow(((ResolvableGenticsContentObject) object).getContentobject());
		} else if (object instanceof Resolvable) {
			// TODO convert Resolvable into GenticsContentObject. This is not
			// possible right now, because the Resolvable will not give
			// information about the properties it can resolve
			return null;
		} else if (object instanceof Map) {
			return new CNDatasourceRow((GenticsContentObject) create((Map) object, -1, false));
		} else {
			return null;
		}
	}
}
