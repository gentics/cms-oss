/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: ConstructFactory.java,v 1.33.2.1 2011-01-18 13:21:53 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.ObjectFactory;
import com.gentics.contentnode.factory.RefreshPermHandler;
import com.gentics.contentnode.factory.RemovePermsTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.i18n.EditableI18nString;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ConstructCategory;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.EditableValueList;
import com.gentics.contentnode.object.Icon;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * An objectfactory to create {@link Construct} objects, based on the {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = Construct.class, name = "construct"),
	@DBTable(clazz = ConstructCategory.class, name = "construct_category") })
public class ConstructFactory extends AbstractFactory {

	/**
	 * SQL Statement for selecting a single construct
	 */
	protected final static String SELECT_CONSTRUCT_SQL = createSelectStatement("construct");

	/**
	 * SQL Statement for batchloading constructs
	 */
	protected final static String BATCHLOAD_CONSTRUCT_SQL = createBatchLoadStatement("construct");

	/**
	 * SQL Statement for selecting a single construct category
	 */
	protected final static String SELECT_CONSTRUCT_CATEGORY_SQL = createSelectStatement("construct_category");

	/**
	 * SQL Statement for batchloading construct categories
	 */
	protected final static String BATCHLOAD_CONSTRUCT_CATEGORY_SQL = createBatchLoadStatement("construct_category");

	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.CONSTRUCT, Construct.TYPE_CONSTRUCT, true, FactoryConstruct.class);
			registerFactoryClass(C.Tables.CONSTRUCT_CATEGORY, ConstructCategory.TYPE_CONSTRUCT_CATEGORY, true, FactoryConstructCategory.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Factory Implementation of Constructs
	 */
	private static class FactoryConstruct extends Construct {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -7701767242704153746L;

		protected I18nString name;

		/**
		 * List of node ids to which this construct is assigned to
		 */
		protected List<Integer> nodeIds;

		@DataField("name_id")
		protected int nameId;

		@DataField("keyword")
		@Updateable
		protected String keyword;
        
		protected Icon icon;

		@DataField("icon")
		@Updateable
		protected String iconName;
		protected I18nString description;

		@DataField("description_id")
		protected int descriptionId;

		@DataField("hopedithook")
		@Updateable
		protected String hopeditHook;

		@DataField("liveeditortagname")
		@Updateable
		protected String liveEditorTagName;

		@DataField("new_editor")
		@Updateable
		protected boolean newEditor;

		@DataField("external_editor_url")
		@Updateable
		protected String externalEditorUrl = "";

		protected List<Integer> partIds;
		protected List<Integer> valueIds;

		@DataField("autoenable")
		@Updateable
		protected boolean autoEnable;

		@DataField("category_id")
		@Updateable
		protected int categoryId;

		@DataField("creator")
		protected int creator;

		@DataField("cdate")
		protected ContentNodeDate cDate = new ContentNodeDate(0);

		@DataField("editor")
		@Updateable
		protected int editor;

		@DataField("edate")
		@Updateable
		protected ContentNodeDate eDate = new ContentNodeDate(0);

		@DataField("ml_id")
		@Updateable
		protected int mlId;

		@DataField("childable")
		@Updateable
		protected boolean mayContainSubtags;

		@DataField("intext")
		@Updateable
		protected boolean mayBeSubtag;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		public FactoryConstruct(NodeObjectInfo info) {
			super(null, info);
		}

		public FactoryConstruct(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap, int udate, GlobalId globalId,
				List<Integer> partIds, List<Integer> valueIds) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
			this.icon = StringUtils.isEmpty(iconName) ? null : new Icon("content", "constr/" + iconName, "");
			this.name = new CNI18nString(Integer.toString(nameId));
			this.description = new CNI18nString(Integer.toString(descriptionId));
			this.partIds = partIds != null ? new Vector<Integer>(partIds) : null;
			this.valueIds = valueIds != null ? new Vector<Integer>(valueIds) : null;
			this.udate = udate;
			this.globalId = globalId;
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

		public I18nString getName() {
			return name;
		}

		@Override
		public int getNameId() {
			return nameId;
		}

		public String getKeyword() {
			return keyword;
		}

		public Icon getIcon() {
			return icon;
		}

		@Override
		public String getIconName() {
			return iconName;
		}

		public I18nString getDescription() {
			return description;
		}

		@Override
		public int getDescriptionId() {
			return descriptionId;
		}

		public List<Part> getParts() throws NodeException {
			return loadParts();
		}

		protected synchronized void loadPartIds() throws NodeException {
			if (partIds == null) {
				partIds = new ArrayList<Integer>();

				// when the construct is new, it has no parts to load
				if (isEmptyId(getId())) {
					return;
				}

				Transaction t = TransactionManager.getCurrentTransaction();

				PreparedStatement stmt = null;
				ResultSet rs = null;

				try {
					stmt = t.prepareStatement("SELECT id FROM part WHERE construct_id = ?");
					stmt.setInt(1, getId());

					rs = stmt.executeQuery();

					while (rs.next()) {
						partIds.add(new Integer(rs.getInt("id")));
					}

				} catch (SQLException e) {
					throw new NodeException("Could not load parts for construct {" + getId() + "}.", e);
				} finally {
					t.closeResultSet(rs);
					t.closeStatement(stmt);
				}
			}
		}

		private List<Part> loadParts() throws NodeException {
			List<Part> parts = Collections.emptyList();

			loadPartIds();

			List<Integer> notDeleted = new ArrayList<Integer>(partIds);
			getFactory().getObjectFactory(Part.class).removeDeleted(Part.class, notDeleted);
			parts = TransactionManager.getCurrentTransaction().getObjects(Part.class, notDeleted, getObjectInfo().isEditable());

			if (parts.size() > 1) {
				// TODO use static Comparator instance here (no need to create a
				// new instance on every invocation)
				Collections.sort(parts, new Comparator<Part>() {
					public int compare(Part o1, Part o2) {
						return o1.getPartOrder() - o2.getPartOrder();
					}
				});
			}

			return parts;
		}

		public ValueList getValues() throws NodeException {
			return loadValues();
		}

		public boolean isAutoEnable() throws NodeException {
			return this.autoEnable;
		}

		private synchronized void loadValueIds() throws NodeException {
			if (valueIds == null) {
				valueIds = new ArrayList<Integer>();

				// if the construct is new, it has no values to load
				if (isEmptyId(getId())) {
					return;
				}
				Transaction t = TransactionManager.getCurrentTransaction();

				PreparedStatement stmt = null;
				ResultSet rs = null;

				try {
					stmt = t.prepareStatement(
							"SELECT value.id FROM part,value WHERE part.construct_id = ? AND "
									+ "part.id = value.part_id AND value.contenttag_id = 0 AND value.templatetag_id = 0 AND " + "value.objtag_id = 0");
					stmt.setInt(1, getId());

					rs = stmt.executeQuery();

					while (rs.next()) {
						valueIds.add(new Integer(rs.getInt("id")));
					}

				} catch (SQLException e) {
					throw new NodeException("Could not load values.", e);
				} finally {
					t.closeResultSet(rs);
					t.closeStatement(stmt);
				}
			}
		}

		private ValueList loadValues() throws NodeException {
			EditableValueList values = new EditableValueList(null);
			Transaction t = TransactionManager.getCurrentTransaction();

			loadValueIds();

			List<Value> vals = t.getObjects(Value.class, valueIds, getObjectInfo().isEditable());

			for (Value value : vals) {
				values.addValue(value);
			}

			return values;
		}

		public Construct getConstruct() throws NodeException {
			return this;
		}

		@Override
		public Integer getConstructId() throws NodeException {
			return getId();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Construct {" + getName() + ", " + getId() + "}";
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
		 */
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			loadValueIds();
			for (Iterator<Integer> iter = valueIds.iterator(); iter.hasNext();) {
				t.dirtObjectCache(Value.class, iter.next(), false);
			}
			loadPartIds();
			for (Iterator<Integer> iter = partIds.iterator(); iter.hasNext();) {
				t.dirtObjectCache(Part.class, iter.next(), false);
			}

			// Note: it is no longer necessary to dirt the caches of all tags using this construct
			// this possibly caused performance problems (when construct was used by a large number of tags and had a large number of editable parts).
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Construct#getContentTags()
		 */
		public List<ContentTag> getContentTags() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			PreparedStatement stmt = null;
			ResultSet rs = null;
			List<Integer> contentTagIds = new Vector<Integer>();

			try {
				stmt = t.prepareStatement("SELECT contenttag.id from contenttag where construct_id = ?");
				stmt.setInt(1, getId());

				rs = stmt.executeQuery();

				while (rs.next()) {
					contentTagIds.add(new Integer(rs.getInt("id")));
				}

				return t.getObjects(ContentTag.class, contentTagIds);
			} catch (SQLException e) {
				throw new NodeException("Could not load contenttags.", e);
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Construct#getTemplateTags()
		 */
		public List<TemplateTag> getTemplateTags() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			PreparedStatement stmt = null;
			ResultSet rs = null;
			List<Integer> templateTagIds = new Vector<Integer>();

			try {
				stmt = t.prepareStatement("SELECT templatetag.id from templatetag where construct_id = ?");
				stmt.setInt(1, getId());

				rs = stmt.executeQuery();

				while (rs.next()) {
					templateTagIds.add(new Integer(rs.getInt("id")));
				}

				return t.getObjects(TemplateTag.class, templateTagIds);
			} catch (SQLException e) {
				throw new NodeException("Could not load templatetags.", e);
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Construct#getObjectTags()
		 */
		public List<ObjectTag> getObjectTags() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			PreparedStatement stmt = null;
			ResultSet rs = null;
			List<Integer> objTagIds = new Vector<Integer>();

			try {
				stmt = t.prepareStatement("SELECT objtag.id from objtag where construct_id = ?");
				stmt.setInt(1, getId());

				rs = stmt.executeQuery();

				while (rs.next()) {
					objTagIds.add(new Integer(rs.getInt("id")));
				}

				return t.getObjects(ObjectTag.class, objTagIds);
			} catch (SQLException e) {
				throw new NodeException("Could not load objecttags.", e);
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}
		}

		@Override
		public boolean isUsed() throws NodeException {
			// for every type of tag, which just need to load one instance referencing this construct, in order to know
			// whether the construct is used.
			List<String> sqlQueries = Arrays.asList("SELECT id FROM templatetag WHERE construct_id = ? LIMIT 1",
					"SELECT id FROM contenttag WHERE construct_id = ? LIMIT 1",
					"SELECt id FROM objtag WHERE construct_id = ? LIMIT 1");

			for (String query : sqlQueries) {
				if (DBUtils.select(query, pst -> pst.setInt(1, getId()), DBUtils.firstInt("id")) > 0) {
					return true;
				}
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#triggerEvent(com.gentics.contentnode.events.DependencyObject, java.lang.String[], int, int)
		 */
		public void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId) throws NodeException {
			super.triggerEvent(object, property, eventMask, depth, channelId);

			if (Events.isEvent(eventMask, Events.UPDATE) && Events.isEvent(eventMask, Events.EVENT_CN_CONTENT)) {
				// trigger UPDATE on all tags built from this construct
				List<Tag> tags = getTags();

				for (Tag tag : tags) {
					tag.triggerEvent(new DependencyObject(tag, (NodeObject) null), null, Events.UPDATE, depth + 1, 0);
				}
			}
		}

		public String getHopeditHook() {
			return hopeditHook;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Construct#getLiveEditorTagName()
		 */
		public String getLiveEditorTagName() {
			return liveEditorTagName;
		}

		@Override
		public boolean isNewEditor() throws NodeException {
			return newEditor;
		}

		@Override
		public String getExternalEditorUrl() {
			return externalEditorUrl;
		}

		@Override
		public ConstructCategory getConstructCategory() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(ConstructCategory.class, categoryId);
		}

		@Override
		public Integer getConstructCategoryId() throws NodeException {
			return categoryId;
		}

		@Override
		public int getMlId() {
			return mlId;
		}

		@Override
		public boolean mayContainSubtags() {
			return mayContainSubtags;
		}

		@Override
		public boolean mayBeSubtag() {
			return mayBeSubtag;
		}

		/**
		 * Load the node ids
		 * @return list of node ids
		 * @throws NodeException
		 */
		protected List<Integer> loadNodeIds() throws NodeException {
			if (nodeIds == null) {
				if (getId() != null) {
					nodeIds = DBUtils.select("SELECT DISTINCT node_id id FROM construct_node WHERE construct_id = ?", st -> {
						st.setInt(1, getId());
					}, DBUtils.IDLIST);
				} else {
					nodeIds = new ArrayList<>();
				}
			}
			return nodeIds;
		}

		@Override
		public List<Node> getNodes() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// when getting the nodes, to which a construct is assigned to, we disable multichannelling fallback
			// (constructs are not assigned to channels)
			t.setDisableMultichannellingFlag(true);
			try {
				return t.getObjects(Node.class, loadNodeIds());
			} finally {
				t.resetDisableMultichannellingFlag();
			}
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(SystemUser.class, creator);
		}

		@Override
		public ContentNodeDate getCDate() {
			return cDate;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(SystemUser.class, editor);
		}

		@Override
		public ContentNodeDate getEDate() {
			return eDate;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryConstruct(this, getFactory().getFactoryHandle(Construct.class).createObjectInfo(Construct.class, true), true);
		}

		@Override
		public int getEffectiveUdate() throws NodeException {
			int udate = super.getEffectiveUdate();
			ObjectFactory oFactory = TransactionManager.getCurrentTransaction().getObjectFactory(getObjectInfo().getObjectClass());

			udate = Math.max(udate, oFactory.getEffectiveOutputUserUdate(getNameId()));
			udate = Math.max(udate, oFactory.getEffectiveOutputUserUdate(getDescriptionId()));
			return udate;
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException,
					NodeException {
			// delete all parts
			List<Part> parts = getParts();

			for (Part part : parts) {
				part.delete();
			}

			// delete the construct
			Transaction t = TransactionManager.getCurrentTransaction();
			ConstructFactory factory = (ConstructFactory) t.getObjectFactory(Construct.class);

			factory.deleteConstruct(this);
		}

		@Override
		public boolean canConvertTo(Construct other) throws NodeException {
			if (other == null) {
				return false;
			}

			// if keywords of editable parts are empty or not unique, the construct cannot be transformed
			if (other.getParts().stream().filter(Part::isEditable).filter(p -> StringUtils.isEmpty(p.getKeyname())).findFirst().isPresent()) {
				return false;
			}
			// if keywords occur multiple times, we cannot transform
			List<String> keyNameList = other.getParts().stream().filter(Part::isEditable).map(Part::getKeyname).collect(Collectors.toList());
			Set<String> set = new HashSet<>(keyNameList);
			if (keyNameList.size() != set.size()) {
				return false;
			}

			Map<String, Part> partMap = other.getParts().stream().filter(Part::isEditable).collect(Collectors.toMap(Part::getKeyname, Function.identity()));

			for (Part part : getParts()) {
				// omit non-editable parts
				if (!part.isEditable()) {
					continue;
				}
				// parts without keyword cannot be mapped
				if (part.getKeyname() == null) {
					return false;
				}

				// no matching part found
				if (!partMap.containsKey(part.getKeyname())) {
					return false;
				}

				Part matchingPart = partMap.get(part.getKeyname());
				boolean bothTextTypes = PartType.TEXTTYPE_PARTS.contains(part.getPartTypeId()) && PartType.TEXTTYPE_PARTS.contains(matchingPart.getPartTypeId());
				boolean equalTypes = part.getPartTypeId() == matchingPart.getPartTypeId();
				// part cannot be migrated, because types are different and not both text
				if (!equalTypes && !bothTextTypes) {
					return false;
				}
			}

			return true;
		}
	}

	/**
	 * Factory Implementation of editable Constructs
	 */
	private static class EditableFactoryConstruct extends FactoryConstruct {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -3443770575699917930L;

		/**
		 * Flag to mark modified objects
		 */
		protected boolean modified = false;

		/**
		 * Editable name
		 */
		protected EditableI18nString editableName = new EditableI18nString();

		/**
		 * Editable description
		 */
		protected EditableI18nString editableDescription = new EditableI18nString();

		/**
		 * List of editable parts
		 */
		protected List<Part> editableParts;

		/**
		 * List of assigned nodes
		 */
		protected List<Node> assignedNodes;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		protected EditableFactoryConstruct(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param construct construct
		 * @param info info
		 * @param asNewObject true for a new object, false for an editable version of the given object
		 * @throws NodeException
		 */
		protected EditableFactoryConstruct(FactoryConstruct construct,
				NodeObjectInfo info, boolean asNewObject) throws NodeException {
			super(asNewObject ? null : construct.getId(), info, getDataMap(construct), asNewObject ? -1 : construct.getUdate(),
					asNewObject ? null : construct.getGlobalId(), null, null);
			// read the names and descriptions
			editableName.init(nameId);
			editableDescription.init(descriptionId);
			if (asNewObject) {
				modified = true;
				// reset the name and description id
				nameId = -1;
				descriptionId = -1;
			}
		}

		@Override
		public List<Part> getParts() throws NodeException {
			if (editableParts == null) {
				editableParts = new Vector<Part>(super.getParts());
			}
			return editableParts;
		}

		@Override
		public ValueList getValues() throws NodeException {
			EditableValueList values = new EditableValueList(null);
			List<Part> parts = getParts();
			for (Part part : parts) {
				values.add(part.getDefaultValue());
			}
			return values;
		}

		@Override
		public List<Node> getNodes() throws NodeException {
			if (assignedNodes == null) {
				assignedNodes = new Vector<Node>(super.getNodes());
			}
			return assignedNodes;
		}

		@Override
		public I18nString getName() {
			return editableName;
		}

		@Override
		public void setName(String name, int language) throws ReadOnlyException {
			editableName.put(language, name);
		}

		@Override
		public I18nString getDescription() {
			return editableDescription;
		}

		@Override
		public void setDescription(String description, int language) throws ReadOnlyException {
			editableDescription.put(language, description);
		}

		@Override
		public void setAutoEnable(boolean autoEnable) throws ReadOnlyException {
			if (this.autoEnable != autoEnable) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Change in '%s': autoEnable changed from '%b' to '%b'", this,
							this.autoEnable, autoEnable));
				}
				this.autoEnable = autoEnable;
				this.modified = true;
			}
		}

		@Override
		public void setHopeditHook(String hopeditHook) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.hopeditHook, hopeditHook)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Change in '%s': hopeditHook changed from '%s' to '%s'", this,
							this.hopeditHook, hopeditHook));
				}
				this.hopeditHook = hopeditHook;
				this.modified = true;
			}
		}

		@Override
		public void setIconName(String iconName) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.iconName, iconName)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Change in '%s': iconName changed from '%s' to '%s'", this,
							this.iconName, iconName));
				}
				this.iconName = iconName;
				this.icon = StringUtils.isEmpty(iconName) ? null : new Icon("content", "constr/" + iconName, "");
				this.modified = true;
			}
		}

		@Override
		public void setKeyword(String keyword) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.keyword, keyword)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Change in '%s': keyword changed from '%s' to '%s'", this,
							this.keyword, keyword));
				}
				this.keyword = keyword;
				this.modified = true;
			}
		}

		@Override
		public void setLiveEditorTagName(String liveEditorTagName) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.liveEditorTagName, liveEditorTagName)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Change in '%s': liveEditorTagName changed from '%s' to '%s'", this,
							this.liveEditorTagName, liveEditorTagName));
				}
				this.liveEditorTagName = liveEditorTagName;
				this.modified = true;
			}
		}

		@Override
		public void setNewEditor(boolean newEditor) throws ReadOnlyException {
			if (this.newEditor != newEditor) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Change in '%s': newEditor changed from '%b' to '%b'", this,
							this.newEditor, newEditor));
				}
				this.newEditor = newEditor;
				this.modified = true;
			}
		}

		@Override
		public void setExternalEditorUrl(String externalEditorUrl) throws ReadOnlyException {
			externalEditorUrl = ObjectTransformer.getString(externalEditorUrl, "");
			if (!StringUtils.isEqual(this.externalEditorUrl, externalEditorUrl)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Change in '%s': externalEditorUrl changed from '%s' to '%s'", this,
							this.externalEditorUrl, externalEditorUrl));
				}
				this.externalEditorUrl = externalEditorUrl;
				this.modified = true;
			}
		}

		@Override
		public void setMayBeSubtag(boolean mayBeSubtag) throws ReadOnlyException {
			if (this.mayBeSubtag != mayBeSubtag) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Change in '%s': mayBeSubtag changed from '%b' to '%b'", this,
							this.mayBeSubtag, mayBeSubtag));
				}
				this.mayBeSubtag = mayBeSubtag;
				this.modified = true;
			}
		}

		@Override
		public void setMayContainSubtags(boolean mayContainSubtags) throws ReadOnlyException {
			if (this.mayContainSubtags != mayContainSubtags) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Change in '%s': mayContainSubtags changed from '%b' to '%b'", this,
							this.mayContainSubtags, mayContainSubtags));
				}
				this.mayContainSubtags = mayContainSubtags;
				this.modified = true;
			}
		}

		@Override
		public void setMlId(int mlId) throws ReadOnlyException {
			if (this.mlId != mlId) {
				this.mlId = mlId;
				this.modified = true;
			}
		}

		@Override
		public void setConstructCategoryId(Integer id) throws ReadOnlyException {
			if (categoryId != ObjectTransformer.getInt(id, 0)) {
				categoryId = ObjectTransformer.getInt(id, 0);
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
			List<UserLanguage> languages = UserLanguageFactory.getActive();

			for (UserLanguage lang : languages) {
				int id = lang.getId();
				// make the name unique
				editableName.put(id, CNDictionary.makeUnique(nameId, id, ObjectTransformer.getString(editableName.get(id), ""), C.Tables.CONSTRUCT, "name_id"));
				dicEntriesChanged |= CNDictionary.saveDicUserEntry(nameId, id, ObjectTransformer.getString(editableName.get(id), ""));
			}

			// save descriptions
			if (descriptionId <= 0) {
				descriptionId = CNDictionary.createNewOutputId();
				this.modified = true;
			}
			for (UserLanguage lang : languages) {
				int id = lang.getId();
				dicEntriesChanged |= CNDictionary.saveDicUserEntry(descriptionId, id, ObjectTransformer.getString(editableDescription.get(id), ""));
			}

			if (dicEntriesChanged && logger.isDebugEnabled()) {
				logger.debug(String.format("Change in '%s': dictionary entry changed", this));
			}

			// make the keyword unique
			setKeyword(UniquifyHelper.makeUnique(keyword, MAX_KEYWORD_LENGTH, "SELECT keyword FROM construct WHERE id != ? AND keyword = ?", ObjectTransformer.getInt(getId(), -1)));

			boolean isModified = this.modified;
			boolean doDirtingDueToModification = false;
			boolean isNew = isEmptyId(getId());
			Construct origConstruct = null;

			if (!isNew) {
				origConstruct = t.getObject(Construct.class, getId());

				// if the construct is not new, we save the parts now. This makes sure, that editor/edate of the construct
				// are modified even if only parts are changed. We only need to invoke dirting contents when parts have been modified.
				// All other properties of constructs are not used during page rendering.
				doDirtingDueToModification = saveParts();
				isModified |= doDirtingDueToModification;
			}

			// save the construct, if necessary
			if (isModified) {
				// set the editor data
				editor = t.getUserId();
				eDate = new ContentNodeDate(t.getUnixTimestamp());

				// for new objects, set creator data
				if (isEmptyId(getId())) {
					creator = t.getUserId();
					cDate = new ContentNodeDate(t.getUnixTimestamp());
				}

				saveFactoryObject(this);
				this.modified = false;
			}

			// for new constructs, we save the parts after saving the construct, because we need an id
			if (isNew) {
				isModified |= saveParts();
			}

			// change node assignment
			List<Integer> currentAssignedNodes = loadNodeIds();
			List<Node> newAssignedNodes = getNodes();
			List<Integer> toAssign = new Vector<Integer>();
			List<Integer> toUnassign = new Vector<Integer>(currentAssignedNodes);

			for (Node node : newAssignedNodes) {
				if (!currentAssignedNodes.contains(node.getId())) {
					// node must be assigned, but is not
					toAssign.add(ObjectTransformer.getInteger(node.getId(), null));
				}
				// node must not be unassigned
				toUnassign.remove(ObjectTransformer.getInteger(node.getId(), null));
			}

			if (!toUnassign.isEmpty()) {
				DBUtils.executeMassStatement("DELETE FROM construct_node WHERE construct_id = ? AND node_id IN", toUnassign, 2, new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, ObjectTransformer.getInt(getId(), -1));
					}
				});
			}
			if (!toAssign.isEmpty()) {
				for (Integer id : toAssign) {
					DBUtils.executeInsert("INSERT IGNORE INTO construct_node (construct_id, node_id) VALUES (?, ?)", new Object[] { getId(), id });
				}
			}

			isModified |= dicEntriesChanged;

			// logcmd and event
			if (isModified) {
				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, Construct.TYPE_CONSTRUCT, getId(), 0, "Construct.create");
					t.addTransactional(new TransactionalTriggerEvent(Construct.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, Construct.TYPE_CONSTRUCT, getId(), 0, "Construct.update");
					int eventMask = Events.UPDATE;
					if (doDirtingDueToModification) {
						eventMask += Events.EVENT_CN_CONTENT;
					}
					t.addTransactional(new TransactionalTriggerEvent(Construct.class, getId(), getModifiedData(origConstruct, this), eventMask));
				}
			}

			if (isModified) {
				t.dirtObjectCache(Construct.class, getId());
			}

			return isModified;
		}

		/**
		 * Save the parts of the construct
		 * @return true if parts were actually changed, false if not
		 * @throws NodeException
		 */
		protected boolean saveParts() throws NodeException {
			if (isEmptyId(getId())) {
				throw new NodeException("Cannot save parts for new constructs before the construct is saved.");
			}
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean isModified = false;

			// save parts
			List<Part> parts = getParts();
			List<Integer> partIdsToRemove = new Vector<Integer>();

			// make sure, we have the partIds loaded
			loadPartIds();
			if (partIds != null) {
				partIdsToRemove.addAll(partIds);
			}
			for (Part part : parts) {
				part.setConstructId(getId());
				isModified |= part.save();

				// do not remove the part, which was saved
				partIdsToRemove.remove(part.getId());
			}

			// remove parts which no longer exist
			if (!partIdsToRemove.isEmpty()) {
				List<Part> partsToRemove = t.getObjects(Part.class, partIdsToRemove);

				for (Part part : partsToRemove) {
					part.delete();
				}
				isModified = true;
			}

			return isModified;
		}
	}

	/**
	 * Factory Implementation of Construct Categories
	 */
	private static class FactoryConstructCategory extends ConstructCategory {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 3267621488269788097L;

		/**
		 * Name ID
		 */
		@DataField("name_id")
		protected int nameId;

		/**
		 * Name
		 */
		protected I18nString name;

		/**
		 * sortorder
		 */
		@DataField("sortorder")
		@Updateable
		protected int sortorder;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		public FactoryConstructCategory(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info object info
		 * @param nameId name id
		 * @param name name
		 * @param sortorder sortorder
		 */
		public FactoryConstructCategory(Integer id, NodeObjectInfo info, Map<String, Object> dataMap, int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
			this.name = new CNI18nString(Integer.toString(nameId));
			this.udate = udate;
			this.globalId = globalId;
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

		@Override
		public I18nString getName() {
			return name;
		}

		@Override
		public int getNameId() {
			return nameId;
		}

		@Override
		public int getSortorder() {
			return sortorder;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryConstructCategory(this,
					getFactory().getFactoryHandle(ConstructCategory.class).createObjectInfo(ConstructCategory.class, true), true);
		}

		@Override
		public int getEffectiveUdate() throws NodeException {
			int udate = super.getEffectiveUdate();
			ObjectFactory oFactory = TransactionManager.getCurrentTransaction().getObjectFactory(getObjectInfo().getObjectClass());

			udate = Math.max(udate, oFactory.getEffectiveOutputUserUdate(getNameId()));
			return udate;
		}

		@Override
		public List<Construct> getConstructs() throws NodeException {
			final List<Integer> constructIds = new Vector<Integer>();

			DBUtils.executeStatement("SELECT id FROM construct WHERE category_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, getId());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException,
							NodeException {
					while (rs.next()) {
						constructIds.add(rs.getInt("id"));
					}
				}
			});
			return TransactionManager.getCurrentTransaction().getObjects(Construct.class, constructIds);
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException,
					NodeException {
			// delete the construct
			Transaction t = TransactionManager.getCurrentTransaction();
			ConstructFactory factory = (ConstructFactory) t.getObjectFactory(ConstructCategory.class);

			factory.deleteCategory(this);
		}
	}

	/**
	 * Factory Implementation of Editable Construct Categories
	 */
	private static class EditableFactoryConstructCategory extends FactoryConstructCategory {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 1925312561823187102L;

		/**
		 * Flag to mark modified objects
		 */
		protected boolean modified = false;

		/**
		 * Editable name
		 */
		protected EditableI18nString editableName;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected EditableFactoryConstructCategory(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param category object
		 * @param info info
		 * @param asNewObject true for new objects, false for editable existing objects
		 * @throws NodeException
		 */
		protected EditableFactoryConstructCategory(FactoryConstructCategory category, NodeObjectInfo info,
				boolean asNewObject) throws NodeException {
			super(asNewObject ? null : category.getId(), info, getDataMap(category), asNewObject ? -1 : category.getUdate(),
					asNewObject ? null : category.getGlobalId());
			// read the names
			if (nameId > 0) {
				List<FactoryDataRow> entries = CNDictionary.getDicuserEntries(nameId);
				editableName = new EditableI18nString();

				for (FactoryDataRow dicEntry : entries) {
					editableName.put(dicEntry.getInt("language_id"), dicEntry.getString("value"));
				}
			}
			if (asNewObject) {
				modified = true;
				this.nameId = -1;
			}
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

		@Override
		public I18nString getName() {
			if (editableName != null) {
				return editableName;
			} else {
				return super.getName();
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
		public void setSortorder(int sortorder) throws ReadOnlyException {
			assertEditable();
			if (this.sortorder != sortorder) {
				this.sortorder = sortorder;
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
					// make the name unique
					editableName.put(id, CNDictionary.makeUnique(nameId, id, ObjectTransformer.getString(editableName.get(id), ""), C.Tables.CONSTRUCT_CATEGORY, "name_id"));
					dicEntriesChanged |= CNDictionary.saveDicUserEntry(nameId, id, ObjectTransformer.getString(editableName.get(id), ""));
				}
			}

			boolean isModified = this.modified;
			boolean isNew = isEmptyId(getId());
			ConstructCategory origObject = null;

			// when the object is not new, get the original field data
			if (!isNew) {
				origObject = t.getObject(ConstructCategory.class, getId());
			}

			// save the construct, if necessary
			if (isModified) {
				saveFactoryObject(this);
				this.modified = false;
			}

			isModified |= dicEntriesChanged;

			// logcmd and event
			if (isModified) {
				if (isNew) {
					// set the initial permissions
					setInitialPermissions();
					// add transactional to refresh the perm handler
					t.addTransactional(new RefreshPermHandler(TYPE_CONSTRUCT_CATEGORY, ObjectTransformer.getInt(getId(), 0)));

					ActionLogger.logCmd(ActionLogger.CREATE, ConstructCategory.TYPE_CONSTRUCT_CATEGORY, getId(), 0, "ConstructCategory.create");
					t.addTransactional(new TransactionalTriggerEvent(ConstructCategory.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, ConstructCategory.TYPE_CONSTRUCT_CATEGORY, getId(), 0, "ConstructCategory.update");
					// get modified attributes
					t.addTransactional(new TransactionalTriggerEvent(ConstructCategory.class, getId(), getModifiedData(origObject, this), Events.UPDATE));
				}
			}

			if (isModified) {
				t.dirtObjectCache(ConstructCategory.class, getId());
			}

			return isModified;
		}

		/**
		 * Sets initial permissions for the construct category
		 * @param id the object's id
		 * @throws NodeException 
		 */
		private void setInitialPermissions() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement s = null;

			try {
				s = t.prepareStatement("INSERT INTO perm (usergroup_id, o_type, o_id, perm) " + "(SELECT id, ?, ?, ? FROM usergroup)");
				s.setInt(1, TYPE_CONSTRUCT_CATEGORY);
				s.setObject(2, getId());
				s.setString(3, "10000000000000000000000000000000");
				s.execute();
			} catch (Exception e) {
				throw new NodeException(e);
			} finally {
				t.closeStatement(s);
			}
		}
	}

	public ConstructFactory() {
		super();
	}

	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) {
		if (ConstructCategory.class.equals(clazz)) {
			return (T) new EditableFactoryConstructCategory(handle.createObjectInfo(ConstructCategory.class, true));
		} else if (Construct.class.equals(clazz)) {
			return (T) new EditableFactoryConstruct(handle.createObjectInfo(Construct.class, true));
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#loadObject(java.lang.Class, java.lang.Integer, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		if (Construct.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_CONSTRUCT_SQL, null, null);
		} else if (ConstructCategory.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_CONSTRUCT_CATEGORY_SQL, null, null);
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.BatchObjectFactory#batchLoadObjects(java.lang.Class, java.util.Collection, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		String idSql = buildIdSql(ids);

		if (Construct.class.equals(clazz)) {
			String[] preloadSql = new String[] {
				"SELECT construct_id AS id, id AS id2 FROM part WHERE construct_id IN " + idSql,
				"SELECT part.construct_id AS id, value.id AS id2 FROM part,value WHERE part.id = value.part_id AND "
						+ "value.contenttag_id = 0 AND value.templatetag_id = 0 AND value.objtag_id = 0 AND " + "part.construct_id IN " + idSql };
    		
			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_CONSTRUCT_SQL + idSql, preloadSql);
		} else if (ConstructCategory.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_CONSTRUCT_CATEGORY_SQL + idSql);
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}

		if (object instanceof FactoryConstruct) {
			return (T) new EditableFactoryConstruct((FactoryConstruct) object, info, false);
		} else if (object instanceof FactoryConstructCategory) {
			return (T) new EditableFactoryConstructCategory((FactoryConstructCategory) object, info, false);
		} else {
			throw new NodeException("ConstructFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the delete lists for all types of tags
		AbstractFactory tagFactory = (AbstractFactory) t.getObjectFactory(ContentTag.class);
		Map<Class<? extends NodeObject>, Collection> tagDeleteLists = new HashMap<Class<? extends NodeObject>, Collection>();

		tagDeleteLists.put(ContentTag.class, tagFactory.getDeleteList(t, ContentTag.class));
		tagDeleteLists.put(TemplateTag.class, tagFactory.getDeleteList(t, TemplateTag.class));
		tagDeleteLists.put(ObjectTag.class, tagFactory.getDeleteList(t, ObjectTag.class));

		Collection<Construct> deletedConstructs = getDeleteList(t, Construct.class);

		if (!isEmptyDeleteList(t, Construct.class)) {
			// collect the output ids to be deleted
			List<Integer> outputIds = new Vector<Integer>();

			// check whether any of the constructs scheduled for deletion is
			// still used by a tag, which is not scheduled for deletion
			for (Construct construct : deletedConstructs) {
				List<Tag> tags = construct.getTags();

				for (Tag tag : tags) {
					if (!tagDeleteLists.get(tag.getObjectInfo().getObjectClass()).contains(tag)) {
						throw new NodeException("Cannot delete " + construct + ": it is (at least) used by " + tag + ", which is not deleted");
					}
				}
				outputIds.add(construct.getNameId());
				outputIds.add(construct.getDescriptionId());
			}

			// do logcmd and trigger event
			for (Construct construct : deletedConstructs) {
				ActionLogger.logCmd(ActionLogger.DEL, Construct.TYPE_CONSTRUCT, construct.getId(), null, "Construct.delete()");
				t.addTransactional(new TransactionalTriggerEvent(construct, null, Events.DELETE));
			}

			// delete the constructs
			DBUtils.selectAndDelete("dicuser", "SELECT id FROM dicuser WHERE output_id IN", outputIds);
			DBUtils.selectAndDelete("outputuser", "SELECT id FROM outputuser WHERE id IN", outputIds);
			flushDelete("DELETE FROM construct_node WHERE construct_id IN", Construct.class);
			flushDelete("DELETE FROM construct WHERE id IN", Construct.class);
		}

		if (!isEmptyDeleteList(t, ConstructCategory.class)) {
			// collect the ids of constructs, that will lose their category
			List<Integer> losingCategory = new Vector<Integer>();
			// collect the output ids to be deleted
			List<Integer> outputIds = new Vector<Integer>();
			Collection<ConstructCategory> deleteList = getDeleteList(t, ConstructCategory.class);

			for (ConstructCategory category : deleteList) {
				List<Construct> constructs = new Vector<Construct>(category.getConstructs());

				constructs.removeAll(deletedConstructs);
				for (Construct c : constructs) {
					losingCategory.add(ObjectTransformer.getInteger(c.getId(), null));
				}
				outputIds.add(category.getNameId());
			}

			if (!losingCategory.isEmpty()) {
				DBUtils.executeMassStatement("UPDATE construct SET category_id = 0 WHERE id IN ", losingCategory, 1, null);
				for (Integer id : losingCategory) {
					t.dirtObjectCache(Construct.class, id);
				}
			}

			// delete the categories
			DBUtils.selectAndDelete("dicuser", "SELECT id FROM dicuser WHERE output_id IN", outputIds);
			DBUtils.selectAndDelete("outputuser", "SELECT id FROM outputuser WHERE id IN", outputIds);

			// Delete permissions for construct categories
			flushDelete("DELETE FROM perm WHERE o_type = " + ConstructCategory.TYPE_CONSTRUCT_CATEGORY + " AND o_id IN", ConstructCategory.class);
			// update the PermissionStore
			for (ConstructCategory del : deleteList) {
				t.addTransactional(new RemovePermsTransactional(ConstructCategory.TYPE_CONSTRUCT_CATEGORY, ObjectTransformer.getInt(del.getId(), 0)));
			}

			flushDelete("DELETE FROM construct_category WHERE id IN ", ConstructCategory.class);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#loadResultSet(java.lang.Class, java.lang.Object, com.gentics.lib.base.object.NodeObjectInfo, com.gentics.contentnode.factory.object.FactoryDataRow, java.util.List<java.lang.Integer>[])
	 */
	@SuppressWarnings("unchecked")
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id,
			NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws NodeException {
		if (Construct.class.equals(clazz)) {
			return (T) loadConstructObject(id, info, rs, idLists);
		} else if (ConstructCategory.class.equals(clazz)) {
			return (T) loadConstructCategoryObject(id, info, rs, idLists);
		} else {
			return null;
		}
	}

	/**
	 * Load a construct
	 * @param id id
	 * @param info info
	 * @param rs object data
	 * @param idLists lists of referenced ids
	 * @return construct
	 */
	private Construct loadConstructObject(Integer id, NodeObjectInfo info,
			FactoryDataRow rs, List<Integer>[] idLists) throws NodeException {
		List<Integer> partIds = idLists != null ? idLists[0] : null;
		List<Integer> valueIds = idLists != null ? idLists[1] : null;

		return new FactoryConstruct(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs), partIds, valueIds);
	}

	/**
	 * Load a Construct Category
	 * @param id id
	 * @param info info
	 * @param rs object data
	 * @param idLists lists of referenced ids
	 * @return construct category
	 */
	private ConstructCategory loadConstructCategoryObject(Integer id,
			NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws NodeException {
		return new FactoryConstructCategory(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs));
	}

	/**
	 * Deletes a Construct Category
	 * @param category construct category
	 */
	protected void deleteCategory(ConstructCategory category) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<ConstructCategory> toDelete = getDeleteList(t, ConstructCategory.class);

		toDelete.add(category);
	}

	/**
	 * Deletes a Construct Category
	 * @param category construct category
	 */
	protected void deleteConstruct(Construct construct) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<Construct> toDelete = getDeleteList(t, Construct.class);

		toDelete.add(construct);
	}
}
