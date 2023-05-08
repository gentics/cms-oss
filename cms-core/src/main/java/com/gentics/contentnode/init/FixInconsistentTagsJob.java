package com.gentics.contentnode.init;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.lib.db.SQLExecutor;


/**
 * Background job that delete page tags that reference none existing constructs.
 */
public class FixInconsistentTagsJob extends InitJob {

	@Override
	public void execute() throws NodeException {
		Transaction transaction = null;
		final List<Integer> tagIds = new Vector<Integer>();

		try {
			transaction = TransactionManager.getCurrentTransaction();

			final Set<Integer> brokenContentTagIds = new HashSet<Integer>();

			// Get all IDs from broken content tags
			DBUtils.executeStatement("SELECT contenttag.id FROM contenttag"
					+ " LEFT JOIN construct ON construct.id = contenttag.construct_id"
					+ " WHERE construct.id IS NULL",
					new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						brokenContentTagIds.add(rs.getInt("id"));
					}
				}
			});

			for (Integer brokenContentTagId : brokenContentTagIds) {
				final Integer finalBrokenContentTagId = brokenContentTagId;

				// Cleanup some children tables
				for (String table : new String[] {"value", "ds", "ds_obj"}) {
					DBUtils.executeStatement("DELETE FROM " + table + " WHERE contenttag_id = ?",
							new SQLExecutor() {
						public void prepareStatement(PreparedStatement preparedStatement) throws SQLException {
							preparedStatement.setObject(1, finalBrokenContentTagId);
						}
					});
				}

				// Cleanup the table contenttag itself
				DBUtils.executeStatement("DELETE FROM contenttag WHERE id = ?",
						new SQLExecutor() {
					public void prepareStatement(PreparedStatement preparedStatement) throws SQLException {
						preparedStatement.setObject(1, finalBrokenContentTagId);
					}
				});
			}
		} catch (NodeException e) {
			throw new NodeException("Error while deleting inconsistent tags", e);
		} finally {
			if (transaction != null) {
				try {
					transaction.commit(false);
				} catch (TransactionException e) {
					throw new NodeException("Error while deleting inconsistent tags", e);
				}
			}
		}
	}
}
