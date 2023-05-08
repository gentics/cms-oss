/*
 * @author norbert
 * @date 30.08.2005
 * @version $Id: SetHelper.java,v 1.2 2006-01-13 17:50:12 laurin Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.resolving;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.lib.base.ChangeableMap;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.log.NodeLogger;

/**
 * Helper class to execute assignment commands on Resolvable objects or Maps.
 * @author norbert
 */
public class SetHelper {

	/**
	 * pattern for set commands
	 */
	private static Pattern p;

	static {
		try {
			// compile the set command pattern
			p = Pattern.compile("\\s*([a-zA-Z0-9\\._]+)\\s*(=|\\+=)\\s*([a-zA-Z0-9\\._]+)\\s*", Pattern.CASE_INSENSITIVE);
		} catch (Exception e) {
			NodeLogger.getLogger(SetHelper.class).error("command pattern for set commands could not be compiled, cannot set properties in ClearFormActions");
		}
	}

	/**
	 * Map of resolvable base objects.
	 */
	private Map baseObjects;

	/**
	 * Resolver for the base objects
	 */
	private Resolvable baseObjectResolver;

	/**
	 * Property resolver
	 */
	private PropertyResolver propertyResolver;

	/**
	 * Property setter
	 */
	private PropertySetter propertySetter;

	/**
	 * Create an instance of the SetHelper.
	 */
	public SetHelper() {
		baseObjects = new HashMap();
		baseObjectResolver = new MapResolver(baseObjects);
		propertyResolver = new PropertyResolver(baseObjectResolver);
		propertySetter = new PropertySetter(baseObjectResolver);
	}

	/**
	 * Add a new base object to the SetHelper. The given object may be a
	 * Resolvable or a Map.
	 * @param objectName name of the base object
	 * @param object base object (Resolvable or Map)
	 */
	public void addBaseObject(String objectName, Object object) {
		if (object instanceof Resolvable) {
			baseObjects.put(objectName, object);
		} else if (object instanceof Map) {
			baseObjects.put(objectName, new ChangeableMap((Map) object));
		}
	}

	/**
	 * Parse and execute the given setCommand
	 * @param setCommand a set command of the form portal.blablabla.whatever =
	 *        view.blablabla
	 * @return true when the assignment could be processed, false if not
	 */
	public boolean executeCommand(String setCommand) {
		Matcher m = p.matcher(setCommand);

		if (m.matches()) {
			String propertyToSet = m.group(1).trim();
			String propertyToRead = m.group(3).trim();
			String assignmentOperator = m.group(2).trim();

			if ("=".equals(assignmentOperator)) {
				// set the property
				setProperty(propertyToSet, getProperty(propertyToRead));
			} else if ("+=".equals(assignmentOperator)) {
				// add a value to the property
				addToProperty(propertyToSet, getProperty(propertyToRead));
			}

			return true;
		} else {
			NodeLogger.getLogger(getClass()).warn("'" + setCommand + "' is no valid set command");
			return false;
		}
	}

	/**
	 * Get the property by its path.
	 * @param propertypath path to the property
	 * @return value of the property or null, if not set
	 */
	protected Object getProperty(String propertypath) {
		// no key, no property
		if (propertypath == null || propertypath.length() == 0) {
			return null;
		}

		Object property = null;

		try {
			property = propertyResolver.resolve(propertypath);
		} catch (UnknownPropertyException ignored) {}

		return property != null ? property : (propertypath.indexOf(".") < 0 ? propertypath : null);
	}

	/**
	 * set the property with given path
	 * @param propertyPath path to the property to set
	 * @param value new value of the property
	 */
	protected void setProperty(String propertyPath, Object value) {
		if (propertyPath == null || propertyPath.length() == 0) {
			return;
		}

		try {
			propertySetter.setProperty(propertyPath, value);
		} catch (Exception ex) {
			NodeLogger.getLogger(getClass()).error("could not set property '" + propertyPath + "'", ex);
		}
	}

	/**
	 * add the given value to the multivalue property
	 * @param propertyPath path to the multivalue property
	 * @param value new value to add to the multivalue property
	 */
	protected void addToProperty(String propertyPath, Object value) {
		if (value != null) {
			// first get the current value(s)
			Object oldValues = getProperty(propertyPath);

			// add the new value to the values only when it is not yet set
			if (oldValues instanceof Collection) {
				// add the new value to the collection
				if (!((Collection) oldValues).contains(value)) {
					((Collection) oldValues).add(value);
					setProperty(propertyPath, oldValues);
				}
			} else {
				// create a collection holding the old and new value
				Collection newValueCollection = new Vector();

				if (oldValues != null) {
					newValueCollection.add(oldValues);
				}
				if (!value.equals(oldValues)) {
					newValueCollection.add(value);
				}
				setProperty(propertyPath, newValueCollection);
			}
		}
	}
}
