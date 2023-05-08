package com.gentics.contentnode.rest.filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.gentics.contentnode.etc.Feature;

/**
 * Annotation for containing a required feature for a resource or method
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
@Repeatable(RequiredFeatures.class)
public @interface RequiredFeature {
	Feature value();
}
