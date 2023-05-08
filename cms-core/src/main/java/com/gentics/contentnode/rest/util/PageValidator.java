/*
 * @author tobiassteiner
 * @date Jan 22, 2011
 * @version $Id: PageValidator.java,v 1.1.2.3 2011-03-07 18:42:01 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.validation.map.inputchannels.FileNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.InputChannel;
import com.gentics.contentnode.validation.map.inputchannels.PageDescriptionInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.PageLanguageInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.PageNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.TagPartInputChannel;
import com.gentics.contentnode.validation.util.ValidationUtils;
import com.gentics.contentnode.validation.validator.ValidationException;
import com.gentics.contentnode.validation.validator.ValidationResult;

/**
 * Page Validation class. Is initialized with a Node (from which it gets the
 * validation policy) and will validate the page given in method
 * {@link #processRestPage(com.gentics.contentnode.rest.model.Page)}.
 */
public abstract class PageValidator extends RestPageProcessor {

	/**
	 * List of validation results
	 */
	protected final ArrayList<ValidationResult> results = new ArrayList<ValidationResult>();

	/**
	 * The node the page, used for input channels that enable
	 * node-specific validation.
	 */
	private final Node node;

	/**
	 * Create an instance of the validator to validate pages based on the policy
	 * configured for the given Node
	 * 
	 * @param node
	 *            node
	 * @throws NodeException
	 */
	public PageValidator(Node node) throws NodeException {
		this.node = node;
	}

	/**
	 * Helper method that uses the {@link ValidationUtils} to validate the given unsafe String.
	 * @param inputChannel input channel defines the content allowed in the given string
	 * @param unsafe string to validate
	 * @throws NodeException for unexpected errors during validation
	 */
	private void validateStrict(InputChannel inputChannel, String unsafe) throws NodeException {
		try {
			ValidationResult result = ValidationUtils.validateStrict(inputChannel, unsafe);

			this.addValidationResult(result);
		} catch (ValidationException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public void processDescription(String description) throws NodeException {
		validateStrict(new PageDescriptionInputChannel(node), description);
	}

	@Override
	public void processName(String name) throws NodeException {
		validateStrict(new PageNameInputChannel(node), name);
	}

	@Override
	public void processFileName(String fileName) throws NodeException {
		// TODO: no input channel specific to file names.
		validateStrict(new FileNameInputChannel(node), fileName);
	}

	@Override
	public void processLanguage(String language) throws NodeException {
		// TODO: no input channels specific to language names.
		validateStrict(new PageLanguageInputChannel(node), language);
	}

	@Override
	public void processPriority(Integer priority) throws NodeException {// input is already numeric, which is always OK from the XSS
		// perspective. a separate input channel may be needed, if
		// validation should be done in this case as well.
	}

	/**
	 * Performs a strict validation on the given rest tag
	 * Important: all tags need to have to a valid construct ID set!
	 * 
	 * @param tag           Rest tag
	 * @param transaction   A started transaction
	 * @throws NodeException
	 */
	@Override
	protected void processTag(com.gentics.contentnode.rest.model.Tag tag, Transaction transaction) throws NodeException {
		Map<String, Property> tagProperties = tag.getProperties();

		Integer constructId = tag.getConstructId();
		if (tag.getConstructId() == null) {
			throw new NodeException("Error while validating the tag tag {"
					+ tag.getId() + " " + tag.getName() + "}: constructId has to be set");
		}

		com.gentics.contentnode.object.Construct construct =
				transaction.getObject(com.gentics.contentnode.object.Construct.class, constructId);
		List<Part> parts = construct.getParts();

		for (Part part : parts) {
			Property tagProperty = tagProperties.get(part.getKeyname());
			if (tagProperty == null) {
				// This part doesn't exist, skip it.
				continue;
			}

			String stringValue = tagProperty.getStringValue();
			if (tagProperty.getStringValue() == null) {
				continue;
			}

			ValidationResult result = null;
			try {
				InputChannel inputChannel = new TagPartInputChannel(part);

				result = ValidationUtils.validate(inputChannel, stringValue);
			} catch (ValidationException e) {
				throw new NodeException(e);
			}

			if (result.hasErrors()) {
				throw new NodeException("Validation error for {"
						+ "tag {" + tag.getId() + " " + tag.getName() + "}"
						+ ", part {" + part.getKeyname() + "}}"
						+ " Reason: " + result.getMessages().toString());
			}
		}
	}

	protected abstract void addValidationResult(ValidationResult result);

	public List<ValidationResult> getResults() {
		return results;
	}
}
