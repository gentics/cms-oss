/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: PartFactory.java,v 1.30.2.2 2011-02-10 13:43:41 tobiassteiner Exp $
 */
package com.gentics.contentnode.factory.object;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.ObjectFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.i18n.EditableI18nString;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.DummyObject;
import com.gentics.contentnode.object.EditableDefaultValue;
import com.gentics.contentnode.object.MarkupLanguage;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * An objectfactory which can create {@link Part} objects, based on the
 * {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = Part.class, name = "part") })
public class PartFactory extends AbstractFactory {
	/**
	 * SQL Statement to select a part
	 */
	protected final static String SELECT_PART_SQL = "SELECT part.*, value.id AS value_id FROM part "
			+ "LEFT JOIN value ON (part.id = value.part_id AND value.templatetag_id = 0 AND " + "value.contenttag_id = 0 AND value.objtag_id = 0) WHERE part.id = ?";

	/**
	 * SQL Statement to batchload parts
	 */
	protected final static String BATCHLOAD_PART_SQL = "SELECT part.*, value.id AS value_id FROM part "
			+ "LEFT JOIN value ON (part.id = value.part_id AND value.templatetag_id = 0 AND " + "value.contenttag_id = 0 AND value.objtag_id = 0) WHERE part.id IN ";

	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.PART, Part.TYPE_PART, true, FactoryPart.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Factory Implementation for Parts
	 */
	private static class FactoryPart extends Part {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -3173123524295792851L;

		protected I18nString name;

		@DataField("name_id")
		protected int nameId;

		@DataField("keyword")
		@Updateable
		protected String keyword;

		@DataField("hidden")
		@Updateable
		protected boolean hidden;

		@DataField("editable")
		@Updateable
		protected int editable;

		@DataField("required")
		@Updateable
		protected boolean required;

		@DataField("construct_id")
		protected Integer constructId;

		@DataField("partorder")
		@Updateable
		protected int partOrder;

		@DataField("type_id")
		@Updateable
		protected int partTypeId;

		@DataField("partoption_id")
		@Updateable
		protected int partOptionId;

		@DataField("ml_id")
		@Updateable
		protected Integer mlId;

		@DataField("info_int")
		@Updateable
		protected int infoInt;

		protected int defaultValueId;

		@DataField("info_text")
		@Updateable
		protected String infoText;

		protected URI policyURI;

		@DataField("policy")
		@Updateable
		protected String policy;

		@DataField("hide_in_editor")
		@Updateable
		protected boolean hideInEditor;

		@DataField("external_editor_url")
		@Updateable
		protected String externalEditorUrl = "";

		protected final static String DEFAULT_VALUE = "defaultvalue";

		public FactoryPart(NodeObjectInfo info) {
			super(null, info);
		}

		public FactoryPart(Integer id, NodeObjectInfo info, Map<String, Object> dataMap,
				int udate, GlobalId globalId, int defaultValueId) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
			this.policyURI = newPolicyURI(id, this.policy);
			this.name = new CNI18nString(Integer.toString(nameId));
			this.udate = udate;
			this.globalId = globalId;
			this.defaultValueId = defaultValueId;
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		@SuppressWarnings("unused")
		public void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		public int getNameId() {
			return nameId;
		}

		public I18nString getName() {
			return name;
		}

		public String getKeyname() {
			return keyword;
		}

		public boolean isHidden() {
			return hidden;
		}

		public boolean isEditable() {
			return editable > 0;
		}

		@Override
		public int getEditable() {
			return editable;
		}

		public boolean isRequired() {
			return required;
		}

		public Integer getConstructId() {
			return constructId;
		}

		public Construct getConstruct() throws NodeException {
			Construct construct = (Construct) TransactionManager.getCurrentTransaction().getObject(Construct.class, constructId);

			// check data consistency
			assertNodeObjectNotNull(construct, constructId, "Construct");
			return construct;
		}

		public int getPartOrder() {
			return partOrder;
		}

		public int getPartTypeId() {
			return partTypeId;
		}

		public int getPartoptionId() {
			return partOptionId;
		}

		public Integer getMlId() {
			return mlId;
		}

