package com.gentics.contentnode.tests.overview;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for rendering overview settings with velocity
 */
public class OverviewSettingsRenderTest {
	public final static String TAG_NAME = "tag";

	public final static String HBS_PARTNAME = "hbs";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);
	private static Node node;
	private static Template template;
	private static Page page;

	@BeforeClass
	public static void setup() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());

		final Integer constructId = supply(() -> createConstruct(node, HandlebarsPartType.class, HBS_PARTNAME, HBS_PARTNAME));

		Trx.consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			update(t.getObject(Construct.class, id), c -> {
				Part part = create(Part.class, p -> {
					p.setEditable(1);
					p.setHidden(true);
					p.setKeyname("ds");
					p.setName("ds", 1);
					p.setPartTypeId(getPartTypeId(OverviewPartType.class));

					OverviewPartSetting setting = new OverviewPartSetting(p);
					setting.setStickyChannel(false);
					setting.setTo(p);
				}, false);
				c.getParts().add(part);
			});
		}, constructId);

		template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setSource("<node " + TAG_NAME + ">");
			tmpl.setName("Template");
			tmpl.addFolder(node.getFolder());

			tmpl.getTags().put(TAG_NAME, create(TemplateTag.class, tTag -> {
				tTag.setConstructId(constructId);
				tTag.setEnabled(true);
				tTag.setName(TAG_NAME);
				tTag.setPublic(true);
			}, false));
		}));

		page = supply(() -> update(createPage(node.getFolder(), template, "Page"), upd -> {
			getPartType(HandlebarsPartType.class, upd.getTag(TAG_NAME), HBS_PARTNAME).setText(
					"{{#with cms.tag.parts.ds}}listType: {{gtx_render listType}}, selectType: {{gtx_render selectType}}, orderBy: {{gtx_render orderBy}}, orderDirection: {{gtx_render orderDirection}}, maxItems: {{gtx_render maxItems}}, recursive: {{gtx_render recursive}}{{/with}}");
		}));
	}

	@Test
	public void testRender() throws NodeException {
		page = execute(p -> update(p, upd -> {
			Overview overview = getPartType(OverviewPartType.class, upd.getTag(TAG_NAME), "ds").getOverview();
			overview.setObjectType(Page.TYPE_PAGE);
			overview.setSelectionType(Overview.SELECTIONTYPE_FOLDER);
			overview.setMaxObjects(3);
			overview.setRecursion(true);
			overview.setOrderKind(Overview.ORDER_NAME);
			overview.setOrderWay(Overview.ORDERWAY_ASC);
		}), page);

		operate(() -> {
			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
				assertThat(page.render()).as("Rendered overview").isEqualTo(
						"listType: PAGE, selectType: FOLDER, orderBy: ALPHABETICALLY, orderDirection: ASC, maxItems: 3, recursive: true");
			}
		});
	}
}
