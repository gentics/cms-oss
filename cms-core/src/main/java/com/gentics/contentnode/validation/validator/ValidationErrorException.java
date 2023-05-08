/*
 * @author tobiassteiner
 * @date Jan 22, 2011
 * @version $Id: ValidationErrorException.java,v 1.1.2.1 2011-02-10 13:43:33 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator;

/**
 * {@link Validator}s don't throw this exception, but it may be used by users
 * to indicate to the caller that a failure to validate is an exceptional
 * occurrence.
 */
public class ValidationErrorException extends ValidationException {
	private static final long serialVersionUID = -6794322368755534168L;
    
	public ValidationErrorException(ValidationResult cause) {
		super(cause.toString());
		if (!cause.hasErrors()) {
			throw new IllegalArgumentException(
					"A " + ValidationErrorException.class.getName() + " must be constructed with a " + ValidationResult.class.getName() + " that has some errors.");
		}
	}
}
