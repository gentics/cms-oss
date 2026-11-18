package com.gentics.contentnode.tests.rest.task;

import static com.gentics.contentnode.factory.Trx.supply;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.task.TaskAction;
import com.gentics.contentnode.rest.model.task.TaskAssignee;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.resource.impl.TaskResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;
import com.gentics.contentnode.testutils.Creator;

/**
 * Test cases for tasks sent to other users and groups
 */
public class TaskAssignmentTest extends AbstractTaskTest {
	/**
	 * User the creator may send tasks to, but who may not see the test page
	 */
	private static SystemUser blindUser;

	@BeforeClass
	public static void setupBlindUser() throws NodeException {
		UserGroup blind = supply(() -> Creator.createUsergroup("Task blind", "", creators));
		blindUser = supply(() -> Creator.createUser("task.blind", "task", "Task", "Blind", "", Arrays.asList(blind)));
	}

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
		assertStatus(failure(creator, () -> new TaskResourceImpl().unclaim(created.getId())), Status.CONFLICT);

		UserTaskModel givenBack = as(colleague, () -> new TaskResourceImpl().unclaim(created.getId()).getTask());
		assertThat(givenBack.getAssignee()).as("Recipient after giving back").isEqualTo(group(creators.getId()));
		assertThat(givenBack.getStatus()).as("Status after giving back").isEqualTo(TaskStatus.open);
	}

	/**
	 * Test that only the user a task is assigned to may change its status
	 * @throws NodeException
	 */
	@Test
	public void testOnlyOwnerChangesStatus() throws NodeException {
		UserTaskModel groupTask = as(creator, () -> new TaskResourceImpl()
				.create(task("New tag type").setAssignee(group(creators.getId()))).getTask());
		assertThat(as(colleague, () -> new TaskResourceImpl().get(groupTask.getId()).getTask().getActions()))
				.as("Actions of a member before claiming").contains(TaskAction.claim)
				.doesNotContain(TaskAction.complete, TaskAction.dismiss, TaskAction.startSession);
		assertStatus(failure(colleague, () -> new TaskResourceImpl().update(groupTask.getId(),
				new UserTaskModel().setStatus(TaskStatus.done))), Status.FORBIDDEN);

		as(colleague, () -> new TaskResourceImpl().claim(groupTask.getId()));
		assertStatus(failure(creator, () -> new TaskResourceImpl().update(groupTask.getId(),
				new UserTaskModel().setStatus(TaskStatus.done))), Status.FORBIDDEN);
		assertThat(as(creator, () -> new TaskResourceImpl().get(groupTask.getId()).getTask().getActions()))
				.as("Actions of the creator, a member who did not claim").contains(TaskAction.claim, TaskAction.edit,
						TaskAction.delete)
				.doesNotContain(TaskAction.complete, TaskAction.dismiss, TaskAction.startSession);
		UserTaskModel done = as(colleague, () -> new TaskResourceImpl().update(groupTask.getId(),
				new UserTaskModel().setStatus(TaskStatus.done)).getTask());
		assertThat(done.getStatus()).as("Status set by the member who claimed").isEqualTo(TaskStatus.done);

		UserTaskModel userTask = as(creator, () -> new TaskResourceImpl()
				.create(task("Translate").setAssignee(user(colleague.getId()))).getTask());
		assertStatus(failure(creator, () -> new TaskResourceImpl().update(userTask.getId(),
				new UserTaskModel().setStatus(TaskStatus.dismissed))), Status.FORBIDDEN);
		as(creator, () -> new TaskResourceImpl().update(userTask.getId(), new UserTaskModel().setAssignee(user(creator.getId()))));
		UserTaskModel dismissed = as(creator, () -> new TaskResourceImpl().update(userTask.getId(),
				new UserTaskModel().setStatus(TaskStatus.dismissed)).getTask());
		assertThat(dismissed.getStatus()).as("Status set by the creator after taking the task").isEqualTo(TaskStatus.dismissed);
	}

	/**
	 * Test that a member may take over a task another member claimed
	 * @throws NodeException
	 */
	@Test
	public void testTakeOverClaimedTask() throws NodeException {
		UserTaskModel created = as(creator, () -> new TaskResourceImpl()
				.create(task("New tag type").setAssignee(group(creators.getId()))).getTask());
		as(colleague, () -> new TaskResourceImpl().claim(created.getId()));

		UserTaskModel taken = as(creator, () -> new TaskResourceImpl().claim(created.getId()).getTask());
		assertThat(taken.getAssignee()).as("Recipient after taking over").isEqualTo(user(creator.getId()));
		assertThat(taken.getAssigneeGroup()).as("Group after taking over").isEqualTo(group(creators.getId()));
		assertThat(taken.getStatus()).as("Status after taking over").isEqualTo(TaskStatus.inProgress);

		assertThat(as(colleague, () -> new TaskResourceImpl().get(created.getId()).getTask().getActions()))
				.as("Actions of the member who lost the task").contains(TaskAction.claim).doesNotContain(TaskAction.unclaim);
		assertStatus(failure(colleague, () -> new TaskResourceImpl().unclaim(created.getId())), Status.CONFLICT);

		UserTaskModel givenBack = as(creator, () -> new TaskResourceImpl().unclaim(created.getId()).getTask());
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

	/**
	 * Test that changing the recipient checks the stored objects against the new recipient
	 * @throws NodeException
	 */
	@Test
	public void testChangeRecipientChecksObjects() throws NodeException {
		assertStatus(failure(creator, () -> new TaskResourceImpl()
				.create(task("Fix the page").setObjects(List.of(pageObject())).setAssignee(user(blindUser.getId())))),
				Status.BAD_REQUEST);

		UserTaskModel created = as(creator, () -> new TaskResourceImpl()
				.create(task("Fix the page").setObjects(List.of(pageObject()))).getTask());
		assertStatus(failure(creator, () -> new TaskResourceImpl().update(created.getId(),
				new UserTaskModel().setAssignee(user(blindUser.getId())))), Status.BAD_REQUEST);

		assertThat(as(blindUser, () -> list(null))).as("Tasks of the user who may not see the page").isEmpty();
	}

	private static TaskAssignee user(int id) {
		return new TaskAssignee(TaskAssignee.Type.user, id);
	}

	private static TaskAssignee group(int id) {
		return new TaskAssignee(TaskAssignee.Type.group, id);
	}
}
