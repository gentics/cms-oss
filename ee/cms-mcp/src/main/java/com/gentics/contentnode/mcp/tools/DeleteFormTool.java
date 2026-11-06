package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.resource.FormResource;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Moves a form to the wastebin. Refuses when the form's node has no wastebin, because the CMS would then delete the
 * form permanently. Delegates to {@link FormResourceImpl#delete}, which checks the delete permission.
 */
public class DeleteFormTool extends AbstractMcpTool {
	/**
	 * Result of the tool
	 * @param ref reference to the deleted form
	 * @param deleted always true
	 * @param restorable always true, the form is in the wastebin
	 */
	public record Result(ObjectRef ref, boolean deleted, boolean restorable) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(Forms.ARG_FORM_ID, schema("integer", "ID of the form.", "minimum", 1));
		properties.put(Forms.ARG_NODE_ID, GetFormTool.nodeIdSchema());

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("ref", ObjectRef.jsonSchema("The deleted form."));
		output.put("deleted", schema("boolean", "Always true."));
		output.put("restorable", schema("boolean", "Always true: the form is in the wastebin."));

		return Tool.builder().name("delete_form").title("Delete form to wastebin")
				.description("Moves a form to the wastebin, where it remains restorable by a human in the CMS. Refused "
						+ "with invalid-data on a node without wastebin, so this never deletes permanently. Confirm "
						+ "with the user before calling it, and check get_related with uses_form first: pages may "
						+ "embed it. Refused with external-form-read-only for an external form.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(Forms.ARG_FORM_ID)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("ref", "deleted")))
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
		ObjectRef ref = Forms.ref(loaded.getItem(), nodeId);

		try (Trx trx = ContentNodeHelper.trx()) {
			Form form = trx.getTransaction().getObject(Form.class, id);
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.WASTEBIN, form.getFolder().getOwningNode())) {
				throw new IllegalArgumentException("The node of form %d has no wastebin, so the form would be deleted "
						.formatted(id) + "permanently. delete_form only moves forms to the wastebin.");
			}
			trx.success();
		}

		requireOk(resource.delete(Integer.toString(id), Forms.string(nodeId), null),
				"Form %d was not deleted".formatted(id));
		return new Result(ref, true, true);
	}
}
