package com.gentics.contentnode.testutils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Interface for implementations of {@link ConfigModifier} that shall be
 * instantiated and used to modify the configuration
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigBy {
	/**
	 * Implementation classes
	 * @return classes
	 */
	public Class<? extends ConfigModifier>[] value();
}
