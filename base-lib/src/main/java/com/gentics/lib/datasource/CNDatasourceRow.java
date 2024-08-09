package com.gentics.lib.datasource;

import java.sql.Timestamp;

import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentHelper;
import com.gentics.lib.content.GenticsContentObject;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 13.08.2004
 */
public class CNDatasourceRow implements DatasourceRow {
	private GenticsContentObject object;

	public CNDatasourceRow(GenticsContentObject cno) {
		this.object = cno;
	}

	public String getString(String column) {
		return GenticsContentHelper.getString(object, column);
	}

	public int getInt(String column) {
		return GenticsContentHelper.getInt(object, column);
	}

	public boolean getBoolean(String column) {
		return GenticsContentHelper.getBoolean(object, column);
	}

	public double getDouble(String column) {
		return GenticsContentHelper.getDouble(object, column);
	}

	public long getLong(String column) {
		return GenticsContentHelper.getLong(object, column);
	}

	public byte[] getBinary(String column) {
		Object ret = GenticsContentHelper.getObject(object, column);

		if (ret == null) {
			return null;
		}
		if (ret instanceof byte[]) {
			return (byte[]) ret;
		}
		return ret.toString().getBytes();
	}

	public int getType(String column) {
		return 0;
	}

	public Timestamp getTimestamp(String column) {
		// TODO check if the attribute timestamp is always available
		return (Timestamp) GenticsContentHelper.getObject(object, "timestamp");
	}

	public GenticsContentAttribute getObject(String column) {
		return GenticsContentHelper.getObject(object, column);
	}

	public Object toObject() {
		return object;
	}

	public Object getProperty(String key) {
		return object.getProperty(key);
	}

	public Object get(String key) {
		return object.getProperty(key);
	}

	public boolean canResolve() {
		return object.canResolve();
	}
}
