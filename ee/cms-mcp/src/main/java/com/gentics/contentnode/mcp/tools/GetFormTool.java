package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Reads one form. Delegates to {@link FormResourceImpl#get}, which checks the view permission.
 */
public class GetFormTool extends AbstractMcpTool {
	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param ref reference to the form
	 * @param name name
	 * @param description description
	 * @param formType form type
	 * @param external whether the form type is external
	 * @param languages language codes
	 * @param online whether the form is online
	 * @param modified whether the form has changes that are not online
	 * @param locked whether the form is locked
	 * @param data definition
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ObjectRef ref, String name, String description, String formType, boolean external,
			List<String> languages, boolean online, boolean modified, boolean locked, Object data) {
	}

	/**
	 * Build the node ID schema
	 * @return schema
	 */
	static Map<String, Object> nodeIdSchema() {
		return schema("integer", "Node (channel) of the form.", "minimum", 1);
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(Forms.ARG_FORM_ID, schema("integer", "ID of the form.", "minimum", 1));
		properties.put(Forms.ARG_NODE_ID, nodeIdSchema());

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("ref", ObjectRef.jsonSchema("The form."));
		output.put("name", schema("string", null));
		output.put("description", schema("string", null));
		output.put("formType", schema("string", null));
		output.put("external", schema("boolean",
				"The form's type is an external form type. Every write to such a form is refused."));
		output.put("languages", schema("array", null, "items", schema("string", null)));
		output.put("online", schema("boolean", null));
		output.put("modified", schema("boolean", null));
		output.put("locked", schema("boolean", null));
		output.put("data", Map.of("description", "The form's definition, in the shape its form type uses."));

		return Tool.builder().name("get_form").title("Get form")
				.description("Reads one form: name, type, languages, publish state, whether it is external, and its "
						+ "definition. Forms, CMS and external alike, are found with search_content and types [form].")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(Forms.ARG_FORM_ID)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required",
						List.of("ref", "formType", "external")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Forms.formId(arguments);
		Integer nodeId = Forms.nodeId(arguments);

		FormResponse response = Forms.load(Forms.resource(), id, Forms.string(nodeId));
		requireOk(response, "The form %d could not be loaded".formatted(id));
		FormModel form = response.getItem();
		return new Result(Forms.ref(form, nodeId), form.getName(), form.getDescription(), form.getFormType(),
				Forms.isExternal(form.getFormType()), form.getLanguages(), form.isOnline(), form.isModified(),
				form.isLocked(), Forms.data(form.getData()));
	}
}
