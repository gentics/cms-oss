/*
 * @author norbert
 * @date 19.12.2006
 * @version $Id: Transaction.java,v 1.22.2.2 2011-02-10 13:43:37 tobiassteiner Exp $
 */
package com.gentics.contentnode.factory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.PreparedStatementHandler;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.i18n.LanguageProvider;

/**
 * Interface for Transaction handles. An instance represents a transaction. The
 * lifetime of a transaction object begins with its creation (the transaction is
 * started) and ends by either calling {@link #commit()} or {@link #rollback()}.
 * The Transaction does not make sure that changes within the Transaction are
 * immediatly visible to the current Transaction. After committing, all changes are
 * available to the current Transaction and all Transactions started after the commit.
 * When the transaction is garbage collected while still open, it is
 * automatically rolled back. Transactions get their ids from a JVM global
 * sequence, which make them unique and ensures that transactions started later
 * have higher ids.
 */
public interface Transaction extends LanguageProvider, PreparedStatementHandler, InstantPublishingResultCollector {

	public static final int SELECT_STATEMENT = 0;

	public static final int INSERT_STATEMENT = 1;

	public static final int UPDATE_STATEMENT = 2;

	public static final int DELETE_STATEMENT = 3;

	/**
	 * Name of the transaction attribute containing objects already published in
	 * the running transaction (instant publishing)
	 */
	String TRX_ATTR_PUBLISHED = "publishedobjects";

	/**
	 * Name of the transaction attribute containing objects already published in
	 * the running transaction (instant publishing) for a specified node
	 */
	String TRX_ATTR_PUBLISHED_PERNODE = "publishedobjectspernode";

	/**
	 * Returns true if the transaction is interrupted.
	 */
	boolean isInterrupted();

	/**
	 * Interrupts the transaction.
	 * This just sets the interrupt flag and processes that are using the transaction should try to stop their work.
	 */
	void interrupt();

	/**
	 * Commit this transaction. Note that the transaction is stopped, even if an exception is thrown. - the same as commit(true)
	 * @throws TransactionException
	 */
	void commit() throws TransactionException;

	/**
	 * Commits this transaction.
	 * @param stopTransaction if the transaction should be stopped after committing.
	 * @see #commit()
	 */
	void commit(boolean stopTransaction) throws TransactionException;

	/**
	 * Rolls back this transaction, all changes made during this transaction
	 * will be unmade. Note that the transaction is stopped, even if an exception is thrown.
	 * @throws TransactionException
	 */
	void rollback() throws TransactionException;

	/**
	 * Rolls back this transaction, all changes made during this transaction
	 * will be unmade. Optionally this will also stop the transaction
	 *
	 * @param stopTransaction
	 *            true if the transaction shall be stopped, false if not
	 * @throws TransactionException
	 */
	void rollback(boolean stopTransaction) throws TransactionException;

	/**
	 * Checks whether the transaction is still open or has been stopped by
	 * calling either {@link #commit()} or {@link #rollback()}.
	 * @return true when this transaction is still open, false if not
	 */
	boolean isOpen();

	/**
	 * Removes a Transactional from the Transaction
	 */
	void removeTransactional(Transactional t);

	/**
	 * Adds a Transactional to the Transaction.
	 */
	void addTransactional(Transactional t);

	/**
	 * Clears the NodeFactory object cache
	 *
	 * @throws NodeException
	 */
	void clearNodeObjectCache() throws NodeException;

	/**
	 * Get the session id that has been used to initialize the transaction.
	 * @return the session id
	 */
	String getSessionId();

	/**
	 * Get the user id associated with this transaction
	 * @return the user id for this session
	 */
	int getUserId();

	/**
	 * Get the unique transaction id
	 * @return unique transaction id
	 */
	long getId();

	/**
	 * Get the PermHandler for the transaction
	 * @return the PermHandler
	 */
	PermHandler getPermHandler();

