package com.gentics.lib.db;

/**
 * Interface for handlers, which can be registered at {@link DBQuery} for tracking SQL statements
 */
public interface DBQueryHandler {
	/**
	 * Callback method for the given SQL statement and the execution duration in ms
	 * @param sql SQL statement
	 * @param durationMs duration in ms
	 */
	void query(String sql, long durationMs);
}
