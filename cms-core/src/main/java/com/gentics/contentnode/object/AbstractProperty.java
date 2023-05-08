/*
 * @author norbert
 * @date 13.02.2007
 * @version $Id: AbstractProperty.java,v 1.2 2007-02-26 07:47:44 norbert Exp $
 */
package com.gentics.contentnode.object;

import com.gentics.lib.etc.StringUtils;

/**
 * Class for resolvable properties of ContentObjects.
 * Provides a method to get the property and some meta information.
 */
public abstract class AbstractProperty {

	/**
	 * string array of the data properties this property depends on
	 */
	protected String[] dependsOn;

	/**
	 * Create an instance of the Property object
	 * @param dependsOn array of data properties this property depends on
	 */
	public AbstractProperty(String[] dependsOn) {
		this.dependsOn = dependsOn;
	}

	/**
	 * Get the name of data properties this property depends on
	 * @return name data properties this property depends on
	 */
	public String[] getDependsOn() {
		return dependsOn;
	}

	/**
	 * Return true when this property depends on the given data property
	 * @param property data property
	 * @return true when this property depends on the data property, false if not
	 */
	public boolean dependsOn(String property) {
		// no dependeny on empty properties
		if (StringUtils.isEmpty(property) || dependsOn == null) {
			return false;
		}
		// check all dependencies
		for (int i = 0; i < dependsOn.length; i++) {
			if (property.equals(dependsOn[i])) {
				// return true on first match
				return true;
			}
		}
		// no match found
		return false;
	}

	/**
	 * Return true when this property depends on any of the given data properties
	 * @param property array of data properties
	 * @return true when this property depends on any of the data properties, false if not
	 */
	public boolean dependsOn(String[] property) {
		// no dependency, if no property given
		if (property == null) {
			return false;
		}
		// check all given properties
		for (int i = 0; i < property.length; i++) {
			if (dependsOn(property[i])) {
				// return true on first match
				return true;
			}
		}
		// no match found
		return false;
	}
}
