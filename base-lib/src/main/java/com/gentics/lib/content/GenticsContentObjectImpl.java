/*
 * @author Erwin Mascher (e.mascher@gentics.com)
 * @date 20.10.2003
 * @version $Id: GenticsContentObjectImpl.java,v 1.2.2.1 2011-04-07 09:57:53 norbert Exp $
 */
package com.gentics.lib.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.datasource.VersioningDatasource;
import com.gentics.api.lib.datasource.VersioningDatasource.Version;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.DatatypeHelper.AttributeType;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.VersionedObject;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.etc.MiscUtils;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;
import com.gentics.lib.parser.rule.DefaultRuleTree;
import com.gentics.lib.resolving.FilterableResolvable;
import com.gentics.lib.util.FileUtil;

/**
 * default implementation of the GenticsContentObject
 */
public class GenticsContentObjectImpl implements GenticsContentObject, Comparable, Cloneable, Serializable, FilterableResolvable {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -2125680413788121244L;

	/**
	 * Maximum number of objects, which are prefetched in a single statement for
	 * mssql server
	 */
	protected final static int MAX_PREFETCH_SIZE_MSSQL = 2000;

	/**
	 * Maximum number of objects, which are prefetched in a single statement for
	 * oracle db server. The oracle db / jdbc driver currently only supports
	 * upto 2^16 prepared statement arguments. This limit is used to splitup the
	 * statements.
	 */
	protected static int MAX_PREFETCH_SIZE_ORACLE = 60000;

	private int m_type, m_id, m_mother_type, m_mother_id, m_updateTimestamp;

	private String m_contentId;
	private String m_mother_contentId;

	private boolean m_isInitialized = false;

	private boolean m_exists = false;

	private HashMap m_attributeHash = new HashMap(10);

	private transient DBHandle handle;

	private transient Datasource datasource;

	private static int PREFETCH_MULTIVALUE_SIZE = 5;

	private static int PREFETCH_ATTRIBUTE_SIZE = 5;

	/**
	 * version timestamp of the object, -1 for the current object
	 */
	private int versionTimestamp = -1;

	/**
	 * collection holding the names of the modified attributes
	 */
	private Collection modifiedAttributeNames;

	/**
	 * An eventually set custom updatetimestamp (or -1 if non set)
	 */
	private int customUpdatetimestamp = -1;

	/**
	 * the logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(GenticsContentObjectImpl.class);

	public DBHandle getDBHandle() {
		return handle;
	}

	/**
	 * Initialize the object instance
	 * @param handle db handle
	 * @return true when the object exists in the content repository, false if not
	 * @throws CMSUnavailableException
	 */
	private boolean initialize(DBHandle handle) throws CMSUnavailableException {
		try {
			RuntimeProfiler.beginMark(ComponentsConstants.GENTICSCONTENTOBJECT_INITIALIZE);

			if (m_isInitialized) {
				return m_exists;
			} else if (isTemporary()) {
				return false;
			} else if (getObjectId() <= 0) {
				// no valid object id...
				return false;
			}
			try {
				SimpleResultProcessor rs = new SimpleResultProcessor();
				String sql = null;
				Object[] params = null;

				if (versionTimestamp >= 0) {
					// this is a versioned object, so get it from the
					// _nodeversion table
					sql = "SELECT * FROM " + getDBHandle().getContentMapName() + "_nodeversion WHERE contentid = ? AND "
							+ "nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM " + getDBHandle().getContentMapName() + "_nodeversion "
							+ "WHERE contentid = ? AND nodeversiontimestamp <= ? AND (nodeversionremoved = 0 OR nodeversionremoved > ?))";
					Integer timestamp = new Integer(versionTimestamp);

					params = new Object[] { m_contentId, m_contentId, timestamp, timestamp};
				} else {
					// this is not a versioned object, so get it from the main
					// table
					sql = "SELECT * FROM " + getDBHandle().getContentMapName() + " WHERE contentid = ?";
					params = new Object[] { m_contentId};
				}
				DB.query(handle, sql, params, rs);
				m_isInitialized = true;
				if (rs.size() == 0) {
					m_exists = false;
				} else {
					SimpleResultRow row = rs.getRow(1);

					m_updateTimestamp = row.getInt("updatetimestamp");
					if (StringUtils.isEmpty(m_mother_contentId)) {
						// only initialize the mother id, if it is empty (otherwise, it was probably set from outside)
						m_mother_contentId = row.getString("motherid");
						m_mother_id = row.getInt("mother_obj_id");
						m_mother_type = row.getInt("mother_obj_type");
					}
					m_exists = true;

					// the object exists, check whether the datasource has
					// autoprefetch activated
					if (datasource instanceof CNDatasource) {
						CNDatasource cnd = (CNDatasource) datasource;

						if (cnd.isAutoPrefetch()) {
							AttributeType[] autoPrefetchedAttributes = cnd.getAutoPrefetchedAttributes();

							for (int i = 0; i < autoPrefetchedAttributes.length; i++) {
								setPrefetchedAttribute(autoPrefetchedAttributes[i], row.getObject(autoPrefetchedAttributes[i].getQuickName()));
							}
						}
					}
				}

				return m_exists;
			} catch (SQLException e) {
				m_exists = false;
				m_isInitialized = false;
				throw new CMSUnavailableException("Could not initialize GenticsContentObject( " + m_contentId + "): " + e.getMessage(), e);
			}
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.GENTICSCONTENTOBJECT_INITIALIZE);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setPrefetchedAttribute(com.gentics.lib.content.DatatypeHelper.AttributeType, java.lang.Object)
	 */
	public void setPrefetchedAttribute(AttributeType attributeType, Object value) {
		GenticsContentAttribute attrib = new GenticsContentAttributeImpl(this, handle, attributeType.getName(), new Object[] { value}, attributeType.getType(),
				attributeType.isMultivalue(), attributeType.isFilesystem());

		m_attributeHash.put(attributeType.getName(), attrib);

		// cache the attribute
		try {
			GenticsContentFactory.cacheAttribute(datasource, this, attrib);
		} catch (Exception e) {
			NodeLogger.getNodeLogger(getClass()).warn("Error while putting attribute into cache", e);
		}
	}

	/**
	 * Check whether the object is temporary (has a temporary contentid)
	 * @return true when the object is temporary, false if not
	 */
	protected boolean isTemporary() {
		return GenticsContentFactory.isTemporary(m_contentId);
	}

	/**
	 * NOTE: two object may be equal, but if they fetched their attributes at
	 * different times, they may actually return different values for the same
	 * attribute!
	 */

	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			// EQUAL objects are always equal ;-)
			return true;
		}

		if (o instanceof GenticsContentObjectImpl) {
			GenticsContentObjectImpl co = (GenticsContentObjectImpl) o;

			return m_contentId.equals(co.m_contentId);
		} else if (o instanceof String) {
			return ((String) o).equals(getContentId());
		} else {
			return false;
		}

