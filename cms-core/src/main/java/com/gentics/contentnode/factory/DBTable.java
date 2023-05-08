package com.gentics.contentnode.factory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.gentics.contentnode.object.NodeObject;

/**
 * Annotation for DBTable of NodeObject classes
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DBTable {
	Class<? extends NodeObject> clazz();

	String name();

	String alias() default "";

	boolean table2Class() default true;
}
