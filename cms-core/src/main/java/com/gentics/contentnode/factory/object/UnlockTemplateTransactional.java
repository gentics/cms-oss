package com.gentics.contentnode.factory.object;

import static com.gentics.contentnode.factory.Trx.operate;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.AbstractTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Template;
import com.gentics.lib.log.NodeLogger;

/**
 * 
 */
public class UnlockTemplateTransactional extends AbstractTransactional {

	/**
	 * Template id to unlock
	 */
	protected int templateId;

	/**
	 * Create an instance for the given template id
	 * @param templateId template id
	 */
	public UnlockTemplateTransactional(int templateId) {
		this.templateId = templateId;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.Transactional#onDBCommit(com.gentics.lib.base.factory.Transaction)
	 */
	public void onDBCommit(Transaction t) throws NodeException {
		PreparedStatement pst = null;

		try {
			pst = t.prepareUpdateStatement("UPDATE template SET locked = 0, locked_by = 0 WHERE id = ? AND locked_by = ?");
			pst.setInt(1, templateId);
			pst.setInt(2, t.getUserId());
			pst.executeUpdate();
			ActionLogger.logCmd(ActionLogger.UNLOCK, Template.TYPE_TEMPLATE, templateId, 0, "unlock-java");
		} catch (SQLException e) {
			throw new NodeException("Error while unlocking {Template " + templateId + "}", e);
		} finally {
			t.closeStatement(pst);

			// dirt the cache
			t.dirtObjectCache(Template.class, templateId);

			TemplateFactory.unsetTemplateLocked(templateId);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.Transactional#onTransactionCommit(com.gentics.lib.base.factory.Transaction)
	 */
	public boolean onTransactionCommit(Transaction t) {
		return false;
	}

	@Override
	public void afterTransactionRollback(Transaction t) {
		try {
			operate(() -> TemplateFactory.unlock(templateId, t.getUserId()));
		} catch (NodeException e) {
			NodeLogger.getNodeLogger(getClass()).error(String.format("Error while unlocking template %d after rollback:", templateId), e);
		}
	}
}