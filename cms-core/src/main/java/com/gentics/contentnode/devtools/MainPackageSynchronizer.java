package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.PackageModel;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.model.devtools.Package;

/**
 * Package synchronization implementation for main packages
 */
public class MainPackageSynchronizer extends PackageSynchronizer {
	/**
	 * Name of the gentics_package.json file
	 */
	public final static String GENTICS_PACKAGE_JSON = "gentics_package.json";

	/**
	 * List containing invalid subpackage directory names
	 */
	public final static List<String> INVALID_SUBPACKAGE_NAMES = Arrays.asList(CONSTRUCTS_DIR, DATASOURCES_DIR, OBJECTPROPERTIES_DIR, TEMPLATES_DIR, FILES_DIR,
			CR_FRAGMENTS_DIR, CONTENTREPOSITORIES_DIR, HANDLEBARS_DIR);

	/**
	 * Lambda that generates the rest model for a package
	 */
	public final static Function<MainPackageSynchronizer, Package> TRANSFORM2REST = synchronizer -> {
		Package restModel = new Package(synchronizer.getName());
		restModel.setConstructs(synchronizer.getObjects(Construct.class, false).size());
		restModel.setDatasources(synchronizer.getObjects(Datasource.class, false).size());
		restModel.setObjectProperties(synchronizer.getObjects(ObjectTagDefinition.class, false).size());
		restModel.setTemplates(synchronizer.getObjects(Template.class, false).size());
		restModel.setCrFragments(synchronizer.getObjects(CrFragment.class, false).size());
		restModel.setContentRepositories(synchronizer.getObjects(ContentRepository.class, false).size());
		restModel.setDescription(synchronizer.getDescription());
		if (synchronizer.subPackageContainer != null) {
			Set<Package> subPackages = new HashSet<>();
			for (String name : synchronizer.subPackageContainer.getPackages()) {
				subPackages.add(SubPackageSynchronizer.TRANSFORM2REST.apply(synchronizer.subPackageContainer.getPackage(name)));
			}
			restModel.setSubPackages(subPackages);
		}
		return restModel;
	};

	/**
	 * Container for subpackages (null if no subpackages exist)
	 */
	protected PackageContainer<SubPackageSynchronizer> subPackageContainer;

	/**
	 * Create an instance with the given path
	 * @param packagePath package path
	 * @throws NodeException
	 */
	public MainPackageSynchronizer(Path packagePath) throws NodeException {
		this(packagePath, true);
	}

	/**
	 * Create an instance with the given path
	 * @param packagePath package path
	 * @param registerWatchers true to register watchers
	 * @throws NodeException
	 */
	public MainPackageSynchronizer(Path packagePath, boolean registerWatchers) throws NodeException {
		super(packagePath, registerWatchers);
		handlePackageFile(registerWatchers);
	}

	@Override
	public void handleChangeBuffered(Path path) throws NodeException {
		PackageContainer<SubPackageSynchronizer> container = subPackageContainer;
		if (path.getParent().equals(packagePath) && path.toFile().getName().equals(GENTICS_PACKAGE_JSON)) {
			handlePackageFile(true);
			return;
		} else if (container != null && path.startsWith(container.getRoot())) {
			SubPackageSynchronizer packageSynchronizer = container.getPackageSynchronizer(path);
			if (packageSynchronizer != null) {
				packageSynchronizer.handleChangeBuffered(path);
			}
		} else {
			super.handleChangeBuffered(path);
		}
	}

	@Override
	public void handleChangeImmediate(Path syncPath) throws NodeException {
		PackageContainer<SubPackageSynchronizer> container = subPackageContainer;
		if (container != null && syncPath.startsWith(container.getRoot())) {
			SubPackageSynchronizer packageSynchronizer = container.getPackageSynchronizer(syncPath);
			if (packageSynchronizer != null) {
				packageSynchronizer.handleChangeImmediate(syncPath);
			}
		} else {
			super.handleChangeImmediate(syncPath);
		}
	}

	@Override
	public <T extends SynchronizableNodeObject> List<PackageObject<T>> getObjects(Class<T> clazz, boolean addPackageName) throws NodeException {
		List<PackageObject<T>> mainObjects = super.getObjects(clazz, false);
		PackageContainer<SubPackageSynchronizer> container = subPackageContainer;
		if (container != null) {
			mainObjects = new ArrayList<>(mainObjects);
			mainObjects.addAll(container.getObjects(clazz));
		}
		return mainObjects;
	}

