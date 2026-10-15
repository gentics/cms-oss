package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.UserRef;
import com.gentics.contentnode.mcp.model.VersionInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.PageVersion;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the versions of a page, newest first, marking the published one. Delegates to {@link PageResourceImpl#load}
 * with {@code versioninfo}, which checks {@code ObjectPermission.view} and never locks.
 */
public class GetPageVersionsTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_SIZE = "size";

	/**
	 * Default number of versions
	 */
	static final int DEFAULT_SIZE = 25;

	/**
	 * Maximum number of versions
	 */
	static final int MAX_SIZE = 100;

	/**
	 * A version of the page
	 * @param number version number
	 * @param timestamp version timestamp
	 * @param editor editor of the version
	 * @param published whether this is the published version
	 */
	@JsonInclude(Include.NON_NULL)
	public record Version(String number, Integer timestamp, UserRef editor, boolean published) {
	}

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param ref reference to the page
	 * @param currentVersion current version
	 * @param publishedVersion published version, if the page is published
	 * @param versions versions, newest first, at most the requested number
	 * @param truncated whether the page has more versions
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ObjectRef ref, VersionInfo currentVersion, VersionInfo publishedVersion,
			List<Version> versions, boolean truncated) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to load the page in.", "minimum", 1));
		properties.put(ARG_SIZE, schema("integer", "Maximum number of versions, newest first.", "minimum", 1,
				"maximum", MAX_SIZE, "default", DEFAULT_SIZE));

		return Tool.builder().name("get_page_versions").title("Get page versions")
				.description("Returns the version history of a page with editor and timestamp for each version, plus "
						+ "which version is current and which is published. Use it to answer who changed a page and "
						+ "when, and to choose a version for restore_page_version. It does NOT return the content of "
						+ "old versions.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Args.id(arguments, ARG_ID);
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
		int size = intArg(arguments, ARG_SIZE, 1, MAX_SIZE, DEFAULT_SIZE);

		PageLoadResponse response = RestPermissions.guard(PageResource.class, new PageResourceImpl())
				.load(Integer.toString(id), false, false, true, false, false, false, false, true, false, false, nodeId,
						null);
		requireOk(response, "The page %d could not be loaded".formatted(id));

		return result(response.getPage(), nodeId, size);
	}

	/**
	 * Build the result from the page loaded with its version info
	 * @param page REST page
	 * @param nodeId node ID the page was loaded for, may be null
	 * @param size maximum number of versions
	 * @return result
	 */
	static Result result(Page page, Integer nodeId, int size) {
		PageVersion published = page.getPublishedVersion();
		// the REST page lists its versions oldest first
		List<PageVersion> all = new ArrayList<>(page.getVersions() != null ? page.getVersions() : List.of());
		all.sort(Comparator.comparingInt(PageVersion::getTimestamp).reversed());
		List<Version> versions = new ArrayList<>();
		for (PageVersion version : all.subList(0, Math.min(size, all.size()))) {
			versions.add(new Version(version.getNumber(), version.getTimestamp(), UserRef.of(version.getEditor()),
					published != null && Objects.equals(published.getNumber(), version.getNumber())));
		}
		ObjectRef ref = nodeId != null ? ObjectRef.forPage(page, nodeId) : ObjectRef.forPage(page);
		return new Result(ref, VersionInfo.of(page.getCurrentVersion()), VersionInfo.of(published), versions,
				all.size() > size);
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> versionProperties = new LinkedHashMap<>(
				castProperties(VersionInfo.jsonSchema().get("properties")));
		versionProperties.put("published", schema("boolean", "Whether this is the published version."));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema(null));
		properties.put("currentVersion", VersionInfo.jsonSchema());
		properties.put("publishedVersion", VersionInfo.jsonSchema());
		properties.put("versions", schema("array", "Versions, newest first.", "items",
				schema("object", null, "properties", versionProperties)));
		properties.put("truncated", schema("boolean", "Whether the page has more versions than returned."));
		return schema("object", null, "properties", properties, "required", List.of("versions"));
	}

	/**
	 * Cast the properties of a model schema
	 * @param properties properties
	 * @return properties map
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> castProperties(Object properties) {
		return (Map<String, Object>) properties;
	}
}
