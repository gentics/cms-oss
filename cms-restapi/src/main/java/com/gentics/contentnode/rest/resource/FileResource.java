package com.gentics.contentnode.rest.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.MultiPart;

import com.gentics.contentnode.rest.model.request.FileCopyRequest;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.response.FileListResponse;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.FileUsageListResponse;
import com.gentics.contentnode.rest.model.response.FolderUsageListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.MultiFileLoadResponse;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.PrivilegesResponse;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;

/**
 * Resource for loading and manipulating Files in GCN
 *
 * @author norbert
 */
@Path("/file")
public interface FileResource extends AuthenticatedResource {

	/**
	 * GET parameter name for qqfile's filename
	 */
	public static final String QQFILE_FILENAME_PARAMETER_NAME = "qqfile";

	public static final String META_DATA_FOLDERID_KEY = "folderId";
	public static final String META_DATA_DESCRIPTION_KEY = "description";
	public static final String META_DATA_FILE_NAME_KEY = "fileName";
	public static final String META_DATA_NODE_ID_KEY = "nodeId";
	public static final String META_DATA_OVERWRITE_KEY = "overwrite";
	public static final String META_DATA_BODY_PART_KEY_CUSTOM_PARAMETER_NAME = "databodypart";

	/**
	 * Get a list of files in the specified folder
	 * @param inFolder Folder ID and recursive parameters
	 * @param fileListParams Further file parameters
	 * @param filterParams Filter parameters
	 * @param sortingParams Sorting parameters
	 * @param pagingParams Paging parameters
	 * @param editableParams Editable parameters
	 * @param wastebinParams Wastebin parameters
	 * @return A list of files
	 */
	@GET
	FileListResponse list(
		@BeanParam InFolderParameterBean inFolder,
		@BeanParam FileListParameterBean fileListParams,
		@BeanParam FilterParameterBean filterParams,
		@BeanParam SortParameterBean sortingParams,
		@BeanParam PagingParameterBean pagingParams,
		@BeanParam EditableParameterBean editableParams,
		@BeanParam WastebinParameterBean wastebinParams);

	/**
	 * Load the file with given id
	 *
	 * @param id
	 *            file id. This can either be the localid or a globalid
	 * @param nodeId id of the node (channel) for which the image shall be
	 *               loaded (when multichannelling is used).
	 * @return response containing file binary data
	 */
	@GET
	@Path("/content/load/{id}")
	Response loadContent(@PathParam("id") final String id, @QueryParam("nodeId") Integer nodeId);

	/**
	 * Load the file (without data)
	 *
	 * @param id
	 *            id of the file
	 * @param update
	 *            true when the file is fetched for updating. Currently, files
	 *            cannot be locked in the backend, but it is still recommended
	 *            to set this parameter to true when the file shall be modified.
	 * @param construct if true, the construct information will be added to tags
	 * @param nodeId id of the node (channel) for which the image shall be
	 *               loaded (when multichannelling is used).
	 * @param stagingPackageName name of a content staging package, to check the file status in it
	 * @return response containing the file
	 */
	@GET
	@Path("/load/{id}")
	FileLoadResponse load(
			@PathParam("id") String id,
			@DefaultValue("false") @QueryParam("update") boolean update,
			@DefaultValue("false") @QueryParam("construct") boolean construct,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("package") String stagingPackageName
			);

	/**
	 * Load a list of files specified in the given {@link MultiObjectLoadRequest request}.
	 *
	 * File ids for which no files exist, or the user does not have the necessary
	 * permissions, are silently ignored.
	 *
	 * @param request The request with he list of file ids to load.
	 *
	 * @return The list of found files, for which the user has enough permissions.
	 */
	@POST
	@Path("/load")
	MultiFileLoadResponse load(MultiObjectLoadRequest request);

