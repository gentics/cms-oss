package com.gentics.contentnode.mcp.tools;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ConstructInfo;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.ConstructParameterBean;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists constructs: those of a node, those usable in a page, or all of them. Delegates to
 * {@link ConstructResourceImpl#list}, which checks the view permission on the node or the page, and otherwise lists
 * the constructs the caller can view. The constructs are fetched unpaged, with their category, and sliced.
 */
public class ListConstructsTool extends AbstractMcpTool {
	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_CATEGORY_ID = "categoryId";

	static final String ARG_PART_TYPE_IDS = "partTypeIds";

	static final String ARG_CHANGEABLE = "changeable";

	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	@Override
	public Tool tool() {
		Map<String, Object> properties = ListArgs.schemaProperties(
				"Optional case-insensitive filter on keyword, name and description.", "construct", "constructs",
				LIMITS);
		properties.put(ARG_NODE_ID, schema("integer", "Only the constructs assigned to this node.", "minimum", 1));
		properties.put(ARG_PAGE_ID, schema("integer", "Only the constructs usable in this page.", "minimum", 1));
		properties.put(ARG_CATEGORY_ID, schema("integer", "Only the constructs of this category.", "minimum", 1));
		properties.put(ARG_PART_TYPE_IDS, schema("array", "Only constructs with a part of one of these types.",
				"maxItems", 20, "uniqueItems", true, "items", schema("integer", null, "minimum", 1)));
		properties.put(ARG_CHANGEABLE, schema("boolean", "Only constructs the acting user may change (true) or may "
				+ "not change (false)."));

		return Tool.builder().name("list_constructs").title("List constructs")
				.description("Lists the constructs (tag types) available, filtered by node or by the page they could "
						+ "be inserted into. ALWAYS call this before creating a construct, to check that the keyword "
						+ "is free and that no existing construct already does the job, and before building page "
						+ "content, to see what you are allowed to use. Filtering by nodeId or pageId is what tells "
						+ "you availability: a construct that exists in the CMS is not necessarily assigned to the "
						+ "node you are working in.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(ListResult.jsonSchema(ConstructInfo.Summary.jsonSchema(), "constructs"))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		ListArgs args = ListArgs.of(arguments, LIMITS);
		ConstructParameterBean constructFilter = new ConstructParameterBean();
		constructFilter.nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
		constructFilter.pageId = intArg(arguments, ARG_PAGE_ID, 1, Integer.MAX_VALUE);
		constructFilter.categoryId = intArg(arguments, ARG_CATEGORY_ID, 1, Integer.MAX_VALUE);
		constructFilter.partTypeId = Args.distinctIntList(arguments, ARG_PART_TYPE_IDS, 20);
		constructFilter.changeable = arguments.containsKey(ARG_CHANGEABLE) ? booleanArg(arguments, ARG_CHANGEABLE,
				false) : null;

		List<Construct> constructs = ListResponses.items(RestPermissions.guard(ConstructResource.class,
				new ConstructResourceImpl()).list(new FilterParameterBean().setQuery(args.query()),
						new SortParameterBean().setSort("keyword"), new PagingParameterBean(), constructFilter,
						new PermsParameterBean(), new EmbedParameterBean().withEmbed("category")));
		return result(constructs, args);
	}

	/**
	 * Map and slice the constructs
	 * @param constructs REST constructs
	 * @param args list arguments
	 * @return result
	 */
	static ListResult<ConstructInfo.Summary> result(List<Construct> constructs, ListArgs args) {
		Slice slice = args.slice(constructs.size());
		return ListResult.of(slice, slice.apply(constructs).stream().map(ConstructInfo.Summary::of).toList());
	}
}
