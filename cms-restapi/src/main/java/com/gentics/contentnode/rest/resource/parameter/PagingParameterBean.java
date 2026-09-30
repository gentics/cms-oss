package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;

/**
 * Parameter bean for paging
 */
public class PagingParameterBean {
	/**
	 * Returned page, if paging is used. Paging starts with <code>1</code>
	 */
	@QueryParam("page")
	@DefaultValue("1")
	@McpToolParam(description = "Returned page, if paging is used. Paging starts with 1.", required = false)
	public int page = 1;

	/**
	 * Page size for paging. If this is set to <code>-1</code> no paging is used (all matching items are returned).
	 * Setting this to <code>0</code> will return no items.
	 */
	@QueryParam("pageSize")
	@DefaultValue("-1")
	@McpToolParam(description = "Page size for paging. -1 (default) returns all matching items unpaged, 0 returns no items.", required = false)
	public int pageSize = -1;

	public PagingParameterBean setPage(int page) {
		this.page = page;
		return this;
	}

	public PagingParameterBean setPageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}
}
