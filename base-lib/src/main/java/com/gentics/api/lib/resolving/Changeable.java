/*
 * @author robert
 * @date 22.07.2004
 * @version $Id: Changeable.java,v 1.3 2006-01-26 10:23:53 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.resolving;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;

/**
 * Interface for Resolvables that my be changed.
 * @author robert
 */
public interface Changeable extends Resolvable {
	// TODO Check implementors of setProperty for correct return value.
	/**
	 * Set (modify) the property name to resolve to the given value
	 * @param name name of the property to set/modify
	 * @param value (new) value to set
	 * @return true if property was set successful, false otherwise.
	 * @throws InsufficientPrivilegesException when the property may not be
	 *         changed
	 */
	boolean setProperty(String name, Object value) throws InsufficientPrivilegesException;
}
