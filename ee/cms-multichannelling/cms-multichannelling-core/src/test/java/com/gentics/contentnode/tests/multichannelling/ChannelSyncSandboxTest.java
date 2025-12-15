package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCFolderResourceImpl;
import com.gentics.contentnode.tests.utils.OverviewHelper;

public class ChannelSyncSandboxTest extends AbstractMultichannellingSandboxTest {

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

	@Test
	public void testPushToMasterLocalizedPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);

		// create a new page over the REST API
		com.gentics.contentnode.rest.model.Page testRestPage = OverviewHelper.createPage(C.overview_test_data.folder_id,
				C.overview_test_data.templates.template1_id);

		// create a list with IDs for items to select
		List<Integer> selectedItemsIds = new ArrayList<Integer>(Arrays.asList(C.overview_test_data.pages.page2_id, C.overview_test_data.testFolder2.pages.page5_id,
				C.overview_test_data.pages.page1_id, C.overview_test_data.testFolder2.pages.page4_id));

		// Get the page from the object model for editing
		Page testPage = t.getObject(Page.class, testRestPage.getId(), true);

		testPage.setName("syncTestPage001");

		// Create a content tag for the test page
		ContentTag contentTag = testPage.getContent().addContentTag(3);
		contentTag.setName("textshort1");
		contentTag.setEnabled(true);
		contentTag.getValues().iterator().next().setValueText("masterContentTag");

		// Get the overview
		com.gentics.contentnode.object.Overview overview = OverviewHelper.extractOverviewFromPage(testPage, "ds", "ds");

		//Change all attributes of the overview
		overview.setObjectClass(Page.class);
		overview.setOrderKind(com.gentics.contentnode.object.Overview.ORDER_CDATE);
		overview.setOrderWay(com.gentics.contentnode.object.Overview.ORDERWAY_ASC);
		overview.setSelectionType(com.gentics.contentnode.object.Overview.SELECTIONTYPE_SINGLE);

		OverviewHelper.addOverviewEntriesToOverview(overview, selectedItemsIds);

		testPage.save();
		t.commit(false);

		// Localize the test page in the channel
		com.gentics.contentnode.rest.model.Page localizedRestTestpage = OverviewHelper.localizeAndPublishOverviewPage(channelId, testRestPage.getId(), true);

		// Get the page from the object model for editing and change the name
		Page localizedTestPage = t.getObject(Page.class, localizedRestTestpage.getId(), true);

		// Change localized copy name
		localizedTestPage.setName("syncLocalizedTestPage001");
		localizedTestPage.getContentTag("textshort1").getValues().iterator().next().setValueText("localizedContentTag");

		//get the Overview from the localized page and change the attributes
		com.gentics.contentnode.object.Overview localizedOverview = OverviewHelper.extractOverviewFromPage(localizedTestPage, "ds", "ds");

		//Change all attributes of the overview
		localizedOverview.setObjectClass(Page.class);
		localizedOverview.setOrderKind(com.gentics.contentnode.object.Overview.ORDER_NAME);
		localizedOverview.setOrderWay(com.gentics.contentnode.object.Overview.ORDERWAY_DESC);
		localizedOverview.setSelectionType(com.gentics.contentnode.object.Overview.SELECTIONTYPE_PARENT);

		//delete all overview entries and add new entries
		selectedItemsIds.remove((Integer)C.overview_test_data.pages.page1_id);
		List<OverviewEntry> overviewEntries = localizedOverview.getOverviewEntries();
		overviewEntries.clear();
		OverviewHelper.addOverviewEntriesToOverview(localizedOverview, selectedItemsIds);

		localizedTestPage.save();
		t.commit(false);

		// Push localized version to master node
		localizedTestPage.pushToMaster(masterNode);
		t.commit(false);

		// Retrieve the latest versions of our page from master and channel nodes
		Page masterTestPage = (Page) t.getObject(Page.class, testPage.getId());

		//extract the overview and see if the attributes are set the same as in the previously localized page
		com.gentics.contentnode.object.Overview masterOverview = OverviewHelper.extractOverviewFromPage(masterTestPage, "ds", "ds");

		assertEquals("Check if the objectClass is the same as in the localized page",masterOverview.getObjectClass(), localizedOverview.getObjectClass());
		assertEquals("Check if the orderKind is the same as in the localized page",masterOverview.getOrderKind(), localizedOverview.getOrderKind());
		assertEquals("Check if the orderWay is the same as in the localized page",masterOverview.getOrderWay(), localizedOverview.getOrderWay());
		assertEquals("Check if the selectionType is the same as in the localized page",masterOverview.getSelectionType(), localizedOverview.getSelectionType());
		
		List<OverviewEntry> masterOverviewEntries = masterOverview.getOverviewEntries();
		Set<Integer> idsMaster = new HashSet<Integer>();
		for(OverviewEntry currentEntry : masterOverviewEntries){
			idsMaster.add(currentEntry.getObjectId());
		}
		Set<Integer> idsPrevious = new HashSet<Integer>();
		idsPrevious.addAll(selectedItemsIds);

		assertEquals("Check if the same items are selected as in the localized page",idsMaster,idsPrevious);


		// Get the content tag from the master version of the page
		contentTag = masterTestPage.getContentTag("textshort1");

		// Check if the behavior is as expected
		assertNotNull("Check if the tag exists", contentTag);

		assertEquals("Check if the content is like in localizedPage", "localizedContentTag", contentTag.getValues().iterator().next().getValueText());

		assertEquals("Check if the name has changed", "syncLocalizedTestPage001", masterTestPage.getName());

		//the localized Page should not exist
		Page localizedChannelPage = (Page) t.getObject(Page.class, localizedTestPage.getId());
		assertNull("Localized copy should not exist now", localizedChannelPage);
	}
	
	@Test
	public void testPushToMasterLocalPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder in the master node
		Folder testFolder = t.createObject(Folder.class);
		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Localize the test page in the channel
		Page localTestPage = t.createObject(Page.class);
		localTestPage.setName("LocalChannelSyncTest");
		localTestPage.setTemplateId(C.reference_test_data.template1_id);
		localTestPage.setFolderId(testFolder.getId());
		localTestPage.setChannelInfo(channelId, localTestPage.getChannelSetId());
		localTestPage.save();
		localTestPage.publish();
		t.commit(false);
		
		// Run publish process and check if the local page is only present in the channel node
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);

		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(localTestPage.getFilename(), false);
		checkFilePathChannel(localTestPage.getFilename(), true);
		
		// Push localized version to master node
		localTestPage.pushToMaster(masterNode);
		t.commit(false);
		
		// Start publish process again and check if the page is in both master and channel nodes
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);

		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(localTestPage.getFilename(), true);
		checkFilePathChannel(localTestPage.getFilename(), true);
		
		// Check if the page is now inherited in channel node
		t.setChannelId(channelId);
		
		Page localizedChannelPage = t.getObject(Page.class, localTestPage.getId());

		assertNotNull("Channel page copy should exist now", localizedChannelPage);
		assertTrue("The channel page should be inherited after the push to master", localizedChannelPage.isInherited());
		
		t.resetChannel();
	}

	@Test
	public void testPushToMasterPublishPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create test page in the test folder ( do not publish )
		Page testPage = t.createObject(Page.class);

		testPage.setName("ChannelSyncTestPage");
		testPage.setTemplateId(C.reference_test_data.template1_id);
		testPage.setFolderId(testFolder.getId());
		testPage.save();
		t.commit(false);
		
		// Localize the test page in the channel ( publish )
		Page localizedTestPage = (Page) testPage.copy();

		localizedTestPage.setChannelInfo(channelId, testPage.getChannelSetId());
		localizedTestPage.save();
		localizedTestPage.publish();
		t.commit(false);
		
		// Run publish process and check if the local page is only present in the channel node
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testPage.getFilename(), false);
		checkFilePathChannel(testPage.getFilename(), true);
		
		// Push localized version to master node
		localizedTestPage = localizedTestPage.reload();
		localizedTestPage.pushToMaster(masterNode);
		t.commit(false);
		
		// Start publish process again and check if the page is in both master and channel nodes
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testPage.getFilename(), true);
		checkFilePathChannel(testPage.getFilename(), true);
	}
	
	@Test
	public void testPushToMasterOfflinePage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);
		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Localize the test page in the channel
		Page testPage = t.createObject(Page.class);
		testPage.setName("ChannelSyncTestPage");
		testPage.setTemplateId(C.reference_test_data.template1_id);
		testPage.setFolderId(testFolder.getId());
		testPage.save();
		testPage.publish();
		t.commit(false);
		
		// Localize the test page in the channel ( take offline )
		Page localizedTestPage = (Page) testPage.copy();
		localizedTestPage.setChannelInfo(channelId, testPage.getChannelSetId());
		localizedTestPage.save();
		localizedTestPage.takeOffline();
		t.commit(false);
		
		// Run publish process and check if the local page is only present in the channel node
		testContext.getContext().startTransaction();
		PublishInfo publishInfo = testContext.getContext().publish(false);

		assertEquals("Check if the publish process works", PublishInfo.RETURN_CODE_SUCCESS, publishInfo.getReturnCode());
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testPage.getFilename(), true);
		checkFilePathChannel(localizedTestPage.getFilename(), false);
		
		// Push local version to master node
		localizedTestPage.pushToMaster(masterNode);
		t.commit(false);
		
		// Start publish process again and check if the page is NOT published in both master and channel nodes
		testContext.getContext().startTransaction();
		publishInfo = testContext.getContext().publish(false);
		assertEquals("Check if the publish process works", PublishInfo.RETURN_CODE_SUCCESS, publishInfo.getReturnCode());
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testPage.getFilename(), false);
		checkFilePathChannel(testPage.getFilename(), false);
	}
	
	@Test
	public void testPushToMasterLocalizedImage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		 
		// Create test image
		ImageFile testImage = t.createObject(ImageFile.class);

		testImage.setName(C.channel_sync.image_name_jpg);
		testImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		testImage.setFolderId(testFolder.getId());
		testImage.save();
		
		// Localize the test image in the channel
		ImageFile localizedTestImage = (ImageFile) testImage.copy();

		localizedTestImage.setChannelInfo(channelId, testImage.getChannelSetId());
		localizedTestImage.setName(C.channel_sync.image_name_png);
		localizedTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_png));
		localizedTestImage.save();
		t.commit(false);
		
		// Push localized version to master node
		localizedTestImage.pushToMaster(masterNode);
		t.commit(false);

		// Retrieve the latest versions of our image from master and channel nodes
		ImageFile masterTestImage = (ImageFile) t.getObject(ImageFile.class, testImage.getId());
		ImageFile localizedChannelImage = (ImageFile) t.getObject(ImageFile.class, localizedTestImage.getId());

		// Check if the behaviour is as expected
		assertNotNull("Master copy should exist", masterTestImage);
		assertNull("Localized copy should not exist now", localizedChannelImage);
		
		assertEquals("Check the name of the master copy", C.channel_sync.image_name_png, masterTestImage.getName());
		
		assertEquals("Check if the filesize has changed", localizedTestImage.getFilesize(), masterTestImage.getFilesize());
		
		assertEquals("Check the filetype is changed", "image/png", masterTestImage.getFiletype());
	}
	
	@Test
	public void testPushToMasterLocalImage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Localize the test image in the channel
		ImageFile testImage = t.createObject(ImageFile.class);

		testImage.setName(C.channel_sync.image_name_jpg);
		testImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		testImage.setFolderId(testFolder.getId());
		testImage.setChannelInfo(channelId, testImage.getChannelSetId());
		testImage.save();
		t.commit(false);
    	
		// Run publish process and check if the local image is only present in the channel node
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testImage.getName(), false);
		checkFilePathChannel(testImage.getName(), true);
		
		// Push localized version to master node
		testImage.pushToMaster(masterNode);
		t.commit(false);
		
		// Start publish process again and check if the image is published in both master and channel nodes
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testImage.getName(), true);
		checkFilePathChannel(testImage.getName(), true);
		
		// Check if the image is now inherited in channel node
		t.setChannelId(channelId);
		
		ImageFile localizedChannelImage = t.getObject(ImageFile.class, testImage.getId());

		assertNotNull("Channel image copy should exist now", localizedChannelImage);
		assertTrue("The channel image should be inherited after the push to master", localizedChannelImage.isInherited());
		
		t.resetChannel();

	}
	
	@Test
	public void testPushToMasterPublishImage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create test image in the test folder
		ImageFile testImage = t.createObject(ImageFile.class);

		testImage.setName("asyncImage1.jpg");
		testImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		testImage.setFolderId(testFolder.getId());
		testImage.save();
		t.commit(false);
		
		// Localize the test image in the channel
		ImageFile localizedTestImage = (ImageFile) testImage.copy();

		localizedTestImage.setName("asyncLocalizedImage1.png");
		localizedTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_png));
		localizedTestImage.setChannelInfo(channelId, testImage.getChannelSetId());
		localizedTestImage.save();
		t.commit(false);
		
		// Run publish process and check if the local image is only present in the channel node
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testImage.getName(), true);
		checkFilePathMaster(localizedTestImage.getName(), false);
		checkFilePathChannel(localizedTestImage.getName(), true);
		checkFilePathChannel(testImage.getName(), false);
		
		// Push localized version to master node
		localizedTestImage.pushToMaster(masterNode);
		t.commit(false);
		
		// Start publish process again and check if the image is published in master node and channel node 
		// and the localized version is not gone and not published anywhere
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		// Retrieve the latest versions of our image from master node
		ImageFile newTestImage = (ImageFile) t.getObject(ImageFile.class, testImage.getId());
		
		checkFilePathMaster(newTestImage.getName(), true);
		checkFilePathMaster(testImage.getName(), false);
		checkFilePathChannel(newTestImage.getName(), true);
		checkFilePathChannel(testImage.getName(), false);
	}
	
	@Test
	public void testPushToMasterLocalizedFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		 
		// Create test file
		File testFile = t.createObject(File.class);

		testFile.setName("asyncFile.bin");
		testFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		testFile.setFolderId(testFolder.getId());
		testFile.save();
		
		// Localize the test file in the channel
		File localizedTestFile = (File) testFile.copy();

		localizedTestFile.setChannelInfo(channelId, testFile.getChannelSetId());
		localizedTestFile.setName("asyncLocalizedFile.bin");
		localizedTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4}));
		localizedTestFile.save();
		t.commit(false);
		
		// Push localized version to master node
		localizedTestFile.pushToMaster(masterNode);
		t.commit(false);

		// Retrieve the latest versions of our file from master and channel nodes
		File masterTestFile = (File) t.getObject(File.class, testFile.getId());
		File localizedChannelFile = (File) t.getObject(File.class, localizedTestFile.getId());

		// Check if the behaviour is as expected
		assertNotNull("Master copy should exist", masterTestFile);
		assertNull("Localized copy should not exist now", localizedChannelFile);
		
		assertEquals("Check the name of the master copy", "asyncLocalizedFile.bin", masterTestFile.getName());
		
		assertEquals("Check if the filesize has changed", localizedTestFile.getFilesize(), masterTestFile.getFilesize());
	}
	
	@Test
	public void testPushToMasterPublishFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create test file in the test folder
		File testFile = t.createObject(File.class);

		testFile.setName("syncTestFile1.bin");
		testFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		testFile.setFolderId(testFolder.getId());
		testFile.save();
		t.commit(false);
		
		// Localize the test file in the channel
		File localizedTestFile = (File) testFile.copy();

		localizedTestFile.setName("syncTestFile2.bin");
		localizedTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4}));
		localizedTestFile.setChannelInfo(channelId, testFile.getChannelSetId());
		localizedTestFile.save();
		t.commit(false);
		
		// Run publish process and check if the local file is only present in the channel node
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testFile.getName(), true);
		checkFilePathMaster(localizedTestFile.getName(), false);
		checkFilePathChannel(localizedTestFile.getName(), true);
		checkFilePathChannel(testFile.getName(), false);
		
		// Push localized version to master node
		localizedTestFile.pushToMaster(masterNode);
		t.commit(false);
		
		// Start publish process again and check where our file is published
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		// Retrieve the latest versions of our file from master and channel nodes
		File newTestFile = (File) t.getObject(File.class, testFile.getId());
		
		checkFilePathMaster(newTestFile.getName(), true);
		checkFilePathMaster(testFile.getName(), false);
		checkFilePathChannel(newTestFile.getName(), true);
		checkFilePathChannel(testFile.getName(), false);
	}
	
	@Test
	public void testPushToMasterLocalFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Localize the test file in the channel
		File testFile = t.createObject(File.class);

		testFile.setName("asyncTestFile.bin");
		testFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		testFile.setFolderId(testFolder.getId());
		testFile.setChannelInfo(channelId, testFile.getChannelSetId());
		testFile.save();
		t.commit(false);
    	
		// Run publish process and check if the local file is only present in the channel node
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testFile.getName(), false);
		checkFilePathChannel(testFile.getName(), true);
		
		// Push localized version to master node
		testFile.pushToMaster(masterNode);
		t.commit(false);
		
		// Start publish process again and check if the file is in both master and channel nodes
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(testFile.getName(), true);
		checkFilePathChannel(testFile.getName(), true);
		
		// Check if the page is now inherited in channel node
		t.setChannelId(channelId);
		
		File localizedChannelFile = t.getObject(File.class, testFile.getId());

		assertNotNull("Channel file copy should exist now", localizedChannelFile);
		assertTrue("The channel file should be inherited after the push to master", localizedChannelFile.isInherited());
		
		t.resetChannel();

	}

	@Test
	public void testPushToMasterLocalizedFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		 
		// Create new folder
		Folder syncTestFolder = t.createObject(Folder.class);

		syncTestFolder.setName("syncFolder1");
		syncTestFolder.setMotherId(testFolder.getId());
		syncTestFolder.save();
		
		// Localize the test folder in the channel
		Folder syncLocalizedTestFolder = (Folder) syncTestFolder.copy();

		syncLocalizedTestFolder.setChannelInfo(channelId, syncTestFolder.getChannelSetId());
		syncLocalizedTestFolder.setName("syncFolder2");
		syncLocalizedTestFolder.save();
		t.commit(false);
		
		// Push localized version to master node
		syncLocalizedTestFolder.pushToMaster(masterNode);
		t.commit(false);

		// Retrieve the latest versions of our folder from master and channel nodes
		Folder syncMasterTestFolder = (Folder) t.getObject(Folder.class, syncTestFolder.getId());
		Folder syncLocalizedChannelFolder = (Folder) t.getObject(Folder.class, syncLocalizedTestFolder.getId());

		// Check if the behaviour is as expected
		assertNotNull("Master copy should exist", syncMasterTestFolder);
		
		assertNull("Localized copy should not exist now", syncLocalizedChannelFolder);
		
		assertEquals("Check the name of the master copy", "syncFolder2", syncMasterTestFolder.getName());
	}

	@Test
	public void testPushToMasterLocalFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create a local folder in the channel and add page so it could be published
		Folder syncTestFolder = t.createObject(Folder.class);

		syncTestFolder.setName("syncTestFolder1");
		syncTestFolder.setMotherId(testFolder.getId());
		syncTestFolder.setPublishDir("/syncTestFolder");
		syncTestFolder.setChannelInfo(channelId, syncTestFolder.getChannelSetId());
		syncTestFolder.save();
		t.commit(false);
    	
		Page tmpPage = t.createObject(Page.class);

		tmpPage.setFolderId(syncTestFolder.getId());
		tmpPage.setName("tmpPage");
		tmpPage.setFilename("tmpPage.html");
		tmpPage.setTemplateId(C.page_channel_test.new_page_template_id);
		tmpPage.setChannelInfo(channelId, tmpPage.getChannelSetId());
		tmpPage.save();
		tmpPage.publish();
		t.commit(false);
    	
		// Run publish process and check if the local folder is only present in the channel node
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(syncTestFolder.getPublishDir(), false);
		checkFilePathChannel(syncTestFolder.getPublishDir(), true);
		
		// Push localized version to master node
		syncTestFolder.pushToMaster(masterNode);
		tmpPage.pushToMaster(masterNode);
		t.commit(false);
		
		// Start publish process again and check if the folder is in both master and channel nodes
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(syncTestFolder.getPublishDir(), true);
		checkFilePathChannel(syncTestFolder.getPublishDir(), true);
		
		// Check if the folder is now inherited in channel node
		t.setChannelId(channelId);
		
		Folder localizedChannelPage = t.getObject(Folder.class, syncTestFolder.getId());

		assertNotNull("Channel folder copy should exist now", localizedChannelPage);
		assertTrue("The channel folder should be inherited after the push to master", localizedChannelPage.isInherited());
		
		t.resetChannel();
	}

	@Test
	public void testPushToMasterLocalFolderInLocalizedFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// create local folder in the root folder of the channel
		Folder syncTestFolder = t.createObject(Folder.class);

		syncTestFolder.setName("syncTestFolder1");
		syncTestFolder.setMotherId(masterNodeRootFolder.getId());
		syncTestFolder.setPublishDir("/syncTestFolder");
		syncTestFolder.setChannelInfo(channelId, syncTestFolder.getChannelSetId());
		syncTestFolder.save();
		t.commit(false);

		// get subfolders of master root folder (must be empty)
		List<Folder> childFolders = masterNodeRootFolder.getChildFolders();

		assertFalse("Root folder must not see local subfolder", childFolders.contains(syncTestFolder));

		// we set the channel id here, because that is, what the Push2MasterJob does
		t.setChannelId(channelId);
		syncTestFolder.pushToMaster(masterNode);
		t.resetChannel();
		t.commit(false);

		// check whether the pushed folder is now a subfolder of the master root folder
		masterNodeRootFolder = t.getObject(Folder.class, masterNodeRootFolder.getId());
		childFolders = masterNodeRootFolder.getChildFolders();
		assertTrue("Root folder must see pushed subfolder", childFolders.contains(syncTestFolder));
	}

	@Test
	public void testPushToMasterPublishFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create new folder in the test folder and add a page so it could be published
		Folder syncTestFolder = t.createObject(Folder.class);

		syncTestFolder.setName("syncTestFolder");
		syncTestFolder.setMotherId(testFolder.getId());
		syncTestFolder.setPublishDir("/syncTestFolder");
		syncTestFolder.save();
		t.commit(false);
		
		Page syncTestPage = t.createObject(Page.class);

		syncTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		syncTestPage.setName("syncTestPage");
		syncTestPage.setFolderId(syncTestFolder.getId());
		syncTestPage.setFilename("syncTestPage.html");
		syncTestPage.save();
		syncTestPage.publish();
		t.commit(false);
		
		// Localize the test folder in the channel
		Folder syncLocalizedTestFolder = (Folder) syncTestFolder.copy();

		syncLocalizedTestFolder.setName("syncLocalizedFolder");
		syncLocalizedTestFolder.setPublishDir("/syncLocalizedTestFolder");
		syncLocalizedTestFolder.setChannelInfo(channelId, syncTestFolder.getChannelSetId());
		syncLocalizedTestFolder.save();
		t.commit(false);
		
		Page syncLocalizedTestPage = (Page) syncTestPage.copy();

		syncLocalizedTestPage.setName("syncLocalizedTestPage");
		syncLocalizedTestPage.setFolderId(syncLocalizedTestFolder.getId());
		syncLocalizedTestPage.setFilename("syncLocalizedTestPage.html");
		syncLocalizedTestPage.setChannelInfo(channelId, syncTestPage.getChannelSetId());
		syncLocalizedTestPage.save();
		syncLocalizedTestPage.publish();
		t.commit(false);
		
		// Run publish process and check if the local folder is only present in the channel node
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(syncTestFolder.getPublishDir(), true);
		checkFilePathChannel(syncLocalizedTestFolder.getPublishDir(), true);
		
		// Push localized version to master node
		syncLocalizedTestFolder.pushToMaster(masterNode);
		syncLocalizedTestPage.pushToMaster(masterNode);
		t.commit(false);
		
		// Start publish process again and check if the folder is in both master and channel nodes
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false);
		
		t = testContext.startTransactionWithPermissions(false);
		
		checkFilePathMaster(syncLocalizedTestFolder.getPublishDir(), true);
		checkFilePathChannel(syncLocalizedTestFolder.getPublishDir(), true);
	}
	
	@Test
	public void testPushToMasterLocalizedTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		 
		// Create test template
		Template syncTestTemplate = t.createObject(Template.class);

		syncTestTemplate.setName("syncLocalizedTestTemplate");
		syncTestTemplate.setDescription("Test description");
		syncTestTemplate.setFolderId(testFolder.getId());
		syncTestTemplate.setMlId(1);
		syncTestTemplate.setSource("test");
		syncTestTemplate.save();
		
		// Localize the test template in the channel
		Template syncLocalizedTestTemplate = (Template) syncTestTemplate.copy();

		syncLocalizedTestTemplate.setChannelInfo(channelId, syncTestTemplate.getChannelSetId());
		syncLocalizedTestTemplate.setName("syncLocalizedTestTemplate");
		syncLocalizedTestTemplate.save();
		t.commit(false);
		
		// Push localized version to master node
		syncLocalizedTestTemplate.pushToMaster(masterNode);
		t.commit(false);

		// Retrieve the latest versions of our template from master and channel nodes
		Template masterTestTemplate = (Template) t.getObject(Template.class, syncTestTemplate.getId());
		Template localizedChannelTemplate = (Template) t.getObject(Template.class, syncLocalizedTestTemplate.getId());

		// Check if the behaviour is as expected
		assertNotNull("Master copy should exist", masterTestTemplate);
		assertNull("Localized copy should not exist now", localizedChannelTemplate);
		
		assertEquals("Check the name of the master copy", "syncLocalizedTestTemplate", masterTestTemplate.getName());
	}
	
	@Test
	public void testPushToMasterLocalTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Localize the test template in the channel
		Template syncLocalTestTemplate = t.createObject(Template.class);

		syncLocalTestTemplate.setName("LocalChannelSyncTest");
		syncLocalTestTemplate.setDescription("Some sample description");
		syncLocalTestTemplate.setMlId(1);
		syncLocalTestTemplate.setSource("some sample source");
		syncLocalTestTemplate.setFolderId(testFolder.getId());
		syncLocalTestTemplate.setChannelInfo(channelId, syncLocalTestTemplate.getChannelSetId());
		syncLocalTestTemplate.save();
		t.commit(false);
		
		// Push localized version to master node
		syncLocalTestTemplate.pushToMaster(masterNode);
		t.commit(false);
		
		Template masterNodeTemplate = t.getObject(Template.class, syncLocalTestTemplate.getId());
		
		assertNotNull("Master template copy should exist now", masterNodeTemplate);
		assertFalse("The channel template shouldn't be inherited after the push to master", masterNodeTemplate.isInherited());
		
		// Check if the template is now inherited in channel node
		t.setChannelId(channelId);
		
		Template localizedChannelTemplate = t.getObject(Template.class, syncLocalTestTemplate.getId());

		assertNotNull("Channel template copy should exist now", localizedChannelTemplate);
		assertTrue("The channel template should be inherited after the push to master", localizedChannelTemplate.isInherited());
		
		t.resetChannel();
	}
	
	@Test
	public void testPushToMasterRestFolderUnrecursive() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.setChannelInfo(channelId, testFolder.getChannelSetId());
		testFolder.save();
		t.commit(false);
		
		// Create test objects in the test folder
		Template syncSubTestTemplate = t.createObject(Template.class);

		syncSubTestTemplate.setName("syncSubTestTemplate");
		syncSubTestTemplate.setDescription("Some sample description");
		syncSubTestTemplate.setMlId(1);
		syncSubTestTemplate.setSource("some sample source");
		syncSubTestTemplate.setFolderId(testFolder.getId());
		syncSubTestTemplate.setChannelInfo(channelId, syncSubTestTemplate.getChannelSetId());
		syncSubTestTemplate.save();
		// link the template to the folder
		testFolder.setTemplates(Arrays.asList(syncSubTestTemplate));
		testFolder.save();

		Page syncSubTestPage = t.createObject(Page.class);

		syncSubTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		syncSubTestPage.setName("syncSubTestPage");
		syncSubTestPage.setFolderId(testFolder.getId());
		syncSubTestPage.setFilename("syncSubTestPage.html");
		syncSubTestPage.setChannelInfo(channelId, syncSubTestPage.getChannelSetId());
		syncSubTestPage.save();
		
		Folder syncSubTestFolder = t.createObject(Folder.class);

		syncSubTestFolder.setName("syncSubTestFolder");
		syncSubTestFolder.setMotherId(testFolder.getId());
		syncSubTestFolder.setPublishDir("/syncSubTestFolder");
		syncSubTestFolder.setChannelInfo(channelId, syncSubTestFolder.getChannelSetId());
		syncSubTestFolder.save();
		
		File syncSubTestFile = t.createObject(File.class);

		syncSubTestFile.setName("syncSubTestFile.bin");
		syncSubTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		syncSubTestFile.setFolderId(testFolder.getId());
		syncSubTestFile.setChannelInfo(channelId, syncSubTestFile.getChannelSetId());
		syncSubTestFile.save();
		
		ImageFile syncSubTestImage = t.createObject(ImageFile.class);

		syncSubTestImage.setName("syncSubTestImage");
		syncSubTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		syncSubTestImage.setFolderId(testFolder.getId());
		syncSubTestImage.setChannelInfo(channelId, syncSubTestImage.getChannelSetId());
		syncSubTestImage.save();
		t.commit(false);

		// Push to master the test folder
		PushToMasterRequest pushToMasterRequest = new PushToMasterRequest();

		pushToMasterRequest.setChannelId(channelId);
		pushToMasterRequest.setForegroundTime(Integer.MAX_VALUE);
		pushToMasterRequest.setMasterId(ObjectTransformer.getInt(masterNode.getId(), -1));
		pushToMasterRequest.setRecursive(false);

		try (Trx trx = new Trx(user)) {
			MCFolderResourceImpl folderResource = new MCFolderResourceImpl();
			GenericResponse response = folderResource.pushToMaster(ObjectTransformer.getInt(testFolder.getId(), -1), pushToMasterRequest);
			assertEquals("Check the response code.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		t.commit(false);
		t.setChannelId(channelId);

		try {
			// check that the objects in the folder still are localized in the channel
			syncSubTestTemplate = t.getObject(Template.class, syncSubTestTemplate.getId());
			assertNotNull("Check that the object exists", syncSubTestTemplate);
			assertFalse("Check that the object is not inherited.", syncSubTestTemplate.isInherited());

			syncSubTestPage = t.getObject(Page.class, syncSubTestPage.getId());
			assertNotNull("Check that the object exists", syncSubTestPage);
			assertFalse("Check that the object is not inherited.", syncSubTestPage.isInherited());

			syncSubTestFolder = t.getObject(Folder.class, syncSubTestFolder.getId());
			assertNotNull("Check that the object exists", syncSubTestFolder);
			assertFalse("Check that the object is not inherited.", syncSubTestFolder.isInherited());

			syncSubTestFile = t.getObject(File.class, syncSubTestFile.getId());
			assertNotNull("Check that the object exists", syncSubTestFile);
			assertFalse("Check that the object is not inherited.", syncSubTestFile.isInherited());

			syncSubTestImage = t.getObject(ImageFile.class, syncSubTestImage.getId());
			assertNotNull("Check that the object exists", syncSubTestImage);
			assertFalse("Check that the object is not inherited.", syncSubTestImage.isInherited());

			testFolder = t.getObject(Folder.class, testFolder.getId());
			assertTrue("Check if the test folder is inherited in the channel after the push", testFolder.isInherited());
		} finally {
			t.resetChannel();
		}
	}

	@Test
	public void testPushToMasterRestFolderRecursive() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.setChannelInfo(channelId, testFolder.getChannelSetId());
		testFolder.save();
		t.commit(false);
		
		// Create test objects in the test folder
		Template syncSubTestTemplate = t.createObject(Template.class);

		syncSubTestTemplate.setName("syncSubTestTemplate");
		syncSubTestTemplate.setDescription("Some sample description");
		syncSubTestTemplate.setMlId(1);
		syncSubTestTemplate.setSource("some sample source");
		syncSubTestTemplate.setFolderId(testFolder.getId());
		syncSubTestTemplate.setChannelInfo(channelId, syncSubTestTemplate.getChannelSetId());
		syncSubTestTemplate.save();
		// link the template to the folder
		testFolder.setTemplates(Arrays.asList(syncSubTestTemplate));
		testFolder.save();

		Page syncSubTestPage = t.createObject(Page.class);

		syncSubTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		syncSubTestPage.setName("syncSubTestPage");
		syncSubTestPage.setFolderId(testFolder.getId());
		syncSubTestPage.setFilename("syncSubTestPage.html");
		syncSubTestPage.setChannelInfo(channelId, syncSubTestPage.getChannelSetId());
		syncSubTestPage.save();
		
		Folder syncSubTestFolder = t.createObject(Folder.class);

		syncSubTestFolder.setName("syncSubTestFolder");
		syncSubTestFolder.setMotherId(testFolder.getId());
		syncSubTestFolder.setPublishDir("/syncSubTestFolder");
		syncSubTestFolder.setChannelInfo(channelId, syncSubTestFolder.getChannelSetId());
		syncSubTestFolder.save();
		
		File syncSubTestFile = t.createObject(File.class);

		syncSubTestFile.setName("syncSubTestFile.bin");
		syncSubTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		syncSubTestFile.setFolderId(testFolder.getId());
		syncSubTestFile.setChannelInfo(channelId, syncSubTestFile.getChannelSetId());
		syncSubTestFile.save();
		
		ImageFile syncSubTestImage = t.createObject(ImageFile.class);

		syncSubTestImage.setName("syncSubTestImage");
		syncSubTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		syncSubTestImage.setFolderId(testFolder.getId());
		syncSubTestImage.setChannelInfo(channelId, syncSubTestImage.getChannelSetId());
		syncSubTestImage.save();
		t.commit(false);

		// Push to master the test folder
		PushToMasterRequest pushToMasterRequest = new PushToMasterRequest();

		pushToMasterRequest.setChannelId(channelId);
		pushToMasterRequest.setForegroundTime(Integer.MAX_VALUE);
		pushToMasterRequest.setMasterId(ObjectTransformer.getInt(masterNode.getId(), -1));
		pushToMasterRequest.setTypes(
				Arrays.asList(PushToMasterRequest.Type.page, PushToMasterRequest.Type.file, PushToMasterRequest.Type.image, PushToMasterRequest.Type.template,
				PushToMasterRequest.Type.folder));
		pushToMasterRequest.setRecursive(true);

		try (Trx trx = new Trx(user)) {
			MCFolderResourceImpl folderResource = new MCFolderResourceImpl();
			GenericResponse response = folderResource.pushToMaster(ObjectTransformer.getInt(testFolder.getId(), -1), pushToMasterRequest);
			assertEquals("Check the response code.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		t.commit(false);
		t.setChannelId(channelId);

		try {
			syncSubTestTemplate = t.getObject(Template.class, syncSubTestTemplate.getId());
			assertTrue("Check that the object is inherited.", syncSubTestTemplate.isInherited());
    	
			syncSubTestPage = t.getObject(Page.class, syncSubTestPage.getId());
			assertTrue("Check that the object is inherited.", syncSubTestPage.isInherited());
    	
			syncSubTestFolder = t.getObject(Folder.class, syncSubTestFolder.getId());
			assertTrue("Check that the object is not inherited.", syncSubTestFolder.isInherited());
    	
			syncSubTestFile = t.getObject(File.class, syncSubTestFile.getId());
			assertTrue("Check that the object is not inherited.", syncSubTestFile.isInherited());
    	
			syncSubTestImage = t.getObject(ImageFile.class, syncSubTestImage.getId());
			assertTrue("Check that the object is not inherited.", syncSubTestImage.isInherited());
			
			testFolder = t.getObject(Folder.class, testFolder.getId());
			assertTrue("Check if the test folder is inherited in the channel after the push", testFolder.isInherited());
		} finally {
			t.resetChannel();
		}
	}

	/**
	 * Test pushing a page, that is currently locked by another user into the master
	 * @throws Exception
	 */
	@Test
	public void testPushLockedPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// start a transaction for the system user
		Transaction sysTrans = testContext.startSystemUserTransaction();

		// reset the normal transaction as current
		TransactionManager.setCurrentTransaction(t);

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);

		// Create a test page
		Page testPage = t.createObject(Page.class);

		testPage.setName("ChannelSyncTestPage");
		testPage.setTemplateId(C.reference_test_data.template1_id);
		testPage.setFolderId(testFolder.getId());
		testPage.save();
		testPage.unlock();
		t.commit(false);

		// localize the page
		Page localizedPage = (Page) testPage.copy();

		localizedPage.setChannelInfo(channelId, testPage.getChannelSetId());
		localizedPage.save();
		localizedPage.unlock();
		t.commit(false);

		// lock the localized page with the system user
		TransactionManager.setCurrentTransaction(sysTrans);
		sysTrans.getObject(Page.class, localizedPage.getId(), true);
		sysTrans.commit(false);

		// now try to push the localized page (with the normal transaction)
		TransactionManager.setCurrentTransaction(t);
		try {
			// we expect this to throw an exception
			localizedPage.pushToMaster(masterNode);
			fail("Pushing the locked page did not fail as expected");
		} catch (ReadOnlyException e) {}
		t.commit();
	}

	/**
	 * Test pushing a page into a master, where the master page is currently locked by another user
	 * @throws Exception
	 */
	@Test
	public void testPushPageMasterLocked() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// start a transaction for the system user
		Transaction sysTrans = testContext.startSystemUserTransaction();

		// reset the normal transaction as current
		TransactionManager.setCurrentTransaction(t);

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);

		// Create a test page
		Page testPage = t.createObject(Page.class);

		testPage.setName("ChannelSyncTestPage");
		testPage.setTemplateId(C.reference_test_data.template1_id);
		testPage.setFolderId(testFolder.getId());
		testPage.save();
		testPage.unlock();
		t.commit(false);

		// localize the page
		Page localizedPage = (Page) testPage.copy();

		localizedPage.setChannelInfo(channelId, testPage.getChannelSetId());
		localizedPage.save();
		localizedPage.unlock();
		t.commit(false);

		// lock the master page with the system user
		TransactionManager.setCurrentTransaction(sysTrans);
		sysTrans.getObject(Page.class, testPage.getId(), true);
		sysTrans.commit(false);

		// now try to push the localized page (with the normal transaction)
		TransactionManager.setCurrentTransaction(t);
		try {
			// we expect this to throw an exception
			localizedPage.pushToMaster(masterNode);
			fail("Pushing the locked page did not fail as expected");
		} catch (ReadOnlyException e) {}
		t.commit();
	}

	/**
	 * Test pushing a template, that is currently locked by another user into the master
	 * @throws Exception
	 */
	@Test
	public void testPushLockedTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// start a transaction for the system user
		Transaction sysTrans = testContext.startSystemUserTransaction();

		// reset the normal transaction as current
		TransactionManager.setCurrentTransaction(t);

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);

		// Create a test template
		Template testTemplate = t.createObject(Template.class);

		testTemplate.setName("syncSubTestTemplate");
		testTemplate.setDescription("Some sample description");
		testTemplate.setMlId(1);
		testTemplate.setSource("some sample source");
		testTemplate.setFolderId(testFolder.getId());
		testTemplate.save();
		testTemplate.unlock();
		t.commit(false);

		// localize the template
		Template localizedTemplate = (Template) testTemplate.copy();

		localizedTemplate.setChannelInfo(channelId, testTemplate.getChannelSetId());
		localizedTemplate.save();
		localizedTemplate.unlock();
		t.commit(false);

		// lock the localized template with the system user
		TransactionManager.setCurrentTransaction(sysTrans);
		sysTrans.getObject(Template.class, localizedTemplate.getId(), true);
		sysTrans.commit(false);

		// now try to push the localized template (with the normal transaction)
		TransactionManager.setCurrentTransaction(t);
		try {
			// we expect this to throw an exception
			localizedTemplate.pushToMaster(masterNode);
			fail("Pushing the locked template did not fail as expected");
		} catch (ReadOnlyException e) {}
		t.commit();
	}

	/**
	 * Test pushing a template into a master, where the master template is currently locked by another user
	 * @throws Exception
	 */
	@Test
	public void testPushTemplateMasterLocked() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// start a transaction for the system user
		Transaction sysTrans = testContext.startSystemUserTransaction();

		// reset the normal transaction as current
		TransactionManager.setCurrentTransaction(t);

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);

		// Create a test template
		Template testTemplate = t.createObject(Template.class);

		testTemplate.setName("syncSubTestTemplate");
		testTemplate.setDescription("Some sample description");
		testTemplate.setMlId(1);
		testTemplate.setSource("some sample source");
		testTemplate.setFolderId(testFolder.getId());
		testTemplate.save();
		testTemplate.unlock();
		t.commit(false);

		// localize the template
		Template localizedTemplate = (Template) testTemplate.copy();

		localizedTemplate.setChannelInfo(channelId, testTemplate.getChannelSetId());
		localizedTemplate.save();
		localizedTemplate.unlock();
		t.commit(false);

		// lock the master template with the system user
		TransactionManager.setCurrentTransaction(sysTrans);
		sysTrans.getObject(Template.class, testTemplate.getId(), true);
		sysTrans.commit(false);

		// now try to push the localized template (with the normal transaction)
		TransactionManager.setCurrentTransaction(t);
		try {
			// we expect this to throw an exception
			localizedTemplate.pushToMaster(masterNode);
			fail("Pushing the locked template did not fail as expected");
		} catch (ReadOnlyException e) {}
		t.commit();
	}

	/**
	 * Test pushing a localized object from a subchannel into a localized folder in a channel
	 * @throws Exception
	 */
	// @Test
	public void testPushIntoLocalizedFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the channel node
		Node channelNode = t.getObject(Node.class, channelId);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);

		// Create a test page
		Page testPage = t.createObject(Page.class);

		testPage.setName("ChannelSyncTestPage");
		testPage.setTemplateId(C.reference_test_data.template1_id);
		testPage.setFolderId(testFolder.getId());
		testPage.save();
		t.commit(false);

		// Create test image
		ImageFile testImage = t.createObject(ImageFile.class);

		testImage.setName(C.channel_sync.image_name_jpg);
		testImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		testImage.setFolderId(testFolder.getId());
		testImage.save();

		// localize the folder in the channel
		Folder localizedFolder = (Folder) testFolder.copy();

		localizedFolder.setChannelInfo(channelId, testFolder.getChannelSetId());
		localizedFolder.save();
		t.commit(false);

		// localize the page in the subchannel
		Page localizedTestPage = (Page) testPage.copy();

		localizedTestPage.setChannelInfo(subChannelId, testPage.getChannelSetId());
		localizedTestPage.save();
		t.commit(false);

		// localize the image in the subchannel
		ImageFile localizedTestImage = (ImageFile) testImage.copy();

		localizedTestImage.setChannelInfo(subChannelId, testImage.getChannelSetId());
		localizedTestImage.save();
		t.commit(false);

		// now push the test objects into the channel
		t.setChannelId(subChannelId);
		Page pushedPage = localizedTestPage.pushToMaster(channelNode);
		File pushedImage = localizedTestImage.pushToMaster(channelNode);

		t.resetChannel();
		t.commit(false);

		assertReferenceIntegrity();
	}
}
