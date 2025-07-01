package com.gentics.contentnode.rest.resource;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.ContentTagCreateRequest;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.MultiObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.MultiPageAssignRequest;
import com.gentics.contentnode.rest.model.request.MultiPageLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiPagePublishRequest;
import com.gentics.contentnode.rest.model.request.MultiPubqueueApproveRequest;
import com.gentics.contentnode.rest.model.request.MultiTagCreateRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageIdSetRequest;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.model.request.PagePreviewRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.TagSortAttribute;
import com.gentics.contentnode.rest.model.request.WorkflowRequest;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.PageFilenameSuggestRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.MultiPageLoadResponse;
import com.gentics.contentnode.rest.model.response.MultiTagCreateResponse;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PagePreviewResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.PrivilegesResponse;
import com.gentics.contentnode.rest.model.response.ReferencedFilesListResponse;
import com.gentics.contentnode.rest.model.response.ReferencedPagesListResponse;
import com.gentics.contentnode.rest.model.response.TagCreateResponse;
import com.gentics.contentnode.rest.model.response.TagListResponse;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.rest.model.response.page.PageFilenameSuggestResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource used for loading, saving and manipulating GCN pages.
 */
@Path("/page")
public interface PageResource extends AuthenticatedResource {

	/**
	 * Get a list of pages in the specified folder.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>fileName</code></li>
	 * <li><code>description</code></li>
	 * <li><code>niceUrl</code></li>
	 * <li><code>alternateUrls</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>fileName</code></li>
	 * <li><code>niceUrl</code></li>
	 * <li><code>alternateUrls</code></li>
	 * </ul>
	 * @param inFolder Folder ID and recursive parameters
	 * @param pageListParams Further page parameters
	 * @param filterParams Filter parameters
	 * @param sortingParams Sorting parameters
	 * @param pagingParams Paging parameters
	 * @param publishParams Publishable parameters
	 * @param wastebinParams Wastebin parameters
	 * @return A list of pages
	 */
	@GET
	PageListResponse list(
		@BeanParam InFolderParameterBean inFolder,
		@BeanParam PageListParameterBean pageListParams,
		@BeanParam FilterParameterBean filterParams,
		@BeanParam SortParameterBean sortingParams,
		@BeanParam PagingParameterBean pagingParams,
		@BeanParam PublishableParameterBean publishParams,
		@BeanParam WastebinParameterBean wastebinParams);

	/**
	 * Create a page based on the given pagecreaterequest
	 * @param request containing data for creating the page
	 * @return PageLoadResponse with the created page
	 */
	@POST
	@Path("/create")
	PageLoadResponse create(PageCreateRequest request);

