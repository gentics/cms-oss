/*
 * @author norbert
 * @date 02.05.2006
 * @version $Id: Property.java,v 1.1 2006-05-03 07:56:05 norbert Exp $
 */
package com.gentics.lib.util;

/**
 * Interface for an anonymous inner class that resolves a property to a value.
 * Can be used to implement resolving of various properties using a Map of
 * instances of this Interface instead of multiple string comparisons.
 */
public interface Property {

	/**
	 * Get the properties value
	 * @return the properties value
	 */
	Object get();
}
