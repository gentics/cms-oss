package com.gentics.lib.db;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@link AutoCloseable} implementation that will call the handlers, which were registered via {@link #register(DBQueryHandler)} in {@link #close()}.
 * Instances are wrapped around SQL executions.
 */
public class DBQuery implements AutoCloseable {
	/**
	 * Static map of currently registered handlers
	 */
	protected final static Map<String, DBQueryHandler> handlers = new HashMap<>();

	/**
	 * SQL Statement
	 */
	protected String sql;

	/**
	 * Start timestamp (for time measurement)
	 */
	protected long start;

	/**
	 * Create a new instance tracking the given SQL statement. This should always be used in try-with-resources
	 * @param sql SQL statement to track
	 * @return instance
	 */
	public final static DBQuery handle(String sql) {
		return new DBQuery(sql);
	}

	/**
	 * Register the given handler
	 * @param handler handler to register
	 * @return registry uuid (can be used to unregister the handler via {@link #unregister(String)})
	 */
	public final static String register(DBQueryHandler handler) {
		String uuid = UUID.randomUUID().toString();
		handlers.put(uuid, handler);
		return uuid;
	}

	/**
	 * Unregister the handler with given registry uuid
	 * @param uuid registry uuid
	 */
	public final static void unregister(String uuid) {
		handlers.remove(uuid);
	}

	/**
	 * Create an instance for the given SQL statement
	 * @param sql SQL statement
	 */
	private DBQuery(String sql) {
		this.sql = sql;
		this.start = System.currentTimeMillis();
	}

	@Override
	public void close() {
		// finalize time measurement and call the handlers
		long duration = System.currentTimeMillis() - start;
		handlers.values().forEach(h -> h.query(sql, duration));
	}
}
