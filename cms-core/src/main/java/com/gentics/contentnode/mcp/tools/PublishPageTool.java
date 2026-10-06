package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.gentics.contentnode.rest.model.Workflow;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Publishes a page now or at a time. Delegates to {@link PageResourceImpl#publish}, which needs view and edit
 * permission and queues the page for approval if the caller may not publish it. That response does not say which
 * happened, so the result is derived from the page read back afterwards.
 */
public class PublishPageTool extends AbstractMcpTool {
	static final String NAME = "publish_page";

	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_AT = "at";

	static final String ARG_ALL_LANGUAGES = "allLanguages";

	static final String ARG_MESSAGE = "message";

	static final String ARG_KEEP_VERSION = "keepVersion";

	/**
	 * Approval the page waits for
	 * @param required always true
	 * @param groups groups that may approve, with a multilevel workflow only
	 * @param message message for the approvers, with a multilevel workflow only
	 */
	@JsonInclude(Include.NON_NULL)
	public record Approval(boolean required, List<ObjectRef> groups, String message) {
	}

	/**
	 * Result of the tool
	 * @param ref reference to the page
	 * @param result published, queued_for_approval or scheduled
	 * @param online whether the page is online
	 * @param publishAt time the page is (or is queued to be) published at
	 * @param approval approval the page waits for
	 * @param messages messages of the CMS
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(ObjectRef ref, String result, boolean online, Integer publishAt, Approval approval,
			List<String> messages) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PAGE_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Not supported yet: calls that set it are rejected.", "minimum",
				1));
		properties.put(ARG_AT, schema("integer", "Unix timestamp in seconds to publish at. Omit to publish now.",
				"minimum", 0));
		properties.put(ARG_ALL_LANGUAGES, schema("boolean", "Also publish the other language variants.", "default",
				false));
		properties.put(ARG_MESSAGE, schema("string", "Note for the approval queue, shown to the approver.",
				"maxLength", 2000));
		properties.put(ARG_KEEP_VERSION, schema("boolean",
				"With at: publish the current version at that time, not the one current then.", "default", false));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Publish page")
				.description("Publishes a page now, or schedules it for a timestamp. The CMS decides from the acting "
						+ "user's permissions and the configured workflow whether the page goes online directly or "
						+ "enters the approval queue, so ALWAYS read result from the response and tell the user which "
						+ "happened: never report a page as live when result is queued_for_approval. Check publish "
						+ "permission with get_permissions first, and verify the content with render_preview before "
						+ "publishing. Mandatory tags left empty make it fail with invalid-data.")
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
		PagePublishRequest request = new PagePublishRequest();
		request.setAt(at);
		request.setAlllang(booleanArg(arguments, ARG_ALL_LANGUAGES, false));
		request.setMessage(stringArg(arguments, ARG_MESSAGE, 0, 2000));
		request.setKeepVersion(booleanArg(arguments, ARG_KEEP_VERSION, false));
		Writes.logIdempotencyKey(NAME, id, arguments);

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		GenericResponse response = pageResource.publish(Integer.toString(id), null, request);
		requireOk(response, "Page %d was not published".formatted(id));

		return result(Writes.readBack(pageResource, id), at, messages(response));
	}

	/**
	 * Derive the result from the page after the publish call
	 * @param page page read back
	 * @param at requested publish time, 0 for now
	 * @param messages messages of the CMS
	 * @return result
	 */
	static Result result(Page page, int at, List<String> messages) {
		Workflow workflow = page.getWorkflow();
		TimeManagement time = page.getTimeManagement();
		Integer queuedAt = time != null && time.getQueuedPublish() != null ? time.getQueuedPublish().getAt() : null;
		Integer publishAt = time != null && positive(time.getAt()) != null ? time.getAt() : positive(queuedAt);

		boolean queued = workflow != null || (time != null ? time.getQueuedPublish() != null : page.isQueued());
		if (queued) {
			Approval approval = workflow != null
					? new Approval(true, groups(workflow), workflow.getMessage())
					: new Approval(true, null, null);
			return new Result(ObjectRef.forPage(page), "queued_for_approval", page.isOnline(), publishAt, approval,
					messages);
		}
		return new Result(ObjectRef.forPage(page), at > 0 ? "scheduled" : "published", page.isOnline(),
				at > 0 ? publishAt : null, null, messages);
	}

	/**
	 * Get the messages of a CMS response
	 * @param response response
	 * @return message texts
	 */
	static List<String> messages(GenericResponse response) {
		return response.getMessages() == null ? List.of()
				: response.getMessages().stream().map(Message::getMessage).filter(Objects::nonNull).toList();
	}

	/**
	 * Get the groups of a workflow as refs
	 * @param workflow workflow
	 * @return refs, null if the workflow has no groups
	 */
	private static List<ObjectRef> groups(Workflow workflow) {
		return workflow.getGroups() == null || workflow.getGroups().isEmpty() ? null
				: workflow.getGroups().stream().map(group -> new ObjectRef(ObjectRef.Type.GROUP, group.getId(), null,
						null, group.getName(), null, null, null, null)).toList();
	}

	/**
	 * Get a timestamp if it is set
	 * @param timestamp timestamp, may be null
	 * @return timestamp, null if not positive
	 */
	static Integer positive(Integer timestamp) {
		return timestamp != null && timestamp > 0 ? timestamp : null;
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> approvalProperties = new LinkedHashMap<>();
		approvalProperties.put("required", schema("boolean", null));
		approvalProperties.put("groups", schema("array", "Groups that may approve, with a multilevel workflow.",
				"items", ObjectRef.jsonSchema(null)));
		approvalProperties.put("message", schema("string", "Message for the approvers, with a multilevel workflow."));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema("The page."));
		properties.put("result", schema("string", null, "enum", List.of("published", "queued_for_approval",
				"scheduled")));
		properties.put("online", schema("boolean", null));
		properties.put("publishAt", schema("integer", "Unix timestamp in seconds."));
		properties.put("approval", schema("object", null, "properties", approvalProperties));
		properties.put("messages", schema("array", null, "items", schema("string", null)));
		return schema("object", null, "properties", properties, "required", List.of("ref", "result"));
	}
}
