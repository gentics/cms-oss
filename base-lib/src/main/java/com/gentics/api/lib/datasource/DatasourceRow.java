/*
 * @author raoul
 * @date 28.07.2004
 * @version $Id: DatasourceRow.java,v 1.2 2006-03-27 15:54:12 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import java.sql.Timestamp;

import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.content.GenticsContentAttribute;

/**
 * Interface for a DatasourceRow contained in a
 * {@link com.gentics.api.lib.datasource.DatasourceRecordSet}. Instances obtained
 * from an instance of DatasrouceRecordSet can be used to fetch column data
 * (depending on the column type), but the preferred method is to fetch the
 * representing object using {@link #toObject()}. Most Implementations of
 * {@link com.gentics.api.lib.datasource.Datasource} will use
 * {@link com.gentics.api.lib.resolving.Resolvable}s as representing Objects.
 */
public interface DatasourceRow extends Resolvable {

	/**
	 * Get the content of the given column as String
	 * @param column column name
	 * @return value as String
	 */
	public String getString(String column);

	/**
	 * Get the content of the given column as int
	 * @param column column name
	 * @return value as int
	 */
	public int getInt(String column);

	/**
	 * Get the content of the given column as boolean
	 * @param column column name
	 * @return value as boolean
	 */
	public boolean getBoolean(String column);

	/**
	 * Get the content of the given column as double
	 * @param column column name
	 * @return value as double
	 */
	public double getDouble(String column);

	/**
	 * Get the content of the given column as long
	 * @param column column name
	 * @return value as long
	 */
	public long getLong(String column);

	/**
	 * Get the content of the given column as byte array
	 * @param column column name
	 * @return value as byte array
	 */
	public byte[] getBinary(String column);

	/**
	 * Get the type of the given column
	 * @param column column name
	 * @return type of the column
	 */
	public int getType(String column);

	/**
	 * Get the value of the given column as Timestamp
	 * @param column column name
	 * @return value as Timestamp
	 */
	public Timestamp getTimestamp(String column);

	/**
	 * Get the value of the given column as Object
	 * @param column column name
	 * @return value as Object
	 */
	public GenticsContentAttribute getObject(String column);

	/**
	 * Get the Object representing the data in this DatasourceRow
	 * @return Object representing the data
	 */
	public Object toObject();
}
