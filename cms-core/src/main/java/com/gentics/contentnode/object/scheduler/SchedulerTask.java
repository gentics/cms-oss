package com.gentics.contentnode.object.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.gentics.contentnode.object.NamedNodeObject;
import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.ObjectReadOnlyException;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SchedulerFactory;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.scheduler.TaskModel;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.scheduler.InternalSchedulerTask;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Interface for scheduler tasks
 */
@TType(SchedulerTask.TYPE_SCHEDULER_TASK)
public interface SchedulerTask extends NamedNodeObject, Resolvable {
	/**
	 * The ttype of the scheduler task object. Value: {@value}
	 */
	public final static int TYPE_SCHEDULER_TASK = 160;

	/**
	 * Transform the node object into its rest model
	 */
	public final static Function<SchedulerTask, TaskModel> TRANSFORM2REST = task -> {
		return new TaskModel()
				.setId(task.getId())
				.setName(task.getName())
				.setDescription(task.getDescription())
				.setCommand(task.getCommand())
				.setInternal(task.isInternal())
				.setCreator(ModelBuilder.getUser(task.getCreator()))
				.setCdate(task.getCDate().getIntTimestamp())
				.setEditor(ModelBuilder.getUser(task.getEditor()))
				.setEdate(task.getEDate().getIntTimestamp());
	};

	/**
	 * Function that transforms the rest model into the given node model
	 */
	public final static BiFunction<TaskModel, SchedulerTask, SchedulerTask> REST2NODE = (restModel, task) -> {
		if (!StringUtils.isBlank(restModel.getName())) {
			task.setName(restModel.getName());
		}
		if (!StringUtils.isBlank(restModel.getDescription())) {
			task.setDescription(restModel.getDescription());
		}
		if (!StringUtils.isBlank(restModel.getCommand())) {
			task.setCommand(restModel.getCommand());
		}

		return task;
	};

	/**
	 * Set the initial permissions on the (newly created) task
	 * @param task task
	 * @throws NodeException
	 */
	public static void setInitialPermission(SchedulerTask task) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		List<Integer> groupIds = PermHandler.getGroupsWithPermissionBit(PermHandler.TYPE_SCHEDULER_ADMIN, PermType.updatetasks.getBit());
		groupIds.retainAll(t.getPermHandler().getGroupIds(0));

		List<UserGroup> groups = t.getObjects(UserGroup.class, groupIds);
		PermHandler.setPermissions(TYPE_SCHEDULER_TASK, task.getId(), groups);
	}

	@Override
	default Integer getTType() {
		return TYPE_SCHEDULER_TASK;
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
	 * Get the command
	 * @return command
	 */
	@FieldGetter("command")
	String getCommand();

	/**
	 * Get the optionally sanitized command
	 * @return sanitized command
	 * @throws NodeException
	 */
	String getSanitizedCommand() throws NodeException;

	/**
	 * Set the command
	 * @param command command
	 * @throws ReadOnlyException
	 */
	@FieldSetter("command")
	default void setCommand(String command) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the internal flag
	 * @return flag
	 */
	@FieldGetter("internal")
	boolean isInternal();

	/**
	 * Set the internal flag
	 * @param internal flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("internal")
	default void setInternal(boolean internal) throws ReadOnlyException {
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
	 * Check whether the task (its command) is valid
	 * @throws NodeException if validation fails
	 */
	public void validate() throws NodeException;

	/**
	 * Execute this task.
	 *
	 * @param output List to gather the output of the task execution.
	 * @return The result status of the task. On success. For internal tasks
	 * 		this is 0 for successful execution and 255 for failed executions.
	 * 		For external tasks it is the exit code of the executed shell
	 * 		command.
	 */
	default int execute(List<String> output) throws NodeException, InterruptedException, IOException {
		int resultStatus;

		if (isInternal()) {
			InternalSchedulerTask internalTask = SchedulerFactory.getInternalSchedulerTask(getCommand());

			if (internalTask == null) {
				throw new NodeException(String.format("Unknown internal task %s", getCommand()));
			}

			try {
				resultStatus = internalTask.execute(output) ? 0 : 255;
			} catch (NodeException e) {
				output.add(ExceptionUtils.getStackTrace(e));
				resultStatus = 255;
			}

			return resultStatus;
		}

		Process p = Runtime.getRuntime().exec(getSanitizedCommand());

		resultStatus = p.waitFor();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			String line;

			while ((line = reader.readLine()) != null) {
				output.add(line);
			}
		}

		return resultStatus;
	}
}
