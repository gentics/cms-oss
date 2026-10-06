package com.gentics.contentnode.mcp.util;

import java.util.List;
import java.util.Map;

import com.gentics.contentnode.mcp.McpArgs;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.lib.log.NodeLogger;

/**
 * Helpers shared by the write tools
 */
public final class Writes {
	private static final NodeLogger logger = NodeLogger.getNodeLogger(Writes.class);

	/**
	 * Name of the idempotency key argument
	 */
	public static final String ARG_IDEMPOTENCY_KEY = "idempotencyKey";

	/**
	 * Maximum length of the idempotency key
	 */
	public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

	/**
	 * Arguments that select a channel, which the write tools do not support
	 */
	static final List<String> CHANNEL_ARGS = List.of("nodeId", "channelId");

	private Writes() {
	}

	/**
	 * Reject a channel argument. Rejected rather than ignored: silently ignoring an explicit channel on a write
	 * could change the wrong channel variant.
	 * @param tool tool name
	 * @param arguments arguments
	 * @throws IllegalArgumentException if {@code nodeId} or {@code channelId} is given
	 */
	public static void rejectChannel(String tool, Map<String, Object> arguments) {
		for (String name : CHANNEL_ARGS) {
			if (arguments.containsKey(name)) {
				throw new IllegalArgumentException("%s does not yet support %s (multichannelling); call it without %s, "
						.formatted(tool, name, name) + "or wait for that support to be added.");
			}
		}
	}

	/**
	 * Validate and log the idempotency key. Calls are not de-duplicated.
	 * @param tool tool name
	 * @param id ID of the object the tool writes, or of its target folder
	 * @param arguments arguments
	 * @throws IllegalArgumentException if the key is too long
	 */
	public static void logIdempotencyKey(String tool, int id, Map<String, Object> arguments) {
		String key = McpArgs.stringArg(arguments, ARG_IDEMPOTENCY_KEY, 0, MAX_IDEMPOTENCY_KEY_LENGTH);
		if (key != null) {
			logger.info("%s for %d with idempotencyKey '%s'".formatted(tool, id, key));
		}
	}

	/**
	 * Load a page after a write, without locking it, with folder (for the node ID), language variants, workflow,
	 * translation status and version info
	 * @param pageResource page resource
	 * @param id page ID
	 * @return REST page
	 * @throws Exception if the page cannot be loaded
	 */
	public static Page readBack(PageResource pageResource, int id) throws Exception {
		PageLoadResponse response = pageResource.load(Integer.toString(id), false, false, true, true, false, true, true,
				true, false, false, null, null);
		if (response == null || response.getResponseInfo() == null
				|| response.getResponseInfo().getResponseCode() != ResponseCode.OK) {
			throw new IllegalStateException("Page %d could not be loaded after the write".formatted(id));
		}
		return response.getPage();
	}
}
