package com.gentics.contentnode.init;

import static com.gentics.contentnode.etc.FormDataUtils.FORM_GRID_OPTIONS;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.stream.Streams;
import org.codehaus.groovy.control.CompilationFailedException;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.HandleSelectResultSet;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.forms.Migration;
import com.gentics.contentnode.rest.model.formtype.BaseSetting;
import com.gentics.contentnode.rest.model.formtype.BlockConfig;
import com.gentics.contentnode.rest.model.formtype.BooleanSetting;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.DateSetting;
import com.gentics.contentnode.rest.model.formtype.IntegerSetting;
import com.gentics.contentnode.rest.model.formtype.NumberSetting;
import com.gentics.contentnode.rest.model.formtype.OptionsSetting;
import com.gentics.contentnode.rest.model.formtype.ReferenceSetting;
import com.gentics.contentnode.rest.model.formtype.ReferenceType;
import com.gentics.contentnode.rest.model.formtype.SelectSetting;
import com.gentics.contentnode.rest.model.formtype.StringSetting;
import com.gentics.contentnode.rest.model.formtype.TranslationSetting;
import com.gentics.contentnode.rest.model.formtype.UserSetting;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.google.common.base.Objects;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Init Job that migrates forms. The "type" attribute of the form "data" is transformed into the "formtype" (lowercase).
 * Form type configurations for existing (not deleted) forms are required to already exist in the configuration (if not, the migration job will fail).
 * Form type configurations will automatically be assigned to nodes, that contain forms with the types.
 * The form "data" is migrated to the new structure
 */
@InitJobName("migrateforms")
public class MigrateForms extends InitJob {
	/**
	 * Name of the optional custom migration script file (read from the configuration)
	 */
	protected final static String CUSTOM_MIGRATION_SCRIPT_NAME = "form_migration.groovy";

	/**
	 * Map from new property ids to old property names
	 */
	protected static Map<String, String> INVERTED_PROPERTY_MIGRATION_MAP = MapUtils.invertMap(Migration.PROPERTY_MIGRATION_MAP);

	/**
	 * SQL Statement to get up to 100 unmigrated forms
	 */
	protected final static String SQL_UNMIGRATED_FORMS = "SELECT form.id, folder.node_id, form.name, form.description, form.languages, form.data, form.success_page_id, form.success_node_id FROM form LEFT JOIN folder ON form.folder_id = folder.id WHERE form.formtype = ? LIMIT 100";

	/**
	 * SQL Statement to get the form versions of a batch of forms
	 */
	protected final static String SQL_UNMIGRATED_FORM_VERSIONS = "SELECT auto_id, name, description, languages, data, success_page_id, success_node_id FROM form_nodeversion WHERE id IN (%s)";

	/**
	 * Get the boolean property from a given i18n property. When at least one translation contains true, this will return true, otherwise an empty optional
	 * @param element source element
	 * @param property property to get
	 * @return optional boolean
	 */
	protected static Optional<Boolean> getBooleanFromI18n(ObjectNode element, String property) {
		return element.optional(property)
				.flatMap(boolNode -> boolNode.valueStream().map(JsonNode::asBoolean).filter(b -> b).findFirst());
	}

	/**
	 * Get the string property from a given i18n property. This will return the first non blank translation, or an empty optional.
	 * @param element source element
	 * @param property property to get
	 * @return optional string
	 */
	protected static Optional<String> getStringFromI18n(ObjectNode element, String property) {
		return element.optional(property).flatMap(stringNode -> stringNode.valueStream().map(JsonNode::asText)
				.filter(s -> StringUtils.isNotBlank(s)).findFirst());
	}

	/**
	 * Get the biggest int from a given i18n property.
	 * @param element source element
	 * @param property property to get
	 * @return optional int
	 */
	protected static Optional<Integer> getMaxIntFromI18n(ObjectNode element, String property) {
		return element.optional(property)
				.flatMap(intNode -> intNode.valueStream().mapToInt(JsonNode::asInt).max().stream().boxed().findFirst());
	}

