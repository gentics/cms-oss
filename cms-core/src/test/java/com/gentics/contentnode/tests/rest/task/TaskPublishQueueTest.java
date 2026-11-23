package com.gentics.contentnode.tests.rest.task;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.request.MultiPubqueueApproveRequest;
import com.gentics.contentnode.rest.model.task.TaskAction;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;
import com.gentics.contentnode.testutils.Creator;

/**
 * Test cases for the virtual tasks of the publish queue
 */
public class TaskPublishQueueTest extends AbstractTaskTest {
	private static SystemUser publisher;

	@BeforeClass
	public static void setupPublisher() throws NodeException {
		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, 2));
		UserGroup publishers = supply(() -> Creator.createUsergroup("Task publishers", "", nodeGroup));
		publisher = supply(() -> Creator.createUser("task.publisher", "task", "Task", "Publisher", "", Arrays.asList(publishers)));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(publishers),
				new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_PUBLISH).toString()));
	}

	@Before
	public void queuePage() throws NodeException {
		operate(t -> t.getObject(Page.class, page.getId()).queuePublish(creator));
	}

	@After
	public void clearQueue() throws NodeException {
		operate(t -> t.getObject(Page.class, page.getId()).clearQueue());
	}

	/**
	 * Test the task of a queued page for a user who may publish it
	 * @throws NodeException
	 */
	@Test
	public void testQueuedPageIsTaskForPublisher() throws NodeException {
		List<UserTaskModel> tasks = as(publisher, () -> list(queueOnly()));

		assertThat(tasks).as("Tasks of the publisher").extracting(UserTaskModel::getId)
				.containsExactly("publishQueue:page:%d".formatted(page.getId()));
		UserTaskModel task = tasks.get(0);
		assertThat(task.getType()).as("Type").isEqualTo("approvePublish");
		assertThat(task.getAssignee()).as("Recipient").isNull();
		assertThat(task.getActions()).as("Actions").contains(TaskAction.open, TaskAction.approve)
				.doesNotContain(TaskAction.assignBack);
		@SuppressWarnings("unchecked")
		Map<String, Object> requestedBy = (Map<String, Object>) task.getDetails().get("requestedBy");
		assertThat(requestedBy.get("id")).as("Requesting user").isEqualTo(creator.getId());
	}

	/**
	 * Test that users without publish permission see no task
	 * @throws NodeException
	 */
	@Test
	public void testNotVisibleWithoutPublishPermission() throws NodeException {
		assertThat(as(creator, () -> list(queueOnly()))).as("Tasks of a user without publish permission").isEmpty();
	}

	/**
	 * Test that approving the page removes the task
	 * @throws NodeException
	 */
	@Test
	public void testApprovedPageDisappears() throws NodeException {
		MultiPubqueueApproveRequest request = new MultiPubqueueApproveRequest();
		request.setIds(List.of(page.getId()));
		assertResponseCodeOk(as(publisher, () -> new PageResourceImpl().pubqueueApprove(request, 0)));

		assertThat(as(publisher, () -> list(queueOnly()))).as("Tasks of the publisher after approving").isEmpty();
	}

	private static TaskFilterParameterBean queueOnly() {
		TaskFilterParameterBean filter = new TaskFilterParameterBean();
		filter.source = Set.of(TaskSource.publishQueue);
		return filter;
	}
}
