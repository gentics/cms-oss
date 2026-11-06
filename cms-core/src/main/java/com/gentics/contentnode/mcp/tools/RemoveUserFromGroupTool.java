package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.UserGroups;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Removes a user from a group ({@link UserResourceImpl#removeFromGroup}, which needs the permission to assign users to
 * the group, i.e. a group of the caller with that permission above it, and refuses to remove the user's last group).
 * Removing a user who is not a member changes nothing. The user's groups are read back.
 */
public class RemoveUserFromGroupTool extends AbstractMcpTool {
	static final String ARG_USER_ID = "userId";

	static final String ARG_GROUP_ID = "groupId";

	/**
	 * Result of the tool
	 * @param userRef user
	 * @param groupRef group
	 * @param groups the user's groups after the removal, as far as the caller can view them
	 * @param warnings warnings
	 */
	public record Result(ObjectRef userRef, ObjectRef groupRef, List<ObjectRef> groups, List<String> warnings) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_USER_ID, schema("integer", "ID of the user.", "minimum", 1));
		properties.put(ARG_GROUP_ID, schema("integer", "ID of the group.", "minimum", 1));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("userRef", ObjectRef.jsonSchema(null));
		output.put("groupRef", ObjectRef.jsonSchema(null));
		output.put("groups", schema("array", "The user's groups you can view, after the removal.", "items",
				ObjectRef.jsonSchema(null)));
		output.put("warnings", schema("array", null, "items", schema("string", null)));

		return Tool.builder().name("remove_user_from_group").title("Remove user from group")
				.description("Removes a user from a group, revoking the rights that group carried. Show "
						+ "preview_permission_impact first: the person may lose access to work in progress. The CMS "
						+ "refuses to remove a user's last group. You can only remove users from subgroups of your "
						+ "own groups, and you cannot remove your own account from a group here: that is refused with "
						+ "permission-denied, so you cannot lose the rights that allow these changes.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_USER_ID, ARG_GROUP_ID)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("userRef", "groupRef")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int userId = Args.id(arguments, ARG_USER_ID);
		int groupId = Args.id(arguments, ARG_GROUP_ID);
		Writes.logIdempotencyKey("remove_user_from_group", userId, arguments);
		requireNotCaller(userId, session);

		UserResource userResource = RestPermissions.guard(UserResource.class, new UserResourceImpl());
		GroupResource groupResource = RestPermissions.guard(GroupResource.class, new GroupResourceImpl());
		ObjectRef userRef = UserGroups.userRef(userResource, userId);
		ObjectRef groupRef = UserGroups.groupRef(groupResource, groupId);

		userResource.removeFromGroup(Integer.toString(userId), Integer.toString(groupId));

		List<ObjectRef> groups = UserGroups.refs(userResource, userId);
		return new Result(userRef, groupRef, groups, warnings(groups));
	}

	/**
	 * Refuse a removal of the caller's own account
	 * @param userId ID of the user to remove
	 * @param session session of the caller
	 * @throws RestMappedException if the user is the caller
	 */
	static void requireNotCaller(int userId, Optional<Session> session) throws RestMappedException {
		if (session.isPresent() && session.get().getUserId() == userId) {
			throw new RestMappedException("You cannot remove your own account from a group through this server. "
					+ "Use the CMS instead.").setResponseCode(ResponseCode.PERMISSION);
		}
	}

	/**
	 * Get the warnings for the user's groups after the removal
	 * @param groups groups the caller can view
	 * @return warnings
	 */
	static List<String> warnings(List<ObjectRef> groups) {
		return groups.isEmpty()
				? List.of("The user is in no group you can view any more: they only keep groups you cannot see.")
				: List.of();
	}
}
