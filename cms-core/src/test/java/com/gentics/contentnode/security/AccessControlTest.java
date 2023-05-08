package com.gentics.contentnode.security;

import static com.gentics.contentnode.runtime.ConfigurationValue.CONF_FILES;
import static com.gentics.contentnode.runtime.ConfigurationValue.CONF_PATH;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.PropertyNodeConfig;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for IP whitelisting with AccessControl
 */
public class AccessControlTest {

	private AccessControlService accessControlService;

	@BeforeClass
	public static void setup() {
		System.setProperty(CONF_PATH.getSystemPropertyName(), "src/test/resources/");
	}

	/**
	 * Test whitelisting of IPs with isIpAddressInList
	 */
	@Test
	public void testAccessByIp() {
		configureAccess("whitelist.yml");

		assertIp("127.0.0.1", true);
		assertIp("192.168.32.5", true);
		assertIp("10.5.5.5", false);
		assertIp("::1", true);
		assertIp("FF:80::5", true);
		assertIp("FF:85::1", false);
	}

	/**
	 * Test whitelisting of hostnames with isHostInList
	 */
	@Test
	public void testAccessByHostname() {
		configureAccess("host.yml");

		assertHostname("allowed-host", true);
		assertHostname("josef", false);
	}

	/**
	 * Assert whether the given IPv4 or IPv6 address is whitelisted
	 *
	 * @param ip          The IPv4/IPv6 address
	 * @param shouldMatch True or false
	 */
	private void assertIp(String ip, boolean shouldMatch) {
		if (shouldMatch) {
			assertTrue("IP address must match", accessControlService.isIpAddressInList(ip));
		} else {
			assertFalse("IP address must NOT match", accessControlService.isIpAddressInList(ip));
		}
	}

	/**
	 * Assert whether the given hostname is whitelisted
	 *
	 * @param hostname    The hostname
	 * @param shouldMatch True or false
	 */
	private void assertHostname(String hostname, boolean shouldMatch) {
		if (shouldMatch) {
			assertTrue("Hostname must match", accessControlService.isHostInList(hostname));
		} else {
			assertFalse("Hostname must NOT match", accessControlService.isHostInList(hostname));
		}
	}

	private void configureAccess(String configFile) {
		System.setProperty(CONF_FILES.getSystemPropertyName(), "auth/" + configFile);
		accessControlService = new AccessControlService("default", () -> {
			try {
				Map<String, Object> data = NodeConfigRuntimeConfiguration
						.loadConfiguration();

				return new PropertyNodeConfig(data)
						.getDefaultPreferences();
			} catch (NodeException e) { throw new RuntimeException(e); }
		});
	}

}
