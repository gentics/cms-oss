package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.factory.Trx.supply;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCFileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCTemplateResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Tests for the <code>/unlocalize/</code> REST endpoints with
 * different combinations of permissions.
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class UnlocalizePermissionTest extends AbstractMultichannellingSandboxTest {

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	/**
	 * Enum for the different NodeObject types to test.
	 */
	private enum ObjectType {
		File(File.class),
		Folder(Folder.class),
		Image(ImageFile.class),
		Page(Page.class),
		Template(Template.class);

		public final Class<? extends LocalizableNodeObject<?>> type;

		private ObjectType(Class<? extends LocalizableNodeObject<?>> type) {
			this.type = type;
		}
	}

	/** The ID of the admin group. */
	private static final int ADMIN_GROUP_ID = 2;

	/**
	 * Generates the parameters for the parameterized test runner.
	 * @return The parameters for the tests.
	 */
	@Parameters(name = "{index}: object type {0}, channel sync perm {1}, delete perm {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (ObjectType objType : ObjectType.values()) {
			data.add(new Object[] { objType, Boolean.FALSE, Boolean.FALSE });
			data.add(new Object[] { objType, Boolean.TRUE, Boolean.FALSE });
			data.add(new Object[] { objType, Boolean.FALSE, Boolean.TRUE });
		}

		return data;
	}

	/** The object type for the current test. */
	private ObjectType objType;

	/** Whether the user should have the channel sync permission for the current test. */
	private boolean channelSyncPerm;

	/** Whether the user should have the delete permission for the current test. */
	private boolean deletePerm;

	/** The group to use for the current test. */
	private UserGroup testGroup;

	/**
	 * Test User
	 */
	private SystemUser testUser;

	/** The login of the test user. */
	private String testUserLogin = "testuser";

	/** The password of the test user. */
	private String testUserPasswd = "12345";

	/** The root folder of the master node. */
	private Folder rootFolder;

	/** The parent folder for the created objects in this test. */
	private Folder testFolder;

	/** The ID of the localized copy. */
	private Integer localizedObjectId;

	/** The ID of the master object. */
	private Integer masterObjectId;

	/**
	 * Creates a new parameterized test instance.
	 *
	 * @param objType The object type for the current test.
	 * @param channelSyncPerm Whether the user should have the channel sync permission.
	 * @param deletePerm Whether the user should have the delete permission.
	 */
	public UnlocalizePermissionTest(ObjectType objType, Boolean channelSyncPerm, Boolean deletePerm) {
		this.objType = objType;
		this.channelSyncPerm = channelSyncPerm;
		this.deletePerm = deletePerm;
	}

	/**
	 * Test setup.
	 *
	 * <ol>
	 *   <li>Creates a group and user</li>
	 *   <li>Creates a test object and a localized copy of it</li>
	 *   <li>Sets permissions for the test user</li>
	 * </ol>
	 * @throws Exception On errors.
	 */
	@Before
	public void setup() throws Exception {
		createGroupAndUser();
		createLocalizedObject();
		setPermissions();
	}

	/**
	 * Creates a new group and a user in it.
	 *
	 * @throws Exception On errors.
	 */
	private void createGroupAndUser() throws Exception {
		UserGroup adminGroup = supply(t -> t.getObject(UserGroup.class, ADMIN_GROUP_ID));

		testGroup = supply(() -> Creator.createUsergroup("Test Group", "", adminGroup));
		testUserLogin = "test-user";

		testUser = supply(() -> Creator.createUser(
			testUserLogin,
			testUserPasswd,
			"Test",
			"User",
			"no@where.com",
			Arrays.asList(testGroup)));
	}

	/**
	 * Creates an object of the currently tested type and a localized copy of it.
	 * @throws Exception On errors.
	 */
	private void createLocalizedObject() throws Exception {
		try (Trx trx = new Trx(testUser)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Node master = t.getObject(Node.class, MASTER_ID);
			Node channel = t.getObject(Node.class, channelId);

			rootFolder = master.getFolder();
			testFolder = Creator.createFolder(rootFolder, "Test Folder", "/");

			switch (objType) {
			case Folder:
				masterObjectId = testFolder.getId();
				localizedObjectId =  ContentNodeTestDataUtils.localize(testFolder, channel).getId();

				return;

			case File:
				File testFile = Creator.createFile(testFolder, "test.txt", "Test".getBytes(), master);

				masterObjectId = testFile.getId();
				localizedObjectId = ContentNodeTestDataUtils.localize(testFile, channel).getId();

				return;

			case Image:
				ImageFile testImage = Creator.createImage(testFolder, "test.jpg", master);

				masterObjectId = testImage.getId();
				localizedObjectId = ContentNodeTestDataUtils.localize(testImage, channel).getId();

				return;

			default:
				// The Template and Page cases are handled below.
			}

			Template testTemplate = Creator.createTemplate(
					"Test Template",
					"Just testing!",
					testFolder);

			if (objType == ObjectType.Template) {
				masterObjectId = testTemplate.getId();
				localizedObjectId = ContentNodeTestDataUtils.localize(testTemplate, channel).getId();

				return;
			}

			Page testPage = Creator.createPage("Test Page", testFolder, testTemplate);

			masterObjectId = testPage.getId();
			localizedObjectId = ContentNodeTestDataUtils.localize(testPage, channel).getId();
			trx.success();
		}
	}

	/**
	 * Sets the permissions of the currently used group according
	 * to {@link #channelSyncPerm} and {@link #deletePerm}.
	 *
	 * @throws Exception On errors.
	 */
	private void setPermissions() throws Exception {
		Trx.operate(() -> {
			List<UserGroup> groups = Arrays.asList(testGroup);

			if (this.channelSyncPerm) {
				PermHandler.setPermissions(
						Node.TYPE_NODE,
						rootFolder.getId(),
						groups,
						new Permission(PermHandler.PERM_CHANNEL_SYNC).toString());
			}

			if (this.deletePerm) {
				String perms = new Permission(
						PermHandler.PERM_FOLDER_UPDATE,
						PermHandler.PERM_FOLDER_DELETE,
						PermHandler.PERM_PAGE_VIEW,
						PermHandler.PERM_PAGE_UPDATE,
						PermHandler.PERM_PAGE_DELETE,
						PermHandler.PERM_TEMPLATE_VIEW,
						PermHandler.PERM_TEMPLATE_UPDATE,
						PermHandler.PERM_TEMPLATE_DELETE,
						PermHandler.PERM_VIEW)
						.toString();
				
				PermHandler.setPermissions(Folder.TYPE_FOLDER, testFolder.getId(), groups, perms);
			}
		});
	}

	/**
	 * Checks the response of the <code>unlocalize</code> request.
	 *
	 * Depending on the permissions of the group the response code
	 * must be <code>OK</code> or <code>PERMISSION</code>.
	 *
	 * Also checks if the localized copy should still exist.
	 *
	 * @param response The response to check.
	 * @throws Exception On errors and assertion failures.
	 */
	private void checkResponse(GenericResponse response) throws Exception {
		if (channelSyncPerm || deletePerm) {
			assertEquals(
				"Check if the response is OK.",
				ResponseCode.OK,
				response.getResponseInfo().getResponseCode());
		}else {
			assertEquals(
				"Check if the response is PERMISSION.",
				ResponseCode.PERMISSION,
				response.getResponseInfo().getResponseCode());
		}

		LocalizableNodeObject<?> object;

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			object = t.getObject(objType.type, masterObjectId);
			assertNotNull("Check if the master object still exists.", object);

			object = t.getObject(objType.type, localizedObjectId);
			if (deletePerm) {
				assertNull("Check if the localized object is gone.", object);

				// Check if the object is inherited in channel instead of localized
				try (ChannelTrx cTrx = new ChannelTrx(channelId)) {
					t = TransactionManager.getCurrentTransaction();
					object = t.getObject(objType.type, masterObjectId);
					assertNotNull("Check if the object exists.", object);
					assertTrue("Check if the object is still inherited in channel.", object.isInherited());
				}
			} else {
				assertNotNull("Check if the localized object still exists.", object);
			}
		}
	}

	/**
	 * Test unlocalizing an object via the corresponding
	 * <code>unlocalize</code> REST endpoint.
	 *
	 * @throws Exception On errors and test failures.
	 */
	@Test
	public void testUnlocalize() throws Exception {
		UnlocalizeRequest request = new UnlocalizeRequest();

		request.setForegroundTime(Integer.MAX_VALUE);
		request.setChannelId(channelId);

		GenericResponse resp = supply(testUser, () -> {
			GenericResponse response;
			Transaction t = TransactionManager.getCurrentTransaction();

			switch (objType) {
			case File:
				response = new MCFileResourceImpl().unlocalize(localizedObjectId, request);

				break;

			case Folder:
				response = new MCFolderResourceImpl().unlocalize(localizedObjectId, request);

				break;

			case Image:
				response = new MCImageResourceImpl().unlocalize(localizedObjectId, request);

				break;

			case Page:
				response = new MCPageResourceImpl().unlocalize(localizedObjectId, request);

				break;

			case Template:
				if (!channelSyncPerm && !deletePerm) {
					exceptionChecker.expect(InsufficientPrivilegesException.class);
				}
				response = new MCTemplateResourceImpl().unlocalize(localizedObjectId, request);

				break;

			default:
				throw new NodeException("Unexpected object type: " + objType);
			}
			return response;
		});

		checkResponse(resp);
	}
}
