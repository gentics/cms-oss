package com.gentics.lib.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;

/**
 * Counts the number of rows in the result set
 */
public class CountingExecutor extends ParamsExecutor {

	/**
	 * Row count
	 */
	protected int rowCount = 0;

	/**
	 * Sets the SQL parameters
	 * @param params SQL parameters
	 */
	public CountingExecutor(Object[] params) {
		super(params);
	}

	/**
	 * Iterates the result set and counts the number of rows
	 */
	@Override
	public void handleResultSet(ResultSet resultSet) throws SQLException,
			NodeException {
		while (resultSet.next()) {
			rowCount++;
		}
	}

	/**
	 * The number of rows in the result set
	 * @return Number of rows
	 */
	public int getRowCount() {
		return rowCount;
	}
}
