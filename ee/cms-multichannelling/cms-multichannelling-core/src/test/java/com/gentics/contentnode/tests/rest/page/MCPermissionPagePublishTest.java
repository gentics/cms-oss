package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertError;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBSessionClosure;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for channel specific permissions and page publish
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = Feature.MULTICHANNELLING)
public class MCPermissionPagePublishTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static UserGroup adminGroup;
	private static UserGroup editorGroup;
	private static SystemUser testUser;
	private static Template template;
	private static Node otherNode;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: folder {0}, view permission {1}, publish permission {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (Localization folderLoc : Localization.values()) {
			for (boolean viewPermission : Arrays.asList(true, false)) {
				for (boolean publishPermission : Arrays.asList(true, false)) {
					data.add(new Object[] {folderLoc, viewPermission, publishPermission});
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			adminGroup = t.getObject(UserGroup.class, 2);
			assertNotNull("AdminGroup must exist");

			editorGroup = Creator.createUsergroup("Editor", "", adminGroup);

			testUser = Creator.createUser("test", "test", "test", "test", "", new ArrayList<UserGroup>());

			testUser.getUserGroups().add(adminGroup);
			testUser.save();

			trx.success();
		}

		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUser.getId(), 0))) {
			node = ContentNodeTestDataUtils.createNode();
			channel = ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/");

			otherNode = ContentNodeTestDataUtils.createNode();

			template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template");

			trx.success();
		}

		try (Trx trx = new Trx()) {
			PermHandler.setPermissions(Node.TYPE_NODE, ObjectTransformer.getInt(node.getFolder().getId(), 0), Arrays.asList(editorGroup),
					new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE).toString());
			PermHandler.setPermissions(Folder.TYPE_FOLDER, ObjectTransformer.getInt(node.getFolder().getId(), 0), Arrays.asList(editorGroup),
					new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString());

			trx.success();
		}
	}

	private Localization folderLoc;
	private boolean viewPermission;
	private boolean publishPermission;

	/**
	 * Create a test instance
	 * @param folderLoc folder localization setting
	 * @param viewPermission view permission
	 * @param publishPermission publish permission
	 */
	public MCPermissionPagePublishTest(Localization folderLoc, boolean viewPermission, boolean publishPermission) {
		this.folderLoc = folderLoc;
		this.viewPermission = viewPermission;
		this.publishPermission = publishPermission;
	}

	/**
	 * Test publishing page
	 */
	@Test
	public void testPublishPage() throws Exception {
		Folder folder = null;
		Page page = null;
		try (Trx trx = new Trx(null,testUser.getId())) {
			switch (folderLoc) {
			case INHERITED:
				folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
				break;
			case LOCALIZED:
				Folder masterFolder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
				folder = ContentNodeTestDataUtils.localize(masterFolder, channel);
				break;
			case LOCAL:
				folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder", channel);
				break;
			}

			page = ContentNodeTestDataUtils.createPage(folder, template, "Page", channel);

			// restrict user to channels
			Transaction t = TransactionManager.getCurrentTransaction();
			Set<Integer> nodeIds = new HashSet<Integer>();
			nodeIds.add(ObjectTransformer.getInt(otherNode.getId(), 0));
			if (viewPermission) {
				nodeIds.add(ObjectTransformer.getInt(channel.getId(), 0));
			}

			testUser = t.getObject(SystemUser.class, testUser.getId(), true);
			Map<Integer, Set<Integer>> groupNodeRestrictions = testUser.getGroupNodeRestrictions();
			groupNodeRestrictions.clear();
			groupNodeRestrictions.put(ObjectTransformer.getInt(publishPermission ? adminGroup.getId() : editorGroup.getId(), 0), nodeIds);
			List<UserGroup> groups = testUser.getUserGroups();
			groups.clear();
			if (publishPermission) {
				groups.add(adminGroup);
			} else {
				groups.add(editorGroup);
			}
			testUser.save();
			testUser = t.getObject(SystemUser.class, testUser.getId());

			trx.success();
		}

		// publish the page
		try (DBSessionClosure ses = new DBSessionClosure(testUser.getId())) {
			String pageId = String.valueOf(page.getId());
			Supplier<GenericResponse> method = () -> getPageResource().publish(pageId, channel.getId(), new PagePublishRequest());

			if (viewPermission) {
				assertSuccess(method, "");
			} else {
				assertError(method, InsufficientPrivilegesException.class, ResponseCode.PERMISSION, "");
			}
		}

		try (Trx trx = new Trx()) {
			page = TransactionManager.getCurrentTransaction().getObject(Page.class, page.getId());

			if (viewPermission && publishPermission) {
				assertThat(page).as("Page").isOnline().isNotModified();
			} else if (viewPermission && !publishPermission) {
				assertThat(page).as("Page").isOffline().hasQueuedPublish(testUser);
			} else {
				assertThat(page).as("Page").isOffline().hasNoQueuedPublish().isNotModified();
			}
		}
	}

	protected static enum Localization {
		INHERITED,

		LOCALIZED,

		LOCAL
	}
}