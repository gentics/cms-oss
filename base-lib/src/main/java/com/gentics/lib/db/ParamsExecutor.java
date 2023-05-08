package com.gentics.lib.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/*
 * Automatically feeds the passed SQL parameter
 * objects to the prepared statement.
 */
public abstract class ParamsExecutor extends SQLExecutor {

	/**
	 * SQL Parameter list
	 */
	protected Object[] params;

	/**
	 * @param params The SQL prepared statement parameters
	 */
	public ParamsExecutor(Object[] params) {
		this.params = params;
	}

	@Override
	public void prepareStatement(PreparedStatement statement) throws SQLException {
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				statement.setObject(i + 1, params[i]);
			}
		}
	}
}