		public MarkupLanguage getMarkupLanguage() throws NodeException {
			MarkupLanguage ml = (MarkupLanguage) TransactionManager.getCurrentTransaction().getObject(MarkupLanguage.class, mlId);

			// check data consistency
			assertNodeObjectNotNull(ml, mlId, "MarkupLanguage", true);
			return ml;
		}

		public int getInfoInt() {
			return infoInt;
		}

		public String getInfoText() {
			return infoText;
		}

		public URI getPolicyURI() {
			return policyURI;
		}

		@Override
		public String getPolicy() {
			return policy;
		}

		@Override
		public boolean isHideInEditor() {
			return hideInEditor;
		}

		@Override
		public String getExternalEditorUrl() {
			return externalEditorUrl;
		}

		public Value getDefaultValue() throws NodeException {
			// use level2 cache here
			Transaction t = TransactionManager.getCurrentTransaction();

			Value defaultValue = (Value) t.getFromLevel2Cache(this, DEFAULT_VALUE);

			if (defaultValue == null) {
				if (isValueless()) {
					defaultValue = new DummyValue(getId(), Construct.class, constructId);
				} else {
					defaultValue = t.getObject(Value.class, defaultValueId);
					// check data consistency
					assertNodeObjectNotNull(defaultValue, defaultValueId, "DefaultValue", true);
					if (defaultValue == null) {
						defaultValue = new DummyValue(getId(), Construct.class, constructId);
					}

					// check whether the default value was deleted in this transaction
					ObjectFactory valueFactory = getFactory().getObjectFactory(Value.class);
					if (valueFactory.isInDeletedList(Value.class, defaultValue)) {
						// try to get another default value (if another value replaced the old one)
						final List<Integer> valueIds = new ArrayList<Integer>();
						DBUtils.executeStatement("SELECT id FROM value WHERE part_id = ? AND templatetag_id = 0 AND contenttag_id = 0 AND objtag_id = 0",
								new SQLExecutor() {
							@Override
							public void prepareStatement(PreparedStatement stmt) throws SQLException {
								stmt.setObject(1, getId());
							}

							@Override
							public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
								while (rs.next()) {
									valueIds.add(rs.getInt("id"));
								}
							}
						});

						for (Integer valueId : valueIds) {
							NodeObjectInfo valueInfo = t.createObjectInfo(Value.class);
							if (!valueFactory.isInDeletedList(Value.class, new DummyObject(valueId, valueInfo))) {
								defaultValueId = valueId;
								break;
							}
						}

						defaultValue = t.getObject(Value.class, defaultValueId);
						// check data consistency
						assertNodeObjectNotNull(defaultValue, defaultValueId, "DefaultValue", true);
						if (defaultValue == null) {
							defaultValue = new DummyValue(getId(), Construct.class, constructId);
						}
					}
				}
				t.putIntoLevel2Cache(this, DEFAULT_VALUE, defaultValue);
			}
			return defaultValue;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Part {" + getName() + ", " + getId() + "}";
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Part#isInlineEditable()
		 */
		public boolean isInlineEditable() {
			return editable > 1;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryPart(this, getFactory().getFactoryHandle(Part.class).createObjectInfo(Part.class, true), true);
		}

		@Override
		public int getEffectiveUdate() throws NodeException {
			int udate = super.getEffectiveUdate();
			Transaction t = TransactionManager.getCurrentTransaction();

			udate = Math.max(udate, t.getObjectFactory(getObjectInfo().getObjectClass()).getEffectiveOutputUserUdate(getNameId()));
			return udate;
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException,
					NodeException {
			// delete the default value
			if (!isValueless()) {
				Value defaultValue = getDefaultValue();
				if (defaultValue != null) {
					defaultValue.delete();
				}
			}

			// delete the part
			Transaction t = TransactionManager.getCurrentTransaction();
			PartFactory factory = (PartFactory) t.getObjectFactory(Part.class);

			factory.deletePart(this);
		}
	}

	/**
	 * Editable Factory Implementation for parts
	 */
	private static class EditableFactoryPart extends FactoryPart {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -5466701200396432058L;

