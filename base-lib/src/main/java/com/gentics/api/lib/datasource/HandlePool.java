/*
 * @author Erwin Mascher
 * @date 17.09.2004
 * @version $Id: HandlePool.java,v 1.1 2006-01-23 16:40:50 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

/**
 * Interface for the handles of a datasource.
 */
public interface HandlePool {

	/**
	 * Get a handle of the datasource
	 * Might return null if no valid handle is available
	 * @return a datasource handle
	 */
	DatasourceHandle getHandle();

	/**
	 * Get the type id of the handles
	 * @return type id
	 */
	String getTypeID();

	/**
	 * Close all handles for this datasource
	 */
	void close();
}
