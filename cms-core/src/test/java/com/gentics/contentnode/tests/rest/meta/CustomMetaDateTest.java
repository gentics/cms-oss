package com.gentics.contentnode.tests.rest.meta;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.fillOverview;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.CustomMetaDateNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for custom item dates (custom_cdate, custom_edate)
 */
public abstract class CustomMetaDateTest<T extends CustomMetaDateNodeObject, R extends ContentNodeItem, I> {
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
	 * Test default values for new created item
	 * @throws NodeException
	 */
	@Test
	public void testDefaultValues() throws NodeException {
		int createTime = 10;
		R item = createMetaDated(createTime);

		assertThat(item).as("Created item")
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
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.of(customCdate), Optional.empty());

		assertThat(item).as("Updated item")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime))
			.has(attribute("customCdate", customCdate))
			.has(attribute("customEdate", 0));
	}

	public R createMetaDated(int createTime) throws NodeException {
		return createMetaDated(createTime, Optional.empty());
	}

	public abstract R createMetaDated(int createTime, Optional<Consumer<I>> maybeInflater) throws NodeException;

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
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.of(customCdate), Optional.empty());

		item = updateMetaDated(updateTime2, item.getId(), Optional.empty(), Optional.empty(), Optional.of(0), Optional.empty());

		assertThat(item).as("Updated item")
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
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(customEdate));

		assertThat(item).as("Updated item")
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
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(customEdate));

		item = updateMetaDated(updateTime2, item.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(0));

		assertThat(item).as("Updated item")
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
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.of(newCdate), Optional.empty(), Optional.empty(), Optional.empty());

		assertThat(item).as("Updated item")
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
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.of(newEdate), Optional.empty(), Optional.empty());

		assertThat(item).as("Updated item")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", createTime))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	protected abstract void updateName(I model, String name);

	/**
	 * Test sorting an overview by edate
	 * @throws NodeException
	 */
	protected void testSortOverviewByEDate(Class<T> classOfT, String expected) throws NodeException {
		List<R> restObjects = new ArrayList<>();
		for (int time : Arrays.asList(100, 200, 300, 400)) {
			R restObject = createMetaDated(time, Optional.of(req -> updateName(req, "EDate " + classOfT.getSimpleName() + " " + time)));
			updateMetaDated(time, restObject.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(time % 200 != 0 ? (1000 - time) : null));
			restObjects.add(restObject);
		}

		Page overviewPage = createPage(1, null);
		AtomicReference<String> tagName = new AtomicReference<>();
		consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<T> nodeObjects = t.getObjects(classOfT, restObjects.stream().map(ContentNodeItem::getId).collect(Collectors.toList()));
			update(t.getObject(com.gentics.contentnode.object.Page.class, id), update -> {
				ContentTag tag = update.getContent().addContentTag(overviewConstructId);
				tagName.set(tag.getName());
				fillOverview(tag, "ds", "[<node " + restObjects.get(0).getType().toString() + ".name>, <node " + restObjects.get(0).getType().toString() + ".edittimestamp>]", 
						classOfT, Overview.SELECTIONTYPE_SINGLE, 0, Overview.ORDER_EDATE, Overview.ORDERWAY_ASC, false, nodeObjects);
			});
		}, overviewPage.getId());

		String renderedOverview = execute(systemUser, id -> {
			PageRenderResponse response = new PageResourceImpl().render(Integer.toString(id), null, "<node " + tagName.get() + ">", false, null,
					LinksType.frontend, false, false, false, 0);
			assertResponseCodeOk(response);
			return response.getContent();
		}, overviewPage.getId());
		assertThat(renderedOverview).as("Rendered Overview").isEqualTo(expected);
	}

	/**
	 * Test sorting an overview by cdate
	 * @throws NodeException
	 */
	protected void testSortOverviewByCDate(Class<T> classOfT, String expected) throws NodeException {
		List<R> restObjects = new ArrayList<>();
		for (int time : Arrays.asList(100, 200, 300, 400)) {
			R restObject = createMetaDated(time, Optional.of(req -> updateName(req, "CDate " + classOfT.getSimpleName() + " " + time)));
			updateMetaDated(time, restObject.getId(), Optional.empty(), Optional.empty(), Optional.ofNullable(time % 200 == 0 ? (1000 - time) : null), Optional.empty());
			restObjects.add(restObject);
		}

		Page overviewPage = createPage(1, null);
		AtomicReference<String> tagName = new AtomicReference<>();
		consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<T> nodeObjects = t.getObjects(classOfT, restObjects.stream().map(ContentNodeItem::getId).collect(Collectors.toList()));
			update(t.getObject(com.gentics.contentnode.object.Page.class, id), update -> {
				ContentTag tag = update.getContent().addContentTag(overviewConstructId);
				tagName.set(tag.getName());
				fillOverview(tag, "ds", "[<node " + restObjects.get(0).getType().toString() + ".name>, <node " + restObjects.get(0).getType().toString() + ".creationtimestamp>]", classOfT, Overview.SELECTIONTYPE_SINGLE, 0, Overview.ORDER_CDATE,
						Overview.ORDERWAY_ASC, false, nodeObjects);
			});
		}, overviewPage.getId());

		String renderedOverview = execute(systemUser, id -> {
			PageRenderResponse response = new PageResourceImpl().render(Integer.toString(id), null, "<node " + tagName.get() + ">", false, null,
					LinksType.frontend, false, false, false, 0);
			assertResponseCodeOk(response);
			return response.getContent();
		}, overviewPage.getId());
		assertThat(renderedOverview).as("Rendered Overview").isEqualTo(expected);
	}

	/**
	 * Create new page
	 * @param timestamp timestamp of the transaction
	 * @param creator optional consumer that can modify the create request
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPage(int timestamp, Consumer<PageCreateRequest> creator) throws NodeException {
		Page page = null;

		try (Trx trx = new Trx()) {
			trx.at(timestamp);

			PageCreateRequest request = new PageCreateRequest();
			request.setFolderId(String.valueOf(node.getFolder().getId()));
			request.setTemplateId(template.getId());

			if (creator != null) {
				creator.accept(request);
			}

			PageLoadResponse response = new PageResourceImpl().create(request);
			assertResponseCodeOk(response);
			page = response.getPage();

			trx.success();
		}

		return page;
	}

	/**
	 * Publish the page
	 * @param pageId page ID
	 * @throws NodeException
	 */
	protected void publishPage(int pageId) throws NodeException {
		operate(() -> {
			GenericResponse response = new PageResourceImpl().publish(Integer.toString(pageId), null, new PagePublishRequest());
			assertResponseCodeOk(response);
		});
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
