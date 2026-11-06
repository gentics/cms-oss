package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationListResponse;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterFormTypeConfigurationsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the form types of a node. Delegates to {@link FormResourceImpl#listFormTypeConfigurations}.
 */
public class ListFormTypesTool extends AbstractMcpTool {
	/**
	 * One form type
	 * @param type form type
	 * @param name display name
	 * @param external whether it is external
	 */
	@JsonInclude(Include.NON_NULL)
	public record Item(String type, String name, boolean external) {
		/**
		 * Build the item of a form type
		 * @param model form type
		 * @return item
		 */
		static Item of(FormTypeConfigurationModel model) {
			Map<String, String> names = model.nameI18n;
			String name = names == null || names.isEmpty() ? null
					: names.getOrDefault("en", names.values().iterator().next());
			return new Item(model.type, name, model.external);
		}
	}

	/**
	 * Result of the tool
	 * @param items form types
	 */
	public record Result(List<Item> items) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(Forms.ARG_NODE_ID, schema("integer", "ID of the node.", "minimum", 1));

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("type", schema("string", null));
		item.put("name", schema("string", null));
		item.put("external", schema("boolean", null));

		return Tool.builder().name("list_form_types").title("List form types")
				.description("Lists the form types a node offers, each saying whether it is external. create_form "
						+ "needs one that is not.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(Forms.ARG_NODE_ID)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", Map.of("items", schema("array", null, "items",
						schema("object", null, "properties", item, "required", List.of("type", "external")))),
						"required", List.of("items")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int nodeId = Args.id(arguments, Forms.ARG_NODE_ID);

		FilterFormTypeConfigurationsParameterBean filter = new FilterFormTypeConfigurationsParameterBean();
		filter.nodeId = Integer.toString(nodeId);
		FormTypeConfigurationListResponse response = Forms.resource().listFormTypeConfigurations(filter,
				new SortParameterBean(), new PagingParameterBean());
		requireOk(response, "The form types of node %d could not be listed".formatted(nodeId));

		List<Item> items = new ArrayList<>();
		for (FormTypeConfigurationModel model : response.getItems()) {
			items.add(Item.of(model));
		}
		return new Result(items);
	}
}
