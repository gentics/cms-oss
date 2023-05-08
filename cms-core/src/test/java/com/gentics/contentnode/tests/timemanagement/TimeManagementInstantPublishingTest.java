package com.gentics.contentnode.tests.timemanagement;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for time management and instant publishing
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = Feature.INSTANT_CR_PUBLISHING)
public class TimeManagementInstantPublishingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("hostname", "Name", PublishTarget.CONTENTREPOSITORY, false, true));

		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		// publish (to sync the attributes with the cr)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}
	}

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: update {0}, in future {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean update : Arrays.asList(false, true)) {
			for (boolean future : Arrays.asList(false, true)) {
				data.add(new Object[] { update, future });
			}
		}
		return data;
	}

	@Parameter(0)
	public boolean update;

	@Parameter(1)
	public boolean future;

	@Test
	public void publishAt() throws NodeException {
		int createTime = (int) (System.currentTimeMillis() / 1000L);
		int updateTime = createTime + 1000;
		int publishAt = future ? updateTime + 86400 : updateTime - 86400;

		// create page
		Page page = null;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(createTime * 1000L);
			page = createPage(node.getFolder(), template, "Page");
			// if testing updates, publish the page now
			if (update) {
				page = update(page, Page::publish);
			}
			trx.success();
		}

		String originalName = execute(Page::getName, page);
		// when page was published, it should be in the CR now
		consume(p -> assertPublishCR(p, node, update), page);

		String updatedName = null;
		if (update) {
			try (Trx trx = new Trx()) {
				trx.getTransaction().setTimestamp(updateTime * 1000L);
				Page editable = trx.getTransaction().getObject(page, true);
				editable.setName("Modified");
				editable.save();
				page = page.reload();
				trx.success();
			}
			updatedName = execute(Page::getName, page);
		}

		// publish at
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(updateTime * 1000L);
			PagePublishRequest request = new PagePublishRequest();
			request.setAt(publishAt);
			GenericResponse response = new PageResourceImpl().publish(String.valueOf(page.getId()), null, request);
			assertResponseCodeOk(response);
			trx.success();
		}

		// we expect the page to be published, if we test update or creating with timestamp in the past
		boolean expected = update || !future;
		String expectedName = update && !future ? updatedName : originalName;

		consume(p -> assertPublishCR(p, node, expected, publishedPage -> {
			assertThat(publishedPage.get("name")).as("Published page name").isEqualTo(expectedName);
		}), page);
	}

	@Test
	public void offlineAt() throws NodeException {
		int createTime = (int) (System.currentTimeMillis() / 1000L);
		int updateTime = createTime + 1000;
		int offlineAt = future ? updateTime + 86400 : updateTime - 86400;

		// create page
		Page page = null;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(createTime * 1000L);
			page = update(createPage(node.getFolder(), template, "Page"), Page::publish);
			trx.success();
		}

		String originalName = execute(Page::getName, page);
		consume(p -> assertPublishCR(p, node, true), page);

		if (update) {
			try (Trx trx = new Trx()) {
				trx.getTransaction().setTimestamp(updateTime * 1000L);
				Page editable = trx.getTransaction().getObject(page, true);
				editable.setName("Modified");
				editable.save();
				page = page.reload();
				trx.success();
			}
		}

		// offline at
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(updateTime * 1000L);
			PageOfflineRequest request = new PageOfflineRequest();
			request.setAt(offlineAt);
			GenericResponse response = new PageResourceImpl().takeOffline(String.valueOf(page.getId()), request);
			assertResponseCodeOk(response);
			trx.success();
		}

		// we expect the page to still be published, because in the current implementation, when doing offline at, the timemanagement is not handled immediately
		boolean expected = true;

		consume(p -> assertPublishCR(p, node, expected, publishedPage -> {
			assertThat(publishedPage.get("name")).as("Published page name").isEqualTo(originalName);
		}), page);
	}
}
