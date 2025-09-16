package com.gentics.contentnode.tests.rest.page;

import static com.gentics.api.lib.etc.ObjectTransformer.getInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.i18n.CNI18nString;

public abstract class AbstractPageCopyTest {

	protected static final boolean DISABLED = false;
	protected static final boolean ENABLED = true;
	protected static final String ENGLISH = "en1";
	protected static final String GERMAN = "de1";
	protected static final String KLINGON = "tlh";
	protected static final String ENGLISH_FILE_EXTENSION = "." + ENGLISH + ".html";
	protected static final String KLINGON_FILE_EXTENSION = "." + KLINGON + ".html";
	protected static final String GERMAN_FILE_EXTENSION = "." + GERMAN + ".html";

	@Rule
	public DBTestContext testContext = new DBTestContext(true);

	protected Node nodeA;
	protected Node nodeB;
	protected Template template;
	protected ContentLanguage de;
	protected ContentLanguage en;
	protected ContentLanguage klingon;
	protected static final String DUMMY_PAGE_NAME = "DummyPage";
	protected static final String DUMMY_PAGE_FILENAME = DUMMY_PAGE_NAME + ".html";

	protected static final String ONLY_VIEW_PERM = new Permission(PermHandler.PERM_VIEW).toString();
	protected static final String ONLY_PAGE_CREATE_PERM = new Permission(PermHandler.PERM_PAGE_CREATE).toString();

	protected static final List<com.gentics.contentnode.rest.model.Page> EMPTY_LIST_OF_PAGES = new ArrayList<com.gentics.contentnode.rest.model.Page>();

	/**
	 * Initializes the client and the WebResource
	 */
	@Before
	public void setUp() throws Exception {

		Transaction t = TransactionManager.getCurrentTransaction();

		nodeA = ContentNodeTestDataUtils.createNode("testnode", "www.testnode.at", "/test", "/test", false, false);
		template = Creator.createTemplate("testTemplate", "blabla", nodeA.getFolder());
		nodeB = ContentNodeTestDataUtils.createNode("testnode2", "www.testnode2.at", "/test", "/test", false, false);

		en = Creator.createLanguage("English", ENGLISH);
		en.getNodes().add(nodeA);
		en.getNodes().add(nodeB);
		en.save();

		de = Creator.createLanguage("German", GERMAN);
		de.getNodes().add(nodeA);
		de.getNodes().add(nodeB);
		de.save();

		klingon = Creator.createLanguage("Klingon", KLINGON);
		klingon.getNodes().add(nodeA);
		klingon.getNodes().add(nodeB);
		klingon.save();

		t.commit(false);
	}

	/**
	 * Add given folder to target folders of the request object
	 * 
	 * @param copyRequest
	 * @param folder
	 * @throws NodeException
	 */
	protected void addTargetFolder(PageCopyRequest copyRequest, Folder folder) throws NodeException {
		TargetFolder targetFolder = new TargetFolder();
		targetFolder.setId(getInteger(folder.getId(), -1));
		if (folder.getChannel() == null) {
			targetFolder.setChannelId(0);
		} else {
			targetFolder.setChannelId(getInteger(folder.getChannel().getId(), -1));
		}
		copyRequest.getTargetFolders().add(targetFolder);
	}

	/**
	 * Add given folder to target folders of the request object
	 * 
	 * @param copyRequest
	 * @param folder
	 */
	protected void addTargetFolder(PageCopyRequest copyRequest, com.gentics.contentnode.rest.model.Folder folder) {
		TargetFolder targetFolder = new TargetFolder();
		targetFolder.setId(folder.getId());
		targetFolder.setChannelId(folder.getChannelId());
		copyRequest.getTargetFolders().add(targetFolder);
	}

	/**
	 * Helper method that check whether folder contains the set of pages.
	 * 
	 * @param rootFolder
	 * @param expectedPages
	 * @throws NodeException
	 */
	protected void assertCopiedPages(final Folder rootFolder, com.gentics.contentnode.rest.model.Page... expectedPages) throws NodeException {
		List<com.gentics.contentnode.rest.model.Page> listOfExpectedPages = Arrays.asList(expectedPages);
		assertCopiedPages(rootFolder, listOfExpectedPages);
	}

