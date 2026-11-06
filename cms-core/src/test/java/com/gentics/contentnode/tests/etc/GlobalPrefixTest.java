package com.gentics.contentnode.tests.etc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.servlets.UdateChecker;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.genericexceptions.UnavailableException;

/**
 * Test cases for {@link UdateChecker#ensureGlobalPrefix()}
 */
public class GlobalPrefixTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Globalprefix, which is stored before every test
	 */
	public final static String STORED_PREFIX = "AB12";

	/**
	 * Store {@link #STORED_PREFIX} and make sure, that no globalprefix is configured
	 * @throws SQLException
	 */
	@Before
	public void setup() throws SQLException {
		System.clearProperty(ConfigurationValue.GLOBALPREFIX.getSystemPropertyName());
		DB.update(getHandle(), "DELETE FROM globalprefix");
		DB.update(getHandle(), "INSERT INTO globalprefix (globalprefix) VALUES (?)", new Object[] { STORED_PREFIX });
	}

	/**
	 * Remove the configured globalprefix
	 */
	@After
	public void tearDown() {
		System.clearProperty(ConfigurationValue.GLOBALPREFIX.getSystemPropertyName());
	}

	/**
	 * Test that a stored globalprefix is kept, if no globalprefix is configured
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testKeepStoredPrefix() throws NodeException, SQLException {
		UdateChecker.ensureGlobalPrefix();
		UdateChecker.ensureGlobalPrefix();

		assertThat(UdateChecker.getStoredGlobalPrefix()).as("Stored globalprefix").isEqualTo(STORED_PREFIX);
	}

	/**
	 * Test that a configured globalprefix replaces the stored globalprefix
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testConfiguredPrefixReplacesStoredPrefix() throws NodeException, SQLException {
		System.setProperty(ConfigurationValue.GLOBALPREFIX.getSystemPropertyName(), "CD34");

		UdateChecker.ensureGlobalPrefix();

		assertThat(UdateChecker.getStoredGlobalPrefix()).as("Stored globalprefix").isEqualTo("CD34");
	}

	/**
	 * Test that a configured globalprefix is stored, if no globalprefix is stored
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testConfiguredPrefixWithoutStoredPrefix() throws NodeException, SQLException {
		DB.update(getHandle(), "DELETE FROM globalprefix");
		System.setProperty(ConfigurationValue.GLOBALPREFIX.getSystemPropertyName(), "EF56");

		UdateChecker.ensureGlobalPrefix();

		assertThat(UdateChecker.getStoredGlobalPrefix()).as("Stored globalprefix").isEqualTo("EF56");
	}

	/**
	 * Test that a random globalprefix is stored, if neither a globalprefix is stored nor configured
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testRandomPrefixWithoutStoredPrefix() throws NodeException, SQLException {
		DB.update(getHandle(), "DELETE FROM globalprefix");

		UdateChecker.ensureGlobalPrefix();
		String generatedPrefix = UdateChecker.getStoredGlobalPrefix();
		assertThat(generatedPrefix).as("Stored globalprefix").matches("[0-9a-f]{4}");

		UdateChecker.ensureGlobalPrefix();
		assertThat(UdateChecker.getStoredGlobalPrefix()).as("Stored globalprefix after second call").isEqualTo(generatedPrefix);
	}

	/**
	 * Test that an invalid configured globalprefix is rejected and the stored globalprefix is not changed
	 * @throws SQLException
	 */
	@Test
	public void testInvalidConfiguredPrefix() throws SQLException {
		for (String invalid : new String[] { "ABCDE", "A.B", "A-B" }) {
			System.setProperty(ConfigurationValue.GLOBALPREFIX.getSystemPropertyName(), invalid);

			assertThatThrownBy(() -> UdateChecker.ensureGlobalPrefix()).as("Configured globalprefix " + invalid)
					.isInstanceOf(UnavailableException.class);
			assertThat(UdateChecker.getStoredGlobalPrefix()).as("Stored globalprefix").isEqualTo(STORED_PREFIX);
		}
	}

	/**
	 * Get the DB handle
	 * @return DB handle
	 */
	protected DBHandle getHandle() {
		return NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getSQLHandle(true).getDBHandle();
	}
}
