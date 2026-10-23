package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.tools.ListPartTypesTool.PartTypeInfo;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.rest.model.PartType;

/**
 * Unit tests for {@link ListPartTypesTool}. The installed part types are covered by the live check.
 */
public class ListPartTypesToolTest {
	private final ListPartTypesTool tool = new ListPartTypesTool();

	private static final List<PartType> TYPES = List.of(type(1, "Text"), type(13, "Overview"), type(29,
			"Select (single)"), type(43, "Handlebars"));

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_part_types", "List part types");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("q", "text", "allowedOnly", true, "size", 10));
		assertRejected(tool, Map.of("allowedOnly", "yes"));
		assertRejected(tool, Map.of("keyword", "text"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testFlags() {
		ListResult<PartTypeInfo> result = ListPartTypesTool.result(TYPES, false, args(Map.of()));

		assertThat(result.items()).extracting(PartTypeInfo::id).containsExactly(1, 13, 29, 43);
		assertThat(result.items()).extracting(PartTypeInfo::allowedForGeneratedConstructs)
				.containsExactly(true, false, true, false);
		assertThat(result.items()).extracting(PartTypeInfo::requiresDatasource)
				.containsExactly(false, false, true, false);
		assertThat(result.items().get(3).note()).contains("handlebarsTemplate");
		assertValidOutput(tool, result);
	}

	@Test
	public void testAllowedOnly() {
		ListResult<PartTypeInfo> result = ListPartTypesTool.result(TYPES, true, args(Map.of("size", 1)));

		assertThat(result.total()).isEqualTo(2);
		assertThat(result.items()).extracting(PartTypeInfo::id).containsExactly(1);
		assertThat(result.nextFrom()).isEqualTo(1);
	}

	private static ListArgs args(Map<String, Object> arguments) {
		return ListArgs.of(arguments, ListPartTypesTool.LIMITS);
	}

	private static PartType type(int id, String name) {
		return new PartType().setId(id).setName(name).setJavaClass("com.example.Type" + id);
	}
}
