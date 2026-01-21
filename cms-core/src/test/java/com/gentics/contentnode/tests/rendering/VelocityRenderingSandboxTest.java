package com.gentics.contentnode.tests.rendering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
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
		"#if(true)before[#gtx_edit($cms.page.tags.vtltag.parts.text)]middle[#gtx_render($cms.page.tags.vtltag.parts.text)]after#end",
		"#if(true)before[#gtx_render($cms.page.tags.vtltag.parts.text)]middle[#gtx_edit($cms.page.tags.vtltag.parts.text)]after#end",
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
	 * Create a test instance
	 * @param editMode edit mode
	 * @param templateIndex template index
	 */
	public VelocityRenderingSandboxTest(String editMode, int templateIndex) throws Exception {
		this.editMode = RenderType.parseEditMode(editMode);
		this.templateRendersInEditMode = TEMPLATES[templateIndex].contains("gtx_edit");
		this.editAfterRender = TEMPLATES[templateIndex].indexOf("gtx_edit") > TEMPLATES[templateIndex].indexOf("gtx_render");
		assertTrue("Given edit mode is unknown", editMode.equals(RenderType.renderEditMode(this.editMode)));
		updateConstruct(TEMPLATES[templateIndex]);
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
		String constructName = page.getContentTag(VTL_TAGNAME).getConstruct().getName().toString();
		String valueId = ObjectTransformer.getString(page.getContentTag(VTL_TAGNAME).getValues().getByKeyname("text").getId(), null);
		String expected = null;
		switch (editMode) {
		case RenderType.EM_ALOHA:
			String editablePart = null;
			if (templateRendersInEditMode) {
				editablePart = "<div id=\"GENTICS_EDITABLE_" + valueId + "\">This is the test content</div>";
			} else {
				editablePart = "This is the test content";
			}
			expected = "<div data-gcn-pageid=\"" + pageId + "\" data-gcn-tagid=\"" + tagId + "\" data-gcn-tagname=\"" + VTL_TAGNAME
					+ "\" data-gcn-i18n-constructname=\"" + constructName
					+ "\" class=\"aloha-block\" id=\"GENTICS_BLOCK_" + tagId;
			if (templateRendersInEditMode && editAfterRender) {
				expected = expected + "\">before[This is the test content]middle[" + editablePart + "]after</div>";
			} else {
				expected = expected + "\">before[" + editablePart + "]middle[This is the test content]after</div>";
			}
			break;
		default:
			expected = "before[This is the test content]middle[This is the test content]after";
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
