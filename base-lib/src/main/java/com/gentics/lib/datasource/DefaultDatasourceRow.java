package com.gentics.lib.datasource;

import java.sql.Timestamp;
import java.util.Map;

import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * created at Oct 17, 2004
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class DefaultDatasourceRow implements DatasourceRow {
	Map row;

	public DefaultDatasourceRow(Map row) {
		this.row = row;
	}

	public String getString(String column) {
		if (row == null) {
			return null;
		}
		return ObjectTransformer.getString(row.get(column), null);
	}

	public int getInt(String column) {
		if (row == null) {
			return 0;
		}
		return ObjectTransformer.getInt(row.get(column), 0);
	}

	public boolean getBoolean(String column) {
		return false;
	}

	public double getDouble(String column) {
		return 0;
	}

	public long getLong(String column) {
		return 0;
	}

	public byte[] getBinary(String column) {
		return new byte[0];
	}

	public int getType(String column) {
		return 0;
	}

	public Timestamp getTimestamp(String column) {
		return null;
	}

	public Object getObject(String column) {
		return null;
	}

	public Object toObject() {
		return null;
	}
    
	public Object getProperty(String key) {
		return row.get(key);
	}

	public Object get(String key) {
		return row.get(key);
	}

	public boolean canResolve() {
		return true;
	}
}
