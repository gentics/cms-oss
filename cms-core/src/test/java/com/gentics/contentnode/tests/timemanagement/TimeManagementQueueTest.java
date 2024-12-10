package com.gentics.contentnode.tests.timemanagement;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.object.Node.TYPE_NODE;
import static com.gentics.contentnode.perm.PermHandler.FULL_PERM;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_CREATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_VIEW;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.resource.impl.MessagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for time management settings and the queue (i.e. what happens if users without "publish" permission do "publish At" or "offline At")
 */
public class TimeManagementQueueTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static UserGroup groupWithPublish;

	private static UserGroup groupWithoutPublish;

	private static SystemUser userWithPublish;

	private static SystemUser userWithoutPublish;

	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());

		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		groupWithPublish = supply(() -> createUserGroup("With Publish Permission", NODE_GROUP_ID));
		userWithPublish = supply(() -> createSystemUser("With", "Perm", null, "withperm", "withperm", Arrays.asList(groupWithPublish)));
		operate(() -> {
			setPermissions(TYPE_NODE, node.getFolder().getId(), Arrays.asList(groupWithPublish), FULL_PERM);
		});
		groupWithoutPublish = supply(() -> createUserGroup("Without Publish Permission", groupWithPublish.getId()));
		userWithoutPublish = supply(() -> createSystemUser("Without", "Perm", null, "withoutperm", "withoutperm", Arrays.asList(groupWithoutPublish)));
		operate(() -> {
			String perm = new Permission(PERM_VIEW, PERM_PAGE_VIEW, PERM_PAGE_UPDATE, PERM_PAGE_CREATE).toString();
			setPermissions(TYPE_NODE, node.getFolder().getId(), Arrays.asList(groupWithoutPublish), perm);
		});
	}

	/**
	 * Clean data before test run (delete inbox messages)
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		operate(() -> DBUtils.update("DELETE FROM msg"));
	}

	/**
	 * Test "publish" from a user without publish permission
	 * @throws NodeException
	 */
	@Test
	public void testQueuePublish() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);

		Page page = null;
		try (Trx trx = new Trx(userWithoutPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			page = createPage(node.getFolder(), template, "page");
			trx.success();
		}

		try (Trx trx = new Trx(userWithoutPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			GenericResponse response = new PageResourceImpl().publish(String.valueOf(page.getId()), null, new PagePublishRequest());
			assertResponseCodeOk(response);
			trx.success();
		}

		page = execute(Page::reload, page);

		consume(p -> {
			assertThat(p).as("Tested page").isOffline()
					.hasVersions(new NodeObjectVersion().setNumber("0.1").setDate(now).setCurrent(true))
					.hasQueuedPublish(userWithoutPublish)
					.hasNoPublishAt()
					.hasNoQueuedPublishAt()
					.hasNoOfflineAt()
					.hasNoQueuedOfflineAt();
		}, page);

		assertInbox(userWithPublish, new Message(Type.INFO, String.format("%s %s möchte die Seite %s veröffentlichen.",
				userWithoutPublish.getFirstname(), userWithoutPublish.getLastname(), getPath(page))));
	}

	/**
	 * Test "publish At" from a user without publish permission
	 * @throws NodeException
	 */
	@Test
	public void testQueuePublishAt() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int publishAt = now + 86400;

		Page page = null;
		try (Trx trx = new Trx(userWithoutPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			page = createPage(node.getFolder(), template, "page");
			trx.success();
		}

		try (Trx trx = new Trx(userWithoutPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			PagePublishRequest request = new PagePublishRequest();
			request.setAt(publishAt);
			GenericResponse response = new PageResourceImpl().publish(String.valueOf(page.getId()), null, request);
			assertResponseCodeOk(response);
			trx.success();
		}

		page = execute(Page::reload, page);

		consume(p -> {
			assertThat(p).as("Tested page").isOffline()
					.hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(now).setCurrent(true))
					.hasNoPublishAt()
					.hasQueuedPublishAt(userWithoutPublish, publishAt, "1.0")
					.hasNoOfflineAt()
					.hasNoQueuedOfflineAt();
		}, page);

		assertInbox(userWithPublish, new Message(Type.INFO, String.format("%s %s möchte die Seite %s am %s veröffentlichen.", userWithoutPublish.getFirstname(),
				userWithoutPublish.getLastname(), getPath(page), new ContentNodeDate(publishAt).getFullFormat())));
	}

	/**
	 * Test "publish At" from a user without publish permission for a page, that already has "publish At" set
	 * @throws NodeException
	 */
	@Test
	public void testPublishAtAndQueue() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int publishAt = now + 86400;
		int queuedPublishAt = publishAt + 86400;

		Page page = null;
		try (Trx trx = new Trx(userWithPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			page = createPage(node.getFolder(), template, "page");
			trx.success();
		}

		try (Trx trx = new Trx(userWithPublish)) {
			trx.getTransaction().setTimestamp((now + 1) * 1000L);
			PagePublishRequest request = new PagePublishRequest();
			request.setAt(publishAt);
			GenericResponse response = new PageResourceImpl().publish(String.valueOf(page.getId()), null, request);
			assertResponseCodeOk(response);
			trx.success();
		}

		try (Trx trx = new Trx(userWithoutPublish)) {
			trx.getTransaction().setTimestamp((now + 2) * 1000L);
			PagePublishRequest request = new PagePublishRequest();
			request.setAt(queuedPublishAt);
			GenericResponse response = new PageResourceImpl().publish(String.valueOf(page.getId()), null, request);
			assertResponseCodeOk(response);
			trx.success();
		}

		page = execute(Page::reload, page);

		consume(p -> {
			assertThat(p).as("Tested page").isOffline()
					.hasVersions(
							new NodeObjectVersion().setNumber("1.0").setDate(now).setCurrent(true))
					.hasPublishAt(publishAt, "1.0")
					.hasQueuedPublishAt(userWithoutPublish, queuedPublishAt, "1.0")
					.hasNoOfflineAt()
					.hasNoQueuedOfflineAt();
		}, page);

		assertInbox(userWithPublish, new Message(Type.INFO, String.format("%s %s möchte die Seite %s am %s veröffentlichen.",
				userWithoutPublish.getFirstname(), userWithoutPublish.getLastname(), getPath(page), new ContentNodeDate(queuedPublishAt).getFullFormat())));
	}

	/**
	 * Test "offline" from a user without publish permission
	 * @throws NodeException
	 */
	@Test
	public void testQueueOffline() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);

		Page page = null;
		try (Trx trx = new Trx(userWithPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			page = createPage(node.getFolder(), template, "page");
			page = update(page, Page::publish);
			trx.success();
		}

		try (Trx trx = new Trx(userWithoutPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			GenericResponse response = new PageResourceImpl().takeOffline(String.valueOf(page.getId()), null);
			assertResponseCodeOk(response);
			trx.success();
		}

		page = execute(Page::reload, page);

		consume(p -> {
			assertThat(p).as("Tested page").isOnline()
					.hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(now).setCurrent(true).setPublished(true))
					.hasNoPublishAt()
					.hasNoQueuedPublishAt()
					.hasQueuedOffline(userWithoutPublish)
					.hasNoOfflineAt()
					.hasNoQueuedOfflineAt();
		}, page);

		assertInbox(userWithPublish, new Message(Type.INFO, String.format("%s %s möchte die Seite %s vom Server nehmen.",
				userWithoutPublish.getFirstname(), userWithoutPublish.getLastname(), getPath(page))));
	}

	/**
	 * Test "offline At" from a user without publish permission
	 * @throws NodeException
	 */
	@Test
	public void testQueueOfflineAt() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int offlineAt = now + 86400;

		Page page = null;
		try (Trx trx = new Trx(userWithPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			page = createPage(node.getFolder(), template, "page");
			page = update(page, Page::publish);
			trx.success();
		}

		try (Trx trx = new Trx(userWithoutPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			PageOfflineRequest request = new PageOfflineRequest();
			request.setAt(offlineAt);
			GenericResponse response = new PageResourceImpl().takeOffline(String.valueOf(page.getId()), request);
			assertResponseCodeOk(response);
			trx.success();
		}

		page = execute(Page::reload, page);

		consume(p -> {
			assertThat(p).as("Tested page").isOnline()
					.hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(now).setCurrent(true).setPublished(true))
					.hasNoPublishAt()
					.hasNoQueuedPublishAt()
					.hasNoOfflineAt()
					.hasQueuedOfflineAt(userWithoutPublish, offlineAt);
		}, page);

		assertInbox(userWithPublish, new Message(Type.INFO, String.format("%s %s möchte die Seite %s am %s vom Server nehmen.",
				userWithoutPublish.getFirstname(), userWithoutPublish.getLastname(), getPath(page), new ContentNodeDate(offlineAt).getFullFormat())));
	}

	/**
	 * Test "offline At" from a user without publish permission for a page that alreads has "offline At" set
	 * @throws NodeException
	 */
	@Test
	public void testOfflineAtAndQueue() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int offlineAt = now + 86400;
		int queuedOfflineAt = offlineAt + 86400;

		Page page = null;
		try (Trx trx = new Trx(userWithPublish)) {
			trx.getTransaction().setTimestamp(now * 1000L);
			page = createPage(node.getFolder(), template, "page");
			page = update(page, Page::publish);
			trx.success();
		}

		try (Trx trx = new Trx(userWithPublish)) {
			trx.getTransaction().setTimestamp((now + 1) * 1000L);
			PageOfflineRequest request = new PageOfflineRequest();
			request.setAt(offlineAt);
			GenericResponse response = new PageResourceImpl().takeOffline(String.valueOf(page.getId()), request);
			assertResponseCodeOk(response);
			trx.success();
		}

		try (Trx trx = new Trx(userWithoutPublish)) {
			trx.getTransaction().setTimestamp((now + 2) * 1000L);
			PageOfflineRequest request = new PageOfflineRequest();
			request.setAt(queuedOfflineAt);
			GenericResponse response = new PageResourceImpl().takeOffline(String.valueOf(page.getId()), request);
			assertResponseCodeOk(response);
			trx.success();
		}

		page = execute(Page::reload, page);

		consume(p -> {
			assertThat(p).as("Tested page").isOnline()
					.hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(now).setCurrent(true).setPublished(true))
					.hasNoPublishAt()
					.hasNoQueuedPublishAt()
					.hasOfflineAt(offlineAt)
					.hasQueuedOfflineAt(userWithoutPublish, queuedOfflineAt);
		}, page);

		assertInbox(userWithPublish, new Message(Type.INFO, String.format("%s %s möchte die Seite %s am %s vom Server nehmen.",
				userWithoutPublish.getFirstname(), userWithoutPublish.getLastname(), getPath(page), new ContentNodeDate(queuedOfflineAt).getFullFormat())));
	}

	/**
	 * Assert inbox messages of the user (checking type and message)
	 * @param user user
	 * @param expected expected messages
	 * @throws NodeException
	 */
	protected void assertInbox(SystemUser user, Message... expected) throws NodeException {
		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MessagingResourceImpl().list(false);
			assertThat(response.getMessages()).as(String.format("Inbox messages for %s", user)).usingElementComparatorOnFields("type", "message")
					.containsOnly(expected);
			trx.success();
		}
	}

	protected String getPath(Page page) throws NodeException {
		return supply(() -> String.format("%s/%s (%d)", page.getFolder().getName(), page.getName(), page.getId()));
	}
}
