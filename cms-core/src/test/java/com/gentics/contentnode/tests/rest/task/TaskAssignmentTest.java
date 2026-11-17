package com.gentics.contentnode.tests.rest.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.task.TaskAction;
import com.gentics.contentnode.rest.model.task.TaskAssignee;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.resource.impl.TaskResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;

/**
 * Test cases for tasks sent to other users and groups
 */
public class TaskAssignmentTest extends AbstractTaskTest {
	/**
	 * Test sending a task to a user of the same group
	 * @throws NodeException
	 */
	@Test
	public void testAssignToUserInSameGroup() throws NodeException {
		UserTaskModel created = as(creator, () -> new TaskResourceImpl()
				.create(task("Review").setAssignee(user(colleague.getId()))).getTask());

		assertThat(as(colleague, () -> list(null))).as("Tasks of the recipient").extracting(UserTaskModel::getId)
				.containsExactly(created.getId());
		assertThat(as(creator, () -> list(null))).as("Tasks of the creator").isEmpty();

		TaskFilterParameterBean assignedByMe = new TaskFilterParameterBean();
		assignedByMe.assignedBy = "me";
		assertThat(as(creator, () -> list(assignedByMe))).as("Tasks the creator sent").extracting(UserTaskModel::getId)
				.containsExactly(created.getId());
	}

	/**
	 * Test sending a task to a user of a subgroup
	 * @throws NodeException
	 */
	@Test
	public void testAssignToUserInSubgroup() throws NodeException {
		UserTaskModel created = as(creator, () -> new TaskResourceImpl()
				.create(task("Review").setAssignee(user(subUser.getId()))).getTask());
		assertThat(created.getAssignee()).as("Recipient").isEqualTo(user(subUser.getId()));
	}

	/**
	 * Test sending a task to a user or group outside the own groups
	 * @throws NodeException
	 */
	@Test
	public void testAssignOutsideGroupsFails() throws NodeException {
		assertStatus(failure(creator, () -> new TaskResourceImpl().create(task("Review").setAssignee(user(outsider.getId())))),
				Status.BAD_REQUEST);
		assertStatus(failure(creator, () -> new TaskResourceImpl().create(task("Review").setAssignee(group(others.getId())))),
				Status.BAD_REQUEST);
	}

	/**
	 * Test sending a task without permission to see users
	 * @throws NodeException
	 */
	@Test
	public void testAssignWithoutAdminPermissionFails() throws NodeException {
		assertStatus(failure(noAdminUser, () -> new TaskResourceImpl().create(task("Review").setAssignee(user(creator.getId())))),
				Status.BAD_REQUEST);
		as(noAdminUser, () -> new TaskResourceImpl().create(task("For myself")));
	}

	/**
	 * Test which users see a group task
	 * @throws NodeException
	 */
	@Test
	public void testGroupTaskVisibility() throws NodeException {
		UserTaskModel created = as(creator, () -> new TaskResourceImpl()
				.create(task("New tag type").setAssignee(group(creators.getId()))).getTask());

		assertThat(as(colleague, () -> list(null))).as("Tasks of a member").extracting(UserTaskModel::getId)
				.containsExactly(created.getId());
		assertThat(as(creator, () -> list(null))).as("Tasks of the creator, also a member").extracting(UserTaskModel::getId)
				.containsExactly(created.getId());
		assertThat(as(subUser, () -> list(null))).as("Tasks of a subgroup member").isEmpty();
		assertThat(as(outsider, () -> list(null))).as("Tasks of a non-member").isEmpty();
	}

	/**
	 * Test claiming and giving back a group task
	 * @throws NodeException
	 */
	@Test
	public void testClaimAndUnclaim() throws NodeException {
		UserTaskModel created = as(creator, () -> new TaskResourceImpl()
				.create(task("New tag type").setAssignee(group(creators.getId()))).getTask());
		assertThat(as(colleague, () -> new TaskResourceImpl().get(created.getId()).getTask().getActions()))
				.as("Actions of a member before claiming").contains(TaskAction.claim);

		UserTaskModel claimed = as(colleague, () -> new TaskResourceImpl().claim(created.getId()).getTask());
		assertThat(claimed.getAssignee()).as("Recipient after claiming").isEqualTo(user(colleague.getId()));
		assertThat(claimed.getAssigneeGroup()).as("Group after claiming").isEqualTo(group(creators.getId()));
		assertThat(claimed.getStatus()).as("Status after claiming").isEqualTo(TaskStatus.inProgress);
		assertThat(claimed.getActions()).as("Actions of the claiming member").contains(TaskAction.unclaim)
				.doesNotContain(TaskAction.claim);

		List<UserTaskModel> seenByOther = as(creator, () -> list(null));
		assertThat(seenByOther).as("Tasks of another member").extracting(UserTaskModel::getAssignee)
				.containsExactly(user(colleague.getId()));
		assertStatus(failure(creator, () -> new TaskResourceImpl().claim(created.getId())), Status.CONFLICT);
		assertStatus(failure(creator, () -> new TaskResourceImpl().unclaim(created.getId())), Status.CONFLICT);

		UserTaskModel givenBack = as(colleague, () -> new TaskResourceImpl().unclaim(created.getId()).getTask());
		assertThat(givenBack.getAssignee()).as("Recipient after giving back").isEqualTo(group(creators.getId()));
		assertThat(givenBack.getStatus()).as("Status after giving back").isEqualTo(TaskStatus.open);
	}

	/**
	 * Test the actions of a GenAIx task
	 * @throws NodeException
	 */
	@Test
	public void testGenaixTaskActions() throws NodeException {
		UserTaskModel created = as(creator, () -> new TaskResourceImpl()
				.create(task("Ready to publish").setSource(TaskSource.genaix)).getTask());
		assertThat(created.getActions()).as("Actions").containsExactly(TaskAction.open);
	}

	private static TaskAssignee user(int id) {
		return new TaskAssignee(TaskAssignee.Type.user, id);
	}

	private static TaskAssignee group(int id) {
		return new TaskAssignee(TaskAssignee.Type.group, id);
	}
}
