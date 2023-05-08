package com.gentics.contentnode.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * This class implements logic, frequently used by all overview related tests.
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
public final class OverviewHelper {

	private OverviewHelper() {}
	
	/**
	 * Test the rendering with specified ordering options in master node and in channel node.
	 * The method makes the necessary assertions.
	 * 
	 * @param page The page we will use for rendering.
	 * @param channelId The channel ID for rendering in channel node.
	 * @param overview The {@link Overview} instance used to set ordering.
	 * @param expectedOutputMain The expected result for master node rendering.
	 * @param expectedOutputChannel The expected result for channel node rendering.
	 * @throws Exception Throws exception if something goes wrong.
	 */
	public static void renderingTest(com.gentics.contentnode.rest.model.Page page, int channelId, Overview overview, 
			OrderBy orderBy, OrderDirection orderDirection,
			String expectedOutputMain, String expectedOutputChannel) throws Exception {
		
		// change the ordering
		overview.setOrderBy(orderBy);
		overview.setOrderDirection(orderDirection);
		OverviewHelper.savePage(page);
		
		// now render the page and check the result
		String renderedPage = OverviewHelper.renderPage(page.getId());

		assertEquals("Check rendered testpage", expectedOutputMain, renderedPage);

		// render the page for the channel
		renderedPage = OverviewHelper.renderPage(page.getId(), channelId);
		assertEquals("Check rendered testpage", expectedOutputChannel, renderedPage);
	}

	/**
	 * Localize and publish the page that contains an overview.
	 * 
	 * @param channelId The channel ID we want to localize the page for.
	 * @param pageId ID of the page.
	 * @param publish true if the page shall be published, false if not
	 * @return The localized {@link com.gentics.contentnode.rest.model.Page} instance.
	 * @throws Exception if something goes wrong.
	 */
	public static com.gentics.contentnode.rest.model.Page localizeAndPublishOverviewPage(int channelId, int pageId, boolean publish) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Page testPage = t.getObject(Page.class, pageId);
		Page localCopy = (com.gentics.contentnode.object.Page) testPage.copy();

		localCopy.setName(localCopy.getName() + "-localized");
		localCopy.setChannelInfo(channelId, testPage.getChannelSetId());
		localCopy.save();
		if (publish) {
			localCopy.publish();
		}
		t.commit(false);

		PageResourceImpl pageResource = new PageResourceImpl();

		pageResource.setTransaction(t);

		String localCopyPageId = Integer.toString(ObjectTransformer.getInteger(localCopy.getId(), -1));
		PageLoadResponse pageLoadResponse = pageResource.load(localCopyPageId, false, false, false, false, false, false, false, false, false, false, null, null);

		assertEquals("Check the response code", ResponseCode.OK, pageLoadResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the page exists", pageLoadResponse.getPage());

		return pageLoadResponse.getPage();
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
        
		String words[] = localCopyImage.getName().split("\\.");

		localCopyImage.setName(words[0] + "-localized." + words[1]);
		localCopyImage.setChannelInfo(channelId, image.getChannelSetId());
		localCopyImage.save();
		t.commit(false);
        
		return localCopyImage;
	}
	
	/**
	 * Save image.
	 * 
	 * @param image The {@link Image} instance we want to save.
	 * @throws Exception if something goes wrong.
	 */
	public static void saveImage(com.gentics.contentnode.rest.model.Image image) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		 
		ImageResourceImpl imageResource = new ImageResourceImpl();

		imageResource.setTransaction(t);
        
		ImageSaveRequest saveRequest = new ImageSaveRequest();

		saveRequest.setImage(image);
		GenericResponse saveResponse = imageResource.save(image.getId(), saveRequest);

