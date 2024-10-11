/*
 * @author Stefan Hepp
 * @date 24.12.2005
 * @version $Id: CnMapPublisher.java,v 1.37.4.2 2011-02-07 14:56:04 norbert Exp $
 */
package com.gentics.contentnode.publish;

import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.DatasourceInfo;
import com.gentics.api.lib.datasource.WritableMultichannellingDatasource;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.StreamingResolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.AsynchronousJob;
import com.gentics.contentnode.etc.AsynchronousWorker;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.ContentMap.ContentMapTrx;
import com.gentics.contentnode.etc.ContentMapTrxHandler;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.GroupableAsynchronousJob;
import com.gentics.contentnode.etc.JobGroup;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.factory.HandleDependenciesTrx;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.PublishedNodeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.factory.object.FileOnlineStatus.FileListForNode;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.contentnode.publish.PublishQueue.PublishAction;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.PropertyStackResolver;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.content.FilesystemAttributeStatistics;
import com.gentics.lib.content.FilesystemAttributeValue;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.lib.datasource.mccr.MCCRStats;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementException;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectManagementManager.ObjectTypeDiff;
import com.gentics.lib.datasource.object.ObjectManagementManager.TypeDiff;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.ContentMapStatistics;
import com.gentics.lib.etc.ContentMapStatistics.Item;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;
import com.gentics.lib.util.FileUtil;

/**
 * The CnMap Publisher updates all configured and contentmaps.
 */
public class CnMapPublisher {
	/**
	 * Lambda that transforms a value into a link
	 */
	public static BiFunction<TagmapEntryRenderer, Object, Object> LINKTRANSFORMER = (entry, value) -> {
		if (value instanceof NodeObject) {
			return String.format("%d.%d", entry.getTargetType(), ((NodeObject) value).getId());
		} else {
			return String.format("%d.%s", entry.getTargetType(), value);
		}
	};

	private NodeConfig config;
	private File dbfilesDir;

	private SimplePublishInfo publishInfo;

	/**
	 * map of contentmaps for nodes, keys are Node instances, values are the
	 * contentmaps (if the node has publishing into contentmap enabled)
	 */
	private Map<Node, ContentMap> nodeContentMaps = new HashMap<Node, ContentMap>();

	/**
	 * list containing all content maps that will really be used for publishing
	 * (values of map nodeContentMaps, but every contentMap just contained once)
	 */
	private List<ContentMap> usedContentMaps = new Vector<ContentMap>();

	/**
	 * Map of asynchronous workers for the used contentmaps (if multithreaded publishing is used)
	 */
	private Map<Integer, AsynchronousWorker> contentMapWorkers = new HashMap<Integer, AsynchronousWorker>();

	/**
	 * the logger for the publisher
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(CnMapPublisher.class);

	private IWorkPhase syncPhase;
	private IWorkPhase publishPhase;
	private IWorkPhase delPhase;
	private List<PublishObjectTask> publishFolderObjectTaskQueue;
	private List<PublishObjectTask> publishFileObjectTaskQueue;

	private boolean isMultithreaded;

	protected final static int JOBGROUPSIZE_DEFAULT = 100; 

	protected final static int JOBBATCHSIZE_DEFAULT = 1;

	/**
	 * Job group size for grouped jobs (default is 100)
	 */
	protected static int jobGroupSize = JOBGROUPSIZE_DEFAULT;

	/**
	 * Batch Size
	 */
	protected static int jobBatchSize = JOBBATCHSIZE_DEFAULT;

	/**
	 * Don't forget to call {@link #init()} before using this CnMapPublisher !
	 * @param config
	 * @param publishInfo publish info
	 */
	public CnMapPublisher(NodeConfig config, SimplePublishInfo publishInfo) {
		this.config = config;
		this.publishInfo = publishInfo;
		NodePreferences prefs = config.getDefaultPreferences();

		dbfilesDir = new File(ConfigurationValue.DBFILES_PATH.get());
		isMultithreaded = prefs.isFeature(Feature.MULTITHREADED_PUBLISHING);
		jobGroupSize = ObjectTransformer.getInt(prefs.getProperty("multithreaded_publishing.groupsize"), JOBGROUPSIZE_DEFAULT);
		jobBatchSize = ObjectTransformer.getInt(prefs.getProperty("multithreaded_publishing.batchsize"), JOBBATCHSIZE_DEFAULT);
	}

	/**
	 * @param config
	 * @param publishInfo publish info
	 * @param publishFolderObjectTaskQueue
	 * @param publishFileObjectTaskQueue
	 */
	public CnMapPublisher(NodeConfig config, SimplePublishInfo publishInfo,
			List<PublishObjectTask> publishFolderObjectTaskQueue, List<PublishObjectTask> publishFileObjectTaskQueue) {
		this(config, publishInfo);
		this.publishFolderObjectTaskQueue = publishFolderObjectTaskQueue;
		this.publishFileObjectTaskQueue = publishFileObjectTaskQueue;
	}

