/**
 *
 */
package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.Executable;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.TranslationStatus;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.rendering.ContentNodeTestContext;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * REST API Tests for:
 * <ul>
 * <li>Creating translations of pages</li>
 * <li>Get synchronization status of pages</li>
 * <li>Synchronizing pages with special versions of language variants</li>
 * <li>Filtering for pages by translation synchronization status</li>
 * <li>Inbox Message, when synchronous translation master of a page changes</li>
 * </ul>
 */
public class PageTranslationSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * folder id of the folder where the tests are done
	 */
	public final static Integer FOLDER_ID = 28;

	/**
	 * Test translating a page via the REST API into another language variant.
	 * @throws NodeException
	 */
	@Test
	public void testTranslatePage() throws NodeException {
		// create a new page (in "de")
		Page germanPage = createPage("de");

		// translate the page via REST API into "en"
		Page englishPage = translatePage(germanPage, "en");

		// load the english page again (with translation status)
		englishPage = loadPageWithTranslationStatus(englishPage.getId());
		TranslationStatus translationStatus = englishPage.getTranslationStatus();

		// check that the translated page is synchronized with the original
		// page, version timestamp 0 (and insync is "false")
		assertNotNull("Check translation status", translationStatus);
		assertEquals("Check translation master", germanPage.getId(), translationStatus.getPageId());
		assertEquals("Check sync timestamp", new Integer(0), translationStatus.getVersionTimestamp());
		assertEquals("Check sync status", false, translationStatus.isInSync());
	}

	/**
	 * Test "translating" a page twice
	 * @throws NodeException
	 */
	@Test
	public void testTranslatePageTwice() throws NodeException {
		// create a new page (in "de")
		Page germanPage = createPage("de");

		// translate the page via REST API into "en"
		Page englishPage = translatePage(germanPage, "en");

		// translate the page again
		Page englishPage2 = translatePage(germanPage, "en");

		// check that the pages are the same (have the same ids)
		assertEquals("Check twice translated page", englishPage.getId(), englishPage2.getId());
	}

	/**
	 * Test "translating" a page that also has a deleted language variant of the same
	 * language in the wastebin (the deleted one has to be first in the database)
	 * @throws Exception
	 */
	@Test
	public void testWithDeletedVariant() throws Exception {
		com.gentics.contentnode.object.Page page  = null;
		com.gentics.contentnode.object.Page page2 = null;
		ContentLanguage klingon                   = null;

		ContentNodeTestContext ctx = testContext.getContext();
		ctx.setFeature(Feature.WASTEBIN, true);

		try (Trx trx = new Trx()) {
			klingon = Creator.createLanguage("klingon", "tlh");

			com.gentics.contentnode.object.Node node
				= ContentNodeTestDataUtils.createNode("node", "node", PublishTarget.NONE, false, false, klingon, klingon);

			Folder folder = node.getFolder();

			// Create pages
			Template template = ContentNodeTestDataUtils.createTemplate(folder, "template");
			page = ContentNodeTestDataUtils.createPage(folder, template, "newpage", null, klingon);
			page2 = (com.gentics.contentnode.object.Page)page.copy();
			page2.save();

			// Deleted the first page
			page.delete(false);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			PageResourceImpl pageResource = new PageResourceImpl();
			PageLoadResponse pageLoadResponse = pageResource.translate(page2.getId(), "tlh", false, null);
			ContentNodeTestUtils.assertResponseCodeOk(pageLoadResponse);

			assertEquals("There must not have been created a new page", pageLoadResponse.getPage().getId(), page2.getId());

			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				page = trx.getTransaction().getObject(page);
				assertNull("There should be no language variants in the wastebin now", page);

				page2 = trx.getTransaction().getObject(page2);
				assertEquals("There should be 1 language variant only", 1, page2.getLanguageVariants(false).size());
			}
		}
	}

	/**
	 * Test synchronizing a page with another language variant
	 * @throws NodeException
	 */
	@Test
	public void testSynchronize() throws NodeException {
		// create a new page (in "de")
		Page germanPage = createPage("de");

		// translate the page via REST API into "en"
		Page englishPage = translatePage(germanPage, "en");

		// load the english page again (with translation status)
		englishPage = loadPageWithTranslationStatus(englishPage.getId());
		TranslationStatus translationStatus = englishPage.getTranslationStatus();

		// check whether the page is not in sync
		assertEquals("Check sync status", false, translationStatus.isInSync());

		// synchronize the page with the current version of the other page
		translationStatus.setVersionTimestamp(translationStatus.getLatestVersion().getVersionTimestamp());
		savePage(englishPage, true);

		// load the english page again (with translation status)
		englishPage = loadPageWithTranslationStatus(englishPage.getId());
		translationStatus = englishPage.getTranslationStatus();

		// check whether the page is in sync now
		assertEquals("Check sync status", true, translationStatus.isInSync());
	}

	/**
	 * Test filtering pages in folder by their translation status
	 * @throws NodeException
	 */
	@Test
	public void testFilterByTranslationStatus() throws NodeException {
		// create a new page (in "de")
		Page germanPage = createPage("de");

		// translate the page via REST API into "en"
		Page englishPage = translatePage(germanPage, "en");

		// now load all pages from the folder
		Pair<Boolean, Boolean> checkResult = checkPagesByTranslationStatus(
			loadPages(null),
			germanPage.getId(),
			englishPage.getId(),
			page -> assertNull("Check whether translationstatus is null", page.getTranslationStatus()));

		// check whether only the expected pages were found
		assertTrue("Check whether page in sync was found in all pages", checkResult.getLeft());
		assertTrue("Check whether page out of sync was found in all pages", checkResult.getRight());

		// Check again with legacy endpoints.
		checkResult = checkPagesByTranslationStatus(
			loadPagesLegacy(null),
			germanPage.getId(),
			englishPage.getId(),
			page -> assertNull("Check whether translationstatus is null (legacy)", page.getTranslationStatus()));

		// check whether only the expected pages were found
		assertTrue("Check whether page in sync was found in all pages (legacy)", checkResult.getLeft());
		assertTrue("Check whether page out of sync was found in all pages (legacy)", checkResult.getRight());

		// now fetch only pages in sync
		checkResult = checkPagesByTranslationStatus(
			loadPages(true),
			germanPage.getId(),
			englishPage.getId(),
			page -> {
				assertNotNull("Check whether translationstatus is not null", page.getTranslationStatus());
				assertTrue("Check whether page is in sync", page.getTranslationStatus().isInSync());
			});

		// check whether only the expected pages were found
		assertTrue("Check whether page in sync was found when filtering with insync=true", checkResult.getLeft());
		assertFalse("Check whether page out of sync was not found when filtering with insync=true", checkResult.getRight());

		// Check again with legacy endpoints.
		checkResult = checkPagesByTranslationStatus(
			loadPagesLegacy(true),
			germanPage.getId(),
			englishPage.getId(),
			page -> {
				assertNotNull("Check whether translationstatus is not null (legacy)", page.getTranslationStatus());
				assertTrue("Check whether page is in sync (legacy)", page.getTranslationStatus().isInSync());
			});

		// check whether only the expected pages were found
		assertTrue("Check whether page in sync was found when filtering with insync=true (legacy)", checkResult.getLeft());
		assertFalse("Check whether page out of sync was not found when filtering with insync=true (legacy)", checkResult.getRight());

		// now fetch only pages out of sync
		checkResult = checkPagesByTranslationStatus(
			loadPages(false),
			germanPage.getId(),
			englishPage.getId(),
			page -> {
				assertNotNull("Check whether translationstatus is not null", page.getTranslationStatus());
				assertFalse("Check whether page is out of sync", page.getTranslationStatus().isInSync());
			});

		// check whether only the expected pages were found
		assertFalse("Check whether page in sync was not found when filtering with insync=false", checkResult.getLeft());
		assertTrue("Check whether page out of sync was found when filtering with insync=false", checkResult.getRight());

		// Check again with legacy endpoints.
		checkResult = checkPagesByTranslationStatus(
			loadPagesLegacy(false),
			germanPage.getId(),
			englishPage.getId(),
			page -> {
				assertNotNull("Check whether translationstatus is not null (legacy)", page.getTranslationStatus());
				assertFalse("Check whether page is out of sync (legacy)", page.getTranslationStatus().isInSync());
			});

		// check whether only the expected pages were found
		assertFalse("Check whether page in sync was not found when filtering with insync=false (legacy)", checkResult.getLeft());
		assertTrue("Check whether page out of sync was found when filtering with insync=false (legacy)", checkResult.getRight());
	}

	/**
	 * Check if the list of pages contains the given german and english page IDs, and if each page satisfies the per page check.
	 * @param pages List of pages to check
	 * @param germanPageId The ID of the expected german page
	 * @param englishPageId The ID of the expected english page
	 * @param perPageCheck A consumer which makes additional assertions per page
	 * @return A tuple where the left element indicates whether the german page was in the list, and the right element
	 * 		indicates wheter the english version was in the list
	 * @throws NodeException
	 */
	private Pair<Boolean, Boolean> checkPagesByTranslationStatus(List<Page> pages, Integer germanPageId, Integer englishPageId, Consumer<Page> perPageCheck) throws NodeException {
		boolean foundGermanPage = false;
		boolean foundEnglishPage = false;

		for (Page page : pages) {
			if (page.getId().equals(germanPageId)) {
				foundGermanPage = true;
			} else if (page.getId().equals(englishPageId)) {
				foundEnglishPage = true;
			}

			perPageCheck.accept(page);
		}

		return Pair.of(foundGermanPage, foundEnglishPage);
	}

	/**
	 * Test changing the translation master of a page (inbox message and sync
	 * status change)
	 * @throws NodeException
	 */
	@Test
	public void testChangeTranslationMaster() throws NodeException {
		// create a new page (in "de")
		Page germanPage = createPage("de");

		// translate the page via REST API into "en"
		Page englishPage = translatePage(germanPage, "en");

		// load the english page again (with translation status)
		englishPage = loadPageWithTranslationStatus(englishPage.getId());
		TranslationStatus translationStatus = englishPage.getTranslationStatus();

		// synchronize the page with the current version of the other page
		translationStatus.setVersionTimestamp(translationStatus.getLatestVersion().getVersionTimestamp());
		savePage(englishPage, true);

		// Update the transaction timestamp (so that we are sure to get another version timestamp)
		TransactionManager.getCurrentTransaction().setTimestamp(TransactionManager.getCurrentTransaction().getTimestamp() + 2000);

		// now we change the translation master (e.g. the name)
		germanPage.setName("This is a new name!");
		savePage(germanPage, true);

		// now get the english page again and check whether it is out of sync now
		englishPage = loadPageWithTranslationStatus(englishPage.getId());
		translationStatus = englishPage.getTranslationStatus();
		assertFalse("Check whether the page is out of sync now", translationStatus.isInSync());
		assertFalse("Check whether the sync'ed version timestamp is different from the latest now",
				translationStatus.getVersionTimestamp() == translationStatus.getLatestVersion().getVersionTimestamp());
		assertFalse("Check whether the sync'ed versions timestamp is different from the latest now",
				translationStatus.getVersion().equals(translationStatus.getLatestVersion().getVersion()));
	}

	/**
	 * Test translating a page in multiple threads
	 * @throws Exception
	 */
	@Test
	public void testMultithreadedTranslatePage() throws Exception {
		// create a new page (in "de")
		Page germanPage = createPage("de");
		TransactionManager.getCurrentTransaction().commit(false);

		int numThreads = 10;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		List<TranslateThread> threads = new ArrayList<PageTranslationSandboxTest.TranslateThread>(numThreads);

		for (int i = 0; i < numThreads; i++) {
			TranslateThread t = new TranslateThread(germanPage, "en");
			threads.add(t);
			executor.execute(t);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}

		Set<Integer> ids = new HashSet<Integer>();
		for (TranslateThread t : threads) {
			if (t.e != null) {
				throw t.e;
			}

			ids.add(t.translation.getId());
		}

		assertTrue("There should be exactly one translation, but IDs of translations were: " + ids, ids.size() == 1);
	}

	/**
	 * Helper method to create a page via REST API
	 *
	 * @param language
	 *            language of the page to create
	 * @return the created page
	 */
	protected Page createPage(String language) throws TransactionException {
		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		// create the request to create a page
		PageCreateRequest request = new PageCreateRequest();

		request.setFolderId(Integer.toString(FOLDER_ID));
		request.setLanguage(language);
		// post the request and get the response
		PageLoadResponse loadResponse = pageResource.create(request);

		TransactionManager.getCurrentTransaction().commit(false);

		// check whether the action succeeded
		assertSuccess(loadResponse);

		// check whether a page was returned
		assertNotNull("Check the created page", loadResponse.getPage());

		// check the language of the returned page
		assertEquals("Check language of created page", language, loadResponse.getPage().getLanguage());

		// return the created page
		return loadResponse.getPage();
	}

	/**
	 * Helper method to translate a page via REST API into the given language
	 *
	 * @param page
	 *            page to translate
	 * @param language
	 *            language of the translated page
	 * @return translated page
	 */
	protected Page translatePage(Page page, String language) throws NodeException {
		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		// post the request and get the response
		PageLoadResponse loadResponse = pageResource.translate(page.getId(), language, true, 0);

		// check whether the action succeeded
		assertSuccess(loadResponse);

		// check whether a page was returned
		assertNotNull("Check the translated page", loadResponse.getPage());

		// check the language of the returned page
		assertEquals("Check language of translated page", language, loadResponse.getPage().getLanguage());

		// Check whether the pages are language variants of each other
		assertEquals("Check contentset_id of translated page", page.getContentSetId(), loadResponse.getPage().getContentSetId());

		testContext.startSystemUserTransaction();

		// return the translated page
		return loadResponse.getPage();
	}

	/**
	 * Load the page with given id, including the translation status
	 *
	 * @param id
	 *            id of the page to load
	 * @return loaded page (might be null)
	 * @throws TransactionException
	 */
	protected Page loadPageWithTranslationStatus(Integer id) throws TransactionException {
		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		PageLoadResponse loadResponse = pageResource.load(String.valueOf(id), false, false, false, false, false, false, true, false, false, false, null, null);

		// check whether the action succeeded
		assertSuccess(loadResponse);

		// return the loaded page
		return loadResponse.getPage();
	}

	/**
	 * Helper method to save the given page
	 * @param page page to save
	 * @param unlock true if the page shall be unlocked, false if not
	 */
	protected void savePage(Page page, boolean unlock) throws NodeException {
		PageSaveRequest saveRequest = new PageSaveRequest(page);
		saveRequest.setUnlock(unlock);

		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		GenericResponse response = 	pageResource.save(page.getId().toString(), saveRequest);

		// check whether the action succeeded
		assertSuccess(response);
	}

	/**
	 * Load pages from the folder via legacy endpoints
	 *
	 * @param syncStatus
	 *            Boolean.TRUE if only pages in sync shall be fetched,
	 *            Boolean.FALSE for only fetching pages not in sync. NULL to
	 *            fetch all pages
	 * @return list of pages
	 */
	protected List<Page> loadPagesLegacy(Boolean syncStatus) throws NodeException {
		List<Page> pages = new ArrayList<>();
		String[] langCodes = { "de", "en"};

		for (String langCode : langCodes) {
			// first get pages in "de"
			LegacyPageListResponse listResponse = ContentNodeRESTUtils.getFolderResource().getPages(
				FOLDER_ID.toString(),
				new InFolderParameterBean().setFolderId(FOLDER_ID.toString()),
				new PageListParameterBean().setLanguage(langCode).setInSync(syncStatus),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new PublishableParameterBean(),
				new WastebinParameterBean());

			// check whether the action succeeded
			assertSuccess(listResponse);

			// return the fetched pages
			pages.addAll(listResponse.getPages());
		}

		return pages;
	}

	/**
	 * Load pages from the folder
	 *
	 * @param syncStatus
	 *            Boolean.TRUE if only pages in sync shall be fetched,
	 *            Boolean.FALSE for only fetching pages not in sync. NULL to
	 *            fetch all pages
	 * @return list of pages
	 */
	protected List<Page> loadPages(Boolean syncStatus) throws NodeException {
		List<Page> pages = new ArrayList<>();
		String[] langCodes = { "de", "en"};

		for (String langCode : langCodes) {
			// first get pages in "de"
			PageListResponse listResponse = ContentNodeRESTUtils.getPageResource().list(
					new InFolderParameterBean().setFolderId(FOLDER_ID.toString()),
					new PageListParameterBean().setLanguage(langCode).setInSync(syncStatus),
					new FilterParameterBean(),
					new SortParameterBean(),
					new PagingParameterBean(),
					new PublishableParameterBean(),
					new WastebinParameterBean());

			// check whether the action succeeded
			assertSuccess(listResponse);

			// return the fetched pages
			pages.addAll(listResponse.getItems());
		}

		return pages;
	}

	/**
	 * Helper method to check the response for response code OK
	 * @param response response to check
	 */
	protected void assertSuccess(GenericResponse response) {
		assertEquals("Check the response code (response message is " + response.getResponseInfo().getResponseMessage() + ")", ResponseCode.OK,
				response.getResponseInfo().getResponseCode());
	}

	/**
	 * Runnable that will translate the given page
	 */
	protected class TranslateThread implements Runnable {
		/**
		 * Transaction
		 */
		protected Transaction t;

		/**
		 * Exception
		 */
		protected Exception e;

		/**
		 * Language of the translation
		 */
		protected String language;

		/**
		 * Page to translate
		 */
		protected Page page;

		/**
		 * Translation
		 */
		protected Page translation;

		/**
		 * Create an instance of the Runnable
		 * @param page page to translate
		 * @param language target language
		 * @throws TransactionException
		 */
		public TranslateThread(Page page, String language) throws TransactionException {
			this.page = page;
			this.language = language;
			this.t = TransactionManager.getCurrentTransaction();
}

		@Override
		public void run() {
			try {
				TransactionManager.setCurrentTransaction(t);
				TransactionManager.execute(new Executable() {
					@Override
					public void execute() throws NodeException {
						translation = translatePage(page, language);
					}
				});
			} catch (Exception e) {
				this.e = e;
			}
		}
	}
}
