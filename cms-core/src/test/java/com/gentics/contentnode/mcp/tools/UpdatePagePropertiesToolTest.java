package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.UpdatePagePropertiesTool.CHANGED_FIELDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.auth.McpRequestCredentials;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.mcp.tools.UpdatePagePropertiesTool.UpdatePagePropertiesResult;
import com.gentics.contentnode.mcp.tools.UpdatePagePropertiesTool.UpdateRequest;
import com.gentics.contentnode.mcp.util.ChangedFields;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.PageVersion;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.TranslationStatus;
import com.gentics.contentnode.rest.model.User;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.util.ToolInputValidator;

/**
 * Unit tests for {@link UpdatePagePropertiesTool}, without a DB/CMS instance. The actual
 * load/save/reload against a real page is covered by
 * {@code com.gentics.contentnode.tests.mcp.UpdatePagePropertiesToolIntegrationTest}.
 */
public class UpdatePagePropertiesToolTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final UpdatePagePropertiesTool tool = new UpdatePagePropertiesTool();

	@Test
	public void testToolDefinition() {
		Tool definition = tool.tool();

		assertThat(definition.name()).isEqualTo("update_page_properties");
		assertThat(definition.description()).contains("update_page_tags").contains("get_template");
		assertThat(definition.inputSchema()).containsEntry("required", List.of("pageId"))
				.containsEntry("additionalProperties", false);

		Map<String, Map<String, Object>> properties = inputProperties(definition);
		assertThat(properties).containsOnlyKeys("pageId", "nodeId", "name", "fileName", "description", "niceUrl",
				"priority", "templateId", "deriveFileName", "failOnDuplicate", "idempotencyKey");

		assertThat(properties.get("pageId")).containsEntry("type", "integer").containsEntry("minimum", 1);
		assertThat(properties.get("nodeId")).containsEntry("type", "integer").containsEntry("minimum", 1);
		assertThat(properties.get("name")).containsEntry("type", "string").containsEntry("minLength", 1)
				.containsEntry("maxLength", 255);
		assertThat(properties.get("fileName")).containsEntry("type", "string").containsEntry("maxLength", 255);
		assertThat(properties.get("description")).containsEntry("type", "string").containsEntry("maxLength", 4000);
		assertThat(properties.get("niceUrl")).containsEntry("type", "string").containsEntry("maxLength", 500);
		assertThat(properties.get("priority")).containsEntry("type", "integer").containsEntry("minimum", 1)
				.containsEntry("maximum", 100);
		assertThat(properties.get("templateId")).containsEntry("type", "integer").containsEntry("minimum", 1);
		assertThat(properties.get("deriveFileName")).containsEntry("type", "boolean").containsEntry("default", false);
		assertThat(properties.get("failOnDuplicate")).containsEntry("type", "boolean").containsEntry("default", true);
		assertThat(properties.get("idempotencyKey")).containsEntry("type", "string").containsEntry("maxLength", 128);

		assertThat(definition.outputSchema()).containsEntry("required", List.of("page", "changedFields"));
	}

	/**
	 * The SDK rejects a tool whose schemas are not valid JSON schemas when it is registered at
	 * server startup ({@code McpAsyncServer#addTool}), which no other test exercises
	 */
	@Test
	public void testSchemasAreValidSchemas() {
		DefaultJsonSchemaValidator validator = new DefaultJsonSchemaValidator();

		ValidationResponse input = validator.validateSchema(tool.tool().inputSchema());
		assertThat(input.valid()).as(input.errorMessage()).isTrue();
		ValidationResponse output = validator.validateSchema(tool.tool().outputSchema());
		assertThat(output.valid()).as(output.errorMessage()).isTrue();
	}

	/**
	 * The SDK validates the call arguments against the input schema before the tool is invoked
	 * ({@code McpAsyncServer} with {@code validateToolInputs}, on by default). Run that same
	 * validation here, so a schema change that loosens a constraint is noticed.
	 */
	@Test
	public void testSdkInputValidationEnforcesConstraints() {
		Tool definition = tool.tool();
		DefaultJsonSchemaValidator validator = new DefaultJsonSchemaValidator();

		assertThat(ToolInputValidator.validate(definition, Map.of("pageId", 1, "name", "x", "priority", 100), true,
				validator)).isNull();

		List<Map<String, Object>> invalid = List.of(Map.of(), Map.of("pageId", 0), Map.of("pageId", 1, "name", ""),
				Map.of("pageId", 1, "name", "x".repeat(256)), Map.of("pageId", 1, "fileName", "x".repeat(256)),
				Map.of("pageId", 1, "description", "x".repeat(4001)), Map.of("pageId", 1, "niceUrl", "x".repeat(501)),
				Map.of("pageId", 1, "priority", 0), Map.of("pageId", 1, "priority", 101),
				Map.of("pageId", 1, "templateId", 0), Map.of("pageId", 1, "idempotencyKey", "x".repeat(129)),
				Map.of("pageId", 1, "deriveFileName", "true"), Map.of("pageId", 1, "tags", Map.of()));
		for (Map<String, Object> arguments : invalid) {
			CallToolResult result = ToolInputValidator.validate(definition, arguments, true, validator);
			assertThat(result).as("validation of %s", arguments).isNotNull();
			assertThat(result.isError()).isTrue();
		}
	}

	@Test
	public void testRequiresAuthenticationByDefault() {
		assertThat(tool.requiresAuthentication()).isTrue();
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		CallToolResult result = tool.call(McpTransportContext.EMPTY,
				CallToolRequest.builder("update_page_properties").arguments(Map.of("pageId", 1, "name", "x")).build());

		assertThat(result.isError()).isTrue();
		assertThat(((TextContent) result.content().get(0)).text()).contains("authenticated CMS session");
	}

	@Test
	public void testRejectsCallWithEmptyCredentialsInContext() {
		McpTransportContext context = McpTransportContext
				.create(Map.of(McpRequestCredentials.CONTEXT_KEY, McpRequestCredentials.EMPTY));

		CallToolResult result = tool.call(context,
				CallToolRequest.builder("update_page_properties").arguments(Map.of("pageId", 1)).build());

		assertThat(result.isError()).isTrue();
	}

	/**
	 * No transaction or session exists here, so any attempt to load the page would fail with a
	 * different error. Getting the nodeId message (even with an otherwise invalid pageId) shows the
	 * call is rejected before anything else happens.
	 */
	@Test
	public void testRejectsNodeIdBeforeTouchingTheCms() {
		assertThatThrownBy(() -> tool.invoke(Map.of("pageId", 1, "nodeId", 2), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(UpdatePagePropertiesTool.NODE_ID_NOT_SUPPORTED);
		assertThatThrownBy(() -> tool.invoke(Map.of("pageId", -5, "nodeId", 2), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(UpdatePagePropertiesTool.NODE_ID_NOT_SUPPORTED);
	}

	@Test
	public void testInvalidArgumentsAreRejectedBeforeTouchingTheCms() {
		assertThatThrownBy(() -> tool.invoke(Map.of("pageId", 1, "priority", 101), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("priority");
	}

	@Test
	public void testUpdateRequestDefaults() {
		UpdateRequest request = UpdateRequest.of(Map.of("pageId", 7));

		assertThat(request).isEqualTo(new UpdateRequest(7, null, null, null, null, null, null, false, true, null));
	}

	@Test
	public void testUpdateRequestAllFields() {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("pageId", 7L);
		arguments.put("name", "Name");
		arguments.put("fileName", "file.html");
		arguments.put("description", "");
		arguments.put("niceUrl", "/nice");
		arguments.put("priority", 100);
		arguments.put("templateId", 3);
		arguments.put("deriveFileName", true);
		arguments.put("failOnDuplicate", false);
		arguments.put("idempotencyKey", "key");

		assertThat(UpdateRequest.of(arguments)).isEqualTo(
				new UpdateRequest(7, "Name", "file.html", "", "/nice", 100, 3, true, false, "key"));
	}

	@Test
	public void testUpdateRequestRejectsInvalidArguments() {
		assertInvalid(Map.of(), "pageId");
		assertInvalid(Map.of("pageId", 0), "pageId");
		assertInvalid(Map.of("pageId", 1.5), "pageId");
		assertInvalid(Map.of("pageId", "1"), "pageId");
		assertInvalid(Map.of("pageId", 1, "name", ""), "name");
		assertInvalid(Map.of("pageId", 1, "name", "x".repeat(256)), "name");
		assertInvalid(Map.of("pageId", 1, "name", 5), "name");
		assertInvalid(Map.of("pageId", 1, "fileName", "x".repeat(256)), "fileName");
		assertInvalid(Map.of("pageId", 1, "description", "x".repeat(4001)), "description");
		assertInvalid(Map.of("pageId", 1, "niceUrl", "x".repeat(501)), "niceUrl");
		assertInvalid(Map.of("pageId", 1, "priority", 0), "priority");
		assertInvalid(Map.of("pageId", 1, "priority", 101), "priority");
		assertInvalid(Map.of("pageId", 1, "templateId", 0), "templateId");
		assertInvalid(Map.of("pageId", 1, "deriveFileName", "true"), "deriveFileName");
		assertInvalid(Map.of("pageId", 1, "failOnDuplicate", 1), "failOnDuplicate");
		assertInvalid(Map.of("pageId", 1, "idempotencyKey", "x".repeat(129)), "idempotencyKey");

		Map<String, Object> nullName = new HashMap<>(Map.of("pageId", 1));
		nullName.put("name", null);
		assertInvalid(nullName, "name");
	}

	@Test
	public void testUpdateRequestBoundaryValues() {
		UpdateRequest request = UpdateRequest.of(Map.of("pageId", 1, "name", "x".repeat(255), "fileName", "",
				"description", "x".repeat(4000), "niceUrl", "x".repeat(500), "priority", 1, "idempotencyKey",
				"x".repeat(128)));

		assertThat(request.name()).hasSize(255);
		assertThat(request.priority()).isEqualTo(1);
	}

	@Test
	public void testToRestPageSetsOnlySuppliedFields() {
		Page restPage = UpdateRequest.of(Map.of("pageId", 7, "name", "New name")).toRestPage();

		assertThat(restPage.getName()).isEqualTo("New name");
		assertThat(restPage.getFileName()).isNull();
		assertThat(restPage.getDescription()).isNull();
		assertThat(restPage.getNiceUrl()).isNull();
		assertThat(restPage.getPriority()).isNull();
		assertThat(restPage.getTemplateId()).isNull();
		// must not be set, ModelBuilder#getPage(restPage, false) would apply them
		assertThat(restPage.getTags()).isNull();
		assertThat(restPage.getTranslationStatus()).isNull();
		assertThat(restPage.getLanguage()).isNull();
		assertThat(restPage.getAlternateUrls()).isNull();
	}

	@Test
	public void testDiffNothingChanged() {
		Page before = page("Name", "name.html", "Description", "/nice", 50, 3);

		assertThat(CHANGED_FIELDS.snapshot(before).diff(page("Name", "name.html", "Description", "/nice", 50, 3)))
				.isEmpty();
	}

	/**
	 * Decision 3: a field resubmitted with its unchanged value is not "changed". The snapshot is
	 * compared to the page after the save, independent of what was in the request.
	 */
	@Test
	public void testDiffUnchangedResubmittedValueIsNotChanged() {
		ChangedFields.Snapshot<Page> before = CHANGED_FIELDS.snapshot(page("Name", "name.html", null, null, 50, 3));

		// the request contained name = "Name" again, so the page after the save is equal
		Page after = page("Name", "name.html", null, null, 50, 3);

		assertThat(before.diff(after)).isEmpty();
	}

	@Test
	public void testDiffEachFieldIndividually() {
		ChangedFields.Snapshot<Page> before = CHANGED_FIELDS.snapshot(
				page("Name", "name.html", "Description", "/nice", 50, 3));

		assertThat(before.diff(page("Other", "name.html", "Description", "/nice", 50, 3))).containsExactly("name");
		assertThat(before.diff(page("Name", "other.html", "Description", "/nice", 50, 3))).containsExactly("fileName");
		assertThat(before.diff(page("Name", "name.html", "Other", "/nice", 50, 3))).containsExactly("description");
		assertThat(before.diff(page("Name", "name.html", "Description", "/other", 50, 3))).containsExactly("niceUrl");
		assertThat(before.diff(page("Name", "name.html", "Description", "/nice", 51, 3))).containsExactly("priority");
		assertThat(before.diff(page("Name", "name.html", "Description", "/nice", 50, 4))).containsExactly("templateId");
	}

	@Test
	public void testDiffAllFieldsInFixedOrder() {
		ChangedFields.Snapshot<Page> before = CHANGED_FIELDS.snapshot(
				page("Name", "name.html", "Description", "/nice", 50, 3));

		assertThat(before.diff(page("A", "a.html", "B", "/c", 1, 9))).containsExactly("name", "fileName",
				"description", "niceUrl", "priority", "templateId");
	}

	/**
	 * A filename derived by save() (deriveFileName) or an extension appended by save() shows up
	 * as changed, although the request contained no fileName (or a different one).
	 */
	@Test
	public void testDiffReportsFileNameChangedBySave() {
		ChangedFields.Snapshot<Page> before = CHANGED_FIELDS.snapshot(page("Old", "old.html", null, null, 1, 3));

		assertThat(before.diff(page("New", "new.html", null, null, 1, 3))).containsExactly("name", "fileName");
	}

	@Test
	public void testDiffNullTransitions() {
		Page withValues = page("Name", "name.html", "Description", "/nice", 50, 3);
		Page withNulls = page("Name", "name.html", null, null, null, null);

		assertThat(CHANGED_FIELDS.snapshot(withValues).diff(withNulls)).containsExactly("description", "niceUrl",
				"priority", "templateId");
		assertThat(CHANGED_FIELDS.snapshot(withNulls).diff(withValues)).containsExactly("description", "niceUrl",
				"priority", "templateId");
		assertThat(CHANGED_FIELDS.snapshot(withNulls).diff(page("Name", "name.html", null, null, null, null)))
				.isEmpty();
	}

	@Test
	public void testDiffEmptyStringIsNotNull() {
		ChangedFields.Snapshot<Page> before = CHANGED_FIELDS.snapshot(
				page("Name", "name.html", "Description", null, 1, 3));

		assertThat(before.diff(page("Name", "name.html", "", null, 1, 3))).containsExactly("description");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPageInfoMapping() throws Exception {
		Page page = fullPage();

		Map<String, Object> map = MAPPER.convertValue(PageInfo.of(page), new TypeReference<Map<String, Object>>() {
		});

		assertThat(map).doesNotContainKeys("tags", "lockedBy", "lockedSince", "published", "publisher");
		assertThat((Map<String, Object>) map.get("ref")).containsEntry("type", "page").containsEntry("id", 7)
				.containsEntry("globalId", "A547.7").containsEntry("nodeId", 2).containsEntry("name", "Home")
				.containsEntry("path", "/Node/News/").containsEntry("language", "en")
				.containsEntry("url", "/preview/home.html");
		assertThat(map).containsEntry("fileName", "home.html").containsEntry("templateId", 3)
				.containsEntry("folderId", 5).containsEntry("priority", 10).containsEntry("online", false)
				.containsEntry("locked", false).containsEntry("created", 1000).containsEntry("edited", 2000);
		assertThat((Map<String, Object>) map.get("creator")).containsOnly(Map.entry("id", 11),
				Map.entry("login", "creator"));

		List<Map<String, Object>> versions = (List<Map<String, Object>>) map.get("versions");
		assertThat(versions).hasSize(1);
		assertThat(versions.get(0)).containsEntry("number", "0.1").containsEntry("timestamp", 2000);
		assertThat((Map<String, Object>) versions.get(0).get("editor")).containsEntry("login", "editor");

		Map<String, Object> languageVariants = (Map<String, Object>) map.get("languageVariants");
		assertThat(languageVariants).containsOnlyKeys("en", "de");
		assertThat((Map<String, Object>) languageVariants.get("de")).containsEntry("id", 8)
				.containsEntry("language", "de").containsEntry("url", "https://www.example.com/News/home.de.html");

		assertThat((Map<String, Object>) map.get("translationStatus")).containsEntry("pageId", 8)
				.containsEntry("inSync", false).containsEntry("version", "0.1")
				.containsEntry("latestVersion", Map.of("version", "0.2", "versionTimestamp", 3000));
	}

	/**
	 * The SDK validates the structured result against the output schema and turns a mismatch into
	 * an error, so check a fully populated and a minimal result against it.
	 */
	@Test
	public void testResultConformsToOutputSchema() {
		DefaultJsonSchemaValidator validator = new DefaultJsonSchemaValidator();
		Map<String, Object> outputSchema = tool.tool().outputSchema();

		for (Page page : List.of(fullPage(), minimalPage())) {
			UpdatePagePropertiesResult result = new UpdatePagePropertiesResult(PageInfo.of(page),
					List.of("name"));
			var validation = validator.validate(outputSchema, MAPPER.convertValue(result, Map.class));
			assertThat(validation.valid()).as(validation.errorMessage()).isTrue();
		}
	}

	@Test
	public void testRefWithoutFolderHasNoNodeId() {
		ObjectRef ref = PageInfo.of(minimalPage()).ref();

		assertThat(ref.nodeId()).isNull();
		assertThat(ref.id()).isEqualTo(7);
	}

	private void assertInvalid(Map<String, Object> arguments, String argument) {
		assertThatThrownBy(() -> UpdateRequest.of(arguments)).as("arguments %s", arguments)
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'" + argument + "'");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Map<String, Object>> inputProperties(Tool definition) {
		return (Map<String, Map<String, Object>>) definition.inputSchema().get("properties");
	}

	private static Page page(String name, String fileName, String description, String niceUrl, Integer priority,
			Integer templateId) {
		Page page = new Page();
		page.setName(name);
		page.setFileName(fileName);
		page.setDescription(description);
		page.setNiceUrl(niceUrl);
		page.setPriority(priority);
		page.setTemplateId(templateId);
		return page;
	}

	private static Page minimalPage() {
		Page page = page("Home", "home.html", null, null, 1, 3);
		page.setId(7);
		return page;
	}

	private static Page fullPage() {
		Page page = page("Home", "home.html", "Description", null, 10, 3);
		page.setId(7);
		page.setGlobalId("A547.7");
		page.setPath("/Node/News/");
		page.setLanguage("en");
		page.setLanguageName("English");
		page.setUrl("/preview/home.html");
		page.setLiveUrl("https://www.example.com/News/home.html");
		page.setPublishPath("/News/");
		page.setLockedSince(-1);
		page.setCreator(new User().setId(11).setLogin("creator"));
		page.setCdate(1000);
		page.setEditor(new User().setId(12).setLogin("editor"));
		page.setEdate(2000);
		page.setTags(Map.of("content", new Tag()));

		// setFolder() also sets the folderId from the folder
		Folder folder = new Folder();
		folder.setId(5);
		folder.setNodeId(2);
		page.setFolder(folder);

		PageVersion version = new PageVersion();
		version.setNumber("0.1").setTimestamp(2000).setEditor(new User().setId(12).setLogin("editor"));
		page.setVersions(List.of(version));
		page.setCurrentVersion(version);

		Page variant = page("Startseite", "home.de.html", null, null, 1, 3);
		variant.setId(8);
		variant.setLanguage("de");
		variant.setLiveUrl("https://www.example.com/News/home.de.html");
		variant.setFolder(folder);
		Map<Object, Page> languageVariants = new LinkedHashMap<>();
		languageVariants.put(1, page);
		languageVariants.put(2, variant);
		page.setLanguageVariants(languageVariants);

		TranslationStatus status = new TranslationStatus();
		status.setPageId(8);
		status.setName("Startseite");
		status.setLanguage("de");
		status.setInSync(false);
		status.setVersion("0.1");
		status.setVersionTimestamp(2000);
		TranslationStatus.Latest latest = new TranslationStatus.Latest();
		latest.setVersion("0.2");
		latest.setVersionTimestamp(3000);
		status.setLatestVersion(latest);
		page.setTranslationStatus(status);
		return page;
	}
}