	/**
	 * Locate the page by examining all pages in the found folder and returning
	 * the found page.
	 * 
	 * @param sourceFolder
	 * @param filename
	 * @return returns the found page otherwise null
	 * @throws NodeException
	 */
	private Page locatePageByFilename(Folder sourceFolder, String filename) throws NodeException {
		Page locatedPage = null;

		String pages = getPageListForFolder(sourceFolder);
		for (Page currentPage : sourceFolder.getPages()) {
			if (filename.equalsIgnoreCase(currentPage.getFilename())) {
				if (locatedPage != null) {
					throw new NodeException("The folder {" + sourceFolder + "} does contain two files with the same filename. This should never happen." + pages);
				}
				locatedPage = currentPage;
			}
		}
		return locatedPage;
	}

	/**
	 * Helper method that checks whether the given list of expected pages can be
	 * found in the root folder. The check uses the name of the pages for
	 * comparison.
	 * 
	 * @param rootFolder
	 * @param expectedPages
	 * @throws NodeException
	 */
	protected void assertCopiedPages(final Folder rootFolder, final List<com.gentics.contentnode.rest.model.Page> expectedPages) throws NodeException {
		final int PAGE_COUNT = expectedPages.size();

		String pages = getPageListForFolder(rootFolder);
		// Validate expected data
		Vector<String> names = new Vector<>();
		for (com.gentics.contentnode.rest.model.Page page : expectedPages) {
			if (StringUtils.isEmpty(page.getFileName())) {
				fail("The filename of page {" + page.getName() + "} is empty. Please add the filename to the testdata.");
			}
			if (names.contains(page.getFileName())) {
				fail("The list of exepected pages for the folder {" + rootFolder + "} does contain pages with duplicate filenames. Conflicting page {" + page.getName()
						+ "} - filename {" + page.getFileName() + "} . This assumption is never valid. Please fix the test assertion.");
			} else {
				names.add(page.getFileName());
			}
		}

		assertEquals("The folder does not contain {" + PAGE_COUNT + "} pages. The following pages could be found:\n" + pages, PAGE_COUNT, rootFolder.getPages().size());

		for (com.gentics.contentnode.rest.model.Page currentExpectedPage : expectedPages) {

			Page locatedPage = locatePageByFilename(rootFolder, currentExpectedPage.getFileName());
			if (locatedPage == null) {
				fail("Page Name: {" + currentExpectedPage.getName() + "}, Filename: {" + currentExpectedPage.getFileName() + "} could not be found in the target folder {"
						+ rootFolder + "}. Only the following pages could be found:\n" + pages);
			} else {
				assertTrue("Found page {" + currentExpectedPage.getFileName() + "} in target folder but the name did not match. Folder contains the following pages:\n" + pages,
						locatedPage.getName().contains(currentExpectedPage.getName()));
				if (currentExpectedPage.getContentSetId() != null) {
					assertEquals("The contentset_id of the found page {" + locatedPage + "} did not match the expected value.", currentExpectedPage.getContentSetId(),
							locatedPage.getContentsetId());
				}
				if (currentExpectedPage.getChannelId() != null) {
					assertEquals("The channel_id of the found page {" + locatedPage + "} did not match the expected value.", currentExpectedPage.getChannelId(), locatedPage
							.getChannel().getId());
				}
				if (currentExpectedPage.getFolderId() != null) {
					assertEquals("The folder_id of the found page {" + locatedPage + "} did not match the expected value.", currentExpectedPage.getFolderId(),
							locatedPage.getFolderId());
				}
				break;

			}
		}

	}

	private String getPageListForFolder(Folder rootFolder) throws NodeException {
		String pages = new String();
		for (Page page : rootFolder.getPages()) {
			pages += "Name: {" + page.getName() + "}, Filename: {" + page.getFilename() + "}\n";
		}
		return "\n" + pages;

	}

	/**
	 * Compare the message string within the response.
	 * 
	 * @param response
	 * @param expected
	 */
	protected void assertMessage(PageCopyResponse response, CNI18nString expected) {
		assertEquals("The message did not match the expected one.", expected.toString(), response.getMessages().get(0).getMessage());
	}


	/**
	 * Do the copy call (in a new transaction).
	 * @param request copy request
	 * @param assertResponseOk true to assert that the response was OK
	 * @return response
	 * @throws Exception
	 */
	protected PageCopyResponse copy(PageCopyRequest request, boolean assertResponseOk) throws Exception {
		testContext.getContext().startTransaction();
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		PageCopyResponse response = pageResource.copy(request, 0);
		testContext.getContext().startTransaction();
		if (assertResponseOk) {
			ContentNodeRESTUtils.assertResponseOK(response);
		}
		return response;
	}
}
