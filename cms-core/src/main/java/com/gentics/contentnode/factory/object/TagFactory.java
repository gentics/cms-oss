/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: TagFactory.java,v 1.45.2.2.2.2.2.2 2011-03-30 14:42:30 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.HandleSelectResultSet;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.NoObjectTagSync;
import com.gentics.contentnode.factory.ObjectFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.PartFactory.DummyValue;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.EditableValueList;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.publish.wrapper.PublishablePage;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * An objectfactory which can create {@link ContentTag}, {@link TemplateTag}
 * and {@link ObjectTag} objects, based on the {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = ContentTag.class, name = "contenttag"),
	@DBTable(clazz = TemplateTag.class, name = "templatetag"),
	@DBTable(clazz = ObjectTag.class, name = "objtag") })
public class TagFactory extends AbstractFactory {

	protected final static String INSERT_CONTENTTAG_SQL = "INSERT INTO contenttag (content_id, construct_id, enabled, name, template, uuid) " + "VALUES (?, ?, ?, ?, ?, ?)";

	protected final static String UPDATE_CONTENTTAG_SQL = "UPDATE contenttag SET content_id = ?, construct_id = ?, enabled = ?, name = ? " + "WHERE id = ?";

	protected final static String INSERT_TEMPLATETAG_SQL = "INSERT INTO templatetag (templategroup_id, template_id, construct_id, pub, "
			+ "enabled, name, mandatory, uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	protected final static String UPDATE_TEMPLATETAG_SQL = "UPDATE templatetag SET " + "templategroup_id = ?, " + "template_id = ?, " + "construct_id = ?, "
			+ "pub = ?, " + "enabled = ?, " + "name = ?, " + "mandatory = ? " + "WHERE id = ?";

	protected final static String INSERT_OBJECTTAG_SQL = "INSERT INTO objtag (obj_id, obj_type, construct_id, enabled, name, intag, inheritable, required, uuid) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	protected final static String UPDATE_OBJECTTAG_SQL = "UPDATE objtag SET obj_id = ?, obj_type = ?, construct_id = ?, enabled = ?, name = ?, intag = ?, inheritable = ?, required = ? "
			+ "WHERE id = ?";

	/**
	 * SQL Statement to select contenttag data
	 */
	protected final static String SELECT_CONTENTTAG_SQL = createSelectStatement("contenttag");

	/**
	 * SQL Statement for batchloading contenttags
	 */
	protected final static String BATCHLOAD_CONTENTTAG_SQL = createBatchLoadStatement("contenttag");

	/**
	 * SQL Statement to select template data
	 */
	protected final static String SELECT_TEMPLATETAG_SQL = createSelectStatement("templatetag");

	/**
	 * SQL Statement for batchloading templatetags
	 */
	protected final static String BATCHLOAD_TEMPLATETAG_SQL = createBatchLoadStatement("templatetag");

	/**
	 * SQL Statement to select objtag data
	 */
	protected final static String SELECT_OBJTAG_SQL = createSelectStatement("objtag");

	/**
	 * SQL Statement for batchloading objtags
	 */
	protected final static String BATCHLOAD_OBJTAG_SQL = createBatchLoadStatement("objtag");

	/**
	 * SQL Statement to select versioned contenttag data
	 */
	protected final static String SELECT_VERSIONED_CONTENTTAG_SQL = "SELECT * FROM contenttag_nodeversion WHERE id = ? AND nodeversiontimestamp = "
			+ "(SELECT MAX(nodeversiontimestamp) FROM contenttag_nodeversion "
			+ "WHERE (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? AND id = ? GROUP BY id)";

	/**
	 * SQL Params for the selection of versioned contenttag data
	 */
	protected final static VersionedSQLParam[] SELECT_VERSIONED_CONTENTTAG_PARAMS = {
		VersionedSQLParam.ID, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.ID};

	/**
	 * Name of the transaction attribute, which is set while objtags are synchronized
	 */
	public final static String SYNC_RUNNING_ATTRIBUTENAME = "objtag.sync_running";

	/**
	 * Implementation class for a ContentTag
	 */
	private static class FactoryContentTag extends ContentTag {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 4798937207953780008L;

		protected Integer contentId;

		protected List<Integer> valueIds;

		/**
		 * True, when this contenttag comes from the template. False if not
		 */
		protected boolean template;

		protected FactoryContentTag(NodeObjectInfo info) {
			super(null, info);
		}

		public FactoryContentTag(Integer id, NodeObjectInfo info, String name,
				Integer constructId, int enabled, Integer contentId, boolean template,
				List<Integer> valueIds, int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			this.name = name;
			this.constructId = constructId;
			this.enabled = enabled;
			this.contentId = contentId;
			this.template = template;
			this.valueIds = valueIds != null ? new Vector(valueIds) : null;
			if (this.valueIds != null) {
				getFactory().getObjectFactory(Value.class).removeDeleted(Value.class, this.valueIds);
			}
			this.udate = udate;
			this.globalId = globalId;
		}

		public Content getContent() throws NodeException {
			Content content = (Content) TransactionManager.getCurrentTransaction().getObject(Content.class, contentId);

			// check data consistency
			assertNodeObjectNotNull(content, contentId, "Content");
			return content;
		}

		public ValueList getValues() throws NodeException {
			return loadValues();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Tag#performDelete()
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			TagFactory tagFactory = (TagFactory) t.getObjectFactory(ContentTag.class);

			tagFactory.deleteTag(this, ContentTag.class);
		}

		public Construct getConstruct() throws NodeException {
			Construct construct = (Construct) TransactionManager.getCurrentTransaction().getObject(Construct.class, constructId);

			// check data consistency
			assertNodeObjectNotNull(construct, constructId, "Construct");
			return construct;
		}

		/**
		 * load the value ids (if not done before)
		 * @throws NodeException
		 */
		private synchronized void loadValueIds() throws NodeException {
			if (valueIds == null) {
				ObjectFactory valueFactory = getFactory().getObjectFactory(Value.class);

				HandleSelectResultSet<List<Integer>> valueIdList = rs -> {
					List<Integer> idList = new ArrayList<>();
					while (rs.next()) {
						if (!valueFactory.isInDeletedList(Value.class, rs.getInt("id"))) {
							idList.add(rs.getInt("id"));
						}
					}
					return idList;
				};

				int contentTagId = ObjectTransformer.getInt(getId(), -1);

				if (getObjectInfo().isCurrentVersion()) {
					valueIds = DBUtils.select("SELECT id FROM value WHERE contenttag_id = ?",
							ps -> ps.setInt(1, contentTagId), valueIdList);
				} else {
					int versionTimestamp = getObjectInfo().getVersionTimestamp();

					valueIds = DBUtils.select(
							"SELECT id FROM value_nodeversion WHERE contenttag_id = ? AND (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? GROUP BY id",
							ps -> {
								ps.setInt(1, contentTagId);
								ps.setInt(2, versionTimestamp);
								ps.setInt(3, versionTimestamp);
							}, valueIdList);
				}
			}
		}

		private ValueList loadValues() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			EditableValueList values = new EditableValueList(getId() + "-" + t.getTType(ContentTag.class));

			loadValueIds();

			List<Value> vals = getObjectInfo().isEditable()
					? t.getObjects(Value.class, valueIds, getObjectInfo().isEditable())
					: t.getObjects(Value.class, valueIds, getObjectInfo().getVersionTimestamp());
			for (Value value : vals) {
				if (value.getPart(false) != null) {
					values.addValue(value);
				}
			}

			return values;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "ContentTag {" + getName() + ", " + getId() + "}";
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
		 */
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			loadValueIds();
			// get the values now, this will make sure that it is not necessary to load the values in single sql statements
			t.getObjects(Value.class, valueIds);

			for (Integer valueId : valueIds) {
				t.dirtObjectCache(Value.class, valueId, false);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryContentTag(this, getFactory().getFactoryHandle(ContentTag.class).createObjectInfo(ContentTag.class, true), true);
		}

