package com.gentics.contentnode.tests.publish.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.contentnode.parttype.ExtensiblePartType;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.BreadcrumbPartType;
import com.gentics.contentnode.object.parttype.ChangeableListPartType;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.DHTMLPartType;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.ExtensiblePartTypeWrapper;
import com.gentics.contentnode.object.parttype.FilePartType;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.HTMLTextPartType;
import com.gentics.contentnode.object.parttype.ImageHeightPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.ImageWidthPartType;
import com.gentics.contentnode.object.parttype.JavaEditorPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLTextPartType;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.NavigationPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.object.parttype.OrderedListPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.SelectClassPartType;
import com.gentics.contentnode.object.parttype.SelectPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.object.parttype.TablePartType;
import com.gentics.contentnode.object.parttype.TemplateTagPartType;
import com.gentics.contentnode.object.parttype.UnorderedListPartType;
import com.gentics.contentnode.object.parttype.VelocityPartType;
import com.gentics.contentnode.publish.wrapper.PublishablePage;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for rendering pages from instances of {@link PublishablePage}
 */
@RunWith(value = Parameterized.class)
public class PublishablePageTest {
	/**
	 * Construct Keyword
	 */
	private static final String CONSTRUCT_KEYWORD = "testconstruct";

	/**
	 * Part keyword for the tested construct
	 */
	private static final String PART_KEYWORD = "testpart";

	/**
	 * Name of the template tag
	 */
	private final static String TEMPLATE_TAG_NAME = "content";

	/**
	 * Test content for the list parttypes
	 */
	private static final String TEST_LIST = "one\ntwo\nthree";

	/**
	 * Name of the template tag, that is target of the TemplateTagPartType
	 */
	private final static String TARGET_TEMPLATETAG_NAME = "target";

	/**
	 * Test cnntent for text based parttypes
	 */
	private static final String TEST_CONTENT = "blablubb";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext(false);

	/**
	 * Node
	 */
	protected static Node node;

	/**
	 * Template
	 */
	protected static Template template;

	/**
	 * ID of the HTML construct
	 */
	protected static int htmlConstructId;

	/**
	 * Target page
	 */
	protected static Page targetPage;

	/**
	 * Target image
	 */
	protected static ImageFile targetImage;

	/**
	 * Target file
	 */
	protected static File targetFile;

	/**
	 * Datasource
	 */
	protected static Datasource datasource;

	/**
	 * Test implementation
	 */
	protected PartTypeTest test;

	/**
	 * Construct ID
	 */
	protected int constructId;

	/**
	 * Page
	 */
	protected Page page;

