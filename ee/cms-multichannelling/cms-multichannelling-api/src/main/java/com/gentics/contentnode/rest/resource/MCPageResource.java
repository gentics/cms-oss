package com.gentics.contentnode.rest.resource;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.gentics.contentnode.rest.model.request.DisinheritRequest;
import com.gentics.contentnode.rest.model.request.LocalizeRequest;
import com.gentics.contentnode.rest.model.request.MultiPushToMasterRequest;
import com.gentics.contentnode.rest.model.request.MultiUnlocalizeRequest;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.response.DisinheritResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LocalizationInfo;
import com.gentics.contentnode.rest.model.response.LocalizationsResponse;
import com.gentics.contentnode.rest.model.response.MultiDisinheritResponse;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse;

/**
 * Resource for handling multichannelling aspects of pages.
 * @HTTP 401 No valid sid and session secret cookie were provided.
 * @HTTP 403 User has insufficient permissions.
 */
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Path("/page")
public interface MCPageResource {
	@GET
	@Path("/usage/syncableObjects")
	ReferencedSyncableObjectsListResponse getSyncableObjects(
			@QueryParam("ids") List<Integer> pageIds,
			@QueryParam("srcNodeId") Integer srcNodeId,
			@QueryParam("dstNodeId") Integer dstNodeId) throws Exception;

	/**
	 * Push a page from a channel into a master
	 * @param id id of the page
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/push2master/{id}")
	GenericResponse pushToMaster(@PathParam("id") Integer id, PushToMasterRequest request) throws Exception;

	/**
	 * Push a list of pages from a channel into a master
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/push2master")
	GenericResponse pushToMaster(MultiPushToMasterRequest request) throws Exception;

	/**
	 * Unlocalize the given page
	 * @param id id of the page to unlocalize
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/unlocalize/{id}")
	GenericResponse unlocalize(@PathParam("id") Integer id, UnlocalizeRequest request) throws Exception;

	/**
	 * Unlocalize pages in a channel
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/unlocalize")
	GenericResponse unlocalize(MultiUnlocalizeRequest request) throws Exception;

	/**
	 * Localize the given page
	 * @param id id of the page to localize
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/localize/{id}")
	GenericResponse localize(@PathParam("id") Integer id, LocalizeRequest request) throws Exception;

	/**
	 * Get localization info for a list of pages for a given channel and all master channels
	 * @param ids list of page ids
	 * @param nodeId id of the node to start with
	 * @return localization info
	 */
	@GET
	@Path("/localizationinfo")
	LocalizationInfo getLocalizationInfo(@QueryParam("id") List<Integer> ids, @QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Get localization info for a page for a given channel and all master channels
	 * @param id id of the page
	 * @param nodeId id of the node to start with
	 * @return localization info
	 */
	@GET
	@Path("/localizationinfo/{id}")
	LocalizationInfo getLocalizationInfo(@PathParam("id") Integer id,
			@QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Get the IDs an their respective node IDs of localized copies of the
	 * specified page.
	 *
	 * @param id The ID of the master page.
	 * @param addStatus When <code>true</code> information about the pages statuses
	 *		will be added to the response.
	 *
	 * @return A map of page IDs to node IDs of localized copies, with optional
	 *		status information.
	 */
	@GET
	@Path("/localizations/{id}")
	LocalizationsResponse localizations(
			@PathParam("id") Integer id,
			@QueryParam("status") @DefaultValue("false") boolean addStatus) throws Exception;

	/**
	 * Get the multichannelling exclusion and disinherit status of the page
	 * @param id page ID
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit/{id}")
	DisinheritResponse disinherit(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Set the multichannelling exclusion and disinherit status
	 * @param id page ID
	 * @param nodeId node ID
	 * @param waitMs wait timeout in milliseconds
	 * @param request request object
	 * @return generic response
	 */
	@POST
	@Path("/disinherit/{id}")
	GenericResponse disinherit(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, @QueryParam("wait") @DefaultValue("0") long waitMs,
			DisinheritRequest request) throws Exception;

	/**
	 * Get the multichannelling exclusion and disinherit status for a list of pages
	 * @param ids list of page IDs
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit")
	MultiDisinheritResponse disinherit(@QueryParam("id") List<String> ids, @QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Set the multichannelling exclusion and disinherit status for a list of pages
	 * @param ids list of page IDs
	 * @param nodeId node ID
	 * @param waitMs wait timeout in milliseconds
	 * @param request request object
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/disinherit")
	GenericResponse disinherit(@QueryParam("id") List<String> ids, @QueryParam("nodeId") Integer nodeId, @QueryParam("wait") @DefaultValue("0") long waitMs,
			DisinheritRequest request) throws Exception;
}
