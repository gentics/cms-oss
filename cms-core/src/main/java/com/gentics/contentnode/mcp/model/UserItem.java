package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.User;

/**
 * A user, as returned by the admin tools. Never contains the password. Fields that are not set are omitted on
 * serialization.
 * @param ref user ref
 * @param login login name
 * @param firstName first name
 * @param lastName last name
 * @param email email address
 * @param description description
 * @param groups the user's groups, unless they were not requested
 */
@JsonInclude(Include.NON_NULL)
public record UserItem(ObjectRef ref, String login, String firstName, String lastName, String email,
		String description, List<ObjectRef> groups) {
	/**
	 * Map the REST user
	 * @param user REST user
	 * @param groups the user's groups, may be null
	 * @return user item
	 */
	public static UserItem of(User user, List<ObjectRef> groups) {
		return new UserItem(ObjectRef.forUser(user), user.getLogin(), user.getFirstName(), user.getLastName(),
				user.getEmail(), user.getDescription(), groups);
	}

	/**
	 * Build the output schema. Must be kept in sync with the components.
	 * @param description description, may be null
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema(String description) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema(null));
		properties.put("login", schema("string", null));
		properties.put("firstName", schema("string", null));
		properties.put("lastName", schema("string", null));
		properties.put("email", schema("string", null));
		properties.put("description", schema("string", null));
		properties.put("groups", schema("array", "The user's groups.", "items", ObjectRef.jsonSchema(null)));
		return schema("object", description, "properties", properties, "required", List.of("ref"));
	}
}
