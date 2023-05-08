/*
 * @author tobiassteiner
 * @date Jan 13, 2011
 * @version $Id: ValidationWarning.java,v 1.1.2.1 2011-02-10 13:43:34 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator;

/**
 * A non-fatal {@link ValidationMessage}. 
 */
public class ValidationWarning extends ValidationMessage {
    
	/**
	 * @see ValidationMessage#ValidationMessage(Object)
	 */
	public ValidationWarning(Object message) {
		super(message);
	}

	/**
	 * @see ValidationMessage#isFatal()
	 */
	@Override
	public boolean isFatal() {
		return false;
	}
}
