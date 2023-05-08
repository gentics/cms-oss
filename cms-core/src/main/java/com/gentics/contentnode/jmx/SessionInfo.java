package com.gentics.contentnode.jmx;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;

/**
 * SessionInfo MBean
 */
public class SessionInfo implements SessionInfoMBean {
	@Override
	public int getSessionCount() {
		try {
			return Trx.supply(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				int sessionAge = ObjectTransformer.getInt(t.getNodeConfig().getDefaultPreferences().getProperty("session_age"), 3600);
				final int allowedSince = t.getUnixTimestamp() - sessionAge;

				return DBUtils.select("SELECT COUNT(*) sessions FROM systemsession WHERE since > ? AND secret != '' AND user_id > 1", st -> st.setInt(1, allowedSince), rs -> {
					if (rs.next()) {
						return rs.getInt("sessions");
					} else {
						return -1;
					}
				});
			});
		} catch (NodeException e) {
			return -1;
		}
	}
}
