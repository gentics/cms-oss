package com.gentics.contentnode.tests.dirting;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.job.DeleteJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
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
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.OverviewHelper;
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

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

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
	 * Objects created by the test case, in order of creation
	 */
	private List<NodeObject> createdObjects = new ArrayList<>();

	/**
	 * Delete the objects created by the test case (in reverse order of creation), the published files of created nodes
	 * and wait for the dirtqueue worker to handle the deletion events
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		Collections.reverse(createdObjects);
		for (NodeObject object : createdObjects) {
			if (object.getId() == null) {
				continue;
			}
			try (Trx trx = new Trx()) {
				NodeObject toDelete = trx.getTransaction().getObject(object);
				if (toDelete != null) {
					toDelete.delete(true);
				}
				trx.success();
			}
			if (object instanceof Node) {
				FileUtils.deleteDirectory(new java.io.File(testContext.getPubDir(), ((Node) object).getHostname()));
			}
		}
		testContext.waitForDirtqueueWorker();
	}

	/**
	 * Register the given object for deletion after the test case
	 * @param object created object
	 * @return object
	 */
	protected <T extends NodeObject> T created(T object) {
		createdObjects.add(object);
		return object;
	}

	/**
	 * Test whether changing a filename of a file dirts pages that link to that file
	 * @throws Exception
	 */
	@Test
	public void testChangeFilename() throws Exception {
		File file = null;
		Page page = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
			Node node = rootFolder.getNode();

			// create a construct
			Construct construct = created(t.createObject(Construct.class));

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
			Template template = created(t.createObject(Template.class));

			template.setFolderId(rootFolder.getId());
			template.setMlId(1);
			template.setName("Test Template");
			template.setSource("<node link1>");
			template.save();
			t.commit(false);

			// create a file
			file = created(t.createObject(File.class));

			file.setFolderId(rootFolder.getId());
			file.setName(TESTFILE_NAME);
			file.setFileStream(getClass().getResourceAsStream(TESTFILE_NAME));
			file.save();

			// create a page
			page = created(t.createObject(Page.class));

			page.setFolderId(rootFolder.getId());
			page.setTemplateId(template.getId());
			ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));

			tag.setEnabled(true);
			tag.setName("link1");

			PartType partType = tag.getValues().getByKeyname("url").getPartType();

			if (partType instanceof FileURLPartType) {
				((FileURLPartType) partType).setTargetFile(file);
			} else {
				fail("Wrong parttype " + partType.getClass());
			}
			page.save();
			page.publish();
			trx.success();
		}

		// run the publish process
		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false, true);
			trx.success();
		}

		// check whether the page is still dirted (entry in publish queue exists)
		final int pageId = ObjectTransformer.getInt(page.getId(), 0);
		final int[] result = { -1};

		try (Trx trx = new Trx()) {
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
			trx.success();
		}

		// now change the filename
		try (Trx trx = new Trx()) {
			file = trx.getTransaction().getObject(file, true);
			file.setName(NEW_TESTFILE_NAME);
			file.save();
			trx.success();
		}

		// wait for the dirting
		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		// check whether the page is dirted now
		result[0] = -1;
		try (Trx trx = new Trx()) {
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
			trx.success();
		}
	}

	/**
	 * Test whether changing the source code of its template causes a page to be dirted
	 * @throws Exception
	 */
	@Test
	public void testChangeTemplateSource() throws Exception {
		Template template = null;
		Page page = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);

			// create a template
			template = created(t.createObject(Template.class));

			template.setFolderId(rootFolder.getId());
			template.setMlId(1);
			template.setName("Test Template");
			template.setSource("old source");
			template.save();
			t.commit(false);

			// create a page
			page = created(t.createObject(Page.class));

			page.setFolderId(rootFolder.getId());
			page.setTemplateId(template.getId());
			page.setName("Page Name");
			page.save();

			page.publish();
			trx.success();
		}

		// run the publish process
		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false, true);
			trx.success();
		}

		// check the page status
		try (Trx trx = new Trx()) {
			page = trx.getTransaction().getObject(page);
			assertThat(page).as("Test page").isOnline();
			trx.success();
		}

		// change the template source text
		try (Trx trx = new Trx()) {
			template = trx.getTransaction().getObject(template, true);
			template.setSource("new source");
			template.save();
			trx.success();
		}

		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
			page = t.getObject(page);

			assertThat(page).as("Check page status after changing the template source (should be dirted)").isOnline();
			List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, rootFolder.getNode());

			assertTrue("The pageId should be listed in the list of dirted pages.", dirtedPageIds.contains(page.getId()));
			trx.success();
		}
	}

	/**
	 * Test whether changing the text of a template tag causes a page based on the template to be dirted
	 * @throws Exception
	 */
	@Test
	public void testChangeTemplateTagText() throws Exception {
		Template template = null;
		Page page = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
			Node node = rootFolder.getNode();

			Construct construct = created(t.createObject(Construct.class));

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
			template = created(t.createObject(Template.class));

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
			page = created(t.createObject(Page.class));

			page.setFolderId(rootFolder.getId());
			page.setTemplateId(template.getId());
			page.setName("Page Name");
			page.save();

			page.publish();
			trx.success();
		}

		// run the publish process
		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false, true);
			trx.success();
		}

		// check the page status
		try (Trx trx = new Trx()) {
			page = trx.getTransaction().getObject(page);
			assertThat(page).as("Page after publish").isOnline();
			trx.success();
		}

		// change the text of the template tag
		try (Trx trx = new Trx()) {
			template = trx.getTransaction().getObject(template, true);
			template.getTemplateTag("ttag").getValues().getByKeyname("text").setValueText("newtext");
			template.save();
			trx.success();
		}

		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			Node node = t.getObject(Folder.class, FOLDER_ID).getNode();
			page = t.getObject(page);

			assertThat(page).as("Page after dirting").isOnline();

			List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, node);

			assertTrue("The pageId should be listed in the list of dirted pages.", dirtedPageIds.contains(page.getId()));
			trx.success();
		}
	}

	/**
	 * Test whether changing a construct description does not dirt pages that uses it.
	 *
	 * @throws Exception
	 */
	@Test
	public void testChangeConstructDescription() throws Exception {
		Construct construct = null;
		Page page = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
			Node node = rootFolder.getNode();

			// create a construct
			construct = created(t.createObject(Construct.class));
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
			Template template = created(t.createObject(Template.class));
			template.setFolderId(rootFolder.getId());
			template.setMlId(1);
			template.setName("Test Template");
			template.setSource("<node link1>");
			template.save();
			t.commit(false);

			// create a page
			page = created(t.createObject(Page.class));
			page.setFolderId(rootFolder.getId());
			page.setTemplateId(template.getId());
			ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));

			tag.setEnabled(true);
			tag.setName("link1");

			page.save();
			page.publish();
			trx.success();
		}

		// run the publish process
		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false, true);
			trx.success();
		}

		// check the page status
		try (Trx trx = new Trx()) {
			page = trx.getTransaction().getObject(page);
			assertThat(page).as("Page after publish process").isOnline();
			trx.success();
		}

		// now change the description
		try (Trx trx = new Trx()) {
			construct = trx.getTransaction().getObject(construct, true);
			construct.setDescription("updated [en]", 1);
			construct.setDescription("updated [de]", 2);
			construct.save();
			trx.success();
		}

		// wait for the dirting
		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		// check the page status
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
			page = t.getObject(page);

			assertThat(page).as("Page after dirting").isOnline();
			List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, rootFolder.getNode());
			assertEquals("No pages should be dirted.", 0, dirtedPageIds.size());
			trx.success();
		}
	}

	/**
	 * Test whether changing construct parts dirts pages that uses it.
	 *
	 * @throws Exception
	 */
	@Test
	public void testChangeConstructParts() throws Exception {
		Construct construct = null;
		Page page = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
			Node node = rootFolder.getNode();

			// create a construct
			construct = created(t.createObject(Construct.class));

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
			Template template = created(t.createObject(Template.class));

			template.setFolderId(rootFolder.getId());
			template.setMlId(1);
			template.setName("Test Template");
			template.setSource("<node link1>");
			template.save();
			t.commit(false);

			// create a page
			page = created(t.createObject(Page.class));

			page.setFolderId(rootFolder.getId());
			page.setTemplateId(template.getId());
			ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));

			tag.setEnabled(true);
			tag.setName("link1");

			page.save();
			page.publish();
			trx.success();
		}

		// run the publish process
		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false, true);
			trx.success();
		}

		// check the page status
		try (Trx trx = new Trx()) {
			page = trx.getTransaction().getObject(page);
			assertThat(page).as("Page after publish process").isOnline();
			trx.success();
		}

		// now change the default value
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			construct = t.getObject(construct, true);
			Part htmlPart = construct.getParts().stream().filter(p -> "url".equals(p.getKeyname())).findFirst().get();

			Value v = t.createObject(Value.class);
			v.setPart(htmlPart);
			v.setValueText("bala2");
			htmlPart.setDefaultValue(v);

			construct.save();
			trx.success();
		}

		// wait for the dirting
		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		// check the page status
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			Folder rootFolder = t.getObject(Folder.class, FOLDER_ID);
			page = t.getObject(page);

			assertThat(page).as("Page after dirting").isOnline();
			List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, rootFolder.getNode());

			assertTrue("The pageId should be listed in the list of dirted pages.", dirtedPageIds.contains(page.getId()));
			trx.success();
		}
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
		final Page page;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Create the testnode
			Node node = created(t.createObject(Node.class));
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
			Construct construct = created(t.createObject(Construct.class));
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
			Template template = created(t.createObject(Template.class));

			template.setFolderId(rootFolder.getId());
			template.setMlId(1);
			template.setName("Test Template");
			template.setSource("<node link1>");
			template.save();
			t.commit(false);

			// Create a page in the previously created folder using the created template
			page = created(t.createObject(Page.class));
			page.setFolderId(rootFolder.getId());
			page.setTemplateId(template.getId());
			ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));
			tag.setEnabled(true);
			tag.setName("link1");
			page.save();

			// 1. Publish the page at a specified time in the future
			//page.publish();
			page.publish((int)(System.currentTimeMillis()/1000)+12, null);
			trx.success();
		}

		// 2. Wait a few seconds
		Thread.sleep(13000);

		// 3. Dirt all objects and publish again
		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false, true);
			trx.success();
		}

		// 4. Check that the page is now active
		try (Trx trx = new Trx()) {
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
			trx.success();
		}

	}
	/**
	 * Test if dirting works so far as that newly published pages show up in their overviews
	 * and removed pages don't show up any more.
	 * @throws Exception
	 */
	@Test
	public void testPublishDirtingOverview() throws Exception {
		Page pageX = null;
		final Page pageY;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Create Node
			Node n = created(t.createObject(Node.class));
			n.setHostname("blargh");
			n.setPublishDir("/");
			Folder rootFolder = t.createObject(Folder.class);
			rootFolder.setName("blargh");
			rootFolder.setPublishDir("/");
			n.setFolder(rootFolder);
			n.save();

			// Create overview tagtype
			Construct construct = created(t.createObject(Construct.class));
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
			Template template = created(t.createObject(Template.class));
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
			pageX = created(t.createObject(Page.class));
			pageX.setFolderId(rootFolder.getId());
			pageX.setTemplateId(template.getId());
			pageX.setName("blargh");
			pageX.setFilename("blargh.html");
			pageX.save();

			// Create page with overview
			pageY = created(t.createObject(Page.class));
			pageY.setFolderId(rootFolder.getId());
			pageY.setTemplateId(template.getId());
			pageY.setName("blarghov");
			pageY.setFilename("blarghov.html");

			pageY.save();

			t.commit(false);

			com.gentics.contentnode.rest.model.Page restpage = getPageResource().load(pageY.getId().toString(), true, false, false, false, false, false, false, false, false, false, null, null).getPage();
			com.gentics.contentnode.rest.model.Overview overview = OverviewHelper.extractOverviewFromRestPage(restpage);
			overview.setListType(ListType.PAGE);
			overview.setSelectType(SelectType.MANUAL);
			overview.setSelectedItemIds(Arrays.asList(new Integer[]{pageX.getId()}));
			overview.setSource("Page name goes here:<node page.name>\n");
			PageSaveRequest psr = new PageSaveRequest();
			psr.setPage(restpage);
			getPageResource().save(pageY.getId().toString(), psr);

			// Publish page with overview
			pageY.publish();
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false, true);
			trx.success();
		}

		// Publish page
		try (Trx trx = new Trx()) {
			trx.getTransaction().getObject(pageX, true).publish();
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false,true);
			trx.success();
		}

		// Check page with overview
		try (Trx trx = new Trx()) {
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
			trx.success();
		}

		try (Trx trx = new Trx()) {
			pageX=trx.getTransaction().getObject(Page.class, pageX.getId(),true);
			pageX.takeOffline();
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false,true);
			trx.success();
		}

		// Check page with overview
		try (Trx trx = new Trx()) {
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
			trx.success();
		}
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
		Node node = null;
		int constructId = 0;
		Page page = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			node = created(ContentNodeTestDataUtils.createNode("testnode", "Testnode", PublishTarget.FILESYSTEM));
			constructId = ContentNodeTestDataUtils.createConstruct(node, HTMLPartType.class, "testconstruct", "testpart");
			created(t.getObject(Construct.class, constructId));
			// the object property definition is not registered for deletion, because it cannot be deleted after its construct was deleted
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, OE_NAME, OE_KEYWORD);

			Template template = created(t.createObject(Template.class));
			template.setFolderId(node.getFolder().getId());
			template.setMlId(1);
			template.setName("Template");
			template.setSource("OE:[<node " + OE_KEYWORD + ">]");
			template.save();
			t.commit(false);

			page = created(t.createObject(Page.class));
			page.setFolderId(node.getFolder().getId());
			page.setName("Page");
			page.setTemplateId(template.getId());
			ObjectTag objectTag = page.getObjectTag(OE_KEYWORD_SHORT);
			assertNotNull("Object Tag must exist", objectTag);
			objectTag.getValues().getByKeyname("testpart").setValueText(OE_CONTENT);
			objectTag.setEnabled(enabled);
			page.save();
			page.publish();
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			node = t.getObject(node);
			page = t.getObject(page);

			java.io.File pageFile = new java.io.File(testContext.getPubDir(), node.getHostname() + node.getPublishDir() + node.getFolder().getPublishDir() + page.getFilename());
			assertTrue("Page must be published", pageFile.exists());
			if (enabled) {
				assertEquals("Check published page", "OE:[" + OE_CONTENT + "]", FileUtil.file2String(pageFile));
			} else {
				assertEquals("Check published page", "OE:[]", FileUtil.file2String(pageFile));
			}
			trx.success();
		}

		// now delete the construct and check whether the page is dirted
		int dirtedPages = 0;

		try (Trx trx = new Trx()) {
			dirtedPages = PublishQueue.countDirtedObjects(Page.class, false, null);

			DeleteJob.process(Construct.class, new ArrayList<Integer>(Arrays.asList(constructId)), false, 0);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			assertNull("Construct must be deleted", t.getObject(Construct.class, constructId));
			testContext.waitForDirtqueueWorker();

			if (enabled) {
				testContext.checkDirtedPages(dirtedPages, new int[] {ObjectTransformer.getInt(page.getId(), 0)});
			} else {
				testContext.checkDirtedPages(dirtedPages, new int[] {});
			}
			trx.success();
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
		Node node = null;
		Folder rootFolder = null;
		Page page = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Create Node
			node = created(t.createObject(Node.class));
			node.setHostname("www.before.com");
			node.setPublishDir("/");
			rootFolder = t.createObject(Folder.class);
			rootFolder.setName("Test Node");
			rootFolder.setPublishDir("/");
			node.setFolder(rootFolder);
			node.save();
			t.commit(false);

			// Create template
			Template template = created(t.createObject(Template.class));
			template.setFolderId(rootFolder.getId());
			template.setSource("Hostname: <node node.host>\nName: <node node.folder.name>");
			template.save();
			t.commit(false);

			// Create page
			page = created(t.createObject(Page.class));
			page.setFolderId(rootFolder.getId());
			page.setTemplateId(template.getId());
			page.setName("Testpage");
			page.setFilename("testpage.html");
			page.save();
			page.publish();
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false, true);
			trx.success();
		}

		// check the page status
		try (Trx trx = new Trx()) {
			page = trx.getTransaction().getObject(page);
			assertThat(page).as("Page after publish process").isOnline();
			trx.success();
		}

		// change the node property
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			switch (changed) {
			case HOST:
				Node editableNode = t.getObject(node, true);
				editableNode.setHostname("www.after.com");
				editableNode.save();
				break;
			case NAME:
				rootFolder = t.getObject(rootFolder, true);
				rootFolder.setName("Changed Test Node");
				rootFolder.save();
				break;
			}

			trx.success();
		}

		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			rootFolder = t.getObject(rootFolder);
			page = t.getObject(page);

			assertThat(page).as("Page after dirting").isOnline();
			List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, rootFolder.getNode());

			assertTrue("The pageId should be listed in the list of dirted pages.", dirtedPageIds.contains(page.getId()));
			trx.success();
		}
	}

	/**
	 * Node properties that can be changed
	 */
	protected static enum NodeProperty {
		HOST,
		NAME
	}
}
