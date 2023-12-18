package com.gentics.contentnode.rest.resource;

import java.util.List;

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
import javax.ws.rs.core.MediaType;

import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.ConstructCategory;
import com.gentics.contentnode.rest.model.request.BulkLinkUpdateRequest;
import com.gentics.contentnode.rest.model.request.ConstructSortAttribute;
import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.response.ConstructCategoryListResponse;
import com.gentics.contentnode.rest.model.response.ConstructCategoryLoadResponse;
import com.gentics.contentnode.rest.model.response.ConstructListResponse;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.PagedConstructListResponse;
import com.gentics.contentnode.rest.resource.parameter.ConstructParameterBean;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for handling constructs
 * @HTTP 401 No valid sid and session secret cookie were provided.
 * @HTTP 403 User has insufficient permissions.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/construct")
public interface ConstructResource {
	/**
	 * Get the list of constructs
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param constructFilter construct filter parameters
	 * @param perms permissions parameters
	 * @param embed optionally embed the referenced objects (category)
	 * @return response containing a list of constructs
	 * @throws Exception
	 * @HTTP 200 The list of constructs is returned.
	 */
	@GET
	PagedConstructListResponse list(
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam ConstructParameterBean constructFilter,
			@BeanParam PermsParameterBean perms,
			@BeanParam EmbedParameterBean embed
	) throws Exception;

	/**
	 * Load the insertable construct and it's categories for a given constructId
	 * @param id construct ID
	 * @param embed optionally embed the referenced objects (category)
	 * @return response containing the construct
	 * @throws Exception
	 * @HTTP 200 The list of constructs is returned.
	 * @HTTP 404 The construct with ID {constructId} does not exist
	 */
	@GET
	@Path("/load/{constructId}")
	ConstructLoadResponse load(
			@PathParam("constructId") final Integer id,
			@BeanParam EmbedParameterBean embed) throws Exception;

	/**
	 * Get the list of constructs that are used by the tags in the given page.
	 * This includes any constructs that exist in other nodes but are nevertheless used by tags in this page.
	 *
	 * @param skipCount number of items to be skipped (paging)
	 * @param maxItems maximum number of items to be returned (paging). -1 for getting all items.
	 * @param search search string for filtering items
	 * @param changeable true to only get changeable constructs, false for only getting not changeable items. Leave empty to get all
	 * @param pageId ID of the page form which to get constructs.
	 * @param nodeId ID of the node for getting constructs linked to a node
	 * @param categoryId ID of the category for filtering
	 * @param partTypeId IDs of part types for filtering
	 * @param sortBy attribute for sorting
	 * @param sortOrder sort order
	 * @return list of constructs
	 * @deprecated
	 * @see ConstructResource#list()
	 * @throws Exception
	 * @HTTP 200 The list of constructs is returned.
	 */
	@GET
	@Path("/list")
	ConstructListResponse list(
			@QueryParam("skipCount") @DefaultValue("0")   final Integer skipCount,
			@QueryParam("maxItems")  @DefaultValue("-1")  final Integer maxItems,
			@QueryParam("search")                         final String search,
			@QueryParam("changeable")                     final Boolean changeable,
			@QueryParam("pageId")                         final Integer pageId,
			@QueryParam("nodeId")                         final Integer nodeId,
			@QueryParam("category")                       final Integer categoryId,
			@QueryParam("partTypeId")                     final List<Integer> partTypeId,
			@QueryParam("sortby")                         final ConstructSortAttribute sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") final SortOrder sortOrder
			) throws Exception;

	/**
	 * Delete a part of a construct.
	 * Since the passed part ID is unique across all constructs,
	 * the construct doesn't have to be specified.
	 * This will not delete the datasource entries for an overview part, they are deleted
	 * when the construct is deleted.
	 *
	 * @param constructId  The ID of the construct where the part is in
	 * @param idOrKeyname  The ID or the keyword of the part to delete. It will first try to delete by ID, if the value is numeric.
	 * @return             Response object
	 * @throws Exception
	 * @HTTP 200 The part has been deleted
	 */
	@POST
	@Path("/delete/{constructId}/{idOrKeyname}")
	public GenericResponse deletePart(
			@PathParam("constructId") final String constructId,
			@PathParam("idOrKeyname") final String idOrKeyname
	) throws Exception;