	/**
	 * Get the biggest double from a given i18n property.
	 * @param element source element
	 * @param property property to get
	 * @return optional double
	 */
	protected static Optional<Double> getMaxNumberFromI18n(ObjectNode element, String property) {
		return element.optional(property).flatMap(numberNode -> numberNode.valueStream().filter(JsonNode::isNumber)
				.mapToDouble(JsonNode::asDouble).max().stream().boxed().findFirst());
	}

	/**
	 * Get the list property from a given i18n property.
	 * @param element source element
	 * @param property property to get
	 * @return optional list of strings
	 */
	protected static Optional<List<String>> getMultiSelectionFromI18n(ObjectNode element, String property) {
		return element.optional(property)
				.flatMap(selectionNode -> selectionNode.valueStream().filter(JsonNode::isArray).findFirst())
				.map(array -> array.valueStream().map(JsonNode::asText).collect(Collectors.toList()));
	}

	/**
	 * Object mapper to use
	 */
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * Optional custom migration script
	 */
	protected Script customMigrationScript;

	/**
	 * Groovy shell
	 */
	protected GroovyShell shell;

	/**
	 * Shared data for the custom migration script
	 */
	protected Binding sharedData;

	/**
	 * JSON Slurper
	 */
	protected JsonSlurper slurper = new JsonSlurper();

	/**
	 * Empty migrator
	 */
	private BiConsumer<ObjectNode, ObjectNode> EMPTY_MIGRATOR = (elementNode, property) -> {};

	/**
	 * Migrator for string schema properties
	 */
	private BiConsumer<ObjectNode, ObjectNode> STRING_PROPERTY_MIGRATOR = (elementNode, property) -> {
		property.put("numberOfLines", 1);
	};

	/**
	 * Migrator for numer schema properties
	 */
	private BiConsumer<ObjectNode, ObjectNode> NUMBER_PROPERTY_MIGRATOR = (elementNode, property) -> {
		property.put("precision", 0);
		property.put("isRaw", true);
		property.withObjectProperty("unit");
	};

	/**
	 * Migrator for date schema properties
	 */
	private BiConsumer<ObjectNode, ObjectNode> DATE_PROPERTY_MIGRATOR = (elementNode, property) -> {
		property.put("dateOnly", true);
	};

	/**
	 * Migrator for multiline string schema properties
	 */
	private BiConsumer<ObjectNode, ObjectNode> MULTILINE_STRING_PROPERTY_MIGRATOR = (elementNode, property) -> {
		STRING_PROPERTY_MIGRATOR.accept(elementNode, property);
		property.put("numberOfLines", 5);
	};

	/**
	 * Migrator for catalog schema properties
	 */
	private BiConsumer<ObjectNode, ObjectNode> CATALOG_PROPERTY_MIGRATOR = (elementNode, property) -> {
		property.put("type", "catalog");
		String selectType = elementNode.optional("select_type_i18n")
				.flatMap(n -> n.valueStream().filter(JsonNode::isTextual).map(JsonNode::asText).findFirst())
				.orElse("");

		switch (selectType) {
		case "radiobuttons":
			break;
		case "singleselect":
			break;
		case "multiselect":
			property.put("isList", true);
			break;
		default:
			break;
		}
	};

	/**
	 * Migrator for catalog uiSchema properties
	 */
	private BiConsumer<ObjectNode, ObjectNode> CATALOG_UI_PROPERTY_MIGRATOR = (elementNode, property) -> {
		String selectType = elementNode.optional("select_type_i18n")
				.flatMap(n -> n.valueStream().filter(JsonNode::isTextual).map(JsonNode::asText).findFirst())
				.orElse("");

		boolean radio = false;

		switch (selectType) {
		case "radiobuttons":
			radio = true;
			break;
		case "singleselect":
			break;
		case "multiselect":
			break;
		default:
			break;
		}

		property.withObjectProperty(FORM_GRID_OPTIONS)
			.put("radio", radio);
	};

