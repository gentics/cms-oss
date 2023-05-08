/*
 * @author jan
 * @date Aug 29, 2008
 * @version $Id: PageDistributor.java,v 1.9 2008-12-09 12:00:52 jan Exp $
 */
package com.gentics.contentnode.publish;

import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.MulticonnectionTransaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionStatistics;
import com.gentics.contentnode.factory.TransactionStatistics.Item;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.lib.genericexceptions.GenericFailureException;
import com.gentics.lib.log.NodeLogger;

/**
 * An instance of this class will be used by the PublishWorkers to get the next page to be rendered.
 */
public class PageDistributor {
	private int nodeId;

	private WorkLoadMonitor loadMonitor;

	private List<NodeObjectWithAttributes<Page>> pages;

	private volatile int activeThreads = 0;

	private ThreadLocal<Object> threadMarker = new ThreadLocal<Object>();

	private SimplePublishInfo publishInfo;

	public static final double DEFAULT_LOAD_LIMIT = 2;

	public static final String LOAD_LIMIT_CONFIG_KEY = "contentnode.config.loadbalancing.loadlimit";

	public static final String THREAD_LIMIT_CONFIG_KEY = "contentnode.config.loadbalancing.threadlimit";

	/**
	 * Name of the configuration parameter to specify the number of pages, for which the dependencies shall be prepared
	 */
	public final static String PREPARE_DEPS_CONFIG_KEY = "multithreaded_publishing.preparedeps";

	/**
	 * Name of the configuration parameter to specify whether versioned data shall be prepared
	 */
	public final static String PREPARE_DATA_CONFIG_KEY = "multithreaded_publishing.preparedata";

	/**
	 * Name of the configuration parameter to specify whether versioned data of the dependencies shall be prepared
	 */
	public final static String PREPARE_DEPSDATA_CONFIG_KEY = "multithreaded_publishing.preparedepsdata";

	private double loadLimit = DEFAULT_LOAD_LIMIT;

	private static NodeLogger logger = NodeLogger.getNodeLogger(PageDistributor.class);

	private volatile int pageCounter = 0;

	private volatile boolean stop = false;

	private NodeException nodeException = null;

	private GenericFailureException genericFailureException = null;

	private PagePublisher pagePublisher;

	private CnMapPublisher cnMapPublisher;

	private TransactionStatistics stats;

	/**
	 * Render result
	 */
	private RenderResult renderResult;

	/**
	 * Create an instance of the page distributor
	 * @param nodeId node ID
	 * @param pages list of pages to distribute
	 * @param loadMonitor load monitor (may be null)
	 * @param publishInfo publish info
	 * @param config configuration
	 * @param pagePublisher page publisher
	 * @param cnMapPublisher contentmap publisher
	 * @param renderResult render result (may be null)
	 */
	public PageDistributor(int nodeId, List<NodeObjectWithAttributes<Page>> pages, WorkLoadMonitor loadMonitor, SimplePublishInfo publishInfo, NodeConfig config, PagePublisher pagePublisher,
			CnMapPublisher cnMapPublisher, RenderResult renderResult) {
		this.nodeId = nodeId;
		this.loadMonitor = loadMonitor;
		this.pages = pages;
		this.publishInfo = publishInfo;
		this.pagePublisher = pagePublisher;
		this.cnMapPublisher = cnMapPublisher;
		this.renderResult = renderResult;

		if (config != null) {
			NodePreferences prefs = config.getDefaultPreferences();
			loadLimit = ObjectTransformer.getDouble(prefs.getProperty(LOAD_LIMIT_CONFIG_KEY), loadLimit);
		}
		this.publishInfo.setLoadLimit((float) loadLimit);

		try {
			stats = TransactionManager.getCurrentTransaction().getStatistics();
		} catch (TransactionException e) {
		}
	}

	/**
	 * This is called every time a worker thread finishes a pages and tries to get an new one.
	 * 
	 * @return the next page to be published or null if there is nothing left to do.
	 */
	public NodeObjectWithAttributes<Page> getNextPage() {
		if (stop) {
			// In case of a problem the stop flag should be set.
			// Returning null to the workers means there's nothing to do -> the workers will
			// stop safely without causing further exceptions.
			return null;
		}

		if (cnMapPublisher != null) {
			cnMapPublisher.keepContentmapsAlive();
		}

		// For debugging and performance monitoring reasons it is usefull to know
		// how many workers are active at the moment. An active worker means it is
		// currently rendering a page -> if the worker thread is inside this function
		// (waiting for the next page) then it is not active.

		// So every time the worker enters this function we decrement the counter and
		// every time it leaves (with a valid page) we increment the counter.

		// To avoid negative numbers (if the thread enters this function for the first time
		// we should not decrement the counter) use a ThreadLocal as marker to see if we have
		// seen this thread before.

		if (threadMarker.get() == null) {
			threadMarker.set(new Object());
		} else {
			activeThreads--;
		}
		NodeObjectWithAttributes<Page> p;

		synchronized (pages) {
			if (pages instanceof AbstractPageList) {
				try {
					((AbstractPageList) pages).prepareDependencies();
				} catch (NodeException e) {
					stop(e);
					return null;
				}
			}

			// The loadbalancing should be inserted here, something like:
			// if (load to high) { sleep some time until it falls }
			if (loadMonitor != null) {
				try {
					loadMonitor.checkHighLoad();
				} catch (Exception e) {
					stop(new NodeException(e));
					return null;
				}
			}

			if (pages.isEmpty()) {
				updatePublishThreadInfos();
				return null;
			} else {
				try {
					if (stats != null) {
						stats.get(Item.GET_NEXT_PAGE).start();
					}
					p = pages.remove(0);
				} catch (UnsupportedOperationException e) {
					Throwable cause = e.getCause();
					if (cause instanceof NodeException) {
						stop((NodeException)cause);
					} else if (cause instanceof GenericFailureException) {
						stop((GenericFailureException)cause);
					} else {
						logger.error("PageDistributor failed due to exception", cause);
						stop();
					}
					return null;
				} finally {
					if (stats != null) {
						stats.get(Item.GET_NEXT_PAGE).stop();
					}
				}
				publishInfo.pageRendered();
				MBeanRegistry.getPublisherInfo().publishedPage(nodeId);
			}
		}

		// Being here means there is some work for the current worker -> it will be active now ->
		// increment the activeThreads-Counter and the pageCounter. 
		activeThreads++;
		updatePublishInfo();
		pageCounter++;

		// update the thread time statistics every 16 pages
		if (pageCounter % 16 == 0) {
			updatePublishThreadInfos();
		}
		return p;
	}

	private void updatePublishInfo() {
		publishInfo.setCurrentThreadCount(activeThreads);
	}

	private void updatePublishThreadInfos() {
		try {
			MulticonnectionTransaction t = (MulticonnectionTransaction) TransactionManager.getCurrentTransaction();

			publishInfo.setPublishThreadInfos(t.getPublishThreadInfos());
		} catch (TransactionException e) {
			logger.error("Cannot resolve current transaction.", e);
		}
	}

	public int getActiveThreads() {
		return activeThreads;
	}

	public void stop() {
		stop = true;
	}

	public void stop(GenericFailureException abortCause) {
		stop();
		this.genericFailureException = abortCause;
	}

	public void stop(NodeException abortCause) {
		stop();
		this.nodeException = abortCause;
	}

	/**
	 * @return the nodeException
	 */
	public NodeException getNodeException() {
		return nodeException;
	}

	/**
	 * @return the genericFailureException
	 */
	public GenericFailureException getGenericFailureException() {
		return genericFailureException;
	}
}
