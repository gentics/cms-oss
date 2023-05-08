package com.gentics.contentnode.testutils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.gentics.contentnode.etc.Feature;

@Retention(RetentionPolicy.RUNTIME)
public @interface GCNFeature {
	/**
	 * Get the features that shall be set during test execution
	 * @return features
	 */
	public Feature[] set() default {};

	/**
	 * Get the features that shall be unset during test execution
	 * @return features
	 */
	public Feature[] unset() default {};
}
