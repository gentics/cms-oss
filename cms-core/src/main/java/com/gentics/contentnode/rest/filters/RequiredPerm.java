package com.gentics.contentnode.rest.filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for resources/methods that need to be authorized
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
@Repeatable(Authorizations.class)
public @interface RequiredPerm {
	/**
	 * Object type to check
	 * @return object type
	 */
	public abstract int type() default 0;

	/**
	 * Object id to check (0 if checking on a type)
	 * @return object id
	 */
	public abstract int id() default 0;

	/**
	 * Permission bit to check
	 * @return perm bit
	 */
	public abstract int bit() default 0;
}
