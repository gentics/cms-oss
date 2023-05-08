package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.ObjectFactory;
import com.gentics.contentnode.factory.RemovePermsTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.i18n.EditableI18nString;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Factory for handling ObjectTag Definitions
 * TODO: we deliberately do not add the @DBTable annotation for the ObjectTagDefinition, because the definitions are stored in the same
 * table as ObjectTag and this would generate a conflict
 */
@DBTables({
	@DBTable(clazz = ObjectTagDefinition.class, name="objtag", table2Class = false),
	@DBTable(clazz = ObjectTagDefinitionCategory.class, name = "objprop_category") })
public class ObjectTagDefinitionFactory extends AbstractFactory {
	/**
	 * Select clause for the columns of table objprop
	 */
	protected final static String OBJPROP_SELECT = StringUtils.merge(
			Arrays.asList("name_id", "description_id", "o_type", "keyword", "creator", "cdate", "editor", "edate", "objtag_id", "category_id", "sync_contentset", "sync_channelset", "sync_variants").toArray(), ",",
			"p.", "");

	/**
	 * SQL Statement to select objtag definition data
	 */
	protected final static String SELECT_OBJTAGDEF_SQL = "SELECT o.*, " + OBJPROP_SELECT
			+ ", p.id objprop_id FROM objtag o LEFT JOIN objprop p ON o.id = p.objtag_id WHERE o.id = ?";

	/**
	 * SQL Statement to batchload objtag definitions
	 */
	protected final static String BATCHLOAD_OBJTAGDEF_SQL = "SELECT o.*, " + OBJPROP_SELECT
			+ ", p.id objprop_id FROM objtag o LEFT JOIN objprop p ON o.id = p.objtag_id WHERE o.id IN ";

	/**
	 * SQL Statement to select objtag definition category data
	 */
	protected final static String SELECT_OBJTAGDEF_CAT_SQL = createSelectStatement("objprop_category");

	/**
	 * SQL Statement to batchload objtag definition categories
	 */
	protected final static String BATCHLOAD_OBJTAGDEF_CAT_SQL = createBatchLoadStatement("objprop_category");

