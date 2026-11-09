package com.gentics.contentnode.rest.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.gentics.contentnode.rest.model.request.DisinheritRequest;
import com.gentics.contentnode.rest.model.request.LocalizeRequest;
import com.gentics.contentnode.rest.model.request.MultiPushToMasterRequest;
import com.gentics.contentnode.rest.model.request.MultiUnlocalizeRequest;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest.Type;
import com.gentics.contentnode.rest.model.response.DisinheritResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LocalizationInfo;
import com.gentics.contentnode.rest.model.response.LocalizationsResponse;
import com.gentics.contentnode.rest.model.response.MultiDisinheritResponse;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse;

/**
 * Resource for handling multichannelling aspects of folders
 * @HTTP 401 No valid sid and session secret cookie were provided.
 * @HTTP 403 User has insufficient permissions.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/folder")
public interface MCFolderResource {
	/**
	 * Get objects which are directly or indirectly linked by the specified folders,
	 * which can be synchronised to a master channel along with the folders themselves.
	 *
	 * Linked objects are pages, files and images specified in <code>UrlPartTypes</code> of
	 * content or object tags.
	 *
	 * Direct dependencies are in the {@link ReferencedSyncableObjectsListResponse#getPages() pages},
	 * {@link ReferencedSyncableObjectsListResponse#getFiles() files} and
	 * {@link ReferencedSyncableObjectsListResponse#getImages() images} files of the response. Indirect
	 * dependencies are mapped in {@link ReferencedSyncableObjectsListResponse#getDependencies dependencies}.
	 *
	 * @param folderIds The IDs of the folders to get linked objects for
	 * @param srcNodeId The channel ID of the source channel
	 * @param dstNodeId The channel ID of the target channel
	 * @param recursive Whether objects in subfolders should be collected (optional, default: <code>false</code>)
	 * @param types The object types which should be synchronised (optional, default: all types)
	 */
	@GET
	@Path("/usage/syncableObjects")
	ReferencedSyncableObjectsListResponse getSyncableObjects(
			@QueryParam("ids") List<Integer> folderIds,
			@QueryParam("srcNodeId") Integer srcNodeId,
			@QueryParam("dstNodeId") Integer dstNodeId,
			@QueryParam("recursive") Boolean recursive,
			@QueryParam("types") List<Type> types) throws Exception;

	/**
	 * Push a folder from a channel into a master
	 * @param id id of the folder
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/push2master/{id}")
	GenericResponse pushToMaster(@PathParam("id") Integer id, PushToMasterRequest request) throws Exception;

	/**
	 * Push a list of folders from a channel into a master
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/push2master")
	GenericResponse pushToMaster(MultiPushToMasterRequest request) throws Exception;

	/**
	 * Unlocalize the given folder
	 * @param id id of the folder to unlocalize
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/unlocalize/{id}")
	GenericResponse unlocalize(@PathParam("id") Integer id, UnlocalizeRequest request) throws Exception;

	/**
	 * Unlocalize folders in a channel
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/unlocalize")
	GenericResponse unlocalize(MultiUnlocalizeRequest request) throws Exception;

	/**
	 * Localize the given folder
	 * @param id id of the folder to localize
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/localize/{id}")
	GenericResponse localize(@PathParam("id") Integer id, LocalizeRequest request) throws Exception;

	/**
	 * Get the IDs an their respective node IDs of localized copies of the
	 * specified folder.
	 *
	 * @param id The ID of the master folder.
	 *
	 * @return A map of folder IDs to node IDs of localized copies.
	 */
	@GET
	@Path("/localizations/{id}")
	LocalizationsResponse localizations(@PathParam("id") Integer id) throws Exception;

	/**
	 * Get localization info for a folder and all subitems for a given channel and all master channels
	 * @param id id of the folder. This can either be the local or global id
	 * @param nodeId id of the node to start with
	 * @param recursive true if the localization info shall be fetched recursively, false if not
	 * @return localization info
	 */
	@GET
	@Path("/localizationinfo/{id}")
	LocalizationInfo getLocalizationInfo(@PathParam("id") String id,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("recursive") boolean recursive) throws Exception;

	/**
	 * Get the multichannelling exclusion and disinherit status of the folder
	 * @param id folder ID
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit/{id}")
	DisinheritResponse disinherit(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Set the multichannelling exclusion and disinherit status
	 * @param id folder ID
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
	 * Get the multichannelling exclusion and disinherit status for a list of folders
	 * @param ids list of folder IDs
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit")
	MultiDisinheritResponse disinherit(@QueryParam("id") List<String> ids, @QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Set the multichannelling exclusion and disinherit status for a list of folders
	 * @param ids list of folder IDs
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
