package com.gentics.contentnode.tests.mcp;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
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
import com.gentics.contentnode.mcp.model.Problem;
import com.gentics.contentnode.mcp.tools.GetPermissionsTool;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.model.token.ApiTokenCreationRequest;
import com.gentics.contentnode.testutils.DBTestContext;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Integration test for {@link GetPermissionsTool}, calling the tool directly (no running MCP
 * server) with the API token of an editor: view and edit (no publish) on pages of the test folder,
 * view and construct editing on the node, view on the administration, nothing on a second folder.
 */
public class GetPermissionsToolIntegrationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Folder folder;

	private static Page page;

	private static Page hiddenPage;

	private static McpTransportContext editorContext;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		folder = supply(() -> createFolder(node.getFolder(), "MCP Permissions"));
		Folder hiddenFolder = supply(() -> createFolder(node.getFolder(), "MCP Permissions Hidden"));
		Template template = supply(() -> createTemplate(folder, "MCP Permissions Template"));
		page = supply(() -> createPage(folder, template, "MCP Permissions Page"));
		hiddenPage = supply(() -> createPage(hiddenFolder, template, "MCP Permissions Hidden Page"));

		UserGroup editors = supply(() -> createUserGroup("MCP Permission Editors", NODE_GROUP_ID));
		// createUserGroup copies the mother group's permissions, start with none instead
		operate(() -> update("DELETE FROM perm WHERE usergroup_id = ?", editors.getId()));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(editors.getId()));
		operate(() -> setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(editors),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));
		operate(() -> setPermissions(Folder.TYPE_FOLDER, folder.getId(), Arrays.asList(editors),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW,
						PermHandler.PERM_PAGE_UPDATE).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(editors),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));

		SystemUser editor = supply(() -> createSystemUser("MCP", "Permission Editor", null, "mcp.permission.editor",
				"mcp.permission.editor", Arrays.asList(editors)));
		editorContext = context(editor);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPagePermissions() {
		Map<String, Object> result = success(Map.of("type", "page", "id", page.getId()));

		Map<String, Object> granted = (Map<String, Object>) result.get("granted");
		assertThat(granted).containsEntry("view", true).containsEntry("edit", true).containsEntry("publish", false)
				.doesNotContainKey("userassignment");
		assertThat((Map<String, Object>) result.get("ref")).isEqualTo(Map.of("type", "page", "id", page.getId()));
		assertThat(result).containsEntry("type", "page");
	}

	@Test
	public void testNarrowedFolderPermission() {
		Map<String, Object> result = success(
				Map.of("type", "folder", "id", folder.getId(), "permissions", List.of("publishpages")));

		assertThat(result.get("granted")).isEqualTo(Map.of("publishpages", false));
	}

	@Test
	public void testNodeIdIsPassedThrough() {
		Map<String, Object> result = success(Map.of("type", "page", "id", page.getId(), "nodeId", node.getId(),
				"permissions", List.of("view", "edit")));

		assertThat(result.get("granted")).isEqualTo(Map.of("view", true, "edit", true));
	}

	@Test
	public void testUpdateconstructsOnNode() {
		Map<String, Object> result = success(
				Map.of("type", "node", "id", node.getId(), "permissions", List.of("updateconstructs")));

		assertThat(result.get("granted")).isEqualTo(Map.of("updateconstructs", true));
	}

	/**
	 * updateconstructs is a node permission of the CMS, not one of the construct type
	 */
	@Test
	public void testUpdateconstructsOnConstructTypeIsInvalid() {
		Map<String, Object> problem = problem(Map.of("type", "construct", "permissions", List.of("updateconstructs")));

		assertThat(problem).containsEntry("type", Problem.TYPE_PREFIX + "invalid-data");
		assertThat((String) problem.get("detail")).contains("'id'");
	}

	@Test
	public void testAdministrationTypePermissions() {
		Map<String, Object> result = success(Map.of("type", "admin", "permissions", List.of("read")));

		assertThat(result.get("granted")).isEqualTo(Map.of("read", true));
		assertThat(result).doesNotContainKey("ref");
	}

	@Test
	public void testFolderWithoutIdIsInvalid() {
		assertThat(problem(Map.of("type", "folder"))).containsEntry("type", Problem.TYPE_PREFIX + "invalid-data");
	}

	@Test
	public void testHiddenPageIsDenied() {
		CallToolResult result = call(Map.of("type", "page", "id", hiddenPage.getId()));

		assertThat(problem(result)).containsEntry("type", Problem.TYPE_PREFIX + "permission-denied");
		assertThat(String.valueOf(result.structuredContent())).doesNotContain("MCP Permissions Hidden Page");
	}

	@Test
	public void testMissingPageIsDenied() {
		assertThat(problem(Map.of("type", "page", "id", Integer.MAX_VALUE))).containsEntry("type",
				Problem.TYPE_PREFIX + "permission-denied");
	}

	private static McpTransportContext context(SystemUser user) throws NodeException {
		String token = ApiTokenFactory.createToken();
		operate(() -> ApiTokenFactory.create(new ApiTokenCreationRequest().setName("MCP Permissions Token"),
				user.getId(), token));
		return McpTransportContext.create(Map.of(McpRequestCredentials.CONTEXT_KEY,
				new McpRequestCredentials(Optional.of(token), Optional.empty())));
	}

	private static CallToolResult call(Map<String, Object> arguments) {
		return new GetPermissionsTool().call(editorContext,
				CallToolRequest.builder("get_permissions").arguments(new HashMap<>(arguments)).build());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> success(Map<String, Object> arguments) {
		CallToolResult result = call(arguments);
		assertThat(result.isError()).as(String.valueOf(result.content())).isNotEqualTo(Boolean.TRUE);

		// calling the tool directly bypasses the SDK's output schema validation, so do that here
		var validation = new DefaultJsonSchemaValidator().validate(new GetPermissionsTool().tool().outputSchema(),
				result.structuredContent());
		assertThat(validation.valid()).as(validation.errorMessage()).isTrue();

		return (Map<String, Object>) result.structuredContent();
	}

	private static Map<String, Object> problem(Map<String, Object> arguments) {
		return problem(call(arguments));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> problem(CallToolResult result) {
		assertThat(result.isError()).isTrue();
		Map<String, Object> problem = (Map<String, Object>) ((Map<String, Object>) result.structuredContent())
				.get("problem");
		assertThat(problem).isNotNull().containsEntry("tool", "get_permissions");
		return problem;
	}
}
