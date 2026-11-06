package com.gentics.contentnode.mcp.tools;

import java.util.List;

import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.McpToolService;

/**
 * Adds the search tools to the MCP server. This module depends on cms-search, so they are always there. While the
 * {@code elasticsearch} feature is off, they answer {@code search-unavailable}.
 */
public class SearchMcpToolService implements McpToolService {
	@Override
	public List<AbstractMcpTool> tools() {
		return List.of(new SearchContentTool(), new CountContentTool(), new FindSimilarTool());
	}
}
