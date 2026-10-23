package com.gentics.contentnode.tests.mcp;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.ApiTokenFactory;
import com.gentics.contentnode.mcp.auth.McpRequestCredentials;
import com.gentics.contentnode.mcp.tools.ListNodesTool;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.model.token.ApiTokenCreationRequest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Integration test for {@link ListNodesTool}, calling the tool directly (no running MCP server)
 * with the API token of a restricted user, so that the permission filtering of the delegated-to
 * {@code NodeResourceImpl#list}/{@code #languages} runs against that user.
 */
public class ListNodesToolIntegrationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static Node alpha;

	private static Node beta;

	private static Node gamma;

	private static Node hidden;

	private static McpTransportContext context;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		UserGroup group = supply(() -> createUserGroup("MCP List Nodes Group", NODE_GROUP_ID));
		// start with no permissions at all, so only the nodes granted below are visible
		operate(() -> update("DELETE FROM perm WHERE usergroup_id = ?", group.getId()));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(group.getId()));

		SystemUser user = supply(
				() -> createSystemUser("MCP", "Lister", null, "mcp.lister", "mcp.lister", Arrays.asList(group)));

		alpha = supply(() -> createNode("alpha.mcp.test", "mcp-list-alpha", PublishTarget.NONE, getLanguage("de"),
				getLanguage("en")));
		beta = supply(() -> createNode("beta.mcp.test", "mcp-list-beta", PublishTarget.NONE, getLanguage("en")));
		gamma = supply(() -> createNode("gamma.mcp.test", "mcp-list-gamma", PublishTarget.NONE));
		hidden = supply(() -> createNode("hidden.mcp.test", "mcp-list-hidden", PublishTarget.NONE, getLanguage("de")));

		for (Node node : List.of(alpha, beta, gamma)) {
			operate(() -> setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(group),
					new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		}

		// same token creation as com.gentics.contentnode.tests.utils.Auth, which does not expose the raw token
		String token = ApiTokenFactory.createToken();
		operate(() -> ApiTokenFactory.create(new ApiTokenCreationRequest().setName("MCP Test Token"), user.getId(), token));
		context = McpTransportContext
				.create(Map.of(McpRequestCredentials.CONTEXT_KEY, new McpRequestCredentials(Optional.of(token), Optional.empty())));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testListsOnlyVisibleNodesWithLanguages() throws Exception {
		Map<String, Object> result = call(Map.of());

		assertThat(result).containsEntry("total", 3).containsEntry("totalIsExact", true)
				.containsEntry("truncated", false).doesNotContainKey("nextFrom");
		assertThat(names(result)).containsExactly("mcp-list-alpha", "mcp-list-beta", "mcp-list-gamma");

		Map<String, Object> alphaItem = items(result).get(0);
		Map<String, Object> ref = (Map<String, Object>) alphaItem.get("ref");
		assertThat(ref).containsEntry("type", "node").containsEntry("id", alpha.getId()).doesNotContainKey("nodeId");
		// the REST model prefixes the hostname with the protocol (ModelBuilder#getNode)
		assertThat(alphaItem).containsEntry("host", "http://alpha.mcp.test");
		assertThat(languageCodes(alphaItem)).containsExactly("de", "en");

		assertThat(languageCodes(items(result).get(1))).containsExactly("en");
		assertThat(languageCodes(items(result).get(2))).isEmpty();
	}

	@Test
	public void testQueryByName() throws Exception {
		Map<String, Object> result = call(Map.of("q", "mcp-list-beta"));

		assertThat(result).containsEntry("total", 1).containsEntry("truncated", false);
		assertThat(names(result)).containsExactly("mcp-list-beta");
	}

	@Test
	public void testQueryMatchingOnlyHiddenNode() throws Exception {
		Map<String, Object> result = call(Map.of("q", "mcp-list-hidden"));

		assertThat(result).containsEntry("total", 0);
		assertThat(items(result)).isEmpty();
	}

	@Test
	public void testPaging() throws Exception {
		Map<String, Object> first = call(Map.of("size", 2, "from", 0));
		assertThat(first).containsEntry("total", 3).containsEntry("truncated", true).containsEntry("nextFrom", 2);
		assertThat(names(first)).containsExactly("mcp-list-alpha", "mcp-list-beta");

		Map<String, Object> second = call(Map.of("size", 2, "from", first.get("nextFrom")));
		assertThat(second).containsEntry("total", 3).containsEntry("truncated", false).doesNotContainKey("nextFrom");
		assertThat(names(second)).containsExactly("mcp-list-gamma");

		List<String> all = new ArrayList<>(names(first));
		all.addAll(names(second));
		assertThat(all).containsExactly("mcp-list-alpha", "mcp-list-beta", "mcp-list-gamma");
	}

	@Test
	public void testFromBeyondTotal() throws Exception {
		Map<String, Object> result = call(Map.of("from", 100));

		assertThat(result).containsEntry("total", 3).containsEntry("truncated", false);
		assertThat(items(result)).isEmpty();
	}

	private static Map<String, Object> call(Map<String, Object> arguments) throws Exception {
		CallToolResult result = new ListNodesTool().call(context,
				CallToolRequest.builder("list_nodes").arguments(new HashMap<>(arguments)).build());
		String text = ((TextContent) result.content().get(0)).text();
		assertThat(result.isError()).as(text).isNotEqualTo(Boolean.TRUE);
		return MAPPER.readValue(text, new TypeReference<Map<String, Object>>() {
		});
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> items(Map<String, Object> result) {
		return (List<Map<String, Object>>) result.get("items");
	}

	@SuppressWarnings("unchecked")
	private static List<String> names(Map<String, Object> result) {
		return items(result).stream().map(item -> (String) ((Map<String, Object>) item.get("ref")).get("name")).toList();
	}

	@SuppressWarnings("unchecked")
	private static List<String> languageCodes(Map<String, Object> item) {
		return ((List<Map<String, Object>>) item.get("languages")).stream().map(l -> (String) l.get("code")).toList();
	}
}
