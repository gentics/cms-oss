/*
 * @author tobiassteiner
 * @date Jan 30, 2011
 * @version $Id: PolicyMapResource.java,v 1.1.2.1 2011-02-10 13:43:31 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.resource.impl;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.PolicyGroupResponse;
import com.gentics.contentnode.rest.model.response.PolicyResponse;
import com.gentics.contentnode.rest.resource.PolicyMapResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.validation.ValidatorFactory;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyGroup;
import com.gentics.contentnode.validation.map.PolicyMap;
import com.gentics.lib.etc.StringUtils;

/**
 * API for reading from the policy map.
 */
@Path("/policyMap")
@Consumes("*/*")
public class PolicyMapResourceImpl extends AuthenticatedContentNodeResource implements PolicyMapResource {

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PolicyMapResource#getPolicyGroup(int)
	 */
	@GET
	@Path("/partType/{typeId}/policyGroup")
	public PolicyGroupResponse getPolicyGroup(@PathParam("typeId") int typeId) throws EntityNotFoundException {
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

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PolicyMapResource#getPolicy(java.lang.String)
	 */
	@GET
	@Path("/policy")
	public PolicyResponse getPolicy(@QueryParam("uri") String uri) throws EntityNotFoundException, URISyntaxException {
		if (StringUtils.isEmpty(uri)) {
			throw new EntityNotFoundException("A valid `uri' parameter must be provided to retrieve a policy");
		}
		ValidatorFactory factory = ValidatorFactory.newInstance();
		Policy policy = factory.getPolicyMap().getPolicyByURI(new URI(uri));

		if (null == policy) {
			throw new EntityNotFoundException("Policy with given uri not found in policy map: `" + uri + "'");
		}
		return ModelBuilder.getPolicyResponse(policy);
	}
}
