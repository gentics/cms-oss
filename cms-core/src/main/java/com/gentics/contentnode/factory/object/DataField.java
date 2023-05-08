package com.gentics.contentnode.factory.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields in factory classes as data fields
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataField {
	/**
	 * Name of the database column
	 * @return column name
	 */
	String value();

	/**
	 * Flag to mark data fields, which will be stored in JSON format
	 * @return json flag
	 */
	boolean json() default false;
}
