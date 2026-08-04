package com.gentics.contentnode.rest.resource.migration;

import com.gentics.contentnode.rest.model.request.migration.MigrationReinvokeRequest;
import com.gentics.contentnode.rest.model.request.migration.MigrationTagsRequest;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.contentnode.rest.model.response.ConstructListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationGetLogResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationGetLogsResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobItemsResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationPartsResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationStatusResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationTagsResponse;
import com.gentics.contentnode.rest.model.response.migration.PossiblePartMappingsResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

/**
 * Resource used for performing Tag Type Migrations and Template Migrations
 * 
 * @author Taylor
 */
@Path("/migration")
public interface MigrationResource {

	/**
	 * Cancel the current tag type migration job, if one is being executed
	 * 
	 * @return response object containing details about the cancellation
	 */
	@GET
	@Path("/cancelMigration")
	GenericResponse cancelMigration() throws Exception;

	/**
	 * Get the migration status for a given tag type migration job
	 * 
	 * @return response object containing details about the status of the current migration
	 */
	@GET
	@Path("/getMigrationStatus")
	MigrationStatusResponse getMigrationStatus() throws Exception;

	/**
	 * Get the migration job items for the given job
	 * 
	 * @param jobId The jobId for which the items should be loaded
	 * @return
	 */
	@GET
	@Path("/getMigrationJobItems/{jobId}")
	MigrationJobItemsResponse getMigrationJobItems(@PathParam("jobId") int jobId) throws Exception;

	/**
	 * Get a list of all tag type migration logs
	 * 
	 * @return
	 */
	@GET
	@Path("/getMigrationLogs")
	MigrationGetLogsResponse getMigrationLogs() throws Exception;

	/**
	 * Get the log for a given tag type migration job
	 * 
	 * @param jobId the id of the migration job
	 * @return
	 */
	@GET
	@Path("/getMigrationLog/{jobId}")
	MigrationGetLogResponse getMigrationLog(@PathParam("jobId") int jobId) throws Exception;

	/**
	 * Perform tag migration process
	 * 
	 * @param request
	 *            migration request object
	 * @return response object
	 */
	@POST
	@Path("/performMigration")
	MigrationResponse performTagTypeMigration(TagTypeMigrationRequest request) throws Exception;

	@POST
	@Path("/performTemplateMigration") 
	MigrationResponse performTemplateMigration(TemplateMigrationRequest request) throws Exception;

	/**
	 * Reinvoke the tag type migration for the given object
	 * @param request
	 * @return
	 */
	@POST
	@Path("/reinvokeMigration")
	MigrationResponse reinvokeTagTypeMigration(MigrationReinvokeRequest request) throws Exception;

	/**
	 * Get a response that contains maps and lists with possible part mappings 
	 * between to both tagtypes
	 *  
	 * @param fromTagTypeId TagTypeId of the tagtype that should be mapped
	 * @param toTagTypeId TagTypeId of the desired target tagtype
	 * @return response object
	 */
	@GET
	@Path("/getPossiblePartMappings")
	PossiblePartMappingsResponse getPossiblePartMappings(@QueryParam("fromTagTypeId") int fromTagTypeId, @QueryParam("toTagTypeId") int toTagTypeId) throws Exception;

	/**
	 * Get a list of parts for a single tag type
	 * 
	 * @param id
	 *            id of the tag type to return tags for
	 * @return response object containing the parts for the requested tag type
	 */
	@GET
	@Path("/getPartsForTagType/{id}")
	MigrationPartsResponse getPartsForTagType(@PathParam("id") String id) throws Exception;

	/**
	 * Get a list of Tag Types for possible migration by examining the given objects
	 * 
	 * @param request
	 *            migration tags request object
	 * @return response object containing the tags for the requested objects
	 * @deprecated use {@link #getMigrationConstructs(MigrationTagsRequest)} instead
	 */
	@POST
	@Path("/getMigrationTagTypes")
	MigrationTagsResponse getMigrationTagTypes(MigrationTagsRequest request) throws Exception;

	/**
	 * Get a list of constructs, that can be migrated for the given type
	 * @param request request containing the migration type
	 * @return list of constructs
	 */
	@POST
	@Path("/getMigrationConstructs")
	ConstructListResponse getMigrationConstructs(MigrationTagsRequest request) throws Exception;
}
