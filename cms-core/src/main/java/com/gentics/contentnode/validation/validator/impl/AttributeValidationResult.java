/*
 * @author tobiassteiner
 * @date Jan 12, 2011
 * @version $Id: AttributeValidationResult.java,v 1.1.2.1 2011-02-10 13:43:29 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator.impl;

import java.util.Collection;

import com.gentics.contentnode.validation.validator.ValidationMessage;

public class AttributeValidationResult extends AntiSamyValidationResult {
	public AttributeValidationResult(Collection<ValidationMessage> messages, String cleanMarkup) {
		super(messages, cleanMarkup);
	}
}
