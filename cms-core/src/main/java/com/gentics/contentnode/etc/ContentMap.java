/*
 * @author norbert
 * @date 02.03.2007
 * @version $Id: ContentMap.java,v 1.17.6.1 2011-03-16 10:45:19 norbert Exp $
 */
package com.gentics.contentnode.etc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.contentnode.publish.CnMapPublishHandler2;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.TagmapEntryWrapper;
import com.gentics.contentnode.publish.cr.DummyTagmapEntry;
import com.gentics.contentnode.publish.cr.PermissionTagmapEntry;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.etc.ContentMapStatistics;
import com.gentics.lib.etc.ContentMapStatistics.Item;
import com.gentics.lib.log.NodeLogger;

/**
 * Class representing a configured contentmap
 */
public class ContentMap {

	/**
	 * Names of the folder permission attributes
	 */
	public final static String[] FOLDER_PERMISSION_ATTRIBUTES = {
		"permgroups_view", "permgroups_edit", "permgroups_pagecreate", "permgroups_foldercreate",
		"permgroups_folderdelete"};

	/**
	 * Permission bits to be checked for the above mentioned permission
	 * attributes
	 */
	public final static int[] FOLDER_PERMISSION_BITS = {
		PermHandler.PERM_VIEW, PermHandler.PERM_FOLDER_UPDATE, PermHandler.PERM_PAGE_CREATE,
		PermHandler.PERM_FOLDER_CREATE, PermHandler.PERM_FOLDER_DELETE};

	/**
	 * Names of the page permission attributes
	 */
	public final static String[] PAGE_PERMISSION_ATTRIBUTES = { "permgroups_view", "permgroups_edit", "permgroups_publish", "permgroups_delete"};

	/**
	 * Permission bits to be checked for the above mentioned permission
	 * attributes
	 */
	public final static int[] PAGE_PERMISSION_BITS = {
		PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE, PermHandler.PERM_PAGE_PUBLISH,
		PermHandler.PERM_PAGE_DELETE};

	/**
	 * Names of the file permission attributes
	 */
	public final static String[] FILE_PERMISSION_ATTRIBUTES = { "permgroups_view", "permgroups_edit", "permgroups_publish", "permgroups_delete"};

	/**
	 * Permission bits to be checked for the above mentioned permission
	 * attributes
	 */
	public final static int[] FILE_PERMISSION_BITS = {
		PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE, PermHandler.PERM_PAGE_PUBLISH,
		PermHandler.PERM_PAGE_DELETE};

	/**
	 * Semaphore map for synchronization of instant publishing transactions to the CRs
	 */
	private final static SemaphoreMap<Integer> semaphoreMap = new SemaphoreMap<>("cr_trx");

