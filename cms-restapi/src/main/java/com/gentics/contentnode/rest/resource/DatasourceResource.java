package com.gentics.contentnode.rest.resource;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.Datasource;
import com.gentics.contentnode.rest.model.DatasourceEntryModel;
import com.gentics.contentnode.rest.model.response.ConstructList;
import com.gentics.contentnode.rest.model.response.DatasourceEntryListResponse;
import com.gentics.contentnode.rest.model.response.DatasourceEntryResponse;
import com.gentics.contentnode.rest.model.response.DatasourceLoadResponse;
import com.gentics.contentnode.rest.model.response.PagedDatasourceListResponse;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for management of datasources
 */
@Path("/datasource")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface DatasourceResource {
	/**
	 * List datasources.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>name</code></li>
	 * <li><code>type</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>name</code></li>
	 * <li><code>type</code></li>
	 * </ul>
	 * @param sorting sort parameters
	 * @param filter filter parameter
	 * @param paging paging parameters
	 * @return response containing a list of datasources
	 * @throws Exception
	 */
	@GET
	@StatusCodes({ @ResponseCode(code = 200, condition = "List of datasources is returned.") })
	PagedDatasourceListResponse list(@BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Create new datasource
	 * @param datasource datasource
	 * @return response containing created datasource
	 * @throws Exception
	 */
	@POST
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Datasource was created."),
		@ResponseCode(code = 400, condition = "No request body was sent."),
		@ResponseCode(code = 400, condition = "Not all required fields had non-null values."),
		@ResponseCode(code = 409, condition = "The given name is already in use by another datasource.")
	})
	DatasourceLoadResponse create(Datasource datasource) throws Exception;

	/**
	 * Get existing datasource
	 * @param id datasource ID
	 * @return response containing the datasource
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Datasource {id} exists."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist.")
	})
	DatasourceLoadResponse get(@PathParam("id") String id) throws Exception;

	/**
	 * Update a datasource
	 * @param id datasource ID
	 * @param datasource updated datasource data
	 * @return response containing the updated datasource
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Datasource {id} was updated."),
		@ResponseCode(code = 400, condition = "No request body was sent."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist."),
		@ResponseCode(code = 409, condition = "The given name is already in use by another datasource.")
	})
	DatasourceLoadResponse update(@PathParam("id") String id, Datasource datasource) throws Exception;

	/**
	 * Delete a datasource
	 * @param id datasource ID
	 * @return empty response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Datasource {id} was deleted."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist."),
		@ResponseCode(code = 409, condition = "Datasource {id} cannot be deleted, because it is used.")
	})
	Response delete(@PathParam("id") String id) throws Exception;

	/**
	 * Get the constructs using the datasource.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>keyword</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>category</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>keyword</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>category</code></li>
	 * </ul>
	 * @param id datasource id
	 * @param sorting sort parameters
	 * @param filter filter parameter
	 * @param paging paging parameters
	 * @param embed optionally embed the referenced objects (category)
	 * @return response containing a list of constructs using the datasource
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/constructs")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Datasource {id} exists."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist.")
	})
	ConstructList constructs(@PathParam("id") String id, @BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed) throws Exception;

	/**
	 * List entries of a datasource
	 * @param id datasource id
	 * @return response containing a list of entries
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/entries")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of entries is returned."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist.")
	})
	DatasourceEntryListResponse listEntries(@PathParam("id") String id) throws Exception;

	/**
	 * Create a datasource entry in the datasource
	 * @param id datasource id
	 * @param item datasource entry
	 * @return response containing the created entry
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/entries")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Entry was created."),
		@ResponseCode(code = 400, condition = "No request body was sent."),
		@ResponseCode(code = 400, condition = "Not all required fields had non-null values."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist."),
		@ResponseCode(code = 409, condition = "One of the given values is already used by another entry.")
	})
	DatasourceEntryResponse addEntry(@PathParam("id") String id, DatasourceEntryModel item) throws Exception;

	/**
	 * Get existing datasource entry
	 * @param id datasource id
	 * @param entryId datasource entry id
	 * @return response containing the entry
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/entries/{entryId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Entry {entryId} is returned."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist or entry {entryId} does not exist.")
	})
	DatasourceEntryResponse getEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception;

	/**
	 * Update a datasource entry
	 * @param id datasource id
	 * @param entryId datasource entry id
	 * @param item updated entry
	 * @return response containing the updated entry
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/entries/{entryId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Entry {entryId} was updated."),
		@ResponseCode(code = 400, condition = "No request body was sent."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist or entry {entryId} does not exist."),
		@ResponseCode(code = 409, condition = "One of the given values is already used by another entry.")
	})
	DatasourceEntryResponse updateEntry(@PathParam("id") String id, @PathParam("entryId") String entryId, DatasourceEntryModel item) throws Exception;

	/**
	 * Delete a datasource entry
	 * @param id datasource id
	 * @param entryId datasource entry id
	 * @return empty response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/entries/{entryId}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Entry {entryId} was deleted."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist or entry {entryId} does not exist.")
	})
	Response deleteEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception;

	/**
	 * Update the entries in the given order
	 * @param id datasource id
	 * @param items list of entries
	 * @return response containing the list of updated entries
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/entries")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Entry {entryId} was updated."),
		@ResponseCode(code = 400, condition = "No request body was sent."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist or entry {entryId} does not exist."),
		@ResponseCode(code = 409, condition = "Some entry data violate the uniqueness constraint.")
	})
	DatasourceEntryListResponse updateEntryList(@PathParam("id") String id, List<DatasourceEntryModel> items) throws Exception;
}
