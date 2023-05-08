/*
 * @author tobiassteiner
 * @date Dec 19, 2010
 * @version $Id: ValidatorInstantiationException.java,v 1.1.2.1 2011-02-10 13:43:34 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator;

public class ValidatorInstantiationException extends ValidationException {    
	private static final long serialVersionUID = -8905439018555082698L;

	public ValidatorInstantiationException(Throwable cause) {
		super(cause);
	}
    
	public ValidatorInstantiationException(String cause) {
		super(cause);
	}
}
