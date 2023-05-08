package com.gentics.lib.db;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface of classes handling DatabaseMetaData. Instance can be passed to the method {@link DB#handleDatabaseMetaData(DBHandle, DatabaseMetaDataHandler)}.
 */
public interface DatabaseMetaDataHandler {

	/**
	 * Handle the meta data
	 * @param metaData meta data
	 * @throws SQLException
	 * @throws NodeException
	 */
	void handleMetaData(DatabaseMetaData metaData) throws SQLException, NodeException;
}
