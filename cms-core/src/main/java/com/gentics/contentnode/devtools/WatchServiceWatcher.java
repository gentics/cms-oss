package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.addPackageSynchronizer;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;

/**
 * {@link IFileWatcher} implementation that uses the {@link WatchService}
 */
public class WatchServiceWatcher extends AbstractFileWatcher {
	/**
	 * Watch service
	 */
	private WatchService watcher;

	/**
	 * All registered watchers
	 */
	private Map<WatchKey, Path> watchKeyMap = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Watcher thread
	 */
	private Thread watcherThread;

	/**
	 * Create the instance
	 */
	public WatchServiceWatcher() {
	}

	@Override
	public void init(Map<String, Object> properties) throws NodeException {
	}

	@Override
	public void start(Path rootPath) throws NodeException {
		try {
			watcher = FileSystems.getDefault().newWatchService();

			watcherThread = new Thread(() -> {
				Synchronizer.logger.debug("Synchronization watcher starting");
				for (;;) {
					// wait for key to be signalled
					WatchKey key;
					try {
						key = watcher.take();
					} catch (InterruptedException x) {
						Synchronizer.logger.debug("Synchronization watcher stopping");
						return;
					}

					Synchronizer.logger.debug("Took key that watches " + key.watchable());

					Path path = watchKeyMap.get(key);
					if (path != null) {
						PackageContainer<?> container = containers.get(path);
						if (container != null) {
							try {
								handleContainerChange(container, key);
							} catch (Exception e) {
								Synchronizer.logger.error("Error while handling event on " + key.watchable(), e);
							}
						} else {
							try {
								handlePackageChange(key, path);
							} catch (Exception e) {
								Synchronizer.logger.error("Error while handling event on " + key.watchable(), e);
							}
						}
					}

					boolean valid = key.reset();
					if (!valid) {
						if (path != null) {
//							deregister(path);
						} else {
							Synchronizer.logger.debug("Remove watcher for " + key.watchable());
							key.cancel();
						}
					}
				}
			});
			watcherThread.start();
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public void stop() {
		if (watcherThread != null) {
			watcherThread.interrupt();
			try {
				watcherThread.join();
			} catch (InterruptedException e) {
			}
			watcherThread = null;
		}
		if (watcher != null) {
			try {
				watcher.close();
			} catch (IOException e) {
			}
			watcher = null;
		}
	}

	@Override
	public void registerAll(Path dir) throws IOException {
		if (Files.isDirectory(dir, NOFOLLOW_LINKS)) {
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					String name = Normalizer.normalize(dir.getFileName().toString(), Normalizer.Form.NFC);
					if (name.startsWith(".")) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					register(dir);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.SKIP_SUBTREE;
				}
			});
		}
	}

	@Override
	public void deregister(Path dir) {
		Set<WatchKey> toRemove = new HashSet<>();
		watchKeyMap.entrySet().forEach(entry -> {
			if (entry.getValue().startsWith(dir)) {
				toRemove.add(entry.getKey());
			}
		});

		for (WatchKey key : toRemove) {
			Synchronizer.logger.debug("Remove watcher for " + key.watchable());
			key.cancel();
			watchKeyMap.remove(key);
		}
	}

	/**
	 * Register the given directory with the watch service
	 * @param dir directory
	 * @return watch key
	 */
	private void register(Path dir) throws IOException {
		if (watchKeyMap.containsValue(dir)) {
			Synchronizer.logger.debug("Already registered watcher for " + dir);
		} else {
			Synchronizer.logger.debug("Register watcher for " + dir);
			WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			watchKeyMap.put(key, dir);
		}
	}

	/**
	 * Resolve the event context for the given watch key
	 * @param key watch key
	 * @param event event
	 * @return path of the event context
	 */
	@SuppressWarnings("unchecked")
	private Path resolve(WatchKey key, WatchEvent<?> event) {
		Path path = watchKeyMap.get(key);
		if (path == null) {
			return null;
		}

		WatchEvent<Path> ev = (WatchEvent<Path>) event;
		Path name = ev.context();
		return path.resolve(name);
	}

	/**
	 * Handle change in the root directory of a package container
	 * @param container package container
	 * @param key watch key
	 * @throws IOException
	 * @throws NodeException
	 */
	private void handleContainerChange(PackageContainer<?> container, WatchKey key) throws IOException, NodeException {
		for (WatchEvent<?> event : key.pollEvents()) {
			Kind<?> kind = event.kind();

			// TODO
			if (kind == OVERFLOW) {
				Synchronizer.logger.debug("Overflow");
				continue;
			}

			Path child = resolve(key, event);

			if (Files.isDirectory(child)) {
				if (kind == ENTRY_CREATE) {
					// new package
					addPackageSynchronizer(container, child);
				}
			}

			if (kind == ENTRY_DELETE) {
				// package removed
				Synchronizer.removePackageSynchronizer(container, child);

				deregister(child);
			}
		}
	}

	/**
	 * Handle change in a package
	 * @param key watch key
	 * @param path path
	 * @throws NodeException
	 * @throws IOException
	 */
	private void handlePackageChange(WatchKey key, Path path) throws NodeException, IOException {
		try {
			Synchronizer.logger.debug(String.format("Handle changes in %s", path));
			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();

				// TODO
				if (kind == OVERFLOW) {
					Synchronizer.logger.debug("Overflow");
					continue;
				}

				Path child = resolve(key, event);
				if (child == null) {
					Synchronizer.logger.debug("Child path is null, ignoring");
					continue;
				}
				Synchronizer.logger.debug(String.format("Found event %s on %s", kind, child));

				if (kind == ENTRY_CREATE) {
					Synchronizer.registerAll(child);

					// when we detected a new directory, we handle everything within, since the events for created subobjects
					// might come faster than we can register the handlers
					if (kind == ENTRY_CREATE && Files.isDirectory(child)) {
						PackageSynchronizer packageSynchronizer = Synchronizer.getPackageSynchronizer(child);
						if (packageSynchronizer != null) {
							Files.walkFileTree(child, new SimpleFileVisitor<Path>() {
								@Override
								public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
									String name = Normalizer.normalize(dir.getFileName().toString(), Normalizer.Form.NFC);
									if (name.startsWith(".")) {
										return FileVisitResult.SKIP_SUBTREE;
									}

									try {
										// if the directory is direct
										// subdirectory of a package container,
										// we add the package to the container
										PackageContainer<?> container = containers.get(dir.getParent());
										if (container != null) {
											addPackageSynchronizer(container, dir);
										} else {
											packageSynchronizer.handleChangeBuffered(dir);
										}
									} catch (NodeException e) {
									}

									return FileVisitResult.CONTINUE;
								}

								@Override
								public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
									return FileVisitResult.SKIP_SUBTREE;
								}
							});
						}
					}
				}

				if (kind == ENTRY_DELETE) {
					Synchronizer.deregister(child);
				}

				// change in a folder handled by a synchronizer
				PackageSynchronizer packageSynchronizer = Synchronizer.getPackageSynchronizer(path);
				if (packageSynchronizer != null) {
					Synchronizer.logger.debug(String.format("Let synchronizer handle change of %s", child));
					packageSynchronizer.handleChangeBuffered(child);
				}
			}
		} finally {
			key.reset();
		}
	}
}
