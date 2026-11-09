package com.gentics.contentnode.server;

import java.io.File;
import java.net.URL;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.auth.filter.AbstractSSOFilter;
import com.gentics.contentnode.auth.filter.KeycloakIntegrationFilter;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;

import jakarta.servlet.DispatcherType;

/**
 * Implementation of {@link ServletContextHandlerService} which adds the keycloak filters, if feature {@link Feature#KEYCLOAK} is enabled.
 */
public class KeycloakServletContextHandlerService implements ServletContextHandlerService {
	/**
	 * Name of the OIDC Filter
	 */
	public final static String OIDC_FILTER_NAME = "Keycloak OIDC Filter";

	/**
	 * Name of the Integration Filter
	 */
	public final static String INTEGRATION_FILTER_NAME = "Keycloak Gentics CMS Integration Filter";

	/**
	 * Format of the keycloak certs URL (without a port)
	 */
	private static final String CERTS_URL_FORMAT = "%s://%s%s/realms/%s/protocol/openid-connect/certs";

	/**
	 * Format of the keycloak certs URL (with a port)
	 */
	private static final String CERTS_URL_FORMAT_WITH_PORT = "%s://%s:%d%s/realms/%s/protocol/openid-connect/certs";

	/**
	 * Format for the expected issuer (without a port)
	 */
	private static final String ISSUER_FORMAT = "%s://%s%s/realms/%s";

	/**
	 * Format for the expected issuer (with a port)
	 */
	private static final String ISSUER_FORMAT_WITH_PORT = "%s://%s:%d%s/realms/%s";

	/**
	 * Logger
	 */
	protected static NodeLogger log = NodeLogger.getNodeLogger(KeycloakServletContextHandlerService.class);

	/**
	 * Serlvet context handler (stored, so that we can add/change filters also in {@link #onReloadConfiguration()})
	 */
	protected ServletContextHandler context;

	@Override
	public void init(ServletContextHandler context) {
		this.context = context;
		addFilterIfFeatureEnabled();
	}

	@Override
	public void onReloadConfiguration() {
		addFilterIfFeatureEnabled();
	}

	/**
	 * Add the filters, if the feature is enabled
	 */
	protected void addFilterIfFeatureEnabled() {
		final var featureIsEnabled = NodeConfigRuntimeConfiguration.isFeature(Feature.KEYCLOAK);
		final var AUTH_PATH = "/rest/auth/ssologin/*";

		if (featureIsEnabled) {
			Map<String, String> keycloakPreferences = NodeConfigRuntimeConfiguration
					.getPreferences()
					.getPropertyObject("keycloak");

			if (keycloakPreferences == null) {
				log.warn("Keycloak features is enabled but not configured.");
				return;
			}

			// TODO: also check that auth-server-url and realm are set
			ServletHandler servletHandler = context.getServletHandler();

			try {
				URL url = new URL(keycloakPreferences.get("auth-server-url"));
				String proto = url.getProtocol();
				String host = url.getHost();
				int port = url.getPort();
				boolean usesDefaultPort = false;
				File keysFile = null;

				if (port == -1) {
					usesDefaultPort = true;
				}
				String keysFilePath = keycloakPreferences.get("keys");
				if (!StringUtils.isBlank(keysFilePath)) {
					keysFile = new File(ConfigurationValue.CONF_PATH.get(), keysFilePath);
					if (!keysFile.isFile()) {
						keysFile = null;
					}
				}

				String realmName = keycloakPreferences.get("realm");

				// get the optional path prefix
				String prefix = url.getPath();
				if (!StringUtils.isBlank(prefix)) {
					// make sure the prefix starts with /, but does not end with /
					prefix = StringUtils.prependIfMissing(prefix, "/");
					prefix = StringUtils.removeEnd(prefix, "/");
				}

				// create the URL for downloading the keys from keycloak and pass onto the filter
				String certsUrl = Optional.ofNullable(keysFile).map(f -> f.toURI().toString())
						.orElse(usesDefaultPort ? String.format(CERTS_URL_FORMAT, proto, host, prefix, realmName)
								: String.format(CERTS_URL_FORMAT_WITH_PORT, proto, host, port, prefix, realmName));
				String issuer = usesDefaultPort ? String.format(ISSUER_FORMAT, proto, host, prefix, realmName)
						: String.format(ISSUER_FORMAT_WITH_PORT, proto, host, port, prefix, realmName);

				addFilter(servletHandler, INTEGRATION_FILTER_NAME, KeycloakIntegrationFilter.class, AUTH_PATH, EnumSet.of(DispatcherType.REQUEST), keycloakServletFilterHolder -> {
					keycloakServletFilterHolder.setInitParameter(KeycloakIntegrationFilter.INIT_KEYCLOAK_URL_PARAM, certsUrl);
					keycloakServletFilterHolder.setInitParameter(KeycloakIntegrationFilter.INIT_KEYCLOAK_ISSUER, issuer);

					if (keycloakPreferences.containsKey("user_created_callback")) {
						keycloakServletFilterHolder.setInitParameter(AbstractSSOFilter.INIT_CALLBACK,
								ObjectTransformer.getString(keycloakPreferences.get("user_created_callback"), null));
					}

					if (keycloakPreferences.containsKey("init_groups")) {
						keycloakServletFilterHolder.setInitParameter(AbstractSSOFilter.INIT_GROUPS_PARAM, "keycloak.init_groups");
					}

					if (keycloakPreferences.containsKey("sync_groups")) {
						keycloakServletFilterHolder.setInitParameter(AbstractSSOFilter.INIT_GROUPS_SYNC,
								ObjectTransformer.getString(keycloakPreferences.get("sync_groups"), null));
					}
				});
			} catch (Exception e) {
				log.error("Error while adding Keycloak filters: " + e.getMessage(), e);
			}

			log.info("Keycloak filter added");
		}
	}
}
