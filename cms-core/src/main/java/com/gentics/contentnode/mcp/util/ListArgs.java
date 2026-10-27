package com.gentics.contentnode.mcp.util;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gentics.contentnode.mcp.McpArgs;

/**
 * The common arguments of list tools: an optional filter query {@code q}, and offset-based
 * paging with {@code size} and {@code from} (see {@link Slice}).
 * @param query filter query, null for no filter (also if blank)
 * @param size page size
 * @param from offset of the first item
 */
public record ListArgs(String query, int size, int from) {
	/**
	 * Name of the query argument
	 */
	public static final String ARG_QUERY = "q";

	/**
	 * Name of the page size argument
	 */
	public static final String ARG_SIZE = "size";

	/**
	 * Name of the offset argument
	 */
	public static final String ARG_FROM = "from";

	/**
	 * Minimum of {@link #ARG_SIZE}
	 */
	public static final int MIN_SIZE = 1;

	/**
	 * Minimum and default of {@link #ARG_FROM}
	 */
	public static final int MIN_FROM = 0;

	/**
	 * Limits of the arguments, which may differ per tool
	 * @param maxQueryLength maximum length of {@link #ARG_QUERY}
	 * @param defaultSize default of {@link #ARG_SIZE}
	 * @param maxSize maximum of {@link #ARG_SIZE}
	 * @param maxFrom maximum of {@link #ARG_FROM}
	 */
	public record Limits(int maxQueryLength, int defaultSize, int maxSize, int maxFrom) {
		/**
		 * Check the limits
		 * @throws IllegalArgumentException if the limits are inconsistent
		 */
		public Limits {
			if (maxQueryLength < 0 || maxSize < MIN_SIZE || defaultSize < MIN_SIZE || defaultSize > maxSize
					|| maxFrom < MIN_FROM) {
				throw new IllegalArgumentException("Inconsistent list argument limits");
			}
		}
	}

	/**
	 * Validate the raw arguments. Values out of the limits are rejected, not clamped.
	 * @param arguments raw arguments
	 * @param limits limits
	 * @return validated arguments
	 * @throws IllegalArgumentException for an invalid argument
	 */
	public static ListArgs of(Map<String, Object> arguments, Limits limits) {
		return new ListArgs(McpArgs.nullIfBlank(McpArgs.stringArg(arguments, ARG_QUERY, 0, limits.maxQueryLength())),
				McpArgs.intArg(arguments, ARG_SIZE, MIN_SIZE, limits.maxSize(), limits.defaultSize()),
				McpArgs.intArg(arguments, ARG_FROM, MIN_FROM, limits.maxFrom(), MIN_FROM));
	}

	/**
	 * Build the input schema properties of the arguments, in the order {@code q}, {@code size},
	 * {@code from}
	 * @param queryDescription description of {@link #ARG_QUERY}, which is specific to the tool
	 * @param singular name of a single item, e.g. {@code "node"}
	 * @param plural name of several items, e.g. {@code "nodes"}
	 * @param limits limits
	 * @return properties (mutable, so a tool can add its own)
	 */
	public static Map<String, Object> schemaProperties(String queryDescription, String singular, String plural,
			Limits limits) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_QUERY, schema("string", queryDescription, "maxLength", limits.maxQueryLength()));
		properties.put(ARG_SIZE, schema("integer", "Maximum number of %s to return.".formatted(plural), "minimum",
				MIN_SIZE, "maximum", limits.maxSize(), "default", limits.defaultSize()));
		properties.put(ARG_FROM, schema("integer",
				"Offset of the first %s to return. Pass the previous result's 'nextFrom' to get the next page."
						.formatted(singular),
				"minimum", MIN_FROM, "maximum", limits.maxFrom(), "default", MIN_FROM));
		return properties;
	}

	/**
	 * Compute the requested slice of a list of {@code total} items
	 * @param total total number of items
	 * @return slice
	 */
	public Slice slice(int total) {
		return Slice.of(from, size, total);
	}
}
