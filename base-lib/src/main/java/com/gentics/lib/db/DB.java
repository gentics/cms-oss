/**
 *
 * @author laurin
 * @date 25.02.2005
 * @version $Id: DB.java,v 1.65.2.1 2011-04-07 09:57:55 norbert Exp $
 *
 */
package com.gentics.lib.db;

import java.io.File;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.dbcp.DelegatingConnection;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.DatatypeHelper.SQLDatatype;
import com.gentics.lib.etc.MiscUtils;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.io.FileRemover;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;
import com.gentics.lib.util.AttributedThreadGroup;

/**
 * @todo make this class non-static, add a Factory or a Manager to allow
 *       multiple simultaneous connections
 * @author laurin
 */
public class DB {
	private static boolean m_autoCommit = true;

	private static Map<DBHandle, Connector> connectorPool = new ConcurrentHashMap<DBHandle, Connector>(50, 0.75f, 10);

	private static NodeLogger logger = NodeLogger.getNodeLogger(DB.class);

	/**
	 * threadlocal map of open transactions (stored object is a map, keys are
	 * the DBHandles, values are the PoolConnections)
	 */
	private static ThreadLocal<Map<DBHandle, PoolConnection>> openTransactions = new ThreadLocal<Map<DBHandle, PoolConnection>>();
    
	private static final String OPEN_TRANSACTIONS_TG_KEY = "DB.openTransactions";

	/**
	 * Name of the cacheregion for metadata
	 * @see #getMetadataCache()
	 */
	private final static String CACHE_METADATACACHEREGION = "gentics-portal-cachedb-metadata";
    
	/**
	 * Cache group for Table Columns (inside of {@link #CACHE_METADATACACHEREGION})
	 * @see #getTableColumns(DBHandle, String)
	 */
	private final static String CACHE_GROUP_TABLECOLUMNS = "tableColumns";
    
	/**
	 * metadata cache, do NOT directly access this attribute, isntead use {@link #getMetadataCache()}
	 * @see #getMetadataCache()
	 */
	private static PortalCache metadataCache;
    
	/**
	 * Stores the exitence of tables and fields. key = handle / value: HashMap of tablename.fieldname / value = Boolean (true/false)
	 */
	private static Map<DBHandle, Map<String, Boolean>> tableFieldExistStorePerHandle = new HashMap<DBHandle, Map<String, Boolean>>();

	/**
	 * map of database product names per dbhandle
	 */
	private static Map<DBHandle, String> databaseProductNames = new HashMap<DBHandle, String>();

	/**
	 * Threadlocal for current handle
	 */
	private static ThreadLocal<DBHandle> currentHandle = new ThreadLocal<DBHandle>();

	/**
	 * Convert the given parameter to a String that can be used in debug output
	 * @param param query parameter value
	 * @return debug string
	 */
	private static String toDebugSql(Object param) {
		if (param instanceof String) {
			return "'" + param.toString() + "'";
		} else if (param == null) {
			return "[NULL]";
		} else {
			return param.toString();
		}
	}

	/**
	 * Create a debug string from the given sql statement and query parameters
	 * @param sql sql statement
	 * @param params query parameters
	 * @return debug string
	 */
	public static String debugSql(String sql, Object[] params) {
		if (params == null) {
			return sql;
		}
		for (int i = 0; i < params.length; i++) {
			Object param = params[i];
			int qmPos = sql.indexOf('?');

			if (qmPos > 0) {
				sql = sql.substring(0, qmPos) + DB.toDebugSql(param) + sql.substring(qmPos + 1);
			} else {
				logger.error("Query failed: Too many parameters for query '" + sql + "': " + StringUtils.merge(params, ","));
			}
		}
		return sql;
	}

	/**
	 * Get the original connection
	 * @param stmt statement
	 * @return connection
	 * @throws SQLException
	 */
	private static Connection getOriginalConnection(PreparedStatement stmt) throws SQLException {
		Connection connection = stmt.getConnection();
		if (connection instanceof DelegatingConnection) {
			connection = ((DelegatingConnection) connection).getInnermostDelegate();
		}
		return connection;
	}

	/**
	 * Feed the given parameters into the prepared statement
	 * @param stmt prepared statement
	 * @param params query parameter values
	 * @param types optional list of parameter value types
	 * @throws SQLException
	 */
	private static void feedParams(PreparedStatement stmt, Object[] params, List<Integer> types) throws SQLException {
		if (params == null) {
			return;
		}
		Object param = null;

		for (int i = 0; i < params.length; i++) {
			param = params[i];
			Integer type = (!ObjectTransformer.isEmpty(types) && types.size() > i) ? types.get(i) : null;
			if (param == null) {
				if (type != null) {
					stmt.setNull(i + 1, type);
				} else {
					stmt.setObject(i + 1, null);
				}
			} else if (param instanceof Date) {
				stmt.setTimestamp(i + 1, new java.sql.Timestamp(((Date) param).getTime()));
			} else if (param instanceof String) {
				if (type != null && (type.intValue() == Types.CLOB || type.intValue() == Types.LONGVARCHAR)) {
					Connection conn = getOriginalConnection(stmt);
					if (conn != null) {
						Clob clob = conn.createClob();
						clob.setString(1, (String)param);
						stmt.setClob(i + 1, clob);
					} else {
						stmt.setString(i + 1, (String) param);
					}
				} else {
					stmt.setString(i + 1, (String) param);
				}
			} else if (param instanceof Long) {
				stmt.setLong(i + 1, ((Long) param).longValue());
			} else if (param instanceof Integer) {
				stmt.setInt(i + 1, ((Integer) param).intValue());
			} else if (param instanceof Float) {
				stmt.setFloat(i + 1, ((Float) param).floatValue());
			} else if (param instanceof Double) {
				stmt.setDouble(i + 1, ((Double) param).doubleValue());
			} else if (param instanceof Resolvable) {
				stmt.setString(i + 1, param.toString());
			} else if (param instanceof Boolean) {
				stmt.setBoolean(i + 1, ((Boolean) param).booleanValue());
			} else if (param instanceof byte[]) {
				if (type != null && type.intValue() == Types.BLOB) {
					Connection conn = getOriginalConnection(stmt);
					if (conn != null) {
						Blob blob = conn.createBlob();
						blob.setBytes(1, (byte[])param);
						stmt.setBlob(i + 1, blob);
					} else {
						stmt.setObject(i + 1, param);
					}
				} else {
					stmt.setObject(i + 1, param);
				}
			} else {
				stmt.setObject(i + 1, param);
			}
		}
	}

	/**
	 * Set the global autocommit flag
	 * @param on true to enable autocommit, false to disable
	 * @throws SQLException
	 */
	public static void setAutocommit(boolean on) throws SQLException {
		m_autoCommit = on;
	}

	/**
	 * Add the given connector
	 * @param con connector
	 * @return DBHandle
	 */
	public static DBHandle addConnector(Connector con) {
		return addConnector(con, null, null, false);
	}

	/**
	 * Add the given connector
	 * @param con connector
	 * @param cacheDBMetaData true if DB metadata shall be cached
	 * @return DBHandle
	 */
	public static DBHandle addConnector(Connector con, boolean cacheDBMetaData) {
		return addConnector(con, null, null, cacheDBMetaData);
	}

	/**
	 * Add the given connector
	 * @param con connector
	 * @param name name
	 * @param description description
	 * @param cacheDBMetaData true to cache DB metadata
	 * @return DBHandle
	 */
	public static DBHandle addConnector(Connector con, String name, String description, boolean cacheDBMetaData) {
		int type = 0;

		if (con instanceof SimpleNamingConnector) {
			type = DBHandle.CONNECTOR_TYPE_NAMING;
		} else {
			type = DBHandle.CONNECTOR_TYPE_GENERIC;
		}
		DBHandle handle = new DBHandle(name, description, type, cacheDBMetaData);

		DB.connectorPool.put(handle, con);
		return handle;
	}