	/**
	 * Copy a batch of pages or a single copy using the {@link PageCopyRequest} options.
	 *
	 * @param request Request containing data for the batch copy
	 * @param waitMs wait timeout in milliseconds
	 * @return GenericResponse
	 */
	@POST
	@Path("/copy")
	PageCopyResponse copy(PageCopyRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Move the given page to another folder
	 * @param id page id
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/move/{id}")
	GenericResponse move(@PathParam("id") String id, ObjectMoveRequest request);

	/**
	 * Move multiple pages to another folder
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/move")
	GenericResponse move(MultiObjectMoveRequest request);

	/**
	 * Saves a page into GCN
	 * @param id Id of the page to save. This can either be a local or global id
	 * @param request page save request
	 * @return GenericResponse
	 */
	@POST
	@Path("/save/{id}")
	GenericResponse save(@PathParam("id") String id, PageSaveRequest request);

	/**
	 * Loads a page from GCN.
	 * For now this page only contains meta data about the page and no contents.
	 *
	 * When using loading pages, it is important to note that the returned tag
	 * data may be loaded differently depending on whether or not the page is
	 * loaded for editing ("update" flag is set to true).
	 *
	 * Loading a page with the update flag set to false allows you to load the
	 * page without causing it to be locked for editing, but it means that any
	 * tags which have not been filled will be loaded without all their tag
	 * parts. Tags that have not been filled, and that do not have default
	 * values will be loaded with empty properties in this case.
	 *
	 * On the other hand, when loading pages for editing, any tags that have no
	 * been filled, will have their constituents part auto-generated.
	 *
	 * Consequently, when loading pages via the REST-API or GCNJSAPI, setting
	 * the update flag to true will cause all tags to be loaded with all parts
	 * data (auto-generated if necessary), whereas when loading a page with
	 * update set to false, any tags which have not had their parts filled will
	 * be loaded without their parts data.
	 *
	 * @param id The id of the page to load. This can either be a localid or a globalid
	 * @param update true when the page shall be locked, false if not
	 * @param template true if the template information shall be added
	 * @param folder true if the folder information shall be added
	 * @param languageVariants true if the language variants shall be added
	 * @param workflow true if the workflow information shall be added
	 * @param translationStatus true if the translationstatus information shall be added
	 * @param versionInfo true if version information shall be added
	 * @param disinherited if true, disinherited channel nodes shall be added
	 * @param construct if true, the construct info will be added to tags
	 * @param nodeId channel id
	 * @param stagingPackageName name of a content staging package, to check the page status in it
	 * @return PageLoadResponse Response with the loaded page
	 */
	@GET
	@Path("/load/{id}")
	PageLoadResponse load(
			@PathParam("id") String id,
			@DefaultValue("false") @QueryParam("update") boolean update,
			@QueryParam("template") @DefaultValue("false") boolean template,
			@QueryParam("folder") @DefaultValue("false") boolean folder,
			@QueryParam("langvars") @DefaultValue("false") boolean languageVariants,
			@QueryParam("pagevars") @DefaultValue("false") boolean pageVariants,
			@QueryParam("workflow") @DefaultValue("false") boolean workflow,
			@QueryParam("translationstatus") @DefaultValue("false") boolean translationStatus,
			@QueryParam("versioninfo") @DefaultValue("false") boolean versionInfo,
			@QueryParam("disinherited") @DefaultValue("false") boolean disinherited,
			@QueryParam("construct") @DefaultValue("false") boolean construct,
			@QueryParam("nodeId") Integer nodeId, 
			@QueryParam("package") String stagingPackageName);

	/**
	 * Load a list of pages specified in the given {@link MultiPageLoadResponse request}.
	 *
	 * Page ids for which no pages exist, or the user does not have the necessary
	 * permissions, are silently ignored.
	 *
	 * @param request The request with he list of page ids to load.
	 * @param fillWithNulls flag to have items, which cannot be loaded returned as "null" objects in the response (instead of just omitting them)
	 *
	 * @return The list of found pages, for which the user has enough permissions.
	 */
	@POST
	@Path("/load")
	MultiPageLoadResponse load(MultiPageLoadRequest request, @QueryParam("fillWithNulls") @DefaultValue("false") boolean fillWithNulls);

	/**
	 * Render given page in a preview (before actually saving it)
	 * @return preview of the page
	 * @throws NodeException
	 */
	@POST
	@Path("/preview")
	PagePreviewResponse preview(PagePreviewRequest request);

	/**
	 * Inform a list of users that the list of pages have been put back into
	 * revision.
	 *
	 * @param request
	 * @return response
	 */
	@POST
	@Path("/assign")
	GenericResponse assign(MultiPageAssignRequest request);

	/**
	 * Publish a list of pages. Instant publishing will not be done, when using this method.
	 * @param nodeId channel id
	 * @param request publish request
	 * @return response object
	 */
	@POST
	@Path("/publish")
	GenericResponse publish(@QueryParam("nodeId") Integer nodeId, MultiPagePublishRequest request);

	/**
	 * Publish the page denoted by id (or send it in the queue when the User has not permission to publish the page).
	 * If the page is published and the node publishes into a contentrepository with instant publishing, the page will
	 * immediately be written into the contentrepository
	 * @param id id of the Page to publish. This can either be a local or globalid
	 * @param nodeId channel id
	 * @param request publish page request
	 * @return response object
	 */
	@POST
	@Path("/publish/{id}")
	GenericResponse publish(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, PagePublishRequest request);

	/**
	 * Delete the page denoted by id. Note that inherited or localized pages can't be deleted in a channel.
	 * However you can delete an inherited page in the master and unlocalize a localized page.
	 * @param id id of the Page to delete. This can either be a local or global id
	 * @param nodeId channel id
	 * @param noCrSync don't sync the deletion with the content repo
	 * @return response object
	 */
	@POST
	@Path("/delete/{id}")
	GenericResponse delete(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, @QueryParam("noSync") Boolean noCrSync);

	/**
	 * Remove the page denoted by the given id from the wastebin.
	 *
	 * @param id
	 *			id of the page to remove from the wastebin. This can either be
	 *			a local or global id
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/delete/{id}")
	GenericResponse deleteFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Remove the given pages from the wastebin
	 * @param request request containing the page IDs
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/delete")
	GenericResponse deleteFromWastebin(PageIdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Restore the given page from the wastebin
	 *
	 * @param id
	 *			id of the page to restore from the wastebin. This can either
	 *			be a local or global id
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/restore/{id}")
	GenericResponse restoreFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Restore the given pages from the wastebin
	 * @param request request containing the page IDs
	 * @param waitMs time in ms for the request to be done in foreground
	 * @return response object
	 */
	@POST
	@Path("/wastebin/restore")
	GenericResponse restoreFromWastebin(PageIdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs);

	/**
	 * Cancel editing the page. The page will be rolled back to the last version (changes made since the last version will be lost).
	 * The edit lock on the page will be raised, so that other users may edit the page.
	 * @param id id of the Page which is currently edited
	 * @param nodeId channel id
	 * @return response object
	 */
	@POST
	@Path("/cancel/{id}")
	GenericResponse cancel(@PathParam("id") Integer id, @QueryParam("nodeId") Integer nodeId);

	/**
	 * Render a preview of the posted page
	 * @param nodeId node id (if rendering a page for a channel)
	 * @param template template to render (if not set, the page's template will be rendered)
	 * @param editMode true for rendering in edit mode, false for preview mode
	 * @param proxyprefix proxyprefix
	 * @param linksType type of links (frontend or backend)
	 * @param tagmap true to also render the tagmap entries
	 * @param inherited true to render the inherited content and properties
	 * @param publish True to render in publish mode
	 * @return response containing the rendered page and other important information
	 */
	@POST
	@Path("/render")
	PageRenderResponse render(@QueryParam("nodeId") Integer nodeId,
			@QueryParam("template") String template,
			@QueryParam("edit") @DefaultValue("false") boolean editMode,
			@QueryParam("proxyprefix") String proxyprefix,
			@QueryParam("links") @DefaultValue("backend") LinksType linksType,
			@QueryParam("tagmap") @DefaultValue("false") boolean tagmap,
			@QueryParam("inherited") @DefaultValue("false") boolean inherited,
			@QueryParam("publish") @DefaultValue("false") boolean publish, Page page);

	/**
	 * Render the given page
	 * Info: This currently doesn't return tag render errors.
	 *
	 * @param id id of the page
	 * @param nodeId node id (if rendering a page for a channel)
	 * @param template template to render (if not set, the page's template will be rendered)
	 * @param editMode true for rendering in edit mode, false for preview mode
	 * @param proxyprefix proxyprefix
	 * @param linksType type of links (frontend or backend)
	 * @param tagmap true to also render the tagmap entries
	 * @param inherited true to render the inherited content and properties
	 * @param publish True to render in publish mode
	 * @param versionTimestamp optional version timestamp for rendering a version of the page
	 * @return response containing the rendered page and other important information
	 */
	@GET
	@Path("/render/{id}")
	PageRenderResponse render(@PathParam("id") String id,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("template") String template,
			@QueryParam("edit") @DefaultValue("false") boolean editMode,
			@QueryParam("proxyprefix") String proxyprefix,
			@QueryParam("links") @DefaultValue("backend") LinksType linksType,
			@QueryParam("tagmap") @DefaultValue("false") boolean tagmap,
			@QueryParam("inherited") @DefaultValue("false") boolean inherited,
			@QueryParam("publish") @DefaultValue("false") boolean publish,
			@QueryParam("version") Integer versionTimestamp);

	/**
	 * Render the content of the given page in preview mode
	 * @param id page ID
	 * @param nodeId optional node ID
	 * @param versionTimestamp optional version timestamp for rendering a version of the page
	 * @return page content response
	 */
	@GET
	@Path("/render/content/{id}")
	Response renderContent(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, @QueryParam("version") Integer versionTimestamp);

	/**
	 * Render the difference between two versions of the page
	 * @param id page ID
	 * @param nodeId optional node ID
	 * @param oldVersion old page version
	 * @param newVersion new page version
	 * @param source true to show the diff in the source code, otherwise the diff is shown in the html
	 * @param daisyDiff true to use the daisy diff algorithm when rendering html diff (ignored when rendering source diff)
	 * @return page content response
	 */
	@GET
	@Path("/diff/versions/{id}")
	Response diffVersions(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId,
			@QueryParam("old") @DefaultValue("0") int oldVersion, @QueryParam("new") @DefaultValue("0") int newVersion,
			@QueryParam("source") @DefaultValue("false") boolean source,
			@QueryParam("daisyDiff") @DefaultValue("false") boolean daisyDiff);

	/**
	 * Render the difference between the page and another page
	 * @param id page ID
	 * @param nodeId optional node ID
	 * @param otherPageId ID of the other page
	 * @param source true to show the diff in the source code, otherwise the diff is shown in the html
	 * @param daisyDiff true to use the daisy diff algorithm when rendering html diff (ignored when rendering source diff)
	 * @return page content response
	 */
	@GET
	@Path("/diff/{id}")
	Response diffWithOtherPage(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId,
			@QueryParam("otherPageId") @DefaultValue("0") int otherPageId,
			@QueryParam("source") @DefaultValue("false") boolean source,
			@QueryParam("daisyDiff") @DefaultValue("false") boolean daisyDiff);

	/**
	 * Render a tag preview for the posted page
	 * @param tag name of the tag to render
	 * @param nodeId node id (if rendering a page for a channel)
	 * @param proxyprefix proxyprefix
	 * @param linksType type of links (frontend or backend)
	 * @param page posted page
	 * @return response containing the rendered tag and other important information
	 */
	@POST
	@Path("/renderTag/{tag}")
	PageRenderResponse renderTag(@PathParam("tag") String tag,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("proxyprefix") String proxyprefix,
			@QueryParam("links") @DefaultValue("backend") LinksType linksType,
			Page page);

	/**
	 * Render a tag of the given page
	 * @param id id of the page
	 * @param tag name of the tag to render
	 * @param nodeId node id (if rendering a page for a channel)
	 * @param proxyprefix proxyprefix
	 * @param linksType type of links (frontend or backend)
	 * @return response containing the rendered tag and other important information
	 */
	@GET
	@Path("/renderTag/{id}/{tag}")
	PageRenderResponse renderTag(@PathParam("id") String id,
			@PathParam("tag") String tag,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("proxyprefix") String proxyprefix,
			@QueryParam("links") @DefaultValue("backend") LinksType linksType);

	/**
	 * Get the list of tags (contenttags and templatetags) for this page
	 * @param id id of the page
	 * @return response object
	 */
	@GET
	@Path("/getTags/{id}")
	TagListResponse getTags(@PathParam("id") Integer id,
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			final @QueryParam("sortby") @DefaultValue("name") TagSortAttribute sortBy,
			final @QueryParam("sortorder") @DefaultValue("asc") SortOrder sortOrder,
			@QueryParam("search") String search);

	/**
	 * Get the privileges of the current user on the given page
	 * @param id id of the page
	 * @return privileges response
	 */
	// @GET
	// @Path("/privileges/{id}")
	PrivilegesResponse getPrivileges(@PathParam("id") Integer id);

	/**
	 * Create a new tag in the given page
	 * @param id id of the page. This can either be a local or globalid
	 * @param constructId id of the tags construct
	 * @param keyword keyword of the construct
	 * @param request tag create request
	 * @return response containing the rendered tag
	 */
	@POST
	@Path("/newtag/{id}")
	TagCreateResponse createTag(@PathParam("id") String id,
			@QueryParam("constructId") Integer constructId,
			@QueryParam("keyword") String keyword,
			ContentTagCreateRequest request);

	/**
	 * Create multiple tags in the given page
	 * @param id id of the page
	 * @param request tag create request
	 * @return response containing the created tags
	 */
	@POST
	@Path("/newtags/{id}")
	MultiTagCreateResponse createTags(@PathParam("id") String id, MultiTagCreateRequest request);

	/**
	 * Restore the page version with the given version timestamp
	 * @param id The id of the page that should be restored. This can either be a local or globalid
	 * @param versionTimestamp version timestamp
	 * @Return restored page
	 */
	@POST
	@Path("/restore/{id}")
	PageLoadResponse restoreVersion(@PathParam("id") String id, @QueryParam("version") Integer versionTimestamp);

	/**
	 * Restore a single tag in the page with the given version timestamp
	 * @param pageId id of the page. This can either be a local or globalid
	 * @param tag id or name of the tag
	 * @param versionTimestamp version timestamp
	 * @return restored tag
	 */
	@POST
	@Path("/restore/{pageid}/{tag}")
	TagListResponse restoreTag(@PathParam("pageid") String pageId, @PathParam("tag") String tag, @QueryParam("version") Integer versionTimestamp);

	/**
	 * Translate the page into the given language.
	 * When the language variant of the page exists, it is just locked and returned, otherwise the page is copied into the language variant and returned.
	 * This method fails, if the requested language is not available for the node of the page or the user has no permission to create/edit the given language variant
	 * @param id id of the page to translate
	 * @param languageCode code of the language into which the page shall be translated
	 * @param locked true if the translation shall be locked, false if not
	 * @param channelId for multichannelling, specify channel in which to create page (can be 0 or equal to node ID to be ignored)
	 * @return page load response
	 */
	@POST
	@Path("/translate/{id}")
	PageLoadResponse translate(@PathParam("id") Integer id,
			@QueryParam("language") String languageCode,
			@QueryParam("locked") @DefaultValue("true") boolean locked, @QueryParam("channelId") Integer channelId);

	/**
	 * Revoke the last step of the workflow
	 * @param id id of the page
	 * @return generic response
	 */
	@POST
	@Path("/workflow/revoke/{id}")
	GenericResponse workflowRevoke(@PathParam("id") Integer id);

	/**
	 * Decline the workflow for the page (i.e. give it back to a lower group)
	 * @param id id of the page. This can either be a local or globalid
	 * @param request request object containing what should be changed about the workflow
	 * @return generic response
	 */
	@POST
	@Path("/workflow/decline/{id}")
	GenericResponse workflowDecline(@PathParam("id") String id, WorkflowRequest request);

	/**
	 * Get the total usage information of the given pages.
	 *
	 * @param pageId
	 *			list of page ids
	 * @param nodeId
	 *			id of the node
	 * @return Total usage response
	 */
	@GET
	@Path("/usage/total")
	TotalUsageResponse getTotalPageUsage(@QueryParam("id") List<Integer> pageId, @QueryParam("nodeId") Integer nodeId);

	/**
	 * Get the pages containing pagetags pointing to one of the given pages.
	 *
	 * @param skipCount
	 *			number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *			maximum number of items to be returned, set to -1 for
	 *		returning all items
	 * @param sortBy
	 *			(optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *			(optional) result sort order - may be "asc" for ascending or
	 *			"desc" for descending other strings will be ignored. defaults
	 *			to "asc".
	 * @param pageId list of page ids
	 * @param nodeId id of the node
	 * @param returnPages true (default) if the pages shall be returned, false for only returning the counts
	 * @param pageModel page model parameters
	 * @return list of pages
	 */
	@GET
	@Path("/usage/tag")
	PageUsageListResponse getPagetagUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel);

