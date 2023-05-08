/*
 * @author norbert
 * @date 20.06.2006
 * @version $Id: PropertyModificationListener.java,v 1.2 2006-07-19 09:06:23 norbert Exp $
 */
package com.gentics.api.lib.resolving;

/**
 * Interface for objects that need to be notified when some of their
 * subproperties are changed.
 */
public interface PropertyModificationListener {

	/**
	 * This method is called by the
	 * com.gentics.api.lib.resolving.PropertySetter when one of the
	 * (sub-)properties of this object is modified.
	 * 
	 * @param path path of the changed property (not containing the actual name of the property)
	 * @param property name of the changed property
	 * @param value new value of the property
	 */
	void propertyModified(String path, String property, Object value);
}
