/*
 * @(#) LDAPDatasourceRow/LDAPDatasourceRow.java   1.0   21.08.2004 10:46:39
 *
 * Copyright 2004 Gentics Net.Solutions
 *
 * created on 21.08.2004 by Robert Reinhardt 
 *
 * Canges:
 */
package com.gentics.lib.datasource;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;

import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * @author robert
 */
public class LDAPDatasourceRow implements DatasourceRow, Resolvable {

	// key = attribute name, value=attribute value
	private HashMap rowElements;

	public LDAPDatasourceRow() {
		this.rowElements = new HashMap();
	}

	public HashMap getMap() {
		return this.rowElements;
	}

	/**
	 * public boolean setRowElement(String key, Object object)
	 * @param comlumnName String - name of row element
	 * @param object Object - value of column
	 * @return boolean true if element has been stored, false if not
	 */
	public boolean setRowElement(String comlumnName, Object object) {

		boolean rc = false;

		if ((comlumnName != null) && (object != null)) {

			this.rowElements.put(comlumnName, object);
			rc = true;

		}

		return rc;

	}

	public Iterator getColumnNames() {

		return this.rowElements.keySet().iterator();

	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#getString(java.lang.String)
	 */
	public String getString(String column) {
		Object value = rowElements.get(column);

		return value != null ? value.toString() : null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#getInt(java.lang.String)
	 */
	public int getInt(String column) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#getBoolean(java.lang.String)
	 */
	public boolean getBoolean(String column) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#getDouble(java.lang.String)
	 */
	public double getDouble(String column) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#getLong(java.lang.String)
	 */
	public long getLong(String column) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#getBinary(java.lang.String)
	 */
	public byte[] getBinary(String column) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#getType(java.lang.String)
	 */
	public int getType(String column) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#getTimestamp(java.lang.String)
	 */
	public Timestamp getTimestamp(String column) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#getObject(java.lang.String)
	 */
	public Object getObject(String column) {

		return this.getProperty(column);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceRow#toObject()
	 */
	public Object toObject() {
		return this;
	}

	/**
	 * Methods from Interface Resolveable
	 */

	public HashMap getPropertyNames() {

		return new HashMap();

	}

	public Object getProperty(String key) {
		return rowElements.get(key);
	}

	public Object get(String key) {
		return getProperty(key);
	}

	// if canResolve returns false, all properties equate to null
	public boolean canResolve() {
		return true;
	}

}