	/**
	 * Current form, which is migrated (for logging)
	 */
	protected FormToMigrate current;

	/**
	 * Current form type configuration (for logging)
	 */
	protected String currentFormType;

	@Override
	public void execute() throws NodeException {
		File customMigrationScriptFile = getCustomMigrationScriptFile();
		if (customMigrationScriptFile != null) {
			sharedData = new Binding();
			shell = new GroovyShell(sharedData);
			try {
				customMigrationScript = shell.parse(customMigrationScriptFile);
			} catch (CompilationFailedException | IOException e) {
				throw new NodeException(e);
			}
		}

		FormTypeConfigurationMap formTypeConfigurations = FormConfigProvider.getConfig();

		Map<Integer, Set<String>> formTypesPerNode = new HashMap<>();

		// migrate the forms in batches of max 100 forms
		while (true) {
			Map<Integer, FormToMigrate> toMigrate = DBUtils.select(SQL_UNMIGRATED_FORMS, pst -> {
				pst.setString(1, FormFactory.UNMIGRATED_FORMS);
			}, resultSetHandler(false));

			if (toMigrate.isEmpty()) {
				break;
			}

			for (Map.Entry<Integer, FormToMigrate> entry : toMigrate.entrySet()) {
				int formId = entry.getKey();
				int nodeId = entry.getValue().nodeId();
				JsonNode data = entry.getValue().data();

				String formType = StringUtils
						.toRootLowerCase(data.optional("type").map(n -> n.asText("generic")).orElse("generic"));

				if (formTypeConfigurations == null || !formTypeConfigurations.containsKey(formType)) {
					throw new NodeException(
							"Migration of forms not possible. Missing required form type configuration '%s'."
									.formatted(formType));
				}

				formTypesPerNode.computeIfAbsent(nodeId, key -> new HashSet<>()).add(formType);

				JsonNode migratedData = migrateFormData(formTypeConfigurations.get(formType), entry.getValue(), formType);

				DBUtils.update("UPDATE form SET data = ?, formtype = ? WHERE id = ?", migratedData.toString(), formType, formId);
			}

			// migrate also the form versions
			String sqlFormVersions = SQL_UNMIGRATED_FORM_VERSIONS
					.formatted(StringUtils.repeat("?", ",", toMigrate.size()));

			Map<Integer, FormToMigrate> versionsToMigrate = DBUtils.select(sqlFormVersions, pst -> {
				int pCounter = 0;
				for (int formId : toMigrate.keySet()) {
					pst.setInt(++pCounter, formId);
				}
			}, resultSetHandler(true));

			for (Map.Entry<Integer, FormToMigrate> entry : versionsToMigrate.entrySet()) {
				int autoId = entry.getKey();
				JsonNode data = entry.getValue().data();

				String formType = StringUtils
						.toRootLowerCase(data.optional("type").map(n -> n.asText("generic")).orElse("generic"));

				if (formTypeConfigurations == null || !formTypeConfigurations.containsKey(formType)) {
					throw new NodeException(
							"Migration of forms not possible. Missing required form type configuration '%s'."
									.formatted(formType));
				}

				JsonNode migratedData = migrateFormData(formTypeConfigurations.get(formType), entry.getValue(), formType);

				DBUtils.update("UPDATE form_nodeversion SET data = ? WHERE auto_id = ?", migratedData.toString(), autoId);
			}

			TransactionManager.getCurrentTransaction().commit(false);
		}

		// assign the form types to nodes, if forms where migrated to specific types in a node
		for (Entry<Integer, Set<String>> entry : formTypesPerNode.entrySet()) {
			int nodeId = entry.getKey();
			Set<String> formTypes = entry.getValue();

			for (String formType : formTypes) {
				DBUtils.update("INSERT IGNORE INTO node_formtype (node_id, formtype) VALUES (?, ?)", nodeId, formType);
			}
		}
	}