	/**
	 * Get the PermHandler for the given group
	 * @param groupId group id
	 * @return PermHandler for the group
	 * @throws NodeException
	 */
	PermHandler getGroupPermHandler(int groupId) throws NodeException;

	/**
	 * Refreshes the PermHandler
	 */
	void refreshPermHandler() throws TransactionException;

	/**
	 * Check whether the transaction with the given id is (or was) open parallel to this transaction
	 * @param transId transaction id
	 * @return true if the questioned transaction is (or was) open parallel, false if not
	 */
	boolean isParallelOpen(long transId);

	/**
	 * Get the currently set rendertype
	 * @return rendertype
	 */
	RenderType getRenderType();

	/**
	 * Set a rendertype
	 * @param renderType to set
	 */
	void setRenderType(RenderType renderType);

	/**
	 * Get the object of the given class and with the given id or null if the object does not exist
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @return the object or null
	 * @throws NodeException when the object cannot be fetched due to an internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, String id) throws NodeException;

	/**
	 * Get the object of the given class and with the given id or null if the object does not exist
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @return the object or null
	 * @throws NodeException when the object cannot be fetched due to an internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, Integer id) throws NodeException;

	/**
	 * Like {@link #getObject(NodeObject)} but whereas the other method will return either
	 * the current object or the published object this method will always return the current object.
	 */
	<T extends NodeObject> T getCurrentObject(Class<T> clazz, Integer id) throws NodeException;

	/**
	 * Get a versioned object of the given class and with the given id or null if the object does not exist (at the given version timestamp)
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @param versionTimestamp version timestamp. -1 for the current version
	 * @return the object or null
	 * @throws NodeException when the object cannot be fetched due to an internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, Integer id, int versionTimestamp) throws NodeException;

	/**
	 * Get the object of the given class and with the given id or null if the object does not exist.
	 * If the object should be loaded for update the page will be locked
	 * for the current user and the updatable flag is set on the object.
	 *
	 * If the object can't be locked for the current user it will be returned in read only mode.
	 *
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @param forUpdate indicates whether the object should be loaded for update
	 * @return the object or null
	 * @throws ReadOnlyException when the object is requested for update but is only available for reading
	 * @throws NodeException when the object cannot be fetched due to an internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, Integer id, boolean forUpdate) throws NodeException, ReadOnlyException;

	/**
	 * Get the object of the given class and with the given id or null if the object does not exist.
	 * If the object should be loaded for update the page will be locked
	 * for the current user and the updatable flag is set on the object.
	 *
	 * If the object can't be locked for the current user it will be returned in read only mode.
	 *
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @param forUpdate indicates whether the object should be loaded for update
	 * @return the object or null
	 * @throws ReadOnlyException when the object is requested for update but is only available for reading
	 * @throws NodeException when the object cannot be fetched due to an internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, String id, boolean forUpdate) throws NodeException, ReadOnlyException;

	/**
	 * Get the object of the given class and with the given id or null if the object does not exist.
	 * If the object should be loaded for update the page will be locked
	 * for the current user and the updatable flag is set on the object.
	 *
	 * If the object can't be locked for the current user it will be returned in read only mode.
	 *
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @param forUpdate indicates whether the object should be loaded for update
	 * @param multichannelFallback true when multichannel fallback shall be done, false if not
	 * @return the object or null
	 * @throws ReadOnlyException when the object is requested for update but is only available for reading
	 * @throws NodeException when the object cannot be fetched due to an internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, String id, boolean forUpdate, boolean multichannelFallback) throws NodeException;

	/**
	 * Get the object of the given class and with the given id or null if the object does not exist.
	 * If the object should be loaded for update the page will be locked
	 * for the current user and the updatable flag is set on the object.
	 *
	 * If the object can't be locked for the current user it will be returned in read only mode.
	 *
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @param forUpdate indicates whether the object should be loaded for update
	 * @return the object or null
	 * @throws ReadOnlyException when the object is requested for update but is only available for reading
	 * @throws NodeException when the object cannot be fetched due to an internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, GlobalId id, boolean forUpdate) throws NodeException, ReadOnlyException;

	/**
	 * Get the object of the given class and with the given id or null if the object does not exist.
	 * If the object should be loaded for update the page will be locked
	 * for the current user and the updatable flag is set on the object.
	 *
	 * If the object can't be locked for the current user it will be returned in read only mode.
	 *
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @return the object or null
	 * @throws ReadOnlyException when the object is requested for update but is only available for reading
	 * @throws NodeException when the object cannot be fetched due to an internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, GlobalId id) throws NodeException, ReadOnlyException;

	/**
	 * Get the object of the given class and with the given id or null if the object does not exist.
	 * If the object should be loaded for update the page will be locked
	 * for the current user and the updatable flag is set on the object.
	 *
	 * If the object can't be locked for the current user it will be returned in read only mode.
	 *
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @param forUpdate indicates whether the object should be loaded for update
	 * @param multichannelFallback true when multichannel fallback shall be
	 *        done, false if not
	 * @return the object or null
	 * @throws ReadOnlyException when the object is requested for update but is only available for reading
	 * @throws NodeException when the object cannot be fetched due to an internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, Integer id, boolean forUpdate,
			boolean multichannelFallback, boolean logErrorIfNotFound) throws NodeException, ReadOnlyException;

	/**
	 * Get an object of the given class and with the given id.
	 * @param clazz class of the object to get
	 * @param id id of the object
	 * @param versionTimestamp version timestamp
	 * @param multichannelFallback true when multichannel fallback shall be
	 *        done, false if not
	 * @return the object or null
	 * @throws NodeException when the object cannot be fetched due to an
	 *         internal error
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, Integer id, int versionTimestamp,
			boolean multichannelFallback) throws NodeException;

	/**
	 * Reload the given object
	 * @param object object
	 * @return reloaded object
	 * @throws NodeException
	 */
	<T extends NodeObject> T getObject(T object) throws NodeException;

