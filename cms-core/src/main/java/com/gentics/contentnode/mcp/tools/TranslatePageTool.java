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
import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.MiscUtils;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Creates the language variant of a page, or returns the existing one. Delegates to {@link PageResourceImpl#translate}
 * without locking the variant (whatever {@code locked} says, so the call leaves no lock behind), which checks the
 * permission to translate and edit pages of that language in the folder. {@code created} is taken from the source
 * page's language variants before the call, and a language the node does not have is rejected before.
 */
public class TranslatePageTool extends AbstractMcpTool {
	static final String NAME = "translate_page";

	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_LANGUAGE = "language";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_CHANNEL_ID = "channelId";

	static final String ARG_LOCKED = "locked";

	/**
	 * Result of the tool
	 * @param page the language variant, unlocked
	 * @param sourceRef the source page
	 * @param created whether the variant was created by this call
	 */
	public record Result(PageInfo page, ObjectRef sourceRef, boolean created) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PAGE_ID, schema("integer", "ID of the source page.", "minimum", 1));
		properties.put(ARG_LANGUAGE, schema("string", "Code of the target language, one of the node's languages.",
				"minLength", 2, "maxLength", 10));
		properties.put(ARG_NODE_ID, schema("integer", "Not supported yet: calls that set it are rejected.", "minimum",
				1));
		properties.put(ARG_CHANNEL_ID, schema("integer", "Not supported yet: calls that set it are rejected.",
				"minimum", 1));
		properties.put(ARG_LOCKED, schema("boolean",
				"Ignored: the variant is always returned unlocked, update_page_tags takes the lock itself.", "default",
				true));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Translate page")
				.description("Creates the language variant of a page, or opens the existing one, as the starting point "
						+ "for a translation. It copies the source content into the new language variant; it does not "
						+ "translate anything. Write the translated text afterwards with update_page_tags. Check "
						+ "list_nodes that the node actually supports the target language. created tells whether the "
						+ "variant is new.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_PAGE_ID, ARG_LANGUAGE)).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Writes.rejectChannel(NAME, arguments);
		int id = Args.id(arguments, ARG_PAGE_ID);
		String language = stringArg(arguments, ARG_LANGUAGE, 2, 10);
		if (language == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_LANGUAGE));
		}
		// validated, but ignored
		booleanArg(arguments, ARG_LOCKED, true);
		Writes.logIdempotencyKey(NAME, id, arguments);

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		PageLoadResponse source = pageResource.load(Integer.toString(id), false, false, true, true, false, false,
				false, false, false, false, null, null);
		requireOk(source, "The page %d could not be loaded".formatted(id));

		checkLanguage(id, language);

		PageLoadResponse translated = pageResource.translate(id, language, false, 0);
		requireOk(translated, "Page %d was not translated into '%s'".formatted(id, language));

		return new Result(PageInfo.of(Writes.readBack(pageResource, translated.getPage().getId())),
				ObjectRef.forPage(source.getPage()), !hasVariant(source.getPage(), language));
	}

	/**
	 * Check that the page's node has the language. The CMS reports a missing one as a general failure.
	 * @param id page ID, of a page the caller can view
	 * @param language language code
	 * @throws Exception
	 * @throws IllegalArgumentException if the node has no such language
	 */
	private static void checkLanguage(int id, String language) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			com.gentics.contentnode.object.Page page = trx.getTransaction()
					.getObject(com.gentics.contentnode.object.Page.class, id);
			if (MiscUtils.getRequestedContentLanguage(page, language) == null) {
				List<String> codes = page.getFolder().getNode().getLanguages().stream().map(ContentLanguage::getCode)
						.toList();
				throw new IllegalArgumentException("The node of page %d has no language '%s', it has %s"
						.formatted(id, language, codes));
			}
			trx.success();
		}
	}

	/**
	 * Check whether a page is in, or has a variant in, a language
	 * @param page REST page, loaded with its language variants
	 * @param language language code
	 * @return true if it has
	 */
	static boolean hasVariant(Page page, String language) {
		if (language.equals(page.getLanguage())) {
			return true;
		}
		if (page.getLanguageVariants() != null) {
			for (Page variant : page.getLanguageVariants().values()) {
				if (language.equals(variant.getLanguage())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("page", PageInfo.jsonSchema("The language variant, unlocked."));
		properties.put("sourceRef", ObjectRef.jsonSchema("The source page."));
		properties.put("created", schema("boolean", "Whether the variant was created by this call."));
		return schema("object", null, "properties", properties, "required", List.of("page"));
	}
}
