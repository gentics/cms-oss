package com.gentics.contentnode.mcp;

import java.util.List;

/**
 * Service for modules that add tools to the MCP server. Implementations are found with the {@link java.util.ServiceLoader}.
 */
public interface McpToolService {
	/**
	 * Get the tools of the module. A module returns none, while what its tools need is not available.
	 * @return tools
	 */
	List<AbstractMcpTool> tools();
}
