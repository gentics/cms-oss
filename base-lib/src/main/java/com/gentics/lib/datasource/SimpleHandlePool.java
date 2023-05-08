package com.gentics.lib.datasource;

import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.HandlePool;

/**
 * Dummy implementation of HandlePool - does not actually pool only
 * holds one handle instance.
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 17.09.2004
 * @see com.gentics.api.lib.datasource.HandlePool
 */
public class SimpleHandlePool implements HandlePool {
	private DatasourceHandle handle;

	public SimpleHandlePool(DatasourceHandle handle) {
		this.handle = handle;
	}

	public DatasourceHandle getHandle() {
		return this.handle;
	}

	public String getTypeID() {
		if (this.handle == null) {
			return null;
		}
		return this.handle.getDatasourceDefinition().getID();
	}

	public void close() {
		handle.close();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return handle == null ? null : handle.toString();
	}
}
