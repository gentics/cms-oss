package com.gentics.contentnode.tests.rendering;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * Test cases for rendering velocity parts
 */
public class SimpleVelocityRenderingSandboxTest extends AbstractVelocityRenderingTest {

	@Before
	public void setUp() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = RenderType.getDefaultRenderType(testContext.getContext().getNodeConfig().getDefaultPreferences(), RenderType.EM_PREVIEW, "sid", -1);
		t.setRenderType(renderType);
	}

	@Test
	public void testRenderHelloWorld() throws Exception {
		String template = "$cms.imps.velocitytools.esc.url(\"hello<br>&world\")";
		assertEquals("The rendered page should print hello world", "hello%3Cbr%3E%26world", renderTemplate(template));
	}

	@Test
	public void testRenderVeloImp() throws Exception {
		String template = "#set ($foo = \"hello world\")$foo";
		assertEquals("The rendered page should print hello world", "hello world", renderTemplate(template));
	}

	/**
	 * Render the created page with the given content
	 * 
	 * @param template
	 * @return
	 * @throws Exception
	 */
	private String renderTemplate(String template) throws Exception {
		updateConstruct(template);
		TransactionManager.getCurrentTransaction().commit(false);
		RenderResult renderResult = new RenderResult();
		String output = page.render(renderResult);
		assertEquals("The return code was not correct. Output was {" + output + "}. Assert ReturnCode: ", "OK", renderResult.getReturnCode());
		return output;
	}
}
