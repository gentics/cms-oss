package com.gentics.contentnode.rest.resource.scheduler;

import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.request.scheduler.SuspendRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.scheduler.JobsResponse;
import com.gentics.contentnode.rest.model.response.scheduler.SchedulerStatusResponse;
import com.gentics.contentnode.rest.model.scheduler.ExecutionListResponse;
import com.gentics.contentnode.rest.model.scheduler.ExecutionResponse;
import com.gentics.contentnode.rest.model.scheduler.ScheduleListResponse;
import com.gentics.contentnode.rest.model.scheduler.ScheduleModel;
import com.gentics.contentnode.rest.model.scheduler.ScheduleResponse;
import com.gentics.contentnode.rest.model.scheduler.TaskListResponse;
import com.gentics.contentnode.rest.model.scheduler.TaskModel;
import com.gentics.contentnode.rest.model.scheduler.TaskResponse;
import com.gentics.contentnode.rest.resource.parameter.ExecutionFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SchedulerJobFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for managing the scheduler
 */
@Path("scheduler")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions on the scheduler."),
})
public interface SchedulerResource {
	/**
	 * Get the scheduler status and optionally restart the scheduler executor if
	 * it is not running.
	 * @param fixExecutor Whether to restart the scheduler executor if it is not
	 * 	running currently.
	 * @return status
	 * @throws Exception
	 */
	@GET
	@Path("/status")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Scheduler status is returned.")
	})
	SchedulerStatusResponse status(@QueryParam("fixExecutor") boolean fixExecutor) throws Exception;

	/**
	 * Suspend the scheduler
	 * @param request suspend request
	 * @return status
	 */
	@PUT
	@Path("/suspend")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Scheduler is suspended."),
		@ResponseCode(code = 405, condition = "Feature suspend_scheduler is not activated.")
	})
	SchedulerStatusResponse suspend(SuspendRequest request) throws Exception;

	/**
	 * Resume the scheduler
	 * @return status
	 * @throws Exception
	 */
	@PUT
	@Path("/resume")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Scheduler is resumed."),
		@ResponseCode(code = 405, condition = "Feature suspend_scheduler is not activated.")
	})
	SchedulerStatusResponse resume() throws Exception;

	/**
	 * List scheduler jobs
	 * @param filter filter parameter bean
	 * @param sorting sorting parameter bean
	 * @param paging paging parameter bean
	 * @param jobFilter job filter parameter bean
	 * @return list of jobs
	 * @throws Exception
	 */
	@GET
	@Path("/jobs")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Jobs are returned.")
	})
	JobsResponse jobs(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam SchedulerJobFilterParameterBean jobFilter) throws Exception;

	/**
	 * List scheduler tasks.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>command</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>command</code></li>
	 * <li><code>cdate</code></li>
	 * <li><code>edate</code></li>
	 * </ul>
	 * @param filter filter parameter bean
	 * @param sorting sorting parameter bean
	 * @param paging paging parameter bean
	 * @param perms permissions parameter bean
	 * @return response containing a list of tasks
	 * @throws Exception
	 * @HTTP 200 The list of tasks is returned.
	 */
	@GET
	@Path("/task")
	TaskListResponse listTasks(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
		@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Create a new task
	 * @param task task
	 * @return response containing the created task
	 * @HTTP 201 The task was created
	 * @throws Exception
	 */
	@POST
	@Path("/task")
	TaskResponse createTask(TaskModel task) throws Exception;

	/**
	 * Load a task
	 * @param taskId task ID
	 * @return response containing the task
	 * @HTTP 200 The task is returned
	 * @HTTP 404 The task with given ID does not exist
	 * @throws Exception
	 */
	@GET
	@Path("/task/{id}")
	TaskResponse getTask(@PathParam("id") String taskId) throws Exception;

	/**
	 * Update a task
	 * @param taskId task ID
	 * @param task task data to update
	 * @return updated task
	 * @HTTP 200 The task was updated
	 * @HTTP 404 The task with given ID does not exist
	 * @throws Exception
	 */
	@PUT
	@Path("/task/{id}")
	TaskResponse updateTask(@PathParam("id") String taskId, TaskModel task) throws Exception;

	/**
	 * Delete a task
	 * @param taskId task ID
	 * @return response
	 * @HTTP 200 The task was deleted
	 * @HTTP 404 The task with given ID does not exist
	 * @throws Exception
	 */
	@DELETE
	@Path("/task/{id}")
	GenericResponse deleteTask(@PathParam("id") String taskId) throws Exception;

	/**
	 * List scheduler schedules.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>taskId</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>taskId</code></li>
	 * <li><code>cdate</code></li>
	 * <li><code>edate</code></li>
	 * </ul>
	 * @param filter filter parameter bean
	 * @param sorting sorting parameter bean
	 * @param paging paging parameter bean
	 * @param perms permissions parameter bean
	 * @param embed optionally embed the referenced object (task)
	 * @param jobFilter job filter parameter bean
	 * @return response containing a list of schedules
	 * @throws Exception
	 * @HTTP 200 The list of schedules is returned.
	 */
	@GET
	@Path("/schedule")
	ScheduleListResponse listSchedules(
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms,
			@BeanParam EmbedParameterBean embed,
			@BeanParam SchedulerJobFilterParameterBean jobFilter
	) throws Exception;

	/**
	 * Create a new Schedule
	 * @param schedule schedule
	 * @return response containing the created schedule
	 * @HTTP 201 The schedule was created
	 * @throws Exception
	 */
	@POST
	@Path("/schedule")
	ScheduleResponse createSchedule(ScheduleModel schedule) throws Exception;

	/**
	 * Load a schedule
	 * @param scheduleId schedule ID
	 * @return response containing the schedule
	 * @HTTP 200 The schedule is returned
	 * @HTTP 404 The schedule with given ID does not exist
	 * @throws Exception
	 */
	@GET
	@Path("/schedule/{id}")
	ScheduleResponse getSchedule(@PathParam("id") String scheduleId) throws Exception;

	/**
	 * Update a schedule
	 * @param scheduleId schedule ID
	 * @param schedule schedule to update
	 * @return updated task
	 * @HTTP 200 The schedule was updated
	 * @HTTP 404 The schedule with given ID does not exist
	 * @throws Exception
	 */
	@PUT
	@Path("/schedule/{id}")
	ScheduleResponse updateSchedule(@PathParam("id") String scheduleId, ScheduleModel schedule) throws Exception;

	/**
	 * Delete a schedule
	 * @param scheduleId schedule ID
	 * @return response
	 * @HTTP 200 The schedule was deleted
	 * @HTTP 404 The schedule with given ID does not exist
	 * @throws Exception
	 */
	@DELETE
	@Path("/schedule/{id}")
	GenericResponse deleteSchedule(@PathParam("id") String scheduleId) throws Exception;

	/**
	 * Execute the specified schedule now.
	 *
	 * <p>
	 *     There is no check if the schedule schould be executed, besides a
	 *     check if it is active.
	 * </p>
	 *
	 * @param scheduleId The schedule to execute
	 * @return response
	 * @HTTP 200 Execution of the schedule was startet
	 * @HTTP 404 The schedule with given ID does not exist
	 */
	@POST
	@Path("/schedule/{id}/execute")
	GenericResponse executeSchedule(@PathParam("id") String scheduleId) throws Exception;

	/**
	 * List all executions of the given schedule which match the filters.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>scheduleId</code></li>
	 * <li><code>log</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>startTime</code></li>
	 * <li><code>endTime</code></li>
	 * <li><code>duration</code></li>
	 * <li><code>result</code></li>
	 * </ul>
	 * @param scheduleId schedule ID
	 * @param filter filter parameter bean
	 * @param sorting sorting parameter bean
	 * @param paging  paging parameter bean
	 * @param executionFilterParameterBean execution specific filter parameter bean
	 * @param embed optionally embed the referenced object (schedule)
	 * @return Response containing the list of executions
	 * @HTTP 200 The list of executions is returned.
	 * @throws Exception
	 */
	@GET
	@Path("/schedule/{id}/execution")
	ExecutionListResponse listExecutions(
			@PathParam("id") String scheduleId,
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam ExecutionFilterParameterBean executionFilterParameterBean,
			@BeanParam EmbedParameterBean embed)
			throws Exception;

	/**
	 * Load an execution
	 * @param executionId execution ID
	 * @return response containing the execution
	 * @HTTP 200 The schedule is returned
	 * @HTTP 404 The schedule with given ID does not exist
	 * @throws Exception
	 */
	@GET
	@Path("/execution/{id}")
	ExecutionResponse getExecution(@PathParam("id") String executionId) throws Exception;
}
