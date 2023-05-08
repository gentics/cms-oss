package com.gentics.contentnode.tests.rest.page;

import static com.gentics.api.lib.etc.ObjectTransformer.getInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Various testcases for the page copy rest api call.
 * 
 * @author johannes2
 * 
 */
public class PageCopyTest extends AbstractPageCopyTest {

	/**
	 * Test whether an invalid copy request will fail correctly.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidCopyRequest() throws Exception {
		PageCopyResponse response = copy(new PageCopyRequest(), false);
		assertTrue("No pages should have been copied", response.getPages().size() == 0);
		assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ")", ResponseCode.FAILURE, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Copy a single page to a target folder. Invoke this action twice. Check
	 * that two copied have been created and that both copied are not the same
	 * page.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopyPageTwice() throws Exception {
		testContext.getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page testPageA = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);

		// Prepare copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		addTargetFolder(copyRequest, nodeA.getFolder());
		copyRequest.getSourcePageIds().add(getInteger(testPageA.getId(), -1));
		// Set the flag to true in order to create an extra copy in the target
		// folder when called mutliple times.
		copyRequest.setCreateCopy(ENABLED);

		// First copy call.
		PageCopyResponse response = copy(copyRequest, true);
		t = TransactionManager.getCurrentTransaction();

		assertTrue("Only one page should have been copied.", response.getPages().size() == 1);

		Page firstCopiedPage = t.getObject(Page.class, response.getPages().get(0).getId());
		assertEquals("The target folder does not match the parent folder of the copied page.", firstCopiedPage.getFolder().getId(), nodeA.getFolder().getId());
		assertEquals("Filename does not match", "DummyPage1.html", firstCopiedPage.getFilename());
		assertEquals("Name does not match", "Kopie von DummyPage", firstCopiedPage.getName());
		t.commit(false);

		// Examining first copy
		firstCopiedPage = t.getObject(Page.class, response.getPages().get(0).getId());
		assertEquals("The target folder does not match the parent folder of the copied page.", firstCopiedPage.getFolder().getId(), nodeA.getFolder().getId());
		assertEquals("Filename does not match", "DummyPage1.html", firstCopiedPage.getFilename());
		assertEquals("Name does not match", "Kopie von DummyPage", firstCopiedPage.getName());
		assertTrue("The content of the copy and the source should not be the same.", testPageA.getContent().getId() != firstCopiedPage.getContent().getId());
		// Copy the testPageA to the target folder a second time
		response = copy(copyRequest, true);
		t = TransactionManager.getCurrentTransaction();

		Page secondCopiedPage = t.getObject(Page.class, response.getPages().get(0).getId());
		assertEquals("The target folder does not match the parent folder of the copied page.", firstCopiedPage.getFolder().getId(), nodeA.getFolder().getId());
		assertTrue("Both resulting copied should not have the same id since the second copy call must create another copy because setCreateCopy was set to true.",
				secondCopiedPage.getId() != firstCopiedPage.getId());
		assertEquals("Filename does not match", "DummyPage2.html", secondCopiedPage.getFilename());
		assertEquals("Name does not match", "Kopie 2 von DummyPage", secondCopiedPage.getName());
		t.commit(false);

		// Examining second copy
		secondCopiedPage = t.getObject(Page.class, response.getPages().get(0).getId());
		assertEquals("The target folder does not match the parent folder of the copied page.", secondCopiedPage.getFolder().getId(), nodeA.getFolder().getId());
		assertEquals("Filename does not match", "DummyPage2.html", secondCopiedPage.getFilename());
		assertEquals("Name does not match", "Kopie 2 von DummyPage", secondCopiedPage.getName());
	}

	/**
	 * Tests whether one page can be copied to a single folder. Create copy is
	 * disabled thus a new page will only be created when the page with the
	 * given name could not be located within the target folder. This test
	 * repeats the copy call to make sure that the second copy does not create
	 * an additional page in the target folder. Instead the previous copy should
	 * be returned.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSingleCopyNoCreateCopy() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page testPage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		t.commit(false);

		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		addTargetFolder(copyRequest, nodeA.getFolder());
		copyRequest.getSourcePageIds().add(getInteger(testPage.getId(), -1));

		// First copy call.
		PageCopyResponse response = copy(copyRequest, false);
		assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ")", ResponseCode.FAILURE, response.getResponseInfo().getResponseCode());

		CNI18nString expectedString = new CNI18nString("page_copy.name_conflict_detected");
		expectedString.addParameter(I18NHelper.getLocation(testPage));
		expectedString.addParameter(I18NHelper.getLocation(nodeA.getFolder()));
		assertMessage(response, expectedString);
		assertEquals("Reponse did not contain the expected error message.", "Error during copy process. Please check the messages.", response.getResponseInfo()
				.getResponseMessage());
		assertEquals("Check whether correct number of copys have been made.", 0, response.getPages().size());

	}

	/**
	 * Copy a set of language variations to a different folder in the same node
	 * and check whether the sync_page_id information was correctly migrated
	 * from the source pages to the newly created copies.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopyWithSyncPageId() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Map<String, Page> pages = createLanguageVariantPages();
		Folder targetFolder = Creator.createFolder(nodeA.getFolder(), "subfolder", "/test");
		t.commit(false);

		// Setup copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(pages.get(GERMAN).getId(), -1));
		addTargetFolder(copyRequest, targetFolder);

		// Invoke copy call
		copy(copyRequest, true);

		List<com.gentics.contentnode.rest.model.Page> expectedPages = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page expectedCopy;

		// German
		expectedCopy = new com.gentics.contentnode.rest.model.Page();
		expectedCopy.setFolderId(getInteger(targetFolder.getId(), -1));
		expectedCopy.setName("Kopie 3 von " + DUMMY_PAGE_NAME);
		expectedCopy.setFileName(DUMMY_PAGE_NAME + GERMAN_FILE_EXTENSION);
		expectedCopy.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.add(expectedCopy);

		// English
		expectedCopy = new com.gentics.contentnode.rest.model.Page();
		expectedCopy.setFolderId(getInteger(targetFolder.getId(), -1));
		expectedCopy.setName("Kopie 2 von " + DUMMY_PAGE_NAME);
		expectedCopy.setFileName(DUMMY_PAGE_NAME + ENGLISH_FILE_EXTENSION);
		expectedCopy.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.add(expectedCopy);

		// Klingon
		expectedCopy = new com.gentics.contentnode.rest.model.Page();
		expectedCopy.setFolderId(getInteger(targetFolder.getId(), -1));
		expectedCopy.setName("Kopie von " + DUMMY_PAGE_NAME);
		expectedCopy.setFileName(DUMMY_PAGE_NAME + KLINGON_FILE_EXTENSION);
		expectedCopy.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.add(expectedCopy);
		assertCopiedPages(targetFolder, expectedPages);

		// Verify that the the sync_page_id was updated for newly created pages
		Map<String, Page> pagesByLanguageCode = new HashMap<String, Page>();
		for (Page page : targetFolder.getPages()) {
			pagesByLanguageCode.put(page.getLanguage().getCode(), page);
		}

		assertEquals("The page sync id should be 0 since the german page is the root page of the set of language variants.", 0, pagesByLanguageCode.get(GERMAN).getSyncPageId()
				.intValue());
		assertEquals("The klingon page was created by creating a language variant from the german page. The page copy action did not update the sync_page_id.", pagesByLanguageCode
				.get(KLINGON).getSyncPageId(), pagesByLanguageCode.get(GERMAN).getId());
		assertEquals("The english page was created by creating a language variant from the german page. The page copy action did not update the sync_page_id.", pagesByLanguageCode
				.get(ENGLISH).getSyncPageId(), pagesByLanguageCode.get(GERMAN).getId());
	}

	/**
	 * Copy a single page into a folder that already contains a file with the
	 * same filename as the page. Verify that the filename gets renamed.
	 * 
	 * @throws Exception
	 * @throws NodeException
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testPageCopyWithExistingFileConflict() throws UnsupportedEncodingException, NodeException, Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		File file = Creator.createFile(nodeB.getFolder(), DUMMY_PAGE_FILENAME, "This is the testfile".getBytes("UTF-8"), null);
		t.commit(false);

		assertEquals("Both objects must have the same filename", sourcePage.getFilename(), file.getName());

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(), -1));
		addTargetFolder(copyRequest, nodeB.getFolder());

		// Invoke the copy call
		copy(copyRequest, true);

		com.gentics.contentnode.rest.model.Page expectedPage = new com.gentics.contentnode.rest.model.Page();
		expectedPage.setFileName(DUMMY_PAGE_NAME + "1.html");
		expectedPage.setName("Kopie von " + DUMMY_PAGE_NAME);
		assertCopiedPages(nodeB.getFolder(), expectedPage);

	}

	/**
	 * Copy a german page with a filename that contains the language code from
	 * nodeA to nodeC. The german language is not assigned to nodeC. Make sure
	 * that the filenames are correctly generated.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopyToNodeWithMissingLanguage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Node nodeC = ContentNodeTestDataUtils.createNode("testnode3", "www.testnode3.at", "/test", "/testbin", false, false);
		Page testPage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template, de);
		testPage.setFilename(DUMMY_PAGE_NAME + GERMAN_FILE_EXTENSION);
		testPage.save();
		t.commit(false);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(testPage.getId(), -1));
		addTargetFolder(copyRequest, nodeC.getFolder());

		// Invoke the copy call
		copy(copyRequest, true);

		com.gentics.contentnode.rest.model.Page firstExpectedCopy = new com.gentics.contentnode.rest.model.Page();
		firstExpectedCopy.setName("Kopie von " + DUMMY_PAGE_NAME);
		firstExpectedCopy.setFileName(testPage.getFilename());
		assertCopiedPages(nodeC.getFolder(), firstExpectedCopy);

		// Repeat the copy call and verify that the language code is detected
		// correctly and the incremental number is in the correct place.
		copy(copyRequest, true);
		com.gentics.contentnode.rest.model.Page secondExpectedCopy = new com.gentics.contentnode.rest.model.Page();
		secondExpectedCopy.setName("Kopie 2 von " + DUMMY_PAGE_NAME);
		secondExpectedCopy.setFileName(DUMMY_PAGE_NAME + "1.de.html");
		assertCopiedPages(nodeC.getFolder(), firstExpectedCopy, secondExpectedCopy);

	}

	/**
	 * Simple batch copy of multiple pages into one target folder.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBatchCopy() throws Exception {
		testContext.getContext().startTransaction();

		// Setup of the text fixture
		Page testPageA = Creator.createPage(DUMMY_PAGE_NAME + "A", nodeA.getFolder(), template);
		Page testPageB = Creator.createPage(DUMMY_PAGE_NAME + "B", nodeA.getFolder(), template);
		Page testPageC = Creator.createPage(DUMMY_PAGE_NAME + "C", nodeA.getFolder(), template);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(testPageA.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageB.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageC.getId(), -1));
		addTargetFolder(copyRequest, nodeA.getFolder());

		// Invoke the copy call
		copy(copyRequest, true);

		List<com.gentics.contentnode.rest.model.Page> expectedPages = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		// We are coping into the same folder. Add previously existing source
		// pages
		expectedPages.add(ModelBuilder.getPage(testPageA));
		expectedPages.add(ModelBuilder.getPage(testPageB));
		expectedPages.add(ModelBuilder.getPage(testPageC));

		// Add the expected copies
		com.gentics.contentnode.rest.model.Page page;
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName("Kopie von " + DUMMY_PAGE_NAME + "A");
		page.setFileName(DUMMY_PAGE_NAME + "A1.html");
		expectedPages.add(page);

		page = new com.gentics.contentnode.rest.model.Page();
		page.setName("Kopie von " + DUMMY_PAGE_NAME + "B");
		page.setFileName(DUMMY_PAGE_NAME + "B1.html");
		expectedPages.add(page);

		page = new com.gentics.contentnode.rest.model.Page();
		page.setName("Kopie von " + DUMMY_PAGE_NAME + "C");
		page.setFileName(DUMMY_PAGE_NAME + "C1.html");
		expectedPages.add(page);
		assertCopiedPages(nodeA.getFolder(), expectedPages);
	}

	/**
	 * Test batch copying multiple pages to multiple folders.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBatchCopyToMultipleFolders1() throws Exception {
		testContext.getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create some test pages & folders
		Page testPageA = Creator.createPage(DUMMY_PAGE_NAME + "A", nodeA.getFolder(), template);
		Page testPageB = Creator.createPage(DUMMY_PAGE_NAME + "B", nodeA.getFolder(), template);
		Page testPageC = Creator.createPage(DUMMY_PAGE_NAME + "C", nodeA.getFolder(), template);

		Folder folderA = Creator.createFolder(nodeA.getFolder(), "subfolder1-nodea", "/");
		Folder folderB = Creator.createFolder(nodeA.getFolder(), "subfolder2-nodea", "/");
		Folder folderC = Creator.createFolder(nodeA.getFolder(), "subfolder3-nodea", "/");
		Folder folderD = Creator.createFolder(nodeA.getFolder(), "subfolder4-nodea", "/");
		t.commit(false);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(testPageA.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageB.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageC.getId(), -1));
		addTargetFolder(copyRequest, folderA);
		addTargetFolder(copyRequest, folderB);
		addTargetFolder(copyRequest, folderC);

		// Invoke copy call
		PageCopyResponse response = copy(copyRequest, true);
		t = TransactionManager.getCurrentTransaction();

		assertEquals("Check whether correct number of copys have been made.", 9, response.getPages().size());

		// Check whether the pages were copied to each folder
		folderA = t.getObject(Folder.class, folderA.getId());
		folderB = t.getObject(Folder.class, folderA.getId());
		folderC = t.getObject(Folder.class, folderA.getId());

		// Check that the expected page copys are in folderA
		List<com.gentics.contentnode.rest.model.Page> expectedPages = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page expectedPageA = new com.gentics.contentnode.rest.model.Page();
		expectedPageA.setName("Kopie von DummyPageA");
		expectedPageA.setFileName("DummyPageA1.html");
		expectedPages.add(expectedPageA);
		com.gentics.contentnode.rest.model.Page expectedPageB = new com.gentics.contentnode.rest.model.Page();
		expectedPageB.setName("Kopie von DummyPageB");
		expectedPageB.setFileName("DummyPageB1.html");
		expectedPages.add(expectedPageB);
		com.gentics.contentnode.rest.model.Page expectedPageC = new com.gentics.contentnode.rest.model.Page();
		expectedPageC.setName("Kopie von DummyPageC");
		expectedPageC.setFileName("DummyPageC1.html");
		expectedPages.add(expectedPageC);

		assertCopiedPages(folderA, expectedPages);
		assertCopiedPages(folderB, expectedPages);
		assertCopiedPages(folderC, expectedPages);
		assertCopiedPages(folderD, EMPTY_LIST_OF_PAGES);

		// Copy again and this time add folder d as well. This means we will
		// copy 3 pages into 4 folders.
		addTargetFolder(copyRequest, folderD);
		response = copy(copyRequest, true);
		t = TransactionManager.getCurrentTransaction();
		assertEquals("Check whether correct number of copys have been made.", 12, response.getPages().size());

		assertCopiedPages(folderA, expectedPages);
		assertCopiedPages(folderB, expectedPages);
		assertCopiedPages(folderC, expectedPages);
		folderD = t.getObject(Folder.class, folderD.getId());
		assertEquals("The newly add folder does not contain the expected amount of pages.", expectedPages.size(), folderD.getPages().size());

	}

	/**
	 * Test batch copying a single page to multiple folders.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBatchCopyToMultipleFolders2() throws Exception {
		testContext.getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create some test pages & folders
		Page testPageA = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Folder folderA = Creator.createFolder(nodeA.getFolder(), "subfolder1-nodea", "/test");
		Folder folderB = Creator.createFolder(nodeA.getFolder(), "subfolder2-nodea", "/test");
		Folder folderC = Creator.createFolder(nodeA.getFolder(), "subfolder3-nodea", "/test");
		t.commit(false);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		copyRequest.getSourcePageIds().add(getInteger(testPageA.getId(), -1));
		addTargetFolder(copyRequest, folderA);
		addTargetFolder(copyRequest, folderB);
		addTargetFolder(copyRequest, folderC);

		// Invoke copy call
		copy(copyRequest, true);
		t = TransactionManager.getCurrentTransaction();

		// Check whether the pages were copied to each folder
		folderA = t.getObject(Folder.class, folderA.getId());
		folderB = t.getObject(Folder.class, folderB.getId());
		folderC = t.getObject(Folder.class, folderC.getId());

		// Check that the expected page copy is in folderA
		List<com.gentics.contentnode.rest.model.Page> expectedPages = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page expectedPageCopy = new com.gentics.contentnode.rest.model.Page();
		expectedPages.add(expectedPageCopy);

		// Verify that the filenames and names will be updated since the three
		// folders share the same pubdir path
		expectedPageCopy.setName(DUMMY_PAGE_NAME);
		expectedPageCopy.setFileName("DummyPage.html");
		assertCopiedPages(folderA, expectedPages);

		expectedPageCopy.setName(DUMMY_PAGE_NAME);
		expectedPageCopy.setFileName("DummyPage1.html");
		assertCopiedPages(folderB, expectedPages);

		expectedPageCopy.setName(DUMMY_PAGE_NAME);
		expectedPageCopy.setFileName("DummyPage2.html");
		assertCopiedPages(folderC, expectedPages);
	}

	/**
	 * Copy pages from Node A to Folders in Node A and B.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCrossNodeBatchCopy() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create source pages
		Page testPageA = Creator.createPage(DUMMY_PAGE_NAME + "A1", nodeA.getFolder(), template);
		Page testPageB = Creator.createPage(DUMMY_PAGE_NAME + "A2", nodeA.getFolder(), template);
		Page testPageC = Creator.createPage(DUMMY_PAGE_NAME + "A3", nodeA.getFolder(), template);

		Folder nodeAfolderA = Creator.createFolder(nodeA.getFolder(), "subfolderA-nodea", "/test");
		Folder nodeBfolderB = Creator.createFolder(nodeB.getFolder(), "subfolderB-nodeb", "/test");
		t.commit(false);

		// Prepare copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.getSourcePageIds().add(getInteger(testPageA.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageB.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageC.getId(), -1));
		addTargetFolder(copyRequest, nodeAfolderA);
		addTargetFolder(copyRequest, nodeBfolderB);

		// Invoke copy call
		copy(copyRequest, true);

		List<com.gentics.contentnode.rest.model.Page> expectedPages = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page page;
		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + "A1.html");
		page.setName(DUMMY_PAGE_NAME + "A1");
		expectedPages.add(page);

		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + "A2.html");
		page.setName(DUMMY_PAGE_NAME + "A2");
		expectedPages.add(page);

		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + "A3.html");
		page.setName(DUMMY_PAGE_NAME + "A3");
		expectedPages.add(page);

		assertCopiedPages(nodeAfolderA, expectedPages);

		// We expect no changes for the filenames in the second folder since the
		// folder has a different publish path compared to the first target
		// folder.
		assertCopiedPages(nodeBfolderB, expectedPages);

	}

	/**
	 * Copy a single page into multiple folders in the same node. Make sure that
	 * the filename of the new pages will be changed and that the page name
	 * stays the same. Invoke this copy with create copy disabled.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishPathCollisionDetectionCreateCopyDisabled() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Folder nodeAfolderA = Creator.createFolder(nodeA.getFolder(), "subfolderA-nodeA", "/test");
		Folder nodeAfolderB = Creator.createFolder(nodeA.getFolder(), "subfolderB-nodeA", "/test");
		Folder nodeAfolderC = Creator.createFolder(nodeA.getFolder(), "subfolderC-nodeA", "/test");
		t.commit(false);

		// Setup copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(), -1));
		addTargetFolder(copyRequest, nodeAfolderA);
		addTargetFolder(copyRequest, nodeAfolderB);
		addTargetFolder(copyRequest, nodeAfolderC);

		// Invoke copy call
		copy(copyRequest, true);

		// Verify that the page copy is only copied to the first folder in the
		// list of target folders.
		List<com.gentics.contentnode.rest.model.Page> expectedPages = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page page;
		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_FILENAME);
		page.setName(DUMMY_PAGE_NAME);
		expectedPages.add(page);
		assertCopiedPages(nodeAfolderA, expectedPages);

		// Second page should have been renamed (filename only)
		expectedPages.clear();
		page.setFileName(DUMMY_PAGE_NAME + "1.html");
		page.setName(DUMMY_PAGE_NAME);
		expectedPages.add(page);
		assertCopiedPages(nodeAfolderB, expectedPages);

		// Third copy should have been renamed as well (filename only)
		expectedPages.clear();
		page.setFileName(DUMMY_PAGE_NAME + "2.html");
		page.setName(DUMMY_PAGE_NAME);
		expectedPages.add(page);
		assertCopiedPages(nodeAfolderC, expectedPages);

	}

	/**
	 * Copy a page with multiple language variations to multiple target folders
	 * that have the same publish path. Make sure the filenames are updated
	 * correctly to avoid publish path collisions.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishPathCollisionWithPageLanguageSet() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Map<String, Page> pages = createLanguageVariantPages();
		Folder targetFolder1 = Creator.createFolder(nodeA.getFolder(), "subfolder1", "/test");
		Folder targetFolder2 = Creator.createFolder(nodeA.getFolder(), "subfolder2", "/test");
		Folder targetFolder3 = Creator.createFolder(nodeA.getFolder(), "subfolder3", "/test");
		t.commit(false);

		// Setup copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(pages.get(ENGLISH).getId(), -1));
		addTargetFolder(copyRequest, targetFolder1);
		addTargetFolder(copyRequest, targetFolder2);
		addTargetFolder(copyRequest, targetFolder3);

		// Invoke copy call
		copy(copyRequest, true);

		Map<String, com.gentics.contentnode.rest.model.Page> expectedPages
			= new LinkedHashMap<String, com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page page;

		// Klingon
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(KLINGON).getName());
		page.setFileName(pages.get(KLINGON).getFilename());
		page.setContentSetId((Integer) pages.get(ENGLISH).getContentsetId() + 1);
		expectedPages.put(KLINGON, page);

		// German
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(GERMAN).getName());
		page.setFileName(pages.get(GERMAN).getFilename());
		page.setContentSetId((Integer) pages.get(ENGLISH).getContentsetId() + 1);
		expectedPages.put(GERMAN, page);

		// English
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(ENGLISH).getName());
		page.setFileName(pages.get(ENGLISH).getFilename());
		page.setContentSetId((Integer) pages.get(ENGLISH).getContentsetId() + 1);
		expectedPages.put(ENGLISH, page);

		assertCopiedPages(targetFolder1, new ArrayList<com.gentics.contentnode.rest.model.Page>(expectedPages.values()));

		expectedPages.get(ENGLISH).setFileName(DUMMY_PAGE_NAME + "1" + ENGLISH_FILE_EXTENSION);
		expectedPages.get(GERMAN).setFileName(DUMMY_PAGE_NAME + "1" + GERMAN_FILE_EXTENSION);
		expectedPages.get(KLINGON).setFileName(DUMMY_PAGE_NAME + "1" + KLINGON_FILE_EXTENSION);
		for (com.gentics.contentnode.rest.model.Page currentPage : expectedPages.values()) {
			currentPage.setContentSetId((Integer) pages.get(ENGLISH).getContentsetId() + 2);
		}
		assertCopiedPages(targetFolder2, new ArrayList<com.gentics.contentnode.rest.model.Page>(expectedPages.values()));

		expectedPages.get(ENGLISH).setFileName(DUMMY_PAGE_NAME + "2" + ENGLISH_FILE_EXTENSION);
		expectedPages.get(GERMAN).setFileName(DUMMY_PAGE_NAME + "2" + GERMAN_FILE_EXTENSION);
		expectedPages.get(KLINGON).setFileName(DUMMY_PAGE_NAME + "2" + KLINGON_FILE_EXTENSION);
		for (com.gentics.contentnode.rest.model.Page currentPage : expectedPages.values()) {
			currentPage.setContentSetId((Integer) pages.get(ENGLISH).getContentsetId() + 3);
		}
		assertCopiedPages(targetFolder3, new ArrayList<com.gentics.contentnode.rest.model.Page>(expectedPages.values()));
	}

	/**
	 * Create a page with additional two lanuguage variations
	 * 
	 * @return
	 * 
	 * @throws NodeException
	 */
	private Map<String, Page> createLanguageVariantPages() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Map<String, Page> pages = new LinkedHashMap<String, Page>();
		Page testPage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template, de);
		testPage.setFilename(DUMMY_PAGE_NAME + GERMAN_FILE_EXTENSION);
		testPage.save();
		pages.put(GERMAN, testPage);

		Page testPageLanugageVariant = Creator.createLanguageVariant(t.getObject(Page.class, testPage.getId()), en);
		testPageLanugageVariant.setFilename(DUMMY_PAGE_NAME + ENGLISH_FILE_EXTENSION);
		testPageLanugageVariant.setSyncPageId(getInteger(testPage.getId(), -1));
		testPageLanugageVariant.setSyncTimestamp(new ContentNodeDate(1337));
		testPageLanugageVariant.save();
		pages.put(ENGLISH, testPageLanugageVariant);

		Page klingonPage = Creator.createLanguageVariant(t.getObject(Page.class, testPage.getId()), klingon);
		klingonPage.setFilename(DUMMY_PAGE_NAME + KLINGON_FILE_EXTENSION);
		klingonPage.setSyncPageId(getInteger(testPage.getId(), -1));
		klingonPage.setSyncTimestamp(new ContentNodeDate(42));
		klingonPage.save();
		pages.put(KLINGON, klingonPage);
		return pages;
	}

	/**
	 * Copy a page with multiple language variations to multiple target folders
	 * that have the same publish path. Make sure the filenames are updated
	 * correctly to avoid publish path collisions and check that the pagename
	 * stays the same.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishPathCollisionWithPageLanguageSetCreateCopyDisabled() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Map<String, Page> pages = createLanguageVariantPages();
		Folder targetFolder1 = Creator.createFolder(nodeA.getFolder(), "subfolder1", "/test");
		Folder targetFolder2 = Creator.createFolder(nodeA.getFolder(), "subfolder2", "/test");
		Folder targetFolder3 = Creator.createFolder(nodeA.getFolder(), "subfolder3", "/test");
		t.commit(false);

		// Setup copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		copyRequest.getSourcePageIds().add(getInteger(pages.get(GERMAN).getId(), -1));
		addTargetFolder(copyRequest, targetFolder1);
		addTargetFolder(copyRequest, targetFolder2);
		addTargetFolder(copyRequest, targetFolder3);

		// Invoke copy call
		copy(copyRequest, true);

		List<com.gentics.contentnode.rest.model.Page> expectedPages = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page page;

		// Check first folder. It should contain the three copied lanuguage
		// variations
		// English
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(ENGLISH).getName());
		page.setFileName(pages.get(ENGLISH).getFilename());
		page.setContentSetId((Integer) pages.get(ENGLISH).getContentsetId() + 1);
		expectedPages.add(page);

		// Klingon
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(KLINGON).getName());
		page.setFileName(pages.get(KLINGON).getFilename());
		page.setContentSetId((Integer) pages.get(KLINGON).getContentsetId() + 1);
		expectedPages.add(page);

		// German
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(GERMAN).getName());
		page.setFileName(pages.get(GERMAN).getFilename());
		page.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.add(page);

		assertCopiedPages(targetFolder1, expectedPages);

		// Check the second folder. Now each filename must be renamed again so
		// that the filenames are unique
		expectedPages.clear();

		// English
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(ENGLISH).getName());
		page.setFileName(DUMMY_PAGE_NAME + "1" + ENGLISH_FILE_EXTENSION);
		page.setContentSetId((Integer) pages.get(ENGLISH).getContentsetId() + 2);
		expectedPages.add(page);

		// Klingon
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(KLINGON).getName());
		page.setFileName(DUMMY_PAGE_NAME + "1" + KLINGON_FILE_EXTENSION);
		page.setContentSetId((Integer) pages.get(KLINGON).getContentsetId() + 2);
		expectedPages.add(page);

		// German
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(GERMAN).getName());
		page.setFileName(DUMMY_PAGE_FILENAME + "1" + GERMAN_FILE_EXTENSION);
		page.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 2);
		expectedPages.add(page);
		assertCopiedPages(targetFolder2, expectedPages);

		// Check the third folder. Filenames must be renamed again. Name must
		// stay the same
		expectedPages.clear();

		// English
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(ENGLISH).getName());
		page.setFileName(DUMMY_PAGE_NAME + "2" + ENGLISH_FILE_EXTENSION);
		page.setContentSetId((Integer) pages.get(ENGLISH).getContentsetId() + 3);
		expectedPages.add(page);

		// Klingon
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(KLINGON).getName());
		page.setFileName(DUMMY_PAGE_NAME + "2" + KLINGON_FILE_EXTENSION);
		page.setContentSetId((Integer) pages.get(KLINGON).getContentsetId() + 3);
		expectedPages.add(page);

		// German
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(pages.get(GERMAN).getName());
		page.setFileName(DUMMY_PAGE_FILENAME + "2" + GERMAN_FILE_EXTENSION);
		page.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 3);
		expectedPages.add(page);

		assertCopiedPages(targetFolder3, expectedPages);

	}

	/**
	 * Copy a page with multiple language variations to a target folder
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopyPageLanguageSet() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Map<String, Page> pages = createLanguageVariantPages();
		Folder targetFolder = Creator.createFolder(nodeA.getFolder(), "subfolder", "/test");
		t.commit(false);

		// Setup copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(pages.get(GERMAN).getId(), -1));
		addTargetFolder(copyRequest, targetFolder);

		// Invoke copy call
		copy(copyRequest, true);

		List<com.gentics.contentnode.rest.model.Page> expectedPages = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page expectedCopy;

		// German
		expectedCopy = new com.gentics.contentnode.rest.model.Page();
		expectedCopy.setFolderId(getInteger(targetFolder.getId(), -1));
		expectedCopy.setName("Kopie 3 von " + DUMMY_PAGE_NAME);
		expectedCopy.setFileName(DUMMY_PAGE_NAME + GERMAN_FILE_EXTENSION);
		expectedCopy.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.add(expectedCopy);

		// English
		expectedCopy = new com.gentics.contentnode.rest.model.Page();
		expectedCopy.setFolderId(getInteger(targetFolder.getId(), -1));
		expectedCopy.setName("Kopie 2 von " + DUMMY_PAGE_NAME);
		expectedCopy.setFileName(DUMMY_PAGE_NAME + ENGLISH_FILE_EXTENSION);
		expectedCopy.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.add(expectedCopy);

		// Klingon
		expectedCopy = new com.gentics.contentnode.rest.model.Page();
		expectedCopy.setFolderId(getInteger(targetFolder.getId(), -1));
		expectedCopy.setName("Kopie von " + DUMMY_PAGE_NAME);
		expectedCopy.setFileName(DUMMY_PAGE_NAME + KLINGON_FILE_EXTENSION);
		expectedCopy.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.add(expectedCopy);

		assertCopiedPages(targetFolder, expectedPages);
	}

	/**
	 * Copy a page with multiple language variations to a target folder. Disable
	 * create copy in order to test whether no new page language variations will
	 * be created in the second copy call.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopyPageLanguageSetCreateCopyDisabled() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup of the test fixture
		Map<String, Page> pages = createLanguageVariantPages();
		Folder targetFolder = Creator.createFolder(nodeA.getFolder(), "DummyFolder", "/test");
		t.commit(false);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		copyRequest.getSourcePageIds().add(getInteger(pages.get(GERMAN).getId(), -1));
		addTargetFolder(copyRequest, targetFolder);

		// Invoke copy call
		PageCopyResponse response = copy(copyRequest, true);
		assertEquals("Check whether correct number of copys have been made.", 3, response.getPages().size());

		Map<String, com.gentics.contentnode.rest.model.Page> expectedPages = new HashMap<String, com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page page;

		// German
		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + GERMAN_FILE_EXTENSION);
		page.setName(DUMMY_PAGE_NAME);
		page.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.put(page.getFileName(), page);

		// English
		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + ENGLISH_FILE_EXTENSION);
		page.setName(DUMMY_PAGE_NAME);
		page.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.put(page.getFileName(), page);

		// Klingon
		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + KLINGON_FILE_EXTENSION);
		page.setName(DUMMY_PAGE_NAME);
		page.setContentSetId((Integer) pages.get(GERMAN).getContentsetId() + 1);
		expectedPages.put(page.getFileName(), page);

		// Check whether the target folder contains the copy
		assertCopiedPages(targetFolder, new ArrayList<com.gentics.contentnode.rest.model.Page>(expectedPages.values()));

		// Check whether the returned page is the correct copy
		for (com.gentics.contentnode.rest.model.Page currentReponsePage : response.getPages()) {

			assertNotNull("Filename in the reponse should not be null.", currentReponsePage.getFileName());
			com.gentics.contentnode.rest.model.Page foundExpectedPage = expectedPages.get(currentReponsePage.getFileName());
			assertNotNull("Could not find any page in the response that matches filename {" + currentReponsePage.getFileName() + "}", foundExpectedPage);

			assertEquals("The name of the page that was added to the reponse does not match the expected copy.", foundExpectedPage.getName(), currentReponsePage.getName());
			assertEquals("The folder id of the page that was added to the reponse does not match the target folder id of the copy request.", targetFolder.getId(), currentReponsePage.getFolderId());
			expectedPages.remove(currentReponsePage.getFileName());
		}

		// Invoke copy call again
		response = copy(copyRequest, false);
		assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ")", ResponseCode.FAILURE, response.getResponseInfo().getResponseCode());

		CNI18nString expected = new CNI18nString("page_copy.name_conflict_detected");
		expected.addParameter(I18NHelper.getLocation(pages.get(KLINGON)));
		expected.addParameter(I18NHelper.getLocation(targetFolder));
		assertMessage(response, expected);
		assertEquals("Reponse did not contain the expected error message.", "Error during copy process. Please check the messages.", response.getResponseInfo()
				.getResponseMessage());
		assertEquals("Check whether correct number of copys have been made.", 0, response.getPages().size());

	}

	/**
	 * Copy pages from Node A and some of Node B to Folders in Node A and B.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCrossNodeBatchCopy2() throws Exception {

		// Setup test fixture
		Page testPageA = Creator.createPage(DUMMY_PAGE_NAME + "A", nodeA.getFolder(), template);
		Page testPageB = Creator.createPage(DUMMY_PAGE_NAME + "B", nodeA.getFolder(), template);
		Page testPageC = Creator.createPage(DUMMY_PAGE_NAME + "C", nodeA.getFolder(), template);
		Page testPageA2 = Creator.createPage(DUMMY_PAGE_NAME + "A1", nodeB.getFolder(), template);
		Page testPageB2 = Creator.createPage(DUMMY_PAGE_NAME + "B1", nodeB.getFolder(), template);
		Page testPageC2 = Creator.createPage(DUMMY_PAGE_NAME + "C1", nodeB.getFolder(), template);
		Folder nodeAfolderA = Creator.createFolder(nodeA.getFolder(), "subfolderA-nodea", "/1");
		Folder nodeBfolderB = Creator.createFolder(nodeB.getFolder(), "subfolderB-nodeb", "/1");

		// Create copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(testPageA.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageB.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageC.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageA2.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageB2.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(testPageC2.getId(), -1));
		addTargetFolder(copyRequest, nodeAfolderA);
		addTargetFolder(copyRequest, nodeBfolderB);

		// Invoke copy
		copy(copyRequest, true);

		List<com.gentics.contentnode.rest.model.Page> expectedPages = new ArrayList<com.gentics.contentnode.rest.model.Page>();

		com.gentics.contentnode.rest.model.Page page;

		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + "A.html");
		page.setName("Kopie von " + DUMMY_PAGE_NAME + "A");
		expectedPages.add(page);

		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + "B.html");
		page.setName("Kopie von " + DUMMY_PAGE_NAME + "B");
		expectedPages.add(page);

		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + "C.html");
		page.setName("Kopie von " + DUMMY_PAGE_NAME + "C");
		expectedPages.add(page);

		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + "A1.html");
		page.setName("Kopie von " + DUMMY_PAGE_NAME + "A1");
		expectedPages.add(page);

		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + "B1.html");
		page.setName("Kopie von " + DUMMY_PAGE_NAME + "B1");
		expectedPages.add(page);

		page = new com.gentics.contentnode.rest.model.Page();
		page.setFileName(DUMMY_PAGE_NAME + "C1.html");
		page.setName("Kopie von " + DUMMY_PAGE_NAME + "C1");
		expectedPages.add(page);

		assertCopiedPages(nodeAfolderA, expectedPages);

		assertCopiedPages(nodeBfolderB, expectedPages);
	}

	/**
	 * Copy a single page to another node
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCrossNodeSingleCopy() throws Exception {
		testContext.getContext().startTransaction();

		// Setup test fixture
		Page testPage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Folder targetFolder = Creator.createFolder(nodeB.getFolder(), "FolderInNodeB", "/test");

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.getSourcePageIds().add(getInteger(testPage.getId(), -1));
		addTargetFolder(copyRequest, targetFolder);
		copyRequest.setCreateCopy(ENABLED);

		// Invoke copy call
		copy(copyRequest, true);

		com.gentics.contentnode.rest.model.Page expectedPage = new com.gentics.contentnode.rest.model.Page();
		// No conflict = no filename changes
		expectedPage.setFileName(DUMMY_PAGE_FILENAME);

		// Check whether the "copy of.." prefix was added
		expectedPage.setName("Kopie von " + DUMMY_PAGE_NAME);
		assertCopiedPages(targetFolder, expectedPage);
	}

	/**
	 * Test whether a copy can be created in the parent folder of source page.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageCopyToSameFolder() throws Exception {

		Folder targetFolder = nodeA.getFolder();
		Transaction t = TransactionManager.getCurrentTransaction();
		// Setup the test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		t.commit(false);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		addTargetFolder(copyRequest, targetFolder);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(), -1));

		// Invoke copy call
		PageCopyResponse response = copy(copyRequest, true);
		assertEquals("Check whether correct number of copys have been made.", 1, response.getPages().size());

		List<com.gentics.contentnode.rest.model.Page> expectedCopys = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page page;
		// The new copy
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName("Kopie von " + DUMMY_PAGE_NAME);
		page.setFileName(DUMMY_PAGE_NAME + "1.html");
		expectedCopys.add(page);

		// The source page
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(DUMMY_PAGE_NAME);
		page.setFileName(DUMMY_PAGE_NAME + ".html");
		expectedCopys.add(page);

		assertCopiedPages(targetFolder, expectedCopys);
	}

	/**
	 * Test whether no copy will be created when a copy call is invoked which
	 * should copy a page to the parent folder of source page.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageCopyToSameFolderCreateCopyDisabled() throws Exception {

		Folder targetFolder = nodeA.getFolder();
		Transaction t = TransactionManager.getCurrentTransaction();
		// Setup the test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		t.commit(false);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		addTargetFolder(copyRequest, targetFolder);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(), -1));

		// Invoke copy call
		PageCopyResponse response = copy(copyRequest, false);
		assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ")", ResponseCode.FAILURE, response.getResponseInfo().getResponseCode());
		assertEquals("Reponse did not contain the expected error message.", "Error during copy process. Please check the messages.", response.getResponseInfo()
				.getResponseMessage());

		// Verify the i18n message
		CNI18nString expected = new CNI18nString("page_copy.name_conflict_detected");
		expected.addParameter(I18NHelper.getLocation(sourcePage));
		expected.addParameter(I18NHelper.getLocation(targetFolder));
		assertMessage(response, expected);

		assertEquals("Check whether correct number of copys have been made.", 0, response.getPages().size());

		// Only the source page should be located in the target folder
		com.gentics.contentnode.rest.model.Page expectedPage = new com.gentics.contentnode.rest.model.Page();
		expectedPage.setFileName(DUMMY_PAGE_FILENAME);
		expectedPage.setName(DUMMY_PAGE_NAME);
		assertCopiedPages(targetFolder, expectedPage);
	}

	@Test
	public void testPageCopyOfPageVariant() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup the test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Folder targetFolder = Creator.createFolder(nodeA.getFolder(), "TestFolder", "/blub");
		Page sourcePageVariant = sourcePage.createVariant();
		sourcePageVariant.setName(DUMMY_PAGE_NAME + "_variant");
		sourcePageVariant.setFilename(sourcePageVariant.getName() + ".html");
		sourcePageVariant.save();
		t.commit(false);

		assertTrue("Both content_ids must be the same. We need page variants here.", sourcePageVariant.getContent().getId() == sourcePage.getContent().getId());

		// Prepare copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		addTargetFolder(copyRequest, targetFolder);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(), -1));
		copyRequest.getSourcePageIds().add(getInteger(sourcePageVariant.getId(), -1));

		// Invoke copy call
		PageCopyResponse response = copy(copyRequest, true);
		assertEquals("Check whether correct number of copys have been made.", 2, response.getPages().size());

		List<com.gentics.contentnode.rest.model.Page> expectedCopys = new ArrayList<com.gentics.contentnode.rest.model.Page>();
		com.gentics.contentnode.rest.model.Page page;
		// The new copy
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(sourcePage.getName());
		page.setFileName(sourcePage.getFilename());
		expectedCopys.add(page);

		// The source page
		page = new com.gentics.contentnode.rest.model.Page();
		page.setName(sourcePageVariant.getName());
		page.setFileName(sourcePageVariant.getFilename());
		expectedCopys.add(page);

		assertCopiedPages(targetFolder, expectedCopys);
		int contentIdA = getInteger(targetFolder.getPages().get(0).getContent().getId(), -1);
		int contentIdB = getInteger(targetFolder.getPages().get(1).getContent().getId(), -1);
		assertTrue("The content_id of both created pages should not be the same.", contentIdA != contentIdB);

	}

	/**
	 * Test whether a copy call fails when trying to copy a page which the user
	 * can't read.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageCopyViewPermInSource() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Folder targetFolder = Creator.createFolder(nodeA.getFolder(), "Subfolder", null);
		t.commit(false);

		// Create group with create perm to target folder and no perm to source
		// folder
		UserGroup parentGroup = t.getObject(UserGroup.class, 2);
		UserGroup testUserGroup = Creator.createUsergroup("maingroup", "", parentGroup);
		SystemUser testUser = Creator.createUser("testuser", "asdf", "first", "last", "e@ma.il", Arrays.asList(new UserGroup[] { testUserGroup }));

		List<UserGroup> groupsWithNoPerm = Arrays.asList(new UserGroup[] { testUserGroup });
		PermHandler.setPermissions(Folder.TYPE_FOLDER, getInteger(targetFolder.getId(), -1), groupsWithNoPerm,
				new Permission(PermHandler.PERM_PAGE_CREATE).toString());
		PermHandler.setPermissions(Folder.TYPE_FOLDER, getInteger(nodeA.getFolder().getId(), -1), groupsWithNoPerm,
				new Permission(PermHandler.EMPTY_PERM).toString());
		PermissionStore.initialize(true);
		t.commit(false);
		testContext.getContext().startTransaction(getInteger(testUser.getId(), -1));

		// Prepare copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		addTargetFolder(copyRequest, targetFolder);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(), -1));

		// Invoke copy call
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		PageCopyResponse response = pageResource.copy(copyRequest, 0);
		assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ")", ResponseCode.FAILURE, response.getResponseInfo().getResponseCode());
		// "Error while copying pages: No view permission to load source page {"
		// + sourcePage + "} in folder {"+ nodeA.getFolder() + "}"
		assertEquals("Reponse did not contain the expected error message.", "Error during copy process. Please check the messages.", response.getResponseInfo()
				.getResponseMessage());

	}

	/**
	 * Test whether a copy call fails when the user has no create perms in the
	 * target folder.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageCopyNoPageCreateInTarget() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Folder sourceFolder = nodeA.getFolder();
		Folder targetFolder = nodeB.getFolder();

		// Create group with no create perm to target folder
		UserGroup parentGroup = t.getObject(UserGroup.class, 2);
		UserGroup testUserGroup = Creator.createUsergroup("maingroup", "", parentGroup);
		SystemUser testUser = Creator.createUser("testuser", "asdf", "first", "last", "e@ma.il", Arrays.asList(new UserGroup[] { testUserGroup }));

		List<UserGroup> testGroups = Arrays.asList(new UserGroup[] { testUserGroup });
		PermHandler.setPermissions(Node.TYPE_NODE, getInteger(sourceFolder.getId(), -1), testGroups, new Permission(PermHandler.FULL_PERM).toString());
		// PermHandler.setPermissions(Folder.TYPE_FOLDER,
		// sourceFolder.getId(), testGroups, new
		// Permission(PermHandler.FULL_PERM).toString());
		PermHandler.setPermissions(Folder.TYPE_FOLDER, getInteger(targetFolder.getId(), -1), testGroups, ONLY_VIEW_PERM);
		PermissionStore.initialize(true);
		t.commit(false);
		testContext.getContext().startTransaction(getInteger(testUser.getId(), -1));

		// Prepare copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		addTargetFolder(copyRequest, targetFolder);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(), -1));

		// Invoke copy call
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		PageCopyResponse response = pageResource.copy(copyRequest, 0);
		assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ")", ResponseCode.FAILURE, response.getResponseInfo().getResponseCode());
		assertEquals("Reponse did not contain the expected error message.", "Error during copy process. Please check the messages.", response.getResponseInfo()
				.getResponseMessage());
		assertEquals("The response should only contain the expected amount of messages.", 1, response.getMessages().size());

		CNI18nString i18nString = new CNI18nString("page_copy.missing_create_perms_for_target");
		i18nString.addParameter(I18NHelper.getLocation(targetFolder));
		assertMessage(response, i18nString);

	}
}