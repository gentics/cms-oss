/*
 * @author tobiassteiner
 * @date Aug 5, 2010
 * @version $Id: PreparedStatementHandlerImpl.java,v 1.2 2010-09-28 17:01:28 norbert Exp $
 */
package com.gentics.contentnode.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.lib.log.NodeLogger;

public class PreparedStatementHandlerImpl implements PreparedStatementHandler {

	private final Connection connection;
    
	private static NodeLogger logger = NodeLogger.getNodeLogger(PreparedStatementHandler.class);

	public PreparedStatementHandlerImpl(Connection conn) {
		connection = conn;
	}
    
	public void closeResultSet(ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				logger.warn("Error while closing the resultset", e);
			}
		}
	}

	public void closeStatement(PreparedStatement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				logger.warn("Error while closing the statement", e);
			}
		}
	}

	public PreparedStatement prepareDeleteStatement(String sql) throws SQLException,
				TransactionException {
		return prepareStatement(sql);
	}

	public PreparedStatement prepareInsertStatement(String sql) throws SQLException,
				TransactionException {
		return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException,
				TransactionException {
		return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	}

	public PreparedStatement prepareStatement(String sql, int type) throws SQLException,
				TransactionException {
		switch (type) {
		case Transaction.INSERT_STATEMENT:
			return prepareInsertStatement(sql);

		case Transaction.UPDATE_STATEMENT:
			return prepareUpdateStatement(sql);

		case Transaction.DELETE_STATEMENT:
			return prepareDeleteStatement(sql);

		default:
			// select and everything else
			return prepareStatement(sql);
		}        
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException, TransactionException {
		return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareUpdateStatement(String sql) throws SQLException,
				TransactionException {
		return prepareStatement(sql);
	}

	@Override
	public PreparedStatement prepareSelectForUpdate(String sql) throws SQLException, TransactionException {
		return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
	}
}
