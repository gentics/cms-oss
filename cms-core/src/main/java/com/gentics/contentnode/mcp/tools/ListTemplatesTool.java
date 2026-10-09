package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.TemplateInNode;
import com.gentics.contentnode.rest.model.response.TemplateListResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.ReducedListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TemplateListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists templates: those linked to a folder ({@link FolderResourceImpl#getTemplates}), those assigned to a node
 * ({@link NodeResourceImpl#getTemplates}), or those of every node the caller can view ({@link TemplateResourceImpl#list},
 * each template once). Every delegate filters by {@code ObjectPermission.view}. The templates are fetched unpaged and
 * sliced.
 */
public class ListTemplatesTool extends AbstractMcpTool {
	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_FOLDER_ID = "folderId";

	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	/**
	 * A template in the list. Fields that are not set are omitted on serialization.
	 * @param ref reference to the template
	 * @param description description
	 * @param markupLanguage name of the markup language
	 * @param locked whether the template is locked
	 */
	@JsonInclude(Include.NON_NULL)
	public record TemplateSummary(ObjectRef ref, String description, String markupLanguage, boolean locked) {
		/**
		 * Map the REST template. Without a node ID, a template listed for a node gets that node.
		 * @param template REST template
		 * @param nodeId node ID of the listing, may be null
		 * @return summary
		 */
		static TemplateSummary of(Template template, Integer nodeId) {
			Integer refNodeId = nodeId == null && template instanceof TemplateInNode inNode ? inNode.getNodeId()
					: nodeId;
			return new TemplateSummary(ObjectRef.forTemplate(template, refNodeId), template.getDescription(),
					template.getMarkupLanguage() != null ? template.getMarkupLanguage().getName() : null,
					template.isLocked());
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_NODE_ID, schema("integer", "Restrict to the templates assigned to this node.", "minimum", 1));
		properties.put(ARG_FOLDER_ID, schema("integer",
				"Restrict to the templates linked to this folder, the set create_page accepts there.", "minimum", 1));
		properties.putAll(ListArgs.schemaProperties("Optional case-insensitive filter on the template name.",
				"template", "templates", LIMITS));

		Map<String, Object> itemProperties = new LinkedHashMap<>();
		itemProperties.put("ref", ObjectRef.jsonSchema(null));
		itemProperties.put("description", schema("string", null));
		itemProperties.put("markupLanguage", schema("string", null));
		itemProperties.put("locked", schema("boolean", null));

		return Tool.builder().name("list_templates").title("List templates")
				.description("Lists the page templates a folder or node allows, with the fields a client needs to let "
						+ "a user choose one before a page is created. Scope it with folderId whenever the target "
						+ "folder is known: the result is then exactly the set create_page accepts for that folder. "
						+ "Use it to offer template options, and get_template to inspect one; it does not return "
						+ "template source.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(ListResult.jsonSchema(schema("object", null, "properties", itemProperties, "required",
						List.of("ref")), "templates"))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
		Integer folderId = intArg(arguments, ARG_FOLDER_ID, 1, Integer.MAX_VALUE);
		ListArgs args = ListArgs.of(arguments, LIMITS);

		List<? extends Template> templates;
		if (folderId != null) {
			InFolderParameterBean inFolder = new InFolderParameterBean();
			inFolder.folderId = Integer.toString(folderId);
			LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
			filter.search = args.query();
			TemplateListResponse response = RestPermissions.guard(FolderResource.class, new FolderResourceImpl())
					.getTemplates(inFolder.folderId, inFolder, new TemplateListParameterBean().setFolderNodeId(nodeId),
							filter, new LegacySortParameterBean(), new LegacyPagingParameterBean(),
							new EditableParameterBean(), new WastebinParameterBean());
			requireOk(response, "The templates of folder %d could not be listed".formatted(folderId));
			templates = response.getTemplates() != null ? response.getTemplates() : List.of();
		} else if (nodeId != null) {
			templates = ListResponses.items(RestPermissions.guard(NodeResource.class, new NodeResourceImpl())
					.getTemplates(Integer.toString(nodeId), new FilterParameterBean().setQuery(args.query()),
							new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean()));
		} else {
			templates = ListResponses.items(RestPermissions.guard(TemplateResource.class, new TemplateResourceImpl())
					.list(null, new FilterParameterBean().setQuery(args.query()), new SortParameterBean(),
							new PagingParameterBean(), new PermsParameterBean(), new ReducedListParameterBean()));
		}

		return result(templates, nodeId, args);
	}

	/**
	 * Remove duplicates (a template assigned to several nodes is listed once per node), keep the delegate's order and
	 * slice
	 * @param templates REST templates
	 * @param nodeId node ID of the listing, may be null
	 * @param args list arguments
	 * @return result
	 */
	static ListResult<TemplateSummary> result(List<? extends Template> templates, Integer nodeId, ListArgs args) {
		Set<Integer> seen = new HashSet<>();
		List<TemplateSummary> all = new ArrayList<>();
		for (Template template : templates) {
			if (seen.add(template.getId())) {
				all.add(TemplateSummary.of(template, nodeId));
			}
		}
		Slice slice = args.slice(all.size());
		return ListResult.of(slice, slice.apply(all));
	}
}
