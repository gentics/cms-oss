package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.events.DependencyManager.createDependency;
import static com.gentics.contentnode.events.Events.UPDATE;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.publish.PublishQueue.getDirtedObjectIds;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.CustomMetaDateNodeObject;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for custom page dates (custom_cdate, custom_edate)
 */
public abstract class CustomMetaDateTest<T extends CustomMetaDateNodeObject, R extends ContentNodeItem> {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Map<String, DateFormat> cDatePropertyMap = new HashMap<>();

	protected static Map<String, DateFormat> eDatePropertyMap = new HashMap<>();

	static {
		cDatePropertyMap.put("creationtimestamp", DateFormat.TIMESTAMP);
		cDatePropertyMap.put("erstellungstimestamp", DateFormat.TIMESTAMP);
		cDatePropertyMap.put("createtimestamp", DateFormat.TIMESTAMP);
		cDatePropertyMap.put("createtime", DateFormat.FULLFORMAT);
		cDatePropertyMap.put("creationdate", DateFormat.TOSTRING);
		cDatePropertyMap.put("erstellungsdatum", DateFormat.TOSTRING);

		eDatePropertyMap.put("edittimestamp", DateFormat.TIMESTAMP);
		eDatePropertyMap.put("bearbeitungstimestamp", DateFormat.TIMESTAMP);
		eDatePropertyMap.put("edittime", DateFormat.FULLFORMAT);
		eDatePropertyMap.put("editdate", DateFormat.TOSTRING);
		eDatePropertyMap.put("bearbeitungsdatum", DateFormat.TOSTRING);
	}

	protected static Node node;

	protected static Template template;

	protected static SystemUser systemUser;

	protected static Integer overviewConstructId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		systemUser = supply(t -> t.getObject(SystemUser.class, 1));

