/*
 * @author herbert
 * @date 05.04.2007
 * @version $Id: DBUtils.java,v 1.9 2010-09-28 17:01:28 norbert Exp $
 */
package com.gentics.contentnode.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.lib.db.CountingExecutor;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

public final class DBUtils {
	/**
	 * Select handler that returns the ids as set of integers
	 */
	public final static HandleSelectResultSet<Set<Integer>> IDS = rs -> {
		Set<Integer> idSet = new HashSet<>();
		while (rs.next()) {
			idSet.add(rs.getInt("id"));
		}
		return idSet;
	};

	/**
	 * Select handler that returns the ids as list of integers
	 */
	public final static HandleSelectResultSet<List<Integer>> IDLIST = rs -> {
		List<Integer> idList = new ArrayList<>();
		while (rs.next()) {
			idList.add(rs.getInt("id"));
		}
		return idList;
	};

	private static NodeLogger logger = NodeLogger.getNodeLogger(DBUtils.class);

	/**
	 * the maximum number of binds in one query for "mass" statements
	 * @see #executeMassStatement(String, List, int, SQLExecutor)
	 */
	private static final int MASS_STATEMENT_MAX = 100;

	private DBUtils() {}

	/**
	 * Executes an sql update statment. Access to handle results and statement preperation is possible by the given SQLExecutor.
	 * When using executeUpdateStament instead of executeStatement you have the possibility to access stmt.generatedKeys() in handleStatement() of the SQLExecutor.
	 * @param sql SQL statement to execute
	 * @param ex  Executor to handle results and prepare the statment
	 * @return true if no error occured
	 * @throws NodeException
	 */
	public static boolean executeUpdateStatement(String sql, SQLExecutor ex) throws NodeException {
		return executeStatement(sql, ex, Transaction.UPDATE_STATEMENT);
	}

	/**
	 * Executes an sql statment. Access to handle results and statement preperation is possible by the given SQLExecutor.
	 * @param sql SQL statment to execute
	 * @param ex SQL Executor to handle results and prepare the statment
	 * @return true if no error occured
	 * @throws NodeException
	 */
	public static boolean executeStatement(String sql, SQLExecutor ex) throws NodeException {
		return executeStatement(sql, ex, Transaction.SELECT_STATEMENT);
	}

	public static boolean executeStatement(String sql, SQLExecutor ex, int type) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;

