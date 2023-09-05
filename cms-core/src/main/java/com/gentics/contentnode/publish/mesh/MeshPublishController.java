package com.gentics.contentnode.publish.mesh;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.PrefixedThreadFactory;
import com.gentics.contentnode.etc.TaskQueueSize;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.publish.CNWorkPhase;
import com.gentics.contentnode.publish.PublishController;
import com.gentics.contentnode.publish.PublishWorkPhaseConstants;
import com.gentics.contentnode.publish.SimplePublishInfo;
import com.gentics.contentnode.publish.WorkPhaseHandler;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.mesh.MeshStatus;

/**
 * Mesh Publish Controller
 */
public class MeshPublishController extends StandardMBean implements AutoCloseable, MeshPublishControllerInfoMBean {
	/**
	 * Executor thread pool for handling the task queue to write objects into Mesh
	 */
	protected ExecutorService writerThreadPool;

	/**
	 * Executor thread pool for rendering
	 */
	protected ExecutorService rendererThreadPool;

	/**
	 * Queue for write tasks
	 */
	private BlockingQueue<AbstractWriteTask> taskQueue = new ArrayBlockingQueue<>(MeshPublisher.TASKQUEUE_SIZE);

	/**
	 * Set of mesh publishers
	 */
	protected Set<MeshPublisher> publishers = new HashSet<>();

	/**
	 * Future that will return the number of completed tasks, when the {@link MeshPublisher#taskQueue} was handled completely
	 */
	protected Future<Integer> taskCounterFuture;

	/**
	 * Flag to mark whether the publish process succeeded
	 */
	protected boolean success = false;

	/**
	 * JMX Bean
	 */
	protected MeshPublishControllerInfoMBean meshPublisherInfo;

	/**
	 * Flag for controller that is used for the publish process (and not instant publishing)
	 */
	protected boolean publishProcess;

	/**
	 * Publish Info
	 */
	protected SimplePublishInfo publishInfo;

	/**
	 * Mesh Workphase
	 */
	protected IWorkPhase meshWorkPhase;

	/**
	 * Init workphase
	 */
	protected IWorkPhase initWorkPhase;

	/**
	 * Wait workphase
	 */
	protected IWorkPhase waitWorkPhase;

	/**
	 * Folders and files workphase
	 */
	protected IWorkPhase foldersAndFilesPhase;

	/**
	 * Postponed workphase
	 */
	protected IWorkPhase postponedPhase;

	/**
	 * Offline workphase
	 */
	protected IWorkPhase offlinePhase;

	/**
	 * State of the publish controller
	 */
	protected State state = State.init;

	/**
	 * Tracker for the render tasks
	 */
	protected TaskQueueSize renderTasks = new TaskQueueSize(MeshPublisher.TASKQUEUE_SIZE * 2);

	/**
	 * Tracker for the write tasks
	 */
	protected TaskQueueSize writeTasks = new TaskQueueSize();

	/**
	 * Get an instance of the Mesh Publish Controller containing MeshPublishers for all Mesh CRs that are assigned to Nodes that do not have publishing disabled
	 * @param publishInfo publish info
	 * @return MeshPublishController instance
	 * @throws NodeException
	 */
	@SuppressWarnings("resource")
	public static MeshPublishController get(SimplePublishInfo publishInfo) throws NodeException {
		MeshPublishController controller;
		try {
			controller = new MeshPublishController();
		} catch (NotCompliantMBeanException e) {
			throw new NodeException(e);
		}

		controller.publishInfo = publishInfo;
		Transaction t = TransactionManager.getCurrentTransaction();
		List<ContentRepository> contentRepositories = t.getObjects(ContentRepository.class,
				DBUtils.select("SELECT id FROM contentrepository WHERE crtype = ?", ps -> ps.setString(1, Type.mesh.toString()), DBUtils.IDS));

		for (ContentRepository cr : contentRepositories) {
			if (cr.getNodes().stream().filter(n -> !n.isPublishDisabled() && n.doPublishContentmap()).findFirst().isPresent()) {
				try {
					MeshPublisher mp = new MeshPublisher(cr);
					mp.controller = controller;
					mp.publishInfo = publishInfo;
					controller.publishers.add(mp);
				} catch (NodeException e) {
					// in case of an error, close all mesh publishers that were successfully initialized
					controller.publishers.forEach(MeshPublisher::close);
					throw e;
				} catch (Throwable e) {
					// in case of an error, close all mesh publishers that were successfully initialized
					controller.publishers.forEach(MeshPublisher::close);
					throw new NodeException(String.format("Error while initializing MeshPublisher for %s", cr.getName()), e);
				}
			}
		}

		int rendererThreadPoolSize = ObjectTransformer.getInt(t.getRenderType().getPreferences().getProperty("contentnode.config.loadbalancing.threadlimit"),
				MeshPublisher.RENDERERPOOL_SIZE);
		controller.init(rendererThreadPoolSize, true);

		return controller;
	}

