package com.gentics.contentnode.init;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.IntegerColumnRetriever;

/**
 * Job that generates a new password for the node user if one was not
 * set yet
 */
public class GenerateNodeUserPassword extends InitJob {
	@Override
	public final void execute() throws NodeException {
		logger.info("Starting job " + getClass().getName());

		String newPassword = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			SystemUser nodeUser = findNodeUser(t);

			if (nodeUser == null) {
				throw new NodeException("Could not generate a new password for the user \"node\" because the user doesn't exist");
			}

			if (!nodeUser.getPassword().isEmpty()) {
				logger.info("The user \"node\" already has a password set");
				return;
			}

			nodeUser = t.getObject(nodeUser, true);

			// first try to get the node password from the configuration
			newPassword = ConfigurationValue.NODE_USER_PASSWORD.get();
			if (ObjectTransformer.isEmpty(newPassword)) {
				// if not set in the environment, set a new random password
				newPassword = generate26CharactersPassword();
			}
			nodeUser.setPassword(SystemUserFactory.hashPassword(newPassword, nodeUser.getId()));
			nodeUser.save();

			trx.success();
		}

		NodeConfigRuntimeConfiguration.runtimeLog.info("A new password has been generated for the user \"node\": \"" + newPassword + "\"");
		NodeConfigRuntimeConfiguration.runtimeLog.info("Please change this password after logging in the first time!");
	}

	/**
	 * Find the node user
	 *
	 * @param t A transaction
	 * @throws NodeException
	 */
	protected SystemUser findNodeUser(Transaction t) throws NodeException {
		IntegerColumnRetriever nodeUserId = new IntegerColumnRetriever("id");
		DBUtils.executeStatement("SELECT id FROM systemuser WHERE login = 'node'", nodeUserId);
		List<Integer> values = nodeUserId.getValues();
		if (values.isEmpty()) {
			return null;
		}

		return t.getObject(SystemUser.class, values.get(0));
	}

	/**
	 * Generates a secure password that has a length of 26 characters
	 * 
	 * @return Secure password
	 */
	protected String generate26CharactersPassword() {
		SecureRandom secureRandom = new SecureRandom();
		return new BigInteger(130, secureRandom).toString(32);
	}
}
