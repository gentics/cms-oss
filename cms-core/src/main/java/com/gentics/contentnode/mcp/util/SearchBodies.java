package com.gentics.contentnode.mcp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builders for the Elasticsearch bodies of the search tools. Every body has {@code query.bool}, because the CMS adds
 * its permission, node, folder and wastebin filters to {@code query.bool.filter} only, and forwards any other body
 * unfiltered.
 */
public final class SearchBodies {
	/**
	 * Object mapper
	 */
	static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Indexed text fields, the name boosted
	 */
	public static final List<String> TEXT_FIELDS = List.of("name^2", "description", "content");

	/**
	 * Indexed text fields for more-like-this and highlighting
	 */
	public static final List<String> PLAIN_TEXT_FIELDS = List.of("name", "description", "content");

	/**
	 * Clauses of a bool query
	 */
	static final List<String> BOOL_CLAUSES = List.of("must", "filter", "should", "must_not");

	private SearchBodies() {
	}

	/**
	 * Create a body with a bool query
	 * @param must query clauses, a match_all if empty
	 * @param filter filter clauses
	 * @return body
	 */
	public static ObjectNode bool(List<JsonNode> must, List<JsonNode> filter) {
		ObjectNode bool = MAPPER.createObjectNode();
		ArrayNode mustArray = bool.putArray("must");
		if (must.isEmpty()) {
			mustArray.addObject().putObject("match_all");
		} else {
			mustArray.addAll(must);
		}
		bool.putArray("filter").addAll(filter);
		ObjectNode body = MAPPER.createObjectNode();
		body.putObject("query").set("bool", bool);
		return body;
	}

	/**
	 * Create the clause matching a query over the text fields. Invalid query syntax is ignored ({@code lenient}).
	 * @param query query in Lucene query string syntax
	 * @return clause
	 */
	public static JsonNode queryString(String query) {
		ObjectNode queryString = MAPPER.createObjectNode();
		queryString.put("query", query);
		TEXT_FIELDS.forEach(queryString.putArray("fields")::add);
		queryString.put("lenient", true);
		return MAPPER.createObjectNode().set("query_string", queryString);
	}

	/**
	 * Create the filter clauses of the tool filters
	 * @param online true for objects online in at least one node, false for objects online in none, null for both
	 * @param templateIds template IDs, may be null
	 * @param editedAfter minimum edit timestamp, may be null
	 * @return filter clauses
	 */
	public static List<JsonNode> filters(Boolean online, List<Integer> templateIds, Integer editedAfter) {
		List<JsonNode> filters = new ArrayList<>();
		if (online != null) {
			// the CMS indexes "online" as the IDs of the nodes the object is online in
			filters.add(online ? onlineClause() : MAPPER.createObjectNode().set("bool",
					MAPPER.createObjectNode().set("must_not", onlineClause())));
		}
		if (templateIds != null && !templateIds.isEmpty()) {
			ArrayNode ids = MAPPER.createArrayNode();
			templateIds.forEach(ids::add);
			filters.add(MAPPER.createObjectNode().set("terms", MAPPER.createObjectNode().set("templateId", ids)));
		}
		if (editedAfter != null) {
			ObjectNode range = MAPPER.createObjectNode();
			// a bare number on a date field is read as epoch milliseconds
			range.putObject("range").putObject("edited").put("gte", editedAfter).put("format", "epoch_second");
			filters.add(range);
		}
		return filters;
	}

	/**
	 * Get the clause matching objects online in at least one node
	 * @return clause
	 */
	public static JsonNode onlineClause() {
		return MAPPER.createObjectNode().set("exists", MAPPER.createObjectNode().put("field", "online"));
	}

	/**
	 * Turn a caller's raw query into a body with a bool query. A full body ({@code {"query": …}}) or a bare query
	 * clause is accepted. A query with its own {@code bool} is kept, its {@code filter} turned into an array, so the
	 * CMS can add its filters; any other query becomes the only {@code must} clause.
	 * @param rawQuery raw query
	 * @return body
	 * @throws IllegalArgumentException if the raw query cannot be wrapped
	 */
	public static ObjectNode raw(Map<String, Object> rawQuery) {
		JsonNode raw = MAPPER.valueToTree(rawQuery);
		JsonNode query = raw;
		if (raw.has("query")) {
			if (raw.size() > 1) {
				throw new IllegalArgumentException("Argument 'rawQuery' may only contain 'query'");
			}
			query = raw.get("query");
		}
		if (!query.isObject() || query.isEmpty()) {
			throw new IllegalArgumentException("Argument 'rawQuery' must contain a query object");
		}
		if (!query.has("bool")) {
			return bool(List.of(query), List.of());
		}
		if (query.size() > 1 || !query.get("bool").isObject()) {
			throw new IllegalArgumentException("Argument 'rawQuery' has a 'bool' query that cannot be wrapped");
		}
		ObjectNode bool = ((ObjectNode) query.get("bool")).deepCopy();
		for (String clause : BOOL_CLAUSES) {
			JsonNode value = bool.get(clause);
			if (value != null && !value.isObject() && !value.isArray()) {
				throw new IllegalArgumentException(
						"Argument 'rawQuery' has a 'bool.%s' that is neither an object nor an array".formatted(clause));
			}
		}
		JsonNode filter = bool.get("filter");
		ArrayNode filterArray = bool.putArray("filter");
		if (filter != null) {
			if (filter.isArray()) {
				filterArray.addAll((ArrayNode) filter);
			} else {
				filterArray.add(filter);
			}
		}
		ObjectNode body = MAPPER.createObjectNode();
		body.putObject("query").set("bool", bool);
		return body;
	}

	/**
	 * Add the highlighting of the text fields: three fragments of 200 characters per field
	 * @param body body
	 * @return body
	 */
	public static ObjectNode highlight(ObjectNode body) {
		ObjectNode highlight = body.putObject("highlight");
		highlight.put("fragment_size", 200);
		highlight.put("number_of_fragments", 3);
		ObjectNode fields = highlight.putObject("fields");
		PLAIN_TEXT_FIELDS.forEach(fields::putObject);
		return body;
	}

	/**
	 * Create a more-like-this clause over the text fields
	 * @param like documents ({@code {"_index", "_id"}}) or text to compare with
	 * @return clause
	 */
	public static JsonNode moreLikeThis(JsonNode like) {
		ObjectNode mlt = MAPPER.createObjectNode();
		PLAIN_TEXT_FIELDS.forEach(mlt.putArray("fields")::add);
		mlt.set("like", like);
		mlt.put("min_term_freq", 1);
		mlt.put("min_doc_freq", 1);
		return MAPPER.createObjectNode().set("more_like_this", mlt);
	}
}
