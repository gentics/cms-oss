package com.gentics.contentnode.rest.model.formtype;

import com.gentics.contentnode.rest.model.response.AbstractItemResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response containing a single form type configuration
 */
public class FormTypeConfigurationResponse extends AbstractItemResponse<FormTypeConfigurationModel> {
	private static final long serialVersionUID = 5765236127074118506L;

	/**
	 * Creator
	 * @param item item
	 * @param responseInfo response info
	 */
	public FormTypeConfigurationResponse(FormTypeConfigurationModel item, ResponseInfo responseInfo) {
		super(item, responseInfo);
	}
}
