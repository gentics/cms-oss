/**
 *
 */
package com.gentics.contentnode.security;

import java.util.function.Supplier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.validator.routines.InetAddressValidator;

import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

import net.ripe.commons.ip.Ipv4;
import net.ripe.commons.ip.Ipv4Range;
import net.ripe.commons.ip.Ipv6;
import net.ripe.commons.ip.Ipv6Range;
import org.jvnet.hk2.annotations.Service;

/**
 * @author clemens
 */
@Service
public class AccessControlService {

	private static NodeLogger log;

	/**
	 * this key identifies the servlet
	 */
	protected String configurationKey;

	/**
	 * shall commands be secured
	 */
	protected boolean commandsSecured;

	/**
	 * List of allowed hosts (or ip addresses), when the access to commands is secured
	 */
	protected List<String> allowedHosts = new ArrayList<>();

	private NodePreferences nodePreferences;

	final String PROPERTY_PREFIX = "config.accesscontrol";

	/**
	 * generate a new instance using provided configuration. configuration parameters read are -
	 * accesscontrol.[key].secured - accesscontrol.[key].allowedfrom
	 *
	 * @param servletConfigKey        key which identifies servlet specific config settings
	 * @param configurationProperties servlet config file
	 */
	public AccessControlService(String servletConfigKey) {
		nodePreferences = NodeConfigRuntimeConfiguration.getPreferences();
		this.configurationKey = servletConfigKey;

		init();
	}

	public AccessControlService(String servletConfigKey, Supplier<NodePreferences> preferencesSupplier) {
		nodePreferences = preferencesSupplier.get();
		this.configurationKey = servletConfigKey;

		init();
	}

	private void init() {
		log = NodeLogger.getNodeLogger(getClass());

		commandsSecured = Boolean.parseBoolean(
				getCustomPropertyOrDefault("secured"));

		String[] allowedFrom = StringUtils.splitString(
				getCustomPropertyOrDefault("allowedfrom"), ',');

		for (String s : allowedFrom) {
			allowedHosts.add(s.trim());
		}
	}

	public AccessControlService() {
	}

	private String getCustomPropertyOrDefault(String propertyKey) {
		// First check servletConfigKey custom property
		String configurationProperty = nodePreferences.getProperty(
				String.format("%s.%s.%s", PROPERTY_PREFIX, this.configurationKey, propertyKey));

		if (configurationProperty != null) {
			return configurationProperty;
		}
		String defaultConfigurationProperty = nodePreferences.getProperty(
				String.format("%s.default.%s", PROPERTY_PREFIX, propertyKey));

		if (defaultConfigurationProperty == null) {
			log.warn(String.format("Neither default nor custom property for '%s.%s' is specified",
					PROPERTY_PREFIX, propertyKey));
			return "";
		}

		return defaultConfigurationProperty;
	}


	/**
	 * Check whether the given IP is in the allowed hosts white list The allowedHosts lists can
	 * contain IPs and subnets in IPv4 or IPv6 format.
	 *
	 * @param address      The IP address in IPv4 or IPv6 format
	 * @param allowedHosts collection of allowed hosts
	 * @return True if the given IP matches one of the allowed entries, false otherwise
	 */

	public static boolean isIpAddressInList(String address, Collection<String> allowedHosts) {
		boolean isIpv6 = address.contains(":");

		if (isIpv6) {
			// We got a IPv6 address
			Ipv6 ipv6 = Ipv6.of(address);

			for (String allowedHostsEntry : allowedHosts) {
				try {
					if (allowedHostsEntry.contains("/")
							&& Ipv6Range.parse(allowedHostsEntry).contains(ipv6)) {
						return true;
					} else if (InetAddressValidator.getInstance().isValidInet6Address(allowedHostsEntry)
							&& Ipv6.of(allowedHostsEntry).equals(ipv6)) {
						return true;
					}
				} catch (IllegalArgumentException ignored) {
					// invalid address is not allowed
				}
			}
		} else {
			// We got a IPv4 address
			Ipv4 ipv4 = Ipv4.of(address);

			for (String allowedHostsEntry : allowedHosts) {
				try {
					if (allowedHostsEntry.contains("/")) {
						return Ipv4Range.parse(allowedHostsEntry).contains(ipv4);
					} else if (InetAddressValidator.getInstance().isValidInet4Address(allowedHostsEntry)
							&& Ipv4.of(allowedHostsEntry).equals(ipv4)) {
						return true;
					}
				} catch (IllegalArgumentException ignored) {
					// invalid address is not allowed
				}
			}
		}
		return false;
	}

	/**
	 * Check whether the given hostname is in the allowed hosts white list
	 *
	 * @param hostname     hostname
	 * @param allowedHosts collection of allowed hosts
	 * @return True if the given hostname matches one of the allowed entries, false otherwise
	 */
	public static boolean isHostInList(String hostname, Collection<String> allowedHosts) {
		for (String allowedHostsEntry : allowedHosts) {
			if (allowedHostsEntry.equalsIgnoreCase(hostname)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * verify if access to resource is granted
	 *
	 * @param request  the HttpServletRequest object to be check if access should be granted
	 * @param response the servlet response - error message will be added if needed
	 * @return true if access is granted
	 */
	public boolean verifyAccess(HttpServletRequest request, HttpServletResponse response) {
		if (!commandsSecured) {
			return true;
		}
		String remoteAddress = getCleanedAddress(request.getRemoteAddr());
		String remoteHost = getCleanedAddress(request.getRemoteHost());

		if (!this.isIpAddressInList(remoteAddress) && !this.isHostInList(remoteHost)) {
			log.error(String.format(
					"Access from remote address {%s} (host {%s}) forbidden - Allowed hosts: {%s} - Configuration: accesscontrol.%s",
					request.getRemoteAddr(),
					request.getRemoteHost(),
					allowedHosts,
					configurationKey));

			if (response != null) {
				try {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
				} catch (IOException e) {
					log.error("could not add error code to response", e);
				}
			}
			return false;
		}

		return true;
	}

	private String getCleanedAddress(String address) {
		return address
				.replace("[", "")
				.replace("]", "")
				.trim();
	}

	/**
	 * Check whether the given IP is in the allowed hosts white list The allowedHosts lists can
	 * contain IPs and subnets in IPv4 or IPv6 format.
	 *
	 * @param address The IP address in IPv4 or IPv6 format
	 * @return True if the given IP matches one of the allowed entries, false otherwise
	 */
	public boolean isIpAddressInList(String address) {
		return isIpAddressInList(address, this.allowedHosts);
	}

	/**
	 * Check whether the given hostname is in the allowed hosts white list
	 *
	 * @param hostname Host name
	 * @return True if the given hostname matches one of the allowed entries, false otherwise
	 */
	public boolean isHostInList(String hostname) {
		return isHostInList(hostname, this.allowedHosts);
	}
}

