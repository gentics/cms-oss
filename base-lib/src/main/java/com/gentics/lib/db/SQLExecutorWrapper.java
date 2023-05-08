/*
 * @author norbert
 * @date 22.12.2009
 * @version $Id: SQLExecutorWrapper.java,v 1.1 2009-12-23 16:25:47 norbert Exp $
 */
package com.gentics.lib.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;

/**
 * Simple Implementation of an {@link SQLExecutor} that wraps another one.
 */
public class SQLExecutorWrapper extends SQLExecutor {

	/**
	 * Wrapped {@link SQLExecutor} instance
	 */
	protected SQLExecutor wrappedExecutor;

	/**
	 * Create an instance of the wrapper
	 * @param wrappedExecutor wrapped executor
	 */
	public SQLExecutorWrapper(SQLExecutor wrappedExecutor) {
		this.wrappedExecutor = wrappedExecutor;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.SQLExecutor#handleResultSet(java.sql.ResultSet)
	 */
	public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
		wrappedExecutor.handleResultSet(rs);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.SQLExecutor#handleUpdateCount(int)
	 */
	public void handleUpdateCount(int count) throws NodeException {
		wrappedExecutor.handleUpdateCount(count);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.SQLExecutor#handleStatment(java.sql.PreparedStatement)
	 */
	public void handleStatment(PreparedStatement stmt) throws SQLException, NodeException {
		wrappedExecutor.handleStatment(stmt);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.SQLExecutor#prepareStatement(java.sql.PreparedStatement)
	 */
	public void prepareStatement(PreparedStatement stmt) throws SQLException {
		wrappedExecutor.prepareStatement(stmt);
	}
}
