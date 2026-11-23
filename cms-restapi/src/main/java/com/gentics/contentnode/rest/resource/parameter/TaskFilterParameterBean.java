package com.gentics.contentnode.rest.resource.parameter;

import java.util.Set;

import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;

/**
 * Task filter parameter bean
 */
public class TaskFilterParameterBean {
	/**
	 * Statuses (default: <code>open</code>, <code>inProgress</code>)
	 */
	@QueryParam("status")
	public Set<TaskStatus> status;

	/**
	 * Sources
	 */
	@QueryParam("source")
	public Set<TaskSource> source;

	/**
	 * Type
	 */
	@QueryParam("type")
	public String type;

	/**
	 * Node of the objects
	 */
	@QueryParam("nodeId")
	public Integer nodeId;

	/**
	 * GenAIx session ID
	 */
	@QueryParam("genaixSessionId")
	public String genaixSessionId;

	/**
	 * External key
	 */
	@QueryParam("externalKey")
	public String externalKey;

	/**
	 * "me" for the tasks the current user sent to others
	 */
	@QueryParam("assignedBy")
	public String assignedBy;
}
