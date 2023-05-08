/*
 * AbstractDatasourceHandle.java
 *
 * Created on 14. August 2004, 14:16
 */

package com.gentics.lib.datasource;

import com.gentics.api.lib.datasource.DatasourceDefinition;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.lib.log.NodeLogger;

/**
 * @author Dietmar
 */
public abstract class AbstractDatasourceHandle implements DatasourceHandle {
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	protected DatasourceDefinition def = null;

	/**
	 * id of the datasource handle
	 */
	protected String id;

	/**
	 * last exception
	 */
	protected Exception lastException;

	/**
	 * Create an instance with given id
	 * @param id id
	 */
	public AbstractDatasourceHandle(String id) {
		this.id = id;
	}

	public void setDatasourceDefinition(DatasourceDefinition def) {
		this.def = def;
	}

	public DatasourceDefinition getDatasourceDefinition() {
		return def;
	}

	public abstract void init(java.util.Map parameters);

	/**
	 * Get the last exception (if any)
	 * @return last exception
	 */
	public Exception getLastException() {
		return lastException;
	}
}