		try {
			stmt = t.prepareStatement(sql, type);

			if (ex != null) {
				ex.prepareStatement(stmt);
			}

			if (stmt.execute()) {
				if (ex != null) {
					ex.handleResultSet(stmt.getResultSet());
				}
			} else {
				stmt.getUpdateCount();
			}

			if (ex != null) {
				ex.handleStatment(stmt);
			}

			return true;
		} catch (SQLException e) {
			throw new NodeException("Error while executing sql statement {" + sql + "}", e);
		} finally {
			if (stmt != null) {
				try {
					t.closeResultSet(stmt.getResultSet());
				} catch (SQLException e) {
					logger.error("Error while closing resultset", e);
				}
			}
			t.closeStatement(stmt);
		}
	}

	/**
	 * Execute the given statement and return the update count (for update statements)
	 * @param sql SQL to execute
	 * @param type type of the statement
	 * @return update count or 0 if not an update statement)
	 * @throws NodeException
	 */
	public static int executeStatement(String sql, int type) throws NodeException {
		return executeStatement(sql, type, null, null, null);
	}

	/**
	 * Execute the given statement and return the update count (for update statements)
	 * @param sql SQL to execute
	 * @param type type of the statement
	 * @param prepare optional prepare code (e.g. for setting parameters)
	 * @return update count or 0 if not an update statement)
	 * @throws NodeException
	 */
	public static int executeStatement(String sql, int type, PrepareStatement prepare) throws NodeException {
		return executeStatement(sql, type, prepare, null, null);
	}

	/**
	 * Execute the given statement and return the update count (for update statements)
	 * @param sql SQL to execute
	 * @param type type of the statement
	 * @param prepare optional prepare code (e.g. for setting parameters)
	 * @param result optional result handler
	 * @return update count or 0 if not an update statement)
	 * @throws NodeException
	 */
	public static int executeStatement(String sql, int type, PrepareStatement prepare, HandleResultSet result) throws NodeException {
		return executeStatement(sql, type, prepare, result, null);
	}

	/**
	 * Execute the given statement and return the update count (for update statements)
	 * @param sql SQL to execute
	 * @param type type of the statement
	 * @param prepare optional prepare code (e.g. for setting parameters)
	 * @param result optional result handler
	 * @param handle optional handler to acquire data from statement after execution
	 * @return update count or 0 if not an update statement)
	 * @throws NodeException
	 */
	public static int executeStatement(String sql, int type, PrepareStatement prepare, HandleResultSet result, HandleStatement handle) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;
		int returnValue = 0;

		try {
			stmt = t.prepareStatement(sql, type);

			if (prepare != null) {
				prepare.prepare(stmt);
			}

			if (stmt.execute()) {
				if (result != null) {
					result.handle(stmt.getResultSet());
				}
			} else {
				returnValue = stmt.getUpdateCount();
			}

			if (handle != null) {
				handle.handle(stmt);
			}

			return returnValue;
		} catch (SQLException e) {
			throw new NodeException("Error while executing sql statement {" + sql + "}", e);
		} finally {
			if (stmt != null) {
				try {
					t.closeResultSet(stmt.getResultSet());
				} catch (SQLException e) {
					logger.error("Error while closing resultset", e);
				}
			}
			t.closeStatement(stmt);
		}
	}

	/**
	 * Execute the select statement and return the result from the given result handler
	 * @param sql sql statement
	 * @param result result handler (may be null)
	 * @return result from the handler or null if no handler given
	 * @throws NodeException
	 */
	public static <R> R select(String sql, HandleSelectResultSet<R> result) throws NodeException {
		return select(sql, null, result);
	}

	/**
	 * Execute the select statement which may be prepared with the given prepare handler and return the result from the result handler
	 * @param sql sql statement
	 * @param prepare prepare handler (may be null)
	 * @param result result handler (may be null)
	 * @return result from the result handler or null if no handler given
	 * @throws NodeException
	 */
	public static <R> R select(String sql, PrepareStatement prepare, HandleSelectResultSet<R> result) throws NodeException {
		return select(sql, prepare, result, Transaction.SELECT_STATEMENT);
	}

	/**
	 * Execute the select statement which may be prepared with the given prepare handler and return the result from the result handler
	 * @param sql sql statement
	 * @param prepare prepare handler (may be null)
	 * @param result result handler (may be null)
	 * @param type statement type
	 * @return result from the result handler or null if no handler given
	 * @throws NodeException
	 */
	public static <R> R select(String sql, PrepareStatement prepare, HandleSelectResultSet<R> result, int type) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;
		ResultSet res = null;
		R retVal = null;

		try {
			stmt = t.prepareStatement(sql, type);

			if (prepare != null) {
				prepare.prepare(stmt);
			}

			res = stmt.executeQuery();

			if (result != null) {
				retVal = result.handle(res);
			}

			return retVal;
		} catch (SQLException e) {
			throw new NodeException("Error while executing sql statement {" + sql + "}", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(stmt);
		}
	}

	/**
	 * Execute the given sql statement in an "updatable" fashion, so that the given handler can update records, delete records or insert records
	 * @param sql sql statement
	 * @param prepare prepare handler
	 * @param handler resultset handler for updating
	 * @throws NodeException
	 */
	public static void selectAndUpdate(String sql, PrepareStatement prepare, HandleResultSet handler) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;
		ResultSet res = null;

		try {
			stmt = t.prepareSelectForUpdate(sql);

			if (prepare != null) {
				prepare.prepare(stmt);
			}

			res = stmt.executeQuery();

			if (handler != null) {
				handler.handle(res);
			}
		} catch (SQLException e) {
			throw new NodeException("Error while executing sql statement {" + sql + "} for update", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(stmt);
		}
	}

	/**
	 * Execute the given insert statement and return the generated keys
	 * @param sql sql statement
	 * @param args arguments to be inserted into the sql statement
	 * @return generated keys
	 * @throws NodeException
	 */
	public static List<Integer> executeInsert(String sql, Object[] args) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;
		ResultSet keys = null;

		try {
			stmt = t.prepareInsertStatement(sql);

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					stmt.setObject(i + 1, args[i]);
				}
			}
			stmt.execute();
			keys = stmt.getGeneratedKeys();

			List<Integer> keysList = new Vector<Integer>();

			while (keys.next()) {
				keysList.add(keys.getInt(1));
			}

			return keysList;
		} catch (SQLException e) {
			throw new NodeException("Error while performing insert statement {" + sql + "}", e);
		} finally {
			t.closeResultSet(keys);
			t.closeStatement(stmt);
		}
	}

	/**
	 * executes an update and returns the update count.
	 * @param sql
	 * @param args
	 * @return the update count.
	 */
	public static int executeUpdate(String sql, Object[] args) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;

		try {
			stmt = t.prepareStatement(sql, Transaction.UPDATE_STATEMENT);

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					stmt.setObject(i + 1, args[i]);
				}
			}

			stmt.execute();

			return stmt.getUpdateCount();
		} catch (SQLException e) {
			throw new NodeException("Error while executing sql statement {" + sql + "}", e);
		} finally {
			t.closeStatement(stmt);
		}
	}

	/**
	 * Execute an update statement and return the update count
	 * @param sql sql statement
	 * @param args optional list of arguments
	 * @return update count
	 * @throws NodeException
	 */
	public static int update(String sql, Object... args) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;

		try {
			stmt = t.prepareStatement(sql, Transaction.UPDATE_STATEMENT);

			for (int i = 0; i < args.length; i++) {
				stmt.setObject(i + 1, args[i]);
			}
			stmt.execute();

			return stmt.getUpdateCount();
		} catch (SQLException e) {
			throw new NodeException("Error while executing sql statement {" + sql + "}", e);
		} finally {
			t.closeStatement(stmt);
		}
	}

	/**
	 * Execute a batch update
	 * @param sql sql of the statement
	 * @param argsList list of arguments
	 * @return update counts
	 * @throws NodeException
	 */
	public static int[] executeBatchUpdate(String sql, List<Object[]> argsList) throws NodeException {
		return executeBatchStatement(sql, Transaction.UPDATE_STATEMENT, argsList, null);
	}

	/**
	 * Execute a batch insert
	 * @param sql sql of the statement
	 * @param argsList list of arguments
	 * @return update counts
	 * @throws NodeException
	 */
	public static int[] executeBatchInsert(String sql, List<Object[]> argsList) throws NodeException {
		return executeBatchStatement(sql, Transaction.INSERT_STATEMENT, argsList, null);
	}

	/**
	 * Execute a batch update (or insert)
	 * @param sql sql of the statement
	 * @param type statement type
	 * @param argsList list of arguments
	 * @param handle optional statement handler
	 * @return update counts
	 * @throws NodeException
	 */
	public static int[] executeBatchStatement(String sql, int type, List<Object[]> argsList, HandleStatement handle) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;

		try {
			stmt = t.prepareStatement(sql, type);

			for (Object[] args : argsList) {
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						stmt.setObject(i + 1, args[i]);
					}
				}
				stmt.addBatch();
			}

			int[] updateCounts = stmt.executeBatch();

			if (handle != null) {
				handle.handle(stmt);
			}

			return updateCounts;
		} catch (SQLException e) {
			throw new NodeException("Error while executing sql statement {" + sql + "}", e);
		} finally {
			t.closeStatement(stmt);
		}
	}

	/**
	 * Execute an sql statements for a given collection of objects as batch
	 * @param sql statement
	 * @param type statement type
	 * @param objects collection of objects
	 * @param argsExtractor function, that extracts the statement parameters from the objects
	 * @param handle optional statement handler
	 * @return update counts
	 * @throws NodeException
	 */
	public static <T> int[] executeBatch(String sql, int type, Collection<T> objects, Function<T, Object[]> argsExtractor, HandleStatement handle)
			throws NodeException {
		List<Object[]> argsList = new ArrayList<>();
		for (T object : objects) {
			argsList.add(argsExtractor.apply(object));
		}
		return executeBatchStatement(sql, type, argsList, handle);
	}

	/**
	 * Helper method to select entries from a database table and delete the returned elements by id.
	 * This can be used if you would have a big amount of ids in the where clause which causes mysql to lock the complete table.
	 *
	 * @param deleteTable The table from which the selected ids will be removed.
	 * @param select The select statment used to select the objects for deletion. Must end with "IN" and select a column named "id".
	 * @param ids list with ids used for the select statement
	 */
	public static void selectAndDelete(String deleteTable, String select, List ids) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		selectAndDelete(deleteTable, select, ids, t);
	}

	public static void selectAndDelete(final String deleteTable, String select, List ids, final PreparedStatementHandler stmth) throws NodeException {
		DBUtils.executeMassStatement(select, null, ids, 1, new SQLExecutor() {
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				Vector toDelete = new Vector();

				while (rs.next()) {
					toDelete.add(new Integer(rs.getInt("id")));
				}
				if (!toDelete.isEmpty()) {
					DBUtils.executeMassStatement("DELETE FROM " + deleteTable + " WHERE id IN", null, toDelete, 1, null, Transaction.DELETE_STATEMENT, stmth);
				}
			}
		}, Transaction.SELECT_STATEMENT, stmth);
	}

	/**
	 * a generic way to create a "mass" statement.. ie. something like
	 * WHERE blah IN (?,?,?,?) ...
	 * if there are too many items in 'list' it will be split up and more than one query will be used
	 * - the max amount of binds is defined in {@link #MASS_STATEMENT_MAX}.
	 *
	 * @param sql the sql statement, the (?,?,?) will be appended to this string !
	 * @param list a list containing the values.
	 * @param startIndex the start index of the first parameter to be appended to the statement.
	 * @param ex
	 * @throws NodeException
	 */
	public static boolean executeMassStatement(String sql, List list, int startIndex, SQLExecutor ex) throws NodeException {
		return executeMassStatement(sql, null, list, startIndex, ex, Transaction.SELECT_STATEMENT);
	}

	/**
	 * a generic way to create a "mass" statement.. ie. something like
	 * WHERE blah IN (?,?,?,?) ...
	 * if there are too many items in 'list' it will be split up and more than one query will be used
	 * - the max amount of binds is defined in {@link #MASS_STATEMENT_MAX}.
	 *
	 * @param sql the sql statement, the (?,?,?) will be appended to this string !
	 * @param suffixSql this sql part will be appended after the (?,?,?)
	 * @param list a list containing the values.
	 * @param startIndex the start index of the first parameter to be appended to the statement.
	 * @param ex
	 * @throws NodeException
	 */
	public static boolean executeMassStatement(String sql, String suffixSql, List list, int startIndex, SQLExecutor ex) throws NodeException {
		return executeMassStatement(sql, suffixSql, list, startIndex, ex, Transaction.SELECT_STATEMENT);
	}

	public static boolean executeMassStatement(String sql, String suffixSql, List list, int startIndex, SQLExecutor ex, int type) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		return executeMassStatement(sql, suffixSql, list, startIndex, ex, type, t);
	}

	public static boolean executeMassStatement(String sql, String suffixSql, List list, int startIndex, SQLExecutor ex, int type, PreparedStatementHandler stmth) throws NodeException {
		PreparedStatement stmt = null;

		try {
			int maxbinds = MASS_STATEMENT_MAX;
			int totalsize = list.size();

			Iterator listiterator = list.iterator();

			for (int current = 0; current < totalsize && listiterator.hasNext();) {
				int bindcount;

				if (current + maxbinds > totalsize) {
					bindcount = totalsize - current;
				} else {
					bindcount = maxbinds;
				}

				StringBuffer newsql = new StringBuffer();

				newsql.append(sql + " (" + StringUtils.repeat("?", bindcount, ",") + ")");
				if (suffixSql != null) {
					newsql.append(" " + suffixSql);
				}
				logger.debug("Running SQL: {" + newsql.toString() + "}");

				// if a statement was created before (in the previous iteration), we close it now and set it to null
				if (stmt != null) {
					stmth.closeStatement(stmt);
					stmt = null;
				}
				stmt = stmth.prepareStatement(newsql.toString(), type);

				if (ex != null) {
					ex.prepareStatement(stmt);
				}
				for (int i = 0; i < bindcount && listiterator.hasNext(); i++) {
					stmt.setObject(startIndex + i, listiterator.next());
				}

				if (stmt.execute()) {
					if (ex != null) {
						ex.handleResultSet(stmt.getResultSet());
					}
				} else {
					if (ex != null) {
						ex.handleUpdateCount(stmt.getUpdateCount());
					}
				}

				if (ex != null) {
					ex.handleStatment(stmt);
				}

				current += bindcount;
			}

			return true;
		} catch (SQLException e) {
			throw new NodeException("Error while executing sql statemtent {" + sql + "}", e);
		} finally {
			if (stmt != null) {
				try {
					stmth.closeResultSet(stmt.getResultSet());
				} catch (SQLException e) {
					logger.error("Error while closing resultset", e);
				}
			}
			stmth.closeStatement(stmt);
		}
	}

	/**
	 * Create a condition with a placeholder String for prepared statement for a SQL
	 * tuple or a single space if the tuple is empty (MySQL doesn't like empty tuples)
	 * @param num number of elements of the tuple
	 * @param startOfCondition a string prefix
	 * @return place holder string
	 */
	public static String makeSqlPlaceHolders(int num, String startOfCondition) {
		if (num == 0) {
			return " ";
		}
		StringBuilder result = new StringBuilder(startOfCondition);

		result.append(" (");
		for (int i = 0; i < num; ++i) {
			if (i > 0) {
				result.append(", ");
			}
			result.append('?');
		}
		result.append(')');
		return result.toString();
	}

	/**
	 * Unwrap lists found in object arrays to be able to use SQL tuples
	 * in prepared statements. Use together with makeSqlPlaceHolders.
	 * Unwrapping is not applied recursively
	 * @params the list of objects to unwrap
	 * @return list of objects with the lists unwrapped
	 */
	public static Object[] unwrapLists(Object[] params) {
		List<Object> result = new ArrayList<Object>();

		for (Object o : params) {
			if (o instanceof List) {
				List<?> l = (List<?>) o;

				for (Object o2 : l) {
					result.add(o2);
				}
			} else {
				result.add(o);
			}
		}
		return result.toArray();
	}

	/**
	 * Perform an update statement with a specific where clause, by first selecting the primary keys and then doing the update on them
	 * @param tablename name of the table
	 * @param primaryKeyColumn name of the primary key column
	 * @param sqlSet SQL statement to set something (without the keyword SET)
	 * @param setParams parameters to be inserted into sqlSet
	 * @param sqlWhere SQL Statement to select something (without the keyword WHERE)
	 * @param whereParams parameters to be inserted into sqlWhere
	 * @throws NodeException
	 */
	public static void updateWithPK(String tablename, final String primaryKeyColumn, String sqlSet, final Object[] setParams, String sqlWhere,
			final Object[] whereParams) throws NodeException {
		// first make a select statement (selecting the primary keys)
		StringBuffer selectSQL = new StringBuffer();

		selectSQL.append("SELECT ").append(primaryKeyColumn).append(" FROM ").append(tablename).append(" WHERE ").append(sqlWhere);

		// execute the select statement and collect the ids
		final List<Integer> ids = new ArrayList<Integer>();

		executeStatement(selectSQL.toString(), new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int pCounter = 1;

				if (whereParams != null) {
					for (Object param : whereParams) {
						stmt.setObject(pCounter++, param);
					}
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					ids.add(rs.getInt(primaryKeyColumn));
				}
			}
		});

		// only proceed if actually selected something
		if (!ids.isEmpty()) {
			StringBuffer updateSQL = new StringBuffer();

			updateSQL.append("UPDATE ").append(tablename).append(" SET ").append(sqlSet).append(" WHERE ").append(primaryKeyColumn).append(" IN (").append(StringUtils.repeat("?", ids.size(), ",")).append(
					")");
			executeUpdateStatement(updateSQL.toString(), new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					int pCounter = 1;

					if (setParams != null) {
						for (Object param : setParams) {
							stmt.setObject(pCounter++, param);
						}
					}
					for (Integer id : ids) {
						stmt.setInt(pCounter++, id);
					}
				}
			});
		}
	}

	/**
	 * Perform delete statement with a specific where clause, by first selecting the primary keys and then deleting them
	 * @param tablename name of the table
	 * @param primaryKeyColumn name of the primary key column
	 * @param sqlWhere SQL Statement to select something (without the keyword WHERE)
	 * @param whereParams parameters to be inserted into sqlWhere
	 * @return update count
	 * @throws NodeException
	 */
	public static int deleteWithPK(String tablename, final String primaryKeyColumn, String sqlWhere, final Object[] whereParams) throws NodeException {
		// first make a select statement (selecting the primary keys)
		StringBuffer selectSQL = new StringBuffer();

		selectSQL.append("SELECT ").append(primaryKeyColumn).append(" FROM ").append(tablename).append(" WHERE ").append(sqlWhere);

		// execute the select statement and collect the ids
		List<Integer> ids = select(selectSQL.toString(), ps -> {
			int pCounter = 1;

			for (Object param : whereParams) {
				ps.setObject(pCounter++, param);
			}
		}, allInt(primaryKeyColumn));

		// only proceed if actually selected something
		if (!ids.isEmpty()) {
			StringBuffer deleteSQL = new StringBuffer();

			deleteSQL.append("DELETE FROM ").append(tablename).append(" WHERE ").append(primaryKeyColumn).append(" IN (").append(StringUtils.repeat("?", ids.size(), ",")).append(
					")");

			return update(deleteSQL.toString(), ids.toArray());
		} else {
			return 0;
		}
	}

	/**
	 * Update or insert a row in the given table
	 * A SELECT statement on the identification keys is executed to determine
	 * if an UPDATE or an INSERT should be done.
	 * @param tableName table name
	 * @param identification map of column values that identify the row to update
	 * @param data map of column values that should be set
	 * @throws NodeException
	 */
	public static void updateOrInsert(String tableName, Map<String, Object> identification, Map<String, Object> data) throws NodeException {
		if (ObjectTransformer.isEmpty(identification)) {
			throw new NodeException("Cannot do updateOrInsert without identification entries");
		}
		if (ObjectTransformer.isEmpty(data)) {
			throw new NodeException("Cannot do updateOrInsert without data entries");
		}

		StringBuilder selectSQL = new StringBuilder("SELECT ");
		StringBuilder updateSQL = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
		StringBuilder insertSQL = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
		StringBuilder whereSQL  = new StringBuilder(" WHERE ");
		List<Object> params = new ArrayList<Object>(identification.size());
		List<Object> whereParams = new ArrayList<Object>(identification.size());
		boolean first = true;
		for (Map.Entry<String, Object> dataEntry : data.entrySet()) {
			if (first) {
				first = false;
			} else {
				updateSQL.append(", ");
				insertSQL.append(", ");
			}

			updateSQL.append(dataEntry.getKey()).append(" = ?");
			insertSQL.append(dataEntry.getKey());
			params.add(dataEntry.getValue());
		}

		boolean firstId = true;
		for (Map.Entry<String, Object> idEntry : identification.entrySet()) {
			if (firstId) {
				firstId = false;
				// We have to fetch some column in the select statement, so let's use the first.
				selectSQL.append(idEntry.getKey()).append(" FROM ").append(tableName);
			} else {
				whereSQL.append(" AND ");
			}
			whereSQL.append(idEntry.getKey()).append(" = ?");
			insertSQL.append(", ").append(idEntry.getKey());
			params.add(idEntry.getValue());
			whereParams.add(idEntry.getValue());
		}

		selectSQL.append(whereSQL.toString());
		updateSQL.append(whereSQL.toString());
		insertSQL.append(") VALUES (").append(StringUtils.repeat("?", identification.size() + data.size(), ",")).append(")");
		Object[] paramsArray = (Object[]) params.toArray(new Object[params.size()]);

		// Select first, to see if rows exist. This statement is deliberately masked as "UPDATE" statement, to ensure that
		// it is done with the same transaction as the UPDATE/INSERT statements below
		int numRows = executeSelectAndCountRows(selectSQL.toString(), whereParams.toArray(), Transaction.UPDATE_STATEMENT);
		if (numRows > 0) {
			// Rows found, update
			executeUpdate(updateSQL.toString(), paramsArray);
		} else {
			// Row does not exist, insert
			executeInsert(insertSQL.toString(), paramsArray);
		}
	}

	/**
	 * Executes a SQL SELECT statement and counts the returned rows.
	 * @param sql
	 * @param args
	 * @return Number of rows
	 */
	public static int executeSelectAndCountRows(String sql, Object[] args) throws NodeException {
		return executeSelectAndCountRows(sql, args, Transaction.SELECT_STATEMENT);
	}

	/**
	 * Executes a SQL SELECT statement and counts the returned rows.
	 * @param sql
	 * @param args
	 * @param type type of the statement
	 * @return Number of rows
	 */
	public static int executeSelectAndCountRows(String sql, Object[] args, int type) throws NodeException {
		CountingExecutor countingExecutor = new CountingExecutor(args);
		executeStatement(sql, countingExecutor, type);
		return countingExecutor.getRowCount();
	}

	/**
	 * Update a cross table
	 * @param table table name
	 * @param key1 name of key1
	 * @param id1 id1
	 * @param key2 name of key2
	 * @param ids2 collection of id2
	 * @param data map containing additional data for inserted rows
	 * @return true if something was changed
	 * @throws NodeException
	 */
	public static boolean updateCrossTable(String table, String key1, int id1, String key2, Collection<Integer> ids2, Map<String, Object> data) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		boolean updated = false;

		String sql = String.format("SELECT * FROM %s WHERE %s = ?", table, key1);

		Set<Integer> notFoundId2 = new HashSet<>(ids2);
		try {
			stmt = t.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setInt(1, id1);

			rs = stmt.executeQuery();

			while (rs.next()) {
				int id2 = rs.getInt(key2);

				if (!ids2.contains(id2)) {
					rs.deleteRow();
					updated = true;
				} else {
					notFoundId2.remove(id2);
				}
			}

			for (Integer toInsert : notFoundId2) {
				rs.moveToInsertRow();
				rs.updateInt(key1, id1);
				rs.updateInt(key2, toInsert);
				for (Map.Entry<String, Object> entry : data.entrySet()) {
					rs.updateObject(entry.getKey(), entry.getValue());
				}
				rs.insertRow();
				updated = true;
			}

		} catch (SQLException e) {
			throw new NodeException(e);
		} finally {
			t.closeResultSet(rs);
			t.closeStatement(stmt);
		}
		return updated;
	}

	/**
	 * Update a cross table with additional data (columns other than the foreign keys)
	 * @param table table name
	 * @param key1 name of foreign key1
	 * @param id1 foreign key id1
	 * @param key2 name of foreign key2
	 * @param data map containing the data to be inserted/updated (keys are the foreign id2, values are data maps)
	 * @return true if something was changed
	 * @throws NodeException
	 */
	public static boolean updateCrossTable(String table, String key1, int id1, String key2, Map<Integer, Map<String, Object>> data) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		boolean updated = false;

		String sql = String.format("SELECT * FROM %s WHERE %s = ?", table, key1);

		Set<Integer> ids2 = data.keySet();
		Set<Integer> notFoundId2 = new HashSet<>(data.keySet());
		try {
			stmt = t.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setInt(1, id1);

			rs = stmt.executeQuery();

			while (rs.next()) {
				int id2 = rs.getInt(key2);

				if (!ids2.contains(id2)) {
					// remove the row
					rs.deleteRow();
					updated = true;
				} else {
					// update the row, if data need to be changed
					Map<String, Object> rowData = data.get(id2);
					boolean changeRow = false;
					for (Map.Entry<String, Object> entry : rowData.entrySet()) {
						if (!Objects.equals(rs.getObject(entry.getKey()), entry.getValue())) {
							rs.updateObject(entry.getKey(), entry.getValue());
							changeRow = true;
						}
					}
					if (changeRow) {
						rs.updateRow();
						updated = true;
					}

					// we found the row, so remove it from the set "notFound"
					notFoundId2.remove(id2);
				}
			}

			for (Integer toInsert : notFoundId2) {
				rs.moveToInsertRow();
				rs.updateInt(key1, id1);
				rs.updateInt(key2, toInsert);
				Map<String, Object> rowData = data.get(toInsert);
				for (Map.Entry<String, Object> entry : rowData.entrySet()) {
					rs.updateObject(entry.getKey(), entry.getValue());
				}
				rs.insertRow();
				updated = true;
			}

		} catch (SQLException e) {
			throw new NodeException(e);
		} finally {
			t.closeResultSet(rs);
			t.closeStatement(stmt);
		}
		return updated;
	}

	/**
	 * Return resultset handler for select statement that returns the value of the given column as integer from the first returned row, or 0 if no rows were selected
	 * @param columnName column name
	 * @return column value as int
	 */
	public static HandleSelectResultSet<Integer> firstInt(String columnName) {
		return rs -> {
			if (rs.next()) {
				return rs.getInt(columnName);
			} else {
				return 0;
			}
		};
	}

	/**
	 * Return resultset handler for select statement that returns the value of the given column as string from the first returned row, or null if no rows were selected
	 * @param columnName column name
	 * @return column value as String
	 */
	public static HandleSelectResultSet<String> firstString(String columnName) {
		return rs -> {
			if (rs.next()) {
				return rs.getString(columnName);
			} else {
				return null;
			}
		};
	}

	/**
	 * Return resultset handler for select statement that returns the values of the specified column as list of integers
	 * @param columnName column name
	 * @return column values as list of integers
	 */
	public static HandleSelectResultSet<List<Integer>> allInt(String columnName) {
		return rs -> {
			List<Integer> list = new ArrayList<>();
			while (rs.next()) {
				list.add(rs.getInt(columnName));
			}
			return list;
		};
	}

	/**
	 * Return resultset handler for select statement that returns the values of the specified column as list of strings
	 * @param columnName column name
	 * @return column values as list of strings
	 */
	public static HandleSelectResultSet<List<String>> allString(String columnName) {
		return rs -> {
			List<String> list = new ArrayList<>();
			while (rs.next()) {
				list.add(rs.getString(columnName));
			}
			return list;
		};
	}

	/**
	 * Interface for preparing a statement (setting parameters)
	 */
	@FunctionalInterface
	public static interface PrepareStatement {
		void prepare(PreparedStatement stmt) throws SQLException, NodeException;
	}

	/**
	 * Interface for handling the resultset
	 */
	@FunctionalInterface
	public static interface HandleResultSet {
		void handle(ResultSet res) throws SQLException, NodeException;
	}

	/**
	 * Interface for handling the resultset for returning the result of a select statement
	 */
	@FunctionalInterface
	public static interface HandleSelectResultSet<R> {
		R handle(ResultSet res) throws SQLException, NodeException;
	}

	/**
	 * Interface for handling statements after execution (e.g. get generated keys)
	 */
	@FunctionalInterface
	public static interface HandleStatement {
		void handle(PreparedStatement stmt) throws SQLException, NodeException;
	}
}
