package com.gentics.contentnode.rest.resource.impl;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.response.UsersnapResponse;
import com.gentics.contentnode.rest.resource.UsersnapResource;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

@Path("/usersnap")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@RequiredFeature(Feature.USERSNAP)
@RequiredPerm(type=PermHandler.TYPE_USERSNAP, bit=PermHandler.PERM_VIEW)
public class UsersnapResourceImpl implements UsersnapResource {

	@GET
	@Override
	public UsersnapResponse getSettings() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Map<String, Object> settingsMap = NodeConfigRuntimeConfiguration.getPreferences().getPropertyMap("usersnap");
			JsonNode settings = null;
			if (settingsMap != null) {
				ObjectMapper mapper = new ObjectMapper();
				settings = mapper.convertValue(settingsMap, JsonNode.class);
			}
			trx.success();
			return new UsersnapResponse().setSettings(settings);
		}
	}
}
