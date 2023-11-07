package com.gentics.contentnode.factory.object;

import static com.gentics.contentnode.perm.PermHandler.PERM_CHANGE_PERM;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_DELETE;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static com.gentics.contentnode.rest.util.PropertySubstitutionUtil.substituteSingleProperty;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.portalnode.connector.DatasourceType;
import com.gentics.api.portalnode.connector.DuplicateIdException;
import com.gentics.api.portalnode.connector.HandleType;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.QueueEntry;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.RefreshPermHandler;
import com.gentics.contentnode.factory.RemovePermsTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.publish.PublishHandlerStore;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.TagmapEntryInconsistencyModel;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.datasource.AbstractContentRepositoryStructure;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.db.DB;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogCollector;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.ClassHelper;

/**
 * Factory for ContentRepository
 */
@DBTables({
	@DBTable(clazz = ContentRepository.class, name = "contentrepository"),
	@DBTable(clazz = TagmapEntry.class, name = "tagmap"),
	@DBTable(clazz = CrFragment.class, name="cr_fragment"),
	@DBTable(clazz = CrFragmentEntry.class, name="cr_fragment_entry")})
public class ContentRepositoryFactory extends AbstractFactory {
	static {
		// register the factory classes
		try {
			registerFactoryClass(C.Tables.CONTENTREPOSITORY, ContentRepository.TYPE_CONTENTREPOSITORY, true, FactoryContentRepository.class);
			registerFactoryClass(C.Tables.TAGMAP, TagmapEntry.TYPE_TAGMAPENTRY, true, FactoryTagmapEntry.class);
			registerFactoryClass(C.Tables.CR_FRAGMENT, CrFragment.TYPE_CR_FRAGMENT, true, FactoryCrFragment.class);
			registerFactoryClass(C.Tables.CR_FRAGMENT_ENTRY, CrFragmentEntry.TYPE_CR_FRAGMENT_ENTRY, true, FactoryCrFragmentEntry.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Create an instance
	 */
	public ContentRepositoryFactory() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#createObject(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle,
			Class<T> clazz) throws NodeException {
		if (ContentRepository.class.equals(clazz)) {
			return (T) new EditableFactoryContentRepository(handle.createObjectInfo(ContentRepository.class, true));
		} else if (TagmapEntry.class.equals(clazz)) {
			return (T) new EditableFactoryTagmapEntry(handle.createObjectInfo(TagmapEntry.class, true));
		} else if (CrFragment.class.equals(clazz)) {
			return (T) new EditableFactoryCrFragment(handle.createObjectInfo(CrFragment.class, true));
		} else if (CrFragmentEntry.class.equals(clazz)) {
			return (T) new EditableFactoryCrFragmentEntry(handle.createObjectInfo(CrFragmentEntry.class, true));
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

		if (object instanceof FactoryContentRepository) {
			return (T) new EditableFactoryContentRepository((FactoryContentRepository) object, info, false);
		} else if (object instanceof FactoryTagmapEntry) {
			return (T) new EditableFactoryTagmapEntry((FactoryTagmapEntry) object, info, false);
		} else if (object instanceof FactoryCrFragment) {
			return (T) new EditableFactoryCrFragment((FactoryCrFragment) object, info, false);
		} else if (object instanceof FactoryCrFragmentEntry) {
			return (T) new EditableFactoryCrFragmentEntry((FactoryCrFragmentEntry) object, info, false);
		} else {
			throw new NodeException("ContentRepositoryFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#loadResultSet(java.lang.Class, java.lang.Object, com.gentics.lib.base.object.NodeObjectInfo, com.gentics.contentnode.factory.object.FactoryDataRow, java.util.List<java.lang.Integer>[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id,
			NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		if (ContentRepository.class.equals(clazz)) {
			return (T) new FactoryContentRepository(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs));
		} else if (TagmapEntry.class.equals(clazz)) {
			return (T) new FactoryTagmapEntry(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs));
		} else if (CrFragment.class.equals(clazz)) {
			return (T) new FactoryCrFragment(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs));
		} else if (CrFragmentEntry.class.equals(clazz)) {
			return (T) new FactoryCrFragmentEntry(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs));
		}
		return null;
	}

	/**
	 * Factory Implementation for ContentRepository
	 */
	private static class FactoryContentRepository extends ContentRepository {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2871871071289354834L;

		@DataField("name")
		@Updateable
		protected String name;

		@DataField("instant_publishing")
		@Updateable
		protected boolean instantPublishing;

		@DataField("language_information")
		@Updateable
		protected boolean languageInformation;

		@DataField("permission_information")
		@Updateable
		protected boolean permissionInformation;

		@DataField("permission_property")
		@Updateable
		protected String permissionProperty;

		@DataField("default_permission")
		@Updateable
		protected String defaultPermission;

		@DataField("diffdelete")
		@Updateable
		protected boolean diffDelete;

		@DataField("basepath")
		@Updateable
		protected String basepath;

		@DataField("crtype")
		@Updateable
		protected Type crType = Type.cr;

		@DataField("dbtype")
		@Updateable
		protected String dbType;

		@DataField("username")
		@Updateable
		protected String username;

		@DataField("password")
		@Updateable
		protected String password;

		@DataField("password_is_property")
		@Updateable
		protected boolean passwordProperty;

		@DataField("url")
		@Updateable
		protected String url;

		@DataField("elasticsearch")
		@Updateable
		protected String elasticsearch;

		@DataField("project_per_node")
		@Updateable
		protected boolean projectPerNode;

		@DataField("http2")
		@Updateable
		protected boolean http2;

		@DataField("nofoldersindex")
		@Updateable
		protected boolean noFoldersIndex;

		@DataField("nofilesindex")
		@Updateable
		protected boolean noFilesIndex;

		@DataField("nopagesindex")
		@Updateable
		protected boolean noPagesIndex;

		@DataField("noformsindex")
		@Updateable
		protected boolean noFormsIndex;

		@DataField("version")
		@Updateable
		protected String version;

		@DataField("checkdate")
		protected ContentNodeDate checkDate = new ContentNodeDate(0);

		@DataField("checkstatus")
		protected int checkStatus = ContentRepository.DATACHECK_STATUS_UNCHECKED;

		@DataField("checkresult")
		protected String checkResult;

		@DataField("statusdate")
		protected ContentNodeDate statusDate = new ContentNodeDate(0);

		@DataField("datastatus")
		protected int dataStatus = ContentRepository.DATACHECK_STATUS_UNCHECKED;

		@DataField("datacheckresult")
		protected String dataCheckResult;

		protected List<Integer> tagmapEntryIds;

		protected Set<Integer> crFragmentIds;

		/**
		 * Create an empty instance
		 * @param info info
		 * @throws NodeException
		 */
		protected FactoryContentRepository(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 * @param udate udate
		 * @param globalId global id
		 */
		protected FactoryContentRepository(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap,
				int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
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

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryContentRepository(this,
					getFactory().getFactoryHandle(ContentRepository.class).createObjectInfo(ContentRepository.class, true), true);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isInstantPublishing() {
			return instantPublishing;
		}

		@Override
		public boolean isLanguageInformation() {
			return languageInformation;
		}

		@Override
		public boolean isPermissionInformation() {
			return permissionInformation;
		}

		@Override
		public String getPermissionProperty() {
			return permissionProperty;
		}

		@Override
		public String getDefaultPermission() {
			return defaultPermission;
		}

		@Override
		public boolean isDiffDelete() {
			return diffDelete;
		}

		@Override
		public String getBasepath() {
			return basepath;
		}

		@Override
		public Type getCrType() {
			return crType;
		}

		@Override
		public String getDbType() {
			return dbType;
		}

		@Override
		public String getUsername() {
			return username;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public boolean isPasswordProperty() {
			return passwordProperty;
		}

		@Override
		public String getUrl() {
			return url;
		}

		@Override
		public String getElasticsearch() {
			return elasticsearch;
		}

		@Override
		public boolean isProjectPerNode() {
			return projectPerNode;
		}

		@Override
		public String getVersion() {
			return version;
		}

		@Override
		public boolean isHttp2() {
			return http2;
		}

		@Override
		public boolean isNoFoldersIndex() {
			return noFoldersIndex;
		}

		@Override
		public boolean isNoFilesIndex() {
			return noFilesIndex;
		}

		@Override
		public boolean isNoPagesIndex() {
			return noPagesIndex;
		}

		@Override
		public boolean isNoFormsIndex() {
			return noFormsIndex;
		}

		@Override
		public List<TagmapEntry> getEntries() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObjects(TagmapEntry.class, loadEntryIds(), getObjectInfo().isEditable());
		}

		@Override
		public Set<CrFragment> getAssignedFragments() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			return new HashSet<>(t.getObjects(CrFragment.class, loadCrFragmentIds()));
		}

		/**
		 * Load the tagmap entry ids
		 * @return tagmap entry ids
		 * @throws NodeException
		 */
		protected synchronized List<Integer> loadEntryIds() throws NodeException {
			if (tagmapEntryIds == null) {
				if (isEmptyId(getId())) {
					tagmapEntryIds = new ArrayList<>();
				} else {
					tagmapEntryIds = DBUtils.select("SELECT id FROM tagmap WHERE contentrepository_id = ?", ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
				}
			}
			return tagmapEntryIds;
		}

		/**
		 * Load the IDs of assigned CrFragments
		 * @return ID set
		 * @throws NodeException
		 */
		protected synchronized Set<Integer> loadCrFragmentIds() throws NodeException {
			if (crFragmentIds == null) {
				if (isEmptyId(getId())) {
					crFragmentIds = new HashSet<>();
				} else {
					crFragmentIds = DBUtils.select("SELECT cr_fragment_id id FROM contentrepository_cr_fragment WHERE contentrepository_id = ?",
							ps -> ps.setInt(1, getId()), DBUtils.IDS);
				}
			}
			return crFragmentIds;
		}

		@Override
		public List<Node> getNodes() throws NodeException {
			Set<Integer> nodeIds = DBUtils.select("SELECT id FROM node WHERE contentrepository_id = ?", st -> st.setInt(1, getId()), rs -> {
				Set<Integer> ids = new HashSet<>();
				while (rs.next()) {
					ids.add(rs.getInt("id"));
				}
				return ids;
			});
			Set<Node> nodes = new HashSet<>(TransactionManager.getCurrentTransaction().getObjects(Node.class, nodeIds, false, false));
			if (getCrType() == ContentRepositoryModel.Type.mccr) {
				Set<Node> channels = new HashSet<>();
				for (Node node : nodes) {
					if (!node.isChannel()) {
						for (Node c : node.getAllChannels()) {
							if (isEmptyId(c.getContentrepositoryId())
									|| Objects.equals(c.getContentrepositoryId(), getId())) {
								channels.add(c);
							}
						}
					}
				}
				nodes.addAll(channels);
			}

			List<Node> nodeList = new ArrayList<>(nodes);
			nodeList.sort((n1, n2) -> n1.getId().compareTo(n2.getId()));
			return nodeList;
		}

		@Override
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			loadEntryIds();
			for (Integer id : tagmapEntryIds) {
				t.dirtObjectCache(TagmapEntry.class, id, false);
			}
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException,
					NodeException {
			// delete all tagmap entries
			List<TagmapEntry> entries = getEntries();

			for (TagmapEntry entry : entries) {
				entry.delete();
			}

			// delete the contentrepository
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentRepositoryFactory factory = (ContentRepositoryFactory) t.getObjectFactory(ContentRepository.class);

			factory.deleteCR(this);
		}

		@Override
		public ContentMap getContentMap() throws NodeException {
			switch (crType) {
			case cr:
			case mccr:
				return getSQLBasedContentMap();
			case mesh:
				return null;
			default:
				throw new NodeException("Cannot get ContentMap for unset crType");
			}
		}

		@Override
		public ContentNodeDate getCheckDate() {
			return checkDate;
		}

		@Override
		public int getCheckStatus() {
			return checkStatus;
		}

		@Override
		public String getCheckResult() {
			return checkResult;
		}

		@Override
		public ContentNodeDate getStatusDate() {
			return statusDate;
		}

		@Override
		public int getDataStatus() {
			return dataStatus;
		}

		@Override
		public String getDataCheckResult() {
			return dataCheckResult;
		}

		@Override
		public boolean checkStructure(boolean repair) throws NodeException {
			switch (crType) {
			case cr:
			case mccr:
				return checkSQLBasedStructure(repair);
			case mesh:
				return checkMeshStructure(repair);
			default:
				throw new NodeException("Cannot check structure for unset crType");
			}
		}

		@Override
		public void checkData(boolean clean) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			QueueEntry entry = new QueueEntry(t.getUnixTimestamp(), -1, -1, Events.DATACHECK_CR,
					new String[] { Integer.toString(getId()), Boolean.toString(clean) }, 0, null);

			if (logger.isDebugEnabled()) {
				logger.debug("Storing event {" + entry + "} into queue.");
			}
			entry.store(ContentNodeFactory.getInstance());

			// set the datastatus of the cr to "in progress"
			DBUtils.executeUpdate("UPDATE contentrepository SET datastatus = ? WHERE id = ?",
					new Object[] { ContentRepository.DATACHECK_STATUS_QUEUED, getId() });
			t.dirtObjectCache(ContentRepository.class, getId());
		}

		/**
		 * Get the SQL based ContentMap instance
		 * @return ContentMap instance
		 * @throws NodeException
		 */
		private ContentMap getSQLBasedContentMap() throws NodeException {
			// check whether dbtype is set:
			if (StringUtils.isEmpty(dbType)) {
				throw new NodeException("ContentRepository {" + name + "} (id " + id + ") does not have a dbtype set");
			}

			// when the contentmap was not checked (or the check failed), we give a publish warning
			Transaction t = TransactionManager.getCurrentTransaction();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

			Map<String, String> handleProperties = getHandleParameters();

			// generate a unique handlekey
			String handleKey = NodeConfig.CNMAP_HANDLE_PREFIX + "|" + id + "|" + handleProperties.get("url") + "|" + handleProperties.get("driverClass") + "|"
					+ username + "|" + StringUtils.md5(password);

			// do not regenerate sql handles, if nothing changed
			SQLHandle handle = null;

			try {
				PortalConnectorFactory.registerHandle(handleKey, HandleType.sql, handleProperties);
			} catch (DuplicateIdException e) {
				// registering the same handle again is absolutely acceptable
				// however, we don't want the handle to be registered again, because this would close and reopen the handle pool
			}

			Map<String, String> datasourceProperties = new HashMap<String, String>();
			datasourceProperties.put("attribute.path", substituteSingleProperty(basepath));
			datasourceProperties.put("sanitycheck", "false");
			datasourceProperties.put("autorepair", "false");
			datasourceProperties.put("sanitycheck2", "false");
			datasourceProperties.put("autorepair2", "false");
			// generally enable the cache for the datasource. This makes sure that implementations of CnMappublishHandler
			// that use the datasource will use the objects, that were cached while preparing the next bunch of objects
			datasourceProperties.put("cache", "true");

			// check whether instant publishing is turned on
			boolean instant = prefs.getFeature("instant_cr_publishing") && instantPublishing;
			// check whether the contentrepository is multichannelling aware
			boolean mccr = NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING) && isMultichannelling();

			// create the datasource instance
			CNWriteableDatasource ds = null;
			WritableMCCRDatasource mccrDs = null;

			if (mccr) {
				PortalConnectorFactory.registerDatasource(name, DatasourceType.mccr, datasourceProperties, Arrays.asList(handleKey), true);
				mccrDs = PortalConnectorFactory.createDatasource(WritableMCCRDatasource.class, name);
				// disable cache for this instance (cache will be enabled while preparing the next bunch of objects written into the cr)
				mccrDs.setCache(false);
				handle = (SQLHandle)mccrDs.getHandlePool().getHandle();
			} else {
				PortalConnectorFactory.registerDatasource(name, DatasourceType.contentrepository, datasourceProperties, Arrays.asList(handleKey), true);
				ds = PortalConnectorFactory.createDatasource(CNWriteableDatasource.class, name);
				// disable cache for this instance (cache will be enabled while preparing the next bunch of objects written into the cr)
				ds.setCache(false);
				// set the prefetch threshold to -1 (we want to prefetch always)
				ds.setPrefetchAttributesThreshold(-1);
				handle = (SQLHandle)ds.getHandlePool().getHandle();
			}

			if (handle == null) {
				throw new NodeException("Could not get valid handle for ContentRepository " + name);
			}

			// get configured contentmap keepalive interval and transform from seconds to milliseconds
			int configuredKeepaliveInterval = ObjectTransformer.getInt(prefs.getProperty("contentmap_keepalive_interval"), 0) * 1000;
			ContentMap map = new ContentMap(this, id, name, ds, mccrDs, prefs.getFeature("contentmap_files"), handle, true, instant, languageInformation, permissionInformation,
					diffDelete, configuredKeepaliveInterval);
			PublishHandlerStore.addPublishHandlers(map);
			return map;
		}

		/**
		 * Do the connectivity and structure test for SQL based CRs
		 * @param repair true to repair
		 * @return true iff structure was checked successfully
		 * @throws NodeException
		 */
		private boolean checkSQLBasedStructure(boolean repair) throws NodeException {
			SQLHandle handle = null;
			Transaction t = TransactionManager.getCurrentTransaction();
			NodeLogger l = NodeLogger.getNodeLogger(AbstractContentRepositoryStructure.class);
			NodeLogger handleLogger = NodeLogger.getNodeLogger(SQLHandle.class);

			try (NodeLogCollector collector = new NodeLogCollector(l, handleLogger)) {
				try {
					// check entries
					List<TagmapEntryInconsistencyModel> inconsistencies = checkEntryConsistency();
					boolean valid = inconsistencies.isEmpty();
					if (!inconsistencies.isEmpty()) {
						l.error(I18NHelper.get("cr.entries.inconsistencies"));
					}
					for (TagmapEntryInconsistencyModel inconsistency : inconsistencies) {
						l.error("- " + inconsistency.getDescription());
					}
					if (!inconsistencies.isEmpty()) {
						l.error("");
					}

					Map<String, String> handleProperties = getHandleParameters();

					handle = new SQLHandle("testingds");
					handle.init(handleProperties);
					if (handle.getDBHandle() == null) {
						throw new NodeException("Could not create database handle");
					}
					Datasource ds = null;

					if (isMultichannelling()) {
						ds = new WritableMCCRDatasource("testingds", new SimpleHandlePool(handle), null);
					} else {
						ds = new CNWriteableDatasource("testingds", new SimpleHandlePool(handle), null);
					}

					DB.startTransaction(handle.getDBHandle());
					AbstractContentRepositoryStructure checkStructure = AbstractContentRepositoryStructure.getStructure(ds, "testingds");
					valid &= checkStructure.checkStructureConsistency(repair);
					valid &= checkStructure.checkDataConsistency(repair);

					// update the contentrepository record
					DBUtils.executeUpdate("UPDATE contentrepository SET checkstatus = ?, checkdate = ?, checkresult = ? WHERE id = ?", new Object[] {
							valid ? ContentRepository.DATACHECK_STATUS_OK : ContentRepository.DATACHECK_STATUS_ERROR,
									t.getUnixTimestamp(),
									collector.getLog(),
									getId()
					});
					t.dirtObjectCache(ContentRepository.class, getId());
					return true;
				} catch (Exception e) {
					l.error("Error while " + (repair ? "repairing" : "checking") + " the contentrepository", e);
					DBUtils.executeUpdate("UPDATE contentrepository SET checkstatus = ?, checkdate = ?, checkresult = ? WHERE id = ?", new Object[] {
						ContentRepository.DATACHECK_STATUS_ERROR,
						t.getUnixTimestamp(),
						collector.getLog(),
						getId()
					});
					t.dirtObjectCache(ContentRepository.class, getId());
					return false;
				}
			} finally {
				if (handle != null && handle.getDBHandle() != null) {
					try {
						DB.commitTransaction(handle.getDBHandle());
					} catch (SQLException e) {
						logger.error("Error while commiting transaction", e);
					}
					DB.closeConnector(handle.getDBHandle());
				}
			}
		}

		/**
		 * Do the connectivity and structure test for Mesh CRs
		 * @param repair true to repair
		 * @return true iff structure was checked successfully
		 * @throws NodeException
		 */
		private boolean checkMeshStructure(boolean repair) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			NodeLogger l = NodeLogger.getNodeLogger(MeshPublisher.class);

			try (NodeLogCollector collector = new NodeLogCollector(Level.INFO, l)) {
				// check entries
				List<TagmapEntryInconsistencyModel> inconsistencies = checkEntryConsistency();
				boolean valid = inconsistencies.isEmpty();
				if (!inconsistencies.isEmpty()) {
					l.error(I18NHelper.get("cr.entries.inconsistencies"));
				}
				for (TagmapEntryInconsistencyModel inconsistency : inconsistencies) {
					l.error("- " + inconsistency.getDescription());
				}
				if (!inconsistencies.isEmpty()) {
					l.error("");
				}

				try (MeshPublisher mp = new MeshPublisher(this)) {
					valid &= mp.checkStatus();

					if (valid) {
						valid &= mp.checkSchemasAndProjects(repair, true);
						valid &= mp.checkRoles(repair);
						valid &= mp.checkDefaultRole(repair);
					}
					if (valid && repair) {
						mp.triggerJobProcessing();
						mp.waitForSchemaMigrations();
						mp.waitForNodeMigrations();
					}

					// update the contentrepository record
					DBUtils.executeUpdate("UPDATE contentrepository SET checkstatus = ?, checkdate = ?, checkresult = ? WHERE id = ?", new Object[] {
							valid ? ContentRepository.DATACHECK_STATUS_OK : ContentRepository.DATACHECK_STATUS_ERROR,
									t.getUnixTimestamp(),
									collector.getLog(),
									getId()
					});
					t.dirtObjectCache(ContentRepository.class, getId());
					return true;
				} catch (Exception e) {
					l.error("Error while " + (repair ? "repairing" : "checking") + " the contentrepository", e);
					DBUtils.executeUpdate("UPDATE contentrepository SET checkstatus = ?, checkdate = ?, checkresult = ? WHERE id = ?", new Object[] {
							ContentRepository.DATACHECK_STATUS_ERROR,
							t.getUnixTimestamp(),
							collector.getLog(),
							getId()
					});
					t.dirtObjectCache(ContentRepository.class, getId());
					return false;
				}
			}
		}

		/**
		 * Get the handle parameters for the SQL Handler of the SQL based CR
		 * @return handle parameters
		 * @throws NodeException
		 */
		private Map<String, String> getHandleParameters() throws NodeException {
			NodeConfig config = TransactionManager.getCurrentTransaction().getNodeConfig();
			Map<String, String> handleProperties = new HashMap<String, String>();

			// determine the driverclass
			@SuppressWarnings("rawtypes")
			Map driverClasses = config.getDefaultPreferences().getPropertyMap("contentrepository_driverclass");
			@SuppressWarnings("rawtypes")
			Map dummyQueries = config.getDefaultPreferences().getPropertyMap("contentrepository_dummyquery");
			String driverClass = ObjectTransformer.getString(driverClasses.get(dbType), null);
			String dummyquery = ObjectTransformer.getString(dummyQueries.get(dbType), null);
			if (ObjectTransformer.isEmpty(driverClass)) {
				throw new NodeException("Error while getting handle parameters for ContentRepository {" + name + "}: could not find driverClass for dbtype {"
						+ dbType + "} in configuration.");
			}

			handleProperties.put("url", substituteSingleProperty(url));
			handleProperties.put("driverClass", driverClass);
			handleProperties.put("username", substituteSingleProperty(username));
			handleProperties.put("passwd", isPasswordProperty() ? substituteSingleProperty(password) : password);
			handleProperties.put("type", "jdbc");
			handleProperties.put(SQLHandle.PARAM_NAME, name);
			handleProperties.put(SQLHandle.PARAM_FETCHSIZE, config.getDefaultPreferences().getProperty("contentrepository_fetchsize"));

			// make some settings for the connection pool
			handleProperties.put("pooling.whenExhaustedAction", "block");
			handleProperties.put("pooling.maxActive", "1");
			handleProperties.put("pooling.minIdle", "1");
			handleProperties.put("pooling.maxIdle", "1");
			handleProperties.put("pooling.testOnBorrow", "true");
			handleProperties.put("pooling.testWhileIdle", "false");
			handleProperties.put("pooling.testOnReturn", "false");
			handleProperties.put("pooling.maxWait", "600000");
			if (dummyquery != null) {
				handleProperties.put("pooling.validationQuery", dummyquery);
			}

			return handleProperties;
		}
	}

