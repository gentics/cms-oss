package com.gentics.lib.db;

import java.sql.SQLException;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 19.11.2003
 */
public interface Connector {

	/**
	 * ensures that the returned connection is valid, otherwise throws
	 * SQLException the caller must not close this connection directly, but
	 * instead call releaseConnection()
	 * @return a valid JDBC-Connection or null if some other error occured
	 *         (NamingException, ClassNotFoundException, ...)
	 * @throws SQLException
	 */
	PoolConnection getConnection() throws SQLException;

	/**
	 * must not be called without prior call to getConnection(), but exactly one
	 * time for every call to getConnection(), that did not return null
	 * @throws SQLException
	 */
	void releaseConnection(PoolConnection c) throws SQLException;

	/**
	 * shuts down all existing pool-connections
	 * @throws SQLException
	 */
	void close() throws SQLException;
}
