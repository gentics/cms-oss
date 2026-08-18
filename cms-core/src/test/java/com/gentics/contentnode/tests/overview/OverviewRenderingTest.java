package com.gentics.contentnode.tests.overview;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Overview rendering tests
 */
public class OverviewRenderingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Template template;
	private static int constructId;

	private final static int createTimestamp = 1000;

	private final static int deleteTagTimestamp = 2000;

	private Page page;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());

		constructId = supply(() -> createConstruct(node, OverviewPartType.class, "overview", "ds"));

		template = create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName("Test Template");
			tmpl.setMlId(1);
			tmpl.setSource("<node tag>[<node name>]</node tag>");
		}).build();
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));

		// create folders
		for (String name : List.of("One", "Two", "Three")) {
			create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName(name);
			}).build();
		}

		// create the page with the overview
		page = createPage();
	}

	@Test
	public void testRenderCurrentVersion() throws NodeException {
		assertThat(render(-1)).as("Rendered page").isEqualTo("[One][Two][Three]");
	}

	@Test
	public void testRenderOlderVersion() throws NodeException {
		assertThat(render(createTimestamp)).as("Rendered page").isEqualTo("[One][Two][Three]");
	}

	@Test
	public void testDeleteTagAndRenderCurrentVersion() throws NodeException {
		deleteOverviewTag();
		assertThat(render(-1)).as("Rendered page").isEqualTo("[]");
	}

	@Test
	public void testDeleteTagAndRenderOlderVersion() throws NodeException {
		deleteOverviewTag();
		assertThat(render(createTimestamp)).as("Rendered page").isEqualTo("[One][Two][Three]");
	}

	/**
	 * Create a page with an overview over folders at {@link #createTimestamp}
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPage() throws NodeException {
		return create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());

			ContentTag contentTag = create(ContentTag.class, c -> {
				c.setConstructId(constructId);
				c.setEnabled(true);
				c.setName("tag");

				Overview overview = getPartType(OverviewPartType.class, c, "ds").getOverview();
				overview.setObjectType(Folder.TYPE_FOLDER);
				overview.setSelectionType(Overview.SELECTIONTYPE_PARENT);
				overview.setOrderKind(Overview.ORDER_SELECT);
				overview.setOrderWay(Overview.ORDERWAY_ASC);
			}).doNotSave().build();
			p.getContent().getContentTags().put("tag", contentTag);

		}).at(createTimestamp).build();
	}

	/**
	 * Render the page version of the given timestamp in publish mode
	 * @param versionTimestamp version timestamp (-1 for current version)
	 * @return rendered page
	 * @throws NodeException
	 */
	protected String render(int versionTimestamp) throws NodeException {
		return execute(id -> {
			Page versionedPage = TransactionManager.getCurrentTransaction().getObject(Page.class, id, versionTimestamp);
			try (RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
				return versionedPage.render();
			}
		}, page.getId());
	}

	/**
	 * Delete the overview tag in the page at {@link #deleteTagTimestamp}
	 * @throws NodeException
	 */
	protected void deleteOverviewTag() throws NodeException {
		page = update(page, p -> {
			p.getContent().getContentTags().remove("tag");
		}).at(deleteTagTimestamp).build();
	}
}
