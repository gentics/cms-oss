package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the page endpoints, which are executed with a lock on the content of the page (save, publish and restore version, see SUP-20172):
 * the endpoints must work without a current transaction (like REST requests), and errors must be thrown (so that they are mapped to responses
 * with the matching HTTP status code) instead of being returned as responses with HTTP status 200.
 */
public class LockedPageEndpointsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * ID of a page, which does not exist
	 */
	private final static String NOT_EXISTING_PAGE_ID = "999999";

	private static Node node;

	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Test saving a page without a current transaction
	 * @throws Exception
	 */
	@Test
	public void testSaveWithoutTransaction() throws Exception {
		Page page = Trx.supply(() -> createPage(node.getFolder(), template, "Page"));

		com.gentics.contentnode.rest.model.Page restPage = new com.gentics.contentnode.rest.model.Page();
		restPage.setId(page.getId());
		restPage.setName("Modified name");

		assertThat(TransactionManager.getCurrentTransactionOrNull()).as("Current transaction").isNull();
		GenericResponse response = new PageResourceImpl().save(Integer.toString(page.getId()), new PageSaveRequest(restPage));
		assertResponseOK(response);

		String name = Trx.supply(t -> t.getObject(Page.class, page.getId()).getName());
		assertThat(name).as("Page name after saving").isEqualTo("Modified name");
	}

	/**
	 * Test that saving a page, which does not exist, throws an EntityNotFoundException
	 */
	@Test
	public void testSaveNotExistingPage() {
		Throwable thrown = catchThrowable(
				() -> new PageResourceImpl().save(NOT_EXISTING_PAGE_ID, new PageSaveRequest(new com.gentics.contentnode.rest.model.Page())));
		assertThat(thrown).as("Thrown exception").isInstanceOf(EntityNotFoundException.class);
	}

	/**
	 * Test that publishing a page, which does not exist, throws an EntityNotFoundException
	 */
	@Test
	public void testPublishNotExistingPage() {
		Throwable thrown = catchThrowable(() -> new PageResourceImpl().publish(NOT_EXISTING_PAGE_ID, null, new PagePublishRequest()));
		assertThat(thrown).as("Thrown exception").isInstanceOf(EntityNotFoundException.class);
	}

	/**
	 * Test that restoring a version of a page, which does not exist, throws an EntityNotFoundException
	 */
	@Test
	public void testRestoreVersionNotExistingPage() {
		Throwable thrown = catchThrowable(() -> new PageResourceImpl().restoreVersion(NOT_EXISTING_PAGE_ID, 1));
		assertThat(thrown).as("Thrown exception").isInstanceOf(EntityNotFoundException.class);
	}
}
