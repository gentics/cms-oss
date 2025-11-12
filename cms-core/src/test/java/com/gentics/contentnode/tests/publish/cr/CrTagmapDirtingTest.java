package com.gentics.contentnode.tests.publish.cr;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.addTagmapEntry;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectTagDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.content.GenticsContentAttribute;

import io.reactivex.Observable;

/**
 * Test cases for dirting of objects based on tagmap entries
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.INSTANT_CR_PUBLISHING, Feature.WASTEBIN })
public class CrTagmapDirtingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Integer htmlConstructId;

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

	private static final String TEMPLATE_OE_NAME = "templateoe";

	private static final String PAGE_OE_NAME = "pageoe";

	private static final List<Template> TEMPLATES = new ArrayList<>();

	/**
	 * Consumer that deletes a given object property (name without object. prefix)
	 */
	private static final BiConsumer<ObjectTagContainer, String> DELETE_OE = (container, name) -> {
		update(container, upd -> {
			Map<String,ObjectTag> objectTags = upd.getObjectTags();
			objectTags.remove(name);
		}).doNotPublish().build();
	};

	/**
	 * Consumer that enables a given object property (name without object. prefix)
	 */
	private static final BiConsumer<ObjectTagContainer, String> ENABLE_OE = (container, name) -> {
		update(container, upd -> {
			ObjectTag objectTag = upd.getObjectTag(name);
			objectTag.setEnabled(true);
		}).doNotPublish().build();
	};

	/**
	 * Consumer that disables a given object property (name without object. prefix)
	 */
	private static final BiConsumer<ObjectTagContainer, String> DISABLE_OE = (container, name) -> {
		update(container, upd -> {
			ObjectTag objectTag = upd.getObjectTag(name);
			objectTag.setEnabled(false);
		}).doNotPublish().build();
	};

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("host", "Node", PublishTarget.CONTENTREPOSITORY));

		htmlConstructId = supply(() -> createConstruct(node, LongHTMLPartType.class, TAGTYPENAME, TAGTYPENAME));

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
		supply(() -> createObjectTagDefinition(TEMPLATE_OE_NAME, Template.TYPE_TEMPLATE, htmlConstructId, node));
		supply(() -> createObjectTagDefinition(PAGE_OE_NAME, Page.TYPE_PAGE, htmlConstructId, node));

		cr = supply(() -> node.getContentRepository());

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		TEMPLATES.addAll(supply(t -> t.getObjects(Template.class, DBUtils.select("SELECT id FROM template", DBUtils.IDS))));
	}

	@Parameters(name = "{index}: test: {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		List<TestCase> testCases = Arrays.asList(
			new TestCase("page.tags.%s".formatted(TAGNAME), "Create template tag", () -> {
				Template tmpTemplate = create(Template.class, tmpl -> {
					tmpl.setName("Template");
					tmpl.setFolderId(node.getFolder().getId());
				});

				return createPage(folder, tmpTemplate, RandomStringUtils.randomAlphabetic(10));
			}, page -> {
				Template tmpTemplate = page.getTemplate();

				update(tmpTemplate, upd -> {
					upd.getTemplateTags().put(TAGNAME, create(TemplateTag.class, tTag -> {
						tTag.setConstructId(htmlConstructId);
						tTag.setEnabled(true);
						tTag.setPublic(false);
						tTag.setName(TAGNAME);
					}, false));
				}).build();
			}),

			new TestCase("page.tags.%s".formatted(TAGNAME), "Update template tag", () -> {
				Template tmpTemplate = create(Template.class, tmpl -> {
					tmpl.setName("Template");
					tmpl.setFolderId(node.getFolder().getId());

					tmpl.getTemplateTags().put(TAGNAME, create(TemplateTag.class, tTag -> {
						tTag.setConstructId(htmlConstructId);
						tTag.setEnabled(true);
						tTag.setPublic(false);
						tTag.setName(TAGNAME);
					}, false));
				});

				return createPage(folder, tmpTemplate, RandomStringUtils.randomAlphabetic(10));
			}, page -> {
				Template tmpTemplate = page.getTemplate();

				update(tmpTemplate, upd -> {
					getPartType(LongHTMLPartType.class, upd.getTemplateTag(TAGNAME), TAGTYPENAME).setText("This is the new content of the template tag");
				}).build();
			}),

			new TestCase("page.object.%s".formatted(FOLDER_OE_NAME), "Create folder object property", () -> {
				Folder testFolder = createFolder(folder, "Testfolder");
				DELETE_OE.accept(testFolder, FOLDER_OE_NAME);

				return createPage(testFolder, template, RandomStringUtils.randomAlphabetic(10));
			}, page -> {
				Folder testFolder = page.getFolder();
				ENABLE_OE.accept(testFolder, FOLDER_OE_NAME);
			}),

			new TestCase("page.object.%s".formatted(FOLDER_OE_NAME), "Enable folder object property", () -> {
				Folder testFolder = createFolder(folder, "Testfolder");

				return createPage(testFolder, template, RandomStringUtils.randomAlphabetic(10));
			}, page -> {
				Folder testFolder = page.getFolder();
				ENABLE_OE.accept(testFolder, FOLDER_OE_NAME);
			}),

			new TestCase("page.object.%s".formatted(FOLDER_OE_NAME), "Disable folder object property", () -> {
				Folder testFolder = createFolder(folder, "Testfolder");
				ENABLE_OE.accept(testFolder, FOLDER_OE_NAME);

				return createPage(testFolder, template, RandomStringUtils.randomAlphabetic(10));
			}, page -> {
				Folder testFolder = page.getFolder();
				DISABLE_OE.accept(testFolder, FOLDER_OE_NAME);
			}),

			new TestCase("page.object.%s".formatted(TEMPLATE_OE_NAME), "Create template object property", () -> {
				Template tmpTemplate = create(Template.class, tmpl -> {
					tmpl.setName("Template");
					tmpl.setFolderId(node.getFolder().getId());
				});
				DELETE_OE.accept(tmpTemplate, TEMPLATE_OE_NAME);

				return createPage(folder, tmpTemplate, RandomStringUtils.randomAlphabetic(10));
			}, page -> {
				Template tmpTemplate = page.getTemplate();
				ENABLE_OE.accept(tmpTemplate, TEMPLATE_OE_NAME);
			}),

			new TestCase("page.object.%s".formatted(TEMPLATE_OE_NAME), "Enable template object property", () -> {
				Template tmpTemplate = create(Template.class, tmpl -> {
					tmpl.setName("Template");
					tmpl.setFolderId(node.getFolder().getId());
				});

				return createPage(folder, tmpTemplate, RandomStringUtils.randomAlphabetic(10));
			}, page -> {
				Template tmpTemplate = page.getTemplate();
				ENABLE_OE.accept(tmpTemplate, TEMPLATE_OE_NAME);
			}),

			new TestCase("page.object.%s".formatted(TEMPLATE_OE_NAME), "Disable template object property", () -> {
				Template tmpTemplate = create(Template.class, tmpl -> {
					tmpl.setName("Template");
					tmpl.setFolderId(node.getFolder().getId());
				});
				ENABLE_OE.accept(tmpTemplate, TEMPLATE_OE_NAME);

				return createPage(folder, tmpTemplate, RandomStringUtils.randomAlphabetic(10));
			}, page -> {
				Template tmpTemplate = page.getTemplate();
				DISABLE_OE.accept(tmpTemplate, TEMPLATE_OE_NAME);
			}),

			new TestCase("page.object.%s".formatted(PAGE_OE_NAME), "Create page object property", () -> {
				Page page = createPage(folder, template, RandomStringUtils.randomAlphabetic(10));
				DELETE_OE.accept(page, PAGE_OE_NAME);

				return page;
			}, page -> {
				ENABLE_OE.accept(page, PAGE_OE_NAME);
			}),

			new TestCase("page.object.%s".formatted(PAGE_OE_NAME), "Enable page object property", () -> {
				return createPage(folder, template, RandomStringUtils.randomAlphabetic(10));
			}, page -> {
				ENABLE_OE.accept(page, PAGE_OE_NAME);
			}),

			new TestCase("page.object.%s".formatted(PAGE_OE_NAME), "Disable page object property", () -> {
				Page page = createPage(folder, template, RandomStringUtils.randomAlphabetic(10));
				ENABLE_OE.accept(page, PAGE_OE_NAME);
				return page;
			}, page -> {
				DISABLE_OE.accept(page, PAGE_OE_NAME);
			})
		);

		for (TestCase test : testCases) {
			data.add(new Object[] { test });
		}

		return data;
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
		 * Description
		 */
		private String description;

		/**
		 * Page supplier
		 */
		private Supplier<Page> pageSupplier;

		/**
		 * Modifier
		 */
		private Consumer<Page> modifier;

		/**
		 * Create an instance
		 * @param tagname tag name
		 * @param pageSupplier page supplier
		 * @param modifier page modifier
		 */
		public TestCase(String tagname, Supplier<Page> pageSupplier, Consumer<Page> modifier) {
			this(tagname, null, pageSupplier, modifier);
		}

		/**
		 * Create an instance
		 * @param tagname tag name
		 * @param description description
		 * @param pageSupplier page supplier
		 * @param modifier page modifier
		 */
		public TestCase(String tagname, String description, Supplier<Page> pageSupplier, Consumer<Page> modifier) {
			this.tagname = tagname;
			this.description = description;
			this.pageSupplier = pageSupplier;
			this.modifier = modifier;
		}

		/**
		 * Prepare the test
		 * @param useTestCaseTagmapEntry true to use the tagmap entry of the test case, false to use a different one
		 * @return test page
		 * @throws NodeException
		 */
		public Page prepare(boolean useTestCaseTagmapEntry) throws NodeException {
			Page page = supply(pageSupplier);

			cr = update(cr, upd -> {
				Observable.fromIterable(upd.getEntries()).filter(entry -> Objects.equals(TAGNAME, entry.getMapname()))
						.blockingForEach(entry -> entry.setTagname(useTestCaseTagmapEntry ? tagname : "page.id"));
			}).build();

			return page;
		}

		/**
		 * Modify something about the page
		 * @param page page
		 * @throws NodeException
		 */
		public void modifyPage(Page page) throws NodeException {
			consume(modifier, page);
		}

		@Override
		public String toString() {
			return description;
		}
	}

	@Parameter(0)
	public TestCase test;

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(folder));
		folder = execute(Folder::reload, folder);

		operate(t -> {
			List<Template> allTemplates = t.getObjects(Template.class, DBUtils.select("SELECT id FROM template", DBUtils.IDS));
			for (Template template : allTemplates) {
				if (!TEMPLATES.contains(template)) {
					template.delete(true);
				}
			}
		});
	}

	/**
	 * Test that the page is dirted due to changes defined by the test case
	 * @throws Exception
	 */
	@Test
	public void testDirting() throws Exception {
		// prepare the test page
		Page page = test.prepare(true);

		// publish the test page
		page = update(page, Page::publish).build();

		// run the publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// modify something about the page
		test.modifyPage(page);

		// wait for the dirtqueueworker
		testContext.waitForDirtqueueWorker();

		// page must be dirted now
		consume(id -> testContext.checkDirtedPages(0, new int[] {id}), page.getId());
	}

	/**
	 * Test that the page is not dirted due to changes defined by the test case, when a different tagmap entry is used
	 * @throws Exception
	 */
	@Test
	public void testNonDirting() throws Exception {
		// prepare the test page
		Page page = test.prepare(false);

		// publish the test page
		page = update(page, Page::publish).build();

		// run the publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// modify something about the page
		test.modifyPage(page);

		// wait for the dirtqueueworker
		testContext.waitForDirtqueueWorker();

		// page must not be dirted now
		operate(() -> testContext.checkDirtedPages(0, new int[] {}));
	}
}
