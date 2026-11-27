package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.gentics.contentnode.rest.model.formtype.BaseSetting;
import com.gentics.contentnode.rest.model.formtype.ElementConfig;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;
import com.gentics.contentnode.rest.model.formtype.OptionsSetting;
import com.gentics.contentnode.rest.model.formtype.SelectSetting;
import com.gentics.contentnode.rest.model.formtype.TranslationSetting;

/**
 * Names, completes and checks a form's definition ({@code data}) against its form type, before create_form and
 * update_form save it. The CMS itself stores the definition as sent.
 */
final class FormDefinitions {
	static final String PLACEHOLDER_PREFIX = "new:";

	static final String SCHEMA = "schema";

	static final String UI_SCHEMA = "ui-schema";

	static final String VERSION = "1.0";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private FormDefinitions() {
	}

	/**
	 * Name the placeholders, fill in the keys and the version, and check the result against the form type
	 * @param data definition as given
	 * @param type configuration of the form's type
	 * @param languages the form's languages
	 * @return definition to save
	 * @throws IllegalArgumentException listing every reason the definition does not fit
	 */
	static JsonNode prepare(JsonNode data, FormTypeConfigurationModel type, List<String> languages) {
		List<String> reasons = new ArrayList<>();
		if (data == null || !data.isObject() || !data.path(SCHEMA).isObject() || !data.path(UI_SCHEMA).isObject()) {
			reasons.add("data must be an object with the objects schema and ui-schema");
		} else {
			data = name(data, reasons);
			fill((ObjectNode) data);
			check(data, type, languages, reasons);
		}
		if (!reasons.isEmpty()) {
			throw new IllegalArgumentException("data does not fit form type '%s': %s".formatted(type.type,
					String.join("; ", reasons)));
		}
		return data;
	}

	/**
	 * Replace every new: placeholder by {@code <control or block key>_<UUID v4 without dashes>}, consistently in
	 * keys and string values. Other names are kept.
	 * @param data definition
	 * @param reasons collects placeholders that cannot be named
	 * @return renamed copy of the definition
	 */
	static JsonNode name(JsonNode data, List<String> reasons) {
		Map<String, String> names = new HashMap<>();
		for (JsonNode element : elements(data)) {
			String id = element.path("id").asText("");
			if (id.startsWith(PLACEHOLDER_PREFIX) && !names.containsKey(id)) {
				String gridType = element.path("formGridOptions").path("type").asText("");
				if (gridType.isBlank()) {
					reasons.add("element '%s' has no formGridOptions.type to name it by".formatted(id));
				} else {
					names.put(id, gridType + "_" + UUID.randomUUID().toString().replace("-", ""));
				}
			}
		}
		for (String key : flatProperties(data).keySet()) {
			if (key.startsWith(PLACEHOLDER_PREFIX) && !names.containsKey(key)) {
				reasons.add("schema property '%s' has no ui-schema element telling its type".formatted(key));
			}
		}
		return rename(data, names);
	}

	/**
	 * Copy a JSON tree, replacing keys and string values that are placeholders
	 * @param node node
	 * @param names placeholder to name
	 * @return copy
	 */
	private static JsonNode rename(JsonNode node, Map<String, String> names) {
		if (node.isTextual()) {
			String name = names.get(node.asText());
			return name != null ? TextNode.valueOf(name) : node;
		} else if (node.isObject()) {
			ObjectNode copy = MAPPER.createObjectNode();
			for (Entry<String, JsonNode> field : node.properties()) {
				copy.set(names.getOrDefault(field.getKey(), field.getKey()), rename(field.getValue(), names));
			}
			return copy;
		} else if (node.isArray()) {
			ArrayNode copy = MAPPER.createArrayNode();
			node.forEach(item -> copy.add(rename(item, names)));
			return copy;
		}
		return node;
	}

	/**
	 * Fill in schema.key and ui-schema.key (one UUID, shared) and their version, where missing
	 * @param data definition
	 */
	private static void fill(ObjectNode data) {
		ObjectNode schema = (ObjectNode) data.get(SCHEMA);
		ObjectNode uiSchema = (ObjectNode) data.get(UI_SCHEMA);
		String key = StringUtils.firstNonBlank(schema.path("key").asText(null), uiSchema.path("key").asText(null),
				UUID.randomUUID().toString());
		for (ObjectNode part : List.of(schema, uiSchema)) {
			if (StringUtils.isBlank(part.path("key").asText(null))) {
				part.put("key", key);
			}
			if (StringUtils.isBlank(part.path("version").asText(null))) {
				part.put("version", VERSION);
			}
		}
	}

