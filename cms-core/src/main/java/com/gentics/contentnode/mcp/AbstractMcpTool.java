package com.gentics.contentnode.mcp;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.auth.McpAuthenticator;
import com.gentics.contentnode.mcp.auth.McpRequestCredentials;
import com.gentics.contentnode.mcp.auth.McpSessionBinding;
import com.gentics.lib.log.NodeLogger;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Base class for manually implemented MCP tools ({@link McpToolProvider}), handling everything
 * that is common to all of them:
 * <ul>
 * <li>resolving the {@link McpRequestCredentials} carried on the {@link McpTransportContext} into
 * a real CMS {@link Session} (see {@link McpAuthenticator});</li>
 * <li>rejecting the call outright, with an {@code isError(true)} result, if
 * {@link #requiresAuthentication()} is true (the default) and no session could be resolved - this
 * always applies, independent of whatever {@code CmsMcpSecurityValidator}/
 * {@code ConfigurationValue#MCP_REQUIRE_AUTH} does at the transport level (see
 * {@code docs/mcp-server-integration.md});</li>
 * <li>binding the resolved session for the duration of the call ({@link McpSessionBinding}), so
 * that whatever the tool delegates to (typically a REST resource implementation method) runs as
 * the real caller, not the CMS system user;</li>
 * <li>serializing the result of {@link #invoke(Map, Optional)} to a JSON text content block, or
 * turning any exception it throws into an {@code isError(true)} result.</li>
 * </ul>
 *
 * <p>
 * Deliberately does <b>not</b> re-run a {@code @RequiredPerm} annotation of whatever method a
 * tool delegates to. Each tool implementation is responsible for whatever additional permission
 * checks that call does not already perform on its own - typically by passing the required
 * {@link com.gentics.contentnode.perm.PermHandler.ObjectPermission} into the delegated-to
 * {@code *ResourceImpl} helper method, the way {@code PageResourceImpl#getPage} already does for
 * {@link com.gentics.contentnode.mcp.tools.PageLoadTool}.
 * </p>
 */
public abstract class AbstractMcpTool implements McpToolProvider {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final NodeLogger logger = NodeLogger.getNodeLogger(AbstractMcpTool.class);

	@Override
	public boolean requiresAuthentication() {
		return true;
	}

	@Override
	public final CallToolResult call(McpTransportContext context, CallToolRequest request) {
		McpRequestCredentials credentials = extractCredentials(context);
		Optional<Session> session = McpAuthenticator.resolve(credentials);

		if (requiresAuthentication() && session.isEmpty()) {
			return CallToolResult.builder().isError(true)
					.addTextContent("This tool requires an authenticated CMS session. Send either an "
							+ "'Authorization: Bearer <api token>' header or a valid CMS session cookie.")
					.build();
		}

		Map<String, Object> arguments = request.arguments() != null ? request.arguments() : Map.of();

		try (McpSessionBinding binding = new McpSessionBinding(session.orElse(null))) {
			Object result = invoke(arguments, session);
			return CallToolResult.builder().addTextContent(MAPPER.writeValueAsString(result)).build();
		} catch (Exception e) {
			// method.invoke()-style reflective delegation wraps the real cause in an
			// InvocationTargetException, whose own getMessage() is null - unwrap it so the actual
			// error is logged/reported instead of "null" (mirrors McpToolRegistry#invoke).
			Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
			logger.error(String.format("Error while invoking MCP tool '%s'", tool().name()), cause);
			return CallToolResult.builder().isError(true).addTextContent(String.valueOf(cause.getMessage())).build();
		}
	}

	/**
	 * Run the actual tool logic. A CMS session is already bound to the current thread (see
	 * {@link McpSessionBinding}) unless {@link #requiresAuthentication()} is {@code false} and no
	 * session was resolved, in which case {@code session} is empty and no session is bound.
	 * @param arguments raw tool call arguments (never {@code null}, but possibly empty)
	 * @param session the resolved session, if any
	 * @return result to serialize back to the caller as JSON
	 * @throws Exception on any failure; turned into an {@code isError(true)} result
	 */
	protected abstract Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception;

	private McpRequestCredentials extractCredentials(McpTransportContext context) {
		Object value = context.get(McpRequestCredentials.CONTEXT_KEY);
		return value instanceof McpRequestCredentials credentials ? credentials : McpRequestCredentials.EMPTY;
	}
}
