/*
 * @author norbert
 * @date 06.10.2006
 * @version $Id: PlausibilityTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.nodecopy;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.contentnode.dbcopy.StructureCopy;

/**
 * Test case for checking the nodecopy. Test all combinations of allowed
 * parameters
 */
@Ignore
public class PlausibilityTest {

	/**
	 * configuration properties
	 */
	protected Properties properties;

	@Before
	public void setUp() throws Exception {

		properties = new Properties();
		properties.load(getClass().getResourceAsStream("plausibilityTest.properties"));
	}

	/**
	 * Test with everything off
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWithnoOptions() throws Exception {
		// only copy folders
		runCopyTest(false, false, false, false, false);
	}

	/**
	 * Test with page copy
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPage() throws Exception {
		runCopyTest(true, false, false, false, false);
	}

	/**
	 * Test with permission copy
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerm() throws Exception {
		runCopyTest(false, true, false, false, false);
	}

	/**
	 * Test with file copy
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFile() throws Exception {
		runCopyTest(false, false, true, false, false);
	}

	/**
	 * Test with template copy
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTemplate() throws Exception {
		runCopyTest(false, false, false, true, false);
	}

	/**
	 * Test with page and permissions
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPagePerm() throws Exception {
		runCopyTest(true, true, false, false, false);
	}

	/**
	 * Test with page and file
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageFile() throws Exception {
		runCopyTest(true, false, true, false, false);
	}

	/**
	 * Test with page and template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageTemplate() throws Exception {
		runCopyTest(true, false, false, true, false);
	}

	/**
	 * Test with permission and fil
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPermFile() throws Exception {
		runCopyTest(false, true, true, false, false);
	}

	/**
	 * Test with permission and template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPermTemplate() throws Exception {
		runCopyTest(false, true, false, true, false);
	}

	/**
	 * Test with file and template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFileTemplate() throws Exception {
		runCopyTest(false, false, true, true, false);
	}

	/**
	 * Test with page, permission and file
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPagePermFile() throws Exception {
		runCopyTest(true, true, true, false, false);
	}

	/**
	 * Test with page, permission and template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPagePermTemplate() throws Exception {
		runCopyTest(true, true, false, true, false);
	}

	/**
	 * Test with page, file and template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageFileTemplate() throws Exception {
		runCopyTest(true, false, true, true, false);
	}

	/**
	 * Test with permission, file and template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPermFileTemplate() throws Exception {
		runCopyTest(false, true, true, true, false);
	}

	/**
	 * Test with page, permission, file and template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPagePermFileTemplate() throws Exception {
		runCopyTest(true, true, true, true, false);
	}

	/**
	 * Test workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWorkflow() throws Exception {
		// only copy folders
		runCopyTest(false, false, false, false, true);
	}

	/**
	 * Test with page and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageWorkflow() throws Exception {
		runCopyTest(true, false, false, false, true);
	}

	/**
	 * Test with permission and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPermWorkflow() throws Exception {
		runCopyTest(false, true, false, false, true);
	}

	/**
	 * Test with file and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFileWorkflow() throws Exception {
		runCopyTest(false, false, true, false, true);
	}

	/**
	 * Test with template and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTemplateWorkflow() throws Exception {
		runCopyTest(false, false, false, true, true);
	}

	/**
	 * Test with page, permissions and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPagePermWorkflow() throws Exception {
		runCopyTest(true, true, false, false, true);
	}

	/**
	 * Test with page, file and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageFileWorkflow() throws Exception {
		runCopyTest(true, false, true, false, true);
	}

	/**
	 * Test with page, template and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageTemplateWorkflow() throws Exception {
		runCopyTest(true, false, false, true, true);
	}

	/**
	 * Test with permission, file and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPermFileWorkflow() throws Exception {
		runCopyTest(false, true, true, false, true);
	}

	/**
	 * Test with permission, template and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPermTemplateWorkflow() throws Exception {
		runCopyTest(false, true, false, true, true);
	}

	/**
	 * Test with file, template and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFileTemplateWorkflow() throws Exception {
		runCopyTest(false, false, true, true, true);
	}

	/**
	 * Test with page, permission, file and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPagePermFileWorkflow() throws Exception {
		runCopyTest(true, true, true, false, true);
	}

	/**
	 * Test with page, permission, template and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPagePermTemplateWorkflow() throws Exception {
		runCopyTest(true, true, false, true, true);
	}

	/**
	 * Test with page, file, template and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageFileTemplateWorkflow() throws Exception {
		runCopyTest(true, false, true, true, true);
	}

	/**
	 * Test with permission, file, template and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPermFileTemplateWorkflow() throws Exception {
		runCopyTest(false, true, true, true, true);
	}

	/**
	 * Test with page, permission, file, template and workflow
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPagePermFileTemplateWorkflow() throws Exception {
		runCopyTest(true, true, true, true, true);
	}

	/**
	 * Run the copy test with the given options
	 * 
	 * @param copyPage
	 *            true for page copy
	 * @param copyPerm
	 *            true for permission copy
	 * @param copyFile
	 *            true for file copy
	 * @param copyTemplate
	 *            true for template copy
	 * @param copyWorkflow
	 *            true for workflow copy
	 * @throws Exception
	 */
	protected void runCopyTest(boolean copyPage, boolean copyPerm,
			boolean copyFile, boolean copyTemplate, boolean copyWorkflow) throws Exception {
		Properties testProperties = new Properties();

		testProperties.setProperty("node", properties.getProperty("node"));
		testProperties.setProperty("copypage", copyPage ? "yes" : "no");
		testProperties.setProperty("copyperm", copyPerm ? "yes" : "no");
		testProperties.setProperty("copyfile", copyFile ? "yes" : "no");
		testProperties.setProperty("copytemplate", copyTemplate ? "yes" : "no");
		testProperties.setProperty("copyworkflow", copyWorkflow ? "yes" : "no");

		// get the copy estimation
		CopyCheck check = new CopyCheck(properties.getProperty("config"), properties.getProperty("url"), properties.getProperty("driverClass"),
				properties.getProperty("username", null), properties.getProperty("password", null), testProperties);
		String estimation = check.getCopyEstimation();

		// get the structure copy preview here
		StructureCopy copy = new StructureCopy(properties.getProperty("config"), null, properties.getProperty("url"), properties.getProperty("driverClass"),
				properties.getProperty("username", null), properties.getProperty("password", null), testProperties);
		Map objectStructure = copy.getObjectStructure(false);
		String copyPreview = copy.getObjectStats(objectStructure);

		copy.finishCopy();

		assertEquals("Check estimation with preview", estimation, copyPreview);
	}
}
