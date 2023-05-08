package com.gentics.contentnode.factory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Page.OnlineStatusChange;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.lib.db.IntegerColumnRetriever;
import com.gentics.lib.db.SQLExecutor;

/**
 * Prepared Publish Data
 */
public class PublishData {
	/**
	 * Map of nodeId -> collection of direct channels
	 */
	protected Map<Integer, Collection<Node>> channels = new HashMap<Integer, Collection<Node>>();

	/**
	 * Map of nodeId -> list of master nodes
	 */
	protected Map<Integer, List<Node>> masterNodes = new HashMap<Integer, List<Node>>();

	/**
	 * Languages per Node
	 */
	protected Map<Integer, Map<Integer, Integer>> nodeLanguages = new HashMap<Integer, Map<Integer,Integer>>();

	/**
	 * Map of channelsetId -> (Map of channelId -> pageId)
	 */
	protected Map<Integer, Map<Integer, Integer>> pageChannelSets = new HashMap<Integer, Map<Integer, Integer>>();

	/**
	 * Map of channelsetId -> (Map of channelId -> templateId)
	 */
	protected Map<Integer, Map<Integer, Integer>> templateChannelSets = new HashMap<Integer, Map<Integer, Integer>>();

	/**
	 * Map of contentsetId -> List of pageIds
	 */
	protected Map<Integer, List<Integer>> pageLangVariants = new HashMap<Integer, List<Integer>>();

	/**
	 * Map of contentId -> List of pageId
	 */
	protected Map<Integer, List<Integer>> pageVariants = new HashMap<Integer, List<Integer>>();

	/**
	 * Map of pageId -> online flag
	 */
	protected Map<Integer, Boolean> pageOnline = new HashMap<Integer, Boolean>();

	/**
	 * Map of pageId -> folderId
	 */
	protected Map<Integer, Integer> pageFolderIds = new HashMap<Integer, Integer>();

	/**
	 * Map of constructId -> construct
	 */
	protected Map<Integer, Construct> constructs = new HashMap<Integer, Construct>();

	/**
	 * Map of partId -> part
	 */
	protected Map<Integer, Part> parts = new HashMap<Integer, Part>();

	/**
	 * Create an instance. Collect and organize some publish data
	 * @throws NodeException
	 */
	public PublishData() throws NodeException {
		this(true, true, true, true);
	}

	/**
	 * Create an instance. Collect and organize some publish data
	 * @param channels true to prepare channels
	 * @param templates true to prepare templates
	 * @param pages true to prepare pages
	 * @param constructs true to prepare constructs
	 * @throws NodeException
	 */
	public PublishData(boolean channels, boolean templates, boolean pages, boolean constructs) throws NodeException {
		try (ChannelTrx cTrx = new ChannelTrx()) {
			if (channels) {
				prepareChannels();
			}
			if (templates) {
				prepareTemplateData();
			}
			if (pages) {
				preparePageData();
			}
			if (constructs) {
				prepareConstructsAndParts();
			}
		}
	}

	/**
	 * Get the list of immediate channels of the given node
	 * @param node node
	 * @return list of channels
	 * @throws NodeException
	 */
	public Collection<Node> getChannels(Node node) throws NodeException {
		Integer nodeId = ObjectTransformer.getInteger(node.getId(), null);
		if (nodeId == null) {
			throw new NodeException("Cannot get channels for node without ID");
		}

		return Collections.unmodifiableCollection(channels.get(nodeId));
	}

	/**
	 * Get the list of master nodes for the given node
	 * @param node node
	 * @return list of master nodes
	 * @throws NodeException
	 */
	public List<Node> getMasterNodes(Node node) throws NodeException {
		Integer nodeId = ObjectTransformer.getInteger(node.getId(), null);
		if (nodeId == null) {
			throw new NodeException("Cannot get master nodes for node without ID");
		}

		return Collections.unmodifiableList(masterNodes.get(nodeId));
	}

	/**
	 * Get the channelset for the given page
	 * @param page page
	 * @return channelset
	 * @throws NodeException
	 */
	public Map<Integer, Integer> getChannelset(Page page) throws NodeException {
		return pageChannelSets.get(page.getChannelSetId());
	}

