/*
 * @author norbert
 * @date 28.08.2006
 * @version $Id: ObjectWrapperHelper.java,v 1.2 2006-09-07 11:33:14 laurin Exp $
 */
package com.gentics.lib.ws.datasource;

import com.gentics.lib.datasource.simple.SimpleAttribute;
import com.gentics.lib.datasource.simple.SimpleObject;

/**
 * Helper class the generates web-service compatible wrappers for instances of
 * {@link com.gentics.lib.datasource.simple.SimpleObject} and
 * {@link com.gentics.lib.datasource.simple.SimpleAttribute}.
 */
public final class ObjectWrapperHelper {

	/**
	 * private constructor for the static helper class
	 */
	private ObjectWrapperHelper() {}

	/**
	 * Create a wrapper aroung the given object
	 * @param object wrapped object
	 * @return object wrapper
	 */
	public static SimpleWSObject wrapObject(SimpleObject object) {
		if (object == null) {
			return null;
		} else {
			return new SimpleWSObject(object);
		}
	}

	/**
	 * Create wrappers around the given objects
	 * @param objects array of wrapped objects
	 * @return array of object wrappers
	 */
	public static SimpleWSObject[] wrapObjects(SimpleObject[] objects) {
		if (objects == null) {
			return null;
		} else {
			SimpleWSObject[] wrappers = new SimpleWSObject[objects.length];

			for (int i = 0; i < objects.length; i++) {
				wrappers[i] = new SimpleWSObject(objects[i]);
			}
			return wrappers;
		}
	}

	/**
	 * Create a wrapper for the given attribute
	 * @param attribute wrapped attribute
	 * @return attribute wrapper
	 */
	public static SimpleWSAttribute wrapAttribute(SimpleAttribute attribute) {
		if (attribute == null) {
			return null;
		} else {
			return new SimpleWSAttribute(attribute);
		}
	}

	/**
	 * Create wrappers around the given attributes
	 * @param attributes array of wrapped attributes
	 * @return array of attribute wrappers
	 */
	public static SimpleWSAttribute[] wrapAttributes(SimpleAttribute[] attributes) {
		if (attributes == null) {
			return null;
		} else {
			SimpleWSAttribute[] wrappers = new SimpleWSAttribute[attributes.length];

			for (int i = 0; i < attributes.length; i++) {
				wrappers[i] = new SimpleWSAttribute(attributes[i]);
			}

			return wrappers;
		}
	}
}
