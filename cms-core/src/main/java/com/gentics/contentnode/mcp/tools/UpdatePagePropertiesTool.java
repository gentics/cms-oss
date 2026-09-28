package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.mcp.util.ChangedFields;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.lib.log.NodeLogger;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Changes the metadata of a single page (name, filename, description, nice URL, priority,
 * template), without touching its content/tags.
 */
public class UpdatePagePropertiesTool extends AbstractMcpTool {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(UpdatePagePropertiesTool.class);

	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_NAME = "name";

	static final String ARG_FILE_NAME = "fileName";

	static final String ARG_DESCRIPTION = "description";

	static final String ARG_NICE_URL = "niceUrl";

	static final String ARG_PRIORITY = "priority";

	static final String ARG_TEMPLATE_ID = "templateId";

	static final String ARG_DERIVE_FILE_NAME = "deriveFileName";

	static final String ARG_FAIL_ON_DUPLICATE = "failOnDuplicate";

	static final String ARG_IDEMPOTENCY_KEY = "idempotencyKey";

	/**
	 * Maximum length of {@link #ARG_NAME}
	 */
	public static final int MAX_NAME_LENGTH = 255;

	/**
	 * Maximum length of {@link #ARG_FILE_NAME}
	 */
	public static final int MAX_FILE_NAME_LENGTH = 255;

	/**
	 * Maximum length of {@link #ARG_DESCRIPTION}
	 */
	public static final int MAX_DESCRIPTION_LENGTH = 4000;

	/**
	 * Maximum length of {@link #ARG_NICE_URL}
	 */
	public static final int MAX_NICE_URL_LENGTH = 500;

	/**
	 * Minimum of {@link #ARG_PRIORITY}
	 */
	public static final int MIN_PRIORITY = 1;

	/**
	 * Maximum of {@link #ARG_PRIORITY}
	 */
	public static final int MAX_PRIORITY = 100;

	/**
	 * Maximum length of {@link #ARG_IDEMPOTENCY_KEY}
	 */
	public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

	/**
	 * Error message for a call with {@link #ARG_NODE_ID}
	 */
	static final String NODE_ID_NOT_SUPPORTED = "update_page_properties does not yet support nodeId "
			+ "(multichannelling); call it without nodeId, or wait for that support to be added.";

