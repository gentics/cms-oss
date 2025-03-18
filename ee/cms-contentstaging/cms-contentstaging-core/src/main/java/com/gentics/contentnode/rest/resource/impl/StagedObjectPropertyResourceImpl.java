package com.gentics.contentnode.rest.resource.impl;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.StagedReferencedObjectTagDefinition;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.staging.ImplementationHashResponse;
import com.gentics.contentnode.rest.resource.ObjectPropertyResource;
import com.gentics.contentnode.rest.resource.StagedObjectPropertyResource;
import com.gentics.contentnode.rest.util.MiscStagingUtils;

/**
 * Resource implementation of {@link ObjectPropertyResource}
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/objectproperty")
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = ObjectTagDefinition.TYPE_OBJTAG_DEFS, bit = PermHandler.PERM_VIEW)
public class StagedObjectPropertyResourceImpl implements StagedObjectPropertyResource {
	@Override
	@GET
	@Path("/{id}/hash")
	public ImplementationHashResponse implementationHash(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return MiscStagingUtils.getImplementationHash(ObjectTagDefinition.class, StagedReferencedObjectTagDefinition::new, id);
		}
	}
}
