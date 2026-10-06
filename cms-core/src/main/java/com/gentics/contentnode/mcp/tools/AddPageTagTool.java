package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.TagInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.Constructs;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.request.ContentTagCreateRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.TagCreateResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Adds one content tag to a page, from a construct or as a copy of a tag of another page. Delegates to
 * {@link PageResourceImpl#createTag}, which checks the edit permission on the page (and view on the copy source) and
 * leaves the page locked, so the tool releases the lock. An unknown construct keyword or copy source, which the CMS
 * reports as a plain failure, is looked up before.
 */
public class AddPageTagTool extends AbstractMcpTool {
	static final String NAME = "add_page_tag";

	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_CONSTRUCT_KEYWORD = "constructKeyword";

	static final String ARG_CONSTRUCT_ID = "constructId";

	static final String ARG_TAG_NAME = "tagName";

	static final String ARG_COPY_FROM = "copyFrom";

	/**
	 * Result of the tool
	 * @param tag the new tag
	 * @param pageRef reference to the page
	 */
	public record Result(TagInfo tag, ObjectRef pageRef) {
	}

	/**
	 * Validated arguments. Exactly one of constructKeyword, constructId and copySource is set.
	 * @param pageId page ID
	 * @param constructKeyword construct keyword, may be null
	 * @param constructId construct ID, may be null
	 * @param copyPageId ID of the page to copy the tag from, may be null
	 * @param copyTagName name of the tag to copy, set with copyPageId
	 */
	record Request(int pageId, String constructKeyword, Integer constructId, Integer copyPageId, String copyTagName) {
		/**
		 * Validate the arguments
		 * @param arguments arguments
		 * @return request
		 * @throws IllegalArgumentException for missing, invalid or conflicting arguments
		 */
		static Request of(Map<String, Object> arguments) {
			int pageId = Args.id(arguments, ARG_PAGE_ID);
			if (arguments.containsKey(ARG_TAG_NAME)) {
				throw new IllegalArgumentException("'%s' is not supported, the CMS generates the name of a new tag. "
						.formatted(ARG_TAG_NAME)
						+ "To create a tag under a given name, use update_page_tags with createMissing.");
			}
			String keyword = nullIfBlank(stringArg(arguments, ARG_CONSTRUCT_KEYWORD, 0, 100));
			Integer constructId = intArg(arguments, ARG_CONSTRUCT_ID, 1, Integer.MAX_VALUE);

			Object copyFrom = arguments.get(ARG_COPY_FROM);
			if (copyFrom != null) {
				if (keyword != null || constructId != null) {
					throw new IllegalArgumentException("Pass either '%s' or a construct, not both"
							.formatted(ARG_COPY_FROM));
				}
				if (!(copyFrom instanceof Map<?, ?> copy)) {
					throw new IllegalArgumentException("Argument '%s' must be an object".formatted(ARG_COPY_FROM));
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> source = (Map<String, Object>) copy;
				String tagName = nullIfBlank(stringArg(source, "tagName", 0, 100));
				if (tagName == null) {
					throw new IllegalArgumentException(
							"Missing required argument '%s.tagName'".formatted(ARG_COPY_FROM));
				}
				return new Request(pageId, null, null, Args.id(source, "pageId"), tagName);
			}

			if ((keyword == null) == (constructId == null)) {
				throw new IllegalArgumentException("Pass exactly one of '%s' and '%s'"
						.formatted(ARG_CONSTRUCT_KEYWORD, ARG_CONSTRUCT_ID));
			}
			return new Request(pageId, keyword, constructId, null, null);
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> copyProperties = new LinkedHashMap<>();
		copyProperties.put("pageId", schema("integer", "ID of the page to copy from.", "minimum", 1));
		copyProperties.put("tagName", schema("string", "Name of the tag to copy.", "maxLength", 100));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PAGE_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Not supported yet: calls that set it are rejected.", "minimum",
				1));
		properties.put(ARG_CONSTRUCT_KEYWORD, schema("string", "Keyword of the construct.", "maxLength", 100));
		properties.put(ARG_CONSTRUCT_ID, schema("integer", "ID of the construct.", "minimum", 1));
		properties.put(ARG_TAG_NAME, schema("string",
				"Not supported: the CMS generates the name. Use update_page_tags with createMissing for a given name.",
				"maxLength", 100));
		properties.put(ARG_COPY_FROM, schema("object",
				"Copy an existing tag, with its values, from another page, instead of a construct.", "properties",
				copyProperties, "required", List.of("pageId", "tagName"), "additionalProperties", false));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Add page tag")
				.description("Adds one new content block of a given construct to a page and returns its generated tag "
						+ "name, which you then fill with update_page_tags. Check with list_constructs that the "
						+ "construct is available in the page's node first. Calling this twice adds two blocks, so do "
						+ "not retry it blindly after a timeout: read the page with get_page_tags instead. Pass exactly "
						+ "one of constructKeyword or constructId, or copyFrom; anything else is refused as "
						+ "invalid-data.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_PAGE_ID))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Writes.rejectChannel(NAME, arguments);
		Request request = Request.of(arguments);
		Writes.logIdempotencyKey(NAME, request.pageId(), arguments);

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		ContentTagCreateRequest create = new ContentTagCreateRequest();
		Integer constructId = request.constructId();
		if (request.constructKeyword() != null) {
			try (Trx trx = ContentNodeHelper.trx()) {
				constructId = Constructs.byKeyword(request.constructKeyword()).getId();
				trx.success();
			}
		} else if (request.copyPageId() != null) {
			checkCopySource(pageResource, request.copyPageId(), request.copyTagName());
			create.setCopyPageId(Integer.toString(request.copyPageId()));
			create.setCopyTagname(request.copyTagName());
		}

		String id = Integer.toString(request.pageId());
		Tag tag;
		try {
			TagCreateResponse response = pageResource.createTag(id, constructId, null, create);
			requireOk(response, "No tag was added to page %s".formatted(id));
			tag = response.getTag();
		} finally {
			// createTag() leaves the page locked
			releaseLock(Page.class, id);
		}

		return new Result(TagInfo.of(tag, TagInfo.constructKeywords(List.of(tag)), false),
				ObjectRef.of(ObjectRef.Type.PAGE, request.pageId()));
	}

	/**
	 * Check that the copy source has a content tag of the name. The CMS fails with a plain error otherwise.
	 * @param pageResource page resource
	 * @param pageId ID of the source page
	 * @param tagName tag name
	 * @throws Exception
	 * @throws EntityNotFoundException if the page has no such content tag
	 */
	private static void checkCopySource(PageResource pageResource, int pageId, String tagName) throws Exception {
		PageLoadResponse source = pageResource.load(Integer.toString(pageId), false, false, false, false, false, false,
				false, false, false, false, null, null);
		requireOk(source, "The page %d to copy from could not be loaded".formatted(pageId));
		Tag tag = source.getPage().getTags() != null ? source.getPage().getTags().get(tagName) : null;
		if (tag == null || tag.getType() != Tag.Type.CONTENTTAG) {
			throw new EntityNotFoundException("Page %d has no content tag '%s'".formatted(pageId, tagName));
		}
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("tag", TagInfo.jsonSchema());
		properties.put("pageRef", ObjectRef.jsonSchema("The page."));
		return schema("object", null, "properties", properties, "required", List.of("tag"));
	}
}
