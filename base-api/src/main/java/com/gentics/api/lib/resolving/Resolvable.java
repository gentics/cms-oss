/*
 * @date 16.06.2003
 * @version $Id: Resolvable.java,v 1.1 2006-01-13 15:25:41 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.resolving;

/**
 * Interface for objects that provide properties by resolving property paths.
 */
public interface Resolvable {

	/**
	 * Get the property named by key or null if the property does not exist or
	 * is not set. Alias for {@link #get(String)}.
	 * @param key key of the property
	 * @return value of the property or null
	 */
	Object getProperty(String key);

	/**
	 * Get the property named by key or null if the property does not exist or
	 * is not set.
	 * @param key key of the property
	 * @return value of the property or null
	 */
	Object get(String key);

	/**
	 * Check whether the resolvable is capable of resolving properties right
	 * now. If this method returns false all calls to
	 * {@link #getProperty(String)} or {@link #get(String)} will return null.
	 * @return true when the resolvable can resolve properties. false if not
	 */
	boolean canResolve();
}