	/**
	 * Get an instance for instant publishing into the CR
	 * @param cr CR
	 * @return instance
	 * @throws NodeException
	 */
	public static MeshPublishController get(ContentRepository cr) throws NodeException {
		MeshPublishController controller;
		try {
			controller = new MeshPublishController();
		} catch (NotCompliantMBeanException e) {
			throw new NodeException(e);
		}

		MeshPublisher mp = new MeshPublisher(cr);
		mp.controller = controller;
		controller.publishers.add(mp);
		controller.init(1, false);

		return controller;
	}

	/**
	 * Create an instance
	 * @throws NotCompliantMBeanException
	 */
	protected MeshPublishController() throws NotCompliantMBeanException {
		super(MeshPublishControllerInfoMBean.class);
	}

	/**
	 * Initialize.
	 * This will submit a callable to the {@link #writerThreadPool} that handles all write tasks
	 * @param rendererThreadPoolSize size of the renderer thread pool
	 * @param publishProcess true when the controller is used for the publish process
	 */
	protected void init(int rendererThreadPoolSize, boolean publishProcess) {
		this.publishProcess = publishProcess;
		if (publishProcess) {
			MBeanRegistry.registerMBean(this, "Publish", "MeshPublishController");
		}

		rendererThreadPool = Executors.newFixedThreadPool(rendererThreadPoolSize, new PrefixedThreadFactory("Mesh Renderer"));
		writerThreadPool = Executors.newCachedThreadPool(new PrefixedThreadFactory("Mesh Writer"));
		taskCounterFuture = writerThreadPool.submit(() -> {
			int taskCounter = 0;
			boolean proceed = true;
			while (proceed) {
				try {
					AbstractWriteTask writeTask = taskQueue.take();
					if (publishProcess && (PublishController.getState() != PublishController.State.running)) {
						MeshPublisher.logger.debug(String.format("Stop processing write tasks, because publisher state is %s", PublishController.getState()));
						proceed = false;
					} else if (writeTask == MeshPublisher.END) {
						MeshPublisher.logger.info("Found End Task, Stop taking Tasks from Task Queue");
						proceed = false;
					} else {
						taskCounter++;
						MeshPublisher.logger.debug(String.format("Performing Task [%s]", writeTask));
						writeTask.perform();
						writeTasks.finish();
					}
				} catch (Throwable e) {
					MeshPublisher.logger.error("Error while publishing", e);
					proceed = false;
					if (publishProcess) {
						PublishController.setError(e);
					}
				}
			}

			return taskCounter;
		});
	}

	/**
	 * Either run the render task synchronously (for instant publishing), or asynchonously (for publish process)
	 * @param renderTask render task
	 * @throws NodeException 
	 */
	protected void runRenderTask(Runnable renderTask) throws NodeException {
		if (publishProcess) {
			try {
				renderTasks.awaitNotFull();
			} catch (InterruptedException e) {
				throw new NodeException("Error while waiting for a place in the render task queue", e);
			}
			renderTasks.schedule();
			rendererThreadPool.submit(renderTask);
		} else {
			renderTask.run();
		}
	}

	/**
	 * Initialize the work phases
	 * @throws NodeException
	 */
	public void initializeWorkPhases() throws NodeException {
		meshWorkPhase = new CNWorkPhase(publishInfo, "mesh", PublishWorkPhaseConstants.PHASE_NAME_MESH_PUBLISH);
		initWorkPhase = new CNWorkPhase(meshWorkPhase, "mesh.init", PublishWorkPhaseConstants.PHASE_NAME_MESH_INIT);
		waitWorkPhase = new CNWorkPhase(meshWorkPhase, "mesh.wait", PublishWorkPhaseConstants.PHASE_NAME_MESH_WAIT);
		foldersAndFilesPhase = new CNWorkPhase(meshWorkPhase, "mesh.foldersfiles", PublishWorkPhaseConstants.PHASE_NAME_MESH_FOLDERS_FILES);
		postponedPhase = new CNWorkPhase(meshWorkPhase, "mesh.postponed", PublishWorkPhaseConstants.PHASE_NAME_MESH_POSTPONED);
		offlinePhase = new CNWorkPhase(meshWorkPhase, "mesh.offline", PublishWorkPhaseConstants.PHASE_NAME_MESH_OFFLINE);

		initWorkPhase.addWork(publishers.size());
		waitWorkPhase.addWork(publishers.size());
		offlinePhase.addWork(publishers.size());

		// set the counts of dirted objects
		for (MeshPublisher mp : publishers) {
			publishInfo.incFolderRenderCount(mp.getNumDirtedFolders());
			publishInfo.incFileRenderCount(mp.getNumDirtedFiles());
			publishInfo.incFormRenderCount(mp.getNumDirtedForms());
		}

		foldersAndFilesPhase.addWork(publishInfo.getRemainingFolderCount());
		foldersAndFilesPhase.addWork(publishInfo.getRemainingFileCount());
		foldersAndFilesPhase.addWork(publishInfo.getRemainingFormCount());
	}