		@Override
		public boolean comesFromTemplate() {
			return template;
		}
	}

	/**
	 * Implementation class for editable copies of ContentTags
	 */
	private static class EditableFactoryContentTag extends FactoryContentTag {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -157251142281218846L;

		/**
		 * editable copy of the value list
		 */
		private ValueList editableValueList;

		/**
		 * Editable content
		 */
		private Content content;

		/**
		 * Create an empty instance for a new contenttag
		 * @param info info about the new tag
		 */
		protected EditableFactoryContentTag(NodeObjectInfo info) {
			super(info);
			modified = true;
			enabled = 3;
		}

		/**
		 * Constructor for creating a copy of the given tag
		 * @param tag tag to copy
		 * @param info info about the copy
		 * @param asNewContentTag true when the editable content tag shall be
		 *        used as new tag, false if the instance is just the editable
		 *        version of the tag
		 */
		protected EditableFactoryContentTag(FactoryContentTag tag, NodeObjectInfo info,
				boolean asNewContentTag) throws NodeException {
			super(asNewContentTag ? null : tag.getId(), info, tag.getName(), tag.getConstruct().getId(), tag.getEnabledValue(),
					asNewContentTag ? null : tag.contentId, tag.template, asNewContentTag ? null : tag.valueIds,
					asNewContentTag ? -1 : tag.getUdate(), asNewContentTag ? null : tag.getGlobalId());
			if (asNewContentTag) {
				modified = true;

				// copy the values
				Transaction t = TransactionManager.getCurrentTransaction();
				editableValueList = new EditableValueList(getId() + "-" + t.getTType(ContentTag.class), tag.getValues(), this);
				editableValueList = getMissingDefaultValues(this, editableValueList);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.TagFactory.FactoryContentTag#getValues()
		 */
		public ValueList getValues() throws NodeException {
			if (editableValueList == null) {
				editableValueList = filterDeletedValues(super.getValues());
				editableValueList = getMissingDefaultValues(this, editableValueList);
			}

			return editableValueList;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentTag#setContentId(java.lang.Object)
		 */
		public Integer setContentId(Integer contentId) throws ReadOnlyException {
			if (ObjectTransformer.getInt(this.contentId, 0) != ObjectTransformer.getInt(contentId, 0)) {
				Integer oldContentId = this.contentId;

				this.contentId = contentId;
				modified = true;
				return oldContentId;
			} else {
				return this.contentId;
			}
		}

		@Override
		public void setContent(Content content) throws NodeException {
			super.setContent(content);
			this.content = content;
		}

		@Override
		public Content getContent() throws NodeException {
			if (content != null) {
				return content;
			}
			return super.getContent();
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#save()
		 */
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			assertEditable();

			boolean isNew = isEmptyId(getId());
			boolean isModified = modified;

			if (modified) {
				// object is modified, so update it
				try {
					isModified = true;
					saveContentTagObject(this);
					modified = false;
				} catch (SQLException e) {
					throw new NodeException("Error while saving {" + this + "}", e);
				}
			}

			Transaction t = TransactionManager.getCurrentTransaction();

			// save all values and check which values no longer exist (and need to be removed)
			ValueList values = getValues();
			List<Integer> valueIdsToRemove = new Vector<Integer>();

			if (valueIds != null) {
				valueIdsToRemove.addAll(valueIds);
			}
			for (Value value : values) {
				value.setContainer(this);
				isModified |= value.save();
				valueIdsToRemove.remove(value.getId());
			}

			// remove the values that no longer exist
			if (!valueIdsToRemove.isEmpty()) {
				List<Value> valuesToRemove = t.getObjects(Value.class, valueIdsToRemove);

				for (Iterator<Value> i = valuesToRemove.iterator(); i.hasNext();) {
					Value valueToRemove = i.next();

					valueToRemove.delete();
				}
				isModified = true;
			}

			if (isNew) {
				updateMissingReferences();
			}

			return isModified;
		}

		@Override
		public void migrateToConstruct(Construct newConstruct) throws NodeException {
			super.migrateToConstruct(newConstruct);
			if (TagFactory.migrateToConstruct(this, newConstruct)) {
				// Set remaining default values
				editableValueList = getMissingDefaultValues(this, editableValueList);
			}
		}

		@Override
		public void clone(TemplateTag tag) throws NodeException {
			// this tag must be new
			if (!isEmptyId(getId())) {
				throw new NodeException("Cannot make " + this + " a clone of " + tag + ", because " + this + " is not new");
			}

			// the tag comes from the template
			template = true;

			// set "enabled" flag
			setEnabled(tag.getEnabledValue());

			// set the construct id
			setConstructId(tag.getConstruct().getId());
			// .. and the name
			setName(tag.getName());

			// ... and set all values
			ValueList tTagValues = tag.getValues();
			ValueList cTagValues = getValues();

			for (Value tTagValue : tTagValues) {
				Value cTagValue = cTagValues.getByPartId(tTagValue.getPartId());

				if (cTagValue != null) {
					// copy the value data
					cTagValue.copyFrom(tTagValue);
				} else {// TODO this is an error!
				}
			}
		}
	}

	/**
	 * Implementation class for a TemplateTag
	 */
	private static class FactoryTemplateTag extends TemplateTag {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 7873312651220245700L;

		protected Integer templateId;

		protected Integer templateGroupId;

		@Updateable
		@DataField("mandatory")
		protected boolean mandatory;

		protected boolean isPublic;

		protected List<Integer> valueIds;

		protected FactoryTemplateTag(NodeObjectInfo info) {
			super(null, info);
		}

		public FactoryTemplateTag(Integer id, NodeObjectInfo info, String name,
				Integer constructId, int enabled, Integer templateId,
				Integer templateGroupId, boolean isPublic, List valueIds,
				int udate, GlobalId globalId, boolean mandatory) throws NodeException {
			super(id, info);
			this.name = name;
			this.constructId = constructId;
			this.enabled = enabled;
			this.templateId = templateId;
			this.templateGroupId = templateGroupId;
			this.isPublic = isPublic;
			this.valueIds = valueIds != null ? new Vector(valueIds) : valueIds;
			if (this.valueIds != null) {
				getFactory().getObjectFactory(Value.class).removeDeleted(Value.class, this.valueIds);
			}
			this.udate = udate;
			this.globalId = globalId;
			this.mandatory = mandatory;
		}

		public ValueList getValues() throws NodeException {
			return loadValues();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Tag#performDelete()
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			TagFactory templateTagFactory = (TagFactory) t.getObjectFactory(TemplateTag.class);

			templateTagFactory.deleteTag(this, TemplateTag.class);
		}

		public Construct getConstruct() throws NodeException {
			Construct construct = (Construct) TransactionManager.getCurrentTransaction().getObject(Construct.class, constructId);

			// check data consistency
			assertNodeObjectNotNull(construct, constructId, "Construct");
			return construct;
		}

		/**
		 * load the value ids (if not done before)
		 * @throws NodeException
		 */
		private synchronized void loadValueIds() throws NodeException {
			if (valueIds == null) {
				ObjectFactory valueFactory = getFactory().getObjectFactory(Value.class);

				HandleSelectResultSet<List<Integer>> valueIdList = rs -> {
					List<Integer> idList = new ArrayList<>();
					while (rs.next()) {
						if (!valueFactory.isInDeletedList(Value.class, rs.getInt("id"))) {
							idList.add(rs.getInt("id"));
						}
					}
					return idList;
				};

				int templateTagId = ObjectTransformer.getInt(getId(), -1);

				valueIds = DBUtils.select("SELECT id FROM value WHERE templatetag_id = ?",
						ps -> ps.setInt(1, templateTagId), valueIdList);

			}
		}

		private ValueList loadValues() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			EditableValueList values = new EditableValueList(getId() + "-" + t.getTType(TemplateTag.class));

			loadValueIds();

			List vals = t.getObjects(Value.class, valueIds, getObjectInfo().isEditable());

			for (int i = 0; i < vals.size(); i++) {
				Value value = (Value) vals.get(i);

				values.addValue(value);
			}

			return values;
		}

		public Template getTemplate() throws NodeException {
			Template template = TransactionManager.getCurrentTransaction().getObject(Template.class, templateId);

			// check data consistency
			assertNodeObjectNotNull(template, templateId, "Template");
			return template;
		}

		@Override
		public Integer getTemplategroupId() {
			return templateGroupId;
		}

		@Override
		public boolean getMandatory() {
			return mandatory;
		}

		public boolean isPublic() {
			return isPublic;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "TemplateTag {" + getName() + ", " + getId() + "}";
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
		 */
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			loadValueIds();
			// get the values now, this will make sure that it is not necessary to load the values in single sql statements
			t.getObjects(Value.class, valueIds);

			for (Integer  valueId : valueIds) {
				t.dirtObjectCache(Value.class, valueId, false);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryTemplateTag(this, getFactory().getFactoryHandle(TemplateTag.class).createObjectInfo(TemplateTag.class, true), true);
		}
	}

	/**
	 * Implementation class for editable TemplateTags
	 */
	private static class EditableFactoryTemplateTag extends FactoryTemplateTag {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -3170879664419585009L;

		/**
		 * Editable copy of the values list
		 */
		private ValueList editableValueList;

		/**
		 * Create an empty instance for a new contenttag
		 * @param info info about the new tag
		 */
		protected EditableFactoryTemplateTag(NodeObjectInfo info) {
			super(info);
			modified = true;
			enabled = 3;
		}

		/**
		 * Constructor for creating a copy of the given tag
		 * @param tag tag to copy
		 * @param info info about the copy
		 * @param asNewTemplateTag true when the template tag shall be a new tag, false if just an editable version of the given tag
		 */
		public EditableFactoryTemplateTag(FactoryTemplateTag tag, NodeObjectInfo info, boolean asNewTemplateTag) throws NodeException {
			super(asNewTemplateTag ? null : tag.getId(), info, tag.getName(), tag.getConstruct().getId(), tag.getEnabledValue(),
					asNewTemplateTag ? null : tag.templateId, asNewTemplateTag ? null : tag.templateGroupId, tag.isPublic, asNewTemplateTag ? null : tag.valueIds,
					asNewTemplateTag ? -1 : tag.getUdate(), asNewTemplateTag ? null : tag.getGlobalId(), asNewTemplateTag ? false : tag.getMandatory());

			if (asNewTemplateTag) {
				modified = true;

				// copy the values
				Transaction t = TransactionManager.getCurrentTransaction();
				editableValueList = new EditableValueList(getId() + "-" + t.getTType(TemplateTag.class), tag.getValues(), this);
				editableValueList = getMissingDefaultValues(this, editableValueList);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.TagFactory.FactoryTemplateTag#getValues()
		 */
		public ValueList getValues() throws NodeException {
			if (editableValueList == null) {
				editableValueList = filterDeletedValues(super.getValues());
				editableValueList = getMissingDefaultValues(this, editableValueList);
			}

			return editableValueList;
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public Integer setTemplateId(Integer templateId) throws ReadOnlyException {
			if (ObjectTransformer.getInt(this.templateId, 0) != ObjectTransformer.getInt(templateId, 0)) {
				Integer oldTemplateId = this.templateId;

				this.templateId = templateId;
				modified = true;
				return oldTemplateId;
			} else {
				return this.templateId;
			}
		}

		@Override
		public void setMandatory(boolean mandatory) throws ReadOnlyException {
			if (this.mandatory != mandatory) {
				this.mandatory = mandatory;
				this.modified = true;
			}
		}

		@Override
		public void setPublic(boolean pub) throws ReadOnlyException {
			if (this.isPublic != pub) {
				this.isPublic = pub;
				this.modified = true;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#save()
		 */
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			assertEditable();

			boolean isModified = modified;
			boolean isNew = Tag.isEmptyId(getId());

			if (modified) {
				// object is modified, so update it
				try {
					isModified = true;
					saveTemplateTagObject(this);
					modified = false;
				} catch (SQLException e) {
					throw new NodeException("Error while saving {" + this + "}", e);
				}
			}

			Transaction t = TransactionManager.getCurrentTransaction();

			// save all values and check which values no longer exist (and need to be removed)
			ValueList values = getValues();
			List<Integer> valueIdsToRemove = new Vector<Integer>();

			if (valueIds != null) {
				valueIdsToRemove.addAll(valueIds);
			}
			for (Value value : values) {
				value.setContainer(this);
				isModified |= value.save();
				valueIdsToRemove.remove(value.getId());
			}

			// remove the values that no longer exist
			if (!valueIdsToRemove.isEmpty()) {
				List<Value> valuesToRemove = t.getObjects(Value.class, valueIdsToRemove);

				for (Iterator<Value> i = valuesToRemove.iterator(); i.hasNext();) {
					Value valueToRemove = i.next();

					valueToRemove.delete();
				}
				isModified = true;
			}

			// when the template tag or its values have been changed,
			// a create or an update event is triggered to cause dirting
			if (isModified && (!isNew || isEnabled())) {
				t.addTransactional(new TransactionalTriggerEvent(TemplateTag.class, getId(), null, isNew ? Events.CREATE : Events.UPDATE));
			}

			if (isNew) {
				updateMissingReferences();
			}

			return isModified;
		}
	}

	/**
	 * Implementation class for an ObjectTag
	 */
	private static class FactoryObjectTag extends ObjectTag {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -5589888289010880409L;

		/**
		 * ID of the parent object tag
		 */
		@DataField("intag")
		@Updateable
		protected Integer inTagId;

		@DataField("inheritable")
		@Updateable
		protected boolean inheritable;

		@DataField("required")
		@Updateable
		protected boolean required;

		protected List<Integer> valueIds;

		/**
		 * id of the objecttag container
		 */
		@DataField("obj_id")
		@Updateable
		protected Integer containerId;

		/**
		 * TType of the container
		 */
		@DataField("obj_type")
		@Updateable
		protected int objType;

		/**
		 * class of the objecttag container
		 */
		protected Class containerClass;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected FactoryObjectTag(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id
		 * @param info
		 * @param name
		 * @param constructId
		 * @param enabled
		 * @param intag
		 * @param inheritable
		 * @param required
		 * @param valueIds
		 * @param containerId
		 * @param containerClass
		 * @param udate
		 * @param globalId
		 */
		protected FactoryObjectTag(Integer id, NodeObjectInfo info, String name,
				Integer constructId, int enabled, Integer inTagId, boolean inheritable, boolean required,
				List valueIds, Integer containerId, Class containerClass,
				int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			this.name = name;
			this.constructId = constructId;
			this.enabled = enabled;
			this.inTagId = inTagId;
			this.inheritable = inheritable;
			this.required = required;
			this.valueIds = valueIds != null ? new Vector(valueIds) : null;
			if (this.valueIds != null) {
				getFactory().getObjectFactory(Value.class).removeDeleted(Value.class, this.valueIds);
			}
			this.containerId = containerId;
			this.containerClass = containerClass;
			this.objType = TransactionManager.getCurrentTransaction().getTType(containerClass);
			this.udate = udate;
			this.globalId = globalId;
		}

		public ValueList getValues() throws NodeException {
			return loadValues();
		}

		// TODO review - written by fg
		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Tag#performDelete()
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			TagFactory tagFactory = (TagFactory) t.getObjectFactory(ObjectTag.class);

			tagFactory.deleteTag(this, ObjectTag.class);

			// synchronize deletion of the objecttag in the contentset, if this is required
			syncDelete();
		}

		public Construct getConstruct() throws NodeException {
			Construct construct = (Construct) TransactionManager.getCurrentTransaction().getObject(Construct.class, constructId);

			// check data consistency
			assertNodeObjectNotNull(construct, constructId, "Construct");
			return construct;
		}

		@Override
		public int getObjType() {
			return objType;
		}

		/**
		 * load the value ids (if not done before)
		 * @throws NodeException
		 */
		private synchronized void loadValueIds() throws NodeException {
			if (valueIds == null) {
				if (isEmptyId(getId())) {
					valueIds = new ArrayList<>();
				} else {
					ObjectFactory valueFactory = getFactory().getObjectFactory(Value.class);

					HandleSelectResultSet<List<Integer>> valueIdList = rs -> {
						List<Integer> idList = new ArrayList<>();
						while (rs.next()) {
							if (!valueFactory.isInDeletedList(Value.class, rs.getInt("id"))) {
								idList.add(rs.getInt("id"));
							}
						}
						return idList;
					};

					valueIds = DBUtils.select("SELECT id FROM value WHERE objtag_id = ?", ps -> ps.setInt(1, getId()),
							valueIdList);
				}
			}
		}

		private ValueList loadValues() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			EditableValueList values = new EditableValueList(getId() + "-" + t.getTType(ObjectTag.class));

			loadValueIds();

			List vals = t.getObjects(Value.class, valueIds, getObjectInfo().isEditable());

			for (int i = 0; i < vals.size(); i++) {
				Value value = (Value) vals.get(i);

				values.addValue(value);
			}

			return values;
		}

		@Override
		public boolean isIntag() {
			return ObjectTransformer.getInt(inTagId, 0) > 0;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.ObjectTag#isInheritable()
		 */
		public boolean isInheritable() {
			return inheritable;
		}

		@Override
		public boolean isRequired() {
			return required;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "ObjectTag {" + getName() + ", " + getId() + "}";
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
		 */
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			loadValueIds();
			// get the values now, this will make sure that it is not necessary to load the values in single sql statements
			t.getObjects(Value.class, valueIds);

			for (Integer valueId : valueIds) {
				t.dirtObjectCache(Value.class, valueId, false);
			}

			NodeObject object = getNodeObject();
			if (object instanceof Page) {
				PublishablePage.removeFromCache(ObjectTransformer.getInt(object.getId(), 0));
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.ObjectTag#getNodeObject()
		 */
		public NodeObject getNodeObject() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(containerClass, containerId);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryObjectTag(this, getFactory().getFactoryHandle(ObjectTag.class).createObjectInfo(ObjectTag.class, true), true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.ObjectTag#setNodeObject(com.gentics.lib.base.object.NodeObject)
		 */
		public void setNodeObject(NodeObject owner) throws NodeException, ReadOnlyException {
			if (owner != null) {
				containerClass = owner.getObjectInfo().getObjectClass();
				containerId = owner.getId();
				objType = TransactionManager.getCurrentTransaction().getTType(containerClass);
			} else {
				containerClass = null;
				containerId = null;
				objType = 0;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.ObjectTag#setInTagObject(com.gentics.lib.base.object.NodeObject)
		 */
		public void setInTagObject(ObjectTag objectTag) throws NodeException, ReadOnlyException {
			failReadOnly();
		}

		/**
		 * Load the definition id for this object tag
		 * @return definition id
		 * @throws NodeException
		 */
		protected Integer loadDefinitionId() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;
			ResultSet res = null;

			try {
				pst = t.prepareStatement("SELECT id FROM objtag WHERE obj_type = ? AND obj_id = ? AND name = ?");
				pst.setInt(1, t.getTType(containerClass));
				pst.setInt(2, 0);
				pst.setString(3, name);
				res = pst.executeQuery();
				if (res.next()) {
					return res.getInt("id");
				} else {
					return null;
				}
			} catch (SQLException e) {
				throw new NodeException("Error while loading objtag definition id", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		}

		@Override
		public ObjectTagDefinition getDefinition() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(ObjectTagDefinition.class, loadDefinitionId());
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.ObjectTag#getInTagObject()
		 */
		@Override
		public ObjectTag getInTagObject() throws NodeException {
			if (AbstractContentObject.isEmptyId(inTagId)) {
				return null;
			}

			// get parent tag
			ObjectTag parent = TransactionManager.getCurrentTransaction().getObject(ObjectTag.class, inTagId);
			if (parent != null && !ObjectTransformer.equals(getNodeObject(), parent.getNodeObject())) {
				parent = null;
			}

			return parent;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.ObjectTag#getInTagId()
		 */
		@Override
		public Integer getInTagId() {
			return this.inTagId;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.ObjectTag#setIntagId(java.lang.Object)
		 */
		@Override
		public void setIntagId(Integer intagId) {
			this.inTagId = intagId;
		}

		@Override
		public void sync() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)
					|| ObjectTransformer.getBoolean(t.getAttributes().get(SYNC_RUNNING_ATTRIBUTENAME), false)) {
				return;
			}
			String tagName = stripObjectTagPrefix(getName());

			Set<Integer> inSyncNow = new HashSet<>();
			inSyncNow.add(getId());

			doSync(variant -> {
				ObjectTag variantObjectTag = variant.getObjectTag(tagName);
				if (variantObjectTag == null) {
					variantObjectTag = (ObjectTag) copy();
					variantObjectTag.setNodeObject(variant);
					variantObjectTag.save();
				} else {
					// get editable version, copy this tag over the other and save
					variantObjectTag = t.getObject(variantObjectTag, true);
					variantObjectTag.copyFrom(this);
					variantObjectTag.save();
				}
				inSyncNow.add(variantObjectTag.getId());
				t.dirtObjectCache(variant.getObjectInfo().getObjectClass(), variant.getId());
			}, true);

			DBUtils.update("UPDATE objtag SET in_sync = 1 WHERE id IN ("
					+ StringUtils.repeat("?", inSyncNow.size(), ",") + ")",
					inSyncNow.toArray(new Object[inSyncNow.size()]));
		}

		@Override
		public Set<Integer> checkSync() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)
					|| ObjectTransformer.getBoolean(t.getAttributes().get(SYNC_RUNNING_ATTRIBUTENAME), false)) {
				return Collections.emptySet();
			}
			String tagName = stripObjectTagPrefix(getName());
			Set<Integer> ids = new HashSet<>();
			ids.add(getId());
			AtomicBoolean inSync = new AtomicBoolean(true);

			// check all variants
			doSync(variant -> {
				ObjectTag variantObjectTag = variant.getObjectTag(tagName);

				if (variantObjectTag != null) {
					ids.add(variantObjectTag.getId());
					if (inSync.get() && !hasSameContent(variantObjectTag)) {
						inSync.set(false);
					}
				} else {
					inSync.set(false);
				}
			}, false);

			// set check stati
			Object[] args = new Integer[ids.size() + 1];
			args[0] = inSync.get() ? 1 : -1;
			System.arraycopy(ids.toArray(new Integer[ids.size()]), 0, args, 1, ids.size());
			DBUtils.update("UPDATE objtag SET in_sync = ? WHERE id IN (" + StringUtils.repeat("?", ids.size(), ",") + ")", args);

			return ids;
		}

		@Override
		public Set<Pair<NodeObject, ObjectTag>> getSyncVariants(boolean lookIntoWastebin) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)
					|| ObjectTransformer.getBoolean(t.getAttributes().get(SYNC_RUNNING_ATTRIBUTENAME), false)) {
				return Collections.emptySet();
			}

			String tagName = stripObjectTagPrefix(getName());
			Set<Pair<NodeObject, ObjectTag>> set = new HashSet<>();
			try (WastebinFilter wb = new WastebinFilter(Wastebin.INCLUDE)) {
				set.add(Pair.of(getNodeObject(), this));
			}

			// get all variants
			doSync(variant -> {
				ObjectTag variantObjectTag = variant.getObjectTag(tagName);
				if (variantObjectTag != null) {
					set.add(Pair.of(variant, variantObjectTag));
				}
			}, lookIntoWastebin);

			return set;
		}

		@Override
		public boolean hasSameContent(ObjectTag other) throws NodeException {
			if (!Objects.equals(getEnabledValue(), other.getEnabledValue())) {
				return false;
			}
			return getValues().hasSameValues(other.getValues());
		}

		/**
		 * Synchronize the deletion of an object tag
		 * @throws NodeException
		 */
		protected void syncDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)
					|| ObjectTransformer.getBoolean(t.getAttributes().get(SYNC_RUNNING_ATTRIBUTENAME), false)) {
				return;
			}
			String tagName = stripObjectTagPrefix(getName());

			doSync(variant -> {
				ObjectTag variantObjectTag = variant.getObjectTag(tagName);
				if (variantObjectTag != null) {
					variantObjectTag.delete();
					t.dirtObjectCache(variant.getObjectInfo().getObjectClass(), variant.getId());
				}
			}, true);
		}

		/**
		 * Do all necessary syncs for the object tag
		 * @param syncMethod lambda that performs the sync for the variant
		 * @param lookIntoWastebin true to look into the wastebin, false to ignore objects in the wastebin
		 * @throws NodeException
		 */
		protected void doSync(Consumer<ObjectTagContainer> syncMethod, boolean lookIntoWastebin) throws NodeException {
			try (WastebinFilter wb = new WastebinFilter(lookIntoWastebin ? Wastebin.INCLUDE : Wastebin.EXCLUDE)) {
				Transaction t = TransactionManager.getCurrentTransaction();
				NodeObject container = getNodeObject();

				Set<Integer> variantIds = new HashSet<>();
				Set<Integer> checkIds = new HashSet<>();
				boolean contentsetSync = needSyncContentset() && container instanceof Page;
				boolean variantSync = needSyncVariants() && container instanceof Page;
				boolean channelsetSync = needSyncChannelset() && container instanceof LocalizableNodeObject;

				// we need to iterate, if more than one sync "direction" is given
				long syncDirections = Observable.fromArray(contentsetSync, variantSync, channelsetSync).filter(b -> b).count().blockingGet();
				if (syncDirections == 0) {
					return;
				}
				boolean iterate = syncDirections > 1;

				String table = t.getTable(container.getObjectInfo().getObjectClass());

				checkIds.add(container.getId());
				do {
					String params = StringUtils.repeat("?", checkIds.size(), ",");
					Set<Integer> newFound = new HashSet<>();
					if (contentsetSync) {
						newFound.addAll(DBUtils.select(
								"SELECT p.id FROM page p LEFT JOIN page c ON p.contentset_id = c.contentset_id AND p.channel_id = c.channel_id WHERE c.id IN ("
										+ params + ") AND p.id != ?",
										ps -> {
											AtomicInteger index = new AtomicInteger();
											Observable.fromIterable(checkIds)
											.blockingForEach(id -> ps.setInt(index.incrementAndGet(), id));
											ps.setInt(index.incrementAndGet(), container.getId());
										}, DBUtils.IDS));
					}
					if (variantSync) {
						newFound.addAll(DBUtils.select(
								"SELECT p.id FROM page p LEFT JOIN page c ON p.content_id = c.content_id WHERE c.id IN ("
										+ params + ") AND p.id != ?",
										ps -> {
											AtomicInteger index = new AtomicInteger();
											Observable.fromIterable(checkIds)
											.blockingForEach(id -> ps.setInt(index.incrementAndGet(), id));
											ps.setInt(index.incrementAndGet(), container.getId());
										}, DBUtils.IDS));
					}
					if (channelsetSync) {
						newFound.addAll(DBUtils.select(
								"SELECT o.id FROM " + table + " o LEFT JOIN " + table
								+ " c ON o.channelset_id = c.channelset_id WHERE c.id IN (" + params + ") AND o.id != ?",
								ps -> {
									AtomicInteger index = new AtomicInteger();
									Observable.fromIterable(checkIds)
									.blockingForEach(id -> ps.setInt(index.incrementAndGet(), id));
									ps.setInt(index.incrementAndGet(), container.getId());
								}, DBUtils.IDS));
					}

					if (iterate) {
						// when we need to iterate, everything we found in this step, which was not already found,
						// needs to be checked again
						checkIds.clear();
						checkIds.addAll(newFound);
						checkIds.removeAll(variantIds);
					}

					variantIds.addAll(newFound);
				} while (iterate && !checkIds.isEmpty());

				if (!variantIds.isEmpty()) {
					try (NoObjectTagSync noSync = new NoObjectTagSync()) {
						List<? extends NodeObject> variants = t.getObjects(container.getObjectInfo().getObjectClass(),
								variantIds, false, false);
						Observable.fromIterable(variants).filter(variant -> !variant.equals(container))
						.cast(ObjectTagContainer.class).blockingForEach(syncMethod);
					}
				}
			}
		}

		/**
		 * Check whether the object tag shall be synchronized over all language variants
		 * @return true for sync
		 * @throws NodeException
		 */
		protected boolean needSyncContentset() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			return NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)
					&& getObjType() == Page.TYPE_PAGE && getDefinition() != null && getDefinition().isSyncContentset()
					&& !ObjectTransformer.getBoolean(t.getAttributes().get(SYNC_RUNNING_ATTRIBUTENAME), false);
		}

		/**
		 * Check whether the object tag shall be synchronized over all page variants
		 * @return true for sync
		 * @throws NodeException
		 */
		protected boolean needSyncVariants() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			return NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)
					&& getObjType() == Page.TYPE_PAGE && getDefinition() != null && getDefinition().isSyncVariants()
					&& !ObjectTransformer.getBoolean(t.getAttributes().get(SYNC_RUNNING_ATTRIBUTENAME), false);
		}

