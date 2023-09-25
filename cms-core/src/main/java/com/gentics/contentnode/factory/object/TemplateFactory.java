/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: TemplateFactory.java,v 1.32.2.1.2.7 2011-02-10 18:58:39 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import static com.gentics.api.lib.etc.ObjectTransformer.getInt;
import static com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration.isFeature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractTemplate;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.MarkupLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.genericexceptions.IllegalUsageException;

import io.reactivex.Flowable;

/**
 * An objectfactory which can create {@link Template} objects, based on the {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = Template.class, name = "template") })
public class TemplateFactory extends AbstractFactory {

	/**
	 * Name of the transaction attribute, that stored templates that are locked by this transaction
	 */
	protected final static String LOCKED_TEMPLATES_IN_TRX = "TemplateFactory.lockedTemplateInTrx";

	/**
	 * SQL Statement to insert a new template
	 */
	protected final static String INSERT_TEMPLATE_SQL = "INSERT INTO template (name, description, locked, locked_by, "
			+ "folder_id, creator, cdate, editor, edate, ml_id, ml, channelset_id, channel_id, is_master, templategroup_id, uuid) VALUES "
			+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL Statement to insert a new templategroup
	 */
	protected final static String INSERT_TEMPLATEGROUP_SQL = "INSERT INTO templategroup (id, uuid) VALUES (NULL, ?)";

	/**
	 * SQL Statement to link a template to a folder
	 */
	protected final static String LINK_TEMPLATE_TO_FOLDER_SQL = "INSERT INTO template_folder (template_id, folder_id) VALUES (?, ?)";

	/**
	 * SQL Statement to update a template
	 */
	protected final static String UPDATE_TEMPLATE_SQL = "UPDATE template SET name = ?, description = ?, folder_id = ?, "
			+ "editor = ?, edate = ?, ml_id = ?, ml = ?, channelset_id = ?, channel_id = ?, is_master = ? WHERE id = ?";

	/**
	 * SQL Statement to select a single template
	 */
	protected final static String SELECT_TEMPLATE_SQL = createSelectStatement("template");

	/**
	 * SQL Statement for batchloading templates
	 */
	protected final static String BATCHLOAD_TEMPLATE_SQL = createBatchLoadStatement("template");

	private static class FactoryTemplate extends AbstractTemplate {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 497106297501713469L;

		protected String name;
		protected String source;
		protected String description = "";
		protected Integer mlId = 1;
		protected List<Integer> objTagIds;
		protected List<Integer> tagIds;
		protected List<Integer> privateTagIds;
		protected Integer folderId;
		protected Integer templategroupId;

		protected Integer editorId = 0;
		protected Integer creatorId = 0;
		protected ContentNodeDate cDate = new ContentNodeDate(0);
		protected ContentNodeDate eDate = new ContentNodeDate(0);

		protected int locked;

		/**
		 * Id of the user, who locked the template
		 */
		protected int lockedBy;

		/**
		 * id of the channelset, if the object is master or local copy in multichannelling
		 */
		protected int channelSetId;

		/**
		 * id of the channel, if the object is a local copy in multichannelling
		 */
		protected int channelId;

		/**
		 * True if this object is the master, false if it is a localized copy
		 */
		protected boolean master = true;

		/**
		 * Map holding the localized variants of this page
		 */
		protected Map<Integer, Integer> channelSet;

		/**
		 * IDs of folders the template is linked to
		 */
		protected Set<Integer> folderIds;

		/**
		 * Create new empty instance of a template
		 * @param info
		 */
		public FactoryTemplate(NodeObjectInfo info) {
			super(null, info);
		}

		public FactoryTemplate(Integer id, NodeObjectInfo info, String name, String description,
				String source, Integer mlId, int locked, int lockedBy, List tagIds, Integer folderId, Integer templategroupId,
				int creatorId, int editorId, ContentNodeDate cDate, ContentNodeDate eDate,
				Integer channelSetId, Integer channelId, boolean master, int udate, GlobalId globalId) {
			super(id, info);
			this.name = name;
			this.description = description;
			this.source = source;
			this.mlId = mlId;
			this.locked = locked;
			this.lockedBy = lockedBy;
			this.tagIds = tagIds != null ? new Vector(tagIds) : null;
			this.folderId = folderId;
			this.templategroupId = templategroupId;
			this.channelSetId = ObjectTransformer.getInt(channelSetId, 0);
			this.channelId = ObjectTransformer.getInt(channelId, 0);
			this.creatorId = creatorId;
			this.editorId = editorId;
			this.cDate = cDate;
			this.eDate = eDate;
			this.master = master;
			this.udate = udate;
			this.globalId = globalId;
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

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public String getSource() {
			return source;
		}

		public Integer getMlId() {
			return mlId;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Template#performDelete()
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			TemplateFactory templateFactory = (TemplateFactory) t.getObjectFactory(Template.class);

			templateFactory.deleteTemplate(this);
		}

		public MarkupLanguage getMarkupLanguage() throws NodeException {
			MarkupLanguage ml = (MarkupLanguage) TransactionManager.getCurrentTransaction().getObject(MarkupLanguage.class, mlId);

			// check data consistency
			assertNodeObjectNotNull(ml, mlId, "MarkupLanguage");
			return ml;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Template#isLocked()
		 */
		public boolean isLocked() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			int lockTimeout = ObjectTransformer.getInt(t.getNodeConfig().getDefaultPreferences().getProperty("lock_time"), 600);

			return (locked != 0 && (t.getUnixTimestamp() - locked) < lockTimeout);
		}

		@Override
		public boolean isLockedByCurrentUser() throws NodeException {
			if (isLocked()) {
				return lockedBy == TransactionManager.getCurrentTransaction().getUserId();
			} else {
				return false;
			}
		}

		@Override
		public SystemUser getLockedBy() throws NodeException {
			if (isLocked()) {
				Transaction t = TransactionManager.getCurrentTransaction();

				return t.getObject(SystemUser.class, lockedBy);
			} else {
				return null;
			}
		}

		@Override
		public boolean isInherited() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
				// multichannelling is not used, so the template cannot be inherited
				return false;
			}

			// determine the current channel id
			Node channel = t.getChannel();

			if (channel == null || !channel.isChannel()) {
				return false;
			}
			// the template is inherited if its channelid is different from the current channel
			return ObjectTransformer.getInt(channel.getId(), -1) != ObjectTransformer.getInt(this.channelId, -1);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Template#getPrivateTemplateTags()
		 */
		public Map<String, TemplateTag> getPrivateTemplateTags() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			Map<String, TemplateTag> templateTags = new HashMap<String, TemplateTag>();

			loadPrivateTemplateTagIds();

			List<TemplateTag> tags = t.getObjects(TemplateTag.class, privateTagIds);

			for (TemplateTag tag : tags) {
				templateTags.put(tag.getName(), tag);
			}

			return templateTags;
		}

		private synchronized void loadPrivateTemplateTagIds() throws NodeException {
			if (privateTagIds == null) {
				if (isEmptyId(getId())) {
					privateTagIds = new ArrayList<>();
				} else {
					privateTagIds = DBUtils.select("SELECT id FROM templatetag WHERE template_id = ? AND pub = 0",
							ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
				}
			}
		}

		public Map<String, TemplateTag> getTemplateTags() throws NodeException {
			return loadTemplateTags();
		}

		private synchronized void loadTemplateTagIds() throws NodeException {
			if (tagIds == null) {
				// when the template is new, it has no templatetags to load
				if (isEmptyId(getId())) {
					tagIds = new ArrayList<>();
				} else {
					tagIds = DBUtils.select("SELECT t.id FROM templatetag t"
							+ " LEFT JOIN construct c ON c.id = t.construct_id"
							+ " WHERE template_id = ? AND c.id IS NOT NULL", ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
				}
			}
		}

		private Map<String, TemplateTag> loadTemplateTags() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			Map<String, TemplateTag> templateTags = new HashMap<String, TemplateTag>();

			loadTemplateTagIds();

			List<TemplateTag> tags = t.getObjects(TemplateTag.class, tagIds, getObjectInfo().isEditable());

			for (TemplateTag tag : tags) {
				templateTags.put(tag.getName(), tag);
			}

			return templateTags;
		}

		private synchronized void loadObjectTagIds() throws NodeException {
			if (objTagIds == null) {
				if (isEmptyId(getId())) {
					objTagIds = new ArrayList<>();
				} else {
					objTagIds = DBUtils.select("SELECT t.id as id FROM objtag t"
							+ " LEFT JOIN construct c ON c.id = t.construct_id"
							+ " WHERE t.obj_id = ? AND t.obj_type = ? AND c.id IS NOT NULL", ps -> {
								ps.setInt(1, getId());
								ps.setInt(2, Template.TYPE_TEMPLATE);
							}, DBUtils.IDLIST);
				}
			}
		}

		protected Map loadObjectTags() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			Map<String, ObjectTag> objectTags = new HashMap<>();

			loadObjectTagIds();

			Collection<ObjectTag> tags;

			tags = t.getObjects(ObjectTag.class, objTagIds, getObjectInfo().isEditable());
			for (ObjectTag tag : tags) {
				if (tag == null) {
					continue;
				}
				String name = tag.getName();

				if (name.startsWith("object.")) {
					name = name.substring(7);
				}

				objectTags.put(name, tag);
			}

			// when the template is editable, get all objecttags which are assigned to the template's nodes
			if (getObjectInfo().isEditable()) {
				// get all node IDs to which the template is assigned (master nodes)
				Set<Integer> nodeIds = new HashSet<>(
						Flowable.fromIterable(getNodes()).map(Node::getMaster).map(Node::getId).toList().blockingGet());

				if (!nodeIds.isEmpty()) {
					List<Integer> objTagDefIds = new ArrayList<>();
					DBUtils.executeStatement("SELECT DISTINCT objtag.id id FROM objtag"
							+ " LEFT JOIN objprop ON objtag.id = objprop.objtag_id"
							+ " LEFT JOIN objprop_node ON objprop.id = objprop_node.objprop_id"
							+ " LEFT JOIN construct c ON c.id = objtag.construct_id"
							+ " WHERE objtag.obj_id = 0 AND objtag.obj_type = ?"
							+ " AND (objprop_node.node_id IN (" + StringUtils.repeat("?", nodeIds.size(), ",")
							+ ") OR objprop_node.node_id IS NULL)"
							+ " AND c.id IS NOT NULL", Transaction.SELECT_STATEMENT, pst -> {
								int counter = 1;
								pst.setInt(counter++, getTType());
								for (Integer nodeId : nodeIds) {
									pst.setInt(counter++, nodeId);
								}
							}, rs -> {
								while (rs.next()) {
									objTagDefIds.add(rs.getInt("id"));
								}
							});

					if (objTagDefIds.size() > 0) {
						// get the objtag (definition)
						List<ObjectTag> objTagDefs = t.getObjects(ObjectTag.class, objTagDefIds);

						for (ObjectTag def : objTagDefs) {
							// get the name (without object. prefix)
							String defName = def.getName();

							if (defName.startsWith("object.")) {
								defName = defName.substring(7);
							}

							// if no objtag of that name exists for the file,
							// generate a copy and add it to the map of objecttags
							if (!objectTags.containsKey(defName)) {
								ObjectTag newObjectTag = (ObjectTag) def.copy();

								newObjectTag.setNodeObject(this);
								newObjectTag.setEnabled(false);
								objectTags.put(defName, newObjectTag);
							}
						}
					}
				}

				// migrate object tags to new constructs, if they were changed
				for (ObjectTag tag : objectTags.values()) {
					tag.migrateToDefinedConstruct();
				}
			}

			return objectTags;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Template {" + getName() + ", " + getId() + "}";
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
		 */
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			loadTemplateTagIds();
			for (Integer tagId : tagIds) {
				t.dirtObjectCache(TemplateTag.class, tagId, false);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Template#getFolder()
		 */
		public Folder getFolder() throws NodeException {
			Folder folder = (Folder) TransactionManager.getCurrentTransaction().getObject(Folder.class, folderId);

			return folder;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Template#getFolders()
		 */
		public List<Folder> getFolders() throws NodeException {
			if (isFeature(Feature.MULTICHANNELLING) && !isMaster()) {
				return getMaster().getFolders();
			}

			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObjects(Folder.class, getFolderIds());
		}

		@Override
		public Set<Integer> getFolderIds() throws NodeException {
			if (isFeature(Feature.MULTICHANNELLING) && !isMaster()) {
				return getMaster().getFolderIds();
			}
			if (folderIds == null) {
				int tId = getInt(getId(), 0);
				if (tId > 0) {
					folderIds = DBUtils.select("SELECT folder_id id FROM template_folder WHERE template_id = ?", pst -> {
						pst.setInt(1, tId);
					}, DBUtils.IDS);
				} else {
					folderIds = Collections.emptySet();
				}
			}
			return folderIds;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryTemplate(this, getFactory().getFactoryHandle(Template.class).createObjectInfo(Template.class, true), true);
		}

		@Override
		public Map<Integer, Integer> getChannelSet() throws NodeException {
			return new HashMap<>(loadChannelSet());
		}

		/**
		 * Internal method to load the channelset
		 * @return channelset
		 * @throws NodeException
		 */
		protected synchronized Map<Integer, Integer> loadChannelSet() throws NodeException {
			if (!isEmptyId(channelSetId) && ObjectTransformer.isEmpty(channelSet)) {
				channelSet = null;
			}
			if (channelSet == null) {
				if (isEmptyId(channelSetId)) {
					channelSet = new HashMap<Integer, Integer>();
				} else {
					channelSet = DBUtils.select("SELECT id, channel_id FROM template WHERE channelset_id = ?", ps -> {
						ps.setInt(1, channelSetId);
					}, rs -> {
						Map<Integer, Integer> tmpMap = new HashMap<>();
						while(rs.next()) {
							tmpMap.put(rs.getInt("channel_id"), rs.getInt("id"));
						}
						return tmpMap;
					});
				}
			}
			return channelSet;
		}

		@Override
		public Node getChannel() throws NodeException {
			if (!isEmptyId(channelId)) {
				Transaction t = TransactionManager.getCurrentTransaction();

				return (Node) t.getObject(Node.class, channelId, -1, false);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.LocalizableNodeObject#getNodeOrChannel()
		 */
		public Node getOwningNode() throws NodeException {
			return null;
		}

		/**
		 * @see {@link Template#isUnlinkable()}
		 */
		public boolean isUnlinkable(Folder folder) throws NodeException {
			// Check if this template is linked to at least one folder. The
			// template is unlinkable if it's linked to only one folder which
			// contains pages that reference to this template. The template can
			// be unlinked completely if there are no pages that have references
			// to that template.
			int folderCount = DBUtils.select("select count(*) from template_folder where template_id = ?", stmt -> {
				stmt.setInt(1, getId());
			}, rs ->{
				if (rs.next()) {
					return rs.getInt(1);
				} else {
					return 0;
				}
			});
			if (folderCount > 1) {
				return true;
			}
			int pageCount = DBUtils.select("select count(*) from page where template_id = ?", stmt -> {
				stmt.setInt(1, getId());
			}, rs -> {
				if (rs.next()) {
					return rs.getInt(1);
				} else {
					return 0;
				}
			});
			return pageCount == 0;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.LocalizableNodeObject#getChannelSetId()
		 */
		public Integer getChannelSetId() throws NodeException {
			if (isEmptyId(channelSetId)) {
				throw new NodeException(this + " does not have a valid channelset_id");
			}
			return channelSetId;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cDate;
		}

		@Override
		public ContentNodeDate getEDate() {
			return eDate;
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			SystemUser creator = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);

			// check for data consistency
			assertNodeObjectNotNull(creator, creatorId, "SystemUser", true);
			return creator;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			SystemUser editor = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);

			// check for data consistency
			assertNodeObjectNotNull(editor, editorId, "SystemUser", true);
			return editor;
		}

		@Override
		public boolean isMaster() throws NodeException {
			return master;
		}

		@Override
		public Integer getTemplategroupId() {
			return templategroupId;
		}

		@Override
		public List<Page> getPages() throws NodeException {
			final List<Integer> pageIds = new Vector<Integer>();

			DBUtils.executeStatement("SELECT id FROM page WHERE template_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, getId());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException,
							NodeException {
					while (rs.next()) {
						pageIds.add(rs.getInt("id"));
					}
				}
			});

			return TransactionManager.getCurrentTransaction().getObjects(Page.class, pageIds);
		}

		@Override
		public Set<Node> getNodes() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			Set<Node> affectedNodes = new HashSet<Node>();
			final List<Integer> allNodeIds = new ArrayList<Integer>();
			DBUtils.executeStatement("SELECT id FROM node", new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						allNodeIds.add(rs.getInt(1));
					}
				}
			});
			List<Node> allNodes = t.getObjects(Node.class, allNodeIds);
			for(Node n : allNodes) {
				if (MultichannellingFactory.isVisibleInNode(n, this)) {
					affectedNodes.add(n);
				}
			}
			return affectedNodes;
		}

		@Override
		public Set<Node> getAssignedNodes() throws NodeException {
			if (!isMaster()) {
				return getMaster().getAssignedNodes();
			} else {
				Set<Node> assignedNodes = new HashSet<>();
				Transaction t = TransactionManager.getCurrentTransaction();
				List<Node> nodes = t.getObjects(Node.class,
						DBUtils.select("select node.id from node, folder where node.folder_id = folder.id and folder.type_id = 10001", DBUtils.IDS));
				for (Node node : nodes) {
					if (node.getTemplates().contains(this)) {
						assignedNodes.add(node);
					}
				}
				return assignedNodes;
			}
		}

		@Override
		public UpdatePagesResult updatePages(int commitAfter, int maxMessages, List<String> tagnames, boolean force) throws NodeException {
			// Which channels are affected?
			Set<Node> affectedNodes = getNodes();

			return new MultiPageUpdater(p -> {
				p.migrateContenttags(this, tagnames, force);
			}).setIds(DBUtils.select("SELECT id FROM page WHERE deleted = 0 AND template_id = ?",
					pst -> pst.setInt(1, getMaster().getId()), DBUtils.IDS)).setCommitAfter(commitAfter)
					.setMaxMessages(maxMessages).setFilter(p -> {
						Node pageNode = p.getChannel() != null ? p.getChannel() : p.getFolder().getNode();
						return affectedNodes.contains(pageNode) && p.needsContenttagMigration(this, tagnames, force);
					}).execute();
		}

		@Override
		public void unlock() throws NodeException {
			// unlock the template, if locked for this user.
			Transaction t = TransactionManager.getCurrentTransaction();

			// check whether templates shall be unlocked immediately or on trx commit
			if (ObjectTransformer.getBoolean(t.getAttributes().get(NodeFactory.UNLOCK_AT_TRX_COMMIT), false)) {
				t.addTransactional(new UnlockTemplateTransactional(ObjectTransformer.getInt(getId(), -1)));
			} else {
				TemplateFactory.unlock(getId(), t.getUserId());
				locked = 0;
				lockedBy = 0;
			}
		}
	}

	/**
	 * Class for implementation of an editable template
	 */
	private static class EditableFactoryTemplate extends FactoryTemplate {

		/**
		 * editable copy of this template's objecttags
		 */
		private Map<String, ObjectTag> editableObjectTags;

		/**
		 * editable copy of this template's templatetags
		 */
		private Map<String, TemplateTag> editableTemplateTags;

		/**
		 * Flag to mark whether the template has been modified (contains changes which need to be persistet by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Flag to mark whether the channelset of this page was changed, or not
		 */
		private boolean channelSetChanged = false;

		/**
		 * Editable list of folders
		 */
		protected List<Folder> editableFolders;

		/**
		 * Global templategroup id
		 */
		protected GlobalId globalTemplateGroupId;

		/**
		 * Create a new empty instance of a template
		 * @param info info about the instance
		 * @throws NodeException
		 */
		protected EditableFactoryTemplate(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
		}

		/**
		 * Constructor for creating a copy of the given template
		 * @param template template
		 * @param info info about the copy
		 * @param asNewTemplate true when the editable copy shall represent a new
		 *        template, false if it shall be the editable version of the same
		 *        template
		 * @throws NodeException when an internal error occurred
		 * @throws ReadOnlyException when the template could not be fetched for
		 *         update
		 */
		protected EditableFactoryTemplate(FactoryTemplate template, NodeObjectInfo info, boolean asNewTemplate) throws ReadOnlyException, NodeException {
			// TODO set some values differently, depending on whether this is a new template
			super(asNewTemplate ? null : template.getId(), info, template.name, template.description, template.source, template.mlId,
					asNewTemplate ? -1 : template.locked, asNewTemplate ? 0 : template.lockedBy, asNewTemplate ? null : template.tagIds, template.folderId, asNewTemplate ? 0 : template.templategroupId,
					template.creatorId, template.editorId, template.cDate, template.eDate, asNewTemplate ? 0 : template.channelSetId, template.channelId,
					template.master, asNewTemplate ? -1 : template.getUdate(), asNewTemplate ? null : template.getGlobalId());
			if (asNewTemplate) {
				// copy the objecttags
				Map<String, ObjectTag> originalObjectTags = template.getObjectTags();

				editableObjectTags = new HashMap<String, ObjectTag>(originalObjectTags.size());
				for (Iterator<Map.Entry<String, ObjectTag>> i = originalObjectTags.entrySet().iterator(); i.hasNext();) {
					Map.Entry<String, ObjectTag> entry = i.next();

					editableObjectTags.put(entry.getKey(), (ObjectTag) entry.getValue().copy());
				}

				// copy the templatetags
				Map<String, TemplateTag> originalTemplateTags = template.getTemplateTags();

				editableTemplateTags = new HashMap<String, TemplateTag>(originalTemplateTags.size());
				for (Iterator<Map.Entry<String, TemplateTag>> i = originalTemplateTags.entrySet().iterator(); i.hasNext();) {
					Map.Entry<String, TemplateTag> entry = i.next();

					editableTemplateTags.put(entry.getKey(), (TemplateTag) entry.getValue().copy());
				}

				modified = true;
			}
			// TODO lock the template (which will also fail whether the template was locked by another user)
		}

		@Override
		public List<Folder> getFolders() throws NodeException {
			if (editableFolders == null) {
				editableFolders = new Vector<Folder>(super.getFolders());
			}
			return editableFolders;
		}

		@Override
		public void addFolder(Folder folder) throws NodeException {
			if (editableFolders == null) {
				editableFolders = new Vector<Folder>(super.getFolders());
			}
			editableFolders.add(folder);
		}

		@Override
		public Map<String, TemplateTag> getTemplateTags() throws NodeException {
			if (editableTemplateTags == null) {
				editableTemplateTags = super.getTemplateTags();
			}

			return editableTemplateTags;
		}

		@Override
		public Map<String, ObjectTag> getObjectTags() throws NodeException {
			if (editableObjectTags == null) {
				editableObjectTags = super.getObjectTags();
			}

			return editableObjectTags;
		}

		@Override
		public String setName(String name) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.name, name)) {
				String oldName = this.name;

				this.modified = true;
				this.name = name;
				return oldName;
			} else {
				return this.name;
			}
		}

		@Override
		public String setDescription(String description) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.description, description)) {
				String oldDescription = this.description;

				this.modified = true;
				this.description = description;
				return oldDescription;
			} else {
				return this.description;
			}
		}

		@Override
		public String setSource(String source) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.source, source)) {
				String oldSource = this.source;

				this.modified = true;
				this.source = source;
				return oldSource;
			} else {
				return this.source;
			}
		}

		@Override
		public Integer setMlId(Integer mlId) throws ReadOnlyException {
			assertEditable();
			if (ObjectTransformer.getInt(this.mlId, 0) != ObjectTransformer.getInt(mlId, 0)) {
				Integer oldMlId = this.mlId;

				this.mlId = mlId;
				modified = true;

				return oldMlId;
			} else {
				return this.mlId;
			}
		}

		@Override
		public Integer setFolderId(Integer folderId) throws NodeException, ReadOnlyException {
			// always set the folder id of the master folder
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder folder = t.getObject(Folder.class, folderId);

			folderId = folder.getMaster().getId();

			assertEditable();
			if (ObjectTransformer.getInt(this.folderId, 0) != ObjectTransformer.getInt(folderId, 0)) {
				Integer oldFolderId = this.folderId;

				this.folderId = folderId;
				modified = true;

				return oldFolderId;
			} else {
				return this.folderId;
			}
		}

		@Override
		public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException {
			// ignore this call, if the channelinfo is already set identical
			if (ObjectTransformer.getInt(this.channelId, 0) == ObjectTransformer.getInt(channelId, 0)
					&& this.channelSetId == ObjectTransformer.getInt(channelSetId, 0)) {
				return;
			}

			// check whether the template is new
			if (!isEmptyId(getId()) && (!allowChange || this.channelSetId != ObjectTransformer.getInt(channelSetId, 0))) {
				// the template is not new, so we must not set the channel
				// information
				throw new NodeException("Cannot change channel information for {" + this + "}, because the template is not new");
			}

			if (ObjectTransformer.getInt(channelId, 0) != 0) {
				// check whether channel exists
				Node channel = TransactionManager.getCurrentTransaction().getObject(Node.class, channelId, -1, false);
				if (channel == null) {
					throw new NodeException("Error while setting channel information: channel {" + channelId + "} does not exist");
				}
				// if the channel exists, but is not channel, we set the channelId to 0 instead
				if (!channel.isChannel()) {
					channelId = 0;
				}
			}

			int iChannelId = ObjectTransformer.getInt(channelId, 0);
			int iChannelSetId = ObjectTransformer.getInt(channelSetId, 0);

			// check whether channelId was set to 0 and channelSetId not
			if (iChannelId == 0 && iChannelSetId != 0) {
				throw new NodeException(
						"Error while setting channel information: channelId was set to {" + channelId + "} and channelSetId was set to {" + channelSetId
						+ "}, which is not allowed (when creating master objects in non-channels, the channelSetId must be autogenerated)");
			}
			// set the data
			this.channelId = ObjectTransformer.getInt(channelId, 0);

			if (iChannelSetId == 0) {
				// when the channelsetid is given as 0 (or empty), we are about
				// to create a master object, so create a channelset id and set
				// master to true
				this.channelSetId = ObjectTransformer.getInt(createChannelsetId(), 0);
			} else {
				// the channelset id is given (not 0), so we are about to create a localized object.
				// TODO we can do plausibility checks here
				this.channelSetId = ObjectTransformer.getInt(channelSetId, 0);
			}
			channelSet = null;

			// set the "master" flag to false, because we are not yet sure,
			// whether this object is a master or not
			this.master = false;

			// now get the master object
			Template master = getMaster();

			if (master == this) {
				this.master = true;
			} else {
				this.master = false;
			}

			modified = true;
			channelSetChanged = true;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.LocalizableNodeObject#modifyChannelId(java.lang.Integer)
		 */
		public void modifyChannelId(Integer channelId) throws ReadOnlyException,
					NodeException {
			if (isEmptyId(getId())) {
				throw new NodeException("Cannot modify the channelId for a new folder");
			}
			if (isEmptyId(this.channelId)) {
				throw new NodeException("Cannot modify the channelId for {" + this + "}, since the folder does not belong to a channel");
			}
			if (!isMaster()) {
				throw new NodeException("Cannot modify the channelId for {" + this + "}, because it is no master");
			}

			// read the channelset
			Map<Integer, Integer> channelSet = getChannelSet();
			Integer oldChannelId = this.channelId;

			if (isEmptyId(channelId)) {
				this.channelId = 0;
				modified = true;
			} else {
				List<Node> masterNodes = getChannel().getMasterNodes();
				boolean foundMasterNode = false;

				for (Node node : masterNodes) {
					if (node.getId().equals(channelId)) {
						foundMasterNode = true;
						break;
					}
				}

				if (!foundMasterNode) {
					throw new NodeException(
							"Cannot modify the channelId for {" + this + "} to {" + channelId + "}, because this is no master channel of the folder's channel");
				}

				this.channelId = ObjectTransformer.getInt(channelId, 0);
				modified = true;
			}

			// modify the channelset, since this object moved from one channel to another
			channelSet.remove(oldChannelId);
			channelSet.put(this.channelId, getId());
			channelSetChanged = true;
		}

		@Override
		public void setTemplategroupId(Integer templategroupId) throws ReadOnlyException {
			if (ObjectTransformer.getInt(this.templategroupId, 0) != ObjectTransformer.getInt(templategroupId, 0)) {
				this.templategroupId = templategroupId;
				this.modified = true;
			}
		}

		@Override
		public void setGlobalTemplategroupId(GlobalId globalId) throws ReadOnlyException, NodeException {
			if (this.globalTemplateGroupId == null || !this.globalTemplateGroupId.equals(globalId)) {
				this.globalTemplateGroupId = globalId;
				if (this.globalTemplateGroupId != null) {
					Integer localId = this.globalTemplateGroupId.getLocalId(C.Tables.TEMPLATEGROUP);

					if (!isEmptyId(localId)) {
						setTemplategroupId(localId);
					}
				}
			}
		}

		@Override
		public boolean save(boolean syncPages) throws InsufficientPrivilegesException, NodeException {
			// first check whether the page is editable
			assertEditable();

			// when no folderId is set, but the template is assigned to at least one folder, we set the folderId
			if (isEmptyId(folderId) && !ObjectTransformer.isEmpty(editableFolders)) {
				folderId = editableFolders.get(0).getId();
			}
			if (folderId == null) {
				folderId = 0;
			}

			boolean isModified = modified;
			boolean isNew = AbstractTemplate.isEmptyId(getId());
			boolean objectSaved = false;

			// now check whether the object has been modified
			if (isModified) {
				// object is modified, so update it
				saveTemplateObject(this);
				modified = false;
				objectSaved = true;
			}

			Transaction t = TransactionManager.getCurrentTransaction();

			// save all the objecttags and check which tags no longer exist (and need to be removed)
			Map<String, ObjectTag> tags = getObjectTags();
			List<Integer> tagIdsToRemove = new Vector<Integer>();

			if (objTagIds != null) {
				tagIdsToRemove.addAll(objTagIds);
			}
			for (Iterator<ObjectTag> i = tags.values().iterator(); i.hasNext();) {
				ObjectTag tag = i.next();

				boolean tagIsNew = isEmptyId(tag.getId());
				tag.setNodeObject(this);
				boolean tagModified = tag.save();
				if (tagModified) {
					ActionLogger.logCmd(ActionLogger.EDIT, Template.TYPE_TEMPLATE, getId(), tag.getId(), (tagIsNew ? "create" : "update") + " tag {" + tag.getName() + "}");
				}
				isModified |= tagModified;

				// do not remove the tag, which was saved
				tagIdsToRemove.remove(tag.getId());
			}

			// eventually remove tags which no longer exist
			if (!tagIdsToRemove.isEmpty()) {
				List<ObjectTag> tagsToRemove = t.getObjects(ObjectTag.class, tagIdsToRemove);

				for (Iterator<ObjectTag> i = tagsToRemove.iterator(); i.hasNext();) {
					ObjectTag tagToRemove = i.next();

					ActionLogger.logCmd(ActionLogger.EDIT, Template.TYPE_TEMPLATE, getId(), tagToRemove.getId(), "delete tag {" + tagToRemove.getName() + "}");
					tagToRemove.delete();
				}
				isModified = true;
			}

			// save all templatetags and check which tags no longer exist (and need to be removed)
			Map<String, TemplateTag> tTags = getTemplateTags();
			boolean isTemplateTagModified = false;

			tagIdsToRemove.clear();
			if (tagIds != null) {
				tagIdsToRemove.addAll(tagIds);
			}
			for (Iterator<TemplateTag> i = tTags.values().iterator(); i.hasNext();) {
				TemplateTag tag = i.next();

				boolean tagIsNew = isEmptyId(tag.getId());
				tag.setTemplateId(getId());
				boolean tagModified = tag.save();
				if (tagModified) {
					ActionLogger.logCmd(ActionLogger.EDIT, Template.TYPE_TEMPLATE, getId(), tag.getId(), (tagIsNew ? "create" : "update") + " tag {" + tag.getName() + "}");
				}
				isModified |= isTemplateTagModified |= tagModified;

				// do not remove the tag, which was saved
				tagIdsToRemove.remove(tag.getId());
			}

			// eventually remove tags which no longer exist
			if (!tagIdsToRemove.isEmpty()) {
				List<TemplateTag> tagsToRemove = t.getObjects(TemplateTag.class, tagIdsToRemove);

				for (Iterator<TemplateTag> i = tagsToRemove.iterator(); i.hasNext();) {
					TemplateTag tagToRemove = i.next();

					ActionLogger.logCmd(ActionLogger.EDIT, Template.TYPE_TEMPLATE, getId(), tagToRemove.getId(), "delete tag {" + tagToRemove.getName() + "}");
					tagToRemove.delete();
				}
				isModified = true;
				isTemplateTagModified = true;
			}

			// update pages when at least one template tag has been created / modified
			// this is not yet perfect, as it calls updatePages a little too often
			if (syncPages && isTemplateTagModified) {
				updatePages(0, 0);
			}

			if (isModified) {
				// dirt the template cache
				TransactionManager.getCurrentTransaction().dirtObjectCache(Template.class, getId());
			}

			// collect all folder ids of folders to be dirted here
			Collection<Integer> folderIdsToDirt = new Vector<Integer>();

			// possibly assigned folders have been changed
			if (editableFolders != null && isMaster()) {
				// find differences in the linked folders and save them
				List<Integer> folderIdsToAdd = new Vector<Integer>();
				List<Integer> folderIdsToRemove = new Vector<Integer>();
				Set<Integer> nodeIdsToAdd = new HashSet<>();

				// currently set folders
				final List<Integer> folderIds = new Vector<Integer>();

				DBUtils.executeStatement("SELECT folder_id FROM template_folder WHERE template_id = ?", new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setObject(1, getId());
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							folderIds.add(rs.getInt("folder_id"));
						}
					}
				});
				folderIdsToRemove.addAll(folderIds);

				// find folders, which shall be added or removed
				for (Folder f : editableFolders) {
					Folder masterF = f.getMaster();
					folderIdsToRemove.remove(masterF.getId());
					if (!folderIds.contains(masterF.getId())) {
						// first folder id
						folderIdsToAdd.add(masterF.getId());
						// then template id
						folderIdsToAdd.add(getId());

						// folder will be dirted
						folderIdsToDirt.add(masterF.getId());

						// also assign template to node
						nodeIdsToAdd.add(masterF.getNode().getMaster().getId());
					}
				}

				// we need to dirt all folders from which the template will be
				// removed
				folderIdsToDirt.addAll(folderIdsToRemove);

				// now add the missing templates
				if (folderIdsToAdd.size() > 0) {
					DBUtils.executeInsert(
							"INSERT INTO template_folder (folder_id, template_id) VALUES " + StringUtils.repeat("(?,?)", folderIdsToAdd.size() / 2, ","),
							(Object[]) folderIdsToAdd.toArray(new Object[folderIdsToAdd.size()]));
				}

				// ... and remove the surplus templates
				if (folderIdsToRemove.size() > 0) {
					folderIdsToRemove.add(0, getId());
					DBUtils.executeUpdate(
							"DELETE FROM template_folder WHERE template_id = ? AND folder_id IN (" + StringUtils.repeat("?", folderIdsToRemove.size() - 1, ",") + ")",
							(Object[]) folderIdsToRemove.toArray(new Object[folderIdsToRemove.size()]));
				}

				// assign to nodes
				if (nodeIdsToAdd.size() > 0) {
					int templateId = getId();
					List<Object[]> args = nodeIdsToAdd.stream().map(id -> new Object[] {templateId, id}).collect(Collectors.toList());
					DBUtils.executeBatchInsert("INSERT IGNORE INTO template_node (template_id, node_id) VALUES (?, ?)", args);

					for (int nodeId : nodeIdsToAdd) {
						t.dirtObjectCache(Node.class, nodeId);
					}
				}
			}

			// if the channelset changed, we need to dirt all other templates of the channelset as well
			if (channelSetChanged) {
				Map<Integer, Integer> locChannelSet = getChannelSet();

				// dirt caches for all templates in the channelset
				for (Map.Entry<Integer, Integer> channelSetEntry : locChannelSet.entrySet()) {
					t.dirtObjectCache(Template.class, channelSetEntry.getValue());
				}

				PreparedStatement stmt = null;
				ResultSet res = null;

				try {
					StringBuffer sql = new StringBuffer("SELECT DISTINCT folder_id FROM template_folder WHERE template_id IN (");

					sql.append(StringUtils.repeat("?", locChannelSet.size() + 1, ","));
					sql.append(")");
					stmt = t.prepareStatement(sql.toString());

					int pCounter = 1;

					stmt.setObject(pCounter++, getId());
					for (Object templateId : locChannelSet.values()) {
						stmt.setObject(pCounter++, templateId);
					}

					res = stmt.executeQuery();

					while (res.next()) {
						if (!folderIdsToDirt.contains(res.getInt("folder_id"))) {
							folderIdsToDirt.add(res.getInt("folder_id"));
						}
					}
				} catch (SQLException e) {
					throw new NodeException("Error while getting folder ids of template", e);
				} finally {
					t.closeResultSet(res);
					t.closeStatement(stmt);
				}

				channelSetChanged = false;
			}

			// when the template is modified, but was not saved itself (i.e. a tag was modified), we update editor/edate
			if (isModified && !objectSaved) {
				// set the editor data
				editorId = t.getUserId();
				eDate = new ContentNodeDate(t.getUnixTimestamp());
				DBUtils.executeUpdate("UPDATE template SET editor = ?, edate = ? WHERE id = ?", new Object[] {editorId, eDate.getIntTimestamp(), getId()});
			}

			// dirt caches for all folders the template is linked to
			for (Integer folderId : folderIdsToDirt) {
				t.dirtObjectCache(Folder.class, folderId);
			}

			if (isModified) {
				// we need to sent the NOTIFY event for the template in order to allow synchronization (for feature devtoos)
				t.addTransactional(new TransactionalTriggerEvent(Template.class, getId(), null, Events.NOTIFY));
			}

			if (isNew) {
				updateMissingReferences();
			}

			return isModified;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);
			Template oTemplate = (Template) original;

			// copy meta data
			setName(oTemplate.getName());
			setDescription(oTemplate.getDescription());
			setMlId(oTemplate.getMlId());
			setSource(oTemplate.getSource());

			// copy object tags
			Map<String, ObjectTag> thisOTags = getObjectTags();
			Map<String, ObjectTag> originalOTags = oTemplate.getObjectTags();

			for (Map.Entry<String, ObjectTag> entry : originalOTags.entrySet()) {
				String tagName = entry.getKey();
				ObjectTag originalTag = entry.getValue();

				if (thisOTags.containsKey(tagName)) {
					// found the tag in this template, copy the original tag over it
					thisOTags.get(tagName).copyFrom(originalTag);
				} else {
					// did not find the tag, so copy the original
					thisOTags.put(tagName, (ObjectTag) originalTag.copy());
				}
			}

			// remove all tags that do not exist in the original template
			for (Iterator<Map.Entry<String, ObjectTag>> i = thisOTags.entrySet().iterator(); i.hasNext();) {
				Entry<String, ObjectTag> entry = i.next();

				if (!originalOTags.containsKey(entry.getKey())) {
					i.remove();
				}
			}

			// copy template tags
			Map<String, TemplateTag> thisTTags = getTemplateTags();
			Map<String, TemplateTag> originalTTags = oTemplate.getTemplateTags();

			for (Entry<String, TemplateTag> entry : originalTTags.entrySet()) {
				String tagName = entry.getKey();
				TemplateTag originalTag = entry.getValue();

				if (thisTTags.containsKey(tagName)) {
					// found the tag in this template, copy the original tag over it
					thisTTags.get(tagName).copyFrom(originalTag);
				} else {
					// did not find the tag, so copy the original
					thisTTags.put(tagName, (TemplateTag) originalTag.copy());
				}
			}

			// remove all tags that do not exist in the original template
			for (Iterator<Map.Entry<String, TemplateTag>> i = thisTTags.entrySet().iterator(); i.hasNext();) {
				Entry<String, TemplateTag> entry = i.next();

				if (!originalTTags.containsKey(entry.getKey())) {
					i.remove();
				}
			}
		}

		@Override
		public Integer getChannelSetId() throws NodeException {
			// check if the object is new and does not have a channelset_id
			if (isEmptyId(channelSetId) && isEmptyId(id)) {
				// create a new channelset_id
				channelSetId = ObjectTransformer.getInt(createChannelsetId(), 0);
			}

			return super.getChannelSetId();
		}
	}

	public TemplateFactory() {
		super();
	}

	/**
	 * Deletes a template but instead of directly deleting the folder the delete action is cached.
	 * When the transaction is committed the delete is performed.
	 * @param template the template to delete
	 * @throws NodeException
	 */
	protected void deleteTemplate(Template template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection deleteList = getDeleteList(t, Template.class);

		deleteList.add(template);

		// when multichannelling is active and the template is a master, also get all localized objects and
		// remove them
		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && template.isMaster()) {
			Map<Integer, Integer> channelSet = template.getChannelSet();

			for (Integer templateId : channelSet.values()) {
				Template locTemplate = (Template) t.getObject(Template.class, templateId, -1, false);

				if (locTemplate != null && !deleteList.contains(locTemplate)) {
					deleteList.add(locTemplate);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#flush()
	 */
	public void flush() throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, Template.class)) {
			Collection deleteList = getDeleteList(t, Template.class);

			final List ids = new LinkedList();

			for (Iterator it = deleteList.iterator(); it.hasNext();) {
				Template template = (Template) it.next();

				ids.add(template.getId());
				ActionLogger.logCmd(ActionLogger.DEL, Template.TYPE_TEMPLATE, template.getId(), null, "Template.delete()");
				Events.trigger(template, null, Events.DELETE);
			}

			// Delete templategroups
			String sql = "SELECT temp.id AS template_id, tg.id as templategroup_id " + "FROM templategroup tg "
					+ "INNER JOIN template temp ON tg.id = temp.templategroup_id " + "WHERE tg.id IN (SELECT t.templategroup_id FROM template t WHERE t.id IN";
			final Set allToDelete = new HashSet();
			final Set notToDelete = new HashSet();

			DBUtils.executeMassStatement(sql, ")", ids, 1, new SQLExecutor() {
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					// be careful, this method could be called more than once
					while (rs.next()) {
						if (!ids.contains(new Integer(rs.getInt("template_id")))) {
							notToDelete.add(new Integer(rs.getInt("templategroup_id")));
						} else {
							allToDelete.add(new Integer(rs.getInt("templategroup_id")));
						}
					}
				}
			});

			allToDelete.removeAll(notToDelete);
			if (!allToDelete.isEmpty()) {
				List toDelete = new LinkedList(allToDelete);

				DBUtils.executeMassStatement("DELETE FROM templategroup WHERE id IN", toDelete, 1, null);
			}

			// Delete references to this template
			final List templatePartIds = new Vector();

			DBUtils.executeStatement("SELECT id FROM part WHERE type_id = 20", new SQLExecutor() {
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						templatePartIds.add(new Integer(rs.getInt("id")));
					}
				}
			});

			if (!templatePartIds.isEmpty()) {
				String idSql = buildIdSql(ids);

				// Clear the cache for the values
				DBUtils.executeMassStatement("SELECT id FROM value WHERE info IN " + idSql + " AND value_text = 't' AND part_id IN", templatePartIds, 1,
						new SQLExecutor() {
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							t.dirtObjectCache(Value.class, rs.getInt("id"));
						}
					}
				});
				// Update the values
				DBUtils.executeMassStatement("UPDATE value SET value_ref=0, info = 0 WHERE info IN " + idSql + " AND value_text='t' AND part_id IN",
						templatePartIds, 1, null);
			}

			// Delete assignments to folders
			flushDelete("DELETE FROM template_folder WHERE template_id IN", Template.class);

			// Delete templates
			flushDelete("DELETE FROM template WHERE id IN", Template.class);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#createObject(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class)
	 */
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		return (T) new EditableFactoryTemplate(handle.createObjectInfo(Template.class, true));
	}

	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		return loadDbObject(clazz, id, info, SELECT_TEMPLATE_SQL, null, null);
	}

	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		String idSql = buildIdSql(ids);
		String[] preloadSql = new String[] { "SELECT template_id AS id, id AS id2 FROM templatetag WHERE template_id IN " + idSql };

		return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_TEMPLATE_SQL + idSql, preloadSql);
	}

	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException {

		String name = rs.getString("name");
		String description = rs.getString("description");
		String source = rs.getString("ml");
		int locked = rs.getInt("locked");
		int lockedBy = rs.getInt("locked_by");
		Integer mlId = new Integer(rs.getInt("ml_id"));
		Integer folderId = new Integer(rs.getInt("folder_id"));
		Integer templategroupId = new Integer(rs.getInt("templategroup_id"));
		Integer channelSetId = new Integer(rs.getInt("channelset_id"));
		Integer channelId = new Integer(rs.getInt("channel_id"));
		boolean master = rs.getBoolean("is_master");

		int creatorId = rs.getInt("creator");
		int editorId = rs.getInt("editor");
		ContentNodeDate cDate = new ContentNodeDate(rs.getInt("cdate"));
		ContentNodeDate eDate = new ContentNodeDate(rs.getInt("edate"));

		List<Integer> tagIds = idLists != null ? idLists[0] : null;

		return (T) new FactoryTemplate(id, info, name, description, source, mlId, locked, lockedBy, tagIds, folderId, templategroupId, creatorId, editorId, cDate, eDate,
				channelSetId, channelId, master, getUdate(rs), getGlobalId(rs));
	}

	/**
	 * Save the given template object
	 * @param template template
	 * @throws NodeException
	 */
	private static void saveTemplateObject(EditableFactoryTemplate template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// make sure that the template has a channelset_id
		template.getChannelSetId();

		boolean isNew = AbstractTemplate.isEmptyId(template.getId());

		// set editor data
		template.editorId = t.getUserId();
		template.eDate = new ContentNodeDate(t.getUnixTimestamp());

		// normalize some data
		template.name = ObjectTransformer.getString(template.name, "");
		template.source = ObjectTransformer.getString(template.source, "");
		template.description = ObjectTransformer.getString(template.description, "");

		if (!StringUtils.isEmpty(template.description)) {
			template.description = template.description.substring(0, Math.min(template.description.length(), Template.MAX_DESCRIPTION_LENGTH));
		}

		if (isNew) {
			// set creator data
			template.creatorId = t.getUserId();
			template.cDate = new ContentNodeDate(t.getUnixTimestamp());

			boolean fixTemplateName = false;

			// when the template name is not set, we set it to empty string (will update it later with the template id)
			if (StringUtils.isEmpty(template.name)) {
				template.name = "";
				fixTemplateName = true;
			}

			template.name = template.name.substring(0, Math.min(template.name.length(), Template.MAX_NAME_LENGTH));

			// generate a new templategroup id
			if (ObjectTransformer.isEmpty(template.templategroupId)) {
				List<Integer> templateGroupId = DBUtils.executeInsert(INSERT_TEMPLATEGROUP_SQL, new Object[] {ObjectTransformer.getString(template.globalTemplateGroupId, "")});

				if (templateGroupId.size() == 1) {
					template.templategroupId = templateGroupId.get(0);
					template.globalTemplateGroupId = null;
				} else {
					throw new NodeException("Error while inserting new templategroup, could not get the insertion id");
				}
			}

			// insert a new record
			template.locked = t.getUnixTimestamp();
			template.lockedBy = t.getUserId();
			List<Integer> keys = DBUtils.executeInsert(INSERT_TEMPLATE_SQL,
					new Object[] {
				template.name, template.description, template.locked, template.lockedBy, template.folderId, template.creatorId, template.cDate.getIntTimestamp(), template.editorId,
				template.eDate.getIntTimestamp(), template.mlId, template.source, template.channelSetId, template.channelId, template.master,
				template.templategroupId, ObjectTransformer.getString(template.getGlobalId(), "")});

			if (keys.size() != 1) {
				throw new NodeException("Error while inserting new template, could not get the insertion id");
			}

			// set the new template id
			template.setId(keys.get(0));
			synchronizeGlobalId(template);

			setTemplateLocked(keys.get(0), template.locked);

			// we need to fix the template name
			if (fixTemplateName) {
				template.name = ObjectTransformer.getString(template.getId(), null);
			} else {
				// or at least make it unique (among all templates that are
				// linked to at least one folder, this template will be linked
				// to)
				fixTemplateName = makeNameUnique(template);
			}

			// save the template (again)
			if (fixTemplateName) {
				DBUtils.executeUpdate(UPDATE_TEMPLATE_SQL,
						new Object[] {
					template.name, template.description, template.folderId, template.editorId, template.eDate.getIntTimestamp(), template.mlId,
					template.source, template.channelSetId, template.channelId, template.master, template.getId()});
			}

			// a master template needs to be linked to a folder, so we will link it to its folder
			if (template.isMaster()) {
				Folder folder = t.getObject(Folder.class, template.folderId);
				List<Folder> folders = template.getFolders();

				if (folder != null && !folders.contains(folder)) {
					folders.add(folder);
				}
			}

			ActionLogger.logCmd(ActionLogger.CREATE, Template.TYPE_TEMPLATE, template.getId(), null, "cmd_template_create-java");

			t.addTransactional(new TransactionalTriggerEvent(Template.class, template.getId(), null, Events.CREATE));
		} else {
			template.name = template.name.substring(0, Math.min(template.name.length(), Template.MAX_NAME_LENGTH));

			// make the name unique
			makeNameUnique(template);

			DBUtils.executeUpdate(UPDATE_TEMPLATE_SQL,
					new Object[] {
				template.name, template.description, template.folderId, template.editorId, template.eDate.getIntTimestamp(), template.mlId,
				template.source, template.channelSetId, template.channelId, template.master, template.getId()});

			ActionLogger.logCmd(ActionLogger.EDIT, Template.TYPE_TEMPLATE, template.getId(), null, "cmd_template_update-java");
			// TODO get the really modified properties and only dirt them
			t.addTransactional(new TransactionalTriggerEvent(Template.class, template.getId(), null, Events.UPDATE));
		}
	}

	/**
	 * Make the template name unique
	 * @param template template
	 * @return true if the template name was changed, false if not
	 * @throws NodeException
	 */
	private static boolean makeNameUnique(EditableFactoryTemplate template) throws NodeException {
		List<Folder> folders = template.getFolders();

		if (folders.size() > 0) {
			StringBuffer sql = new StringBuffer(
					"SELECT name FROM template LEFT JOIN template_folder ON template.id = template_folder.template_id WHERE template_folder.folder_id IN (");

			sql.append(StringUtils.repeat("?", folders.size(), ",")).append(") AND id != ?");

			boolean appendChannelSet = false;

			if (TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
				if (!AbstractTemplate.isEmptyId(template.getChannelSetId())) {
					sql.append(" AND channelset_id != ?");
					appendChannelSet = true;
				}
			}
			sql.append(" AND name = ?");

			Object[] params = new Object[folders.size() + (appendChannelSet ? 2 : 1)];
			int paramCounter = 0;

			for (Folder f : folders) {
				params[paramCounter++] = f.getId();
			}
			params[paramCounter++] = ObjectTransformer.getInt(template.getId(), -1);
			if (appendChannelSet) {
				params[paramCounter++] = template.getChannelSetId();
			}
			String newTemplatename = UniquifyHelper.makeUnique(template.name, Template.MAX_NAME_LENGTH, sql.toString(), SeparatorType.blank, params);

			if (!StringUtils.isEqual(template.name, newTemplatename)) {
				template.name = newTemplatename;
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T getEditableCopy(final T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}

		if (object instanceof Template) {
			// get and store the current transaction
			Transaction currentTransaction = TransactionManager.getCurrentTransaction();

			int lockTime = getTemplateLockTime(object.getId());
			int lockedBy = -1;
			if (lockTime == 0) {
				// start a new transaction
				Transaction tmpTransaction = TransactionManager.getTransaction(currentTransaction);

				TransactionManager.setCurrentTransaction(tmpTransaction);
				PreparedStatement pst = null;
				ResultSet rs = null;

				boolean locked = false;
				boolean setLocked = false;
				// this will be set to true, if the user already owned the lock (and the lock was not too old)
				boolean alreadyLocked = false;

				// check the locks and eventually lock the template (do this in another transaction)
				try {
					pst = tmpTransaction.prepareStatement("SELECT locked, locked_by FROM template WHERE id = ? FOR UPDATE");
					pst.setObject(1, object.getId());
					rs = pst.executeQuery();

					if (rs.next()) {
						lockTime = rs.getInt("locked");
						lockedBy = rs.getInt("locked_by");

						int lockTimeout = ObjectTransformer.getInt(
								tmpTransaction.getNodeConfig().getDefaultPreferences().getProperty("lock_time"), 600);

						if (lockTime != 0 && lockedBy > 0 && (currentTransaction.getUnixTimestamp() - lockTime) < lockTimeout
								&& (currentTransaction.getUserId() != lockedBy)) {
							locked = true;
						} else if (lockedBy == currentTransaction.getUserId() && lockTime != 0 && (currentTransaction.getUnixTimestamp() - lockTime) < lockTimeout) {
							alreadyLocked = true;
						}
					}

					tmpTransaction.closeResultSet(rs);
					rs = null;
					tmpTransaction.closeStatement(pst);
					pst = null;

					if (!locked) {
						// now lock the template
						lockTime = currentTransaction.getUnixTimestamp();
						lockedBy = currentTransaction.getUserId();
						pst = tmpTransaction.prepareUpdateStatement("UPDATE template SET locked = ?, locked_by = ? WHERE id = ?");
						pst.setInt(1, lockTime);
						pst.setInt(2, lockedBy);
						pst.setObject(3, object.getId());

						pst.executeUpdate();

						// set the template to be locked
						setLocked = true;
					} else {
						throw new ReadOnlyException(
								"Could not lock template for user {" + currentTransaction.getUserId() + "}, since it is locked for user {" + lockedBy + "} since {"
								+ lockTime + "}", "template.readonly.locked", String.format("%s (%d)", I18NHelper.getName(object), object.getId()));
					}
				} catch (SQLException e) {
					throw new NodeException("Error while locking template for editing for user {" + currentTransaction.getUserId() + "}", e);
				} finally {
					tmpTransaction.closeResultSet(rs);
					tmpTransaction.closeStatement(pst);

					// commit this transaction
					tmpTransaction.commit(true);

					// restore the original transaction
					TransactionManager.setCurrentTransaction(currentTransaction);

					// now set the template to be locked, it's important to do this after
					// the initial transaction has been restored, because this info is stored
					// in the transaction (as attributes)
					if (setLocked) {
						setTemplateLocked(object.getId(), lockTime);

						if (!alreadyLocked) {
							// we also write the logcmd entry with the original transation to preserve timestamps
							ActionLogger.logCmd(ActionLogger.LOCK, Template.TYPE_TEMPLATE, object.getId(), 0, "lock-java");
						}
					}
				}
			} else {
				currentTransaction.dirtObjectCache(Template.class, object.getId());
			}

			EditableFactoryTemplate editableTemplate = new EditableFactoryTemplate((FactoryTemplate) object, info, false);
			editableTemplate.locked = lockTime;
			editableTemplate.lockedBy = lockedBy;
			return (T) editableTemplate;
		} else {
			throw new NodeException("TemplateFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Get the map of locked template ids -&gt; lock time
	 * @return map of template ids -&gt; lock time
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	private static Map<Integer, Integer> getLockedTemplateIds() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Map<String, Object> attributes = t.getAttributes();

		Object data = attributes.get(LOCKED_TEMPLATES_IN_TRX);
		Map<Integer, Integer> lockedTemplates = null;

		if (data instanceof Map) {
			lockedTemplates = (Map<Integer, Integer>) data;
		} else {
			lockedTemplates = new HashMap<Integer, Integer>();
			attributes.put(LOCKED_TEMPLATES_IN_TRX, lockedTemplates);
		}

		return lockedTemplates;
	}

	/**
	 * Set the given template id to be locked in this transaction
	 * @param templateId template id
	 * @param lockTime lock time
	 * @throws NodeException
	 */
	public static void setTemplateLocked(int templateId, int lockTime) throws NodeException {
		getLockedTemplateIds().put(templateId, lockTime);
	}

	/**
	 * Check whether the given template id is locked in this transaction
	 * @param templateId template id
	 * @return true if it is locked, false if not
	 * @throws NodeException
	 */
	public static boolean isTemplateLocked(int templateId) throws NodeException {
		return getLockedTemplateIds().containsKey(templateId);
	}

	/**
	 * Get the template lock time from the transaction or 0
	 * @param templateId template id
	 * @return lock time (0 if not stored in transaction)
	 * @throws NodeException
	 */
	public static int getTemplateLockTime(int templateId) throws NodeException {
		return getLockedTemplateIds().getOrDefault(templateId, 0);
	}

	/**
	 * Unset the given template id as locked
	 * @param templateId template id
	 * @throws NodeException
	 */
	public static void unsetTemplateLocked(int templateId) throws NodeException {
		getLockedTemplateIds().remove(templateId);
	}

	/**
	 * Unlock the template with given ID, if locked by the user of the current transaction
	 * @param templateId template ID
	 * @throws NodeException
	 */
	public static void unlock(int templateId, int userId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		try {
			DBUtils.executeUpdate("UPDATE template SET locked = 0, locked_by = 0 WHERE id = ? AND locked_by = ?", new Object[] { templateId, userId });
			ActionLogger.logCmd(ActionLogger.UNLOCK, Template.TYPE_TEMPLATE, templateId, 0, "unlock-java");
		} finally {
			// dirt the cache
			t.dirtObjectCache(Template.class, templateId);

			// set the template to be unlocked
			unsetTemplateLocked(templateId);
		}
	}

	/**
	 * Link the template to the given set of folders (if not already linked)
	 * @param template template
	 * @param folders set of folders
	 * @throws NodeException
	 */
	public static void link(Template template, Set<Folder> folders) throws NodeException {
		Set<Folder> toAdd = new HashSet<>(Flowable.fromIterable(folders).map(Folder::getMaster).toList().blockingGet());
		toAdd.removeAll(template.getFolders());

		if (!toAdd.isEmpty()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			int templateId = template.getMaster().getId();
			List<Object[]> args = Flowable.fromIterable(toAdd)
					.map(folder -> new Object[] { templateId, folder.getId() }).toList().blockingGet();

			DBUtils.executeBatchInsert("INSERT INTO template_folder (template_id, folder_id) VALUES (?, ?)", args);

			// assign to nodes
			List<Integer> nodeIds = Flowable.fromIterable(toAdd).map(folder -> folder.getNode().getMaster().getId()).toList().blockingGet();
			if (nodeIds.size() > 0) {
				args = Flowable.fromIterable(nodeIds).map(id -> new Object[] {templateId, id}).toList().blockingGet();
				DBUtils.executeBatchInsert("INSERT IGNORE INTO template_node (template_id, node_id) VALUES (?, ?)", args);

				Flowable.fromIterable(nodeIds).blockingForEach(nodeId -> t.dirtObjectCache(Node.class, nodeId));
			}

			// dirt caches for all folders the template is linked to
			Flowable.fromIterable(toAdd).map(folder -> folder.getId())
					.blockingForEach(id -> t.dirtObjectCache(Folder.class, id));
			t.dirtObjectCache(Template.class, templateId);
		}
	}

	/**
	 * Unlink the template from the given set of folders
	 * @param template template
	 * @param folders set of folders
	 * @throws IllegalUsageException if the template would not be linked to any other folder after unlinking
	 * @throws NodeException
	 */
	public static void unlink(Template template, Set<Folder> folders) throws IllegalUsageException, NodeException {
		folders = new HashSet<>(Flowable.fromIterable(folders).map(Folder::getMaster).toList().blockingGet());
		Set<Folder> remaining = new HashSet<>(template.getFolders());
		remaining.removeAll(folders);

		if (remaining.isEmpty()) {
			throw new IllegalUsageException(
					"Could not unlink " + template.toString() + ": template would not be linked to any folder",
					"template.unlink.notlinked", template.toString());
		}

		if (!folders.isEmpty()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			int templateId = template.getMaster().getId();
			List<Integer> folderIds = folders.stream().map(Folder::getId).collect(Collectors.toList());
			DBUtils.executeMassStatement("DELETE FROM template_folder WHERE template_id = ? AND folder_id IN", folderIds, 2, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, templateId);
				}
			});

			// dirt caches for all folders the template is linked to
			Flowable.fromIterable(folders).map(Folder::getId).blockingForEach(id -> t.dirtObjectCache(Folder.class, id));
			t.dirtObjectCache(Template.class, templateId);
		}
	}
}
