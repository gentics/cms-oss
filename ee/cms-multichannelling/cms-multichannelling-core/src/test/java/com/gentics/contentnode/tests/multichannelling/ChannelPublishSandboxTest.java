package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishInfo;

/**
 * Publish Tests for multichannelling.
 * Master and Channel both publish into the filesystem.
 * Master publishes into [pubDir]/multichannelling/Content.Node/...
 * Channel publishes into [pubDir]/channel/Content.Node/...
 */
public class ChannelPublishSandboxTest extends AbstractMultichannellingSandboxTest {

	private void checkFilePathChannel(String path, boolean checkIfExist) throws IOException {
		StringBuilder builder = new StringBuilder("channel").append(java.io.File.separatorChar).append("Content.Node");
		
		checkFilePath(path, checkIfExist, builder.toString());
	}
	
	private void checkFilePathMaster(String path, boolean checkIfExist) throws IOException {
		StringBuilder builder = new StringBuilder("multichannelling").append(java.io.File.separatorChar).append("Content.Node");
		
		checkFilePath(path, checkIfExist, builder.toString());
	}
	
	private void checkFilePath(String path, boolean checkIfExist, String basePath) throws IOException {
		StringBuilder checkPath = new StringBuilder(testContext.getPubDir().getCanonicalPath()).append(java.io.File.separatorChar).append(basePath);

		if ((path.startsWith("/") || path.startsWith("\\")) && path.length() > 1) {
			path = path.substring(1);
		}
		checkPath.append(java.io.File.separatorChar).append(path);
		
		java.io.File checkFile = new java.io.File(checkPath.toString());

		if (checkIfExist) {
			assertTrue("Check if filepath exists: " + checkPath.toString(), checkFile.exists());
		} else {
			assertFalse("Check if filepath don't exists: " + checkPath.toString(), checkFile.exists());
		}
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// disable publishing for all other nodes
		DBUtils.executeUpdate("UPDATE node SET disable_publish = 1, publish_fs = 0, publish_contentmap = 0 WHERE id NOT IN (?, ?, ?)",
				new Object[] { MASTER_ID, channelId, subChannelId});
	}

	/**
	 * Test Publishing pages into channels
	 */
	@Test
	public void testPublishPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
    	
		// create test pages in master (and publish)
    	
		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();
        
		Folder testFolder = (Folder) t.createObject(Folder.class);

		testFolder.setName("testFolder");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		Page testPage1 = (Page) t.createObject(Page.class);

		testPage1.setDescription("Some description");
		testPage1.setFilename("publishTestPage1.html");
		testPage1.setFolderId(testFolder.getId());
		testPage1.setName("publishTestPage1");
		testPage1.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage1.save();
		testPage1.publish();
    	
		Page testPage2 = (Page) t.createObject(Page.class);

		testPage2.setDescription("Some description");
		testPage2.setFilename("publishTestPage2.html");
		testPage2.setFolderId(testFolder.getId());
		testPage2.setName("publishTestPage2");
		testPage2.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage2.save();
		testPage2.publish();
    	
		Page testPage3 = (Page) t.createObject(Page.class);

		testPage3.setDescription("Some description");
		testPage3.setFilename("publishTestPage3.html");
		testPage3.setFolderId(testFolder.getId());
		testPage3.setName("publishTestPage3");
		testPage3.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage3.save();
		testPage3.publish();
		t.commit(false);

		// run the publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(true).getReturnCode());

		// check in the filesystem whether pages were correctly published into the master and channel
		checkFilePathMaster(testPage1.getFilename(), true);
		checkFilePathMaster(testPage2.getFilename(), true);
		checkFilePathMaster(testPage3.getFilename(), true);

		checkFilePathChannel(testPage1.getFilename(), true);
		checkFilePathChannel(testPage2.getFilename(), true);
		checkFilePathChannel(testPage3.getFilename(), true);

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// localize one test page in channel (and publish)
		Page localizedTestPage2 = (Page) testPage2.copy();

