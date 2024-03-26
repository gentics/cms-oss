package com.gentics.contentnode.rest.resource.impl.scheduler;

import static com.gentics.contentnode.rest.util.MiscUtils.NEW_FIELD_CHECKER;
import static com.gentics.contentnode.rest.util.MiscUtils.checkBodyWithFunction;
import static com.gentics.contentnode.rest.util.MiscUtils.load;
import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;
import static com.gentics.contentnode.rest.util.RequestParamHelper.embeddedParameterContainsAttribute;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.PrepareStatement;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SchedulerFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.scheduler.SuspendRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.scheduler.JobStatus;
import com.gentics.contentnode.rest.model.response.scheduler.JobsResponse;
import com.gentics.contentnode.rest.model.response.scheduler.SchedulerExecutorStatus;
import com.gentics.contentnode.rest.model.response.scheduler.SchedulerStatus;
import com.gentics.contentnode.rest.model.response.scheduler.SchedulerStatusResponse;
import com.gentics.contentnode.rest.model.scheduler.ExecutionListResponse;
import com.gentics.contentnode.rest.model.scheduler.ExecutionModel;
import com.gentics.contentnode.rest.model.scheduler.ExecutionResponse;
import com.gentics.contentnode.rest.model.scheduler.ScheduleListResponse;
import com.gentics.contentnode.rest.model.scheduler.ScheduleModel;
import com.gentics.contentnode.rest.model.scheduler.ScheduleResponse;
import com.gentics.contentnode.rest.model.scheduler.ScheduleStatus;
import com.gentics.contentnode.rest.model.scheduler.TaskListResponse;
import com.gentics.contentnode.rest.model.scheduler.TaskModel;
import com.gentics.contentnode.rest.model.scheduler.TaskResponse;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.ExecutionFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SchedulerJobFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.scheduler.SchedulerResource;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

@Produces({ MediaType.APPLICATION_JSON })
@Path("scheduler")
@Authenticated
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
public class SchedulerResourceImpl implements SchedulerResource {

	/** Select additional information about schedules (runs, average time and last execution). **/
	private final String SELECT_SCHEDULE_INFO =
		"SELECT s.id, runs, average_time, e.* " +
		"FROM scheduler_schedule s LEFT JOIN scheduler_execution e " +
			"ON s.scheduler_execution_id = e.id";

	@GET
	@Path("/status")
	@Override
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	public SchedulerStatusResponse status(@QueryParam("fixExecutor") boolean fixExecutor) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			SchedulerStatusResponse response = getStatus();
			boolean executorRunning = getSchedulerFactory().checkExecutor(fixExecutor);
			SchedulerExecutorStatus executorStatus = executorRunning
				? SchedulerExecutorStatus.RUNNING
				: fixExecutor ? SchedulerExecutorStatus.RESTARTED : SchedulerExecutorStatus.NOT_RUNNING;

