/*
 * @author tobiassteiner
 * @date Jan 2, 2011
 * @version $Id: AbstractValidationResult.java,v 1.1.2.1 2011-02-10 13:43:34 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator;

import java.util.Collection;
import java.util.Collections;

/**
 * A basic immutable implementation of a ValidationResult.
 */
public class ValidationResultImpl implements ValidationResult {
    
	protected final Collection<ValidationMessage> messages;
	protected final String cleanMarkup;

	public ValidationResultImpl(Collection<ValidationMessage> messages, String cleanMarkup) {
		this.messages = Collections.unmodifiableCollection(messages);
		this.cleanMarkup = cleanMarkup;
	}
    
	public String getCleanMarkup() {
		return cleanMarkup;
	}

	public Collection<ValidationMessage> getMessages() {
		return messages;
	}

	public boolean hasErrors() {
		for (ValidationMessage msg : getMessages()) {
			if (msg.isFatal()) {
				return true;
			}
		}
		return false;
	}
    
	public String toString() {
		return new StringBuilder().append("messages: ").append(getMessages().toString()).append("\nclean markup: `").append(getCleanMarkup()).append("'").toString();
	}
}
