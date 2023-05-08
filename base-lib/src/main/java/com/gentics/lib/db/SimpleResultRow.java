package com.gentics.lib.db;

import gnu.trove.TObjectIntHashMap;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) <p/>Date: 27.01.2004
 */
public class SimpleResultRow {
	// Map row;
	Object[] row;

	TObjectIntHashMap nameHash;

	/**
	 * @param nameHash hash from name -> columnId, where column-id has to start
	 *        with 0!!
	 * @param row
	 */
	public SimpleResultRow(TObjectIntHashMap nameHash, Object[] row) {
		this.nameHash = nameHash;
		this.row = row;
	}

	public SimpleResultRow(TObjectIntHashMap nameHash) {
		this.nameHash = nameHash;
		this.row = new Object[nameHash.size()];
	}

	public void add(String colName, Object value) {
		if (nameHash.containsKey(colName)) {
			int col = nameHash.get(colName);

			row[col] = value;
		}
	}

	public void expandCapacity(int size) {
		Object[] newRow = new Object[row.length + size];

		for (int i = 0; i < row.length; i++) {
			newRow[i] = row[i];
		}
		row = newRow;
	}

	public int getColId(String colName) {
		return nameHash.get(colName);
	}

	private Object getCol(String colName) {
		if (!nameHash.containsKey(colName.toLowerCase())) {
			return null;
		}
		int col = nameHash.get(colName.toLowerCase());

		return row[col];
	}

	public String getString(String colName) {
		Object o = getCol(colName);

		if (o == null) {
			return null;
		}
		if (o instanceof byte[]) {
			try {
				// TODO support other content charsets than utf-8
				return new String((byte[]) o, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (o instanceof String) {
			return (String) o;
		}
		return o.toString();
	}

	public int getInt(String colName) {
		return ObjectTransformer.getInt(getCol(colName), 0);
	}

	public long getLong(String colName) {
		return ObjectTransformer.getLong(getCol(colName), 0);
	}

	public double getDouble(String colName) {
		return ObjectTransformer.getDouble(getCol(colName), 0);
	}

	public boolean getBoolean(String colName) {
		return ObjectTransformer.getBoolean(getCol(colName), false);
	}

	public Object getObject(String colName) {
		return getCol(colName);
	}

	public Object[] getRowData() {
		return this.row;
	}

	/**
	 * Get row data as map
	 * @return row data as map
	 */
	public Map<String, Object> getMap() {
		Map<String, Object> ret = new HashMap<String, Object>(nameHash.size());
		Object[] o = nameHash.keys();

		for (int i = 0; i < o.length; i++) {
			ret.put(ObjectTransformer.getString(o[i], null), this.row[nameHash.get(o[i])]);
		}
		return ret;
	}
}
