package com.gentics.lib.datasource.mccr;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.auth.GenticsUser;
import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceInfo;
import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.datasource.WritableMultichannellingDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.datasource.NumDatasourceInfo;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;

/**
 * Implementation of the writeable multichannelling aware datasource
 */
public class WritableMCCRDatasource extends MCCRDatasource implements WritableMultichannellingDatasource {

	/**
	 * Update timestamp on Write
	 */
	protected ThreadLocal<Boolean> updateTimestampOnWrite = new ThreadLocal<Boolean>();

	/**
	 * Create an instance of this datasource
	 * @param id datasource id
	 * @param pool handle pool
	 * @param parameters init parameters
	 */
	public WritableMCCRDatasource(String id, HandlePool pool, Map<String, String> parameters) {
		super(id, pool, parameters);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#canWrite()
	 */
	public boolean canWrite() {
		return true;
	}

	// old methods, which are not implemented

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#store(com.gentics.api.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo store(DatasourceRecordSet rst) throws DatasourceException {
		throw new DatasourceException("This method is not implemented, use store(Collection) instead");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#store(com.gentics.api.lib.datasource.DatasourceRecordSet, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo store(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		throw new DatasourceException("This method is not implemented, use store(Collection) instead");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#update(com.gentics.api.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo update(DatasourceRecordSet rst) throws DatasourceException {
		throw new DatasourceException("This method is not implemented, use update(Collection) instead");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#update(com.gentics.api.lib.datasource.DatasourceRecordSet, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo update(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		throw new DatasourceException("This method is not implemented, use update(Collection) instead");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#insert(com.gentics.api.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo insert(DatasourceRecordSet rst) throws DatasourceException {
		throw new DatasourceException("This method is not implemented, use insert(Collection) instead");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#insert(com.gentics.api.lib.datasource.DatasourceRecordSet, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo insert(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		throw new DatasourceException("This method is not implemented, use insert(Collection) instead");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#delete(com.gentics.api.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo delete(DatasourceRecordSet rst) throws DatasourceException {
		throw new DatasourceException("This method is not implemented, use delete(Collection) instead");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#delete(com.gentics.api.lib.datasource.DatasourceRecordSet, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo delete(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		throw new DatasourceException("This method is not implemented, use delete(Collection) instead");
	}

