package com.gentics.contentnode.tests.rest.meta;

import static com.gentics.contentnode.events.DependencyManager.createDependency;
import static com.gentics.contentnode.events.Events.UPDATE;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.publish.PublishQueue.getDirtedObjectIds;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

public class CustomPageMetaDateTest extends CustomMetaDateTest<com.gentics.contentnode.object.Page, Page, PageCreateRequest> {

	/**
	 * Test sorting an overview by cdate
	 * @throws NodeException
	 */
	@Test
	public void testSortOverviewByCDate() throws NodeException {
		testSortOverviewByCDate(com.gentics.contentnode.object.Page.class, "[CDate Page 100, 100][CDate Page 300, 300][CDate Page 400, 600][CDate Page 200, 800]");
	}

	/**
	 * Test sorting an overview by edate
	 * @throws NodeException
	 */
	@Test
	public void testSortOverviewByEDate() throws NodeException {
		testSortOverviewByEDate(com.gentics.contentnode.object.Page.class, "[EDate Page 200, 200][EDate Page 400, 400][EDate Page 300, 700][EDate Page 100, 900]");
	}

	@Override
	public Page createMetaDated(int createTime, Optional<Consumer<PageCreateRequest>> maybeInflater) throws NodeException {
		return createPage(createTime, maybeInflater.orElse(null));
	}

	@Override
	public Page updateMetaDated(int updateTime, Integer id, Optional<Integer> maybeDate, Optional<Integer> maybeEDate,
			Optional<Integer> maybeCustomCDate, Optional<Integer> maybeCustomEDate) throws NodeException {
		Page page = updatePage(updateTime, id, request -> {
			maybeDate.ifPresent(cdate -> request.getPage().setCdate(cdate));
			maybeEDate.ifPresent(edate -> request.getPage().setEdate(edate));
			maybeCustomCDate.ifPresent(cdate -> request.getPage().setCustomCdate(cdate));
			maybeCustomEDate.ifPresent(edate -> request.getPage().setCustomEdate(edate));
		});
		publishPage(page.getId());
		return page;
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
	 * Test rendering default cdate
	 * @throws NodeException
	 */
	@Test
	public void testRenderCDate() throws NodeException {
		int createTime = 86400;
		Page item = createMetaDated(createTime);

		assertRenderedCDate(item.getId(), createTime);
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
		Page item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.of(customCdate), Optional.empty());

		assertRenderedCDate(item.getId(), customCdate);
	}

	/**
	 * Test rendering default edate
	 * @throws NodeException
	 */
	@Test
	public void testRenderEDate() throws NodeException {
		int createTime = 86400;
		Page item = createMetaDated(createTime);

		assertRenderedEDate(item.getId(), createTime);
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
		Page item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(customEdate));

		assertRenderedEDate(item.getId(), customEdate);
	}

	/**
	 * Assert that the rendered CDate (in all possible variants) is equal to the expected timestamp
	 * @param itemId item ID
	 * @param expectedTimestamp expected timestamp
	 * @throws NodeException
	 */
	protected void assertRenderedCDate(int itemId, int expectedTimestamp) throws NodeException {
		ContentNodeDate date = new ContentNodeDate(expectedTimestamp);
		for (Map.Entry<String, DateFormat> entry : cDatePropertyMap.entrySet()) {
			assertThat(render(itemId, entry.getKey())).as("Rendered " + entry.getKey()).isEqualTo(entry.getValue().render(date));
		}
	}

	/**
	 * Assert that the rendered EDate (in all possible variants) is equal to the expected timestamp
	 * @param itemId item ID
	 * @param expectedTimestamp expected timestamp
	 * @throws NodeException
	 */
	protected void assertRenderedEDate(int itemId, int expectedTimestamp) throws NodeException {
		ContentNodeDate date = new ContentNodeDate(expectedTimestamp);
		for (Map.Entry<String, DateFormat> entry : eDatePropertyMap.entrySet()) {
			assertThat(render(itemId, entry.getKey())).as("Rendered " + entry.getKey()).isEqualTo(entry.getValue().render(date));
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

		Page page = createMetaDated(createTime);

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

	@Override
	protected void updateName(PageCreateRequest model, String name) {
		model.setPageName(name);
	}
}
