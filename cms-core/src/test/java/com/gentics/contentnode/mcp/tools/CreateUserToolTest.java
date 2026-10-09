package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.model.UserItem;
import com.gentics.contentnode.mcp.tools.CreateUserTool.Result;
import com.gentics.contentnode.rest.model.User;

/**
 * Unit tests for {@link CreateUserTool}. Creating, the duplicate login and the permission checks are covered by the
 * live check.
 */
public class CreateUserToolTest {
	private static final String PASSWORD = "correct-horse-battery";

	private final CreateUserTool tool = new CreateUserTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "create_user", "Create user in group", "groupId", "login", "firstName", "lastName");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, args());
		assertAccepted(tool, Map.of("groupId", 7, "login", "mcp.new", "firstName", "Mia", "lastName", "New"));
		assertRejected(tool, with("password", "short"));
		assertRejected(tool, with("login", "m"));
		assertRejected(tool, with("lastName", ""));
		assertRejected(tool, Map.of("groupId", 7, "login", "mcp.new", "firstName", "Mia"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, args());
	}

	@Test
	public void testArgumentsCheckedBeforeCreating() {
		assertInvalid(with("password", "short1"), "password");
		assertInvalid(with("email", "mia at example.com"), "email");
		assertInvalid(with("lastName", "  "), "lastName");
	}

	@Test
	public void testOutput() {
		User user = new User();
		user.setId(51);
		user.setLogin("mcp.new");
		user.setFirstName("Mia");
		user.setLastName("New");
		user.setPassword(PASSWORD);

		Map<String, Object> json = assertValidOutput(tool, new Result(UserItem.of(user, List.of(ObjectRef.of(
				Type.GROUP, 19))), true));

		assertThat(json.toString()).doesNotContain(PASSWORD).doesNotContain("password");
	}

	private void assertInvalid(Map<String, Object> arguments, String name) {
		assertThatThrownBy(() -> tool.invoke(arguments, Optional.empty())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("'" + name + "'").message().doesNotContain(PASSWORD).doesNotContain("short1");
	}

	private static Map<String, Object> args() {
		return Map.of("groupId", 7, "login", "mcp.new", "firstName", "Mia", "lastName", "New", "email",
				"mia@example.com", "password", PASSWORD);
	}

	private static Map<String, Object> with(String name, Object value) {
		Map<String, Object> arguments = new HashMap<>(args());
		arguments.put(name, value);
		return arguments;
	}
}
