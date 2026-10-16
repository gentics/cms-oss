package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;
import com.gentics.contentnode.rest.model.response.TotalUsageInfo;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.ImageResource;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Reverse lookup of the objects referencing a page, file or image, using the CMS usage endpoints of
 * {@link PageResourceImpl}, {@link FileResourceImpl} and {@link ImageResourceImpl}. Those do not check the permission
 * on the referenced object, so the tool first loads it ({@code ObjectPermission.view}); the referencing objects are
 * filtered by view in the delegates. A relation that does not apply to the object type is answered with an empty
 * group.
 */
public class GetRelatedTool extends AbstractMcpTool {
	static final String ARG_REF = "ref";

	static final String ARG_RELATIONS = "relations";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_SIZE = "size";

	static final String ARG_FROM = "from";

	/**
	 * Relations, as in the contract
	 */
	static final List<String> RELATIONS = List.of("links_to_page", "links_to_file", "links_to_image",
			"uses_template", "uses_tag", "variants", "total");

	/**
	 * Default page size per relation
	 */
	static final int DEFAULT_SIZE = 25;

	/**
	 * Maximum page size per relation
	 */
	static final int MAX_SIZE = 200;

	/**
	 * Maximum offset
	 */
	static final int MAX_FROM = 10000;

	/**
	 * Usage lookups of the CMS
	 */
	enum Usage {
		/**
		 * Pages linking to the page
		 */
		PAGES_LINKING_PAGE,
		/**
		 * Pages with a tag linking to a tag of the page
		 */
		PAGES_LINKING_PAGE_TAG,
		/**
		 * Variants of the page
		 */
		PAGE_VARIANTS,
		/**
		 * Templates linking to the page
		 */
		TEMPLATES_LINKING_PAGE,
		/**
		 * Pages linking to the file
		 */
		PAGES_LINKING_FILE,
		/**
		 * Templates linking to the file
		 */
		TEMPLATES_LINKING_FILE,
		/**
		 * Pages linking to the image
		 */
		PAGES_LINKING_IMAGE,
		/**
		 * Templates linking to the image
		 */
		TEMPLATES_LINKING_IMAGE
	}

