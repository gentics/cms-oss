package com.gentics.contentnode.init;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Init Job that migrates forms. The "type" attribute of the form "data" is transformed into the "formtype" (lowercase).
 * Form type configurations for existing (not deleted) forms are required to already exist in the configuration (if not, the migration job will fail).
 * Form type configurations will automatically be assigned to nodes, that contain forms with the types.
 * The form "data" is migrated to the new structure
 */
@InitJobName("migrateforms")
public class MigrateForms extends InitJob {
	/**
	 * Configuration path to the type configurations
	 */
	private static final String TYPES_CONFIG_PATH = "forms.types";

	private ObjectMapper mapper = new ObjectMapper();

	private BiConsumer<ObjectNode, ObjectNode> EMPTY_MIGRATOR = (elementNode, property) -> {};

	private BiConsumer<ObjectNode, ObjectNode> STRING_PROPERTY_MIGRATOR = (elementNode, property) -> {
		property.put("type", "string");
		int maxCharCount = getMaxIntFromI18n(elementNode, "maxCharCount_i18n");
		if (maxCharCount > 0) {
			property.withObjectProperty("validation")
				.put("minLength", 0)
				.put("maxLength", maxCharCount);
		}
	};

	private BiConsumer<ObjectNode, ObjectNode> BOOLEAN_PROPERTY_MIGRATOR = (elementNode, property) -> {
		property.put("type", "boolean");
	};

	@Override
	public void execute() throws NodeException {
		Map<String, Object> formTypeConfigurations = NodeConfigRuntimeConfiguration.getPreferences()
				.getPropertyMap(TYPES_CONFIG_PATH);

		Map<Integer, Set<String>> formTypesPerNode = new HashMap<>();

		while (true) {
			@SuppressWarnings("unchecked")
			Map<Integer, FormToMigrate> toMigrate = DBUtils.select(
					"SELECT form.id, folder.node_id, form.name, form.description, form.languages, form.data, form.success_page_id, form.success_node_id FROM form LEFT JOIN folder ON form.folder_id = folder.id WHERE form.formtype = ? LIMIT 100",
					pst -> {
						pst.setString(1, FormFactory.UNMIGRATED_FORMS);
					}, rs -> {
						Map<Integer, FormToMigrate> result = new HashMap<>();

						while (rs.next()) {
							int formId = rs.getInt("id");
							int nodeId = rs.getInt("node_id");
							String formName = rs.getString("name");
							String formDescription = rs.getString("description");
							int successPageId = rs.getInt("success_page_id");
							int successNodeId = rs.getInt("success_node_id");

							JsonNode data;
							try {
								data = mapper.readTree(rs.getString("data"));
							} catch (JsonProcessingException e) {
								data = mapper.createObjectNode();
							}

							List<String> languages;
							try {
								languages = mapper.readValue(ObjectTransformer.getString(rs.getString("languages"), "[]"), List.class);
							} catch (JsonProcessingException e) {
								languages = Collections.emptyList();
							}
							result.put(formId, new FormToMigrate(nodeId, formName, formDescription, languages, data,
									successPageId, successNodeId));
						}
						return result;
					});

			if (toMigrate.isEmpty()) {
				break;
			}

			for (Map.Entry<Integer, FormToMigrate> entry : toMigrate.entrySet()) {
				int formId = entry.getKey();
				int nodeId = entry.getValue().nodeId();
				JsonNode data = entry.getValue().data();

				String formType = StringUtils.toRootLowerCase(data.get("type").asText("generic"));

				if (formTypeConfigurations == null || !formTypeConfigurations.containsKey(formType)) {
					throw new NodeException(
							"Migration of forms not possible. Missing required form type configuration '%s'."
									.formatted(formType));
				}

				formTypesPerNode.computeIfAbsent(nodeId, key -> new HashSet<>()).add(formType);

				JsonNode migratedData = migrateFormData(entry.getValue());

				DBUtils.update("UPDATE form SET data = ?, formtype = ? WHERE id = ?", migratedData.toString(), formType, formId);

				// TODO migrate all form_nodeversion data
			}

			TransactionManager.getCurrentTransaction().commit(false);
		}

		for (Entry<Integer, Set<String>> entry : formTypesPerNode.entrySet()) {
			int nodeId = entry.getKey();
			Set<String> formTypes = entry.getValue();

			for (String formType : formTypes) {
				DBUtils.update("INSERT IGNORE INTO node_formtype (node_id, formtype) VALUES (?, ?)", nodeId, formType);
			}
		}
	}

