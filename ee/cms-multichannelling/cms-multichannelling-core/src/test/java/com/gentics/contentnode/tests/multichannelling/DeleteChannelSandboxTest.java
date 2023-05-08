package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test cases for deleting channels
 */
public class DeleteChannelSandboxTest extends AbstractMultichannellingSandboxTest {

	/**
	 * Test deleting a channel that contains localized/local folders
	 * @throws Exception
	 */
	@Test
	public void testDeleteChannelWithFolders() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the subchannel
		Node subchannel = t.getObject(Node.class, subChannelId);

		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();

		// Create test folder for our the new objects
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.folder_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// create a new folder as child of the masters root folder
		Folder newFolder = t.createObject(Folder.class);

		newFolder.setName(C.folder_channel_test.new_folder_name);
		newFolder.setMotherId(testFolder.getId());
		newFolder.save();
		t.commit(false);

		// create a localized copy of the test folder
		Folder newFolderLocalized = (Folder) newFolder.copy();

		newFolderLocalized.setChannelInfo(subchannel.getId(), newFolder.getChannelSetId());
		newFolderLocalized.save();
		t.commit(false);

		// create a local folder in the channel
		Folder newLocalFolder = t.createObject(Folder.class);

		newLocalFolder.setMotherId(testFolder.getId());
		newLocalFolder.setName(C.folder_channel_test.new_folder_name);
		newLocalFolder.setChannelInfo(subchannel.getId(), newLocalFolder.getChannelSetId());
		newLocalFolder.save();
		t.commit(false);

		// now delete the subchannel
		subchannel.delete();
		t.commit(false);

