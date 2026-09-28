package com.gentics.contentnode.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builders for the hand-built JSON schemas of the MCP tools (input and output schemas), shared by
 * the tools ({@link AbstractMcpTool} delegates to them) and by the records in
 * {@code com.gentics.contentnode.mcp.model}, which provide the schema of their own output shape.
 */
public final class McpSchemas {
	private McpSchemas() {
	}

	/**
	 * Build a property schema
	 * @param type JSON type
	 * @param description description, may be null
	 * @param keyValues additional constraint keywords and their values, alternating
	 * @return schema
	 */
	public static Map<String, Object> schema(String type, String description, Object... keyValues) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", type);
		if (description != null) {
			schema.put("description", description);
		}
		for (int i = 0; i + 1 < keyValues.length; i += 2) {
			schema.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
		}
		return schema;
	}
}
