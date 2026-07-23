package com.gentics.api.lib.resolving;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Extension to the {@link Resolvable} that adds default implementations to the methods, which
 * will resolve data by calling the getter methods of the class
 */
public interface IResolvableBean extends Resolvable {
	@Override
	default Object get(String key) {
		// simply call the getter on the object
		try {
			return PropertyUtils.getProperty(this, key);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	default Object getProperty(String key) {
		return get(key);
	}

	@Override
	default boolean canResolve() {
		return true;
	}
}
