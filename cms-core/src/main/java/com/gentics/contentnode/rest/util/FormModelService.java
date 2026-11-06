package com.gentics.contentnode.rest.util;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.rest.model.PublishableContentItem;

/**
 * Interface for the service implementation that transforms {@link Form}s into their REST Model
 */
public interface FormModelService {
	/**
	 * Transform the given form into its REST Model
	 * @param form form
	 * @return REST Model of the form
	 * @throws NodeException
	 */
	PublishableContentItem getForm(Form form) throws NodeException;
}
