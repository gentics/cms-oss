package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.request.ContentTagCreateRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.TagCreateResponse;
import com.gentics.contentnode.tests.utils.OverviewHelper;

/**
 * This class implements logic, frequently used by all overview related tests.
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
final class ReferenceHelper {

	private ReferenceHelper() {}
	
	/**
	 * Localize and publish page.
	 * 
	 * @param channelId The channel ID we want to localize the page for.
	 * @param pageId ID of the page we want to localize.
	 * @return The localized {@link Page} instance.
	 * @throws Exception if something goes wrong.
	 */
	public static Page localizeAndPublishPage(int channelId, int pageId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		com.gentics.contentnode.object.Page page = t.getObject(com.gentics.contentnode.object.Page.class, pageId);
		com.gentics.contentnode.object.Page localCopyPage = (com.gentics.contentnode.object.Page) page.copy();

		localCopyPage.setFilename(localCopyPage.getFilename() + "-localized");
		localCopyPage.setChannelInfo(channelId, page.getChannelSetId());
		localCopyPage.save();
		localCopyPage.publish();
		t.commit(false);
        
		return localCopyPage;
	}
	
	/**
	 * Localize and publish a page. Also change the content tag value of the page.
	 * 
	 * @param channelId ID of the channel node.
	 * @param pageId ID of the page.
	 * @param tag tag
	 * @return The localized copy of the page.
	 * @throws Exception if anything goes wrong.
	 */
	public static Page localizeAndPublishPageWithChangedContentTag(int channelId, int pageId, com.gentics.contentnode.rest.model.Tag tag) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		com.gentics.contentnode.object.Page page = t.getObject(com.gentics.contentnode.object.Page.class, pageId);
		com.gentics.contentnode.object.Page localCopyPage = (com.gentics.contentnode.object.Page) page.copy();
        
		localCopyPage.setChannelInfo(channelId, page.getChannelSetId());
		localCopyPage.save();
		localCopyPage.publish();
		t.commit(false);
        
		com.gentics.contentnode.rest.model.Page restLocalizedPage = ReferenceHelper.loadPage(ObjectTransformer.getInt(localCopyPage.getId(), -1));

		restLocalizedPage.getTags().get(tag.getName()).getProperties().get("text").setStringValue(C.reference_test_data.page_tag_channel_value);
        
		savePage(restLocalizedPage);
        
		return localCopyPage;
	}
	
	/**
	 * Save {@link com.gentics.contentnode.rest.model.Page}.
	 * 
	 * @param page The {@link com.gentics.contentnode.rest.model.Page} instance we want to save.
	 * @throws Exception if something goes wrong.
	 */
	public static void savePage(com.gentics.contentnode.rest.model.Page page) throws Exception {
		PageSaveRequest saveRequest = new PageSaveRequest();

		saveRequest.setPage(page);
		saveRequest.setUnlock(true);
		GenericResponse saveResponse = getPageResource().save(Integer.toString(page.getId()), saveRequest);

		assertEquals("Check the response code", ResponseCode.OK, saveResponse.getResponseInfo().getResponseCode());
	}
	
	/**
	 * Load page, using REST interface.
	 * 
	 * @param pageId ID of the page
	 * @return The loaded {@link com.gentics.contentnode.rest.model.Page} instance.
	 * @throws Exception if anything goes wrong.
	 */
	public static com.gentics.contentnode.rest.model.Page loadPage(int pageId) throws Exception {
		PageLoadResponse pageLoadResponse = getPageResource().load(Integer.toString(pageId), false, false, false, false, false, false, false, false, false, false, null, null);

		assertEquals("Check the response code", ResponseCode.OK, pageLoadResponse.getResponseInfo().getResponseCode());

		return pageLoadResponse.getPage();
	}
	
	/**
	 * Publish a page.
	 * 
	 * @param page The {@link com.gentics.contentnode.rest.model.Page} instance we want to publish.
	 * @throws Exception if anything goes wrong.
	 */
	public static void publishPage(com.gentics.contentnode.rest.model.Page page) throws Exception {
		GenericResponse publishResponse = getPageResource().publish(Integer.toString(page.getId()), null, new PagePublishRequest());

		assertEquals("Check the response code", ResponseCode.OK, publishResponse.getResponseInfo().getResponseCode());
	}
	
	/**
	 * Set template source content.
	 * 
	 * @param template_id ID of the template
	 * @param initalSource the source content
	 * @return the edited {@link Template} instance.
	 * @throws Exception if anything goes wrong.
	 */
	public static Template setTemplateSource(int template_id, String initalSource) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template normalTemplate = t.getObject(Template.class, template_id, true);

		normalTemplate.setSource(initalSource);
		normalTemplate.save();
		t.commit(false);
        
		return normalTemplate;
	}
	
	/**
	 * Localize template.
	 * 
	 * @param channelId The channel ID we want to localize the file for.
	 * @param templateId ID of the file we want to localize.
	 * @return The localized {@link File} instance.
	 * @throws Exception if something goes wrong.
	 */
	public static Template localizeTemplate(int channelId, int templateId, String source) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Template template = t.getObject(Template.class, templateId);
		Template localCopyTemplate = (Template) template.copy();

		localCopyTemplate.setSource(source);
		localCopyTemplate.setChannelInfo(channelId, template.getChannelSetId());
		localCopyTemplate.save();
		t.commit(false);
        
		return localCopyTemplate;
	}

	/**
	 * Create new {@link com.gentics.contentnode.rest.model.Page}.
	 * 
	 * @param folderId ID of the folder where the new page will be created.
	 * @param templateId ID of the template the new page will have.
	 * @return The created {@link com.gentics.contentnode.rest.model.Page} instance.
	 * @throws Exception if something goes wrong.
	 */
	public static com.gentics.contentnode.rest.model.Page createPage(int folderId, int templateId) throws Exception {
		return createPage(folderId, templateId, 0);
	}

	/**
	 * Create new {@link com.gentics.contentnode.rest.model.Page}.
	 * 
	 * @param folderId ID of the folder where the new page will be created.
	 * @param templateId ID of the template the new page will have.
	 * @param nodeId ID of the node or 0
	 * @return The created {@link com.gentics.contentnode.rest.model.Page} instance.
	 * @throws Exception if something goes wrong.
	 */
	public static com.gentics.contentnode.rest.model.Page createPage(int folderId, int templateId, int nodeId) throws Exception {
		PageCreateRequest createPageRequest = new PageCreateRequest();

		if (nodeId != 0) {
			createPageRequest.setNodeId(nodeId);
		}
		createPageRequest.setFolderId(Integer.toString(folderId));
		createPageRequest.setTemplateId(templateId);
		PageLoadResponse createPageResponse = getPageResource().create(createPageRequest);

		assertEquals("Check the reponse code", ResponseCode.OK, createPageResponse.getResponseInfo().getResponseCode());

		// get the test page
		com.gentics.contentnode.rest.model.Page testPage = createPageResponse.getPage();

		return testPage;
	}
	
	/**
	 * Create a tag in the page with preset value.
	 * 
	 * @param pageId ID of the page.
	 * @return The created {@link Tag} instance.
	 * @throws Exception if anything goes wrong.
	 */
	public static com.gentics.contentnode.rest.model.Tag createTag(int pageId) throws Exception {
		ContentTagCreateRequest tagCreateRequest = new ContentTagCreateRequest();

		tagCreateRequest.setMagicValue(C.reference_test_data.page_tag_master_value);

		TagCreateResponse tagCreateResponse = getPageResource().createTag(Integer.toString(pageId), 3, null, tagCreateRequest);

		assertEquals("Check the reponse code", ResponseCode.OK, tagCreateResponse.getResponseInfo().getResponseCode());

		return tagCreateResponse.getTag();
	}
	
	/**
	 * Localize file. Set the name to "channel.bin".
	 * 
	 * @param channelId The channel ID we want to localize the file for.
	 * @param fileId ID of the file we want to localize.
	 * @return The localized {@link File} instance.
	 * @throws Exception if something goes wrong.
	 */
	public static File localizeFile(int channelId, int fileId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		File testFile = t.getObject(com.gentics.contentnode.object.File.class, fileId);
		File localTestFile = (File) testFile.copy();

		localTestFile.setName("channel.bin");
		localTestFile.setChannelInfo(channelId, testFile.getChannelSetId());
		localTestFile.save();
		t.commit(false);
        
		return localTestFile;
	}

	/**
	 * Create file with name "master.bin".
	 * 
	 * @param folderId The parent folder id.
	 * @return the created {@link File} instance.
	 * @throws Exception if anything goes wrong.
	 */
	public static File createFile(int folderId) throws Exception {
		return createFile(folderId, 0);
	}

	/**
	 * Create file with name "master.bin".
	 * 
	 * @param folderId The parent folder id.
	 * @param nodeId ID of the node or 0
	 * @return the created {@link File} instance.
	 * @throws Exception if anything goes wrong.
	 */
	public static File createFile(int folderId, int nodeId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		File newFile = t.createObject(File.class);

		newFile.setName("master.bin");
		newFile.setFolderId(folderId);
		newFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
		if (nodeId != 0) {
			newFile.setChannelInfo(nodeId, newFile.getChannelSetId());
		}
		newFile.save();
		t.commit(false);
        
		return newFile;
	}
	
	/**
	 * Localize image.
	 * 
	 * @param channelId The channel ID we want to localize the image for.
	 * @param imageId ID of the image.
	 * @return The localized {@link ImageFile} instance.
	 * @throws Exception if something goes wrong.
	 */
	public static ImageFile localizeImage(int channelId, int imageId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		com.gentics.contentnode.object.ImageFile image = t.getObject(com.gentics.contentnode.object.ImageFile.class, imageId);
		com.gentics.contentnode.object.ImageFile localCopyImage = (com.gentics.contentnode.object.ImageFile) image.copy();
        
		localCopyImage.setName("channel.jpg");
		localCopyImage.setChannelInfo(channelId, image.getChannelSetId());
		localCopyImage.save();
		t.commit(false);
        
		return localCopyImage;
	}

	/**
	 * Create image with name "master.jpg".
	 * 
	 * @param folderId The parent folder id.
	 * @return the created {@link ImageFile} instance.
	 * @throws Exception if anything goes wrong.
	 */
	public static ImageFile createImage(int folderId) throws Exception {
		return createImage(folderId, 0);
	}

	/**
	 * Create image with name "master.jpg".
	 * 
	 * @param folderId The parent folder id.
	 * @param nodeId ID of the node or 0
	 * @return the created {@link ImageFile} instance.
	 * @throws Exception if anything goes wrong.
	 */
	public static ImageFile createImage(int folderId, int nodeId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		ImageFile newImage = t.createObject(ImageFile.class);

		newImage.setName("master.jpg");
		newImage.setFolderId(folderId);
		newImage.setFileStream(new ByteArrayInputStream(C.reference_test_data.image_data));
		if (nodeId != 0) {
			newImage.setChannelInfo(nodeId, newImage.getChannelSetId());
		}
		newImage.save();
		t.commit(false);
        
		return newImage;
	}
	
	/**
	 * Test the rendering with specified ordering options in master node and in channel node.
	 * The method makes the necessary assertations.
	 * 
	 * @param page The page we will use for rendering.
	 * @param masterId master ID for rendering
	 * @param channelId The channel ID for rendering in channel node.
	 * @param expectedOutputMain The expected result for master node rendering.
	 * @param expectedOutputChannel The expected result for channel node rendering.
	 * @throws Exception Throws exception if something goes wrong.
	 */
	public static void renderingTest(com.gentics.contentnode.rest.model.Page page, int masterId,
			int channelId, String expectedOutputMain, String expectedOutputChannel) throws Exception {
		
		// now render the page and check the result
		String renderedPage = OverviewHelper.renderPage(page.getId());

		renderedPage = renderedPage.replaceAll("\\r", "");
		assertEquals("Check rendered testpage", expectedOutputMain, renderedPage);

		// render the page for the channel
		renderedPage = OverviewHelper.renderPage(page.getId(), channelId);
		renderedPage = renderedPage.replaceAll("\\r", "");
		assertEquals("Check rendered testpage", expectedOutputChannel, renderedPage);
	}

	/**
	 * Create folder with name "master-folder" and publish dir "/master-folder".
	 * 
	 * @param folderId The parent folder id.
	 * @return the created {@link Folder} instance.
	 * @throws Exception if anything goes wrong.
	 */
	public static Folder createFolder(int folderId) throws Exception {
		return createFolder(folderId, 0);
	}

	/**
	 * Create folder with name "master-folder" and publish dir "/master-folder".
	 * 
	 * @param folderId The parent folder id.
	 * @param nodeId ID of the node or 0
	 * @return the created {@link Folder} instance.
	 * @throws Exception if anything goes wrong.
	 */
	public static Folder createFolder(int folderId, int nodeId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Folder newFolder = t.createObject(Folder.class);

		newFolder.setName("master-folder");
		newFolder.setMotherId(folderId);
		newFolder.setPublishDir("/master-folder");
		if (nodeId != 0) {
			newFolder.setChannelInfo(nodeId, newFolder.getChannelSetId());
		}
		newFolder.save();
		t.commit(false);
        
		return newFolder;
	}
	
	/**
	 * Localize folder. Change the name to "channel-folder" and publish dir to "/channel-folder".
	 * 
	 * @param channelId The channel ID we want to localize the folder for.
	 * @param folderId ID of the folder we want to localize.
	 * @return The localized {@link Folder} instance.
	 * @throws Exception if something goes wrong.
	 */
	public static Folder localizeFolder(int channelId, int folderId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		com.gentics.contentnode.object.Folder testFolder = t.getObject(com.gentics.contentnode.object.Folder.class, folderId);
		com.gentics.contentnode.object.Folder localTestFolder = (com.gentics.contentnode.object.Folder) testFolder.copy();

		localTestFolder.setName("channel-folder");
		localTestFolder.setPublishDir("/channel-folder");
		localTestFolder.setChannelInfo(channelId, testFolder.getChannelSetId());
		localTestFolder.save();
		t.commit(false);
        
		return localTestFolder;
	}
	
	/**
	 * Render page for the master node.
	 * 
	 * @param pageId ID of the page we want to render.
	 * @return The rendered content.
	 * @throws Exception if something goes wrong.
	 */
	public static String renderPage(int pageId) throws Exception {
		return renderPage(pageId, -1);
	}
	
	/**
	 * Render page for the specified channel.
	 * 
	 * @param pageId ID of the page we want to render.
	 * @param channelId ID of the channel we want to render the page for.
	 * @return The rendered content.
	 * @throws Exception if something goes wrong.
	 */
	public static String renderPage(int pageId, int channelId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Page page = t.getObject(Page.class, pageId);
		
		if (channelId > 0) {
			t.setChannelId(channelId);
		}
		
		RenderResult renderResult = new RenderResult();
		String renderedPage = page.render(renderResult);
        
		if (channelId > 0) {
			t.resetChannel();
		}
        
		return renderedPage;
	}
	
	/**
	 * Create an expected string for use in rendering assertations.
	 * 
	 * The created rendering string is created based on the following template:
	 * 
	 * object_name (object_id)
	 * 
	 * @param input The {@link List} of {@link ExpectedObject} that will be transformed to expected {@link String}. 
	 * @return The generated expected {@link String} instance.
	 */
	public static String generateExpectedRenderingOutput(List<ExpectedObject> input) {
		StringBuilder builder = new StringBuilder();

		for (ExpectedObject expectedObject : input) {
			builder.append(expectedObject.name);
			builder.append(" (");
			builder.append(expectedObject.id);
			builder.append(")\n");
		}
		return builder.toString();
	}
	
	/**
	 * This class holds the an item information for use in expected string generation.
	 * 
	 * @author Antoniy Chonkov <antoniy@web-teh.net>
	 */
	public final static class ExpectedObject {
		String name;
		int id;
		
		public ExpectedObject(String name, int id) {
			this.name = name;
			this.id = id;
		}
	}
}
