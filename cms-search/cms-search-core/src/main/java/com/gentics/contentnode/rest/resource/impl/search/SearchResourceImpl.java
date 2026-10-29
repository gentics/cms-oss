package com.gentics.contentnode.rest.resource.impl.search;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Providers;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.search.IndexType;
import com.gentics.contentnode.object.search.Indexer;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.StagingStatus;
import com.gentics.contentnode.rest.resource.impl.AbstractContentNodeResource;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.SearchParameterBean;
import com.gentics.contentnode.rest.resource.search.SearchResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.staging.StagingUtil;

import io.vertx.core.json.JsonObject;

/**
 * SearchResource implementation
 */
@Produces({ MediaType.APPLICATION_JSON })
@Path("/elastic")
@Authenticated
@RequiredFeature(Feature.ELASTICSEARCH)
public class SearchResourceImpl extends AbstractContentNodeResource implements SearchResource {
	/**
	 * Name of the attribute containing the objects in the search result
	 */
	protected final static String OBJECT_ATTRIBUTE = "_object";

	@Context
	protected Providers providers;

	@Override
	@POST
	@Path("/{searchType}/_search")
	public Response searchType(@PathParam("searchType") String type, @BeanParam SearchParameterBean search, String body) throws NodeException {
		try {
			IndexType indexType = IndexType.valueOf(type);
			return doSearch("_search", search, body, Arrays.asList(indexType));
		} catch (IllegalArgumentException e) {
			try (Trx trx = ContentNodeHelper.trx()) {
				throw new RestMappedException(I18NHelper.get("search.invalid.type", type)).setMessageType(Type.CRITICAL)
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}
		}
	}

	@Override
	@POST
	@Path("{path: .*}")
	public Response search(@PathParam("path") String path, @BeanParam SearchParameterBean search, String body) throws NodeException {
		return doSearch(path, search, body, Arrays.asList(IndexType.values()));
	}

