package com.gentics.contentnode.tests.factory.page;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

@RunWith(value = Parameterized.class)
public class FilenameSuggestionTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Template htmlTemplate;
	private static Template genericTemplate;
	private static Page htmlPageNoLanguage;
	private static Page genericPageNoLanguage;
	private static Page htmlPage;
	private static Page genericPage;

	@Parameters(name = "{index}: getFilenameAsPagename {0}, filenameForcetolower {1}, contentgroup3Pagefilename {2}, contentgroup3PagefilenameNoApachefilename {3}, fixed {4}, html {5}, language {6}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean getFilenameAsPagename : Arrays.asList(true, false)) {
			for (boolean filenameForcetolower : Arrays.asList(true, false)) {
				if (!getFilenameAsPagename && !filenameForcetolower) {
					continue;
				}
				for (boolean contentgroup3Pagefilename : Arrays.asList(true, false)) {
					for (boolean contentgroup3PagefilenameNoApachefilename : Arrays.asList(true, false)) {
						if (!contentgroup3Pagefilename && contentgroup3PagefilenameNoApachefilename) {
							continue;
						}
						for (boolean fixedExtension: Arrays.asList(true, false)) {
							for (boolean htmlPage: Arrays.asList(true, false)) {
								for (boolean language : Arrays.asList(true, false)) {
									data.add(new Object[] { getFilenameAsPagename, filenameForcetolower, contentgroup3Pagefilename,
											contentgroup3PagefilenameNoApachefilename, fixedExtension, htmlPage, language });
								}
							}
						}
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		operate(() -> DBUtils.executeUpdate("INSERT IGNORE INTO ml (id, name, ext, contenttype) VALUES (?, ?, ?, ?)", new Object[] {20, "Generic", "", "text/plain"}));

		node = supply(() -> createNode("node", "Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		htmlTemplate = supply(() -> create(Template.class, t -> {
			t.setSource("");
			t.setName("HTML");
			t.setMlId(1);
			t.addFolder(node.getFolder());
		}));
		genericTemplate = supply(() -> create(Template.class, t -> {
			t.setSource("");
			t.setName("Generic");
			t.setMlId(20);
			t.addFolder(node.getFolder());
		}));

		htmlPageNoLanguage = supply(() -> createPage(node.getFolder(), htmlTemplate, "HTML Page"));
		htmlPage = supply(() -> createPage(node.getFolder(), htmlTemplate, "HTML Page (De)", null, getLanguage("de")));
		genericPageNoLanguage = supply(() -> createPage(node.getFolder(), genericTemplate, "Generic Page"));
		genericPage = supply(() -> createPage(node.getFolder(), genericTemplate, "Generic Page (De)", null, getLanguage("de")));
	}

	@Parameter(0)
	public boolean getFilenameAsPagename;

	@Parameter(1)
	public boolean filenameForcetolower;

	@Parameter(2)
	public boolean contentgroup3Pagefilename;

	@Parameter(3)
	public boolean contentgroup3PagefilenameNoApachefilename;

	@Parameter(4)
	public boolean fixedExtension;

	@Parameter(5)
	public boolean html;

	@Parameter(6)
	public boolean language;

	@Test
	public void testPage() throws NodeException {
		Page testedPage = html ? (language ? htmlPage : htmlPageNoLanguage) : (language ? genericPage : genericPageNoLanguage);
		String suggestedFilename = Trx.execute(page -> {
			try (FeatureClosure f1 = new FeatureClosure(Feature.GET_FILENAME_AS_PAGENAME, getFilenameAsPagename);
					FeatureClosure f2 = new FeatureClosure(Feature.FILENAME_FORCETOLOWER, filenameForcetolower);
					FeatureClosure f3 = new FeatureClosure(Feature.CONTENTGROUP3_PAGEFILENAME, contentgroup3Pagefilename);
					FeatureClosure f4 = new FeatureClosure(Feature.CONTENTGROUP3_PAGEFILENAME_NO_APACHEFILENAME, contentgroup3PagefilenameNoApachefilename)) {
				if (fixedExtension) {
					return PageFactory.suggestFilename(page, p -> "bla");
				} else {
					return PageFactory.suggestFilename(page);
				}
			}
		}, testedPage);

		assertThat(suggestedFilename).as("Suggested filename").isEqualTo(getExpectedFilename(testedPage.getId()));
	}

	/**
	 * Get the expected filename
	 * @param pageId page ID
	 * @return expected filename
	 */
	protected String getExpectedFilename(int pageId) {
		String base = "";
		String langExt = "";
		String ext = "";
		String expected = null;
		if (fixedExtension) {
			ext = ".bla";
		} else if (html) {
			ext = ".html";
		}

		if (getFilenameAsPagename) {
			if (html) {
				if (language) {
					base = "HTML-Page-(De)";
				} else {
					base = "HTML-Page";
				}
			} else {
				if (language) {
					base = "Generic-Page-(De)";
				} else {
					base = "Generic-Page";
				}
			}
		} else {
			base = Integer.toString(pageId);
		}

		if (contentgroup3Pagefilename && language) {
			langExt = ".de";
		}

		if (contentgroup3PagefilenameNoApachefilename) {
			expected = String.format("%s%s%s", base, langExt, ext);
		} else {
			expected = String.format("%s%s%s", base, ext, langExt);
		}

		if (filenameForcetolower) {
			expected = expected.toLowerCase();
		}
		return expected;
	}
}
