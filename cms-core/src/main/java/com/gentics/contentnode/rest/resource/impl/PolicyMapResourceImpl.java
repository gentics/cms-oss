package com.gentics.contentnode.rest.resource.impl;

import java.net.URI;
import java.net.URISyntaxException;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.response.PolicyGroupResponse;
import com.gentics.contentnode.rest.model.response.PolicyResponse;
import com.gentics.contentnode.rest.resource.PolicyMapResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.validation.ValidatorFactory;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyGroup;
import com.gentics.contentnode.validation.map.PolicyMap;
import com.gentics.lib.etc.StringUtils;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

/**
 * API for reading from the policy map.
 */
@Path("/policyMap")
@Consumes("*/*")
@Authenticated
public class PolicyMapResourceImpl implements PolicyMapResource {

	@Override
	@GET
	@Path("/partType/{typeId}/policyGroup")
	public PolicyGroupResponse getPolicyGroup(@PathParam("typeId") int typeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			ValidatorFactory factory = ValidatorFactory.newInstance();
			PolicyMap.PartType partType = factory.getPolicyMap().getPartTypeById(typeId);
			PolicyGroup group;

			if (null == partType) {
				group = factory.getPolicyMap().getDefaultPolicyGroup();
			} else {
				group = partType.getPolicyGroup();
			}
			if (null == group) {
				throw new EntityNotFoundException("No part type with id `" + typeId + "' and no default policy group");
			}
			return ModelBuilder.getPolicyGroupResponse(group);
		}
	}

	@Override
	@GET
	@Path("/policy")
	public PolicyResponse getPolicy(@QueryParam("uri") String uri) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (StringUtils.isEmpty(uri)) {
				throw new EntityNotFoundException("A valid `uri' parameter must be provided to retrieve a policy");
			}
			ValidatorFactory factory = ValidatorFactory.newInstance();
			Policy policy = factory.getPolicyMap().getPolicyByURI(new URI(uri));

			if (null == policy) {
				throw new EntityNotFoundException("Policy with given uri not found in policy map: `" + uri + "'");
			}
			return ModelBuilder.getPolicyResponse(policy);
		} catch (URISyntaxException e) {
			throw new NodeException(e);
		}
	}
}
