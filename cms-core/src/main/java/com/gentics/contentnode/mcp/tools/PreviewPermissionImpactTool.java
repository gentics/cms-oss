package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.UserGroups;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.perm.TypePermissionItem;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.model.response.NodeRestrictionResponse;
import com.gentics.contentnode.rest.model.response.TypePermissionResponse;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Previews the permissions a user would gain or lose if groups were added or removed, without changing anything. The
 * CMS cannot evaluate a hypothetical membership, so the preview is composed: the permissions of each group before and
 * after ({@link GroupResourceImpl#getTypePerms}, {@link GroupResourceImpl#getInstancePerms} on the root folder of each
 * node) are merged with OR, as the CMS merges group permissions, respecting the user's node restrictions per group
 * ({@link UserResourceImpl#getGroupNodeRestrictions}), and diffed. Folder-tree inheritance, channels, roles and
 * languages are not evaluated.
 */
public class PreviewPermissionImpactTool extends AbstractMcpTool {
	static final String ARG_USER_ID = "userId";

	static final String ARG_ADD_GROUP_IDS = "addGroupIds";

	static final String ARG_REMOVE_GROUP_IDS = "removeGroupIds";

	static final String ARG_TYPES = "types";

	static final String ARG_NODE_IDS = "nodeIds";

	/**
	 * Maximum number of groups or nodes per list
	 */
	static final int MAX_IDS = 20;

	/**
	 * Types of the contract
	 */
	static final List<String> TYPES = List.of("page", "folder", "template", "file", "image", "form", "construct",
			"node", "group", "user", "admin");

	/**
	 * Default types
	 */
	static final List<String> DEFAULT_TYPES = List.of("page", "folder", "template", "construct", "admin");

	/**
	 * Permission types of the CMS evaluated for the type-level types
	 */
	static final Map<String, List<String>> TYPE_SCOPES = Map.of("admin", List.of("admin", "useradmin", "groupadmin"),
			"user", List.of("useradmin"), "group", List.of("groupadmin"));

	/**
	 * Permissions of a node's root folder evaluated for the node-bound types, empty for all
	 */
	static final Map<String, Set<String>> NODE_PERMISSIONS = Map.of(
			"folder", Set.of("read", "setperm", "create", "updatefolder", "deletefolder", "linkoverview",
					"createoverview"),
			"page", Set.of("readitems", "createitems", "updateitems", "deleteitems", "importitems", "publishpages",
					"translatepages"),
			"file", Set.of("readitems", "createitems", "updateitems", "deleteitems", "importitems"),
			"image", Set.of("readitems", "createitems", "updateitems", "deleteitems", "importitems"),
			"form", Set.of("viewform", "createform", "updateform", "deleteform", "publishform", "formreport"),
			"template", Set.of("readtemplates", "createtemplates", "updatetemplates", "deletetemplates",
					"linktemplates"),
			"construct", Set.of("updateconstructs"),
			"node", Set.of());

	/**
	 * Warning for previews on nodes
	 */
	static final String NODE_WARNING = "Permissions on nodes are those of the node's root folder: folder-tree "
			+ "inheritance below the root folder, channels, roles and languages are not evaluated.";

	/**
	 * A permission granted or not
	 * @param scope scope: a CMS permission type like 'admin' or 'useradmin', or a requested type on a node
	 * @param scopeRef node of the scope, not set for type-level scopes
	 * @param permission permission name
	 * @param granted whether the user has it
	 */
	@JsonInclude(Include.NON_NULL)
	public record PermissionGrant(String scope, ObjectRef scopeRef, String permission, boolean granted) {
	}

	/**
	 * A permission gained or lost
	 * @param scope scope
	 * @param scopeRef node of the scope, not set for type-level scopes
	 * @param permission permission name
	 */
	@JsonInclude(Include.NON_NULL)
	public record PermissionChange(String scope, ObjectRef scopeRef, String permission) {
	}

	/**
	 * Result of the tool
	 * @param userRef user
	 * @param before permissions with the current groups
	 * @param after permissions with the changed groups
	 * @param added permissions gained
	 * @param removed permissions lost
	 * @param groupsBefore current groups the caller can view
	 * @param groupsAfter changed groups
	 * @param computedBy always 'composition'
	 * @param warnings what the preview does not cover
	 */
	public record Result(ObjectRef userRef, List<PermissionGrant> before, List<PermissionGrant> after,
			List<PermissionChange> added, List<PermissionChange> removed, List<ObjectRef> groupsBefore,
			List<ObjectRef> groupsAfter, String computedBy, List<String> warnings) {
	}

	/**
	 * A scope to evaluate
	 * @param name scope name
	 * @param ref node, null for type-level scopes
	 * @param source CMS permission type ('node' for the node's root folder)
	 * @param permissions permissions to evaluate, empty for all
	 */
	record Scope(String name, ObjectRef ref, String source, Set<String> permissions) {
		/**
		 * Get the key of the permissions read for this scope
		 * @return key
		 */
		String key() {
			return ref != null ? source + ":" + ref.id() : source;
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_USER_ID, schema("integer", "ID of the user.", "minimum", 1));
		properties.put(ARG_ADD_GROUP_IDS, idsSchema("Groups the user would be added to."));
		properties.put(ARG_REMOVE_GROUP_IDS, idsSchema("Groups the user would be removed from."));
		properties.put(ARG_TYPES, schema("array", "What to evaluate. 'admin', 'user' and 'group' are type-level, the "
				+ "others are evaluated on the root folder of each node in nodeIds.", "maxItems", TYPES.size(),
				"uniqueItems", true, "items", schema("string", null, "enum", TYPES), "default", DEFAULT_TYPES));
		properties.put(ARG_NODE_IDS, idsSchema("Nodes to evaluate the node-bound types on. Without nodes they are "
				+ "skipped."));

		Map<String, Object> grant = new LinkedHashMap<>();
		grant.put("scope", schema("string", null));
		grant.put("scopeRef", ObjectRef.jsonSchema("Node of the scope."));
		grant.put("permission", schema("string", null));
		grant.put("granted", schema("boolean", null));
		Map<String, Object> change = new LinkedHashMap<>(grant);
		change.remove("granted");
		Map<String, Object> grants = schema("array", null, "items", schema("object", null, "properties", grant,
				"required", List.of("scope", "permission", "granted")));
		Map<String, Object> changes = schema("array", null, "items", schema("object", null, "properties", change,
				"required", List.of("scope", "permission")));

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("userRef", ObjectRef.jsonSchema(null));
		output.put("before", grants);
		output.put("after", grants);
		output.put("added", changes);
		output.put("removed", changes);
		output.put("groupsBefore", schema("array", null, "items", ObjectRef.jsonSchema(null)));
		output.put("groupsAfter", schema("array", null, "items", ObjectRef.jsonSchema(null)));
		output.put("computedBy", schema("string", null, "const", "composition"));
		output.put("warnings", schema("array", null, "items", schema("string", null)));

		return Tool.builder().name("preview_permission_impact").title("Preview permission impact")
				.description("Shows what a user will and will not be able to do if the listed groups are added or "
						+ "removed, WITHOUT changing anything. Always run this and show the result before "
						+ "assign_user_to_group or remove_user_from_group (for create_user, show the target group's "
						+ "permissions with get_group_permissions). It is a composition of the group permissions the "
						+ "user would hold, not a CMS simulation: it does not reproduce folder-tree inheritance or "
						+ "every role and language subtlety, so present it as a preview and read the warnings it "
						+ "returns.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_USER_ID))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("userRef", "before",
						"after", "computedBy")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int userId = Args.id(arguments, ARG_USER_ID);
		List<Integer> addIds = orEmpty(Args.distinctIntList(arguments, ARG_ADD_GROUP_IDS, MAX_IDS));
		List<Integer> removeIds = orEmpty(Args.distinctIntList(arguments, ARG_REMOVE_GROUP_IDS, MAX_IDS));
		List<String> types = Args.enumList(arguments, ARG_TYPES, TYPES, DEFAULT_TYPES);
		List<Integer> nodeIds = orEmpty(Args.distinctIntList(arguments, ARG_NODE_IDS, MAX_IDS));

		UserResource userResource = RestPermissions.guard(UserResource.class, new UserResourceImpl());
		GroupResource groupResource = RestPermissions.guard(GroupResource.class, new GroupResourceImpl());
		NodeResource nodeResource = RestPermissions.guard(NodeResource.class, new NodeResourceImpl());
		ObjectRef userRef = UserGroups.userRef(userResource, userId);
		List<ObjectRef> groupsBefore = UserGroups.refs(userResource, userId);
		Map<Integer, ObjectRef> groups = new LinkedHashMap<>();
		groupsBefore.forEach(group -> groups.put(group.id(), group));
		for (int groupId : addIds) {
			groups.putIfAbsent(groupId, UserGroups.groupRef(groupResource, groupId));
		}
		for (int groupId : removeIds) {
			groups.putIfAbsent(groupId, UserGroups.groupRef(groupResource, groupId));
		}

		List<String> warnings = new ArrayList<>();
		List<ObjectRef> groupsAfter = new ArrayList<>();
		for (ObjectRef group : groupsBefore) {
			if (!removeIds.contains(group.id())) {
				groupsAfter.add(group);
			}
		}
		for (int groupId : addIds) {
			if (groupsAfter.stream().anyMatch(group -> group.id() == groupId)) {
				warnings.add("The user already is in group %d.".formatted(groupId));
			} else {
				groupsAfter.add(groups.get(groupId));
			}
		}
		for (int groupId : removeIds) {
			if (groupsBefore.stream().noneMatch(group -> group.id() == groupId)) {
				warnings.add("The user is not in group %d.".formatted(groupId));
			}
		}

		// scopes, and the root folder of each node
		List<Scope> scopes = new ArrayList<>();
		for (String type : types) {
			if (TYPE_SCOPES.containsKey(type)) {
				for (String scope : TYPE_SCOPES.get(type)) {
					if (scopes.stream().noneMatch(s -> s.name().equals(scope))) {
						scopes.add(new Scope(scope, null, scope, Set.of()));
					}
				}
			}
		}
		Map<Integer, Integer> rootFolders = new HashMap<>();
		List<String> nodeTypes = types.stream().filter(NODE_PERMISSIONS::containsKey).toList();
		if (!nodeTypes.isEmpty() && nodeIds.isEmpty()) {
			warnings.add("%s not evaluated: pass nodeIds.".formatted(String.join(", ", nodeTypes)));
		}
		if (!nodeTypes.isEmpty() && !nodeIds.isEmpty()) {
			for (int nodeId : nodeIds) {
				NodeLoadResponse node = nodeResource.get(Integer.toString(nodeId), false);
				requireOk(node, "The node %d could not be loaded".formatted(nodeId));
				rootFolders.put(nodeId, node.getNode().getFolderId());
				ObjectRef nodeRef = ObjectRef.forNode(node.getNode());
				for (String type : nodeTypes) {
					scopes.add(new Scope(type, nodeRef, "node", NODE_PERMISSIONS.get(type)));
				}
			}
			warnings.add(NODE_WARNING);
		}

		// node restrictions of the current memberships, only needed for scopes on nodes
		Map<Integer, Set<Integer>> restrictions = new HashMap<>();
		if (!rootFolders.isEmpty()) {
			for (ObjectRef group : groupsBefore) {
				NodeRestrictionResponse response = userResource.getGroupNodeRestrictions(Integer.toString(userId),
						Integer.toString(group.id()));
				boolean restricted = !response.getNodeIds().isEmpty()
						|| (response.getHidden() != null && response.getHidden() > 0);
				if (restricted) {
					restrictions.put(group.id(), response.getNodeIds());
				}
			}
		}

		// permissions per group and scope source
		Map<Integer, Map<String, Map<String, Boolean>>> permissions = new HashMap<>();
		for (int groupId : groups.keySet()) {
			Map<String, Map<String, Boolean>> perGroup = new HashMap<>();
			for (Scope scope : scopes) {
				if (!perGroup.containsKey(scope.key())) {
					String group = Integer.toString(groupId);
					TypePermissionResponse response = scope.ref() != null
							? groupResource.getInstancePerms(group, scope.source(), rootFolders.get(scope.ref().id()))
							: groupResource.getTypePerms(group, scope.source());
					requireOk(response, "The permissions of group %d could not be loaded".formatted(groupId));
					Map<String, Boolean> values = new LinkedHashMap<>();
					for (TypePermissionItem item : response.getPerms()) {
						values.put(item.getType().name(), item.isValue());
					}
					perGroup.put(scope.key(), values);
				}
			}
			permissions.put(groupId, perGroup);
		}

		int hidden = hiddenGroups(userId, groupsBefore.size());
		if (hidden > 0) {
			warnings.add("The user is in %d group(s) you cannot view, which are not evaluated.".formatted(hidden));
		}

		List<PermissionGrant> before = compose(scopes, ids(groupsBefore), permissions, restrictions);
		List<PermissionGrant> after = compose(scopes, ids(groupsAfter), permissions, restrictions);
		return new Result(userRef, before, after, diff(after, before), diff(before, after), groupsBefore, groupsAfter,
				"composition", warnings);
	}

	/**
	 * Compose the permissions of a set of groups: a permission is granted if one of the groups grants it. A group whose
	 * membership is restricted to other nodes grants nothing on a node.
	 * @param scopes scopes
	 * @param groupIds IDs of the groups
	 * @param permissions permissions per group, scope key and permission name (of every group, for the names)
	 * @param restrictions node restrictions per group, groups without restrictions are missing
	 * @return grants, per scope in the order of the permission names
	 */
	static List<PermissionGrant> compose(List<Scope> scopes, List<Integer> groupIds,
			Map<Integer, Map<String, Map<String, Boolean>>> permissions, Map<Integer, Set<Integer>> restrictions) {
		List<PermissionGrant> grants = new ArrayList<>();
		for (Scope scope : scopes) {
			Set<String> names = new LinkedHashSet<>();
			for (Map<String, Map<String, Boolean>> perGroup : permissions.values()) {
				names.addAll(perGroup.getOrDefault(scope.key(), Map.of()).keySet());
			}
			for (String name : names) {
				if (!scope.permissions().isEmpty() && !scope.permissions().contains(name)) {
					continue;
				}
				boolean granted = false;
				for (int groupId : groupIds) {
					Set<Integer> nodes = restrictions.get(groupId);
					if (scope.ref() != null && nodes != null && !nodes.contains(scope.ref().id())) {
						continue;
					}
					granted |= Boolean.TRUE.equals(permissions.getOrDefault(groupId, Map.of())
							.getOrDefault(scope.key(), Map.of()).get(name));
				}
				grants.add(new PermissionGrant(scope.name(), scope.ref(), name, granted));
			}
		}
		return grants;
	}

	/**
	 * Get the permissions granted in one list, but not in the other
	 * @param grants grants
	 * @param other other grants
	 * @return changes
	 */
	static List<PermissionChange> diff(List<PermissionGrant> grants, List<PermissionGrant> other) {
		List<PermissionChange> changes = new ArrayList<>();
		for (PermissionGrant grant : grants) {
			boolean granted = other.stream().anyMatch(o -> o.granted() && o.scope().equals(grant.scope())
					&& o.permission().equals(grant.permission()) && Objects.equals(o.scopeRef(), grant.scopeRef()));
			if (grant.granted() && !granted) {
				changes.add(new PermissionChange(grant.scope(), grant.scopeRef(), grant.permission()));
			}
		}
		return changes;
	}

	/**
	 * Count the groups of the user the caller cannot view
	 * @param userId user ID
	 * @param visible number of groups the caller can view
	 * @return number of other groups
	 * @throws Exception if the user cannot be read
	 */
	static int hiddenGroups(int userId, int visible) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			SystemUser user = trx.getTransaction().getObject(SystemUser.class, userId);
			int hidden = user != null ? user.getUserGroups().size() - visible : 0;
			trx.success();
			return hidden;
		}
	}

	/**
	 * Build the schema of a list of IDs
	 * @param description description
	 * @return schema
	 */
	private static Map<String, Object> idsSchema(String description) {
		return schema("array", description, "maxItems", MAX_IDS, "uniqueItems", true, "items", schema("integer", null,
				"minimum", 1));
	}

	/**
	 * Get the IDs of refs
	 * @param refs refs
	 * @return IDs
	 */
	private static List<Integer> ids(List<ObjectRef> refs) {
		return refs.stream().map(ObjectRef::id).toList();
	}

	/**
	 * Replace null by an empty list
	 * @param ids list, may be null
	 * @return list
	 */
	private static List<Integer> orEmpty(List<Integer> ids) {
		return ids != null ? ids : List.of();
	}
}
