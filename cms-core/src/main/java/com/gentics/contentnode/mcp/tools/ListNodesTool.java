package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.response.LanguageList;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the nodes (sites/channels) visible to the caller, each with the languages assigned to it.
 *
 * <p>
 * Delegates to {@link NodeResourceImpl#list} (which already filters to nodes the caller has
 * {@code ObjectPermission.view} on) and, per returned node, to {@link NodeResourceImpl#languages}
 * (which re-checks {@code ObjectPermission.view} on that node). No additional permission check is
 * needed in this tool itself, same as {@link PageLoadTool}.
 * </p>
 *
 * <p>
 * Paging is done by fetching all matching nodes unpaged and slicing {@code [from, from + size)}
 * in memory: the tool's input is offset-based, while {@link PagingParameterBean} is page-number
 * based, and node counts are small enough for this to be cheap (see
 * {@code docs/mcp-server-integration.md} §11).
 * </p>
 */
public class ListNodesTool extends AbstractMcpTool {
	private static final String ARG_QUERY = "q";

	private static final String ARG_SIZE = "size";

	private static final String ARG_FROM = "from";

	/**
	 * Maximum length of {@link #ARG_QUERY}
	 */
	public static final int MAX_QUERY_LENGTH = 200;

	/**
	 * Default for {@link #ARG_SIZE}
	 */
	public static final int DEFAULT_SIZE = 25;

	/**
	 * Minimum for {@link #ARG_SIZE}
	 */
	public static final int MIN_SIZE = 1;

	/**
	 * Maximum for {@link #ARG_SIZE}
	 */
	public static final int MAX_SIZE = 200;

	/**
	 * Default for {@link #ARG_FROM}
	 */
	public static final int DEFAULT_FROM = 0;

	/**
	 * Minimum for {@link #ARG_FROM}
	 */
	public static final int MIN_FROM = 0;

	/**
	 * Maximum for {@link #ARG_FROM}
	 */
	public static final int MAX_FROM = 10000;

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_QUERY, schema("string", "Optional case-insensitive filter on the node's ID or name.",
				"maxLength", MAX_QUERY_LENGTH));
		properties.put(ARG_SIZE, schema("integer", "Maximum number of nodes to return.", "minimum", MIN_SIZE,
				"maximum", MAX_SIZE, "default", DEFAULT_SIZE));
		properties.put(ARG_FROM, schema("integer",
				"Offset of the first node to return. Pass the previous result's 'nextFrom' to get the next page.",
				"minimum", MIN_FROM, "maximum", MAX_FROM, "default", DEFAULT_FROM));

		return Tool.builder().name("list_nodes").title("List Nodes")
				.description("Resolve a node (site or channel) by name or ID, and learn which languages a node "
						+ "supports before creating or translating a page. Returns every node the caller can see, "
						+ "each with its host, publish directory, assigned languages, default file/image folders "
						+ "and content repository. Do NOT use this to browse content, use get_folder_tree instead.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		String query = queryArg(arguments.get(ARG_QUERY));
		int size = intArg(arguments, ARG_SIZE, DEFAULT_SIZE, MIN_SIZE, MAX_SIZE);
		int from = intArg(arguments, ARG_FROM, DEFAULT_FROM, MIN_FROM, MAX_FROM);

		NodeResourceImpl nodeResource = new NodeResourceImpl();

		// fetch all matching nodes unpaged (the default PagingParameterBean has pageSize -1), sorted
		// by the resource's own default ("name"), then slice
		NodeList nodeList = nodeResource.list(new FilterParameterBean().setQuery(query), new SortParameterBean(),
				new PagingParameterBean(), new PermsParameterBean(), null);

		List<Node> allNodes = nodeList.getItems() != null ? nodeList.getItems() : List.of();
		Slice slice = Slice.of(from, size, allNodes.size());

		List<NodeListItem> items = new ArrayList<>();
		for (Node node : allNodes.subList(slice.fromIndex(), slice.toIndex())) {
			LanguageList languageList = nodeResource.languages(String.valueOf(node.getId()),
					new FilterParameterBean(), new PagingParameterBean());
			List<LanguageInfo> languages = languageList.getItems() == null ? List.of()
					: languageList.getItems().stream().map(l -> new LanguageInfo(l.getId(), l.getCode(), l.getName()))
							.toList();

			items.add(new NodeListItem(ObjectRef.forNode(node), node.getHost(), node.getPublishDir(), languages,
					node.getDefaultFileFolderId(), node.getDefaultImageFolderId(), node.getContentRepositoryId()));
		}

		return new ListNodesResult(slice.total(), true, slice.nextFrom(), slice.truncated(), items);
	}

	/**
	 * Bounds of the requested page within a list of {@code total} items.
	 * @param fromIndex inclusive start index (always within {@code [0, total]})
	 * @param toIndex exclusive end index (always within {@code [fromIndex, total]})
	 * @param total total number of items
	 */
	record Slice(int fromIndex, int toIndex, int total) {
		/**
		 * Compute the slice {@code [from, from + size)} of a list of {@code total} items, cut off at
		 * {@code total}
		 * @param from requested offset (non-negative)
		 * @param size requested page size (positive)
		 * @param total total number of items
		 * @return slice
		 */
		static Slice of(int from, int size, int total) {
			int fromIndex = Math.min(from, total);
			// long arithmetic, so from + size cannot overflow
			int toIndex = (int) Math.min((long) fromIndex + size, total);
			return new Slice(fromIndex, toIndex, total);
		}

		/**
		 * Whether there are more items after this slice
		 * @return true iff truncated
		 */
		boolean truncated() {
			return toIndex < total;
		}

		/**
		 * Offset to request the next page with, if there is one
		 * @return offset of the next page, or null
		 */
		Integer nextFrom() {
			return truncated() ? toIndex : null;
		}
	}

	/**
	 * Result of the tool
	 * @param total total number of matching nodes
	 * @param totalIsExact whether total is an exact count (always true, all nodes are counted)
	 * @param nextFrom offset of the next page, if truncated
	 * @param truncated whether there are more nodes after this page
	 * @param items nodes in this page
	 */
	@JsonInclude(Include.NON_NULL)
	record ListNodesResult(Integer total, Boolean totalIsExact, Integer nextFrom, Boolean truncated,
			List<NodeListItem> items) {
	}

	/**
	 * A single node
	 * @param ref reference to the node
	 * @param host hostname of the node
	 * @param publishDir publish directory of the node
	 * @param languages languages assigned to the node
	 * @param defaultFileFolderId ID of the default folder for files
	 * @param defaultImageFolderId ID of the default folder for images
	 * @param contentRepositoryId ID of the assigned content repository
	 */
	@JsonInclude(Include.NON_NULL)
	record NodeListItem(ObjectRef ref, String host, String publishDir, List<LanguageInfo> languages,
			Integer defaultFileFolderId, Integer defaultImageFolderId, Integer contentRepositoryId) {
	}

	/**
	 * A language assigned to a node
	 * @param id language ID
	 * @param code language code
	 * @param name language name
	 */
	@JsonInclude(Include.NON_NULL)
	record LanguageInfo(Integer id, String code, String name) {
	}

	/**
	 * Normalize the query argument: null or blank means "no filter"
	 * @param raw raw argument value
	 * @return query or null
	 */
	private static String queryArg(Object raw) {
		if (raw == null) {
			return null;
		}
		String query = String.valueOf(raw);
		if (query.isBlank()) {
			return null;
		}
		if (query.length() > MAX_QUERY_LENGTH) {
			throw new IllegalArgumentException(
					"Argument '%s' must not be longer than %d characters".formatted(ARG_QUERY, MAX_QUERY_LENGTH));
		}
		return query;
	}

	/**
	 * Get an integer argument, falling back to the default if missing and clamping it to
	 * {@code [min, max]} (the input schema already constrains it, this is purely defensive)
	 * @param arguments arguments
	 * @param name argument name
	 * @param defaultValue default value
	 * @param min minimum value
	 * @param max maximum value
	 * @return value
	 */
	static int intArg(Map<String, Object> arguments, String name, int defaultValue, int min, int max) {
		Object raw = arguments.get(name);
		int value;
		if (raw == null) {
			value = defaultValue;
		} else if (raw instanceof Number number) {
			value = number.intValue();
		} else {
			try {
				value = Integer.parseInt(String.valueOf(raw).trim());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Argument '%s' must be an integer".formatted(name));
			}
		}
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Build a property schema
	 * @param type JSON type
	 * @param description description
	 * @param keyValues additional constraint keywords and their values, alternating
	 * @return schema
	 */
	private static Map<String, Object> schema(String type, String description, Object... keyValues) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", type);
		if (description != null) {
			schema.put("description", description);
		}
		for (int i = 0; i + 1 < keyValues.length; i += 2) {
			schema.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
		}
		return schema;
	}

	/**
	 * Build the output schema. {@code ref} is described by its fields but not validated further,
	 * consistent with the (non-{@code $ref}) schemas elsewhere in the MCP tool layer. The SDK
	 * validates the structured result against this schema, which is why the result records omit
	 * null values ({@code "type": "integer"} does not accept {@code null}).
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> refProperties = new LinkedHashMap<>();
		refProperties.put("type", schema("string", null));
		refProperties.put("id", schema("integer", null));
		refProperties.put("globalId", schema("string", null));
		refProperties.put("nodeId", schema("integer", null));
		refProperties.put("name", schema("string", null));
		refProperties.put("path", schema("string", null));
		refProperties.put("language", schema("string", null));
		refProperties.put("niceUrl", schema("string", null));
		refProperties.put("url", schema("string", null));

		Map<String, Object> languageProperties = new LinkedHashMap<>();
		languageProperties.put("id", schema("integer", null));
		languageProperties.put("code", schema("string", null));
		languageProperties.put("name", schema("string", null));

		Map<String, Object> itemProperties = new LinkedHashMap<>();
		itemProperties.put("ref", schema("object", "Reference to the node.", "properties", refProperties));
		itemProperties.put("host", schema("string", null));
		itemProperties.put("publishDir", schema("string", null));
		itemProperties.put("languages", schema("array", "Languages assigned to the node.", "items",
				schema("object", null, "properties", languageProperties)));
		itemProperties.put("defaultFileFolderId", schema("integer", null));
		itemProperties.put("defaultImageFolderId", schema("integer", null));
		itemProperties.put("contentRepositoryId", schema("integer", null));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("total", schema("integer", "Total number of matching nodes."));
		properties.put("totalIsExact", schema("boolean", null));
		properties.put("nextFrom", schema("integer", "Offset of the next page, if 'truncated'."));
		properties.put("truncated", schema("boolean", "Whether more nodes exist after this page."));
		properties.put("items", schema("array", null, "items", schema("object", null, "properties", itemProperties)));

		Map<String, Object> outputSchema = new LinkedHashMap<>();
		outputSchema.put("type", "object");
		outputSchema.put("properties", properties);
		outputSchema.put("required", List.of("items"));
		return outputSchema;
	}
}
