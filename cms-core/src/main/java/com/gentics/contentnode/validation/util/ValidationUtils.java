/*
 * @author tobiassteiner
 * @date Jan 22, 2011
 * @version $Id: ValidationUtils.java,v 1.1.2.2 2011-02-26 08:57:44 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.validation.ValidatorFactory;
import com.gentics.contentnode.validation.map.inputchannels.InputChannel;
import com.gentics.contentnode.validation.validator.ValidationError;
import com.gentics.contentnode.validation.validator.ValidationException;
import com.gentics.contentnode.validation.validator.ValidationMessage;
import com.gentics.contentnode.validation.validator.ValidationResult;
import com.gentics.contentnode.validation.validator.ValidationResultImpl;
import com.gentics.contentnode.validation.validator.Validator;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

public class ValidationUtils {

	private final static NodeLogger logger = NodeLogger.getNodeLogger(ValidationUtils.class);
	private static boolean performValidation;



	/**
	 * @return the performvalidation
	 */
	public static boolean isValidationEnabled() {
		return performValidation;
	}

	/**
	 * @param performvalidation the performvalidation to set
	 */
	public static void setValidationEnabled(boolean performvalidation) {
		performValidation = performvalidation;
	}

	static {
		NodeConfigRuntimeConfiguration runtimeConfiguration = NodeConfigRuntimeConfiguration.getDefault();
		NodePreferences nodePreferences = runtimeConfiguration.getNodeConfig().getDefaultPreferences();

		performValidation = nodePreferences.getFeature("validation");
	}

	/**
	 * A convenience method that checks the FEATURE["validation"] and logs an
	 * error if the validation was unsuccessful.
	 * 
	 * @param inputChannel the input channel to validate the input for
	 * @param unsafe the input to validate
	 * @return the result of the validation
	 */
	public static ValidationResult validate(InputChannel inputChannel, final String unsafe) throws ValidationException, TransactionException {
		if (!performValidation) {
			// returns an always successful validation result
			return new ValidationResult() {
				public String getCleanMarkup() {
					return unsafe;
				}

				public Collection<ValidationMessage> getMessages() {
					return Collections.emptyList();
				}

				public boolean hasErrors() {
					return false;
				}
			};
		}

		ValidatorFactory factory = ValidatorFactory.newInstance();
		Validator validator = factory.newValidatorForInputChannel(inputChannel);
		ValidationResult result = validator.validate(unsafe);

		if (result.hasErrors()) {
			logger.error("validation error for " + inputChannel.toString() + ": " + result.getMessages().toString());
		}

		return result;
	}

	/**
	 * Performs strict validation on a string.
	 * 
	 * Strict validation will fail if the validated string is not equal
	 * to the input string.
	 * 
	 * An alternative to strict validation would be to take the clean markup of the
	 * validation result and use that - even though the clean markup returned
	 * in the ValidationResult may not be equal to the input string, it is still
	 * safe from an XSS perspective.
	 * 
	 * Using strict validation is the most appropriate way to validate if the input
	 * String must be validated exactly the way it is.
	 * 
	 * @param inputChannel
	 * 		The channel specifying the context for the validation.
	 * @param unsafe
	 * 		The potentially unsafe text to validate.
	 */
	public static ValidationResult validateStrict(InputChannel inputChannel, String unsafe) throws ValidationException, TransactionException {
		ValidationResult result = validate(inputChannel, unsafe);

		if (!unsafe.trim().equals(result.getCleanMarkup().trim())) {
			ArrayList<ValidationMessage> messages = new ArrayList<ValidationMessage>(result.getMessages());

			messages.add(new ValidationError(new CNI18nString("validation.error.strippedcontent")));
			return new ValidationResultImpl(messages, result.getCleanMarkup());
		}
		return result;
	}

	/**
	 * @param error {@link ValidationResult#hasErrors()} should be true on
	 *   the given error result.
	 * @return a formatted error message (html).
	 */
	public static String formatValidationError(ValidationResult error) {
		StringBuilder builder = new StringBuilder();

		builder.append("<ul>");
		for (ValidationMessage message : error.getMessages()) {
			if (message.isFatal()) {
				builder.append("<li class='error'>").append(message.toString()).append("</li>");
			}
		}
		builder.append("</ul>");
		// TODO: make error message nicer
		return builder.toString();
	}
}
