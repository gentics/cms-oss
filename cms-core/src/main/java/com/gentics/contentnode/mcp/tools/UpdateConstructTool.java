package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.gentics.contentnode.mcp.util.ChangedFields;
import com.gentics.contentnode.mcp.util.Constructs;
import com.gentics.contentnode.mcp.util.PartSpecs;
import com.gentics.contentnode.mcp.util.PartSpecs.PartSpec;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.Part;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Changes a construct. The CMS replaces the whole part list on update, so the tool loads the construct and merges the
 * given changes over it: parts that keep their keyword and type keep their identity (global ID), so tag values
 * survive. Delegates to {@link ConstructResourceImpl#update}, which needs the edit permission on the construct (the
 * construct admin update permission and {@code updateconstructs} on every node it is assigned to) and view on the
 * nodes; the tool checks {@code updateconstructs} on nodes it is added to, which the delegate does not.
 */
public class UpdateConstructTool extends AbstractMcpTool {
	static final String NAME = "update_construct";

	static final String ARG_ID = "id";

	static final String ARG_KEYWORD = "keyword";

	static final String ARG_NAME = "name";

	static final String ARG_DESCRIPTION = "description";

	static final String ARG_CATEGORY_ID = "categoryId";

	static final String ARG_HANDLEBARS_TEMPLATE = "handlebarsTemplate";

	static final String ARG_NODE_IDS = "nodeIds";

	static final String ARG_MAY_BE_SUBTAG = "mayBeSubtag";

	static final String ARG_MAY_CONTAIN_SUBTAGS = "mayContainSubtags";

	static final String ARG_AUTO_ENABLE = "autoEnable";

	static final String ARG_EDITOR_CONTROL_STYLE = "editorControlStyle";

	/**
	 * Keyword of a Handlebars part added to a construct that has none
	 */
	static final String DEFAULT_TEMPLATE_KEYWORD = "handlebars";

	/**
	 * Fields reported as changed
	 */
	static final ChangedFields<ConstructInfo> CHANGED_FIELDS = ChangedFields.<ConstructInfo>builder()
			.field(ARG_NAME, ConstructInfo::nameI18n).field(ARG_DESCRIPTION, ConstructInfo::descriptionI18n)
			.field(ARG_CATEGORY_ID, ConstructInfo::categoryId)
			.field(ARG_HANDLEBARS_TEMPLATE, info -> info.template() != null ? info.template().source() : null)
			.field(PartSpecs.ARG_PARTS, ConstructInfo::parts).field(ARG_MAY_BE_SUBTAG, ConstructInfo::mayBeSubtag)
			.field(ARG_MAY_CONTAIN_SUBTAGS, ConstructInfo::mayContainSubtags)
			.field(ARG_AUTO_ENABLE, ConstructInfo::autoEnable)
			.field(ARG_EDITOR_CONTROL_STYLE, ConstructInfo::editorControlStyle).build();

	/**
	 * Result of the tool
	 * @param construct construct after the update
	 * @param changedFields fields that changed
	 * @param replacedParts whether the part list was given
	 * @param removedPartKeywords keywords of the parts that were removed
	 * @param nodeRefs nodes the construct is assigned to
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ConstructInfo construct, List<String> changedFields, boolean replacedParts,
			List<String> removedPartKeywords, List<ObjectRef> nodeRefs) {
	}

	/**
	 * Validated arguments
	 * @param changes REST construct with the changed fields only (no parts)
	 * @param source new Handlebars source, null to keep it
	 * @param parts new editable parts, null to keep them
	 * @param nodeIds new node assignment, null to keep it
	 */
	record Request(Construct changes, String source, List<PartSpec> parts, List<Integer> nodeIds) {
		/**
		 * Validate the arguments, except the construct ID
		 * @param arguments arguments
		 * @param uiLanguages codes of the active UI languages
		 * @return request
		 * @throws IllegalArgumentException for invalid arguments
		 */
		static Request of(Map<String, Object> arguments, Collection<String> uiLanguages) {
			List<Integer> nodeIds = Args.distinctIntList(arguments, ARG_NODE_IDS, CreateConstructTool.MAX_NODES);
			if (nodeIds != null && nodeIds.isEmpty()) {
				throw new IllegalArgumentException("'%s' must not be empty, the CMS ignores an empty list. Use "
						.formatted(ARG_NODE_IDS) + "assign_construct_to_nodes with mode remove or set to unassign.");
			}
			CreateConstructTool.checkVisibleInMenu(arguments);

			Construct changes = new Construct();
			changes.setNameI18n(Args.i18nMap(arguments, ARG_NAME, 255, uiLanguages));
			changes.setDescriptionI18n(Args.i18nMap(arguments, ARG_DESCRIPTION, 1000, uiLanguages));
			changes.setCategoryId(intArg(arguments, ARG_CATEGORY_ID, 1, Integer.MAX_VALUE));
			changes.setMayBeSubtag(optionalBoolean(arguments, ARG_MAY_BE_SUBTAG));
			changes.setMayContainSubtags(optionalBoolean(arguments, ARG_MAY_CONTAIN_SUBTAGS));
			changes.setAutoEnable(optionalBoolean(arguments, ARG_AUTO_ENABLE));
			changes.setEditorControlStyle(CreateConstructTool.editorControlStyle(arguments, null));
			return new Request(changes, stringArg(arguments, ARG_HANDLEBARS_TEMPLATE, 0,
					CreateConstructTool.MAX_TEMPLATE_LENGTH), PartSpecs.parse(arguments, uiLanguages), nodeIds);
		}
	}

