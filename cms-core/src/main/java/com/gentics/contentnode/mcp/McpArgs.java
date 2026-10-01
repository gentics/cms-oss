package com.gentics.contentnode.mcp;

import java.util.Map;

/**
 * Parsing and validation of the raw arguments of MCP tool calls, shared by the tools
 * ({@link AbstractMcpTool} delegates to them) and by argument records outside of tool classes
 * (e.g. {@code com.gentics.contentnode.mcp.util.ListArgs}).
 *
 * <p>
 * The SDK already validates the arguments against the tool's input schema before the tool is
 * invoked. These methods repeat that, so a tool is also safe when {@link AbstractMcpTool#call} is
 * invoked directly: they reject invalid values (wrong JSON type, out of bounds) instead of
 * adjusting them, and throw {@link IllegalArgumentException} with a message naming the argument.
 * </p>
 */
public final class McpArgs {
	private McpArgs() {
	}

	/**
	 * Get an optional string argument
	 * @param arguments arguments
	 * @param name argument name
	 * @param minLength minimum length
	 * @param maxLength maximum length
	 * @return value, or null if not supplied
	 * @throws IllegalArgumentException if the value is not a string or its length is out of bounds
	 */
	public static String stringArg(Map<String, Object> arguments, String name, int minLength, int maxLength) {
		if (!arguments.containsKey(name)) {
			return null;
		}
		if (!(arguments.get(name) instanceof String value)) {
			throw new IllegalArgumentException("Argument '%s' must be a string".formatted(name));
		}
		if (value.length() < minLength) {
			throw new IllegalArgumentException(
					"Argument '%s' must be at least %d characters long".formatted(name, minLength));
		}
		if (value.length() > maxLength) {
			throw new IllegalArgumentException(
					"Argument '%s' must not be longer than %d characters".formatted(name, maxLength));
		}
		return value;
	}

	/**
	 * Get an optional integer argument. Values out of {@code [min, max]} are rejected, not clamped.
	 * @param arguments arguments
	 * @param name argument name
	 * @param min minimum value
	 * @param max maximum value
	 * @return value, or null if not supplied
	 * @throws IllegalArgumentException if the value is not an integral number or out of bounds
	 */
	public static Integer intArg(Map<String, Object> arguments, String name, int min, int max) {
		if (!arguments.containsKey(name)) {
			return null;
		}
		Object raw = arguments.get(name);
		if (!(raw instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())) {
			throw new IllegalArgumentException("Argument '%s' must be an integer".formatted(name));
		}
		long value = number.longValue();
		if (value < min || value > max) {
			throw new IllegalArgumentException(
					"Argument '%s' must be between %d and %d".formatted(name, min, max));
		}
		return (int) value;
	}

	/**
	 * Get an optional integer argument, like {@link #intArg(Map, String, int, int)}, with a default
	 * @param arguments arguments
	 * @param name argument name
	 * @param min minimum value
	 * @param max maximum value
	 * @param defaultValue value if not supplied
	 * @return value
	 * @throws IllegalArgumentException if the value is not an integral number or out of bounds
	 */
	public static int intArg(Map<String, Object> arguments, String name, int min, int max, int defaultValue) {
		Integer value = intArg(arguments, name, min, max);
		return value != null ? value : defaultValue;
	}

	/**
	 * Get an optional boolean argument
	 * @param arguments arguments
	 * @param name argument name
	 * @param defaultValue value if not supplied
	 * @return value
	 * @throws IllegalArgumentException if the value is not a boolean
	 */
	public static boolean booleanArg(Map<String, Object> arguments, String name, boolean defaultValue) {
		if (!arguments.containsKey(name)) {
			return defaultValue;
		}
		if (!(arguments.get(name) instanceof Boolean value)) {
			throw new IllegalArgumentException("Argument '%s' must be a boolean".formatted(name));
		}
		return value;
	}

	/**
	 * Normalize an optional string (typically a filter): null or blank means "not set"
	 * @param value value, may be null
	 * @return value, or null if null or blank
	 */
	public static String nullIfBlank(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
