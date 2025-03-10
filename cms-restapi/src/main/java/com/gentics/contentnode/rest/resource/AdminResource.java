package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.contentnode.rest.model.request.ContentMaintenanceActionRequest;
import com.gentics.contentnode.rest.model.request.MaintenanceModeRequest;
import com.gentics.contentnode.rest.model.response.DirtQueueEntryList;
import com.gentics.contentnode.rest.model.response.DirtQueueSummaryResponse;
import com.gentics.contentnode.rest.model.response.FeatureResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.MaintenanceResponse;
import com.gentics.contentnode.rest.model.response.PublishQueueResponse;
import com.gentics.contentnode.rest.model.response.VersionResponse;
import com.gentics.contentnode.rest.model.response.admin.PublishInfoResponse;
import com.gentics.contentnode.rest.model.response.admin.ToolsResponse;
import com.gentics.contentnode.rest.model.response.admin.UpdatesInfoResponse;
import com.gentics.contentnode.rest.model.response.log.ActionLogEntryList;
import com.gentics.contentnode.rest.model.response.log.ActionLogTypeList;
import com.gentics.contentnode.rest.model.response.log.ActionModelList;
import com.gentics.contentnode.rest.model.response.log.ErrorLogEntryList;
import com.gentics.contentnode.rest.resource.parameter.ActionLogParameterBean;
import com.gentics.contentnode.rest.resource.parameter.DirtQueueParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for various tasks used by the administrator (like retrieving version numbers)
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface AdminResource {

	/**
	 * Get the current version of the REST API on the server
	 *
	 * <p>
	 *     Apart from the CMS version a map with version information about
	 *     CMP components for each node will be included.
	 * </p>
	 *
	 * @return VersionResponse
	 */
	@GET
	@Path("/version")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Requested data is returned.")
	})
	VersionResponse currentVersion() throws Exception;

	/**
	 * Get info about a feature activation
	 *
	 * @pathExample /admin/features/new_tageditor
	 * @param name name of the feature
	 * @return feature response
	 */
	@GET
	@Path("/features/{name}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Requested data is returned.")
	})
	FeatureResponse featureInfo(@PathParam("name") String name);

	/**
	 * Get the tools, that shall be shown in the UI
	 * @return list of tools
	 * @throws Exception
	 */
	@GET
	@Path("/tools")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Requested data is returned.")
	})
	ToolsResponse tools() throws Exception;

	/**
	 * Get information of the current publish process.
	 * Since collecting the statistics may be resource intensive, this is not done during this request but the statistics are collected in a background job.
	 * The delay between the jobs can be configured with the variable <code>$PUBLISH_QUEUE_STATS["refresh_delay"]</code> and defaults to 60000 milliseconds (one minute).
	 * @return publish info response
	 * @throws Exception
	 */
	@GET
	@Path("/publishInfo")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Requested data is returned.")
	})
	PublishInfoResponse publishInfo() throws Exception;

	/**
	 * Get available updates
	 * @return response containing available updates
	 * @throws Exception
	 */
	@GET
	@Path("/updates")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Requested data is returned.")
	})
	UpdatesInfoResponse updatesAvailable() throws Exception;

	/**
	 * Get the public server key as JWK
	 * @responseExample application/json {"kty": "RSA", "e": "AQAB", "use": "sig", "kid": "a9eb8baa-801e-4802-be20-f04aeda1efa0", "n": "6UiLZTzmDC1tAVh0hdA3yGx1eLAc5Ybg9ClgeJk1cZn-OzTHZCT5GT58UF1y4AR-dMqqLybzOTvV-BdwVcLh92S1p_mpl-1rZOSsdATDuwH72EpLg5dSvTq520Ju3MbBJn_JkZWVRkhWBctRR6W0qWYkK8ecg67knKzOwOng8z3IRISOgPSZ3JS0eaZzSIfRnqIvh5ZBt3AuX-P0QH_S9FQYUqD2lY7OoRDPtCeCVaDi5blZPT_X_u0M1n0qJdw1MjPIIJJzeJmKslxBuenHPqlpuofZ9s53VJ2dqkB--UA5k2XTUUwrhoKhYBhV3pdaCPIiIWTmCN2kweHrPJ14mw"}
	 * @return Public server key as JWK
	 * @throws Exception
	 */
	@GET
	@Path("/publicKey")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Public Key is returned.")
	})
	JsonNode publicKey() throws Exception;

	/**
	 * Get the action log
	 * @param paging paging parameter
	 * @param query query parameter
	 * @return response containing log entries
	 * @throws Exception
	 */
	@GET
	@Path("/actionlog")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Action log is returned.")
	})
	ActionLogEntryList getActionLog(@BeanParam PagingParameterBean paging, @BeanParam ActionLogParameterBean query) throws Exception;

	/**
	 * Get the object types, that are logged.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>name</code></li>
	 * <li><code>label</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>name</code></li>
	 * <li><code>label</code></li>
	 * </ul>
	 * @param filter filter parameter
	 * @param sorting sorting parameter
	 * @param paging paging parameter
	 * @return response containing a list of object types
	 * @throws Exception
	 */
	@GET
	@Path("/actionlog/types")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Action log types are returned.")
	})
	ActionLogTypeList getActionLogTypes(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Get the actions, which are logged.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>name</code></li>
	 * <li><code>label</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>name</code></li>
	 * <li><code>label</code></li>
	 * </ul>
	 * @param filter filter parameter
	 * @param sorting sorting parameter
	 * @param paging paging parameter
	 * @return response containing a list of actions
	 * @throws Exception
	 */
	@GET
	@Path("/actionlog/actions")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Action log actions are returned.")
	})
	ActionModelList getActionLogActions(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Get error log
	 * @param filter filter parameter
	 * @param paging paging parameter
	 * @return response containing list of logged errors
	 * @throws Exception
	 */
	@GET
	@Path("/errorlog")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Error log is returned.")
	})
	ErrorLogEntryList getErrorLog(@BeanParam FilterParameterBean filter, @BeanParam PagingParameterBean paging)
			throws Exception;

	/**
	 * Get publish queue information.
	 * Since collecting the statistics may be resource intensive, this is not done during this request but the statistics are collected in a background job.
	 * The delay between the jobs can be configured with the variable <code>$PUBLISH_QUEUE_STATS["refresh_delay"]</code> and defaults to 60000 milliseconds (one minute).
	 * @return response containing object counts per node and type
	 * @throws Exception
	 * @HTTP 200 The publish queue information is returned.
	 */
	@GET
	@Path("/content/publishqueue")
	PublishQueueResponse getPublishQueue() throws Exception;

	/**
	 * Perform a maintenance action on the publish queue
	 * @param request maintenance action request
	 * @return response
	 * @throws Exception
	 * @HTTP 200 The content maintenance action is queued.
	 * @HTTP 400 No request body was sent.
	 * @HTTP 400 Not all required fields had non-null values.
	 */
	@POST
	@Path("/content/publishqueue")
	GenericResponse performContentMaintenanceAction(ContentMaintenanceActionRequest request) throws Exception;

	/**
	 * Get the sorted list of dirt queue entries
	 * @param paging paging parameters
	 * @param filter filter parameters
	 * @return response containing a list of dirt queue entries
	 * @throws Exception
	 * @HTTP 200 The dirt queue entry list is returned.
	 */
	@GET
	@Path("/content/dirtqueue")
	DirtQueueEntryList getDirtQueue(@BeanParam PagingParameterBean paging, @BeanParam DirtQueueParameterBean filter) throws Exception;

	/**
	 * Get a sorted summary of current dirt queue entries
	 * @return response containing a sorted summary
	 * @throws Exception
	 * @HTTP 200 The dirt queue summary is returned.
	 */
	@GET
	@Path("/content/dirtqueue/summary")
	DirtQueueSummaryResponse getDirtQueueSummary() throws Exception;

	/**
	 * Delete the failed dirt queue entry with given ID
	 * @param entryId entry ID
	 * @return empty response
	 * @throws Exception
	 * @HTTP 204 The dirt queue entry has been deleted.
	 * @HTTP 404 Dirt queue entry {id} was not found.
	 * @HTTP 409 Dirt queue entry {id} could not be deleted, because it is not marked 'failed'.
	 */
	@DELETE
	@Path("/content/dirtqueue/{id}")
	Response deleteDirtQueueEntry(@PathParam("id") int entryId) throws Exception;


	/**
	 * Batch delete dirt queue entries based on filters
	 * @param filter filter parameters
	 * @return empty response
	 * @throws Exception
	 * @HTTP 200 The dirt queue entries have been deleted
	 */
	@DELETE
	@Path("/content/dirtqueue")
	Response deleteDirtQueueEntries(@BeanParam DirtQueueParameterBean filter) throws Exception;

	/**
	 * Repeat the failed dirt queue entry with given ID
	 * @param entryId entry ID
	 * @return empty response
	 * @throws Exception
	 * @HTTP 204 The dirt queue entry has been refreshed, so that it will be repeated.
	 * @HTTP 404 Dirt queue entry {id} was not found.
	 * @HTTP 409 Dirt queue entry {id} could not be repeated, because it is not marked 'failed'.
	 */
	@PUT
	@Path("/content/dirtqueue/{id}/redo")
	Response redoDirtQueueEntry(@PathParam("id") int entryId) throws Exception;

	/**
	 * Reload the configuration
	 * @return generic response
	 * @throws Exception
	 */
	@PUT
	@Path("/config/reload")
	GenericResponse reloadConfiguration() throws Exception;

	/**
	 * Set or unset the maintenancemode, including the maintenance message.
	 * When the maintenance mode is enabled, all other sessions will be invalidated.
	 * @param request request
	 * @return response
	 * @throws Exception
	 * @HTTP 200 Maintenance mode has been changed
	 */
	@POST
	@Path("/maintenance")
	MaintenanceResponse setMaintenanceMode(MaintenanceModeRequest request) throws Exception;
}
