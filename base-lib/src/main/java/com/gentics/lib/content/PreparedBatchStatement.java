package com.gentics.lib.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;

/**
 * Class encapsulating data for batched sql statements
 */
public class PreparedBatchStatement {
	/**
	 * SQL Statement
	 */
	protected String sql;

	/**
	 * List of SQL Datatypes used for the parameters in the SQL Statement
	 */
	protected List<Integer> types;

	/**
	 * Map of parameter columns. Maps column names to parameter indices
	 */
	protected Map<String, Integer> parameterColumns;

	/**
	 * Collection of parameter arrays
	 */
	protected Collection<Object[]> params = new ArrayList<Object[]>();

	/**
	 * Create an instance
	 * @param sql sql statement
	 * @param types list of types
	 * @param parameterColumns map for column names -&gt; index of parameter in SQL statement
	 */
	public PreparedBatchStatement(String sql, List<Integer> types, Map<String, Integer> parameterColumns) {
		this.sql = sql;
		this.types = types;
		this.parameterColumns = parameterColumns;
	}

	/**
	 * Add parameters for a single statement
	 * @param statementParams statement parameters
	 */
	public void add(Object[] statementParams) {
		params.add(statementParams);
	}

	/**
	 * Add parameters for a single statement
	 * @param statementParams map of parameters
	 */
	public void add(Map<String, Object> statementParams) {
		if (!ObjectTransformer.isEmpty(statementParams)) {
			Object[] toAdd = new Object[parameterColumns.size()];

			for (Map.Entry<String, Object> entry : statementParams.entrySet()) {
				int index = ObjectTransformer.getInt(parameterColumns.get(entry.getKey()), -1);
				if (index >= 0) {
					toAdd[index] = entry.getValue();
				}
			}

			params.add(toAdd);
		}
	}

	/**
	 * Execute the batched statements with the given handle
	 * @param handle db handle
	 * @throws SQLException
	 */
	public void execute(DBHandle handle) throws SQLException {
		DB.batchUpdate(handle, sql, params, types);
	}
}
