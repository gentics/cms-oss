package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;

/**
 * Parameter bean for getting templates of a folder
 *
 * @author plyhun
 *
 */
public class TemplateListParameterBean extends ReducedListParameterBean {

	/**
	 * node id for this folder - for use with multichannelling
	 */
	@QueryParam("folderNodeId")
	@McpToolParam(description = "Node ID for this folder - for use with multichannelling.", required = false)
	public Integer nodeId;

	/**
	 * true to only return inherited templates in the given node, false to only get local/localized templates, null to get local and inherited templates
	 */
	@QueryParam("inherited")
	@McpToolParam(description = "True to only return inherited templates in the given node, false to only get "
			+ "local/localized templates, unset to get local and inherited templates.", required = false)
	public Boolean inherited;

	/**
	 * true if the access permission should be checked for every template
	 */
	@QueryParam("checkPermission")
	@DefaultValue("true")
	@McpToolParam(description = "True if the access permission should be checked for every template.", required = false)
	public boolean checkPermission = true;

	/**
	 * true if the list should be reduced to the unique template occurrences only. valid only when {@link InFolderParameterBean#recursive} flag is set.
	 */
	@QueryParam("reduce")
	@DefaultValue("false")
	@McpToolParam(description = "True if the list should be reduced to the unique template occurrences only. Only "
			+ "valid when the 'recursive' flag is set.", required = false)
	public boolean reduce = false;

	public TemplateListParameterBean setFolderNodeId(Integer nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	public TemplateListParameterBean setInherited(Boolean inherited) {
		this.inherited = inherited;
		return this;
	}

	public TemplateListParameterBean setCheckPermission(boolean checkPermission) {
		this.checkPermission = checkPermission;
		return this;
	}

	public TemplateListParameterBean setReduce(boolean reduce) {
		this.reduce = reduce;
		return this;
	}
}
