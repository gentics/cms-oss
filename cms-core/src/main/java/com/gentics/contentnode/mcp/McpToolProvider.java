package com.gentics.contentnode.mcp;

import com.gentics.contentnode.mcp.auth.McpRequestCredentials;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * A manually implemented MCP tool.
 *
 * <p>
 * This is the replacement for the previous approach of deriving tools automatically from
 * {@code @McpTool}-annotated REST resource methods via {@link McpToolRegistry}'s classpath scan.
 * That scan (and the {@code @McpTool}/{@code @McpToolParam} annotations it looks for) is kept in
 * the codebase for now, but is no longer invoked from {@code OSSRunner} - see
 * {@code docs/mcp-server-integration.md}. Tools are now registered explicitly, one
 * {@link McpToolProvider} implementation per tool, via {@link ManualMcpTools}.
 * </p>
 *
 * <p>
 * Implementations should extend {@link AbstractMcpTool} rather than implementing this interface
 * directly, to get its authentication handling (resolving the caller's CMS session, rejecting an
 * unauthenticated call, binding the session for the duration of the call) for free.
 * </p>
 */
public interface McpToolProvider {
	/**
	 * The tool's definition (name, title, description, input schema), as reported to MCP clients.
	 * @return tool definition
	 */
	Tool tool();

	/**
	 * Whether this tool requires an authenticated CMS session (resolved from an API token or
	 * session cookie, see {@link McpRequestCredentials}) before it runs.
	 *
	 * <p>
	 * {@code true} (the default in {@link AbstractMcpTool}) for almost every tool. {@code false}
	 * is reserved for a tool that does not itself need to act as an already-authenticated CMS
	 * user - e.g. a future login tool that establishes a session in the first place.
	 * </p>
	 * @return true iff an authenticated session is required to run this tool
	 */
	boolean requiresAuthentication();

	/**
	 * Handle an incoming tool call.
	 * @param context transport context of the request, carrying the extracted
	 * {@link McpRequestCredentials} under {@link McpRequestCredentials#CONTEXT_KEY}
	 * @param request incoming tool call request
	 * @return result of the tool call
	 */
	CallToolResult call(McpTransportContext context, CallToolRequest request);

	/**
	 * Build the {@link SyncToolSpecification} used to register this tool on a
	 * {@code McpSyncServer} (see {@link ManualMcpTools#registerAll}).
	 * @return tool specification
	 */
	default SyncToolSpecification toSyncToolSpecification() {
		return SyncToolSpecification.builder().tool(tool())
				.callHandler((exchange, request) -> call(exchange.transportContext(), request)).build();
	}
}
