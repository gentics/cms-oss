package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.api.rest.ModelBuilderApiHelper;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests the utility methods of the model builder api helper
 * 
 * @author johannes2
 * 
 */
public class ModelBuilderApiHelperSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

	/**
	 * Valid page ID
	 */
	private static final String PAGE_ID = "45";

	/**
	 * PageResourceImpl that is used to call the server
	 */
	private PageResourceImpl pageResource = new PageResourceImpl();

	@Test
	public void testPageRendering() throws Exception {
		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			Page restPage = pageResource.load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage();

			assertNotNull("The restpage with id {" + PAGE_ID + "} should be loaded", restPage);
			String content = ModelBuilderApiHelper.renderPage(restPage);
			assertTrue("The content of the rendered page should not be empty.", !StringUtils.isEmpty(content));
		}
	}

	@Test
	public void testPageRenderingWithTemplate() throws Exception {
		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			Page restPage = pageResource.load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage();

			assertNotNull("The restpage with id {" + PAGE_ID + "} should be loaded", restPage);
			String content = ModelBuilderApiHelper.renderPage(restPage, "<node content>");
			assertTrue("The content of the rendered page should not be empty.", !StringUtils.isEmpty(content));
		}
	}
}
