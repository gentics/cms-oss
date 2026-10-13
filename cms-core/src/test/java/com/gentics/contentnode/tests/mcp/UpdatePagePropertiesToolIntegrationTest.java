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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.ApiTokenFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.mcp.auth.McpRequestCredentials;
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
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Integration test for {@link UpdatePagePropertiesTool}, calling the tool directly (no running MCP
 * server) with the API tokens of an editor (edit permission on the test folder) and a viewer (view
 * permission only). Every test works on its own pages. The page state is always checked
 * independently of the tool (object layer, and the {@code content} table for the lock).
 */
public class UpdatePagePropertiesToolIntegrationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static Node node;

	private static Folder folder;

	private static Template template;

	private static Template otherTemplate;

	private static McpTransportContext editorContext;

	private static McpTransportContext viewerContext;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		folder = supply(() -> createFolder(node.getFolder(), "MCP Update"));
		template = supply(() -> createTemplate(folder, "MCP Template"));
		otherTemplate = supply(() -> createTemplate(folder, "MCP Other Template"));

		UserGroup editors = restrictedGroup("MCP Update Editors", new PermHandler.Permission(PermHandler.PERM_VIEW,
				PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE).toString());
		UserGroup viewers = restrictedGroup("MCP Update Viewers",
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString());

		SystemUser editor = supply(() -> createSystemUser("MCP", "Editor", null, "mcp.update.editor",
				"mcp.update.editor", Arrays.asList(editors)));
		SystemUser viewer = supply(() -> createSystemUser("MCP", "Viewer", null, "mcp.update.viewer",
				"mcp.update.viewer", Arrays.asList(viewers)));

		editorContext = context(editor);
		viewerContext = context(viewer);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testChangeNameOnly() throws Exception {
		Page page = page("Name Only");
		PageState before = state(page);

		Map<String, Object> result = success(editorContext, Map.of("pageId", page.getId(), "name", "Name Only Renamed"));

		assertThat(result.get("changedFields")).isEqualTo(List.of("name"));
		Map<String, Object> resultPage = (Map<String, Object>) result.get("page");
		Map<String, Object> ref = (Map<String, Object>) resultPage.get("ref");
		assertThat(ref).containsEntry("type", "page").containsEntry("id", page.getId())
				.containsEntry("name", "Name Only Renamed").containsEntry("nodeId", node.getId());
		assertThat(resultPage).containsEntry("locked", false).containsEntry("folderId", folder.getId())
				.doesNotContainKey("tags");

		assertThat(state(page)).isEqualTo(before.withName("Name Only Renamed"));
		assertThat(isLocked(page)).isFalse();
	}

	@Test
	public void testResubmitSameNameChangesNothing() throws Exception {
		Page page = page("Same Name");
		PageState before = state(page);

		Map<String, Object> result = success(editorContext, Map.of("pageId", page.getId(), "name", "Same Name"));

		assertThat(result.get("changedFields")).isEqualTo(List.of());
		assertThat(state(page)).isEqualTo(before);
		assertThat(isLocked(page)).isFalse();
	}

	@Test
	public void testChangeSeveralFields() throws Exception {
		Page page = page("Several");
		PageState before = state(page);

		Map<String, Object> result = success(editorContext,
				Map.of("pageId", page.getId(), "description", "New description", "priority", 42));

		assertThat(result.get("changedFields")).isEqualTo(List.of("description", "priority"));
		assertThat(state(page)).isEqualTo(new PageState(before.name(), before.fileName(), "New description",
				before.niceUrl(), 42, before.templateId()));
	}

	@Test
	public void testChangeTemplate() throws Exception {
		Page page = page("Template Change");

		Map<String, Object> result = success(editorContext,
				Map.of("pageId", page.getId(), "templateId", otherTemplate.getId()));

		assertThat(result.get("changedFields")).isEqualTo(List.of("templateId"));
		assertThat(pageOf(result)).containsEntry("templateId", otherTemplate.getId());
		assertThat(state(page).templateId()).isEqualTo(otherTemplate.getId());
	}

	/**
	 * The filename is derived by save() without being in the request, the snapshot diff reports
	 * it anyway
	 */
	@Test
	public void testDeriveFileName() throws Exception {
		Page page = page("Derive Before");
		PageState before = state(page);

		Map<String, Object> result = success(editorContext,
				Map.of("pageId", page.getId(), "name", "Derive After", "deriveFileName", true));

		assertThat(result.get("changedFields")).isEqualTo(List.of("name", "fileName"));
		PageState after = state(page);
		assertThat(after.name()).isEqualTo("Derive After");
		assertThat(after.fileName()).isNotEqualTo(before.fileName());
		assertThat(pageOf(result)).containsEntry("fileName", after.fileName());
	}

	/**
	 * save() rejects the duplicate name with INVALIDDATA after the page was locked: the tool must
	 * report the error, release the lock, and not change (or restore) anything
	 */
	@Test
	public void testDuplicateNameIsRejectedAndPageUnlocked() throws Exception {
		page("Duplicate Existing");
		Page page = page("Duplicate Target");
		PageState before = state(page);

		String error = failure(editorContext, Map.of("pageId", page.getId(), "name", "Duplicate Existing"));

		assertThat(error).contains("was not saved");
		assertThat(isLocked(page)).as("page locked after rejected save").isFalse();
		assertThat(state(page)).isEqualTo(before);
	}

	@Test
	public void testDuplicateFileNameIsRejectedAndPageUnlocked() throws Exception {
		Page existing = page("Duplicate File Existing");
		Page page = page("Duplicate File Target");
		PageState before = state(page);

		String error = failure(editorContext,
				Map.of("pageId", page.getId(), "fileName", state(existing).fileName()));

		assertThat(error).contains("was not saved");
		assertThat(isLocked(page)).as("page locked after rejected save").isFalse();
		assertThat(state(page)).isEqualTo(before);
	}

	@Test
	public void testViewerIsRejected() throws Exception {
		Page page = page("Viewer");
		PageState before = state(page);

		failure(viewerContext, Map.of("pageId", page.getId(), "name", "Viewer Renamed"));

		assertThat(state(page)).isEqualTo(before);
		assertThat(isLocked(page)).isFalse();
	}

	@Test
	public void testNodeIdIsRejected() throws Exception {
		Page page = page("With Node ID");
		PageState before = state(page);

		String error = failure(editorContext,
				Map.of("pageId", page.getId(), "nodeId", node.getId(), "name", "With Node ID Renamed"));

		assertThat(error).contains("does not yet support nodeId");
		assertThat(state(page)).isEqualTo(before);
		assertThat(isLocked(page)).isFalse();
	}

	/**
	 * A page locked by another user (here: the system user, which created it) cannot be locked by
	 * the editor, so save() fails. The tool must report that, and must not release the other
	 * user's lock.
	 */
	@Test
	public void testPageLockedByOtherUserIsRejectedAndLockKept() throws Exception {
		Page page = lockedPage("Locked By Other");
		PageState before = state(page);
		int lockOwner = lockedBy(page);
		assertThat(lockOwner).as("lock owner before the call").isNotZero();

		String error = failure(editorContext, Map.of("pageId", page.getId(), "name", "Locked By Other Renamed"));

		assertThat(error).contains("Could not lock");
		assertThat(isLocked(page)).as("other user's lock kept").isTrue();
		assertThat(lockedBy(page)).isEqualTo(lockOwner);
		assertThat(state(page)).isEqualTo(before);
	}

	@Test
	public void testUnknownPageIsRejected() throws Exception {
		failure(editorContext, Map.of("pageId", Integer.MAX_VALUE, "name", "Nothing"));
	}

	/**
	 * The fields the tool can change, read from the object layer
	 * @param name name
	 * @param fileName filename
	 * @param description description
	 * @param niceUrl nice URL
	 * @param priority priority
	 * @param templateId template ID
	 */
	private record PageState(String name, String fileName, String description, String niceUrl, int priority,
			Integer templateId) {
		PageState withName(String newName) {
			return new PageState(newName, fileName, description, niceUrl, priority, templateId);
		}
	}

	private static PageState state(Page page) throws NodeException {
		return supply(t -> {
			Page current = t.getObject(Page.class, page.getId());
			return new PageState(current.getName(), current.getFilename(), current.getDescription(),
					current.getNiceUrl(), current.getPriority(), current.getTemplate().getId());
		});
	}

	private static boolean isLocked(Page page) throws NodeException {
		return supply(() -> DBUtils.select(
				"SELECT c.locked FROM content c JOIN page p ON p.content_id = c.id WHERE p.id = ?",
				ps -> ps.setInt(1, page.getId()), rs -> rs.next() && rs.getInt("locked") != 0));
	}

	/**
	 * Create an unlocked page. A new page is locked by its creator (the system user here, see
	 * {@code PageFactory#saveContentObject}), which would make every call of the editor fail with
	 * "Could not lock".
	 * @param name page name
	 * @return page
	 * @throws NodeException
	 */
	private static Page page(String name) throws NodeException {
		Page page = lockedPage(name);
		operate(() -> page.unlock());
		return page;
	}

	/**
	 * Create a page, which stays locked by its creator, the system user
	 * @param name page name
	 * @return page
	 * @throws NodeException
	 */
	private static Page lockedPage(String name) throws NodeException {
		return supply(() -> createPage(folder, template, name));
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
		// same token creation as com.gentics.contentnode.tests.utils.Auth, which does not expose the raw token
		String token = ApiTokenFactory.createToken();
		operate(() -> ApiTokenFactory.create(new ApiTokenCreationRequest().setName("MCP Test Token"), user.getId(),
				token));
		return McpTransportContext.create(Map.of(McpRequestCredentials.CONTEXT_KEY,
				new McpRequestCredentials(Optional.of(token), Optional.empty())));
	}

	private static CallToolResult call(McpTransportContext context, Map<String, Object> arguments) {
		return new UpdatePagePropertiesTool().call(context,
				CallToolRequest.builder("update_page_properties").arguments(new HashMap<>(arguments)).build());
	}

	private static Map<String, Object> success(McpTransportContext context, Map<String, Object> arguments)
			throws Exception {
		CallToolResult result = call(context, arguments);
		String text = ((TextContent) result.content().get(0)).text();
		assertThat(result.isError()).as(text).isNotEqualTo(Boolean.TRUE);

		// calling the tool directly bypasses the SDK, which would validate the structured content
		// against the output schema (and turn a mismatch into an error), so do that here
		var validation = new DefaultJsonSchemaValidator().validate(new UpdatePagePropertiesTool().tool().outputSchema(),
				result.structuredContent());
		assertThat(validation.valid()).as(validation.errorMessage()).isTrue();

		return MAPPER.readValue(text, new TypeReference<Map<String, Object>>() {
		});
	}

	private static String failure(McpTransportContext context, Map<String, Object> arguments) {
		CallToolResult result = call(context, arguments);
		String text = ((TextContent) result.content().get(0)).text();
		assertThat(result.isError()).as(text).isTrue();
		return text;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> pageOf(Map<String, Object> result) {
		return (Map<String, Object>) result.get("page");
	}
}
