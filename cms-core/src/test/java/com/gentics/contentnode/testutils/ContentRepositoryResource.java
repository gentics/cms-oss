package com.gentics.contentnode.testutils;

import java.sql.SQLException;

import org.junit.rules.ExternalResource;

import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.infrastructure.TestEnvironment;

public class ContentRepositoryResource extends ExternalResource {

	protected static final NodeLogger logger = NodeLogger.getNodeLogger(DBTestContext.class);
	private SQLUtils crDBUtils;

	@Override
	protected void before() throws Throwable {
		logger.debug("Before invoked. Setup starting..");
		setupCR(TestEnvironment.getRandomHash(8));
		logger.debug("Setup done.");
	}

	private void setupCR(String hash) throws JDBCMalformedURLException, SQLUtilException {
		crDBUtils = ContentNodeTestUtils.createCRDatabase(ContentRepositoryResource.class, hash);
	}

	@Override
	protected void after() {
		logger.debug("After invoked. Cleanup starting..");
		try {
			removeCR();
		} catch (Exception e) {
			logger.debug("Error while cleaning up.", e);
		}
		logger.debug("Cleanup done.");
	}

	/**
	 * Returns the sqlutils for the CR
	 * 
	 * @return
	 */
	public SQLUtils getCRTestUtils() {
		return crDBUtils;
	}

	/**
	 * Removes the CR (if possible)
	 * 
	 * @throws SQLUtilException
	 * @throws SQLException
	 */
	public void removeCR() throws SQLUtilException, SQLException {
		if (crDBUtils != null) {
			if (crDBUtils.getConnection() != null && crDBUtils.getConnection().isClosed()) {
				crDBUtils.connectDatabase();
			}
			logger.debug("Removing cr database");
			crDBUtils.removeDatabase();
			crDBUtils.disconnectDatabase();
			crDBUtils = null;
		}
	}

}
