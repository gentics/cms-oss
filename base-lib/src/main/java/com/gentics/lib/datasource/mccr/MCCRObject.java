package com.gentics.lib.datasource.mccr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.StreamingResolvable;
import com.gentics.lib.content.FilesystemAttributeValue;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.etc.StringUtils;

/**
 * Implementation of an object stored in a {@link MCCRDatasource}
 */
public class MCCRObject implements Changeable, StreamingResolvable, Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1342127012853831684L;

	/**
	 * Datasource instance this object was fetched from
	 */
	protected transient MCCRDatasource ds;

	/**
	 * Internal ID of the object. If this is set > 0, the object is persisted. if == 0, we don't know (yet), if < 0, the object does not exist.
	 */
	protected int id = 0;

	/**
	 * Channel ID of the object
	 */
	protected int channelId;

	/**
	 * Channelset ID of the object
	 */
	protected int channelsetId;

	/**
	 * Content ID
	 */
	protected ContentId contentId;

	/**
	 * Currently stored Object ID. This will be set, if the object is created with a channelsetId and objId, while the stored objId is different (because the object was
	 * inherited and is not localized or vice-versa)
	 */
	protected int storedObjId;

	/**
	 * Attribute values
	 */
	protected transient Map<String, Object> attributes = new HashMap<String, Object>();

	/**
	 * Fixed updatetimestamp (if one is set)
	 */
	protected Integer updateTimestamp;

	/**
	 * Create an instance of an object
	 * @param ds datasource
	 * @param channelId channel id
	 * @param channelsetId channelset id
	 * @param contentId contentId
	 */
	protected MCCRObject(MCCRDatasource ds, int channelId, int channelsetId, ContentId contentId) {
		this.ds = ds;
		this.channelId = channelId;
		this.channelsetId = channelsetId;
		if (contentId != null) {
			this.contentId = new ContentId(contentId.objType, contentId.objId);
		} else {
			this.contentId = new ContentId(0, 0);
		}
	}

	/**
	 * Create a clone of the given object for the given datasource
	 * @param ds datasource
	 * @param obj object to clone
	 * @throws DatasourceException
	 */
	protected MCCRObject(MCCRDatasource ds, MCCRObject obj) throws DatasourceException {
		this(ds, obj, true);
	}

	/**
	 * Create a clone of the given object for the given datasource
	 * @param ds datasource
	 * @param obj object to clone
	 * @param cloneAttributes true if attributes shall be cloned, false if not
	 * @throws DatasourceException
	 */
	protected MCCRObject(MCCRDatasource ds, MCCRObject obj, boolean cloneAttributes) throws DatasourceException {
		this.ds = ds;
		// also clone the id, if the datasources are equal
		// otherwise the clone goes into another datasource which will have its own ids
		if (this.ds.equals(obj.ds)) {
			this.id = obj.id;
		}
		this.channelId = obj.channelId;
		this.channelsetId = obj.channelsetId;
		this.contentId = obj.contentId;

		if (cloneAttributes) {
			for (String attrName : obj.attributes.keySet()) {
				if (isStreamable(attrName) && obj.isStreamable(attrName)) {
					String basePath = obj.ds.getAttributePath();
					List<FilesystemAttributeValue> oldValues = MCCRHelper.getFSValues(obj, attrName);
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
					try {
						if (newValues.size() == 0) {
							setProperty(attrName, null);
						} else if (newValues.size() == 1) {
							setProperty(attrName, newValues.get(0));
						} else {
							setProperty(attrName, newValues);
						}
					} catch (InsufficientPrivilegesException e) {
						throw new DatasourceException("Error while cloning object " + obj, e);
					}
				} else {
					this.attributes.put(attrName, obj.attributes.get(attrName));
				}
			}
		}

		// set the update timestamp after the attributes were read, because setProperty() will change the update timestamp
		this.updateTimestamp = obj.updateTimestamp;
	}

	/**
	 * Get the object with given id. This will immediately check for existence of the object and throw an exception, if the object does not exist
	 * @param ds datasource
	 * @param id id
	 */
	protected MCCRObject(MCCRDatasource ds, int id) {
		this.ds = ds;
		this.id = id;
		this.contentId = new ContentId(0, 0);
	}

	/**
	 * Create an instance with the data from the given result row
	 * @param ds datasource
	 * @param row row containing the data
	 * @throws DatasourceException
	 */
	protected MCCRObject(MCCRDatasource ds, SimpleResultRow row) throws DatasourceException {
		this.ds = ds;
		this.id = row.getInt("id");
		this.channelId = row.getInt("channel_id");
		this.channelsetId = row.getInt("channelset_id");
		this.contentId = new ContentId(row.getInt("obj_type"), row.getInt("obj_id"));
		this.updateTimestamp = row.getInt("updatetimestamp");
		MCCRHelper.initializeQuickAttributes(this, row);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		if (StringUtils.isEmpty(key)) {
			return null;
		} else if ("contentid".equals(key)) {
			return contentId.contentId;
		} else if ("obj_type".equals(key)) {
			return contentId.objType;
		} else if ("obj_id".equals(key)) {
			return contentId.objId;
		} else if ("channel_id".equals(key)) {
			return channelId;
		} else if ("channelset_id".equals(key)) {
			return channelsetId;
		} else if ("updatetimestamp".equals(key)) {
			return updateTimestamp;
		} else {
			if (!attributes.containsKey(key)) {
				Object fromDataMap;
				try {
					fromDataMap = MCCRHelper.getFromDataMap(this, key);
					if (fromDataMap != null) {
						return MCCRHelper.normalizeValueForOutput(ds, this, MCCRHelper.getAttributeType(ds, contentId.objType, key), fromDataMap, false);
					}
				} catch (DatasourceException e) {
				}

				try {
					MCCRHelper.initializeAttribute(this, key);
				} catch (DatasourceException e) {
					MCCRDatasource.logger.error("Error while reading attribute {" + key + "} for {" + this + "}", e);
				}
			}
			try {
				return MCCRHelper.normalizeValueForOutput(ds, this, MCCRHelper.getAttributeType(ds, contentId.objType, key), attributes.get(key), false);
			} catch (DatasourceException e) {
				return attributes.get(key);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Changeable#setProperty(java.lang.String, java.lang.Object)
	 */
	public boolean setProperty(String name, Object value) throws InsufficientPrivilegesException {
		try {
			ObjectAttributeBean type = MCCRHelper.getAttributeType(ds, contentId.objType, name);
			if (type != null) {
				setProperty(type, value);
				return true;
			} else {
				MCCRHelper.logger.error("Error while setting value for attribute {" + name + "} for {" + this + "}: Attribute does not exist");
				return false;
			}
		} catch (DatasourceException e) {
			MCCRHelper.logger.error("Error while setting value for attribute {" + name + "} for {" + this + "}", e);
			return false;
		}
	}

	/**
	 * Set the property for the given attribute type
	 * @param type type
	 * @param value value
	 */
	protected void setProperty(ObjectAttributeBean type, Object value) throws DatasourceException {
		attributes.put(type.getName(), MCCRHelper.normalizeValueForStoring(ds, this, type, value, false));
	}

	/**
	 * Check whether the object exists
	 * @return true if the object exists, false if it doesn't
	 * @throws DatasourceException
	 */
	public boolean exists() throws DatasourceException {
		return MCCRHelper.initialize(this);
	}

	@Override
	public String toString() {
		return contentId.toString();
	}

	/**
	 * Set the update timestamp for the object
	 * @param updateTimestamp update timestamp
	 */
	public void setUpdateTimestamp(int updateTimestamp) {
		this.updateTimestamp = updateTimestamp;
	}

	/**
	 * Get the update timestamp for the object
	 * @return update timestamp for the object
	 */
	public int getUpdateTimestamp() {
		if (updateTimestamp != null) {
			return updateTimestamp.intValue();
		} else {
			return (int) (System.currentTimeMillis() / 1000);
		}
	}

	/**
	 * Get the internal id
	 * @return internal id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get the channel id
	 * @return channel id
	 */
	public int getChannelId() {
		return channelId;
	}

	/**
	 * Get the channelset id
	 * @return channelset id
	 */
	public int getChannelSetId() {
		return channelsetId;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#getStreamableProperties()
	 */
	public Collection<String> getStreamableProperties() {
		// all filesystem attributes are streamable
		try {
			Collection<ObjectAttributeBean> attrTypes = MCCRHelper.getAttributeTypes(ds, contentId.objType);
			Collection<String> streamable = new Vector<String>();

			for (ObjectAttributeBean attrType : attrTypes) {
				if (attrType.isFilesystem()) {
					streamable.add(attrType.getName());
				}
			}
			return streamable;
		} catch (DatasourceException e) {
			MCCRDatasource.logger.error("Error while getting filesystem attributes for type " + contentId.objType, e);
			return Collections.emptyList();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#isStreamable(java.lang.String)
	 */
	public boolean isStreamable(String name) {
		try {
			ObjectAttributeBean attrType = MCCRHelper.getAttributeType(ds, contentId.objType, name);

			if (attrType == null) {
				return false;
			} else {
				return attrType.isFilesystem();
			}
		} catch (DatasourceException e) {
			MCCRDatasource.logger.error("Error while checking attribute " + name + " for type " + contentId.objType, e);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#getNumStreams(java.lang.String)
	 */
	public int getNumStreams(String name) {
		if (isStreamable(name)) {
			if (!attributes.containsKey(name)) {
				try {
					MCCRHelper.initializeAttribute(this, name);
				} catch (DatasourceException e) {
					MCCRDatasource.logger.error("Error while reading attribute {" + name + "} for {" + this + "}", e);
					return 0;
				}
			}
			return ObjectTransformer.getCollection(attributes.get(name), Collections.emptyList()).size();
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#getInputStream(java.lang.String, int)
	 */
	public InputStream getInputStream(String name, int n) throws IOException, ArrayIndexOutOfBoundsException {
		if (isStreamable(name)) {
			try {
				if (!attributes.containsKey(name)) {
					MCCRHelper.initializeAttribute(this, name);
				}
				List<FilesystemAttributeValue> fsValues = MCCRHelper.getFSValues(this, name);

				return fsValues.get(n).getInputStream(ds.getHandle(), ds.getAttributePath(), this);
			} catch (DatasourceException e) {
				MCCRDatasource.logger.error("Error while reading attribute {" + name + "} for {" + this + "}", e);
				throw new IOException("Error while reading attribute {" + name + "} for {" + this + "}");
			}
		} else {
			throw new IOException("Attribute " + name + " cannot be streamed for " + this);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return contentId.equals(obj);
	}

	@Override
	public int hashCode() {
		return contentId.hashCode();
	}
}
