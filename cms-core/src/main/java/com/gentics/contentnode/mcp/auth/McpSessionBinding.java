package com.gentics.contentnode.mcp.auth;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;

/**
 * Binds a resolved CMS {@link Session} to the current thread (via {@link ContentNodeHelper}) for
 * the duration of an MCP tool call, and restores the previous session (and language) afterwards.
 *
 * <p>
 * This matters because a sync MCP tool handler does <b>not</b> run on the servlet thread that
 * received the HTTP request - the MCP Java SDK offloads it onto a pooled
 * {@code Schedulers.boundedElastic()} thread (see {@code McpServerFeatures.AsyncToolSpecification#
 * fromSync}), and that thread is reused across many unrelated tool calls. Leaving a session bound
 * after a call would leak it into whatever runs next on that thread, so binding always happens in
 * a try-with-resources together with this class.
 * </p>
 *
 * <p>
 * If no session is given (only possible for a tool whose
 * {@code McpToolProvider#requiresAuthentication()} is {@code false}), a fixed backend language ID
 * is set instead, the same way {@code PublishWorker}/{@code Publisher}/{@code JobController} (and
 * the annotation-scan tool path, {@code McpToolRegistry#invoke}) already do for their own
 * sessionless background work - without it, {@code ContentNodeHelper#getLanguageId()} has nothing
 * to return, which makes i18n-translated messages fall back to their raw, untranslated key.
 * </p>
 */
public final class McpSessionBinding implements AutoCloseable {
	/**
	 * Language ID used when no session is bound. Same convention as
	 * {@code com.gentics.contentnode.mcp.McpToolRegistry#BACKEND_LANGUAGE_ID}.
	 */
	private static final int BACKEND_LANGUAGE_ID = 2;

	private final Session previousSession;

	private final int previousLanguageId;

	/**
	 * Bind the given session (or, if {@code null}, the fixed backend language) to the current
	 * thread.
	 * @param session session to bind, or {@code null}
	 */
	public McpSessionBinding(Session session) {
		this.previousSession = ContentNodeHelper.getSession();
		this.previousLanguageId = ContentNodeHelper.getLanguageId(-1);

		ContentNodeHelper.setSession(session);
		if (session == null) {
			ContentNodeHelper.setLanguageId(BACKEND_LANGUAGE_ID);
		}
	}

	@Override
	public void close() {
		ContentNodeHelper.setSession(previousSession);
		ContentNodeHelper.setLanguageId(previousLanguageId);
	}
}
