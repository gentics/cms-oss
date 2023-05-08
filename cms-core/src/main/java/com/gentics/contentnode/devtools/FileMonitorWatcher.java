package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.addPackageSynchronizer;
import static com.gentics.contentnode.devtools.Synchronizer.removePackageSynchronizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;

/**
 * {@link IFileWatcher} implementation that uses a @link {@link FileAlterationMonitor}
 */
public class FileMonitorWatcher extends AbstractFileWatcher {
	/**
	 * Monitor
	 */
	private FileAlterationMonitor monitor;

	/**
	 * Polling interval in ms
	 */
	private int interval = 1000;

	/**
	 * Map containing container observers
	 */
	private Map<Path, FileAlterationObserver> containerObservers = new HashMap<>();

	/**
	 * Create the instance
	 */
	public FileMonitorWatcher() {
	}

	@Override
	public void addContainer(PackageContainer<?> container, Consumer<String> success) {
		super.addContainer(container, success);
		Path rootPath = container.getRoot();

		FileAlterationObserver observer = new FileAlterationObserver(rootPath.toFile(), file -> file.isDirectory()
				&& rootPath.toFile().equals(file.getParentFile()));
		observer.addListener(new FileAlterationListenerAdaptor() {
			@Override
			public void onDirectoryCreate(File dir) {
				try {
					addPackageSynchronizer(container, dir.toPath());
				} catch (Exception e) {
					Synchronizer.logger.error(String.format("Error while handling create event on %s", dir), e);
				}
			}

			@Override
			public void onDirectoryDelete(File dir) {
				try {
					removePackageSynchronizer(container, dir.toPath());
				} catch (Exception e) {
					Synchronizer.logger.error(String.format("Error while handling delete event on %s", dir), e);
				}
			}
		});

		monitor.addObserver(observer);
		containerObservers.put(rootPath, observer);
	}

	@Override
	public void removeContainer(Path root, Consumer<String> success) {
		super.removeContainer(root, success);

		FileAlterationObserver removed = containerObservers.remove(root);
		if (removed != null) {
			try {
				monitor.removeObserver(removed);
				removed.destroy();
			} catch (Exception e) {
			}
		}
	}

	@Override
	public void init(Map<String, Object> properties) throws NodeException {
		interval = ObjectTransformer.getInt(properties.get("interval"), interval);
	}

	@Override
	public void start(Path rootPath) throws NodeException {
		FileAlterationObserver packageObserver = new FileAlterationObserver(rootPath.toFile());
		packageObserver.addListener(new FileAlterationListenerAdaptor() {
			@Override
			public void onFileCreate(File file) {
				PackageSynchronizer packageSynchronizer = Synchronizer.getPackageSynchronizer(file.toPath());
				if (packageSynchronizer != null) {
					try {
						packageSynchronizer.handleChangeBuffered(file.toPath());
					} catch (Exception e) {
						Synchronizer.logger.error(String.format("Error while handling create event on %s", file), e);
					}
				}
			}

			@Override
			public void onFileChange(File file) {
				PackageSynchronizer packageSynchronizer = Synchronizer.getPackageSynchronizer(file.toPath());
				if (packageSynchronizer != null) {
					try {
						packageSynchronizer.handleChangeBuffered(file.toPath());
					} catch (Exception e) {
						Synchronizer.logger.error(String.format("Error while handling change event on %s", file), e);
					}
				}
			}

			@Override
			public void onFileDelete(File file) {
				PackageSynchronizer packageSynchronizer = Synchronizer.getPackageSynchronizer(file.toPath());
				if (packageSynchronizer != null) {
					try {
						packageSynchronizer.clearCache();
					} catch (Exception e) {
						Synchronizer.logger.error(String.format("Error while handling delete event on %s", file), e);
					}
				}
			}
		});

		monitor = new FileAlterationMonitor(interval, packageObserver);
		try {
			monitor.start();
		} catch (Exception e) {
			throw new NodeException("Could not start FileAlterationMonitor", e);
		}
	}

	@Override
	public void stop() {
		if (monitor != null) {
			try {
				monitor.stop();
			} catch (Exception e) {
				Synchronizer.logger.error("Error while stopping FileAlterationMonitor", e);
			}
			monitor = null;
		}
	}

	@Override
	public void registerAll(Path dir) throws IOException {
		// nothing to do, since the FileAlterationMonitor will automatically monitor the whole directory structure
	}

	@Override
	public void deregister(Path dir) {
		// nothing to do, since the FileAlterationMonitor will automatically monitor the whole directory structure
	}
}
