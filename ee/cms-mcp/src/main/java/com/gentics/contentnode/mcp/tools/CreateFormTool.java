package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpArgs.intArg;
import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.ExternalFormException;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Creates one offline CMS form. Delegates to {@link FormResourceImpl#create}, which checks the permissions. The
 * external ID is deliberately not exposed: with one, the CMS turns a create into an update.
 */
public class CreateFormTool extends AbstractMcpTool {
	static final String ARG_FOLDER_ID = "folderId";

	static final String ARG_FORM_TYPE = "formType";

	/**
	 * Result of the tool
	 * @param ref reference to the new form
	 */
	public record Result(ObjectRef ref) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_FOLDER_ID, schema("integer", "Target folder. Omit for the node's default form folder.",
				"minimum", 1));
		properties.put(Forms.ARG_NODE_ID, GetFormTool.nodeIdSchema());
		properties.put(ARG_FORM_TYPE, schema("string", "The form type, see list_form_types."));
		properties.putAll(Forms.fieldProperties());

		return Tool.builder().name("create_form").title("Create form")
				.description("Creates one offline CMS form in a folder, or in the node's default form folder when "
						+ "folderId is left out. formType must not be an external type: such a form is refused with "
						+ "external-form-read-only, because external forms come from their external system.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(Forms.ARG_NAME, ARG_FORM_TYPE, Forms.ARG_LANGUAGES))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", Map.of("ref", ObjectRef.jsonSchema("The new form.")),
						"required", List.of("ref")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Integer nodeId = Forms.nodeId(arguments);
		String formType = stringArg(arguments, ARG_FORM_TYPE, 1, 255);
		FormModel form = Forms.model(arguments);
		if (form.getName() == null || formType == null || form.getLanguages() == null) {
			throw new IllegalArgumentException("Arguments '%s', '%s' and '%s' are required".formatted(Forms.ARG_NAME,
					ARG_FORM_TYPE, Forms.ARG_LANGUAGES));
		}
		form.setFormType(formType);
		form.setFolderId(intArg(arguments, ARG_FOLDER_ID, 1, Integer.MAX_VALUE));
		if (Forms.isExternal(formType)) {
			throw new ExternalFormException("Form type '%s' is external: such forms come from their external system."
					.formatted(formType));
		}

		FormResponse response = Forms.resource().create(form, Forms.string(nodeId));
		requireOk(response, "The form could not be created");
		releaseLock(Form.class, Integer.toString(response.getItem().getId()));
		return new Result(Forms.ref(response.getItem(), nodeId));
	}
}
