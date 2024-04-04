package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.rest.util.MiscUtils.unwrap;
import static com.gentics.contentnode.rest.util.MiscUtils.wrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.factory.Trx;

/**
 * Container for packages
 */
public abstract class PackageContainer<T extends PackageSynchronizer> {
	/**
	 * PackageSynchronizer class
	 */
	private Class<T> clazz;

	/**
	 * Root path
	 */
	private Path root;

	/**
	 * Map of packages per path
	 */
	private Map<Path, T> packages = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Create an instance with given root path
	 * @param clazz PackageSynchronizer class
	 * @param root root path
	 * @throws NodeException
	 */
	protected PackageContainer(Class<T> clazz, Path root) throws NodeException {
		this.clazz = clazz;
		this.root = root;

		try {
			// register file watcher on the root path
			Synchronizer.registerAll(this.root);

			// initial scan for packages
			if (this.root.toFile().isDirectory()) {
				File[] files = this.root.toFile().listFiles(f -> {
					return f.isDirectory() && !f.getName().startsWith(".");
				});
				if (files == null) {
					Synchronizer.logger.error(String.format(
							"Unable to list directories in package root folder %s. Filesystem permissions need to be checked.",
							this.root.toFile()));
				} else {
					for (File packageDir : files) {
						String packageName = packageDir.getName();
						Synchronizer.logger.debug("Found package " + packageName);
						Synchronizer.addPackageSynchronizer(this, packageDir.toPath());
					}
				}
			}
		} catch (IOException e) {
			throw new NodeException(e);
		}

		Synchronizer.addContainer(this);
	}

	/**
	 * Destroy the container
	 */
	public void destroy() {
		Synchronizer.deregister(root);
		Synchronizer.removeContainer(root);
	}

	/**
	 * Synchronize the given object. This method will create new transactions for each package with the user, for which the sync is activated
	 * @param object
	 */
	public void synchronize(SynchronizableNodeObject object) {
		new ArrayList<>(packages.values()).forEach(synchronizer -> {
			try (Trx trx = new Trx(null, Synchronizer.getUserId()); LangTrx langTrx = new LangTrx("en", false)) {
				synchronizer.synchronize(object, false);
				trx.success();
			} catch (Exception e) {
				Synchronizer.logger.error("", e);
			}
		});
	}

	/**
	 * Remove the object from all packages
	 * @param object
	 */
	public  void remove(SynchronizableNodeObject object) {
		new ArrayList<>(packages.values()).forEach(synchronizer -> {
			try {
				synchronizer.remove(object, false);
			} catch (Exception e) {
				Synchronizer.logger.error("", e);
			}
		});
	}

	/**
	 * Get all package names
	 * @return set of package names
	 */
	public Set<String> getPackages() {
		return new ArrayList<>(packages.values()).stream().map(PackageSynchronizer::getName).collect(Collectors.toSet());
	}

	/**
	 * Get the package synchronizer with given name or null, if not found
	 * @param name package name
	 * @return package synchronizer or null
	 */
	public T getPackage(final String name) {
		return new ArrayList<>(packages.values()).stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
	}

	/**
	 * Get the package synchronizer for the given path or null
	 * @param path path
	 * @return package synchronizer instance or null
	 */
	public T getPackageSynchronizer(Path path) {
		return new HashSet<>(packages.entrySet()).stream().filter(entry -> path.startsWith(entry.getKey())).map(Map.Entry::getValue).findFirst().orElse(null);
	}

	/**
	 * Add a package synchronizer for the given package directory, if not done before
	 * @param packageDir package directory
	 * @param success optional success callback
	 * @throws NodeException
	 */
	public void addPackageSynchronizer(Path packageDir, Consumer<String> success) throws NodeException {
		String name = packageDir.toFile().getName();
		if (Synchronizer.allowedPackageName(name)) {
			unwrap(() -> packages.computeIfAbsent(packageDir, key -> {
				Synchronizer.logger.debug(String.format("New package %s", name));
				T packageSynchronizer = wrap(() -> {
					try {
						return clazz.getConstructor(Path.class).newInstance(packageDir);
					} catch (Exception e) {
						throw new NodeException(e);
					}
				});
				if (success != null) {
					success.accept(name);
				}
				return packageSynchronizer;
			}));
		}
	}

	/**
	 * Remove the package synchronizer for the given package directory
	 * @param packageDir package directory
	 * @param success optional success callback
	 */
	public void removePackageSynchronizer(Path packageDir, Consumer<String> success) {
		if (packages.containsKey(packageDir)) {
			String name = packageDir.toFile().getName();
			Synchronizer.logger.debug(String.format("Package %s removed", name));
			packages.remove(packageDir);
			if (success != null) {
				success.accept(name);
			}
		}
	}

	/**
	 * Clear the cache of contained objects
	 */
	public void clearCache() {
		new ArrayList<>(packages.values()).forEach(PackageSynchronizer::clearCache);
	}

	/**
	 * Clear the packages
	 */
	public void clear() {
		packages.clear();
	}

	/**
	 * Get the root path
	 * @return root path
	 */
	public Path getRoot() {
		return root;
	}

	/**
	 * Get all objects of given class from the packages
	 * @param clazz object class
	 * @return list of objects
	 * @throws NodeException
	 */
	public <U extends SynchronizableNodeObject> List<PackageObject<U>> getObjects(Class<U> clazz) throws NodeException {
		List<PackageObject<U>> objects = new ArrayList<>();
		for (T synchronizer : new ArrayList<>(packages.values())) {
			objects.addAll(synchronizer.getObjects(clazz, true));
		}

		return objects;
	}
}
