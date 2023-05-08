/*
 * @author tobiassteiner
 * @date Jan 22, 2011
 * @version $Id: PageValidatorAndErrorCollector.java,v 1.1.2.2 2011-02-26 08:57:44 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.util;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.validation.validator.ValidationResult;

/**
 * A {@link PageValidator} that is only interested in {@link ValidationResult}s
 * for which {@link ValidationResult#hasErrors()} is true.
 */
public class PageValidatorAndErrorCollector extends PageValidator {

	public PageValidatorAndErrorCollector(Node node) throws NodeException {
		super(node);
	}

	@Override
	protected void addValidationResult(ValidationResult result) {
		if (result.hasErrors()) {
			results.add(result);
		}
	}
}
