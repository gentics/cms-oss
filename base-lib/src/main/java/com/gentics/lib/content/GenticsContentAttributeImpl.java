/*
 * @author erwin
 * @date 20.10.2003
 * @version $Id: GenticsContentAttributeImpl.java,v 1.2 2010-09-28 17:01:27 norbert Exp $
 */

package com.gentics.lib.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.datasource.VersioningDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.db.TableVersion;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class GenticsContentAttributeImpl implements GenticsContentAttribute, Cloneable, Serializable {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -2428910767370889178L;

	private transient GenticsContentObject m_parent;

	private String m_name;

	private int m_dataType;

	private boolean m_isInitialized = false;

	private Vector m_result = null;

	private transient Iterator m_iterator;

	private transient DBHandle m_handle;

	private boolean m_isMultivalue;

	/**
	 * Flag to mark attributes, that are stored in the filesystem
	 */
	private boolean filesystem;

	/**
	 * obj type of the linked object (if m_dataType is ATTR_TYPE_OBJ)
	 */
	private int m_linkedObjType;

	/**
	 * attribute of the foreign object linking to this object (if attribute is a
	 * foreign link)
	 */
	private String m_foreignLinkAttribute;

	/**
	 * flag to mark multivalue attributes that have values with incorrect set sortorder
	 */
	private boolean fixSortorder = false;

	protected final static Integer DEFAULT_INTEGER = new Integer(0);

	private class AttributeIterator implements Iterator {
		public static final int TYPE_VALUE = 1;

		public static final int TYPE_OBJECT = 2;

		public static final int TYPE_BINARY = 3;

		private Iterator m_iterator;

		private int m_type;

		public AttributeIterator(Iterator iterator, int type) {
			m_iterator = iterator;
			m_type = type;
		}

		public void remove() {
			throw new UnsupportedOperationException("You cannot remove an object's attribute");
		}

		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		public Object next() {
			try {
				switch (m_type) {
				case TYPE_VALUE:
					return getNextValue(m_iterator);

				case TYPE_OBJECT:
					return getNextContentObject(m_iterator);

				case TYPE_BINARY:
					return getNextBinaryValue(m_iterator);
				}
			} catch (CMSUnavailableException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * NOTE: the clone's resultset-pointer will be reset to the beginning (if
	 * initialized)!
	 * @param clone
	 */
	public GenticsContentAttributeImpl(GenticsContentAttributeImpl clone) {
		m_parent = clone.m_parent;
		m_name = clone.m_name;
		m_dataType = clone.m_dataType;
		m_isInitialized = clone.m_isInitialized;
		m_handle = clone.m_handle;
		m_isMultivalue = clone.m_isMultivalue;
		m_linkedObjType = clone.m_linkedObjType;
		m_foreignLinkAttribute = clone.m_foreignLinkAttribute;
		if (m_isInitialized) {
			// TODO check copying link attributes (clone the GenticsContentObjects?)
			m_result = new Vector(clone.m_result.size());
			m_result.addAll(clone.m_result);
			m_iterator = m_result.iterator();
		} else {
			m_result = new Vector();
			m_iterator = m_result.iterator();
		}
		fixSortorder = clone.fixSortorder;
		filesystem = clone.filesystem;
	}

	public GenticsContentAttributeImpl(GenticsContentObject parent, GenticsContentAttributeImpl clone, DBHandle handle) {
		m_parent = parent;
		m_name = clone.m_name;
		m_dataType = clone.m_dataType;
		m_isInitialized = clone.m_isInitialized;
		m_handle = handle;
		m_isMultivalue = clone.m_isMultivalue;
		m_linkedObjType = clone.m_linkedObjType;
		m_foreignLinkAttribute = clone.m_foreignLinkAttribute;
		if (m_isInitialized) {
			// check copying link attributes (clone the GenticsContentObjects?)
			m_result = new Vector(clone.m_result.size());
			m_result.addAll(clone.m_result);
			m_iterator = m_result.iterator();
		} else {
			m_result = new Vector();
			m_iterator = m_result.iterator();
		}
		fixSortorder = clone.fixSortorder;
		filesystem = clone.filesystem;
	}

	public GenticsContentAttributeImpl(GenticsContentObject parent, String name) throws CMSUnavailableException, NodeIllegalArgumentException {
		m_parent = parent;
		m_name = name;
		m_handle = ((GenticsContentObjectImpl) parent).getDBHandle();
		initialize();
	}

	public GenticsContentAttributeImpl(GenticsContentObject parent, DBHandle handle,
			String name, Object[] values, int dataType, boolean isMultivalue, boolean filesystem) {
		m_parent = parent;
		m_dataType = dataType;
		// attributes of type foreignobject are always multivalue
		m_isMultivalue = m_dataType == ATTR_TYPE_FOREIGNOBJ ? true : isMultivalue;
		this.filesystem = filesystem;
		m_isInitialized = true;
		m_handle = handle;
		m_name = name;
		m_result = new Vector();
		try {
			m_foreignLinkAttribute = DatatypeHelper.getForeignLinkAttribute(handle, name);
		} catch (Exception e) {
			NodeLogger.getLogger(getClass()).error("Error while creating content attribute", e);
		}
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				try {
					values[i] = normalizeValue(values[i]);
					if (ObjectTransformer.isEmpty(values[i])) {
						continue;
					}
				} catch (NodeIllegalArgumentException e) {
					continue;
				}
				if (this.filesystem) {
					FilesystemAttributeValue fsValue = null;

					if (values[i] instanceof FilesystemAttributeValue) {
						fsValue = (FilesystemAttributeValue) values[i];
					} else {
						fsValue = new FilesystemAttributeValue();
						fsValue.setData(values[i]);
					}
					m_result.add(fsValue);
				} else if (m_dataType == ATTR_TYPE_BLOB || m_dataType == ATTR_TYPE_OBJ || m_dataType == ATTR_TYPE_FOREIGNOBJ) {
					m_result.add(values[i]);
				} else if (m_dataType == ATTR_TYPE_INTEGER && values[i] instanceof Date) {
					// store a Date object into an integer field: store the
					// timestamp (in secs)
					m_result.add(new Integer(ObjectTransformer.getTimestamp(values[i], 0)));
				} else if (m_dataType == ATTR_TYPE_INTEGER) {
					// do not add null values (null means: the value is not set)
					if (values[i] != null) {
						// all other values are transformed to integers, non-integers to the default
						m_result.add(ObjectTransformer.getInteger(values[i], DEFAULT_INTEGER));
					}
				} else if (m_dataType == ATTR_TYPE_LONG) {
					Object val = values[i];

					if (val instanceof Long || val instanceof Integer) {
						m_result.add(val);
					} else if (val != null) {
						m_result.add(new Long(ObjectTransformer.getLong(val, 0)));
					}
				} else if (m_dataType == ATTR_TYPE_DOUBLE) {
					Object val = values[i];

					if (val instanceof Float || val instanceof Double) {
						m_result.add(val);
					} else if (val != null) {
						m_result.add(new Double(ObjectTransformer.getDouble(val, 0)));
					}
				} else if (m_dataType == ATTR_TYPE_DATE) {
					Object val = values[i];

					if (val instanceof Date) {
						m_result.add(val);
					} else if (val != null) {
						long timestamp = ((long) ObjectTransformer.getInt(val, 0)) * 1000;

						m_result.add(new Date(timestamp));
					}
				} else {
					if (values[i] != null) {
						if (values[i] instanceof byte[]) {
							try {
								String s = new String((byte[]) values[i], "utf-8");

								m_result.add(s);
							} catch (UnsupportedEncodingException e) {}
						} else {
							String s = values[i].toString();

							m_result.add(s);
						}
					}
				}
			}
		}
		m_iterator = m_result.iterator();
	}

	/**
	 * Hopefully initialize the attribute data for the given type column
	 * @param typeColumn name of the type column
	 * @throws SQLException
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	private void initializeAttributeData(String typeColumn) throws SQLException,
				CMSUnavailableException, NodeIllegalArgumentException, NodeException {
		if (filesystem) {
			// special case: the attribute is stored in the filesystem
			m_result = new Vector();
			DB.query(m_handle,
					"SELECT value_text, value_clob, value_long, sortorder FROM " + m_handle.getContentAttributeName()
					+ " WHERE contentid = ? AND name = ? ORDER BY sortorder",
					new Object[] { m_parent.getContentId(), m_name },
					new ResultProcessor() {
				public void takeOver(ResultProcessor p) {}

				public void process(ResultSet rs) throws SQLException {
					while (rs.next()) {
						m_result.add(new FilesystemAttributeValue(rs.getString("value_text"), rs.getString("value_clob"), rs.getLong("value_long")));
					}
				}
			});
			m_isInitialized = true;
			m_iterator = m_result.iterator();
		} else if (typeColumn.length() > 0) {
			String sql;
			Object[] params;
			String optimizedCol = DatatypeHelper.getQuickColumn(m_handle, m_name);

			if (optimizedCol == null) {
				sql = "SELECT " + typeColumn + ", sortorder FROM " + m_handle.getContentAttributeName() + " WHERE contentid=? AND name=? ORDER BY sortorder";
				params = new Object[] { m_parent.getContentId(), m_name };
			} else {
				typeColumn = optimizedCol;
				sql = "SELECT " + optimizedCol + " FROM " + m_handle.getContentMapName() + " WHERE contentid=?";
				params = new Object[] { m_parent.getContentId() };
			}
			SimpleResultProcessor result = new SimpleResultProcessor();

			// check whether we have to get another than the current version
			if (!m_parent.isCurrentVersion()) {
				TableVersion tv = new TableVersion();

				tv.setHandle(m_handle);
				if (optimizedCol == null) {
					tv.setTable(m_handle.getContentAttributeName());
					tv.setWherePart("gentics_main.contentid = ? AND gentics_main.name = ?");
				} else {
					tv.setTable(m_handle.getContentMapName());
					tv.setWherePart("gentics_main.contentid = ?");
				}
				result = tv.getVersionData(params, m_parent.getVersionTimestamp());
			} else {
				DB.query(this.m_handle, sql, params, result);
			}
			// ServletPrinter.out("TIME: " + (t2 - t1) + "ms (SQL: " + sql +
			// ")<br>");
			m_isInitialized = true;
			m_result = new Vector();
			Iterator resultIterator = result.iterator();
			int valueCounter = 0;

			while (resultIterator.hasNext()) {
				SimpleResultRow row = (SimpleResultRow) resultIterator.next();

				valueCounter++;

				// check whether the sortorder is correctly set (for multivalue
				// attributes)
				if (!fixSortorder && m_isMultivalue && valueCounter != row.getInt("sortorder")) {
					fixSortorder = true;
				}

				switch (m_dataType) {
				case ATTR_TYPE_BLOB:
				case ATTR_TYPE_LONG:
				case ATTR_TYPE_DOUBLE:
				case ATTR_TYPE_DATE: {
					// do not add null values here
					Object value = row.getObject(typeColumn);

					if (value != null) {
						m_result.add(value);
					}
					break;
				}

				case ATTR_TYPE_INTEGER:
					if (m_parent.getDatasource() instanceof CNDatasource && ((CNDatasource) (m_parent.getDatasource())).isTreatIntegerAsString()) {
						// do not add null values here
						String value = row.getString(typeColumn);

						if (value != null) {
							m_result.add(value);
						}
					} else {
						// do not add null values here
						Object value = row.getObject(typeColumn);

						if (value != null) {
							m_result.add(value);
						}
					}
					break;

				default: {
					// do not add null values here
					String value = row.getString(typeColumn);

					if (!StringUtils.isEmpty(value)) {
						m_result.add(value);
					}
					break;
				}
				}
			}
			m_iterator = m_result.iterator();
		} else if (m_parent.getDatasource() != null) {
			// check that the attribute is a foreign linked attribute and use the datasource to get the foreign linked objects
			// int foreignDataType = DatatypeHelper.getDatatype(m_handle,
			// m_foreignLinkAttribute);
			// if (foreignDataType == GenticsContentAttribute.ATTR_TYPE_UNKOWN)
			// throw new NodeIllegalArgumentException("Attribute " + foreignDataType
			// + " is not defined!");
			// typeColumn = DatatypeHelper.getTypeColumn(foreignDataType);
			// if (typeColumn == null)
			// throw new NodeIllegalArgumentException("Attribute " + foreignDataType
			// + " is defined incorrectly!");

			// clone the datasource here, since we might modify the versiontimestamp
			Datasource ds;

			try {
				ds = (Datasource) m_parent.getDatasource().clone();
			} catch (CloneNotSupportedException e1) {
				// this will never happen, but throw the exception anyway
				throw new CMSUnavailableException("Error while fetching foreign linked objects", e1);
			}

			// check for versioning
			if (ds instanceof VersioningDatasource) {
				// set the parents versiontimestamp into the datasource
				((VersioningDatasource) ds).setVersionTimestamp(m_parent.getVersionTimestamp());
			} else if (!m_parent.isCurrentVersion()) {
				// the parent is not the current version, but the datasource is
				// not versioning -> something's wrong here
				NodeLogger.getLogger(getClass()).error("cannot get versioned objects of foreignlinkattribute '" + m_name + "'; datasource is not versioning");
				return;
			}

			// get the objects from the datasource
			try {
				RuleTree ruleTree = DatatypeHelper.getForeignLinkAttributeRuleTree(m_handle, m_name, m_parent);

				ruleTree.addResolver("data", m_parent);
				ds.setRuleTree(ruleTree);
				Collection result = ds.getResult();

				m_result = new Vector();
				for (Iterator resultIterator = result.iterator(); resultIterator.hasNext();) {
					DatasourceRow row = (DatasourceRow) resultIterator.next();

					// m_result.add(((GenticsContentObject)row.toObject()).getContentId());
					m_result.add(row.toObject());
				}
				m_iterator = m_result.iterator();
				m_isInitialized = true;

			} catch (Exception e) {
				NodeLogger.getLogger(getClass()).error("error while getting objects for attribute '" + m_name + "'", e);
			}

		} else {
			// TODO NOP: find a better solution for this!
			// TODO NOP: get versioned foreign links (when
			// !m_parent.isCurrentVersion())
			// get foreign objects that link to this object
			int foreignDataType = DatatypeHelper.getDatatype(m_handle, m_foreignLinkAttribute);

			if (foreignDataType == GenticsContentAttribute.ATTR_TYPE_UNKOWN) {
				throw new NodeIllegalArgumentException("Attribute " + foreignDataType + " is not defined!");
			}
			typeColumn = DatatypeHelper.getTypeColumn(foreignDataType);
			if (typeColumn == null) {
				throw new NodeIllegalArgumentException("Attribute " + foreignDataType + " is defined incorrectly!");
			}

			// check for an eventually existing rule
			if (DatatypeHelper.hasForeignLinkAttributeRule(m_handle, m_name)) {
				NodeLogger.getLogger(getClass()).error(
						"cannot fetch objects for foreignlinkattribute '" + m_name + "'; rule defined, but contentobject was not created with datasource set");
				return;
			}

			SimpleResultProcessor result = new SimpleResultProcessor();

			Object[] params = new Object[] { m_linkedObjType + ".%", m_foreignLinkAttribute, m_parent.getContentId() };

			if (!m_parent.isCurrentVersion()) {
				TableVersion tv = new TableVersion();

				tv.setHandle(m_handle);
				tv.setTable(m_handle.getContentAttributeName());
				tv.setWherePart("gentics_main.contentid LIKE ? AND gentics_main.name = ? AND gentics_main." + typeColumn + " = ?");
				result = tv.getVersionData(params, m_parent.getVersionTimestamp());

			} else {
				String sql = "SELECT contentid from " + m_handle.getContentAttributeName() + " WHERE contentid LIKE ? AND name = ? AND " + typeColumn
						+ " = ? group by contentid";

				DB.query(this.m_handle, sql, params, result);
			}

			m_isInitialized = true;
			m_result = new Vector();
			Iterator resultIterator = result.iterator();

			while (resultIterator.hasNext()) {
				SimpleResultRow row = (SimpleResultRow) resultIterator.next();
				String foreignLinkedContentId = row.getString("contentid");

				if (!m_result.contains(foreignLinkedContentId)) {
					m_result.add(foreignLinkedContentId);
				}
			}
			m_iterator = m_result.iterator();
		}
	}

	private void initialize() throws CMSUnavailableException, NodeIllegalArgumentException {
		if (m_isInitialized) {
			return;
		}
		try {
			RuntimeProfiler.beginMark(ComponentsConstants.GENTICSCONTENTATTRIBUTE_INITIALIZE, m_handle.getName() + "/" + m_name);
			m_dataType = DatatypeHelper.getDatatype(m_handle, m_name);
			// foreign linked attributes are always multivalue
			m_isMultivalue = m_dataType == ATTR_TYPE_FOREIGNOBJ ? true : DatatypeHelper.isMultivalue(m_handle, m_name);
			filesystem = DatatypeHelper.isFilesystem(m_handle, m_name);
			if (m_dataType == GenticsContentAttribute.ATTR_TYPE_UNKOWN) {
				throw new NodeIllegalArgumentException("Attribute " + m_name + " is not defined!");
			}
			String typeColumn = DatatypeHelper.getTypeColumn(m_dataType);

			if (typeColumn == null) {
				throw new NodeIllegalArgumentException("Attribute " + m_name + " is defined incorrectly!");
			}
			m_linkedObjType = DatatypeHelper.getLinkedObjectType(m_handle, m_name);
			m_foreignLinkAttribute = DatatypeHelper.getForeignLinkAttribute(m_handle, m_name);

			// only initialize the attribute data, when the parent object really exists
			if (m_parent.exists()) {
				initializeAttributeData(typeColumn);
			} else {
				// initialize empty
				m_result = new Vector();
				m_iterator = m_result.iterator();
			}

		} catch (SQLException e) {
			throw new CMSUnavailableException("could not initialize Attribute (" + m_name + ") : " + e.getMessage(), e);
		} catch (NodeIllegalArgumentException e) {
			throw e;
		} catch (NodeException e) {
			throw new CMSUnavailableException("could not initialize Attribute (" + m_name + ") : " + e.getMessage(), e);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.GENTICSCONTENTATTRIBUTE_INITIALIZE, m_handle.getName() + "/" + m_name);
		}
	}

	public GenticsContentObject getParent() {
		return m_parent;
	}

	/**
	 * Set a new parent for the attribute. When the parent is set to null, also
	 * the db handle is set to null.
	 * @param parent new parent (or null)
	 */
	public void setParent(GenticsContentObject parent) {
		m_parent = parent;
		if (m_parent != null) {
			m_handle = ((GenticsContentObjectImpl) parent).getDBHandle();
		} else {
			m_handle = null;
		}
	}

	public int getAttributeType() {
		switch (m_dataType) {
		case ATTR_TYPE_TEXT:
		case ATTR_TYPE_TEXT_LONG:
			return ATTR_TYPE_TEXT;

		case ATTR_TYPE_OBJ:
			return ATTR_TYPE_OBJ;

		default:
			return m_dataType;
		}
	}

	public boolean isMultivalue() {
		return m_isMultivalue;
	}

	public int getRealAttributeType() {
		return m_dataType;
	}

	public String getAttributeName() {
		return m_name;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#getForeignLinkAttribute()
	 */
	public String getForeignLinkAttribute() {
		return m_foreignLinkAttribute;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#getLinkedObjectType()
	 */
	public int getLinkedObjectType() {
		return m_linkedObjType;
	}

	public String getNextValue() throws CMSUnavailableException {
		return getNextValue(m_iterator);
	}

	public String getNextValue(Iterator it) throws CMSUnavailableException {
		if (it == null || !it.hasNext()) {
			return null;
		}
		Object next = it.next();

		if (next instanceof String) {
			return (String) next;
		} else if (next != null) {
			return next.toString();
		} else {
			return "";
		}
	}

	public Object getNextObjectValue() throws CMSUnavailableException {
		return getNextObjectValue(m_iterator);
	}

	public Object getNextObjectValue(Iterator it) throws CMSUnavailableException {
		if (!it.hasNext()) {
			return null;
		}
		return it.next();
	}

	public byte[] getNextBinaryValue() throws CMSUnavailableException {
		return getNextBinaryValue(m_iterator);
	}

	private byte[] getNextBinaryValue(Iterator it) throws CMSUnavailableException {
		if (!it.hasNext()) {
			return null;
		} else {
			Object o = it.next();

			if (o instanceof byte[]) {
				return (byte[]) o;
			} else {
				return null;
			}
		}
	}

	public String toString() {
		if (isMultivalue()) {
			return m_result != null ? m_result.toString() : "";
		} else if (m_result != null && m_result.size() > 0) {
			return m_result.get(0) != null ? m_result.get(0).toString() : "";
		} else {
			return "";
		}
	}

	private boolean isObjectType() {
		return m_dataType == GenticsContentAttribute.ATTR_TYPE_OBJ || m_dataType == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ;
	}

	public GenticsContentObject getNextContentObject() throws CMSUnavailableException {
		return getNextContentObject(m_iterator);
	}

	private GenticsContentObject getNextContentObject(Iterator it) throws CMSUnavailableException {
		if (!isObjectType()) {
			throw new IllegalStateException("Attribute (" + m_name + ") for contentId (" + m_parent.getContentId() + ") is not an Object-Attribute.");
		}
		if (!it.hasNext()) {
			return null;
		}

		Object nextObject = it.next();

		if (nextObject instanceof GenticsContentObject) {
			return (GenticsContentObject) nextObject;
		} else if (nextObject instanceof String) {
			String ret = (String) nextObject;

			try {
				if (ret == null) {
					return null;
				}
				if (ret.length() == 0) {
					return null;
				}
				GenticsContentObject newObject = null;

				// ensure that for past/future versions of the object also the
				// linked objects are shown in the same version
				if (m_parent.isCurrentVersion()) {
					if (m_parent.getDatasource() != null) {
						newObject = GenticsContentFactory.createContentObject(ret, m_parent.getDatasource());
					} else {
						newObject = GenticsContentFactory.createContentObject(ret, m_handle);
					}
				} else {
					if (m_parent.getDatasource() != null) {
						newObject = GenticsContentFactory.createContentObject(ret, m_parent.getDatasource(), m_parent.getVersionTimestamp(), false);
					} else {
						newObject = GenticsContentFactory.createContentObject(ret, m_handle, m_parent.getVersionTimestamp());
					}
				}
				if (newObject != null && newObject.getObjectId() <= 0) {
					// Object link points to a 'null' object. (CN backward
					// compatbility)
					newObject = null;
				}

				if (newObject == null && m_parent.getDatasource() instanceof CNDatasource) {
					CNDatasource cnDatasource = (CNDatasource) m_parent.getDatasource();

					if (cnDatasource.isGetIllegalLinksAsDummyObjects() && ret.indexOf('.') >= 0) {
						newObject = new DummyGenticsContentObject(ret);
					}
				}

				return newObject;
			} catch (NodeIllegalArgumentException e) {
				// must not happen, otherwise database is corrupt!

				throw new CMSUnavailableException("Could not create Object " + ret, e);
			}
		} else {
			return null;
		}
	}

	public Iterator binaryIterator() {

		/*
		 * todo implement + extend Interface to reflect change (implement so
		 * multi-values can have null-values)
		 */
		return new AttributeIterator(m_result.iterator(), AttributeIterator.TYPE_BINARY);
	}

	public Iterator valueIterator() {
		return new AttributeIterator(m_result.iterator(), AttributeIterator.TYPE_VALUE);
	}

	public Iterator objectIterator() {
		return new AttributeIterator(m_result.iterator(), AttributeIterator.TYPE_OBJECT);
	}

	public int countValues() {
		if (!this.isMultivalue()) {
			return 1;
		}
		return this.m_result.size();
	}

	/**
	 * reset the internal iterator (such that at the next invocation of any
	 * getNext** method, the first object will be returned)
	 */
	public void resetIterator() {
		m_iterator = m_result.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#isSet()
	 */
	public boolean isSet() {
		return m_result.size() > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#getValues()
	 */
	public List getValues() {
		return Collections.unmodifiableList(m_result);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#getFSValues()
	 */
	public List<FilesystemAttributeValue> getFSValues() throws DatasourceException {
		if (!filesystem) {
			throw new DatasourceException("Attribute " + m_name + " does not store in the filesystem");
		}
		List<FilesystemAttributeValue> retVal = new Vector<FilesystemAttributeValue>();

		for (Object value : m_result) {
			if (value instanceof FilesystemAttributeValue) {
				retVal.add((FilesystemAttributeValue) value);
			} else {
				throw new DatasourceException("Found value of " + value.getClass() + " in attribute " + m_name + " that stores in the filesystem");
			}
		}
		return retVal;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#getInputStream(int)
	 */
	public InputStream getInputStream(int n) throws IOException,
				ArrayIndexOutOfBoundsException {
		if (filesystem) {
			// check for out of bounds situations
			List<?> values = getValues();
			int numValues = values.size();

			if (n < 0 || n >= numValues) {
				throw new ArrayIndexOutOfBoundsException(
						"Error while gettin stream for value " + n + " of attribute " + m_name + ": only " + numValues + " values available");
			}
			Object value = values.get(n);

			if (value instanceof FilesystemAttributeValue) {
				FilesystemAttributeValue fsValue = (FilesystemAttributeValue) value;
				CNDatasource ds = getDatasource();

				if (ds == null) {
					throw new IOException("Error while getting stream for attribute " + m_name + ": Datasource not set");
				}
				String basePath = ds.getAttributePath();

				if (StringUtils.isEmpty(basePath)) {
					throw new IOException("Error while getting stream for attribute " + m_name + ": basepath not set for datasource");
				}

				return fsValue.getInputStream(this, basePath);
			} else {
				throw new IOException("Error while getting stream for attribute " + m_name + ": attribute not initialized correctly");
			}
		} else {
			throw new IOException("Error while getting stream for attribute " + m_name + ": Attribute is not stored in the filesystem");
		}
	}

	/**
	 * Get the CNDatasource instance of this attribute or null
	 * @return CNDatasource instance or null
	 */
	protected CNDatasource getDatasource() {
		if (m_parent == null) {
			return null;
		} else {
			Datasource ds = m_parent.getDatasource();

			if (ds instanceof CNDatasource) {
				return (CNDatasource) ds;
			} else {
				return null;
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#equals(com.gentics.lib.content.GenticsContentAttribute)
	 */
	public boolean equals(GenticsContentAttribute attribute) {
		if (attribute == null) {
			return false;
		}
		// compare the name
		if (!attribute.getAttributeName().equals(getAttributeName())) {
			return false;
		}

		// TODO implement a good comparison between attribute values here
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	protected Object clone() throws CloneNotSupportedException {
		// TODO better use another copy-constructor that clones also GenticsContentObjects?
		return new GenticsContentAttributeImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#isCacheable()
	 */
	public boolean isCacheable() {
		if (m_dataType == ATTR_TYPE_FOREIGNOBJ) {
			// for foreign linked attributes check the datasource
			if (m_parent != null) {
				Datasource ds = m_parent.getDatasource();

				if (ds instanceof CNDatasource) {
					// check the datasource setting
					return ((CNDatasource) ds).isCacheForeignLinkAttributes();
				}
			}
			// when the parent or datasource could not be fetched, the foreign
			// linked attribute is not cacheable (although this should never
			// happen)
			return false;
		} else {
			// all attributes but foreign link attributes are always cacheable
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#isFilesystem()
	 */
	public boolean isFilesystem() {
		return filesystem;
	}

	/**
	 * Prepare the attribute to be cached.
	 * This method shall be called on the attribute instance before it is put into the cache.
	 */
	public void prepareForCaching() {
		// when the attribute is a foreign linked attribute, replace the
		// linked objects by their contentids (as strings) and store them.
		if (m_dataType == ATTR_TYPE_FOREIGNOBJ) {
			Vector newResult = new Vector(m_result.size());

			for (Iterator iter = m_result.iterator(); iter.hasNext();) {
				Object element = (Object) iter.next();

				if (element instanceof GenticsContentObject) {
					newResult.add(((GenticsContentObject) element).getContentId());
				} else if (element instanceof String) {
					newResult.add(element);
				} else {
					NodeLogger.getLogger(getClass()).error(
							"Error while preparing foreign linked attribute {" + m_name + "} for caching: value is neither String nor GenticsContentObject");
				}
			}

			m_result = newResult;
			m_iterator = m_result.iterator();
		}
	}

	/**
	 * Get the cachable values
	 * @return cachable values
	 */
	public Object getCachableValue() {
		List<Object> cachableValues = null;

		// when the attribute is a foreign linked attribute, replace the
		// linked objects by their contentids (as strings) and store them.
		if (m_dataType == ATTR_TYPE_FOREIGNOBJ) {
			cachableValues = new Vector(m_result.size());

			for (Iterator iter = m_result.iterator(); iter.hasNext();) {
				Object element = (Object) iter.next();

				if (element instanceof GenticsContentObject) {
					cachableValues.add(((GenticsContentObject) element).getContentId());
				} else if (element instanceof String) {
					cachableValues.add(element);
				} else {
					NodeLogger.getLogger(getClass()).error(
							"Error while preparing foreign linked attribute {" + m_name + "} for caching: value is neither String nor GenticsContentObject");
				}
			}
		} else {
			cachableValues = new ArrayList<Object>(m_result);
		}
		if (cachableValues.isEmpty()) {
			return GenticsContentFactory.CACHED_NULL;
		} else {
			if (cachableValues.size() == 1) {
				return cachableValues.get(0);
			} else {
				return (Object[]) cachableValues.toArray(new Object[cachableValues.size()]);
			}
		}
	}

	/**
	 * Prepare the attribute for use after it has been taken out of the cache
	 */
	public void prepareForUseAfterCaching() {
		// when the attribute is a foreign linked attribute, replace the
		// contentids by the linked objects
		if (m_dataType == ATTR_TYPE_FOREIGNOBJ) {
			Vector newResult = new Vector(m_result.size());

			for (Iterator iter = m_result.iterator(); iter.hasNext();) {
				Object element = (Object) iter.next();

				if (element instanceof String) {
					try {
						newResult.add(GenticsContentFactory.createContentObject(element.toString(), m_parent.getDatasource(), m_parent.getVersionTimestamp()));
					} catch (Exception e) {
						NodeLogger.getLogger(getClass()).error("Error while preparing foreign linked attribute {" + m_name + "} for usage after cache: ", e);
					}
				} else {
					NodeLogger.getLogger(getClass()).error(
							"Error while preparing foreign linked attribute {" + m_name + "} for usage after cache: value is not a String");
				}
			}

			m_result = newResult;
			m_iterator = m_result.iterator();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#normalizeValue(java.lang.Object)
	 */
	public Object normalizeValue(Object value) throws NodeIllegalArgumentException {
		if (value instanceof FilesystemAttributeValue) {
			FilesystemAttributeValue fsValue = (FilesystemAttributeValue) value;

			fsValue.setData(normalizeValue(fsValue.getData()), fsValue.getMD5(), fsValue.getLength());
			return fsValue;
		} else if (value instanceof Collection) {
			// value is a collection
			Collection colValue = (Collection) value;

			if (!isMultivalue()) {
				// attribute is not multivalue, so only take the first value
				// get the first value (if any) and normalize it
				if (colValue.size() > 0) {
					return normalizeValue(colValue.iterator().next());
				} else {
					// no value at all, value is null
					return null;
				}
			} else {
				// generate a collection of normalized objects
				Collection normalizedValues = new Vector();

				for (Iterator iter = colValue.iterator(); iter.hasNext();) {
					normalizedValues.add(normalizeValue(iter.next()));
				}

				return normalizedValues;
			}
		} else if (value != null) {
			// for filesystem attributes, we will accept files as data
			if (filesystem && value instanceof File) {
				return value;
			}
			switch (m_dataType) {
			case ATTR_TYPE_TEXT:
			case ATTR_TYPE_TEXT_LONG:
				// do the same as in #initializeAttributeData( ... ) return null if it is an empty string.
				String strValue = ObjectTransformer.getString(value, null);

				if (StringUtils.isEmpty(strValue)) {
					return null;
				}
				return strValue;

			case ATTR_TYPE_OBJ:
				// TODO allow Strings (contentid) and GenticsContentObjects here
				return value;

			case ATTR_TYPE_INTEGER:
				if (value instanceof Boolean) {
					value = ((Boolean) value).booleanValue() ? 1 : 0;
				}
				Integer intValue = ObjectTransformer.getInteger(value, null);

				if (intValue == null) {
					throw new NodeIllegalArgumentException("Failed to normalize object of class {" + value.getClass().getName() + "} to integer");
				} else {
					return intValue;
				}

			case ATTR_TYPE_BLOB:
				// TODO shall we check for binary data here?
				return value;

			case ATTR_TYPE_FOREIGNOBJ:
				// TODO only allow GenticsContentObjects here
				return value;

			case ATTR_TYPE_LONG:
				Long longValue = ObjectTransformer.getLong(value, null);

				if (longValue == null) {
					throw new NodeIllegalArgumentException("Failed to normalize object of class {" + value.getClass().getName() + "} to long");
				} else {
					return longValue;
				}

			case ATTR_TYPE_DOUBLE:
				Double doubleValue = ObjectTransformer.getDouble(value, null);

				if (doubleValue == null) {
					throw new NodeIllegalArgumentException("Failed to normalize object of class {" + value.getClass().getName() + "} to double");
				} else {
					return doubleValue;
				}

			case ATTR_TYPE_DATE:
				Date dateValue = ObjectTransformer.getDate(value, null);

				if (dateValue == null) {
					throw new NodeIllegalArgumentException("Failed to normalize object of class {" + value.getClass().getName() + "} to date");
				} else {
					return dateValue;
				}

			default:
				return value;
			}
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentAttribute#needsSortorderFixed()
	 */
	public boolean needsSortorderFixed() {
		return fixSortorder;
	}
    
	public void setNeedsSortorderFixed() {
		fixSortorder = true;
	}
}
