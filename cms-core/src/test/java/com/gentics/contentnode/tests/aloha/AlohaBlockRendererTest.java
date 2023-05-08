package com.gentics.contentnode.tests.aloha;

import static com.gentics.contentnode.tests.utils.ContentNodeMockUtils.construct;
import static com.gentics.contentnode.tests.utils.ContentNodeMockUtils.content;
import static com.gentics.contentnode.tests.utils.ContentNodeMockUtils.contentTag;
import static com.gentics.contentnode.tests.utils.ContentNodeMockUtils.page;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.junit.LabelledParameterized;

/**
 * Tests rendering of aloha blocks
 * 
 * @author floriangutmann
 */
@RunWith(LabelledParameterized.class)
public class AlohaBlockRendererTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Filename for this test
	 */
	private String filename;

	private static Page page;

	private static ContentTag outerTag;

	private static ContentTag innerTag;

	private static ContentTag linkTag;

	/**
	 * Set up the test environment. Create a new test context
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Construct linkConstruct = construct(777, "gtxalohapagelink", "Dummy link");

		// create tha dummy page
		Folder folder = mock(Folder.class);
		page = page(555, content(666));
		when(page.getFolder()).thenReturn(folder);

		// two new dummy tags
		outerTag = contentTag(123, null, "dummy123", page.getContent());
		when(outerTag.isEditable()).thenReturn(true);
		when(outerTag.isAlohaBlock()).thenReturn(true);

		innerTag = contentTag(4711, null, "dummy4711", page.getContent());
		when(innerTag.isEditable()).thenReturn(true);
		when(innerTag.isAlohaBlock()).thenReturn(true);

		// and a dummy link tag.
		linkTag = contentTag(7777, linkConstruct, "gtxalohapagelink7777", page.getContent());
		when(linkTag.isEditable()).thenReturn(true);
		when(linkTag.isAlohaBlock()).thenReturn(true);

		RenderType renderType = t.getRenderType();

		renderType.push(page);
		renderType.setEditMode(RenderType.EM_ALOHA);
	}

	/**
	 * Get the filenames for which the tests should run
	 * @return Collection of filenames
	 */
	@Parameters
	public static Collection<String[]> getFilenames() throws Exception {
		return GenericTestUtils.getFilenamesForParameterizedTest(AlohaBlockRendererTest.class, "blockinputs", new String[] { "html"});
	}
	
	/**
	 * Instantiates the Aloha block test with a given filename.
	 * @param filename The filename that should be used to read the input and result file.
	 */
	public AlohaBlockRendererTest(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Reads an input file from the package "com.gentics.tests.contentnode.aloha.blockinputs",
	 * makes a block out of it and compares the result with the expected results from
	 * package "com.gentics.tests.contentnode.aloha.blockresults".
	 */
	@Test
	public void block() throws Exception {
		InputStream inputStream = this.getClass().getResourceAsStream("blockinputs/" + filename);
		InputStream resultStream = this.getClass().getResourceAsStream("blockresults/" + filename);
		
		String template = IOUtils.toString(inputStream);
		String result = IOUtils.toString(resultStream);
		
		AlohaRenderer renderer = new AlohaRenderer();
		
		assertEquals("Check if processing { " + filename + " } returns expected output", result, renderer.block(template, outerTag, new RenderResult()));
	}

	/**
	 * Reads an input file from the package "com.gentics.tests.contentnode.aloha.blockinputs",
	 * makes a block out of it and compares the result with the expected results from
	 * package "com.gentics.tests.contentnode.aloha.linkresults".
	 */
	@Test
	public void link() throws Exception {
		InputStream inputStream = this.getClass().getResourceAsStream("blockinputs/" + filename);
		InputStream resultStream = this.getClass().getResourceAsStream("linkresults/" + filename);
		String template = IOUtils.toString(inputStream);
		String result = IOUtils.toString(resultStream);

		AlohaRenderer renderer = new AlohaRenderer();

		assertEquals("Check if processing { " + filename + " } returns expected output", result, renderer.block(template, linkTag, new RenderResult()));
	}

	/**
	 * Reads an input file from the package "com.gentics.tests.contentnode.aloha.blockinputs",
	 * makes a block out of it and compares the result with the expected results from
	 * package "com.gentics.tests.contentnode.aloha.plinkresults".
	 *
	 * The Aloha renderer is wrapped in a class that performs the plink replacement again,
	 * after rendering the block.
	 */
	@Test
	public void plinks() throws Exception {
		InputStream inputStream = this.getClass().getResourceAsStream("blockinputs/" + filename);
		InputStream resultStream = this.getClass().getResourceAsStream("plinkresults/" + filename);
		String template = IOUtils.toString(inputStream);
		String result = IOUtils.toString(resultStream);

		AlohaRenderer renderer = new AlohaRenderer() {
			@Override
			public String block(String code, ParserTag parserTag, RenderResult renderResult) throws NodeException {
				String rendered = super.block(code, parserTag, renderResult);

				return savePLinks(rendered).get("code");
			}
		};

		assertEquals("Check if processing { " + filename + " } returns expected output", result, renderer.block(template, outerTag, new RenderResult()));
	}

	/**
	 * Will do the same as {@link #block()}, but will re-render the output
	 * This simulates the situation when a contenttag is nested within a htmllang
	 * tag which has no additional content
	 */
	@Test
	public void nestedBlock() throws Exception {
		InputStream inputStream = this.getClass().getResourceAsStream("blockinputs/" + filename);
		InputStream resultStream = this.getClass().getResourceAsStream("nestedblockresults/" + filename);
		
		String template = IOUtils.toString(inputStream);
		String result = IOUtils.toString(resultStream);
		
		AlohaRenderer renderer = new AlohaRenderer();
		
		RenderResult r = new RenderResult();
		String out = renderer.block(template, outerTag, r);
		
		assertEquals("Check if nested processing of { " + filename + " } returns expected output", result, renderer.block(out, innerTag, r));
	}

	/**
	 * Will do the same as {@link #block()}, but will render in aloha-readonly mode (feature copy_tags off)
	 * This should not change the blocks at all.
	 */
	@Test
	public void readOnlyBlock() throws Exception {
		InputStream inputStream = this.getClass().getResourceAsStream("blockinputs/" + filename);
		InputStream resultStream = this.getClass().getResourceAsStream("blockinputs/" + filename);
		
		String template = IOUtils.toString(inputStream);
		String result = IOUtils.toString(resultStream);
		
		AlohaRenderer renderer = new AlohaRenderer();

		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		int editMode = renderType.getEditMode();

		try {
			renderType.setEditMode(RenderType.EM_ALOHA_READONLY);
			assertEquals("Check if processing { " + filename + " } returns expected output", result, renderer.block(template, outerTag, new RenderResult()));
		} finally {
			renderType.setEditMode(editMode);
		}
	}

	/**
	 * Will do the same as {@link #block()}, but will render in aloha-readonly mode and the feature copy_tags on
	 */
	@Test
	public void readOnlyBlockCopyTags() throws Exception {
		InputStream inputStream = this.getClass().getResourceAsStream("blockinputs/" + filename);
		InputStream resultStream = this.getClass().getResourceAsStream("roblockresults/" + filename);
		
		String template = IOUtils.toString(inputStream);
		String result = IOUtils.toString(resultStream);
		
		AlohaRenderer renderer = new AlohaRenderer();

		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		int editMode = renderType.getEditMode();
		boolean featureValue = prefs.isFeature(Feature.COPY_TAGS);

		try {
			renderType.setEditMode(RenderType.EM_ALOHA_READONLY);
			prefs.setFeature(Feature.COPY_TAGS.toString().toLowerCase(), true);
			assertEquals("Check if processing { " + filename + " } returns expected output", result, renderer.block(template, outerTag, new RenderResult()));
		} finally {
			renderType.setEditMode(editMode);
			prefs.setFeature(Feature.COPY_TAGS.toString().toLowerCase(), featureValue);
		}
	}
}