	/**
	 * Close the connector identified by the DBHandle. Remove the connector from the pool and remove all cached data
	 * @param handle DBHandle
	 */
	public static void closeConnector(DBHandle handle) {
		Connector c = DB.connectorPool.get(handle);

		if (c != null) {
			try {
				c.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		tableFieldExistStorePerHandle.remove(handle);
		databaseProductNames.remove(handle);
		if (metadataCache != null) {
			try {
				metadataCache.clear();
			} catch (PortalCacheException e) {
			}
		}
		DB.connectorPool.remove(handle);
	}

	/**
	 * Get a pooled connection for the given handle
	 * @param handle db handle
	 * @return pooled connection
	 * @throws IllegalArgumentException
	 * @throws SQLException
	 */
	public static PoolConnection getPoolConnection(DBHandle handle) throws IllegalArgumentException, SQLException {
		Connector connector = DB.connectorPool.get(handle);

		if (connector != null) {
			PoolConnection c = connector.getConnection();
			// set the current handle to the threadlocal. This makes sure, that e.g. the RoundRobinHandlePool can get the current handle for
			// the thread, as long as the thread uses this connection
			currentHandle.set(handle);
			return c;
		} else {
			throw new IllegalArgumentException("No connector found for the handle ");
		}
	}

	/**
	 * Perform the given query and log errors.
	 * @param handle db handle
	 * @param sql sql statement
	 * @param params parameters
	 * @param proc result processor (collection the results)
	 * @throws SQLException
	 */
	public static void query(DBHandle handle, String sql, Object[] params, ResultProcessor proc) throws SQLException {
		query(handle, sql, params, proc, true);
	}

	/**
	 * Execute multiple queries in a batch
	 * @param handle DBHandle
	 * @param sql array of SQL statements
	 * @param params array of query parameters
	 * @param proc result processor (must not be null)
	 * @param uProc optional update processor
	 * @param logErrors true to log errors
	 * @param continueOnErrors true to continue in case of errors
	 * @throws SQLException
	 */
	public static void queryBatch(DBHandle handle, String[] sql, Object[][] params,
			ResultProcessor proc, UpdateProcessor uProc, boolean logErrors, boolean continueOnErrors) throws SQLException {
		try (DBTrx trx = new DBTrx(handle, logErrors)) {
			RuntimeProfiler.beginMark(ComponentsConstants.DB_QUERY_WITH_HANDLE, sql);
			boolean debug = logger.isDebugEnabled();

			int i = 0;

			for (i = 0; i < sql.length; ++i) {
				try {
					if (sql[i].trim().toLowerCase().startsWith("select")) {
						internalQuery(trx.getConnection(), sql[i], params.length > i ? params[i] : null, null, proc, logErrors, debug, handle.getFetchSize());
					} else {
						internalUpdate(trx.getConnection(), sql[i], params.length > i ? params[i] : null, null, uProc, true, debug);
					}
				} catch (SQLException e) {
					if (continueOnErrors) {
						logger.error("Error while performing sql statement {" + sql[i] + "}. Continuing with next statement", e);
					} else {
						if (logErrors) {
							logger.error("error in sql statement " + sql[i]);
						}
						throw e;
					}
				}
			}

			trx.success();
		} catch (RuntimeException e) {
			throw e;
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DB_QUERY_WITH_HANDLE, sql);
		}
	}

	/**
	 * Perform the given query (internal method)
	 * @param c connection
	 * @param sql sql statement
	 * @param params parameter array
	 * @param types optional array of {@link Types}
	 * @param proc result processor
	 * @param logErrors true if errors shall be logged
	 * @param debug true if debug log is enabled
	 * @param fetchsize fetch size to be passed to the statement
	 * @throws SQLException
	 */
	protected static void internalQuery(Connection c, String sql, Object[] params,
			List<Integer> types, ResultProcessor proc, boolean logErrors, boolean debug, int fetchsize) throws SQLException {
		long time1 = 0, time2 = 0, time3 = 0, time4 = 0;

		if (debug) {
			time1 = System.currentTimeMillis();
		}
		PreparedStatement p = null;
		ResultSet rs = null;

		try {
			p = c.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			p.setFetchSize(fetchsize);
			feedParams(p, params, types);
			if (debug) {
				time2 = System.currentTimeMillis();
			}
			rs = p.executeQuery();
			if (debug) {
				time3 = System.currentTimeMillis();
			}
			proc.process(rs);
			if (debug) {
				time4 = System.currentTimeMillis();
			}
			if (debug) {
				long now = System.currentTimeMillis();
				logger.debug(
						"sql: (" + (now - time1) + ")=(" + (time2 - time1) + "/" + (time3 - time2) + "/" + (time4 - time3) + "/" + (now - time4) + "): "
						+ DB.debugSql(sql, params));
			}
		} finally {
			close(rs);
			close(p);
		}
	}

	/**
	 * Perform the given query
	 * @param handle db handle
	 * @param sql sql statement
	 * @param params parameters
	 * @param proc result processor (collection the results)
	 * @param logErrors true when errors shall be logged, false if not
	 * @throws SQLException
	 */
	public static void query(DBHandle handle, String sql, Object[] params, ResultProcessor proc, boolean logErrors) throws SQLException {
		query(handle, sql, params, null, proc, logErrors);
	}

	/**
	 * Perform the given query
	 * @param handle db handle
	 * @param sql sql statement
	 * @param params parameters
	 * @param types optional list of {@link Types}
	 * @param proc result processor (collection the results)
	 * @param logErrors true when errors shall be logged, false if not
	 * @throws SQLException
	 */
	public static void query(DBHandle handle, String sql, Object[] params, List<Integer> types, ResultProcessor proc, boolean logErrors) throws SQLException {
		try (DBTrx trx = new DBTrx(handle, logErrors)) {
			RuntimeProfiler.beginMark(ComponentsConstants.DB_QUERY_WITH_HANDLE, sql);
			boolean debug = logger.isDebugEnabled();

			internalQuery(trx.getConnection(), sql, params, types, proc, logErrors, debug, handle.getFetchSize());
			trx.success();
		} catch (SQLException e) {
			if (logErrors) {
				logger.error("error in sql statement " + sql, e);
			}
			throw e;
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DB_QUERY_WITH_HANDLE, sql);
		}
	}

	/**
	 * Execute the given query
	 * @param handle DB Handle
	 * @param sql sql statement
	 * @param param query parameters
	 * @param proc result processor
	 * @throws SQLException
	 */
	public static void query(DBHandle handle, String sql, Object param, ResultProcessor proc) throws SQLException {
		DB.query(handle, sql, new Object[] { param }, proc);
	}

	/**
	 * Execute the given query (without query parameters)
	 * @param handle DB Handle
	 * @param sql sql statement
	 * @param proc result processor
	 * @throws SQLException
	 */
	public static void query(DBHandle handle, String sql, ResultProcessor proc) throws SQLException {
		DB.query(handle, sql, new Object[] {}, proc);
	}

	/**
	 * Perform an update
	 * @param handle db handle
	 * @param sql sql statement
	 * @param params statement parameters
	 * @return number of modified rows
	 * @throws SQLException
	 */
	public static int update(DBHandle handle, String sql, Object[] params) throws SQLException {
		return update(handle, sql, params, null);
	}

	/**
	 * Perform an update
	 * @param handle db handle
	 * @param sql sql statement to perform
	 * @param params parameters
	 * @param proc update processor
	 * @return number of modified rows
	 * @throws SQLException
	 */
	public static int update(DBHandle handle, String sql, Object[] params, UpdateProcessor proc) throws SQLException {
		return update(handle, sql, params, null, proc, true);
	}

