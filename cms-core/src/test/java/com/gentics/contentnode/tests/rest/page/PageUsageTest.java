package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.OverviewHelper.addOverviewEntriesToOverview;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.UrlPartType;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.TotalUsageInfo;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for getting the usageinfo for pages
 */
@RunWith(Parameterized.class)
public class PageUsageTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Node node;

	protected static Template template;

	protected static Integer pageUrlConstructId;

	protected static Integer fileUrlConstructId;

	protected static Integer imageUrlConstructId;

	protected static Integer folderUrlConstructId;

	protected static Integer overviewConstructId;

	protected static Integer pageTagConstructId;

	protected static Function<Page, Tag> PAGEURL_CONTENTTAG = page -> page.getContent().addContentTag(pageUrlConstructId);
	protected static Function<Page, Tag> PAGEURL_OBJECTTAG = page -> page.getObjectTag("pageurl");
	protected static Function<Page, Tag> FILEURL_CONTENTTAG = page -> page.getContent().addContentTag(fileUrlConstructId);
	protected static Function<Page, Tag> FILEURL_OBJECTTAG = page -> page.getObjectTag("fileurl");
	protected static Function<Page, Tag> IMAGEURL_CONTENTTAG = page -> page.getContent().addContentTag(imageUrlConstructId);
	protected static Function<Page, Tag> IMAGEURL_OBJECTTAG = page -> page.getObjectTag("imageurl");
	protected static Function<Page, Tag> FOLDERURL_CONTENTTAG = page -> page.getContent().addContentTag(folderUrlConstructId);
	protected static Function<Page, Tag> FOLDERURL_OBJECTTAG = page -> page.getObjectTag("folderurl");
	protected static Function<Page, Tag> OVERVIEW_CONTENTTAG = page -> page.getContent().addContentTag(overviewConstructId);
	protected static Function<Page, Tag> OVERVIEW_OBJECTTAG = page -> page.getObjectTag("overview");
	protected static Function<Page, Tag> PAGETAG_CONTENTTAG = page -> page.getContent().addContentTag(pageTagConstructId);

	protected static Function<Template, TemplateTag> OVERVIEW_TEMPLATETAG = template -> {
		Map<String, TemplateTag> tags = template.getTags();

		TemplateTag tag = create(TemplateTag.class, create -> {
			create.setConstructId(overviewConstructId);
			create.setName("overview");
			create.setPublic(false);
		}).doNotSave().build();

		tags.put("overview", tag);

		return tag;
	};

	protected static BiConsumer<Page, Tag> REFERENCING_PAGE = (page, tag) -> {
		UrlPartType partType = getPartType(UrlPartType.class, tag, "url");
		Value value = partType.getValueObject();
		value.setValueRef(page.getId());
		if (partType instanceof PageURLPartType) {
			value.setInfo(1);
		}
	};

	protected static BiConsumer<Page, Tag> SELECTING_PAGE = (page, tag) -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(Page.TYPE_PAGE);
		overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
		addOverviewEntriesToOverview(overview, Arrays.asList(page.getId()));
	};

	protected static BiConsumer<Page, Tag> SELECTING_PAGE_PARENT = (page, tag) -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(Page.TYPE_PAGE);
		overview.setSelectionType(Overview.SELECTIONTYPE_PARENT);
		addOverviewEntriesToOverview(overview, Arrays.asList(page.getId()));
	};

	protected static BiConsumer<Page, Tag> SELECTING_FILE = (page, tag) -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(File.TYPE_FILE);
		overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
		addOverviewEntriesToOverview(overview, Arrays.asList(page.getId()));
	};

	protected static BiConsumer<Page, Tag> SELECTING_IMAGE = (page, tag) -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(ImageFile.TYPE_IMAGE);
		overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
		addOverviewEntriesToOverview(overview, Arrays.asList(page.getId()));
	};

	protected static BiConsumer<Page, Tag> SELECTING_FOLDER = (page, tag) -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(Folder.TYPE_FOLDER);
		overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
		addOverviewEntriesToOverview(overview, Arrays.asList(page.getId()));
	};

	protected static BiConsumer<Page, Tag> REFERENCING_PAGETAG = (page, tag) -> {
		PageTagPartType partType = getPartType(PageTagPartType.class, tag, "tag");
		Optional<Tag> optPageTag = page.getTags().values().stream().findFirst();
		if (optPageTag.isPresent()) {
			partType.setPageTag(page, optPageTag.get());
		}
	};

	protected static BiConsumer<Page, Tag> ENABLED = (page, tag) -> tag.setEnabled(true);

	protected static BiConsumer<Page, Tag> DISABLED = (page, tag) -> tag.setEnabled(false);

	protected static Map<TestCaseKey, TestCase> testCases = new HashMap<>();

	/**
	 * Setup test data
	 * @throws NodeException
	 * @throws IOException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());

		template = create(Template.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setName("Test Template");
			create.setMlId(1);
			create.setSource("");
		}).build();

		// constructs for object references
		pageUrlConstructId = supply(() -> createConstruct(node, PageURLPartType.class, "pageurl", "url"));
		fileUrlConstructId = supply(() -> createConstruct(node, FileURLPartType.class, "fileurl", "url"));
		imageUrlConstructId = supply(() -> createConstruct(node, ImageURLPartType.class, "imageurl", "url"));
		folderUrlConstructId = supply(() -> createConstruct(node, FolderURLPartType.class, "folderurl", "url"));

		// object properties for object references
		supply(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, pageUrlConstructId, "Page URL", "pageurl"));
		supply(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, fileUrlConstructId, "File URL", "fileurl"));
		supply(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, imageUrlConstructId, "Image URL", "imageurl"));
		supply(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, folderUrlConstructId, "Folder URL", "folderurl"));

		// construct for an overview
		overviewConstructId = supply(() -> createConstruct(node, OverviewPartType.class, "overview", "ds"));
		// object property for overview
		supply(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, overviewConstructId, "Overview", "overview"));

		// construct for pagetag
		pageTagConstructId = supply(() -> createConstruct(node, PageTagPartType.class, "pagetag", "tag"));

		for (Object[] test : data()) {
			TestCaseKey key = TestCaseKey.class.cast(test[0]);
			testCases.put(key, new TestCase(key));
		}
	}

	@Parameters(name = "{index}: case: {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (boolean pageInTag : Arrays.asList(true, false)) {
			for (boolean pageInObjectTag : Arrays.asList(true, false)) {
				for (boolean overviewInTag : Arrays.asList(true, false)) {
					for (boolean overviewInObjectTag : Arrays.asList(true, false)) {
						for (boolean overviewInTemplateTag : Arrays.asList(true, false)) {
							for (boolean variant : Arrays.asList(true, false)) {
								for (boolean pageTag : Arrays.asList(true, false)) {
									data.add(new Object[] { new TestCaseKey(pageInTag, pageInObjectTag, overviewInTag,
											overviewInObjectTag, overviewInTemplateTag, variant, pageTag) });
								}
							}
						}
					}
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public TestCaseKey key;

	protected TestCase testCase;

	@Before
	public void setup() throws NodeException {
		testCase = testCases.get(key);
	}

	/**
	 * Test getting the total page usage
	 * @throws NodeException
	 */
	@Test
	public void testTotalUsage() throws NodeException {
		TotalUsageResponse totalUsageResponse = supply(
				() -> getPageResource().getTotalPageUsage(Arrays.asList(testCase.testPage.getId()), null));
		assertResponseOK(totalUsageResponse);

		assertThat(totalUsageResponse.getInfos()).as("Total usage infos").containsOnly(Map.entry(testCase.testPage.getId(),
				new TotalUsageInfo().setTotal(testCase.expectedTotalUsage).setPages(testCase.expectedTotalUsage)));
	}

	/**
	 * Test getting the page usage
	 * @throws NodeException
	 */
	@Test
	public void testPageUsage() throws NodeException {
		PageUsageListResponse pageListResponse = supply(() -> getPageResource().getPageUsageInfo(0, -1, null, null, Arrays.asList(testCase.testPage.getId()), null, true,
				new PageModelParameterBean()));
		assertResponseOK(pageListResponse);
		assertThat(pageListResponse.getPages().stream().map(com.gentics.contentnode.rest.model.Page::getId))
		.as("Page IDs").containsOnlyElementsOf(testCase.expectedPageUsage);
	}

	/**
	 * Test getting the variants usage
	 * @throws NodeException
	 */
	@Test
	public void testVariantsUsage() throws NodeException {
		PageUsageListResponse pageListResponse = supply(() -> getPageResource().getVariantsUsageInfo(0, -1, null, null, Arrays.asList(testCase.testPage.getId()), null,
				true, new PageModelParameterBean()));
		assertResponseOK(pageListResponse);
		assertThat(pageListResponse.getPages().stream().map(com.gentics.contentnode.rest.model.Page::getId))
			.as("Page IDs").containsOnlyElementsOf(testCase.expectedVariantUsage);
	}

	/**
	 * Test using the tag usage
	 * @throws NodeException
	 */
	@Test
	public void testTagUsage() throws NodeException {
		PageUsageListResponse pageListResponse = supply(() -> getPageResource().getPagetagUsageInfo(0, -1, null, null, Arrays.asList(testCase.testPage.getId()),
				null, true, new PageModelParameterBean()));
		assertResponseOK(pageListResponse);
		assertThat(pageListResponse.getPages().stream().map(com.gentics.contentnode.rest.model.Page::getId))
			.as("Page IDs").containsOnlyElementsOf(testCase.expectedTagUsage);
	}

	/**
	 * TestCase Key
	 */
	protected static record TestCaseKey(boolean pageInTag, boolean pageInObjectTag, boolean overviewInTag,
			boolean overviewInObjectTag, boolean overviewInTemplateTag, boolean variant, boolean pageTag) {
	}

	/**
	 * TestCase
	 */
	protected static class TestCase {
		protected TestCaseKey key;

		protected Folder testFolder;

		protected Page testPage;
		protected Set<Integer> expectedPageUsage = new HashSet<>();
		protected Set<Integer> expectedVariantUsage = new HashSet<>();
		protected Set<Integer> expectedTagUsage = new HashSet<>();
		protected int expectedTotalUsage = 0;

		/**
		 * Create the TestCase for the given key
		 * @param key key
		 * @throws NodeException
		 */
		public TestCase(TestCaseKey key) throws NodeException {
			this.key = key;

			testFolder = create(Folder.class, create -> {
				create.setMotherId(node.getFolder().getId());
				create.setName(key.toString());
				create.setPublishDir("/%d".formatted(key.hashCode()));
			}).build();

			// create the test page
			testPage = create(Page.class, create -> {
				create.setFolder(node, testFolder);
				create.setTemplateId(template.getId());

				Tag tag = PAGEURL_CONTENTTAG.apply(create);
				ENABLED.accept(create, tag);
			}).build();

			if (key.pageInTag) {
				// create page referencing the test page via content tag
				expectedPageUsage.add(createPage(testPage, PAGEURL_CONTENTTAG, REFERENCING_PAGE, ENABLED));

				// create page referencing the test page in a disabled tag (distractor)
				createPage(testPage, PAGEURL_CONTENTTAG, REFERENCING_PAGE, DISABLED);

				// create pages referencing other objects with the same ID as the test page (distractors)
				createPage(testPage, FILEURL_CONTENTTAG, REFERENCING_PAGE, ENABLED);
				createPage(testPage, IMAGEURL_CONTENTTAG, REFERENCING_PAGE, ENABLED);
				createPage(testPage, FOLDERURL_CONTENTTAG, REFERENCING_PAGE, ENABLED);

				// we expect one more page using the test page
				expectedTotalUsage++;
			}
			if (key.pageInObjectTag) {
				// create page referencing the test page via object tag
				expectedPageUsage.add(createPage(testPage, PAGEURL_OBJECTTAG, REFERENCING_PAGE, ENABLED));

				// create page referencing the test page in a disabled object tag (distractor)
				createPage(testPage, PAGEURL_OBJECTTAG, REFERENCING_PAGE, DISABLED);

				// create pages referencing other objects with the same ID as the test page via object tags (distractors)
				createPage(testPage, FILEURL_OBJECTTAG, REFERENCING_PAGE, ENABLED);
				createPage(testPage, IMAGEURL_OBJECTTAG, REFERENCING_PAGE, ENABLED);
				createPage(testPage, FOLDERURL_OBJECTTAG, REFERENCING_PAGE, ENABLED);

				// we expect one more page using the test page
				expectedTotalUsage++;
			}
			if (key.overviewInTag) {
				// create page selecting the test page in an overview
				expectedPageUsage.add(createPage(testPage, OVERVIEW_CONTENTTAG, SELECTING_PAGE, ENABLED));

				// create page selecting the test page in an overview of a disabled tag (distractor)
				createPage(testPage, OVERVIEW_CONTENTTAG, SELECTING_PAGE, DISABLED);

				// create pages selecting other objects with the same ID in an overview (distractors)
				createPage(testPage, OVERVIEW_CONTENTTAG, SELECTING_PAGE_PARENT, ENABLED);
				createPage(testPage, OVERVIEW_CONTENTTAG, SELECTING_FOLDER, ENABLED);
				createPage(testPage, OVERVIEW_CONTENTTAG, SELECTING_FILE, ENABLED);
				createPage(testPage, OVERVIEW_CONTENTTAG, SELECTING_IMAGE, ENABLED);

				// we expect one more page using the test page
				expectedTotalUsage++;
			}
			if (key.overviewInObjectTag) {
				// create page selecting the test page in an overview of an object tag
				expectedPageUsage.add(createPage(testPage, OVERVIEW_OBJECTTAG, SELECTING_PAGE, ENABLED));

				// create page selecting the test page in an overview of a disabled object tag (distractor)
				createPage(testPage, OVERVIEW_OBJECTTAG, SELECTING_PAGE, DISABLED);

				// create pages selecting other objects with the same ID in an overview of an object tag (distractors)
				createPage(testPage, OVERVIEW_OBJECTTAG, SELECTING_PAGE_PARENT, ENABLED);
				createPage(testPage, OVERVIEW_OBJECTTAG, SELECTING_FOLDER, ENABLED);
				createPage(testPage, OVERVIEW_OBJECTTAG, SELECTING_FILE, ENABLED);
				createPage(testPage, OVERVIEW_OBJECTTAG, SELECTING_IMAGE, ENABLED);

				// we expect one more page using the test page
				expectedTotalUsage++;
			}
			if (key.overviewInTemplateTag) {
				// create page with template having an overview selecting the page
				expectedPageUsage.add(createTemplateWithPage(testPage, OVERVIEW_TEMPLATETAG, SELECTING_PAGE, ENABLED));

				// create page with template having an overview selecting the page in a disabled tag (distractor)
				createTemplateWithPage(testPage, OVERVIEW_TEMPLATETAG, SELECTING_PAGE, DISABLED);

				// create pages with templates having an overview selecting other objects with same ID (distractors)
				createTemplateWithPage(testPage, OVERVIEW_TEMPLATETAG, SELECTING_PAGE_PARENT, ENABLED);
				createTemplateWithPage(testPage, OVERVIEW_TEMPLATETAG, SELECTING_FOLDER, ENABLED);
				createTemplateWithPage(testPage, OVERVIEW_TEMPLATETAG, SELECTING_FILE, ENABLED);
				createTemplateWithPage(testPage, OVERVIEW_TEMPLATETAG, SELECTING_IMAGE, ENABLED);

				// we expect one more page using the test page
				expectedTotalUsage++;
			}
			if (key.variant) {
				// create a page variant
				expectedVariantUsage.add(create(Page.class, create -> {
					create.setFolder(node, testFolder);
					create.setTemplateId(template.getId());

					create.setContentId(testPage.getContent().getId());
				}).build().getId());

				// we expect one more page using the test page
				expectedTotalUsage++;
			}
			if (key.pageTag) {
				// create a page using a tag of the testpage via pagetag
				expectedTagUsage.add(createPage(testPage, PAGETAG_CONTENTTAG, REFERENCING_PAGETAG, ENABLED));

				createPage(testPage, PAGETAG_CONTENTTAG, REFERENCING_PAGETAG, DISABLED);

				expectedTotalUsage++;
			}
		}

		/**
		 * Create a page using the test page in a way
		 * @param testPage test page
		 * @param tagFunction function to create a tag
		 * @param tagConsumers consumers that will modify the tag
		 * @return id of the generated page
		 * @throws NodeException
		 */
		@SafeVarargs
		protected final int createPage(Page testPage, Function<Page, Tag> tagFunction, BiConsumer<Page, Tag>... tagConsumers) throws NodeException {
			return create(Page.class, create -> {
				create.setFolder(node, testFolder);
				create.setTemplateId(template.getId());

				Tag tag = tagFunction.apply(create);
				for (BiConsumer<Page, Tag> consumer : tagConsumers) {
					consumer.accept(testPage, tag);
				}
			}).build().getId();
		}

		/**
		 * Create a template and a page using the test page in a way
		 * @param testPage test page
		 * @param tagFunction function to create a tag in the template
		 * @param tagConsumers consumer that will modify the tag
		 * @return id of the generated page
		 * @throws NodeException
		 */
		@SafeVarargs
		protected final int createTemplateWithPage(Page testPage, Function<Template, TemplateTag> tagFunction, BiConsumer<Page, Tag>...tagConsumers) throws NodeException {
			Template tmpl = create(Template.class, create -> {
				create.setFolderId(testFolder.getId());
				create.setName(RandomStringUtils.randomAlphabetic(10));
				create.setMlId(1);
				create.setSource("");

				Tag tag = tagFunction.apply(create);
				for (BiConsumer<Page, Tag> consumer : tagConsumers) {
					consumer.accept(testPage, tag);
				}
			}).build();

			return create(Page.class, create -> {
				create.setFolder(node, testFolder);
				create.setTemplateId(tmpl.getId());
			}).build().getId();
		}
	}
}
