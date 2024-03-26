package com.gentics.contentnode.rest.resource;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderListRequest;
import com.gentics.contentnode.rest.model.request.FolderMoveRequest;
import com.gentics.contentnode.rest.model.request.FolderPublishDirSanitizeRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.MultiFolderLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiFolderMoveRequest;
import com.gentics.contentnode.rest.model.request.StartpageRequest;
import com.gentics.contentnode.rest.model.response.FolderExternalLinksResponse;
import com.gentics.contentnode.rest.model.response.FolderListResponse;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.FolderObjectCountResponse;
import com.gentics.contentnode.rest.model.response.FolderPublishDirSanitizeResponse;
import com.gentics.contentnode.rest.model.response.FoundFilesListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ItemListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFolderListResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.MultiFolderLoadResponse;
import com.gentics.contentnode.rest.model.response.PrivilegesResponse;
import com.gentics.contentnode.rest.model.response.TemplateListResponse;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FolderListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TemplateListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;

/**
 * Resource for loading and manipulating folders in GCN
 */
@Path("/folder")
public interface FolderResource extends AuthenticatedResource {
	@POST
	@Path("/create")
	FolderLoadResponse create(FolderCreateRequest request);

	/**
	 * Load a single folder
	 * @param folderId id of the folder to load. This can be either the localid or a globalid
	 * @param update
	 *            true when the folder is fetched for updating. Currently, folders
	 *            cannot be locked in the backend, but it is still recommended
	 *            to set this parameter to true when the folder shall be modified.
	 * @param addPrivileges flag whether the privileges should be added to the reponse
	 * @param construct if true, the construct information will be added to tags
	 * @param nodeId id of the node (channel) for which the folder shall be loaded (when multichannelling is used)
	 * @param stagingPackageName name of a content staging package, to check the folder status in it
	 * @return response containing the folder to load
	 */
	@GET
	@Path("/load/{id}")
	FolderLoadResponse load(@PathParam("id") String folderId,
			@DefaultValue("false") @QueryParam("update") boolean update,
			@QueryParam("privileges") @DefaultValue("false") boolean addPrivileges,
			@QueryParam("construct") @DefaultValue("false") boolean construct,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("package") String stagingPackageName);

	/**
	 * Load a list of folders specified in the given {@link MultiFolderLoadRequest request}.
	 *
	 * Folder ids for which no folders exist, or the user does not have the necessary
	 * permissions, are silently ignored.
	 *
	 * @param request The request with he list of folder ids to load.
	 *
	 * @return The list of found folders, for which the user has enough permissions.
	 */
	@POST
	@Path("/load")
	MultiFolderLoadResponse load(MultiFolderLoadRequest request);

	/**
	 * Load the breadcrumb to the given folder
	 * @param id id of the folder. This can either be a localid or a globalid
	 * @param nodeId node id (for multichannelling)
	 * @param includeWastebin true to include folders from the wastebin, false (default) if not
	 * @param includeTags true to include the folders tags, false (default)if not
	 * @return list of folders. the first entry is the root folder of the node, the last one the requested folder.
	 */
	@GET
	@Path("/breadcrumb/{id}")
	LegacyFolderListResponse getBreadcrumb(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId,
			@QueryParam("wastebin") @DefaultValue("false") boolean includeWastebin,
			@QueryParam("tags") @DefaultValue("false") boolean includeTags);

	/**
	 * get a list of pages for this folder
	 *
	 * @param id id of the folder. This can be the local or global id of the folder
	 * @param inFolder folder parameters
	 * @param pageListParams page list params
	 * @param filterParams filter parameters
	 * @param sortingParams sorting parameters
	 * @param pagingParams paging parameters
	 * @param publishParams publishable params
	 * @param wastebinParams wastebin params
	 * @return list of pages
	 * @deprecated Use {@link PageResource#list(InFolderParameterBean, PageListParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, PublishableParameterBean, WastebinParameterBean)} instead
	 */
	@GET
	@Path("/getPages/{id}")
	LegacyPageListResponse getPages(
		@PathParam("id") String id,
		@BeanParam InFolderParameterBean inFolder,
		@BeanParam PageListParameterBean pageListParams,
		@BeanParam LegacyFilterParameterBean filterParams,
		@BeanParam LegacySortParameterBean sortingParams,
		@BeanParam LegacyPagingParameterBean pagingParams,
		@BeanParam PublishableParameterBean publishParams,
		@BeanParam WastebinParameterBean wastebinParams);

