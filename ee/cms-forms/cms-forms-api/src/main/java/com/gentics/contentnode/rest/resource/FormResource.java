package com.gentics.contentnode.rest.resource;

import java.util.List;

import com.gentics.contentnode.rest.model.form.FormListResponse;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationListResponse;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ItemVersionListResponse;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterFormDataParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterFormParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterFormTypeConfigurationsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FormDataExportParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource for forms
 * @HTTP 401 No valid sid and session secret cookie were provided.
 * @HTTP 403 User has insufficient permissions.
 */
@Path("form")
public interface FormResource {
	/**
	 * List forms.<br>
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
	 * </ul>
	 * @param inFolder parameter bean
	 * @param filter parameter bean
	 * @param sorting parameter bean
	 * @param paging parameter bean
	 * @param wastebin parameter bean
	 * @param publishable parameter bean
	 * @return response containing a list of forms
	 * @HTTP 200 The list of forms is returned.
	 * @HTTP 404 The given folder does not exist
	 * @throws Exception
	 */
	@GET
	FormListResponse list(@BeanParam InFolderParameterBean inFolder, @BeanParam FilterFormParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam WastebinParameterBean wastebin, @BeanParam PublishableParameterBean publishable) throws Exception;

	/**
	 * Create a new form in the folder
	 * @param form form
	 * @return response containing the created form
	 * @HTTP 201 The form was created
	 * @HTTP 404 The given folder does not exist
	 * @throws Exception
	 */
	@POST
	FormResponse create(FormModel form) throws Exception;

	/**
	 * Load a form
	 * @param formId form ID
	 * @param stagingPackageName name of a content staging package, to check the form status in the package
	 * @return response containing the form
	 * @HTTP 200 The form is returned
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@GET
	@Path("{id}")
	FormResponse get(@PathParam("id") String formId, @QueryParam("package") String stagingPackageName) throws Exception;

	/**
	 * Update a form
	 * @param formId form ID
	 * @param form form data to update
	 * @return updated form
	 * @HTTP 200 The form was updated
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@PUT
	@Path("{id}")
	FormResponse update(@PathParam("id") String formId, FormModel form) throws Exception;

	/**
	 * Delete a form
	 * @param formId form ID
	 * @param disableInstantDelete don't delete the item from the content repo immediately
	 * @return response
	 * @HTTP 200 The form was deleted
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@DELETE
	@Path("{id}")
	GenericResponse delete(@PathParam("id") String formId, @QueryParam("disableInstantDelete") Boolean disableInstantDelete) throws Exception;

	/**
	 * Publish a form
	 * @param formId form ID
	 * @param at timestamp for scheduled publishing (0 for publishing immediately)
	 * @param keepVersion true to keep the currently scheduled version (if "at" is not 0 and a form version is currently scheduled to be published)
	 * @return response containing the form
	 * @HTTP 200 The form was published
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@PUT
	@Path("{id}/online")
	FormResponse publish(@PathParam("id") String formId, @QueryParam("at") @DefaultValue("0") int at, @QueryParam("keepVersion") @DefaultValue("false") boolean keepVersion) throws Exception;

	/**
	 * Take a form offline
	 * @param formId form ID
	 * @param at timestamp for scheduled taking offline (0 for taking offline immediately)
	 * @return response containing the form
	 * @HTTP 200 The form was taken offline
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@DELETE
	@Path("{id}/online")
	FormResponse takeOffline(@PathParam("id") String formId, @QueryParam("at") @DefaultValue("0") int at) throws Exception;

	/**
	 * Remove the time for scheduled publishing
	 * @param formId form ID
	 * @return response containing the form
	 * @HTTP 200 The time management of the form was modified
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@DELETE
	@Path("{id}/publish/at")
	FormResponse removePublishAt(@PathParam("id") String formId) throws Exception;

	/**
	 * Remove the time for scheduled taking offline
	 * @param formId form ID
	 * @return response containing the form
	 * @HTTP 200 The time management of the form was modified
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@DELETE
	@Path("{id}/offline/at")
	FormResponse removeOfflineAt(@PathParam("id") String formId) throws Exception;

	/**
	 * Lock the form
	 * @param formId form ID
	 * @return response containing the locked form
	 * @HTTP 200 The form was locked
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@PUT
	@Path("{id}/lock")
	FormResponse lock(@PathParam("id") String formId) throws Exception;

	/**
	 * Unlock the form
	 * @param formId form ID
	 * @return response containing the unlocked form
	 * @HTTP 200 The form was unlocked
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@DELETE
	@Path("{id}/lock")
	FormResponse unlock(@PathParam("id") String formId) throws Exception;

	/**
	 * Get the versions of a form
	 * @param formId form ID
	 * @return response containing a list of versions
	 * @HTTP 200 The versions are returned
	 * @HTTP 404 The form with given ID does not exist
	 * @throws Exception
	 */
	@GET
	@Path("{id}/version")
	ItemVersionListResponse listVersions(@PathParam("id") String formId) throws Exception;

