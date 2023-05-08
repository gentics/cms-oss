/*
 * @author jan
 * @date Jun 5, 2008
 * @version $Id: PublishWorker.java,v 1.6 2010-11-09 09:59:00 clemens Exp $
 */
package com.gentics.contentnode.publish;

import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.MulticonnectionTransaction;
import com.gentics.contentnode.factory.PublishedNodeTrx;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionStatistics;
import com.gentics.contentnode.factory.TransactionStatistics.Item;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.jmx.PublishWorkerInfo;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.genericexceptions.GenericFailureException;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.exception.RecoverableException;

/**
 * This class represents a worker of the multithreaded publisher.
 * 
 * If you want to create a new thread with this Runnable make sure you use
 * an AttributedThreadGroup (the same as the one of the Publisher Thread).
 * 
 * @author jan
 *
 */
public class PublishWorker implements Runnable {
    
	private RenderResult renderResult;
    
	private boolean publishRun;
    
	private List<Page> republish = new Vector<Page>();
    
	private MulticonnectionTransaction transaction;
    
	private Thread thread;
    
	private NodeConfig configuration;
    
	private IWorkPhase publishPagePhase;
    
	private NodeLogger logger = NodeLogger.getNodeLogger(PublishWorker.class);
    
	private PagePublisher pagePublisher;
    
	private PageDistributor pageDistributor;

	/**
	 * Management Bean
	 */
	private PublishWorkerInfo publishWorkerInfo;

	/**
	 * Channel ID
	 */
	private Integer channelId;

	private Integer publishedNodeId;

	/**
	 * Returns the thread of this runnable.
	 * This is not the Thread.currentThread(). It't the thread 
	 * in which this runnable is running. You can use this for 
	 * interrupting this worker from the outside (=from an other thread).
	 * @return
	 */
	public Thread getThread() {
		return thread;
	}

	/**
	 * Set the thread for this publish worker.
	 * @param thread must be the parent thread of this runnable.
	 */
	public void setThread(Thread thread) {
		this.thread = thread;
	}

