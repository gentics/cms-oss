package com.gentics.contentnode.rest.resource;

import java.io.InputStream;
import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.model.request.ImageCreateRequest;
import com.gentics.contentnode.rest.model.request.ImageResizeRequest;
import com.gentics.contentnode.rest.model.request.ImageRotateRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.FileUsageListResponse;
import com.gentics.contentnode.rest.model.response.FolderUsageListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageListResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.model.response.MultiImageLoadResponse;
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
 * Resource for loading and manipulating Images in GCN
 * @author norbert
 */
@Path("/image")
public interface ImageResource extends AuthenticatedResource {

	/**
	 * Get a list of images in the specified folder.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>niceUrl</code></li>
	 * <li><code>alternateUrls</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>niceUrl</code></li>
	 * <li><code>alternateUrls</code></li>
	 * <li><code>fileSize</code></li>
	 * <li><code>fileType</code></li>
	 * </ul>
	 * @param inFolder Folder ID and recursive parameters
	 * @param fileListParams Further file parameters
	 * @param filterParams Filter parameters
	 * @param sortingParams Sorting parameters
	 * @param pagingParams Paging parameters
	 * @param editableParams Editable parameters
	 * @param wastebinParams Wastebin parameters
	 * @return A list of images
	 */
	@GET
	ImageListResponse list(
		@BeanParam InFolderParameterBean inFolder,
		@BeanParam FileListParameterBean fileListParams,
		@BeanParam FilterParameterBean filterParams,
		@BeanParam SortParameterBean sortingParams,
		@BeanParam PagingParameterBean pagingParams,
		@BeanParam EditableParameterBean editableParams,
		@BeanParam WastebinParameterBean wastebinParams);

	/**
	 * Load the image with given id
	 * @param id image id. This can either be a local or globalid
	 * @param update
	 *            true when the image is fetched for updating. Currently, images
	 *            cannot be locked in the backend, but it is still recommended
	 *            to set this parameter to true when the image shall be modified.
	 * @param construct if true, the construct information will be added to tags
	 * @param nodeId id of the node (channel) for which the image shall be
	 *               loaded (when multichannelling is used).
	 * @param stagingPackageName name of a content staging package, to check the image status in it
	 * @return response containing the image meta data
	 */
	@GET
	@Path("/load/{id}")
	ImageLoadResponse load(
			@PathParam("id") String id,
			@DefaultValue("false") @QueryParam("update") boolean update,
			@DefaultValue("false") @QueryParam("construct") boolean construct,
			@QueryParam("nodeId") Integer nodeId, 
			@QueryParam("package") String stagingPackageName
			);

	/**
	 * Load a list of images specified in the given {@link MultiObjectLoadRequest request}.
	 *
	 * Image ids for which no images exist, or the user does not have the necessary
	 * permissions, are silently ignored.
	 *
	 * @param request The request with he list of image ids to load.
	 * @param fillWithNulls flag to have items, which cannot be loaded returned as "null" objects in the response (instead of just omitting them)
	 *
	 * @return The list of found images, for which the user has enough permissions.
	 */
	@POST
	@Path("/load")
	MultiImageLoadResponse load(MultiObjectLoadRequest request, @QueryParam("fillWithNulls") @DefaultValue("false") boolean fillWithNulls);

	/**
	 * Rotate by 90° (optionally), crop (optionally) and resize an image (in this order).
	 * @param imageResizeRequest resize request
	 * @return response
	 */
	@POST
	@Path("/resize/")
	FileUploadResponse resize(ImageResizeRequest imageResizeRequest);

	/**
	 * Rotate an image by 90°
	 * @param request rotate request
	 * @return response
	 */
	@POST
	@Path("/rotate")
	ImageLoadResponse rotate(ImageRotateRequest request);

	/**
	 * Get the content of the image with given id
	 * @param id image id to get
	 * @return binary content of the image
	 */
	// @GET
	// @Path("/content/load/{id}")
	// @Produces("image/*")
	Response loadContent(@PathParam("id") Integer id);

	/**
	 * Create a new image
	 * @param request request with data for the image to be created
	 * @return response containing the image meta data
	 */
	// @POST
	// @Path("/create")
	ImageLoadResponse create(ImageCreateRequest request);


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
	 * Save the given image
	 * @param request request with the image to be saved
	 * @return generic response
	 */
	@POST
	@Path("/save/{id}")
	GenericResponse save(@PathParam("id") Integer id, ImageSaveRequest request);

