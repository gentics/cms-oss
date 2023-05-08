package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests saving of a page into GCN by using the REST API.
 *
 * @author floriangutmann
 */
public class PageSaveSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * The Node used for testing
	 */
	private Node node;

	/**
	 * The Template used for testing.
	 */
	private Template template;

	/**
	 * Base URI of the page resource
	 */
	public static String BASE_URI = null;

	/**
	 * Initializes the client and the WebResource
	 */
	@Before
	public void setUp() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		node = ContentNodeTestDataUtils.createNode("pagesavesandboxtestnode",
				"www.pagesavesandboxtestnode.at", "/test", "/testbin", false,
				false);

		template = Creator.createTemplate("pagesavesandboxtesttemplate", "blabla", node.getFolder());

		t.commit(false);
	}
	/**
	 * Create a page containing an empty overview for the testSaveWithEmptyOverview and testLoadEmptyOverview Tests
	 *
	 * @return the newly created page object
	 * @throws Exception
	 */
	private com.gentics.contentnode.object.Page createPageWithEmptyOverview() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		// Create a construct with
		Part overviewPart = Creator.createOverviewPart("overviewpart", 1);
		Construct construct = Creator.createConstruct("emptyOverviewTest", "emptyOverviewTest", "emptyOverviewTest", Arrays.asList(new Part[] {overviewPart}));
		// Create a template to make page creation possible
		Template template = Creator.createTemplate("emptyOverviewTest", "test", node.getFolder());
		t.commit(false);

		// create a page containing the tag
		com.gentics.contentnode.object.Page page = Creator.createPage("emptyOverviewTest", node.getFolder(), template, null);
		page.getContent().addContentTag((Integer) construct.getId());
		page.save();
		t.commit(false);
		return page;
	}

	/**
	 * Test if a page can be saved via the restapi even when the overview property of a overview tagpart is missing
	 * @throws Exception
	 */
	@Test
	public void testSaveWithEmptyOverview() throws Exception {
		com.gentics.contentnode.object.Page page = createPageWithEmptyOverview();

		// request the page via the rest api
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		Page restPage = pageResource.load(Integer.toString((Integer) page.getId()), false, false, false, false, false, false, false, false, false, false, (Integer) node.getId(), null).getPage();
		// make sure the overviewpart is empty (because the page was requested with update=false and was never edited before)
		assertNull("If page load is called with update=false the overview property of the overview tag part should be null",restPage.getTags().get("emptyOverviewTest1").getProperties().get("overviewpart").getOverview());
		// The page save request should not throw an exception
		PageSaveRequest pageSaveRequest = new PageSaveRequest(restPage);
		// page save should be successful (no error should be thrown!)
		pageResource.save(Integer.toString((Integer) page.getId()), pageSaveRequest);
	}

	/**
	 * Test if overviews are included in the rest response when a page was never edited before
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoadEmptyOverview() throws Exception {
		com.gentics.contentnode.object.Page page = createPageWithEmptyOverview();

		// request the page via the rest api
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		Page restPage = pageResource.load(Integer.toString((Integer) page.getId()), true, false, false, false, false, false, false, false, false, false, (Integer) node.getId(), null).getPage();
		assertNotNull("If page load is called with update=true the overview property of the overview tag part should not be null", restPage.getTags().get("emptyOverviewTest1").getProperties().get("overviewpart").getOverview());
	}

	/**
	 * Test page with invalid data (name, filename)
	 *
	 * @throws Exception
	 */
	@Test
	public void testInvalidPageData() throws Exception {
		testContext.getContext().startTransaction();

		PageCreateRequest pageCreateRequest = new PageCreateRequest();
		pageCreateRequest.setFolderId(node.getFolder().getId().toString());

		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		Page testPage = pageResource.create(pageCreateRequest).getPage();

		Page invalidDataPage = new Page();
		String description = "description was updated!";

		invalidDataPage.setId(testPage.getId());
		invalidDataPage.setName("");
		invalidDataPage.setFileName("");
		invalidDataPage.setDescription(description);

		PageSaveRequest pageSaveRequest = new PageSaveRequest(invalidDataPage);
		GenericResponse saveResponse = pageResource.save(testPage.getId()
				.toString(), pageSaveRequest);

		assertEquals("Saving should be successful", saveResponse
				.getResponseInfo().getResponseCode(), ResponseCode.OK);

		Page resultPage = pageResource.load(testPage.getId().toString(), false,
				false, false, false, false, false, false, false, false,
				false, ObjectTransformer.getInteger(node.getId(), -1), null).getPage();

		assertEquals("Description should be updated",
				resultPage.getDescription(), description);
		assertEquals("Name should NOT have been updated", testPage.getName(),
				resultPage.getName());
		assertEquals("Filename should NOT have been updated",
				testPage.getFileName(), resultPage.getFileName());
	}

	/**
	 * Tests if a 404 Error is returned when bogus page id is specified
	 */
	@Test
	public void testNoPage() throws Exception {
		testContext.getContext().startTransaction();

		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		PageLoadResponse pageLoad = pageResource.load("-42", false, false,
				false, false, false, false, false, false, false, false, null, null);

		assertEquals("Checking for correct response code",
				ResponseCode.NOTFOUND, pageLoad.getResponseInfo()
						.getResponseCode());
	}

	/**
	 * Tests a successful page save request
	 *
	 * @throws Exception
	 */
	@Test
	public void testSuccessfulSave() throws Exception {
		testContext.getContext().startTransaction();

		PageCreateRequest pageCreateRequest = new PageCreateRequest();
		pageCreateRequest.setFolderId(node.getFolder().getId().toString());

		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		Page testPage = pageResource.create(pageCreateRequest).getPage();

		String name = "90a8hgasfjnasldfhasdf";
		testPage.setName(name);

		GenericResponse response = pageResource.save(testPage.getId()
				.toString(), new PageSaveRequest(testPage));

		assertEquals("Response code should be OK", ResponseCode.OK, response
				.getResponseInfo().getResponseCode());

		Page checkPage = pageResource.load(testPage.getId().toString(), false,
				false, false, false, false, false, false, false, false, false, null, null)
				.getPage();

		assertEquals("Name should be updated", checkPage.getName(), name);
	}

	/**
	 * Tests whether page filename is generated from page name when
	 * {@link PageSaveRequest#deriveFileName} is set.
	 *
	 * The {@link PageSaveRequest} has the <code>deriveFileName</code>
	 * flag set, and contains a file with a new page name, but without
	 * a filename. After saving this page, the filename should be
	 * automatically generated from the page name.
	 *
	 * @throws Exception On test failure.
	 */
	@Test
	public void testDeriveFilenameWithoutFilename() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Page originalPage = Creator.createPage("Original Name",
			node.getFolder(),
			template);
		Integer id = originalPage.getId();

		t.commit(false);

		Page newPage = new Page();

		newPage.setId(id);
		newPage.setName("New Pagename");
		newPage.setFileName("");

		PageSaveRequest saveRequest = new PageSaveRequest(newPage);

		saveRequest.setDeriveFileName(true);

		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		GenericResponse saveResponse = pageResource.save(id.toString(),saveRequest);

		assertEquals("Saving should be successful",
			saveResponse.getResponseInfo().getResponseCode(),
			ResponseCode.OK);

		PageLoadResponse loadResponse = pageResource.load(id.toString(),
			false, false, false, false, false, false, false, false, false,
			false, node.getId(), null);

		assertEquals("Empty filename should have been fixed",
			loadResponse.getPage().getFileName(),
			"New-Pagename.html");
	}

	/**
	 * Tests whether given filename is used when
	 * {@link PageSaveRequest#deriveFileName} is set.
	 *
	 * The {@link PageSaveRequest} has the <code>deriveFileName</code>
	 * flag set, and contains a file with a new page- and filename. After
	 * saving this page, the filename should be the name provided in
	 * the save request.
	 *
	 * @throws Exception On test failure.
	 */
	@Test
	public void testDeriveFilenameWithFilename() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Page originalPage = Creator.createPage("Original Name",
			node.getFolder(),
			template);
		Integer id = originalPage.getId();

		t.commit(false);

		String expectedFilename = "expected-filename.en.html";
		Page newPage = new Page();

		newPage.setId(id);
		newPage.setName("New Pagename");
		newPage.setFileName(expectedFilename);

		PageSaveRequest saveRequest = new PageSaveRequest(newPage);

		saveRequest.setDeriveFileName(true);

		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		GenericResponse saveResponse = pageResource.save(id.toString(),saveRequest);

		assertEquals("Saving should be successful",
			saveResponse.getResponseInfo().getResponseCode(),
			ResponseCode.OK);

		PageLoadResponse loadResponse = pageResource.load(id.toString(),
			false, false, false, false, false, false, false, false, false,
			false, node.getId(), null);

		assertEquals("Filename should have been updated normally",
				loadResponse.getPage().getFileName(),
				expectedFilename);
	}

	/**
	 * Tests whether page filename is untouched when
	 * {@link PageSaveRequest#deriveFileName} is <em>not</em> set.
	 *
	 * The {@link PageSaveRequest} has the <code>deriveFileName</code>
	 * flag <em>not</em> set, and contains a file with a new page name,
	 * but without a filename. After saving this page, the filename
	 * should be the same as before the request (original behavior
	 * before introducing the <code>deriveFileName</code> flag).
	 *
	 * @throws Exception On test failure.
	 */
	@Test
	public void testNoDeriveFilenameWithoutFilename() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Page originalPage = Creator.createPage("Original Name",
			node.getFolder(),
			template);
		Integer id = originalPage.getId();
		String originalFilename = originalPage.getFilename();

		t.commit(false);

		Page newPage = new Page();

		newPage.setId(id);
		newPage.setName("New Pagename");
		newPage.setFileName("");

		PageSaveRequest saveRequest = new PageSaveRequest(newPage);
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		GenericResponse saveResponse = pageResource.save(id.toString(),saveRequest);

		assertEquals("Saving should be successful",
			saveResponse.getResponseInfo().getResponseCode(),
			ResponseCode.OK);

		PageLoadResponse loadResponse = pageResource.load(id.toString(),
			false, false, false, false, false, false, false, false, false,
			false, node.getId(), null);

		assertEquals("Filename should not have changed",
				loadResponse.getPage().getFileName(),
				originalFilename);
	}

	/**
	 * Tests whether given filename is used when
	 * {@link PageSaveRequest#deriveFileName} is <em>not</em> set.
	 *
	 * The {@link PageSaveRequest} has the <code>deriveFileName</code>
	 * flag <em>not</em> set, and contains a file with a new page- and
	 * filename. After saving this page, the filename should be the name
	 * provided in the save request (original behavior before introducing
	 * the <code>deriveFileName</code> flag).
	 *
	 * @throws Exception On test failure.
	 */
	@Test
	public void testNoDeriveFilenameWithFilename() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Page originalPage = Creator.createPage("Original Name",
			node.getFolder(),
			template);
		Integer id = originalPage.getId();

		t.commit(false);

		String expectedFilename = "expected-filename.en.html";
		Page newPage = new Page();

		newPage.setId(id);
		newPage.setName("New Pagename");
		newPage.setFileName(expectedFilename);

		PageSaveRequest saveRequest = new PageSaveRequest(newPage);
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		GenericResponse saveResponse = pageResource.save(id.toString(),saveRequest);

		assertEquals("Saving should be successful",
			saveResponse.getResponseInfo().getResponseCode(),
			ResponseCode.OK);

		PageLoadResponse loadResponse = pageResource.load(id.toString(),
			false, false, false, false, false, false, false, false, false,
			false, node.getId(), null);

		assertEquals("Filename should have been updated normally",
				loadResponse.getPage().getFileName(),
				expectedFilename);
	}
}