	/**
	 * Flag to mark whether a transaction has been started for this contentmap 
	 */
	private static Map<Integer, ThreadLocal<Boolean>> transactionStarted = new HashMap<Integer, ThreadLocal<Boolean>>();

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ContentMap.class);

	/**
	 * Id of the contentmap (empty for static contentmap)
	 */
	protected Integer id;

	/**
	 * Name of the contentmap
	 */
	protected String name;

	/**
	 * datasource (non multichannelling)
	 */
	private CNWriteableDatasource datasource;

	/**
	 * datasource (multichannelling)
	 */
	private WritableMCCRDatasource mccrDatasource;

	/**
	 * whether files (binarycontent) shall be published into this contentmap
	 */
	private boolean publishFiles;

	/**
	 * the sql handle which is also used by the datasource.
	 */
	private SQLHandle handle;

	/**
	 * use transaction publishing into contentmap.
	 */
	private boolean useTransaction;

	/**
	 * marks if contents have been updated used to check if contentstatus
	 * lastupdate shall be written
	 */
	private boolean changed = false;

	/**
	 * list of publish handlers to be used for this ContentMap instance
	 */
	private List<CnMapPublishHandler> publishHandlers = new ArrayList<CnMapPublishHandler>();

	/**
	 * list of opened publish handlers
	 */
	private List<CnMapPublishHandler> openedPublishHandlers = new ArrayList<CnMapPublishHandler>();

	/**
	 * marks if instant publishing is turned on for this contentmap
	 */
	private boolean instantPublishing = false;

	/**
	 * marks whether language information attributes shall automatically be
	 * added to this contentrepository
	 */
	private boolean addLanguageInformation = false;

	/**
	 * marks whether permission information attributes shall automatically be
	 * added to this contentrepository
	 */
	private boolean addPermissionInformation = false;

	/**
	 * marks whether the multichannelling datasource shall do the sync of deleted objects
	 * differentially or fully
	 */
	private boolean differentialDelete = true;

	/**
	 * map of tagmap entries for types
	 */
	private Map<Integer, List<TagmapEntryRenderer>> tagmapEntries = new HashMap<>();

	/**
	 * list of nodes which publish into this contentmap
	 */
	private List<Node> nodes = new Vector<Node>();

	/**
	 * timestamp, when the contentmap was last changed (in ms)
	 */
	private long lastChange = 0;

	/**
	 * Keepalive interval (in ms). Default is 1 hour
	 */
	private int keepaliveInterval = 60 * 60 * 1000;

	/**
	 * Statistics (if statistics started)
	 */
	private ContentMapStatistics statistics;

	/**
	 * ContentRepository
	 */
	private ContentRepository cr;

	/**
	 * Create an instance of the contentmap
	 * @param cr ContentRepository
	 * @param id id of the contentmap
	 * @param name name of the contentmap
	 * @param datasource datasource instance (non multichannelling)
	 * @param mccrDatasource datasource instance (multichannelling)
	 * @param publishFiles whether files shall be published into the contentmap
	 * @param handle handle
	 * @param transaction
	 * @param instantPublishing true when instant publishing is turned on for this contentmap, false if not
	 * @param addLanguageInformation true when language information attributes shall automatically be added, false if not
	 * @param addPermissionInformation true when permission information attributes shall automatically be added, false if not
	 * @param differentialDelete true if deleted objects shall be synchronized differentially
	 * @param keepaliveInterval interval for keepalive statements. If 0, the default value (1 hour) will be used
	 */
	public ContentMap(ContentRepository cr, Integer id, String name, CNWriteableDatasource datasource, WritableMCCRDatasource mccrDatasource, boolean publishFiles, SQLHandle handle,
			boolean transaction, boolean instantPublishing, boolean addLanguageInformation, boolean addPermissionInformation, boolean differentialDelete,
			int keepaliveInterval) {
		this.cr = cr;
		this.id = id;
		this.name = name;
		if (keepaliveInterval > 0) {
			this.keepaliveInterval = keepaliveInterval;
		}
		// create a semaphore for this contentmap
		semaphoreMap.init(id);
		this.datasource = datasource;
		this.mccrDatasource = mccrDatasource;
		// we don't want to repair the id counters on every insert (will do this
		// later, before committing)
		if (this.datasource != null) {
			this.datasource.setRepairIDCounterOnInsert(false);
		}
		this.publishFiles = publishFiles;
		this.handle = handle;
		this.useTransaction = transaction;
		this.instantPublishing = instantPublishing;
		this.addLanguageInformation = addLanguageInformation;
		this.addPermissionInformation = addPermissionInformation;
		this.differentialDelete = differentialDelete;
	}

	/**
	 * Get the non-multichannelling datasource
	 * @return Returns the datasource instance or null if multichannelling
	 */
	public CNWriteableDatasource getDatasource() {
		return datasource;
	}

	/**
	 * Get the MCCR datasource instance
	 * @return MCCR datasource or null if not multichanneling
	 */
	public WritableMCCRDatasource getMCCRDatasource() {
		return mccrDatasource;
	}

	/**
	 * Get the dbhandle for the datasource
	 * @return
	 * @throws CMSUnavailableException 
	 */
	public DBHandle getHandle() throws DatasourceException, CMSUnavailableException {
		return datasource != null ? datasource.getHandle().getDBHandle() : mccrDatasource.getHandle();
	}

	/**
	 * Get the writable datasource
	 * @return writable datasource instance (never null)
	 */
	public WriteableDatasource getWritableDatasource() {
		return datasource != null ? datasource : mccrDatasource;
	}

	/**
	 * Check whether the datasource instance is multichannelling aware
	 * @return true if multichannelling aware
	 */
	public boolean isMultichannelling() {
		return mccrDatasource != null;
	}

	/**
	 * set changed to...
	 * @param changed
	 */
	public void setChanged(boolean changed) {
		this.changed = changed;
		this.lastChange = System.currentTimeMillis();
	}
    
	public boolean isChanged() {
		return changed;
	}

	/**
	 * Get the id of the contentmap
	 * @return id of the contentmap
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Get the name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the publishFiles.
	 */
	public boolean isPublishFiles() {
		return publishFiles;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer("ContentMap ").append(id);

		if (handle != null) {
			DBHandle dbHandle = handle.getDBHandle();

			if (dbHandle != null) {
				buffer.append(", ").append(dbHandle.toString());
			}
		}
		return buffer.toString();
	}

	public void startTransaction() throws SQLException, TransactionException, CnMapPublishException {
		// aquire the semaphore
		semaphoreMap.acquire(getId());

		boolean started = false;
		boolean dbTransactionStarted = false;

		try {
			// start a new DB transaction. If we find an open DB connection, we will reuse it
			long startTime = System.currentTimeMillis();

			DB.startTransaction(handle.getDBHandle(), true);
			dbTransactionStarted = true;
			if (logger.isDebugEnabled()) {
				logger.debug("Started transaction for contentrepository " + id + " in " + (System.currentTimeMillis() - startTime) + " ms");
			}
			startTime = System.currentTimeMillis();
			Transaction t = TransactionManager.getCurrentTransaction();

			for (CnMapPublishHandler handler : publishHandlers) {
				handler.open(t.getTimestamp());
				openedPublishHandlers.add(handler);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Started " + openedPublishHandlers.size() + " publish handlers in " + (System.currentTimeMillis() - startTime) + " ms");
			}
			// the transaction has been started
			started = true;
			// this thread just started a transaction
			getTransactionFlag(getId()).set(true);
		} finally {
			// if the transaction was not started, a failure occurred (an exception was thrown)
			// so release the semaphore now
			if (!started) {
				if (dbTransactionStarted) {
					DB.rollbackTransaction(handle.getDBHandle(), true);
				}
				semaphoreMap.release(getId());
			}
		}
	}

	/**
	 * Commit the transaction, which was opened for the contentmap
	 * @param close true to also close the DB connection, false to leave it open
	 * @throws SQLException
	 * @throws CMSUnavailableException 
	 */
	public void commit(boolean close) throws SQLException, CMSUnavailableException {
		// check whether this thread started a transaction
		ThreadLocal<Boolean> transactionFlag = getTransactionFlag(getId());
		boolean transactionStarted = ObjectTransformer.getBoolean(transactionFlag.get(), false);

		// when something was changed in the contentrepository and a transaction was started, repair the id_counter
		if (datasource != null && transactionStarted && changed) {
			datasource.repairIdCounters(null);
		}
		long startTime = System.currentTimeMillis();

		DB.commitTransaction(handle.getDBHandle(), close);
		if (logger.isDebugEnabled()) {
			if (close) {
				logger.debug("Committed transaction for (and closed connection to) cr " + id + " in " + (System.currentTimeMillis() - startTime) + " ms");
			} else {
				logger.debug("Committed transaction for cr " + id + " in " + (System.currentTimeMillis() - startTime) + " ms");
			}
		}
		// only do publish handlers and release the semaphore, if a transaction was started before (which acquired the semaphore)
		if (transactionStarted) {
			try {
				startTime = System.currentTimeMillis();
				for (CnMapPublishHandler handler : openedPublishHandlers) {
					handler.commit();
					handler.close();
				}
				if (logger.isDebugEnabled()) {
					logger.debug(
							"Committed and closed " + openedPublishHandlers.size() + " publish handlers for cr " + id + " in "
							+ (System.currentTimeMillis() - startTime) + " ms");
				}
			} finally {
				openedPublishHandlers.clear();
			}
			// release the semaphore
			transactionFlag.set(false);
			semaphoreMap.release(getId());
		}
	}

	/**
	 * Rollback the transaction, which was opened for the contentmap
	 * @param close true to also close the DB connection, false to leave it open
	 * @throws SQLException
	 */
	public void rollback(boolean close) throws SQLException {
		ThreadLocal<Boolean> transactionFlag = getTransactionFlag(getId());
		boolean transactionStarted = ObjectTransformer.getBoolean(transactionFlag.get(), false);

		try {

			long startTime = System.currentTimeMillis();

			DB.rollbackTransaction(handle.getDBHandle(), close);
			if (logger.isDebugEnabled()) {
				logger.debug("Rolled back transaction for cr " + id + " in " + (System.currentTimeMillis() - startTime) + " ms");
			}
			// only do publish handlers if a transaction was started
			if (transactionStarted) {
				try {
					startTime = System.currentTimeMillis();
					for (CnMapPublishHandler handler : openedPublishHandlers) {
						handler.rollback();
						handler.close();
					}
					if (logger.isDebugEnabled()) {
						logger.debug(
								"Rolled back and closed " + openedPublishHandlers.size() + " publish handlers for cr " + id + " in "
								+ (System.currentTimeMillis() - startTime) + " ms");
					}
				} finally {
					openedPublishHandlers.clear();
				}
			}
		} finally {
			// only release the semaphore, when a transaction was started before
			if (transactionStarted) {
				// release the semaphore
				transactionFlag.set(false);
				semaphoreMap.release(getId());
			}
		}
	}

	public Connection getConnection() {
		return DB.getTransactionConnection(handle.getDBHandle());
	}

	/**
	 * Set the lastupdate timestamp to the given value
	 * @param timestamp timestamp of last update
	 * @param publishedNodes collection of nodes, which were actually written into the contentmap
	 * @param updateCR true if also the contentrepository object in the backend shall be changed
	 * @throws SQLException
	 */
	public void setLastMapUpdate(int timestamp, Collection<Node> publishedNodes, boolean updateCR) throws SQLException, NodeException {
		if (!changed) {
			// if entries have not changed there is no need to update the
			// timestamp
			return;
		}
		if (isMultichannelling()) {
			for (Node node : nodes) {
				if (!publishedNodes.contains(node)) {
					continue;
				}
				DB.update(handle.getDBHandle(), "UPDATE " + handle.getDBHandle().getChannelName() + " SET updatetimestamp = ? WHERE id = ?",
						new Object[] { timestamp, node.getId() });
			}
		} else {
			SimpleResultProcessor result = new SimpleResultProcessor();

			DB.query(handle.getDBHandle(), "SELECT intvalue FROM " + handle.getDBHandle().getContentStatusName() + " WHERE name = ?",
					new Object[] { "lastupdate"}, result);
			if (result.size() > 0) {
				DB.update(handle.getDBHandle(), "UPDATE " + handle.getDBHandle().getContentStatusName() + " SET intvalue = ? WHERE name = ?",
						new Object[] { new Integer(timestamp), "lastupdate"});
			} else {
				DB.update(handle.getDBHandle(), "INSERT INTO " + handle.getDBHandle().getContentStatusName() + " (name, intvalue) VALUES (?, ?)",
						new Object[] { "lastupdate", new Integer(timestamp)});
			}
		}

		// also set the timestamp in the table contentrepository
		if (updateCR) {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;

			try {
				pst = t.prepareStatement("UPDATE contentrepository SET statusdate = ? WHERE id = ?", Transaction.UPDATE_STATEMENT);
				pst.setInt(1, timestamp);
				pst.setInt(2, getId());
        		
				pst.executeUpdate();
			} finally {
				t.closeStatement(pst);
			}
		}
	}

	/**
	 * Get the timestamp when the given node was published into the contentmap the last time
	 * @param node node
	 * @return timestamp of the last update or -1 if not found
	 * @throws NodeException
	 */
	public int getLastMapUpdate(Node node) throws NodeException {
		try {
			if (isMultichannelling()) {
				DBHandle handle = mccrDatasource.getHandle();
				SimpleResultProcessor result = new SimpleResultProcessor();

				if (node == null) {
					DB.query(handle, "SELECT min(updatetimestamp) updatetimestamp FROM channel", null, result);
				} else {
					DB.query(handle, "SELECT updatetimestamp FROM channel WHERE id = ?", new Object[] { node.getId()}, result);
				}
				if (result.size() > 0) {
					return result.iterator().next().getInt("updatetimestamp");
				} else {
					return -1;
				}
			} else {
				DBHandle handle = datasource.getHandle().getDBHandle();
				SimpleResultProcessor result = new SimpleResultProcessor();

				DB.query(handle, "SELECT intvalue FROM " + handle.getContentStatusName() + " WHERE name = ?", new Object[] { "lastupdate"}, result);
				if (result.size() > 0) {
					return result.iterator().next().getInt("intvalue");
				} else {
					return -1;
				}
			}
		} catch (SQLException e) {
			throw new NodeException("Error while reading last update timestamp", e);
		}
	}

	/**
	 * Add the given publish handler
	 * @param handler publish handler
	 */
	public void addPublishHandler(CnMapPublishHandler handler) {
		publishHandlers.add(handler);
	}

	/**
	 * Handle creation of an object in the contentmap, calls {@link CnMapPublishHandler#createObject(Resolvable)} for all configured publish handlers
	 * @param object created object
	 * @throws CnMapPublishException when any of the handler throws that exception
	 */
	public void handleCreateObject(Resolvable object) throws CnMapPublishException {
		for (CnMapPublishHandler handler : openedPublishHandlers) {
			if (statistics != null) {
				statistics.get(Item.PUBLISH_HANDLER).start();
			}
			try {
				handler.createObject(object);
			} finally {
				if (statistics != null) {
					statistics.get(Item.PUBLISH_HANDLER).stop();
				}
			}
		}
	}

	/**
	 * Handle updating of an object in the contentmap, calls {@link CnMapPublishHandler#updateObject(Resolvable)} for all configured publish handlers
	 * @param object updated object
	 * @param attributes optional set of changed attributes
	 * @throws CnMapPublishException when any of the handler throws that exception
	 */
	public void handleUpdateObject(Resolvable object, Set<String> attributes) throws CnMapPublishException {
		for (CnMapPublishHandler handler : openedPublishHandlers) {
			if (statistics != null) {
				statistics.get(Item.PUBLISH_HANDLER).start();
			}
			try {
				if (handler instanceof CnMapPublishHandler2) {
					((CnMapPublishHandler2) handler).updateObject(object, getOriginal(object), attributes);
				} else {
					handler.updateObject(object);
				}
			} finally {
				if (statistics != null) {
					statistics.get(Item.PUBLISH_HANDLER).stop();
				}
			}
		}
	}

	/**
	 * Handle deletion of an object in the contentmap, calls {@link CnMapPublishHandler#deleteObject(Resolvable)} for all configured publish handlers
	 * @param object delete object
	 * @throws CnMapPublishException when any of the handler throws that exception
	 */
	public void handleDeleteObject(Resolvable object) throws CnMapPublishException {
		for (CnMapPublishHandler handler : openedPublishHandlers) {
			if (statistics != null) {
				statistics.get(Item.PUBLISH_HANDLER).start();
			}
			try {
				handler.deleteObject(object);
			} finally {
				if (statistics != null) {
					statistics.get(Item.PUBLISH_HANDLER).stop();
				}
			}
		}
	}

	/**
	 * Enable/disable capturing statistics
	 * @param enable true to enable, false to disable
	 */
	public void enableStatistics(boolean enable) {
		if (enable && statistics == null) {
			statistics = new ContentMapStatistics();
		} else if (!enable) {
			statistics = null;
		}
	}

	/**
	 * Get the statistics (if enabled). When disabled, null will be returned
	 * @return statistics (if enabled) or null
	 */
	public ContentMapStatistics getStatistics() {
		return statistics;
	}

	/**
	 * This method needs to be called before writing an object of given type into the CR
	 * @param type object type
	 */
	public void startWrite(int type) {
		if (statistics != null) {
			switch (type) {
			case Folder.TYPE_FOLDER:
				statistics.get(Item.WRITE_FOLDER).start();
				break;
			case File.TYPE_FILE:
			case ImageFile.TYPE_IMAGE:
				statistics.get(Item.WRITE_FILE).start();
				break;
			case Page.TYPE_PAGE:
				statistics.get(Item.WRITE_PAGE).start();
				break;
			}
		}
	}

	/**
	 * This method needs to be called after writing an object of given type into the CR
	 * @param type object type
	 */
	public void endWrite(int type) {
		endWrite(type, 1);
	}

	/**
	 * This method needs to be called after writing objects of given type into the CR
	 * @param type object type
	 * @param objects number of objects
	 */
	public void endWrite(int type, int objects) {
		if (statistics != null) {
			switch (type) {
			case Folder.TYPE_FOLDER:
				statistics.get(Item.WRITE_FOLDER).stop(objects);
				break;
			case File.TYPE_FILE:
			case ImageFile.TYPE_IMAGE:
				statistics.get(Item.WRITE_FILE).stop(objects);
				break;
			case Page.TYPE_PAGE:
				statistics.get(Item.WRITE_PAGE).stop(objects);
				break;
			}
		}
	}

	/**
	 * Get the handle properties from the resultset (current row)
	 * @param res resultset which should point to a record holding
	 *        contentrepository data
	 * @param config configuration instance
	 * @return the handle properties
	 * @throws SQLException
	 */
	public static Map<String, String> getHandleParameters(ResultSet res, NodeConfig config) throws SQLException, NodeException {
		Map<String, String> handleProperties = new HashMap<String, String>();
		String dbType = res.getString("dbtype");
		String url = res.getString("url");

		// determine the driverclass
		Map driverClasses = config.getDefaultPreferences().getPropertyMap("contentrepository_driverclass");
		Map dummyQueries = config.getDefaultPreferences().getPropertyMap("contentrepository_dummyquery");
		String driverClass = ObjectTransformer.getString(driverClasses.get(dbType), null);
		String dummyquery = ObjectTransformer.getString(dummyQueries.get(dbType), null);
		if (ObjectTransformer.isEmpty(driverClass)) {
			throw new NodeException(
					"Error while getting handle parameters for ContentRepository {" + res.getString("name") + "}: could not find driverClass for dbtype {" + dbType
					+ "} in configuration.");
		}

		handleProperties.put("url", url);
		handleProperties.put("driverClass", driverClass);
		handleProperties.put("username", res.getString("username"));
		handleProperties.put("passwd", res.getString("password"));
		handleProperties.put("type", "jdbc");
		handleProperties.put(SQLHandle.PARAM_NAME, res.getString("name"));
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

	/**
	 * Check whether instant publishing is turned on for this contentmap
	 * @return true when instant publishing is turned on, false if not
	 */
	public boolean isInstantPublishing() {
		return instantPublishing;
	}

	/**
	 * Check whether language information attributes shall automatically be added to this contentrepository
	 * @return true for automatic language information, false if not
	 */
	public boolean addLanguageInformation() {
		return addLanguageInformation;
	}

	/**
	 * Check whether permission information attributes shall automatically be
	 * added to this contentrepository
	 * @return true for automatic permission information, false if not
	 */
	public boolean addPermissionInformation() {
		return addPermissionInformation;
	}

	/**
	 * Check whether the sync of deleted objects shall be done differentially
	 * @return true for differential sync, false for full sync
	 */
	public boolean isDifferentialDelete() {
		return differentialDelete;
	}

	/**
	 * Get the tagmap entries to write into this contentmap for the given type
	 * as {@link List} of {@link TagmapEntry} instances.
	 * This method assumes that there is a transaction already running.
	 * @param type object type
	 * @return list of tagmap entries
	 * @throws NodeException
	 */
	public List<TagmapEntryRenderer> getTagmapEntries(int type) throws NodeException {
		Integer tagmapEntriesKey = new Integer(type);

		if (tagmapEntries.containsKey(tagmapEntriesKey)) {
			return tagmapEntries.get(tagmapEntriesKey);
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		ContentRepository cr = t.getObject(ContentRepository.class, id);
		if (cr == null) {
			// TODO
		}

		// collect the attribute names here (we will need them
		// when language information must be added)
		List<String> attributeNames = new ArrayList<>();
		List<TagmapEntryRenderer> tagmapEntriesList = new ArrayList<>();

		cr.getAllEntries().stream().filter(entry -> entry.getObject() == type).forEach(entry -> {
			tagmapEntriesList.add(new TagmapEntryWrapper(entry));
			attributeNames.add(entry.getMapname());
		});

		// add language information attributes if configured and the type is PAGE
		if (type == Page.TYPE_PAGE && addLanguageInformation) {
			if (!attributeNames.contains("content_languages")) {
				tagmapEntriesList.add(new DummyTagmapEntry(Page.TYPE_PAGE, "node.languages.code", "content_languages", GenticsContentAttribute.ATTR_TYPE_TEXT, 0));
			}

			if (!attributeNames.contains("content_language")) {
				tagmapEntriesList.add(new DummyTagmapEntry(Page.TYPE_PAGE, "page.language.code", "content_language", GenticsContentAttribute.ATTR_TYPE_TEXT, 0));
			}

			if (!attributeNames.contains("contentset_id")) {
				tagmapEntriesList.add(new DummyTagmapEntry(Page.TYPE_PAGE, "page.contentset_id", "contentset_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, 0));
			}

			List<ContentLanguage> languages = getLanguages();

			for (ContentLanguage language : languages) {
				String attributeName = "contentid_" + language.getCode();

				if (!attributeNames.contains(attributeName)) {
					tagmapEntriesList.add(
							new DummyTagmapEntry(Page.TYPE_PAGE, "page.languageset.pages." + language.getCode() + ".id", attributeName,
							GenticsContentAttribute.ATTR_TYPE_OBJ, Page.TYPE_PAGE));
				}
			}
		}

		// add permission information attributes, if required
		if (addPermissionInformation) {
			// for folders
			if (type == Folder.TYPE_FOLDER) {
				addPermissionAttributes(type, FOLDER_PERMISSION_ATTRIBUTES, FOLDER_PERMISSION_BITS, attributeNames, tagmapEntriesList);
			} else if (type == Page.TYPE_PAGE) {
				addPermissionAttributes(type, PAGE_PERMISSION_ATTRIBUTES, PAGE_PERMISSION_BITS, attributeNames, tagmapEntriesList);
			} else if (type == ContentFile.TYPE_FILE) {
				addPermissionAttributes(type, FILE_PERMISSION_ATTRIBUTES, FILE_PERMISSION_BITS, attributeNames, tagmapEntriesList);
			}
		}

		tagmapEntries.put(tagmapEntriesKey, tagmapEntriesList);

		return tagmapEntriesList;
	}

	/**
	 * Add the permission attributes to the list of tagmap entries
	 * @param type type of the published object
	 * @param permissionAttributes string array of the permission attributes to add
	 * @param permissionBits int array of the permission bits to check
	 * @param attributeNames list of already found attribute names
	 * @param tagmapEntriesList list of tagmap entries (will be modified)
	 */
	@SuppressWarnings("unchecked")
	protected void addPermissionAttributes(int type, String[] permissionAttributes,
			int[] permissionBits, List attributeNames, List tagmapEntriesList) {
		// loop through all folder permission attributes
		for (int i = 0; i < permissionAttributes.length; i++) {
			String attributeName = permissionAttributes[i];

			// only add the permission attribute, if no other attribute with
			// that name exists
			if (!attributeNames.contains(attributeName)) {
				tagmapEntriesList.add(
						new PermissionTagmapEntry("folder.id", attributeName, GenticsContentAttribute.ATTR_TYPE_INTEGER, type, permissionBits[i]));
			}
		}
	}

	/**
	 * Add the given node to the list of nodes which publish into this contentmap
	 * @param node node which publishes into this contentmap
	 */
	public void addNode(Node node) {
		if (!nodes.contains(node)) {
			nodes.add(node);
		}
	}

	/**
	 * Get the list of nodes publishing into this contentmap
	 * @return list of nodes publishing into this contentmap
	 */
	public List<Node> getNodes() {
		return nodes;
	}

	/**
	 * Get all languages which are published into this contentmap
	 * @return list of {@link ContentLanguage} objects
	 * @throws NodeException
	 */
	public List<ContentLanguage> getLanguages() throws NodeException {
		List<ContentLanguage> languages = new Vector<ContentLanguage>();

		for (Node node : nodes) {
			List<ContentLanguage> nodeLanguages = node.getLanguages();

			for (ContentLanguage language : nodeLanguages) {
				if (!languages.contains(language)) {
					languages.add(language);
				}
			}
		}
		return languages;
	}

	/**
	 * Check whether the given object shall be published into this contentrepository
	 * @param object object to check
	 * @return true if the object shall be published into this contentrepository, false if not
	 * @throws NodeException
	 */
	public boolean isPublishedIntoCR(NodeObject object) throws NodeException {
		return cr.mustContain(object);
	}

	/**
	 * Check whether the given folder shall be published into this contentrepository
	 * @param folder folder to check
	 * @return true if the folder shall be published into this contentrepository, false if not
	 * @throws NodeException
	 */
	public boolean isPublishedIntoCR(Folder folder) throws NodeException {
		return cr.mustContain(folder);
	}

	/**
	 * Check whether the given folder shall be published into this contentrepository for the given node
	 * @param folder folder to check
	 * @param node node to check
	 * @return true if the folder shall be published into this contentrepository for the given node, false if not
	 * @throws NodeException
	 */
	public boolean isPublishedIntoCR(Folder folder, Node node) throws NodeException {
		return cr.mustContain(folder, node);
	}

	/**
	 * Check whether the given page shall be published into this contentrepository
	 * @param page page to check
	 * @return true if the page shall be published into this contentrepository, false if not
	 * @throws NodeException
	 */
	public boolean isPublishedIntoCR(Page page) throws NodeException {
		return cr.mustContain(page);
	}

	/**
	 * Check whether the given page shall be published into this contentrepository for the given node
	 * @param page page to check
	 * @param node node to check
	 * @return true if the page shall be published into this contentrepository for the node, false if not
	 * @throws NodeException
	 */
	public boolean isPublishedIntoCR(Page page, Node node) throws NodeException {
		return cr.mustContain(page, node);
	}

	/**
	 * Check whether the given file shall be published into this contentrepository, false if not
	 * @param file file to check
	 * @return true if the file shall be published into this contentrepository, false if not
	 * @throws NodeException
	 */
	public boolean isPublishedIntoCR(File file) throws NodeException {
		return cr.mustContain(file);
	}

	/**
	 * Check whether the given file shall be published into this contentrepository in a given node, false if not
	 * @param file file to check
	 * @param node the node where the file should be published
	 * @return true if the file shall be published into this contentrepository, false if not
	 * @throws NodeException
	 */
	public boolean isPublishedIntoCR(File file, Node node) throws NodeException {
		if (file instanceof ContentFile) {
			return cr.mustContain((ContentFile)file, node);
		} else {
			return false;
		}
	}

	/**
	 * Keep the DB connection alive.
	 * If the last access to the ContentMap was longer than 
	 * @param worker contentmap worker (may be null)
	 */
	public void keepAlive(AsynchronousWorker worker) {
		long now = System.currentTimeMillis();

		if (now - lastChange > keepaliveInterval) {
			if (logger.isDebugEnabled()) {
				logger.debug("Last access to " + this + " was @" + lastChange + ", keeping connection alive now.");
			}
			keepConnectionAlive(worker);
			lastChange = now;
		}
	}

	/**
	 * Enable/disable the cache for the datasource.
	 * Disabling the cache will also clear all cache regions
	 * @param enabled true to enable, false to disable
	 */
	public void setCache(boolean enabled) {
		if (datasource != null) {
			datasource.setCache(enabled);
		}
		if (mccrDatasource != null) {
			mccrDatasource.setCache(enabled);
		}
	}

	/**
	 * Get the ContentRepository
	 * @return ContentRepository
	 */
	public ContentRepository getContentRepository() {
		return cr;
	}

	/**
	 * Do something, that keeps the connection alive
	 * @param worker contentmap worker (may be null)
	 */
	protected void keepConnectionAlive(AsynchronousWorker worker) {
		if (worker != null) {
			try {
				worker.addAsynchronousJob(new AsynchronousJob() {

					/* (non-Javadoc)
					 * @see com.gentics.lib.etc.AsynchronousJob#process()
					 */
					public int process(RenderResult renderResult) throws Exception {
						keepConnectionAlive(null);
						return 1;
					}

					/* (non-Javadoc)
					 * @see com.gentics.lib.etc.AsynchronousJob#getDescription()
					 */
					public String getDescription() {
						return "Keep connection alive";
					}

					/* (non-Javadoc)
					 * @see com.gentics.lib.etc.AsynchronousJob#isLogged()
					 */
					public boolean isLogged() {
						return false;
					}
				});
			} catch (NodeException e) {
				logger.warn("Could not keep connection alive.", e);
			}
		} else {
			try {
				String dummyStatement = handle.getDBHandle().getDummyStatement();

				if (dummyStatement != null) {
					DB.query(handle.getDBHandle(), dummyStatement, new SimpleResultProcessor());
				} else {
					logger.warn("Could not keep connection alive, because no dummy statement was found");
				}
			} catch (SQLException e) {
				logger.warn("Error while keeping connection alive", e);
			}
		}
	}

	/**
	 * Get the threadlocal transaction flag for this contentmap
	 * @return threadlocal transaction flag
	 */
	private static synchronized ThreadLocal<Boolean> getTransactionFlag(Integer id) {
		if (!transactionStarted.containsKey(id)) {
			transactionStarted.put(id, new ThreadLocal<Boolean>());
		}

		return transactionStarted.get(id);
	}

	/**
	 * Start a new transaction, if the contentmap uses instant publishing.
	 * Do nothing, if the contentmap does not use instant publishing
	 * @return an instance of {@link ContentMapTrx} if a transaction was started, null otherwise
	 * @throws NodeException
	 */
	public ContentMapTrx startInstantPublishingTrx() throws NodeException {
		return startInstantPublishingTrx(null);
	}

	/**
	 * Start a new transaction, if the contentmap uses instant publishing.
	 * Do nothing, if the contentmap does not use instant publishing.
	 * It is mandatory to call {@link ContentMapTrx#setSuccess()} on the returned instance (if not null) so that
	 * the transaction will be committed and not rolled back.
	 * @param commitHandler optional commit handler
	 * @return an instance of {@link ContentMapTrx} if a transaction was started, null otherwise
	 * @throws NodeException
	 */
	public ContentMapTrx startInstantPublishingTrx(ContentMapTrxHandler commitHandler) throws NodeException {
		if (isInstantPublishing()) {
			return new ContentMapTrx(commitHandler);
		} else {
			return null;
		}
	}

	/**
	 * Get the original object for the given object that is published
	 * @param object published object
	 * @return original object
	 * @throws DatasourceException
	 */
	protected Resolvable getOriginal(Resolvable object) throws CnMapPublishException {
		Resolvable original = null;
		try {
			if (isMultichannelling() && object instanceof MCCRObject) {
				// clone the object without attributes, because we want to access the original attributes
				original = getMCCRDatasource().getClone((MCCRObject)object, false);
			}
			return original;
		} catch (Exception e) {
			throw new CnMapPublishException(e);
		}
	}

	/**
	 * ContentMap Transaction implementation, that implements {@link AutoCloseable} for automatic management of rollback/commit.
	 * If the method {@link #setSuccess()} of this instance is not called before the resource is closed, the underlying transaction
	 * will be rolled back. Only if {@link #setSuccess()} is called, the transaction will be committed (but never closed).
	 */
	public class ContentMapTrx implements AutoCloseable {
		/**
		 * Internal success state
		 */
		protected boolean success = false;

		/**
		 * handler instance for commit
		 */
		protected ContentMapTrxHandler commitHandler;

		/**
		 * Start a new transaction
		 * @param handler optional handler for commit
		 * @throws NodeException
		 */
		protected ContentMapTrx(ContentMapTrxHandler handler) throws NodeException {
			try {
				startTransaction();
				this.commitHandler = handler;
			} catch (Exception e) {
				throw new NodeException("Error while starting transaction for {" + ContentMap.this + "}", e);
			}
		}

		/**
		 * Set the success state of the transaction to true.
		 */
		public void setSuccess() {
			this.success = true;
		}

		@Override
		public void close() throws NodeException {
			if (success) {
				try {
					commit(true);
					if (commitHandler != null) {
						commitHandler.handle();
					}
				} catch (Exception ce) {
					// rollback the contentmap
					try {
						rollback(true);
					} catch (Exception re) {
						logger.error("Error while rolling back transaction for {" + ContentMap.this + "}", re);
					}

					throw new NodeException("Error while comitting transaction for {" + ContentMap.this + "}", ce);
				}
			} else {
				try {
					rollback(true);
				} catch (Exception e1) {
					logger.error("Error while rolling back transaction for {" + ContentMap.this + "}", e1);
				}
			}
		}
	}
}