	/**
	 * Merged part list
	 * @param parts REST parts to save, null if the parts stay as they are
	 * @param removedKeywords keywords of the editable parts that are removed
	 */
	record Merge(List<Part> parts, List<String> removedKeywords) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("integer", "ID of the construct.", "minimum", 1));
		properties.put(ARG_KEYWORD, schema("string", "Keyword of the construct. It cannot be changed.", "maxLength",
				PartSpecs.MAX_KEYWORD_LENGTH));
		properties.put(ARG_NAME, PartSpecs.i18nInputSchema(255));
		properties.put(ARG_DESCRIPTION, schema("object", "Language code -> text.", "maxProperties",
				Args.MAX_I18N_ENTRIES, "additionalProperties", schema("string", null, "maxLength", 1000)));
		properties.put(ARG_CATEGORY_ID, schema("integer", "Existing category, see ensure_construct_category.",
				"minimum", 1));
		properties.put(ARG_HANDLEBARS_TEMPLATE, schema("string", "New Handlebars source. Access part values as "
				+ "cms.tag.parts.<partKeyword>.", "maxLength", CreateConstructTool.MAX_TEMPLATE_LENGTH));
		properties.put(PartSpecs.ARG_PARTS, PartSpecs.jsonSchema("COMPLETE replacement list of editable parts. "
				+ "Omit to keep the current parts."));
		properties.put(ARG_NODE_IDS, schema("array", "Exact node assignment, adding and removing as needed. Omit to "
				+ "leave it untouched; an empty list is rejected.", "maxItems", CreateConstructTool.MAX_NODES,
				"uniqueItems", true, "items", schema("integer", null, "minimum", 1)));
		properties.put(ARG_MAY_BE_SUBTAG, schema("boolean", null));
		properties.put(ARG_MAY_CONTAIN_SUBTAGS, schema("boolean", null));
		properties.put(ARG_AUTO_ENABLE, schema("boolean", null));
		properties.put(CreateConstructTool.ARG_VISIBLE_IN_MENU, schema("boolean", "Only true is accepted: the CMS "
				+ "derives it from the category permission."));
		properties.put(ARG_EDITOR_CONTROL_STYLE, schema("string", null, "enum", List.of("ASIDE", "ABOVE", "CLICK")));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Update construct")
				.description("Changes an existing construct. The CMS replaces the whole part list on update, so this "
						+ "tool reads the construct first and merges your changes over it: supply only what you want "
						+ "changed. If you do pass parts, it must be the COMPLETE desired list, because anything "
						+ "omitted is deleted; a part that keeps its keyword and type keeps its identity and the "
						+ "values pages have for it. Passing nodeIds sets the exact node assignment; omit it to leave "
						+ "assignments alone. Existing pages keep content for parts you delete but can no longer edit "
						+ "it, so prefer adding parts over renaming or removing them. Pass exactly one of id or "
						+ "keyword. Read the construct with get_construct first.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Request request = Request.of(arguments, Args.uiLanguages());
		int id = GetConstructTool.constructId(arguments, ARG_ID, ARG_KEYWORD);
		Writes.logIdempotencyKey(NAME, id, arguments);

		ConstructResource resource = RestPermissions.guard(ConstructResource.class, new ConstructResourceImpl());
		ConstructLoadResponse loaded = resource.get(Integer.toString(id), new EmbedParameterBean());
		requireOk(loaded, "Construct %d could not be loaded".formatted(id));
		Construct before = loaded.getConstruct();
		ChangedFields.Snapshot<ConstructInfo> snapshot = CHANGED_FIELDS.snapshot(ConstructInfo.of(before));

		Set<Integer> nodesBefore;
		try (Trx trx = ContentNodeHelper.trx()) {
			Part template = PartSpecs.templateOf(before.getParts());
			if (request.parts() != null) {
				PartSpecs.validate(request.parts(), template != null ? template.getKeyword()
						: DEFAULT_TEMPLATE_KEYWORD, PartSpecs.existingDatasources(request.parts())::contains);
			}
			CreateConstructTool.checkCategory(request.changes().getCategoryId());
			nodesBefore = nodeIds(id);
			trx.success();
		}
		if (request.nodeIds() != null) {
			List<Integer> added = request.nodeIds().stream().filter(nodeId -> !nodesBefore.contains(nodeId)).toList();
			Constructs.checkUpdateConstructs(added);
		}

		Merge merge = merge(before.getParts(), request.source(), request.parts());
		Construct changes = request.changes();
		changes.setParts(merge.parts());
		requireOk(resource.update(Integer.toString(id), changes, request.nodeIds()),
				"Construct %d was not updated".formatted(id));

		ConstructLoadResponse reloaded = resource.get(Integer.toString(id),
				new EmbedParameterBean().withEmbed("category"));
		requireOk(reloaded, "Construct %d could not be loaded".formatted(id));
		ConstructInfo after = ConstructInfo.of(reloaded.getConstruct());
		List<String> changedFields = new ArrayList<>(snapshot.diff(after));
		Set<Integer> nodesAfter;
		try (Trx trx = ContentNodeHelper.trx()) {
			nodesAfter = nodeIds(id);
			trx.success();
		}
		if (!nodesAfter.equals(nodesBefore)) {
			changedFields.add(ARG_NODE_IDS);
		}
		return new Result(after, changedFields, request.parts() != null, merge.removedKeywords(),
				Constructs.nodeRefs(id));
	}

	/**
	 * Merge the changes into the current parts. The Handlebars part keeps its identity; an editable part keeps its
	 * identity if a given part has the same keyword and type.
	 * @param current current REST parts
	 * @param source new Handlebars source, null to keep it
	 * @param parts new editable parts, null to keep them
	 * @return merged parts
	 */
	static Merge merge(List<Part> current, String source, List<PartSpec> parts) {
		if (source == null && parts == null) {
			return new Merge(null, List.of());
		}
		List<Part> existing = current != null ? current : List.of();
		Part template = PartSpecs.templateOf(existing);
		List<Part> merged = new ArrayList<>();
		if (template != null) {
			merged.add(source != null ? PartSpecs.templatePart(template.getKeyword(), source, template) : template);
		} else if (source != null) {
			merged.add(PartSpecs.templatePart(DEFAULT_TEMPLATE_KEYWORD, source, null));
		}

		List<Part> editable = existing.stream().filter(part -> part != template).toList();
		if (parts == null) {
			merged.addAll(editable);
			return new Merge(merged, List.of());
		}
		Set<String> kept = new HashSet<>();
		for (int i = 0; i < parts.size(); i++) {
			PartSpec spec = parts.get(i);
			Part same = editable.stream().filter(part -> spec.keyword().equals(part.getKeyword())
					&& spec.typeId() == part.getTypeId()).findFirst().orElse(null);
			merged.add(PartSpecs.toRest(spec, i + 2, same));
			kept.add(spec.keyword());
		}
		List<String> removed = editable.stream().map(Part::getKeyword).filter(keyword -> !kept.contains(keyword))
				.toList();
		return new Merge(merged, removed);
	}

	/**
	 * Get the IDs of all nodes a construct is assigned to. Must be called in a transaction.
	 * @param constructId construct ID
	 * @return node IDs
	 * @throws Exception
	 */
	private static Set<Integer> nodeIds(int constructId) throws Exception {
		com.gentics.contentnode.object.Construct construct = TransactionManager.getCurrentTransaction()
				.getObject(com.gentics.contentnode.object.Construct.class, constructId);
		if (construct == null) {
			throw new EntityNotFoundException("Construct %d does not exist".formatted(constructId));
		}
		Set<Integer> ids = new HashSet<>();
		for (Node node : construct.getNodes()) {
			ids.add(node.getId());
		}
		return ids;
	}

	/**
	 * Get an optional boolean argument
	 * @param arguments arguments
	 * @param name argument name
	 * @return value, null if not supplied
	 */
	private static Boolean optionalBoolean(Map<String, Object> arguments, String name) {
		return arguments.containsKey(name) ? booleanArg(arguments, name, false) : null;
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("construct", ConstructInfo.jsonSchema("The construct after the update."));
		properties.put("changedFields", schema("array", "Arguments whose value actually changed.", "items",
				schema("string", null)));
		properties.put("replacedParts", schema("boolean", "Whether the part list was replaced."));
		properties.put("removedPartKeywords", schema("array", "Keywords of the removed parts.", "items",
				schema("string", null)));
		properties.put("nodeRefs", schema("array", "Nodes the construct is assigned to.", "items",
				objectRefSchema(null)));
		return schema("object", null, "properties", properties, "required", List.of("construct", "changedFields"));
	}
}
