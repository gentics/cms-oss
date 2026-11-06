package com.gentics.contentnode.rest.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.gentics.contentnode.rest.model.request.LocalizeRequest;
import com.gentics.contentnode.rest.model.request.MultiPushToMasterRequest;
import com.gentics.contentnode.rest.model.request.MultiUnlocalizeRequest;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LocalizationInfo;
import com.gentics.contentnode.rest.model.response.LocalizationsResponse;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse;

/**
 * Resource for handling multichannelling aspects of templates.
 * @HTTP 401 No valid sid and session secret cookie were provided.
 * @HTTP 403 User has insufficient permissions.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/template")
public interface MCTemplateResource {
	/**
	 * Get objects which are directly or indirectly linked by the specified templates,
	 * which can be synchronised to a master channel along with the templates themselves.
	 *
	 * Linked objects are pages, files and images specified in <code>UrlPartTypes</code> of
	 * content or object tags.
	 *
	 * Direct dependencies are in the {@link ReferencedSyncableObjectsListResponse#getPages() pages},
	 * {@link ReferencedSyncableObjectsListResponse#getFiles() files} and
	 * {@link ReferencedSyncableObjectsListResponse#getImages() images} files of the response. Indirect
	 * dependencies are mapped in {@link ReferencedSyncableObjectsListResponse#getDependencies dependencies}.
	 *
	 * @param templateIds The IDs of the templates to get linked objects for
	 * @param srcNodeId The channel ID of the source channel
	 * @param dstNodeId The channel ID of the target channel
	 * @throws Exception
	 */
	@GET
	@Path("/usage/syncableObjects")
	ReferencedSyncableObjectsListResponse getSyncableObjects(
			@QueryParam("ids") List<Integer> templateIds,
			@QueryParam("srcNodeId") Integer srcNodeId,
			@QueryParam("dstNodeId") Integer dstNodeId) throws Exception;

	/**
	 * Push a template from a channel into a master
	 * @param id id of the template
	 * @param request request
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/push2master/{id}")
	GenericResponse pushToMaster(@PathParam("id") Integer id, PushToMasterRequest request) throws Exception;

	/**
	 * Push a list of templates from a channel into a master
	 * @param request request
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/push2master")
	GenericResponse pushToMaster(MultiPushToMasterRequest request) throws Exception;

	/**
	 * Localize the given template
	 * @param id ID of the template
	 * @param request request
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/localize/{id}")
	GenericResponse localize(@PathParam("id") String id, LocalizeRequest request) throws Exception;

	/**
	 * Unlocalize the given template
	 * @param id id of the template to unlocalize
	 * @param request request
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/unlocalize/{id}")
	GenericResponse unlocalize(@PathParam("id") Integer id, UnlocalizeRequest request) throws Exception;

	/**
	 * Unlocalize templates in a channel
	 * @param request request
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/unlocalize")
	GenericResponse unlocalize(MultiUnlocalizeRequest request) throws Exception;

	/**
	 * Get localization info for a list of templates for a given channel and all master channels
	 * @param ids list of template ids
	 * @param nodeId id of the node to start with
	 * @return localization info
	 * @throws Exception
	 */
	@GET
	@Path("/localizationinfo")
	LocalizationInfo getLocalizationInfo(@QueryParam("id") List<Integer> ids, @QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Get localization info for a template for a given channel and all master channels
	 * @param id id of the template. This can either be the local or global id of the template.
	 * @param nodeId id of the node to start with
	 * @return localization info
	 * @throws Exception
	 */
	@GET
	@Path("/localizationinfo/{id}")
	LocalizationInfo getLocalizationInfo(@PathParam("id") String id,
			@QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Get the IDs an their respective node IDs of localized copies of the
	 * specified template.
	 *
	 * @param id The ID of the master template.
	 *
	 * @return A map of template IDs to node IDs of localized copies.
	 * @throws Exception
	 */
	@GET
	@Path("/localizations/{id}")
	LocalizationsResponse localizations(@PathParam("id") Integer id) throws Exception;
}
