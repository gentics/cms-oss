package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;

/**
 * Unit tests for {@link AddConstructToPackageTool}. Adding, duplicates and the feature check are covered by the live
 * check.
 */
public class AddConstructToPackageToolTest {
	private final AddConstructToPackageTool tool = new AddConstructToPackageTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "add_construct_to_package", "Add construct to package", "packageName");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("packageName", "genaix", "constructId", 12));
		assertAccepted(tool, Map.of("packageName", "genaix", "constructKeyword", "teaser", "idempotencyKey", "k"));
		assertRejected(tool, Map.of("constructId", 12));
		assertRejected(tool, Map.of("packageName", "", "constructId", 12));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("packageName", "genaix", "constructId", 12));
	}

	@Test
	public void testExactlyOne() {
		assertThatThrownBy(() -> tool.invoke(Map.of("packageName", "genaix"), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one");
		assertThatThrownBy(() -> tool.invoke(Map.of("packageName", " ", "constructId", 12), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("packageName");
	}

	@Test
	public void testOutput() {
		assertValidOutput(tool, new AddConstructToPackageTool.Result("genaix", ObjectRef.of(ObjectRef.Type.CONSTRUCT,
				12), false));
	}
}
