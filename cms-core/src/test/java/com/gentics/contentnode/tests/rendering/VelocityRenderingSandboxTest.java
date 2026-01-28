package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;

/**
 * Test cases for rendering velocity parts
 */
@RunWith(value = Parameterized.class)
public class VelocityRenderingSandboxTest extends AbstractVelocityRenderingTest {

	/**
	 * Tested templates
	 */
	public final static String[] TEMPLATES = {
		"before[#gtx_render(\"text\")]middle[#gtx_render(\"text\")]after",
		"before[#gtx_render($cms.tag.parts.text)]middle[#gtx_render($cms.tag.parts.text)]after",
		"before[#gtx_edit(\"text\")]middle[#gtx_render(\"text\")]after",
		"before[#gtx_render(\"text\")]middle[#gtx_edit(\"text\")]after",
		"before[#gtx_edit($cms.tag.parts.text)]middle[#gtx_render($cms.tag.parts.text)]after",
		"before[#gtx_render($cms.tag.parts.text)]middle[#gtx_edit($cms.tag.parts.text)]after",
		"before[#gtx_edit($cms.page.tags.vtltag.parts.text)]middle[#gtx_render($cms.page.tags.vtltag.parts.text)]after",
		"before[#gtx_render($cms.page.tags.vtltag.parts.text)]middle[#gtx_edit($cms.page.tags.vtltag.parts.text)]after",
		"before[#gtx_edit($cms.page.tags.url_construct1)]middle[#gtx_render($cms.page.tags.url_construct1)]after",
		"before[#gtx_render($cms.page.tags.url_construct1)]middle[#gtx_edit($cms.page.tags.url_construct1)]after",
	};

	/**
	 * Tested render modes
	 */
	public final static String[] EDIT_MODE = {
		"publish",
		"preview",
		"aloha_readonly",
		"aloha"
	};

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: rendertype: {0}, template: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (int t = 0; t < TEMPLATES.length; t++) {
			for (int e = 0; e < EDIT_MODE.length; e++) {
				data.add(new Object[] {EDIT_MODE[e], t});
			}
		}

