package com.gentics.contentnode.rest.model.scheduler;

import com.gentics.contentnode.rest.model.response.AbstractItemResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing a scheduler schedule
 */
@XmlRootElement
public class ScheduleResponse extends AbstractItemResponse<ScheduleModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1338010818677274759L;

	/**
	 * Create empty response
	 */
	public ScheduleResponse() {
		super();
	}

	/**
	 * Create response with schedule and info
	 * @param item item
	 * @param responseInfo info
	 */
	public ScheduleResponse(ScheduleModel item, ResponseInfo responseInfo) {
		super(item, responseInfo);
	}
}
