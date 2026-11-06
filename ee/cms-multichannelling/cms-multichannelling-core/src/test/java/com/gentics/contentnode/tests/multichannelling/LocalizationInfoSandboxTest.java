package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
import com.gentics.contentnode.rest.model.response.ChannelLocalizationInfo;
import com.gentics.contentnode.rest.model.response.LocalizationInfo;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCFolderResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for localization info functionality.
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
@RunWith(value = Parameterized.class)
public class LocalizationInfoSandboxTest extends AbstractMultichannellingSandboxTest {
	/**
	 * Flag to mark whether the tests shall be done with a restricted user
	 */
	protected boolean restricted;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: restricted {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (Boolean restricted : Arrays.asList(true, false)) {
			data.add(new Object[] {restricted});
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param restricted true for restricting the user, false if not
	 */
	public LocalizationInfoSandboxTest(boolean restricted) {
		this.restricted = restricted;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.tests.multichannelling.AbstractMultichannellingSandboxTest#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		if (restricted) {
			restrictUserToChannel(DBTestContext.USER_WITH_PERMS, subChannelId);
		}

		testContext.getContext().startTransaction(DBTestContext.USER_WITH_PERMS);
	}

	/**
	 * Test localization info using pages.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testLocalizationInfoWithPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("LocalizationInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create four test objects in the test folder
		Page localizationInfoPage1 = t.createObject(Page.class);

		localizationInfoPage1.setTemplateId(C.page_channel_test.new_page_template_id);
		localizationInfoPage1.setName("localizationInfoPage1");
		localizationInfoPage1.setFolderId(testFolder.getId());
		localizationInfoPage1.setFilename("localizationInfoPage1.html");
		localizationInfoPage1.save();
		
		Page localizationInfoPage2 = t.createObject(Page.class);

		localizationInfoPage2.setTemplateId(C.page_channel_test.new_page_template_id);
		localizationInfoPage2.setName("localizationInfoPage2");
		localizationInfoPage2.setFolderId(testFolder.getId());
		localizationInfoPage2.setFilename("localizationInfoPage2.html");
		localizationInfoPage2.save();
		
		Page localizationInfoPage3 = t.createObject(Page.class);

		localizationInfoPage3.setTemplateId(C.page_channel_test.new_page_template_id);
		localizationInfoPage3.setName("localizationInfoPage3");
		localizationInfoPage3.setFolderId(testFolder.getId());
		localizationInfoPage3.setFilename("localizationInfoPage3.html");
		localizationInfoPage3.save();
		
		Page localizationInfoPage4 = t.createObject(Page.class);

		localizationInfoPage4.setTemplateId(C.page_channel_test.new_page_template_id);
		localizationInfoPage4.setName("localizationInfoPage4");
		localizationInfoPage4.setFolderId(testFolder.getId());
		localizationInfoPage4.setFilename("localizationInfoPage4.html");
		localizationInfoPage4.save();
		
		Folder localizationInfoSubFolder = t.createObject(Folder.class);

		localizationInfoSubFolder.setName("localizationInfoSubFolder");
		localizationInfoSubFolder.setMotherId(testFolder.getId());
		localizationInfoSubFolder.save();
		t.commit(false);
    	
		Page localizationInfoPage5 = t.createObject(Page.class);

		localizationInfoPage5.setTemplateId(C.page_channel_test.new_page_template_id);
		localizationInfoPage5.setName("localizationInfoPage5");
		localizationInfoPage5.setFolderId(localizationInfoSubFolder.getId());
		localizationInfoPage5.setFilename("localizationInfoPage5.html");
		localizationInfoPage5.save();
		t.commit(false);
		
		// Localized object in channel, subchannel and in both
		Page localizationInfoSubPage1Localized = (Page) localizationInfoPage1.copy();

		localizationInfoSubPage1Localized.setChannelInfo(channelId, localizationInfoPage1.getChannelSetId());
		localizationInfoSubPage1Localized.save();
		
		Page localizationInfoSubPage2Localized = (Page) localizationInfoPage2.copy();

		localizationInfoSubPage2Localized.setChannelInfo(subChannelId, localizationInfoPage2.getChannelSetId());
		localizationInfoSubPage2Localized.save();
		
		Page localizationInfoSubPage3Localized = (Page) localizationInfoPage3.copy();

		localizationInfoSubPage3Localized.setChannelInfo(channelId, localizationInfoPage3.getChannelSetId());
		localizationInfoSubPage3Localized.save();
		
		Page localizationInfoSubPage3LocalizedSub = (Page) localizationInfoPage3.copy();

		localizationInfoSubPage3LocalizedSub.setChannelInfo(subChannelId, localizationInfoPage3.getChannelSetId());
		localizationInfoSubPage3LocalizedSub.save();
		t.commit(false);
		
		// Create local objects in channel and subchannel
		Page localizationInfoPageLocal1 = t.createObject(Page.class);

		localizationInfoPageLocal1.setTemplateId(C.page_channel_test.new_page_template_id);
		localizationInfoPageLocal1.setName("localizationInfoPageLocal1");
		localizationInfoPageLocal1.setFolderId(testFolder.getId());
		localizationInfoPageLocal1.setFilename("localizationInfoPageLocal1.html");
		localizationInfoPageLocal1.setChannelInfo(channelId, localizationInfoPageLocal1.getChannelSetId());
		localizationInfoPageLocal1.save();
		
		Page localizationInfoPageLocal2 = t.createObject(Page.class);

		localizationInfoPageLocal2.setTemplateId(C.page_channel_test.new_page_template_id);
		localizationInfoPageLocal2.setName("localizationInfoPageLocal1");
		localizationInfoPageLocal2.setFolderId(testFolder.getId());
		localizationInfoPageLocal2.setFilename("localizationInfoPageLocal1.html");
		localizationInfoPageLocal2.setChannelInfo(subChannelId, localizationInfoPageLocal2.getChannelSetId());
		localizationInfoPageLocal2.save();
		t.commit(false);
		
		// Get localization info for the test folder in master
		LocalizationInfo localizationInfo = null;
		try (Trx trx = new Trx(user)) {
			// Get localization info - NOT recursive
			localizationInfo = new MCFolderResourceImpl().getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, false);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}
		
		int nodeCheckCounter = 0;

		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 4, channelLocalizationInfo.getPage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getPage().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getPage().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getPage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 2, channelLocalizationInfo.getPage().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getPage().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getPage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 3, channelLocalizationInfo.getPage().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getPage().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
		
		// Get localization info - RECURSIVE
		try (Trx trx = new Trx(user)) {
			localizationInfo = new MCFolderResourceImpl().getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, true);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}

		nodeCheckCounter = 0;
		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 5, channelLocalizationInfo.getPage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getPage().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getPage().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getPage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 3, channelLocalizationInfo.getPage().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getPage().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getPage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 4, channelLocalizationInfo.getPage().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getPage().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
	}
	
	/**
	 * Test localization info using images.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testLocalizationInfoWithImage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("LocalizationInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create four test objects in the test folder
		ImageFile localizationInfoImage1 = t.createObject(ImageFile.class);

		localizationInfoImage1.setName("localizationInfoImage1");
		localizationInfoImage1.setFolderId(testFolder.getId());
		localizationInfoImage1.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		localizationInfoImage1.save();
		
		ImageFile localizationInfoImage2 = t.createObject(ImageFile.class);

		localizationInfoImage2.setName("localizationInfoImage2");
		localizationInfoImage2.setFolderId(testFolder.getId());
		localizationInfoImage2.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		localizationInfoImage2.save();
		
		ImageFile localizationInfoImage3 = t.createObject(ImageFile.class);

		localizationInfoImage3.setName("localizationInfoImage3");
		localizationInfoImage3.setFolderId(testFolder.getId());
		localizationInfoImage3.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		localizationInfoImage3.save();
		
		ImageFile localizationInfoImage4 = t.createObject(ImageFile.class);

		localizationInfoImage4.setName("localizationInfoImage4");
		localizationInfoImage4.setFolderId(testFolder.getId());
		localizationInfoImage4.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		localizationInfoImage4.save();

		Folder localizationInfoTestFolder = t.createObject(Folder.class);

		localizationInfoTestFolder.setName("localizationInfoTestFolder");
		localizationInfoTestFolder.setMotherId(testFolder.getId());
		localizationInfoTestFolder.save();
		t.commit(false);
		
		ImageFile localizationInfoImage5 = t.createObject(ImageFile.class);

		localizationInfoImage5.setName("localizationInfoImage5");
		localizationInfoImage5.setFolderId(localizationInfoTestFolder.getId());
		localizationInfoImage5.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		localizationInfoImage5.save();
		t.commit(false);
		
		// Localized object in channel, subchannel and in both
		ImageFile localizationInfoSubImage1Localized = (ImageFile) localizationInfoImage1.copy();

		localizationInfoSubImage1Localized.setChannelInfo(channelId, localizationInfoImage1.getChannelSetId());
		localizationInfoSubImage1Localized.save();
		
		ImageFile localizationInfoSubImage2Localized = (ImageFile) localizationInfoImage2.copy();

		localizationInfoSubImage2Localized.setChannelInfo(subChannelId, localizationInfoImage2.getChannelSetId());
		localizationInfoSubImage2Localized.save();
		
		ImageFile localizationInfoSubImage3Localized = (ImageFile) localizationInfoImage3.copy();

		localizationInfoSubImage3Localized.setChannelInfo(channelId, localizationInfoImage3.getChannelSetId());
		localizationInfoSubImage3Localized.save();
		
		ImageFile localizationInfoSubImage3LocalizedSub = (ImageFile) localizationInfoImage3.copy();

		localizationInfoSubImage3LocalizedSub.setChannelInfo(subChannelId, localizationInfoImage3.getChannelSetId());
		localizationInfoSubImage3LocalizedSub.save();
		t.commit(false);
		
		// Create local objects in channel and subchannel
		ImageFile localizationInfoImageLocal1 = t.createObject(ImageFile.class);

		localizationInfoImageLocal1.setName("localizationInfoImageLocal1");
		localizationInfoImageLocal1.setFolderId(testFolder.getId());
		localizationInfoImageLocal1.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		localizationInfoImageLocal1.setChannelInfo(channelId, localizationInfoImageLocal1.getChannelSetId());
		localizationInfoImageLocal1.save();
		
		ImageFile localizationInfoImageLocal2 = t.createObject(ImageFile.class);

		localizationInfoImageLocal2.setName("localizationInfoImageLocal2");
		localizationInfoImageLocal2.setFolderId(testFolder.getId());
		localizationInfoImageLocal2.setFileStream(new ByteArrayInputStream(C.channel_sync.image_data_jpg));
		localizationInfoImageLocal2.setChannelInfo(subChannelId, localizationInfoImageLocal2.getChannelSetId());
		localizationInfoImageLocal2.save();
		t.commit(false);
		
		// Get localization info for the test folder in master
		LocalizationInfo localizationInfo = null;
		try (Trx trx = new Trx(user)) {
			MCFolderResourceImpl folderResource = new MCFolderResourceImpl();
			// Get localization info - NOT recursive
			localizationInfo = folderResource.getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, false);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}

		int nodeCheckCounter = 0;

		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 4, channelLocalizationInfo.getImage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getImage().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getImage().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getImage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 2, channelLocalizationInfo.getImage().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getImage().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getImage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 3, channelLocalizationInfo.getImage().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getImage().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
		
		// Get localization info - RECURSIVE
		try (Trx trx = new Trx(user)) {
			localizationInfo = new MCFolderResourceImpl().getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, true);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}

		nodeCheckCounter = 0;
		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 5, channelLocalizationInfo.getImage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getImage().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getImage().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getImage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 3, channelLocalizationInfo.getImage().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getImage().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getImage().getLocal());
				assertEquals("Check if the inherited object count is as expected", 4, channelLocalizationInfo.getImage().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getImage().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
	}
	
	/**
	 * Test localization info using files.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testLocalizationInfoWithFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("LocalizationInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create four test objects in the test folder
		File localizationInfoFile1 = t.createObject(File.class);

		localizationInfoFile1.setName("localizationInfoFile1");
		localizationInfoFile1.setFolderId(testFolder.getId());
		localizationInfoFile1.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		localizationInfoFile1.save();
		
		File localizationInfoFile2 = t.createObject(File.class);

		localizationInfoFile2.setName("localizationInfoFile2");
		localizationInfoFile2.setFolderId(testFolder.getId());
		localizationInfoFile2.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		localizationInfoFile2.save();
		
		File localizationInfoFile3 = t.createObject(File.class);

		localizationInfoFile3.setName("localizationInfoFile3");
		localizationInfoFile3.setFolderId(testFolder.getId());
		localizationInfoFile3.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		localizationInfoFile3.save();
		
		File localizationInfoFile4 = t.createObject(File.class);

		localizationInfoFile4.setName("localizationInfoFile4");
		localizationInfoFile4.setFolderId(testFolder.getId());
		localizationInfoFile4.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		localizationInfoFile4.save();

		Folder localizationInfoTestFolder = t.createObject(Folder.class);

		localizationInfoTestFolder.setName("localizationInfoTestFolder");
		localizationInfoTestFolder.setMotherId(testFolder.getId());
		localizationInfoTestFolder.save();
		t.commit(false);
		
		File localizationInfoFile5 = t.createObject(File.class);

		localizationInfoFile5.setName("localizationInfoFile5");
		localizationInfoFile5.setFolderId(localizationInfoTestFolder.getId());
		localizationInfoFile5.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		localizationInfoFile5.save();
		t.commit(false);
		
		// Localized object in channel, subchannel and in both
		File localizationInfoSubFile1Localized = (File) localizationInfoFile1.copy();

		localizationInfoSubFile1Localized.setChannelInfo(channelId, localizationInfoFile1.getChannelSetId());
		localizationInfoSubFile1Localized.save();
		
		File localizationInfoSubFile2Localized = (File) localizationInfoFile2.copy();

		localizationInfoSubFile2Localized.setChannelInfo(subChannelId, localizationInfoFile2.getChannelSetId());
		localizationInfoSubFile2Localized.save();
		
		File localizationInfoSubFile3Localized = (File) localizationInfoFile3.copy();

		localizationInfoSubFile3Localized.setChannelInfo(channelId, localizationInfoFile3.getChannelSetId());
		localizationInfoSubFile3Localized.save();
		
		File localizationInfoSubFile3LocalizedSub = (File) localizationInfoFile3.copy();

		localizationInfoSubFile3LocalizedSub.setChannelInfo(subChannelId, localizationInfoFile3.getChannelSetId());
		localizationInfoSubFile3LocalizedSub.save();
		t.commit(false);
		
		// Create local objects in channel and subchannel
		File localizationInfoFileLocal1 = t.createObject(File.class);

		localizationInfoFileLocal1.setName("localizationInfoFileLocal1");
		localizationInfoFileLocal1.setFolderId(testFolder.getId());
		localizationInfoFileLocal1.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		localizationInfoFileLocal1.setChannelInfo(channelId, localizationInfoFileLocal1.getChannelSetId());
		localizationInfoFileLocal1.save();
		
		File localizationInfoFileLocal2 = t.createObject(File.class);

		localizationInfoFileLocal2.setName("localizationInfoFileLocal2");
		localizationInfoFileLocal2.setFolderId(testFolder.getId());
		localizationInfoFileLocal2.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		localizationInfoFileLocal2.setChannelInfo(subChannelId, localizationInfoFileLocal2.getChannelSetId());
		localizationInfoFileLocal2.save();
		t.commit(false);
		
		// Get localization info for the test folder in master
		LocalizationInfo localizationInfo = null;
		try (Trx trx = new Trx(user)) {
			// Get localization info - NOT recursive
			localizationInfo = new MCFolderResourceImpl().getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, false);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}

		int nodeCheckCounter = 0;

		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 4, channelLocalizationInfo.getFile().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getFile().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getFile().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getFile().getLocal());
				assertEquals("Check if the inherited object count is as expected", 2, channelLocalizationInfo.getFile().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getFile().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getFile().getLocal());
				assertEquals("Check if the inherited object count is as expected", 3, channelLocalizationInfo.getFile().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getFile().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
		
		// Get localization info - RECURSIVE
		try (Trx trx = new Trx(user)) {
			localizationInfo = new MCFolderResourceImpl().getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, true);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}

		nodeCheckCounter = 0;
		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 5, channelLocalizationInfo.getFile().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getFile().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getFile().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getFile().getLocal());
				assertEquals("Check if the inherited object count is as expected", 3, channelLocalizationInfo.getFile().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getFile().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getFile().getLocal());
				assertEquals("Check if the inherited object count is as expected", 4, channelLocalizationInfo.getFile().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getFile().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
	}
	
	/**
	 * Test localization info using templates.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testLocalizationInfoWithTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("LocalizationInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create four test objects in the test folder
		Template localizationInfoTempalte1 = t.createObject(Template.class);

		localizationInfoTempalte1.setName("localizationInfoTempalte1");
		localizationInfoTempalte1.setFolderId(testFolder.getId());
		localizationInfoTempalte1.setDescription("Test description");
		localizationInfoTempalte1.setMlId(1);
		localizationInfoTempalte1.setSource("test");
		localizationInfoTempalte1.save();
		
		Template localizationInfoTempalte2 = t.createObject(Template.class);

		localizationInfoTempalte2.setName("localizationInfoTempalte2");
		localizationInfoTempalte2.setFolderId(testFolder.getId());
		localizationInfoTempalte2.setDescription("Test description");
		localizationInfoTempalte2.setMlId(1);
		localizationInfoTempalte2.setSource("test");
		localizationInfoTempalte2.save();
		
		Template localizationInfoTempalte3 = t.createObject(Template.class);

		localizationInfoTempalte3.setName("localizationInfoTempalte3");
		localizationInfoTempalte3.setFolderId(testFolder.getId());
		localizationInfoTempalte3.setDescription("Test description");
		localizationInfoTempalte3.setMlId(1);
		localizationInfoTempalte3.setSource("test");
		localizationInfoTempalte3.save();
		
		Template localizationInfoTempalte4 = t.createObject(Template.class);

		localizationInfoTempalte4.setName("localizationInfoTempalte4");
		localizationInfoTempalte4.setFolderId(testFolder.getId());
		localizationInfoTempalte4.setDescription("Test description");
		localizationInfoTempalte4.setMlId(1);
		localizationInfoTempalte4.setSource("test");
		localizationInfoTempalte4.save();
		
		testFolder.setTemplates(Arrays.asList(localizationInfoTempalte1, localizationInfoTempalte2, localizationInfoTempalte3, localizationInfoTempalte4));
		testFolder.save();
		t.commit(false);
		
		Folder localizationInfoTestFolder = t.createObject(Folder.class);

		localizationInfoTestFolder.setName("localizationInfoTestFolder");
		localizationInfoTestFolder.setMotherId(testFolder.getId());
		localizationInfoTestFolder.save();
		t.commit(false);
		
		Template localizationInfoTempalte5 = t.createObject(Template.class);

		localizationInfoTempalte5.setName("localizationInfoTempalte5");
		localizationInfoTempalte5.setFolderId(localizationInfoTestFolder.getId());
		localizationInfoTempalte5.setDescription("Test description");
		localizationInfoTempalte5.setMlId(1);
		localizationInfoTempalte5.setSource("test");
		localizationInfoTempalte5.save();
		
		localizationInfoTestFolder.setTemplates(Arrays.asList(localizationInfoTempalte5));
		localizationInfoTestFolder.save();
		t.commit(false);
		
		// Localized object in channel, subchannel and in both
		Template localizationInfoSubTemplate1Localized = (Template) localizationInfoTempalte1.copy();

		localizationInfoSubTemplate1Localized.setChannelInfo(channelId, localizationInfoTempalte1.getChannelSetId());
		localizationInfoSubTemplate1Localized.save();
		
		Template localizationInfoSubTemplate2Localized = (Template) localizationInfoTempalte2.copy();

		localizationInfoSubTemplate2Localized.setChannelInfo(subChannelId, localizationInfoTempalte2.getChannelSetId());
		localizationInfoSubTemplate2Localized.save();
		
		Template localizationInfoSubTemplate3Localized = (Template) localizationInfoTempalte3.copy();

		localizationInfoSubTemplate3Localized.setChannelInfo(channelId, localizationInfoTempalte3.getChannelSetId());
		localizationInfoSubTemplate3Localized.save();
		
		Template localizationInfoSubTemplate3LocalizedSub = (Template) localizationInfoTempalte3.copy();

		localizationInfoSubTemplate3LocalizedSub.setChannelInfo(subChannelId, localizationInfoTempalte3.getChannelSetId());
		localizationInfoSubTemplate3LocalizedSub.save();
		t.commit(false);
		
		// Create local objects in channel and subchannel
		Template localizationInfoTemplateLocal1 = t.createObject(Template.class);

		localizationInfoTemplateLocal1.setName("localizationInfoTemplateLocal1");
		localizationInfoTemplateLocal1.setFolderId(testFolder.getId());
		localizationInfoTemplateLocal1.setDescription("Test description");
		localizationInfoTemplateLocal1.setMlId(1);
		localizationInfoTemplateLocal1.setSource("test");
		localizationInfoTemplateLocal1.setChannelInfo(channelId, localizationInfoTemplateLocal1.getChannelSetId());
		localizationInfoTemplateLocal1.save();
		
		Template localizationInfoTemplateLocal2 = t.createObject(Template.class);

		localizationInfoTemplateLocal2.setName("localizationInfoTemplateLocal2");
		localizationInfoTemplateLocal2.setFolderId(testFolder.getId());
		localizationInfoTemplateLocal2.setDescription("Test description");
		localizationInfoTemplateLocal2.setMlId(1);
		localizationInfoTemplateLocal2.setSource("test");
		localizationInfoTemplateLocal2.setChannelInfo(subChannelId, localizationInfoTemplateLocal2.getChannelSetId());
		localizationInfoTemplateLocal2.save();
		
		t.commit(false);
		
		// Get localization info for the test folder in master
		LocalizationInfo localizationInfo = null;
		try (Trx trx = new Trx(user)) {
			// Get localization info - NOT recursive
			localizationInfo = new MCFolderResourceImpl().getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, false);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}

		int nodeCheckCounter = 0;

		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 4, channelLocalizationInfo.getTemplate().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getTemplate().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getTemplate().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getTemplate().getLocal());
				assertEquals("Check if the inherited object count is as expected", 2, channelLocalizationInfo.getTemplate().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getTemplate().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getTemplate().getLocal());
				assertEquals("Check if the inherited object count is as expected", 3, channelLocalizationInfo.getTemplate().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getTemplate().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
		
		// Get localization info - RECURSIVE
		try (Trx trx = new Trx(user)) {
			localizationInfo = new MCFolderResourceImpl().getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, true);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}

		nodeCheckCounter = 0;
		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 5, channelLocalizationInfo.getTemplate().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getTemplate().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getTemplate().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getTemplate().getLocal());
				assertEquals("Check if the inherited object count is as expected", 3, channelLocalizationInfo.getTemplate().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getTemplate().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getTemplate().getLocal());
				assertEquals("Check if the inherited object count is as expected", 4, channelLocalizationInfo.getTemplate().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getTemplate().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
	}
	
	/**
	 * Test localization info using folders.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testLocalizationInfoWithFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName("LocalizationInfoTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Create four test objects in the test folder
		Folder localizationInfoFolder1 = t.createObject(Folder.class);

		localizationInfoFolder1.setName("localizationInfoFolder1");
		localizationInfoFolder1.setMotherId(testFolder.getId());
		localizationInfoFolder1.save();
		
		Folder localizationInfoFolder2 = t.createObject(Folder.class);

		localizationInfoFolder2.setName("localizationInfoFolder2");
		localizationInfoFolder2.setMotherId(testFolder.getId());
		localizationInfoFolder2.save();
		
		Folder localizationInfoFolder3 = t.createObject(Folder.class);

		localizationInfoFolder3.setName("localizationInfoFolder3");
		localizationInfoFolder3.setMotherId(testFolder.getId());
		localizationInfoFolder3.save();
		
		Folder localizationInfoFolder4 = t.createObject(Folder.class);

		localizationInfoFolder4.setName("localizationInfoFolder4");
		localizationInfoFolder4.setMotherId(testFolder.getId());
		localizationInfoFolder4.save();
		t.commit(false);
		
		Folder localizationInfoFolder5 = t.createObject(Folder.class);

		localizationInfoFolder5.setName("localizationInfoFolder5");
		localizationInfoFolder5.setMotherId(localizationInfoFolder4.getId());
		localizationInfoFolder5.save();
		t.commit(false);
		
		// Localized object in channel, subchannel and in both
		Folder localizationInfoSubFolder1Localized = (Folder) localizationInfoFolder1.copy();

		localizationInfoSubFolder1Localized.setChannelInfo(channelId, localizationInfoFolder1.getChannelSetId());
		localizationInfoSubFolder1Localized.save();
		
		Folder localizationInfoSubFolder2Localized = (Folder) localizationInfoFolder2.copy();

		localizationInfoSubFolder2Localized.setChannelInfo(subChannelId, localizationInfoFolder2.getChannelSetId());
		localizationInfoSubFolder2Localized.save();
		
		Folder localizationInfoSubFolder3Localized = (Folder) localizationInfoFolder3.copy();

		localizationInfoSubFolder3Localized.setChannelInfo(channelId, localizationInfoFolder3.getChannelSetId());
		localizationInfoSubFolder3Localized.save();
		
		Folder localizationInfoSubFolder3LocalizedSub = (Folder) localizationInfoFolder3.copy();

		localizationInfoSubFolder3LocalizedSub.setChannelInfo(subChannelId, localizationInfoFolder3.getChannelSetId());
		localizationInfoSubFolder3LocalizedSub.save();
		t.commit(false);
		
		// Create local objects in channel and subchannel
		Folder localizationInfoFolderLocal1 = t.createObject(Folder.class);

		localizationInfoFolderLocal1.setName("localizationInfoFolderLocal1");
		localizationInfoFolderLocal1.setMotherId(testFolder.getId());
		localizationInfoFolderLocal1.setChannelInfo(channelId, localizationInfoFolderLocal1.getChannelSetId());
		localizationInfoFolderLocal1.save();
		
		Folder localizationInfoFolderLocal2 = t.createObject(Folder.class);

		localizationInfoFolderLocal2.setName("localizationInfoFolderLocal2");
		localizationInfoFolderLocal2.setMotherId(testFolder.getId());
		localizationInfoFolderLocal2.setChannelInfo(subChannelId, localizationInfoFolderLocal2.getChannelSetId());
		localizationInfoFolderLocal2.save();
		
		t.commit(false);
		
		// Get localization info for the test folder
		LocalizationInfo localizationInfo = null;
		try (Trx trx = new Trx(user)) {
			// Get localization info - NOT recursive
			localizationInfo = new MCFolderResourceImpl().getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, false);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}

		int nodeCheckCounter = 0;

		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 5, channelLocalizationInfo.getFolder().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getFolder().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getFolder().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getFolder().getLocal());
				assertEquals("Check if the inherited object count is as expected", 3, channelLocalizationInfo.getFolder().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getFolder().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getFolder().getLocal());
				assertEquals("Check if the inherited object count is as expected", 4, channelLocalizationInfo.getFolder().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getFolder().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
		
		// Get localization info - RECURSIVE
		try (Trx trx = new Trx(user)) {
			localizationInfo = new MCFolderResourceImpl().getLocalizationInfo(ObjectTransformer.getString(testFolder.getId(), null), subChannelId, true);
			assertEquals("Check if the response is OK", ResponseCode.OK, localizationInfo.getResponseInfo().getResponseCode());
			trx.success();
		}

		nodeCheckCounter = 0;
		for (ChannelLocalizationInfo channelLocalizationInfo : localizationInfo.getChannels()) {
			if (channelLocalizationInfo.getChannelId() == 0) {
				assertEquals("Check if the local object count is as expected", 6, channelLocalizationInfo.getFolder().getLocal());
				assertEquals("Check if the inherited object count is as expected", 0, channelLocalizationInfo.getFolder().getInherited());
				assertEquals("Check if the localized object count is as expected", 0, channelLocalizationInfo.getFolder().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == channelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getFolder().getLocal());
				assertEquals("Check if the inherited object count is as expected", 4, channelLocalizationInfo.getFolder().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getFolder().getLocalized());
				nodeCheckCounter++;
			} else if (channelLocalizationInfo.getChannelId() == subChannelId) {
				assertEquals("Check if the local object count is as expected", 1, channelLocalizationInfo.getFolder().getLocal());
				assertEquals("Check if the inherited object count is as expected", 5, channelLocalizationInfo.getFolder().getInherited());
				assertEquals("Check if the localized object count is as expected", 2, channelLocalizationInfo.getFolder().getLocalized());
				nodeCheckCounter++;
			}
		}
		
		assertEquals("Test if we checked all nodes and channels.", 3, nodeCheckCounter);
	}
}
