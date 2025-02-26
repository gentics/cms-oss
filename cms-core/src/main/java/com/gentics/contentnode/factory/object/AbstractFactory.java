/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: AbstractFactory.java,v 1.19.2.1.2.3 2011-02-04 13:17:27 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.BatchObjectFactory;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.Executable;
import com.gentics.contentnode.factory.TransactionStatistics;
import com.gentics.contentnode.factory.TransactionStatistics.Item;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.DummyObject;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.NodeObjectWithModel;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.PublishableNodeObject;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import com.google.common.base.CaseFormat;

/**
 * This is a generic implementation of the Objectfactory with batch loading functionality.
 * The Abstractfactory can be used to implement simple objectfactories.
 */
public abstract class AbstractFactory implements BatchObjectFactory {
	protected static final Class<? extends NodeObject>[] EMPTY_CLASS = new Class[0];

	/**
	 * Map for caching the objects to delete
	 * This map holds for each transaction another map where each object to delete is associated with a class.
	 */
	private Map<Transaction, Map<Class<? extends NodeObject>, Collection>> deleteMap = new HashMap<Transaction, Map<Class<? extends NodeObject>, Collection>>();

	/**
	 * Logger
	 */
	protected static final NodeLogger logger = NodeLogger.getNodeLogger(AbstractFactory.class);

	/**
	 * SQL Statement for creating a new channelset id
	 */
	protected final static String INSERT_CHANNELSET_SQL = "INSERT INTO channelset (id) VALUES (NULL)";

	/**
	 * Map containing all the factory data information for factory classes
	 */
	protected final static Map<Integer, FactoryDataInformation> FACTORY_DATA_INFO = new HashMap<Integer, AbstractFactory.FactoryDataInformation>();

	/**
	 * Object Mapper for conversion to/from REST Model
	 */
	protected final static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Map of class -&gt; ttype of {@link NodeObject} implementation handled by this factory
	 */
	private Map<Class<? extends NodeObject>, Integer> class2Ttype = new HashMap<>();

	/**
	 * Map of class -&gt; {@link DBTable} annotation
	 */
	private Map<Class<? extends NodeObject>, DBTable> class2DBTable = new HashMap<>();

