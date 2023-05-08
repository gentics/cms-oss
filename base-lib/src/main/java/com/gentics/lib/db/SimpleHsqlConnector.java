/*
 * @author norbert
 * @date 17.11.2005
 * @version $Id: SimpleHsqlConnector.java,v 1.3 2006-01-26 09:27:13 herbert Exp $
 */
package com.gentics.lib.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.gentics.lib.log.NodeLogger;

/**
 * Connector for hsql databases
 * @author norbert
 */
public class SimpleHsqlConnector extends DefaultConnectionManager {

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
	public SimpleHsqlConnector(String database, String username, String passwd,
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
			Class.forName("org.hsqldb.jdbcDriver");
			String url = "jdbc:hsqldb:" + database;

			return DriverManager.getConnection(url, username, passwd);
		} catch (ClassNotFoundException e) {
			NodeLogger.getLogger(getClass()).fatal("Could not load Driver for Hsql DB!", e);
			return null;
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see com.gentics.lib.db.Connector#close()
	 */
	public synchronized void close() throws SQLException {
		// execute a database shutdown
		try {
			Connection con = connect();
			Statement st = con.createStatement();

			st.execute("SHUTDOWN");
		} finally {
			super.close();
		}
	}
}
