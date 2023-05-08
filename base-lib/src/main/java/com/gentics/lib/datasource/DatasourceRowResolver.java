package com.gentics.lib.datasource;

import java.util.HashMap;

import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * created at Nov 12, 2004
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class DatasourceRowResolver implements Resolvable {
	protected DatasourceRow row = null;

	public void setRow(DatasourceRow row) {
		this.row = row;
	}

	public HashMap getPropertyNames() {
		return null;
	}

	public Object getProperty(String key) {
		return row.getString(key);
	}

	public Object get(String key) {
		return getProperty(key);
	}

	// if canResolve returns false, all properties equate to null
	public boolean canResolve() {
		return row != null;
	}
}
