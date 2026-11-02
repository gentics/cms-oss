package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.TagInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.TemplateTag;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Loads a template with its template tags and the folders it is linked to. Delegates to {@link TemplateResourceImpl#get}
 * and {@link TemplateResourceImpl#folders}, which check {@code ObjectPermission.view} (the folders also filtered by
 * view).
 */
public class GetTemplateTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_INCLUDE_SOURCE = "includeSource";

	/**
	 * A tag of the template
	 * @param name tag name
	 * @param constructId construct ID
	 * @param constructKeyword construct keyword
	 * @param editableInPage whether pages may fill the tag
	 * @param mandatory whether pages must fill the tag
	 */
	@JsonInclude(Include.NON_NULL)
	public record TemplateTagInfo(String name, Integer constructId, String constructKeyword, Boolean editableInPage,
			Boolean mandatory) {
	}

	/**
	 * The template. Fields that are not set are omitted on serialization.
	 * @param ref reference to the template
	 * @param description description
	 * @param markupLanguage name of the markup language
	 * @param locked whether the template is locked
	 * @param source template source, if requested
	 * @param templateTags tags of the template, sorted by name
	 * @param folderRefs folders the template is linked to
	 */
	@JsonInclude(Include.NON_NULL)
	public record TemplateInfo(ObjectRef ref, String description, String markupLanguage, boolean locked, String source,
			List<TemplateTagInfo> templateTags, List<ObjectRef> folderRefs) {
	}

	/**
	 * Result of the tool
	 * @param template the template
	 */
	public record Result(TemplateInfo template) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("integer", "ID of the template.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to load the template in.", "minimum", 1));
		properties.put(ARG_INCLUDE_SOURCE, schema("boolean",
				"Include the raw template markup. Large; only request it when you must inspect the markup.", "default",
				false));

		return Tool.builder().name("get_template").title("Get template")
				.description("Loads a template and the tags it defines, including which of those tags a page may fill "
						+ "(editableInPage) and which are mandatory. Call this before building page content: it tells "
						+ "you the slots that exist. It does NOT tell you which constructs are available in the node, "
						+ "use list_constructs with nodeId for that.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Args.id(arguments, ARG_ID);
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
		boolean includeSource = booleanArg(arguments, ARG_INCLUDE_SOURCE, false);

		TemplateResource templateResource = RestPermissions.guard(TemplateResource.class, new TemplateResourceImpl());
		TemplateLoadResponse response = templateResource.get(Integer.toString(id), nodeId, false, true);
		requireOk(response, "The template %d could not be loaded".formatted(id));
		Template template = response.getTemplate();
		List<Folder> folders = ListResponses.items(templateResource.folders(Integer.toString(id),
				new SortParameterBean(), new FilterParameterBean(), new PagingParameterBean()));

		Map<Integer, String> keywords = template.getTemplateTags() != null
				? TagInfo.constructKeywords(template.getTemplateTags().values())
				: Map.of();
		return new Result(info(template, nodeId, includeSource, folders, keywords));
	}

	/**
	 * Build the template info
	 * @param template REST template
	 * @param nodeId node ID the template was loaded for, may be null
	 * @param includeSource whether to include the source
	 * @param folders folders the template is linked to
	 * @param keywords construct keywords by construct ID, for tags without a loaded construct
	 * @return template info
	 */
	static TemplateInfo info(Template template, Integer nodeId, boolean includeSource, List<Folder> folders,
			Map<Integer, String> keywords) {
		List<TemplateTagInfo> tags = new ArrayList<>();
		if (template.getTemplateTags() != null) {
			for (TemplateTag tag : template.getTemplateTags().values()) {
				String keyword = tag.getConstruct() != null && tag.getConstruct().getKeyword() != null
						? tag.getConstruct().getKeyword()
						: keywords.get(tag.getConstructId());
				tags.add(new TemplateTagInfo(tag.getName(), tag.getConstructId(), keyword, tag.getEditableInPage(),
						tag.getMandatory()));
			}
		}
		tags.sort(Comparator.comparing(TemplateTagInfo::name, String.CASE_INSENSITIVE_ORDER));
		List<ObjectRef> folderRefs = folders.stream().map(ObjectRef::forFolder).toList();

		return new TemplateInfo(ObjectRef.forTemplate(template, nodeId), template.getDescription(),
				template.getMarkupLanguage() != null ? template.getMarkupLanguage().getName() : null,
				template.isLocked(), includeSource ? template.getSource() : null, tags, folderRefs);
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> tagProperties = new LinkedHashMap<>();
		tagProperties.put("name", schema("string", null));
		tagProperties.put("constructId", schema("integer", null));
		tagProperties.put("constructKeyword", schema("string", null));
		tagProperties.put("editableInPage", schema("boolean", "Whether pages may fill the tag."));
		tagProperties.put("mandatory", schema("boolean", "Whether pages must fill the tag."));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema(null));
		properties.put("description", schema("string", null));
		properties.put("markupLanguage", schema("string", null));
		properties.put("locked", schema("boolean", null));
		properties.put("source", schema("string", "Template markup, with includeSource."));
		properties.put("templateTags", schema("array", "Tags of the template, sorted by name.", "items",
				schema("object", null, "properties", tagProperties)));
		properties.put("folderRefs", schema("array", "Folders the template is linked to.", "items",
				ObjectRef.jsonSchema(null)));

		return schema("object", null, "properties", Map.of("template", schema("object", null, "properties",
				properties, "required", List.of("ref"))), "required", List.of("template"));
	}
}