		/**
		 * Check whether the object tag shall be synchronized over all channel variants
		 * @return true for sync
		 * @throws NodeException
		 */
		protected boolean needSyncChannelset() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			return NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)
					&& NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)
					&& getDefinition() != null && getDefinition().isSyncChannelset()
					&& !ObjectTransformer.getBoolean(t.getAttributes().get(SYNC_RUNNING_ATTRIBUTENAME), false);
		}
	}

	/**
	 * Implementation class for editable ObjectTags
	 */
	private static class EditableFactoryObjectTag extends FactoryObjectTag {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -8213007359735753477L;

		/**
		 * Editable copy of the values list
		 */
		private ValueList editableValueList;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected EditableFactoryObjectTag(NodeObjectInfo info) {
			super(info);
			this.modified = true;
		}

		/**
		 * Constructor for creating a copy of the given tag
		 * @param tag tag to copy
		 * @param info info about the copy
		 * @param asNewObjectTag true when the object tag shall be a new tag, false if just an editable version of the given tag
		 */
		protected EditableFactoryObjectTag(FactoryObjectTag tag, NodeObjectInfo info, boolean asNewObjectTag) throws NodeException {
			super(asNewObjectTag ? null : tag.getId(), info, tag.getName(), tag.getConstructId(), tag.getEnabledValue(), tag.inTagId, tag.inheritable,
					tag.required, asNewObjectTag ? null : tag.valueIds, asNewObjectTag ? null : tag.containerId,
					asNewObjectTag ? null : tag.containerClass, asNewObjectTag ? -1 : tag.getUdate(), asNewObjectTag ? null : tag.getGlobalId());
			if (asNewObjectTag) {
				modified = true;

				// copy the values
				Transaction t = TransactionManager.getCurrentTransaction();
				editableValueList = new EditableValueList(getId() + "-" + t.getTType(ObjectTag.class), tag.getValues(), this);
				editableValueList = getMissingDefaultValues(this, editableValueList);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.TagFactory.FactoryTemplateTag#getValues()
		 */
		public ValueList getValues() throws NodeException {
			if (editableValueList == null) {
				editableValueList = filterDeletedValues(super.getValues());
				editableValueList = getMissingDefaultValues(this, editableValueList);
			}

			return editableValueList;
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public void setInheritable(boolean inheritable) throws ReadOnlyException {
			if (this.inheritable != inheritable) {
				this.inheritable = inheritable;
				this.modified = true;
			}
		}

		@Override
		public void setInTagObject(ObjectTag objectTag) throws NodeException, ReadOnlyException {
			if (objectTag != null && objectTag.getId() != this.inTagId) {
				this.inTagId = objectTag.getId();
				this.modified = true;
			} else {
				this.inTagId = 0;
			}
		}

		@Override
		public void setObjType(int objType) throws ReadOnlyException, NodeException {
			if (this.objType != objType) {
				this.objType = objType;
				this.containerClass = TransactionManager.getCurrentTransaction().getClass(objType);
				this.modified = true;
			}
		}

		@Override
		public void setRequired(boolean required) throws ReadOnlyException {
			if (this.required != required) {
				this.required = required;
				this.modified = true;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#save()
		 */
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			assertEditable();

			boolean isModified = modified;
			boolean isNew = Tag.isEmptyId(getId());

			if (modified) {
				// object is modified, so update it
				try {
					isModified = true;
					saveObjectTagObject(this);
					modified = false;
				} catch (SQLException e) {
					throw new NodeException("Error while saving {" + this + "}", e);
				}
			}

			Transaction t = TransactionManager.getCurrentTransaction();

			// save all values and check which values no longer exist (and need to be removed)
			ValueList values = getValues();
			List<Integer> valueIdsToRemove = new Vector<Integer>();

			if (valueIds != null) {
				valueIdsToRemove.addAll(valueIds);
			}
			for (Value value : values) {
				value.setContainer(this);
				isModified |= value.save();
				valueIdsToRemove.remove(value.getId());
			}

			// remove the values that no longer exist
			if (!valueIdsToRemove.isEmpty()) {
				List<Value> valuesToRemove = t.getObjects(Value.class, valueIdsToRemove);

				for (Iterator<Value> i = valuesToRemove.iterator(); i.hasNext();) {
					Value valueToRemove = i.next();

					valueToRemove.delete();
				}
				isModified = true;
			}

			// when the objtag or at least one value was updated, we need to
			// trigger an event
			// when the objtag is new, we only trigger the event, if it is enabled
			if (isModified && (!isNew || isEnabled())) {
				t.addTransactional(new TransactionalTriggerEvent(ObjectTag.class, getId(), null, isNew ? Events.CREATE : Events.UPDATE));
			}

			// sync object tags, if this is required
			if (isModified) {
				sync();
			}

			return isModified;
		}

		@Override
		public void migrateToConstruct(Construct newConstruct) throws NodeException {
			super.migrateToConstruct(newConstruct);
			if (TagFactory.migrateToConstruct(this, newConstruct)) {
				// Set remaining default values
				editableValueList = getMissingDefaultValues(this, editableValueList);
			}
		}
	}

	@Override
	public boolean isVersioningSupported(Class<? extends NodeObject> clazz) {
		// contenttag support versioning
		return ContentTag.class.equals(clazz);
	}

	/**
	 * Deletes a Tag but instead of directly deleting the Tag the delete action is cached.
	 * When the Transaction is committed the delete is performed.
	 * @param tag The ObjectTag to delete
	 * @param class The generic class of the Tag to delete. (Not the factory implementation of the class.)
	 */
	protected void deleteTag(Tag tag, Class<? extends NodeObject> clazz) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<Tag> toDelete = getDeleteList(t, clazz);

		toDelete.add(tag);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#flush()
	 */
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, ObjectTag.class)) {
			Collection objTags = getDeleteList(t, ObjectTag.class);
			Vector objTagIds = new Vector();

			for (Iterator it = objTags.iterator(); it.hasNext();) {
				ObjectTag objTag = (ObjectTag) it.next();

				objTagIds.add(objTag.getId());
				t.dirtObjectCache(ObjectTag.class, objTag.getId());
			}
			// delete lists and there versions for this ObjectTag
			DBUtils.selectAndDelete("ds", "SELECT id FROM ds WHERE objtag_id IN", objTagIds);
			DBUtils.selectAndDelete("ds_obj", "SELECT id FROM ds_obj WHERE objtag_id IN", objTagIds);
			// DBUtils.selectAndDelete("ds_nodeversion", "SELECT id FROM ds_nodeversion WHERE objtag_id IN", objTagIds);
			// DBUtils.selectAndDelete("ds_obj_nodeversion", "SELECT id FROM ds_obj_nodeversion WHERE objtag_id IN", objTagIds);
			// delete ObjectTags
			flushDelete("DELETE FROM objtag WHERE id IN", ObjectTag.class);
		}
		if (!isEmptyDeleteList(t, TemplateTag.class)) {
			Collection templateTags = getDeleteList(t, TemplateTag.class);
			Vector templateTagIds = new Vector();

			for (Iterator it = templateTags.iterator(); it.hasNext();) {
				TemplateTag templateTag = (TemplateTag) it.next();

				templateTagIds.add(templateTag.getId());
			}
			// delete lists and there versions for this ObjectTag
			DBUtils.selectAndDelete("ds", "SELECT id FROM ds WHERE templatetag_id IN", templateTagIds);
			DBUtils.selectAndDelete("ds_obj", "SELECT id FROM ds_obj WHERE templatetag_id IN", templateTagIds);
			// DBUtils.selectAndDelete("ds_nodeversion", "SELECT id FROM ds_nodeversion WHERE templatetag_id IN", templateTagIds);
			// DBUtils.selectAndDelete("ds_obj_nodeversion", "SELECT id FROM ds_obj_nodeversion WHERE templatetag_id IN", templateTagIds);
			// delete TemplateTags
			flushDelete("DELETE FROM templatetag WHERE id IN", TemplateTag.class);
		}
		if (!isEmptyDeleteList(t, ContentTag.class)) {
			Collection contentTags = getDeleteList(t, ContentTag.class);
			Vector contentTagIds = new Vector();

			for (Iterator it = contentTags.iterator(); it.hasNext();) {
				ContentTag contentTag = (ContentTag) it.next();

				contentTagIds.add(contentTag.getId());
			}
			// delete lists and there versions for this ObjectTag
			DBUtils.selectAndDelete("ds", "SELECT id FROM ds WHERE contenttag_id IN", contentTagIds);
			DBUtils.selectAndDelete("ds_obj", "SELECT id FROM ds_obj WHERE contenttag_id IN", contentTagIds);
			// DBUtils.selectAndDelete("ds_nodeversion", "SELECT id FROM ds_nodeversion WHERE contenttag_id IN", contentTagIds);
			// DBUtils.selectAndDelete("ds_obj_nodeversion", "SELECT id FROM ds_obj_nodeversion WHERE contenttag_id IN", contentTagIds);
			// delete ContentTags
			// flushDelete("DELETE FROM contenttag_nodeversion WHERE id IN", ContentTag.class);
			flushDelete("DELETE FROM contenttag WHERE id IN", ContentTag.class);
		}
	}

	public TagFactory() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#createObject(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) {
		if (ContentTag.class.equals(clazz)) {
			return (T) new EditableFactoryContentTag(handle.createObjectInfo(ContentTag.class, true));
		} else if (TemplateTag.class.equals(clazz)) {
			return (T) new EditableFactoryTemplateTag(handle.createObjectInfo(TemplateTag.class, true));
		} else if (ObjectTag.class.equals(clazz)) {
			return (T) new EditableFactoryObjectTag(handle.createObjectInfo(ObjectTag.class, true));
		} else {
			return null;
		}
	}

	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		T tag = null;

		if (ContentTag.class.equals(clazz)) {
			tag = loadDbObject(clazz, id, info, SELECT_CONTENTTAG_SQL, SELECT_VERSIONED_CONTENTTAG_SQL, SELECT_VERSIONED_CONTENTTAG_PARAMS);
		} else if (TemplateTag.class.equals(clazz)) {
			tag = loadDbObject(clazz, id, info, SELECT_TEMPLATETAG_SQL, null, null);
		} else if (ObjectTag.class.equals(clazz)) {
			tag = loadDbObject(clazz, id, info, SELECT_OBJTAG_SQL, null, null);
		}
		return tag;
	}

	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		Collection<T> tags = Collections.emptyList();
		String idSql = buildIdSql(ids);

		if (ContentTag.class.equals(clazz)) {
			String[] preloadSql = new String[] { "SELECT contenttag_id AS id, id AS id2 FROM value WHERE contenttag_id IN " + idSql};

			tags = batchLoadDbObjects(clazz, ids, info, BATCHLOAD_CONTENTTAG_SQL + idSql, preloadSql);
		} else if (TemplateTag.class.equals(clazz)) {
			String[] preloadSql = new String[] { "SELECT templatetag_id AS id, id AS id2 FROM value WHERE templatetag_id IN " + idSql};

			tags = batchLoadDbObjects(clazz, ids, info, BATCHLOAD_TEMPLATETAG_SQL + idSql, preloadSql);
		} else if (ObjectTag.class.equals(clazz)) {
			String[] preloadSql = new String[] { "SELECT objtag_id AS id, id AS id2 FROM value WHERE objtag_id IN " + idSql};

			tags = batchLoadDbObjects(clazz, ids, info, BATCHLOAD_OBJTAG_SQL + idSql, preloadSql);
		}
		return tags;
	}

	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info,
			FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {

		String name = rs.getString("name");
		Integer constructId = new Integer(rs.getInt("construct_id"));
		int enabled = rs.getInt("enabled");

		List<Integer> valueIds = idLists != null ? idLists[0] : null;

		T tag = null;

		if (ContentTag.class.equals(clazz)) {

			Integer contentId = new Integer(rs.getInt("content_id"));

			tag = (T) new FactoryContentTag(id, info, name, constructId, enabled, contentId, rs.getBoolean("template"), valueIds, getUdate(rs), getGlobalId(rs));

		} else if (TemplateTag.class.equals(clazz)) {

			Integer templateId = new Integer(rs.getInt("template_id"));
			boolean isPublic = rs.getInt("pub") > 0;

			tag = (T) new FactoryTemplateTag(id, info, name, constructId, enabled, templateId, rs.getInt("templategroup_id"), isPublic, valueIds, getUdate(rs),
					getGlobalId(rs), Boolean.valueOf(rs.getBoolean("mandatory")));

		} else if (ObjectTag.class.equals(clazz)) {

			boolean inheritable = rs.getInt("inheritable") > 0;
			Integer containerId = new Integer(rs.getInt("obj_id"));
			Integer inTagId     = new Integer(rs.getInt("intag"));
			Class<? extends NodeObject> containerClass = TransactionManager.getCurrentTransaction().getClass(rs.getInt("obj_type"));

			tag = (T) new FactoryObjectTag(id, info, name, constructId, enabled, inTagId, inheritable, rs.getBoolean("required"),
					valueIds, containerId, containerClass, getUdate(rs), getGlobalId(rs));

		}

		return tag;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}
		if (object instanceof FactoryContentTag) {
			return (T) new EditableFactoryContentTag((FactoryContentTag) object, info, false);
		} else if (object instanceof FactoryTemplateTag) {
			return (T) new EditableFactoryTemplateTag((FactoryTemplateTag) object, info, false);
		} else if (object instanceof FactoryObjectTag) {
			return (T) new EditableFactoryObjectTag((FactoryObjectTag) object, info, false);
		} else {
			throw new NodeException("TagFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Save the given contenttag object
	 * @param tag tag
	 * @throws NodeException
	 * @throws SQLException
	 */
	private static void saveContentTagObject(EditableFactoryContentTag tag) throws NodeException, SQLException {
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean isNew = Tag.isEmptyId(tag.getId());

		String tagName = tag.getName();
		if (!StringUtils.isEmpty(tagName)) {
			tagName = tagName.trim();
		}

		if (isNew) {
			// for new contenttags, we need to check whether the tag comes from a templatetag
			for (Page page : tag.getContent().getPages()) {
				if (page.getTemplate().getTags().containsKey(tagName)) {
					tag.template = true;
					break;
				}
			}

			// insert a new record
			List<Integer> keys = DBUtils.executeInsert(INSERT_CONTENTTAG_SQL, new Object[] {
				tag.contentId, tag.getConstruct().getId(), tag.getEnabledValue(), tagName, tag.template, ObjectTransformer.getString(tag.getGlobalId(), "")});

			if (keys.size() == 1) {
				// set the new page id
				tag.setId(keys.get(0));
				synchronizeGlobalId(tag);
			} else {
				throw new NodeException("Error while inserting new contenttag, could not get the insertion id");
			}
		} else {
			DBUtils.executeUpdate(UPDATE_CONTENTTAG_SQL, new Object[] {
				tag.contentId, tag.getConstruct().getId(), tag.getEnabledValue(), tagName, tag.getId()
			});

			// and dirt the cache for the updated object
			t.dirtObjectCache(ContentTag.class, tag.getId());
		}
	}

	/**
	 * Save the given templatetag object
	 * @param tag tag
	 * @throws NodeException
	 * @throws SQLException
	 */
	private static void saveTemplateTagObject(EditableFactoryTemplateTag tag) throws NodeException, SQLException {
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean isNew = Tag.isEmptyId(tag.getId());

		String tagName = tag.getName();
		if (!StringUtils.isEmpty(tagName)) {
			tagName = tagName.trim();
		}

		if (isNew) {
			// insert a new record
			// templategroup_id, template_id, construct_id, pub, enabled, name
			List<Integer> keys = DBUtils.executeInsert(INSERT_TEMPLATETAG_SQL,
					new Object[] {
				tag.getTemplate().getTemplategroupId(), tag.templateId, tag.getConstruct().getId(), tag.isPublic ? 1 : 0, tag.getEnabledValue(), tagName,
				tag.getMandatory(), ObjectTransformer.getString(tag.getGlobalId(), "")
			});

			if (keys.size() == 1) {
				// set the new page id
				tag.setId(keys.get(0));
				synchronizeGlobalId(tag);
			} else {
				throw new NodeException("Error while inserting new templatetag, could not get the insertion id");
			}
		} else {
			DBUtils.executeUpdate(UPDATE_TEMPLATETAG_SQL, new Object[] {
				0, tag.templateId, tag.getConstruct().getId(), tag.isPublic ? 1 : 0, tag.getEnabledValue(), tagName, tag.getMandatory(), tag.getId()
			});

			// and dirt the cache for the updated object
			t.dirtObjectCache(ContentTag.class, tag.getId());
		}
	}

	/**
	 * Save the given objecttag object
	 * @param tag tag
	 * @throws NodeException
	 * @throws SQLException
	 */
	private static void saveObjectTagObject(EditableFactoryObjectTag tag) throws NodeException, SQLException {
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean isNew = Tag.isEmptyId(tag.getId());

		String tagName = tag.getName();
		if (!StringUtils.isEmpty(tagName)) {
			tagName = tagName.trim();
		}

		if (isNew) {
			// make sure the containerId is not null
			tag.containerId = ObjectTransformer.getInteger(tag.containerId, 0);
			// also the inTagId
			tag.inTagId = ObjectTransformer.getInteger(tag.inTagId, 0);
			// insert a new record
			// obj_id, obj_type, construct_id, enabled, name, intag, inheritable, required
			// TODO: intag, required?
			List<Integer> keys = DBUtils.executeInsert(INSERT_OBJECTTAG_SQL,
					new Object[] {
				tag.containerId, t.getTType(tag.containerClass), tag.getConstruct().getId(),
				tag.getEnabledValue(), tagName, tag.inTagId, tag.inheritable ? 1 : 0, tag.required ? 1 : 0,
				ObjectTransformer.getString(tag.getGlobalId(), "")});

			if (keys.size() == 1) {
				// set the new page id
				tag.setId(keys.get(0));
				synchronizeGlobalId(tag);
			} else {
				throw new NodeException("Error while inserting new objecttag, could not get the insertion id");
			}
		} else {
			DBUtils.executeUpdate(UPDATE_OBJECTTAG_SQL,
					new Object[] {
				tag.containerId, t.getTType(tag.containerClass), tag.getConstruct().getId(), tag.getEnabledValue(), tagName, tag.inTagId, tag.inheritable ? 1 : 0,
				tag.required ? 1 : 0, tag.getId()
			});


			// and dirt the cache for the updated object
			t.dirtObjectCache(ContentTag.class, tag.getId());
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#batchLoadVersionedObjects(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class, java.lang.Class, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> Set<T> batchLoadVersionedObjects(FactoryHandle handle, Class<T> clazz, Class<? extends NodeObject> mainClazz, Map<Integer, Integer> timestamps) throws NodeException {
		if (ContentTag.class.equals(clazz)) {
			Set<T> preparedTags = new HashSet<T>();
			Map<Integer, Map<Integer, FactoryDataRow>> contentTagData = getVersionedData("SELECT *, content_id gentics_obj_id FROM contenttag_nodeversion WHERE content_id IN", timestamps);

			for (Map.Entry<Integer, Map<Integer, FactoryDataRow>> rowMapEntry : contentTagData.entrySet()) {
				Integer mainObjId = rowMapEntry.getKey();
				int versionTimestamp = timestamps.get(mainObjId);
				Map<Integer, FactoryDataRow> rowMap = rowMapEntry.getValue();
				for (Map.Entry<Integer, FactoryDataRow> entry : rowMap.entrySet()) {
					Integer objId = entry.getKey();
					FactoryDataRow row = entry.getValue();

					try {
						ContentTag tag = loadResultSet(ContentTag.class, objId, handle.createObjectInfo(ContentTag.class, versionTimestamp), row, null);
						preparedTags.add((T)tag);
						handle.putObject(ContentTag.class, tag, versionTimestamp);
					} catch (SQLException e) {
						throw new NodeException("Error while batchloading contenttags", e);
					}
				}
			}

			Transaction t = TransactionManager.getCurrentTransaction();
			t.prepareVersionedObjects(Value.class, Content.class, timestamps);

			return preparedTags;
		} else {
			return Collections.emptySet();
		}
	}

	/**
	 * Create a new instance of {@link EditableValueList}, that will contain the given values and construct default values
	 * @param tag tag for which the default values shall be added
	 * @param values existing valuelist
	 * @return editable valuelist with default values added
	 * @throws NodeException
	 */
	private static EditableValueList getMissingDefaultValues(Tag tag, ValueList values) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the parts of the construct
		List<Part> parts = tag.getConstruct().getParts();

		// this will be the new list
		EditableValueList newValues = new EditableValueList(tag.getId());

		// now generate the values for the editable parts
		for (Iterator<Part> i = parts.iterator(); i.hasNext();) {
			Part part = i.next();

			if (part.isEditable()) {
				Value currentValue = values.getByPartId(part.getId());

				if (currentValue == null) {
					Value defaultValue = part.getDefaultValue();

					// generate a new value out of the default value
					currentValue = t.createObject(Value.class);
					currentValue.setContainer(tag);
					currentValue.copyFrom(defaultValue);
				}

				// add the value to the new list
				newValues.addValue(currentValue);
			}
		}

		return newValues;
	}

	/**
	 * Migrate the tag to the construct, if it not already uses the construct
	 * @param tag tag to migrate
	 * @param newConstruct new construct
	 * @return true if the tag was migrated, false if not
	 * @throws NodeException
	 */
	private static boolean migrateToConstruct(Tag tag, Construct newConstruct) throws NodeException {
		Construct oldConstruct = tag.getConstruct();

		if (oldConstruct.equals(newConstruct)) {
			return false;
		}

		Map<String, Part> newPartMap = newConstruct.getParts().stream().filter(Part::isEditable).collect(Collectors.toMap(Part::getKeyname, Function.identity()));
		Map<String, Part> oldPartMap = oldConstruct.getParts().stream().filter(Part::isEditable).collect(Collectors.toMap(Part::getKeyname, Function.identity()));

		Set<String> convertibleKeys = new HashSet<String>();
		convertibleKeys.addAll(newPartMap.keySet());
		convertibleKeys.retainAll(oldPartMap.keySet());

		// Migrate values of compatibel part types to new parts
		for (Iterator<String> i = convertibleKeys.iterator(); i.hasNext();) {
			String key = i.next();
			Part oldPart = oldPartMap.get(key);
			Part newPart = newPartMap.get(key);

			boolean bothTextTypes = PartType.TEXTTYPE_PARTS.contains(oldPart.getPartTypeId()) && PartType.TEXTTYPE_PARTS.contains(newPart.getPartTypeId());
			boolean equalTypes = oldPart.getPartTypeId() == newPart.getPartTypeId();
			if (!(equalTypes || bothTextTypes)) {
				i.remove();
			}
		}
		ValueList contentValues = tag.getValues();

		for (String key : convertibleKeys) {
			Value contentValue = contentValues.getByKeyname(key);
			Part newPart = newPartMap.get(key);

			if (contentValue != null) {
				Value oldDefaultValue = contentValue.getPart().getDefaultValue();
				boolean oldDefaultValueExists = !(oldDefaultValue instanceof DummyValue);

				Value newDefaultValue = newPart.getDefaultValue();
				boolean newDefaultValueExists = !(newDefaultValue instanceof DummyValue);

				if (oldDefaultValueExists && newDefaultValueExists && oldDefaultValue.getValueRef() == contentValue.getValueRef()
						&& StringUtils.isEqual(oldDefaultValue.getValueText(), contentValue.getValueText())) {
					// delete default values so they are replaced by the new default values
					contentValues.remove(contentValue);
				} else {
					// just update the part id
					contentValue.setPartId(newPart.getId());

					// EditableValueList keeps internal indices which must be updated
					contentValues.remove(contentValue);
					contentValues.add(contentValue);
				}
			}
		}

		// Remove all other values
		List<Value> toRemove = new ArrayList<Value>();
		for (Value val : contentValues) {
			if (!convertibleKeys.contains(val.getPart().getKeyname())) {
				toRemove.add(val);
			}
		}
		contentValues.removeAll(toRemove);

		// Activate autoenable tags because they were edited (by this method)
		if (tag.getEnabledValue() == 3 && newConstruct.isAutoEnable()) {
			tag.setEnabled(1);
		}

		// Update construct ID
		tag.setConstructId(newConstruct.getId());

		return true;
	}

	/**
	 * If the name starts with the objecttag prefix "object.", strip it off.
	 * @param name name
	 * @return optionally stripped name (null, if the name was null)
	 */
	public static String stripObjectTagPrefix(String name) {
		if (name == null) {
			return null;
		} else if (name.startsWith("object.")) {
			return name.substring(7);
		} else {
			return name;
		}
	}

	/**
	 * Load the object tag definitions for the given object type, optionally filtered by visibility in the give node
	 * @param type object type
	 * @param node optional node
	 * @return list of object tag definitions
	 * @throws NodeException
	 */
	public static List<ObjectTagDefinition> load(int type, Optional<Node> node) throws NodeException {
		Set<Integer> ids = DBUtils.select("SELECT objtag.id FROM objtag LEFT JOIN construct c ON c.id = objtag.construct_id WHERE c.id IS NOT NULL AND obj_type = ? AND obj_id = 0", pst -> {
			pst.setInt(1, type);
		}, DBUtils.IDS);

		List<ObjectTagDefinition> defs = new ArrayList<>(
				TransactionManager.getCurrentTransaction().getObjects(ObjectTagDefinition.class, ids));
		if (node.isPresent()) {
			for (Iterator<ObjectTagDefinition> i = defs.iterator(); i.hasNext();) {
				ObjectTagDefinition def = i.next();
				if (!def.isVisibleIn(node.get())) {
					i.remove();
				}
			}
		}
		return defs;
	}

	/**
	 * Load the keynames of the object tag definitions for the given object type, optionally filtered by visibility in the given node
	 * @param type object type
	 * @param node optional node
	 * @return list of object tag definition key names
	 * @throws NodeException
	 */
	public static List<String> loadKeynames(int type, Optional<Node> node) throws NodeException {
		List<ObjectTagDefinition> defs = load(type, node);
		return MiscUtils.unwrap(() -> defs.stream().map(def -> MiscUtils.wrap(() -> def.getObjectTag().getName())).collect(Collectors.toList()));
	}
}
