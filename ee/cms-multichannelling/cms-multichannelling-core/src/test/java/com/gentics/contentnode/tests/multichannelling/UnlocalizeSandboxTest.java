package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest.Type;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCFileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCTemplateResourceImpl;

/**
 * Test cases for unlocalize function.
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
public class UnlocalizeSandboxTest extends AbstractMultichannellingSandboxTest {

	/**
	 * test unlocalize functionality using page inheritance.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeInheritedPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);

		// Create test page in the test folder
		Page unlocalizeTestPage = t.createObject(Page.class);

		unlocalizeTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		unlocalizeTestPage.setName("unlocalizeInfoPage");
		unlocalizeTestPage.setFolderId(testFolder.getId());
		unlocalizeTestPage.setFilename("unlocalizeInfoPage.html");
		unlocalizeTestPage.save();
		t.commit(false);

		// Unlocalize test page
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCPageResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestPage.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test page again
		unlocalizeTestPage = t.getObject(Page.class, unlocalizeTestPage.getId());
		assertNotNull("Check if the page still exists.", unlocalizeTestPage);
		
		// Check if the page is still inherited in channel
		t.setChannelId(channelId);
		
		unlocalizeTestPage = t.getObject(Page.class, unlocalizeTestPage.getId());
		assertNotNull("Check if the page exists.", unlocalizeTestPage);
		assertTrue("Check if the page is still inherited in channel.", unlocalizeTestPage.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using page localization.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeLocalizedPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test page in the test folder
		Page unlocalizeTestPage = t.createObject(Page.class);

		unlocalizeTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		unlocalizeTestPage.setName("unlocalizeInfoPage");
		unlocalizeTestPage.setFolderId(testFolder.getId());
		unlocalizeTestPage.setFilename("unlocalizeInfoPage.html");
		unlocalizeTestPage.save();
		t.commit(false);
		
		Page unlocalizeTestPageLocalized = (Page) unlocalizeTestPage.copy();

		unlocalizeTestPageLocalized.setChannelInfo(channelId, unlocalizeTestPage.getChannelSetId());
		unlocalizeTestPageLocalized.save();
		t.commit(false);
		
		// Unlocalize test page
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCPageResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestPageLocalized.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test page again
		unlocalizeTestPage = t.getObject(Page.class, unlocalizeTestPage.getId());
		assertNotNull("Check if the page still exists.", unlocalizeTestPage);
		unlocalizeTestPageLocalized = t.getObject(Page.class, unlocalizeTestPageLocalized.getId());
		assertNull("Check if the localized page still exists.", unlocalizeTestPageLocalized);

		// Check if the page is inherited in channel instead of localized
		t.setChannelId(channelId);
		
		unlocalizeTestPage = t.getObject(Page.class, unlocalizeTestPage.getId());
		assertNotNull("Check if the page exists.", unlocalizeTestPage);
		assertTrue("Check if the page is still inherited in channel.", unlocalizeTestPage.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using local pages.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeLocalPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test page in the test folder
		Page unlocalizeTestPage = t.createObject(Page.class);

		unlocalizeTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		unlocalizeTestPage.setName("unlocalizeTestPage");
		unlocalizeTestPage.setFolderId(testFolder.getId());
		unlocalizeTestPage.setFilename("unlocalizeTestPage.html");
		unlocalizeTestPage.setChannelInfo(channelId, unlocalizeTestPage.getChannelSetId());
		unlocalizeTestPage.save();
		t.commit(false);
		
		// Unlocalize test page
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCPageResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestPage.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Check if the page no longer exists
		t.setChannelId(channelId);
		
		unlocalizeTestPage = t.getObject(Page.class, unlocalizeTestPage.getId());
		assertNull("Check if the page exists.", unlocalizeTestPage);
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using image inheritance.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeInheritedImage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test image in the test folder
		ImageFile unlocalizeTestImage = t.createObject(ImageFile.class);

		unlocalizeTestImage.setName("unlocalizeTestImage.jpg");
		unlocalizeTestImage.setFolderId(testFolder.getId());
		unlocalizeTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		unlocalizeTestImage.save();
		t.commit(false);
		
		// Unlocalize test image
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCImageResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestImage.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test image again
		unlocalizeTestImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());
		assertNotNull("Check if the image still exists.", unlocalizeTestImage);
		
		// Check if the image is still inherited in channel
		t.setChannelId(channelId);
		
		unlocalizeTestImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());
		assertNotNull("Check if the image exists.", unlocalizeTestImage);
		assertTrue("Check if the image is still inherited in channel.", unlocalizeTestImage.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using image localization.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeLocalizedImage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test image in the test folder
		ImageFile unlocalizeTestImage = t.createObject(ImageFile.class);

		unlocalizeTestImage.setName("unlocalizeTestImage.jpg");
		unlocalizeTestImage.setFolderId(testFolder.getId());
		unlocalizeTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		unlocalizeTestImage.save();
		t.commit(false);
		
		ImageFile unlocalizeTestImageLocalized = (ImageFile) unlocalizeTestImage.copy();

		unlocalizeTestImageLocalized.setChannelInfo(channelId, unlocalizeTestImage.getChannelSetId());
		unlocalizeTestImageLocalized.save();
		t.commit(false);
		
		// Unlocalize test image
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCImageResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestImageLocalized.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test page again
		unlocalizeTestImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());
		assertNotNull("Check if the image still exists.", unlocalizeTestImage);
		unlocalizeTestImageLocalized = t.getObject(ImageFile.class, unlocalizeTestImageLocalized.getId());
		assertNull("Check if the localized image still exists.", unlocalizeTestImageLocalized);
		
		// Check if the image is inherited in channel instead of localized
		t.setChannelId(channelId);
		
		unlocalizeTestImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());
		assertNotNull("Check if the image exists.", unlocalizeTestImage);
		assertTrue("Check if the image is still inherited in channel.", unlocalizeTestImage.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using local images.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	@Ignore("Disabled unstable test")
	public void testUnlocalizeLocalImage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test image in the test folder
		ImageFile unlocalizeTestImage = t.createObject(ImageFile.class);

		unlocalizeTestImage.setName("unlocalizeTestImage.jpg");
		unlocalizeTestImage.setFolderId(testFolder.getId());
		unlocalizeTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		unlocalizeTestImage.setChannelInfo(channelId, unlocalizeTestImage.getChannelSetId());
		unlocalizeTestImage.save();
		t.commit(false);
		
		// Unlocalize test image
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCImageResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestImage.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Check if the page no longer exists
		t.setChannelId(channelId);
		
		unlocalizeTestImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());
		assertNull("Check if the image exists.", unlocalizeTestImage);
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using file inheritance.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeInheritedFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test file in the test folder
		File unlocalizeTestFile = t.createObject(File.class);

		unlocalizeTestFile.setName("unlocalizeTestFile.bin");
		unlocalizeTestFile.setFolderId(testFolder.getId());
		unlocalizeTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		unlocalizeTestFile.save();
		t.commit(false);
		
		// Unlocalize test file
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFileResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestFile.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test file again
		unlocalizeTestFile = t.getObject(File.class, unlocalizeTestFile.getId());
		assertNotNull("Check if the file still exists.", unlocalizeTestFile);
		
		// Check if the file is still inherited in channel
		t.setChannelId(channelId);
		
		unlocalizeTestFile = t.getObject(File.class, unlocalizeTestFile.getId());
		assertNotNull("Check if the file exists.", unlocalizeTestFile);
		assertTrue("Check if the file is still inherited in channel.", unlocalizeTestFile.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using file localization.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeLocalizedFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test file in the test folder
		File unlocalizeTestFile = t.createObject(File.class);

		unlocalizeTestFile.setName("unlocalizeTestFile.bin");
		unlocalizeTestFile.setFolderId(testFolder.getId());
		unlocalizeTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		unlocalizeTestFile.save();
		t.commit(false);
		
		File unlocalizeTestFileLocalized = (File) unlocalizeTestFile.copy();

		unlocalizeTestFileLocalized.setChannelInfo(channelId, unlocalizeTestFile.getChannelSetId());
		unlocalizeTestFileLocalized.save();
		t.commit(false);
		
		// Unlocalize test file
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFileResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestFileLocalized.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test file again
		unlocalizeTestFile = t.getObject(File.class, unlocalizeTestFile.getId());
		assertNotNull("Check if the file still exists.", unlocalizeTestFile);
		unlocalizeTestFileLocalized = t.getObject(File.class, unlocalizeTestFileLocalized.getId());
		assertNull("Check if the localized file still exists.", unlocalizeTestFileLocalized);
		
		// Check if the file is inherited in channel instead of localized
		t.setChannelId(channelId);
		
		unlocalizeTestFile = t.getObject(File.class, unlocalizeTestFile.getId());
		assertNotNull("Check if the file exists.", unlocalizeTestFile);
		assertTrue("Check if the file is still inherited in channel.", unlocalizeTestFile.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using local files.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeLocalFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test file in the test folder
		File unlocalizeTestFile = t.createObject(File.class);

		unlocalizeTestFile.setName("unlocalizeTestFile.bin");
		unlocalizeTestFile.setFolderId(testFolder.getId());
		unlocalizeTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		unlocalizeTestFile.setChannelInfo(channelId, unlocalizeTestFile.getChannelSetId());
		unlocalizeTestFile.save();
		t.commit(false);
		
		// Unlocalize test file
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFileResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestFile.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Check if the file no longer exists
		t.setChannelId(channelId);
		
		unlocalizeTestFile = t.getObject(File.class, unlocalizeTestFile.getId());
		assertNull("Check if the file exists.", unlocalizeTestFile);
		
		t.resetChannel();
	}
	
	@Test
	public void testUnlocalizeInheritedTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test template in the test folder
		Template unlocalizeTestTemplate = t.createObject(Template.class);

		unlocalizeTestTemplate.setName("unlocalizeTestTemplate");
		unlocalizeTestTemplate.setFolderId(testFolder.getId());
		unlocalizeTestTemplate.setDescription("Test description");
		unlocalizeTestTemplate.setMlId(1);
		unlocalizeTestTemplate.setSource("test");
		unlocalizeTestTemplate.save();
		t.commit(false);

		SystemUser systemUser = t.getObject(SystemUser.class, 1);

		Trx.consume(systemUser, templateId -> {
			// Unlocalize test file
			UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

			unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
			unlocalizeRequest.setChannelId(channelId);

			GenericResponse response = new MCTemplateResourceImpl().unlocalize(templateId, unlocalizeRequest);

			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		}, unlocalizeTestTemplate.getId());

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test template again
		unlocalizeTestTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());
		assertNotNull("Check if the template still exists.", unlocalizeTestTemplate);
		
		// Check if the template is still inherited in channel
		t.setChannelId(channelId);
		
		unlocalizeTestTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());
		assertNotNull("Check if the template exists.", unlocalizeTestTemplate);
		assertTrue("Check if the template is still inherited in channel.", unlocalizeTestTemplate.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using template localization.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeLocalizedTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test template in the test folder
		Template unlocalizeTestTemplate = t.createObject(Template.class);

		unlocalizeTestTemplate.setName("unlocalizeTestFile.jpg");
		unlocalizeTestTemplate.setFolderId(testFolder.getId());
		unlocalizeTestTemplate.setDescription("Test description");
		unlocalizeTestTemplate.setMlId(1);
		unlocalizeTestTemplate.setSource("test");
		unlocalizeTestTemplate.save();
		unlocalizeTestTemplate.unlock();
		t.commit(false);
		
		Template unlocalizeTestTemplateLocalized = (Template) unlocalizeTestTemplate.copy();

		unlocalizeTestTemplateLocalized.setChannelInfo(channelId, unlocalizeTestTemplate.getChannelSetId());
		unlocalizeTestTemplateLocalized.save();
		unlocalizeTestTemplateLocalized.unlock();
		t.commit(false);

		SystemUser systemUser = t.getObject(SystemUser.class, 1);

		Trx.consume(systemUser, templateId -> {
			// Unlocalize test template
			UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

			unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
			unlocalizeRequest.setChannelId(channelId);

			GenericResponse response = new MCTemplateResourceImpl().unlocalize(templateId, unlocalizeRequest);

			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		}, unlocalizeTestTemplateLocalized.getId());

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test template again
		unlocalizeTestTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());
		assertNotNull("Check if the template still exists.", unlocalizeTestTemplate);
		unlocalizeTestTemplateLocalized = t.getObject(Template.class, unlocalizeTestTemplateLocalized.getId());
		assertNull("Check if the localized template still exists.", unlocalizeTestTemplateLocalized);
		
		// Check if the template is inherited in channel instead of localized
		t.setChannelId(channelId);
		
		unlocalizeTestTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());
		assertNotNull("Check if the template exists.", unlocalizeTestTemplate);
		assertTrue("Check if the template is still inherited in channel.", unlocalizeTestTemplate.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using local files.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeLocalTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test template in the test folder
		Template unlocalizeTestTemplate = t.createObject(Template.class);

		unlocalizeTestTemplate.setName("unlocalizeTestFile.jpg");
		unlocalizeTestTemplate.setFolderId(testFolder.getId());
		unlocalizeTestTemplate.setDescription("Test description");
		unlocalizeTestTemplate.setMlId(1);
		unlocalizeTestTemplate.setSource("test");
		unlocalizeTestTemplate.setChannelInfo(channelId, unlocalizeTestTemplate.getChannelSetId());
		unlocalizeTestTemplate.save();
		unlocalizeTestTemplate.unlock();
		t.commit(false);

		SystemUser systemUser = t.getObject(SystemUser.class, 1);

		Trx.consume(systemUser, templateId -> {
			// Unlocalize test template
			UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

			unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
			unlocalizeRequest.setChannelId(channelId);

			GenericResponse response = new MCTemplateResourceImpl().unlocalize(templateId, unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		}, unlocalizeTestTemplate.getId());

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Check if the template no longer exists
		t.setChannelId(channelId);
		
		unlocalizeTestTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());
		assertNull("Check if the template exists.", unlocalizeTestTemplate);
		
		t.resetChannel();
	}
	
	@Test
	public void testUnlocalizeInheritedFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test folder in the test folder
		Folder unlocalizeTestFolder = t.createObject(Folder.class);

		unlocalizeTestFolder.setName("unlocalizeTestFolder");
		unlocalizeTestFolder.setMotherId(testFolder.getId());
		unlocalizeTestFolder.setDescription("Test description");
		unlocalizeTestFolder.save();
		t.commit(false);
		
		// Unlocalize test folder
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestFolder.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test folder again
		unlocalizeTestFolder = t.getObject(Folder.class, unlocalizeTestFolder.getId());
		assertNotNull("Check if the folder still exists.", unlocalizeTestFolder);
		
		// Check if the folder is still inherited in channel
		t.setChannelId(channelId);
		
		unlocalizeTestFolder = t.getObject(Folder.class, unlocalizeTestFolder.getId());
		assertNotNull("Check if the folder exists.", unlocalizeTestFolder);
		assertTrue("Check if the folder is still inherited in channel.", unlocalizeTestFolder.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using folder localization.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	@Ignore("Disable unstable test")
	public void testUnlocalizeLocalizedFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test folder in the test folder
		Folder unlocalizeTestFolder = t.createObject(Folder.class);

		unlocalizeTestFolder.setName("unlocalizeTestFolder");
		unlocalizeTestFolder.setMotherId(testFolder.getId());
		unlocalizeTestFolder.setDescription("Test description");
		unlocalizeTestFolder.save();
		t.commit(false);
		
		Folder unlocalizeTestFolderLocalized = (Folder) unlocalizeTestFolder.copy();

		unlocalizeTestFolderLocalized.setChannelInfo(channelId, unlocalizeTestFolder.getChannelSetId());
		unlocalizeTestFolderLocalized.save();
		t.commit(false);
		
		// Unlocalize test folder
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestFolderLocalized.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Retrieve the test folder again
		unlocalizeTestFolder = t.getObject(Folder.class, unlocalizeTestFolder.getId());
		assertNotNull("Check if the folder still exists.", unlocalizeTestFolder);
		unlocalizeTestFolderLocalized = t.getObject(Folder.class, unlocalizeTestFolderLocalized.getId());
		assertNull("Check if the localized folder still exists.", unlocalizeTestFolderLocalized);
		
		// Check if the folder is inherited in channel instead of localized
		t.setChannelId(channelId);
		
		unlocalizeTestFolder = t.getObject(Folder.class, unlocalizeTestFolder.getId());
		assertNotNull("Check if the folder exists.", unlocalizeTestFolder);
		assertTrue("Check if the folder is still inherited in channel.", unlocalizeTestFolder.isInherited());
		
		t.resetChannel();
	}
	
	/**
	 * test unlocalize functionality using local folders.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testUnlocalizeLocalFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("UnlocalizeInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create test folder in the test folder
		Folder unlocalizeTestFolder = t.createObject(Folder.class);

		unlocalizeTestFolder.setName("unlocalizeTestFolder");
		unlocalizeTestFolder.setMotherId(testFolder.getId());
		unlocalizeTestFolder.setDescription("Test description");
		unlocalizeTestFolder.setChannelInfo(channelId, unlocalizeTestFolder.getChannelSetId());
		unlocalizeTestFolder.save();
		t.commit(false);
		
		// Unlocalize test folder
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().unlocalize(ObjectTransformer.getInt(unlocalizeTestFolder.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);

		// Check if the folder no longer exists
		t.setChannelId(channelId);
		
		unlocalizeTestFolder = t.getObject(Folder.class, unlocalizeTestFolder.getId());
		assertNull("Check if the folder exists.", unlocalizeTestFolder);
		
		t.resetChannel();
	}
	
	@Test
	public void testUnlocalizeFolderRecursive() throws Exception {
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
		
		// Create test objects in the test folder
		Template unlocalizeTestTemplate = t.createObject(Template.class);

		unlocalizeTestTemplate.setName("unlocalizeTestTemplate");
		unlocalizeTestTemplate.setDescription("Some sample description");
		unlocalizeTestTemplate.setMlId(1);
		unlocalizeTestTemplate.setSource("some sample source");
		unlocalizeTestTemplate.setFolderId(testFolder.getId());
		unlocalizeTestTemplate.save();
		// link the template to the folder
		testFolder.setTemplates(Arrays.asList(unlocalizeTestTemplate));
		testFolder.save();

		Page unlocalizeTestPage = t.createObject(Page.class);

		unlocalizeTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		unlocalizeTestPage.setName("unlocalizeTestPage");
		unlocalizeTestPage.setFolderId(testFolder.getId());
		unlocalizeTestPage.setFilename("unlocalizeTestPage.html");
		unlocalizeTestPage.save();
		
		Folder unlocalizeTestFolder = t.createObject(Folder.class);

		unlocalizeTestFolder.setName("unlocalizeTestFolder");
		unlocalizeTestFolder.setMotherId(testFolder.getId());
		unlocalizeTestFolder.setPublishDir("/syncSubTestFolder");
		unlocalizeTestFolder.save();
		
		File unlocalizeTestFile = t.createObject(File.class);

		unlocalizeTestFile.setName("unlocalizeTestFile");
		unlocalizeTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		unlocalizeTestFile.setFolderId(testFolder.getId());
		unlocalizeTestFile.save();
		
		ImageFile unlocalizeTestImage = t.createObject(ImageFile.class);

		unlocalizeTestImage.setName("unlocalizeTestImage");
		unlocalizeTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		unlocalizeTestImage.setFolderId(testFolder.getId());
		unlocalizeTestImage.save();
		t.commit(false);
    	
		// Localize objects
		Folder localizedTestFolder = (Folder) testFolder.copy();

		localizedTestFolder.setName("unlocalizeTestFolder");
		localizedTestFolder.setPublishDir("/unlocalizeTestFolder");
		localizedTestFolder.setChannelInfo(channelId, testFolder.getChannelSetId());
		localizedTestFolder.save();
    	
		Template unlocalizeLocalizedTestTemplate = (Template) unlocalizeTestTemplate.copy();

		unlocalizeLocalizedTestTemplate.setName("unlocalizeLocalizedTestTemplate");
		unlocalizeLocalizedTestTemplate.setChannelInfo(channelId, unlocalizeTestTemplate.getChannelSetId());
		unlocalizeLocalizedTestTemplate.save();
		
		Page unlocalizeLocalizedTestPage = (Page) unlocalizeTestPage.copy();

		unlocalizeLocalizedTestPage.setName("unlocalizeLocalizedTestPage");
		unlocalizeLocalizedTestPage.setFilename("unlocalizeLocalizedTestPage.html");
		unlocalizeLocalizedTestPage.setChannelInfo(channelId, unlocalizeTestPage.getChannelSetId());
		unlocalizeLocalizedTestPage.save();
		
		Folder unlocalizeLocalizedTestFolder = (Folder) unlocalizeTestFolder.copy();

		unlocalizeLocalizedTestFolder.setName("unlocalizeLocalizedTestFolder");
		unlocalizeLocalizedTestFolder.setPublishDir("/unlocalizeLocalizedTestFolder");
		unlocalizeLocalizedTestFolder.setChannelInfo(channelId, unlocalizeTestFolder.getChannelSetId());
		unlocalizeLocalizedTestFolder.save();
		
		File unlocalizeLocalizedTestFile = (File) unlocalizeTestFile.copy();

		unlocalizeLocalizedTestFile.setName("unlocalizeLocalizedTestFile.bin");
		unlocalizeLocalizedTestFile.setChannelInfo(channelId, unlocalizeTestFile.getChannelSetId());
		unlocalizeLocalizedTestFile.save();
		
		ImageFile unlocalizeLocalizedTestImage = (ImageFile) unlocalizeTestImage.copy();

		unlocalizeLocalizedTestImage.setName("unlocalizeLocalizedTestImage");
		unlocalizeLocalizedTestImage.setChannelInfo(channelId, unlocalizeTestImage.getChannelSetId());
		unlocalizeLocalizedTestImage.save();
		t.commit(false);
    	
		// Unlocalize test folder - not recursively
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);
		unlocalizeRequest.setRecursive(false);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().unlocalize(ObjectTransformer.getInt(localizedTestFolder.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);
		
		// Check objects state from the master node point of view
		Folder checkMasterTestFolder = t.getObject(Folder.class, testFolder.getId());

		assertNotNull("The test folder should exist in master node", checkMasterTestFolder);
		
		Folder checkMasterTestFolderLocalized = t.getObject(Folder.class, localizedTestFolder.getId());

		assertNull("Test localized folder should not exist.", checkMasterTestFolderLocalized);
		
		Template checkMasterTestTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());

		assertNotNull("The test template should exist in master node", checkMasterTestTemplate);
		
		Template checkMasterTestTemplateLocalized = t.getObject(Template.class, unlocalizeLocalizedTestTemplate.getId());

		assertNotNull("Test localized template should exist.", checkMasterTestTemplateLocalized);

		Page checkMasterTestPage = t.getObject(Page.class, unlocalizeTestPage.getId());

		assertNotNull("The test page should exist in master node", checkMasterTestPage);
		
		Page checkMasterTestPageLocalized = t.getObject(Page.class, unlocalizeLocalizedTestPage.getId());

		assertNotNull("Test localized page should exist.", checkMasterTestPageLocalized);
		
		File checkMasterTestFile = t.getObject(File.class, unlocalizeTestFile.getId());

		assertNotNull("The test file should exist in master node", checkMasterTestFile);
		
		File checkMasterTestFileLocalized = t.getObject(File.class, unlocalizeLocalizedTestFile.getId());

		assertNotNull("Test localized file should exist.", checkMasterTestFileLocalized);
		
		ImageFile checkMasterTestImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());

		assertNotNull("The test template should exist in master node", checkMasterTestImage);
		
		ImageFile checkMasterTestImageLocalized = t.getObject(ImageFile.class, unlocalizeLocalizedTestImage.getId());

		assertNotNull("Test localized template should exist.", checkMasterTestImageLocalized);
		
		t.setChannelId(channelId);
		
		// Check objects state from the channel node point of view
		// Check if only the containing test folder is unlocalized.
		Folder checkChannelTestFolder = t.getObject(Folder.class, testFolder.getId());

		assertNotNull("The test folder should exist in channel node", checkChannelTestFolder);
		assertTrue("Test folder in channel should be inherited now.", checkChannelTestFolder.isInherited());
		
		Template tmpTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());

		assertNotNull("The test template should exist in channel node", tmpTemplate);
		assertFalse("Check if the template is still localized and not inherited.", tmpTemplate.isInherited());
		
		Page tmpPage = t.getObject(Page.class, unlocalizeTestPage.getId());

		assertNotNull("The test page should exist in channel node", tmpPage);
		assertFalse("Check if the page is still localized and not inherited.", tmpPage.isInherited());
		
		Folder tmpFolder = t.getObject(Folder.class, unlocalizeTestFolder.getId());

		assertNotNull("The test folder should exist in channel node", tmpFolder);
		assertFalse("Check if the folder is still localized and not inherited.", tmpFolder.isInherited());
		
		File tmpFile = t.getObject(File.class, unlocalizeTestFile.getId());

		assertNotNull("The test file should exist in channel node", tmpFile);
		assertFalse("Check if the file is still localized and not inherited.", tmpFile.isInherited());
		
		ImageFile tmpImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());

		assertNotNull("The test image should exist in channel node", tmpImage);
		assertFalse("Check if the image is still localized and not inherited.", tmpImage.isInherited());
		
		t.resetChannel();
		
		// Localize test folder again
		localizedTestFolder = (Folder) testFolder.copy();
		localizedTestFolder.setName("unlocalizeTestFolder");
		localizedTestFolder.setPublishDir("/unlocalizeTestFolder");
		localizedTestFolder.setChannelInfo(channelId, testFolder.getChannelSetId());
		localizedTestFolder.save();
		t.commit(false);
		
		// Do recursive unlocalization
		unlocalizeRequest.setRecursive(true);
		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setTypes(Arrays.asList(Type.page, Type.file, Type.template, Type.image, Type.folder));

		try (Trx trx = new Trx(user)) {
			new MCFolderResourceImpl().unlocalize(ObjectTransformer.getInt(localizedTestFolder.getId(), -1), unlocalizeRequest);
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(false);
		
		// Check objects state from the master node point of view
		checkMasterTestFolder = t.getObject(Folder.class, testFolder.getId());
		assertNotNull("The test folder should exist in master node", checkMasterTestFolder);
		
		checkMasterTestFolderLocalized = t.getObject(Folder.class, localizedTestFolder.getId());
		assertNull("Test localized folder should not exist.", checkMasterTestFolderLocalized);
		
		checkMasterTestTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());
		assertNotNull("The test template should exist in master node", checkMasterTestTemplate);
		
		checkMasterTestTemplateLocalized = t.getObject(Template.class, unlocalizeLocalizedTestTemplate.getId());
		assertNull("Test localized template should not exist.", checkMasterTestTemplateLocalized);
		
		checkMasterTestPage = t.getObject(Page.class, unlocalizeTestPage.getId());
		assertNotNull("The test page should exist in master node", checkMasterTestPage);
		
		checkMasterTestPageLocalized = t.getObject(Page.class, unlocalizeLocalizedTestPage.getId());
		assertNull("Test localized page should not exist.", checkMasterTestPageLocalized);
		
		checkMasterTestFile = t.getObject(File.class, unlocalizeTestFile.getId());
		assertNotNull("The test file should exist in master node", checkMasterTestFile);
		
		checkMasterTestFileLocalized = t.getObject(File.class, unlocalizeLocalizedTestFile.getId());
		assertNull("Test localized file should not exist.", checkMasterTestFileLocalized);
		
		checkMasterTestImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());
		assertNotNull("The test image should exist in master node", checkMasterTestImage);
		
		checkMasterTestImageLocalized = t.getObject(ImageFile.class, unlocalizeLocalizedTestImage.getId());
		assertNull("Test localized image should not exist.", checkMasterTestImageLocalized);
		
		t.setChannelId(channelId);
		
		// Check objects state from the channel node point of view
		// Check if the test folder and all the other objects are unlocalized.
		checkChannelTestFolder = t.getObject(Folder.class, testFolder.getId());
		assertNotNull("The test folder should exist in channel node", checkChannelTestFolder);
		assertTrue("Test folder in channel should be inherited now.", checkChannelTestFolder.isInherited());
		
		tmpTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());
		assertNotNull("The test template should exist in channel node", tmpTemplate);
		assertTrue("Check if the template is inherited.", tmpTemplate.isInherited());
		
		tmpPage = t.getObject(Page.class, unlocalizeTestPage.getId());
		assertNotNull("The test page should exist in channel node", tmpPage);
		assertTrue("Check if the page is inherited.", tmpPage.isInherited());
		
		tmpFolder = t.getObject(Folder.class, unlocalizeTestFolder.getId());
		assertNotNull("The test folder should exist in channel node", tmpFolder);
		assertTrue("Check if the folder is inherited.", tmpFolder.isInherited());
		
		tmpFile = t.getObject(File.class, unlocalizeTestFile.getId());
		assertNotNull("The test image should exist in channel node", tmpFile);
		assertTrue("Check if the file is inherited.", tmpFile.isInherited());
		
		tmpImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());
		assertNotNull("The test image should exist in channel node", tmpImage);
		assertTrue("Check if the image is inherited.", tmpImage.isInherited());
		
		t.resetChannel();
	}
	
	@Test
	public void testUnlocalizePageInFolder() throws Exception {
		testUnlocalizeObjectInFolder(Type.page);
	}
	
	@Test
	public void testUnlocalizeImageInFolder() throws Exception {
		testUnlocalizeObjectInFolder(Type.image);
	}
	
	@Test
	public void testUnlocalizeFileInFolder() throws Exception {
		testUnlocalizeObjectInFolder(Type.file);
	}
	
	@Test
	public void testUnlocalizeTemplateInFolder() throws Exception {
		testUnlocalizeObjectInFolder(Type.template);
	}
	
	@Test
	public void testUnlocalizeFolderInFolder() throws Exception {
		testUnlocalizeObjectInFolder(Type.folder);
	}
	
	private void testUnlocalizeObjectInFolder(Type type) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create test objects in the test folder
		Template unlocalizeTestTemplate = t.createObject(Template.class);

		unlocalizeTestTemplate.setName("unlocalizeTestTemplate");
		unlocalizeTestTemplate.setDescription("Some sample description");
		unlocalizeTestTemplate.setMlId(1);
		unlocalizeTestTemplate.setSource("some sample source");
		unlocalizeTestTemplate.setFolderId(testFolder.getId());
		unlocalizeTestTemplate.save();
		// link the template to the folder
		testFolder.setTemplates(Arrays.asList(unlocalizeTestTemplate));
		testFolder.save();

		Page unlocalizeTestPage = t.createObject(Page.class);

		unlocalizeTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		unlocalizeTestPage.setName("unlocalizeTestPage");
		unlocalizeTestPage.setFolderId(testFolder.getId());
		unlocalizeTestPage.setFilename("unlocalizeTestPage.html");
		unlocalizeTestPage.save();
		
		Folder unlocalizeTestFolder = t.createObject(Folder.class);

		unlocalizeTestFolder.setName("unlocalizeTestFolder");
		unlocalizeTestFolder.setMotherId(testFolder.getId());
		unlocalizeTestFolder.setPublishDir("/syncSubTestFolder");
		unlocalizeTestFolder.save();
		
		File unlocalizeTestFile = t.createObject(File.class);

		unlocalizeTestFile.setName("unlocalizeTestFile");
		unlocalizeTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		unlocalizeTestFile.setFolderId(testFolder.getId());
		unlocalizeTestFile.save();
		
		ImageFile unlocalizeTestImage = t.createObject(ImageFile.class);

		unlocalizeTestImage.setName("unlocalizeTestImage");
		unlocalizeTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		unlocalizeTestImage.setFolderId(testFolder.getId());
		unlocalizeTestImage.save();
		t.commit(false);
    	
		// Localize objects
		Folder localizedTestFolder = (Folder) testFolder.copy();

		localizedTestFolder.setName("unlocalizeTestFolder");
		localizedTestFolder.setPublishDir("/unlocalizeTestFolder");
		localizedTestFolder.setChannelInfo(channelId, testFolder.getChannelSetId());
		localizedTestFolder.save();
    	
		Template unlocalizeLocalizedTestTemplate = (Template) unlocalizeTestTemplate.copy();

		unlocalizeLocalizedTestTemplate.setName("unlocalizeLocalizedTestTemplate");
		unlocalizeLocalizedTestTemplate.setChannelInfo(channelId, unlocalizeTestTemplate.getChannelSetId());
		unlocalizeLocalizedTestTemplate.save();
		
		Page unlocalizeLocalizedTestPage = (Page) unlocalizeTestPage.copy();

		unlocalizeLocalizedTestPage.setName("unlocalizeLocalizedTestPage");
		unlocalizeLocalizedTestPage.setFilename("unlocalizeLocalizedTestPage.html");
		unlocalizeLocalizedTestPage.setChannelInfo(channelId, unlocalizeTestPage.getChannelSetId());
		unlocalizeLocalizedTestPage.save();
		
		Folder unlocalizeLocalizedTestFolder = (Folder) unlocalizeTestFolder.copy();

		unlocalizeLocalizedTestFolder.setName("unlocalizeLocalizedTestFolder");
		unlocalizeLocalizedTestFolder.setPublishDir("/unlocalizeLocalizedTestFolder");
		unlocalizeLocalizedTestFolder.setChannelInfo(channelId, unlocalizeTestFolder.getChannelSetId());
		unlocalizeLocalizedTestFolder.save();
		
		File unlocalizeLocalizedTestFile = (File) unlocalizeTestFile.copy();

		unlocalizeLocalizedTestFile.setName("unlocalizeLocalizedTestFile.bin");
		unlocalizeLocalizedTestFile.setChannelInfo(channelId, unlocalizeTestFile.getChannelSetId());
		unlocalizeLocalizedTestFile.save();
		
		ImageFile unlocalizeLocalizedTestImage = (ImageFile) unlocalizeTestImage.copy();

		unlocalizeLocalizedTestImage.setName("unlocalizeLocalizedTestImage");
		unlocalizeLocalizedTestImage.setChannelInfo(channelId, unlocalizeTestImage.getChannelSetId());
		unlocalizeLocalizedTestImage.save();
		t.commit(false);
    	
		// Unlocalize test folder - not recursively
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);
		unlocalizeRequest.setRecursive(false);
		unlocalizeRequest.setTypes(Arrays.asList(type));

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().unlocalize(ObjectTransformer.getInt(localizedTestFolder.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		// start a new transaction (because the unlocalize job was done in a
		// separate transaction and the results wouldn't be seen by our old
		// transaction)
		t = testContext.startTransactionWithPermissions(true);
		
		// Check objects state from the master node point of view
		Folder checkMasterRootFolder = t.getObject(Folder.class, testFolder.getId());

		assertNotNull("The test folder should exist in master node", checkMasterRootFolder);
		
		Folder checkMasterRootFolderLocalized = t.getObject(Folder.class, localizedTestFolder.getId());

		assertNull("Test localized folder should not exist.", checkMasterRootFolderLocalized);
		
		Template checkMasterTestTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());

		assertNotNull("The test template should exist in master node", checkMasterTestTemplate);
		
		Template checkMasterTestTemplateLocalized = t.getObject(Template.class, unlocalizeLocalizedTestTemplate.getId());

		if (type == Type.template) {
			assertNull("Test localized template should not exist.", checkMasterTestTemplateLocalized);
		} else {
			assertNotNull("Test localized template should exist.", checkMasterTestTemplateLocalized);
		}

		Page checkMasterTestPage = t.getObject(Page.class, unlocalizeTestPage.getId());

		assertNotNull("The test page should exist in master node", checkMasterTestPage);

		Page checkMasterTestPageLocalized = t.getObject(Page.class, unlocalizeLocalizedTestPage.getId());

		if (type == Type.page) {
			assertNull("Test localized page should not exist.", checkMasterTestPageLocalized);
		} else {
			assertNotNull("Test localized page should exist.", checkMasterTestPageLocalized);
		}
		
		File checkMasterTestFile = t.getObject(File.class, unlocalizeTestFile.getId());

		assertNotNull("The test file should exist in master node", checkMasterTestFile);
		
		File checkMasterTestFileLocalized = t.getObject(File.class, unlocalizeLocalizedTestFile.getId());

		if (type == Type.file) {
			assertNull("Test localized file should not exist.", checkMasterTestFileLocalized);
		} else {
			assertNotNull("Test localized file should exist.", checkMasterTestFileLocalized);
		}
		
		ImageFile checkMasterTestImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());

		assertNotNull("The test template should exist in master node", checkMasterTestImage);
		
		ImageFile checkMasterTestImageLocalized = t.getObject(ImageFile.class, unlocalizeLocalizedTestImage.getId());

		if (type == Type.image) {
			assertNull("Test localized image should not exist.", checkMasterTestImageLocalized);
		} else {
			assertNotNull("Test localized image should exist.", checkMasterTestImageLocalized);
		}
		
		Folder checkMasterTestFolder = t.getObject(Folder.class, unlocalizeTestFolder.getId());

		assertNotNull("The test template should exist in master node", checkMasterTestFolder);
		
		Folder checkMasterTestFolderLocalized = t.getObject(Folder.class, unlocalizeLocalizedTestFolder.getId());

		if (type == Type.folder) {
			assertNull("Test localized folder should not exist.", checkMasterTestFolderLocalized);
		} else {
			assertNotNull("Test localized folder should exist.", checkMasterTestFolderLocalized);
		}
		
		t.setChannelId(channelId);
		
		// Check objects state from the channel node point of view
		// Check if only the containing test folder is unlocalized.
		Folder checkChannelTestFolder = t.getObject(Folder.class, testFolder.getId());

		assertNotNull("The test folder should exist in channel node", checkChannelTestFolder);
		assertTrue("Test folder in channel should be inherited now.", checkChannelTestFolder.isInherited());
		
		Template tmpTemplate = t.getObject(Template.class, unlocalizeTestTemplate.getId());

		assertNotNull("The test template should exist in channel node", tmpTemplate);
		if (type == Type.template) {
			assertTrue("Check if the template is inherited.", tmpTemplate.isInherited());
		} else {
			assertFalse("Check if the template is still localized and not inherited.", tmpTemplate.isInherited());
		}
		
		Page tmpPage = t.getObject(Page.class, unlocalizeTestPage.getId());

		assertNotNull("The test page should exist in channel node", tmpPage);
		if (type == Type.page) {
			assertTrue("Check if the page is inherited.", tmpPage.isInherited());
		} else {
			assertFalse("Check if the page is still localized and not inherited.", tmpPage.isInherited());
		}
		
		Folder tmpFolder = t.getObject(Folder.class, unlocalizeTestFolder.getId());

		assertNotNull("The test folder should exist in channel node", tmpFolder);
		if (type == Type.folder) {
			assertTrue("Check if the folder is inherited.", tmpFolder.isInherited());
		} else {
			assertFalse("Check if the folder is still localized and not inherited.", tmpFolder.isInherited());
		}
		
		File tmpFile = t.getObject(File.class, unlocalizeTestFile.getId());

		assertNotNull("The test file should exist in channel node", tmpFile);
		if (type == Type.file) {
			assertTrue("Check if the file is inherited.", tmpFile.isInherited());
		} else {
			assertFalse("Check if the file is still localized and not inherited.", tmpFile.isInherited());
		}
		
		ImageFile tmpImage = t.getObject(ImageFile.class, unlocalizeTestImage.getId());

		assertNotNull("The test image should exist in channel node", tmpImage);
		if (type == Type.image) {
			assertTrue("Check if the image is inherited.", tmpImage.isInherited());
		} else {
			assertFalse("Check if the image is still localized and not inherited.", tmpImage.isInherited());
		}
		
		t.resetChannel();
	}
	
	@Test
	public void testUnlocalizeLockedPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.save();
		t.commit(false);
		
		Page unlocalizeTestPage = t.createObject(Page.class);

		unlocalizeTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		unlocalizeTestPage.setName("unlocalizeTestPage");
		unlocalizeTestPage.setFolderId(testFolder.getId());
		unlocalizeTestPage.setFilename("unlocalizeTestPage.html");
		unlocalizeTestPage.save();
		unlocalizeTestPage.unlock();
		t.commit(false);
		
		Page unlocalizeLocalizedTestPage = (Page) unlocalizeTestPage.copy();

		unlocalizeLocalizedTestPage.setName("unlocalizeLocalizedTestPage");
		unlocalizeLocalizedTestPage.setFilename("unlocalizeLocalizedTestPage.html");
		unlocalizeLocalizedTestPage.setChannelInfo(channelId, unlocalizeTestPage.getChannelSetId());
		unlocalizeLocalizedTestPage.save();
		unlocalizeLocalizedTestPage.unlock();
		t.commit(false);
		
		// start a transaction for the system user
		Transaction sysTrans = testContext.startSystemUserTransaction();

		TransactionManager.setCurrentTransaction(sysTrans);
		sysTrans.getObject(Page.class, unlocalizeLocalizedTestPage.getId(), true);
		sysTrans.commit(false);
		
		TransactionManager.setCurrentTransaction(t);
		
		// Unlocalize test folder - not recursively
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);
		unlocalizeRequest.setRecursive(false);
		unlocalizeRequest.setTypes(Arrays.asList(Type.page));

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().unlocalize(ObjectTransformer.getInt(testFolder.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response code is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		Page checkPage = t.getObject(Page.class, unlocalizeLocalizedTestPage.getId());

		assertNotNull("Verify that the localized page still exists.", checkPage);
	}
	
	@Test
	public void testUnlocalizeLockedTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.save();
		t.commit(false);
		
		Template unlocalizeTestTemplate = t.createObject(Template.class);

		unlocalizeTestTemplate.setName("unlocalizeTestTemplate");
		unlocalizeTestTemplate.setDescription("Some sample description");
		unlocalizeTestTemplate.setMlId(1);
		unlocalizeTestTemplate.setSource("some sample source");
		unlocalizeTestTemplate.setFolderId(testFolder.getId());
		unlocalizeTestTemplate.save();
		// link the template to the folder
		testFolder.setTemplates(Arrays.asList(unlocalizeTestTemplate));
		testFolder.save();
		testFolder.unlock();
		t.commit(false);
		
		Template unlocalizeLocalizedTestTemplate = (Template) unlocalizeTestTemplate.copy();

		unlocalizeLocalizedTestTemplate.setName("unlocalizeLocalizedTestTemplate");
		unlocalizeLocalizedTestTemplate.setChannelInfo(channelId, unlocalizeTestTemplate.getChannelSetId());
		unlocalizeLocalizedTestTemplate.save();
		unlocalizeLocalizedTestTemplate.unlock();
		t.commit(false);
		
		// start a transaction for the system user
		Transaction sysTrans = testContext.startSystemUserTransaction();

		TransactionManager.setCurrentTransaction(sysTrans);
		sysTrans.getObject(Template.class, unlocalizeLocalizedTestTemplate.getId(), true);
		sysTrans.commit(false);
		
		TransactionManager.setCurrentTransaction(t);
		
		// Unlocalize test folder - not recursively
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setChannelId(channelId);
		unlocalizeRequest.setRecursive(false);
		unlocalizeRequest.setTypes(Arrays.asList(Type.template));

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().unlocalize(ObjectTransformer.getInt(testFolder.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response code is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		Template checkTemplate = t.getObject(Template.class, unlocalizeLocalizedTestTemplate.getId());

		assertNotNull("Verify that the localized template still exists.", checkTemplate);
	}
}
