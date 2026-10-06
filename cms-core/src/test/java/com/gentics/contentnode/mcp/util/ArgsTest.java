package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for the construct argument helpers of {@link Args}
 */
public class ArgsTest {
	private static final List<String> UI_LANGUAGES = List.of("de", "en");

	@Test
	public void testDistinctIntList() {
		assertThat(Args.distinctIntList(Map.of("ids", List.of(3, 4)), "ids", 5)).containsExactly(3, 4);
		assertThat(Args.distinctIntList(Map.of(), "ids", 5)).isNull();
		assertRejected(() -> Args.distinctIntList(Map.of("ids", List.of(3, 3)), "ids", 5), "more than once");
		assertRejected(() -> Args.distinctIntList(Map.of("ids", List.of(1, 2, 3)), "ids", 2), "at most 2");
		assertRejected(() -> Args.distinctIntList(Map.of("ids", List.of(0)), "ids", 2), "positive integer");
	}

	@Test
	public void testExactlyOne() {
		assertThat(Args.exactlyOne(Map.of("id", 1), "id", "keyword")).isEqualTo("id");
		assertThat(Args.exactlyOne(Map.of("keyword", "hero"), "id", "keyword")).isEqualTo("keyword");
		assertRejected(() -> Args.exactlyOne(Map.of(), "id", "keyword"), "exactly one of 'id' and 'keyword'");
		assertRejected(() -> Args.exactlyOne(Map.of("id", 1, "keyword", "hero"), "id", "keyword"), "exactly one");
	}

	@Test
	public void testI18nMap() {
		assertThat(Args.i18nMap(Map.of("name", Map.of("de", "Box", "fr", "Boîte")), "name", 10, UI_LANGUAGES))
				.containsEntry("de", "Box").containsEntry("fr", "Boîte");
		assertThat(Args.i18nMap(Map.of(), "name", 10, UI_LANGUAGES)).isNull();
		assertRejected(() -> Args.i18nMap(Map.of("name", Map.of("fr", "Boîte")), "name", 10, UI_LANGUAGES),
				"one of the UI languages [de, en]");
		assertRejected(() -> Args.i18nMap(Map.of("name", Map.of("de", "x".repeat(11))), "name", 10, UI_LANGUAGES),
				"at most 10 characters");
		assertRejected(() -> Args.i18nMap(Map.of("name", Map.of("de", 1)), "name", 10, UI_LANGUAGES), "texts");
		assertRejected(() -> Args.i18nMap(Map.of("name", "Box"), "name", 10, UI_LANGUAGES), "must map");
	}

	private static void assertRejected(Runnable call, String message) {
		assertThatThrownBy(call::run).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
	}
}
