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
import com.gentics.contentnode.mcp.model.WhoamiUser;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Returns the CMS user the call's credential resolves to, with their groups.
 *
 * <p>
 * Delegates to {@link UserResourceImpl#getMe}, which reads the transaction's own user, i.e. the
 * user {@code AbstractMcpTool} bound from the credential. No permission check beyond the
 * authentication is needed: every user may see themselves.
 * </p>
 */
public class WhoamiTool extends AbstractMcpTool {
	/**
	 * Name of the argument for including the groups
	 */
	static final String ARG_INCLUDE_GROUPS = "includeGroups";

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param user the acting user
	 * @param groups the user's groups, unless they were not requested
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(WhoamiUser user, List<ObjectRef> groups) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_INCLUDE_GROUPS, schema("boolean", "Whether to include the user's groups.", "default", true));

		return Tool.builder().name("whoami").title("Who am I")
				.description("Returns the CMS user this session acts as, with their groups. Call this once at the "
						+ "start of any task so you can address the user correctly and know which groups bound your "
						+ "permissions. Do NOT use it to decide whether an action is allowed: group membership does "
						+ "not tell you object permissions, use get_permissions for that.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		boolean includeGroups = booleanArg(arguments, ARG_INCLUDE_GROUPS, true);

		UserLoadResponse response = new UserResourceImpl().getMe(includeGroups);
		requireOk(response, "The current user could not be loaded");

		List<ObjectRef> groups = null;
		if (includeGroups) {
			List<Group> restGroups = response.getUser().getGroups();
			groups = restGroups == null ? List.of() : restGroups.stream().map(WhoamiTool::groupRef).toList();
		}
		return new Result(WhoamiUser.of(response.getUser()), groups);
	}

	/**
	 * Create the ref for a group
	 * @param group REST group
	 * @return ref
	 */
	static ObjectRef groupRef(Group group) {
		return new ObjectRef(ObjectRef.Type.GROUP, group.getId(), null, null, group.getName(), null, null, null,
				null);
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("user", WhoamiUser.jsonSchema());
		properties.put("groups", schema("array", "The user's groups.", "items", ObjectRef.jsonSchema(null)));
		return schema("object", null, "properties", properties, "required", List.of("user"));
	}
}
