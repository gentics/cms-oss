package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.AbstractTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.Page;

/**
 * Transactional to unlock contents on transaction commit
 */
public class UnlockContentTransactional extends AbstractTransactional {

	/**
	 * Content id to unlock
	 */
	protected int contentId;

	/**
	 * Create a transactional for the given contentid
	 * @param contentId content id
	 */
	public UnlockContentTransactional(int contentId) {
		this.contentId = contentId;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.Transactional#onDBCommit(com.gentics.lib.base.factory.Transaction)
	 */
	public void onDBCommit(Transaction t) throws NodeException {
		PreparedStatement pst = null;

		try {
			pst = t.prepareUpdateStatement("UPDATE content SET locked = 0, locked_by = 0 WHERE id = ? AND locked_by = ?");
			pst.setInt(1, contentId);
			pst.setInt(2, t.getUserId());
			pst.executeUpdate();
			ActionLogger.logCmd(ActionLogger.UNLOCK, Content.TYPE_CONTENT, contentId, 0, "unlock-java");
			Content content = t.getObject(Content.class, contentId);
			if (content != null) {
				for (Page page : content.getPages()) {
					ActionLogger.logCmd(ActionLogger.UNLOCK, Page.TYPE_PAGE, page.getId(), 0, "unlock-java");
				}
			}
		} catch (SQLException e) {
			throw new NodeException("Error while unlocking {Content " + contentId + "}", e);
		} finally {
			t.closeStatement(pst);

			// dirt the cache
			t.dirtObjectCache(Content.class, contentId);

			PageFactory.unsetContentLocked(contentId);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.Transactional#onTransactionCommit(com.gentics.lib.base.factory.Transaction)
	 */
	public boolean onTransactionCommit(Transaction t) {
		return false;
	}
}
