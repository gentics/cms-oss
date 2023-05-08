package com.gentics.lib.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.gentics.lib.log.NodeLogger;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 19.11.2003
 */
public class SimpleMysqlConnector extends DefaultConnectionManager {
	String server, database, username, passwd, type;

	int port;

	boolean useUTF8;

	public SimpleMysqlConnector(String server, String database, String username, String passwd,
			int port) {
		this(server, database, username, passwd, port, 20, false);
	}

	public SimpleMysqlConnector(String server, String database, String username, String passwd,
			int port, boolean useUTF8) {
		this(server, database, username, passwd, port, 20, useUTF8);
	}

	public SimpleMysqlConnector(String server, String database, String username, String passwd,
			int port, int maxConnections, boolean useUTF8) {
		this(server, database, username, passwd, port, 20, useUTF8, "mysql");
	}

	public SimpleMysqlConnector(String server, String database, String username, String passwd,
			int port, int maxConnections, boolean useUTF8, String type) {
		super(maxConnections);
		this.server = server;
		this.database = database;
		this.username = username;
		this.passwd = passwd;
		this.port = port;
		this.useUTF8 = useUTF8;
		this.type = type;
	}

	protected Connection connect() throws SQLException {
		try {
			if (type == "mysql") {
				// Only needed for Oracle MySQL, MariaDB loads automatically
				Class.forName("com.mysql.jdbc.Driver");
			}

			String url = "jdbc:" + type + "://" + server + ":" + port + "/" + database + "?autoReconnect=true";

			// TODO should be configurable; localsessionstate does not reconnect on mysql restart
			// url += "&cachePrepStmts=true&useLocalSessionState=true";
            
			if (useUTF8) {
				url += "&useUnicode=true&characterEncoding=UTF8";
			}
			return DriverManager.getConnection(url, username, passwd);
		} catch (ClassNotFoundException e) {
			NodeLogger.getLogger(getClass()).fatal("Could not load Mysql-Driver-Class!");
			return null;
		}
	}
}
