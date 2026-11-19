package com.gentics.contentnode.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.task.TaskAction;
import com.gentics.contentnode.rest.model.task.TaskAssignee;
import com.gentics.contentnode.rest.model.task.TaskCreator;
import com.gentics.contentnode.rest.model.task.TaskGenaix;
import com.gentics.contentnode.rest.model.task.TaskObject;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.task.TaskFactory.StoredTask;

/**
 * Rules for stored tasks, used by the REST resource and by MCP tools
 */
public final class TaskService {
	private final static Map<String, Class<? extends NodeObject>> OBJECT_TYPES = Map.of("page", Page.class, "folder",
			Folder.class, "file", File.class, "image", ImageFile.class);

	private TaskService() {
	}

	/**
	 * Get the stored tasks sent to the current user or one of the user's groups
	 * @return list of tasks
	 * @throws NodeException
	 */
	public static List<ResolvableUserTaskModel> listForCurrentUser() throws NodeException {
		Viewer viewer = Viewer.current();
		List<ResolvableUserTaskModel> tasks = new ArrayList<>();
		for (StoredTask task : TaskFactory.listForRecipient(viewer.userId, viewer.groupIds)) {
			tasks.add(toModel(task, viewer));
		}
		return tasks;
	}

	/**
	 * Get the stored tasks the current user sent to other users or groups
	 * @return list of tasks
	 * @throws NodeException
	 */
	public static List<ResolvableUserTaskModel> listAssignedByCurrentUser() throws NodeException {
		Viewer viewer = Viewer.current();
		List<ResolvableUserTaskModel> tasks = new ArrayList<>();
		for (StoredTask task : TaskFactory.listCreatedBy(viewer.userId)) {
			if (!Objects.equals(task.assigneeUserId, viewer.userId)) {
				tasks.add(toModel(task, viewer));
			}
		}
		return tasks;
	}

	/**
	 * Load a stored task the current user may see
	 * @param id task ID
	 * @return task
	 * @throws NodeException when not found or not visible
	 */
	public static ResolvableUserTaskModel get(String id) throws NodeException {
		Viewer viewer = Viewer.current();
		return toModel(loadVisible(id, viewer), viewer);
	}

	/**
	 * Create a stored task, or update the task with the same source and external key
	 * @param request task data
	 * @return created or updated task
	 * @throws NodeException
	 */
	public static ResolvableUserTaskModel create(UserTaskModel request) throws NodeException {
		Viewer viewer = Viewer.current();
		if (request == null) {
			throw invalid(I18NHelper.get("task.missing", "task"));
		}
		TaskSource source = request.getSource() != null ? request.getSource() : TaskSource.manual;
		if (source.isVirtual()) {
			throw conflict(I18NHelper.get("task.virtualsource", source.name()));
		}

		if (!StringUtils.isEmpty(request.getExternalKey())) {
			var existing = TaskFactory.getByExternalKey(source, request.getExternalKey());
			if (existing.isPresent()) {
				if (!Objects.equals(existing.get().creatorId, viewer.userId)) {
					throw conflict(I18NHelper.get("task.externalkey.conflict", request.getExternalKey()));
				}
				return update(Integer.toString(existing.get().id), request);
			}
		}

		if (StringUtils.isEmpty(request.getType())) {
			throw invalid(I18NHelper.get("task.missing", "type"));
		}
		if (StringUtils.isEmpty(request.getTitle())) {
			throw invalid(I18NHelper.get("task.missing", "title"));
		}

		TaskAssignee assignee = request.getAssignee() != null ? request.getAssignee()
				: new TaskAssignee(TaskAssignee.Type.user, viewer.userId);
		checkRecipient(assignee, viewer);
		List<TaskObject> objects = checkObjects(request.getObjects(), assignee, viewer);

		Transaction t = TransactionManager.getCurrentTransaction();
		int now = t.getUnixTimestamp();
		StoredTask task = new StoredTask();
		task.source = source;
		task.type = request.getType();
		task.title = request.getTitle();
		task.description = request.getDescription();
		task.status = request.getStatus() != null ? request.getStatus() : TaskStatus.open;
		task.dueAt = request.getDueAt();
		setRecipient(task, assignee);
		task.creatorId = viewer.userId;
		task.creatorName = viewer.user.getFullName();
		task.cdate = now;
		task.edate = now;
		task.completedAt = task.status.isFinal() ? now : null;
		task.objects = objects;
		task.details = request.getDetails();
		task.externalKey = StringUtils.defaultIfEmpty(request.getExternalKey(), null);
		applyGenaix(task, request.getGenaix());

		TaskFactory.insert(task);
		return toModel(task, viewer);
	}

