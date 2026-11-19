package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.task.UserTaskCountResponse;
import com.gentics.contentnode.rest.model.task.UserTaskListResponse;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.model.task.UserTaskResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for the tasks of users
 */
@Path("/task")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
})
public interface TaskResource {
	/**
	 * Get the tasks of the current user and the user's groups
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param taskFilter task filter parameters
	 * @return list of tasks
	 * @throws Exception
	 * @HTTP 200 The list of tasks is returned.
	 */
	@GET
	UserTaskListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam TaskFilterParameterBean taskFilter) throws Exception;

	/**
	 * Count the open tasks of the current user per source
	 * @return counts
	 * @throws Exception
	 * @HTTP 200 The counts are returned.
	 */
	@GET
	@Path("/count")
	UserTaskCountResponse count() throws Exception;

	/**
	 * Load a task
	 * @param id task ID
	 * @return task
	 * @throws Exception
	 * @HTTP 200 The task is returned.
	 * @HTTP 404 The task does not exist or is not visible.
	 */
	@GET
	@Path("/{id}")
	UserTaskResponse get(@PathParam("id") String id) throws Exception;

	/**
	 * Create a stored task, or update the task with the same source and external key
	 * @param task task
	 * @return created or updated task
	 * @throws Exception
	 * @HTTP 200 The task was created or updated.
	 * @HTTP 400 Data is missing, or recipient or objects are not allowed.
	 * @HTTP 409 The source is virtual.
	 */
	@POST
	UserTaskResponse create(UserTaskModel task) throws Exception;

	/**
	 * Update a stored task, null fields stay unchanged
	 * @param id task ID
	 * @param task changed data
	 * @return updated task
	 * @throws Exception
	 * @HTTP 200 The task was updated.
	 * @HTTP 400 The recipient is not allowed.
	 * @HTTP 403 Only the creator may change other fields than the status, only the user the task is assigned to may change the status.
	 * @HTTP 404 The task does not exist or is not visible.
	 * @HTTP 409 The task is virtual.
	 */
	@PUT
	@Path("/{id}")
	UserTaskResponse update(@PathParam("id") String id, UserTaskModel task) throws Exception;

	/**
	 * Delete a stored task
	 * @param id task ID
	 * @return response
	 * @throws Exception
	 * @HTTP 200 The task was deleted.
	 * @HTTP 403 Only the creator may delete the task.
	 * @HTTP 404 The task does not exist or is not visible.
	 * @HTTP 409 The task is virtual.
	 */
	@DELETE
	@Path("/{id}")
	GenericResponse delete(@PathParam("id") String id) throws Exception;

	/**
	 * Assign a group task to the current user, also when another member of the group claimed it
	 * @param id task ID
	 * @return updated task
	 * @throws Exception
	 * @HTTP 200 The task was claimed.
	 * @HTTP 404 The task does not exist or is not visible.
	 * @HTTP 409 The task is no group task, is closed or is already assigned to the current user.
	 */
	@POST
	@Path("/{id}/claim")
	UserTaskResponse claim(@PathParam("id") String id) throws Exception;

	/**
	 * Give a claimed task back to its group
	 * @param id task ID
	 * @return updated task
	 * @throws Exception
	 * @HTTP 200 The task was given back.
	 * @HTTP 404 The task does not exist or is not visible.
	 * @HTTP 409 The task was not claimed by the current user.
	 */
	@POST
	@Path("/{id}/unclaim")
	UserTaskResponse unclaim(@PathParam("id") String id) throws Exception;
}
