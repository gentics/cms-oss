package com.gentics.lib.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Update Processor implementation that extracts the generated keys
 */
public class SimpleUpdateProcessor implements UpdateProcessor {
	private SimpleResultProcessor generatedKeys;

	/* (non-Javadoc)
	 * @see com.gentics.lib.db.UpdateProcessor#process(java.sql.Statement)
	 */
	public void process(Statement stmt) throws SQLException {
		generatedKeys = new SimpleResultProcessor();
		try (ResultSet res = stmt.getGeneratedKeys()) {
			generatedKeys.process(res);
		}
	}

	/**
	 * Get the generated keys
	 * @return generated keys
	 */
	public SimpleResultProcessor getGeneratedKeys() {
		return generatedKeys;
	}
}
