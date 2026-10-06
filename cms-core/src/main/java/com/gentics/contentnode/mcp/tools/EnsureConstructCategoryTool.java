package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ConstructInfo.Category;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.PartSpecs;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.ConstructCategory;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.ConstructCategoryLoadResponse;
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
 * Returns the construct category with a given name, or creates it. Looks for a category the caller can view whose name
 * matches in one of the given languages, ignoring case ({@link ConstructResourceImpl#listCategories}), and creates one
 * otherwise ({@link ConstructResourceImpl#createCategory}). The delegates check no class permission, so the tool
 * checks the view permission on construct categories, which is what the CMS UI needs to manage them.
 */
public class EnsureConstructCategoryTool extends AbstractMcpTool {
	static final String NAME = "ensure_construct_category";

	static final String ARG_NAME = "name";

	static final String ARG_SORT_ORDER = "sortOrder";

	/**
	 * Result of the tool
	 * @param category category
	 * @param created whether the category was created by this call
	 */
	public record Result(Category category, boolean created) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_NAME, PartSpecs.i18nInputSchema(255));
		properties.put(ARG_SORT_ORDER, schema("integer", "Sort order of a new category. Defaults to the end.",
				"minimum", 0, "maximum", 10000));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		Map<String, Object> outputProperties = new LinkedHashMap<>();
		outputProperties.put("category", Category.jsonSchema());
		outputProperties.put("created", schema("boolean", "Whether the category was created by this call."));

		return Tool.builder().name(NAME).title("Ensure construct category")
				.description("Returns the construct category with the given name, creating it if it does not exist. "
						+ "A category whose name matches in any of the given languages, ignoring case, is reused. "
						+ "Needed because create_construct takes a category id. Call list_construct_categories first "
						+ "if you would rather reuse an existing category under a different name.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_NAME))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", outputProperties, "required", List.of("category",
						"created")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Map<String, String> name = Args.i18nMap(arguments, ARG_NAME, 255, Args.uiLanguages());
		if (name == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_NAME));
		}
		Integer sortOrder = intArg(arguments, ARG_SORT_ORDER, 0, 10000);
		Writes.logIdempotencyKey(NAME, 0, arguments);

		try (Trx trx = ContentNodeHelper.trx()) {
			if (!trx.getTransaction().getPermHandler().canView(null,
					com.gentics.contentnode.object.ConstructCategory.class, null)) {
				throw new InsufficientPrivilegesException("Missing permission to view construct categories", null,
						null, com.gentics.contentnode.object.ConstructCategory.TYPE_CONSTRUCT_CATEGORIES, 0,
						PermType.read);
			}
			trx.success();
		}

		ConstructResource resource = RestPermissions.guard(ConstructResource.class, new ConstructResourceImpl());
		ConstructCategory existing = find(ListResponses.items(resource.listCategories(new SortParameterBean(),
				new FilterParameterBean(), new PagingParameterBean(), new EmbedParameterBean(),
				new ConstructCategoryParameterBean())), name);
		if (existing != null) {
			return new Result(Category.of(existing, null), false);
		}

		ConstructCategory category = new ConstructCategory();
		category.setNameI18n(name);
		category.setSortOrder(sortOrder);
		ConstructCategoryLoadResponse response = resource.createCategory(category);
		requireOk(response, "The construct category was not created");
		return new Result(Category.of(response.getConstructCategory(), null), true);
	}

	/**
	 * Find the first category whose name matches one of the given names in the same language, ignoring case
	 * @param categories categories
	 * @param name names per language
	 * @return category, null if none matches
	 */
	static ConstructCategory find(List<ConstructCategory> categories, Map<String, String> name) {
		for (ConstructCategory category : categories) {
			Map<String, String> names = category.getNameI18n() != null ? category.getNameI18n() : Map.of();
			for (Map.Entry<String, String> entry : name.entrySet()) {
				String existing = names.get(entry.getKey());
				if (existing != null && !entry.getValue().isBlank() && existing.equalsIgnoreCase(entry.getValue())) {
					return category;
				}
			}
		}
		return null;
	}
}
