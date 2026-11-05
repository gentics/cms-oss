package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.model.UserItem;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.User;

/**
 * Unit tests for {@link ListUsersTool}. Listing, the visible groups and permissions are covered by the live check.
 */
public class ListUsersToolTest {
	private final ListUsersTool tool = new ListUsersTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_users", "List users");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("q", "mcp.test", "includeGroups", false, "size", 10));
		assertRejected(tool, Map.of("includeGroups", "yes"));
		assertRejected(tool, Map.of("from", 10001));
		assertRejected(tool, Map.of("active", true));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testOutput() {
		User user = new User();
		user.setId(35);
		user.setLogin("mcp.test");
		user.setFirstName("Mia");
		user.setLastName("Test");
		user.setEmail("mia@example.com");
		user.setPassword("secret-password");
		List<UserItem> items = List.of(UserItem.of(user, List.of(ObjectRef.of(Type.GROUP, 7))), UserItem.of(user,
				null));

		Map<String, Object> json = assertValidOutput(tool, ListResult.of(Slice.of(0, 25, 2), items));

		assertThat(json.toString()).doesNotContain("password", "secret");
		assertThat((List<?>) json.get("items")).last().asInstanceOf(MAP).doesNotContainKey("groups");
	}
}
