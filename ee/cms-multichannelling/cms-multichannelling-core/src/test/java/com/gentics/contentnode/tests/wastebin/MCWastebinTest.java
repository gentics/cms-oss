package com.gentics.contentnode.tests.wastebin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.request.FolderMoveRequest;
import com.gentics.contentnode.rest.model.request.LocalizeRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.PageLocalizeRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.i18n.CNI18nString;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Multichannelling Wastebin Tests
 */
@GCNFeature(set = {Feature.MULTICHANNELLING, Feature.WASTEBIN})
public class MCWastebinTest {
	public final static String TEST_LOGIN = "test";
	public final static String TEST_PASSWORD = "test";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test Master Node
	 */
	private static Node master;

	/**
	 * Test channel
	 */
	private static Node channel;

	/**
	 * Test subchannel
	 */
	private static Node subChannel;

	/**
	 * Test User
	 */
	private static SystemUser testUser;

	/**
	 * SID of the testuser
	 */
	private static String sid;
	private static ContentLanguage german;
	private static ContentLanguage english;

	@BeforeClass
	public static void setupOnce() throws Exception {
		UserGroup adminGroup = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(UserGroup.class, 1));
		german = getLanguage("de");
		english = getLanguage("en");
		testUser = Trx.supply(() -> Creator.createUser(TEST_LOGIN, TEST_PASSWORD, "Test", "Test", "", Arrays.asList(adminGroup)));
		master = Trx.supply(() -> ContentNodeTestDataUtils.createNode("master", "Master Node", PublishTarget.NONE, german, english));
		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(master, "Channel Node", "channel", "/"));
		subChannel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(channel, "Subchannel Node", "subchannel", "/"));
		sid = ContentNodeRESTUtils.login(TEST_LOGIN, TEST_PASSWORD);
	}

	/**
	 * Test putting master page with a localized copy into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testDeleteMasterPage() throws Exception {
		// create a page with localized copies
		Template template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(master.getFolder(), "Template source", "Template"));
		Page masterPage = Trx.supply(() -> ContentNodeTestDataUtils.createPage(master.getFolder(), template, "Page"));
		Page channelPage = Trx.supply(() -> ContentNodeTestDataUtils.localize(masterPage, channel));
		Page subChannelPage = Trx.supply(() -> ContentNodeTestDataUtils.localize(channelPage, subChannel));

		// delete the page
		Trx.operate(() -> masterPage.delete());

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		try (WastebinFilter f = Wastebin.INCLUDE.set()) {
			// check whether page and all localized copies were put into the wastebin
			for (Page p : Arrays.asList(masterPage, channelPage, subChannelPage)) {
				Page page = t.getObject(Page.class, p.getId(), false, false, false);
				assertNotNull(p + " must still exist", page);
				assertTrue(p + " must be put into the wastebin", page.isDeleted());
			}
		}
		});
	}

	/**
	 * Test putting the localized copy of a page into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testDeleteLocalizedPage() throws Exception {
		// create a page with localized copies
		Template template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(master.getFolder(), "Template source", "Template"));
		Page masterPage = Trx.supply(() -> ContentNodeTestDataUtils.createPage(master.getFolder(), template, "Page"));
		Page channelPage = Trx.supply(() -> ContentNodeTestDataUtils.localize(masterPage, channel));
		Page subChannelPage = Trx.supply(() -> ContentNodeTestDataUtils.localize(channelPage, subChannel));

		// unlocalize the channel page
		Trx.operate(() -> channelPage.delete());

		Map<Node, Page> expectedPages = new HashMap<Node, Page>();
		expectedPages.put(master, masterPage);
		expectedPages.put(channel, masterPage);
		expectedPages.put(subChannel, subChannelPage);

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		for (Node c : Arrays.asList(master, channel, subChannel)) {
			try (ChannelTrx cTrx = new ChannelTrx(c)) {
				Page p = t.getObject(Page.class, masterPage.getId());
				assertNotNull("Could not get page for " + c, p);
				assertEquals("Did not get expected page for " + c, expectedPages.get(c), p);
			}
		}
		});
	}

	/**
	 * Test localizing the page (again) after putting the localized copy into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testLocalizePage() throws Exception {
		// create a page with localized copies
		Template template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(master.getFolder(), "Template source", "Template"));
		Page masterPage = Trx.supply(() -> ContentNodeTestDataUtils.createPage(master.getFolder(), template, "Page"));
		Page channelPage = Trx.supply(() -> ContentNodeTestDataUtils.localize(masterPage, channel));

		// unlocalize the channel page
		Trx.operate(() -> channelPage.delete());

		// localize the page in the channel
		Trx.operate(() -> {
		PageLocalizeRequest request = new PageLocalizeRequest();
		request.setChannelId(channel.getId());
		request.setForegroundTime(50000);
		ContentNodeRESTUtils.assertResponseOK(new MCPageResourceImpl().localize(masterPage.getId(), request));
		});

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				assertNull("Old page in channel must have been deleted now", t.getObject(channelPage));
		}
		});
	}

	/**
	 * Test putting a master folder into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testDeleteMasterFolder() throws Exception {
		// create a folder with localized copies
		Folder masterFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(master.getFolder(), "Folder"));
		Folder channelFolder = Trx.supply(() -> ContentNodeTestDataUtils.localize(masterFolder, channel));
		Folder subChannelFolder = Trx.supply(() -> ContentNodeTestDataUtils.localize(channelFolder, subChannel));

		// delete the folder
		Trx.operate(() -> masterFolder.delete());

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			// check whether folder and all localized copies were put into the wastebin
			for (Folder f : Arrays.asList(masterFolder, channelFolder, subChannelFolder)) {
				Folder folder = t.getObject(Folder.class, f.getId(), false, false, false);
				assertNotNull(f + " must still exist", folder);
				assertTrue(f + " must be put into the wastebin", folder.isDeleted());
			}
		}
		});
	}

	/**
	 * Test putting a localized folder into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testDeleteLocalizedFolder() throws Exception {
		// create a folder with localized copies
		Folder masterFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(master.getFolder(), "Folder"));
		Folder channelFolder = Trx.supply(() -> ContentNodeTestDataUtils.localize(masterFolder, channel));
		Folder subChannelFolder = Trx.supply(() -> ContentNodeTestDataUtils.localize(channelFolder, subChannel));

		// unlocalize the channel folder
		Trx.operate(() -> channelFolder.delete());

		Map<Node, Folder> expectedFolders = new HashMap<Node, Folder>();
		expectedFolders.put(master, masterFolder);
		expectedFolders.put(channel, masterFolder);
		expectedFolders.put(subChannel, subChannelFolder);

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		for (Node c : Arrays.asList(master, channel, subChannel)) {
			try (ChannelTrx cTrx = new ChannelTrx(c)) {
				Folder f = t.getObject(Folder.class, masterFolder.getId());
				assertNotNull("Could not get folder for " + c, f);
				assertEquals("Did not get expected folder for " + c, expectedFolders.get(c), f);
			}
		}
		});
	}

	/**
	 * Test localizing a folder (again) after putting the localized copy into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testLocalizeFolder() throws Exception {
		// create a folder with localized copies
		Folder masterFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(master.getFolder(), "Folder"));
		Folder channelFolder = Trx.supply(() -> ContentNodeTestDataUtils.localize(masterFolder, channel));

		// unlocalize the channel folder
		Trx.operate(() -> channelFolder.delete());

		// localize the folder in the channel
		Trx.operate(() -> {
		LocalizeRequest request = new LocalizeRequest();
		request.setChannelId(channel.getId());
		ContentNodeRESTUtils.assertResponseOK(new MCFolderResourceImpl().localize(masterFolder.getId(), request));
		});

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				assertNull("Old folder in channel must have been deleted now", t.getObject(channelFolder));
		}
		});
	}

	/**
	 * Test putting a master file into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testDeleteMasterFile() throws Exception {
		// create a file with localized copies
		File masterFile = Trx.supply(() -> ContentNodeTestDataUtils.createFile(master.getFolder(), "testfile.txt", "Contents".getBytes()));
		File channelFile = Trx.supply(() -> ContentNodeTestDataUtils.localize(masterFile, channel));
		File subChannelFile = Trx.supply(() -> ContentNodeTestDataUtils.localize(channelFile, subChannel));

		// delete the file
		Trx.operate(() -> masterFile.delete());

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			// check whether file and all localized copies were put into the wastebin
			for (File f : Arrays.asList(masterFile, channelFile, subChannelFile)) {
				File file = t.getObject(File.class, f.getId(), false, false, false);
				assertNotNull(f + " must still exist", file);
				assertTrue(f + " must be put into the wastebin", file.isDeleted());
			}
		}
		});
	}

	/**
	 * Test putting a localized file into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testDeleteLocalizedFile() throws Exception {
		// create a file with localized copies
		File masterFile = Trx.supply(() -> ContentNodeTestDataUtils.createFile(master.getFolder(), "testfile.txt", "Contents".getBytes()));
		File channelFile = Trx.supply(() -> ContentNodeTestDataUtils.localize(masterFile, channel));
		File subChannelFile = Trx.supply(() -> ContentNodeTestDataUtils.localize(channelFile, subChannel));

		// unlocalize the channel file
		Trx.operate(() -> channelFile.delete());

		Map<Node, File> expectedFiles = new HashMap<Node, File>();
		expectedFiles.put(master, masterFile);
		expectedFiles.put(channel, masterFile);
		expectedFiles.put(subChannel, subChannelFile);

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		for (Node c : Arrays.asList(master, channel, subChannel)) {
			try (ChannelTrx cTrx = new ChannelTrx(c)) {
				File f = t.getObject(File.class, masterFile.getId());
				assertNotNull("Could not get file for " + c, f);
				assertEquals("Did not get expected file for " + c, expectedFiles.get(c), f);
			}
		}
		});
	}

	/**
	 * Test localizing a file (again) after putting the localized copy into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testLocalizeFile() throws Exception {
		// create a file with localized copies
		File masterFile = Trx.supply(() -> ContentNodeTestDataUtils.createFile(master.getFolder(), "testfile.txt", "Contents".getBytes()));
		File channelFile = Trx.supply(() -> ContentNodeTestDataUtils.localize(masterFile, channel));

		// unlocalize the channel file
		Trx.operate(() -> channelFile.delete());

		// localize the file in the channel
		Trx.operate(() -> {
		LocalizeRequest request = new LocalizeRequest();
		request.setChannelId(channel.getId());
		ContentNodeRESTUtils.assertResponseOK(new MCFileResourceImpl().localize(masterFile.getId(), request));
		});

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				assertNull("Old file in channel must have been deleted now", t.getObject(channelFile));
		}
		});
	}

	/**
	 * Test deleting a node and a channel that have different objects
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteNode() throws Exception {
		// Create a node
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());

		// Create a folder
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Some folder"));

		// Create a template + page
		Template template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template", "Template"));
		Page page = Trx.supply(() -> ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Test page"));

		// Create a file
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(node.getFolder(), "File", "test".getBytes()));

		// Create a channel
		Node channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "publishdir"));

		Trx.operate(() -> {
		ContentNodeTestDataUtils.localize(folder,   channel);
		ContentNodeTestDataUtils.localize(template, channel);
		ContentNodeTestDataUtils.localize(page,     channel);
		ContentNodeTestDataUtils.localize(file,     channel);
		});

		// Delete the channel
		Trx.operate(() -> channel.delete());

		// Verify that the master node still has the unlocalized objects
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			assertNotNull("Folder must exist",   t.getObject(folder));
			assertNotNull("Template must exist", t.getObject(template));
			assertNotNull("Page must exist",     t.getObject(page));
			assertNotNull("File must exist",     t.getObject(file));
		}

		// Verify that the objects are really deleted in the channel (not in wastebin)
		assertNull("Channel must be really deleted",     t.getObject(channel));
		});


		// Delete the node
		Trx.operate(() -> node.delete());

		// Verify that the objects are really deleted in the node (not in wastebin)
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			assertNull("Node must be really deleted",     t.getObject(node));
			assertNull("Folder must be really deleted",   t.getObject(folder));
			assertNull("Template must be really deleted", t.getObject(template));
			assertNull("Page must be really deleted",     t.getObject(page));
			assertNull("File must be really deleted",     t.getObject(file));
		}
		});
	}

	/**
	 * Test moving a master page having localized copies in the wastebin to the channel.
	 * This is expected to remove all conflicting channel variants from the wastebin
	 * @throws NodeException
	 */
	@Test
	public void testMovePageToChannel() throws NodeException {
		// create page with localized copies in channel and subchannel
		Page page = Trx.supply(() -> ContentNodeTestDataUtils.createTemplateAndPage(master.getFolder(), "Page"));
		Page inChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(page, channel));
		Page inSubChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(page, subChannel));

		// delete localized copies
		Trx.operate(() -> inChannel.delete());
		Trx.operate(() -> inSubChannel.delete());

		// move the master into the channel
		Trx.operate(() -> {
			PageResource res = new PageResourceImpl();
			ObjectMoveRequest request = new ObjectMoveRequest();
			request.setFolderId(master.getFolder().getId());
			request.setNodeId(channel.getId());
			GenericResponse response = res.move(page.getId().toString(), request);
			try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
				CNI18nString msg = new CNI18nString("object.move.localization.error");
				msg.addParameter(I18NHelper.getPath(TransactionManager.getCurrentTransaction().getObject(inChannel)));
				ContentNodeRESTUtils.assertResponse(response, ResponseCode.FAILURE, "Error", new Message(Message.Type.WARNING, msg.toString()));
}
		});
	}

	/**
	 * Test moving a master folder having localized copies in the wastebin to the channel.
	 * This is expected to remove all conflicting channel variants from the wastebin
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderToChannel() throws NodeException {
		// create folder with localized copies in channel and subchannel
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(master.getFolder(), "Folder"));
		Folder inChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(folder, channel));
		Folder inSubChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(folder, subChannel));

		// delete localized copies
		Trx.operate(() -> inChannel.delete());
		Trx.operate(() -> inSubChannel.delete());

		// move the master into the channel
		Trx.operate(() -> {
			try (Trx trx = new Trx(sid, testUser.getId())) {
				FolderResource res = new FolderResourceImpl();
				FolderMoveRequest request = new FolderMoveRequest();
				request.setFolderId(master.getFolder().getId());
				request.setNodeId(channel.getId());
				request.setForegroundTime(Integer.MAX_VALUE);
				try {
					GenericResponse response = res.move(folder.getId().toString(), request);

					try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
						CNI18nString msg = new CNI18nString("object.move.localization.error");
						msg.addParameter(I18NHelper.getPath(TransactionManager.getCurrentTransaction().getObject(inChannel)));
						ContentNodeRESTUtils.assertResponse(response, ResponseCode.FAILURE, "Job finished with error",
								new Message(Message.Type.CRITICAL, msg.toString()));
					}
				} catch (Exception e) {
					throw new NodeException(e);
				}
				trx.success();
			}
		});
	}

	/**
	 * Test moving a master file having localized copies in the wastebin to the channel.
	 * This is expected to remove all conflicting channel variants from the wastebin
	 * @throws NodeException
	 */
	@Test
	public void testMoveFileToChannel() throws NodeException {
		// create file with localized copies in channel and subchannel
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(master.getFolder(), "testfile.txt", "content".getBytes()));
		File inChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(file, channel));
		File inSubChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(file, subChannel));

		// delete localized copies
		Trx.operate(() -> inChannel.delete());
		Trx.operate(() -> inSubChannel.delete());

		// move the master into the channel
		Trx.operate(() -> {
			FileResource res = new FileResourceImpl();
			ObjectMoveRequest request = new ObjectMoveRequest();
			request.setFolderId(master.getFolder().getId());
			request.setNodeId(channel.getId());
			try {
				GenericResponse response = res.move(file.getId().toString(), request);
				try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
					CNI18nString msg = new CNI18nString("object.move.localization.error");
					msg.addParameter(I18NHelper.getPath(TransactionManager.getCurrentTransaction().getObject(inChannel)));
					ContentNodeRESTUtils.assertResponse(response, ResponseCode.FAILURE, "Error", new Message(Message.Type.WARNING, msg.toString()));
				}
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
	}

	/**
	 * Test moving a master page having localized copies in the wastebin to another folder.
	 * This is expected to also change the folder of the localized copies
	 * @throws NodeException
	 */
	@Test
	public void testMovePageToFolder() throws NodeException {
		Folder targetFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(master.getFolder(), "Target"));

		// create page with localized copies in channel and subchannel
		Page page = Trx.supply(() -> ContentNodeTestDataUtils.createTemplateAndPage(master.getFolder(), "Page"));
		Page inChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(page, channel));
		Page inSubChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(page, subChannel));

		// delete localized copies
		Trx.operate(() -> inChannel.delete());
		Trx.operate(() -> inSubChannel.delete());

		// move the master into another folder
		Trx.operate(() -> {
			PageResource res = new PageResourceImpl();
			ObjectMoveRequest request = new ObjectMoveRequest();
			request.setFolderId(targetFolder.getId());
			GenericResponse response = res.move(page.getId().toString(), request);
			ContentNodeRESTUtils.assertResponseOK(response);
		});

		Trx.operate(() -> {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				Transaction t = TransactionManager.getCurrentTransaction();
				assertThat(t.getObject(inChannel)).as("Variant in channel").isNotNull();
				assertThat(t.getObject(inChannel).getFolder()).as("Folder of variant in channel").isEqualTo(targetFolder);
				assertThat(t.getObject(inSubChannel)).as("Variant in subchannel").isNotNull();
				assertThat(t.getObject(inSubChannel).getFolder()).as("Folder of variant in subchannel").isEqualTo(targetFolder);
			}
		});
	}

	/**
	 * Test moving a master folder having localized copies in the wastebin to another folder.
	 * This is expected to also change the folder of the localized copies
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderToFolder() throws NodeException {
		Folder targetFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(master.getFolder(), "Target"));

		// create folder with localized copies in channel and subchannel
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(master.getFolder(), "Folder"));
		Folder inChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(folder, channel));
		Folder inSubChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(folder, subChannel));

		// delete localized copies
		Trx.operate(() -> inChannel.delete());
		Trx.operate(() -> inSubChannel.delete());

		// move the master into the channel
		Trx.operate(() -> {
			try (Trx trx = new Trx(sid, testUser.getId())) {
				FolderResource res = new FolderResourceImpl();
				FolderMoveRequest request = new FolderMoveRequest();
				request.setFolderId(targetFolder.getId());
				request.setForegroundTime(Integer.MAX_VALUE);
				try {
					GenericResponse response = res.move(folder.getId().toString(), request);
					ContentNodeRESTUtils.assertResponseOK(response);
				} catch (Exception e) {
					throw new NodeException(e);
				}
				trx.success();
			}
		});

		Trx.operate(() -> {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				Transaction t = TransactionManager.getCurrentTransaction();
				assertThat(t.getObject(inChannel)).as("Variant in channel").isNotNull();
				assertThat(t.getObject(inChannel).getMother()).as("Folder of variant in channel").isEqualTo(targetFolder);
				assertThat(t.getObject(inSubChannel)).as("Variant in subchannel").isNotNull();
				assertThat(t.getObject(inSubChannel).getMother()).as("Folder of variant in subchannel").isEqualTo(targetFolder);
			}
		});
	}

	/**
	 * Test moving a master file having localized copies in the wastebin to another folder.
	 * This is expected to also change the folder of the localized copies
	 * @throws NodeException
	 */
	@Test
	public void testMoveFileToFolder() throws NodeException {
		Folder targetFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(master.getFolder(), "Target"));

		// create file with localized copies in channel and subchannel
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(master.getFolder(), "testfile.txt", "content".getBytes()));
		File inChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(file, channel));
		File inSubChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(file, subChannel));

		// delete localized copies
		Trx.operate(() -> inChannel.delete());
		Trx.operate(() -> inSubChannel.delete());

		// move the master into the channel
		Trx.operate(() -> {
			FileResource res = new FileResourceImpl();
			ObjectMoveRequest request = new ObjectMoveRequest();
			request.setFolderId(targetFolder.getId());
			try {
				GenericResponse response = res.move(file.getId().toString(), request);
				ContentNodeRESTUtils.assertResponseOK(response);
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});

		Trx.operate(() -> {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				Transaction t = TransactionManager.getCurrentTransaction();
				assertThat(t.getObject(inChannel)).as("Variant in channel").isNotNull();
				assertThat(t.getObject(inChannel).getFolder()).as("Folder of variant in channel").isEqualTo(targetFolder);
				assertThat(t.getObject(inSubChannel)).as("Variant in subchannel").isNotNull();
				assertThat(t.getObject(inSubChannel).getFolder()).as("Folder of variant in subchannel").isEqualTo(targetFolder);
			}
		});
	}

	/**
	 * Test creating a language variant in the master, when a conflicting language variant is in the wastebin (in master and channel)
	 * @throws NodeException
	 */
	@Test
	public void testTranslateMasterPage() throws NodeException {
		Template template = supply(() -> createTemplate(master.getFolder(), "Translate Test"));
		// create german and english master
		Page germanMaster = supply(() -> createPage(master.getFolder(), template, "Deutsch", null, german));
		Page englishMaster = supply(() -> {
			Page translation = (Page)germanMaster.copy();
			translation.setLanguage(english);
			translation.setName("English");
			translation.save();
			return translation.reload();
		});

		// localize english variant
		Page englishLocalized = execute(p -> localize(p, channel), englishMaster);

		// delete english master and localized copy into wastebin
		consume(Page::delete, englishMaster);

		for (Page page : Arrays.asList(englishMaster, englishLocalized)) {
			consume(p -> {
				try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
					Page deletedPage = p.reload();
					assertThat(deletedPage).isNotNull().isDeleted(true);
				}
			}, page);
		}

		// translate english master again (using REST API)
		consume(p -> {
			PageLoadResponse response = getPageResource().translate(germanMaster.getId(), "en", false, null);
			assertResponseCodeOk(response);
		}, germanMaster);

		// assert that both old translations are completely gone now
		for (Page page : Arrays.asList(englishMaster, englishLocalized)) {
			consume(p -> {
				try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
					Page deletedPage = p.reload();
					assertThat(deletedPage).isNull();
				}
			}, page);
		}
	}
}
