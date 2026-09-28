package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.PageVersion;
import com.gentics.contentnode.rest.model.TranslationStatus;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
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
		MutableFieldsSnapshot before = MutableFieldsSnapshot.of(loaded);

		// step 2: save only the supplied fields, and unlock
		boolean saved = false;
		try {
			PageSaveRequest saveRequest = new PageSaveRequest(update.toRestPage());
			saveRequest.setUnlock(true);
			saveRequest.setDeriveFileName(update.deriveFileName());
			saveRequest.setFailOnDuplicate(update.failOnDuplicate());

			GenericResponse response = pageResource.save(id, saveRequest);
			// save() reports e.g. duplicate names as INVALIDDATA in the response instead of throwing
			if (response.getResponseInfo() == null
					|| response.getResponseInfo().getResponseCode() != ResponseCode.OK) {
				throw new IllegalArgumentException(errorMessage(id, response));
			}
			saved = true;
		} finally {
			if (!saved) {
				releaseLock(id);
			}
		}

		// step 3: reload, save() does not return the page
		com.gentics.contentnode.rest.model.Page after = pageResource
				.load(id, false, false, true, true, false, false, true, true, false, false, null, null).getPage();

		return new UpdatePagePropertiesResult(PageProperties.of(after), before.diff(after));
	}

	/**
	 * Release the calling user's lock on the page, after a failed save. Only unlocks: unlike
	 * {@link PageResourceImpl#cancel}, this does not restore the latest page version. The unlock
	 * statement is restricted to locks held by the current user, so it never releases someone
	 * else's lock. Errors are logged, not thrown, so that they do not replace the original failure.
	 * @param id page ID
	 */
	static void releaseLock(String id) {
		try (Trx trx = ContentNodeHelper.trx()) {
			Page page = trx.getTransaction().getObject(Page.class, id);
			if (page != null) {
				page.unlock();
			}
			trx.success();
		} catch (Exception e) {
			logger.error(String.format("Error while releasing the lock on page %s after a failed update", id), e);
		}
	}

	/**
	 * Build the error message for a rejected save
	 * @param id page ID
	 * @param response response of the save
	 * @return message
	 */
	static String errorMessage(String id, GenericResponse response) {
		String messages = response.getMessages() == null ? ""
				: response.getMessages().stream().map(Message::getMessage).filter(Objects::nonNull)
						.collect(Collectors.joining(" "));
		if (!messages.isBlank()) {
			return "Page %s was not saved: %s".formatted(id, messages);
		}
		String info = response.getResponseInfo() != null ? response.getResponseInfo().getResponseMessage() : null;
		return "Page %s was not saved: %s".formatted(id, info != null ? info : "unknown error");
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

		/**
		 * Get an optional string argument
		 * @param arguments arguments
		 * @param name argument name
		 * @param minLength minimum length
		 * @param maxLength maximum length
		 * @return value, or null if not supplied
		 */
		private static String stringArg(Map<String, Object> arguments, String name, int minLength, int maxLength) {
			if (!arguments.containsKey(name)) {
				return null;
			}
			if (!(arguments.get(name) instanceof String value)) {
				throw new IllegalArgumentException("Argument '%s' must be a string".formatted(name));
			}
			if (value.length() < minLength) {
				throw new IllegalArgumentException(
						"Argument '%s' must be at least %d characters long".formatted(name, minLength));
			}
			if (value.length() > maxLength) {
				throw new IllegalArgumentException(
						"Argument '%s' must not be longer than %d characters".formatted(name, maxLength));
			}
			return value;
		}

		/**
		 * Get an optional integer argument
		 * @param arguments arguments
		 * @param name argument name
		 * @param min minimum value
		 * @param max maximum value
		 * @return value, or null if not supplied
		 */
		private static Integer intArg(Map<String, Object> arguments, String name, int min, int max) {
			if (!arguments.containsKey(name)) {
				return null;
			}
			Object raw = arguments.get(name);
			if (!(raw instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())) {
				throw new IllegalArgumentException("Argument '%s' must be an integer".formatted(name));
			}
			long value = number.longValue();
			if (value < min || value > max) {
				throw new IllegalArgumentException(
						"Argument '%s' must be between %d and %d".formatted(name, min, max));
			}
			return (int) value;
		}

		/**
		 * Get an optional boolean argument
		 * @param arguments arguments
		 * @param name argument name
		 * @param defaultValue value if not supplied
		 * @return value
		 */
		private static boolean booleanArg(Map<String, Object> arguments, String name, boolean defaultValue) {
			if (!arguments.containsKey(name)) {
				return defaultValue;
			}
			if (!(arguments.get(name) instanceof Boolean value)) {
				throw new IllegalArgumentException("Argument '%s' must be a boolean".formatted(name));
			}
			return value;
		}
	}

	/**
	 * Snapshot of the fields this tool can change, for computing {@code changedFields}
	 * @param name name
	 * @param fileName filename
	 * @param description description
	 * @param niceUrl nice URL
	 * @param priority priority
	 * @param templateId template ID
	 */
	record MutableFieldsSnapshot(String name, String fileName, String description, String niceUrl, Integer priority,
			Integer templateId) {

		/**
		 * Take the snapshot of the given page
		 * @param page REST page
		 * @return snapshot
		 */
		static MutableFieldsSnapshot of(com.gentics.contentnode.rest.model.Page page) {
			return new MutableFieldsSnapshot(page.getName(), page.getFileName(), page.getDescription(),
					page.getNiceUrl(), page.getPriority(), page.getTemplateId());
		}

		/**
		 * Get the names of the fields whose value in the given page differs from this snapshot
		 * @param after REST page after the update
		 * @return changed field names, in a fixed order
		 */
		List<String> diff(com.gentics.contentnode.rest.model.Page after) {
			MutableFieldsSnapshot other = of(after);
			List<String> changed = new ArrayList<>();
			addIfChanged(changed, ARG_NAME, name, other.name);
			addIfChanged(changed, ARG_FILE_NAME, fileName, other.fileName);
			addIfChanged(changed, ARG_DESCRIPTION, description, other.description);
			addIfChanged(changed, ARG_NICE_URL, niceUrl, other.niceUrl);
			addIfChanged(changed, ARG_PRIORITY, priority, other.priority);
			addIfChanged(changed, ARG_TEMPLATE_ID, templateId, other.templateId);
			return changed;
		}

		private static void addIfChanged(List<String> changed, String field, Object before, Object after) {
			if (!Objects.equals(before, after)) {
				changed.add(field);
			}
		}
	}

	/**
	 * Result of the tool
	 * @param page page after the update
	 * @param changedFields names of the fields whose value actually changed
	 */
	record UpdatePagePropertiesResult(PageProperties page, List<String> changedFields) {
	}

	/**
	 * Page properties returned by the tool. Deliberately has no tags (content is out of scope for
	 * this tool). Timestamps are unix timestamps; timestamps that are not set (e.g. {@code published}
	 * for a page never published, {@code lockedSince} for an unlocked page) are omitted.
	 * @param ref reference to the page
	 * @param fileName filename
	 * @param description description
	 * @param niceUrl nice URL
	 * @param templateId template ID
	 * @param folderId folder ID
	 * @param language language code
	 * @param languageName language name
	 * @param priority priority
	 * @param url preview URL
	 * @param liveUrl live URL
	 * @param publishPath publish path
	 * @param online whether the page is online
	 * @param modified whether the page is modified since it was last published
	 * @param queued whether the page is queued for publishing
	 * @param planned whether the page has planned time management
	 * @param locked whether the page is locked
	 * @param lockedBy user who locked the page
	 * @param lockedSince timestamp since when the page is locked
	 * @param creator creator
	 * @param created creation timestamp
	 * @param editor last editor
	 * @param edited last edit timestamp
	 * @param publisher last publisher
	 * @param published last publish timestamp
	 * @param versions page versions, newest first
	 * @param currentVersion current version
	 * @param publishedVersion published version
	 * @param languageVariants language variants by language code
	 * @param translationStatus translation status
	 */
	@JsonInclude(Include.NON_NULL)
	record PageProperties(ObjectRef ref, String fileName, String description, String niceUrl, Integer templateId,
			Integer folderId, String language, String languageName, Integer priority, String url, String liveUrl,
			String publishPath, boolean online, boolean modified, boolean queued, boolean planned, boolean locked,
			UserRef lockedBy, Integer lockedSince, UserRef creator, Integer created, UserRef editor, Integer edited,
			UserRef publisher, Integer published, List<VersionInfo> versions, VersionInfo currentVersion,
			VersionInfo publishedVersion, Map<String, ObjectRef> languageVariants,
			TranslationStatusInfo translationStatus) {

		/**
		 * Map the REST page
		 * @param page REST page, loaded with folder, language variants, translation status and versions
		 * @return page properties
		 */
		static PageProperties of(com.gentics.contentnode.rest.model.Page page) {
			List<VersionInfo> versions = page.getVersions() == null ? null
					: page.getVersions().stream().map(VersionInfo::of).toList();

			Map<String, ObjectRef> languageVariants = null;
			if (page.getLanguageVariants() != null) {
				languageVariants = new LinkedHashMap<>();
				for (Map.Entry<Object, com.gentics.contentnode.rest.model.Page> entry : page.getLanguageVariants()
						.entrySet()) {
					com.gentics.contentnode.rest.model.Page variant = entry.getValue();
					String key = variant.getLanguage() != null ? variant.getLanguage() : String.valueOf(entry.getKey());
					languageVariants.put(key, ObjectRef.forPage(variant, nodeId(variant)));
				}
			}

			return new PageProperties(ObjectRef.forPage(page, nodeId(page)), page.getFileName(),
					page.getDescription(), page.getNiceUrl(), page.getTemplateId(), page.getFolderId(),
					page.getLanguage(), page.getLanguageName(), page.getPriority(), page.getUrl(), page.getLiveUrl(),
					page.getPublishPath(), page.isOnline(), page.isModified(), page.isQueued(), page.isPlanned(),
					page.isLocked(), UserRef.of(page.getLockedBy()), timestamp(page.getLockedSince()),
					UserRef.of(page.getCreator()), timestamp(page.getCdate()), UserRef.of(page.getEditor()),
					timestamp(page.getEdate()), UserRef.of(page.getPublisher()), timestamp(page.getPdate()), versions,
					VersionInfo.of(page.getCurrentVersion()), VersionInfo.of(page.getPublishedVersion()),
					languageVariants, TranslationStatusInfo.of(page.getTranslationStatus()));
		}

		/**
		 * Get the ID of the node the page belongs to, from its folder (if loaded)
		 * @param page REST page
		 * @return node ID or null
		 */
		private static Integer nodeId(com.gentics.contentnode.rest.model.Page page) {
			return page.getFolder() != null ? page.getFolder().getNodeId() : null;
		}

		/**
		 * Map a timestamp that is 0 or negative when not set
		 * @param timestamp timestamp
		 * @return timestamp or null
		 */
		private static Integer timestamp(int timestamp) {
			return timestamp > 0 ? timestamp : null;
		}
	}

	/**
	 * Reference to a user
	 * @param id user ID
	 * @param login login name
	 */
	@JsonInclude(Include.NON_NULL)
	record UserRef(Integer id, String login) {
		/**
		 * Map the REST user
		 * @param user REST user, may be null
		 * @return user ref or null
		 */
		static UserRef of(User user) {
			return user != null ? new UserRef(user.getId(), user.getLogin()) : null;
		}
	}

	/**
	 * A page version
	 * @param number version number
	 * @param timestamp version timestamp
	 * @param editor editor of the version
	 */
	@JsonInclude(Include.NON_NULL)
	record VersionInfo(String number, Integer timestamp, UserRef editor) {
		/**
		 * Map the REST page version
		 * @param version REST version, may be null
		 * @return version info or null
		 */
		static VersionInfo of(PageVersion version) {
			return version != null ? new VersionInfo(version.getNumber(), version.getTimestamp(),
					UserRef.of(version.getEditor())) : null;
		}
	}

	/**
	 * Translation status of the page. For a page that is not synchronized with another language
	 * variant, only {@code inSync} (true) is set.
	 * @param pageId ID of the page this page is synchronized with
	 * @param name name of that page
	 * @param language language code of that page
	 * @param inSync whether this page is in sync with that page
	 * @param version version of that page this page is synchronized with
	 * @param versionTimestamp timestamp of that version
	 * @param latestVersion latest version of that page
	 */
	@JsonInclude(Include.NON_NULL)
	record TranslationStatusInfo(Integer pageId, String name, String language, boolean inSync, String version,
			Integer versionTimestamp, LatestVersionInfo latestVersion) {
		/**
		 * Map the REST translation status
		 * @param status REST translation status, may be null
		 * @return translation status info or null
		 */
		static TranslationStatusInfo of(TranslationStatus status) {
			if (status == null) {
				return null;
			}
			TranslationStatus.Latest latest = status.getLatestVersion();
			return new TranslationStatusInfo(status.getPageId(), status.getName(), status.getLanguage(),
					status.isInSync(), status.getVersion(), status.getVersionTimestamp(),
					latest != null ? new LatestVersionInfo(latest.getVersion(), latest.getVersionTimestamp()) : null);
		}
	}

	/**
	 * Latest version of the page a page is synchronized with
	 * @param version version number
	 * @param versionTimestamp version timestamp
	 */
	@JsonInclude(Include.NON_NULL)
	record LatestVersionInfo(String version, Integer versionTimestamp) {
	}

	/**
	 * Build a property schema
	 * @param type JSON type
	 * @param description description
	 * @param keyValues additional constraint keywords and their values, alternating
	 * @return schema
	 */
	private static Map<String, Object> schema(String type, String description, Object... keyValues) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", type);
		if (description != null) {
			schema.put("description", description);
		}
		for (int i = 0; i + 1 < keyValues.length; i += 2) {
			schema.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
		}
		return schema;
	}

	/**
	 * Build the output schema. The SDK validates the structured result against it, which is why
	 * the result records omit null values ({@code "type": "integer"} does not accept {@code null}).
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> refProperties = new LinkedHashMap<>();
		refProperties.put("type", schema("string", null));
		refProperties.put("id", schema("integer", null));
		refProperties.put("globalId", schema("string", null));
		refProperties.put("nodeId", schema("integer", null));
		refProperties.put("name", schema("string", null));
		refProperties.put("path", schema("string", null));
		refProperties.put("language", schema("string", null));
		refProperties.put("niceUrl", schema("string", null));
		refProperties.put("url", schema("string", null));
		Map<String, Object> refSchema = schema("object", null, "properties", refProperties);

		Map<String, Object> userProperties = new LinkedHashMap<>();
		userProperties.put("id", schema("integer", null));
		userProperties.put("login", schema("string", null));
		Map<String, Object> userSchema = schema("object", null, "properties", userProperties);

		Map<String, Object> versionProperties = new LinkedHashMap<>();
		versionProperties.put("number", schema("string", null));
		versionProperties.put("timestamp", schema("integer", null));
		versionProperties.put("editor", userSchema);
		Map<String, Object> versionSchema = schema("object", null, "properties", versionProperties);

		Map<String, Object> latestProperties = new LinkedHashMap<>();
		latestProperties.put("version", schema("string", null));
		latestProperties.put("versionTimestamp", schema("integer", null));

		Map<String, Object> translationProperties = new LinkedHashMap<>();
		translationProperties.put("pageId", schema("integer", null));
		translationProperties.put("name", schema("string", null));
		translationProperties.put("language", schema("string", null));
		translationProperties.put("inSync", schema("boolean", null));
		translationProperties.put("version", schema("string", null));
		translationProperties.put("versionTimestamp", schema("integer", null));
		translationProperties.put("latestVersion", schema("object", null, "properties", latestProperties));

		Map<String, Object> pageProperties = new LinkedHashMap<>();
		pageProperties.put("ref", refSchema);
		pageProperties.put("fileName", schema("string", null));
		pageProperties.put("description", schema("string", null));
		pageProperties.put("niceUrl", schema("string", null));
		pageProperties.put("templateId", schema("integer", null));
		pageProperties.put("folderId", schema("integer", null));
		pageProperties.put("language", schema("string", null));
		pageProperties.put("languageName", schema("string", null));
		pageProperties.put("priority", schema("integer", null));
		pageProperties.put("url", schema("string", null));
		pageProperties.put("liveUrl", schema("string", null));
		pageProperties.put("publishPath", schema("string", null));
		pageProperties.put("online", schema("boolean", null));
		pageProperties.put("modified", schema("boolean", null));
		pageProperties.put("queued", schema("boolean", null));
		pageProperties.put("planned", schema("boolean", null));
		pageProperties.put("locked", schema("boolean", null));
		pageProperties.put("lockedBy", userSchema);
		pageProperties.put("lockedSince", schema("integer", null));
		pageProperties.put("creator", userSchema);
		pageProperties.put("created", schema("integer", null));
		pageProperties.put("editor", userSchema);
		pageProperties.put("edited", schema("integer", null));
		pageProperties.put("publisher", userSchema);
		pageProperties.put("published", schema("integer", null));
		pageProperties.put("versions", schema("array", "Page versions, newest first.", "items", versionSchema));
		pageProperties.put("currentVersion", versionSchema);
		pageProperties.put("publishedVersion", versionSchema);
		pageProperties.put("languageVariants", schema("object", "Language variants by language code.",
				"additionalProperties", refSchema));
		pageProperties.put("translationStatus", schema("object", null, "properties", translationProperties));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("page", schema("object", "The page after the update.", "properties", pageProperties,
				"required", List.of("ref")));
		properties.put("changedFields", schema("array", "Fields whose value actually changed.", "items",
				schema("string", null)));

		Map<String, Object> outputSchema = new LinkedHashMap<>();
		outputSchema.put("type", "object");
		outputSchema.put("properties", properties);
		outputSchema.put("required", List.of("page", "changedFields"));
		return outputSchema;
	}
}
