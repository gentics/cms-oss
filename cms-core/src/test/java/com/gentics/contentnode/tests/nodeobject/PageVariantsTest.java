package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for page variants
 */
public class PageVariantsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Test that publishing one page variant updates the pdate of other (published) page variants
	 * @throws NodeException
	 */
	@Test
	public void testRepublish() throws NodeException {
		int createdTimestamp = 1;
		int republishTimestamp = 2;
		Pair<Page, Page> variants = createVariants(createdTimestamp);
		Page firstVariant = variants.getLeft();
		Page secondVariant = variants.getRight();

		assertThat(execute(Page::getPDate, firstVariant).getIntTimestamp()).as("Pdate for first page").isEqualTo(createdTimestamp);
		assertThat(execute(Page::getPDate, secondVariant).getIntTimestamp()).as("Pdate for second page").isEqualTo(createdTimestamp);

		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(republishTimestamp * 1000L);
			update(firstVariant, p -> {
				p.setName("Modified" + p.getName());
			});
			trx.success();
		}
		firstVariant = execute(Page::reload, firstVariant);
		secondVariant = execute(Page::reload, secondVariant);

		assertThat(execute(Page::getPDate, firstVariant).getIntTimestamp()).as("Pdate for first page").isEqualTo(republishTimestamp);
		assertThat(execute(Page::getPDate, secondVariant).getIntTimestamp()).as("Pdate for second page").isEqualTo(republishTimestamp);
	}

	/**
	 * Test that publishing one page variant does not update the pdate of modified page variants
	 * @throws NodeException
	 */
	@Test
	public void testRepublishModified() throws NodeException {
		int createTimestamp = 1;
		int updateTimestamp = 2;
		int republishTimestamp = 3;
		Pair<Page, Page> variants = createVariants(createTimestamp);
		Page firstVariant = variants.getLeft();
		Page secondVariant = variants.getRight();

		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(updateTimestamp * 1000L);

			Page forUpdate = trx.getTransaction().getObject(secondVariant, true);
			forUpdate.setName("Modified " + forUpdate.getName());
			forUpdate.save();

			trx.success();
		}

		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(republishTimestamp * 1000L);
			update(firstVariant, p -> {
				p.setName("Modified" + p.getName());
			});
			trx.success();
		}
		firstVariant = execute(Page::reload, firstVariant);
		secondVariant = execute(Page::reload, secondVariant);

		assertThat(execute(Page::getPDate, firstVariant).getIntTimestamp()).as("Pdate for first page").isEqualTo(republishTimestamp);
		assertThat(execute(Page::getPDate, secondVariant).getIntTimestamp()).as("Pdate for second page").isEqualTo(createTimestamp);
	}

	/**
	 * Create a pair of published page variants
	 * @param timestamp transaction timestamp
	 * @return pair of pages
	 * @throws NodeException
	 */
	protected Pair<Page, Page> createVariants(int timestamp) throws NodeException {
		Page firstVariant = null;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			firstVariant = create(Page.class, create -> {
				create.setFolderId(node.getFolder().getId());
				create.setTemplateId(template.getId());
				create.setName("First Variant");
			});
			trx.success();
		}
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			update(firstVariant, Page::publish);
			trx.success();
		}
		firstVariant = execute(Page::reload, firstVariant);

		Page secondVariant = null;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			secondVariant = firstVariant.createVariant();
			secondVariant.save();
			secondVariant = secondVariant.reload();
			trx.success();
		}
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			update(secondVariant, Page::publish);
			trx.success();
		}
		secondVariant = execute(Page::reload, secondVariant);

		return Pair.of(firstVariant, secondVariant);
	}
}
