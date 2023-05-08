/*
 * @author herbert
 * @date May 18, 2007
 * @version $Id: ObjectRetrievalSQLExecutor.java,v 1.2 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.lib.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;

public class ObjectRetrievalSQLExecutor extends SQLExecutor {
	Object objectVal = null;
	boolean retrievedValue = false;
	private int column;
    
	public ObjectRetrievalSQLExecutor() {
		this(1);
	}
    
	public ObjectRetrievalSQLExecutor(int column) {
		this.column = column;
	}

	public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
		if (rs.next()) {
			objectVal = rs.getObject(column);
			retrievedValue = true;
		}
	}

	public int getIntegerValue() {
		if (objectVal == null) {
			return 0;
		} else if (objectVal instanceof Integer) {
			return ((Integer) objectVal).intValue();
		} else if (objectVal instanceof Long) {
			return ((Long) objectVal).intValue();
		} else {
			return Integer.parseInt(objectVal.toString());
		}
	}

	public Object getObjectValue() {
		return objectVal;
	}

	public boolean retrievedValue() {
		return retrievedValue;
	}
}
