package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
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
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.FolderObjectCountResponse;
import com.gentics.contentnode.rest.model.response.LegacyFolderListResponse;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FolderListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Returns the folder tree of a node, starting at its root folder or at a given folder, down to a depth. Each level is
 * loaded with {@link FolderResourceImpl#getFolders} (folders the caller can view, not recursive), the start folder with
 * {@link FolderResourceImpl#load} and the root folder ID with {@link NodeResourceImpl#get}. The optional page counts
 * come from {@link FolderResourceImpl#getObjectCounts}.
 */
public class GetFolderTreeTool extends AbstractMcpTool {
	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_FOLDER_ID = "folderId";

	static final String ARG_DEPTH = "depth";

	static final String ARG_INCLUDE_COUNTS = "includeCounts";

	/**
	 * Default depth
	 */
	static final int DEFAULT_DEPTH = 2;

	/**
	 * Maximum depth
	 */
	static final int MAX_DEPTH = 5;

	/**
	 * Maximum number of folders in the tree
	 */
	static final int MAX_FOLDERS = 500;

	/**
	 * A folder in the tree. Fields that are not set are omitted on serialization.
	 * @param ref reference to the folder
	 * @param pageCount number of pages in the folder, if requested
	 * @param hasMoreChildren whether the folder has subfolders below the requested depth
	 * @param children subfolders within the requested depth
	 */
	@JsonInclude(Include.NON_NULL)
	public record FolderTreeNode(ObjectRef ref, Integer pageCount, boolean hasMoreChildren,
			List<FolderTreeNode> children) {
	}

	/**
	 * Result of the tool
	 * @param root the start folder
	 * @param tree its subfolders
	 * @param truncated whether folders were left out, because the tree has more than {@link #MAX_FOLDERS}
	 */
	public record Result(ObjectRef root, List<FolderTreeNode> tree, boolean truncated) {
	}

	/**
	 * Loads one level of subfolders
	 */
	interface Subfolders {
		/**
		 * Get the subfolders of a folder
		 * @param folderId folder ID
		 * @return subfolders
		 * @throws Exception
		 */
		List<Folder> of(int folderId) throws Exception;
	}

	/**
	 * Counts the pages of a folder
	 */
	interface PageCounter {
		/**
		 * Count the pages in a folder
		 * @param folderId folder ID
		 * @return page count
		 * @throws Exception
		 */
		int count(int folderId) throws Exception;
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_NODE_ID, schema("integer", "ID of the node.", "minimum", 1));
		properties.put(ARG_FOLDER_ID, schema("integer", "Folder to start at, default the node's root folder.",
				"minimum", 1));
		properties.put(ARG_DEPTH, schema("integer", "Number of folder levels below the start folder.", "minimum", 1,
				"maximum", MAX_DEPTH, "default", DEFAULT_DEPTH));
		properties.put(ARG_INCLUDE_COUNTS, schema("boolean",
				"Add pageCount per folder. Costs one extra CMS call per folder.", "default", false));

		return Tool.builder().name("get_folder_tree").title("Get folder tree")
				.description("Returns the folder structure of a node as a nested tree, so you can choose where a page "
						+ "belongs. Start at the node root by omitting folderId. Keep depth small: a deep tree of a "
						+ "real site will not fit in context. Do NOT use it to list pages or files, it returns folders "
						+ "only, use list_folder_items.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_NODE_ID)).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int nodeId = Args.id(arguments, ARG_NODE_ID);
		Integer folderId = intArg(arguments, ARG_FOLDER_ID, 1, Integer.MAX_VALUE);
		int depth = intArg(arguments, ARG_DEPTH, 1, MAX_DEPTH, DEFAULT_DEPTH);
		boolean includeCounts = booleanArg(arguments, ARG_INCLUDE_COUNTS, false);

		FolderResource folderResource = RestPermissions.guard(FolderResource.class, new FolderResourceImpl());
		if (folderId == null) {
			NodeLoadResponse node = RestPermissions.guard(NodeResource.class, new NodeResourceImpl())
					.get(Integer.toString(nodeId), false);
			requireOk(node, "The node %d could not be loaded".formatted(nodeId));
			folderId = node.getNode().getFolderId();
		}
		FolderLoadResponse start = folderResource.load(Integer.toString(folderId), false, false, false, nodeId, null);
		requireOk(start, "The folder %d could not be loaded".formatted(folderId));

		Subfolders subfolders = id -> {
			FolderListParameterBean folderListParams = new FolderListParameterBean();
			folderListParams.nodeId = nodeId;
			LegacyFolderListResponse response = folderResource.getFolders(Integer.toString(id), null, false,
					new InFolderParameterBean(), folderListParams, new LegacyFilterParameterBean(),
					new LegacySortParameterBean(), new LegacyPagingParameterBean(), new EditableParameterBean(),
					new WastebinParameterBean());
			requireOk(response, "The subfolders of folder %d could not be loaded".formatted(id));
			return response.getFolders();
		};
		PageCounter counter = !includeCounts ? null : id -> {
			FolderObjectCountResponse response = folderResource.getObjectCounts(id, nodeId, null, null,
					new InFolderParameterBean(), new WastebinParameterBean());
			requireOk(response, "The objects of folder %d could not be counted".formatted(id));
			return response.getPages();
		};

		return tree(ObjectRef.forFolder(start.getFolder()), folderId, depth, subfolders, counter);
	}

	/**
	 * Build the tree
	 * @param root ref of the start folder
	 * @param rootId ID of the start folder
	 * @param depth number of levels
	 * @param subfolders loads the subfolders of a folder
	 * @param counter counts the pages of a folder, null for no counts
	 * @return result
	 * @throws Exception
	 */
	static Result tree(ObjectRef root, int rootId, int depth, Subfolders subfolders, PageCounter counter)
			throws Exception {
		int[] budget = { MAX_FOLDERS };
		List<FolderTreeNode> tree = level(rootId, 1, depth, subfolders, counter, budget);
		return new Result(root, tree, budget[0] < 0);
	}

	/**
	 * Build one level of the tree
	 * @param folderId parent folder ID
	 * @param level level of the subfolders, 1 for the subfolders of the start folder
	 * @param depth number of levels
	 * @param subfolders loads the subfolders of a folder
	 * @param counter counts the pages of a folder, null for no counts
	 * @param budget remaining number of folders, below 0 once a folder was left out
	 * @return nodes of the level
	 * @throws Exception
	 */
	private static List<FolderTreeNode> level(int folderId, int level, int depth, Subfolders subfolders,
			PageCounter counter, int[] budget) throws Exception {
		List<FolderTreeNode> nodes = new ArrayList<>();
		for (Folder folder : subfolders.of(folderId)) {
			if (budget[0] <= 0) {
				budget[0] = -1;
				break;
			}
			budget[0]--;
			Integer pageCount = counter != null ? counter.count(folder.getId()) : null;
			if (level < depth) {
				nodes.add(new FolderTreeNode(ObjectRef.forFolder(folder), pageCount, false,
						level(folder.getId(), level + 1, depth, subfolders, counter, budget)));
			} else {
				nodes.add(new FolderTreeNode(ObjectRef.forFolder(folder), pageCount, folder.isHasSubfolders(),
						List.of()));
			}
		}
		return nodes;
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> nodeProperties = new LinkedHashMap<>();
		nodeProperties.put("ref", ObjectRef.jsonSchema(null));
		nodeProperties.put("pageCount", schema("integer", "Number of pages, with includeCounts."));
		nodeProperties.put("hasMoreChildren", schema("boolean", "Whether the folder has subfolders below depth."));
		nodeProperties.put("children", schema("array", "Subfolders, in the same shape.", "items",
				schema("object", null)));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("root", ObjectRef.jsonSchema("The start folder."));
		properties.put("tree", schema("array", "Subfolders of the start folder.", "items",
				schema("object", null, "properties", nodeProperties)));
		properties.put("truncated", schema("boolean",
				"Whether folders were left out because the tree has more than %d folders.".formatted(MAX_FOLDERS)));
		return schema("object", null, "properties", properties, "required", List.of("root", "tree"));
	}
}
