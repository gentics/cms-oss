/*
 * @author Stefan Hepp
 * @date 24.12.2006
 * @version $Id: PagePublisher.java,v 1.31.2.2 2011-03-15 17:26:52 tobiassteiner Exp $
 */
package com.gentics.contentnode.publish;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.MulticonnectionTransaction;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.PublishedNodeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionStatistics;
import com.gentics.contentnode.factory.TransactionStatistics.Item;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectIdWithAttributes;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.contentnode.publish.PublishQueue.PublishAction;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.publish.mesh.MeshPublishController;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.publish.wrapper.PublishablePage;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.IntegerColumnRetriever;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.db.SQLExecutorWrapper;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.genericexceptions.GenericFailureException;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.exception.RecoverableException;

/**
 * The pagepublisher translates all pages to publish and writes the results into the
 * publish-db.
 *
 * TODO update contentmap for pages too (use cnmappublisher!)
 * TODO function to publish all pages (move loop from publisher)
 */
public class PagePublisher {
    
	protected static NodeLogger logger = NodeLogger.getNodeLogger(PagePublisher.class);

	protected NodeConfig config;

	protected CnMapPublisher cnMapPublisher;

	protected MeshPublishController meshPublishController;

	protected ContentNodeFactory factory;
    
	protected IWorkPhase workPhase;

	protected SimplePublishInfo publishInfo;

	protected boolean force;
	
	/**
	 * Create a new instance of the page publisher (either multithreaded or single threaded)
	 * @param config config
	 * @param cnMapPublisher cnmap publisher
	 * @param factory node factory
	 * @param workPhase workphase
	 * @param publishInfo publish info
	 * @return instance
	 */
	public static PagePublisher getPagePublisher(NodeConfig config, CnMapPublisher cnMapPublisher, ContentNodeFactory factory, IWorkPhase workPhase, SimplePublishInfo publishInfo) {
		if (config.getDefaultPreferences().isFeature(Feature.MULTITHREADED_PUBLISHING)) {
			return new MultithreadedPagePublisher(config, cnMapPublisher, factory, workPhase, publishInfo);
		} else {
			return new PagePublisher(config, cnMapPublisher, factory, workPhase, publishInfo);
		}
	}

	/**
	 * Create an instance of the single-threaded page publisher
	 * @param config config
	 * @param cnMapPublisher cnmap publisher
	 * @param factory node factory
	 * @param workPhase workphase
	 * @param publishInfo publish info
	 */
	protected PagePublisher(NodeConfig config, CnMapPublisher cnMapPublisher, ContentNodeFactory factory, IWorkPhase workPhase, SimplePublishInfo publishInfo) {
		this.config = config;
		this.cnMapPublisher = cnMapPublisher;
		this.factory = factory;
		this.workPhase = workPhase;
		this.force = config.getDefaultPreferences().getFeature("override_publish_errors");
		this.publishInfo = publishInfo;
	}

	/**
	 * handle time management (NOTE: this must be done before pages are
	 * removed from publish table, because in order to successfully take
	 * pages offline, we need to know whether the page is currently online
	 * or not, and this information is taken from the publish table)
	 * @param factory A factory to create the page objects to manipulate
	 * @throws NodeException
	 */
	public static void doTimeManagement(ContentNodeFactory factory) throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		final int time = t.getUnixTimestamp();
		final Set<Integer> pageIds = new HashSet<>();

		// get all pages that are due to publish at a certain timestamp
		pageIds.addAll(DBUtils.select("SELECT id FROM page WHERE time_pub > ? AND time_pub <= ? AND time_pub_version IS NOT NULL AND deleted = ?", ps -> {
			ps.setInt(1, 0);
			ps.setInt(2, time);
			ps.setInt(3, 0);
		}, DBUtils.IDS));

		// get all pages that are due to take offline at a certain timestamp
		pageIds.addAll(DBUtils.select("SELECT id FROM page WHERE time_off > ? AND time_off <= ? AND deleted = ?", ps -> {
			ps.setInt(1, 0);
			ps.setInt(2, time);
			ps.setInt(3, 0);
		}, DBUtils.IDS));

