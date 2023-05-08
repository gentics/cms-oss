package com.gentics.lib.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 19.11.2003
 */
public class SimpleOracleConnector extends DefaultConnectionManager {
	private String server, database, username, passwd;

	private int port;

	private void init(String server, String database, String username, String passwd, int port) {
		this.server = server;
		this.database = database;
		this.username = username;
		this.passwd = passwd;
		this.port = port;
	}

	public SimpleOracleConnector(String server, String database, String username,
			String passwd, int port) {
		init(server, database, username, passwd, port);
	}

	public SimpleOracleConnector(String server, String database, String username,
			String passwd, int port, int maxConnections) {
		super(maxConnections);
		init(server, database, username, passwd, port);
	}

	public Connection connect() throws SQLException {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			String url = "jdbc:oracle:thin:@" + server + ":" + port + ":" + database;

			return DriverManager.getConnection(url, username, passwd);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
}
