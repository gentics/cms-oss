package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.UserGroups;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.lib.log.NodeLogger;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Adds a user to a group ({@link UserResourceImpl#addToGroup}), optionally restricted to nodes
 * ({@link UserResourceImpl#addGroupNodeRestriction}). Both need the permission to assign users to the group, i.e. a
 * group of the caller with that permission above it. The nodes are loaded before anything changes. Each delegate runs
 * its own transaction: if a restriction fails, a membership added by this call is removed again. Restrictions are
 * added to existing ones.
 */
public class AssignUserToGroupTool extends AbstractMcpTool {
	private static final NodeLogger logger = NodeLogger.getNodeLogger(AssignUserToGroupTool.class);

	static final String ARG_USER_ID = "userId";

	static final String ARG_GROUP_ID = "groupId";

	static final String ARG_NODE_IDS = "nodeIds";

	/**
	 * Maximum number of nodes
	 */
	static final int MAX_NODES = 50;

	/**
	 * Result of the tool
	 * @param userRef user
	 * @param groupRef group
	 * @param groups the user's groups, as far as the caller can view them
	 * @param nodeRefs nodes the membership is restricted to (as far as the caller can view them), empty for all nodes
	 */
	public record Result(ObjectRef userRef, ObjectRef groupRef, List<ObjectRef> groups, List<ObjectRef> nodeRefs) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_USER_ID, schema("integer", "ID of the user.", "minimum", 1));
		properties.put(ARG_GROUP_ID, schema("integer", "ID of the group.", "minimum", 1));
		properties.put(ARG_NODE_IDS, schema("array", "Restrict the membership to these nodes, in addition to existing "
				+ "restrictions. Omit for all nodes.", "maxItems", MAX_NODES, "uniqueItems", true, "items",
				schema("integer", null, "minimum", 1)));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("userRef", ObjectRef.jsonSchema(null));
		output.put("groupRef", ObjectRef.jsonSchema(null));
		output.put("groups", schema("array", "The user's groups you can view.", "items", ObjectRef.jsonSchema(null)));
		output.put("nodeRefs", schema("array", "Nodes the membership is restricted to, empty for all nodes.", "items",
				ObjectRef.jsonSchema(null)));

		return Tool.builder().name("assign_user_to_group").title("Assign user to group")
				.description("Adds an existing user to a group, which is how a role is granted. Optionally restrict "
						+ "the membership to specific nodes. Show preview_permission_impact first and get "
						+ "confirmation: this changes what the person can do immediately. You can only assign users "
						+ "to subgroups of your own groups. Repeating the call is harmless.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_USER_ID, ARG_GROUP_ID)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("userRef", "groupRef")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int userId = Args.id(arguments, ARG_USER_ID);
		int groupId = Args.id(arguments, ARG_GROUP_ID);
		List<Integer> nodeIds = Args.distinctIntList(arguments, ARG_NODE_IDS, MAX_NODES);
		Writes.logIdempotencyKey("assign_user_to_group", userId, arguments);

		UserResource userResource = RestPermissions.guard(UserResource.class, new UserResourceImpl());
		GroupResource groupResource = RestPermissions.guard(GroupResource.class, new GroupResourceImpl());
		NodeResource nodeResource = RestPermissions.guard(NodeResource.class, new NodeResourceImpl());
		ObjectRef userRef = UserGroups.userRef(userResource, userId);
		ObjectRef groupRef = UserGroups.groupRef(groupResource, groupId);
		Map<Integer, ObjectRef> nodes = new LinkedHashMap<>();
		for (int nodeId : nodeIds != null ? nodeIds : List.<Integer>of()) {
			NodeLoadResponse node = nodeResource.get(Integer.toString(nodeId), false);
			requireOk(node, "The node %d could not be loaded".formatted(nodeId));
			nodes.put(nodeId, ObjectRef.forNode(node.getNode()));
		}

		String user = Integer.toString(userId);
		String group = Integer.toString(groupId);
		boolean wasMember = UserGroups.refs(userResource, userId).stream().anyMatch(ref -> ref.id() == groupId);
		userResource.addToGroup(user, group);
		try {
			for (int nodeId : nodes.keySet()) {
				userResource.addGroupNodeRestriction(user, group, Integer.toString(nodeId));
			}
		} catch (Exception e) {
			if (!wasMember) {
				compensate(userResource, user, group, e);
			}
			throw e;
		}

		List<ObjectRef> nodeRefs = new ArrayList<>();
		for (int nodeId : userResource.getGroupNodeRestrictions(user, group).getNodeIds()) {
			nodeRefs.add(nodes.getOrDefault(nodeId, ObjectRef.of(Type.NODE, nodeId)));
		}
		return new Result(userRef, groupRef, UserGroups.refs(userResource, userId), nodeRefs);
	}

	/**
	 * Remove the membership added by this call after a failed restriction
	 * @param userResource user resource
	 * @param user user ID
	 * @param group group ID
	 * @param failure failure of the restriction
	 * @throws IllegalStateException if the membership could not be removed, naming it
	 */
	static void compensate(UserResource userResource, String user, String group, Exception failure) {
		try {
			userResource.removeFromGroup(user, group);
		} catch (Exception e) {
			logger.error("User %s may remain in group %s without node restriction".formatted(user, group), e);
			// no cause, so that the problem detail names the remaining membership
			IllegalStateException remaining = new IllegalStateException(("Restricting the membership failed (%s), "
					+ "and user %s may remain in group %s for all nodes: remove it with remove_user_from_group")
					.formatted(failure.getMessage(), user, group));
			remaining.addSuppressed(failure);
			remaining.addSuppressed(e);
			throw remaining;
		}
	}
}
