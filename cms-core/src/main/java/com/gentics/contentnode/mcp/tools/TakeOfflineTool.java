package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.TimeManagement;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Takes a page offline now or at a time. Delegates to {@link PageResourceImpl#takeOffline}, which needs view and edit
 * permission and queues the request for approval if the caller may not publish the page. The result is derived from the
 * page read back afterwards.
 */
public class TakeOfflineTool extends AbstractMcpTool {
	static final String NAME = "take_offline";

	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_AT = "at";

	static final String ARG_ALL_LANGUAGES = "allLanguages";

	/**
	 * Result of the tool
	 * @param ref reference to the page
	 * @param online whether the page is still online
	 * @param offlineAt time the page is (or is queued to be) taken offline at
	 * @param result offline, scheduled or queued_for_approval
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ObjectRef ref, boolean online, Integer offlineAt, String result) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PAGE_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Not supported yet: calls that set it are rejected.", "minimum",
				1));
		properties.put(ARG_AT, schema("integer", "Unix timestamp in seconds to take the page offline at. Omit for now.",
				"minimum", 0));
		properties.put(ARG_ALL_LANGUAGES, schema("boolean", "Also take the other language variants offline.",
				"default", false));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Take offline")
				.description("Unpublishes a page immediately or at a timestamp. The page and its content remain in "
						+ "the CMS; only the published version is withdrawn. Without publish permission the request "
						+ "is queued for approval: read result and tell the user. Check with get_related that nothing "
						+ "important links to the page before you do this. It is not a delete, use delete_page for "
						+ "that.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_PAGE_ID))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Writes.rejectChannel(NAME, arguments);
		int id = Args.id(arguments, ARG_PAGE_ID);
		int at = intArg(arguments, ARG_AT, 0, Integer.MAX_VALUE, 0);
		PageOfflineRequest request = new PageOfflineRequest();
		request.setAt(at);
		request.setAlllang(booleanArg(arguments, ARG_ALL_LANGUAGES, false));
		Writes.logIdempotencyKey(NAME, id, arguments);

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		requireOk(pageResource.takeOffline(Integer.toString(id), request),
				"Page %d was not taken offline".formatted(id));

		return result(Writes.readBack(pageResource, id), at);
	}

	/**
	 * Derive the result from the page after the call
	 * @param page page read back
	 * @param at requested time, 0 for now
	 * @return result
	 */
	static Result result(Page page, int at) {
		TimeManagement time = page.getTimeManagement();
		Integer queuedAt = time != null && time.getQueuedOffline() != null ? time.getQueuedOffline().getAt() : null;
		Integer offlineAt = time != null && PublishPageTool.positive(time.getOfflineAt()) != null ? time.getOfflineAt()
				: PublishPageTool.positive(queuedAt);

		String result;
		if (time != null && time.getQueuedOffline() != null) {
			result = "queued_for_approval";
		} else if (at > 0) {
			result = "scheduled";
		} else {
			result = "offline";
			offlineAt = null;
		}
		return new Result(ObjectRef.forPage(page), page.isOnline(), offlineAt, result);
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema("The page."));
		properties.put("online", schema("boolean", null));
		properties.put("offlineAt", schema("integer", "Unix timestamp in seconds."));
		properties.put("result", schema("string", "offline, scheduled, or queued_for_approval if the caller may not "
				+ "publish the page.", "enum", List.of("offline", "scheduled", "queued_for_approval")));
		return schema("object", null, "properties", properties, "required", List.of("ref", "online", "result"));
	}
}
