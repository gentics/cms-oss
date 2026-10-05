package com.gentics.contentnode.mcp.tools;

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
import com.gentics.contentnode.rest.model.perm.RoleItem;
import com.gentics.contentnode.rest.model.perm.TypePermissionItem;
import com.gentics.contentnode.rest.model.response.GroupLoadResponse;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.model.response.TypePermissionResponse;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Returns the permissions of a group on a type or an instance ({@link GroupResourceImpl#getTypePerms} or
 * {@link GroupResourceImpl#getInstancePerms}, which check the view permission on the group and on the type or
 * instance). The instance of type {@code node} is the node's root folder. Pages, templates, files, images, forms and
 * constructs have no permissions of their own, they are granted on folders and nodes, so these types are rejected.
 */
public class GetGroupPermissionsTool extends AbstractMcpTool {
	static final String ARG_GROUP_ID = "groupId";

	static final String ARG_TYPE = "type";

	static final String ARG_INSTANCE_ID = "instanceId";

	/**
	 * Types of the contract
	 */
	static final List<String> TYPES = List.of("page", "folder", "template", "file", "image", "form", "construct",
			"node", "group", "user", "admin", "scheduler");

	/**
	 * Types whose permissions are granted on folders and nodes
	 */
	static final List<String> FOLDER_BOUND_TYPES = List.of("page", "template", "file", "image", "form", "construct");

	/**
	 * A permission of the group
	 * @param name permission name
	 * @param label label
	 * @param description description
	 * @param category category
	 * @param granted whether the group has it
	 * @param editable whether the caller may change it
	 */
	@JsonInclude(Include.NON_NULL)
	public record GroupPermission(String name, String label, String description, String category, boolean granted,
			boolean editable) {
		/**
		 * Map the REST permission item
		 * @param item REST item
		 * @return permission
		 */
		static GroupPermission of(TypePermissionItem item) {
			return new GroupPermission(item.getType().name(), item.getLabel(), item.getDescription(),
					item.getCategory(), item.isValue(), item.isEditable());
		}
	}

	/**
	 * A role of the group on the instance
	 * @param id role ID
	 * @param name role name
	 * @param granted whether the group has it
	 */
	public record GroupRole(int id, String name, boolean granted) {
		/**
		 * Map the REST role item
		 * @param item REST item
		 * @return role
		 */
		static GroupRole of(RoleItem item) {
			return new GroupRole(item.getId(), item.getLabel(), item.isValue());
		}
	}

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param groupRef group
	 * @param type type
	 * @param instanceRef instance, if one was given
	 * @param permissions permissions
	 * @param roles roles, for types with roles
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ObjectRef groupRef, String type, ObjectRef instanceRef, List<GroupPermission> permissions,
			List<GroupRole> roles) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_GROUP_ID, schema("integer", "ID of the group.", "minimum", 1));
		properties.put(ARG_TYPE, schema("string", "Object type. Pages, templates, files, images, forms and constructs "
				+ "have no permissions of their own: use 'folder' or 'node'.", "enum", TYPES));
		properties.put(ARG_INSTANCE_ID, schema("integer", "ID of one object of the type, e.g. a folder. For 'node' "
				+ "the node ID. Required for folder, node, user and group; not allowed for admin and scheduler.",
				"minimum", 1));

		return Tool.builder().name("get_group_permissions").title("Get group permissions")
				.description("Returns what a group may do, either on an object type or on one specific object, as "
						+ "a list of named permissions with labels. This is the read a permission impact preview is "
						+ "built from. It describes a GROUP, not a user: a user's effective rights are the union over "
						+ "their groups, which is what preview_permission_impact computes.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_GROUP_ID, ARG_TYPE)).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int groupId = Args.id(arguments, ARG_GROUP_ID);
		String type = stringArg(arguments, ARG_TYPE, 1, 20);
		if (type == null || !TYPES.contains(type)) {
			throw new IllegalArgumentException("Argument '%s' must be one of %s".formatted(ARG_TYPE, TYPES));
		}
		Integer instanceId = intArg(arguments, ARG_INSTANCE_ID, 1, Integer.MAX_VALUE);
		if (FOLDER_BOUND_TYPES.contains(type)) {
			throw new IllegalArgumentException(("Permissions on %s are granted on folders and nodes: call with type "
					+ "'folder' and a folder ID, or 'node' and a node ID").formatted(type));
		}

		GroupResource resource = RestPermissions.guard(GroupResource.class, new GroupResourceImpl());
		GroupLoadResponse group = resource.get(Integer.toString(groupId), new PermsParameterBean());
		requireOk(group, "The group %d could not be loaded".formatted(groupId));

		ObjectRef instanceRef = null;
		Integer permInstanceId = instanceId;
		if (instanceId != null && type.equals("node")) {
			NodeResource nodeResource = RestPermissions.guard(NodeResource.class, new NodeResourceImpl());
			NodeLoadResponse node = nodeResource.get(Integer.toString(instanceId), false);
			requireOk(node, "The node %d could not be loaded".formatted(instanceId));
			instanceRef = ObjectRef.forNode(node.getNode());
			permInstanceId = node.getNode().getFolderId();
		}

		TypePermissionResponse response = permInstanceId != null
				? resource.getInstancePerms(Integer.toString(groupId), type, permInstanceId)
				: resource.getTypePerms(Integer.toString(groupId), type);
		requireOk(response, "The permissions of group %d could not be loaded".formatted(groupId));
		if (instanceRef == null && instanceId != null) {
			// folder, user or group: the other types have no instances, which the delegate rejected
			instanceRef = ObjectRef.of(Type.fromValue(type), instanceId);
		}

		return new Result(ObjectRef.forGroup(group.getGroup()), type, instanceRef,
				response.getPerms().stream().map(GroupPermission::of).toList(),
				response.getRoles() != null ? response.getRoles().stream().map(GroupRole::of).toList() : null);
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	static Map<String, Object> outputSchema() {
		Map<String, Object> permission = new LinkedHashMap<>();
		permission.put("name", schema("string", "Permission name, e.g. 'read' or 'createitems'."));
		permission.put("label", schema("string", null));
		permission.put("description", schema("string", null));
		permission.put("category", schema("string", null));
		permission.put("granted", schema("boolean", null));
		permission.put("editable", schema("boolean", "Whether you may change it in the CMS."));
		Map<String, Object> role = new LinkedHashMap<>();
		role.put("id", schema("integer", null));
		role.put("name", schema("string", null));
		role.put("granted", schema("boolean", null));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("groupRef", ObjectRef.jsonSchema(null));
		properties.put("type", schema("string", null));
		properties.put("instanceRef", ObjectRef.jsonSchema("The object, for 'node' the node."));
		properties.put("permissions", schema("array", null, "items", schema("object", null, "properties", permission,
				"required", List.of("name", "granted"))));
		properties.put("roles", schema("array", "Roles, only for types with roles.", "items", schema("object", null,
				"properties", role, "required", List.of("id", "granted"))));
		return schema("object", null, "properties", properties, "required", List.of("groupRef", "permissions"));
	}
}
