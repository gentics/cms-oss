package com.gentics.contentnode.rest.model.response.admin;

import java.util.List;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Response containing the custom tools to display in the UI
 */
public class ToolsResponse extends GenericResponse {
	protected List<CustomTool> tools;

	/**
	 * List of custom tools
	 * @return tool list
	 */
	public List<CustomTool> getTools() {
		return tools;
	}

	/**
	 * Set custom tools list
	 * @param tool list
	 */
	public void setTools(List<CustomTool> tools) {
		this.tools = tools;
	}
}