	/**
	 * SQL Statement to insert a objprop entry
	 */
	protected final static String INSERT_OBJPROP_SQL = "INSERT INTO objprop (name_id, description_id, o_type, creator, cdate, editor, edate, objtag_id, category_id, sync_contentset, sync_channelset, sync_variants, uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL Statement to update a objprop entry
	 */
	protected final static String UPDATE_OBJPROP_SQL = "UPDATE objprop SET editor = ?, edate = ?, category_id = ?, sync_contentset = ?, sync_channelset = ?, sync_variants = ? WHERE id = ?";

	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.OBJPROP_CATEGORY, ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, true, FactoryObjectTagDefinitionCategory.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Create an instance
	 */
	public ObjectTagDefinitionFactory() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.BatchObjectFactory#batchLoadObjects(java.lang.Class, java.util.Collection, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> Collection<T> batchLoadObjects(
			Class<T> clazz, Collection<Integer> ids,
			NodeObjectInfo info) throws NodeException {
		String idSql = buildIdSql(ids);

		if (ObjectTagDefinition.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_OBJTAGDEF_SQL + idSql);
		} else if (ObjectTagDefinitionCategory.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_OBJTAGDEF_CAT_SQL + idSql);
		} else {
			return Collections.emptyList();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#createObject(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle,
			Class<T> clazz) throws NodeException {
		if (ObjectTagDefinition.class.equals(clazz)) {
			return (T) new EditableFactoryObjectTagDefinition(handle.createObjectInfo(ObjectTagDefinition.class, true));
		} else if (ObjectTagDefinitionCategory.class.equals(clazz)) {
			return (T) new EditableFactoryObjectTagDefinitionCategory(handle.createObjectInfo(ObjectTagDefinitionCategory.class, true));
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#loadObject(java.lang.Class, java.lang.Object, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id,
			NodeObjectInfo info) throws NodeException {
		if (ObjectTagDefinition.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_OBJTAGDEF_SQL, null, null);
		} else if (ObjectTagDefinitionCategory.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_OBJTAGDEF_CAT_SQL, null, null);
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}

		if (object instanceof FactoryObjectTagDefinition) {
			return (T) new EditableFactoryObjectTagDefinition((FactoryObjectTagDefinition) object, info, false);
		} else if (object instanceof FactoryObjectTagDefinitionCategory) {
			return (T) new EditableFactoryObjectTagDefinitionCategory((FactoryObjectTagDefinitionCategory) object, info, false);
		} else {
			throw new NodeException("ObjectTagDefinitionFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#loadResultSet(java.lang.Class, java.util.Properties.Integer, com.gentics.lib.base.object.NodeObjectInfo, com.gentics.contentnode.factory.object.FactoryDataRow, java.util.List<java.lang.Integer>[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id,
			NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		if (ObjectTagDefinition.class.equals(clazz)) {
			int nameId = rs.getInt("name_id");
			int descriptionId = rs.getInt("description_id");
			int objpropId = rs.getInt("objprop_id");
			int targetType = rs.getInt("obj_type");
			int categoryId = rs.getInt("category_id");
			boolean syncContentset = rs.getBoolean("sync_contentset");
			boolean syncChannelset = rs.getBoolean("sync_channelset");
			boolean syncVariants = rs.getBoolean("sync_variants");

			return (T) new FactoryObjectTagDefinition(id, info, nameId, descriptionId, objpropId, targetType,
					categoryId, syncContentset, syncChannelset, syncVariants, getUdate(rs), getGlobalId(rs));
		} else if (ObjectTagDefinitionCategory.class.equals(clazz)) {
			return (T) new FactoryObjectTagDefinitionCategory(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs));
		} else {
			return null;
		}
	}

	/**
	 * Save the objprop entry for the given definition
	 * @param def object tag definition
	 * @throws NodeException
	 */
	protected static void saveObjProp(EditableFactoryObjectTagDefinition def) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean isNew = ObjectTagDefinition.isEmptyId(def.objpropId);

		// set the editor data
		def.editorId = t.getUserId();
		def.eDate = new ContentNodeDate(t.getUnixTimestamp());

		if (isNew) {
			// set the creator data
			def.creatorId = t.getUserId();
			def.cDate = new ContentNodeDate(t.getUnixTimestamp());

			List<Integer> keys = DBUtils.executeInsert(INSERT_OBJPROP_SQL,
					new Object[] {
				def.nameId, def.descriptionId, def.targetType, def.creatorId, def.cDate.getIntTimestamp(), def.editorId, def.eDate.getIntTimestamp(),
				def.getId(), def.categoryId, def.syncContentset, def.syncChannelset, def.syncVariants, ""
			});

			if (keys.size() != 1) {
				throw new NodeException("Error while inserting new page, could not get the insertion id");
			}

			// set the new objpropId id
			def.objpropId = keys.get(0);

			// TODO set initial permissions
		} else {
			DBUtils.executeUpdate(UPDATE_OBJPROP_SQL, new Object[] {
				def.editorId, def.eDate.getIntTimestamp(), def.categoryId, def.syncContentset, def.syncChannelset, def.syncVariants, def.objpropId });

			if (def.syncChanged) {
				DBUtils.executeUpdate("UPDATE objtag SET in_sync = 0 WHERE obj_type = ? AND name = ? AND obj_id != 0",
						new Object[] { def.targetType, def.getObjectTag().getName() });
			}
		}
	}

	/**
	 * Factory Implementation of the ObjectTagDefinition
	 */
	private static class FactoryObjectTagDefinition extends ObjectTagDefinition {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -1736039046460727042L;

		/**
		 * Name ID
		 */
		protected int nameId;

		/**
		 * Name
		 */
		protected I18nString name;

		/**
		 * Description ID
		 */
		protected int descriptionId;

		/**
		 * Description
		 */
		protected I18nString description;

		/**
		 * objprop id
		 */
		protected int objpropId;

		/**
		 * Target ttype
		 */
		protected int targetType;

		/**
		 * Category ID
		 */
		protected int categoryId;

		/**
		 * List of node ids
		 */
		private List<Integer> nodeIds;

		/**
		 * Creator id
		 */
		protected int creatorId = 0;

		/**
		 * Creation date
		 */
		protected ContentNodeDate cDate = new ContentNodeDate(0);

		/**
		 * Editor id
		 */
		protected int editorId = 0;

		/**
		 * Edit date
		 */
		protected ContentNodeDate eDate = new ContentNodeDate(0);

		/**
		 * Synchronize Contentset Flag
		 */
		protected boolean syncContentset;

		/**
		 * Synchronize Channelset Flag
		 */
		protected boolean syncChannelset;

		/**
		 * Synchronize Variants Flag
		 */
		protected boolean syncVariants;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected FactoryObjectTagDefinition(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param nameId name id
		 * @param descriptionId description id
		 * @param objPropId object property id
		 * @param targetType target type
		 * @param categoryId category id
		 * @param syncContentset sync Contentset flag
		 * @param syncChannelset sync Channelset flag
		 * @param syncVariants sync Variants flag
		 * @param udate udate
		 * @param globalId globalid
		 */
		protected FactoryObjectTagDefinition(Integer id, NodeObjectInfo info, int nameId, int descriptionId,
				int objPropId, int targetType, int categoryId, boolean syncContentset, boolean syncChannelset,
				boolean syncVariants, int udate, GlobalId globalId) {
			super(id, info);
			this.udate = udate;
			this.globalId = globalId;
			this.nameId = nameId;
			if (nameId > 0) {
				name = new CNI18nString(Integer.toString(nameId));
			}
			this.descriptionId = descriptionId;
			if (descriptionId > 0) {
				description = new CNI18nString(Integer.toString(descriptionId));
			}
			this.objpropId = objPropId;
			this.targetType = targetType;
			this.categoryId = categoryId;
			this.syncContentset = syncContentset;
			this.syncChannelset = syncChannelset;
			this.syncVariants = syncVariants;
		}

		@Override
		public String getName() {
			if (name != null) {
				return name.toString();
			} else {
				try {
					return getObjectTag().getName();
				} catch (NodeException e) {
					return null;
				}
			}
		}

		@Override
		public int getNameId() {
			return nameId;
		}

		@Override
		public String getDescription() {
			if (description != null) {
				return description.toString();
			} else {
				return null;
			}
		}

		@Override
		public int getDescriptionId() {
			return descriptionId;
		}

		@Override
		public int getObjectPropId() {
			return objpropId;
		}

		@Override
		public ObjectTag getObjectTag() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(ObjectTag.class, id, getObjectInfo().isEditable());
		}

		@Override
		public int getTargetType() {
			return targetType;
		}

		@Override
		public ObjectTagDefinitionCategory getCategory() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(ObjectTagDefinitionCategory.class, categoryId);
		}

		@Override
		public boolean isSyncContentset() {
			return syncContentset;
		}

		@Override
		public boolean isSyncChannelset() {
			return syncChannelset;
		}

		@Override
		public boolean isSyncVariants() {
			return syncVariants;
		}

		/**
		 * Load the node ids
		 * @return list of node ids
		 * @throws NodeException
		 */
		protected synchronized List<Integer> loadNodeIds() throws NodeException {
			if (nodeIds == null) {
				if (isEmptyId(getId())) {
					nodeIds = new ArrayList<>();
				} else {
					nodeIds = DBUtils.select("SELECT node_id id FROM objprop_node WHERE objprop_id = ?",
							ps -> ps.setInt(1, objpropId), DBUtils.IDLIST);
				}
			}
			return nodeIds;
		}

		@Override
		public List<Node> getNodes() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObjects(Node.class, loadNodeIds());
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryObjectTagDefinition(this,
					getFactory().getFactoryHandle(ObjectTagDefinition.class).createObjectInfo(ObjectTagDefinition.class, true), true);
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
		public List<ObjectTag> getObjectTags() throws NodeException {
			final List<Integer> objTagIds = new Vector<Integer>();
			final ObjectTag tag = getObjectTag();

			DBUtils.executeStatement("SELECT id FROM objtag WHERE obj_type = ? AND name = ? AND obj_id != ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, getTargetType());
					stmt.setString(2, tag.getName());
					stmt.setInt(3, 0);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException,
							NodeException {
					while (rs.next()) {
						objTagIds.add(rs.getInt("id"));
					}
				}
			});
			return TransactionManager.getCurrentTransaction().getObjects(ObjectTag.class, objTagIds);
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException,
					NodeException {
			// delete the objtag
			getObjectTag().delete();

			// delete the objprop
			Transaction t = TransactionManager.getCurrentTransaction();
			ObjectTagDefinitionFactory factory = (ObjectTagDefinitionFactory) t.getObjectFactory(ObjectTagDefinition.class);

			factory.deleteObjTagDefinition(this);
		}

		@Override
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.dirtObjectCache(ObjectTag.class, id, false);
		}

		@Override
		public I18nString getNameI18n() {
			return name;
		}

		@Override
		public I18nString getDescriptionI18n() {
			return description;
		}

		@Override
		public String toString() {
			return "Object Property {" + getName() + ", " + getId() + "}";
		}
	}

	/**
	 * Factory Implementation of Editable ObjectTagDefinition
	 */
	private static class EditableFactoryObjectTagDefinition extends FactoryObjectTagDefinition {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 3658621738436418623L;

		/**
		 * Flag to mark whether this instance has been modified
		 */
		protected boolean modified = false;

		/**
		 * Editable name
		 */
		protected EditableI18nString editableName;

		/**
		 * Editable description
		 */
		protected EditableI18nString editableDescription;

		/**
		 * Editable instance of the object tag
		 */
		protected ObjectTag editableObjectTag;

		/**
		 * List of nodes to which this object property is linked to
		 */
		protected List<Node> nodes;

		/**
		 * Flag which is set, when the synchronization options are changed
		 */
		protected boolean syncChanged = false;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected EditableFactoryObjectTagDefinition(NodeObjectInfo info) throws NodeException {
			super(info);
			this.modified = true;
			Transaction t = TransactionManager.getCurrentTransaction();

			// create a new object tag
			editableObjectTag = t.createObject(ObjectTag.class);
		}

		/**
		 * Create a new instance
		 * @param def definition
		 * @param info object info
		 * @param asNew true for creating as new object, false for an editable copy
		 * @throws NodeException
		 */
		protected EditableFactoryObjectTagDefinition(FactoryObjectTagDefinition def, NodeObjectInfo info, boolean asNew) throws NodeException {
			super(asNew ? null : def.getId(), info, def.nameId, def.descriptionId, def.objpropId, def.targetType,
					def.categoryId, def.syncContentset, def.syncChannelset, def.syncVariants,
					asNew ? -1 : def.getUdate(), asNew ? null : def.getGlobalId());
			// read the names and descriptions
			if (nameId > 0) {
				List<FactoryDataRow> entries = CNDictionary.getDicuserEntries(nameId);
				editableName = new EditableI18nString();

				for (FactoryDataRow dicEntry : entries) {
					editableName.put(dicEntry.getInt("language_id"), dicEntry.getString("value"));
				}
			}
			if (descriptionId > 0) {
				List<FactoryDataRow> entries = CNDictionary.getDicuserEntries(descriptionId);
				editableDescription = new EditableI18nString();

				for (FactoryDataRow dicEntry : entries) {
					editableDescription.put(dicEntry.getInt("language_id"), dicEntry.getString("value"));
				}
			}
			if (asNew) {
				// reset internal ids
				this.nameId = -1;
				this.descriptionId = -1;
				this.objpropId = -1;
				// get the object tag
				getObjectTag();
				// and make a copy
				this.editableObjectTag = (ObjectTag) editableObjectTag.copy();
				this.modified = true;
			}
		}

		@Override
		public ObjectTag getObjectTag() throws NodeException {
			if (editableObjectTag == null) {
				editableObjectTag = super.getObjectTag();
			}
			return editableObjectTag;
		}

		@Override
		public String getName() {
			if (editableName != null) {
				return editableName.toString();
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
		public String getDescription() {
			if (editableDescription != null) {
				return editableDescription.toString();
			} else {
				return super.getDescription();
			}
		}

		@Override
		public void setDescription(String description, int language) throws ReadOnlyException {
			if (editableDescription == null) {
				editableDescription = new EditableI18nString();
			}
			editableDescription.put(language, description);
		}

		@Override
		public void setTargetType(int targetType) throws ReadOnlyException {
			if (this.targetType != targetType) {
				this.targetType = targetType;
				this.modified = true;
			}
		}

		@Override
		public void setCategoryId(Integer id) throws ReadOnlyException {
			if (this.categoryId != ObjectTransformer.getInt(id, -1)) {
				this.categoryId = ObjectTransformer.getInt(id, -1);
				this.modified = true;
			}
		}

		@Override
		public void setSyncContentset(boolean syncContentset) throws ReadOnlyException {
			if (this.syncContentset != syncContentset) {
				this.syncContentset = syncContentset;
				this.modified = true;
				this.syncChanged = true;
			}
		}

		@Override
		public void setSyncChannelset(boolean syncChannelset) throws ReadOnlyException {
			if (this.syncChannelset != syncChannelset) {
				this.syncChannelset = syncChannelset;
				this.modified = true;
				this.syncChanged = true;
			}
		}

		@Override
		public void setSyncVariants(boolean syncVariants) throws ReadOnlyException {
			if (this.syncVariants != syncVariants) {
				this.syncVariants = syncVariants;
				this.modified = true;
				this.syncChanged = true;
			}
		}

		@Override
		public List<Node> getNodes() throws NodeException {
			if (nodes == null) {
				nodes = new Vector<Node>(super.getNodes());
			}
			return nodes;
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException,
					NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			ObjectTag objectTag = getObjectTag();

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
					editableName.put(id,
							UniquifyHelper.makeUnique(ObjectTransformer.getString(editableName.get(id), ""), 0,
									"SELECT value FROM dicuser WHERE language_id = ? AND output_id != ? AND output_id IN (SELECT name_id FROM objprop LEFT JOIN objtag ON objtag_id = objtag.id WHERE obj_type = ?) AND value = ?",
									SeparatorType.blank, id, nameId, objectTag.getObjType()));
					dicEntriesChanged |= CNDictionary.saveDicUserEntry(nameId, id, ObjectTransformer.getString(editableName.get(id), ""));
				}
			}

			// save descriptions
			if (descriptionId <= 0) {
				descriptionId = CNDictionary.createNewOutputId();
				this.modified = true;
			}
			if (editableDescription != null) {
				for (UserLanguage lang : UserLanguageFactory.getActive()) {
					int id = lang.getId();
					dicEntriesChanged |= CNDictionary.saveDicUserEntry(descriptionId, id, ObjectTransformer.getString(editableDescription.get(id), ""));
				}
			}

			boolean isModified = this.modified;

			boolean isNew = isEmptyId(getId());

			// save the objecttag
			// the globalId is the globalId of the objecttag, so set it
			if (isEmptyId(objectTag.getId())) {
				objectTag.setGlobalId(globalId);
			}

			// make the objectTag name unique
			objectTag.setName(UniquifyHelper.makeUnique(objectTag.getName(), ObjectTagDefinition.MAX_NAME_LENGTH,
					"SELECT name FROM objtag WHERE obj_type = ? AND obj_id = 0 AND id != ? AND name = ?", objectTag.getObjType(),
					ObjectTransformer.getInt(objectTag.getId(), -1)));

			isModified |= objectTag.save();
			// get the id from the objecttag
			if (isEmptyId(id)) {
				id = objectTag.getId();
			}
			this.targetType = objectTag.getObjType();

			// set the initial permissions
			if (isNew) {
				PermHandler.duplicatePermissions(ObjectTagDefinition.TYPE_OBJTAG_DEF_FOR_TYPE, targetType, ObjectTagDefinition.TYPE_OBJTAG_DEF,
						ObjectTransformer.getInt(id, -1));
			}

			// now probably save the objprop
			if (this.modified) {
				saveObjProp(this);
				this.modified = false;
			}

			// possibly assigned nodes have been changed
			if (nodes != null) {
				// find differences in the linked nodes and save them
				List<Object> nodeIdsToAdd = new Vector<Object>();
				List<Object> nodeIdsToRemove = new Vector<Object>();

				// currently set nodes
				final List<Integer> nodeIds = loadNodeIds();

				nodeIdsToRemove.addAll(nodeIds);

				// find nodes, which shall be added or removed
				for (Node n : nodes) {
					nodeIdsToRemove.remove(n.getId());
					if (!nodeIds.contains(n.getId())) {
						// first node id
						nodeIdsToAdd.add(n.getId());
						// then objprop id
						nodeIdsToAdd.add(objpropId);
					}
				}

				// now add the missing nodes
				if (nodeIdsToAdd.size() > 0) {
					DBUtils.executeInsert("INSERT INTO objprop_node (node_id, objprop_id) VALUES " + StringUtils.repeat("(?,?)", nodeIdsToAdd.size() / 2, ","),
							(Object[]) nodeIdsToAdd.toArray(new Object[nodeIdsToAdd.size()]));
				}

				// ... and remove the surplus nodes
				if (nodeIdsToRemove.size() > 0) {
					nodeIdsToRemove.add(0, objpropId);
					DBUtils.executeUpdate(
							"DELETE FROM objprop_node WHERE objprop_id = ? AND node_id IN (" + StringUtils.repeat("?", nodeIdsToRemove.size() - 1, ",") + ")",
							(Object[]) nodeIdsToRemove.toArray(new Object[nodeIdsToRemove.size()]));
				}
			}

			isModified |= dicEntriesChanged;

			// add logcmd and trigger event
			if (isModified) {
				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, ObjectTagDefinition.TYPE_OBJTAG_DEF, getId(), 0, "ObjectTagDefinition.create");
					t.addTransactional(new TransactionalTriggerEvent(ObjectTagDefinition.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, ObjectTagDefinition.TYPE_OBJTAG_DEF, getId(), 0, "ObjectTagDefinition.update");
					t.addTransactional(new TransactionalTriggerEvent(ObjectTagDefinition.class, getId(), null, Events.UPDATE));
				}
			}

			t.dirtObjectCache(ObjectTagDefinition.class, getId());

			return isModified;
		}
	}

	/**
	 * Factory Implementation of the ObjectTagDefinitionCategory
	 */
	private static class FactoryObjectTagDefinitionCategory extends ObjectTagDefinitionCategory {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -6870069597748345538L;

		/**
		 * Name ID
		 */
		@DataField("name_id")
		protected int nameId;

		/**
		 * Name
		 */
		private I18nString name;

		/**
		 * Sortorder
		 */
		@DataField("sortorder")
		@Updateable
		protected int sortorder;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected FactoryObjectTagDefinitionCategory(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param nameId name id
		 * @param sortorder sort order
		 * @param udate udate
		 * @param globalId globalId
		 */
		protected FactoryObjectTagDefinitionCategory(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap, int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			this.udate = udate;
			this.globalId = globalId;
			setDataMap(this, dataMap);
			if (nameId > 0) {
				this.name = new CNI18nString(Integer.toString(nameId));
			}
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
		public int getNameId() {
			return nameId;
		}

		@Override
		public String getName() {
			if (name != null) {
				return name.toString();
			} else {
				return null;
			}
		}

		@Override
		public I18nString getNameI18n() {
			return name;
		}

		@Override
		public int getSortorder() {
			return sortorder;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryObjectTagDefinitionCategory(this,
					getFactory().getFactoryHandle(ObjectTagDefinitionCategory.class).createObjectInfo(ObjectTagDefinitionCategory.class, true), true);
		}

		@Override
		public int getEffectiveUdate() throws NodeException {
			int udate = super.getEffectiveUdate();
			ObjectFactory oFactory = TransactionManager.getCurrentTransaction().getObjectFactory(getObjectInfo().getObjectClass());

			udate = Math.max(udate, oFactory.getEffectiveOutputUserUdate(getNameId()));
			return udate;
		}

		@Override
		public List<ObjectTagDefinition> getObjectTagDefinitions() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObjects(ObjectTagDefinition.class,
					DBUtils.select("SELECT objtag_id id FROM objprop WHERE category_id = ?", pst -> pst.setInt(1, getId()), DBUtils.IDS));
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			// delete the category
			Transaction t = TransactionManager.getCurrentTransaction();
			ObjectTagDefinitionFactory factory = (ObjectTagDefinitionFactory) t.getObjectFactory(ObjectTagDefinitionCategory.class);

			factory.deleteCategory(this);
		}
	}

	/**
	 * Factory Implementation of the editable ObjectTagDefinitionCategory
	 */
	private static class EditableFactoryObjectTagDefinitionCategory extends FactoryObjectTagDefinitionCategory {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -3568314672636889653L;

		/**
		 * Flag to mark whether this instance has been modified
		 */
		protected boolean modified = false;

		/**
		 * Editable name
		 */
		protected EditableI18nString editableName;

		/**
		 * Create an empty instance
		 * @param info
		 * @throws NodeException
		 */
		protected EditableFactoryObjectTagDefinitionCategory(NodeObjectInfo info) throws NodeException {
			super(info);
			this.modified = true;
		}

		/**
		 * Create an editable copy of the given category
		 * @param cat category
		 * @param info info
		 * @param asNew true for creating a new object, false for an editable copy
		 * @throws NodeException
		 */
		protected EditableFactoryObjectTagDefinitionCategory(FactoryObjectTagDefinitionCategory cat, NodeObjectInfo info, boolean asNew) throws NodeException {
			super(asNew ? null : cat.getId(), info, getDataMap(cat), asNew ? -1 : cat.getUdate(), asNew ? null : cat.getGlobalId());
			// read the names and descriptions
			if (nameId > 0) {
				editableName = new EditableI18nString();
				List<FactoryDataRow> entries = CNDictionary.getDicuserEntries(nameId);

				for (FactoryDataRow dicEntry : entries) {
					editableName.put(dicEntry.getInt("language_id"), dicEntry.getString("value"));
				}
			}
			if (asNew) {
				this.modified = true;
				this.nameId = -1;
			}
		}

		@Override
		public String getName() {
			if (editableName != null) {
				return editableName.toString();
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
			if (this.sortorder != sortorder) {
				this.sortorder = sortorder;
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException,
					NodeException {
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
					editableName.put(id, CNDictionary.makeUnique(nameId, id, ObjectTransformer.getString(editableName.get(id), ""), C.Tables.OBJPROP_CATEGORY, "name_id"));
					dicEntriesChanged |= CNDictionary.saveDicUserEntry(nameId, id, ObjectTransformer.getString(editableName.get(id), ""));
				}
			}

			boolean isModified = this.modified;
			boolean isNew = isEmptyId(getId());

			// now probably save the objprop
			if (this.modified) {
				saveFactoryObject(this);
				this.modified = false;
			}

			isModified |= dicEntriesChanged;
			// add logcmd and trigger event
			if (isModified) {
				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, getId(), 0,
							"ObjectTagDefinitionCategory.create");
					t.addTransactional(new TransactionalTriggerEvent(ObjectTagDefinitionCategory.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, getId(), 0,
							"ObjectTagDefinitionCategory.update");
					t.addTransactional(new TransactionalTriggerEvent(ObjectTagDefinitionCategory.class, getId(), null, Events.UPDATE));
				}
			}

			t.dirtObjectCache(ObjectTagDefinitionCategory.class, getId());

			return isModified;
		}
	}

	/**
	 * Delete the given object tag definition
	 * @param def definition
	 * @throws NodeException
	 */
	protected void deleteObjTagDefinition(ObjectTagDefinition def) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<ObjectTagDefinition> toDelete = getDeleteList(t, ObjectTagDefinition.class);

		toDelete.add(def);
	}

	/**
	 * Deletes a object tag def Category
	 * @param category object tag def category
	 */
	protected void deleteCategory(ObjectTagDefinitionCategory category) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<ObjectTagDefinitionCategory> toDelete = getDeleteList(t, ObjectTagDefinitionCategory.class);

		toDelete.add(category);
	}

	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Collection<ObjectTagDefinition> toDel = getDeleteList(t, ObjectTagDefinition.class);

		if (!isEmptyDeleteList(t, ObjectTagDefinition.class)) {
			// get the deleted objecttags
			AbstractFactory tagFactory = (AbstractFactory) t.getObjectFactory(ObjectTag.class);
			Collection<ObjectTag> deletedObjTags = tagFactory.getDeleteList(t, ObjectTag.class);

			// collect the output ids to be deleted
			List<Integer> outputIds = new Vector<Integer>();

			// get the objtag definitions to delete
			List<Integer> objPropIds = new Vector<Integer>();

			for (ObjectTagDefinition def : toDel) {
				List<ObjectTag> tags = def.getObjectTags();

				for (ObjectTag tag : tags) {
					if (!deletedObjTags.contains(tag)) {
						throw new NodeException("Cannot delete " + def + " at least " + tag + " is still using it");
					}
				}

				outputIds.add(def.getNameId());
				outputIds.add(def.getDescriptionId());
				outputIds.add(def.getObjectPropId());

				objPropIds.add(def.getObjectPropId());

				// logcmd and event
				ActionLogger.logCmd(ActionLogger.DEL, ObjectTagDefinition.TYPE_OBJTAG_DEF, def.getId(), 0, "ObjectTagDefinition.delete");
				Events.trigger(def, null, Events.DELETE);
			}

			// dic entries
			DBUtils.selectAndDelete("dicuser", "SELECT id FROM dicuser WHERE output_id IN", outputIds);
			DBUtils.selectAndDelete("outputuser", "SELECT id FROM outputuser WHERE id IN", outputIds);
			// node assignments
			DBUtils.executeMassStatement("DELETE FROM objprop_node WHERE objprop_id IN", objPropIds, 1, null);
			// delete the objprops
			DBUtils.selectAndDelete("objprop", "SELECT id FROM objprop WHERE id IN", objPropIds);
			// permissions
			flushDelete("DELETE FROM perm WHERE o_type = " + ObjectTagDefinition.TYPE_OBJTAG_DEF + " AND o_id IN", ObjectTagDefinition.class);
			// refresh permission store
			for (ObjectTagDefinition def : toDel) {
				t.addTransactional(new RemovePermsTransactional(ObjectTagDefinition.TYPE_OBJTAG_DEF, ObjectTransformer.getInt(def.getId(), 0)));
			}
		}

		if (!isEmptyDeleteList(t, ObjectTagDefinitionCategory.class)) {
			// collect the ids of object tag definitions, that will lose their category
			List<Integer> losingCategory = new ArrayList<Integer>();
			// collect the output ids to be deleted
			List<Integer> outputIds = new ArrayList<Integer>();
			Collection<ObjectTagDefinitionCategory> deleteList = getDeleteList(t, ObjectTagDefinitionCategory.class);

			for (ObjectTagDefinitionCategory category : deleteList) {
				losingCategory.addAll(category.getObjectTagDefinitions().stream().filter(def -> !toDel.contains(def)).map(ObjectTagDefinition::getId)
						.collect(Collectors.toList()));
				outputIds.add(category.getNameId());
			}

			if (!losingCategory.isEmpty()) {
				DBUtils.executeMassStatement("UPDATE objprop SET category_id = -9999 WHERE id IN ", losingCategory, 1, null);
				for (Integer id : losingCategory) {
					t.dirtObjectCache(ObjectTagDefinition.class, id);
				}
			}

			// delete the categories
			DBUtils.selectAndDelete("dicuser", "SELECT id FROM dicuser WHERE output_id IN", outputIds);
			DBUtils.selectAndDelete("outputuser", "SELECT id FROM outputuser WHERE id IN", outputIds);

			// Delete permissions for construct categories
			flushDelete("DELETE FROM perm WHERE o_type = " + ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY + " AND o_id IN", ObjectTagDefinitionCategory.class);
			// update the PermissionStore
			for (ObjectTagDefinitionCategory del : deleteList) {
				t.addTransactional(new RemovePermsTransactional(ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, del.getId()));
			}

			flushDelete("DELETE FROM objprop_category WHERE id IN ", ObjectTagDefinitionCategory.class);
		}
	}
}