	/**
	 * Find the custom forms migration file (if present)
	 * @return custom migration file or null if not found
	 */
	protected File getCustomMigrationScriptFile() {
		String confPath = ConfigurationValue.CONF_PATH.get();
		String configFiles = ConfigurationValue.CONF_FILES.get();

		for (String filePath : StringUtils.split(configFiles, ", ")) {
			File configFile = null;
			if (!StringUtils.isBlank(confPath) && new File(confPath, filePath).exists()) {
				configFile = new File(confPath, filePath);
			} else {
				configFile = new File(filePath);
			}
			if (configFile.isDirectory()) {
				// if the configured file is a directory, we read the files in the directory
				Optional<File> optFile = Streams.of(configFile.listFiles(file -> {
					return file.isFile() && Strings.CI.equals(file.getName(), CUSTOM_MIGRATION_SCRIPT_NAME);
				})).findFirst();
				if (optFile.isPresent()) {
					return optFile.get();
				}
			} else if (configFile.exists() && Strings.CI.equals(configFile.getName(), CUSTOM_MIGRATION_SCRIPT_NAME)) {
				return configFile;
			}
		}

		return null;
	}

	/**
	 * Put the given data node into the {@link #sharedData} with given variable name
	 * @param name variable name
	 * @param data data node
	 */
	protected void putToSharedData(String name, JsonNode data) {
		sharedData.setVariable(name, slurper.parseText(data.toString()));
	}

	/**
	 * Get the data node from the {@link #sharedData} with given variable name
	 * @param name variable name
	 * @return data node
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	protected JsonNode getFromSharedData(String name) throws JsonMappingException, JsonProcessingException {
		return mapper.readTree(JsonOutput.toJson(sharedData.getVariable(name)));
	}

	@SuppressWarnings("unchecked")
	protected HandleSelectResultSet<Map<Integer, FormToMigrate>> resultSetHandler(boolean version) {
		return rs -> {
			Map<Integer, FormToMigrate> result = new HashMap<>();

			while (rs.next()) {
				int formId = version ? rs.getInt("auto_id") : rs.getInt("id");
				int nodeId = version ? 0 : rs.getInt("node_id");
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
		};
	}

	/**
	 * Migrate the form data
	 * @param config form type configuration
	 * @param toMigrate form to migrate
	 * @param formType form type
	 * @return migrated form data
	 */
	protected JsonNode migrateFormData(FormTypeConfiguration config, FormToMigrate toMigrate, String formType) {
		current = toMigrate;
		currentFormType = formType;
		ObjectNode migrated = mapper.createObjectNode();

		// migrate some basic data
		migrated.put("adminEmailAddress", toMigrate.data().optional("email").map(n -> n.asText()).orElse(""));
		migrated.put("adminEmailPageId", toMigrate.data().optional("mailsource_pageid").map(n -> n.asInt()).orElse(0));
		migrated.put("adminEmailNodeId", toMigrate.data().optional("mailsource_nodeid").map(n -> n.asInt()).orElse(0));
		migrated.set("adminEmailSubject", toMigrate.data().optional("mailsubject_i18n").orElseGet(() -> mapper.createObjectNode()));
		migrated.set("adminEmailTemplate", toMigrate.data().optional("mailtemp_i18n").orElseGet(() -> mapper.createObjectNode()));
		migrated.put("templateContext", toMigrate.data().optional("templateContext").map(n -> n.asText()).orElse(""));
		migrated.put("successPageId", toMigrate.successPageId());
		migrated.put("successNodeId", toMigrate.successNodeId());
		migrated.set("successUrl", toMigrate.data().optional("successurl_i18n").orElseGet(() -> mapper.createObjectNode()));

		ObjectNode schema = mapper.createObjectNode();
		ObjectNode schemaProperties = mapper.createObjectNode();
		schema.set("properties", schemaProperties);

		ObjectNode uiSchema = mapper.createObjectNode();

		ObjectNode name = mapper.createObjectNode();
		uiSchema.set("formname", name);
		ObjectNode description = mapper.createObjectNode();
		uiSchema.set("formdescription", description);

		ArrayNode pages = uiSchema.withArrayProperty("pages");
		ObjectNode firstPage = pages.addObject();

		for (String language : toMigrate.languages()) {
			name.put(language, toMigrate.name());
			description.put(language, toMigrate.description());
		}

		// Migrate the elements
		toMigrate.data().optional("elements").filter(element -> element.isArray()).ifPresent(elements -> {
			for (JsonNode element : elements) {
				if (element.isObject()) {
					migrateElement(config, (ObjectNode) element, schemaProperties, pages, firstPage);
				}
			}
		});

		// return any empty pages from the ui schema
		if (pages.size() > 1) {
			pages.removeIf(page -> page.isEmpty());
		}

		migrated.set("schema", schema);
		migrated.set("uiSchema", uiSchema);

		return migrated;
	}

