package com.gentics.contentnode.mcp;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.rest.version.Main;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.log.NodeLogger;

import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

/**
 * Bootstrap for the MCP (Model Context Protocol) server of the CMS.
 *
 * <p>
 * The MCP server is exposed over the "Streamable HTTP" transport of the MCP Java SDK,
 * which is implemented as a plain {@link jakarta.servlet.http.HttpServlet}. The servlet
 * returned by {@link #getServlet()} has to be registered by the server runner (see
 * {@code com.gentics.contentnode.server.OSSRunner}) under the configured
 * {@link ConfigurationValue#MCP_PATH path} (which is {@code /mcp} by default).
 * </p>
 *
 * <p>
 * Note that the transport provider requires an asynchronous servlet, so the servlet
 * holder must be configured with {@code setAsyncSupported(true)}.
 * </p>
 *
 * <p>
 * Tools, resources and prompts are not registered here. They are added to the
 * {@link McpSyncServer} instance returned by {@link #getServer()}.
 * </p>
 */
public final class MCPServer {
	/**
	 * Name of the MCP server, which is reported to the clients
	 */
	public final static String SERVER_NAME = "Gentics CMS";

	/**
	 * Instructions, which are reported to the clients
	 */
	public final static String SERVER_INSTRUCTIONS = "MCP server of a Gentics CMS instance. It provides access to the CMS REST API.";

	/**
	 * Logger
	 */
	private final static NodeLogger logger = NodeLogger.getNodeLogger(MCPServer.class);

	/**
	 * Transport provider (also the servlet), if the MCP server has been initialized
	 */
	private static HttpServletStreamableServerTransportProvider transportProvider;

	/**
	 * MCP server instance, if the MCP server has been initialized
	 */
	private static McpSyncServer server;

	/**
	 * Static class, no instances
	 */
	private MCPServer() {
	}

	/**
	 * Check whether the MCP endpoint is enabled
	 * @return true iff the MCP endpoint is enabled
	 */
	public static boolean isEnabled() {
		return Boolean.parseBoolean(ConfigurationValue.MCP_ENABLED.get());
	}

	/**
	 * Get the path, under which the MCP endpoint is served
	 * @return path of the MCP endpoint (e.g. {@code /mcp})
	 */
	public static String getPath() {
		return ConfigurationValue.MCP_PATH.get();
	}

	/**
	 * Get the servlet, which serves the MCP endpoint. The MCP server is initialized on the
	 * first call.
	 * @return servlet for the MCP endpoint
	 */
	public static synchronized HttpServletStreamableServerTransportProvider getServlet() {
		if (transportProvider == null) {
			String path = getPath();

			transportProvider = HttpServletStreamableServerTransportProvider.builder()
					.jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
					.mcpEndpoint(path)
					.build();

			server = McpServer.sync(transportProvider)
					.jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
					.jsonSchemaValidator(new DefaultJsonSchemaValidator())
					.serverInfo(SERVER_NAME, Main.getImplementationVersion())
					.instructions(SERVER_INSTRUCTIONS)
					.capabilities(ServerCapabilities.builder().tools(true).build())
					.build();

			logger.info(String.format("Initialized MCP server at %s", path));
		}

		return transportProvider;
	}

	/**
	 * Get the MCP server instance, if it has been initialized. This is the instance, to which
	 * tools, resources and prompts can be added.
	 * @return optional MCP server instance
	 */
	public static synchronized Optional<McpSyncServer> getServer() {
		return Optional.ofNullable(server);
	}

	/**
	 * Shut the MCP server down (if it was initialized). Closing all open sessions.
	 */
	public static synchronized void shutdown() {
		if (server != null) {
			try {
				server.closeGracefully();
			} catch (Exception e) {
				logger.warn("Error while shutting down the MCP server", e);
			}
			server = null;
			transportProvider = null;

			logger.info("Shut down MCP server");
		}
	}
}
