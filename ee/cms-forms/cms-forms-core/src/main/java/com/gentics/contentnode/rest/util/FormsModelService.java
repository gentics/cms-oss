package com.gentics.contentnode.rest.util;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.rest.model.PublishableContentItem;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;

/**
 * Implementation of {@link FormModelService} that uses {@link FormResourceImpl#TRANSFORM2REST}
 */
public class FormsModelService implements FormModelService {
	@Override
	public PublishableContentItem getForm(Form form) throws NodeException {
		return FormResourceImpl.TRANSFORM2REST.apply(form);
	}
}
