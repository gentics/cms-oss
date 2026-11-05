package com.gentics.contentnode.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

/**
 * Unit tests for {@link McpArgs}
 */
public class McpArgsTest {
	@Test
	public void testStringArg() {
		assertThat(McpArgs.stringArg(Map.of(), "name", 1, 5)).isNull();
		assertThat(McpArgs.stringArg(Map.of("name", "abc"), "name", 1, 5)).isEqualTo("abc");
		assertThat(McpArgs.stringArg(Map.of("name", ""), "name", 0, 5)).isEmpty();

		assertInvalid(() -> McpArgs.stringArg(Map.of("name", ""), "name", 1, 5), "name");
		assertInvalid(() -> McpArgs.stringArg(Map.of("name", "abcdef"), "name", 1, 5), "name");
		assertInvalid(() -> McpArgs.stringArg(Map.of("name", 1), "name", 1, 5), "name");
		assertInvalid(() -> McpArgs.stringArg(nullValue("name"), "name", 1, 5), "name");
	}

	@Test
	public void testIntArg() {
		assertThat(McpArgs.intArg(Map.of(), "size", 1, 200)).isNull();
		assertThat(McpArgs.intArg(Map.of("size", 10), "size", 1, 200)).isEqualTo(10);
		assertThat(McpArgs.intArg(Map.of("size", 10L), "size", 1, 200)).isEqualTo(10);
		assertThat(McpArgs.intArg(Map.of("size", 10.0), "size", 1, 200)).isEqualTo(10);
		assertThat(McpArgs.intArg(Map.of("size", 1), "size", 1, 200)).isEqualTo(1);
		assertThat(McpArgs.intArg(Map.of("size", 200), "size", 1, 200)).isEqualTo(200);

		// out of range values are rejected, not clamped
		assertInvalid(() -> McpArgs.intArg(Map.of("size", 0), "size", 1, 200), "size");
		assertInvalid(() -> McpArgs.intArg(Map.of("size", 1000), "size", 1, 200), "size");
		assertInvalid(() -> McpArgs.intArg(Map.of("size", Long.MAX_VALUE), "size", 1, Integer.MAX_VALUE),
				"size");
		assertInvalid(() -> McpArgs.intArg(Map.of("size", 10.5), "size", 1, 200), "size");
		// numeric strings are rejected
		assertInvalid(() -> McpArgs.intArg(Map.of("size", "10"), "size", 1, 200), "size");
		assertInvalid(() -> McpArgs.intArg(nullValue("size"), "size", 1, 200), "size");
	}

	@Test
	public void testIntArgWithDefault() {
		assertThat(McpArgs.intArg(Map.of(), "size", 1, 200, 25)).isEqualTo(25);
		assertThat(McpArgs.intArg(Map.of("size", 10), "size", 1, 200, 25)).isEqualTo(10);

		assertInvalid(() -> McpArgs.intArg(Map.of("size", 0), "size", 1, 200, 25), "size");
		assertInvalid(() -> McpArgs.intArg(Map.of("size", "10"), "size", 1, 200, 25), "size");
	}

	@Test
	public void testBooleanArg() {
		assertThat(McpArgs.booleanArg(Map.of(), "flag", true)).isTrue();
		assertThat(McpArgs.booleanArg(Map.of(), "flag", false)).isFalse();
		assertThat(McpArgs.booleanArg(Map.of("flag", false), "flag", true)).isFalse();

		assertInvalid(() -> McpArgs.booleanArg(Map.of("flag", "true"), "flag", false), "flag");
		assertInvalid(() -> McpArgs.booleanArg(nullValue("flag"), "flag", false), "flag");
	}

	@Test
	public void testNullIfBlank() {
		assertThat(McpArgs.nullIfBlank(null)).isNull();
		assertThat(McpArgs.nullIfBlank("")).isNull();
		assertThat(McpArgs.nullIfBlank(" \t")).isNull();
		assertThat(McpArgs.nullIfBlank(" a ")).isEqualTo(" a ");
	}

	private static void assertInvalid(ThrowingCallable call, String argument) {
		assertThatThrownBy(call).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("'" + argument + "'");
	}

	/**
	 * Get arguments containing the given argument with value null ({@link Map#of} does not allow that)
	 * @param name argument name
	 * @return arguments
	 */
	private static Map<String, Object> nullValue(String name) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put(name, null);
		return arguments;
	}
}
