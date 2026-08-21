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
}