	/**
	 * Get the set of {@link MeshPublisher} instances
	 * @return set of instances
	 */
	public Set<MeshPublisher> get() {
		return publishers;
	}

	/**
	 * Get the {@link MeshPublisher} instance for the node or null, if the Node does not publish into Mesh
	 * @param node node
	 * @return instance or null
	 * @throws NodeException
	 */
	public MeshPublisher get(Node node) throws NodeException {
		for (MeshPublisher mp : publishers) {
			if (mp.cr.getNodes().contains(node)) {
				return mp;
			}
		}
		return null;
	}

	/**
	 * Begin the mesh workphase (if not null)
	 */
	public void begin() {
		if (meshWorkPhase != null) {
			meshWorkPhase.begin();
		}
	}

	/**
	 * Check the existence and validity of schemas and projects of all mesh publishers (with repairing)
	 * @throws NodeException
	 */
	public void checkSchemasAndProjects() throws NodeException {
		state = State.checkSchemas;
		try (WorkPhaseHandler phase = new WorkPhaseHandler(initWorkPhase)) {
			for (MeshPublisher mp : publishers) {
				MeshStatus status = mp.getStatus();
				if (status != MeshStatus.READY) {
					throw new NodeException(String.format("Mesh status for %s is %s", mp.getCr().getName(), status));
				}
				if (!mp.checkSchemasAndProjects(true, true)) {
					throw new NodeException(String.format("%s is not valid", mp.getCr().getName()));
				}
				phase.work();
			}
		}
	}

	/**
	 *  Wait for possibly running schema/node migrations
	 * @throws NodeException
	 */
	public void waitForMigrations() throws NodeException {
		state = State.waitForMigrations;
		try (WorkPhaseHandler phase = new WorkPhaseHandler(waitWorkPhase)) {
			for (MeshPublisher mp : publishers) {
				mp.triggerJobProcessing();
				mp.waitForSchemaMigrations();
				mp.waitForNodeMigrations();
				phase.work();
			}
		}
	}

	/**
	 * Publish folders and files
	 * @throws NodeException
	 */
	public void publishFoldersAndFiles() throws NodeException {
		state = State.publishFoldersAndFiles;
		try (WorkPhaseHandler phase = new WorkPhaseHandler(foldersAndFilesPhase)) {
			for (MeshPublisher mp : publishers) {
				mp.publishFoldersAndFiles(phase);
			}
		}
	}

	/**
	 * Set the state to publishPages
	 * @throws NodeException
	 */
	public void publishPages() throws NodeException {
		state = State.publishPages;
		for (MeshPublisher mp : publishers) {
			mp.info(String.format("Publish pages into '%s'", mp.getCr().getName()));
		}
	}

	/**
	 * Set the statue to checkOfflineFiles
	 * @throws NodeException
	 */
	public void checkOfflineFiles() throws NodeException {
		state = State.checkOfflineFiles;
		for (MeshPublisher mp : publishers) {
			mp.info(String.format("Check offline files for '%s'", mp.getCr().getName()));
		}
	}

	/**
	 * Remove offline objects
	 * @throws NodeException
	 */
	public void removeOfflineObjects() throws NodeException {
		state = State.removeOfflineObjects;
		try (WorkPhaseHandler phase = new WorkPhaseHandler(offlinePhase)) {
			for (MeshPublisher mp : publishers) {
				mp.checkObjectConsistency(true, true);
				phase.work();
			}
		}
	}

	/**
	 * Handle postponed updates
	 * @throws NodeException
	 */
	public void handlePostponedUpdates() throws NodeException {
		state = State.handlePostponedTasks;
		if (postponedPhase != null) {
			for (MeshPublisher mp : publishers) {
				postponedPhase.addWork(mp.postponedTasks.size());
			}
		}
		try (WorkPhaseHandler phase = new WorkPhaseHandler(postponedPhase)) {
			for (MeshPublisher mp : publishers) {
				mp.handlePostponedUpdates(phase);
			}
		}
	}