		/**
		 * Flag to mark modified objects
		 */
		protected boolean modified = false;

		/**
		 * Editable name
		 */
		protected EditableI18nString editableName;

		/**
		 * Editable default value instance
		 */
		protected Value editableDefaultValue;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		protected EditableFactoryPart(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param part part
		 * @param info info
		 * @param asNewObject true for a new object, false for an editable version of the given object
		 * @throws NodeException
		 */
		protected EditableFactoryPart(FactoryPart part,
				NodeObjectInfo info, boolean asNewObject) throws NodeException {
			super(asNewObject ? null : part.getId(), info, getDataMap(part), asNewObject ? -1 : part.getUdate(), asNewObject ? null : part.getGlobalId(),
					asNewObject ? -1 : part.defaultValueId);
			
			if (asNewObject) {
				nameId = -1;
				name = null;
				modified = true;
			} else {
				// read the names and descriptions
				if (nameId > 0) {
					editableName = new EditableI18nString();
					List<FactoryDataRow> entries = CNDictionary.getDicuserEntries(nameId);

					for (FactoryDataRow dicEntry : entries) {
						editableName.put(dicEntry.getInt("language_id"), dicEntry.getString("value"));
					}
				}
			}
		}

		@Override
		public void setConstructId(Integer constructId) throws ReadOnlyException, NodeException {
			if (ObjectTransformer.getInt(this.constructId, -1) != ObjectTransformer.getInt(constructId, -1)) {
				// only set the cosntruct id, if not done before
				if (ObjectTransformer.getInt(this.constructId, -1) <= 0) {
					this.constructId = constructId;
					this.modified = true;
				} else {
					throw new NodeException("Error while setting construct id " + constructId + " for part " + this.globalId + ". Construct id " + this.constructId + " is already set!");
				}
			}
		}

		@Override
		public void setEditable(int editable) throws ReadOnlyException {
			if (this.editable != editable) {
				this.editable = editable;
				this.modified = true;
			}
		}

		@Override
		public void setHidden(boolean hidden) throws ReadOnlyException {
			if (this.hidden != hidden) {
				this.hidden = hidden;
				this.modified = true;
			}
		}

		@Override
		public void setInfoInt(int infoInt) throws ReadOnlyException {
			if (this.infoInt != infoInt) {
				this.infoInt = infoInt;
				this.modified = true;
			}
		}

		@Override
		public void setInfoText(String infoText) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.infoText, infoText)) {
				this.infoText = infoText;
				this.modified = true;
			}
		}

