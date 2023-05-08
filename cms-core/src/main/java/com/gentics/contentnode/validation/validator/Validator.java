/*
 * @author tobiassteiner
 * @date Dec 19, 2010
 * @version $Id: Validator.java,v 1.1.2.1 2011-02-10 13:43:33 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator;

public interface Validator {
	ValidationResult validate(String markup) throws ValidationException;
}