	/**
	 * Reload the given object
	 * @param object object
	 * @param forUpdate true to get an editable copy of the object
	 * @return reloaded object
	 * @throws NodeException
	 */
	<T extends NodeObject> T getObject(T object, boolean forUpdate) throws NodeException;

	/**
	 * Reload the given object
	 * @param object object
	 * @param forUpdate true to get an editable copy of the object
	 * @param multichannelFallback true for multichannelling fallback
	 * @return reloaded object
	 * @throws NodeException
	 */
	<T extends NodeObject> T getObject(T object, boolean forUpdate, boolean multichannelFallback) throws NodeException;

	/**
	 * Get a bunch of objects from the factory
	 * @param <T>
	 * @param clazz class of the objects
	 * @param ids collection of object ids
	 * @return the list of objects that were found
	 * @throws NodeException when the objects cannot be loaded due to an internal error
	 */
	<T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids) throws NodeException;

	/**
	 * Get a bunch of objects from the factory
	 * @param <T>
	 * @param clazz class of the objects
	 * @param ids collection of object ids
	 * @return the list of objects that were found
	 * @throws NodeException when the objects cannot be loaded due to an internal error
	 */
	<T extends NodeObject> List<T> getObjectsByStringIds(Class<T> clazz, Collection<String> ids) throws NodeException;

	/**
	 * Get a bunch of versioned objects from the factory
	 * @param clazz class of the objects
	 * @param ids collection of object ids
	 * @param versionTimestamp version timestamp. -1 for current version
	 * @return the list of objects that were found
	 * @throws NodeException when the objects could not be fetched due to an internal error
	 */
	<T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids, int versionTimestamp) throws NodeException;

	/**
	 * Get a bunch of objects from the factory
	 * @param clazz class of the objects
	 * @param ids collection of object ids
	 * @param forUpdate true when the objects shall be loaded for update, false if not
	 * @return the list of objects that were found
	 * @throws NodeException when an internal error occurred
	 * @throws ReadOnlyException when the objects were requested for update but at least one is only available for reading
	 */
	<T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids, boolean forUpdate) throws NodeException, ReadOnlyException;

	/**
	 * Get a bunch of objects from the factory
	 * @param clazz class of the objects
	 * @param ids collection of object ids
	 * @param forUpdate true to fetch the objects for update
	 * @param allowMultichannelingFallback true when the objects shall be loaded for update, false if not
	 * @return the list of objects that were found
	 * @throws NodeException when an internal error occurred
	 * @throws ReadOnlyException when the objects were requested for update but at least one is only available for reading
	 */
	<T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids, boolean forUpdate, boolean allowMultichannelingFallback) throws NodeException, ReadOnlyException;

	/**
	 * Get the global ID of the object, or null if the object does not exist, or the id is null or 0
	 * @param <T> type of the object class
	 * @param clazz object class
	 * @param id local object id (may be null or 0)
	 * @return global ID, if the object exists, null otherwise
	 * @throws NodeException
	 */
	default <T extends NodeObject> String getGlobalId(Class<T> clazz, Integer id) throws NodeException {
		T object = getObject(clazz, id);
		if (object == null) {
			return null;
		} else {
			return object.getGlobalId().toString();
		}
	}

	/**
	 * Get the local ID of the object, or null if the object does not exist, or the given id is null
	 * @param <T> type of the object class
	 * @param clazz object class
	 * @param id global object ID (may be null)
	 * @return local ID, if the object exists, null otherwise
	 * @throws NodeException
	 */
	default <T extends NodeObject> Integer getLocalId(Class<T> clazz, String id) throws NodeException {
		return getLocalId(clazz, id, false);
	}

	/**
	 * Get the local ID of the object, or null if the object does not exist, or the given id is null
	 * @param <T> type of the object class
	 * @param clazz object class
	 * @param id global object ID (may be null)
	 * @param throwOnNotFound should I throw a {@link EntityNotFoundException} on inexisting id, or give back null?
	 * @return local ID, if the object exists, null otherwise
	 * @throws NodeException
	 */
	default <T extends NodeObject> Integer getLocalId(Class<T> clazz, String id, boolean throwOnNotFound) throws NodeException {
		if (StringUtils.isBlank(id)) {
			return null;
		}
		T object = getObject(clazz, id);
		if (object == null) {
			if (throwOnNotFound) {
				throw new EntityNotFoundException(I18NHelper.get(String.format("%s.notfound", getTable(clazz)), id));
			} else {
				return null;
			}
		} else {
			return object.getId();
		}
	}

	/**
	 * Get the local ID of the object, or null if the object does not exist, or the given id is null
	 * @param <T> type of the object class
	 * @param clazz object class
	 * @param id global object ID (may be null)
	 * @param onNotFound optional operator, which is called if the object could not be found
	 * @return local ID or null
	 * @throws NodeException
	 */
	default <T extends NodeObject> Integer getLocalId(Class<T> clazz, String id, Operator onNotFound) throws NodeException {
		if (StringUtils.isBlank(id)) {
			return null;
		}
		T object = getObject(clazz, id);
		if (object == null) {
			if (onNotFound != null) {
				onNotFound.operate();
			}
			return null;
		} else {
			return object.getId();
		}
	}

	/**
	 * Create a new object which can later be stored
	 * @param clazz class of the object to create
	 * @return a new object instance, which is not yet saved (and thus has no id)
	 * @throws NodeException
	 */
	<T extends NodeObject> T createObject(Class<T> clazz) throws NodeException;

	/**
	 * Render the given object
	 * @param object object to be rendered
	 * @param result render result
	 * @return rendered object as String
	 * @throws NodeException when the object could not be rendered due to
	 */
	String renderObject(GCNRenderable object, RenderResult result) throws NodeException;

	/**
	 * Check whether the object is cached
	 * @param clazz object class
	 * @param id object id
	 * @return true when the object is cached, false if not
	 * @throws NodeException
	 */
	boolean isCached(Class<? extends NodeObject> clazz, Integer id) throws NodeException;

	/**
	 * Create an object info for an object of the given class
	 * @param clazz object class
	 * @return object info
	 * @throws NodeException
	 */
	NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz) throws NodeException;

	/**
	 * Create an object info for an object of given class
	 * @param clazz object class
	 * @param versionTimestamp version timestamp
	 * @return object info
	 * @throws NodeException
	 */
	NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz, int versionTimestamp) throws NodeException;

	/**
	 * Create an object info for an object of given class
	 * @param clazz object class
	 * @param editable true for editable
	 * @return object info
	 * @throws NodeException
	 */
	NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz, boolean editable) throws NodeException;

	/**
	 * add an object into the cache.
	 *
	 * @param clazz class under which the the object is to be put in the cache.
	 * @param obj the object to put.
	 * @return the object which was previously stored, or null if it was not cached before.
	 * @throws NodeException
	 */
	<T extends NodeObject> T putObject(Class<T> clazz, NodeObject obj) throws NodeException;

	/**
	 * Get the ObjectFactory for a specific type of Object.
	 * @param clazz Class for which the ObjectFactory should be returned
	 * @return The ObjectFactory for the specific class.
	 */
	ObjectFactory getObjectFactory(Class<? extends NodeObject> clazz);

	/**
	 * Get the class for a given ttype.
	 * @param objType the ttype the find the correspondig class to.
	 * @return the mapped class for this ttype, or null if the ttype is unknown.
	 */
	Class<? extends NodeObject> getClass(int objType);

	/**
	 * Get the class for a given tablename
	 * @param tableName name of the table
	 * @return object class or null if not found
	 */
	Class<? extends NodeObject> getClass(String tableName);

	/**
	 * Get the table into which objects of given class are stored
	 * @param clazz class
	 * @return table name
	 */
	String getTable(Class<? extends NodeObject> clazz);

	/**
	 * Try to get the TType of a given objectclass.
	 * @param clazz class of the object.
	 * @return the corresponding ttype, or 0 if no ttype is mapped to this class.
	 */
	int getTType(Class<? extends NodeObject> clazz);

	/**
	 * Dirt the object cache for the given object. When calling this method, the cache will be
	 * dirted immediately, and again on transaction commit
	 * @param clazz object class
	 * @param id object id
	 * @throws NodeException
	 */
	void dirtObjectCache(Class<? extends NodeObject> clazz, Integer id) throws NodeException;

	/**
	 * Dirt the object cache for the given object.
	 * @param clazz object class
	 * @param id object id
	 * @param atCommit true if the cache shall also be dirted at transaction commit, false if not
	 * @throws NodeException
	 */
	void dirtObjectCache(Class<? extends NodeObject> clazz, Integer id, boolean atCommit) throws NodeException;

	/**
	 * Get the start timestamp of this transaction
	 * @return start timestamp
	 */
	long getTimestamp();

	/**
	 * Enable/disable the level2 Cache.
	 * @param enabled true to enable, false to disable
	 * @return true if level2 cache was enabled before, false if not
	 */
	boolean enableLevel2Cache(boolean enabled);

	/**
	 * Clear the "level2" cache for all objects (this should be done every time a page has been rendered)
	 */
	void clearLevel2Cache();

	/**
	 * Get an object from the "level2" cache or null if object not yet cached
	 * @param nodeObject node object
	 * @param key cache key
	 * @return cached object or null
	 */
	Object getFromLevel2Cache(NodeObject nodeObject, Object key);

	/**
	 * Put an object into the "level2" cache
	 * @param nodeObject node object
	 * @param key cache key
	 * @param object object to cache
	 */
	void putIntoLevel2Cache(NodeObject nodeObject, Object key, Object object);

	/**
	 * Sets the start timestamp (php's $TIME equivalent)
	 * @param timestamp
	 */
	void setTimestamp(long timestamp);

	/**
	 * Returns the start timestamp of this transaction the same as
	 * {@link #getTimestamp()}, except that the unix timestamp (seconds instead
	 * of milliseconds) will be returned.
	 * @return start timestamp
	 */
	int getUnixTimestamp();

	/**
	 * returns the current render result which can be used to
	 * communicate errors to the user.
	 * @return the renderresult
	 */
	RenderResult getRenderResult();

	/**
	 * @see #getRenderResult()
	 */
	void setRenderResult(RenderResult renderResult);

	/**
	 * Get a (not prepared) statement. Use this for frequently chaning sql
	 * statements (e.g. when id's or the like are contained in the statement
	 * itself)<br/>
	 * After usage of the statement, it has to be closed with {@link #closeStatement(Statement)}.
	 * @return statement object
	 * @throws SQLException
	 */
	Statement getStatement() throws SQLException;

	/**
	 * Close the statement. This method should be called after usage of a
	 * statement fetched via {@link #getStatement()}.
	 * @param statement statement
	 */
	void closeStatement(Statement statement);

	NodeConfig getNodeConfig();

	/**
	 * Get the db handle for the connection in this transaction
	 * @return db handle
	 */
	DBHandle getDBHandle();

	/**
	 * @return the session associated with this transaction, or null
	 *   if this transaction was created without a session ID.
	 */
	Session getSession();

	/**
	 * Get the transaction attributes. The transaction attributes can be used to
	 * store arbitrary information relevant to the running transaction.
	 * The attributes will be lost, once the transaction is closed
	 * @return modifiable map of attributes
	 */
	Map<String, Object> getAttributes();

	/**
	 * Get the db connection used by this transaction. Note: callers should not
	 * rollback, commit or close this connection directly, but rather use the
	 * methods {@link #commit()}, {@link #commit(boolean)} or
	 * {@link #rollback()} instead.
	 *
	 * Callers must release the connection calling {@link #releaseConnection(Connection)} after usage.
	 *
	 * @return connection
	 */
	Connection getConnection();

	/**
	 * Release the connection, that was fetched using {@link #getConnection()}.
	 * @param c connection
	 */
	void releaseConnection(Connection c);

	/**
	 * Set the "disable multichannelling flag" to the given value. If set to true, multichannelling fallback is disabled, otherwise it is enabled
	 * The previous value is put onto a stack and must be restored via {@link #resetDisableMultichannellingFlag()}.
	 * @param flag true to disable multichannelling fallback, false to enable
	 */
	void setDisableMultichannellingFlag(boolean flag);

	/**
	 * Check whether multichannelling fallback is currently disabled
	 * @return true if the flag is set, false if not
	 */
	boolean isDisableMultichannellingFlag();

	/**
	 * Reset the "disable multichannelling flag" to its previous value
	 */
	void resetDisableMultichannellingFlag();

	/**
	 * Set the "disable versioned publishing" flag to the given value.
	 * If set to true, getting objects that support versioning will not fall back to the published version if in a publish run.
	 * @param flag true to disable versioned publishing
	 */
	void setDisableVersionedPublish(boolean flag);

	/**
	 * Check whether versioned publishing is (temporarily) disabled
	 * @return true if disabled, false if not
	 */
	boolean isDisableVersionedPublish();

	/**
	 * Temporarily enable/disable the publish cache for this transaction.
	 * Note that the publish cache will only be used, if the {@link Feature#PUBLISH_CACHE} is also enabled.
	 * @param flag true to enable, false to disable
	 */
	void setPublishCacheEnabled(boolean flag);

	/**
	 * Check whether the {@link Feature#PUBLISH_CACHE} is enabled and the publish cache has not been temporarily disabled for this transaction via {@link #setPublishCacheEnabled(boolean)}.
	 * @return true if the publish cache is enabled, false if not
	 */
	boolean isPublishCacheEnabled();

	/**
	 * Set the current channel for multichannelling fallback. Previously set
	 * channels will be put onto a stack and must be restored via
	 * {@link #resetChannel()}.
	 * @param channelId channel id
	 */
	void setChannelId(Integer channelId);

	/**
	 * Get the currently set channel ID or null, if no channel is set.
	 * If the channel ID shall be used to get the channel (as instance of {@link Node}), better use the method {@link #getChannel()}.
	 * @return current channel ID or null
	 */
	Integer getChannelId();

	/**
	 * Get the currently set channel or null, if no channel is set.
	 * @return current channel or null
	 */
	Node getChannel();

	/**
	 * Get the ID of the node that is currently being published.
	 *
	 * This <em>should</em> be <code>null</code> outside of
	 * publish runs.
	 *
	 * @return The ID of the node that is currently being published.
	 */
	Integer getPublishedNodeId();

	/**
	 * Set the node that is currently being published.
	 *
	 * @param nodeId The node that is currently being published.
	 */
	void setPublishedNodeId(Integer nodeId);

	/**
	 * When no published node id has been set yet and a node is given, return an instance of {@link PublishedNodeTrx}, which will set the given node.
	 * Otherwise, return null.
	 * @param node published node
	 * @return instance of PublishedNodeTrx or null
	 * @throws NodeException
	 */
	PublishedNodeTrx initPublishedNode(Node node) throws NodeException;

	/**
	 * Reset the current channel to the previous value
	 */
	void resetChannel();

	/**
	 * Check whether the transaction allows viewing the given object
	 * @param object object
	 * @return true if viewing is allowed, false if not
	 * @throws NodeException
	 */
	boolean canView(NodeObject object) throws NodeException;

	/**
	 * Check whether the transaction allows editing the given object. This implies permission to view the object
	 * @param object object
	 * @return true if editing is allowed, false if not
	 * @throws NodeException
	 */
	boolean canEdit(NodeObject object) throws NodeException;

	/**
	 * Check whether the transaction allows deleting the given object. This implies permission to view the object
	 * @param object object
	 * @return true if deleting is allowed, false if not
	 * @throws NodeException
	 */
	boolean canDelete(NodeObject object) throws NodeException;

	/**
	 * Check whether the transaction allows publishing the given object. This implies permission to view the object
	 * @param object object
	 * @return true if publishing is allowed, false if not
	 * @throws NodeException
	 */
	boolean canPublish(NodeObject object) throws NodeException;

	/**
	 * Check whether the transaction allows viewing of objects of the given class in the given folder
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if viewing the objects is allowed, false if not
	 * @throws NodeException
	 */
	boolean canView(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException;

	/**
	 * Check whether the transaction allows creation of objects of the given class in the given folder
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if creating the objects is allowed, false if not
	 * @throws NodeException
	 */
	boolean canCreate(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException;

	/**
	 * Check whether the transaction allows editing of objects of the given class in the given folder
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if editing the objects is allowed, false if not
	 * @throws NodeException
	 */
	boolean canEdit(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException;

	/**
	 * Check whether the transaction allows deleting of objects of the given class in the given folder
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if deleting the objects is allowed, false if not
	 * @throws NodeException
	 */
	boolean canDelete(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException;

	/**
	 * Check whether the transaction allows publishing of objects of the given class in the given folder
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if publishing the objects is allowed, false if not
	 * @throws NodeException
	 */
	boolean canPublish(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException;

	/**
	 * Check whether the transaction allows viewing and restoring objects from the wastebin of the given node
	 * @param n node
	 * @return true if viewing and restoring objects from the wastebin is allowed, false if not
	 * @throws NodeException
	 */
	boolean canWastebin(Node n) throws NodeException;

	/**
	 * Get the field data from the given object
	 * @param object object
	 * @return map of field data
	 * @throws NodeException
	 */
	Map<String, Object> getFieldData(NodeObject object) throws NodeException;

	/**
	 * Set the field data to the given object
	 * @param object object
	 * @param dataMap data map
	 * @throws NodeException
	 */
	void setFieldData(NodeObject object, Map<String, Object> dataMap) throws NodeException;

	/**
	 * Enable/disable instant publishing for this transaction
	 * @param instantPublishing true if instant publishing shall be enabled, false for disabling
	 */
	void setInstantPublishingEnabled(boolean instantPublishing);

	/**
	 * Check whether instant publishing is enabled for this transaction
	 * @return true for enabled, false for disabled
	 */
	boolean isInstantPublishingEnabled();

	/**
	 * Set the (threadlocal) flag to check permissions in ANY channel of the folder (otherwise the permission will be checked in the current channel only)
	 * @param flag flag
	 */
	void setCheckAnyChannel(boolean flag);

	/**
	 * Get the flag for checking the permission in ANY channel of the folder
	 * @return flag value
	 */
	boolean isCheckAnyChannel();

	/**
	 * Check whether publishable pages shall be used
	 * @return true to use publishable page instances
	 */
	boolean usePublishablePages();

	/**
	 * Keep the DB connection of this transaction alive
	 */
	void keepAlive();

	/**
	 * Load the versioned data for the given class of objects and put into the cache.
	 * The version timestamps are defined per main object ID (main objects are specified by the given mainClass)
	 *
	 * Example: When preparing the contenttags by content_id, the mainClazz would by Content.class and the keys of the timestamps map would be the content_id's.
	 * @param clazz class of the objects to prepare
	 * @param mainClazz class of the main objects (id's will be the keys of the timestamps map)
	 * @param timestamps map specifying the main objects, for which the data has to be prepared and the timestamps for each main object
	 * @return the set of prepared versioned objects
	 * @throws NodeException
	 */
	<T extends NodeObject> Set<T> prepareVersionedObjects(Class<T> clazz, Class<? extends NodeObject> mainClazz, Map<Integer, Integer> timestamps) throws NodeException;

	/**
	 * Enable/disable capturing statistics
	 * @param enable true to enable, false to disable
	 */
	void enableStatistics(boolean enable);

	/**
	 * Get the statistics (if enabled). When disabled, null will be returned
	 * @return statistics (if enabled) or null
	 */
	TransactionStatistics getStatistics();

	/**
	 * Prepare an instance of {@link PublishData}, that can be fetched via {@link #getPublishData()}.
	 * After Usage, the instance should be removed via {@link #resetPublishData()}.
	 * @throws NodeException
	 */
	void preparePublishData() throws NodeException;

	/**
	 * Set publish data that were prepared
	 * @param publishData publish data
	 */
	void setPublishData(PublishData publishData);

	/**
	 * Get the instance of {@link PublishData}, that was prepared via {@link #preparePublishData()}.
	 * @return instance of PublishData or null if not prepared
	 */
	PublishData getPublishData();

	/**
	 * Reset the instance of {@link PublishData}, that was prepared via {@link #preparePublishData()}
	 */
	void resetPublishData();

	/**
	 * Set a new {@link Wastebin} filter. Setting to null will set the default filter {@link Wastebin#EXCLUDE}
	 * @param filter new wastebin filter
	 * @return old wastebin filter
	 */
	Wastebin setWastebinFilter(Wastebin filter);

	/**
	 * Get the currently set wastebin filter
	 * @return curret wastebin filter
	 */
	Wastebin getWastebinFilter();
}
