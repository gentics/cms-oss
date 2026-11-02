package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the groups ({@link GroupResourceImpl#list}, which needs the group admin view permission and lists the
 * caller's groups and their subgroups). The groups are fetched unpaged and sliced; parent and member count of the
 * slice are read in an own transaction. The parent is only returned if the caller can view it, the member count only
 * if the caller can view users.
 */
public class ListGroupsTool extends AbstractMcpTool {
	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	/**
	 * A group. Fields that are not set are omitted on serialization.
	 * @param ref group ref
	 * @param description description
	 * @param memberCount number of members
	 * @param parentGroupRef parent group
	 */
	@JsonInclude(Include.NON_NULL)
	public record GroupItem(ObjectRef ref, String description, Integer memberCount, ObjectRef parentGroupRef) {
		/**
		 * Build the output schema. Must be kept in sync with the components.
		 * @return schema
		 */
		static Map<String, Object> jsonSchema() {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("ref", ObjectRef.jsonSchema(null));
			properties.put("description", schema("string", null));
			properties.put("memberCount", schema("integer", "Number of members, if you may view users."));
			properties.put("parentGroupRef", ObjectRef.jsonSchema("Parent group, if you may view it."));
			return schema("object", null, "properties", properties, "required", List.of("ref"));
		}
	}

	@Override
	public Tool tool() {
		return Tool.builder().name("list_groups").title("List groups")
				.description("Lists the CMS groups. Groups carry the permissions that make up a role, so this is how "
						+ "you find the group that corresponds to a role the user names. Group names alone do not "
						+ "prove what a group may do: confirm with get_group_permissions. Lists your own groups and "
						+ "their subgroups.")
				.inputSchema(JsonSchema.builder().type("object")
						.properties(ListArgs.schemaProperties(
								"Optional case-insensitive filter on name and description.", "group", "groups", LIMITS))
						.required(List.of()).additionalProperties(false).build())
				.outputSchema(ListResult.jsonSchema(GroupItem.jsonSchema(), "groups"))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		ListArgs args = ListArgs.of(arguments, LIMITS);

		GroupResource resource = RestPermissions.guard(GroupResource.class, new GroupResourceImpl());
		List<Group> groups = ListResponses.items(resource.list(new FilterParameterBean().setQuery(args.query()),
				new SortParameterBean().setSort("name"), new PagingParameterBean(), new PermsParameterBean(),
				new PermsFilterParameterBean()));
		Slice slice = args.slice(groups.size());

		List<GroupItem> items = new ArrayList<>();
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			boolean viewUsers = t.getPermHandler().canView(null, SystemUser.class, null);
			for (Group group : slice.apply(groups)) {
				UserGroup object = t.getObject(UserGroup.class, group.getId());
				UserGroup mother = object != null ? object.getMother() : null;
				ObjectRef parent = mother != null && t.getPermHandler().canView(mother)
						? ObjectRef.forGroup(UserGroup.TRANSFORM2REST.apply(mother))
						: null;
				Integer memberCount = viewUsers && object != null ? object.getMembers().size() : null;
				items.add(new GroupItem(ObjectRef.forGroup(group), group.getDescription(), memberCount, parent));
			}
			trx.success();
		}
		return ListResult.of(slice, items);
	}
}
