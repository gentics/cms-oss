package com.gentics.contentnode.factory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for setter methods of data fields of NodeObjects
 * The value of the annotation determines the name of the field
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldSetter {

	/**
	 * Get the name of the data field
	 * @return name of the field
	 */
	String value();
}