			response.setExecutorStatus(executorStatus);
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			trx.success();
			return response;
		}
	}

	@PUT
	@Path("/suspend")
	@RequiredFeature(Feature.SUSPEND_SCHEDULER)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_SCHEDULER_SUSPEND)
	@Override
	public SchedulerStatusResponse suspend(SuspendRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Set<Integer> allowRunIds = Optional.ofNullable(request)
				.map(SuspendRequest::getAllowRun)
				.orElse(Collections.emptySet());

			getSchedulerFactory().suspend(allowRunIds);

			SchedulerStatusResponse response = getStatus();
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			trx.success();
			return response;
		}
	}

	@PUT
	@Path("/resume")
	@RequiredFeature(Feature.SUSPEND_SCHEDULER)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_SCHEDULER_SUSPEND)
	@Override
	public SchedulerStatusResponse resume() throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			getSchedulerFactory().resume();

			SchedulerStatusResponse response = getStatus();
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/jobs")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	public JobsResponse jobs(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam SchedulerJobFilterParameterBean jobFilter) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			JobsResponse response = ListBuilder.from(DBUtils.select("SELECT jr.id, jr.starttime, jr.endtime, jr.returnvalue, j.name, j.id job_id, j.status FROM job j LEFT JOIN jobrun jr ON j.last_valid_jobrun_id = jr.id", rs -> {
				List<ResolvableJobStatus> jobs = new ArrayList<>();
				while (rs.next()) {
					ResolvableJobStatus job = new ResolvableJobStatus();
					job.setId(rs.getInt("job_id"))
						.setActive(rs.getBoolean("status"))
						.setStart(rs.getInt("starttime"))
						.setEnd(rs.getInt("endtime"))
						.setReturnValue(rs.getInt("returnvalue"))
						.setName(rs.getString("name"));
					jobs.add(job);
				}
				return jobs;
			}), j -> (JobStatus) j)
				.filter(job -> TypePerms.job.canView(job.getId()))
				.filter(job -> {
					if (jobFilter != null && jobFilter.failed != null) {
						if (ObjectTransformer.getBoolean(jobFilter.failed, false)) {
							return job.getReturnValue() != 0;
						} else {
							return job.getReturnValue() == 0;
						}
					} else {
						return true;
					}
				})
				.filter(job -> {
					if (jobFilter != null && jobFilter.active != null) {
						return job.isActive() == ObjectTransformer.getBoolean(jobFilter.active, true);
					} else {
						return true;
					}
				})
				.filter(ResolvableFilter.get(filter, "id", "name"))
				.sort(ResolvableComparator.get(sorting, "id", "name", "start", "end", "returnValue", "active"))
				.page(paging)
				.to(new JobsResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/task")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	public TaskListResponse listTasks(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			List<SchedulerTask> tasks = t.getObjects(SchedulerTask.class, DBUtils.select("SELECT id FROM scheduler_task", DBUtils.IDS));

			TaskListResponse response = ListBuilder.from(tasks, SchedulerTask.TRANSFORM2REST)
				.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
				.filter(ResolvableFilter.get(filter, "id", "name", "description", "command"))
				.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
				.sort(ResolvableComparator.get(sorting, "id", "name", "description", "command", "cdate", "edate"))
				.page(paging)
				.to(new TaskListResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/task")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_SCHEDULER_TASK_UPDATE)
	public TaskResponse createTask(TaskModel task) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			// check input
			checkBodyWithFunction(task, NEW_FIELD_CHECKER, fm -> Pair.of(I18NHelper.get("name"), fm.getName()),
					fm -> Pair.of(I18NHelper.get("command"), fm.getCommand()));

			if (!t.canCreate(null, SchedulerTask.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("scheduler_task.nopermission"), null, null,
						SchedulerTask.TYPE_SCHEDULER_TASK, 0, PermType.create);
			}

			// create the task (which will external always)
			SchedulerTask nodeTask = SchedulerTask.REST2NODE.apply(task, t.createObject(SchedulerTask.class));
			nodeTask.setInternal(false);
			nodeTask.validate();
			nodeTask.save();

			// set view permission on the scheduler task
			SchedulerTask.setInitialPermission(nodeTask);

			TaskResponse response = new TaskResponse(SchedulerTask.TRANSFORM2REST.apply(nodeTask.reload()),
					ResponseInfo.ok("Successfully created task"));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/task/{id}")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	public TaskResponse getTask(@PathParam("id") String taskId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			SchedulerTask task = load(SchedulerTask.class, taskId);

			TaskResponse response = new TaskResponse(SchedulerTask.TRANSFORM2REST.apply(task),
					ResponseInfo.ok("Successfully loaded task"));

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/task/{id}")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_SCHEDULER_TASK_UPDATE)
	public TaskResponse updateTask(@PathParam("id") String taskId, TaskModel task) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SchedulerTask update = load(SchedulerTask.class, taskId, ObjectPermission.edit);

			if (update.isInternal() && !StringUtils.isEmpty(task.getCommand())
					&& !StringUtils.isEqual(task.getCommand(), update.getCommand())) {
				throw new RestMappedException(I18NHelper.get("scheduler_task.update.internal")).setMessageType(Message.Type.CRITICAL)
					.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			update = SchedulerTask.REST2NODE.apply(task, t.getObject(update, true));
			update.validate();
			update.save();

			TaskResponse response = new TaskResponse(SchedulerTask.TRANSFORM2REST.apply(update.reload()),
					ResponseInfo.ok("Successfully updated task"));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/task/{id}")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_SCHEDULER_TASK_UPDATE)
	public GenericResponse deleteTask(@PathParam("id") String taskId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SchedulerTask toDelete = load(SchedulerTask.class, taskId, ObjectPermission.delete);

			if (toDelete.isInternal()) {
				throw new RestMappedException(I18NHelper.get("scheduler_task.delete.internal")).setMessageType(Message.Type.CRITICAL)
					.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			t.getObject(toDelete, true).delete();

			trx.success();
			return new GenericResponse(null, ResponseInfo.ok("Successfully deleted task"));
		}
	}

	@Override
	@GET
	@Path("/schedule")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	public ScheduleListResponse listSchedules(
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms,
			@BeanParam EmbedParameterBean embed
	) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			Map<Integer, ScheduleModel> scheduleModels = DBUtils.select(SELECT_SCHEDULE_INFO, this::getAdditionalScheduleInfo);
			List<SchedulerSchedule> schedules = t.getObjects(SchedulerSchedule.class, scheduleModels.keySet());

			ScheduleListResponse response = ListBuilder.from(
					schedules,
					schedule -> {
						ScheduleModel scheduleModel = scheduleModels.get(schedule.getId());
						return SchedulerSchedule.TRANSFORM2REST.apply(schedule)
								.setStatus(getScheduleStatus(schedule.getId()))
								.setRuns(scheduleModel.getRuns())
								.setAverageTime(scheduleModel.getAverageTime())
								.setLastExecution(scheduleModel.getLastExecution());
					})
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "name", "description", "taskId"))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "id", "name", "description", "taskId", "cdate", "edate"))
					.embed(embed, "task", SchedulerSchedule.EMBED_TASK)
					.page(paging)
					.to(new ScheduleListResponse());

			trx.success();

			return response;
		}
	}

	@Override
	@POST
	@Path("/schedule")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_SCHEDULER_SCHEDULE_UPDATE)
	public ScheduleResponse createSchedule(ScheduleModel scheduleModel) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			// check input
			checkBodyWithFunction(
				scheduleModel,
				NEW_FIELD_CHECKER,
				fm -> Pair.of(I18NHelper.get("name"), fm.getName()),
				fm -> Pair.of(I18NHelper.get("task_id"), fm.getTaskId()),
				fm -> Pair.of(I18NHelper.get("schedule_data"), fm.getScheduleData()));

			if (!t.canCreate(null, SchedulerSchedule.class, null)) {
				throw new InsufficientPrivilegesException(
					I18NHelper.get("scheduler_schedule.nopermission"),
					null,
					null,
					SchedulerSchedule.TYPE_SCHEDULER_SCHEDULE, 0, PermType.create);
			}

			SchedulerSchedule schedule = SchedulerSchedule.REST2NODE.apply(scheduleModel, t.createObject(SchedulerSchedule.class));

			schedule.save();

			// set view permission on the scheduler task
			SchedulerSchedule.setInitialPermission(schedule);

			ScheduleResponse response = new ScheduleResponse(
				SchedulerSchedule.TRANSFORM2REST.apply(schedule.reload()),
				ResponseInfo.ok("Successfully created schedule"));

			trx.success();

			return response;
		}
	}

	@Override
	@GET
	@Path("/schedule/{id}")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	public ScheduleResponse getSchedule(@PathParam("id") String scheduleId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			SchedulerSchedule schedule = load(SchedulerSchedule.class, scheduleId);
			ScheduleModel additionalInfo = DBUtils.select(
				SELECT_SCHEDULE_INFO + " WHERE s.id = ?",
				stmt -> stmt.setInt(1, schedule.getId()),
				this::getAdditionalScheduleInfo)
				.get(schedule.getId());
			ScheduleResponse response = new ScheduleResponse(
				SchedulerSchedule.TRANSFORM2REST.apply(schedule)
					.setStatus(getScheduleStatus(schedule.getId()))
					.setRuns(additionalInfo.getRuns())
					.setAverageTime(additionalInfo.getAverageTime())
					.setLastExecution(additionalInfo.getLastExecution()),
				ResponseInfo.ok("Successfully loaded schedule"));

			trx.success();

			return response;
		}
	}

	@Override
	@PUT
	@Path("/schedule/{id}")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_SCHEDULER_SCHEDULE_UPDATE)
	public ScheduleResponse updateSchedule(@PathParam("id") String scheduleId, ScheduleModel schedule) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SchedulerSchedule update = load(SchedulerSchedule.class, scheduleId, ObjectPermission.edit);

			update = SchedulerSchedule.REST2NODE.apply(schedule, t.getObject(update, true));
			update.save();

			ScheduleResponse response = new ScheduleResponse(
				SchedulerSchedule.TRANSFORM2REST.apply(update.reload()),
				ResponseInfo.ok("Successfully updated schedule"));

			trx.success();

			return response;
		}
	}

	@Override
	@DELETE
	@Path("/schedule/{id}")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_SCHEDULER_TASK_UPDATE)
	public GenericResponse deleteSchedule(@PathParam("id") String scheduleId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SchedulerSchedule toDelete = load(SchedulerSchedule.class, scheduleId, ObjectPermission.delete);

			t.getObject(toDelete, true).delete();
			trx.success();

			return new GenericResponse(null, ResponseInfo.ok("Successfully deleted schedule"));
		}
	}

	@Override
	@POST
	@Path("/schedule/{id}/execute")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	public GenericResponse executeSchedule(@PathParam("id") String scheduleId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			SchedulerSchedule schedule = trx.getTransaction().getObject(SchedulerSchedule.class, scheduleId);

			if (schedule == null) {
				throw new EntityNotFoundException(String.format("Schedule %s not found", scheduleId), "scheduler_schedule.notfound");
			}

			if (schedule.isActive()) {
				try {
					DistributionUtil.call(new ExecuteScheduleTask(schedule));
				} catch (Exception e) {
					I18nString message = new CNI18nString("rest.general.error");

					return new GenericResponse(
						new Message(Message.Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.FAILURE, "Error while executing schedules."));
				}
			}

			trx.success();

			return new GenericResponse(null, ResponseInfo.ok("Execution of schedule was started"));
		}
	}

	@Override
	@GET
	@Path("/schedule/{id}/execution")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	public ExecutionListResponse listExecutions(
			@PathParam("id") String id,
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam ExecutionFilterParameterBean executionFilterParameterBean,
			@BeanParam EmbedParameterBean embed
	) throws NodeException {
		int scheduleId;

		try {
			scheduleId = Integer.parseInt(id);
		} catch (NumberFormatException e) {
			throw new EntityNotFoundException("Invalid schedule ID", "scheduler_schedule.notfound");
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			SchedulerExecutionQuery query = new SchedulerExecutionQuery();
			query.query(scheduleId, executionFilterParameterBean, filter, sorting, paging);
			List<ExecutionModel> executionModels = query.get();

			if (embeddedParameterContainsAttribute(embed, "schedule")) {
				SchedulerSchedule schedule = load(SchedulerSchedule.class, id);

				if (schedule != null) {
					for (ExecutionModel executionModel : executionModels) {
						ScheduleModel scheduleModel = SchedulerSchedule.TRANSFORM2REST.apply(schedule);
						executionModel.setSchedule(scheduleModel);
					}
				}
			}

			ExecutionListResponse response = new ExecutionListResponse();
			response.setNumItems(query.count());
			response.setItems(executionModels);
			response.setHasMoreItems(query.hasMore(response.getNumItems()));
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Executions loaded"));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/execution/{id}")
	@RequiredPerm(type = PermHandler.TYPE_SCHEDULER_ADMIN, bit = PermHandler.PERM_VIEW)
	public ExecutionResponse getExecution(@PathParam("id") String executionId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			ExecutionModel execution = DBUtils.select(
				"SELECT * FROM scheduler_execution WHERE id = ?",
				ps -> ps.setInt(1, Integer.parseInt(executionId)),
				rs -> rs.next() ? ExecutionModel.fromDbResult(rs) : null);

			if (execution == null) {
				throw new EntityNotFoundException("Execution not found", "scheduler_execution.notfound");
			}

			setDataForRunningExecution(execution);

			ExecutionResponse response = new ExecutionResponse(execution, new ResponseInfo(ResponseCode.OK, "Execution loaded"));

			trx.success();

			return response;
		}
	}

	/**
	 * Determine the current scheduler status
	 * @return scheduler status
	 * @throws NodeException
	 */
	protected SchedulerStatusResponse getStatus() throws NodeException {
		SchedulerStatusResponse response = new SchedulerStatusResponse();
		Set<Integer> allowedScheduleIds = new HashSet<>();
		boolean suspended = getSchedulerFactory().getStatus(allowedScheduleIds);

		if (suspended) {
			if (isAnyScheduleRunning(allowedScheduleIds)) {
				response.setStatus(SchedulerStatus.suspending);
			} else {
				response.setStatus(SchedulerStatus.suspended);
			}
		} else {
			response.setStatus(SchedulerStatus.running);
		}

		return response.setAllowRun(allowedScheduleIds);
	}

	/**
	 * Check whether any schedule is currently running
	 * @param allowedScheduleIds set of allowed schedule IDs, which are not taken into consideration
	 * @return true if a schedule is running, false if not
	 * @throws NodeException
	 */
	protected boolean isAnyScheduleRunning(Set<Integer> allowedScheduleIds) throws NodeException {
		Set<Integer> running = new HashSet<>(getSchedulerFactory().getRunningSchedules());
		running.removeAll(allowedScheduleIds);
		return !running.isEmpty();
	}

	/**
	 * Get the scheduler factory instance.
	 * @return The scheduler factory instance
	 */
	private static SchedulerFactory getSchedulerFactory() {
		return (SchedulerFactory) ContentNodeFactory.getInstance().getFactory().getObjectFactory(SchedulerSchedule.class);
	}

	/**
	 * Task for executing a schedule.
	 */
	private class ExecuteScheduleTask implements Callable<Void>, Serializable {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -1672304311552835556L;
		private final SchedulerSchedule schedule;

		public ExecuteScheduleTask(SchedulerSchedule schedule) {
			this.schedule = schedule;
		}

		@Override
		public Void call() throws Exception {
			if (schedule != null) {
				SchedulerResourceImpl.getSchedulerFactory().executeNow(schedule);
			}

			return null;
		}
	}

	protected ScheduleStatus getScheduleStatus(Integer scheduleId) throws NodeException {
		SchedulerFactory schedulerFactory = getSchedulerFactory();

		if (schedulerFactory == null) {
			return ScheduleStatus.IDLE;
		}

		Set<Integer> runningSchedules = schedulerFactory.getRunningSchedules();
		Set<Integer> dueSchedules = schedulerFactory.getDueSchedules().stream().map(SchedulerSchedule::getId).collect(Collectors.toSet());

		if (runningSchedules.contains(scheduleId)) {
			return ScheduleStatus.RUNNING;
		}

		if (dueSchedules.contains(scheduleId)) {
			return ScheduleStatus.DUE;
		}

		return ScheduleStatus.IDLE;
	}

	/**
	 * Extract additional schedule info from the database result set.
	 *
	 * <p>
	 *     The result sets cursor is expected to be at the correct position.
	 * </p>
	 *
	 * @param rs The database result set with the schedule infomration.
	 * @return A mapping from schedule IDs to the respective schedule information.
	 */
	protected Map<Integer, ScheduleModel> getAdditionalScheduleInfo(ResultSet rs) throws SQLException {
		Map<Integer, ScheduleModel> models = new LinkedHashMap<>();

		while (rs.next()) {
			ExecutionModel execution = rs.getObject("e.id") instanceof Integer
				? ExecutionModel.fromDbResult("e.id", rs)
				: null;

			setDataForRunningExecution(execution);

			ScheduleModel schedule = new ScheduleModel()
				.setRuns(rs.getInt("runs"))
				.setAverageTime(rs.getInt("average_time"))
				.setLastExecution(execution);

			models.put(rs.getInt("id"), schedule);
		}

		return models;
	}

	/**
	 * If the given execution is still running (does not have an end time), we probably can find the current log output
	 * in a file. It that's the case, the current contents of the file will be read and set as "log" to the given model
	 * @param execution execution
	 * @return the execution
	 */
	protected static ExecutionModel setDataForRunningExecution(ExecutionModel execution) {
		if (execution != null && execution.isRunning()) {
			if (StringUtils.isEmpty(execution.getLog())) {
				File out = SchedulerFactory.getExecutionStdout(execution.getId(), false);
				if (out != null) {
					try {
						execution.setLog(FileUtils.readFileToString(out));
					} catch (IOException e) {
					}
				}
			}
			try {
				int current = TransactionManager.getCurrentTransaction().getUnixTimestamp();
				execution.setDuration(current - execution.getStartTime());
			} catch (TransactionException e) {
			}
		}
		return execution;
	}

	/**
	 * Query for getting scheduler executions
	 */
	private static class SchedulerExecutionQuery {

		private int scheduleId;

		private ExecutionFilterParameterBean executionFilter;

		private FilterParameterBean filter;

		private SortParameterBean sorting;

		private PagingParameterBean paging;

		private int start;

		/**
		 * Set query parameters
		 * @param scheduleId The schedule ID.
		 * @param executionFilter Execution filter parameters.
		 * @param filter Filter parameters.
		 * @param sorting Sorting parameters.
		 * @param paging Paging parameters.
		 * @return fluent API fil
		 */
		public SchedulerExecutionQuery query(int scheduleId, ExecutionFilterParameterBean executionFilter,
				FilterParameterBean filter, SortParameterBean sorting, PagingParameterBean paging) {
			this.scheduleId = scheduleId;
			this.executionFilter = executionFilter != null ? executionFilter : new ExecutionFilterParameterBean();
			this.filter = filter != null ? filter : new FilterParameterBean();
			this.sorting = sorting != null ? sorting : new SortParameterBean();
			this.paging = paging != null ? paging : new PagingParameterBean();
			this.start = (Math.max(this.paging.page, 1) - 1) * this.paging.pageSize;

			return this;
		}

		/**
		 * Get the filtered, paged, sorted list
		 * @return list
		 * @throws NodeException
		 */
		public List<ExecutionModel> get() throws NodeException {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT *");
			sql.append(getFromClause());
			sql.append(getWhereClause());

			Map<String, String> fieldMap = new HashMap<>();
			fieldMap.put("startTime", "starttime");
			fieldMap.put("endTime", "endtime");

			ResolvableComparator<Resolvable> sorter = ResolvableComparator.get(sorting, fieldMap, "id", "startTime", "starttime", "endTime", "endtime", "duration", "result");
			sql.append(sorter.getOrderClause());

			if (paging.pageSize > 0) {
				sql.append(String.format(" LIMIT %d, %d", start, paging.pageSize));
			}

			return DBUtils.select(sql.toString(), params(), rs -> {
				List<ExecutionModel> executions = new ArrayList<>();

				while (rs.next()) {
					executions.add(setDataForRunningExecution(ExecutionModel.fromDbResult(rs)));
				}

				return executions;
			});
		}

		/**
		 * Count the number of filtered entries
		 * @return count
		 * @throws NodeException
		 */
		public int count() throws NodeException {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT count(*) c");
			sql.append(getFromClause());
			sql.append(getWhereClause());

			return DBUtils.select(sql.toString(), params(), DBUtils.firstInt("c"));
		}

		/**
		 * Check whether there are more items (if paging is used)
		 * @param totalCount total count
		 * @return true if there are more items
		 */
		public boolean hasMore(int totalCount) {
			if (paging.pageSize <= 0) {
				return false;
			} else {
				return totalCount > start + paging.pageSize;
			}
		}

		/**
		 * Get parameter preparator
		 * @return preparator
		 */
		protected PrepareStatement params() {
			return stmt -> {
				int paramCounter = 0;

				stmt.setInt(++paramCounter, scheduleId);
				if (executionFilter.timestampMin != null) {
					stmt.setInt(++paramCounter, executionFilter.timestampMin);
				}
				if (executionFilter.timestampMax != null
						&& executionFilter.timestampMax > ObjectTransformer.getInt(executionFilter.timestampMin, 0)) {
					stmt.setInt(++paramCounter, executionFilter.timestampMax);
				}
				if (filter.query != null) {
					stmt.setInt(++paramCounter, ObjectTransformer.getInt(filter.query, 0));
					stmt.setInt(++paramCounter, ObjectTransformer.getInt(filter.query, 0));
					stmt.setString(++paramCounter, "%" + filter.query.toLowerCase() + "%");
				}
			};
		}

		/**
		 * Get the FROM clause
		 * @return clause
		 * @throws NodeException
		 */
		protected String getFromClause() throws NodeException {
			StringBuilder sql = new StringBuilder();
			sql.append(" FROM scheduler_execution");
			return sql.toString();
		}


		/**
		 * Get the WHERE clause
		 * @return clause
		 * @throws NodeException
		 */
		protected String getWhereClause() throws NodeException {
			StringBuilder sql = new StringBuilder();
			sql.append(" WHERE scheduler_schedule_id = ?");

			if (executionFilter.failed != null) {
				if (executionFilter.failed) {
					sql.append(" AND result > 0");
				} else {
					sql.append(" AND result = 0");
				}
			}
			if (executionFilter.timestampMin != null) {
				sql.append(" AND starttime >= ?");
			}
			if (executionFilter.timestampMax != null
					&& executionFilter.timestampMax > ObjectTransformer.getInt(executionFilter.timestampMin, 0)) {
				sql.append(" AND starttime <= ?");
			}
			if (filter.query != null) {
				sql.append(" AND (id = ? OR scheduler_schedule_id = ? OR LOWER(log) LIKE ?)");
			}

			return sql.toString();
		}
	}
}
