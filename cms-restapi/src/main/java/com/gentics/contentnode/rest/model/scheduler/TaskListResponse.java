package com.gentics.contentnode.rest.model.scheduler;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of tasks
 */
@XmlRootElement
public class TaskListResponse extends AbstractListResponse<TaskModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 9005835912389449699L;
}