	/**
	 * Check a named definition against the form type
	 * @param data definition
	 * @param type form type configuration
	 * @param languages the form's languages
	 * @param reasons collects every reason it does not fit
	 */
	static void check(JsonNode data, FormTypeConfigurationModel type, List<String> languages, List<String> reasons) {
		Map<String, ? extends ElementConfig> controls = type.controls != null ? type.controls : Map.of();
		Map<String, ? extends ElementConfig> blocks = type.blocks != null ? type.blocks : Map.of();

		Map<String, JsonNode> properties = flatProperties(data);
		for (Entry<String, JsonNode> property : properties.entrySet()) {
			String propertyType = property.getValue().path("type").asText("");
			if (!controls.containsKey(propertyType)) {
				reasons.add("schema property '%s' has type '%s', which is not a control of the form type"
						.formatted(property.getKey(), propertyType));
			}
			String name = property.getValue().path("name").asText(null);
			if (name != null && !name.equals(property.getKey())) {
				reasons.add("schema property '%s' has the name '%s'".formatted(property.getKey(), name));
			}
		}

		JsonNode pages = data.path(UI_SCHEMA).path("pages");
		for (int i = 0; i < pages.size(); i++) {
			if (!pages.get(i).path("pagename").isObject()) {
				reasons.add("page %d has no pagename (an object of language code to text)".formatted(i));
			}
			checkTexts(pages.get(i), "pagename", "page %d".formatted(i), languages, reasons);
		}

		Set<String> controlIds = new HashSet<>();
		for (JsonNode element : elements(data)) {
			String id = element.path("id").asText("");
			String gridType = element.path("formGridOptions").path("type").asText("");
			String where = "element '%s'".formatted(id);
			if (id.isBlank()) {
				reasons.add("an element of type '%s' has no id".formatted(gridType));
			}
			checkTexts(element, "label", where, languages, reasons);
			checkTexts(element, "description", where, languages, reasons);

			JsonNode property = properties.get(id);
			if (controls.containsKey(gridType)) {
				controlIds.add(id);
				if (property == null) {
					reasons.add("%s is a control without a schema property of the same name".formatted(where));
				}
				checkSettings(controls.get(gridType), property, element, where, languages, reasons);
			} else if (!blocks.containsKey(gridType)) {
				reasons.add("%s has formGridOptions.type '%s', which is neither a control nor a block of the form type"
						.formatted(where, gridType));
			}
		}

		for (String name : properties.keySet()) {
			if (!controlIds.contains(name)) {
				reasons.add("schema property '%s' has no ui-schema control element".formatted(name));
			}
		}
	}

	/**
	 * Check that a text object, where present, has a text for every language
	 * @param owner object holding the text object
	 * @param field name of the text object
	 * @param where description of the owner for a reason
	 * @param languages languages
	 * @param reasons reasons
	 */
	private static void checkTexts(JsonNode owner, String field, String where, List<String> languages,
			List<String> reasons) {
		JsonNode texts = owner.get(field);
		if (texts == null || texts.isNull()) {
			return;
		}
		checkTextObject(texts, "%s: %s".formatted(where, field), languages, reasons);
	}

	/**
	 * Check that a text object has a text for every language
	 * @param texts text object
	 * @param what description of the text object for a reason
	 * @param languages languages
	 * @param reasons reasons
	 */
	private static void checkTextObject(JsonNode texts, String what, List<String> languages, List<String> reasons) {
		if (!texts.isObject()) {
			reasons.add("%s must be an object of language code to text".formatted(what));
			return;
		}
		for (String language : languages) {
			if (texts.path(language).asText("").isBlank()) {
				reasons.add("%s has no text for language '%s'".formatted(what, language));
			}
		}
	}

	/**
	 * Check an options setting's value: a list of {value, label}, value a non-blank text, label a text object
	 * @param options value of the setting
	 * @param what description of the setting for a reason
	 * @param languages languages
	 * @param reasons reasons
	 */
	private static void checkOptions(JsonNode options, String what, List<String> languages, List<String> reasons) {
		if (!options.isArray()) {
			reasons.add("%s must be a list of options {value, label}".formatted(what));
			return;
		}
		for (int i = 0; i < options.size(); i++) {
			JsonNode option = options.get(i);
			String which = "%s, option %d".formatted(what, i);
			if (!option.path("value").isTextual() || option.path("value").asText().isBlank()) {
				reasons.add("%s needs a value (the text stored on submission)".formatted(which));
			}
			if (!option.has("label")) {
				reasons.add("%s needs a label (an object of language code to text)".formatted(which));
			} else {
				checkTextObject(option.get("label"), "%s: label".formatted(which), languages, reasons);
			}
		}
	}

