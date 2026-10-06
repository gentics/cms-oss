package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.UserItem;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists users ({@link UserResourceImpl#list}, which needs the user admin view permission and lists the users of the
 * caller's groups and their subgroups). The users are fetched unpaged and sliced; the groups of the slice are read in
 * an own transaction, limited to the groups the caller can view (as {@link UserResourceImpl#groups} does).
 */
public class ListUsersTool extends AbstractMcpTool {
	static final String ARG_INCLUDE_GROUPS = "includeGroups";

	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	@Override
	public Tool tool() {
		Map<String, Object> properties = ListArgs.schemaProperties(
				"Optional case-insensitive filter on login, first name, last name and email.", "user", "users", LIMITS);
		properties.put(ARG_INCLUDE_GROUPS, schema("boolean", "Whether to include the groups of each user you may view.",
				"default", true));

		return Tool.builder().name("list_users").title("List users")
				.description("Finds CMS users by name, login or email. Call it before create_user to check whether the "
						+ "person already has an account, which is the common case in an existing installation. Lists "
						+ "the users of your own groups and their subgroups.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(ListResult.jsonSchema(UserItem.jsonSchema(null), "users"))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		ListArgs args = ListArgs.of(arguments, LIMITS);
		boolean includeGroups = booleanArg(arguments, ARG_INCLUDE_GROUPS, true);

		UserResource resource = RestPermissions.guard(UserResource.class, new UserResourceImpl());
		List<User> users = ListResponses.items(resource.list(new FilterParameterBean().setQuery(args.query()),
				new SortParameterBean().setSort("login"), new PagingParameterBean(), new PermsParameterBean(),
				new EmbedParameterBean()));
		Slice slice = args.slice(users.size());

		List<UserItem> items = new ArrayList<>();
		try (Trx trx = ContentNodeHelper.trx()) {
			for (User user : slice.apply(users)) {
				items.add(UserItem.of(user, includeGroups ? visibleGroups(user.getId()) : null));
			}
			trx.success();
		}
		return ListResult.of(slice, items);
	}

	/**
	 * Get the groups of a user the caller can view, in the current transaction
	 * @param userId user ID
	 * @return group refs
	 * @throws Exception if the groups cannot be read
	 */
	static List<ObjectRef> visibleGroups(int userId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		SystemUser user = t.getObject(SystemUser.class, userId);
		List<ObjectRef> groups = new ArrayList<>();
		if (user != null) {
			for (UserGroup group : user.getUserGroups()) {
				if (t.getPermHandler().canView(group)) {
					groups.add(ObjectRef.forGroup(UserGroup.TRANSFORM2REST.apply(group)));
				}
			}
		}
		return groups;
	}
}
