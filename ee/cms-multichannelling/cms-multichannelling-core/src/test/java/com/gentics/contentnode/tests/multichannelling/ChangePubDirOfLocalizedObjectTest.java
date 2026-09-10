package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test changing the publish directory of a localized object
 */
public class ChangePubDirOfLocalizedObjectTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Master node
	 */
	private Node master;

	/**
	 * Channel
	 */
	private Node channel;

	/**
	 * Setup test data
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(
				Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		master = ContentNodeTestDataUtils.createNode("Node", "Node", PublishTarget.NONE);
		channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "Channel", "/");
	}

	/**
	 * Test changing the publish directory back to the same of the master object
	 * @throws Exception
	 */
	@Test
	public void testChangePublishDirOfLocalized() throws Exception {
		Folder folder = null;
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = ContentNodeTestDataUtils.createFolder(master.getFolder(), "Folder");
			folder = t.getObject(folder, true);
			folder.setPublishDir("test");
			folder.save();
			trx.success();

			Template template = ContentNodeTestDataUtils.createTemplate(folder, "Template");
			ContentNodeTestDataUtils.createPage(folder, template, "Page");
		}

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			Folder localizedFolder = ContentNodeTestDataUtils.localize(folder, channel);
			localizedFolder = t.getObject(localizedFolder, true);

			localizedFolder.setPublishDir("testnew");
			localizedFolder.save();

			localizedFolder.setPublishDir("test");
			localizedFolder.save();
		}
	}

	/**
	 * Test changing the publish directory recursively for a localized folder with an
	 * also localized subfolder. This should also change the publish directory of all
	 * (and only) it's localized subfolders.
	 * @throws Exception
	 */
	@Test
	public void testRecursiveChangePublishDirOfLocalizedFolder() throws Exception {
		//Master folder
		Folder folder = null;
		//Master subfolder
		Folder subfolder = null;
		//Master subfolder that is not localized later on
		Folder notLocalizedSubfolder = null;
		//Local subfolder in channel
		Folder localFolder = null;

		//Create folder and subfolder
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = ContentNodeTestDataUtils.createFolder(master.getFolder(), "Folder");
			folder = t.getObject(folder, true);
			folder.setPublishDir("test");
			folder.save();
			subfolder = ContentNodeTestDataUtils.createFolder(folder, "Subfolder");
			subfolder = t.getObject(subfolder, true);
			subfolder.setPublishDir("test");
			subfolder.save();
			trx.success();
			notLocalizedSubfolder = ContentNodeTestDataUtils.createFolder(folder, "NotLocalizedSubfolder");
			notLocalizedSubfolder = t.getObject(notLocalizedSubfolder, true);
			notLocalizedSubfolder.setPublishDir("test");
			notLocalizedSubfolder.save();
			trx.success();
		}

		//Localize folder and subfolder
		Folder localizedFolder = null;
		Folder localizedSubfolder = null;
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			localizedFolder = ContentNodeTestDataUtils.localize(folder, channel);
			localizedFolder = t.getObject(localizedFolder, true);
			localizedFolder.setName("localized Folder");
			localizedFolder.save();
			trx.success();
		}

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			localizedSubfolder = ContentNodeTestDataUtils.localize(subfolder, channel);
			localizedSubfolder = t.getObject(localizedSubfolder, true);
			localizedSubfolder.setName("localized Subfolder");
			localizedSubfolder.save();
			trx.success();
		}

		//Create a local subfolder in the channel and change its pubdir
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			localFolder = ContentNodeTestDataUtils.createFolder(localizedFolder, "local Folder", channel);
			localFolder = t.getObject(localFolder, true);
			localFolder.setPublishDir("test");
			localFolder.save();
			trx.success();
		}

		//Change pubdir of localized folder recursively
		try (Trx trx = new Trx()) {
			FolderSaveRequest request = new FolderSaveRequest();
			com.gentics.contentnode.rest.model.Folder restFolder = new com.gentics.contentnode.rest.model.Folder();
			restFolder.setId(localizedFolder.getId());
			restFolder.setPublishDir("newPubDir");
			request.setFolder(restFolder);
			request.setNodeId(channel.getId());
			request.setRecursive(true);
			request.setForegroundTime(60);
			getFolderResource().save(Integer.toString(localizedFolder.getId()), request);
			trx.success();
		}

		//Check if the recursive change was successfull
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = t.getObject(folder);
			subfolder = t.getObject(subfolder);
			localizedFolder = t.getObject(localizedFolder);
			localizedSubfolder = t.getObject(localizedSubfolder);
			notLocalizedSubfolder = t.getObject(notLocalizedSubfolder);
			localFolder = t.getObject(localFolder);
			assertEquals("Check pubdir of master Folder", "/test/", folder.getPublishDir());
			assertEquals("Check pubdir of master Subfolder", "/test/", subfolder.getPublishDir());
			assertEquals("Check new pubdir of localized Folder", "/newPubDir/", localizedFolder.getPublishDir());
			assertEquals("Check new pubdir of localized Subfolder", "/newPubDir/", localizedSubfolder.getPublishDir());
			assertEquals("Check pubdir of not localized Subfolder", "/test/", notLocalizedSubfolder.getPublishDir());
			assertEquals("Check new pubdir of local Subfolder", "/newPubDir/", localFolder.getPublishDir());
		}
	}
}