		@Override
		public void setKeyname(String keyname) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.keyword, keyname)) {
				this.keyword = keyname;
				this.modified = true;
			}
		}

		@Override
		public void setMlId(int mlId) throws ReadOnlyException {
			if (ObjectTransformer.getInt(this.mlId, -1) != mlId) {
				this.mlId = mlId;
				this.modified = true;
			}
		}

		@Override
		public void setPartoptionId(int partOptionId) throws ReadOnlyException {
			if (this.partOptionId != partOptionId) {
				this.partOptionId = partOptionId;
				this.modified = true;
			}
		}

		@Override
		public void setPartOrder(int partOrder) throws ReadOnlyException {
			if (this.partOrder != partOrder) {
				this.partOrder = partOrder;
				this.modified = true;
			}
		}

		@Override
		public void setPartTypeId(int partTypeId) throws ReadOnlyException {
			if (this.partTypeId != partTypeId) {
				this.partTypeId = partTypeId;
				this.modified = true;
			}
		}

		@Override
		public void setPolicy(String policy) throws ReadOnlyException, NodeException {
			if (!StringUtils.isEqual(this.policy, policy)) {
				this.policy = policy;
				this.modified = true;
				this.policyURI = newPolicyURI(getId(), this.policy);
			}
		}

		@Override
		public void setRequired(boolean required) throws ReadOnlyException {
			if (this.required != required) {
				this.required = required;
				this.modified = true;
			}
		}

		@Override
		public void setName(String name, int language) throws ReadOnlyException {
			if (editableName == null) {
				editableName = new EditableI18nString();
			}
			editableName.put(language, name);
		}

		@Override
		public void setHideInEditor(boolean hideInEditor) throws ReadOnlyException {
			if (this.hideInEditor != hideInEditor) {
				this.hideInEditor = hideInEditor;
				this.modified = true;
			}
		}

		@Override
		public void setExternalEditorUrl(String externalEditorUrl) throws ReadOnlyException {
			externalEditorUrl = ObjectTransformer.getString(externalEditorUrl, "");
			if (!StringUtils.isEqual(this.externalEditorUrl, externalEditorUrl)) {
				this.externalEditorUrl = externalEditorUrl;
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException,
					NodeException {
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();

			boolean dicEntriesChanged = false;

			// save names
			if (nameId <= 0) {
				nameId = CNDictionary.createNewOutputId();
				this.modified = true;
			}
			if (editableName != null) {
				for (UserLanguage lang : UserLanguageFactory.getActive()) {
					int id = lang.getId();
					dicEntriesChanged |= CNDictionary.saveDicUserEntry(nameId, id, ObjectTransformer.getString(editableName.get(id), ""));
				}
			}

			boolean isModified = this.modified;
			boolean isNew = isEmptyId(getId());
			Part origPart = null;

			if (!isNew) {
				origPart = t.getObject(Part.class, getId());
			}

			// save the construct, if necessary
			if (isModified) {
				saveFactoryObject(this);
				this.modified = false;
			}

			// save default value (if not a dummy value)
			Value defaultValue = getDefaultValue();

			if (!(defaultValue instanceof DummyValue)) {
				defaultValue.setPartId(getId());
				defaultValue.setContainer(getConstruct());
				isModified |= defaultValue.save();

				if (defaultValueId != ObjectTransformer.getInt(defaultValue.getId(), -1)) {
					// if another default value was set, delete the old default value
					if (defaultValueId > 0) {
						Value oldDefaultValue = t.getObject(Value.class, defaultValueId);

						oldDefaultValue.delete();
					}
					defaultValueId = ObjectTransformer.getInt(defaultValue.getId(), -1);
					isModified = true;
				}
			}

			isModified |= dicEntriesChanged;
			// event
			if (isModified) {
				if (isNew) {
					t.addTransactional(new TransactionalTriggerEvent(Part.class, getId(), null, Events.CREATE));
				} else {
					// get modified attributes
					t.addTransactional(new TransactionalTriggerEvent(Part.class, getId(), getModifiedData(origPart, this), Events.UPDATE));
				}
			}

			if (isModified) {
				t.dirtObjectCache(Part.class, getId());
			}

			return isModified;
		}

		@Override
		public Value getDefaultValue() throws NodeException {
			if (editableDefaultValue == null) {
				Transaction t = TransactionManager.getCurrentTransaction();

				editableDefaultValue = super.getDefaultValue();
				if (!(editableDefaultValue instanceof DummyValue)) {
					// get editable copy of the default value (if not a dummy value)
					editableDefaultValue = t.getObject(Value.class, editableDefaultValue.getId(), true);
					// connect the editable part with the default value
					if (editableDefaultValue instanceof EditableDefaultValue) {
						((EditableDefaultValue) editableDefaultValue).setEditablePart(this);
					}
				}
			}
			return editableDefaultValue;
		}

		@Override
		public void setDefaultValue(Value value) throws ReadOnlyException,
					NodeException {
			editableDefaultValue = value;
		}
	}

	public PartFactory() {
		super();
	}

	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) {
		if (Part.class.equals(clazz)) {
			return (T) new EditableFactoryPart(handle.createObjectInfo(Part.class, true));
		} else {
			return null;
		}
	}

	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		return loadDbObject(clazz, id, info, SELECT_PART_SQL, null, null);
	}

	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_PART_SQL + buildIdSql(ids));
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}

		if (object instanceof FactoryPart) {
			return (T) new EditableFactoryPart((FactoryPart) object, info, false);
		} else {
			throw new NodeException("PartFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Deletes a Part
	 * @param part Part
	 */
	protected void deletePart(Part part) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<Part> toDelete = getDeleteList(t, Part.class);

		toDelete.add(part);
	}

	@SuppressWarnings("unchecked")
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info,
			FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		return (T) new FactoryPart(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs), rs.getInt("value_id"));
	}

	protected static URI newPolicyURI(Integer id, String uri) throws NodeException {
		if (StringUtils.isEmpty(uri)) {
			return null;
		}
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new NodeException("policyURI for part `" + id + "' is malformed: " + uri);
		}
	}

	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, Part.class)) {
			// get deleted parts
			Collection<Part> deletedParts = getDeleteList(t, Part.class);

			// get deleted constructs
			AbstractFactory constructFactory = (AbstractFactory) t.getObjectFactory(Construct.class);
			Collection<Construct> deletedConstructs = constructFactory.getDeleteList(t, Construct.class);

			// determine, which constructs have to be dirted
			List<Construct>  constructsToDirt       = new Vector<Construct>();
			List<Integer>    dicUserEntriesToDelete = new Vector<Integer>();

			for (Part part : deletedParts) {
				constructsToDirt.add(part.getConstruct());

				// Remember userdic entries for deletion
				int nameId = part.getNameId();
				if (nameId > 0) {
					dicUserEntriesToDelete.add(nameId);
				}

				// Log the deletion action
				ActionLogger.logCmd(ActionLogger.DEL, Part.TYPE_PART, part.getId(), null, "Part.delete()");
			}

			// don't dirt constructs, that will be deleted anyway
			constructsToDirt.removeAll(deletedConstructs);

			// dirt constructs
			for (Construct c : constructsToDirt) {
				t.dirtObjectCache(Construct.class, c.getId());
			}

			// delete all values
			flushDelete("DELETE FROM value WHERE part_id IN", Part.class);
			flushDelete("DELETE FROM value_nodeversion WHERE part_id IN", Part.class);

			// delete all dicuser entries for all parts
			CNDictionary.deleteDicUserEntries(dicUserEntriesToDelete);

			// delete the parts
			flushDelete("DELETE FROM part WHERE id IN", Part.class);

			// The datasource entries for the part are not deleted in here.
			// They will be deleted when the construct is deleted.
		}
	}

	/**
	 * Internal helper class for implementation of dummy values (for parttypes that do not have an own value)
	 */
	static class DummyValue extends Value {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 3061327938925304710L;

		/**
		 * part id
		 */
		private Integer partId;

		/**
		 * container type (will be construct)
		 */
		private Class containerType;

		/**
		 * container id
		 */
		private Integer containerId;

		/**
		 * Create an instance of the DummyValue
		 * @param partId part id
		 * @param containerType container type
		 * @param containerId container id
		 */
		public DummyValue(Integer partId, Class containerType, Integer containerId) throws NodeException {
			super(new Integer(0), TransactionManager.getCurrentTransaction().createObjectInfo(Value.class));
			this.partId = partId;
			this.containerType = containerType;
			this.containerId = containerId;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#performDelete()
		 */
		protected void performDelete() throws NodeException {}
        
		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#getInfo()
		 */
		public int getInfo() {
			return 0;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#isStatic()
		 */
		public boolean isStatic() {
			return true;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#getValueText()
		 */
		public String getValueText() {
			return null;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#getValueRef()
		 */
		public int getValueRef() {
			return 0;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#getPart(boolean)
		 */
		public Part getPart(boolean checkForNull) throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(Part.class, partId);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#getPartId()
		 */
		public Integer getPartId() {
			return partId;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#getContainer()
		 */
		public ValueContainer getContainer() throws NodeException {
			return (ValueContainer) TransactionManager.getCurrentTransaction().getObject(containerType, containerId);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#setInfo(int)
		 */
		public int setInfo(int info) throws ReadOnlyException {
			// dummy implementation
			assertEditable();
			return getInfo();
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#setValueRef(int)
		 */
		public int setValueRef(int valueRef) throws ReadOnlyException {
			// dummy implementation
			assertEditable();
			return getValueRef();
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Value#setValueText(java.lang.String)
		 */
		public String setValueText(String valueText) throws ReadOnlyException {
			// dummy implementation
			assertEditable();
			return getValueText();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			// TODO implement this
			return null;
		}
	}
}
