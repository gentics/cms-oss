package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.TagInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Pages through the tags of one page, sorted by name, optionally filtered by tag name or construct keyword. Delegates to
 * {@link PageResourceImpl#load} without {@code update} (view permission, no lock), not to
 * {@code PageResourceImpl#getTags}, which ignores the node (channel) and returns no construct keywords.
 */
public class GetPageTagsTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	static final String ARG_NODE_ID = "nodeId";

	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	/**
	 * Result of the tool
	 * @param pageRef reference to the page
	 * @param list page of tags
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ObjectRef pageRef, @JsonUnwrapped ListResult<TagInfo> list) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to load the page in.", "minimum", 1));
		properties.putAll(ListArgs.schemaProperties(
				"Optional case-insensitive filter on the tag name or the construct keyword.", "tag", "tags", LIMITS));

		return Tool.builder().name("get_page_tags").title("Get page tags")
				.description("Returns just the content blocks of a page, paged and searchable by tag name. Use it when "
						+ "a page has many tags and the full page object from get_page would be too large. Do NOT use "
						+ "it as the first read of an unknown page: it gives you no page properties and no template "
						+ "context.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Args.id(arguments, ARG_ID);
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
		ListArgs args = ListArgs.of(arguments, LIMITS);

		PageLoadResponse response = RestPermissions.guard(PageResource.class, new PageResourceImpl())
				.load(Integer.toString(id), false, false, true, false, false, false, false, false, false, false, nodeId,
						null);
		requireOk(response, "The page %d could not be loaded".formatted(id));
		Page page = response.getPage();
		List<Tag> tags = page.getTags() != null ? new ArrayList<>(page.getTags().values()) : new ArrayList<>();

		return result(page, nodeId, tags, TagInfo.constructKeywords(tags), args);
	}

	/**
	 * Filter, sort and slice the tags
	 * @param page REST page
	 * @param nodeId node ID the page was loaded for, may be null
	 * @param tags tags of the page
	 * @param keywords construct keywords by construct ID
	 * @param args list arguments
	 * @return result
	 */
	static Result result(Page page, Integer nodeId, List<Tag> tags, Map<Integer, String> keywords, ListArgs args) {
		List<TagInfo> matching = new ArrayList<>();
		String query = args.query() != null ? args.query().toLowerCase(Locale.ROOT) : null;
		for (Tag tag : tags) {
			TagInfo info = TagInfo.of(tag, keywords, false);
			if (query == null || contains(info.name(), query) || contains(info.constructKeyword(), query)) {
				matching.add(info);
			}
		}
		matching.sort(Comparator.comparing(TagInfo::name, String.CASE_INSENSITIVE_ORDER));

		Slice slice = args.slice(matching.size());
		ObjectRef ref = nodeId != null ? ObjectRef.forPage(page, nodeId) : ObjectRef.forPage(page);
		return new Result(ref, ListResult.of(slice, slice.apply(matching)));
	}

	/**
	 * Check whether a value contains the lowercase query, ignoring case
	 * @param value value, may be null
	 * @param query lowercase query
	 * @return true if it contains the query
	 */
	private static boolean contains(String value, String query) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(query);
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> outputSchema() {
		Map<String, Object> schema = ListResult.jsonSchema(TagInfo.jsonSchema(), "tags");
		((Map<String, Object>) schema.get("properties")).put("pageRef", ObjectRef.jsonSchema("The page."));
		return schema;
	}
}
