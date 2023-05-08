package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.validation.util.ValidationUtils;
import com.gentics.testutils.GenericTestUtils;

/**
 * Tests saving of a page into GCN by using the REST API.
 *
 * @author bernhardkaszt
 */
public class PageValidationSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * The Node used for testing
	 */
	private Node node;

	/**
	 * Base URI of the page resource
	 */
	public static String BASE_URI = null;

	/**
	 * The construct id of the created test tag
	 */
	private static final int CONSTRUCT_ID = 1;

	/**
	 * Page resource used in various tests
	 */
	private PageResource pageResource;

	/**
	 * Initializes the client and the WebResource
	 */
	@Before
	public void setUp() throws Exception {

		node = ContentNodeTestDataUtils.createNode("validationsandboxtestnode",
				"www.validationsandboxtestnode.at", "/test", "/testbin", false,
				false);
		create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName("validationsandboxtesttemplate");
			tmpl.setSource("blabla");
		});

		// Enable the validation feature
		ValidationUtils.setValidationEnabled(true);

		// Set our own policy map file
		NodeConfigRuntimeConfiguration runtimeConfiguration = NodeConfigRuntimeConfiguration.getDefault();
		NodePreferences nodePreferences = runtimeConfiguration.getNodeConfig().getDefaultPreferences();
		String policyMapFilepath = "/com/gentics/testutils/resources/validation/policy-map.custom.xml";
		java.net.URL resourcePath = GenericTestUtils.class.getResource(policyMapFilepath);
		nodePreferences.setProperty("validation.policyMap", "file://" + resourcePath.getPath());

		testContext.getContext().startTransaction();
		pageResource = getPageResource();
	}

	/**
	 * Creates a page with a "bad" page properties and tries to save the page
	 *
	 * @throws Exception
	 */
	@Test
	public void testSavePageWithValidProperties() throws Exception {
		Page testPage = createTestPage();
		testPage.setName("<span>name</span>");
		testPage.setDescription("<span>description</span>");
		testPage.setFileName("filename.ext");
		testPage.setLanguage("somelang");

		savePageAndcheckResponse(testPage.getId(), testPage, ResponseCode.OK);
	}

	/**
	 * Creates a page with a "bad" page properties and tries to save the page
	 *
	 * @throws Exception
	 */
	@Test
	public void testSavePageWithBadProperties() throws Exception {
		Page testPage = createTestPage();

		List<String> testProperties = Arrays.asList(new String[]
				{ "name", "description", "filename", "language" });

		for (String testProperty : testProperties) {
			Page newPageData = new Page();

			if (testProperty.equals("name")) {
				newPageData.setName("<script>");
			} else if (testProperty.equals("description")) {
				newPageData.setDescription("<script>");
			} else if (testProperty.equals("filename")) {
				newPageData.setFileName("<script>");
			} else if (testProperty.equals("language")) {
				newPageData.setLanguage("<script>");
			}

			GenericResponse genericResponse = savePageAndcheckResponse(testPage.getId(), newPageData, ResponseCode.FAILURE);
			assertEquals("The script tag is not allowed", true,
					genericResponse.getResponseInfo().getResponseMessage().contains("The script tag is not allowed"));
		}
	}

	/**
	 * Creates a page with a "bad" tag and tries to save it.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSavePageWithInvalidTags() throws Exception {
		Page testPage = createTestPage();
		PageSaveRequest pageSaveRequest = new PageSaveRequest(testPage);

		Map<String, Tag> tags = new HashMap<String, Tag>();
		Tag tag = new Tag();
		tag.setType(Tag.Type.CONTENTTAG);
		tag.setName("text");
		tag.setConstructId(CONSTRUCT_ID);
		tag.setActive(true);

		Map<String, Property> tagProperties = new HashMap<String, Property>();
		Property tagProperty = new Property();
		tagProperty.setStringValue("<script>");
		tagProperty.setType(Property.Type.RICHTEXT);

		tagProperties.put("html", tagProperty);
		tag.setProperties(tagProperties);
		tags.put("text", tag);

		pageSaveRequest.getPage().setTags(tags);

		GenericResponse genericResponse = savePageAndcheckResponse(testPage.getId(), testPage, ResponseCode.FAILURE);
		assertEquals("The script tag is not allowed", true,
				genericResponse.getResponseInfo().getResponseMessage().contains("The script tag is not allowed"));
	}

	/**
	 * Save the page and check if the response matches
	 * @param responseCode
	 * @return
	 */
	protected GenericResponse savePageAndcheckResponse(
			Integer pageId, Page page, ResponseCode responseCode) {

		PageSaveRequest pageSaveRequest = new PageSaveRequest(page);
		GenericResponse saveResponse = pageResource.save(pageId.toString(), pageSaveRequest);

		assertEquals("Saving should throw an error", responseCode,
				saveResponse.getResponseInfo().getResponseCode());

		return saveResponse;
	}

	/**
	 * Get the page resource
	 *
	 * @return page resource
	 * @throws TransactionException
	 */
	protected PageResource getPageResource() throws TransactionException {
		PageResourceImpl pageResource = new PageResourceImpl();

		pageResource.setTransaction(TransactionManager.getCurrentTransaction());
		return pageResource;
	}

	/**
	 * Creates a simple naked page for testing
	 *
	 * @return Page
	 * @throws Exception
	 */
	protected Page createTestPage() throws Exception {
		PageCreateRequest pageCreateRequest = new PageCreateRequest();
		pageCreateRequest.setFolderId(node.getFolder().getId().toString());

		Page page = pageResource.create(pageCreateRequest).getPage();

		return page;
	}
}
