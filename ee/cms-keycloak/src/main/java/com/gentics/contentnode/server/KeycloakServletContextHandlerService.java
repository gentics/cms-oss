package com.gentics.contentnode.server;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.servlet.ServletContextHandler;

import com.gentics.contentnode.auth.filter.KeycloakIntegrationFilter;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of {@link ServletContextHandlerService} which adds the keycloak filters, if feature {@link Feature#KEYCLOAK} is enabled.
 */
public class KeycloakServletContextHandlerService implements ServletContextHandlerService {
	/**
	 * Logger
	 */
	protected static NodeLogger log = NodeLogger.getNodeLogger(KeycloakServletContextHandlerService.class);

	@Override
	public void init(ServletContextHandler context) {
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

			var keycloakOidcFilterHolder = context.addFilter(
					org.keycloak.adapters.servlet.KeycloakOIDCFilter.class,
					AUTH_PATH, EnumSet.of(DispatcherType.REQUEST));

			keycloakOidcFilterHolder.setInitParameter("keycloak.config.file",
					ConfigurationValue.UI_CONF_PATH.get() + "keycloak.json");

			var keycloakServletFilterHolder = context.addFilter(KeycloakIntegrationFilter.class,
					AUTH_PATH, EnumSet.of(DispatcherType.REQUEST));

			if (keycloakPreferences.containsKey("user_created_callback")) {
				keycloakServletFilterHolder.setInitParameter("userCreatedCallback",
						keycloakPreferences.get("user_created_callback"));
			}

			if (keycloakPreferences.containsKey("init_groups")) {
				keycloakServletFilterHolder.setInitParameter("initGroups",
						keycloakPreferences.get("init_groups"));
			}
			log.info("Keycloak filter added");
		}
	}
}
