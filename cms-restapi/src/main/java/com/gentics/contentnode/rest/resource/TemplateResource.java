package com.gentics.contentnode.rest.resource;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.request.LinkRequest;
import com.gentics.contentnode.rest.model.request.MultiLinkRequest;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.TagSortAttribute;
import com.gentics.contentnode.rest.model.request.TemplateCopyRequest;
import com.gentics.contentnode.rest.model.request.TemplateCreateRequest;
import com.gentics.contentnode.rest.model.request.TemplateSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.PagedFolderListResponse;
import com.gentics.contentnode.rest.model.response.TagList;
import com.gentics.contentnode.rest.model.response.TagListResponse;
import com.gentics.contentnode.rest.model.response.TagStatusResponse;
import com.gentics.contentnode.rest.model.response.TemplateInNodeResponse;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TemplateListParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource used for loading, saving and manipulating GCN templates.
 */
@Path("/template")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface TemplateResource {
	/**
	 * List templates assigned to the given list of nodes
	 * @param nodeIds list of node IDs
	 * @param filterParams filter parameters
	 * @param sortingParams sorting parameters
	 * @param pagingParams paging parameters
	 * @param perms permissions parameters
	 * @return response containing the list of assigned templates
	 * @throws Exception
	 */
	@GET
	TemplateInNodeResponse list(@QueryParam("nodeId") List<String> nodeIds,
			@BeanParam FilterParameterBean filterParams,
			@BeanParam SortParameterBean sortingParams,
			@BeanParam PagingParameterBean pagingParams,
			@BeanParam PermsParameterBean perms,
			@BeanParam TemplateListParameterBean listParams) throws Exception;

	/**
	 * Create a new template
	 * @param request create request
	 * @return created template
	 * @throws Exception
	 */
	@POST
	TemplateLoadResponse create(TemplateCreateRequest request) throws Exception;

	/**
	 * Load the template with the given id and return it.
	 * @param id The template id (either local or global)
	 * @param nodeId channel id
	 * @param update true when the template shall be locked, false if not
	 * @param construct if true, the construct info will be added to tags
	 * @return template load response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	TemplateLoadResponse get(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, @QueryParam("update") @DefaultValue("false") boolean update,
			@QueryParam("construct") @DefaultValue("false") boolean construct) throws Exception;

	/**
	 * Update the template with given id
	 * @param id The template id (either local or global)
	 * @param request template update request
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}")
	GenericResponse update(@PathParam("id") String id, TemplateSaveRequest request) throws Exception;

	/**
	 * Unlock a template
	 * @param id template id (either local or global)
	 * @return template load response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/unlock")
	TemplateLoadResponse unlock(@PathParam("id") String id) throws Exception;

	/**
	 * Delete the template with given id
	 * @param id The template id (either local or global)
	 * @return generic response
	 */
	@DELETE
	@Path("/{id}")
	Response delete(@PathParam("id") String id) throws Exception;

	/**
	 * Get the folders to which the template is assigned
	 * @param id template ID
	 * @param sort sort parameters
	 * @param filter filter parameters
	 * @param paging paging parameters
	 * @return response containing a list of folders
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/folders")
	PagedFolderListResponse folders(@PathParam("id") String id, @BeanParam SortParameterBean sort, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Get the nodes to which the template is assigned
	 * @param id template ID
	 * @param sort sort parameters
	 * @param filter filter parameters
	 * @param paging paging parameters
	 * @return response containing a list of nodes
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/nodes")
	NodeList nodes(@PathParam("id") String id, @BeanParam SortParameterBean sort, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Load the template with the given id and return it.
	 * @param id The template id
	 * @param nodeId channel id
	 * @return
	 * @deprecated Use <code>GET /template/{id}</code> instead
	 * @throws Exception
	 */
	@GET
	@Path("/load/{id}")
	@Deprecated
	TemplateLoadResponse load(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Get the list of templatetags for this template
	 * @param id id of the template. The local or global id
	 * @return response object
	 * @deprecated because new endpoint exists. use {@link #listTags(String, FilterParameterBean, SortParameterBean, PagingParameterBean)} instead
	 * @throws Exception
	 */
	@GET
	@Path("/getTags/{id}")
	@Deprecated
	TagListResponse getTags(@PathParam("id") String id,
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			final @QueryParam("sortby") @DefaultValue("name") TagSortAttribute sortBy,
			final @QueryParam("sortorder") @DefaultValue("asc") SortOrder sortOrder,
			@QueryParam("search") String search) throws Exception;


	/**
	 * Get the list of template tags for this template
	 * @param id id of the template. The local or global id
	 * @param filter filter parameters. Possible filter fields are (id, globalId, constructId, name, enabled)
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param embed optionally embed the referenced object (construct)
	 * @return List of template tags
	 * @throws Exception in case of errors
	 */
	@GET
	@Path("/{id}/tag")
	TagList listTags(
			@PathParam("id") String id,
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam EmbedParameterBean embed) throws Exception;

	/**
	 * Link the given template to the given folders.
	 * For linking templates to folders, the user must have the following permissions:
	 * <ul>
	 *   <li>edit the template (which includes viewing the template)</li>
	 *   <li>link templates to all target folders (which includes viewing folder and viewing templates in the folder)</li>
	 * </ul>
	 * If at least on of these permissions is not granted to the user, the method will fail.
	 * @param id id of the template
	 * @param request request containing the list of folders and other data
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/link/{id}")
	GenericResponse link(@PathParam("id") String id, LinkRequest request) throws Exception;

	/**
	 * Link the given templates to the given folders.
	 * For linking templates to folders, the user must have the following permissions:
	 * <ul>
	 *   <li>edit the template (which includes viewing the template)</li>
	 *   <li>link templates to all target folders (which includes viewing folder and viewing templates in the folder)</li>
	 * </ul>
	 * If at least on of these permissions is not granted to the user, the method will fail.
	 * @param request request containing the list of folders and other data
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/link")
	GenericResponse link(MultiLinkRequest request) throws Exception;

	/**
	 * Unlink the given template from the given folders.
	 * For unlinking templates from folders, the user must have the following permissions:
	 * <ul>
	 *   <li>edit the template (which includes viewing the template)</li>
	 *   <li>link templates to all target folders (which includes viewing folder and viewing templates in the folder)</li>
	 * </ul>
	 * If at least on of these permissions is not granted to the user, the method will fail.
	 * @param id id of the template
	 * @param request request containing the list of folders and other data
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/unlink/{id}")
	GenericResponse unlink(@PathParam("id") String id, LinkRequest request) throws Exception;

	/**
	 * Unlink the given templates from the given folders.
	 * For unlinking templates from folders, the user must have the following permissions:
	 * <ul>
	 *   <li>edit the template (which includes viewing the template)</li>
	 *   <li>link templates to all target folders (which includes viewing folder and viewing templates in the folder)</li>
	 * </ul>
	 * If at least on of these permissions is not granted to the user, the method will fail.
	 * @param request request containing the list of folders and other data
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/unlink")
	GenericResponse unlink(MultiLinkRequest request) throws Exception;

	/**
	 * Get the tag status for a template. The tag status will contain a list of all template tags, which are editable in pages together with the count of
	 * pages in sync, out of sync, incompatible or missing
	 * @param id template ID
	 * @param sort sort parameters
	 * @param filter filter parameters
	 * @param paging paging parameters
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/tagstatus")
	TagStatusResponse tagStatus(@PathParam("id") String id, @BeanParam SortParameterBean sort, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Create a copy of the template
	 * @param id template id
	 * @param request copy request
	 * @return response containing the generated copy
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/copy")
	TemplateLoadResponse copy(@PathParam("id") String id, TemplateCopyRequest request) throws Exception;
}
