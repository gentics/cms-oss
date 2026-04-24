package com.gentics.contentnode.forms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.stream.Streams;
import org.codehaus.groovy.control.CompilationFailedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.rest.model.formtype.BaseSetting;
import com.gentics.contentnode.rest.model.formtype.BlockConfig;
import com.gentics.contentnode.rest.model.formtype.BooleanSetting;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.ElementConfig;
import com.gentics.contentnode.rest.model.formtype.NumberSetting;
import com.gentics.contentnode.rest.model.formtype.OptionsSetting;
import com.gentics.contentnode.rest.model.formtype.PublishAsType;
import com.gentics.contentnode.rest.model.formtype.ReferenceSetting;
import com.gentics.contentnode.rest.model.formtype.ReferenceType;
import com.gentics.contentnode.rest.model.formtype.SelectOption;
import com.gentics.contentnode.rest.model.formtype.SelectSetting;
import com.gentics.contentnode.rest.model.formtype.StringSetting;
import com.gentics.contentnode.rest.model.formtype.TranslationSetting;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Migration tool for form type configurations
 */
public class Migration {
	/**
	 * Known types
	 */
	protected final static List<String> FORM_TYPES = Arrays.asList("generic", "poll");

	/**
	 * List of unmigrated properties (properties, which are standard)
	 */
	protected final static List<String> UNMIGRATED_PROPERTIES = Arrays.asList("label", "description", "mandatory",
			"validation", "select_type", "element_to_check", "value_to_check");

	/**
	 * List of unmigrated properties per element type ID
	 */
	protected final static Map<String, List<String>> UNMIGRATED_PROPERTIES_PER_ID = Map.of(
			"string", Arrays.asList("minVal", "maxVal"),
			"datetime", Arrays.asList("minVal", "maxVal", "maxCharCount"),
			"time", Arrays.asList("minVal", "maxVal", "maxCharCount"),
			"number", Arrays.asList("maxCharCount"));

	/**
	 * Map of property id to propertyPath settings
	 */
	protected final static Map<String, String> PROPERTY_PATH_MAP = Map.of(
			"minValue", "validation.minValue",
			"maxValue", "validation.maxValue",
			"maxLength", "validation.maxLength");

	/**
	 * Migration map for property ids
	 */
	public final static Map<String, String> PROPERTY_MIGRATION_MAP = Map.of(
			"additional_element", "customOption",
			"max_filesize", "maxFileSize",
			"options", "selectOptions",
			"value", "defaultValue",
			"minVal", "minValue",
			"maxVal", "maxValue",
			"maxCharCount", "maxLength");

	/**
	 * List of property ids, which are backend properties (have the backend flag set)
	 */
	public final static List<String> BACKEND_PROPERTIES = Arrays.asList("minValue", "maxValue", "maxLength", "maxFileSize", "allowedFiletypes",
			"mailsubject", "mailtemplate", "mailsource", "defaultValue", "selectOptions");

	/**
	 * List of property ids, which should be migrated to "string" instead of "translation"
	 */
	public final static List<String> STRING_PROPERTIES = Arrays.asList("defaultValue");

	/**
	 * List of unmigrated element types
	 */
	protected final static List<String> UNMIGRATED_ELEMENT_TYPES = Arrays.asList("textarea", "buttons", "formpage", "formprogress");

	/**
	 * Migration map for element types
	 */
	protected final static Map<String, String> ELEMENT_TYPE_MIGRATION_MAP = Map.of(
			"input", "string",
			"checkbox", "boolean",
			"selectgroup", "catalog",
			"file", "binary");

	/**
	 * Map of form types to configuration files (expected to reside in the ui-conf directory)
	 */
	protected final static Map<String, String> CONFIG_FILENAMES = Map.of("generic", "form-editor.json", "poll",
			"form-poll-editor.json");

	/**
	 * Names of the form type configurations
	 */
	protected final static Map<String, Map<String, String>> NAMES = Map.of(
			"generic", Map.of("de", "Standard", "en", "Generic"),
			"poll", Map.of("de", "Umfrage", "en", "Poll"));

	/**
	 * Name of the environment variable containing the ui-conf path
	 */
	protected final static String UI_CONF_PATH_ENV = "UI_CONF_PATH";

