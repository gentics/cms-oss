package com.gentics.contentnode.devtools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.model.AbstractCRModel;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.distributed.TrxCallable;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.etc.QueueWithDelay;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.AbstractModel;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Synchronization implementation for the {@link Feature#DEVTOOLS}
 */
public class Synchronizer {
	/**
	 * Name of the Preferences of the synchronizer
	 */
	public final static String SYNCHRONIZER_PREFERENCES = "devtools.synchronizer";

	/**
	 * Default sync queue delay (in ms)
	 */
	public final static int DEFAULT_SYNC_QUEUE_DELAY_MS = 300;

	/**
	 * Map of priorities
	 */
	public final static Map<Class<? extends SynchronizableNodeObject>, Integer> PRIORITIES = new HashMap<>();

	/**
	 * Classes of synchronizable objects in the order they must be synchronized
	 */
	public final static List<Class<? extends SynchronizableNodeObject>> CLASSES = Arrays.asList(Datasource.class, Construct.class, ObjectTagDefinition.class,
			Template.class, CrFragment.class, ContentRepository.class);

	/**
	 * Logger
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(Synchronizer.class);

	/**
	 * Container for the packages
	 */
	private static PackageContainer<MainPackageSynchronizer> container;

	/**
	 * FileWatcher implementation
	 */
	private static IFileWatcher watcher;

	/**
	 * Synchronizer status
	 */
	private static Status status = Status.DOWN;

	/**
	 * Registered listeners for synchronization to the FS
	 */
	private static Map<UUID, BiConsumer<SynchronizableNodeObject, Path>> syncToFSListeners = Collections.synchronizedMap(new TreeMap<>());

	/**
	 * Registered listeners for synchronization from the FS
	 */
	private static Map<UUID, BiConsumer<Path, SynchronizableNodeObject>> syncFromFSListeners = Collections.synchronizedMap(new TreeMap<>());

	/**
	 * Registered listeners for package events
	 */
	private static Map<UUID, BiConsumer<String, MainPackageSynchronizer.Event>> packageListeners = Collections.synchronizedMap(new TreeMap<>());

	/**
	 * Sync Path Queue
	 */
	private static QueueWithDelay<PathWithPriority> syncEventQueue = null;

	/**
	 * Queue handler
	 */
	private static ExecutorService queueHandler;

	/**
	 * ID of the user who started the sync
	 */
	private static int userId;

	/**
	 * Object Mapper for serialization/deserialization
	 */
	private static ObjectMapper mapper;

	/**
	 * Alternate Object Mapper for serialization/deserialization (will include NON_EMPTY attributes)
	 */
	private static ObjectMapper alternateMapper;

	static {
		AtomicInteger priority = new AtomicInteger();
		for (Class<? extends SynchronizableNodeObject> clazz : CLASSES) {
			PRIORITIES.put(clazz, priority.incrementAndGet());
		}
		mapper = MiscUtils.newObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(AbstractCRModel.class, new CRModelDeserializer());
		mapper.registerModule(module);

		alternateMapper = MiscUtils.newObjectMapper(Include.NON_EMPTY);
		module = new SimpleModule();
		module.addDeserializer(AbstractCRModel.class, new CRModelDeserializer());
		alternateMapper.registerModule(module);
	}

	/**
	 * No initialization
	 */
	private Synchronizer() {
	}

	/**
	 * Start the synchronizer. When the synchronizer is started, event handlers will be registered, but objects will not be synchronized,
	 * unless synchronization is enabled for a user (via {@link #enable(int)})
	 */
	public synchronized final static void start() throws NodeException {
		status = Status.INIT;

		try {
			NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
			File packagesDir = new File(ConfigurationValue.PACKAGES_PATH.get());

			// if there is already a watcher instance, we stop it first
			if (watcher != null) {
				watcher.stop();
				watcher = null;
			}

			if (queueHandler != null) {
				queueHandler.shutdownNow();
				queueHandler = null;
			}

			if (syncEventQueue != null) {
				syncEventQueue = null;
			}

			// read devtools configuration
			Map<String, Object> synchronizerProperties = prefs.getPropertyMap(SYNCHRONIZER_PREFERENCES);

			// first get classname of IFileWatcher implementation
			String implementationClassName = WatchServiceWatcher.class.getName();
			if (synchronizerProperties != null) {
				implementationClassName = ObjectTransformer.getString(synchronizerProperties.get("class"), implementationClassName);
			}

			try {
				Class<?> implementationClass = Class.forName(implementationClassName);
				if (!IFileWatcher.class.isAssignableFrom(implementationClass)) {
					throw new NodeException(String.format("Synchronizer class %s does not implement %s", implementationClassName, IFileWatcher.class.getName()));
				}
				watcher = (IFileWatcher)implementationClass.newInstance();
			} catch (Exception e) {
				throw new NodeException(String.format("Cannot create synchronizer %s", implementationClassName), e);
			}

			int syncQueueDelay = ObjectTransformer.getInt(synchronizerProperties != null ? synchronizerProperties.get("delay") : DEFAULT_SYNC_QUEUE_DELAY_MS,
					DEFAULT_SYNC_QUEUE_DELAY_MS);

			if (synchronizerProperties != null) {
				watcher.init(synchronizerProperties);
			}

			// try to generate the packages directory, if it does not yet exist
			packagesDir.mkdirs();

			// start the watcher
			watcher.start(packagesDir.toPath());

			// create the package container
			container = new MainPackageContainer(packagesDir.toPath());

			// create the queue
			syncEventQueue = new QueueWithDelay<>(syncQueueDelay, TimeUnit.MILLISECONDS, (o1, o2) -> Integer.compare(o1.priority, o2.priority));

			// start the queue handler
			queueHandler = Executors.newSingleThreadExecutor();
			queueHandler.execute(() -> {
				boolean run = true;
				while (run) {
					try {
						Path syncPath = syncEventQueue.take().path;
						logger.debug(String.format("Fetched %s from queue", syncPath));
						PackageSynchronizer packageSynchronizer = getPackageSynchronizer(syncPath);
						if (packageSynchronizer != null) {
							try {
								if (!isEnabled()) {
									logger.debug(String.format("Sync not enabled, ignore event on %s", syncPath));
									packageSynchronizer.clearCache(syncPath);
								} else {
									packageSynchronizer.handleChangeImmediate(syncPath);
								}
							} catch (Exception e) {
								logger.error(String.format("Error while handling change on %s", syncPath), e);
							}
						} else {
							logger.debug("Found no package synchronizer, ignoring path");
						}
					} catch (InterruptedException e) {
						run = false;
					} catch (NullPointerException e) {
						run = false;
					}
				}
			});

			status = Status.UP;
		} finally {
			if (status == Status.INIT) {
				status = Status.DOWN;
			}
		}
	}

	/**
	 * Register the directory and all sub directories
	 * @param dir directory
	 * @throws IOException
	 */
	protected static void registerAll(Path dir) throws IOException {
		if (watcher != null) {
			watcher.registerAll(dir);
		}
	}

	/**
	 * Deregister the given dir and all subdirs from the watcher
	 * @param dir dir
	 */
	protected static void deregister(Path dir) {
		if (watcher != null) {
			watcher.deregister(dir);
		}
	}

	/**
	 * Stop the synchronizer
	 */
	public synchronized final static void stop() {
		status = Status.SHUTDOWN;
		if (watcher != null) {
			watcher.stop();
			watcher = null;
		}

		if (queueHandler != null) {
			queueHandler.shutdownNow();
			queueHandler = null;
		}

		if (syncEventQueue != null) {
			syncEventQueue = null;
		}

		if (container != null) {
			container.destroy();
			container = null;
		}

		status = Status.DOWN;
	}

	/**
	 * Terminate
	 */
	public synchronized final static void terminate() {
		stop();
	}

	/**
	 * Enable synchronization for the given user
	 * @param userId user ID
	 */
	public final static void enable(int userId) {
		Synchronizer.userId = userId;
	}

	/**
	 * Disable synchronization
	 */
	public final static void disable() {
		userId = 0;
	}

	/**
	 * Get true, when the sync is enabled
	 * @return true when enabled
	 */
	public final static boolean isEnabled() {
		return userId != 0;
	}

	/**
	 * Check that object to transform from/to are not null
	 * @param from source object
	 * @param to target object
	 * @throws NodeException if either object is null
	 */
	public static void checkNotNull(Object from, Object to) throws NodeException {
		if (from == null) {
			throw new NodeException("Cannot transform from null");
		}
		if (to == null) {
			throw new NodeException("Cannot transform to null");
		}
	}

	/**
	 * Synchronize the given object
	 * @param object
	 */
	public static void synchronize(SynchronizableNodeObject object) {
		try {
			DistributionUtil.call(new SynchronizeTask().setObject(object));
		} catch (Exception e) {
			logger.error(String.format("Error while synchronizing %s", object), e);
		}
	}

	/**
	 * Remove the object from all packages
	 * @param object
	 */
	public static void remove(SynchronizableNodeObject object) {
		try {
			DistributionUtil.call(new RemoveTask().setObject(object));
		} catch (Exception e) {
			logger.error(String.format("Error while removing %s", object), e);
		}
	}

	/**
	 * Get all package names
	 * @return set of package names
	 */
	public static Set<String> getPackages() {
		return container.getPackages();
	}

	/**
	 * Get the package synchronizer with given name or null, if not found
	 * @param name package name
	 * @return package synchronizer or null
	 */
	public static MainPackageSynchronizer getPackage(final String name) {
		return container.getPackage(name);
	}

	/**
	 * Add the package with given name. This will just create the package root folder.
	 * @param name package name
	 */
	public static void addPackage(final String name) throws NodeException {
		File packageRoot = new File(container.getRoot().toFile(), name);
		if (!packageRoot.isDirectory()) {
			if (!packageRoot.mkdir()) {
				throw new NodeException(
						String.format("Error while creating package %s: Could not create package folder %s", name,
								packageRoot.getAbsolutePath()));
			} else {
				addPackageSynchronizer(container, packageRoot.toPath());
			}
		}
	}

	/**
	 * Remove the package with given name. This will just remove the package root folder.
	 * @param name package name
	 */
	public static void removePackage(final String name) throws NodeException {
		File packageRoot = new File(container.getRoot().toFile(), name);
		if (packageRoot.isDirectory()) {
			try {
				deregister(packageRoot.toPath());
				FileUtils.deleteDirectory(packageRoot);
				container.removePackageSynchronizer(packageRoot.toPath(), null);
			} catch (IOException e) {
				throw new NodeException(String.format("Error while removing package %s", name), e);
			}
		} else if (packageRoot.exists()) {
			throw new NodeException(String.format("Error while removing package %s: %s is not a folder", name,
					packageRoot.getAbsolutePath()));
		}
	}

	/**
	 * Get the package names of packages assigned to the given node
	 * @param node node
	 * @return set of package names
	 */
	public static Set<String> getPackages(Node node) throws NodeException {
		Set<String> allPackages = getPackages();
		Set<String> packageNames = new HashSet<>();
		DBUtils.executeStatement("SELECT package FROM node_package WHERE node_id = ?", Transaction.SELECT_STATEMENT, st -> {
			st.setInt(1, node.getId());
		}, rs -> {
			while (rs.next()) {
				String packageName = rs.getString("package");
				if (allPackages.contains(packageName))
				packageNames.add(packageName);
			}
		});

		return packageNames;
	}

	/**
	 * Remove a package from the given node
	 * @param node node
	 * @param packageName name of the package to remove
	 * @throws NodeException
	 */
	public static void removePackage(Node node, String packageName) throws NodeException {
		DBUtils.executeStatement("DELETE FROM node_package WHERE node_id = ? AND package = ?", Transaction.DELETE_STATEMENT, st -> {
			st.setInt(1, node.getId());
			st.setString(2, packageName);
		});
	}

	/**
	 * Add a package to a node
	 * @param node node
	 * @param packageName name of the package to add
	 * @throws NodeException
	 */
	public static void addPackage(Node node, String packageName) throws NodeException {
		PackageSynchronizer packageSynchronizer = getPackage(packageName);
		if (packageSynchronizer == null) {
			// TODO i18n
			throw new EntityNotFoundException("Package not found", "", Arrays.asList(packageName));
		}

		DBUtils.executeStatement("INSERT IGNORE INTO node_package (node_id, package) VALUES (?, ?)", Transaction.INSERT_STATEMENT, st -> {
			st.setInt(1, node.getId());
			st.setString(2, packageName);
		});

		for (Class<? extends SynchronizableNodeObject> clazz : CLASSES) {
			AbstractSynchronizer<? extends SynchronizableNodeObject, ? extends AbstractModel> synchronizer = packageSynchronizer.synchronizersPerClass
					.get(clazz);
			synchronizer.assignAll(node);
			}
		}

	/**
	 * Get the package synchronizer for the given path or null
	 * @param path path
	 * @return package synchronizer instance or null
	 */
	public static MainPackageSynchronizer getPackageSynchronizer(Path path) {
		return container.getPackageSynchronizer(path);
	}

	/**
	 * Add a package synchronizer for the given package directory, if not done before
	 * @param container package container
	 * @param packageDir package directory
	 * @throws NodeException
	 */
	public static void addPackageSynchronizer(PackageContainer<?> container, Path packageDir) throws NodeException {
		container.addPackageSynchronizer(packageDir, name -> callListeners(name, PackageSynchronizer.Event.CREATE));
	}

	/**
	 * Remove the package synchronizer for the given package directory
	 * @param container package container
	 * @param packageDir package directory
	 */
	public static void removePackageSynchronizer(PackageContainer<?> container, Path packageDir) {
		container.removePackageSynchronizer(packageDir, name -> callListeners(name, PackageSynchronizer.Event.DELETE));
	}

	/**
	 * Clear the cache of contained objects
	 */
	public static void clearCache() {
		container.clearCache();
	}

	/**
	 * Unwrap and re-throw any NodeException wrapped into a RuntimeException and thrown from the given operator
	 * @param operator operator
	 * @throws NodeException
	 */
	public static void unwrap(Operator operator) throws NodeException {
		try {
			operator.operate();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else {
				throw e;
			}
		}
	}

	/**
	 * Unwrap and re-throw any NodeException wrapped into a RuntimeException and throws from the given supplier
	 * @param supplier supplier
	 * @return supplied value
	 * @throws NodeException
	 */
	public static <R> R unwrap(Supplier<R> supplier) throws NodeException {
		try {
			return supplier.supply();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else {
				throw e;
			}
		}
	}

	/**
	 * Wrap the given operator into a try catch and rethrow any thrown NodeException wrapped into a RuntimeException
	 * @param operator operator
	 */
	public static void wrap(Operator operator) {
		try {
			operator.operate();
		} catch (NodeException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wrap the given supplier into a try catch and rethrow any thrown NodeException wrapped into a RuntimeException
	 * @param supplier supplier
	 * @return supplied value
	 */
	public static <R> R wrap(Supplier<R> supplier) {
		try {
			return supplier.supply();
		} catch (NodeException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the current synchronizer status
	 * @return current status
	 */
	public static Status getStatus() {
		return status;
	}

	/**
	 * Get the ID of the user who started the sync (last)
	 * @return user ID
	 */
	public static int getUserId() {
		return userId;
	}

	/**
	 * Add a listener for sync to the FS
	 * @param listener listener
	 * @return uuid of the listener
	 */
	public static UUID addListenerToFS(BiConsumer<SynchronizableNodeObject, Path> listener) {
		UUID uuid = UUID.randomUUID();
		syncToFSListeners.put(uuid, listener);
		return uuid;
	}

	/**
	 * Add a listener for sync from the FS
	 * @param listener listener
	 * @return uuid of the listener
	 */
	public static UUID addListenerFromFS(BiConsumer<Path, SynchronizableNodeObject> listener) {
		UUID uuid = UUID.randomUUID();
		syncFromFSListeners.put(uuid, listener);
		return uuid;
	}

	/**
	 * Add a listener for package events
	 * @param listener listener
	 * @return uuid of the listener
	 */
	public static UUID addPackageListener(BiConsumer<String, MainPackageSynchronizer.Event> listener) {
		UUID uuid = UUID.randomUUID();
		packageListeners.put(uuid, listener);
		return uuid;
	}

	/**
	 * Remove the listener with given uuid
	 * @param uuid uuid
	 */
	public static void removeListener(UUID uuid) {
		syncToFSListeners.remove(uuid);
		syncFromFSListeners.remove(uuid);
		packageListeners.remove(uuid);
	}

	/**
	 * Call listeners that need to handle synchronization of objects to the FS
	 * @param object synchronized object
	 * @param folder target folder
	 */
	static void callListeners(SynchronizableNodeObject object, Path folder) {
		new HashSet<>(syncToFSListeners.entrySet()).stream().forEach(entry -> {
			try {
				entry.getValue().accept(object, folder);
			} catch (Exception e) {
				Synchronizer.logger.error(String.format("Error while calling listener %s for sync of %s to %s", entry.getKey(), object, folder), e);
			}
		});
	}

	/**
	 * Add package container to the watcher
	 * @param container package container
	 */
	public static void addContainer(PackageContainer<?> container) {
		if (watcher != null) {
			watcher.addContainer(container, name -> callListeners(name, PackageSynchronizer.Event.ADD_CONTAINER));
		}
	}

	/**
	 * Remove package container from the path
	 * @param path path
	 */
	public static void removeContainer(Path path) {
		if (watcher != null) {
			watcher.removeContainer(path, name -> callListeners(name, PackageSynchronizer.Event.REMOVE_CONTAINER));
		}
	}

	/**
	 * Check whether the given name is allowed as package name
	 * @param name name to check
	 * @return true if allowed
	 */
	public static boolean allowedPackageName(String name) {
		if (StringUtils.isEmpty(name)) {
			return false;
		} else if (name.startsWith(".")) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Call listeners that need to handle synchronization of objects from the FS
	 * @param folder source folder
	 * @param object synchronized object
	 */
	static void callListeners(Path folder, SynchronizableNodeObject object) {
		new HashSet<>(syncFromFSListeners.entrySet()).stream().forEach(entry -> {
			try {
				entry.getValue().accept(folder, object);
			} catch (Exception e) {
				Synchronizer.logger.error(String.format("Error while calling listener %s for sync of %s from %s", entry.getKey(), object, folder), e);
			}
		});
	}

	/**
	 * Call listeners that need to handle package events
	 * @param name package name
	 * @param event event
	 */
	static void callListeners(String name, MainPackageSynchronizer.Event event) {
		new HashSet<>(packageListeners.entrySet()).stream().forEach(entry -> {
			try {
				entry.getValue().accept(name, event);
			} catch (Exception e) {
				Synchronizer.logger.error(String.format("Error while calling listener %s for %s of package %s", entry.getKey(), event, name), e);
			}
		});
	}

	/**
	 * Add a path to synchronizer into the queue. If the path is already contained in the queue, remove it first and readd it (moving it to the tail of the queue)
	 * @param syncPath sync path
	 * @param priority priority
	 */
	static void addSyncEvent(Path syncPath, int priority) {
		if (syncEventQueue != null) {
			PathWithPriority entry = new PathWithPriority(syncPath, priority);
			if (!syncEventQueue.contains(entry)) {
				syncEventQueue.put(entry);
			}
		}
	}

	/**
	 * Fix sorting of a list property in the model according to the order in the given reference model
	 * @param model model to fix
	 * @param reference reference model
	 * @param propertyExtractor reference extractor
	 * @param idExtractor extractor for getting the ID(s) of the elements in the list property. The first non-null value returned will be used
	 */
	static <T, U> void fixSorting(U model, U reference, Function<U, List<T>> propertyExtractor, Function<T, List<String>> idExtractor) {
		Map<String, Integer> sortMap = new HashMap<>();
		AtomicInteger counter = new AtomicInteger();

		Function<T, List<String>> nullFreeIdExtractor = e -> {
			return idExtractor.apply(e).stream().filter(s -> s != null).collect(Collectors.toList());
		};

		List<T> sortList = propertyExtractor.apply(model);
		if (ObjectTransformer.isEmpty(sortList)) {
			return;
		}

		List<T> referenceList = propertyExtractor.apply(reference);
		if (!ObjectTransformer.isEmpty(referenceList)) {
			for (T element : referenceList) {
				sortMap.put(nullFreeIdExtractor.apply(element).get(0), counter.getAndIncrement());
			}
		}

		sortList.sort((e1, e2) -> {
			List<String> ids1 = nullFreeIdExtractor.apply(e1);
			List<String> ids2 = nullFreeIdExtractor.apply(e2);
			int sort1 = ids1.stream().filter(sortMap::containsKey).map(sortMap::get).findFirst().orElse(counter.get());
			int sort2 = ids2.stream().filter(sortMap::containsKey).map(sortMap::get).findFirst().orElse(counter.get());

			if (sort1 == sort2) {
				return ids1.get(0).compareTo(ids2.get(0));
			} else {
				return sort1 - sort2;
			}
		});
	}

	/**
	 * Get the Object Mapper instance for deserialization/serialization
	 * @return object mapper
	 */
	public static ObjectMapper mapper() {
		return mapper(false);
	}

	/**
	 * Get the Object Mapper instance for deserialization/serialization
	 * @param alternate true to get the alternate mapper
	 * @return object mapper
	 */
	public static ObjectMapper mapper(boolean alternate) {
		return alternate ? alternateMapper : mapper;
	}

	/**
	 * Synchronizer stati
	 */
	public static enum Status {
		/**
		 * Synchronizer is not running
		 */
		DOWN,

		/**
		 * Synchronizer is initializing
		 */
		INIT,

		/**
		 * Synchronizer is up and running
		 */
		UP,

		/**
		 * Synchronizer is shutting down
		 */
		SHUTDOWN
	}

	/**
	 * Wrapper class for encapsulating a path with its priority
	 */
	protected static class PathWithPriority {
		/**
		 * Path
		 */
		Path path;

		/**
		 * Priority
		 */
		int priority;

		/**
		 * Create an instance
		 * @param path path
		 * @param priority priority
		 */
		public PathWithPriority(Path path, int priority) {
			this.path = path;
			this.priority = priority;
		}

		@Override
		public int hashCode() {
			return path.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PathWithPriority) {
				return path.equals(((PathWithPriority) obj).path);
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return String.format("Prio %d, Path %s", priority, path);
		}
	}

	/**
	 * Task to remove an object from the packages
	 */
	public static class RemoveTask extends TrxCallable<Boolean> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -769649314458302809L;

		protected SynchronizableNodeObject object;

		/**
		 * Set the object
		 * @param object object to remove
		 * @return fluent API
		 */
		public RemoveTask setObject(SynchronizableNodeObject object) {
			this.object = object;
			return this;
		}

		@Override
		protected Boolean callWithTrx() throws NodeException {
			if (watcher != null) {
				container.remove(object);
			}
			return true;
		}
	}

	/**
	 * Task to synchronize an object with the packages
	 */
	public static class SynchronizeTask extends TrxCallable<Boolean> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2935955052795467L;

		protected SynchronizableNodeObject object;

		/**
		 * Set the object
		 * @param object object to synchronize
		 * @return fluent API
		 */
		public SynchronizeTask setObject(SynchronizableNodeObject object) {
			this.object = object;
			return this;
		}

		@Override
		protected Boolean callWithTrx() throws NodeException {
			if (watcher != null && isEnabled()) {
				container.synchronize(object);
				return true;
			} else {
				return false;
			}
		}
	}
}
