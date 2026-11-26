package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.mcp.McpArgs;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.ExternalFormException;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.model.ItemVersion;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationResponse;
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

	/**
	 * A form's version, as the form editor's version history shows it
	 * @param number version number
	 * @param timestamp timestamp of the version
	 */
	@JsonInclude(Include.NON_NULL)
	record Version(String number, int timestamp) {
	}

	private Forms() {
	}

	/**
	 * Get a form's current version
	 * @param form form
	 * @return version, null if the form has none
	 */
	static Version version(FormModel form) {
		ItemVersion version = form.getVersion();
		return version != null && version.getNumber() != null ? new Version(version.getNumber(), version.getTimestamp())
				: null;
	}

	/**
	 * Build the schema of a form's version
	 * @return schema
	 */
	static Map<String, Object> versionSchema() {
		return schema("object", "The form's current version, as the CMS form editor's version history shows it. A "
				+ "write over a version the caller has not seen is the caller's to prevent.", "properties",
				Map.of("number", schema("string", null), "timestamp", schema("integer", null)), "required",
				List.of("number"));
	}

	/**
	 * Get a form type's configuration, as GET /form/types/{type} does
	 * @param resource form resource
	 * @param type form type
	 * @return configuration
	 * @throws Exception not found for an unknown type
	 */
	static FormTypeConfigurationModel type(FormResource resource, String type) throws Exception {
		FormTypeConfigurationResponse response = resource.getFormTypeconfiguration(type);
		return response.getItem();
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
		properties.put(ARG_DATA, schema("object", "A CMS form's definition, the shape the CMS form editor saves. "
				+ "schema.properties holds one entry per control, keyed by its name: {name, type, mandatory?, formPage?, "
				+ "...the control's settings}; type is a control key of the form type. ui-schema.pages is the form's "
				+ "pages, each {pagename: {<lang>: text}, elements: [...]}, an element {type: property (container or aggregate "
				+ "for one holding elements), id: <the control's name or the block's id>, label?: {<lang>: text}, "
				+ "description?: {<lang>: text}, uiSchemaPage: <page index>, formGridOptions: {type: <control or block key>, numberOfColumns: 1-12, "
				+ "inForm, inSummary, ...}, elements?: [...] for a container}. Form-level settings sit beside them: "
				+ "flowId, formWidth, adminEmailAddress, adminEmailSubject, adminEmailTemplate, adminEmailNodeId, "
				+ "adminEmailPageId, captchaId, successNodeId, successPageId, successUrlI18n, templateContext. "
				+ "get_form_type gives the controls, blocks and settings a type allows. NAMES: never invent a name. "
				+ "Give each new control or block an id of the form new:<anything>, the same in its schema.properties "
				+ "key, its name and its element id, and in any condition that refers to it; the server replaces every "
				+ "new: placeholder with <control or block key>_<UUID v4 without dashes>, the name the form editor "
				+ "gives, and keeps every other name, so stored submissions stay attached. schema.key and ui-schema.key "
				+ "(one UUID per form) and version are filled in when missing. A write replaces the whole definition: "
				+ "send all of it, not a change.", "properties",
				Map.of(FormDefinitions.SCHEMA, schema("object", null), FormDefinitions.UI_SCHEMA, schema("object", null)),
				"additionalProperties", true));
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
