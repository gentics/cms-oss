package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.tools.AssignUserToGroupTool.Result;
import com.gentics.contentnode.rest.resource.UserResource;

/**
 * Unit tests for {@link AssignUserToGroupTool}. Assigning, the node check and the permission checks are covered by
 * the live check.
 */
public class AssignUserToGroupToolTest {
	private final AssignUserToGroupTool tool = new AssignUserToGroupTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "assign_user_to_group", "Assign user to group", "userId", "groupId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("userId", 35, "groupId", 7));
		assertAccepted(tool, Map.of("userId", 35, "groupId", 7, "nodeIds", List.of(1, 3)));
		assertRejected(tool, Map.of("userId", 35, "groupId", 7, "nodeIds", List.of(1, 1)));
		assertRejected(tool, Map.of("userId", 35, "groupId", 7, "nodeIds", List.of(0)));
		assertRejected(tool, Map.of("groupId", 7));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("userId", 35, "groupId", 7));
	}

	@Test
	public void testInvalidArgumentsRejectedBeforeAnyWrite() {
		// the arguments are checked before any delegate or transaction is touched, which would fail here
		assertThatThrownBy(() -> tool.invoke(Map.of("userId", 35, "groupId", 7, "nodeIds", List.of(1, 1)),
				Optional.empty())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nodeIds");
		assertThatThrownBy(() -> tool.invoke(Map.of("userId", 35, "groupId", 7, "nodeIds",
				IntStream.rangeClosed(1, 51).boxed().toList()), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testCompensate() throws Exception {
		UserResource resource = mock(UserResource.class);

		assertThatCode(() -> AssignUserToGroupTool.compensate(resource, "35", "7", new Exception("no node")))
				.doesNotThrowAnyException();
		verify(resource).removeFromGroup("35", "7");
	}

	@Test
	public void testFailedCompensationNamesTheMembership() throws Exception {
		UserResource resource = mock(UserResource.class);
		when(resource.removeFromGroup("35", "7")).thenThrow(new IllegalStateException("database gone"));

		assertThatThrownBy(() -> AssignUserToGroupTool.compensate(resource, "35", "7", new Exception("no node")))
				.isInstanceOf(IllegalStateException.class).hasNoCause()
				.hasMessageContaining("user 35 may remain in group 7").hasMessageContaining("no node");
	}

	@Test
	public void testOutput() {
		assertValidOutput(tool, new Result(ObjectRef.of(Type.USER, 35), ObjectRef.of(Type.GROUP, 7),
				List.of(ObjectRef.of(Type.GROUP, 7)), List.of(ObjectRef.of(Type.NODE, 1))));
	}
}
