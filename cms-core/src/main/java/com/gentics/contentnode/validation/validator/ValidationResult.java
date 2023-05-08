/*
 * @author tobiassteiner
 * @date Dec 19, 2010
 * @version $Id: ValidationResult.java,v 1.1.2.1 2011-02-10 13:43:34 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator;

import java.util.Collection;

public interface ValidationResult {

	/**
	 * Callers must not make any assumptions about the mutability of the
	 * returned collection.
	 */
	Collection<ValidationMessage> getMessages();
    
	/**
	 * @return true if the validation request could not be fulfilled -
	 *   {@link #getCleanMarkup()} may still return a non-empty string, but with
	 *   possibly important information removed.
	 */
	boolean hasErrors();

	/**
	 * @return A String which in the ideal case would be equal to the
	 *   validated String, but more likely a normalized version that is
	 *   considered safe according to the effective validation policy. 
	 *   This may possibly be the empty string if fatal errors occurred.
	 */
	String getCleanMarkup();
}
