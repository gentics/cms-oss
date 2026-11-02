package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.LanguageInfo;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.model.NodeInfo;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.resource.NodeResource;
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
 * based, and node counts are small enough for this to be cheap (see {@link Slice} and
 * {@code docs/mcp-server-integration.md} §11).
 * </p>
 */
public class ListNodesTool extends AbstractMcpTool {
	/**
	 * Maximum length of the query ({@link ListArgs#ARG_QUERY})
	 */
	public static final int MAX_QUERY_LENGTH = 200;

	/**
	 * Default page size ({@link ListArgs#ARG_SIZE})
	 */
	public static final int DEFAULT_SIZE = 25;

	/**
	 * Maximum page size ({@link ListArgs#ARG_SIZE})
	 */
	public static final int MAX_SIZE = 200;

	/**
	 * Maximum offset ({@link ListArgs#ARG_FROM})
	 */
	public static final int MAX_FROM = 10000;

	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(MAX_QUERY_LENGTH, DEFAULT_SIZE, MAX_SIZE, MAX_FROM);

	@Override
	public Tool tool() {
		Map<String, Object> properties = ListArgs.schemaProperties(
				"Optional case-insensitive filter on the node's ID or name.", "node", "nodes", LIMITS);

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
		ListArgs args = ListArgs.of(arguments, LIMITS);

		NodeResource nodeResource = RestPermissions.guard(NodeResource.class, new NodeResourceImpl());

		// fetch all matching nodes unpaged (the default PagingParameterBean has pageSize -1), sorted
		// by the resource's own default ("name"), then slice
		List<Node> allNodes = ListResponses.items(nodeResource.list(new FilterParameterBean().setQuery(args.query()),
				new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean(), null));
		Slice slice = args.slice(allNodes.size());

		List<NodeInfo> items = new ArrayList<>();
		for (Node node : slice.apply(allNodes)) {
			List<LanguageInfo> languages = ListResponses.items(nodeResource.languages(String.valueOf(node.getId()),
					new FilterParameterBean(), new PagingParameterBean())).stream().map(LanguageInfo::of).toList();
			items.add(NodeInfo.of(node, languages));
		}

		return ListResult.of(slice, items);
	}

	/**
	 * Build the output schema. The SDK validates the structured result against it, which is why
	 * the result records omit null values ({@code "type": "integer"} does not accept {@code null}).
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		return ListResult.jsonSchema(NodeInfo.jsonSchema(), "nodes");
	}
}