	/**
	 * Create a new file handling simple post data
	 *
	 * @param multiPart
	 *            multipart/form-data request data
	 * @param request
	 *            request with data for the file to be created
	 * @param folderId
	 *             Folder ID where to save the file in
	 * @param customBodyPartName
	 *             Custom name for the file data body part
	 * @param qqFileUploaderFileName
	 *             Meta data filename
	 * @param description
	 *             File description
	 * @param overwrite
	 *             Whether a file with the same name would be overwritten
	 * @return response containing the file meta data
	 */
	@POST
	@Path("/createSimple")
	@Consumes("multipart/form-data")
	@Produces(MediaType.APPLICATION_JSON)
	FileUploadResponse createSimpleMultiPartFallback(MultiPart multiPart, @Context HttpServletRequest request,
			@QueryParam(META_DATA_FOLDERID_KEY) String folderId,
			@QueryParam(META_DATA_BODY_PART_KEY_CUSTOM_PARAMETER_NAME) String customBodyPartName,
			@QueryParam(QQFILE_FILENAME_PARAMETER_NAME) String qqFileUploaderFileName,
			@QueryParam(META_DATA_DESCRIPTION_KEY) String description,
			@QueryParam(META_DATA_OVERWRITE_KEY) @DefaultValue("false") boolean overwrite);

	/**
	 * Create a new file handling simple post data
	 *
	 * @param request
	 *            request with data for the file to be created
	 * @param folderId
	 *             Folder ID where to save the file in
	 * @param nodeId
	 *             Node ID
	 * @param customBodyPartKeyName
	 * @param fileName
	 *             File name
	 * @param description
	 *             File description
	 * @param overwrite
	 *             Whether a file with the same name would be overwritten
	 * @return response containing the file meta data
	 */
	@POST
	@Path("/createSimple")
	@Produces(MediaType.APPLICATION_JSON)
	FileUploadResponse createSimple(@Context HttpServletRequest request, @QueryParam(META_DATA_FOLDERID_KEY) int folderId,
			@QueryParam(META_DATA_NODE_ID_KEY) @DefaultValue("0") int nodeId,
			@QueryParam(META_DATA_BODY_PART_KEY_CUSTOM_PARAMETER_NAME) String customBodyPartKeyName,
			@QueryParam(QQFILE_FILENAME_PARAMETER_NAME) String fileName,
			@QueryParam(META_DATA_DESCRIPTION_KEY) String description,
			@QueryParam(META_DATA_OVERWRITE_KEY) @DefaultValue("false") boolean overwrite);

	/**
	 * Create a new file handling multipart form-data. The data and meta data is encoded within separate body parts.
	 *
	 * @param multiPart
	 *            request with data for the file to be created
	 * @return response containing the file meta data
	 */
	@POST
	@Path("/create")
	@Consumes("multipart/form-data")
	@Produces(MediaType.APPLICATION_JSON)
	FileUploadResponse create(MultiPart multiPart);

	/**
	 * Create a new file with the binary data loaded from a URL
	 * @param request request
	 * @return response containing the file meta data
	 */
	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	FileUploadResponse create(FileCreateRequest request);

	/**
	 * Create a copy of the given file.
	 *
	 * @param request
	 * @return
	 */
	@POST
	@Path("/copy")
	FileUploadResponse copyFile(FileCopyRequest request);

