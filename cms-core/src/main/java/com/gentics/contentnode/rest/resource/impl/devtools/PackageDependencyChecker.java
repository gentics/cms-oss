package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.devtools.dependency.PackageDependency;
import com.gentics.contentnode.rest.model.devtools.dependency.ReferenceDependency;
import com.gentics.contentnode.rest.resource.impl.devtools.resolver.AbstractDependencyResolver;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Service to lookup package dependencies and test them for consistency.
 */
public class PackageDependencyChecker {

	private static final NodeLogger LOGGER = NodeLogger.getNodeLogger(PackageDependencyChecker.class);
	private final PackageSynchronizer packageSynchronizer;
	private List<Class<? extends SynchronizableNodeObject>> dependencyClasses = Arrays.asList(
			Construct.class, ObjectTagDefinition.class, Template.class, Datasource.class);
	private String packageName;

	public PackageDependencyChecker(String packageName) {
		this.packageName = packageName;
		this.packageSynchronizer = Synchronizer.getPackage(packageName);
	}

	/**
	 * Utility method to filter the given dependency list to only contain missing entities
	 *
	 * @param dependencies the list that should be filtered
	 * @return the filtered dependency list
	 */
	public static List<PackageDependency> filterMissingDependencies(
			List<PackageDependency> dependencies) {

		List<PackageDependency> missingDependencies = removeEmptyDependencyList(dependencies).stream()
				.filter(dependency -> dependency.getReferenceDependencies().stream()
						.anyMatch(
								reference -> Boolean.FALSE.equals(reference.getIsInPackage()) ||
										Boolean.FALSE.equals(reference.getIsInOtherPackage())))
				.collect(Collectors.toList());

		missingDependencies.forEach(dependency -> {
			dependency.withReferenceDependencies(getDistinctReferenceList(dependency.getReferenceDependencies()));
		});

		return removeEmptyDependencyList(missingDependencies);
	}

	private static List<ReferenceDependency> getDistinctReferenceList(List<ReferenceDependency> references) {
		List<ReferenceDependency> distinctList = new ArrayList<>();

		references.forEach(reference -> {
			boolean alreadyInList = distinctList.stream()
					.noneMatch(item -> item.getGlobalId().equals(reference.getGlobalId()));
			if (alreadyInList) {
				distinctList.add(reference);
			}
		});
		return distinctList;
	}

	/**
	 * Utility method to clean the given dependency list from empty references
	 *
	 * @param dependencies the list that should be cleaned
	 * @return the cleaned dependency list
	 */
	private static List<PackageDependency> removeEmptyDependencyList(
			List<PackageDependency> dependencies) {
		return dependencies.stream().filter(
				packageObject -> packageObject.getReferenceDependencies() != null
						&& !packageObject.getReferenceDependencies().isEmpty()).collect(
				Collectors.toList());
	}

	/**
	 * Collects all dependencies and its sub dependencies
	 *
	 * @return the dependencies
	 * @throws NodeException
	 */
	public List<PackageDependency> collectDependencies() throws NodeException {
		LOGGER.info("Collecting dependencies for package " + packageName);
		try (Trx trx = ContentNodeHelper.trx()) {
			List<PackageDependency> dependencies = new ArrayList<>();
			for (Class<? extends SynchronizableNodeObject> dependencyClass : dependencyClasses) {
				AbstractDependencyResolver resolver = new AbstractDependencyResolver
						.Builder(dependencyClass)
						.withSynchronizer(packageSynchronizer)
						.build();

				dependencies.addAll(resolver.resolve());
			}

			return removeEmptyDependencyList(dependencies);
		}
	}

	/**
	 * Checks if the package has unmet dependencies
	 *
	 * @param dependencies dependency list to check
	 * @return true if all dependencies are synced to the filesystem
	 */
	public boolean isPackageComplete(List<PackageDependency> dependencies) {
		return filterMissingDependencies(dependencies)
				.isEmpty();
	}

	/**
	 * Gets the package name
	 *
	 * @return the package name
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * Sets the package name
	 *
	 * @param packageName the new package name
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * Get the list of dependency classes
	 * @return the list of dependency classes
	 */
	public List<Class<? extends SynchronizableNodeObject>> getDependencyClasses() {
		return dependencyClasses;
	}

	/**
	 * Override the list of classes that should be considered for the dependency check
	 * @param dependencyClasses the new list of dependency classes. Each dependency class must
	 *                          correspond to a resolver implementation
	 * @see AbstractDependencyResolver
	 */
	public void setDependencyClasses(
			List<Class<? extends SynchronizableNodeObject>> dependencyClasses) {
		this.dependencyClasses = dependencyClasses;
	}

}
