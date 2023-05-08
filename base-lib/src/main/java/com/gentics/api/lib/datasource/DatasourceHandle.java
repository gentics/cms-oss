/*
 * @author haymo
 * @date 04.08.2004
 * @version $Id: DatasourceHandle.java,v 1.3 2008-01-09 09:11:21 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

/**
 * Interface for a datasource handle
 */
public interface DatasourceHandle {

	/**
	 * Initialize the datasource handle with the given parameters
	 * @param parameters map of parameters
	 */
	public void init(java.util.Map parameters);

	/**
	 * Close the datasource handle
	 */
	public void close();

	/**
	 * Get the datasource definition
	 * @return datasource definition
	 */
	public DatasourceDefinition getDatasourceDefinition();

	/**
	 * Check whether the connection is still alive
	 * @return true when the connection is alive, false if not
	 */
	public boolean isAlive();

	/**
	 * Get the last exception
	 * @return last exception or null
	 */
	public Exception getLastException();
}
