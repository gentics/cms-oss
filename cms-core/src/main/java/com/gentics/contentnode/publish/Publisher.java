/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Publisher.java,v 1.60.2.1 2011-03-15 17:26:52 tobiassteiner Exp $
 */
package com.gentics.contentnode.publish;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.logging.log4j.Level;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.etc.ObjectTransformer.InputStreamReaderRunnable;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.QueueEntry;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.PublishData;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.Executable;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.jmx.PublisherInfo;
import com.gentics.contentnode.jmx.PublisherPhase;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.contentnode.publish.mesh.MeshPublishController;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.publish.mesh.MeshPublisher.Scheduled;
import com.gentics.contentnode.publish.wrapper.PublishableTemplate;
import com.gentics.contentnode.render.PublishRenderResult;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.content.FilesystemAttributeValue;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.genericexceptions.GenericFailureException;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;
import com.gentics.lib.render.exception.PublishException;

/**
 * Main publish process class. This implements and controls the publish process.
 *
 * You should not call this directly. Publish processes are managed by the publishController.
 * @see PublishController
 *
 */
public class Publisher implements Runnable {

	private ContentNodeFactory factory;
	private SimplePublishInfo myPublishInfo;
	private long lastPublishRun;

	/** A collection of objects which were instant published since the last regular publish run started. */
	private Set<String> instantPublished = Collections.synchronizedSet(new HashSet<>());

	/**
	 * keep last publish run as a unix timestamp as needed for dependency map cleanup
	 */
	private int lastPublishRunUnixtime;
	private String lftpPath;

	/**
	 * the logger for the publisher
	 */
	private NodeLogger logger = NodeLogger.getNodeLogger(Publisher.class);
	private long timestamp;
	private RenderResult renderResult;
	private File publishDir;
	private CnMapPublisher cnMapPublisher;
	private IWorkPhase contentMapPublishPhase;

	/**
	 * Workphase to check offline files
	 */
	private IWorkPhase checkOfflineFilesPhase;

	/**
	 * Workphase to check online files
	 */
	private IWorkPhase checkOnlineFilesPhase;

	private IWorkPhase publishPagePhase;

	/**
	 * List of page ids to publish
	 */
	private PagePublisher pagePublisher;
	private IWorkPhase writeFsPhase;
	private FilePublisher filePublisher;
	private IWorkPhase writeFsPagesAndFilesPhase;
	private IWorkPhase writeFsImageStorePhase;

	/**
	 * workphase for the "work" of waiting for the dirt events to be done
	 */
	private IWorkPhase waitForDirtEventsPhase;

	/**
	 * Log file used by the publisher
	 */
	private File logfile;

	/**
	 * Verbose log file used by the publisher
	 */
	private File logfileVerbose;

	/**
	 * File usage map. Will be prepared in {@link #prepareFileUsageMap()} and used in {@link #checkOfflineFiles(IWorkPhase, MeshPublishController)}.
	 */
	private Map<Integer, Set<Integer>> fileUsageMap;

	/**
	 * The number of pages at which a publish run is considered
	 * representative
	 */
	private int representativePageCount = 100;
	private IWorkPhase timeManagementPhase;
	private IWorkPhase initPhase;
	private IWorkPhase finalizePhase;

	private PublishData publishData;

	private Throwable error;

	private static SimpleDateFormat logfilenameformat = new SimpleDateFormat("'publishrun_'yyyy-MM-dd_HH-mm-ss'.txt'");
	private static SimpleDateFormat logfilenameformatVerbose = new SimpleDateFormat("'publishrun_'yyyy-MM-dd_HH-mm-ss'.verbose.txt'");
	private static SimpleDateFormat logfilenameformatCsv = new SimpleDateFormat("'publishrun_'yyyy-MM-dd_HH-mm-ss'.etastat.csv.txt'");
	// private static SimpleDateFormat logfilenameformatCsvPng = new SimpleDateFormat("'publishrun_'yyyy-MM-dd_HH-mm-ss'.etastat.png'");
	private static String logfilenameformatCsvPngCurrent = "publishrun_current.etastat.png";

