package com.gentics.contentnode.rest.model.form;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractStagingStatusListResponse;

/**
 * Response containing a list of forms
 */
@XmlRootElement
public class FormListResponse extends AbstractStagingStatusListResponse<FormModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5581831898873935470L;
}