	/**
	 * Create new construct
	 * @param construct construct
	 * @param nodeIds IDs of node assignments, mandatory
	 * @return response containing created construct
	 * @throws Exception
	 */
	@POST
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Construct {id} was created.")
	})
	ConstructLoadResponse create(Construct construct, @QueryParam("nodeId") List<Integer> nodeIds) throws Exception;

	/**
	 * Get existing construct
	 * @param id construct ID
	 * @param embed optionally embed the referenced objects (category)
	 * @return response containing the construct
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Construct {id} exists."),
		@ResponseCode(code = 404, condition = "Construct {id} does not exist.")
	})
	ConstructLoadResponse get(
			@PathParam("id") String id,
			@BeanParam EmbedParameterBean embed) throws Exception;

	/**
	 * Update a construct
	 * @param id construct ID
	 * @param construct updated construct data
	 * @param nodeIds IDs of new node assignments
	 * @return response containing the updated construct
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Construct {id} was updated."),
		@ResponseCode(code = 404, condition = "Construct {id} does not exist.")
	})
	ConstructLoadResponse update(@PathParam("id") String id, Construct construct, @QueryParam("nodeId") List<Integer> nodeIds) throws Exception;

	/**
	 * Delete a construct
	 * @param id construct ID
	 * @return empty response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Construct {id} was deleted."),
		@ResponseCode(code = 404, condition = "Construct {id} does not exist."),
		@ResponseCode(code = 409, condition = "Construct {id} could not be deleted due to a conflict.")
	})
	GenericResponse delete(@PathParam("id") String id) throws Exception;

	/**
	 * Create a new construct category
	 * @param category new category REST model
	 * @return response containing the construct category
	 * @throws Exception
	 * @HTTP 200 The category
	 */
	@POST
	@Path("/category")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The construct category has been created."),
	})
	ConstructCategoryLoadResponse createCategory(ConstructCategory category) throws Exception;

	/**
	 * Load the construct category by its ID
	 * @param categoryId construct category ID
	 * @return response containing the construct category
	 * @throws Exception
	 * @HTTP 200 The category is returned.
	 * @HTTP 404 The category with ID {id} does not exist
	 */
	@GET
	@Path("/category/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The construct category has been loaded."),
		@ResponseCode(code = 404, condition = "The construct category {id} does not exist.")
	})
	ConstructCategoryLoadResponse getCategory(@PathParam("id") String categoryId) throws Exception;

	/**
	 * Edit the construct category by its ID
	 * @param constructCategoryId construct category ID
	 * @return response containing the updated category
	 * @throws Exception
	 * @HTTP 200 The updated category is returned.
	 * @HTTP 404 The category with ID {id} does not exist
	 */
	@PUT
	@Path("/category/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The construct category has been loaded."),
		@ResponseCode(code = 404, condition = "The construct category {id} does not exist.")
	})
	ConstructCategoryLoadResponse updateCategory(@PathParam("id") String constructCategoryId, ConstructCategory category) throws Exception;

	/**
	 * Delete the construct category by its ID
	 * @param constructCategoryId construct category ID
	 * @return general success response
	 * @throws Exception
	 * @HTTP 200 The category is deleted
	 * @HTTP 404 The category with ID {id} does not exist
	 */
	@DELETE
	@Path("/category/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The construct category has been loaded."),
		@ResponseCode(code = 404, condition = "The construct category {id} does not exist.")
	})
	GenericResponse deleteCategory(@PathParam("id") String constructCategoryId) throws Exception;

	/**
	 * List all construct categories
	 * @return response with categories list
	 * @throws Exception
	 * @HTTP 200 The list is returned
	 */
	@GET
	@Path("/category")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of construct categories is returned."),
	})
	ConstructCategoryListResponse listCategories(@BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed) throws Exception;

	@POST
	@Path("/category/sortorder")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Sortorder of construct categories has been set.")
	})
	ConstructCategoryListResponse sortCategories(IdSetRequest categoryOrder) throws Exception;

	/**
	 * Link a set of constructs to the selected nodes
	 * @param request POST body with source + target IDs
	 * @return general success response
	 * @throws Exception
	 * @HTTP 200 The link succeeded for all the requested entities
	 * @HTTP 404 The distinct node or construct does not exist
	 */
	@POST
	@Path("/link/nodes")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The link succeeded for all the requested entities."),
		@ResponseCode(code = 404, condition = "The distinct node or construct does not exist.")
	})
	GenericResponse link(BulkLinkUpdateRequest request) throws Exception;

	/**
	 * Unlink a set of constructs from the selected nodes
	 * @param request POST body with source + target IDs
	 * @return general success response
	 * @throws Exception
	 * @HTTP 200 The unlink succeeded for all the requested entities.
	 * @HTTP 404 The distinct node or construct does not exist
	 */
	@POST
	@Path("/unlink/nodes")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The unlink succeeded for all the requested entities."),
		@ResponseCode(code = 404, condition = "The distinct node or construct does not exist.")
	})
	GenericResponse unlink(BulkLinkUpdateRequest request) throws Exception;

	/**
	 * Return a list of nodes, linked to the the given Construct
	 *
	 * @param constructId
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/nodes")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The list of nodes for construct {id}, permitted for the reading for the current user, is returned."),
		@ResponseCode(code = 404, condition = "The construct {id} does not exist.")
	})
	NodeList listConstructNodes(@PathParam("id") String constructId) throws Exception;
}
