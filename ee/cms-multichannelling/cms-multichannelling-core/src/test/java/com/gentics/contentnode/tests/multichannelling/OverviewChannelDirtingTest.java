package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.lib.db.SQLExecutor;

public class OverviewChannelDirtingTest extends AbstractMultichannellingSandboxTest {
	private static final Object OVERVIEW_CONSTRUCT_ID = 5;

	public static volatile boolean stop = false;

	private int timestamp = 1;

	/**
	 * Test if overviews get dirted in the correct channels only if a page is created in a channel in a folder contained by the overview
	 * @throws Exception
	 */
	@Test
	public void testChannelPageOverviewDirting() throws Exception {
		Transaction t = testContext.startTransaction(timestamp++);
		Node node = t.getObject(Node.class, MASTER_ID);
		Folder rootFolder = node.getFolder();

		// Create overview construct
		Construct overviewConstruct = t.createObject(Construct.class);
		overviewConstruct.setName("OverviewConstruct", 1);
		overviewConstruct.setName("OverviewConstruct", 2);
		overviewConstruct.setKeyword("dirtingoverview");
		Part overviewPart = t.createObject(Part.class);
		overviewPart.setPartTypeId(OverviewPartType.TYPE_ID);
		overviewPart.setKeyname("ovp");
		overviewPart.setEditable(1);
		overviewPart.setHidden(false);
		overviewConstruct.getParts().add(overviewPart);
		overviewConstruct.setAutoEnable(true);
		overviewConstruct.save();

		t = testContext.startTransaction(timestamp++);

		// Create some folders
		Folder overviewFolder = t.createObject(Folder.class);
		overviewFolder.setName("overviewFolder");
		overviewFolder.setMotherId(rootFolder.getId());
		overviewFolder.setPublishDir("/");
		overviewFolder.save();

		Folder overviewSubfolder = t.createObject(Folder.class);
		overviewSubfolder.setName("overviewSubfolder");
		overviewSubfolder.setMotherId(overviewFolder.getId());
		overviewSubfolder.setPublishDir("/");
		overviewSubfolder.save();

		t = testContext.startTransaction(timestamp++);

		// Create some templates
		Template emptyTemplate = t.createObject(Template.class);
		emptyTemplate.setSource("Blah");
		emptyTemplate.setFolderId(rootFolder.getId());
		emptyTemplate.addFolder(overviewFolder);
		emptyTemplate.addFolder(overviewSubfolder);
		emptyTemplate.setName("empty template");
		emptyTemplate.save();

		// One with overview
		Template overviewtemplate = t.createObject(Template.class);
		overviewtemplate.setSource("<node ov>");
		overviewtemplate.setFolderId(rootFolder.getId());
		overviewtemplate.setName("overview template");
		overviewtemplate.save();

		TemplateTag tag = t.createObject(TemplateTag.class);
		tag.setConstructId(overviewConstruct.getId());
		tag.setEnabled(true);
		tag.setName("ov");
		tag.setPublic(true);

		overviewtemplate.getTemplateTags().put(tag.getName(), tag);
		overviewtemplate.save();

		tag = t.getObject(TemplateTag.class, tag.getId(), true);
		Value ovtValue = tag.getValues().getByKeyname("ovp");
		ovtValue.setPart(overviewPart);
		tag.getValues().add(ovtValue);
		ovtValue.setValueText("<node page.name><br/>");
		tag.save();


		t = testContext.startTransaction(timestamp++);

		// Create Overview page
		Page overviewPage = t.createObject(Page.class);
		overviewPage.setTemplateId(overviewtemplate.getId());
		overviewPage.setName("Source page");
		overviewPage.setFolderId(rootFolder.getId());
		ContentTag overviewTag = overviewPage.getContentTag("ov");

		assertNotNull("Tag does not exist", overviewTag);
		Value ovValue = overviewTag.getValues().getByKeyname("ovp");
		PartType partType = ovValue.getPartType();

		if (!(partType instanceof OverviewPartType)) {
			fail("Unexpected parttype of part");
		}

		Overview overview = ((OverviewPartType) partType).getOverview();
		overview.setObjectType(Page.TYPE_PAGE);
		overview.setRecursion(true);
		overview.setSelectionType(Overview.SELECTIONTYPE_FOLDER);
		overview.setOrderKind(Overview.ORDER_NAME);
		overview.setOrderWay(Overview.ORDERWAY_ASC);
		OverviewEntry overviewEntry = t.createObject(OverviewEntry.class);
		overviewEntry.setObjectId(overviewFolder.getId());
		overview.getOverviewEntries().add(overviewEntry);
		overviewPage.save();
		overviewPage.publish();
		t = testContext.startTransaction(timestamp++);

		// Create fill pages
		Page page1 = t.createObject(Page.class);
		page1.setTemplateId(emptyTemplate.getId());
		page1.setName("page1");
		page1.setFolderId(overviewFolder.getId());
		page1.save();
		page1.publish();

		Page page2 = t.createObject(Page.class);
		page2.setTemplateId(emptyTemplate.getId());
		page2.setName("page2");
		page2.setFolderId(overviewSubfolder.getId());
		page2.save();
		page2.publish();
		t = testContext.startTransaction(timestamp++);

		// publish everything
		testContext.publish(timestamp++);
		testContext.getContext().stopDirtQueueWorker();
		assertInPageSource(false, overviewPage.getId(), channelId, "page3");

		// create & publish an additional page
		Page page3 = t.createObject(Page.class);
		page3.setTemplateId(emptyTemplate.getId());
		page3.setName("page3");
		page3.setFolderId(overviewSubfolder.getId());
		page3.setChannelInfo(channelId, page3.getChannelSetId());
		page3.save();
		page3.publish();
		t = testContext.startTransaction(timestamp++);

		// check publishqueue for available  update events
		testContext.waitForDirtqueueWorker();
		assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[]{channelId, subChannelId});
		testContext.publish(timestamp++);
		assertInPageSource(true, overviewPage.getId(), channelId, "page3");
		assertInPageSource(true, overviewPage.getId(), subChannelId, "page3");

