package com.gentics.contentnode.init;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.lib.db.SQLExecutor;

/**
 * Background job that hashes the already with md5 encoded passwords with encrypt.
 * This is done to ensure that the weak md5 passwords are protected until the users login
 * the first time and bcrypt only hashing is applied.
 */
public class BcryptPasswords extends InitJob {
	@Override
	public final void execute() throws NodeException {
		if (logger.isInfoEnabled()) {
			logger.info("Starting job " + getClass().getName());
		}
		Transaction t = null;

		try {
			t = TransactionManager.getCurrentTransaction();

			if (logger.isInfoEnabled()) {
				logger.info("Starting to hash all systemuser passwords with bcrypt");
			}

			// Select all passwords, filter out already bcrypt-hashed ones
			// They are easy to distinct, they start with a "$"
			DBUtils.executeStatement("SELECT id, password FROM systemuser"
					+ " WHERE password NOT LIKE '$%' AND password NOT LIKE 'leg-%'", new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					int numRowsChanged = 0;

					while (rs.next()) {
						int userId = rs.getInt("id");
						String md5EncodedPassword = rs.getString("password");

						// "leg" stands for legacy and identifies legacy passwords
						// that are currently double hashed with bcrypt(md5(password))
						// but will be re-hashed with bcrypt only on first user login.
						String bcryptMd5EncodedPassword =
								SystemUserFactory.LEGACY_PASSWORD_PREFIX + SystemUserFactory.hashPassword(md5EncodedPassword, userId);

						numRowsChanged += DBUtils.executeUpdate(
								"UPDATE systemuser SET password = ? WHERE id = ?",
								new Object[] {bcryptMd5EncodedPassword, userId});
					}

					if (logger.isInfoEnabled()) {
						logger.info("Password hashing with bcrypt finished. "
								+ numRowsChanged + " passwords changed.");
					}
				}
			});
		} finally {
			try {
				t.commit(false);
			} catch (TransactionException ignored) {
				if (logger.isWarnEnabled()) {
					logger.warn("BcryptPasswords: TransactionException occured ");
				}
			}
		}
	}
}