	/**
	 * Get the page variants of the given pages
	 * @param skipCount
	 *			number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *			maximum number of items to be returned, set to -1 for
	 *		returning all items
	 * @param sortBy
	 *			(optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *			(optional) result sort order - may be "asc" for ascending or
	 *			"desc" for descending other strings will be ignored. defaults
	 *			to "asc".
	 * @param pageId list of page ids
	 * @param nodeId id of the node
	 * @param returnPages true (default) if the pages shall be returned, false for only returning the counts
	 * @param pageModel page model parameters
	 * @return list of page variants
	 */
	@GET
	@Path("/usage/variant")
	PageUsageListResponse getVariantsUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel);

	/**
	 * Get the pages using one of the given pages
	 * @param skipCount
	 *			number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *			maximum number of items to be returned, set to -1 for
	 *		returning all items
	 * @param sortBy
	 *			(optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *			(optional) result sort order - may be "asc" for ascending or
	 *			"desc" for descending other strings will be ignored. defaults
	 *			to "asc".
	 * @param pageId list of page ids, for which the usage shall be fetched
	 * @param nodeId id of the node
	 * @param returnPages true (default) if the pages shall be returned, false for only returning the counts
	 * @param pageModel page model parameters
	 * @return list of pages using the given pages
	 */
	@GET
	@Path("/usage/page")
	PageUsageListResponse getPageUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel);

	/**
	 * Get the templates using one of the given pages
	 * @param skipCount
	 *			number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *			maximum number of items to be returned, set to -1 for
	 *		returning all items
	 * @param sortBy
	 *			(optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *			(optional) result sort order - may be "asc" for ascending or
	 *			"desc" for descending other strings will be ignored. defaults
	 *			to "asc".
	 * @param pageId list of page ids, for which the usage shall be fetched
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
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("templates") @DefaultValue("true") boolean returnTemplates);

	/**
	 * Get pages, which are used by one of the given pages
	 * @param skipCount
	 *			number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *			maximum number of items to be returned, set to -1 for
	 *		returning all items
	 * @param sortBy
	 *			(optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *			(optional) result sort order - may be "asc" for ascending or
	 *			"desc" for descending other strings will be ignored. defaults
	 *			to "asc".
	 * @param pageId list of page ids, for which the usage shall be fetched
	 * @param nodeId id of the node
	 * @return response
	 */
	@GET
	@Path("/usage/linkedPage")
	ReferencedPagesListResponse getLinkedPages(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId);

	/**
	 * Get files, which are used by one of the given pages
	 * @param skipCount
	 *			number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *			maximum number of items to be returned, set to -1 for
	 *		returning all items
	 * @param sortBy
	 *			(optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *			(optional) result sort order - may be "asc" for ascending or
	 *			"desc" for descending other strings will be ignored. defaults
	 *			to "asc".
	 * @param pageId list of page ids, for which the usage shall be fetched
	 * @param nodeId id of the node
	 * @return response
	 */
	@GET
	@Path("/usage/linkedFile")
	ReferencedFilesListResponse getLinkedFiles(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId);

	/**
	 * Get images, which are used by one of the given pages
	 * @param skipCount
	 *			number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *			maximum number of items to be returned, set to -1 for
	 *		returning all items
	 * @param sortBy
	 *			(optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *			(optional) result sort order - may be "asc" for ascending or
	 *			"desc" for descending other strings will be ignored. defaults
	 *			to "asc".
	 * @param pageId list of page ids, for which the usage shall be fetched
	 * @param nodeId id of the node
	 * @return response
	 */
	@GET
	@Path("/usage/linkedImage")
	ReferencedFilesListResponse getLinkedImages(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId);

	/**
	 * Take a specific page offline
	 *
	 * @param id  ID of the page
	 * @return	Generic response
	 */
	@POST
	@Path("/takeOffline/{id}")
	GenericResponse takeOffline(@PathParam("id") String id, PageOfflineRequest request);

	/**
	 * Do a quick search for pages that contain the given query string in their name or have it as ID.
	 * Return an html representation of the page list, sorted by node name, folder name and page name.
	 * @param q query string, must contain at least three characters (otherwise an empty result is returned).
	 * @param limit maximum number of results returned. Default is 15
	 * @return html representation of matching pages
	 */
	@GET
	@Path("/autocomplete")
	@Produces(MediaType.TEXT_HTML)
	String autocomplete(@QueryParam("q") String q, @QueryParam("limit") int limit);

	/**
	 * NOTE:
	 * This endpoint is currently under construction and will be changed in the future.
	 *
	 * Load a page with which resolves to the given liveUrl
	 *
	 * Please note that the page can only be resolved to a liveUrl if the was
	 * already published once.
	 * The resolving of liveUrls is always coupled to the publish state of the
	 * page. Local changes which have not been published will not be incorporated
	 * in the resolving process.
	 *
	 * Loads a page from GCN. For now this page only contains meta data about
	 * the page and no contents.
	 *
	 * When using loading pages, it is important to note that the returned tag
	 * data may be loaded differently depending on whether or not the page is
	 * loaded for editing ("update" flag is set to true).
	 *
	 * Loading a page with the update flag set to false allows you to load the
	 * page without causing it to be locked for editing, but it means that any
	 * tags which have not been filled will be loaded without all their tag
	 * parts. Tags that have not been filled, and that do not have default
	 * values will be loaded with empty properties in this case.
	 *
	 * On the other hand, when loading pages for editing, any tags that have no
	 * been filled, will have their constituents part auto-generated.
	 *
	 * Consequently, when loading pages via the REST-API or GCNJSAPI, setting
	 * the update flag to true will cause all tags to be loaded with all parts
	 * data (auto-generated if necessary), whereas when loading a page with
	 * update set to false, any tags which have not had their parts filled will
	 * be loaded without their parts data.
	 *
	 * @param update
	 *			true when the page shall be locked, false if not
	 * @param template
	 *			true if the template information shall be added
	 * @param folder
	 *			true if the folder information shall be added
	 * @param languageVariants
	 *			true if the language variants shall be added
	 * @param workflow
	 *			true if the workflow information shall be added
	 * @param translationStatus
	 *			true if the translationstatus information shall be added
	 * @param versionInfo
	 *			true if version information shall be added
	 * @param disinherited
	 *			if true, disinherited channel nodes shall be added
	 * @param nodeId
	 *			Optional channel id which can be specified to restrict search to a specific channel. Adding the channel id will speed up the search process.
	 * @param liveUrl Live url to search for. (e.g.: hostname.tld/folderA/index.html)
	 * @return PageLoadResponse Response with the loaded page
	 */
	@GET
	@Path("/search")
	PageLoadResponse search(
			@DefaultValue("false") @QueryParam("update") boolean update,
			@QueryParam("template") @DefaultValue("false") boolean template,
			@QueryParam("folder") @DefaultValue("false") boolean folder,
			@QueryParam("langvars") @DefaultValue("false") boolean languageVariants,
			@QueryParam("pagevars") @DefaultValue("false") boolean pageVariants,
			@QueryParam("workflow") @DefaultValue("false") boolean workflow,
			@QueryParam("translationstatus") @DefaultValue("false") boolean translationStatus,
			@QueryParam("versioninfo") @DefaultValue("false") boolean versionInfo,
			@QueryParam("disinherited") @DefaultValue("false") boolean disinherited,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("liveUrl") String liveUrl);

	/**
	 * Get all pages (for all nodes) in the publish queue
	 * @param skipCount
	 *			number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *			maximum number of items to be returned, set to -1 for
	 *		returning all items
	 * @param search
	 *			(optional) search string (may be empty for no search) - this will
	 *			filter the results if either the ID, the name (partial match or
	 *			the description (partial match) matches the given search string.
	 * @param sortBy
	 *			(optional) attribute to sort by. It is possible to sort by name,
	 *			cdate, edate, pdate, filename, template, folder, masterNode,
	 *			priority, excluded, deletedat. defaults to name
	 * @param sortOrder
	 *			(optional) result sort order - may be "asc" for ascending or
	 *			"desc" for descending other strings will be ignored. defaults
	 *			to "asc".
	 * @return list of pages in the publish queue
	 */
	@GET
	@Path("/pubqueue")
	LegacyPageListResponse pubqueue(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("search") String search,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder);

	/**
	 * Approved queued changes to online status of pages
	 * @param request request containing page IDs
	 * @param waitMs wait timeout in milliseconds
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/pubqueue/approve")
	GenericResponse pubqueueApprove(MultiPubqueueApproveRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs) throws Exception;

	/**
	 * Suggest the filename to be used for a (new) page, based on other metadata
	 * @param request request
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/suggest/filename")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Suggested fileName was returned."),
		@ResponseCode(code = 403, condition = "User has insufficient permissions on the folder."),
		@ResponseCode(code = 404, condition = "Either referenced folder or template does not exist.")
	})
	PageFilenameSuggestResponse suggestFilename(PageFilenameSuggestRequest request) throws Exception;
}
