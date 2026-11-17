package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpArgs.nullIfBlank;
import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.UserItem;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.UserGroups;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Creates a user in a group ({@link GroupResourceImpl#createUser}, which needs the permission to create users and to
 * assign users to the group, i.e. a group of the caller with that permission above it). A login already in use is
 * rejected by the CMS. The user is created without a password and sets it on a one-time CMS link (GPU-2776). The
 * result carries only the link's expiry: the administrator fetches the link with {@code POST /user/{id}/passwordLink},
 * so it never reaches the model or the stored session.
 */
public class CreateUserTool extends AbstractMcpTool {
	static final String ARG_GROUP_ID = "groupId";

	static final String ARG_LOGIN = "login";

	static final String ARG_EMAIL = "email";

	static final String ARG_FIRST_NAME = "firstName";

	static final String ARG_LAST_NAME = "lastName";

	static final String ARG_DESCRIPTION = "description";

	/**
	 * Loose email check: one @, no whitespace
	 */
	static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+");

	/**
	 * Result of the tool
	 * @param user the created user, with the groups the caller can view
	 * @param created always true
	 * @param passwordLink expiry of the link on which the user sets their password
	 */
	public record Result(UserItem user, boolean created, PasswordLink passwordLink) {
	}

	/**
	 * Expiry of a created user's password link, never the link itself
	 * @param expiresAt ISO 8601 date-time
	 */
	public record PasswordLink(String expiresAt) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_GROUP_ID, schema("integer", "ID of the group to create the user in.", "minimum", 1));
		properties.put(ARG_LOGIN, schema("string", "Login name, must not be in use.", "minLength", 2, "maxLength",
				100));
		properties.put(ARG_EMAIL, schema("string", null, "format", "email", "maxLength", 255));
		properties.put(ARG_FIRST_NAME, schema("string", null, "minLength", 1, "maxLength", 100));
		properties.put(ARG_LAST_NAME, schema("string", null, "minLength", 1, "maxLength", 100));
		properties.put(ARG_DESCRIPTION, schema("string", null, "maxLength", 1000));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("user", UserItem.jsonSchema("The created user."));
		output.put("created", schema("boolean", null));
		output.put("passwordLink", schema("object", "The CMS created a one-time link on which the user sets their "
				+ "password. Only its expiry is returned, never the link or its token.", "properties",
				Map.of("expiresAt", schema("string", null, "format", "date-time")), "required", List.of("expiresAt")));

		return Tool.builder().name("create_user").title("Create user in group")
				.description("Creates a CMS user and places them in a group in the same call. groupId is REQUIRED: "
						+ "the CMS has no standalone user-create endpoint, so there is no way to create a user without "
						+ "a group, and you can only create users in subgroups of your own groups. Check with "
						+ "list_users that the login is free, and with get_group_permissions that the group carries "
						+ "the intended role. Show the user what the group grants and get their confirmation before "
						+ "calling this. There is no password argument: the CMS creates a one-time link on which the "
						+ "new user sets their password, and the result carries only its expiry.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_GROUP_ID, ARG_LOGIN, ARG_FIRST_NAME, ARG_LAST_NAME))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required",
						List.of("user", "created", "passwordLink")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int groupId = Args.id(arguments, ARG_GROUP_ID);
		User user = new User();
		user.setLogin(required(arguments, ARG_LOGIN, 2, 100));
		user.setFirstName(required(arguments, ARG_FIRST_NAME, 1, 100));
		user.setLastName(required(arguments, ARG_LAST_NAME, 1, 100));
		user.setEmail(stringArg(arguments, ARG_EMAIL, 0, 255));
		if (user.getEmail() != null && !EMAIL.matcher(user.getEmail()).matches()) {
			throw new IllegalArgumentException("Argument '%s' must be an email address".formatted(ARG_EMAIL));
		}
		user.setDescription(stringArg(arguments, ARG_DESCRIPTION, 0, 1000));
		Writes.logIdempotencyKey("create_user", groupId, arguments);

		GroupResource groupResource = RestPermissions.guard(GroupResource.class, new GroupResourceImpl());
		UserLoadResponse response = groupResource.createUser(Integer.toString(groupId), user, true);
		requireOk(response, "The user could not be created");
		if (response.getExpiresAt() == null) {
			throw new IllegalStateException("The CMS created the user without a password link");
		}

		User created = response.getUser();
		UserResource userResource = RestPermissions.guard(UserResource.class, new UserResourceImpl());
		PasswordLink link = new PasswordLink(Instant.ofEpochSecond(response.getExpiresAt()).toString());
		return new Result(UserItem.of(created, UserGroups.refs(userResource, created.getId())), true, link);
	}

	/**
	 * Get a required, non-blank string argument
	 * @param arguments arguments
	 * @param name argument name
	 * @param minLength minimum length
	 * @param maxLength maximum length
	 * @return value
	 * @throws IllegalArgumentException if the argument is missing, blank or its length is out of bounds
	 */
	static String required(Map<String, Object> arguments, String name, int minLength, int maxLength) {
		String value = nullIfBlank(stringArg(arguments, name, minLength, maxLength));
		if (value == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(name));
		}
		return value;
	}
}