	/**
	 * Update a stored task, null fields stay unchanged
	 * @param id task ID
	 * @param request changed data
	 * @return updated task
	 * @throws NodeException
	 */
	public static ResolvableUserTaskModel update(String id, UserTaskModel request) throws NodeException {
		Viewer viewer = Viewer.current();
		StoredTask task = loadVisible(id, viewer);
		if (request == null) {
			throw invalid(I18NHelper.get("task.missing", "task"));
		}

		boolean creator = isCreator(task, viewer);
		boolean changesCreatorFields = request.getTitle() != null || request.getDescription() != null
				|| request.getDueAt() != null || request.getAssignee() != null || request.getObjects() != null
				|| request.getDetails() != null || request.getGenaix() != null || request.getType() != null;
		if (changesCreatorFields && !creator) {
			throw forbidden(I18NHelper.get("task.forbidden", id));
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		int now = t.getUnixTimestamp();

		if (request.getAssignee() != null) {
			checkRecipient(request.getAssignee(), viewer);
			setRecipient(task, request.getAssignee());
		}
		if (request.getObjects() != null) {
			task.objects = checkObjects(request.getObjects(), currentAssignee(task), viewer);
		}
		if (request.getType() != null) {
			task.type = request.getType();
		}
		if (request.getTitle() != null) {
			task.title = request.getTitle();
		}
		if (request.getDescription() != null) {
			task.description = request.getDescription();
		}
		if (request.getDueAt() != null) {
			task.dueAt = request.getDueAt();
		}
		if (request.getDetails() != null) {
			task.details = request.getDetails();
		}
		if (request.getGenaix() != null) {
			applyGenaix(task, request.getGenaix());
		}
		if (request.getStatus() != null && request.getStatus() != task.status) {
			task.status = request.getStatus();
			task.completedAt = task.status.isFinal() ? Integer.valueOf(now) : null;
		}
		task.edate = now;

		TaskFactory.update(task);
		return toModel(task, viewer);
	}

	/**
	 * Delete a stored task
	 * @param id task ID
	 * @throws NodeException
	 */
	public static void delete(String id) throws NodeException {
		Viewer viewer = Viewer.current();
		StoredTask task = loadVisible(id, viewer);
		if (!isCreator(task, viewer)) {
			throw forbidden(I18NHelper.get("task.forbidden", id));
		}
		TaskFactory.delete(task.id);
	}

	/**
	 * Assign a group task to the current user
	 * @param id task ID
	 * @return updated task
	 * @throws NodeException
	 */
	public static ResolvableUserTaskModel claim(String id) throws NodeException {
		Viewer viewer = Viewer.current();
		StoredTask task = loadVisible(id, viewer);
		if (!canClaim(task, viewer)) {
			throw conflict(I18NHelper.get("task.claim.notpossible", id));
		}
		task.originGroupId = task.assigneeGroupId;
		task.assigneeGroupId = null;
		task.assigneeUserId = viewer.userId;
		task.status = TaskStatus.inProgress;
		task.edate = TransactionManager.getCurrentTransaction().getUnixTimestamp();
		TaskFactory.update(task);
		return toModel(task, viewer);
	}

	/**
	 * Give a claimed task back to its group
	 * @param id task ID
	 * @return updated task
	 * @throws NodeException
	 */
	public static ResolvableUserTaskModel unclaim(String id) throws NodeException {
		Viewer viewer = Viewer.current();
		StoredTask task = loadVisible(id, viewer);
		if (!canUnclaim(task, viewer)) {
			throw conflict(I18NHelper.get("task.unclaim.notpossible", id));
		}
		task.assigneeGroupId = task.originGroupId;
		task.assigneeUserId = null;
		task.status = TaskStatus.open;
		task.edate = TransactionManager.getCurrentTransaction().getUnixTimestamp();
		TaskFactory.update(task);
		return toModel(task, viewer);
	}

	/**
	 * Check whether the ID belongs to a virtual task
	 * @param id task ID
	 * @return true for IDs like {@code publishQueue:page:9142}
	 */
	public static boolean isVirtualId(String id) {
		if (id == null || !id.contains(":")) {
			return false;
		}
		String prefix = id.substring(0, id.indexOf(':'));
		for (TaskSource source : TaskSource.values()) {
			if (source.isVirtual() && source.name().equals(prefix)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Exception for a change of a virtual task
	 * @param id task ID
	 * @return exception (409)
	 */
	public static RestMappedException virtualTaskConflict(String id) {
		return conflict(I18NHelper.get("task.virtual", id));
	}

	/**
	 * Exception for a task that does not exist or is not visible
	 * @param id task ID
	 * @return exception (404)
	 */
	public static EntityNotFoundException notFound(String id) {
		return new EntityNotFoundException(I18NHelper.get("task.notfound", id));
	}

	private static StoredTask loadVisible(String id, Viewer viewer) throws NodeException {
		if (isVirtualId(id)) {
			throw virtualTaskConflict(id);
		}
		int numericId;
		try {
			numericId = Integer.parseInt(id);
		} catch (NumberFormatException e) {
			throw notFound(id);
		}
		StoredTask task = TaskFactory.get(numericId).orElseThrow(() -> notFound(id));
		if (!isRecipient(task, viewer) && !isCreator(task, viewer)) {
			throw notFound(id);
		}
		return task;
	}

	private static boolean isRecipient(StoredTask task, Viewer viewer) {
		return Objects.equals(task.assigneeUserId, viewer.userId)
				|| (task.assigneeGroupId != null && viewer.groupIds.contains(task.assigneeGroupId))
				|| (task.originGroupId != null && viewer.groupIds.contains(task.originGroupId));
	}

	private static boolean isCreator(StoredTask task, Viewer viewer) {
		return Objects.equals(task.creatorId, viewer.userId);
	}

	private static boolean canClaim(StoredTask task, Viewer viewer) {
		return !task.status.isFinal() && task.assigneeGroupId != null && viewer.groupIds.contains(task.assigneeGroupId);
	}

	private static boolean canUnclaim(StoredTask task, Viewer viewer) {
		return !task.status.isFinal() && task.originGroupId != null && Objects.equals(task.assigneeUserId, viewer.userId);
	}

	/**
	 * GenAIx tasks only get open, since only GenAIx changes them
	 */
	private static List<TaskAction> actions(StoredTask task, Viewer viewer) {
		List<TaskAction> actions = new ArrayList<>();
		if (task.source == TaskSource.genaix) {
			actions.add(TaskAction.open);
			return actions;
		}
		boolean recipient = isRecipient(task, viewer);
		if (task.objects != null && !task.objects.isEmpty()) {
			actions.add(TaskAction.open);
		}
		if (!task.status.isFinal()) {
			if (recipient) {
				actions.add(TaskAction.startSession);
				actions.add(TaskAction.complete);
				actions.add(TaskAction.dismiss);
			}
			if (canClaim(task, viewer)) {
				actions.add(TaskAction.claim);
			}
			if (canUnclaim(task, viewer)) {
				actions.add(TaskAction.unclaim);
			}
		}
		if (isCreator(task, viewer)) {
			actions.add(TaskAction.edit);
			actions.add(TaskAction.delete);
		}
		return actions;
	}

	private static void checkRecipient(TaskAssignee assignee, Viewer viewer) throws NodeException {
		if (assignee.getType() == null) {
			throw invalid(I18NHelper.get("task.missing", "assignee.type"));
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		NodeObject recipient = switch (assignee.getType()) {
		case user -> assignee.getId() == viewer.userId ? viewer.user : t.getObject(SystemUser.class, assignee.getId());
		case group -> t.getObject(UserGroup.class, assignee.getId());
		};
		boolean self = assignee.getType() == TaskAssignee.Type.user && assignee.getId() == viewer.userId;
		if (recipient == null || (!self && !t.canView(recipient))) {
			throw invalid(I18NHelper.get("task.assignee.invalid", assignee.getType().name(), String.valueOf(assignee.getId())));
		}
	}

	private static List<TaskObject> checkObjects(List<TaskObject> objects, TaskAssignee assignee, Viewer viewer)
			throws NodeException {
		if (objects == null || objects.isEmpty()) {
			return objects;
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler recipientPerms = null;
		if (assignee != null && assignee.getType() == TaskAssignee.Type.user && assignee.getId() != viewer.userId) {
			recipientPerms = new PermHandler();
			recipientPerms.initForUser(assignee.getId());
		}

		for (TaskObject object : objects) {
			Class<? extends NodeObject> clazz = OBJECT_TYPES.get(object.getType());
			if (clazz == null) {
				throw invalid(I18NHelper.get("task.object.type", String.valueOf(object.getType())));
			}
			try (ChannelTrx cTrx = new ChannelTrx(object.getNodeId())) {
				NodeObject loaded = t.getObject(clazz, object.getId());
				if (loaded == null || !t.canView(loaded) || (recipientPerms != null && !recipientPerms.canView(loaded))) {
					throw invalid(I18NHelper.get("task.object.notvisible", object.getType(), String.valueOf(object.getId())));
				}
				if (StringUtils.isEmpty(object.getName())) {
					object.setName(loaded instanceof Page page ? page.getName()
							: loaded instanceof Folder folder ? folder.getName()
							: loaded instanceof File file ? file.getName() : null);
				}
			}
		}
		return objects;
	}

	private static TaskAssignee currentAssignee(StoredTask task) {
		if (task.assigneeUserId != null) {
			return new TaskAssignee(TaskAssignee.Type.user, task.assigneeUserId);
		}
		if (task.assigneeGroupId != null) {
			return new TaskAssignee(TaskAssignee.Type.group, task.assigneeGroupId);
		}
		return null;
	}

	private static void setRecipient(StoredTask task, TaskAssignee assignee) {
		if (assignee.getType() == TaskAssignee.Type.group) {
			task.assigneeGroupId = assignee.getId();
			task.originGroupId = assignee.getId();
			task.assigneeUserId = null;
		} else {
			task.assigneeUserId = assignee.getId();
			task.assigneeGroupId = null;
			task.originGroupId = null;
		}
	}

	private static void applyGenaix(StoredTask task, TaskGenaix genaix) {
		if (genaix == null) {
			return;
		}
		task.genaixSessionId = genaix.getSessionId();
		task.genaixPlanId = genaix.getPlanId();
		task.genaixInstruction = genaix.getInstruction();
	}

	private static ResolvableUserTaskModel toModel(StoredTask task, Viewer viewer) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ResolvableUserTaskModel model = new ResolvableUserTaskModel();
		model.setId(Integer.toString(task.id))
			.setSource(task.source)
			.setType(task.type)
			.setTitle(task.title)
			.setDescription(task.description)
			.setStatus(task.status)
			.setDueAt(task.dueAt)
			.setCreatedAt(task.cdate)
			.setUpdatedAt(task.edate)
			.setCompletedAt(task.completedAt)
			.setObjects(task.objects)
			.setDetails(task.details)
			.setExternalKey(task.externalKey)
			.setActions(actions(task, viewer));

		if (task.assigneeUserId != null) {
			SystemUser user = t.getObject(SystemUser.class, task.assigneeUserId);
			model.setAssignee(new TaskAssignee(TaskAssignee.Type.user, task.assigneeUserId)
					.setName(user != null ? user.getFullName() : null));
		} else if (task.assigneeGroupId != null) {
			model.setAssignee(groupAssignee(task.assigneeGroupId));
		}
		if (task.originGroupId != null) {
			model.setAssigneeGroup(groupAssignee(task.originGroupId));
		}

		if (task.creatorId != null) {
			model.setCreator(new TaskCreator().setType(TaskCreator.Type.user).setId(task.creatorId).setName(task.creatorName));
		} else {
			model.setCreator(new TaskCreator().setType(TaskCreator.Type.system).setName(task.creatorName));
		}

		if (task.genaixSessionId != null || task.genaixPlanId != null || task.genaixInstruction != null) {
			model.setGenaix(new TaskGenaix().setSessionId(task.genaixSessionId).setPlanId(task.genaixPlanId)
					.setInstruction(task.genaixInstruction));
		}
		return model;
	}

	private static TaskAssignee groupAssignee(int groupId) throws NodeException {
		UserGroup group = TransactionManager.getCurrentTransaction().getObject(UserGroup.class, groupId);
		return new TaskAssignee(TaskAssignee.Type.group, groupId).setName(group != null ? group.getName() : null);
	}

	private static RestMappedException invalid(String message) {
		return new RestMappedException(message).setMessageType(Message.Type.WARNING)
				.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
	}

	private static RestMappedException conflict(String message) {
		return new RestMappedException(message).setMessageType(Message.Type.WARNING)
				.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
	}

	private static RestMappedException forbidden(String message) {
		return new RestMappedException(message).setMessageType(Message.Type.WARNING)
				.setResponseCode(ResponseCode.PERMISSION).setStatus(Status.FORBIDDEN);
	}

	private static class Viewer {
		final SystemUser user;

		final int userId;

		final Set<Integer> groupIds;

		private Viewer(SystemUser user, Collection<UserGroup> groups) {
			this.user = user;
			this.userId = user.getId();
			this.groupIds = groups.stream().map(UserGroup::getId).collect(Collectors.toSet());
		}

		static Viewer current() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			if (user == null) {
				throw new NodeException("Cannot find the current user %d".formatted(t.getUserId()));
			}
			return new Viewer(user, user.getUserGroups());
		}
	}
}
