package com.gentics.contentnode.rest.resource;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.ContentLanguage;
import com.gentics.contentnode.rest.model.NodeFeature;
import com.gentics.contentnode.rest.model.request.NodeCopyRequest;
import com.gentics.contentnode.rest.model.request.NodeFeatureRequest;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.FeatureList;
import com.gentics.contentnode.rest.model.response.FeatureModelList;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LanguageList;
import com.gentics.contentnode.rest.model.response.LanguageListResponse;
import com.gentics.contentnode.rest.model.response.NodeFeatureResponse;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.model.response.NodeSettingsResponse;
import com.gentics.contentnode.rest.model.response.PagedConstructListResponse;
import com.gentics.contentnode.rest.model.response.PagedObjectPropertyListResponse;
import com.gentics.contentnode.rest.model.response.PagedTemplateListResponse;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for handling Nodes in GCN
 */
@Path("node")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface NodeResource {
	/**
	 * Create a new node.
	 *
	 * See {@link NodeResourceImpl#checkCreateRequest} for a detailed list of
	 * constraints on the given request. In short:
	 * <ul>
	 *  <li>the node and hostname must be provided</li>
	 *  <li>no other node with the same hostname and
	 *      (binary) publish directory may exist</li>
	 *  <li>if the Aloha editor is to be used, utf8 must be enabled</li>
	 * </ul>
	 *
	 * @param request The request containing a REST model of
	 *		the node to be created.
	 *
	 * @return A node load response containing the values of
	 *		the newly created node on success, or a response
	 *		code indicating any errors on failure.
	 */
	@PUT
	@Path("/")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The node was created."),
		@ResponseCode(code = 400, condition = "The node could not be created due to insufficient data provided."),
		@ResponseCode(code = 409, condition = "The node could not be created due to a conflict with other nodes.")
	})
	NodeLoadResponse add(NodeSaveRequest request) throws Exception;

	/**
	 * Load a single node
	 * @param nodeId id of the node to load. This can be either the localid or a globalid
	 * @param update
	 *            true when the folder is fetched for updating. Currently, nodes
	 *            cannot be locked in the backend, but it is still recommended
	 *            to set this parameter to true when the node shall be modified.
	 * @return response containing the node to load
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The node was returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	NodeLoadResponse get(@PathParam("id") String nodeId,
			@DefaultValue("false") @QueryParam("update") boolean update) throws Exception;

	/**
	 * Saves the values specified in the request to the
	 * node.
	 * 
	 * See {@link NodeResourceImpl#checkRequestConsistency} for a detailed
	 * list of constraints on the request. In short:
	 * <ul>
	 *  <li>any folder id that is given must be valid
	 *  <li>no other node with the same hostname and
	 *      (binary) publish directory may exist</li>
	 *  <li>if the Aloha editor is to be used, utf8 must be enabled</li>
	 * </ul>
	 *
	 * @param id The id of the node to save.
	 * @param request The request containing the fields
	 *		to be updated in the node.
	 * @return A generic response indicating the success or
	 *		failure of the operation.
	 */
	@POST
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The node was updated."),
		@ResponseCode(code = 400, condition = "The node could not be updated due to insufficient data provided."),
		@ResponseCode(code = 404, condition = "The node was not found."),
		@ResponseCode(code = 409, condition = "The node could not be updated due to a conflict with other nodes.")
	})
	GenericResponse update(@PathParam("id") String nodeId, NodeSaveRequest request) throws Exception;

	/**
	 * Delete the given node
	 * @param nodeId id of the node
	 * @param waitMs wait timeout in milliseconds
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The node was deleted."),
		@ResponseCode(code = 404, condition = "The node was not found."),
		@ResponseCode(code = 409, condition = "The node could not be deleted due to a conflict with other nodes.")
	})
	GenericResponse delete(@PathParam("id") String nodeId, @QueryParam("wait") @DefaultValue("0") long waitMs) throws Exception;

	/**
	 * List nodes.
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms perms parameters
	 * @param stagingPackageName if given, check the node status against the staging package
	 * @return node list
	 * @throws Exception in case of errors
	 */
	@GET
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The node list was returned.")
	})
	NodeList list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms,
			@QueryParam("package") String stagingPackageName) throws Exception;

	/**
	 * Get list of languages in the node
	 * 
	 * @param nodeId
	 *            node id
	 * @param filter filter parameters
	 * @param paging paging parameters
	 * @return ordered list of languages
	 */
	@GET
	@Path("/{id}/languages")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Languages were returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	LanguageList languages(@PathParam("id") String nodeId, @BeanParam FilterParameterBean filter, @BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Get list of languages, which can be assigned to the node
	 * 
	 * @param nodeId
	 *            node id
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @return list of available languages
	 */
	@GET
	@Path("/{id}/availableLanguages")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Languages were returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	LanguageList availableLanguages(@PathParam("id") String nodeId, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Add language to node
	 * @param nodeId node id
	 * @param languageId language id or language code
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/languages/{languageId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Language was added to the node."),
		@ResponseCode(code = 404, condition = "The node or language was not found.")
	})
	GenericResponse addLanguage(@PathParam("id") String nodeId, @PathParam("languageId") String languageId) throws Exception;

	/**
	 * Remove language from node
	 * @param nodeId node id
	 * @param languageId language id or language code
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/languages/{languageId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Language was removed from the node."),
		@ResponseCode(code = 404, condition = "The node or language was not found."),
		@ResponseCode(code = 409, condition = "Language could not be removed due to a conflict with existing data.")
	})
	GenericResponse removeLanguage(@PathParam("id") String nodeId, @PathParam("languageId") String languageId) throws Exception;

	/**
	 * Set ordered list of languages
	 * @param nodeId node id
	 * @param languages ordered list of languages
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/languages")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Language were set."),
		@ResponseCode(code = 404, condition = "The node or a language was not found."),
		@ResponseCode(code = 409, condition = "Language could not be removed due to a conflict with existing data.")
	})
	LanguageList setLanguages(@PathParam("id") String nodeId, List<ContentLanguage> languages) throws Exception;

	/**
	 * Create a new node.
	 *
	 * See {@link NodeResourceImpl#checkCreateRequest} for a detailed list of
	 * constraints on the given request. In short:
	 * <ul>
	 *  <li>the node and hostname must be provided</li>
	 *  <li>no other node with the same hostname and
	 *      (binary) publish directory may exist</li>
	 *  <li>if the Aloha editor is to be used, utf8 must be enabled</li>
	 * </ul>
	 *
	 * @param request The request containing a REST model of
	 *		the node to be created.
	 *
	 * @return A node load response containing the values of
	 *		the newly created node on success, or a response
	 *		code indicating any errors on failure.
	 */
	@POST
	@Path("/create")
	@Deprecated
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The node was created."),
		@ResponseCode(code = 400, condition = "The node could not be created due to insufficient data provided."),
		@ResponseCode(code = 409, condition = "The node could not be created due to a conflict with other nodes.")
	})
	NodeLoadResponse create(NodeSaveRequest request) throws Exception;

	/**
	 * Saves the values specified in the request to the
	 * node.
	 * 
	 * See {@link NodeResourceImpl#checkRequestConsistency} for a detailed
	 * list of constraints on the request. In short:
	 * <ul>
	 *  <li>any folder id that is given must be valid
	 *  <li>no other node with the same hostname and
	 *      (binary) publish directory may exist</li>
	 *  <li>if the Aloha editor is to be used, utf8 must be enabled</li>
	 * </ul>
	 *
	 * @param id The id of the node to save.
	 * @param request The request containing the fields
	 *		to be updated in the node.
	 * @return A generic response indicating the success or
	 *		failure of the operation.
	 */
	@POST
	@Path("/save/{id}")
	@Deprecated
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The node was updated."),
		@ResponseCode(code = 400, condition = "The node could not be updated due to insufficient data provided."),
		@ResponseCode(code = 404, condition = "The node was not found."),
		@ResponseCode(code = 409, condition = "The node could not be updated due to a conflict with other nodes.")
	})
	GenericResponse save(@PathParam("id") String nodeId, NodeSaveRequest request) throws Exception;

	/**
	 * Load a single node
	 * @param nodeId id of the node to load. This can be either the localid or a globalid
	 * @param update
	 *            true when the folder is fetched for updating. Currently, nodes
	 *            cannot be locked in the backend, but it is still recommended
	 *            to set this parameter to true when the node shall be modified.
	 * @return response containing the node to load
	 */
	@GET
	@Path("/load/{id}")
	@Deprecated
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The node was returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	NodeLoadResponse load(@PathParam("id") String nodeId,
			@DefaultValue("false") @QueryParam("update") boolean update) throws Exception;

	/**
	 * Get the ordered list of languages of the node
	 * @param nodeId node id
	 * @return ordered list of languages
	 */
	@GET
	@Path("/getLanguages/{id}")
	@Deprecated
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Languages were returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	LanguageListResponse languages(@PathParam("id") String nodeId) throws Exception;

	/**
	 * Get list of features activated for the node
	 * @param nodeId node ID
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @return list of activated features
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/features")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Features were returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	FeatureList features(@PathParam("id") String nodeId, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Activate the feature for the node
	 * @param nodeId node ID
	 * @param feature feature to activate
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/features/{feature}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Features was activated."),
		@ResponseCode(code = 404, condition = "The node was not found."),
		@ResponseCode(code = 405, condition = "Requested feature is not generally activated.")
	})
	GenericResponse activateFeature(@PathParam("id") String nodeId, @PathParam("feature") NodeFeature feature) throws Exception;

	/**
	 * Deactivate the feature for the node
	 * @param nodeId node ID
	 * @param feature feature to activate
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/features/{feature}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Features was deactivated."),
		@ResponseCode(code = 404, condition = "The node was not found."),
		@ResponseCode(code = 405, condition = "Requested feature is not generally activated.")
	})
	GenericResponse deactivateFeature(@PathParam("id") String nodeId, @PathParam("feature") NodeFeature feature) throws Exception;

	/**
	 * Load the activated features
	 * @param nodeId id of the node
	 * @return response containing the activated features
	 * @throws Exception
	 */
	@GET
	@Path("/features/{id}")
	@Deprecated
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Features were returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	NodeFeatureResponse getFeatures(@PathParam("id") String nodeId) throws Exception;

	/**
	 * Activate the given list of features (features not listed will not be changed)
	 * @param nodeId id of the node. This can be either the localid or the globalid
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/features/activate/{id}")
	@Deprecated
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Features were activated."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	GenericResponse activateFeatures(@PathParam("id") String nodeId, NodeFeatureRequest request) throws Exception;

	/**
	 * Deactivate the given list of features (features not listed will not be changed)
	 * @param nodeId id of the node. This can be either the localid or the globalid
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/features/deactivate/{id}")
	@Deprecated
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Features were deactivated."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	GenericResponse deactivateFeatures(@PathParam("id") String nodeId, NodeFeatureRequest request) throws Exception;

	/**
	 * Set the given list of features. Exactly the listed features will be activated, all other will be deactivated
	 * @param nodeId id of the node. This can be either the localid or the globalid
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/features/set/{id}")
	@Deprecated
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Features were set."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	GenericResponse setFeatures(@PathParam("id") String nodeId, NodeFeatureRequest request) throws Exception;

	/**
	 * Get the templates assigned to this node
	 * @param nodeId Node ID (local or global)
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return Response containing the templates assigned to this node
	 */
	@GET
	@Path("/{nodeId}/templates")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Templates were returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	PagedTemplateListResponse getTemplates(@PathParam("nodeId") String nodeId, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Get the template, if it is assigned to the node
	 * @param nodeId Node ID (local or global)
	 * @param templateId Template ID (local or global)
	 * @return template load response
	 * @throws Exception
	 */
	@GET
	@Path("/{nodeId}/templates/{templateId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Template was found in the node."),
		@ResponseCode(code = 404, condition = "Either the node was not found, or the template is not assigned to the node.")
	})
	TemplateLoadResponse getTemplate(@PathParam("nodeId") String nodeId, @PathParam("templateId") String templateId) throws Exception;

	/**
	 * Add a template to a node. This will just assign the template to the node, but not link it to any folders.
	 * @param nodeId Node ID (local or global)
	 * @param templateId Template ID (local or global)
	 * @return Generic response
	 * @throws Exception
	 */
	@PUT
	@Path("/{nodeId}/templates/{templateId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Template was added."),
		@ResponseCode(code = 404, condition = "The node or template was not found.")
	})
	GenericResponse addTemplate(@PathParam("nodeId") String nodeId, @PathParam("templateId") String templateId) throws Exception;

	/**
	 * Remove a template from a node. This will also unlink the template from all folders of the node
	 * @param nodeId Node ID (local or global)
	 * @param templateId Template ID (local or global)
	 * @return Generic response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{nodeId}/templates/{templateId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Template was removed."),
		@ResponseCode(code = 404, condition = "The node or template was not found.")
	})
	GenericResponse removeTemplate(@PathParam("nodeId") String nodeId, @PathParam("templateId") String templateId) throws Exception;

	/**
	 * Get the constructs assigned to this node
	 * @param nodeId Node ID (local or global)
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return Response containing the constructs assigned to this node
	 * @throws Exception
	 */
	@GET
	@Path("/{nodeId}/constructs")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Constructs were returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	PagedConstructListResponse getConstructs(@PathParam("nodeId") String nodeId, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Add a construct to a node.
	 * @param nodeId Node ID (local or global)
	 * @param constructId Construct ID (local or global)
	 * @return Generic response
	 * @throws Exception
	 */
	@PUT
	@Path("/{nodeId}/constructs/{constructId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Construct was added."),
		@ResponseCode(code = 404, condition = "The node or construct was not found.")
	})
	GenericResponse addConstruct(@PathParam("nodeId") String nodeId, @PathParam("constructId") String constructId) throws Exception;

	/**
	 * Remove a construct from a node.
	 * @param nodeId Node ID (local or global)
	 * @param constructId Construct ID (local or global)
	 * @return Generic response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{nodeId}/constructs/{constructId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Construct was removed."),
		@ResponseCode(code = 404, condition = "The node or construct was not found.")
	})
	GenericResponse removeConstruct(@PathParam("nodeId") String nodeId, @PathParam("constructId") String constructId) throws Exception;

	/**
	 * Get the object properties assigned to this node
	 * @param nodeId Node ID (local or global)
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return Response containing the object properties assigned to this node
	 * @throws Exception
	 */
	@GET
	@Path("/{nodeId}/objectproperties")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Object properties were returned."),
		@ResponseCode(code = 404, condition = "The node was not found.")
	})
	PagedObjectPropertyListResponse getObjectProperties(@PathParam("nodeId") String nodeId, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Add an object property to a node.
	 * @param nodeId Node ID (local or global)
	 * @param objectPropertyId Property ID
	 * @return Generic response
	 * @throws Exception
	 */
	@PUT
	@Path("/{nodeId}/objectproperties/{objectPropertyId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Object property was added."),
		@ResponseCode(code = 404, condition = "The node or object property was not found.")
	})
	GenericResponse addObjectProperty(@PathParam("nodeId") String nodeId, @PathParam("objectPropertyId") String opId) throws Exception;

	/**
	 * Remove an object property from a node.
	 * @param nodeId Node ID (local or global)
	 * @param opId Construct ID (local or global)
	 * @return Generic response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{nodeId}/objectproperties/{objectPropertyId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Object property was removed."),
		@ResponseCode(code = 404, condition = "The node or object property was not found.")
	})
	GenericResponse removeObjectProperty(@PathParam("nodeId") String nodeId, @PathParam("objectPropertyId") String opId) throws Exception;

	/**
	 * Load settings specific to the specified node.
	 *
	 * To get the settings in <code>$NODE_SETTINGS_GLOBAL</code> are loaded and then the values from <code>$NODE_SETTINGS[nodeId]</code>
	 * are added.
	 *
	 * @param nodeId Node ID (local or global)
	 * @return The conigured settings for this node in JSON format
	 * @throws Exception On errors
	 */
	@GET
	@Path("/{nodeId}/settings")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Node settings were returned.")
	})
	NodeSettingsResponse settings(@PathParam("nodeId") String nodeId) throws Exception;

	/**
	 * Get list of available node features
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @return list response
	 * @throws Exception
	 */
	@GET
	@Path("/features")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Available features were returned.")
	})
	FeatureModelList availableFeatures(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Copy the given node.
	 * @param nodeId node ID
	 * @param waitMs wait timeout in milliseconds
	 * @param request copy request
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{nodeId}/copy")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Node was copied.")
	})
	GenericResponse copy(@PathParam("nodeId") String nodeId, @QueryParam("wait") @DefaultValue("0") long waitMs, NodeCopyRequest request) throws Exception;
}
