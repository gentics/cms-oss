package com.gentics.contentnode.mcp.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.SearchHit;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NamedNodeObject;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInFolder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.resource.parameter.SearchParameterBean;
import com.gentics.lib.log.NodeLogger;

/**
 * Access to the Elasticsearch passthrough of the enterprise module cms-search, which cms-oss cannot compile against.
 * Every enterprise class and method is looked up by reflection; when the module or the {@code elasticsearch} feature
 * is missing, a {@link SearchUnavailableException} is thrown.
 *
 * <p>
 * A search runs the passthrough's own request path: the body (which must have {@code query.bool}, see
 * {@link SearchBodies}) gets the CMS permission, node, folder and wastebin filters from
 * {@code SearchResourceImpl#addFilters}, and is posted to the indices of {@code Indexer#getMultiIndexUrl} with
 * {@code Indexer#getClient}. The response handling of the passthrough is not used: it needs JAX-RS injection and reads
 * the hit type from {@code _type}, which Elasticsearch 8 no longer returns. Hits are post-filtered with
 * {@link Transaction#canView}, as the passthrough does.
 * </p>
 */
public final class EnterpriseSearch {
	private static final NodeLogger logger = NodeLogger.getNodeLogger(EnterpriseSearch.class);

	static final String SEARCH_RESOURCE_CLASS = "com.gentics.contentnode.rest.resource.impl.search.SearchResourceImpl";

	static final String INDEXER_CLASS = "com.gentics.contentnode.object.search.Indexer";

	static final String INDEX_TYPE_CLASS = "com.gentics.contentnode.object.search.IndexType";

	/**
	 * Object mapper
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Object classes by index type
	 */
	static final Map<String, Class<? extends NodeObject>> CLASSES = Map.of("page", Page.class, "folder",
			Folder.class, "file", File.class, "image", ImageFile.class, "form", Form.class);

	/**
	 * Scope of a search
	 * @param types object types (index types)
	 * @param nodeId node ID, may be null
	 * @param folderIds folder IDs, may be null
	 * @param recursive whether to include the subfolders of the folders
	 * @param languages language codes, may be null
	 */
	public record Scope(List<String> types, Integer nodeId, List<Integer> folderIds, boolean recursive,
			List<String> languages) {
	}

	/**
	 * A hit the caller may view
	 * @param result hit as returned by the tools
	 * @param index Elasticsearch index of the hit
	 */
	public record Hit(SearchHit result, String index) {
	}

	/**
	 * Response of a search
	 * @param total total number of matches (filtered by permission groups, not per object)
	 * @param tookMs search time in milliseconds
	 * @param hits hits the caller may view
	 * @param aggregations aggregations, may be null
	 * @param queryUsed body sent to Elasticsearch
	 */
	public record Response(long total, Integer tookMs, List<Hit> hits, JsonNode aggregations, ObjectNode queryUsed) {
		/**
		 * Get the hits as returned by the tools
		 * @return hits
		 */
		public List<SearchHit> results() {
			return hits.stream().map(Hit::result).toList();
		}
	}

	private EnterpriseSearch() {
	}

	/**
	 * Search
	 * @param body body with {@code query.bool}
	 * @param scope scope
	 * @return response
	 * @throws SearchUnavailableException if the search is not available
	 * @throws IllegalArgumentException if Elasticsearch rejects the query
	 * @throws Exception if the search fails
	 */
	public static Response search(ObjectNode body, Scope scope) throws Exception {
		Class<?> resourceClass = available(SEARCH_RESOURCE_CLASS, Feature.ELASTICSEARCH::isActivated);

		try (Trx trx = ContentNodeHelper.trx();
				ChannelTrx cTrx = new ChannelTrx(scope.nodeId());
				WastebinFilter wbf = new WastebinFilter(Wastebin.EXCLUDE)) {
			ObjectNode sent = addFilters(resourceClass, body, scope);
			String indices = (String) enterpriseMethod(INDEXER_CLASS, "getMultiIndexUrl", List.class, List.class)
					.invoke(null, scope.languages(), indexTypes(scope.types()));
			if (indices == null || indices.equals("/")) {
				// no index for the types and languages: never send "//_search", which searches every index
				trx.success();
				return new Response(0, null, List.of(), null, sent);
			}

			HttpClient client = (HttpClient) enterpriseMethod(INDEXER_CLASS, "getClient").invoke(null);
			PostMethod post = new PostMethod(indices + "/_search");
			int status;
			String answer;
			try {
				post.setRequestEntity(new StringRequestEntity(MAPPER.writeValueAsString(sent), "application/json",
						"UTF-8"));
				status = client.executeMethod(post);
				// Elasticsearch answers JSON without charset, which HttpClient would read as ISO-8859-1
				byte[] bytes = post.getResponseBody();
				answer = bytes != null ? new String(bytes, StandardCharsets.UTF_8) : "";
			} finally {
				post.releaseConnection();
			}
			if (status == 400) {
				throw new IllegalArgumentException("Elasticsearch rejected the query: %s".formatted(reason(answer)));
			}
			if (status != 200) {
				throw new NodeException("Elasticsearch answered %d: %s".formatted(status, reason(answer)));
			}

			JsonNode response = MAPPER.readTree(answer);
			List<Hit> hits = visibleHits(response.path("hits").path("hits"), scope.nodeId(),
					trx.getTransaction());
			trx.success();
			return new Response(total(response), response.hasNonNull("took") ? response.get("took").asInt() : null,
					hits, response.get("aggregations"), sent);
		}
	}