	/**
	 * Get all nodes that have publishing enabled
	 * @return list of nodes with publishing enabled
	 * @throws NodeException
	 */
	public static List<Node> getPublishedNodes() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		return t.getObjects(Node.class, DBUtils.select("SELECT id FROM node WHERE disable_publish = ?", ps -> {
			ps.setInt(1, 0);
		}, DBUtils.IDS));
	}

	/**
	 * create a new publish process.
	 * @param force true, if errors in stages should be ignored.
	 *
	 * @throws NodeException if an error happens during initialization.
	 */
	public Publisher(boolean force) {
		factory = ContentNodeFactory.getInstance();

		myPublishInfo = new SimplePublishInfo();
		generateWorkPhases();
	}

	public void run() {
		PublisherInfo publisherInfo = MBeanRegistry.getPublisherInfo();

		publisherInfo.setRunning(true);
		publisherInfo.setPhase(PublisherPhase.INITIALIZING);
		RuntimeProfiler.beginMark(ComponentsConstants.GENTICSROOT);
		RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_RUN);

		// make sure that the publisher has a backend language set
		ContentNodeHelper.setLanguageId(2);

		myPublishInfo.begin();

		PublishRenderResult loggingRenderResult = null;
		boolean writeFsSuccess = false;
		NodePreferences prefs = getConfiguration().getDefaultPreferences();
		boolean isMultithreaded = prefs.isFeature(Feature.MULTITHREADED_PUBLISHING);

		publisherInfo.setMultiThreaded(isMultithreaded);

		Transaction t = null;
		QueueEntry publishEntry = null;

		try {

			// Create render result which logs to file ...

			representativePageCount = ObjectTransformer.getInt(prefs.getProperty("contentnode.config.representative_page_count"), representativePageCount);
			File logdir = new File(ConfigurationValue.LOGS_PATH.get(), "publish");

			if (!logdir.exists()) {
				logdir.mkdirs();
			}
			logfile = new File(logdir, logfilenameformat.format(new Date()));
			logfileVerbose = new File(logdir, logfilenameformatVerbose.format(new Date()));
			Level verboseMinLevel = Level.INFO;

			if (Boolean.getBoolean("com.gentics.contentnode.publish.enableverboselogging")) {
				verboseMinLevel = Level.DEBUG;
			}
			loggingRenderResult = new PublishRenderResult(logfile, Level.INFO, verboseMinLevel);
			loggingRenderResult.setVerboseLogfile(logfileVerbose);
			this.renderResult = loggingRenderResult;

			Map<?, ?> errorhandling = prefs.getPropertyMap("contentnode.config.publish_errorhandling");

			loggingRenderResult.configureErrorHandling(errorhandling);

			// determine the Gentics Content.Node Version
			String versionInfo = "";
			try {
				// load the Main class for GCN
				Class<?> mainGCNClass = Class.forName("com.gentics.contentnode.Main");
				// invoke the static getInfo method
				Method getInfo = mainGCNClass.getMethod("getInfo");
				versionInfo = getInfo.invoke(null).toString();
				// don't print the version info, if it starts with null (which is the case in development environments)
				if (versionInfo.startsWith("null")) {
					versionInfo = "";
				}
			} catch (Exception e1) {
			}
			renderResult.info(Publisher.class, "Starting Publishrun ... " + versionInfo);

			// first add the publish run into the dirtqueue
			publishEntry = new QueueEntry((int) (timestamp / 1000));
			publishEntry.store(factory);

			// check whether we are allowed to start the publish process
			boolean beginPublish = false;

			boolean workPhaseStarted = false;
			int lastOlderQueueEntries = 0;

			publisherInfo.setPhase(PublisherPhase.DIRTQUEUE);
			while (!beginPublish) {
				// check whether the publish process has been interrupted
				PublishRenderResult.checkInterrupted();

				t = factory.startTransaction(true);

				int olderQueueEntries = QueueEntry.getNumberOfOlderQueueEntries(publishEntry);

				if (!workPhaseStarted) {
					waitForDirtEventsPhase.addWork(olderQueueEntries);
					waitForDirtEventsPhase.begin();
					lastOlderQueueEntries = olderQueueEntries;
					workPhaseStarted = true;
				} else {
					waitForDirtEventsPhase.doneWork(lastOlderQueueEntries - olderQueueEntries);
					// TODO: check whether no work was done in a specified amount of time
					lastOlderQueueEntries = olderQueueEntries;
				}

				// get the oldest entry
				QueueEntry oldestEntry = QueueEntry.getOldestQueueEntry();

				if (!publishEntry.equals(oldestEntry)) {
					// we found an older queued entry that is not the
					// publishing

					// the oldest entry is an old publish entry (not this
					// one),
					// so just delete it
					if (oldestEntry.isPublish()) {
						oldestEntry.delete();
					} else {
						renderResult.info(Publisher.class, "Waiting for queued dirt events to complete... (still to be done: " + olderQueueEntries + ")");
						try {
							Thread.sleep(ObjectTransformer.getInt(System.getProperty("com.gentics.contentnode.dirtqueue.wait"), 10000));
						} catch (InterruptedException e) {
							throw new PublishInterruptedException("Sleep was interrupted - publish run was probably canceled by user.", e);
						}
					}
				} else {
					// the oldest queued entry is this publish run, we may start
					// now
					beginPublish = true;
				}

				t.commit();
			}

			if (workPhaseStarted) {
				waitForDirtEventsPhase.done();
			}

			myPublishInfo.begin(); // restart publish phase ..
			PublishController.setRunning();

			try {
				doTimeManagement(prefs);
			} catch (NodeException e) {
				renderResult.fatal(Publisher.class, "Error while doing time management.", e);
				throw e;
			}

			try {
				// start a new transaction
				// do not use connection pooling for the db connections here!
				// (reduces memory consumption during long running publish
				// processes)
				// when doing this for a test, we do use the connection pool, because tests will start/stop many publish processes in a short time, and this might exhaust the available ports on the system or the database
				t = factory.startTransaction(ObjectTransformer.getBoolean(System.getProperty("com.gentics.contentnode.testmode"), false), isMultithreaded);
				t.setTimestamp(timestamp);
				RenderType renderType = RenderType.getDefaultRenderType(getConfiguration().getDefaultPreferences(), RenderType.EM_PUBLISH, null, -1);

				renderType.setRenderUrlFactory(
						new StaticUrlFactory(RenderType.parseLinkWay(prefs.getProperty("contentnode.linkway")),
						RenderType.parseLinkWay(prefs.getProperty("contentnode.linkway_file")), prefs.getProperty("contentnode.linkway_file_path")));
				t.setRenderType(renderType);
				t.setRenderResult(renderResult);
				TransactionManager.setCurrentTransaction(t);
				t.preparePublishData();

				publishData = t.getPublishData();
			} catch (NodeException e) {
				renderResult.fatal(Publisher.class, "Error while starting transaction.", e);
				throw e;
			}

			// start the transaction for dependencymap2
			DependencyManager.startPublishTransaction(factory);

			long startPublish = System.currentTimeMillis();

			@SuppressWarnings("serial")
			Map<Integer, Integer> objectsToPublishCount = new HashMap<Integer, Integer>() {
				{
					put(Folder.TYPE_FOLDER_INTEGER, 0);
					put(Page.TYPE_PAGE_INTEGER, 0);
					put(com.gentics.contentnode.object.File.TYPE_FILE_INTEGER, 0);
				}
			};

			try {

				// Create a logcmd entry for the start of the publish run
				ActionLogger.logCmd(ActionLogger.PUBLISH_START, 0, null, null, "Publish Run start");

				// Create a logcmd entry for the publish start of every node
				List<Node> nodes = getPublishedNodes();

				for (Node node : nodes) {
					ActionLogger.logCmd(ActionLogger.PUBLISH_NODE_START, Node.TYPE_NODE, node.getId(), null,
							"Publish Run for node { " + node.getId() + " } started");
				}

				// Get last publish run ...
				lastPublishRun = 0;
				lastPublishRunUnixtime = 0;

				// TODO use ActionLogger class
				DBUtils.executeStatement("SELECT MAX(timestamp) as timestamp FROM logcmd WHERE cmd_desc_id = ?", new SQLExecutor() {
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, ActionLogger.PUBLISH_RUN);
						stmt.setFetchSize(1);
					}

					public void handleResultSet(ResultSet rs) throws SQLException {
						if (rs.next()) {
							lastPublishRunUnixtime = rs.getInt(1);
							lastPublishRun = lastPublishRunUnixtime * 1000L;
						}
					}
				});
				if (lastPublishRun == 0) {
					logger.error("Unable to get timestamp of last publish run.");
				}

				if (logger.isInfoEnabled()) {
					logger.info("Starting background publish process with transaction {" + t + "}");
				}

				if (renderResult != null) {
					renderResult.info(Publisher.class, "Last Publishrun completed at " + new Date(lastPublishRun));
				}

				// initialize MeshPublisher instances
				try (MeshPublishController meshPublishController = MeshPublishController.get(myPublishInfo)) {
					objectsToPublishCount = initializeWorkPhases(nodes, meshPublishController);

					// start dependencymap cleanup
					if (renderResult != null) {
						renderResult.info(Publisher.class, "Removing dependencies of offline/deleted pages");
					}
					DependencyManager.DependencyMapCleaner.cleanupDependencies(lastPublishRunUnixtime);

					pagePublisher.setMeshPublishController(meshPublishController);

					meshPublishController.begin();
					meshPublishController.checkSchemasAndProjects();
					meshPublishController.waitForMigrations();

					meshPublishController.removeOfflineObjects();

					try {
						publisherInfo.setPhase(PublisherPhase.CONTENTMAP);
						RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_UPDATEMAPS);

						// start updating folders and files
						meshPublishController.publishFoldersAndFiles();

						cnMapPublisher = updateContentMaps();
					} finally {
						RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_UPDATEMAPS);
					}

					try {
						publisherInfo.setPhase(PublisherPhase.PAGES);
						RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_UPDATEPAGES);
						meshPublishController.publishPages();
						pagePublisher.publishPages(renderResult);
						// if (isMultithreaded) {
						// publishPagesThreaded(cnMapPublisher);
						// } else {
						// publishPages(cnMapPublisher);
						// }
					} finally {
						RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_UPDATEPAGES);
					}

					boolean autoOffline = prefs.isFeature(Feature.CONTENTFILE_AUTO_OFFLINE);

					try {
						publisherInfo.setPhase(PublisherPhase.FILEDEPENDENCIES);
						RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_FILEDEPENDENCIES);

						if (autoOffline) {
							// prepare the file usage map
							prepareFileUsageMap();
						}

						checkOfflineFilesPhase.begin();
						// check files marked as offline (whether they should go online)
						meshPublishController.checkOfflineFiles();
						checkOfflineFiles(checkOfflineFilesPhase, meshPublishController);
						checkOfflineFilesPhase.done();

						// check files marked as online (whether they should go offline)
						checkOnlineFilesPhase.begin();
						checkOnlineFiles(checkOnlineFilesPhase, meshPublishController);
						checkOnlineFilesPhase.done();
					} finally {
						if (autoOffline) {
							removeFileUsageMap();
						}
						RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_FILEDEPENDENCIES);
					}

					meshPublishController.waitForRenderAndWrite();
					meshPublishController.handlePostponedUpdates();

					meshPublishController.waitForRenderAndWrite();
					meshPublishController.success();
				}

				try {
					RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_WRITEFS);
					writeFsPhase.begin();
					writeFsSuccess = writeFs();
					writeFsPhase.done();
				} finally {
					RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_WRITEFS);
				}

				finalizePhase.begin();

				publisherInfo.setPhase(PublisherPhase.FINALIZING);
				// set lastupdate timestamps
				RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_LASTUPDATE);
				cnMapPublisher.setLastMapUpdate();
				RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_UPDATEMAPS_LASTUPDATE);

				// finish the page publisher (sets the page status for published pages to 2)
				try {
					RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_UPDATEPAGES_FINISH);
					long start = System.currentTimeMillis();

					logger.debug("starting Publisher.finish():");
					pagePublisher.finish();
					renderResult.info(Publisher.class, "Finalized page publisher in {" + (System.currentTimeMillis() - start) + "} ms.");
				} catch (NodeException e) {
					throw new GenericFailureException("Error while finalizing page publisher.", e);
				} finally {
					RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_UPDATEPAGES_FINISH);
				}

				// Updating publish timestamp in nodesetup
				ActionLogger.logCmd(ActionLogger.PUBLISH_RUN, 0, new Integer(0), new Integer(0), "Publish Run completed. {" + System.currentTimeMillis() + "}");

				if (writeFsSuccess) {
					// rsync
					if (!StringUtils.isEmpty(prefs.getProperty("rsync"))) {
						String cmd = prefs.getProperty("rsync");

						renderResult.info(Publisher.class, "executing rsync command {" + cmd + "}");
						ExecResult execResult = readExecResult(cmd);

						renderResult.info(Publisher.class, "executed rsync command. " + execResult.toString());
					}
					// ftpsync
					if (ObjectTransformer.getBoolean(prefs.getProperty("ftpsync"), false)) {
						lftpPath = prefs.getProperty("lftp_path");

						DBUtils.executeStatement("SELECT host, pub_dir, ftphost, ftplogin, ftppassword," + " ftpwwwroot FROM node WHERE ftpsync = 1",
								new SQLExecutor() {
							public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
								while (rs.next()) {
									execFtpSync(renderResult, rs.getString("host"), rs.getString("pub_dir"), rs.getString("ftphost"), rs.getString("ftplogin"),
											rs.getString("ftppassword"), rs.getString("ftpwwwroot"));
								}
							}
						});
					}
				}

				myPublishInfo.setPublishedFolderCount(
					objectsToPublishCount.get(Folder.TYPE_FOLDER_INTEGER));
				myPublishInfo.setPublishedPageCount(
					objectsToPublishCount.get(Page.TYPE_PAGE_INTEGER));
				myPublishInfo.setPublishedFileCount(
					objectsToPublishCount.get(com.gentics.contentnode.object.File.TYPE_FILE_INTEGER));
				myPublishInfo.setPublishedFormCount(objectsToPublishCount.get(Form.TYPE_FORM));
			} catch (Throwable e) {
				// if an error was set before, it is the cause for the publish process to fail, so we use that instead of the caught error
				if (error != null) {
					e = error;
				}
				if (!prefs.getFeature("override_publish_errors")) {
					try {
						logger.error("Error during publish run. Rolling back transaction.", e);
						renderResult.error(Publisher.class, "Publish run failed. Exiting.", e);
						myPublishInfo.setReturnCode(PublishInfo.RETURN_CODE_ERROR);
						myPublishInfo.setError(e);
					} finally {
						// rollback also dependencymap2 transaction (this is
						// important, without this dependencymap2 table will stay
						// locked !)
						try {
							DependencyManager.rollbackPublishTransaction();

						// handle failed publish process
						Map<Integer, Integer> notPublishedCount = PublishQueue.handleFailedPublishProcess();

						myPublishInfo.setPublishedFolderCount(
							objectsToPublishCount.get(Folder.TYPE_FOLDER_INTEGER)
								- notPublishedCount.get(Folder.TYPE_FOLDER_INTEGER));
						myPublishInfo.setPublishedPageCount(
							objectsToPublishCount.get(Page.TYPE_PAGE_INTEGER)
								- notPublishedCount.get(Page.TYPE_PAGE_INTEGER));
						myPublishInfo.setPublishedFileCount(
							objectsToPublishCount.get(com.gentics.contentnode.object.File.TYPE_FILE_INTEGER)
								- notPublishedCount.get(com.gentics.contentnode.object.File.TYPE_FILE_INTEGER));
						myPublishInfo.setPublishedFormCount(
								objectsToPublishCount.get(Form.TYPE_FORM) - notPublishedCount.get(Form.TYPE_FORM));
						} finally {
							t.rollback();
						}

						if (cnMapPublisher != null) {
							cnMapPublisher.rollback();
							cnMapPublisher = null;
						}
					}

					return;
				} else {
					logger.error(
							"Error during publish run. " + "Error override was activated, there will be NO ROLLBACK! "
							+ "Expect inconsisten data and other problems. " + "Make sure you have a successful full publish before launching any CRSyncs!.");
					NodeConfigRuntimeConfiguration.runtimeLog.info("The feature override_publish_errors should be used only for maintanence purposes and never on productive systems!");
					renderResult.error(Publisher.class, "Publish run failed. Committing nevertheless.", e);
					myPublishInfo.setPublishedFolderCount(0);
					myPublishInfo.setPublishedPageCount(0);
					myPublishInfo.setPublishedFileCount(0);
					myPublishInfo.setPublishedFormCount(0);
					myPublishInfo.setReturnCode(PublishInfo.RETURN_CODE_ERROR);
					myPublishInfo.setError(e);
				}

			} finally {
				// disable statistics
				FilesystemAttributeValue.enableStatistics(false);
				MCCRHelper.enableStatistics(false);
			}

			finalizePhase.done();
			myPublishInfo.done();

			try {
				// If 100 pages were rendered, it is considered representative
				boolean representative = false;

				logger.info("rendered pages: {" + myPublishInfo.getTotalPageRenderCount() + "} - representative page count: {" + representativePageCount + "}");
				if (myPublishInfo.getTotalPageRenderCount() > representativePageCount) {
					representative = true;
				}
				myPublishInfo.finish(representative);
			} catch (NodeException e) {
				logger.error("Error while storing work phase information into database.", e);
			}

			// It makes no sense to commit a transaction in a
			// finally block .. after all .. we only want to commit
			// if the publish run went successfully.
			if (t != null) {
				try {
					// print dependency manager statistics
					DependencyManager.printStatistics(renderResult);

					long precommit = System.currentTimeMillis();

					// commit also dependencymap2 transaction
					DependencyManager.commitPublishTransaction();

					// finalize the publish process
					PublishQueue.finalizePublishProcess();

					t.commit();
					renderResult.info(Publisher.class, "Committed transaction in {" + (System.currentTimeMillis() - precommit) + "} ms.");
				} catch (TransactionException e) {
					throw new NodeException("Error while committing transaction to finalize publish run.", e);
					// TODO update publish run status !
				}
			}
			if (cnMapPublisher != null) {
				cnMapPublisher.commit();
				cnMapPublisher = null;
			}

			renderResult.info(Publisher.class, "Finished publish. Total duration: {" + (System.currentTimeMillis() - startPublish) + " ms}");

		} catch (NodeException e) {
			// remember error for invoker and scheduler
			myPublishInfo.setError(e);
			myPublishInfo.setReturnCode(PublishInfo.RETURN_CODE_ERROR);
			logger.fatal("Error during publish run.", e);
		} finally {
			t.resetPublishData();
			publishData = null;

			PublishableTemplate.clearCache();
			myPublishInfo.done();
			if (loggingRenderResult != null) {
				loggingRenderResult.close();
			}
			RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_RUN);
			RuntimeProfiler.endMark(ComponentsConstants.GENTICSROOT);

			if (publishEntry != null) {
				try {
					t = factory.startTransaction(true);
					publishEntry.delete();
				} catch (NodeException e) {
					logger.error("Error while removing publish entry from dirtqueue", e);
				} finally {
					try {
						t.commit();
					} catch (TransactionException e) {}
				}
			}

			// just to make sure we did not forget any open connections
			if (cnMapPublisher != null) {
				logger.warn("CnMapPublisher was still running (should not be the case for successfull publications), rolling back the transactions.");
				cnMapPublisher.rollback();
				cnMapPublisher = null;
			}

			instantPublished.clear();

			PublishController.setStopped();
			publisherInfo.setRunning(false);
			publisherInfo.setPhase(PublisherPhase.IDLE);
		}

		// TODO when finished, notify the php-backend, so that the scheduler can start subsequent tasks (ftp, rsync,..)

	}

	/**
	 * Mark the given object as instant published.
	 *
	 * During the publish process, {@link #wasInstantPublished}
	 * should be used to determine whether or not to publish
	 * an object, because it was already instant published.
	 *
	 * @see PublishController#instantPublished
	 *
	 * @param object The instant published object.
	 */
	void instantPublished(NodeObject object) {
		if (logger.isInfoEnabled()) {
			logger.info("Adding " + object + " to instant published objects");
		}

		if (object != null) {
			instantPublished.add(String.format("%d.%d", object.getTType(), object.getId()));
		}
	}

	/**
	 * Revoke a prior call to {@link #instantPublished}, because
	 * the actual instant publication failed.
	 *
	 * @see PublishController#instantPublishFailed
	 *
	 * @param object The object that could not be instant published.
	 */
	void instantPublishFailed(NodeObject object) {
		if (logger.isInfoEnabled()) {
			logger.info("Removing " + object
				+ " from instant published objects because instant publishing failed");
		}

		if (object != null) {
			instantPublished.remove(String.format("%d.%d", object.getTType(), object.getId()));
		}
	}

	/**
	 * Check whether the given object was instant published
	 * since the current publish run started.
	 *
	 * @see PublishController#wasInstantPublished
	 *
	 * @param object The object to check.
	 * @return <code>true</code> if the object was instant
	 *		published since the current publish run started,
	 *		and <code>false</code> otherwise.
	 */
	boolean wasInstantPublished(NodeObject object) {
		if (object == null) {
			return false;
		}
		return instantPublished.contains(String.format("%d.%d", object.getTType(), object.getId()));
	}

	/**
	 * Check whether the given object was instant published
	 * since the current publish run started.
	 *
	 * @see PublishController#wasInstantPublished
	 *
	 * @param objType The object type
	 * @param objId object ID
	 * @return <code>true</code> if the object was instant
	 *		published since the current publish run started,
	 *		and <code>false</code> otherwise.
	 */
	boolean wasInstantPublished(int objType, int objId) {
		return instantPublished.contains(String.format("%d.%d", objType, objId));
	}

	private void generateWorkPhases() {
		timeManagementPhase = new CNWorkPhase(myPublishInfo, "timemanagement", PublishWorkPhaseConstants.PHASE_NAME_TIMEMANAGEMENT);
		timeManagementPhase.addWork(1);
		initPhase = new CNWorkPhase(myPublishInfo, "init", PublishWorkPhaseConstants.PHASE_NAME_INITIALIZATION);
		initPhase.addWork(1);
		waitForDirtEventsPhase = new CNWorkPhase(myPublishInfo, "waitForDirtEvents", PublishWorkPhaseConstants.PHASE_NAME_WAIT_FOR_DIRTEVENTS);
		contentMapPublishPhase = new CNWorkPhase(myPublishInfo, "cnMapPublisher", PublishWorkPhaseConstants.PHASE_NAME_CNMAP_PUBLISH);
		publishPagePhase = new CNWorkPhase(myPublishInfo, "publishPages", PublishWorkPhaseConstants.PHASE_NAME_RENDER_PAGES);

		checkOfflineFilesPhase = new CNWorkPhase(myPublishInfo, "checkOfflineFiles", PublishWorkPhaseConstants.PHASE_NAME_CHECK_OFFLINE_FILE_DEPENDENCIES);
		checkOnlineFilesPhase = new CNWorkPhase(myPublishInfo, "checkOnlineFiles", PublishWorkPhaseConstants.PHASE_NAME_CHECK_ONLINE_FILE_DEPENDENCIES);

		writeFsPhase = new CNWorkPhase(myPublishInfo, "writefs", PublishWorkPhaseConstants.PHASE_NAME_WRITEFS);
		writeFsPagesAndFilesPhase = new CNWorkPhase(writeFsPhase, "writeFsPagesAndFiles", PublishWorkPhaseConstants.PHASE_NAME_WRITEFS_PAGES_FILES);
		writeFsImageStorePhase = new CNWorkPhase(writeFsPhase, "writeFsImageStorePhase", PublishWorkPhaseConstants.PHASE_NAME_WRITEFS_IMAGE_STORE);

		cnMapPublisher = new CnMapPublisher(getConfiguration(), myPublishInfo);
		cnMapPublisher.generateWorkPhases(contentMapPublishPhase);

		finalizePhase = new CNWorkPhase(myPublishInfo, "finalizing", PublishWorkPhaseConstants.PHASE_NAME_FINALIZING);
	}

	/**
	 * Do the timemanagement in a new transaction
	 * @param prefs node preferences
	 * @throws NodeException
	 */
	protected void doTimeManagement(NodePreferences prefs) throws NodeException {
		Transaction t = null;
		timeManagementPhase.begin();
		try {
			t = factory.startTransaction(true);
			t.setTimestamp(timestamp);
			RenderType renderType = RenderType.getDefaultRenderType(getConfiguration().getDefaultPreferences(), RenderType.EM_PUBLISH, null, -1);

			renderType.setRenderUrlFactory(new StaticUrlFactory(RenderType.parseLinkWay(prefs.getProperty("contentnode.linkway")), RenderType
					.parseLinkWay(prefs.getProperty("contentnode.linkway_file")), prefs.getProperty("contentnode.linkway_file_path")));
			t.setRenderType(renderType);
			t.setRenderResult(renderResult);
			TransactionManager.setCurrentTransaction(t);

			// Do Timemanagement
			if (renderResult != null) {
				renderResult.info(Publisher.class, "Processing time management");
			}
			PagePublisher.doTimeManagement(factory);
			FormFactory.doTimeManagement();
			if (renderResult != null) {
				renderResult.info(Publisher.class, "Finished processing time management");
			}

		} finally {
			t.commit();
			timeManagementPhase.done();
		}
	}

	/**
	 * Initialize the publish work phases for the list of published nodes
	 * @param publishedNodes list of published nodes
	 * @param meshPublishController mesh publish controller
	 * @return A Map containing counters for folders, pages
	 *		and files which are to be published. The keys of the
	 *		Map are the numeric object codes.
	 * @throws NodeException
	 * @throws GenericFailureException
	 */
	private Map<Integer, Integer> initializeWorkPhases(List<Node> publishedNodes, MeshPublishController meshPublishController)
			throws NodeException, GenericFailureException {
		Map<Integer, Map<Integer, Integer>> objectsToPublishCount = new HashMap<>();

		initPhase.begin();

		try {
			if (renderResult != null) {
				renderResult.info(Publisher.class, "Marking changes about to be published in this run");
			}
			objectsToPublishCount = PublishQueue.startPublishProcess(publishedNodes);

			// set the number of objects to publish into the JMX bean
			for (Map.Entry<Integer, Map<Integer, Integer>> entry : objectsToPublishCount.entrySet()) {
				int nodeId = entry.getKey();
				Map<Integer, Integer> nodeCounts = entry.getValue();

				MBeanRegistry.getPublisherInfo().setObjectsToPublish(nodeId, nodeCounts.getOrDefault(Page.TYPE_PAGE, 0),
						nodeCounts.getOrDefault(com.gentics.contentnode.object.File.TYPE_FILE, 0),
						nodeCounts.getOrDefault(Folder.TYPE_FOLDER, 0), nodeCounts.getOrDefault(Form.TYPE_FORM, 0));
			}

			meshPublishController.initializeWorkPhases();

			// Contentmap Publisher
			try {
				cnMapPublisher.init(renderResult);
				cnMapPublisher.initializeWorkPhases();

				// initPhase.doneWork();
			} catch (NodeException e1) {
				throw new GenericFailureException("Error while initializing contentmap publisher: " + e1.getLocalizedMessage(), e1);
			}

			// This must be called AFTER startPublishProcess(), because it relies
			// on that all pages it should touch already have the publish_flag set
			initializePagePublisher();
			// initPhase.doneWork();

			writeFsPhase.addWork(1);
			initWriteFs();

			finalizePhase.addWork(1);

			myPublishInfo.init();
		} finally {
			initPhase.done();
		}

		Map<Integer, Integer> totalCounts = new HashMap<>();
		for (Integer type : Arrays.asList(Folder.TYPE_FOLDER_INTEGER, Page.TYPE_PAGE_INTEGER,
				com.gentics.contentnode.object.File.TYPE_FILE_INTEGER, Form.TYPE_FORM)) {
			totalCounts.put(type, objectsToPublishCount.values().stream()
					.map(nodeCounts -> nodeCounts.getOrDefault(type, 0)).reduce(0, Integer::sum));
		}

		return totalCounts;
	}

	/**
	 * executes a given command and reads it's output and error output. On any
	 * error (including exit status != 0) an exception is raised.
	 *
	 * @param cmdarray the command to execute
	 * @return standard output
	 * @throws PublishException Thrown if any error is encountered. Caller
	 *         should catch exception and add the command. (The command is not
	 *         logged because
	 */
	protected ExecResult readExecResult(String[] cmdarray) throws PublishException {
		try {
			Process p = Runtime.getRuntime().exec(cmdarray);

			// Attach our readers to stdout and error stream.
			InputStreamReaderRunnable inputReader = new ObjectTransformer.InputStreamReaderRunnable(p.getInputStream());
			InputStreamReaderRunnable errorReader = new ObjectTransformer.InputStreamReaderRunnable(p.getErrorStream());
			Thread inputReaderThread = new Thread(inputReader);
			Thread errorReaderThread = new Thread(errorReader);

			inputReaderThread.start();
			errorReaderThread.start();

			int r = p.waitFor();

			inputReaderThread.join();
			errorReaderThread.join();

			if (r != 0) {
				// We do not log the command here, because it might contain passwords.
				throw new PublishException(
						"Command did not return exit status 0. stdout {" + inputReader.getString() + "} error {" + errorReader.getString() + "} exit status {" + r
						+ "}");
			}

			return new ExecResult(r, inputReader.getString(), errorReader.getString());
		} catch (Throwable e) {
			if (e instanceof PublishException) {
				throw (PublishException) e;
			}
			throw new PublishException("Encountered problem while executing command.", e);
		}
	}

	/**
	 * @param command
	 * @return standard output
	 * @throws PublishException
	 * @see #readExecResult(String[])
	 */
	protected ExecResult readExecResult(String command) throws PublishException {
		StringTokenizer st = new StringTokenizer(command);
		String[] cmdarray = new String[st.countTokens()];

		for (int i = 0; st.hasMoreTokens(); i++) {
			cmdarray[i] = st.nextToken();
		}
		return readExecResult(cmdarray);
	}

	/**
	 * execute FTP sync shell command
	 * windows compatibility is ignored, afaik not even scheduled for later implementation
	 * @param host to be synced
	 * @param pubdir to be synced (naming changed to satisfy checkstyle)
	 * @param ftphost target host for sync
	 * @param ftplogin user login
	 * @param ftppassword user password
	 * @param ftpwwwroot target directory
	 * @return true on success
	 * @throws NodeException
	 */
	protected boolean execFtpSync(RenderResult renderResult, String host, String pubdir, String ftphost, String ftplogin,
			String ftppassword, String ftpwwwroot) throws NodeException {
		String[] cmdarray = new String[6];

		cmdarray[0] = lftpPath;
		cmdarray[1] = "-u";
		cmdarray[2] = ftplogin + "," + ftppassword;
		cmdarray[3] = "-e";
		cmdarray[4] = "set dns:fatal-timeout 60" + ";set net:timeout 60" // value of 2 retries once in older versions, twice in newer versions
				+ ";set net:max-retries 2" + ";set net:reconnect-interval-base 20" + ";set net:reconnect-interval-max 20" + ";set net:reconnect-interval-multiplier 1"
				+ ";mirror -R -L -e " + publishDir.getAbsolutePath() + "/pub/" + host + pubdir + " " + ftpwwwroot + pubdir + ";exit";
		cmdarray[5] = ftphost;

		// make sure there are no passwords in the log files
		String[] copyarr = (String[]) copyArray(cmdarray, new String[cmdarray.length]);

		copyarr[2] = ftplogin + ",*PASSWORD HIDDEN*";
		String debugcmd = StringUtils.merge(copyarr, " ", "\"", "\"");

		renderResult.info(Publisher.class, "executing FTP sync: {" + debugcmd + "}");
		try {
			ExecResult execResult = readExecResult(cmdarray);

			renderResult.info(Publisher.class, "executed FTP sync. " + execResult.toString());
		} catch (PublishException e) {
			throw new PublishException("Error while executing ftp sync command {" + debugcmd + "}.", e);
		}
		return true;
	}

	private Object[] copyArray(Object[] array, Object[] dest) {
		Object[] ret = dest;

		for (int i = 0; i < array.length; i++) {
			ret[i] = array[i];
		}
		return ret;
	}

	/**
	 * get a cummulative info-object which contains the status of the main publish thread.
	 * @return a publishinfo which contains the current main status.
	 */
	public PublishInfo getPublishInfo() {
		return myPublishInfo;
	}

	/**
	 * get the configuration used by this publisher.
	 * @return the current configuration.
	 */
	public NodeConfig getConfiguration() {
		return NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();
	}

	private CnMapPublisher updateContentMaps() throws GenericFailureException {

		// TODO create new publishstatus and give it to publisher
		try {
			contentMapPublishPhase.begin();
			cnMapPublisher.publishObjects(renderResult);
			contentMapPublishPhase.done();

			// set all files/folders to be published
			PublisherInfo publisherInfo = MBeanRegistry.getPublisherInfo();

			publisherInfo.publishedAllFiles();
			publisherInfo.publishedAllFolders();

			return cnMapPublisher;
		} catch (NodeException e) {
			throw new GenericFailureException(e);
		}
	}

	private void initializePagePublisher() throws GenericFailureException {
		pagePublisher = PagePublisher.getPagePublisher(getConfiguration(), cnMapPublisher, factory, publishPagePhase, myPublishInfo);

		try {
			pagePublisher.initialize(lastPublishRun, renderResult);
			int pagesToPublish = PublishQueue.countDirtedObjects(Page.class, true, null);

			myPublishInfo.setTotalPageRenderCount(pagesToPublish);

			// Each page is counted twice, because rerendereing might be required..
			publishPagePhase.addWork(pagesToPublish * 2);
		} catch (NodeException e1) {
			throw new GenericFailureException(e1);
		}
	}

	/**
	 * Check offline files. If files are not marked online, but should be because on of the conditions are true
	 * <ul>
	 * <li>Feature contentfile_auto_offline is off for the Node</li>
	 * <li>The file has the flag "force_online" set</li>
	 * <li>The file is used by other objects</li>
	 * </ul>
	 * The the file is set online and published into the CR.
	 * @param workPhase work phase (optional)
	 * @param meshPublishController mesh publish controller
	 * TODO: logging!
	 * @throws NodeException
	 */
	private void checkOfflineFiles(IWorkPhase workPhase, MeshPublishController meshPublishController) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		// reset the publish queue for files
		cnMapPublisher.resetTaskQueues();

		// get all Nodes that publish either into the filesystem or the contentrepository (or both) and don't have publishing disabled
		final List<Integer> nodeIds = new Vector<Integer>();

		DBUtils.executeStatement("SELECT id FROM node"
				+ " WHERE ((publish_fs = 1 AND publish_fs_files = 1)"
				+ " OR (publish_contentmap = 1 AND publish_contentmap_files = 1))"
				+ " AND disable_publish = 0", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					nodeIds.add(rs.getInt("id"));
				}
			}
		});
		List<Node> nodes = t.getObjects(Node.class, nodeIds);

		// add some work to the work phase
		if (workPhase != null) {
			workPhase.addWork(nodes.size());
		}

		for (Node node : nodes) {
			renderResult.info(Publisher.class, "Start checking offline files for " + node);
			boolean autoOffline = prefs.isFeature(Feature.CONTENTFILE_AUTO_OFFLINE, node);
			@SuppressWarnings("resource")
			MeshPublisher mp = node.doPublishContentMapFiles() ? meshPublishController.get(node) : null;
			Collection<Scheduled> meshPublisherQueue = null;
			if (mp != null) {
				meshPublisherQueue = new ArrayList<>();
			}

			Node master = null;
			List<Node> masters = node.getMasterNodes();

			if (!ObjectTransformer.isEmpty(masters)) {
				master = masters.get(masters.size() - 1);
			} else {
				master = node;
			}
			final Object nodeId = node.getId();
			final Object masterId = master.getId();

			// get all offline master files
			final List<Integer> fileIds = new Vector<Integer>();
			StringBuffer sql = new StringBuffer();

			sql.append("SELECT contentfile.id FROM contentfile ");
			sql.append("LEFT JOIN folder ON contentfile.folder_id = folder.id ");
			sql.append("LEFT JOIN contentfile_online ON contentfile.id = contentfile_online.contentfile_id AND contentfile_online.node_id = ? ");
			sql.append("WHERE contentfile.deleted = 0 AND contentfile.is_master = ? AND folder.node_id = ? AND contentfile_online.node_id IS NULL");
			DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, nodeId);
					stmt.setInt(2, 1);
					stmt.setObject(3, masterId);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						fileIds.add(rs.getInt("id"));
					}
				}
			});
			List<com.gentics.contentnode.object.File> offlineFiles = t.getObjects(com.gentics.contentnode.object.File.class, fileIds);

			renderResult.info(Publisher.class, "Checking " + offlineFiles.size() + " offline files for " + node);
			int offlineFilesPublished = 0;

			// check all files
			for (com.gentics.contentnode.object.File file : offlineFiles) {
				Node fileChannel = file.getChannel();

				if (fileChannel != null && !(fileChannel.equals(node) || node.isChannelOf(fileChannel))) {
					continue;
				}
				com.gentics.contentnode.object.File channelVariant = file.getChannelVariant(node);

				if (channelVariant == null) {
					continue;
				}

				// check forceOnline for the channel specific file
				if (!autoOffline || channelVariant.isForceOnline()) {
					if (logger.isDebugEnabled()) {
						if (!autoOffline) {
							renderResult.debug(Publisher.class, file + " will go online, because feature is OFF for " + node);
						} else {
							renderResult.debug(Publisher.class, file + " is forced online");
						}
					}
					// set online
					offlineFilesPublished++;
					FileOnlineStatus.setOnline(file, node, true);
					if (cnMapPublisher.getContentMap(node, true) != null) {
						cnMapPublisher.addPublishTask(channelVariant, node);
					}
					if (meshPublisherQueue != null) {
						meshPublisherQueue.add(Scheduled.from(node.getId(), new NodeObjectWithAttributes<>(channelVariant)));
					}
				} else {
					if (DependencyManager.isFileUsed(this.fileUsageMap, file, node)) {
						if (logger.isDebugEnabled()) {
							renderResult.debug(Publisher.class, file + " is used by other objects and will go online");
						}
						// found a dependency from another object (non file)
						offlineFilesPublished++;
						FileOnlineStatus.setOnline(file, node, true);
						if (cnMapPublisher.getContentMap(node, true) != null) {
							cnMapPublisher.addPublishTask(channelVariant, node);
						}
						if (meshPublisherQueue != null) {
							meshPublisherQueue.add(Scheduled.from(node.getId(), new NodeObjectWithAttributes<>(channelVariant)));
						}
					}
				}
			}
			renderResult.info(Publisher.class, offlineFilesPublished + " offline files where set online for " + node);

			if (workPhase != null) {
				workPhase.doneWork();
			}

			if (mp != null && !ObjectTransformer.isEmpty(meshPublisherQueue)) {
				mp.processQueue(meshPublisherQueue, node, null, null);
			}

			renderResult.info(Publisher.class, "Finished checking offline files for " + node);
		}

		// publish the files, that have been set online now
		// TODO publish phase
		cnMapPublisher.publishFiles(null);
	}

	/**
	 * Check online files for nodes that have the feature contentfile_auto_offline activated. If a files has force online flag not set and is not used anywhere, remove it
	 * @param workPhase work phase
	 * @param meshPublishController mesh publish controller
	 * @throws NodeException
	 */
	private void checkOnlineFiles(IWorkPhase workPhase, MeshPublishController meshPublishController) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		// get all Nodes that publish either into the filesystem or the contentrepository (or both) and don't have publishing disabled
		final List<Integer> nodeIds = new Vector<Integer>();

		DBUtils.executeStatement("SELECT id FROM node"
				+ " WHERE ((publish_fs = 1 AND publish_fs_files = 1) OR (publish_contentmap = 1 AND publish_contentmap_files = 1))"
				+ " AND disable_publish = 0", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					nodeIds.add(rs.getInt("id"));
				}
			}
		});
		List<Node> nodes = t.getObjects(Node.class, nodeIds);

		// add some work to the workphase
		if (workPhase != null) {
			workPhase.addWork(nodes.size());
		}

		for (Node node : nodes) {
			renderResult.info(Publisher.class, "Starting checking online files for " + node);
			boolean autoOffline = prefs.isFeature(Feature.CONTENTFILE_AUTO_OFFLINE, node);

			Node master = null;
			List<Node> masters = node.getMasterNodes();

			if (!ObjectTransformer.isEmpty(masters)) {
				master = masters.get(masters.size() - 1);
			} else {
				master = node;
			}
			final Integer nodeId = node.getId();
			final Integer masterId = master.getId();

			// get all online master files
			final List<Integer> fileIds = new Vector<Integer>();
			StringBuilder sql = new StringBuilder();

			// check files that are no longer published into the node, for which they are set to be online (because the file was moved into another node)
			sql.append("SELECT contentfile.id FROM contentfile ");
			sql.append("LEFT JOIN folder ON contentfile.folder_id = folder.id ");
			sql.append("LEFT JOIN contentfile_online ON contentfile.id = contentfile_online.contentfile_id AND contentfile_online.node_id = ? ");
			sql.append("WHERE contentfile.deleted = 0 AND contentfile.is_master = ? AND folder.node_id != ? AND contentfile_online.node_id = ?");
			DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, nodeId);
					stmt.setInt(2, 1);
					stmt.setObject(3, masterId);
					stmt.setObject(4, nodeId);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						fileIds.add(rs.getInt("id"));
					}
				}
			});
			if (!fileIds.isEmpty()) {
				boolean singular = fileIds.size() == 1;
				renderResult.info(Publisher.class, "Setting " + fileIds.size() + " " + (singular ? "file" : "files") + " offline for " + node + ", which "
						+ (singular ? "was" : "were") + " apparently moved to another node");
				TransactionManager.execute(new Executable() {
					/* (non-Javadoc)
					 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
					 */
					public void execute() throws NodeException {
						for (Integer fileId : fileIds) {
							FileOnlineStatus.setOnlineStatus(fileId, nodeId, false);
						}
					}
				});
			}

			if (!autoOffline) {
				// when the feature is off for the node, we don't do anything
				renderResult.info(Publisher.class, "Nothing more to check for " + node + " (feature contentfile_auto_offline is OFF)");
				continue;
			}

			sql = new StringBuilder();
			fileIds.clear();
			sql.append("SELECT contentfile.id FROM contentfile ");
			sql.append("LEFT JOIN folder ON contentfile.folder_id = folder.id ");
			sql.append("LEFT JOIN contentfile_online ON contentfile.id = contentfile_online.contentfile_id AND contentfile_online.node_id = ? ");
			sql.append("WHERE contentfile.is_master = ? AND folder.node_id = ? AND contentfile_online.node_id = ?");
			DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, nodeId);
					stmt.setInt(2, 1);
					stmt.setObject(3, masterId);
					stmt.setObject(4, nodeId);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						fileIds.add(rs.getInt("id"));
					}
				}
			});
			List<com.gentics.contentnode.object.File> onlineFiles = t.getObjects(com.gentics.contentnode.object.File.class, fileIds);

			// get the contentmap, if the node publishes into one
			ContentMap contentMap = node.doPublishContentMapFiles() ? cnMapPublisher.getContentMap(node, true) : null;
			@SuppressWarnings("resource")
			MeshPublisher mp = node.doPublishContentMapFiles() ? meshPublishController.get(node) : null;
			List<NodeObject> filesToDelete = null;

			// only create the list of files to delete from the CR, if the node publishes into a cr
			if (contentMap != null || mp != null) {
				filesToDelete = new ArrayList<NodeObject>();
			}

			renderResult.info(Publisher.class, "Will check " + onlineFiles.size() + " online files for " + node);
			// check all files
			for (com.gentics.contentnode.object.File file : onlineFiles) {
				com.gentics.contentnode.object.File channelVariant = file.getChannelVariant(node);

				// if no channel variant exists, the file is not visible in the current channel
				if (channelVariant == null) {
					if (logger.isDebugEnabled()) {
						renderResult.debug(Publisher.class, file + " is not visible and will go offline");
					}

					FileOnlineStatus.setOnline(file, node, false);
					// add the file to the list of files to delete, if the node publishes into a cr
					if (filesToDelete != null) {
						filesToDelete.add(file);
					}
					continue;
				}

				// check forceOnline for the channel specific file
				if (channelVariant.isForceOnline()) {
					if (logger.isDebugEnabled()) {
						renderResult.debug(Publisher.class, file + " is forced online");
					}
					continue;
				}

				if (!DependencyManager.isFileUsed(this.fileUsageMap, file, node)) {
					if (logger.isDebugEnabled()) {
						renderResult.debug(Publisher.class, file + " is not used and will go offline");
					}

					// found no dependency from another object (non file)
					FileOnlineStatus.setOnline(file, node, false);
					// add the file to the list of files to delete, if the node publishes into a cr
					if (filesToDelete != null) {
						filesToDelete.add(channelVariant);
					}
				} else {
					if (logger.isDebugEnabled()) {
						renderResult.debug(Publisher.class, file + " is still used and will remain online");
					}
				}
			}

			// now delete the files from the contentrepository
			if (!ObjectTransformer.isEmpty(filesToDelete)) {
				if (contentMap != null) {
					renderResult.info(Publisher.class, "Deleting " + filesToDelete.size() + " files of from contentrepository " + contentMap);
					cnMapPublisher.deleteObjects(contentMap, filesToDelete, node);
				}

				if (mp != null) {
					renderResult.info(Publisher.class, "Deleting " + filesToDelete.size() + " files of from contentrepository " + mp.getCr());
					mp.remove(node, filesToDelete);
				}
			}

			if (workPhase != null) {
				workPhase.doneWork();
			}

			renderResult.info(Publisher.class, "Finished checking online files for " + node);
		}
	}

	/**
	 * Prepare the file usage map
	 * @throws NodeException
	 */
	private void prepareFileUsageMap() throws NodeException {
		renderResult.info(Publisher.class, "Prepare file usage data");
		fileUsageMap = DependencyManager.getFileUsageMap();
		renderResult.info(Publisher.class, "Finished preparing file usage data");
	}

	/**
	 * Remove the file usage map
	 */
	private void removeFileUsageMap() {
		fileUsageMap = null;
	}



	private void initWriteFs() throws NodeException {
		filePublisher = new FilePublisher(getConfiguration(), renderResult);

		int workUnits = filePublisher.getPagesToWrite() + filePublisher.getFilesToWrite();

		writeFsPagesAndFilesPhase.addWork(workUnits);

	}

	private boolean writeFs() throws NodeException {

		// TODO create new publishstatus and give it to publisher

		if (logger.isDebugEnabled()) {
			logger.debug("beginning writefs with encoding '" + System.getProperty("file.encoding") + "'");
		}

		publishDir = filePublisher.getPublishDir();

		try {
			if (!filePublisher.initializeWriter()) {
				return false;
			}

			MBeanRegistry.getPublisherInfo().setPhase(PublisherPhase.FILESYSTEM);
			writeFsPagesAndFilesPhase.begin();

			RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_WRITEFS_PAGES);
			filePublisher.writePages(writeFsPagesAndFilesPhase, cnMapPublisher);
			RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_WRITEFS_PAGES);
			RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_WRITEFS_FILES);
			filePublisher.writeFiles(writeFsPagesAndFilesPhase, cnMapPublisher);
			RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_WRITEFS_FILES);

			writeFsPagesAndFilesPhase.done();

			MBeanRegistry.getPublisherInfo().setPhase(PublisherPhase.GENTICSIMAGESTORE);
			writeFsImageStorePhase.begin();
			RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER);
			filePublisher.writeTagImageResizer(writeFsImageStorePhase, cnMapPublisher);
			RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER);
			writeFsImageStorePhase.done();

			filePublisher.finalizeWriter(true);
		} catch (Throwable e) {
			logger.fatal("Error during writefs", e);
			filePublisher.finalizeWriter(false);
			if (e instanceof NodeException) {
				throw (NodeException) e;
			} else {
				throw new NodeException("Error during writefs. " + e.getLocalizedMessage(), e);
			}
		}
		return true;
	}

	public RenderResult getRenderResult() {
		return this.renderResult;
	}

	/**
	 * Sets the start timestamp (php's $TIME)
	 * @param timestamp
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Get the log file for this publish process
	 * @param verbose true to get the verbose log file
	 * @return log file (may be null if not yet created)
	 */
	public File getLogFile(boolean verbose) {
		return verbose ? logfileVerbose : logfile;
	}

	/**
	 * Get prepared publish data or null, if not prepared
	 * @return prepared publish data
	 */
	public PublishData getPublishData() {
		return publishData;
	}

	/**
	 * Set the given error, if it has not been set before (capture the first error that occurred)
	 * @param error error
	 */
	public void setError(Throwable error) {
		if (this.error == null && error != null) {
			this.error = error;
		}
	}

	public static class ExecResult {
		public int exitStatus;
		public String stdout;
		public String stderr;

		public ExecResult(int exitStatus, String stdout, String stderr) {
			this.exitStatus = exitStatus;
			this.stdout = stdout;
			this.stderr = stderr;
		}

		public String toString() {
			return "ExitStatus {" + exitStatus + "} Stdout {" + stdout + "} Stderr {" + stderr + "}";
		}
	}
}

