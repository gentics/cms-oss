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
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.fillOverview;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for custom page dates (custom_cdate, custom_edate)
 */
public class CustomMetaDateTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Map<String, DateFormat> cDatePropertyMap = new HashMap<>();

	private static Map<String, DateFormat> eDatePropertyMap = new HashMap<>();

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

	private static Node node;

	private static Template template;

	private static SystemUser systemUser;

	private static Integer overviewConstructId;

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
		Page page = createPage(createTime, null);

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
		Page page = createPage(createTime, null);

		page = updatePage(updateTime, page.getId(), request -> {
			request.getPage().setCustomCdate(customCdate);
		});

		assertThat(page).as("Updated page")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime))
			.has(attribute("customCdate", customCdate))
			.has(attribute("customEdate", 0));
	}

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
		Page page = createPage(createTime, null);

		page = updatePage(updateTime, page.getId(), request -> {
			request.getPage().setCustomCdate(customCdate);
		});

		page = updatePage(updateTime2, page.getId(), request -> {
			request.getPage().setCustomCdate(0);
		});

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
		Page page = createPage(createTime, null);

		page = updatePage(updateTime, page.getId(), request -> {
			request.getPage().setCustomEdate(customEdate);
		});

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
		Page page = createPage(createTime, null);

		page = updatePage(updateTime, page.getId(), request -> {
			request.getPage().setCustomEdate(customEdate);
		});

		page = updatePage(updateTime2, page.getId(), request -> {
			request.getPage().setCustomEdate(0);
		});

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
		Page page = createPage(createTime, null);

		page = updatePage(updateTime, page.getId(), request -> {
			request.getPage().setCdate(newCdate);
		});

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
		Page page = createPage(createTime, null);

		page = updatePage(updateTime, page.getId(), request -> {
			request.getPage().setEdate(newEdate);
		});

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
		Page page = createPage(createTime, null);

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
		Page page = createPage(createTime, null);

		page = updatePage(updateTime, page.getId(), request -> {
			request.getPage().setCustomCdate(customCdate);
		});

		assertRenderedCDate(page.getId(), customCdate);
	}

	/**
	 * Test rendering default edate
	 * @throws NodeException
	 */
	@Test
	public void testRenderEDate() throws NodeException {
		int createTime = 86400;
		Page page = createPage(createTime, null);

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
		Page page = createPage(createTime, null);

		page = updatePage(updateTime, page.getId(), request -> {
			request.getPage().setCustomEdate(customEdate);
		});

		assertRenderedEDate(page.getId(), customEdate);
	}

	/**
	 * Test dirting when custom cdate is changed
	 * @throws NodeException
	 */
	@Test
	public void testCustomCDateDirting() throws NodeException {
		int customCdate = 100 * 86400;

		for (String property : cDatePropertyMap.keySet()) {
			doDirtingTest(page -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				createDependency(t.getObject(com.gentics.contentnode.object.Page.class, page.getId()), property, node.getFolder(), UPDATE).store();
			}, page -> {
				page.setCustomCdate(customCdate);
			}, page -> {
				assertThat(getDirtedObjectIds(Folder.class, false, node)).as("Dirted folders after change of " + property).containsOnly(node.getFolder().getId());
			});
		}
	}

	/**
	 * Test dirting when custom edate is changed
	 * @throws NodeException
	 */
	@Test
	public void testCustomEDateDirting() throws NodeException {
		int customEdate = 100 * 86400;

		for (String property : eDatePropertyMap.keySet()) {
			doDirtingTest(page -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				createDependency(t.getObject(com.gentics.contentnode.object.Page.class, page.getId()), property, node.getFolder(), UPDATE).store();
			}, page -> {
				page.setCustomEdate(customEdate);
			}, page -> {
				assertThat(getDirtedObjectIds(Folder.class, false, node)).as("Dirted folders after change of " + property).containsOnly(node.getFolder().getId());
			});
		}
	}

	/**
	 * Test sorting an overview by cdate
	 * @throws NodeException
	 */
	@Test
	public void testSortOverviewByCDate() throws NodeException {
		Set<Page> pages = new HashSet<>();
		for (int time : Arrays.asList(100, 200, 300, 400)) {
			Page page = createPage(time, req -> {
				req.setPageName("CDate Page " + time);
			});
			updatePage(time, page.getId(), req -> {
				if (time % 200 == 0) {
					req.getPage().setCustomCdate((1000 - time));
				}
			});
			publishPage(page.getId());
			pages.add(page);
		}

		Page overviewPage = createPage(1, null);
		AtomicReference<String> tagName = new AtomicReference<>();
		consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<com.gentics.contentnode.object.Page> nodePages = t.getObjects(com.gentics.contentnode.object.Page.class,
					pages.stream().map(Page::getId).collect(Collectors.toList()));
			update(t.getObject(com.gentics.contentnode.object.Page.class, id), update -> {
				ContentTag tag = update.getContent().addContentTag(overviewConstructId);
				tagName.set(tag.getName());
				fillOverview(tag, "ds", "[<node page.name>, <node page.creationtimestamp>]", com.gentics.contentnode.object.Page.class, Overview.SELECTIONTYPE_SINGLE, 0, Overview.ORDER_CDATE,
						Overview.ORDERWAY_ASC, false, nodePages);
			});
		}, overviewPage.getId());

		String renderedOverview = execute(systemUser, id -> {
			PageRenderResponse response = new PageResourceImpl().render(Integer.toString(id), null, "<node " + tagName.get() + ">", false, null,
					LinksType.frontend, false, false, false);
			assertResponseCodeOk(response);
			return response.getContent();
		}, overviewPage.getId());
		assertThat(renderedOverview).as("Rendered Overview").isEqualTo("[CDate Page 100, 100][CDate Page 300, 300][CDate Page 400, 600][CDate Page 200, 800]");
	}

	/**
	 * Test sorting an overview by edate
	 * @throws NodeException
	 */
	@Test
	public void testSortOverviewByEDate() throws NodeException {
		Set<Page> pages = new HashSet<>();
		for (int time : Arrays.asList(100, 200, 300, 400)) {
			Page page = createPage(time, req -> {
				req.setPageName("EDate Page " + time);
			});
			updatePage(time, page.getId(), req -> {
				if (time % 200 != 0) {
					req.getPage().setCustomEdate((1000 - time));
				}
			});
			publishPage(page.getId());
			pages.add(page);
		}

		Page overviewPage = createPage(1, null);
		AtomicReference<String> tagName = new AtomicReference<>();
		consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<com.gentics.contentnode.object.Page> nodePages = t.getObjects(com.gentics.contentnode.object.Page.class,
					pages.stream().map(Page::getId).collect(Collectors.toList()));
			update(t.getObject(com.gentics.contentnode.object.Page.class, id), update -> {
				ContentTag tag = update.getContent().addContentTag(overviewConstructId);
				tagName.set(tag.getName());
				fillOverview(tag, "ds", "[<node page.name>, <node page.edittimestamp>]", com.gentics.contentnode.object.Page.class, Overview.SELECTIONTYPE_SINGLE, 0, Overview.ORDER_EDATE,
						Overview.ORDERWAY_ASC, false, nodePages);
			});
		}, overviewPage.getId());

		String renderedOverview = execute(systemUser, id -> {
			PageRenderResponse response = new PageResourceImpl().render(Integer.toString(id), null, "<node " + tagName.get() + ">", false, null,
					LinksType.frontend, false, false, false);
			assertResponseCodeOk(response);
			return response.getContent();
		}, overviewPage.getId());
		assertThat(renderedOverview).as("Rendered Overview").isEqualTo("[EDate Page 200, 200][EDate Page 400, 400][EDate Page 300, 700][EDate Page 100, 900]");
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
	 * Update the page
	 * @param timestamp transaction timestamp
	 * @param pageId page ID
	 * @param updater optional consumer for the page save request
	 * @return updated page
	 * @throws NodeException
	 */
	protected Page updatePage(int timestamp, int pageId, Consumer<PageSaveRequest> updater) throws NodeException {
		try (Trx trx = new Trx()) {
			trx.at(timestamp);

			Page update = new Page();
			PageSaveRequest request = new PageSaveRequest(update);
			if (updater != null) {
				updater.accept(request);
			}
			GenericResponse response = new PageResourceImpl().save(String.valueOf(pageId), request);
			assertResponseCodeOk(response);

			trx.success();
		}

		return loadPage(pageId);
	}

	/**
	 * Load the page with ID
	 * @param pageId page ID
	 * @return page
	 * @throws NodeException
	 */
	protected Page loadPage(int pageId) throws NodeException {
		return loadPage(String.valueOf(pageId));
	}

	/**
	 * Load the page with ID
	 * @param pageId global or local ID
	 * @return page
	 * @throws NodeException
	 */
	protected Page loadPage(String pageId) throws NodeException {
		return supply(() -> {
			PageLoadResponse response = new PageResourceImpl().load(pageId, false, false, false, false, false, false, false, false, false, false, 0, null);
			assertResponseCodeOk(response);
			return response.getPage();
		});
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
	 * Render the property for the page
	 * @param pageId page ID
	 * @param property page property to rendered
	 * @return rendered property
	 * @throws NodeException
	 */
	protected String render(int pageId, String property) throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
			PageRenderResponse response = new PageResourceImpl().render(String.valueOf(pageId), 0, String.format("<node page.%s>", property), false, null,
					LinksType.frontend, false, false, false);
			assertResponseCodeOk(response);
			trx.success();
			return response.getContent();
		}
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

	protected void doDirtingTest(Consumer<Page> dependencyCreator, Consumer<Page> updater, Consumer<Page> dirtChecker) throws NodeException {
		operate(() -> {
			DBUtils.update("DELETE FROM dependencymap2");
			DBUtils.update("DELETE FROM publishqueue");
			DBUtils.update("DELETE FROM dirtqueue");
		});

		int createTime = 86400;
		int publishTime = 2 * 86400;
		int dependencyTime = 3 * 86400;
		int updateTime = 4 * 86400;
		int publishTime2 = 5 * 86400;

		Page page = createPage(createTime, null);

		try (Trx trx = new Trx()) {
			trx.at(publishTime);
			GenericResponse response = new PageResourceImpl().publish(String.valueOf(page.getId()), null, new PagePublishRequest());
			assertResponseCodeOk(response);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			trx.at(dependencyTime);
			dependencyCreator.accept(page);
			trx.success();
		}

		updater.accept(page);
		try (Trx trx = new Trx()) {
			trx.at(updateTime);
			GenericResponse response = new PageResourceImpl().save(String.valueOf(page.getId()), new PageSaveRequest(page));
			assertResponseCodeOk(response);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			trx.at(publishTime2);
			GenericResponse response = new PageResourceImpl().publish(String.valueOf(page.getId()), null, new PagePublishRequest());
			assertResponseCodeOk(response);
			trx.success();
		}

		try {
			testContext.waitForDirtqueueWorker();
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}

		Trx.operate(() -> {
			dirtChecker.accept(page);
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
