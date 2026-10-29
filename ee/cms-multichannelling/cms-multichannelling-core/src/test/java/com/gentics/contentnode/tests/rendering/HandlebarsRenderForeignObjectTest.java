package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.LoaderHelperSource;
import com.gentics.contentnode.testutils.TestHelpersHandlebarsService;
import com.gentics.lib.etc.StringUtils;

/**
 * Test cases for rendering tags of foreign objects in handlebars
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class HandlebarsRenderForeignObjectTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	public static Pattern pattern = Pattern.compile("<head>.*</head>", Pattern.DOTALL | Pattern.MULTILINE);

	public final static String TEMPLATE_TAG_NAME = "outer_tag";

	public final static String CONTENT_TAG_NAME = "inner_tag";

	private static Node master;

	private static Node channel;

	private static Construct hbsConstruct;

	private static Template template;

	private static SystemUser systemUser;

	/**
	 * Set up static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		TestHelpersHandlebarsService.addHelper(LoaderHelperSource.class);

		master = supply(() -> createNode());
		channel = supply(() -> createChannel(master, "Channel", "channel", "/"));
		hbsConstruct = Builder.create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("handlebars");
			c.setName("Handlebars", 1);
			List<Part> parts = c.getParts();
			c.getNodes().add(master);

			// add handlebars part
			parts.add(Builder.create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
				p.setKeyname("hbs");
				p.setEditable(1);
				p.setHidden(false);
			}).doNotSave().build());

			// add another editable part
			parts.add(Builder.create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
				p.setKeyname("editable");
				p.setEditable(2);
				p.setHidden(true);
			}).doNotSave().build());
		}).save().build();

		template = Builder.create(Template.class, tmpl -> {
			tmpl.setFolderId(master.getFolder().getId());
			tmpl.setName("Template");
			tmpl.setSource("<node " + TEMPLATE_TAG_NAME + ">");
			tmpl.getTags().put(TEMPLATE_TAG_NAME, Builder.create(TemplateTag.class, tag -> {
				tag.setName(TEMPLATE_TAG_NAME);
				tag.setConstructId(hbsConstruct.getId());
				tag.setEnabled(true);
				tag.setPublic(true);
			}).doNotSave().build());
		}).build();

		systemUser = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> {
			clear(channel);
			clear(master);
		});
	}

	/**
	 * Test that if rendering a tag of the master page in a localized page, the tag (and all its contents) will not be rendered in edit mode
	 * @throws NodeException
	 * @throws IOException
	 */
	@Test
	public void testTagFromMasterPage() throws NodeException, IOException {
		String outerTagHbs = StringUtils.readStream(getClass().getResourceAsStream("nested/render_tag_from_masterpage.hbs"));
		String innerTagHbs = StringUtils.readStream(getClass().getResourceAsStream("nested/render_part_in_editmode.hbs"));
		Page masterPage = Builder.create(Page.class, p -> {
			p.setFolderId(master.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Master Page");
			getPartType(HandlebarsPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), "hbs").setText(outerTagHbs);

			p.getContentTags().put(CONTENT_TAG_NAME, Builder.create(ContentTag.class, c -> {
				c.setName(CONTENT_TAG_NAME);
				c.setConstructId(hbsConstruct.getId());
				c.setEnabled(true);
				getPartType(HandlebarsPartType.class, c, "hbs").setText(innerTagHbs);
			}).doNotSave().build());
		}).build();

		Page localizedPage = supply(() -> localize(masterPage, channel));

		// render master page
		try (Trx trx = new Trx(systemUser); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_ALOHA)) {
			String expectedRenderedMasterPage = StringUtils.readStream(getClass().getResourceAsStream("nested/rendered_masterpage.txt"))
					.replaceAll(Pattern.quote("##PAGE_ID##"), masterPage.getId().toString())
					.replaceAll(Pattern.quote("##OUTER_TAG_ID##"), masterPage.getContentTag(TEMPLATE_TAG_NAME).getId().toString())
					.replaceAll(Pattern.quote("##INNER_TAG_ID##"), masterPage.getContentTag(CONTENT_TAG_NAME).getId().toString())
					.replaceAll(Pattern.quote("##INNER_TAG_PART_ID##"), masterPage.getContentTag(CONTENT_TAG_NAME).getValues().getByKeyname("editable").getId().toString());
			String content = masterPage.render();
			content = pattern.matcher(content).replaceAll("");

			assertThat(content).as("Rendered master page").isEqualTo(expectedRenderedMasterPage);
			trx.success();
		}

		// render localized page
		try (Trx trx = new Trx(systemUser); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_ALOHA); ChannelTrx cTrx = new ChannelTrx(channel)) {
			String expectedRenderedLocalizedPage = StringUtils.readStream(getClass().getResourceAsStream("nested/rendered_localizedpage.txt"))
					.replaceAll(Pattern.quote("##PAGE_ID##"), localizedPage.getId().toString())
					.replaceAll(Pattern.quote("##OUTER_TAG_ID##"), localizedPage.getContentTag(TEMPLATE_TAG_NAME).getId().toString())
					.replaceAll(Pattern.quote("##INNER_TAG_ID##"), masterPage.getContentTag(CONTENT_TAG_NAME).getId().toString())
					.replaceAll(Pattern.quote("##INNER_TAG_PART_ID##"), masterPage.getContentTag(CONTENT_TAG_NAME).getValues().getByKeyname("editable").getId().toString());
			String content = localizedPage.render();
			content = pattern.matcher(content).replaceAll("");

			assertThat(content).as("Rendered localized page").isEqualTo(expectedRenderedLocalizedPage);
			trx.success();
		}
	}
}
