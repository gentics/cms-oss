package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for reloading objects in a gtx_channel Helper
 */
@RunWith(Parameterized.class)
@GCNFeature(set = {Feature.MULTICHANNELLING})
public class ChannelHelperReloadResolvingTest {
	public final static String HBS_PART_NAME = "hbs";

	public final static String TAG_NAME = "hbs_tag";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Parameters(name = "{index}: load {0}, {1} resolve {2} channel {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		data.add(new Object[] {"node", "cms.node.id", "id", false, "node.id"});
		data.add(new Object[] {"node", "cms.node.id", "id", true, "channel.id"});
		data.add(new Object[] {"node", "cms.node.id", "host", false, "node.host"});
		data.add(new Object[] {"node", "cms.node.id", "host", true, "channel.host"});
		data.add(new Object[] {"page", "cms.page.id", "id", false, "masterPage.id"});
		data.add(new Object[] {"page", "cms.page.id", "id", true, "channelPage.id"});
		data.add(new Object[] {"page", "cms.page.id", "name", false, "masterPage.name"});
		data.add(new Object[] {"page", "cms.page.id", "name", true, "channelPage.name"});
		data.add(new Object[] {"folder", "cms.folder.id", "id", false, "masterFolder.id"});
		data.add(new Object[] {"folder", "cms.folder.id", "id", true, "channelFolder.id"});
		data.add(new Object[] {"folder", "cms.folder.id", "name", false, "masterFolder.name"});
		data.add(new Object[] {"folder", "cms.folder.id", "name", true, "channelFolder.name"});

		return data;
	}

	private static Node node;
	private static Node channel;
	private static Folder masterFolder;
	private static Folder channelFolder;
	private static Template template;
	private static Page masterPage;
	private static Page channelPage;
	private static Construct hbsConstruct;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("master", "Master Node", PublishTarget.NONE));
		channel = supply(() -> createChannel(node, "Channel Node", "channel", "/"));

		masterFolder = create(Folder.class, f -> {
			f.setName("Master Folder");
			f.setMotherId(node.getFolder().getId());
			f.setPublishDir("/folder");
		}).build();

		channelFolder = supply(() -> update(localize(masterFolder, channel), f -> {
			f.setName("Channel Folder");
		}).build());

		hbsConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("hbs");
			c.setName("Handlebars", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setKeyname(HBS_PART_NAME);
				p.setEditable(1);
				p.setHidden(false);
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
			}).doNotSave().build());
		}).build();

		template = create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setName("Template");

			TemplateTag tag = create(TemplateTag.class, tTag -> {
				tTag.setName(TAG_NAME);
				tTag.setConstructId(hbsConstruct.getId());
				tTag.setEnabled(true);
				tTag.setPublic(true);
			}).doNotSave().build();
			t.getTags().put(tag.getName(), tag);

			t.setSource("<node %s>".formatted(TAG_NAME));
		}).unlock().build();

		masterPage = create(Page.class, p -> {
			p.setFolderId(masterFolder.getId());
			p.setTemplateId(template.getId());
			p.setName("Master Page");
		}).unlock().build();

		channelPage = supply(() -> update(localize(masterPage, channel), p -> {
			p.setName("Channel Page");
		}).unlock().build());
	}

	@Parameter(0)
	public String type;

	@Parameter(1)
	public String id;

	@Parameter(2)
	public String resolve;

	@Parameter(3)
	public boolean inChannel;

	@Parameter(4)
	public String expect;

	protected String expectedRenderedPage;

	@Before
	public void setup() throws NodeException {
		masterPage = update(masterPage, p -> {
			getPartType(HandlebarsPartType.class, p.getContentTag(TAG_NAME), HBS_PART_NAME).setText(
					"{{#gtx_channel %d}}{{#with (gtx_load \"%s\" %s) as | loaded | }}{{ loaded.%s }}{{/with}}{{/gtx_channel}}"
							.formatted(inChannel ? channel.getId() : node.getId(), type, id, resolve));
		}).unlock().build();

		PropertyResolver resolver = new PropertyResolver(new Resolvable() {
			@Override
			public Object getProperty(String key) {
				return get(key);
			}

			@Override
			public Object get(String key) {
				switch (key) {
				case "node":
					return node;
				case "channel":
					return channel;
				case "masterFolder":
					return masterFolder;
				case "channelFolder":
					return channelFolder;
				case "masterPage":
					return masterPage;
				case "channelPage":
					return channelPage;
				default:
					return null;
				}
			}

			@Override
			public boolean canResolve() {
				return true;
			}
		});

		expectedRenderedPage = supply(() -> ObjectTransformer.getString(resolver.resolve(expect, false), ""));
		assertThat(expectedRenderedPage).as("Expected page content").isNotBlank();
	}

	@Test
	public void testRender() throws NodeException {
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			String renderedPage = masterPage.render();
			assertThat(renderedPage).as("Rendered Page").isEqualTo(expectedRenderedPage);
			trx.success();
		}
	}
}
