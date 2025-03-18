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
import com.gentics.contentnode.object.StagedReferencedTemplate;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.staging.ImplementationHashResponse;
import com.gentics.contentnode.rest.resource.StagedTemplateResource;
import com.gentics.contentnode.rest.util.MiscStagingUtils;

/**
 * Resource used for loading, saving and manipulating GCN templates.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/template")
public class StagedTemplateResourceImpl implements StagedTemplateResource {

	@Override
	@GET
	@Path("/{id}/hash")
	public ImplementationHashResponse implementationHash(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return MiscStagingUtils.getImplementationHash(Template.class, StagedReferencedTemplate::new, id);
		}
	}
}
