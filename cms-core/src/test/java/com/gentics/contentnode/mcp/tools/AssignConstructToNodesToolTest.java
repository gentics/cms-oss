package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.rest.model.request.BulkLinkUpdateRequest;

/**
 * Unit tests for {@link AssignConstructToNodesTool}. Linking and permissions are covered by the live check.
 */
public class AssignConstructToNodesToolTest {
	private final AssignConstructToNodesTool tool = new AssignConstructToNodesTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "assign_construct_to_nodes", "Assign construct to nodes", "constructIds", "nodeIds");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("constructIds", List.of(12), "nodeIds", List.of(1, 2), "mode", "set"));
		assertRejected(tool, Map.of("constructIds", List.of(), "nodeIds", List.of(1)));
		assertRejected(tool, Map.of("constructIds", List.of(12), "nodeIds", List.of(1), "mode", "replace"));
		assertRejected(tool, Map.of("constructIds", List.of(12)));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("constructIds", List.of(12), "nodeIds", List.of(1)));
	}

	@Test
	public void testChanges() {
		Set<Integer> current = Set.of(3, 4);

		assertChanges("add", current, List.of(3, 5), Set.of(5), Set.of());
		assertChanges("remove", current, List.of(3, 5), Set.of(), Set.of(3));
		assertChanges("set", current, List.of(3), Set.of(), Set.of(4));
		assertChanges("set", current, List.of(5), Set.of(5), Set.of(3, 4));
	}

	@Test
	public void testRequest() {
		BulkLinkUpdateRequest request = AssignConstructToNodesTool.request(Set.of(3), 12);

		assertThat(request.getIds()).containsExactly(3);
		assertThat(request.getTargetIds()).containsExactly("12");
	}

	@Test
	public void testOutput() {
		assertValidOutput(tool, new AssignConstructToNodesTool.Result(List.of(new AssignConstructToNodesTool
				.Assignment(ObjectRef.of(ObjectRef.Type.CONSTRUCT, 12), List.of(ObjectRef.of(ObjectRef.Type.NODE,
						3))))));
	}

	private static void assertChanges(String mode, Set<Integer> current, List<Integer> nodeIds, Set<Integer> add,
			Set<Integer> remove) {
		Set<Integer> actualAdd = new LinkedHashSet<>();
		Set<Integer> actualRemove = new LinkedHashSet<>();
		AssignConstructToNodesTool.changes(mode, current, nodeIds, actualAdd, actualRemove);
		assertThat(actualAdd).as("%s add", mode).isEqualTo(add);
		assertThat(actualRemove).as("%s remove", mode).isEqualTo(remove);
	}
}
