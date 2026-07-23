package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createVelocityConstruct;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.ChangeableListPartType;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.DHTMLPartType;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.HTMLTextPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.JavaEditorPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLTextPartType;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.object.parttype.OrderedListPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.object.parttype.TablePartType;
import com.gentics.contentnode.object.parttype.TemplateTagPartType;
import com.gentics.contentnode.object.parttype.UnorderedListPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.etc.StringUtils;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for using the velocity directives #gtx_render and #gtx_edit to render other tags
 */
@RunWith(value = Parameterized.class)
public class VelocityDirectivesTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Tested class
	 */
	private Class<? extends PartType> partTypeClass = null;

	/**
	 * Tested rendermode
	 */
	private int renderMode = 0;

	/**
	 * Tested template
	 */
	private String vtl = null;

	private static Node node;
	private static Template template;
	private static Page page;

	private static Template targetTemplate;

	private static TemplateTag templateTag;

	private static Page targetPage;

	private static File targetFile;

	private static ImageFile targetImage;

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		// Create node
		node = supply(() -> createNode("testnode", "Test Node", PublishTarget.NONE));

		// Create velocity construct that renders the given template
		int vtlConstructId = supply(() -> createVelocityConstruct(node, "vtl", "vtl"));

		// Create template using the construct
		template = create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setMlId(1);
			tmpl.setName("Template");
			tmpl.setSource("<node vtl>");

			TemplateTag vtlTag = create(TemplateTag.class, tt -> {
				tt.setConstructId(vtlConstructId);
				tt.setEnabled(true);
				tt.setName("vtl");
				tt.setPublic(false);
			}).doNotSave().build();

			tmpl.getTemplateTags().put(vtlTag.getName(), vtlTag);
		}).build();

		// Create a page
		page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Testpage");
		}).unlock().build();

		// create a target template
		targetTemplate = create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setMlId(1);
			tmpl.setName("Target Template");
			tmpl.setSource("<node vtl>");

			templateTag = create(TemplateTag.class, tt -> {
				tt.setConstructId(vtlConstructId);
				tt.setEnabled(true);
				tt.setName("vtl");
				tt.setPublic(true);
				tt.getValues().getByKeyname("template").setValueText("This is the target template tag");
			}).doNotSave().build();

			tmpl.getTemplateTags().put(templateTag.getName(), templateTag);
		}).build();

		// create a target page
		targetPage = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(targetTemplate.getId());
			p.setName("Target Page");
			p.getContentTag("vtl").getValues().getByKeyname("template").setValueText("This is the target content tag");
		}).unlock().publish().build();

		// create a target file
		targetFile = create(File.class, f -> {
			f.setFileStream(new ByteArrayInputStream("File contents".getBytes()));
			f.setFolderId(node.getFolder().getId());
			f.setName("targetfile.txt");
		}).build();

		// create a target image
		try (InputStream in = GenericTestUtils.getPictureResource("blume.jpg")) {
			targetImage = create(ImageFile.class, i -> {
				i.setFileStream(in);
				i.setFolderId(node.getFolder().getId());
				i.setName("blume.jpg");
			}).build();
		}
	}

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: PartType: {0}, rendermode: {1}, template: {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (Class<? extends PartType> clazz : Arrays.asList(CheckboxPartType.class, SingleSelectPartType.class, MultiSelectPartType.class,
				ChangeableListPartType.class, OrderedListPartType.class, UnorderedListPartType.class, OverviewPartType.class, PageTagPartType.class,
				TemplateTagPartType.class, HTMLPartType.class, HTMLTextPartType.class, LongHTMLPartType.class, LongHTMLTextPartType.class,
				NormalTextPartType.class, ShortTextPartType.class, FileURLPartType.class, FolderURLPartType.class, ImageURLPartType.class,
				PageURLPartType.class, JavaEditorPartType.class, DHTMLPartType.class, TablePartType.class, DatasourcePartType.class, NodePartType.class)) {
			for (int renderMode : Arrays.asList(RenderType.EM_ALOHA, RenderType.EM_ALOHA_READONLY, RenderType.EM_LIVEPREVIEW,
					RenderType.EM_PREVIEW, RenderType.EM_PUBLISH)) {
				for (String vtl : Arrays.asList("gtx_render_name.vm", "gtx_edit_name.vm", "gtx_render_ref.vm", "gtx_edit_ref.vm")) {
					data.add(new Object[] {clazz, RenderType.renderEditMode(renderMode), vtl});
				}
			}
		}

		return data;
	}

	/**
	 * Create a test instance
	 * @param partTypeClass parttype class
	 * @param renderMode render mode
	 * @param vtl vtl code to render
	 */
	public VelocityDirectivesTest(Class<? extends PartType> partTypeClass, String renderMode, String vtl) {
		this.partTypeClass = partTypeClass;
		this.renderMode = RenderType.parseEditMode(renderMode);
		this.vtl = vtl;
	}

	/**
	 * Do the test. Render the velocity template, check that the content is not empty and compare with the "old style"
	 * @throws Exception
	 */
	@Test
	public void test() throws Exception {
		try (InputStream in = getClass().getResourceAsStream(vtl)) {
			String content = StringUtils.readStream(in);
			template = update(template, tmpl -> {
				tmpl.getTemplateTag("vtl").getValues().getByKeyname("template").setValueText(content);
			}).build();
		}

		int testedConstruct = supply(() -> createConstruct(node, partTypeClass, partTypeClass.getSimpleName().toLowerCase(), "part"));

		page = update(page, p -> {
			p.setTemplateId(template.getId());
			p.getContent().getTags().clear();
			ContentTag testedTag = p.getContent().addContentTag(testedConstruct);
			testedTag.setName("testtag");
			ContentNodeTestDataUtils.fillValue(testedTag.getValues().getByKeyname("part"), "Test text", true, Arrays.asList("one", "two", "three"), targetPage.getContentTag("vtl"), templateTag,
					targetFile, node, node.getFolder(), targetImage, targetPage);
			testedTag.setEnabled(true);
		}).build();

		String content = supply(t -> {
			RenderType renderType = RenderType.getDefaultRenderType(testContext.getContext().getNodeConfig().getDefaultPreferences(), renderMode, -1);
			t.setRenderType(renderType);
			// set the url factory
			renderType.setRenderUrlFactory(new StaticUrlFactory(RenderUrl.LINKWAY_AUTO, RenderUrl.LINKWAY_AUTO, ""));

			RenderResult renderResult = new RenderResult();
			return page.render(renderResult);
		});
		assertFalse("Content must not be empty", ObjectTransformer.isEmpty(content));

		try (InputStream in = getClass().getResourceAsStream("compare_" + vtl)) {
			String vtlContent = StringUtils.readStream(in);
			template = update(template, tmpl -> {
				tmpl.getTemplateTag("vtl").getValues().getByKeyname("template").setValueText(vtlContent);
			}).build();
		}

		String oldContent = supply(t -> {
			RenderType renderType = RenderType.getDefaultRenderType(testContext.getContext().getNodeConfig().getDefaultPreferences(), renderMode, -1);
			t.setRenderType(renderType);
			// set the url factory
			renderType.setRenderUrlFactory(new StaticUrlFactory(RenderUrl.LINKWAY_AUTO, RenderUrl.LINKWAY_AUTO, ""));

			RenderResult renderResult = new RenderResult();
			return page.render(renderResult);
		});

		assertEquals("Check rendered content", oldContent, content);
	}
}
