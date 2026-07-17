package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_CREATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_VIEW;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertError;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.testutils.DBSessionClosure;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for handling time management over REST API
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class TimeManagementRestTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Folder testFolder;
	private static Page masterPage;
	private static Page channelPage;
	private static UserGroup editGroup;
	private static UserGroup publishGroup;
	private static SystemUser testUser;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		testFolder = supply(() -> createFolder(node.getFolder(), "Folder"));

		Template template = supply(() -> createTemplate(testFolder, "Template"));
		masterPage = supply(() -> createPage(testFolder, template, "Master Page"));
		channelPage = supply(() -> createPage(testFolder, template, "Channel Page", channel));

		masterPage = update(masterPage, p -> {
		}).unlock().publish().build();

		channelPage = update(channelPage, p -> {
		}).unlock().publish().build();

		editGroup = supply(() -> createUserGroup("Edit Group", NODE_GROUP_ID));
		publishGroup = supply(() -> createUserGroup("Publish Group", NODE_GROUP_ID));

		testUser = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(editGroup, publishGroup)));

		operate(() -> {
			String perm = new Permission(PERM_VIEW, PERM_PAGE_VIEW, PERM_PAGE_UPDATE, PERM_PAGE_CREATE).toString();
			setPermissions(Folder.TYPE_FOLDER, testFolder.getId(), Arrays.asList(editGroup), perm);
		});
	}

	@Parameters(name = "{index}: in {0}, publish permission {1}, restricted to {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestNode in : TestNode.values()) {
			for (Boolean publishPermission : Arrays.asList(true, false)) {
				for (Restricted restrictedTo : Restricted.values()) {
					data.add(new Object[] {in, publishPermission, restrictedTo});
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public TestNode in;

	@Parameter(1)
	public boolean publishPermission;

	@Parameter(2)
	public Restricted restrictedTo;

	private Page testPage;

	private Node testNode;

	private boolean effectivePublishPermission;

	private int futureTimestamp;

	/**
	 * Setup test data
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		// unlock the pages (in cases, where they stayed locked after a test)
		operate(() -> {
			masterPage.unlock();
			channelPage.unlock();
		});

		// clear time management for test pages
		masterPage = update(masterPage, p -> {
			p.clearQueue();
			p.clearTimePub();
			p.clearTimeOff();
		}).unlock().build();
		channelPage = update(channelPage, p -> {
			p.clearQueue();
			p.clearTimePub();
			p.clearTimeOff();
		}).unlock().build();

		// setup group permissions
		operate(() -> {
			String perm = null;
			if (publishPermission) {
				perm = new Permission(PermHandler.PERM_PAGE_PUBLISH).toString();
			} else {
				perm = PermHandler.EMPTY_PERM;
			}
			setPermissions(Folder.TYPE_FOLDER, testFolder.getId(), Arrays.asList(publishGroup), perm);
		});

		// setup node restriction to user/group assignment
		testUser = update(testUser, u -> {
			Map<Integer, Set<Integer>> restrictionsMap = u.getGroupNodeRestrictions();
			restrictionsMap.clear();
			switch (restrictedTo) {
			case channel:
				restrictionsMap.put(publishGroup.getId(), Collections.singleton(channel.getId()));
				break;
			case master:
				restrictionsMap.put(publishGroup.getId(), Collections.singleton(node.getId()));
				break;
			case both:
				restrictionsMap.put(publishGroup.getId(), new HashSet<>(Arrays.asList(node.getId(), channel.getId())));
				break;
			case none:
			default:
				break;
			}
		}).build();

		switch (in) {
		case channel:
			testPage = channelPage;
			testNode = channel;
			break;
		case master:
			testPage = masterPage;
			testNode = node;
			break;
		default:
			fail("Unexpected value of 'in': " + in);
			break;
		}

		if (publishPermission) {
			effectivePublishPermission = in.isSufficient(restrictedTo);
		} else {
			effectivePublishPermission = false;
		}

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, 5);
		futureTimestamp = (int)(cal.getTime().getTime() / 1000);
	}

	/**
	 * Test setting a "publish at" time
	 * @throws NodeException
	 */
	@Test
	public void testSetPublishAt() throws NodeException {
		// call the endpoint
		try (DBSessionClosure ses = new DBSessionClosure(testUser.getId())) {
			PagePublishRequest request = new PagePublishRequest();
			request.setAt(futureTimestamp);
			assertSuccess(() -> getPageResource().publish(Integer.toString(testPage.getId()), testNode.getId(), request), "");
		}

		operate(() -> {
			testPage = testPage.reload();
			if (effectivePublishPermission) {
				// with publish permission: page is planned now
				assertThat(testPage).isOnline().isPlanned().isNotQueued();
			} else {
				// without publish permission: page is queued now
				assertThat(testPage).isOnline().isNotPlanned().isQueued();
			}
		});
	}

	/**
	 * Test setting a "offline at" time
	 * @throws NodeException
	 */
	@Test
	public void testSetOfflinehAt() throws NodeException {
		// call the endpoint
		try (DBSessionClosure ses = new DBSessionClosure(testUser.getId())) {
			PageOfflineRequest request = new PageOfflineRequest();
			request.setAt(futureTimestamp);
			assertSuccess(() -> getPageResource().takeOffline(Integer.toString(testPage.getId()), request), "");
		}

		operate(() -> {
			testPage = testPage.reload();
			if (effectivePublishPermission) {
				// with publish permission: page is planned now
				assertThat(testPage).isOnline().isPlanned().isNotQueued();
			} else {
				// without publish permission: page is queued now
				assertThat(testPage).isOnline().isNotPlanned().isQueued();
			}
		});
	}

	/**
	 * Test clearing a previously set "publish at" time
	 * @throws NodeException
	 */
	@Test
	public void testClearPublishAt() throws NodeException {
		// set a "publish at" time
		testPage = update(testPage, p -> {
		}).unlock().publish(futureTimestamp).build();

		// call the endpoint to clear the "publish at" time
		try (DBSessionClosure ses = new DBSessionClosure(testUser.getId())) {
			PageSaveRequest request = new PageSaveRequest();
			request.setPage(new com.gentics.contentnode.rest.model.Page());
			request.setClearPublishAt(true);
			request.setUnlock(true);
			Supplier<GenericResponse> method = () -> getPageResource().save(Integer.toString(testPage.getId()), request);
			if (effectivePublishPermission) {
				assertSuccess(method, "");
			} else {
				assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, "");
			}
		}

		operate(() -> {
			testPage = testPage.reload();
			if (effectivePublishPermission) {
				// with permission: planned "publish at" must be removed
				assertThat(testPage).isOnline().isNotPlanned().isNotQueued();
			} else {
				// without permission: page must still be planned
				assertThat(testPage).isOnline().isPlanned().isNotQueued();
			}
		});
	}

	/**
	 * Test clearing a previously set "offline at" time
	 * @throws NodeException
	 */
	@Test
	public void testClearOfflineAt() throws NodeException {
		// set a "offline at" time
		testPage = update(testPage, p -> {
		}).unlock().takeOffline(futureTimestamp).build();

		// call the endpoint to clear the "offline at" time
		try (DBSessionClosure ses = new DBSessionClosure(testUser.getId())) {
			PageSaveRequest request = new PageSaveRequest();
			request.setPage(new com.gentics.contentnode.rest.model.Page());
			request.setClearOfflineAt(true);
			request.setUnlock(true);
			Supplier<GenericResponse> method = () -> getPageResource().save(Integer.toString(testPage.getId()), request);
			if (effectivePublishPermission) {
				assertSuccess(method, "");
			} else {
				assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, "");
			}
		}

		operate(() -> {
			testPage = testPage.reload();
			if (effectivePublishPermission) {
				// with permission: planned "offline at" must be removed
				assertThat(testPage).isOnline().isNotPlanned().isNotQueued();
			} else {
				// without permission: page must still be planned
				assertThat(testPage).isOnline().isPlanned().isNotQueued();
			}
		});
	}

	/**
	 * Possible test nodes
	 */
	public static enum TestNode {
		/**
		 * Test in master node
		 */
		master(Restricted.master, Restricted.none, Restricted.both),

		/**
		 * Test in channel
		 */
		channel(Restricted.channel, Restricted.none, Restricted.both);

		private List<Restricted> requiredRestrictions;

		/**
		 * Create instance
		 * @param requiredRestrictions required restrictions
		 */
		TestNode(Restricted...requiredRestrictions) {
			this.requiredRestrictions = Arrays.asList(requiredRestrictions);
		}

		/**
		 * Check whether the given user/group restriction is sufficient for "publish" permission in the test node
		 * @param restriction given restriction
		 * @return true if the restriction is sufficient for "publish" permission
		 */
		public boolean isSufficient(Restricted restriction) {
			return requiredRestrictions.contains(restriction);
		}
	}

	/**
	 * Possible restrictions of the user/group assignment
	 */
	public static enum Restricted {
		/**
		 * Assignment restricted to master node
		 */
		master,

		/**
		 * Assignment restricted to channel node
		 */
		channel,

		/**
		 * Assignment not restricted
		 */
		none,

		/**
		 * Assignment restricted to both (master and channel)
		 */
		both;
	}
}
