/*
 * @author tobiassteiner
 * @date Dec 6, 2010
 * @version $Id: AntiSamyValidationResult.java,v 1.1.2.1 2011-02-10 13:43:30 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator.impl;

import java.util.Collection;

import com.gentics.contentnode.validation.validator.ValidationResultImpl;
import com.gentics.contentnode.validation.validator.ValidationMessage;

/**
 * The result of validating a string with the AntiSamyValidator.
 */
public class AntiSamyValidationResult extends ValidationResultImpl {
    
	public AntiSamyValidationResult(Collection<ValidationMessage> messages, String cleanMarkup) {
		super(messages, cleanMarkup);
	}
}