		overviewConstructId = supply(() -> createConstruct(node, OverviewPartType.class, "overview", "ds"));
	}

	/**
	 * Test default values for new created page
	 * @throws NodeException
	 */
	@Test
	public void testDefaultValues() throws NodeException {
		int createTime = 10;
		R page = createMetaDated(createTime);

		assertThat(page).as("Created page")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", createTime))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Test setting a custom cdate
	 * @throws NodeException
	 */
	@Test
	public void testSetCustomCDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int customCdate = 20;
		R page = createMetaDated(createTime);

		page = updateMetaDated(updateTime, page.getId(), Optional.empty(), Optional.empty(), Optional.of(customCdate), Optional.empty());

		assertThat(page).as("Updated page")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime))
			.has(attribute("customCdate", customCdate))
			.has(attribute("customEdate", 0));
	}

	public abstract R createMetaDated(int createTime) throws NodeException;

	public abstract R updateMetaDated(int updateTime, Integer id, Optional<Integer> maybeDate, Optional<Integer> maybeEDate, Optional<Integer> maybeCustomCDate, Optional<Integer> maybeCustomEDate) throws NodeException;

	/**
	 * Test unsetting the custom cdate
	 * @throws NodeException
	 */
	@Test
	public void testUnsetCustomCDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int customCdate = 20;
		int updateTime2 = 40;
		R page = createMetaDated(createTime);

		page = updateMetaDated(updateTime, page.getId(), Optional.empty(), Optional.empty(), Optional.of(customCdate), Optional.empty());

		page = updateMetaDated(updateTime, page.getId(), Optional.empty(), Optional.empty(), Optional.of(0), Optional.empty());

		assertThat(page).as("Updated page")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime2))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Test setting a custom edate
	 * @throws NodeException
	 */
	@Test
	public void testSetCustomEDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int customEdate = 20;
		R page = createMetaDated(createTime);

		page = updateMetaDated(updateTime, page.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(customEdate));

		assertThat(page).as("Updated page")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", customEdate));
	}

	/**
	 * Test unsetting a custom edate
	 * @throws NodeException
	 */
	@Test
	public void testUnsetCustomEDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int customEdate = 20;
		int updateTime2 = 40;
		R page = createMetaDated(createTime);

		page = updateMetaDated(updateTime, page.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(customEdate));

		page = updateMetaDated(updateTime, page.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(0));

		assertThat(page).as("Updated page")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime2))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Test setting cdate (which does not change anything)
	 * @throws NodeException
	 */
	@Test
	public void testSetCDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int newCdate = 20;
		R page = createMetaDated(createTime);

		page = updateMetaDated(updateTime, page.getId(), Optional.of(newCdate), Optional.empty(), Optional.empty(), Optional.empty());

		assertThat(page).as("Updated page")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", createTime))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Test setting edate (which does not change anything)
	 * @throws NodeException
	 */
	@Test
	public void testSetEDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int newEdate = 20;
		R page = createMetaDated(createTime);

		page = updateMetaDated(updateTime, page.getId(), Optional.empty(), Optional.of(newEdate), Optional.empty(), Optional.empty());

		assertThat(page).as("Updated page")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", createTime))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Test rendering default cdate
	 * @throws NodeException
	 */
	@Test
	public void testRenderCDate() throws NodeException {
		int createTime = 86400;
		R page = createMetaDated(createTime);

		assertRenderedCDate(page.getId(), createTime);
	}

	/**
	 * Test rendering custom cdate
	 * @throws NodeException
	 */
	@Test
	public void testRenderCustomCDate() throws NodeException {
		int createTime = 86400;
		int updateTime = 86400 * 3;
		int customCdate = 86400 * 2;
		R page = createMetaDated(createTime);

		page = updateMetaDated(updateTime, page.getId(), Optional.empty(), Optional.empty(), Optional.of(customCdate), Optional.empty());

		assertRenderedCDate(page.getId(), customCdate);
	}

	/**
	 * Test rendering default edate
	 * @throws NodeException
	 */
	@Test
	public void testRenderEDate() throws NodeException {
		int createTime = 86400;
		R page = createMetaDated(createTime);

		assertRenderedEDate(page.getId(), createTime);
	}

	/**
	 * Test rendering custom edate
	 * @throws NodeException
	 */
	@Test
	public void testRenderCustomEDate() throws NodeException {
		int createTime = 86400;
		int updateTime = 86400 * 3;
		int customEdate = 86400 * 2;
		R page = createMetaDated(createTime);

		page = updateMetaDated(updateTime, page.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(customEdate));

		assertRenderedEDate(page.getId(), customEdate);
	}

	/**
	 * Assert that the rendered CDate (in all possible variants) is equal to the expected timestamp
	 * @param pageId page ID
	 * @param expectedTimestamp expected timestamp
	 * @throws NodeException
	 */
	protected void assertRenderedCDate(int pageId, int expectedTimestamp) throws NodeException {
		ContentNodeDate date = new ContentNodeDate(expectedTimestamp);
		for (Map.Entry<String, DateFormat> entry : cDatePropertyMap.entrySet()) {
			assertThat(render(pageId, entry.getKey())).as("Rendered " + entry.getKey()).isEqualTo(entry.getValue().render(date));
		}
	}

	/**
	 * Assert that the rendered EDate (in all possible variants) is equal to the expected timestamp
	 * @param pageId page ID
	 * @param expectedTimestamp expected timestamp
	 * @throws NodeException
	 */
	protected void assertRenderedEDate(int pageId, int expectedTimestamp) throws NodeException {
		ContentNodeDate date = new ContentNodeDate(expectedTimestamp);
		for (Map.Entry<String, DateFormat> entry : eDatePropertyMap.entrySet()) {
			assertThat(render(pageId, entry.getKey())).as("Rendered " + entry.getKey()).isEqualTo(entry.getValue().render(date));
		}
	}

	/**
	 * Render the property for the page
	 * @param pageId page ID
	 * @param property page property to rendered
	 * @return rendered property
	 * @throws NodeException
	 */
	protected String render(int pageId, String property) throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
			PageRenderResponse response = new PageResourceImpl().render(String.valueOf(pageId), 0, String.format("<node page.%s>", property), false, null,
					LinksType.frontend, false, false, false, 0);
			assertResponseCodeOk(response);
			trx.success();
			return response.getContent();
		}
	}

	/**
	 * Enum of date formats for rendering
	 */
	protected static enum DateFormat {
		/**
		 * Rendering the timestamp
		 */
		TIMESTAMP(date -> String.valueOf(date.getIntTimestamp())),

		/**
		 * Rendering by calling {@link ContentNodeDate#getFullFormat()}
		 */
		FULLFORMAT(ContentNodeDate::getFullFormat),

		/**
		 * Rendering by calling {@link ContentNodeDate#toString()}
		 */
		TOSTRING(ContentNodeDate::toString);

		/**
		 * Rendering function
		 */
		protected Function<ContentNodeDate, String> renderer;

		/**
		 * Create instance with rendering function
		 * @param renderer function
		 */
		DateFormat(Function<ContentNodeDate, String> renderer) {
			this.renderer = renderer;
		}

		/**
		 * Render the date
		 * @param date date
		 * @return rendered date
		 */
		public String render(ContentNodeDate date) {
			return renderer.apply(date);
		}
	}
}
