package com.gentics.contentnode.tests.utils;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.gentics.api.lib.exception.NodeException;

/**
 * Annotate test methods that expect a specific NodeException to be thrown.
 * 
 * This annotation only works, when Rule {@link ExceptionChecker} is used.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Expected {
	/**
	 * Expected exception
	 * @return expected exception
	 */
	Class<? extends NodeException> ex();

	/**
	 * Expected message (optional)
	 * @return expected message
	 */
	String message() default "";
}
