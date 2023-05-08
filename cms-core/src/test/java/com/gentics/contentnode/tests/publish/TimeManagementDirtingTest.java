package com.gentics.contentnode.tests.publish;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.events.DependencyManager.createDependency;
import static com.gentics.contentnode.events.DependencyManager.initDependencyTriggering;
import static com.gentics.contentnode.events.DependencyManager.resetDependencyTriggering;
import static com.gentics.contentnode.events.Events.UPDATE;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.publish.PublishQueue.finishFastDependencyDirting;
import static com.gentics.contentnode.publish.PublishQueue.getDirtedObjectIds;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for dirting due to time management
 */
public class TimeManagementDirtingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Template template;
	private static Page dependentPage;
	private static Page independentPage;

	/**
	 * Set up static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setUpOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		dependentPage = supply(() -> createPage(node.getFolder(), template, "Dependent Page"));
		independentPage = supply(() -> createPage(node.getFolder(), template, "Independent Page"));

		operate(() -> update(dependentPage, Page::publish));
		operate(() -> update(independentPage, Page::publish));
	}

	/**
	 * Set up dependencies and dirtqueue
	 * @throws NodeException
	 */
	@Before
	public void setUp() throws NodeException {
		operate(() -> {
			update("DELETE FROM dirtqueue");
			update("DELETE FROM publishqueue");
			update("DELETE FROM dependencymap2");
		});
	}

	@Test
	public void testDirectDependencyGoOnline() throws NodeException {
		doGoOnline(page -> {
			createDependency(page, "online", dependentPage, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(page.getId(), dependentPage.getId());
		});
	}

	@Test
	public void testIndirectDependencyGoOnline() throws NodeException {
		doGoOnline(page -> {
			createDependency(page.getFolder(), "pages", dependentPage, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(page.getId(), dependentPage.getId());
		});
	}

	@Test
	public void testDirectNameDependencyGoOnline() throws NodeException {
		doGoOnline(page -> {
			createDependency(page, "name", dependentPage, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(page.getId());
		});
	}

	@Test
	public void testIndirectNameDependencyGoOnline() throws NodeException {
		doGoOnline(page -> {
			createDependency(Folder.class, page.getFolder().getId(), Page.class, null, "name", Page.class, dependentPage.getId(), null, null, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(page.getId());
		});
	}

	@Test
	public void testDirectDependencyGoOffline() throws NodeException {
		doGoOffline(page -> {
			createDependency(page, "online", dependentPage, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(dependentPage.getId());
		});
	}

	@Test
	public void testIndirectDependencyGoOffline() throws NodeException {
		doGoOffline(page -> {
			createDependency(page.getFolder(), "pages", dependentPage, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(dependentPage.getId());
		});
	}

	@Test
	public void testDirectNameDependencyGoOffline() throws NodeException {
		doGoOffline(page -> {
			createDependency(page, "name", dependentPage, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly();
		});
	}

	@Test
	public void testIndirectNameDependencyGoOffline() throws NodeException {
		doGoOffline(page -> {
			createDependency(Folder.class, page.getFolder().getId(), Page.class, null, "name", Page.class, dependentPage.getId(), null, null, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly();
		});
	}

	@Test
	public void testDirectDependencyRepublish() throws NodeException {
		doRepublish(page -> {
			createDependency(page, "online", dependentPage, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(page.getId());
		});
	}

	@Test
	public void testIndirectDependencyRepublish() throws NodeException {
		doRepublish(page -> {
			createDependency(page.getFolder(), "pages", dependentPage, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(page.getId());
		});
	}

	@Test
	public void testDirectNameDependencyRepublish() throws NodeException {
		doRepublish(page -> {
			createDependency(page, "name", dependentPage, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(page.getId(), dependentPage.getId());
		});
	}

	@Test
	public void testIndirectNameDependencyRepublish() throws NodeException {
		doRepublish(page -> {
			createDependency(Folder.class, page.getFolder().getId(), Page.class, null, "name", Page.class, dependentPage.getId(), null, null, UPDATE).store();
		}, page -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").containsOnly(page.getId(), dependentPage.getId());
		});
	}

	/**
	 * Create a page and let it go online with time management
	 * @param dependencyCreator consumer, that creates fake dependencies
	 * @param dirtChecker consumer, that checks dirted objects after page went online
	 * @throws NodeException
	 */
	protected void doGoOnline(Consumer<Page> dependencyCreator, Consumer<Page> dirtChecker) throws NodeException {
		int createTime = 1;
		int timeStart  = 3;
		int beforeDue  = 2;
		int afterDue   = 4;

		// create page that will go online
		Page page = supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, upd -> upd.publish(timeStart, null));
		});

		// fake dependencies
		consume(dependencyCreator, page);

		// handle timemanagement before due time
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(beforeDue * 1000L);
			initDependencyTriggering();
			assertThat(page.handleTimemanagement()).as("Time Management handled").isFalse();
			finishFastDependencyDirting();
			trx.success();
		} finally {
			resetDependencyTriggering();
		}

		Trx.operate(() -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").isEmpty();
			assertThat(getDirtedObjectIds(Folder.class, false, node)).as("Dirted folders").isEmpty();
		});

		// handle timemanagement after due time
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(afterDue * 1000L);
			initDependencyTriggering();
			assertThat(page.handleTimemanagement()).as("Time Management handled").isTrue();
			finishFastDependencyDirting();
			trx.success();
		} finally {
			resetDependencyTriggering();
		}

		// check dirted objects
		consume(dirtChecker, page);
	}

	/**
	 * Create a page and let it go offline with time management
	 * @param dependencyCreator consumer, that creates fake dependencies
	 * @param dirtChecker consumer, that checks dirted objects after page went offline
	 * @throws NodeException
	 */
	protected void doGoOffline(Consumer<Page> dependencyCreator, Consumer<Page> dirtChecker) throws NodeException {
		int createTime = 1;
		int timeEnd  = 3;
		int beforeDue  = 2;
		int afterDue   = 4;

		// create page that will go offline
		Page page = supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			update(created, Page::publish);
			return update(created, upd -> upd.takeOffline(timeEnd));
		});

		// fake dependencies
		consume(dependencyCreator, page);

		// handle timemanagement before due time
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(beforeDue * 1000L);
			initDependencyTriggering();
			assertThat(page.handleTimemanagement()).as("Time Management handled").isFalse();
			finishFastDependencyDirting();
			trx.success();
		} finally {
			resetDependencyTriggering();
		}

		Trx.operate(() -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").isEmpty();
			assertThat(getDirtedObjectIds(Folder.class, false, node)).as("Dirted folders").isEmpty();
		});

		// handle timemanagement after due time
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(afterDue * 1000L);
			initDependencyTriggering();
			assertThat(page.handleTimemanagement()).as("Time Management handled").isTrue();
			finishFastDependencyDirting();
			trx.success();
		} finally {
			resetDependencyTriggering();
		}

		// check dirted objects
		consume(dirtChecker, page);
	}

	/**
	 * Create a published page and let it republish with changed name
	 * @param dependencyCreator consumer, that creates fake dependencies
	 * @param dirtChecker consumer, that checks dirted objects after page was republished
	 * @throws NodeException
	 */
	protected void doRepublish(Consumer<Page> dependencyCreator, Consumer<Page> dirtChecker) throws NodeException {
		int createTime    = 1;
		int updateTime    = 2;
		int timeRepublish = 4;
		int beforeDue     = 3;
		int afterDue      = 5;

		// create page that will be republished with different name
		Page page = supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, Page::publish);
		});

		// update and let be republished
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(updateTime * 1000L);
			Page updated = trx.getTransaction().getObject(page, true);
			updated.setName("Modified " + updated.getName());
			updated.save();
			updated.publish(timeRepublish, null);

			page = page.reload();
			trx.success();
		}

		// fake dependencies
		consume(dependencyCreator, page);

		// handle timemanagement before due time
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(beforeDue * 1000L);
			initDependencyTriggering();
			assertThat(page.handleTimemanagement()).as("Time Management handled").isFalse();
			finishFastDependencyDirting();
			trx.success();
		} finally {
			resetDependencyTriggering();
		}

		Trx.operate(() -> {
			assertThat(getDirtedObjectIds(Page.class, false, node)).as("Dirted pages").isEmpty();
			assertThat(getDirtedObjectIds(Folder.class, false, node)).as("Dirted folders").isEmpty();
		});

		// handle timemanagement after due time
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(afterDue * 1000L);
			initDependencyTriggering();
			assertThat(page.handleTimemanagement()).as("Time Management handled").isTrue();
			finishFastDependencyDirting();
			trx.success();
		} finally {
			resetDependencyTriggering();
		}

		// check dirted objects
		consume(dirtChecker, page);
	}
}