	/**
	 * Initializes contentmap handlers and MCCR channel structures
	 * @param renderResult Logs to this object. No logging if null.
	 * @throws NodeException
	 */
	public void init(RenderResult renderResult) throws NodeException {
		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Initializing contentmap handlers");
		}
		initContentMaps();

		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Initializing channel structure for MCCRs");
		}
		initChannelStructures();
	}

	/**
	 * Initialize the contentmap handles per node
	 * @throws NodeException
	 */
	protected void initContentMaps() throws NodeException {
		// get all nodes that publish into a contentmap (and the contentmap keys)
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement st = null;
		ResultSet res = null;        
		Collection<ContentMap> contentMaps = config.getContentMaps();

		for (ContentMap cm : contentMaps) {
			// reset "changed" status for each ContentMap
			logger.debug("Initializing contentmap - setChange(false)" + cm);
			cm.setChanged(false);
		}
		List<ContentMap> mapsWithRunningTransactions = new Vector<ContentMap>();
		try {
			st = t.prepareStatement("SELECT id, contentrepository_id FROM " + "node WHERE publish_contentmap = ? AND disable_publish = ?");
			st.setInt(1, 1);
			st.setInt(2, 0);
			res = st.executeQuery();
			while (res.next()) {
				Node node = t.getObject(Node.class, res.getInt("id"));

				if (node != null) {
					Integer cnMapKey = res.getInt("contentrepository_id");

					for (ContentMap contentMap : contentMaps) {
						if (contentMap.getId().equals(cnMapKey)) {
							// for multichannelling aware content repositories, check whether the feature is enabled
							if (contentMap.isMultichannelling() && !NodeConfigRuntimeConfiguration.isFeature(Feature.MCCR)) {
								st.close();
								res.close();
								throw new NodeException(
										"Error while initializing the content repository " + contentMap.getName()
										+ ": Content Repository is Multichannelling Aware, but the feature MCCR is not licensed!");
							}

							// put node -> contentmap into the map
							nodeContentMaps.put(node, contentMap);

							// add the node to the contentmap
							contentMap.addNode(node);
                            
							logger.debug("We need content map {" + contentMap + "} for node {" + node + "}");
							// put the contentmap into the list (if not yet contained) and start a transaction
							if (!usedContentMaps.contains(contentMap)) {
								// contentMap.startTransaction();
								usedContentMaps.add(contentMap);
							}

							// if the contentmap is multichannelling, we add it for all channels of the node that do not have a different CR assigned
							if (contentMap.isMultichannelling()) {
								Collection<Node> allChannels = node.getAllChannels();

								for (Node channel : allChannels) {
									if (ContentRepository.isEmptyId(channel.getContentrepositoryId()) || Objects
											.equals(channel.getContentrepositoryId(), node.getContentrepositoryId())) {
										nodeContentMaps.put(channel, contentMap);
										contentMap.addNode(channel);
									}
								}
							}

							break;
						}
					}
				}
			}

			t.closeResultSet(res);
			res = null;
			t.closeStatement(st);
			st = null;

			boolean publishStats = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.PUBLISH_STATS);

			if (publishStats) {
				FilesystemAttributeValue.enableStatistics(true);
				MCCRHelper.enableStatistics(true);
			}

			// start transactions

			for (ContentMap contentMap : usedContentMaps) {
				try {
					if (publishStats) {
						contentMap.enableStatistics(true);
					}
					// when instant publishing is turned on, we will use a single transaction for every published object
					if (!contentMap.isInstantPublishing()) {
						contentMap.startTransaction();
						mapsWithRunningTransactions.add(contentMap);
					}

					// if multithreaded publishing is used, generate a worker for the contentmap
					if (isMultithreaded) {
						AsynchronousWorker contentMapWorker = new CnMapWorker("Worker for " + contentMap.toString(), true, t);

						contentMapWorkers.put(contentMap.getId(), contentMapWorker);
						contentMapWorker.start();
					}
				} catch (Exception e) {
					throw new NodeException("Error while intializing contentmap " + contentMap, e);
				}
			}
		} catch (Exception e) {
			// We only need to end the transactions in this method if there is an exception inside it
			// if not, they must be kept open
			List<ContentMap> mapsWithRollbackErrors = new ArrayList<ContentMap>();
			for (ContentMap map : mapsWithRunningTransactions) {
				try {
					map.rollback(true);
				} catch (Exception e1) {
					mapsWithRollbackErrors.add(map);
				}
			}
			if (!mapsWithRollbackErrors.isEmpty()) {
				throw new NodeException("Error while intializing contentmap handles and rolling back transactions for " + mapsWithRollbackErrors.toString(), e);
			}
			throw new NodeException("Error while intializing contentmap handles", e);
		} finally {
			t.closeResultSet(res);
			res = null;
			t.closeStatement(st);
			st = null;
		}
	}
    
	/**
	 * For all currently used multichannelling contentrepositories, store the channel structures (for all nodes assigned to the contentrepository, not only those that
	 * have publish enabled)
	 * 
	 * @throws NodeException
	 */
	protected void initChannelStructures() throws NodeException {
		for (final ContentMap contentMap : usedContentMaps) {
			// omit non-multichannelling content repositories
			if (!contentMap.isMultichannelling()) {
				continue;
			}
			// get all nodes that have the contentrepository assigned
			final List<Integer> nodeIds = new Vector<Integer>();

			DBUtils.executeStatement("SELECT id FROM node WHERE publish_contentmap = ? AND contentrepository_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, 1); // publish_contentmap = ?
					stmt.setInt(2, contentMap.getId()); // contentrepository_id = ?
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						nodeIds.add(rs.getInt("id"));
					}
				}
			});
			Transaction t = TransactionManager.getCurrentTransaction();
			List<Node> nodes = t.getObjects(Node.class, nodeIds);

			// omit channels (they shouldn't be assigned anyway)
			for (Iterator<Node> i = nodes.iterator(); i.hasNext();) {
				Node node = i.next();

				if (node.isChannel()) {
					i.remove();
				}
			}

			// construct the tree and save it
			ChannelTree tree = createChannelTree(nodes);

			try (ContentMapTrx trx = contentMap.startInstantPublishingTrx()) {
			contentMap.getMCCRDatasource().saveChannelStructure(tree);
				if (trx != null) {
					trx.setSuccess();
		}
	}
		}
	}

	/**
	 * Create the channel tree for the given collection of nodes
	 * @param nodes collection of nodes
	 * @return channel tree
	 * @throws NodeException
	 */
	protected ChannelTree createChannelTree(Collection<Node> nodes) throws NodeException {
		ChannelTree tree = new ChannelTree();

		tree.getChildren().addAll(recursiveGetChannelTree(nodes));
		return tree;
	}

	/**
	 * Recursive method to create the channel tree
	 * @param nodes collection of nodes
	 * @return list of channel tree nodes
	 * @throws NodeException
	 */
	protected Collection<ChannelTreeNode> recursiveGetChannelTree(Collection<Node> nodes) throws NodeException {
		Collection<ChannelTreeNode> treeNodes = new Vector<ChannelTreeNode>(nodes.size());

		for (Node node : nodes) {
			ChannelTreeNode treeNode = new ChannelTreeNode(new DatasourceChannel(ObjectTransformer.getInt(node.getId(), 0), node.getFolder().getName()));

			treeNodes.add(treeNode);
			treeNode.getChildren().addAll(recursiveGetChannelTree(node.getChannels()));
		}
		return treeNodes;
	}

	/**
	 * Commits all open transactions.
	 *
	 */
	public void commit() {
		for (ContentMap contentMap : usedContentMaps) {
			try {
				// we will commit the transaction and close the connection of the contentmap in any case.
				// because even if instant publishing was used, the connection is still open
				contentMap.commit(true);
			} catch (Exception e) {
				logger.error("Error while comitting transaction for {" + contentMap + "}", e);
				// rollback the contentmap
				try {
					contentMap.rollback(true);
				} catch (Exception e1) {
					logger.error("Error while rolling back transaction for {" + contentMap + "}", e);
				}
			}
		}
	}
    
	/**
	 * Rollsback all open transactions.
	 */
	public void rollback() {
		for (ContentMap contentMap : usedContentMaps) {
			try {
				// abort the workers
				AsynchronousWorker worker = contentMapWorkers.get(contentMap.getId());

				if (worker != null) {
					worker.abort();
				}

				// disable statistics
				contentMap.enableStatistics(false);

				// we will rollback the transaction and close the connection of the contentmap in any case.
				// because even if instant publishing was used, the connection is still open
				contentMap.rollback(true);
			} catch (SQLException e) {
				logger.error("Error while rolling back transaction for {" + contentMap + "}", e);
			}
		}
	}

	/**
	 * Publish the page into the contentmap
	 * @param page page to publish
	 * @param tagmapEntries map of tagmap entries 
	 * @param source rendered source of the page
	 * @param reportToPublishQueue true to report back to the publish queue when done
	 * @throws NodeException
	 */
	public void publishPage(final Page page, Map<TagmapEntryRenderer, Object> tagmapEntries, String source, final boolean reportToPublishQueue) throws NodeException {
		// check whether the page shall be published into the contentmap
		final Node node = page.getFolder().getNode();

		if (node.doPublishContentMapPages() && nodeContentMaps.containsKey(node)) {
			ContentMap contentMap = nodeContentMaps.get(node);

			try (ContentMapTrx trx = isMultithreaded ? null : contentMap.startInstantPublishingTrx(new ContentMapTrxHandler() {
				@Override
				public void handle() throws NodeException {
						if (reportToPublishQueue) {
							PublishQueue.reportPublishActionDone(Page.TYPE_PAGE, ObjectTransformer.getInt(page.getId(), 0),
									ObjectTransformer.getInt(node.getId(), 0), PublishAction.WRITE_CR);
						}
					}
			})) {
				writePageIntoCR(page, contentMap, tagmapEntries, source, contentMapWorkers.get(contentMap.getId()), reportToPublishQueue).operate();
				if (trx != null) {
					trx.setSuccess();
				}
			}
		}
	}
    
	/**
	 * Asynchronous job to write a page into a content repository, when multithreaded publishing is used
	 */
	private static class AsynchronousCnMapUpdate implements GroupableAsynchronousJob {

		/**
		 * contentmap instance
		 */
		private ContentMap contentMap;

		/**
		 * Map containing the data
		 */
		private Map<String, Object> dataMap;

		/**
		 * ID of the page
		 */
		private int pageId;

		/**
		 * ID of the channel
		 */
		private int channelId;

		/**
		 * ID of the folder
		 */
		private int folderId;

		/**
		 * True if the successful writing of the object shall be reported to the publish queue
		 */
		private boolean reportToPublishQueue = false;

		/**
		 * Create an instance of the job
		 * @param contentMap contentmap
		 * @param dataMap map containing the data to be written into the contentmap
		 * @param pageId page id
		 * @param channelId channel id
		 * @param folderId folder id 
		 * @param reportToPublishQueue true if success shall be reported to the publish queue
		 */
		public AsynchronousCnMapUpdate(ContentMap contentMap, Map<String, Object> dataMap, int pageId, int channelId, int folderId, boolean reportToPublishQueue) {
			this.contentMap = contentMap;
			this.dataMap = dataMap;
			this.pageId = pageId;
			this.channelId = channelId;
			this.folderId = folderId;
			this.reportToPublishQueue = reportToPublishQueue;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.etc.AsynchronousJob#process()
		 */
		public int process(RenderResult renderResult) throws Exception {
			// keep the current transaction alive
			Transaction t = TransactionManager.getCurrentTransaction();

			if (t != null) {
				t.keepAlive();
			}

			contentMap.startWrite(Page.TYPE_PAGE);
			try (ContentMapTrx trx = contentMap.startInstantPublishingTrx(new ContentMapTrxHandler() {
				@Override
				public void handle() throws NodeException {
					if (reportToPublishQueue) {
						PublishQueue.reportPublishActionDone(Page.TYPE_PAGE, pageId, channelId, PublishAction.WRITE_CR);
				}
			}
			})) {
				Changeable changeable = prepareChangeableForPage(contentMap, dataMap, channelId, folderId);
				boolean objectExists = checkObjectExistence(changeable);

				try {
					if (objectExists) {
						contentMap.handleUpdateObject(changeable, Collections.unmodifiableSet(dataMap.keySet()));
					} else {
						contentMap.handleCreateObject(changeable);
					}
				} catch (CnMapPublishException e) {
					throw new NodeException("Publish object {" + changeable + "} into the ContentRepository failed (publish handler threw exception)", e);
				}

				if (contentMap.isMultichannelling()) {
					contentMap.getMCCRDatasource().setChannel(channelId);
				}

				contentMap.getWritableDatasource().store(Collections.singleton(changeable));
				if (trx != null) {
					trx.setSuccess();
						}
			} finally {
				contentMap.endWrite(Page.TYPE_PAGE);
			}
			return 1;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.etc.AsynchronousJob#getDescription()
		 */
		public String getDescription() {
			StringBuilder str = new StringBuilder("Write object ");
			str.append(dataMap.get("contentid"));
			if (channelId > 0) {
				str.append(" (channel ").append(channelId).append(")");
			}
			str.append(" into CR");
			return str.toString();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.etc.AsynchronousJob#isLogged()
		 */
		public boolean isLogged() {
			return true;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.etc.GroupableAsynchronousJob#getGroup()
		 */
		@SuppressWarnings("unchecked")
		public <T extends GroupableAsynchronousJob> JobGroup<T> getGroup() {
			return (JobGroup<T>) new CnMapUpdateGroup(this);
		}
	}

	/**
	 * Group implementation that will fetch all objects to be updated in a
	 * single statement before the jobs are processed and clear the cache
	 * afterwards. This minimizes the statements necessary to process the jobs
	 * (in case no object needs to be written).
	 */
	private static class CnMapUpdateGroup extends JobGroup<AsynchronousCnMapUpdate> {
		/**
		 * Contentmap
		 */
		protected ContentMap contentMap;

		/**
		 * Channel Id
		 */
		protected int channelId;

		/**
		 * True if successfully written objects shall be reported to the publish queue, false if not
		 */
		protected boolean reportToPublishQueue;

		/**
		 * Create an instance
		 * @param job job
		 */
		public CnMapUpdateGroup(AsynchronousCnMapUpdate job) {
			super(job);
			this.contentMap = job.contentMap;
			this.channelId = job.channelId;
			this.reportToPublishQueue = job.reportToPublishQueue;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.etc.AsynchronousJob#getDescription()
		 */
		public String getDescription() {
			StringBuilder str = new StringBuilder("Write ");
			str.append(jobQueue.size());
			str.append(" objects into CR");
			return str.toString();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.etc.AsynchronousJob#isLogged()
		 */
		public boolean isLogged() {
			return true;
		}

		@Override
		protected int jobLimit() {
			return jobGroupSize;
		}

		@Override
		protected void preProcess(RenderResult renderResult) throws Exception {
			ContentMapStatistics stats = contentMap.getStatistics();
			if (stats != null) {
				stats.get(Item.PREPARE_DATA).start();
			}
			try (ContentMapTrx trx = contentMap.startInstantPublishingTrx()) {
				if (renderResult != null) {
					renderResult.info(Publisher.class, "preProcess for " + jobQueue.size() + " grouped jobs");
				}
				// enable the cache
				contentMap.setCache(true);

				prepareData(contentMap,
						jobQueue.stream().map(job -> ObjectTransformer.getString(job.dataMap.get("contentid"), null)).collect(Collectors.toList()), channelId);

				if (trx != null) {
					trx.setSuccess();
				}
			} finally {
				if (stats != null) {
					stats.get(Item.PREPARE_DATA).stop();
				}
			}
		}

		@Override
		protected void postProcess(RenderResult renderResult) throws Exception {
			if (renderResult != null) {
				renderResult.info(Publisher.class, "postProcess for " + jobQueue.size() + " grouped jobs");
			}
			// disable the cache
			contentMap.setCache(false);
			MCCRHelper.resetPreparedForUpdate();
		}

		@Override
		public int process(RenderResult renderResult) throws Exception {
			Transaction t = TransactionManager.getCurrentTransaction();

			preProcess(renderResult);
			// process all grouped jobs
			int loggedJobs = 0;
			int batchSize = jobBatchSize;
			for (int start = 0; start < jobQueue.size(); start += batchSize) {
				int end = Math.min(start + batchSize, jobQueue.size());

				final List<AsynchronousCnMapUpdate> chunk = jobQueue.subList(start, end);

				// keep the current transaction alive
				if (t != null) {
					t.keepAlive();
				}
				contentMap.startWrite(Page.TYPE_PAGE);

				long startTime = System.currentTimeMillis();
				Collection<Changeable> toStore = new ArrayList<Changeable>(chunk.size());
				ContentMapStatistics cnStats = contentMap.getStatistics();
				try (ContentMapTrx trx = contentMap.startInstantPublishingTrx(new ContentMapTrxHandler() {
					@Override
					public void handle() throws NodeException {
						if (reportToPublishQueue) {
					for (AsynchronousCnMapUpdate job : chunk) {
								PublishQueue.reportPublishActionDone(Page.TYPE_PAGE, job.pageId, channelId, PublishAction.WRITE_CR);
							}
						}
					}
				})) {
					for (AsynchronousCnMapUpdate job : chunk) {
						if (cnStats != null) {
							cnStats.get(Item.PREPARE).start();
						}
						Changeable changeable = prepareChangeableForPage(contentMap, job.dataMap, channelId, job.folderId);
						if (cnStats != null) {
							cnStats.get(Item.PREPARE).stop();
							cnStats.get(Item.EXISTENCE).start();
						}
						boolean objectExists = checkObjectExistence(changeable);
						if (cnStats != null) {
							cnStats.get(Item.EXISTENCE).stop();
						}

						try {
							if (objectExists) {
								contentMap.handleUpdateObject(changeable, Collections.unmodifiableSet(job.dataMap.keySet()));
							} else {
								contentMap.handleCreateObject(changeable);
							}
						} catch (CnMapPublishException e) {
							throw new NodeException("Publish object {" + changeable + "} into the ContentRepository failed (publish handler threw exception)", e);
						}

						toStore.add(changeable);

						if (job.isLogged()) {
							loggedJobs++;
						}
					}

					contentMap.getWritableDatasource().store(toStore);

					if (trx != null) {
						trx.setSuccess();
								}
				} finally {
					contentMap.endWrite(Page.TYPE_PAGE, chunk.size());
				}

				long duration = System.currentTimeMillis() - startTime;

				if (renderResult != null) {
					renderResult.info(Publisher.class, Thread.currentThread().getName() + " processed " + chunk.size() + " jobs in " + duration + " ms");
				}
			}
			postProcess(renderResult);
			return loggedJobs;
		}

		@Override
		public boolean isGroupable(GroupableAsynchronousJob job) {
			if (!super.isGroupable(job)) {
				return false;
			} else {
				AsynchronousCnMapUpdate cnMapJob = (AsynchronousCnMapUpdate)job;
				return channelId == cnMapJob.channelId && contentMap.equals(cnMapJob.contentMap);
			}
		}
	}

	public void generateWorkPhases(IWorkPhase parent) {
		syncPhase = new CNWorkPhase(parent, "syncObjectTypes", PublishWorkPhaseConstants.PHASE_NAME_CNMAP_SYNC);
		publishPhase = new CNWorkPhase(parent, "mapPublishPhase", PublishWorkPhaseConstants.PHASE_NAME_CNMAP_DOPUBLISH);
		delPhase = new CNWorkPhase(parent, "delPhase", PublishWorkPhaseConstants.PHASE_NAME_CNMAP_DELETE_OLD);
	}
    
	public void initializeWorkPhases() throws NodeException {
		int folderCount = initializePublishObjects(Folder.TYPE_FOLDER, "folder", publishPhase);
		int fileCount = initializePublishObjects(ContentFile.TYPE_FILE, "files", publishPhase);
		if (publishInfo != null) {
			publishInfo.incFileRenderCount(fileCount);
			publishInfo.incFolderRenderCount(folderCount);
		}
	}

	/**
	 * Publish objects into the content repositories
	 * @param renderResult render result
	 * @throws NodeException
	 */
	public void publishObjects(RenderResult renderResult) throws NodeException {
		if (syncPhase != null) {
			syncPhase.begin();
		}
		RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_TYPES);
		syncObjectTypes(syncPhase, renderResult);
		RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_TYPES);
		if (syncPhase != null) {
			syncPhase.done();
		}

		if (publishPhase != null) {
			publishPhase.begin();
		}
		RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_FOLDERS);
		publishFolders(publishPhase);
		RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_FOLDERS);
        
		RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_FILES);
		publishFiles(publishPhase);
		RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_FILES);
		if (publishPhase != null) {
			publishPhase.done();
		}

		if (delPhase != null) {
			delPhase.begin();
		}
		RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_DELETED);
		syncDeletedObjects(delPhase, renderResult);
		RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_DELETED);
		if (delPhase != null) {
			delPhase.done();
		}
	}

	/**
	 * Sync the objecttypes for all configured contentrepositories
	 * @param contentMapPublishPhase 
	 * @param renderResult render result
	 * @throws NodeException
	 */
	public void syncObjectTypes(IWorkPhase syncPhase, RenderResult renderResult) throws NodeException {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean fsAttributes = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.CR_FILESYSTEM_ATTRIBUTES);

			Collection<ContentMap> contentMaps = this.nodeContentMaps.values();
			// make list unique
			Collection<ContentMap> uniqueContentMaps = new Vector<ContentMap>();

			for (ContentMap contentMap : contentMaps) {
				if (!uniqueContentMaps.contains(contentMap)) {
					uniqueContentMaps.add(contentMap);
				}
			}
			contentMaps = uniqueContentMaps;

			if (syncPhase != null) {
				syncPhase.addWork(contentMaps.size());
			}
			for (ContentMap contentMap : contentMaps) {
				WriteableDatasource wds = contentMap.getWritableDatasource();

				if (renderResult != null) {
					renderResult.info(CnMapPublisher.class, "Syncing object types for " + contentMap);
				}

				try (ContentMapTrx trx = contentMap.startInstantPublishingTrx()) {
					// get all entries from the tagmap
					TagmapResultProcessor result = new TagmapResultProcessor(contentMap);

					try {
						// we do not use connection pooling here
						DB.query(config.getSQLHandle(false).getDBHandle(),
								"SELECT * FROM tagmap WHERE contentrepository_id = ?", new Object[] { contentMap.getId()}, result);

						// TODO check for duplicates
						for (CrFragment fragment : contentMap.getContentRepository().getAssignedFragments()) {
							DB.query(config.getSQLHandle(false).getDBHandle(),
									"SELECT mapname, attribute_type attributetype, optimized, multivalue, obj_type object, target_type objtype, foreignlink_attribute foreignlinkattribute, foreignlink_attribute_rule foreignlinkattributerule, filesystem FROM cr_fragment_entry WHERE cr_fragment_id = ?",
									new Object[] { fragment.getId() }, result);
						}
					} catch (SQLException e) {
						throw new NodeException("Error while updating tagmap", e);
					}
					Collection<ObjectTypeBean> newObjectTypes = new Vector<ObjectTypeBean>();

					newObjectTypes.add(result.getPage());
					newObjectTypes.add(result.getFolder());
					newObjectTypes.add(result.getFile());

					// check whether there exist filesystem attributes, but the contentrepository does not contain the column "contentattributetype.filesystem"
					DBHandle dbHandle = contentMap.getHandle();

					try {
						if (renderResult != null && fsAttributes && !DB.fieldExists(dbHandle, dbHandle.getContentAttributeTypeName(), "filesystem")) {
							for (ObjectTypeBean oType : newObjectTypes) {
								for (ObjectAttributeBean oAttr : oType.getAttributeTypesList()) {
									if (oAttr.isFilesystem()) {
										renderResult.warn(CnMapPublisher.class,
												"Attribute " + oAttr.getName()
												+ " should be written into the filesystem, but the database structure of content repository " + contentMap
												+ " is not appropriate. Use 'Check'/'Repair' on the content repository to fix it (attribute will not be written into the filesystem).");
									}
								}
							}
						}
					} catch (SQLException e) {
						throw new NodeException("Error while checking contentrepository structure", e);
					}

					// TODO check consistency of generated objecttypes/attributetypes
    
					// get the objecttypes/attributetypes from the cr
					Collection<ObjectTypeBean> objectTypes = ObjectManagementManager.loadObjectTypes(wds, false);
    
					// calculate the diff
					TypeDiff diff = ObjectManagementManager.getDiff(objectTypes, newObjectTypes);
    
					// save all added objecttypes
					for (ObjectTypeBean newObjectType : diff.getAddedObjectTypes()) {
						ObjectManagementManager.save(wds, newObjectType, true, true, false);
					}
    
					// remove all deleted objecttypes
					for (ObjectTypeBean deletedObjectType : diff.getDeletedObjectTypes()) {
						ObjectManagementManager.delete(wds, deletedObjectType, true);
					}
    
					// save all modified objecttypes
					for (ObjectTypeDiff typeDiff : diff.getModifiedObjectTypes()) {
						if (wds instanceof CNDatasource) {
							// remove all deleted attributetypes
							for (ObjectAttributeBean deletedAttribute : typeDiff.getDeletedAttributeTypes()) {
								ObjectManagementManager.deleteAttributeType(((CNDatasource) wds).getHandle().getDBHandle(), deletedAttribute, true);
							}
						}
						// and save the modified objecttype (adds and saves modified
						// attributes)
						ObjectManagementManager.save(wds, typeDiff.getModifiedObjectType(), true, true, false);
					}

					// remove unused attribute types
					if (contentMap.isMultichannelling()) {
						ObjectManagementManager.cleanUnusedAttributeTypes(contentMap.getMCCRDatasource());
					}

					if (syncPhase != null) {
						syncPhase.doneWork();
					}

					if (trx != null) {
						trx.setSuccess();
						}
					}

				if (renderResult != null) {
					renderResult.info(CnMapPublisher.class, "Synced object types for " + contentMap);
				}
			}
		} catch (ObjectManagementException e) {
			throw new NodeException("Error while syncing objecttypes", e);
		}
	}

	protected void publishFolders(IWorkPhase publishPhase) throws NodeException {
		publishObjects(Folder.TYPE_FOLDER, "folders", publishPhase);
	}

	protected void publishFiles(IWorkPhase publishPhase) throws NodeException {
		publishObjects(ContentFile.TYPE_FILE, "files", publishPhase);
	}

	/**
	 * Initialize publishing the modified objects of the specified type into
	 * their respective node. As a result, one of the lists
	 * {@link #publishFileObjectTaskQueue} or
	 * {@link #publishFolderObjectTaskQueue} will be filled with instances of
	 * the class {@link PublishObjectTask}.
	 * @param objType object type of the objects to initialize
	 * @param objTypeName descriptive name of the object type (will be used for
	 *        logging)
	 * @param publishPhase publish phase. For every object that must be
	 *        published, a work unit is added to this phase
	 * @return total number of objects to publish
	 * @throws NodeException
	 */
	protected int initializePublishObjects(int objType, String objTypeName, IWorkPhase publishPhase) throws NodeException {
		NodePreferences prefs = TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences();
		List<PublishObjectTask> publishObjectTaskQueue = new ArrayList<PublishObjectTask>();

		// loop over all maps
		Collection<ContentMap> contentMaps = usedContentMaps;
		List<Node> publishedNodes = Publisher.getPublishedNodes();

		int totalCount = 0;

		for (ContentMap contentMap : contentMaps) {
			// loop over all nodes
			for (Node node : publishedNodes) {
				// check whether the node publishes into this contentmap
				if (!contentMap.equals(nodeContentMaps.get(node))) {
					continue;
				}

				// Don't publish this type of objects,
				// if the type is disabled for this node
				switch (objType) {
				case Folder.TYPE_FOLDER:
					if (!node.doPublishContentMapFolders()) {
						continue;
					}
					break;
				case ContentFile.TYPE_FILE:
					if (!node.doPublishContentMapFiles()) {
						continue;
					}
					break;
				}

				// get the timestamp of the last publish process of this node
				// into this contentmap
				int lastNodePublish;

				// Make sure the contentmap is accessed in a transaction. When instant publishing is not enabled,
				// startInstantPublishingTrx() will do nothing, but the transaction was already started in
				// initContentMaps().
				try (ContentMapTrx trx = contentMap.startInstantPublishingTrx()) {
					lastNodePublish = Math.min(node.getLastPublishTimestamp(), contentMap.getLastMapUpdate(node));

					if (trx != null) {
						trx.setSuccess();
					}
				}

				if (logger.isInfoEnabled()) {
					logger.info(node + " was last published into contentmap @ " + lastNodePublish);
				}

				// get all objects that were modified since the last publish (or
				// get all objects)
				List<NodeObjectWithAttributes<? extends NodeObject>> modifiedObjects = null;

				switch (objType) {
				case Folder.TYPE_FOLDER:
					if (prefs.isFeature(Feature.ATTRIBUTE_DIRTING)) {
						modifiedObjects = new ArrayList<>(PublishQueue.getDirtedObjectsWithAttributes(Folder.class, node));
					} else {
						modifiedObjects = new ArrayList<>(PublishQueue.getDirtedObjects(Folder.class, node));
					}
					break;

				case ContentFile.TYPE_FILE:
					List<NodeObjectWithAttributes<com.gentics.contentnode.object.File>> files = null;
					if (prefs.isFeature(Feature.ATTRIBUTE_DIRTING)) {
						files = PublishQueue.getDirtedObjectsWithAttributes(com.gentics.contentnode.object.File.class, node);
					} else {
						files = PublishQueue.getDirtedObjects(com.gentics.contentnode.object.File.class, node);
					}

					// if files may be offline, we need to check for that
					FileListForNode fileList = FileOnlineStatus.prepareForNode(node);

					for (Iterator<NodeObjectWithAttributes<com.gentics.contentnode.object.File>> iter = files.iterator(); iter.hasNext();) {
						NodeObjectWithAttributes<com.gentics.contentnode.object.File> f = iter.next();

						if (!fileList.isOnline(f.object)) {
							iter.remove();
						}
					}

					modifiedObjects = new ArrayList<>(files);
					break;

				default:
					// TODO exit here
					break;
				}

				publishObjectTaskQueue.add(new PublishObjectTask(contentMap, node, modifiedObjects, lastNodePublish));
				if (publishPhase != null) {
					publishPhase.addWork(modifiedObjects.size());
				}

				totalCount += modifiedObjects.size();

				if (logger.isInfoEnabled()) {
					logger.info("found " + modifiedObjects.size() + " " + objTypeName + " in " + node + " to publish into {" + contentMap + "}");
				}
			}
		}
		if (objType == Folder.TYPE_FOLDER) {
			publishFolderObjectTaskQueue = publishObjectTaskQueue;
		} else if (objType == ContentFile.TYPE_FILE) {
			publishFileObjectTaskQueue = publishObjectTaskQueue;
		}

		return totalCount;
	}
    
	/**
	 * Clear the modified objects in all task queues (so that they can be filled again)
	 */
	public void resetTaskQueues() {
		resetTaskQueue(publishFolderObjectTaskQueue);
		resetTaskQueue(publishFileObjectTaskQueue);
	}

	/**
	 * Clear the modified objects in the given task queue
	 * @param taskQueue task queue
	 */
	protected void resetTaskQueue(List<PublishObjectTask> taskQueue) {
		for (PublishObjectTask task : taskQueue) {
			task.modifiedObjects.clear();
		}
	}

	/**
	 * Add the given file to be published into the given node
	 * @param file file
	 * @param node node
	 * @throws NodeException when no publish task for the node is found
	 */
	public void addPublishTask(com.gentics.contentnode.object.File file, Node node) throws NodeException {
		for (PublishObjectTask task : publishFileObjectTaskQueue) {
			if (task.node.equals(node)) {
				task.modifiedObjects.add(new PublishQueue.NodeObjectWithAttributes<NodeObject>(file));
				return;
			}
		}

		throw new NodeException(
				"Error while adding " + file + " to publish queue of " + node + ": Not publishing into contentrepository of " + node + " in this publish run");
	}

	/**
	 * A class which stores all information which is need to publish objects into the content repository.
	 * 
	 * This is needed so we can correctly initialize the publisher and store this tasks until they are
	 * processed.
	 * 
	 * @author herbert
	 */
	public static class PublishObjectTask {

		/**
		 * Node for which the objects are published
		 */
		public Node node;

		/**
		 * List of modified objects
		 */
		public List<PublishQueue.NodeObjectWithAttributes<? extends NodeObject>> modifiedObjects;

		/**
		 * Contentmap instance into which is published
		 */
		public ContentMap contentMap;

		/**
		 * Last publish timestamp into the content repository
		 */
		public int lastNodePublish;

		/**
		 * Create an instance of the publish object task
		 * @param contentMap contentmap into which is published
		 * @param node node for which the objects will be published
		 * @param modifiedObjects list of objects to publish
		 * @param lastNodePublish timestamp of the last publish run into the content repository
		 */
		public PublishObjectTask(ContentMap contentMap, Node node, List<NodeObjectWithAttributes<? extends NodeObject>> modifiedObjects, int lastNodePublish) {
			this.contentMap = contentMap;
			this.node = node;
			this.modifiedObjects = new ArrayList<NodeObjectWithAttributes<? extends NodeObject>>(modifiedObjects);
			this.lastNodePublish = lastNodePublish;
		}
	}

	protected void publishObjects(final int objType, String objTypeName, IWorkPhase publishPhase) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderResult renderResult = t.getRenderResult();
		final boolean reportToPublishQueue = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.RESUMABLE_PUBLISH_PROCESS);
		List<PublishObjectTask> publishObjectTaskQueue = null;

		if (objType == Folder.TYPE_FOLDER) {
			publishObjectTaskQueue = publishFolderObjectTaskQueue;
		} else if (objType == ContentFile.TYPE_FILE) {
			publishObjectTaskQueue = publishFileObjectTaskQueue;
		}
		if (publishObjectTaskQueue == null) {
			throw new NodeException(
					"Error while publishing objects into content repository - unable to retrieve objects to be published (failed to initialize ?)");
		}

		for (PublishObjectTask pubObjTask : publishObjectTaskQueue) {
			List<NodeObjectWithAttributes<? extends NodeObject>> modifiedObjects = pubObjTask.modifiedObjects;
			Node node = pubObjTask.node;
			ContentMap contentMap = pubObjTask.contentMap;
			ContentMapStatistics stats = contentMap.getStatistics();

			final int nodeId = ObjectTransformer.getInt(node.getId(), 0);

			if (logger.isInfoEnabled()) {
				logger.info("found " + modifiedObjects.size() + " " + objTypeName + " in " + node + " to publish into {" + contentMap + "}");
			}

			// if multichannelling is used, we set the node as channel into the rendertype
			if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
				t.setChannelId(node.getId());
			} else {
				t.setChannelId(null);
			}

			// set the channel in the contentmap
			if (contentMap.isMultichannelling()) {
				contentMap.getMCCRDatasource().setChannel(nodeId);
			}

			try (PublishedNodeTrx pnTrx = new PublishedNodeTrx(node)) {
				// loop over folders and publish into contentmap
				int batchSize = jobBatchSize;
				for (int start = 0; start < modifiedObjects.size(); start += batchSize) {
					int end = Math.min(start + batchSize, modifiedObjects.size());
					final List<NodeObjectWithAttributes<? extends NodeObject>> chunk = new ArrayList<>(modifiedObjects.subList(start, end));
					final List<Integer> chunkObjectIds = new ArrayList<>(chunk.size());

					keepContentmapsAlive();
					t.keepAlive();

					// if instant_publish is turned on for the contentmap, start a new transaction here
					try (ContentMapTrx trx = contentMap.startInstantPublishingTrx(new ContentMapTrxHandler() {
						@Override
						public void handle() throws NodeException {
							if (reportToPublishQueue) {
								for (Integer objId : chunkObjectIds) {
									PublishQueue.reportPublishActionDone(objType, objId, nodeId, PublishAction.WRITE_CR);
								}
							}
						}
					})) {
						Iterator<NodeObjectWithAttributes<? extends NodeObject>> it = chunk.iterator();

						while (it.hasNext()) {
							NodeObjectWithAttributes<? extends NodeObject> object = it.next();

							if (PublishController.wasInstantPublished(object.object)) {
								PublishQueue.requeueObject(object.object, node);
								renderResult.info(Publisher.class, "Omit " + object + ", because it was already instant published");
								it.remove();
							}
						}

						for (NodeObjectWithAttributes<? extends NodeObject> object : chunk) {
							chunkObjectIds.add(object.object.getId());
						}

						if (contentMap.isInstantPublishing()) {
							if (reportToPublishQueue) {
								for (Integer objId : chunkObjectIds) {
									PublishQueue.initiatePublishAction(objType, objId, nodeId, PublishAction.WRITE_CR);
								}
							}
						}

						Collection<Changeable> toStore = new ArrayList<Changeable>(chunk.size());

						// prepare existing MCCR data
						if (contentMap.isMultichannelling()) {
							if (stats != null) {
								stats.get(Item.PREPARE_DATA).start();
							}
							prepareData(contentMap, chunk.stream().map(object -> {
								int ttype = object.object.getTType();
								if (ttype == ImageFile.TYPE_IMAGE) {
									ttype = ContentFile.TYPE_FILE;
								}
								return ttype + "." + object.object.getId();
							}).collect(Collectors.toList()), nodeId);
							if (stats != null) {
								stats.get(Item.PREPARE_DATA).stop(chunk.size());
							}
						}

						if (stats != null) {
							stats.get(Item.PREPARE).start();
						}
						for (NodeObjectWithAttributes<? extends NodeObject> object : chunk) {
							toStore.add(prepareObjectForWriting(object.object, object.attributes, contentMap, dbfilesDir, true, true).changeable);
						}
						if (stats != null) {
							stats.get(Item.PREPARE).stop(chunk.size());
						}

						contentMap.startWrite(objType);
						// now store all objects
						contentMap.getWritableDatasource().store(toStore);

						for (NodeObjectWithAttributes<? extends NodeObject> object : chunk) {
							renderResult.info(CnMapPublisher.class, "written " + object + " into {" + contentMap + "} for " + node);
							if (!ObjectTransformer.isEmpty(object.attributes)) {
								renderResult.debug(CnMapPublisher.class, "Attributes for " + object + ": " + object.attributes);
							}
						}
						contentMap.setChanged(true);

						if (trx != null) {
							trx.setSuccess();
						}
						contentMap.endWrite(objType, chunk.size());
					} finally {
						if (contentMap.isMultichannelling()) {
							MCCRHelper.resetPreparedForUpdate();
						}
						if (publishPhase != null) {
							publishPhase.doneWork(chunk.size());
						}
						if (objType == Folder.TYPE_FOLDER) {
							for (int i = 0; i < chunk.size(); i++) {
								MBeanRegistry.getPublisherInfo().publishedFolder(nodeId);
								if (publishInfo != null) {
									publishInfo.folderRendered();
								}
							}
						} else if (objType == ContentFile.TYPE_FILE) {
							for (int i = 0; i < chunk.size(); i++) {
								MBeanRegistry.getPublisherInfo().publishedFile(nodeId);
								if (publishInfo != null) {
									publishInfo.fileRendered();
								}
							}
						}
					}
				}
			} finally {
				// reset the channel after publishing the objects
				t.resetChannel();
			}
		}

		// Clear reference so we don't hold a reference to all objects the whole publish run..
		resetTaskQueue(publishObjectTaskQueue);
	}

	/**
	 * Check whether the tagmap entry is for the page content
	 * @param entry tagmap entry
	 * @return true if the entry is the page content
	 */
	public static boolean isPageContent(TagmapEntryRenderer entry) {
		return entry.getObjectType() == Page.TYPE_PAGE && "content".equals(entry.getMapname()) && ObjectTransformer.isEmpty(entry.getTagname());
	}

	public static Object resolveTagmapEntry(RenderType renderType, RenderResult renderResult,
			TagmapEntryRenderer entry) throws NodeException {
		// special behaviour for page content
		if (isPageContent(entry)) {
			return renderType.getRenderedRootObject();
		}

		Object evaluatedValue = null;
		Object resolvedValue = null;
		Object value = null;
		boolean useExpressionParser = renderType.getPreferences().getFeature("tagmap_useexpressionparser");
		boolean debugExpressionParser = renderType.getPreferences().getFeature("tagmap_useexpressionparser_debug");

		if (useExpressionParser) {
			// Evaluate our expression ..
			try {
				Expression tagnameExpression = ExpressionParser.getInstance().parse(entry.getTagname());

				evaluatedValue = ((EvaluableExpression) tagnameExpression).evaluate(
						new ExpressionQueryRequest(new PropertyStackResolver(renderType.getStack()), null), ExpressionEvaluator.OBJECTTYPE_ANY);
			} catch (ExpressionParserException e) {
				renderResult.error(CnMapPublisher.class, "Error while evaluating expression for tagmap {" + entry.getTagname() + "}", e);
			} catch (Throwable e) {
				throw new NodeException("Error while evaluating expression for tagmap {" + entry.getTagname() + "}", e);
			}
			value = evaluatedValue;
		}
		if (debugExpressionParser || !useExpressionParser) {
			// Resolve tagname ..
			resolvedValue = renderType.getStack().resolve(entry.getTagname());
            
			if (!useExpressionParser) {
				// If we are only resolving, this is our value
				value = resolvedValue;
			}
			if (debugExpressionParser) {
				// If debug is enabled, output values and compare them.
                
				String debug = "mapname: {" + entry.getMapname() + "} tagname: {" + entry.getTagname() + "} Evaluated: {" + String.valueOf(evaluatedValue) + "} ("
						+ (evaluatedValue == null ? "null" : evaluatedValue.getClass().getName()) + ") Resolved: {" + String.valueOf(resolvedValue) + "} ("
						+ (resolvedValue == null ? "null" : resolvedValue.getClass().getName()) + ")";

				if (evaluatedValue != resolvedValue) {
					if (evaluatedValue == null || !evaluatedValue.equals(resolvedValue)) {
						renderResult.warn(CnMapPublisher.class, "tagmap expression differs between evaluated expression and resolved tagname: " + debug);
					}
				}
				renderResult.debug(CnMapPublisher.class, "tagmap expression debug: " + debug);
			}
		}
		return value;
	}

	/**
	 * Synchronize the deleted objects for the contentrepositories
	 * @param delPhase 
	 * @param renderResult render result
	 * @throws NodeException
	 */
	protected void syncDeletedObjects(IWorkPhase delPhase, RenderResult renderResult) throws NodeException {
		Collection<ContentMap> contentMaps = usedContentMaps;

		if (delPhase != null) {
			delPhase.addWork(contentMaps.size());
		}
		for (ContentMap contentMap : contentMaps) {
			syncDeletedObjects(contentMap, renderResult);

			// remove the objects from nodes that are not published into the CR at all
			deleteObjectsNotPublishedIntoCR(contentMap, renderResult);
			if (delPhase != null) {
				delPhase.doneWork();
			}
		}
	}

	/**
	 * Delete objects that are not (no longer) published into the given contentmap
	 * @param contentMap contentmap
	 * @param renderResult render result
	 * @throws NodeException
	 */
	private void deleteObjectsNotPublishedIntoCR(final ContentMap contentMap, RenderResult renderResult) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		CNWriteableDatasource ds = contentMap.getDatasource();

		if (ds == null) {
			return;
		}

		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Start to remove detached nodes from {" + contentMap + "}");
		}

		// get all nodes that publish into this CR (also get the nodes, that have disable publish currently set)
		final List<Integer> nodeIds = new ArrayList<Integer>();

		DBUtils.executeStatement("SELECT id FROM node WHERE publish_contentmap = ? AND contentrepository_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, 1); // publish_contentmap = ?
				stmt.setInt(2, contentMap.getId()); // contentrepository_id = ?
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					nodeIds.add(rs.getInt("id"));
				}
			}
		});

		// check all types
		List<Integer> types = Arrays.asList(Page.TYPE_PAGE, ContentFile.TYPE_FILE, Folder.TYPE_FOLDER);
		Map<Integer, String> typeDescriptions = new HashMap<Integer, String>();

		typeDescriptions.put(Page.TYPE_PAGE, "page(s)");
		typeDescriptions.put(ContentFile.TYPE_FILE, "file(s)");
		typeDescriptions.put(Folder.TYPE_FOLDER, "folder(s)");

		StringBuffer expression = new StringBuffer("object.obj_type == data.type");

		for (Integer nodeId : nodeIds) {
			Node node = t.getObject(Node.class, nodeId);
			if (node == null) {
				continue;
			}

			// Also delete objects of object types that are not
			// enabled to be published into the CR
			boolean publishFolders = node.doPublishContentMapFolders();
			boolean publishPages   = node.doPublishContentMapPages();
			boolean publishFiles   = node.doPublishContentMapFiles();

			if (!publishFolders || !publishPages || !publishFiles) {
				List<String> typesToDelete = new ArrayList<String>();

				expression.append(" AND (object.node_id != ").append(nodeId);

				if (!publishFolders) {
					typesToDelete.add("object.obj_type == " + Folder.TYPE_FOLDER);
				}

				if (!publishPages) {
					typesToDelete.add("object.obj_type == " + Page.TYPE_PAGE);
				}

				if (!publishFiles) {
					typesToDelete.add("object.obj_type == " + ContentFile.TYPE_FILE);
				}

				expression.append(" OR (object.node_id == ").append(nodeId)
					.append(" AND (")
					.append(StringUtils.join(typesToDelete, " OR "))
					.append(")))");
			} else {
				expression.append(" AND object.node_id != ").append(nodeId);
			}
		}

		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(expression.toString()));
		Map<String, Object> dataMap = new HashMap<String, Object>();

		filter.addBaseResolvable("data", new MapResolver(dataMap));
		for (Integer type : types) {
			int count = 0;

			dataMap.clear();
			dataMap.put("type", type);
			dataMap.put("nodeIds", nodeIds);
			Collection<Resolvable> objects = null;

			do {
				objects = ds.getResult(filter, null, 0, 1000, null);
				count += objects.size();
				internalDeleteObjects(contentMap, objects);
			} while (!objects.isEmpty());

			if (renderResult != null) {
				renderResult.info(CnMapPublisher.class, "Removed " + count + " " + typeDescriptions.get(type) + " for detached nodes from {" + contentMap + "}");
			}
		}
    
		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Removed detached nodes from {" + contentMap + "}");
		}
	}

	/**
	 * Synchronize the deleted objects for the given (non multichannelling) datasource
	 * @param contentMap contentmap
	 * @param ds datasource
	 * @param timestamp timestamp of the last mapupdate
	 * @throws NodeException
	 */
	protected void syncDeletedObjectsDifferential(ContentMap contentMap, CNWriteableDatasource ds, int timestamp, RenderResult renderResult) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Resolvable> deletedObjects = new Vector<Resolvable>();
		Map<String, String> dataMap = new HashMap<String, String>(1);

		int[] types = { Page.TYPE_PAGE, ContentFile.TYPE_FILE, Folder.TYPE_FOLDER};

		for (final Node node : contentMap.getNodes()) {
			for (int type : types) {
				Class<? extends NodeObject> objectClass = t.getClass(type);
				List<Integer> removedObjectIds = null;

				if ((type == Folder.TYPE_FOLDER && node.doPublishContentMapFolders())
						|| (type == Page.TYPE_PAGE && node.doPublishContentMapPages())
						|| (type == ContentFile.TYPE_FILE && node.doPublishContentMapFiles())) {
					// If publishing is enabled for this type, only get the really removed objects
					removedObjectIds = PublishQueue.getRemovedObjectIds(objectClass, true, node);
				} else {
					// Get all objects of this type for this node otherwise
					Map<String, Object> filterDataMap = new HashMap<String, Object>(2);
					DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(
							"object.obj_type == data.obj_type AND object.node_id == data.node_id"));
					filter.addBaseResolvable("data", new MapResolver(filterDataMap));
					filterDataMap.put("obj_type", type);
					filterDataMap.put("node_id", node.getId());
					Collection<Resolvable> objects = ds.getResult(filter, null);

					removedObjectIds = new ArrayList<Integer>();
					for (Resolvable dataSourceObject : objects) {
						removedObjectIds.add(ObjectTransformer.getInteger(dataSourceObject.get("obj_id"), 0));
					}
				}

				for (Integer removedId : removedObjectIds) {
					// if the object still shall be published into this contentrepository, it was probably moved from one node
					// to another and both nodes publish into the same CR
					if (contentMap.isPublishedIntoCR(t.getObject(objectClass, removedId))) {
						continue;
					}

					dataMap.clear();
					String contentid = type + "." + removedId;
					dataMap.put("contentid", contentid);
					Changeable object = ds.create(dataMap, -1, false);
					deletedObjects.add(object);
				}
			}
		}

		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Start to remove "+ deletedObjects.size() +" deleted objects from {" +
					contentMap + ", Instant publishing: " + contentMap.isInstantPublishing() + "}.");
		}

		internalDeleteObjects(contentMap, deletedObjects);
		for (Resolvable del : deletedObjects) {
			if (renderResult != null) {
				renderResult.info(CnMapPublisher.class, "Removed " + del.get("contentid"));
			}
		}
		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Removed " + deletedObjects.size() + " objects from {" + contentMap + "}");
		}
	}

	/**
	 * Synchronize the deleted objects for the given (non multichannelling) datasource by doing a full compare
	 * @param contentMap contentmap
	 * @param ds datasource
	 * @param renderResult render result
	 * @throws NodeException
	 */
	protected void syncDeletedObjectsFull(ContentMap contentMap, CNWriteableDatasource ds, RenderResult renderResult) throws NodeException {
		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Start to remove deleted objects from {" + contentMap + "}");
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Resolvable> deletedObjects = new Vector<Resolvable>();

		// get all folders from the contentrepository
		DatasourceFilter folderFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10002"));
		Collection<Resolvable> folders = ds.getResult(folderFilter, null);

		for (Resolvable folder : folders) {
			if (!contentMap.isPublishedIntoCR(t.getObject(Folder.class, (Integer)folder.get("obj_id"), -1))) {
				deletedObjects.add(folder);
			}
		}

		// get all pages from the contentrepository
		DatasourceFilter pageFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10007"));
		Collection<Resolvable> pages = ds.getResult(pageFilter, null);

		for (Resolvable page : pages) {
			if (!contentMap.isPublishedIntoCR(t.getObject(Page.class, (Integer)page.get("obj_id"), -1))) {
				deletedObjects.add(page);
			}
		}

		// get all files from the contentrepository
		DatasourceFilter fileFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10008"));
		Collection<Resolvable> files = ds.getResult(fileFilter, null);

		for (Resolvable file : files) {
			if (!contentMap.isPublishedIntoCR(t.getObject(com.gentics.contentnode.object.File.class, (Integer)file.get("obj_id"), -1))) {
				deletedObjects.add(file);
			}
		}

		internalDeleteObjects(contentMap, deletedObjects);
		for (Resolvable del : deletedObjects) {
			if (renderResult != null) {
				renderResult.info(CnMapPublisher.class, "Removed " + del.get("contentid"));
			}
		}
		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Removed " + deletedObjects.size() + " objects from {" + contentMap + "}");
		}
	}

	/**
	 * Synchronize the deleted objects for the multichannelling datasource and the given node differentially
	 * @param contentMap contentmap
	 * @param node node
	 * @param renderResult render result
	 * @throws NodeException
	 */
	protected void syncDeletedObjectsDifferential(ContentMap contentMap, Node node, RenderResult renderResult) throws NodeException {
		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Start to remove deleted objects of " + node + " from {" + contentMap + "} differentially");
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Resolvable> deletedObjects = new Vector<Resolvable>();
		WritableMCCRDatasource ds = contentMap.getMCCRDatasource();
		Map<String, Object> dataMap = new HashMap<String, Object>();

		// set the current channel
		t.setChannelId(node.getId());
		ds.setChannel(ObjectTransformer.getInt(node.getId(), 0));
		try {
			Collection<Class<? extends NodeObject>> classes = new ArrayList<Class<? extends NodeObject>>(3);
			classes.add(Page.class);
			classes.add(Folder.class);
			classes.add(com.gentics.contentnode.object.File.class);

			for (Class<? extends NodeObject> clazz : classes) {
				int objType = t.getTType(clazz);

				dataMap.clear();
				StringBuffer expression = new StringBuffer("object.obj_type == data.obj_type"
						+ " AND object.channel_id == data.channel_id");
				dataMap.put("obj_type", objType);
				dataMap.put("channel_id", node.getId());

				// If publishing of this type is enabled for this node,
				// we only want to delete removed objects for this type, not all.
				// Remove all objects otherwise.
				if ((objType == Folder.TYPE_FOLDER && node.doPublishContentMapFolders())
						|| (objType == Page.TYPE_PAGE && node.doPublishContentMapPages())
						|| (objType == ContentFile.TYPE_FILE && node.doPublishContentMapFiles())) {
					expression.append(" AND object.obj_id CONTAINSONEOF data.obj_id");
					dataMap.put("obj_id", PublishQueue.getRemovedObjectIds(clazz, true, node));
				}

				DatasourceFilter filter = ds.createDatasourceFilter(
						ExpressionParser.getInstance().parse(expression.toString()));
				filter.addBaseResolvable("data", new MapResolver(dataMap));
				deletedObjects.addAll(ds.getResult(filter, null));
			}
		} finally {
			t.resetChannel();
		}
		internalDeleteObjects(contentMap, deletedObjects);
		for (Resolvable del : deletedObjects) {
			if (renderResult != null) {
				renderResult.info(CnMapPublisher.class, "Removed " + del.get("contentid"));
			}
		}
		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Removed " + deletedObjects.size() + " objects of " + node + " from {" + contentMap + "}");
		}
	}

	/**
	 * Synchronize the deleted objects for the multichannelling datasource and the given node
	 * @param contentMap contentmap
	 * @param node node
	 * @param renderResult render result
	 * @throws NodeException
	 */
	protected void syncDeletedObjects(ContentMap contentMap, Node node, RenderResult renderResult) throws NodeException {
		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Start to remove deleted objects of " + node + " from {" + contentMap + "}");
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Resolvable> deletedObjects = new Vector<Resolvable>();
		WritableMCCRDatasource ds = contentMap.getMCCRDatasource();

		// set the current channel
		t.setChannelId(node.getId());
		ds.setChannel(ObjectTransformer.getInt(node.getId(), 0));
		Map<String, Object> dataMap = new HashMap<String, Object>(1);
		dataMap.put("channel_id", node.getId());
		MapResolver dataResolver = new MapResolver(dataMap);
		try {
			// get all folders from the contentrepository
			DatasourceFilter folderFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10002 AND object.channel_id == data.channel_id"));
			folderFilter.addBaseResolvable("data", dataResolver);
			Collection<Resolvable> folders = ds.getResult(folderFilter, null);

			for (Resolvable folder : folders) {
				if (!contentMap.isPublishedIntoCR(t.getObject(Folder.class, ObjectTransformer.getInteger(folder.get("obj_id"), null)))) {
					deletedObjects.add(folder);
				}
			}

			// get all pages from the contentrepository
			DatasourceFilter pageFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10007 AND object.channel_id == data.channel_id"));
			pageFilter.addBaseResolvable("data", dataResolver);
			Collection<Resolvable> pages = ds.getResult(pageFilter, null);

			try (PublishCacheTrx pcTrx = new PublishCacheTrx(false)) {
				for (Resolvable page : pages) {
					if (!contentMap.isPublishedIntoCR(t.getObject(Page.class, ObjectTransformer.getInteger(page.get("obj_id"), null)))) {
						deletedObjects.add(page);
					}
				}
			}

			// get all files from the contentrepository
			DatasourceFilter fileFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10008 AND object.channel_id == data.channel_id"));
			fileFilter.addBaseResolvable("data", dataResolver);
			Collection<Resolvable> files = ds.getResult(fileFilter, null);

			for (Resolvable file : files) {
				if (!contentMap.isPublishedIntoCR(t.getObject(com.gentics.contentnode.object.File.class, ObjectTransformer.getInteger(file.get("obj_id"), null)), node)) {
					deletedObjects.add(file);
				}
			}
		} finally {
			t.resetChannel();
		}
		internalDeleteObjects(contentMap, deletedObjects);
		for (Resolvable del : deletedObjects) {
			if (renderResult != null) {
				renderResult.info(CnMapPublisher.class, "Removed " + del.get("contentid"));
			}
		}
		if (renderResult != null) {
			renderResult.info(CnMapPublisher.class, "Removed " + deletedObjects.size() + " objects of " + node + " from {" + contentMap + "}");
		}
	}

	/**
	 * Synchronize the deleted objects for the given contentmap
	 * @param contentMap contentmap
	 * @param renderResult render result
	 * @throws NodeException
	 */
	protected void syncDeletedObjects(ContentMap contentMap, RenderResult renderResult) throws NodeException {
		if (contentMap.isMultichannelling()) {
			List<Node> nodes = contentMap.getNodes();

			if (contentMap.isDifferentialDelete()) {
				for (Node node : nodes) {
					syncDeletedObjectsDifferential(contentMap, node, renderResult);
				}
			} else {
				for (Node node : nodes) {
					syncDeletedObjects(contentMap, node, renderResult);
				}
			}
		} else {
			if (contentMap.isDifferentialDelete()) {
				syncDeletedObjectsDifferential(contentMap, contentMap.getDatasource(), contentMap.getLastMapUpdate(null), renderResult);
			} else {
				syncDeletedObjectsFull(contentMap, contentMap.getDatasource(), renderResult);
			}
		}
	}

	/**
	 * Delete the given list of objects from the contentrepository. Calling this method will also handle the deletion in instances of {@link CnMapPublishHandler}.
	 * 
	 * @param contentMap contentmap
	 * @param objectsToDelete list of objects to delete
	 * @param node node
	 * @throws NodeException
	 */
	public void deleteObjects(ContentMap contentMap, List<NodeObject> objectsToDelete, Node node) throws NodeException {
		if (ObjectTransformer.isEmpty(objectsToDelete)) {
			return;
		}
		WriteableDatasource ds = contentMap.getWritableDatasource();
		boolean multichannellingDs = contentMap.isMultichannelling();

		List<Resolvable> changeables = new Vector<Resolvable>(objectsToDelete.size());

		Map<String, Object> dataMap = new HashMap<String, Object>(multichannellingDs ? 3 :1);

		try (HandleDependenciesTrx depsTrx = new HandleDependenciesTrx(false)) {
		for (NodeObject nodeObject : objectsToDelete) {
			int type = ObjectTransformer.getInt(nodeObject.getTType(), -1);

			if (type == ImageFile.TYPE_IMAGE) {
				type = ImageFile.TYPE_FILE;
			}
			if (type == -1) {
				continue;
			}
			String contentId = type + "." + nodeObject.getId();

			dataMap.clear();
			dataMap.put("contentid", contentId);
			if (multichannellingDs && nodeObject instanceof LocalizableNodeObject<?>) {
				@SuppressWarnings("unchecked")
					LocalizableNodeObject<NodeObject> locObject = (LocalizableNodeObject<NodeObject>) nodeObject;
				dataMap.put(WritableMCCRDatasource.MCCR_CHANNEL_ID, node.getId());
				dataMap.put(WritableMCCRDatasource.MCCR_CHANNELSET_ID, locObject.getChannelSetId());
			}

			changeables.add(ds.create(dataMap));
		}
		}

		internalDeleteObjects(contentMap, changeables);
	}

	/**
	 * Internal method to delete a list of changeables
	 * @param contentMap contentmap
	 * @param objectsToDelete list of changeables
	 * @throws NodeException
	 */
	protected void internalDeleteObjects(ContentMap contentMap, Collection<Resolvable> objectsToDelete) throws NodeException {
		WriteableDatasource ds = contentMap.getWritableDatasource();

		// when the contentmap has instant publishing, we need to do each object separately
		if (contentMap.isInstantPublishing()) {
			for (Resolvable objectToDelete : objectsToDelete) {
				try (ContentMapTrx trx = contentMap.startInstantPublishingTrx()) {
					contentMap.handleDeleteObject(objectToDelete);
					ds.delete(Collections.singleton(objectToDelete));

					// tell the contentmap that it was changed
					contentMap.setChanged(true);

					// to account for long running deletions of many objects,
					// check in a regular interval if contentmap dbs have to be kept alive
					keepContentmapsAlive();

					// commit the transaction (but don't close connection)
					if (trx != null) {
						trx.setSuccess();
					}
				} catch (Exception e) {
					throw new NodeException("Error while removing object " + objectToDelete + " from " + contentMap, e);
				}
			}
		} else {
			// let the event handler handle the delete events
			for (Resolvable objectToDelete : objectsToDelete) {
				try {
					contentMap.handleDeleteObject(objectToDelete);

					// to account for long running deletions of many objects,
					// check in a regular interval if contentmap dbs have to be kept alive
					keepContentmapsAlive();

				} catch (CnMapPublishException e) {
					throw new NodeException("Delete object {" + objectToDelete + "} from the ContentRepository failed (publish handler threw exception)", e);
				}
			}

			if (!objectsToDelete.isEmpty()) {
				// delete the objects
				ds.delete(objectsToDelete);

				// tell the contentmap that it was changed
				contentMap.setChanged(true);
			}
		}
	}

	/**
	 * Set the timestamp of the last mapupdate to all contentrepositories that were used 
	 * @throws NodeException
	 */
	public void setLastMapUpdate() throws NodeException {
		RenderResult renderResult = TransactionManager.getCurrentTransaction().getRenderResult();

		Collection<ContentMap> contentMaps = usedContentMaps;

		logger.debug("Setting last map update .. for contentmaps: {" + contentMaps + "}");
		for (ContentMap contentMap : contentMaps) {
			try {
				logger.debug("contentMap.setLastMapUpdate ... " + contentMap + " - " + contentMap.isChanged());
				// probably wait on the worker to finish
				AsynchronousWorker worker = contentMapWorkers.get(contentMap.getId());

				if (worker != null) {
					worker.flush();
					worker.stop();
					worker.join();
					worker.throwExceptionOnFailure();
				}
				// We use wallclock time so that instant publishing doesn't break
				// cache consistency during a publish run
				int timestamp = (int) (System.currentTimeMillis() / 1000);

				// Make sure the contentmap is accessed in a transaction. When instant publishing is not enabled,
				// startInstantPublishingTrx() will do nothing, but the transaction was already started in
				// initContentMaps().
				try (ContentMapTrx trx = contentMap.startInstantPublishingTrx()) {
				contentMap.setLastMapUpdate(timestamp, Publisher.getPublishedNodes(), true);

					if (trx != null) {
						trx.setSuccess();
					}
				}
			} catch (SQLException e) {
				logger.error("Error while setting last mapupdate for {" + contentMap + "}", e);
			}

			ContentMapStatistics cnMapStats = contentMap.getStatistics();
			if (cnMapStats != null && renderResult != null) {
				for (Item item : Item.values()) {
					renderResult.info(CnMapPublisher.class, contentMap + " " + item.getDescription() + ": \t" + cnMapStats.get(item).getInfo());
				}
			}
			contentMap.enableStatistics(false);
		}

		// log filesystem attribute value statistics, if gathered
		FilesystemAttributeStatistics fsAttrStats = FilesystemAttributeValue.getStatistics();
		if (fsAttrStats != null && renderResult != null) {
			for (FilesystemAttributeStatistics.Item item : FilesystemAttributeStatistics.Item.values()) {
				renderResult.info(CnMapPublisher.class, item.getDescription() + ": \t" + fsAttrStats.get(item).getInfo());
			}
		}
		FilesystemAttributeValue.enableStatistics(false);

		MCCRStats mccrStats = MCCRHelper.getStatistics();
		if (mccrStats != null) {
			for (MCCRStats.Item item : MCCRStats.Item.values()) {
				renderResult.info(CnMapPublisher.class, item.getDescription() + ": \t" + mccrStats.get(item).getInfo());
			}
		}

		// log publishing of nodes
		List<Node> publishedNodes = Publisher.getPublishedNodes();

		for (Node node : publishedNodes) {
			node.setLastPublishTimestamp();
			ActionLogger.logCmd(ActionLogger.PAGEPUB, Node.TYPE_NODE, node.getId(), new Integer(0), "published " + node);
		}
	}

	/**
	 * Keep the connections of all contentmaps alive
	 */
	public void keepContentmapsAlive() {
		for (ContentMap contentMap : usedContentMaps) {
			contentMap.keepAlive(contentMapWorkers.get(contentMap.getId()));
		}
	}

	/**
	 * Get the collection of workers that will write into the contentmaps
	 * @return collection of workers (may be empty, but never null)
	 */
	public Collection<AsynchronousWorker> getWorkers() {
		return contentMapWorkers.values();
	}

	/**
	 * Get the contentmap for the given node
	 * @param node node
	 * @param considerNodeSetting true to consider the node setting "publish_contentmap"
	 * @return contentmap (may be null)
	 */
	public ContentMap getContentMap(Node node, boolean considerNodeSetting) {
		if (node == null) {
			return null;
		}
		if (considerNodeSetting && !node.doPublishContentmap()) {
			return null;
		}
		return nodeContentMaps.get(node);
	}

	/**
	 * Prepare the data for the given contentIds
	 * @param contentMap content map
	 * @param contentIds collection of contentIds
	 * @param channelId channel id (for mccr)
	 * @throws NodeException
	 */
	protected static void prepareData(ContentMap contentMap, Collection<String> contentIds, int channelId) throws NodeException {
		WriteableDatasource ds = contentMap.getWritableDatasource();
		String filterRule = null;
		Map<String, Object> dataMap = new HashMap<String, Object>();
		if (contentMap.isMultichannelling()) {
			// select objects by contentid
			filterRule = "object.contentid CONTAINSONEOF data.contentIds && object.channel_id == data.channel_id";
			// add all contentids
			dataMap.put("contentIds", contentIds);
			dataMap.put("channel_id", channelId);
			// set the channel id
			contentMap.getMCCRDatasource().setChannel(channelId);
		} else {
			// select objects by contentid
			filterRule = "object.contentid CONTAINSONEOF data.contentIds";
			// add all contentids to the data map
			dataMap.put("contentIds", contentIds);
		}
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(filterRule));
		filter.addBaseResolvable("data", new MapResolver(dataMap));

		// get the objects and prefill all attributes
		Collection<Resolvable> result = ds.getResult(filter, null);

		if (contentMap.isMultichannelling()) {
			MCCRHelper.prepareForUpdate(result);
		}
	}

