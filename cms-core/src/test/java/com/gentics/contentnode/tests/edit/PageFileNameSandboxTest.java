package com.gentics.contentnode.tests.edit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

@RunWith(Parameterized.class)
public class PageFileNameSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	private static class TestParameters {
		final public List<String> existingFilenames;
		final public String fileNameToGenerate;
		final public boolean exceptionExpected;
		final public String filenameExpected;
		public TestParameters(String[] existingFilenames, String filenameToGenerate, String filenameExpected) {
			this.existingFilenames=Arrays.asList(existingFilenames);
			this.fileNameToGenerate=filenameToGenerate;
			this.filenameExpected=filenameExpected;
			this.exceptionExpected=false;
		}
		public TestParameters(String[] existingFilenames, String filenameToGenerate) {
			this.existingFilenames=Arrays.asList(existingFilenames);
			this.fileNameToGenerate=filenameToGenerate;
			this.exceptionExpected = true;
			this.filenameExpected=null;
		}
	}

	@Parameterized.Parameters
	public static Collection<Object[]> parameters() {
		String[] manyfilenames = new String[202];
		for (int i = 0; i< manyfilenames.length; ++i) {
			manyfilenames[i]=String.format(Locale.ROOT, "filename%05d.test", 3000+i);
		}
		return Arrays.asList(new Object[][]{
						{ new TestParameters(new String[] {}, "thisisatest", "thisisatest") },
						{ new TestParameters(new String[] {"thisisatest"}, "thisisatest", "thisisatest1") },
						{ new TestParameters(new String[] {}, "thisisatest.txt", "thisisatest.txt") },
						{ new TestParameters(new String[] { "thisisatest.txt" }, "thisisatest.txt", "thisisatest1.txt") },
						{ new TestParameters(new String[] { "thisisatest0.txt" }, "thisisatest0.txt", "thisisatest1.txt") },
						{ new TestParameters(new String[] { "thisisatest09.txt" }, "thisisatest09.txt", "thisisatest10.txt") },
						{ new TestParameters(new String[] { "thisisatest9.txt" }, "thisisatest9.txt", "thisisatest10.txt") },
						{ new TestParameters(manyfilenames, "filename03000.test", "filename03202.test") },
						{ new TestParameters(new String[] { "thatsareallyreallyreallyreallyreallyreallyreallylongfilename.ext" },
								"thatsareallyreallyreallyreallyreallyreallyreallylongfilename.ext",
								"thatsareallyreallyreallyreallyreallyreallyreallylongfilenam1.ext") },
						{ new TestParameters(new String[] { "9.thatsareallyreallyreallyreallyreallyreallyreallylong_extension" },
								"9.thatsareallyreallyreallyreallyreallyreallyreallylong_extension",
								"10.thatsareallyreallyreallyreallyreallyreallyreallylong_extensio") },
						{ new TestParameters(new String[] { "111111111111111111111111111111111111111111111111111111111111.ext" },
								"111111111111111111111111111111111111111111111111111111111111.ext",
								"111111111111111111111111111111111111111111111111111111111112.ext") },
						{ new TestParameters(new String[] { "99999999999999999999999999999999999999999999999999999999999999.t" },
								"99999999999999999999999999999999999999999999999999999999999999.t") },
						{ new TestParameters(new String[] { "9999999999999999999999999999999999999999999999999999999999999998" },
								"9999999999999999999999999999999999999999999999999999999999999998",
								"9999999999999999999999999999999999999999999999999999999999999999") },
						{ new TestParameters(new String[] { "9999999999999999999999999999999999999999999999999999999999999999" },
										"9999999999999999999999999999999999999999999999999999999999999999") },
						{ new TestParameters(new String[] { "x9999999999999999999999999999999999999999999999999999999999999.t" },
								"x9999999999999999999999999999999999999999999999999999999999999.t",
								"10000000000000000000000000000000000000000000000000000000000000.t") },
						{ new TestParameters(new String[] { "MD-450-PAYSIC---Incoming-Customer---Standard-System-.en.html" },
								"MD-450-PAYSIC---Incoming-Customer---Standard-System-.en.html", "MD-450-PAYSIC---Incoming-Customer---Standard-System-1.en.html") },
						{ new TestParameters(new String[] { "MD-450.PAYSIC---Incoming-Customer---Standard-System-.en.html" },
								"MD-450.PAYSIC---Incoming-Customer---Standard-System-.en.html", "MD-451.PAYSIC---Incoming-Customer---Standard-System-.en.html") },
		});
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = Builder.create(Node.class, n -> {
			n.setHostname("test");
			n.setPublishDir("test");

			n.setFolder(Builder.create(Folder.class, root -> {
				root.setName("test");
				root.setPublishDir("test");
			}).doNotSave().build());
		}).build();

		template = Builder.create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setSource("hallo");
		}).build();
	}

	@Parameter(0)
	public TestParameters params;

	int pageNameCounter =0;
	private String createPageName() {
		pageNameCounter++;
		return "page"+pageNameCounter;
	}

	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> {
			ContentNodeTestDataUtils.clear(node);
		});
	}

	@Test
	public void testPageFilenameRenaming() throws NodeException {
		for (String filename : params.existingFilenames) {
			Builder.create(Page.class, p -> {
				p.setFilename(filename);
				p.setTemplateId(template.getId());
				p.setFolderId(node.getFolder().getId());
				p.setName(createPageName());
			}).build();
		}
		try {
			Page testPage = Builder.create(Page.class, p -> {
				p.setFilename(params.fileNameToGenerate);
				p.setTemplateId(template.getId());
				p.setFolderId(node.getFolder().getId());
				p.setName(createPageName());
			}).build();

			if (params.exceptionExpected) {
				fail ("An exception was expected, but filename was generated: " + testPage.getFilename());
			}
			assertEquals("Filename does not match", params.filenameExpected, testPage.getFilename());
		} catch (NodeException e) {
				if (!params.exceptionExpected) {
					fail("Unexpected exception when renaming page filename");
				}
		}
	}
}