	/**
	 * Name of the system property containing the ui-conf path
	 */
	protected final static String UI_CONF_PATH_PROP = "com.gentics.contentnode.ui.conf_path";

	/**
	 * Default value for the ui-conf path
	 */
	protected final static String UI_CONF_PATH_DEFAULT = "ui-conf";

	/**
	 * Pattern for migration of the telephone number validation
	 */
	protected final static String TELEPHONE_PATTERN = "^[\\s\\d\\+\\/]*$";

	/**
	 * Pattern for migration of the social security number validation
	 */
	protected final static String SOCIAL_SECURITY_PATTERN = "^[1-9]\\d{9}$";

	/**
	 * Pattern for migration of the email validation
	 */
	protected final static String EMAIL_PATTERN = "^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";

	/**
	 * Flag to enable verbose output
	 */
	protected boolean verbose;

	/**
	 * Current file name
	 */
	protected String currentFileName;

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
	 * Object mapper instance used to parse the input files
	 */
	protected ObjectMapper mapper = new ObjectMapper();

	/**
	 * YAML Mapper instance for writing the output
	 */
	protected YAMLMapper yamlMapper = new YAMLMapper().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);

	/**
	 * Main method
	 * @param args arguments
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		Options options = createOptions();

		try {
			line = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Invalid arguments found: " + e.getMessage());
			e.printStackTrace(System.err);
			System.exit(-1);
		}

		// empty line or help message
		if (line == null) {
			printHelpAndExit(options);
		}
		if (line.hasOption("help")) {
			printHelpAndExit(options);
		}

		if (!line.hasOption("out")) {
			System.err.println("Missing option -out");
			printHelpAndExit(options);
		}

		File outFile = new File(line.getOptionValue("out"));

		// if the file is not empty, we need the overwrite flag
		if (outFile.exists() && outFile.isFile() && outFile.length() > 0 && !line.hasOption("overwrite")) {
			System.err.println(
					"Output file %s is not empty, but missing option -overwrite".formatted(outFile.getAbsolutePath()));
			printHelpAndExit(options);
		}

		// do the migration
		Migration migration = new Migration(line.hasOption("verbose"));
		if (line.hasOption("custom")) {
			migration.parseScript(new File(line.getOptionValue("custom")));
		}
		migration.doMigration(outFile, line);
	}

	/**
	 * Private helper method to print the help screen and exit
	 */
	static private void printHelpAndExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("Migration", options);
		System.exit(0);
	}

	/**
	 * Create cli options
	 * @return cli options
	 */
	static private Options createOptions() {
		Options options = new Options();
		options.addOption("ui_conf", true, "Path to the ui-conf directory (containing the files to be migrated)");
		options.addOption("out", true, "Output file");
		options.addOption("overwrite", false, "Must be set to overwrite existing output files");
		options.addOption("verbose", false, "Verbose output");
		options.addOption("custom", true, "Custom migration groovy script");
		options.addOption("help", false, "Help");
		return options;
	}

	/**
	 * Get the configured ui-conf path
	 * @param line command line parameters
	 * @return path
	 */
	static private String getUiConfPath(CommandLine line) {
		String value = null;
		if (line.hasOption("ui_conf")) {
			value = line.getOptionValue("ui_conf");
		}
		if (StringUtils.isBlank(value)) {
			value = System.getenv(UI_CONF_PATH_ENV);
		}
		if (StringUtils.isBlank(value)) {
			value = System.getProperty(UI_CONF_PATH_PROP);
		}
		if (StringUtils.isBlank(value)) {
			value = UI_CONF_PATH_DEFAULT;
		}

		return value;
	}

	/**
	 * Create an instance
	 * @param verbose true for verbose output
	 */
	public Migration(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Parse the custom migration script
	 * @param scriptFile
	 * @throws CompilationFailedException
	 * @throws IOException
	 */
	public void parseScript(File scriptFile) throws CompilationFailedException, IOException {
		sharedData = new Binding();
		shell = new GroovyShell(sharedData);
		customMigrationScript = shell.parse(scriptFile);
	}

	/**
	 * Do the migration
	 * @param out output file
	 * @param line command line parameters
	 * @throws IOException
	 */
	public void doMigration(File out, CommandLine line) throws IOException {
		String uiConfPath = getUiConfPath(line);
		FormTypeConfigurationMap configMap = new FormTypeConfigurationMap();

		for (String formType : FORM_TYPES) {
			String fileName = CONFIG_FILENAMES.get(formType);
			File configFile = new File(uiConfPath, fileName);
			if (configFile.exists() && configFile.isFile()) {
				configMap.put(formType, migrate(formType, configFile));
			}
		}

		try (FileOutputStream fileOut = new FileOutputStream(out)) {
			yamlMapper.writeValue(fileOut, Map.of("forms", Map.of("types", configMap)));
		}
	}

	/**
	 * Migrate the configuration from the given file for the given form type
	 * @param formType form type
	 * @param configFile file containing the configuration
	 * @return migrated configuration
	 * @throws IOException
	 */
	public FormTypeConfiguration migrate(String formType, File configFile) throws IOException {
		currentFileName = configFile.getName();
		if (verbose) {
			System.out.println("Migrating file %s to form type configuration '%s'".formatted(configFile.getAbsolutePath(), formType));
		}

		return migrate(formType, mapper.readTree(configFile));
	}

	/**
	 * Migrate the configuration from the given JsonNode
	 * @param formType form type
	 * @param config old configuration
	 * @return migrated configuration
	 * @throws IOException
	 */
	public FormTypeConfiguration migrate(String formType, JsonNode config) throws IOException {
		// basic data (name, pluginName, external, submitOnce)
		FormTypeConfiguration out = new FormTypeConfiguration(NAMES.get(formType), "forms");
		out.external = false;
		out.submitOnce = Strings.CI.equals(formType, "poll");

		config.optional("form_properties")
				.flatMap(formProperties -> formProperties.optional("admin_mail_options"))
				.ifPresent(adminMailOptions -> out.adminEmailTemplateOptions = toSelectOptions(adminMailOptions));

		config.optional("form_properties")
				.flatMap(formProperties -> formProperties.optional("template_context_options"))
				.ifPresent(templateContextOptions -> out.templateOptions = toSelectOptions(templateContextOptions));

		out.blocks = new HashMap<>();
		out.controls = new HashMap<>();

		// controls/blocks
		config.optional("elements").filter(elements -> elements.isArray())
				.ifPresent(elements -> {
					elements.valueStream().forEach(element -> {
						if (verbose) {
							System.out.println("%s: Migrating element '%s'".formatted(currentFileName, element.toString()));
						}
						element.optional("type").ifPresentOrElse(typeNode -> {
							String key = typeNode.isTextual() ? typeNode.asText() : null;
							if (StringUtils.isBlank(key)) {
								System.err.println("%s: Ignoring element with empty type: '%s'".formatted(currentFileName, element.toString()));
							} else if (isBlock(key, element)) {
								migrate(key, element, e -> toBlock(key, e, out.blocks));
							} else {
								migrate(key, element, e -> toControl(key, e, out.controls));
							}
						}, () -> {
							System.err.println("%s: Ignoring element without type: '%s'".formatted(currentFileName, element.toString()));
						});
					});
				});

		return out;
	}

	/**
	 * Migrate the property of the node to a list of SelectOptions
	 * @param node node
	 * @param property name of the property
	 * @param warnIfEmpty warn if the property is empty
	 * @return migrated list or null
	 */
	protected List<SelectOption> getSelectOptions(JsonNode node, String property, boolean warnIfEmpty) {
		JsonNode propertyNode = node.get(property);
		if (warnIfEmpty && (propertyNode == null || !propertyNode.isArray())) {
			System.err.println("%s: Property %s of '%s' must be an array.".formatted(currentFileName, property, node.toString()));
		}
		return toSelectOptions(propertyNode);
	}

	/**
	 * Migrate the given node to a list of SelectOptions
	 * @param node node
	 * @return migrated list or null
	 */
	protected List<SelectOption> toSelectOptions(JsonNode node) {
		if (node != null && node.isArray()) {
			return node.valueStream()
					.map(option -> new SelectOption(option.get("key").asText(), toTranslationMap(option.get("value_i18n_ui"))))
					.collect(Collectors.toList());
		} else {
			return null;
		}
	}

	/**
	 * Migrate the property of the node to a translation map
	 * @param node node
	 * @param property name of the property
	 * @param warnIfEmpty warn if the property is empty
	 * @return migrated map (may be empty)
	 */
	protected Map<String, String> getTranslationMap(JsonNode node, String property, boolean warnIfEmpty) {
		JsonNode propertyNode = node.get(property);
		if (warnIfEmpty && (propertyNode == null || ! propertyNode.isObject())) {
			System.err.println("%s: Property %s of '%s' must be a translation map.".formatted(currentFileName, property, node.toString()));
		}
		return toTranslationMap(propertyNode);
	}

	/**
	 * Migrate the given node to a translation map
	 * @param node node
	 * @return migrated map (may be empty)
	 */
	protected Map<String, String> toTranslationMap(JsonNode node) {
		Map<String, String> map = new TreeMap<>();
		if (node != null && node.isObject()) {
			node.propertyStream().forEach(entry -> {
				map.put(entry.getKey(), entry.getValue().asText(""));
			});
		}
		return map;
	}

	/**
	 * Check whether the given element of the given type shall be migrated to a block
	 * @param type element type
	 * @param element element
	 * @return true for blocks
	 */
	protected boolean isBlock(String type, JsonNode element) {
		/**
		 * Containers are blocks
		 */
		if (getBoolean(element, "is_container")) {
			return true;
		}

		/**
		 * Some known types are considered to be blocks
		 */
		switch (type) {
		case "sectionheadline":
		case "text":
		case "spacer":
		case "group":
		case "conditionalcontainer":
			return true;
		default:
			return false;
		}
	}

	/**
	 * Migrate the element to a control. Omit elements with keys listed in {@link #UNMIGRATED_ELEMENT_TYPES}.
	 * If a {@link #customMigrationScript} is present, first call the script with the element data.
	 * @param key element key
	 * @param element element to migrate
	 * @param migrator migrator
	 */
	protected void migrate(String key, JsonNode element, Consumer<JsonNode> migrator) {
		if (UNMIGRATED_ELEMENT_TYPES.contains(key)) {
			return;
		}
		if (customMigrationScript != null) {
			sharedData.getVariables().clear();
			sharedData.setVariable("verbose", verbose);
			sharedData.setVariable("element", new JsonSlurper().parseText(element.toString()));
			boolean migrate = ObjectTransformer.getBoolean(customMigrationScript.run(), true);
			if (!migrate) {
				return;
			}
			try {
				element = mapper.readTree(JsonOutput.toJson(sharedData.getVariable("element")));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
		migrator.accept(element);
	}

	/**
	 * Migrate the given element to a block
	 * @param key type key
	 * @param element element
	 * @param blocks map of blocks
	 */
	protected void toBlock(String key, JsonNode element, Map<String, BlockConfig> blocks) {
		BlockConfig block = new BlockConfig(getTranslationMap(element, "type_label_i18n_ui", true));
		fillElementConfig(key, block, element);
		block.container = getBoolean(element, "is_container");
		blocks.put(key, block);
	}

	/**
	 * Migrate the given element to a control (or probable multiple controls)
	 * @param key type key
	 * @param element element
	 * @param controls map of controls
	 */
	protected void toControl(String key, JsonNode element, Map<String, ControlConfig> controls) {
		key = ELEMENT_TYPE_MIGRATION_MAP.getOrDefault(key, key);
		ControlConfig control = new ControlConfig(getTranslationMap(element, "type_label_i18n_ui", true));
		fillElementConfig(key, control, element);
		controls.put(key, control);

		// some types require special handling
		switch (key) {
		case "string":
			SelectSetting validationSetting = new SelectSetting("validation", Collections.emptyMap(),
					new ArrayList<>());
			validationSetting.backend = true;
			validationSetting.propertyPath = "validation.regexValidation";

			element.optional("properties").filter(JsonNode::isArray).ifPresent(properties -> {
				properties.valueStream()
						.filter(el -> el.optional("name").filter(JsonNode::isTextual).map(JsonNode::asText)
								.filter(name -> Strings.CI.equals(name, "validation")).isPresent())
						.findFirst().ifPresent(validation -> {
							validationSetting.labelI18n = getTranslationMap(validation, "label_i18n_ui", true);

							validation.optional("options").filter(JsonNode::isArray).ifPresent(options -> {
								for (JsonNode option : options) {
									String optionKey = option.optional("key").filter(JsonNode::isTextual)
											.map(JsonNode::asText).orElse("");
									switch (optionKey) {
									case "number":
										ControlConfig numberControl = new ControlConfig(getTranslationMap(option, "value_i18n_ui", true));
										fillElementConfig("number", numberControl, element);
										controls.put("number", numberControl);
										break;
									case "telephone":
										validationSetting.options
												.add(createRegexValidationOption(option, TELEPHONE_PATTERN));
										break;
									case "socialsecurity":
										validationSetting.options
												.add(createRegexValidationOption(option, SOCIAL_SECURITY_PATTERN));
										break;
									case "date":
										ControlConfig dateTimeControl = new ControlConfig(getTranslationMap(option, "value_i18n_ui", true));
										fillElementConfig("datetime", dateTimeControl, element);
										controls.put("datetime", dateTimeControl);
										break;
									case "datepicker":
										break;
									case "time":
										ControlConfig timeControl = new ControlConfig(getTranslationMap(option, "value_i18n_ui", true));
										fillElementConfig("time", timeControl, element);
										controls.put("time", timeControl);
										break;
									case "email":
										validationSetting.options
												.add(createRegexValidationOption(option, EMAIL_PATTERN));
										break;
									}
								}
							});
						});
			});

			if (!validationSetting.options.isEmpty()) {
				if (control.settings == null) {
					control.settings = new ArrayList<>();
				}
				control.settings.add(validationSetting);
			}

			break;
		}
	}

	/**
	 * Transform the given option into a {@link SelectOption}
	 * @param option option to transform
	 * @param defaultRegex default regex, if none was stored in the option
	 * @return SelectOption instance
	 */
	protected SelectOption createRegexValidationOption(JsonNode option, String defaultRegex) {
		ObjectNode value = mapper.createObjectNode();
		value.put("key", option.optional("key").map(JsonNode::asText).orElse(null));
		value.put("regex",
				option.optional("regex").filter(JsonNode::isTextual).map(JsonNode::asText).orElse(defaultRegex));
		value.set("errorMessage", option.get("errorMessage"));

		return new SelectOption(value, getTranslationMap(option, "value_i18n_ui", true));
	}

	/**
	 * Fill the common ElementConfig properties from the source into the given element
	 * @param key element key
	 * @param element element to modify
	 * @param source source
	 */
	protected void fillElementConfig(String key, ElementConfig element, JsonNode source) {
		source.optional("description_i18n_ui").map(this::toTranslationMap)
				.ifPresent(description -> element.descriptionI18n = description);
		source.optional("properties").filter(properties -> properties.isArray()).ifPresent(properties -> {
			element.settings = properties.valueStream().map(s -> toSetting(key, s)).filter(setting -> setting != null)
					.collect(Collectors.toList());
		});
	}

	/**
	 * Migrate the given property to setting
	 * @param elementType for which the setting is defined
	 * @param property property to migrate
	 * @return setting
	 */
	protected BaseSetting toSetting(String elementType, JsonNode property) {
		String type = property.optional("type").map(JsonNode::asText).orElse(null).toLowerCase();
		if (StringUtils.isBlank(type)) {
			System.err.println("%s: Ignoring property without type '%s'".formatted(property.toString()));
			return null;
		}
		String id = property.optional("name").map(JsonNode::asText).orElse(null);
		if (StringUtils.isBlank(id)) {
			System.err.println("%s: Ignoring property without id '%s'".formatted(property.toString()));
			return null;
		}

		if (UNMIGRATED_PROPERTIES.contains(id)) {
			return null;
		}

		if (UNMIGRATED_PROPERTIES_PER_ID.getOrDefault(elementType, Collections.emptyList()).contains(id)) {
			return null;
		}

		id = PROPERTY_MIGRATION_MAP.getOrDefault(id, id);

		Map<String, String> label = getTranslationMap(property, "label_i18n_ui", true);
		BaseSetting setting = null;

		switch (type) {
		case "string":
			if (STRING_PROPERTIES.contains(id)) {
				StringSetting stringSetting = new StringSetting(id, label);
				stringSetting.richContent = getBoolean(property, "allow_rich_content");
				setting = stringSetting;
			} else {
				TranslationSetting translationSetting = new TranslationSetting(id, label);
				translationSetting.richContent = getBoolean(property, "allow_rich_content");
				setting = translationSetting;
			}
			break;
		case "boolean":
			setting = new BooleanSetting(id, label);
			break;
		case "select":
			SelectSetting selectSetting = new SelectSetting(id, label, getSelectOptions(property, "options", true));
			selectSetting.multiple = getBoolean(property, "multiple");
			setting = selectSetting;
			break;
		case "number":
			setting = new NumberSetting(id, label);
			break;
		case "selectable_options":
			OptionsSetting optionsSetting = new OptionsSetting(id, label);
			optionsSetting.multiple = getBoolean(property, "multiple");
			setting = optionsSetting;
			break;
		case "repository_browser":
			ReferenceSetting referenceSetting = new ReferenceSetting(id, label,
					getReferenceTypes(property, true, "options", "allowedSelection"), PublishAsType.content);

			referenceSetting.multiple = getBoolean(property, "options", "selectMultiple");
			referenceSetting.onlyInCurrentNode = getBoolean(property, "options", "onlyInCurrentNode");

			property.optional("options").ifPresent(options -> {
				options.optional("includeMlId").filter(includeMlId -> includeMlId.isArray()).ifPresent(includeMlId -> {
					referenceSetting.allowedMarkupLanguageIds = includeMlId.valueStream()
							.filter(ml -> ml.isIntegralNumber()).map(ml -> ml.asInt()).collect(Collectors.toSet());
				});

				options.optional("title").filter(title -> title.isTextual()).ifPresent(title -> {
					referenceSetting.titleI18n = Map.of("de", title.asText(), "en", title.asText());
				});
			});

			setting = referenceSetting;
			break;
		default:
			System.err.println("%s: Ignoring property with unknown type '%s'".formatted(currentFileName, property.toString()));
			break;
		}

		if (setting != null) {
			setting.descriptionI18n = toTranslationMap(property.get("description_i18n_ui"));
			setting.backend = BACKEND_PROPERTIES.contains(id);
			setting.propertyPath = PROPERTY_PATH_MAP.get(id);
		}

		return setting;
	}

	/**
	 * Get the given property from the node as boolean (with false as default)
	 * @param node node
	 * @param properties name of the property
	 * @return boolean value
	 */
	protected boolean getBoolean(JsonNode node, String...properties) {
		return getBoolean(node, false, properties);
	}

	/**
	 * Get the given property from the node as boolean or the given default value
	 * @param node node
	 * @param defaultValue default value
	 * @param properties name of the property
	 * @return boolean value
	 */
	protected boolean getBoolean(JsonNode node, boolean defaultValue, String...properties) {
		JsonNode propNode = node;
		for (String property : properties) {
			propNode = propNode.get(property);
			if (propNode == null || propNode.isNull()) {
				return defaultValue;
			}
		}
		if (propNode.isBoolean()) {
			return propNode.asBoolean();
		} else {
			return defaultValue;
		}
	}

	protected Set<ReferenceType> getReferenceTypes(JsonNode node, boolean warnIfEmpty, String...properties) {
		JsonNode refNode = node;
		String combinedProperty = Streams.of(properties).collect(Collectors.joining("."));
		for (String property : properties) {
			refNode = refNode.get(property);
			if (refNode == null || refNode.isNull()) {
				if (warnIfEmpty) {
					System.err.println("%s. Property '%s' of '%s' must be a single string or an array of strings"
							.formatted(currentFileName, combinedProperty, node.toString()));
				}
				return null;
			}
		}

		if (refNode.isTextual()) {
			// just a single value
			ReferenceType refType = toReferenceType(refNode.asText(), node);
			if (refType != null) {
				return Collections.singleton(refType);
			} else {
				return null;
			}
		} else if (refNode.isArray()) {
			return refNode.valueStream().filter(ref -> ref.isTextual()).map(ref -> toReferenceType(ref.asText(), node))
					.filter(refType -> refType != null).collect(Collectors.toSet());
		} else {
			return null;
		}
	}

	protected ReferenceType toReferenceType(String string, JsonNode node) {
		try {
			return ReferenceType.valueOf(string);
		} catch (IllegalArgumentException e) {
			System.err.println("%s. Unknown selection type '%s' in '%s'".formatted(currentFileName, string, node.toString()));
			return null;
		}
	}
}
