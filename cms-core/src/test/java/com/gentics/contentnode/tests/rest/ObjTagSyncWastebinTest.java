package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for synchronized object tags and the wastebin
 */
@GCNFeature(set = { Feature.WASTEBIN, Feature.OBJTAG_SYNC })
public class ObjTagSyncWastebinTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static int constructId;

	private static ObjectTagDefinition objectTagDefinition;

	private static Template template;

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode("host", "Test Node", PublishTarget.NONE));

		constructId = supply(() -> createConstruct(node, LongHTMLPartType.class, "text", "text"));

		// create an object tag definition, which is synchronized over language variants and page variants
		objectTagDefinition = supply(
				() -> createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, "synchronized", "synchronized"));
		update(objectTagDefinition, update -> {
			update.setSyncContentset(true);
			update.setSyncVariants(true);
		}).build();

		template = supply(() -> createTemplate(node.getFolder(), "Test Template"));
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> clear(node));
	}

	@Test
	public void testLoadPageWithVariantInWastebin() throws NodeException {
		// create Folder A
		Folder folderA = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder A");
			create.setPublishDir("/");
		}).build();

		// create page in Folder A
		Page pageA = create(Page.class, create -> {
			create.setTemplateId(template.getId());
			create.setFolder(node, folderA);
		}).build();

		// create Folder B
		Folder folderB = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder B");
			create.setPublishDir("/");
		}).build();

		// create page in Folder B as page variant of pageA
		Page pageB = create(Page.class, create -> {
			create.setTemplateId(template.getId());
			create.setFolder(node, folderB);
			create.setContentId(pageA.getContent().getId());
		}).build();

		// delete folder A (which will delete page A also)
		update(folderA, update -> {
			update.delete();
		}).build();

		// now we get the REST Model of pageB with object tags, this will check
		// permission on the object tag which would fail, if the deleted page in the
		// deleted folder would be considered
		execute(p -> ModelBuilder.getPage(p, Arrays.asList(Reference.OBJECT_TAGS)), pageB);
	}

	@Test
	public void testUpdatePageWithVariantInWastebin() throws NodeException {
		// create Folder A
		Folder folderA = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder A");
			create.setPublishDir("/");
		}).build();

		// create page in Folder A
		Page pageA = create(Page.class, create -> {
			create.setTemplateId(template.getId());
			create.setFolder(node, folderA);
		}).build();

		// create Folder B
		Folder folderB = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder B");
			create.setPublishDir("/");
		}).build();

		// create page in Folder B as page variant of pageA
		Page pageB = create(Page.class, create -> {
			create.setTemplateId(template.getId());
			create.setFolder(node, folderB);
			create.setContentId(pageA.getContent().getId());
		}).build();

		// delete folder A (which will delete page A also)
		update(folderA, update -> {
			update.delete();
		}).build();

		// update pageB by setting the synchronized object tag
		pageB = update(pageB, update -> {
			getPartType(LongHTMLPartType.class, update.getObjectTag("synchronized"), "text").setText("This is the value to be synchronized");
		}).build();

		// restore folderA and pageA from wastebin
		consume(p -> {
			try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				p.reload().restore();
			}
		}, pageA);

		// check that the object tag of the restored page is now synchronized
		String tagValueOfRestoredPage = execute(p -> {
			p = p.reload();
			return getPartType(LongHTMLPartType.class, p.getObjectTag("synchronized"), "text").getText();
		}, pageA);
		assertThat(tagValueOfRestoredPage).as("Tag value of restored page").isEqualTo("This is the value to be synchronized");
	}

	@Test
	public void testGetSyncInfoWithVariantInWastebin() throws NodeException {
		// create Folder A
		Folder folderA = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder A");
			create.setPublishDir("/");
		}).build();

		// create page in Folder A
		Page pageA = create(Page.class, create -> {
			create.setTemplateId(template.getId());
			create.setFolder(node, folderA);
		}).build();

		int pageAObjectTagId = execute(p -> p.getObjectTag("synchronized").getId(), pageA);

		// create Folder B
		Folder folderB = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder B");
			create.setPublishDir("/");
		}).build();

		// create page in Folder B as page variant of pageA
		Page pageB = create(Page.class, create -> {
			create.setTemplateId(template.getId());
			create.setFolder(node, folderB);
			create.setContentId(pageA.getContent().getId());
		}).build();

		int pageBObjectTagId = execute(p -> p.getObjectTag("synchronized").getId(), pageB);

		// delete folder A (which will delete page A also)
		update(folderA, update -> {
			update.delete();
		}).build();

		Set<Integer> checkedIds = execute(p -> p.getObjectTag("synchronized").checkSync(), pageB);
		assertThat(checkedIds).as("Checked object tags").containsOnly(pageBObjectTagId);

		// restore pageA
		consume(p -> {
			try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				p.reload().restore();
			}
		}, pageA);

		checkedIds = execute(p -> p.getObjectTag("synchronized").checkSync(), pageB);
		assertThat(checkedIds).as("Checked object tags").containsOnly(pageAObjectTagId, pageBObjectTagId);
	}
}
