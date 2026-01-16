/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: ValueFactory.java,v 1.29.2.1.4.3 2011-03-18 12:27:12 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils.BatchUpdater;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.EditableDefaultValue;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.lib.etc.StringUtils;

/**
 * An objectfactory to create {@link Value} objects, based on the {@link AbstractFactory}.
 *
 * This factory is able to preload values for content and templates.
 *
 * Preloading-statements seem to cost to much performance themselves
 * as to improve speed significantly. Therefore this factory does not implement
 * PreloadableObjectFactory, even if the methods are implemented.
 */
@DBTables({ @DBTable(clazz = Value.class, name = "value") })
public class ValueFactory extends AbstractFactory {

	/**
	 * SQL Statement to select value data
	 */
	protected final static String SELECT_VALUE_SQL = createSelectStatement("value");

	/**
	 * SQL Statement to batchload value
	 */
	protected final static String BATCHLOAD_VALUE_SQL = createBatchLoadStatement("value");

	/**
	 * SQL Statement to select versioned value data
	 */
	protected final static String SELECT_VERSIONED_VALUE_SQL = "SELECT * FROM value_nodeversion WHERE id = ? AND nodeversiontimestamp = "
			+ "(SELECT MAX(nodeversiontimestamp) FROM value_nodeversion "
			+ "WHERE (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? AND id = ? GROUP BY id)";

	/**
	 * SQL Params for the selection of versioned value data
	 */
	protected final static VersionedSQLParam[] SELECT_VERSIONED_VALUE_PARAMS = {
		VersionedSQLParam.ID, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.ID};

	private static final Class<? extends NodeObject>[] PRELOAD_TRIGGER = new Class[] { Content.class, Template.class};

	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.VALUE, Value.TYPE_VALUE, true, FactoryValue.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Implementation class for Values
	 */
	private static class FactoryValue extends Value {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -7156414240087306612L;

		@DataField("part_id")
		@Updateable
		protected Integer partId;

		@DataField("info")
		@Updateable
		protected int info;

		@DataField("static")
		@Updateable
		protected boolean isStatic;

		@DataField("value_text")
		@Updateable
		protected String valueText;

		@DataField("value_ref")
		@Updateable
		protected int valueRef;

		@DataField("contenttag_id")
		protected int contentTagId;

		@DataField("templatetag_id")
		protected int templateTagId;

		@DataField("objtag_id")
		protected int objTagId;

		protected Class<? extends ValueContainer> containerType;
		protected int containerId;

		protected final static String PART = "part";

		public FactoryValue(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap, int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);

			if (contentTagId > 0) {
				containerId = contentTagId;
				containerType = ContentTag.class;
			} else if (templateTagId > 0) {
				containerId = templateTagId;
				containerType = TemplateTag.class;
			} else if (objTagId > 0) {
				containerId = objTagId;
				containerType = ObjectTag.class;
			} else {
				containerType = Construct.class;
				Part part = getPart();

				containerId = ObjectTransformer.getInt(part.getConstructId(), -1);
			}
			this.udate = udate;
			this.globalId = globalId;
		}