	/**
	 * Load a version of a form
	 * @param formId form ID
	 * @param version version number
	 * @return response containing the version number
	 * @HTTP 200 The version is returned
	 * @HTTP 404 The form with given ID or the version does not exist
	 * @throws Exception
	 */
	@GET
	@Path("{id}/version/{version}")
	FormResponse getVersion(@PathParam("id") String formId, @PathParam("version") String version) throws Exception;

	/**
	 * Restore a version of the form
	 * @param formId form ID
	 * @param version version number
	 * @return response containing the restored form
	 * @throws Exception
	 */
	@POST
	@Path("{id}/version/{version}/restore")
	FormResponse restoreVersion(@PathParam("id") String formId, @PathParam("version") String version) throws Exception;

	/**
	 * Remove a form from the wastebin
	 * @param formId form ID
	 * @return response
	 * @HTTP 200 Form was removed from the wastebin
	 * @HTTP 404 Form did not exist in the wastebin
	 * @throws Exception
	 */
	@DELETE
	@Path("wastebin/{id}")
	GenericResponse removeFromWastebin(@PathParam("id") String formId) throws Exception;

	/**
	 * Restore a form from the wastebin
	 * @param formId form ID
	 * @return response containing the restored form
	 * @HTTP 200 Form was restored from the wastebin
	 * @HTTP 404 Form did not exist in the wastebin
	 * @throws Exception
	 */
	@POST
	@Path("wastebin/{id}/restore")
	FormResponse restoreFromWastebin(@PathParam("id") String formId) throws Exception;

	/**
	 * Move the form to the given folder
	 * @param formId form ID
	 * @param folderId target folder ID
	 * @return folder response
	 * @HTTP 200 Form was moved to the folder
	 * @HTTP 404 Either the form or the folder do not exist
	 * @HTTP 409 Form cannot be moved, because of reasons TODO
	 * @throws Exception
	 */
	@PUT
	@Path("{id}/folder/{folderId}")
	FormResponse move(@PathParam("id") String formId, @PathParam("folderId") String folderId) throws Exception;

	/**
	 * Render a preview of the form in the given language
	 * @param formId form ID
	 * @param language language code
	 * @return rendered preview
	 * @throws Exception
	 */
	@GET
	@Path("{id}/preview/{language}")
	@Produces({ MediaType.TEXT_HTML })
	String preview(@PathParam("id") String formId, @PathParam("language") String language) throws Exception;

	/**
	 * Render a preview of the form in the given language
	 * @param formId form ID
	 * @param language language code
	 * @param formModel form model
	 * @return rendered preview
	 * @throws Exception
	 */
	@POST
	@Path("{id}/preview/{language}")
	@Produces({ MediaType.TEXT_HTML })
	String preview(@PathParam("id") String formId, @PathParam("language") String language, FormModel formModel) throws Exception;

	/**
	 * List data entries from Mesh, which have been submitted to the form
	 * @param formId form ID
	 * @param filter parameter bean
	 * @param paging parameter bean
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("{id}/data")
	Response listData(@PathParam("id") String formId, @BeanParam FilterFormDataParameterBean filter,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Load a single data entry from Mesh, which has been submitted to the form
	 * @param formId form ID
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("{id}/data/{dataId}")
	Response getData(@PathParam("id") String formId, @PathParam("dataId") String dataId) throws Exception;

	/**
	 * Permanently delete a single data entry from Mesh, which has been submitted to the form
	 * @param formId form ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("{id}/data/{dataId}")
	Response deleteData(@PathParam("id") String formId, @PathParam("dataId") String dataId) throws Exception;

	/**
	 * Download the binary data for a single field in a data entry
	 * @param formId form ID
	 * @param dataId data ID (uuid of the data node in Mesh)
	 * @param field field name
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("{id}/data/{dataId}/binary/{field}")
	@Produces({ "*/*" })
	Response downloadDataBinaryField(@PathParam("id") String formId, @PathParam("dataId") String dataId, @PathParam("field") String field) throws Exception;

