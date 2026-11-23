package com.gentics.contentnode.tests.rest.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.task.TaskAction;
import com.gentics.contentnode.rest.model.task.TaskAssignee;
import com.gentics.contentnode.rest.model.task.TaskCreator;
import com.gentics.contentnode.rest.model.task.TaskGenaix;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.resource.impl.TaskResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;

/**
 * Test cases for stored tasks
 */
public class TaskResourceTest extends AbstractTaskTest {
	/**
	 * Test creating a task for the current user
	 * @throws NodeException
	 */
	@Test
	public void testCreateForSelf() throws NodeException {
		UserTaskModel created = as(creator, () -> new TaskResourceImpl().create(task("Check the teaser")).getTask());

		assertThat(created.getAssignee()).as("Recipient").isEqualTo(new TaskAssignee(TaskAssignee.Type.user, creator.getId()));
		assertThat(created.getCreator().getType()).as("Creator type").isEqualTo(TaskCreator.Type.user);
		assertThat(created.getCreator().getId()).as("Creator").isEqualTo(creator.getId());
		assertThat(created.getStatus()).as("Status").isEqualTo(TaskStatus.open);
		assertThat(created.getSource()).as("Source").isEqualTo(TaskSource.manual);
		assertThat(created.getActions()).as("Actions").contains(TaskAction.complete, TaskAction.dismiss, TaskAction.edit,
				TaskAction.delete);

		List<UserTaskModel> tasks = as(creator, () -> list(null));
		assertThat(tasks).as("Tasks of the creator").extracting(UserTaskModel::getId).containsExactly(created.getId());
	}

	/**
	 * Test that creating a task with an existing external key updates it
	 * @throws NodeException
	 */
	@Test
	public void testUpsertByExternalKey() throws NodeException {
		String key = "genaix:session-1:plan-1:release";
		UserTaskModel first = as(creator, () -> new TaskResourceImpl().create(task("Ready to publish").setSource(TaskSource.genaix)
				.setExternalKey(key).setGenaix(new TaskGenaix().setSessionId("session-1").setPlanId("plan-1"))).getTask());
		UserTaskModel second = as(creator, () -> new TaskResourceImpl().create(task("Ready to publish, updated")
				.setSource(TaskSource.genaix).setExternalKey(key)).getTask());

		assertThat(second.getId()).as("ID after repeating the request").isEqualTo(first.getId());
		assertThat(second.getTitle()).as("Title after repeating the request").isEqualTo("Ready to publish, updated");

		TaskFilterParameterBean byKey = new TaskFilterParameterBean();
		byKey.externalKey = key;
		assertThat(as(creator, () -> list(byKey))).as("Tasks with the key").extracting(UserTaskModel::getId)
				.containsExactly(first.getId());

		TaskFilterParameterBean bySession = new TaskFilterParameterBean();
		bySession.genaixSessionId = "session-1";
		assertThat(as(creator, () -> list(bySession))).as("Tasks of the session").extracting(UserTaskModel::getId)
				.containsExactly(first.getId());
	}

	/**
	 * Test setting a task done
	 * @throws NodeException
	 */
	@Test
	public void testCompleteSetsCompletedAt() throws NodeException {
		UserTaskModel created = as(creator, () -> new TaskResourceImpl().create(task("Proofread")).getTask());
		assertThat(created.getCompletedAt()).as("Completion time of an open task").isNull();

		UserTaskModel done = as(creator, () -> new TaskResourceImpl().update(created.getId(),
				new UserTaskModel().setStatus(TaskStatus.done)).getTask());
		assertThat(done.getStatus()).as("Status").isEqualTo(TaskStatus.done);
		assertThat(done.getCompletedAt()).as("Completion time").isNotNull();

		assertThat(as(creator, () -> list(null))).as("Open tasks").isEmpty();
		assertThat(as(creator, () -> list(status(TaskStatus.done)))).as("Done tasks").extracting(UserTaskModel::getId)
				.containsExactly(created.getId());
	}

	/**
	 * Test that only the creator may edit and delete a task
	 * @throws NodeException
	 */
	@Test
	public void testOnlyCreatorEditsAndDeletes() throws NodeException {
		UserTaskModel created = as(creator, () -> new TaskResourceImpl()
				.create(task("Translate").setAssignee(new TaskAssignee(TaskAssignee.Type.user, colleague.getId()))).getTask());

		assertStatus(failure(colleague, () -> new TaskResourceImpl().update(created.getId(), new UserTaskModel().setTitle("Other"))),
				Status.FORBIDDEN);
		assertStatus(failure(colleague, () -> new TaskResourceImpl().delete(created.getId())), Status.FORBIDDEN);

		UserTaskModel done = as(colleague, () -> new TaskResourceImpl().update(created.getId(),
				new UserTaskModel().setStatus(TaskStatus.done)).getTask());
		assertThat(done.getStatus()).as("Status set by the recipient").isEqualTo(TaskStatus.done);

		as(creator, () -> new TaskResourceImpl().delete(created.getId()));
		assertThatThrownBy(() -> as(creator, () -> new TaskResourceImpl().get(created.getId())))
				.as("Loading the deleted task").isInstanceOf(EntityNotFoundException.class);
	}

	/**
	 * Test creating a task for an object the user cannot see
	 * @throws NodeException
	 */
	@Test
	public void testObjectNotVisible() throws NodeException {
		assertStatus(failure(outsider, () -> new TaskResourceImpl().create(task("Fix the page").setObjects(List.of(pageObject())))),
				Status.BAD_REQUEST);

		UserTaskModel created = as(creator, () -> new TaskResourceImpl()
				.create(task("Fix the page").setObjects(List.of(pageObject()))).getTask());
		assertThat(created.getObjects()).as("Objects").hasSize(1);
		assertThat(created.getObjects().get(0).getName()).as("Filled in object name").isEqualTo("Task page");
	}

	/**
	 * Test changing virtual tasks
	 * @throws NodeException
	 */
	@Test
	public void testVirtualTaskConflict() throws NodeException {
		String id = "publishQueue:page:%d".formatted(page.getId());
		assertStatus(failure(creator, () -> new TaskResourceImpl().update(id, new UserTaskModel().setStatus(TaskStatus.done))),
				Status.CONFLICT);
		assertStatus(failure(creator, () -> new TaskResourceImpl().delete(id)), Status.CONFLICT);
		assertStatus(failure(creator, () -> new TaskResourceImpl().create(task("Virtual").setSource(TaskSource.linkChecker))),
				Status.CONFLICT);
	}

	/**
	 * Test counting the open tasks
	 * @throws NodeException
	 */
	@Test
	public void testCount() throws NodeException {
		as(creator, () -> new TaskResourceImpl().create(task("One")));
		as(creator, () -> new TaskResourceImpl().create(task("Two").setStatus(TaskStatus.inProgress)));
		as(creator, () -> new TaskResourceImpl().create(task("Three").setStatus(TaskStatus.done)));
		as(creator, () -> new TaskResourceImpl().create(task("Four").setSource(TaskSource.genaix)));

		Map<TaskSource, Integer> counts = as(creator, () -> new TaskResourceImpl().count().getCounts());
		assertThat(counts).as("Counts").containsEntry(TaskSource.manual, 2).containsEntry(TaskSource.genaix, 1);
	}
}
