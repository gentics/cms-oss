package com.gentics.contentnode.factory;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ChannelTrxInvocationHandler;
import com.gentics.contentnode.object.Page;
import com.gentics.lib.log.NodeLogger;

/**
 * Lightweight Implementation for page lists.
 * Stores only the IDs of the page objects
 */
public class LightWeightPageList extends AbstractList<Page> {
	/**
	 * Logger instance
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(LightWeightPageList.class);

	/**
	 * List of NodeIdPageId instances
	 */
	protected List<NodeIdPageId> pageIds = null;

	/**
	 * Flag whether sticky channel shall be used
	 */
	protected boolean stickyChannel = false;

	/**
	 * Create an empty list
	 */
	public LightWeightPageList() {
		this(false);
	}

	/**
	 * Create an empty list
	 * @param stickyChannel true for sticky channel
	 */
	public LightWeightPageList(boolean stickyChannel) {
		pageIds = new ArrayList<>();
		this.stickyChannel = stickyChannel;
	}

	/**
	 * Create an empty list with the given initial capacity
	 * @param initialCapacity initial capacity
	 */
	public LightWeightPageList(int initialCapacity) {
		this(initialCapacity, false);
	}

	/**
	 * Create an empty list with the given initial capacity
	 * @param initialCapacity initial capacity
	 * @param stickyChannel true for sticky channel
	 */
	public LightWeightPageList(int initialCapacity, boolean stickyChannel) {
		pageIds = new ArrayList<>(initialCapacity);
		this.stickyChannel = stickyChannel;
	}

	/**
	 * Create a list with the given page IDs
	 * @param pageIds page IDs
	 */
	public LightWeightPageList(List<Integer> pageIds) {
		this.pageIds = new ArrayList<>(pageIds.stream().map(id -> new NodeIdPageId(id)).collect(Collectors.toList()));
		this.stickyChannel = false;
	}

	/**
	 * Add the page with given id
	 * @param id page ID
	 * @param nodeId node ID
	 */
	public void addPage(int id, int nodeId) {
		if (stickyChannel) {
			pageIds.add(new NodeIdPageId(nodeId, id));
		} else {
			pageIds.add(new NodeIdPageId(id));
		}
	}

	@Override
	public Page get(int index) {
		NodeIdPageId tuple = pageIds.get(index);
		if (tuple == null) {
			return null;
		}
		if (stickyChannel) {
			try (ChannelTrx cTrx = new ChannelTrx(tuple.nodeId)) {
				Page page = TransactionManager.getCurrentTransaction().getObject(Page.class, tuple.pageId);
				if (page == null) {
					return null;
				} else {
					return (Page) ChannelTrxInvocationHandler.wrap(tuple.nodeId, page);
				}
			} catch (NodeException e) {
				logger.error("Error while getting page " + tuple.pageId, e);
				return null;
			}
		} else {
			try {
				return TransactionManager.getCurrentTransaction().getObject(Page.class, tuple.pageId);
			} catch (NodeException e) {
				logger.error("Error while getting page " + tuple.pageId, e);
				return null;
			}
		}
	}

	@Override
	public int size() {
		return pageIds.size();
	}

	@Override
	public Page set(int index, Page page) {
		Page oldPage = get(index);
		if (page == null) {
			pageIds.set(index, null);
		} else {
			if (stickyChannel) {
				try {
					pageIds.set(index, new NodeIdPageId(page.getNode().getId(), page.getId()));
				} catch (NodeException e) {
					logger.error("Error while setting " + page, e);
				}
			} else {
				pageIds.set(index, new NodeIdPageId(page.getId()));
			}
		}
		return oldPage;
	}

	@Override
	public void add(int index, Page page) {
		if (page == null) {
			pageIds.add(index, null);
		} else {
			if (stickyChannel) {
				try {
					pageIds.add(index, new NodeIdPageId(page.getNode().getId(), page.getId()));
				} catch (NodeException e) {
					logger.error("Error while adding " + page, e);
				}
			} else {
				pageIds.add(index, new NodeIdPageId(page.getId()));
			}
		}
	}

	@Override
	public boolean addAll(Collection<? extends Page> c) {
		if (c instanceof LightWeightPageList) {
			LightWeightPageList lwPl = (LightWeightPageList) c;
			return pageIds.addAll(lwPl.pageIds);
		} else {
			return super.addAll(c);
		}
	}

	@Override
	public Page remove(int index) {
		Page oldPage = get(index);
		pageIds.remove(index);
		return oldPage;
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof NodeIdPageId) {
			return pageIds.contains(o);
		} else if (o instanceof Page) {
			try {
				Page p = (Page) o;
				int pageId = p.getId();
				int nodeId = p.getNode().getId();
				if (stickyChannel) {
					return pageIds.contains(new NodeIdPageId(nodeId, pageId));
				} else {
					return pageIds.contains(new NodeIdPageId(pageId));
				}
			} catch (NodeException e) {
				logger.error("Error while checking contains(" + o + ")", e);
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Class for instances of nodeId/pageId tuples stored in the list
	 */
	protected static class NodeIdPageId {
		protected int nodeId;
		protected int pageId;

		/**
		 * Create instance with only pageId
		 * @param pageId page ID
		 */
		public NodeIdPageId(int pageId) {
			this(0, pageId);
		}

		/**
		 * Create instance with nodeId and pageId
		 * @param nodeId node ID
		 * @param pageId page ID
		 */
		public NodeIdPageId(int nodeId, int pageId) {
			this.nodeId = nodeId;
			this.pageId = pageId;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NodeIdPageId) {
				NodeIdPageId other = (NodeIdPageId) obj;
				return nodeId == other.nodeId && pageId == other.pageId;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return nodeId + pageId;
		}
	}
}
