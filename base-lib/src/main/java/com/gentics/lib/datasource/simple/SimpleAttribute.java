/*
 * @author norbert
 * @date 31.07.2006
 * @version $Id: SimpleAttribute.java,v 1.1 2006-09-07 11:33:15 laurin Exp $
 */
package com.gentics.lib.datasource.simple;

import java.util.Calendar;

/*
 * Implementation notice: keep this class synchronized with the class SimpleWSAttribute
 */

/**
 * Interface for attributes of
 * {@link com.gentics.lib.datasource.simple.SimpleObject}.
 */
public interface SimpleAttribute {

	/**
	 * Get the attribute name. This is never null or empty
	 * @return attribute name
	 */
	String getName();

	/**
	 * Get the attribute type. This is one of (stringValue, integerValue, longValue, doubleValue,
	 * dateValue, binaryValue, objectValue, multiStringValue, multiIntegerValue, multiLongValue, multiDoubleValue,
	 * multiDateValue, multiBinaryValue or multiObjectValue). Depending on the type, you have to call the
	 * get[Type]() method to retrieve the attributes value ([Type] is the
	 * returnvalue of this method with uppercase first letter).
	 * @return attribute type
	 * @see #getStringValue()
	 * @see #getMultiStringValue()
	 * @see #getIntegerValue()
	 * @see #getMultiIntegerValue()
	 * @see #getLongValue()
	 * @see #getMultiLongValue()
	 * @see #getBinaryValue()
	 * @see #getMultiBinaryValue()
	 * @see #getDoubleValue()
	 * @see #getMultiDoubleValue()
	 * @see #getDateValue()
	 * @see #getMultiDateValue()
	 * @see #getObjectValue()
	 * @see #getMultiObjectValue()
	 */
	String getType();

	/**
	 * Get the singlevalue string value. This returns null if the value is empty
	 * or the type different from "stringValue".
	 * @return string value or null
	 * @see #getType()
	 */
	String getStringValue();

	/**
	 * Get the singlevalue integer value. This returns null if the value is
	 * empty or the type different from "integerValue".
	 * @return integer value or null
	 * @see #getType()
	 */
	Integer getIntegerValue();

	/**
	 * Get the singlevalue long value. This returns null if the value is empty
	 * or the type different from "longValue".
	 * @return long value or null
	 * @see #getType()
	 */
	Long getLongValue();

	/**
	 * Get the singlevalue binary value. This returns null if the value is empty
	 * or the type different from "binaryValue".
	 * @return binary value or null
	 * @see #getType()
	 */
	byte[] getBinaryValue();

	/**
	 * Get the singlevalue double value. This returns null if the value is empty
	 * or the type different from "doubleValue".
	 * @return double value or null
	 * @see #getType()
	 */
	Double getDoubleValue();

	/**
	 * Get the singlevalue date value. This returns null if the value is empty
	 * or the type different from "dateValue".
	 * @return date value or null
	 */
	Calendar getDateValue();

	/**
	 * Get the singlevalue object value. This returns null if the value is empty
	 * or the type different from "objectValue".
	 * @return object value or null
	 */
	SimpleObject getObjectValue();

	/**
	 * Get the multivalue string values. This returns null if the type is
	 * different from "multiStringValue"
	 * @return array of string values
	 * @see #getType()
	 */
	String[] getMultiStringValue();

	/**
	 * Get the multivalue integer values. This returns null if the type is
	 * different from "multiIntegerValue"
	 * @return array of integer values
	 * @see #getType()
	 */
	Integer[] getMultiIntegerValue();

	/**
	 * Get the multivalue long values. This returns null if the type is
	 * different from "multiLongValue"
	 * @return array of long values
	 * @see #getType()
	 */
	Long[] getMultiLongValue();

	/**
	 * Get the multivalue binary values. This returns null if the type is
	 * different from "multiBinaryValue"
	 * @return array of binary values
	 * @see #getType()
	 */
	byte[][] getMultiBinaryValue();

	/**
	 * Get the multivalue double values. This returns null if the type is
	 * different from "multiDoubleValue"
	 * @return array of double values
	 * @see #getType()
	 */
	Double[] getMultiDoubleValue();

	/**
	 * Get the multivalue date values. This returns null if the type is
	 * different from "multiDateValue"
	 * @return array of date values
	 * @see #getType()
	 */
	Calendar[] getMultiDateValue();

	/**
	 * Get the multivalue object values. This returns null if the type is
	 * different from "multiObjectValue".
	 * @return array of object values
	 * @see #getType()
	 */
	SimpleObject[] getMultiObjectValue();
}
