package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.User;

/**
 * The acting user, as returned by {@code whoami}. Never contains the password. Fields that are not
 * set are omitted on serialization.
 * @param id user ID
 * @param login login name
 * @param firstName first name
 * @param lastName last name
 * @param email email address
 */
@JsonInclude(Include.NON_NULL)
public record WhoamiUser(Integer id, String login, String firstName, String lastName, String email) {
	/**
	 * Map the REST user
	 * @param user REST user
	 * @return user
	 */
	public static WhoamiUser of(User user) {
		return new WhoamiUser(user.getId(), user.getLogin(), user.getFirstName(), user.getLastName(),
				user.getEmail());
	}

	/**
	 * Build the output schema of a user. Must be kept in sync with the components.
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("id", schema("integer", null));
		properties.put("login", schema("string", null));
		properties.put("firstName", schema("string", null));
		properties.put("lastName", schema("string", null));
		properties.put("email", schema("string", null));
		return schema("object", "The acting user.", "properties", properties);
	}
}
