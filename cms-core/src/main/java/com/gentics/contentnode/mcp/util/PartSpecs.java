package com.gentics.contentnode.mcp.util;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

import com.gentics.contentnode.mcp.McpArgs;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectSetting;

/**
 * The parts of constructs created or updated by MCP tools: parsing, the rules the CMS does not check (part type
 * allowlist, one Handlebars part, unique keywords, datasources of select parts) and the mapping to REST parts
 */
public final class PartSpecs {
	/**
	 * Name of the parts argument
	 */
	public static final String ARG_PARTS = "parts";

	/**
	 * Maximum number of parts
	 */
	public static final int MAX_PARTS = 50;

	/**
	 * Maximum length of a keyword
	 */
	public static final int MAX_KEYWORD_LENGTH = 100;

	/**
	 * Pattern of construct and part keywords
	 */
	public static final Pattern KEYWORD = Pattern.compile("^[a-z][a-z0-9_]*$");

	/**
	 * Part types that generated constructs may use: text, HTML, links, checkbox and selects. Not configurable yet,
	 * see {@code list_part_types}.
	 */
	public static final Set<Integer> ALLOWED_TYPES = Set.of(Part.TEXT, Part.TEXTHMTL, Part.HTML, Part.URLPAGE,
			Part.URLIMAGE, Part.URLFILE, Part.TEXTSHORT, Part.TEXTHTMLLONG, Part.HTMLLONG, Part.URLFOLDER,
			Part.SELECTSINGLE, Part.SELECTMULTIPLE, Part.CHECKBOX, Part.FILEUPLOAD, Part.FOLDERUPLOAD);

	/**
	 * Part types that need a datasource
	 */
	public static final Set<Integer> DATASOURCE_TYPES = Set.of(Part.SELECTSINGLE, Part.SELECTMULTIPLE);

	private static final Set<String> MEMBERS = Set.of("keyword", "name", "typeId", "datasourceId", "editable",
			"liveEditable", "mandatory", "hidden", "partOrder", "defaultValue");

	private PartSpecs() {
	}

	/**
	 * A part as given to a tool
	 * @param keyword keyword
	 * @param name name per UI language
	 * @param typeId part type ID
	 * @param datasourceId datasource of a select part
	 * @param editable whether the part is editable
	 * @param liveEditable whether the part is live editable
	 * @param mandatory whether the part is mandatory
	 * @param hidden whether the part is hidden
	 * @param partOrder part order, null for the position in the list
	 * @param defaultValue default value, a string or boolean
	 */
	public record PartSpec(String keyword, Map<String, String> name, int typeId, Integer datasourceId,
			boolean editable, boolean liveEditable, boolean mandatory, boolean hidden, Integer partOrder,
			Object defaultValue) {
	}

