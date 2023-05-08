/*
 * @author norbert
 * @date 04.05.2007
 * @version $Id: NonPoolingConnector.java,v 1.2 2010-09-28 17:01:28 norbert Exp $
 */
package com.gentics.lib.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Implementation of a connector that does not use connection pooling
 */
public class NonPoolingConnector implements Connector {

	/**
	 * database connection url
	 */
	protected String url;

	/**
	 * password
	 */
	protected String passwd;

	/**
	 * username
	 */
	protected String username;

	/**
	 * Create an instance of the non pooling connector
	 * @param url database connection url
	 * @param username username
	 * @param passwd password
	 */
	public NonPoolingConnector(String url, String username, String passwd) {
		this.url = url;
		this.username = username;
		this.passwd = passwd;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.Connector#getConnection()
	 */
	public PoolConnection getConnection() throws SQLException {
		Connection conn = DriverManager.getConnection(url, username, passwd);

		return new PoolConnection(-1, conn);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.Connector#releaseConnection(com.gentics.lib.db.PoolConnection)
	 */
	public void releaseConnection(PoolConnection c) throws SQLException {
		c.getConnection().close();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.Connector#close()
	 */
	public void close() throws SQLException {}
}
