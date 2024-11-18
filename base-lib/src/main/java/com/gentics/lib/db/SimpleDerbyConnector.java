/*
 * @author norbert
 * @date 16.11.2005
 * @version $Id: SimpleDerbyConnector.java,v 1.1 2005-11-18 10:05:51 norbert Exp $
 */
package com.gentics.lib.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.gentics.lib.log.NodeLogger;

/**
 * Connector for derby databases
 * @author norbert
 */
public class SimpleDerbyConnector extends DefaultConnectionManager {

	/**
	 * database name
	 */
	protected String database;

	/**
	 * username
	 */
	protected String username;

	/**
	 * password
	 */
	protected String passwd;

	/**
	 * 
	 */
	public SimpleDerbyConnector(String database, String username, String passwd,
			int maxConnections) {
		super(maxConnections);
		this.database = database;
		this.username = username;
		this.passwd = passwd;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.DefaultConnectionManager#connect()
	 */
	protected Connection connect() throws SQLException {
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			// TODO: find better solution for this
			System.setProperty("user.dir", "H:\\cvs\\workspace\\Portal.Node\\WEB-INF\\.node");
			String url = "jdbc:derby:" + database;

			return DriverManager.getConnection(url, username, passwd);
		} catch (ClassNotFoundException e) {
			NodeLogger.getLogger(getClass()).error("Could not load Driver for Derby DB!", e);
			return null;
		}
	}
}
