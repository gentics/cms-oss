/*
 * @author tobiassteiner
 * @date Jan 2, 2011
 * @version $Id: ValidationException.java,v 1.1.2.1 2011-02-10 13:43:34 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator;

@SuppressWarnings("serial")
public class ValidationException extends RuntimeException {
	public ValidationException(Throwable cause) {
		super(cause);
	}

	public ValidationException(String cause) {
		super(cause);
	}
}
