package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * Test cases for rendering velocity parts
 */
public class SimpleVelocityRenderingSandboxTest extends AbstractVelocityRenderingTest {
	@Test
	public void testRenderHelloWorld() throws NodeException {
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
	private String renderTemplate(String template) throws NodeException {
		operate(() -> updateConstruct(template));

		return supply(user, t -> {
			RenderType renderType = RenderType.getDefaultRenderType(testContext.getContext().getNodeConfig().getDefaultPreferences(), RenderType.EM_PREVIEW, -1);
			t.setRenderType(renderType);

			RenderResult renderResult = new RenderResult();
			String output = page.render(renderResult);
			assertEquals("The return code was not correct. Output was {" + output + "}. Assert ReturnCode: ", "OK", renderResult.getReturnCode());
			return output;
		});
	}
}
