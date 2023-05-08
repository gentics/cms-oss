package com.gentics.contentnode.factory.object;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for unversioned data fields
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Unversioned {
}
