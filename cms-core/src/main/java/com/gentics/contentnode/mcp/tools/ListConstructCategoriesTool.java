package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ConstructInfo.Category;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.ConstructCategory;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.ConstructCategoryParameterBean;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the construct categories ({@link ConstructResourceImpl#listCategories}, which lists the categories the caller
 * can view). The categories are fetched unpaged and sliced; the constructs of the slice are counted in an own
 * transaction.
 */
public class ListConstructCategoriesTool extends AbstractMcpTool {
	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	@Override
	public Tool tool() {
		return Tool.builder().name("list_construct_categories").title("List construct categories")
				.description("Lists the construct categories, which group tag types in the editor's insert menu. Use "
						+ "it to pick an existing category before falling back to ensure_construct_category.")
				.inputSchema(JsonSchema.builder().type("object")
						.properties(ListArgs.schemaProperties("Optional case-insensitive filter on the name.",
								"category", "categories", LIMITS))
						.required(List.of()).additionalProperties(false).build())
				.outputSchema(ListResult.jsonSchema(Category.jsonSchema(), "categories"))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		ListArgs args = ListArgs.of(arguments, LIMITS);

		List<ConstructCategory> categories = ListResponses.items(RestPermissions.guard(ConstructResource.class,
				new ConstructResourceImpl()).listCategories(new SortParameterBean(), new FilterParameterBean()
						.setQuery(args.query()), new PagingParameterBean(), new EmbedParameterBean(),
						new ConstructCategoryParameterBean()));
		Slice slice = args.slice(categories.size());

		List<Category> items = new ArrayList<>();
		try (Trx trx = ContentNodeHelper.trx()) {
			for (ConstructCategory category : slice.apply(categories)) {
				com.gentics.contentnode.object.ConstructCategory object = trx.getTransaction()
						.getObject(com.gentics.contentnode.object.ConstructCategory.class, category.getId());
				items.add(Category.of(category, object != null ? object.getConstructs().size() : null));
			}
			trx.success();
		}
		return ListResult.of(slice, items);
	}
}
