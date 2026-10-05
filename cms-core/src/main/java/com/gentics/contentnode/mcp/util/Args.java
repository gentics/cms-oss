package com.gentics.contentnode.mcp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.mcp.McpArgs;

/**
 * Tool arguments not covered by {@link McpArgs}
 */
public final class Args {
	private Args() {
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

	/**
	 * Get an optional list of distinct values out of a fixed set
	 * @param arguments arguments
	 * @param name argument name
	 * @param allowed allowed values
	 * @param defaultValue value if not supplied
	 * @return value
	 * @throws IllegalArgumentException if the argument is not a list of distinct allowed values
	 */
	public static List<String> enumList(Map<String, Object> arguments, String name, List<String> allowed,
			List<String> defaultValue) {
		Object value = arguments.get(name);
		if (value == null) {
			return defaultValue;
		}
		if (!(value instanceof List<?> list)) {
			throw new IllegalArgumentException("Argument '%s' must be a list of %s".formatted(name, allowed));
		}
		List<String> values = new ArrayList<>();
		for (Object item : list) {
			if (!(item instanceof String string) || !allowed.contains(string)) {
				throw new IllegalArgumentException(
						"Argument '%s' contains '%s', expected one of %s".formatted(name, item, allowed));
			}
			if (values.contains(string)) {
				throw new IllegalArgumentException("Argument '%s' contains '%s' more than once".formatted(name, string));
			}
			values.add(string);
		}
		return values;
	}

	/**
	 * Get an optional list of positive integers
	 * @param arguments arguments
	 * @param name argument name
	 * @param maxItems maximum number of items
	 * @return value, null if not supplied
	 * @throws IllegalArgumentException if the argument is not a list of at most maxItems positive integers
	 */
	public static List<Integer> intList(Map<String, Object> arguments, String name, int maxItems) {
		Object value = arguments.get(name);
		if (value == null) {
			return null;
		}
		if (!(value instanceof List<?> list) || list.size() > maxItems) {
			throw new IllegalArgumentException(
					"Argument '%s' must be a list of at most %d positive integers".formatted(name, maxItems));
		}
		List<Integer> values = new ArrayList<>();
		for (Object item : list) {
			if (!(item instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())
					|| number.longValue() < 1 || number.longValue() > Integer.MAX_VALUE) {
				throw new IllegalArgumentException(
						"Argument '%s' contains '%s', expected a positive integer".formatted(name, item));
			}
			values.add(number.intValue());
		}
		return values;
	}

	/**
	 * Get an optional list of strings
	 * @param arguments arguments
	 * @param name argument name
	 * @param maxItems maximum number of items
	 * @param maxLength maximum length of an item
	 * @return value, null if not supplied
	 * @throws IllegalArgumentException if the argument is not a list of at most maxItems strings of at most maxLength
	 */
	public static List<String> stringList(Map<String, Object> arguments, String name, int maxItems, int maxLength) {
		Object value = arguments.get(name);
		if (value == null) {
			return null;
		}
		if (!(value instanceof List<?> list) || list.size() > maxItems) {
			throw new IllegalArgumentException(
					"Argument '%s' must be a list of at most %d strings".formatted(name, maxItems));
		}
		List<String> values = new ArrayList<>();
		for (Object item : list) {
			if (!(item instanceof String string) || string.length() > maxLength) {
				throw new IllegalArgumentException("Argument '%s' must contain strings of at most %d characters"
						.formatted(name, maxLength));
			}
			values.add(string);
		}
		return values;
	}
}