	@Parameters(name = "{index}: test {0}")
	public static Collection<Object[]> data() {
		List<? extends PartTypeTest> tests = Arrays.asList(new PartTypeTest(NormalTextPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(HTMLTextPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(HTMLPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(PageURLPartType.class, "internal") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.getPartType(PageURLPartType.class, tag, PART_KEYWORD).setTargetPage(targetPage);
			}
		}, new PartTypeTest(PageURLPartType.class, "external") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.getPartType(PageURLPartType.class, tag, PART_KEYWORD).setExternalTarget("http://www.orf.at/");
			}
		}, new PartTypeTest(ImageURLPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.getPartType(ImageURLPartType.class, tag, PART_KEYWORD).setTargetImage(targetImage);
			}
		}, new PartTypeTest(FileURLPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.getPartType(FileURLPartType.class, tag, PART_KEYWORD).setTargetFile(targetFile);
			}
		}, new PartTypeTest(ShortTextPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(LongHTMLTextPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(PageTagPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.getPartType(PageTagPartType.class, tag, PART_KEYWORD).setPageTag(targetPage, targetPage.getTag(TEMPLATE_TAG_NAME));
			}
		}, new PartTypeTest(ChangeableListPartType.class, "unordered") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				Value value = tag.getValues().iterator().next();
				value.setInfo(0);
				value.setValueText(TEST_LIST);
			}
		}, new PartTypeTest(ChangeableListPartType.class, "ordered") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				Value value = tag.getValues().iterator().next();
				value.setInfo(1);
				value.setValueText(TEST_LIST);
			}
		}, new PartTypeTest(UnorderedListPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				Value value = tag.getValues().iterator().next();
				value.setValueText(TEST_LIST);
			}
		}, new PartTypeTest(OrderedListPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				Value value = tag.getValues().iterator().next();
				value.setValueText(TEST_LIST);
			}
		}, new PartTypeTest(ImageHeightPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				Value value = tag.getValues().iterator().next();
				value.setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(ImageWidthPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				Value value = tag.getValues().iterator().next();
				value.setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(TemplateTagPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.getPartType(TemplateTagPartType.class, tag, PART_KEYWORD).setTemplateTag(template, template.getTemplateTag(TARGET_TEMPLATETAG_NAME));
			}
		}, new PartTypeTest(LongHTMLPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(FilePartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(TablePartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				// make a 2x2 table
				tag.getValues().iterator().next().setValueText("2;2");

				// add the cell tags
				addTag(page, htmlConstructId, tag.getName() + ".A1", "top left");
				addTag(page, htmlConstructId, tag.getName() + ".B1", "top right");
				addTag(page, htmlConstructId, tag.getName() + ".A2", "bottom left");
				addTag(page, htmlConstructId, tag.getName() + ".B2", "bottom right");
			}
		}, new PartTypeTest(SelectClassPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(NodePartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.getPartType(NodePartType.class, tag, PART_KEYWORD).setNode(node);
			}
		}, new PartTypeTest(FolderURLPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.getPartType(FolderURLPartType.class, tag, PART_KEYWORD).setTargetFolder(node.getFolder());
			}
		}, new PartTypeTest(JavaEditorPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(DHTMLPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(SingleSelectPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(datasource.getEntries().get(1).getDsid() + "");
			}
		}, new PartTypeTest(MultiSelectPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(datasource.getEntries().get(1).getDsid() + "|-|" + datasource.getEntries().get(3).getDsid());
			}
		}, new PartTypeTest(CheckboxPartType.class, "checked") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText("1");
			}
		}, new PartTypeTest(CheckboxPartType.class, "unchecked") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText("0");
			}
		}, new PartTypeTest(DatasourcePartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				Datasource ds = ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, tag, PART_KEYWORD).getDatasource();
				ContentNodeTestDataUtils.fillDatasource(ds, Arrays.asList("Uno", "Due", "Tre"));
			}
		}, new PartTypeTest(HTMLPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(NormalTextPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().iterator().next().setValueText(TEST_CONTENT);
			}
		}, new PartTypeTest(ExtensiblePartTypeWrapper.class, VelocityPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				tag.getValues().getByKeyname("template").setValueText("$page.name");
			}
		}, new PartTypeTest(ExtensiblePartTypeWrapper.class, BreadcrumbPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
			}
		}, new PartTypeTest(ExtensiblePartTypeWrapper.class, NavigationPartType.class, null) {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
			}
		}, new PartTypeTest(OverviewPartType.class, "single page") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.fillOverview(tag, PART_KEYWORD, "<node page.name>", Page.class, Overview.SELECTIONTYPE_SINGLE, 0, Overview.ORDER_NAME,
						Overview.ORDERWAY_ASC, false, Arrays.asList(targetPage));
			}
		}, new PartTypeTest(OverviewPartType.class, "pages by folder, asc") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.fillOverview(tag, PART_KEYWORD, "<node page.name>", Page.class, Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME,
						Overview.ORDERWAY_ASC, false, Arrays.asList(node.getFolder()));
			}
		}, new PartTypeTest(OverviewPartType.class, "pages by folder, desc") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.fillOverview(tag, PART_KEYWORD, "<node page.name>", Page.class, Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME,
						Overview.ORDERWAY_DESC, false, Arrays.asList(node.getFolder()));
			}
		}, new PartTypeTest(OverviewPartType.class, "files") {
			@Override
			protected void fillContentTag(Page page, ContentTag tag) throws Exception {
				ContentNodeTestDataUtils.fillOverview(tag, PART_KEYWORD, "<node file.name>", File.class, Overview.SELECTIONTYPE_PARENT, 0, Overview.ORDER_NAME,
						Overview.ORDERWAY_ASC, false, null);
			}
		});

		Collection<Object[]> data = new ArrayList<Object[]>();
		for (PartTypeTest test : tests) {
			data.add(new Object[] {test});
		}
		return data;
	}

	@BeforeClass
	public static void setUpOnce() throws Exception {
		node = ContentNodeTestDataUtils.createNode("Test", "test", "/", null, false, false);

		htmlConstructId = ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, "text", "text");

		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		renderType.setRenderUrlFactory(new StaticUrlFactory(RenderUrl.LINKWAY_AUTO, RenderUrl.LINKWAY_AUTO, null));
		renderType.setFrontEnd(true);

		template = t.createObject(Template.class);
		template.setMlId(1);
		template.setName("Testtemplate");
		template.setSource("<node " + TEMPLATE_TAG_NAME + ">");
		template.getFolders().add(node.getFolder());
		TemplateTag templateTag = t.createObject(TemplateTag.class);
		templateTag.setConstructId(htmlConstructId);
		templateTag.setEnabled(true);
		templateTag.setName(TEMPLATE_TAG_NAME);
		templateTag.setPublic(true);
		template.getTemplateTags().put(TEMPLATE_TAG_NAME, templateTag);

		TemplateTag targetTemplateTag = t.createObject(TemplateTag.class);
		targetTemplateTag.setConstructId(htmlConstructId);
		targetTemplateTag.setEnabled(true);
		targetTemplateTag.setName(TARGET_TEMPLATETAG_NAME);
		targetTemplateTag.setPublic(false);
		targetTemplateTag.getValues().iterator().next().setValueText("This is the TemplateTag");
		template.getTemplateTags().put(TARGET_TEMPLATETAG_NAME, targetTemplateTag);

		template.save();
		t.commit(false);

		// create target objects
		targetPage = t.createObject(Page.class);
		targetPage.setTemplateId(template.getId());
		targetPage.setFolderId(node.getFolder().getId());
		targetPage.getTag(TEMPLATE_TAG_NAME).getValues().iterator().next().setValueText("Target page content");
		targetPage.save();
		targetPage.publish();
		t.commit(false);
		targetPage = t.getObject(Page.class, targetPage.getId());

		targetImage = t.createObject(ImageFile.class);
		targetImage.setFolderId(node.getFolder().getId());
		targetImage.setName("blume.jpg");
		targetImage.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		targetImage.save();
		t.commit(false);
		targetImage = t.getObject(ImageFile.class, targetImage.getId());

		targetFile = t.createObject(File.class);
		targetFile.setFolderId(node.getFolder().getId());
		targetFile.setName("file.doc");
		targetFile.setFileStream(GenericTestUtils.getFileResource("file.doc"));
		targetFile.save();
		t.commit(false);
		targetFile = t.getObject(File.class, targetFile.getId());

		datasource = ContentNodeTestDataUtils.createDatasource("Test Datasource", Arrays.asList("One", "Two", "Three", "Four"));
	}

	/**
	 * Create a new contenttag with given name, constructId and filled with valueText and add to the page
	 * @param page page
	 * @param constructId construct ID
	 * @param name tag name
	 * @param valueText tag content
	 * @throws Exception
	 */
	protected static void addTag(Page page, int constructId, String name, String valueText) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		ContentTag cellA1 = t.createObject(ContentTag.class);
		cellA1.setConstructId(constructId);
		cellA1.setEnabled(true);
		cellA1.setName(name);
		cellA1.getValues().iterator().next().setValueText(valueText);
		page.getContentTags().put(cellA1.getName(), cellA1);
	}

	/**
	 * Create a test instance
	 * @param test what to test
	 */
	public PublishablePageTest(PartTypeTest test) {
		this.test = test;
	}

	/**
	 * Setup the test data
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (test.wrappedClazz != null) {
			constructId = ContentNodeTestDataUtils.createExtensibleConstruct(node, test.wrappedClazz, CONSTRUCT_KEYWORD, PART_KEYWORD);
		} else {
			constructId = ContentNodeTestDataUtils.createConstruct(node, test.clazz, CONSTRUCT_KEYWORD, PART_KEYWORD);
		}

		// for selectparttypes, set the datasource id
		if (SelectPartType.class.isAssignableFrom(test.clazz)) {
			Construct construct = t.getObject(Construct.class, constructId, true);
			construct.getParts().get(0).setInfoInt(ObjectTransformer.getInt(datasource.getId(), 0));
			construct.save();
			t.commit(false);
		}

		page = t.createObject(Page.class);
		page.setTemplateId(template.getId());
		page.setFolderId(node.getFolder().getId());
		ContentTag contentTag = page.getContent().addContentTag(constructId);
		page.getContentTag(TEMPLATE_TAG_NAME).getValues().getByKeyname("text").setValueText("<node " + contentTag.getName() + ">");

		test.fillContentTag(page, contentTag);

		page.save();
		page.publish();
		t.commit(false);
		page = t.getObject(Page.class, page.getId());
	}

	/**
	 * Test rendering the page
	 * @throws Exception
	 */
	@Test
	public void testRender() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		try {
			t.preparePublishData();
			PublishablePage pPage = PublishablePage.getInstance(ObjectTransformer.getInt(page.getId(), 0));
			assertNotNull("PublishablePage instance must not be null", pPage);

			// rendered page must not be empty
			String content = pPage.render(new RenderResult());

			assertFalse("Rendered page must not be empty", ObjectTransformer.isEmpty(content));
			assertEquals("Check rendered page", page.render(new RenderResult()), content);
		} finally {
			t.resetPublishData();
		}
	}

	/**
	 * Abstract test class for encapsulating test information and code
	 */
	protected static abstract class PartTypeTest {
		/**
		 * Parttype class that is tested
		 */
		protected Class<? extends PartType> clazz;

		/**
		 * Wrapped Parttype class, if {@link #clazz} is {@link ExtensiblePartTypeWrapper}
		 */
		protected Class<? extends ExtensiblePartType> wrappedClazz;

		/**
		 * Description to be shown in the test overview
		 */
		protected String description;

		/**
		 * Create an instance for testing the clazz with a description
		 * @param clazz tested class
		 * @param description description
		 */
		protected PartTypeTest(Class<? extends PartType> clazz, String description) {
			this(clazz, null, description);
		}

		/**
		 * Create an instance for testing the clazz with a description
		 * @param clazz tested class
		 * @param wrappedClazz wrapped class
		 * @param description description
		 */
		protected PartTypeTest(Class<? extends PartType> clazz, Class<? extends ExtensiblePartType> wrappedClazz, String description) {
			this.clazz = clazz;
			this.wrappedClazz = wrappedClazz;
			this.description = description;
		}

		/**
		 * Fill the content tag
		 * @param page page
		 * @param tag tag
		 * @throws Exception
		 */
		protected abstract void fillContentTag(Page page, ContentTag tag) throws Exception;

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			if (wrappedClazz != null) {
				str.append(wrappedClazz.getSimpleName());
			} else {
				str.append(clazz.getSimpleName());
			}
			if (!ObjectTransformer.isEmpty(description)) {
				str.append(" - ").append(description);
			}
			return str.toString();
		}
	}
}
