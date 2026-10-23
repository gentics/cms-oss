package com.gentics.contentnode.mcp.util;

import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.McpArgs;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
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
	 * Name of the argument that selects the node (or channel) a write acts in
	 */
	public static final String ARG_NODE_ID = "nodeId";

	private Writes() {
	}

	/**
	 * Resolve the page argument in the node given by {@code nodeId}: the page as that node sees it, which is its
	 * localized copy if the node has one. A page the node only inherits is refused, because a write would change it in
	 * its master node and in every channel inheriting it.
	 * @param tool tool name
	 * @param arguments arguments
	 * @param pageArg name of the page ID argument
	 * @return the arguments with the ID of the resolved page, unchanged without {@code nodeId}
	 * @throws NodeException
	 * @throws IllegalArgumentException for an invalid argument or a page the node inherits
	 * @throws EntityNotFoundException if the node, or the page in it, does not exist
	 */
	public static Map<String, Object> pageInNode(String tool, Map<String, Object> arguments, String pageArg)
			throws NodeException {
		Integer nodeId = McpArgs.intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
		if (nodeId == null) {
			return arguments;
		}
		int pageId = Args.id(arguments, pageArg);
		int resolved = pageInNode(tool, pageId, nodeId, false);
		if (resolved == pageId) {
			return arguments;
		}
		Map<String, Object> changed = new HashMap<>(arguments);
		changed.put(pageArg, resolved);
		return changed;
	}

	/**
	 * Get the ID of a page as a node sees it, which is its localized copy if the node has one
	 * @param tool tool name
	 * @param pageId page ID
	 * @param nodeId node ID
	 * @param inherited true to accept a page the node inherits from a master node
	 * @return page ID in the node
	 * @throws NodeException
	 * @throws IllegalArgumentException for a page the node inherits, unless accepted
	 * @throws EntityNotFoundException if the node, or the page in it, does not exist
	 */
	public static int pageInNode(String tool, int pageId, int nodeId, boolean inherited) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			Node node = node(t, nodeId);
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				Page page = t.getObject(Page.class, pageId);
				if (page == null || !t.canView(page) || !inNode(page.getOwningNode(), node)) {
					throw new EntityNotFoundException("Page %d does not exist in node %d".formatted(pageId, nodeId));
				}
				if (!inherited && page.isInherited()) {
					int masterId = page.getChannel() != null ? page.getChannel().getId() : page.getOwningNode().getId();
					throw new IllegalArgumentException(("Page %d is inherited by node %d from node %d, so %s would "
							+ "change it there and in every channel that inherits it. Call %s with nodeId %d to change "
							+ "it for all of them, or have the page localized in node %d first.").formatted(pageId,
									nodeId, masterId, tool, tool, masterId, nodeId));
				}
				int id = page.getId();
				trx.success();
				return id;
			}
		}
	}

	/**
	 * Check that a folder is visible in a node: the folder's node is the node itself or one of its master nodes
	 * @param folderId folder ID
	 * @param nodeId node ID
	 * @throws NodeException
	 * @throws EntityNotFoundException if the node, or the folder in it, does not exist
	 */
	public static void folderInNode(int folderId, int nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			Node node = node(t, nodeId);
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				Folder folder = t.getObject(Folder.class, folderId);
				if (folder == null || !t.canView(folder) || !inNode(folder.getOwningNode(), node)) {
					throw new EntityNotFoundException(
							"Folder %d does not exist in node %d".formatted(folderId, nodeId));
				}
			}
			trx.success();
		}
	}

	/**
	 * Load a node
	 * @param t transaction
	 * @param nodeId node ID
	 * @return node
	 * @throws NodeException
	 * @throws EntityNotFoundException if the node does not exist
	 */
	private static Node node(Transaction t, int nodeId) throws NodeException {
		Node node = t.getObject(Node.class, nodeId);
		if (node == null) {
			throw new EntityNotFoundException("Node %d does not exist, see list_nodes".formatted(nodeId));
		}
		return node;
	}

	/**
	 * Check whether objects of a node are visible in another node, which is the node itself or one of its channels
	 * @param owner node of the objects
	 * @param node node
	 * @return true if they are
	 * @throws NodeException
	 */
	private static boolean inNode(Node owner, Node node) throws NodeException {
		return owner != null && (owner.equals(node) || node.isChannelOf(owner));
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
	 * @param nodeId node (channel) to load the page in, may be null
	 * @return REST page
	 * @throws Exception if the page cannot be loaded
	 */
	public static com.gentics.contentnode.rest.model.Page readBack(PageResource pageResource, int id, Integer nodeId)
			throws Exception {
		PageLoadResponse response = pageResource.load(Integer.toString(id), false, false, true, true, false, true, true,
				true, false, false, nodeId, null);
		if (response == null || response.getResponseInfo() == null
				|| response.getResponseInfo().getResponseCode() != ResponseCode.OK) {
			throw new IllegalStateException("Page %d could not be loaded after the write".formatted(id));
		}
		return response.getPage();
	}
}
