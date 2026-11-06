package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.mcp.McpArgs;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.ExternalFormException;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.resource.FormResource;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;

/**
 * Shared parts of the Forms tools.
 */
final class Forms {
	static final String ARG_FORM_ID = "formId";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_NAME = "name";

	static final String ARG_DESCRIPTION = "description";

	static final String ARG_LANGUAGES = "languages";

	static final String ARG_DATA = "data";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private Forms() {
	}

	/**
	 * Get the guarded form resource
	 * @return form resource
	 */
	static FormResource resource() {
		return RestPermissions.guard(FormResource.class, new FormResourceImpl());
	}

	/**
	 * Check whether a form type is external
	 * @param formType form type
	 * @return true for an external form type
	 */
	static boolean isExternal(String formType) {
		Map<String, FormTypeConfiguration> config = FormConfigProvider.getConfig();
		FormTypeConfiguration type = config != null && formType != null ? config.get(formType) : null;
		return type != null && type.external;
	}

	/**
	 * Refuse a write to a form of an external type
	 * @param form form
	 * @throws ExternalFormException for an external form
	 */
	static void requireWritable(FormModel form) {
		if (isExternal(form.getFormType())) {
			throw new ExternalFormException("Form %d is an external form, which comes from its external system."
					.formatted(form.getId()));
		}
	}

	/**
	 * Load a form, checking the delegate's view permission
	 * @param resource form resource
	 * @param id form ID
	 * @param nodeId node ID, may be null
	 * @return form
	 * @throws Exception if the form cannot be loaded
	 */
	static FormResponse load(FormResource resource, int id, String nodeId) throws Exception {
		return resource.get(Integer.toString(id), nodeId, null);
	}

	/**
	 * Build the reference to a form
	 * @param form form
	 * @param nodeId node ID, may be null
	 * @return reference
	 */
	static ObjectRef ref(FormModel form, Integer nodeId) {
		return new ObjectRef(ObjectRef.Type.FORM, form.getId(), form.getGlobalId(), nodeId, form.getName(), null, null,
				null, null);
	}

	/**
	 * Get the form ID argument
	 * @param arguments arguments
	 * @return form ID
	 */
	static int formId(Map<String, Object> arguments) {
		return Args.id(arguments, ARG_FORM_ID);
	}

	/**
	 * Get the node ID argument
	 * @param arguments arguments
	 * @return node ID, may be null
	 */
	static Integer nodeId(Map<String, Object> arguments) {
		return McpArgs.intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
	}

	/**
	 * Get a node ID as the delegate takes it
	 * @param nodeId node ID, may be null
	 * @return string, may be null
	 */
	static String string(Integer nodeId) {
		return nodeId != null ? nodeId.toString() : null;
	}

	/**
	 * Copy the fields a create and an update share into a form model
	 * @param arguments arguments
	 * @return form model, and whichever fields were given
	 */
	static FormModel model(Map<String, Object> arguments) {
		FormModel form = new FormModel();
		form.setName(stringArg(arguments, ARG_NAME, 1, 255));
		form.setDescription(stringArg(arguments, ARG_DESCRIPTION, 0, Integer.MAX_VALUE));
		if (arguments.containsKey(ARG_LANGUAGES)) {
			List<String> languages = Args.stringList(arguments, ARG_LANGUAGES, 50, 16);
			if (languages == null || languages.isEmpty()) {
				throw new IllegalArgumentException("Argument '%s' must not be empty".formatted(ARG_LANGUAGES));
			}
			form.setLanguages(languages);
		}
		if (arguments.get(ARG_DATA) != null) {
			form.setData(MAPPER.valueToTree(arguments.get(ARG_DATA)));
		}
		return form;
	}

	/**
	 * Build the schemas of the fields a create and an update share
	 * @return properties
	 */
	static Map<String, Object> fieldProperties() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_NAME, schema("string", "Name of the form.", "minLength", 1, "maxLength", 255));
		properties.put(ARG_DESCRIPTION, schema("string", "Description of the form."));
		properties.put(ARG_LANGUAGES, schema("array", "Language codes of the form.", "minItems", 1, "items",
				schema("string", null)));
		properties.put(ARG_DATA, Map.of("description", "The form's definition, in the shape its form type uses."));
		return properties;
	}

	/**
	 * Convert a form's definition for a result
	 * @param data definition, may be null
	 * @return definition, may be null
	 */
	static Object data(JsonNode data) {
		return data != null ? MAPPER.convertValue(data, Object.class) : null;
	}
}
