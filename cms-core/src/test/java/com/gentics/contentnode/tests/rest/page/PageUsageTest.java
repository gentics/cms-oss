package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
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

	protected Page testPage;

	protected Consumer<Tag> REFERENCING_PAGE = tag -> {
		UrlPartType partType = getPartType(UrlPartType.class, tag, "url");
		Value value = partType.getValueObject();
		value.setValueRef(testPage.getId());
		if (partType instanceof PageURLPartType) {
			value.setInfo(1);
		}
	};

	protected Consumer<Tag> SELECTING_PAGE = tag -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(Page.TYPE_PAGE);
		overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
		addOverviewEntriesToOverview(overview, Arrays.asList(testPage.getId()));
	};

	protected Consumer<Tag> SELECTING_PAGE_PARENT = tag -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(Page.TYPE_PAGE);
		overview.setSelectionType(Overview.SELECTIONTYPE_PARENT);
		addOverviewEntriesToOverview(overview, Arrays.asList(testPage.getId()));
	};

	protected Consumer<Tag> SELECTING_FILE = tag -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(File.TYPE_FILE);
		overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
		addOverviewEntriesToOverview(overview, Arrays.asList(testPage.getId()));
	};

	protected Consumer<Tag> SELECTING_IMAGE = tag -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(ImageFile.TYPE_IMAGE);
		overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
		addOverviewEntriesToOverview(overview, Arrays.asList(testPage.getId()));
	};

	protected Consumer<Tag> SELECTING_FOLDER = tag -> {
		Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
		overview.setMaxObjects(1);
		overview.setObjectType(Folder.TYPE_FOLDER);
		overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
		addOverviewEntriesToOverview(overview, Arrays.asList(testPage.getId()));
	};

	protected Consumer<Tag> ENABLED = tag -> tag.setEnabled(true);

	protected Consumer<Tag> DISABLED = tag -> tag.setEnabled(false);

	protected Set<Integer> expectedPageUsage = new HashSet<>();

	protected Set<Integer> expectedVariantUsage = new HashSet<>();

	protected int expectedTotalUsage = 0;

	protected Set<Template> testTemplates = new HashSet<>();

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
	}

	@Parameters(name = "{index}: page in tag: {0}, page in objectag: {1}, overview in tag: {2}, overview in objecttag: {3}, overview in templatetag: {4}, variant: {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (Boolean pageInTag : Arrays.asList(true, false)) {
			for (Boolean pageInObjectTag : Arrays.asList(true, false)) {
				for (Boolean overviewInTag : Arrays.asList(true, false)) {
					for (Boolean overviewInObjectTag : Arrays.asList(true, false)) {
						for (Boolean overviewInTemplateTag : Arrays.asList(true, false)) {
							for (Boolean variant : Arrays.asList(true, false)) {
								data.add(new Object[] { pageInTag, pageInObjectTag, overviewInTag, overviewInObjectTag,
										overviewInTemplateTag, variant });
							}
						}
					}
				}
			}
		}

		return data;
	}

	/**
	 * The test page is directly referenced by another page (in a contenttag with a part of type URL Page)
	 */
	@Parameter(0)
	public boolean pageInTag;

	@Parameter(1)
	public boolean pageInObjectTag;

	@Parameter(2)
	public boolean overviewInTag;

	@Parameter(3)
	public boolean overviewInObjectTag;

	@Parameter(4)
	public boolean overviewInTemplateTag;

	@Parameter(5)
	public boolean variant;

	@Before
	public void setup() throws NodeException {
		// create the test page
		testPage = create(Page.class, create -> {
			create.setFolder(node, node.getFolder());
			create.setTemplateId(template.getId());
		}).build();

		if (pageInTag) {
			// create page referencing the test page via content tag
			expectedPageUsage.add(createPage(PAGEURL_CONTENTTAG, REFERENCING_PAGE, ENABLED));

			// create page referencing the test page in a disabled tag (distractor)
			createPage(PAGEURL_CONTENTTAG, REFERENCING_PAGE, DISABLED);

			// create pages referencing other objects with the same ID as the test page (distractors)
			createPage(FILEURL_CONTENTTAG, REFERENCING_PAGE, ENABLED);
			createPage(IMAGEURL_CONTENTTAG, REFERENCING_PAGE, ENABLED);
			createPage(FOLDERURL_CONTENTTAG, REFERENCING_PAGE, ENABLED);

			// we expect one more page using the test page
			expectedTotalUsage++;
		}
		if (pageInObjectTag) {
			// create page referencing the test page via object tag
			expectedPageUsage.add(createPage(PAGEURL_OBJECTTAG, REFERENCING_PAGE, ENABLED));

			// create page referencing the test page in a disabled object tag (distractor)
			createPage(PAGEURL_OBJECTTAG, REFERENCING_PAGE, DISABLED);

			// create pages referencing other objects with the same ID as the test page via object tags (distractors)
			createPage(FILEURL_OBJECTTAG, REFERENCING_PAGE, ENABLED);
			createPage(IMAGEURL_OBJECTTAG, REFERENCING_PAGE, ENABLED);
			createPage(FOLDERURL_OBJECTTAG, REFERENCING_PAGE, ENABLED);

			// we expect one more page using the test page
			expectedTotalUsage++;
		}
		if (overviewInTag) {
			// create page selecting the test page in an overview
			expectedPageUsage.add(createPage(OVERVIEW_CONTENTTAG, SELECTING_PAGE, ENABLED));

			// create page selecting the test page in an overview of a disabled tag (distractor)
			createPage(OVERVIEW_CONTENTTAG, SELECTING_PAGE, DISABLED);

			// create pages selecting other objects with the same ID in an overview (distractors)
			createPage(OVERVIEW_CONTENTTAG, SELECTING_PAGE_PARENT, ENABLED);
			createPage(OVERVIEW_CONTENTTAG, SELECTING_FOLDER, ENABLED);
			createPage(OVERVIEW_CONTENTTAG, SELECTING_FILE, ENABLED);
			createPage(OVERVIEW_CONTENTTAG, SELECTING_IMAGE, ENABLED);

			// we expect one more page using the test page
			expectedTotalUsage++;
		}
		if (overviewInObjectTag) {
			// create page selecting the test page in an overview of an object tag
			expectedPageUsage.add(createPage(OVERVIEW_OBJECTTAG, SELECTING_PAGE, ENABLED));

			// create page selecting the test page in an overview of a disabled object tag (distractor)
			createPage(OVERVIEW_OBJECTTAG, SELECTING_PAGE, DISABLED);

			// create pages selecting other objects with the same ID in an overview of an object tag (distractors)
			createPage(OVERVIEW_OBJECTTAG, SELECTING_PAGE_PARENT, ENABLED);
			createPage(OVERVIEW_OBJECTTAG, SELECTING_FOLDER, ENABLED);
			createPage(OVERVIEW_OBJECTTAG, SELECTING_FILE, ENABLED);
			createPage(OVERVIEW_OBJECTTAG, SELECTING_IMAGE, ENABLED);

			// we expect one more page using the test page
			expectedTotalUsage++;
		}
		if (overviewInTemplateTag) {
			// create page with template having an overview selecting the page
			expectedPageUsage.add(createTemplateWithPage(OVERVIEW_TEMPLATETAG, SELECTING_PAGE, ENABLED));

			// create page with template having an overview selecting the page in a disabled tag (distractor)
			createTemplateWithPage(OVERVIEW_TEMPLATETAG, SELECTING_PAGE, DISABLED);

			// create pages with templates having an overview selecting other objects with same ID (distractors)
			createTemplateWithPage(OVERVIEW_TEMPLATETAG, SELECTING_PAGE_PARENT, ENABLED);
			createTemplateWithPage(OVERVIEW_TEMPLATETAG, SELECTING_FOLDER, ENABLED);
			createTemplateWithPage(OVERVIEW_TEMPLATETAG, SELECTING_FILE, ENABLED);
			createTemplateWithPage(OVERVIEW_TEMPLATETAG, SELECTING_IMAGE, ENABLED);

			// we expect one more page using the test page
			expectedTotalUsage++;
		}
		if (variant) {
			// create a page variant
			expectedVariantUsage.add(create(Page.class, create -> {
				create.setFolder(node, node.getFolder());
				create.setTemplateId(template.getId());

				create.setContentId(testPage.getContent().getId());
			}).build().getId());

			// we expect one more page using the test page
			expectedTotalUsage++;
		}
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> {
			clear(node);
		});

		operate(() -> {
			for (Template t : testTemplates) {
				t.delete(true);
			}
			testTemplates.clear();
		});
	}

	@Test
	public void test() throws NodeException {
		TotalUsageResponse totalUsageResponse = supply(
				() -> getPageResource().getTotalPageUsage(Arrays.asList(testPage.getId()), null));
		assertResponseOK(totalUsageResponse);

		assertThat(totalUsageResponse.getInfos()).as("Total usage infos").containsOnly(Map.entry(testPage.getId(),
				new TotalUsageInfo().setTotal(expectedTotalUsage).setPages(expectedTotalUsage)));

		PageUsageListResponse pageListResponse = supply(() -> getPageResource().getPageUsageInfo(0, -1, null, null, Arrays.asList(testPage.getId()), null, true,
				new PageModelParameterBean()));
		assertResponseOK(pageListResponse);
		assertThat(pageListResponse.getPages().stream().map(com.gentics.contentnode.rest.model.Page::getId))
				.as("Page IDs").containsOnlyElementsOf(expectedPageUsage);

		pageListResponse = supply(() -> getPageResource().getVariantsUsageInfo(0, -1, null, null, Arrays.asList(testPage.getId()), null,
				true, new PageModelParameterBean()));
		assertResponseOK(pageListResponse);
		assertThat(pageListResponse.getPages().stream().map(com.gentics.contentnode.rest.model.Page::getId))
			.as("Page IDs").containsOnlyElementsOf(expectedVariantUsage);
	}

	@SafeVarargs
	protected final int createPage(Function<Page, Tag> tagFunction, Consumer<Tag>... tagConsumers) throws NodeException {
		return create(Page.class, create -> {
			create.setFolder(node, node.getFolder());
			create.setTemplateId(template.getId());

			Tag tag = tagFunction.apply(create);
			for (Consumer<Tag> consumer : tagConsumers) {
				consumer.accept(tag);
			}
		}).build().getId();
	}

	@SafeVarargs
	protected final int createTemplateWithPage(Function<Template, TemplateTag> tagFunction, Consumer<Tag>...tagConsumers) throws NodeException {
		Template tmpl = create(Template.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setName(RandomStringUtils.randomAlphabetic(10));
			create.setMlId(1);
			create.setSource("");

			Tag tag = tagFunction.apply(create);
			for (Consumer<Tag> consumer : tagConsumers) {
				consumer.accept(tag);
			}
		}).build();

		return create(Page.class, create -> {
			create.setFolder(node, node.getFolder());
			create.setTemplateId(tmpl.getId());
		}).build().getId();

	}
}
