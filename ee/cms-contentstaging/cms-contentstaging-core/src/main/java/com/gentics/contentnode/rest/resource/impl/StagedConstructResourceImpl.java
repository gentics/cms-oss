package com.gentics.contentnode.rest.resource.impl;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.StagedReferencedConstruct;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.staging.ImplementationHashResponse;
import com.gentics.contentnode.rest.resource.StagedConstructResource;
import com.gentics.contentnode.rest.util.MiscStagingUtils;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/construct")
@Authenticated
public class StagedConstructResourceImpl implements StagedConstructResource {

	@Override
	@GET
	@Path("/{id}/hash")
	public ImplementationHashResponse implementationHash(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return MiscStagingUtils.getImplementationHash(com.gentics.contentnode.object.Construct.class, StagedReferencedConstruct::new, id);
		}
	}
}
