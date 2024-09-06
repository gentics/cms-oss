package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.tautua.markdownpapers.Markdown;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.exceptions.DuplicateEntityException;
import com.gentics.contentnode.rest.model.AbstractModel;

/**
 * Abstract base class for package synchronizers
 */
public abstract class PackageSynchronizer {
	/**
	 * Name of the subdirectory containing the constructs
	 */
	public final static String CONSTRUCTS_DIR = "constructs";

	/**
	 * Name of the subdirectory containing the templates
	 */
	public final static String TEMPLATES_DIR = "templates";

	/**
	 * Name of the subdirectory containing datasources
	 */
	public final static String DATASOURCES_DIR = "datasources";

	/**
	 * Name of the subdirectory containing objectproperties
	 */
	public final static String OBJECTPROPERTIES_DIR = "objectproperties";

	/**
	 * Name of the subdirectory containing cr fragments
	 */
	public final static String CR_FRAGMENTS_DIR = "cr_fragments";

	/**
	 * Name of the subdirectory containing contentrepositories
	 */
	public final static String CONTENTREPOSITORIES_DIR = "contentrepositories";

	/**
	 * Name of the subdirectory containing static files
	 */
	public final static String FILES_DIR = "files";

	/**
	 * Name of the subdirectory containing the localized copies of localizable objects
	 */
	public final static String CHANNELS_DIR = "channels";

	/**
	 * Name of the subdirectory containing handlebars helpers and partials
	 */
	public final static String HANDLEBARS_DIR = "handlebars";

	/**
	 * Directory map for object class
	 */
	public final static Map<Class<? extends SynchronizableNodeObject>, String> directoryMap = new HashMap<>(4);

	static {
		directoryMap.put(Construct.class, CONSTRUCTS_DIR);
		directoryMap.put(Template.class, TEMPLATES_DIR);
		directoryMap.put(Datasource.class, DATASOURCES_DIR);
		directoryMap.put(ObjectTagDefinition.class, OBJECTPROPERTIES_DIR);
		directoryMap.put(CrFragment.class, CR_FRAGMENTS_DIR);
		directoryMap.put(ContentRepository.class, CONTENTREPOSITORIES_DIR);
	}

	/**
	 * Name of the readme file containing the package description
	 */
	public final static String README_FILE = "README.md";


	/**
	 * Package path
	 */
	protected Path packagePath;

	/**
	 * Map of synchronizer implementations per path
	 */
	protected Map<Path, AbstractSynchronizer<? extends SynchronizableNodeObject, ? extends AbstractModel>> synchronizersPerPath = new HashMap<>();

	/**
	 * Map of synchronizer implementations per class
	 */
	protected Map<Class<? extends SynchronizableNodeObject>, AbstractSynchronizer<? extends SynchronizableNodeObject, ? extends AbstractModel>> synchronizersPerClass = new HashMap<>();

	/**
	 * Map of handlers for changes in paths (if they are not handled by a synchronizer)
	 */
	protected Map<Path, Consumer<Path>> pathHandlers = new HashMap<>();

	/**
	 * Package name
	 */
	protected String packageName;

	/**
	 * Cached handlebars helpers
	 */
	protected String handlebarsHelpers;

	/**
	 * Create an instance with the given path
	 * @param packagePath package path
	 * @throws NodeException
	 */
	public PackageSynchronizer(Path packagePath) throws NodeException {
		this(packagePath, true);
	}

