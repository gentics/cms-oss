package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.ConstructCategory;
import com.gentics.contentnode.rest.model.EditorControlStyle;
import com.gentics.contentnode.rest.model.Property;

/**
 * A construct, as returned by MCP tools. The Handlebars part (type {@link Part#HANDLEBARS}) is returned as
 * {@link #template()}, not among the {@link #parts()}, since REST reports its type as {@code RICHTEXT}. Fields that are
 * not set are omitted on serialization.
 * @param ref reference to the construct
 * @param keyword keyword
 * @param nameI18n name per UI language
 * @param descriptionI18n description per UI language
 * @param categoryId ID of the category
 * @param category category, if embedded
 * @param mayBeSubtag whether the construct may be used as subtag
 * @param mayContainSubtags whether the construct may contain subtags
 * @param autoEnable whether new tags are enabled
 * @param visibleInMenu whether the construct is shown in the insert menu
 * @param editorControlStyle style of the editor controls
 * @param template Handlebars part
 * @param parts other parts, in part order
 * @param created creation timestamp
 * @param edited last edit timestamp
 */
@JsonInclude(Include.NON_NULL)
public record ConstructInfo(ObjectRef ref, String keyword, Map<String, String> nameI18n,
		Map<String, String> descriptionI18n, Integer categoryId, Category category, Boolean mayBeSubtag,
		Boolean mayContainSubtags, Boolean autoEnable, Boolean visibleInMenu, EditorControlStyle editorControlStyle,
		Template template, List<PartInfo> parts, Integer created, Integer edited) {

	/**
	 * Reported REST type of the Handlebars part
	 */
	public static final String TEMPLATE_REPORTED_TYPE = "RICHTEXT";

	/**
	 * A construct category. Fields that are not set are omitted on serialization.
	 * @param id ID
	 * @param globalId global ID
	 * @param nameI18n name per UI language
	 * @param sortOrder sort order
	 * @param constructCount number of constructs in the category, only in lists
	 */
	@JsonInclude(Include.NON_NULL)
	public record Category(Integer id, String globalId, Map<String, String> nameI18n, Integer sortOrder,
			Integer constructCount) {
		/**
		 * Map the REST category
		 * @param category REST category, may be null
		 * @param constructCount number of constructs, may be null
		 * @return category, null for null
		 */
		public static Category of(ConstructCategory category, Integer constructCount) {
			return category == null ? null
					: new Category(category.getId(), category.getGlobalId(), category.getNameI18n(),
							category.getSortOrder(), constructCount);
		}

		/**
		 * Build the output schema. Must be kept in sync with the components.
		 * @return schema
		 */
		public static Map<String, Object> jsonSchema() {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("id", schema("integer", null));
			properties.put("globalId", schema("string", null));
			properties.put("nameI18n", i18nSchema());
			properties.put("sortOrder", schema("integer", null));
			properties.put("constructCount", schema("integer", "Number of constructs in the category."));
			return schema("object", null, "properties", properties, "required", List.of("id"));
		}
	}

	/**
	 * The Handlebars part of a construct
	 * @param partKeyword keyword of the part
	 * @param source Handlebars source
	 * @param typeId always {@link Part#HANDLEBARS}
	 * @param reportedType always {@link ConstructInfo#TEMPLATE_REPORTED_TYPE}
	 */
	public record Template(String partKeyword, String source, int typeId, String reportedType) {
		/**
		 * Build the output schema. Must be kept in sync with the components.
		 * @return schema
		 */
		public static Map<String, Object> jsonSchema() {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("partKeyword", schema("string", null));
			properties.put("source", schema("string", "Handlebars source."));
			properties.put("typeId", schema("integer", null, "const", Part.HANDLEBARS));
			properties.put("reportedType", schema("string", null, "const", TEMPLATE_REPORTED_TYPE));
			return schema("object", "The Handlebars part. REST reports its type as RICHTEXT.", "properties",
					properties);
		}
	}

	/**
	 * A part of a construct, other than the Handlebars part. Fields that are not set are omitted on serialization.
	 * @param keyword keyword
	 * @param nameI18n name per UI language
	 * @param typeId part type ID
	 * @param type REST property type
	 * @param editable whether the part is editable
	 * @param liveEditable whether the part is live editable
	 * @param mandatory whether the part is mandatory
	 * @param hidden whether the part is hidden
	 * @param partOrder part order
	 * @param defaultValue default value of a text or checkbox part
	 * @param datasourceId datasource of a select part
	 * @param globalId global ID
	 */
	@JsonInclude(Include.NON_NULL)
	public record PartInfo(String keyword, Map<String, String> nameI18n, int typeId, Property.Type type,
			boolean editable, boolean liveEditable, boolean mandatory, boolean hidden, Integer partOrder,
			Object defaultValue, Integer datasourceId, String globalId) {
		/**
		 * Map the REST part
		 * @param part REST part
		 * @return part info
		 */
		public static PartInfo of(com.gentics.contentnode.rest.model.Part part) {
			Integer datasourceId = part.getSelectSettings() != null && part.getSelectSettings().getDatasourceId() > 0
					? part.getSelectSettings().getDatasourceId()
					: null;
			return new PartInfo(part.getKeyword(), part.getNameI18n(), part.getTypeId(), part.getType(),
					part.isEditable(), part.isLiveEditable(), part.isMandatory(), part.isHidden(), part.getPartOrder(),
					defaultValue(part.getDefaultProperty()), datasourceId, part.getGlobalId());
		}

		/**
		 * Get the default value of a text or checkbox part
		 * @param property default property, may be null
		 * @return value, null for other parts
		 */
		static Object defaultValue(Property property) {
			if (property == null || property.getType() == null) {
				return null;
			}
			return switch (property.getType()) {
			case STRING, RICHTEXT -> property.getStringValue();
			case BOOLEAN -> property.getBooleanValue();
			default -> null;
			};
		}

		/**
		 * Build the output schema. Must be kept in sync with the components.
		 * @return schema
		 */
		public static Map<String, Object> jsonSchema() {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("keyword", schema("string", null));
			properties.put("nameI18n", i18nSchema());
			properties.put("typeId", schema("integer", "Part type ID, see list_part_types."));
			properties.put("type", schema("string", "REST property type."));
			properties.put("editable", schema("boolean", null));
			properties.put("liveEditable", schema("boolean", null));
			properties.put("mandatory", schema("boolean", null));
			properties.put("hidden", schema("boolean", null));
			properties.put("partOrder", schema("integer", null));
			properties.put("defaultValue", Map.of("description", "Default value of a text or checkbox part."));
			properties.put("datasourceId", schema("integer", "Datasource of a select part."));
			properties.put("globalId", schema("string", null));
			return schema("object", null, "properties", properties);
		}
	}

	/**
	 * A construct in a list. Fields that are not set are omitted on serialization.
	 * @param ref reference to the construct
	 * @param keyword keyword
	 * @param nameI18n name per UI language
	 * @param descriptionI18n description per UI language
	 * @param category category, if embedded
	 * @param partKeywords keywords of the parts other than the Handlebars part
	 * @param hasHandlebarsPart whether the construct has a Handlebars part
	 * @param mayBeSubtag whether the construct may be used as subtag
	 * @param mayContainSubtags whether the construct may contain subtags
	 */
	@JsonInclude(Include.NON_NULL)
	public record Summary(ObjectRef ref, String keyword, Map<String, String> nameI18n,
			Map<String, String> descriptionI18n, Category category, List<String> partKeywords,
			boolean hasHandlebarsPart, Boolean mayBeSubtag, Boolean mayContainSubtags) {
		/**
		 * Map the REST construct
		 * @param construct REST construct
		 * @return summary
		 */
		public static Summary of(Construct construct) {
			ConstructInfo info = ConstructInfo.of(construct);
			return new Summary(info.ref(), info.keyword(), info.nameI18n(), info.descriptionI18n(), info.category(),
					info.parts().stream().map(PartInfo::keyword).toList(), info.template() != null,
					info.mayBeSubtag(), info.mayContainSubtags());
		}

		/**
		 * Build the output schema. Must be kept in sync with the components.
		 * @return schema
		 */
		public static Map<String, Object> jsonSchema() {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("ref", ObjectRef.jsonSchema(null));
			properties.put("keyword", schema("string", null));
			properties.put("nameI18n", i18nSchema());
			properties.put("descriptionI18n", i18nSchema());
			properties.put("category", Category.jsonSchema());
			properties.put("partKeywords", schema("array", "Keywords of the parts, without the Handlebars part.",
					"items", schema("string", null)));
			properties.put("hasHandlebarsPart", schema("boolean", null));
			properties.put("mayBeSubtag", schema("boolean", null));
			properties.put("mayContainSubtags", schema("boolean", null));
			return schema("object", null, "properties", properties, "required", List.of("ref", "keyword"));
		}
	}

	/**
	 * Map the REST construct. The first Handlebars part becomes the template.
	 * @param construct REST construct
	 * @return construct info
	 */
	public static ConstructInfo of(Construct construct) {
		Template template = null;
		List<PartInfo> parts = new ArrayList<>();
		if (construct.getParts() != null) {
			for (com.gentics.contentnode.rest.model.Part part : construct.getParts()) {
				if (template == null && part.getTypeId() == Part.HANDLEBARS) {
					String source = part.getDefaultProperty() != null ? part.getDefaultProperty().getStringValue()
							: null;
					template = new Template(part.getKeyword(), source, Part.HANDLEBARS, TEMPLATE_REPORTED_TYPE);
				} else {
					parts.add(PartInfo.of(part));
				}
			}
		}
		return new ConstructInfo(ObjectRef.forConstruct(construct), construct.getKeyword(), construct.getNameI18n(),
				construct.getDescriptionI18n(), construct.getCategoryId(), Category.of(construct.getCategory(), null),
				construct.getMayBeSubtag(), construct.getMayContainSubtags(), construct.getAutoEnable(),
				construct.getVisibleInMenu(), construct.getEditorControlStyle(), template, parts,
				Timestamps.orNull(construct.getCdate()), Timestamps.orNull(construct.getEdate()));
	}

	/**
	 * Build the output schema. Must be kept in sync with the components.
	 * @param description description, may be null
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema(String description) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema(null));
		properties.put("keyword", schema("string", null));
		properties.put("nameI18n", i18nSchema());
		properties.put("descriptionI18n", i18nSchema());
		properties.put("categoryId", schema("integer", null));
		properties.put("category", Category.jsonSchema());
		properties.put("mayBeSubtag", schema("boolean", null));
		properties.put("mayContainSubtags", schema("boolean", null));
		properties.put("autoEnable", schema("boolean", null));
		properties.put("visibleInMenu", schema("boolean", null));
		properties.put("editorControlStyle", schema("string", null, "enum", List.of("ASIDE", "ABOVE", "CLICK")));
		properties.put("template", Template.jsonSchema());
		properties.put("parts", schema("array", "The parts other than the Handlebars part.", "items",
				PartInfo.jsonSchema()));
		properties.put("created", schema("integer", "Unix timestamp in seconds."));
		properties.put("edited", schema("integer", "Unix timestamp in seconds."));
		return schema("object", description, "properties", properties, "required", List.of("ref", "keyword"));
	}

	/**
	 * Build the schema of a map from UI language code to text
	 * @return schema
	 */
	public static Map<String, Object> i18nSchema() {
		return schema("object", "UI language code -> text.", "additionalProperties", schema("string", null));
	}
}
