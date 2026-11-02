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
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.mcp.auth.McpRequestCredentials;
import com.gentics.contentnode.mcp.model.Problem;
import com.gentics.contentnode.mcp.tools.UpdatePagePropertiesTool;
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
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Integration test for the problem results of failed tool calls ({@link Problem}), with
 * {@code update_page_properties} as the vehicle, because its failures cover the CMS's ways of
 * reporting an error: a thrown lock or privilege failure, a missing object and a response with
 * {@code INVALIDDATA}.
 */
public class McpProblemIntegrationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Folder folder;

	private static Template template;

	private static McpTransportContext editorContext;

	private static McpTransportContext viewerContext;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		Node node = supply(() -> createNode());
		folder = supply(() -> createFolder(node.getFolder(), "MCP Problems"));
		template = supply(() -> createTemplate(folder, "MCP Problems Template"));

		UserGroup editors = restrictedGroup("MCP Problem Editors", new PermHandler.Permission(PermHandler.PERM_VIEW,
				PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE).toString());
		UserGroup viewers = restrictedGroup("MCP Problem Viewers",
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString());

		SystemUser editor = supply(() -> createSystemUser("MCP", "Problem Editor", null, "mcp.problem.editor",
				"mcp.problem.editor", Arrays.asList(editors)));
		SystemUser viewer = supply(() -> createSystemUser("MCP", "Problem Viewer", null, "mcp.problem.viewer",
				"mcp.problem.viewer", Arrays.asList(viewers)));

		editorContext = context(editor);
		viewerContext = context(viewer);
	}

	/**
	 * A page locked by another user (the system user, which created it) is object-locked, and that
	 * user's lock is kept
	 */
	@Test
	public void testLockedPageIsObjectLocked() throws Exception {
		Page page = supply(() -> createPage(folder, template, "Problem Locked"));
		int lockOwner = lockedBy(page);

		Map<String, Object> problem = problem(editorContext, Map.of("pageId", page.getId(), "name", "Renamed"));

		assertThat(problem).containsEntry("type", Problem.TYPE_PREFIX + "object-locked").containsEntry("status", 423)
				.containsEntry("retryable", true).containsEntry("tool", "update_page_properties");
		assertThat(lockedBy(page)).as("other user's lock kept").isEqualTo(lockOwner).isNotZero();
	}

	@Test
	public void testViewerIsPermissionDenied() throws Exception {
		Page page = page("Problem Viewer");

		Map<String, Object> problem = problem(viewerContext, Map.of("pageId", page.getId(), "name", "Renamed"));

		assertThat(problem).containsEntry("type", Problem.TYPE_PREFIX + "permission-denied")
				.containsEntry("status", 403).containsEntry("retryable", false);
		assertThat(cms(problem)).containsEntry("responseCode", "PERMISSION");
	}

	@Test
	public void testUnknownPageIsNotFound() throws Exception {
		Map<String, Object> problem = problem(editorContext, Map.of("pageId", Integer.MAX_VALUE, "name", "Nothing"));

		assertThat(problem).containsEntry("type", Problem.TYPE_PREFIX + "not-found").containsEntry("status", 404);
		assertThat(cms(problem)).containsEntry("responseCode", "NOTFOUND");
	}

	/**
	 * save() answers the duplicate name with {@code INVALIDDATA} instead of throwing
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testDuplicateNameIsInvalidData() throws Exception {
		page("Problem Duplicate Existing");
		Page page = page("Problem Duplicate Target");

		Map<String, Object> problem = problem(editorContext,
				Map.of("pageId", page.getId(), "name", "Problem Duplicate Existing"));

		assertThat(problem).containsEntry("type", Problem.TYPE_PREFIX + "invalid-data").containsEntry("status", 422);
		assertThat(cms(problem)).containsEntry("responseCode", "INVALIDDATA");
		assertThat((List<Map<String, Object>>) cms(problem).get("messages")).isNotEmpty();
	}

	private static Map<String, Object> problem(McpTransportContext context, Map<String, Object> arguments) {
		CallToolResult result = new UpdatePagePropertiesTool().call(context,
				CallToolRequest.builder("update_page_properties").arguments(new HashMap<>(arguments)).build());
		assertThat(result.isError()).isTrue();
		assertThat(result.content()).isNotEmpty();

		@SuppressWarnings("unchecked")
		Map<String, Object> problem = (Map<String, Object>) ((Map<String, Object>) result.structuredContent())
				.get("problem");
		assertThat(problem).isNotNull();
		assertThat((String) problem.get("detail")).isNotBlank().doesNotContain("\tat ");
		return problem;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> cms(Map<String, Object> problem) {
		return (Map<String, Object>) problem.get("cms");
	}

	/**
	 * Create an unlocked page (a new page is locked by its creator)
	 * @param name page name
	 * @return page
	 * @throws NodeException
	 */
	private static Page page(String name) throws NodeException {
		Page page = supply(() -> createPage(folder, template, name));
		operate(() -> page.unlock());
		return page;
	}

	private static int lockedBy(Page page) throws NodeException {
		return supply(() -> DBUtils.select(
				"SELECT c.locked_by FROM content c JOIN page p ON p.content_id = c.id WHERE p.id = ?",
				ps -> ps.setInt(1, page.getId()), rs -> rs.next() ? rs.getInt("locked_by") : 0));
	}

	private static UserGroup restrictedGroup(String name, String folderPerm) throws NodeException {
		UserGroup group = supply(() -> createUserGroup(name, NODE_GROUP_ID));
		// createUserGroup copies the mother group's permissions, start with none instead
		operate(() -> update("DELETE FROM perm WHERE usergroup_id = ?", group.getId()));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(group.getId()));
		operate(() -> setPermissions(Folder.TYPE_FOLDER, folder.getId(), Arrays.asList(group), folderPerm));
		return group;
	}

	private static McpTransportContext context(SystemUser user) throws NodeException {
		String token = ApiTokenFactory.createToken();
		operate(() -> ApiTokenFactory.create(new ApiTokenCreationRequest().setName("MCP Problem Token"),
				user.getId(), token));
		return McpTransportContext.create(Map.of(McpRequestCredentials.CONTEXT_KEY,
				new McpRequestCredentials(Optional.of(token), Optional.empty())));
	}
}
