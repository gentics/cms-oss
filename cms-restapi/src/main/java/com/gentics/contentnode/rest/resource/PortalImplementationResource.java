package com.gentics.contentnode.rest.resource;

import java.io.InputStream;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.PortalImplementationFileListResponse;
import com.gentics.contentnode.rest.model.PortalImplementationFileModel;
import com.gentics.contentnode.rest.model.PortalImplementationFolderModel;
import com.gentics.contentnode.rest.model.PortalImplementationListResponse;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.PortalImplementationFileResponse;
import com.gentics.contentnode.rest.model.response.PortalImplementationFolderResponse;
import com.gentics.contentnode.rest.model.response.PortalImplementationResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for handling Portal Implementations and their contents.<br>
 * The contents of a Portal Implementation are files, which are identified by their path (including the filename) relative to the root folder
 * of the Portal Implementation. Folders are separated by <code>/</code>. Folders are not stored separately, they exist as long as they contain
 * at least one file.<br>
 * Reading the contents requires the permission to view the Portal Implementation, changing the contents requires the permission to edit it.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/portalimplementations")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface PortalImplementationResource {
	/**
	 * List available Portal Implementations.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>keyword</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>type</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>keyword</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>type</code></li>
	 * <li><code>cdate</code></li>
	 * <li><code>edate</code></li>
	 * </ul>
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return List of Portal Implementations
	 * @throws Exception
	 */
	@GET
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Portal Implementation list is returned.")
	})
	PortalImplementationListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Create a new Portal Implementation.<br>
	 * The attributes <code>keyword</code>, <code>name</code> and <code>type</code> are mandatory. <code>keyword</code> and <code>name</code> must be unique.
	 * The <code>globalId</code> is optional and will be generated, if not provided.
	 * @param item Portal Implementation to create
	 * @return created Portal Implementation
	 * @throws Exception
	 */
	@POST
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Portal Implementation was created."),
		@ResponseCode(code = 400, condition = "A mandatory attribute is missing."),
		@ResponseCode(code = 409, condition = "The keyword, name or globalId is already used by another Portal Implementation.")
	})
	PortalImplementationResponse add(PortalImplementationModel item) throws Exception;

	/**
	 * Get the Portal Implementation with given id
	 * @param id internal or external Portal Implementation ID
	 * @return Response containing the Portal Implementation
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Portal Implementation {id} exists."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} does not exist.")
	})
	PortalImplementationResponse get(@PathParam("id") String id) throws Exception;

	/**
	 * Update Portal Implementation with given id. Only attributes, which are set in the request, will be changed.
	 * @param id internal or external Portal Implementation ID
	 * @param item updated Portal Implementation
	 * @return updated Portal Implementation
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Portal Implementation {id} was updated."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} does not exist."),
		@ResponseCode(code = 409, condition = "The keyword or name is already used by another Portal Implementation.")
	})
	PortalImplementationResponse update(@PathParam("id") String id, PortalImplementationModel item) throws Exception;

	/**
	 * Delete the Portal Implementation with given id
	 * @param id internal or external Portal Implementation ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Portal Implementation {id} was deleted."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} does not exist.")
	})
	Response delete(@PathParam("id") String id) throws Exception;
	/**
	 * Get the contents of a folder of the Portal Implementation: the names of the direct subfolders and the files directly contained in the folder.
	 * @param id internal or external Portal Implementation ID
	 * @param path path of the folder (omit for the root folder)
	 * @return folder
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/folders{path: (/.*)?}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Folder is returned."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} or the folder does not exist.")
	})
	PortalImplementationFolderResponse getFolder(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * Move (rename) a folder of the Portal Implementation. All files contained in the folder (and its subfolders) will be moved.
	 * @param id internal or external Portal Implementation ID
	 * @param path path of the folder
	 * @param folder folder containing the new <code>path</code>
	 * @return moved folder
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/folders/{path: .+}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Folder was moved."),
		@ResponseCode(code = 400, condition = "The new path is invalid."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} or the folder does not exist."),
		@ResponseCode(code = 409, condition = "Moving the folder would conflict with existing files.")
	})
	PortalImplementationFolderResponse moveFolder(@PathParam("id") String id, @PathParam("path") String path, PortalImplementationFolderModel folder)
			throws Exception;

	/**
	 * Delete a folder of the Portal Implementation, including all files contained in the folder (and its subfolders)
	 * @param id internal or external Portal Implementation ID
	 * @param path path of the folder
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/folders/{path: .+}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Folder was deleted."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} or the folder does not exist.")
	})
	Response deleteFolder(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * List all files of the Portal Implementation.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>path</code></li>
	 * <li><code>name</code></li>
	 * <li><code>folder</code></li>
	 * <li><code>mimetype</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>path</code></li>
	 * <li><code>name</code></li>
	 * <li><code>folder</code></li>
	 * <li><code>mimetype</code></li>
	 * <li><code>filesize</code></li>
	 * <li><code>cdate</code></li>
	 * <li><code>edate</code></li>
	 * </ul>
	 * @param id internal or external Portal Implementation ID
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @return list of files
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/files")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "File list is returned."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} does not exist.")
	})
	PortalImplementationFileListResponse listFiles(@PathParam("id") String id, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Get the meta information of a file of the Portal Implementation
	 * @param id internal or external Portal Implementation ID
	 * @param path path of the file
	 * @return file
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/files/{path: .+}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "File is returned."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} or the file does not exist.")
	})
	PortalImplementationFileResponse getFile(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * Change the meta information of a file of the Portal Implementation. Only the attributes <code>path</code> (to move or rename the file)
	 * and <code>mimetype</code> can be changed, other attributes are ignored. Attributes, which are not set, are not changed.
	 * @param id internal or external Portal Implementation ID
	 * @param path path of the file
	 * @param file file containing the changed attributes
	 * @return updated file
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/files/{path: .+}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "File was updated."),
		@ResponseCode(code = 400, condition = "The new path is invalid."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} or the file does not exist."),
		@ResponseCode(code = 409, condition = "The new path conflicts with existing files or folders.")
	})
	PortalImplementationFileResponse updateFile(@PathParam("id") String id, @PathParam("path") String path, PortalImplementationFileModel file)
			throws Exception;

	/**
	 * Delete a file of the Portal Implementation
	 * @param id internal or external Portal Implementation ID
	 * @param path path of the file
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/files/{path: .+}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "File was deleted."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} or the file does not exist.")
	})
	Response deleteFile(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * Get the contents of a file of the Portal Implementation. The response has the mimetype of the file as content type.
	 * @param id internal or external Portal Implementation ID
	 * @param path path of the file
	 * @return response containing the file contents
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/content/{path: .+}")
	@Produces(MediaType.WILDCARD)
	@StatusCodes({
		@ResponseCode(code = 200, condition = "File contents are returned."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} or the file does not exist.")
	})
	Response getContent(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * Create or replace a file of the Portal Implementation with the request body as contents. The content type of the request is used as mimetype of the file.
	 * If no content type is given (or <code>application/octet-stream</code>), the mimetype is determined from the filename and the contents.
	 * Missing folders are created implicitly.
	 * @param id internal or external Portal Implementation ID
	 * @param path path of the file
	 * @param contentType content type of the request
	 * @param content file contents
	 * @return created or updated file
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/content/{path: .+}")
	@Consumes(MediaType.WILDCARD)
	@StatusCodes({
		@ResponseCode(code = 200, condition = "File was created or replaced."),
		@ResponseCode(code = 400, condition = "The path is invalid."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} does not exist."),
		@ResponseCode(code = 409, condition = "The path conflicts with existing files or folders.")
	})
	PortalImplementationFileResponse putContent(@PathParam("id") String id, @PathParam("path") String path,
			@HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType, InputStream content) throws Exception;
}
