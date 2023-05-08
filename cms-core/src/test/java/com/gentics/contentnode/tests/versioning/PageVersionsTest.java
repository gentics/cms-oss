package com.gentics.contentnode.tests.versioning;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for page versions
 */
public class PageVersionsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Construct construct;
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());

		construct = create(Construct.class, c -> {
			c.setName("Test construct", 1);
			c.setIconName("icon.jpg");
			c.setKeyword("testconstruct");

			List<Part> parts = c.getParts();
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("textpart");
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
			}).doNotSave().build());
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("dspart");
				p.setPartTypeId(getPartTypeId(DatasourcePartType.class));
			}).doNotSave().build());

			c.getNodes().add(node);
		}).build();

		template = create(Template.class, t -> {
			t.setMlId(1);
			t.setName("Template");
			t.setSource("");

			t.getNodes().add(node);
		}).build();
	}

	/**
	 * Test that removing a tag also removes the tag from the page version
	 * @throws NodeException
	 */
	@Test
	public void testRemoveTag() throws NodeException {
		// create a page containing two tags
		Page page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Test page");

			Map<String, ContentTag> tags = p.getContent().getContentTags();
			tags.put("tag1", create(ContentTag.class, c -> {
				c.setConstructId(construct.getId());
				c.setName("tag1");
				getPartType(LongHTMLPartType.class, c, "textpart").setText("Content of tag1");
				List<DatasourceEntry> items = getPartType(DatasourcePartType.class, c, "dspart").getItems();
				items.add(create(DatasourceEntry.class, ds -> {
					ds.setDsid(1);
					ds.setKey("key1");
					ds.setValue("value1");
				}).doNotSave().build());
			}).doNotSave().build());
			tags.put("tag2", create(ContentTag.class, c -> {
				c.setConstructId(construct.getId());
				c.setName("tag2");
				getPartType(LongHTMLPartType.class, c, "textpart").setText("Content of tag2");
				List<DatasourceEntry> items = getPartType(DatasourcePartType.class, c, "dspart").getItems();
				items.add(create(DatasourceEntry.class, ds -> {
					ds.setDsid(1);
					ds.setKey("key2");
					ds.setValue("value2");
				}).doNotSave().build());
			}).doNotSave().build());
		}).at(1).build();

		// update page by removing one of the tags
		page = update(page, p -> {
			p.getContent().getContentTags().remove("tag2");
		}).at(2).build();

		// load the page version @1
		Page pageVersion1 = execute(p -> TransactionManager.getCurrentTransaction().getObject(Page.class, p.getId(), 1), page);
		consume(p -> assertThat(p.getContent().getContentTags()).as("Content Tags of version 1").containsOnlyKeys("tag1", "tag2"), pageVersion1);

		// load the page version @2
		Page pageVersion2 = execute(p -> TransactionManager.getCurrentTransaction().getObject(Page.class, p.getId(), 2), page);
		consume(p -> assertThat(p.getContent().getContentTags()).as("Content Tags of version 2").containsOnlyKeys("tag1"), pageVersion2);
	}
}
