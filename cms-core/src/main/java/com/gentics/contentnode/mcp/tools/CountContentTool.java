package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.EnterpriseSearch;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.SearchArgs;
import com.gentics.contentnode.mcp.util.SearchBodies;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.lib.log.NodeLogger;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Counts objects in the CMS Elasticsearch indices, optionally grouped by language, template or online state, through
 * the enterprise passthrough ({@link EnterpriseSearch}). Counts are filtered by the CMS permission groups only.
 * Template groups get the template's name as label when the caller may view it ({@link TemplateResourceImpl#get}).
 */
public class CountContentTool extends AbstractMcpTool {
	private static final NodeLogger logger = NodeLogger.getNodeLogger(CountContentTool.class);

	static final String ARG_QUERY = "query";

	static final String ARG_GROUP_BY = "groupBy";

	static final String ARG_GROUP_LIMIT = "groupLimit";

	/**
	 * Values of {@link #ARG_GROUP_BY}
	 */
	static final List<String> GROUP_BY = List.of("language", "template", "online");

	/**
	 * Object mapper
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * A group of the count. Fields that are not set are omitted on serialization.
	 * @param key group key
	 * @param label label of the key (template name)
	 * @param count number of objects
	 * @param ref reference to the key object (template)
	 */
	@JsonInclude(Include.NON_NULL)
	public record Group(String key, String label, long count, ObjectRef ref) {
	}

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param total total number of matches, filtered by permission groups only
	 * @param totalIsExact always false, the total is an upper bound
	 * @param tookMs search time in milliseconds
	 * @param groupBy grouping, if requested
	 * @param groups groups, if requested
	 * @param queryUsed body sent to Elasticsearch
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(long total, boolean totalIsExact, Integer tookMs, String groupBy, List<Group> groups,
			ObjectNode queryUsed) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_QUERY, schema("string", "Omit to count everything in scope.", "maxLength", 2000));
		properties.putAll(SearchArgs.schemaProperties(true, true));
		properties.put(ARG_GROUP_BY, schema("string",
				"Group the count one way. Omit for a single total, which is the cheaper path.", "enum", GROUP_BY));
		properties.put(ARG_GROUP_LIMIT, schema("integer", null, "minimum", 1, "maximum", 100, "default", 25));

		Map<String, Object> group = new LinkedHashMap<>();
		group.put("key", schema("string", null));
		group.put("label", schema("string", null));
		group.put("count", schema("integer", null));
		group.put("ref", ObjectRef.jsonSchema(null));
		Map<String, Object> output = new LinkedHashMap<>();
		output.put("total", schema("integer", "Upper bound: filtered by permission groups, not per object."));
		output.put("totalIsExact", schema("boolean", "Always false, the total is an upper bound."));
		output.put("tookMs", schema("integer", null));
		output.put("groupBy", schema("string", null, "enum", GROUP_BY));
		output.put("groups", schema("array", "Present only when groupBy was given.", "items",
				schema("object", null, "properties", group)));
		output.put("queryUsed", schema("object", null));

		return Tool.builder().name("count_content").title("Count and group content")
				.description(description())
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("total")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		String query = nullIfBlank(stringArg(arguments, ARG_QUERY, 0, 2000));
		SearchArgs args = SearchArgs.of(arguments);
		String groupBy = stringArg(arguments, ARG_GROUP_BY, 1, 16);
		if (groupBy != null && !GROUP_BY.contains(groupBy)) {
			throw new IllegalArgumentException(
					"Argument '%s' must be one of %s, but was '%s'".formatted(ARG_GROUP_BY, GROUP_BY, groupBy));
		}
		int groupLimit = intArg(arguments, ARG_GROUP_LIMIT, 1, 100, 25);

		EnterpriseSearch.Response response = EnterpriseSearch.search(body(query, args, groupBy, groupLimit),
				args.scope());
		List<Group> groups = groupBy != null ? groups(response.aggregations()) : null;
		if ("template".equals(groupBy)) {
			groups = labelTemplates(groups, args.nodeId());
		}
		return new Result(response.total(), false, response.tookMs(), groupBy, groups, response.queryUsed());
	}

	/**
	 * Build the body: no hits, the total and at most one aggregation "groups"
	 * @param query query, may be null
	 * @param args search arguments
	 * @param groupBy grouping, may be null
	 * @param groupLimit maximum number of groups
	 * @return body
	 */
	static ObjectNode body(String query, SearchArgs args, String groupBy, int groupLimit) {
		ObjectNode body = SearchBodies.bool(query != null ? List.of(SearchBodies.queryString(query)) : List.of(),
				args.filterClauses());
		body.put("size", 0);
		body.put("track_total_hits", true);
		if (groupBy != null) {
			ObjectNode aggregation = body.putObject("aggs").putObject("groups");
			switch (groupBy) {
			case "language" -> aggregation.putObject("terms").put("field", "languageCode").put("size", groupLimit);
			case "template" -> aggregation.putObject("terms").put("field", "templateId").put("size", groupLimit);
			default -> {
				// "online" holds the IDs of the nodes the object is online in
				ObjectNode filters = aggregation.putObject("filters").putObject("filters");
				filters.set("true", SearchBodies.onlineClause());
				filters.putObject("false").putObject("bool").set("must_not", SearchBodies.onlineClause());
			}
			}
		}
		return body;
	}

	/**
	 * Get the groups of the aggregation "groups" (terms buckets or keyed filters buckets)
	 * @param aggregations aggregations of the response, may be null
	 * @return groups
	 */
	static List<Group> groups(JsonNode aggregations) {
		List<Group> groups = new ArrayList<>();
		JsonNode buckets = aggregations != null ? aggregations.path("groups").path("buckets") : MAPPER.nullNode();
		if (buckets.isArray()) {
			for (JsonNode bucket : buckets) {
				JsonNode key = bucket.has("key_as_string") ? bucket.get("key_as_string") : bucket.get("key");
				groups.add(new Group(key.asText(), null, bucket.path("doc_count").asLong(), null));
			}
		} else if (buckets.isObject()) {
			buckets.fields().forEachRemaining(
					entry -> groups.add(new Group(entry.getKey(), null, entry.getValue().path("doc_count").asLong(),
							null)));
		}
		return groups;
	}

	/**
	 * Add the template names and refs to template groups, for templates the caller may view
	 * @param groups groups keyed by template ID
	 * @param nodeId node ID of the count, may be null
	 * @return labelled groups
	 */
	private static List<Group> labelTemplates(List<Group> groups, Integer nodeId) {
		TemplateResource templateResource = RestPermissions.guard(TemplateResource.class, new TemplateResourceImpl());
		List<Group> labelled = new ArrayList<>();
		for (Group group : groups) {
			try {
				TemplateLoadResponse response = templateResource.get(group.key(), nodeId, false, false);
				requireOk(response, "Template %s could not be loaded".formatted(group.key()));
				labelled.add(new Group(group.key(), response.getTemplate().getName(), group.count(),
						ObjectRef.forTemplate(response.getTemplate(), nodeId)));
			} catch (Exception e) {
				logger.debug("Template %s of a count group is not labelled".formatted(group.key()), e);
				labelled.add(group);
			}
		}
		return labelled;
	}

	/**
	 * Get the tool description
	 * @return description
	 */
	private static String description() {
		return "Counts matching objects, and optionally groups the count one way, without retrieving documents. Use it "
				+ "for reporting questions such as how many pages are offline, or how many per template. Counts are "
				+ "filtered by the acting user's permission groups but NOT by per-object permission, so they are an "
				+ "upper bound: when a number has to be exact, retrieve the objects with search_content and count what "
				+ "you received. CORPUS LIMIT: the same as search_content, only text an editor typed into a text part "
				+ "is indexed. Do NOT use this tool when you need the objects themselves. Counts run over the same "
				+ "indexed fields and object types search_content matches: a count_content call with a search's own "
				+ "inputs (query, types, nodeId, folderId, recursive, languages, filters) reproduces that search's "
				+ "total. queryUsed is returned for display and cannot be passed back to either tool.";
	}
}
