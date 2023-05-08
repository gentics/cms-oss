package com.gentics.contentnode.tests.versioning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for restoring page versions
 */
public class RestoreTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Integer htmlConstructId;
	private static Integer overviewConstructId;
	private static Integer datasourceConstructId;

	/**
	 * Setup common test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		htmlConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, "html", "html"));
		overviewConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, OverviewPartType.class, "overview", "overview"));
		datasourceConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, DatasourcePartType.class, "datasource", "datasource"));
	}

	/**
	 * Before every testcase, we assert the DB consistency
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		cleanDB();
	}

	/**
	 * After every testcase, we assert the DB consistency
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		assertDBConsistency();
	}

	/**
	 * Test deleting a tag with values while restoring a version
	 * @throws NodeException
	 */
	@Test
	public void testDeleteTagWithValues() throws NodeException {
		// create page (with ts 1000)
		Page page = Trx.supply(() -> {
			TransactionManager.getCurrentTransaction().setTimestamp(1000);
			return ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Page");
		});

		// add tag to page (with ts 2000)
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setTimestamp(2000);

			Page editablePage = t.getObject(page, true);
			ContentTag tag = editablePage.getContent().addContentTag(htmlConstructId);
			tag.setEnabled(true);
			ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, tag, "html").getValueObject().setValueText("bla");
			editablePage.save();
		});

		// restore version 1000 (with ts 3000)
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setTimestamp(3000);

			PageResource res = new PageResourceImpl();
			ContentNodeRESTUtils.assertResponseOK(res.restoreVersion(page.getId().toString(), 1));
		});

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			// assert the current page
			assertThat(t.getObject(page).getContent().getTags()).as("Tags in page").isEmpty();

			// assert the page versions
			Page version1 = t.getObject(Page.class, page.getId(), 1);
			Page version2 = t.getObject(Page.class, page.getId(), 2);
			Page version3 = t.getObject(Page.class, page.getId(), 3);

			assertThat(version1.getContent().getTags()).as("Tags in version 1").isEmpty();
			assertThat(version2.getContent().getTags()).as("Tags in version 2").hasSize(1);
			Tag tag = version2.getContent().getTags().values().iterator().next();
			assertThat(tag.getValues().getByKeyname("html").getValueText()).as("Value in version 2").isEqualTo("bla");
			assertThat(version3.getContent().getTags()).as("Tags in version 3").isEmpty();
		});
	}

	/**
	 * Test deleting an overview tag while restoring a version
	 * @throws NodeException
	 */
	@Test
	public void testDeleteOverviewTag() throws NodeException {
		// create page (with ts 1000)
		Page page = Trx.supply(() -> {
			TransactionManager.getCurrentTransaction().setTimestamp(1000);
			return ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Page");
		});

		// add overview tag to page (with ts 2000)
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setTimestamp(2000);

			Page editablePage = t.getObject(page, true);
			ContentTag tag = editablePage.getContent().addContentTag(overviewConstructId);
			tag.setEnabled(true);
			ContentNodeTestDataUtils.fillOverview(tag, "overview", "bla", Folder.class, Overview.SELECTIONTYPE_SINGLE, 0, Overview.ORDER_NAME,
					Overview.ORDERWAY_ASC, false, Arrays.asList(node.getFolder()));
			editablePage.save();
		});

		// restore version 1000 (with ts 3000)
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setTimestamp(3000);

			PageResource res = new PageResourceImpl();
			ContentNodeRESTUtils.assertResponseOK(res.restoreVersion(page.getId().toString(), 1));
		});

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			// assert the current page
			assertThat(t.getObject(page).getContent().getTags()).as("Tags in page").isEmpty();

			// assert the page versions
			Page version1 = t.getObject(Page.class, page.getId(), 1);
			Page version2 = t.getObject(Page.class, page.getId(), 2);
			Page version3 = t.getObject(Page.class, page.getId(), 3);

			assertThat(version1.getContent().getTags()).as("Tags in version 1").isEmpty();
			assertThat(version2.getContent().getTags()).as("Tags in version 2").hasSize(1);
			Tag tag = version2.getContent().getTags().values().iterator().next();
			List<NodeObject> objects = new ArrayList<>(ContentNodeTestDataUtils
					.getPartType(OverviewPartType.class, tag, "overview").getOverview().getSelectedObjects());
			assertThat(objects).as("Overview in version 2").containsExactly(node.getFolder());
			assertThat(version3.getContent().getTags()).as("Tags in version 3").isEmpty();
		});
	}

	/**
	 * Test deleting a datasource tag while restoring a version
	 * @throws NodeException
	 */
	@Test
	public void testDeleteDatasourceTag() throws NodeException {
		// create page (with ts 1000)
		Page page = Trx.supply(() -> {
			TransactionManager.getCurrentTransaction().setTimestamp(1000);
			return ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Page");
		});

		// add overview tag to page (with ts 2000)
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setTimestamp(2000);

			Page editablePage = t.getObject(page, true);
			ContentTag tag = editablePage.getContent().addContentTag(datasourceConstructId);
			tag.setEnabled(true);
			Datasource datasource = ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, tag, "datasource").getDatasource();
			ContentNodeTestDataUtils.fillDatasource(datasource, Arrays.asList("one", "two", "three"));
			editablePage.save();
		});

		// restore version 1000 (with ts 3000)
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setTimestamp(3000);

			PageResource res = new PageResourceImpl();
			ContentNodeRESTUtils.assertResponseOK(res.restoreVersion(page.getId().toString(), 1));
		});

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			// assert the current page
			assertThat(t.getObject(page).getContent().getTags()).as("Tags in page").isEmpty();

			// assert the page versions
			Page version1 = t.getObject(Page.class, page.getId(), 1);
			Page version2 = t.getObject(Page.class, page.getId(), 2);
			Page version3 = t.getObject(Page.class, page.getId(), 3);

			assertThat(version1.getContent().getTags()).as("Tags in version 1").isEmpty();
			assertThat(version2.getContent().getTags()).as("Tags in version 2").hasSize(1);
			Tag tag = version2.getContent().getTags().values().iterator().next();
			assertThat(ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, tag, "datasource").getValues()).as("Values in version 2").containsExactly("one", "two", "three");
			assertThat(version3.getContent().getTags()).as("Tags in version 3").isEmpty();
		});
	}

	/**
	 * Clean the relevant data from the DB
	 * @throws NodeException
	 */
	protected void cleanDB() throws NodeException {
		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE datasource_value FROM datasource_value LEFT JOIN datasource ON datasource_value.datasource_id = datasource.id WHERE datasource.name IS NULL", null);
			DBUtils.executeUpdate("DELETE FROM datasource_value_nodeversion", null);
			DBUtils.executeUpdate("DELETE FROM datasource WHERE name IS NULL", null);
			DBUtils.executeUpdate("DELETE FROM datasource_nodeversion WHERE name IS NULL", null);
			DBUtils.executeUpdate("DELETE FROM ds_obj WHERE contenttag_id != 0", null);
			DBUtils.executeUpdate("DELETE FROM ds_obj_nodeversion WHERE contenttag_id != 0", null);
			DBUtils.executeUpdate("DELETE FROM ds WHERE contenttag_id != 0", null);
			DBUtils.executeUpdate("DELETE FROM ds_nodeversion WHERE contenttag_id != 0", null);
			DBUtils.executeUpdate("DELETE FROM value WHERE contenttag_id != 0", null);
			DBUtils.executeUpdate("DELETE FROM value_nodeversion WHERE contenttag_id != 0", null);
			DBUtils.executeUpdate("DELETE FROM contenttag", null);
			DBUtils.executeUpdate("DELETE FROM contenttag_nodeversion", null);
			DBUtils.executeUpdate("DELETE FROM content", null);
			DBUtils.executeUpdate("DELETE FROM page", null);
			DBUtils.executeUpdate("DELETE FROM page_nodeversion", null);
			DBUtils.executeUpdate("DELETE FROM nodeversion", null);
		});
	}

	/**
	 * Assert that the data that can be versioned is consistent
	 * @throws NodeException
	 */
	protected void assertDBConsistency() throws NodeException {
		// there must not be values referencing contenttags, that do not exist
		Trx.operate(() -> assertThat(
				DBUtils.select(
						"SELECT value.id FROM value LEFT JOIN contenttag ON value.contenttag_id = contenttag.id WHERE value.contenttag_id != 0 AND contenttag.id IS NULL",
						DBUtils.IDS)).as("Values referencing inexistent contenttags").isEmpty());

		// there must not be overviews referencing contenttags, that do not exist
		Trx.operate(() -> assertThat(
				DBUtils.select(
						"SELECT ds.id FROM ds LEFT JOIN contenttag ON ds.contenttag_id = contenttag.id WHERE ds.contenttag_id != 0 AND contenttag.id IS NULL",
						DBUtils.IDS)).as("Overviews referencing inexistent contenttags").isEmpty());

		// there must not be overview entries referencing contenttags, that do not exist
		Trx.operate(() -> assertThat(
				DBUtils.select(
						"SELECT ds_obj.id FROM ds_obj LEFT JOIN contenttag ON ds_obj.contenttag_id = contenttag.id WHERE ds_obj.contenttag_id != 0 AND contenttag.id IS NULL",
						DBUtils.IDS)).as("Overview entries referencing inexistent contenttags").isEmpty());

		// there must not be overview entries referencing overviews, that do not exist
		Trx.operate(() -> assertThat(
				DBUtils.select(
						"SELECT ds_obj.id FROM ds_obj LEFT JOIN ds ON ds_obj.ds_id = ds.id WHERE ds.id IS NULL",
						DBUtils.IDS)).as("Overview entries referencing inexistent overviews").isEmpty());

		// there must not be datasources with empty name, that are not referenced by any value
		Trx.operate(() -> assertThat(
				DBUtils.select(
						"SELECT datasource.id FROM datasource LEFT JOIN value ON datasource.id = value.value_ref AND value.part_id IN (SELECT id FROM part WHERE type_id = 32) WHERE datasource.name IS NULL AND value.id IS NULL",
						DBUtils.IDS)).as("Overviews with empty name not referenced from values").isEmpty());

		// there must not be datasource_values referencing datasources, that do not exist
		Trx.operate(() -> assertThat(
				DBUtils.select(
						"SELECT datasource_value.id FROM datasource_value LEFT JOIN datasource ON datasource_value.datasource_id = datasource.id WHERE datasource.id IS NULL",
						DBUtils.IDS)).as("Overview entries referencing inexistent overviews").isEmpty());
	}
}
