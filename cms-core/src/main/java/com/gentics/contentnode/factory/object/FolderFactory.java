/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: FolderFactory.java,v 1.44.2.1.2.11.2.8 2011-03-18 17:18:52 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import static com.gentics.contentnode.rest.util.MiscUtils.when;
import static com.gentics.contentnode.rest.util.PropertySubstitutionUtil.substituteSingleProperty;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.LightWeightPageList;
import com.gentics.contentnode.factory.MultiChannellingFallbackList;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.PublishData;
import com.gentics.contentnode.factory.RemovePermsTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.TrxAttribute;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FileOnlineStatus.FileListForNode;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.object.AbstractFolder;
import com.gentics.contentnode.object.AbstractNode;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.OpResult.Status;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;

/**
 * An objectfactory to create {@link Folder} and {@link Node} objects, based on the {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = Folder.class, name = "folder"),
	@DBTable(clazz = Node.class, name = "node") })
public class FolderFactory extends AbstractFactory {
	/**
	 * SQL Statement to select a single node
	 */
	protected final static String SELECT_NODE_SQL = createSelectStatement("node");

	/**
	 * SQL Statement to batchload nodes
	 */
	protected final static String BATCHLOAD_NODE_SQL = createBatchLoadStatement("node");

	/**
	 * SQL Statement to select a single folder
	 */
	protected final static String SELECT_FOLDER_SQL = createSelectStatement("folder");

	/**
	 * SQL Statement to batchload folders
	 */
	protected final static String BATCHLOAD_FOLDER_SQL = createBatchLoadStatement("folder");

	/**
	 * SQL Statement to update the delete flag of a folder
	 */
	protected final static String UPDATE_FOLDER_DELETEFLAG = "UPDATE folder SET deleted = ?, deletedby = ? WHERE id = ?";

	/**
	 * SQL Statement to batchload i18n data of folders
	 */
	protected final static String BATCHLOAD_I18N_SQL = "SELECT * FROM folder_i18n WHERE folder_id IN";

	/**
	 * Name of the dummy attribute that will be dirted for pages, if they need
	 * to be updated in the publish table, because their path changed
	 */
	public final static String DUMMY_DIRT_ATTRIBUTE = "gtx.table.publish";

	/**
	 * Name of the Transaction attribute to omit verification of pubDirSegment setting in channels
	 */
	public final static String OMIT_PUB_DIR_SEGMENT_VERIFY = "gtx.omit.pubDirSegment.verify";

	/**
	 * Name of the Transaction attribute to omit verification of cr assignment
	 */
	public final static String OMIT_CR_VERIFY = "gtx.omit.cr.verify";

	/**
	 * Wastebin Logger
	 */
	private final static NodeLogger wastebinLogger = NodeLogger.getNodeLogger("wastebin");

	/**
	 * Function that gets all names of the folder (generic name and optional translated names)
	 */
	protected final static Function<Folder, Set<String>> NAME_FUNCTION = folder -> {
		Set<String> names = new HashSet<>();
		names.add(folder.getName());
		names.addAll(folder.getNameI18n().values());
		return names;
	};

	/**
	 * Function that gets all pub_dirs of the folder (generic pub_dir and optional translated pub_dirs)
	 */
	protected final static Function<Folder, Set<String>> PUB_DIR_FUNCTION = folder -> {
		Set<String> pubDirs = new HashSet<>();
		pubDirs.add(folder.getPublishDir());
		pubDirs.addAll(folder.getPublishDirI18n().values());
		return pubDirs;
	};

	/**
	 * Loader for {@link FolderService}s
	 */
	protected final static ServiceLoaderUtil<FolderService> folderFactoryServiceLoader = ServiceLoaderUtil
			.load(FolderService.class);

	/**
	 * Loader for {@link NodeService}s
	 */
	protected final static ServiceLoaderUtil<NodeService> nodeFactoryServiceLoader = ServiceLoaderUtil
			.load(NodeService.class);

	static {
		// register the factory classes
		try {
			registerFactoryClass(C.Tables.NODE, Node.TYPE_NODE, true, FactoryNode.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	private static class FactoryFolder extends AbstractFolder implements DisinheritableInternal<Folder> {

		/**
		 * Serial version uid
		 */
		private static final long serialVersionUID = -480298094314627141L;

		protected final static String OBJECTTAGS = "objecttags";

		/**
		 * Used for loadFilesAndImages()
		 */
		protected static final int LOAD_FILES = 0;
		protected static final int LOAD_IMAGES = 1;
		protected static final int LOAD_FILESANDIMAGES = 2;

		protected String name;
		protected String description;
		protected Integer motherId;
		protected Integer nodeId;
		protected String pubDir;
		protected List<Integer> objectTagIds;

		/**
		 * Child folder ids
		 */
		protected PerNodeStore childIds = new PerNodeStore();

		/**
		 * page ids
		 */
		protected PerNodeStore pageIds = new PerNodeStore();

		/**
		 * This map contains the lists of template ids which are currently linked to this folder for a specific channel id.
		 * Keys are the channel ids, values are the collections of template ids
		 */
		protected Map<Integer, Collection<Integer>> templateIds = Collections.synchronizedMap(new HashMap<Integer, Collection<Integer>>());

		protected PerNodeStore fileIds = new PerNodeStore();
		protected PerNodeStore imageIds = new PerNodeStore();

		protected PerNodeStore formIds = new PerNodeStore();

		protected ContentNodeDate cDate = new ContentNodeDate(0);
		protected ContentNodeDate eDate = new ContentNodeDate(0);
		protected Integer editorId = 0;
		protected Integer creatorId = 0;

		/**
		 * Id of the master folder if this is the root folder of a channel node (otherwise empty)
		 */
		protected Integer masterId;

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
		protected Map<Wastebin, Map<Integer, Integer>> channelSet;

		/**
		 * whether multichannelling inheritance is excluded for this folder
		 */
		protected boolean excluded = false;

		/**
		 * Indicates whether this folder is disinherited by default in new channels (default: false).
		 */
		protected boolean disinheritDefault = false;

		/**
		 * ids of channels that are disinherited for this folder
		 */
		protected Set<Integer> disinheritedChannelIds = null;

		/**
		 * Timestamp of deletion, if the object was deleted (and pub into the wastebin), 0 if object is not deleted
		 */
		protected int deleted;

		/**
		 * ID of the user who put the object into the wastebin, 0 if not deleted
		 */
		protected int deletedBy;

		/**
		 * Map of translated folder names
		 */
		protected I18nMap nameI18n = new I18nMap();

		/**
		 * Map of translated folder descriptions
		 */
		protected I18nMap descriptionI18n = new I18nMap();

		/**
		 * Map of translated folder publish directories (or publish directory segments)
		 */
		protected I18nMap publishDirI18n = new I18nMap();

		public FactoryFolder(Integer id, NodeObjectInfo info, String name, String description,
				Integer motherId, Integer nodeId, String pubDir,
				List<Integer> objectTagIds, ContentNodeDate cDate, ContentNodeDate eDate,
				Integer creatorId, Integer editorId, Integer masterId,
				Integer channelSetId, Integer channelId, boolean master, boolean excluded,
				boolean disinheritDefault, int deleted, int deletedBy, int udate, GlobalId globalId) {
			super(id, info);
			this.name = name;
			this.description = description;
			this.motherId = motherId;
			this.nodeId = nodeId;
			this.pubDir = pubDir;
			this.objectTagIds = objectTagIds != null ? new Vector<Integer>(objectTagIds) : null;
			this.cDate = cDate;
			this.eDate = eDate;
			this.creatorId = creatorId;
			this.editorId = editorId;
			this.masterId = masterId;
			this.channelSetId = ObjectTransformer.getInt(channelSetId, 0);
			this.channelId = ObjectTransformer.getInt(channelId, 0);
			this.master = master;
			this.excluded = excluded;
			this.disinheritDefault = disinheritDefault;
			this.deleted = deleted;
			this.deletedBy = deletedBy;
			this.udate = udate;
			this.globalId = globalId;
		}

		/**
		 * Create a new empty instance of a file
		 * @param info info about the instance
		 * @throws NodeException
		 */
		protected FactoryFolder(NodeObjectInfo info) throws NodeException {
			super(null, info);
			description = "";
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

		public Folder getMother() throws NodeException {
			Folder mother = (Folder) TransactionManager.getCurrentTransaction().getObject(Folder.class, motherId);

			// check for consistent data
			if (motherId != null && motherId.intValue() != 0) {
				assertNodeObjectNotNull(mother, motherId, "Mother");
			}
			return mother;
		}

		public Folder getChannelMaster() throws NodeException {
			Folder master = (Folder) TransactionManager.getCurrentTransaction().getObject(Folder.class, masterId, -1, false);

			// check for consistent data
			if (!isEmptyId(masterId)) {
				assertNodeObjectNotNull(master, masterId, "Master");
			}
			return master;
		}

		@Override
		public boolean isChannelRoot() throws NodeException {
			// the folder is the channel root, if it has no mother, but a masterId
			return isEmptyId(motherId) && !isEmptyId(masterId);
		}

		@Override
		public boolean isRoot() throws NodeException {
			return isEmptyId(motherId);
		}

		public Node getNode() throws NodeException {
			Node node = TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId);

			// check for consistent data
			assertNodeObjectNotNull(node, nodeId, "Node");
			return node;
		}

		public String getPublishDir() {
			return pubDir;
		}

		public List<Folder> getChildFolders() throws NodeException {
			return loadChildFolders();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#getTemplatesToDelete(java.util.List)
		 */
		protected Collection<Template> getTemplatesToDelete(Collection<Template> templates) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FolderFactory folderFactory = (FolderFactory) t.getObjectFactory(Folder.class);

			return folderFactory.getTemplatesToDelete(this, templates);
		}

		protected Collection<Template> getTemplatesToDelete(Collection<Template> templates, boolean addFolderToDeleteList) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FolderFactory folderFactory = (FolderFactory) t.getObjectFactory(Folder.class);

			return folderFactory.getTemplatesToDelete(this, templates, addFolderToDeleteList);
		}

		public List<Template> getTemplates(TemplateSearch search) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObjects(Template.class, getTemplateIds(search));
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#getPages(java.lang.String)
		 */
		public List<Page> getPages(PageSearch search) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			// get the page ids for the given search
			Collection<Integer> loadedPageIds = getPageIds(search);

			// load the page objects
			List<Page> loadedPages = t.getObjects(Page.class, loadedPageIds);
			if (!(loadedPages instanceof LightWeightPageList)) {
				loadedPages = new ArrayList<>(loadedPages);
			}

			// if a search was done in a multichannelled environment, the result
			// probably is not correct:
			// If e.g. searching in a channel and the master page matches, but
			// the localized page does not, we would get the id of the master
			// page in loadedPageIds and
			// getting the objects would do the multichannelling fallback to the
			// localized page.
			// This case can be detected, if a page in loadedPages has an id,
			// which is not present in loadedPageIds, because we trust that for
			// the pages in loadedPageIds, the multichannelling fallback has
			// already been done.
			if (search != null && !search.isEmpty()) {
				for (Iterator<Page> iPage = loadedPages.iterator(); iPage.hasNext();) {
					Page page = iPage.next();

					// Prefill the secondlevel cache for page online since we know that those pages are online.
					// The search only returned pages that were marked as being online.
					when(search.getOnline(), () -> t.putIntoLevel2Cache(page, PageFactory.ONLINE_CACHE_KEY, true),
							() -> t.putIntoLevel2Cache(page, PageFactory.ONLINE_CACHE_KEY, false));
					if (!loadedPageIds.contains(page.getId())) {
						iPage.remove();
					} else if (search.isRecursive() && !PermHandler.ObjectPermission.view.checkObject(page)) {
						// when a recursive search was done, we need to remove pages without permission.
						// this has to be done page by page, because the permission might depend on the language (role)
						iPage.remove();
					}
				}
			}

			return loadedPages;
		}

		@Override
		public List<Form> getForms(FormSearch search) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			try (WastebinFilter wbf = WastebinFilter.get(search != null && search.getWastebin() != Wastebin.EXCLUDE,
					getOwningNode())) {
				return t.getObjects(Form.class, getFormIds(search));
			}
		}

		public Map<String, ObjectTag> getObjectTags() throws NodeException {
			// use level2 cache
			Transaction t = TransactionManager.getCurrentTransaction();
			Map<String, ObjectTag> objectTags = (Map<String, ObjectTag>) t.getFromLevel2Cache(this, OBJECTTAGS);

			if (objectTags == null) {
				objectTags = loadObjectTags();
				t.putIntoLevel2Cache(this, OBJECTTAGS, objectTags);
			}
			return objectTags;
		}

		/**
		 * Performs the unlink of a template from this folder
		 * @param templateId The id of the template to unlink
		 */
		protected void performUnlinkTemplate(Integer templateId) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FolderFactory folderFactory = (FolderFactory) t.getObjectFactory(Folder.class);

			folderFactory.unlinkTemplate(this, templateId);
		}

		/**
		 * Performs the delete of a folder
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FolderFactory folderFactory = (FolderFactory) t.getObjectFactory(Folder.class);

			folderFactory.deleteFolder(this);
		}

		@Override
		public OpResult move(Folder target, int targetChannelId) throws ReadOnlyException, NodeException {
			failReadOnly();
			return null;
		}

		@Override
		protected void putIntoWastebin() throws NodeException {
			if (isDeleted()) {
				return;
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			Set<Folder> toPutIntoWastebin = new HashSet<Folder>();
			toPutIntoWastebin.add(this);
			if (isMaster()) {
				toPutIntoWastebin.addAll(t.getObjects(Folder.class, getChannelSet().values(), false, false));
			}

			for (Folder f : toPutIntoWastebin) {
				// Mark folder as being deleted
				DBUtils.executeUpdate(UPDATE_FOLDER_DELETEFLAG,
						new Object[] { t.getUnixTimestamp(), t.getUserId(), f.getId() });

				ActionLogger.logCmd(ActionLogger.WASTEBIN, Folder.TYPE_FOLDER, f.getId(), null, "Folder.delete()");
				t.dirtObjectCache(Folder.class, f.getId(), true);

				try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
					f = f.reload();
				}

				t.addTransactional(TransactionalTriggerEvent.deleteIntoWastebin(getNode(), f));
			}

			// if the folder is a localized copy, it was hiding other folders (which are now "created")
			if (!isMaster()) {
				unhideFormerHiddenObjects(Folder.TYPE_FOLDER, getId(), getChannel(), getChannelSet());
			}
		}

		@Override
		public void restore() throws NodeException {
			if (!isDeleted()) {
				return;
			}

			// we need to restore the mother folder as well (if this is not root
			// folder and the mother folder is also in the wastebin)
			try (WastebinFilter filter = new WastebinFilter(Wastebin.INCLUDE)) {
				if (!isRoot()) {
					getMother().restore();
				}
			}

			// if this is a localized copy, we need to restore its master first
			// (if the master is in the wastebin too)
			if (!isMaster()) {
				getMaster().restore();
			}

			// restore the object
			Transaction t = TransactionManager.getCurrentTransaction();
			DBUtils.executeUpdate(UPDATE_FOLDER_DELETEFLAG, new Object[] {0, 0, getId()});
			deleted = 0;
			deletedBy = 0;
			ActionLogger.logCmd(ActionLogger.WASTEBINRESTORE, Folder.TYPE_FOLDER, getId(), null, "Folder.restore()");
			channelSet = null;
			t.dirtObjectCache(Folder.class, getId(), true);
			t.addTransactional(new TransactionalTriggerEvent(this, null, Events.CREATE));

			if (!isMaster()) {
				hideFormerInheritedObjects(Folder.TYPE_FOLDER, getId(), getChannel(), getChannelSet());
			}

			// make the name unique
			FactoryFolder editableFolder = t.getObject(this, true);
			DisinheritUtils.makeUniqueDisinheritable(editableFolder, SeparatorType.blank, Folder.MAX_NAME_LENGTH);
			editableFolder.save();
			editableFolder.unlock();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#getFeature(java.lang.String)
		 */
		protected boolean getFeature(String feature) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FolderFactory folderFactory = (FolderFactory) t.getObjectFactory(Folder.class);

			return folderFactory.getConfiguration().getDefaultPreferences().getFeature(feature);
		}

		private synchronized void loadObjectTagIds() throws NodeException {
			if (objectTagIds == null && getId() != null) {
				objectTagIds = DBUtils.select("SELECT t.id as id FROM objtag t"
							+ " LEFT JOIN construct c ON c.id = t.construct_id"
							+ " WHERE t.obj_id = ? AND t.obj_type = ? AND c.id IS NOT NULL", ps -> {
								ps.setInt(1, getId());
								ps.setInt(2, Folder.TYPE_FOLDER);
							}, DBUtils.IDLIST);
			}
		}

		private Map<String, ObjectTag> loadObjectTags() throws NodeException {
			Map<String, ObjectTag> objectTags = new HashMap<String, ObjectTag>();
			Transaction t = TransactionManager.getCurrentTransaction();

			loadObjectTagIds();

			Collection<ObjectTag> tags = t.getObjects(ObjectTag.class, objectTagIds, getObjectInfo().isEditable());

			Node owningNode = getOwningNode();

			for (ObjectTag tag : tags) {
				ObjectTagDefinition def = tag.getDefinition();
				if (def != null && !def.isVisibleIn(owningNode)) {
					continue;
				}

				String name = tag.getName();

				if (name.startsWith("object.")) {
					name = name.substring(7);
				}

				objectTags.put(name, tag);
			}

			// when the folder is editable, get all objecttags which are assigned to the folder's node
			if (getObjectInfo().isEditable()) {
				List<ObjectTagDefinition> folderDefs = TagFactory.load(Folder.TYPE_FOLDER, Optional.ofNullable(owningNode));

				for (ObjectTagDefinition def : folderDefs) {
					// get the name (without object. prefix)
					String defName = def.getObjectTag().getName();

					if (defName.startsWith("object.")) {
						defName = defName.substring(7);
					}

					// if no objtag of that name exists for the folder,
					// generate a copy and add it to the map of objecttags
					if (!objectTags.containsKey(defName)) {
						ObjectTag newObjectTag = (ObjectTag) def.getObjectTag().copy();

						newObjectTag.setNodeObject(this);
						newObjectTag.setEnabled(false);
						objectTags.put(defName, newObjectTag);
					}
				}

				// migrate object tags to new constructs, if they were changed
				for (ObjectTag tag : objectTags.values()) {
					tag.migrateToDefinedConstruct();
				}
			}

			return objectTags;
		}

		/**
		 * Get the ids of the child folders. If already fetched from the db,
		 * return the cached list. Otherwise get from the db. Multichannelling
		 * is respected.
		 *
		 * @return collection of child folder ids
		 * @throws NodeException
		 */
		private Collection<Integer> loadChildFolderIds() throws NodeException {
			return childIds.get(getNode().getId(), TransactionManager.getCurrentTransaction().getWastebinFilter(), (nodeId, wastebin) -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				Collection<Integer> ids = new ArrayList<>();

				// check whether multichannelling is supported and this node is a
				// channel
				boolean multiChannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);
				int masterId = getMaster().getId();

				if (multiChannelling) {
					// get the master nodes
					List<Node> masterNodes = getNode().getMasterNodes();

					// prepare the objectlist with multichannelling fallback
					MultiChannellingFallbackList objectList = new MultiChannellingFallbackList(getNode());

					// prepare the sql statement
					StringBuilder sql = new StringBuilder("SELECT id, channelset_id, folder.channel_id, mc_exclude, folder_disinherit.channel_id disinherited_node FROM folder ")
						.append(" LEFT JOIN folder_disinherit on folder.id = folder_disinherit.folder_id ")
						.append("WHERE mother = ? AND folder.channel_id IN (");

					sql.append(StringUtils.repeat("?", masterNodes.size() + 2, ","));
					sql.append(")");

					sql.append(wastebin.filterClause("folder"));

					DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							int pCounter = 1;

							stmt.setInt(pCounter++, masterId);

							// add the channel ids
							stmt.setObject(pCounter++, 0);
							for (Node node : masterNodes) {
								stmt.setObject(pCounter++, node.getId());
							}
							stmt.setObject(pCounter++, nodeId);
						}

						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								objectList.addObject(rs.getInt("id"), rs.getInt("channelset_id"), rs.getInt("channel_id"), rs.getBoolean("mc_exclude"),
										rs.getInt("disinherited_node"));
							}
						}
					});

					ids.addAll(objectList.getObjectIds());
				} else {
					StringBuilder sql = new StringBuilder("SELECT id FROM folder WHERE mother = ?");
					sql.append(wastebin.filterClause("folder"));

					DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, masterId);
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								ids.add(rs.getInt("id"));
							}
						}
					});
				}
				return ids;
			});
		}

		private List<Folder> loadChildFolders() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObjects(Folder.class, loadChildFolderIds());
		}

		/**
		 * Get the template IDs linked to this folder
		 * @param search optional search
		 * @return template IDs
		 * @throws NodeException
		 */
		private Collection<Integer> getTemplateIds(TemplateSearch search) throws NodeException {
			if (search == null) {
				search = TemplateSearch.create();
			}

			if (!search.isEmpty()) {
				return loadTemplateIds(search);
			} else {
				Integer nodeId = getNode().getId();
				if (templateIds.get(nodeId) == null) {
					synchronized (templateIds) {
						if (templateIds.get(nodeId) == null) {
							templateIds.put(nodeId, loadTemplateIds(search));
						}
					}
				}

				return templateIds.get(nodeId);
			}

		}

		/**
		 * Returns the template ids which are linked to this folder (eventually specific to the current channel).
		 * Ids are stored within the instance, so multiple calls will only access the database once
		 * @param search search string (may be null)
		 * @throws NodeException
		 */
		private Collection<Integer> loadTemplateIds(TemplateSearch search) throws NodeException {
			if (search == null) {
				search = TemplateSearch.create();
			}
			boolean isSearch = !search.isEmpty();
			boolean isStringSearch = !ObjectTransformer.isEmpty(search.getSearchString());
			Collection<Integer> ids = new ArrayList<Integer>();

			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement stmt = null;
			ResultSet rs = null;

			// check whether multichannelling is supported and this node is a
			// channel
			boolean multiChannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);

			try {
				if (multiChannelling) {
					boolean nodeIsChannel = getNode().isChannel();

					// first check the special case when searching for inherited pages in master nodes
					if (search.getInherited() != null && search.getInherited().booleanValue() && !nodeIsChannel) {
						return ids;
					}

					// get the master nodes
					List<Node> masterNodes = getNode().getMasterNodes();

					// prepare the objectlist with multichannelling fallback
					MultiChannellingFallbackList objectList = new MultiChannellingFallbackList(getNode());

					// filter for channels, if only inherited or localized/local objects shall be fetched
					if (search.getInherited() != null) {
						if (search.getInherited().booleanValue()) {
							// filter for only inherited pages
							for (Node node : masterNodes) {
								objectList.addFilteredChannelId(node.getId());
							}
							if (nodeIsChannel) {
								// if the node is a channel, also pages from the master node (with channelId = 0) are allowed
								objectList.addFilteredChannelId(0);
							}
						} else {
							// filter for only local/localized pages
							objectList.addFilteredChannelId(getNode().getId());
							if (!nodeIsChannel) {
								// if the node is a master, also add 0 to the list of filtered channel ids
								objectList.addFilteredChannelId(0);
							}
						}
					}

					// this statement selects all templates linked to the folder, which have no channelset
					StringBuffer sql = new StringBuffer("SELECT template.id, template.channelset_id, template.channel_id FROM template_folder ");

					sql.append("LEFT JOIN template ON template_folder.template_id = template.id ");
					sql.append("WHERE template_folder.folder_id = ? AND template.channelset_id = 0");

					// do a search
					if (isStringSearch) {
						sql.append(" AND (template.id = ?").append(" OR LOWER(template.name) LIKE ?").append(" OR LOWER(template.description) LIKE ?)");
					}

					List<SystemUser> editors = search.getEditors();

					if (editors != null) {
						if (ObjectTransformer.isEmpty(editors)) {
							return ids;
						}
						sql.append(" AND template.editor IN (");
						sql.append(StringUtils.repeat("?", editors.size(), ","));
						sql.append(")");
					}
					List<SystemUser> creators = search.getCreators();

					if (creators != null) {
						if (ObjectTransformer.isEmpty(creators)) {
							return ids;
						}
						sql.append(" AND template.creator IN (");
						sql.append(StringUtils.repeat("?", creators.size(), ","));
						sql.append(")");
					}

					if (search.getEditedBefore() > 0) {
						sql.append(" AND template.edate <= ?");
					}
					if (search.getEditedSince() > 0) {
						sql.append(" AND template.edate >= ?");
					}
					if (search.getCreatedBefore() > 0) {
						sql.append(" AND template.cdate <= ?");
					}
					if (search.getCreatedSince() > 0) {
						sql.append(" AND template.cdate >= ?");
					}

					stmt = t.prepareStatement(sql.toString());
					int pCounter = 1;

					stmt.setObject(pCounter++, getMaster().getId());

					if (isStringSearch) {
						String pattern_nolike = search.getSearchString();
						String pattern_like = "%" + search.getSearchString() + "%";

						// add search term three times (for name, description and id)
						stmt.setObject(pCounter++, pattern_nolike);
						stmt.setObject(pCounter++, pattern_like);
						stmt.setObject(pCounter++, pattern_like);
					}

					if (editors != null) {
						for (SystemUser editor : editors) {
							stmt.setObject(pCounter++, editor.getId());
						}
					}
					if (creators != null) {
						for (SystemUser creator : creators) {
							stmt.setObject(pCounter++, creator.getId());
						}
					}

					if (search.getEditedBefore() > 0) {
						stmt.setObject(pCounter++, search.getEditedBefore());
					}
					if (search.getEditedSince() > 0) {
						stmt.setObject(pCounter++, search.getEditedSince());
					}
					if (search.getCreatedBefore() > 0) {
						stmt.setObject(pCounter++, search.getCreatedBefore());
					}
					if (search.getCreatedSince() > 0) {
						stmt.setObject(pCounter++, search.getCreatedSince());
					}

					rs = stmt.executeQuery();

					// get all the selected templates and add to the multichannel fallback list
					while (rs.next()) {
						objectList.addObject(rs.getInt("id"), rs.getInt("channelset_id"), rs.getInt("channel_id"), false, (Integer)null);
					}

					// close resultset and statement
					t.closeResultSet(rs);
					t.closeStatement(stmt);

					// this statement selects all channelset variants of templates which are linked to the folder
					sql = new StringBuffer("SELECT c.id, c.channelset_id, c.channel_id FROM template_folder ");
					sql.append("LEFT JOIN template ON template_folder.template_id = template.id ");
					sql.append("LEFT JOIN template c ON template.channelset_id = c.channelset_id AND template.channelset_id != 0 ");
					sql.append("WHERE template_folder.folder_id = ? AND c.id IS NOT NULL");

					// do a search
					if (isStringSearch) {
						sql.append(" AND (c.id = ?").append(" OR LOWER(c.name) LIKE ?").append(" OR LOWER(c.description) LIKE ?)");
					}

					if (editors != null) {
						if (ObjectTransformer.isEmpty(editors)) {
							return ids;
						}
						sql.append(" AND template.editor IN (");
						sql.append(StringUtils.repeat("?", editors.size(), ","));
						sql.append(")");
					}
					if (creators != null) {
						if (ObjectTransformer.isEmpty(creators)) {
							return ids;
						}
						sql.append(" AND template.creator IN (");
						sql.append(StringUtils.repeat("?", creators.size(), ","));
						sql.append(")");
					}

					if (search.getEditedBefore() > 0) {
						sql.append(" AND template.edate <= ?");
					}
					if (search.getEditedSince() > 0) {
						sql.append(" AND template.edate >= ?");
					}
					if (search.getCreatedBefore() > 0) {
						sql.append(" AND template.cdate <= ?");
					}
					if (search.getCreatedSince() > 0) {
						sql.append(" AND template.cdate >= ?");
					}

					sql.append(" GROUP BY c.id");

					stmt = t.prepareStatement(sql.toString());
					pCounter = 1;

					stmt.setObject(pCounter++, getMaster().getId());

					if (isStringSearch) {
						String pattern_nolike = search.getSearchString();
						String pattern_like = "%" + search.getSearchString() + "%";

						// add search term three times (for name, description and id)
						stmt.setObject(pCounter++, pattern_nolike);
						stmt.setObject(pCounter++, pattern_like);
						stmt.setObject(pCounter++, pattern_like);
					}

					if (editors != null) {
						for (SystemUser editor : editors) {
							stmt.setObject(pCounter++, editor.getId());
						}
					}
					if (creators != null) {
						for (SystemUser creator : creators) {
							stmt.setObject(pCounter++, creator.getId());
						}
					}

					if (search.getEditedBefore() > 0) {
						stmt.setObject(pCounter++, search.getEditedBefore());
					}
					if (search.getEditedSince() > 0) {
						stmt.setObject(pCounter++, search.getEditedSince());
					}
					if (search.getCreatedBefore() > 0) {
						stmt.setObject(pCounter++, search.getCreatedBefore());
					}
					if (search.getCreatedSince() > 0) {
						stmt.setObject(pCounter++, search.getCreatedSince());
					}

					rs = stmt.executeQuery();

					// get all the selected templates and add to the multichannel fallback list
					while (rs.next()) {
						objectList.addObject(rs.getInt("id"), rs.getInt("channelset_id"), rs.getInt("channel_id"), false, (Integer)null);
					}

					// get the ids
					ids = objectList.getObjectIds();
				} else {
					if (isSearch) {
						// get all templates linked to the folder which satisfy the given search term
						StringBuffer sql = new StringBuffer("SELECT template.id FROM template_folder ");

						sql.append("LEFT JOIN template ON template_folder.template_id = template.id ");
						sql.append("WHERE template_folder.folder_id = ?");

						// do the search
						if (isStringSearch) {
							sql.append(" AND (template.id = ?").append(" OR LOWER(template.name) LIKE ?").append(" OR LOWER(template.description) LIKE ?)");
						}

						List<SystemUser> editors = search.getEditors();

						if (editors != null) {
							if (ObjectTransformer.isEmpty(editors)) {
								return ids;
							}
							sql.append(" AND editor IN (");
							sql.append(StringUtils.repeat("?", editors.size(), ","));
							sql.append(")");
						}
						List<SystemUser> creators = search.getCreators();

						if (creators != null) {
							if (ObjectTransformer.isEmpty(creators)) {
								return ids;
							}
							sql.append(" AND template.creator IN (");
							sql.append(StringUtils.repeat("?", creators.size(), ","));
							sql.append(")");
						}

						if (search.getEditedBefore() > 0) {
							sql.append(" AND template.edate <= ?");
						}
						if (search.getEditedSince() > 0) {
							sql.append(" AND template.edate >= ?");
						}
						if (search.getCreatedBefore() > 0) {
							sql.append(" AND template.cdate <= ?");
						}
						if (search.getCreatedSince() > 0) {
							sql.append(" AND template.cdate >= ?");
						}

						stmt = t.prepareStatement(sql.toString());

						int pCounter = 1;

						stmt.setObject(pCounter++, getMaster().getId());

						if (isStringSearch) {
							String pattern_nolike = search.getSearchString();
							String pattern_like = "%" + search.getSearchString() + "%";

							// add search term three times (for name, description and id)
							stmt.setObject(pCounter++, pattern_nolike);
							stmt.setObject(pCounter++, pattern_like);
							stmt.setObject(pCounter++, pattern_like);
						}

						if (editors != null) {
							for (SystemUser editor : editors) {
								stmt.setObject(pCounter++, editor.getId());
							}
						}
						if (creators != null) {
							for (SystemUser creator : creators) {
								stmt.setObject(pCounter++, creator.getId());
							}
						}

						if (search.getEditedBefore() > 0) {
							stmt.setObject(pCounter++, search.getEditedBefore());
						}
						if (search.getEditedSince() > 0) {
							stmt.setObject(pCounter++, search.getEditedSince());
						}
						if (search.getCreatedBefore() > 0) {
							stmt.setObject(pCounter++, search.getCreatedBefore());
						}
						if (search.getCreatedSince() > 0) {
							stmt.setObject(pCounter++, search.getCreatedSince());
						}

						rs = stmt.executeQuery();
					} else {
						// just get the linked templates
						stmt = t.prepareStatement("SELECT template_id id FROM template_folder WHERE folder_id = ?");
						stmt.setObject(1, getMaster().getId());

						rs = stmt.executeQuery();
					}

					while (rs.next()) {
						ids.add(new Integer(rs.getInt("id")));
					}
				}

			} catch (Exception e) {
				throw new NodeException("Could not load template ids for " + this, e);
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}

			return ids;
		}

		/**
		 * Get the pageIds for the given search. If the search is empty, the pageIds might be stored in the map {@link #pageIds}.
		 * @param search page search
		 * @return page IDs
		 * @throws NodeException
		 */
		private Collection<Integer> getPageIds(PageSearch search) throws NodeException {
			if (search == null) {
				search = PageSearch.create();
			}
			Wastebin wastebin = TransactionManager.getCurrentTransaction().getWastebinFilter();
			if (!search.isEmpty()) {
				return loadPageIds(search, wastebin);
			} else {
				return pageIds.get(getNode().getId(), wastebin, (nodeId, wb) -> {
					return loadPageIds(PageSearch.create(), wb);
				});
			}
		}

		/**
		 * Load the page ids and return them
		 * @param search search string if search shall be done (may be null)
		 * @param wastebin wastebin state
		 * @return list of page ids
		 * @throws NodeException
		 */
		private Collection<Integer> loadPageIds(PageSearch search, Wastebin wastebin) throws NodeException {
			if (search == null) {
				search = PageSearch.create();
			}
			boolean isSearch = !search.isEmpty();
			boolean isStringSearch = !ObjectTransformer.isEmpty(search.getSearchString());
			boolean isFileNameSearch = !ObjectTransformer.isEmpty(search.getFileNameSearch());
			boolean isNiceUrlSearch = !ObjectTransformer.isEmpty(search.getNiceUrlSearch());

			Collection<Integer> ids = new ArrayList<Integer>();

			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement stmt = null;
			ResultSet rs = null;

			// check whether multichannelling is supported and this node is a
			// channel
			boolean multiChannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);

			try {
				List<Integer> folderIds = new Vector<Integer>();
				folderIds.add(ObjectTransformer.getInteger(getMaster().getId(), 0));

				if (search.isRecursive()) {
					// Construct the list of needed folder permissions.
					// Also add the custom search permissions in order to
					// only include those which match the given permissions.
					List<Integer> neededPerms = new ArrayList<>();
					neededPerms.add(PermHandler.PERM_VIEW);
					// Resolve the given folder permissions to perm bit offsets
					for (Permission perm : search.getPermissions()) {
						int permOffset = PermHandler.resolvePermission(perm, Page.TYPE_PAGE);
						if (permOffset != -1) {
							neededPerms.add(permOffset);
						}
					}
					Integer[] perms = new Integer[neededPerms.size()];
					perms = neededPerms.toArray(perms);
					collectSubfolderIds(folderIds, Page.TYPE_PAGE, perms);
				}

				if (multiChannelling) {
					boolean nodeIsChannel = getNode().isChannel();

					// first check the special case when searching for inherited pages in master nodes
					if (search.getInherited() != null && search.getInherited().booleanValue() && !nodeIsChannel) {
						return ids;
					}

					// get the master nodes
					List<Node> masterNodes = getNode().getMasterNodes();

					// prepare the objectlist with multichannelling fallback
					MultiChannellingFallbackList objectList = new MultiChannellingFallbackList(getNode());

					// filter for channels, if only inherited or localized/local objects shall be fetched
					if (search.getInherited() != null) {
						if (search.getInherited().booleanValue()) {
							// filter for only inherited pages
							for (Node node : masterNodes) {
								objectList.addFilteredChannelId(node.getId());
							}
							if (nodeIsChannel) {
								// if the node is a channel, also pages from the master node (with channelId = 0) are allowed
								objectList.addFilteredChannelId(0);
							}
						} else {
							// filter for only local/localized pages
							objectList.addFilteredChannelId(getNode().getId());
							if (!nodeIsChannel) {
								// if the node is a master, also add 0 to the list of filtered channel ids
								objectList.addFilteredChannelId(0);
							}
						}
					}

					// get all pages
					StringBuffer sql = new StringBuffer("SELECT DISTINCT page.id, page.channelset_id, page.channel_id, page.mc_exclude, page_disinherit.channel_id disinherited_node FROM page ");

					// ... that have a content and a template
					sql.append("LEFT JOIN page_disinherit ON page.id = page_disinherit.page_id ");

					sql.append("LEFT JOIN content ON page.content_id = content.id ");
					sql.append("LEFT JOIN template ON page.template_id = template.id ");

					if (search.isWorkflowOwn() || search.isWorkflowWatch()) {
						sql.append("LEFT JOIN publishworkflow wf ON page.id = wf.page_id ");
					}
					if (search.isWorkflowOwn()) {
						sql.append("LEFT JOIN publishworkflow_step wfs ON wf.currentstep_id = wfs.id ");
						sql.append("LEFT JOIN publishworkflowstep_group wfsg ON wfs.id = wfsg.publishworkflowstep_id ");
					}
					if (search.isWorkflowWatch()) {
						sql.append("LEFT JOIN publishworkflow_step wfs2 ON wf.id = wfs2.publishworkflow_id ");
					}

					if (isStringSearch && search.isSearchContent()) {
						sql.append("LEFT JOIN contenttag ON contenttag.content_id = page.content_id ");
						sql.append("LEFT JOIN value ON value.contenttag_id = contenttag.id ");
					}

					if (isNiceUrlSearch || isStringSearch) {
						sql.append("LEFT JOIN page_alt_url ON page.id = page_alt_url.page_id ");
					}

					sql.append("WHERE page.folder_id IN (").append(StringUtils.repeat("?", folderIds.size(), ",")).append(") AND page.channel_id IN (");
					sql.append(StringUtils.repeat("?", masterNodes.size() + 2, ","));
					sql.append(")");

					sql.append(" AND content.id IS NOT NULL AND template.id IS NOT NULL");

					SystemUser user = (SystemUser) t.getObject(SystemUser.class, t.getUserId());
					Integer userId = null;
					List<UserGroup> userGroups = Collections.emptyList();

					if (user != null) {
						userId = user.getId();
						userGroups = user.getUserGroups();
					}

					// restrict for status flags
					when(search.getOnline(), () -> sql.append(" AND page.online = 1"), () -> sql.append(" AND page.online = 0"));
					when(search.getModified(), () -> sql.append(" AND page.modified = 1"), () -> sql.append(" AND page.modified = 0"));
					when(search.getQueued(), () -> sql.append(" AND (page.pub_queue != 0 OR page.off_queue != 0)"),
							() -> sql.append(" AND (page.pub_queue = 0 AND page.off_queue = 0)"));
					when(search.getPlanned(), () -> sql.append(" AND (page.time_pub != 0 OR page.time_off != 0)"),
							() -> sql.append(" AND (page.time_pub = 0 AND page.time_off = 0)"));

					// do a string search
					if (isStringSearch) {
						sql.append(" AND (page.id = ?").append(" OR LOWER(page.name) LIKE ?").append(" OR LOWER(page.filename) LIKE ?").append(
								" OR LOWER(page.description) LIKE ? OR LOWER(page.nice_url) LIKE ? OR LOWER(page_alt_url.url) LIKE ?");
						if (search.isSearchContent()) {
							sql.append(" OR LOWER(value.value_text) LIKE ?");
						}
						sql.append(")");
					}

					// do a filename search
					if (isFileNameSearch) {
						sql.append(" AND LOWER(page.filename) LIKE ?");
					}

					if (isNiceUrlSearch) {
						sql.append(" AND (page.nice_url REGEXP ? OR page_alt_url.url REGEXP ?)");
					}

					if (search.isWorkflowWatch()) {
						sql.append(" AND wfs2.creator = ?");
					}
					if (search.isWorkflowOwn()) {
						sql.append(" AND wfsg.group_id IN (");
						sql.append(StringUtils.repeat("?", userGroups.size(), ","));
						sql.append(")");
					}

					if (search.isEditor()) {
						sql.append(" AND page.editor = ?");
					}
					if (search.isCreator()) {
						sql.append(" AND page.creator = ?");
					}
					if (search.isPublisher()) {
						sql.append(" AND page.publisher = ?");
					}

					if (search.getPriority() > 0) {
						sql.append(" AND page.priority = ?");
					}

					List<Integer> templateIds = search.getTemplateIds();

					if (!ObjectTransformer.isEmpty(templateIds)) {
						sql.append(" AND page.template_id IN (");
						sql.append(StringUtils.repeat("?", templateIds.size(), ","));
						sql.append(")");
					}

					List<SystemUser> editors = search.getEditors();

					if (editors != null) {
						if (ObjectTransformer.isEmpty(editors)) {
							return ids;
						}
						sql.append(" AND page.editor IN (");
						sql.append(StringUtils.repeat("?", editors.size(), ","));
						sql.append(")");
					}
					List<SystemUser> creators = search.getCreators();

					if (creators != null) {
						if (ObjectTransformer.isEmpty(creators)) {
							return ids;
						}
						sql.append(" AND page.creator IN (");
						sql.append(StringUtils.repeat("?", creators.size(), ","));
						sql.append(")");
					}
					List<SystemUser> publishers = search.getPublishers();

					if (publishers != null) {
						if (ObjectTransformer.isEmpty(publishers)) {
							return ids;
						}
						sql.append(" AND page.publisher IN (");
						sql.append(StringUtils.repeat("?", publishers.size(), ","));
						sql.append(")");
					}

					if (search.getEditedBefore() > 0) {
						sql.append(" AND page.edate <= ?");
					}
					if (search.getEditedSince() > 0) {
						sql.append(" AND page.edate >= ?");
					}
					if (search.getCreatedBefore() > 0) {
						sql.append(" AND page.cdate <= ?");
					}
					if (search.getCreatedSince() > 0) {
						sql.append(" AND page.cdate >= ?");
					}
					if (search.getPublishedBefore() > 0) {
						sql.append(" AND page.pdate > 0 AND page.pdate <= ?");
					}
					if (search.getPublishedSince() > 0) {
						sql.append(" AND page.pdate >= ?");
					}

					if (search.isWastebin()) {
						sql.append(" AND page.deleted != 0");
					}

					sql.append(wastebin.filterClause("page"));

					List<Integer> includeMlIds = search.getIncludeMlIds();
					if (!ObjectTransformer.isEmpty(includeMlIds)) {
						sql.append(" AND template.ml_id IN (");
						sql.append(StringUtils.repeat("?", includeMlIds.size(), ","));
						sql.append(")");
					}

					List<Integer> excludeMlIds = search.getExcludeMlIds();
					if (!ObjectTransformer.isEmpty(excludeMlIds)) {
						sql.append(" AND template.ml_id NOT IN (");
						sql.append(StringUtils.repeat("?", excludeMlIds.size(), ","));
						sql.append(")");
					}

					stmt = t.prepareStatement(sql.toString());
					int pCounter = 1;

					for (Integer fId : folderIds) {
						stmt.setObject(pCounter++, fId);
					}

					// add the channel ids
					stmt.setObject(pCounter++, 0);
					for (Node node : masterNodes) {
						stmt.setObject(pCounter++, node.getId());
					}
					stmt.setObject(pCounter++, getNode().getId());

					if (isStringSearch) {
						String pattern_nolike = search.getSearchString();
						String pattern_like = "%" + search.getSearchString() + "%";

						// add search term for id, name, filename, description, nice_url, page_alt_url.url
						stmt.setObject(pCounter++, pattern_nolike);
						stmt.setObject(pCounter++, pattern_like);
						stmt.setObject(pCounter++, pattern_like);
						stmt.setObject(pCounter++, pattern_like);
						stmt.setObject(pCounter++, pattern_like);
						stmt.setObject(pCounter++, pattern_like);

						if (search.isSearchContent()) {
							// once again (for content search)
							stmt.setObject(pCounter++, pattern_like);
						}
					}

					if (isFileNameSearch) {
						stmt.setObject(pCounter++, "%" + search.getFileNameSearch() + "%");
					}

					if (isNiceUrlSearch) {
						stmt.setObject(pCounter++, search.getNiceUrlSearch());
						stmt.setObject(pCounter++, search.getNiceUrlSearch());
					}

					if (search.isWorkflowWatch()) {
						stmt.setObject(pCounter++, userId);
					}
					if (search.isWorkflowOwn()) {
						for (UserGroup group : userGroups) {
							stmt.setObject(pCounter++, group.getId());
						}
					}

					if (search.isEditor()) {
						stmt.setObject(pCounter++, userId);
					}
					if (search.isCreator()) {
						stmt.setObject(pCounter++, userId);
					}
					if (search.isPublisher()) {
						stmt.setObject(pCounter++, userId);
					}

					if (search.getPriority() > 0) {
						stmt.setInt(pCounter++, search.getPriority());
					}

					if (!ObjectTransformer.isEmpty(templateIds)) {
						for (Integer templateId : templateIds) {
							stmt.setInt(pCounter++, templateId);
						}
					}

					if (editors != null) {
						for (SystemUser editor : editors) {
							stmt.setObject(pCounter++, editor.getId());
						}
					}
					if (creators != null) {
						for (SystemUser creator : creators) {
							stmt.setObject(pCounter++, creator.getId());
						}
					}
					if (publishers != null) {
						for (SystemUser publisher : publishers) {
							stmt.setObject(pCounter++, publisher.getId());
						}
					}

					if (search.getEditedBefore() > 0) {
						stmt.setObject(pCounter++, search.getEditedBefore());
					}
					if (search.getEditedSince() > 0) {
						stmt.setObject(pCounter++, search.getEditedSince());
					}
					if (search.getCreatedBefore() > 0) {
						stmt.setObject(pCounter++, search.getCreatedBefore());
					}
					if (search.getCreatedSince() > 0) {
						stmt.setObject(pCounter++, search.getCreatedSince());
					}
					if (search.getPublishedBefore() > 0) {
						stmt.setObject(pCounter++, search.getPublishedBefore());
					}
					if (search.getPublishedSince() > 0) {
						stmt.setObject(pCounter++, search.getPublishedSince());
					}

					if (!ObjectTransformer.isEmpty(includeMlIds)) {
						for (Integer mlId : includeMlIds) {
							stmt.setInt(pCounter++, mlId);
						}
					}

					if (!ObjectTransformer.isEmpty(excludeMlIds)) {
						for (Integer mlId : excludeMlIds) {
							stmt.setInt(pCounter++, mlId);
						}
					}

					rs = stmt.executeQuery();

					// get all the selected templates and add to the multichannel fallback list
					while (rs.next()) {
						objectList.addObject(rs.getInt("id"), rs.getInt("channelset_id"), rs.getInt("channel_id"), rs.getBoolean("mc_exclude"), rs.getInt("disinherited_node"));
					}

					// get the ids
					ids = objectList.getObjectIds();
				} else {
					if (isSearch) {
						// get all pages which satisfy the given search term
						StringBuffer sql = new StringBuffer("SELECT DISTINCT page.id FROM page ");

						sql.append("LEFT JOIN content ON page.content_id = content.id ");
						sql.append("LEFT JOIN template ON page.template_id = template.id ");

						if (search.isWorkflowOwn() || search.isWorkflowWatch()) {
							sql.append("LEFT JOIN publishworkflow wf ON page.id = wf.page_id ");
						}
						if (search.isWorkflowOwn()) {
							sql.append("LEFT JOIN publishworkflow_step wfs ON wf.currentstep_id = wfs.id ");
							sql.append("LEFT JOIN publishworkflowstep_group wfsg ON wfs.id = wfsg.publishworkflowstep_id ");
						}
						if (search.isWorkflowWatch()) {
							sql.append("LEFT JOIN publishworkflow_step wfs2 ON wf.id = wfs2.publishworkflow_id ");
						}
						if (isStringSearch && search.isSearchContent()) {
							sql.append("LEFT JOIN contenttag ON contenttag.content_id = page.content_id ");
							sql.append("LEFT JOIN value ON value.contenttag_id = contenttag.id ");
						}

						if (isNiceUrlSearch || isStringSearch) {
							sql.append("LEFT JOIN page_alt_url ON page.id = page_alt_url.page_id ");
						}

						sql.append("WHERE page.folder_id IN (").append(StringUtils.repeat("?", folderIds.size(), ",")).append(") ");
						sql.append(" AND content.id IS NOT NULL AND template.id IS NOT NULL");

						SystemUser user = (SystemUser) t.getObject(SystemUser.class, t.getUserId());
						Object userId = null;
						List<UserGroup> userGroups = Collections.emptyList();

						if (user != null) {
							userGroups = user.getUserGroups();
							userId = user.getId();
						}

						// restrict for status flags
						when(search.getOnline(), () -> sql.append(" AND page.online = 1"), () -> sql.append(" AND page.online = 0"));
						when(search.getModified(), () -> sql.append(" AND page.modified = 1"), () -> sql.append(" AND page.modified = 0"));
						when(search.getQueued(), () -> sql.append(" AND (page.pub_queue != 0 OR page.off_queue != 0)"),
								() -> sql.append(" AND (page.pub_queue = 0 AND page.off_queue = 0)"));
						when(search.getPlanned(), () -> sql.append(" AND (page.time_pub != 0 OR page.time_off != 0)"),
								() -> sql.append(" AND (page.time_pub = 0 AND page.time_off = 0)"));

						// do a string search
						if (isStringSearch) {
							sql.append(" AND (page.id = ?").append(" OR LOWER(page.name) LIKE ?").append(" OR LOWER(page.filename) LIKE ?").append(
									" OR LOWER(page.description) LIKE ? OR LOWER(page.nice_url) LIKE ? OR LOWER(page_alt_url.url) LIKE ?");
							if (search.isSearchContent()) {
								sql.append(" OR LOWER(value.value_text) LIKE ?");
							}
							sql.append(")");
						}

						// do the filename search
						if (isFileNameSearch) {
							sql.append(" AND LOWER(page.filename) LIKE ?");
						}

						if (isNiceUrlSearch) {
							sql.append(" AND (page.nice_url REGEXP ? OR page_alt_url.url REGEXP ?)");
						}

						if (search.isWorkflowWatch()) {
							sql.append(" AND wfs2.creator = ?");
						}
						if (search.isWorkflowOwn()) {
							sql.append(" AND wfsg.group_id IN (");
							sql.append(StringUtils.repeat("?", userGroups.size(), ","));
							sql.append(")");
						}

						if (search.isEditor()) {
							sql.append(" AND page.editor = ?");
						}
						if (search.isCreator()) {
							sql.append(" AND page.creator = ?");
						}
						if (search.isPublisher()) {
							sql.append(" AND page.publisher = ?");
						}

						if (search.getPriority() > 0) {
							sql.append(" AND page.priority = ?");
						}

						List<Integer> templateIds = search.getTemplateIds();

						if (!ObjectTransformer.isEmpty(templateIds)) {
							sql.append(" AND page.template_id IN (");
							sql.append(StringUtils.repeat("?", templateIds.size(), ","));
							sql.append(")");
						}

						List<SystemUser> editors = search.getEditors();

						if (editors != null) {
							if (ObjectTransformer.isEmpty(editors)) {
								return ids;
							}
							sql.append(" AND page.editor IN (");
							sql.append(StringUtils.repeat("?", editors.size(), ","));
							sql.append(")");
						}
						List<SystemUser> creators = search.getCreators();

						if (creators != null) {
							if (ObjectTransformer.isEmpty(creators)) {
								return ids;
							}
							sql.append(" AND page.creator IN (");
							sql.append(StringUtils.repeat("?", creators.size(), ","));
							sql.append(")");
						}
						List<SystemUser> publishers = search.getPublishers();

						if (publishers != null) {
							if (ObjectTransformer.isEmpty(publishers)) {
								return ids;
							}
							sql.append(" AND page.publisher IN (");
							sql.append(StringUtils.repeat("?", publishers.size(), ","));
							sql.append(")");
						}

						if (search.getEditedBefore() > 0) {
							sql.append(" AND page.edate <= ?");
						}
						if (search.getEditedSince() > 0) {
							sql.append(" AND page.edate >= ?");
						}
						if (search.getCreatedBefore() > 0) {
							sql.append(" AND page.cdate <= ?");
						}
						if (search.getCreatedSince() > 0) {
							sql.append(" AND page.cdate >= ?");
						}
						if (search.getPublishedBefore() > 0) {
							sql.append(" AND page.pdate <= ?");
						}
						if (search.getPublishedSince() > 0) {
							sql.append(" AND page.pdate >= ?");
						}

						if (search.isWastebin()) {
							sql.append(" AND page.deleted != 0");
						}
						sql.append(wastebin.filterClause("page"));

						List<Integer> includeMlIds = search.getIncludeMlIds();
						if (!ObjectTransformer.isEmpty(includeMlIds)) {
							sql.append(" AND template.ml_id IN (");
							sql.append(StringUtils.repeat("?", includeMlIds.size(), ","));
							sql.append(")");
						}

						List<Integer> excludeMlIds = search.getExcludeMlIds();
						if (!ObjectTransformer.isEmpty(excludeMlIds)) {
							sql.append(" AND template.ml_id NOT IN (");
							sql.append(StringUtils.repeat("?", excludeMlIds.size(), ","));
							sql.append(")");
						}

						stmt = t.prepareStatement(sql.toString());

						int pCounter = 1;

						for (Integer fId : folderIds) {
							stmt.setObject(pCounter++, fId);
						}

						if (isStringSearch) {
							String pattern_nolike = search.getSearchString();
							String pattern_like = "%" + search.getSearchString() + "%";

							// add search term for id, name, filename and description, nice_url, page_alt_url.url
							stmt.setObject(pCounter++, pattern_nolike);
							stmt.setObject(pCounter++, pattern_like);
							stmt.setObject(pCounter++, pattern_like);
							stmt.setObject(pCounter++, pattern_like);
							stmt.setObject(pCounter++, pattern_like);
							stmt.setObject(pCounter++, pattern_like);

							if (search.isSearchContent()) {
								// once again (for content search)
								stmt.setObject(pCounter++, pattern_like);
							}
						}

						if (isFileNameSearch) {
							stmt.setObject(pCounter++, "%" + search.getFileNameSearch() + "%");
						}

						if (isNiceUrlSearch) {
							stmt.setObject(pCounter++, search.getNiceUrlSearch());
							stmt.setObject(pCounter++, search.getNiceUrlSearch());
						}

						if (search.isWorkflowWatch()) {
							stmt.setObject(pCounter++, userId);
						}
						if (search.isWorkflowOwn()) {
							for (UserGroup group : userGroups) {
								stmt.setObject(pCounter++, group.getId());
							}
						}

						if (search.isEditor()) {
							stmt.setObject(pCounter++, userId);
						}
						if (search.isCreator()) {
							stmt.setObject(pCounter++, userId);
						}
						if (search.isPublisher()) {
							stmt.setObject(pCounter++, userId);
						}

						if (search.getPriority() > 0) {
							stmt.setInt(pCounter++, search.getPriority());
						}

						if (!ObjectTransformer.isEmpty(templateIds)) {
							for (Integer templateId : templateIds) {
								stmt.setInt(pCounter++, templateId);
							}
						}

						if (editors != null) {
							for (SystemUser editor : editors) {
								stmt.setObject(pCounter++, editor.getId());
							}
						}
						if (creators != null) {
							for (SystemUser creator : creators) {
								stmt.setObject(pCounter++, creator.getId());
							}
						}
						if (publishers != null) {
							for (SystemUser publisher : publishers) {
								stmt.setObject(pCounter++, publisher.getId());
							}
						}

						if (search.getEditedBefore() > 0) {
							stmt.setObject(pCounter++, search.getEditedBefore());
						}
						if (search.getEditedSince() > 0) {
							stmt.setObject(pCounter++, search.getEditedSince());
						}
						if (search.getCreatedBefore() > 0) {
							stmt.setObject(pCounter++, search.getCreatedBefore());
						}
						if (search.getCreatedSince() > 0) {
							stmt.setObject(pCounter++, search.getCreatedSince());
						}
						if (search.getPublishedBefore() > 0) {
							stmt.setObject(pCounter++, search.getPublishedBefore());
						}
						if (search.getPublishedSince() > 0) {
							stmt.setObject(pCounter++, search.getPublishedSince());
						}

						if (!ObjectTransformer.isEmpty(includeMlIds)) {
							for (Integer mlId : includeMlIds) {
								stmt.setInt(pCounter++, mlId);
							}
						}

						if (!ObjectTransformer.isEmpty(excludeMlIds)) {
							for (Integer mlId : excludeMlIds) {
								stmt.setInt(pCounter++, mlId);
							}
						}

						rs = stmt.executeQuery();
					} else {
						// just get the pages
						StringBuilder sql = new StringBuilder(
								"SELECT page.id FROM page LEFT JOIN content ON page.content_id = content.id LEFT JOIN template ON page.template_id = template.id WHERE page.folder_id = ? AND content.id IS NOT NULL AND template.id IS NOT NULL");
						sql.append(wastebin.filterClause("page"));

						stmt = t.prepareStatement(sql.toString());
						stmt.setObject(1, getMaster().getId());

						rs = stmt.executeQuery();
					}

					while (rs.next()) {
						ids.add(new Integer(rs.getInt("id")));
					}
				}
			} catch (Exception e) {
				throw new NodeException("Could not load page ids for " + this, e);
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}

			return ids;
		}

		/**
		 * Collect the subfolder ids with given permission bits for the current user
		 * @param ids collection of ids, which will contain all matching subfolders
		 * @param objType type of the objects, that will be fetched from the subfolders (for checking role permissions)
		 * @param permBits permission bits to check
		 * @throws NodeException
		 */
		private void collectSubfolderIds(List<Integer> ids, int objType, Integer... permBits) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PermHandler permHandler = t.getPermHandler();

			List<Folder> childFolders = getChildFolders();

			for (Folder child : childFolders) {
				if (permHandler == null || ((FactoryFolder) child).checkFolderPermissions(permHandler, objType, permBits)) {
					ids.add(ObjectTransformer.getInteger(child.getMaster().getId(), 0));
					((FactoryFolder) child).collectSubfolderIds(ids, objType, permBits);
				}
			}
		}

		/**
		 * Check whether the user has the given permission bit set on this folder
		 * @param permHandler permission handler
		 * @param objType type of the objects, that will be fetched from the subfolders (for checking role permissions)
		 * @param permBit permission bit to check
		 * @return true when the user has the permission bit set, false if not
		 */
		private boolean checkPermission(PermHandler permHandler, int objType, int permBit) {
			int roleBit = -1;
			switch (objType) {
			case Page.TYPE_PAGE:
			case File.TYPE_FILE:
			case ImageFile.TYPE_IMAGE:
				switch (permBit) {
				case PermHandler.PERM_PAGE_VIEW:
					roleBit = PermHandler.ROLE_VIEW;
					break;
				case PermHandler.PERM_PAGE_CREATE:
					roleBit = PermHandler.ROLE_CREATE;
					break;
				case PermHandler.PERM_PAGE_UPDATE:
					roleBit = PermHandler.ROLE_UPDATE;
					break;
				case PermHandler.PERM_PAGE_DELETE:
					roleBit = PermHandler.ROLE_DELETE;
					break;
				case PermHandler.PERM_PAGE_PUBLISH:
					roleBit = PermHandler.ROLE_PUBLISH;
					break;
				}
				break;
			default:
				break;
			}
			return permHandler.checkPermissionBit(Folder.TYPE_FOLDER, getId(), permBit, objType, -1, roleBit)
					|| permHandler.checkPermissionBit(Node.TYPE_NODE, getId(), permBit, objType, -1, roleBit);
		}

		/**
		 * Check whether the user has ALL of the given permission bits set on this folder
		 * @param permHandler permission handler
		 * @param objType type of the objects, that will be fetched from the subfolders (for checking role permissions)
		 * @param permBits array of permission bits to check
		 * @return true when the user has ALL of the permission bits set, false if not
		 */
		protected boolean checkFolderPermissions(PermHandler permHandler, int objType, Integer... permBits) {
			boolean perm = true;

			for (int i = 0; i < permBits.length; i++) {
				perm &= checkPermission(permHandler, objType, permBits[i]);
			}

			return perm;
		}

		/**
		 * retrieve images and files from node database as defined by loadType
		 * @param loadType determines if you want to load images, files or both. Use defined statics
		 * @return list of images or/and files
		 * @throws NodeException
		 */
		private <T extends File> List<T> loadFilesAndImages(Class<T> clazz, int loadType, FileSearch search) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			Collection<Integer> objectIds = null;
			List<T> objects = null;

			switch (loadType) {
			case LOAD_FILES:
				objectIds = new Vector<Integer>(getFileIds(LOAD_FILES, search));
				objects = new Vector<T>(t.getObjects(clazz, objectIds));
				break;

			case LOAD_IMAGES:
				objectIds = new Vector<Integer>(getFileIds(LOAD_IMAGES, search));
				objects = new Vector<T>(t.getObjects(clazz, objectIds));
				break;

			case LOAD_FILESANDIMAGES:
				// get the files
				objectIds = new Vector<Integer>(getFileIds(LOAD_FILES, search));
				objects = new Vector<T>(t.getObjects(clazz, objectIds));

				// get the images
				Collection<Integer> imageIds = getFileIds(LOAD_IMAGES, search);

				objectIds.addAll(imageIds);
				objects.addAll(t.getObjects(clazz, imageIds));
				break;

			default:
				// if we reach this part of code an invalid loadType was
				// provided
				throw new NodeException("Invalid loadType {" + loadType + "} specified).");
			}

			// if a search was done in a multichannelled environment, the result
			// probably is not correct, see Method getPages() for details.
			if (search != null && !search.isEmpty()) {
				Node node = t.getChannel();
				if (node == null) {
					node = getNode();
				}

				Map<Integer, Set<Integer>> fileUsageMap = null;
				if (search.getUsed() != null) {
					fileUsageMap = DependencyManager.getFileUsageMap();
				}

				for (Iterator<T> iObject = objects.iterator(); iObject.hasNext();) {
					T file = iObject.next();

					if (!objectIds.contains(file.getId())) {
						iObject.remove();
					} else if (search.getOnline() != null && FileOnlineStatus.isOnline(file, node) != search.getOnline()) {
						iObject.remove();
					} else if (search.getBroken() != null && file.isBroken() != search.getBroken().booleanValue()) {
						iObject.remove();
					} else if (search.getUsed() != null && file.isUsed(fileUsageMap, search.getUsedIn()) != search.getUsed().booleanValue()) {
						iObject.remove();
					}
				}
			}

			return objects;
		}

		/**
		 * Get the file IDs of given type for the search
		 * @param loadType type of objects
		 * @param search search
		 * @return list of object IDs
		 * @throws NodeException
		 */
		private Collection<Integer> getFileIds(int loadType, FileSearch search) throws NodeException {
			if (loadType != LOAD_FILES && loadType != LOAD_IMAGES) {
				throw new NodeException("Unsupported loadType {" + loadType + "} specified, expecting LOAD_FILES or LOAD_IMAGES");
			}
			if (search == null) {
				search = FileSearch.create();
			}
			Wastebin wastebin = TransactionManager.getCurrentTransaction().getWastebinFilter();
			if (!search.isEmpty()) {
				return loadFileIds(loadType, search, wastebin);
			} else {
				if (loadType == LOAD_FILES) {
					return fileIds.get(getNode().getId(), wastebin, (nodeId, wb) -> {
						return loadFileIds(LOAD_FILES, FileSearch.create(), wb);
					});
				} else {
					return imageIds.get(getNode().getId(), wastebin, (nodeId, wb) -> {
						return loadFileIds(LOAD_IMAGES, FileSearch.create(), wb);
					});
				}
			}
		}

		/**
		 * load ids for files and images
		 * @param loadType whether to load ids of images, files or both
		 * @param search search
		 * @param wastebin wastebin filter (must not be null)
		 * @throws NodeException
		 */
		private Collection<Integer> loadFileIds(int loadType, FileSearch search, Wastebin wastebin) throws NodeException {
			if (loadType != LOAD_FILES && loadType != LOAD_IMAGES) {
				throw new NodeException("Unsupported loadType {" + loadType + "} specified, expecting LOAD_FILES or LOAD_IMAGES");
			}
			if (search == null) {
				search = FileSearch.create();
			}

			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement stmt = null;
			ResultSet rs = null;
			List<Integer> ids = new ArrayList<Integer>();

			// check whether multichannelling is supported and this node is a
			// channel
			boolean multiChannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);

			boolean niceUrlSearch = !StringUtils.isEmpty(search.getNiceUrlSearch());
			boolean isStringSearch = !StringUtils.isEmpty(search.getSearchString());

			try {
				List<Integer> folderIds = new Vector<Integer>();

				folderIds.add(ObjectTransformer.getInteger(getMaster().getId(), 0));

				if (search.isRecursive()) {
					collectSubfolderIds(folderIds, File.TYPE_FILE, PermHandler.PERM_VIEW);
				}

				if (multiChannelling) {
					boolean nodeIsChannel = getNode().isChannel();

					// first check the special case when searching for inherited files in master nodes
					if (search.getInherited() != null && search.getInherited().booleanValue() && !nodeIsChannel) {
						return ids;
					}

					List<Node> masterNodes = getNode().getMasterNodes();
					StringBuffer sql = new StringBuffer("SELECT contentfile.id, channelset_id, contentfile.channel_id, mc_exclude, contentfile_disinherit.channel_id disinherited_node FROM contentfile ")
							.append("LEFT JOIN contentfile_disinherit on contentfile.id = contentfile_disinherit.contentfile_id ");
					if (niceUrlSearch || isStringSearch) {
						sql.append("LEFT JOIN contentfile_alt_url ON contentfile.id = contentfile_alt_url.contentfile_id ");
					}
					sql.append("WHERE folder_id IN (");

					sql.append(StringUtils.repeat("?", folderIds.size(), ",")).append(")");

					appendFileSearchSql(sql, search, loadType);
					sql.append(" AND contentfile.channel_id IN (").append(StringUtils.repeat("?", masterNodes.size() + 2, ",")).append(")");

					sql.append(wastebin.filterClause("contentfile"));

					stmt = t.prepareStatement(sql.toString());

					int pCounter = 1;

					for (Integer fId : folderIds) {
						stmt.setObject(pCounter++, fId);
					}

					pCounter = setFileSearchSqlValues(stmt, search, pCounter);
					// always search for files without channel_id
					stmt.setObject(pCounter++, 0);
					// ... and belonging to this channel
					stmt.setObject(pCounter++, getNode().getId());
					// ... and all channels in above this channel
					for (Node node : masterNodes) {
						stmt.setObject(pCounter++, node.getId());
					}

					rs = stmt.executeQuery();

					// prepare the objectlist
					MultiChannellingFallbackList objectList = new MultiChannellingFallbackList(getNode());

					// filter for channels, if only inherited or localized/local objects shall be fetched
					if (search.getInherited() != null) {
						if (search.getInherited().booleanValue()) {
							// filter for only inherited pages
							for (Node node : masterNodes) {
								objectList.addFilteredChannelId(node.getId());
							}
							if (nodeIsChannel) {
								// if the node is a channel, also pages from the master node (with channelId = 0) are allowed
								objectList.addFilteredChannelId(0);
							}
						} else {
							// filter for only local/localized pages
							objectList.addFilteredChannelId(getNode().getId());
							if (!nodeIsChannel) {
								// if the node is a master, also add 0 to the list of filtered channel ids
								objectList.addFilteredChannelId(0);
							}
						}
					}

					// fill in all the contentfiles
					while (rs.next()) {
						objectList.addObject(rs.getInt("id"), rs.getInt("channelset_id"), rs.getInt("channel_id"), rs.getBoolean("mc_exclude"), rs.getInt("disinherited_node"));
					}

					ids = objectList.getObjectIds();
				} else {
					StringBuffer sql = new StringBuffer("SELECT contentfile.id FROM contentfile ");
					if (niceUrlSearch || isStringSearch) {
						sql.append("LEFT JOIN contentfile_alt_url ON contentfile.id = contentfile_alt_url.contentfile_id ");
					}
					sql.append("WHERE folder_id IN (");

					sql.append(StringUtils.repeat("?", folderIds.size(), ","));
					sql.append(")");

					appendFileSearchSql(sql, search, loadType);
					sql.append(wastebin.filterClause("contentfile"));

					stmt = t.prepareStatement(sql.toString());

					int pCounter = 1;

					for (Integer fId : folderIds) {
						stmt.setObject(pCounter++, fId);
					}
					pCounter = setFileSearchSqlValues(stmt, search, pCounter);

					rs = stmt.executeQuery();

					while (rs.next()) {
						ids.add(new Integer(rs.getInt("id")));
					}
				}

				return ids;
			} catch (Exception e) {
				throw new NodeException("Could not load contentfiles or images (loadType {" + loadType + "}).", e);
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}
		}

		/**
		 * Will append to the given StringBuffer a SQL expression representing the given
		 * FileSearch and loadType.
		 *
		 * Will use '?' characters in place of values. To get the values the resulting
		 * SQL must be made part of a PreparedStatment and {@link #setFileSearchSqlValues(PreparedStatement, FileSearch, int)}
		 * must be invoked on the PreparedStatement.
		 *
		 * @param sql
		 * 			Will receive a SQL expression. A leading ' AND' is appended, so it
		 * 			requires a leading expression already to be present in the given SqlBuffer.
		 * 			The caller is responsible for parenthesizing the expression if required.
		 * @param search
		 * 			The FileSearch according to which the SQL is constructed
		 * @param loadType
		 * 			One of LOAD_FILES and LOAD_IMAGES which can be used to
		 * 			the condition to match only one or the other. If none of these
		 * 			two values are given, both types will appear in the result.
		 * @return
		 * 			The given StringBuffer
		 */
		private StringBuffer appendFileSearchSql(StringBuffer sql, FileSearch search, int loadType) {
			boolean isStringSearch = !ObjectTransformer.isEmpty(search.getSearchString()); //

			if (isStringSearch) {
				sql.append(" AND (contentfile.id = ? OR name LIKE ? OR description LIKE ? OR LOWER(contentfile.nice_url) LIKE ? OR LOWER(contentfile_alt_url.url) LIKE ?)");
			}

			List<SystemUser> editors = search.getEditors();

			if (editors != null) {
				if (ObjectTransformer.isEmpty(editors)) {
					// In this case a search was made for a page that has no editors,
					// which makes no sense because every page must have an editor,
					// but to be consistent, we have to make the search return no results.
					sql.append(" AND 1 = 0");
				} else {
					sql.append(" AND editor IN (");
					sql.append(StringUtils.repeat("?", editors.size(), ","));
					sql.append(")");
				}
			}

			List<SystemUser> creators = search.getCreators();

			if (creators != null) {
				if (ObjectTransformer.isEmpty(creators)) {
					// Same as above: a search was made for a page with no creators,
					// which must return no results, because every page must have a creator.
					sql.append(" AND 1 = 0");
				} else {
					sql.append(" AND creator IN (");
					sql.append(StringUtils.repeat("?", creators.size(), ","));
					sql.append(")");
				}
			}

			if (search.getEditedBefore() > 0) {
				sql.append(" AND edate <= ?");
			}
			if (search.getEditedSince() > 0) {
				sql.append(" AND edate >= ?");
			}
			if (search.getCreatedBefore() > 0) {
				sql.append(" AND cdate <= ?");
			}
			if (search.getCreatedSince() > 0) {
				sql.append(" AND cdate >= ?");
			}

			switch (loadType) {
			case LOAD_FILES:
				sql.append(" AND (filetype NOT LIKE 'image%' OR filetype IS NULL)");
				break;

			case LOAD_IMAGES:
				sql.append(" AND filetype LIKE 'image%'");
				break;

			default:
				break;
			}

			if (search.isWastebin()) {
				sql.append(" AND deleted != 0");
			}

			if (!StringUtils.isEmpty(search.getNiceUrlSearch())) {
				sql.append(" AND (contentfile.nice_url REGEXP ? OR contentfile_alt_url.url REGEXP ?)");
			}

			if (!StringUtils.isEmpty(search.getMimeType())) {
				sql.append(" AND contentfile.filetype ")
					.append(search.isExcludeMimeType() ? "<>" : "=")
					.append(" ?");
			}

			return sql;
		}

		/**
		 * Will set the values of a PreparedStatement according to the given FileSearch.
		 * To be used in conjunction with {@link #appendFileSearchSql(StringBuffer, FileSearch, int)}.
		 *
		 * @param stmt
		 * 			The PreparedStatement that will receive the values.
		 * @param search
		 * 			The FileSearch according to which the values will be set.
		 * @param pCounter
		 * 			The index value to use with {@link PreparedStatement#setObject(int, Object)}.
		 * 			Must be the index of the first value to be set.
		 * @return
		 * 			The index of the last value set with {@link PreparedStatement#setObject(int, Object)}
		 * 			incremented by 1 (can be used to set the next value).
		 * @throws SQLException
		 */
		private int setFileSearchSqlValues(PreparedStatement stmt, FileSearch search, int pCounter) throws SQLException {
			boolean isStringSearch = !ObjectTransformer.isEmpty(search.getSearchString());

			if (isStringSearch) {
				String pattern_nolike = search.getSearchString();
				String pattern_like = "%" + search.getSearchString() + "%";

				// add search term for id, name, description, nice_url, contentfile_alt_url.url
				stmt.setObject(pCounter++, pattern_nolike);
				stmt.setObject(pCounter++, pattern_like);
				stmt.setObject(pCounter++, pattern_like);
				stmt.setObject(pCounter++, pattern_like);
				stmt.setObject(pCounter++, pattern_like);
			}

			List<SystemUser> editors = search.getEditors();

			if (editors != null) {
				for (SystemUser editor : editors) {
					stmt.setObject(pCounter++, editor.getId());
				}
			}

			List<SystemUser> creators = search.getCreators();

			if (creators != null) {
				for (SystemUser creator : creators) {
					stmt.setObject(pCounter++, creator.getId());
				}
			}

			if (search.getEditedBefore() > 0) {
				stmt.setObject(pCounter++, search.getEditedBefore());
			}
			if (search.getEditedSince() > 0) {
				stmt.setObject(pCounter++, search.getEditedSince());
			}
			if (search.getCreatedBefore() > 0) {
				stmt.setObject(pCounter++, search.getCreatedBefore());
			}
			if (search.getCreatedSince() > 0) {
				stmt.setObject(pCounter++, search.getCreatedSince());
			}

			if (!StringUtils.isEmpty(search.getNiceUrlSearch())) {
				stmt.setObject(pCounter++, search.getNiceUrlSearch());
				stmt.setObject(pCounter++, search.getNiceUrlSearch());
			}

			if (!StringUtils.isEmpty(search.getMimeType())) {
				stmt.setObject(pCounter++, search.getMimeType());
			}

			return pCounter;
		}

		/**
		 * Get the form IDs of given type for the search
		 * @param search search
		 * @return list of object IDs
		 * @throws NodeException
		 */
		private Collection<Integer> getFormIds(FormSearch search) throws NodeException {
			if (search == null) {
				search = FormSearch.create();
			}
			if (!search.isEmpty()) {
				return loadFormIds(search);
			} else {
				return formIds.get(getNode().getId(), search.getWastebin(), (nodeId, wb) -> {
					return loadFormIds(FormSearch.create().setWastebin(wb));
				});
			}
		}

		/**
		 * Load ids for forms
		 * @param search search
		 * @throws NodeException
		 */
		private Collection<Integer> loadFormIds(FormSearch search) throws NodeException {
			if (search == null) {
				search = FormSearch.create();
			}
			final FormSearch fSearch = search;
			int userId = TransactionManager.getCurrentTransaction().getUserId();

			List<Integer> folderIds = new Vector<Integer>();

			folderIds.add(ObjectTransformer.getInteger(getMaster().getId(), 0));

			if (fSearch.isRecursive()) {
				collectSubfolderIds(folderIds, Form.TYPE_FORM, PermHandler.PERM_VIEW);
			}

			StringBuffer sql = new StringBuffer("SELECT id FROM form WHERE folder_id IN (");
			sql.append(StringUtils.repeat("?", folderIds.size(), ","));
			sql.append(")");

			if (!StringUtils.isEmpty(fSearch.getSearchString())) {
				sql.append("AND (id = ? OR LOWER(name) LIKE ? OR LOWER(description) LIKE ?)");
			}

			if (search.isCreator()) {
				sql.append(" AND creator = ?");
			}
			if (search.isEditor()) {
				sql.append(" AND editor = ?");
			}
			if (search.isPublisher()) {
				sql.append(" AND publisher = ?");
			}

			List<SystemUser> editors = search.getEditors();

			if (editors != null) {
				if (ObjectTransformer.isEmpty(editors)) {
					return Collections.emptyList();
				}
				sql.append(" AND editor IN (");
				sql.append(StringUtils.repeat("?", editors.size(), ","));
				sql.append(")");
			}
			List<SystemUser> creators = search.getCreators();

			if (creators != null) {
				if (ObjectTransformer.isEmpty(creators)) {
					return Collections.emptyList();
				}
				sql.append(" AND creator IN (");
				sql.append(StringUtils.repeat("?", creators.size(), ","));
				sql.append(")");
			}
			List<SystemUser> publishers = search.getPublishers();

			if (publishers != null) {
				if (ObjectTransformer.isEmpty(publishers)) {
					return Collections.emptyList();
				}
				sql.append(" AND publisher IN (");
				sql.append(StringUtils.repeat("?", publishers.size(), ","));
				sql.append(")");
			}

			if (search.getEditedBefore() > 0) {
				sql.append(" AND edate <= ?");
			}
			if (search.getEditedSince() > 0) {
				sql.append(" AND edate >= ?");
			}
			if (search.getCreatedBefore() > 0) {
				sql.append(" AND cdate <= ?");
			}
			if (search.getCreatedSince() > 0) {
				sql.append(" AND cdate >= ?");
			}
			if (search.getPublishedBefore() > 0) {
				sql.append(" AND pdate > 0 AND pdate <= ?");
			}
			if (search.getPublishedSince() > 0) {
				sql.append(" AND pdate >= ?");
			}

			if (search.getModified() != null) {
				sql.append(" AND modified = ?");
			}
			if (search.getOnline() != null) {
				sql.append(" AND online = ?");
			}

			sql.append(fSearch.getWastebin().filterClause("form"));

			return DBUtils.select(sql.toString(), st -> {
				int pCounter = 1;
				for (Integer fId : folderIds) {
					st.setObject(pCounter++, fId);
				}

				if (!StringUtils.isEmpty(fSearch.getSearchString())) {
					String pattern = "%" + fSearch.getSearchString() + "%";
					st.setString(pCounter++, fSearch.getSearchString()); // id = ?
					st.setString(pCounter++, pattern); // name LIKE ?
					st.setString(pCounter++, pattern); // description LIKE ?
				}

				if (fSearch.isCreator()) {
					st.setInt(pCounter++, userId); // creator = ?
				}
				if (fSearch.isEditor()) {
					st.setInt(pCounter++, userId); // editor = ?
				}
				if (fSearch.isPublisher()) {
					st.setInt(pCounter++, userId); // publisher = ?
				}

				if (editors != null) {
					for (SystemUser editor : editors) {
						st.setInt(pCounter++, editor.getId());
					}
				}
				if (creators != null) {
					for (SystemUser creator : creators) {
						st.setInt(pCounter++, creator.getId());
					}
				}
				if (publishers != null) {
					for (SystemUser publisher : publishers) {
						st.setInt(pCounter++, publisher.getId());
					}
				}

				if (fSearch.getEditedBefore() > 0) {
					st.setInt(pCounter++, fSearch.getEditedBefore());
				}
				if (fSearch.getEditedSince() > 0) {
					st.setInt(pCounter++, fSearch.getEditedSince());
				}
				if (fSearch.getCreatedBefore() > 0) {
					st.setInt(pCounter++, fSearch.getCreatedBefore());
				}
				if (fSearch.getCreatedSince() > 0) {
					st.setInt(pCounter++, fSearch.getCreatedSince());
				}
				if (fSearch.getPublishedBefore() > 0) {
					st.setInt(pCounter++, fSearch.getPublishedBefore());
				}
				if (fSearch.getPublishedSince() > 0) {
					st.setInt(pCounter++, fSearch.getPublishedSince());
				}

				if (fSearch.getModified() != null) {
					st.setBoolean(pCounter++, fSearch.getModified());
				}
				if (fSearch.getOnline() != null) {
					st.setBoolean(pCounter++, fSearch.getOnline());
				}
			}, DBUtils.IDLIST);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Folder {" + getName() + ", " + getId() + "}";
		}

		public ContentNodeDate getCDate() {
			return cDate;
		}

		public ContentNodeDate getEDate() {
			return eDate;
		}

		public SystemUser getCreator() throws NodeException {
			SystemUser creator = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);

			// check for data consistency
			assertNodeObjectNotNull(creator, creatorId, "Creator");
			return creator;
		}

		public SystemUser getEditor() throws NodeException {
			SystemUser editor = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);

			// check for data consistency
			assertNodeObjectNotNull(editor, editorId, "Editor");
			return editor;
		}

		@Override
		public List<File> getFiles(FileSearch search) throws NodeException {
			return loadFilesAndImages(File.class, LOAD_FILES, search);
		}

		@Override
		public List<ImageFile> getImages(FileSearch search) throws NodeException {
			return loadFilesAndImages(ImageFile.class, LOAD_IMAGES, search);
		}

		@Override
		public List<File> getFilesAndImages(FileSearch search) throws NodeException {
			return loadFilesAndImages(File.class, LOAD_FILESANDIMAGES, search);
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#setDescription(java.lang.String)
		 */
		public String setDescription(String description) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#setMother(com.gentics.contentnode.object.Folder)
		 */
		public Integer setMotherId(Integer folderId) throws NodeException, ReadOnlyException {
			failReadOnly();
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#setName(java.lang.String)
		 */
		public String setName(String name) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		@Override
		public void setNodeId(Integer id) throws ReadOnlyException, NodeException {
			failReadOnly();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#setPublishDir(java.lang.String)
		 */
		public String setPublishDir(String pubDir) throws ReadOnlyException, NodeException {
			failReadOnly();
			return null;
		}

		@Override
		public void setTemplates(List<Template> templates) throws ReadOnlyException, NodeException {
			failReadOnly();
		}

		@Override
		public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException {
			failReadOnly();
		}

		@Override
		public void setChannelMaster(Folder folder) throws ReadOnlyException, NodeException {
			failReadOnly();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.LocalizableNodeObject#modifyChannelId(java.lang.Integer)
		 */
		public void modifyChannelId(Integer channelId) throws ReadOnlyException,
					NodeException {
			failReadOnly();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			Class clazz = getObjectInfo().getObjectClass();

			return new EditableFactoryFolder(this, getFactory().getFactoryHandle(clazz).createObjectInfo(clazz, true), true);
		}

		@Override
		public Page getStartpage() {
			try {
				Transaction t = TransactionManager.getCurrentTransaction();

				NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

				// default startpage property name
				String startPagePropName = prefs.getProperty("folder_startpage_objprop_name");

				// get "per node" configuration
				Map startPagePerNode = prefs.getPropertyMap("folder_startpage_objprop_per_node");

				// check whether the property is configured for the node, and
				// take the default value if not
				if (startPagePerNode != null && startPagePerNode.containsKey(getNode().getId())) {
					startPagePropName = ObjectTransformer.getString(startPagePerNode.get(getNode().getId()), startPagePropName);
				}

				// if there is no startpage property there is no startpage
				if (StringUtils.isEmpty(startPagePropName)) {
					return null;
				}

				// strip unwanted "object."
				if (startPagePropName.startsWith("object.")) {
					startPagePropName = startPagePropName.substring(7);
				}

				// get the objecttag
				ObjectTag startPageTag = getObjectTags().get(startPagePropName);

				if (startPageTag == null) {
					return null;
				}

				// find the first value of type PageUrlPartType
				ValueList values = startPageTag.getValues();
				Value urlValue = null;

				for (Value v : values) {
					if (v.getPartType() instanceof PageURLPartType) {
						urlValue = v;
						break;
					}
				}

				if (urlValue != null) {
					return (Page) t.getObject(Page.class, new Integer(urlValue.getValueRef()));
				} else {
					return null;
				}
			} catch (Exception e) {
				logger.error("Unable to retrieve startpage for folder {" + toString() + "}", e);
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#isInherited()
		 */
		public boolean isInherited() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
				// multichannelling is not used, so the folder cannot be inherited
				return false;
			}

			// determine the current channel
			Node channel = t.getChannel();

			if (channel == null || !channel.isChannel()) {
				return false;
			}

			// Check if the current channel is actually a channel of the folder's owning node
			if (!channel.isChannelOf(getOwningNode())) {
				return false;
			}

			// the folder is inherited if its channelid is different from the current channel
			return ObjectTransformer.getInt(channel.getId(), -1) != ObjectTransformer.getInt(this.channelId, -1);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#isMaster()
		 */
		public boolean isMaster() throws NodeException {
			return master;
		}

		@Override
		public Map<Integer, Integer> getChannelSet() throws NodeException {
			Map<Wastebin, Map<Integer, Integer>> cSet = loadChannelSet();

			return new HashMap<>(cSet.get(TransactionManager.getCurrentTransaction().getWastebinFilter()));
		}

		/**
		 * Internal method to load the channelset
		 * @return channelset
		 * @throws NodeException
		 */
		protected synchronized Map<Wastebin, Map<Integer, Integer>> loadChannelSet() throws NodeException {
			if (!isEmptyId(channelSetId) && MultichannellingFactory.isEmpty(channelSet)) {
				channelSet = null;
			}
			// check for incomplete channelsets
			if (channelSet != null && !channelSet.keySet().containsAll(Arrays.asList(Wastebin.INCLUDE, Wastebin.EXCLUDE, Wastebin.ONLY))) {
				channelSet = null;
			}
			if (channelSet == null) {
				channelSet = MultichannellingFactory.loadChannelset(getObjectInfo().getObjectClass(), channelSetId);
			}
			return channelSet;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#getChannel()
		 */
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
			try (NoMcTrx noMc = new NoMcTrx()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (isChannelRoot()) {
				return getNode().getMaster();
			} else {
				return t.getObject(Node.class, nodeId, -1, false);
			}
		}
		}

		@Override
		public boolean hasChannel() throws NodeException {
			return !isEmptyId(channelId);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#getChannelSetId(boolean)
		 */
		public Integer getChannelSetId() throws NodeException {
			if (isEmptyId(channelSetId)) {
				throw new NodeException(this + " does not have a valid channelset_id");
			}
			return channelSetId;
		}

		@Override
		public int getChildFoldersCount() throws NodeException {
			switch (TransactionManager.getCurrentTransaction().getWastebinFilter()) {
			case EXCLUDE:
			case ONLY:
				return getChildFolders().size();
			case INCLUDE:
			default:
				return loadChildFolderIds().size();
			}
		}

		@Override
		public int getFilesCount() throws NodeException {
			switch (TransactionManager.getCurrentTransaction().getWastebinFilter()) {
			case EXCLUDE:
			case ONLY:
				return getFiles().size();
			case INCLUDE:
			default:
				return getFileIds(LOAD_FILES, null).size();
			}
		}

		@Override
		public int getImagesCount() throws NodeException {
			switch (TransactionManager.getCurrentTransaction().getWastebinFilter()) {
			case EXCLUDE:
			case ONLY:
				return getImages().size();
			case INCLUDE:
			default:
				return getFileIds(LOAD_IMAGES, null).size();
			}
		}

		@Override
		public int getPagesCount() throws NodeException {
			// TODO do language fallback and count individual pages
			return getPageIds(null).size();
		}

		@Override
		public int getTemplatesCount() throws NodeException {
			return getTemplateIds(null).size();
		}

		@Override
		public boolean isExcluded() {
			return excluded;
		}

		@Override
		public void setExcluded(boolean value) throws ReadOnlyException {
			this.excluded = value;
		}

		/**
		 * Indicates whether this folder will be disinherited by default in
		 * newly created channels.
		 *
		 * @return <code>true</code> if this folder will be disinherited in
		 *		new channels.
		 * @throws NodeException On internal errors
		 */
		@Override
		public boolean isDisinheritDefault() throws NodeException {
			if (isMaster()) {
				return this.disinheritDefault;
			} else {
				var master = getMaster();

				return this != master && master.isDisinheritDefault();
			}
		}

		/**
		 * Set whether this folder should be disinherited by default in
		 * newly created channels.
		 *
		 * If <code>value</code> is <code>true</code> the change will be
		 * propagated to all ancestor objects regardles of the value of
		 * <code>recursive</code>.
		 *
		 * @see DisinheritUtils#updateDisinheritDefault
		 *
		 * @param value Set to <code>true</code> if this folder should be
		 *		disinherited in new channels.
		 * @param recursive If set to <code>true</code> the same value for
		 *		the {@link #disinheritDefault} flag will be set to all
		 *		ancestors of this folder.
		 */
		@Override
		public void setDisinheritDefault(boolean value, boolean recursive) throws NodeException {
			if (!isMaster()) {
				return;
			}

			DisinheritUtils.updateDisinheritDefault(this, value);

			disinheritDefault = value;
			recursive |= value;

			if (!recursive) {
				return;
			}

			Map<Integer, Set<Integer>> subObjects = getSubObjects();
			Transaction t = TransactionManager.getCurrentTransaction();

			if (subObjects.containsKey(Folder.TYPE_FOLDER)) {
				Set<Integer> folderObjects = subObjects.get(Folder.TYPE_FOLDER);
				List<Folder> folders = t.getObjects(Folder.class, folderObjects, false, false);

				for (Folder folder: folders) {
					folder.setDisinheritDefault(value, true);
				}
			}

			Set<Disinheritable<?>> otherObjects = new HashSet<>();

			if (subObjects.containsKey(Page.TYPE_PAGE)) {
				otherObjects.addAll(t.getObjects(Page.class, subObjects.get(Page.TYPE_PAGE), false, false));
			}

			if (subObjects.containsKey(ContentFile.TYPE_FILE)) {
				otherObjects.addAll(t.getObjects(File.class, subObjects.get(File.TYPE_FILE), false, false));
			}

			for (Disinheritable<?> o : otherObjects) {
				o.setDisinheritDefault(value, true);
			}
		}

		@Override
		public Set<Node> getDisinheritedChannels() throws NodeException {
			return DisinheritUtils.loadDisinheritedChannelsInternal(this);
		}

		@Override
		public Set<Integer> getOriginalDisinheritedNodeIds() {
			return disinheritedChannelIds;
		}

		@Override
		public void setOriginalDisinheritedNodeIds(Set<Integer> nodeIds) {
			this.disinheritedChannelIds = nodeIds;
		}

		@Override
		public void changeMultichannellingRestrictions(boolean excluded, Set<Node> disinheritedNodes, boolean recursive) throws NodeException {
			changeMultichannellingRestrictions(excluded, disinheritedNodes, recursive ? RecursiveMCRestrictions.set : RecursiveMCRestrictions.check);
		}

		/**
		 * Creates a map of object types to the ids of all subobjects of this
		 * folder in all channels.
		 *
		 * @see DisinheritUtils#getChannelIndependentFolderChildren
		 *
		 * @return A map of object types to the ids of all subobjects of this
		 *		folder in all channels.
		 *
		 * @throws NodeException On error.
		 */
		private Map<Integer, Set<Integer>> getSubObjects() throws NodeException {
			Map<DisinheritableObjectReference, DisinheritableObjectData> children = DisinheritUtils.getChannelIndependentFolderChildren(this);
			Map<Integer, Set<Integer>> subObjects = new HashMap<>();

			for (DisinheritableObjectReference key : children.keySet()) {
				Integer type = key.getType();

				if (!subObjects.containsKey(type)) {
					subObjects.put(type, new HashSet<Integer>());
				}

				subObjects.get(type).add(key.getId());
			}

			return subObjects;
		}

		/**
		 * Implementation of changeMultichannellingRestrictions(boolean, Set&lt;Node>)
		 * @param excluded whether the folder should be excluded
		 * @param disinheritedNodes set of nodes the folder should disinherit
		 * @param rec setting for handling recursion
		 * @throws NodeException
		 */
		private void changeMultichannellingRestrictions(boolean excluded, Set<Node> disinheritedNodes, RecursiveMCRestrictions rec) throws NodeException {
			if (!isMaster()) {
				return;
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			ChannelTreeSegment originalSegment = new ChannelTreeSegment(this, true);
			ChannelTreeSegment newSegment = new ChannelTreeSegment(this.getMaster().getOwningNode(), excluded,  disinheritedNodes);
			@SuppressWarnings("unchecked")
			Set<Node> netDisinheritedChannels = new HashSet<>(ListUtils.removeAll(originalSegment.getAllNodes(), newSegment.getAllNodes()));
			Map<Integer, Set<Integer>> subObjects = getSubObjects();

			if (!netDisinheritedChannels.isEmpty()) {
				if (rec == RecursiveMCRestrictions.check) {
					try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
						DisinheritUtils.checkChangeConsistency(this, excluded, disinheritedNodes, false);
					}
				}

				if (subObjects.containsKey(Folder.TYPE_FOLDER)) {
					Set<Integer> folderObjects = subObjects.get(Folder.TYPE_FOLDER);
					List<Folder> folders = t.getObjects(Folder.class, folderObjects, false, false);
					for (Folder f : folders) {
						ChannelTreeSegment newRestrictions = new ChannelTreeSegment(f, true).addRestrictions(excluded, disinheritedNodes);
						((FactoryFolder) f).changeMultichannellingRestrictions(newRestrictions.isExcluded(), newRestrictions.getRestrictions(), RecursiveMCRestrictions.skip);
					}
				}

				Set<Disinheritable<?>> otherObjects = new HashSet<>();
				if (subObjects.containsKey(Page.TYPE_PAGE)) {
					otherObjects.addAll(t.getObjects(Page.class, subObjects.get(Page.TYPE_PAGE), false, false));
				}
				if (subObjects.containsKey(ContentFile.TYPE_FILE)) {
					otherObjects.addAll(t.getObjects(File.class, subObjects.get(File.TYPE_FILE), false, false));
				}
				for (Disinheritable<?> o : otherObjects) {
					ChannelTreeSegment newRestrictions = new ChannelTreeSegment(o, true).addRestrictions(excluded, disinheritedNodes);
					o.changeMultichannellingRestrictions(newRestrictions.isExcluded(), newRestrictions.getRestrictions(), false);
				}
			}

			DisinheritUtils.updateDisinheritedNodeAssociations(this, excluded, disinheritedNodes, rec != RecursiveMCRestrictions.check);

			if (rec == RecursiveMCRestrictions.set) {
				if (subObjects.containsKey(Folder.TYPE_FOLDER)) {
					Set<Integer> folderObjects = subObjects.get(Folder.TYPE_FOLDER);
					List<Folder> folders = t.getObjects(Folder.class, folderObjects, false, false);
					for (Folder f : folders) {
						((FactoryFolder) f).changeMultichannellingRestrictions(excluded, disinheritedNodes, RecursiveMCRestrictions.set);
					}
				}

				Set<Disinheritable<?>> otherObjects = new HashSet<>();
				if (subObjects.containsKey(Page.TYPE_PAGE)) {
					otherObjects.addAll(t.getObjects(Page.class, subObjects.get(Page.TYPE_PAGE), false, false));
				}
				if (subObjects.containsKey(ContentFile.TYPE_FILE)) {
					otherObjects.addAll(t.getObjects(File.class, subObjects.get(File.TYPE_FILE), false, false));
				}
				for (Disinheritable<?> o : otherObjects) {
					o.changeMultichannellingRestrictions(excluded, disinheritedNodes, false);
				}
			}

			for (Form form : getForms(null)) {
				form.folderInheritanceChanged();
			}
		}

		@Override
		public boolean isDeleted() {
			return deleted > 0;
		}

		@Override
		public int getDeleted() {
			return deleted;
		}

		@Override
		public SystemUser getDeletedBy() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(SystemUser.class, deletedBy);
		}

		@Override
		public void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId) throws NodeException {
			super.triggerEvent(object, property, eventMask, depth, channelId);

			// when the folder's pub_dir changes, we need to dirt all pages that are present in the publish table for the folder
			Map<Integer, List<Integer>> pageIdsInChannel = new HashMap<>();
			if (Events.isEvent(eventMask, Events.UPDATE) && !ObjectTransformer.isEmpty(property) && Arrays.asList(property).contains("pub_dir")) {
				Transaction t = TransactionManager.getCurrentTransaction();

				DBUtils.executeStatement("SELECT node_id, page_id FROM publish WHERE folder_id = ? AND active = ?", Transaction.SELECT_STATEMENT, p -> {
					p.setInt(1, getId());
					p.setInt(2, 1);
				}, rs -> {
					while (rs.next()) {
						int nodeId = rs.getInt("node_id");
						int pageId = rs.getInt("page_id");
						pageIdsInChannel.computeIfAbsent(nodeId, key -> new ArrayList<>()).add(pageId);
					}
				});

				for (Map.Entry<Integer, List<Integer>> entry : pageIdsInChannel.entrySet()) {
					int nodeId = entry.getKey();
					for (Page page : t.getObjects(Page.class, entry.getValue())) {
						// dirt the page with a dummy attribute, so that the content will not be rendered
						PublishQueue.dirtObject(page, Action.DEPENDENCY, nodeId, DUMMY_DIRT_ATTRIBUTE);
					}
				}
			}
		}

		@Override
		public I18nMap getNameI18n() {
			return nameI18n;
	}

		@Override
		public I18nMap getDescriptionI18n() {
			return descriptionI18n;
		}

		@Override
		public I18nMap getPublishDirI18n() {
			return publishDirI18n;
		}

		@Override
		public List<ExtensibleObjectService<Folder>> getServices() {
			return StreamSupport.stream(folderFactoryServiceLoader.spliterator(), false).collect(Collectors.toList());
		}
	}

	/**
	 * editable folder
	 */
	private static class EditableFactoryFolder extends FactoryFolder implements ExtensibleObject<Folder> {

		private static final long serialVersionUID = 2377749588272165702L;

		/**
		 * true if the object has been modified
		 */
		private boolean modified = false;

		/**
		 * Flag to mark whether the channelset of this page was changed, or not
		 */
		private boolean channelSetChanged = false;

		/**
		 * list of templates currently linked to this folder
		 */
		private List<Template> templates = null;

		/**
		 * editable copies of the objecttags of this folder
		 */
		private Map<String, ObjectTag> objectTags = null;

		/**
		 * Node owning this folder
		 */
		private Node owningNode;

		/**
		 * Create an editable copy of the given folder
		 * @param folder folder
		 * @param info object info
		 * @param asNewFolder true when the editable copy shall represent a new
		 *        folder, false if it shall be the editable version of the same
		 *        folder
		 * @throws NodeException
		 */
		protected EditableFactoryFolder(FactoryFolder folder, NodeObjectInfo info, boolean asNewFolder) throws NodeException {
			super(asNewFolder ? null : folder.getId(), info, folder.name, folder.description, folder.motherId, folder.nodeId, folder.pubDir, asNewFolder ? null
					: folder.objectTagIds, folder.cDate, folder.eDate, folder.creatorId, folder.editorId, folder.masterId, asNewFolder ? 0
					: folder.channelSetId, folder.channelId, folder.master, folder.excluded, folder.disinheritDefault, asNewFolder ? 0 : folder.deleted, asNewFolder ? 0
					: folder.deletedBy, asNewFolder ? -1 : folder.getUdate(), asNewFolder ? null : folder.getGlobalId());

			// copy over the i18n data
			nameI18n.putAll(folder.nameI18n);
			descriptionI18n.putAll(folder.descriptionI18n);
			publishDirI18n.putAll(folder.publishDirI18n);

			if (asNewFolder) {
				// copy the objecttags
				Map<String, ObjectTag> originalObjectTags = folder.getObjectTags();

				objectTags = new HashMap<String, ObjectTag>(originalObjectTags.size());
				for (Iterator<Map.Entry<String, ObjectTag>> i = originalObjectTags.entrySet().iterator(); i.hasNext();) {
					Map.Entry<String, ObjectTag> entry = i.next();

					objectTags.put(entry.getKey(), (ObjectTag) entry.getValue().copy());
				}

				// set the templates
				templates = new Vector<Template>(folder.getTemplates());
				modified = true;
			} else {
				// get the objecttags (which will also generate new objtag instances if necessary)
				getObjectTags();
			}
		}

		/**
		 * Create a new instance
		 * @param info object info
		 * @throws NodeException
		 */
		protected EditableFactoryFolder(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
			templates = new ArrayList<Template>();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#save()
		 */
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			// first check whether the file is editable
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();
			boolean isModified = modified;
			boolean isNew = AbstractFolder.isEmptyId(getId());

			// now check whether the object has been modified
			if (isModified) {
				// object is modified, so update it
				saveFolderObject(this);
				modified = false;
			}

			if (isNew && getMother() != null ) {
				if (isNew) {
					Disinheritable<?> restrictionSource = isMaster() ? getMother().getMaster() : getMaster();
					DisinheritUtils.saveNewDisinheritedAssociations(this, restrictionSource.isExcluded(), restrictionSource.getDisinheritedChannels());
					setDisinheritDefault(
						disinheritDefault || restrictionSource.isDisinheritDefault(),
						false);
				}
			}

			// save the template assignment (even if the folder itself was not changed)
			isModified |= saveTemplateAssignment(isNew, this);

			// save the object tags
			Map<String, ObjectTag> objTags = getObjectTags();
			List<Integer> tagIdsToRemove = new Vector<Integer>();

			if (objectTagIds != null) {
				tagIdsToRemove.addAll(objectTagIds);
			}
			for (ObjectTag tag : objTags.values()) {
				tag.setNodeObject(this);
				isModified |= tag.save();

				// do not remove the tag, which was saved
				tagIdsToRemove.remove(tag.getId());
			}

			// eventually remove tags which no longer exist
			if (!tagIdsToRemove.isEmpty()) {
				List<ObjectTag> tagsToRemove = t.getObjects(ObjectTag.class, tagIdsToRemove);

				for (Iterator<ObjectTag> i = tagsToRemove.iterator(); i.hasNext();) {
					NodeObject tagToRemove = i.next();

					tagToRemove.delete();
				}
				isModified = true;
			}

			// dirt the folder cache
			if (isModified) {
				t.dirtObjectCache(Folder.class, getId(), true);
			}

			// if the channelset changed, we need to dirt all other folders of the channelset as well
			if (channelSetChanged || MultichannellingFactory.isEmpty(channelSet)) {
				channelSet = null;
				Map<Integer, Integer> locChannelSet = getChannelSet();

				// dirt caches for all pages in the channelset
				for (Map.Entry<Integer, Integer> channelSetEntry : locChannelSet.entrySet()) {
					t.dirtObjectCache(Folder.class, channelSetEntry.getValue());
				}

				channelSetChanged = false;
			}

			if (isModified) {
				onSave(this, isNew, false, t.getUserId());
			}

			if (isNew) {
				updateMissingReferences();
			}

			return isModified;
		}

		/**
		 * Save the given folder to the DB
		 * @param folder folder object to be saved
		 * @throws NodeException
		 */
		private static void saveFolderObject(EditableFactoryFolder folder) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			NodePreferences nodePreferences = t.getNodeConfig().getDefaultPreferences();

			Map<String, String> sanitizeCharacters = nodePreferences.getPropertyMap("sanitize_character");
			String replacementChararacter = nodePreferences.getProperty("sanitize_replacement_character");
			String[] preservedCharacters = nodePreferences.getProperties("sanitize_allowed_characters");
			// make sure that the folder has a channelset_id set
			folder.getChannelSetId();

			// prepare the object IDs of the folders siblings (for uniqueness checks)
			ChannelTreeSegment channelTreeSegment = new ChannelTreeSegment(folder, false);
			Map<Node, MultiChannellingFallbackList> siblingsFallback = DisinheritUtils.getSiblings(folder, channelTreeSegment);
			Set<Integer> objectIds = siblingsFallback.values().stream().flatMap(fb -> fb.getObjectIds().stream()).collect(Collectors.toSet());

			boolean multiChannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);
			boolean isNew = AbstractFolder.isEmptyId(folder.getId());

			Folder orgFolder = null;

			// make sure, that the channelId is set correctly
			if (isNew && folder.master && !folder.isRoot()) {
				folder.channelId = MultichannellingFactory.correctChannelId(ObjectTransformer.getInt(folder.motherId, 0), folder.channelId);
			}

			if (!StringUtils.isEmpty(folder.description)) {
				folder.description = folder.description.substring(0, Math.min(folder.description.length(), Folder.MAX_DESCRIPTION_LENGTH));
			}

			if (!isNew) {
				// Get the original file for comparision
				orgFolder = t.getObject(Folder.class, folder.getId());
			}

			// set the type of the folder
			Folder mother = folder.getMother();
			int type = mother != null ? TYPE_FOLDER : (folder.isMaster() ? Node.TYPE_NODE : Node.TYPE_CHANNEL);

			if (!isNew || type == TYPE_FOLDER) {
				if (!ObjectTransformer.getBoolean(t.getAttributes().get(OMIT_PUB_DIR_SEGMENT_VERIFY), false)) {
					if (folder.getNode().isPubDirSegment()) {
						if (!StringUtils.isEmpty(folder.pubDir)) {
							folder.pubDir = FileUtil.sanitizeName(folder.pubDir, sanitizeCharacters, replacementChararacter, preservedCharacters);
						}
						for (Entry<Integer, String> entry : folder.publishDirI18n.entrySet()) {
							entry.setValue(FileUtil.sanitizeName(entry.getValue(), sanitizeCharacters, replacementChararacter, preservedCharacters));
						}

						if (folder.getMother() != null) {
							t.getAttributes().put(OMIT_PUB_DIR_SEGMENT_VERIFY, true);
							try {
								folder.setPublishDir(UniquifyHelper.makeUnique(folder, folder.pubDir, PUB_DIR_FUNCTION,
										objectIds, SeparatorType.underscore, Folder.MAX_PUB_DIR_LENGTH));

								// make the translated publish directories unique among each other
								folder.setPublishDirI18n(UniquifyHelper.makeUnique(folder.getOwningNode().getLanguages(), folder.publishDirI18n,
										folder.pubDir, Optional.ofNullable(orgFolder).map(Folder::getPublishDirI18n), SeparatorType.underscore,
										Folder.MAX_PUB_DIR_LENGTH));

								// and then make them unique with all other
								folder.setPublishDirI18n(UniquifyHelper.makeUnique(folder, folder.publishDirI18n,
										PUB_DIR_FUNCTION, objectIds, SeparatorType.underscore, Folder.MAX_PUB_DIR_LENGTH));
							} finally {
								t.getAttributes().remove(OMIT_PUB_DIR_SEGMENT_VERIFY);
						}
						}
					} else {
						folder.pubDir = FileUtil.sanitizeFolderPath(folder.pubDir, sanitizeCharacters, replacementChararacter, preservedCharacters);
						for (Entry<Integer, String> entry : folder.publishDirI18n.entrySet()) {
							entry.setValue(FileUtil.sanitizeFolderPath(entry.getValue(), sanitizeCharacters, replacementChararacter, preservedCharacters));
					}
				}
			}
			}

			// check whether changing the publish directory is possible
			// TODO do this also for the translated publish directories
			if (orgFolder != null) {
				if (!StringUtils.isEqual(orgFolder.getPublishDir(), folder.getPublishDir())) {
					if (!DisinheritUtils.checkPubDirChangeConsistency(folder, folder.getPublishDir())) {
						throw new ObjectModificationException("publishDir", "Publish directory of " + folder + " cannot be changed to '"
								+ folder.getPublishDir() + "' because this would cause duplicate filenames", "the_publishing_directory");
					}
				}
			}

			// Check & add numbers until the folder name is unique
			if (!StringUtils.isEmpty(folder.name)) {
				folder.name = folder.name.trim();
				folder.name = folder.name.substring(0, Math.min(folder.name.length(), Folder.MAX_NAME_LENGTH));
			}

			if (mother != null) {
				folder.setName(UniquifyHelper.makeUnique(folder, folder.name, NAME_FUNCTION, objectIds, SeparatorType.none,
						Folder.MAX_NAME_LENGTH));
				folder.setNameI18n(UniquifyHelper.makeUnique(folder, folder.nameI18n, NAME_FUNCTION, objectIds, SeparatorType.none, Folder.MAX_NAME_LENGTH));
			} else {
				// make name of root folder unique
				int folderId = ObjectTransformer.getInt(folder.getId(), -1);
				Set<Integer> otherRootFolderIds = DBUtils.select("SELECT id FROM folder WHERE mother = 0 AND id != ? AND deleted = 0",
						ps -> ps.setInt(1, folderId), DBUtils.IDS);
				folder.setName(UniquifyHelper.makeUnique(folder, folder.name, NAME_FUNCTION, otherRootFolderIds, SeparatorType.none, Folder.MAX_NAME_LENGTH));
				folder.setNameI18n(UniquifyHelper.makeUnique(folder, folder.nameI18n, NAME_FUNCTION, otherRootFolderIds, SeparatorType.none, Folder.MAX_NAME_LENGTH));
			}

			// make sure that the motherId is set properly
			if (folder.motherId == null) {
				folder.motherId = 0;
			}

			// set editor data
			folder.editorId = t.getUserId();
			folder.eDate = new ContentNodeDate(t.getUnixTimestamp());

			// insert new folder
			if (isNew) {
				// set creator data
				folder.creatorId = t.getUserId();
				folder.cDate = new ContentNodeDate(t.getUnixTimestamp());

				List<Integer> keys = DBUtils.executeInsert(
						"INSERT INTO folder (mother, name, type_id, pub_dir, node_id, creator, "
								+ "cdate, editor, edate, description, channelset_id, channel_id, is_master, master_id, mc_exclude, disinherit_default, uuid) " + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
								new Object[] {
					folder.motherId, folder.getName(), type, folder.getPublishDir(), folder.nodeId, folder.creatorId, folder.cDate.getIntTimestamp(),
					folder.editorId, folder.eDate.getIntTimestamp(), folder.getDescription(), folder.channelSetId, folder.channelId, folder.master,
					ObjectTransformer.getInt(folder.masterId, 0), folder.excluded, folder.disinheritDefault, ObjectTransformer.getString(folder.getGlobalId(), "")});

				if (keys.size() == 1) {
					// set the new file id
					folder.setId(keys.get(0));
					synchronizeGlobalId(folder);
					if (folder.isMaster() && mother != null) {
						// copy the permissions from the root folder
						PermHandler.duplicatePermissions(mother.isRoot() ? Node.TYPE_NODE : Folder.TYPE_FOLDER, ObjectTransformer.getInt(mother.getId(), -1),
								Folder.TYPE_FOLDER, ObjectTransformer.getInt(folder.getId(), -1));
					}
				} else {
					throw new NodeException("Error while inserting new folder, could not get the insertion id");
				}

				saveI18nData(folder);

				ActionLogger.logCmd(ActionLogger.CREATE, Folder.TYPE_FOLDER, folder.getId(), folder.nodeId, "cmd_folder_create-java");
				t.addTransactional(new TransactionalTriggerEvent(Folder.class, folder.getId(), null, Events.CREATE));

				// when the folder is a localized copy, we will "delete" all master objects from this channel and all channels below
				if (!folder.isMaster() && mother != null) {
					hideFormerInheritedObjects(Folder.TYPE_FOLDER, folder.getId(), folder.getChannel(), folder.getChannelSet());
				}
			} else {
				// update existing folder
				DBUtils.executeUpdate(
						"UPDATE folder SET mother = ?, name = ?, pub_dir = ?, node_id = ?, editor = ?, "
								+ "edate = ?, description = ?, channelset_id = ?, channel_id = ? WHERE id = ?",
								new Object[] {
					folder.motherId, folder.getName(), folder.getPublishDir(), folder.nodeId, folder.editorId, folder.eDate.getIntTimestamp(),
					folder.getDescription(), folder.channelSetId, folder.channelId, folder.getId()});

				saveI18nData(folder);

				ActionLogger.logCmd(ActionLogger.EDIT, Folder.TYPE_FOLDER, folder.getId(), 0, "cmd_folder_update-java");

				// Find all attributes that have changed between both folders
				List<String> attributes = FolderFactory.getChangedProperties(orgFolder, folder);

				t.addTransactional(new TransactionalTriggerEvent(Folder.class, folder.getId(), (String[]) attributes.toArray(new String[0]), Events.UPDATE));
			}
		}

		/**
		 * Save additional i18n data for the given folder
		 * @param folder folder
		 * @throws NodeException
		 */
		private static void saveI18nData(EditableFactoryFolder folder) throws NodeException {
			// clean empty translations
			folder.nameI18n.values().removeIf(v -> StringUtils.isEmpty(v));
			folder.descriptionI18n.values().removeIf(v -> StringUtils.isEmpty(v));
			folder.publishDirI18n.values().removeIf(v -> StringUtils.isEmpty(v));

			// collect translations into a combined map (key is the contentgroup_id, values are triples of name, description, pub_dir)
			Set<Integer> contentGroupIds = new HashSet<>();
			contentGroupIds.addAll(folder.nameI18n.keySet());
			contentGroupIds.addAll(folder.descriptionI18n.keySet());
			contentGroupIds.addAll(folder.publishDirI18n.keySet());

			Map<Integer, Map<String, Object>> combinedI18nMap = contentGroupIds.stream()
					.collect(Collectors.toMap(java.util.function.Function.identity(), key -> {
						Map<String, Object> rowMap = new HashMap<>();
						rowMap.put("name", folder.nameI18n.get(key));
						rowMap.put("description", folder.descriptionI18n.get(key));
						rowMap.put("pub_dir", folder.publishDirI18n.get(key));

						return rowMap;
					}));

			DBUtils.updateCrossTable("folder_i18n", "folder_id", folder.id, "contentgroup_id", combinedI18nMap);
		}

		/**
		 * Save the template assignment for this folder
		 * @param isNew true if the folder is new
		 * @param folder folder
		 * @return true if something was modified
		 * @throws NodeException
		 */
		private static boolean saveTemplateAssignment(boolean isNew, FactoryFolder folder) throws NodeException {
			if (folder.isMaster()) {
				boolean modified = false;

				// find differences in the linked templates and save them
				List<Integer> templateIdsToAdd = new ArrayList<>();
				List<Integer> templateIdsToRemove = new ArrayList<>();

				// new list of templates
				List<Template> templates = folder.getTemplates();

				// currently set templates
				Collection<Integer> templateIds = null;

				// reload the list of currently set templates.
				// But don't do this, if the folder was new, because it does not make sense
				// and we might be just in the middle of saving a new node.
				if (!isNew) {
					folder.templateIds.clear();
					folder.getTemplateIds(null);
					templateIds = folder.templateIds.get(folder.getNode().getId());
				}

				if (templateIds == null) {
					templateIds = Collections.emptyList();
				}
				templateIdsToRemove.addAll(templateIds);

				Collection<Integer> templateIdsToDirt = new HashSet<>();

				// find templates, which shall be added or removed
				for (Iterator<Template> iter = templates.iterator(); iter.hasNext();) {
					Template tmpl = iter.next();

					templateIdsToRemove.remove(tmpl.getId());
					if (!templateIds.contains(tmpl.getId())) {
						templateIdsToAdd.add(folder.getId());
						templateIdsToAdd.add(tmpl.getId());
						templateIdsToDirt.add(tmpl.getId());
					}
				}
				templateIdsToDirt.addAll(templateIdsToRemove);

				// now add the missing templates
				if (templateIdsToAdd.size() > 0) {
					DBUtils.executeInsert(
							"INSERT INTO template_folder (folder_id, template_id) VALUES " + StringUtils.repeat("(?,?)", templateIdsToAdd.size() / 2, ","),
							(Object[]) templateIdsToAdd.toArray(new Object[templateIdsToAdd.size()]));
					modified = true;
				}

				// ... and remove the surplus templates
				if (templateIdsToRemove.size() > 0) {
					templateIdsToRemove.add(0, folder.getId());
					DBUtils.executeUpdate(
							"DELETE FROM template_folder WHERE folder_id = ? AND template_id IN (" + StringUtils.repeat("?", templateIdsToRemove.size() - 1, ",")
							+ ")",
							(Object[]) templateIdsToRemove.toArray(new Object[templateIdsToRemove.size()]));
					modified = true;
				}

				Transaction t = TransactionManager.getCurrentTransaction();
				for (int tmplId : templateIdsToDirt) {
					t.dirtObjectCache(Template.class, tmplId);
				}

				return modified;
			} else {
				return false;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.FolderFactory.FactoryFolder#setDescription(java.lang.String)
		 */
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

		/**
		 * sets a folder's mother id and will also set it's node id and pub dir
		 * to the same values as it's mother folder
		 * @param folderId mother id to be set
		 * @return old mother id on success, null otherwise
		 */
		public Integer setMotherId(Integer folderId) throws NodeException, ReadOnlyException {
			// always set the mother id of the master folder
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder folder = t.getObject(Folder.class, folderId);

			folderId = folder.getMaster().getId();

			assertEditable();
			if (ObjectTransformer.getInt(this.motherId, 0) != ObjectTransformer.getInt(folderId, 0)) {
				Integer oldMotherId = this.motherId;

				this.modified = true;
				this.motherId = ObjectTransformer.getInteger(folderId, 0);

				// now also update the folder's node
				try {
					Folder motherFolder = (Folder) TransactionManager.getCurrentTransaction().getObject(Folder.class, this.motherId);

					this.nodeId = motherFolder.getNode().getId();
					if (this.pubDir == null) {
						this.pubDir = motherFolder.getPublishDir();
					}
				} catch (Exception e) {
					logger.error("Could not set nodeId of folder {" + this.getId() + " " + this.getName() + "} from mother folder {" + this.motherId + "}");
				}

				return oldMotherId;
			}
			return null;
		}

		@Override
		public void setNodeId(Integer id) throws ReadOnlyException, NodeException {
			if (ObjectTransformer.getInt(this.nodeId, -1) != ObjectTransformer.getInt(id, -1)) {
				// we only can set the nodeId, if not yet done before
				if (!isEmptyId(nodeId)) {
					throw new NodeException("Cannot set the node id to " + id + ", node id already set to " + nodeId);
				}
				this.nodeId = id;
				if (this.channelId == -1 && !isEmptyId(masterId)) {
					this.channelId = ObjectTransformer.getInt(id, -1);
				}
				this.modified = true;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.FolderFactory.FactoryFolder#setName(java.lang.String)
		 */
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

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.FolderFactory.FactoryFolder#setPublishDir(java.lang.String)
		 */
		public String setPublishDir(String pubDir) throws ReadOnlyException, NodeException {
			assertEditable();

			pubDir = cleanPubDirIfNecessary(pubDir);

			if (!StringUtils.isEqual(this.pubDir, pubDir)) {
				String oldPubDir = this.pubDir;

				this.modified = true;
				this.pubDir = pubDir;
				return oldPubDir;
			} else {
				return this.pubDir;
			}
		}

		@Override
		public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException {
			// ignore this call, if the channelinfo is already set identical
			if (ObjectTransformer.getInt(this.channelId, 0) == ObjectTransformer.getInt(channelId, 0)
					&& this.channelSetId == ObjectTransformer.getInt(channelSetId, 0)) {
				return;
			}

			// check whether the folder is new
			if (!isEmptyId(getId()) && (!allowChange || this.channelSetId != ObjectTransformer.getInt(channelSetId, 0))) {
				// the folder is not new, so we must not set the channel
				// information
				throw new NodeException("Cannot change channel information for {" + this + "}, because the folder is not new");
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
			if (iChannelId == 0 && iChannelSetId != 0 && !isEmptyId(motherId)) {
				throw new NodeException(
						"Error while setting channel information: channelId was set to {" + channelId + "} and channelSetId was set to {" + channelSetId
						+ "}, which is not allowed (when creating master objects in non-channels, the channelSetId must be autogenerated)");
			}
			// set the data
			this.channelId = iChannelId;

			if (iChannelSetId == 0) {
				this.channelSetId = ObjectTransformer.getInt(createChannelsetId(), 0);
			} else {
				this.channelSetId = iChannelSetId;
			}
			channelSet = null;

			// set the "master" flag to false, because we are not yet sure,
			// whether this object is a master or not
			this.master = false;

			// now get the master object
			Folder masterFolder = getMaster();

			if (masterFolder == this) {
				this.master = true;
				this.masterId = null;
			} else {
				this.master = false;
				this.masterId = masterFolder != null ? masterFolder.getId() : null;
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

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.FolderFactory.FactoryFolder#getTemplates()
		 */
		public List<Template> getTemplates(TemplateSearch search) throws NodeException {
			if (search == null) {
				if (templates == null) {
					// make a copy of the list, so that is is modifiable in any case
					templates = new ArrayList<Template>(super.getTemplates(null));
				}
				return templates;
			} else {
				return super.getTemplates(search);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#setTemplates(java.util.List)
		 */
		public void setTemplates(List<Template> templates) throws ReadOnlyException,
					NodeException {
			this.templates = new Vector<Template>(templates);
			modified = true;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.FolderFactory.FactoryFolder#getObjectTags()
		 */
		public Map<String, ObjectTag> getObjectTags() throws NodeException {
			if (objectTags == null) {
				objectTags = super.getObjectTags();
			}
			return objectTags;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);
			Folder oFolder = (Folder) original;

			// copy Meta Information
			setName(oFolder.getName());
			setDescription(oFolder.getDescription());
			setPublishDir(oFolder.getPublishDir());

			// copy i18n data
			setNameI18n(new I18nMap(oFolder.getNameI18n()));
			setDescriptionI18n(new I18nMap(oFolder.getDescriptionI18n()));
			setPublishDirI18n(new I18nMap(oFolder.getPublishDirI18n()));

			// copy object tags
			Map<String, ObjectTag> thisOTags = getObjectTags();
			Map<String, ObjectTag> originalOTags = oFolder.getObjectTags();

			for (Map.Entry<String, ObjectTag> entry : originalOTags.entrySet()) {
				String tagName = entry.getKey();
				ObjectTag originalTag = entry.getValue();

				if (thisOTags.containsKey(tagName)) {
					// found the tag in this folder, copy the original tag over it
					thisOTags.get(tagName).copyFrom(originalTag);
				} else {
					// did not find the tag, so copy the original
					thisOTags.put(tagName, (ObjectTag) originalTag.copy());
				}
			}

			// remove all tags that do not exist in the original folder
			for (Iterator<Map.Entry<String, ObjectTag>> i = thisOTags.entrySet().iterator(); i.hasNext();) {
				Entry<String, ObjectTag> entry = i.next();

				if (!originalOTags.containsKey(entry.getKey())) {
					i.remove();
				}
			}
		}

		@Override
		public void setChannelMaster(Folder folder) throws ReadOnlyException,
					NodeException {
			// this only works, if ...

			// .. this folder is new
			if (!isEmptyId(getId())) {
				throw new NodeException("Cannot make " + this + " a root folder of a channel, because it is not new");
			}
			// .. this folder is a root folder
			if (!isEmptyId(motherId)) {
				throw new NodeException("Cannot make " + this + " a root folder of a channel, because it already has a mother");
			}

			// .. the given folder is not new
			if (isEmptyId(folder.getId())) {
				throw new NodeException("Cannot set " + folder + " as master folder, because it is new");
			}

			// .. the given folder is a root folder
			if (folder.getMother() != null) {
				throw new NodeException("Cannot set " + folder + " as master folder, because it is no root folder");
			}

			master = false;
			masterId = folder.getId();
			channelSetId = ObjectTransformer.getInt(folder.getChannelSetId(), -1);
			// channel will temporarily be set to -1 (which means, this will be set, when the node is saved)
			channelId = -1;

			// store the owning node, because in the process of saving the channel root folder for the first time,
			// we will not be able to determine the owning node
			owningNode = folder.getOwningNode();
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

		@Override
		public Node getOwningNode() throws NodeException {
			if (owningNode == null) {
				owningNode = super.getOwningNode();
			}
			return owningNode;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Folder#move(com.gentics.contentnode.object.Folder, int)
		 */
		@Override
		public OpResult move(Folder target, final int targetChannelId) throws ReadOnlyException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// === Check for data validity

			if (target == null) {
				return OpResult.fail(Folder.class, "op.no_targetfolder");
			}

			// get the master folder of the target
			if (!target.isMaster()) {
				target = target.getMaster();
			}

			if (isRoot()) {
				return OpResult.fail(Folder.class, "move.rootfolder");
			}

			Folder targetMother = target;
			while (targetMother != null) {
				if (this.equals(targetMother)) {
					return OpResult.fail(Folder.class, "move.target_is_child");
				}
				targetMother = targetMother.getMother();
			}

			final int targetFolderId = ObjectTransformer.getInt(target.getId(), 0);
			final int targetNodeId = ObjectTransformer.getInt(target.getOwningNode().getId(), 0);

			Node targetChannel = null;
			if (targetChannelId > 0) {
				targetChannel = t.getObject(Node.class, targetChannelId);
				if (targetChannel == null) {
					return OpResult.fail(Folder.class, "move.missing_targetchannel");
				}
				if (MultichannellingFactory.getChannelVariant(target, targetChannel) == null) {
					return OpResult.fail(Folder.class, "move.target_invisible");
				}
			}

			if (!isMaster()) {
				return OpResult.fail(Folder.class, "move.no_master");
			}

			// === Check whether something actually needs to be done
			Node channelOrNode = Optional.ofNullable(getChannel()).orElse(getOwningNode());
			if (Objects.equals(target, getMother().getMaster()) && (targetChannel == null || Objects.equals(targetChannel, channelOrNode))) {
				return OpResult.OK;
			}

			// === Check for sufficient permissions
			try {
				t.setChannelId(targetChannelId);
				if (!t.canCreate(target, Folder.class, null)) {
					return OpResult.fail(Folder.class, "829.you_have_no_permission_to");
				}
			} finally {
				t.resetChannel();
			}
			try {
				t.setChannelId(0);
				if (!t.canDelete(this)) {
					return OpResult.fail(Folder.class, "rest.folder.nopermission", ObjectTransformer.getString(getId(), null));
				}
			} finally {
				t.resetChannel();
			}

			// get all channelset variants
			final Set<Integer> ids = new HashSet<Integer>();
			ids.add(ObjectTransformer.getInt(getId(), 0));
			for (Object csVariantId : getChannelSet().values()) {
				ids.add(ObjectTransformer.getInteger(csVariantId, 0));
			}
			// wbIds contains all IDs of channelset variants, including the objects from the wastebin
			final Set<Integer> wbIds = new HashSet<Integer>();
			wbIds.add(getId());
			try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
				wbIds.addAll(getChannelSet().values());
			}

			Node channel = getChannel();
			final int sourceChannelId = (channel != null ? ObjectTransformer.getInt(channel.getId(), 0) : 0);
			final int oldMotherId = ObjectTransformer.getInt(getMother().getId(), 0);
			final int oldNodeId = ObjectTransformer.getInt(getOwningNode().getId(), 0);
			final int oldChannelOrNodeId = sourceChannelId != 0 ? sourceChannelId : oldNodeId;
			List<Integer> subFolderIds = new ArrayList<Integer>(getSubfolderIds(this, true, true));
			boolean moveToOtherNode = false;

			// === Check for localization issues
			if (!getOwningNode().equals(target.getOwningNode())) {
				moveToOtherNode = true;
				final boolean[] localizationFound = {false};
				// the folder shall be moved to another channel structure, so
				// neither the folder, nor any object in the folder may have
				// localized copies or may be local to a subchannel
				DBUtils.executeMassStatement(
						"SELECT f2.id FROM folder f1 LEFT JOIN folder f2 ON f1.channelset_id = f2.channelset_id WHERE (f2.is_master = ? OR f2.channel_id != ?) AND f1.id IN",
						subFolderIds, 3, new SQLExecutor() {
							@Override
							public void prepareStatement(PreparedStatement stmt) throws SQLException {
								stmt.setInt(1, 0);
								stmt.setInt(2, sourceChannelId);
							}

							@Override
							public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
								if (rs.next()) {
									localizationFound[0] = true;
								}
							}
						});
				if (localizationFound[0]) {
					return OpResult.fail(Folder.class, "folder.move.localization.error");
				}
				DBUtils.executeMassStatement("SELECT id FROM page WHERE (is_master = ? OR channel_id != ?) AND folder_id IN", subFolderIds, 3, new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, 0);
						stmt.setInt(2, sourceChannelId);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						if (rs.next()) {
							localizationFound[0] = true;
						}
					}
				});
				if (localizationFound[0]) {
					return OpResult.fail(Folder.class, "folder.move.localization.error");
				}
				DBUtils.executeMassStatement("SELECT id FROM contentfile WHERE (is_master = ? OR channel_id != ?) AND folder_id IN", subFolderIds, 3, new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, 0);
						stmt.setInt(2, sourceChannelId);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						if (rs.next()) {
							localizationFound[0] = true;
						}
					}
				});

				if (localizationFound[0]) {
					return OpResult.fail(Folder.class, "folder.move.localization.error");
				}
			}

			if (targetChannel != null) {
				// the folder shall be moved into a channel, so neither the
				// folder, nor any object in the folder may have localized
				// copies in the target channel or any of its parents

				final List<Integer> forbiddenChannelIds = new ArrayList<Integer>();
				// first add all channel ids
				for (Node anyChannel : targetChannel.getMaster().getAllChannels()) {
					forbiddenChannelIds.add(ObjectTransformer.getInt(anyChannel.getId(), 0));
				}
				// then remove the allowed channels
				for (Node allowedChannel : targetChannel.getAllChannels()) {
					forbiddenChannelIds.remove(ObjectTransformer.getInteger(allowedChannel.getId(), null));
				}
				if (!forbiddenChannelIds.isEmpty()) {
					final int[] localizationFound = {0};
					String qMarks = StringUtils.repeat("?", forbiddenChannelIds.size(), ",");
					DBUtils.executeMassStatement("SELECT f2.id FROM folder f1 LEFT JOIN folder f2 ON f1.channelset_id = f2.channelset_id WHERE f2.is_master = ? AND f2.channel_id IN (" + qMarks + ") AND f1.id IN", subFolderIds, 2 + forbiddenChannelIds.size(), new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							int pCounter = 1;
							stmt.setInt(pCounter++, 0);
							for (Integer id : forbiddenChannelIds) {
								stmt.setInt(pCounter++, id);
							}
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							if (rs.next()) {
								localizationFound[0] = rs.getInt("id");
							}
						}
					});
					if (localizationFound[0] != 0) {
						try (WastebinFilter wbFilter = Wastebin.INCLUDE.set()) {
							return OpResult.fail(Folder.class, "object.move.localization.error",
									I18NHelper.getPath(t.getObject(Folder.class, localizationFound[0], -1, false)));
						}
					}

					localizationFound[0] = 0;
					DBUtils.executeMassStatement("SELECT id FROM page WHERE is_master = ? AND channel_id IN (" + qMarks + ") AND folder_id IN", subFolderIds,
							2 + forbiddenChannelIds.size(), new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							int pCounter = 1;
							stmt.setInt(pCounter++, 0);
							for (Integer id : forbiddenChannelIds) {
								stmt.setInt(pCounter++, id);
							}
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							if (rs.next()) {
								localizationFound[0] = rs.getInt("id");
							}
						}
					});
					if (localizationFound[0] != 0) {
						try (WastebinFilter wbFilter = Wastebin.INCLUDE.set()) {
							return OpResult.fail(Folder.class, "object.move.localization.error",
									I18NHelper.getPath(t.getObject(Page.class, localizationFound[0], -1, false)));
						}
					}

					localizationFound[0] = 0;
					DBUtils.executeMassStatement("SELECT id FROM contentfile WHERE is_master = ? AND channel_id IN (" + qMarks + ") AND folder_id IN", subFolderIds,
							2 + forbiddenChannelIds.size(), new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							int pCounter = 1;
							stmt.setInt(pCounter++, 0);
							for (Integer id : forbiddenChannelIds) {
								stmt.setInt(pCounter++, id);
							}
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							if (rs.next()) {
								localizationFound[0] = rs.getInt("id");
							}
						}
					});
					if (localizationFound[0] != 0) {
						try (WastebinFilter wbFilter = Wastebin.INCLUDE.set()) {
							return OpResult.fail(Folder.class, "object.move.localization.error",
									I18NHelper.getPath(t.getObject(ContentFile.class, localizationFound[0], -1, false)));
						}
					}
				}
			}

			// === Check for possible naming conflicts for the folder
			final boolean[] conflictFound = {false};
			DBUtils.executeStatement("SELECT id FROM folder WHERE mother = ? AND LOWER(name) = LOWER(?) AND deleted = 0", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, targetFolderId);
					stmt.setString(2, getName());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					if (rs.next()) {
						int id = rs.getInt("id");
						if (!ids.contains(id)) {
							conflictFound[0] = true;
						}
					}
				}
			});
			if (conflictFound[0]) {
				return OpResult.fail(Folder.class, "451.a_folder_with_this_name");
			}

			// === Check for possible pub_dir conflicts for the folder
			if (target.getNode().isPubDirSegment()) {
				Set<Integer> conflicts = DBUtils.select("SELECT id FROM folder WHERE mother = ? AND LOWER(pub_dir) = LOWER(?) AND deleted = 0", rs -> {
					rs.setInt(1, targetFolderId);
					rs.setString(2, cleanPubDir(getPublishDir(), true, true));
				}, DBUtils.IDS);
				conflicts.removeAll(ids);

				if (!conflicts.isEmpty()) {
					Folder conflict = t.getObject(Folder.class, conflicts.iterator().next(), -1, false);
					return OpResult.fail(Folder.class, "move.pub_dir_conflict", getName(), I18NHelper.getPath(target), I18NHelper.getPath(conflict));
				}

				// if the source node did not have pub_dir segments, we need to check uniqueness of the pub_dir's in the moved folder structure
				if (!getNode().isPubDirSegment()) {
					Folder conflict = null;
					for (Folder child : getChildFolders()) {
						conflict = checkPubDirUniqueness(child);
						if (conflict != null) {
							break;
						}
					}

					if (conflict != null) {
						return OpResult.fail(Folder.class, "move.pub_dir_conflict", getName(), I18NHelper.getPath(target), I18NHelper.getPath(conflict));
					}
				}
			}

			// === Check for possible naming conflicts of objects in the moved folder structure, when moving between nodes
			if (!getOwningNode().equals(target.getOwningNode()) && !target.getOwningNode().isPubDirSegment()) {
				List<Integer> folderIds = new ArrayList<Integer>(getSubfolderIds(this, true, true));

				// get the pub_dirs of the moved folders
				final Set<String> movedPubDirs = new HashSet<String>();
				// if the source node uses pubdir segments, and the target node does not, we need to clean the pub dirs
				boolean cleanSourcePubDirs = getNode().isPubDirSegment();
				DBUtils.executeMassStatement("SELECT pub_dir FROM folder WHERE deleted = 0 AND id IN", folderIds, 1, new SQLExecutor() {
					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							String pubDir = rs.getString("pub_dir");
							if (cleanSourcePubDirs) {
								pubDir = cleanPubDir(pubDir, false, true);
							}
							movedPubDirs.add(pubDir);
						}
					}
				});

				// get the pub_dirs of the folders in the target node
				final Set<String> targetPubDirs = new HashSet<String>();
				DBUtils.executeStatement("SELECT pub_dir FROM folder WHERE deleted = 0 AND node_id = ?", new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, targetNodeId);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							targetPubDirs.add(rs.getString("pub_dir"));
						}
					}
				});

				// if we have at least one duplicate, we need to check filenames as well
				final Set<String> conflictingPubDirs = new HashSet<String>(targetPubDirs);
				conflictingPubDirs.retainAll(movedPubDirs);
				if (!conflictingPubDirs.isEmpty()) {
					// first collect all pathnames of pages and files within the folder structure which is about to be moved
					final Set<String> movedPaths = new HashSet<String>();

					DBUtils.executeMassStatement("SELECT pub_dir, page.filename FROM page, folder WHERE page.deleted = 0 AND folder.deleted = 0 AND page.folder_id = folder.id AND folder.id IN", folderIds, 1, new SQLExecutor() {
						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								String pubDir = rs.getString("pub_dir");
								if (cleanSourcePubDirs) {
									pubDir = cleanPubDir(pubDir, false, true);
								}
								if (conflictingPubDirs.contains(pubDir)) {
									movedPaths.add(FilePublisher.getPath(true, false, pubDir, rs.getString("filename")));
								}
							}
						}
					});
					DBUtils.executeMassStatement("SELECT pub_dir, contentfile.name filename FROM contentfile, folder WHERE contentfile.deleted = 0 AND folder.deleted = 0 AND contentfile.folder_id = folder.id AND folder.id IN", folderIds, 1, new SQLExecutor() {
						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								String pubDir = rs.getString("pub_dir");
								if (cleanSourcePubDirs) {
									pubDir = cleanPubDir(pubDir, false, true);
								}
								if (conflictingPubDirs.contains(pubDir)) {
									movedPaths.add(FilePublisher.getPath(true, false, pubDir, rs.getString("filename")));
								}
							}
						}
					});

					conflictFound[0] = false;
					final String[] conflictingPath = {null};
					// now we check against the existing paths of pages
					DBUtils.executeStatement("SELECT folder.pub_dir, page.filename FROM page, folder WHERE page.deleted = 0 AND folder.deleted = 0 AND page.folder_id = folder.id AND folder.node_id = ?", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, targetNodeId);
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								String path = FilePublisher.getPath(true, false, rs.getString("pub_dir"), rs.getString("filename"));
								if (movedPaths.contains(path)) {
									conflictFound[0] = true;
									conflictingPath[0] = path;
									break;
								}
							}
						}
					});

					if (conflictFound[0]) {
						return OpResult.fail(Folder.class, "move.namingconflict", getName(), target.getOwningNode().getFolder().getName(), conflictingPath[0]);
					}

					// check against existing paths of files
					DBUtils.executeStatement("SELECT folder.pub_dir, contentfile.name filename FROM contentfile, folder WHERE contentfile.deleted = 0 AND folder.deleted = 0 AND contentfile.folder_id = folder.id AND folder.node_id = ?", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, targetNodeId);
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								String path = FilePublisher.getPath(true, false, rs.getString("pub_dir"), rs.getString("filename"));
								if (movedPaths.contains(path)) {
									conflictFound[0] = true;
									conflictingPath[0] = path;
									break;
								}
							}
						}
					});

					if (conflictFound[0]) {
						return OpResult.fail(Folder.class, "move.namingconflict", getName(), target.getOwningNode().getFolder().getName(), conflictingPath[0]);
					}
				}
			}

			// === Check mc exclusion and disinheriting consistency
			boolean multichannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);
			if (multichannelling) {
				try {
					ChannelTreeSegment newSegment = new ChannelTreeSegment(this, true).addRestrictions(target.isExcluded(), target.getDisinheritedChannels());
					try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
						DisinheritUtils.checkChangeConsistency(this, newSegment.isExcluded(), newSegment.getRestrictions(), false);
					}
				} catch (ChannelInheritanceException e) {
					return new OpResult(Status.FAILURE, new DefaultNodeMessage(Level.ERROR, Folder.class, e.getLocalizedMessage()));
				}
			}

			// === Perform the move
			// set the new mother for the folder (and all channelset variants)
			DBUtils.executeMassStatement("UPDATE folder SET mother = ? WHERE id IN", null, new ArrayList<Integer>(wbIds), 2, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, targetFolderId);
				}
			}, Transaction.UPDATE_STATEMENT);
			motherId = targetFolderId;

			List<Integer> changeNodeId = new ArrayList<Integer>(wbIds);
			changeNodeId.addAll(getSubfolderIds(this, false, null));

			// when moving into another node, update the node_id for the moved folders and all subfolders
			if (!getOwningNode().equals(target.getOwningNode())) {

				DBUtils.executeMassStatement("UPDATE folder SET node_id = ? WHERE id IN", changeNodeId, 2, new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, targetNodeId);
					}
				});
				nodeId = targetNodeId;

				// if the nodes have different pub_dir_segment settings, we need to clean all pub_dirs
				if (getOwningNode().isPubDirSegment() != target.getOwningNode().isPubDirSegment()) {
					Map<Integer, String> cleanedPubDirMap = new HashMap<>();
					boolean targetPubDirSegment = target.getOwningNode().isPubDirSegment();
					DBUtils.executeMassStatement("SELECT id, pub_dir FROM folder WHERE id IN", changeNodeId, 1, new SQLExecutor() {
						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								cleanedPubDirMap.put(rs.getInt("id"), cleanPubDir(rs.getString("pub_dir"), targetPubDirSegment, true));
							}
						}
					});

					for (Map.Entry<Integer, String> entry : cleanedPubDirMap.entrySet()) {
						DBUtils.executeUpdate("UPDATE folder SET pub_dir = ? WHERE id = ?", new Object[] {entry.getValue(), entry.getKey()});
					}
				}
			}

			final int setChannelId = (targetChannel != null && targetChannel.isChannel() ? targetChannelId : 0);
			Set<Integer> unchangedChannelIds = new HashSet<Integer>();
			unchangedChannelIds.add(setChannelId);
			if (targetChannel != null) {
				unchangedChannelIds.add(ObjectTransformer.getInt(targetChannel.getId(), 0));

				Collection<Node> allSubChannels = targetChannel.getAllChannels();
				for (Node subChannel : allSubChannels) {
					unchangedChannelIds.add(ObjectTransformer.getInt(subChannel.getId(), 0));
				}
			}
			// set the target channel id for all folders
			List<Integer> folderIdsToChannel = getIdsMoveToChannel(TYPE_FOLDER, sourceChannelId, subFolderIds, unchangedChannelIds);
			if (!folderIdsToChannel.contains(getId())) {
				folderIdsToChannel.add(ObjectTransformer.getInt(getId(), 0));
			}
			DBUtils.executeMassStatement("UPDATE folder SET channel_id = ? WHERE id IN", folderIdsToChannel, 2, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, setChannelId);
				}
			});
			channelId = setChannelId;

			// change the channel id for all contained pages
			List<Integer> pageIdsToChannel = getIdsMoveToChannel(Page.TYPE_PAGE, sourceChannelId, subFolderIds, unchangedChannelIds);
			if (!ObjectTransformer.isEmpty(pageIdsToChannel)) {
				DBUtils.executeMassStatement("UPDATE page SET channel_id = ? WHERE id IN", pageIdsToChannel, 2, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, setChannelId);
				}
			});
			}

			// change the channel id for all contained files
			List<Integer> fileIdsToChannel = getIdsMoveToChannel(File.TYPE_FILE, sourceChannelId, subFolderIds, unchangedChannelIds);
			if (!ObjectTransformer.isEmpty(fileIdsToChannel)) {
				DBUtils.executeMassStatement("UPDATE contentfile SET channel_id = ? WHERE id IN", fileIdsToChannel, 2, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, setChannelId);
				}
			});
			}

			// write logcmd entries, trigger events, clear caches
			int targetChannelOrNodeId = targetChannelId > 0 ? targetChannelId : targetNodeId;
			String[] oldNodeProps = new String[] {Integer.toString(oldChannelOrNodeId)};
			ActionLogger.logCmd(ActionLogger.MOVE, Folder.TYPE_FOLDER, getId(), target.getId(), "Folder.move()");
			t.dirtObjectCache(Folder.class, getId(), true);
			t.dirtObjectCache(Folder.class, targetFolderId, true);
			t.dirtObjectCache(Folder.class, oldMotherId, true);
			t.addTransactional(new TransactionalTriggerEvent(Folder.class, getId(), oldNodeProps, Events.MOVE));
			t.addTransactional(new TransactionalTriggerEvent(Folder.class, targetFolderId, null, Events.MOVE | Events.CHILD));
			t.addTransactional(new TransactionalTriggerEvent(Folder.class, oldMotherId, null, Events.MOVE | Events.CHILD));
			for (Integer id : wbIds) {
				t.dirtObjectCache(Folder.class, id, true);
			}
				List<Integer> movedFolderIds = new ArrayList<Integer>();
			final List<Integer> movedPageIds = new ArrayList<Integer>();
			final List<Integer> movedFileIds = new ArrayList<Integer>();

			if (moveToOtherNode || targetChannel != null) {
				if (moveToOtherNode) {
					movedFolderIds.addAll(subFolderIds);
				} else {
					movedFolderIds.addAll(folderIdsToChannel);
				}
				for (Integer subFolderId : movedFolderIds) {
					ActionLogger.logCmd(ActionLogger.MOVE, Folder.TYPE_FOLDER, subFolderId, target.getId(), "Folder.move() to node " + targetChannelOrNodeId);
					t.addTransactional(new TransactionalTriggerEvent(Folder.class, subFolderId, oldNodeProps, Events.MOVE));
					t.dirtObjectCache(Folder.class, subFolderId, true);
				}

				// trigger move event for all pages, images and files that were moved to another channel
				if (moveToOtherNode) {
				DBUtils.executeMassStatement("SELECT id FROM page WHERE is_master = ? AND deleted = 0 AND folder_id IN", subFolderIds, 2, new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, 1);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
								movedPageIds.add(rs.getInt("id"));
						}
					}
				});
				} else {
					movedPageIds.addAll(pageIdsToChannel);
				}
				for (Integer pageId : movedPageIds) {
					ActionLogger.logCmd(ActionLogger.MOVE, Page.TYPE_PAGE, pageId, targetChannelOrNodeId, "Move page in folder to other node");
					t.addTransactional(new TransactionalTriggerEvent(Page.class, pageId, oldNodeProps, Events.MOVE));
					t.dirtObjectCache(Page.class, pageId, true);
				}

				if (moveToOtherNode) {
				DBUtils.executeMassStatement("SELECT id FROM contentfile WHERE is_master = ? AND deleted = 0 AND folder_id IN", subFolderIds, 2, new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, 1);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
								movedFileIds.add(rs.getInt("id"));
						}
					}
				});
				} else {
					movedFileIds.addAll(fileIdsToChannel);
				}
				for (Integer fileId : movedFileIds) {
					ActionLogger.logCmd(ActionLogger.MOVE, ContentFile.TYPE_FILE, fileId, targetChannelOrNodeId, "Move file in folder to other node");
					t.addTransactional(new TransactionalTriggerEvent(File.class, fileId, oldNodeProps, Events.MOVE));
					t.dirtObjectCache(File.class, fileId, true);
				}
			}

			if (multichannelling) {
				List<Disinheritable<?>> movedObjects = new ArrayList<>(movedFolderIds.size() + movedPageIds.size() + movedFileIds.size());

				movedObjects.addAll(t.getObjects(Folder.class, movedFolderIds));
				movedObjects.addAll(t.getObjects(Page.class, movedPageIds));
				movedObjects.addAll(t.getObjects(File.class, movedFileIds));

				Folder mother = t.getObject(Folder.class, getId()).getMother();
				ChannelTreeSegment motherSegment = new ChannelTreeSegment(mother, true);

				for (Disinheritable<?> obj : movedObjects) {
					ChannelTreeSegment objSegment = new ChannelTreeSegment(obj, true);
					ChannelTreeSegment restrictedSegment = objSegment.addRestrictions(motherSegment.isExcluded(), motherSegment.getRestrictions());

					obj.changeMultichannellingRestrictions(restrictedSegment.isExcluded(), restrictedSegment.getRestrictions(), false);

					if (mother.isDisinheritDefault() && !obj.isDisinheritDefault()) {
						obj.setDisinheritDefault(true, true);
				}
				};
			}

			return OpResult.OK;
		}

		@Override
		public void setNameI18n(I18nMap nameI18n) throws ReadOnlyException {
			if (!Objects.deepEquals(this.nameI18n, nameI18n)) {
				this.nameI18n = nameI18n;
				this.modified = true;
			}
		}

		@Override
		public void setDescriptionI18n(I18nMap descriptionI18n) throws ReadOnlyException {
			if (!Objects.deepEquals(this.descriptionI18n, descriptionI18n)) {
				this.descriptionI18n = descriptionI18n;
				this.modified = true;
			}
		}

		@Override
		public void setPublishDirI18n(I18nMap publishDirI18n) throws NodeException {
			if (publishDirI18n != null) {
				for (Map.Entry<Integer, String> entry : publishDirI18n.entrySet()) {
					entry.setValue(cleanPubDirIfNecessary(entry.getValue()));
				}
			}

			if (!Objects.deepEquals(this.publishDirI18n, publishDirI18n)) {
				this.publishDirI18n = publishDirI18n;
				this.modified = true;
			}
		}

		/**
		 * Get the set of IDs of objects of given type, that are contained in the given folders (or ARE the given folders) and must be moved to the target channel
		 * @param type object type
		 * @param sourceChannelId ID of the source channel
		 * @param folderIds list of folder IDs
		 * @param unchangedChannelIds list of channel IDs for objects, that must NOT be changed (may be empty). Note that objects in the source channel will aleays
		 * 	be moved to the new channel.
		 * @return set of IDs of objects to move to the channel
		 * @throws NodeException
		 */
		protected List<Integer> getIdsMoveToChannel(int type, int sourceChannelId, List<Integer> folderIds, final Set<Integer> unchangedChannelIds) throws NodeException {
			String idField;
			final List<Integer> ids = new ArrayList<Integer>();
			int params = unchangedChannelIds.size() + 1;
			StringBuilder sql = new StringBuilder("SELECT id FROM ");

			switch (type) {
			case TYPE_FOLDER:
				sql.append("folder");
				idField = "id";
				break;

			case Page.TYPE_PAGE:
				sql.append("page");
				idField = "folder_id";
				break;

			case File.TYPE_FILE:
				sql.append("contentfile");
				idField = "folder_id";
				break;

			default:
				return ids;
			}

			sql.append(" WHERE is_master = 1 AND ");
			if (!ObjectTransformer.isEmpty(unchangedChannelIds)) {
				sql
					.append("(channel_id = ? OR channel_id NOT IN (")
					.append(StringUtils.repeat("?", unchangedChannelIds.size(), ","))
					.append(")) AND ");
			}
			sql.append(idField).append(" IN");

			DBUtils.executeMassStatement(sql.toString(), folderIds, params + 1, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					int paramsIndex = 1;
					stmt.setInt(paramsIndex++, sourceChannelId);
					if (!ObjectTransformer.isEmpty(unchangedChannelIds)) {
						for (Integer id : unchangedChannelIds) {
							stmt.setInt(paramsIndex++, id);
						}
					}
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						ids.add(rs.getInt("id"));
					}
				}
			});

			return ids;
		}

		/**
		 * Clean the publish directory, if necessary
		 * @param pubDir pub dir
		 * @return cleaned pub dir
		 * @throws NodeException
		 */
		protected String cleanPubDirIfNecessary(String pubDir) throws NodeException {
			if (!ObjectTransformer.getBoolean(TransactionManager.getCurrentTransaction().getAttributes().get(OMIT_PUB_DIR_SEGMENT_VERIFY), false)) {
				pubDir = cleanPubDir(pubDir, !isEmptyId(nodeId) && getNode().isPubDirSegment(), true);
	}

			// truncate, if too long (preserving trailing /)
			if (pubDir.length() > Folder.MAX_PUB_DIR_LENGTH) {
				pubDir = String.format("%s/", pubDir.substring(0, Folder.MAX_PUB_DIR_LENGTH - 1));
			}

			return pubDir;
		}
	}

	/**
	 * Factory Implementation of Node
	 */
	private static class FactoryNode extends AbstractNode {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 1114224666024839608L;

		@DataField("folder_id")
		protected Integer folderId;

		@DataField("pub_dir")
		@Updateable
		protected String pubDir;

		@DataField("pub_dir_bin")
		@Updateable
		protected String pubDirBin;

		@DataField("pub_dir_segment")
		@Updateable
		protected boolean pubDirSegment;

		@DataField("https")
		@Updateable
		protected boolean https;

		@DataField("pub_img_variants")
		@Updateable
		protected boolean pubImgVariants;

		@DataField("host")
		@Updateable
		protected String host;

		@DataField("host_property")
		@Updateable
		protected String hostProperty;

		@DataField("ftphost")
		@Updateable
		protected String ftpHost = "";

		@DataField("ftplogin")
		@Updateable
		protected String ftpLogin = "";

		@DataField("ftppassword")
		@Updateable
		protected String ftpPw = "";

		@DataField("ftpwwwroot")
		@Updateable
		protected String ftpWwwRoot = "";

		@DataField("ftpsync")
		@Updateable
		protected boolean ftpSync;

		@DataField("publish_fs")
		@Updateable
		protected boolean publishFilesystem;

		@DataField("publish_fs_pages")
		@Updateable
		protected boolean publishFilesystemPages;

		@DataField("publish_fs_files")
		@Updateable
		protected boolean publishFilesystemFiles;

		@DataField("publish_contentmap")
		@Updateable
		protected boolean publishContentmap;

		@DataField("publish_contentmap_pages")
		@Updateable
		protected boolean publishContentMapPages;

		@DataField("publish_contentmap_files")
		@Updateable
		protected boolean publishContentMapFiles;

		@DataField("publish_contentmap_folders")
		@Updateable
		protected boolean publishContentMapFolders;

		@DataField("disable_publish")
		@Updateable
		protected boolean disablePublish;

		@DataField("contentmap_handle")
		@Updateable
		protected String cnMapKeyname = "";

		@DataField("utf8")
		@Updateable
		protected boolean utf8;

		@DataField("contentrepository_id")
		@Updateable
		protected Integer contentrepositoryId;

		@DataField("editorversion")
		@Updateable
		protected int editorversion;

		@DataField("creator")
		protected int creatorId;

		@DataField("cdate")
		protected ContentNodeDate cdate = new ContentNodeDate(0);

		@DataField("editor")
		@Updateable
		protected int editorId;

		@DataField("edate")
		@Updateable
		protected ContentNodeDate edate = new ContentNodeDate(0);

		@DataField("default_file_folder_id")
		@Updateable
		protected Integer defaultFileFolderId;

		@DataField("default_image_folder_id")
		@Updateable
		protected Integer defaultImageFolderId;

		@DataField("urlrenderway_pages")
		@Updateable
		protected int urlRenderWayPages;

		@DataField("urlrenderway_files")
		@Updateable
		protected int urlRenderWayFiles;

		@DataField("omit_page_extension")
		@Updateable
		protected boolean omitPageExtension;

		@DataField("page_language_code")
		@Updateable
		protected PageLanguageCode pageLanguageCode = PageLanguageCode.FILENAME;

		@DataField("mesh_preview_url")
		@Updateable
		protected String meshPreviewUrl;

		@DataField("mesh_preview_url_property")
		@Updateable
		protected String meshPreviewUrlProperty;

		@DataField("insecure_preview_url")
		@Updateable
		protected boolean insecurePreviewUrl;

		@DataField("mesh_project_name")
		@Updateable
		protected String meshProjectName;

		/**
		 * List of language ids assigned to this node
		 */
		protected List<Integer> languageIds;

		/**
		 * MD5 Hash over all languages assigned to the node
		 */
		protected String languageIdsMD5;

		/**
		 * Ids of the template assigned to this node
		 */
		protected Set<Integer> templateIds;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		public FactoryNode(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 * @param udate udate
		 * @param globalId globalid
		 */
		public FactoryNode(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap, int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
			this.udate = udate;
			this.globalId = globalId;
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

		public Folder getFolder() throws NodeException {
			// get the root folder of the node (without multichannelling fallback)
			Folder folder = (Folder) TransactionManager.getCurrentTransaction().getObject(Folder.class, folderId, getObjectInfo().isEditable(), false, true);

			// check data consistency
			assertNodeObjectNotNull(folder, folderId, "Folder");
			return folder;
		}

		public String getPublishDir() {
			return pubDir;
		}

		@Override
		public String getBinaryPublishDir() {
			return pubDirBin;
		}

		@Override
		public boolean isPubDirSegment() {
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.PUB_DIR_SEGMENT)) {
				return pubDirSegment;
			} else {
				return false;
			}
		}

		@Override
		public boolean isPublishImageVariants() {
			return pubImgVariants;
		}

		public boolean isHttps() {
			return https;
		}

		public String getHostname() {
			return host;
		}

		@Override
		public String getHostnameProperty() {
			return hostProperty;
		}

		public String getFtpHostname() {
			return ftpHost;
		}

		public String getFtpLogin() {
			return ftpLogin;
		}

		public String getFtpPassword() {
			return ftpPw;
		}

		public String getFtpWwwRoot() {
			return ftpWwwRoot;
		}

		public boolean doFtpSync() {
			return ftpSync;
		}

		public boolean doPublishFilesystem() {
			return publishFilesystem;
		}

		public boolean doPublishFilesystemPages() {
			return publishFilesystemPages;
		}

		public boolean doPublishFilesystemFiles() {
			return publishFilesystemFiles;
		}

		public boolean doPublishContentmap() {
			return publishContentmap;
		}

		public boolean doPublishContentMapPages() {
			return publishContentMapPages;
		}

		public boolean doPublishContentMapFiles() {
			return publishContentMapFiles;
		}

		public boolean doPublishContentMapFolders() {
			return publishContentMapFolders;
		}

		public String getContentmapKeyword() {
			return cnMapKeyname;
		}

		public boolean isUtf8() {
			return utf8;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Node#isPublishDisabled()
		 */
		public boolean isPublishDisabled() {
			return disablePublish;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Node {" + getId() + "}";
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Node#performDelete()
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FolderFactory folderFactory = (FolderFactory) t.getObjectFactory(Folder.class);

			folderFactory.deleteNode(this);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Node#getLastPublishTimestamp()
		 */
		public int getLastPublishTimestamp() throws NodeException {
			final List<Integer> result = new Vector<Integer>();

			DBUtils.executeStatement("SELECT pdate FROM node_pdate WHERE node_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, getId());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					if (rs.next()) {
						result.add(rs.getInt("pdate"));
					}
				}
			});

			int ret = -1;

			if (!result.isEmpty()) {
				if (result.get(0) > 0) {
					ret = result.get(0);
				}
			}

			return ret;
		}

		@Override
		public void setLastPublishTimestamp() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			final List<Boolean> found = new ArrayList<Boolean>(1);

			DBUtils.executeStatement("SELECT node_id FROM node_pdate WHERE node_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, getId());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					if (rs.next()) {
						found.add(true);
					} else {
						found.add(false);
					}
				}
			});
			if (found.get(0)) {
				// update
				DBUtils.executeUpdate("UPDATE node_pdate SET pdate = ? WHERE node_id = ?", new Object[] { t.getUnixTimestamp(), getId() });
			} else {
				// insert
				DBUtils.executeInsert("INSERT INTO node_pdate (pdate, node_id) VALUES (?, ?)", new Object[] { t.getUnixTimestamp(), getId() });
			}
		}

		public HashMap getOrderedNodeLanguageIds() throws NodeException {
			PreparedStatement st = null;
			ResultSet res = null;
			Transaction t = TransactionManager.getCurrentTransaction();

			try {

				st = t.prepareStatement("SELECT sortorder, contentgroup_id FROM node_contentgroup " + "WHERE node_id = ? ORDER BY sortorder ASC");
				st.setInt(1, ObjectTransformer.getInt(getId(), -1));

				res = st.executeQuery();

				HashMap nodeCgs = new HashMap();

				while (res.next()) {
					nodeCgs.put(new Integer(res.getInt("contentgroup_id")), new Integer(res.getInt("sortorder")));
				}
				return nodeCgs;
			} catch (Exception e) {
				logger.error("Unable to retrieve node_contentgroups", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(st);
			}
			return null;
		}

		/**
		 * load language ids (if not done before)
		 * @throws NodeException
		 */
		private synchronized void loadLanguageIds() throws NodeException {
			if (languageIds == null && !isEmptyId(getId())) {
				Transaction t = TransactionManager.getCurrentTransaction();

				Integer nodeId = getId();

				// for multichannelling, we get the language ids from the master node
				if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && isChannel()) {
					List<Node> masterNodes = getMasterNodes();

					if (!masterNodes.isEmpty()) {
						nodeId = masterNodes.get(masterNodes.size() - 1).getId();
					}
				}

				final Integer finalNodeId = nodeId;
				languageIds = DBUtils.select(
						"SELECT contentgroup_id id FROM node_contentgroup WHERE node_id = ? ORDER BY sortorder",
						ps -> ps.setInt(1, finalNodeId), DBUtils.IDLIST);
				languageIdsMD5 = StringUtils.md5(StringUtils.merge(languageIds.toArray(), ","));
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Node#getLanguages()
		 */
		public List<ContentLanguage> getLanguages() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && isChannel()) {
				List<Node> masterNodes = getMasterNodes();
				if (!masterNodes.isEmpty()) {
					return masterNodes.get(masterNodes.size() - 1).getLanguages();
				}
			}

			// load the ids (if not done before)
			loadLanguageIds();

			return t.getObjects(ContentLanguage.class, languageIds);
		}

		@Override
		public String getLanguagesMD5() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && isChannel()) {
				List<Node> masterNodes = getMasterNodes();
				if (!masterNodes.isEmpty()) {
					return masterNodes.get(masterNodes.size() - 1).getLanguagesMD5();
				}
			}

			loadLanguageIds();
			return languageIdsMD5;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Node#getContentrepositoryId()
		 */
		public Integer getContentrepositoryId() {
			return contentrepositoryId;
		}

		@Override
		public ContentRepository getContentRepository() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(ContentRepository.class, contentrepositoryId);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Node#getEditorversion()
		 */
		public int getEditorversion() {
			return editorversion;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Node#getUrlRenderWayPages()
		 */
		public int getUrlRenderWayPages() {
			return urlRenderWayPages;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Node#getUrlRenderWayFiles()
		 */
		public int getUrlRenderWayFiles() {
			return urlRenderWayFiles;
		}

		@Override
		public boolean isOmitPageExtension() throws NodeException {
			if (isChannel()) {
				return getMaster().isOmitPageExtension();
			} else {
				return omitPageExtension;
			}
		}

		@Override
		public PageLanguageCode getPageLanguageCode() throws NodeException {
			if (isChannel()) {
				return getMaster().getPageLanguageCode();
			} else {
				return pageLanguageCode;
			}
		}

		@Override
		public String getMeshPreviewUrl() {
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
				return meshPreviewUrl;
			} else {
				return "";
			}
		}

		@Override
		public String getMeshPreviewUrlProperty() {
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
				return meshPreviewUrlProperty;
			} else {
				return "";
			}
		}

		@Override
		public boolean isInsecurePreviewUrl() {
			return insecurePreviewUrl;
		}

		@Override
		public String getMeshProjectName() {
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
				return meshProjectName;
			} else {
				return "";
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			// TODO implement this
			return null;
		}

		@Override
		public List<Node> getMasterNodes() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// use the prepared NodeStructure if available
			PublishData publishData = t.getPublishData();
			if (publishData != null) {
				return publishData.getMasterNodes(this);
			}

			try {
				// avoid multichannelling fallback here
				t.setDisableMultichannellingFlag(true);

				List<Node> masterNodes = new Vector<Node>();
				Folder masterFolder = getFolder().getChannelMaster();

				while (masterFolder != null) {
					masterNodes.add(masterFolder.getNode());
					masterFolder = masterFolder.getChannelMaster();
				}

				return masterNodes;
			} finally {
				// restore the old value for avoiding multichannelling fallback here
				t.resetDisableMultichannellingFlag();
			}
		}

		@Override
		public Collection<Node> getChannels() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// use the prepared Publish data if available
			PublishData publishData = t.getPublishData();
			if (publishData != null) {
				return publishData.getChannels(this);
			}

			Collection<Node> channels = new Vector<Node>();
			PreparedStatement pst = null;
			ResultSet res = null;

			try {
				pst = t.prepareStatement("SELECT node_id FROM folder WHERE master_id = ?");
				pst.setObject(1, folderId);
				res = pst.executeQuery();

				while (res.next()) {
					//TODO Don't load objects within a result stream. Non mysql drivers might cry
					channels.add(t.getObject(Node.class, res.getInt("node_id"), -1, false));
				}

				return channels;
			} catch (SQLException e) {
				throw new NodeException("Error while getting channels of {" + this + "}", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		}

		@Override
		public Collection<Node> getAllChannels() throws NodeException {
			// first the the direct channels
			Collection<Node> allChannels = new Vector<Node>();
			Collection<Node> channels = getChannels();

			allChannels.addAll(channels);

			// and for all channels add the subchannels as well
			for (Node node : channels) {
				allChannels.addAll(node.getAllChannels());
			}

			return allChannels;
		}

		@Override
		public Collection<File> getOnlineFiles() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;
			ResultSet res = null;
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

			// check whether multichannelling is supported and this node is a
			// channel
			boolean multiChannelling = prefs.isFeature(Feature.MULTICHANNELLING);

			// check whether files may be offline in this node
			boolean autoOffline = prefs.isFeature(Feature.CONTENTFILE_AUTO_OFFLINE, this);

			try {
				if (multiChannelling) {
					// we will get all contentfiles from this channel and all master nodes
					List<Node> masterNodes = getMasterNodes();
					StringBuffer sql = new StringBuffer();

					sql.append("SELECT contentfile.id, contentfile.channelset_id, contentfile.channel_id, contentfile.mc_exclude, contentfile_disinherit.channel_id disinherited_node FROM contentfile ");
					sql.append("LEFT JOIN contentfile_disinherit on contentfile.id = contentfile_disinherit.contentfile_id ");
					sql.append("LEFT JOIN folder ON contentfile.folder_id = folder.id ");
					sql.append("WHERE folder.node_id = ? AND contentfile.channel_id IN (");
					sql.append(StringUtils.repeat("?", masterNodes.size() + 2, ","));
					sql.append(")");
					pst = t.prepareStatement(sql.toString());

					int pCounter = 1;

					// as node_id, use the original master node
					if (isChannel()) {
						pst.setObject(pCounter++, masterNodes.get(masterNodes.size() - 1).getId());
					} else {
						pst.setObject(pCounter++, getId());
					}

					// always search for files without channel_id
					pst.setObject(pCounter++, 0);
					// ... and belonging to this channel
					pst.setObject(pCounter++, getId());
					// ... and all channels in above this channel
					for (Node node : masterNodes) {
						pst.setObject(pCounter++, node.getId());
					}

					res = pst.executeQuery();

					// prepare the objectlist
					MultiChannellingFallbackList objectList = new MultiChannellingFallbackList(this);

					// fill in all the contentfiles
					while (res.next()) {
						objectList.addObject(res.getInt("id"), res.getInt("channelset_id"), res.getInt("channel_id"), res.getBoolean("mc_exclude"), res.getInt("disinherited_node"));
					}

					// now load all the files and return them
					// TODO check whether this will load images and files correctly
					List<File> files = new Vector<File>(t.getObjects(File.class, objectList.getObjectIds()));

					// if files may be offline, we need to check for that
					if (autoOffline) {
						FileListForNode preparedData = FileOnlineStatus.prepareForNode(this);

						for (Iterator<File> iter = files.iterator(); iter.hasNext();) {
							File f = iter.next();

							if (!preparedData.isOnline(f)) {
								iter.remove();
							}
						}
					}

					return files;
				} else {
					// just get the contentfiles which belong to this node
					StringBuffer sql = new StringBuffer();

					sql.append("SELECT contentfile.id FROM contentfile ");
					if (autoOffline) {
						sql.append("LEFT JOIN contentfile_online ON contentfile.id = contentfile_online.contentfile_id ");
						sql.append("WHERE contentfile_online.node_id = ?");
					} else {
						sql.append("LEFT JOIN folder ON contentfile.folder_id = folder.id ");
						sql.append("WHERE folder.node_id = ?");
					}

					pst = t.prepareStatement(sql.toString());
					pst.setObject(1, getId());

					res = pst.executeQuery();

					Collection<Integer> objectIds = new Vector<Integer>();

					while (res.next()) {
						objectIds.add(res.getInt("id"));
					}

					return t.getObjects(File.class, objectIds);
				}
			} catch (SQLException e) {
				throw new NodeException("Error while getting files for {" + this + "}", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		}

		@Override
		public Collection<Folder> getLocalChannelFolders() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;
			ResultSet res = null;

			try {
				Collection<Integer> localFolderIds = new Vector<Integer>();

				pst = t.prepareStatement("SELECT id FROM folder WHERE channel_id = ?");
				pst.setObject(1, getId());

				res = pst.executeQuery();
				while (res.next()) {
					localFolderIds.add(res.getInt("id"));
				}

				// get the folders
				List<Folder> folders = new Vector<Folder>(t.getObjects(Folder.class, localFolderIds));

				// now we sort the folders, so that the child folders always come before their parent
				Collections.sort(folders, new Comparator<Folder>() {
					public int compare(Folder f1, Folder f2) {
						try {
							return comparePaths(getPath(f2), getPath(f1));
						} catch (NodeException e) {
							return 0;
						}
					}

					/**
					 * Compare the given paths:
					 * If one path is fully contained in the other, the shorter path is smaller.
					 * Otherwise compare the first different object
					 * @param path1 path1
					 * @param path2 path2
					 * @return
					 */
					protected int comparePaths(List<Integer> path1, List<Integer> path2) {
						// iterate through all elements (as long as elements exist in both lists)
						for (int i = 0; i < Math.min(path1.size(), path2.size()); i++) {
							Integer id1 = path1.get(i);
							Integer id2 = path2.get(i);

							// when we found one different gid, we compare them and are done
							if (!id1.equals(id2)) {
								return id1.compareTo(id2);
							}
						}

						// when we come here, we found no different elements in the paths, so just compare the lengths
						return path1.size() - path2.size();
					}

					/**
					 * Get the path to the mother for the given folder
					 * @param folder folder
					 * @return path
					 * @throws NodeException
					 */
					protected List<Integer> getPath(
							Folder folder) throws NodeException {
						List<Integer> path = new Vector<Integer>();

						while (folder != null) {
							folder = folder.getMaster();
							path.add(ObjectTransformer.getInteger(folder.getId(), null));
							folder = folder.getMother();
						}
						// reverse the path (top mother should be the first element)
						Collections.reverse(path);
						return path;
					}
				});

				return folders;
			} catch (SQLException e) {
				throw new NodeException("Error while getting local channel folders for {" + this + "}", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		}

		@Override
		public Collection<Page> getLocalChannelPages() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;
			ResultSet res = null;

			try {
				Collection<Integer> localPageIds = new Vector<Integer>();

				pst = t.prepareStatement("SELECT id FROM page WHERE channel_id = ?");
				pst.setObject(1, getId());

				res = pst.executeQuery();
				while (res.next()) {
					localPageIds.add(res.getInt("id"));
				}

				return t.getObjects(Page.class, localPageIds);
			} catch (SQLException e) {
				throw new NodeException("Error while getting local channel pages for {" + this + "}", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		}

		@Override
		public Collection<Template> getLocalChannelTemplates() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;
			ResultSet res = null;

			try {
				Collection<Integer> localTemplateIds = new Vector<Integer>();

				pst = t.prepareStatement("SELECT id FROM template WHERE channel_id = ?");
				pst.setObject(1, getId());

				res = pst.executeQuery();
				while (res.next()) {
					localTemplateIds.add(res.getInt("id"));
				}

				return t.getObjects(Template.class, localTemplateIds);
			} catch (SQLException e) {
				throw new NodeException("Error while getting local channel templates for {" + this + "}", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		}

		@Override
		public Collection<File> getLocalChannelFiles() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;
			ResultSet res = null;

			try {
				Collection<Integer> localFileIds = new Vector<Integer>();

				pst = t.prepareStatement("SELECT id FROM contentfile WHERE channel_id = ?");
				pst.setObject(1, getId());

				res = pst.executeQuery();
				while (res.next()) {
					localFileIds.add(res.getInt("id"));
				}

				return t.getObjects(File.class, localFileIds);
			} catch (SQLException e) {
				throw new NodeException("Error while getting local channel files for {" + this + "}", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		}

		/**
		 * Load the construct ids
		 * @return list of construct ids
		 * @throws NodeException
		 */
		protected List<Integer> loadConstructIds() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;
			ResultSet res = null;
			List<Integer> constructIds = new Vector<Integer>();

			try {
				Object id = getId();

				// TODO multichannelling support
				pst = t.prepareStatement("SELECT construct_id FROM construct_node WHERE node_id = ?");
				pst.setObject(1, id);
				res = pst.executeQuery();

				while (res.next()) {
					if (!constructIds.contains(res.getInt("construct_id"))) {
						constructIds.add(res.getInt("construct_id"));
					}
				}

				return constructIds;
			} catch (SQLException e) {
				throw new NodeException("Error while loading constructs for node", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		}

		/**
		 * Load the construct ids
		 * @return list of construct ids
		 * @throws NodeException
		 */
		protected List<Integer> loadObjectTagDefinitionIds() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;
			ResultSet res = null;
			List<Integer> otdIds = new Vector<Integer>();

			try {
				Object id = getId();

				pst = t.prepareStatement("SELECT p.objtag_id FROM objprop_node n JOIN objprop p ON n.objprop_id = p.id WHERE n.node_id = ?");
				pst.setObject(1, id);
				res = pst.executeQuery();

				while (res.next()) {
					if (!otdIds.contains(res.getInt("p.objtag_id"))) {
						otdIds.add(res.getInt("p.objtag_id"));
					}
				}

				return otdIds;
			} catch (SQLException e) {
				throw new NodeException("Error while loading object tag definitions for node", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		}

		@Override
		public List<Construct> getConstructs() throws NodeException {
			if (isChannel()) {
				List<Node> masterNodes = getMasterNodes();

				if (!masterNodes.isEmpty()) {
					return masterNodes.get(masterNodes.size() - 1).getConstructs();
				} else {
					throw new NodeException("Error while getting constructs for " + this + ": Channel does not have a master node.");
				}
			} else {
				Transaction t = TransactionManager.getCurrentTransaction();

				return t.getObjects(Construct.class, loadConstructIds());
			}
		}

		@Override
		public ContentNodeDate getCDate() {
			return cdate;
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			SystemUser creator = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);

			// check for data consistency
			assertNodeObjectNotNull(creator, creatorId, "Creator");
			return creator;
		}

		@Override
		public ContentNodeDate getEDate() {
			return edate;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			SystemUser editor = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);

			// check for data consistency
			assertNodeObjectNotNull(editor, editorId, "Editor");
			return editor;
		}

		@Override
		public Folder getDefaultFileFolder() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(Folder.class, defaultFileFolderId);
		}

		@Override
		public Folder getDefaultImageFolder() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(Folder.class, defaultImageFolderId);
		}

		@Override
		public List<Feature> getFeatures() throws NodeException {
			final Set<Feature> features = new HashSet<Feature>();

			DBUtils.executeStatement("SELECT feature FROM node_feature WHERE node_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, getId());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						String featureString = rs.getString("feature");
						Feature feature = Feature.getByName(featureString);

						if (feature == null) {
							if (logger.isDebugEnabled()) {
								logger.debug("Ignoring unknown feature " + featureString + " for " + FactoryNode.this);
							}
						} else if (!feature.isPerNode()) {
							if (logger.isDebugEnabled()) {
								logger.debug("Ignoring feature " + featureString + " for " + FactoryNode.this + ", because it cannot be activated per node");
							}
						} else if (!feature.isActivated()) {
							if (logger.isDebugEnabled()) {
								logger.debug("Ignoring feature " + featureString + " for " + FactoryNode.this + ", because it is generally deactivated");
							}
						} else {
							features.add(feature);
						}
					}
				}
			});

			// for channels, get the inheritable features from the master node
			if (isChannel()) {
				Set<Feature> inheritableMasterFeatures = getMaster().getFeatures().stream().filter(Feature::isInheritable).collect(Collectors.toSet());
				features.addAll(inheritableMasterFeatures);
			}

			return new ArrayList<>(features);
		}

		@Override
		public void activateFeature(Feature feature) throws NodeException {
			if (feature == null) {
				throw new NodeException("Cannot activate null as feature for " + this);
			}
			// check whether the feature can be activated per node
			if (!feature.isPerNode()) {
				throw new NodeException(
						"Cannot activate feature " + feature.toString().toLowerCase() + " for " + this + ": feature cannot be activated per node");
			}

			// check whether the feature is generally active
			if (!feature.isActivated()) {
				throw new NodeException(
						"Cannot activate feature " + feature.toString().toLowerCase() + " for " + this + ": feature is not available");
			}

			// check whether the feature already is activated
			if (getFeatures().contains(feature)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Feature " + feature.toString().toLowerCase() + " is already activated for " + this);
				}
				return;
			}

			DBUtils.executeUpdate("INSERT INTO node_feature (node_id, feature) VALUES (?, ?)", new Object[] { getId(), feature.toString().toLowerCase() });
		}

		@Override
		public void deactivateFeature(Feature feature) throws NodeException {
			if (feature == null) {
				throw new NodeException("Cannot deactivate null as feature for " + this);
			}
			// check whether the feature can be activated per node
			if (!feature.isPerNode()) {
				throw new NodeException(
						"Cannot deactivate feature " + feature.toString().toLowerCase() + " for " + this + ": feature cannot be deactivated per node");
			}

			// check whether the feature is generally active
			if (!feature.isActivated()) {
				throw new NodeException(
						"Cannot activate feature " + feature.toString().toLowerCase() + " for " + this + ": feature is not available");
			}

			// check whether the feature is activated at all
			if (!getFeatures().contains(feature)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Feature " + feature.toString().toLowerCase() + " is not activated for " + this);
				}
				return;
			}

			DBUtils.executeUpdate("DELETE FROM node_feature WHERE node_id = ? AND feature = ?", new Object[] { getId(), feature.toString().toLowerCase() });
		}

		@Override
		public Map<Integer, Integer> purgeWastebin() throws NodeException {
			Map<Integer, Integer> result = new HashMap<>();

			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				wastebinLogger.info("Start purging wastebin for " + this + " (" + getFolder().getName() + ")");
				Transaction t = TransactionManager.getCurrentTransaction();
				NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

				int deletedBefore = 0;
				if (prefs.isFeature(Feature.WASTEBIN, getMaster())) {
					int wastebinMaxage = getWastebinMaxage(this);
					if (wastebinMaxage > 0) {
						deletedBefore = t.getUnixTimestamp() - wastebinMaxage;
					}
					wastebinLogger.info("Feature activated, maxage is " + wastebinMaxage
							+ (wastebinMaxage > 0 ? " (" + DurationFormatUtils.formatDurationWords(wastebinMaxage * 1000L, true, true) + ")" : ""));
				} else {
					deletedBefore = t.getUnixTimestamp();
					wastebinLogger.info("Feature deactivated, purging everything");
				}

				if (deletedBefore == 0) {
					wastebinLogger.info("maxage is 0, no purging");
					return result;
				}

				List<Page> pages = getPagesToPurge(this, deletedBefore);
				int pageCount = pages.size();
				for (Page page : pages) {
					page.delete(true);
					t.commit(false);
				}
				wastebinLogger.info("Purged " + pageCount + (pageCount == 1 ? " page": " pages"));
				result.put(Page.TYPE_PAGE_INTEGER, pageCount);

				List<File> files = getFilesToPurge(this, deletedBefore);
				int fileCount = files.size();
				for (File file : files) {
					file.delete(true);
					t.commit(false);
				}
				wastebinLogger.info("Purged " + fileCount + (fileCount == 1 ? " file" : " files"));
				result.put(File.TYPE_FILE_INTEGER, fileCount);

				List<Folder> folders = getFoldersToPurge(this, deletedBefore);
				int folderCount = folders.size();
				folders = new ArrayList<Folder>(reduce(folders, ReductionType.PARENT));
				for (Folder folder : folders) {
					folder.delete(true);
					t.commit(false);
				}
				wastebinLogger.info("Purged " + folderCount + (folderCount == 1 ? " folder" : " folders"));
				result.put(Folder.TYPE_FOLDER_INTEGER, folderCount);
			}

			return result;
		}

		@Override
		public void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId) throws NodeException {
			super.triggerEvent(object, property, eventMask, depth, channelId);
			// when the node's host or pub_dir changes, we need to dirt all pages that are present in the publish table
			List<Integer> pageIds = new ArrayList<>();
			if (Events.isEvent(eventMask, Events.UPDATE) && !ObjectTransformer.isEmpty(property)
					&& !Collections.disjoint(Arrays.asList(property), Arrays.asList("host", "pub_dir"))) {
				Transaction t = TransactionManager.getCurrentTransaction();

				DBUtils.executeStatement("SELECT page_id FROM publish WHERE node_id = ? AND active = ?", Transaction.SELECT_STATEMENT, p -> {
					p.setInt(1, getId());
					p.setInt(2, 1);
				}, rs -> {
					while (rs.next()) {
						pageIds.add(rs.getInt("page_id"));
					}
				});

				for (Page page : t.getObjects(Page.class, pageIds)) {
					// dirt the page with a dummy attribute, so that the content will not be rendered
					PublishQueue.dirtObject(page, Action.DEPENDENCY, channelId, DUMMY_DIRT_ATTRIBUTE);
				}
			}
		}

		@Override
		public Set<Template> getTemplates() throws NodeException {
			if (isChannel()) {
				return getMaster().getTemplates();
			}
			if (templateIds == null) {
				templateIds = DBUtils.select("SELECT template_id id FROM template_node WHERE node_id = ?", ps -> ps.setInt(1, getId()), DBUtils.IDS);
			}

			return new HashSet<>(TransactionManager.getCurrentTransaction().getObjects(Template.class, templateIds));
		}

		@Override
		public void addTemplate(Template template) throws NodeException {
			if (isChannel()) {
				getMaster().addTemplate(template);
			} else {
				Transaction t = TransactionManager.getCurrentTransaction();

				DBUtils.executeStatement("INSERT IGNORE INTO template_node (template_id, node_id) VALUES (?, ?)", Transaction.UPDATE_STATEMENT, st -> {
					st.setInt(1, template.getMaster().getId());
					st.setInt(2, getId());
				});
				templateIds = null;
				t.dirtObjectCache(Node.class, getId());
			}
		}

		@Override
		public void removeTemplate(Template template) throws NodeException {
			if (isChannel()) {
				getMaster().removeTemplate(template);
			} else {
				Transaction t = TransactionManager.getCurrentTransaction();

				DBUtils.executeStatement("DELETE FROM template_node WHERE template_id = ? AND node_id = ?", Transaction.DELETE_STATEMENT, st -> {
					st.setInt(1, template.getMaster().getId());
					st.setInt(2, getId());
				});
				templateIds = null;
				t.dirtObjectCache(Node.class, getId());
			}
		}

		@Override
		public void addConstruct(Construct construct) throws NodeException {
			if (isChannel()) {
				getMaster().addConstruct(construct);
			} else {
				Transaction t = TransactionManager.getCurrentTransaction();

				DBUtils.executeStatement("INSERT IGNORE INTO construct_node (construct_id, node_id) VALUES (?, ?)", Transaction.UPDATE_STATEMENT, st -> {
					st.setInt(1, construct.getId());
					st.setInt(2, getId());
				});
				t.dirtObjectCache(Construct.class, construct.getId());
			}
		}

		@Override
		public void removeConstruct(Construct construct) throws NodeException {
			if (isChannel()) {
				getMaster().removeConstruct(construct);
			} else {
				Transaction t = TransactionManager.getCurrentTransaction();

				DBUtils.executeStatement("DELETE FROM construct_node WHERE construct_id = ? AND node_id = ?", Transaction.DELETE_STATEMENT, st -> {
					st.setInt(1, construct.getId());
					st.setInt(2, getId());
				});
				t.dirtObjectCache(Construct.class, construct.getId());
			}
		}
		@Override
		public void addObjectTagDefinition(ObjectTagDefinition def) throws NodeException {
			if (isChannel()) {
				getMaster().addObjectTagDefinition(def);
			} else {
				Transaction t = TransactionManager.getCurrentTransaction();

				DBUtils.executeStatement("INSERT IGNORE INTO objprop_node (objprop_id, node_id) VALUES (?, ?)", Transaction.UPDATE_STATEMENT, st -> {
					st.setInt(1, def.getObjectPropId());
					st.setInt(2, getId());
				});
				t.dirtObjectCache(ObjectTagDefinition.class, def.getId());
			}
		}

		@Override
		public void removeObjectTagDefinition(ObjectTagDefinition def) throws NodeException {
			if (isChannel()) {
				getMaster().removeObjectTagDefinition(def);
			} else {
				Transaction t = TransactionManager.getCurrentTransaction();

				DBUtils.executeStatement("DELETE FROM objprop_node WHERE objprop_id = ? AND node_id = ?", Transaction.DELETE_STATEMENT, st -> {
					st.setInt(1, def.getObjectPropId());
					st.setInt(2, getId());
				});
				t.dirtObjectCache(ObjectTagDefinition.class, def.getId());
			}
		}

		@Override
		public List<ObjectTagDefinition> getObjectTagDefinitions() throws NodeException {
			if (isChannel()) {
				List<Node> masterNodes = getMasterNodes();

				if (!masterNodes.isEmpty()) {
					return masterNodes.get(masterNodes.size() - 1).getObjectTagDefinitions();
				} else {
					throw new NodeException("Error while getting object tag definitions for " + this + ": Channel does not have a master node.");
				}
			} else {
				Transaction t = TransactionManager.getCurrentTransaction();

				return t.getObjects(ObjectTagDefinition.class, loadObjectTagDefinitionIds());
			}
		}

		@Override
		public List<? extends ExtensibleObjectService<Node>> getServices() {
			return StreamSupport.stream(nodeFactoryServiceLoader.spliterator(), false).collect(Collectors.toList());
		}
	}

	/**
	 * Factory Implementation of Editable Node
	 */
	private static class EditableFactoryNode extends FactoryNode {

		/**
		 * true if the object has been modified
		 */
		private boolean modified = false;

		/**
		 * Editable root folder
		 */
		protected Folder folder;

		/**
		 * true for channels, false for normal nodes
		 */
		protected boolean channel;

		/**
		 * List of assigned constructs
		 */
		protected List<Construct> assignedConstructs;

		/**
		 * List of assigned object tag definitions
		 */
		protected List<ObjectTagDefinition> assignedObjectTagDefinitions;

		/**
		 * List of assigned languages
		 */
		protected List<ContentLanguage> languages;

		/**
		 * Create an empty instance
		 * @param info
		 */
		protected EditableFactoryNode(NodeObjectInfo info) {
			super(info);
		}

		/**
		 * Create an editable copy of the node
		 * @param node node
		 * @param info info
		 * @param asNew true if creating as new instance, false for an editable copy
		 * @throws NodeException
		 */
		protected EditableFactoryNode(FactoryNode node, NodeObjectInfo info, boolean asNew) throws NodeException {
			super(asNew ? null : node.getId(), info, getDataMap(node), asNew ? -1 : node.getUdate(), asNew ? null : node.getGlobalId());
			if (asNew) {
				folderId = 0;
				modified = true;
				folder = TransactionManager.getCurrentTransaction().createObject(Folder.class);
				// TODO this is not always true
				channel = true;
			} else {
				channel = super.isChannel();
				getFolder();
			}
		}

		@Override
		public boolean isChannel() throws NodeException {
			return channel;
		}

		@Override
		public Folder getFolder() throws NodeException {
			if (folder == null) {
				folder = super.getFolder();
			}
			return folder;
		}

		@Override
		public void setContentrepositoryId(Integer contentrepositoryId) throws ReadOnlyException {
			if (ObjectTransformer.getInt(this.contentrepositoryId, -1) != ObjectTransformer.getInt(contentrepositoryId, -1)) {
				this.contentrepositoryId = contentrepositoryId;
				this.modified = true;
			}
		}

		@Override
		public void setContentmapKeyword(String contentmapKeyword) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.cnMapKeyname, contentmapKeyword)) {
				this.cnMapKeyname = contentmapKeyword;
				this.modified = true;
			}
		}

		@Override
		public void setEditorversion(int editorversion) throws ReadOnlyException {
			if (this.editorversion != editorversion) {
				this.editorversion = editorversion;
				this.modified = true;
			}
		}

		@Override
		public void setFolder(Folder folder) throws ReadOnlyException,
					NodeException {
			if (folder == null) {
				throw new NodeException("Cannot set root folder of node to null");
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			// if the folder is not new but not editable, get the editable folder
			if (!isEmptyId(folder.getId()) && !folder.getObjectInfo().isEditable()) {
				folder = t.getObject(Folder.class, folder.getId(), true);
			}
			if (this.folder != null && !isEmptyId(this.folder.getId()) && !this.folder.equals(folder)) {
				throw new NodeException("Cannot set root folder, folder already set to " + this.folder);
			}
			this.folder = folder;
		}

		@Override
		public void setHttps(boolean https) throws ReadOnlyException {
			if (this.https != https) {
				this.https = https;
				this.modified = true;
			}
		}

		@Override
		public void setPublishImageVariants(boolean publishImageVariants) throws ReadOnlyException {
			if (this.pubImgVariants != publishImageVariants) {
				this.pubImgVariants = publishImageVariants;
				this.modified = true;
			}
		}

		@Override
		public void setFtpHostname(String ftpHostname) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.ftpHost, ftpHostname)) {
				this.ftpHost = ftpHostname;
				this.modified = true;
			}
		}

		@Override
		public void setFtpLogin(String ftpLogin) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.ftpLogin, ftpLogin)) {
				this.ftpLogin = ftpLogin;
				this.modified = true;
			}
		}

		@Override
		public void setFtpPassword(String ftpPassword) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.ftpPw, ftpPassword)) {
				this.ftpPw = ftpPassword;
				this.modified = true;
			}
		}

		@Override
		public void setFtpSync(boolean ftpSync) throws ReadOnlyException {
			if (this.ftpSync != ftpSync) {
				this.ftpSync = ftpSync;
				this.modified = true;
			}
		}

		@Override
		public void setFtpWwwRoot(String ftpWwwRoot) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.ftpWwwRoot, ftpWwwRoot)) {
				this.ftpWwwRoot = ftpWwwRoot;
				this.modified = true;
			}
		}

		@Override
		public void setHostname(String hostname) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.host, hostname)) {
				this.host = hostname;
				this.modified = true;
			}
		}

		@Override
		public void setHostnameProperty(String hostProperty) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.hostProperty, hostProperty)) {
				this.hostProperty = hostProperty;
				this.modified = true;
				resolveHostnameProperty();
			}
		}

		@Override
		public void resolveHostnameProperty() throws ReadOnlyException {
			// if hostProperty is not empty, resolve and set host also
			if (!org.apache.commons.lang3.StringUtils.isBlank(this.hostProperty)) {
				String resolvedHost = substituteSingleProperty(this.hostProperty, Node.NODE_HOST_FILTER);
				if (!StringUtils.isEqual(this.host, resolvedHost)) {
					this.host = resolvedHost;
					this.modified = true;
				}
			}
		}

		@Override
		public void setPublishContentmap(boolean publishContentmap) throws ReadOnlyException {
			if (this.publishContentmap != publishContentmap) {
				this.publishContentmap        = publishContentmap;
				this.publishContentMapPages   = publishContentmap;
				this.publishContentMapFiles   = publishContentmap;
				this.publishContentMapFolders = publishContentmap;
				this.modified                 = true;
			}
		}

		@Override
		public void setPublishContentMapPages(boolean publish) throws ReadOnlyException {
			if (this.publishContentMapPages != publish) {
				this.publishContentMapPages = publish;
				this.modified = true;
			}
		}

		@Override
		public void setPublishContentMapFiles(boolean publish) throws ReadOnlyException {
			if (this.publishContentMapFiles != publish) {
				this.publishContentMapFiles = publish;
				this.modified = true;
			}
		}

		@Override
		public void setPublishContentMapFolders(boolean publish) throws ReadOnlyException {
			if (this.publishContentMapFolders != publish) {
				this.publishContentMapFolders = publish;
				this.modified = true;
			}
		}

		@Override
		public void setPublishDir(String publishDir) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.pubDir, publishDir)) {
				this.pubDir = publishDir;
				this.modified = true;
				// also set it to the binary publish dir (if not set differently)
				if (this.pubDirBin == null) {
					this.pubDirBin = publishDir;
				}
			}
		}

		@Override
		public void setBinaryPublishDir(String publishDir) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.pubDirBin, publishDir)) {
				this.pubDirBin = publishDir;
				this.modified = true;
			}
		}

		@Override
		public void setPubDirSegment(boolean pubDirSegment) throws ReadOnlyException {
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.PUB_DIR_SEGMENT)) {
				pubDirSegment = false;
			}
			if (this.pubDirSegment != pubDirSegment) {
				this.pubDirSegment = pubDirSegment;
				this.modified = true;
			}
		}

		@Override
		public void setPublishDisabled(boolean publishDisabled) throws ReadOnlyException {
			if (this.disablePublish != publishDisabled) {
				this.disablePublish = publishDisabled;
				this.modified = true;
			}
		}

		@Override
		public void setPublishFilesystem(boolean publishFilesystem) throws ReadOnlyException {
			if (this.publishFilesystem != publishFilesystem) {
				this.publishFilesystem      = publishFilesystem;
				this.publishFilesystemPages = publishFilesystem;
				this.publishFilesystemFiles = publishFilesystem;
				this.modified               = true;
			}
		}

		@Override
		public void setPublishFilesystemPages(boolean publish) throws ReadOnlyException {
			if (this.publishFilesystemPages != publish) {
				this.publishFilesystemPages = publish;
				this.modified = true;
			}
		}

		@Override
		public void setPublishFilesystemFiles(boolean publish) throws ReadOnlyException {
			if (this.publishFilesystemFiles != publish) {
				this.publishFilesystemFiles = publish;
				this.modified = true;
			}
		}

		@Override
		public void setUtf8(boolean utf8) throws ReadOnlyException {
			if (this.utf8 != utf8) {
				this.utf8 = utf8;
				this.modified = true;
			}
		}

		@Override
		public void setDefaultFileFolder(Folder folder) throws ReadOnlyException, NodeException {
			Integer folderId = null;

			if (folder != null) {
				folderId = folder.getMaster().getId();
			}

			if (ObjectTransformer.getInt(defaultFileFolderId, -1) != ObjectTransformer.getInt(folderId, -1)) {
				defaultFileFolderId = folderId;
				modified = true;
			}
		}

		@Override
		public void setDefaultImageFolder(Folder folder) throws ReadOnlyException, NodeException {
			Integer folderId = null;

			if (folder != null) {
				folderId = folder.getMaster().getId();
			}

			if (ObjectTransformer.getInt(defaultImageFolderId, -1) != ObjectTransformer.getInt(folderId, -1)) {
				defaultImageFolderId = folderId;
				modified = true;
			}
		}

		@Override
		public void setUrlRenderWayPages(int way) throws ReadOnlyException {
			if (this.urlRenderWayPages != way) {
				this.urlRenderWayPages = way;
				this.modified = true;
			}
		}

		@Override
		public void setUrlRenderWayFiles(int way) throws ReadOnlyException {
			if (this.urlRenderWayFiles != way) {
				this.urlRenderWayFiles = way;
				this.modified = true;
			}
		}

		@Override
		public void setOmitPageExtension(boolean omitPageExtension) throws NodeException {
			if (!isChannel()) {
				if (this.omitPageExtension != omitPageExtension) {
					this.omitPageExtension = omitPageExtension;
					this.modified = true;
				}
			}
		}

		@Override
		public void setPageLanguageCode(PageLanguageCode pageLanguageCode) throws NodeException {
			if (!isChannel()) {
				if (this.pageLanguageCode != pageLanguageCode) {
					this.pageLanguageCode = pageLanguageCode;
					this.modified = true;
				}
			}
		}

		@Override
		public void setMeshPreviewUrl(String url) throws ReadOnlyException {
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
				url = "";
			}
			if (!StringUtils.isEqual(this.meshPreviewUrl, url)) {
				this.meshPreviewUrl = url;
				this.modified = true;
			}
		}

		@Override
		public void setMeshPreviewUrlProperty(String urlProperty) throws ReadOnlyException {
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
				urlProperty = "";
			}
			if (!StringUtils.isEqual(this.meshPreviewUrlProperty, urlProperty)) {
				this.meshPreviewUrlProperty = urlProperty;
				this.modified = true;
				resolveMeshPreviewUrlProperty();
			}
		}

		@Override
		public void resolveMeshPreviewUrlProperty() throws ReadOnlyException {
			// if meshPreviewUrlProperty is not empty, resolve and set meshPreviewUrl also
			if (!org.apache.commons.lang3.StringUtils.isBlank(this.meshPreviewUrlProperty)) {
				String resolvedMeshPreviewUrl = substituteSingleProperty(this.meshPreviewUrlProperty, Node.NODE_PREVIEWURL_FILTER);
				if (!StringUtils.isEqual(this.meshPreviewUrl, resolvedMeshPreviewUrl)) {
					this.meshPreviewUrl = resolvedMeshPreviewUrl;
					this.modified = true;
				}
			}
		}

		@Override
		public void setInsecurePreviewUrl(boolean insecurePreviewUrl) {
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY) && this.insecurePreviewUrl != insecurePreviewUrl) {
				this.insecurePreviewUrl = insecurePreviewUrl;
				this.modified = true;
			}
		}

		@Override
		public void setMeshProjectName(String meshProjectName) throws ReadOnlyException {
			if (meshProjectName == null) {
				return;
			}
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
				meshProjectName = "";
			}
			if (!StringUtils.isEqual(this.meshProjectName, meshProjectName)) {
				this.meshProjectName = meshProjectName;
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException,
					NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = this.modified;
			boolean isNew = isEmptyId(getId());
			Node origNode = null;

			if (isNew) {
				setUtf8(true);
				setEditorversion(EDITOR_VERSION_ALOHA_EDITOR);
			} else {
				origNode = t.getObject(Node.class, getId());
			}
			Folder folder = getFolder();

			// if the node is a new channel, we take over the sync'able object properties
			if (isNew && NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)) {
				// the node is a channel, if the master of the root folder is different from the root folder
				Folder folderMaster = folder.getMaster();
				if (!Objects.equals(folderMaster, folder)) {
					for (Entry<String, ObjectTag> entry : folderMaster.getObjectTags().entrySet()) {
						String objectTagName = entry.getKey();
						ObjectTag tag = entry.getValue();
						ObjectTagDefinition definition = tag.getDefinition();
						if (definition != null && definition.isSyncChannelset()) {
							folder.getObjectTags().put(objectTagName, (ObjectTag) tag.copy());
						}
					}
				}
			}

			// save the node, if necessary
			if (isModified) {
				// set the editor data
				editorId = t.getUserId();
				edate = new ContentNodeDate(t.getUnixTimestamp());

				// for new objects, set creator data
				if (isNew) {
					creatorId = t.getUserId();
					cdate = new ContentNodeDate(t.getUnixTimestamp());
				}

				// make sure the folderId is not null
				if (folder != null) {
					folderId = folder.getId();
				}
				folderId = ObjectTransformer.getInteger(folderId, 0);

				// make sure default folder id's are not null
				defaultFileFolderId = ObjectTransformer.getInteger(defaultFileFolderId, 0);
				defaultImageFolderId = ObjectTransformer.getInteger(defaultImageFolderId, 0);

				meshPreviewUrl = ObjectTransformer.getString(meshPreviewUrl, "");
				meshPreviewUrlProperty = ObjectTransformer.getString(meshPreviewUrlProperty, "");
				meshProjectName = ObjectTransformer.getString(meshProjectName, "");
				hostProperty = ObjectTransformer.getString(hostProperty, "");

				// when the node is a channel, check whether the master node publishes into a MCCR,
				// and if so, set the same contentrepository to the channel
				// also set the pubDirSegment identical to the master
				Node master = getMaster();
				if (!master.equals(this)) {
					ContentRepository cr = master.getContentRepository();
					if (isEmptyId(contentrepositoryId)
							&& !ObjectTransformer.getBoolean(t.getAttributes().get(OMIT_CR_VERIFY), false) && cr != null
							&& cr.getCrType() == ContentRepositoryModel.Type.mccr) {
						contentrepositoryId = cr.getId();
					}
					if (!ObjectTransformer.getBoolean(t.getAttributes().get(OMIT_PUB_DIR_SEGMENT_VERIFY), false)) {
						pubDirSegment = master.isPubDirSegment();
					}
				}

				// before saving the node, we need to check whether pub_dir_segment can be changed (if it shall be changed)
				if (origNode != null && !origNode.isPubDirSegment() && isPubDirSegment()) {
					try (ChannelTrx cTrx = new ChannelTrx(this)) {
						Folder pubDirSegmentConflict = checkPubDirUniqueness(folder);
						if (pubDirSegmentConflict != null) {
							throw new NodeException("PubDirSegment cannot be used for node", "rest.node.conflict.publishDirSegment",
									I18NHelper.getPath(pubDirSegmentConflict));
						}
					}
				}

				saveFactoryObject(this);

				// we maybe need to correct the folder's pub_dirs
				if (origNode != null && origNode.isPubDirSegment() != isPubDirSegment()) {
					// we want to omit the verification checks for the pub_dir, because the folders would use the cached nodes, which do
					// not have the property pubDirSegment changed yet (we are setting correctly clean pub_dir's anyway, so no need for another check while saving)
					try (TrxAttribute omitPubDirSegmentVerifyTrx = new TrxAttribute(OMIT_PUB_DIR_SEGMENT_VERIFY, true)) {
						// for all folders that are not inherited, clean the publish directory
						try (ChannelTrx cTrx = new ChannelTrx(this)) {
							doForFoldersRecursive(folder, f -> {
								if (!f.isInherited()) {
									Folder editable = t.getObject(f, true);
									editable.setPublishDir(cleanPubDir(editable.getPublishDir(), isPubDirSegment(), true));
									editable.save();
								}
							});
						}

						if (!isChannel()) {
							for (Node c : getAllChannels()) {
								Node editableChannel = t.getObject(c, true);
								editableChannel.setPubDirSegment(isPubDirSegment());
								editableChannel.save();
							}
						}
					}
				}

				this.modified = false;
			}
			folder.setNodeId(getId());

			isModified |= folder.save();

			channel = Boolean.valueOf((getFolder().getChannelMaster() != null));

			if (isNew) {
				// when the node and its root folder are new, we need to set the root folder id to the node now
				DBUtils.executeUpdate("UPDATE node SET folder_id = ? WHERE id = ?", new Object[] {
					folder.getId(), getId()
				});
				folderId = folder.getId();

				if (isPubDirSegment()) {
					// set the publish dir again. This will clean the publish directory according to the node`s pubDirSegment setting
					t.dirtObjectCache(Node.class, getId());
					// remove the owningNode stored in the folder, because that node instance will not have the root folder ID set (which would cause subsequent errors)
					((EditableFactoryFolder)folder).owningNode = null;
					folder.setPublishDir(folder.getPublishDir());
					folder.save();
				}

				// set initial permissions
				SystemUser user = t.getObject(SystemUser.class, t.getUserId());
				// get the groups (and all mother groups)
				List<UserGroup> groups = user.getAllGroupsWithParents();

				PermHandler.setPermissions(Folder.TYPE_FOLDER, ObjectTransformer.getInt(folder.getId(), -1), groups,
						TypePerms.node.pattern(StringUtils.repeat("1", 32), true));
				PermHandler.setPermissions(folder.isMaster() ? Node.TYPE_NODE : Node.TYPE_CHANNEL,
						ObjectTransformer.getInt(folder.getId(), -1), groups,
						TypePerms.node.pattern(StringUtils.repeat("1", 32), true));
			}

			// change construct assignment
			if (assignedConstructs != null) {
				List<Integer> currentAssignedConstructs = loadConstructIds();
				List<Integer> toAssign = new Vector<Integer>();
				List<Integer> toUnassign = new Vector<Integer>(currentAssignedConstructs);

				for (Construct construct : assignedConstructs) {
					if (!currentAssignedConstructs.contains(construct.getId())) {
						// construct must be assigned, but is not
						toAssign.add(ObjectTransformer.getInteger(construct.getId(), null));
					}
					// construct must not be unassigned
					toUnassign.remove(ObjectTransformer.getInteger(construct.getId(), null));
				}

				if (!toUnassign.isEmpty()) {
					DBUtils.executeMassStatement("DELETE FROM construct_node WHERE node_id = ? AND construct_id IN", toUnassign, 2, new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, ObjectTransformer.getInt(getId(), -1));
						}
					});
				}
				if (!toAssign.isEmpty()) {
					for (Integer id : toAssign) {
						DBUtils.executeInsert("INSERT IGNORE INTO construct_node (node_id, construct_id) VALUES (?, ?)", new Object[] { getId(), id });
					}
				}
			}

			// change construct assignment
			if (assignedObjectTagDefinitions != null) {
				List<Integer> currentAssignedObjectTagDefinitions = loadConstructIds();
				List<Integer> toAssign = new Vector<Integer>();
				List<Integer> toUnassign = new Vector<Integer>(currentAssignedObjectTagDefinitions);

				for (ObjectTagDefinition otd : assignedObjectTagDefinitions) {
					if (!currentAssignedObjectTagDefinitions.contains(otd.getId())) {
						// objprop must be assigned, but is not
						toAssign.add(ObjectTransformer.getInteger(otd.getId(), null));
					}
					// objprop must not be unassigned
					toUnassign.remove(ObjectTransformer.getInteger(otd.getId(), null));
				}

				if (!toUnassign.isEmpty()) {
					DBUtils.executeMassStatement("DELETE FROM objprop_node WHERE node_id = ? AND objprop_id IN", toUnassign, 2, new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, ObjectTransformer.getInt(getId(), -1));
						}
					});
				}
				if (!toAssign.isEmpty()) {
					for (Integer id : toAssign) {
						DBUtils.executeInsert("INSERT IGNORE INTO objprop_node (node_id, objprop_id) VALUES (?, ?)", new Object[] { getId(), id });
					}
				}
			}

			boolean clearCache = false;
			// change language assignment
			if (languages != null) {
				// generate maps of language order
				Map<Integer, Integer> oldOrderMap = new HashMap<Integer, Integer>();
				Map<Integer, Integer> newOrderMap = new HashMap<Integer, Integer>();
				int sortOrder = 1;
				if (languageIds != null) {
					for (Integer id : languageIds) {
						if (!oldOrderMap.containsKey(id)) {
							oldOrderMap.put(id, sortOrder++);
						}
					}
				}

				sortOrder = 1;
				for (ContentLanguage language : languages) {
					Integer id = ObjectTransformer.getInteger(language.getId(), 0);
					if (!newOrderMap.containsKey(id)) {
						newOrderMap.put(id, sortOrder++);
					}
				}

				// now change the setting
				for (Map.Entry<Integer, Integer> entry : oldOrderMap.entrySet()) {
					Integer id = entry.getKey();
					Integer oldSortOrder = entry.getValue();

					if (newOrderMap.containsKey(id)) {
						Integer newSortOrder = newOrderMap.get(id);
						if (!ObjectTransformer.equals(oldSortOrder, newSortOrder)) {
							DBUtils.executeUpdate("UPDATE node_contentgroup SET sortorder = ? WHERE node_id = ? AND contentgroup_id = ?", new Object[] {newSortOrder, getId(), id});
							clearCache = true;
						}
					} else {
						// delete the entry
						DBUtils.executeUpdate("DELETE FROM node_contentgroup WHERE node_id = ? AND contentgroup_id = ?", new Object[] { getId(), id });
						t.dirtObjectCache(ContentLanguage.class, id, true);
						clearCache = true;
					}
				}
				for (Map.Entry<Integer, Integer> entry : newOrderMap.entrySet()) {
					Integer id = entry.getKey();
					Integer newSortOrder = entry.getValue();

					if (!oldOrderMap.containsKey(id)) {
						DBUtils.executeInsert("INSERT INTO node_contentgroup (node_id, contentgroup_id, sortorder) VALUES (?, ?, ?)", new Object[] {getId(), id, newSortOrder});
						t.dirtObjectCache(ContentLanguage.class, id, true);
						clearCache = true;
					}
				}
			}

			// logcmd and event
			if (isModified) {
				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, Node.TYPE_NODE, getId(), 0, "Node.create");
					t.addTransactional(new TransactionalTriggerEvent(Node.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, Node.TYPE_NODE, getId(), 0, "Node.update");
					t.addTransactional(new TransactionalTriggerEvent(Node.class, getId(), getModifiedNodeData(origNode, this), Events.UPDATE));
				}

				onSave(this, isNew, false, t.getUserId());
			}

			if (isModified || clearCache) {
				t.dirtObjectCache(Node.class, getId());
			}

			if (isNew) {
				updateMissingReferences();
			}

			return isModified;
		}

		@Override
		public List<Construct> getConstructs() throws NodeException {
			if (isChannel()) {
				return super.getConstructs();
			} else {
				if (assignedConstructs == null) {
					assignedConstructs = new ArrayList<Construct>(super.getConstructs());
				}
				return assignedConstructs;
			}
		}

		@Override
		public List<ObjectTagDefinition> getObjectTagDefinitions() throws NodeException {
			if (isChannel()) {
				return super.getObjectTagDefinitions();
			} else {
				if (assignedObjectTagDefinitions == null) {
					assignedObjectTagDefinitions = new ArrayList<>(super.getObjectTagDefinitions());
				}
				return assignedObjectTagDefinitions;
			}
		}

		@Override
		public List<ContentLanguage> getLanguages() throws NodeException {
			if (isChannel()) {
				return super.getLanguages();
			} else {
				if (languages == null) {
					languages = new ArrayList<ContentLanguage>(super.getLanguages());
				}
				return languages;
			}
		}
	}

	public FolderFactory() {
		super();
	}

	@Override
	public void initialize() throws NodeException {
		super.initialize();

		// get all existing nodes, which have a _property set
		List<Node> nodes = Trx.supply(t -> t.getObjects(Node.class, DBUtils.select(
				"SELECT id FROM node WHERE host_property != '' OR mesh_preview_url_property != ''",
				DBUtils.IDLIST)));

		// resolve the properties, because their value might have changed
		for (Node node : nodes) {
			try {
				Trx.consume(update -> {
					Transaction t = TransactionManager.getCurrentTransaction();
					update = t.getObject(update, true);
					update.resolveHostnameProperty();
					update.resolveMeshPreviewUrlProperty();
					update.save();
				}, node);
			} catch (NodeException e) {
				logger.error("Error while resolving properties set for node " + I18NHelper.getName(node));
			}
		}
	}

	/**
	 * Gets a list of Templates which should be removed and not just unlinked
	 * on basis of a list of templates which are linked to the specified folder.
	 * @param folder Folder for which the templates should be deleted.
	 * @param templates Collection of Templates which are suggested for deletion.
	 * @return The Collection with Templates which should be really deleted.
	 */
	public Collection<Template> getTemplatesToDelete(FactoryFolder folder, Collection<Template> templates) throws NodeException {
		return getTemplatesToDelete(folder, templates, true);
	}

	/**
	 * Gets a list of Templates which should be removed and not just unlinked
	 * on basis of a list of templates which are linked to the specified folder.
	 * @param folder Folder for which the templates should be deleted.
	 * @param templates Collection of Templates which are suggested for deletion.
	 * @param addFolderToDeleteList If the given folder should also be added to the list of folders marked for deletion.
	 * @return The Collection with Templates which should be really deleted.
	 */
	public Collection<Template> getTemplatesToDelete(FactoryFolder folder, Collection<Template> templates, boolean addFolderToDeleteList) throws NodeException {

		if (templates == null) {
			return Collections.emptyList();
		}
		if (templates.isEmpty()) {
			return Collections.emptyList();
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		// Map with all templates marked for deletion id => template
		Map<Object, Template> templateMap = new HashMap<Object, Template>();

		for (Template template : templates) {
			templateMap.put(template.getId(), template);
		}

		// List with folderIds marked for deletion
		List<Object> folderIds = new ArrayList<Object>();

		if (addFolderToDeleteList) {
			folderIds.add(folder.getId());
		}
		Collection<Folder> folders = getDeleteList(t, Folder.class);

		for (Folder f : folders) {
			folderIds.add(f.getId());
		}

		// List with pageIds marked for deletion
		PageFactory pageFactory = (PageFactory) t.getObjectFactory(Page.class);
		Collection<Page> pages = pageFactory.getDeleteList(t, Page.class);
		LinkedList<Object> pageIds = new LinkedList<Object>();

		for (Page p : pages) {
			pageIds.add(p.getId());
		}

		// Map with Folder -> Collection<templateId> combinations marked for deletion
		Map templatesToUnlinkMap = new HashMap();
		Iterator deleteListIterator = getDeleteList(t, Template.class).iterator();

		if (deleteListIterator.hasNext()) {
			for (Iterator it = ((Map) deleteListIterator.next()).entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				Folder f = (Folder) entry.getKey();

				templatesToUnlinkMap.put(f.getId(), entry.getValue());
			}
		}

		List<Object> params = new ArrayList<Object>();

		// Set of templates that are not allowed to be removed
		Set<Integer> dontDelete = new HashSet<Integer>();

		// Template -> Folder links
		StringBuffer sql = new StringBuffer();

		sql.append("SELECT template_id, folder_id FROM template_folder tf WHERE tf.template_id IN (");
		sql.append(StringUtils.repeat("?", templateMap.size(), ","));
		params.addAll(templateMap.keySet());
		sql.append(")");
		if (!folderIds.isEmpty()) {
			sql.append(" AND tf.folder_id NOT IN (");
			sql.append(StringUtils.repeat("?", folderIds.size(), ","));
			params.addAll(folderIds);
			sql.append(")");
		}

		DBUtils.executeStatement(sql.toString(), Transaction.SELECT_STATEMENT, stmt -> {
			int pCounter = 1;

			for (Object p : params) {
				stmt.setObject(pCounter++, p);
			}
		}, rs -> {
			while (rs.next()) {
				if (templatesToUnlinkMap != null) {
					Integer folderId = new Integer(rs.getInt("folder_id"));
					Integer templateId = new Integer(rs.getInt("template_id"));

					if (!folderId.equals(folder.getId())) {
						Collection tmp = (Collection) templatesToUnlinkMap.get(folderId);

						if (tmp != null) {
							if (!tmp.contains(templateId)) {
								dontDelete.add(templateId);
							}
						} else {
							dontDelete.add(templateId);
						}
					}
				} else {
					dontDelete.add(rs.getInt("template_id"));
				}
			}
		});

		// Template -> Page links
		sql = new StringBuffer();
		params.clear();
		sql.append("SELECT template_id FROM page p WHERE p.template_id IN (");
		sql.append(StringUtils.repeat("?", templateMap.size(), ","));
		params.addAll(templateMap.keySet());
		sql.append(")");
		if (!pageIds.isEmpty()) {
			sql.append(" AND p.id NOT IN (");
			sql.append(StringUtils.repeat("?", pageIds.size(), ","));
			params.addAll(pageIds);
			sql.append(")");
		}
		sql.append(" GROUP BY (template_id)");

		DBUtils.executeStatement(sql.toString(), Transaction.SELECT_STATEMENT, stmt -> {
			int pCounter = 1;

			for (Object p : params) {
				stmt.setObject(pCounter++, p);
			}
		}, rs -> {
			while (rs.next()) {
				dontDelete.add(rs.getInt("template_id"));
			}
		});

		// Remove all that are not for deletion
		for (Iterator<Integer> dontDeleteIterator = dontDelete.iterator(); dontDeleteIterator.hasNext();) {
			Integer id = dontDeleteIterator.next();

			templateMap.remove(id);
		}

		return templateMap.values();
	}

	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (Folder.class.equals(clazz)) {
			return (T) new EditableFactoryFolder(handle.createObjectInfo(Folder.class, true));
		} else if (Node.class.equals(clazz)) {
			return (T) new EditableFactoryNode(handle.createObjectInfo(Node.class, true));
		} else {
			return null;
		}
	}

	/**
	 * Unlinks a template but instead of directly unlinking it, the action is cached.
	 * When committing the transaction the unlink is performed.
	 *
	 * @param factoryFolder The folder from which to unlink the template
	 * @param templateId The templateId of the template to unlink
	 */
	protected void unlinkTemplate(FactoryFolder factoryFolder, Object templateId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection toDelete = getDeleteList(t, Template.class);

		// Get the deleteMap from the list, or create a new one
		Map deleteMap;
		Iterator it = toDelete.iterator();

		if (it.hasNext()) {
			deleteMap = (Map) it.next();
		} else {
			deleteMap = new HashMap();
			toDelete.add(deleteMap);
		}

		// Get the list with templates marked to delete for this folder
		Collection templates = (Collection) deleteMap.get(factoryFolder);

		if (templates == null) {
			templates = new Vector();
			deleteMap.put(factoryFolder, templates);
		}

		// Add the template
		templates.add(templateId);
	}

	/**
	 * Deletes a node but instead of directly deleting it, the delete action is cached.
	 * When the transaction is committed the delete is performed.
	 *
	 * @param node the node to delete
	 */
	protected void deleteNode(FactoryNode node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection toDelete = getDeleteList(t, Node.class);

		toDelete.add(node);
	}

	/**
	 * Deletes a folder but instead of directly deleting it, the delete action is cached.
	 * When the transaction is committed the delete is performed.
	 *
	 * @param folder the folder to delete
	 */
	protected void deleteFolder(FactoryFolder folder) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection toDelete = getDeleteList(t, Folder.class);

		toDelete.add(folder);

		// when multichannelling is active and the folder is a master, also get all
		// localized objects and remove them
		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && folder.isMaster()) {
			Map<Integer, Integer> channelSet = folder.getChannelSet();

			for (Integer pageId : channelSet.values()) {
				Folder locFolder = t.getObject(Folder.class, pageId, -1, false);

				if (!toDelete.contains(locFolder)) {
					toDelete.add(locFolder);
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

		// Delete the folders
		if (!isEmptyDeleteList(t, Folder.class)) {
			Collection deleteList = getDeleteList(t, Folder.class);

			LinkedList folderIds = new LinkedList();

			for (Iterator it = deleteList.iterator(); it.hasNext();) {
				Folder folder = (Folder) it.next();

				folderIds.add(folder.getId());
			}

			// Delete the folder from every overview
			DBUtils.selectAndDelete("ds_obj",
					"SELECT ds_obj.id AS id FROM ds_obj, ds WHERE " + "ds_obj.templatetag_id = ds.templatetag_id AND "
					+ "ds_obj.contenttag_id = ds.contenttag_id AND " + "ds_obj.objtag_id = ds.objtag_id AND " + "(ds.o_type = " + Folder.TYPE_FOLDER
					+ " OR ds.is_folder = 1) AND " + "ds_obj.o_id IN",
					folderIds);
			// DBUtils.selectAndDelete("ds_obj_nodeversion", "SELECT ds_obj.id AS id FROM ds_obj_nodeversion ds_obj, ds_nodeversion ds WHERE " +
			// "ds_obj.templatetag_id = ds.templatetag_id AND " +
			// "ds_obj.contenttag_id = ds.contenttag_id AND " +
			// "ds_obj.objtag_id = ds.objtag_id AND " +
			// "(ds.o_type = " + Folder.TYPE_FOLDER + " OR ds.is_folder = 1) AND " +
			// "ds_obj.o_id IN", folderIds);

			// Delete from dependency map
			flushDelete("DELETE FROM dependencymap WHERE mod_type = " + Folder.TYPE_FOLDER + " AND mod_id IN", Folder.class);
			flushDelete("DELETE FROM dependencymap WHERE dep_type = " + Folder.TYPE_FOLDER + " AND dep_id IN", Folder.class);
			// log command and trigger event
			for (Iterator it = deleteList.iterator(); it.hasNext();) {
				Folder folder = (Folder) it.next();

				ActionLogger.logCmd(ActionLogger.DEL, Folder.TYPE_FOLDER, folder.getId(), null, "Folder.delete()");
				Events.trigger(folder,
						new String[] { ObjectTransformer.getString(folder.getNode().getId(), ""),
								MeshPublisher.getMeshUuid(folder), MeshPublisher.getMeshLanguage(folder) },
						Events.DELETE);

				// if the folder is a localized copy, it was hiding other folders (which are now "created")
				if (!folder.isMaster()) {
					unhideFormerHiddenObjects(Folder.TYPE_FOLDER, folder.getId(), folder.getChannel(), folder.getChannelSet());
				}
			}
			// Delete workflow links for folder
			DBUtils.executeMassStatement("SELECT id FROM workflowlink WHERE o_type = ? AND o_id IN", folderIds, 2, new SQLExecutor() {
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, Folder.TYPE_FOLDER);
				}

				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					LinkedList<Integer> workflowLinkIds = new LinkedList<Integer>();

					while (rs.next()) {
						workflowLinkIds.add(rs.getInt("id"));
					}
					if (!workflowLinkIds.isEmpty()) {
						DBUtils.executeMassStatement("DELETE FROM eventprop WHERE workflowlink_id IN", workflowLinkIds, 1, null);
						DBUtils.executeMassStatement("DELETE FROM reactionprop WHERE workflowlink_id IN", workflowLinkIds, 1, null);
						DBUtils.executeMassStatement("DELETE FROM triggerevent WHERE workflowlink_id IN", workflowLinkIds, 1, null);
						DBUtils.executeMassStatement("DELETE FROM workflowlink WHERE id IN", workflowLinkIds, 1, null);
					}
				}
			});
			// Update references to this folder
			String folderIdSql = buildIdSql(folderIds);
			final List<Integer> partIds = new Vector<Integer>();

			DBUtils.executeStatement("SELECT id FROM part WHERE type_id = 25", new SQLExecutor() {
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						partIds.add(rs.getInt("id"));
					}
				}
			});

			if (!partIds.isEmpty()) {
				// Dirt the cache of the values
				final Vector<Integer> valueIds = new Vector<Integer>();

				DBUtils.executeMassStatement("SELECT id FROM value WHERE value_ref IN " + folderIdSql + " AND part_id IN", partIds, 1, new SQLExecutor() {
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							Integer id = rs.getInt("id");
							t.dirtObjectCache(Value.class, id);
							valueIds.add(id);
						}
					}
				});
				// Update the values
				if (!valueIds.isEmpty()) {
					DBUtils.executeMassStatement("UPDATE value SET value_ref = 0 WHERE id IN ", valueIds, 1, null);
				}
			}

			// Delete links from template to folder
			flushDelete("DELETE FROM template_folder WHERE folder_id IN", Folder.class);
			// Delete permissions for folder
			flushDelete("DELETE FROM perm WHERE o_type IN ( " + Folder.TYPE_FOLDER + ", " + Node.TYPE_NODE + ", " + Node.TYPE_CHANNEL + ") AND o_id IN", Folder.class);
			for (Object folderId : folderIds) {
				t.addTransactional(new RemovePermsTransactional(Folder.TYPE_FOLDER, ObjectTransformer.getInt(folderId, 0)));
				t.addTransactional(new RemovePermsTransactional(Node.TYPE_NODE, ObjectTransformer.getInt(folderId, 0)));
				t.addTransactional(new RemovePermsTransactional(Node.TYPE_CHANNEL, ObjectTransformer.getInt(folderId, 0)));
			}
			// Delete folder
			flushDelete("DELETE FROM folder WHERE id IN", Folder.class);
		}

        // Delete the nodes
        if (!isEmptyDeleteList(t, Node.class)) {
            Collection<Node> nodes = getDeleteList(t, Node.class);
            // log command und trigger event
            for (Node node : nodes) {
                ActionLogger.logCmd(ActionLogger.DEL, Node.TYPE_NODE, node.getId(), null, "Node.delete()");
                Events.trigger(node, null, Events.DELETE);
            }
            flushDelete("DELETE FROM objprop_node WHERE node_id IN", Node.class);
            flushDelete("DELETE FROM construct_node WHERE node_id IN", Node.class);
            flushDelete("DELETE FROM node_contentgroup WHERE node_id IN", Node.class);
            flushDelete("DELETE FROM node WHERE id IN", Node.class);
        }

		// Unlink the Templates
		if (!isEmptyDeleteList(t, Template.class)) {
			Collection templates = getDeleteList(t, Template.class);
			Map templateMap = (Map) templates.iterator().next();

			for (Iterator it = templateMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				final Folder folder = (Folder) entry.getKey();
				Collection templateIds = (Collection) entry.getValue();

				if (templateIds != null) {
					for (Iterator templateIdIterator = templateIds.iterator(); templateIdIterator.hasNext();) {
						final Integer templateId = (Integer) templateIdIterator.next();

						DBUtils.executeStatement("DELETE FROM template_folder WHERE template_id=? AND folder_id=?", new SQLExecutor() {
							public void prepareStatement(PreparedStatement stmt) throws SQLException {
								stmt.setInt(1, templateId.intValue());
								stmt.setInt(2, folder.getId());
							}
						}, Transaction.DELETE_STATEMENT);
						ActionLogger.logCmd(ActionLogger.DEL, Template.TYPE_TEMPLATE, templateId, null, "Folder.unlinkTemplate()");
						Events.trigger(folder, new String[] { "templates"}, Events.UPDATE);
					}
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		if (Folder.class.equals(clazz)) {
			Folder folder = loadDbObject(Folder.class, id, info, SELECT_FOLDER_SQL, null, null);
			batchLoadI18nData(Collections.singleton(folder));
			return (T) folder;
		}
		if (Node.class.equals(clazz)) {
			NodeObject node = loadDbObject(Node.class, id, info, SELECT_NODE_SQL, null, null);

			return (T) node;
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		String idSql = buildIdSql(ids);

		if (Folder.class.equals(clazz)) {
			String[] preloadSql = new String[] {
				"SELECT obj_id AS id, id AS id2 FROM objtag WHERE obj_type = " + Folder.TYPE_FOLDER + " AND obj_id IN " + idSql };

			Collection<Folder> folders = batchLoadDbObjects(Folder.class, ids, info, BATCHLOAD_FOLDER_SQL + idSql, preloadSql);
			batchLoadI18nData(folders);
			return (Collection<T>) folders;
		}
		if (Node.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_NODE_SQL + idSql);
		}
		return null;
	}

	/**
	 * Load i18n data for the given collection of folders and set to the folders
	 * @param folders collection of folders
	 * @throws NodeException
	 */
	private void batchLoadI18nData(Collection<Folder> folders) throws NodeException {
		// prepare a folder map
		Map<Integer, FactoryFolder> folderMap = folders.stream().filter(f -> f instanceof FactoryFolder)
				.map(f -> (FactoryFolder) f)
				.collect(Collectors.toMap(Folder::getId, java.util.function.Function.identity()));

		if (!folderMap.isEmpty()) {
			String placeholders = StringUtils.repeat("?", folderMap.size(), ",");
			String sql = String.format("%s (%s)", BATCHLOAD_I18N_SQL, placeholders);

			DBUtils.select(sql, st -> {
				int index = 1;
				for (int id : folderMap.keySet()) {
					st.setInt(index++, id);
				}
			}, rs -> {
				while (rs.next()) {
					int folderId = rs.getInt("folder_id");
					int contentGroupId = rs.getInt("contentgroup_id");
					String name = rs.getString("name");
					String description = rs.getString("description");
					String publishDir = rs.getString("pub_dir");

					if (!StringUtils.isEmpty(name) || !StringUtils.isEmpty(description) || !StringUtils.isEmpty(publishDir)) {
						FactoryFolder factoryFolder = folderMap.get(folderId);
						if (factoryFolder != null) {
							if (!StringUtils.isEmpty(name)) {
								factoryFolder.nameI18n.put(contentGroupId, name);
							}
							if (!StringUtils.isEmpty(description)) {
								factoryFolder.descriptionI18n.put(contentGroupId, description);
							}
							if (!StringUtils.isEmpty(publishDir)) {
								factoryFolder.publishDirI18n.put(contentGroupId, publishDir);
							}
						}
					}
				}
				return null;
			});
		}
	}

	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws NodeException {
		if (Folder.class.equals(clazz)) {
			return (T) loadFolderObject(id, info, rs, idLists);
		}
		if (Node.class.equals(clazz)) {
			return (T) loadNodeObject(id, info, rs);
		}
		return null;
	}

	private Node loadNodeObject(Integer id, NodeObjectInfo info, FactoryDataRow rs) throws NodeException {
		return new FactoryNode(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs, "node"));
	}

	private Folder loadFolderObject(Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws NodeException {

		String name = rs.getString("name");
		String description = rs.getString("description");
		Integer motherId = new Integer(rs.getInt("mother"));
		Integer nodeId = new Integer(rs.getInt("node_id"));
		String pubDir = rs.getString("pub_dir");
		ContentNodeDate cDate = new ContentNodeDate(rs.getInt("cdate"));
		ContentNodeDate eDate = new ContentNodeDate(rs.getInt("edate"));
		Integer creatorId = new Integer(rs.getInt("creator"));
		Integer editorId = new Integer(rs.getInt("editor"));
		Integer masterId = new Integer(rs.getInt("master_id"));
		List<Integer> objTypeIds = idLists != null ? idLists[0] : null;
		Integer channelSetId = new Integer(rs.getInt("channelset_id"));
		Integer channelId = new Integer(rs.getInt("channel_id"));
		boolean master = rs.getBoolean("is_master");
		boolean excluded = rs.getBoolean("mc_exclude");
		boolean disinheritDefault = rs.getBoolean("disinherit_default");

		return new FactoryFolder(id, info, name, description, motherId, nodeId, pubDir, objTypeIds, cDate, eDate, creatorId, editorId, masterId, channelSetId,
				channelId, master, excluded, disinheritDefault, rs.getInt("deleted"), rs.getInt("deletedby"), getUdate(rs), getGlobalId(rs, "folder"));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}
		if (object instanceof FactoryFolder) {
			EditableFactoryFolder editableCopy = new EditableFactoryFolder((FactoryFolder) object, info, false);

			return (T) editableCopy;
		} else if (object instanceof FactoryNode) {
			return (T) new EditableFactoryNode((FactoryNode) object, info, false);
		} else {
			throw new NodeException("FolderFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Concatenate the Node.pub_dir and Folder.pub_dir to a path. The path will always begin with a slash, will not contain duplicate slashes and will end with a slash, if requested
	 * @param nodePubDir node pub_dir
	 * @param folderPubDir folder pub_dir
	 * @param endSlash true when the path must end with a slash
	 * @return path
	 */
	public static String getPath(String nodePubDir, String folderPubDir, boolean endSlash) {
		StringBuilder builder = new StringBuilder();
		if (!StringUtils.isEmpty(nodePubDir) && !"/".equals(nodePubDir)) {
			builder.append(nodePubDir);
		}
		if (!StringUtils.isEmpty(folderPubDir) && !"/".equals(folderPubDir)) {
			if (!builder.toString().endsWith("/") && !folderPubDir.startsWith("/")) {
				builder.append("/");
			}

			if (builder.toString().endsWith("/") && folderPubDir.startsWith("/")) {
				builder.append(folderPubDir.substring(1));
			} else {
				builder.append(folderPubDir);
			}
		}
		if (endSlash && !builder.toString().endsWith("/")) {
			builder.append("/");
		}

		if (builder.length() == 0) {
			return "/";
		}

		if (!builder.toString().startsWith("/")) {
			builder.insert(0, "/");
		}

		return builder.toString();
	}

	/**
	 * Get list of properties which are different between the two folder instances
	 * @param original original folder
	 * @param updated update folder
	 * @return list of changed properties
	 * @throws NodeException
	 */
	private static List<String> getChangedProperties(Folder original, Folder updated) throws NodeException {
		List<String> modified = new Vector<String>();

		if (original == null || updated == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Folder to compare with was null returning empty property set");
			}
			return modified;
		}

		if (!StringUtils.isEqual(original.getName(), updated.getName()) || !Objects.deepEquals(original.getNameI18n(), updated.getNameI18n())) {
			modified.add("name");
		}
		if (!StringUtils.isEqual(original.getDescription(), updated.getDescription()) || !Objects.deepEquals(original.getDescriptionI18n(), updated.getDescriptionI18n())) {
			modified.add("description");
		}
		if (!StringUtils.isEqual(original.getPublishDir(), updated.getPublishDir()) || !Objects.deepEquals(original.getPublishDirI18n(), updated.getPublishDirI18n())) {
			modified.add("pub_dir");
		}
		if (!original.getEditor().equals(updated.getEditor())) {
			modified.add("editor");
		}
		modified.add("edate");

		return modified;
	}

	/**
	 * Get the ids of all subfolders (master objects only) regardless of their channel
	 * @param folder folder to start with
	 * @param includeRoot true if the folder itself shall also be added to the set
	 * @param masterState true to only get master folders, false for only localized copies, null to get both
	 * @return set of subfolder ids
	 * @throws NodeException
	 */
	public static Set<Integer> getSubfolderIds(Folder folder, boolean includeRoot, final Boolean masterState) throws NodeException {
		int folderId = ObjectTransformer.getInt(folder.getId(), 0);
		final Set<Integer> toCheck = new HashSet<Integer>();
		final Set<Integer> subFolderIds = new HashSet<Integer>();

		toCheck.add(folderId);
		if (includeRoot) {
			subFolderIds.add(folderId);
		}

		StringBuilder sqlBuilder = new StringBuilder("SELECT id, is_master FROM folder WHERE mother = ?");
		if (masterState != null) {
			sqlBuilder.append(" AND is_master = ?");
		}
		String sql = sqlBuilder.toString();
		while (!toCheck.isEmpty()) {
			final int checkId = toCheck.iterator().next();
			DBUtils.executeStatement(sql, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, checkId);								// mother = ?
					if (masterState != null) {
						stmt.setInt(2, masterState.booleanValue() ? 1 : 0);	// is_master = ?
					}
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						int childId = rs.getInt("id");
						if (rs.getBoolean("is_master")) {
							toCheck.add(childId);
						}
						subFolderIds.add(childId);
					}
				}
			});
			toCheck.remove(checkId);
		}

		return subFolderIds;
	}

	/**
	 * Check whether the folder's proposed names is available
	 * @param folder folder with proposed names
	 * @return pair of the used folder name and the other object using the name or null if the names are available
	 * @throws NodeException
	 */
	public static Pair<String, NodeObject> isNameAvailable(Folder folder) throws NodeException {
		if (folder == null) {
			throw new NodeException("Cannot check name availability without folder");
		}
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(folder, false);

		Map<Node, MultiChannellingFallbackList> siblingsFallback = DisinheritUtils.getSiblings(folder, objectSegment);
		Set<Integer> objectIds = siblingsFallback.values().stream().flatMap(fb -> fb.getObjectIds().stream()).collect(Collectors.toSet());

		Folder conflictingObject = null;
		conflictingObject = UniquifyHelper.getObjectUsingProperty(Folder.class, folder.getName(), NAME_FUNCTION, objectIds);
		if (conflictingObject != null) {
			return Pair.of(folder.getName(), conflictingObject);
	}

		for (String i18nName : folder.getNameI18n().values()) {
			conflictingObject = UniquifyHelper.getObjectUsingProperty(Folder.class, i18nName, NAME_FUNCTION, objectIds);
			if (conflictingObject != null) {
				return Pair.of(i18nName, conflictingObject);
			}
		}

		return null;
	}

	/**
	 * Check whether the folder's proposed pub_dirs are available
	 * @param folder folder with proposed pub_dirs
	 * @return pair of the used pub_dir and the other object using the pub_dir or null if the pub_dirs are available
	 * @throws NodeException
	 */
	public static Pair<String, NodeObject> isPubDirAvailable(Folder folder) throws NodeException {
		if (folder == null) {
			throw new NodeException("Cannot check pub_dir availability without folder");
		}
		if (!folder.getNode().isPubDirSegment()) {
			return null;
		}
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(folder, false);

		Map<Node, MultiChannellingFallbackList> siblingsFallback = DisinheritUtils.getSiblings(folder, objectSegment);
		Set<Integer> objectIds = siblingsFallback.values().stream().flatMap(fb -> fb.getObjectIds().stream()).collect(Collectors.toSet());

		Folder conflictingObject = null;
		conflictingObject = UniquifyHelper.getObjectUsingProperty(Folder.class, folder.getPublishDir(), PUB_DIR_FUNCTION, objectIds);
		if (conflictingObject != null) {
			return Pair.of(folder.getPublishDir(), conflictingObject);
		}

		for (String i18nPubDir : folder.getPublishDirI18n().values()) {
			conflictingObject = UniquifyHelper.getObjectUsingProperty(Folder.class, i18nPubDir, PUB_DIR_FUNCTION, objectIds);
			if (conflictingObject != null) {
				return Pair.of(i18nPubDir, conflictingObject);
			}
		}

		// finally check for uniqueness of the (translated) pubdirs of the folder itself
		if (!folder.getPublishDirI18n().isEmpty()) {
			Set<String> checked = new HashSet<>();
			checked.add(folder.getPublishDir());

			for (String i18nPubDir : folder.getPublishDirI18n().values()) {
				if (checked.contains(i18nPubDir)) {
					return Pair.of(i18nPubDir, folder);
				}
				checked.add(i18nPubDir);
			}
		}

		return null;
	}

	/**
	 * Reduce the given collection of folders
	 * @param folders folders
	 * @param type reduction type
	 * @return reduced collection
	 * @throws NodeException
	 */
	public static Collection<Folder> reduce(Collection<Folder> folders, ReductionType type) throws NodeException {
		Collection<Folder> reducedList = new ArrayList<Folder>();

		switch (type) {
		case CHILD:
			// add all folders to the collection
			reducedList.addAll(folders);

			// iterate through all folders, and remove all parents from the collection
			for (Folder folder : folders) {
				reducedList.removeAll(folder.getParents());
			}
			break;
		case PARENT:
			// iterate over the folders
			for (Folder folder : folders) {
				// get the parents of the folder
				List<Folder> parents = new ArrayList<Folder>(folder.getParents());

				// keep the parents, which are in the original folder list
				parents.retainAll(folders);

				// only add the folder to the reduced list, if no parents are left (where in the original list)
				if (parents.size() == 0) {
					reducedList.add(folder);
				}
			}
			break;
		default:
			break;
		}
		return reducedList;
	}

	/**
	 * Get the folders to purge
	 * @param node node
	 * @param deletedBefore get all objects, that were deleted before this timestamp
	 * @return folders to purge
	 * @throws NodeException
	 */
	private static List<Folder> getFoldersToPurge(Node node, int deletedBefore) throws NodeException {
		int nodeId = node.getMaster().getId();
		int channelId = node.isChannel() ? node.getId() : 0;
		List<Integer> ids = new ArrayList<Integer>();
		DBUtils.executeStatement("SELECT id FROM folder WHERE node_id = ? AND channel_id = ? AND deleted > 0 AND deleted < ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, nodeId);
				stmt.setInt(2, channelId);
				stmt.setInt(3, deletedBefore);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					ids.add(rs.getInt("id"));
				}
			}
		});

		return TransactionManager.getCurrentTransaction().getObjects(Folder.class, ids);
	}

	/**
	 * Get the files to purge
	 * @param node node
	 * @param deletedBefore get all objects, that were deleted before this timestamp
	 * @return files to purge
	 * @throws NodeException
	 */
	private static List<File> getFilesToPurge(Node node, int deletedBefore) throws NodeException {
		int nodeId = node.getMaster().getId();
		int channelId = node.isChannel() ? node.getId() : 0;
		List<Integer> ids = new ArrayList<Integer>();
		DBUtils.executeStatement(
				"SELECT cf.id FROM contentfile cf, folder f WHERE cf.folder_id = f.id AND f.node_id = ? AND cf.channel_id = ? AND cf.deleted > 0 AND cf.deleted < ?",
				new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, nodeId);
						stmt.setInt(2, channelId);
						stmt.setInt(3, deletedBefore);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							ids.add(rs.getInt("id"));
						}
					}
				});

		return TransactionManager.getCurrentTransaction().getObjects(File.class, ids);
	}

	/**
	 * Get the pages to purge
	 * @param node node
	 * @param deletedBefore get all objects, that were deleted before this timestamp
	 * @return pages to purge
	 * @throws NodeException
	 */
	private static List<Page> getPagesToPurge(Node node, int deletedBefore) throws NodeException {
		int nodeId = node.getMaster().getId();
		int channelId = node.isChannel() ? node.getId() : 0;
		List<Integer> ids = new ArrayList<Integer>();
		DBUtils.executeStatement(
				"SELECT p.id FROM page p, folder f WHERE p.folder_id = f.id AND f.node_id = ? AND p.channel_id = ? AND p.deleted > 0 AND p.deleted < ?",
				new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, nodeId);
						stmt.setInt(2, channelId);
						stmt.setInt(3, deletedBefore);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							ids.add(rs.getInt("id"));
						}
					}
				});

		return TransactionManager.getCurrentTransaction().getObjects(Page.class, ids);
	}

	/**
	 * Get the configured wastebin maxage value (in seconds) for the node.
	 * <ol>
	 * <li>If <code>$WASTEBIN_MAXAGE_NODE[nodeId]</code> is set (to a number), this is returned</li>
	 * <li>If <code>$WASTEBIN_MAXAGE</code> is set (to a number), this is returned</li>
	 * <li>Return 0 as default value (turning off purging of wastebin)</li>
	 * </ol>
	 * @param node node
	 * @return configured maxage
	 * @throws NodeException
	 */
	private static int getWastebinMaxage(Node node) throws NodeException {
		NodePreferences prefs = TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences();

		int wastebinMaxage = 0;

		// get the global value (if set)
		wastebinMaxage = ObjectTransformer.getInt(prefs.getProperty("wastebin_maxage"), wastebinMaxage);

		// get the node specific value (if set)
		Map<?, ?> maxAgeMap = prefs.getPropertyMap("wastebin_maxage_node");
		if (maxAgeMap != null) {
			wastebinMaxage = ObjectTransformer.getInt(maxAgeMap.get(node.getId().toString()), wastebinMaxage);
		}

		return wastebinMaxage;
	}

	/**
	 * Make a clean pubDir. This will always add/remove leading/trailing slashes and will optionally also sanitize the pubDir
	 * @param pubDir pubDir to clean
	 * @param pubDirSegments true for pubDir segments
	 * @param sanitize true to also sanitize
	 * @return cleaned (and possibly sanitized) pubDir
	 * @throws NodeException
	 */
	public static String cleanPubDir(String pubDir, boolean pubDirSegments, boolean sanitize) throws NodeException {
		// we need to sanitize, so first strip leading and trailing slashes
		if (sanitize && !StringUtils.isEmpty(pubDir)) {
			if (pubDir.length() > 0 && pubDir.startsWith("/")) {
				pubDir = pubDir.substring(1);
			}
			if (pubDir.length() > 0 && pubDir.endsWith("/")) {
				pubDir = pubDir.substring(0, pubDir.length() - 1);
			}

			if (!StringUtils.isEmpty(pubDir)) {
				// sanitize
				Transaction t = TransactionManager.getCurrentTransaction();
				NodePreferences nodePreferences = t.getNodeConfig().getDefaultPreferences();

				Map<String, String> sanitizeCharacters = nodePreferences.getPropertyMap("sanitize_character");
				String replacementChararacter = nodePreferences.getProperty("sanitize_replacement_character");
				String[] preservedCharacters = nodePreferences.getProperties("sanitize_allowed_characters");

		if (pubDirSegments) {
					pubDir = FileUtil.sanitizeName(pubDir, sanitizeCharacters, replacementChararacter, preservedCharacters);
				} else {
					pubDir = FileUtil.sanitizeFolderPath(pubDir, sanitizeCharacters, replacementChararacter, preservedCharacters);
				}
			}
		}

		// finally fix leading and trailing slashes
		if (pubDirSegments) {
			if (pubDir.length() > 0 && pubDir.startsWith("/")) {
				pubDir = pubDir.substring(1);
			}
			if (pubDir.length() > 0 && pubDir.endsWith("/")) {
				pubDir = pubDir.substring(0, pubDir.length() - 1);
			}
		} else {
			// make sure that the pub dir always starts and ends with /
			if (StringUtils.isEmpty(pubDir)) {
				pubDir = "/";
			} else {
				if (!pubDir.startsWith("/")) {
					pubDir = String.format("/%s", pubDir);
				}
				if (!pubDir.endsWith("/")) {
					pubDir = String.format("%s/", pubDir);
				}
			}
		}

		return pubDir;
	}

	/**
	 * Recursively check uniqueness of pub_dir's starting with the given folder
	 * @param folder folder
	 * @return first folder found with non-unique pub_dir or null if none found
	 * @throws NodeException
	 */
	public static Folder checkPubDirUniqueness(Folder folder) throws NodeException {
		return getFromFoldersRecursive(folder, f -> {
			if (!DisinheritUtils.isPropertyAvailable(f, f1 -> f1.getObject().getPublishDir(), "pub_dir", new ChannelTreeSegment(f, false))) {
				return f;
			} else {
				return null;
			}
		});
	}

	/**
	 * Recursively apply the consumer to the folder and all its subfolders
	 * @param folder folder
	 * @param consumer consumer
	 * @throws NodeException
	 */
	public static void doForFoldersRecursive(Folder folder, Consumer<Folder> consumer) throws NodeException {
		doForFoldersRecursive(folder, null, consumer);
	}

	/**
	 * Recursively apply the consumer to the folder and all its subfolders with an optional filter
	 * @param folder folder
	 * @param filter optional filter (may be null). If the filter returns false for a folder, the folder and all its children will be omitted.
	 * @param consumer consumer
	 * @throws NodeException
	 */
	public static void doForFoldersRecursive(Folder folder, Function<Folder, Boolean> filter, Consumer<Folder> consumer) throws NodeException {
		if (filter != null && !ObjectTransformer.getBoolean(filter.apply(folder), true)) {
			return;
		}
		consumer.accept(folder);
		for (Folder child : folder.getChildFolders()) {
			doForFoldersRecursive(child, consumer);
		}
	}

	/**
	 * Recursively apply the getter to the folder, until it returns something != null
	 * @param folder folder
	 * @param getter getter
	 * @return return value
	 * @throws NodeException
	 */
	public static <R> R getFromFoldersRecursive(Folder folder, Function<Folder, R> getter) throws NodeException {
		R retval = getter.apply(folder);
		if (retval != null) {
			return retval;
		}
		for (Folder child : folder.getChildFolders()) {
			retval = getFromFoldersRecursive(child, getter);
			if (retval != null) {
				return retval;
			}
		}
		return retval;
	}

	/**
	 * Get the hostname and base path for the given node
	 * <ul>
	 * <li>If the node has pub_dir_segments active, this will return [hostname]/[node.pub_dir]/[rootfolder.pub_dir_segment]</li>
	 * <li>Otherwise this will return [hostname]/[node.pub_dir]</li>
	 * </ul>
	 * @param node node
	 * @return hostname and basepath
	 * @throws NodeException
	 */
	public static List<String> getHostnameAndBasePath(Node node) throws NodeException {
		if (node.isPubDirSegment()) {
			return Arrays.asList(FilePublisher.getPath(false, true, node.getHostname(), node.getPublishDir(), node.getFolder().getPublishDir()).toLowerCase(),
					FilePublisher.getPath(false, true, node.getHostname(), node.getBinaryPublishDir(), node.getFolder().getPublishDir()).toLowerCase());
		} else {
			return Arrays.asList(FilePublisher.getPath(false, true, node.getHostname(), node.getPublishDir()).toLowerCase(),
					FilePublisher.getPath(false, true, node.getHostname(), node.getBinaryPublishDir()).toLowerCase());
		}
	}

	/**
	 * Get the names of attributes, which were changed
	 * @param origObject original object
	 * @param modObject modified object
	 * @return array of attribute names
	 * @throws NodeException
	 */
	protected static String[] getModifiedNodeData(Node origObject, Node modObject) throws NodeException {
		if (origObject == null || modObject == null) {
			return null;
		}
		Set<String> modifiedAttributes = getModifiedAttributes(origObject, modObject);

		// when the page language code is modified to or from PATH to something else, we add "pub_dir" to the
		// modified attributes to enable correct dirting
		if (origObject.getPageLanguageCode() != modObject.getPageLanguageCode()
				&& (origObject.getPageLanguageCode() == PageLanguageCode.PATH
						|| modObject.getPageLanguageCode() == PageLanguageCode.PATH)) {
			modifiedAttributes.add("pub_dir");
		}

		return modifiedAttributes.toArray(new String[modifiedAttributes.size()]);
	}

	/**
	 * Enum for the reduction type used in method {@link FolderFactory#reduce(Collection, ReductionType)}
	 */
	public static enum ReductionType {
		CHILD, PARENT
	}

	/**
	 * Possible values for recursive changing/checking of multichannelling restrictions
	 */
	protected static enum RecursiveMCRestrictions {
		/**
		 * Skip setting or checking
		 */
		skip,

		/**
		 * Check for consistency
		 */
		check,

		/**
		 * Set the restriction recursively
		 */
		set
	}
}