	/**
	 * Perform the search
	 * @param path search path
	 * @param search parameter bean
	 * @param body body
	 * @param types types for restricting the search
	 * @return response
	 * @throws NodeException
	 */
	protected Response doSearch(String path, SearchParameterBean search, String body, List<IndexType> types) throws NodeException {
		Wastebin wb = Wastebin.EXCLUDE;
		switch (search.wastebinSearch) {
		case include:
		case only:
			wb = Wastebin.INCLUDE;
			break;
		default:
			wb = Wastebin.EXCLUDE;
			break;
		}

		try (Trx trx = ContentNodeHelper.trx();
				ChannelTrx cTrx = new ChannelTrx(search.nodeId);
				WastebinFilter wbf = new WastebinFilter(wb)) {
			Transaction t = trx.getTransaction();
			// construct the elasticsearch URL
			StringBuilder url = new StringBuilder(Indexer.getMultiIndexUrl(search.languages, types));
			url.append("/").append(path);

			// create the method
			PostMethod post = new PostMethod(url.toString());
			String query = null;
			if (body != null) {
				query = addFilters(body, search);
				post.setRequestEntity(new StringRequestEntity(query, "application/json", "utf8"));
			}

			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Forwarding request to %s: %s", post.getURI(), query));
			}

			// perform the search request
			HttpClient httpClient = Indexer.getClient();
			int responseCode = httpClient.executeMethod(post);

			if (responseCode != 200) {
				return Response.status(responseCode).entity(post.getResponseBodyAsString()).build();
			} else {
				JSONObject response = new JSONObject(post.getResponseBodyAsString());
				JSONArray hitsArray = response.getJSONObject("hits").getJSONArray("hits");
				ObjectMapper mapper = providers.getContextResolver(ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE).getContext(null);

				Collection<Reference> fillRefs = new ArrayList<>();
				if (search.template) {
					fillRefs.add(Reference.TEMPLATE);
				}
				if (search.folder) {
					fillRefs.add(Reference.FOLDER);
				}
				if (search.languageVariants) {
					fillRefs.add(Reference.LANGUAGEVARIANTS);
				}
				if (search.translationStatus) {
					fillRefs.add(Reference.TRANSLATIONSTATUS);
				}
				if (search.contentTags) {
					fillRefs.add(Reference.CONTENT_TAGS);
				}
				if (search.objectTags) {
					fillRefs.add(Reference.TAGS);
					fillRefs.add(Reference.OBJECT_TAGS_VISIBLE);
				}
				if (search.addPrivileges) {
					fillRefs.add(Reference.PRIVILEGES);
				}
				if (search.privilegeMap) {
					fillRefs.add(Reference.PRIVILEGEMAP);
				}
				if (fillRefs.isEmpty()) {
					fillRefs = null;
				}

				JSONArray filteredArray = new JSONArray();
				Map<String, List<String>> stagingCheckParams = StringUtils.isNotBlank(search.stagingPackageName) ? new HashMap<>() : null;
				for (int i = 0; i < hitsArray.length(); i++) {
					JSONObject hit = hitsArray.getJSONObject(i);
					String type = hit.getString("_type");
					String id = hit.getString("_id");

					Class<? extends NodeObject> clazz = null;

					switch (type) {
					case "page":
						clazz = Page.class;
						break;
					case "folder":
						clazz = Folder.class;
						break;
					case "image":
						clazz = ImageFile.class;
						break;
					case "file":
						clazz = File.class;
						break;
					case "form":
						clazz = Form.class;
						break;
					}

					if (clazz != null) {
						// filter out the hits of objects the user is not allowed to see
						NodeObject object = t.getObject(clazz, id);
						if (object != null && t.canView(object)) {
							// add object to hit
							switch (type) {
							case "page":
								hit.put(OBJECT_ATTRIBUTE, new JSONObject(mapper.writeValueAsString(ModelBuilder.getPage((Page) object, fillRefs))));
								break;
							case "folder":
								hit.put(OBJECT_ATTRIBUTE, new JSONObject(
										mapper.writeValueAsString(ModelBuilder.getFolder((Folder) object, null, fillRefs))));
								break;
							case "image":
								hit.put(OBJECT_ATTRIBUTE, new JSONObject(mapper.writeValueAsString(ModelBuilder.getImage((ImageFile) object, fillRefs))));
								break;
							case "file":
								hit.put(OBJECT_ATTRIBUTE, new JSONObject(mapper.writeValueAsString(ModelBuilder.getFile((File) object, fillRefs))));
								break;
							case "form":
								hit.put(OBJECT_ATTRIBUTE, new JSONObject(mapper.writeValueAsString(FormResourceImpl.TRANSFORM2REST.apply((Form)object))));
								break;
							}

							filteredArray.put(hit);
							if (stagingCheckParams != null) {
								List<String> uuids = stagingCheckParams.get(type);
								if (uuids == null) {
									uuids = new ArrayList<>();
									stagingCheckParams.put(type, uuids);
								}
								uuids.add(object.getGlobalId().toString());
							}
						} else {
							Indexer.logger.warn(String.format("%s was filtered out from search result due to insufficient privileges", object));
						}
					}
				}
				response.getJSONObject("hits").put("hits", filteredArray);

				Map<String, StagingStatus> stagingResponse = StagingUtil.checkStagingStatus(search.stagingPackageName, stagingCheckParams, search.languageVariants);
				if (stagingResponse != null) {
					response.getJSONObject("hits").put("staging", new JsonObject().put(search.stagingPackageName, new JsonObject(mapper.writeValueAsString(stagingResponse))));
				}
				return Response.status(responseCode).entity(response.toString()).build();
			}
		} catch (NodeException e) {
			throw e;
		} catch (UnknownHostException | SocketException e) {
			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug("Elasticsearch unavailable: " + e.getMessage(), e);
			}

