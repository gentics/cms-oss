package com.gentics.contentnode.resolving;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for getter methods which should be made available also in resolving
 */
@Target(METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResolvableGetter {
}
