package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionalDirtCache;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test class for moving a page from a node to another
 * @author drainer
 *
 */
public class MovePageTest extends AbstractInstantPublishingTestSandboxTest {

	public static int timestampCounter = 1;

	private static boolean helper_flag;

	/**
	 * This test moves a page to another node and takes it offline. It ensures
	 * that the entries in the publishqueue are correct.
	 * @throws Exception
	 */
	@Test
	public void testMovePage() throws Exception {
		testContext.getContext().startTransaction();

		Node source = ContentNodeTestDataUtils.createNode("src", "src", PublishTarget.CONTENTREPOSITORY);
		Node target = ContentNodeTestDataUtils.createNode("src", "src", PublishTarget.NONE);

		Template template = createTemplate(source.getFolder());
		Page page = createPage(source.getFolder(), template, "pageToMove");

		Transaction t = TransactionManager.getCurrentTransaction();
		page.publish();
		t.commit(false);

		//do an initial publishing run
		testContext.getContext().publish(false, true);
		testContext.getContext().startTransaction();

		//move the page
		movePage(page, target);
		testContext.getContext().startDirtQueueWorker();
		testContext.waitForDirtqueueWorker();

		boolean checkPublishQueueEntries = testPublishQueueEntriesAfterMove(page.getId(), source.getId(), target.getId(), false);

		assertTrue("Wrong entries in publishqueue", checkPublishQueueEntries);

		//take page offline
		t = TransactionManager.getCurrentTransaction();
		Page updatedPage = t.getObject(Page.class, page.getId(), true);
		updatedPage.takeOffline();
		t.commit(false);
		testContext.getContext().startDirtQueueWorker();
		testContext.waitForDirtqueueWorker();

		checkPublishQueueEntries = testPublishQueueEntriesAfterMove(page.getId(), source.getId(), target.getId(), true);
		assertTrue("Wrong entries in publishqueue", checkPublishQueueEntries);
	}

	/**
	 * Move a page to a target node and trigger the correct events
	 * @param page the page to move
	 * @param target the target node
	 * @throws NodeException
	 */
	private void movePage(Page page, Node target) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder targetFolder = target.getFolder();
		Folder sourceFolder = page.getFolder();

		DBUtils.executeUpdate("UPDATE page set folder_id = ? where id = ?", new Object[] { targetFolder.getId(), page.getId() });
		t.addTransactional(new TransactionalDirtCache(Page.class, page.getId()));
		t.commit(false);

		Page updatedPage = t.getObject(Page.class, page.getId(), true);
		t.addTransactional(new TransactionalTriggerEvent(updatedPage, new String[] { sourceFolder.getNode().getId().toString() }, Events.MOVE));
		t.addTransactional(new TransactionalTriggerEvent(sourceFolder, new String[] { "pages" }, Events.UPDATE));
		t.addTransactional(new TransactionalTriggerEvent(targetFolder, new String[] { "pages" }, Events.UPDATE));

		ActionLogger.logCmd(ActionLogger.MOVE, Page.TYPE_PAGE, page.getId(), targetFolder.getId(), "Folder.move()");
		t.commit(false);
	}

	/**
	 * This method looks for the correct entries in the publishqueue table after
	 * moving a page from a source node to a target node. 
	 * @param pageId the page id
	 * @param sourceNodeId the source node id
	 * @param targetNodeId the target node id
	 * @param isOffline set true if the page was set offline after moving
	 * @return true if testing was successful
	 * @throws NodeException
	 */
	public boolean testPublishQueueEntriesAfterMove(final int pageId, final int sourceNodeId, final int targetNodeId, final boolean isOffline)
			throws NodeException {

		helper_flag = false;
		DBUtils.executeStatement("SELECT * FROM publishqueue WHERE obj_type = ? AND obj_id = ?", new SQLExecutor() {

			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				// TODO Auto-generated method stub
				stmt.setString(1, "" + Page.TYPE_PAGE);
				stmt.setString(2, "" + pageId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				// TODO Auto-generated method stub
				boolean removeFromSource = false;
				boolean moveToTarget = false;
				boolean isSetOffline = false;

				while (rs.next()) {
					String action = rs.getString("action");
					String channelId = rs.getString("channel_id");
					if (action.toUpperCase().equals("MOVE") && channelId.equals("" + targetNodeId)) {
						moveToTarget = true;
					}
					if (action.toUpperCase().equals("REMOVE") && channelId.equals("" + sourceNodeId)) {
						removeFromSource = true;
					}
					if (action.toUpperCase().equals("OFFLINE") && channelId.equals("" + targetNodeId)) {
						isSetOffline = true;
					}
				}

				if (isOffline) {
					helper_flag = removeFromSource && isSetOffline && !moveToTarget;
				} else {
					helper_flag = removeFromSource && moveToTarget;
				}
			}
		});
		return helper_flag;
	}

}
