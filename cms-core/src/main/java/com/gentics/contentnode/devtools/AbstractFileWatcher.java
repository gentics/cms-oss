package com.gentics.contentnode.devtools;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Abstract implementation of {@link IFileWatcher}
 */
public abstract class AbstractFileWatcher implements IFileWatcher {
	/**
	 * Registered containers
	 */
	protected Map<Path, PackageContainer<?>> containers = new HashMap<>();

	@Override
	public void addContainer(PackageContainer<?> container, Consumer<String> success) {
		if (containers.containsKey(container.getRoot())) {
			removeContainer(container.getRoot(), null);
		}
		containers.put(container.getRoot(), container);
		Synchronizer.logger.debug(String.format("Added package container to %s", container.getRoot()));
		if (success != null) {
			success.accept(container.getRoot().toFile().getName());
		}
	}

	@Override
	public void removeContainer(Path root, Consumer<String> success) {
		if (containers.remove(root) != null) {
			Synchronizer.logger.debug(String.format("Removed package container from %s", root));
			if (success != null) {
				success.accept(root.toFile().getName());
			}
		}
	}

	@Override
	public PackageContainer<?> getContainer(Path path) {
		return containers.get(path);
	}
}
