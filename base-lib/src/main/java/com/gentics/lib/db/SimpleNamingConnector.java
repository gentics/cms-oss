package com.gentics.lib.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 19.11.2003
 */
public class SimpleNamingConnector extends DefaultConnectionManager {
	private int CONNECTION_ATTEMPTS = 3;

	private String name, user, password;

	public SimpleNamingConnector(String name, String user, String password) {
		this.name = name;
		this.user = user;
		this.password = password;
	}

	public SimpleNamingConnector(String name, String user, String password,
			int connectionAttempts) {
		this.name = name;
		this.user = user;
		this.password = password;
		this.CONNECTION_ATTEMPTS = connectionAttempts;
	}

	private DataSource getDataSource(String name) {
		try {
			InitialContext context = new InitialContext();
			DataSource dataSource = (DataSource) context.lookup("jdbc/" + name);

			context.close();
			return dataSource;
		} catch (NamingException ne) {
			return null;
		}
	}

	public Connection getConnection(String name, String user, String password) throws SQLException {
		DataSource dataSource = getDataSource(name);

		if (null == dataSource) {
			return null;

		}

		Connection result = null;
		SQLException ex = null;

		int tries = 0;

		while ((CONNECTION_ATTEMPTS > tries) && (null == result)) {
			try {
				result = dataSource.getConnection(user, password);
				ex = null;
			} catch (SQLException se) {
				ex = se;
			}
			tries++;
		}

		if (null != ex) {
			throw ex;
		}

		return result;
	}

	protected Connection connect() throws SQLException {
		return getConnection(name, user, password);
	}
}
