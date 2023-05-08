/*
 * @author floriangutmann
 * @date Dec 4, 2009
 * @version $Id: PersistentSchedulerConnectionProvider.java,v 1.2 2009-12-16 16:12:08 herbert Exp $
 */
package com.gentics.contentnode.scheduler;

import java.sql.Connection;
import java.sql.SQLException;

import org.quartz.utils.ConnectionProvider;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * ConnectionProvider which provides connections for the 
 * PersistentScheduler from the Datasoure in the NodeConfig.
 * 
 * @author floriangutmann
 */
public class PersistentSchedulerConnectionProvider implements ConnectionProvider {
	/**
	 * Default constructor, called by Quartz
	 */
	public PersistentSchedulerConnectionProvider() {}

	/*
	 * (non-Javadoc)
	 * @see org.quartz.utils.ConnectionProvider#getConnection()
	 */
	public Connection getConnection() throws SQLException {
		try {
			return NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getConnection();
		} catch (NodeException e) {
			if (e.getCause() instanceof SQLException) {
				throw (SQLException) e.getCause();
			} else {
				throw new SQLException(e.getMessage());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.quartz.utils.ConnectionProvider#shutdown()
	 */
	public void shutdown() throws SQLException {}
}