	// convenience methods

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#store(java.util.Collection, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo store(@SuppressWarnings("rawtypes") Collection objects, GenticsUser user) throws DatasourceException {
		return store(objects);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#update(java.util.Collection, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo update(@SuppressWarnings("rawtypes") Collection objects, GenticsUser user) throws DatasourceException {
		return update(objects);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#insert(java.util.Collection, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo insert(@SuppressWarnings("rawtypes") Collection objects, GenticsUser user) throws DatasourceException {
		return insert(objects);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#delete(java.util.Collection, com.gentics.api.lib.auth.GenticsUser)
	 */
	public DatasourceInfo delete(@SuppressWarnings("rawtypes") Collection objects, GenticsUser user) throws DatasourceException {
		return delete(objects);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#delete(com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter)
	 */
	public DatasourceInfo delete(DatasourceFilter filter) throws DatasourceException {
		return delete(getResult(filter, null));
	}

	// actual implemented methods

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#store(java.util.Collection)
	 */
	public DatasourceInfo store(@SuppressWarnings("rawtypes") Collection objects) throws DatasourceException {
		if (ObjectTransformer.isEmpty(objects)) {
			return new NumDatasourceInfo(0);
		}

		Collection<MCCRObject> toInsert = new Vector<MCCRObject>();
		Collection<MCCRObject> toUpdate = new Vector<MCCRObject>();

		for (Object o : objects) {
			if (!(o instanceof MCCRObject)) {
				if (o != null) {
					// log an warning, that we omit an object
					logger.warn("Omiting object of " + o.getClass() + " while storing objects");
				}
			} else {
				MCCRObject mccrObject = (MCCRObject) o;

				if (!equals(mccrObject.ds)) {
					mccrObject = new MCCRObject(this, mccrObject);
				}
				if (mccrObject.exists()) {
					toUpdate.add(mccrObject);
				} else {
					toInsert.add(mccrObject);
				}
			}
		}

		// insert new objects
		DatasourceInfo insertInfo = insert(toInsert);
		// update existing objects
		DatasourceInfo updateInfo = update(toUpdate);

		return new NumDatasourceInfo(insertInfo.getAffectedRecordCount() + updateInfo.getAffectedRecordCount());
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#update(java.util.Collection)
	 */
	public DatasourceInfo update(@SuppressWarnings("rawtypes") Collection objects) throws DatasourceException {
		if (ObjectTransformer.isEmpty(objects)) {
			return new NumDatasourceInfo(0);
		}

		Collection<MCCRObject> batchUpdate = new ArrayList<MCCRObject>(objects.size());
		for (Object o : objects) {
			if (!(o instanceof MCCRObject)) {
				if (o != null) {
					// log an warning, that we omit an object
					logger.warn("Omiting object of " + o.getClass() + " while updating objects");
				}
			} else {
				MCCRObject mccrObj = (MCCRObject) o;

				if (!equals(mccrObj.ds)) {
					mccrObj = new MCCRObject(this, mccrObj);
				}
				batchUpdate.add(mccrObj);
			}
		}

		if (batchUpdate.size() == 1) {
			MCCRObject mccrObj = batchUpdate.iterator().next();
			if (MCCRHelper.update(mccrObj)) {
				if (isUpdateTimestampOnWrite()) {
					setLastUpdate(mccrObj.channelId);
				}
				return new NumDatasourceInfo(1);
			} else {
				return new NumDatasourceInfo(0);
			}
		} else {
			Map<Integer, Integer> updateCountPerChannel = MCCRHelper.update(this, batchUpdate);
			int totalUpdateCount = 0;
			for (Map.Entry<Integer, Integer> entry : updateCountPerChannel.entrySet()) {
				int updateCount = entry.getValue();
				totalUpdateCount += updateCount;
				// set the updatetimestamp to the channel
				if (isUpdateTimestampOnWrite() && updateCount > 0) {
					int channelId = entry.getKey();
					setLastUpdate(channelId);
				}
			}
			return new NumDatasourceInfo(totalUpdateCount);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#insert(java.util.Collection)
	 */
	public DatasourceInfo insert(@SuppressWarnings("rawtypes") Collection objects) throws DatasourceException {
		if (ObjectTransformer.isEmpty(objects)) {
			return new NumDatasourceInfo(0);
		}

		int insertCount = 0;
		List<Integer> updatedChannels = new ArrayList<Integer>();

		Collection<MCCRObject> batchInsert = new ArrayList<MCCRObject>(objects.size());
		for (Object o : objects) {
			if (!(o instanceof MCCRObject)) {
				if (o != null) {
					// log an warning, that we omit an object
					logger.warn("Omiting object of " + o.getClass() + " while inserting objects");
				}
			} else {
				MCCRObject mccrObj = (MCCRObject) o;

				if (!equals(mccrObj.ds)) {
					mccrObj = new MCCRObject(this, mccrObj);
				}
				batchInsert.add(mccrObj);
				insertCount++;
				if (!updatedChannels.contains(mccrObj.getChannelId())) {
					updatedChannels.add(mccrObj.getChannelId());
				}
			}
		}
		// for a single object, use the normal insert, for multiple objects, use batch insert
		if (batchInsert.size() == 1) {
			MCCRHelper.insert(batchInsert.iterator().next());
		} else {
			MCCRHelper.insert(this, batchInsert, false);
		}

		// set the updatetimestamp to the channel
		if (isUpdateTimestampOnWrite() && insertCount > 0) {
			for (Integer channelId : updatedChannels) {
				setLastUpdate(channelId);
			}
		}

		return new NumDatasourceInfo(insertCount);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#delete(java.util.Collection)
	 */
	public DatasourceInfo delete(@SuppressWarnings("rawtypes") Collection objects) throws DatasourceException {
		if (ObjectTransformer.isEmpty(objects)) {
			return new NumDatasourceInfo(0);
		}

		int deleteCount = 0;
		List<Integer> updatedChannels = new ArrayList<Integer>();

		for (Object o : objects) {
			if (!(o instanceof MCCRObject)) {
				if (o != null) {
					// log an warning, that we omit an object
					logger.warn("Omiting object of " + o.getClass() + " while deleting objects");
				}
			} else {
				MCCRObject mccrObj = (MCCRObject) o;

				if (!equals(mccrObj.ds)) {
					mccrObj = new MCCRObject(this, mccrObj);
				}
				MCCRHelper.delete(mccrObj);
				deleteCount++;
				if (!updatedChannels.contains(mccrObj.getChannelId())) {
					updatedChannels.add(mccrObj.getChannelId());
				}
			}
		}

		// set the updatetimestamp to the channel
		if (isUpdateTimestampOnWrite() && deleteCount > 0) {
			for (Integer channelId : updatedChannels) {
				setLastUpdate(channelId);
			}
		}

		return new NumDatasourceInfo(deleteCount);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#create(java.util.Map)
	 */
	public Changeable create(@SuppressWarnings("rawtypes") Map objectParameters) throws DatasourceException {
		// first check whether a channel has been set
		// TODO don't rely on the current set channel, but expect the channel to be set as parameter.
		// FInd all callers to this method and change to add the channel as object parameter
		int channelId = ObjectTransformer.getInt(objectParameters.get(WritableMultichannellingDatasource.MCCR_CHANNEL_ID), 0);

		if (channelId == 0) {
			throw new DatasourceException("Cannot create an object without setting a channelId in the parameters (with key " + MCCR_CHANNEL_ID + ")");
		}

		// next, we need a contentid and a channelsetid
		if (ObjectTransformer.isEmpty(objectParameters) || ObjectTransformer.isEmpty(objectParameters.get("contentid"))
				|| ObjectTransformer.getInt(objectParameters.get(WritableMultichannellingDatasource.MCCR_CHANNELSET_ID), 0) <= 0) {
			throw new DatasourceException("Cannot create an object without contentid or channelsetid");
		}

		// validate the contentid
		ContentId contentId = new ContentId(ObjectTransformer.getString(objectParameters.get("contentid"), ""));
		int channelsetId = ObjectTransformer.getInt(objectParameters.get(WritableMultichannellingDatasource.MCCR_CHANNELSET_ID), 0);

		// TODO check for valid object type

		// TODO check for inconsistent channelset_id/contentid (object with contentid already exists, but has different channelset_id)


		// try the cache first
		MCCRObject object = MCCRCacheHelper.getByChannelsetId(this, channelsetId, channelId);
		if (object == null) {
            object = new MCCRObject(this, channelId, channelsetId, contentId);
		} else {
			// if the cached object happens to have a different contentid, we modify it now
			if (!contentId.equals(object.contentId)) {
				object.storedObjId = object.contentId.objId;
				object.contentId = contentId;
			}
		}

		// set attribute values
		Collection<ObjectAttributeBean> attrTypes = MCCRHelper.getAttributeTypes(this, contentId.objType);

		for (ObjectAttributeBean attrType : attrTypes) {
			if (objectParameters.containsKey(attrType.getName())) {
				object.setProperty(attrType, objectParameters.get(attrType.getName()));
			}
		}

		return object;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WritableMultichannellingDatasource#saveChannelStructure(com.gentics.api.lib.datasource.ChannelTree)
	 */
	public void saveChannelStructure(ChannelTree root) throws DatasourceException {
		MCCRHelper.saveChannelStructure(this, root);
		// renew the channel selection map
		getChannelSelectionMap(true);
	}

	/**
	 * Check whether the updatetimestamp shall be modified when writing
	 * @return true for updating, false if not
	 */
	public boolean isUpdateTimestampOnWrite() {
		return ObjectTransformer.getBoolean(this.updateTimestampOnWrite.get(), true);
	}

	/**
	 * Set whether the updatetimestamp shall be modified when writing
	 * @param updateTimestampOnWrite true to update, false if not
	 */
	public void setUpdateTimestampOnWrite(boolean updateTimestampOnWrite) {
		if (updateTimestampOnWrite) {
			this.updateTimestampOnWrite.remove();
		} else {
			this.updateTimestampOnWrite.set(Boolean.FALSE);
		}
	}

	/**
	 * Set the updatetimestamp for the current channel to the current system time
	 * @throws DatasourceException
	 */
	public void setLastUpdate(int channelId) throws DatasourceException {
		setLastUpdate(channelId, System.currentTimeMillis() / 1000);
	}

	/**
	 * Set the updatetimestamp for the current channel to the given timestamp
	 * @param timestamp
	 * @throws DatasourceException
	 */
	public void setLastUpdate(int channelId, long timestamp) throws DatasourceException {
		DBHandle handle = getHandle();

		try {
			DB.update(handle, "UPDATE " + handle.getChannelName() + " SET updatetimestamp = ? WHERE id = ?", new Object[] { timestamp, channelId});
		} catch (SQLException e) {
			throw new DatasourceException("Error while setting updatetimestamp", e);
		}
	}
}
