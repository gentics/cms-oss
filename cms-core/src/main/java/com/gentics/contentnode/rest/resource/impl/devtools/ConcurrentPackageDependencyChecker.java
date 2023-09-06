package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Service to collect and check packages concurrently
 */
public class ConcurrentPackageDependencyChecker {

	private static final NodeLogger LOGGER = NodeLogger.getNodeLogger(
			ConcurrentPackageDependencyChecker.class);
	private static final ExecutorService executor = Operator.getExecutor();
	private List<Future<List<PackageDependency>>> dependencyCheckerTasks;

	/**
	 * Check if missing dependency is in other package
	 *
	 * @param searchDependencyInOtherPackagesList the list of dependencies that should be looked for
	 *                                            in other packages
	 * @param collectedDependencies               package dependencies from other package
	 * @return true if dependency was found in other package
	 */
	private static void checkOtherPackages(
			List<PackageDependency> searchDependencyInOtherPackagesList,
			List<PackageDependency> collectedDependencies) {
		for (PackageDependency searchDependency : searchDependencyInOtherPackagesList) {
			boolean isInOtherPkg = false;

			for (PackageDependency collectedDependency : collectedDependencies) {
				// searching the missing dependency in other package dependency
				Optional<PackageDependency> missingDependency = findMissingDependency(searchDependency,
						collectedDependency);

				if (missingDependency.isPresent()) {
					PackageDependency foundMissingDependency = missingDependency.get();
					isInOtherPkg = (foundMissingDependency.getIsInPackage() == null
							|| foundMissingDependency.getIsInPackage()) &&
							(foundMissingDependency.getIsInOtherPackage() == null
									|| foundMissingDependency.getIsInOtherPackage());
				}

				searchDependency.setIsInOtherPackage(isInOtherPkg);
				if (isInOtherPkg) {
					break;
				}
			}
		}
	}

	private static Optional<PackageDependency> findMissingDependency(
			PackageDependency searchDependency,
			PackageDependency collectedDependency) {
		// top level comparison (i.e.: check not reference but dependency itself)
		if (collectedDependency.getGlobalId().equals(searchDependency.getGlobalId())) {
			return Optional.of(collectedDependency);
		}

		// check references
		if (collectedDependency.getReferencedDependencies() == null) {
			return Optional.empty();
		}

		return collectedDependency.getReferencedDependencies()
				.stream().filter(
						referencedDependency ->
								referencedDependency.getGlobalId().equals(searchDependency.getGlobalId())
				).findAny();
	}

	/**
	 * Creates and submits dependency checking task
	 *
	 * @param excludedPackage   the package that has been already checked
	 * @param dependencyClasses list of dependency classes that should b considered for the
	 *                          consistency check
	 */
	public void createDependencyCheckerTasks(
			String excludedPackage, List<Class<? extends SynchronizableNodeObject>> dependencyClasses) {
		dependencyCheckerTasks = new ArrayList<>();
		Set<String> packages = Synchronizer.getPackages();
		// ignore already checked package
		packages.remove(excludedPackage);
		LOGGER.info(String.format("Searching %s packages for missing objects", packages.size()));

		for (String packageName : packages) {
			Callable<List<PackageDependency>> collectDependencyTask = () -> {
				PackageDependencyChecker dependencyChecker = new PackageDependencyChecker(packageName);
				dependencyChecker.setDependencyClasses(dependencyClasses);
				return dependencyChecker.collectDependencies();
			};

			dependencyCheckerTasks.add(executor.submit(collectDependencyTask));
		}
	}

	/**
	 * @param searchDependencyInOtherPackagesList the list of dependencies that should be looked for
	 *                                            in other packages
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public void checkAllPackageDependencies(
			List<PackageDependency> searchDependencyInOtherPackagesList)
			throws ExecutionException, InterruptedException {
		for (Future<List<PackageDependency>> task : dependencyCheckerTasks) {
			List<PackageDependency> collectedDependencies = task.get();

			checkOtherPackages(searchDependencyInOtherPackagesList, collectedDependencies);
		}
	}

}

