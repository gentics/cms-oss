package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for rendering the #gtx_gis directive
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = Feature.PUB_DIR_SEGMENT)
public class GisDirectiveRenderTest extends AbstractVelocityRenderingTest {
	/**
	 * Folder
	 */
	protected static Folder folder;

	/**
	 * Image
	 */
	protected static File image;

	/**
	 * SVG Image
	 */
	protected static File svgImage;

	/**
	 * Tested templates
	 */
	public final static String[] TEMPLATES = {
		"#gtx_gis($cms.imps.loader.getImage(#IMAGEID#), {\"width\": 100})",
		"#set($image = $cms.imps.loader.getImage(#IMAGEID#))##\n#set($resize = {\"height\": 50, \"mode\": \"smart\"})##\n#set($crop = {\"x\": 10, \"y\": 15, \"width\": 30, \"height\": 35})##\n<img src='#gtx_gis($image, $resize, $crop)'>",
		"#gtx_gis($cms.imps.loader.getImage(#IMAGEID#), {\"width\": 100, \"type\": \"phpwidget\"})",
		"#set($image = $cms.imps.loader.getImage(#IMAGEID#))##\n#set($resize = {\"type\": \"phpwidget\", \"height\": 50, \"mode\": \"smart\"})##\n#set($crop = {\"x\": 10, \"y\": 15, \"width\": 30, \"height\": 35})##\n<img src='#gtx_gis($image, $resize, $crop)'>",
		"#gtx_gis($cms.imps.loader.getImage(#IMAGEID#), {\"width\": 80, \"height\": 80, \"mode\": \"force\"})",
		"#gtx_gis($cms.imps.loader.getImage(#IMAGEID#), {\"width\": 100, \"height\": 100, \"mode\": \"fpsmart\"})",
		"#gtx_gis($cms.imps.loader.getImage(#IMAGEID#), {\"width\": 100, \"height\": 1000, \"mode\": \"fpsmart\"})"
	};

	/**
	 * Prefix
	 */
	public final static String PREFIX = "gis_";

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
	@Parameters(name = "{index}: rendertype: {0}, template: {1}, pub_dir_segment: {2}, svg: {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (int t = 0; t < TEMPLATES.length; t++) {
			for (int e = 0; e < EDIT_MODE.length; e++) {
				for (boolean pubDirSegment : Arrays.asList(true, false)) {
					for (boolean svg : Arrays.asList(false, true)) {
						data.add(new Object[] { EDIT_MODE[e], t, pubDirSegment, svg });
					}
				}
			}
		}

		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		AbstractVelocityRenderingTest.setupOnce();

		Folder mother = update(ContentNodeTestDataUtils.createFolder(node.getFolder(), "Motherfolder"), f -> {
			f.setPublishDir("mother");
		});

		folder = update(ContentNodeTestDataUtils.createFolder(mother, "Folder"), f -> {
			f.setPublishDir("/bla");
		});

		image = ContentNodeTestDataUtils.createImage(folder, "blume.jpg", IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg")));
		svgImage = ContentNodeTestDataUtils.createImage(folder, "Cat_silhouette.svg", IOUtils.toByteArray(GisDirectiveRenderTest.class.getResourceAsStream("Cat_silhouette.svg")));
	}

	/**
	 * Edit mode
	 */
	protected int editMode;

	/**
	 * Expected content
	 */
	protected String expectedContent;

	/**
	 * Create a test instance
	 * @param editMode edit mode
	 * @param templateIndex template index
	 * @param pubDirSegment true for publish directory segments
	 * @param svg true for testing svg image
	 */
	public GisDirectiveRenderTest(String editMode, int templateIndex, boolean pubDirSegment, boolean svg) throws Exception {
		this.editMode = RenderType.parseEditMode(editMode);
		assertTrue("Given edit mode is unknown", editMode.equals(RenderType.renderEditMode(this.editMode)));
		updateConstruct(fillPlaceholders(TEMPLATES[templateIndex], svg ? svgImage : image));

		String fileName = String.format("%s%s_%s%s%s.txt", PREFIX, templateIndex, editMode, (editMode == "publish" && pubDirSegment) ? "_seg" : "", svg ? "_svg" : "");
		expectedContent = fillPlaceholders(IOUtils.toString(getClass().getResourceAsStream(fileName)), svg ? svgImage : image);

		node = update(node, n -> {
			n.setPubDirSegment(pubDirSegment);
		});
	}

	/**
	 * Test rendering
	 * @throws Exception
	 */
	@Test
	public void testRender() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// set the render type
		RenderType renderType = RenderType.getDefaultRenderType(testContext.getContext().getNodeConfig().getDefaultPreferences(), editMode, "sid", -1);
		t.setRenderType(renderType);
		// set the url factory
		switch (editMode) {
		case RenderType.EM_PUBLISH:
			renderType.setRenderUrlFactory(new StaticUrlFactory(RenderUrl.LINKWAY_AUTO, RenderUrl.LINKWAY_AUTO, ""));
			break;
		default:
			renderType.setRenderUrlFactory(new DynamicUrlFactory(t.getSessionId()));
			break;
		}

		RenderResult renderResult = new RenderResult();
		String content = page.render(renderResult);

		// strip away the head, which probably was rendered for aloha editor (we don't want to test it, because it contains
		// JSON and the order of the properties might vary on different systems)
		Pattern pattern = Pattern.compile("<head>.*</head>", Pattern.DOTALL | Pattern.MULTILINE);
		content = pattern.matcher(content).replaceAll("");

		assertEquals("Check rendered content", expectedContent, content);
		assertEquals("Check render result", "OK", renderResult.getReturnCode());
	}

	/**
	 * Fill the placeholders in the given string and return the result
	 * <ul>
	 * <li><i>#PAGEID#</i>: page ID</li>
	 * <li><i>#IMAGEID#</i>: image ID</li>
	 * <li><i>#TAGID#</i>: tag ID</li>
	 * </ul>
	 * @param string string containing placeholders
	 * @param testImage tested image
	 * @return string with placeholders filled
	 * @throws NodeException
	 */
	protected String fillPlaceholders(String string, File testImage) throws NodeException {
		return string.replaceAll("#PAGEID#", Integer.toString(page.getId()))
				.replaceAll("#IMAGEID#", Integer.toString(testImage.getId()))
				.replaceAll("#TAGID#", Integer.toString(page.getTag(VTL_TAGNAME).getId()))
				.replaceAll("#IMAGENAME#", testImage.getFilename()).replaceAll("#IMAGEMD5#", testImage.getMd5()).replaceAll("#NODEID#", Integer.toString(node.getId()));
	}
}
