package com.gentics.contentnode.tests.dirting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;

public class AbstractPageDirtingTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	protected static final String TEXT_PARTNAME = "text";

	protected static final String CONTENT_TAGNAME = "content";

	protected static final String URL_PARTNAME = "url";

	protected static final String VTL_PARTNAME = "vtl";

	protected static final String TEMPLATE_PARTNAME = "template";

	protected static final String OVERVIEW_PARTNAME = "ds";

	/**
	 * Name of the pagetag tag
	 */
	protected static final String PAGETAG_PARTNAME = "pagetag";

	/**
	 * Timestamp for initial creation of test data
	 */
	protected final static int creationTime = 1;

	/**
	 * Timestamp for initial publish process
	 */
	protected final static int initialPublishTime = 2;

	/**
	 * Timestamp for the transaction at test start
	 */
	protected final static int testStartTime = 3;

	/**
	 * Node holding the tests
	 */
	protected Node node;

	/**
	 * Target page ID
	 */
	protected int targetPageId;

	/**
	 * ID of the page containing a page url tag
	 */
	protected int urlPageId;

	/**
	 * ID of the page containing a pagetag tag
	 */
	protected int pagetagPageId;

	/**
	 * ID of the page rendering a vtl list
	 */
	protected int vtlListPage;

	/**
	 * ID of the page rendering another page after loading it
	 */
	protected int vtlLoaderPage;

	/**
	 * ID of the page rendering the publishdate of another page after loading it
	 */
	protected int vtlPdatePage;

	/**
	 * ID of the page rendering a manual page overview
	 */
	protected int manualOverviewPage;

	/**
	 * ID of the page rendering an overview over pages in a folder
	 */
	protected int folderOverviewPage;

	/**
	 * List of dependent pages
	 */
	protected List<Integer> dependentPages;

	/**
	 * List of pages dependent on the publishdate
	 */
	protected List<Integer> pdateDependentPages;

	@Before
	public void setUp() throws Exception {

		testContext.startTransaction(creationTime);

		Transaction t = TransactionManager.getCurrentTransaction();

		// disable publishing for all nodes
		DBUtils.executeUpdate("UPDATE node SET disable_publish = ?", new Object[] { 1 });

		// create a node
		node = ContentNodeTestDataUtils.createNode("Test Node", "testnode", "/Content.Node", null, false, false);

		Folder targetFolder = t.createObject(Folder.class);
		targetFolder.setMotherId(node.getFolder().getId());
		targetFolder.setName("Target Folder");
		targetFolder.setPublishDir("/");
		targetFolder.save();
		t.commit(false);

		// create a test folder
		Folder folder = t.createObject(Folder.class);
		folder.setMotherId(node.getFolder().getId());
		folder.setName("Test Folder");
		folder.setPublishDir("/");
		folder.save();
		t.commit(false);

		// create the template
		Template template = t.createObject(Template.class);
		template.getFolders().add(folder);
		template.getFolders().add(targetFolder);
		template.setMlId(1);
		template.setName("Template");
		template.setSource("<node page.name>: [<node " + CONTENT_TAGNAME + ">]");

		TemplateTag tTag = t.createObject(TemplateTag.class);
		tTag.setConstructId(ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, TEXT_PARTNAME, TEXT_PARTNAME));
		tTag.setEnabled(true);
		tTag.setName(CONTENT_TAGNAME);
		tTag.setPublic(true);

		template.getTemplateTags().put(CONTENT_TAGNAME, tTag);
		template.save();
		t.commit(false);

		targetPageId = createTargetPage(targetFolder, template);
		Page targetPage = t.getObject(Page.class, targetPageId);

		int vtlConstructId = ContentNodeTestDataUtils.createVelocityConstruct(node, VTL_PARTNAME, VTL_PARTNAME);
		int overviewConstructId = ContentNodeTestDataUtils.createConstruct(node, OverviewPartType.class, OVERVIEW_PARTNAME, OVERVIEW_PARTNAME);

		Construct c = t.getObject(Construct.class, overviewConstructId, true);
		c.getValues().getByKeyname(OVERVIEW_PARTNAME).setInfo(1);
		c.save();
		t.commit(false);

		urlPageId = createUrlPage(folder, template, targetPage);
		pagetagPageId = createPageTagPage(folder, template, targetPage);
		vtlListPage = createVtlListPage(folder, template, targetPage, vtlConstructId);
		vtlLoaderPage = createVtlLoaderPage(folder, template, targetPage, vtlConstructId);
		vtlPdatePage = createVtlPdatePage(folder, template, targetPage, vtlConstructId);
		manualOverviewPage = createManualOverviewPage(folder, template, targetPage, overviewConstructId);
		folderOverviewPage = createFolderOverviewPage(folder, template, targetPage, overviewConstructId);

		// Note: the page rendering the page tag is not dependent on the
		// targetpage being online
		dependentPages = Arrays.asList(urlPageId, vtlListPage, vtlLoaderPage, vtlPdatePage, manualOverviewPage, folderOverviewPage);
		pdateDependentPages = Arrays.asList(vtlPdatePage);

		// run the publish process
		testContext.publish(initialPublishTime);

		// start a new transaction
		testContext.startTransaction(testStartTime);
	}

	/**
	 * Create the target page
	 *
	 * @param folder
	 *            folder
	 * @param template
	 *            template
	 * @return id of the target page
	 * @throws NodeException
	 */
	protected int createTargetPage(Folder folder, Template template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setName("Target page");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		page.getContentTag(CONTENT_TAGNAME).getValues().getByKeyname(TEXT_PARTNAME).setValueText("Target content");
		page.save();
		t.commit(false);
		return ObjectTransformer.getInt(page.getId(), 0);
	}

	/**
	 * Create the url page
	 *
	 * @param folder
	 *            folder
	 * @param template
	 *            template
	 * @param targetPage
	 *            target page
	 * @return id of the url page
	 * @throws NodeException
	 */
	protected int createUrlPage(Folder folder, Template template, Page targetPage) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setName("Url page");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		ContentTag tag = page.getContent().addContentTag(ContentNodeTestDataUtils.createConstruct(folder.getNode(), PageURLPartType.class, URL_PARTNAME, URL_PARTNAME));
		ContentNodeTestDataUtils.getPartType(PageURLPartType.class, tag, URL_PARTNAME).setTargetPage(targetPage);
		page.getContentTag(CONTENT_TAGNAME).getValues().getByKeyname(TEXT_PARTNAME).setValueText("<node " + tag.getName() + ">");
		page.save();
		page.publish();
		t.commit(false);
		return ObjectTransformer.getInt(page.getId(), 0);
	}

	/**
	 * Create the pagetag page
	 *
	 * @param folder
	 *            folder
	 * @param template
	 *            template
	 * @param targetPage
	 *            target page
	 * @return id of the pagetag page
	 * @throws NodeException
	 */
	protected int createPageTagPage(Folder folder, Template template, Page targetPage) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setName("Pagetag page");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		ContentTag tag = page.getContent().addContentTag(ContentNodeTestDataUtils.createConstruct(folder.getNode(), PageTagPartType.class, PAGETAG_PARTNAME, PAGETAG_PARTNAME));
		ContentNodeTestDataUtils.getPartType(PageTagPartType.class, tag, PAGETAG_PARTNAME).setPageTag(targetPage, targetPage.getTag(CONTENT_TAGNAME));
		page.getContentTag(CONTENT_TAGNAME).getValues().getByKeyname(TEXT_PARTNAME).setValueText("<node " + tag.getName() + ">");
		page.save();
		page.publish();
		t.commit(false);
		return ObjectTransformer.getInt(page.getId(), 0);
	}

	/**
	 * Create test page that renders a velocity list of pages
	 *
	 * @param folder
	 *            folder
	 * @param template
	 *            template
	 * @param targetPage
	 *            target page
	 * @param vtlConstructId
	 *            velocity construct id
	 * @return test page id
	 * @throws NodeException
	 */
	protected int createVtlListPage(Folder folder, Template template, Page targetPage, int vtlConstructId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setName("Vtl list page");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		ContentTag tag = page.getContent().addContentTag(vtlConstructId);
		Folder targetFolder = targetPage.getFolder();
		ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, tag, TEMPLATE_PARTNAME).getValueObject().setValueText(
				"#set($folder = $cms.imps.loader.getFolder(" + targetFolder.getId() + "))#foreach($page in $folder.pages)#if($page.online)Page [$page.name]\n#end#end");
		page.getContentTag(CONTENT_TAGNAME).getValues().getByKeyname(TEXT_PARTNAME).setValueText("<node " + tag.getName() + ">");
		page.save();
		page.publish();
		t.commit(false);
		return ObjectTransformer.getInt(page.getId(), 0);
	}

	/**
	 * Create test page that loads the page with the loader imp
	 *
	 * @param folder
	 *            folder
	 * @param template
	 *            template
	 * @param targetPage
	 *            target page
	 * @param vtlConstructId
	 *            velocity construct id
	 * @return test page id
	 * @throws NodeException
	 */
	protected int createVtlLoaderPage(Folder folder, Template template, Page targetPage, int vtlConstructId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setName("Vtl loader page");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		ContentTag tag = page.getContent().addContentTag(vtlConstructId);
		ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, tag, TEMPLATE_PARTNAME).getValueObject()
				.setValueText("#set($page = $cms.imps.loader.getPage(" + targetPage.getId() + "))#if($page.online)$page.name#end");
		page.getContentTag(CONTENT_TAGNAME).getValues().getByKeyname(TEXT_PARTNAME).setValueText("<node " + tag.getName() + ">");
		page.save();
		page.publish();
		t.commit(false);
		return ObjectTransformer.getInt(page.getId(), 0);
	}

	/**
	 * Create test page that loads the page with the loader imp and renders the
	 * pdate
	 *
	 * @param folder
	 *            folder
	 * @param template
	 *            template
	 * @param targetPage
	 *            target page
	 * @param vtlConstructId
	 *            velocity construct id
	 * @return test page id
	 * @throws NodeException
	 */
	protected int createVtlPdatePage(Folder folder, Template template, Page targetPage, int vtlConstructId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setName("Vtl pdate page");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		ContentTag tag = page.getContent().addContentTag(vtlConstructId);
		ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, tag, TEMPLATE_PARTNAME).getValueObject()
				.setValueText("#set($page = $cms.imps.loader.getPage(" + targetPage.getId() + "))#if($page.online)$page.name - $page.publishtimestamp#end");
		page.getContentTag(CONTENT_TAGNAME).getValues().getByKeyname(TEXT_PARTNAME).setValueText("<node " + tag.getName() + ">");
		page.save();
		page.publish();
		t.commit(false);
		return ObjectTransformer.getInt(page.getId(), 0);
	}

	/**
	 * Create test page rendering a manual overview
	 *
	 * @param folder
	 *            folder
	 * @param template
	 *            template
	 * @param targetPage
	 *            target page
	 * @param overviewConstructId
	 *            overview construct id
	 * @return test page id
	 * @throws NodeException
	 */
	protected int createManualOverviewPage(Folder folder, Template template, Page targetPage, int overviewConstructId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setName("Manual overview page");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		ContentTag tag = page.getContent().addContentTag(overviewConstructId);
		Overview overview = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, tag, OVERVIEW_PARTNAME).getOverview();
		overview.setMaxObjects(0);
		overview.setObjectClass(Page.class);
		overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
		List<OverviewEntry> entries = overview.getOverviewEntries();
		OverviewEntry pageEntry = t.createObject(OverviewEntry.class);
		pageEntry.setObjectId(targetPage.getId());
		entries.add(pageEntry);
		tag.getValues().getByKeyname(OVERVIEW_PARTNAME).setValueText("<node page.name>");

		page.getContentTag(CONTENT_TAGNAME).getValues().getByKeyname(TEXT_PARTNAME).setValueText("<node " + tag.getName() + ">");
		page.save();
		page.publish();
		t.commit(false);
		return ObjectTransformer.getInt(page.getId(), 0);
	}

	/**
	 * Create test page rendering an overview over pages in a folder
	 *
	 * @param folder
	 *            folder
	 * @param template
	 *            template
	 * @param targetPage
	 *            target page
	 * @param overviewConstructId
	 *            overview construct id
	 * @return test page id
	 * @throws NodeException
	 */
	protected int createFolderOverviewPage(Folder folder, Template template, Page targetPage, int overviewConstructId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setName("Folder overview page");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		ContentTag tag = page.getContent().addContentTag(overviewConstructId);
		Overview overview = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, tag, OVERVIEW_PARTNAME).getOverview();
		overview.setMaxObjects(0);
		overview.setObjectClass(Page.class);
		overview.setSelectionType(Overview.SELECTIONTYPE_FOLDER);
		List<OverviewEntry> entries = overview.getOverviewEntries();
		OverviewEntry pageEntry = t.createObject(OverviewEntry.class);
		pageEntry.setObjectId(targetPage.getFolder().getId());
		entries.add(pageEntry);
		tag.getValues().getByKeyname(OVERVIEW_PARTNAME).setValueText("<node page.name>");

		page.getContentTag(CONTENT_TAGNAME).getValues().getByKeyname(TEXT_PARTNAME).setValueText("<node " + tag.getName() + ">");
		page.save();
		page.publish();
		t.commit(false);
		return ObjectTransformer.getInt(page.getId(), 0);
	}

	/**
	 * Assert the published contents of the pages
	 *
	 * @param expectOnline
	 *            true if the target page is expected to be online, false if not
	 * @param expectedPdate
	 *            expected pdate of the target page
	 * @throws NodeException
	 */
	protected void assertPublishedContents(boolean expectOnline, int expectedPdate) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page targetPage = t.getObject(Page.class, targetPageId);
		assertEquals("Check online status of target page", expectOnline, targetPage.isOnline());
		if (expectOnline) {
			assertEquals("Check target page pdate", expectedPdate, targetPage.getPDate().getIntTimestamp());
			assertPublishedContent(targetPageId, "Target page: [Target content]");
			assertPublishedContent(urlPageId, "Url page: [/Content.Node/Target-page.html]");
			assertPublishedContent(pagetagPageId, "Pagetag page: [Target content]");
			assertPublishedContent(vtlListPage, "Vtl list page: [Page [Target page]\n]");
			assertPublishedContent(vtlLoaderPage, "Vtl loader page: [Target page]");
			assertPublishedContent(vtlPdatePage, "Vtl pdate page: [Target page - " + expectedPdate + "]");
			assertPublishedContent(manualOverviewPage, "Manual overview page: [Target page]");
			assertPublishedContent(folderOverviewPage, "Folder overview page: [Target page]");
		} else {
			assertPublishedContent(targetPageId, null);
			assertPublishedContent(urlPageId, "Url page: [#]");
			assertPublishedContent(pagetagPageId, "Pagetag page: [Target content]"); // pagetag
																						// is
																						// rendered
																						// for
																						// offline
																						// pages
																						// as
																						// well
			assertPublishedContent(vtlListPage, "Vtl list page: []");
			assertPublishedContent(vtlLoaderPage, "Vtl loader page: []");
			assertPublishedContent(vtlPdatePage, "Vtl pdate page: []");
			assertPublishedContent(manualOverviewPage, "Manual overview page: []");
			assertPublishedContent(folderOverviewPage, "Folder overview page: []");
		}
	}

	/**
	 * Make an assertion about the published content of the given page
	 *
	 * @param pageId
	 *            page id
	 * @param expectedSource
	 *            expected source
	 * @throws NodeException
	 */
	protected void assertPublishedContent(final int pageId, final String expectedSource) throws NodeException {
		DBUtils.executeStatement("SELECT source FROM publish WHERE page_id = ? AND active = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, pageId);
				stmt.setInt(2, 1);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					if (expectedSource != null) {
						assertEquals("Check published content of page " + pageId, expectedSource, rs.getString("source"));
					} else {
						fail("Expected the page " + pageId + " to not being published, but found content " + rs.getString("source"));
					}
				} else {
					if (expectedSource != null) {
						fail("Did not find published content of page " + pageId);
					}
				}
			}
		});
	}

}