		protected FactoryValue(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		public void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		public int getInfo() {
			return info;
		}

		public boolean isStatic() {
			return isStatic;
		}

		public String getValueText() {
			return valueText;
		}

		public int getValueRef() {
			return valueRef;
		}

		// TODO review implemented by fg
		/**
		 * Performs the delete of the Value
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			ValueFactory valueFactory = (ValueFactory) t.getObjectFactory(Value.class);

			valueFactory.deleteValue(this);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#getPart(boolean)
		 */
		public Part getPart(boolean checkForNull) throws NodeException {
			// use level2 cache here
			Transaction t = TransactionManager.getCurrentTransaction();
			Part part = (Part) t.getFromLevel2Cache(this, PART);

			if (part == null || (partId != null && !partId.equals(part.getId()))) {
				// if the part is loaded without checking for null values, warning messages
				// concerning missing parts will not be sent to the logs, as these may cause
				// massive log flooding if tag parts have been removed from constructs
				part = TransactionManager.getCurrentTransaction().getObject(Part.class, partId, false, true, checkForNull);
				if (checkForNull) {
					// check for data consistency
					assertNodeObjectNotNull(part, partId, "Part");
				}
				t.putIntoLevel2Cache(this, PART, part);
			}
			return part;            
		}

		public Integer getPartId() {
			return partId;
		}

		public ValueContainer getContainer() throws NodeException {
			ValueContainer container = (ValueContainer) TransactionManager.getCurrentTransaction().getObject(containerType, containerId,
					getObjectInfo().getVersionTimestamp());

			// check for data consistency
			assertNodeObjectNotNull(container, containerId, "Container");
			return container;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			try {
				Part part = getPart(false);

				if (part == null) {
					return "Value {[null-part], " + getId() + "}";
				} else {
					I18nString partName = part.getName();
					String name = null;

					if (partName == null) {
						name = part.getKeyname();
					} else {
						name = partName.toString();
					}
					return "Value {" + name + ", " + getId() + "}";
				}
			} catch (NodeException e) {
				return "Value {" + getId() + "}";
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryValue(this, getFactory().getFactoryHandle(Value.class).createObjectInfo(Value.class, true), true);
		}
	}

	/**
	 * Implementation class for editable Values
	 */
	private static class EditableFactoryValue extends FactoryValue implements EditableDefaultValue {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 4715508539771408716L;

		/**
		 * Flag to mark whether the value has been modified (contains changes which need to be persistet by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Editable container
		 */
		private ValueContainer container;

		/**
		 * Part of this value
		 */
		private Part part;

		/**
		 * Create an empty instance of an editable value
		 * @param info
		 */
		public EditableFactoryValue(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Constructor to create a copy of the given value
		 * @param value value to copy
		 * @param info info about the copy
		 * @param asNewValue true when the value shall be a new value, false for just the editable version of the value
		 */
		public EditableFactoryValue(FactoryValue value, NodeObjectInfo info, boolean asNewValue) throws NodeException {
			super(asNewValue ? null : value.getId(), info, getDataMap(value), asNewValue ? -1 : value.getUdate(), asNewValue ? null : value.getGlobalId());
			if (asNewValue) {
				// for new values, reset the container ids
				contentTagId = 0;
				templateTagId = 0;
				objTagId = 0;
				containerId = 0;
				containerType = null;
				this.modified = true;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#setInfo(int)
		 */
		public int setInfo(int info) throws ReadOnlyException {
			assertEditable();
			if (this.info != info) {
				int oldInfo = this.info;

				this.info = info;
				modified = true;
				return oldInfo;
			} else {
				return this.info;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#setValueText(java.lang.String)
		 */
		public String setValueText(String valueText) throws ReadOnlyException {
			assertEditable();
			valueText = ObjectTransformer.getString(valueText, "");
			if (!StringUtils.isEqual(ObjectTransformer.getString(this.valueText, ""), valueText)) {
				String oldValueText = this.valueText;

				this.valueText = valueText;
				modified = true;
				return oldValueText;
			} else {
				return this.valueText;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#setValueRef(int)
		 */
		public int setValueRef(int valueRef) throws ReadOnlyException {
			assertEditable();
			if (this.valueRef != valueRef) {
				int oldValueRef = this.valueRef;

				this.valueRef = valueRef;
				modified = true;
				return oldValueRef;
			} else {
				return this.valueRef;
			}
		}

		@Override
		public void setStatic(boolean stat) throws ReadOnlyException {
			if (this.isStatic != stat) {
				this.isStatic = stat;
				this.modified = true;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#setContainer(com.gentics.contentnode.object.ValueContainer)
		 */
		public ValueContainer setContainer(ValueContainer container) throws NodeException {
			assertEditable();
			Class<? extends ValueContainer> containerType = null;
			int containerId = 0;

			if (container != null) {
				containerType = (Class<ValueContainer>) (container.getObjectInfo().getObjectClass());
				containerId = ObjectTransformer.getInt(container.getId(), 0);
			}

			Class<? extends ValueContainer> oldContainerType = this.containerType;
			int oldContainerId = this.containerId;

			if (this.containerType != containerType || this.containerId != containerId) {
				if (this.containerId > 0) {
					throw new NodeException("Cannot change the container tag of " + this + " from " + this.containerId + " to " + containerId
							+ ". This might indicate a data inconcistency!");
				}

				// set the new container
				this.containerType = containerType;
				if (container instanceof ContentTag) {
					contentTagId = containerId;
				} else if (container instanceof TemplateTag) {
					templateTagId = containerId;
				} else if (container instanceof ObjectTag) {
					objTagId = containerId;
				}
				this.containerId = containerId;
				this.container = container;
				modified = true;
			}

			return (ValueContainer) TransactionManager.getCurrentTransaction().getObject(oldContainerType, oldContainerId);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.ValueFactory.FactoryValue#getContainer()
		 */
		public ValueContainer getContainer() throws NodeException {
			if (container == null) {
				container = super.getContainer();
			}
			return container;
		}

		@Override
		public Part getPart(boolean checkForNull) throws NodeException {
			if (part == null) {
				part = super.getPart(checkForNull);
			}
			return part;
		}

		@Override
		public void setPart(Part part) throws ReadOnlyException, NodeException {
			getPart(false);
			if (!ObjectTransformer.equals(this.part,part)) {
				this.part = part;
				this.modified = true;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#setPartId(java.lang.Integer)
		 */
		public Integer setPartId(Integer partId) throws ReadOnlyException {
			if (ObjectTransformer.getInt(this.partId, 0) != ObjectTransformer.getInt(partId, 0)) {
				Integer oldPartId = this.partId;

				this.partId = partId;
				this.part = null;
				modified = true;
				return oldPartId;
			} else {
				return this.partId;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			return saveBatch(null);
		}

		@Override
		public boolean saveBatch(BatchUpdater batchUpdater) throws InsufficientPrivilegesException, NodeException {
			assertEditable();

			boolean isModified = false;
			PartType type = getPartType();

			// do PartType specific saving before saving the value
			type.preSave(batchUpdater);

			if (modified) {
				// make sure, valueText is not null
				valueText = ObjectTransformer.getString(valueText, "");

				// if a part was set, we set the partId now
				if (isEmptyId(partId) && part != null) {
					partId = part.getId();
				}
				// cannot save value with empty partId
				if (isEmptyId(partId)) {
					throw new NodeException("Error while saving " + this + ": no partId was set");
				}

				// object is modified, so update it
				isModified = true;
				saveFactoryObject(this, batchUpdater);
				modified = false;
			}

			// do PartType specific saving after saving the value
			isModified |= type.postSave(batchUpdater);

			return isModified;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);
			Value oValue = (Value) original;

			// set meta information
			setInfo(oValue.getInfo());
			setPartId(oValue.getPartId());
			setValueRef(oValue.getValueRef());
			setValueText(oValue.getValueText());

			// copy parttype specific data (by copying the parttype)
			getPartType().copyFrom(oValue.getPartType());
		}

		/**
		 * Check whether this tag has been modified
		 * @return True if modified
		 */
		public boolean isModified() {
			return this.modified;
		}

		@Override
		public void setEditablePart(Part part) {
			this.part = part;
		}
	}

	public ValueFactory() {
		super();
	}

	@Override
	public boolean isVersioningSupported(Class<? extends NodeObject> clazz) {
		// value supports versioning
		return Value.class.equals(clazz);
	}

	// TODO review - written by fg
	/**
	 * Deletes a Value Object but instead of directly deleting the Value Object the delete action is cached.
	 * When the Transaction is committed the delete is performed.
	 * @param tag The tag to delete
	 */
	public void deleteValue(FactoryValue factoryValue) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection deleteList = getDeleteList(t, Value.class);

		deleteList.add(factoryValue);
	}
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#flush()
	 */
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, Value.class)) {
			flushDelete("DELETE FROM value WHERE id IN", Value.class);
			// flushDelete("DELETE FROM value_nodeversion WHERE id IN", Value.class);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#createObject(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class)
	 */
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) {
		if (Value.class.equals(clazz)) {
			return (T) new EditableFactoryValue(handle.createObjectInfo(Value.class, true));
		} else {
			return null;
		}
	}

	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		return loadDbObject(clazz, id, info, SELECT_VALUE_SQL, SELECT_VERSIONED_VALUE_SQL, SELECT_VERSIONED_VALUE_PARAMS);
	}

	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_VALUE_SQL + buildIdSql(ids));
	}

	public Class<? extends NodeObject>[] getPreloadTriggerClasses() {
		return PRELOAD_TRIGGER;
	}

	public void preload(Class<? extends NodeObject> objClass, Integer objId) throws NodeException {
		String sql;

		if (Content.class.equals(objClass)) {
			sql = "SELECT value.* FROM value, contenttag WHERE value.contenttag_id = contenttag.id AND " + "contenttag.content_id = ?";
		} else if (Template.class.equals(objClass)) {
			sql = "SELECT value.* FROM value, templatetag WHERE value.templatetag_id = templatetag.id AND " + "templatetag.template_id = ?";
		} else {
			return;
		}
		preloadDbObjects(Value.class, objId, sql, null);
	}

	public void preload(Class<? extends NodeObject> objClass, Collection<Integer> ids) throws NodeException {
		String sql;
		String idSql = buildIdSql(ids);

		if (Content.class.equals(objClass)) {
			sql = "SELECT value.* FROM value, contenttag WHERE value.contenttag_id = contenttag.id AND " + "contenttag.content_id IN " + idSql;
		} else if (Template.class.equals(objClass)) {
			sql = "SELECT value.* FROM value, templatetag WHERE value.templatetag_id = templatetag.id AND " + "templatetag.template_id IN " + idSql;
		} else {
			return;
		}
		preloadDbObjects(Value.class, sql, null);
	}

	@SuppressWarnings("unchecked")
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id,
			NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		return (T) new FactoryValue(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs, "value"));
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}

		if (object instanceof FactoryValue) {
			return (T) new EditableFactoryValue((FactoryValue) object, info, false);
		} else {
			throw new NodeException("ValueFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#batchLoadVersionedObjects(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class, java.lang.Class, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> Set<T> batchLoadVersionedObjects(FactoryHandle handle, Class<T> clazz, Class<? extends NodeObject> mainClazz,
			Map<Integer, Integer> timestamps) throws NodeException {
		if (Value.class.equals(clazz) && Content.class.equals(mainClazz)) {
			Set<T> preparedValues = new HashSet<T>();
			// we load the contenttags that exist for the CURRENT version of the page, which might be different than the version we are looking for.
			// the result is, that we probably do not prepare data for all contenttags, or prepare data for contenttags that don't exist in the version
			Map<Integer, Map<Integer, FactoryDataRow>> valueData = getVersionedData(
					"SELECT v.*, ct.content_id gentics_obj_id FROM value_nodeversion v, contenttag ct WHERE v.contenttag_id = ct.id AND ct.content_id IN",
					timestamps);

			for (Map.Entry<Integer, Map<Integer, FactoryDataRow>> rowMapEntry : valueData.entrySet()) {
				Integer mainObjId = rowMapEntry.getKey();
				int versionTimestamp = timestamps.get(mainObjId);
				Map<Integer, FactoryDataRow> rowMap = rowMapEntry.getValue();
				for (Map.Entry<Integer, FactoryDataRow> entry : rowMap.entrySet()) {
					Integer objId = entry.getKey();
					FactoryDataRow row = entry.getValue();

					try {
						Value value = loadResultSet(Value.class, objId, handle.createObjectInfo(Value.class, versionTimestamp), row, null);
						preparedValues.add((T)value);
						handle.putObject(Value.class, value, versionTimestamp);
					} catch (SQLException e) {
						throw new NodeException("Error while batchloading values", e);
					}
				}
			}

			return preparedValues;
		} else {
			return Collections.emptySet();
		}
	}
}
