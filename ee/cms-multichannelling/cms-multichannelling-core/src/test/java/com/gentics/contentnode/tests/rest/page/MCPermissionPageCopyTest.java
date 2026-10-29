package com.gentics.contentnode.tests.rest.page;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
import com.gentics.contentnode.factory.AutoCommit;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.page.PageCopyOpResult;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for channel specific permissions and page copy
 */
@RunWith(value = Parameterized.class)
public class MCPermissionPageCopyTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node sourceNode;
	private static Node sourceChannel;
	private static Node targetNode;
	private static Node targetChannel;
	private static UserGroup adminGroup;
	private static SystemUser testUser;
	private static Template template;
	private static Node otherNode;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: source folder {0}, source page {1}, target folder {2}, source permission {3}, target permission {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (Localization sourceFolderLoc : Localization.values()) {
			for (Localization sourcePageLoc : Localization.values()) {
				if (sourceFolderLoc == Localization.LOCAL && sourcePageLoc != Localization.LOCAL) {
					continue;
				}
				for (Localization targetFolderLoc : Localization.values()) {
					for (boolean sourcePermission : Arrays.asList(true, false)) {
						for (boolean targetPermission : Arrays.asList(true, false)) {
							data.add(new Object[] {sourceFolderLoc, sourcePageLoc, targetFolderLoc, sourcePermission, targetPermission});
						}
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().setFeature(Feature.MULTICHANNELLING, true);

		try (AutoCommit ac = new AutoCommit()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			adminGroup = t.getObject(UserGroup.class, 2);
			assertNotNull("AdminGroup must exist", adminGroup);

			testUser = Creator.createUser("test", "test", "test", "test", "", Arrays.asList(adminGroup));
			ac.success();
		}

		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUser.getId(), 0))) {
			sourceNode = ContentNodeTestDataUtils.createNode();
			sourceChannel = ContentNodeTestDataUtils.createChannel(sourceNode, "Source Channel", "source", "/");
			targetNode = ContentNodeTestDataUtils.createNode();
			targetChannel = ContentNodeTestDataUtils.createChannel(targetNode, "Target Channel", "target", "/");

			otherNode = ContentNodeTestDataUtils.createNode();

			template = ContentNodeTestDataUtils.createTemplate(sourceNode.getFolder(), "Template");

			trx.success();
		}
	}

	private Localization sourceFolderLoc;
	private Localization sourcePageLoc;
	private Localization targetFolderLoc;
	private boolean sourcePermission;
	private boolean targetPermission;

	/**
	 * Create a test instance
	 * @param sourceFolderLoc source folder localization setting
	 * @param sourcePageLoc source page localization setting
	 * @param targetFolderLoc target folder localization setting
	 * @param sourcePermission source permission
	 * @param targetPermission target permission
	 */
	public MCPermissionPageCopyTest(Localization sourceFolderLoc, Localization sourcePageLoc, Localization targetFolderLoc, boolean sourcePermission,
			boolean targetPermission) {
		this.sourceFolderLoc = sourceFolderLoc;
		this.sourcePageLoc = sourcePageLoc;
		this.targetFolderLoc = targetFolderLoc;
		this.sourcePermission = sourcePermission;
		this.targetPermission = targetPermission;
	}

	/**
	 * Test copying page
	 */
	@Test
	public void testCopyPage() throws Exception {
		// create page to move and source and target folders
		Folder sourceFolder = null;
		Page sourcePage = null;
		Folder targetFolder = null;
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
				sourceFolder = ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "Source Folder", sourceChannel);
				break;
			}

			switch (sourcePageLoc) {
			case INHERITED:
				sourcePage = ContentNodeTestDataUtils.createPage(sourceFolder, template, "Page");
				break;
			case LOCALIZED:
				Page masterPage = ContentNodeTestDataUtils.createPage(sourceFolder, template, "Page");
				sourcePage = ContentNodeTestDataUtils.localize(masterPage, sourceChannel);
				break;
			case LOCAL:
				sourcePage = ContentNodeTestDataUtils.createPage(sourceFolder, template, "Page", sourceChannel);
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
				targetFolder = ContentNodeTestDataUtils.createFolder(targetNode.getFolder(), "Target Folder", targetChannel);
				break;
			}

			// restrict user to channels
			Transaction t = TransactionManager.getCurrentTransaction();
			Set<Integer> nodeIds = new HashSet<Integer>();
			nodeIds.add(ObjectTransformer.getInt(otherNode.getId(), 0));
			if (sourcePermission) {
				nodeIds.add(ObjectTransformer.getInt(sourceChannel.getId(), 0));
			}
			if (targetPermission) {
				nodeIds.add(ObjectTransformer.getInteger(targetChannel.getId(), 0));
			}
			testUser = t.getObject(SystemUser.class, testUser.getId(), true);
			Map<Integer, Set<Integer>> groupNodeRestrictions = testUser.getGroupNodeRestrictions();
			groupNodeRestrictions.put(2, nodeIds);
			testUser.save();
			testUser = t.getObject(SystemUser.class, testUser.getId());
			trx.success();
		}

		// copy the page
		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUser.getId(), 0))) {
			PageCopyOpResult result = sourcePage.copyTo(ObjectTransformer.getInt(sourceChannel.getId(), 0), targetFolder, true, 0,
					ObjectTransformer.getInt(targetChannel.getId(), 0));
			if (sourcePermission && targetPermission) {
				assertTrue("Copy operation should have succeeded, but failed with " + result.getMessages(), result.isOK());
			} else {
				assertFalse("Copy operation should have failed", result.isOK());
			}
			trx.success();
		}
	}

	protected static enum Localization {
		INHERITED,

		LOCALIZED,

		LOCAL
	}
}
