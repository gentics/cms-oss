package com.gentics.contentnode.tests.rest.task;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.task.TaskObject;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.resource.impl.TaskResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBSessionClosure;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

/**
 * Test data for the task tests
 */
public abstract class AbstractTaskTest {
	protected static DBTestContext testContext = new DBTestContext();

	protected static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	protected static Node node;

	protected static Page page;

	protected static UserGroup creators;

	protected static UserGroup sub;

	protected static UserGroup others;

	protected static UserGroup noAdmin;

	protected static SystemUser creator;

	protected static SystemUser colleague;

	protected static SystemUser subUser;

	protected static SystemUser outsider;

	protected static SystemUser noAdminUser;

	@BeforeClass
	public static void setupTaskData() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> ContentNodeTestDataUtils.createNode("tasks", "Tasks", PublishTarget.NONE));
		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, 2));
		creators = supply(() -> Creator.createUsergroup("Task creators", "", nodeGroup));
		sub = supply(() -> Creator.createUsergroup("Task sub", "", creators));
		others = supply(() -> Creator.createUsergroup("Task others", "", nodeGroup));
		noAdmin = supply(() -> Creator.createUsergroup("Task no admin", "", nodeGroup));

		creator = supply(() -> Creator.createUser("task.creator", "task", "Task", "Creator", "", Arrays.asList(creators)));
		colleague = supply(() -> Creator.createUser("task.colleague", "task", "Task", "Colleague", "", Arrays.asList(creators)));
		subUser = supply(() -> Creator.createUser("task.sub", "task", "Task", "Sub", "", Arrays.asList(sub)));
		outsider = supply(() -> Creator.createUser("task.outsider", "task", "Task", "Outsider", "", Arrays.asList(others)));
		noAdminUser = supply(() -> Creator.createUser("task.noadmin", "task", "Task", "NoAdmin", "", Arrays.asList(noAdmin)));

		operate(() -> {
			String view = new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString();
			PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(creators, sub, noAdmin), view);
			PermHandler.setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(creators), PermHandler.FULL_PERM);
			PermHandler.setPermissions(SystemUser.TYPE_USERADMIN, Arrays.asList(creators), PermHandler.FULL_PERM);
			PermHandler.setPermissions(UserGroup.TYPE_GROUPADMIN, Arrays.asList(creators), PermHandler.FULL_PERM);
		});

		page = supply(() -> {
			Template template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template");
			return ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Task page");
		});
		operate(() -> page.unlock());
	}

	@After
	public void deleteTasks() throws NodeException {
		operate(() -> update("DELETE FROM user_task"));
	}

	protected static <T> T as(SystemUser user, Supplier<T> call) throws NodeException {
		try (DBSessionClosure ses = new DBSessionClosure(user)) {
			return call.supply();
		}
	}

	protected static RestMappedException failure(SystemUser user, Supplier<?> call) {
		try {
			as(user, call);
		} catch (RestMappedException e) {
			return e;
		} catch (NodeException e) {
			throw new AssertionError("Unexpected exception", e);
		}
		throw new AssertionError("Expected a RestMappedException");
	}

	protected static void assertStatus(RestMappedException e, Status status) {
		assertThat(e.getStatus()).as("Response status of '%s'", e.getMessage()).isEqualTo(status);
	}

	protected static UserTaskModel task(String title) {
		return new UserTaskModel().setType("custom").setTitle(title);
	}

	protected static TaskObject pageObject() {
		return new TaskObject().setType("page").setId(page.getId()).setNodeId(node.getId());
	}

	protected static List<UserTaskModel> list(TaskFilterParameterBean filter) throws NodeException {
		return new TaskResourceImpl().list(null, null, null, filter).getItems();
	}

	protected static TaskFilterParameterBean status(TaskStatus... status) {
		TaskFilterParameterBean filter = new TaskFilterParameterBean();
		filter.status = Set.of(status);
		return filter;
	}
}
