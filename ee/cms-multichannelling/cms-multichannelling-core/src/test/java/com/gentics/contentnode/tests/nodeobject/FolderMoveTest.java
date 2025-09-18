package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.assertResultMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for moving folders
 */
public class FolderMoveTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	@Before
	public void setup() throws Exception {
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
	}

	/**
	 * Test whether trying to move a root folder fails
	 * @throws Exception
	 */
	@Test
	public void testMoveRootFolder() throws Exception {
		Node node = ContentNodeTestDataUtils.createNode("test", "Test Node", PublishTarget.NONE);
		Node target = ContentNodeTestDataUtils.createNode("target", "Target Node", PublishTarget.NONE);

		Transaction t = TransactionManager.getCurrentTransaction();
		OpResult result = t.getObject(Folder.class, node.getFolder().getId(), true).move(target.getFolder(), 0);
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "move.rootfolder");
	}

	/**
	 * Test whether trying to move a folder without target folder fails
	 * @throws Exception
	 */
	@Test
	public void testMoveWithoutTargetFolder() throws Exception {
		Node node = ContentNodeTestDataUtils.createNode("test", "Test Node", PublishTarget.NONE);
		Folder folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");

		Transaction t = TransactionManager.getCurrentTransaction();
		OpResult result = t.getObject(Folder.class, folder.getId(), true).move(null, 0);
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "op.no_targetfolder");
	}

	/**
	 * Test whether trying to move a folder into an inexistent channel fails
	 * @throws Exception
	 */
	@Test
	public void testMoveIntoInvalidChannel() throws Exception {
		Node node = ContentNodeTestDataUtils.createNode("test", "Test Node", PublishTarget.NONE);
		Folder source = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Source");
		Folder target = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Target");

		Transaction t = TransactionManager.getCurrentTransaction();
		OpResult result = t.getObject(Folder.class, source.getId(), true).move(target, 47110815);
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "move.missing_targetchannel");
	}

	/**
	 * Test whether trying to move a folder into a target folder, that is not visible in the target channel fails
	 * @throws Exception
	 */
	@Test
	public void testMoveIntoWrongChannel() throws Exception {
		Node node = ContentNodeTestDataUtils.createNode("test", "Test Node", PublishTarget.NONE);
		Node channel = ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/");
		Node targetChannel = ContentNodeTestDataUtils.createChannel(node, "target", "Target Channel", "/");

		Folder folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
		Folder target = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Target", channel);

		Transaction t = TransactionManager.getCurrentTransaction();
		OpResult result = t.getObject(Folder.class, folder.getId(), true).move(target, ObjectTransformer.getInt(targetChannel.getId(), 0));
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "move.target_invisible");
	}

	/**
	 * Test whether trying to move a folder into itself fails
	 * @throws Exception
	 */
	@Test
	public void testMoveIntoItself() throws Exception {
		Node node = ContentNodeTestDataUtils.createNode("test", "Test Node", PublishTarget.NONE);
		Folder folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");

		Transaction t = TransactionManager.getCurrentTransaction();
		OpResult result = t.getObject(Folder.class, folder.getId(), true).move(folder, 0);
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "move.target_is_child");
	}

	/**
	 * Test whether trying to move a folder into a childfolder fails
	 * @throws Exception
	 */
	@Test
	public void testMoveIntoChild() throws Exception {
		Node node = ContentNodeTestDataUtils.createNode("test", "Test Node", PublishTarget.NONE);
		Folder folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
		Folder childFolder = ContentNodeTestDataUtils.createFolder(folder, "Childfolder");

		Transaction t = TransactionManager.getCurrentTransaction();
		OpResult result = t.getObject(Folder.class, folder.getId(), true).move(childFolder, 0);
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "move.target_is_child");
	}

	/**
	 * Test whether trying to move a folder next to another folder with the same name fails
	 * @throws Exception
	 */
	@Test
	public void testNamingConflict() throws Exception {
		Node node = ContentNodeTestDataUtils.createNode("test", "Test Node", PublishTarget.NONE);
		Folder source = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Source");
		Folder toMove = ContentNodeTestDataUtils.createFolder(source, "Folder");
		Folder target = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Target");
		ContentNodeTestDataUtils.createFolder(target, "Folder");

		Transaction t = TransactionManager.getCurrentTransaction();
		OpResult result = t.getObject(Folder.class, toMove.getId(), true).move(target, 0);
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "451.a_folder_with_this_name");
	}

	/**
	 * Test whether moving folders into other nodes will fail, if this creates naming conflicts for files
	 * @throws Exception
	 */
	@Test
	public void testMoveFileNamingConflicts() throws Exception {
		Node sourceNode = ContentNodeTestDataUtils.createNode("source", "Source Node", PublishTarget.NONE);
		Folder toMove = ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "Folder");
		ContentNodeTestDataUtils.createFile(toMove, "testfile.txt", "This is the testfile".getBytes("UTF-8"));
		Node targetNode = ContentNodeTestDataUtils.createNode("target", "Target Node", PublishTarget.NONE);
		Folder inTheWay = ContentNodeTestDataUtils.createFolder(targetNode.getFolder(), "In the way");
		ContentNodeTestDataUtils.createFile(inTheWay, "testfile.txt", "This file is in the way".getBytes("UTF-8"));

		Transaction t = TransactionManager.getCurrentTransaction();
		OpResult result = t.getObject(Folder.class, toMove.getId(), true).move(targetNode.getFolder(), 0);
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "move.namingconflict", toMove.getName(), targetNode.getFolder().getName(), inTheWay.getPublishDir() + "testfile.txt");
	}

	/**
	 * Test whether moving folders into other nodes will fail, if this creates naming conflicts for pages
	 * @throws Exception
	 */
	@Test
	public void testMovePageNamingConflicts() throws Exception {
		Node sourceNode = ContentNodeTestDataUtils.createNode("source", "Source Node", PublishTarget.NONE);
		Folder toMove = ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "Folder");
		Node targetNode = ContentNodeTestDataUtils.createNode("target", "Target Node", PublishTarget.NONE);
		Folder inTheWay = ContentNodeTestDataUtils.createFolder(targetNode.getFolder(), "In the way");

		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);
		template.setSource("");
		template.setName("Template");
		template.addFolder(toMove);
		template.addFolder(inTheWay);
		template.save();
		t.commit(false);

		Page testPage = ContentNodeTestDataUtils.createPage(toMove, template, "Testpage");
		Page conflictPage = ContentNodeTestDataUtils.createPage(inTheWay, template, "Testpage");
		assertEquals("The pages must have the same filename", testPage.getFilename(), conflictPage.getFilename());

		OpResult result = t.getObject(Folder.class, toMove.getId(), true).move(targetNode.getFolder(), 0);
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "move.namingconflict", toMove.getName(), targetNode.getFolder().getName(), inTheWay.getPublishDir() + testPage.getFilename());
	}

	/**
	 * Test whether moving folders into other nodes will fail, if this creates naming conflicts between pages and files
	 * @throws Exception
	 */
	@Test
	public void testMovePageFileNamingConflicts() throws Exception {
		Node sourceNode = ContentNodeTestDataUtils.createNode("source", "Source Node", PublishTarget.NONE);
		Folder toMove = ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "Folder");
		Node targetNode = ContentNodeTestDataUtils.createNode("target", "Target Node", PublishTarget.NONE);
		Folder inTheWay = ContentNodeTestDataUtils.createFolder(targetNode.getFolder(), "In the way");

		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);
		template.setSource("");
		template.setName("Template");
		template.addFolder(toMove);
		template.addFolder(inTheWay);
		template.save();
		t.commit(false);

		Page testPage = ContentNodeTestDataUtils.createPage(toMove, template, "Testpage");
		ContentNodeTestDataUtils.createFile(inTheWay, testPage.getFilename(), "This is the conflicting file".getBytes("UTF-8"));

		OpResult result = t.getObject(Folder.class, toMove.getId(), true).move(targetNode.getFolder(), 0);
		assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
		assertResultMessage(result, "move.namingconflict", toMove.getName(), targetNode.getFolder().getName(), inTheWay.getPublishDir() + testPage.getFilename());
	}

	/**
	 * Test whether trying to move a folder with insufficient privileges fails
	 * @throws Exception
	 */
	@Test
	public void testMoveInsufficientPrivileges() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		UserGroup adminGroup = t.getObject(UserGroup.class, 2);
		assertNotNull("Admin Group must exist", adminGroup);
		
		UserGroup testGroup = Creator.createUsergroup("Testgroup", "", adminGroup);
		SystemUser testUser = Creator.createUser("test", "test", "Test", "Test", "", Arrays.asList(testGroup));
		
		Node node = ContentNodeTestDataUtils.createNode("test", "Test Node", PublishTarget.NONE);
		Folder toMove = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
		Node target = ContentNodeTestDataUtils.createNode("target", "Target Node", PublishTarget.NONE);

		testPrivileges(Arrays.asList(testGroup), ObjectTransformer.getInt(testUser.getId(), 0), toMove, target.getFolder(),
				new FolderPerm(toMove, PermHandler.PERM_VIEW, PermHandler.PERM_FOLDER_DELETE),
				new FolderPerm(target.getFolder(), PermHandler.PERM_VIEW, PermHandler.PERM_FOLDER_CREATE));
	}

	/**
	 * Test whether moving the folder requires all of the given folder permissions
	 * @param groups list of groups of the user
	 * @param userId user id
	 * @param toMove folder that shall be moved
	 * @param target target folder
	 * @param perms list of permissions, that is expected to be required
	 * @throws Exception
	 */
	protected void testPrivileges(List<UserGroup> groups, int userId, Folder toMove, Folder target, FolderPerm...perms) throws Exception {
		for (UserGroup group : groups) {
			DBUtils.executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] {group.getId()});
		}

		for (FolderPerm omit : perms) {
			for (FolderPerm perm : perms) {
				if (omit == perm) {
					continue;
				}

				// set perm
				PermHandler.setPermissions(perm.type, perm.folderId, groups, perm.fullPerm.toString());
			}

			for (Permission bits : omit.perms) {
				for (UserGroup group : groups) {
					DBUtils.executeUpdate("DELETE FROM perm WHERE o_type = ? AND o_id = ? AND usergroup_id = ?", new Object[] {omit.type, omit.folderId, group.getId()});
				}
				// set bits and test
				PermHandler.setPermissions(omit.type, omit.folderId, groups, bits.toString());
				TransactionManager.getCurrentTransaction().commit(false);

				// do test (expect failure)
				testContext.getContext().startTransaction(userId);
				try {
					OpResult result = TransactionManager.getCurrentTransaction().getObject(Folder.class, toMove.getId(), true).move(target, 0);
					assertEquals("Check operation status", OpResult.Status.FAILURE, result.getStatus());
				} finally {
					testContext.getContext().startTransaction();
				}
			}
		}

		// set all perms and test again (expect success)
		for (UserGroup group : groups) {
			DBUtils.executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] {group.getId()});
		}
		for (FolderPerm perm : perms) {
			PermHandler.setPermissions(perm.type, perm.folderId, groups, perm.fullPerm.toString());
		}
		TransactionManager.getCurrentTransaction().commit(false);

		testContext.getContext().startTransaction(userId);
		try {
			OpResult result = TransactionManager.getCurrentTransaction().getObject(Folder.class, toMove.getId(), true).move(target, 0);
			assertEquals("Check operation status", OpResult.Status.OK, result.getStatus());
		} finally {
			testContext.getContext().startTransaction();
		}
	}

	/**
	 * Helper class to encapsulate the necessary folder permissions
	 */
	protected static class FolderPerm {
		protected int type;

		protected int folderId;

		protected Permission fullPerm;

		protected List<Permission> perms = new ArrayList<Permission>();

		public FolderPerm(Folder folder, int...permBits) throws NodeException {
			if (folder.isRoot()) {
				type = Node.TYPE_NODE;
			} else {
				type = Folder.TYPE_FOLDER;
			}
			folderId = ObjectTransformer.getInt(folder.getId(), 0);

			fullPerm = new Permission(permBits);
			for (int omit : permBits) {
				Permission perm = new Permission(StringUtils.repeat("0", 32));
				for (int bit : permBits) {
					if (bit == omit) {
						continue;
					}
					perm.mergeBits(new Permission(bit).toString());
				}

				perms.add(perm);
			}
		}
	}
}
