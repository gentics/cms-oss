package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ConstructInfo.Category;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.util.Slice;

/**
 * Unit tests for {@link ListConstructCategoriesTool}. Listing and counting is covered by the live check.
 */
public class ListConstructCategoriesToolTest {
	private final ListConstructCategoriesTool tool = new ListConstructCategoriesTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_construct_categories", "List construct categories");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("q", "GenAIx", "size", 5));
		assertRejected(tool, Map.of("q", "x".repeat(201)));
		assertRejected(tool, Map.of("nodeId", 1));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testOutput() {
		assertValidOutput(tool, ListResult.of(Slice.of(0, 25, 1), List.of(new Category(5, "A547.5",
				Map.of("de", "GenAIx", "en", "GenAIx"), 3, 2))));
	}
}
