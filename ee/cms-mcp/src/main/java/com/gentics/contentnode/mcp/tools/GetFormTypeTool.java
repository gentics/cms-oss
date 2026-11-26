package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationListResponse;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;
import com.gentics.contentnode.rest.resource.FormResource;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterFormTypeConfigurationsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Reads one form type's configuration on a node. Delegates to {@link FormResourceImpl#listFormTypeConfigurations},
 * limited to the node's types, and {@link FormResourceImpl#getFormTypeconfiguration}.
 */
public class GetFormTypeTool extends AbstractMcpTool {
	static final String ARG_TYPE = "type";

	/**
	 * Unset members are left out, so the result fits the output schema
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper().setDefaultPropertyInclusion(Include.NON_NULL);

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(Forms.ARG_NODE_ID, schema("integer", "ID of the node.", "minimum", 1));
		properties.put(ARG_TYPE, schema("string", "The form type, see list_form_types.", "minLength", 1));

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("type", schema("string", null));
		output.put("nameI18n", schema("object", null));
		output.put("external", schema("boolean", null));
		output.put("pluginName", schema("string", null));
		output.put("controls", schema("object", "Control key -> its configuration (labelI18n, settings, aggregate)."));
		output.put("blocks", schema("object", "Block key -> its configuration (labelI18n, settings, container)."));
		output.put("flows",
				schema("array", "The submission flows a form of this type may use; a form's flowId names one."));
		output.put("adminEmailTemplateOptions", schema("array", null));
		output.put("captchaOptions", schema("array", null));
		output.put("submitOnce", schema("boolean", null));

		return Tool.builder().name("get_form_type").title("Get form type")
				.description("Reads one form type's configuration on a node: the controls and blocks a form of this "
						+ "type may contain, each with its settings (select options included), the form-level options "
						+ "such as adminEmailTemplateOptions and captchaOptions, and whether the type is external. "
						+ "Build a form's definition only from what this returns; create_form and update_form refuse "
						+ "anything else.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(Forms.ARG_NODE_ID, ARG_TYPE)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required",
						List.of("type", "external", "controls")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int nodeId = Args.id(arguments, Forms.ARG_NODE_ID);
		String type = stringArg(arguments, ARG_TYPE, 1, 255);
		if (type == null) {
			throw new IllegalArgumentException("Argument '%s' is required".formatted(ARG_TYPE));
		}

		FormResource resource = Forms.resource();
		FilterFormTypeConfigurationsParameterBean filter = new FilterFormTypeConfigurationsParameterBean();
		filter.nodeId = Integer.toString(nodeId);
		FormTypeConfigurationListResponse offered = resource.listFormTypeConfigurations(filter,
				new SortParameterBean(), new PagingParameterBean());
		requireOk(offered, "The form types of node %d could not be listed".formatted(nodeId));
		requireOffered(offered.getItems(), type, nodeId);

		return result(Forms.type(resource, type));
	}

	/**
	 * Refuse a type the node does not offer
	 * @param offered the node's types
	 * @param type form type
	 * @param nodeId node ID
	 * @throws EntityNotFoundException if the node does not offer it
	 */
	static void requireOffered(List<FormTypeConfigurationModel> offered, String type, int nodeId)
			throws EntityNotFoundException {
		if (offered == null || offered.stream().noneMatch(model -> type.equals(model.type))) {
			throw new EntityNotFoundException("Node %d does not offer the form type '%s'".formatted(nodeId, type));
		}
	}

	/**
	 * Convert a form type's configuration for the result
	 * @param model configuration
	 * @return configuration as JSON, without unset members
	 */
	@SuppressWarnings("unchecked")
	static Map<String, Object> result(FormTypeConfigurationModel model) {
		return MAPPER.convertValue(model, Map.class);
	}
}
