package com.gentics.contentnode.mcp.util;

import java.util.Map;

import com.gentics.contentnode.mcp.McpArgs;

/**
 * Required tool arguments
 */
public final class RequiredArgs {
	private RequiredArgs() {
	}

	/**
	 * Get a required object ID argument (a positive integer)
	 * @param arguments arguments
	 * @param name argument name
	 * @return ID
	 * @throws IllegalArgumentException if the argument is missing or not a positive integer
	 */
	public static int id(Map<String, Object> arguments, String name) {
		Integer id = McpArgs.intArg(arguments, name, 1, Integer.MAX_VALUE);
		if (id == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(name));
		}
		return id;
	}
}
