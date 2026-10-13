package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.User;

/**
 * Reference to a user (e.g. creator, editor, publisher or lock holder of an object), as returned
 * by MCP tools. Fields that are not set are omitted on serialization.
 * @param id user ID
 * @param login login name
 */
@JsonInclude(Include.NON_NULL)
public record UserRef(Integer id, String login) {
	/**
	 * Map the REST user
	 * @param user REST user, may be null
	 * @return user ref or null
	 */
	public static UserRef of(User user) {
		return user != null ? new UserRef(user.getId(), user.getLogin()) : null;
	}

	/**
	 * Build the output schema of a user ref. Must be kept in sync with the components.
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("id", schema("integer", null));
		properties.put("login", schema("string", null));
		return schema("object", null, "properties", properties);
	}
}
