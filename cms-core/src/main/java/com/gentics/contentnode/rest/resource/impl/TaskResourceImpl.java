package com.gentics.contentnode.rest.resource.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskCountResponse;
import com.gentics.contentnode.rest.model.task.UserTaskListResponse;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.model.task.UserTaskResponse;
import com.gentics.contentnode.rest.resource.TaskResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;
import com.gentics.contentnode.rest.util.Filter;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.task.ResolvableUserTaskModel;
import com.gentics.contentnode.task.TaskProviderService;
import com.gentics.contentnode.task.TaskService;

/**
 * Implementation of {@link TaskResource}
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/task")
public class TaskResourceImpl implements TaskResource {
	private final static ServiceLoaderUtil<TaskProviderService> PROVIDERS = ServiceLoaderUtil.load(TaskProviderService.class);

	private final static Set<TaskStatus> ACTIVE = EnumSet.of(TaskStatus.open, TaskStatus.inProgress);

	private final static String[] SORT_ATTRIBUTES = { "dueAt", "createdAt", "updatedAt", "title", "status", "source", "type" };

	@Override
	@GET
	public UserTaskListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam TaskFilterParameterBean taskFilter) throws NodeException {
		TaskFilterParameterBean tf = taskFilter != null ? taskFilter : new TaskFilterParameterBean();
		try (Trx trx = ContentNodeHelper.trx()) {
			List<ResolvableUserTaskModel> tasks = collect(tf);

			// the bean's default "name" is no task attribute; the task list is sorted by due date unless asked otherwise
			String sort = sorting == null || StringUtils.isEmpty(sorting.sort) || "name".equals(sorting.sort) ? "+dueAt" : sorting.sort;
			ResolvableComparator<ResolvableUserTaskModel> comparator = ResolvableComparator.get(sort, SORT_ATTRIBUTES);
			if (comparator != null) {
				comparator.setNullsAsLast("dueAt");
			}

			UserTaskListResponse response = ListBuilder.from(tasks, UserTaskModel.class::cast)
				.filter(statusFilter(tf.status))
				.filter(task -> tf.type == null || tf.type.equals(task.getType()))
				.filter(task -> tf.nodeId == null || (task.getObjects() != null
						&& task.getObjects().stream().anyMatch(o -> Objects.equals(o.getNodeId(), tf.nodeId))))
				.filter(task -> tf.genaixSessionId == null
						|| (task.getGenaix() != null && tf.genaixSessionId.equals(task.getGenaix().getSessionId())))
				.filter(task -> tf.externalKey == null || tf.externalKey.equals(task.getExternalKey()))
				.filter(queryFilter(filter))
				.sort(comparator)
				.page(paging)
				.to(new UserTaskListResponse());
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/count")
	public UserTaskCountResponse count() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Map<TaskSource, Integer> counts = new LinkedHashMap<>();
			for (ResolvableUserTaskModel task : collect(new TaskFilterParameterBean())) {
				if (ACTIVE.contains(task.getStatus())) {
					counts.merge(task.getSource(), 1, Integer::sum);
				}
			}
			trx.success();
			return new UserTaskCountResponse(counts, ResponseInfo.ok("Successfully counted tasks"));
		}
	}

	@Override
	@GET
	@Path("/{id}")
	public UserTaskResponse get(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserTaskModel task;
			if (TaskService.isVirtualId(id)) {
				task = virtualTasks(null).stream().filter(t -> id.equals(t.getId())).findFirst()
						.orElseThrow(() -> TaskService.notFound(id));
			} else {
				task = TaskService.get(id);
			}
			trx.success();
			return new UserTaskResponse(task, ResponseInfo.ok("Successfully loaded task"));
		}
	}

	@Override
	@POST
	public UserTaskResponse create(UserTaskModel task) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserTaskModel created = TaskService.create(task);
			trx.success();
			return new UserTaskResponse(created, ResponseInfo.ok("Successfully saved task"));
		}
	}

	@Override
	@PUT
	@Path("/{id}")
	public UserTaskResponse update(@PathParam("id") String id, UserTaskModel task) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserTaskModel updated = TaskService.update(id, task);
			trx.success();
			return new UserTaskResponse(updated, ResponseInfo.ok("Successfully updated task"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}")
	public GenericResponse delete(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			TaskService.delete(id);
			trx.success();
			return new GenericResponse(null, ResponseInfo.ok("Successfully deleted task %s".formatted(id)));
		}
	}

	@Override
	@POST
	@Path("/{id}/claim")
	public UserTaskResponse claim(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserTaskModel task = TaskService.claim(id);
			trx.success();
			return new UserTaskResponse(task, ResponseInfo.ok("Successfully claimed task"));
		}
	}

	@Override
	@POST
	@Path("/{id}/unclaim")
	public UserTaskResponse unclaim(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserTaskModel task = TaskService.unclaim(id);
			trx.success();
			return new UserTaskResponse(task, ResponseInfo.ok("Successfully gave task back"));
		}
	}

	/**
	 * Collect stored and virtual tasks, providers are skipped when only stored tasks can match
	 */
	private List<ResolvableUserTaskModel> collect(TaskFilterParameterBean tf) throws NodeException {
		boolean assignedByMe = "me".equals(tf.assignedBy);
		List<ResolvableUserTaskModel> tasks = new ArrayList<>(
				assignedByMe ? TaskService.listAssignedByCurrentUser() : TaskService.listForCurrentUser());
		tasks.removeIf(task -> tf.source != null && !tf.source.isEmpty() && !tf.source.contains(task.getSource()));

		boolean storedOnly = assignedByMe || tf.genaixSessionId != null || tf.externalKey != null;
		if (!storedOnly) {
			tasks.addAll(virtualTasks(tf.source));
		}
		return tasks;
	}

	private List<ResolvableUserTaskModel> virtualTasks(Set<TaskSource> sources) throws NodeException {
		List<ResolvableUserTaskModel> tasks = new ArrayList<>();
		for (TaskProviderService provider : PROVIDERS) {
			if (sources == null || sources.isEmpty() || sources.contains(provider.source())) {
				tasks.addAll(provider.tasks());
			}
		}
		return tasks;
	}

	private static Filter<ResolvableUserTaskModel> statusFilter(Set<TaskStatus> status) {
		Set<TaskStatus> allowed = status == null || status.isEmpty() ? ACTIVE : status;
		return task -> allowed.contains(task.getStatus());
	}

	private static Filter<ResolvableUserTaskModel> queryFilter(FilterParameterBean filter) {
		if (filter == null || StringUtils.isEmpty(filter.query)) {
			return null;
		}
		String query = filter.query.toLowerCase();
		return task -> (task.getTitle() != null && task.getTitle().toLowerCase().contains(query))
				|| (task.getObjects() != null && task.getObjects().stream()
						.anyMatch(o -> o.getName() != null && o.getName().toLowerCase().contains(query)));
	}
}