	/**
	 * Migrate a single element
	 * @param config form type configuration
	 * @param elementNode element node to migrate
	 * @param schemaProperties root node collecting the schema properties
	 * @param uiSchemaPages root node collecing the uiSchema pages
	 * @param currentPage current page
	 */
	protected void migrateElement(FormTypeConfiguration config, ObjectNode elementNode, ObjectNode schemaProperties,
			ArrayNode uiSchemaPages, ObjectNode currentPage) {
		String elementType = elementNode.optional("type").map(n -> n.asText()).orElse("unknown");

		switch (elementType) {
		case "buttons":
		case "formprogress":
			break;
		case "input":
			// if there is a special validation, the element may be migrated to another type
			String validation = getStringFromI18n(elementNode, "validation_i18n").orElse("");
			switch (validation) {
			case "number":
				migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, "number", NUMBER_PROPERTY_MIGRATOR, EMPTY_MIGRATOR);
				break;
			case "date":
			case "datepicker":
				migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, "datetime", DATE_PROPERTY_MIGRATOR, EMPTY_MIGRATOR);
				break;
			case "time":
				migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, "time", EMPTY_MIGRATOR, EMPTY_MIGRATOR);
				break;
			default:
				migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, "string", STRING_PROPERTY_MIGRATOR, EMPTY_MIGRATOR);
				break;
			}
			break;
		case "textarea":
			migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, "string", MULTILINE_STRING_PROPERTY_MIGRATOR, EMPTY_MIGRATOR);
			break;
		case "checkbox":
			migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, "boolean", EMPTY_MIGRATOR, EMPTY_MIGRATOR);
			break;
		case "selectgroup":
			migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, "catalog", CATALOG_PROPERTY_MIGRATOR, CATALOG_UI_PROPERTY_MIGRATOR);
			break;
		case "file":
			migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, "binary", EMPTY_MIGRATOR, EMPTY_MIGRATOR);
			break;
		case "usermail":
			migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, "usermail", STRING_PROPERTY_MIGRATOR, EMPTY_MIGRATOR);
			break;
		case "formpage":
			ObjectNode page = uiSchemaPages.addObject();
			page.set("pagename", elementNode.optional("description_i18n").orElseGet(() -> mapper.createObjectNode()));
			elementNode.optional("elements").filter(element -> element.isArray()).ifPresent(elements -> {
				for (JsonNode element : elements) {
					if (element.isObject()) {
						migrateElement(config, (ObjectNode) element, schemaProperties, uiSchemaPages, page);
					}
				}
			});
			break;
		default:
			migrateElement(config, elementNode, schemaProperties, uiSchemaPages, currentPage, elementType, EMPTY_MIGRATOR, EMPTY_MIGRATOR);
			break;
		}
	}

	/**
	 * Migrate a single element
	 * @param config form type configuration
	 * @param elementNode element node
	 * @param schemaProperties root node collecting the schema properties
	 * @param uiSchemaPages root node collecing the uiSchema pages
	 * @param currentPage current page
	 * @param migratedType migrated type
	 * @param propertyMigrator property migrator
	 * @param uiPropertyMigrator ui property migrator
	 */
	protected void migrateElement(FormTypeConfiguration config, ObjectNode elementNode, ObjectNode schemaProperties, ArrayNode uiSchemaPages,
			ObjectNode currentPage, String migratedType, BiConsumer<ObjectNode, ObjectNode> propertyMigrator, BiConsumer<ObjectNode, ObjectNode> uiPropertyMigrator) {
		// extract the name
		String name = elementNode.optional("name").map(JsonNode::asText).orElse("");

		Optional<ControlConfig> optionalControl = Optional.ofNullable(config.controls).map(c -> c.get(migratedType));
		Optional<BlockConfig> optionalBlock = Optional.ofNullable(config.blocks).map(c -> c.get(migratedType));

		if (optionalControl.isEmpty() && optionalBlock.isEmpty()) {
			logger.error("Error while migrating form '%s': type '%s' of element '%s' is unknown in the form type configuration '%s'"
					.formatted(Optional.ofNullable(current).map(FormToMigrate::name).orElse("<unknown form>"), migratedType, name, currentFormType));
		}

		AtomicReference<JsonNode> propertyRef = new AtomicReference<>();

		optionalControl.ifPresent(controlConfig -> {
			// create the schema property and migrate common data
			ObjectNode property = mapper.createObjectNode();

			property.put("name", name);
			property.put("type", migratedType);
			property.put("updateOnChange", false);
			boolean mandatory = elementNode.optional("mandatory_i18n")
					.flatMap(mandatoryNode -> mandatoryNode.valueStream().map(n -> n.asBoolean()).filter(b -> b).findFirst())
					.orElse(false);
			property.put("mandatory", mandatory);
			property.put("isList", false);
			property.withObjectProperty("validation");

			// migrate type specific data
			propertyMigrator.accept(elementNode, property);

			// store the property in the reference, we will add it later
			propertyRef.set(property);

			// migrate all backend settings
			Optional.ofNullable(controlConfig.settings).ifPresent(settings -> {
				settings.stream().filter(s -> s.backend).forEach(s -> {
					migrateSetting(s, elementNode, property);
				});
			});
		});

		// create the ui property and migrate common data
		ObjectNode uiProperty = mapper.createObjectNode();
		uiProperty.put("type", "property");
		uiProperty.put("id", name);
		uiProperty.set("label", elementNode.optional("label_i18n").orElseGet(() -> mapper.createObjectNode()));
		uiProperty.set("description", elementNode.optional("description_i18n").filter(n -> !n.isNull()).orElseGet(() -> mapper.createObjectNode()));

		// migrate condition
		Optional<String> optConditionSource = getStringFromI18n(elementNode, "element_to_check_i18n");
		Optional<String> optEqualsValue = getStringFromI18n(elementNode, "value_to_check_i18n");
		if (optConditionSource.isPresent() && optEqualsValue.isPresent()) {
			ObjectNode condition = uiProperty.withObjectProperty(FORM_GRID_OPTIONS).withObjectProperty("condition");
			condition.withObjectProperty("source").put("schema", optConditionSource.get());
			condition.put("equals", optEqualsValue.get());
		}

		// migrate all frontend settings defined in the type configuration to formGridOptions
		optionalControl.map(c -> c.settings).ifPresent(settings -> {
			settings.stream().filter(s -> !s.backend).forEach(s -> {
				migrateSetting(s, elementNode, uiProperty);
			});
		});
		optionalBlock.map(b -> b.settings).ifPresent(settings -> {
			settings.stream().filter(s -> !s.backend).forEach(s -> {
				migrateSetting(s, elementNode, uiProperty);
			});
		});
		optionalBlock.ifPresent(blockConfig -> {
			uiProperty.withObjectProperty(FORM_GRID_OPTIONS).put("type", migratedType);

			// when the block is a container, it may contain inner elements, which we need to migrate
			if (blockConfig.container) {
				elementNode.optional("elements").filter(element -> element.isArray()).ifPresent(elements -> {
					for (JsonNode element : elements) {
						if (element.isObject()) {
							migrateElement(config, (ObjectNode) element, schemaProperties, uiSchemaPages, uiProperty);
						}
					}
				});
			}
		});

		// migrate type specific ui data
		uiPropertyMigrator.accept(elementNode, uiProperty);

		AtomicReference<JsonNode> uiPropertyRef = new AtomicReference<>(uiProperty);

		// run the optional migration script
		if (customMigrationScript != null) {
			// clear shared data
			sharedData.getVariables().clear();

			putToSharedData("element", elementNode);
			// add data
			if (propertyRef.get() != null) {
				putToSharedData("property", propertyRef.get());
			}
			putToSharedData("uiProperty", uiPropertyRef.get());

			// run the script
			customMigrationScript.run();

			// get possibly modified property and uiProperty
			try {
				if (propertyRef.get() != null) {
					propertyRef.set(getFromSharedData("property"));
				}
				uiPropertyRef.set(getFromSharedData("uiProperty"));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}

		}

		// add the schema property to the schema
		if (propertyRef.get() != null) {
			schemaProperties.set(propertyRef.get().get("name").asText(), propertyRef.get());
		}

		// add the ui property to the current page
		currentPage.withArrayProperty("elements").add(uiPropertyRef.get());
	}

	/**
	 * Migrate a single setting
	 * @param setting setting to migrate
	 * @param elementNode element node
	 * @param propertyOrUiProperty either the schema property or the uiSchema property
	 */
	protected void migrateSetting(BaseSetting setting, ObjectNode elementNode, ObjectNode propertyOrUiProperty) {
		String oldName = "%s_i18n".formatted(INVERTED_PROPERTY_MIGRATION_MAP.getOrDefault(setting.id, setting.id));

		String settingTypeName = Optional.ofNullable(setting.getClass().getAnnotation(JsonTypeName.class))
				.map(JsonTypeName::value).orElse("");

		String propertyPath = Optional.ofNullable(setting.propertyPath)
				.orElse("%s.%s".formatted(FORM_GRID_OPTIONS, setting.id));

		switch (settingTypeName) {
		case StringSetting.TYPE:
			getStringFromI18n(elementNode, oldName)
					.ifPresent(string -> setProperty(propertyOrUiProperty, propertyPath, (n, p) -> n.put(p, string)));
			break;
		case TranslationSetting.TYPE:
			elementNode.optional(oldName).filter(n -> !n.isNull()).ifPresent(
					translation -> setProperty(propertyOrUiProperty, propertyPath, (n, p) -> n.set(p, translation)));
			break;
		case IntegerSetting.TYPE:
			getMaxIntFromI18n(elementNode, oldName)
					.ifPresent(value -> setProperty(propertyOrUiProperty, propertyPath, (n, p) -> n.put(p, value)));
			break;
		case NumberSetting.TYPE:
			getMaxNumberFromI18n(elementNode, oldName)
				.ifPresent(value -> setProperty(propertyOrUiProperty, propertyPath, (n, p) -> n.put(p, value)));
			break;
		case BooleanSetting.TYPE:
			boolean booleanValue = getBooleanFromI18n(elementNode, oldName).orElse(false);
			setProperty(propertyOrUiProperty, propertyPath, (n, p) -> n.put(p, booleanValue));
			break;
		case DateSetting.TYPE:
			break;
		case SelectSetting.TYPE:
			if (setting instanceof SelectSetting selectSetting) {
				if (selectSetting.multiple) {
					getMultiSelectionFromI18n(elementNode, oldName)
							.ifPresent(selection -> setProperty(propertyOrUiProperty, propertyPath,
									(n, p) -> n.set(p, mapper.valueToTree(selection))));
				} else {
					getStringFromI18n(elementNode, oldName).ifPresent(selection -> {
						// get the selection from the setting
						Object selectedOption = getSelectedOption(selectSetting, selection);
						if (selectedOption instanceof Map<?, ?> mapOption) {
							JsonNode nodeOption = mapper.valueToTree(mapOption);
							setProperty(propertyOrUiProperty, propertyPath, (n, p) -> n.set(p, nodeOption));
						} else if (selectedOption instanceof JsonNode nodeOption) {
							setProperty(propertyOrUiProperty, propertyPath, (n, p) -> n.set(p, nodeOption));
						} else if (selectedOption instanceof String stringOption) {
							setProperty(propertyOrUiProperty, propertyPath, (n, p) -> n.put(p, stringOption));
						}
					});
				}
			}
			break;
		case OptionsSetting.TYPE:
			oldName = INVERTED_PROPERTY_MIGRATION_MAP.getOrDefault(setting.id, setting.id);
			elementNode.optional(oldName).filter(JsonNode::isArray).ifPresent(options -> {
				options.valueStream().map(option -> {
					JsonNode migratedOption = mapper.createObjectNode().put("value", option.get("key").asText())
							.set("label", option.get("value_i18n"));
					return migratedOption;
				}).forEach(option -> {
					propertyOrUiProperty.withObjectProperty(FORM_GRID_OPTIONS).withArrayProperty(setting.id)
							.add(option);
				});
			});
			break;
		case UserSetting.TYPE:
			break;
		case ReferenceSetting.TYPE:
			String itemIdProperty = "%s_pageid".formatted(INVERTED_PROPERTY_MIGRATION_MAP.getOrDefault(setting.id, setting.id));
			String nodeIdProperty = "%s_nodeid".formatted(INVERTED_PROPERTY_MIGRATION_MAP.getOrDefault(setting.id, setting.id));

			int itemId = elementNode.optional(itemIdProperty).filter(JsonNode::isInt).map(JsonNode::asInt).orElse(0);
			int nodeId = elementNode.optional(nodeIdProperty).filter(JsonNode::isInt).map(JsonNode::asInt).orElse(0);

			if (itemId > 0 && setting instanceof ReferenceSetting referenceSetting) {
				ReferenceType refType = referenceSetting.referenceTypes.stream().findFirst().orElse(ReferenceType.page);

				if (referenceSetting.multiple) {
					ArrayNode refArray = propertyOrUiProperty.withObjectProperty(FORM_GRID_OPTIONS).withArrayProperty(setting.id);
					ObjectNode reference = refArray.addObject();
					reference.put("id", itemId);
					reference.put("nodeId", nodeId);
					reference.put("type", refType.name());
				} else {
					ObjectNode reference = propertyOrUiProperty.withObjectProperty(FORM_GRID_OPTIONS)
							.withObjectProperty(setting.id);
					reference.put("id", itemId);
					reference.put("nodeId", nodeId);
					reference.put("type", refType.name());
				}
			}

			break;
		}
	}

	/**
	 * Get the selected option based on the given value.
	 * Either the option.value equals the given value, or it as a property "key" that equals the value
	 * @param selectSetting select setting
	 * @param value property to be migrated
	 * @return selected option value
	 */
	protected Object getSelectedOption(SelectSetting selectSetting, String value) {
		return Optional.ofNullable(selectSetting.options).flatMap(options -> options.stream().filter(option -> {
			if (Objects.equal(option.value, value)) {
				return true;
			} else if (option.value instanceof JsonNode oValue) {
				return Objects.equal(oValue.optional("key"), value);
			} else if (option.value instanceof Map<?, ?> oMap) {
				return Objects.equal(oMap.get("key"), value);
			} else {
				return false;
			}
		}).findFirst()).map(option -> option.value).orElse(value);
	}

	/**
	 * Set the property by path to the node
	 * @param node node to modify
	 * @param path property path
	 * @param setter setter
	 */
	protected void setProperty(ObjectNode node, String path, BiConsumer<ObjectNode, String> setter) {
		String[] parts = StringUtils.split(path, '.');
		for (int i = 0; i < parts.length - 1; i++) {
			node = node.withObjectProperty(parts[i]);
		}
		setter.accept(node, parts[parts.length - 1]);
	}

	/**
	 * Record holding data for a form to be migrated
	 */
	protected record FormToMigrate(int nodeId, String name, String description, List<String> languages, JsonNode data, int successPageId, int successNodeId) {}
}