	/**
	 * Where the form editor keeps a setting's value: at its propertyPath, else at formGridOptions.&lt;id&gt;, in the
	 * control's schema property when the setting is a backend setting, else in its ui-schema element
	 * @param setting setting
	 * @return root ("schema property" or "ui-schema element") and path segments, null when the path is unusable
	 */
	private static Pair<Boolean, List<String>> place(BaseSetting setting) {
		String path = StringUtils.isNotBlank(setting.propertyPath) ? setting.propertyPath : "formGridOptions." + setting.id;
		if (setting.id == null || !path.matches("[\\w-]+(\\.[\\w-]+)*")) {
			return null;
		}
		return Pair.of(setting.backend, List.of(path.split("\\.")));
	}

	private static JsonNode at(JsonNode root, List<String> path) {
		JsonNode value = root;
		for (String segment : path) {
			value = value.path(segment);
		}
		return value;
	}

	private static String describe(boolean backend, List<String> path) {
		return "%s.%s".formatted(backend ? "the schema property" : "the ui-schema element", String.join(".", path));
	}

	/**
	 * Check a control's settings as the form editor stores them: each value in its place (see {@link #place}),
	 * options as a list of {value, label} with a label for every language, translations with a text for every
	 * language, and select values among their options
	 * @param config control configuration
	 * @param property schema property, may be null
	 * @param element ui-schema element
	 * @param where description of the element for a reason
	 * @param languages the form's languages
	 * @param reasons reasons
	 */
	private static void checkSettings(ElementConfig config, JsonNode property, JsonNode element, String where,
			List<String> languages, List<String> reasons) {
		if (config == null || config.settings == null || property == null) {
			return;
		}
		for (BaseSetting setting : config.settings) {
			Pair<Boolean, List<String>> place = place(setting);
			if (place == null) {
				continue;
			}
			boolean backend = place.getLeft();
			List<String> path = place.getRight();
			for (Pair<Boolean, List<String>> wrong : List.of(Pair.of(true, List.of(setting.id)),
					Pair.of(true, List.of("formGridOptions", setting.id)), Pair.of(false, List.of(setting.id)),
					Pair.of(false, List.of("formGridOptions", setting.id)))) {
				// the place itself, or an object on its way (validation for validation.regexValidation)
				List<String> other = wrong.getRight();
				if (wrong.getLeft() == backend && other.size() <= path.size()
						&& path.subList(0, other.size()).equals(other)) {
					continue;
				}
				JsonNode misplaced = at(wrong.getLeft() ? property : element, wrong.getRight());
				if (!misplaced.isMissingNode() && !misplaced.isNull()) {
					reasons.add("%s: setting '%s' is at %s, but belongs at %s".formatted(where, setting.id,
							describe(wrong.getLeft(), wrong.getRight()), describe(backend, path)));
				}
			}
			JsonNode value = at(backend ? property : element, path);
			if (value.isMissingNode() || value.isNull()) {
				continue;
			}
			if (setting instanceof OptionsSetting) {
				checkOptions(value, "%s: setting '%s'".formatted(where, setting.id), languages, reasons);
				continue;
			}
			if (setting instanceof TranslationSetting) {
				checkTextObject(value, "%s: setting '%s'".formatted(where, setting.id), languages, reasons);
				continue;
			}
			if (!(setting instanceof SelectSetting select) || select.options == null) {
				continue;
			}
			List<JsonNode> values = new ArrayList<>();
			if (value.isArray() && select.multiple) {
				value.forEach(values::add);
			} else {
				values.add(value);
			}
			for (JsonNode chosen : values) {
				if (select.options.stream().noneMatch(option -> MAPPER.valueToTree(option.value).equals(chosen))) {
					reasons.add("%s: setting '%s' has the value %s, which is not one of its options"
							.formatted(where, setting.id, chosen));
				}
			}
		}
	}

	/**
	 * Get every ui-schema element, those inside containers included
	 * @param data definition
	 * @return elements
	 */
	static List<JsonNode> elements(JsonNode data) {
		List<JsonNode> elements = new ArrayList<>();
		for (JsonNode page : data.path(UI_SCHEMA).path("pages")) {
			addElements(page.path("elements"), elements);
		}
		return elements;
	}

	private static void addElements(JsonNode list, List<JsonNode> elements) {
		for (JsonNode element : list) {
			elements.add(element);
			addElements(element.path("elements"), elements);
		}
	}

	/**
	 * Get every schema property by its key, those nested in an aggregate's properties included
	 * @param data definition
	 * @return key to property
	 */
	static Map<String, JsonNode> flatProperties(JsonNode data) {
		Map<String, JsonNode> properties = new LinkedHashMap<>();
		addProperties(data.path(SCHEMA).path("properties"), properties);
		return properties;
	}

	private static void addProperties(JsonNode node, Map<String, JsonNode> properties) {
		for (Entry<String, JsonNode> field : node.properties()) {
			properties.put(field.getKey(), field.getValue());
			addProperties(field.getValue().path("properties"), properties);
		}
	}
}
