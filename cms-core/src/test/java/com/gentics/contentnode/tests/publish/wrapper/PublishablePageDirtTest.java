package com.gentics.contentnode.tests.publish.wrapper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for dirting the PublishCache of pages
 */
public class PublishablePageDirtTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	public final static String CONSTRUCT_KEYWORD = "text";

	public final static String PART_KEYWORD = "text";

	public final static String OE_NAME = "test";

	@Before
	public void setupOnce() throws Exception {
		TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().setFeature(Feature.PUBLISH_CACHE.toString().toLowerCase(), true);
	}

	/**
	 * Test whether republishing of a changed Page OE works
	 * @throws Exception
	 */
	@Test
	public void testChangeOE() throws Exception {
		Node node = null;
		int constructId = 0;
		Template template = null;
		Page page = null;
		ObjectTag objectTag = null;

		// create the initial test data
		try (Trx trx = new Trx(null, 1)) {
			node = ContentNodeTestDataUtils.createNode();
			constructId = ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, CONSTRUCT_KEYWORD, PART_KEYWORD);
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, "Test", OE_NAME);
			template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "OE: {<node object." + OE_NAME + ">]", "OE Template");
			page = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page");

			page = TransactionManager.getCurrentTransaction().getObject(Page.class, page.getId(), true);
			objectTag = page.getObjectTag(OE_NAME);
			assertNotNull("Object Tag must exist", objectTag);
			objectTag.getValues().getByKeyname(PART_KEYWORD).setValueText("Initial Value");
			objectTag.setEnabled(true);
			page.save();
			page.publish();

			page = TransactionManager.getCurrentTransaction().getObject(Page.class, page.getId());

			trx.success();
		}

		// run publish process
		try (Trx trx = new Trx(null, 1)) {
			testContext.publish(false);
			trx.success();
		}

		// check initial published page
		try (Trx trx = new Trx(null, 1)) {
			ContentNodeTestUtils.assertPublishedPageContent(node, page, "OE: {Initial Value]");
			trx.success();
		}

		// modify the OE of the page
		try (Trx trx = new Trx(null, 1)) {
			objectTag = TransactionManager.getCurrentTransaction().getObject(ObjectTag.class, objectTag.getId(), true);
			objectTag.getValues().getByKeyname(PART_KEYWORD).setValueText("Modified Value");
			objectTag.save();
			objectTag = TransactionManager.getCurrentTransaction().getObject(ObjectTag.class, objectTag.getId());
			trx.success();
		}

		// wait for dirtqueue worker
		testContext.waitForDirtqueueWorker();

		// check dirted objects
		try (Trx trx = new Trx(null, 1)) {
			List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, null);
			assertTrue(page + " must be dirted", dirtedPageIds.contains(page.getId()));
		}

		// run publish process
		try (Trx trx = new Trx(null, 1)) {
			testContext.publish(false);
			trx.success();
		}

		// check initial published page
		try (Trx trx = new Trx(null, 1)) {
			ContentNodeTestUtils.assertPublishedPageContent(node, page, "OE: {Modified Value]");
			trx.success();
		}
	}

	/**
	 * Test whether republishing of changed page variants works
	 * @throws Exception
	 */
	@Test
	public void testPageVariants() throws Exception {
		Node node = null;
		int constructId = 0;
		Template template = null;
		Page page = null;
		Page variant = null;

		// create the initial test data
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			node = ContentNodeTestDataUtils.createNode();
			constructId = ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, CONSTRUCT_KEYWORD, PART_KEYWORD);
			template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Content: {<node content>}", "OE Template");
			template = t.getObject(Template.class, template.getId(), true);
			TemplateTag tTag = t.createObject(TemplateTag.class);
			tTag.setConstructId(constructId);
			tTag.setEnabled(true);
			tTag.setPublic(true);
			tTag.setName("content");
			template.getTemplateTags().put(tTag.getName(), tTag);
			template.save();

			page = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page");
			page = t.getObject(Page.class, page.getId(), true);
			page.getContentTag("content").getValues().getByKeyname(PART_KEYWORD).setValueText("This is the content");
			page.save();
			page.publish();

			variant = page.createVariant();
			variant.save();
			variant.publish();

			trx.success();
		}

		// run publish process
		try (Trx trx = new Trx(null, 1)) {
			testContext.publish(false);
			trx.success();
		}

		// check initial published page
		try (Trx trx = new Trx(null, 1)) {
			ContentNodeTestUtils.assertPublishedPageContent(node, page, "Content: {This is the content}");
			ContentNodeTestUtils.assertPublishedPageContent(node, variant, "Content: {This is the content}");
			trx.success();
		}

		// modify one page
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			page = t.getObject(Page.class, page.getId(), true);
			page.getContentTag("content").getValues().getByKeyname(PART_KEYWORD).setValueText("Modified");
			page.save();
			page.publish();

			trx.success();
		}

		// wait for dirtqueue worker
		testContext.waitForDirtqueueWorker();

		// check dirted objects
		try (Trx trx = new Trx(null, 1)) {
			List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, null);
			assertTrue(page + " must be dirted", dirtedPageIds.contains(page.getId()));
			assertTrue(variant + " must be dirted", dirtedPageIds.contains(variant.getId()));
		}

		// run publish process
		try (Trx trx = new Trx(null, 1)) {
			testContext.publish(false);
			trx.success();
		}

		// check initial published page
		try (Trx trx = new Trx(null, 1)) {
			ContentNodeTestUtils.assertPublishedPageContent(node, page, "Content: {Modified}");
			ContentNodeTestUtils.assertPublishedPageContent(node, variant, "Content: {Modified}");
			trx.success();
		}

	}
}
