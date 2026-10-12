package com.gentics.contentnode.resolving;

import java.util.Set;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * Interface for {@link Resolvable} implementations that can be wrapped into {@link ResolvableMapWrapper}
 */
public interface ResolvableMapWrappable extends Resolvable {
	/**
	 * Get the keys, this Resolvable can resolve
	 * @return set of keys
	 */
	Set<String> getResolvableKeys();

	/**
	 * Resolve the given key as value in a map. The default implementation will simply call {@link Resolvable#get(String)}, but
	 * this may be overwritten, if resolving in a map must behave differently
	 * @param key key of the property
	 * @return value of the property or null
	 */
	default Object getAsMapValue(String key) {
		return get(key);
	}

	/**
	 * Check whether instances of this type must be replaced with their string representation
	 * when they were resolved via iteration over a map. The default implementation returns false
	 * @return true when the instance must be replaced with its string representation
	 */
	default boolean replaceWithStringInMap() {
		return false;
	}
}