	public PublishWorker(RenderResult renderResult, boolean publishRun, MulticonnectionTransaction transaction, NodeConfig configuration, IWorkPhase publishPagePhase, PagePublisher pagePublisher, Integer channelId, Integer publishedNodeId) {
		super();
		this.renderResult = renderResult;
		this.publishRun = publishRun;
		this.transaction = transaction;
		this.configuration = configuration;
		this.publishPagePhase = publishPagePhase;
		this.pagePublisher = pagePublisher;
		this.channelId = channelId;
		this.publishedNodeId = publishedNodeId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if (channelId != null) {
			transaction.setChannelId(channelId);
		}

		publishWorkerInfo = new PublishWorkerInfo();
		MBeanRegistry.registerMBean(publishWorkerInfo, "Publish", "PublishWorker " + thread.getName());

		// set the backend language
		ContentNodeHelper.setLanguageId(2);

		// this will give us a new RenderType
		RenderType renderType = RenderType.getDefaultRenderType(configuration.getDefaultPreferences(), RenderType.EM_PUBLISH, null, -1);
		NodePreferences prefs = configuration.getDefaultPreferences();

		renderType.setRenderUrlFactory(
			new StaticUrlFactory(RenderType.parseLinkWay(prefs.getProperty("contentnode.linkway")),
			RenderType.parseLinkWay(prefs.getProperty("contentnode.linkway_file")),
			prefs.getProperty("contentnode.linkway_file_path")));
        
		// TransactionManager.currentTransaction is a ThreadLocal!
		// That's why we have to set it again (we are no longer in the "Publisher"-thread!)
		TransactionManager.setCurrentTransaction(transaction);
        
		// PublishThreadInfo is just for debugging purposes
		transaction.setPublishThreadInfo(new PublishThreadInfo(Thread.currentThread()));
		TransactionStatistics stats = transaction.getStatistics();

		try {
			TransactionManager.getCurrentTransaction().setRenderType(renderType);
		} catch (TransactionException e1) {
			// just forward the exception to the pagedistributor
			pageDistributor.stop(e1);
		}
		NodeObjectWithAttributes<Page> pageWithAttributes = null;
		Page page = null;

		// TODO double check if publishedNode can be eliminated because at this
		// time channelId and publishedNodeId should be equal anyway.
		try (PublishedNodeTrx pnTrx = new PublishedNodeTrx(publishedNodeId)) {
			// The main loop of the worker.
			// 1. We ask the pageDistributor for next page
			// -- this might block/sleep for an unspecified amount of time
			// (because of high load) - record the timestamp before we start waiting
			// 2. getNextPage() - might return null => there's nothing to do => exit the loop =>
			// => end this thread
			// 3. If the next page is not null measure the time we have spent waiting (current - startWait)
			// 4. Publish the page. If the page needs republish, store it an internal list which then be read 
			// by the main "Publisher"-thread after we're finished and (combined with the lists of the other workers) 
			// used for the next publish run.
			// 5. Go to 1.
            
			long startWait = System.currentTimeMillis();

			while ((pageWithAttributes = pageDistributor.getNextPage()) != null) {
				try {
					page = pageWithAttributes.getObject();
					publishWorkerInfo.startPage(ObjectTransformer.getString(page.getId(), ""));
					transaction.getPublishThreadInfo().increaseTimeWaitingLoad((System.currentTimeMillis() - startWait));
					String infomsg = "Starting update of page {" + page.getId() + "}";
					NodeObjectVersion pVersion = page.getVersion();

					if (pVersion != null) {
						infomsg += " (" + pVersion + ")";
					}
					renderResult.info(Publisher.class, infomsg);
					logger.info("Starting update of page {" + page.getId() + "}");
					long startPublish = System.currentTimeMillis();
					if (stats != null) {
						stats.get(Item.RENDER_PAGE).start();
					}
					long[] times = new long[2];
					boolean needsRepublish = pagePublisher.update(renderResult, page, publishRun, pageWithAttributes.getAttributes(), times);

					transaction.getPublishThreadInfo().increaseTimePublish((System.currentTimeMillis()) - startPublish);
					if (needsRepublish) {
						republish.add(page);
					}
					infomsg = PagePublisher.getLogMessage(page, page.getId(), System.currentTimeMillis() - startPublish, times, needsRepublish);
					renderResult.info(Publisher.class, infomsg);

					if (!ObjectTransformer.isEmpty(pageWithAttributes.attributes)) {
						renderResult.debug(Publisher.class, "Attributes for " + page + ": " + pageWithAttributes.attributes);
					}

					publishPagePhase.doneWork();
                    
					// the next call will be getNextPage() so record the current timestamp so we can see how long we've waited
					startWait = System.currentTimeMillis();
				} catch (RecoverableException e) {
					logger.error("publishing of page {" + page + "} failed. - Ignoring and proceeding with next page.", e);
				} finally {
					if (stats != null) {
						stats.get(Item.RENDER_PAGE).stop();
					}
					publishWorkerInfo.stopPage();
				}
			}
            
			// Rethrowing the exceptions here makes no sense! This has to be done in the main "Publisher"-thread.
			// In case of an error we tell the PageDistributor to stop and remember the cause of the problem
			// (e.g. the Exception we have caught).
			// If we stop the PageDistributor then all workers will be stopped as well (because they will get a null 
			// after calling getNextPage). After that the main "Publisher"-thread will check if there were any error 
			// reported by the workers and eventually rethrow the exception.
		} catch (NodeException e) {
			logger.error("Error while rendering page. {" + page + "}", e);
			pageDistributor.stop(e);
		} catch (Exception e) {
			pageDistributor.stop(new GenericFailureException("publishing of page {" + page + "} failed.", e));

		} finally {
			MBeanRegistry.unregisterMBean("Publish", "PublishWorker " + thread.getName());
			if (channelId != null) {
				transaction.resetChannel();
			}
		}
	}

	/**
	 * Returns the list of pages that have to be published again
	 * during the next publish round.
	 * @return
	 */
	public List<Page> getRepublish() {
		return republish;
	}

	/**
	 * Sets the reference of the pageDistributor. This has to be done
	 * before you start a new thread with this runnable.
	 * 
	 * @param pageDistributor the pageDistributor to set
	 */
	public void setPageDistributor(PageDistributor pageDistributor) {
		this.pageDistributor = pageDistributor;
	}
}
