package com.gentics.contentnode.rest.resource;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.ObjectProperty;
import com.gentics.contentnode.rest.model.ObjectPropertyCategory;
import com.gentics.contentnode.rest.model.request.BulkLinkUpdateRequest;
import com.gentics.contentnode.rest.model.response.ConstructListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.ObjectPropertyCategoryListResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyCategoryLoadResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyListResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyLoadResponse;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.ObjectPropertyParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for management of object property definitions
 */
@Path("/objectproperty")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface ObjectPropertyResource {
	/**
	 * List object properties.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>keyword</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>keyword</code></li>
	 * <li><code>type</code></li>
	 * <li><code>required</code></li>
	 * <li><code>inheritable</code></li>
	 * <li><code>syncContentset</code></li>
	 * <li><code>syncChannelset</code></li>
	 * <li><code>syncVariants</code></li>
	 * <li><code>restricted</code></li>
	 * <li><code>construct.name</code></li>
	 * <li><code>category.name</code></li>
	 * </ul>
	 * @param sorting sort parameters
	 * @param filter filter parameter
	 * @param paging paging parameters
	 * @param typeFilter type filter parameters
	 * @param embed optionally embed the referenced objects (category, construct)
	 * @return response containing a list of object properties
	 * @throws Exception
	 */
	@GET
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of object properties is returned.")
	})
	ObjectPropertyListResponse list(@BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging, @BeanParam ObjectPropertyParameterBean typeFilter, @BeanParam EmbedParameterBean embed) throws Exception;

	/**
	 * Create a new construct category.
	 *
	 * <p>
	 *     If any of the following requirements are not met, the operation will
	 *     fail with a <em>400 Bad Request</em> status code:
	 *     <ul>
	 *         <li>The {@code keyword} field is missing or empty</li>
	 *         <li>The {@code keyword} field contains characters other than letters (A-Z and a-z), digits (0-9), minus ('-') and underscore ('_')</li>
	 *         <li>The {@code type} field is missing</li>
	 *     </ul>
	 * </p>
	 *
	 * @param objectProperty new object property REST model
	 * @return response containing the object property
	 * @throws Exception
	 * @HTTP 200 The created object property
	 * @HTTP 400 The provided object property definition is invalid
	 */
	@POST
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The object property has been created."),
		@ResponseCode(code = 400, condition = "The provided object property data is invalid.")
	})
	ObjectPropertyLoadResponse create(ObjectProperty objectProperty) throws Exception;

	/**
	 * Load the object property by its ID
	 * @param objectPropertyId object property ID
	 * @param embed optionally embed the referenced objects (category, construct)
	 * @return response containing the updated object property
	 * @throws Exception
	 * @HTTP 200 The object property is returned.
	 * @HTTP 404 The object property with ID {id} does not exist
	 */
	@GET
	@Path("{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The object property has been loaded."),
		@ResponseCode(code = 404, condition = "The object property {id} does not exist.")
	})
	ObjectPropertyLoadResponse get(
			@PathParam("id") String objectPropertyId,
			@BeanParam EmbedParameterBean embed
	) throws Exception;

	/**
	 * Edit the object property by its ID
	 * @param objectPropertyId object property ID
	 * @return response containing the updated object property
	 * @throws Exception
	 * @HTTP 200 The updated object property is returned.
	 * @HTTP 404 The object property with ID {id} does not exist
	 */
	@PUT
	@Path("{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The object property has been updated."),
		@ResponseCode(code = 404, condition = "The object property {id} does not exist.")
	})
	ObjectPropertyLoadResponse update(@PathParam("id") String objectPropertyId, ObjectProperty objectProperty) throws Exception;

	/**
	 * Delete the object property by its ID
	 * @param objectPropertyId object property ID
	 * @return general success response
	 * @throws Exception
	 * @HTTP 200 The object property is deleted
	 * @HTTP 404 The object property with ID {id} does not exist
	 */
	@DELETE
	@Path("{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The object property has been deleted."),
		@ResponseCode(code = 404, condition = "The object property {id} does not exist.")
	})
	GenericResponse delete(@PathParam("id") String objectPropertyId, @QueryParam("foregroundTime") @DefaultValue("-1") int foregroundTime) throws Exception;

	/**
	 * Create a new object property category
	 * @param category new category REST model
	 * @return response containing the created category
	 * @throws Exception
	 * @HTTP 200 The category
	 */
	@POST
	@Path("/category")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The object property category has been created."),
	})
	ObjectPropertyCategoryLoadResponse createCategory(ObjectPropertyCategory category) throws Exception;

	/**
	 * Load the object property category by its ID
	 * @param categoryId category ID
	 * @return response containing the category
	 * @throws Exception
	 * @HTTP 200 The category is returned.
	 * @HTTP 404 The category with ID {id} does not exist
	 */
	@GET
	@Path("/category/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The object property category has been loaded."),
		@ResponseCode(code = 404, condition = "The object property category {id} does not exist.")
	})
	ObjectPropertyCategoryLoadResponse getCategory(@PathParam("id") String categoryId) throws Exception;

	/**
	 * Edit the object property category by its ID
	 * @param objectPropertyCategoryId category ID
	 * @return response containing the updated object property category
	 * @throws Exception
	 * @HTTP 200 The updated category is returned.
	 * @HTTP 404 The category with ID {id} does not exist
	 */
	@PUT
	@Path("/category/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The object property category has been loaded."),
		@ResponseCode(code = 404, condition = "The object property category {id} does not exist.")
	})
	ObjectPropertyCategoryLoadResponse updateCategory(@PathParam("id") String objectPropertyCategoryId, ObjectPropertyCategory category) throws Exception;

	/**
	 * Delete the object property category by its ID
	 * @param objectPropertyCategoryId category ID
	 * @return general success response
	 * @throws Exception
	 * @HTTP 200 The object property category is deleted
	 * @HTTP 404 The object property category with ID {id} does not exist
	 */
	@DELETE
	@Path("/category/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The object property category has been loaded."),
		@ResponseCode(code = 404, condition = "The object property category {id} does not exist.")
	})
	GenericResponse deleteCategory(@PathParam("id") String objectPropertyCategoryId) throws Exception;

	/**
	 * List all object property categories.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>name</code></li>
	 * <li><code>sortOrder</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>name</code></li>
	 * <li><code>sortOrder</code></li>
	 * </ul>
	 * @return response with list
	 * @throws Exception
	 * @HTTP 200 The list is returned
	 */
	@GET
	@Path("/category")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of object property categories is returned."),
	})
	ObjectPropertyCategoryListResponse listCategories(@BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed) throws Exception;

	/**
	 * Link a set of object property definitions to the selected nodes
	 * @param request POST body with source + target IDs
	 * @return general success response
	 * @throws Exception
	 * @HTTP 200 The link succeeded for all the requested entities.
	 * @HTTP 404 The distinct node or object property definition does not exist
	 */
	@POST
	@Path("/link/nodes")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The unlink succeeded for all the requested entities."),
		@ResponseCode(code = 404, condition = "The distinct node or object property definition does not exist.")
	})
	GenericResponse link(BulkLinkUpdateRequest request) throws Exception;

	/**
	 * Unlink a set of object property definitions from the selected nodes
	 * @param request POST body with source + target IDs
	 * @return general success response
	 * @throws Exception
	 * @HTTP 200 The unlink succeeded for all the requested entities.
	 * @HTTP 404 The distinct node or object property definition does not exist
	 */
	@POST
	@Path("/unlink/nodes")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The unlink succeeded for all the requested entities."),
		@ResponseCode(code = 404, condition = "The distinct node or object property definition does not exist.")
	})
	GenericResponse unlink(BulkLinkUpdateRequest request) throws Exception;

	/**
	 * Return a list of constructs, utilizing the given Object Property
	 *
	 * @param objectPropertyId
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/constructs")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The list of constructs for object property {id} is returned."),
		@ResponseCode(code = 404, condition = "The object property {id} does not exist.")
	})
	ConstructListResponse listObjectPropertyConstructs(@PathParam("id") String objectPropertyId) throws Exception;

	/**
	 * Return a list of nodes, linked to the the given Object Property
	 *
	 * @param objectPropertyId
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/nodes")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The list of nodes for object property {id}, permitted for the reading for the current user, is returned."),
		@ResponseCode(code = 404, condition = "The object property {id} does not exist.")
	})
	NodeList listObjectPropertyNodes(@PathParam("id") String objectPropertyId) throws Exception;
}
