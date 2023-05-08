package com.gentics.contentnode.rest.resource.impl;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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
