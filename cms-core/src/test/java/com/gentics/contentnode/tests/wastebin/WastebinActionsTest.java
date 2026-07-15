package com.gentics.contentnode.tests.wastebin;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.etc.Feature;
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
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBSessionClosure;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.etc.StringUtils;

/**
 * Test cases for wastebin actions (restore and remove)
 * @author norbert
 *
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = Feature.WASTEBIN)
public class WastebinActionsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	private static Node node;

	private static Template template;

	private TestedType type;

	private boolean wastebinPermission;

	private static Integer permUserId;

	private static Integer noPermUserId;

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
		testContext.getContext().getTransaction().commit();

		node = supply(() -> ContentNodeTestDataUtils.createNode("node", "Node", PublishTarget.NONE));

		// create two groups with users
		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, 2));
		UserGroup groupWithPermission = supply(() -> Creator.createUsergroup("With Wastebin Permission", "", nodeGroup));
		permUserId = supply(() -> Creator.createUser("perm", "perm", "perm", "perm", "", Arrays.asList(groupWithPermission)).getId());
		UserGroup groupWithoutPermission = supply(() -> Creator.createUsergroup("Without Wastebin Permission", "", nodeGroup));
		noPermUserId = supply(() -> Creator.createUser("noperm", "noperm", "noperm", "noperm", "", Arrays.asList(groupWithoutPermission)).getId());

		// set permissions
		operate(() -> {
			PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(groupWithPermission), new Permission(PermHandler.PERM_VIEW,
					PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_DELETE, PermHandler.PERM_FOLDER_DELETE, PermHandler.PERM_NODE_WASTEBIN).toString());
			PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(groupWithoutPermission), new Permission(PermHandler.PERM_VIEW,
					PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_DELETE, PermHandler.PERM_FOLDER_DELETE).toString());
		});

		template = supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "", "Template"));
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

		// create the object
		LocalizableNodeObject<?> object = supply(() -> type.create(node.getFolder(), template));

		// delete the object (putting it into the wastebin
		consume(NodeObject::delete, object);

		operate(t -> {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				LocalizableNodeObject<?> reloaded = t.getObject(object);
				assertNotNull("Object must still exist", reloaded);
				assertTrue("Object must be in wastebin now", reloaded.isDeleted());
			}
		});

		// try to remove the object from the wastebin
		if (!wastebinPermission) {
			exceptionChecker.expect(InsufficientPrivilegesException.class);
		}
		try (DBSessionClosure ses = new DBSessionClosure(wastebinPermission ? permUserId : noPermUserId)) {
			ContentNodeRESTUtils.assertResponse(type.deleteFromWastebin(object), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		operate(t -> {
			// check whether removing worked
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				LocalizableNodeObject<?> reloaded = t.getObject(object);
				if (wastebinPermission) {
					assertNull("Object must be removed now", reloaded);
				} else {
					assertNotNull("Object must still exist", reloaded);
					assertTrue("Object must still be in wastebin now", reloaded.isDeleted());
				}
			}
		});
	}

	/**
	 * Test removing objects recursively
	 * @throws Exception
	 */
	@Test
	public void testRemoveRecursive() throws Exception {
		Folder folder = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));

		// create the tested object
		NodeObject object = supply(() -> type.create(folder, template));

		// delete the folder (putting it and the tested object into the wastebin)
		consume(NodeObject::delete, folder);

		operate(t -> {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				Folder reloaded = t.getObject(folder);
				assertNotNull("Folder must still exist", reloaded);
				assertTrue("Folder must be in wastebin now", reloaded.isDeleted());

				NodeObject reloadedObject = t.getObject(object);
				assertNotNull("Object must still exist", reloadedObject);
				assertTrue("Object must be in wastebin now", reloadedObject.isDeleted());
			}
		});

		// try removing the folder from wastebin
		if (!wastebinPermission) {
			exceptionChecker.expect(InsufficientPrivilegesException.class);
		}
		try (DBSessionClosure ses = new DBSessionClosure(wastebinPermission ? permUserId : noPermUserId)) {
			ContentNodeRESTUtils.assertResponse(TestedType.folder.deleteFromWastebin(folder), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		operate(t -> {
			// check whether removing worked
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				Folder reloaded = t.getObject(folder);
				NodeObject reloadedObject = t.getObject(object);
				if (wastebinPermission) {
					assertNull("Folder must be removed now", reloaded);
					assertNull("Object must be removed now", reloadedObject);
				} else {
					assertNotNull("Folder must still exist", reloaded);
					assertTrue("Folder must still be in wastebin now", reloaded.isDeleted());
					assertNotNull("Object must still exist", reloadedObject);
					assertTrue("Object must still be in wastebin now", reloadedObject.isDeleted());
				}
			}
		});
	}

	/**
	 * Test restoring an object from the wastebin
	 * @throws Exception
	 */
	@Test
	public void testRestore() throws Exception {
		// create the object
		NodeObject object = supply(() -> type.create(node.getFolder(), template));
		consume(NodeObject::unlock, object);

		// delete the object (putting it into the wastebin
		consume(NodeObject::delete, object);

		operate(t -> {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				NodeObject reloaded = t.getObject(object);
				assertNotNull("Object must still exist", reloaded);
				assertTrue("Object must be in wastebin now", reloaded.isDeleted());
			}
		});

		// try to restore the object from wastebin now
		if (!wastebinPermission) {
			exceptionChecker.expect(InsufficientPrivilegesException.class);
		}
		try (DBSessionClosure ses = new DBSessionClosure(wastebinPermission ? permUserId : noPermUserId)) {
			ContentNodeRESTUtils.assertResponse(type.restoreFromWastebin(object), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		operate(t -> {
			// check whether restoring worked
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				NodeObject reloaded = t.getObject(object);
				assertNotNull("Object must still exist", reloaded);
				if (wastebinPermission) {
					assertFalse("Object must be restored from wastebin now", reloaded.isDeleted());
				} else {
					assertTrue("Object must still be in wastebin now", reloaded.isDeleted());
				}
			}
		});
	}

	/**
	 * Test restoring an object from the wastebin, with a naming conflict
	 * @throws Exception
	 */
	@Test
	public void testRestoreWithConflict() throws Exception {
		// create the object
		LocalizableNodeObject<?> object = supply(() -> type.create(node.getFolder(), template));
		consume(NodeObject::unlock, object);
		String name = object.getName();
		String filename = object instanceof Page ? ((Page) object).getFilename() : null;

		// delete the object (putting it into the wastebin
		consume(NodeObject::delete, object);

		operate(t -> {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				NodeObject reloaded = t.getObject(object);
				assertNotNull("Object must still exist", reloaded);
				assertTrue("Object must be in wastebin now", reloaded.isDeleted());
			}
		});

		// create another object with the same name now (should be possible)
		LocalizableNodeObject<?> conflictingObject = supply(() -> type.create(node.getFolder(), name, template));
		assertNotNull("Conflicting object must be created", conflictingObject);
		assertEquals("Check name of conflicting object", name, conflictingObject.getName());
		if (conflictingObject instanceof Page) {
			assertEquals("Check filename of conflicting page", filename, ((Page) conflictingObject).getFilename());
		}

		// try to restore the object from wastebin now
		if (!wastebinPermission) {
			exceptionChecker.expect(InsufficientPrivilegesException.class);
		}
		try (DBSessionClosure ses = new DBSessionClosure(wastebinPermission ? permUserId : noPermUserId)) {
			ContentNodeRESTUtils.assertResponse(type.restoreFromWastebin(object), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		// check whether restoring worked
		operate(t -> {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				LocalizableNodeObject<?> reloaded = t.getObject(object);
				assertNotNull("Object must still exist", reloaded);
				if (wastebinPermission) {
					assertFalse("Object must be restored from wastebin now", reloaded.isDeleted());
					assertFalse("Name must be different now.", StringUtils.isEqual(name, reloaded.getName()));
					if (reloaded instanceof Page) {
						assertFalse("Filename must be different now.", StringUtils.isEqual(filename, ((Page) reloaded).getFilename()));
					}
				} else {
					assertTrue("Object must still be in wastebin now", reloaded.isDeleted());
					assertEquals("Name must be unchanged.", name, reloaded.getName());
					if (reloaded instanceof Page) {
						assertEquals("Filename must be unchanged", filename, ((Page) reloaded).getFilename());
					}
				}
			}
		});
	}

	/**
	 * Test restoring an object in a deleted folder
	 * @throws Exception
	 */
	@Test
	public void testRestoreRecursive() throws Exception {
		Folder folder = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));

		// create the object
		NodeObject object = supply(() -> type.create(folder, template));
		consume(NodeObject::unlock, object);

		// delete the folder (putting it and the tested object into the wastebin)
		consume(Folder::delete, folder);

		operate(t -> {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				Folder reloadedFolder = t.getObject(folder);
				assertNotNull("Folder must still exist", reloadedFolder);
				assertTrue("Folder must be in wastebin now", reloadedFolder.isDeleted());

				NodeObject reloadedObject = t.getObject(object);
				assertNotNull("Object must still exist", reloadedObject);
				assertTrue("Object must be in wastebin now", reloadedObject.isDeleted());
			}
		});

		// try to restore the object now
		if (!wastebinPermission) {
			exceptionChecker.expect(InsufficientPrivilegesException.class);
		}
		try (DBSessionClosure ses = new DBSessionClosure(wastebinPermission ? permUserId : noPermUserId)) {
			ContentNodeRESTUtils.assertResponse(type.restoreFromWastebin(object), wastebinPermission ? ResponseCode.OK : ResponseCode.PERMISSION);
		}

		operate(t -> {
			// check whether restoring worked
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				Folder reloadedFolder = t.getObject(folder);
				assertNotNull("Folder must still exist", reloadedFolder);

				NodeObject reloadedObject = t.getObject(object);
				assertNotNull("Object must still exist", reloadedObject);

				if (wastebinPermission) {
					assertFalse("Folder must be restored from wastebin now", reloadedFolder.isDeleted());
					assertFalse("Object must be restored from wastebin now", reloadedObject.isDeleted());
				} else {
					assertTrue("Folder must still be in wastebin now", reloadedFolder.isDeleted());
					assertTrue("Object must still be in wastebin now", reloadedObject.isDeleted());
				}
			}
		});
	}
}