	/**
	 * get a list of files for this folder
	 * @param folderId id of the folder
	 * @param inFolder folder parameters
	 * @param fileListParams file list parameters
	 * @param filterParams filter parameters
	 * @param sortingParams sorting parameters
	 * @param pagingParams paging parameters
	 * @param editableParams editable parameters
	 * @param wastebinParams wastebin parameters
	 * @return list of files
	 * @deprecated Use {@link FileResource#list(InFolderParameterBean, FileListParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, EditableParameterBean, WastebinParameterBean)} instead
	 */
	@GET
	@Path("/getFiles/{folderId}")
	LegacyFileListResponse getFiles(
		@PathParam("folderId") String folderId,
		@BeanParam InFolderParameterBean inFolder,
		@BeanParam FileListParameterBean fileListParams,
		@BeanParam LegacyFilterParameterBean filterParams,
		@BeanParam LegacySortParameterBean sortingParams,
		@BeanParam LegacyPagingParameterBean pagingParams,
		@BeanParam EditableParameterBean editableParams,
		@BeanParam WastebinParameterBean wastebinParams);

	/**
	 * get a list of images for this folder
	 * @param folderId id of the folder
	 * @param inFolder folder parameters
	 * @param fileListParams file list parameters
	 * @param filterParams filter parameters
	 * @param sortingParams sorting parameters
	 * @param pagingParams paging parameters	 * @param editableParams editable parameters
	 * @param wastebinParams wastebin parameters
	 * @return list of iamges
	 * @deprecated Use {@link ImageResource#list(InFolderParameterBean, FileListParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, EditableParameterBean, WastebinParameterBean)} instead
	 */
	@GET
	@Path("/getImages/{folderId}")
	LegacyFileListResponse getImages(
		@PathParam("folderId") String folderId,
		@BeanParam InFolderParameterBean inFolder,
		@BeanParam FileListParameterBean fileListParams,
		@BeanParam LegacyFilterParameterBean filterParams,
		@BeanParam LegacySortParameterBean sortingParams,
		@BeanParam LegacyPagingParameterBean pagingParams,
		@BeanParam EditableParameterBean editableParams,
		@BeanParam WastebinParameterBean wastebinParams);

	/**
	 * Get the list of folders in this folder
	 * @param folderListRequest
	 * @return list of folders
	 */
	@POST
	@Path("/getFolders/")
	LegacyFolderListResponse getFolders(FolderListRequest folderListRequest);

	/**
	 * Get the list of folders in this folder
	 *
	 * @param id
	 *            local id of the folder. This can either be the local or
	 *            globalid.
	 * @param nodeId
	 *            node id (for multichannelling)
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for
	 *            returning all items
	 * @param recursive
	 *            true when the folders shall be fetched recursively
	 * @param sortBy
	 *            name of the sorted attribute. It is possible to sort by name,
	 *            cdate, edate, masterNode, excluded, deletedat. defaults to "name"
	 * @param sortOrder
	 *            sorting order, defaults to "asc"
	 * @param inherited
	 *            true to only return inherited folders in the given node, false
	 *            to only get local/localized folders, null to get local and
	 *            inherited folders
	 * @param search
	 *            search string to search for in name, null if not searching -
	 *            this will filter the results if either the ID, the name
	 *            (partial match or the description (partial match) matches the
	 *            given search string.
	 * @param tree
	 *            true when folders shall be returned as tree(s). Subfolders
	 *            will be attached to their mothers. This only makes sense, when
	 *            recursive is true.
	 * @param recursiveIds
	 *            optional list of folder ids, for which the children shall be
	 *            fetched. if recursive is true (ignored if recursive is false).
	 *            The ids might be composed as nodeId/folderId to get children
	 *            for folders in specific channels only.
	 * @param privilegeMap
	 *            true if the privileges shall be added to the folders as map, false if not
	 * @param addPrivileges
	 *            true if the privileges shall be added to the folders, false if not
	 * @param editableParams
	 *            filter params for creator/editor and created and edited timestamps
	 * @param wastebinParams
	 *            exclude (default) to exclude deleted objects, include to include
	 *            deleted objects, only to return only deleted objects
	 * @return list of folders
	 * @deprecated Use {@link #list(InFolderParameterBean, FolderListParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, EditableParameterBean, WastebinParameterBean)} instead
	 */
	@GET
	@Path("/getFolders/{id}")
	LegacyFolderListResponse getFolders(
		@PathParam("id") String id,
		@QueryParam("recId") List<String> recursiveIds,
		@QueryParam("privileges") @DefaultValue("false") boolean addPrivileges,
		@BeanParam InFolderParameterBean inFolder,
		@BeanParam FolderListParameterBean folderListParams,
		@BeanParam LegacyFilterParameterBean filterParams,
		@BeanParam LegacySortParameterBean sortParams,
		@BeanParam LegacyPagingParameterBean pagingParams,
		@BeanParam EditableParameterBean editableParams,
		@BeanParam WastebinParameterBean wastebinParams);

