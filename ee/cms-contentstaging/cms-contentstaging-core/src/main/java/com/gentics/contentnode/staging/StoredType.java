package com.gentics.contentnode.staging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying the type of an implementation of {@link StoredModel}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StoredType {
	/**
	 * Type
	 * @return type
	 */
	String value();
}