		localizedTestPage2.setChannelInfo(channelId, testPage2.getChannelSetId());
		localizedTestPage2.setFilename("publishTestPage2-localized.html");
		localizedTestPage2.setName("publishTestPage2-localized");
		localizedTestPage2.save();
		localizedTestPage2.publish();
    	
		Page localizedTestPage3 = (Page) testPage3.copy();

		localizedTestPage3.setChannelInfo(channelId, testPage3.getChannelSetId());
		localizedTestPage3.setFilename("localizedTestPage3-localized.html");
		localizedTestPage3.setName("localizedTestPage3-localized");
		localizedTestPage3.save();
		localizedTestPage3.takeOffline();
		t.commit(false);
    	
		// create local test page in channel (and publish)
		Page testPage4 = t.createObject(Page.class);

		testPage4.setDescription("Some description");
		testPage4.setFilename("publishTestPage4.html");
		testPage4.setFolderId(testFolder.getId());
		testPage4.setName("publishTestPage4");
		testPage4.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage4.setChannelInfo(channelId, testPage4.getChannelSetId());
		testPage4.save();
		testPage4.publish();
		t.commit(false);

		// run the publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(true).getReturnCode());

		// check in the filesystem whether pages were correctly published into the master and channel
		checkFilePathMaster(testPage1.getFilename(), true);
		checkFilePathMaster(testPage2.getFilename(), true);
		checkFilePathMaster(testPage3.getFilename(), true);
		checkFilePathMaster(localizedTestPage3.getFilename(), false);
		checkFilePathMaster(localizedTestPage2.getFilename(), false);
		checkFilePathMaster(testPage4.getFilename(), false);

