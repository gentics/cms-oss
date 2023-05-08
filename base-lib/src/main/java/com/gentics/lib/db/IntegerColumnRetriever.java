package com.gentics.lib.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;

/**
 * Implementation of a {@link SQLExecutor} that will retrieve all values of a single integer column from the resultset
 */
public class IntegerColumnRetriever extends SQLExecutor {
	/**
	 * Column name
	 */
	protected String columnName;

	/**
	 * Retrieved values
	 */
	protected List<Integer> values = new ArrayList<Integer>();

	/**
	 * Create an instance for the given column name
	 * @param columnName column name
	 */
	public IntegerColumnRetriever(String columnName) {
		this.columnName = columnName;
	}

	@Override
	public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
		while (rs.next()) {
			values.add(rs.getInt(columnName));
		}
	}

	/**
	 * Get the retrieved values
	 * @return retrieved values
	 */
	public List<Integer> getValues() {
		return values;
	}
}
