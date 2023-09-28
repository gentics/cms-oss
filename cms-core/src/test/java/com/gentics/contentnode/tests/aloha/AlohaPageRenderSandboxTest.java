package com.gentics.contentnode.tests.aloha;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.parser.ContentRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.testutils.DBTestContext;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests rendering of pages in Aloha mode and checks if the result is the expected result.
 * 
 * @author floriangutmann
 */
@RunWith(Parameterized.class)
public class AlohaPageRenderSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * The render type used for rendering pages
	 */
	private RenderType renderType;
	
	/***
	 * The render result used for the render process
	 */
	private RenderResult renderResult;
	
	/**
	 * Transaction used for rendering the page
	 */
	private Transaction t;
	
	/**
	 * Filename of the currently running test
	 */
	private String filename;
	
	/**
	 * Whether the transaction should be created for a user that has write
	 * permissions to the page under test.
	 */
	private boolean writePerms;
	
	/**
	 * Provides the parameters for this parameterized test.
	 * Returns a collection of string arrays that contain the parameters for the constructor:
	 * 1: Name of the result file
	 * 2: true if the transaction should be created with a user that has write permissions for
	 *    the pages under test, false otherwise
	 */
	@Parameters
	public static Collection<String[]> getFilenames() throws Exception {
		RendererFactory.registerRenderer(ContentRenderer.RENDERER_ALOHA, new AlohaRenderer());
		return Arrays.asList(new String[][] {
			{ "42.html", "true"}, // aloha1
			{ "43.html", "true"}, // aloha2
			// {"45.html", "false"}, //aloha3,
			{ "46.html", "true"}   // alohalinks
		});
	}
	
	/**
	 * Creates the AlohaPageRenderTest for the given filename
	 * @param filename Filename of the result
	 */
	public AlohaPageRenderSandboxTest(String filename, String writePerms) {
		this.filename = filename;
		this.writePerms = Boolean.valueOf(writePerms);
	}
	
	/**
	 * Sets up the test context and renderer
	 */
	@Before
	public void setUp() throws Exception {

		if (writePerms) {
			t = testContext.startTransactionWithPermissions(true);
		} else {
			t = testContext.startTransactionWithoutPermissions(true);
		}
		ContentNodeHelper.setLanguageId(1);

		NodePreferences preferences = t.getNodeConfig().getDefaultPreferences();

		renderType = RenderType.getDefaultRenderType(preferences, RenderType.EM_ALOHA, t.getSessionId(), 0);
		renderType.setRenderUrlFactory(new DynamicUrlFactory(t.getSessionId()));
		renderType.setParameter(AlohaRenderer.ADD_SCRIPT_INCLUDES, Boolean.TRUE);

		t.setRenderType(renderType);
		renderResult = new RenderResult();
	}

	/**
	 * Tests rendering the page and comparing the result.
	 * Timestamps in JSON Objets are stripped out with {@link #removeTimestamps(String)}. 
	 */
	@Test
	@Ignore ("Ignored, because test relies on order of elements in JSON object, which is not equal on all platforms.")
	public void testRenderPage() throws Exception {
		Integer pageId = Integer.parseInt(filename.substring(0, filename.lastIndexOf('.')));
		Page page = (Page) t.getObject(Page.class, pageId);
		String result = page.render(renderResult);
		
		String expectedResult = getExpectedResult();

		if (expectedResult == null) {
			System.out.println("Render result:\n\n" + result);
			fail("No expected result found for page " + filename);
		}
		expectedResult = removeTimestamps(getExpectedResult());
		
		// TODO: comparing the rendered page with an expected result
		// is not a good way to test. This will fail too often when
		// something is changed.
		
		// ignore the session ID
		result = result.replaceAll(t.getSessionId(), "");
		// ignore any script includes
		result = result.replaceAll("<script.*?src=.*?</script>\n", "");
		// ignore timestamps
		result = removeTimestamps(result);

		assertEquals("Check if the results match", expectedResult, result);
	}
	
	/**
	 * Removes timestamps and fix newlines from the given string.
	 * Timestamps that are supported:
	 * <ul>
	 * <li>Timestamps in JSON objects that look like <code>"timestamp": 122311221.</code></li>
	 * <li>Timestamps in URLs that look like <code>time=12151658465</code></li>
	 * </ul>
	 * 
	 */
	private String removeTimestamps(String string) {
		return string.replaceAll("\"timestamp\"\\s:\\s[0-9]*", "\"timestamp\" : 0").replaceAll("time=[0-9]*", "time=0").replaceAll("\r\n", "\n").replaceAll(
				"\"buildRootTimestamp\" : \"[0-9\\-]*\"", "\"buildRootTimestamp\" : \"0\"");
	}
	
	/**
	 * Gets the expected result for the current test.
	 * The expected result is read form the package "com.gentics.aloha.renderresults".
	 * 
	 * @return The expected result as String 
	 */
	protected String getExpectedResult() {
		try {
			InputStream in = this.getClass().getResourceAsStream("renderresults/" + filename);

			return IOUtils.toString(in, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
