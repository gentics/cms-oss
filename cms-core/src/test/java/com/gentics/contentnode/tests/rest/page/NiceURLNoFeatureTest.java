package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;

public class NiceURLNoFeatureTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Test template
	 */
	private static Template template;

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Test creating a page with a nice URL
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		Page page = Trx.supply(() -> create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			p.setName("Testpage");
			p.setNiceUrl("/feature/not/activated");
		}));

		assertThat(page.getNiceUrl()).as("Nice URL").isNull();
	}

	/**
	 * Test updating a page with a nice URL
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws NodeException {
		Page page = Trx.supply(() -> createPage(node.getFolder(), template, "Testpage"));

		page = update(page, p -> {
			p.setNiceUrl("/feature/not/activated");
		});

		assertThat(page.getNiceUrl()).as("Nice URL").isNull();
	}
}
