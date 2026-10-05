package com.gentics.contentnode.mcp;

import java.util.List;

import com.gentics.contentnode.mcp.tools.AddConstructToPackageTool;
import com.gentics.contentnode.mcp.tools.AddPageTagTool;
import com.gentics.contentnode.mcp.tools.AssignConstructToNodesTool;
import com.gentics.contentnode.mcp.tools.AssignUserToGroupTool;
import com.gentics.contentnode.mcp.tools.CountContentTool;
import com.gentics.contentnode.mcp.tools.CreateConstructTool;
import com.gentics.contentnode.mcp.tools.CreatePageTool;
import com.gentics.contentnode.mcp.tools.CreateUserTool;
import com.gentics.contentnode.mcp.tools.DeletePageTool;
import com.gentics.contentnode.mcp.tools.EnsureConstructCategoryTool;
import com.gentics.contentnode.mcp.tools.FindSimilarTool;
import com.gentics.contentnode.mcp.tools.GetConstructTool;
import com.gentics.contentnode.mcp.tools.GetDatasourceTool;
import com.gentics.contentnode.mcp.tools.GetFileTool;
import com.gentics.contentnode.mcp.tools.GetFolderTreeTool;
import com.gentics.contentnode.mcp.tools.GetGroupPermissionsTool;
import com.gentics.contentnode.mcp.tools.GetImageTool;
import com.gentics.contentnode.mcp.tools.GetPageTagsTool;
import com.gentics.contentnode.mcp.tools.GetPageTool;
import com.gentics.contentnode.mcp.tools.GetPageVersionsTool;
import com.gentics.contentnode.mcp.tools.GetPermissionsTool;
import com.gentics.contentnode.mcp.tools.GetRelatedTool;
import com.gentics.contentnode.mcp.tools.GetTemplateTool;
import com.gentics.contentnode.mcp.tools.ListConstructCategoriesTool;
import com.gentics.contentnode.mcp.tools.ListConstructsTool;
import com.gentics.contentnode.mcp.tools.ListDatasourcesTool;
import com.gentics.contentnode.mcp.tools.ListFolderItemsTool;
import com.gentics.contentnode.mcp.tools.ListGroupsTool;
import com.gentics.contentnode.mcp.tools.ListNodesTool;
import com.gentics.contentnode.mcp.tools.ListPackagesTool;
import com.gentics.contentnode.mcp.tools.ListPartTypesTool;
import com.gentics.contentnode.mcp.tools.ListTemplatesTool;
import com.gentics.contentnode.mcp.tools.ListUsersTool;
import com.gentics.contentnode.mcp.tools.PageLoadTool;
import com.gentics.contentnode.mcp.tools.PreviewPermissionImpactTool;
import com.gentics.contentnode.mcp.tools.PublishPageTool;
import com.gentics.contentnode.mcp.tools.RemoveUserFromGroupTool;
import com.gentics.contentnode.mcp.tools.RenderPreviewTool;
import com.gentics.contentnode.mcp.tools.RestorePageVersionTool;
import com.gentics.contentnode.mcp.tools.SearchContentTool;
import com.gentics.contentnode.mcp.tools.TakeOfflineTool;
import com.gentics.contentnode.mcp.tools.TranslatePageTool;
import com.gentics.contentnode.mcp.tools.UpdateConstructTool;
import com.gentics.contentnode.mcp.tools.UpdatePagePropertiesTool;
import com.gentics.contentnode.mcp.tools.UpdatePageTagsTool;
import com.gentics.contentnode.mcp.tools.UploadFileTool;
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
			new UpdatePagePropertiesTool(), new WhoamiTool(), new GetPermissionsTool(), new GetFileTool(),
			new GetImageTool(), new GetPageVersionsTool(), new GetPageTool(), new GetPageTagsTool(),
			new ListFolderItemsTool(), new GetFolderTreeTool(), new ListTemplatesTool(), new GetTemplateTool(),
			new RenderPreviewTool(), new GetRelatedTool(), new SearchContentTool(), new CountContentTool(),
			new FindSimilarTool(), new CreatePageTool(), new RestorePageVersionTool(), new AddPageTagTool(),
			new UpdatePageTagsTool(), new PublishPageTool(), new TakeOfflineTool(), new TranslatePageTool(),
			new DeletePageTool(), new UploadFileTool(), new ListPartTypesTool(), new ListDatasourcesTool(),
			new GetDatasourceTool(), new ListConstructCategoriesTool(), new ListConstructsTool(),
			new GetConstructTool(), new EnsureConstructCategoryTool(), new CreateConstructTool(),
			new UpdateConstructTool(), new AssignConstructToNodesTool(), new ListPackagesTool(),
			new AddConstructToPackageTool(), new ListUsersTool(), new ListGroupsTool(),
			new GetGroupPermissionsTool(), new CreateUserTool(), new AssignUserToGroupTool(),
			new RemoveUserFromGroupTool(), new PreviewPermissionImpactTool());

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