	/**
	 * Migrate the form data
	 * @param data original data
	 * @return migrated form data
	 */
	protected JsonNode migrateFormData(FormToMigrate toMigrate) {
		// currently disabled
		if (true) {
			return toMigrate.data();
		}
		ObjectNode migrated = mapper.createObjectNode();

		// migrate some basic data
		migrated.put("adminEmailAddress", toMigrate.data().optional("email").map(n -> n.asText()).orElse(""));
		migrated.put("adminEmailPageId", toMigrate.data().optional("mailsource_pageid").map(n -> n.asInt()).orElse(0));
		migrated.put("adminEmailNodeId", toMigrate.data().optional("mailsource_nodeid").map(n -> n.asInt()).orElse(0));
		migrated.set("adminEmailSubjectI18n", toMigrate.data().optional("mailsubject_i18n").orElseGet(() -> mapper.createObjectNode()));
		migrated.set("adminEmailTemplateI18n", toMigrate.data().optional("mailtemp_i18n").orElseGet(() -> mapper.createObjectNode()));
		migrated.put("templateContext", toMigrate.data().optional("templateContext").map(n -> n.asText()).orElse(""));
		migrated.put("successPageId", toMigrate.successPageId());
		migrated.put("successNodeId", toMigrate.successNodeId());
		migrated.set("successUrlI18n", toMigrate.data().optional("successurl_i18n").orElseGet(() -> mapper.createObjectNode()));

		ObjectNode schema = mapper.createObjectNode();
		ObjectNode schemaProperties = mapper.createObjectNode();
		schema.set("properties", schemaProperties);

		ObjectNode uiSchema = mapper.createObjectNode();

		ObjectNode name = mapper.createObjectNode();
		uiSchema.set("formname", name);
		ObjectNode description = mapper.createObjectNode();
		uiSchema.set("formdescription", description);

		ArrayNode pages = mapper.createArrayNode();
		ObjectNode firstPage = mapper.createObjectNode();
		pages.add(firstPage);
		uiSchema.set("pages", pages);

		for (String language : toMigrate.languages()) {
			name.put(language, toMigrate.name());
			description.put(language, toMigrate.description());
		}

		// Migrate the elements
		toMigrate.data().optional("elements").filter(element -> element.isArray()).ifPresent(elements -> {
			for (JsonNode element : elements) {
				if (element.isObject()) {
					migrateElement((ObjectNode) element, schemaProperties, pages, firstPage);
				}
			}
		});

		migrated.set("schema", schema);
		migrated.set("ui-schema", uiSchema);

		return migrated;
	}

	protected void migrateElement(ObjectNode elementNode, ObjectNode schemaProperties, ArrayNode uiSchemaPages,
			ObjectNode currentPage) {
		String elementType = elementNode.optional("type").map(n -> n.asText()).orElse("unknown");

		switch (elementType) {
		case "input":
			migrateElement(elementNode, schemaProperties, uiSchemaPages, currentPage, STRING_PROPERTY_MIGRATOR, EMPTY_MIGRATOR);
			break;
		case "checkbox":
			migrateElement(elementNode, schemaProperties, uiSchemaPages, currentPage, BOOLEAN_PROPERTY_MIGRATOR, EMPTY_MIGRATOR);
			break;
		default:
			// TODO log
		}
	}

	protected void migrateElement(ObjectNode elementNode, ObjectNode schemaProperties, ArrayNode uiSchemaPages,
			ObjectNode currentPage, BiConsumer<ObjectNode, ObjectNode> propertyMigrator, BiConsumer<ObjectNode, ObjectNode> uiPropertyMigrator) {
		// extract the name
		String name = elementNode.optional("name").map(JsonNode::asText).orElse("");

		// create the schema property and migrate common data
		ObjectNode property = mapper.createObjectNode();
		property.put("name", name);
		property.put("updateOnChange", false);
		boolean mandatory = elementNode.optional("mandatory_i18n")
				.flatMap(mandatoryNode -> mandatoryNode.valueStream().map(n -> n.asBoolean()).filter(b -> b).findFirst())
				.orElse(false);
		property.put("mandatory", mandatory);
		property.put("isList", false);
		property.withObjectProperty("validation");

		JsonNode defaultValue = elementNode.optional("value_i18n").flatMap(n -> n.valueStream().findFirst()).orElse(null);
		if (defaultValue != null) {
			property.set("defaultValue", defaultValue);
		}

		// migrate type specific data
		propertyMigrator.accept(elementNode, property);

		// add the schema property to the schema
		schemaProperties.set(property.get("name").asText(), property);

		// create the ui property and migrate common data
		ObjectNode uiProperty = mapper.createObjectNode();
		uiProperty.put("type", "property");
		uiProperty.put("id", name);
		uiProperty.set("label", elementNode.optional("label_i18n").orElseGet(() -> mapper.createObjectNode()));
		uiProperty.withObjectProperty("description");

		// migrate type specific ui data
		uiPropertyMigrator.accept(elementNode, uiProperty);

		// add the ui property to the current page
		currentPage.withArrayProperty("elements").add(uiProperty);
	}

	protected boolean getBooleanFromI18n(ObjectNode element, String property) {
		return element.optional(property)
				.flatMap(boolNode -> boolNode.valueStream().map(JsonNode::asBoolean).filter(b -> b).findFirst())
				.orElse(false);
	}

	protected int getMaxIntFromI18n(ObjectNode element, String property) {
		return element.optional(property)
				.map(intNode -> intNode.valueStream().mapToInt(JsonNode::asInt).max().orElse(0)).orElse(0);
	}

	protected record FormToMigrate(int nodeId, String name, String description, List<String> languages, JsonNode data, int successPageId, int successNodeId) {}
}