		// note: we don't go the expensive route of comparing all cached
		// attributes;
	}

	public void setAttributeNeedsSortorderFixed(String name) throws NodeIllegalArgumentException,
				CMSUnavailableException {
		boolean isMulti = DatatypeHelper.isMultivalue(handle, name);

		if (isMulti) {
			GenticsContentAttribute attr = (GenticsContentAttribute) m_attributeHash.get(name);

			attr.setNeedsSortorderFixed();
		}
	}
    
	public void setAttribute(String name, Object[] values, int dataType, boolean isMulti, boolean filesystem) {
		// do not set default attributes like this
		if (DatatypeHelper.getDefaultColumnTypes().keySet().contains(name)) {
			if ("true".equals(System.getProperty("com.gentics.portalnode.datasource.allowsetobjtype")) && "obj_type".equals(name)) {// a telekom workaround (again)
			} else {
				return;
			}
		}

		GenticsContentAttribute newAttribute = new GenticsContentAttributeImpl(this, this.handle, name, values, dataType, isMulti, filesystem);

		if (!newAttribute.equals((GenticsContentAttribute) m_attributeHash.get(name))) {
			m_attributeHash.put(name, newAttribute);
			markAttributeModified(name);
		}
	}

	public void setAttribute(String name, Object[] values) throws NodeIllegalArgumentException,
				CMSUnavailableException {

		int dataType = DatatypeHelper.getDatatype(handle, name);

		boolean isMulti = DatatypeHelper.isMultivalue(handle, name);

		setAttribute(name, values, dataType, isMulti, DatatypeHelper.isFilesystem(handle, name));

	}

	public void setAttribute(String name, Object value, int dataType, boolean isMultivalue, boolean filesystem) {
		// do not set default attributes like this
		if (DatatypeHelper.getDefaultColumnTypes().keySet().contains(name) // TA obj_type hack
				&& !((datasource instanceof CNWriteableDatasource && ((CNWriteableDatasource) datasource).getAllowSettingObjType() && "obj_type".equals(name)))) {
			return;
		}

		// TODO Why can Attr only constructed with Object[]
		if (isMultivalue || dataType == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
			Collection<?> valueCollection = ObjectTransformer.getCollection(value, null);
			Object[] valueArray = null;

			if (!ObjectTransformer.isEmpty(valueCollection)) {
				valueArray = valueCollection.toArray(new Object[valueCollection.size()]);
			}

			GenticsContentAttribute newAttribute = new GenticsContentAttributeImpl(this, this.handle, name, valueArray, dataType, isMultivalue, filesystem);

			if (!newAttribute.equals((GenticsContentAttribute) m_attributeHash.get(name))) {
				m_attributeHash.put(name, newAttribute);
				markAttributeModified(name);
			}
		} else {
			Object[] safevalue;

			if (value instanceof Collection) {
				Collection colValue = (Collection) value;

				if (colValue.size() > 0) {
					safevalue = new Object[] { colValue.iterator().next()};
				} else {
					safevalue = new Object[] { null};
				}
			} else {
				safevalue = new Object[] { value};
			}

			GenticsContentAttribute newAttribute = new GenticsContentAttributeImpl(this, this.handle, name, safevalue, dataType, isMultivalue, filesystem);

			if (!newAttribute.equals((GenticsContentAttribute) m_attributeHash.get(name))) {
				m_attributeHash.put(name, newAttribute);
				markAttributeModified(name);
			}
		}
	}

	public void setAttribute(String name, Object value) throws NodeIllegalArgumentException,
				CMSUnavailableException {
		// special treatment for the attribute "versionTimestamp"
		if (VersionedObject.VERSIONTIMESTAMP_PROPERTY.equals(name)) {
			setVersionTimestamp(ObjectTransformer.getInt(value, getVersionTimestamp()));
		} else {

			int dataType = DatatypeHelper.getDatatype(handle, name);

			boolean isMulti = DatatypeHelper.isMultivalue(handle, name);

			setAttribute(name, value, dataType, isMulti, DatatypeHelper.isFilesystem(handle, name));
		}
	}

	public GenticsContentAttribute getAttribute(String name) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		if (name == null) {
			throw new NodeIllegalArgumentException("There is no attribute with {null} as name!");
		}
		GenticsContentAttributeImpl ret;

		ret = (GenticsContentAttributeImpl) m_attributeHash.get(name);
		if (ret == null) {
			// The name can ba a substring of a very large string (e.g. the
			// portaltemplate). Since we don't want to store the very large
			// string, we create a copy of the name here
			name = new String(name);

			try {
				// first try to get cached attributes
				GenticsContentAttribute cachedAttribute = GenticsContentFactory.getCachedAttribute(datasource, this, name);

				if (cachedAttribute != null) {
					m_attributeHash.put(name, cachedAttribute);
					return new GenticsContentAttributeImpl((GenticsContentAttributeImpl) cachedAttribute);
				}
			} catch (PortalCacheException e) {
				NodeLogger.getNodeLogger(getClass()).warn("Error while fetching cached attribute", e);
			}

			ret = new GenticsContentAttributeImpl(this, name);
			m_attributeHash.put(name, ret);

			// cache the attribute
			try {
				GenticsContentFactory.cacheAttribute(datasource, this, ret);
			} catch (Exception e) {
				NodeLogger.getNodeLogger(getClass()).warn("Error while putting attribute into cache", e);
			}
		}

		// always return clones with their own internal result-pointers
		return new GenticsContentAttributeImpl(ret);
	}

	// ------------------- single-value helper methods -------------------------

	public String getTextAttribute(String name) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		return getTextAttribute(name, "");
	}

	public String getTextAttribute(String name, String defaultValue) throws CMSUnavailableException, NodeIllegalArgumentException {
		GenticsContentAttribute attr = getAttribute(name);
		String ret = attr.getNextValue();

		if (ret != null) {
			return ret;
		}

		return defaultValue;
	}

	public GenticsContentObject getObjectAttribute(String name) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		GenticsContentAttribute attr = getAttribute(name);
		GenticsContentObject ret = attr.getNextContentObject();

		return ret;
	}

	/**
	 * Internal helper method to transform the value, which is supposed to be an
	 * instance of {@link FilesystemAttributeValue} into a byte[]
	 * @param attribute attribute
	 * @param value value
	 * @return data as byte[]
	 * @throws CMSUnavailableException
	 * @throws IOException
	 */
	protected byte[] getAsBinary(GenticsContentAttribute attribute, Object value) throws CMSUnavailableException, IOException {
		if (value instanceof FilesystemAttributeValue) {
			FilesystemAttributeValue fsValue = (FilesystemAttributeValue) value;
			InputStream in = fsValue.getInputStream(attribute, ((CNDatasource) getDatasource()).getAttributePath());
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			try {
				FileUtil.pooledBufferInToOut(in, out);
			} finally {
				try {
					in.close();
				} catch (IOException e) {// ignored
				}
			}
			return out.toByteArray();
		} else {
			Class<?> valueClass = null;

			if (value != null) {
				valueClass = value.getClass();
			}
			throw new CMSUnavailableException(
					"Error while reading attribute " + attribute.getAttributeName() + " for " + this + ": incompatible value found. Expected "
					+ FilesystemAttributeValue.class + " but got " + valueClass);
		}
	}

	/**
	 * Get the single value from attribute with given name as byte[]
	 * @param name attribute name
	 * @return byte[] or null
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public byte[] getBinaryAttribute(String name) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		GenticsContentAttribute attr = getAttribute(name);
		byte[] ret = null;

		if (attr.isFilesystem()) {
			try {
				List<?> values = attr.getValues();

				if (!values.isEmpty()) {
					ret = getAsBinary(attr, values.get(0));
				}
			} catch (IOException e) {
				throw new CMSUnavailableException("Error while reading attribute " + name + " for " + this, e);
			}
		} else {
			ret = attr.getNextBinaryValue();
		}

		return ret;
	}

	/**
	 * Get the multivalue from attribute with given name as List&lt;byte[]&gt;
	 * @param name attribute name
	 * @return list of byte[]
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public List<byte[]> getBinaryValues(String name) throws CMSUnavailableException, NodeIllegalArgumentException {
		GenticsContentAttribute attr = getAttribute(name);
		List<byte[]> ret = new Vector<byte[]>();

		if (attr.isFilesystem()) {
			try {
				List<?> values = attr.getValues();

				for (Object value : values) {
					ret.add(getAsBinary(attr, value));
				}
			} catch (IOException e) {
				throw new CMSUnavailableException("Error while reading attribute " + name + " for " + this, e);
			}
		} else {
			byte[] value = null;

			while ((value = attr.getNextBinaryValue()) != null) {
				ret.add(value);
			}
		}
		return ret;
	}

	public int getObjectType() {
		return m_type;
	}

	public int getObjectId() {
		return m_id;
	}

	public void setObjectId(int obj_id) {
		m_id = obj_id;
		m_contentId = m_type + "." + m_id;
	}

	public String getContentId() {
		return m_contentId;
	}

	public int getUpdateTimestamp() {
		return m_updateTimestamp;
	}

	public int getMotherObjectId() {
		return m_mother_id;
	}

	public int getMotherObjectType() {
		return m_mother_type;
	}

	public String getMotherContentId() {
		return m_mother_contentId;
	}

	public boolean exists() {
		try {
			return initialize(this.handle);
		} catch (CMSUnavailableException e) {
			NodeLogger.getNodeLogger(this.getClass()).error("failed to init ContentObject {" + m_contentId + "}", e);
			return false;
		}
	}

	/**
	 * split the given contentid in objecttype and id part
	 * @param contentId contentid
	 * @return array holding the objecttype (index 0) and id (index 1)
	 * @throws NodeIllegalArgumentException when the contentid is not composed
	 *         like [objecttype].[id]
	 */
	public static int[] splitContentId(String contentId) throws NodeIllegalArgumentException {
		String type, id;

		int idx;

		if (contentId == null) {
			throw new NodeIllegalArgumentException("Invalid contentId (null)");
		}
		if ((idx = contentId.indexOf('.')) == -1) {
			throw new NodeIllegalArgumentException("Invalid contentId (" + contentId + ")");
		}

		type = contentId.substring(0, idx);
		id = contentId.substring(idx + 1);
		int[] ret = new int[2];

		try {
			ret[0] = Integer.parseInt(type);
			ret[1] = Integer.parseInt(id);
			return ret;
		} catch (NumberFormatException nfe) {
			throw new NodeIllegalArgumentException("Invalid contentId (" + contentId + ")");
		}
	}

	/**
	 * create an instance of a contentobject with given contentid using the
	 * given handle to access the underlying data storage
	 * @param contentId contentid
	 * @param handle database handle
	 * @throws NodeIllegalArgumentException
	 */
	public GenticsContentObjectImpl(String contentId, DBHandle handle) throws NodeIllegalArgumentException {
		int[] splitId = GenticsContentObjectImpl.splitContentId(contentId);

		m_type = splitId[0];
		m_id = splitId[1];
		m_contentId = contentId;

		m_mother_id = 0;
		m_mother_type = 0;
		m_mother_contentId = "";

		m_updateTimestamp = 0;

		this.handle = handle;

	}

	/**
	 * create an instance of a contentobject for a specific timestamp (past or
	 * future version) with given contentid using the given handle to access the
	 * underlying data storage
	 * @param contentId contentid
	 * @param handle database handle
	 * @param timestamp timestamp of the version
	 * @throws NodeIllegalArgumentException
	 */
	public GenticsContentObjectImpl(String contentId, DBHandle handle, int timestamp) throws NodeIllegalArgumentException {
		this(contentId, handle);
		versionTimestamp = timestamp;
	}

	/**
	 * create a new object of the given type
	 * @param type type of the contentobject to create
	 * @param handle database handle
	 * @param tempContentId temporary contentid
	 */
	public GenticsContentObjectImpl(int type, DBHandle handle, String tempContentId) {
		m_type = type;
		m_id = 0;
		m_contentId = tempContentId;
		m_mother_id = 0;
		m_mother_type = 0;
		m_mother_contentId = "";
		m_updateTimestamp = 0;
		this.handle = handle;
	}

	/**
	 * create a new object of the given type at the given timestamp
	 * @param type type of the contentobject to create
	 * @param handle database handle
	 * @param timestamp timestamp of the object
	 * @param tempContentId temporary contentid
	 */
	public GenticsContentObjectImpl(int type, DBHandle handle, int timestamp, String tempContentId) {
		this(type, handle, tempContentId);
		versionTimestamp = timestamp;
	}

	/**
	 * Create an instance of the GenticsContentObject from data given in the row
	 * @param row row holding metadata of the GenticsContentObject
	 * @param datasource datasource
	 * @param timestamp version timestamp, -1 for current version
	 */
	public GenticsContentObjectImpl(SimpleResultRow row, Datasource datasource, int timestamp) {
		m_id = row.getInt("obj_id");
		m_type = row.getInt("obj_type");
		StringBuffer contentId = new StringBuffer(20);

		contentId.append(m_type).append(".").append(m_id);
		m_contentId = contentId.toString();
		m_isInitialized = true;
		m_exists = true;
		m_updateTimestamp = row.getInt("updatetimestamp");
		m_mother_id = row.getInt("mother_obj_id");
		m_mother_type = row.getInt("mother_obj_type");
		if (m_mother_id != 0 && m_mother_type != 0) {
			StringBuffer motherId = new StringBuffer(20);

			motherId.append(m_mother_type).append(".").append(m_mother_id);
			m_mother_contentId = motherId.toString();
		} else {
			m_mother_contentId = "";
		}
		versionTimestamp = timestamp;
		setDatasource(datasource);
	}

	/**
	 * Create a clone of the original object
	 * @param original original object to clone
	 */
	protected GenticsContentObjectImpl(GenticsContentObjectImpl original) {
		// clone the access data
		datasource = original.datasource;
		handle = original.handle;

		// clone the contentid
		m_contentId = original.m_contentId;
		m_id = original.m_id;
		m_type = original.m_type;

		// clone the mothers information
		m_mother_contentId = original.m_mother_contentId;
		m_mother_id = original.m_mother_id;
		m_mother_type = original.m_mother_type;

		// clone meta information
		m_isInitialized = original.m_isInitialized;
		m_exists = original.m_exists;
		m_updateTimestamp = original.m_updateTimestamp;
		versionTimestamp = original.versionTimestamp;
		customUpdatetimestamp = original.customUpdatetimestamp;

		// clone the attributes
		if (original.m_attributeHash != null) {
			for (Iterator iter = original.m_attributeHash.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				
				try {
					m_attributeHash.put(entry.getKey(), ((GenticsContentAttributeImpl) entry.getValue()).clone());
				} catch (CloneNotSupportedException e) {
					NodeLogger.getNodeLogger(getClass()).error("Error while cloning object " + original.toString(), e);
				}
			}
		}
		if (original.modifiedAttributeNames != null) {
			modifiedAttributeNames = new Vector(original.modifiedAttributeNames);
            
		}
	}

	/**
	 * Prefill all contentObjects with Attributevalues listed in prefetchAttribs
	 * @param datasource datasource
	 * @param contentObjects Array of GenticsContentObjects
	 * @param prefetchAttribs Attributenames to fetch
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static void prefillContentObjects(Datasource datasource,
			GenticsContentObject[] contentObjects, String[] prefetchAttribs) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		prefillContentObjects(datasource, contentObjects, prefetchAttribs, -1, false, false);
	}
    
	/**
	 * Prefill all contentObjects with Attributevalues listed in prefetchAttribs
	 * @param datasource datasource
	 * @param contentObjects Array of GenticsContentObjects
	 * @param prefetchAttribs Attributenames to fetch
	 * @param omitLinkedObjectsIfPossible omit prefetching of linked objects, if
	 *        this is possible (no attributes of the linked objects need to be
	 *        prefetched)
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static void prefillContentObjects(Datasource datasource,
			GenticsContentObject[] contentObjects, String[] prefetchAttribs,
			boolean omitLinkedObjectsIfPossible) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		prefillContentObjects(datasource, contentObjects, prefetchAttribs, -1, false, omitLinkedObjectsIfPossible);
	}

	/**
	 * Prefill all contentObjects with Attributevalues listed in prefetchAttribs
	 * @param datasource datasource
	 * @param contentObjects Array of GenticsContentObjects
	 * @param prefetchAttribs Attributenames to fetch
	 * @param timestamp version timestamp
	 * @param omitLinkedObjectsIfPossible omit prefetching of linked objects, if
	 *        this is possible (no attributes of the linked objects need to be
	 *        prefetched)
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static void prefillContentObjects(Datasource datasource,
			GenticsContentObject[] contentObjects, String[] prefetchAttribs, int timestamp,
			boolean omitLinkedObjectsIfPossible) throws CMSUnavailableException, NodeIllegalArgumentException {
		prefillContentObjects(datasource, contentObjects, prefetchAttribs, timestamp, false, omitLinkedObjectsIfPossible);
	}

	/**
	 * Prefill all contentObjects with Attributevalues listed in prefetchAttribs
	 * @param datasource datasource
	 * @param contentObjects Array of GenticsContentObjects
	 * @param prefetchAttribs Attributenames to fetch
	 * @param timestamp timestamp to use for versioned queries (-1 for
	 *        non-versioned queries)
	 * @param forcePrefetch true when testing of the prefetch thresholds shall
	 *        be omitted and the attributes be fetched in any case
	 * @param omitLinkedObjectsIfPossible omit prefetching of linked objects, if
	 *        this is possible (no attributes of the linked objects need to be
	 *        prefetched)
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */

	public static void prefillContentObjects(Datasource datasource,
			GenticsContentObject[] contentObjects, String[] prefetchAttribs, int timestamp,
			boolean forcePrefetch, boolean omitLinkedObjectsIfPossible) throws CMSUnavailableException, NodeIllegalArgumentException {
		DBHandle handle = GenticsContentFactory.getHandle(datasource);

		// check whether the datasource is versioning, when a versiontimestamp was given
		if (timestamp >= 0) {
			if (!(datasource instanceof VersioningDatasource)) {
				NodeLogger.getLogger(GenticsContentObjectImpl.class).error("cannot prefill versioned objects with non-versioning datasource");
				return;
			}
			((VersioningDatasource) datasource).setVersionTimestamp(timestamp);
		}

		// we do this only for instances of CNDatasource
		if (!(datasource instanceof CNDatasource)) {
			throw new NodeIllegalArgumentException("Cannot prefill objects for a datasource other than CNDatasource");
		}
		CNDatasource cnDatasource = (CNDatasource) datasource;

		// no contentObjects to fill
		if (contentObjects == null) {
			return;
		}
		if (contentObjects.length == 0) {
			return;
		}

		// no attributes to fill
		if (prefetchAttribs == null || prefetchAttribs.length == 0) {
			return;
		}

		if (!forcePrefetch) {
			// check the total number of attributes to prefetch
			int totalAttributes = contentObjects.length * prefetchAttribs.length;
			int prefetchThreshold = cnDatasource.getPrefetchAttributesThreshold();

			// when less attributes shall be prefetch than the threshold says,
			// try to fetch them from the cache first
			if (prefetchThreshold >= 0 && totalAttributes <= prefetchThreshold) {
				prefillObjectsFromCache(datasource, contentObjects, prefetchAttribs, timestamp,
						cnDatasource.getPrefetchAttributesCacheMissThreshold(totalAttributes), omitLinkedObjectsIfPossible);
				return;
			}
		}

		List<String> typeColumns = new Vector<String>();
		List<String> contentIds = new ArrayList<String>(contentObjects.length);
		Map<String, Map<String, List<SortedValue>>> objectValues = new HashMap<String, Map<String, List<SortedValue>>>(contentObjects.length);
		// used if all attributes are fetched
		List<String> attribNames = new ArrayList<String>(PREFETCH_ATTRIBUTE_SIZE);

		// collect all foreign linked objects to prefetch here (keys are the
		// attribute names, values are Lists containing all attributes to
		// prefetch for the foreign objects)
		Map<String, List<String>> foreignObjects = new HashMap<String, List<String>>();

		// collect all "direct" attributes (no foreign link attributes) to
		// prefetch here
		List<String> directAttributes = new Vector<String>();

		// fetch datatypes + columns for prefetched attributes
		// TODO does this make sense?
		for (int i = 0; i < prefetchAttribs.length; i++) {
			String attrib = prefetchAttribs[i];
			DatatypeHelper.AttributeType type = null;

			// if (attrib.indexOf('.') >= 0) {
			if (GenticsContentFactory.isAttributePath(attrib)) {
				// the attribute seems to be an attribute of a linked object

				// extract the link attribute name and the attribute of the
				// linked object
				// String attributeParts[] = attrib.split("\\.", 2);
				String attributeParts[] = GenticsContentFactory.splitAttributePath(attrib);

				// get the attribute type of the base attribute
				try {
					type = DatatypeHelper.getComplexDatatype(handle, attributeParts[0]);
				} catch (Exception ex) {
					NodeLogger.getLogger(GenticsContentObjectImpl.class).warn(
							"could not find attribute '" + attributeParts[0] + "'. Skipping attribute '" + attrib + "'");
					continue;
				}

				if (type.getType() != GenticsContentAttribute.ATTR_TYPE_OBJ && type.getType() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
					NodeLogger.getLogger(GenticsContentObjectImpl.class).warn(
							"attribute '" + attributeParts[0] + "' is no link attribute. Skipping attribute '" + attrib + "'");
					continue;
				}

				List<String> foreignObjectAttributes = null;

				if (foreignObjects.containsKey(attributeParts[0])) {
					// the foreign object was recognized before
					foreignObjectAttributes = foreignObjects.get(attributeParts[0]);
				} else {
					// the foreign object has to be added to the map
					foreignObjectAttributes = new Vector<String>();
					// add the foreign link attribute
					if (type.getType() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
						foreignObjectAttributes.add(type.getForeignLinkedAttribute());
					}
					foreignObjects.put(attributeParts[0], foreignObjectAttributes);
				}

				// add the foreign attribute (if not added before)
				if (!foreignObjectAttributes.contains(attributeParts[1])) {
					foreignObjectAttributes.add(attributeParts[1]);
				}

				// the attribute is also a "direct" attribute
				if (!directAttributes.contains(attributeParts[0])) {
					directAttributes.add(attributeParts[0]);

					if (type.getType() == GenticsContentAttribute.ATTR_TYPE_OBJ) {
						String typeColumn = type.getColumn();

						if (!typeColumns.contains(typeColumn)) {
							typeColumns.add(typeColumn);
						}
						// add additional columns where filesystem attribute store data in
						if (type.isFilesystem()) {
							if (!typeColumns.contains("value_clob")) {
								typeColumns.add("value_clob");
							}
							if (!typeColumns.contains("value_long")) {
								typeColumns.add("value_long");
							}
						}
					}
				}
			} else {
				attrib = GenticsContentFactory.normalizeAttributeName(attrib);
				try {
					type = DatatypeHelper.getComplexDatatype(handle, attrib);
				} catch (Exception ex) {
					NodeLogger.getLogger(GenticsContentObjectImpl.class).warn("could not find attribute '" + attrib + "'. Skipping");
					continue;
				}

				// the attribute is a main attribute of the objects
				if (!directAttributes.contains(attrib)) {

					directAttributes.add(attrib);

					if (type.getType() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
						// get datatype and type column
						String typeColumn = type.getColumn();

						if (typeColumn == null) {
							throw new NodeIllegalArgumentException("Could not get Type for Attribute " + attrib);
						}

						if (!typeColumns.contains(typeColumn)) {
							typeColumns.add(typeColumn);
						}
						// add additional columns where filesystem attribute store data in
						if (type.isFilesystem()) {
							if (!typeColumns.contains("value_clob")) {
								typeColumns.add("value_clob");
							}
							if (!typeColumns.contains("value_long")) {
								typeColumns.add("value_long");
							}
						}
					}
				}

				// when the attribute is a link, add it to the map of foreign
				// objects
				if (type.getType() == GenticsContentAttribute.ATTR_TYPE_OBJ || type.getType() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
					if (!foreignObjects.containsKey(attrib)) {
						// the foreign object has to be added to the map
						List<String> foreignObjectAttributes = new Vector<String>();

						// add the foreign link attribute
						if (type.getType() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
							foreignObjectAttributes.add(type.getForeignLinkedAttribute());
						}
						foreignObjects.put(attrib, foreignObjectAttributes);
					}
				}
			}
		}

		// extract ids from contentObjects and put a value-hash into a map by
		// contentid
		// this is used as a temporary storage
		// layout: objectvalues[contentId][attribname] {multivalue}
		for (int i = 0; i < contentObjects.length; i++) {
			if (contentObjects[i] == null) {
				NodeLogger.getLogger(GenticsContentObjectImpl.class).warn("null contentObject in contentObjects Array. Skipping.");
			} else if (contentObjects[i].getVersionTimestamp() != timestamp) {
				// timestamp of object does not match given timestamp
				NodeLogger.getLogger(GenticsContentObjectImpl.class).warn(
						"timestamp of contentObject '" + contentObjects[i].getContentId() + "' does not match given timestamp. Skipping.");
			} else {
				contentIds.add(contentObjects[i].getContentId());
				Map<String, List<SortedValue>> values = new HashMap<String, List<SortedValue>>(directAttributes.size());

				for (String directAttributeName : directAttributes) {
					values.put(directAttributeName, new ArrayList<SortedValue>(PREFETCH_MULTIVALUE_SIZE));
				}

				// save Attribute HashMap
				objectValues.put(contentObjects[i].getContentId(), values);
			}
		}

		// when no objects remain to be prefilled, we are done
		if (contentIds.size() == 0) {
			return;
		}

		// build select statement
		String columns = null;

		if (directAttributes.size() == 0) {
			// fetch all attributes
			columns = MiscUtils.getArrayString(DatatypeHelper.getTypeColumns(GenticsContentAttribute.ATTR_TYPE_ALL), ", ");
		} else {
			// TODO does this make sense?
			// fetch specific attributes
			columns = StringUtils.merge(typeColumns.toArray(), ",");
		}

		// now really do the prefetching

		// check whether we need to split the list of objects to be prefetched
		boolean isOracle = DatatypeHelper.ORACLE_NAME.equals(cnDatasource.getDatabaseProductName());
		boolean isMSSQL = DatatypeHelper.MSSQL_NAME.equals(cnDatasource.getDatabaseProductName());
		int maxPrefetchSize = -1;

		if (isOracle) {
			maxPrefetchSize = MAX_PREFETCH_SIZE_ORACLE;
		}
		if (isMSSQL) {
			maxPrefetchSize = MAX_PREFETCH_SIZE_MSSQL;
		}

		boolean doSplitForPrefetch = maxPrefetchSize != -1;
		if (doSplitForPrefetch) {
			for (int i = 0; i < contentIds.size(); i += maxPrefetchSize) {
				int start = i;
				int end = Math.min(contentIds.size(), i + maxPrefetchSize);

				doPrefetchAttributes(handle, datasource, contentIds.subList(start, end), directAttributes, columns, timestamp, attribNames, prefetchAttribs,
						objectValues, foreignObjects, omitLinkedObjectsIfPossible);
			}
		} else {
			doPrefetchAttributes(handle, datasource, contentIds, directAttributes, columns, timestamp, attribNames, prefetchAttribs, objectValues,
					foreignObjects, omitLinkedObjectsIfPossible);
		}

		// fill fetched attributes in from value-hash into contentobjects
		boolean doCacheAttribute = true;

		for (int i = 0; i < contentObjects.length; i++) {
			GenticsContentObjectImpl contentObject = (GenticsContentObjectImpl) contentObjects[i];
			HashMap valueHash = (HashMap) objectValues.get(contentObject.getContentId());

			if (directAttributes.size() == 0) {
				for (int j = 0; j < attribNames.size(); j++) {
					String attrib = (String) attribNames.get(j);
					ArrayList values = (ArrayList) valueHash.get(attrib);

					// add the sorted values to the contentobject
					SortedValue.addSortedValuesToContentObject(contentObject, attrib, values);
					// and put the attribute into the cache
					try {
						if (doCacheAttribute && !DatatypeHelper.getDefaultColumnTypes().keySet().contains(attrib)) {
							GenticsContentFactory.cacheAttribute(datasource, contentObject, contentObject.getAttribute(attrib));
						}
					} catch (Exception e) {
						NodeLogger.getLogger(GenticsContentObjectImpl.class).warn("Error while putting object into cache", e);
						// putting the attribute into the cached failed, so
						// stop caching attributes for now (generate only
						// one warning per call to this method)
						doCacheAttribute = false;
					}
				}
			} else {
				for (Iterator iter = valueHash.keySet().iterator(); iter.hasNext();) {
					String attributeName = (String) iter.next();
					ArrayList values = (ArrayList) valueHash.get(attributeName);

					// add the sorted values to the contentobject
					SortedValue.addSortedValuesToContentObject(contentObject, attributeName, values);
					// and put the attribute into the cache
					try {
						if (doCacheAttribute && !DatatypeHelper.getDefaultColumnTypes().keySet().contains(attributeName)) {
							GenticsContentFactory.cacheAttribute(datasource, contentObject, contentObject.getAttribute(attributeName));
						}
					} catch (Exception e) {
						NodeLogger.getLogger(GenticsContentObjectImpl.class).warn("Error while putting object into cache", e);
						// putting the attribute into the cached failed, so
						// stop caching attributes for now (generate only
						// one warning per call to this method)
						doCacheAttribute = false;
					}
				}
			}
		}
	}

	/**
	 * Internal method to do the prefetching
	 * @param handle database handle
	 * @param datasource datasource
	 * @param contentIds list of contentids for which the prefilling shall be done
	 * @param directAttributes list containing the names of direct attributes to be prefilled
	 * @param columns string containing a comma separated list of db columns to be selected
	 * @param timestamp timestamp of the versioned objects (-1 for current object)
	 * @param attribNames list of attribute names which were prefilled (will be modified)
	 * @param prefetchAttribs string array of the attributes to be prefetched
	 * @param objectValues map which will hold the prefetched values (will be modified)
	 * @param foreignObjects map containing the foreign objects (will be modified)
	 * @param omitLinkedObjectsIfPossible omit prefetching of linked objects, if
	 *        this is possible (no attributes of the linked objects need to be
	 *        prefetched)
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	protected static void doPrefetchAttributes(DBHandle handle, Datasource datasource,
			List<String> contentIds, List<String> directAttributes,
			String columns, int timestamp, List<String> attribNames, String[] prefetchAttribs,
			Map<String, Map<String, List<SortedValue>>> objectValues, Map<String, List<String>> foreignObjects, boolean omitLinkedObjectsIfPossible) throws CMSUnavailableException, NodeIllegalArgumentException {
		try {
			CNDatasource cnDatasource = (CNDatasource) datasource;

			SimpleResultProcessor rs = new SimpleResultProcessor();

			// rb - if prefetchAttribs == null, fetch all attributes
			String sql = null;

			// workaround for Oracle 1000 Element IN limit; assumes contentIds.size() > 0
			int inLimit = 999;
			String limRep = MiscUtils.repeatString("?", inLimit, ",");
			String modRep = MiscUtils.repeatString("?", contentIds.size() % inLimit, ",");  
			String divRep = MiscUtils.repeatString(limRep, contentIds.size() / inLimit, ") OR contentid IN (");
			String contentidSql = "contentid IN (" + divRep + (divRep.length() > 0 && modRep.length() > 0 ? ") OR contentid IN (" : "") + modRep + ")";

			if (timestamp >= 0) {
				// versioned query
				sql = "SELECT contentid, name, sortorder" + (columns != null && columns.length() > 0 ? ", " : " ") + columns + " FROM "
						+ handle.getContentAttributeName() + "_nodeversion gentics_main WHERE " + "(" + contentidSql + ")"
						+ " AND nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM " + handle.getContentAttributeName() + "_nodeversion WHERE "
						+ handle.getContentAttributeName() + "_nodeversion.id = gentics_main.id AND nodeversiontimestamp <= " + timestamp
						+ " AND (nodeversionremoved = 0 OR nodeversionremoved > " + timestamp + "))";
			} else {
				// non-versioned query
				sql = "SELECT contentid, name, sortorder" + (columns != null && columns.length() > 0 ? ", " : " ") + columns + " FROM "
						+ handle.getContentAttributeName() + " WHERE " + "(" + contentidSql + ")";
			}

			// fetch only wanted attributes from prefetchAttribs
			if (directAttributes.size() > 0) {
				sql += " AND name IN (" + MiscUtils.repeatString("?", directAttributes.size(), ",") + ")";
			}

			// put params into an array
			Object[] params = new Object[contentIds.size() + directAttributes.size()];

			int k = 0;

			for (String contentId : contentIds) {
				params[k++] = contentId;
			}

			for (String directAttributeName : directAttributes) {
				params[k++] = directAttributeName;
			}

			DB.query(handle, sql, params, rs);

			// fill fetched attributes in value-hash
			Iterator<SimpleResultRow> it = rs.iterator();

			while (it.hasNext()) {
				SimpleResultRow row = it.next();
				String attrib = row.getString("name");

				// TODO: move method setAttribute() to interface
				// GenticsContentObject

				// fill multivalues in objectValues[contentId][xxxx]
				if (prefetchAttribs.length == 0) {
					// prefetchAttribs == null
					Map<String, List<SortedValue>> objValue = objectValues.get(row.getString("contentid"));
					List<SortedValue> values = objValue.get(attrib);

					// create objValue[contentId] Hashmaps on the fly
					if (values == null) {
						values = new ArrayList<SortedValue>(PREFETCH_MULTIVALUE_SIZE);
						objValue.put(attrib, values);

						// save attrib name for further use
						if (!attribNames.contains(attrib)) {
							attribNames.add(attrib);
						}
					}

					values.add(new SortedValue(DatatypeHelper.getComplexDatatype(handle, attrib), row));
				} else {
					List<SortedValue> values = objectValues.get(row.getString("contentid")).get(attrib);

					values.add(new SortedValue(DatatypeHelper.getComplexDatatype(handle, attrib), row));
				}
			}

			// now fetch all linked objects (and prefill them)
			for (Map.Entry<String, List<String>> entry : foreignObjects.entrySet()) {
				String element = entry.getKey();
				List<String> foreignAttributes = entry.getValue();
				DatatypeHelper.AttributeType type = DatatypeHelper.getComplexDatatype(handle, element);

				// when the list of foreignAttributes is empty add at least the
				// contentid
				if (foreignAttributes.size() == 0) {
					// hmm better omit this attribte?
					if (omitLinkedObjectsIfPossible) {
						continue;
					} else {
						foreignAttributes.add("contentid");
					}
				}

				if (type.getType() == GenticsContentAttribute.ATTR_TYPE_OBJ) {
					// this is a linked object
					// collect all contentids of linked objects
					List<String> linkedContentIds = new Vector<String>();

					for (Map<String, List<SortedValue>> valueMap : objectValues.values()) {
						if (valueMap.containsKey(element)) {
							// add the values here
							List<SortedValue> values = valueMap.get(element);

							for (SortedValue sortedValue : values) {
								// do not add null values (would produce a NPE later on)
								if (sortedValue.getValue() != null) {
									linkedContentIds.add(ObjectTransformer.getString(sortedValue.getValue(), ""));
								}
							}
						}
					}

					// linkedContentIds shall hold every entry only once
					Collections.sort(linkedContentIds);
					String lastEntry = null;

					for (Iterator<String> iter1 = linkedContentIds.iterator(); iter1.hasNext();) {
						String thisEntry = iter1.next();

						if (lastEntry != null && lastEntry.equals(thisEntry)) {
							// when there is a last entry and it equals this
							// entry, remove this entry
							iter1.remove();
						} else {
							// we have a new last entry
							lastEntry = thisEntry;
						}
					}

					RuleTree ruleTree = new DefaultRuleTree();
					Map<String, Object> ruleData = new HashMap<String, Object>();

					ruleData.put("contentid", linkedContentIds);
					ruleTree.addResolver("data", ruleData);
					// prepare the rule to fetch the foreign linked objects
					String fetchRule = "object.obj_type == " + type.getLinkedObjectType() + " AND object." + GenticsContentAttribute.ATTR_CONTENT_ID
							+ " CONTAINSONEOF data.contentid";

					try {
						Datasource dsClone = (Datasource) datasource.clone();

						ruleTree.parse(fetchRule);
						dsClone.setRuleTree(ruleTree);
						DatasourceRecordSet foreignResult = (DatasourceRecordSet) dsClone.getResult();

						GenticsContentObject[] foreignObjectArray = new GenticsContentObject[foreignResult.size()];

						for (int i = 0; i < foreignResult.size(); ++i) {
							foreignObjectArray[i] = (GenticsContentObject) foreignResult.getRow(i).toObject();
						}
						// sort the array of foreign objects (for searching
						// later)
						Arrays.sort(foreignObjectArray);

						prefillContentObjects(dsClone, foreignObjectArray, (String[]) foreignAttributes.toArray(new String[foreignAttributes.size()]), timestamp,
								true);

						// add the objects also to the values of the attribute
						// TODO loop through all main objects, loop through the
						// values in the attribute, find the
						// content objects
						for (Map.Entry<String, Map<String, List<SortedValue>>> ovEntry : objectValues.entrySet()) {
							String contentId = ovEntry.getKey();
							Map<String, List<SortedValue>> objValue = ovEntry.getValue();
							List<SortedValue> values = objValue.get(element);

							if (values != null) {
								List<SortedValue> newValues = new ArrayList<SortedValue>(values.size());

								for (SortedValue oldSortedValue : values) {
									String foreignContentId = ObjectTransformer.getString(oldSortedValue.getValue(), null);

									if (foreignContentId != null) {
										int index = Arrays.binarySearch(foreignObjectArray, foreignContentId);

										if (index >= 0) {
											newValues.add(new SortedValue(foreignObjectArray[index], oldSortedValue.getSortorder()));
										} else if (cnDatasource.isGetIllegalLinksAsDummyObjects()) {
											newValues.add(new SortedValue(foreignContentId, oldSortedValue.getSortorder()));
										}
									} else {
										if (logger.isDebugEnabled()) {
											logger.debug("The foreign contentid of contentid {" + contentId + "}, attribute {" + element + "} is null");
										}
									}
								}

								objValue.put(element, newValues);
							}
						}

					} catch (Exception ex) {
						NodeLogger.getLogger(GenticsContentObjectImpl.class).error("error while prefetching linked objects", ex);
					}

				} else if (type.getType() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
					// foreign linked objects, fetch via rule
					RuleTree ruleTree = new DefaultRuleTree();
					Map<String, Object> ruleData = new HashMap<String, Object>();

					ruleData.put("contentid", contentIds);
					ruleTree.addResolver("data", ruleData);

					String fetchRule = "object.obj_type == " + type.getLinkedObjectType() + " AND object." + type.getForeignLinkedAttribute()
							+ " CONTAINSONEOF data.contentid";
					// fetch the configured rule for the foreign link attribute
					String additionalForeignLinkAttributeRule = type.getForeignLinkAttributeRule();

					// use also the rule configured with the attribute
					if (additionalForeignLinkAttributeRule != null && additionalForeignLinkAttributeRule.length() > 0) {
						fetchRule = "(" + fetchRule + ") && (" + additionalForeignLinkAttributeRule + ")";
					}

					try {
						Datasource dsClone = (Datasource) datasource.clone();

						ruleTree.parse(fetchRule);
						dsClone.setRuleTree(ruleTree);
						DatasourceRecordSet foreignResult = (DatasourceRecordSet) dsClone.getResult();

						GenticsContentObject[] foreignObjectArray = new GenticsContentObject[foreignResult.size()];

						for (int i = 0; i < foreignResult.size(); ++i) {
							foreignObjectArray[i] = (GenticsContentObject) foreignResult.getRow(i).toObject();
						}

						prefillContentObjects(dsClone, foreignObjectArray, (String[]) foreignAttributes.toArray(new String[foreignAttributes.size()]), timestamp,
								true);

						// add the objects also to the values of the attribute
						for (int i = 0; i < foreignObjectArray.length; ++i) {
							String contentId = foreignObjectArray[i].getAttribute(type.getForeignLinkedAttribute()).getNextValue();

							Map<String, List<SortedValue>> objValue = objectValues.get(contentId);

							if (objValue == null) {
								// TODO log an error/warning here? (data
								// inconsistency found, found an object linking
								// to a root object but link was not fetched?)
								continue;
							}
							List<SortedValue> values = objValue.get(element);

							// create objValue[contentId] Hashmaps on the fly
							if (values == null) {
								values = new ArrayList<SortedValue>(PREFETCH_MULTIVALUE_SIZE);
								objValue.put(element, values);

								// save attrib name for further use
								if (!attribNames.contains(element)) {
									attribNames.add(element);
								}
							}

							values.add(new SortedValue(foreignObjectArray[i], i));
						}

					} catch (Exception ex) {
						NodeLogger.getLogger(GenticsContentObjectImpl.class).error("error while prefetching linked objects", ex);
					}
				}
			}

		} catch (SQLException e) {
			throw new CMSUnavailableException(
					"Could not initialize Attributes (" + MiscUtils.getArrayString(prefetchAttribs, ", ") + ") for GenticsContentObjects( " + contentIds + "): "
					+ e.getMessage(),
					e);
		}
	}

	/**
	 * Prefill the objects with attributes from the cache. When
	 * cacheMissThreshold is given a values &gt;=0 and more that
	 * cacheMissThreshold attributes are not found in the cache, the prefetching
	 * from the cache is stopped and
	 * {@link #prefillContentObjects(Datasource, GenticsContentObject[], String[], int, boolean)}
	 * is called (fallback to prefilling directly from the database).
	 * @param datasource datasource
	 * @param contentObjects array of contentobjects
	 * @param prefetchAttribs array of attribute names to prefetch
	 * @param timestamp versiontimestamp (may be -1 for fetching current
	 *        versions)
	 * @param cacheMissThreshold threshold for allowed cache misses, -1 to
	 *        deactivate fallback to prefilling from the database
	 * @param omitLinkedObjectsIfPossible 
	 * @throws NodeIllegalArgumentException 
	 * @throws CMSUnavailableException 
	 */
	protected static void prefillObjectsFromCache(Datasource datasource,
			GenticsContentObject[] contentObjects, String[] prefetchAttribs, int timestamp,
			int cacheMissThreshold, boolean omitLinkedObjectsIfPossible) throws CMSUnavailableException, NodeIllegalArgumentException {
		// prepare the attributes tree
		Map attributesTree = prepareAttributeTree(prefetchAttribs);

		try {
			int cacheMisses = 0;

			for (int i = 0; i < contentObjects.length && (cacheMissThreshold < 0 || cacheMisses < cacheMissThreshold); i++) {
				cacheMisses = prefillObjectFromCache(datasource, contentObjects[i], attributesTree, timestamp, cacheMissThreshold, cacheMisses);
			}
			if (cacheMisses >= cacheMissThreshold) {
				// do the fallback (too many cache misses)
				prefillContentObjects(datasource, contentObjects, prefetchAttribs, timestamp, true, omitLinkedObjectsIfPossible);
			}
		} catch (PortalCacheException e) {
			NodeLogger.getNodeLogger(GenticsContentObjectImpl.class).warn("Error while prefilling objects from cache", e);
			// do the fallback (error while getting attributes from cache)
			prefillContentObjects(datasource, contentObjects, prefetchAttribs, timestamp, true, omitLinkedObjectsIfPossible);
		}
	}

	/**
	 * Recursively prefill the object with attributes given in the tree
	 * @param datasource datasource
	 * @param contentObject contentobject to prefill
	 * @param attributesTree prepared attribute tree
	 * @param timestamp timestamp
	 * @param cacheMissThreshold cache miss threshold
	 * @param cacheMisses number of cache misses so far
	 * @return number of cache misses
	 * @throws PortalCacheException
	 */
	protected static int prefillObjectFromCache(Datasource datasource,
			GenticsContentObject contentObject, Map attributesTree, int timestamp,
			int cacheMissThreshold, int cacheMisses) throws PortalCacheException {
		// loop through all primary attributes as long as we do not reach the cacheMissThreshold
		for (Iterator iter = attributesTree.entrySet().iterator(); (cacheMissThreshold < 0 || cacheMisses < cacheMissThreshold) && iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			String attributeName = (String) entry.getKey();
			Map subAttributesTree = (Map) entry.getValue();

			// try to get the attribute from the cache
			GenticsContentAttribute cachedAttribute = GenticsContentFactory.getCachedAttribute(datasource, contentObject, attributeName);

			if (cachedAttribute == null) {
				// this is a cache miss
				cacheMisses++;
			} else {
				// this is a cache hit, put the attribute into the map
				((GenticsContentObjectImpl) contentObject).m_attributeHash.put(attributeName, cachedAttribute);
				int type = cachedAttribute.getAttributeType();

				if ((type == GenticsContentAttribute.ATTR_TYPE_OBJ || type == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) && !subAttributesTree.isEmpty()) {
					// get the linked objects
					Object value = contentObject.get(attributeName);

					if (value instanceof Collection) {
						// loop through all linked objects and prefill it
						for (Iterator iterator = ((Collection) value).iterator(); (cacheMissThreshold < 0 || cacheMisses < cacheMissThreshold)
								&& iterator.hasNext();) {
							GenticsContentObject subObject = (GenticsContentObject) iterator.next();

							// prefill the object, count the additional cache
							// misses
							cacheMisses += prefillObjectFromCache(datasource, subObject, subAttributesTree, timestamp, cacheMissThreshold, cacheMisses);
						}
					} else if (value instanceof GenticsContentObject) {
						// prefill the object, count the additional cache misses
						cacheMisses += prefillObjectFromCache(datasource, (GenticsContentObject) value, subAttributesTree, timestamp, cacheMissThreshold,
								cacheMisses);
					}
				}
			}
		}
		return cacheMisses;
	}

	/**
	 * Transform the given array of attribute names into an attribute tree.
	 * Return a map, keys are the primary attributes, values are maps of
	 * secondary attributes (if any), and so on.
	 * @param attributes array of attribute names
	 * @return map containing the attribute tree
	 */
	public static Map prepareAttributeTree(String[] attributes) {
		Map attributeTree = new HashMap();

		if (attributes != null) {
			for (int i = 0; i < attributes.length; i++) {
				// split the attribute into its parts (primary, secondary, ...)
				if (!StringUtils.isEmpty(attributes[i])) {
					String[] parts = attributes[i].split("\\.");

					// add the path into the map
					addAttributeStructure(attributeTree, parts, 0);
				}
			}
		}

		return attributeTree;
	}

	/**
	 * Recursively add the attribute path into the attribute map.
	 * @param attributeMap attribute map
	 * @param parts attribute path, already split into the parts
	 * @param pos position in the path that needs to be processed next
	 */
	private static void addAttributeStructure(Map attributeMap, String[] parts, int pos) {
		// recursion exit when all parts have been processed
		if (pos >= parts.length) {
			return;
		}

		Map subMap = null;

		if (DatatypeHelper.getDefaultColumnTypes().keySet().contains(parts[pos])) {
			// exclude the default column types
			return;
		} else if (attributeMap.containsKey(parts[pos])) {
			// the attribute is already in the map, just fetch the map of sub-attributes
			subMap = (Map) attributeMap.get(parts[pos]);
		} else {
			// add the attribute into the map
			subMap = new HashMap();
			attributeMap.put(parts[pos], subMap);
		}

		// do the recursion step
		addAttributeStructure(subMap, parts, pos + 1);
	}

	public String[] getAccessedAttributeNames(boolean omitMetaAttributes) {
		Collection attributeNames = new Vector(m_attributeHash.keySet());

		if (omitMetaAttributes) {
			attributeNames.removeAll(DatatypeHelper.getDefaultColumnTypes().keySet());
		}
		Iterator it = attributeNames.iterator();
		int i = 0;
		String[] ret = new String[attributeNames.size()];

		while (it.hasNext()) {
			ret[i] = (String) it.next();
			i++;
		}
		return ret;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getModifiedAttributeNames()
	 */
	public String[] getModifiedAttributeNames() {
		if (modifiedAttributeNames != null) {
			return (String[]) modifiedAttributeNames.toArray(new String[modifiedAttributeNames.size()]);
		} else {
			return new String[0];
		}
	}

	public List getAttributeDefinitions() {
		GenticsContentAttribute attr;
		NodeLogger logger = NodeLogger.getNodeLogger(getClass());

		ArrayList list = new ArrayList();

		// add all default attributes
		for (Iterator i = DatatypeHelper.getDefaultColumnTypes().keySet().iterator(); i.hasNext();) {
			String defaultColumn = (String) i.next();

			try {
				attr = getAttribute(defaultColumn);
				list.add(attr);
			} catch (Exception e) {
				logger.error("error while initializing default attribute '" + defaultColumn + "'", e);
			}
		}

		String sql = "SELECT name FROM " + getDBHandle().getContentAttributeTypeName() + " WHERE objecttype = " + this.m_type;
		SimpleResultProcessor rs = new SimpleResultProcessor();

		try {
			DB.query(handle, sql, rs);

			// fill fetched attributes in value-hash
			Iterator it = rs.iterator();

			while (it.hasNext()) {
				SimpleResultRow row = (SimpleResultRow) it.next();
				String name = row.getString("name");

				try {
					attr = getAttribute(name);
					list.add(attr);
				} catch (Exception e) {
					logger.error("error while initializing attribute '" + name + "'", e);
				}
			}
		} catch (SQLException e) {
			logger.error("error while getting attribute definitions", e);
		}

		return (List) list;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getVersionTimestamp()
	 */
	public int getVersionTimestamp() {
		return versionTimestamp;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#isCurrentVersion()
	 */
	public boolean isCurrentVersion() {
		return versionTimestamp == -1;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#isFutureVersion()
	 */
	public boolean isFutureVersion() {
		if (versionTimestamp < 0) {
			// it's a current version
			return false;
		}
		int now = (int) (System.currentTimeMillis() / 1000);

		return versionTimestamp > now;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#isPastVersion()
	 */
	public boolean isPastVersion() {
		if (versionTimestamp < 0) {
			// it's a current version
			return false;
		}
		int now = (int) (System.currentTimeMillis() / 1000);

		return versionTimestamp < now;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getVersionDate()
	 */
	public Date getVersionDate() {
		if (versionTimestamp < 0) {
			return new Date();
		} else {
			return new Date(versionTimestamp * 1000L);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#setVersionTimestamp(int)
	 */
	public void setVersionTimestamp(int versionTimestamp) {
		if (this.versionTimestamp != versionTimestamp) {
			// when the versiontimestamp is modified, we suppose all accessed
			// attributes to be changed to make sure they will be correctly
			// saved
			if (modifiedAttributeNames == null) {
				modifiedAttributeNames = new Vector();
			}
			modifiedAttributeNames.clear();
			modifiedAttributeNames.addAll(m_attributeHash.keySet());
		}
		this.versionTimestamp = versionTimestamp;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return m_contentId;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (o instanceof GenticsContentObject) {
			return m_contentId.compareTo(((GenticsContentObject) o).getContentId());
		} else if (o instanceof ResolvableGenticsContentObject) {
			return m_contentId.compareTo(((ResolvableGenticsContentObject) o).getContentobject().getContentId());
		} else {
			// TODO this cast origins from java 1.4's compareTo(Object) implementation, clean this up.
			return m_contentId.compareTo((String) o);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setMotherObject(com.gentics.lib.content.GenticsContentObject)
	 */
	public void setMotherObject(GenticsContentObject motherObject) {
		if (motherObject == null) {
			m_mother_id = 0;
			m_mother_type = 0;
			m_mother_contentId = "";
		} else {
			m_mother_id = motherObject.getObjectId();
			m_mother_type = motherObject.getObjectType();
			m_mother_contentId = motherObject.getContentId();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setMotherContentId(java.lang.String)
	 */
	public void setMotherContentId(String motherContentId) throws NodeIllegalArgumentException {
		if (StringUtils.isEmpty(motherContentId)) {
			m_mother_id = 0;
			m_mother_type = 0;
			m_mother_contentId = "";
		} else {
			int[] parts = splitContentId(motherContentId);

			m_mother_id = parts[1];
			m_mother_type = parts[0];
			m_mother_contentId = motherContentId;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		Object object = "";

		if ("contentid".equals(key)) {
			return getContentId();
		} else if ("obj_type".equals(key)) {
			return new Integer(getObjectType());
		} else if ("obj_id".equals(key)) {
			return new Integer(getObjectId());
		} else if ("mother_obj_type".equals(key)) {
			return new Integer(getMotherObjectType());
		} else if ("mother_obj_id".equals(key)) {
			return new Integer(getMotherObjectId());
		} else if ("updatetimestamp".equals(key)) {
			return new Integer(getUpdateTimestamp());
		} else if ("motherid".equals(key)) {
			return getMotherContentId();
		}

		try {
			GenticsContentAttribute attr = getAttribute(key);
			int attrType = attr.getAttributeType();

			switch (attrType) {
			case GenticsContentAttribute.ATTR_TYPE_OBJ:
			case GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ:
				object = this.getObjectAttribute(attr);
				break;

			case GenticsContentAttribute.ATTR_TYPE_BLOB:
				if (attr.isMultivalue()) {
					object = getBinaryValues(key);
				} else {
					object = this.getBinaryAttribute(key);
				}
				break;

			case GenticsContentAttribute.ATTR_TYPE_LONG:
			case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
			case GenticsContentAttribute.ATTR_TYPE_DATE:
				object = getAttributeValue(attr);
				break;

			case GenticsContentAttribute.ATTR_TYPE_INTEGER:
				if (datasource instanceof CNDatasource && ((CNDatasource) datasource).isTreatIntegerAsString()) {
					object = getStringAttribute(attr);
				} else {
					object = getAttributeValue(attr);
				}
				break;

			default:
				object = getStringAttribute(attr);
				break;
			}
			// if (attr.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_OBJ
			// || attr.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
			// object = this.getObjectAttribute(attr);
			// } else if(attr.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_BLOB) {
			// object = this.getBinaryAttribute(key);
			// } else {
			// object = this.getStringAttribute(attr);
			// }
		} catch (NodeIllegalArgumentException e) {
			NodeLogger.getLogger(getClass()).warn("property " + key + " of contentobject " + getContentId() + " not found", e);
		} catch (CMSUnavailableException e) {
			NodeLogger.getLogger(getClass()).error("cms unavailable during access on property " + key + " of contentobject " + getContentId() + " not found", e);
		}

		return object;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/**
	 * Get the value(s) of the given attribute as GenticsContentObjects.
	 * @param attrib attribute for which the values shall be fetched
	 * @return value of the attribute (GenticsContentObject for non-multivalue
	 *         link attributes or Collection of GenticsContentObjects for
	 *         multivalue or foreignlink attributes)
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	protected Object getObjectAttribute(GenticsContentAttribute attrib) throws NodeIllegalArgumentException, CMSUnavailableException {
		if (attrib != null) {
			if (!attrib.isMultivalue() && attrib.getAttributeType() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
				attrib.resetIterator();
				return attrib.getNextContentObject();
			} else {
				attrib.resetIterator();
				ArrayList ret = new ArrayList(20);
				Iterator it = attrib.objectIterator();
				int i = 0;

				while (it.hasNext()) {
					Object nextObject = it.next();

					if (nextObject instanceof GenticsContentObject) {
						ret.add((GenticsContentObject) nextObject);
						i++;
					} else if (nextObject != null) {
						throw new NodeIllegalArgumentException(
								"attribute " + attrib.getAttributeName() + " is supposed to return GenticsContentObject but returned object of class "
								+ nextObject.getClass().getName());
					}
				}
				return ret;
			}
		}
		return null;
	}

	/**
	 * Get the value(s) of the given Attribute as String or Collection of
	 * Strings
	 * @param attrib Attribute for which the values shall be fetched
	 * @return null, if the attribute is not given, String value or Collection
	 *         of String values
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	protected Object getStringAttribute(GenticsContentAttribute attrib) throws NodeIllegalArgumentException, CMSUnavailableException {
		if (attrib != null) {
			if (attrib.isFilesystem()) {
				try {
					List<?> values = attrib.getValues();

					if (attrib.isMultivalue()) {
						// get multivalue
						Collection<String> strings = new Vector<String>(values.size());

						for (Object v : values) {
							FilesystemAttributeValue fsValue = (FilesystemAttributeValue) v;
							InputStream in = fsValue.getInputStream(attrib, ((CNDatasource) getDatasource()).getAttributePath());

							try {
								strings.add(StringUtils.readStream(in, "UTF-8"));
							} finally {
								try {
									in.close();
								} catch (IOException e) {// ignored
								}
							}
						}
						return strings;
					} else {
						// get singlevalue
						if (values.isEmpty()) {
							return null;
						} else {
							InputStream in = ((FilesystemAttributeValue) values.get(0)).getInputStream(attrib,
									((CNDatasource) getDatasource()).getAttributePath());

							try {
								return StringUtils.readStream(in, "UTF-8");
							} finally {
								try {
									in.close();
								} catch (IOException e) {// ignored
								}
							}
						}
					}
				} catch (IOException e) {
					throw new CMSUnavailableException("Error while reading attribute " + attrib.getAttributeName() + " for " + this, e);
				}
			} else {
				if (!attrib.isMultivalue()) {
					return attrib.getNextValue();
				} else {
					ArrayList ret = new ArrayList(20);
					Iterator it = attrib.valueIterator();

					while (it.hasNext()) {
						String s = (String) it.next();

						ret.add(s);
					}
					return ret;
				}
			}
		}
		return null;
	}

	/**
	 * Get the value(s) of the given Attribute as Object or Collection of Objects
	 * @param attrib Attribute for which the values shall be fetched
	 * @return null, if the attribute is not given, Object value or Collection of Object values
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	protected Object getAttributeValue(GenticsContentAttribute attrib) throws NodeIllegalArgumentException, CMSUnavailableException {
		if (attrib != null) {
			if (attrib.isMultivalue()) {
				return new Vector(attrib.getValues());
			} else {
				List values = attrib.getValues();

				if (values.size() > 0) {
					return values.get(0);
				} else {
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Get the datasource used to handle this object
	 * @return Returns the datasource.
	 */
	public Datasource getDatasource() {
		return datasource;
	}

	/**
	 * Set the datasource used to handle this object
	 * @param datasource The datasource to set.
	 */
	public void setDatasource(Datasource datasource) {
		this.datasource = datasource;
		if (datasource != null) {
			HandlePool handlePool = datasource.getHandlePool();

			if (handlePool != null) {
				DatasourceHandle handle = handlePool.getHandle();

				if (handle instanceof SQLHandle) {
					this.handle = ((SQLHandle) handle).getDBHandle();
				}
			}
		} else {
			handle = null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.Changeable#setProperty(java.lang.String, java.lang.Object)
	 */
	public boolean setProperty(String name, Object value) throws InsufficientPrivilegesException {
		// TODO: check this (multivalue, etc.)
		try {
			setAttribute(name, value);
			return true;
		} catch (Exception e) {
			NodeLogger.getLogger(getClass()).error("error while setting attribute '" + name + "' for object '" + this + "'", e);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return m_contentId != null && m_contentId.length() > 0 ? m_contentId.hashCode() : super.hashCode();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getCurrentVersion()
	 */
	public VersionedObject getCurrentVersion() {
		if (isCurrentVersion()) {
			return this;
		} else {
			try {
				return GenticsContentFactory.createContentObject(getContentId(), datasource);
			} catch (Exception e) {
				NodeLogger.getLogger(getClass()).error("error while getting the current version for object {" + getContentId() + "}", e);
				return null;
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getVersion(int)
	 */
	public VersionedObject getVersion(int versionTimestamp) {
		if (this.versionTimestamp == versionTimestamp) {
			return this;
		} else {
			try {
				return GenticsContentFactory.createContentObject(getContentId(), datasource, versionTimestamp);
			} catch (Exception e) {
				NodeLogger.getLogger(getClass()).error(
						"error while getting the version for object {" + getContentId() + "} at timestamp {" + versionTimestamp + "}", e);
				return null;
			}
		}
	}

	/**
	 * Mark the given attribute as being modified
	 * @param attributeName name of the modified attribute
	 */
	protected void markAttributeModified(String attributeName) {
		if (modifiedAttributeNames == null) {
			modifiedAttributeNames = new Vector();
		}
		if (!modifiedAttributeNames.contains(attributeName)) {
			modifiedAttributeNames.add(attributeName);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getVersions()
	 */
	public Version[] getVersions() {
		if (datasource instanceof VersioningDatasource && ((VersioningDatasource) datasource).isVersioning()) {
			return ((VersioningDatasource) datasource).getVersions(m_contentId);
		} else {
			return VersioningDatasource.EMPTY_VERSIONLIST;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#isVersioned()
	 */
	public boolean isVersioned() {
		if (datasource instanceof CNDatasource) {
			CNDatasource cnDatasource = (CNDatasource) datasource;

			return cnDatasource.isObjecttypeVersioned(m_type);
		} else {
			// no datasource (or a non Versioning) set, so no versioning support
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		return new GenticsContentObjectImpl(this);
	}

	/**
	 * Inner helper class for sorting multivalue values
	 */
	protected static class SortedValue implements Comparable {

		/**
		 * value
		 */
		protected Object value;

		/**
		 * sortorder of the value
		 */
		protected int sortorder;

		/**
		 * MD5 hash for filesystem attribute values
		 */
		protected String fileMd5;

		/**
		 * File length for filesystem attribute values
		 */
		protected long fileLength;

		/**
		 * Create instance of the sorted value
		 * @param value value
		 * @param sortorder sortorder
		 */
		public SortedValue(Object value, int sortorder) {
			this.value = value;
			this.sortorder = sortorder;
		}

		/**
		 * Create an instance for the given attribute type, where the data is stored in the row
		 * @param type attribute type
		 * @param row row containing the data
		 */
		public SortedValue(AttributeType type, SimpleResultRow row) {
			value = row.getObject(type.getColumn());
			sortorder = row.getInt("sortorder");
			if (type.isFilesystem()) {
				fileMd5 = row.getString("value_clob");
				fileLength = row.getLong("value_long");
			}
		}

		/**
		 * Get the sortorder of the value
		 * @return sortorder
		 */
		public int getSortorder() {
			return sortorder;
		}

		/**
		 * Get the value
		 * @return value
		 */
		public Object getValue() {
			return value;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			SortedValue ov = (SortedValue) o;

			return sortorder - ov.sortorder;
		}

		/**
		 * Helper method to add a list of sorted values to a contentobject
		 * @param contentObject content object
		 * @param attributeName name of the attribute
		 * @param sortedValues list holding instances of {@link SortedValue}
		 * @throws NodeIllegalArgumentException
		 * @throws CMSUnavailableException
		 */
		public static void addSortedValuesToContentObject(GenticsContentObject contentObject,
				String attributeName, List sortedValues) throws NodeIllegalArgumentException,
					CMSUnavailableException {
			if (sortedValues.size() > 1) {
				Collections.sort(sortedValues);
			}
			boolean notSorted = false;
			boolean filesystem = DatatypeHelper.isFilesystem(((CNDatasource) contentObject.getDatasource()).getHandle().getDBHandle(), attributeName);
			Object[] array = new Object[sortedValues.size()];
			int arrayIndex = 0;

			for (Iterator iter = sortedValues.iterator(); iter.hasNext();) {
				Object next = iter.next();

				if (next instanceof SortedValue) {
					SortedValue sortedValue = (SortedValue) next;

					array[arrayIndex++] = filesystem
							? new FilesystemAttributeValue(ObjectTransformer.getString(sortedValue.getValue(), null), sortedValue.fileMd5, sortedValue.fileLength)
							: sortedValue.getValue();
					notSorted = notSorted || sortedValue.getSortorder() != arrayIndex;
				} else {
					array[arrayIndex++] = filesystem ? new FilesystemAttributeValue(ObjectTransformer.getString(next, null), null, -1) : next;
				}
			}
			contentObject.setAttribute(attributeName, array);
			if (notSorted) {
				contentObject.setAttributeNeedsSortorderFixed(attributeName);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setCustomUpdatetimestamp(int)
	 */
	public void setCustomUpdatetimestamp(int timestamp) throws NodeIllegalArgumentException {
		if (datasource instanceof VersioningDatasource && ((VersioningDatasource) datasource).isVersioning()) {
			throw new NodeIllegalArgumentException("Versioning datasources do not support setting custom updatetimestamps");
		}

		customUpdatetimestamp = timestamp;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getCustomUpdatetimestamp()
	 */
	public int getCustomUpdatetimestamp() {
		return customUpdatetimestamp;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.resolving.FilterableResolvable#getFiltered(java.lang.String, java.lang.String)
	 */
	public DatasourceFilter getFiltered(String key, String filterString) throws DatasourceException {
		try {
			DBHandle handle = getDBHandle();
			int attrType = DatatypeHelper.getDatatype(handle, key);

			if (attrType == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
				String ruleTreeString = DatatypeHelper.getForeignLinkAttributeRuleTreeString(handle, key);
				String foreignFilterString;

				if (ObjectTransformer.isEmpty(filterString)) {
					foreignFilterString = ruleTreeString;
				} else {
					foreignFilterString = "(" + ruleTreeString + ") AND (" + filterString + ")";
				}
				Expression expression = ExpressionParser.getInstance().parse(foreignFilterString);
				DatasourceFilter filter = getDatasource().createDatasourceFilter(expression);

				filter.addBaseResolvable("data", this);
				return filter;
			} else if (attrType == GenticsContentAttribute.ATTR_TYPE_OBJ) {
				String linkFilterString = "object.contentid CONTAINSONEOF data." + key;

				if (!ObjectTransformer.isEmpty(filterString)) {
					linkFilterString = "(" + linkFilterString + ") AND (" + filterString + ")";
				}
				Expression expression = ExpressionParser.getInstance().parse(linkFilterString);
				DatasourceFilter filter = getDatasource().createDatasourceFilter(expression);

				filter.addBaseResolvable("data", this);
				return filter;
			} else {
				throw new IllegalArgumentException("Requested attribute is not a object link or foreign link {" + key + "}");
			}
		} catch (NodeException e) {
			throw new DatasourceException("Error while trying to filter attribute", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#getStreamableProperties()
	 */
	public Collection<String> getStreamableProperties() {
		try {
			AttributeType[] fileSystemTypes = DatatypeHelper.getAttributeTypes(handle, null, null, true, new int[] { m_type}, null, null);
			Collection<String> filesystem = new Vector<String>();

			for (AttributeType type : fileSystemTypes) {
				filesystem.add(type.getName());
			}
			return filesystem;
		} catch (CMSUnavailableException e) {
			logger.error("Error while getting filesystem attributes for type " + m_type, e);
			return Collections.emptyList();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#isStreamable(java.lang.String)
	 */
	public boolean isStreamable(String name) {
		try {
			// an attribute is streamable, if it is stored in the filesystem
			GenticsContentAttribute attribute = getAttribute(name);

			return attribute.isFilesystem();
		} catch (Exception e) {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#getNumStreams(java.lang.String)
	 */
	public int getNumStreams(String name) {
		try {
			// an attribute is streamable, if it is stored in the filesystem
			GenticsContentAttribute attribute = getAttribute(name);

			if (attribute.isFilesystem()) {
				return attribute.countValues();
			} else {
				return 0;
			}
		} catch (Exception e) {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#getInputStream(java.lang.String, int)
	 */
	public InputStream getInputStream(String name, int n) throws IOException,
				ArrayIndexOutOfBoundsException {
		try {
			// an attribute is streamable, if it is stored in the filesystem
			GenticsContentAttribute attribute = getAttribute(name);

			if (attribute.isFilesystem()) {
				return attribute.getInputStream(n);
			} else {
				throw new IOException("Error while getting stream for attribute '" + name + "': attribute is not streamable");
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Error while getting stream for attribute '" + name + "': " + e.getLocalizedMessage());
		}
	}

	/**
	 * Prepare the object to be cached.
	 * This method shall be called on the object instance before it is put into the cache.
	 */
	public void prepareForCaching() {
		// unset the datasource instance
		setDatasource(null);

 		m_contentId = null;
		m_mother_contentId = null;
		m_attributeHash = null;
		modifiedAttributeNames = null;
	}

	/**
	 * Prepare the object for use after it has been taken out of the cache
	 * @param ds datasource instance
	 */
	public void prepareForUseAfterCaching(Datasource ds) {
		// set the datasource instance
		setDatasource(ds);

		StringBuffer contentId = new StringBuffer(20);
		contentId.append(m_type).append(".").append(m_id);
		m_contentId = contentId.toString();
		if (m_mother_id != 0 && m_mother_type != 0) {
			StringBuffer motherId = new StringBuffer(20);

			motherId.append(m_mother_type).append(".").append(m_mother_id);
			m_mother_contentId = motherId.toString();
		} else {
			m_mother_contentId = "";
		}
		m_attributeHash = new HashMap(10);
	}
}