		int oldEditMode = renderType.getEditMode();
		try {
			renderType.setEditMode(RenderType.EM_PREVIEW);
			List<Page> pages = t.getObjects(Page.class, pageIds);
			DependencyManager.initDependencyTriggering();
			for (Page page : pages) {
				page.handleTimemanagement();
			}

			PublishQueue.finishFastDependencyDirting();
		} finally {
			renderType.setEditMode(oldEditMode);
			try {
				t.commit(false);
			} catch (TransactionException e) {
				logger.error("Couldn't Transaction.commit(false)", e);
			}
			DependencyManager.resetDependencyTriggering();
		}
	}

	/**
	 * Initializes the page publisher:
	 * - removes inactive pages from publish table
	 *
	 * Afterwards commits the current transaction !
	 * @param lastPublishRun timestamp of the last publish run.
	 * @param renderResult render result
	 * @throws NodeException
	 */
	public void initialize(final long lastPublishRun, RenderResult renderResult) throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();

		// delete offline pages / deleted pages from 'publish' table
		final Set<Integer> pageIds = new HashSet<Integer>();

		if (renderResult != null) {
			renderResult.info(PagePublisher.class, "Collecting deleted pages");
		}

		PublishQueue.getDeletedObjects(Page.TYPE_PAGE, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					pageIds.add(rs.getInt("o_id"));
				}
			}
		});
		final int deletedPageCount = pageIds.size();

		if (renderResult != null) {
			renderResult.info(PagePublisher.class, "Collecting offline pages");
		}

		PublishQueue.getOfflinePages(new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					pageIds.add(rs.getInt("o_id"));
				}
			}
		});
		final int offlinePageCount = pageIds.size() - deletedPageCount;

		if (renderResult != null) {
			renderResult.info(PagePublisher.class, "Deactivating " + deletedPageCount + " deleted and " + offlinePageCount + " offline pages in publish table");
		}
		if (!pageIds.isEmpty()) {
			DBUtils.executeMassStatement("DELETE FROM publish WHERE page_id IN", null, new ArrayList<Integer>(pageIds), 1, null, Transaction.DELETE_STATEMENT);
		}

		// clean publish table for all nodes, that have publishing not disabled
		cleanPublishTable(renderResult);

		// remove inactive entries
		DBUtils.executeUpdate("DELETE FROM publish WHERE active = ?", new Object[] {0});

		// remove PublishablePage instances from cache
		boolean publishCache = t.isPublishCacheEnabled();
		if (publishCache) {
			for (Integer pageId : pageIds) {
				PublishablePage.removeFromCache(pageId);
			}

			// we also need to refresh the cache for pages, that were changed
			pageIds.clear();
			pageIds.addAll(PublishQueue.getDirtedObjectIds(Page.class, true, null, Action.DEPENDENCY));
			for (Integer pageId : pageIds) {
				PublishablePage.removeFromCache(pageId);
			}
		} else {
			// when the feature is disabled, clear the whole cache
			PublishablePage.clearCache();
		}

		t.commit(false);
	}

	/**
	 * Set the mesh publishers
	 * @param meshPublishController mesh publishers
	 */
	public void setMeshPublishController(MeshPublishController meshPublishController) {
		this.meshPublishController = meshPublishController;
	}

	/**
	 * render a page and update the page source in the publish-db.
	 *
	 * @param renderResult the renderresult for log messages.
	 * @param page the page to update.
	 * @return true if page has to be republished.
	 * @throws SQLException if any sql-exception occurs.
	 */
	public boolean update(RenderResult renderResult, Page page, boolean allowRepublish) throws SQLException, NodeException {
		return update(renderResult, page, allowRepublish, null, null);
	}

	/**
	 * render a page and update the page source in the publish-db.
	 *
	 * @param renderResult the renderresult for log messages.
	 * @param page the page to update.
	 * @param attributes optional set of attributes to be updated
	 * @param times int array that will get the render times (for content and attributes) set, if not null
	 * @return true if page has to be republished.
	 * @throws SQLException if any sql-exception occurs.
	 */
	public boolean update(RenderResult renderResult, Page page, boolean allowRepublish, Set<String> attributes, long[] times) throws SQLException, NodeException {
		// get the channels of the node
		Node node = page.getFolder().getNode();

		// publish the page for its node
		return updatePageForNode(renderResult, page, allowRepublish, node, attributes, times);
	}

	/**
	 * Render the page in the scope if the given node. Update the page source in the publish-db and write the page into the content repository.
	 * @param renderResult the render result for log messages
	 * @param page the page to update
	 * @param allowRepublish true if republishing the page is allowed
	 * @param node node for which the page shall be rendered
	 * @param attributes optional set of attributes to be updated
	 * @param times int array that will get the render times set (if not null)
	 * @return true if the page has to be republished
	 * @throws SQLException
	 * @throws NodeException
	 */
	protected boolean updatePageForNode(RenderResult renderResult, Page page,
			boolean allowRepublish, Node node, Set<String> attributes, long[] times) throws SQLException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		int pageId = ObjectTransformer.getInt(page.getId(), 0);
		int nodeId = ObjectTransformer.getInt(node.getId(), 0);

		String source = null;
		List<TagmapEntryRenderer> tagmapEntries = null;
		ContentMap contentMap = node.doPublishContentMapPages() ? cnMapPublisher.getContentMap(node, true) : null;
		MeshPublisher mp = node.doPublishContentMapPages() ? meshPublishController.get(node) : null;

		// we can mark pages immediately being published, if the feature "resumable_publish_process" is on
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		boolean reportToPublishQueue = prefs.isFeature(Feature.RESUMABLE_PUBLISH_PROCESS);
		boolean writeFs = node.doPublishFilesystem();
		boolean omitPublishTable = !writeFs && prefs.isFeature(Feature.OMIT_PUBLISH_TABLE);

		BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer = null;

		if (contentMap != null) {
			tagmapEntries = contentMap.getTagmapEntries(Page.TYPE_PAGE);
			// we only can immediately mark pages being published, if the contentmap has instant publishing activated
			reportToPublishQueue &= contentMap.isInstantPublishing();
			linkTransformer = CnMapPublisher.LINKTRANSFORMER;
		} else if (mp != null) {
			tagmapEntries = mp.getEntries(Page.TYPE_PAGE);
			reportToPublishQueue &= mp.getCr().isInstantPublishing();
			linkTransformer = MeshPublisher.LINKTRANSFORMER;
		} else {
			tagmapEntries = Collections.emptyList();
		}
		Map<TagmapEntryRenderer, Object> renderedAttributes = new HashMap<>(tagmapEntries.size());

		for (TagmapEntryRenderer entry : tagmapEntries) {
			renderedAttributes.put(entry, null);
		}

		PageRenderResult pageRenderResult = new PageRenderResult(renderResult);

		t.setRenderResult(pageRenderResult);
		pageRenderResult.setAllowRepublish(allowRepublish);

		// when we have to write the page into the publish table, we add a renderer for the path
		// this way, the dependencies will be calculated and stored properly
		PublishtablePathRenderer publishTablePathRenderer = null;
		String publishTablePath = null;
		if (!omitPublishTable) {
			publishTablePathRenderer = new PublishtablePathRenderer(page);
			renderedAttributes.put(publishTablePathRenderer, null);
			if (attributes != null) {
				attributes.add(publishTablePathRenderer.getMapname());
			}
		}

		source = page.render(pageRenderResult, renderedAttributes, attributes, linkTransformer, times);

		if (publishTablePathRenderer != null) {
			publishTablePath = ObjectTransformer.getString(renderedAttributes.get(publishTablePathRenderer), null);
			renderedAttributes.remove(publishTablePathRenderer);
			if (attributes != null) {
				attributes.remove(publishTablePathRenderer.getMapname());
			}
		}

		// check whether the dependency stacks/lists are all cleared now
		// (otherwise it is most likely that dependencies were not stored
		// correctly)
		if (!t.getRenderType().areDependenciesCleared()) {
			throw new NodeException("Error while rendering {" + page + "}: dependencies were not stored!");
		}
		t.setRenderResult(renderResult);

		boolean needsRepublish = allowRepublish && pageRenderResult.needsRepublish();

		if (reportToPublishQueue) {
			if (!omitPublishTable) {
				PublishQueue.initiatePublishAction(Page.TYPE_PAGE, pageId, nodeId, PublishAction.UPDATE_PUBLISH_TABLE);
			}
			if (contentMap != null) {
				PublishQueue.initiatePublishAction(Page.TYPE_PAGE, pageId, nodeId, PublishAction.WRITE_CR);
			} else if (mp != null) {
				PublishQueue.initiatePublishAction(Page.TYPE_PAGE, pageId, nodeId, PublishAction.WRITE_CR);
			}
		}
		if (!needsRepublish && !omitPublishTable) {
			if (t instanceof MulticonnectionTransaction) {
				MulticonnectionTransaction mt = (MulticonnectionTransaction) t;
				PublishtableUpdateJob updateJob = new PublishtableUpdateJob(page, source, publishTablePath, mt, ObjectTransformer.getInt(t.getChannelId(), 0),
						nodeId, reportToPublishQueue);

				mt.addAsynchronousJob(updateJob);
			} else {
				updatePageSource(page, source, publishTablePath);
				if (reportToPublishQueue) {
					PublishQueue.reportPublishActionDone(Page.TYPE_PAGE, pageId, nodeId, PublishAction.UPDATE_PUBLISH_TABLE);
				}
			}
		}

		if (contentMap != null) {
			cnMapPublisher.publishPage(page, renderedAttributes, source, reportToPublishQueue);
		} else if (mp != null) {
			mp.publishPage(node, page, renderedAttributes, source, attributes, reportToPublishQueue);
		}

		return needsRepublish;
	}

	/**
	 * write the source of a page to the publish db.
	 * @param page the page to write.
	 * @param source the translated source of the page.
	 * @param path path
	 * @throws SQLException
	 */
	private void updatePageSource(final Page page, String source, String path) throws SQLException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean niceUrlsFeature = NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS);

		final Folder folder = page.getFolder();
		final Node node = folder.getNode();

		// TODO check if absolutepath/canonicalpath is a real subdir or if someone is doing some nasty ../

		// TODO maybe do delayed update, every 10 pages or so

		Map<String, Object> idMap = new HashMap<String, Object>();
		idMap.put("page_id", page.getId());
		idMap.put("node_id", node.getId());

		Map<String, Object> data = new HashMap<String, Object>();
		if (source != null) {
			data.put("source", source);
		}
		data.put("active", 1);
		data.put("path", path);
		data.put("filename", page.getFilename());
		data.put("pdate", t.getUnixTimestamp());
		data.put("folder_id", folder.getMaster().getId());
		data.put("updateimagestore", node.doPublishFilesystem() ? 1 : 0);

		if (niceUrlsFeature && !ObjectTransformer.isEmpty(page.getNiceUrl())) {
			data.put("nice_url", new StringBuffer(node.getHostname()).append(page.getNiceUrl()).toString());
		}

		// also remove all entries for other channelset IDs of this page from the node (only one channelset variant of the page may be published into a node/channel)
		for (Object id : page.getChannelSet().values()) {
			DBUtils.executeUpdate("UPDATE publish SET active = 0 WHERE page_id = ? AND node_id = ?", new Object[] {id, node.getId()});
		}

		DBUtils.updateOrInsert("publish", idMap, data);

		if (niceUrlsFeature) {
			PublishtableUpdateJob.updateAlternateUrls(page, node);
		}

		t.commit(false);

	}

	/**
	 * finish page publishing by updating page status ..
	 * this should be done in a new transaction.
	 * 
	 * this method will commit the transaction, this has to be done by the caller.
	 */
	public void finish() throws NodeException {/*
		 final Transaction t = TransactionManager.getCurrentTransaction();

		 final List publishedIds = new ArrayList(pageStatusUpdates.size());
		 for(Iterator i = pageStatusUpdates.iterator() ; i.hasNext() ; ) {
		 PageStatusUpdate statusUpdate = (PageStatusUpdate) i.next();
		 if(statusUpdate.status == Page.STATUS_PUBLISHED) {
		 publishedIds.add(statusUpdate.pageId);
		 }
		 }
		 // Asserting that all pages in statusUpdate were actually of STATUS_PUBLISHED
		 if(publishedIds.size() != pageStatusUpdates.size()) {
		 throw new NodeException("Internal Error - Found page status != STATUS_PUBLISHED");
		 }

		 // set all pages from status 1 to status 2, which were published in this run, but not dirted after the publishrun started
		 DBUtils.executeMassStatement("UPDATE page SET status = 2 WHERE status = 1 AND ddate <= ? AND id IN ", null, publishedIds, 2, new SQLExecutor() {
		 protected int all = publishedIds.size();

		 public void prepareStatement(PreparedStatement stmt) throws SQLException {
		 stmt.setInt(1, t.getUnixTimestamp());
		 }

		 public void handleUpdateCount(int count) throws NodeException {
		 all -= count;
		 t.getRenderResult().info(PagePublisher.class, "Updated the page status of {" + count + "} pages. {" + all + "} still to go (total rendered pages: {" + publishedIds.size() + "}.");
		 }
		 }, Transaction.UPDATE_STATEMENT);
		 
		 t.commit(false);
		 */}
    
	/**
	 * Publish all dirted pages.
	 * @param renderResult render result
	 * @throws NodeException
	 * @throws GenericFailureException 
	 */
	public void publishPages(RenderResult renderResult) throws NodeException, GenericFailureException {
		if (workPhase != null) {
			workPhase.begin();
		}
		long startRender = System.currentTimeMillis();
		Transaction t = TransactionManager.getCurrentTransaction();

		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.PUBLISH_STATS)) {
			// activate statistics
			t.enableStatistics(true);
			PublishablePage.enableStatistics(true);
		}

		try {
			int totalPageSize = 0;

			// iterate over all nodes, that need to be published
			List<Node> nodes = Publisher.getPublishedNodes();

			for (Node node : nodes) {
				// get the pages to update
				List<NodeObjectWithAttributes<Page>> dirtedPages = null;
				try (PublishCacheTrx trx = new PublishCacheTrx(false)) {
					if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.ATTRIBUTE_DIRTING)) {
						dirtedPages = PublishQueue.getDirtedObjectsWithAttributes(Page.class, node);
					} else {
						dirtedPages = PublishQueue.getDirtedObjects(Page.class, node);
					}
				}
				List<NodeObjectIdWithAttributes> dirtedPageIds = new ArrayList<>(dirtedPages.size());
				for (NodeObjectWithAttributes<Page> page : dirtedPages) {
					dirtedPageIds.add(new NodeObjectIdWithAttributes(page));
				}

				totalPageSize += dirtedPageIds.size();

				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					publishPages(node, dirtedPageIds, renderResult);
				} finally {
					DependencyManager.clearPreparedDependencies();
				}
			}

			long duration = System.currentTimeMillis() - startRender;

			TransactionStatistics transStats = t.getStatistics();
			if (transStats != null) {
				for (TransactionStatistics.Item item : TransactionStatistics.Item.values()) {
					renderResult.info(Publisher.class, item.getDescription() + ": \t" + transStats.get(item).getInfo());
				}
			}
			PublishablePageStatistics stats = PublishablePage.getStatistics();
			if (stats != null) {
				for (PublishablePageStatistics.Item item : PublishablePageStatistics.Item.values()) {
					renderResult.info(Publisher.class, item.getDescription() + ": \t" + stats.get(item).getInfo());
				}
			}

			renderResult.info(Publisher.class,
					"Rendered " + totalPageSize + " pages in " + duration + " ms"
					+ (totalPageSize > 0 && duration > 0
					? " (" + (totalPageSize * 1000 / duration) + " pages/sec, avg. " + (duration / totalPageSize) + " ms/page)"
					: ""));
		} finally {
			if (workPhase != null) {
				workPhase.done();
			}

			// deactivate statistics
			t.enableStatistics(false);
			PublishablePage.enableStatistics(false);
		}
	}

	/**
	 * Publish the pages for the given node
	 * @param node
	 * @param dirtedPageIds
	 * @param renderResult render result
	 * @throws NodeException
	 * @throws GenericFailureException 
	 */
	protected void publishPages(Node node, List<NodeObjectIdWithAttributes> dirtedPageIds, RenderResult renderResult) throws NodeException, GenericFailureException {
		if (logger.isInfoEnabled()) {
			logger.info("Starting rendering of " + dirtedPageIds.size() + " pages for Node " + node);
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		TransactionStatistics stats = t.getStatistics();
		renderResult.info(Publisher.class, "Starting rendering of " + dirtedPageIds.size() + " pages for Node " + node);

		List<NodeObjectIdWithAttributes> nextPublishPages = dirtedPageIds;

		// Make at most 2 publish runs ..
		for (int publishRun = 0; publishRun < 2 && nextPublishPages. /**/size() > 0; publishRun++) {
			// do not handle normal dependencies in publish run 1
			if (publishRun > 0) {
				TransactionManager.getCurrentTransaction().getRenderType().setHandleDependencies(false);
			}

			Iterator<NodeObjectIdWithAttributes> i = nextPublishPages.iterator();

			if (logger.isInfoEnabled()) {
				logger.info("Starting publish run {" + publishRun + "} - {" + nextPublishPages.size() + "} pages to go.");
			}
			renderResult.info(Publisher.class, "Starting publish run {" + publishRun + "} - {" + nextPublishPages.size() + "} pages to go.");
			nextPublishPages = new ArrayList<>();

			// Iterate through all pages
			for (; i.hasNext();) {
				if (cnMapPublisher != null) {
					cnMapPublisher.keepContentmapsAlive();
				}

				NodeObjectIdWithAttributes pageIdWithAttributes = i.next();

				try (PublishedNodeTrx pnTrx = new PublishedNodeTrx(node)) {
					if (PublishController.getState() != PublishController.State.running) {
						throw new NodeException(String.format("Stop publishing pages, because publisher state is %s", PublishController.getState()));
					}

					if (stats != null) {
						stats.get(Item.RENDER_PAGE).start();
					}
					long startPage = System.currentTimeMillis();
					Page page = factory.getPage(pageIdWithAttributes.id);
					String infomsg = "Publishing page {" + pageIdWithAttributes + "}";

					renderResult.info(Publisher.class, infomsg);

					if (pageMayBePublished(page, node, renderResult)) {
						if (logger.isInfoEnabled()) {
							logger.info(infomsg);
						}
						long[] times = new long[2];
						boolean needsRepublish = update(renderResult, page, publishRun == 0, pageIdWithAttributes.attributes, times);

						infomsg = getLogMessage(page, pageIdWithAttributes.id, System.currentTimeMillis() - startPage, times, needsRepublish);
						renderResult.info(Publisher.class, infomsg);

						if (!ObjectTransformer.isEmpty(pageIdWithAttributes.attributes)) {
							renderResult.debug(Publisher.class, "Attributes for " + page + ": " + pageIdWithAttributes.attributes);
						}

						if (workPhase != null) {
							workPhase.doneWork();
						}
						if (needsRepublish) {
							// Page needs republishing (probably because of XNL module...)
							nextPublishPages.add(pageIdWithAttributes);
						} else {
							if (workPhase != null) {
								workPhase.doneWork();
							}
						}

						// reduce the remaining page count
						publishInfo.pageRendered();
						MBeanRegistry.getPublisherInfo().publishedPage(node.getId());
					}
				} catch (RecoverableException e) {
					logger.error("publishing of page {" + pageIdWithAttributes + "} failed. - Ignoring and proceeding with next page.", e);
				} catch (NodeException e) {
					if (force) {
						renderResult.error(Publisher.class, "publishing of page {" + pageIdWithAttributes + "} failed: " + e.toString());
					} else {
						logger.error("Error while rendering page. {" + pageIdWithAttributes + "}", e);
						throw e;
					}
				} catch (Exception e) {
					if (force) {
						renderResult.error(Publisher.class, "publishing of page {" + pageIdWithAttributes + "} failed: " + e.toString());
						logger.error("publishing of page {" + pageIdWithAttributes + "} failed.", e);
					} else {
						throw new GenericFailureException("publishing of page {" + pageIdWithAttributes + "} failed.", e);
					}
				} finally {
					if (stats != null) {
						stats.get(Item.RENDER_PAGE).stop();
					}
				}
			}

			// set handle dependencies to true again
			if (publishRun > 0) {
				t.getRenderType().setHandleDependencies(true);
			}
		}
	}

	 /**
	 * Clean the publish table for all nodes that have publishing not disabled
	 * @param renderResult render result
	 * @throws NodeException
	 */
	protected void cleanPublishTable(RenderResult renderResult) throws NodeException {
		if (renderResult != null) {
			renderResult.info(PagePublisher.class, "Start cleaning publish table");
		}

		// first remove all publishtable entries for nodes that do not exist
		IntegerColumnRetriever allNodes = new IntegerColumnRetriever("id");
		DBUtils.executeStatement("SELECT id FROM node", allNodes);
		if (!allNodes.getValues().isEmpty()) {
			DBUtils.executeUpdate("DELETE FROM publish WHERE node_id NOT IN (" + StringUtils.repeat("?", allNodes.getValues().size(), ",") + ")", allNodes
					.getValues().toArray());
		}

		final List<Integer> nodeIds = new ArrayList<Integer>();

		DBUtils.executeStatement("SELECT id FROM node WHERE disable_publish = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, 0); // disable_publish = ?
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					nodeIds.add(rs.getInt("id"));
				}
			}
		});
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean omitPublishTable = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.OMIT_PUBLISH_TABLE);
		List<Node> nodes = t.getObjects(Node.class, nodeIds);

		for (Node node : nodes) {
			if (omitPublishTable && !node.doPublishFilesystem()) {
				renderResult.info(PagePublisher.class, "Omit " + node + ", because it does not write into filesystem");
				continue;
			}
			cleanPublishTable(node, renderResult);
		}

		if (renderResult != null) {
			renderResult.info(PagePublisher.class, "Finished cleaning publish table");
		}
	}

	/**
	 * Select the publish table entries (by id) with the given select statement and set their active value to 0
	 * @param selectSQL select statement, that must return a single column "id" (being the ids of publish table entries to set inactive).
	 * @param ex SQLExecutor instance that may prepare the statement by setting bind variables. The handleResultSet method of this instance will not be used.
	 * @throws NodeException
	 */
	protected void setInactiveInPublishTable(String selectSQL, SQLExecutor ex) throws NodeException {
		final List<Integer> publishIds = new ArrayList<Integer>();

		DBUtils.executeStatement(selectSQL, new SQLExecutorWrapper(ex) {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					publishIds.add(rs.getInt("id"));
				}
			}
		});
		DBUtils.executeMassStatement("DELETE FROM publish WHERE id IN", null, publishIds, 1, null, Transaction.DELETE_STATEMENT);
	}

	/**
	 * Clean the publish table for the given node: Set all entries for pages, that cannot publish into the given node to active=0
	 * Pages cannot publish into the given node, when:
	 * <ol>
	 * <li>They don't exist (any more)</li>
	 * <li>They belong to a folder of another node</li>
	 * </ol>
	 * @param node
	 * @param renderResult render result
	 * @throws NodeException
	 */
	protected void cleanPublishTable(final Node node, RenderResult renderResult) throws NodeException {
		if (renderResult != null) {
			renderResult.info(PagePublisher.class, "Start cleaning publish table for " + node);
		}

		// disable publish entries for pages that were moved into different folders
		setInactiveInPublishTable(
				"SELECT publish.id FROM publish LEFT JOIN page ON page_id = page.id WHERE publish.node_id = ? AND publish.folder_id != page.folder_id AND active = ?",
				new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ObjectTransformer.getInt(node.getId(), 0)); // publish.node_id = ?
				stmt.setInt(2, 1); // active = ?
			}
		});

		// disable publish entries for pages that were moved to different nodes
		if (node.isChannel()) {
			final List<Integer> nodeIds = new ArrayList<Integer>();

			nodeIds.add(ObjectTransformer.getInt(node.getId(), 0));
			List<Node> masterNodes = node.getMasterNodes();

			for (Node master : masterNodes) {
				nodeIds.add(ObjectTransformer.getInt(master.getId(), 0));
			}
			StringBuffer sql = new StringBuffer();

			sql.append(
					"SELECT publish.id FROM publish LEFT JOIN page ON page_id = page.id LEFT JOIN folder ON page.folder_id = folder.id WHERE publish.node_id = ? AND active = ? AND folder.node_id NOT IN (");
			sql.append(StringUtils.repeat("?", nodeIds.size(), ","));
			sql.append(")");
			setInactiveInPublishTable(sql.toString(), new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, ObjectTransformer.getInt(node.getId(), 0)); // publish.node_id = ?
					stmt.setInt(2, 1); // active = ?
					int pCounter = 3;

					for (Integer nodeId : nodeIds) {
						stmt.setInt(pCounter++, nodeId); // folder.node_id NOT IN (?, ...)
					}
				}
			});
		} else {
			setInactiveInPublishTable(
					"SELECT publish.id FROM publish LEFT JOIN page ON page_id = page.id LEFT JOIN folder ON page.folder_id = folder.id WHERE publish.node_id = ? AND folder.node_id != ? AND active = ?",
					new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, ObjectTransformer.getInt(node.getId(), 0)); // publish.node_id = ?
					stmt.setInt(2, ObjectTransformer.getInt(node.getId(), 0)); // folder.node_id != ?
					stmt.setInt(3, 1); // active = ?
				}
			});
		}

		// disable publish entries for pages that don't exist
		setInactiveInPublishTable(
				"SELECT publish.id FROM publish LEFT JOIN page ON page_id = page.id WHERE publish.node_id = ? AND active = ? AND page.id IS NULL", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ObjectTransformer.getInt(node.getId(), 0)); // publish.node_id = ?
				stmt.setInt(2, 1); // active = ?
			}
		});

		// disable publish entries for pages that were removed, hidden or taken offline
		List<Integer> removedPageIds = PublishQueue.getRemovedObjectIds(Page.class, true, node);
		if (!removedPageIds.isEmpty()) {
			final List<Integer> publishIds = new ArrayList<Integer>();
			DBUtils.executeMassStatement("SELECT id FROM publish WHERE node_id = ? AND page_id IN ", removedPageIds, 2, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, ObjectTransformer.getInt(node.getId(), 0));
				}
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						publishIds.add(rs.getInt("id"));
					}
				}
			});

			if (!publishIds.isEmpty()) {
				DBUtils.executeMassStatement("UPDATE publish SET active = 0 WHERE id IN ", null, publishIds, 1, null, Transaction.UPDATE_STATEMENT);
			}
		}

		if (renderResult != null) {
			renderResult.info(PagePublisher.class, "Finished cleaning publish table for " + node);
		}
	}

	/**
	 * Check whether the given page may be published
	 * @param page page
	 * @param node node
	 * @param renderResult render result
	 * @return true if the page may be published, false if not
	 * @throws NodeException
	 */
	public static boolean pageMayBePublished(Page page, Node node, RenderResult renderResult) throws NodeException {
		// check for trivial cases
		if (page == null) {
			return false;
		}

		// check the online flag (just to make sure)
		if (!page.isOnline()) {
			renderResult.info(Publisher.class, "Omit " + page + ", because it is not online");
			return false;
		}

		// check whether the page's markup language is excluded from publishing
		if (page.getTemplate().getMarkupLanguage().isExcludeFromPublishing()) {
			renderResult.info(Publisher.class, String.format("Omit %s, because it uses %s, which is excluded from publishing", page, page.getTemplate().getMarkupLanguage()));
			return false;
		}

		// When the page was already instant published, it must not be published again during this run.
		if (PublishController.wasInstantPublished(page)) {
			PublishQueue.requeueObject(page, node);
			renderResult.info(Publisher.class, "Omit " + page + ", because it was already instant published");
			return false;
		}

		// when versioned publishing is disabled, we only may publish pages that have status 1
		Transaction t = TransactionManager.getCurrentTransaction();

		if (t.getNodeConfig().getDefaultPreferences().getFeature("disable_versioned_publishing") && page.isModified()) {
			renderResult.info(Publisher.class, "Omit " + page + ", because it is modified");
			return false;
		}

		// in all other cases, we are allowed to publish the page
		return true;
	}

	/**
	 * Get the log message for a published page
	 * @param page published page
	 * @param pageId page Id
	 * @param totalTime total time
	 * @param times optional times for rendering content and attributes
	 * @param needsRepublish true if the page needs to be republished
	 * @return info message
	 */
	public static String getLogMessage(Page page, Integer pageId, long totalTime, long times[], boolean needsRepublish) {
		StringBuilder info = new StringBuilder(255);
		info.append("Published page {").append(page.getFilename()).append("} / {").append(pageId).append("} in {");
		info.append(totalTime);
		if (times != null && times.length >= 2) {
			info.append("/").append(times[0]).append("/").append(times[1]);
		}
		info.append(" ms}.");
		if (needsRepublish) {
			info.append(" Needs Republish.");
		}
		return info.toString();
	}

	/**
	 * Prepare a map holding the version timestamps of the published versions of the given pages.
	 * Keys will be the page ids, values the version timestamps
	 * @param pages list of pages
	 * @return map holding the version timestamps
	 * @throws NodeException
	 */
	public Map<Integer, Integer> preparePublishedTimestamps(final Set<Integer> pageIds) throws NodeException {
		if (ObjectTransformer.isEmpty(pageIds)) {
			return Collections.emptyMap();
		}
		final Map<Integer, Integer> publishedTimestamps = new HashMap<Integer, Integer>(pageIds.size());
		StringBuilder sql = new StringBuilder("SELECT o_id, timestamp FROM nodeversion WHERE o_type = ? AND o_id IN (");
		sql.append(StringUtils.repeat("?", pageIds.size(), ","));
		sql.append(") AND published = ?");
		DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int idCounter = 1;
				stmt.setInt(idCounter++, Page.TYPE_PAGE);
				for (Integer pageId : pageIds) {
					stmt.setObject(idCounter++, pageId);
				}
				stmt.setInt(idCounter++, 1);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					publishedTimestamps.put(rs.getInt("o_id"), rs.getInt("timestamp"));
				}
			}
		});
		return publishedTimestamps;
	}
}
