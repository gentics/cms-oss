package com.gentics.contentnode.tests.dirting;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.job.DeleteJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.OverviewHelper;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.util.FileUtil;

/**
 * Test cases for dirting
 */
@GCNFeature(unset = {Feature.TAG_IMAGE_RESIZER, Feature.PUBLISH_CACHE})
public class DirtingSandboxTest {
	/**
	 * Content of the filled object property
	 */
	private static final String OE_CONTENT = "OE content";

	/**
	 * Name of the object property
	 */
	private static final String OE_NAME = "Object Property";

	/**
	 * Keyword of the object property, including the object. prefix
	 */
	private static final String OE_KEYWORD = "object.test";

	/**
	 * Keyword of the object property, without the object. prefix
	 */
	private static final String OE_KEYWORD_SHORT = "test";

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * ID of the root folder
	 */
	public final static int FOLDER_ID = 1;

	/**
	 * Name of the testfile
	 */
	public final static String TESTFILE_NAME = "testfile.txt";

	/**
	 * New Name of the testfile
	 */
	public final static String NEW_TESTFILE_NAME = "new_testfile.txt";

	/**
	 * Test whether changing a filename of a file dirts pages that link to that file
	 * @throws Exception
	 */
	@Test
	public void testChangeFilename() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
		Node node = rootFolder.getNode();

		// create a construct
		Construct construct = t.createObject(Construct.class);

		construct.setKeyword("link");
		construct.setName("Link (de)", 1);
		construct.setName("Link (en)", 2);
		construct.getNodes().add(node);
		Part urlPart = t.createObject(Part.class);

		urlPart.setKeyname("url");
		urlPart.setHidden(false);
		urlPart.setEditable(1);
		urlPart.setName("Url", 1);
		urlPart.setName("Url", 2);
		urlPart.setPartOrder(1);
		urlPart.setPartTypeId(8);
		List<Part> parts = construct.getParts();

		parts.add(urlPart);
		construct.save();

		// create a template
		Template template = t.createObject(Template.class);

		template.setFolderId(rootFolder.getId());
		template.setMlId(1);
		template.setName("Test Template");
		template.setSource("<node link1>");
		template.save();
		t.commit(false);

		// create a file
		File file = t.createObject(File.class);

		file.setFolderId(rootFolder.getId());
		file.setName(TESTFILE_NAME);
		file.setFileStream(getClass().getResourceAsStream(TESTFILE_NAME));
		file.save();

