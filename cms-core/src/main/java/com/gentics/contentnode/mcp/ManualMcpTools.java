package com.gentics.contentnode.mcp;

import java.util.List;

import com.gentics.contentnode.mcp.tools.GetPermissionsTool;
import com.gentics.contentnode.mcp.tools.ListNodesTool;
import com.gentics.contentnode.mcp.tools.PageLoadTool;
import com.gentics.contentnode.mcp.tools.UpdatePagePropertiesTool;
import com.gentics.contentnode.mcp.tools.WhoamiTool;
import com.gentics.lib.log.NodeLogger;

import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Registers every manually implemented {@link McpToolProvider} on the MCP server.
 *
 * <p>
 * This replaces the classpath-scan-based registration that {@code McpToolRegistry} used to
 * perform for {@code @McpTool}-annotated REST resource methods. That scan is kept in the
 * codebase (its tests still pass), but {@code OSSRunner} no longer calls it - see
 * {@code docs/mcp-server-integration.md}.
 * </p>
 */
public final class ManualMcpTools {
	private static final NodeLogger logger = NodeLogger.getNodeLogger(ManualMcpTools.class);

	/**
	 * Every manually implemented tool, in registration order.
	 */
	private static final List<McpToolProvider> TOOLS = List.of(new PageLoadTool(), new ListNodesTool(),
			new UpdatePagePropertiesTool(), new WhoamiTool(), new GetPermissionsTool());

	/**
	 * Static class, no instances
	 */
	private ManualMcpTools() {
	}

	/**
	 * Get every manually implemented tool, in registration order
	 * @return tools
	 */
	static List<McpToolProvider> tools() {
		return TOOLS;
	}

	/**
	 * Register every tool in {@link #TOOLS} on the given server.
	 * @param server MCP server to register the tools on
	 */
	public static void registerAll(McpSyncServer server) {
		for (McpToolProvider provider : TOOLS) {
			server.addTool(provider.toSyncToolSpecification());
			logger.info(String.format("Registered MCP tool '%s' (%s)", provider.tool().name(),
					provider.getClass().getName()));
		}
	}
}
