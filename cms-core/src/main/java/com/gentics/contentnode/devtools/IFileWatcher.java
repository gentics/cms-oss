package com.gentics.contentnode.devtools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface for filewatcher implementations
 */
public interface IFileWatcher {
	/**
	 * Initialize the implementation
	 * @param properties properties map
	 * @throws NodeException
	 */
	void init(Map<String, Object> properties) throws NodeException;

	/**
	 * Start watching for file changes under the given root path
	 * @param rootPath root path
	 * @throws NodeException if watching files could not be started
	 */
	void start(Path rootPath) throws NodeException;

	/**
	 * Stop watching for file changes
	 */
	void stop();

	/**
	 * Register everything under the given (new) directory
	 * @param dir directory
	 * @throws IOException in case of errors
	 */
	void registerAll(Path dir) throws IOException;

	/**
	 * Deregister the given directory (and everything unterneath)
	 * @param dir directory
	 */
	void deregister(Path dir);

	/**
	 * Add a package container
	 * @param container package container
	 * @param success success callback
	 */
	void addContainer(PackageContainer<?> container, Consumer<String> success);

	/**
	 * Remove a package container for the given root path
	 * @param path root path
	 * @param success success callback
	 */
	void removeContainer(Path path, Consumer<String> success);

	/**
	 * Get the package container with given root path, or null if not found
	 * @param path container root path
	 * @return package container or null
	 */
	PackageContainer<?> getContainer(Path path);
}
