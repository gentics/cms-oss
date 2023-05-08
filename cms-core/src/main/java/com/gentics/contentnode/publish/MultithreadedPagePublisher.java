package com.gentics.contentnode.publish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Level2CacheTrx;
import com.gentics.contentnode.factory.MulticonnectionTransaction;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectIdWithAttributes;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.contentnode.publish.wrapper.PublishablePage;
import com.gentics.contentnode.publish.wrapper.PublishablePageList;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.etc.StatsItem;
import com.gentics.lib.genericexceptions.GenericFailureException;
import com.gentics.lib.render.exception.RecoverableException;

/**
 * Multithreaded page publisher
 */
public class MultithreadedPagePublisher extends PagePublisher {
	private static final int DEFAULT_THREAD_LIMIT = 8;

	/**
	 * Create an instance
	 * @param config config
	 * @param cnMapPublisher cn map publisher
	 * @param factory node factory
	 * @param workPhase workphase
	 * @param publishInfo publish info
	 */
	protected MultithreadedPagePublisher(NodeConfig config, CnMapPublisher cnMapPublisher, ContentNodeFactory factory, IWorkPhase workPhase, SimplePublishInfo publishInfo) {
		super(config, cnMapPublisher, factory, workPhase, publishInfo);
	}

	@Override
	protected void publishPages(Node node, List<NodeObjectIdWithAttributes> dirtedPageIds, final RenderResult renderResult) throws NodeException, GenericFailureException {
		Transaction t = TransactionManager.getCurrentTransaction();
		MulticonnectionTransaction mt;

		boolean disableVersionedPublishing = t.getNodeConfig().getDefaultPreferences().getFeature("disable_versioned_publishing");
		boolean publishCache = t.isPublishCacheEnabled();

		if (t instanceof MulticonnectionTransaction) {
			mt = (MulticonnectionTransaction) t;
		} else {
			throw new NodeException("Threaded publisher was started with normal transaction however a multiconnection transaction is required!");
		}

		renderResult.info(Publisher.class, "Starting rendering of " + dirtedPageIds.size() + " pages for Node " + node);

		List<NodeObjectIdWithAttributes> nextPublishPages = dirtedPageIds;

		// Make at most 2 publish runs ..
		for (int publishRun = 0; publishRun < 2 && nextPublishPages. /**/size() > 0; publishRun++) {
			List<PublishWorker> workers = new ArrayList<PublishWorker>();

			// do not handle normal dependencies in publish run 1
			if (publishRun > 0) {
				mt.getRenderType().setHandleDependencies(false);
			}

			if (logger.isInfoEnabled()) {
				logger.info("Starting publish run {" + publishRun + "} - {" + nextPublishPages.size() + "} pages to go.");
			}
			renderResult.info(Publisher.class, "Starting publish run {" + publishRun + "} - {" + nextPublishPages.size() + "} pages to go.");

			List<NodeObjectIdWithAttributes> thisPublishPages = nextPublishPages;
			nextPublishPages = new ArrayList<>();

			int threadLimit;

			try {
				// ugly NullPointerExceptions and NumberformatExcp. can happen here, watch out!
				threadLimit = Integer.parseInt(t.getRenderType().getPreferences().getProperty("contentnode.config.loadbalancing.threadlimit"));
			} catch (RuntimeException e1) {
				threadLimit = DEFAULT_THREAD_LIMIT;
			}
			publishInfo.setThreadLimit(threadLimit);
			renderResult.info(Publisher.class, "Creating " + threadLimit + " threads for multithreaded publishing");
			for (int j = 0; j < threadLimit; j++) {
				PublishWorker worker = new PublishWorker(renderResult, publishRun == 0, mt, config, workPhase, this, t.getChannelId(), node.getId());
				Thread workerThread = new Thread(Thread.currentThread().getThreadGroup(), worker, "Publishing worker thread " + j);

				worker.setThread(workerThread);
				workers.add(worker);
			}

			// this is a list of Page IDs for the distributor
			List<NodeObjectIdWithAttributes> pagesToDistribute = new ArrayList<>();

			renderResult.info(Publisher.class, "Preparing pages for publishing");

			final boolean publishStats = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.PUBLISH_STATS);
			final StatsItem initializeCacheStats = new StatsItem();
			StatsItem prepareStats = null;
			if (publishStats) {
				prepareStats = new StatsItem();
			}
			// Iterate through all pages
			t.setDisableVersionedPublish(true);
			t.setPublishCacheEnabled(false);
			try {
				int batchSize = 100;
				for (int start = 0; start < thisPublishPages.size(); start += batchSize) {
					int end = Math.min(start + batchSize, thisPublishPages.size());
					List<NodeObjectIdWithAttributes> chunk = thisPublishPages.subList(start, end);

					if (prepareStats != null) {
						prepareStats.start();
					}

					try {
						for (NodeObjectIdWithAttributes pageIdWithAttributes : chunk) {
							Page page = t.getObject(Page.class, pageIdWithAttributes.id);
							if (pageMayBePublished(page, node, renderResult)) {
								pagesToDistribute.add(new NodeObjectIdWithAttributes(page.getId(), pageIdWithAttributes.attributes));
							}
						}
					} catch (RecoverableException e) {
						logger.error("Preparing of pages for publishing failed. - Ignoring and proceeding anyway", e);
					} catch (NodeException e) {
						if (force) {
							renderResult.error(Publisher.class, "Preparing of pages for publishing failed: " + e.toString());
						} else {
							logger.error("Error while preparing pages for rendering", e);
							throw e;
						}
					} catch (Exception e) {
						if (force) {
							renderResult.error(Publisher.class, "Error while preparing pages for rendering: " + e.toString());
							logger.error("Error while preparing pages for rendering.", e);
						} else {
							throw new GenericFailureException("Error while preparing pages for rendering.", e);
						}
					} finally {
						if (prepareStats != null) {
							prepareStats.stop(chunk.size());
						}
					}
				}

				if (prepareStats != null) {
					renderResult.info(Publisher.class, "Prepared pages for publishing: " + prepareStats.getInfo());
				} else {
					renderResult.info(Publisher.class, "Prepared pages for publishing");
				}
				prepareStats = null;

				// if versioned publishing is used, we prepare the REST models of the pages
				// and put them in the cache (if not already there)
				t.setPublishCacheEnabled(true);
				if (!disableVersionedPublishing && publishCache) {
					renderResult.info(Publisher.class, "Initializing PublishablePage cache using " + threadLimit + " threads");
					ExecutorService threadPool = Executors.newFixedThreadPool(threadLimit);
					final List<Integer> prepareList = new ArrayList<>(pagesToDistribute.stream().map(e -> e.id).collect(Collectors.toList()));
					Collection<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>(threadLimit);
					final Transaction threadTransaction = t;
					for (int i = 0; i < threadLimit; i++) {
						tasks.add(new Callable<Boolean> () {
							@Override
							public Boolean call() throws Exception {
								TransactionManager.setCurrentTransaction(threadTransaction);
								Integer pageId = null;
								while (true) {
									pageId = null;
									synchronized (prepareList) {
										if (prepareList.size() > 0) {
											pageId = prepareList.remove(0);
										}
									}
									if (pageId == null) {
										break;
									}
									if (publishStats) {
										initializeCacheStats.start();
									}
									try {
										PublishablePage.getInstance(pageId);
									} finally {
										if (publishStats) {
											initializeCacheStats.stop();
										}
									}
								}
								return true;
							}});
					}
					try (Level2CacheTrx level2Trx = new Level2CacheTrx(false)) {
						threadPool.invokeAll(tasks);
						threadPool.shutdown();
						threadPool.awaitTermination(1, TimeUnit.DAYS);
					} catch (InterruptedException e) {
						renderResult.error(Publisher.class, "An error occurred while preparing publish cache, continuing anyway", e);
					}
					if (publishStats) {
						renderResult.info(Publisher.class, "Initialized PublishablePage cache: " + initializeCacheStats.getInfo());
					} else {
						renderResult.info(Publisher.class, "Initialized PublishablePage cache");
					}
				}
			} finally {
				t.setDisableVersionedPublish(false);
				t.setPublishCacheEnabled(true);
			}

			AsynchronousWorkerLoadMonitor loadMonitor = new AsynchronousWorkerLoadMonitor(cnMapPublisher.getWorkers());

			List<NodeObjectWithAttributes<Page>> pages = null;
			if (disableVersionedPublishing || !publishCache) {
				pages = new PageList(pagesToDistribute, t.getNodeConfig(), threadLimit);
			} else {
				pages = new PublishablePageList(pagesToDistribute, t.getNodeConfig(), threadLimit);
			}
			PageDistributor pageDistributor = new PageDistributor(node.getId(), pages, loadMonitor, publishInfo, t.getNodeConfig(), this, cnMapPublisher, renderResult);

			long start = System.currentTimeMillis();
			Iterator<PublishWorker> j = workers.iterator();

			while (j.hasNext()) {
				PublishWorker pw = j.next();

				pw.setPageDistributor(pageDistributor);
				pw.getThread().start();
			}

			try {
				Iterator<PublishWorker> k = workers.iterator();

				while (k.hasNext()) {
					PublishWorker w = k.next();

					w.getThread().join();
					for (Iterator<Page> it = w.getRepublish().iterator(); it.hasNext();) {
						Page page = it.next();

						// dirt the objects as a whole, we don't care about attribute specific publishing, because republish is
						// due to XNL Objects, which are deprecated and mean and evil and should not be used anyway
						nextPublishPages.add(new NodeObjectIdWithAttributes(page.getId(), null));
					}
				}

				// now get any exceptions from PublishWorkers
				NodeException e1 = pageDistributor.getNodeException();

				if (e1 != null) {
					throw e1;
				}
				GenericFailureException e2 = pageDistributor.getGenericFailureException();

				if (e2 != null) {
					throw e2;
				}
                
			} catch (InterruptedException e) {
				logger.error("The publish process has been interuppted - stopped? Starting rollback.");
                
				// stop the page distributor - no new pages will be given to the workers
				pageDistributor.stop();
                
				// now let the workers finish what they have started (with timeout)
				// otherwise we will get a lot of ugly nullpointerexceptions 
				// because the rollback will close the connections
				for (Iterator<PublishWorker> waitIt = workers.iterator(); waitIt.hasNext();) {
					PublishWorker w = waitIt.next();

					try {
						w.getThread().join(5000);
					} catch (InterruptedException e1) {
						break;
					}
				}
				throw new PublishInterruptedException("The publish process has been interuppted");
			}
			mt.waitForAsynchronousJobs();
			logger.info("Workers finished in " + (System.currentTimeMillis() - start) + "ms.");

			for (Iterator<PublishThreadInfo> infoIterator = publishInfo.getPublishThreadInfos().iterator(); infoIterator.hasNext();) {
				logger.info(infoIterator.next().toString());
			}            

			// set handle dependencies to true again
			if (publishRun > 0) {
				mt.getRenderType().setHandleDependencies(true);
			}
		}
	}
}
