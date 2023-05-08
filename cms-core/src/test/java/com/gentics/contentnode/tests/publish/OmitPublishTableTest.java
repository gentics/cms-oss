package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.IntegerColumnRetriever;

/**
 * Test for the feature {@link Feature#OMIT_PUBLISH_TABLE}.
 * If the feature is on, pages are only written into the publish table, if they are published into the filesystem
 */
@RunWith(value = Parameterized.class)
public class OmitPublishTableTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * true for multithreaded publishing
	 */
	protected boolean multithreaded;

	/**
	 * true if the tested feature is on
	 */
	protected boolean omitPublishTable;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: multithreaded {0}, omit {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean multithreaded : Arrays.asList(true, false)) {
			for (boolean omitPublishTable : Arrays.asList(true, false)) {
					data.add(new Object[] {multithreaded, omitPublishTable});
			}
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param multithreaded true for multithreaded publishing
	 * @param omitPublishTable true for omitPublishTable feature
	 */
	public OmitPublishTableTest(boolean multithreaded, boolean omitPublishTable) {
		this.multithreaded = multithreaded;
		this.omitPublishTable = omitPublishTable;
	}

	@Before
	public void setUp() throws Exception {
		DBUtils.executeUpdate("UPDATE node SET disable_publish = ?", new Object[] {1});
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTITHREADED_PUBLISHING, multithreaded);
		prefs.setFeature(Feature.OMIT_PUBLISH_TABLE, omitPublishTable);
	}

	/**
	 * Test the publish process
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		testContext.startTransaction(1);
		Transaction t = TransactionManager.getCurrentTransaction();

		Node FSNode = ContentNodeTestDataUtils.createNode("Filesystem", "filesystem", "/Content.Node", null, true, false);
		Node nonFSNode = ContentNodeTestDataUtils.createNode("Non Filesystem", "nonfilesystem", "/Content.Node", null, false, false);

		Template template = t.createObject(Template.class);
		template.setMlId(1);
		template.setName("Template");
		template.setSource("The page is <node page.name>");
		template.addFolder(FSNode.getFolder());
		template.addFolder(nonFSNode.getFolder());
		template.save();
		t.commit(false);

		Page pageFS = t.createObject(Page.class);
		pageFS.setFolderId(FSNode.getFolder().getId());
		pageFS.setTemplateId(template.getId());
		pageFS.save();
		pageFS.publish();
		t.commit(false);

		Page pageNonFS = t.createObject(Page.class);
		pageNonFS.setFolderId(nonFSNode.getFolder().getId());
		pageNonFS.setTemplateId(template.getId());
		pageNonFS.save();
		pageNonFS.publish();
		t.commit(false);

		testContext.publish(2);

		testContext.startTransaction(3);

		// check publish table
		IntegerColumnRetriever pageIds = new IntegerColumnRetriever("page_id");
		DBUtils.executeStatement("SELECT page_id FROM publish", pageIds);

		assertTrue(pageFS + " must be written into publish table", pageIds.getValues().contains(pageFS.getId()));
		if (omitPublishTable) {
			assertFalse(pageNonFS + " must not be written into publish table", pageIds.getValues().contains(pageNonFS.getId()));
		} else {
			assertTrue(pageNonFS + " must be written into publish table", pageIds.getValues().contains(pageNonFS.getId()));
		}
	}

	/**
	 * Test whether dirting of a dependent page works as expected.
	 * The source page renders the tag of the target page, and the tag in the target page will be modified
	 * @throws Exception
	 */
	@Test
	public void testDirtingDependency() throws Exception {
		int createTS = 1;
		int publishTS = 2;
		int changeTS = 3;
		int checkDirted = 4;

		Transaction t = testContext.startTransaction(createTS);

		// create a node for testing
		Node node = ContentNodeTestDataUtils.createNode("testnode", "Test Node", PublishTarget.CONTENTREPOSITORY);

		// create constructs
		int htmlConstructId = ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, "html", "part");
		int pageTagConstructId = ContentNodeTestDataUtils.createConstruct(node, PageTagPartType.class, "pagetag", "part");

		// create the template
		Template template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "<node content>", "Template");
		template = t.getObject(Template.class, template.getId(), true);
		TemplateTag templateTag = t.createObject(TemplateTag.class);
		templateTag.setConstructId(htmlConstructId);
		templateTag.setEnabled(true);
		templateTag.setName("content");
		templateTag.setPublic(true);
		template.getTemplateTags().put("content", templateTag);
		template.save();
		t.commit(false);
		template = t.getObject(Template.class, template.getId());

		// create the source page
		Page sourcePage = t.createObject(Page.class);
		sourcePage.setFolderId(node.getFolder().getId());
		sourcePage.setTemplateId(template.getId());
		sourcePage.setName("Source Page");
		sourcePage.getContentTag("content").getValues().getByKeyname("part").setValueText("Source Content");
		sourcePage.save();
		sourcePage.publish();
		t.commit(false);

		// create the target page, that renders a tag from the source page
		Page targetPage = t.createObject(Page.class);
		targetPage.setFolderId(node.getFolder().getId());
		targetPage.setTemplateId(template.getId());
		targetPage.setName("Target Page");
		ContentTag pageTagTag = targetPage.getContent().addContentTag(pageTagConstructId);
		ContentNodeTestDataUtils.getPartType(PageTagPartType.class, pageTagTag, "part").setPageTag(sourcePage, sourcePage.getContentTag("content"));
		targetPage.getContentTag("content").getValues().getByKeyname("part").setValueText("<node " + pageTagTag.getName() + ">");
		targetPage.save();
		targetPage.publish();
		t.commit(false);

		// publish the pages, which should set up the dependencies
		testContext.publish(publishTS);

		// now change the tag of the source page
		t = testContext.startTransaction(changeTS);

		sourcePage = t.getObject(Page.class, sourcePage.getId(), true);
		sourcePage.getContentTag("content").getValues().getByKeyname("part").setValueText("Modified content");
		sourcePage.save();
		sourcePage.publish();
		t.commit(false);

		// wait until the dirting has been done
		testContext.waitForDirtqueueWorker();

		// check whether the target page has been dirted
		t = testContext.startTransaction(checkDirted);

		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, node);
		assertTrue("Source Page must be dirted", dirtedPageIds.contains(sourcePage.getId()));
		assertTrue("Target Page must be dirted", dirtedPageIds.contains(targetPage.getId()));
	}

	/**
	 * Test whether a non-dependency is not dirted.
	 * The source page renders the tag of the target page, and the name of the target page will be modified
	 * @throws Exception
	 */
	@Test
	public void testDirtingNonDependency() throws Exception {
		int createTS = 1;
		int publishTS = 2;
		int changeTS = 3;
		int checkDirted = 4;

		Transaction t = testContext.startTransaction(createTS);

		// create a node for testing
		Node node = ContentNodeTestDataUtils.createNode("testnode", "Test Node", PublishTarget.CONTENTREPOSITORY);

		// create constructs
		int htmlConstructId = ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, "html", "part");
		int pageTagConstructId = ContentNodeTestDataUtils.createConstruct(node, PageTagPartType.class, "pagetag", "part");

		// create the template
		Template template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "<node content>", "Template");
		template = t.getObject(Template.class, template.getId(), true);
		TemplateTag templateTag = t.createObject(TemplateTag.class);
		templateTag.setConstructId(htmlConstructId);
		templateTag.setEnabled(true);
		templateTag.setName("content");
		templateTag.setPublic(true);
		template.getTemplateTags().put("content", templateTag);
		template.save();
		t.commit(false);
		template = t.getObject(Template.class, template.getId());

		// create the source page
		Page sourcePage = t.createObject(Page.class);
		sourcePage.setFolderId(node.getFolder().getId());
		sourcePage.setTemplateId(template.getId());
		sourcePage.setName("Source Page");
		sourcePage.getContentTag("content").getValues().getByKeyname("part").setValueText("Source Content");
		sourcePage.save();
		sourcePage.publish();
		t.commit(false);

		// create the target page, that renders a tag from the source page
		Page targetPage = t.createObject(Page.class);
		targetPage.setFolderId(node.getFolder().getId());
		targetPage.setTemplateId(template.getId());
		targetPage.setName("Target Page");
		ContentTag pageTagTag = targetPage.getContent().addContentTag(pageTagConstructId);
		ContentNodeTestDataUtils.getPartType(PageTagPartType.class, pageTagTag, "part").setPageTag(sourcePage, sourcePage.getContentTag("content"));
		targetPage.getContentTag("content").getValues().getByKeyname("part").setValueText("<node " + pageTagTag.getName() + ">");
		targetPage.save();
		targetPage.publish();
		t.commit(false);

		// publish the pages, which should set up the dependencies
		testContext.publish(publishTS);

		// now change the name of the source page
		t = testContext.startTransaction(changeTS);

		sourcePage = t.getObject(Page.class, sourcePage.getId(), true);
		sourcePage.setName("Modified source page");
		sourcePage.save();
		sourcePage.publish();
		t.commit(false);

		// wait until the dirting has been done
		testContext.waitForDirtqueueWorker();

		// check whether the target page has been dirted
		t = testContext.startTransaction(checkDirted);

		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, node);
		assertTrue("Source Page must be dirted", dirtedPageIds.contains(sourcePage.getId()));
		assertFalse("Target Page must not be dirted", dirtedPageIds.contains(targetPage.getId()));
	}
}
