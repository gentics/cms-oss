package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.AutoCommit;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Class for move tests with channel specific permissions.
 */
@RunWith(Parameterized.class)
public class MCPermissionMoveTest {

	private static enum Localization {
		INHERITED,
		LOCALIZED,
		LOCAL
	}

	private static final int ADMIN_GROUP_ID = 2;

	@ClassRule public static DBTestContext testContext = new DBTestContext();

	private static Node sourceNode;
	private static Node sourceChannel;
	private static Node targetNode;
	private static Node targetChannel;
	private static Node otherNode;
	private static Integer testUserId;

	private final Localization sourceObjectLoc;
	private final Folder sourceFolder;
	private final Folder targetFolder;
	private final boolean sourcePermission;
	private final boolean targetPermission;

	/**
	 * Create the test parameters.
	 *
	 * @return A collection of test parameter sets.
	 */
	@Parameters(name = "{index}: source folder {0}, source object {1}, target folder {2}, source permission {3}, target permission {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (Localization sourceFolderLoc: Localization.values()) {
			for (Localization sourceObjectLoc: Localization.values()) {
				if (sourceFolderLoc == Localization.LOCAL && sourceObjectLoc != Localization.LOCAL) {
					continue;
				}

				for (Localization targetFolderLoc: Localization.values()) {
					for (boolean sourcePermission: Arrays.asList(true, false)) {
						for (boolean targetPermission: Arrays.asList(true, false)) {
							data.add(new Object[] {
								sourceFolderLoc,
								sourceObjectLoc,
								targetFolderLoc,
								sourcePermission,
								targetPermission
							});
						}
					}
				}
			}
		}

