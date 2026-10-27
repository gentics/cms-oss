package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link ListArgs}
 */
public class ListArgsTest {
	private static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	@Test
	public void testDefaults() {
		assertThat(ListArgs.of(Map.of(), LIMITS)).isEqualTo(new ListArgs(null, 25, 0));
	}

	@Test
	public void testValues() {
		assertThat(ListArgs.of(Map.of("q", "news", "size", 10, "from", 20), LIMITS))
				.isEqualTo(new ListArgs("news", 10, 20));
		assertThat(ListArgs.of(Map.of("size", 10L, "from", 20.0), LIMITS)).isEqualTo(new ListArgs(null, 10, 20));
	}

	@Test
	public void testBoundaries() {
		assertThat(ListArgs.of(Map.of("q", "x".repeat(200), "size", 1, "from", 0), LIMITS).size()).isEqualTo(1);
		assertThat(ListArgs.of(Map.of("size", 200, "from", 10000), LIMITS)).isEqualTo(new ListArgs(null, 200, 10000));
	}

	@Test
	public void testBlankQueryMeansNoFilter() {
		assertThat(ListArgs.of(Map.of("q", ""), LIMITS).query()).isNull();
		assertThat(ListArgs.of(Map.of("q", "  "), LIMITS).query()).isNull();
	}

	/**
	 * Values are rejected, not clamped, and must have the right JSON type
	 */
	@Test
	public void testInvalidValuesAreRejected() {
		assertInvalid(Map.of("size", 0), "size");
		assertInvalid(Map.of("size", 201), "size");
		assertInvalid(Map.of("size", "10"), "size");
		assertInvalid(Map.of("size", 10.5), "size");
		assertInvalid(Map.of("from", -1), "from");
		assertInvalid(Map.of("from", 10001), "from");
		assertInvalid(Map.of("q", "x".repeat(201)), "q");
		assertInvalid(Map.of("q", 5), "q");

		Map<String, Object> nullQuery = new HashMap<>();
		nullQuery.put("q", null);
		assertInvalid(nullQuery, "q");
	}

	@Test
	public void testSlice() {
		assertThat(new ListArgs(null, 2, 3).slice(10)).isEqualTo(Slice.of(3, 2, 10));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSchemaProperties() {
		Map<String, Object> properties = ListArgs.schemaProperties("Filter.", "node", "nodes", LIMITS);

		assertThat(properties).containsOnlyKeys("q", "size", "from");
		assertThat((Map<String, Object>) properties.get("q")).containsEntry("type", "string")
				.containsEntry("description", "Filter.").containsEntry("maxLength", 200);
		assertThat((Map<String, Object>) properties.get("size")).containsEntry("type", "integer")
				.containsEntry("description", "Maximum number of nodes to return.").containsEntry("minimum", 1)
				.containsEntry("maximum", 200).containsEntry("default", 25);
		assertThat((Map<String, Object>) properties.get("from")).containsEntry("type", "integer")
				.containsEntry("minimum", 0).containsEntry("maximum", 10000).containsEntry("default", 0);
		assertThat((String) ((Map<String, Object>) properties.get("from")).get("description"))
				.startsWith("Offset of the first node to return.");
	}

	@Test
	public void testInconsistentLimitsAreRejected() {
		assertThatThrownBy(() -> new ListArgs.Limits(-1, 25, 200, 10000)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new ListArgs.Limits(200, 0, 200, 10000)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new ListArgs.Limits(200, 25, 10, 10000)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new ListArgs.Limits(200, 25, 200, -1)).isInstanceOf(IllegalArgumentException.class);
	}

	private static void assertInvalid(Map<String, Object> arguments, String argument) {
		assertThatThrownBy(() -> ListArgs.of(arguments, LIMITS)).as("arguments %s", arguments)
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'" + argument + "'");
	}
}
