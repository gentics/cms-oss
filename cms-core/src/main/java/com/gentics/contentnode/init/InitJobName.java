package com.gentics.contentnode.init;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for naming initialization jobs
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InitJobName {
	/**
	 * Get the name of the initialization job
	 * @return name of the job
	 */
	String value();
}