	/**
	 * The fields this tool can change, named like the arguments that change them, for computing
	 * {@code changedFields}
	 */
	static final ChangedFields<com.gentics.contentnode.rest.model.Page> CHANGED_FIELDS = ChangedFields
			.<com.gentics.contentnode.rest.model.Page>builder()
			.field(ARG_NAME, com.gentics.contentnode.rest.model.Page::getName)
			.field(ARG_FILE_NAME, com.gentics.contentnode.rest.model.Page::getFileName)
			.field(ARG_DESCRIPTION, com.gentics.contentnode.rest.model.Page::getDescription)
			.field(ARG_NICE_URL, com.gentics.contentnode.rest.model.Page::getNiceUrl)
			.field(ARG_PRIORITY, com.gentics.contentnode.rest.model.Page::getPriority)
			.field(ARG_TEMPLATE_ID, com.gentics.contentnode.rest.model.Page::getTemplateId)
			.build();

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PAGE_ID, schema("integer", "ID of the page to update.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer",
				"ID of the node/channel to update the page in. Not supported yet: calls that set it are rejected.",
				"minimum", 1));
		properties.put(ARG_NAME, schema("string", "New name of the page.", "minLength", 1, "maxLength",
				MAX_NAME_LENGTH));
		properties.put(ARG_FILE_NAME, schema("string",
				"New filename of the page. The template's file extension is appended if missing.", "maxLength",
				MAX_FILE_NAME_LENGTH));
		properties.put(ARG_DESCRIPTION, schema("string", "New description of the page. Empty string clears it.",
				"maxLength", MAX_DESCRIPTION_LENGTH));
		properties.put(ARG_NICE_URL, schema("string",
				"New nice URL of the page. Empty string clears it. Ignored if the nice URL feature is not active.",
				"maxLength", MAX_NICE_URL_LENGTH));
		properties.put(ARG_PRIORITY, schema("integer", "New priority of the page.", "minimum", MIN_PRIORITY,
				"maximum", MAX_PRIORITY));
		properties.put(ARG_TEMPLATE_ID, schema("integer", "ID of the new template of the page.", "minimum", 1));
		properties.put(ARG_DERIVE_FILE_NAME, schema("boolean",
				"Derive the filename from the page name. Only applies if 'fileName' is not given.", "default",
				false));
		properties.put(ARG_FAIL_ON_DUPLICATE, schema("boolean",
				"Fail if the new name or filename is already used by another object, instead of making it unique.",
				"default", true));
		properties.put(ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name("update_page_properties").title("Update Page Properties")
				.description("Change the metadata of a single page: name, filename, description, nice URL, "
						+ "priority or template. Only the given fields are changed. Returns the page after "
						+ "saving and 'changedFields', the fields whose value actually changed. Use get_template "
						+ "to find a valid templateId. Do NOT use this tool to change content, that is "
						+ "update_page_tags.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_PAGE_ID)).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		if (arguments.containsKey(ARG_NODE_ID)) {
			// rejected rather than ignored: silently ignoring an explicit channel on a write could edit
			// the wrong page variant
			throw new IllegalArgumentException(NODE_ID_NOT_SUPPORTED);
		}

		// validate all arguments before the page is loaded (and locked)
		UpdateRequest update = UpdateRequest.of(arguments);
		String id = Integer.toString(update.pageId());

		if (update.idempotencyKey() != null) {
			logger.info(String.format("update_page_properties for page %s with idempotencyKey '%s'", id,
					update.idempotencyKey()));
		}

		PageResourceImpl pageResource = new PageResourceImpl();

		// step 1: load for update, which checks the edit permission and locks the page (committed)
		com.gentics.contentnode.rest.model.Page loaded = pageResource
				.load(id, true, false, false, false, false, false, false, false, false, false, null, null).getPage();
		ChangedFields.Snapshot<com.gentics.contentnode.rest.model.Page> before = CHANGED_FIELDS.snapshot(loaded);

		// step 2: save only the supplied fields, and unlock. save() reports e.g. duplicate names as
		// INVALIDDATA in the response instead of throwing, and leaves the page locked then
		saveOrReleaseLock(Page.class, id, "Page %s was not saved".formatted(id), () -> {
			PageSaveRequest saveRequest = new PageSaveRequest(update.toRestPage());
			saveRequest.setUnlock(true);
			saveRequest.setDeriveFileName(update.deriveFileName());
			saveRequest.setFailOnDuplicate(update.failOnDuplicate());
			return pageResource.save(id, saveRequest);
		});

		// step 3: reload, save() does not return the page
		com.gentics.contentnode.rest.model.Page after = pageResource
				.load(id, false, false, true, true, false, false, true, true, false, false, null, null).getPage();

		return new UpdatePagePropertiesResult(PageInfo.of(after), before.diff(after));
	}

	/**
	 * Validated tool arguments. The SDK already validates the arguments against the input schema
	 * before the tool is invoked; these checks repeat that, so the tool is safe on its own (e.g.
	 * when {@link #call} is invoked directly), and reject invalid values instead of clamping them.
	 * @param pageId page ID
	 * @param name new name, null if not supplied
	 * @param fileName new filename, null if not supplied
	 * @param description new description, null if not supplied
	 * @param niceUrl new nice URL, null if not supplied
	 * @param priority new priority, null if not supplied
	 * @param templateId new template ID, null if not supplied
	 * @param deriveFileName whether to derive the filename
	 * @param failOnDuplicate whether to fail on duplicate names/filenames
	 * @param idempotencyKey idempotency key, null if not supplied
	 */
	record UpdateRequest(int pageId, String name, String fileName, String description, String niceUrl,
			Integer priority, Integer templateId, boolean deriveFileName, boolean failOnDuplicate,
			String idempotencyKey) {

		/**
		 * Validate the raw arguments
		 * @param arguments raw arguments
		 * @return validated request
		 * @throws IllegalArgumentException for a missing or invalid argument
		 */
		static UpdateRequest of(Map<String, Object> arguments) {
			if (arguments.get(ARG_PAGE_ID) == null) {
				throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_PAGE_ID));
			}
			return new UpdateRequest(intArg(arguments, ARG_PAGE_ID, 1, Integer.MAX_VALUE),
					stringArg(arguments, ARG_NAME, 1, MAX_NAME_LENGTH),
					stringArg(arguments, ARG_FILE_NAME, 0, MAX_FILE_NAME_LENGTH),
					stringArg(arguments, ARG_DESCRIPTION, 0, MAX_DESCRIPTION_LENGTH),
					stringArg(arguments, ARG_NICE_URL, 0, MAX_NICE_URL_LENGTH),
					intArg(arguments, ARG_PRIORITY, MIN_PRIORITY, MAX_PRIORITY),
					intArg(arguments, ARG_TEMPLATE_ID, 1, Integer.MAX_VALUE),
					booleanArg(arguments, ARG_DERIVE_FILE_NAME, false),
					booleanArg(arguments, ARG_FAIL_ON_DUPLICATE, true),
					stringArg(arguments, ARG_IDEMPOTENCY_KEY, 0, MAX_IDEMPOTENCY_KEY_LENGTH));
		}

		/**
		 * Build the REST page for the save request, with only the supplied fields set
		 * ({@code ModelBuilder#getPage(restPage, false)} leaves every null field unchanged)
		 * @return REST page
		 */
		com.gentics.contentnode.rest.model.Page toRestPage() {
			com.gentics.contentnode.rest.model.Page restPage = new com.gentics.contentnode.rest.model.Page();
			restPage.setName(name);
			restPage.setFileName(fileName);
			restPage.setDescription(description);
			restPage.setNiceUrl(niceUrl);
			restPage.setPriority(priority);
			restPage.setTemplateId(templateId);
			return restPage;
		}
	}

	/**
	 * Result of the tool
	 * @param page page after the update
	 * @param changedFields names of the fields whose value actually changed
	 */
	record UpdatePagePropertiesResult(PageInfo page, List<String> changedFields) {
	}

	/**
	 * Build the output schema. The SDK validates the structured result against it, which is why
	 * the result records omit null values ({@code "type": "integer"} does not accept {@code null}).
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("page", PageInfo.jsonSchema("The page after the update."));
		properties.put("changedFields", schema("array", "Fields whose value actually changed.", "items",
				schema("string", null)));

		Map<String, Object> outputSchema = new LinkedHashMap<>();
		outputSchema.put("type", "object");
		outputSchema.put("properties", properties);
		outputSchema.put("required", List.of("page", "changedFields"));
		return outputSchema;
	}
}