		// take offline, check
		page3.takeOffline();
		t = testContext.startTransaction(timestamp++);

		testContext.waitForDirtqueueWorker();
		assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[]{channelId, subChannelId});
		testContext.publish(timestamp++);
		assertInPageSource(false, overviewPage.getId(), channelId, "page3");
		assertInPageSource(false, overviewPage.getId(), subChannelId, "page3");

		// take offline once more, shouldn't have any effect
		page3.takeOffline();
		t = testContext.startTransaction(timestamp++);

		testContext.waitForDirtqueueWorker();
		assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[]{});
		testContext.publish(timestamp++);

		// take online, check again
		page3.publish();
		t = testContext.startTransaction(timestamp++);

		testContext.waitForDirtqueueWorker();
		assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[]{channelId, subChannelId});
		testContext.publish(timestamp++);
		assertInPageSource(true, overviewPage.getId(), channelId, "page3");
		assertInPageSource(true, overviewPage.getId(), subChannelId, "page3");

		// publish once more, shouldn't have an effect
		page3.publish();
		t = testContext.startTransaction(timestamp++);

		testContext.waitForDirtqueueWorker();
		assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[]{});
		testContext.publish(timestamp++);
	}

	/**
	 * Check if an action is in the publish queue for all of the selected channels and for no other channel
	 * @param typeid event object type
	 * @param objid	event object id
	 * @param action	action
	 * @param channels	array of node ids where the event is expected to be contained
	 * @throws NodeException
	 */
	public void assertInPublishqueue(final int typeid, final int objid, final Action action, int[] channels) throws NodeException {
		final Set<Integer> results = new HashSet<Integer>();
		DBUtils.executeStatement("SELECT channel_id FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND action = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, typeid);
				stmt.setInt(2, objid);
				stmt.setString(3, action.toString());
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while(rs.next()) {
					results.add(rs.getInt(1));
				}
			}
		});
		for (int channel:channels) {
			assertTrue("Channel " + channel + " expected to be affected, but isn't.", results.contains(channel));
		}
		assertEquals("Extra channels affected", channels.length, results.size());
	}

	/**
	 * Check for the existence of a search string in the rendered page source in the publish table
	 * @param visible	whether the string should be in the source or not
	 * @param pageId	the id of the page to look for
	 * @param nodeId	the id of the node to look for
	 * @param searchString	the search string
	 * @throws NodeException
	 */
	public void assertInPageSource(final boolean visible, final int pageId, final int nodeId, final String searchString) throws NodeException {
		DBUtils.executeStatement("select source from publish where page_id = ? and node_id = ? and active = 1", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, pageId);
				stmt.setInt(2, channelId);
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				assertTrue("There is no rendered page",rs.next());
				String source = rs.getString(1);
				assertNotNull("There is no source", source);
				assertTrue("Expectation about existence of search string not met", source.contains(searchString) == visible);
				assertFalse("There should be only one page", rs.next());
			}
		});
	}
}