	/**
	 * Get the channelset for the given template
	 * @param template template
	 * @return channelset
	 * @throws NodeException
	 */
	public Map<Integer, Integer> getChannelset(Template template) throws NodeException {
		return templateChannelSets.get(template.getChannelSetId());
	}

	/**
	 * Get pageIds of pages, that are hiding this page
	 * @param page page
	 * @return map of pageIds -> channelIds
	 * @throws NodeException
	 */
	public Map<Integer, Integer> getHidingPageIds(Page page) throws NodeException {
		Node channel = page.getChannel();
		if (channel == null) {
			// if the page does not belong to a channel (i.e. belongs to the top node), no other page can hide it
			return Collections.emptyMap();
		}
		Map<Integer, Integer> channelSet = getChannelset(page);
		List<Node> masters = getMasterNodes(channel);
		Map<Integer, Integer> hidingPageIds = new HashMap<Integer, Integer>();

		Integer pageId = channelSet.get(0);
		if (pageId != null) {
			hidingPageIds.put(0, pageId);
		}

		for (Node node : masters) {
			Integer nodeId = node.getId();
			pageId = channelSet.get(nodeId);
			if (pageId != null) {
				hidingPageIds.put(nodeId, pageId);
			}
		}

		return hidingPageIds;
	}

	/**
	 * Get pageIds of pages, that are hidden by this page
	 * @param page page
	 * @return map of pageIds -> channelIds
	 * @throws NodeException
	 */
	public Map<Integer, Integer> getHiddenPageIds(Page page) throws NodeException {
		Map<Integer, Integer> hiddenPageIds = new HashMap<Integer, Integer>();

		Node channel = page.getChannel();
		List<Integer> masterNodeIds = new ArrayList<Integer>();

		masterNodeIds.add(0);
		if (channel != null) {
			List<Node> masterNodes = getMasterNodes(channel);

			for (Node node : masterNodes) {
				masterNodeIds.add(ObjectTransformer.getInteger(node.getId(), null));
			}
		}

		Map<Integer, Integer> channelSet = getChannelset(page);
		for (Map.Entry<Integer, Integer> entry : channelSet.entrySet()) {
			Integer channelId = entry.getKey();
			Integer pageId = entry.getValue();

			if (masterNodeIds.contains(channelId)) {
				hiddenPageIds.put(channelId, pageId);
			}
		}

		return hiddenPageIds;
	}

