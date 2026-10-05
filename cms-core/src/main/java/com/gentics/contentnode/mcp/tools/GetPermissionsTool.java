package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.response.PermBitsResponse;
import com.gentics.contentnode.rest.model.response.PermResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PermResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Returns the caller's effective permissions on one object, or on a whole object type, as named
 * booleans.
 *
 * <p>
 * With {@code id}, every permission is checked with {@link PermResourceImpl#getObjectPermission}
 * ({@code GET /perm/{perm}/{type}/{id}}), after a {@code view} check: an object the caller cannot
 * view (or that does not exist) is answered with a permission refusal, without its name. Without
 * {@code id}, the type-level map of {@link PermResourceImpl#getPermissions(String, boolean)}
 * ({@code GET /perm/{type}?map=true}) is used. Both evaluate the permissions of the transaction's
 * user, i.e. the caller, so no further permission check is needed.
 * </p>
 */
public class GetPermissionsTool extends AbstractMcpTool {
	/**
	 * Object types, as in the contract
	 */
	static final List<String> TYPES = List.of("page", "folder", "template", "file", "image", "form", "construct",
			"node", "group", "user", "admin");

	/**
	 * Permission names, as in the contract
	 */
	static final List<String> PERMISSIONS = List.of("view", "read", "create", "edit", "update", "delete", "publish",
			"translatepages", "publishpages", "updateconstructs", "readtemplates", "createtemplates",
			"updatetemplates", "deletetemplates", "linktemplates", "wastebin", "setperm", "userassignment");

	/**
	 * Maximum number of requested permissions
	 */
	static final int MAX_PERMISSIONS = 20;

	static final String ARG_TYPE = "type";

	static final String ARG_ID = "id";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_PERMISSIONS = "permissions";

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param ref reference to the object (type and ID only), if an object was given
	 * @param type object type
	 * @param granted permission name to granted flag
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ObjectRef ref, String type, Map<String, Boolean> granted) {
	}

	/**
	 * Validated arguments of a call
	 * @param type object type
	 * @param id object ID, null for the type-level permissions
	 * @param nodeId node (channel) ID, null if not given
	 * @param permissions requested permission names, null for all
	 */
	record Request(String type, Integer id, Integer nodeId, List<String> permissions) {
		/**
		 * Parse and validate the arguments, without accessing the CMS
		 * @param arguments arguments
		 * @return request
		 * @throws IllegalArgumentException if an argument is invalid
		 */
		static Request of(Map<String, Object> arguments) {
			String type = stringArg(arguments, ARG_TYPE, 1, 32);
			if (type == null) {
				throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_TYPE));
			}
			if (!TYPES.contains(type)) {
				throw new IllegalArgumentException(
						"Argument '%s' must be one of %s, but was '%s'".formatted(ARG_TYPE, TYPES, type));
			}
			Integer id = intArg(arguments, ARG_ID, 1, Integer.MAX_VALUE);
			Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
			List<String> permissions = permissionsArg(arguments);
			if (id != null && permissions != null && permissions.contains("userassignment")) {
				throw new IllegalArgumentException("Permission 'userassignment' cannot be checked on a single object, "
						+ "omit 'id' to get it for the type '%s'".formatted(type));
			}
			return new Request(type, id, nodeId, permissions);
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_TYPE, schema("string", null, "enum", TYPES));
		properties.put(ARG_ID, schema("integer", null, "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", null, "minimum", 1));
		properties.put(ARG_PERMISSIONS, schema("array", "Optional narrowing. Omit to get the full decoded map.",
				"maxItems", MAX_PERMISSIONS, "uniqueItems", true, "items",
				schema("string", null, "enum", PERMISSIONS)));

		return Tool.builder().name("get_permissions").title("Get effective permissions")
				.description("Returns the acting user's effective permissions on one CMS object, or on a whole object "
						+ "type, as named booleans. Call this before proposing any write, and before telling the user "
						+ "an action is possible. Do NOT use it to enumerate what other users may do, that is "
						+ "get_group_permissions, and do NOT treat a granted flag on a type as permission on every "
						+ "instance of that type.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_TYPE))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Request request = Request.of(arguments);
		PermResourceImpl permResource = new PermResourceImpl();

		if (request.id() == null) {
			return new Result(null, request.type(), typePermissions(permResource, request));
		}

		int nodeId = request.nodeId() != null ? request.nodeId() : 0;
		// check view first: the other checks are not meant for objects the caller cannot see (or that
		// do not exist), and must not reveal anything about them
		if (!permResource.getObjectPermission(Permission.view, request.type(), request.id(), nodeId).isGranted()) {
			throw new RestMappedException("No permission to view %s %d".formatted(request.type(), request.id()))
					.setResponseCode(ResponseCode.PERMISSION);
		}

		Map<String, Boolean> granted = new LinkedHashMap<>();
		for (String name : request.permissions() != null ? request.permissions() : PERMISSIONS) {
			if ("userassignment".equals(name)) {
				// not a per-object permission (rejected in Request.of when requested)
				continue;
			}
			PermResponse response = permResource.getObjectPermission(Permission.valueOf(name), request.type(),
					request.id(), nodeId);
			if (response.getResponseInfo() == null || response.getResponseInfo().getResponseCode() == ResponseCode.OK) {
				granted.put(name, response.isGranted());
			} else if (request.permissions() != null) {
				requireOk(response, "Permission '%s' cannot be checked on %s %d".formatted(name, request.type(),
						request.id()));
			}
		}
		// the contract's ObjectRef has no type for the administration
		ObjectRef ref = "admin".equals(request.type()) ? null
				: ObjectRef.of(ObjectRef.Type.fromValue(request.type()), request.id());
		return new Result(ref, request.type(), granted);
	}

	/**
	 * Get the type-level permissions. Only the permissions the CMS defines for the type are in the
	 * map, a requested permission it does not define is rejected.
	 * @param permResource resource
	 * @param request request
	 * @return permission name to granted flag
	 * @throws Exception if the permissions cannot be loaded, e.g. for a type with per-instance
	 *         permissions only
	 */
	private static Map<String, Boolean> typePermissions(PermResourceImpl permResource, Request request)
			throws Exception {
		PermBitsResponse response = permResource.getPermissions(request.type(), true);
		requireOk(response, "The permissions on type '%s' could not be loaded".formatted(request.type()));
		Map<PermType, Boolean> map = response.getPermissionsMap() != null
				&& response.getPermissionsMap().getPermissions() != null
						? response.getPermissionsMap().getPermissions()
						: Map.of();

		Map<String, Boolean> granted = new LinkedHashMap<>();
		for (String name : request.permissions() != null ? request.permissions() : PERMISSIONS) {
			Boolean value = permTypeOf(name).map(map::get).orElse(null);
			if (value != null) {
				granted.put(name, value);
			} else if (request.permissions() != null) {
				throw new IllegalArgumentException("Permission '%s' is not defined for the type '%s'%s".formatted(name,
						request.type(), "admin".equals(request.type()) ? "" : ", pass 'id' to check it on an object"));
			}
		}
		return granted;
	}

	/**
	 * Get the perm type with the given name
	 * @param name permission name
	 * @return perm type, empty if there is none with that name
	 */
	private static Optional<PermType> permTypeOf(String name) {
		try {
			return Optional.of(PermType.valueOf(name));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/**
	 * Get the optional list of requested permissions
	 * @param arguments arguments
	 * @return permission names, null if not given
	 * @throws IllegalArgumentException if the argument is not a list of distinct known permission
	 *         names, or too long
	 */
	static List<String> permissionsArg(Map<String, Object> arguments) {
		Object value = arguments.get(ARG_PERMISSIONS);
		if (value == null) {
			return null;
		}
		if (!(value instanceof List<?> list) || list.size() > MAX_PERMISSIONS) {
			throw new IllegalArgumentException("Argument '%s' must be a list of at most %d permission names"
					.formatted(ARG_PERMISSIONS, MAX_PERMISSIONS));
		}
		List<String> permissions = new ArrayList<>();
		for (Object item : list) {
			if (!(item instanceof String name) || !PERMISSIONS.contains(name)) {
				throw new IllegalArgumentException("Argument '%s' contains '%s', expected one of %s"
						.formatted(ARG_PERMISSIONS, item, PERMISSIONS));
			}
			if (permissions.contains(name)) {
				throw new IllegalArgumentException(
						"Argument '%s' contains '%s' more than once".formatted(ARG_PERMISSIONS, name));
			}
			permissions.add(name);
		}
		return permissions;
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema("Reference to the object, if one was given."));
		properties.put("type", schema("string", null));
		properties.put("granted", schema("object", "Permission name to granted flag.", "additionalProperties",
				schema("boolean", null)));
		return schema("object", null, "properties", properties, "required", List.of("granted"));
	}
}
