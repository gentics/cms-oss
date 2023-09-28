package com.gentics.contentnode.rest.resource.devtools;

import com.gentics.contentnode.rest.resource.parameter.FilterPackageCheckBean;
import java.util.List;

import javax.ws.rs.BeanParam;
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

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;

import com.gentics.contentnode.rest.model.devtools.AutocompleteItem;
import com.gentics.contentnode.rest.model.devtools.Package;
import com.gentics.contentnode.rest.model.devtools.PackageListResponse;
import com.gentics.contentnode.rest.model.devtools.SyncInfo;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.model.response.ContentRepositoryFragmentResponse;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.model.response.DatasourceLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyLoadResponse;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedConstructInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedContentRepositoryFragmentInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedContentRepositoryInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedDatasourceInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedObjectPropertyInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedTemplateInPackageListResponse;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

@Path("/devtools")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions on the devtools."),
	@ResponseCode(code = 405, condition = "Feature devtools is not activated.")
})
public interface PackageResource {
	/**
	 * List available packages
	 *
	 * @param filter  filter parameters
	 * @param sorting sorting parameters
	 * @param paging  paging parameters
	 * @return List of packages
	 * @throws Exception
	 */
	@GET
	@Path("/packages")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Package list is returned.")
	})
	PackageListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging)
			throws Exception;

	/**
	 * Get the package with given name
	 * @param name Package name
	 * @return package
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Package {name} exists."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	Package get(@PathParam("name") String name) throws Exception;

	/**
	 * Add the package with given name
	 * @param name Package name
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/packages/{name}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Package {name} was created."),
		@ResponseCode(code = 409, condition = "Package {name} already exists.")
	})
	Response add(@PathParam("name") String name) throws Exception;

	/**
	 * Delete the package with given name
	 * @param name Package name
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/packages/{name}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Package {name} was deleted."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	Response delete(@PathParam("name") String name) throws Exception;


	/**
	 * Checks the package for completeness
	 * @param name name of the package to check
	 * @param checkAll if true, other packages are also searched
	 * @param waitMs wait timeout in ms. When set to 0, response will be sent, when the action completes
	 * @param filter filter options. The dependency type can be filtered (e.g.: type=CONSTRUCT).
	 *                Additionally, missing references can be filtered (e.g.: filter=INCOMPLETE)
	 * @param paging the paging parameter
	 * @return list of dependencies
	 * @throws Exception
	 */
	@GET
	@Path("/package/{name}/check")
	GenericResponse performPackageConsistencyCheck(
			@PathParam("name") String name,
			@QueryParam("checkAll")  @DefaultValue("false")  boolean checkAll,
			@QueryParam("wait") @DefaultValue("0") long waitMs,
			@BeanParam FilterPackageCheckBean filter,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Get the result from the package consistency check if available
	 * @param packageName name of the package
	 * @return the result of the check
	 * @throws Exception
	 */
	@GET
	@Path("/package/{name}/check/result")
	Response obtainPackageConsistencyCheckResult(@PathParam("name") String packageName) throws Exception;


	/**
	 * Trigger synchronization of all objects in the given package to the filesystem
	 * @param name name of the package
	 * @param waitMs wait timeout in ms. When set to 0, response will be sent, when the action completes
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/packages/{name}/cms2fs")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Synchronization succeeded within timeout, or was sent to the background."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	GenericResponse synchronizeToFS(@PathParam("name") String name, @QueryParam("wait") @DefaultValue("0") long waitMs) throws Exception;

	/**
	 * Trigger synchronization of all objects in the given package to the cms
	 * @param name name of the package
	 * @param waitMs wait timeout in ms. When set to 0, response will be sent, when the action completes
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/packages/{name}/fs2cms")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Synchronization succeeded within timeout, or was sent to the background."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	GenericResponse synchronizeFromFS(@PathParam("name") String name, @QueryParam("wait") @DefaultValue("0") long waitMs) throws Exception;

	/**
	 * Get the list of constructs assigned to the package
	 * @param name Package name
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @param embed optionally embed the referenced objects (category)
	 * @return List of constructs
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/constructs")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of constructs is returned."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	PagedConstructInPackageListResponse listConstructs(@PathParam("name") String name, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms, @BeanParam EmbedParameterBean embed) throws Exception;

	/**
	 * Get the construct from the package
	 * @param name Package name
	 * @param construct construct ID (internal or global) or keyword
	 * @return construct response
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/constructs/{construct}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Construct is returned for package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {construct}.")
	})
	ConstructLoadResponse getConstruct(@PathParam("name") String name, @PathParam("construct") String construct) throws Exception;

	/**
	 * Add a construct to a package
	 * @param name Package name
	 * @param construct construct ID (internal or global) or keyword
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/packages/{name}/constructs/{construct}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "The construct was added to the package."),
		@ResponseCode(code = 404, condition = "Either the package {name} or the {construct} does not exist."),
		@ResponseCode(code = 409, condition = "{construct} is already part of the package {name}.")
	})
	Response addConstruct(@PathParam("name") String name, @PathParam("construct") String construct)
			throws Exception;

	/**
	 * Remove a construct from a package
	 * @param name Package name
	 * @param construct construct ID (internal or global) or keyword
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/packages/{name}/constructs/{construct}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "The construct was removed from the package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {construct}."),
		@ResponseCode(code = 409, condition = "{construct} is contained in a subpackage and cannot be removed.")
	})
	Response removeConstruct(@PathParam("name") String name, @PathParam("construct") String construct) throws Exception;

	/**
	 * Get the templates of a package
	 * @param name Package name
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return List of templates
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/templates")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of templates is returned."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	PagedTemplateInPackageListResponse listTemplates(@PathParam("name") String name, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Get the template in the package
	 * @param name Package name
	 * @param template global template ID (uuid)
	 * @return template response
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/templates/{template}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Template is returned for package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {template}.")
	})
	TemplateLoadResponse getTemplate(@PathParam("name") String name, @PathParam("template") String template) throws Exception;

	/**
	 * Add a template to a package
	 * @param name Package name
	 * @param template global template ID (uuid)
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/packages/{name}/templates/{template}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "The template was added to the package."),
		@ResponseCode(code = 404, condition = "Either the package {name} or the {template} does not exist."),
		@ResponseCode(code = 409, condition = "{template} is already part of the package {name}.")
	})
	Response addTemplate(@PathParam("name") String name, @PathParam("template") String template) throws Exception;

	/**
	 * Remove a template from a package
	 * @param name Package name
	 * @param template global template ID (uuid)
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/packages/{name}/templates/{template}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "The template was removed from the package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {template}."),
		@ResponseCode(code = 409, condition = "{template} is contained in a subpackage and cannot be removed.")
	})
	Response removeTemplate(@PathParam("name") String name, @PathParam("template") String template) throws Exception;

	/**
	 * Get the datasources in a package
	 * @param name Package name
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return List of datasources
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/datasources")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of datasources is returned."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	PagedDatasourceInPackageListResponse listDatasources(@PathParam("name") String name, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Get the datasource in a package
	 * @param name Package name
	 * @param datasource datasource name
	 * @return datasource response
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/datasources/{datasource}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Datasource is returned for package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {datasource}.")
	})
	DatasourceLoadResponse getDatasource(@PathParam("name") String name, @PathParam("datasource") String datasource) throws Exception;

	/**
	 * Add a datasource to a package
	 * @param name Package name
	 * @param datasource datasource name
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/packages/{name}/datasources/{datasource}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "The datasource was added to the package."),
		@ResponseCode(code = 404, condition = "Either the package {name} or the {datasource} does not exist."),
		@ResponseCode(code = 409, condition = "{datasource} is already part of the package {name}.")
	})
	Response addDatasource(@PathParam("name") String name, @PathParam("datasource") String datasource) throws Exception;

	/**
	 * Remove a datasource from a package
	 * @param name Package name
	 * @param datasource datasource name
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/packages/{name}/datasources/{datasource}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "The datasource was removed from the package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {datasource}."),
		@ResponseCode(code = 409, condition = "{datasource} is contained in a subpackage and cannot be removed.")
	})
	Response removeDatasource(@PathParam("name") String name, @PathParam("datasource") String datasource) throws Exception;

	/**
	 * Get the object properties in a package
	 * @param name Package name
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param embed embed parameters
	 * @param perms permissions parameters
	 * @return List of object properties
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/objectproperties")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of object properties is returned."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	PagedObjectPropertyInPackageListResponse listObjectProperties(@PathParam("name") String name, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed,
			@BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Get the object property in a package
	 * @param name Package name
	 * @param objectproperty object property keyword
	 * @return object property response
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/objectproperties/{objectproperty}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Object property is returned for package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {objectproperty}.")
	})
	ObjectPropertyLoadResponse getObjectProperty(@PathParam("name") String name, @PathParam("objectproperty") String objectproperty) throws Exception;

	/**
	 * Add an object property to a package
	 * @param name Package name
	 * @param objectproperty object property keyword
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/packages/{name}/objectproperties/{objectproperty}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "The object property was added to the package."),
		@ResponseCode(code = 404, condition = "Either the package {name} or the {objectproperty} does not exist."),
		@ResponseCode(code = 409, condition = "{objectproperty} is already part of the package {name}.")
	})
	Response addObjectProperty(@PathParam("name") String name, @PathParam("objectproperty") String objectproperty) throws Exception;

	/**
	 * Remove an object property from a package
	 * @param name Package name
	 * @param objectproperty object property keyword
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/packages/{name}/objectproperties/{objectproperty}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "The object property was removed from the package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {objectproperty}."),
		@ResponseCode(code = 409, condition = "{objectproperty} is contained in a subpackage and cannot be removed.")
	})
	Response removeObjectProperty(@PathParam("name") String name, @PathParam("objectproperty") String objectproperty) throws Exception;

	/**
	 * Get the ContentRepository Fragments in a package
	 * @param name Package name
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param embed embed parameters
	 * @param perms permissions parameters
	 * @return List of ContentRepository Fragments
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/cr_fragments")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of ContentRepository Fragments is returned."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	PagedContentRepositoryFragmentInPackageListResponse listCrFragments(@PathParam("name") String name, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed,
			@BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Get the ContentRepository Fragment in a package
	 * @param name Package name
	 * @param crFragment ContentRepository Fragment name, local ID or global ID
	 * @return ContentRepository Fragment response
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/cr_fragments/{cr_fragment}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository Fragment is returned for package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {cr_fragment}.")
	})
	ContentRepositoryFragmentResponse getCrFragment(@PathParam("name") String name, @PathParam("cr_fragment") String crFragment) throws Exception;

	/**
	 * Add a ContentRepository Fragment to a package
	 * @param name Package name
	 * @param crFragment ContentRepository Fragment name, local ID or global ID
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/packages/{name}/cr_fragments/{cr_fragment}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "The ContentRepository Fragment was added to the package."),
		@ResponseCode(code = 404, condition = "Either the package {name} or the {cr_fragment} does not exist."),
		@ResponseCode(code = 409, condition = "{cr_fragment} is already part of the package {name}.")
	})
	Response addCrFragment(@PathParam("name") String name, @PathParam("cr_fragment") String crFragment) throws Exception;

	/**
	 * Remove a ContentRepository Fragment from a package
	 * @param name Package name
	 * @param crFragment ContentRepository Fragment name, local ID or global ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/packages/{name}/cr_fragments/{cr_fragment}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "The ContentRepository Fragment was removed from the package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {cr_fragment}."),
		@ResponseCode(code = 409, condition = "{cr_fragment} is contained in a subpackage and cannot be removed.")
	})
	Response removeCrFragment(@PathParam("name") String name, @PathParam("cr_fragment") String crFragment) throws Exception;

	/**
	 * Get the ContentRepositories in a package
	 * @param name Package name
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param embed embed parameters
	 * @param perms permissions parameters
	 * @return List of ContentRepositories
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/contentrepositories")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of ContentRepositories is returned."),
		@ResponseCode(code = 404, condition = "Package {name} does not exist.")
	})
	PagedContentRepositoryInPackageListResponse listContentRepositories(@PathParam("name") String name, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed,
			@BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Get the ContentRepository in a package
	 * @param name Package name
	 * @param contentrepository ContentRepository name, local ID or global ID
	 * @return ContentRepository response
	 * @throws Exception
	 */
	@GET
	@Path("/packages/{name}/contentrepositories/{contentrepository}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "ContentRepository is returned for package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {contentrepository}.")
	})
	ContentRepositoryResponse getContentRepository(@PathParam("name") String name, @PathParam("contentrepository") String contentrepository) throws Exception;

	/**
	 * Add a ContentRepository to a package
	 * @param name Package name
	 * @param contentrepository ContentRepository name, local ID or global ID
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/packages/{name}/contentrepositories/{contentrepository}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "The ContentRepository was added to the package."),
		@ResponseCode(code = 404, condition = "Either the package {name} or the {contentrepository} does not exist."),
		@ResponseCode(code = 409, condition = "{contentrepository} is already part of the package {name}.")
	})
	Response addContentRepository(@PathParam("name") String name, @PathParam("contentrepository") String contentrepository) throws Exception;

	/**
	 * Remove a ContentRepository from a package
	 * @param name Package name
	 * @param contentrepository ContentRepository name, local ID or global ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/packages/{name}/contentrepositories/{contentrepository}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "The ContentRepository was removed from the package."),
		@ResponseCode(code = 404, condition = "Either package {name} does not exist, or it does not contain {contentrepository}."),
		@ResponseCode(code = 409, condition = "{contentrepository} is contained in a subpackage and cannot be removed.")
	})
	Response removeContentRepository(@PathParam("name") String name, @PathParam("contentrepository") String contentrepository) throws Exception;

	/**
	 * List packages assigned to a node. It is not possible to get packages to channels
	 * @param nodeId Node ID (either global or local)
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @return List of packages in the node
	 * @throws Exception
	 */
	@GET
	@Path("/nodes/{nodeId}/packages")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "List of packages is returned."),
		@ResponseCode(code = 404, condition = "Node {nodeId} was not found.")
	})
	PackageListResponse listNodePackages(@PathParam("nodeId") String nodeId, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Remove the package from the node. It is not possible to remove packages from channels
	 * @param nodeId node ID (either global or local)
	 * @param packageName Package name
	 * @return generic response
	 * @throws Exception
	 */
	@DELETE
	@Path("/nodes/{nodeId}/packages/{packageName}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Package was removed from the node."),
		@ResponseCode(code = 404, condition = "Node {nodeId} was not found.")
	})
	Response removePackage(@PathParam("nodeId") String nodeId, @PathParam("packageName") String packageName) throws Exception;

	/**
	 * Add the package to the node. It is not possible to add packages to channels
	 * @param nodeId node ID (either global or local)
	 * @param packageName Package name
	 * @return generic response
	 * @throws Exception
	 */
	@PUT
	@Path("/nodes/{nodeId}/packages/{packageName}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Package was added to the node."),
		@ResponseCode(code = 404, condition = "Either node {nodeId} or package {packageName} does not exist.")
	})
	Response addPackage(@PathParam("nodeId") String nodeId, @PathParam("packageName") String packageName) throws Exception;

	/**
	 * Add the package to the node. It is not possible to add packages to channels
	 * @param nodeId node ID (either global or local)
	 * @param addedPackage Package to add
	 * @return generic response
	 * @throws Exception
	 */
	@PUT
	@Path("/nodes/{nodeId}/packages")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Package was added to the node."),
		@ResponseCode(code = 404, condition = "Either node {nodeId} or the package does not exist.")
	})
	Response addPackage(@PathParam("nodeId") String nodeId, Package addedPackage) throws Exception;

	/**
	 * Get self refreshing live preview of the given page
	 * @param id page ID
	 * @param nodeId node ID
	 * @return HTML page that contains the live preview of the page was iframe.
	 * @throws Exception
	 */
	@GET
	@Path("/preview/page/{id}")
	@Produces(MediaType.TEXT_HTML)
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Page preview is returned.")
	})
	String renderPage(@PathParam("id") String id, @QueryParam("nodeId") String nodeId) throws Exception;

	/**
	 * Listen on changes on dependencies with the given UUID
	 * @param uuid UUID of the registration
	 * @return Events that are emitted, when a dependency changes.
	 * @throws Exception
	 */
	@GET
	@Path("/listen/{uuid}")
	@Produces(SseFeature.SERVER_SENT_EVENTS)
	EventOutput listenPageChange(@PathParam("uuid") String uuid) throws Exception;

	/**
	 * Remove the listener with given UUID
	 * @param uuid UUID of the registration
	 * @return empty response
	 * @throws Exception
	 */
	@POST
	@Path("/stoplisten/{uuid}")
	Response removeListener(@PathParam("uuid") String uuid) throws Exception;

	/**
	 * Render the live preview of the page, which was registered before
	 * @param uuid UUID of the registration
	 * @return Rendered page
	 * @throws Exception
	 */
	@GET
	@Path("/preview/{uuid}")
	@Produces(MediaType.TEXT_HTML)
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Page preview is returned.")
	})
	String preview(@PathParam("uuid") String uuid) throws Exception;

	/**
	 * Endpoint for autocomplete UI components for constructs. Returns at most 10 constructs matching the given term
	 * @param term term to match
	 * @return list of at most 10 constructs
	 * @throws Exception
	 */
	@GET
	@Path("/autocomplete/constructs")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Autocomplete list is returned.")
	})
	List<AutocompleteItem> autocompleteConstructs(@QueryParam("term") String term) throws Exception;

	/**
	 * Endpoint for autocomplete UI components for templates. Returns at most 10 templates matching the given term
	 * @param term term to match
	 * @return list of at most 10 templates
	 * @throws Exception
	 */
	@GET
	@Path("/autocomplete/templates")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Autocomplete list is returned.")
	})
	List<AutocompleteItem> autocompleteTemplates(@QueryParam("term") String term) throws Exception;

	/**
	 * Endpoint for autocomplete UI components for datasources. Returns at most 10 datasources matching the given term
	 * @param term term to match
	 * @return list of at most 10 datasources
	 * @throws Exception
	 */
	@GET
	@Path("/autocomplete/datasources")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Autocomplete list is returned.")
	})
	List<AutocompleteItem> autocompleteDatasources(@QueryParam("term") String term) throws Exception;

	/**
	 * Endpoint for autocomplete UI components for object properties. Returns at most 10 object properties matching the given term
	 * @param term term to match
	 * @return list of at most 10 object properties
	 * @throws Exception
	 */
	@GET
	@Path("/autocomplete/objectproperties")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Autocomplete list is returned.")
	})
	List<AutocompleteItem> autocompleteObjectProperties(@QueryParam("term") String term) throws Exception;

	/**
	 * Endpoint for autocomplete UI components for cr fragments. Returns at most 10 cr fragments matching the given term
	 * @param term term to match
	 * @return list of at most 10 cr fragments
	 * @throws Exception
	 */
	@GET
	@Path("/autocomplete/cr_fragments")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Autocomplete list is returned.")
	})
	List<AutocompleteItem> autocompleteCrFragments(@QueryParam("term") String term) throws Exception;

	/**
	 * Endpoint for autocomplete UI components for contentrepositories. Returns at most 10 contentrepositories matching the given term
	 * @param term term to match
	 * @return list of at most 10 contentrepositories
	 * @throws Exception
	 */
	@GET
	@Path("/autocomplete/contentrepositories")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Autocomplete list is returned.")
	})
	List<AutocompleteItem> autocompleteContentRepositories(@QueryParam("term") String term) throws Exception;

	/**
	 * Get the current status information for the automatic synchronization.
	 * @return Sync information
	 * @throws Exception
	 */
	@GET
	@Path("/sync")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Autosync status is returned.")
	})
	SyncInfo getSyncInfo() throws Exception;

	/**
	 * Start the sync for the current user (if not started before)
	 * @return sync info
	 * @throws Exception
	 */
	@PUT
	@Path("/sync")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Autosync was enabled."),
		@ResponseCode(code = 409, condition = "Autosync already was enabled.")
	})
	SyncInfo startSync() throws Exception;

	/**
	 * Stop the sync, if it was started by the current user
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/sync")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Autosync was disabled."),
		@ResponseCode(code = 409, condition = "Autosync already was disabled.")
	})
	Response stopSync() throws Exception;
}
