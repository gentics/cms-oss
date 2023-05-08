/*
 * NotPoolObjectException.java
 *
 * Created on 28. August 2004, 10:22
 */

package com.gentics.lib.pooling;

/**
 * @author Dietmar
 */
public class NotPoolObjectException extends Exception {

	/** Creates a new instance of NotPoolObjectException */
	public NotPoolObjectException() {
		super("The Object does not belong to the Pool!");
	}

}
