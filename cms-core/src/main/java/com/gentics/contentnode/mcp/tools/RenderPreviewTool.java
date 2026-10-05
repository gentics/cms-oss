package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
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
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.PageVersion;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Renders a page in view mode, optionally a version of it, and returns a preview URL and optionally the HTML. Delegates
 * to {@link PageResourceImpl#load} (view permission, version lookup) and {@link PageResourceImpl#render} without edit
 * mode, which would lock the page; edit mode is rejected.
 */
public class RenderPreviewTool extends AbstractMcpTool {
	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_MODE = "mode";

	static final String ARG_VERSION = "version";

	static final String ARG_INCLUDE_HTML = "includeHtml";

	static final String ARG_MAX_CHARS = "maxChars";

	/**
	 * Default of {@link #ARG_MAX_CHARS}
	 */
	static final int DEFAULT_MAX_CHARS = 200000;

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param ref reference to the page
	 * @param previewUrl preview URL, relative to the CMS base URL; opening it needs a CMS session
	 * @param html rendered HTML, if requested
	 * @param truncated whether the HTML was cut at maxChars
	 * @param renderedAt render timestamp
	 * @param messages messages of the CMS
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ObjectRef ref, String previewUrl, String html, boolean truncated, int renderedAt,
			List<String> messages) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PAGE_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to render the page in.", "minimum", 1));
		properties.put(ARG_MODE, schema("string", "Only view is supported; edit would lock the page.", "enum",
				List.of("view", "edit"), "default", "view"));
		properties.put(ARG_VERSION, schema("integer",
				"Version to render: a major version number (2 for version 2.0) or a version timestamp from "
						+ "get_page_versions. Default the current version.",
				"minimum", 1));
		properties.put(ARG_INCLUDE_HTML, schema("boolean", null, "default", false));
		properties.put(ARG_MAX_CHARS, schema("integer", null, "minimum", 1000, "maximum", 500000, "default",
				DEFAULT_MAX_CHARS));

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("ref", ObjectRef.jsonSchema(null));
		output.put("previewUrl", schema("string", "Relative to the CMS base URL, needs a CMS session."));
		output.put("html", schema("string", null));
		output.put("truncated", schema("boolean", "Whether html was cut at maxChars."));
		output.put("renderedAt", schema("integer", "Unix timestamp in seconds."));
		output.put("messages", schema("array", null, "items", schema("string", null)));

		return Tool.builder().name("render_preview").title("Render page preview")
				.description("Renders a saved page the way the CMS will publish it, and returns a preview URL plus "
						+ "optionally the HTML. Use it to check that content and constructs actually render, and give "
						+ "the URL to the user so they can look at the page. Request html only when you need to "
						+ "inspect the output yourself: rendered HTML is large. Preview covers pages only; there is no "
						+ "template or construct preview.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_PAGE_ID)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("previewUrl")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int pageId = Args.id(arguments, ARG_PAGE_ID);
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
		String mode = Optional.ofNullable(stringArg(arguments, ARG_MODE, 1, 8)).orElse("view");
		if (!mode.equals("view")) {
			throw new IllegalArgumentException(
					"Argument '%s' only supports 'view', edit mode would lock the page".formatted(ARG_MODE));
		}
		Integer version = intArg(arguments, ARG_VERSION, 1, Integer.MAX_VALUE);
		boolean includeHtml = booleanArg(arguments, ARG_INCLUDE_HTML, false);
		int maxChars = intArg(arguments, ARG_MAX_CHARS, 1000, 500000, DEFAULT_MAX_CHARS);

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		String id = Integer.toString(pageId);
		PageLoadResponse loaded = pageResource.load(id, false, false, true, false, false, false, false,
				version != null, false, false, nodeId, null);
		requireOk(loaded, "The page %d could not be loaded".formatted(pageId));
		Page page = loaded.getPage();
		Integer versionTimestamp = version != null ? versionTimestamp(page, version) : null;

		PageRenderResponse rendered = pageResource.render(id, nodeId, null, false, null, LinksType.backend, false,
				false, false, versionTimestamp);
		requireOk(rendered, "The page %d could not be rendered".formatted(pageId));

		return result(page, nodeId, versionTimestamp, includeHtml ? rendered.getContent() : null, maxChars,
				rendered.getMessages(), (int) (System.currentTimeMillis() / 1000));
	}

	/**
	 * Get the timestamp of a page version, given as major version number or as version timestamp
	 * @param page page loaded with its versions
	 * @param version major version number or version timestamp
	 * @return version timestamp
	 * @throws EntityNotFoundException if the page has no such version
	 */
	static int versionTimestamp(Page page, int version) throws EntityNotFoundException {
		List<PageVersion> versions = page.getVersions() != null ? page.getVersions() : List.of();
		for (PageVersion candidate : versions) {
			if (candidate.getTimestamp() == version) {
				return version;
			}
		}
		for (PageVersion candidate : versions) {
			if (Objects.equals(candidate.getNumber(), version + ".0")) {
				return candidate.getTimestamp();
			}
		}
		throw new EntityNotFoundException("Page %d has no version %d".formatted(page.getId(), version));
	}

	/**
	 * Build the result
	 * @param page REST page
	 * @param nodeId node ID, may be null
	 * @param versionTimestamp rendered version, null for the current one
	 * @param html rendered HTML, null if not requested
	 * @param maxChars maximum length of the HTML
	 * @param messages CMS messages, may be null
	 * @param renderedAt render timestamp
	 * @return result
	 */
	static Result result(Page page, Integer nodeId, Integer versionTimestamp, String html, int maxChars,
			List<Message> messages, int renderedAt) {
		StringBuilder url = new StringBuilder("/rest/page/render/content/").append(page.getId());
		String separator = "?";
		if (nodeId != null) {
			url.append(separator).append("nodeId=").append(nodeId);
			separator = "&";
		}
		if (versionTimestamp != null) {
			url.append(separator).append("version=").append(versionTimestamp);
		}
		boolean truncated = html != null && html.length() > maxChars;
		List<String> texts = new ArrayList<>();
		if (messages != null) {
			for (Message message : messages) {
				if (message.getMessage() != null) {
					texts.add(message.getMessage());
				}
			}
		}
		ObjectRef ref = nodeId != null ? ObjectRef.forPage(page, nodeId) : ObjectRef.forPage(page);
		return new Result(ref, url.toString(), truncated ? html.substring(0, maxChars) : html, truncated, renderedAt,
				texts);
	}
}