		checkFilePathChannel(testPage1.getFilename(), true);
		checkFilePathChannel(testPage2.getFilename(), false);
		checkFilePathChannel(testPage3.getFilename(), false);
		checkFilePathChannel(localizedTestPage3.getFilename(), false);
		checkFilePathChannel(localizedTestPage2.getFilename(), true);
		checkFilePathChannel(testPage4.getFilename(), true);
	}
    
	/**
	 * Test Publishing folders into channels
	 */
	@Test
	public void testPublishFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
    	
		// create test pages in master (and publish)
    	
		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();
        
		// create two test folders in the root folder of master node
		Folder publishTestFolder1 = t.createObject(Folder.class);

		publishTestFolder1.setDescription("Some description");
		publishTestFolder1.setName("publishTestFolder1");
		publishTestFolder1.setMotherId(masterNodeFolder.getId());
		publishTestFolder1.setPublishDir("/publishTestFolder1");
		publishTestFolder1.save();
        
		Folder publishTestFolder2 = t.createObject(Folder.class);

		publishTestFolder2.setDescription("Some description");
		publishTestFolder2.setName("publishTestFolder2");
		publishTestFolder2.setMotherId(masterNodeFolder.getId());
		publishTestFolder2.setPublishDir("/publishTestFolder2");
		publishTestFolder2.save();
        
		t.commit(false);
        
		// create 4 test pages in the test folder
		Page testPage1 = (Page) t.createObject(Page.class);

		testPage1.setDescription("Some description");
		testPage1.setFilename("publishTestPage1.html");
		testPage1.setFolderId(publishTestFolder1.getId());
		testPage1.setName("publishTestPage1");
		testPage1.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage1.save();
		testPage1.publish();
    	
		Page testPage2 = (Page) t.createObject(Page.class);

		testPage2.setDescription("Some description");
		testPage2.setFilename("publishTestPage2.html");
		testPage2.setFolderId(publishTestFolder1.getId());
		testPage2.setName("publishTestPage2");
		testPage2.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage2.save();
		testPage2.publish();
    	
		Page testPage3 = (Page) t.createObject(Page.class);

		testPage3.setDescription("Some description");
		testPage3.setFilename("publishTestPage3.html");
		testPage3.setFolderId(publishTestFolder2.getId());
		testPage3.setName("publishTestPage3");
		testPage3.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage3.save();
		testPage3.publish();
		t.commit(false);
    	
		Page testPage4 = (Page) t.createObject(Page.class);

		testPage4.setDescription("Some description");
		testPage4.setFilename("publishTestPage4.html");
		testPage4.setFolderId(publishTestFolder2.getId());
		testPage4.setName("publishTestPage4");
		testPage4.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage4.save();
		testPage4.publish();
		t.commit(false);
    	
		// localize two test page in channel (and publish) + one of the test folders
		Page localizedTestPage2 = (Page) testPage2.copy();

		localizedTestPage2.setChannelInfo(channelId, testPage2.getChannelSetId());
		localizedTestPage2.setFilename("publishTestPage2-localized.html");
		localizedTestPage2.setName("publishTestPage2-localized");
		localizedTestPage2.save();
		localizedTestPage2.publish();
    	
		Page localizedTestPage3 = (Page) testPage3.copy();

		localizedTestPage3.setChannelInfo(channelId, testPage3.getChannelSetId());
		localizedTestPage3.setFilename("localizedTestPage3-localized.html");
		localizedTestPage3.setName("localizedTestPage3-localized");
		localizedTestPage3.save();
		localizedTestPage3.publish();
    	
		Folder localizedPublishTestFolder1 = (Folder) publishTestFolder1.copy();

		localizedPublishTestFolder1.setName("publishTestFolder1-localized");
		localizedPublishTestFolder1.setPublishDir("/publishTestFolder1-localized");
		localizedPublishTestFolder1.setMotherId(masterNodeFolder.getId());
		localizedPublishTestFolder1.setChannelInfo(channelId, publishTestFolder1.getChannelSetId());
		localizedPublishTestFolder1.save();
		t.commit(false);
    	
		// create local test folder in channel
		Folder publishTestFolder3 = t.createObject(Folder.class);

		publishTestFolder3.setDescription("Some description");
		publishTestFolder3.setName("publishTestFolder3");
		publishTestFolder3.setMotherId(masterNodeFolder.getId());
		publishTestFolder3.setPublishDir("/publishTestFolder3");
		publishTestFolder3.setChannelInfo(channelId, publishTestFolder3.getChannelSetId());
		publishTestFolder3.save();
		t.commit(false);
    	
		Page testPage5 = (Page) t.createObject(Page.class);

		testPage5.setDescription("Some description");
		testPage5.setFilename("testPage5.html");
		testPage5.setFolderId(publishTestFolder3.getId());
		testPage5.setName("testPage5");
		testPage5.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage5.setChannelInfo(channelId, testPage5.getChannelSetId());
		testPage5.save();
		testPage5.publish();
		t.commit(false);

		// run the publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(true).getReturnCode());

		// check in the filesystem whether pages were correctly published into the master and channel
		String baseFolder1 = "publishTestFolder1" + java.io.File.separator;
		String localizedBaseFolder1 = "publishTestFolder1-localized" + java.io.File.separator;
		String baseFolder2 = "publishTestFolder2" + java.io.File.separator;

		// --- check for master ---

		// we should use the master base folder 1
		checkFilePathMaster(baseFolder1, true);
        
		// not use channel base folder 1
		checkFilePathMaster(localizedBaseFolder1, false);
        
		// check if test page 1 and 2 exist in the base folder 1
		checkFilePathMaster(baseFolder1 + testPage1.getFilename(), true);
		checkFilePathMaster(baseFolder1 + testPage2.getFilename(), true);
        
		// check if localized test page 2 don't exist in the master
		checkFilePathMaster(baseFolder1 + localizedTestPage2.getFilename(), false);

		// check if test page 3 and 4 exist in the base folder 2
		checkFilePathMaster(baseFolder2 + testPage3.getFilename(), true);
		checkFilePathMaster(baseFolder2 + testPage4.getFilename(), true);
        
		// check if localized test page 3 don't exist in the base folder 2
		checkFilePathMaster(baseFolder2 + localizedTestPage3.getFilename(), false);
        
		// the test folder 3 was created in the channel directly so it shouldn't be in master
		checkFilePathMaster(publishTestFolder3.getPublishDir(), false);

		// --- check for channel ---
        
		// we shouldn't use base folder 1 in the channel
		checkFilePathChannel(baseFolder1, false);
        
		// we should have localized base folder 1 in the channel
		checkFilePathChannel(localizedBaseFolder1, true);
        
		// we should have test page 1 and localized test page 2 in the localized base folder 1
		checkFilePathChannel(localizedBaseFolder1 + testPage1.getFilename(), true);
		checkFilePathChannel(localizedBaseFolder1 + localizedTestPage2.getFilename(), true);
        
		// we shouldn't have test page 2 in the localized base folder 1 in the channel node
		checkFilePathChannel(localizedBaseFolder1 + testPage2.getFilename(), false);
        
		// we should have localized test page 3 and test page 4 in the base folder 2 in the channel
		checkFilePathChannel(baseFolder2 + localizedTestPage3.getFilename(), true);
		checkFilePathChannel(baseFolder2 + testPage4.getFilename(), true);
        
		// we shouldn't have test page 3 in the base folder 2 in the channel node
		checkFilePathChannel(baseFolder2 + testPage3.getFilename(), false);
        
		// the test folder 3 should be present in the channel node
		checkFilePathChannel(publishTestFolder3.getPublishDir(), true);
	}
    
	/**
	 * Test Publishing images into channels
	 */
	@Test
	public void testPublishImage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
    	
		// create test images in master
    	
		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();
        
		Folder testFolder = (Folder) t.createObject(Folder.class);

		testFolder.setName("testFolder");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		ImageFile testImage1 = (ImageFile) t.createObject(ImageFile.class);

		testImage1.setDescription("Some description");
		testImage1.setFolderId(testFolder.getId());
		testImage1.setFileStream(new ByteArrayInputStream(C.Resource.getImagePng()));
		testImage1.setName("publishTestImage1.png");
		testImage1.save();
    	
		ImageFile testImage2 = (ImageFile) t.createObject(ImageFile.class);

		testImage2.setDescription("Some description");
		testImage2.setFolderId(testFolder.getId());
		testImage2.setFileStream(new ByteArrayInputStream(C.Resource.getImagePng()));
		testImage2.setName("publishTestImage2.png");
		testImage2.save();
    	
		ImageFile testImage3 = (ImageFile) t.createObject(ImageFile.class);

		testImage3.setDescription("Some description");
		testImage3.setFolderId(testFolder.getId());
		testImage3.setFileStream(new ByteArrayInputStream(C.Resource.getImagePng()));
		testImage3.setName("publishTestImage3.png");
		testImage3.save();
		t.commit(false);

		// localize one test image in channel
		ImageFile localizedTestImage2 = (ImageFile) testImage2.copy();

		localizedTestImage2.setChannelInfo(channelId, testImage2.getChannelSetId());
		localizedTestImage2.setName("publishTestImage2-localized.png");
		localizedTestImage2.save();
    	
		// create local test image in channel
		ImageFile testImage4 = t.createObject(ImageFile.class);

		testImage4.setDescription("Some description");
		testImage4.setFolderId(testFolder.getId());
		testImage4.setFileStream(new ByteArrayInputStream(C.Resource.getImagePng()));
		testImage4.setName("publishTestImage4.png");
		testImage4.setChannelInfo(channelId, testImage4.getChannelSetId());
		testImage4.save();
		t.commit(false);

		System.out.println(masterNodeFolder.getPublishDir());

		// run the publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(true).getReturnCode());

		// check in the filesystem whether images were correctly published into the master and channel
		checkFilePathMaster(testImage1.getName(), true);
		checkFilePathMaster(testImage2.getName(), true);
		checkFilePathMaster(testImage3.getName(), true);
		checkFilePathMaster(localizedTestImage2.getName(), false);
		checkFilePathMaster(testImage4.getName(), false);

		checkFilePathChannel(testImage1.getName(), true);
		checkFilePathChannel(testImage2.getName(), false);
		checkFilePathChannel(testImage3.getName(), true);
		checkFilePathChannel(localizedTestImage2.getName(), true);
		checkFilePathChannel(testImage4.getName(), true);
	}

	/**
	 * Test Publishing files into channels
	 */
	@Test
	public void testPublishFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
    	
		// create test files in master
    	
		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();
        
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("testFolderFiles");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		File testFile1 = t.createObject(File.class);

		testFile1.setDescription("Some description");
		testFile1.setFolderId(testFolder.getId());
		testFile1.setFileStream(new ByteArrayInputStream(C.Resource.getImagePng()));
		testFile1.setName("publishTestFile1.bin");
		testFile1.save();
    	
		File testFile2 = t.createObject(File.class);

		testFile2.setDescription("Some description");
		testFile2.setFolderId(testFolder.getId());
		testFile2.setFileStream(new ByteArrayInputStream(C.Resource.getImagePng()));
		testFile2.setName("publishTestFile2.bin");
		testFile2.save();
    	
		File testFile3 = t.createObject(File.class);

		testFile3.setDescription("Some description");
		testFile3.setFolderId(testFolder.getId());
		testFile3.setFileStream(new ByteArrayInputStream(C.Resource.getImagePng()));
		testFile3.setName("publishTestFile3.bin");
		testFile3.save();
		t.commit(false);

		// localize one test file in channel
		File localizedTestFile2 = (File) testFile2.copy();

		localizedTestFile2.setChannelInfo(channelId, testFile2.getChannelSetId());
		localizedTestFile2.setName("publishTestFile2-localized.bin");
		localizedTestFile2.save();
    	
		// create local test file in channel
		File testFile4 = t.createObject(File.class);

		testFile4.setDescription("Some description");
		testFile4.setFolderId(testFolder.getId());
		testFile4.setFileStream(new ByteArrayInputStream(C.Resource.getImagePng()));
		testFile4.setName("publishTestFile4.bin");
		testFile4.setChannelInfo(channelId, testFile4.getChannelSetId());
		testFile4.save();
		t.commit(false);

		System.out.println(masterNodeFolder.getPublishDir());

		// run the publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(true).getReturnCode());

		// check in the filesystem whether files were correctly published into the master and channel
		checkFilePathMaster(testFile1.getName(), true);
		checkFilePathMaster(testFile2.getName(), true);
		checkFilePathMaster(testFile3.getName(), true);
		checkFilePathMaster(localizedTestFile2.getName(), false);
		checkFilePathMaster(testFile4.getName(), false);

		checkFilePathChannel(testFile1.getName(), true);
		checkFilePathChannel(testFile2.getName(), false);
		checkFilePathChannel(testFile3.getName(), true);
		checkFilePathChannel(localizedTestFile2.getName(), true);
		checkFilePathChannel(testFile4.getName(), true);
	}

	/**
	 * Test for RT#57813, where a page in a channel would not publish to the FS when the master has updates disabled or is not publishing to FS
	 * @throws Exception 
	 */
	@Test
	public void testDisabledMasterPublish() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Disable master node publishing
		DBUtils.executeUpdate("UPDATE node SET disable_publish = 1, publish_fs = 0, publish_contentmap = 0 WHERE id = ?",
				new Object[] { MASTER_ID});

		t.commit(false);

		// create test pages in master (and publish)
    	
		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();
        
		Folder testFolder = (Folder) t.createObject(Folder.class);

		testFolder.setName("testFolder");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		Page testPage1 = (Page) t.createObject(Page.class);

		testPage1.setDescription("Some description");
		testPage1.setFilename("fsPublishTest.html");
		testPage1.setFolderId(testFolder.getId());
		testPage1.setName("publishTestPage1");
		testPage1.setTemplateId(C.page_channel_test.new_page_template_id);
		testPage1.save();
		testPage1.publish();

		t.commit(false);

		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(true).getReturnCode());
		
		checkFilePathChannel(testPage1.getFilename(), true);
	}
}
