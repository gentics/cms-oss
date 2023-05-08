package com.gentics.contentnode.tests.publish.cr;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.addTagmapEntry;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectTagDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createVelocityConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishCR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.content.GenticsContentAttribute;

import io.reactivex.Observable;

/**
 * Test cases for rendering various tagmap entries
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.INSTANT_CR_PUBLISHING, Feature.WASTEBIN })
public class CrRenderTagmapTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Integer htmlConstructId;

	private static Integer vtlConstructId;

	private static Template template;

	private static Folder folder;

	private static ContentRepository cr;

	private static final String TAGTYPENAME = "html";

	private static final String TAGNAME = "testtag";

	/**
	 * Name of the template tag (not editable in pages), which is a container for the testtag (which is editable in pages)
	 */
	private static final String TMPL_CONTAINER_TAG = "container";

	private static final String FOLDER_OE_NAME = "folderoe";

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("host", "Node", PublishTarget.CONTENTREPOSITORY));

		htmlConstructId = supply(() -> createConstruct(node, LongHTMLPartType.class, TAGTYPENAME, TAGTYPENAME));
		vtlConstructId = supply(() -> createVelocityConstruct(node, "vtl", "vtl"));

		template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setName("Template");
			tmpl.setFolderId(node.getFolder().getId());

			tmpl.getTemplateTags().put(TAGNAME, create(TemplateTag.class, tTag -> {
				tTag.setConstructId(htmlConstructId);
				tTag.setEnabled(true);
				tTag.setPublic(true);
				tTag.setName(TAGNAME);
			}, false));
			tmpl.getTemplateTag(TAGNAME).getValues().getByKeyname(TAGTYPENAME).setValueText("Default content from the template");

			tmpl.getTemplateTags().put(TMPL_CONTAINER_TAG, create(TemplateTag.class, tTag -> {
				tTag.setConstructId(htmlConstructId);
				tTag.setEnabled(true);
				tTag.setPublic(false);
				tTag.setName(TMPL_CONTAINER_TAG);
			}, false));

			tmpl.getTemplateTag(TMPL_CONTAINER_TAG).getValues().getByKeyname(TAGTYPENAME).setValueText("<node " + TAGNAME + ">");

			tmpl.setSource("Content: [<node " + TAGNAME + ">]");
		}));

		folder = supply(() -> createFolder(node.getFolder(), "Folder"));

		operate(() -> addTagmapEntry(node.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, TAGNAME, TAGNAME, null, false,
				false, true, -1, null, null));

		supply(() -> createObjectTagDefinition(FOLDER_OE_NAME, Folder.TYPE_FOLDER, htmlConstructId, node));

		cr = supply(() -> node.getContentRepository());

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}
	}

	@Parameters(name = "{index}: test: {0}, instant: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		List<TestCase> testCases = Arrays.asList(new TestCase("page.folder.node.folder.object." + FOLDER_OE_NAME, page -> {
			fillObjectTag(node.getFolder(), "Testdata of Root Folder");
			fillObjectTag(folder, "Testdata of Other Folder");
			return page;
		}, res -> {
			assertThat(res.getProperty(TAGNAME)).as("Property").isEqualTo("Testdata of Root Folder");
		}), new TestCase("node.folder.object." + FOLDER_OE_NAME, page -> {
			fillObjectTag(node.getFolder(), "Testdata of Root Folder");
			fillObjectTag(folder, "Testdata of Other Folder");
			return page;
		}, res -> {
			assertThat(res.getProperty(TAGNAME)).as("Property").isEqualTo("Testdata of Root Folder");
		}), new TestCase("folder.object." + FOLDER_OE_NAME, page -> {
			fillObjectTag(node.getFolder(), "Testdata of Root Folder");
			fillObjectTag(folder, "Testdata of Other Folder");
			return page;
		}, res -> {
			assertThat(res.getProperty(TAGNAME)).as("Property").isEqualTo("Testdata of Other Folder");
		}), new TestCase("object." + FOLDER_OE_NAME, page -> {
			fillObjectTag(node.getFolder(), "Testdata of Root Folder");
			fillObjectTag(folder, "Testdata of Other Folder");
			return page;
		}, res -> {
			assertThat(res.getProperty(TAGNAME)).as("Property").isEqualTo("Testdata of Other Folder");
		}), new TestCase("page.tags.overview", page -> {
			operate(() -> update(createPage(folder, template, "Online Page"), Page::publish));
			operate(() -> createPage(folder, template, "Offline Page"));
			operate(() -> update(createPage(folder, template, "Deleted Page"), Page::delete));

			return update(page, upd -> {
				ContentTag cTag = create(ContentTag.class, tag -> {
					tag.setConstructId(vtlConstructId);
					tag.setEnabled(true);
					tag.setName("overview");
				}, false);
				getPartType(LongHTMLPartType.class, cTag, ContentNodeTestDataUtils.TEMPLATE_PARTNAME).getValueObject()
						.setValueText(
								"#set($pages =  $cms.folder.pages)##\n#set($pages = $cms.imps.sorter.sort($pages, \"page.name\"))##\n#foreach($page in $pages)##\n$page.name ($page.online)\n#end##");
				upd.getContent().getContentTags().put("overview", cTag);
			});
		}, res -> {
			assertThat(res.getProperty(TAGNAME)).as("Property").isEqualTo("Offline Page (false)\nOnline Page (true)\nTestpage page.tags.overview (true)\n");
		}), new TestCase("page.tags." + TMPL_CONTAINER_TAG, page -> {

			return update(page, upd -> {
				upd.getContentTag(TAGNAME).getValues().getByKeyname(TAGTYPENAME).setValueText("This comes from the page");
			});
		}, res -> {
			assertThat(res.getProperty(TAGNAME)).as("Property").isEqualTo("This comes from the page");
		}));

		for (TestCase test : testCases) {
			for (boolean instant : Arrays.asList(true, false)) {
				data.add(new Object[] { test, instant });
			}
		}

		return data;
	}

	/**
	 * Fill the object tag of the given folder with the text
	 * @param f folder
	 * @param text object tag text
	 * @throws NodeException
	 */
	private static void fillObjectTag(Folder f, String text) throws NodeException {
		update(f, upd -> {
			ObjectTag objectTag = upd.getObjectTag(FOLDER_OE_NAME);
			getPartType(LongHTMLPartType.class, objectTag, TAGTYPENAME).getValueObject().setValueText(text);
			objectTag.setEnabled(true);
		});
	}

	@Parameter(0)
	public TestCase test;

	@Parameter(1)
	public boolean instant;

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(folder));
		folder = execute(Folder::reload, folder);
	}

	/**
	 * Test publishing the page
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		// set instant publishing flag
		cr = update(cr, upd -> {
			upd.setInstantPublishing(instant);
		});

		// prepare the test page
		Page page = test.prepare();

		// publish the test page
		page = update(page, Page::publish);

		if (instant) {
			// instant publish must have published the page
			consume(p -> assertPublishCR(p, node, true, test.getAsserter()), page);
		} else {
			// page must not have been published (no instant publishing)
			consume(p -> assertPublishCR(p, node, false), page);

			// run the publish process
			try (Trx trx = new Trx()) {
				testContext.publish(false);
				trx.success();
			}

			// page must have been published now
			consume(p -> assertPublishCR(p, node, true, test.getAsserter()), page);
		}
	}

	/**
	 * Class containing test case data
	 */
	public static class TestCase {
		/**
		 * Tagname
		 */
		private String tagname;

		/**
		 * Optional postfix
		 */
		private String postfix;

		/**
		 * Optional page preparator
		 */
		private Function<Page, Page> pagePreparator;

		/**
		 * Asserter
		 */
		private Consumer<Resolvable> asserter;

		/**
		 * Create an instance
		 * @param tagname tag name
		 * @param pagePreparator optional page preparator
		 * @param asserter asserter
		 */
		public TestCase(String tagname, Function<Page, Page> pagePreparator, Consumer<Resolvable> asserter) {
			this(tagname, null, pagePreparator, asserter);
		}

		/**
		 * Create an instance
		 * @param tagname tag name
		 * @param postfix optional postfix to testcase name
		 * @param pagePreparator optional page preparator
		 * @param asserter asserter
		 */
		public TestCase(String tagname, String postfix, Function<Page, Page> pagePreparator, Consumer<Resolvable> asserter) {
			this.tagname = tagname;
			this.postfix = postfix;
			this.pagePreparator = pagePreparator;
			this.asserter = asserter;
		}

		/**
		 * Prepare the test
		 * @return test page
		 * @throws NodeException
		 */
		public Page prepare() throws NodeException {
			Page page = supply(() -> createPage(folder, template, "Testpage " + tagname));
			if (pagePreparator != null) {
				page = execute(pagePreparator, page);
			}

			cr = update(cr, upd -> {
				Observable.fromIterable(upd.getEntries()).filter(entry -> Objects.equals(TAGNAME, entry.getMapname()))
						.blockingForEach(entry -> entry.setTagname(tagname));
			});

			return page;
		}

		/**
		 * Get the asserter
		 * @return asserter
		 */
		public Consumer<Resolvable> getAsserter() {
			return asserter;
		}

		@Override
		public String toString() {
			if (postfix == null) {
				return tagname;
			} else {
				return tagname + " " + postfix;
			}
		}
	}
}
