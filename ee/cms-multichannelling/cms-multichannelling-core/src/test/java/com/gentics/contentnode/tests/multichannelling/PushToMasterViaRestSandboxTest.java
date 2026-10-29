package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

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
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.MultiPushToMasterRequest;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest.Type;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCFolderResourceImpl;

public class PushToMasterViaRestSandboxTest extends AbstractMultichannellingSandboxTest {

	/**
	 * Test push to master via REST functionality when pushing page that is in a specified folder.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testPushToMasterPageInFolder() throws Exception {
		checkPushToMasterObjectInFolder(Type.page);
	}
	
	/**
	 * Test push to master via REST functionality when pushing file that is in a specified folder.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testPushToMasterFileInFolder() throws Exception {
		checkPushToMasterObjectInFolder(Type.file);
	}
	
	/**
	 * Test push to master via REST functionality when pushing template that is in a specified folder.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testPushToMasterTemplateInFolder() throws Exception {
		checkPushToMasterObjectInFolder(Type.template);
	}
	
	/**
	 * Test push to master via REST functionality when pushing image that is in a specified folder.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testPushToMasterImageInFolder() throws Exception {
		checkPushToMasterObjectInFolder(Type.image);
	}
	
	/**
	 * Test push to master via REST functionality when pushing folder that is in a specified folder.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testPushToMasterFolderInFolder() throws Exception {
		checkPushToMasterObjectInFolder(Type.folder);
	}

	/**
	 * Test pushing a page using a local template (which must not work)
	 * @throws Exception
	 */
	@Test
	public void testPushPageWithLocalTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
    	
		// Create the local template
		Template tmpTestTemplate = t.createObject(Template.class);
		tmpTestTemplate.setName("tmpTestTemplate");
		tmpTestTemplate.setDescription("Some sample description");
		tmpTestTemplate.setMlId(1);
		tmpTestTemplate.setSource("some sample source");
		tmpTestTemplate.setFolderId(testFolder.getId());
		tmpTestTemplate.setChannelInfo(channelId, tmpTestTemplate.getChannelSetId());
		tmpTestTemplate.save();

		// link the template to the folder
		testFolder.setTemplates(Arrays.asList(tmpTestTemplate));
		testFolder.save();

		// create the local page
		Page tmpTestPage = t.createObject(Page.class);
		tmpTestPage.setTemplateId(tmpTestTemplate.getId());
		tmpTestPage.setName("tmpTestPage");
		tmpTestPage.setFolderId(testFolder.getId());
		tmpTestPage.setFilename("tmpTestPage.html");
		tmpTestPage.setChannelInfo(channelId, tmpTestPage.getChannelSetId());
		tmpTestPage.save();

		t.commit(false);

		// Do the push to master
		MultiPushToMasterRequest pushToMasterRequest = new MultiPushToMasterRequest();

		pushToMasterRequest.setChannelId(channelId);
		pushToMasterRequest.setMasterId(MASTER_ID);
		pushToMasterRequest.setForegroundTime(Integer.MAX_VALUE);
		pushToMasterRequest.setTypes(Arrays.asList(Type.page));
		pushToMasterRequest.setIds(Arrays.asList(ObjectTransformer.getInt(testFolder.getId(), -1)));
		pushToMasterRequest.setRecursive(false);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().pushToMaster(pushToMasterRequest);
			assertEquals("Check if the response code is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		t = testContext.startTransactionWithPermissions(true);

		// Check object state in CHANNEL
		t.setChannelId(channelId);

		tmpTestPage = t.getObject(Page.class, tmpTestPage.getId());
		assertFalse("Page using local template must not have been pushed to master", tmpTestPage.isInherited());
	}

