package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.SearchHit;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.EnterpriseSearch;
import com.gentics.contentnode.mcp.util.SearchArgs;
import com.gentics.contentnode.mcp.util.SearchBodies;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Finds objects similar to an indexed object or to a text with a more-like-this query, through the enterprise
 * passthrough ({@link EnterpriseSearch}), filtered like {@link SearchContentTool}. For a ref, the object is first
 * looked up in the index (which also checks that the caller may view it), and it is not among the hits.
 */
public class FindSimilarTool extends AbstractMcpTool {
	static final String ARG_REF = "ref";

	static final String ARG_TEXT = "text";

	static final String ARG_SIZE = "size";

	static final String ARG_MIN_SCORE = "minScore";

	/**
	 * Object mapper
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param total total number of matches, filtered by permission groups only
	 * @param hits hits the caller may view
	 * @param queryUsed body sent to Elasticsearch
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(long total, List<SearchHit> hits, ObjectNode queryUsed) {
	}

	/**
	 * Validated arguments of a call
	 * @param ref object to compare with, null for text
	 * @param text text to compare with, null for ref
	 * @param args search arguments
	 * @param size number of hits
	 * @param minScore minimum score, may be null
	 */
	record Request(ObjectRef ref, String text, SearchArgs args, int size, Double minScore) {
		/**
		 * Parse and validate the arguments
		 * @param arguments arguments
		 * @return request
		 * @throws IllegalArgumentException if an argument is invalid, or not exactly one of ref and text is given
		 */
		@SuppressWarnings("unchecked")
		static Request of(Map<String, Object> arguments) {
			Object refArg = arguments.get(ARG_REF);
			String text = nullIfBlank(stringArg(arguments, ARG_TEXT, 0, 20000));
			if ((refArg == null) == (text == null)) {
				throw new IllegalArgumentException(
						"Pass exactly one of '%s' and '%s'".formatted(ARG_REF, ARG_TEXT));
			}
			ObjectRef ref = null;
			if (refArg != null) {
				if (!(refArg instanceof Map<?, ?> map)) {
					throw new IllegalArgumentException("Argument '%s' must be an object".formatted(ARG_REF));
				}
				ObjectRef.Type type = ObjectRef.Type.fromValue(String.valueOf(map.get("type")));
				if (!SearchArgs.TYPES.contains(type.value())) {
					throw new IllegalArgumentException("Argument '%s' must be one of the types %s, not a %s"
							.formatted(ARG_REF, SearchArgs.TYPES, type.value()));
				}
				ref = ObjectRef.of(type, Args.id((Map<String, Object>) map, "id"));
			}
			Object minScore = arguments.get(ARG_MIN_SCORE);
			if (minScore != null && (!(minScore instanceof Number number) || number.doubleValue() < 0)) {
				throw new IllegalArgumentException("Argument '%s' must be a number of at least 0".formatted(
						ARG_MIN_SCORE));
			}
			return new Request(ref, text, SearchArgs.of(arguments), intArg(arguments, ARG_SIZE, 1, 50, 10),
					minScore != null ? ((Number) minScore).doubleValue() : null);
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> ref = new LinkedHashMap<>(ObjectRef.jsonSchema("The object to compare with."));
		ref.put("required", List.of("type", "id"));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_REF, ref);
		properties.put(ARG_TEXT, schema("string", "The text to compare with.", "maxLength", 20000));
		properties.putAll(SearchArgs.schemaProperties(false, false));
		properties.put(ARG_SIZE, schema("integer", null, "minimum", 1, "maximum", 50, "default", 10));
		properties.put(ARG_MIN_SCORE, schema("number", null, "minimum", 0));

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("total", schema("integer", "Upper bound: filtered by permission groups, not per object."));
		output.put("hits", schema("array", null, "items", SearchHit.jsonSchema()));
		output.put("queryUsed", schema("object", null));

		return Tool.builder().name("find_similar").title("Find similar objects")
				.description("Finds objects similar to a given object or to a block of text, for duplicate detection "
						+ "before creating a page and for checking whether a construct like the one you are about to "
						+ "build already exists. Pass exactly one of ref or text; a call with neither or both is "
						+ "refused as invalid-params. Do NOT use it for keyword search with filters, that is "
						+ "search_content.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("hits")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Request request = Request.of(arguments);

		JsonNode like;
		if (request.ref() != null) {
			// find the indexed document of the object, filtered like any search
			ObjectNode lookup = SearchBodies.bool(List.of(idsClause(request.ref().id())), List.of());
			lookup.put("size", 1);
			EnterpriseSearch.Scope scope = request.args().scope();
			EnterpriseSearch.Response found = EnterpriseSearch.search(lookup, new EnterpriseSearch.Scope(
					List.of(request.ref().type().value()), scope.nodeId(), null, false, scope.languages()));
			if (found.hits().isEmpty()) {
				throw new EntityNotFoundException("%s %d is not in the search index, or not visible to you".formatted(
						request.ref().type().value(), request.ref().id()));
			}
			ArrayNode documents = MAPPER.createArrayNode();
			documents.addObject().put("_index", found.hits().get(0).index()).put("_id",
					Integer.toString(request.ref().id()));
			like = documents;
		} else {
			like = MAPPER.getNodeFactory().textNode(request.text());
		}

		EnterpriseSearch.Response response = EnterpriseSearch.search(body(like, request), request.args().scope());
		return result(response, request);
	}

	/**
	 * Build the more-like-this body. One hit more than requested is asked for, because the ref's own object is removed.
	 * @param like documents or text
	 * @param request request
	 * @return body
	 */
	static ObjectNode body(JsonNode like, Request request) {
		ObjectNode body = SearchBodies.bool(List.of(SearchBodies.moreLikeThis(like)), List.of());
		body.put("size", request.ref() != null ? request.size() + 1 : request.size());
		body.put("track_total_hits", true);
		if (request.minScore() != null) {
			body.put("min_score", request.minScore());
		}
		return SearchBodies.highlight(body);
	}

	/**
	 * Build the result, without the ref's own object
	 * @param response search response
	 * @param request request
	 * @return result
	 */
	static Result result(EnterpriseSearch.Response response, Request request) {
		List<SearchHit> hits = response.results().stream()
				.filter(hit -> request.ref() == null || hit.ref().type() != request.ref().type()
						|| hit.ref().id() != request.ref().id())
				.limit(request.size()).toList();
		return new Result(response.total(), hits, response.queryUsed());
	}

	/**
	 * Create the clause matching a document ID
	 * @param id ID
	 * @return clause
	 */
	private static JsonNode idsClause(int id) {
		ObjectNode ids = MAPPER.createObjectNode();
		ids.putObject("ids").putArray("values").add(Integer.toString(id));
		return ids;
	}
}