	/**
	 * Check that the enterprise search is available
	 * @param className name of the class that must exist
	 * @param feature check of the feature
	 * @return the class
	 * @throws SearchUnavailableException if the class is missing or the feature is off
	 */
	static Class<?> available(String className, BooleanSupplier feature) {
		Class<?> clazz;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException | LinkageError e) {
			throw new SearchUnavailableException("The search module is not installed in this CMS", e);
		}
		if (!feature.getAsBoolean()) {
			throw new SearchUnavailableException("The CMS feature 'elasticsearch' is not activated", null);
		}
		return clazz;
	}

	/**
	 * Add the CMS filters to the body with {@code SearchResourceImpl#addFilters}, and check that it did
	 * @param resourceClass search resource class
	 * @param body body
	 * @param scope scope
	 * @return filtered body
	 * @throws Exception
	 */
	private static ObjectNode addFilters(Class<?> resourceClass, ObjectNode body, Scope scope) throws Exception {
		SearchParameterBean bean = new SearchParameterBean();
		bean.nodeId = scope.nodeId() != null ? scope.nodeId() : 0;
		bean.folderId = scope.folderIds();
		bean.recursive = scope.recursive();
		bean.languages = scope.languages();

		Method addFilters;
		Object resource;
		try {
			addFilters = resourceClass.getDeclaredMethod("addFilters", String.class, SearchParameterBean.class);
			addFilters.setAccessible(true);
			resource = resourceClass.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw unavailable(e);
		}
		String filtered;
		try {
			filtered = (String) addFilters.invoke(resource, MAPPER.writeValueAsString(body), bean);
		} catch (InvocationTargetException e) {
			throw e.getCause() instanceof Exception cause ? cause : e;
		}

		ObjectNode sent = (ObjectNode) MAPPER.readTree(filtered);
		if (!hasPermissionFilter(sent)) {
			// fail closed: the passthrough forwards a body it cannot filter unchanged
			throw new IllegalStateException("The search body was not filtered by the CMS, it is not sent");
		}
		return sent;
	}

	/**
	 * Check whether the body has the CMS permission filter (a {@code terms} filter on {@code groupId})
	 * @param body body
	 * @return true if filtered
	 */
	static boolean hasPermissionFilter(JsonNode body) {
		for (JsonNode filter : body.path("query").path("bool").path("filter")) {
			if (filter.path("terms").has("groupId")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the enterprise index types
	 * @param types type names
	 * @return list of IndexType constants
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<Object> indexTypes(List<String> types) {
		try {
			Class<? extends Enum> indexType = (Class<? extends Enum>) Class.forName(INDEX_TYPE_CLASS);
			List<Object> constants = new ArrayList<>();
			for (String type : types) {
				constants.add(Enum.valueOf(indexType, type));
			}
			return constants;
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw unavailable(e);
		}
	}

	/**
	 * Get a public static method of an enterprise class
	 * @param className class name
	 * @param name method name
	 * @param parameterTypes parameter types
	 * @return method
	 * @throws SearchUnavailableException if the method does not exist
	 */
	private static Method enterpriseMethod(String className, String name, Class<?>... parameterTypes) {
		try {
			return Class.forName(className).getMethod(name, parameterTypes);
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw unavailable(e);
		}
	}

	/**
	 * Create the exception for an enterprise class that does not have the expected shape
	 * @param cause cause
	 * @return exception
	 */
	private static SearchUnavailableException unavailable(Exception cause) {
		logger.error("The search module does not have the expected classes and methods", cause);
		return new SearchUnavailableException("The search module of this CMS is not supported", cause);
	}

	/**
	 * Get the hits the caller may view
	 * @param hits hits of the response
	 * @param nodeId node ID of the search, may be null
	 * @param t transaction
	 * @return visible hits
	 * @throws NodeException
	 */
	private static List<Hit> visibleHits(JsonNode hits, Integer nodeId, Transaction t) throws NodeException {
		List<Hit> visible = new ArrayList<>();
		for (JsonNode hit : hits) {
			String type = type(hit);
			Class<? extends NodeObject> clazz = CLASSES.get(type);
			if (clazz == null) {
				continue;
			}
			NodeObject object = t.getObject(clazz, hit.path("_id").asText());
			if (object == null || !t.canView(object)) {
				continue;
			}
			Integer refNodeId = nodeId;
			if (refNodeId == null) {
				Folder folder = object instanceof Folder f ? f
						: object instanceof NodeObjectInFolder inFolder ? inFolder.getFolder()
								: object instanceof File file ? file.getFolder() : null;
				refNodeId = folder != null && folder.getNode() != null ? folder.getNode().getId() : null;
			}
			String name = object instanceof NamedNodeObject named ? named.getName() : null;
			String globalId = object.getGlobalId() != null ? object.getGlobalId().toString() : null;
			SearchHit result = hit(hit, type, object.getId(), globalId, refNodeId, name);
			visible.add(new Hit(result, hit.path("_index").asText()));
		}
		return visible;
	}

	/**
	 * Get the object type of a hit: {@code _source._type} (as indexed by the CMS), or the hit's {@code _type} of
	 * Elasticsearch versions before 8
	 * @param hit hit
	 * @return type, may be null
	 */
	static String type(JsonNode hit) {
		JsonNode sourceType = hit.path("_source").get("_type");
		if (sourceType != null && sourceType.isTextual()) {
			return sourceType.asText();
		}
		return hit.hasNonNull("_type") ? hit.get("_type").asText() : null;
	}

	/**
	 * Map a hit
	 * @param hit hit
	 * @param type object type
	 * @param id object ID
	 * @param globalId global ID, may be null
	 * @param nodeId node ID, may be null
	 * @param name object name, may be null
	 * @return search hit
	 */
	static SearchHit hit(JsonNode hit, String type, int id, String globalId, Integer nodeId, String name) {
		JsonNode source = hit.path("_source");
		String language = textOrNull(source.get("languageCode"));
		ObjectRef ref = new ObjectRef(ObjectRef.Type.fromValue(type), id, globalId, nodeId, name, null, language,
				null, null);
		List<String> snippets = new ArrayList<>();
		for (JsonNode fragments : hit.path("highlight")) {
			fragments.forEach(fragment -> snippets.add(fragment.asText()));
		}
		JsonNode online = source.get("online");
		Boolean isOnline = online == null || online.isNull() ? null
				: online.isArray() ? !online.isEmpty() : online.isBoolean() ? online.asBoolean() : online.asInt() > 0;
		return new SearchHit(ref, hit.hasNonNull("_score") ? hit.get("_score").asDouble() : null, snippets, language,
				isOnline, intOrNull(source.get("edited")), intOrNull(source.get("templateId")),
				intOrNull(source.get("folderId")));
	}

	/**
	 * Get the total of a response ({@code hits.total.value} or, before Elasticsearch 7, {@code hits.total})
	 * @param response response
	 * @return total
	 */
	static long total(JsonNode response) {
		JsonNode total = response.path("hits").path("total");
		return total.isObject() ? total.path("value").asLong() : total.asLong();
	}

	/**
	 * Get the reason of an Elasticsearch error answer
	 * @param answer answer
	 * @return reason, at most 500 characters
	 */
	static String reason(String answer) {
		String reason = answer;
		try {
			JsonNode error = MAPPER.readTree(answer).path("error");
			JsonNode rootCause = error.path("root_cause").path(0).path("reason");
			reason = rootCause.isTextual() ? rootCause.asText()
					: error.path("reason").isTextual() ? error.path("reason").asText() : answer;
		} catch (Exception e) {
			// not JSON, use the answer itself
		}
		return reason != null && reason.length() > 500 ? reason.substring(0, 500) : reason;
	}

	/**
	 * Get a text value
	 * @param node node, may be null
	 * @return text or null
	 */
	private static String textOrNull(JsonNode node) {
		return node != null && node.isValueNode() && !node.isNull() ? node.asText() : null;
	}

	/**
	 * Get an integer value
	 * @param node node, may be null
	 * @return integer or null
	 */
	private static Integer intOrNull(JsonNode node) {
		return node != null && node.isNumber() ? node.asInt() : null;
	}
}
