package com.gentics.contentnode.rest.model.scheduler;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractItemResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response containing an item
 */
@XmlRootElement
public class TaskResponse extends AbstractItemResponse<TaskModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 7072898546817139072L;

	/**
	 * Create empty response
	 */
	public TaskResponse() {
		super();
	}

	/**
	 * Create response with item and info
	 * @param item item
	 * @param responseInfo info
	 */
	public TaskResponse(TaskModel item, ResponseInfo responseInfo) {
		super(item, responseInfo);
	}
}
