package com.gentics.contentnode.rest.resource.impl;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Resource for providing the keycloak configuration for the UI
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/keycloak")
public class KeycloakResource {
	/**
	 * Get the keycloak configuration.
	 * @return configuration in json format
	 */
	@GET
	public Response getConfiguration() {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.KEYCLOAK)) {
			ObjectNode configuration = new ObjectMapper().createObjectNode();
			NodePreferences preferences = NodeConfigRuntimeConfiguration.getPreferences();
			for (String key : Arrays.asList("auth-server-url", "realm", "resource")) {
				configuration.put(key, preferences.getProperty(String.format("keycloak.%s", key)));
			}

			configuration.put("showSSOButton", ObjectTransformer.getBoolean(preferences.getProperty("keycloak.show_sso_button"), false));

			return Response.ok(configuration, MediaType.APPLICATION_JSON_TYPE).build();
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}
}
