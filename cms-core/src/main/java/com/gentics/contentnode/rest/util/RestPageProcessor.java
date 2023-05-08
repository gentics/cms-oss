/*
 * @author tobiassteiner
 * @date Jan 22, 2011
 * @version $Id: RestPageProcessor.java,v 1.1.2.2 2011-02-26 08:57:44 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.util;

import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;

/**
 * Subclasses extend this abstract class If the various elements of a
 * {@link com.gentics.contentnode.rest.model.Page} must be processed, e.g. to
 * verify the validity of the information or to store the contents of the page.
 */
public abstract class RestPageProcessor {
	protected abstract void processDescription(String description) throws NodeException;

	protected abstract void processFileName(String fileName) throws NodeException;

	protected abstract void processName(String name) throws NodeException;

	protected abstract void processPriority(Integer priority) throws NodeException;

	protected abstract void processLanguage(String language) throws NodeException;

	/**
	 * Process the given rest tag
	 * @param tag   Rest tag
	 * @param transaction
	 * @throws NodeException
	 */
	protected abstract void processTag(
			com.gentics.contentnode.rest.model.Tag tag, Transaction transaction) throws NodeException;

	/**
	 * Process the given rest page
	 * 
	 * @param restPage      A rest page object
	 * @param transaction   A started transaction
	 * @throws NodeException
	 */
	public void processRestPage(
			com.gentics.contentnode.rest.model.Page restPage, Transaction transaction) throws NodeException {
		String description = restPage.getDescription();

		if (description != null) {
			processDescription(restPage.getDescription());
		}

		String fileName = restPage.getFileName();

		if (fileName != null) {
			processFileName(fileName);
		}

		String pageName = restPage.getName();

		if (pageName != null) {
			processName(pageName);
		}

		Integer priority = restPage.getPriority();

		if (priority != null) {
			processPriority(priority);
		}

		String language = restPage.getLanguage();

		if (language != null) {
			processLanguage(language);
		}

		Map<String, com.gentics.contentnode.rest.model.Tag> restTags = restPage.getTags();

		if (restTags != null) {
			processTags(restTags, transaction);
		}
	}

	/**
	 * Process a map of rest tags (e.g.: Validation)
	 *
	 * @param tags          A map of rest tags
	 * @param transaction   A started transaction
	 * @throws NodeException
	 */
	protected void processTags(Map<String, com.gentics.contentnode.rest.model.Tag> tags,
			Transaction transaction) throws NodeException {

		for (Map.Entry<String, com.gentics.contentnode.rest.model.Tag> tagEntry : tags.entrySet()) {
			com.gentics.contentnode.rest.model.Tag tag = tagEntry.getValue();
			processTag(tag, transaction);
		}
	}
}
