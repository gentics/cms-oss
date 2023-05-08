package com.gentics.contentnode.init;

import java.math.BigInteger;
import java.security.SecureRandom;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SystemUserFactory;

/**
 * Background job that generates new passwords for the
 * system and the gentics users.
 */
public class SecureSystemGenticsLogins extends InitJob {
	@Override
	public final void execute() throws NodeException {
		if (logger.isInfoEnabled()) {
			logger.info("Starting job " + getClass().getName());
		}
		Transaction t = null;

		try {
			t = TransactionManager.getCurrentTransaction();

			if (logger.isInfoEnabled()) {
				logger.info("Starting to generate new passwords for system and gentics");
			}

			setPasswordForSystemUser("system", SystemUserFactory.hashPassword(
					generate62CharactersPassword(), 0));
			setPasswordForSystemUser("gentics", SystemUserFactory.hashPassword(
					generate62CharactersPassword(), 0));
		} finally {
			try {
				t.commit(false);
			} catch (TransactionException e) {
				String message = "Error while generating new passwords";
				logger.error(message + ": " + e.getLocalizedMessage());
				throw new NodeException(message, e);
			}
		}
	}

	/**
	 * Generates a secure password that has a length of 62 characters
	 * @return Secure password
	 */
	protected String generate62CharactersPassword() {
		SecureRandom secureRandom = new SecureRandom();
		return new BigInteger(130, secureRandom).toString(32)
				+ new BigInteger(130, secureRandom).toString(32);
	}

	/**
	 * Sets a password for the specified user
	 */
	protected void setPasswordForSystemUser(final String userName, final String password) throws NodeException {
		DBUtils.executeUpdate("UPDATE systemuser SET password = ? WHERE login = ? LIMIT 1",
				new Object[] {password, userName});
	}
}
