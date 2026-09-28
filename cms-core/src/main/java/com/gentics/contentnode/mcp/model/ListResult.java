package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.mcp.util.Slice;

/**
 * Result of a list tool: one page of items, with the paging information. Fields that are not set
 * (e.g. {@code nextFrom} on the last page) are omitted on serialization.
 * @param <T> item type
 * @param total total number of matching items
 * @param totalIsExact whether total is an exact count
 * @param nextFrom offset of the next page, if truncated
 * @param truncated whether there are more items after this page
 * @param items items in this page
 */
@JsonInclude(Include.NON_NULL)
public record ListResult<T>(Integer total, Boolean totalIsExact, Integer nextFrom, Boolean truncated, List<T> items) {
	/**
	 * Create the result for a slice of a list whose total was counted exactly
	 * @param <T> item type
	 * @param slice slice, see {@link Slice#of}
	 * @param items items in the slice
	 * @return result
	 */
	public static <T> ListResult<T> of(Slice slice, List<T> items) {
		return new ListResult<>(slice.total(), true, slice.nextFrom(), slice.truncated(), items);
	}

	/**
	 * Build the output schema of a list result. Must be kept in sync with the components. Only
	 * {@code items} is required.
	 * @param itemSchema schema of a single item
	 * @param plural name of several items, e.g. {@code "nodes"}
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema(Map<String, Object> itemSchema, String plural) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("total", schema("integer", "Total number of matching %s.".formatted(plural)));
		properties.put("totalIsExact", schema("boolean", null));
		properties.put("nextFrom", schema("integer", "Offset of the next page, if 'truncated'."));
		properties.put("truncated", schema("boolean", "Whether more %s exist after this page.".formatted(plural)));
		properties.put("items", schema("array", null, "items", itemSchema));
		return schema("object", null, "properties", properties, "required", List.of("items"));
	}
}
