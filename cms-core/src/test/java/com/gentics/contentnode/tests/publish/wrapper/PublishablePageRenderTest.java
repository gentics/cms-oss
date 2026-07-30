package com.gentics.contentnode.tests.publish.wrapper;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.ChannelTrxInvocationHandler;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.publish.wrapper.PublishablePage;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.util.FileUtil;

/**
 * Render tests for PublishablePages
 */
public class PublishablePageRenderTest {
	private static final String TEMPLATE_TAG_NAME = "content";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test node
	 */
	private static Node node;

	private static int hbsConstructId;

	private static Template template;

	private static ContentLanguage de;

	private static ContentLanguage en;

	/**
	 * Create the static test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		RenderType renderType = t.getRenderType();
		renderType.setRenderUrlFactory(new StaticUrlFactory(RenderUrl.LINKWAY_AUTO, RenderUrl.LINKWAY_AUTO, null));
		renderType.setFrontEnd(true);

		node = ContentNodeTestDataUtils.createNode("Test", "test", "/", null, false, false);
		hbsConstructId = createConstruct(node, HandlebarsPartType.class, "hbs", "hbs");

		de = t.createObject(ContentLanguage.class);
		de.setCode("de");
		de.setName("Deutsch");
		de.getNodes().add(node);
		de.save();
		t.commit(false);

		en = t.createObject(ContentLanguage.class);
		en.setCode("en");
		en.setName("English");
		en.getNodes().add(node);
		en.save();
		t.commit(false);

		template = t.createObject(Template.class);
		template.setMlId(1);
		template.setName("Testtemplate");
		template.setSource("<node " + TEMPLATE_TAG_NAME + ">");
		template.getFolders().add(node.getFolder());
		TemplateTag templateTag = t.createObject(TemplateTag.class);
		templateTag.setConstructId(hbsConstructId);
		templateTag.setEnabled(true);
		templateTag.setName(TEMPLATE_TAG_NAME);
		templateTag.setPublic(true);
		template.getTemplateTags().put(TEMPLATE_TAG_NAME, templateTag);
		template.save();

		t.commit(false);
	}

	/**
	 * Test rendering page properties
	 * @throws Exception
	 */
	@Test
	public void testRenderPageProperties() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("page_props.hbs")) {
			String vtl = FileUtil.stream2String(in, "UTF-8");
			assertRender(createHandlebarsPage(vtl));
		}
	}

	/**
	 * Test rendering tag properties
	 * @throws Exception
	 */
	@Test
	public void testRenderTags() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("tags.hbs")) {
			String vtl = FileUtil.stream2String(in, "UTF-8");
			assertRender(createHandlebarsPage(vtl));
		}
	}

	/**
	 * Test rendering language variants
	 * @throws Exception
	 */
	@Test
	public void testRenderLanguageVariants() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("language_variants.hbs")) {
			String vtl = FileUtil.stream2String(in, "UTF-8");
			Page page = createHandlebarsPage(vtl);
			Page englishVariant = (Page)page.copy();
			englishVariant.setContentsetId(page.getContentsetId());
			englishVariant.setLanguage(en);
			englishVariant.save();
			TransactionManager.getCurrentTransaction().commit(false);

			assertRender(page);
			assertRender(englishVariant);
		}
	}

	/**
	 * Test rendering page properties
	 * @throws Exception
	 */
	@Test
	public void testRenderPagePropertiesProxy() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("page_props.hbs")) {
			String vtl = FileUtil.stream2String(in, "UTF-8");
			assertRenderProxy(createHandlebarsPage(vtl));
		}
	}

	/**
	 * Test rendering tag properties
	 * @throws Exception
	 */
	@Test
	public void testRenderTagsProxy() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("tags.hbs")) {
			String vtl = FileUtil.stream2String(in, "UTF-8");
			assertRenderProxy(createHandlebarsPage(vtl));
		}
	}

	/**
	 * Test rendering language variants
	 * @throws Exception
	 */
	@Test
	public void testRenderLanguageVariantsProxy() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("language_variants.hbs")) {
			String vtl = FileUtil.stream2String(in, "UTF-8");
			Page page = createHandlebarsPage(vtl);
			Page englishVariant = (Page)page.copy();
			englishVariant.setContentsetId(page.getContentsetId());
			englishVariant.setLanguage(en);
			englishVariant.save();
			TransactionManager.getCurrentTransaction().commit(false);

			assertRenderProxy(page);
			assertRenderProxy(englishVariant);
		}
	}

	/**
	 * Test rendering page properties
	 * @throws Exception
	 */
	@Test
	public void testRenderPagePropertiesPublishableProxy() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("page_props.hbs")) {
			String vtl = FileUtil.stream2String(in, "UTF-8");
			assertRenderPublishablePageProxy(createHandlebarsPage(vtl));
		}
	}

	/**
	 * Test rendering tag properties
	 * @throws Exception
	 */
	@Test
	public void testRenderTagsPublishableProxy() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("tags.hbs")) {
			String vtl = FileUtil.stream2String(in, "UTF-8");
			assertRenderPublishablePageProxy(createHandlebarsPage(vtl));
		}
	}

	/**
	 * Test rendering language variants
	 * @throws Exception
	 */
	@Test
	public void testRenderLanguageVariantsPublishableProxy() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("language_variants.hbs")) {
			String vtl = FileUtil.stream2String(in, "UTF-8");
			Page page = createHandlebarsPage(vtl);
			Page englishVariant = (Page)page.copy();
			englishVariant.setContentsetId(page.getContentsetId());
			englishVariant.setLanguage(en);
			englishVariant.save();
			TransactionManager.getCurrentTransaction().commit(false);

			assertRenderPublishablePageProxy(page);
			assertRenderPublishablePageProxy(englishVariant);
		}
	}

	/**
	 * Create a page rendering the given hbs code
	 * @param hbs hbs code
	 * @return page instance
	 * @throws Exception
	 */
	protected Page createHandlebarsPage(String hbs) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setDescription("Rendering test page");
		page.setTemplateId(template.getId());
		page.setFolderId(node.getFolder().getId());
		page.setLanguage(de);
		page.getContentTag(TEMPLATE_TAG_NAME).getValues().getByKeyname("hbs").setValueText(hbs);
		page.save();
		page.publish();
		t.commit(false);

		return t.getObject(Page.class, page.getId());
	}

	/**
	 * Assert that the {@link PublishablePage} instance renders identical to the {@link PageFactory} instance.
	 * @param page page to render
	 * @throws Exception
	 */
	protected void assertRender(Page page) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		try {
			t.preparePublishData();
			PublishablePage pPage = PublishablePage.getInstance(ObjectTransformer.getInt(page.getId(), 0));
			assertNotNull("PublishablePage instance must not be null", pPage);
			
			// rendered page must not be empty
			String content = pPage.render(new RenderResult());
			
			assertEquals("Check rendered page", page.render(new RenderResult()), content);
			assertFalse("Rendered page must not be empty", ObjectTransformer.isEmpty(content));
		} finally {
			t.resetPublishData();
		}
	}

	/**
	 * Assert that the Proxy for the page renders identical to the {@link PageFactory} instance.
	 * @param page page to render
	 * @throws Exception
	 */
	protected void assertRenderProxy(Page page) throws Exception {
		Page pPage = (Page) ChannelTrxInvocationHandler.wrap(node.getId(), page);
		assertNotNull("Proxy instance must not be null", pPage);

		// rendered page must not be empty
		String content = pPage.render(new RenderResult());

		assertEquals("Check rendered page", page.render(new RenderResult()), content);
		assertFalse("Rendered page must not be empty", ObjectTransformer.isEmpty(content));
	}

	/**
	 * Assert that the Proxy for the PublishablePage renders identical to the {@link PageFactory} instance
	 * @param page page to render
	 * @throws Exception
	 */
	protected void assertRenderPublishablePageProxy(Page page) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		try {
			t.preparePublishData();
			Page pPage = (Page) ChannelTrxInvocationHandler.wrap(node.getId(), PublishablePage.getInstance(ObjectTransformer.getInt(page.getId(), 0)));
			assertNotNull("PublishablePage Proxy instance must not be null", pPage);

			// rendered page must not be empty
			String content = pPage.render(new RenderResult());

			assertEquals("Check rendered page", page.render(new RenderResult()), content);
			assertFalse("Rendered page must not be empty", ObjectTransformer.isEmpty(content));
		} finally {
			t.resetPublishData();
		}
	}
}