	/**
	 * Get the language variants of the given page
	 * @param page page
	 * @param nodeId id of a node, if the languages of that node shall be considered
	 * @return list of language variants
	 * @throws NodeException
	 */
	public List<Page> getLanguageVariants(Page page, Integer nodeId) throws NodeException {
		List<Integer> contentSet = pageLangVariants.get(page.getContentsetId());
		if (ObjectTransformer.isEmpty(contentSet)) {
			return Collections.emptyList();
		}

		// if the node languages shall be considered, we transform the nodeId to
		// the master nodeId (because languages are assigned to the master)
		if (nodeId != null) {
			List<Node> masters = masterNodes.get(nodeId);
			if (!ObjectTransformer.isEmpty(masters)) {
				nodeId = ObjectTransformer.getInteger(masters.get(masters.size() - 1).getId(), nodeId);
			}

			// we check now, whether the master node has any languages assigned, if not -> there are not language variants
			if (!nodeLanguages.containsKey(nodeId)) {
				return Collections.emptyList();
			}
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		List<Page> languageVariants = t.getObjects(Page.class, contentSet);

		if (nodeId != null) {

			languageVariants = new ArrayList<Page>(languageVariants);
			final Map<Integer, Integer> languageMap = nodeLanguages.get(nodeId);

			// filter by node languages
			for (Iterator<Page> i = languageVariants.iterator(); i.hasNext(); ) {
				Page langPage = i.next();
				if (!languageMap.containsKey(langPage.getLanguageId())) {
					i.remove();
				}
			}

			// sort by node languages
			Collections.sort(languageVariants, new Comparator<Page>() {
				public int compare(Page page1, Page page2) {
					int sort1 = ObjectTransformer.getInt(languageMap.get(page1.getLanguageId()), 0);
					int sort2 = ObjectTransformer.getInt(languageMap.get(page2.getLanguageId()), 0);
					return sort1 - sort2;
				}
			});
		}

		return languageVariants;
	}

	/**
	 * Get the page variants for the given page
	 * @param page page
	 * @return page variants
	 * @throws NodeException
	 */
	public List<Page> getPageVariants(Page page) throws NodeException {
		List<Integer> pageIds = pageVariants.get(page.getContent().getId());
		if (pageIds == null) {
			return Collections.singletonList(page);
		} else {
			return TransactionManager.getCurrentTransaction().getObjects(Page.class, pageIds);
		}
	}

	/**
	 * Get whether the page is online or not
	 * @param page page
	 * @return true if the page is online
	 * @throws NodeException
	 */
	public boolean isPageOnline(Page page) throws NodeException {
		return ObjectTransformer.getBoolean(pageOnline.get(ObjectTransformer.getInt(page.getId(), 0)), false);
	}

	/**
	 * Set the page online/offline
	 * @param page page
	 * @param online true to set it online, false for setting it offline
	 * @throws NodeException
	 */
	public void setOnline(Page page, boolean online) throws NodeException {
		Integer pageId = ObjectTransformer.getInteger(page.getId(), Integer.valueOf(0));
		if (online) {
			pageOnline.put(pageId, Boolean.TRUE);
		} else {
			pageOnline.remove(pageId);
		}
	}

	/**
	 * Get the folderId of the page
	 * @param page page
	 * @return folderId
	 * @throws NodeException if the page does not exist
	 */
	public int getFolderId(Page page) throws NodeException {
		Integer folderId = pageFolderIds.get(page.getId());
		if (folderId == null) {
			throw new NodeException("Error while getting folderId for " + page + ": Page does not exist");
		}

		return folderId;
	}

	/**
	 * Get the construct with given id
	 * @param constructId construct id
	 * @return construct or null
	 */
	public Construct getConstruct(int constructId) {
		return constructs.get(constructId);
	}

	/**
	 * Get the part with given id
	 * @param partId part id
	 * @return part or null
	 */
	public Part getPart(int partId) {
		return parts.get(partId);
	}

	/**
	 * Prepare constructs and parts
	 * @throws NodeException
	 */
	protected void prepareConstructsAndParts() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		// load all constructs
		IntegerColumnRetriever constructIds = new IntegerColumnRetriever("id");
		DBUtils.executeStatement("SELECT id FROM construct", constructIds);
		List<Construct> constructs = t.getObjects(Construct.class, constructIds.getValues());
		for (Construct construct : constructs) {
			this.constructs.put(ObjectTransformer.getInteger(construct.getId(), null), construct);
		}

		// load all parts
		IntegerColumnRetriever partIds = new IntegerColumnRetriever("id");
		DBUtils.executeStatement("SELECT id FROM part", partIds);
		List<Part> parts = t.getObjects(Part.class, partIds.getValues());
		for (Part part : parts) {
			this.parts.put(ObjectTransformer.getInteger(part.getId(), null), part);
		}
	}