		// check whether the node is gone
		DBUtils.executeStatement("SELECT * FROM node WHERE id = " + subChannelId, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				if (rs.next()) {
					Assert.fail("Still found the subchannel after deleting it");
				}
			}
		});

		// check whether all channel specific folders are gone
		DBUtils.executeStatement("SELECT * FROM folder WHERE channel_id = " + subChannelId, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				if (rs.next()) {
					Assert.fail("Still found channel specific folders after deleting");
				}
			}
		});
	}

	/**
	 * Test deleting a channel that contains localized/local pages
	 * @throws Exception
	 */
	@Test
	public void testDeleteChannelWithPages() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the subchannel
		Node subchannel = t.getObject(Node.class, subChannelId);

		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();

		// Create test folder for our the new objects
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.folder_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// create a new page
		Page newPage = t.createObject(Page.class);

		newPage.setName(C.page_channel_test.new_page_name);
		newPage.setTemplateId(C.page_channel_test.new_page_template_id);
		newPage.setFolderId(testFolder.getId());
		newPage.save();
		t.commit(false);

		// create a localized copy of the test page
		Page newPageLocalized = (Page) newPage.copy();

		newPageLocalized.setChannelInfo(subchannel.getId(), newPage.getChannelSetId());
		newPageLocalized.save();
		t.commit(false);

		// create a local page in the channel
		Page newLocalPage = t.createObject(Page.class);

		newLocalPage.setName(C.page_channel_test.new_page_name);
		newLocalPage.setTemplateId(C.page_channel_test.new_page_template_id);
		newLocalPage.setFolderId(testFolder.getId());
		newLocalPage.setChannelInfo(subchannel.getId(), newLocalPage.getChannelSetId());
		newLocalPage.save();
		t.commit(false);

		// now delete the subchannel
		subchannel.delete();
		t.commit(false);

		// check whether the node is gone
		DBUtils.executeStatement("SELECT * FROM node WHERE id = " + subChannelId, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				if (rs.next()) {
					Assert.fail("Still found the subchannel after deleting it");
				}
			}
		});

		// check whether all channel specific pages are gone
		DBUtils.executeStatement("SELECT * FROM page WHERE channel_id = " + subChannelId, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				if (rs.next()) {
					Assert.fail("Still found channel specific pages after deleting");
				}
			}
		});
	}

	/**
	 * Test deleting a channel that contains localized/local files
	 * @throws Exception
	 */
	@Test
	public void testDeleteChannelWithFiles() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the subchannel
		Node subchannel = t.getObject(Node.class, subChannelId);

		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();

		// Create test folder for our the new objects
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.folder_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// create a new file
		File newFile = t.createObject(File.class);

		newFile.setName(C.file_channel_test.first_file_name);
		newFile.setFileStream(new ByteArrayInputStream(C.file_channel_test.first_file_data));
		newFile.setFiletype("application/octet-stream");
		newFile.setFolderId(testFolder.getId());
		newFile.save();
		t.commit(false);

		// create a localized copy of the test file
		File newFileLocalized = (File) newFile.copy();

		newFileLocalized.setChannelInfo(subchannel.getId(), newFile.getChannelSetId());
		newFileLocalized.save();
		t.commit(false);

		// create a local file in the channel
		File newLocalFile = t.createObject(File.class);

		newLocalFile.setName(C.file_channel_test.second_file_name);
		newLocalFile.setFileStream(new ByteArrayInputStream(C.file_channel_test.second_file_data));
		newLocalFile.setFiletype("application/octet-stream");
		newLocalFile.setFolderId(testFolder.getId());
		newLocalFile.setChannelInfo(subchannel.getId(), newLocalFile.getChannelSetId());
		newLocalFile.save();
		t.commit(false);

		// now delete the subchannel
		subchannel.delete();
		t.commit(false);

		// check whether the node is gone
		DBUtils.executeStatement("SELECT * FROM node WHERE id = " + subChannelId, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				if (rs.next()) {
					Assert.fail("Still found the subchannel after deleting it");
				}
			}
		});

		// check whether all channel specific files are gone
		DBUtils.executeStatement("SELECT * FROM contentfile WHERE channel_id = " + subChannelId, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				if (rs.next()) {
					Assert.fail("Still found channel specific files after deleting");
				}
			}
		});
	}

	/**
	 * Test deleting a channel that contains localized/local templates
	 * @throws Exception
	 */
	@Test
	public void testDeleteChannelWithTemplates() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the subchannel
		Node subchannel = t.getObject(Node.class, subChannelId);

		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();

		// Create test folder for our the new objects
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.folder_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// create a new template
		Template newTemplate = t.createObject(Template.class);

		newTemplate.setName(C.template_channel_test.first_template_name);
		newTemplate.setDescription(C.template_channel_test.first_template_description);
		newTemplate.setSource(C.template_channel_test.template_content);
		newTemplate.setMlId(1);
		newTemplate.setFolderId(testFolder.getId());
		newTemplate.save();
		t.commit(false);

		// create a localized copy of the test template
		Template newTemplateLocalized = (Template) newTemplate.copy();

		newTemplateLocalized.setChannelInfo(subchannel.getId(), newTemplate.getChannelSetId());
		newTemplateLocalized.save();
		t.commit(false);

		// create a local template in the channel
		Template newLocalTemplate = t.createObject(Template.class);

		newLocalTemplate.setName(C.template_channel_test.second_template_name);
		newLocalTemplate.setDescription(C.template_channel_test.second_template_description);
		newLocalTemplate.setSource(C.template_channel_test.template_content);
		newLocalTemplate.setMlId(1);
		newLocalTemplate.setFolderId(testFolder.getId());
		newLocalTemplate.setChannelInfo(subchannel.getId(), newLocalTemplate.getChannelSetId());
		newLocalTemplate.save();
		t.commit(false);

		// now delete the subchannel
		subchannel.delete();
		t.commit(false);

		// check whether the node is gone
		DBUtils.executeStatement("SELECT * FROM node WHERE id = " + subChannelId, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				if (rs.next()) {
					Assert.fail("Still found the subchannel after deleting it");
				}
			}
		});

		// check whether all channel specific templates are gone
		DBUtils.executeStatement("SELECT * FROM template WHERE channel_id = " + subChannelId, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				if (rs.next()) {
					Assert.fail("Still found channel specific templates after deleting");
				}
			}
		});
	}

	/**
	 * Test deleting a master folder that has a localized copy
	 * @throws NodeException
	 */
	@Test
	public void testDeleteMasterWithLocalizedFolder() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();

		// Create test folder
		Folder testFolder = t.createObject(Folder.class);
		testFolder.setName("Test Folder");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.save();
		t.commit(false);

		// Localize the test folder
		Folder localizedCopy = (Folder) testFolder.copy();
		localizedCopy.setChannelInfo(channelId, testFolder.getChannelSetId());
		localizedCopy.save();
		t.commit(false);

		// now delete the folder
		testFolder = t.getObject(Folder.class, testFolder.getId());
		testFolder.delete();
		t.commit(false);

		// check that the folder and the localized copy are gone
		assertNull("Deleted master folder must not exist", t.getObject(Folder.class, testFolder.getId()));
		assertNull("Localized copy of deleted master folder must not exist", t.getObject(Folder.class, localizedCopy.getId()));
	}

	/**
	 * Test deleting a master folder that contains local channel objects
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteMasterFolderWithLocalObjects() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();

		// create a template in the root folder
		Template template = t.createObject(Template.class);
		template.setFolderId(masterNodeFolder.getId());
		template.setMlId(1);
		template.setName("Template");
		template.setSource("dummy source");
		template.save();
		t.commit(false);

		// Create test folder
		Folder testFolder = t.createObject(Folder.class);
		testFolder.setName("Test Folder");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.save();
		t.commit(false);

		// create a master subfolder
		Folder masterFolder = t.createObject(Folder.class);
		masterFolder.setName("Master Folder");
		masterFolder.setMotherId(testFolder.getId());
		masterFolder.save();
		t.commit(false);

		// create a master template
		Template masterTemplate = t.createObject(Template.class);
		masterTemplate.setFolderId(testFolder.getId());
		masterTemplate.setMlId(1);
		masterTemplate.setName("Master Template");
		masterTemplate.setSource("dummy source");
		masterTemplate.save();
		t.commit(false);

		// create a master page
		Page masterPage = t.createObject(Page.class);
		masterPage.setFolderId(testFolder.getId());
		masterPage.setName("Master Page");
		masterPage.setTemplateId(template.getId());
		masterPage.save();
		masterPage.unlock();
		t.commit(false);

		// create a master file
		File masterFile = t.createObject(File.class);
		masterFile.setFileStream(new ByteArrayInputStream(new byte[] {1, 2, 3}));
		masterFile.setFolderId(testFolder.getId());
		masterFile.setName("Master-File.bin");
		masterFile.save();
		t.commit(false);

		// create a master image
		ImageFile masterImage = t.createObject(ImageFile.class);
		masterImage.setFileStream(getClass().getResourceAsStream("gentics.png"));
		masterImage.setFolderId(testFolder.getId());
		masterImage.setName("Master-Image.png");
		masterImage.save();
		t.commit(false);

		// create a local subfolder
		Folder localFolder = t.createObject(Folder.class);
		localFolder.setName("Local Folder");
		localFolder.setMotherId(testFolder.getId());
		localFolder.setChannelInfo(channelId, null);
		localFolder.save();
		t.commit(false);

		// create a local template
		Template localTemplate = t.createObject(Template.class);
		localTemplate.setFolderId(testFolder.getId());
		localTemplate.setMlId(1);
		localTemplate.setName("Local Template");
		localTemplate.setSource("dummy source");
		localTemplate.setChannelInfo(channelId, null);
		localTemplate.save();
		t.commit(false);

		// create a local page
		Page localPage = t.createObject(Page.class);
		localPage.setFolderId(testFolder.getId());
		localPage.setName("Local Page");
		localPage.setTemplateId(template.getId());
		localPage.setChannelInfo(channelId, null);
		localPage.save();
		localPage.unlock();
		t.commit(false);

		// create a local file
		File localFile = t.createObject(File.class);
		localFile.setFileStream(new ByteArrayInputStream(new byte[] {1, 2, 3}));
		localFile.setFolderId(testFolder.getId());
		localFile.setName("Local-File.bin");
		localFile.setChannelInfo(channelId, null);
		localFile.save();
		t.commit(false);

		// create a local image
		ImageFile localImage = t.createObject(ImageFile.class);
		localImage.setFileStream(getClass().getResourceAsStream("gentics.png"));
		localImage.setFolderId(testFolder.getId());
		localImage.setName("Local-Image.png");
		localImage.setChannelInfo(channelId, null);
		localImage.save();
		t.commit(false);

		// check whether all objects in the folder exist now
		t.setChannelId(channelId);
		for (NodeObject obj : Arrays.asList(masterFolder, masterTemplate, masterPage, masterFile, masterImage, localFolder, localTemplate, localPage,
				localFile, localImage)) {
			assertNotNull(obj + " must exist", t.getObject(obj.getObjectInfo().getObjectClass(), obj.getId()));
		}
		t.resetChannel();

		// now delete the folder
		testFolder.delete();
		t.commit(false);

		// check whether all objects in the folder were removed now
		t.setChannelId(channelId);
		for (NodeObject obj : Arrays.asList(masterFolder, masterTemplate, masterPage, masterFile, masterImage, localFolder, localTemplate, localPage,
				localFile, localImage)) {
			assertNull(obj + " must be deleted", t.getObject(obj.getObjectInfo().getObjectClass(), obj.getId()));
		}
		t.resetChannel();
	}
}