//	/**
//	 * Read the tagmap entries 
//	 * @param type
//	 * @param nodeId
//	 * @return
//	 * @throws NodeException
//	 */
//	public static List<TagmapEntry> readTagmapEntries(int type, int nodeId) throws NodeException {
//		Transaction t = TransactionManager.getCurrentTransaction();
//		PreparedStatement st = null;
//		ResultSet res = null;
//		List<TagmapEntry> tagmapEntriesList = new Vector<TagmapEntry>();
//
//		try {
//			st = t.prepareStatement(
//					"SELECT t.tagname, t.mapname, t.attributetype, t.objtype, n.id " + "FROM tagmap t "
//					+ "INNER JOIN node n on n.contentrepository_id = t.contentrepository_id " + "WHERE t.object = ? AND n.id = ?");
//			st.setInt(1, type);
//			st.setInt(2, nodeId);
//			res = st.executeQuery();
//
//			while (res.next()) {
//				tagmapEntriesList.add(new TagmapEntry(res.getString("tagname"), res.getString("mapname"), res.getInt("attributetype"), res.getInt("objtype")));
//			}
//
//			return tagmapEntriesList;
//		} catch (SQLException e) {
//			throw new NodeException("Error while reading tagmap entries", e);
//		} finally {
//			t.closeResultSet(res);
//			t.closeStatement(st);
//		}
//	}

	/**
	 * Prepare the given object for writing it into the contentmap
	 * @param object object (file, image or folder)
	 * @param attributes optional set of attributes to prepare (null to prepare all attributes)
	 * @param contentMap content map
	 * @param dbfilesDir directory for db files
	 * @param writeBinaryContent true to also prepare binary content
	 * @param publishHandlers true to invoke the publish handlers
	 * @return instance containing the changeable and the set of attributes that were written into the changeable 
	 * @throws NodeException
	 */
	public static ChangeableWithAttributes prepareObjectForWriting(NodeObject object, Set<String> attributes, ContentMap contentMap, File dbfilesDir, boolean writeBinaryContent,
			boolean publishHandlers) throws NodeException {
		// We use wallclock time so that instant publishing doesn't break
		// cache consistency during a publish run
		int timestamp = (int) (System.currentTimeMillis() / 1000);
		Map<String, Object> dataMap = new HashMap<String, Object>();
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		RenderResult renderResult = t.getRenderResult();
		Changeable changeable = null;
		Set<String> publishedAttributes = new HashSet<>();

		int objType = -1;
		try (HandleDependenciesTrx depsTrx = new HandleDependenciesTrx(false)) {
			objType = ObjectTransformer.getInt(object.getTType(), -1);
		}

		// Images must be published as files into the content repository
		if (10011 == objType) {
			objType = 10008;
		}

		// check whether binary content shall really be written
		if (writeBinaryContent && !ObjectTransformer.isEmpty(attributes) && !attributes.contains("binarycontent")) {
			writeBinaryContent = false;
		}

		List<TagmapEntryRenderer> tagmapEntries = contentMap.getTagmapEntries(objType);

		if (logger.isInfoEnabled()) {
			logger.info("publishing " + object + " into contentmap " + contentMap);
		}

		renderType.push((StackResolvable) object);

		try {
			if (renderType.doHandleDependencies()) {
				DependencyObject depObject = new DependencyObject(object);
				renderType.pushRootDependentObject(depObject);
			}

			GenticsContentObject cnObject = null;
			StreamingResolvable streamable = null;

			if (contentMap.isMultichannelling()) {
				int channelId = 0;

				if (object instanceof Folder) {
					channelId = ObjectTransformer.getInt(((Folder) object).getNode().getId(), 0);
				} else if (object instanceof ContentFile) {
					channelId = ObjectTransformer.getInt(((ContentFile) object).getFolder().getNode().getId(), 0);
				}
				contentMap.getMCCRDatasource().setChannel(channelId);
				dataMap.clear();
				dataMap.put("contentid", objType + "." + object.getId());
				if (object instanceof ContentFile) {
					dataMap.put(WritableMultichannellingDatasource.MCCR_CHANNELSET_ID, ((ContentFile) object).getChannelSetId());
					dataMap.put(WritableMultichannellingDatasource.MCCR_CHANNEL_ID, channelId);
				} else if (object instanceof Folder) {
					dataMap.put(WritableMultichannellingDatasource.MCCR_CHANNELSET_ID, ((Folder) object).getChannelSetId());
					dataMap.put(WritableMultichannellingDatasource.MCCR_CHANNEL_ID, channelId);
				}
				MCCRObject mccrObject = (MCCRObject) contentMap.getMCCRDatasource().create(dataMap);

				changeable = mccrObject;
				streamable = mccrObject;
				mccrObject.setUpdateTimestamp(timestamp);
			} else {
				dataMap.clear();
				dataMap.put("contentid", objType + "." + object.getId());
				cnObject = (GenticsContentObject) contentMap.getDatasource().create(dataMap, -1, false);
				changeable = cnObject;
				streamable = cnObject;
				cnObject.setCustomUpdatetimestamp(timestamp);
			}

			// resolve all tagnames for the given folder and set as
			// properties
			for (TagmapEntryRenderer entry : tagmapEntries) {
				// omit writing attribute, if not dirted
				if (entry.skip(attributes)) {
					// preserve dependencies on omitted attribute
					renderType.preserveDependencies(entry.getMapname());
					continue;
				}

				// omit writing of binarycontent here (will be done later)
				if (object instanceof ContentFile && "binarycontent".equals(entry.getMapname())) {
					continue;
				}

				// only set the rendered property, if a tagname is set and the attribute is not a foreign link attribute
				if (!StringUtils.isEmpty(entry.getTagname()) && entry.getAttributeType() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
					// set the rendered property
					renderType.setRenderedProperty(entry.getMapname());

					Object value = resolveTagmapEntry(renderType, renderResult, entry);

					if (value instanceof GCNRenderable && value != null) {
						RenderResult result = new RenderResult();

						changeable.setProperty(entry.getMapname(), entry.transformValue(((GCNRenderable) value).render(result), LINKTRANSFORMER));
					} else {
						changeable.setProperty(entry.getMapname(), entry.transformValue(value, LINKTRANSFORMER));
					}
					publishedAttributes.add(entry.getMapname());
				}
			}

			// for files, determine whether the binarycontent shall
			// be written into the contentmap
			if (writeBinaryContent && object instanceof ContentFile && contentMap.isPublishFiles()) {
				try {
					ContentFile contentFile = (ContentFile) object;

					if (TransactionManager.getCurrentTransaction().getRenderType().getPreferences().getFeature("contentfile_data_to_db")) {
						PreparedStatement stmt = TransactionManager.getCurrentTransaction().prepareStatement(
								"SELECT binarycontent FROM contentfiledata WHERE contentfile_id = ?");

						stmt.setObject(1, object.getId());
						ResultSet rs = stmt.executeQuery();

						if (!rs.first()) {
							logger.error("Cannot find binary content for file " + object.getId() + "!");
							throw new NodeException("Cannot find binary content for file " + object.getId() + "!");
						}
						InputStream in = rs.getBinaryStream("binarycontent");
						ByteArrayOutputStream dataStream = new ByteArrayOutputStream();

						FileUtil.pooledBufferInToOut(in, dataStream);
						changeable.setProperty("binarycontent", dataStream.toByteArray());
						publishedAttributes.add("binarycontent");
					} else {
						// read the binary content from the db file
						// and store in the object
						File dataFile = new File(dbfilesDir, object.getId() + ".bin");

						if (logger.isInfoEnabled()) {
							logger.info("Writing binary content of {" + object + "} (" + dataFile.getAbsolutePath() + ") into {" + contentMap + "}");
						}

						if (contentFile.getFilesize() > 0) {
							// when the binary content is streamable, we set it as FilesystemAttributeValue
							if (streamable.isStreamable("binarycontent")) {
								FilesystemAttributeValue data = new FilesystemAttributeValue();
								
							data.setContinueIfFileNotFound(true);
								data.setData(dataFile);
								changeable.setProperty("binarycontent", data);
							} else {
								ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
								
								FileUtil.pooledBufferInToOut(new FileInputStream(dataFile), dataStream);
								changeable.setProperty("binarycontent", dataStream.toByteArray());
							}
							publishedAttributes.add("binarycontent");
						}
					}

					if (logger.isInfoEnabled()) {
						logger.info("Written binary content of {" + object + "}");
					}
				} catch (IOException e) {
					logger.error("Error while writing binarycontent of {" + object + "} into {" + contentMap + "}", e);
				} catch (SQLException e) {
					e.printStackTrace();
					logger.error("Error while getting binary content for file " + object.getId() + "!");
					// throw new NodeException("Error while getting binary
					// content for file " + object.getId() + "!");
				}
			}

			if (object instanceof ContentFile) {
				// set mother for files
				ContentFile file = (ContentFile) object;

				if (cnObject != null) {
					dataMap.clear();
					dataMap.put("contentid", Folder.TYPE_FOLDER_INTEGER + "." + file.getFolder().getId());
					GenticsContentObject mother = (GenticsContentObject) contentMap.getDatasource().create(dataMap, -1, false);

					cnObject.setMotherObject(mother);
				}

				// TODO asynchronize this!!!

				if (publishHandlers) {
					try {
						if (checkObjectExistence(changeable)) {
							contentMap.handleUpdateObject(changeable, Collections.unmodifiableSet(publishedAttributes));
						} else {
							contentMap.handleCreateObject(changeable);
						}
					} catch (CnMapPublishException e) {
						throw new NodeException("Publish object {" + changeable + "} into the ContentRepository failed (publish handler threw exception)", e);
					}
				}
			} else if (object instanceof Folder) {
				// set mother for folders
				Folder folder = (Folder) object;
				Folder mother = folder.getMother();

				if (mother != null && cnObject != null) {
					dataMap.clear();
					dataMap.put("contentid", Folder.TYPE_FOLDER_INTEGER + "." + mother.getId());
					GenticsContentObject motherObj = (GenticsContentObject) contentMap.getDatasource().create(dataMap, -1, false);

					if (motherObj == null || StringUtils.isEmpty(motherObj.getContentId())) {
						cnObject.setMotherContentId(Folder.TYPE_FOLDER_INTEGER + ".0");
					} else {
						cnObject.setMotherObject(motherObj);
					}
				} else if (cnObject != null) {
					cnObject.setMotherContentId(Folder.TYPE_FOLDER_INTEGER + ".0");
				}

				if (publishHandlers) {
					try {
						if (checkObjectExistence(changeable)) {
							contentMap.handleUpdateObject(changeable, Collections.unmodifiableSet(publishedAttributes));
						} else {
							contentMap.handleCreateObject(changeable);
						}
					} catch (CnMapPublishException e) {
						throw new NodeException("Publish object {" + changeable + "} into the ContentRepository failed (publish handler threw exception)", e);
					}
				}
			}

		} finally {
			// reset the rendered property
			renderType.setRenderedProperty(null);

			renderType.pop();
			if (renderType.doHandleDependencies()) {
				renderType.popDependentObject();
				renderType.storeDependencies();
			}
		}

		return new ChangeableWithAttributes(changeable, publishedAttributes);
	}

	/**
	 * Create operator instance that will write a changeable into a contentmap
	 * @param nodeId node ID
	 * @param wrapper wrapper for the changeable to write into the contentmap and the set of attributes that were prepared 
	 * @param contentMap contentmap to write to
	 * @param objectInfo object info
	 * @param publishHandlers true to invoke the publish handlers
	 * @return operator instance
	 * @throws NodeException
	 */
	public static Operator writePreparedObject(int nodeId, ChangeableWithAttributes wrapper, ContentMap contentMap, String objectInfo, boolean publishHandlers)
			throws NodeException {
		return () -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			RenderResult renderResult = t.getRenderResult();

			int objType = ObjectTransformer.getInt(wrapper.changeable.get("obj_type"), 0);
			contentMap.startWrite(objType);
			WriteableDatasource wds = contentMap.getWritableDatasource();

			try {
				if (publishHandlers) {
					try {
						if (checkObjectExistence(wrapper.changeable)) {
							contentMap.handleUpdateObject(wrapper.changeable, Collections.unmodifiableSet(wrapper.attributes));
						} else {
							contentMap.handleCreateObject(wrapper.changeable);
						}
					} catch (CnMapPublishException e) {
						throw new NodeException("Publish object {" + wrapper.changeable + "} into the ContentRepository failed (publish handler threw exception)", e);
					}
				}

				wds.store(Collections.singleton(wrapper.changeable));

				renderResult.info(CnMapPublisher.class, "written " + objectInfo + " into {" + contentMap + "}");
				logger.debug("setChanged(true)" + contentMap);
				contentMap.setChanged(true);
			} finally {
				contentMap.endWrite(objType);

				// notify the JMX bean
				if (objType == Folder.TYPE_FOLDER) {
					MBeanRegistry.getPublisherInfo().publishedFolder(nodeId);
				} else if (objType == ContentFile.TYPE_FILE) {
					MBeanRegistry.getPublisherInfo().publishedFile(nodeId);
				}
			}
		};
	}

	/**
	 * Write the page into the contentmap
	 * @param page page
	 * @param contentMap contentmap
	 * @param tagmapEntries tagmap entries
	 * @param source rendered page source
	 * @param worker worker (may be null)
	 * @param reportToPublishQueue true to report back to the publish queue when done
	 * @return Operator instance that will initiate necessary changes in the contentmap, when called
	 * @throws NodeException
	 */
	public static Operator writePageIntoCR(Page page, ContentMap contentMap, Map<TagmapEntryRenderer, Object> tagmapEntries, String source,
			AsynchronousWorker worker, boolean reportToPublishQueue) throws NodeException {
		Map<String, Object> dataMap = new HashMap<String, Object>();

		int channelId = ObjectTransformer.getInt(page.getFolder().getNode().getId(), 0);

		// prepare the object
		dataMap.clear();
		dataMap.put("contentid", Page.TYPE_PAGE + "." + page.getId());
		if (contentMap.isMultichannelling()) {
			dataMap.put(WritableMCCRDatasource.MCCR_CHANNELSET_ID, page.getChannelSetId());
			dataMap.put(WritableMCCRDatasource.MCCR_CHANNEL_ID, channelId);
		}

		// resolve all tagnames for the given folder and set as
		// properties
		for (Map.Entry<TagmapEntryRenderer, Object> entry : tagmapEntries.entrySet()) {
			TagmapEntryRenderer tagmapEntry = entry.getKey();
			Object value = entry.getValue();

			// only write the content, if a tagmap entry exists
			if ("content".equals(tagmapEntry.getMapname())) {
				dataMap.put("content", source);
			}

			// only set the rendered property, if a tagname is set and the attribute is not a foreign link attribute
			if (!StringUtils.isEmpty(tagmapEntry.getTagname()) && tagmapEntry.getAttributeType() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
				if (value instanceof GCNRenderable && value != null) {
					RenderResult result = new RenderResult();

					dataMap.put(tagmapEntry.getMapname(), ((GCNRenderable) value).render(result));
				} else {
					// even if the value is null, it is still added to the dataMap
					// to make sure the value and its entry are removed from the tagmap
					dataMap.put(tagmapEntry.getMapname(), value);
				}
			}
		}

		// when multichannelling, we do this asynchronously
		int pageId = ObjectTransformer.getInt(page.getId(), 0);
		int folderId = ObjectTransformer.getInt(page.getFolder().getId(), 0);
		if (worker != null) {
			return () -> {
				AsynchronousJob cnMapUpdate = new AsynchronousCnMapUpdate(contentMap, dataMap, pageId, channelId, folderId, reportToPublishQueue);

				worker.addAsynchronousJob(cnMapUpdate);
				contentMap.setChanged(true);
			};
		} else {
			return () -> {
				contentMap.startWrite(Page.TYPE_PAGE);
				try {
					Changeable changeable = prepareChangeableForPage(contentMap, dataMap, channelId, folderId);
					boolean objectExists = checkObjectExistence(changeable);
					WriteableDatasource wds = contentMap.getWritableDatasource();

					try {
						if (objectExists) {
							contentMap.handleUpdateObject(changeable, Collections.unmodifiableSet(dataMap.keySet()));
						} else {
							contentMap.handleCreateObject(changeable);
						}
					} catch (CnMapPublishException e) {
						throw new NodeException("Publish object {" + changeable + "} into the ContentRepository failed (publish handler threw exception)", e);
					}

					wds.store(Collections.singleton(changeable));
				} finally {
					contentMap.endWrite(Page.TYPE_PAGE);
				}
				contentMap.setChanged(true);
			};
		}
	}

	/**
	 * Prepare a changeable from the data in the given datamap that is suitable for storing in the given content map
	 * @param contentMap content map
	 * @param dataMap map holding all data
	 * @param channelId channel id
	 * @param folderId folder id
	 * @return changeable
	 * @throws NodeException
	 */
	protected static Changeable prepareChangeableForPage(ContentMap contentMap, Map<String, Object> dataMap, int channelId, int folderId) throws NodeException {
		Changeable changeable = null;

		// set the current timestamp as updatetimestamp for the changeables
		int updatetimestamp = (int)(System.currentTimeMillis()/1000);

		// prepare the object
		if (contentMap.isMultichannelling()) {
			MCCRObject mccrObject = (MCCRObject) contentMap.getMCCRDatasource().create(dataMap);
			mccrObject.setUpdateTimestamp(updatetimestamp);

			changeable = mccrObject;
		} else {
			GenticsContentObject cnObject = (GenticsContentObject) contentMap.getDatasource().create(dataMap, -1, false);
			cnObject.setCustomUpdatetimestamp(updatetimestamp);

			changeable = cnObject;

			// set the mother
			Map<String, Object> motherMap = new HashMap<String, Object>(1);
			motherMap.put("contentid", Folder.TYPE_FOLDER_INTEGER + "." + folderId);
			GenticsContentObject mother = (GenticsContentObject) contentMap.getDatasource().create(motherMap, -1, false);

			cnObject.setMotherObject(mother);
		}

		return changeable;
	}

	/**
	 * Check the given changeable for existence
	 * @param changeable changeable
	 * @return true if the object already exists, false if not
	 * @throws NodeException
	 */
	protected static boolean checkObjectExistence(Changeable changeable) throws NodeException {
		if (changeable instanceof MCCRObject) {
			return ((MCCRObject) changeable).exists();
		} else if (changeable instanceof GenticsContentObject) {
			return ((GenticsContentObject) changeable).exists();
		} else {
			if (changeable == null) {
				throw new NodeException("Cannot store null object into contentmap");
			} else {
				throw new NodeException("Cannot store object of " + changeable.getClass() + " into contentmap");
			}
		}
	}

	/**
	 * Remove the given object from the contentrepository
	 * @param object object to remove
	 * @param contentMap contentmap where the object shall be removed
	 * @param node Node of the object
	 * @return Operator instance that will initiate necessary changes in the contentmap, when called
	 * @throws NodeException
	 */
	public static Operator removeObjectFromCR(NodeObject object, ContentMap contentMap, Node node) throws NodeException {
		Map<String, String> dataMap = new HashMap<String, String>();
		int objType = ObjectTransformer.getInt(object.getTType(), -1);
		if (objType == ContentFile.TYPE_IMAGE) {
			objType = ContentFile.TYPE_FILE;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Removing " + object + " from contentmap " + contentMap + " of " + node);
		}

		dataMap.put("contentid", objType + "." + object.getId());

		return () -> {
			if (contentMap.isMultichannelling()) {
				if (node == null) {
					throw new NodeException("Cannot remove an object from a mccr without specifying a node");
				}
				if (object instanceof LocalizableNodeObject<?>) {
					@SuppressWarnings("unchecked")
					LocalizableNodeObject<NodeObject> locObject = (LocalizableNodeObject<NodeObject>) object;

					contentMap.getMCCRDatasource().setChannel(ObjectTransformer.getInt(node.getId(), 0));
					dataMap.put(WritableMCCRDatasource.MCCR_CHANNELSET_ID, ObjectTransformer.getString(locObject.getChannelSetId(), null));
					// set the channel id
					dataMap.put(WritableMultichannellingDatasource.MCCR_CHANNEL_ID, ObjectTransformer.getString(node.getId(), null));
				} else {
					throw new NodeException("Cannot write object " + object + " into a mccr");
				}
			}

			Changeable dsObject = contentMap.getWritableDatasource().create(dataMap);
			DatasourceInfo deleteInfo = contentMap.getWritableDatasource().delete(Collections.singletonList(dsObject));

			if (deleteInfo.getAffectedRecordCount() > 0) {
				contentMap.setChanged(true);
				try {
					contentMap.handleDeleteObject(dsObject);
				} catch (CnMapPublishException e) {
					throw new NodeException("Remove object {" + dsObject + "} from the ContentRepository failed (publish handler threw exception)", e);
				}
			}
		};
	}

	/**
	 * Result Processor instance that generates ObjectTypeBeans and AttributeTypeBeans for the contentrepository
	 */
	public static class TagmapResultProcessor implements ResultProcessor {

		/**
		 * ObjectType Bean for folders
		 */
		protected ObjectTypeBean folder;

		/**
		 * ObjectType Bean for pages
		 */
		protected ObjectTypeBean page;

		/**
		 * ObjectType Bean for files
		 */
		protected ObjectTypeBean file;

		/**
		 * contentmap object for which the attribute type information is collected
		 */
		protected ContentMap contentMap;

		/**
		 * Create an instance of this result processor
		 * @param contentMap content map for which the attribute type
		 *        information is collected
		 */
		public TagmapResultProcessor(ContentMap contentMap) {
			this.contentMap = contentMap;

			// create the objecttypes
			folder = new ObjectTypeBean();
			folder.setType(new Integer(Folder.TYPE_FOLDER));
			folder.setName("folder");
			folder.setExcludeVersioning(false);

			page = new ObjectTypeBean();
			page.setType(new Integer(Page.TYPE_PAGE));
			page.setName("page");
			page.setExcludeVersioning(false);

			file = new ObjectTypeBean();
			file.setType(new Integer(ContentFile.TYPE_FILE));
			file.setName("file");
			file.setExcludeVersioning(false);
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
		 */
		public void process(ResultSet rs) throws SQLException {
			// collect the attribute names for pages, files and folders here (we will need them
			// when language or permission information must be added)
			List<String> pageAttributeNames = new Vector<String>();
			List<String> folderAttributeNames = new Vector<String>();
			List<String> fileAttributeNames = new Vector<String>();

			Transaction t = null;
			boolean filesystemAttributes = false;

			try {
				t = TransactionManager.getCurrentTransaction();
				filesystemAttributes = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.CR_FILESYSTEM_ATTRIBUTES);
			} catch (TransactionException e1) {
				throw new SQLException("Error while getting tagmap entries: Could not get current transaction");
			}

			while (rs.next()) {
				ObjectAttributeBean attribute = new ObjectAttributeBean(rs.getString("mapname"), rs.getInt("attributetype"), rs.getBoolean("optimized"), null,
						rs.getBoolean("multivalue"), rs.getInt("object"), rs.getInt("objtype"), null, rs.getString("foreignlinkattribute"),
						rs.getString("foreignlinkattributerule"), false, rs.getBoolean("filesystem"));

				// when the feature is switched off, we set all attributes to not store in the filesystem
				if (!filesystemAttributes) {
					attribute.setFilesystem(false);
				}

				// normalize the attribute, in case some weird data is set (e.g. a linkedObjectType for a non-link attribute)
				attribute.normalizedAttribute();

				switch (attribute.getObjecttype()) {
				case Folder.TYPE_FOLDER:
					folder.addAttributeType(attribute);
					folderAttributeNames.add(attribute.getName());
					break;

				case Page.TYPE_PAGE:
					page.addAttributeType(attribute);
					pageAttributeNames.add(attribute.getName());
					break;

				case ContentFile.TYPE_FILE:
					file.addAttributeType(attribute);
					fileAttributeNames.add(attribute.getName());
					break;

				default:
					break;
				}
			}

			// add the attribute types for the language information to the page object (if not already defined)
			// content_languages, contentid_[code]
			if (contentMap.addLanguageInformation()) {
				if (!pageAttributeNames.contains("content_languages")) {
					page.addAttributeType(
							new ObjectAttributeBean("content_languages", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, true, Page.TYPE_PAGE, 0, null, null,
							null, false, false));
				}

				if (!pageAttributeNames.contains("content_language")) {
					page.addAttributeType(
							new ObjectAttributeBean("content_language", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, Page.TYPE_PAGE, 0, null, null,
							null, false, false));
				}

				if (!pageAttributeNames.contains("contentset_id")) {
					page.addAttributeType(
							new ObjectAttributeBean("contentset_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, false, null, false, Page.TYPE_PAGE, 0, null, null,
							null, false, false));
				}

				// get the languages which are published into the contentmap
				try {
					List<ContentLanguage> languages = contentMap.getLanguages();

					for (ContentLanguage language : languages) {
						String attributeName = "contentid_" + language.getCode();

						if (!pageAttributeNames.contains(attributeName)) {
							page.addAttributeType(
									new ObjectAttributeBean(attributeName, GenticsContentAttribute.ATTR_TYPE_OBJ, false, null, false, Page.TYPE_PAGE, Page.TYPE_PAGE,
									null, null, null, false, false));
						}
					}
				} catch (NodeException e) {
					logger.error(e);
					throw new SQLException("Error while adding the language information attributes");
				}
			}

			// add permission information if configured to do so
			if (contentMap.addPermissionInformation()) {
				// folder attributes
				for (int i = 0; i < ContentMap.FOLDER_PERMISSION_ATTRIBUTES.length; i++) {
					String attributeName = ContentMap.FOLDER_PERMISSION_ATTRIBUTES[i];

					if (!folderAttributeNames.contains(attributeName)) {
						folder.addAttributeType(
								new ObjectAttributeBean(attributeName, GenticsContentAttribute.ATTR_TYPE_INTEGER, false, null, true, Folder.TYPE_FOLDER, 0, null, null,
								null, false, false));
					}
				}

				// page attributes
				for (int i = 0; i < ContentMap.PAGE_PERMISSION_ATTRIBUTES.length; i++) {
					String attributeName = ContentMap.PAGE_PERMISSION_ATTRIBUTES[i];

					if (!pageAttributeNames.contains(attributeName)) {
						page.addAttributeType(
								new ObjectAttributeBean(attributeName, GenticsContentAttribute.ATTR_TYPE_INTEGER, false, null, true, Page.TYPE_PAGE, 0, null, null,
								null, false, false));
					}
				}

				// file attributes
				for (int i = 0; i < ContentMap.FILE_PERMISSION_ATTRIBUTES.length; i++) {
					String attributeName = ContentMap.FILE_PERMISSION_ATTRIBUTES[i];

					if (!fileAttributeNames.contains(attributeName)) {
						file.addAttributeType(
								new ObjectAttributeBean(attributeName, GenticsContentAttribute.ATTR_TYPE_INTEGER, false, null, true, ContentFile.TYPE_FILE, 0, null,
								null, null, false, false));
					}
				}
			}
		}

		/**
		 * Get the file objecttype bean
		 * @return file objecttype bean
		 */
		public ObjectTypeBean getFile() {
			return file;
		}

		/**
		 * Get the folder objecttype bean
		 * @return folder objecttype bean
		 */
		public ObjectTypeBean getFolder() {
			return folder;
		}

		/**
		 * Get the page objecttype bean
		 * @return page objecttype bean
		 */
		public ObjectTypeBean getPage() {
			return page;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
		 */
		public void takeOver(ResultProcessor p) {}
	}

	/**
	 * Class encapsulating a changeable and the set of attributes that were filled into the changeable
	 */
	public static class ChangeableWithAttributes {
		/**
		 * Changeable
		 */
		public Changeable changeable;

		/**
		 * Set of attributes
		 */
		public Set<String> attributes;

		/**
		 * Create an instance
		 * @param changeable changeable
		 * @param attributes set of attributes
		 */
		public ChangeableWithAttributes(Changeable changeable, Set<String> attributes) {
			this.changeable = changeable;
			this.attributes = attributes;
		}
	}

	/**
	 * Asynchronous worker that will set the current transaction when running (into the worker thread)
	 */
	public class CnMapWorker extends AsynchronousWorker {

		/**
		 * Transaction
		 */
		protected Transaction t;

		/**
		 * Create an instance
		 * @param name name
		 * @param onErrorExit true if the worker shall exit on error
		 * @param t transaction
		 */
		public CnMapWorker(String name, boolean onErrorExit, Transaction t) {
			super(name, onErrorExit, ObjectTransformer.getInt(
					config.getDefaultPreferences().getProperty("multithreaded_publishing.queuelimit"),
					AsynchronousWorker.DEFAULT_QUEUELIMIT));
			setRenderResult(t.getRenderResult());
			this.t = t;
		}

		@Override
		public void run() {
			// first set the transaction to be the current transaction in the worker thread
			TransactionManager.setCurrentTransaction(t);
			super.run();
		}
	}
}
