package com.gentics.contentnode.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link McpSchemas}
 */
public class McpSchemasTest {
	@Test
	public void testSchema() {
		assertThat(McpSchemas.schema("integer", "Some number.", "minimum", 1, "maximum", 10))
				.containsExactly(Map.entry("type", "integer"), Map.entry("description", "Some number."),
						Map.entry("minimum", 1), Map.entry("maximum", 10));
	}

	@Test
	public void testSchemaWithoutDescription() {
		assertThat(McpSchemas.schema("string", null)).containsExactly(Map.entry("type", "string"));
	}

	@Test
	public void testSchemaIgnoresKeyWithoutValue() {
		assertThat(McpSchemas.schema("string", null, "maxLength", 5, "minLength"))
				.containsExactly(Map.entry("type", "string"), Map.entry("maxLength", 5));
	}
}
