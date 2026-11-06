package com.gentics.contentnode.rest.model.form;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractItemResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.StagingStatus;

/**
 * Response containing an item
 */
@XmlRootElement
public class FormResponse extends AbstractItemResponse<FormModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1456891600922433600L;

	/**
	 * Staging package inclusion status
	 */
	private StagingStatus stagingStatus;

	/**
	 * Create empty response
	 */
	public FormResponse() {
		super();
	}

	/**
	 * Create response with item and info
	 * @param item item
	 * @param responseInfo info
	 */
	public FormResponse(FormModel item, ResponseInfo responseInfo) {
		super(item, responseInfo);
	}

	public StagingStatus getStagingStatus() {
		return stagingStatus;
	}

	public void setStagingStatus(StagingStatus stagingStatus) {
		this.stagingStatus = stagingStatus;
	}
}
