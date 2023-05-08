package com.gentics.contentnode.tests.wastebin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.AutoCommit;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.etc.StringUtils;

/**
 * Test cases for wastebin actions (restore and remove)
 * @author norbert
 *
 */
@RunWith(value = Parameterized.class)
public class WastebinActionsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	private TestedType type;

	private boolean wastebinPermission;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: test: {0}, permission: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (TestedType type : TestedType.values()) {
			for (boolean wastebinPermission : Arrays.asList(true, false)) {
				data.add(new Object[] {type, wastebinPermission});
			}
		}
		return data;
	}

	/**
	 * Create test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), true);
		node = ContentNodeTestDataUtils.createNode("node", "Node", PublishTarget.NONE);
		t.commit(false);

		// create two groups with users
		UserGroup nodeGroup = t.getObject(UserGroup.class, 2);
		UserGroup groupWithPermission = Creator.createUsergroup("With Wastebin Permission", "", nodeGroup);
		Creator.createUser("perm", "perm", "perm", "perm", "", Arrays.asList(groupWithPermission));
		UserGroup groupWithoutPermission = Creator.createUsergroup("Without Wastebin Permission", "", nodeGroup);
		Creator.createUser("noperm", "noperm", "noperm", "noperm", "", Arrays.asList(groupWithoutPermission));

		// set permissions
		PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(groupWithPermission), new Permission(PermHandler.PERM_VIEW,
				PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_DELETE, PermHandler.PERM_FOLDER_DELETE, PermHandler.PERM_NODE_WASTEBIN).toString());
		PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(groupWithoutPermission), new Permission(PermHandler.PERM_VIEW,
				PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_DELETE, PermHandler.PERM_FOLDER_DELETE).toString());
		t.commit(false);

		template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "", "Template");
		t.commit(false);
	}

	@Before
	public void setup() throws Exception {
		if (wastebinPermission) {
			testContext.getContext().login("perm", "perm");
		} else {
			testContext.getContext().login("noperm", "noperm");
		}
	}

	/**
	 * Create a test instance
	 * @param type tested type
	 * @param wastebinPermission true for wastebin permission
	 */
	public WastebinActionsTest(TestedType type, boolean wastebinPermission) {
		this.type = type;
		this.wastebinPermission = wastebinPermission;
	}

	/**
	 * Test removing an object from the wastebin
	 * @throws Exception
	 */
	@Test
	public void testRemove() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create the object
		LocalizableNodeObject<?> object = type.create(node.getFolder(), template);

		// delete the object (putting it into the wastebin
		object.delete();
		t.commit(false);

		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			object = t.getObject(object);
			assertNotNull("Object must still exist", object);
			assertTrue("Object must be in wastebin now", object.isDeleted());
		}

		// try to remove the object from the wastebin
		try (AutoCommit trx = new AutoCommit()) {
			ContentNodeRESTUtils.assertResponse(type.deleteFromWastebin(object), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// check whether removing worked
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			object = t.getObject(object);
			if (wastebinPermission) {
				assertNull("Object must be removed now", object);
			} else {
				assertNotNull("Object must still exist", object);
				assertTrue("Object must still be in wastebin now", object.isDeleted());
			}
		}
	}

	/**
	 * Test removing objects recursively
	 * @throws Exception
	 */
	@Test
	public void testRemoveRecursive() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");

		// create the tested object
		NodeObject object = type.create(folder, template);

		// delete the folder (putting it and the tested object into the wastebin)
		folder.delete();
		t.commit(false);

		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			folder = t.getObject(folder);
			assertNotNull("Folder must still exist", folder);
			assertTrue("Folder must be in wastebin now", folder.isDeleted());

			object = t.getObject(object);
			assertNotNull("Object must still exist", object);
			assertTrue("Object must be in wastebin now", object.isDeleted());
		}

		// try removing the folder from wastebin
		try (AutoCommit trx = new AutoCommit()) {
			ContentNodeRESTUtils.assertResponse(TestedType.folder.deleteFromWastebin(folder), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();
		// check whether removing worked
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			folder = t.getObject(folder);
			object = t.getObject(object);
			if (wastebinPermission) {
				assertNull("Folder must be removed now", folder);
				assertNull("Object must be removed now", object);
			} else {
				assertNotNull("Folder must still exist", folder);
				assertTrue("Folder must still be in wastebin now", folder.isDeleted());
				assertNotNull("Object must still exist", object);
				assertTrue("Object must still be in wastebin now", object.isDeleted());
			}
		}
	}

	/**
	 * Test restoring an object from the wastebin
	 * @throws Exception
	 */
	@Test
	public void testRestore() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create the object
		NodeObject object = type.create(node.getFolder(), template);

		// delete the object (putting it into the wastebin
		object.delete();
		t.commit(false);

		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			object = t.getObject(object);
			assertNotNull("Object must still exist", object);
			assertTrue("Object must be in wastebin now", object.isDeleted());
		}

		// try to restore the object from wastebin now
		try (AutoCommit trx = new AutoCommit()) {
			ContentNodeRESTUtils.assertResponse(type.restoreFromWastebin(object), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();
		// check whether restoring worked
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			object = t.getObject(object);
			assertNotNull("Object must still exist", object);
			if (wastebinPermission) {
				assertFalse("Object must be restored from wastebin now", object.isDeleted());
			} else {
				assertTrue("Object must still be in wastebin now", object.isDeleted());
			}
		}
	}

	/**
	 * Test restoring an object from the wastebin, with a naming conflict
	 * @throws Exception
	 */
	@Test
	public void testRestoreWithConflict() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create the object
		LocalizableNodeObject<?> object = type.create(node.getFolder(), template);
		String name = object.getName();
		String filename = null;
		if (object instanceof Page) {
			filename = ((Page) object).getFilename();
		}

		// delete the object (putting it into the wastebin
		object.delete();
		t.commit(false);

		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			object = t.getObject(object);
			assertNotNull("Object must still exist", object);
			assertTrue("Object must be in wastebin now", object.isDeleted());
		}

		// create another object with the same name now (should be possible)
		LocalizableNodeObject<?> conflictingObject = type.create(node.getFolder(), name, template);
		assertNotNull("Conflicting object must be created", conflictingObject);
		assertEquals("Check name of conflicting object", name, conflictingObject.getName());
		if (conflictingObject instanceof Page) {
			assertEquals("Check filename of conflicting page", filename, ((Page) conflictingObject).getFilename());
		}

		// try to restore the object from wastebin now
		try (AutoCommit trx = new AutoCommit()) {
			ContentNodeRESTUtils.assertResponse(type.restoreFromWastebin(object), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();
		// check whether restoring worked
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			object = t.getObject(object);
			assertNotNull("Object must still exist", object);
			if (wastebinPermission) {
				assertFalse("Object must be restored from wastebin now", object.isDeleted());
				assertFalse("Name must be different now.", StringUtils.isEqual(name, object.getName()));
				if (object instanceof Page) {
					assertFalse("Filename must be different now.", StringUtils.isEqual(filename, ((Page) object).getFilename()));
				}
			} else {
				assertTrue("Object must still be in wastebin now", object.isDeleted());
				assertEquals("Name must be unchanged.", name, object.getName());
				if (object instanceof Page) {
					assertEquals("Filename must be unchanged", filename, ((Page) object).getFilename());
				}
			}
		}
	}

	/**
	 * Test restoring an object in a deleted folder
	 * @throws Exception
	 */
	@Test
	public void testRestoreRecursive() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");

		// create the object
		NodeObject object = type.create(folder, template);

		// delete the folder (putting it and the tested object into the wastebin)
		folder.delete();
		t.commit(false);

		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			folder = t.getObject(folder);
			assertNotNull("Folder must still exist", folder);
			assertTrue("Folder must be in wastebin now", folder.isDeleted());

			object = t.getObject(object);
			assertNotNull("Object must still exist", object);
			assertTrue("Object must be in wastebin now", object.isDeleted());
		}

		// try to restore the object now
		try (AutoCommit trx = new AutoCommit()) {
			ContentNodeRESTUtils.assertResponse(type.restoreFromWastebin(object), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();
		// check whether restoring worked
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			folder = t.getObject(folder);
			assertNotNull("Folder must still exist", folder);

			object = t.getObject(object);
			assertNotNull("Object must still exist", object);

			if (wastebinPermission) {
				assertFalse("Folder must be restored from wastebin now", folder.isDeleted());
				assertFalse("Object must be restored from wastebin now", object.isDeleted());
			} else {
				assertTrue("Folder must still be in wastebin now", folder.isDeleted());
				assertTrue("Object must still be in wastebin now", object.isDeleted());
			}
		}
	}
}
