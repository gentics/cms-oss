package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Loads a single CMS page by ID.
 *
 * <p>
 * This is the first manually implemented MCP tool ({@link AbstractMcpTool}), deliberately kept
 * minimal: its purpose is to prove that MCP tool calls now run as the real, authenticated caller
 * instead of the CMS system user (see {@code docs/mcp-server-integration.md}). It delegates to
 * {@link PageResourceImpl#load}, so the same object-permission check
 * ({@code PermHandler.ObjectPermission.view}, performed inside
 * {@code PageResourceImpl#getPage}) that the real {@code GET /rest/page/load/{id}} REST endpoint
 * performs also applies here - with a real session bound by {@link AbstractMcpTool#call}, that
 * check runs against the calling user's actual permissions, not the system user's unrestricted
 * ones. No additional permission check is needed in this tool itself, since
 * {@code PageResourceImpl#load} already performs it.
 * </p>
 */
public class PageLoadTool extends AbstractMcpTool {
	private static final String ARG_ID = "id";

	private static final String ARG_NODE_ID = "nodeId";

	@Override
	public Tool tool() {
		Map<String, Object> idSchema = new LinkedHashMap<>();
		idSchema.put("type", "string");
		idSchema.put("description", "ID of the page to load.");

		Map<String, Object> nodeIdSchema = new LinkedHashMap<>();
		nodeIdSchema.put("type", "integer");
		nodeIdSchema.put("description", "ID of the node/channel to load the page in.");

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, idSchema);
		properties.put(ARG_NODE_ID, nodeIdSchema);

		return Tool.builder().name("page_load").title("Load Page")
				.description("Load a single CMS page by ID.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_ID)).additionalProperties(false).build())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Object rawId = arguments.get(ARG_ID);
		if (rawId == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_ID));
		}

		String id = String.valueOf(rawId);
		Object rawNodeId = arguments.get(ARG_NODE_ID);
		Integer nodeId = rawNodeId != null ? Integer.valueOf(String.valueOf(rawNodeId)) : null;

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());

		return pageResource.load(id, false, false, false, false, false, false, false, false, false, false, nodeId,
				null);
	}
}
