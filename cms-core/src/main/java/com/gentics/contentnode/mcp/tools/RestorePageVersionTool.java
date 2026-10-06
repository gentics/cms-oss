package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.mcp.model.VersionInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.PageVersion;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Restores a version of a page. The version is looked up with {@link PageResourceImpl#load} first, because
 * {@link PageResourceImpl#restoreVersion} reports an unknown version as a general failure. The delegate checks the edit
 * permission and leaves the page locked, so the tool releases the lock, also on failure.
 */
public class RestorePageVersionTool extends AbstractMcpTool {
	static final String NAME = "restore_page_version";

	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_VERSION = "version";

	static final String ARG_NODE_ID = "nodeId";

	/**
	 * Result of the tool
	 * @param page page after the restore
	 * @param restoredVersion the restored version
	 */
	public record Result(PageInfo page, VersionInfo restoredVersion) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PAGE_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_VERSION, schema("integer", "Version timestamp, as returned by get_page_versions.", "minimum",
				1));
		properties.put(ARG_NODE_ID, schema("integer", "Not supported yet: calls that set it are rejected.", "minimum",
				1));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Restore page version")
				.description("Restores a previous version of a page, making it the current version. Always list the "
						+ "versions with get_page_versions first and confirm the target version with the user: this "
						+ "overwrites the current content. Restoring does not publish; call publish_page afterwards if "
						+ "the page should go live.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_PAGE_ID, ARG_VERSION)).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Writes.rejectChannel(NAME, arguments);
		int id = Args.id(arguments, ARG_PAGE_ID);
		int version = Args.id(arguments, ARG_VERSION);
		Writes.logIdempotencyKey(NAME, id, arguments);

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		PageLoadResponse loaded = pageResource.load(Integer.toString(id), false, false, false, false, false, false,
				false, true, false, false, null, null);
		requireOk(loaded, "The page %d could not be loaded".formatted(id));
		PageVersion restored = find(loaded.getPage().getVersions(), version);
		if (restored == null) {
			throw new EntityNotFoundException(
					"Page %d has no version %d, see get_page_versions".formatted(id, version));
		}

		try {
			requireOk(pageResource.restoreVersion(Integer.toString(id), version),
					"Version %d of page %d was not restored".formatted(version, id));
		} finally {
			releaseLock(Page.class, Integer.toString(id));
		}

		return new Result(PageInfo.of(Writes.readBack(pageResource, id)), VersionInfo.of(restored));
	}

	/**
	 * Find a version by its timestamp
	 * @param versions versions, may be null
	 * @param timestamp version timestamp
	 * @return version or null
	 */
	static PageVersion find(List<PageVersion> versions, int timestamp) {
		if (versions != null) {
			for (PageVersion candidate : versions) {
				if (candidate.getTimestamp() == timestamp) {
					return candidate;
				}
			}
		}
		return null;
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("page", PageInfo.jsonSchema("The page after the restore, unlocked and not published."));
		properties.put("restoredVersion", VersionInfo.jsonSchema());
		return schema("object", null, "properties", properties, "required", List.of("page"));
	}
}
