/*
 * @author herbert
 * @date 05.04.2007
 * @version $Id: SQLExecutor.java,v 1.4 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;

public abstract class SQLExecutor {
    
	public void prepareStatement(PreparedStatement stmt) throws SQLException {}
    
	public void handleResultSet(ResultSet rs) throws SQLException, NodeException {}
    
	public void handleUpdateCount(int count) throws NodeException {}
    
	/**
	 * Gives access to the statement after it was executed.
	 * Could be used for example to get access to the generatedKeys.
	 * @param stmt The prepared statement.
	 * @throws SQLException, NodeException 
	 */
	public void handleStatment(PreparedStatement stmt) throws SQLException, NodeException {}
}