	/**
	 * Export data from Mesh, which has been submitted to the form
	 *
	 * <p>
	 *     Note that this endpoint will download the export immediately, and
	 *     block for as long as it takes the Mesh plugin to create it. Use
	 *     {@link #requestExport(String, FormDataExportParameterBean, FilterFormDataParameterBean)} and
	 *     {@link #downloadFile(String, String)} for asynchronous downloads.
	 * </p>
	 *
	 * @param formId form ID
	 * @param exportFormat parameter bean
	 * @param filter filter bean
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("{id}/export")
	Response exportData(@PathParam("id") String formId, @BeanParam FormDataExportParameterBean exportFormat, @BeanParam FilterFormDataParameterBean filter) throws Exception;

	/**
	 * Check available export downloads for the given form.
	 *
	 * <p>
	 *     The response will contain whether there are export downloads ready
	 *     which match the given {@code exportFormat} and {@code filter}
	 *     (field {@code "downloadReady"}), and if so the downloads
	 *     UUID and creation timestamp (fields {@code "downloadUuid"} and
	 *     {@code "downloadTimestamp"}).
	 * </p>
	 *
	 * <p>
	 *     When a download is currently being prepared, the field
	 *     {@code "downloadPending"} will be {@code true}.
	 * </p>
	 *
	 * <p>
	 *     Any errors will be in the field {@code errors}.
	 * </p>
	 *
	 * @param formId The forms ID.
	 * @return The response from the Mesh forms plugin.
	 */
	@GET
	@Path("{id}/export/status")
	Response exportStatus(@PathParam("id") String formId, @BeanParam FormDataExportParameterBean exportFormat, @BeanParam FilterFormDataParameterBean filter) throws Exception;

	/**
	 * Request an asynchronous export download for the given forms data.
	 *
	 * <p>
	 *     The response will contain the UUID of the generated download node.
	 *     Whether the download is ready has to be checked via requests to
	 *     {@link #exportStatus(String, FormDataExportParameterBean, FilterFormDataParameterBean)}.
	 * </p>
	 *
	 * @param formId The forms ID.
	 * @return The response from the Mesh forms plugin.
	 */
	@POST
	@Path("{id}/export")
	Response requestExport(@PathParam("id") String formId, @BeanParam FormDataExportParameterBean exportFormat, @BeanParam FilterFormDataParameterBean filter) throws Exception;

	/**
	 * Download binary data from Mesh, which has been submitted to the form
	 *
	 * <p>
	 *     Note that this endpoint will download the binaries immediately, and
	 *     block for as long as it takes the Mesh plugin to create it. Use
	 *     {@link #requestDataBinaries(String)} and
	 *     {@link #downloadFile(String, String)} for asynchronous downloads.
	 * </p>
	 *
	 * @param formId form ID
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("{id}/binaries")
	@Produces({ "application/zip" })
	Response downloadDataBinaries(@PathParam("id") String formId) throws Exception;

	/**
	 * Check available binary downloads for the given form.
	 *
	 * <p>
	 *     The response will contain whether there are binary downloads ready
	 *     (field {@code "downloadReady"}), and if so the downloads
	 *     UUID and creation timestamp (fields {@code "downloadUuid"} and
	 *     {@code "downloadTimestamp"}).
	 * </p>
	 *
	 * <p>
	 *     When a download is currently being prepared, the field
	 *     {@code "downloadPending"} will be {@code true}.
	 * </p>
	 *
	 * <p>
	 *     Any errors will be in the field {@code errors}.
	 * </p>
	 *
	 * @param formId The forms ID.
	 * @return The response from the Mesh forms plugin.
	 */
	@GET
	@Path("{id}/binaries/status")
	Response dataBinariesStatus(@PathParam("id") String formId) throws Exception;

	/**
	 * Request an asynchronous binary download for the given forms data.
	 *
	 * <p>
	 *     The response will contain the UUID of the generated download node.
	 *     Whether the download is ready has to be checked via requests to
	 *     {@link #dataBinariesStatus(String)}.
	 * </p>
	 *
	 * @param formId The forms ID.
	 * @return The response from the Mesh forms plugin.
	 */
	@POST
	@Path("{id}/binaries")
	Response requestDataBinaries(@PathParam("id") String formId) throws Exception;

