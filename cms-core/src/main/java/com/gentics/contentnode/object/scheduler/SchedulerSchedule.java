package com.gentics.contentnode.object.scheduler;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.ObjectReadOnlyException;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NamedNodeObject;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.scheduler.ScheduleData;
import com.gentics.contentnode.rest.model.scheduler.ScheduleModel;
import com.gentics.contentnode.rest.util.ModelBuilder;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

/**
 * Interface for scheduler schedules
 */
@TType(SchedulerSchedule.TYPE_SCHEDULER_SCHEDULE)
public interface SchedulerSchedule extends NamedNodeObject, Resolvable {
	/**
	 * The ttype of the scheduler schedule object. Value: {@value}
	 */
	int TYPE_SCHEDULER_SCHEDULE = 161;

	/**
	 * Transform the node object into its rest model
	 */
	Function<SchedulerSchedule, ScheduleModel> TRANSFORM2REST = schedule -> new ScheduleModel()
		.setId(schedule.getId())
		.setName(schedule.getName())
		.setDescription(schedule.getDescription())
		.setTaskId(schedule.getSchedulerTask().getId())
		.setScheduleData(schedule.getScheduleData())
		.setParallel(schedule.isParallel())
		.setActive(schedule.isActive())
		.setNotificationEmail(schedule.getNotificationEmail())
		.setCreator(ModelBuilder.getUser(schedule.getCreator()))
		.setCdate(schedule.getCDate().getIntTimestamp())
		.setEditor(ModelBuilder.getUser(schedule.getEditor()))
		.setEdate(schedule.getEDate().getIntTimestamp());

	/**
	 * Function that transforms the rest model into the given node model
	 */
	BiFunction<ScheduleModel, SchedulerSchedule, SchedulerSchedule> REST2NODE = (restModel, schedule) -> {
		if (StringUtils.isNotBlank(restModel.getName())) {
			schedule.setName(restModel.getName());
		}

		if (StringUtils.isNotBlank(restModel.getDescription())) {
			schedule.setDescription(restModel.getDescription());
		}

		if (restModel.getTaskId() != null) {
			SchedulerTask task = TransactionManager.getCurrentTransaction().getObject(SchedulerTask.class, restModel.getTaskId());

			if (task == null) {
				throw new EntityNotFoundException("Task not found", "scheduler_task.notfound");
			}

			schedule.setSchedulerTask(task);
		}

		if (restModel.getScheduleData() != null) {
			if (restModel.getScheduleData().isValid()) {
				schedule.setScheduleData(restModel.getScheduleData());
			} else {
				throw new NodeException("Invalid schedule data", "scheduler_data.invalid");
			}
		}

		if (restModel.getParallel() != null) {
			schedule.setParallel(restModel.getParallel());
		}
		if (restModel.getActive() != null) {
			schedule.setActive(restModel.getActive());
		}

		if (restModel.getNotificationEmail() != null) {
			schedule.setNotificationEmail(restModel.getNotificationEmail());
		}

		return schedule;
	};

	Consumer<ScheduleModel> EMBED_TASK = restSchedule -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		SchedulerTask nodeTask = t.getObject(SchedulerTask.class, restSchedule.getTaskId());
		if (nodeTask == null) {
			return;
		}

		restSchedule.setTask(SchedulerTask.TRANSFORM2REST.apply(nodeTask));
	};

	/**
	 * Set the initial permissions on the (newly created) schedule
	 * @param schedule schedule
	 * @throws NodeException
	 */
	static void setInitialPermission(SchedulerSchedule schedule) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		List<Integer> groupIds = PermHandler.getGroupsWithPermissionBit(PermHandler.TYPE_SCHEDULER_ADMIN, PermType.updateschedules.getBit());
		groupIds.retainAll(t.getPermHandler().getGroupIds(0));

		List<UserGroup> groups = t.getObjects(UserGroup.class, groupIds);
		PermHandler.setPermissions(TYPE_SCHEDULER_SCHEDULE, schedule.getId(), groups);
	}

	@Override
	default Integer getTType() {
		return TYPE_SCHEDULER_SCHEDULE;
	}

	/**
	 * Get the name
	 * @return name
	 */
	@FieldGetter("name")
	String getName();

	/**
	 * Set the name
	 * @param name name
	 * @throws ReadOnlyException
	 */
	@FieldSetter("name")
	default void setName(String name) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the description
	 * @return description
	 */
	@FieldGetter("description")
	String getDescription();

	/**
	 * Set the description
	 * @param description description
	 * @throws ReadOnlyException
	 */
	@FieldSetter("description")
	default void setDescription(String description) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Scheduler task
	 * @return scheduler task
	 * @throws NodeException
	 */
	SchedulerTask getSchedulerTask() throws NodeException;

	/**
	 * Set the scheduler task
	 * @param schedulerTask scheduler task
	 * @throws ReadOnlyException
	 */
	default void setSchedulerTask(SchedulerTask schedulerTask) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Whether the schedule can be executed in parallel.
	 * @return Whether the schedule can be executed in parallel.
	 */
	boolean isParallel();

	/**
	 * Set whether the schedule can be executed in parallel.
	 * @param parallel Whether the schedule can be executed in parallel.
	 * @throws ReadOnlyException
	 */
	default void setParallel(boolean parallel) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the schedule data.
	 * @return The schedule data.
	 */
	ScheduleData getScheduleData();

	/**
	 * Set the schedule data.
	 * @param data The schedule data.
	 * @throws ReadOnlyException
	 */
	default void setScheduleData(ScheduleData data) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the active flag
	 * @return flag
	 */
	@FieldGetter("active")
	boolean isActive();

	/**
	 * Set the active flag
	 * @param active flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("active")
	default void setActive(boolean active) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the creation date as a unix timestamp
	 * @return creation date unix timestamp
	 */
	ContentNodeDate getCDate();

	/**
	 * Object creator
	 * @return creator of the object
	 * @throws NodeException
	 */
	SystemUser getCreator() throws NodeException;

	/**
	 * Get the edit date as a unix timestamp
	 * @return edit date unix timestamp
	 */
	ContentNodeDate getEDate();

	/**
	 * Object editor
	 * @return last editor of the form
	 * @throws NodeException
	 */
	SystemUser getEditor() throws NodeException;

	/**
	 * Get the email addresses for notification in case of errors
	 * @return email addresses
	 */
	@Nonnull
	List<String> getNotificationEmail();

	/**
	 * Set the notification email addresses
	 * @param notificationEmail email addresses
	 * @throws ReadOnlyException
	 */
	default void setNotificationEmail(List<String> notificationEmail) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Check whether the schedule is "due" (the task should be executed)
	 * @return true for "due", false for "waiting"
	 * @throws NodeException
	 */
	boolean shouldExecute() throws NodeException;
}
