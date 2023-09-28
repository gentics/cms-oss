package com.gentics.contentnode.tests.aloha;

import static org.junit.Assert.assertEquals;

import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;
import java.io.InputStream;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the Aloha editable rendering
 * 
 * @author floriangutmann
 */
@RunWith(Parameterized.class)
public class AlohaEditableRendererTest {
	

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	
	/**
	 * Filename for this test
	 */
	private String filename;

	/**
	 * Get the filenames for which the tests should run
	 * @return Collection of filenames
	 */
	@Parameters(name = "{index}: filename: {0}")
	public static Collection<String[]> getFilenames() throws Exception {
		return GenericTestUtils.getFilenamesForParameterizedTest(AlohaEditableRendererTest.class, "editableinputs", new String[] { "html"});
	}
	
	/**
	 * Instantiates the AlohaEditable test with a given filename.
	 * @param filename The filename that should be used to read the input and result file.
	 */
	public AlohaEditableRendererTest(String filename) {
		this.filename = filename;
	}

	/**
	 * Reads an input file from the package "com.gentics.tests.contentnode.aloha.editableinputs",
	 * replaces the editables and compares the result with the expected results from
	 * package "com.gentics.tests.contentnode.aloha.editableresults".
	 * 
	 * @param filename The name of the file to look for, run and compare
	 */
	@Test
	public void replaceEditables() throws Exception {
		InputStream inputStream = this.getClass().getResourceAsStream("editableinputs/" + filename);
		InputStream resultStream = this.getClass().getResourceAsStream("editableresults/" + filename);
		
		String template = IOUtils.toString(inputStream);
		String result = IOUtils.toString(resultStream);
		
		testContext.getContext().getNodeConfig().getDefaultPreferences().unsetFeature("aloha_annotate_editables");
		AlohaRenderer renderer = new AlohaRenderer();
		
		assertEquals("Check if processing { " + filename + " } returns expected output", result, renderer.replaceEditables(template, new RenderResult()));		
	}

	/**
	 * Test for rendering annotated editables
	 * @throws Exception
	 */
	@Test
	public void replaceEditablesAnnotated() throws Exception {
		InputStream inputStream = this.getClass().getResourceAsStream("editableinputs/" + filename);
		InputStream resultStream = this.getClass().getResourceAsStream("editableresultsannotated/" + filename);
		
		String template = IOUtils.toString(inputStream);
		String result = IOUtils.toString(resultStream);
		
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("aloha_annotate_editables", true);
		AlohaRenderer renderer = new AlohaRenderer();
		
		assertEquals("Check if processing { " + filename + " } returns expected output", result, renderer.replaceEditables(template, new RenderResult()));		
	}
}
