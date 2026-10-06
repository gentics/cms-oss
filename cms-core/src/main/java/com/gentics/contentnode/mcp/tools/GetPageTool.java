package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.mcp.model.TagInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Loads one page with its metadata and, by default, its tags with flattened properties. Delegates to
 * {@link PageResourceImpl#load} without {@code update}, which checks {@code ObjectPermission.view} and never locks.
 */
public class GetPageTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_INCLUDE = "include";

	/**
	 * Values of {@link #ARG_INCLUDE}, as in the contract
	 */
	static final List<String> INCLUDES = List.of("tags", "template", "folder", "languageVariants", "versions",
			"translationStatus", "constructs");

	/**
	 * The page, as {@link PageInfo} plus its tags
	 * @param info page metadata
	 * @param tags tags by name, if requested
	 */
	@JsonInclude(Include.NON_NULL)
	public record PageWithTags(@JsonUnwrapped PageInfo info, Map<String, TagInfo> tags) {
	}

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param page the page
	 * @param template the page's template, if requested
	 * @param folder the page's folder, if requested
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(PageWithTags page, ObjectRef template, ObjectRef folder) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to load the page in.", "minimum", 1));
		properties.put(ARG_INCLUDE, schema("array", "What to load with the page.", "maxItems", INCLUDES.size(),
				"uniqueItems", true, "items", schema("string", null, "enum", INCLUDES), "default", List.of("tags")));

		return Tool.builder().name("get_page").title("Get page")
				.description(description())
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Args.id(arguments, ARG_ID);
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
		List<String> include = Args.enumList(arguments, ARG_INCLUDE, INCLUDES, List.of("tags"));

		// the folder is always loaded, the page's ref takes its node from it
		PageLoadResponse response = RestPermissions.guard(PageResource.class, new PageResourceImpl()).load(
				Integer.toString(id), false, include.contains("template"), true, include.contains("languageVariants"),
				false, false, include.contains("translationStatus"), include.contains("versions"), false,
				include.contains("constructs"), nodeId, null);
		requireOk(response, "The page %d could not be loaded".formatted(id));
		Page page = response.getPage();

		boolean lookup = include.contains("tags") && !include.contains("constructs") && page.getTags() != null;
		Map<Integer, String> keywords = lookup ? TagInfo.constructKeywords(page.getTags().values()) : Map.of();
		return result(page, include, keywords);
	}

	/**
	 * Build the result from the loaded page
	 * @param page REST page
	 * @param include requested includes
	 * @param keywords construct keywords by construct ID
	 * @return result
	 */
	static Result result(Page page, List<String> include, Map<Integer, String> keywords) {
		Map<String, TagInfo> tags = null;
		if (include.contains("tags") && page.getTags() != null) {
			tags = new LinkedHashMap<>();
			for (Map.Entry<String, Tag> entry : page.getTags().entrySet()) {
				tags.put(entry.getKey(), TagInfo.of(entry.getValue(), keywords, include.contains("constructs")));
			}
		}
		PageInfo info = PageInfo.of(page);
		ObjectRef template = include.contains("template") && page.getTemplate() != null
				? ObjectRef.forTemplate(page.getTemplate(), info.ref().nodeId())
				: null;
		ObjectRef folder = include.contains("folder") && page.getFolder() != null
				? ObjectRef.forFolder(page.getFolder())
				: null;
		return new Result(new PageWithTags(info, tags), template, folder);
	}

	/**
	 * Get the tool description
	 * @return description
	 */
	private static String description() {
		return "Loads one page: properties, and with include tags, its content blocks and their values. This is the "
				+ "authoritative read: use it to verify exact wording before quoting a page, and to read a page back after "
				+ "writing it. It does NOT take an edit lock, so it is safe to call at any time and it will not block a "
				+ "human editor. Use render_preview if you need the rendered HTML rather than the tag values.";
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> outputSchema() {
		Map<String, Object> pageSchema = PageInfo.jsonSchema("The page.");
		Map<String, Object> pageProperties = new LinkedHashMap<>((Map<String, Object>) pageSchema.get("properties"));
		pageProperties.put("tags", schema("object", "Tags keyed by tag name.", "additionalProperties",
				TagInfo.jsonSchema()));
		Map<String, Object> page = new LinkedHashMap<>(pageSchema);
		page.put("properties", pageProperties);

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("page", page);
		properties.put("template", ObjectRef.jsonSchema("The page's template, with include 'template'."));
		properties.put("folder", ObjectRef.jsonSchema("The page's folder, with include 'folder'."));
		return schema("object", null, "properties", properties, "required", List.of("page"));
	}
}