		return data;
	}

	static Construct urlConstruct;

	/**
	 * Edit mode
	 */
	protected int editMode;

	/**
	 * Flag to mark whether the template renders the part in edit mode
	 */
	protected boolean templateRendersInEditMode;

	/**
	 * Whether to expect the editable after rendered content, not before
	 */
	protected boolean editAfterRender;

	/**
	 * Whether to expect the whole tag rendering, instead of its distinct part
	 */
	protected boolean renderWholeTag;

	/**
	 * The expected tag content
	 */
	protected String tagContent;

	/**
	 * Create a test instance
	 * @param editMode edit mode
	 * @param templateIndex template index
	 */
	public VelocityRenderingSandboxTest(String editMode, int templateIndex) throws Exception {
		this.editMode = RenderType.parseEditMode(editMode);
		this.templateRendersInEditMode = TEMPLATES[templateIndex].contains("gtx_edit");
		this.editAfterRender = TEMPLATES[templateIndex].indexOf("gtx_edit") > TEMPLATES[templateIndex].indexOf("gtx_render");
		this.renderWholeTag = TEMPLATES[templateIndex].contains(".") && (TEMPLATES[templateIndex].contains(".tags.") || TEMPLATES[templateIndex].endsWith(".tag")) && !TEMPLATES[templateIndex].contains(".parts.");
		this.tagContent = TEMPLATES[templateIndex].contains("tags.url_construct") ? "/nowhere/82.html" : "This is the test content";
		assertTrue("Given edit mode is unknown", editMode.equals(RenderType.renderEditMode(this.editMode)));
		updateConstruct(TEMPLATES[templateIndex]);
	}

	@BeforeClass
	public static void setupMore() throws NodeException {
		urlConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("url_construct");
			c.setName("url_construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(PageURLPartType.class));
				p.setEditable(2);
				p.setHidden(false);
				p.setKeyname("page");
			}).doNotSave().build());
		}).build();

		page = update(page, p -> {
			ContentTag urlsTag = p.getContent().addContentTag(urlConstruct.getId());
			getPartType(PageURLPartType.class, urlsTag, "page").setTargetPage(page);
			getPartType(PageURLPartType.class, urlsTag, "page").setNode(node);
		}).unlock().build();
		page = update(page, p -> {
		}).unlock().publish().build();
	}

	@Test
	public void testRender() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// set the render type
		RenderType renderType = RenderType.getDefaultRenderType(testContext.getContext().getNodeConfig().getDefaultPreferences(), editMode, "sid", -1);
		t.setRenderType(renderType);
		// set the url factory
		renderType.setRenderUrlFactory(new StaticUrlFactory(RenderUrl.LINKWAY_AUTO, RenderUrl.LINKWAY_AUTO, ""));

		RenderResult renderResult = new RenderResult();
		String content = page.render(renderResult);

		// strip away the head, which probably was rendered for aloha editor (we don't want to test it, because it contains
		// JSON and the order of the properties might vary on different systems)
		Pattern pattern = Pattern.compile("<head>.*</head>", Pattern.DOTALL | Pattern.MULTILINE);
		content = pattern.matcher(content).replaceAll("");

		assertEquals("Check rendered content", getExpectedContent(), content);
		assertEquals("Check render result", "OK", renderResult.getReturnCode());
	}

	/**
	 * Get the expected content
	 * @return
	 * @throws Exception
	 */
	protected String getExpectedContent() throws Exception {
		// prepare data which is rendered into the editable tags
		String pageId = ObjectTransformer.getString(page.getId(), null);
		String tagId = ObjectTransformer.getString(page.getContentTag(VTL_TAGNAME).getId(), null);
		String urlTagId = ObjectTransformer.getString(page.getContentTag("url_construct1").getId(), null);
		String constructName = page.getContentTag(VTL_TAGNAME).getConstruct().getName().toString();
		String valueId = ObjectTransformer.getString(page.getContentTag(VTL_TAGNAME).getValues().getByKeyname("text").getId(), null);
		String expected = null;
		switch (editMode) {
		case RenderType.EM_ALOHA:
			String editablePart = null;
			if (templateRendersInEditMode) {
				if (renderWholeTag) {
					editablePart = "<div data-gcn-pageid=\"" + pageId + "\" data-gcn-tagid=\"" + urlTagId + "\" data-gcn-tagname=\"url_construct1"
							+ "\" data-gcn-i18n-constructname=\"url_construct"
							+ "\" class=\"aloha-block\" id=\"GENTICS_BLOCK_" + urlTagId + "\">" + this.tagContent + "</div>";
				} else {
					editablePart = "<div id=\"GENTICS_EDITABLE_" + valueId + "\">" + this.tagContent + "</div>";
				}
			} else {
				editablePart = this.tagContent;
			}
			expected = "<div data-gcn-pageid=\"" + pageId + "\" data-gcn-tagid=\"" + tagId + "\" data-gcn-tagname=\"" + VTL_TAGNAME
					+ "\" data-gcn-i18n-constructname=\"" + constructName
					+ "\" class=\"aloha-block\" id=\"GENTICS_BLOCK_" + tagId;
			if (templateRendersInEditMode && editAfterRender) {
				expected = expected + "\">before[" + this.tagContent + "]middle[" + editablePart + "]after</div>";
			} else {
				expected = expected + "\">before[" + editablePart + "]middle[" + this.tagContent + "]after</div>";
			}
			break;
		default:
			expected = "before[" + this.tagContent + "]middle[" + this.tagContent + "]after";
			break;
		}
		switch (editMode) {
		case RenderType.EM_ALOHA:
		case RenderType.EM_ALOHA_READONLY:
			expected = "<!DOCTYPE html>\n<body>\n" + expected + "</body>\n";
			break;
		}
		return expected;
	}
}
