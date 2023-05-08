package com.gentics.contentnode.rest.resource.impl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.StagedReferencedDatasource;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.staging.ImplementationHashResponse;
import com.gentics.contentnode.rest.resource.StagedDatasourceResource;
import com.gentics.contentnode.rest.util.MiscStagingUtils;

@Produces({ "application/json; charset=UTF-8", "application/xml; charset=UTF-8"})
@Authenticated
@Path("/datasource")
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = com.gentics.contentnode.object.Datasource.TYPE_DATASOURCE, bit = PermHandler.PERM_VIEW)
public class StagedDatasourceResourceImpl implements StagedDatasourceResource {
	@Override
	@GET
	@Path("/{id}/hash")
	public ImplementationHashResponse implementationHash(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return MiscStagingUtils.getImplementationHash(com.gentics.contentnode.object.Datasource.class, StagedReferencedDatasource::new, id);
		}
	}
}
