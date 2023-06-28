package com.gentics.contentnode.server;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.keycloak.adapters.servlet.KeycloakOIDCFilter;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.filter.AbstractSSOFilter;
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
	 * Name of the OIDC Filter
	 */
	public final static String OIDC_FILTER_NAME = "Keycloak OIDC Filter";

	/**
	 * Name of the Integration Filter
	 */
	public final static String INTEGRATION_FILTER_NAME = "Keycloak Gentics CMS Integration Filter";

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
			Map<String, Object> keycloakPreferences = NodeConfigRuntimeConfiguration
					.getPreferences()
					.getPropertyObject("keycloak");

			if (keycloakPreferences == null) {
				log.warn("Keycloak features is enabled but not configured.");
				return;
			}

			ServletHandler servletHandler = context.getServletHandler();

			try {
				addFilter(servletHandler, OIDC_FILTER_NAME, KeycloakOIDCFilter.class, AUTH_PATH, EnumSet.of(DispatcherType.REQUEST), keycloakOidcFilterHolder -> {
					keycloakOidcFilterHolder.setInitParameter("keycloak.config.file",
							ConfigurationValue.UI_CONF_PATH.get() + "keycloak.json");
				});

				addFilter(servletHandler, INTEGRATION_FILTER_NAME, KeycloakIntegrationFilter.class, AUTH_PATH, EnumSet.of(DispatcherType.REQUEST), keycloakServletFilterHolder -> {
					if (keycloakPreferences.containsKey("user_created_callback")) {
						keycloakServletFilterHolder.setInitParameter(AbstractSSOFilter.INIT_CALLBACK,
								ObjectTransformer.getString(keycloakPreferences.get("user_created_callback"), null));
					}

					if (keycloakPreferences.containsKey("init_groups")) {
						keycloakServletFilterHolder.setInitParameter(AbstractSSOFilter.INIT_GROUPS_PARAM,
								ObjectTransformer.getString(keycloakPreferences.get("init_groups"), null));
					}

					if (keycloakPreferences.containsKey("sync_groups")) {
						keycloakServletFilterHolder.setInitParameter(AbstractSSOFilter.INIT_GROUPS_SYNC,
								ObjectTransformer.getString(keycloakPreferences.get("sync_groups"), null));
					}
				});
			} catch (NodeException e) {
				log.error(e);
			}

			log.info("Keycloak filter added");
		}
	}
}
