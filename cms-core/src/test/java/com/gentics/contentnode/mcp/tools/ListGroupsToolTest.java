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
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.tools.ListGroupsTool.GroupItem;
import com.gentics.contentnode.mcp.util.Slice;

/**
 * Unit tests for {@link ListGroupsTool}. Listing, parents, member counts and permissions are covered by the live
 * check.
 */
public class ListGroupsToolTest {
	private final ListGroupsTool tool = new ListGroupsTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_groups", "List groups");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("q", "editors", "size", 200, "from", 25));
		assertRejected(tool, Map.of("size", 0));
		assertRejected(tool, Map.of("includeUsers", true));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testOutput() {
		ObjectRef parent = ObjectRef.of(Type.GROUP, 2);
		List<GroupItem> items = List.of(new GroupItem(ObjectRef.of(Type.GROUP, 7), "Editors", 3, parent),
				new GroupItem(ObjectRef.of(Type.GROUP, 2), null, null, null));

		Map<String, Object> json = assertValidOutput(tool, ListResult.of(Slice.of(0, 25, 2), items));

		assertThat((List<?>) json.get("items")).last().isEqualTo(Map.of("ref", Map.of("type", "group", "id", 2)));
	}
}
