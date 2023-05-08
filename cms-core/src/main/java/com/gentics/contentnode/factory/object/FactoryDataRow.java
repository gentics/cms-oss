/*
 * @author norbert
 * @date 22.12.2010
 * @version $Id: FactoryDataRow.java,v 1.1.2.2 2011-01-19 14:43:47 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * Instances of this class contain the data which were fetched from the database
 * for a factory object. This class basically mimics the behaviour of
 * {@link ResultSet} (for fetching data of a specific row) and implements
 * additional behaviour like combining versioned and non-versioned data.
 */
public class FactoryDataRow {

	/**
	 * internally stored values
	 */
	protected Map<String, Object> values = new HashMap<String, Object>();

	/**
	 * This flag will be set, if an attribute with null as value was accessed
	 */
	protected boolean wasNull = false;

	/**
	 * Create an instance, which contains all values from the current row in the
	 * given result set
	 * @param rs result set
	 * @throws SQLException if accessing the resultset fails
	 */
	public FactoryDataRow(ResultSet rs) throws SQLException {
		mergeWithResultSet(rs);
	}

	/**
	 * Create an instance which contains the given values
	 * @param values values
	 */
	public FactoryDataRow(Map<String, Object> values) {
		this.values = values;
	}

	/**
	 * Merge the data currently stored with the data from the given result set.
	 * The data given in the resultset will overwrite existing values already
	 * stored in the instance.
	 * @param rs resultset containing additional data
	 */
	public void mergeWithResultSet(ResultSet rs) throws SQLException {
		// get the metadata
		ResultSetMetaData metaData = rs.getMetaData();

		// fill the values map with all values contained in the current row of the resultset
		for (int i = 1; i <= metaData.getColumnCount(); i++) {
			String key = metaData.getColumnLabel(i);

			values.put(key, rs.getObject(key));
		}
	}

	/**
	 * Get the attribute value for the given name as {@link String}.
	 * @param name name of the attribute
	 * @return value as string or null
	 */
	public String getString(String name) {
		return ObjectTransformer.getString(getAttributeValue(name), null);
	}

	/**
	 * Get the attribute value for the given name as boolean
	 * @param name name of the attribute
	 * @return value as boolean
	 */
	public boolean getBoolean(String name) {
		return ObjectTransformer.getBoolean(getAttributeValue(name), false);
	}

	/**
	 * Get the attribute value for the given name as int
	 * @param name name of the attribute
	 * @return value as integer
	 */
	public int getInt(String name) {
		return ObjectTransformer.getInt(getAttributeValue(name), 0);
	}

	/**
	 * Get the attribute value for the given name as Integer
	 * @param name name of the attribute
	 * @return value as Integer (may be null)
	 */
	public Integer getInteger(String name) {
		return ObjectTransformer.getInteger(getAttributeValue(name), null);
	}

	/**
	 * Get the attribute value for the given name as float
	 * @param name name of the attribute
	 * @param defaultValue Default value which will be used if the value can't be parsed
	 * @return value as float
	 */
	public float getFloat(String name, float defaultValue) {
		return ObjectTransformer.getFloat(getAttributeValue(name), defaultValue);
	}

	/**
	 * Check whether the value of the last accessed attribute value was null
	 * (but e.g. was accessed with the method {@link #getInt(String)}, which
	 * would return 0 in such a case).
	 * @return true if the alst accessed attribute value was null, false if not
	 */
	public boolean wasNull() {
		return wasNull;
	}

	/**
	 * Get all values as a map
	 * @return values as map
	 */
	public Map<String, Object> getValues() {
		return values;
	}

	/**
	 * Internal helper method to get the value of the attribute name and set the {@link #wasNull} flag appropriately
	 * @param name name of the attribute
	 * @return attribute value
	 */
	protected Object getAttributeValue(String name) {
		Object value = values.get(name);

		wasNull = value == null;
		return value;
	}
}
