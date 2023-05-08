package com.gentics.contentnode.tests.publish.attributes;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.tests.publish.filesystem.FilesystemPublishTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.CountDirective;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for publishing into the filesystem with attribute dirting (these tests only make sense for pages)
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTITHREADED_PUBLISHING, Feature.ATTRIBUTE_DIRTING })
public class AttributeFilesystemPublishTest extends FilesystemPublishTest {
	/**
	 * Template for the vtl construct that counts, how often it is rendered
	 */
	public final static String VTL = "value of $cms.tag.name: [#gtx_test_count($cms.tag.name)]";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(prefs -> {
		prefs.set("contentnode.velocity.userdirective", CountDirective.class.getName());
	});

	/**
	 * Tagname
	 */
	public final static String TAGNAME = "content";

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: type {0}, language {1}, pageLanguageCode {2}, omitPageExtension {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedType type : Arrays.asList(TestedType.page)) {
			for (boolean language : Arrays.asList(true, false)) {
				// languages make only sense for pages
				if (type != TestedType.page && language) {
					continue;
				}
				for (PageLanguageCode pageLanguageCode : PageLanguageCode.values()) {
					for (boolean omitPageExtension : Arrays.asList(true, false)) {
						data.add(new Object[] { type, language, pageLanguageCode, omitPageExtension });
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("hostname", "Node name", PublishTarget.FILESYSTEM, getLanguage("de"), getLanguage("en")));
		otherNode = supply(() -> createNode("otherhost", "Other Node name", PublishTarget.FILESYSTEM));

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Override
	public DBTestContext getTestContext() {
		return testContext;
	}

	@Override
	public void prepareData() throws Exception {
		super.prepareData();
		CountDirective.reset();
	}

	@Override
	protected void preparePage(Page page) throws NodeException {
		super.preparePage(page);
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = ContentNodeTestDataUtils.createVelocityConstruct(node);
		construct.getParts().stream().filter(part -> "template".equals(part.getKeyname())).forEach(part -> {
			try {
				part.getDefaultValue().setValueText(VTL);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Setup of construct failed");
			}
		});
		construct.save();
		Template template = t.getObject(page.getTemplate(), true);
		template.setSource("<node " + TAGNAME + ">");
		TemplateTag templateTag = ContentNodeTestDataUtils.createTemplateTag(construct.getId(), TAGNAME, false, false);
		template.getTemplateTags().put(TAGNAME, templateTag);
		template.save();
	}

	@Override
	@Test
	public void testChangeFilename() throws Exception {
		// the page will be republished -> content will be rendered
		try (AutoCloseable asserter = CountDirective.asserter(TAGNAME, 1)) {
			super.testChangeFilename();
		}
	}

	@Override
	@Test
	public void testChangeFolderPubdir() throws Exception {
		// page will be dirted as dependency -> content not rendered
		try (AutoCloseable asserter = CountDirective.asserter(TAGNAME, 0)) {
			super.testChangeFolderPubdir();
		}
	}

	@Override
	@Test
	public void testChangeHostname() throws Exception {
		// page will be dirted as dependency -> content not rendered
		try (AutoCloseable asserter = CountDirective.asserter(TAGNAME, 0)) {
			super.testChangeHostname();
		}
	}

	@Override
	@Test
	public void testMoveFolderToNode() throws Exception {
		// page will be dirted as as a whole -> content will be rendered
		try (AutoCloseable asserter = CountDirective.asserter(TAGNAME, 1)) {
			super.testMoveFolderToNode();
		}
	}

	@Override
	@Test
	public void testMoveToFolder() throws Exception {
		// move dirts page as a whole
		try (AutoCloseable asserter = CountDirective.asserter(TAGNAME, 1)) {
			super.testMoveToFolder();
		}
	}

	@Override
	@Test
	public void testMoveToNode() throws Exception {
		// move dirts page as a whole
		try (AutoCloseable asserter = CountDirective.asserter(TAGNAME, 1)) {
			super.testMoveToNode();
		}
	}
}
