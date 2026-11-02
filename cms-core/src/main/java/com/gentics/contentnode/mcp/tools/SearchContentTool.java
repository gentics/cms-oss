package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.SearchHit;
import com.gentics.contentnode.mcp.util.EnterpriseSearch;
import com.gentics.contentnode.mcp.util.SearchArgs;
import com.gentics.contentnode.mcp.util.SearchBodies;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Searches the CMS Elasticsearch indices through the enterprise passthrough ({@link EnterpriseSearch}), which adds the
 * CMS permission group, node, folder and wastebin filters; the hits are post-filtered by
 * {@code ObjectPermission.view}. Answers {@code search-unavailable} without the enterprise search.
 */
public class SearchContentTool extends AbstractMcpTool {
	static final String ARG_QUERY = "query";

	static final String ARG_SIZE = "size";

	static final String ARG_FROM = "from";

	static final String ARG_RAW_QUERY = "rawQuery";

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param total total number of matches, filtered by permission groups only
	 * @param totalIsExact always false, the total is an upper bound
	 * @param tookMs search time in milliseconds
	 * @param hits hits the caller may view
	 * @param queryUsed body sent to Elasticsearch
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(long total, boolean totalIsExact, Integer tookMs, List<SearchHit> hits,
			ObjectNode queryUsed) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_QUERY, schema("string",
				"Natural language or Lucene query_string syntax, matched over the name, description and indexed text "
						+ "of the object.",
				"minLength", 1, "maxLength", 2000));
		properties.putAll(SearchArgs.schemaProperties(true, true));
		properties.put(ARG_SIZE, schema("integer", null, "minimum", 1, "maximum", 50, "default", 10));
		properties.put(ARG_FROM, schema("integer", null, "minimum", 0, "maximum", 10000, "default", 0));
		properties.put(ARG_RAW_QUERY, schema("object",
				"Escape hatch: an Elasticsearch query (or a body with only 'query'), used instead of query and "
						+ "filters. It is wrapped into a bool query so the CMS adds its permission, node, folder and "
						+ "wastebin filters, and it is rejected if it cannot be wrapped. Report queryUsed to the user "
						+ "whenever you use it."));

		return Tool.builder().name("search_content").title("Search content")
				.description(description())
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_QUERY))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		String query = stringArg(arguments, ARG_QUERY, 1, 2000);
		if (query == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_QUERY));
		}
		SearchArgs args = SearchArgs.of(arguments);
		int size = intArg(arguments, ARG_SIZE, 1, 50, 10);
		int from = intArg(arguments, ARG_FROM, 0, 10000, 0);
		Object rawQuery = arguments.get(ARG_RAW_QUERY);
		if (rawQuery != null && !(rawQuery instanceof Map)) {
			throw new IllegalArgumentException("Argument '%s' must be an object".formatted(ARG_RAW_QUERY));
		}

		ObjectNode body = body(query, args, (Map<String, Object>) rawQuery, size, from);
		EnterpriseSearch.Response response = EnterpriseSearch.search(body, args.scope());
		return new Result(response.total(), false, response.tookMs(), response.results(), response.queryUsed());
	}

	/**
	 * Build the body
	 * @param query query
	 * @param args search arguments
	 * @param rawQuery raw query, replaces query and filters, may be null
	 * @param size page size
	 * @param from offset
	 * @return body
	 * @throws IllegalArgumentException if the raw query cannot be wrapped
	 */
	static ObjectNode body(String query, SearchArgs args, Map<String, Object> rawQuery, int size, int from) {
		ObjectNode body = rawQuery != null ? SearchBodies.raw(rawQuery)
				: SearchBodies.bool(List.of(SearchBodies.queryString(query)), args.filterClauses());
		body.put("from", from);
		body.put("size", size);
		body.put("track_total_hits", true);
		return SearchBodies.highlight(body);
	}

	/**
	 * Get the tool description
	 * @return description
	 */
	private static String description() {
		return "The one retrieval tool for CMS content. Searches the Elasticsearch indices for pages, folders, files, "
				+ "images and forms the acting user may see, and returns references with highlight snippets. TWO-STEP "
				+ "PATTERN: this tool finds candidates, it does not give you their content. Call get_page (or "
				+ "get_file, get_image) on a hit before quoting it, summarising it or asserting anything about its "
				+ "wording. CORPUS LIMIT: for pages the indexed text is only what an editor typed into a text part, as "
				+ "separate values, for the current version. Text that comes from the template, from navigation or "
				+ "from rendering is NOT indexed, so a miss does not mean the published page lacks the phrase. The "
				+ "total is filtered by the user's permission groups but not by per-object permission, so treat it "
				+ "as an upper bound and count the hits you actually received when the number matters. Do NOT use "
				+ "this tool to list a folder (use list_folder_items), to count without reading (use count_content) "
				+ "or to find duplicates of a known object (use find_similar).";
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("total", schema("integer", "Upper bound: filtered by permission groups, not per object."));
		properties.put("totalIsExact", schema("boolean", "Always false, the total is an upper bound."));
		properties.put("tookMs", schema("integer", null));
		properties.put("hits", schema("array", null, "items", SearchHit.jsonSchema()));
		properties.put("queryUsed", schema("object", "The Elasticsearch body actually sent, for transparency."));
		return schema("object", null, "properties", properties, "required", List.of("total", "hits"));
	}
}
