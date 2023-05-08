package com.gentics.contentnode.factory;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.gentics.contentnode.object.NodeObject;

/**
 * Annotation for the TType of a subclass or subinterface of {@link NodeObject}
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface TType {
	int value();
}
