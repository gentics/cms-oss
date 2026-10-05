package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.PartSpecs;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.PartType;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.resource.PartTypeResource;
import com.gentics.contentnode.rest.resource.impl.PartTypeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PartTypeListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the part types installed in the CMS ({@link PartTypeResourceImpl#list}), marked with whether generated
 * constructs may use them ({@link PartSpecs#ALLOWED_TYPES}). The delegate checks no permission, so the tool checks
 * the view permission on constructs. The part types are fetched unpaged and sliced.
 */
public class ListPartTypesTool extends AbstractMcpTool {
	static final String ARG_ALLOWED_ONLY = "allowedOnly";

	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	/**
	 * A part type in the list. Fields that are not set are omitted on serialization.
	 * @param id ID
	 * @param name name
	 * @param description description
	 * @param javaClass implementing class
	 * @param deprecated whether the type is deprecated
	 * @param allowedForGeneratedConstructs whether create_construct and update_construct accept it
	 * @param requiresDatasource whether a part of this type needs a datasource
	 * @param note why the type is not allowed, if there is a specific reason
	 */
	@JsonInclude(Include.NON_NULL)
	public record PartTypeInfo(int id, String name, String description, String javaClass, boolean deprecated,
			boolean allowedForGeneratedConstructs, boolean requiresDatasource, String note) {
		/**
		 * Map the REST part type
		 * @param type REST part type
		 * @return part type info
		 */
		static PartTypeInfo of(PartType type) {
			String note = type.getId() == Part.HANDLEBARS
					? "The Handlebars template: pass it as handlebarsTemplate, not as a part."
					: null;
			return new PartTypeInfo(type.getId(), type.getName(), type.getDescription(), type.getJavaClass(),
					type.isDeprecated(), PartSpecs.ALLOWED_TYPES.contains(type.getId()),
					PartSpecs.DATASOURCE_TYPES.contains(type.getId()), note);
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = ListArgs.schemaProperties(
				"Optional case-insensitive filter on name, description and Java class.", "part type", "part types",
				LIMITS);
		properties.put(ARG_ALLOWED_ONLY, schema("boolean", "Return only the part types allowed for generated "
				+ "constructs.", "default", false));

		Map<String, Object> itemProperties = new LinkedHashMap<>();
		itemProperties.put("id", schema("integer", null));
		itemProperties.put("name", schema("string", null));
		itemProperties.put("description", schema("string", null));
		itemProperties.put("javaClass", schema("string", null));
		itemProperties.put("deprecated", schema("boolean", null));
		itemProperties.put("allowedForGeneratedConstructs", schema("boolean",
				"Whether create_construct and update_construct accept the type."));
		itemProperties.put("requiresDatasource", schema("boolean", "Whether a part of the type needs a "
				+ "datasourceId."));
		itemProperties.put("note", schema("string", null));

		return Tool.builder().name("list_part_types").title("List part types")
				.description("Lists the part types installed in this CMS, and which of them may be used in a "
						+ "construct created through this server. ALWAYS call this before create_construct or "
						+ "update_construct and pick type ids it reports with allowedForGeneratedConstructs true. "
						+ "Type id 43 is never allowed, because the Handlebars template goes into create_construct's "
						+ "handlebarsTemplate rather than into a part. To see the parts of an existing construct, use "
						+ "get_construct.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(ListResult.jsonSchema(schema("object", null, "properties", itemProperties, "required",
						List.of("id")), "part types"))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		ListArgs args = ListArgs.of(arguments, LIMITS);
		boolean allowedOnly = booleanArg(arguments, ARG_ALLOWED_ONLY, false);

		try (Trx trx = ContentNodeHelper.trx()) {
			if (!trx.getTransaction().getPermHandler().canView(null, Construct.class, null)) {
				throw new InsufficientPrivilegesException("Missing permission to view constructs", null, null,
						Construct.TYPE_CONSTRUCT, 0, PermType.read);
			}
			trx.success();
		}

		PartTypeResource resource = RestPermissions.guard(PartTypeResource.class, new PartTypeResourceImpl());
		List<PartType> types = ListResponses.items(resource.list(new FilterParameterBean().setQuery(args.query()),
				new SortParameterBean(), new PagingParameterBean(), new PartTypeListParameterBean()));
		return result(types, allowedOnly, args);
	}

	/**
	 * Map, filter and slice the part types
	 * @param types REST part types
	 * @param allowedOnly whether to keep only the allowed types
	 * @param args list arguments
	 * @return result
	 */
	static ListResult<PartTypeInfo> result(List<PartType> types, boolean allowedOnly, ListArgs args) {
		List<PartTypeInfo> all = types.stream().map(PartTypeInfo::of)
				.filter(type -> !allowedOnly || type.allowedForGeneratedConstructs()).toList();
		Slice slice = args.slice(all.size());
		return ListResult.of(slice, slice.apply(all));
	}
}
