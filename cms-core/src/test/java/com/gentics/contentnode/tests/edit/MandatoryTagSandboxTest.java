package com.gentics.contentnode.tests.edit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests for changing the mandatory flag of TemplateTags.
 * 
 * @author Petro Salema
 * @date 28 January 2013
 */
public class MandatoryTagSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * The id of the test template. This template contains 9 tags, that have a text part that require digits as valid values.
	 * Name: Mandatory Tags
	 */
	public final static int TEMPLATE_ID = 94;

	/**
	 * The template which the test template is linked to, and in which we will create test pages.
	 */
	private final static int FOLDER_ID = 182;

	/**
	 * Retrieves the test template in read/write mode.
	 * 
	 * @return Template object in read/write mode.
	 */
	public static Template getTestTemplate() throws NodeException {
		return ((Template) TransactionManager.getCurrentTransaction().getObject(Template.class, TEMPLATE_ID, true));
	}

	/**
	 * Returns any tag in the given template.
	 * 
	 * @param template
	 *            The template in which to search for a tag.
	 * @return A template tag.
	 */
	public static String getRandomTemplateTagname(Template template) throws NodeException {
		Map<String, TemplateTag> tags = template.getTemplateTags();
		String[] tagnames = (String[]) ((Set<String>) tags.keySet()).toArray(new String[0]);
		int index = (int) Math.round((tagnames.length - 1) * Math.random());

		return tagnames[index];
	}

	/**
	 * Gets the mandatory flag on the given tag.
	 * 
	 * @param template
	 *            The template whose tag is to inspect.
	 * @param tagname
	 *            The name of the tag to modify.
	 * @return The value of the mandatory flag of the given template tag.
	 * @throws NodeException
	 */
	public static boolean getFlag(Template template, String tagname) throws NodeException {
		TemplateTag tag = (TemplateTag) template.getTag(tagname);

		return tag.getMandatory();
	}

	/**
	 * Sets the mandatory flag of the given template tag name.
	 * 
	 * @param template
	 *            A template with tags.
	 * @param tagname
	 *            The name of the tag to modify.
	 * @param flag
	 *            The value which the flag should be set to.
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public static void setFlag(Template template, String tagname, boolean flag) throws ReadOnlyException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		TemplateTag tag = (TemplateTag) template.getTemplateTag(tagname);

		tag.setMandatory(flag);
		template.save();
		t.commit(false);
	}

	/**
	 * Tests the modifying of the mandatory flag on a TemplateTag.
	 * 
	 * Will set the value of the mandatory flag of the tag in the test template. Will then check whether the value was saved correctly.
	 * 
	 * @param tagname
	 *            The name of the tag to modify.
	 * @param value
	 *            The value to which the mandatory flag should be set.
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	private static void runFlagSetTest(String tagname, boolean value) throws ReadOnlyException, NodeException {
		Template template = getTestTemplate();

		setFlag(template, tagname, value);
		assertEquals("Tag " + tagname + " should have its mandatory flag set to " + (value ? "true" : "false"), value, getFlag(template, tagname));
	}

	/**
	 * Creates a page from the given template, inside FOLDER_ID.
	 * 
	 * @param template
	 *            The template from which to create the page.
	 */
	private static Page createTestPage(Template template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = (Page) t.createObject(Page.class);
		String rand = Double.toString(Math.random());

		page.setFilename("page-with-mandatory-tags-" + rand + ".html");
		page.setName("Page With Mandatory Tags " + rand);
		page.setTemplateId(template.getId());
		page.setFolderId(FOLDER_ID);
		page.save();
		t.commit(false);
		return page;
	}

	/**
	 * Publishes the given page, and compares the response code with the expected one.
	 * 
	 * @param page
	 *            The page to publish
	 * @param code
	 *            The REST-API response code
	 * @throws TransactionException
	 */
	protected void publishPage(Page page, ResponseCode code) throws TransactionException {
		Transaction t = TransactionManager.getCurrentTransaction();

		try {
			PageResourceImpl pageResource = new PageResourceImpl();

			pageResource.setTransaction(t);

			PagePublishRequest request = new PagePublishRequest();

			request.setAlllang(true);

			assertResponse(pageResource.publish(Integer.toString(page.getId()), null, request), code);
		} finally {
			t.commit(false);
		}
	}

	/**
	 * Assert that the response code is {@link ResponseCode.OK}
	 * 
	 * @param response
	 *            RestAPI response
	 * @param code
	 *            Expected response code
	 */
	private void assertResponse(GenericResponse response, ResponseCode responseCode) {
		assertTrue("The response code did not match. The response message was {" + response.getResponseInfo().getResponseMessage()
				+ "} Expected code {" + responseCode + "} but got {" + response.getResponseInfo().getResponseCode() + "}", response.getResponseInfo()
				.getResponseCode() == responseCode);
	}

	/**
	 * Runs 2 simple tests to ensure that the mandatory flag can be set to TRUE and FALSE on a TemplateTag.
	 * 
	 * @throws SandboxException
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@Test
	public void testMandatoryFlag() throws ReadOnlyException, NodeException {
		Template template = getTestTemplate();

		if (null == template) {
			throw new NodeException("Template " + TEMPLATE_ID + " does not exist.");
		}

		String tagname = getRandomTemplateTagname(template);

		if (null == tagname) {
			throw new NodeException("Template " + TEMPLATE_ID + " has no template tags.");
		}

		runFlagSetTest(tagname, false);
		runFlagSetTest(tagname, true);
	}

	/**
	 * Sets 1 of the the 9 template tags of the test template to be mandatory.
	 * 
	 * Creates a page which would have 1 mandatory tag.
	 * 
	 * Runs several tests to check behavior of try to publish a page which contains this mandatory tag.
	 * 
	 * @throws SandboxException
	 * @throws NodeException
	 * @throws Exception
	 */
	@Test
	public void testPublishingMandatoryTags() throws NodeException {
		Template template = getTestTemplate();

		if (null == template) {
			throw new NodeException("Template " + TEMPLATE_ID + " does not exist.");
		}

		String tagname = getRandomTemplateTagname(template);

		if (null == tagname) {
			throw new NodeException("Template " + TEMPLATE_ID + " has no template tags.");
		}

		setFlag(template, tagname, true);
		Page testpage = createTestPage(template);

		// Because mandatory tag's required part has unfilled value
		publishPage(testpage, ResponseCode.INVALIDDATA);

		Tag tag = testpage.getTag(tagname);
		tag.getTagValues().getByKeyname("txt").setValueText("wrong");
		testpage.save();
		TransactionManager.getCurrentTransaction().commit(false);

		// Because mandatory tag's required part has wrong value
		publishPage(testpage, ResponseCode.INVALIDDATA);

		tag.getTagValues().getByKeyname("txt").setValueText("12");
		testpage.save();
		TransactionManager.getCurrentTransaction().commit(false);

		// Because mandatory tag's required part is correctly filled
		publishPage(testpage, ResponseCode.OK);

		testpage.deleteAllLanguages();
	}
}