	/**
	 * Objects with one relation to the referenced object
	 * @param relation relation
	 * @param total number of referencing objects, including those the caller cannot view
	 * @param items referencing objects in the requested page
	 */
	public record RelatedGroup(String relation, int total, List<ObjectRef> items) {
	}

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param ref the referenced object
	 * @param groups one group per requested relation except total
	 * @param total total usage count, if requested
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ObjectRef ref, List<RelatedGroup> groups, Integer total) {
	}

	/**
	 * Check that related objects can be looked up for the type
	 * @param type type of the referenced object
	 * @return type
	 * @throws IllegalArgumentException for a type other than page, file or image
	 */
	static Type supportedType(Type type) {
		if (type != Type.PAGE && type != Type.FILE && type != Type.IMAGE) {
			throw new IllegalArgumentException(
					"Related objects can only be looked up for a page, file or image, not for a %s".formatted(
							type.value()));
		}
		return type;
	}

	/**
	 * Get the usage lookup for a relation of an object type
	 * @param type type of the referenced object, see {@link #supportedType(Type)}
	 * @param relation relation, other than total
	 * @return usage lookup, null if the relation does not apply to the type
	 */
	static Usage usage(Type type, String relation) {
		return switch (supportedType(type)) {
		case PAGE -> switch (relation) {
			case "links_to_page" -> Usage.PAGES_LINKING_PAGE;
			case "uses_tag" -> Usage.PAGES_LINKING_PAGE_TAG;
			case "variants" -> Usage.PAGE_VARIANTS;
			case "uses_template" -> Usage.TEMPLATES_LINKING_PAGE;
			default -> null;
			};
		case FILE -> switch (relation) {
			case "links_to_file" -> Usage.PAGES_LINKING_FILE;
			case "uses_template" -> Usage.TEMPLATES_LINKING_FILE;
			default -> null;
			};
		case IMAGE -> switch (relation) {
			case "links_to_image" -> Usage.PAGES_LINKING_IMAGE;
			case "uses_template" -> Usage.TEMPLATES_LINKING_IMAGE;
			default -> null;
			};
		default -> null;
		};
	}

	@Override
	public Tool tool() {
		Map<String, Object> ref = new LinkedHashMap<>(ObjectRef.jsonSchema("The object to look up."));
		ref.put("required", List.of("type", "id"));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_REF, ref);
		properties.put(ARG_RELATIONS, schema("array", "Relations to look up.", "minItems", 1, "maxItems", 8,
				"uniqueItems", true, "items", schema("string", null, "enum", RELATIONS), "default", List.of("total")));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to look up in.", "minimum", 1));
		properties.put(ARG_SIZE, schema("integer", "Maximum number of objects per relation.", "minimum", 1, "maximum",
				MAX_SIZE, "default", DEFAULT_SIZE));
		properties.put(ARG_FROM, schema("integer", "Offset in each relation.", "minimum", 0, "maximum", MAX_FROM,
				"default", 0));

		Map<String, Object> group = new LinkedHashMap<>();
		group.put("relation", schema("string", null));
		group.put("total", schema("integer", "Referencing objects, including those the caller cannot view."));
		group.put("items", schema("array", null, "items", ObjectRef.jsonSchema(null)));
		Map<String, Object> output = new LinkedHashMap<>();
		output.put("ref", ObjectRef.jsonSchema(null));
		output.put("groups", schema("array", null, "items", schema("object", null, "properties", group)));
		output.put("total", schema("integer", "Total usage count, with relation 'total'."));

		return Tool.builder().name("get_related").title("Get related objects")
				.description("Reverse lookup: which objects reference this one. Use it to find pages to link to, and "
						+ "to check whether an object is safe to take offline or delete. This is relational usage "
						+ "data, not search, so it is exact rather than ranked. Do NOT use it to find textually "
						+ "similar content, that is find_similar.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_REF))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("groups")))
				.build();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		if (!(arguments.get(ARG_REF) instanceof Map<?, ?> refArg)) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_REF));
		}
		Type type = supportedType(Type.fromValue(String.valueOf(((Map<String, Object>) refArg).get("type"))));
		int id = Args.id((Map<String, Object>) refArg, "id");
		List<String> relations = Args.enumList(arguments, ARG_RELATIONS, RELATIONS, List.of("total"));
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
		int size = intArg(arguments, ARG_SIZE, 1, MAX_SIZE, DEFAULT_SIZE);
		int from = intArg(arguments, ARG_FROM, 0, MAX_FROM, 0);
		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		FileResource fileResource = RestPermissions.guard(FileResource.class, new FileResourceImpl());
		ImageResource imageResource = RestPermissions.guard(ImageResource.class, new ImageResourceImpl());
		String idString = Integer.toString(id);
		List<Integer> ids = List.of(id);

		// the usage endpoints do not check the referenced object, so load it (view permission)
		ObjectRef ref = switch (type) {
		case PAGE -> {
			PageLoadResponse response = pageResource.load(idString, false, false, true, false, false, false, false,
					false, false, false, nodeId, null);
			requireOk(response, "The page %d could not be loaded".formatted(id));
			Page page = response.getPage();
			yield nodeId != null ? ObjectRef.forPage(page, nodeId) : ObjectRef.forPage(page);
		}
		case FILE -> {
			FileLoadResponse response = fileResource.load(idString, false, false, nodeId, null);
			requireOk(response, "The file %d could not be loaded".formatted(id));
			yield ObjectRef.forFile(response.getFile(),
					nodeId != null ? nodeId : response.getFile().getInheritedFromId());
		}
		default -> {
			ImageLoadResponse response = imageResource.load(idString, false, false, nodeId, null);
			requireOk(response, "The image %d could not be loaded".formatted(id));
			yield ObjectRef.forImage(response.getImage(),
					nodeId != null ? nodeId : response.getImage().getInheritedFromId());
		}
		};

		List<RelatedGroup> groups = new ArrayList<>();
		Integer total = null;
		for (String relation : relations) {
			if (relation.equals("total")) {
				TotalUsageResponse response = switch (type) {
				case PAGE -> pageResource.getTotalPageUsage(ids, nodeId);
				case FILE -> fileResource.getTotalUsageInfo(ids, nodeId);
				default -> imageResource.getTotalFileUsageInfo(ids, nodeId);
				};
				requireOk(response, "The usage of %s %d could not be counted".formatted(type.value(), id));
				TotalUsageInfo info = response.getInfos() != null ? response.getInfos().get(id) : null;
				total = info != null ? info.getTotal() : 0;
				continue;
			}
			Usage usage = usage(type, relation);
			if (usage == null) {
				groups.add(new RelatedGroup(relation, 0, List.of()));
				continue;
			}
			PageModelParameterBean pageModel = new PageModelParameterBean();
			pageModel.folder = true;
			GenericResponse response = switch (usage) {
			case PAGES_LINKING_PAGE -> pageResource.getPageUsageInfo(from, size, "name", "asc", ids, nodeId, true,
					pageModel);
			case PAGES_LINKING_PAGE_TAG -> pageResource.getPagetagUsageInfo(from, size, "name", "asc", ids, nodeId,
					true, pageModel);
			case PAGE_VARIANTS -> pageResource.getVariantsUsageInfo(from, size, "name", "asc", ids, nodeId, true,
					pageModel);
			case TEMPLATES_LINKING_PAGE -> pageResource.getTemplateUsageInfo(from, size, "name", "asc", ids, nodeId,
					true);
			case PAGES_LINKING_FILE -> fileResource.getPageUsageInfo(from, size, "name", "asc", ids, nodeId, true,
					pageModel);
			case TEMPLATES_LINKING_FILE -> fileResource.getTemplateUsageInfo(from, size, "name", "asc", ids, nodeId,
					true);
			case PAGES_LINKING_IMAGE -> imageResource.getPageUsageInfo(from, size, "name", "asc", ids, nodeId, true,
					pageModel);
			case TEMPLATES_LINKING_IMAGE -> imageResource.getTemplateUsageInfo(from, size, "name", "asc", ids,
					nodeId, true);
			};
			requireOk(response, "The usage '%s' of %s %d could not be loaded".formatted(relation, type.value(), id));
			groups.add(group(relation, response, nodeId));
		}

		return new Result(ref, groups, total);
	}

	/**
	 * Build the group of a usage response
	 * @param relation relation
	 * @param response page or template usage response
	 * @param nodeId node ID of the lookup, may be null
	 * @return group
	 */
	static RelatedGroup group(String relation, GenericResponse response, Integer nodeId) {
		List<ObjectRef> items = new ArrayList<>();
		int total;
		if (response instanceof PageUsageListResponse pages) {
			total = pages.getTotal();
			for (Page page : pages.getPages() != null ? pages.getPages() : List.<Page>of()) {
				items.add(nodeId != null ? ObjectRef.forPage(page, nodeId) : ObjectRef.forPage(page));
			}
		} else {
			TemplateUsageListResponse templates = (TemplateUsageListResponse) response;
			total = templates.getTotal();
			for (Template template : templates.getTemplates() != null ? templates.getTemplates()
					: List.<Template>of()) {
				items.add(ObjectRef.forTemplate(template, nodeId));
			}
		}
		return new RelatedGroup(relation, total, items);
	}
}