	/**
	 * Wait until everything, which was scheduled so far, is rendered and written
	 * @throws NodeException
	 */
	public void waitForRenderAndWrite() throws NodeException, InterruptedException {
		for (MeshPublisher meshPublisher : publishers) {
			meshPublisher.checkForErrors();
		}
		renderTasks.awaitEmpty();
		writeTasks.awaitEmpty();
	}

	/**
	 * Set the success flag
	 */
	public void success() {
		MeshPublisher.logger.info("Set success");
		success = true;
		if (meshWorkPhase != null) {
			meshWorkPhase.done();
		}
	}

	/**
	 * Put a write task into the queue
	 * @param task write task
	 * @throws NodeException
	 */
	public void putWriteTask(AbstractWriteTask task) throws NodeException {
		try {
			writeTasks.schedule();
			taskQueue.put(task);
		} catch (InterruptedException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Wait for the renderer to finish all its tasks and put the END task to the task queue
	 * @param clearTaskQueue true to clear the task queue first (because an error happened)
	 * @throws InterruptedException
	 */
	public void putEndTask(boolean clearTaskQueue) throws InterruptedException {
		if (clearTaskQueue) {
			taskQueue.clear();
		}
		MeshPublisher.logger.info("Stopping renderer Thread pool");
		state = State.waitForRenderers;
		rendererThreadPool.shutdown();
		try {
			while (!rendererThreadPool.awaitTermination(MeshPublisher.POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)) {
				if (publishProcess && (PublishController.getState() != PublishController.State.running)) {
					MeshPublisher.logger.debug(String.format("Stop waiting for the renderer Thread pool, because publisher state is %s", PublishController.getState()));
					rendererThreadPool.shutdownNow();
					break;
				}
				MeshPublisher.logger.debug("Still waiting for the renderer Thread pool to stop");
			}
		} catch (InterruptedException e) {
			rendererThreadPool.shutdownNow();
		} finally {
			rendererThreadPool = null;
			MeshPublisher.logger.info("Stopped renderer Thread pool");
			MeshPublisher.logger.info("Putting End Task to Task Queue");
			taskQueue.put(MeshPublisher.END);
		}
	}

	@Override
	public void close() throws NodeException {
		try {
			if (publishProcess && (PublishController.getState() != PublishController.State.running)) {
				MeshPublisher.logger.debug(String.format("Stop waiting for the renderer Thread pool, because publisher state is %s", PublishController.getState()));
				rendererThreadPool.shutdownNow();
			} else {
				putEndTask(!success);
				if (taskCounterFuture != null) {
					state = State.waitForWriteTasks;
					taskCounterFuture.get();
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new NodeException(e);
		} finally {
			state = State.done;
			if (writerThreadPool != null) {
				writerThreadPool.shutdownNow();
			}
			taskQueue.clear();
			publishers.forEach(MeshPublisher::close);

			if (publishProcess) {
				MBeanRegistry.unregisterMBean("Publish", "MeshPublishController");
			}
		}

		for (MeshPublisher meshPublisher : publishers) {
			meshPublisher.checkForErrors();
		}
	}

	@Override
	public int getWriteTaskQueueSize() {
		return taskQueue != null ? taskQueue.size() : 0;
	}

	@Override
	public int getPostponedWriteTasks() {
		return publishers.stream().mapToInt(mp -> mp.postponedTasks.size()).sum();
	}

	@Override
	public int getRemainingWriteTasks() {
		return writeTasks.getRemainingTasks();
	}

	@Override
	public int getTotalWriteTasks() {
		return writeTasks.getTotalTasks();
	}

	@Override
	public int getRemainingRenderTasks() {
		return renderTasks.getRemainingTasks();
	}

	@Override
	public int getTotalRenderTasks() {
		return renderTasks.getTotalTasks();
	}

	@Override
	public String getState() {
		return state.name();
	}

	/**
	 * Check whether the controller is still busy (either render or write tasks still open)
	 * @return true for busy
	 */
	public boolean isBusy() {
		return renderTasks.isBusy() || writeTasks.isBusy();
	}

	/**
	 * States of the publish controller
	 */
	public static enum State {
		init, checkSchemas, waitForMigrations, publishFoldersAndFiles, publishPages, checkOfflineFiles, handlePostponedTasks, removeOfflineObjects, waitForRenderers, waitForWriteTasks, done
	}
}
