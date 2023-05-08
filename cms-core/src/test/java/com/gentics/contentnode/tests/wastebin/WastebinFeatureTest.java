package com.gentics.contentnode.tests.wastebin;

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
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test cases for wastebin
 */
@RunWith(value = Parameterized.class)
public class WastebinFeatureTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Feature setting
	 */
	protected FeatureSetting feature;

	/**
	 * Node to test in
	 */
	private Node node;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: feature {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (FeatureSetting f : FeatureSetting.values()) {
			data.add(new Object[] {f});
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param feature feature setting
	 */
	public WastebinFeatureTest(FeatureSetting feature) {
		this.feature = feature;
	}

	@Before
	public void setup() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		node = ContentNodeTestDataUtils.createNode("testnode", "Test node", PublishTarget.BOTH);
		switch (feature) {
		case off:
			prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), false);
			break;
		case on:
			prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), true);
			break;
		}

		t.commit(false);

		testContext.getContext().stopDirtQueueWorker();
	}

	/**
	 * Test deleting a page
	 * @throws Exception
	 */
	@Test
	public void testDeletePage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a page
		Template template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template", "Template");
		Page page = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Test page");
		t.commit(false);

		int pageId = page.getId();
		int contentId = page.getContent().getId();

		t.getObject(Page.class, pageId, true).publish();
		t.commit(false);
		testContext.publish(false);

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		ContentNodeTestUtils.assertPublishCR(page, node, true);

		// delete the page
		page.delete();
		t.commit(false);

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();
		t.getRenderType().setEditMode(RenderType.EM_PREVIEW);

		checkObject(Page.class, pageId);
		switch (feature) {
		case off:
			// content must really be deleted
			assertNull("Content must be deleted", t.getObject(Content.class, contentId));
			break;
		case on:
			// content must not really be deleted
			assertNotNull("Content must not be deleted", t.getObject(Content.class, contentId));
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				page = t.getObject(page);
				assertThat(page).as("Page in wastebin").isOffline();
			}
			break;
		}

		t.getRenderType().setEditMode(RenderType.EM_PUBLISH);
		testContext.publish(false);

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();
		ContentNodeTestUtils.assertPublishCR(page, node, false);
	}

	/**
	 * Test deleting a folder
	 * @throws Exception
	 */
	@Test
	public void testDeleteFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a folder
		Folder folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
		t.commit(false);

		// create objects in the folder
		Folder subFolder = ContentNodeTestDataUtils.createFolder(folder, "Subfolder");
		Template template = ContentNodeTestDataUtils.createTemplate(folder, "Template source", "Template");
		Page page = ContentNodeTestDataUtils.createPage(folder, template, "Page");
		File file = ContentNodeTestDataUtils.createFile(folder, "testfile.txt", "Testfile contents".getBytes());
		t.commit(false);

		int folderId = folder.getId();
		int subFolderId = subFolder.getId();
		int pageId = page.getId();
		int fileId = file.getId();

		testContext.publish(false);
		ContentNodeTestUtils.assertPublishCR(folder, node, true);

		// delete the folder
		folder.delete();
		t.commit(false);

		checkObject(Folder.class, folderId);
		checkObject(Folder.class, subFolderId);
		checkObject(Page.class, pageId);
		checkObject(File.class, fileId);

		testContext.publish(false);
		ContentNodeTestUtils.assertPublishCR(folder, node, false);
	}

	/**
	 * Test deleting a file
	 * @throws Exception
	 */
	@Test
	public void testDeleteFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a file
		File file = ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile.txt", "File contents".getBytes());
		t.commit(false);

		int fileId = file.getId();
		testContext.publish(false);

		ContentNodeTestUtils.assertPublishCR(file, node, true);

		// delete the file
		file.delete();
		t.commit(false);

		checkObject(File.class, fileId);

		testContext.publish(false);
		ContentNodeTestUtils.assertPublishCR(file, node, false);
	}

	/**
	 * Check whether the object was deleted or put into the wastebin
	 * @param clazz object class
	 * @param id object id
	 * @throws Exception
	 */
	protected void checkObject(Class<? extends NodeObject> clazz, final int id) throws Exception {
		switch (feature) {
		case off:
			checkObject(clazz, id, false, false);
			break;
		case on:
			checkObject(clazz, id, true, true);
			break;
		}
	}

	/**
	 * Check whether the object was deleted or put into the wastebin
	 * @param clazz object class
	 * @param id object id
	 * @param expectExistence true if the object is expected to still exist
	 * @param expectWastebin true if the object is expected to be in the wastebin
	 * @throws Exception
	 */
	protected void checkObject(Class<? extends NodeObject> clazz, final int id, boolean expectExistence, boolean expectWastebin) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Integer> logCmd = getLogCmd(clazz, id);
		List<Integer> events = getEvents(clazz, id);

		if (expectExistence) {
			checkFilterObject(clazz, id, expectWastebin);

			final int[] deleted = new int[1];
			final int[] deletedBy = new int[1];

			// page must be marked as being deleted
			DBUtils.executeStatement("SELECT deleted, deletedby FROM " + t.getTable(clazz) + " WHERE id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, id);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					if (rs.next()) {
						deleted[0] = rs.getInt("deleted");
						deletedBy[0] = rs.getInt("deletedby");
					}
				}
			});

			if (expectWastebin) {
				assertTrue("Object must be marked as being deleted", deleted[0] != 0);
				assertTrue("Object must be marked as being deleted by user", deletedBy[0] != 0);

				// we expect a logcmd entry and an event
				assertTrue("Expected wastebin log", logCmd.contains(ActionLogger.WASTEBIN));
				// templates get logs for "del" when they are unlinked (not deleted)
				if (!clazz.isAssignableFrom(Template.class)) {
					assertFalse("Unexpected del log", logCmd.contains(ActionLogger.DEL));
				}
				assertTrue("Expected delete event", events.contains(Events.DELETE | Events.WASTEBIN));
			} else {
				assertTrue("Object must not be marked as being deleted", deleted[0] == 0);
				assertTrue("Object must not be marked as being deleted by user", deletedBy[0] == 0);

				assertFalse("Unexpected wastebin log", logCmd.contains(ActionLogger.WASTEBIN));
				assertFalse("Unexpected del log", logCmd.contains(ActionLogger.DEL));
				assertFalse("Unexpected delete event", events.contains(Events.DELETE));
			}

			assertMarkedAsDeleted(t.getObject(clazz, id), expectWastebin);
		} else {
			assertNull("Object must be deleted", t.getObject(clazz, id));

			assertFalse("Unexpected wastebin log", logCmd.contains(ActionLogger.WASTEBIN));
			assertTrue("Expected del log", logCmd.contains(ActionLogger.DEL));
			assertTrue("Expected delete event", events.contains(Events.DELETE));
		}
	}

	/**
	 * Check getting the object with different Wastebin filters
	 * @param clazz object class
	 * @param id object id
	 * @param expectWastebin true if the object is expected to be in the wastebin
	 * @throws Exception
	 */
	protected void checkFilterObject(Class<? extends NodeObject> clazz, int id, boolean expectWastebin) throws Exception {
		checkFilterObject(clazz, id, Wastebin.INCLUDE, true);
		checkFilterObject(clazz, id, Wastebin.ONLY, expectWastebin);
		checkFilterObject(clazz, id, Wastebin.EXCLUDE, !expectWastebin);
	}

	/**
	 * Check getting the object with the given filter
	 * @param clazz object class
	 * @param id object id
	 * @param wastebin wastebin filter
	 * @param expect true if the object is expected to be filtered, false if not
	 * @throws Exception
	 */
	protected void checkFilterObject(Class<? extends NodeObject> clazz, int id, Wastebin wastebin, boolean expect) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		try (WastebinFilter f = wastebin.set()) {
			if (expect) {
				assertNotNull("Object should be found for " + wastebin + " filter", t.getObject(clazz, id, false, true, false));
				assertFalse("Object should be found for " + wastebin + " filter", t.getObjects(clazz, Arrays.asList(id), false, true).isEmpty());
			} else {
				assertNull("Object should not be found for " + wastebin + " filter", t.getObject(clazz, id, false, true, false));
				assertTrue("Object should not be found for " + wastebin + " filter", t.getObjects(clazz, Arrays.asList(id), false, true).isEmpty());
			}
		}
	}

	/**
	 * Get all cmdDescIds logged for the given object
	 * @param clazz object class
	 * @param id object id
	 * @return list of cmdDescIds
	 * @throws NodeException
	 */
	protected List<Integer> getLogCmd(Class<? extends NodeObject> clazz, final int id) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		final int tType = t.getTType(clazz);
		final List<Integer> logCmd = new ArrayList<Integer>();
		DBUtils.executeStatement("SELECT cmd_desc_id FROM logcmd WHERE o_type = ? AND o_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, tType);
				stmt.setInt(2, id);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					logCmd.add(rs.getInt("cmd_desc_id"));
				}
			}
		});
		return logCmd;
	}

	/**
	 * Get all event masks for the given object
	 * @param clazz object class
	 * @param id object id
	 * @return list of event masks
	 * @throws NodeException
	 */
	protected List<Integer> getEvents(Class<? extends NodeObject> clazz, final int id) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		final int tType = t.getTType(clazz);
		final List<Integer> events = new ArrayList<Integer>();
		DBUtils.executeStatement("SELECT events FROM dirtqueue WHERE obj_type = ? AND obj_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, tType);
				stmt.setInt(2, id);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					events.add(rs.getInt("events"));
				}
			}
		});

		return events;
	}

	/**
	 * Assert whether the object is marked as deleted
	 * @param object object
	 * @param expected true if the object is expected to be marked as being deleted
	 * @throws Exception
	 */
	protected void assertMarkedAsDeleted(NodeObject object, boolean expected) throws Exception {
		if (object instanceof Page) {
			assertEquals("Check whether " + object + " is marked as deleted", expected, ((Page) object).isDeleted());
		} else if (object instanceof Folder) {
			assertEquals("Check whether " + object + " is marked as deleted", expected, ((Folder) object).isDeleted());
		} else if (object instanceof File) {
			assertEquals("Check whether " + object + " is marked as deleted", expected, ((File) object).isDeleted());
		} else if (object instanceof Template) {
			assertEquals("Check whether " + object + " is marked as deleted", expected, ((Template) object).isDeleted());
		} else if (!expected) {
			fail(object + " cannot be marked as being deleted");
		}
	}

	/**
	 * Values for the feature setting
	 */
	public static enum FeatureSetting {
		/**
		 * Feature is generally off
		 */
		off,

		/**
		 * Feature is generally on
		 */
		on
	}
}
