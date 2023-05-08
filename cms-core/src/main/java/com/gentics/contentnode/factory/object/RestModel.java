package com.gentics.contentnode.factory.object;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for fields in factory implementation, that contain the RestModel of the object
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface RestModel {
	/**
	 * DB field names, that can be updated when the object is stored
	 * @return list of field names
	 */
	String[] update() default {};

	/**
	 * DB field names, that can be inserted (when the object is created), but not updated
	 * @return list of field names
	 */
	String[] insert() default {};
}
