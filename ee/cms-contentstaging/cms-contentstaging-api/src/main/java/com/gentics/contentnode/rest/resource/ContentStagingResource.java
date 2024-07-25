package com.gentics.contentnode.rest.resource;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;

import com.gentics.contentnode.rest.model.request.IdTypeMapRequest;
import com.gentics.contentnode.rest.model.response.ContentPackageListResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.StagingStatusResponse;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

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
	 * @return list of packages
	 * @throws Exception
	 */
	@GET
	@Path("/package")
	ContentPackageListResponse list(
				@BeanParam FilterParameterBean filterParams,
				@BeanParam SortParameterBean sortingParams,
				@BeanParam PagingParameterBean pagingParams
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
	@Path("/package/{name}/{type}/{uuid}")
	ContentPackageLoadResponse add(@PathParam("name") String name, @PathParam("type") String objectType, @PathParam("uuid") String objectUuid, @QueryParam("recursive") @DefaultValue("false") boolean recursive) throws Exception;

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
	@Path("/package/{name}/{type}/{uuid}")
	ContentPackageLoadResponse remove(@PathParam("name") String name, @PathParam("type") String objectType, @PathParam("uuid") String objectUuid) throws Exception;

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
	ContentPackageLoadResponse upload(@PathParam("name") String name, FormDataMultiPart multiPart) throws Exception;

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
}