			throw new RestMappedException("Elasticsearch unavailable", "search.unavailable")
				.setMessageType(Type.CRITICAL)
				.setResponseCode(ResponseCode.FAILURE)
				.setStatus(Status.BAD_GATEWAY);
		} catch (IOException | JSONException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * If either nodeId or folderId's are given, add filder terms to the search query contained in body
	 * @param body body, which should contain the search query
	 * @param searchParam search parameter bean
	 * @return search query, which possible was modified
	 * @throws IOException
	 * @throws NodeException
	 */
	protected String addFilters(String body, SearchParameterBean searchParam) throws IOException, NodeException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode search = mapper.readTree(body);

		ObjectNode query = null;
		if (search.hasNonNull("query")) {
			query = getObject(search.get("query"), mapper);
		}

		if (query == null) {
			return body;
		}

		ObjectNode bool = null;
		if (query.hasNonNull("bool")) {
			bool = getObject(query.get("bool"), mapper);
		} else {
			// TODO add bool?
		}

		if (bool == null) {
			return body;
		}

		ArrayNode filter = null;
		if (bool.hasNonNull("filter")) {
			filter = getArray(bool.get("filter"), mapper);
		} else {
			filter = mapper.createArrayNode();
			bool.set("filter", filter);
		}

		if (filter == null) {
			return body;
		}

		filter.add(getGroupIdFilter(searchParam.nodeId, mapper));
		if (searchParam.nodeId > 0) {
			filter.add(getNodeIdFilter(searchParam.nodeId, mapper));
		}
		if (!ObjectTransformer.isEmpty(searchParam.folderId)) {
			filter.add(getFolderIdFilter(searchParam.folderId, searchParam.recursive, mapper));
		}

		switch (searchParam.wastebinSearch) {
		case exclude:
			filter.add(getWastebinFilter(false, mapper));
			break;
		case only:
			filter.add(getWastebinFilter(true, mapper));
			break;
		default:
			break;
		}

		return mapper.writeValueAsString(search);
	}

	/**
	 * Get the given node as ArrayNode. Either it already is an ArrayNode, or a new ArrayNode is created and the
	 * given node added as first child.
	 * @param node node
	 * @param mapper mapper
	 * @return ArrayNode instance (may be null)
	 */
	protected ArrayNode getArray(JsonNode node, ObjectMapper mapper) {
		switch (node.getNodeType()) {
		case ARRAY:
			return (ArrayNode)node;
		case OBJECT:
			ArrayNode arrayNode = mapper.createArrayNode();
			arrayNode.add(node);
			return arrayNode;
		default:
			return null;
		}
	}

	/**
	 * Get the node as ObjectNode
	 * @param node node
	 * @param mapper mapper
	 * @return ObjectNode instance (may be null)
	 */
	protected ObjectNode getObject(JsonNode node, ObjectMapper mapper) {
		switch (node.getNodeType()) {
		case OBJECT:
			return (ObjectNode)node;
		default:
			return null;
		}
	}

	/**
	 * Get the nodeId filter
	 * @param nodeId node ID
	 * @param mapper object mapper
	 * @return filter
	 */
	protected JsonNode getNodeIdFilter(int nodeId, ObjectMapper mapper) {
		return mapper.createObjectNode().set("term", mapper.createObjectNode().put("nodeId", nodeId));
	}

	/**
	 * Get the folderId filter
	 * @param folderId list of folder IDs
	 * @param recursive true to also add all subfolders (visible to the current transaction)
	 * @param mapper object mapper
	 * @return filter
	 * @throws NodeException
	 */
	protected JsonNode getFolderIdFilter(List<Integer> folderId, boolean recursive, ObjectMapper mapper) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<Integer> idSet = new HashSet<>();
		for (Folder folder : t.getObjects(Folder.class, folderId)) {
			addVisibleFolders(folder.getMaster(), idSet, recursive, t);
		}
		ArrayNode arrayNode = mapper.createArrayNode();
		for (int id : idSet) {
			arrayNode.add(id);
		}
		return mapper.createObjectNode().set("terms", mapper.createObjectNode().set("folderId", arrayNode));
	}

	/**
	 * Get the "deleted" filter
	 * @param wastebin true to search in the wastebin (only), false to not search in the wastebin
	 * @param mapper object mapper
	 * @return filter
	 * @throws NodeException
	 */
	protected JsonNode getWastebinFilter(boolean wastebin, ObjectMapper mapper) throws NodeException {
		return mapper.createObjectNode().set("term", mapper.createObjectNode().put("deleted", wastebin));
	}

	/**
	 * Get the groupId filter (filtering for user permissions)
	 * @param nodeId node ID
	 * @param mapper object mapper
	 * @return filter
	 * @throws NodeException
	 */
	protected JsonNode getGroupIdFilter(int nodeId, ObjectMapper mapper) throws NodeException {
		List<Integer> groupIds = TransactionManager.getCurrentTransaction().getPermHandler().getGroupIds(nodeId);
		ArrayNode arrayNode = mapper.createArrayNode();
		for (int id : groupIds) {
			arrayNode.add(id);
		}
		return mapper.createObjectNode().set("terms", mapper.createObjectNode().set("groupId", arrayNode));
	}

	/**
	 * Add the ID of the folder (and subfolders, if recursive == true) to idSet, if the folder is visible to the current transaction)
	 * @param folder folder
	 * @param idSet idSet (will be modified)
	 * @param recursive true for adding subfolders
	 * @param t transaction
	 * @throws NodeException
	 */
	protected void addVisibleFolders(Folder folder, Set<Integer> idSet, boolean recursive, Transaction t) throws NodeException {
		if (folder == null || !t.canView(folder)) {
			return;
		}
		if (idSet.add(folder.getId()) && recursive) {
			for (Folder child : folder.getChildFolders()) {
				addVisibleFolders(child, idSet, recursive, t);
			}
		}
	}
}
