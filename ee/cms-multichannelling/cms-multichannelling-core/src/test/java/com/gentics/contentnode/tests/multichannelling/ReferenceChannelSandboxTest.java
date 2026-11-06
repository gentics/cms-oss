package com.gentics.contentnode.tests.multichannelling;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;

/**
 * Reference Tests for multichannelling support.
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
@RunWith(value = Parameterized.class)
public class ReferenceChannelSandboxTest extends AbstractMultichannellingSandboxTest {
	/**
	 * Inheritance setting for the reference
	 */
	protected Inheritance reference;

	/**
	 * Foreign node
	 */
	protected Node foreignNode;

	/**
	 * Foreign channel
	 */
	protected Node foreignChannel;

	/**
	 * ID of the foreign node
	 */
	protected int foreignNodeId;

	/**
	 * ID of the foreign channel
	 */
	protected int foreignChannelId;

	/**
	 * ID of the root folder of the foreign node
	 */
	protected int foreignFolderId;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: reference {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (Inheritance reference : Inheritance.values()) {
			data.add(new Object[] {reference});
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param reference reference setting
	 */
	public ReferenceChannelSandboxTest(Inheritance reference) {
		this.reference = reference;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// create another node with a channel
		foreignNode = ContentNodeTestDataUtils.createNode("Foreign Node", "foreign.node", "/Content.Node", "/Content.Node", false, false);
		foreignNodeId = ObjectTransformer.getInt(foreignNode.getId(), 0);
		foreignFolderId = ObjectTransformer.getInt(foreignNode.getFolder().getId(), 0);
		foreignChannel = ContentNodeTestDataUtils.createChannel(foreignNode, "Foreign channel", "foreign.channel", "/Content.Node");
		foreignChannelId = ObjectTransformer.getInt(foreignChannel.getId(), 0);

		// link the template to the folder
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder rootFolder = t.getObject(Folder.class, foreignFolderId, true);
		rootFolder.getTemplates().add(t.getObject(Template.class, C.reference_test_data.template1_id));
		rootFolder.save();
		t.commit(false);
	}

	/**
	 * Test page, referencing to localized template.
	 * TODO: make test for using inherited template
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testReferenceTemplate() throws Exception {
		// create a new page over the REST API
		Page testPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);

		// set initial source value to the referencing template
		ReferenceHelper.setTemplateSource(C.reference_test_data.template1_id, "Master");

		// localize the template and change the referencing value
		ReferenceHelper.localizeTemplate(channelId, C.reference_test_data.template1_id, "Localized");

		// compare the source value when rendered in master node and channel node
		ReferenceHelper.renderingTest(testPage, MASTER_ID, channelId, "Master", "Localized");
	}

	/**
	 * Test page, referencing to localized page.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testReferencePage() throws Exception {
		// create a new page over the REST API
		Page testPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);

		// create a new page over the REST API
		Page referencedPage = null;

		switch (reference) {
		case INHERITED:
			referencedPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);
			ReferenceHelper.publishPage(referencedPage);
			break;
		case LOCAL:
			referencedPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id, channelId);
			ReferenceHelper.publishPage(referencedPage);
			break;
		case LOCALIZED:
			referencedPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);
			ReferenceHelper.publishPage(referencedPage);
			ReferenceHelper.localizeAndPublishPage(channelId, referencedPage.getId());
			break;
		case FOREIGNNODE:
			referencedPage = ReferenceHelper.createPage(foreignFolderId, C.reference_test_data.template1_id);
			ReferenceHelper.publishPage(referencedPage);
			break;
		case FOREIGNCHANNEL:
			referencedPage = ReferenceHelper.createPage(foreignFolderId, C.reference_test_data.template1_id, foreignChannelId);
			ReferenceHelper.publishPage(referencedPage);
			break;
		}

		// get test page tags, get the page tag and set the reference
		Tag pageTag = testPage.getTags().get("pageurl");
		Property url = pageTag.getProperties().get("url");

		url.setPageId(referencedPage.getId());
		pageTag.setActive(true);

		// save and publish the test page
		ReferenceHelper.savePage(testPage);
		ReferenceHelper.publishPage(testPage);

		String expectedMaster = null;
		String expectedChannel = null;

		switch (reference) {
		case INHERITED:
			expectedMaster = "Master Template\n/Content.Node/" + referencedPage.getFileName();
			expectedChannel = "Master Template\n/Content.Node/" + referencedPage.getFileName();
			break;
		case LOCAL:
			expectedMaster = "Master Template\n#";
			expectedChannel = "Master Template\n/Content.Node/" + referencedPage.getFileName();
			break;
		case LOCALIZED:
			expectedMaster = "Master Template\n/Content.Node/" + referencedPage.getFileName();
			expectedChannel = "Master Template\n/Content.Node/" + referencedPage.getFileName() + "-localized";
			break;
		case FOREIGNNODE:
			expectedMaster = "Master Template\nhttp://" + foreignNode.getHostname() + "/Content.Node/" + referencedPage.getFileName();
			expectedChannel = "Master Template\nhttp://" + foreignNode.getHostname() + "/Content.Node/" + referencedPage.getFileName();
			break;
		case FOREIGNCHANNEL:
			expectedMaster = "Master Template\n#";
			expectedChannel = "Master Template\n#";
			break;
		}

		// compare the source value when rendered in master node and channel node
		ReferenceHelper.renderingTest(testPage, MASTER_ID, channelId, expectedMaster, expectedChannel);
	}

	/**
	 * Test page, referencing to localized file.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testReferenceFile() throws Exception {
		// create a new page over the REST API
		Page testPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);

		// create a new file over the REST API
		File newFile = null;
		File fileInMaster = null;
		File fileInChannel = null;
		boolean foreign = false;

		switch (reference) {
		case INHERITED:
			newFile = ReferenceHelper.createFile(C.reference_test_data.folder_id);
			fileInMaster = newFile;
			fileInChannel = newFile;
			break;
		case LOCAL:
			newFile = ReferenceHelper.createFile(C.reference_test_data.folder_id, channelId);
			fileInChannel = newFile;
			break;
		case LOCALIZED:
			newFile = ReferenceHelper.createFile(C.reference_test_data.folder_id);
			fileInMaster = newFile;
			fileInChannel = ReferenceHelper.localizeFile(channelId, ObjectTransformer.getInt(newFile.getId(), -1));
			break;
		case FOREIGNNODE:
			newFile = ReferenceHelper.createFile(foreignFolderId);
			fileInMaster = newFile;
			fileInChannel = newFile;
			foreign = true;
			break;
		case FOREIGNCHANNEL:
			newFile = ReferenceHelper.createFile(foreignFolderId, foreignChannelId);
			fileInMaster = null;
			fileInChannel = null;
			break;
		}

		// get test page tags, get the file tag and set the reference
		Tag fileTag = testPage.getTags().get("fileurl");
		Property fileId = fileTag.getProperties().get("file");

		fileId.setFileId(ObjectTransformer.getInteger(newFile.getId(), -1));
		fileTag.setActive(true);

		// save and publish the test page
		ReferenceHelper.savePage(testPage);
		ReferenceHelper.publishPage(testPage);

		String expectedMaster = "Master Template\n";
		if (fileInMaster != null) {
			if (foreign) {
				expectedMaster += "http://" + foreignNode.getHostname();
			}
			expectedMaster += "/Content.Node/" + fileInMaster.getName();
		} else {
			expectedMaster += "#";
		}
		String expectedChannel = "Master Template\n";
		if (fileInChannel != null) {
			if (foreign) {
				expectedChannel += "http://" + foreignNode.getHostname();
			}
			expectedChannel += "/Content.Node/" + fileInChannel.getName();
		} else {
			expectedChannel += "#";
		}

		// compare the source value when rendered in master node and channel node
		ReferenceHelper.renderingTest(testPage, MASTER_ID, channelId, expectedMaster, expectedChannel);
	}

	/**
	 * Test page, referencing to localized image.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testReferenceImage() throws Exception {
		// create a new page over the REST API
		Page testPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);

		// create a new image over the REST API
		ImageFile newImage = null;
		ImageFile imageInMaster = null;
		ImageFile imageInChannel = null;
		boolean foreign = false;

		switch (reference) {
		case INHERITED:
			newImage = ReferenceHelper.createImage(C.reference_test_data.folder_id);
			imageInMaster = newImage;
			imageInChannel = newImage;
			break;
		case LOCAL:
			newImage = ReferenceHelper.createImage(C.reference_test_data.folder_id, channelId);
			imageInChannel = newImage;
			break;
		case LOCALIZED:
			newImage = ReferenceHelper.createImage(C.reference_test_data.folder_id);
			imageInMaster = newImage;
			imageInChannel = ReferenceHelper.localizeImage(channelId, ObjectTransformer.getInt(newImage.getId(), -1));
			break;
		case FOREIGNNODE:
			newImage = ReferenceHelper.createImage(foreignFolderId);
			imageInMaster = newImage;
			imageInChannel = newImage;
			foreign = true;
			break;
		case FOREIGNCHANNEL:
			newImage = ReferenceHelper.createImage(foreignFolderId, foreignChannelId);
			imageInMaster = null;
			imageInChannel = null;
			break;
		}

		// get test page tags, get the image tag and set the reference
		Tag fileTag = testPage.getTags().get("imageurl");
		Property fileId = fileTag.getProperties().get("img");

		fileId.setImageId(ObjectTransformer.getInteger(newImage.getId(), -1));
		fileTag.setActive(true);

		// save and publish the test page
		ReferenceHelper.savePage(testPage);
		ReferenceHelper.publishPage(testPage);

		String expectedMaster = "Master Template\n";
		if (imageInMaster != null) {
			if (foreign) {
				expectedMaster += "http://" + foreignNode.getHostname();
			}
			expectedMaster += "/Content.Node/" + imageInMaster.getName();
		} else {
			expectedMaster += "#";
		}
		String expectedChannel = "Master Template\n";
		if (imageInChannel != null) {
			if (foreign) {
				expectedChannel += "http://" + foreignNode.getHostname();
			}
			expectedChannel += "/Content.Node/" + imageInChannel.getName();
		} else {
			expectedChannel += "#";
		}

		// compare the source value when rendered in master node and channel node
		ReferenceHelper.renderingTest(testPage, MASTER_ID, channelId, expectedMaster, expectedChannel);
	}

	/**
	 * Test page, referencing to localized folder.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testReferenceFolder() throws Exception {
		// create a new page over the REST API
		Page testPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);

		// create a new folder over the REST API
		Folder newFolder = null;
		Folder folderInMaster = null;
		Folder folderInChannel = null;
		boolean foreign = false;

		switch (reference) {
		case INHERITED:
			newFolder = ReferenceHelper.createFolder(C.reference_test_data.folder_id);
			folderInMaster = newFolder;
			folderInChannel = newFolder;
			break;
		case LOCAL:
			newFolder = ReferenceHelper.createFolder(C.reference_test_data.folder_id, channelId);
			folderInChannel = newFolder;
			break;
		case LOCALIZED:
			newFolder = ReferenceHelper.createFolder(C.reference_test_data.folder_id);
			folderInMaster = newFolder;
			folderInChannel = ReferenceHelper.localizeFolder(channelId, ObjectTransformer.getInt(newFolder.getId(), -1));
			break;
		case FOREIGNNODE:
			newFolder = ReferenceHelper.createFolder(foreignFolderId);
			folderInMaster = newFolder;
			folderInChannel = newFolder;
			foreign = true;
			break;
		case FOREIGNCHANNEL:
			newFolder = ReferenceHelper.createFolder(foreignFolderId, foreignChannelId);
			folderInMaster = null;
			folderInChannel = null;
			break;
		}

		// get test page tags, get the folder tag and set the reference
		Tag fileTag = testPage.getTags().get("folderurl");
		Property fileId = fileTag.getProperties().get("folder");

		fileId.setFolderId(ObjectTransformer.getInteger(newFolder.getId(), -1));
		fileTag.setActive(true);

		// save and publish the test page
		ReferenceHelper.savePage(testPage);
		ReferenceHelper.publishPage(testPage);

		String expectedMaster = "Master Template\n";
		if (folderInMaster != null) {
			if (foreign) {
				expectedMaster += "http://" + foreignNode.getHostname();
			}
			expectedMaster += "/Content.Node" + folderInMaster.getPublishDir();
		} else {
			expectedMaster += "#";
		}
		String expectedChannel = "Master Template\n";
		if (folderInChannel != null) {
			if (foreign) {
				expectedChannel += "http://" + foreignNode.getHostname();
			}
			expectedChannel += "/Content.Node" + folderInChannel.getPublishDir();
		} else {
			expectedChannel += "#";
		}

		// compare the source value when rendered in master node and channel node
		ReferenceHelper.renderingTest(testPage, MASTER_ID, channelId, expectedMaster, expectedChannel);
	}

	/**
	 * Test page, referencing to localized template tag.
	 * TODO: make test for using inherited or local template tag
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testReferenceTemplateTag() throws Exception {
		// create a new page over the REST API
		Page testPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);

		// create a test template
		Template newTemplate = TransactionManager.getCurrentTransaction().createObject(Template.class);

		newTemplate.setFolderId(49);
		newTemplate.setName("testTemplate");
		newTemplate.setDescription("Test description");
		newTemplate.setMlId(1);
		newTemplate.setSource("");
		newTemplate.save();

		// create a template tag for the test template - set inital value
		TemplateTag newTemplateTag = TransactionManager.getCurrentTransaction().createObject(TemplateTag.class);

		newTemplateTag.setConstructId(3);
		newTemplateTag.setName("testTag");
		newTemplateTag.setEnabled(true);
		newTemplateTag.getValues().getByKeyname("text").setValueText(C.reference_test_data.page_tag_master_value);
		newTemplateTag.setTemplateId(newTemplate.getId());
		newTemplateTag.save();
		TransactionManager.getCurrentTransaction().commit(false);

		// add the template tag to the test template
		newTemplate.getTemplateTags().put("testTag", newTemplateTag);
		newTemplate.save();

		TransactionManager.getCurrentTransaction().commit(false);

		// get test page tags, get the folder tag and set the reference
		Tag contentTag = testPage.getTags().get("templatetag");
		Property tag = contentTag.getProperties().get("tag");

		tag.setTemplateTagId(ObjectTransformer.getInteger(newTemplateTag.getId(), -1));
		tag.setTemplateId(ObjectTransformer.getInteger(newTemplate.getId(), -1));
		// tag.setStringValue(C.reference_test_data.page_tag_master_value);
		contentTag.setActive(true);

		// save and publish the test page
		ReferenceHelper.savePage(testPage);
		ReferenceHelper.publishPage(testPage);

		// localize test template and change the template tag's value
		Template localizedTemplate = (Template) newTemplate.copy();

		localizedTemplate.setChannelInfo(channelId, newTemplate.getChannelSetId());
		localizedTemplate.getTemplateTag("testTag").getValues().getByKeyname("text").setValueText(C.reference_test_data.page_tag_channel_value);
		localizedTemplate.save();

		TransactionManager.getCurrentTransaction().commit(false);

		// create expected rendering results for master and channel nodes
		String expectedMaster = "Master Template\n" + C.reference_test_data.page_tag_master_value;
		String expectedChannel = "Master Template\n" + C.reference_test_data.page_tag_channel_value;

		// compare the source value when rendered in master node and channel node
		ReferenceHelper.renderingTest(testPage, MASTER_ID, channelId, expectedMaster, expectedChannel);
	}

	/**
	 * Test page, referencing to localized page tag.
	 * TODO: make test for using inherited or local template tag
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testReferenceContentTag() throws Exception {
		// create a new page over the REST API
		Page testPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);

		// create a new page over the REST API
		Page referencedPage = ReferenceHelper.createPage(C.reference_test_data.folder_id, C.reference_test_data.template1_id);

		Tag tag2 = ReferenceHelper.createTag(referencedPage.getId());

		ReferenceHelper.savePage(referencedPage);
		ReferenceHelper.publishPage(referencedPage);

		referencedPage = ReferenceHelper.loadPage(referencedPage.getId());

		// get test page tags, get the folder tag and set the reference
		Tag contentTag = testPage.getTags().get("contenttag");
		Property tag = contentTag.getProperties().get("tag");

		tag.setContentTagId(referencedPage.getTags().get(tag2.getName()).getId());
		tag.setPageId(referencedPage.getId());
		contentTag.setActive(true);

		// save and publish the test page
		ReferenceHelper.savePage(testPage);
		ReferenceHelper.publishPage(testPage);

		ReferenceHelper.localizeAndPublishPageWithChangedContentTag(channelId, ObjectTransformer.getInt(referencedPage.getId(), -1), tag2);

		String expectedMaster = "Master Template\n" + C.reference_test_data.page_tag_master_value;
		String expectedChannel = "Master Template\n" + C.reference_test_data.page_tag_channel_value;

		// compare the source value when rendered in master node and channel node
		ReferenceHelper.renderingTest(testPage, MASTER_ID, channelId, expectedMaster, expectedChannel);
	}

	/**
	 * Possible inheritance settings for the referenced object
	 */
	public static enum Inheritance {
		/**
		 * referenced object is created in the master and inherited in the channel
		 */
		INHERITED,

		/**
		 * referenced object is created in the master and localized in the channel
		 */
		LOCALIZED,

		/**
		 * referenced object is created in the channel
		 */
		LOCAL,

		/**
		 * referenced object is created in another node
		 */
		FOREIGNNODE,

		/**
		 * referenced object is created in another channel
		 */
		FOREIGNCHANNEL
	}
}
