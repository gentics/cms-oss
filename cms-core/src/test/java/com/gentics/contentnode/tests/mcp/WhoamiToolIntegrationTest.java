package com.gentics.contentnode.tests.mcp;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.ApiTokenFactory;
import com.gentics.contentnode.mcp.auth.McpRequestCredentials;
import com.gentics.contentnode.mcp.tools.WhoamiTool;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.token.ApiTokenCreationRequest;
import com.gentics.contentnode.testutils.DBTestContext;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Integration test for {@link WhoamiTool}, calling the tool directly (no running MCP server) with
 * the API tokens of two users in different groups.
 */
public class WhoamiToolIntegrationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static SystemUser alice;

	private static SystemUser bob;

	private static UserGroup aliceGroup;

	private static McpTransportContext aliceContext;

	private static McpTransportContext bobContext;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		aliceGroup = supply(() -> createUserGroup("MCP Whoami Alice", NODE_GROUP_ID));
		UserGroup bobGroup = supply(() -> createUserGroup("MCP Whoami Bob", NODE_GROUP_ID));
		alice = supply(() -> createSystemUser("Alice", "Whoami", "alice@example.com", "mcp.whoami.alice",
				"mcp.whoami.alice", Arrays.asList(aliceGroup)));
		bob = supply(() -> createSystemUser("Bob", "Whoami", null, "mcp.whoami.bob", "mcp.whoami.bob",
				Arrays.asList(bobGroup)));

		aliceContext = context(token(alice));
		bobContext = context(token(bob));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEachUserGetsThemselves() {
		Map<String, Object> aliceUser = (Map<String, Object>) success(aliceContext, Map.of()).get("user");
		Map<String, Object> bobUser = (Map<String, Object>) success(bobContext, Map.of()).get("user");

		assertThat(aliceUser).containsEntry("id", alice.getId()).containsEntry("login", "mcp.whoami.alice")
				.containsEntry("firstName", "Alice").containsEntry("lastName", "Whoami")
				.containsEntry("email", "alice@example.com");
		assertThat(bobUser).containsEntry("id", bob.getId()).containsEntry("login", "mcp.whoami.bob");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNoPassword() {
		Map<String, Object> user = (Map<String, Object>) success(aliceContext, Map.of()).get("user");

		assertThat(user.keySet()).isSubsetOf("id", "login", "firstName", "lastName", "email");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGroupsByDefault() {
		List<Map<String, Object>> groups = (List<Map<String, Object>>) success(aliceContext, Map.of()).get("groups");

		assertThat(groups).contains(Map.of("type", "group", "id", aliceGroup.getId(), "name", "MCP Whoami Alice"));
	}

	@Test
	public void testWithoutGroups() {
		assertThat(success(aliceContext, Map.of("includeGroups", false))).containsOnlyKeys("user");
	}

	@Test
	public void testDeletedTokenIsRefused() throws NodeException {
		String token = ApiTokenFactory.createToken();
		int tokenId = supply(() -> ApiTokenFactory
				.create(new ApiTokenCreationRequest().setName("MCP Whoami Deleted"), alice.getId(), token).getId());
		operate(() -> ApiTokenFactory.delete(tokenId));

		CallToolResult result = call(context(token), Map.of());

		assertThat(result.isError()).isTrue();
		assertThat(result.structuredContent()).isNull();
	}

	private static String token(SystemUser user) throws NodeException {
		String token = ApiTokenFactory.createToken();
		operate(() -> ApiTokenFactory.create(new ApiTokenCreationRequest().setName("MCP Whoami Token"), user.getId(),
				token));
		return token;
	}

	private static McpTransportContext context(String token) {
		return McpTransportContext.create(Map.of(McpRequestCredentials.CONTEXT_KEY,
				new McpRequestCredentials(Optional.of(token), Optional.empty())));
	}

	private static CallToolResult call(McpTransportContext context, Map<String, Object> arguments) {
		return new WhoamiTool().call(context,
				CallToolRequest.builder("whoami").arguments(new HashMap<>(arguments)).build());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> success(McpTransportContext context, Map<String, Object> arguments) {
		CallToolResult result = call(context, arguments);
		assertThat(result.isError()).as(String.valueOf(result.content())).isNotEqualTo(Boolean.TRUE);

		// calling the tool directly bypasses the SDK's output schema validation, so do that here
		var validation = new DefaultJsonSchemaValidator().validate(new WhoamiTool().tool().outputSchema(),
				result.structuredContent());
		assertThat(validation.valid()).as(validation.errorMessage()).isTrue();

		return (Map<String, Object>) result.structuredContent();
	}
}