	/**
	 * Prepare the channel information
	 * @throws NodeException
	 */
	protected void prepareChannels() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		final List<Integer> nodeIds = new ArrayList<Integer>();
		final Map<Integer, Integer> masterIds = new HashMap<Integer, Integer>();
		final Map<Integer, Integer> folderToNodeIds = new HashMap<Integer, Integer>();
		DBUtils.executeStatement("SELECT node.id, node.folder_id, folder.master_id FROM node, folder WHERE node.folder_id = folder.id AND folder.deleted = 0", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					int nodeId = rs.getInt("id");
					int folderId = rs.getInt("folder_id");
					int masterId = rs.getInt("master_id");
					nodeIds.add(nodeId);
					folderToNodeIds.put(folderId, nodeId);
					if (masterId > 0) {
						masterIds.put(nodeId, masterId);
					}
				}
			}
		});

		List<Node> nodes = t.getObjects(Node.class, nodeIds);
		Map<Integer, Node> idToNodes = new HashMap<Integer, Node>();
		for (Node node : nodes) {
			Integer nodeId = ObjectTransformer.getInteger(node.getId(), null);
			if (nodeId == null) {
				throw new NodeException("Found node without id: " + node);
			}

			channels.put(nodeId, new HashSet<Node>());
			masterNodes.put(nodeId, new ArrayList<Node>());
			idToNodes.put(nodeId, node);
		}

		for (Node node : nodes) {
			Integer nodeId = ObjectTransformer.getInteger(node.getId(), null);

			Node master = null;
			Integer currentId = nodeId;
			do {
				master = null;
				Integer masterNodeId = folderToNodeIds.get(masterIds.get(currentId));
				if (masterNodeId != null) {
					master = idToNodes.get(masterNodeId);

					masterNodes.get(nodeId).add(master);
					channels.get(masterNodeId).add(idToNodes.get(currentId));

					currentId = masterNodeId;
				}
			} while (master != null);
		}

		// prepare the Node languages
		DBUtils.executeStatement("SELECT node_id, contentgroup_id, sortorder FROM node_contentgroup", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					Integer nodeId = rs.getInt("node_id");
					Integer contentgroupId = rs.getInt("contentgroup_id");
					Integer sortorder = rs.getInt("sortorder");

					Map<Integer, Integer> languageMap = nodeLanguages.get(nodeId);
					if (languageMap == null) {
						languageMap = new HashMap<Integer, Integer>();
						nodeLanguages.put(nodeId, languageMap);
					}
					languageMap.put(contentgroupId, sortorder);
				}
			}
		});
	}

	/**
	 * Prepare the template data (channelsets)
	 * @throws NodeException
	 */
	protected void prepareTemplateData() throws NodeException {
		DBUtils.executeStatement("SELECT id, channelset_id, channel_id FROM template", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					Integer templateId = rs.getInt("id");
					Integer channelsetId = rs.getInt("channelset_id");
					Integer channelId = rs.getInt("channel_id");

					// build channelset
					Map<Integer, Integer> templateMap = templateChannelSets.get(channelsetId);
					if (templateMap == null) {
						templateMap = new HashMap<Integer, Integer>();
						templateChannelSets.put(channelsetId, templateMap);
					}
					templateMap.put(channelId, templateId);
				}
			}
		});
	}

	/**
	 * Prepare the page data (channelsets, language variants, page variants)
	 * @throws NodeException
	 */
	protected void preparePageData() throws NodeException {
		DBUtils.executeStatement("SELECT id, channelset_id, channel_id, content_id, contentset_id, contentgroup_id, online, folder_id FROM page WHERE deleted = 0", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					Integer pageId = rs.getInt("id");
					Integer channelsetId = rs.getInt("channelset_id");
					Integer channelId = rs.getInt("channel_id");
					Integer contentId = rs.getInt("content_id");
					Integer contentsetId = rs.getInt("contentset_id");
					Integer contentgroupId = rs.getInt("contentgroup_id");
					boolean online = rs.getInt("online") == OnlineStatusChange.ONLINE.code;
					Integer folderId = rs.getInt("folder_id");

					// build channelset
					Map<Integer, Integer> pageMap = pageChannelSets.get(channelsetId);
					if (pageMap == null) {
						pageMap = new HashMap<Integer, Integer>();
						pageChannelSets.put(channelsetId, pageMap);
					}
					pageMap.put(channelId, pageId);

					// build language variants
					if (contentgroupId != 0) {
						List<Integer> contentSet = pageLangVariants.get(contentsetId);
						if (contentSet == null) {
							contentSet = new ArrayList<Integer>();
							pageLangVariants.put(contentsetId, contentSet);
						}
						contentSet.add(pageId);
					}

					// build page variants
					List<Integer> pageList = pageVariants.get(contentId);
					if (pageList == null) {
						pageList = new ArrayList<Integer>();
						pageVariants.put(contentId, pageList);
					}
					pageList.add(pageId);

					// store online flag
					if (online) {
						pageOnline.put(pageId, Boolean.TRUE);
					}

					// store folderId
					pageFolderIds.put(pageId, folderId);
				}
			}
		});

		// for page variants, do an optimization: If only one entry exists for page variants, remove from the map
		for (Iterator<List<Integer>> i = pageVariants.values().iterator(); i.hasNext(); ) {
			List<Integer> pageList = i.next();
			if (pageList.size() == 1) {
				i.remove();
			}
		}
	}
}
