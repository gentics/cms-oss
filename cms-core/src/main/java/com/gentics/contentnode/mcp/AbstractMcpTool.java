package com.gentics.contentnode.mcp;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.auth.McpAuthenticator;
import com.gentics.contentnode.mcp.auth.McpRequestCredentials;
import com.gentics.contentnode.mcp.auth.McpSessionBinding;
import com.gentics.contentnode.mcp.model.Problem;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.lib.log.NodeLogger;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Base class for MCP tools, one subclass per tool, registered explicitly via
 * {@link McpTools}. Handles everything that is common to all of them:
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
 * <li>serializing the result of {@link #invoke(Map, Optional)} to a JSON text content block (and,
 * if the tool declares an output schema, also as structured content), or turning any exception it
 * throws into an {@code isError(true)} result, with a {@link Problem} as structured content.</li>
 * </ul>
 *
 * <p>
 * It also provides static helpers shared by the tools, for handling failed REST responses
 * ({@link #errorMessage}, {@link #requireOk}) and releasing the lock on an object after a failed
 * save ({@link #saveOrReleaseLock}, {@link #releaseLock}). Arguments are parsed with
 * {@link McpArgs}, schemas built with {@link McpSchemas}.
 * </p>
 *
 * <p>
 * Does no permission checks of its own. The {@code @RequiredPerm} annotations of a REST resource
 * implementation a tool delegates to are enforced by wrapping it with
 * {@link com.gentics.contentnode.mcp.util.RestPermissions#guard}; object permissions are checked
 * by the delegated-to code itself, as for REST calls. Each tool implementation is responsible for
 * whatever additional permission checks those do not perform.
 * </p>
 */
public abstract class AbstractMcpTool {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final NodeLogger logger = NodeLogger.getNodeLogger(AbstractMcpTool.class);

	/**
	 * The tool's definition (name, title, description, input schema), as reported to MCP clients.
	 * @return tool definition
	 */
	public abstract Tool tool();

	/**
	 * Whether this tool requires an authenticated CMS session (resolved from an API token or
	 * session cookie, see {@link McpRequestCredentials}) before it runs.
	 *
	 * <p>
	 * {@code true} (the default) for almost every tool. {@code false} is reserved for a tool that
	 * does not itself need to act as an already-authenticated CMS user - e.g. a future login tool
	 * that establishes a session in the first place.
	 * </p>
	 * @return true iff an authenticated session is required to run this tool
	 */
	public boolean requiresAuthentication() {
		return true;
	}

	/**
	 * Handle an incoming tool call.
	 * @param context transport context of the request, carrying the extracted
	 * {@link McpRequestCredentials} under {@link McpRequestCredentials#CONTEXT_KEY}
	 * @param request incoming tool call request
	 * @return result of the tool call
	 */
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
			CallToolResult.Builder builder = CallToolResult.builder().addTextContent(MAPPER.writeValueAsString(result));
			// the SDK turns a result without structured content into an error if the tool declares an
			// output schema (and validates the structured content against it), so only set it then
			if (tool().outputSchema() != null) {
				builder.structuredContent(MAPPER.convertValue(result, Map.class));
			}
			return builder.build();
		} catch (Exception e) {
			// reflective calls (e.g. into the enterprise search module, see EnterpriseSearch) wrap the
			// real cause in an InvocationTargetException, whose own getMessage() is null - unwrap it so
			// the actual error is logged/reported instead of "null".
			Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
			logger.error(String.format("Error while invoking MCP tool '%s'", tool().name()), cause);
			Problem problem = Problem.of(tool().name(), cause);
			return CallToolResult.builder().isError(true).addTextContent(String.valueOf(cause.getMessage()))
					.structuredContent(Map.of("problem", MAPPER.convertValue(problem, Map.class))).build();
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

	/**
	 * Build the {@link SyncToolSpecification} used to register this tool on a
	 * {@code McpSyncServer} (see {@link McpTools#registerAll}).
	 * @return tool specification
	 */
	public SyncToolSpecification toSyncToolSpecification() {
		return SyncToolSpecification.builder().tool(tool())
				.callHandler((exchange, request) -> call(exchange.transportContext(), request)).build();
	}

	/**
	 * Build the error message for a response of a REST resource implementation that reports a
	 * failure (e.g. {@code INVALIDDATA}) instead of throwing. Uses the response's messages, or its
	 * response message if there are none.
	 * @param failure description of what failed, e.g. {@code "Page 7 was not saved"}
	 * @param response response
	 * @return message in the form {@code "<failure>: <reason>"}
	 */
	protected static String errorMessage(String failure, GenericResponse response) {
		String messages = response.getMessages() == null ? ""
				: response.getMessages().stream().map(Message::getMessage).filter(Objects::nonNull)
						.collect(Collectors.joining(" "));
		if (!messages.isBlank()) {
			return "%s: %s".formatted(failure, messages);
		}
		String info = response.getResponseInfo() != null ? response.getResponseInfo().getResponseMessage() : null;
		return "%s: %s".formatted(failure, info != null ? info : "unknown error");
	}

	/**
	 * Throw if the response of a REST resource implementation reports a failure (anything but
	 * {@link ResponseCode#OK}). Resource implementations report e.g. {@code INVALIDDATA} this way,
	 * instead of throwing.
	 * @param response response
	 * @param failure description of what failed, e.g. {@code "Page 7 was not saved"}, see
	 *        {@link #errorMessage}
	 * @throws IllegalArgumentException if the response is not OK, caused by a {@link RestMappedException} carrying
	 *         the response
	 */
	protected static void requireOk(GenericResponse response, String failure) {
		if (response == null || response.getResponseInfo() == null
				|| response.getResponseInfo().getResponseCode() != ResponseCode.OK) {
			GenericResponse failed = response != null ? response : new GenericResponse();
			throw new IllegalArgumentException(errorMessage(failure, failed), new RestMappedException(failed));
		}
	}

	/**
	 * Run a save that unlocks the object when it succeeds (e.g. {@code PageResourceImpl#save} with
	 * {@code unlock = true}), after the object was locked for the update. If the save throws or
	 * its response is not OK ({@link #requireOk}), the lock is released ({@link #releaseLock}) and
	 * the failure rethrown.
	 * @param clazz object class
	 * @param id object ID
	 * @param failure description of what failed, e.g. {@code "Page 7 was not saved"}
	 * @param save the save
	 * @return response of the save
	 * @throws Exception the failure of the save
	 */
	protected static GenericResponse saveOrReleaseLock(Class<? extends NodeObject> clazz, String id, String failure,
			Callable<? extends GenericResponse> save) throws Exception {
		boolean saved = false;
		try {
			GenericResponse response = save.call();
			requireOk(response, failure);
			saved = true;
			return response;
		} finally {
			if (!saved) {
				releaseLock(clazz, id);
			}
		}
	}

	/**
	 * Release the calling user's lock on an object, e.g. after a failed save. Only unlocks: this
	 * does not restore anything (unlike e.g. {@code PageResourceImpl#cancel}, which also restores
	 * the latest page version). Errors are logged, not thrown, so that they do not replace the
	 * original failure.
	 *
	 * <p>
	 * Delegates to {@link NodeObject#unlock()}, which only does something for objects that can be
	 * locked: pages ({@code PageFactory}) and templates ({@code TemplateFactory}) only clear a lock
	 * held by the current user; forms ({@code FormFactory}) throw if another user holds the lock,
	 * which is logged. For most other object types, {@code unlock()} does nothing.
	 * </p>
	 * @param clazz object class
	 * @param id object ID
	 */
	protected static void releaseLock(Class<? extends NodeObject> clazz, String id) {
		try (Trx trx = ContentNodeHelper.trx()) {
			NodeObject object = trx.getTransaction().getObject(clazz, id);
			if (object != null) {
				object.unlock();
			}
			trx.success();
		} catch (Exception e) {
			logger.error(String.format("Error while releasing the lock on %s %s", clazz.getSimpleName(), id), e);
		}
	}

	private McpRequestCredentials extractCredentials(McpTransportContext context) {
		Object value = context.get(McpRequestCredentials.CONTEXT_KEY);
		return value instanceof McpRequestCredentials credentials ? credentials : McpRequestCredentials.EMPTY;
	}
}
