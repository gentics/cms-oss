package com.gentics.contentnode.tests.overview;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.localize;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for rendering page overviews
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class PageOverviewRenderTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);

	private static Node node;

	private static Node channel;

	private static Template template;

	private static Construct hbsConstruct;

	private static Integer overviewPartTypeId;

	private static Folder folder;

	private static int nodeId;

	private static int channelId;

	private static List<Page> allPages = new ArrayList<>();

	@Parameters(name = "{index}: sticky: {0}, from master: {1}, dsfallback: {2}, publish_cache: {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Boolean stickyChannel : Arrays.asList(true, false)) {
			for (Boolean fromMaster : Arrays.asList(true, false)) {
				if (!fromMaster && !stickyChannel) {
					continue;
				}
				for (Boolean dsFallback : Arrays.asList(true, false)) {
					for (Boolean publishCache : Arrays.asList(true, false)) {
						data.add(new Object[] {stickyChannel, fromMaster, dsFallback, publishCache});
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> ContentNodeTestDataUtils.createNode("node", "node", PublishTarget.NONE,
				getLanguage("de"), getLanguage("en")));
		nodeId = execute(Node::getId, node);

		channel = supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));
		channelId = execute(Node::getId, channel);

		// create handlebars construct
		hbsConstruct = supply(t -> t.getObject(Construct.class, createConstruct(node, HandlebarsPartType.class, "hbs", "hbs")));

		overviewPartTypeId = supply(() -> getPartTypeId(OverviewPartType.class));

		hbsConstruct = update(hbsConstruct, c -> {
			Part dsPart = create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname("ds");
				p.setName("ds", 1);
				p.setPartTypeId(overviewPartTypeId);
			}).doNotSave().build();
			c.getParts().add(dsPart);
		}).build();

		template = create(Template.class, t -> {
			t.setName("Template");
			t.addFolder(node.getFolder());
			t.setSource("<node overview>");
			TemplateTag tag = create(TemplateTag.class, tt -> {
				tt.setConstructId(hbsConstruct.getId());
				tt.setName("overview");
				tt.setEnabled(true);
				tt.setPublic(true);
				tt.setMandatory(false);
			}).doNotSave().build();
			t.getTemplateTags().put("overview", tag);
		}).build();

		folder = create(Folder.class, f -> {
			f.setName("Test Folder");
			f.setMotherId(node.getFolder().getId());
		}).build();

		// create pages in master to be inherited
		allPages.add(create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("German Inherited");
			p.setLanguage(getLanguage("de"));
		}).publish().build());

		allPages.add(create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("English Inherited");
			p.setLanguage(getLanguage("en"));
		}).publish().build());

		Page germanInherited = create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("German with English Inherited");
			p.setLanguage(getLanguage("de"));
		}).publish().build();
		allPages.add(germanInherited);

		allPages.add(create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("English with German Inherited");
			p.setLanguage(getLanguage("en"));
			p.setContentsetId(germanInherited.getContentsetId());
		}).publish().build());

		// create pages in master to be localized
		Page germanInMaster = create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("German In Master");
			p.setLanguage(getLanguage("de"));
		}).publish().build();
		allPages.add(germanInMaster);

		Page englishInMaster = create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("English In Master");
			p.setLanguage(getLanguage("en"));
		}).publish().build();
		allPages.add(englishInMaster);

		Page germanWithEnglishInMaster = create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("German with English In Master");
			p.setLanguage(getLanguage("de"));
		}).publish().build();
		allPages.add(germanWithEnglishInMaster);

		Page englishWithGermanInMaster = create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("English with German In Master");
			p.setLanguage(getLanguage("en"));
			p.setContentsetId(germanWithEnglishInMaster.getContentsetId());
		}).publish().build();
		allPages.add(englishWithGermanInMaster);

		// localize pages in channel
		allPages.add(localize(germanInMaster, channel, p -> {
			p.setName("German Localized");
		}).publish().build());

		allPages.add(localize(englishInMaster, channel, p -> {
			p.setName("English Localized");
		}).publish().build());

		allPages.add(localize(germanWithEnglishInMaster, channel, p -> {
			p.setName("German with English Localized");
		}).publish().build());

		allPages.add(localize(englishWithGermanInMaster, channel, p -> {
			p.setName("English with German Localized");
		}).publish().build());

		// create local pages in channel
		allPages.add(create(Page.class, p -> {
			p.setFolder(channel, folder);
			p.setChannelInfo(channel.getId(), null);
			p.setTemplateId(template.getId());
			p.setName("German Local");
			p.setLanguage(getLanguage("de"));
		}).publish().build());

		allPages.add(create(Page.class, p -> {
			p.setFolder(channel, folder);
			p.setChannelInfo(channel.getId(), null);
			p.setTemplateId(template.getId());
			p.setName("English Local");
			p.setLanguage(getLanguage("en"));
		}).publish().build());

		Page germanLocal = create(Page.class, p -> {
			p.setFolder(channel, folder);
			p.setChannelInfo(channel.getId(), null);
			p.setTemplateId(template.getId());
			p.setName("German with English Local");
			p.setLanguage(getLanguage("de"));
		}).publish().build();
		allPages.add(germanLocal);

		allPages.add(create(Page.class, p -> {
			p.setFolder(channel, folder);
			p.setChannelInfo(channel.getId(), null);
			p.setTemplateId(template.getId());
			p.setName("English with German Local");
			p.setLanguage(getLanguage("en"));
			p.setContentsetId(germanLocal.getContentsetId());
		}).publish().build());
	}

	/**
	 * Flag for using sticky channel
	 */
	@Parameter(0)
	public boolean stickyChannel;

	/**
	 * Flag for picking overview entries from master or channel (when {@link #stickyChannel} is true)
	 */
	@Parameter(1)
	public boolean fromMaster;

	/**
	 * Flag to enable {@link Feature#DSFALLBACK}
	 */
	@Parameter(2)
	public boolean dsFallback;

	/**
	 * Flag to enable {@link Feature#PUBLISH_CACHE}
	 */
	@Parameter(3)
	public boolean publishCache;

	/**
	 * Enable/disable the sticky channel flag on the overview tagtype
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		hbsConstruct = update(hbsConstruct, c -> {
			Optional<Part> optionalPart = c.getParts().stream().filter(p -> p.getPartTypeId() == overviewPartTypeId)
					.findFirst();
			if (optionalPart.isPresent()) {
				Part p = optionalPart.get();
				OverviewPartSetting setting = new OverviewPartSetting(p);
				setting.setStickyChannel(stickyChannel);
				setting.setTo(p);
			}
		}).build();
	}

	/**
	 * Test rendering an overview in german in the master
	 * @throws NodeException
	 */
	@Test
	public void testRenderDeInMaster() throws NodeException {
		doRenderTest(node, "de");
	}

	/**
	 * Test rendering an overview in english in the master
	 * @throws NodeException
	 */
	@Test
	public void testRenderEnInMaster() throws NodeException {
		doRenderTest(node, "en");
	}

	/**
	 * Test rendering an overview in german in the channel
	 * @throws NodeException
	 */
	@Test
	public void testRenderDeInChannel() throws NodeException {
		doRenderTest(channel, "de");
	}

	/**
	 * Test rendering an overview in english in the channel
	 * @throws NodeException
	 */
	@Test
	public void testRenderEnInChannel() throws NodeException {
		doRenderTest(channel, "en");
	}

	/**
	 * Execute the render test
	 * @param n node in which the overview is rendered
	 * @param language language of the overview page
	 * @throws NodeException
	 */
	protected void doRenderTest(Node n, String language) throws NodeException {
		// set the required features
		try (FeatureClosure f1 = new FeatureClosure(Feature.DSFALLBACK, dsFallback);
				FeatureClosure f2 = new FeatureClosure(Feature.PUBLISH_CACHE, publishCache)) {

			// we will collect the expected output in lines here
			List<String> expectedOutputLines = new ArrayList<>();

			// create the overview page
			Page overviewPage = create(Page.class, p -> {
				p.setFolder(node, node.getFolder());
				p.setTemplateId(template.getId());
				p.setLanguage(getLanguage(language));
				ContentTag overviewTag = p.getContentTag("overview");
				OverviewPartType overviewPartType = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, overviewTag, "ds");
				Overview overview = overviewPartType.getOverview();

				// list pages, which are selected manually and ordered by name
				overview.setObjectClass(Page.class);
				overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
				overview.setOrderKind(Overview.ORDER_NAME);
				overview.setOrderWay(Overview.ORDERWAY_ASC);
				overview.setMaxObjects(0);
				overview.setRecursion(false);

				List<OverviewEntry> entries = overview.getOverviewEntries();

				// collect the listed pages to not render the same page twice
				Set<Page> listedPages = new HashSet<>();

				// the rendered node depends on "sticky channel"
				Node renderedNode = stickyChannel ? (fromMaster ? node : channel) : n;

				// add all test pages to the overview
				for (Page page : allPages) {
					entries.add(create(OverviewEntry.class, entry -> {
						entry.setObjectId(page.getId());
						// stick to the channel
						if (stickyChannel) {
							if (fromMaster) {
								entry.setNodeId(nodeId);
							} else {
								entry.setNodeId(channelId);
							}
						}

						// get the page, which will actually be rendered
						Page rendered = getPageInOverview(page, renderedNode, language);
						// do not render the same page twice
						if (listedPages.contains(rendered)) {
							rendered = null;
						}
						if (rendered != null) {
							listedPages.add(rendered);
							// render the line for the page
							expectedOutputLines.add("%s,%s\n".formatted(rendered.getName(), I18NHelper.getName(renderedNode)));
						}
						entry.setObjectOrder(entries.size() + 1);
					}).doNotSave().build());
				}

				// set the template to be rendered
				overviewTag.getValues().getByKeyname("hbs").setValueText("{{#each cms.tag.parts.ds.items}}{{name}},{{folder.node.folder.name}}\n{{/each}}");
			}).build();

			// sort the expected output lines and combine to the expected output
			Collections.sort(expectedOutputLines);
			String expectedOutput = expectedOutputLines.stream().collect(Collectors.joining());

			Trx.operate(() -> {
				// render publish mode
				try (RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
					TransactionManager.getCurrentTransaction().preparePublishData();
					try (ChannelTrx cTrx = new ChannelTrx(n)) {
						String renderedPage = overviewPage.render();
						assertThat(renderedPage).as("Rendered overview").isEqualTo(expectedOutput);
					}
				}
			});
		}
	}

	/**
	 * Based on the page selected for the overview, the node/channel and the
	 * language of the overview, get the actually rendered page (may be null, if the
	 * page will be omitted in the overview)
	 * 
	 * @param page selected page
	 * @param n node/channel
	 * @param language language of the overview
	 * @return actually rendered page (may be null)
	 * @throws NodeException
	 */
	protected Page getPageInOverview(Page page, Node n, String language) throws NodeException {
		// compare the page language with the overview language
		boolean matchingLanguage = StringUtils.equals(page.getLanguage().getCode(), language);

		// do multichannelling fallback
		Page variant = MultichannellingFactory.getChannelVariant(page, n);

		// if the page is not visible in the node, omit it
		if (variant == null) {
			return null;
		}

		if (matchingLanguage) {
			// page has matching language, so just return it
			return variant;
		} else {
			// page has not matching language, so get the language variant in the overview language
			Page matchingVariant = variant.getLanguageVariant(language, n.getId());
			if (matchingVariant == null) {
				if (dsFallback) {
					// page does not have a variant in the overview language, but "dsfallback" is enabled, so the page is returned
					return variant;
				} else {
					// page does not have a variant in the overview language, so it will be omitted
					return null;
				}
			} else {
				// return the matching language variant
				return matchingVariant;
			}
		}
	}
}