	/**
	 * Save the posted content into the given image
	 * @param fileContent image content
	 * @return generic response
	 */
	// @POST
	// @Path("/content/save/{id}")
	// @Consumes("image/*")
	GenericResponse saveContent(InputStream fileContent);

	/**
	 * Delete the image denoted by id
	 * @param id id of the image to delete
	 * @param nodeId id of the node (channel) of the image
	 * @param noCrSync don't sync the deletion with the content repo
	 * @return response object
	 */
	@POST
	@Path("/delete/{id}")
	GenericResponse delete(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, @QueryParam("noSync") Boolean noCrSync);

	/**
	 * Remove the image denoted by the given id from the wastebin.
	 *
	 * @param id
	 *            id of the image to remove from the wastebin. This can either be
	 *            a local or global id
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/delete/{id}")
	GenericResponse deleteFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Remove the given images from the wastebin
	 * @param request request containing the image IDs
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/delete")
	GenericResponse deleteFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Restore the given image from the wastebin
	 *
	 * @param id
	 *            id of the image to restore from the wastebin. This can either
	 *            be a local or global id
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/restore/{id}")
	GenericResponse restoreFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Restore the given images from the wastebin
	 * @param request request containing the image IDs
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/restore")
	GenericResponse restoreFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Get the privileges of the current user on the given image
	 * @param id id of the image
	 * @return privileges response
	 */
	// @GET
	// @Path("/privileges/{id}")
	PrivilegesResponse getPrivileges(@PathParam("id") Integer id);

	/**
	 * Get the total usage info for the given images.
	 *
	 * @param imageId
	 *            list of image ids, for which the usage shall be fetched
	 * @param nodeId
	 *            id of the node
	 * @return Response which contains the total usage info
	 */
	@GET
	@Path("/usage/total")
	TotalUsageResponse getTotalFileUsageInfo(@QueryParam("id") List<Integer> imageId, @QueryParam("nodeId") Integer nodeId);

	/**
	 * Get the folders using one of the given images.
	 *
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for
	 *        returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or
	 *            "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param imageId list of image ids, for which the usage shall be fetched
	 * @param nodeId id of the node
	 * @param returnFolders true (default) if the folders shall be returned, false for only returning the counts
	 * @return response
	 */
	@GET
	@Path("/usage/folder")
	FolderUsageListResponse getFolderUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("folders") @DefaultValue("true") boolean returnFolders);

	/**
	 * Get the pages using one of the given images
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for
	 *        returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or
	 *            "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param imageId list of image ids, for which the usage shall be fetched
	 * @param nodeId id of the node
	 * @param returnPages true (default) if the pages shall be returned, false for only returning the counts
	 * @param pageModel page model parameters
	 * @return response
	 */
	@GET
	@Path("/usage/page")
	PageUsageListResponse getPageUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel);

	/**
	 * Get the templates using one of the given images
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for
	 *        returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or
	 *            "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param imageId list of image ids, for which the usage shall be fetched
	 * @param nodeId id of the node
	 * @param returnTemplates true (default) if the templates shall be returned, false for only returning the counts
	 * @return response
	 */
	@GET
	@Path("/usage/template")
	TemplateUsageListResponse getTemplateUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("templates") @DefaultValue("true") boolean returnTemplates);

	/**
	 * Get the images using one of the given images
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for
	 *        returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or
	 *            "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param imageId list of image ids, for which the usage shall be fetched
	 * @param nodeId id of the node
	 * @param returnImages true (default) if the images shall be returned, false for only returning the counts
	 * @return response
	 */
	@GET
	@Path("/usage/image")
	FileUsageListResponse getImageUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("images") @DefaultValue("true") boolean returnImages);

	/**
	 * Get the files using one of the given images
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for
	 *        returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or
	 *            "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param imageId list of image ids, for which the usage shall be fetched
	 * @param nodeId id of the node
	 * @param returnFiles true (default) if the files shall be returned, false for only returning the counts
	 * @return response
	 */
	@GET
	@Path("/usage/file")
	FileUsageListResponse getFileUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("files") @DefaultValue("true") boolean returnFiles);
}