	/**
	 * Create the SQL statement to select a single record from the given table together with the globalid and udate
	 * @param tableName name of the table
	 * @return SQL statement
	 */
	public final static String createSelectStatement(String tableName) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("SELECT * FROM `").append(tableName).append("` WHERE id = ?");
		return buffer.toString();
	}

	/**
	 * Create the SQL statement for batchloading records from the given table together with the globalid and udate
	 * @param tableName name of the table
	 * @return SQL statement
	 */
	public final static String createBatchLoadStatement(String tableName) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("SELECT * FROM `").append(tableName).append("` WHERE id IN ");
		return buffer.toString();
	}

	/**
	 * Create the SQL statement to select a single record from the version table
	 * @param baseTableName name of the base table
	 * @return SQL statement
	 */
	public final static String createSelectVersionedStatement(String baseTableName) {
		String nodeVersionTable = baseTableName + "_nodeversion";

		return String.format(
				"SELECT object.* FROM `%s` object WHERE object.id = ? AND nodeversiontimestamp = (SELECT MAX(nodeversiontimestamp) FROM `%s` WHERE (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? AND id = ? GROUP BY id)",
				nodeVersionTable, nodeVersionTable);
	}

	/**
	 * Check whether the given object has a globalId set, which is not yet bound to a local object
	 * @param object object
	 * @throws NodeException
	 */
	protected final static void assertNewGlobalId(NodeObject object) throws NodeException {
		// get a possibly set globalid
		GlobalId globalId = object.getGlobalId();

		if (globalId != null) {
			Integer localId = globalId.getLocalId(object.getObjectInfo().getObjectClass());

			if (localId != null) {
				throw new NodeException("Cannot create object with global ID " + globalId + ": found another object with that global ID");
			}
		}
	}

	/**
	 * Synchronize the globalId for a new created object
	 * @param object object
	 * @throws NodeException
	 */
	protected final static void synchronizeGlobalId(final NodeObject object) throws NodeException {
		if (object.getGlobalId() == null) {
			Transaction t = TransactionManager.getCurrentTransaction();
			String tableName = t.getTable(object.getObjectInfo().getObjectClass());

			final String[] uuid = new String[1];
			DBUtils.executeStatement("SELECT uuid FROM " + tableName + " WHERE id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, object.getId());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					if (rs.next()) {
						uuid[0] = rs.getString("uuid");
					}
				}
			});

			if (uuid[0] != null) {
				object.setGlobalId(new GlobalId(uuid[0]));
			}
		}
	}

	/**
	 * Register the factory class. This will generate sql statements to insert and update records.
	 * @param tableName table name
	 * @param ttype ttype
	 * @param uuid true if the table has columns uuid and udate
	 * @param clazz class
	 * @throws NodeException
	 */
	protected static void registerFactoryClass(String tableName, int ttype, boolean uuid, Class<? extends NodeObject> clazz) throws NodeException {
		List<FactoryDataField> dataFields = new Vector<AbstractFactory.FactoryDataField>();
		// get the fields
		Field[] fields = clazz.getDeclaredFields();

		Field restModelField = null;

		for (Field field : fields) {
			// check for the @DataField annotation
			if (field.isAnnotationPresent(DataField.class)) {
				dataFields.add(new FactoryDataField(field));
			}
			if (field.isAnnotationPresent(RestModel.class)) {
				restModelField = field;
			}
		}

		// now generate the insert and update sql statements
		List<String> insertFields = new ArrayList<>();
		List<String> updateFields = new ArrayList<>();
		for (FactoryDataField dataField : dataFields) {
			if (!insertFields.contains(dataField.fieldName)) {
				insertFields.add(dataField.fieldName);
			}
			if (dataField.update && !updateFields.contains(dataField.fieldName)) {
				updateFields.add(dataField.fieldName);
			}
		}

		if (restModelField != null) {
			RestModel restModelAnnotation = restModelField.getAnnotation(RestModel.class);
			for (String insert : restModelAnnotation.insert()) {
				if (!insertFields.contains(insert)) {
					insertFields.add(insert);
				}
			}

			for (String update : restModelAnnotation.update()) {
				if (!insertFields.contains(update)) {
					insertFields.add(update);
				}
				if (!updateFields.contains(update)) {
					updateFields.add(update);
				}
			}
		}

		if (uuid) {
			if (!insertFields.contains("uuid")) {
				insertFields.add("uuid");
			}
		}

		String insertSQL = null;
		if (insertFields.isEmpty()) {
			insertSQL = String.format("INSERT INTO `%s`", tableName);
		} else {
			insertSQL = String.format(
				"INSERT INTO `%s` (%s) VALUES (%s)",
				tableName,
				insertFields.stream().map(field -> String.format("`%s`", field)).collect(Collectors.joining(", ")),
				insertFields.stream().map(s -> "?").collect(Collectors.joining(", ")));
		}
		String updateSQL = null;

		if (!updateFields.isEmpty()) {
			updateSQL = String.format(
				"UPDATE `%s` SET %s WHERE id = ?",
				tableName,
				updateFields.stream().map(s -> String.format("`%s` = ?", s)).collect(Collectors.joining(", ")));
		}

		try {
			FactoryDataInformation info = new FactoryDataInformation().setSelectSQL(createSelectStatement(tableName))
					.setBatchLoadSQL(createBatchLoadStatement(tableName)).setInsertSQL(insertSQL)
					.setInsertFields(insertFields).setUpdateSQL(updateSQL).setUpdateFields(updateFields)
					.setFactoryDataFields(dataFields).setRestModelField(restModelField)
					.setSetIdMethod(clazz.getMethod("setId", Integer.class)).setUuid(uuid);

			if (PublishableNodeObject.class.isAssignableFrom(clazz)) {
				info.setSelectVersionedSQL(createSelectVersionedStatement(tableName)).setVersionSQLParams(
						new VersionedSQLParam[] { VersionedSQLParam.ID, VersionedSQLParam.VERSIONTIMESTAMP,
								VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.ID });
			}

			FACTORY_DATA_INFO.put(ttype, info);
		} catch (Exception e) {
			throw new NodeException("Error while registering " + clazz, e);
		}
	}

	/**
	 * Save the given factory object into the DB
	 * @param clazz factory class
	 * @param object object
	 * @throws NodeException
	 */
	protected final static void saveFactoryObject(NodeObject object) throws NodeException {
		FactoryDataInformation info = FACTORY_DATA_INFO.get(ObjectTransformer.getInt(object.getTType(), -1));

		if (info == null) {
			throw new NodeException("Error while saving object of " + object.getObjectInfo().getObjectClass() + " could not find factory data information");
		}

		boolean isNew = AbstractContentObject.isEmptyId(object.getId());

		if (isNew) {
			assertNewGlobalId(object);

			// insert a new record
			List<Integer> keys = DBUtils.executeInsert(info.insertSQL, info.getInsertData(object));

			if (keys.size() == 1) {
				// set the new entry id
				info.setId(object, keys.get(0));
				if (info.uuid) {
					synchronizeGlobalId(object);
				}
			} else {
				throw new NodeException("Error while inserting new object, could not get the insertion id");
			}
		} else {
			if (!ObjectTransformer.isEmpty(info.updateSQL)) {
				DBUtils.executeUpdate(info.updateSQL, info.getUpdateData(object));
			}
		}
	}

	/**
	 * Delete a factory object
	 * @param object object to delete
	 * @throws NodeException
	 */
	protected final static void deletedFactoryObject(NodeObject object) throws NodeException {
		if (object == null) {
			return;
		}
		Class<? extends NodeObject> clazz = object.getObjectInfo().getObjectClass();

		Transaction t = TransactionManager.getCurrentTransaction();
		AbstractFactory objectFactory = (AbstractFactory)t.getObjectFactory(clazz);

		@SuppressWarnings("unchecked")
		Collection<NodeObject> deleteList = objectFactory.getDeleteList(t, clazz);
		deleteList.add(object);
	}

	/**
	 * Get the data map of the given object
	 * @param object object
	 * @return data map
	 * @throws NodeException
	 */
	protected final static Map<String, Object> getDataMap(NodeObject object) throws NodeException {
		return getDataMap(object, false);
	}

	/**
	 * Get the data map of the given object
	 * @param object object
	 * @param unversioned true to only get data of fields with the {@link Unversioned} annotation
	 * @return data map
	 * @throws NodeException
	 */
	protected final static Map<String, Object> getDataMap(NodeObject object, boolean unversioned) throws NodeException {
		FactoryDataInformation info = FACTORY_DATA_INFO.get(ObjectTransformer.getInt(object.getTType(), -1));

		if (info == null) {
			throw new NodeException("Error while getting data for " + object.getObjectInfo().getObjectClass() + " could not find factory data information");
		}

		Map<String, Object> dataMap = new HashMap<String, Object>();

		if (info.restModelField != null && !unversioned) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> camelDataMap = mapper.convertValue(info.restModelField.get(object), Map.class);
				for (Map.Entry<String, Object> entry : camelDataMap.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();

					if (value instanceof Map) {
						value = mapper.convertValue(value, JsonNode.class).toString();
					}

					if ("globalId".equals(key)) {
						dataMap.put("uuid", value);
					} else {
						dataMap.put(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), value);
					}
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new NodeException(String.format("Error while getting data for %s", object), e);
			}
		}

		for (FactoryDataField dataField : info.factoryDataFields) {
			if (!unversioned || dataField.unversioned) {
				dataMap.put(dataField.fieldName, dataField.get(object));
			}
		}

		return dataMap;
	}

	/**
	 * Set the data map of the given object
	 * @param object object
	 * @param dataMap data map
	 * @throws NodeException
	 */
	protected final static void setDataMap(NodeObject object, Map<String, Object> dataMap) throws NodeException {
		setDataMap(object, dataMap, false);
	}

	/**
	 * Set the data map of the given object
	 * @param object object
	 * @param dataMap data map
	 * @param unversioned true to only set data for fields with the {@link Unversioned} annotation
	 * @throws NodeException
	 */
	protected final static void setDataMap(NodeObject object, Map<String, Object> dataMap, boolean unversioned) throws NodeException {
		FactoryDataInformation info = FACTORY_DATA_INFO.get(ObjectTransformer.getInt(object.getTType(), -1));

		if (info == null) {
			throw new NodeException("Error while getting data for " + object.getObjectInfo().getObjectClass() + " could not find factory data information");
		}

		for (FactoryDataField dataField : info.factoryDataFields) {
			if (!unversioned  || dataField.unversioned) {
				dataField.set(object, dataMap.get(dataField.fieldName));
			}
		}

		if (info.restModelField != null && !unversioned) {
			Map<String, Object> camelDataMap = new HashMap<>(dataMap.size());
			for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
				if ("uuid".equals(entry.getKey())) {
					camelDataMap.put("globalId", entry.getValue());
				} else {
					camelDataMap.put(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, entry.getKey()), entry.getValue());
				}
			}

			try {
				info.restModelField.set(object, mapper.convertValue(camelDataMap, info.restModelField.getType()));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new NodeException(String.format("Error while setting data for %s", object), e);
			}
		}
	}

	/**
	 * Update the model of the given object
	 * @param object object
	 * @param from model with updated values
	 * @return true if the model was changed, false if not
	 * @throws NodeException
	 */
	protected final static <T> boolean updateModel(NodeObjectWithModel<T> object, T from) throws NodeException {
		try {
			// do not allow to overwrite id and globalId
			List<String> protectedKeys = Arrays.asList("id", "globalId");
			// store original content as serialized string (for modification check)
			String originalContent = mapper.writeValueAsString(object.getModel());

			// convert "from" model into map and remove protected entries and null values
			@SuppressWarnings("unchecked")
			Map<String, Object> map = mapper.convertValue(from, Map.class);
			map.entrySet().removeIf(e -> e.getValue() == null || protectedKeys.contains(e.getKey()));

			mapper.updateValue(object.getModel(), map);
			String updatedContent = mapper.writeValueAsString(object.getModel());

			return !originalContent.equals(updatedContent);
		} catch (JsonProcessingException e) {
			throw new NodeException(String.format("Error while updating %s from REST model", object), e);
		}
	}

	/**
	 * Update the model from data of the given node object
	 * @param model model to update
	 * @param from object from which to get the data
	 * @return updated model
	 * @throws NodeException
	 */
	public final static <T> T update(T model, NodeObjectWithModel<T> from) throws NodeException {
		try {
			return mapper.updateValue(model, from.getModel());
		} catch (JsonMappingException e) {
			throw new NodeException(String.format("Error while updating REST model from %s", from), e);
		}
	}

	/**
	 * Get the set of modified attributes
	 * @param origObject original object
	 * @param modObject modified object
	 * @return set of modified attributes, null if the original or modified object is null
	 * @throws NodeException
	 */
	protected final static Set<String> getModifiedAttributes(NodeObject origObject, NodeObject modObject) throws NodeException {
		if (origObject == null || modObject == null) {
			return null;
		}

		// get the data from the objects
		Map<String, Object> origDataMap = getDataMap(origObject);
		Map<String, Object> modDataMap = getDataMap(modObject);

		// compare the data
		Set<String> modifiedAttributes = new HashSet<>();

		for (String key : origDataMap.keySet()) {
			if (!StringUtils.isEqual(ObjectTransformer.getString(origDataMap.get(key), null), ObjectTransformer.getString(modDataMap.get(key), null))) {
				modifiedAttributes.add(key);
			}
		}

		return modifiedAttributes;
	}

	/**
	 * Get the list of modified attributes
	 * @param origObject original object
	 * @param modObject modified object
	 * @return list of modified attributes, null if the original or modified object is null
	 * @throws NodeException
	 */
	protected final static String[] getModifiedData(NodeObject origObject, NodeObject modObject) throws NodeException {
		Set<String> modifiedAttributes = getModifiedAttributes(origObject, modObject);
		if (modifiedAttributes == null) {
			return null;
		}
		return modifiedAttributes.toArray(new String[modifiedAttributes.size()]);
	}

	/**
	 * This is the default constructor, which initializes a new factory.
	 * The key for the used connection is {@link ContentConfiguration#NODE_DB_KEY}.
	 */
	protected AbstractFactory() {
		DBTables dbTables = getClass().getAnnotation(DBTables.class);
		if (dbTables != null) {
			for (DBTable dbTable : dbTables.value()) {
				Class<? extends NodeObject> clazz = dbTable.clazz();
				class2DBTable.put(clazz, dbTable);
				TType ttype = clazz.getAnnotation(TType.class);
				if (ttype != null) {
					class2Ttype.put(clazz, ttype.value());
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#reloadConfiguration()
	 */
	public void reloadConfiguration() {}
    
	/**
	 * get the currently used configuation.
	 * @return the configuration used by this factory.
	 */
	public NodeConfig getConfiguration() {
		return NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();
	}

	/**
	 * Checks if the deleteList contains entries.
	 * Should be preferred over getDeleteList().isEmpty() because it is more performant.
	 * @return 
	 */
	protected boolean isEmptyDeleteList(Transaction t, Class<? extends NodeObject> clazz) {
		Map<Class<? extends NodeObject>, Collection> toDelete = this.deleteMap.get(t);

		if (toDelete == null) {
			return true;
		}
		Collection col = toDelete.get(clazz);

		if (col == null) {
			return true;
		}
		return col.isEmpty();
	}
    
	/**
	 * Gets the size of all delete lists in this factory summed together.<br />
	 * This represents the number of all objects that are marked for deletion in this factory.
	 * @param t Transaction for which to get the size of objects marked for deletion.
	 * @return The number of objects which are marked for deletion.
	 */
	public synchronized int getDeleteListsSize(Transaction t) {
		Map<Class<? extends NodeObject>, Collection> toDelete = this.deleteMap.get(t);

		if (toDelete == null) {
			return 0;
		}
		int cnt = 0;

		for (Iterator<Collection> it = toDelete.values().iterator(); it.hasNext();) {
			cnt += it.next().size();
		}
		return cnt;
	}

	/**
	 * Get a list of Objects that should be deleted when committing the
	 * transaction. Returns an empty collection which is writable if no objects
	 * are stored for deletion. Never returns null.
	 * @param t Transaction for which the list should be delivered
	 * @param clazz Class for which the list should be delivered
	 * @return The Collection of objects that should be deleted.
	 */
	protected synchronized Collection getDeleteList(Transaction t, Class<? extends NodeObject> clazz) {
		// images will be collected as "Files"
		if (ImageFile.class.equals(clazz)) {
			clazz = File.class;
		}
		Map<Class<? extends NodeObject>, Collection> toDelete = this.deleteMap.get(t);

		if (toDelete == null) {
			toDelete = new HashMap<Class<? extends NodeObject>, Collection>();
			this.deleteMap.put(t, toDelete);
		}
		Collection col = toDelete.get(clazz);

		if (col == null) {
			col = new HashSet<Object>();
			toDelete.put(clazz, col);
		}
		return col;
	}

	/**
	 * Get the delete list for the given type of objects
	 * @param clazz class
	 * @return collection of objects
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	protected <T extends NodeObject> Collection<T> getDeleteList(Class<T> clazz) throws NodeException {
		return getDeleteList(TransactionManager.getCurrentTransaction(), clazz);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#removeDeleteList(com.gentics.lib.base.factory.Transaction)
	 */
	public synchronized void removeDeleteList(Transaction t) {
		this.deleteMap.remove(t);
	}
    
	/**
	 * Removes a deleteList for the specified transaction and class.
	 * @param t Transaction for which the list should be removed
	 * @param clazz Class for which the list should be removed
	 */
	protected synchronized void removeDeleteList(Transaction t, Class<? extends NodeObject> clazz) {
		Map<Class<? extends NodeObject>, Collection> toDelete = this.deleteMap.get(t);

		if (toDelete == null) {
			return;
		}
		toDelete.remove(clazz);
		if (toDelete.isEmpty()) {
			this.deleteMap.remove(t);
		}
	}
    
	/**
	 * Performs the delete flush for the objects from the specified class and
	 * the current Transaction.
	 * @param deleteSql SQL Statement which is used for delete with
	 *        {@link DBUtils#executeMassStatement(String, List, int, com.gentics.lib.db.SQLExecutor)}
	 *        . The statement must not contain any bind variables and has to end with "IN".
	 * @param clazz Class for which the delete should be performed
	 * @throws NodeException if an internal error occurs
	 */
	protected void flushDelete(String deleteSql, Class<? extends NodeObject> clazz) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<? extends NodeObject> toDelete = getDeleteList(t, clazz);

		if (!toDelete.isEmpty()) {
			List<Object> ids = new LinkedList<Object>();

			for (Iterator<? extends NodeObject> it = toDelete.iterator(); it.hasNext();) {
				NodeObject obj = it.next();

				ids.add(obj.getId());

				t.dirtObjectCache(clazz, obj.getId(), true);
			}

			DBUtils.executeMassStatement(deleteSql, ids, 1, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#isInDeletedList(Class<? extends NodeObject>, NodeObject)
	 */
	public boolean isInDeletedList(Class<? extends NodeObject> clazz, NodeObject obj) throws TransactionException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<?> toDelete = getDeleteList(t, clazz);
		return toDelete.contains(obj);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#isInDeletedList(java.lang.Class, java.lang.Integer)
	 */
	public boolean isInDeletedList(Class<? extends NodeObject> clazz, Integer id) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<?> toDelete = getDeleteList(t, clazz);
		return toDelete.contains(new DummyObject(id, t.createObjectInfo(clazz)));
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#removeDeleted(java.lang.Class, java.util.Collection)
	 */
	public void removeDeleted(Class<? extends NodeObject> clazz, Collection<Integer> ids) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<?> toDelete = getDeleteList(t, clazz);
		for (Iterator<Integer> i = ids.iterator(); i.hasNext(); ) {
			Integer id = i.next();
			if (toDelete.contains(new DummyObject(id, t.createObjectInfo(clazz)))) {
				i.remove();
			}
		}
	}

	@Override
	public Collection<Integer> getDeletedIds(Class<? extends NodeObject> clazz) throws NodeException {
		return getDeleteList(clazz).stream().map(NodeObject::getId).collect(Collectors.toSet());
	}

	@Override
	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		int ttype = class2Ttype.getOrDefault(clazz, 0);
		if (ttype > 0) {
			FactoryDataInformation factoryDataInfo = FACTORY_DATA_INFO.get(ttype);
			if (factoryDataInfo != null) {
				return batchLoadDbObjects(clazz, ids, info, factoryDataInfo.batchLoadSQL + buildIdSql(ids));
			}
		}
		return Collections.emptyList();
	}

	@Override
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id,
			NodeObjectInfo info) throws NodeException {
		int ttype = class2Ttype.getOrDefault(clazz, 0);
		if (ttype > 0) {
			FactoryDataInformation factoryDataInfo = FACTORY_DATA_INFO.get(ttype);
			if (factoryDataInfo != null) {
				return loadDbObject(clazz, id, info, factoryDataInfo.selectSQL, factoryDataInfo.selectVersionedSQL,
						factoryDataInfo.versionSQLParams);
			}
		}
		return null;
	}

	/**
	 * This is a helper method which loads a single object from the database
	 * using a given sql-statement. The statement must contain one bind
	 * variable, which will be binded with the id of the object.
	 * @param clazz the class of the object to load.
	 * @param id the id of the object to load.
	 * @param info an info object with info about the objects type to create.
	 * @param sql a sql statement which returns all needed values.
	 * @param versionSQL variant of the sql statement which fetches the
	 *        versioned data (optional, but must be set when the info has a
	 *        version timestamp set)
	 * @param versionSQLParams defines, which bind variables in the versionSQL
	 *        must be filled with the id, and which with the version timestamp
	 * @return a new nodeobject, created with
	 *         {@link #loadResultSet(Class, Object, NodeObjectInfo, FactoryDataRow, List[])},
	 *         or null if not found.
	 * @throws NodeException
	 */
	protected <T extends NodeObject> T loadDbObject(Class<T> clazz,
			Integer id, NodeObjectInfo info, String sql, String versionSQL,
			VersionedSQLParam[] versionSQLParams) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		T obj = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		FactoryDataRow row = null;

		try {
			stmt = t.prepareStatement(sql);
			stmt.setInt(1, id);

			rs = stmt.executeQuery();
			if (rs.next()) {
				row = new FactoryDataRow(rs);
			}

			// if the object shall contain versioned data and a version sql was provided, use it to get the versioned data
			if (info.getVersionTimestamp() >= 0 && versionSQL != null) {
				if (versionSQLParams == null) {
					throw new NodeException(
							"Error while loading versioned data for object {" + id + "} of class {" + clazz.getName()
							+ "}: parameter information for versioned query is missing");
				}

				t.closeResultSet(rs);
				t.closeStatement(stmt);

				// prepare the statement for fetching
				stmt = t.prepareStatement(versionSQL);
				for (int i = 0; i < versionSQLParams.length; i++) {
					switch (versionSQLParams[i]) {
					case ID:
						stmt.setInt(i + 1, id);
						break;

					case VERSIONTIMESTAMP:
						stmt.setInt(i + 1, info.getVersionTimestamp());
						break;

					default:
						throw new NodeException(
								"Error while loading versioned data for object {" + id + "} of class {" + clazz.getName() + "}: unexpected parameter type "
								+ versionSQLParams[i] + " found");
					}
				}

				rs = stmt.executeQuery();
				if (rs.next()) {
					if (row != null) {
						row.mergeWithResultSet(rs);
					} else {
						row = new FactoryDataRow(rs);
					}
				}
			}

			if (row != null) {
				obj = loadResultSet(clazz, id, info, row, null);
			}
		} catch (SQLException e) {
			throw new NodeException("Could not load object {" + id + "} of class {" + clazz.getName() + "}", e);
		} finally {
			t.closeResultSet(rs);
			t.closeStatement(stmt);
		}

		return obj;
	}

	/**
	 * Load a list of objects from the database, using a single given sql-statement.
	 * The statement must not contain any bind variables.
	 *
	 * @param clazz the class of the objects to load.
	 * @param ids the list of ids of the objects to load.
	 * @param info an info of the type of objects to load.
	 * @param sql the statement to get all the values of the objects.
	 * @return a list of all found objects, created with {@link #loadResultSet(Class, Object, NodeObjectInfo, FactoryDataRow, List[])}.
	 * @throws NodeException
	 */
	protected <T extends NodeObject> Collection<T> batchLoadDbObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info, String sql) throws NodeException {
		return batchLoadDbObjects(clazz, ids, info, sql, null);
	}

	/**
	 * Load a list of objects from the database, using a single given sql-statement.
	 * The statement must not contain any bind variables. In Addition, several lists of ids
	 * per object can be loaded using only one statement per id-list. <br>
	 * Each statement in the preloadIdSql list is executed and expected to return a result which contains
	 * a column named 'id' which contains one of the ids of the loaded objects, and a column named 'id2'
	 * which contains ids which should be associated with the object with the id of the first column. <br>
	 * The id-lists are hashed by the 'id' column, and a list with lists of the ids of 'id2' are given in the same
	 * order and position as the statements are given in {@param preloadIdSql}.
	 *
	 * @param clazz the class of the objects to load.
	 * @param ids the list of ids of the objects to load.
	 * @param info an info of the type of objects to load.
	 * @param sql the statement to get all the values of the objects.
	 * @param preloadIdSql a list of sql-statements which return the ids per object-id.
	 * @return a list of all found objects, created with {@link #loadResultSet(Class, Object, NodeObjectInfo, FactoryDataRow, List[])}.
	 * @throws NodeException
	 */
	protected <T extends NodeObject> Collection<T> batchLoadDbObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info,
			String sql, String[] preloadIdSql) throws NodeException {
		// when versioned objects shall be loaded, we do this one by one
		if (!info.isCurrentVersion()) {
			Collection<T> objects = new Vector<T>(ids.size());

			for (Integer id : ids) {
				T object = loadObject(clazz, id, info);
				if (object != null) {
					objects.add(object);
				}
			}

			return objects;
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		Collection<T> objs = new ArrayList<T>(ids.size());

		Statement stmt = null;
		ResultSet rs = null;

		try {

			Map<Integer, List<Integer>[]> idMap = fetchIdMap((Collection<Integer>) ids, preloadIdSql);

			stmt = t.getStatement();
			rs = stmt.executeQuery(sql);

			while (rs.next()) {
				Integer id = new Integer(rs.getInt("id"));
				List<Integer>[] idLists = (idMap != null ? idMap.get(id) : null);

				objs.add((T) loadResultSet(clazz, id, info, new FactoryDataRow(rs), idLists));
			}

		} catch (SQLException e) {
			throw new NodeException("Could not batch-load objects", e);
		} finally {
			t.closeResultSet(rs);
			t.closeStatement(stmt);
		}

		return objs;
	}

	/**
	 * Preload a list of objects, using a given sql-statement. The Statement must contain one bind-
	 * variable which will be set to the {@param oId}. The resultset must contain a column named 'id' which
	 * holds the id of the object represented by the resultrow. The loaded objects are stored to the factory
	 * using the FactoryHandle.<br>
	 * In Addition, several lists of ids per object can be loaded using only one statement per id-list. <br>
	 *
	 * Each statement in the preloadIdSql list is executed and expected to return a result which contains
	 * a column named 'id' which contains one of the ids of the loaded objects, and a column named 'id2'
	 * which contains ids which should be associated with the object with the id of the first column. <br>
	 * The id-lists are hashed by the 'id' column, and a list with lists of the ids of 'id2' are given in the same
	 * order and position as the statements are given in {@param preloadIdSql}.
	 *
	 * @param clazz the class of the objects to load.
	 * @param oId the id of the object which triggered the preload action.
	 * @param sql the statement to get all the values of the objects.
	 * @param preloadIdSql a list of sql-statements which return the ids per object-id.
	 * @throws NodeException
	 */
	protected void preloadDbObjects(Class<? extends NodeObject> clazz, Integer oId, String sql,
			String[] preloadIdSql) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		PreparedStatement stmt = null;
		ResultSet rs = null;

		NodeObjectInfo info = t.createObjectInfo(clazz);

		try {

			Map idMap = fetchIdMap(null, preloadIdSql);

			stmt = t.prepareStatement(sql);
			stmt.setInt(1, oId.intValue());

			rs = stmt.executeQuery();

			while (rs.next()) {
				Integer id = rs.getInt("id");

				if (!t.isCached(clazz, id)) {
					List[] idLists = (List[]) (idMap != null ? idMap.get(id) : null);

					t.putObject(clazz, loadResultSet(clazz, id, info, new FactoryDataRow(rs), idLists));
				}

			}

		} catch (SQLException e) {
			throw new NodeException("Could not preload objects.", e);
		} finally {
			t.closeResultSet(rs);
			t.closeStatement(stmt);
		}
	}

	/**
	 * Preload a list of objects, using a given sql-statement. The Statement must not contain any bind-
	 * variables. The resultset must contain a column named 'id' which
	 * holds the id of the object represented by the resultrow. The loaded objects are stored to the factory
	 * using the FactoryHandle.<br>
	 * In Addition, several lists of ids per object can be loaded using only one statement per id-list. <br>
	 *
	 * Each statement in the preloadIdSql list is executed and expected to return a result which contains
	 * a column named 'id' which contains one of the ids of the loaded objects, and a column named 'id2'
	 * which contains ids which should be associated with the object with the id of the first column. <br>
	 * The id-lists are hashed by the 'id' column, and a list with lists of the ids of 'id2' are given in the same
	 * order and position as the statements are given in {@param preloadIdSql}.
	 *
	 * @param clazz the class of the objects to load.
	 * @param sql the statement to get all the values of the objects.
	 * @param preloadIdSql a list of sql-statements which return the ids per object-id.
	 * @throws NodeException
	 */
	protected void preloadDbObjects(Class<? extends NodeObject> clazz, String sql,
			String[] preloadIdSql) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Statement stmt = null;
		ResultSet rs = null;

		NodeObjectInfo info = t.createObjectInfo(clazz);

		try {

			Map<Integer, List<Integer>[]> idMap = fetchIdMap(null, preloadIdSql);

			stmt = t.getStatement();
			rs = stmt.executeQuery(sql);

			while (rs.next()) {
				Integer id = new Integer(rs.getInt("id"));

				if (!t.isCached(clazz, id)) {
					List<Integer>[] idLists = (idMap != null ? idMap.get(id) : null);

					t.putObject(clazz, loadResultSet(clazz, id, info, new FactoryDataRow(rs), idLists));
				}

			}

		} catch (SQLException e) {
			throw new TransactionException("Could not preload objects", e);
		} finally {
			t.closeResultSet(rs);
			t.closeStatement(stmt);
		}
	}

	/**
	 * This method is used to create a new object from a fetched result row.
	 * @param clazz the class of the object to create.
	 * @param id the id of the object to create.
	 * @param info an info object describing the type of the object to load.
	 * @param rs the pointer to the current result row with the fetched values.
	 * @param idLists a list of Lists of ids as Integers which are loaded using the preloadIdSql statements, or null if not loaded.
	 *
	 * @return a new object initialized with the given values, or null if the object could not be created.
	 * @throws SQLException
	 * @throws NodeException 
	 */
	protected abstract <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs,
			List<Integer>[] idLists) throws SQLException, NodeException;

	/**
	 * This method flushes all outstanding operations for this transaction that were previously cached.
	 * It is called for every factory when a Transaction is committed.
	 * @throws NodeException if an internal error occurs
	 */
	public void flush() throws NodeException {}
    
	/**
	 * A helper function to build a list of ids as (id1,id2,..) from the given collection of ids.
	 * @param ids a collection of Integers.
	 * @return the ids as list in parenthesis.
	 */
	protected String buildIdSql(Collection<Integer> ids) {

		StringBuffer sql = new StringBuffer("('");

		for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
			Integer id = it.next();

			sql.append(id);
			if (it.hasNext()) {
				sql.append("','");
			}
		}
		sql.append("')");
		return sql.toString();
	}

	/**
	 * A helper method to close a connection and to catch all errors.
	 * If the connection is null or closed, nothing is done.
	 * @param connect the connection to be closed.
	 */
	protected void returnConnection(Connection connect) {
		NodeConfig config = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();

		config.returnConnection(connect);
	}

	public boolean revalidate(FactoryHandle handle, Class clazz, int id, Date date) {
		return false;
	}

	public void store(FactoryHandle handle, Class clazz, NodeObject object) {}

	// ----- Helper functions for idMap preloading ------

	private Map<Integer, List<Integer>[]> createIdMap(Collection<Integer> ids, String[] sql) {

		Map<Integer, List<Integer>[]> idMap;

		if (sql != null && sql.length > 0) {
			idMap = new HashMap<Integer, List<Integer>[]>(ids.size());
			for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
				Integer id = it.next();

				List<Integer>[] idLists = new List[sql.length];

				for (int j = 0; j < idLists.length; j++) {
					idLists[j] = new ArrayList<Integer>();
				}

				idMap.put(id, idLists);
			}
		} else {
			idMap = null;
		}

		return idMap;
	}

	private Map<Integer, List<Integer>[]> fetchIdMap(int oId, String[] sql) throws SQLException, NodeException {

		Map<Integer, List<Integer>[]> idMap = new HashMap<Integer, List<Integer>[]>();

		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			for (int i = 0; i < sql.length; i++) {
				stmt = t.prepareStatement(sql[i]);
				stmt.setInt(1, oId);

				rs = stmt.executeQuery();

				loadIdMapResultSet(rs, null, idMap, sql.length, i);

			}
		} finally {
			t.closeResultSet(rs);
			t.closeStatement(stmt);
		}

		return idMap;
	}

	private Map<Integer, List<Integer>[]> fetchIdMap(Collection<Integer> ids, String[] sql) throws SQLException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Map<Integer, List<Integer>[]> idMap = null;

		if (ids != null) {
			idMap = createIdMap(ids, sql);
		} else if (sql != null && sql.length > 0) {
			idMap = new HashMap<Integer, List<Integer>[]>();
		}

		Statement stmt = null;
		ResultSet rs = null;

		if (idMap != null) {
			for (int i = 0; i < sql.length; i++) {
				try {
					stmt = t.getStatement();
					rs = stmt.executeQuery(sql[i]);

					loadIdMapResultSet(rs, ids, idMap, sql.length, i);
				} finally {
					t.closeResultSet(rs);
					t.closeStatement(stmt);
				}
			}
		}

		return idMap;
	}

	private void loadIdMapResultSet(ResultSet rs, Collection<Integer> ids, Map<Integer, List<Integer>[]> idMap, int size, int pos) throws SQLException {

		while (rs.next()) {
			Integer id = new Integer(rs.getInt("id"));
			Integer id2 = new Integer(rs.getInt("id2"));

			List<Integer>[] idLists;

			if (ids == null && !idMap.containsKey(id)) {
				idLists = new List[size];
				for (int j = 0; j < size; j++) {
					idLists[j] = new ArrayList<Integer>();
				}

				idMap.put(id, idLists);
			} else {
				idLists = idMap.get(id);
			}
			if (idLists != null) {
				idLists[pos].add(id2);
			}
		}

	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException,
				ReadOnlyException {
		// this dummy implementation just returns the object itself
		return object;
	}

	/**
	 * Enumeration for the SQL Parameters given for versioned SQL Statements.
	 */
	public static enum VersionedSQLParam {
		ID, VERSIONTIMESTAMP
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#doMultichannellingFallback(com.gentics.lib.base.object.NodeObject)
	 */
	public <T extends NodeObject> T doMultichannellingFallback(T object) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			// avoid unterminated recursions here
			if (t.isDisableMultichannellingFlag()) {
				// multichannelling search is in progress, so do no further fallback
				return object;
			}

			TransactionStatistics stats = t.getStatistics();
			try {
				if (stats != null) {
					stats.get(Item.MC_FALLBACK).start();
				}

				// do the multichannelling fallback here, so first set the flag
				t.setDisableMultichannellingFlag(true);

				// special case for nodes
				if (object instanceof Node) {
					// when the current channel is in the same channel structure as the given node
					// we return the current channel
					Node node = (Node)object;
					Node master = node.getMaster();
					Node channel = getCurrentChannel();
					if (channel != null && channel.isChannelOf(master)) {
						return (T)channel;
					} else {
						// otherwise we return the given node
						return object;
					}
				} else if (!(object instanceof LocalizableNodeObject)) {
					return object;
				}

				LocalizableNodeObject<T> localizable = (LocalizableNodeObject)object;
				return MultichannellingFactory.getChannelVariant(localizable, getCurrentChannel());
			} finally {
				// unset the flag
				t.resetDisableMultichannellingFlag();

				if (stats != null) {
					stats.get(Item.MC_FALLBACK).stop(1, object.getObjectInfo().getObjectClass().getSimpleName());
				}
			}

		} else {
			return object;
		}
	}

	/**
	 * Create a new channelset id
	 * @return new channelset id
	 * @throws NodeException
	 */
	public static Integer createChannelsetId() throws NodeException {
		final List<Integer> channelSetId = new ArrayList<Integer>();

		TransactionManager.execute(new Executable() {
			public void execute() throws NodeException {
				channelSetId.addAll(DBUtils.executeInsert(INSERT_CHANNELSET_SQL, null));
			}
		});

		if (channelSetId.size() == 1) {
			return channelSetId.get(0);
		} else {
			throw new NodeException("Error while inserting new channelset, could not get the insertion id");
		}
	}

	/**
	 * This method will add MC_HIDE commands into the logcmd table for master
	 * objects of the given object for the given channel and all subchannels.
	 * The method must be called, when a localized copy of an object is created
	 * (published)
	 * It will also dirt the object for hiding and its dependent objects for
	 * changed dependencies.
	 * @param objectType object type
	 * @param oId object id
	 * @param channel channel of the object
	 * @param channelSet channelset of the object
	 * @throws NodeException
	 */
	public static void hideFormerInheritedObjects(int objectType, Integer oId, Node channel,
			Map<Integer, Integer> channelSet) throws NodeException {
		List<Node> masterNodes = channel.getMasterNodes();
		Collection<Node> subChannels = new Vector<Node>(channel.getAllChannels());

		subChannels.add(channel);

		Transaction t = TransactionManager.getCurrentTransaction();

		for (Node master : masterNodes) {
			if (channelSet.containsKey(master.getId())) {
				Integer id = channelSet.get(master.getId());
				NodeObject objectToHide = t.getObject(t.getClass(objectType), id, false, false, true);

				for (Node subChannel : subChannels) {
					int channelId = ObjectTransformer.getInt(subChannel.getId(), 0);
					ActionLogger.logCmd(ActionLogger.MC_HIDE, objectType, id, channelId,
							"inherited object overwritten by localized copy " + oId + " in channel " + channelId);
					PublishQueue.dirtObject(objectToHide, Action.HIDE, channelId);
					DependencyObject depObj = new DependencyObject(objectToHide);
					objectToHide.triggerEvent(depObj, new String[]{"online"}, Events.UPDATE, 0, channelId);
				}
			}
		}

		// special behaviour for the master object
		Integer id = channelSet.get(0);
		NodeObject objectToHide = t.getObject(t.getClass(objectType), id, false, false, true);

		if (objectToHide != null) {
			for (Node subChannel : subChannels) {
				int channelId = ObjectTransformer.getInt(subChannel.getId(), 0);
				ActionLogger.logCmd(ActionLogger.MC_HIDE, objectType, id, subChannel.getId(),
						"inherited object overwritten by localized copy " + oId + " in channel " + channelId);
				PublishQueue.dirtObject(objectToHide, Action.HIDE, ObjectTransformer.getInt(subChannel.getId(), 0));
				DependencyObject depObj = new DependencyObject(objectToHide);
				objectToHide.triggerEvent(depObj, new String[]{"online"}, Events.UPDATE, 0, channelId);
			}
		}
	}

	/**
	 * This method will add MC_UNHIDE commands into the logcmd table for the immediate master object
	 * of the given object for the given channel and all subchannels, that will now inherit the master object.
	 * The method must be called, when a localized copy of an object is deleted (taken offline)
	 * @param objectType object type
	 * @param oId object id
	 * @param channel channel of the object
	 * @param channelSet channelset of the object
	 * @throws NodeException
	 */
	protected static void unhideFormerHiddenObjects(int objectType, Integer oId,
			Node channel, Map<Integer, Integer> channelSet) throws NodeException {
		// determine the next higher master object
		Integer masterId = null;
		List<Node> masterNodes = channel.getMasterNodes();

		for (Node master : masterNodes) {
			if (channelSet.containsKey(master.getId())) {
				masterId = channelSet.get(master.getId());
				break;
			}
		}

		// not found for a node, so try 0 (master object)
		if (masterId == null) {
			masterId = channelSet.get(0);
		}

		// still not found?
		if (masterId == null) {
			// we are done
			return;
		}

		// check whether the master object itself is deleted
		Transaction t = TransactionManager.getCurrentTransaction();
		Class<? extends NodeObject> objectClass = t.getClass(objectType);
		if (t.getObjectFactory(objectClass).isInDeletedList(objectClass, masterId)) {
			return;
		}

		// recursively unhide the objects
		recursiveUnhideObjects(objectType, oId, masterId, channel, channelSet,
				"inherited object no longer hidden by localized copy " + oId + " in channel " + channel.getId());
	}

	/**
	 * Recursive method to unhide an inherited master object. This method will
	 * check, whether a localized copy of the master exists in the channel. If
	 * no localized copy exists, the master will be unidden and the method will
	 * do the recursion into the subchannels.
	 * 
	 * @param objectType
	 *            object type
	 * @param oId
	 *            object id of the object, which was hiding the master
	 * @param masterId
	 *            object id of the master
	 * @param channel
	 *            channel, in which the object will now be visible
	 * @param channelSet
	 *            channelset of the object
	 * @param message
	 *            message to include in the logcmd entry
	 * @throws NodeException
	 */
	protected static void recursiveUnhideObjects(int objectType, Integer oId,
			Integer masterId, Node channel, Map<Integer, Integer> channelSet,
			String message) throws NodeException {
		// check whether a localized copy (!= oId) exists in the channel
		int localId = ObjectTransformer.getInt(channelSet.get(channel.getId()), -1);

		if (localId != -1 && localId != ObjectTransformer.getInt(oId, -1)) {
			// a local object exists and is different than the removed copy, so
			// the master will NOT become visible in this channel (and all
			// subchannels)
			return;
		}

		// make the master visible in this channel
		unhideObject(objectType, masterId, channel.getId(), message);

		// get all subchannels, and continue there
		Collection<Node> subChannels = channel.getChannels();

		for (Node subChannel : subChannels) {
			recursiveUnhideObjects(objectType, oId, masterId, subChannel, channelSet, message);
		}
	}

	/**
	 * Unhide the given object for the given channel
	 * @param objectType object type
	 * @param objectId object id
	 * @param channelId channel id
	 * @param message message for logcmd
	 * @throws NodeException
	 */
	protected static void unhideObject(int objectType, Integer objectId, Integer channelId, String message) throws NodeException {
		// make the master visible in this channel
		ActionLogger.logCmd(ActionLogger.MC_UNHIDE, objectType, objectId, channelId, message);
		Transaction t = TransactionManager.getCurrentTransaction();

		// we get the object to unhide without multichannelling fallback, since the object id is already the correct one
		NodeObject object = t.getObject(t.getClass(objectType), objectId, false, false, true);
		if (object != null) {
			PublishQueue.dirtObject(object, Action.UNHIDE, ObjectTransformer.getInt(channelId, 0));
			object.triggerEvent(new DependencyObject(object), null, Events.REVEAL, 1, channelId);
		}
	}

	/**
	 * Get the current channel for multichannelling or null
	 * @return current channel or null
	 * @throws NodeException
	 */
	protected Node getCurrentChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			return t.getChannel();
		} else {
			return null;
		}
	}

	/**
	 * Get the udate from the given data row
	 * @param rs data row
	 * @return udate
	 */
	protected int getUdate(FactoryDataRow rs) {
		return rs.getInt("udate");
	}

	/**
	 * Get the GlobalId from the given data row
	 * @param rs data row
	 * @return globalId (may be null)
	 */
	protected GlobalId getGlobalId(FactoryDataRow rs) {
		String uuid = rs.getString("uuid");
		GlobalId globalId = null;

		if (!ObjectTransformer.isEmpty(uuid)) {
			globalId = new GlobalId(uuid);
		}
		return globalId;
	}

	/**
	 * Get the effective udate for the dicuser entries of the given output user id
	 * @param outputUserId output user id
	 * @return maximum udate
	 * @throws NodeException
	 */
	public int getEffectiveOutputUserUdate(int outputUserId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			pst = t.prepareStatement("SELECT MAX(udate) udate FROM dicuser WHERE output_id = ?");
			pst.setInt(1, outputUserId);
			res = pst.executeQuery();

			if (res.next()) {
				return res.getInt("udate");
			} else {
				return -1;
			}
		} catch (SQLException e) {
			throw new NodeException("Error while getting dicuser udate", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#isVersioningSupported(java.lang.Class)
	 */
	public boolean isVersioningSupported(Class<? extends NodeObject> clazz) {
		if (clazz != null && PublishableNodeObject.class.isAssignableFrom(clazz)) {
			return true;
		}
		// default implementation does not support versioning
		return false;
	}

	@Override
	public void updateNonVersionedData(NodeObject versionedObject, NodeObject currentVersion) throws NodeException {
		if (currentVersion != null && FACTORY_DATA_INFO.containsKey(currentVersion.getTType())) {
			Map<String, Object> unversionedData = getDataMap(currentVersion, true);
			if (!unversionedData.isEmpty()) {
				setDataMap(versionedObject, unversionedData, true);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.BatchObjectFactory#batchLoadVersionedObjects(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class, java.util.Map)
	 */
	public <T extends NodeObject> Set<T> batchLoadVersionedObjects(FactoryHandle handle, Class<T> clazz, Class<? extends NodeObject> mainClazz, Map<Integer, Integer> timestamps) throws NodeException {
		// empty default implementation
		return Collections.emptySet();
	}

	/**
	 * Get versioned data by executing the given sql statement.
	 * The part <code> (?, ?, ...)</code> will be appended to the given sql statement (having the correct amount of bind parameters).
	 * The sql statement must select all data from a <code>_nodeversion</code> table (for the main objects specified by the timestamps parameter) and must also select the id of the "main" object to which the record belongs with alias <code>gentics_obj_id</code>
	 * @param sqlStatement sql statement which must end with <code>IN</code>, the part <code> (?, ?, ...)</code> will be appended
	 * @param timestamps map of version timestamps. Keys are the id's of the main objects, values are the version timestamps
	 * @return nested maps containing the data rows holding the version data. Keys of the outer map are the main object id's, keys of the inner maps are the id's of the versioned records.
	 * @throws NodeException
	 */
	protected Map<Integer, Map<Integer, FactoryDataRow>> getVersionedData(String sqlStatement, final Map<Integer, Integer> timestamps)
			throws NodeException {
		if (ObjectTransformer.isEmpty(timestamps)) {
			return Collections.emptyMap();
		}
		final Map<Integer, Map<Integer, FactoryDataRow>> versionData = new HashMap<Integer, Map<Integer, FactoryDataRow>>();

		StringBuilder sql = new StringBuilder(sqlStatement).append(" (").append(StringUtils.repeat("?", timestamps.size(), ",")).append(")");

		DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int paramCounter = 1;
				for (Integer id : timestamps.keySet()) {
					stmt.setInt(paramCounter++, id);
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					int objId = rs.getInt("gentics_obj_id");
					int recId = rs.getInt("id");
					int rowTimestamp = rs.getInt("nodeversiontimestamp");
					int removeTimestamp = rs.getInt("nodeversionremoved");
					int timestamp = timestamps.get(objId);

					// ignore entries that are removed or are too young
					if (removeTimestamp > 0 || rowTimestamp > timestamp) {
						continue;
					}
					Map<Integer, FactoryDataRow> objData = versionData.get(objId);
					if (objData == null) {
						objData = new HashMap<Integer, FactoryDataRow>();
						versionData.put(objId, objData);
					}
					FactoryDataRow stored = objData.get(recId);
					if (stored != null) {
						int storedTimestamp = stored.getInt("nodeversiontimestamp");
						if (rowTimestamp > storedTimestamp) {
							objData.put(recId, new FactoryDataRow(rs));
						}
					} else {
						objData.put(recId, new FactoryDataRow(rs));
					}
				}
			}
		});

		return versionData;
	}

	/**
	 * Move the given object to the target folder
	 * @param object object (may be a file, image or page)
	 * @param target target folder
	 * @param targetChannelId target channel id (0 to move into the master node)
	 * @param omitMCExclusions true to omit updating mc exclusions and disinheriting. This can be used to move language variants.
	 * @return operation result
	 * @throws NodeException
	 */
	protected static OpResult moveObject(Disinheritable<? extends NodeObject> object, Folder target, int targetChannelId, boolean omitMCExclusions) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		String tableName = null;
		String singular = null;
		String plural = null;
		String folderProperty = null;

		if (object instanceof ContentFile) {
			tableName = "contentfile";
			singular = "file";
			plural = "files";
			if (((ContentFile) object).isImage()) {
				folderProperty = "images";
			} else {
				folderProperty = "files";
			}
		} else if (object instanceof Page) {
			tableName = "page";
			singular = "page";
			plural = "pages";
			folderProperty = "pages";
		} else {
			throw new NodeException("Moving is only implemented for files, images and pages");
		}

		Class<? extends NodeObject> objectClass = object.getObjectInfo().getObjectClass();

		// === Check for data validity

		if (target == null) {
			return OpResult.fail(objectClass, "op.no_targetfolder");
		}

		// get the master folder of the target
		if (!target.isMaster()) {
			target = target.getMaster();
		}

		final int targetFolderId = ObjectTransformer.getInt(target.getId(), 0);

		Node targetChannel = null;
		if (targetChannelId > 0) {
			targetChannel = t.getObject(Node.class, targetChannelId);
			if (targetChannel == null) {
				return OpResult.fail(objectClass, "move.missing_targetchannel");
			}
			if (MultichannellingFactory.getChannelVariant(target, targetChannel) == null) {
				return OpResult.fail(objectClass, "move.target_invisible");
			}
		}

		if (!object.isMaster()) {
			return OpResult.fail(Folder.class, "move.no_master");
		}

		// === Check whether something actually needs to be done
		Node channelOrNode = Optional.ofNullable(object.getChannel()).orElse(object.getOwningNode());
		NodeObject currentParent;
		try (NoMcTrx noMcTrx = new NoMcTrx()) {
			currentParent = object.getParentObject();
		}
		if (Objects.equals(target, currentParent) && (targetChannel == null || Objects.equals(targetChannel, channelOrNode))) {
			return OpResult.OK;
		}

		// === Check for sufficient permissions
		try (ChannelTrx trx = new ChannelTrx(t.getObject(Node.class, targetChannelId))) {
			if (!t.canCreate(target, objectClass, null)) {
				return OpResult.fail(objectClass, "object.move.target.permission", new CNI18nString(plural).toString(), I18NHelper.getName(target));
			}
		}

		// If the object can be moved out of its source folder depends on a feature.
		// Either the delete (standard) or the edit permission is needed on the source folder.
		boolean canMoveSourcePermission = false;
		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MOVE_PERM_WITH_EDIT)) {
			if (t.canEdit(object)) {
				canMoveSourcePermission = true;
			}
		} else {
			// This is the standard behavior
			if (t.canDelete(object)) {
				canMoveSourcePermission = true;
			}
		}

		NodeObject parentObject = object.getParentObject();

		if (!canMoveSourcePermission) {
			return OpResult.fail(objectClass, "object.move.source.permission", new CNI18nString(plural).toString(),
					I18NHelper.getName(parentObject));
		}

		// get all channelset variants
		final Set<Integer> ids = new HashSet<Integer>();
		ids.add(ObjectTransformer.getInt(object.getId(), 0));
		try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
			for (Object csVariantId : object.getChannelSet().values()) {
				ids.add(ObjectTransformer.getInteger(csVariantId, 0));
			}
		}

		Node channel = object.getChannel();
		final int sourceChannelId = (channel != null ? ObjectTransformer.getInt(channel.getId(), 0) : 0);
		final int oldMotherId = ObjectTransformer.getInt(parentObject.getId(), 0);
		final int oldNodeId = ObjectTransformer.getInt(object.getOwningNode().getId(), 0);
		final int oldChannelOrNodeId = sourceChannelId != 0 ? sourceChannelId : oldNodeId;

		// === Check for localization issues
		if (!object.getOwningNode().equals(target.getOwningNode()) && ids.size() > 1) {
			NodeObject localization = null;
			for (Integer id : ids) {
				if (!id.equals(object.getId())) {
					localization = t.getObject(objectClass, id, -1, false);
					break;
				}
			}
			return OpResult.fail(objectClass, "object.move.localization.error", I18NHelper.getPath(localization));
		}

		if (targetChannel != null) {
			// include objects from the wastebin, since conflicting channel variants in the wastebin must be
			// removed
			try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
				// the object shall be moved into a channel, so it must not have a localized copy there (which is not this object)
				NodeObject targetChannelVariant = MultichannellingFactory.getChannelVariant(object, targetChannel);
				if (targetChannelVariant != null && !object.equals(targetChannelVariant)) {
					return OpResult.fail(objectClass, "object.move.localization.error", I18NHelper.getPath(targetChannelVariant));
				}

				// the object shall be moved into a channel, so it must not have localized copies in any of the channels
				// where the object will no longer be inherited
				ChannelTreeSegment newSegment = new ChannelTreeSegment(targetChannel, object.isExcluded(), object.getDisinheritedChannels());
				Set<Node> newChannels = newSegment.getAllNodes();
				for (Map.Entry<Integer, Integer> entry : object.getChannelSet().entrySet()) {
					Integer channelId = entry.getKey();
					Integer locObjId = entry.getValue();
					if (locObjId.equals(object.getId())) {
						continue;
					}
					Node locChannel = t.getObject(Node.class, channelId, -1, false);
					if (locChannel != null && !newChannels.contains(locChannel)) {
						NodeObject locObject = t.getObject(objectClass, locObjId, -1, false);
						return OpResult.fail(objectClass, "object.move.localization.error", I18NHelper.getPath(locObject));
					}
				}
			}
		}

		// check whether mc restriction of target folder allows moving the object into it
		ChannelTreeSegment targetSegment = new ChannelTreeSegment(target, true);
		Set<Node> targetNodes = targetSegment.getAllNodes();
		for (Map.Entry<Integer, Integer> entry : object.getChannelSet().entrySet()) {
			Integer channelId = entry.getKey();
			Integer locObjId = entry.getValue();
			if (locObjId.equals(object.getId())) {
				continue;
			}
			Node locChannel = t.getObject(Node.class, channelId, -1, false);
			if (locChannel != null && !targetNodes.contains(locChannel)) {
				NodeObject locObject = t.getObject(objectClass, locObjId, -1, false);
				return OpResult.fail(objectClass, "object.move.localization.error", I18NHelper.getPath(locObject));
			}
		}

		// === Compose the new visibility of the object (after it was moved to the target folder)
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(object, true);
		ChannelTreeSegment folderSegment = new ChannelTreeSegment(target, true);
		ChannelTreeSegment restrictedSegment = folderSegment.addRestrictions(objectSegment.isExcluded(), objectSegment.getRestrictions());

		// === Check for possible naming conflicts for the object
		final Folder finalTarget = target;
		Disinheritable<?> obstructor = (Disinheritable<?>) DisinheritUtils.getObjectUsingURL(object, node -> {
			return object.getFullPublishPath(finalTarget, true, true);
		}, () -> object.getFilename(), DisinheritUtils.getFoldersWithPotentialObstructors(target, restrictedSegment),
				folderSegment);
		if (obstructor != null) {
			String path = obstructor.getFullPublishPath(true) + object.getFilename();
			if (t.canView(obstructor)) {
				return OpResult.fail(objectClass, "object.move.filename", path, I18NHelper.get(singular), I18NHelper.getPath(obstructor));
			} else {
				return OpResult.fail(objectClass, "object.move.filename.noperm", path, I18NHelper.get(singular));
			}
		}

		// === Perform the move
		// set the new folder_id for the object (and all channelset variants)
		DBUtils.executeMassStatement("UPDATE " + tableName + " SET folder_id = ? WHERE id IN", null, new ArrayList<Integer>(ids), 2, new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, targetFolderId);
			}
		}, Transaction.UPDATE_STATEMENT);

		final int setChannelId = (targetChannel != null && targetChannel.isChannel() ? targetChannelId : 0);
		// set the target channel id for the object
		DBUtils.executeUpdate("UPDATE " + tableName + " SET channel_id = ? WHERE id = ?", new Object[] { setChannelId, object.getId() });

		// write logcmd entries, trigger events, clear caches
		String[] oldNodeProps = new String[] {Integer.toString(oldChannelOrNodeId)};
		ActionLogger.logCmd(ActionLogger.MOVE, object.getTType(), object.getId(), target.getId(), "Object.move()");
		t.addTransactional(new TransactionalTriggerEvent(objectClass, object.getId(), oldNodeProps, Events.MOVE));
		t.addTransactional(new TransactionalTriggerEvent(Folder.class, targetFolderId, new String[] { folderProperty }, Events.UPDATE));
		t.addTransactional(new TransactionalTriggerEvent(Folder.class, oldMotherId, new String[] { folderProperty }, Events.UPDATE));
		for (Integer id : ids) {
			t.dirtObjectCache(objectClass, id, true);
		}
		t.dirtObjectCache(Folder.class, targetFolderId, true);
		t.dirtObjectCache(Folder.class, oldMotherId, true);

		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			Disinheritable<?> movedObject = (Disinheritable<?>) t.getObject(objectClass, object.getId());

			if (!omitMCExclusions) {
				objectSegment = new ChannelTreeSegment(movedObject, true);
				restrictedSegment = folderSegment.addRestrictions(objectSegment.isExcluded(), objectSegment.getRestrictions());
				movedObject.changeMultichannellingRestrictions(restrictedSegment.isExcluded(), restrictedSegment.getRestrictions(), false);
			}

			NodeObject parent = movedObject.getParentObject();

			if (parent instanceof Folder) {
				if (((Folder) parent).isDisinheritDefault()) {
					logger.debug("Setting disinherit default flag for object {"
						+ movedObject.getName() + ", " + movedObject.getId()
						+ ") because target folder has it set");

					movedObject.setDisinheritDefault(true, true);
				}
			}
		}

		return OpResult.OK;
	}

	/**
	 * Internal class for Factory Data Fields
	 */
	public final static class FactoryDataField {

		/**
		 * Name of the field
		 */
		protected String fieldName;

		/**
		 * Field
		 */
		protected Field field;

		/**
		 * True if the field can be changed when updating a record, false if not
		 */
		protected boolean update;

		/**
		 * True if the field is unversioned in an object, that has versions
		 */
		protected boolean unversioned;

		/**
		 * True to store the value in json format
		 */
		protected boolean json;

		/**
		 * Create an instance
		 * @param fieldName field name
		 * @param field field
		 * @param update true for updateable fields
		 */
		protected FactoryDataField(Field field) {
			DataField dataFieldAnnotation = field.getAnnotation(DataField.class);
			this.fieldName = dataFieldAnnotation.value();
			this.json = dataFieldAnnotation.json();
			this.field = field;
			this.update = field.isAnnotationPresent(Updateable.class);
			this.unversioned = field.isAnnotationPresent(Unversioned.class);
		}

		/**
		 * Get the field value
		 * @param object object
		 * @return field value
		 * @throws NodeException
		 */
		public Object get(NodeObject object) throws NodeException {
			try {
				return internal2External(field.get(object), json);
			} catch (Exception e) {
				throw new NodeException("Error while getting field " + fieldName + " of " + object, e);
			}
		}

		/**
		 * Set the field value
		 * @param object object
		 * @param value field value
		 * @throws NodeException
		 */
		public void set(NodeObject object, Object value) throws NodeException {
			try {
				field.set(object, external2Internal(value, field.getType(), json));
			} catch (Exception e) {
				throw new NodeException("Error while setting field " + fieldName + " of " + object + " to " + object, e);
			}
		}

		/**
		 * Normalize the internal value (like it is stored in the NodeObject)
		 * to an external value (like it is stored in the DB or exported)
		 * @param value field value
		 * @return normalized value
		 * @throws JsonProcessingException 
		 */
		public static Object internal2External(Object value) throws JsonProcessingException {
			return internal2External(value, false);
		}

		/**
		 * Normalize the internal value (like it is stored in the NodeObject)
		 * to an external value (like it is stored in the DB or exported)
		 * @param value field value
		 * @param json true to transform to json
		 * @return normalized value
		 * @throws JsonProcessingException 
		 */
		public static Object internal2External(Object value, boolean json) throws JsonProcessingException {
			if (json) {
				return mapper.writeValueAsString(value);
			} else if (value instanceof Boolean) {
				// transform booleans to integers
				value = ((Boolean) value).booleanValue() ? 1 : 0;
			} else if (value instanceof ContentNodeDate) {
				// transform contentnodedate to the timestamp
				value = ((ContentNodeDate) value).getTimestamp();
			} else if (value != null && value.getClass().isEnum()) {
				return value.toString();
			} else if (value instanceof JsonNode) {
				return mapper.writeValueAsString(value);
			} else if (value instanceof Collection) {
				return mapper.writeValueAsString(value);
			}
			return value;
		}

		/**
		 * Normalize the external value (like it is stored in the DB or exported)
		 * to an internal value (like it is stored in the NodeObject)
		 * @param value value
		 * @param clazz field class
		 * @return normalized value
		 * @throws IOException 
		 * @throws JsonMappingException 
		 * @throws JsonParseException 
		 */
		public static Object external2Internal(Object value, Class<?> clazz) throws JsonParseException, JsonMappingException, IOException {
			return external2Internal(value, clazz, false);
		}

		/**
		 * Normalize the external value (like it is stored in the DB or exported)
		 * to an internal value (like it is stored in the NodeObject)
		 * @param value value
		 * @param clazz field class
		 * @param json true to transform from json
		 * @return normalized value
		 * @throws IOException 
		 * @throws JsonMappingException 
		 * @throws JsonParseException 
		 */
		public static Object external2Internal(Object value, Class<?> clazz, boolean json) throws JsonParseException, JsonMappingException, IOException {
			if (json) {
				value = mapper.readValue(ObjectTransformer.getString(value, "{}"), clazz);
			} else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
				// transform the value into a boolean
				value = ObjectTransformer.getBoolean(value, false);
			} else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
				// transforme the value into an int
				value = ObjectTransformer.getInt(value, 0);
			} else if (clazz.equals(ContentNodeDate.class)) {
				// transform the value int a contentnode date
				value = new ContentNodeDate(ObjectTransformer.getInt(value, 0));
			} else if (clazz.isEnum()) {
				for (Object enumConstant : clazz.getEnumConstants()) {
					if (ObjectTransformer.getString(enumConstant, "").equalsIgnoreCase(ObjectTransformer.getString(value, null))) {
						return enumConstant;
					}
				}
			} else if (List.class.isAssignableFrom(clazz)) {
				value = mapper.readValue(ObjectTransformer.getString(value, "[]"), List.class);
			} else if (Set.class.isAssignableFrom(clazz)) {
				value = mapper.readValue(ObjectTransformer.getString(value, "[]"), Set.class);
			} else if (Collection.class.isAssignableFrom(clazz)) {
				value = mapper.readValue(ObjectTransformer.getString(value, "[]"), List.class);
			} else if (clazz.equals(JsonNode.class)) {
				value = mapper.readValue(ObjectTransformer.getString(value, "{}"), JsonNode.class);
			}
			return value;
		}
	}

	/**
	 * Internal class for Factory Data Information
	 */
	protected final static class FactoryDataInformation {
		/**
		 * SQL Statement to select data
		 */
		protected String selectSQL;

		/**
		 * SQL Statement for batchloading
		 */
		protected String batchLoadSQL;

		/**
		 * SQL Statement to select versioned data
		 */
		protected String selectVersionedSQL;

		/**
		 * SQL Parameters in the {@link #selectVersionedSQL}
		 */
		protected VersionedSQLParam[] versionSQLParams;

		/**
		 * SQL Statement to insert a new record
		 */
		protected String insertSQL;

		/**
		 * List of fields to insert
		 */
		protected List<String> insertFields;

		/**
		 * SQL Statement to update a record
		 */
		protected String updateSQL;

		/**
		 * List of fields to update
		 */
		protected List<String> updateFields;

		/**
		 * Method to set the id of a new created object
		 */
		protected Method setIdMethod;

		/**
		 * List of factory data fields
		 */
		protected List<FactoryDataField> factoryDataFields;

		/**
		 * Field holding the REST model
		 */
		protected Field restModelField;

		/**
		 * True if the object has uuid and udate
		 */
		protected boolean uuid;

		/**
		 * Set the select SQL Statement
		 * @param selectSQL sql
		 * @return fluent API
		 */
		public FactoryDataInformation setSelectSQL(String selectSQL) {
			this.selectSQL = selectSQL;
			return this;
		}

		/**
		 * Set the batch load SQL Statement
		 * @param batchLoadSQL sql
		 * @return fluent API
		 */
		public FactoryDataInformation setBatchLoadSQL(String batchLoadSQL) {
			this.batchLoadSQL = batchLoadSQL;
			return this;
		}

		/**
		 * Set the select versioned SQL Statement
		 * @param selectVersionedSQL sql
		 * @return fluent API
		 */
		public FactoryDataInformation setSelectVersionedSQL(String selectVersionedSQL) {
			this.selectVersionedSQL = selectVersionedSQL;
			return this;
		}

		/**
		 * Set the parameters for select versioned SQL Statement
		 * @param versionSQLParams parameters
		 * @return fluent API
		 */
		public FactoryDataInformation setVersionSQLParams(VersionedSQLParam[] versionSQLParams) {
			this.versionSQLParams = versionSQLParams;
			return this;
		}

		/**
		 * Set the insert SQL Statement
		 * @param insertSQL sql
		 * @return fluent API
		 */
		public FactoryDataInformation setInsertSQL(String insertSQL) {
			this.insertSQL = insertSQL;
			return this;
		}

		/**
		 * Set list of fields in the insert statement
		 * @param insertFields fields
		 * @return fluent API
		 */
		public FactoryDataInformation setInsertFields(List<String> insertFields) {
			this.insertFields = insertFields;
			return this;
		}

		/**
		 * Set the update SQL Statement
		 * @param updateSQL sql
		 * @return fluent API
		 */
		public FactoryDataInformation setUpdateSQL(String updateSQL) {
			this.updateSQL = updateSQL;
			return this;
		}

		/**
		 * Set list of fields in the update statement
		 * @param updateFields fields
		 * @return fluent API
		 */
		public FactoryDataInformation setUpdateFields(List<String> updateFields) {
			this.updateFields = updateFields;
			return this;
		}

		/**
		 * Set list of factory data fields
		 * @param factoryDataFields data fields
		 * @return fluent API
		 */
		public FactoryDataInformation setFactoryDataFields(List<FactoryDataField> factoryDataFields) {
			this.factoryDataFields = factoryDataFields;
			return this;
		}

		/**
		 * Set optional field holding the REST Model
		 * @param restModelField field
		 * @return fluent API
		 */
		public FactoryDataInformation setRestModelField(Field restModelField) {
			this.restModelField = restModelField;
			return this;
		}

		/**
		 * Set Method to set the ID
		 * @param setIdMethod method
		 * @return fluent API
		 */
		public FactoryDataInformation setSetIdMethod(Method setIdMethod) {
			this.setIdMethod = setIdMethod;
			return this;
		}

		/**
		 * Set true if the object has uuid and udate
		 * @param uuid flag
		 * @return fluent API
		 */
		public FactoryDataInformation setUuid(boolean uuid) {
			this.uuid = uuid;
			return this;
		}

		/**
		 * Set the id to the object
		 * @param object object
		 * @param id id
		 * @throws NodeException
		 */
		public void setId(NodeObject object, Integer id) throws NodeException {
			try {
				setIdMethod.invoke(object, id);
			} catch (Exception e) {
				throw new NodeException("Error while setting id of object", e);
			}
		}

		/**
		 * Get the insert data from the object
		 * @param object object
		 * @return array of insert data objects
		 * @throws NodeException
		 */
		public Object[] getInsertData(NodeObject object) throws NodeException {
			Map<String, Object> dataMap = getDataMap(object);
			if (uuid) {
				dataMap.put("uuid", ObjectTransformer.getString(dataMap.get("uuid"), ObjectTransformer.getString(object.getGlobalId(), "")));
			}
			List<Object> data = insertFields.stream().map(field -> dataMap.get(field)).collect(Collectors.toList());
			return (Object[]) data.toArray(new Object[data.size()]);
		}

		/**
		 * Get the update data from the object
		 * @param object object
		 * @return array containing the update data
		 * @throws NodeException
		 */
		public Object[] getUpdateData(NodeObject object) throws NodeException {
			Map<String, Object> dataMap = getDataMap(object);

			List<Object> data = updateFields.stream().map(field -> dataMap.get(field)).collect(Collectors.toList());

			// finally add the id
			data.add(object.getId());
			return (Object[]) data.toArray(new Object[data.size()]);
		}
	}
}