	/**
	 * Get the list of folders in this folder.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>publishDir</code></li>
	 * </ul>
	 * @param inFolder Folder ID and recursive parameters
	 * @param folderListParams further folder parameters
	 * @param filter Search parameters
	 * @param sorting Sorting parameters
	 * @param paging Paging parameters
	 * @param editable Editable parameters
	 * @param wastebin Wastebin parameters
	 * @return A list of folders
	 */
	@GET
	FolderListResponse list(
		@BeanParam InFolderParameterBean inFolder,
		@BeanParam FolderListParameterBean folderListParams,
		@BeanParam FilterParameterBean filter,
		@BeanParam SortParameterBean sorting,
		@BeanParam PagingParameterBean paging,
		@BeanParam EditableParameterBean editable,
		@BeanParam WastebinParameterBean wastebin);

	/**
	 * Get the templates which are linked into the given folder
	 * @param folderId id of the folder. This can either be a localid or a globalid
	 * @param inFolder folder parameters
	 * @param templateListParams template-specific list parameters
	 * @param filterParams filter parameters
	 * @param sortingParams sorting parameters
	 * @param pagingParams paging parameters
	 * @param editableParams editable parameters
	 * @param wastebinParams wastebin parameters
	 * @return list of templates
	 */
	@GET
	@Path("/getTemplates/{folderId}")
	TemplateListResponse getTemplates(@PathParam("folderId") String folderId,
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam TemplateListParameterBean templateListParams,
			@BeanParam LegacyFilterParameterBean filterParams,
			@BeanParam LegacySortParameterBean sortingParams,
			@BeanParam LegacyPagingParameterBean pagingParams,
			@BeanParam EditableParameterBean editableParams,
			@BeanParam WastebinParameterBean wastebinParams);

	/**
	 * Get a list of items for this folder
	 * @param folderId id of the folder
	 * @param types list of types to return, e.g. page, file and/or image
	 * @param nodeId node id for this folder - for use with multichanneling
	 * @param languageVariants
	 *            true when the language variants should be added to the pages
	 * @param language
	 *            code of the language in which the pages shall be fetched.
	 * @param langFallback
	 *            true if the language fallback shall be done when getting pages
	 *            in a language, false if not. If a page is not present in the
	 *            given language and langFallback is true, the language variant
	 *            with highest priority in the node is used instead, otherwise
	 *            the page will not be present in the list
	 * @param inFolder (optional) folder parameters
	 * @param filter (optional) filter parameters
	 * @param sorting (optional) sorting parameters
	 * @param paging (optional) paging parameters
	 * @param publishParams (optional) editable/publishable parameters
	 * @return list of items
	 */
	@GET
	@Path("/getItems/{folderId}")
	ItemListResponse getItems(
			@PathParam("folderId") String folderId,
			@QueryParam("type") List<ItemType> types,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("template") @DefaultValue("false") boolean template,
			@QueryParam("langvars") @DefaultValue("false") boolean languageVariants,
			@QueryParam("language") String language,
			@QueryParam("langfallback") @DefaultValue("true") boolean langFallback,
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam LegacyFilterParameterBean filter,
			@BeanParam LegacySortParameterBean sorting,
			@BeanParam LegacyPagingParameterBean paging,
			@BeanParam PublishableParameterBean publishParams);

	/**
	 * Find pages by name, eventually starting with the given folder id
	 * @param folderId start folder id, if set to 0, the search is done over all folders
	 * @param query string to be searched (currently, only page names are searched)
	 * @param skipCount number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems maximum number of items to be returned, set to -1 for returning all items
	 * @param links type of the links to the page ("backend" for backend links, "frontend" for frontend links)
	 * @param recursive true when the search shall be done recursive, false if only flat in the given folder
	 * @return list of found pages
	 * @deprecated use {@link #getPages(String, InFolderParameterBean, PageListParameterBean, LegacyFilterParameterBean, LegacySortParameterBean, LegacyPagingParameterBean, PublishableParameterBean, WastebinParameterBean)} instead
	 */
	@GET
	@Path("/findPages")
	LegacyPageListResponse findPages(@QueryParam("folderId") @DefaultValue("0") Integer folderId,
			@QueryParam("query") String query,
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("100") Integer maxItems,
			@QueryParam("links") @DefaultValue("backend") LinksType links,
			@QueryParam("recursive") @DefaultValue("true") boolean recursive);