		return data;
	}

	/**
	 * Setup user, nodes and channels.
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().setFeature(Feature.MULTICHANNELLING, true);

		try (AutoCommit ac = new AutoCommit()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			UserGroup adminGroup = t.getObject(UserGroup.class, ADMIN_GROUP_ID);

			assertNotNull("AdminGroup must exist", adminGroup);

			testUserId = Creator.createUser("test", "test", "test", "test", "", Arrays.asList(adminGroup)).getId();
			ac.success();
		}

		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUserId, 0))) {
			sourceNode = ContentNodeTestDataUtils.createNode();
			sourceChannel = ContentNodeTestDataUtils.createChannel(sourceNode, "Source Channel", "source", "/");
			targetNode = ContentNodeTestDataUtils.createNode();
			targetChannel = ContentNodeTestDataUtils.createChannel(targetNode, "Target Channel", "target", "/");

			otherNode = ContentNodeTestDataUtils.createNode();

			trx.success();
		}
	}

	/**
	 * Initialize source and target folders as well as channel restrictions.
	 * 
	 * @param sourceFolderLoc The localization type of the source folder.
	 * @param sourceObjectLoc The localization type of the source object.
	 * @param targetFolderLoc The localization type of the target folder.
	 * @param sourcePermission Grant permissions on the source folder.
	 * @param targetPermission Grant permissions on the target folder.
	 */
	public MCPermissionMoveTest(
			Localization sourceFolderLoc,
			Localization sourceObjectLoc,
			Localization targetFolderLoc,
			boolean sourcePermission,
			boolean targetPermission) throws Exception {
		this.sourceObjectLoc = sourceObjectLoc;
		this.sourcePermission = sourcePermission;
		this.targetPermission = targetPermission;
		
		try (Trx trx = new Trx()) {
			switch (sourceFolderLoc) {
			case INHERITED:
				sourceFolder = ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "Source Folder");
				break;

			case LOCALIZED:
				Folder masterFolder = ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "Source Folder");

				sourceFolder = ContentNodeTestDataUtils.localize(masterFolder, sourceChannel);
				break;
				
			case LOCAL:
			default:
				sourceFolder = ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "Source Folder", sourceChannel);
				break;
			}

			switch (targetFolderLoc) {
			case INHERITED:
				targetFolder = ContentNodeTestDataUtils.createFolder(targetNode.getFolder(), "Target Folder");
				break;
				
			case LOCALIZED:
				Folder masterFolder = ContentNodeTestDataUtils.createFolder(targetNode.getFolder(), "Target Folder");
				
				targetFolder = ContentNodeTestDataUtils.localize(masterFolder, targetChannel);
				break;
				
			case LOCAL:
			default:
				targetFolder = ContentNodeTestDataUtils.createFolder(targetNode.getFolder(), "Target Folder", targetChannel);
				break;
			}

			Set<Integer> nodeIds = new HashSet<Integer>();
			
			nodeIds.add(ObjectTransformer.getInt(otherNode.getId(), 0));
			if (sourcePermission) {
				nodeIds.add(ObjectTransformer.getInt(sourceChannel.getId(), 0));
			}
			if (targetPermission) {
				nodeIds.add(ObjectTransformer.getInteger(targetChannel.getId(), 0));
			}
			
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser testUser = t.getObject(SystemUser.class, testUserId, true);
			Map<Integer, Set<Integer>> groupNodeRestrictions = testUser.getGroupNodeRestrictions();
			
			groupNodeRestrictions.put(2, nodeIds);
			testUser.save();
			testUser = t.getObject(SystemUser.class, testUser.getId());

			trx.success();
		}
	}

	/**
	 * Checks the result of a move operation.
	 * 
	 * @param result The result returned by the move operation.
	 * @param objectType The type of the object to move.
	 * 
	 * @throws Exception On test failure.
	 */
	private void checkResult(OpResult result, String objectType) throws Exception {
		if (Localization.LOCALIZED.equals(sourceObjectLoc)) {
			assertFalse("Move operation should have failed for localized " + objectType, result.isOK());
		} else if (Localization.INHERITED.equals(sourceObjectLoc)) {
			assertFalse("Move operation should have failed for inherited " + objectType, result.isOK());
		} else if (sourcePermission && targetPermission) {
			assertTrue("Move operation should have succeeded, but failed with " + result.getMessages(), result.isOK());
		} else {
			assertFalse("Move operation should have failed", result.isOK());
		}
	}

	/**
	 * Tests moving of folders.
	 * 
	 * @throws Exception On test failure.
	 */
	@Test
	public void testMoveFolder() throws Exception {
		Integer folderId;
		
		try (Trx trx = new Trx()) {
			switch (sourceObjectLoc) {
			case INHERITED:
				folderId = ContentNodeTestDataUtils.createFolder(sourceFolder, "Testfolder").getId();
				break;

			case LOCALIZED:
				Folder masterFolder = ContentNodeTestDataUtils.createFolder(sourceFolder, "Masterfolder");
				
				folderId = ContentNodeTestDataUtils.localizeFolder(sourceChannel, masterFolder).getId();
				break;
				
			case LOCAL:
			default:
				folderId = ContentNodeTestDataUtils.createFolder(sourceFolder, "Testfolder", sourceChannel).getId();
			}
		}

		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUserId, 0))) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder toMove = t.getObject(Folder.class, folderId, true);
			OpResult result = toMove.move(targetFolder, targetChannel.getId());

			checkResult(result, "page");
			
			trx.success();
		}
	}

	/**
	 * Tests moving of pages.
	 * 
	 * @throws Exception On test failure.
	 */
	@Test
	public void testMovePage() throws Exception {
		Template template;
		Page sourcePage;


		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUserId, 0))) {
			template = ContentNodeTestDataUtils.createTemplate(sourceNode.getFolder(), "Template");
		}

		try (Trx trx = new Trx()) {
			switch (sourceObjectLoc) {
			case INHERITED:
				sourcePage = ContentNodeTestDataUtils.createPage(sourceFolder, template, "Testpage");
				break;

			case LOCALIZED:
				Page masterPage = ContentNodeTestDataUtils.createPage(sourceFolder, template, "Masterpage");

				sourcePage = ContentNodeTestDataUtils.localizePage(sourceChannel, masterPage);
				break;

			case LOCAL:
			default:
				sourcePage = ContentNodeTestDataUtils.createPage(sourceFolder, template, "Testpage", sourceChannel);
			}
		}
		
		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUserId, 0))) {
			OpResult result = sourcePage.move(targetFolder, targetChannel.getId(), true);

			checkResult(result, "page");
			
			trx.success();
		}
	}

	/**
	 * Tests moving of files.
	 * 
	 * @throws Exception On test failure.
	 */
	@Test
	public void testMoveFile() throws Exception {
		File sourceFile;
		byte[] data = "Testfile contents".getBytes();

		try (Trx trx = new Trx()) {
			switch (sourceObjectLoc) {
			case INHERITED:
				sourceFile = ContentNodeTestDataUtils.createFile(sourceFolder, "Testfile", data);
				break;

			case LOCALIZED:
				File masterFile = ContentNodeTestDataUtils.createFile(sourceFolder, "Masterfile", data);
				
				sourceFile = ContentNodeTestDataUtils.localizeFile(sourceChannel , masterFile);
				break;

			case LOCAL:
			default:
				sourceFile = ContentNodeTestDataUtils.createFile(sourceFolder, "Testfile", data, sourceChannel);
			}
		}
		
		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUserId, 0))) {
			OpResult result = sourceFile.move(targetFolder, targetChannel.getId());

			checkResult(result, "file");

			trx.success();
		}
	}

	/**
	 * Tests moving of images.
	 * 
	 * @throws Exception On test failure.
	 */
	@Test
	public void testMoveImage() throws Exception {
		File sourceImage;
		byte[] data = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));

		try (Trx trx = new Trx()) {
			switch (sourceObjectLoc) {
			case INHERITED:
				sourceImage = ContentNodeTestDataUtils.createImage(sourceFolder, "Testimage", data);
				break;

			case LOCALIZED:
				ImageFile masterImage = (ImageFile) ContentNodeTestDataUtils.createImage(sourceFolder, "Masterimage", data);
				
				sourceImage = ContentNodeTestDataUtils.localizeImage(sourceChannel, masterImage);
				break;

			case LOCAL:
			default:
				sourceImage = ContentNodeTestDataUtils.createImage(sourceFolder, "Testimage", data, sourceChannel);
			}
		}
		
		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUserId, 0))) {
			OpResult result = sourceImage.move(targetFolder, targetChannel.getId());

			checkResult(result, "image");

			trx.success();
		}
	}
}
