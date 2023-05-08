package com.gentics.contentnode.publish;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectIdWithAttributes;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract List Implementation that contains a list of pageIds with optional attributes and will return instances of {@link NodeObjectWithAttributes<Page>}.
 * If the method {@link #remove(int)} is used to get another page from this list, the dependencies for the next
 * couple of pages will be prepared (if no prepared dependencies are laft).
 */
public abstract class AbstractPageList extends AbstractList<NodeObjectWithAttributes<Page>> {
	/**
	 * Name of the configuration parameter to specify the number of pages, for which the dependencies shall be prepared
	 */
	public final static String PREPARE_DEPS_CONFIG_KEY = "multithreaded_publishing.preparedeps";

	/**
	 * by default, we prepare the dependencies for up to 100 pages
	 */
	public final static int DEFAULT_PREPARE_DEPS = 100;

	/**
	 * Logger instance
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(AbstractPageList.class);

	/**
	 * Page IDs
	 */
	protected List<NodeObjectIdWithAttributes> pageIds;

	/**
	 * Number of pages for which the dependencies shall be prepared
	 */
	protected int prepareDepsCount = DEFAULT_PREPARE_DEPS;

	/**
	 * Threshold for the list of still prepared dependencies
	 * If less dependencies are available, the next bunch will be prepared.
	 */
	protected int prepareThreshold;

	/**
	 * Flag to mark whether a thread is currently preparing dependencies
	 */
	protected boolean preparing = false;

	/**
	 * Index of the next page, for which dependencies will be prepared
	 */
	protected int prepareIndex = 0;

	/**
	 * Current index pointer to the list of page Ids
	 */
	protected AtomicInteger indexPointer = new AtomicInteger(0);

	/**
	 * Create an instance for the given list of page IDs
	 * @param pageIds list of pageIds
	 * @param config configuration
	 */
	public AbstractPageList(List<NodeObjectIdWithAttributes> pageIds, NodeConfig config) {
		this.pageIds = pageIds;
		if (config != null) {
			NodePreferences prefs = config.getDefaultPreferences();
			prepareDepsCount = ObjectTransformer.getInt(prefs.getProperty(PREPARE_DEPS_CONFIG_KEY), prepareDepsCount);
		}
		this.prepareThreshold = prepareDepsCount / 2;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractList#get(int)
	 */
	@Override
	public NodeObjectWithAttributes<Page> get(int index) {
		NodeObjectIdWithAttributes pageId = pageIds.get(index);
		try {
			return getPage(pageId);
		} catch (NodeException e) {
			logger.error("Error while getting page " + pageId, e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#size()
	 */
	@Override
	public int size() {
		return Math.max(pageIds.size() - indexPointer.get(), 0);
	}

	@Override
	public NodeObjectWithAttributes<Page> remove(int index) {
		int current = indexPointer.getAndAdd(1);
		try {
			return getPage(pageIds.get(current));
		} catch (NodeException e) {
			throw new UnsupportedOperationException("Could get next page to publish", e);
		}
	}

	/**
	 * Prepare the next couple of dependencies if necessary
	 * @throws NodeException
	 */
	public void prepareDependencies() throws NodeException {
		if (preparing) {
			return;
		}

		synchronized (pageIds) {
			// if no page dependencies are prepared any more,
			// we prepare the dependencies for the next couple of pages now
			// this will reduce the number of SQL statements necessary to load the dependencies for the pages
			// that are rendered
			if (DependencyManager.getPreparedDependencyCount() < prepareThreshold && prepareIndex < pageIds.size()) {
				try {
					preparing = true;
					int endPrepareIndex = Math.min(prepareIndex + prepareDepsCount, pageIds.size());
					DependencyManager.prepareDependencies(pageIds.subList(prepareIndex, endPrepareIndex).stream().map(entry -> entry.id)
							.collect(Collectors.toList()));
					prepareIndex = endPrepareIndex;
				} finally {
					preparing = false;
				}
			}
		}
	}

	/**
	 * Get the page with the given id
	 * @param pageId page id
	 * @return page
	 * @throws NodeException
	 */
	protected abstract NodeObjectWithAttributes<Page> getPage(NodeObjectIdWithAttributes pageId) throws NodeException;
}
