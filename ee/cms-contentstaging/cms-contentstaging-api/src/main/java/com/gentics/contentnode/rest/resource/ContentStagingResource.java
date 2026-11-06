package com.gentics.contentnode.rest.resource;

import java.util.List;

import org.glassfish.jersey.media.multipart.MultiPart;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.request.IdTypeMapRequest;
import com.gentics.contentnode.rest.model.response.ContentPackageFolderResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageListResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.StagingStatusResponse;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.parameter.ContentPackageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;

/**
 * Content Staging REST API
 */
@Path("/content")
public interface ContentStagingResource {
	/**
	 * Get packages list.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>nodes</code></li>
	 * <li><code>pages</code></li>
	 * <li><code>folders</code></li>
	 * <li><code>files</code></li>
	 * <li><code>images</code></li>
	 * </ul>
	 * @param filterParams filter parameters
	 * @param sortingParams sorting parameters
	 * @param pagingParams paging parameters
	 * @param listParams list parameters
	 * @return list of packages
	 * @throws Exception
	 */
	@GET
	@Path("/package")
	ContentPackageListResponse list(
				@BeanParam FilterParameterBean filterParams,
				@BeanParam SortParameterBean sortingParams,
				@BeanParam PagingParameterBean pagingParams,
				@BeanParam ContentPackageListParameterBean listParams
			) throws Exception;

	/**
	 * Create new package.
	 * 
	 * @param model package model
	 * @return created package
	 * @throws Exception
	 */
	@PUT
	@Path("/package")
	ContentPackageLoadResponse create(StagedContentPackage model) throws Exception;

	/**
	 * Get the package by its name
	 * 
	 * @param name package name
	 * @return response
	 * @throws Exception
	 */
	@GET
    @Path("/package/{name}")
	ContentPackageLoadResponse read(@PathParam("name") String name) throws Exception;

	/**
	 * Update the package with the given name.
	 * 
	 * @param name package name
	 * @param model model for updating
	 * @return updated package
	 * @throws Exception
	 */
	@POST
    @Path("/package/{name}")
	ContentPackageLoadResponse update(@PathParam("name") String name, StagedContentPackage model) throws Exception;

	/**
	 * Delete the package with the given name
	 * 
	 * @param name package name
	 * @return response
	 * @throws Exception
	 */
	@DELETE
    @Path("/package/{name}")
	GenericResponse delete(@PathParam("name") String name) throws Exception;

	/**
	 * Add an entity, keeping its folder path, to the package of a given name.
	 * 
	 * @param name package name
	 * @param objectType Type of the object to add. supported values are: node, file, image, folder, page, form
	 * @param objectUuid Global ID of the object to add
	 * @param recursive Should the contained objects be added recursively. Applicable to the folders only.
	 * @return the package status after the add.
	 * @throws Exception
	 */
	@PUT
	@Path("/package/{name}/{objectType}/{uuid}")
	ContentPackageLoadResponse add(@PathParam("name") String name, @PathParam("objectType") String objectType, @PathParam("uuid") String objectUuid, @QueryParam("recursive") @DefaultValue("false") boolean recursive) throws Exception;

	/**
	 * Remove an entity from the package of a given name.
	 * 
	 * @param name package name
	 * @param objectType Type of the object to remove. supported values are: node, file, image, folder, page, form
	 * @param objectUuid Global ID of the object to remove
	 * @return the package status after the removal.
	 * @throws Exception
	 */
	@DELETE
	@Path("/package/{name}/{objectType}/{uuid}")
	ContentPackageLoadResponse remove(@PathParam("name") String name, @PathParam("objectType") String objectType, @PathParam("uuid") String objectUuid) throws Exception;

	/**
	 * Download the package of the given name as ZIP archive.
	 * 
	 * @param name package name
	 * @return download response
	 * @throws Exception
	 */
	@GET
	@Path("/package/{name}/zip")
	@Produces({ "application/zip" })
	Response download(@PathParam("name") String name) throws Exception;

	/**
	 * Upload the package of the given name in the form of ZIP archive.
	 * 
	 * @param name package name
	 * @param multiPart
	 * @return package response
	 * @throws Exception
	 */
	@POST
	@Path("/package/{name}/zip")
	@Consumes("multipart/form-data")
	ContentPackageLoadResponse upload(@PathParam("name") String name, MultiPart multiPart) throws Exception;


	/**
	 * Retrieve a list of import errors
	 *
	 * @param name package name
	 * @return response containing the error list
	 * @throws Exception
	 */
	@GET
	@Path("/package/{name}/import/errors")
	Response getImportErrors(@PathParam("name") String name) throws Exception;


	/**
	 * Import the entities of a package of given name into CMS.
	 * 
	 * @param name package name
	 * @param waitMs wait timeout in ms. When set to 0, response will be sent, when the action completes
	 * @param isTest do not make an actual import, but test instead if the package can be imported.
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/package/{name}/import")
	GenericResponse importIntoCms(@PathParam("name") String name, @QueryParam("wait") @DefaultValue("0") long waitMs, @QueryParam("test") @DefaultValue("false") boolean isTest) throws Exception;

	/**
	 * Export the CMS entities, contained in the package with the given name, into the package, i.e. make a package sync.
	 * 
	 * @param name package name
	 * @param waitMs wait timeout in ms. When set to 0, response will be sent, when the action completes
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/package/{name}/export")
	GenericResponse exportFromCms(@PathParam("name") String name, @QueryParam("wait") @DefaultValue("0") long waitMs) throws Exception;

	/**
	 * Check the staging package inclusion package for the bulk of various types.
	 * 
	 * @param name package name
	 * @param request request
	 * @return staging status response
	 * @throws NodeException
	 */
	@POST
	@Path("/package/{name}/status")
	StagingStatusResponse status(String name, IdTypeMapRequest request) throws Exception;

	/**
	 * List the contents of a given folder of a package of the given name.
	 * 
	 * @param name package name
	 * @param segments folder path
	 * @param filter filter the results
	 * @return download response
	 * @throws Exception
	 */
	@GET
	@Path("/package/{name}/list/{pathSegments: .*}")
	ContentPackageFolderResponse folderList(@PathParam("name") String name, @PathParam("pathSegments") List<PathSegment> segments, @QueryParam("filter") String filter) throws Exception;
}