	/**
	 * Factory Implementation for Editable ContentRepository
	 */
	private static class EditableFactoryContentRepository extends FactoryContentRepository {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -5175325848743300166L;

		/**
		 * Flag to mark modified objects
		 */
		protected boolean modified = false;

		/**
		 * List of entries
		 */
		protected List<TagmapEntry> entries;

		/**
		 * Editable set of assigned fragments
		 */
		protected Set<CrFragment> fragments;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		protected EditableFactoryContentRepository(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param cr contentrepository
		 * @param info info
		 * @param asNewObject true for a new object, false for an editable version of the given object
		 * @throws NodeException
		 */
		protected EditableFactoryContentRepository(FactoryContentRepository cr,
				NodeObjectInfo info, boolean asNewObject) throws NodeException {
			super(asNewObject ? null : cr.getId(), info, getDataMap(cr), asNewObject ? -1 : cr.getUdate(), asNewObject ? null : cr.getGlobalId());
			if (asNewObject) {
				modified = true;
			}
		}

		@Override
		public List<TagmapEntry> getEntries() throws NodeException {
			if (entries == null) {
				entries = new Vector<TagmapEntry>(super.getEntries());
			}
			return entries;
		}

		@Override
		public Set<CrFragment> getAssignedFragments() throws NodeException {
			if (fragments == null) {
				fragments = new HashSet<>(super.getAssignedFragments());
			}
			return fragments;
		}

		@Override
		public void setName(String name) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.name, name)) {
				this.name = name;
				this.modified = true;
			}
		}

		@Override
		public void setCrType(Type type) throws ReadOnlyException {
			if (this.crType != type) {
				this.crType = type;
				this.modified = true;
			}
		}

		@Override
		public void setInstantPublishing(boolean instantPublishing) throws ReadOnlyException {
			if (this.instantPublishing != instantPublishing) {
				this.instantPublishing = instantPublishing;
				this.modified = true;
			}
		}

		@Override
		public void setLanguageInformation(boolean languageInformation) throws ReadOnlyException {
			if (this.languageInformation != languageInformation) {
				this.languageInformation = languageInformation;
				this.modified = true;
			}
		}

		@Override
		public void setPermissionInformation(boolean permissionInformation) throws ReadOnlyException {
			if (this.permissionInformation != permissionInformation) {
				this.permissionInformation = permissionInformation;
				this.modified = true;
			}
		}

		@Override
		public void setPermissionProperty(String permissionProperty) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.permissionProperty, permissionProperty)) {
				this.permissionProperty = permissionProperty;
				this.modified = true;
			}
		}

		@Override
		public void setDefaultPermission(String defaultPermission) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.defaultPermission, defaultPermission)) {
				this.defaultPermission = defaultPermission;
				this.modified = true;
			}
		}

		@Override
		public void setDiffDelete(boolean diffDelete) throws ReadOnlyException {
			if (this.diffDelete != diffDelete) {
				this.diffDelete = diffDelete;
				this.modified = true;
			}
		}

		@Override
		public void setBasepath(String basepath) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.basepath, basepath)) {
				this.basepath = basepath;
				this.modified = true;
			}
		}

		@Override
		public void setMultichannelling(boolean multichannelling) throws ReadOnlyException {
			if (multichannelling && crType == Type.cr) {
				setCrType(Type.mccr);
			} else if (crType == Type.mccr) {
				setCrType(Type.cr);
			}
		}

		@Override
		public void setDbType(String dbType) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.dbType, dbType)) {
				this.dbType = dbType;
				this.modified = true;
			}
		}

		@Override
		public void setUsername(String username) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.username, username)) {
				this.username = username;
				this.modified = true;
			}
		}

		@Override
		public void setPassword(String password) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.password, password)) {
				this.password = password;
				this.modified = true;
			}
		}

		@Override
		public void setPasswordProperty(boolean passwordProperty) throws ReadOnlyException {
			if (this.passwordProperty != passwordProperty) {
				this.passwordProperty = passwordProperty;
				this.modified = true;
			}
		}

		@Override
		public void setUrl(String url) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.url, url)) {
				this.url = url;
				this.modified = true;
			}
		}

		@Override
		public void setElasticsearch(String elasticsearch) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.elasticsearch, elasticsearch)) {
				this.elasticsearch = elasticsearch;
				this.modified = true;
			}
		}

		@Override
		public void setHttp2(boolean http2) throws ReadOnlyException {
			if (this.http2 != http2) {
				this.http2 = http2;
				this.modified = true;
			}
		}

		@Override
		public void setNoFoldersIndex(boolean noIndex) throws ReadOnlyException {
			if (this.noFoldersIndex != noIndex) {
				this.noFoldersIndex = noIndex;
				this.modified = true;
			}
		}

		@Override
		public void setNoFilesIndex(boolean noIndex) throws ReadOnlyException {
			if (this.noFilesIndex != noIndex) {
				this.noFilesIndex = noIndex;
				this.modified = true;
			}
		}

		@Override
		public void setNoPagesIndex(boolean noIndex) throws ReadOnlyException {
			if (this.noPagesIndex != noIndex) {
				this.noPagesIndex = noIndex;
				this.modified = true;
			}
		}

		@Override
		public void setNoFormsIndex(boolean noIndex) throws ReadOnlyException {
			if (this.noFormsIndex != noIndex) {
				this.noFormsIndex = noIndex;
				this.modified = true;
			}
		}

		@Override
		public void setProjectPerNode(boolean projectPerNode) throws ReadOnlyException {
			if (this.projectPerNode != projectPerNode) {
				this.projectPerNode = projectPerNode;
				this.modified = true;
			}
		}

		@Override
		public void setVersion(String version) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.version, version)) {
				this.version = version;
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException,
					NodeException {
			return save(null);
		}

		public boolean save(Integer userGroupId) throws InsufficientPrivilegesException,
					NodeException {
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();

			// make the name unique
			setName(UniquifyHelper.makeUnique(name, ContentRepository.MAX_NAME_LENGTH, "SELECT name FROM contentrepository WHERE id != ? AND name = ?",
					SeparatorType.blank, ObjectTransformer.getInt(getId(), -1)));
			// normalize data that must not be null
			setBasepath(ObjectTransformer.getString(getBasepath(), ""));
			setUsername(ObjectTransformer.getString(username, ""));
			setPassword(ObjectTransformer.getString(password, ""));
			setUrl(ObjectTransformer.getString(url, ""));
			setPermissionProperty(ObjectTransformer.getString(permissionProperty, ""));
			setDefaultPermission(ObjectTransformer.getString(defaultPermission, ""));
			setDbType(ObjectTransformer.getString(dbType, ""));
			setElasticsearch(ObjectTransformer.getString(elasticsearch, ""));
			setVersion(ObjectTransformer.getString(getVersion(), ""));

			boolean isModified = this.modified;
			boolean isNew = isEmptyId(getId());

			// save the contentrepository, if necessary
			if (isModified) {
				saveFactoryObject(this);
				this.modified = false;
				if (isNew) {
					UserGroup userGroup = null;
					if (userGroupId != null) {
						userGroup = t.getObject(UserGroup.class, userGroupId);
					}

					setInitialPermissions(this, userGroup); // null is ok
					t.addTransactional(new RefreshPermHandler(TYPE_CONTENTREPOSITORY, ObjectTransformer.getInt(getId(), 0)));
				}
			}

			// save tagmap entries
			List<TagmapEntry> entries = getEntries();
			List<Integer> entryIdsToRemove = new Vector<Integer>();

			if (tagmapEntryIds != null) {
				entryIdsToRemove.addAll(tagmapEntryIds);
			}
			for (TagmapEntry entry : entries) {
				entry.setContentRepositoryId(getId());
				isModified |= entry.save();

				// do not remove the entry, which was saved
				entryIdsToRemove.remove(entry.getId());
			}

			// remove parts which no longer exist
			if (!entryIdsToRemove.isEmpty()) {
				List<TagmapEntry> entriesToRemove = t.getObjects(TagmapEntry.class, entryIdsToRemove);

				for (TagmapEntry entry : entriesToRemove) {
					entry.delete();
				}
				isModified = true;
			}

			// save fragment assignment
			if (fragments != null) {
				Map<String, Object> data = new HashMap<>();
				data.put("uuid", "");
				isModified |= DBUtils.updateCrossTable("contentrepository_cr_fragment", "contentrepository_id", getId(), "cr_fragment_id",
						fragments.stream().map(CrFragment::getId).collect(Collectors.toSet()), data);
			}

			// logcmd
			if (isModified) {
				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, ContentRepository.TYPE_CONTENTREPOSITORY, getId(), 0, "ContentRepository.create");
					t.addTransactional(new TransactionalTriggerEvent(ContentRepository.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, ContentRepository.TYPE_CONTENTREPOSITORY, getId(), 0, "ContentRepository.update");
					t.addTransactional(new TransactionalTriggerEvent(ContentRepository.class, getId(), null, Events.UPDATE));
				}
			}

			if (isModified) {
				t.dirtObjectCache(ContentRepository.class, getId());
			}

			return isModified;
		}

		@Override
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentRepository oCr = (ContentRepository)original;

			// copy name (as "copy of ...")
			int i = 0;
			boolean nameUsed = false;
			while (i == 0 || nameUsed) {
				String tmp = "";
				if (i != 0) {
					tmp = " " + (i + 1);
				}
				CNI18nString i18n = new CNI18nString("copy_of");
				i18n.addParameter(tmp);
				i18n.addParameter(oCr.getName());
				String name = i18n.toString();
				if (DBUtils.select("SELECT id FROM contentrepository WHERE name = ?", ps -> ps.setString(1, name), DBUtils.IDS).isEmpty()) {
					setName(name);
					nameUsed = false;
				} else {
					nameUsed = true;
				}
				i++;
			}

			setCrType(oCr.getCrType());
			setDbType(oCr.getDbType());
			setElasticsearch(oCr.getElasticsearch());
			setInstantPublishing(oCr.isInstantPublishing());
			setLanguageInformation(oCr.isLanguageInformation());
			setPermissionInformation(oCr.isPermissionInformation());
			setPermissionProperty(oCr.getPermissionProperty());
			setProjectPerNode(oCr.isProjectPerNode());

			entries = new ArrayList<>();
			for (TagmapEntry oEntry : oCr.getEntries()) {
				TagmapEntry entry = t.createObject(TagmapEntry.class);
				entry.copyFrom(oEntry);
				entries.add(entry);
			}
		}

		@Override
		public void addEntry(String tagName, String mapName, int object, int targetType, AttributeType type, boolean multivalue, boolean stat,
				boolean optimized, String foreignLinkAttribute) throws NodeException {
			if (!type.validFor(crType)) {
				throw new NodeException(String.format("Attribute type %s is not valid for ContentRepository %s of type %s", type, name, crType));
			}
			Transaction t = TransactionManager.getCurrentTransaction();
			TagmapEntry entry = t.createObject(TagmapEntry.class);
			entry.setTagname(tagName);
			entry.setMapname(mapName);
			entry.setObject(object);
			entry.setTargetType(targetType);
			entry.setAttributeTypeId(type.getType());
			entry.setMultivalue(multivalue);
			entry.setStatic(stat);
			entry.setOptimized(optimized);
			entry.setForeignlinkAttribute(foreignLinkAttribute);
			getEntries().add(entry);
		}

		@Override
		public void addEntry(String tagName, String mapName, int object, int targetType, AttributeType type, boolean multivalue, boolean stat,
				boolean segmentfield, boolean displayfield, boolean urlfield, boolean noIndex) throws NodeException {
			if (!type.validFor(crType)) {
				throw new NodeException(String.format("Attribute type %s is not valid for ContentRepository %s of type %s", type, name, crType));
			}
			Transaction t = TransactionManager.getCurrentTransaction();
			TagmapEntry entry = t.createObject(TagmapEntry.class);
			entry.setTagname(tagName);
			entry.setMapname(mapName);
			entry.setObject(object);
			entry.setTargetType(targetType);
			entry.setAttributeTypeId(type.getType());
			entry.setMultivalue(multivalue);
			entry.setStatic(stat);
			entry.setSegmentfield(segmentfield);
			entry.setDisplayfield(displayfield);
			entry.setUrlfield(urlfield);
			entry.setNoIndex(noIndex);
			getEntries().add(entry);
		}
	}

	/**
	 * Factory Implementation for TagmapEntry
	 */
	private static class FactoryTagmapEntry extends TagmapEntry {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 3395009224537160738L;

		@DataField("tagname")
		@Updateable
		protected String tagname;

		@DataField("mapname")
		@Updateable
		protected String mapname;

		@DataField("object")
		@Updateable
		protected int object;

		@DataField("objtype")
		@Updateable
		protected int targetType;

		@DataField("attributetype")
		@Updateable
		protected int attributeTypeId;

		protected AttributeType attributeType;

		@DataField("multivalue")
		@Updateable
		protected boolean multivalue;

		@DataField("static")
		protected boolean isStatic;

		@DataField("optimized")
		@Updateable
		protected boolean optimized;

		@DataField("filesystem")
		@Updateable
		protected boolean filesystem;

		@DataField("foreignlinkattribute")
		@Updateable
		protected String foreignlinkAttribute;

		@DataField("foreignlinkattributerule")
		@Updateable
		protected String foreignlinkAttributeRule;

		@DataField("contentrepository_id")
		protected int contentrepositoryId;

		@DataField("category")
		@Updateable
		protected String category;

		@DataField("segmentfield")
		@Updateable
		protected boolean segmentfield;

		@DataField("no_index")
		@Updateable
		protected boolean noIndex;

		@DataField("displayfield")
		@Updateable
		protected boolean displayfield;

		@DataField("urlfield")
		@Updateable
		protected boolean urlfield;

		@DataField("elasticsearch")
		@Updateable
		protected String elasticsearch;

		@DataField("micronode_filter")
		@Updateable
		protected String micronodeFilter;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected FactoryTagmapEntry(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 * @param udate udate
		 * @param globalId global id
		 */
		protected FactoryTagmapEntry(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap, int udate,
				GlobalId globalId) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
			this.attributeType = AttributeType.getForType(attributeTypeId);
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

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryTagmapEntry(this, getFactory().getFactoryHandle(TagmapEntry.class).createObjectInfo(TagmapEntry.class, true), true);
		}

		@Override
		public String getTagname() {
			return tagname;
		}

		@Override
		public String getMapname() {
			return mapname;
		}

		@Override
		public int getObject() {
			return object;
		}

		@Override
		public int getTargetType() {
			return targetType;
		}

		@Override
		public int getAttributeTypeId() {
			return attributeTypeId;
		}

		@Override
		public AttributeType getAttributetype() {
			return attributeType;
		}

		@Override
		public boolean isMultivalue() {
			return multivalue;
		}

		@Override
		public boolean isStatic() {
			return isStatic;
		}

		@Override
		public boolean isOptimized() {
			return optimized;
		}

		@Override
		public boolean isFilesystem() {
			return filesystem;
		}

		@Override
		public String getForeignlinkAttribute() {
			return foreignlinkAttribute;
		}

		@Override
		public String getForeignlinkAttributeRule() {
			return foreignlinkAttributeRule;
		}

		@Override
		public ContentRepository getContentRepository() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(ContentRepository.class, contentrepositoryId);
		}

		@Override
		public String getCategory() {
			return category;
		}

		@Override
		public boolean isSegmentfield() {
			return segmentfield;
		}

		@Override
		public boolean isDisplayfield() {
			return displayfield;
		}

		@Override
		public boolean isUrlfield() {
			return urlfield;
		}

		@Override
		public boolean isNoIndex() {
			return noIndex;
		};

		@Override
		public String getElasticsearch() {
			return elasticsearch;
		}

		@Override
		public String getMicronodeFilter() {
			return micronodeFilter;
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException,
					NodeException {
			// delete the tagmap entry
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentRepositoryFactory factory = (ContentRepositoryFactory) t.getObjectFactory(TagmapEntry.class);

			factory.deleteEntry(this);
		}
	}

	/**
	 * Factory Implementation for Editable TagmapEntry
	 */
	private static class EditableFactoryTagmapEntry extends FactoryTagmapEntry {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -8698049181343853975L;

		/**
		 * Flag to mark modified objects
		 */
		protected boolean modified = false;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected EditableFactoryTagmapEntry(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param entry object
		 * @param info info
		 * @param asNewObject true for new objects, false for editable existing objects
		 * @throws NodeException
		 */
		protected EditableFactoryTagmapEntry(FactoryTagmapEntry entry, NodeObjectInfo info,
				boolean asNewObject) throws NodeException {
			super(asNewObject ? null : entry.getId(), info, getDataMap(entry), asNewObject ? -1 : entry.getUdate(), asNewObject ? null : entry.getGlobalId());
			if (asNewObject) {
				modified = true;
				contentrepositoryId = 0;
			}
		}

		@Override
		public void setAttributeTypeId(int attributeTypeId) throws ReadOnlyException {
			if (this.attributeTypeId != attributeTypeId) {
				this.attributeTypeId = attributeTypeId;
				this.attributeType = AttributeType.getForType(attributeTypeId);
				this.modified = true;
			}
		}

		@Override
		public void setCategory(String category) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.category, category)) {
				this.category = category;
				this.modified = true;
			}
		}

		@Override
		public void setContentRepositoryId(Integer contentRepositoryId) throws ReadOnlyException, NodeException {
			int cr = ObjectTransformer.getInt(contentRepositoryId, -1);

			if (this.contentrepositoryId != cr) {
				if (this.contentrepositoryId > 0) {
					throw new NodeException("Cannot set contentrepository id, contentrepository id already set");
				}
				this.contentrepositoryId = cr;
				this.modified = true;
			}
		}

		@Override
		public void setForeignlinkAttribute(String foreignlinkAttribute) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.foreignlinkAttribute, foreignlinkAttribute)) {
				this.foreignlinkAttribute = foreignlinkAttribute;
				this.modified = true;
			}
		}

		@Override
		public void setForeignlinkAttributeRule(String rule) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.foreignlinkAttributeRule, rule)) {
				this.foreignlinkAttributeRule = rule;
				this.modified = true;
			}
		}

		@Override
		public void setMapname(String mapname) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.mapname, mapname)) {
				this.mapname = mapname;
				this.modified = true;
			}
		}

		@Override
		public void setTagname(String tagname) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.tagname, tagname)) {
				this.tagname = tagname;
				this.modified = true;
			}
		}

		@Override
		public void setMultivalue(boolean multivalue) throws ReadOnlyException {
			if (this.multivalue != multivalue) {
				this.multivalue = multivalue;
				this.modified = true;
			}
		}

		@Override
		public void setObject(int object) throws ReadOnlyException {
			if (this.object != object) {
				this.object = object;
				this.modified = true;
			}
		}

		@Override
		public void setOptimized(boolean optimized) throws ReadOnlyException {
			if (this.optimized != optimized) {
				this.optimized = optimized;
				this.modified = true;
			}
		}

		@Override
		public void setFilesystem(boolean filesystem) throws ReadOnlyException {
			if (this.filesystem != filesystem) {
				this.filesystem = filesystem;
				this.modified = true;
			}
		}

		@Override
		public void setStatic(boolean stat) throws ReadOnlyException {
			if (isStatic != stat) {
				this.isStatic = stat;
				this.modified = true;
			}
		}

		@Override
		public void setTargetType(int targetType) throws ReadOnlyException {
			if (this.targetType != targetType) {
				this.targetType = targetType;
				this.modified = true;
			}
		}

		@Override
		public void setNoIndex(boolean noIndex) throws ReadOnlyException, NodeException {
			if (this.noIndex != noIndex) {
				this.noIndex = noIndex;
				this.modified = true;
			}
		}

		@Override
		public void setSegmentfield(boolean segmentfield) throws ReadOnlyException, NodeException {
			if (this.segmentfield != segmentfield) {
				this.segmentfield = segmentfield;
				this.modified = true;
			}
		}

		@Override
		public void setDisplayfield(boolean displayfield) throws ReadOnlyException, NodeException {
			if (this.displayfield != displayfield) {
				this.displayfield = displayfield;
				this.modified = true;
			}
		}

		@Override
		public void setUrlfield(boolean urlfield) throws ReadOnlyException, NodeException {
			if (this.urlfield != urlfield) {
				this.urlfield = urlfield;
				this.modified = true;
			}
		}

		@Override
		public void setElasticsearch(String elasticsearch) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.elasticsearch, elasticsearch)) {
				this.elasticsearch = elasticsearch;
				this.modified = true;
			}
		}

		@Override
		public void setMicronodeFilter(String micronodeFilter) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.micronodeFilter, micronodeFilter)) {
				this.micronodeFilter = micronodeFilter;
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException,
					NodeException {
			assertEditable();
			boolean isModified = this.modified;

			// save the tagmap entry, if necessary
			if (isModified) {
				// we cannot save a tagmap entry without a contentrepository_id
				if (contentrepositoryId <= 0) {
					throw new NodeException("Error while saving tagmap entry, it does not belong to a contentrepository");
				}

				// make sure the string values are not null
				this.mapname = ObjectTransformer.getString(this.mapname, "");
				this.tagname = ObjectTransformer.getString(this.tagname, "");
				this.category = ObjectTransformer.getString(this.category, "");
				this.foreignlinkAttribute = ObjectTransformer.getString(this.foreignlinkAttribute, "");
				this.foreignlinkAttributeRule = ObjectTransformer.getString(this.foreignlinkAttributeRule, "");
				this.elasticsearch = ObjectTransformer.getString(this.elasticsearch, "");
				if ("null".equals(this.elasticsearch)) {
					this.elasticsearch = "";
				}

				saveFactoryObject(this);
				this.modified = false;
			}

			return isModified;
		}

		@Override
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);
			TagmapEntry oEntry = (TagmapEntry)original;
			setTagname(oEntry.getTagname());
			setMapname(oEntry.getMapname());
			setObject(oEntry.getObject());
			setTargetType(oEntry.getTargetType());
			setAttributeTypeId(oEntry.getAttributeTypeId());
			setMultivalue(oEntry.isMultivalue());
			setStatic(oEntry.isStatic());
			setOptimized(oEntry.isOptimized());
			setFilesystem(oEntry.isFilesystem());
			setForeignlinkAttribute(oEntry.getForeignlinkAttribute());
			setForeignlinkAttributeRule(oEntry.getForeignlinkAttributeRule());
			setCategory(oEntry.getCategory());
			setDisplayfield(oEntry.isDisplayfield());
			setSegmentfield(oEntry.isSegmentfield());
			setUrlfield(oEntry.isUrlfield());
			setNoIndex(oEntry.isNoIndex());
			setElasticsearch(oEntry.getElasticsearch());
		}
	}

	/**
	 * Readonly factory implementation of a {@link CrFragment}
	 */
	private static class FactoryCrFragment extends AbstractContentObject implements CrFragment {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -5146108299629838748L;

		@RestModel(update = { "name" })
		protected ContentRepositoryFragmentModel model;

		/**
		 * List of entry IDs
		 */
		protected List<Integer> entryIds;

		/**
		 * Create an empty instance
		 * @param info info
		 * @throws NodeException
		 */
		protected FactoryCrFragment(NodeObjectInfo info) {
			super(null, info);
			model = new ContentRepositoryFragmentModel();
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 * @param udate udate
		 * @param globalId global id
		 */
		protected FactoryCrFragment(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap,
				int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
			this.udate = udate;
			this.globalId = globalId;
		}

		@Override
		public Object get(String key) {
			try {
				if (model != null) {
					return ClassHelper.invokeGetter(model, key);
				}
			} catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			return super.get(key);
		}

		@SuppressWarnings("unused")
		public void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
				this.model.setId(id);
			}
		}

		@Override
		public void setGlobalId(GlobalId globalId) throws ReadOnlyException, NodeException {
			super.setGlobalId(globalId);
			this.model.setGlobalId(globalId.toString());
		}

		@Override
		public String getName() {
			return model.getName();
		}

		@Override
		public List<CrFragmentEntry> getEntries() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObjects(CrFragmentEntry.class, loadEntryIds(), getObjectInfo().isEditable());
		}

		@Override
		public ContentRepositoryFragmentModel getModel() {
			return model;
		}

		@Override
		public NodeObject copy() throws NodeException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			loadEntryIds();
			for (Integer id : entryIds) {
				t.dirtObjectCache(CrFragmentEntry.class, id, false);
			}
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			// delete all entries
			for (CrFragmentEntry entry : getEntries()) {
				entry.delete();
			}
			deletedFactoryObject(this);
		}

		/**
		 * Load the entry IDs, if not done before
		 * @return list of entry IDs
		 * @throws NodeException
		 */
		protected synchronized List<Integer> loadEntryIds() throws NodeException {
			if (entryIds == null) {
				if (isEmptyId(getId())) {
					entryIds = new ArrayList<>();
				} else {
					entryIds = DBUtils.select("SELECT id FROM cr_fragment_entry WHERE cr_fragment_id = ?",
							ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
				}
			}

			return entryIds;
		}
	}

	/**
	 * Editable factory implementation of a {@link CrFragment}
	 */
	private static class EditableFactoryCrFragment extends FactoryCrFragment {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -4344093265869744064L;

		/**
		 * Flag to mark modified objects
		 */
		protected boolean modified = false;

		/**
		 * List of entries
		 */
		protected List<CrFragmentEntry> entries;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		protected EditableFactoryCrFragment(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param crFragment original
		 * @param info info
		 * @param asNewObject true for a new object, false for an editable version of the given object
		 * @throws NodeException
		 */
		protected EditableFactoryCrFragment(FactoryCrFragment crFragment,
				NodeObjectInfo info, boolean asNewObject) throws NodeException {
			super(asNewObject ? null : crFragment.getId(), info, getDataMap(crFragment), asNewObject ? -1 : crFragment.getUdate(),
					asNewObject ? null : crFragment.getGlobalId());
			if (asNewObject) {
				modified = true;
			}
		}

		@Override
		public List<CrFragmentEntry> getEntries() throws NodeException {
			if (entries == null) {
				entries = new ArrayList<>(super.getEntries());
			}
			return entries;
		}

		@Override
		public void setName(String name) throws ReadOnlyException {
			if (!StringUtils.isEqual(model.getName(), name)) {
				model.setName(name);
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			return save(null);
		}

		@Override
		public boolean save(Integer userGroupId) throws InsufficientPrivilegesException, NodeException {
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();

			// make the name unique
			setName(UniquifyHelper.makeUnique(model.getName(), ContentRepository.MAX_NAME_LENGTH, "SELECT name FROM cr_fragment WHERE id != ? AND name = ?",
					SeparatorType.blank, ObjectTransformer.getInt(getId(), -1)));

			boolean isModified = this.modified;
			boolean isNew = isEmptyId(getId());

			// save the cr_fragment, if necessary
			if (isModified) {
				saveFactoryObject(this);
				this.modified = false;
				if (isNew) {
					UserGroup userGroup = null;
					if (userGroupId != null) {
						userGroup = t.getObject(UserGroup.class, userGroupId);
					}

					setInitialPermissions(this, userGroup); // null is ok
				}
			}

			// save fragment entries
			List<CrFragmentEntry> entries = getEntries();
			List<Integer> entryIdsToRemove = new ArrayList<>();

			if (entryIds != null) {
				entryIdsToRemove.addAll(entryIds);
			}
			for (CrFragmentEntry entry : entries) {
				entry.setCrFragmentId(getId());
				isModified |= entry.save();

				// do not remove the entry, which was saved
				entryIdsToRemove.remove(entry.getId());
			}

			// remove parts which no longer exist
			if (!entryIdsToRemove.isEmpty()) {
				List<CrFragmentEntry> entriesToRemove = t.getObjects(CrFragmentEntry.class, entryIdsToRemove);

				for (CrFragmentEntry entry : entriesToRemove) {
					entry.delete();
				}
				isModified = true;
			}

			// logcmd
			if (isModified) {
				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, CrFragment.TYPE_CR_FRAGMENT, getId(), 0, "CrFragment.create");
					t.addTransactional(new TransactionalTriggerEvent(CrFragment.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, CrFragment.TYPE_CR_FRAGMENT, getId(), 0, "CrFragment.update");
					t.addTransactional(new TransactionalTriggerEvent(CrFragment.class, getId(), null, Events.UPDATE));
				}
			}

			if (isModified) {
				t.dirtObjectCache(CrFragment.class, getId());
			}

			return isModified;
		}

		@Override
		public void fromModel(ContentRepositoryFragmentModel model) throws ReadOnlyException, NodeException {
			this.modified = updateModel(this, model);
		}
	}

	/**
	 * Readonly factory implementation of a {@link CrFragmentEntry}
	 */
	private static class FactoryCrFragmentEntry extends AbstractContentObject implements CrFragmentEntry {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2813293515615986297L;

		@DataField("cr_fragment_id")
		protected int crFragmentId;

		@RestModel(update = { "tagname", "mapname", "obj_type", "attribute_type", "multivalue", "optimized", "filesystem", "target_type",
				"foreignlink_attribute", "foreignlink_attribute_rule", "category", "displayfield", "segmentfield", "urlfield", "no_index", "elasticsearch", "micronode_filter" })
		protected ContentRepositoryFragmentEntryModel model;

		protected AttributeType attributeType;

		/**
		 * Create an empty instance
		 * @param info info
		 * @throws NodeException
		 */
		protected FactoryCrFragmentEntry(NodeObjectInfo info) {
			super(null, info);
			model = new ContentRepositoryFragmentEntryModel();
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 * @param udate udate
		 * @param globalId global id
		 */
		protected FactoryCrFragmentEntry(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap,
				int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			// special handling for "elasticsearch"
			if (dataMap.get("elasticsearch") instanceof String) {
				try {
					dataMap.put("elasticsearch", mapper.readValue(ObjectTransformer.getString(dataMap.get("elasticsearch"), null), JsonNode.class));
				} catch (IOException e) {
					logger.error(String.format("Error while setting elasticsearch config to %s", dataMap.get("elasticsearch")), e);
				}
			}
			setDataMap(this, dataMap);
			this.attributeType = AttributeType.getForType(model.getAttributeType());
			this.udate = udate;
			this.globalId = globalId;
		}

		@Override
		public Object get(String key) {
			try {
				if (model != null) {
					return ClassHelper.invokeGetter(model, key);
				}
			} catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			return super.get(key);
		}

		@SuppressWarnings("unused")
		public void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
				this.model.setId(id);
			}
		}

		@Override
		public void setGlobalId(GlobalId globalId) throws ReadOnlyException, NodeException {
			super.setGlobalId(globalId);
			this.model.setGlobalId(globalId.toString());
		}

		@Override
		public int getCrFragmentId() {
			return crFragmentId;
		}

		@Override
		public String getTagname() {
			return model.getTagname();
		}

		@Override
		public String getMapname() {
			return model.getMapname();
		}

		@Override
		public int getObjType() {
			return ObjectTransformer.getInt(model.getObjType(), 0);
		}

		@Override
		public int getTargetType() {
			return ObjectTransformer.getInt(model.getTargetType(), 0);
		}

		@Override
		public int getAttributeTypeId() {
			return ObjectTransformer.getInt(model.getAttributeType(), 0);
		}

		@Override
		public AttributeType getAttributetype() {
			return attributeType;
		}

		@Override
		public boolean isMultivalue() {
			return ObjectTransformer.getBoolean(model.getMultivalue(), false);
		}

		@Override
		public boolean isOptimized() {
			return ObjectTransformer.getBoolean(model.getOptimized(), false);
		}

		@Override
		public boolean isFilesystem() {
			return ObjectTransformer.getBoolean(model.getFilesystem(), false);
		}

		@Override
		public String getForeignlinkAttribute() {
			return model.getForeignlinkAttribute();
		}

		@Override
		public String getForeignlinkAttributeRule() {
			return model.getForeignlinkAttributeRule();
		}

		@Override
		public String getCategory() {
			return model.getCategory();
		}

		@Override
		public boolean isSegmentfield() {
			return ObjectTransformer.getBoolean(model.getSegmentfield(), false);
		}

		@Override
		public boolean isDisplayfield() {
			return ObjectTransformer.getBoolean(model.getDisplayfield(), false);
		}

		@Override
		public boolean isUrlfield() {
			return ObjectTransformer.getBoolean(model.getUrlfield(), false);
		}

		@Override
		public boolean isNoIndex() {
			return ObjectTransformer.getBoolean(model.getNoIndex(), false);
		}

		@Override
		public String getElasticsearch() {
			return ObjectTransformer.getString(model.getElasticsearch(), null);
		}

		@Override
		public String getMicronodeFilter() {
			return model.getMicronodeFilter();
		}

		@Override
		public ContentRepositoryFragmentEntryModel getModel() {
			return model;
		}

		@Override
		public NodeObject copy() throws NodeException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			deletedFactoryObject(this);
		}
	}

	/**
	 * Editable factory implementation of {@link CrFragmentEntry}
	 */
	private static class EditableFactoryCrFragmentEntry extends FactoryCrFragmentEntry {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2620178630328893940L;

		/**
		 * Flag to mark modified objects
		 */
		protected boolean modified = false;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		protected EditableFactoryCrFragmentEntry(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param entry original
		 * @param info info
		 * @param asNewObject true for a new object, false for an editable version of the given object
		 * @throws NodeException
		 */
		protected EditableFactoryCrFragmentEntry(FactoryCrFragmentEntry entry,
				NodeObjectInfo info, boolean asNewObject) throws NodeException {
			super(asNewObject ? null : entry.getId(), info, getDataMap(entry), asNewObject ? -1 : entry.getUdate(), asNewObject ? null : entry.getGlobalId());
			if (asNewObject) {
				modified = true;
			}
		}

		@Override
		public void setCrFragmentId(int crFragmentId) throws NodeException {
			if (this.crFragmentId != crFragmentId) {
				if (this.crFragmentId > 0) {
					throw new NodeException("Cannot set fragment id, fragment id already set");
				}
				this.crFragmentId = crFragmentId;
				this.modified = true;
			}
		}

		@Override
		public void setAttributeTypeId(int attributeTypeId) throws ReadOnlyException {
			if (getAttributeTypeId() != attributeTypeId) {
				model.setAttributeType(attributeTypeId);
				this.attributeType = AttributeType.getForType(attributeTypeId);
				this.modified = true;
			}
		}

		@Override
		public void setCategory(String category) throws ReadOnlyException {
			if (!StringUtils.isEqual(getCategory(), category)) {
				model.setCategory(category);
				this.modified = true;
			}
		}

		@Override
		public void setDisplayfield(boolean displayfield) throws ReadOnlyException, NodeException {
			if (isDisplayfield() != displayfield) {
				model.setDisplayfield(displayfield);
				this.modified = true;
			}
		}

		@Override
		public void setElasticsearch(String elasticsearch) throws ReadOnlyException, NodeException {
			if (!StringUtils.isEqual(getElasticsearch(), elasticsearch)) {
				if (!ObjectTransformer.isEmpty(elasticsearch)) {
					try {
						model.setElasticsearch(mapper.readValue(elasticsearch, JsonNode.class));
					} catch (IOException e) {
						throw new NodeException(String.format("Error while setting elasticsearch setting to '%s'", elasticsearch), e);
					}
				} else {
					model.setElasticsearch(null);
				}
				this.modified = true;
			}
		}

		@Override
		public void setMicronodeFilter(String micronodeFilter) throws ReadOnlyException {
			if (!StringUtils.isEqual(model.getMicronodeFilter(), micronodeFilter)) {
				model.setMicronodeFilter(micronodeFilter);
				this.modified = true;
			}
		}

		@Override
		public void setFilesystem(boolean filesystem) throws ReadOnlyException {
			if (isFilesystem() != filesystem) {
				model.setFilesystem(filesystem);
				this.modified = true;
			}
		}

		@Override
		public void setForeignlinkAttribute(String foreignlinkAttribute) throws ReadOnlyException {
			if (!StringUtils.isEqual(getForeignlinkAttribute(), foreignlinkAttribute)) {
				model.setForeignlinkAttribute(foreignlinkAttribute);
				this.modified = true;
			}
		}

		@Override
		public void setForeignlinkAttributeRule(String foreignlinkAttributeRule) throws ReadOnlyException {
			if (!StringUtils.isEqual(getForeignlinkAttributeRule(), foreignlinkAttributeRule)) {
				model.setForeignlinkAttributeRule(foreignlinkAttributeRule);
				this.modified = true;
			}
		}

		@Override
		public void setMapname(String mapname) throws ReadOnlyException {
			if (!StringUtils.isEqual(getMapname(), mapname)) {
				model.setMapname(mapname);
				this.modified = true;
			}
		}

		@Override
		public void setMultivalue(boolean multivalue) throws ReadOnlyException {
			if (isMultivalue() != multivalue) {
				model.setMultivalue(multivalue);
				this.modified = true;
			}
		}

		@Override
		public void setObjType(int objType) throws ReadOnlyException {
			if (getObjType() != objType) {
				model.setObjType(objType);
				this.modified = true;
			}
		}

		@Override
		public void setOptimized(boolean optimized) throws ReadOnlyException {
			if (isOptimized() != optimized) {
				model.setOptimized(optimized);
				this.modified = true;
			}
		}

		@Override
		public void setSegmentfield(boolean segmentfield) throws ReadOnlyException, NodeException {
			if (isSegmentfield() != segmentfield) {
				model.setSegmentfield(segmentfield);
				this.modified = true;
			}
		}

		@Override
		public void setTagname(String tagname) throws ReadOnlyException {
			if (!StringUtils.isEqual(getTagname(), tagname)) {
				model.setTagname(tagname);
				this.modified = true;
			}
		}

		@Override
		public void setTargetType(int targetType) throws ReadOnlyException {
			if (getTargetType() != targetType) {
				model.setTargetType(targetType);
				this.modified = true;
			}
		}

		@Override
		public void setUrlfield(boolean urlfield) throws ReadOnlyException, NodeException {
			if (isUrlfield() != urlfield) {
				model.setUrlfield(urlfield);
				this.modified = true;
			}
		}

		@Override
		public void setNoIndex(boolean noIndex) throws ReadOnlyException ,NodeException {
			if (isNoIndex() != noIndex) {
				model.setNoIndex(noIndex);
				this.modified = true;
			}
		};

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			assertEditable();
			boolean isModified = this.modified;

			// save the entry, if necessary
			if (isModified) {
				// we cannot save a entry without a cr_fragment_id
				if (crFragmentId <= 0) {
					throw new NodeException("Error while saving cr_fragment entry, it does not belong to a fragment");
				}

				// make sure the string values are not null
				model.setTargetType(getTargetType());
				model.setDisplayfield(isDisplayfield());
				model.setFilesystem(isFilesystem());
				model.setMultivalue(isMultivalue());
				model.setOptimized(isOptimized());
				model.setSegmentfield(isSegmentfield());
				model.setUrlfield(isUrlfield());
				model.setNoIndex(isNoIndex());
				setMapname(ObjectTransformer.getString(getMapname(), ""));
				setTagname(ObjectTransformer.getString(getTagname(), ""));
				setCategory(ObjectTransformer.getString(getCategory(), ""));
				setForeignlinkAttribute(ObjectTransformer.getString(getForeignlinkAttribute(), ""));
				setForeignlinkAttributeRule(ObjectTransformer.getString(getForeignlinkAttributeRule(), ""));
				setElasticsearch(ObjectTransformer.getString(getElasticsearch(), ""));
				if ("null".equals(getElasticsearch())) {
					setElasticsearch("");
				}

				saveFactoryObject(this);
				this.modified = false;
			}

			return isModified;
		}

		@Override
		public void fromModel(ContentRepositoryFragmentEntryModel model) throws ReadOnlyException, NodeException {
			this.modified = updateModel(this, model);

			// special handling of elasticsearch
			if (model.getElasticsearch() != null) {
				setElasticsearch(model.getElasticsearch().toString());
			}
		}
	}

	/**
	 * Delete the given entry
	 * @param factoryTagmapEntry entry
	 */
	@SuppressWarnings("unchecked")
	public void deleteEntry(FactoryTagmapEntry entry) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<TagmapEntry> toDelete = getDeleteList(t, TagmapEntry.class);

		toDelete.add(entry);
	}

	/**
	 * Delete the given contentrepository
	 * @param factoryContentRepository contentrepository
	 */
	@SuppressWarnings("unchecked")
	public void deleteCR(FactoryContentRepository cr) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<ContentRepository> toDelete = getDeleteList(t, ContentRepository.class);

		toDelete.add(cr);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, TagmapEntry.class)) {
			// delete the tagmap entries
			flushDelete("DELETE FROM tagmap WHERE id IN", TagmapEntry.class);
		}

		if (!isEmptyDeleteList(t, ContentRepository.class)) {
			// update all nodes losing their contentrepository
			Collection<ContentRepository> deletedCRs = getDeleteList(t, ContentRepository.class);

			for (ContentRepository cr : deletedCRs) {
				DBUtils.executeUpdate("UPDATE node SET contentrepository_id = ? WHERE contentrepository_id = ?", new Object[] { 0, cr.getId()});

				List<Node> nodes = cr.getNodes();

				for (Node node : nodes) {
					t.dirtObjectCache(Node.class, node.getId());
				}

				// add logcmd
				ActionLogger.logCmd(ActionLogger.DEL, ContentRepository.TYPE_CONTENTREPOSITORY, cr.getId(), 0, "ContentRepository.delete");
				Events.trigger(cr, null, Events.DELETE);
			}

			// delete the contentrepositories
			flushDelete("DELETE FROM contentrepository WHERE id IN", ContentRepository.class);

			// permissions
			flushDelete("DELETE FROM perm WHERE o_type = " + ContentRepository.TYPE_CONTENTREPOSITORY + " AND o_id IN", ContentRepository.class);
			// update the PermissionStore
			for (ContentRepository cr : deletedCRs) {
				t.addTransactional(new RemovePermsTransactional(ContentRepository.TYPE_CONTENTREPOSITORY, ObjectTransformer.getInt(cr.getId(), 0)));
			}
		}

		if (!isEmptyDeleteList(t, CrFragmentEntry.class)) {
			flushDelete("DELETE FROM cr_fragment_entry WHERE ID IN", CrFragmentEntry.class);
		}

		if (!isEmptyDeleteList(t, CrFragment.class)) {
			Collection<CrFragment> deletedCrFragments = getDeleteList(t, CrFragment.class);

			for (CrFragment cr : deletedCrFragments) {
				// add logcmd
				ActionLogger.logCmd(ActionLogger.DEL, CrFragment.TYPE_CR_FRAGMENT, cr.getId(), 0, "CrFragment.delete");
				Events.trigger(cr, null, Events.DELETE);
			}

			// delete the fragments
			flushDelete("DELETE FROM cr_fragment WHERE id IN", CrFragment.class);

			// permissions
			flushDelete("DELETE FROM perm WHERE o_type = " + CrFragment.TYPE_CR_FRAGMENT + " AND o_id IN", CrFragment.class);
			// update the PermissionStore
			for (CrFragment cr : deletedCrFragments) {
				t.addTransactional(new RemovePermsTransactional(CrFragment.TYPE_CR_FRAGMENT, ObjectTransformer.getInt(cr.getId(), 0)));
			}
		}
	}

	/**
	 * Either get the groups of the current transaction user (including all parent groups), or - if startUserGroup is given -
	 * get the startUserGroup and all its parent groups
	 * @param startUserGroup start group (may be null)
	 * @return list of groups
	 * @throws NodeException
	 */
	private static List<UserGroup> getGroups(UserGroup startUserGroup) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		List<UserGroup> groups = null;
		if (startUserGroup == null) {
			// get the current user
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			// get the groups (and all mother groups)
			groups = user.getAllGroupsWithParents();
		} else {
			// Get all parent groups of the given group
			groups = startUserGroup.getParents();
			// Add the startGroup to this list also
			groups.add(startUserGroup);
		}

		return groups;
	}

	/**
	 * Set the initial permission for the (just created) contentrepository for the user of the transaction
	 * The permissions will be set for the current/given group, and all parent groups.
	 *
	 * @param rep            content repository
	 * @param startUserGroup Group to set permission for, the parents of this group will be resolved automatically.
	 *                       If null is passed it will take the group from the user of the current transaction.
	 * @throws NodeException
	 */
	private static void setInitialPermissions(ContentRepository rep, UserGroup startUserGroup) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		for (UserGroup userGroup : getGroups(startUserGroup)) {
			PermHandler groupPermHandler = t.getGroupPermHandler(userGroup.getId());
			String perm = null;

			// Check if the group has permission to grant someone else permissions on contentrepositories.
			// This perm entry usually has the o_id 86 (= GCN menu ID), so we have to pass null to find the entry.
			if (groupPermHandler.checkPermissionBit(ContentRepository.TYPE_CONTENTREPOSITORIES, null, 1)) {
				// Give View, Modify, Grant & Delete permissions
				perm = "11110000000000000000000000000000";
			} else {
				// Give View, Modify & Delete, but no Grant permission
				perm = "10110000000000000000000000000000";
			}

			DBUtils.executeInsert("INSERT INTO perm (usergroup_id, o_type, o_id, perm) VALUES (?, ?, ?, ?)",
					new Object[] { userGroup.getId(), ContentRepository.TYPE_CONTENTREPOSITORY, rep.getId(), perm });
		}
	}

	/**
	 * Set the initial permission for the (just created) cr_fragment for the user of the transaction.
	 * The permissions will be set for the current/given group, and all parent groups.
	 *
	 * @param fragment CR fragment
	 * @param startUserGroup optional group to set the permissions
	 * @throws NodeException
	 */
	private static void setInitialPermissions(CrFragment fragment, UserGroup startUserGroup) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		for (UserGroup userGroup : getGroups(startUserGroup)) {
			PermHandler groupPermHandler = t.getGroupPermHandler(userGroup.getId());
			String perm = null;

			// Check if the group has permission to grant someone else permissions on cr_fragments.
			if (groupPermHandler.checkPermissionBit(CrFragment.TYPE_CR_FRAGMENTS, null, PERM_CHANGE_PERM)) {
				// Give View, Modify, Grant & Delete permissions
				perm = Permissions.get(PERM_VIEW, PERM_CHANGE_PERM, PERM_CONTENTREPOSITORY_UPDATE, PERM_CONTENTREPOSITORY_DELETE).toString();
			} else {
				// Give View, Modify & Delete, but no Grant permission
				perm = Permissions.get(PERM_VIEW, PERM_CONTENTREPOSITORY_UPDATE, PERM_CONTENTREPOSITORY_DELETE).toString();
			}

			PermHandler.setPermissions(CrFragment.TYPE_CR_FRAGMENT, fragment.getId(), Arrays.asList(userGroup), perm);
		}
	}
}
