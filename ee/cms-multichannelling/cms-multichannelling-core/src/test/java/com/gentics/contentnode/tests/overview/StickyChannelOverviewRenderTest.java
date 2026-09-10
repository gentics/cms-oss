package com.gentics.contentnode.tests.overview;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.etc.StringUtils;

/**
 * Test cases for sticky channel overviews
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.PUBLISH_CACHE })
@RunWith(value = Parameterized.class)
public class StickyChannelOverviewRenderTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);

	/**
	 * Map of overview items
	 */
	protected static Map<MCTestedType, String> TEMPLATES = new HashMap<>();

	/**
	 * Renderer implementation for rendering the overview
	 */
	protected static BiFunction<LocalizableNodeObject<?>, Node, String> overviewRenderer = (o, n) -> {
		StringBuilder line = new StringBuilder();
		line.append(o.getName()).append(",").append(((Folder) o.getParentObject()).getName()).append(",").append(n.getFolder().getName()).append("\n");
		return line.toString();
	};

	/**
	 * Map of Handlebars Templates
	 */
	protected static Map<MCTestedType, String> HBS_TEMPLATES = new HashMap<>();

	/**
	 * Renderer implementation for rendering handlebars
	 */
	protected static BiFunction<LocalizableNodeObject<?>, Node, String> hbsRenderer = overviewRenderer;

	private static Node node;

	private static Node channel;

	private static Template template;

	static {
		TEMPLATES.put(MCTestedType.file, "<node file.name>,<node file.folder.name>,<node file.folder.node.folder.name>\n");
		TEMPLATES.put(MCTestedType.image, "<node image.name>,<node image.folder.name>,<node image.folder.node.folder.name>\n");
		TEMPLATES.put(MCTestedType.page, "<node page.name>,<node page.folder.name>,<node page.folder.node.folder.name>\n");
		TEMPLATES.put(MCTestedType.folder, "<node folder.name>,<node folder.parent.name>,<node folder.node.folder.name>\n");

		HBS_TEMPLATES.put(MCTestedType.file, "{{#each cms.tag.parts.ds.items}}{{name}},{{folder.name}},{{folder.node.folder.name}}\n{{/each}}");
		HBS_TEMPLATES.put(MCTestedType.image, "{{#each cms.tag.parts.ds.items}}{{name}},{{folder.name}},{{folder.node.folder.name}}\n{{/each}}");
		HBS_TEMPLATES.put(MCTestedType.page, "{{#each cms.tag.parts.ds.items}}{{name}},{{folder.name}},{{folder.node.folder.name}}\n{{/each}}");
		HBS_TEMPLATES.put(MCTestedType.folder, "{{#each cms.tag.parts.ds.items}}{{name}},{{parent.name}},{{node.folder.name}}\n{{/each}}");
	}

	@Parameters(name = "{index}: sticky: {0}, type: {1}, sort: {2}, ascending: {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean stickyChannel : Arrays.asList(true, false)) {
			for (MCTestedType type : MCTestedType.values()) {
				for (int sort : Arrays.asList(Overview.ORDER_NAME, Overview.ORDER_SELECT)) {
					for (boolean ascending : Arrays.asList(true, false)) {
						data.add(new Object[] { stickyChannel, type, sort, ascending });
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setup() throws NodeException {
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));
		template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template"));
	}

	@Parameter(0)
	public boolean stickyChannel;

	@Parameter(1)
	public MCTestedType type;

	@Parameter(2)
	public int sort;

	@Parameter(3)
	public boolean ascending;

	/**
	 * Name prefix used to make names unique between tests
	 */
	protected String namePrefix;

	/**
	 * Inherited object
	 */
	protected LocalizableNodeObject<?> inherited;

	/**
	 * Localized master object
	 */
	protected LocalizableNodeObject<?> localizedMaster;

	/**
	 * Localized channel object
	 */
	protected LocalizableNodeObject<?> localizedChannel;

	/**
	 * Local channel object
	 */
	protected LocalizableNodeObject<?> local;

	@Before
	public void setupData() throws NodeException {
		namePrefix = stickyChannel + "-" + type.toString() + "-" + sort + "-" + ascending + "-";

		// create folder
		final Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), namePrefix + "Folder"));
		// localize the folder
		Trx.supply(() -> ContentNodeTestDataUtils.update(ContentNodeTestDataUtils.localize(folder, channel), f -> {
			f.setName(namePrefix + "Localized Folder");
		}));

		inherited = Trx.supply(() -> type.publish(type.create(folder, namePrefix + "Inherited", template)));
		localizedMaster = Trx.supply(() -> type.publish(type.create(folder, namePrefix + "Localized Master", template)));
		localizedChannel = Trx.execute(master -> type.publish(ContentNodeTestDataUtils.update(type.localize(master, channel), o -> {
			o.setName(namePrefix + "Localized Channel");
		})), localizedMaster);
		local = Trx.supply(() -> type.publish(type.create(folder, namePrefix + "Local", template, channel)));
	}

	/**
	 * Test normal rendering of an overview
	 * @throws NodeException
	 */
	@Test
	public void testRenderOverview() throws NodeException {
		// create overview tagtype
		final Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, OverviewPartType.class, "ds", "ds"));

		Trx.consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentNodeTestDataUtils.update(t.getObject(Construct.class, id), c -> {
				Part part = c.getParts().get(0);
				part.getDefaultValue().setValueText(TEMPLATES.get(type));
				OverviewPartSetting setting = new OverviewPartSetting(part);
				setting.setStickyChannel(stickyChannel);
				setting.setTo(part);
			});
		}, constructId);

		// create test template
		Template testTemplate = Trx.supply(() -> ContentNodeTestDataUtils.create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName(namePrefix + "Testtemplate");
			tmpl.setSource("<node overview>");
			TemplateTag tTag = ContentNodeTestDataUtils.createTemplateTag(constructId, "overview", true, false);
			tmpl.getTemplateTags().put(tTag.getName(), tTag);
		}));

		List<NameAndRenderedLine> masterLines = new ArrayList<>();
		List<NameAndRenderedLine> channelLines = new ArrayList<>();

		// create test page
		Page testPage = createTestPage(testTemplate, masterLines, channelLines, overviewRenderer);

		if (sort == Overview.ORDER_NAME) {
			Collections.sort(masterLines, (s1, s2) -> {
				return StringUtils.mysqlLikeCompare(s1.name, s2.name);
			});
			Collections.sort(channelLines, (s1, s2) -> {
				return StringUtils.mysqlLikeCompare(s1.name, s2.name);
			});
			if (!ascending) {
				Collections.reverse(masterLines);
				Collections.reverse(channelLines);
			}
		}

		Trx.operate(() -> {
			// render preview
			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					assertEquals("Check rendered page in master", masterLines.stream().map(o -> o.renderedLine).collect(Collectors.joining()), testPage.render());
				}
				try (ChannelTrx cTrx = new ChannelTrx(channel)) {
					assertEquals("Check rendered page in channel", channelLines.stream().map(o -> o.renderedLine).collect(Collectors.joining()), testPage.render());
				}
			}

			// render in publish mode
			try (RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
				TransactionManager.getCurrentTransaction().preparePublishData();
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					assertEquals("Check rendered page in master (publish mode)", masterLines.stream().map(o -> o.renderedLine).collect(Collectors.joining()), testPage.render());
				}
				try (ChannelTrx cTrx = new ChannelTrx(channel)) {
					assertEquals("Check rendered page in channel (publish mode)", channelLines.stream().map(o -> o.renderedLine).collect(Collectors.joining()), testPage.render());
				}
			}
		});
	}

	/**
	 * Test rendering the overview using handlebars
	 * @throws NodeException
	 */
	@Test
	public void testRenderHandlebars() throws NodeException {
		// create handlebars construct
		final Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, HandlebarsPartType.class, "hbs", "hbs"));

		Trx.consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentNodeTestDataUtils.update(t.getObject(Construct.class, id), c -> {
				Part part = ContentNodeTestDataUtils.create(Part.class, p -> {
					p.setEditable(1);
					p.setHidden(true);
					p.setKeyname("ds");
					p.setName("ds", 1);
					p.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(OverviewPartType.class));

					OverviewPartSetting setting = new OverviewPartSetting(p);
					setting.setStickyChannel(stickyChannel);
					setting.setTo(p);
				}, false);
				c.getParts().add(part);
			});
		}, constructId);

		// create test template
		Template testTemplate = Trx.supply(() -> ContentNodeTestDataUtils.create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName(namePrefix + "Testtemplate");
			tmpl.setSource("<node overview>");
			TemplateTag tTag = ContentNodeTestDataUtils.createTemplateTag(constructId, "overview", true, false);
			tmpl.getTemplateTags().put(tTag.getName(), tTag);
		}));

		List<NameAndRenderedLine> masterLines = new ArrayList<>();
		List<NameAndRenderedLine> channelLines = new ArrayList<>();

		// create test page
		Page testPage = createTestPage(testTemplate, masterLines, channelLines, hbsRenderer);

		// set the hbs in the page
		Trx.operate(() -> {
			ContentNodeTestDataUtils.update(testPage, p -> {
				p.getContentTag("overview").getValues().getByKeyname("hbs").setValueText(HBS_TEMPLATES.get(type));
			});
		});

		if (sort == Overview.ORDER_NAME) {
			Collections.sort(masterLines, (s1, s2) -> {
				return StringUtils.mysqlLikeCompare(s1.name, s2.name);
			});
			Collections.sort(channelLines, (s1, s2) -> {
				return StringUtils.mysqlLikeCompare(s1.name, s2.name);
			});
			if (!ascending) {
				Collections.reverse(masterLines);
				Collections.reverse(channelLines);
			}
		}

		Trx.operate(() -> {
			// render preview
			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					assertEquals("Check rendered page in master", masterLines.stream().map(o -> o.renderedLine).collect(Collectors.joining()), testPage.render());
				}
				try (ChannelTrx cTrx = new ChannelTrx(channel)) {
					assertEquals("Check rendered page in channel", channelLines.stream().map(o -> o.renderedLine).collect(Collectors.joining()), testPage.render());
				}
			}

			// render publish mode
			try (RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
				TransactionManager.getCurrentTransaction().preparePublishData();
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					assertEquals("Check rendered page in master (publish mode)", masterLines.stream().map(o -> o.renderedLine).collect(Collectors.joining()), testPage.render());
				}
				try (ChannelTrx cTrx = new ChannelTrx(channel)) {
					assertEquals("Check rendered page in channel (publish mode)", channelLines.stream().map(o -> o.renderedLine).collect(Collectors.joining()), testPage.render());
				}
			}
		});
	}

	/**
	 * Create the test page, add entries to the overview and return the expected lines of the rendered overview (for master and channel)
	 * @param testTemplate test template
	 * @param masterLines list where the expected overview lines for the master are added
	 * @param channelLines list where the expected overview lines for the channel are added
	 * @param renderer renderer implementation
	 * @return test page
	 * @throws NodeException
	 */
	protected Page createTestPage(Template testTemplate, List<NameAndRenderedLine> masterLines, List<NameAndRenderedLine> channelLines,
			BiFunction<LocalizableNodeObject<?>, Node, String> renderer) throws NodeException {
		// create test page
		return Trx.supply(() -> ContentNodeTestDataUtils.create(Page.class, page -> {
			page.setFolderId(node.getFolder().getId());
			page.setTemplateId(testTemplate.getId());

			ContentTag cTag = page.getContentTag("overview");
			OverviewPartType overviewPartType = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, cTag, "ds");
			Overview overview = overviewPartType.getOverview();

			overview.setObjectClass(type.getClazz());
			// TODO variation
			overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
			overview.setMaxObjects(0);
			overview.setOrderKind(sort);
			overview.setOrderWay(ascending ? Overview.ORDERWAY_ASC : Overview.ORDERWAY_DESC);
			// TODO variation
			overview.setRecursion(false);

			List<OverviewEntry> entries = overview.getOverviewEntries();

			// pick inherited object from node
			pick(inherited, node, entries, masterLines, channelLines, renderer);

			// pick the localized object from channel
			pick(localizedChannel, channel, entries, masterLines, channelLines, renderer);

			// pick the local object from channel
			pick(local, channel, entries, masterLines, channelLines, renderer);

			// pick the localized object from node
			pick(localizedMaster, node, entries, masterLines, channelLines, renderer);

			// pick inherited object from channel (but only if sortorder is
			// "select". Otherwise we would have two identical objects in
			// the overview where the exact sort order cannot be determined)
			if (sort == Overview.ORDER_SELECT) {
				pick(inherited, channel, entries, masterLines, channelLines, renderer);
			}
		}));
	}

	/**
	 * Pick the given object for the node/folder. Add an entry to the entries and append the expected output
	 * @param object object to pick
	 * @param n node from which the object is picked
	 * @param f folder from which the object is picked
	 * @param entries entries
	 * @param masterLines list of lines of the overview
	 * @param channelLines list of lines of the overview
	 * @param renderer implementation for rendering the expected output
	 * @throws NodeException
	 */
	protected void pick(LocalizableNodeObject<?> object, Node n, List<OverviewEntry> entries, List<NameAndRenderedLine> masterLines,
			List<NameAndRenderedLine> channelLines, BiFunction<LocalizableNodeObject<?>, Node, String> renderer) throws NodeException {
		final int objectId = object.getMaster().getId();
		final int nodeId = stickyChannel ? n.getId() : 0;

		// when not sticky channel, only add every object once
		if (!stickyChannel && entries.stream().filter(e -> e.getObjectId().equals(objectId)).findFirst().isPresent()) {
			return;
		}

		entries.add(ContentNodeTestDataUtils.create(OverviewEntry.class, entry -> {
			entry.setObjectId(objectId);
			entry.setNodeId(nodeId);
			entry.setObjectOrder(entries.size() + 1);
		}, false));

		if (stickyChannel) {
			try (ChannelTrx cTrx = new ChannelTrx(n)) {
				NameAndRenderedLine line = new NameAndRenderedLine(object.getName(), renderer.apply(object, n));
				if (ascending) {
					masterLines.add(line);
					channelLines.add(line);
				} else {
					masterLines.add(0, line);
					channelLines.add(0, line);
				}
			}
		} else {
			Transaction t = TransactionManager.getCurrentTransaction();
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				LocalizableNodeObject<?> cObject = t.getObject(object);
				if (cObject != null) {
					NameAndRenderedLine line = new NameAndRenderedLine(cObject.getName(), renderer.apply(cObject, node));
					if (ascending) {
						masterLines.add(line);
					} else {
						masterLines.add(0, line);
					}
				}
			}
			try (ChannelTrx cTrx = new ChannelTrx(channel)) {
				LocalizableNodeObject<?> cObject = t.getObject(object);
				if (cObject != null) {
					NameAndRenderedLine line = new NameAndRenderedLine(cObject.getName(), renderer.apply(cObject, channel));
					if (ascending) {
						channelLines.add(line);
					} else {
						channelLines.add(0, line);
					}
				}
			}
		}
	}

	protected static class NameAndRenderedLine {
		protected String name;

		protected String renderedLine;

		protected NameAndRenderedLine(String name, String renderedLine) {
			this.name = name;
			this.renderedLine = renderedLine;
		}
	}
}