	/**
	 * Download the requested binary archive.
	 *
	 * <p>
	 *     The {@code downloadUuid} must be a valid UUID returned the the
	 *     {@link #requestDataBinaries(String)} method.
	 * </p>
	 *
	 * @param formId The forms ID.
	 * @param downloadUuid The UUID of the download node.
	 * @return The response from the Mesh forms plugin.
	 */
	@GET
	@Path("{id}/download/{downloadUuid}")
	Response downloadFile(@PathParam("id") String formId, @PathParam("downloadUuid") String downloadUuid) throws Exception;

	/**
	 * Get the total usage information of the given forms.
	 *
	 * @param formId list of form IDs
	 * @param nodeId ID of the node
	 * @return Total usage response
	 * @throws Exception
	 */
	@GET
	@Path("/usage/total")
	TotalUsageResponse getTotalUsage(@QueryParam("id") List<Integer> formId, @QueryParam("nodeId") Integer nodeId) throws Exception;

	/**
	 * Get the pages using one of the given forms
	 *
	 * @param skipCount   number of items to be skipped, set to 0 for skipping no
	 *                    items
	 * @param maxItems    maximum number of items to be returned, set to -1 for
	 *                    returning all items
	 * @param sortBy      (optional) attribute to sort by. defaults to name
	 * @param sortOrder   (optional) result sort order - may be "asc" for ascending
	 *                    or "desc" for descending other strings will be ignored.
	 *                    defaults to "asc".
	 * @param formId      list of form IDs, for which the usage shall be fetched
	 * @param nodeId      ID of the node
	 * @param returnPages true (default) if the pages shall be returned, false for
	 *                    only returning the counts
	 * @param pageModel   page model parameters
	 * @return list of pages using the given forms
	 * @throws Exception
	 */
	@GET
	@Path("/usage/page")
	PageUsageListResponse getPageUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> formId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel) throws Exception;

	/**
	 * Get the templates using one of the given forms
	 *
	 * @param skipCount       number of items to be skipped, set to 0 for skipping
	 *                        no items
	 * @param maxItems        maximum number of items to be returned, set to -1 for
	 *                        returning all items
	 * @param sortBy          (optional) attribute to sort by. defaults to name
	 * @param sortOrder       (optional) result sort order - may be "asc" for
	 *                        ascending or "desc" for descending other strings will
	 *                        be ignored. defaults to "asc".
	 * @param formId          list of form ids, for which the usage shall be fetched
	 * @param nodeId          id of the node
	 * @param returnTemplates true (default) if the templates shall be returned,
	 *                        false for only returning the counts
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("/usage/template")
	TemplateUsageListResponse getTemplateUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> formId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("templates") @DefaultValue("true") boolean returnTemplates) throws Exception;

	/**
	 * List the form type configurations
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @return response containing a list of form type configurations
	 * @HTTP 200 The list of form type configurations is returned.
	 * @throws Exception
	 */
	@GET
	@Path("/types")
	FormTypeConfigurationListResponse listFormTypeConfigurations(
			@BeanParam FilterFormTypeConfigurationsParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Get the form type configuration for the given type
	 * @param type type
	 * @return form type configuration
	 * @HTTP 200 The form type configuration is returned.
	 * @HTTP 404 The form type configuration is not found.
	 * @throws Exception
	 */
	@GET
	@Path("/types/{type}")
	FormTypeConfigurationResponse getFormTypeconfiguration(@PathParam("type") String type) throws Exception;

	/**
	 * Remove the form type configuration from the node.
	 * @param nodeId node ID (either global or local)
	 * @param type form type key
	 * @return generic response
	 * @HTTP 204 Form type configuration was removed from the node
	 * @HTTP 404 Either the node or the form type configuration was not found.
	 * @throws Exception
	 */
	@DELETE
	@Path("/nodes/{nodeId}/types/{type}")
	Response removeFormTypeConfigurationsFromNode(@PathParam("nodeId") String nodeId, @PathParam("type") String type) throws Exception;

	/**
	 * Add the form tyoe configuration to the node.
	 * @param nodeId node ID (either global or local)
	 * @param type form type key
	 * @return generic response
	 * @HTTP 201 Form type configuration was added to the node
	 * @HTTP 404 Either the node or the form type configuration was not found.
	 * @throws Exception
	 */
	@PUT
	@Path("/nodes/{nodeId}/types/{type}")
	Response addFormTypeConfigurationsToNode(@PathParam("nodeId") String nodeId, @PathParam("type") String type) throws Exception;

}
