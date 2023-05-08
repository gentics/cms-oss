/*
 * @author norbert
 * @date 06.02.2006
 * @version $Id: DataSourceConnector.java,v 1.2 2006-07-28 11:58:36 norbert Exp $
 */
package com.gentics.lib.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

/**
 * Implementation for a Connector that is based on a DataSource. Eventually used
 * Connection pooling is provided by the DataSource and need not be implemented
 * here.
 */
public class DataSourceConnector implements Connector {

	/**
	 * unterlaying DataSource
	 */
	private DataSource dataSource = null;

	/**
	 * shutdown command
	 */
	private String shutDownCommand = null;

	/**
	 * Create an instance of the DataSourceConnector
	 * @param dataSource underlying DataSource
	 * @param shutDownCommand shutdown command
	 */
	public DataSourceConnector(DataSource dataSource, String shutDownCommand) {
		this.dataSource = dataSource;
		this.shutDownCommand = shutDownCommand;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.Connector#getConnection()
	 */
	public PoolConnection getConnection() throws SQLException {
		Connection conn = dataSource.getConnection();

		return new PoolConnection(-1, conn);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.Connector#releaseConnection(com.gentics.lib.db.PoolConnection)
	 */
	public void releaseConnection(PoolConnection c) throws SQLException {
		Connection conn = c.getConnection();

		if (conn != null) {
			conn.close();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.Connector#close()
	 */
	public void close() throws SQLException {
		// when a shutdown command is set, execute it now
		PoolConnection pc = getConnection();
		try {
			if (shutDownCommand != null) {
				Connection c = pc.getConnection();
				Statement st = c.createStatement();

				st.execute(shutDownCommand);
				st.close();
			}
		} finally {
			if (pc != null) {
				releaseConnection(pc);
			}
		}
	}
}