	/**
	 * Internal method that performs an update
	 * @param c connection
	 * @param sql sql statement
	 * @param params query parameters
	 * @param types optional list of parameter value types
	 * @param proc optional update processor
	 * @param usePreparedStatement true to use a prepared statement
	 * @param debug true to add debug log
	 * @return count of updated rows
	 * @throws SQLException
	 */
	protected static int internalUpdate(Connection c, String sql, Object[] params,
			List<Integer> types, UpdateProcessor proc, boolean usePreparedStatement, boolean debug) throws SQLException {
		if (debug) {
			logger.debug("sql: " + DB.debugSql(sql, params));
		}
		int ret = 0;
		Statement p = null;

		try {
			if (usePreparedStatement) {
				p = c.prepareStatement(sql);
				feedParams((PreparedStatement) p, params, types);
				ret = ((PreparedStatement) p).executeUpdate();
			} else {
				p = c.createStatement();
				ret = p.executeUpdate(sql);
			}
			if (proc != null) {
				proc.process(p);
			}
		} finally {
			close(p);
		}
		return ret;
	}

	/**
	 * Perform an update
	 * @param handle db handle
	 * @param sql sql statement to perform
	 * @param params parameters
	 * @param proc update processor
	 * @param usePreparedStatement true if prepared statements should be used, false if not
	 * @return number of modified rows
	 * @throws SQLException
	 */
	public static int update(DBHandle handle, String sql, Object[] params,
			UpdateProcessor proc, boolean usePreparedStatement) throws SQLException {
		return update(handle, sql, params, null, proc, usePreparedStatement);
	}

	/**
	 * Perform an update
	 * @param handle db handle
	 * @param sql sql statement to perform
	 * @param params parameters
	 * @param types optional list of types
	 * @param proc update processor
	 * @param usePreparedStatement true if prepared statements should be used, false if not
	 * @return number of modified rows
	 * @throws SQLException
	 */
	public static int update(DBHandle handle, String sql, Object[] params, List<Integer> types,
			UpdateProcessor proc, boolean usePreparedStatement) throws SQLException {
		return update(handle, sql, params, types, proc, usePreparedStatement, null);
	}

