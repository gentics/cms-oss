package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ConstructInfo;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.Constructs;
import com.gentics.contentnode.mcp.util.PartSpecs;
import com.gentics.contentnode.mcp.util.PartSpecs.PartSpec;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.EditorControlStyle;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.lib.log.NodeLogger;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Creates a construct with a Handlebars part holding the template and the given editable parts, assigned to nodes.
 * Delegates to {@link ConstructResourceImpl#create}, which needs the construct admin update permission and
 * {@code updateconstructs} on each node. Before that, the tool checks what the CMS does not: the parts
 * ({@link PartSpecs#validate}), the category, and that the keyword is free and short enough, since the CMS would
 * rename it. If a concurrent call took the keyword anyway, the construct is deleted again.
 */
public class CreateConstructTool extends AbstractMcpTool {
	static final String NAME = "create_construct";

	static final String ARG_KEYWORD = "keyword";

	static final String ARG_NAME = "name";

	static final String ARG_DESCRIPTION = "description";

	static final String ARG_CATEGORY_ID = "categoryId";

	static final String ARG_NODE_IDS = "nodeIds";

	static final String ARG_HANDLEBARS_TEMPLATE = "handlebarsTemplate";

	static final String ARG_TEMPLATE_PART_KEYWORD = "templatePartKeyword";

	static final String ARG_MAY_BE_SUBTAG = "mayBeSubtag";

	static final String ARG_MAY_CONTAIN_SUBTAGS = "mayContainSubtags";

	static final String ARG_AUTO_ENABLE = "autoEnable";

	static final String ARG_VISIBLE_IN_MENU = "visibleInMenu";

	static final String ARG_EDITOR_CONTROL_STYLE = "editorControlStyle";

	static final String ARG_ALLOW_EXISTING = "allowExisting";

	/**
	 * Maximum length of a construct keyword in the CMS, which shortens longer ones
	 */
	static final int MAX_KEYWORD_LENGTH = com.gentics.contentnode.object.Construct.MAX_KEYWORD_LENGTH;

	/**
	 * Maximum length of a Handlebars template
	 */
	static final int MAX_TEMPLATE_LENGTH = 200000;

	/**
	 * Maximum number of nodes
	 */
	static final int MAX_NODES = 50;

	private static final NodeLogger logger = NodeLogger.getNodeLogger(CreateConstructTool.class);

	/**
	 * The Handlebars part, as reported by the tool
	 * @param keyword keyword
	 * @param typeId always {@link Part#HANDLEBARS}
	 * @param reportedType always {@link ConstructInfo#TEMPLATE_REPORTED_TYPE}
	 */
	public record TemplatePart(String keyword, int typeId, String reportedType) {
	}

	/**
	 * Result of the tool
	 * @param construct construct
	 * @param created whether the construct was created by this call
	 * @param nodeRefs nodes the construct is assigned to
	 * @param templatePart the Handlebars part
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ConstructInfo construct, boolean created, List<ObjectRef> nodeRefs,
			TemplatePart templatePart) {
	}

	/**
	 * Validated arguments
	 * @param construct REST construct to create
	 * @param nodeIds node IDs
	 * @param parts editable parts
	 * @param allowExisting whether an existing keyword returns that construct
	 */
	record Request(Construct construct, List<Integer> nodeIds, List<PartSpec> parts, boolean allowExisting) {
		/**
		 * Validate the arguments
		 * @param arguments arguments
		 * @param uiLanguages codes of the active UI languages
		 * @return request
		 * @throws IllegalArgumentException for missing or invalid arguments
		 */
		static Request of(Map<String, Object> arguments, Collection<String> uiLanguages) {
			String keyword = required(ARG_KEYWORD, stringArg(arguments, ARG_KEYWORD, 2,
					PartSpecs.MAX_KEYWORD_LENGTH));
			PartSpecs.checkKeyword(ARG_KEYWORD, keyword, MAX_KEYWORD_LENGTH);
			Map<String, String> name = required(ARG_NAME, Args.i18nMap(arguments, ARG_NAME, 255,
					uiLanguages));
			Map<String, String> description = Args.i18nMap(arguments, ARG_DESCRIPTION, 1000, uiLanguages);
			List<Integer> nodeIds = required(ARG_NODE_IDS, Args.distinctIntList(arguments, ARG_NODE_IDS,
					MAX_NODES));
			if (nodeIds.isEmpty()) {
				throw new IllegalArgumentException("Argument '%s' needs at least one node".formatted(ARG_NODE_IDS));
			}
			String source = required(ARG_HANDLEBARS_TEMPLATE, stringArg(arguments,
					ARG_HANDLEBARS_TEMPLATE, 1, MAX_TEMPLATE_LENGTH));
			String templateKeyword = stringArg(arguments, ARG_TEMPLATE_PART_KEYWORD, 1, PartSpecs.MAX_KEYWORD_LENGTH);
			templateKeyword = templateKeyword != null ? templateKeyword : "handlebars";
			PartSpecs.checkKeyword(ARG_TEMPLATE_PART_KEYWORD, templateKeyword, PartSpecs.MAX_KEYWORD_LENGTH);
			List<PartSpec> parts = PartSpecs.parse(arguments, uiLanguages);
			parts = parts != null ? parts : List.of();
			checkVisibleInMenu(arguments);

			Construct construct = new Construct();
			construct.setKeyword(keyword);
			construct.setNameI18n(name);
			construct.setDescriptionI18n(description);
			construct.setCategoryId(intArg(arguments, ARG_CATEGORY_ID, 1, Integer.MAX_VALUE));
			construct.setMayBeSubtag(booleanArg(arguments, ARG_MAY_BE_SUBTAG, true));
			construct.setMayContainSubtags(booleanArg(arguments, ARG_MAY_CONTAIN_SUBTAGS, false));
			construct.setAutoEnable(booleanArg(arguments, ARG_AUTO_ENABLE, true));
			construct.setEditorControlStyle(editorControlStyle(arguments, EditorControlStyle.ABOVE));
			List<com.gentics.contentnode.rest.model.Part> restParts = new ArrayList<>();
			restParts.add(PartSpecs.templatePart(templateKeyword, source, null));
			for (int i = 0; i < parts.size(); i++) {
				restParts.add(PartSpecs.toRest(parts.get(i), i + 2, null));
			}
			construct.setParts(restParts);
			return new Request(construct, nodeIds, parts, booleanArg(arguments, ARG_ALLOW_EXISTING, false));
		}

		/**
		 * Get the keyword of the Handlebars part
		 * @return keyword
		 */
		String templateKeyword() {
			return construct.getParts().get(0).getKeyword();
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_KEYWORD, schema("string", "Unique machine name, also the tag type name in the editor. At "
				+ "most 64 characters: the CMS would shorten a longer one.", "minLength", 2, "maxLength",
				PartSpecs.MAX_KEYWORD_LENGTH, "pattern", PartSpecs.KEYWORD.pattern()));
		properties.put(ARG_NAME, PartSpecs.i18nInputSchema(255));
		properties.put(ARG_DESCRIPTION, schema("object", "Language code -> text.", "maxProperties",
				Args.MAX_I18N_ENTRIES, "additionalProperties", schema("string", null, "maxLength", 1000)));
		properties.put(ARG_CATEGORY_ID, schema("integer", "Existing category, see ensure_construct_category.",
				"minimum", 1));
		properties.put(ARG_NODE_IDS, schema("array", "Nodes to assign the construct to.", "minItems", 1, "maxItems",
				MAX_NODES, "uniqueItems", true, "items", schema("integer", null, "minimum", 1)));
		properties.put(ARG_HANDLEBARS_TEMPLATE, schema("string", "Handlebars source. Access part values as "
				+ "cms.tag.parts.<partKeyword> (for example {{cms.tag.parts.headline}}); the shorter "
				+ "tag.parts.<keyword> and a bare {{keyword}} do not resolve in the CMS Handlebars part type.",
				"minLength", 1, "maxLength", MAX_TEMPLATE_LENGTH));
		properties.put(ARG_TEMPLATE_PART_KEYWORD, schema("string", "Keyword of the Handlebars part.", "maxLength",
				PartSpecs.MAX_KEYWORD_LENGTH, "default", "handlebars", "pattern", PartSpecs.KEYWORD.pattern()));
		properties.put(PartSpecs.ARG_PARTS, PartSpecs.jsonSchema("Editable parts. Do NOT include the Handlebars "
				+ "template part here."));
		properties.put(ARG_MAY_BE_SUBTAG, schema("boolean", null, "default", true));
		properties.put(ARG_MAY_CONTAIN_SUBTAGS, schema("boolean", null, "default", false));
		properties.put(ARG_AUTO_ENABLE, schema("boolean", null, "default", true));
		properties.put(ARG_VISIBLE_IN_MENU, schema("boolean", "Only true is accepted: the CMS derives it from the "
				+ "category permission.", "default", true));
		properties.put(ARG_EDITOR_CONTROL_STYLE, schema("string", null, "enum", List.of("ASIDE", "ABOVE", "CLICK"),
				"default", "ABOVE"));
		properties.put(ARG_ALLOW_EXISTING, schema("boolean", "When the keyword already exists: false fails, true "
				+ "returns the existing construct with created false.", "default", false));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Create construct")
				.description("Creates a new construct with a Handlebars template and editable parts, and assigns it "
						+ "to the given nodes. Pass the template as plain Handlebars source in handlebarsTemplate: the "
						+ "server creates the type-43 template part itself, so never describe that part in parts. "
						+ "Before calling: check the keyword is free with list_constructs, resolve the category with "
						+ "ensure_construct_category, discover which part types this installation allows with "
						+ "list_part_types, and validate the template syntax client-side, because the CMS accepts a "
						+ "broken template and only fails at render time. Afterwards, prove it renders by inserting "
						+ "it into a page and calling render_preview. Tier 1 constructs only: it cannot create "
						+ "structured fields that need a content-repository mapping.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_KEYWORD, ARG_NAME, ARG_NODE_IDS, ARG_HANDLEBARS_TEMPLATE))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Request request = Request.of(arguments, Args.uiLanguages());
		String keyword = request.construct().getKeyword();
		Writes.logIdempotencyKey(NAME, 0, arguments);

		Integer existingId;
		try (Trx trx = ContentNodeHelper.trx()) {
			PartSpecs.validate(request.parts(), request.templateKeyword(),
					PartSpecs.existingDatasources(request.parts())::contains);
			checkCategory(request.construct().getCategoryId());
			existingId = Constructs.idByKeyword(keyword);
			trx.success();
		}

		ConstructResource resource = RestPermissions.guard(ConstructResource.class, new ConstructResourceImpl());
		if (existingId != null) {
			if (!request.allowExisting()) {
				throw new IllegalArgumentException("Construct %d already has the keyword '%s'. Pass allowExisting=true "
						.formatted(existingId, keyword) + "to get it, or choose another keyword.");
			}
			ConstructLoadResponse existing = resource.get(Integer.toString(existingId),
					new EmbedParameterBean().withEmbed("category"));
			requireOk(existing, "Construct %d could not be loaded".formatted(existingId));
			return result(existing.getConstruct(), false, Constructs.nodeRefs(existingId));
		}

		ConstructLoadResponse response = resource.create(request.construct(), request.nodeIds());
		requireOk(response, "The construct '%s' was not created".formatted(keyword));
		Construct created = response.getConstruct();
		if (!keyword.equals(created.getKeyword())) {
			// another call took the keyword meanwhile, so the CMS renamed this one
			try {
				resource.delete(Integer.toString(created.getId()));
			} catch (Exception e) {
				logger.error("Error while deleting construct %d, which got the keyword '%s' instead of '%s'"
						.formatted(created.getId(), created.getKeyword(), keyword), e);
			}
			throw new IllegalArgumentException("The keyword '%s' was taken while the construct was created"
					.formatted(keyword));
		}
		ConstructLoadResponse reloaded = resource.get(Integer.toString(created.getId()),
				new EmbedParameterBean().withEmbed("category"));
		requireOk(reloaded, "Construct %d could not be loaded".formatted(created.getId()));
		return result(reloaded.getConstruct(), true, Constructs.nodeRefs(created.getId()));
	}

	/**
	 * Build the result
	 * @param construct REST construct
	 * @param created whether it was created
	 * @param nodeRefs nodes
	 * @return result
	 */
	static Result result(Construct construct, boolean created, List<ObjectRef> nodeRefs) {
		ConstructInfo info = ConstructInfo.of(construct);
		TemplatePart templatePart = info.template() != null ? new TemplatePart(info.template().partKeyword(),
				Part.HANDLEBARS, ConstructInfo.TEMPLATE_REPORTED_TYPE) : null;
		return new Result(info, created, nodeRefs, templatePart);
	}

	/**
	 * Check that a category exists. Must be called in a transaction.
	 * @param categoryId category ID, may be null
	 * @throws Exception
	 * @throws IllegalArgumentException if it does not exist
	 */
	static void checkCategory(Integer categoryId) throws Exception {
		if (categoryId != null && TransactionManager.getCurrentTransaction()
				.getObject(com.gentics.contentnode.object.ConstructCategory.class, categoryId) == null) {
			throw new IllegalArgumentException("Construct category %d does not exist, see ensure_construct_category"
					.formatted(categoryId));
		}
	}

	/**
	 * Reject visibleInMenu other than true, since the CMS cannot store it
	 * @param arguments arguments
	 * @throws IllegalArgumentException if it is false
	 */
	static void checkVisibleInMenu(Map<String, Object> arguments) {
		if (!booleanArg(arguments, ARG_VISIBLE_IN_MENU, true)) {
			throw new IllegalArgumentException("'%s' cannot be false: the CMS shows a construct in the menu if the "
					.formatted(ARG_VISIBLE_IN_MENU) + "user may view its category");
		}
	}

	/**
	 * Get the editor control style argument
	 * @param arguments arguments
	 * @param defaultValue value if not supplied
	 * @return style
	 * @throws IllegalArgumentException for an unknown style
	 */
	static EditorControlStyle editorControlStyle(Map<String, Object> arguments, EditorControlStyle defaultValue) {
		String value = stringArg(arguments, ARG_EDITOR_CONTROL_STYLE, 1, 10);
		if (value == null) {
			return defaultValue;
		}
		try {
			return EditorControlStyle.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Argument '%s' must be ASIDE, ABOVE or CLICK"
					.formatted(ARG_EDITOR_CONTROL_STYLE));
		}
	}

	/**
	 * Fail for a missing required argument
	 * @param <T> value type
	 * @param name argument name
	 * @param value parsed value
	 * @return value
	 * @throws IllegalArgumentException if the value is null
	 */
	static <T> T required(String name, T value) {
		if (value == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(name));
		}
		return value;
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> templateProperties = new LinkedHashMap<>();
		templateProperties.put("keyword", schema("string", null));
		templateProperties.put("typeId", schema("integer", null, "const", Part.HANDLEBARS));
		templateProperties.put("reportedType", schema("string", null, "const", ConstructInfo.TEMPLATE_REPORTED_TYPE));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("construct", ConstructInfo.jsonSchema(null));
		properties.put("created", schema("boolean", "Whether the construct was created by this call."));
		properties.put("nodeRefs", schema("array", "Nodes the construct is assigned to.", "items",
				objectRefSchema(null)));
		properties.put("templatePart", schema("object", "The Handlebars part.", "properties", templateProperties));
		return schema("object", null, "properties", properties, "required", List.of("construct", "created"));
	}
}
