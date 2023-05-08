/*
 * @author norbert
 * @date 28.08.2006
 * @version $Id: SimpleWSAttribute.java,v 1.3 2006-09-07 11:33:14 laurin Exp $
 */
package com.gentics.lib.ws.datasource;

import java.util.Calendar;

import com.gentics.lib.datasource.simple.SimpleAttribute;

/*
 * Implementation notice: keep this class synchronized with the interface SimpleAttribute
 */

/**
 * Wrapper around the interface {@link SimpleAttribute}. This wrapper is used
 * as return value for webservice calls.
 */
public class SimpleWSAttribute {

	/**
	 * wrapped attribute
	 */
	private SimpleAttribute attribute;

	/**
	 * Create instance of the wrapper
	 * @param attribute wrapped attribute
	 */
	public SimpleWSAttribute(SimpleAttribute attribute) {
		this.attribute = attribute;
	}

	/**
	 * Get the attribute name. This is never null or empty
	 * @return attribute name
	 */
	public String getName() {
		return attribute.getName();
	}

	/**
	 * Get the attribute type. This is one of (string, integer, long, double,
	 * date, binary, object, multiString, multiInteger, multiLong, multiDouble,
	 * multiDate, multiBinary or multiObject). Depending on the type, you have
	 * to call the get[Type]() method to retrieve the attributes value ([Type]
	 * is the returnvalue of this method with uppercase first letter).
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
	public String getType() {
		return attribute.getType();
	}

	/**
	 * Get the singlevalue string value. This returns null if the value is empty
	 * or the type different from "string".
	 * @return string value or null
	 * @see #getType()
	 */
	public String getStringValue() {
		return attribute.getStringValue();
	}

	/**
	 * Get the singlevalue integer value. This returns null if the value is
	 * empty or the type different from "integer".
	 * @return integer value or null
	 * @see #getType()
	 */
	public Integer getIntegerValue() {
		return attribute.getIntegerValue();
	}

	/**
	 * Get the singlevalue long value. This returns null if the value is empty
	 * or the type different from "long".
	 * @return long value or null
	 * @see #getType()
	 */
	public Long getLongValue() {
		return attribute.getLongValue();
	}

	/**
	 * Get the singlevalue binary value. This returns null if the value is empty
	 * or the type different from "binary".
	 * @return binary value or null
	 * @see #getType()
	 */
	public byte[] getBinaryValue() {
		return attribute.getBinaryValue();
	}

	/**
	 * Get the singlevalue double value. This returns null if the value is empty
	 * or the type different from "double".
	 * @return double value or null
	 * @see #getType()
	 */
	public Double getDoubleValue() {
		return attribute.getDoubleValue();
	}

	/**
	 * Get the singlevalue date value. This returns null if the value is empty
	 * or the type different from "date".
	 * @return date value or null
	 */
	public Calendar getDateValue() {
		return attribute.getDateValue();
	}

	/**
	 * Get the singlevalue object value. This returns null if the value is empty
	 * or the type different from "object".
	 * @return object value or null
	 */
	public SimpleWSObject getObjectValue() {
		return ObjectWrapperHelper.wrapObject(attribute.getObjectValue());
	}

	/**
	 * Get the multivalue string values. This returns null if the type is
	 * different from "multiString"
	 * @return array of string values
	 * @see #getType()
	 */
	public String[] getMultiStringValue() {
		return attribute.getMultiStringValue();
	}

	/**
	 * Get the multivalue integer values. This returns null if the type is
	 * different from "multiInteger"
	 * @return array of integer values
	 * @see #getType()
	 */
	public Integer[] getMultiIntegerValue() {
		return attribute.getMultiIntegerValue();
	}

	/**
	 * Get the multivalue long values. This returns null if the type is
	 * different from "multiLong"
	 * @return array of long values
	 * @see #getType()
	 */
	public Long[] getMultiLongValue() {
		return attribute.getMultiLongValue();
	}

	/**
	 * Get the multivalue binary values. This returns null if the type is
	 * different from "multiBinary"
	 * @return array of binary values
	 * @see #getType()
	 */
	public byte[][] getMultiBinaryValue() {
		return attribute.getMultiBinaryValue();
	}

	/**
	 * Get the multivalue double values. This returns null if the type is
	 * different from "multiDouble"
	 * @return array of double values
	 * @see #getType()
	 */
	public Double[] getMultiDoubleValue() {
		return attribute.getMultiDoubleValue();
	}

	/**
	 * Get the multivalue date values. This returns null if the type is
	 * different from "multiDate"
	 * @return array of date values
	 * @see #getType()
	 */
	public Calendar[] getMultiDateValue() {
		return attribute.getMultiDateValue();
	}

	/**
	 * Get the multivalue object values. This returns null if the type is
	 * different from "multiObject".
	 * @return array of object values
	 * @see #getType()
	 */
	public SimpleWSObject[] getMultiObjectValue() {
		return ObjectWrapperHelper.wrapObjects(attribute.getMultiObjectValue());
	}
}
