package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.resource.FormResource;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Puts a form online. Delegates to {@link FormResourceImpl#publish}, which checks the publish permission.
 */
public class PublishFormTool extends AbstractMcpTool {
	/**
	 * Result of the tool
	 * @param ref reference to the form
	 * @param online whether the form is online
	 */
	public record Result(ObjectRef ref, boolean online) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(Forms.ARG_FORM_ID, schema("integer", "ID of the form.", "minimum", 1));
		properties.put(Forms.ARG_NODE_ID, GetFormTool.nodeIdSchema());

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("ref", ObjectRef.jsonSchema("The form."));
		output.put("online", schema("boolean", null));

		return Tool.builder().name("publish_form").title("Publish form")
				.description("Puts a form online. Called by the harness during a plan's publish, never by the model, "
						+ "as for publish_page. Refused with external-form-read-only for an external form.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(Forms.ARG_FORM_ID)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("ref", "online")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Forms.formId(arguments);
		Integer nodeId = Forms.nodeId(arguments);

		FormResource resource = Forms.resource();
		FormResponse loaded = Forms.load(resource, id, Forms.string(nodeId));
		requireOk(loaded, "The form %d could not be loaded".formatted(id));
		Forms.requireWritable(loaded.getItem());

		FormResponse response = resource.publish(Integer.toString(id), Forms.string(nodeId), 0, false);
		requireOk(response, "Form %d was not published".formatted(id));
		return new Result(Forms.ref(response.getItem(), nodeId), response.getItem().isOnline());
	}
}