	/**
	 * Mpve the given file to another folder
	 * @param id file id
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/move/{id}")
	GenericResponse move(@PathParam("id") String id, ObjectMoveRequest request);

	/**
	 * Move multiple files to another folder
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/move")
	GenericResponse move(MultiObjectMoveRequest request);

	/**
	 * Save the given file
	 *
	 * @param request
	 *            request with the file to be saved
	 * @return generic response
	 */
	@POST
	@Path("/save/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	GenericResponse save(@PathParam("id") Integer id, FileSaveRequest request);

	/**
	 * Save the posted content into the given file
	 *
	 * @param id
	 *            id of the file
	 * @param multiPart
	 *            request with data for the file to be created
	 * @return generic response
	 */
	@POST
	@Path("/save/{id}")
	@Consumes("multipart/form-data")
	@Produces(MediaType.APPLICATION_JSON)
	GenericResponse save(@PathParam("id") Integer id, MultiPart multiPart);

	/**
	 * Delete the file denoted by id
	 *
	 * @param id
	 *            id of the File to delete
	 * @param nodeId id of the node (channel) of the file
	 * @return response object
	 */
	@POST
	@Path("/delete/{id}")
	GenericResponse delete(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId);

	/**
	 * Remove the file denoted by the given id from the wastebin.
	 *
	 * @param id
	 *            id of the file to remove from the wastebin. This can either be
	 *            a local or global id
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/delete/{id}")
	GenericResponse deleteFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Remove the given files from the wastebin
	 * @param request request containing the file IDs
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/delete")
	GenericResponse deleteFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Restore the given file from the wastebin
	 *
	 * @param id
	 *            id of the file to restore from the wastebin. This can either
	 *            be a local or global id
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/restore/{id}")
	GenericResponse restoreFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Restore the given files from the wastebin
	 * @param request request containing the file IDs
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/restore")
	GenericResponse restoreFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Get the privileges of the current user on the given file
	 *
	 * @param id
	 *            id of the file
	 * @return privileges response
	 */
	// @GET
	// @Path("/privileges/{id}")
	PrivilegesResponse getPrivileges(@PathParam("id") Integer id);

	/**
	 * Get the total usage information for the given files.
	 *
	 * @param nodeId
	 *            id of the node
	 * @param nodeId
	 *            id of the node
	 * @return response
	 */
	@GET
	@Path("/usage/total")
	TotalUsageResponse getTotalUsageInfo(@QueryParam("id") List<Integer> fileId, @QueryParam("nodeId") Integer nodeId);

	/**
	 * Get the folders using one of the given files.
	 *
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param fileId
	 *            list of file ids, for which the usage shall be fetched
	 * @param nodeId
	 *            id of the node
	 * @param returnFolders
	 *            true (default) if the folders shall be returned, false for only returning the counts
	 * @return response
	 */
	@GET
	@Path("/usage/folder")
	FolderUsageListResponse getFolderUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("folders") @DefaultValue("true") boolean returnFolders);

	/**
	 * Get the pages using one of the given files
	 *
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param fileId
	 *            list of file ids, for which the usage shall be fetched
	 * @param nodeId
	 *            id of the node
	 * @param returnPages
	 *            true (default) if the pages shall be returned, false for only returning the counts
	 * @param pageModel page model parameters
	 * @return response
	 */
	@GET
	@Path("/usage/page")
	PageUsageListResponse getPageUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel);

	/**
	 * Get the templates using one of the given files
	 *
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param fileId
	 *            list of file ids, for which the usage shall be fetched
	 * @param nodeId
	 *            id of the node
	 * @param returnTemplates
	 *            true (default) if the templates shall be returned, false for only returning the counts
	 * @return response
	 */
	@GET
	@Path("/usage/template")
	TemplateUsageListResponse getTemplateUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("templates") @DefaultValue("true") boolean returnTemplates);

	/**
	 * Get the images using one of the given files
	 *
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param fileId
	 *            list of file ids, for which the usage shall be fetched
	 * @param nodeId
	 *            id of the node
	 * @param returnImages
	 *            true (default) if the files shall be returned, false for only returning the counts
	 * @return response
	 */
	@GET
	@Path("/usage/image")
	FileUsageListResponse getImageUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("files") @DefaultValue("true") boolean returnImages);

	/**
	 * Get the files using one of the given files
	 *
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param fileId
	 *            list of file ids, for which the usage shall be fetched
	 * @param nodeId
	 *            id of the node
	 * @param returnFiles
	 *            true (default) if the files shall be returned, false for only returning the counts
	 * @return response
	 */
	@GET
	@Path("/usage/file")
	FileUsageListResponse getFileUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("files") @DefaultValue("true") boolean returnFiles);
}
