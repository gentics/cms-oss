package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryListResponse;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentListResponse;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentModel;
import com.gentics.contentnode.rest.model.response.ContentRepositoryFragmentEntryResponse;
import com.gentics.contentnode.rest.model.response.ContentRepositoryFragmentResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for handling ContentRepository Fragments
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/cr_fragments")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface ContentRepositoryFragmentResource {
	/**
	 * List available ContentRepository Fragments.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>name</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>name</code></li>
	 * </ul>
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return List of ContentRepository Fragments
	 * @throws Exception
	 */
	@GET
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository Fragment list is returned.")
	})
	ContentRepositoryFragmentListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Create a new ContentRepository Fragment
	 * @param item ContentRepository Fragment to create
	 * @return created ContentRepository Fragment
	 * @throws Exception
	 */
	@POST
	@StatusCodes({
		@ResponseCode(code = 201, condition = "ContentRepository Fragment was created.")
	})
	ContentRepositoryFragmentResponse add(ContentRepositoryFragmentModel item) throws Exception;

	/**
	 * Get the ContentRepository Fragment with given id
	 * @param id internal or external ContentRepository ID
	 * @return Response containing the ContentRepository
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository Fragment {id} exists."),
		@ResponseCode(code = 404, condition = "ContentRepository Fragment {id} does not exist.")
	})
	ContentRepositoryFragmentResponse get(@PathParam("id") String id) throws Exception;

	/**
	 * Update ContentRepository Fragment with given id
	 * @param id internal or external ContentRepository Fragment ID
	 * @param item updated ContentRepository Fragment
	 * @return updated ContentRepository Fragment
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "ContentRepository Fragment {id} was updated."),
		@ResponseCode(code = 404, condition = "ContentRepository Fragment {id} does not exist.")
	})
	ContentRepositoryFragmentResponse update(@PathParam("id") String id, ContentRepositoryFragmentModel item) throws Exception;

	/**
	 * Delete the ContentRepository Fragment with given id
	 * @param id internal or external ContentRepository Fragment ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "ContentRepository Fragment {id} was deleted."),
		@ResponseCode(code = 404, condition = "ContentRepository Fragment {id} does not exist.")
	})
	Response delete(@PathParam("id") String id) throws Exception;

	/**
	 * Get the entries in the ContentRepository Fragment.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>tagname</code></li>
	 * <li><code>mapname</code></li>
	 * <li><code>foreignlinkAttribute</code></li>
	 * <li><code>foreignlinkAttributeRule</code></li>
	 * <li><code>category</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>tagname</code></li>
	 * <li><code>mapname</code></li>
	 * <li><code>objType</code></li>
	 * <li><code>attributeType</code></li>
	 * <li><code>targetType</code></li>
	 * <li><code>multivalue</code></li>
	 * <li><code>optimized</code></li>
	 * <li><code>filesystem</code></li>
	 * <li><code>foreignlinkAttribute</code></li>
	 * <li><code>foreignlinkAttributeRule</code></li>
	 * <li><code>category</code></li>
	 * <li><code>segmentfield</code></li>
	 * <li><code>displayfield</code></li>
	 * <li><code>urlfield</code></li>
	 * </ul>
	 * @param id internal or external ID
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @return list of entries
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/entries")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of entries in ContentRepository Fragment {id} is returned."),
		@ResponseCode(code = 404, condition = "ContentRepository Fragment {id} does not exist.")
	})
	ContentRepositoryFragmentEntryListResponse listEntries(@PathParam("id") String id, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Create a new ContentRepository Fragment Entry
	 * @param id internal or external ID of the ContentRepository Fragment
	 * @param item entry to create
	 * @return created entry
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/entries")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Entry was created."),
		@ResponseCode(code = 404, condition = "ContentRepository Fragment {id} does not exist.")
	})
	ContentRepositoryFragmentEntryResponse addEntry(@PathParam("id") String id, ContentRepositoryFragmentEntryModel item) throws Exception;

	/**
	 * Get a Fragment Entry
	 * @param id internal or external ID of the ContentRepository Fragment
	 * @param entryId internal or external ID of the entry
	 * @return response containing the entry
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/entries/{entryId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Entry {entryId} is returned."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} or Entry {entryId} does not exist.")
	})
	ContentRepositoryFragmentEntryResponse getEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception;

	/**
	 * Update Fragment entry
	 * @param id internal or external ContentRepository Fragment ID
	 * @param entryId internal or external entry ID
	 * @param item updated entry
	 * @return updated entry
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/entries/{entryId}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Entry {entryId} was updated."),
		@ResponseCode(code = 404, condition = "ContentRepository Fragment {id} or Entry {entryId} does not exist.")
	})
	ContentRepositoryFragmentEntryResponse updateEntry(@PathParam("id") String id, @PathParam("entryId") String entryId,
			ContentRepositoryFragmentEntryModel item) throws Exception;

	/**
	 * Delete the Fragment entry
	 * @param id internal or external ContentRepository Fragment ID
	 * @param entryId internal or external entry ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/entries/{entryId}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Entry {entryId} was deleted."),
		@ResponseCode(code = 404, condition = "ContentRepository Fragment {id} or Entry {entryId} does not exist.")
	})
	Response deleteEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception;
}
