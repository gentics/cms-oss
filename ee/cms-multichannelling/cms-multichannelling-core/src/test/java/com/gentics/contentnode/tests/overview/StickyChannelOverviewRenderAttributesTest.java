package com.gentics.contentnode.tests.overview;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.etc.StringUtils;

@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.PUBLISH_CACHE })
@RunWith(value = Parameterized.class)
public class StickyChannelOverviewRenderAttributesTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);
	private static ContentLanguage klingon;
	private static Node node;

	@Parameters(name = "{index}: template: {0}, sticky channel: {1}, publish cache: {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String template : Arrays.asList("page_props.hbs", "tags.hbs")) {
			for (Boolean stickyChannel : Arrays.asList(false, true)) {
				for (Boolean publishCache : Arrays.asList(false, true)) {
					data.add(new Object[] { template, stickyChannel, publishCache });
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setup() throws NodeException {
		klingon = Trx.supply(() -> Creator.createLanguage("Klingon", "kl"));
		node = Trx.supply(() -> ContentNodeTestDataUtils.update(ContentNodeTestDataUtils.createNode(), node -> {
			node.getLanguages().add(klingon);
		}));
	}

	@Parameter(0)
	public String template;

	@Parameter(1)
	public boolean stickyChannel;

	@Parameter(2)
	public boolean publishCache;

	@Test
	public void testRender() throws Exception {
		final Integer htmlConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, "html", "html"));
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
					setting.setStickyChannel(false);
					setting.setTo(p);
				}, false);
				c.getParts().add(part);
			});
		}, constructId);

		// create test template
		Template testTemplate = Trx.supply(() -> ContentNodeTestDataUtils.create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName("Testtemplate");
			tmpl.setSource("<node overview>");
			TemplateTag tTag = ContentNodeTestDataUtils.createTemplateTag(constructId, "overview", true, false);
			tmpl.getTemplateTags().put(tTag.getName(), tTag);
		}));

		// create page for overview
		Page page = Trx.supply(() -> ContentNodeTestDataUtils.update(ContentNodeTestDataUtils.create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(testTemplate.getId());
			p.setLanguage(klingon);

			p.setDescription("Description of the page");

			ContentTag cTag = p.getContent().addContentTag(htmlConstructId);
			cTag.getValues().getByKeyname("html").setValueText("Content Tag Value");
		}), p -> {
			p.publish();
		}));

		// create test page
		Page testPage = Trx.supply(() -> ContentNodeTestDataUtils.create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(testTemplate.getId());

			// prepare overview
			ContentTag cTag = p.getContentTag("overview");
			OverviewPartType overviewPartType = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, cTag, "ds");
			Overview overview = overviewPartType.getOverview();
			overview.setObjectClass(Page.class);
			overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
			overview.setMaxObjects(0);
			overview.setOrderKind(Overview.ORDER_SELECT);
			overview.setOrderWay(Overview.ORDERWAY_ASC);
			overview.setRecursion(false);

			List<OverviewEntry> entries = overview.getOverviewEntries();
			entries.add(ContentNodeTestDataUtils.create(OverviewEntry.class, e -> {
				e.setObjectId(page.getId());
				e.setNodeId(node.getId());
				e.setObjectOrder(1);
			}, false));

			// prepare vtl
			try {
				cTag.getValues().getByKeyname("hbs")
						.setValueText(StringUtils.readStream(getClass().getResourceAsStream(template)));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}));

		setStickyChannel(constructId, false);
		String referenceOutput = Trx.supply(() -> {
			try (FeatureClosure f = new FeatureClosure(Feature.PUBLISH_CACHE, false); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
				TransactionManager.getCurrentTransaction().preparePublishData();
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					return testPage.render();
				}
			}
		});

		setStickyChannel(constructId, stickyChannel);
		Trx.operate(() -> {
			try (FeatureClosure f = new FeatureClosure(Feature.PUBLISH_CACHE, publishCache); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
				TransactionManager.getCurrentTransaction().preparePublishData();
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					assertEquals("Check rendered output. publish cache: " + publishCache + ", stickyChannel: " + stickyChannel, referenceOutput,
							testPage.render());
				}
			}
		});
	}

	/**
	 * Set the sticky channel flag
	 * @param constructId construct ID
	 * @param stickyChannel true for sticky channel
	 * @throws NodeException
	 */
	protected void setStickyChannel(Integer constructId, boolean stickyChannel) throws NodeException {
		Trx.consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentNodeTestDataUtils.update(t.getObject(Construct.class, id), c -> {
				int overviewTypeId = ContentNodeTestDataUtils.getPartTypeId(OverviewPartType.class);
				Part part = c.getParts().stream().filter(p -> p.getPartTypeId() == overviewTypeId).findFirst()
						.orElseThrow(() -> new NodeException("Could not find overview part"));

				OverviewPartSetting setting = new OverviewPartSetting(part);
				setting.setStickyChannel(stickyChannel);
				setting.setTo(part);
			});
		}, constructId);

	}
}