	/**
	 * Find files by name, starting with the given folder id
	 * @param folderId start folder id, if set to 0, the search is done over all folders
	 * @param query string to be searched (currently, only page names are searched)
	 * @param skipCount number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems maximum number of items to be returned, set to -1 for returning all items
	 * @param links type of the links to the file ("backend" for backend links, "frontend" for frontend links)
	 * @param recursive true when the search shall be done recursive, false if only flat in the given folder
	 * @return list of found files
	 * @deprecated use {@link FileResource#list(InFolderParameterBean, FileListParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, EditableParameterBean, WastebinParameterBean)} instead
	 */
	@GET
	@Path("/findFiles")
	FoundFilesListResponse findFiles(@QueryParam("folderId") @DefaultValue("0") Integer folderId,
			@QueryParam("query") String query,
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("100") Integer maxItems,
			@QueryParam("links") @DefaultValue("backend") LinksType links,
			@QueryParam("recursive") @DefaultValue("true") boolean recursive);

	/**
	 * Get externals links from all the pages inside the folder with id `folderId`.
	 * @param folderId Id of the folder.
	 * @param recursive If true, pages in subfolders will be included in the response.
	 * @return list of pages including its external links (see {@link FolderExternalLinksResponse}).
	 */
	@GET
	@Path("/getExternalLinks/{folderId}")
	FolderExternalLinksResponse getExternalLinks(
			@PathParam("folderId") Integer folderId,
			@QueryParam("recursive")@DefaultValue("false") boolean recursive);

	/**
	 * Save a single folder
	 * @param id folder id. This can either be a local or globalid
	 * @param request folder save request
	 * @return generic response
	 */
	@POST
	@Path("/save/{id}")
	GenericResponse save(@PathParam("id") String id, FolderSaveRequest request);

	/**
	 * Delete a single folder. Note that inherited or localized folders can't be deleted in a channel.
	 * However you can delete an inherited folder in the master and unlocalize a localized folder.
	 * @param id id of the folder to be deleted. This can either be local or global id
	 * @return generic response
	 */
	@POST
	@Path("/delete/{id}")
	GenericResponse delete(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId);

	/**
	 * Remove the folder denoted by the given id from the wastebin.
	 *
	 * @param id
	 *            id of the folder to remove from the wastebin. This can either be
	 *            a local or global id
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/delete/{id}")
	GenericResponse deleteFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Delete folders denoted by the set of IDs from the wastebin
	 * @param request request containing ID set
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/delete")
	GenericResponse deleteFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Restore the given folder from the wastebin
	 *
	 * @param id
	 *            id of the folder to restore from the wastebin. This can either
	 *            be a local or global id
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/restore/{id}")
	GenericResponse restoreFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Restore the denoted folders from the wastebin
	 * @param request request containing ID set
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/restore")
	GenericResponse restoreFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Get the privileges of the current user on the given folder
	 * @param id id of the folder.
	 * @return privileges response
	 */
	@GET
	@Path("/privileges/{id}")
	PrivilegesResponse getPrivileges(@PathParam("id") Integer id);

	/**
	 * Get the object counts for objects in the specified folder
	 * @param id id of the folder
	 * @param nodeId node id
	 * @param language language code
	 * @param inherited true to only count inherited objects, false to count not inherited objects, null (default) to count both
	 * @param inFolder folder parameters
	 * @param wastebinParams wastebin search option
	 * @return response containing the object counts
	 */
	@GET
	@Path("/count/{id}")
	FolderObjectCountResponse getObjectCounts(
		@PathParam("id") Integer id,
		@QueryParam("nodeId") Integer nodeId,
		@QueryParam("language") String language,
		@QueryParam("inherited") Boolean inherited,
		@BeanParam InFolderParameterBean inFolder,
		@BeanParam WastebinParameterBean wastebinParams);

	/**
	 * Request to set a startpage for a page
	 * @param id folder ID
	 * @param request request containing the requested startpage
	 * @return generic response
	 */
	@POST
	@Path("/startpage/{id}")
	GenericResponse setStartpage(@PathParam("id") String id, StartpageRequest request);

	/**
	 * Move the folder with id to a different location
	 * @param id id of the folder to move
	 * @param request request containing target information
	 * @return generic response
	 * @throws Exception in case of errors
	 */
	@POST
	@Path("/move/{id}")
	GenericResponse move(@PathParam("id") String id, FolderMoveRequest request) throws Exception;

	/**
	 * Move multiple folders to a different location
	 * @param request request containing source and target information
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/move")
	GenericResponse move(MultiFolderMoveRequest request) throws Exception;

	/**
	 * Sanitize the given folder publish directory according to the configuration and settings of the given node
	 * @param request request
	 * @return response containing the sanitized publish directory
	 * @throws Exception
	 */
	@POST
	@Path("/sanitize/publishDir")
	FolderPublishDirSanitizeResponse sanitizePubdir(FolderPublishDirSanitizeRequest request) throws Exception;
}
