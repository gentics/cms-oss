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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.ContentRepositoryFragmentListResponse;
import com.gentics.contentnode.rest.model.ContentRepositoryListResponse;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.TagmapEntryConsistencyResponse;
import com.gentics.contentnode.rest.model.TagmapEntryListResponse;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.contentnode.rest.model.request.MeshRolesRequest;
import com.gentics.contentnode.rest.model.response.ContentRepositoryFragmentResponse;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.model.response.MeshRolesResponse;
import com.gentics.contentnode.rest.model.response.TagmapEntryResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for handling ContentRepositories
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/contentrepositories")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface ContentRepositoryResource {
	/**
	 * List available ContentRepositories
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return List of ContentRepositories
	 * @throws Exception
	 */
	@GET
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository list is returned.")
	})
	ContentRepositoryListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Create a new ContentRepository
	 * @param item ContentRepository to create
	 * @return created ContentRepository
	 * @throws Exception
	 */
	@POST
	@StatusCodes({
		@ResponseCode(code = 201, condition = "ContentRepository was created.")
	})
	ContentRepositoryResponse add(ContentRepositoryModel item) throws Exception;

	/**
	 * Get the ContentRepository with given id
	 * @param id internal or external ContentRepository ID
	 * @return Response containing the ContentRepository
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository {id} exists."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	ContentRepositoryResponse get(@PathParam("id") String id) throws Exception;

	/**
	 * Update ContentRepository with given id
	 * @param id internal or external ContentRepository ID
	 * @param item updated ContentRepository
	 * @return updated ContentRepository
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "ContentRepository {id} was updated."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	ContentRepositoryResponse update(@PathParam("id") String id, ContentRepositoryModel item) throws Exception;

	/**
	 * Delete the ContentRepository with given id
	 * @param id internal or external ContentRepository ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "ContentRepository {id} was deleted."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	Response delete(@PathParam("id") String id) throws Exception;

	/**
	 * Check the connectivity and structure of the given contentrepository
	 * @param id internal or external ContentRepository ID
	 * @param waitMs wait timeout in ms. When set to 0, response will be sent, when the action completes
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/structure/check")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository {id} exists and was checked."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	ContentRepositoryResponse check(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) throws Exception;

	/**
	 * Check and repair the connectivity and structure of the given contentrepository
	 * @param id internal or external ContentRepository ID
	 * @param waitMs wait timeout in ms. When set to 0, response will be sent, when the action completes
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/structure/repair")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository {id} exists and was checked and repair attempted."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	ContentRepositoryResponse repair(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) throws Exception;

	/**
	 * Check the data in the given contentrepository
	 * @param id internal or external ContentRepository ID
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/data/check")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository {id} exists and was checked."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	ContentRepositoryResponse checkData(@PathParam("id") String id) throws Exception;

	/**
	 * Check and repair the data in the given contentrepository
	 * @param id internal or external ContentRepository ID
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/data/repair")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository {id} exists and was checked and repair attempted."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	ContentRepositoryResponse repairData(@PathParam("id") String id) throws Exception;

	/**
	 * Copy a ContentRepotiroy
	 * @param id internal or external ID of the ContentRepository to copy
	 * @return response containing the copy
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/copy")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "ContentRepository {id} was copied."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	ContentRepositoryResponse copy(@PathParam("id") String id) throws Exception;

	/**
	 * Get the entries in the ContentRepository
	 * @param id internal or external ID
	 * @param fragments true to include entries from assigned fragments, false (which is the default) to only list entries of the ContentRepository itself
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @return list of entries
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/entries")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of entries in ContentRepository {id} is returned."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	TagmapEntryListResponse listEntries(@PathParam("id") String id, @QueryParam("fragments") @DefaultValue("false") boolean fragments, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Check consistency of tagmap entries and return inconsistencies
	 * @param id internal or external ID
	 * @return consistency check result
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/entries/check")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Consistency of ContentRepository {id} was checked."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	TagmapEntryConsistencyResponse checkEntryConsistency(@PathParam("id") String id) throws Exception;

	/**
	 * Create a new tagmap entry
	 * @param id internal or external ID of the ContentRepository
	 * @param item entry to create
	 * @return created entry
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/entries")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Entry was created."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	TagmapEntryResponse addEntry(@PathParam("id") String id, TagmapEntryModel item) throws Exception;

	/**
	 * Get a tagmap entry
	 * @param id internal or external ID of the ContentRepository
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
	TagmapEntryResponse getEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception;

	/**
	 * Update tagmap entry
	 * @param id internal or external ContentRepository ID
	 * @param entryId internal or external entry ID
	 * @param item updated entry
	 * @return updated entry
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/entries/{entryId}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Entry {entryId} was updated."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} or Entry {entryId} does not exist.")
	})
	TagmapEntryResponse updateEntry(@PathParam("id") String id, @PathParam("entryId") String entryId, TagmapEntryModel item) throws Exception;

	/**
	 * Delete the tagmap entry
	 * @param id internal or external ContentRepository ID
	 * @param entryId internal or external entry ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/entries/{entryId}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Entry {entryId} was deleted."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} or Entry {entryId} does not exist.")
	})
	Response deleteEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception;

	/**
	 * Get the ContnetRepository Fragments assigned to the ContentRepository
	 * @param id internal or external ID
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return list of Cr Fragments
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/cr_fragments")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of ContentRepository Fragments assigned to ContentRepository {id} is returned."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist.")
	})
	ContentRepositoryFragmentListResponse listCrFragments(@PathParam("id") String id, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Get a ContentRepository Fragment assigned to the ContentRepository
	 * @param id internal or external ID of the ContentRepository
	 * @param crFragmentId internal or external ID of the ContentRepository Fragment
	 * @return response containing the ContentRepository Fragment
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/cr_fragments/{crFragmentId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository Fragment {crFragmentId} is returned."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} or ContentRepository Fragment {crFragmentId} does not exist.")
	})
	ContentRepositoryFragmentResponse getCrFragment(@PathParam("id") String id, @PathParam("crFragmentId") String crFragmentId) throws Exception;

	/**
	 * Assign a ContentRepository Fragment to the ContentRepository
	 * @param id internal or external ContentRepository ID
	 * @param crFragmentId internal or external ContentRepository Fragment ID
	 * @return Response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/cr_fragments/{crFragmentId}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "ContentRepository Fragment {crFragmentId} was assigned."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} or ContentRepository Fragment {crFragmentId} does not exist."),
		@ResponseCode(code = 409, condition = "{crFragmentId} is already assigned to the ContentRepository {id}.")
	})
	Response addCrFragment(@PathParam("id") String id, @PathParam("crFragmentId") String crFragmentId) throws Exception;

	/**
	 * Remove the ContentRepository Fragment from the ContentRepository
	 * @param id internal or external ContentRepository ID
	 * @param crFragmentId internal or external ContentRepository Fragment ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/cr_fragments/{crFragmentId}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "ContentRepository Fragment {crFragmentId} was removed from the ContentRepository."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} or ContentRepository Fragment {crFragmentId} does not exist.")
	})
	Response removeCrFragment(@PathParam("id") String id, @PathParam("crFragmentId") String crFragmentId) throws Exception;

	/**
	 * Get the roles currently set in the datasource used in the roles object property for the Mesh ContentRepository
	 * @param id ID of the Mesh ContentRepository
	 * @return response containing list of role names
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/roles")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository {id} is a Mesh CR with roles property set and currently set roles are returned."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist."),
		@ResponseCode(code = 409, condition = "ContentRepository {id} is either not a Mesh CR or does not have the roles property set.")
	})
	MeshRolesResponse getRoles(@PathParam("id") String id) throws Exception;

	/**
	 * Get the roles available in the Mesh instance for the Mesh ContentRepository
	 * @param id ID of the Mesh ContentRepository
	 * @return response containing list of role names
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/availableroles")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository {id} is a Mesh CR with roles property set and currently set roles are returned."),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist."),
		@ResponseCode(code = 409, condition = "ContentRepository {id} is either not a Mesh CR or connection to Mesh CR failed.")
	})
	MeshRolesResponse getAvailableRoles(@PathParam("id") String id) throws Exception;

	/**
	 * Set the roles to be used in the datasource of the roles object property for the Mesh ContentRepository
	 * @param id ID of the Mesh ContentRepository
	 * @param request request containing the roles to set
	 * @return response containing the list of set role names
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/roles")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository {id} is a Mesh CR with roles property set and currently set roles are returned."),
		@ResponseCode(code = 400, condition = "The roles could not be set, because not all exist in Mesh"),
		@ResponseCode(code = 404, condition = "ContentRepository {id} does not exist."),
		@ResponseCode(code = 409, condition = "ContentRepository {id} is either not a Mesh CR or does not have the roles property set.")
	})
	MeshRolesResponse setRoles(@PathParam("id") String id, MeshRolesRequest request) throws Exception;
}