		// create a page
		Page page = t.createObject(Page.class);

		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));

		tag.setEnabled(true);
		PartType partType = tag.getValues().getByKeyname("url").getPartType();

		if (partType instanceof FileURLPartType) {
			((FileURLPartType) partType).setTargetFile(file);
		} else {
			fail("Wrong parttype " + partType.getClass());
		}
		page.save();
		page.publish();
		t.commit(false);

		// run the publish process
		testContext.getContext().publish(false, true);

		// check whether the page is still dirted (entry in publish queue exists)
		final int pageId = ObjectTransformer.getInt(page.getId(), 0);
		final int[] result = { -1};

		DBUtils.executeStatement("SELECT COUNT(DISTINCT obj_id) c FROM publishqueue WHERE obj_type = ? AND obj_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, Page.TYPE_PAGE);
				stmt.setInt(2, pageId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					result[0] = rs.getInt("c");
				}
			}
		});
		assertEquals("Check # of publishqueue entries for " + page, 0, result[0]);

		// now change the filename
		file.setName(NEW_TESTFILE_NAME);
		file.save();
		t.commit(false);

		// wait for the dirting
		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		// check whether the page is dirted now
		result[0] = -1;
		DBUtils.executeStatement("SELECT COUNT(DISTINCT obj_id) c FROM publishqueue WHERE obj_type = ? AND obj_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, Page.TYPE_PAGE);
				stmt.setInt(2, pageId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					result[0] = rs.getInt("c");
				}
			}
		});
		assertEquals("Check # of publishqueue entries for " + page, 1, result[0]);
	}

	/**
	 * Test whether changing the source code of its template causes a page to be dirted
	 * @throws Exception
	 */
	@Test
	public void testChangeTemplateSource() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);

		// create a template
		Template template = t.createObject(Template.class);

		template.setFolderId(rootFolder.getId());
		template.setMlId(1);
		template.setName("Test Template");
		template.setSource("old source");
		template.save();
		t.commit(false);

		// create a page
		Page page = t.createObject(Page.class);

		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		page.setName("Page Name");
		page.save();

		page.publish();
		t.commit(false);

		// run the publish process
		testContext.getContext().publish(false, true);

		// check the page status
		assertThat(page).as("Test page").isOnline();

		// change the template source text
		template.setSource("new source");
		template.save();

		t.commit(false);

		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		assertThat(page).as("Check page status after changing the template source (should be dirted)").isOnline();
		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, rootFolder.getNode());

		assertTrue("The pageId should be listed in the list of dirted pages.", dirtedPageIds.contains(page.getId()));

	}

	/**
	 * Test whether changing the text of a template tag causes a page based on the template to be dirted
	 * @throws Exception
	 */
	@Test
	public void testChangeTemplateTagText() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
		Node node = rootFolder.getNode();

		Construct construct = t.createObject(Construct.class);

		construct.setKeyword("construct");
		construct.setName("Construct (de)", 1);
		construct.setName("Construct (en)", 2);
		construct.getNodes().add(node);

		Part textPart = t.createObject(Part.class);

		textPart.setKeyname("text");
		textPart.setHidden(false);
		textPart.setEditable(1);
		textPart.setName("Text", 1);
		textPart.setName("Text", 2);
		textPart.setPartOrder(1);
		textPart.setPartTypeId(2);
		List<Part> parts = construct.getParts();

		parts.add(textPart);

		construct.save();

		// create a template
		Template template = t.createObject(Template.class);

		template.setFolderId(rootFolder.getId());
		template.setMlId(1);
		template.setName("Test Template");
		template.setSource("");
		template.save();

		Map<String, TemplateTag> tags = template.getTemplateTags();
		TemplateTag tag = t.createObject(TemplateTag.class);

		tag.setPublic(false);
		tag.setEnabled(true);
		tag.setConstructId(construct.getId());
		tag.setName("ttag");
		tag.setTemplateId(template.getId());
		tag.getValues().getByKeyname("text").setValueText("oldtext");
		tag.save();

		tags.put(tag.getName(), tag);

		template.setSource("<node ttag>");
		template.save();
		t.commit(false);

		// create a page
		Page page = t.createObject(Page.class);

		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		page.setName("Page Name");
		page.save();

		page.publish();
		t.commit(false);

		// run the publish process
		testContext.getContext().publish(false, true);

		// check the page status
		assertThat(page).as("Page after publish").isOnline();

		// change the text of the template tag
		template.getTemplateTag("ttag").getValues().getByKeyname("text").setValueText("newtext");
		template.save();

		t.commit(false);

		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		assertThat(page).as("Page after dirting").isOnline();

		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, node);

		assertTrue("The pageId should be listed in the list of dirted pages.", dirtedPageIds.contains(page.getId()));

	}

	/**
	 * Test whether changing a construct description does not dirt pages that uses it.
	 *
	 * @throws Exception
	 */
	@Test
	public void testChangeConstructDescription() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
		Node node = rootFolder.getNode();

		// create a construct
		Construct construct = t.createObject(Construct.class);
		construct.setKeyword("link");
		construct.setName("Link (de)", 1);
		construct.setName("Link (en)", 2);
		construct.getNodes().add(node);

		Part htmlPart = t.createObject(Part.class);
		htmlPart.setKeyname("url");
		htmlPart.setHidden(false);
		htmlPart.setEditable(0);
		htmlPart.setName("Url", 1);
		htmlPart.setName("Url", 2);
		htmlPart.setPartOrder(1);
		htmlPart.setPartTypeId(10);

		Value v = t.createObject(Value.class);
		v.setPart(htmlPart);
		v.setValueText("bala");

		htmlPart.setDefaultValue(v);
		List<Part> parts = construct.getParts();

		parts.add(htmlPart);
		construct.save();

		// create a template
		Template template = t.createObject(Template.class);
		template.setFolderId(rootFolder.getId());
		template.setMlId(1);
		template.setName("Test Template");
		template.setSource("<node link1>");
		template.save();
		t.commit(false);

		// create a page
		Page page = t.createObject(Page.class);
		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));

		tag.setEnabled(true);

		page.save();
		page.publish();
		t.commit(false);

		// run the publish process
		testContext.getContext().publish(false, true);

		// check the page status
		assertThat(page).as("Page after publish process").isOnline();

		// now change the description
		construct.setDescription("updated [en]", 1);
		construct.setDescription("updated [de]", 2);
		construct.save();
		t.commit(false);

		// wait for the dirting
		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());
		t.commit(false);

		// check the page status
		assertThat(page).as("Page after dirting").isOnline();
		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, rootFolder.getNode());
		assertEquals("No pages should be dirted.", 0, dirtedPageIds.size());
	}

	/**
	 * Test whether changing construct parts dirts pages that uses it.
	 *
	 * @throws Exception
	 */
	@Test
	public void testChangeConstructParts() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
		Node node = rootFolder.getNode();

		// create a construct
		Construct construct = t.createObject(Construct.class);

		construct.setKeyword("link");
		construct.setName("Link (de)", 1);
		construct.setName("Link (en)", 2);
		construct.getNodes().add(node);

		Part htmlPart = t.createObject(Part.class);

		htmlPart.setKeyname("url");
		htmlPart.setHidden(false);
		htmlPart.setEditable(0);
		htmlPart.setName("Url", 1);
		htmlPart.setName("Url", 2);
		htmlPart.setPartOrder(1);
		htmlPart.setPartTypeId(10);

		Value v = t.createObject(Value.class);

		v.setPart(htmlPart);
		v.setValueText("bala");

		htmlPart.setDefaultValue(v);
		List<Part> parts = construct.getParts();

		parts.add(htmlPart);

		construct.save();

		// create a template
		Template template = t.createObject(Template.class);

		template.setFolderId(rootFolder.getId());
		template.setMlId(1);
		template.setName("Test Template");
		template.setSource("<node link1>");
		template.save();
		t.commit(false);

		// create a page
		Page page = t.createObject(Page.class);

		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));

		tag.setEnabled(true);

		page.save();
		page.publish();
		t.commit(false);

		// run the publish process
		testContext.getContext().publish(false, true);

		// check the page status
		assertThat(page).as("Page after publish process").isOnline();

		// now change the default value
		v = t.createObject(Value.class);
		v.setPart(htmlPart);
		v.setValueText("bala2");
		htmlPart.setDefaultValue(v);

		construct.save();
		t.commit(false);

		// wait for the dirting
		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());
		t.commit(false);

		// check the page status
		assertThat(page).as("Page after dirting").isOnline();
		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, rootFolder.getNode());

		assertTrue("The pageId should be listed in the list of dirted pages.", dirtedPageIds.contains(page.getId()));
	}

	/**
	 * Test, whether a page is published after the first publish run after the
	 * timeout value of "publish at" has been reached. This test is unstable as
	 * it fails only sometimes if the corresponding bug is present.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFirstPublishAfterTimeout() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create the testnode
		Node node = t.createObject(Node.class);
		node.setHostname("remove");
		node.setPublishDir("/");

		// Create the testfolder
		Folder rootFolder= t.createObject(Folder.class);
		rootFolder.setName("remove");
		rootFolder.setPublishDir("/remove/");
		node.setFolder(rootFolder);

		// Save both objects
		node.save();
		rootFolder.save();

		// Create a dummy construct
		Construct construct = t.createObject(Construct.class);
		construct.setKeyword("link");
		construct.setName("Link (de)", 1);
		construct.setName("Link (en)", 2);
		construct.getNodes().add(node);
		Part urlPart = t.createObject(Part.class);

		urlPart.setKeyname("url");
		urlPart.setHidden(false);
		urlPart.setEditable(1);
		urlPart.setName("Url", 1);
		urlPart.setName("Url", 2);
		urlPart.setPartOrder(1);
		urlPart.setPartTypeId(8);
		List<Part> parts = construct.getParts();

		parts.add(urlPart);
		construct.save();

		// Create a template
		Template template = t.createObject(Template.class);

		template.setFolderId(rootFolder.getId());
		template.setMlId(1);
		template.setName("Test Template");
		template.setSource("<node link1>");
		template.save();
		t.commit(false);

		// Create a page in the previously created folder using the created template
		final Page page = t.createObject(Page.class);
		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));
		tag.setEnabled(true);
		page.save();

		// 1. Publish the page at a specified time in the future
		//page.publish();
		page.publish((int)(System.currentTimeMillis()/1000)+12, null);
		t.commit(false);

		// 2. Wait a few seconds
		Thread.sleep(13000);

		// 3. Dirt all objects and publish again
		testContext.getContext().startTransaction();
		testContext.getContext().publish(false, true);

		// 4. Check that the page is now active
		DBUtils.executeStatement("select id from publish where page_id=? and active=1", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ObjectTransformer.getInt(page.getId(), -1));
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				assertTrue("Must have at least one result", rs.next());
				assertFalse("Must have at most one result", rs.next());
			}
		});

	}
	/**
	 * Test if dirting works so far as that newly published pages show up in their overviews
	 * and removed pages don't show up any more.
	 * @throws Exception
	 */
	@Test
	public void testPublishDirtingOverview() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create Node
		Node n = t.createObject(Node.class);
		n.setHostname("blargh");
		n.setPublishDir("/");
		Folder rootFolder = t.createObject(Folder.class);
		rootFolder.setName("blargh");
		rootFolder.setPublishDir("/");
		n.setFolder(rootFolder);
		n.save();

		// Create overview tagtype
		Construct construct = t.createObject(Construct.class);
		construct.setKeyword("ov");
		construct.setName("blargh", 1);
		construct.setName("blargh", 2);
		construct.getNodes().add(n);
		construct.setAutoEnable(true);

		Part ov = t.createObject(Part.class);
		ov.setPartTypeId(13);
		ov.setKeyname("ds");
		ov.setEditable(1);
		ov.setHidden(false);
		construct.getParts().add(ov);

		Value v = t.createObject(Value.class);
		v.setPart(ov);
		v.setInfo(1);
		ov.setDefaultValue(v);

		construct.save();

		// Create template
		Template template = t.createObject(Template.class);
		template.setFolderId(rootFolder.getId());
		template.setSource("Templatetag goes here:<node ds>");
		TemplateTag tt1 = t.createObject(TemplateTag.class);
		tt1.setName("ds");
		tt1.setConstructId(construct.getId());
		tt1.setPublic(true);
		tt1.setEnabled(true);
		template.getTemplateTags().put("ds", tt1);
		template.save();

		// Create page
		Page pageX = t.createObject(Page.class);
		pageX.setFolderId(rootFolder.getId());
		pageX.setTemplateId(template.getId());
		pageX.setName("blargh");
		pageX.setFilename("blargh.html");
		pageX.save();

		// Create page with overview
		final Page pageY = t.createObject(Page.class);
		pageY.setFolderId(rootFolder.getId());
		pageY.setTemplateId(template.getId());
		pageY.setName("blarghov");
		pageY.setFilename("blarghov.html");

		pageY.save();

		PageResourceImpl pri = new PageResourceImpl();
		pri.setTransaction(t);
		com.gentics.contentnode.rest.model.Page restpage = pri.load(pageY.getId().toString(), true, false, false, false, false, false, false, false, false, false, null, null).getPage();
		com.gentics.contentnode.rest.model.Overview overview = OverviewHelper.extractOverviewFromRestPage(restpage);
		overview.setListType(ListType.PAGE);
		overview.setSelectType(SelectType.MANUAL);
		overview.setSelectedItemIds(Arrays.asList(new Integer[]{pageX.getId()}));
		overview.setSource("Page name goes here:<node page.name>\n");
		PageSaveRequest psr = new PageSaveRequest();
		psr.setPage(restpage);
		pri.save(pageY.getId().toString(), psr);

		// Publish page with overview
		pageY.publish();
		t.commit(false);
		testContext.getContext().publish(false, true);

		// Publish page
		pageX.publish();
		t.commit(false);
		testContext.getContext().publish(false,true);
		testContext.getContext().startTransaction();

		t = TransactionManager.getCurrentTransaction();
		// Check page with overview
		DBUtils.executeStatement("select source from publish where page_id=? and active=1", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setObject(1, pageY.getId());
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				assertTrue("Overview page must be there", rs.next());
				assertEquals("Wrong content","Templatetag goes here:Page name goes here:blargh\n",rs.getString(1));
			}
		});
		pageX=t.getObject(Page.class, pageX.getId(),true);
		pageX.takeOffline();
		t.commit(false);
		testContext.getContext().publish(false,true);
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();
		// Check page with overview
		DBUtils.executeStatement("select source from publish where page_id=? and active=1", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setObject(1, pageY.getId());
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				assertTrue("Overview page must be there", rs.next());
				assertEquals("Wrong content","Templatetag goes here:",rs.getString(1));
			}
		});
	}

	/**
	 * Test changing the node's name
	 * @throws Exception
	 */
	@Test
	public void testChangeNodeName() throws Exception {
		testChangeNodeProperty(NodeProperty.NAME);
	}

	/**
	 * Test changing the node's host
	 * @throws Exception
	 */
	@Test
	public void testChangeNodeHost() throws Exception {
		testChangeNodeProperty(NodeProperty.HOST);
	}

	/**
	 * Test deleting a construct that is used by an Object Tag
	 * @param enabled true if the object tag shall be enabled, false for disabled
	 * @throws Exception
	 */
	protected void testDeleteConstruct(boolean enabled) throws Exception {
		testContext.getContext().login("node", "node");
		Transaction t = TransactionManager.getCurrentTransaction();
		String sid = t.getSession().getSessionId() + t.getSession().getSessionSecret();
		int userId = t.getUserId();

		Node node = ContentNodeTestDataUtils.createNode("testnode", "Testnode", PublishTarget.FILESYSTEM);
		int constructId = ContentNodeTestDataUtils.createConstruct(node, HTMLPartType.class, "testconstruct", "testpart");
		ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, OE_NAME, OE_KEYWORD);

		Template template = t.createObject(Template.class);
		template.setFolderId(node.getFolder().getId());
		template.setMlId(1);
		template.setName("Template");
		template.setSource("OE:[<node " + OE_KEYWORD + ">]");
		template.save();
		t.commit(false);

		Page page = t.createObject(Page.class);
		page.setFolderId(node.getFolder().getId());
		page.setName("Page");
		page.setTemplateId(template.getId());
		ObjectTag objectTag = page.getObjectTag(OE_KEYWORD_SHORT);
		assertNotNull("Object Tag must exist", objectTag);
		objectTag.getValues().getByKeyname("testpart").setValueText(OE_CONTENT);
		objectTag.setEnabled(enabled);
		page.save();
		page.publish();
		t.commit(false);

		testContext.publish(false);

		java.io.File pageFile = new java.io.File(testContext.getPubDir(), node.getHostname() + node.getPublishDir() + node.getFolder().getPublishDir() + page.getFilename());
		assertTrue("Page must be published", pageFile.exists());
		if (enabled) {
			assertEquals("Check published page", "OE:[" + OE_CONTENT + "]", FileUtil.file2String(pageFile));
		} else {
			assertEquals("Check published page", "OE:[]", FileUtil.file2String(pageFile));
		}

		// now delete the construct and check whether the page is dirted
		int dirtedPages = PublishQueue.countDirtedObjects(Page.class, false, null);

		DeleteJob.process(Construct.class, new ArrayList<Integer>(Arrays.asList(constructId)), false, 0);
		t.commit(false);

		t = testContext.startTransaction((int)(System.currentTimeMillis() / 1000));

		assertNull("Construct must be deleted", t.getObject(Construct.class, constructId));
		testContext.waitForDirtqueueWorker();

		if (enabled) {
			testContext.checkDirtedPages(dirtedPages, new int[] {ObjectTransformer.getInt(page.getId(), 0)});
		} else {
			testContext.checkDirtedPages(dirtedPages, new int[] {});
		}
	}

	/**
	 * Test deleting a construct, that is used in an enabled object tag
	 * We expect the object to be dirted
	 * @throws Exception
	 */
	@Test
	public void testDeleteConstructWithEnabledTags() throws Exception {
		testDeleteConstruct(true);
	}

	/**
	 * Test deleting a construct, that is used in a disabled object tag
	 * We expect the object to not be dirted
	 * @throws Exception
	 */
	@Test
	public void testDeleteConstructWithDisabledTags() throws Exception {
		testDeleteConstruct(false);
	}

	/**
	 * Test changing a node property
	 * @param changed changed property
	 * @throws Exception
	 */
	protected void testChangeNodeProperty(NodeProperty changed) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create Node
		Node node = t.createObject(Node.class);
		node.setHostname("www.before.com");
		node.setPublishDir("/");
		Folder rootFolder = t.createObject(Folder.class);
		rootFolder.setName("Test Node");
		rootFolder.setPublishDir("/");
		node.setFolder(rootFolder);
		node.save();
		t.commit(false);

		// Create template
		Template template = t.createObject(Template.class);
		template.setFolderId(rootFolder.getId());
		template.setSource("Hostname: <node node.host>\nName: <node node.folder.name>");
		template.save();
		t.commit(false);

		// Create page
		Page page = t.createObject(Page.class);
		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		page.setName("Testpage");
		page.setFilename("testpage.html");
		page.save();
		page.publish();
		t.commit(false);

		testContext.getContext().publish(false, true);

		// check the page status
		assertThat(page).as("Page after publish process").isOnline();

		// change the node property
		switch (changed) {
		case HOST:
			node.setHostname("www.after.com");
			node.save();
			break;
		case NAME:
			rootFolder.setName("Changed Test Node");
			rootFolder.save();
			break;
		}

		t.commit(false);

		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		assertThat(page).as("Page after dirting").isOnline();
		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, rootFolder.getNode());

		assertTrue("The pageId should be listed in the list of dirted pages.", dirtedPageIds.contains(page.getId()));
	}

	/**
	 * Node properties that can be changed
	 */
	protected static enum NodeProperty {
		HOST,
		NAME
	}
}
