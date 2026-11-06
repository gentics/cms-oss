package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.resource.FormResource;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Changes one CMS form. Delegates to {@link FormResourceImpl#update}, which checks the edit permission and leaves the
 * form locked, so the lock is released here.
 */
public class UpdateFormTool extends AbstractMcpTool {
	/**
	 * Result of the tool
	 * @param ref reference to the form
	 * @param changed names of the fields given
	 */
	public record Result(ObjectRef ref, List<String> changed) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(Forms.ARG_FORM_ID, schema("integer", "ID of the form.", "minimum", 1));
		properties.put(Forms.ARG_NODE_ID, GetFormTool.nodeIdSchema());
		properties.putAll(Forms.fieldProperties());

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("ref", ObjectRef.jsonSchema("The form."));
		output.put("changed", schema("array", "The fields given.", "items", schema("string", null)));

		return Tool.builder().name("update_form").title("Update form")
				.description("Changes one form's name, description, languages or definition, and completes its own "
						+ "lock cycle. A published form stays online in its published version until publish_form. "
						+ "Refused with external-form-read-only for an external form.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(Forms.ARG_FORM_ID)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("ref")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Forms.formId(arguments);
		Integer nodeId = Forms.nodeId(arguments);
		FormModel form = Forms.model(arguments);
		List<String> changed = changed(arguments);
		if (changed.isEmpty()) {
			throw new IllegalArgumentException("Nothing to change");
		}

		FormResource resource = Forms.resource();
		FormResponse loaded = Forms.load(resource, id, Forms.string(nodeId));
		requireOk(loaded, "The form %d could not be loaded".formatted(id));
		Forms.requireWritable(loaded.getItem());

		String idString = Integer.toString(id);
		FormResponse response = (FormResponse) saveOrReleaseLock(Form.class, idString,
				"The form %d was not saved".formatted(id), () -> resource.update(idString, Forms.string(nodeId), form));
		releaseLock(Form.class, idString);
		return new Result(Forms.ref(response.getItem(), nodeId), changed);
	}

	/**
	 * Get the names of the fields given
	 * @param arguments arguments
	 * @return names
	 */
	static List<String> changed(Map<String, Object> arguments) {
		List<String> changed = new ArrayList<>();
		for (String field : List.of(Forms.ARG_NAME, Forms.ARG_DESCRIPTION, Forms.ARG_LANGUAGES, Forms.ARG_DATA)) {
			if (arguments.get(field) != null) {
				changed.add(field);
			}
		}
		return changed;
	}
}