	@Override
	public void clearCache() {
		super.clearCache();
		if (subPackageContainer != null) {
			subPackageContainer.clearCache();
		}
	}

	@Override
	public void clearCache(Path syncPath) {
		PackageContainer<SubPackageSynchronizer> container = subPackageContainer;
		if (container != null && syncPath.startsWith(container.getRoot())) {
			SubPackageSynchronizer packageSynchronizer = container.getPackageSynchronizer(syncPath);
			if (packageSynchronizer != null) {
				packageSynchronizer.clearCache(syncPath);
			}
		} else {
			super.clearCache(syncPath);
		}
	}

	@Override
	public <T extends SynchronizableNodeObject> int syncAllFromFilesystem(Class<T> clazz) throws NodeException {
		int count = super.syncAllFromFilesystem(clazz);
		PackageContainer<SubPackageSynchronizer> tempContainer = subPackageContainer;
		if (tempContainer != null) {
			Set<String> subPackages = tempContainer.getPackages();
			for (String name : subPackages) {
				SubPackageSynchronizer synchronizer = tempContainer.getPackage(name);
				if (synchronizer != null) {
					count += synchronizer.syncAllFromFilesystem(clazz);
				}
			}
		}
		return count;
	}

	/**
	 * Get the subpackage container
	 * @return subpackage container or null
	 */
	public PackageContainer<SubPackageSynchronizer> getSubPackageContainer() {
		return subPackageContainer;
	}

	/**
	 * Parse the package file into the model (if a package file exists)
	 * @return model or null if no package file exists
	 * @throws NodeException when the package file exists but cannot be parsed
	 */
	protected PackageModel parsePackageFile() throws NodeException {
		File packageFile = new File(packagePath.toFile(), GENTICS_PACKAGE_JSON);
		if (!packageFile.exists() || !packageFile.isFile()) {
			return null;
		}
		try (InputStream in = new FileInputStream(packageFile)) {
			return mapper().readValue(in, PackageModel.class);
		} catch (IOException e) {
			throw new NodeException(String.format("Error while parsing %s", packageFile), e);
		}
	}

	/**
	 * Handle package file
	 * @param registerWatchers true to register watchers
	 * @throws NodeException
	 */
	protected void handlePackageFile(boolean registerWatchers) throws NodeException {
		PackageModel packageModel = parsePackageFile();
		if (packageModel != null) {
			String subpackages = packageModel.getSubpackages();

			// check validity of subpackages directory
			if (!ObjectTransformer.isEmpty(subpackages)) {
				try {
					File canonicalDir = new File(packagePath.toFile(), subpackages).getCanonicalFile();
					if (!subpackages.equalsIgnoreCase(canonicalDir.getName())) {
						Synchronizer.logger.error(String.format("%s is not a valid subpackages directory name", subpackages));
						subpackages = null;
					} else if (INVALID_SUBPACKAGE_NAMES.contains(subpackages)) {
						Synchronizer.logger.error(String.format("%s is not a valid subpackages directory name", subpackages));
						subpackages = null;
					}
				} catch (IOException e) {
					Synchronizer.logger.error(String.format("%s is not a valid subpackages directory name", subpackages));
					subpackages = null;
				}
			}

			if (!ObjectTransformer.isEmpty(subpackages)) {
				Path newSubpackagesRootPath = new File(packagePath.toFile(), subpackages).toPath();
				if (subPackageContainer == null || !subPackageContainer.getRoot().equals(newSubpackagesRootPath)) {
					if (subPackageContainer != null) {
						subPackageContainer.destroy();
					}

					subPackageContainer = new SubPackageContainer(newSubpackagesRootPath);
				}
			} else {
				if (subPackageContainer != null) {
					subPackageContainer.destroy();
				}

				// no sub packages
				subPackageContainer = null;
			}
		} else {
			if (subPackageContainer != null) {
				subPackageContainer.destroy();
			}

			// no sub packages
			subPackageContainer = null;
		}

		Synchronizer.callListeners(packageName, Event.HANDLE_PACKAGE_JSON);
	}
}
