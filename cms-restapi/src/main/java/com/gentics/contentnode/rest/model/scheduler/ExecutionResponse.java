package com.gentics.contentnode.rest.model.scheduler;

import com.gentics.contentnode.rest.model.response.AbstractItemResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing information about a scheduler execution.
 */
@XmlRootElement
public class ExecutionResponse extends AbstractItemResponse<ExecutionModel> {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1233482807780386760L;

	/**
	 * Create empty response
	 */
	public ExecutionResponse() {
		super();
	}

	/**
	 * Create response with execution and info
	 * @param item item
	 * @param responseInfo info
	 */
	public ExecutionResponse(ExecutionModel item, ResponseInfo responseInfo) {
		super(item, responseInfo);
	}
}