	/**
	 * @param type {@link Type} instance that specify which content of a page will be pushed to master node.
	 * @throws Exception if anything goes wrong.
	 */
	private void checkPushToMasterObjectInFolder(Type type) throws Exception {
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
		Template tmpTestTemplate = t.createObject(Template.class);

		tmpTestTemplate.setName("tmpTestTemplate");
		tmpTestTemplate.setDescription("Some sample description");
		tmpTestTemplate.setMlId(1);
		tmpTestTemplate.setSource("some sample source");
		tmpTestTemplate.setFolderId(testFolder.getId());
		tmpTestTemplate.save();
		// link the template to the folder
		testFolder.setTemplates(Arrays.asList(tmpTestTemplate));
		testFolder.save();

		Page tmpTestPage = t.createObject(Page.class);

		tmpTestPage.setTemplateId(C.page_channel_test.new_page_template_id);
		tmpTestPage.setName("tmpTestPage");
		tmpTestPage.setFolderId(testFolder.getId());
		tmpTestPage.setFilename("tmpTestPage.html");
		tmpTestPage.save();
		
		Folder tmpTestFolder = t.createObject(Folder.class);

		tmpTestFolder.setName("tmpTestFolder");
		tmpTestFolder.setMotherId(testFolder.getId());
		tmpTestFolder.setPublishDir("/tmpTestFolder");
		tmpTestFolder.save();
		
		File tmpTestFile = t.createObject(File.class);

		tmpTestFile.setName("tmpTestFile");
		tmpTestFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		tmpTestFile.setFolderId(testFolder.getId());
		tmpTestFile.save();
		
		ImageFile tmpTestImage = t.createObject(ImageFile.class);

		tmpTestImage.setName("tmpTestImage");
		tmpTestImage.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		tmpTestImage.setFolderId(testFolder.getId());
		tmpTestImage.save();
		t.commit(false);
    	
		// Localize objects
		Folder localizedTestFolder = (Folder) testFolder.copy();

		localizedTestFolder.setName("localizedTestFolder");
		localizedTestFolder.setPublishDir("/localizedTestFolder");
		localizedTestFolder.setChannelInfo(channelId, testFolder.getChannelSetId());
		localizedTestFolder.save();
    	
		Template tmpLocalizedTestTemplate = (Template) tmpTestTemplate.copy();

		tmpLocalizedTestTemplate.setName("tmpLocalizedTestTemplate");
		tmpLocalizedTestTemplate.setChannelInfo(channelId, tmpTestTemplate.getChannelSetId());
		tmpLocalizedTestTemplate.save();
		
		Page tmpLocalizedTestPage = (Page) tmpTestPage.copy();

		tmpLocalizedTestPage.setName("tmpLocalizedTestPage");
		tmpLocalizedTestPage.setFilename("tmpLocalizedTestPage.html");
		tmpLocalizedTestPage.setChannelInfo(channelId, tmpTestPage.getChannelSetId());
		tmpLocalizedTestPage.save();
		
		Folder tmpLocalizedTestFolder = (Folder) tmpTestFolder.copy();

		tmpLocalizedTestFolder.setName("tmpLocalizedTestFolder");
		tmpLocalizedTestFolder.setPublishDir("/tmpLocalizedTestFolder");
		tmpLocalizedTestFolder.setChannelInfo(channelId, tmpTestFolder.getChannelSetId());
		tmpLocalizedTestFolder.save();
		
		File tmpLocalizedTestFile = (File) tmpTestFile.copy();

		tmpLocalizedTestFile.setName("tmpLocalizedTestFile.bin");
		tmpLocalizedTestFile.setChannelInfo(channelId, tmpTestFile.getChannelSetId());
		tmpLocalizedTestFile.save();
		
		ImageFile tmpLocalizedTestImage = (ImageFile) tmpTestImage.copy();

		tmpLocalizedTestImage.setName("tmpLocalizedTestImage");
		tmpLocalizedTestImage.setChannelInfo(channelId, tmpTestImage.getChannelSetId());
		tmpLocalizedTestImage.save();
		t.commit(false);
    	
		// Do the push to master
		MultiPushToMasterRequest pushToMasterRequest = new MultiPushToMasterRequest();

		pushToMasterRequest.setChannelId(channelId);
		pushToMasterRequest.setMasterId(MASTER_ID);
		pushToMasterRequest.setForegroundTime(Integer.MAX_VALUE);
		pushToMasterRequest.setTypes(Arrays.asList(type));
		pushToMasterRequest.setIds(Arrays.asList(ObjectTransformer.getInt(localizedTestFolder.getId(), -1)));
		pushToMasterRequest.setRecursive(true);

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().pushToMaster(pushToMasterRequest);
			assertEquals("Check if the response code is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}

		t = testContext.startTransactionWithPermissions(true);
    	
		// Check object state in CHANNEL
		t.setChannelId(channelId);
    	
		tmpTestPage = t.getObject(Page.class, tmpTestPage.getId());
		tmpLocalizedTestPage = t.getObject(Page.class, tmpLocalizedTestPage.getId());
		if (type == Type.page) {
			assertTrue("Master object should be still inherited.", tmpTestPage.isInherited());
			assertNull("Localized object should not exist now", tmpLocalizedTestPage);
		} else {
			assertFalse("Master object should NOT be inherited now.", tmpTestPage.isInherited());
			assertNotNull("Localized object should still exist", tmpLocalizedTestPage);
		}
    	
		tmpTestFile = t.getObject(File.class, tmpTestFile.getId());
		tmpLocalizedTestFile = t.getObject(File.class, tmpLocalizedTestFile.getId());
		if (type == Type.file) {
			assertTrue("Master object should be still inherited.", tmpTestFile.isInherited());
			assertNull("Localized object should not exist now", tmpLocalizedTestFile);
		} else {
			assertFalse("Master object should NOT be inherited now.", tmpTestFile.isInherited());
			assertNotNull("Localized object should still exist", tmpLocalizedTestFile);
		}
    	
		tmpTestTemplate = t.getObject(Template.class, tmpTestTemplate.getId());
		tmpLocalizedTestTemplate = t.getObject(Template.class, tmpLocalizedTestTemplate.getId());
		if (type == Type.template) {
			assertTrue("Master object should be still inherited.", tmpTestTemplate.isInherited());
			assertNull("Localized object should not exist now", tmpLocalizedTestTemplate);
		} else {
			assertFalse("Master object should NOT be inherited now.", tmpTestTemplate.isInherited());
			assertNotNull("Localized object should still exist", tmpLocalizedTestTemplate);
		}
    	
		tmpTestImage = t.getObject(ImageFile.class, tmpTestImage.getId());
		tmpLocalizedTestImage = t.getObject(ImageFile.class, tmpLocalizedTestImage.getId());
		if (type == Type.image) {
			assertTrue("Master object should be still inherited.", tmpTestImage.isInherited());
			assertNull("Localized object should not exist now", tmpLocalizedTestImage);
		} else {
			assertFalse("Master object should NOT be inherited now.", tmpTestImage.isInherited());
			assertNotNull("Localized object should still exist", tmpLocalizedTestImage);
		}
    	
		tmpTestFolder = t.getObject(Folder.class, tmpTestFolder.getId());
		tmpLocalizedTestFolder = t.getObject(Folder.class, tmpLocalizedTestFolder.getId());
		if (type == Type.folder) {
			assertTrue("Master object should be still inherited.", tmpTestFolder.isInherited());
			assertNull("Localized object should not exist now", tmpLocalizedTestFolder);
		} else {
			assertFalse("Master object should NOT be inherited now.", tmpTestFolder.isInherited());
			assertNotNull("Localized object should still exist", tmpLocalizedTestFolder);
		}
    	
		t.resetChannel();
	}
}