	/**
	 * Create an instance with the given path
	 * @param packagePath package path
	 * @param registerWatchers true to register watchers
	 * @throws NodeException
	 */
	public PackageSynchronizer(Path packagePath, boolean registerWatchers) throws NodeException {
		this.packagePath = packagePath;
		this.packageName = Normalizer.normalize(packagePath.getFileName().toString(), Normalizer.Form.NFC);
		Synchronizer.logger.debug("Creating new package " + packageName);
		Path constructsPath = new File(packagePath.toFile(), CONSTRUCTS_DIR).toPath();
		Path templatesPath = new File(packagePath.toFile(), TEMPLATES_DIR).toPath();
		Path datasourcesPath = new File(packagePath.toFile(), DATASOURCES_DIR).toPath();
		Path objectPropertiesPath = new File(packagePath.toFile(), OBJECTPROPERTIES_DIR).toPath();
		Path crFragmentsPath = new File(packagePath.toFile(), CR_FRAGMENTS_DIR).toPath();
		Path contentrepositoriesPath = new File(packagePath.toFile(), CONTENTREPOSITORIES_DIR).toPath();
		try (Trx trx = new Trx(); LangTrx langTrx = new LangTrx("en")) {
			addSynchronizer(constructsPath, new ConstructSynchronizer(this, constructsPath));
			addSynchronizer(templatesPath, new TemplateSynchronizer(this, templatesPath));
			addSynchronizer(datasourcesPath, new DatasourceSynchronizer(this, datasourcesPath));
			addSynchronizer(objectPropertiesPath, new ObjectTagDefinitionSynchronizer(this, objectPropertiesPath));
			addSynchronizer(crFragmentsPath, new ContentRepositoryFragmentSynchronizer(this, crFragmentsPath));
			addSynchronizer(contentrepositoriesPath, new ContentRepositorySynchronizer(this, contentrepositoriesPath));
			trx.success();
		}
		// add a handler for changes in the handlebars directory
		pathHandlers.put(new File(packagePath.toFile(), HANDLEBARS_DIR).toPath(), changedPath -> {
			// clean the cached handlebars helpers
			handlebarsHelpers = null;
		});
		if (registerWatchers) {
			try {
				Synchronizer.registerAll(packagePath);

				if (Synchronizer.getStatus() == Synchronizer.Status.UP) {
					Files.walkFileTree(packagePath, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							String name = Normalizer.normalize(dir.getFileName().toString(), Normalizer.Form.NFC);
							if (name.startsWith(".")) {
								return FileVisitResult.SKIP_SUBTREE;
							}

							try (Trx trx = new Trx(); LangTrx langTrx = new LangTrx("en")) {
								handleChangeBuffered(dir);
							} catch (NodeException e) {
								Synchronizer.logger.error("Error while synchronizing " + dir, e);
							}
							return FileVisitResult.CONTINUE;
						}
					});
				}
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}
	}

	/**
	 * Get the package name
	 * @return name
	 */
	public String getName() {
		return packageName;
	}

	/**
	 * Handle a change in the filesystem by adding the path to the queue
	 * @param path path
	 * @throws NodeException in case of errors
	 */
	public void handleChangeBuffered(Path path) throws NodeException {
		try (SynchronizedPath sPath = new SynchronizedPath(path)) {
			Path syncPath = path;
			AbstractSynchronizer<? extends SynchronizableNodeObject, ? extends AbstractModel> synchronizer = null;

			while (synchronizer == null && syncPath != null) {
				Optional.ofNullable(pathHandlers.get(syncPath.getParent())).ifPresent(handler -> handler.accept(path));

				synchronizer = synchronizersPerPath.get(syncPath.getParent());
				if (synchronizer == null) {
					syncPath = syncPath.getParent();
					if (!syncPath.startsWith(packagePath)) {
						syncPath = null;
					}
				}
			}
			if (synchronizer != null && syncPath != null) {
				Synchronizer.addSyncEvent(syncPath, synchronizer.getPriority());
			} else {
				Synchronizer.logger.debug(String.format("Could not find synchronizer for %s", path));
			}
		} catch (PathAlreadyHandledException e) {
			Synchronizer.logger.debug(String.format("Not handling change in path %s, because it is already handled", e.getPath()));
		}
	}

	/**
	 * Handle a change in the filesystem immediately
	 * @param syncPath sync path
	 * @throws NodeException
	 */
	public void handleChangeImmediate(Path syncPath) throws NodeException {
		AbstractSynchronizer<? extends SynchronizableNodeObject, ? extends AbstractModel> synchronizer = synchronizersPerPath.get(syncPath.getParent());
		if (synchronizer != null) {
			try (Trx trx = new Trx(null, Synchronizer.getUserId()); LangTrx langTrx = new LangTrx("en")) {
				synchronizer.syncFromFilesystem(syncPath, null);
				trx.success();
			}
		}
	}

	/**
	 * Synchronize the given object with the synchronizer. This method must be called with a transaction
	 * @param synchronizer synchronizer
	 * @param object object
	 * @param add true to add the object, if not yet contained
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	protected <T extends SynchronizableNodeObject> void internalSyncToFilesystem(AbstractSynchronizer<T, ? extends AbstractModel> synchronizer, T object, boolean add)
			throws NodeException {
		try {
			// when the object has a master, and shall not be added, we only
			// synchronize, if the master is contained
			if (!object.isMaster() && !add && synchronizer.getCurrentSyncLocation((T) object.getMaster()) == null) {
				return;
			}

			File currentSyncLocation = synchronizer.getCurrentSyncLocation(object);
			File syncTarget = synchronizer.getSyncTarget(object);
			T existing = synchronizer.findObject(syncTarget.toPath());

			if (existing != null && !existing.equals(object)) {
				throw new DuplicateEntityException(I18NHelper.get("devtools_package.add.duplicate", object.toString(), packageName, existing.toString()));
			}

			try (SynchronizedPath sPath = new SynchronizedPath(syncTarget.toPath())) {
				if (currentSyncLocation != null && !currentSyncLocation.equals(syncTarget)) {
					if (syncTarget.exists()) {
						Synchronizer.deregister(currentSyncLocation.toPath());
						FileUtils.deleteDirectory(currentSyncLocation);
					} else {
						currentSyncLocation.renameTo(syncTarget);
					}
				}

				if (syncTarget.isDirectory()) {
					synchronizer.syncToFilesystem(object, syncTarget.toPath());
				} else if (add || !object.isMaster()) {
					if (syncTarget.mkdirs()) {
						synchronizer.syncToFilesystem(object, syncTarget.toPath());
					} else {
						Synchronizer.logger.error(String.format("Error while synchronizing %s: Could not create directory %s", object,
								syncTarget.getAbsolutePath()));
					}
				}
			}
		} catch (PathAlreadyHandledException e) {
			Synchronizer.logger.debug(String.format("Not synchronizing %s because the path %s is already handled", object, e.getPath()));
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(String.format("Error while synchronizing %s", object), e);
		}
	}

	/**
	 * Synchronize the given object. If the object is not yet contained in the package and add is true, the object will be added first
	 * @param object to synchronize
	 * @param add true to add if not yet added
	 * @throws NodeException if the object cannot be synchronized
	 */
	@SuppressWarnings("unchecked")
	public <T extends SynchronizableNodeObject> void synchronize(T object, boolean add) throws NodeException {
		if (object == null) {
			return;
		}
		AbstractSynchronizer<T, ? extends AbstractModel> synchronizer = (AbstractSynchronizer<T, ? extends AbstractModel>) synchronizersPerClass.get(object.getObjectInfo().getObjectClass());
		if (synchronizer != null) {
			internalSyncToFilesystem(synchronizer, object, add);
		}
	}

	/**
	 * Synchronize all objects of the given class to the filesystem.
	 * This method must be called with a transaction
	 * @param clazz clazz
	 * @return number of synchronized objects
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public <T extends SynchronizableNodeObject> int syncAllToFilesystem(Class<T> clazz) throws NodeException {
		int objectCount = 0;

		AbstractSynchronizer<T, ? extends AbstractModel> synchronizer = (AbstractSynchronizer<T, ? extends AbstractModel>) synchronizersPerClass.get(clazz);
		if (synchronizer != null) {
			for (SynchronizableNodeObject object : synchronizer.getObjects()) {
				// omit objects that are not present in the cms
				if (AbstractContentObject.isEmptyId(object.getId())) {
					continue;
				}
				internalSyncToFilesystem(synchronizer, (T)object, false);
				objectCount++;
			}
		}

		return objectCount;
	}

	/**
	 * Synchronize all objects of the given class from the filesystem into the cms.
	 * This method must be called with a transaction. The transaction will be committed, but not closed after every object (to avoid long lasting db locks)
	 * @param clazz clazz
	 * @return number of synchronized objects
	 * @throws NodeException
	 */
	public <T extends SynchronizableNodeObject> int syncAllFromFilesystem(Class<T> clazz) throws NodeException {
		int objectCount = 0;
		Transaction t = TransactionManager.getCurrentTransaction();
		@SuppressWarnings("unchecked")
		AbstractSynchronizer<T, ? extends AbstractModel> synchronizer = (AbstractSynchronizer<T, ? extends AbstractModel>) synchronizersPerClass.get(clazz);
		if (synchronizer != null) {
			for (Path path : synchronizer.getObjectPaths()) {
				synchronizer.syncFromFilesystem(path, null);
				objectCount++;
				t.commit(false);
			}
		}

		return objectCount;
	}

	/**
	 * Remove an object from the package
	 * @param object object to remove
	 * @param force true to force deletion, even if sync is disabled
	 */
	public <T extends SynchronizableNodeObject> void remove(T object, boolean force) throws NodeException {
		if (object == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		AbstractSynchronizer<T, ? extends AbstractModel> synchronizer = (AbstractSynchronizer<T, ? extends AbstractModel>) synchronizersPerClass.get(object
				.getObjectInfo().getObjectClass());
		if (synchronizer != null) {
			synchronizer.remove(object, force);
		}
	}

	/**
	 * Get the list of objects of given class contained in this package
	 * @param clazz object class
	 * @return list of contained objects
	 * @throws NodeException
	 */
	public <T extends SynchronizableNodeObject> List<PackageObject<T>> getObjects(Class<T> clazz) throws NodeException {
		return getObjects(clazz, true);
	}

	/**
	 * Get the list of objects of given class contained in this package
	 * @param clazz object class
	 * @param addPackageName true to add the package name
	 * @return list of contained objects
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public <T extends SynchronizableNodeObject> List<PackageObject<T>> getObjects(Class<T> clazz, boolean addPackageName) throws NodeException {
		Entry<Path, AbstractSynchronizer<? extends SynchronizableNodeObject, ? extends AbstractModel>> entry = synchronizersPerPath.entrySet().stream()
				.filter(e -> e.getValue().getClazz().equals(clazz)).findFirst().orElseGet(null);
		if (entry == null) {
			return Collections.emptyList();
		} else {
			List<PackageObject<T>> list = new ArrayList<>();
			for (SynchronizableNodeObject o : entry.getValue().getObjects()) {
				list.add((PackageObject<T>)new PackageObject<>(o, addPackageName ? packageName : null));
			}
			return list;
		}
	}

	/**
	 * Get nodes to which this package is assigned
	 * @return nodes
	 * @throws NodeException
	 */
	public List<Node> getNodes() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Set<Integer> nodeIds = new HashSet<>();
		DBUtils.executeStatement("SELECT node_id FROM node_package WHERE package = ?", Transaction.SELECT_STATEMENT, st -> {
			st.setString(1, getName());
		}, rs -> {
			while (rs.next()) {
				nodeIds.add(rs.getInt("node_id"));
			}
		});
		return t.getObjects(Node.class, nodeIds);
	}

	/**
	 * Clear the cache of contained objects
	 */
	public void clearCache() {
		synchronizersPerPath.values().forEach(AbstractSynchronizer::clearCache);
		handlebarsHelpers = null;
	}

	/**
	 * Clear the cache of the synchronizer handling the given path
	 * @param syncPath sync path
	 */
	public void clearCache(Path syncPath) {
		AbstractSynchronizer<? extends SynchronizableNodeObject, ? extends AbstractModel> synchronizer = synchronizersPerPath.get(syncPath.getParent());
		if (synchronizer != null) {
			synchronizer.clearCache();
		}
	}

	/**
	 * Get the description, if the README.md file exists, null otherwise
	 * @return description or null
	 */
	public String getDescription() {
		File readMeFile = new File(packagePath.toFile(), README_FILE);
		if (readMeFile.isFile()) {
			try (Reader in = new FileReader(readMeFile); StringWriter out = new StringWriter()) {
				Markdown md = new Markdown();
				md.transform(in, out);
				return out.toString();
			} catch (Exception e) {
				logger.error(String.format("Error while parsing file %s", readMeFile.getAbsolutePath()), e);
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Get the package root path
	 * @return package root path
	 */
	public Path getPackagePath() {
		return packagePath;
	}

	/**
	 * Get the handlebars helpers in this package.
	 * @return handlebars helpers
	 * @throws IOException
	 */
	public String getHandlebarsHelpers() throws IOException {
		if (handlebarsHelpers == null) {
			File handlebarsDirectory = new File(this.packagePath.toFile(), HANDLEBARS_DIR);
			File helpersDirectory = new File(handlebarsDirectory, "helpers");

			if (helpersDirectory.isDirectory()) {
				StringBuilder registerHelpers = new StringBuilder();
				File[] files = helpersDirectory.listFiles((dir, name) -> StringUtils.endsWith(name, ".js"));

				if (files != null) {
					for (File helperFile : files) {
						String helperNameShort = StringUtils.removeEnd(helperFile.getName(), ".js");
						String helperName = String.format("%s.%s", packageName, helperNameShort);
						String helperFileContents = FileUtils.readFileToString(helperFile, StandardCharsets.UTF_8);
						String register = String.format("Handlebars.registerHelper('%s', %s)", helperName, helperFileContents);

						registerHelpers.append(register).append("\n");
					}
				}

				handlebarsHelpers = registerHelpers.toString();
			} else {
				handlebarsHelpers = "";
			}
		}

		return handlebarsHelpers;
	}

	/**
	 * Get the handlebars partial directory (regardless of whether it exists or not)
	 * @return handlebars partial directory
	 */
	public File getHandlebarsPartialsDirectory() {
		File handlebarsDirectory = new File(this.packagePath.toFile(), HANDLEBARS_DIR);
		return new File(handlebarsDirectory, "partials");
	}

	/**
	 * Add a synchronizer implementation
	 * @param path base path
	 * @param implementation implementation
	 */
	private void addSynchronizer(Path path, AbstractSynchronizer<? extends SynchronizableNodeObject, ? extends AbstractModel> implementation) {
		synchronizersPerPath.put(path, implementation);
		synchronizersPerClass.put(implementation.getClazz(), implementation);
	}

	protected static class SynchronizedPath implements AutoCloseable {
		/**
		 * Set storing the currently synchronized paths
		 */
		private final static Set<Path> pathsCurrentlySynchronized = Collections.synchronizedSet(new HashSet<>());

		/**
		 * Check whether the given path is itself currently synchronized or is a subpath of a currently synchronized path
		 * @param path path to check
		 * @return true if the path or any of its parents is currently synchronized
		 */
		private static boolean isSynchronized(Path path) {
			while (path != null) {
				if (pathsCurrentlySynchronized.contains(path)) {
					return true;
				}
				path = path.getParent();
			}
			return false;
		}

		private Path path;

		public SynchronizedPath(Path path) throws PathAlreadyHandledException {
			synchronized (pathsCurrentlySynchronized) {
				if (isSynchronized(path)) {
					throw new PathAlreadyHandledException(path.toString());
				}

				pathsCurrentlySynchronized.add(path);
				this.path = path;
			}
		}

		@Override
		public void close() {
			pathsCurrentlySynchronized.remove(path);
		}
	}

	/**
	 * Exception that is thrown when a path is already synchronized
	 */
	protected static class PathAlreadyHandledException extends Exception {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -5938742219457046340L;

		/**
		 * Path
		 */
		private String path;

		/**
		 * Create instance for the path
		 * @param path path
		 */
		public PathAlreadyHandledException(String path) {
			this.path = path;
		}

		/**
		 * Path
		 * @return path
		 */
		public String getPath() {
			return path;
		}
	}

	/**
	 * Enumeration of possible package events
	 */
	public static enum Event {
		/**
		 * Package has been created
		 */
		CREATE,

		/**
		 * Package has been deleted
		 */
		DELETE,

		/**
		 * Subpackage container has been added
		 */
		ADD_CONTAINER,

		/**
		 * Subpackage container has been removed
		 */
		REMOVE_CONTAINER,

		/**
		 * gentics_package.json has been handled
		 */
		HANDLE_PACKAGE_JSON
	}
}
