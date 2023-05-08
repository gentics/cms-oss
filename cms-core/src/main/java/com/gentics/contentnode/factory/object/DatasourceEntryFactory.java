/*
 * @author clemens
 * @date 02.02.2007
 * @version $Id: DatasourceEntryFactory.java,v 1.8.2.1.4.2 2011-03-08 12:28:10 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.RemovePermsTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

@DBTables({ @DBTable(clazz = Datasource.class, name = "datasource"),
	@DBTable(clazz = DatasourceEntry.class, name = "datasource_value") })
public class DatasourceEntryFactory extends AbstractFactory {

	/**
	 * SQL Statement to select a single datasource
	 */
	protected final static String SELECT_DATASOURCE_SQL = createSelectStatement("datasource");

	/**
	 * SQL Statement to select a single datasource_value
	 */
	protected final static String SELECT_DATASOURCE_VALUE_SQL = createSelectStatement("datasource_value");

	/**
	 * SQL Statement to select versioned datasource data
	 */
	protected final static String SELECT_VERSIONED_DATASOURCE_SQL = "SELECT * FROM datasource_nodeversion WHERE id = ? AND nodeversiontimestamp = "
			+ "(SELECT MAX(nodeversiontimestamp) FROM datasource_nodeversion "
			+ "WHERE (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? AND id = ? GROUP BY id)";

	/**
	 * SQL Params for the selection of versioned datasource data
	 */
	protected final static VersionedSQLParam[] SELECT_VERSIONED_DATASOURCE_PARAMS = {
		VersionedSQLParam.ID, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.ID};

	/**
	 * SQL Statement to select versioned datasource_value data
	 */
	protected final static String SELECT_VERSIONED_DATASOURCE_VALUE_SQL = "SELECT * FROM datasource_value_nodeversion WHERE id = ? AND nodeversiontimestamp = "
			+ "(SELECT MAX(nodeversiontimestamp) FROM datasource_value_nodeversion "
			+ "WHERE (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? AND id = ? GROUP BY id)";

	/**
	 * SQL Params for the selection of versioned datasource_value data
	 */
	protected final static VersionedSQLParam[] SELECT_VERSIONED_DATASOURCE_VALUE_PARAMS = {
		VersionedSQLParam.ID, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.ID};

	/**
	 * SQL Statement for batchloading datasources
	 */
	protected final static String BATCHLOAD_DATASOURCE_SQL = createBatchLoadStatement("datasource");

	/**
	 * SQL Statement for batchloading datasource_values
	 */
	protected final static String BATCHLOAD_DATASOURCE_VALUE_SQL = createBatchLoadStatement("datasource_value");

	/**
	 * SQL Statement to insert a new datasource
	 */
	protected final static String INSERT_DATASOURCE_SQL = "INSERT INTO datasource (source_type, name, param_id, uuid) VALUES (?, ?, ?, ?)";

	/**
	 * SQL Statement to update a datasource
	 */
	protected final static String UPDATE_DATASOURCE_SQL = "UPDATE datasource SET source_type = ?, name = ?, param_id = ? WHERE id = ?";

	/**
	 * SQL Statement to insert a new entry
	 */
	protected final static String INSERT_ENTRY_SQL = "INSERT INTO datasource_value (datasource_id, sorder, dskey, value, dsid, uuid) VALUES (?, ?, ?, ?, ?, ?)";

	/**
	 * SQL Statement to update an entry TODO: do we update the dsid?
	 */
	protected final static String UPDATE_ENTRY_SQL = "UPDATE datasource_value SET sorder = ?, dskey = ?, value = ? WHERE id = ?";

	/**
	 * Save the datasource
	 * @param ds datasource
	 * @throws NodeException
	 */
	private static void saveDatasourceObject(EditableFactoryDatasource ds) throws NodeException {
		boolean isNew = Datasource.isEmptyId(ds.getId());

		if (isNew) {
			assertNewGlobalId(ds);

			// insert a new record
			// source_type, name, param_id
			List<Integer> keys = DBUtils.executeInsert(INSERT_DATASOURCE_SQL, new Object[] {
				ds.sourceType.getVal(), ds.name, ds.paramId, ObjectTransformer.getString(ds.getGlobalId(), "") });

			if (keys.size() == 1) {
				// set the new ds id
				ds.setId(keys.get(0));
				synchronizeGlobalId(ds);
			} else {
				throw new NodeException("Error while inserting new datasource, could not get the insertion id");
			}
		} else {
			DBUtils.executeUpdate(UPDATE_DATASOURCE_SQL, new Object[] {
				ds.sourceType.getVal(), ds.name, ds.paramId, ds.getId()
			});
		}
	}

	/**
	 * Save the datasource entry
	 * @param entry datasource entry
	 * @throws NodeException
	 */
	private static void saveEntryObject(EditableFactoryDatasourceEntry entry) throws NodeException {
		boolean isNew = DatasourceEntry.isEmptyId(entry.getId());

		if (isNew) {
			assertNewGlobalId(entry);

			// insert a new record
			List<Integer> keys = DBUtils.executeInsert(INSERT_ENTRY_SQL, new Object[] {
				entry.datasourceId, entry.sortOrder, entry.key, entry.value, entry.dsid, ObjectTransformer.getString(entry.getGlobalId(), "")
			});

			if (keys.size() == 1) {
				// set the new entry id
				entry.setId(keys.get(0));
				synchronizeGlobalId(entry);
			} else {
				throw new NodeException("Error while inserting new datasource entry, could not get the insertion id");
			}
		} else {
			DBUtils.executeUpdate(UPDATE_ENTRY_SQL, new Object[] {
				entry.sortOrder, entry.key, entry.value, entry.getId()
			});
		}
	}

	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.DATASOURCE, Datasource.TYPE_DATASOURCE, true, FactoryDatasource.class);
			registerFactoryClass(C.Tables.DATASOURCE_ENTRY, DatasourceEntry.TYPE_DATASOURCEENTRY, true, FactoryDatasourceEntry.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	public DatasourceEntryFactory() {
		super();
	}

	@Override
	public boolean isVersioningSupported(Class<? extends NodeObject> clazz) {
		// ds and ds_obj support versioning
		if (DatasourceEntry.class.equals(clazz)) {
			return true;
		} else if (Datasource.class.equals(clazz)) {
			return true;
		} else {
			return false;
		}
	}

	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info,
			FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		if (DatasourceEntry.class.equals(clazz)) {
			int datasourceId = rs.getInt("datasource_id");
			int sortOrder = rs.getInt("sorder");
			String key = rs.getString("dskey");
			String value = rs.getString("value");
			int dsid = rs.getInt("dsid");

			// to satisfy IntegrationDatasource/SiteminderDatasource needs set dsid to -1 if it's NULL to be able to fallback to ds.value later on
			if (rs.wasNull()) {
				dsid = -1;

			}

			return (T) new FactoryDatasourceEntry(id, info, datasourceId, sortOrder, key, value, dsid, getUdate(rs), getGlobalId(rs));
		} else if (Datasource.class.equals(clazz)) {
			String name = rs.getString("name");
			Integer paramId = rs.getInt("param_id");
			int sourceType = rs.getInt("source_type");

			return (T) new FactoryDatasource(id, info, name, paramId, sourceType, getUdate(rs), getGlobalId(rs));
		} else {
			return null;
		}
	}

	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		final String idSql = buildIdSql(ids);

		if (Datasource.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_DATASOURCE_SQL + idSql);
		} else if (DatasourceEntry.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_DATASOURCE_VALUE_SQL + idSql + " ORDER BY sorder ASC");
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#createObject(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) {
		if (Datasource.class.equals(clazz)) {
			return (T) new EditableFactoryDatasource(handle.createObjectInfo(Datasource.class, true));
		} else if (DatasourceEntry.class.equals(clazz)) {
			return (T) new EditableFactoryDatasourceEntry(handle.createObjectInfo(DatasourceEntry.class, true));
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#loadObject(java.lang.Class, java.lang.Object, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		if (Datasource.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_DATASOURCE_SQL, SELECT_VERSIONED_DATASOURCE_SQL, SELECT_VERSIONED_DATASOURCE_PARAMS);
		} else if (DatasourceEntry.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_DATASOURCE_VALUE_SQL, SELECT_VERSIONED_DATASOURCE_VALUE_SQL, SELECT_VERSIONED_DATASOURCE_VALUE_PARAMS);
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

		if (object instanceof FactoryDatasource) {
			return (T) new EditableFactoryDatasource((FactoryDatasource) object, info, false);
		} else if (object instanceof FactoryDatasourceEntry) {
			return (T) new EditableFactoryDatasourceEntry((FactoryDatasourceEntry) object, info, false);
		} else {
			throw new NodeException("DatasourceFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Delete the given entry
	 * @param entry entry to delete
	 * @throws NodeException
	 */
	protected void deleteEntry(FactoryDatasourceEntry entry) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection deleteList = getDeleteList(t, DatasourceEntry.class);

		deleteList.add(entry);
	}

	/**
	 * Delete the given datasource
	 * @param ds datasource to delete
	 * @throws NodeException
	 */
	protected void deleteDatasource(FactoryDatasource ds) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection deleteList = getDeleteList(t, Datasource.class);

		deleteList.add(ds);
	}

	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, DatasourceEntry.class)) {
			flushDelete("DELETE FROM datasource_value WHERE id IN", DatasourceEntry.class);
		}
		if (!isEmptyDeleteList(t, Datasource.class)) {
			Collection<Datasource> deletedDS = getDeleteList(t, Datasource.class);

			for (Datasource ds : deletedDS) {
				// logcmd
				ActionLogger.logCmd(ActionLogger.DEL, Datasource.TYPE_SINGLE_DATASOURCE, ds.getId(), 0, "Datasource.delete");
				// event
				Events.trigger(ds, null, Events.DELETE);
			}

			// delete the datasources
			flushDelete("DELETE FROM datasource WHERE id IN", Datasource.class);

			// delete the permissions
			flushDelete("DELETE FROM perm WHERE o_type = " + Datasource.TYPE_SINGLE_DATASOURCE + " AND o_id IN", Datasource.class);
			// refresh the PermissionStore
			for (Datasource ds : deletedDS) {
				t.addTransactional(new RemovePermsTransactional(Datasource.TYPE_SINGLE_DATASOURCE, ObjectTransformer.getInt(ds.getId(), 0)));
			}
		}
	}

	/**
	 * Factory class for non-editable Datasources
	 */
	private static class FactoryDatasource extends Datasource {

		/**
		 * Serial version uid
		 */
		private static final long serialVersionUID = 7635105494071063133L;

		/**
		 * Ids of entries contained in this static datasource. The ids are store as Set (in no particular order)
		 */
		protected Set<Integer> entryIds = null;

		protected String name;
		protected Integer paramId;
		protected SourceType sourceType;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		protected FactoryDatasource(NodeObjectInfo info) {
			super(null, info);
		}

		public FactoryDatasource(Integer id, NodeObjectInfo info, String name, Integer paramId, int sourceType,
				int udate, GlobalId globalId) {
			super(id, info);
			this.name = name;
			this.paramId = paramId;
			this.sourceType = SourceType.getForVal(sourceType);
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

		public List<DatasourceEntry> getEntries() throws NodeException {
			List<DatasourceEntry> entries = loadEntries();
			int i = 1;

			for (DatasourceEntry dse : entries) {
				((FactoryDatasourceEntry) dse).selectionNr = i++;
			}
			return entries;
		}

		/**
		 * Return a sorted list of the given datasource entries (sorted by sortorder)
		 * @param entries list of entries (will not be modified)
		 * @return sorted list
		 * @throws NodeException
		 */
		protected List<DatasourceEntry> sort(List<DatasourceEntry> entries) throws NodeException {
			if (ObjectTransformer.isEmpty(entries)) {
				return entries;
			}
			List<DatasourceEntry> sortedEntries = new ArrayList<DatasourceEntry>(entries);
			Collections.sort(sortedEntries, new Comparator<DatasourceEntry>() {
				public int compare(DatasourceEntry o1, DatasourceEntry o2) {
					return o1.getSortOrder() - o2.getSortOrder();
				}
			});
			return sortedEntries;
		}

		/**
		 * Load the entries
		 * @return sorted list of entries
		 * @throws NodeException
		 */
		protected List<DatasourceEntry> loadEntries() throws NodeException {
			loadEntryIds();
			Transaction t = TransactionManager.getCurrentTransaction();

			NodeObjectInfo info = getObjectInfo();
			if (info.isEditable()) {
				return sort(t.getObjects(DatasourceEntry.class, entryIds, true));
			} else if (!info.isCurrentVersion()) {
				return sort(t.getObjects(DatasourceEntry.class, entryIds, info.getVersionTimestamp()));
			} else {
				return sort(t.getObjects(DatasourceEntry.class, entryIds));
			}
		}

		/**
		 * Load the entry IDs (if not done before)
		 * @throws NodeException
		 */
		private synchronized void loadEntryIds() throws NodeException {
			// load ids if not yet loaded
			if (entryIds == null) {
				if (isEmptyId(getId())) {
					entryIds = new HashSet<>();
				} else if (info.isCurrentVersion()) {
					entryIds = DBUtils.select("SELECT id FROM datasource_value WHERE datasource_id = ?", ps -> ps.setInt(1, getId()), DBUtils.IDS);
				} else {
					int versionTimestamp = info.getVersionTimestamp();
					entryIds = DBUtils.select("SELECT id FROM datasource_value_nodeversion WHERE datasource_id = ? AND (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ?", ps -> {
						ps.setInt(1, getId());
						ps.setInt(2, versionTimestamp);
						ps.setInt(3, versionTimestamp);
					}, DBUtils.IDS);
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
		 */
		public void dirtCache() throws NodeException {
			loadEntryIds();
			Transaction t = TransactionManager.getCurrentTransaction();

			for (Iterator<Integer> iter = entryIds.iterator(); iter.hasNext();) {
				Integer entryId = iter.next();

				t.dirtObjectCache(DatasourceEntry.class, entryId, false);
			}
		}

		/**
		 * Get the name
		 * @return name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Get the param_id (whatever it is)
		 * @return param_id
		 */
		public Integer getParamId() {
			return paramId;
		}

		/**
		 * Get the source type
		 * @return source type
		 */
		public SourceType getSourceType() {
			return sourceType;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Datasource {" + getId() + "}";
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryDatasource(this, getFactory().getFactoryHandle(Datasource.class).createObjectInfo(Datasource.class, true), true);
		}

		@Override
		public List<Part> getParts() throws NodeException {
			final List<Integer> partIds = new Vector<Integer>();

			DBUtils.executeStatement("SELECT id FROM part WHERE type_id IN (?, ?, ?) AND info_int = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					// these are the datasource part types
					stmt.setInt(1, 29);
					stmt.setInt(2, 30);
					stmt.setInt(3, 32);
					// set the datasource id
					stmt.setObject(4, getId());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException,
							NodeException {
					while (rs.next()) {
						partIds.add(rs.getInt("id"));
					}
				}
			});
			return TransactionManager.getCurrentTransaction().getObjects(Part.class, partIds);
		}

		/**
		 * Performs the delete of the datasource
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			DatasourceEntryFactory entryFactory = (DatasourceEntryFactory) t.getObjectFactory(Datasource.class);

			entryFactory.deleteDatasource(this);
		}
	}

	/**
	 * Factory class for editable Datasources
	 */
	private static class EditableFactoryDatasource extends FactoryDatasource {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -5248413213548864859L;

		/**
		 * Flag to mark modified objects
		 */
		protected boolean modified = false;

		/**
		 * Editable list of entries
		 */
		protected List<DatasourceEntry> entries = null;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected EditableFactoryDatasource(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param ds object
		 * @param info info
		 * @param asNewObject true for new objects, false for editable existing objects
		 * @throws NodeException
		 */
		protected EditableFactoryDatasource(FactoryDatasource ds, NodeObjectInfo info,
				boolean asNewObject) throws NodeException {
			super(asNewObject ? null : ds.getId(), info, ds.name, ds.paramId, ds.sourceType.getVal(), asNewObject ? -1 : ds.getUdate(),
					asNewObject ? null : ds.getGlobalId());
			if (asNewObject) {
				// copy the datasource entries
				List<DatasourceEntry> originalEntries = ds.getEntries();
				entries = new ArrayList<DatasourceEntry>(originalEntries.size());
				for (DatasourceEntry entry : originalEntries) {
					entries.add((DatasourceEntry)entry.copy());
				}

				modified = true;
			}
		}

		@Override
		public void setName(String name) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.name, name)) {
				this.name = name;
				this.modified = true;
			}
		}

		@Override
		public void setSourceType(SourceType sourceType) throws ReadOnlyException {
			assertEditable();
			if (this.sourceType != sourceType) {
				this.sourceType = sourceType;
				this.modified = true;
			}
		}

		@Override
		public void setParamId(Integer paramId) throws ReadOnlyException {
			assertEditable();
			if (ObjectTransformer.getInt(this.paramId, 0) != ObjectTransformer.getInt(paramId, 0)) {
				this.paramId = paramId;
				this.modified = true;
			}
		}

		@Override
		public List<DatasourceEntry> getEntries() throws NodeException {
			if (entries == null) {
				entries = new Vector<DatasourceEntry>(super.getEntries());
			}
			return entries;
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException,
					NodeException {
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();

			// make the name unique
			if (name != null) {
				setName(UniquifyHelper.makeUnique(name, Datasource.MAX_NAME_LENGTH, "SELECT name FROM datasource WHERE id != ? AND name = ?",
						SeparatorType.blank, ObjectTransformer.getInt(getId(), -1)));
			}

			boolean isModified = this.modified;
			boolean isNew = isEmptyId(getId());

			// save the datasource, if necessary
			if (isModified) {
				saveDatasourceObject(this);
				this.modified = false;
			}

			// save all the entries and check which no longer exist (and need to be removed)
			List<DatasourceEntry> entries = getEntries();
			List<Integer> entryIdsToRemove = new Vector<Integer>();

			if (entryIds != null) {
				entryIdsToRemove.addAll(entryIds);
			}
			int sortOrderCounter = 1;

			boolean changedEntries = false;
			for (DatasourceEntry entry : entries) {
				// set datasource id and sortorder
				entry.setDatasourceId(ObjectTransformer.getInt(getId(), 0));
				entry.setSortOrder(sortOrderCounter++);
				boolean entrySaved = entry.save();
				isModified |= entrySaved;
				changedEntries |= entrySaved;

				// do not remove the entry, which was saved
				entryIdsToRemove.remove(entry.getId());
			}

			// remove entries which no longer exist
			if (!entryIdsToRemove.isEmpty()) {
				List<DatasourceEntry> entriesToRemove = t.getObjects(DatasourceEntry.class, entryIdsToRemove);

				for (DatasourceEntry entry : entriesToRemove) {
					entry.delete();
					changedEntries = true;
				}
				isModified = true;
			}

			// logcmd and event
			if (isModified) {
				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, Datasource.TYPE_SINGLE_DATASOURCE, getId(), 0, "Datasource.create");
					t.addTransactional(new TransactionalTriggerEvent(Datasource.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, Datasource.TYPE_SINGLE_DATASOURCE, getId(), 0, "Datasource.update");
					// TODO get modified attributes (this requires annotating the data fields with @DataField)
					t.addTransactional(new TransactionalTriggerEvent(Datasource.class, getId(), null, Events.UPDATE));

					// if entries were changed, we dirt all constructs using this datasource.
					// this is not perfect, since we might dirt way too many objects with that, but it is equal to the current
					// dirting behaviour when changing a datasource over the UI and clicking "Synchronize"
					if (changedEntries) {
						List<Part> parts = getParts();
						Set<Integer> constructIds = new HashSet<Integer>();
						for (Part part : parts) {
							constructIds.add(ObjectTransformer.getInteger(part.getConstructId(), 0));
				}

						for (Integer constructId : constructIds) {
							t.addTransactional(new TransactionalTriggerEvent(Construct.class, constructId, null, Events.UPDATE | Events.EVENT_CN_CONTENT));
			}
					}
				}
			}

			// set the initial permissions for the new datasource, if the feature to have individual datasource permissions is on
			if (isNew && t.getNodeConfig().getDefaultPreferences().isFeature(Feature.DATASOURCE_PERM)) {
				final List<Integer> treeIds = new ArrayList<Integer>(1);
				DBUtils.executeStatement("SELECT id FROM tree WHERE type_id = ?", new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, Datasource.TYPE_DATASOURCE);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						if (rs.next()) {
							treeIds.add(rs.getInt("id"));
						}
					}
				});
				if (treeIds.isEmpty()) {
					logger.warn("Could not set initial permissions on " + this + ", because the tree entry for type " + Datasource.TYPE_DATASOURCE + " was not found");
				} else {
					PermHandler.duplicatePermissions(Datasource.TYPE_DATASOURCE, treeIds.get(0), Datasource.TYPE_SINGLE_DATASOURCE, ObjectTransformer.getInt(getId(), -1));
				}
			}

			if (isModified) {
				t.dirtObjectCache(Datasource.class, getId());
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

			Datasource originalDatasource = (Datasource)original;

			// copy the meta data
			setName(originalDatasource.getName());
			setParamId(originalDatasource.getParamId());
			setSourceType(originalDatasource.getSourceType());

			// copy the datasource entries
			List<DatasourceEntry> origEntries = originalDatasource.getEntries();
			List<DatasourceEntry> thisEntries = getEntries();

			// copy the original entries over this entries
			for (int i = 0; i < origEntries.size(); ++i) {
				DatasourceEntry entry = origEntries.get(i);

				if (thisEntries.size() <= i) {
					// this datasource has less entries, so add a copy
					thisEntries.add((DatasourceEntry) entry.copy());
				} else {
					// this datasource has at least so many entries, so copy the i-th overview entry
					thisEntries.get(i).copyFrom(entry);
				}
			}

			// remove any surplus entries
			while (thisEntries.size() > origEntries.size()) {
				thisEntries.remove(thisEntries.size() - 1);
			}
		}
	}

	/**
	 * Factory Implementation for datasource entries
	 */
	private static class FactoryDatasourceEntry extends DatasourceEntry {

		private static final long serialVersionUID = -6033782753590849682L;

		@DataField("datasource_id")
		protected int datasourceId;

		@DataField("sorder")
		@Updateable
		protected int sortOrder;

		@DataField("dskey")
		@Updateable
		protected String key;

		@DataField("value")
		@Updateable
		protected String value;

		@DataField("dsid")
		protected int dsid;

		/**
		 * selectionNr is set for copies of DatasourceEntry that represent selected DatasourceEntries
		 */
		protected int selectionNr = -1;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		protected FactoryDatasourceEntry(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Copy constructor for selection copies
		 * @param entry original entry
		 * @param selectionNr selection nr
		 */
		protected FactoryDatasourceEntry(FactoryDatasourceEntry entry, int selectionNr) {
			this(entry.getId(), entry.getObjectInfo(), entry.datasourceId, entry.sortOrder, entry.key, entry.value, entry.dsid, entry.getUdate(),
					entry.getGlobalId());
			this.selectionNr = selectionNr;
		}

		protected FactoryDatasourceEntry(Integer id, NodeObjectInfo info, int datasourceId, int sortOrder,
				String key, String value, int dsid, int udate, GlobalId globalId) {
			super(id, info);

			this.datasourceId = datasourceId;
			this.sortOrder = sortOrder;
			this.key = key;
			this.value = value;
			this.dsid = dsid;
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

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.DatasourceEntry#getSelectionCopy(int)
		 */
		public DatasourceEntry getSelectionCopy(int selectionNr) {
			return new FactoryDatasourceEntry(this, selectionNr);
		}

		public int getDatasourceId() {
			return datasourceId;
		}

		@Override
		public Datasource getDatasource() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(Datasource.class, datasourceId);
		}

		public int getSortOrder() {
			return sortOrder;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}

		public int getDsid() {
			return dsid;
		}

		public String[] getStackKeywords() {
			return new String[] { "value"};
		}

		public Resolvable getKeywordResolvable(String keyword) throws NodeException {
			return this;
		}

		public Resolvable getShortcutResolvable() throws NodeException {
			return this;
		}

		public String getStackHashKey() {
			return "dsentry:{" + getHashKey() + "}";
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "DatasourceEntry {" + getId() + "}";
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.DatasourceEntry#getNr()
		 */
		public int getNr() {
			return selectionNr;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryDatasourceEntry(this, getFactory().getFactoryHandle(DatasourceEntry.class).createObjectInfo(DatasourceEntry.class, true),
					true);
		}

		/**
		 * Performs the delete of the entry
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			DatasourceEntryFactory entryFactory = (DatasourceEntryFactory) t.getObjectFactory(DatasourceEntry.class);

			entryFactory.deleteEntry(this);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);
			DatasourceEntry oEntry = (DatasourceEntry) original;

			setDsid(oEntry.getDsid());
			setKey(oEntry.getKey());
			setSortOrder(oEntry.getSortOrder());
			setValue(oEntry.getValue());
		}
	}

	/**
	 * Factory Implementation for editable datasource entries
	 */
	private static class EditableFactoryDatasourceEntry extends FactoryDatasourceEntry {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -8443884650045832116L;

		/**
		 * Flag to mark modified objects
		 */
		protected boolean modified = false;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected EditableFactoryDatasourceEntry(NodeObjectInfo info) {
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
		protected EditableFactoryDatasourceEntry(FactoryDatasourceEntry entry, NodeObjectInfo info,
				boolean asNewObject) throws NodeException {
			super(asNewObject ? null : entry.getId(), info, entry.datasourceId, entry.sortOrder, entry.key, entry.value, entry.dsid,
					asNewObject ? -1 : entry.getUdate(), asNewObject ? null : entry.getGlobalId());
			if (asNewObject) {
				modified = true;
				this.datasourceId = 0;
			}
		}

		@Override
		public void setDatasourceId(int datasourceId) throws ReadOnlyException,
					NodeException {
			assertEditable();
			if (this.datasourceId != datasourceId) {
				// only set the datasource id, if not done before
				if (this.datasourceId <= 0) {
					this.datasourceId = datasourceId;
					this.modified = true;
				} else {
					throw new NodeException("Error while setting datasource id. Datasource id is already set!");
				}
			}
		}

		@Override
		public void setDsid(int dsId) throws ReadOnlyException {
			assertEditable();
			if (this.dsid != dsId) {
				this.dsid = dsId;
				this.modified = true;
			}
		}

		@Override
		public void setKey(String key) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.key, key)) {
				this.key = key;
				this.modified = true;
			}
		}

		@Override
		public void setValue(String value) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.value, value)) {
				this.value = value;
				this.modified = true;
			}
		}

		@Override
		public void setSortOrder(int sortOrder) throws ReadOnlyException {
			assertEditable();
			if (this.sortOrder != sortOrder) {
				this.sortOrder = sortOrder;
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException,
					NodeException {
			assertEditable();
			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = false;
			boolean isNew = isEmptyId(getId());
			DatasourceEntry origEntry = null;

			if (!isNew) {
				origEntry = t.getObject(DatasourceEntry.class, getId());
			}

			if (modified) {
				// object is modified, so update it
				isModified = true;
				saveEntryObject(this);
				modified = false;
			}

			// events
			if (isModified) {
				if (isNew) {
					t.addTransactional(new TransactionalTriggerEvent(DatasourceEntry.class, getId(), null, Events.CREATE));
				} else {
					t.addTransactional(new TransactionalTriggerEvent(DatasourceEntry.class, getId(), getModifiedData(origEntry, this), Events.UPDATE));
					t.dirtObjectCache(DatasourceEntry.class, getId());
				}
			}

			return isModified;
		}
	}
}