	/**
	 * Perform an update
	 * @param handle db handle
	 * @param sql sql statement to perform
	 * @param params parameters
	 * @param types optional list of types
	 * @param proc update processor
	 * @param usePreparedStatement true if prepared statements should be used, false if not
	 * @param returnGeneratedKeys names of the columns that will contain generated keys, null to not return any generated keys
	 * @return number of modified rows
	 * @throws SQLException
	 */
	public static int update(DBHandle handle, String sql, Object[] params, List<Integer> types,
			UpdateProcessor proc, boolean usePreparedStatement, String[] returnGeneratedKeys) throws SQLException {
		Statement p = null;
		long time1 = 0, time2 = 0, time3 = 0;

		try (DBTrx trx = new DBTrx(handle, true)) {
			RuntimeProfiler.beginMark(ComponentsConstants.DB_UPDATE_WITH_HANDLE, sql);
			boolean debug = logger.isDebugEnabled();

			int ret = 0;

			try {
				if (usePreparedStatement) {
					if (debug) {
						time1 = System.currentTimeMillis();
					}
					if (returnGeneratedKeys != null) {
						if (handle.isUpperCaseIdentifiers()) {
							for (int i = 0; i < returnGeneratedKeys.length; i++) {
								returnGeneratedKeys[i] = returnGeneratedKeys[i].toUpperCase();
							}
						}
						p = trx.getConnection().prepareStatement(sql, returnGeneratedKeys);
					} else {
						p = trx.getConnection().prepareStatement(sql);
					}
					feedParams((PreparedStatement) p, params, types);
					if (debug) {
						time2 = System.currentTimeMillis();
					}
					ret = ((PreparedStatement) p).executeUpdate();
				} else {
					if (debug) {
						time1 = System.currentTimeMillis();
					}
					p = trx.getConnection().createStatement();
					if (debug) {
						time2 = System.currentTimeMillis();
					}
					if (returnGeneratedKeys != null) {
						if (handle.isUpperCaseIdentifiers()) {
							for (int i = 0; i < returnGeneratedKeys.length; i++) {
								returnGeneratedKeys[i] = returnGeneratedKeys[i].toUpperCase();
							}
						}
						ret = p.executeUpdate(sql, returnGeneratedKeys);
					} else {
						ret = p.executeUpdate(sql);
					}
				}
				if (proc != null) {
					proc.process(p);
				}

				trx.success();

				if (debug) {
					time3 = System.currentTimeMillis();
				}

				if (debug) {
					long now = System.currentTimeMillis();
					logger.debug("sql: (" + (now - time1) + ")=(" + (time2 - time1) + "/" + (time3 - time2) + "/" + (now - time3) + "): "
							+ DB.debugSql(sql, params));
				}

				return ret;
			} finally {
				close(p);
			}
		} catch (SQLException e) {
			logger.error("error while running sql statement {" + DB.debugSql(sql, params) + "}", e);
			throw e;
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DB_UPDATE_WITH_HANDLE, sql);
		}
	}

	/**
	 * Perform an update
	 * @param handle db handle
	 * @param sql sql statement
	 * @throws SQLException
	 * @return number of modified rows
	 */
	public static int update(DBHandle handle, String sql) throws SQLException {
		return update(handle, sql, null);
	}

	/**
	 * Perform the given statement with a batch of parameters
	 * @param handle handle
	 * @param sql sql statement
	 * @param paramsColl collection of parameters
	 * @param types parameter types
	 * @return number of modified records
	 * @throws SQLException
	 */
	public static int batchUpdate(DBHandle handle, String sql, Collection<Object[]> paramsColl, List<Integer> types) throws SQLException {
		if (ObjectTransformer.isEmpty(paramsColl)) {
			return 0;
		}

		// if the driver does not support batch statements, we do a series of normal updates instead
		if (!handle.supportsBatchUpdates()) {
			int modified = 0;
			for (Object[] params : paramsColl) {
				modified += update(handle, sql, params, types, null, true);
			}
			return modified;
		}

		long time1 = 0, time2 = 0;

		try (DBTrx trx = new DBTrx(handle, true)) {
			RuntimeProfiler.beginMark(ComponentsConstants.DB_UPDATE_WITH_HANDLE, sql);
			boolean debug = logger.isDebugEnabled();

			if (debug) {
				time1 = System.currentTimeMillis();
			}

			try (PreparedStatement p = trx.getConnection().prepareStatement(sql)) {
				for (Object[] params : paramsColl) {
					if (debug) {
						logger.debug("sql (batch): " + DB.debugSql(sql, params));
					}
					feedParams(p, params, types);
					p.addBatch();
				}

				if (debug) {
					time2 = System.currentTimeMillis();
				}

				int[] ret = p.executeBatch();

				trx.success();

				if (debug) {
					long now = System.currentTimeMillis();
					logger.debug("batch: (" + (now - time1) + ")=(" + (time2 - time1) + "/" + (now - time2) + ")");
				}

				int changed = 0;
				for (int i : ret) {
					if (i >= 0) {
						changed += i;
					}
				}
				return changed;
			}
		} catch (SQLException e) {
			logger.error("error while running batch sql statement {" + sql + "}", e);
			throw e;
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DB_UPDATE_WITH_HANDLE, sql);
		}
	}

	/**
	 * Commit the open transaction of the given DB Handle
	 * @param handle DB Handle
	 * @throws SQLException
	 */
	public static void commit(DBHandle handle) throws SQLException {
		if (handle != null) {
			Connector conn = DB.connectorPool.get(handle);

			if (conn != null) {
				PoolConnection c = conn.getConnection();
				try {
					c.getConnection().commit();
					c.onCommit();
				} finally {
					DB.safeRelease(handle, c);
				}
			} else {
				throw new IllegalArgumentException("DB::query: No valid Connection bound to submitted Handle");
			}
		} else {
			throw new IllegalArgumentException("DB::query: Handle is null");
		}
	}

	/**
	 * Release the {@link PoolConnection} instance, that was fetched with the DB Handle
	 * @param handle DB Handle
	 * @param c PoolConnection instance
	 * @throws SQLException
	 */
	public static void safeRelease(DBHandle handle, PoolConnection c) throws SQLException {
		if (handle != null) {
			if (c != null) {
				Connector con = DB.connectorPool.get(handle);

				if (con != null) {
					con.releaseConnection(c);
					// the thread released the connection, so the current handle is removed also
					currentHandle.remove();
				} else {
					throw new IllegalArgumentException("DB::query: No valid Connection bound to submitted Handle");
				}
			}
		} else {
			throw new IllegalArgumentException("DB::query: Handle is null");
		}
	}

	/**
	 * Get the column data for the given table
	 * @param handle db handle
	 * @param table table
	 * @return column data
	 * @throws SQLException
	 */
	public static ColumnData getColumnData(DBHandle handle, String table) throws SQLException {
		// Try to retrieve result from cache
		Object cacheKey = getTableColumnCacheKey(handle, table);
		Object cachedResult = getFromMetadataCache(CACHE_GROUP_TABLECOLUMNS, cacheKey);

		ColumnData columnData = null;

		// If we got it cached, return it
		if (cachedResult instanceof ColumnData) {
			columnData = (ColumnData) cachedResult;
		} else {
			columnData = new ColumnData(handle, table);
			putIntoMetadataCache(CACHE_GROUP_TABLECOLUMNS, cacheKey, columnData);
		}

		return columnData;
	}

	/**
	 * get a sorted list of columnnames of a table using DatabaseMetaData,
	 * ensureConnection and safeRelease TODO implement
	 * @param handle database handle
	 * @param table tablename
	 * @return list of columnames (Strings).
	 */
	public static List<String> getTableColumns(DBHandle handle, String table) throws SQLException {
		return getColumnData(handle, table).getNames();
	}

	/**
	 * Get list of column data types for the given table
	 * @param handle database handle
	 * @param table tablename
	 * @return list of column data types
	 * @throws SQLException
	 */
	public static List<Integer> getColumnDataTypes(DBHandle handle, String table) throws SQLException {
		return getColumnData(handle, table).getTypes();
	}

	/**
	 * check whether the given table exists
	 * @param handle db handle
	 * @param tableName name of the table to check
	 * @return true when the table exists, false if not
	 * @throws SQLException
	 */
	public static boolean tableExists(DBHandle handle, String tableName) throws SQLException {
		if (handle != null) {
			try (DBTrx trx = new DBTrx(handle)) {
				boolean ret;
				Map<String, Boolean> tableFieldExistStore = getTableFieldExistStore(handle);

				synchronized (tableFieldExistStore) {
					if (handle.isCacheDBMetaData()) {
						Boolean cached = tableFieldExistStore.get(tableName);

						if (cached != null) {
							return cached.booleanValue();
						}
					}
					DatabaseMetaData metaData = trx.getConnection().getMetaData();

					try (ResultSet tableDefs = metaData.getTables(null, handle.getDbSchema(),
							metaData.storesUpperCaseIdentifiers() ? tableName.toUpperCase() : tableName, null)) {
						ret = tableDefs.next();
					}

					trx.success();

					if (handle.isCacheDBMetaData()) {
						tableFieldExistStore.put(tableName, ret);
					}

					return ret;
				}
			}
		} else {
			return false;
		}
	}

	/**
	 * Get the map containing cached existance information for tables and fields.
	 * If the map does not yet exist, a new one is generated
	 * @param handle DB Handle
	 * @return map containing cached information (never null)
	 */
	private static Map<String, Boolean> getTableFieldExistStore(DBHandle handle) {
		synchronized (tableFieldExistStorePerHandle) {
			Map<String, Boolean> map = tableFieldExistStorePerHandle.get(handle);

			if (map == null) {
				map = new HashMap<String, Boolean>();
				tableFieldExistStorePerHandle.put(handle, map);
			}
			return map;
		}
	}

	/**
	 * check whether the given field exists in the given table
	 * @param handle db handle
	 * @param tableName table name
	 * @param fieldName field name
	 * @return true when the field exists, false if not
	 * @throws SQLException
	 */
	public static boolean fieldExists(DBHandle handle, String tableName, String fieldName) throws SQLException {
		if (handle != null) {
			try (DBTrx trx = new DBTrx(handle)) {
				// check if it is cached ...
				String key = tableName + "." + fieldName;
				boolean ret;
				Map<String, Boolean> tableFieldExistStore = getTableFieldExistStore(handle);

				synchronized (tableFieldExistStore) {
					if (handle.isCacheDBMetaData()) {
						Boolean cached = tableFieldExistStore.get(key);

						if (cached != null) {
							return cached.booleanValue();
						}
					}

					DatabaseMetaData metaData = trx.getConnection().getMetaData();
					boolean upperCase = metaData.storesUpperCaseIdentifiers();
					try (ResultSet fieldDefs = metaData.getColumns(null, handle.getDbSchema(), upperCase ? tableName.toUpperCase() : tableName,
							upperCase ? fieldName.toUpperCase() : fieldName)) {
						ret = fieldDefs.next();
					}

					trx.success();

					if (handle.isCacheDBMetaData()) {
						tableFieldExistStore.put(key, ret);
					}
					return ret;
				}
			}
		} else {
			return false;
		}
	}

	/**
	 * Get the product name of the underlying database for the given handle.
	 * This information is stored in a static map for performance reasons.
	 * @param handle db handle
	 * @return database product name
	 * @throws SQLException
	 */
	public static String getDatabaseProductName(DBHandle handle) throws SQLException {
		// no handle given, so there is not product
		if (handle == null) {
			return null;
		}

		// try to get from internal store
		String databaseProductName = databaseProductNames.get(handle);

		if (databaseProductName != null) {
			return databaseProductName;
		}

		// get database connection
		Connector conn = DB.connectorPool.get(handle);
		PoolConnection c = null;

		try {
			if (conn != null) {
				c = conn.getConnection();
				DatabaseMetaData metaData = c.getConnection().getMetaData();

				// retrieve the name
				databaseProductName = metaData.getDatabaseProductName();
			}
		} finally {
			// release the database connection
			safeRelease(handle, c);
		}

		// when the product name is known now, put it into the local store
		if (databaseProductName != null) {
			databaseProductNames.put(handle, databaseProductName);
		}

		return databaseProductName;
	}

	/**
	 * Get the database metadata object of the underlying database and pass the instance to the given handler. The handler can use the object in the handle method, but
	 * must not store it for later use, since it will become unusable, when the connection is closed.
	 * @param dbHandle db handle
	 * @param handler handler
	 * @throws SQLException
	 * @throws NodeException
	 */
	public static void handleDatabaseMetaData(DBHandle dbHandle, DatabaseMetaDataHandler handler) throws SQLException, NodeException {
		if (dbHandle != null && handler != null) {
			PoolConnection c = null;
			boolean transaction = false;

			try {
				// first check for running transactions
				c = getOpenConnection(dbHandle);
				if (c != null) {
					// found an open transaction (will not release the
					// connection)
					transaction = true;
				} else {
					c = getPoolConnection(dbHandle);
				}

				DatabaseMetaData metaData = c.getConnection().getMetaData();

				handler.handleMetaData(metaData);
			} finally {
				if (!transaction) {
					safeRelease(dbHandle, c);
				}
			}
		}
	}

	/**
	 * Check the given table
	 * @param handle db handle to test
	 * @param table table definition to test
	 * @param handleId handle id
	 * @param createIfMissing true when missing columns shall be created, false
	 *        if not
	 * @return true when the table passed all tests, false if not
	 * @throws SQLException
	 */
	public static boolean checkTable(DBHandle handle, TableDefinition table, String handleId,
			boolean createIfMissing) throws SQLException {
		boolean allChecksPassed = true;

		if (!DB.tableExists(handle, table.getTableName())) {
			String message = "Table {" + table.getTableName() + "} does not exist for datasource-handle {" + handleId + "}!";

			if (table.required) {
				logger.error(message);
			} else {
				logger.warn(message);
			}
			allChecksPassed = table.required ? false : true;
		} else {
			allChecksPassed = checkColumns(handle, table.columns, handleId, createIfMissing);
		}

		return allChecksPassed;
	}

	/**
	 * Check the given column definitions
	 * @param handle db handle to test
	 * @param columns array of column definitions
	 * @param handleId handle id
	 * @param createIfMissing true when missing columns shall be created, false if not
	 * @return true when the handle passed all tests, false if not
	 */
	public static boolean checkColumns(DBHandle handle, ColumnDefinition[] columns, String handleId, 
			boolean createIfMissing) throws SQLException {
		boolean allChecksPassed = true;

		try (DBTrx trx = new DBTrx(handle)) {
			for (int i = 0; i < columns.length; ++i) {
				allChecksPassed &= checkColumn(trx.getConnection(), columns[i], handleId, createIfMissing, handle);
			}

			trx.success();
		} catch (Exception e) {
			logger.error("Error while checking columns", e);
			allChecksPassed = false;
		}

		return allChecksPassed;
	}
    
	/**
	 * Check whether the given column definition exists and is of the correct type.
	 * @param handle db handle to test
	 * @param column column definition
	 * @return true if the column exists and has the correct type, false otherwise
	 * @throws SQLException
	 */
	public static boolean checkColumn(DBHandle handle, ColumnDefinition column) throws SQLException {
		return checkColumns(handle, new ColumnDefinition[] { column}, "checkColumnHandleId", false);
	}

	/**
	 * Check the given column definition
	 * @param conn database connection
	 * @param column column definition
	 * @param handleId handle id
	 * @param createIfMissing true when missing columns shall be created, false if not
	 * @param dbHandle db handle
	 * @return true when the handle passed all tests, false if not
	 */
	protected static boolean checkColumn(Connection conn, ColumnDefinition column, String handleId, 
			boolean createIfMissing, DBHandle dbHandle) throws SQLException {
		boolean checkPassed = true;
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		String databaseName = databaseMetaData.getDatabaseProductName();

		boolean allUpperCase = databaseMetaData.storesUpperCaseIdentifiers();
		String columnName = allUpperCase ? column.getColumnName().toUpperCase() : column.getColumnName();
		String tableName = allUpperCase ? column.getTableName().toUpperCase() : column.getTableName();
		// check whether the column exists
		try (ResultSet columns = databaseMetaData.getColumns(null, dbHandle.getDbSchema(), tableName, columnName)) {
			if (columns.next()) {
				checkPassed = column.checkColumn(databaseName, columns);
				if (!checkPassed) {
					String message = "Column {" + column.columnName + "} in table {" + column.tableName + "} of datasource-handle {" + handleId
							+ "} does not have the required type!";

					if (column.required) {
						logger.error(message);
					} else {
						logger.warn(message);
					}
				}
			} else {
				if (createIfMissing) {
					try {
						forceInfoLog(
								"Creating column {" + column.columnName + "} in table {" + column.tableName
								+ "}. This may take a while, depending on your overall system performance and the table size...");
						long start = System.currentTimeMillis();

						conn.createStatement().executeUpdate(column.getCreateStatement(databaseName));
						DB.clearTableFieldCache();
						forceInfoLog(
								"Successfully created column {" + column.columnName + "} in table {" + column.tableName + "} in " + (System.currentTimeMillis() - start)
								+ " ms.");
					} catch (SQLException ex) {
						String message = "Error while creating column {" + column.columnName + "} in table {" + column.tableName + "} of datasource-handle {"
								+ handleId + "}";

						if (column.required) {
							logger.error(message, ex);
						} else {
							logger.warn(message, ex);
						}
						checkPassed = false;
					}
				} else {
					String message = "Column {" + column.columnName + "} in table {" + column.tableName + "} of datasource-handle {" + handleId + "} is missing!";

					if (column.required) {
						logger.error(message);
					} else {
						logger.warn(message);
					}
					checkPassed = false;
				}
			}
		}

		// when all checks were passed so far, we do eventually existing specific tests
		if (checkPassed) {
			// even if the column is not required, the tests for it must be passed, when the column exists
			if (!column.doSpecificTest(dbHandle, handleId, createIfMissing, NodeLogger.getNodeLogger(DB.class))) {
				return false;
			}
		}

		return column.required ? checkPassed : true;
	}

	/**
	 * Inner class for a column definition
	 */
	public static class ColumnDefinition {

		/**
		 * table name
		 */
		protected String tableName;

		/**
		 * column name
		 */
		protected String columnName;

		/**
		 * real sql type (read from metadata)
		 */
		protected int sqlType;

		/**
		 * name of the sql type in the DDL statement
		 */
		protected String sqlTypeName;

		/**
		 * whether the column is nullable or not
		 */
		protected boolean nullable;

		/**
		 * the default value of the volumn
		 */
		protected String defaultValue;

		/**
		 * Whether this column is required
		 */
		protected boolean required;

		/**
		 * Create an instance of the column definition
		 * @param tableName table name
		 * @param columnName column name
		 * @param sqlType sql type
		 * @param sqlTypeName name of the sql type in the DDL
		 * @param nullable whether the column may be null or not
		 * @param defaultValue default value of the column (may be null)
		 * @param required whether this column is required
		 */
		public ColumnDefinition(String tableName, String columnName, int sqlType, String sqlTypeName, boolean nullable, String defaultValue, boolean required) {
			this.tableName = tableName;
			this.columnName = columnName;
			this.sqlType = sqlType;
			this.sqlTypeName = sqlTypeName;
			this.nullable = nullable;
			this.defaultValue = defaultValue;
			this.required = required;
		}

		/**
		 * @return Returns the columnName.
		 */
		public String getColumnName() {
			return columnName;
		}

		/**
		 * @param columnName The columnName to set.
		 */
		public void setColumnName(String columnName) {
			this.columnName = columnName;
		}

		/**
		 * @return Returns the defaultValue.
		 */
		public String getDefaultValue() {
			return defaultValue;
		}

		/**
		 * @param defaultValue The defaultValue to set.
		 */
		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		/**
		 * @return Returns the nullable.
		 */
		public boolean isNullable() {
			return nullable;
		}

		/**
		 * @param nullable The nullable to set.
		 */
		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}

		/**
		 * @return Returns the sqlType.
		 */
		public int getSqlType() {
			return sqlType;
		}

		/**
		 * @param sqlType The sqlType to set.
		 */
		public void setSqlType(int sqlType) {
			this.sqlType = sqlType;
		}

		/**
		 * @return Returns the sqlTypeName.
		 */
		public String getSqlTypeName() {
			return sqlTypeName;
		}

		/**
		 * @param sqlTypeName The sqlTypeName to set.
		 */
		public void setSqlTypeName(String sqlTypeName) {
			this.sqlTypeName = sqlTypeName;
		}

		/**
		 * @return Returns the tableName.
		 */
		public String getTableName() {
			return tableName;
		}

		/**
		 * @param tableName The tableName to set.
		 */
		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		/**
		 * Check whether the column data given in the resultset is sufficient for this column definition
		 * @param databaseName database product name
		 * @param column resultset holding the column data
		 * @return true when the check is passed, false if not
		 * @throws SQLException
		 */
		public boolean checkColumn(String databaseName, ResultSet column) throws SQLException {
			boolean checkPassed = true;
			boolean tempCheck = true;
			SQLDatatype datatype = DatatypeHelper.getDBSpecificSQLDatatype(databaseName, sqlTypeName);

			// check table name
			tempCheck = MiscUtils.objectsEqual(tableName, column.getString("TABLE_NAME"), false);
			if (!tempCheck) {
				logger.warn("table name check for " + columnName + " failed: expected {" + tableName + "} but was {" + column.getString("TABLE_NAME") + "}");
			}
			checkPassed &= tempCheck;

			// check column name
			tempCheck = MiscUtils.objectsEqual(columnName, column.getString("COLUMN_NAME"), false);
			if (!tempCheck) {
				logger.warn("column name check for " + columnName + " failed: expected {" + columnName + "} but was {" + column.getString("COLUMN_NAME") + "}");
			}
			checkPassed &= tempCheck;

			// check datatype
			tempCheck = datatype != null ? datatype.matchesType(column.getInt("DATA_TYPE")) : sqlType == column.getInt("DATA_TYPE");
			if (!tempCheck) {
				logger.warn(
						"datatype check for " + columnName + " failed: expected {" + (datatype != null ? datatype.getSqlType() : sqlType) + "} but was {"
						+ column.getInt("DATA_TYPE") + "}");
			}
			checkPassed &= tempCheck;

			// check for default value
			String def = column.getString("COLUMN_DEF");

			if (def != null) {
				// this is necessary because of mssql
				def = def.replaceAll("\\(", "").replaceAll("\\)", "").trim();
			}
			if ("null".equalsIgnoreCase(def)) {
				def = null;
			}
			tempCheck = MiscUtils.objectsEqual(defaultValue, def, false);
			if (!tempCheck) {
				logger.warn("default value check for " + columnName + " failed: expected {" + defaultValue + "} but was {" + def + "}");
			}
			checkPassed &= tempCheck;

			// check for nullability
			tempCheck = nullable ? column.getInt("NULLABLE") == DatabaseMetaData.columnNullable : column.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
			if (!tempCheck) {
				logger.warn(
						"nullable check for " + columnName + " failed: expected {" + (nullable ? DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls)
						+ "} but was {" + column.getInt("NULLABLE") + "}");
			}
			checkPassed &= tempCheck;

			return checkPassed;
		}

		/**
		 * Return the create statement for the given column 
		 * @param databaseName database product name
		 * @return sql statement to create the column
		 */
		public String getCreateStatement(String databaseName) {
			return getCreateStatement(databaseName, tableName);
		}

		/**
		 * return the create statement for the given column in the given table
		 * @param databaseName database product name
		 * @param tableName table name
		 * @return sql statement to create the column
		 */
		public String getCreateStatement(String databaseName, String tableName) {
			StringBuffer sqlStatement = new StringBuffer();
			SQLDatatype datatype = DatatypeHelper.getDBSpecificSQLDatatype(databaseName, sqlTypeName);
			String defaultPart = ObjectTransformer.getString(DatatypeHelper.getDBSpecificDefaultStatementPart(databaseName, tableName, columnName), "DEFAULT");

			sqlStatement.append("ALTER TABLE ").append(tableName).append(" ADD ").append(columnName);
			sqlStatement.append(" ").append(datatype != null ? datatype.getSqlTypeName() : sqlTypeName).append(" ").append(defaultPart).append(" ").append(
					defaultValue);

			if (!nullable) {
				sqlStatement.append(" NOT NULL");
			}

			return sqlStatement.toString();
		}
        
		/**
		 * Get the sql statements needed to drop the column
		 * @param databaseName database name
		 * @return sql statements
		 */
		public String[] getDropStatements(String databaseName) {
			return getDropStatements(databaseName, tableName);
		}

		/**
		 * Get the sql statements needed to drop the column in the given table
		 * @param databaseName database product name
		 * @param tableName name of the table to drop the column from
		 * @return sql statements to drop the column
		 */
		public String[] getDropStatements(String databaseName, String tableName) {
			return DatatypeHelper.getDBSpecificDropColumnStatements(databaseName, tableName, columnName);
		}

		/**
		 * Empty implementation for column specific test (eventually with
		 * autorepair functionality). This method may be implemented to perform
		 * specific column tests. This method should use the given logger to log
		 * messages. When overwriting this method, do not call super()
		 * @param dbHandle database handle
		 * @param handleId datasource handle id
		 * @param autoRepair true when autorepair may be done, false if not
		 * @param logger logger where to write log messages
		 * @return true when the column is correct (or the test is not required for optional columns), false if not
		 * @throws SQLException
		 */
		public boolean doSpecificTest(DBHandle dbHandle, String handleId, boolean autoRepair, NodeLogger logger) throws SQLException {
			if (logger.isDebugEnabled()) {
				logger.debug("No specific test implemented for column {" + tableName + "}.{" + columnName + "}");
			}
			return true;
		}
	}

	/**
	 * Inner class for a table definition
	 */
	public static class TableDefinition {

		/**
		 * table name
		 */
		protected String tableName;

		/**
		 * columns in the table
		 */
		protected ColumnDefinition[] columns;

		/**
		 * whether this table is required
		 */
		protected boolean required;

		/**
		 * Create a table definition
		 * @param tableName name of the table
		 * @param columns columns of the table
		 * @param required whether this table is required
		 */
		public TableDefinition(String tableName, ColumnDefinition[] columns, boolean required) {
			this.tableName = tableName;
			this.columns = columns;
			this.required = required;
		}

		/**
		 * @return Returns the columns.
		 */
		public ColumnDefinition[] getColumns() {
			return columns;
		}

		/**
		 * @return Returns the tableName.
		 */
		public String getTableName() {
			return tableName;
		}
	}

	/**
	 * Generate the logger message with loglevel INFO. Force the message to be
	 * generated by setting the loglevel manually.
	 * @param message message to be output as INFO message
	 */
	public final static synchronized void forceInfoLog(String message) {
		logger.forceInfo(message);
	}

	/**
	 * Returns the metadata cache for DB. always use this method, instead of directly
	 * using {@link #metadataCache} because this method will initialize it if it
	 * wasn't alerady
	 * @return initialized metadata cache.
	 */
	private static PortalCache getMetadataCache() {
		if (metadataCache != null) {
			return metadataCache;
		}
		return createMetadataCache();
	}

	/**
	 * Should only be called by {@link #getMetadataCache()}. 
	 * Creates a new instance of the metedata cache if it wasn't already.
	 * @return meta data cache.
	 */
	private static synchronized PortalCache createMetadataCache() {
		if (metadataCache != null) {
			return metadataCache;
		}
		try {
			metadataCache = PortalCache.getCache(CACHE_METADATACACHEREGION);
		} catch (PortalCacheException e) {
			logger.error("Error while initializing the protal cache for DB metadata, cache region {" + CACHE_METADATACACHEREGION + "}.", e);
		} catch (Exception e) {
			logger.error("Error while initializing the protal cache for DB metadata, cache region {" + CACHE_METADATACACHEREGION + "}.", e);
		}
		return metadataCache;
	}

	/**
	 * Trys to retrieve cached results from the metadata cache.
	 * @param groupName groupname of the cached result
	 * @param cacheKey cache key for the result.
	 * @return cached object or null if not found or an error occurred
	 */
	private static Object getFromMetadataCache(String groupName, Object cacheKey) {
		PortalCache cache = getMetadataCache();

		if (cache != null) {
			try {
				return cache.getFromGroup(groupName, cacheKey);
			} catch (PortalCacheException e) {
				logger.error("Error while trying to retrieve cached object from group {" + groupName + "} using cacheKey {" + cacheKey.toString() + "}", e);
			}
		}
		return null;
	}

	/**
	 * Stores the given result in the emtadata cache using the
	 * given group name.
	 * @param groupName groupName where to cache the result object - e.g. {@link #CACHE_GROUP_TABLECOLUMNS}
	 * @param cacheKey cache key associate with the result
	 * @param result result itself which should be cached.
	 */
	private static void putIntoMetadataCache(String groupName, Object cacheKey, Object result) {
		PortalCache cache = getMetadataCache();

		if (cache != null) {
			try {
				cache.putIntoGroup(groupName, cacheKey, result);
			} catch (PortalCacheException e) {
				logger.error("Error while trying to store object into cache group {" + groupName + "} and cacheKey {" + cacheKey.toString() + "}");
			}
		}
	}

	/**
	 * Creates a cache key for the given table in the handle.
	 * 
	 * @param handle handle of the connection from which the table origins.
	 * @param table table name
	 * @return cache key
	 */
	private static Object getTableColumnCacheKey(DBHandle handle, String table) {
		return new TableColumnCacheKey(handle, table);
	}

	/**
	 * Get the threadlocal transaction map (ensuring that one exists)
	 * @return transaction map
	 */
	private static Map<DBHandle, PoolConnection> getTransactionMap() {
		@SuppressWarnings("unchecked")
		Map<DBHandle, PoolConnection> transactionMap = (Map<DBHandle, PoolConnection>) AttributedThreadGroup.getForCurrentThreadGroup(OPEN_TRANSACTIONS_TG_KEY,
				openTransactions);

		if (transactionMap == null) {
			transactionMap = new ConcurrentHashMap<DBHandle, PoolConnection>();
			AttributedThreadGroup.setForCurrentThreadGroup(OPEN_TRANSACTIONS_TG_KEY, transactionMap, openTransactions);
		}

		return transactionMap;
	}

	/**
	 * Get the currently open transaction for the handle, or null if no currently open transaction
	 * @param handle db handle
	 * @return open transaction or null
	 * @throws SQLException
	 */
	public static PoolConnection getOpenConnection(DBHandle handle) {
		Map<DBHandle, PoolConnection> transactionMap = getTransactionMap();

		return transactionMap.get(handle);
	}

	/**
	 * Clean all still running transactions (commit all)
	 */
	public static void cleanupAllTransactions() {
		Map<DBHandle, PoolConnection> transactionMap = getTransactionMap();

		for (PoolConnection running : transactionMap.values()) {
			try {
				logger.warn("Found a still running transaction, commiting it now");
				running.getConnection().commit();
			} catch (SQLException e) {
				logger.error("Error while commiting transaction", e);
			}
		}

		// clear the transaction map
		transactionMap.clear();
	}

	/**
	 * Start a new transaction for the given handle
	 * @param handle db handle
	 * @throws SQLException
	 */
	public static void startTransaction(DBHandle handle) throws SQLException {
		startTransaction(handle, false);
	}

	/**
	 * Start a new transaction for the given handle
	 * @param handle db handle
	 * @param reuseOpen true to reuse an open connection if one found
	 * @throws SQLException
	 */
	public static void startTransaction(DBHandle handle, boolean reuseOpen) throws SQLException {
		Map<DBHandle, PoolConnection> transactionMap = getTransactionMap();
		PoolConnection running = transactionMap.get(handle);
		boolean debug = logger.isDebugEnabled();
		long time1 = 0;

		if (debug) {
			time1 = System.currentTimeMillis();
		}

		if (running != null) {
			try {
				running.getConnection().commit();
			} catch (SQLException e) {
				safeRelease(handle, running);
				throw e;
			} finally {
				transactionMap.remove(handle);
			}
		}

		PoolConnection c = null;
		try {
			if (reuseOpen && running != null) {
				c = running;
				if (debug) {
					long now = System.currentTimeMillis();
					logger.debug("start (reusing): (" + (now - time1) + ")");
				}

			} else {
				c = getPoolConnection(handle);
			}
			c.getConnection().setAutoCommit(false);
			transactionMap.put(handle, c);
		} catch (SQLException e) {
			if (c != null) {
				safeRelease(handle, c);
			}
			throw e;
		}

		if (debug) {
			long now = System.currentTimeMillis();
			logger.debug("start: " + (now - time1));
		}
	}

	/**
	 * get the jdbc connection of the currently open transaction of the handle,
	 * or null if no transaction is running (or not a jdbc connection).
	 *  
	 * Only use this function if you really need direct access to the jdbc connection!
	 * To commit/rollback/.. this connection, use the functions of this DB class. 
	 * 
	 * @param handle the db handle
	 * @return the currently open connection or null if not open
	 */
	public static Connection getTransactionConnection(DBHandle handle) {
		
		PoolConnection running = getTransactionMap().get(handle);

		if (running != null) {
			return running.getConnection();
		}
		
		return null;
	}

	/**
	 * Commit the currently running connection of the given handle and close the connection
	 * @param handle db handle
	 * @throws SQLException
	 */
	public static void commitTransaction(DBHandle handle) throws SQLException {
		commitTransaction(handle, true);
	}

	/**
	 * Commit the currently running connection of the given handle
	 * @param handle db handle
	 * @param close true to also close the connection, false to leave it open
	 * @throws SQLException
	 */
	public static void commitTransaction(DBHandle handle, boolean close) throws SQLException {
		Map<DBHandle, PoolConnection> transactionMap = getTransactionMap();
		PoolConnection running = transactionMap.get(handle);
		boolean debug = logger.isDebugEnabled();
		long time1 = 0, time2 = 0;

		if (debug) {
			time1 = System.currentTimeMillis();
		}

		if (running != null) {
			try {
				// commit the transaction
				running.getConnection().commit();

				if (debug) {
					time2 = System.currentTimeMillis();
				}

				running.onCommit();

				if (debug) {
					long now = System.currentTimeMillis();
					logger.debug("commit: (" + (now - time1) + ")=(" + (time2 - time1) + "/" + (now - time2) + ")");
				}
			} finally {
				if (close) {
					// remove the running transaction
					safeCloseTransaction(handle, running, transactionMap);
				}
			}
		}
	}

	/**
	 * Rollback the currently running connection of the given handle and close the connection
	 * @param handle db handle
	 * @throws SQLException
	 */
	public static void rollbackTransaction(DBHandle handle) throws SQLException {
		rollbackTransaction(handle, true);
	}

	/**
	 * Rollback the currently running connection of the given handle
	 * @param handle db handle
	 * @param close true to also close the connection, false to leave it open
	 * @throws SQLException
	 */
	public static void rollbackTransaction(DBHandle handle, boolean close) throws SQLException {
		// rollback the transaction
		Map<DBHandle, PoolConnection> transactionMap = getTransactionMap();
		PoolConnection running = transactionMap.get(handle);

		if (running != null) {
			try {
				// rollback the transaction
				running.getConnection().rollback();
				running.onRollback();
			} finally {
				if (close) {
					// remove the running transaction
					safeCloseTransaction(handle, running, transactionMap);
				}
			}
		}
	}

	/**
	 * Safely close the running transaction and remove it from the transaction map
	 * @param handle db handle
	 * @param connection open connection
	 * @param transactionMap transaction map
	 */
	protected static void safeCloseTransaction(DBHandle handle, PoolConnection connection,
			Map<DBHandle, PoolConnection> transactionMap) {
		try {
			connection.getConnection().close();
		} catch (SQLException e) {}
		transactionMap.remove(handle);
	}

	/**
	 * Helper function to close the given statement (if not null) and ignore any
	 * thrown exceptions
	 * @param st statement to be closed
	 */
	public static void close(Statement st) {
		if (st != null) {
			try {
				st.close();
			} catch (SQLException ignored) {}
		}
	}

	/**
	 * Helper function to close the given resultset (if not null) and ignore any
	 * thrown exceptions
	 * @param res resultset to be closed
	 */
	public static void close(ResultSet res) {
		if (res != null) {
			try {
				res.close();
			} catch (SQLException ignored) {}
		}
	}

	/**
	 * Cache key for a Table Column
	 */
	private static class TableColumnCacheKey implements Serializable {
		private static final long serialVersionUID = 1L;
        
		/**
		 * handle for the db connection.
		 */
		private DBHandle handle;

		/**
		 * table name.
		 */
		private String table;

		public TableColumnCacheKey(DBHandle handle, String table) {
			this.handle = handle;
			this.table = table;
		}
        
		public boolean equals(Object obj) {
			if (!(obj instanceof TableColumnCacheKey)) {
				return false;
			}
			TableColumnCacheKey key = (TableColumnCacheKey) obj;

			return key.handle.equals(this.handle) && key.table.equals(this.table);
		}
        
		public int hashCode() {
			return this.handle.hashCode() ^ this.table.hashCode();
		}
	}

	/**
	 * Clears the "cache" for table exists and table field exists statements.
	 */
	public static void clearTableFieldCache() {
		synchronized (tableFieldExistStorePerHandle) {
			tableFieldExistStorePerHandle.clear();
		}
	}

	/**
	 * Schedule the given file to be deleted on transaction commit.
	 * If no transaction was started, the file is immediately deleted.
	 * Deletion of files is made "access safe". That means that if a file cannot be deleted, because another
	 * thread still has a file handle open, the file is queued and deletion is tried later
	 * @param file to be deleted
	 */
	public static void removeFileOnCommit(DBHandle handle, File file) {
		PoolConnection connection = getOpenConnection(handle);

		if (connection != null) {
			connection.removeFileOnCommit(file);
		} else {
			// no open connection found, so remove the file immediately
			FileRemover.removeFile(file);
		}
	}

	/**
	 * Schedule the given file to be deleted on transaction rollback.
	 * If no transaction was started, the call is ignored.
	 * Deletion of files is made "access safe". That means that if a file cannot be deleted, because another
	 * thread still has a file handle open, the file is queued and deletion is tried later
	 * @param file to be deleted
	 */
	public static void removeFileOnRollback(DBHandle handle, File file) {
		PoolConnection connection = getOpenConnection(handle);

		if (connection != null) {
			connection.removeFileOnRollback(file);
		}
	}

	/**
	 * Get the current handle of this thread or null if none set
	 * @return current handle or null
	 */
	public static DBHandle getCurrentHandle() {
		return currentHandle.get();
	}

	/**
	 * Internal helper class for cached column metadata
	 */
	protected static class ColumnData implements Serializable {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -496843862829867082L;

		/**
		 * List of column names
		 */
		protected List<String> names;

		/**
		 * List of column types
		 */
		protected List<Integer> types;

		/**
		 * Create an instance holding the column data for the given table in the given handle
		 * @param handle db handle
		 * @param table table name
		 * @throws SQLException
		 */
		public ColumnData(DBHandle handle, String table) throws SQLException {
			names = new Vector<String>();
			types = new Vector<Integer>();

			try (DBTrx trx = new DBTrx(handle)) {
				if (trx.getConnection().getMetaData().storesUpperCaseIdentifiers()
						&& "true".equals(System.getProperty("com.gentics.portalnode.datasource.consideruppercase"))) {
					table = table.toUpperCase();
				}
				try (ResultSet rs = trx.getConnection().getMetaData().getColumns(null, handle.getDbSchema(), table, null)) {
					while (rs.next()) {
						names.add(rs.getString("COLUMN_NAME"));
						types.add(rs.getInt("DATA_TYPE"));
					}
				}

				trx.success();
			} catch (SQLException e) {
				logger.error(e);
				throw e;
			} catch (Exception e) {
				throw new SQLException(e.getClass().getName() + ": " + e.getMessage() + " (see log for details)");
			}
		}

		/**
		 * Get the list of column names
		 * @return list of column names
		 */
		public List<String> getNames() {
			return names;
		}

		/**
		 * Get the list of column types
		 * @return list of column types
		 */
		public List<Integer> getTypes() {
			return types;
		}
	}

	/**
	 * {@link AutoCloseable} implementation that gets a {@link PoolConnection} instance, and releases it in {@link DBTrx#close()}.
	 */
	public static class DBTrx implements AutoCloseable {
		/**
		 * {@link DBHandle} instance
		 */
		protected DBHandle handle;

		/**
		 * {@link PoolConnection} instance
		 */
		protected PoolConnection c;

		/**
		 * Flag whether the {@link PoolConnection} shall be released in {@link #close()}
		 */
		protected boolean release = true;

		/**
		 * Flag whether a running transaction is used
		 */
		protected boolean transaction = false;

		/**
		 * True if errors shall be logged
		 */
		protected boolean logErrors = true;

		/**
		 * Flag that will be set upon call to {@link #success()}.
		 * If set, the current transaction will be committed in {@link #close()} (before being released)
		 */
		protected boolean success = false;

		/**
		 * Create an instance with the given handle, errors will be logged
		 * @param handle handle
		 * @throws SQLException
		 */
		public DBTrx(DBHandle handle) throws SQLException {
			this(handle, true);
		}

		/**
		 * Create an instance with the given handle
		 * @param handle handle
		 * @param logErrors true to log errors
		 * @throws SQLException
		 */
		public DBTrx(DBHandle handle, boolean logErrors) throws SQLException {
			this.handle = handle;
			this.logErrors = logErrors;
			if (handle != null) {
				// first check for running transactions
				transaction = false;
				release = true;
				c = getOpenConnection(handle);

				if (c != null) {
					// found an open transaction (will not release the connection)
					transaction = true;
					release = false;
				} else {
					c = DB.getPoolConnection(handle);
					getTransactionMap().put(handle, c);
				}
				// if the {@link PoolConnection} instance contains a
				// connection with a running transaction, we will not set the autocommit flag
				transaction |= c.isRunningTransaction();

				try {
					if (!transaction) {
						getConnection().setAutoCommit(DB.m_autoCommit);
					}
				} catch (Exception e) {
					if (logErrors) {
						logger.error("Error while starting transaction", e);
					}
					close();
					throw e;
				}
			} else {
				throw new IllegalArgumentException("DB::query: Handle is null");
			}
		}

		@Override
		public void close() {
			if (release && c != null) {
				if (success && !transaction && !DB.m_autoCommit) {
					try {
						getConnection().commit();
					} catch (Exception e) {
						if (logErrors) {
							logger.error("Error while commiting transaction", e);
						}
					}
				}

				if (!success && !transaction && !DB.m_autoCommit) {
					try {
						getConnection().rollback();
					} catch (Exception e) {
						if (logErrors) {
							logger.error("Error while rolling back transaction", e);
						}
					}
				}

				try {
					safeRelease(handle, c);
				} catch (Exception e) {
					if (logErrors) {
						logger.error("Error while releasing connection", e);
					}
				}
				getTransactionMap().remove(handle);
				c = null;
			}
		}

		/**
		 * Set the success flag
		 */
		public void success() {
			success = true;
		}

		/**
		 * Get the connection
		 * @return connection
		 */
		public Connection getConnection() {
			return c.getConnection();
		}
	}
}
