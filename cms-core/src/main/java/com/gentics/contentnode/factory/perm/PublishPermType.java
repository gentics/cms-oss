package com.gentics.contentnode.factory.perm;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.gentics.contentnode.rest.model.perm.PermType;

/**
 * Annotation for specifying, which {@link PermType} defines the publish permission
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface PublishPermType {
	/**
	 * PermType
	 * @return PermType
	 */
	PermType value();
}
