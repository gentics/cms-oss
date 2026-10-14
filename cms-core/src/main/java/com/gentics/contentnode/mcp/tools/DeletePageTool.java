package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Moves a page to the wastebin. Refuses when the page's node has no wastebin, because the CMS would then delete the
 * page permanently. Delegates to {@link PageResourceImpl#delete}, which checks the delete permission.
 */
public class DeletePageTool extends AbstractMcpTool {
	static final String NAME = "delete_page";

	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_NODE_ID = "nodeId";

	/**
	 * Result of the tool
	 * @param ref reference to the deleted page
	 * @param deleted always true
	 * @param restorable always true, the page is in the wastebin
	 */
	public record Result(ObjectRef ref, boolean deleted, boolean restorable) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PAGE_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to change the page in: the page as that node "
				+ "sees it, its localized copy if the node has one. A page the node only inherits is refused.",
				"minimum", 1));

		return Tool.builder().name(NAME).title("Delete page")
				.description("Moves a page to the wastebin, where it remains restorable by a human in the CMS. It is "
						+ "not a permanent delete and this tool cannot purge anything; on a node without wastebin it "
						+ "refuses. Confirm with the user before calling it, and check get_related first: other pages "
						+ "may link to this one.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_PAGE_ID))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		arguments = Writes.pageInNode(NAME, arguments, ARG_PAGE_ID);
		int id = Args.id(arguments, ARG_PAGE_ID);
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		// the ref is taken before the delete, and the load checks that the caller can view the page
		PageLoadResponse loaded = pageResource.load(Integer.toString(id), false, false, true, false, false, false,
				false, false, false, false, nodeId, null);
		requireOk(loaded, "The page %d could not be loaded".formatted(id));
		ObjectRef ref = ObjectRef.forPage(loaded.getPage());

		try (Trx trx = ContentNodeHelper.trx()) {
			Page page = trx.getTransaction().getObject(Page.class, id);
			if (!page.isMaster()) {
				throw new IllegalArgumentException("Page %d is a localized copy, which the CMS cannot delete. "
						.formatted(id) + "Have it unlocalized in the CMS instead.");
			}
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.WASTEBIN, page.getOwningNode())) {
				throw new IllegalArgumentException("The node of page %d has no wastebin, so the page would be deleted "
						.formatted(id) + "permanently. delete_page only moves pages to the wastebin.");
			}
			trx.success();
		}

		requireOk(pageResource.delete(Integer.toString(id), nodeId, null), "Page %d was not deleted".formatted(id));
		return new Result(ref, true, true);
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema("The deleted page."));
		properties.put("deleted", schema("boolean", "Always true."));
		properties.put("restorable", schema("boolean", "Always true: the page is in the wastebin."));
		return schema("object", null, "properties", properties, "required", List.of("ref", "deleted"));
	}
}
