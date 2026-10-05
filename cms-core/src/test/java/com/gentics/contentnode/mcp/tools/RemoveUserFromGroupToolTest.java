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

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.tools.RemoveUserFromGroupTool.Result;

/**
 * Unit tests for {@link RemoveUserFromGroupTool}. Removing and the permission checks are covered by the live check.
 */
public class RemoveUserFromGroupToolTest {
	private final RemoveUserFromGroupTool tool = new RemoveUserFromGroupTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "remove_user_from_group", "Remove user from group", "userId", "groupId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("userId", 35, "groupId", 7, "idempotencyKey", "k1"));
		assertRejected(tool, Map.of("userId", 35));
		assertRejected(tool, Map.of("userId", 35, "groupId", 7, "nodeIds", List.of(1)));
		assertRejected(tool, Map.of("userId", 35, "groupId", 7, "idempotencyKey", "k".repeat(129)));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("userId", 35, "groupId", 7));
	}

	@Test
	public void testWarnings() {
		assertThat(RemoveUserFromGroupTool.warnings(List.of())).singleElement().asString()
				.contains("no group you can view");
		assertThat(RemoveUserFromGroupTool.warnings(List.of(ObjectRef.of(Type.GROUP, 8)))).isEmpty();
	}

	@Test
	public void testOutput() {
		assertValidOutput(tool, new Result(ObjectRef.of(Type.USER, 35), ObjectRef.of(Type.GROUP, 7), List.of(),
				RemoveUserFromGroupTool.warnings(List.of())));
	}
}
