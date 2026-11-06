package com.gentics.contentnode.mcp.tools;

import java.util.List;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.McpToolService;

/**
 * Adds the Forms tools to the MCP server, while the feature {@link Feature#FORMS} is provided.
 */
public class FormMcpToolService implements McpToolService {
	@Override
	public List<AbstractMcpTool> tools() {
		if (!Feature.FORMS.isAvailable()) {
			return List.of();
		}
		return List.of(new GetFormTool(), new ListFormTypesTool(), new CreateFormTool(), new UpdateFormTool(),
				new DeleteFormTool(), new PublishFormTool());
	}
}
