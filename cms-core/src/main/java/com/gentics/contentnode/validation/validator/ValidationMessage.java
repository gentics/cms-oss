/*
 * @author tobiassteiner
 * @date Jan 13, 2011
 * @version $Id: ValidationMessage.java,v 1.1.2.1 2011-02-10 13:43:33 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator;

public abstract class ValidationMessage {
	protected final Object message;
    
	/**
	 * @param message The message must have a resonable implementation of {@link Object#toString()}.
	 */
	public ValidationMessage(Object message) {
		this.message = message;
	}
    
	/**
	 * @return whether this message explains why a validation request
	 *   could not be explained. The presence of such a message indicates
	 *   a validation failure. The absence of such a message indicates
	 *   that validation succeeded.
	 */
	public abstract boolean isFatal();

	/**
	 * @return the string representation of the Object this message
	 *   was constructed with. This is assumed to contain HTML which
	 *   should not be escaped.
	 */
	public String toString() {
		return message.toString();
	}
}