	/**
	 * Parse the parts argument. Checks each part on its own, {@link #validate} checks the list.
	 * @param arguments arguments
	 * @param uiLanguages codes of the active UI languages
	 * @return parts, null if not supplied
	 * @throws IllegalArgumentException for an invalid part
	 */
	public static List<PartSpec> parse(Map<String, Object> arguments, Collection<String> uiLanguages) {
		Object value = arguments.get(ARG_PARTS);
		if (value == null) {
			return null;
		}
		if (!(value instanceof List<?> list) || list.size() > MAX_PARTS) {
			throw new IllegalArgumentException(
					"Argument '%s' must be a list of at most %d parts".formatted(ARG_PARTS, MAX_PARTS));
		}
		List<PartSpec> parts = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			try {
				parts.add(parsePart(list.get(i), uiLanguages));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("%s[%d]: %s".formatted(ARG_PARTS, i, e.getMessage()), e);
			}
		}
		return parts;
	}

	/**
	 * Check a construct or part keyword
	 * @param name argument name
	 * @param keyword keyword
	 * @param maxLength maximum length
	 * @throws IllegalArgumentException if the keyword does not match {@link #KEYWORD} or is too long
	 */
	public static void checkKeyword(String name, String keyword, int maxLength) {
		if (keyword.length() > maxLength || !KEYWORD.matcher(keyword).matches()) {
			throw new IllegalArgumentException("Argument '%s' must be at most %d characters of a-z, 0-9 and _, "
					.formatted(name, maxLength) + "starting with a letter, but is '%s'".formatted(keyword));
		}
	}

	/**
	 * Check the rules of the part list
	 * @param parts parts
	 * @param templateKeyword keyword of the Handlebars part
	 * @param datasourceExists test whether a datasource exists
	 * @throws IllegalArgumentException naming the first part that breaks a rule
	 */
	public static void validate(List<PartSpec> parts, String templateKeyword, IntPredicate datasourceExists) {
		Set<String> keywords = new HashSet<>();
		for (PartSpec part : parts) {
			String prefix = "Part '%s': ".formatted(part.keyword());
			if (part.typeId() == Part.HANDLEBARS) {
				throw new IllegalArgumentException(prefix + "type %d is the Handlebars template, pass it as "
						.formatted(Part.HANDLEBARS) + "handlebarsTemplate");
			}
			if (!ALLOWED_TYPES.contains(part.typeId())) {
				throw new IllegalArgumentException(prefix + "type %d is not allowed, see list_part_types"
						.formatted(part.typeId()));
			}
			if (part.keyword().equals(templateKeyword)) {
				throw new IllegalArgumentException(prefix + "the keyword is the one of the Handlebars part");
			}
			if (!keywords.add(part.keyword())) {
				throw new IllegalArgumentException(prefix + "the keyword is used more than once");
			}
			if (DATASOURCE_TYPES.contains(part.typeId())) {
				if (part.datasourceId() == null) {
					throw new IllegalArgumentException(prefix + "a select part needs a datasourceId, see "
							+ "list_datasources");
				}
				if (!datasourceExists.test(part.datasourceId())) {
					throw new IllegalArgumentException(prefix + "datasource %d does not exist"
							.formatted(part.datasourceId()));
				}
			} else if (part.datasourceId() != null) {
				throw new IllegalArgumentException(prefix + "only select parts take a datasourceId");
			}
			if (part.defaultValue() != null && defaultProperty(part) == null) {
				throw new IllegalArgumentException(prefix + "a default value needs a text type (string) or a "
						+ "checkbox (boolean)");
			}
		}
	}

	/**
	 * Build the REST part
	 * @param part part
	 * @param defaultOrder part order if the part has none
	 * @param existing existing REST part with the same keyword and type, which keeps its identity, may be null
	 * @return REST part
	 */
	public static com.gentics.contentnode.rest.model.Part toRest(PartSpec part, int defaultOrder,
			com.gentics.contentnode.rest.model.Part existing) {
		com.gentics.contentnode.rest.model.Part rest = existing != null ? existing
				: new com.gentics.contentnode.rest.model.Part();
		rest.setKeyword(part.keyword());
		rest.setNameI18n(part.name());
		rest.setTypeId(part.typeId());
		rest.setEditable(part.editable());
		rest.setLiveEditable(part.liveEditable());
		rest.setMandatory(part.mandatory());
		rest.setHidden(part.hidden());
		rest.setPartOrder(part.partOrder() != null ? part.partOrder() : defaultOrder);
		if (part.datasourceId() != null) {
			SelectSetting settings = rest.getSelectSettings() != null ? rest.getSelectSettings() : new SelectSetting();
			settings.setDatasourceId(part.datasourceId());
			rest.setSelectSettings(settings);
		}
		Property defaultProperty = defaultProperty(part);
		if (defaultProperty != null) {
			rest.setDefaultProperty(defaultProperty);
		}
		return rest;
	}

	/**
	 * Build the REST Handlebars part, with order 1, not editable
	 * @param keyword keyword
	 * @param source Handlebars source
	 * @param existing existing Handlebars part, which keeps its identity, may be null
	 * @return REST part
	 */
	public static com.gentics.contentnode.rest.model.Part templatePart(String keyword, String source,
			com.gentics.contentnode.rest.model.Part existing) {
		com.gentics.contentnode.rest.model.Part rest = existing != null ? existing
				: new com.gentics.contentnode.rest.model.Part();
		rest.setKeyword(keyword);
		rest.setTypeId(Part.HANDLEBARS);
		rest.setEditable(false);
		rest.setPartOrder(1);
		Property property = new Property();
		property.setType(Property.Type.RICHTEXT);
		property.setStringValue(source);
		rest.setDefaultProperty(property);
		return rest;
	}

	/**
	 * Build the input schema of the parts argument
	 * @param description description
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema(String description) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("keyword", schema("string", "Machine name used in the Handlebars template as "
				+ "cms.tag.parts.<keyword>.", "minLength", 1, "maxLength", MAX_KEYWORD_LENGTH, "pattern",
				KEYWORD.pattern()));
		properties.put("name", i18nInputSchema(255));
		properties.put("typeId", schema("integer", "Part type ID. Use one that list_part_types reports with "
				+ "allowedForGeneratedConstructs true; anything else, and type 43, is rejected.", "minimum", 1));
		properties.put("datasourceId", schema("integer", "Required for type 29 (single select) and 30 (multi "
				+ "select), rejected otherwise. See list_datasources.", "minimum", 1));
		properties.put("editable", schema("boolean", null, "default", true));
		properties.put("liveEditable", schema("boolean", null, "default", false));
		properties.put("mandatory", schema("boolean", null, "default", false));
		properties.put("hidden", schema("boolean", null, "default", false));
		properties.put("partOrder", schema("integer", "Defaults to the position in the list, after the "
				+ "Handlebars part.", "minimum", 1, "maximum", 100));
		properties.put("defaultValue", Map.of("type", List.of("string", "boolean", "integer", "null"),
				"description", "Initial value: a string for text types, a boolean for a checkbox."));
		Map<String, Object> part = schema("object", null, "properties", properties, "required",
				List.of("keyword", "name", "typeId"), "additionalProperties", false);
		return schema("array", description, "maxItems", MAX_PARTS, "items", part);
	}

	/**
	 * Build the input schema of a map from UI language code to text
	 * @param maxLength maximum length of a text
	 * @return schema
	 */
	public static Map<String, Object> i18nInputSchema(int maxLength) {
		return schema("object", "Language code -> text, e.g. {\"de\": \"Hallo Box\", \"en\": \"Hello box\"}. At "
				+ "least one UI language of the CMS.", "minProperties", 1, "maxProperties", Args.MAX_I18N_ENTRIES,
				"additionalProperties", schema("string", null, "maxLength", maxLength));
	}

	/**
	 * Parse a single part
	 * @param value raw part
	 * @param uiLanguages codes of the active UI languages
	 * @return part
	 */
	private static PartSpec parsePart(Object value, Collection<String> uiLanguages) {
		if (!(value instanceof Map<?, ?> map)) {
			throw new IllegalArgumentException("A part must be an object");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> raw = (Map<String, Object>) map;
		for (String member : raw.keySet()) {
			if (!MEMBERS.contains(member)) {
				throw new IllegalArgumentException("Unknown member '%s'".formatted(member));
			}
		}
		String keyword = McpArgs.stringArg(raw, "keyword", 1, MAX_KEYWORD_LENGTH);
		if (keyword == null) {
			throw new IllegalArgumentException("Missing required argument 'keyword'");
		}
		checkKeyword("keyword", keyword, MAX_KEYWORD_LENGTH);
		Map<String, String> name = Args.i18nMap(raw, "name", 255, uiLanguages);
		if (name == null) {
			throw new IllegalArgumentException("Missing required argument 'name'");
		}
		Object defaultValue = raw.get("defaultValue");
		if (defaultValue != null && !(defaultValue instanceof String) && !(defaultValue instanceof Boolean)) {
			throw new IllegalArgumentException("Argument 'defaultValue' must be a string or boolean");
		}
		return new PartSpec(keyword, name, Args.id(raw, "typeId"), McpArgs.intArg(raw, "datasourceId", 1,
				Integer.MAX_VALUE), McpArgs.booleanArg(raw, "editable", true), McpArgs.booleanArg(raw,
						"liveEditable", false), McpArgs.booleanArg(raw, "mandatory", false), McpArgs.booleanArg(raw,
								"hidden", false), McpArgs.intArg(raw, "partOrder", 1, 100), defaultValue);
	}

	/**
	 * Build the default property of a part
	 * @param part part
	 * @return property, null if the part has no default value or it does not fit the part type
	 */
	private static Property defaultProperty(PartSpec part) {
		Property.Type type = Property.Type.get(part.typeId());
		Property property = new Property();
		property.setType(type);
		if (part.defaultValue() instanceof String text && (type == Property.Type.STRING
				|| type == Property.Type.RICHTEXT)) {
			property.setStringValue(text);
			return property;
		}
		if (part.defaultValue() instanceof Boolean flag && type == Property.Type.BOOLEAN) {
			property.setBooleanValue(flag);
			return property;
		}
		return null;
	}

	/**
	 * Get the Handlebars part of a REST construct
	 * @param parts REST parts, may be null
	 * @return Handlebars part, null if there is none
	 */
	public static com.gentics.contentnode.rest.model.Part templateOf(
			List<com.gentics.contentnode.rest.model.Part> parts) {
		return parts == null ? null
				: parts.stream().filter(part -> part.getTypeId() == Part.HANDLEBARS).findFirst().orElse(null);
	}
}
