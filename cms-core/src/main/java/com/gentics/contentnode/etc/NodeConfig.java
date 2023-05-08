package com.gentics.contentnode.etc;

import java.sql.Connection;
import java.util.Collection;

import org.quartz.Scheduler;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.datasource.SQLHandle;

/**
 * Main configuration interface for a configuration container.
 * The NodeConfig can create connections and preferences.
 * @see NodePreferences
 */
public interface NodeConfig {
	public final static String CNMAP_HANDLE_PREFIX = "gtx_cnmap_handle";

	/**
	 * Initialize connections, background threads, etc.
	 * @throws NodeException
	 */
	void init() throws NodeException;

	/**
	 * Closes all open connections and frees other resources
	 * @throws NodeException 
	 */
	void close() throws NodeException;

	/**
	 * Gets a Scheduler which uses a persistant Jobstore (JDBCJobstore)
	 */
	Scheduler getPersistentScheduler() throws NodeException;

	/**
	 * Get the backend database connection. The connection is retrieved from a pool, so
	 * you should return it as soon as possible.
	 *
	 * @return a new connection, or null if it cannot be established.
	 * @throws NodeException 
	 */
	default Connection getConnection() throws NodeException {
		return getConnection(true);
	}

	/**
	 * Get the backend databse connection. The connection is either fetched from a
	 * connection pool, or created new (depending on the flag useConnectionPool)
	 * @param useConnectionPool true when the connection shall be fetched from
	 *        the pool, false if created new
	 * @return a new connection or null if it cannot be established
	 * @throws NodeException 
	 */
	Connection getConnection(boolean useConnectionPool) throws NodeException;

	/**
	 * return the connection to the pool. This is a shortcut to connection.close() with
	 * exception handling. If the connection is null or already closed, nothing is done.
	 *
	 * @param connection the connection to return.
	 */
	void returnConnection(Connection connection);

	/**
	 * Get the SQLHandle for the backend connection
	 * @param useConnectionPool flag for using connection pooling
	 * @return SQLHandle
	 */
	SQLHandle getSQLHandle(boolean useConnectionPool);

	/**
	 * Get the preferences for all users.
	 * @return The preferences for all users.
	 */
	NodePreferences getDefaultPreferences();

	/**
	 * This method can be used to overwrite the default Preferences.
	 * Useful for making a difference between actual preferences which
	 * are fetched from a current CN installation and local settings
	 * read in through portlet/servlet configuration.
	 * 
	 * The usual usage would be to get the default preferences with
	 * {@link #getDefaultPreferences()} and create a new NodePreferences
	 * object with your local settings which falls back to the original
	 * default preferences.
	 * 
	 * @param nodePreferences the new default preferences.
	 */
	void overwriteDefaultPreferences(NodePreferences nodePreferences);

	/**
	 * Get the preferences for a specified user. This are user's
	 * preferences merged with the default-preferences.
	 *
	 * @param userId the userid of the user.
	 * @return the preferences for the specified user merged with the defaultpreferences.
	 */
	NodePreferences getUserPreferences(int userId);

	/**
	 * Get a list of all available contentmaps
	 * @return list of contentmaps
	 * @throws NodeException 
	 */
	Collection<ContentMap> getContentMaps() throws NodeException;
}
