package com.gentics.contentnode.rest.model.formtype;

import com.gentics.contentnode.rest.model.response.AbstractItemResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response containing form translations
 */
public class FormGenTranslationsResponse extends AbstractItemResponse<FormGenTranslations> {
	private static final long serialVersionUID = -6851972026294659241L;

	/**
	 * Create empty instance
	 */
	public FormGenTranslationsResponse() {
		super();
	}

	/**
	 * Create instance
	 * @param item translations
	 * @param responseInfo respone info
	 */
	public FormGenTranslationsResponse(FormGenTranslations item, ResponseInfo responseInfo) {
		super(item, responseInfo);
	}
}
