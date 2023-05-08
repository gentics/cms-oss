/*
 * @author tobiassteiner
 * @date Jan 13, 2011
 * @version $Id: AntiSamyValidator.java,v 1.1.2.1 2011-02-10 13:43:30 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator.impl;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;

import com.gentics.contentnode.validation.util.ErrorMessagesView;
import com.gentics.contentnode.validation.util.NodeTagUtils;
import com.gentics.contentnode.validation.validator.ValidationException;
import com.gentics.contentnode.validation.validator.ValidationMessage;

public class AntiSamyValidator extends AbstractAntiSamyValidator {
	public AntiSamyValidator(AntiSamyPolicy config, Policy policy, Locale locale) {
		super(config, policy, locale);
	}

	public AntiSamyValidationResult validate(String markup) throws ValidationException {
		if (config.getConvertNodeTags()) {
			markup = NodeTagUtils.ungtxtifyNodeTags(markup);
		}
		CleanResults cr;

		try {
			cr = antiSamy.scan(markup, config.domMode ? AntiSamy.DOM : AntiSamy.SAX);
		} catch (PolicyException e) {
			throw new ValidationException(e);
		} catch (ScanException e) {
			throw new ValidationException(e);
		}

		// TODO we should also ensure that no node tags occur in comments and cdata sections
		// which would otherwise escape validation.

		String cleanMarkup = cr.getCleanHTML();

		if (config.getConvertNodeTags()) {

			cleanMarkup = NodeTagUtils.gtxtifyNodeTags(cleanMarkup);
		}

		Collection<ValidationMessage> errors = new ErrorMessagesView(cr.getErrorMessages());

		return new AntiSamyValidationResult(errors, cleanMarkup);
	}
}
