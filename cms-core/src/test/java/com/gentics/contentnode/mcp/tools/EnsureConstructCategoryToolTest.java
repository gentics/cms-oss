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

import com.gentics.contentnode.mcp.model.ConstructInfo.Category;
import com.gentics.contentnode.rest.model.ConstructCategory;

/**
 * Unit tests for {@link EnsureConstructCategoryTool}. Finding and creating is covered by the live check.
 */
public class EnsureConstructCategoryToolTest {
	private final EnsureConstructCategoryTool tool = new EnsureConstructCategoryTool();

	private static final List<ConstructCategory> CATEGORIES = List.of(category(1, "Standard", "Default"),
			category(7, "GenAIx", "GenAIx"));

	@Test
	public void testDefinition() {
		assertDefinition(tool, "ensure_construct_category", "Ensure construct category", "name");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("name", Map.of("de", "GenAIx", "en", "GenAIx"), "sortOrder", 3));
		assertRejected(tool, Map.of());
		assertRejected(tool, Map.of("name", Map.of()));
		assertRejected(tool, Map.of("name", Map.of("en", "x"), "sortOrder", -1));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("name", Map.of("en", "GenAIx")));
	}

	@Test
	public void testMatching() {
		assertThat(EnsureConstructCategoryTool.find(CATEGORIES, Map.of("en", "genaix")).getId()).isEqualTo(7);
		assertThat(EnsureConstructCategoryTool.find(CATEGORIES, Map.of("de", "x", "en", "DEFAULT")).getId())
				.isEqualTo(1);
		// the name must match in the same language
		assertThat(EnsureConstructCategoryTool.find(CATEGORIES, Map.of("de", "Default"))).isNull();
		assertThat(EnsureConstructCategoryTool.find(CATEGORIES, Map.of("en", "GenAIx 2"))).isNull();
		assertThat(EnsureConstructCategoryTool.find(CATEGORIES, Map.of("en", " "))).isNull();
	}

	@Test
	public void testOutput() {
		assertValidOutput(tool, new EnsureConstructCategoryTool.Result(Category.of(CATEGORIES.get(1), null), false));
	}

	private static ConstructCategory category(int id, String de, String en) {
		ConstructCategory category = new ConstructCategory().setId(id).setSortOrder(id);
		category.setNameI18n(Map.of("de", de, "en", en));
		return category;
	}
}
