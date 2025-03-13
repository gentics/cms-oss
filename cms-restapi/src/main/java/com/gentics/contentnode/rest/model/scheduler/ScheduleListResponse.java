package com.gentics.contentnode.rest.model.scheduler;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing a list of schedules
 */
@XmlRootElement
public class ScheduleListResponse extends AbstractListResponse<ScheduleModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8105489874485471726L;
}