		assertEquals("Check the response code", ResponseCode.OK, saveResponse.getResponseInfo().getResponseCode());
	}
	
	/**
	 * Localize file.
	 * 
	 * @param channelId The channel ID we want to localize the file for.
	 * @param fileId ID of the file we want to localize.
	 * @return The localized {@link File} instance.
	 * @throws Exception if something goes wrong.
	 */
	public static File localizeFile(int channelId, int fileId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		com.gentics.contentnode.object.File testFile = t.getObject(com.gentics.contentnode.object.File.class, fileId);
		com.gentics.contentnode.object.File localTestFile = (com.gentics.contentnode.object.File) testFile.copy();

		localTestFile.setName(localTestFile.getName() + "-localized");
		localTestFile.setChannelInfo(channelId, testFile.getChannelSetId());
		localTestFile.save();
		t.commit(false);
        
		return localTestFile;
	}
	
	/**
	 * Save file.
	 * 
	 * @param file The {@link com.gentics.contentnode.rest.model.File} instance we want to save.
	 * @throws Exception if something goes wrong.
	 */
	public static void saveFile(com.gentics.contentnode.rest.model.File file) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		 
		FileResourceImpl fileResource = new FileResourceImpl();

		fileResource.setTransaction(t);
        
		FileSaveRequest saveRequest = new FileSaveRequest();

		saveRequest.setFile(file);
		GenericResponse saveResponse = fileResource.save(file.getId(), saveRequest);

		assertEquals("Check the response code", ResponseCode.OK, saveResponse.getResponseInfo().getResponseCode());
	}
	
	/**
	 * Localize folder.
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

		localTestFolder.setName(localTestFolder.getName() + "-localized");
		localTestFolder.setChannelInfo(channelId, testFolder.getChannelSetId());
		localTestFolder.save();
		t.commit(false);
        
		return localTestFolder;
	}
	
	/**
	 * Save folder.
	 * 
	 * @param folder The {@link com.gentics.contentnode.rest.model.Folder} instance we want to save.
	 * @throws Exception if something goes wrong.
	 */
	public static void saveFolder(com.gentics.contentnode.rest.model.Folder folder) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		 
		FolderResourceImpl folderResource = new FolderResourceImpl();

		folderResource.setTransaction(t);
        
		FolderSaveRequest saveRequest = new FolderSaveRequest();

		saveRequest.setFolder(folder);
		GenericResponse saveResponse = folderResource.save(Integer.toString(folder.getId()), saveRequest);

		assertEquals("Check the response code", ResponseCode.OK, saveResponse.getResponseInfo().getResponseCode());
	}
	
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

		localCopyPage.setName(localCopyPage.getName() + "-localized");
		localCopyPage.setChannelInfo(channelId, page.getChannelSetId());
		localCopyPage.save();
		localCopyPage.publish();
		t.commit(false);
        
		return localCopyPage;
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
		Transaction t = TransactionManager.getCurrentTransaction();
		
		PageResourceImpl pageResource = new PageResourceImpl();

		pageResource.setTransaction(t);
        
		PageCreateRequest createPageRequest = new PageCreateRequest();

		createPageRequest.setFolderId(Integer.toString(folderId));
		createPageRequest.setTemplateId(templateId);
		PageLoadResponse createPageResponse = pageResource.create(createPageRequest);

		assertEquals("Check the reponse code", ResponseCode.OK, createPageResponse.getResponseInfo().getResponseCode());

		// get the test page
		com.gentics.contentnode.rest.model.Page testPage = createPageResponse.getPage();
        
		return testPage;
	}
	
	/**
	 * Save {@link com.gentics.contentnode.rest.model.Page}.
	 * 
	 * @param page The {@link com.gentics.contentnode.rest.model.Page} instance we want to save.
	 * @throws Exception if something goes wrong.
	 */
	public static void savePage(com.gentics.contentnode.rest.model.Page page) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		PageResourceImpl pageResource = new PageResourceImpl();

		pageResource.setTransaction(t);
        
		PageSaveRequest saveRequest = new PageSaveRequest();

		saveRequest.setPage(page);
		saveRequest.setUnlock(true);
		GenericResponse saveResponse = pageResource.save(Integer.toString(page.getId()), saveRequest);

		assertEquals("Check the response code", ResponseCode.OK, saveResponse.getResponseInfo().getResponseCode());
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
	 * Create and configure {@link Overview} instance with specified config parameters.
	 * 
	 * @param page The {@link com.gentics.contentnode.rest.model.Page} we will create this overview from.
	 * @param listType {@link ListType} instance that specifies the type of content that the overview has.
	 * @param selectType {@link SelectType} instance that specifies what selection will be used.
	 * @param selectedItemsIds The list of IDs for the items we want to select - pass <b>null</b> if no 
	 * selected items need to be provided.
	 * @param recursive true if we want to get the objects recursively in the subfolders, false - otherwise.
	 * @return The created and configured {@link Overview} instance.
	 */
	public static Overview getConfiguredOverviewFromPage(com.gentics.contentnode.rest.model.Page page,
			ListType listType, SelectType selectType, String renderingTemplate, List<Integer> selectedItemsIds) {
		return getConfiguredOverviewFromPage(page, listType, selectType, renderingTemplate, selectedItemsIds, false);
	}
	
	/**
	 * Create and configure {@link Overview} instance with specified config parameters.
	 * 
	 * @param page The {@link com.gentics.contentnode.rest.model.Page} we will create this overview from.
	 * @param listType {@link ListType} instance that specifies the type of content that the overview has.
	 * @param selectType {@link SelectType} instance that specifies what selection will be used.
	 * @param selectedItemsIds The list of IDs for the items we want to select - pass <b>null</b> if no 
	 * selected items need to be provided.
	 * @return The created and configured {@link Overview} instance.
	 */
	public static Overview getConfiguredOverviewFromPage(com.gentics.contentnode.rest.model.Page page,
			ListType listType, SelectType selectType, String renderingTemplate, List<Integer> selectedItemsIds, boolean recursive) {
		Overview overview = extractOverviewFromRestPage(page);

		// modify the tag to directly select pages (and set the overview template)
		overview.setListType(listType);
		overview.setSelectType(selectType);
		overview.setOrderBy(OrderBy.ALPHABETICALLY);
		overview.setOrderDirection(OrderDirection.ASC);
		if (selectedItemsIds != null) {
			overview.setSelectedItemIds(selectedItemsIds);
		}
		overview.setSource(renderingTemplate);
		overview.setRecursive(recursive);
        
		return overview;
	}
	
	/**
	 * Create new {@link Overview} instance from a page.
	 * 
	 * @param page ID of the page we will use to create the overview from.
	 * @return The created {@link Overview} instance.
	 */
	public static Overview extractOverviewFromRestPage(com.gentics.contentnode.rest.model.Page page) {
		Tag overviewTag = page.getTags().get("ds");

		assertNotNull("Check that the overview tag exists", overviewTag);
		assertEquals("Check the type of the overview tag", Tag.Type.CONTENTTAG, overviewTag.getType());

		// get the properties of the tag
		Map<String, Property> properties = overviewTag.getProperties();

		assertNotNull("Check the tag properties", properties);

		// get the overview property
		Property property = properties.get("ds");

		assertNotNull("Check the overview property", property);
		assertEquals("Check the overview property type", Property.Type.OVERVIEW, property.getType());

		// get the overview
		Overview overview = property.getOverview();

		assertNotNull("Check the overview", overview);

		return overview;
	}

	/**
	 * This method extracts an {@link com.gentics.contentnode.object.Overview}
	 * from a {@link Page} by a given contentTagName and the valueKeyName.
	 * Postcondition: the overview must not be null
	 * @param {@link Page} the page node object, must not be null
	 * @param contentTagName
	 *            the name of the contenttag
	 * @param valueKeyName
	 *            the name of the
	 *            {@link com.gentics.contentnode.object.Overview}
	 * @return the {@link com.gentics.contentnode.object.Overview} contained in
	 *         the Page
	 * @throws NodeException
	 *             if there is an error getting the {@link ContentTag}, the
	 *             {@link Value} or the {@link PartType}
	 */
	public static com.gentics.contentnode.object.Overview extractOverviewFromPage(Page page, String contentTagName, String valueKeyName) throws NodeException {
		assertNotNull("The page does not exist (is null)", page);

		com.gentics.contentnode.object.Overview overview = null;
		ContentTag overviewTag = page.getContentTag(contentTagName);
		Value overviewValue = overviewTag.getValues().getByKeyname(valueKeyName);
		PartType partType = overviewValue.getPartType();
		if (partType instanceof OverviewPartType) {
			overview = ((OverviewPartType) partType).getOverview();
		}

		// Postcondition: the overview must exist in the page
		assertNotNull("Overview not found in contenttag: " + contentTagName, overview);
		return overview;
	}
	
	/**
	 * This method adds {@link OverviewEntry}'s from a list of selected items to
	 * the given {@link com.gentics.contentnode.object.Overview}
	 * @param the {@link com.gentics.contentnode.object.Overview}, must not be null
	 * @param selectedItemsIds the id's of the selected items to add, must not
	 * be null
	 * @throws NodeException delegated
	 */
	public static void addOverviewEntriesToOverview(com.gentics.contentnode.object.Overview overview, List<Integer> selectedItemsIds) throws NodeException {
		assertNotNull("The input overview must not be null",overview);
		assertNotNull("The list of selectedItemIds must not be null",selectedItemsIds);
		Transaction t = TransactionManager.getCurrentTransaction();
		List<OverviewEntry> overviewEntries = overview.getOverviewEntries();

		OverviewEntry entry = null;
		for(int selectedItem: selectedItemsIds){
			entry = t.createObject(OverviewEntry.class);
			if (entry!=null) {
				entry.setObjectId(selectedItem);
				entry.setObjectOrder(0);
				overviewEntries.add(entry);
			}
		}
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
	 * This class holds the an item informaction for use in expected string generation.
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
