/*
 * @author tobiassteiner
 * @date Dec 19, 2010
 * @version $Id: PassThroughValidator.java,v 1.1.2.1 2011-02-10 13:43:28 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator.impl;

import java.util.Collection;
import java.util.Collections;

import com.gentics.contentnode.validation.validator.ValidationMessage;
import com.gentics.contentnode.validation.validator.ValidationResult;
import com.gentics.contentnode.validation.validator.Validator;

public class PassThroughValidator implements Validator {
	public PassThroughValidator(PassThroughPolicy policy) {}

	public ValidationResult validate(final String markup) {
		return new ValidationResult() {
			public Collection<ValidationMessage> getMessages() {
				return Collections.emptyList();
			}

			public String getCleanMarkup() {
				return markup;
			}

			public boolean hasErrors() {
				return false;
			}
		};
	}
}
