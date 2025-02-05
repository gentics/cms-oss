package com.gentics.contentnode.rest.filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * Annotation for resources/methods that need to be authenticated.
 * The value must be the name of the configuration parameter, which contains the allowed hostnames/IP's
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
@NameBinding
public @interface AccessControl {
	String value();
}
