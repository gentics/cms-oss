package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.devtools.dependency.ReferenceDependency;
import com.gentics.contentnode.rest.model.devtools.dependency.Type;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	private List<Future<Map<Class<?>, List<?>>>> dependencyCheckerTasks;
	private final List<Class<? extends SynchronizableNodeObject>> dependencyClasses = Arrays.asList(
			Construct.class, ObjectTagDefinition.class, Template.class, Datasource.class);


	/**
	 * Creates and submits dependency checking task
	 *
	 * @param excludedPackage   the package that has been already checked
	 */
	public void createDependencyCheckerTasks(String excludedPackage) {
		dependencyCheckerTasks = new ArrayList<>();
		Set<String> packages = Synchronizer.getPackages();
		// ignore already checked package
		packages.remove(excludedPackage);
		LOGGER.info(String.format("Searching %s packages for missing objects in '%s'", packages.size(),
				excludedPackage));

		for (String packageName : packages) {
			Callable<Map<Class<?>, List<?>>> collectPackageObjectTask = () -> {
				Map<Class<?>, List<?>> classPackageObjectMap = new HashMap<>();
				PackageSynchronizer synchronizer = Synchronizer.getPackage(packageName);

				try (Trx trx = ContentNodeHelper.trx()) {
					for (Class<? extends SynchronizableNodeObject> dependencyClass : dependencyClasses) {
						List<?> packageObjects = synchronizer.getObjects(dependencyClass);
						classPackageObjectMap.put(dependencyClass, packageObjects);
					}
				}

				return classPackageObjectMap;
			};

			dependencyCheckerTasks.add(executor.submit(collectPackageObjectTask));
		}
	}

	/**
	 * @param searchDependencyInOtherPackagesList the list of dependencies that should be looked for
	 *                                            in other packages
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public void checkAllPackageDependencies(
			List<ReferenceDependency> searchDependencyInOtherPackagesList)
			throws ExecutionException, InterruptedException, NodeException {
		for (Future<Map<Class<?>, List<?>>> task : dependencyCheckerTasks) {
			Map<Class<?>, List<?>> collectedPackageObjects = task.get();
			checkOtherPackages(searchDependencyInOtherPackagesList, collectedPackageObjects);
		}
	}

	/**
	 * Check if missing dependency is in other package
	 *
	 * @param searchDependencyInOtherPackagesList the list of dependencies that should be looked for
	 *                                            in other packages
	 * @param collectedDependencies               package dependencies from other package
	 * @return true if dependency was found in other package
	 */
	private static void checkOtherPackages(
			List<ReferenceDependency> searchDependencyInOtherPackagesList,
			Map<Class<?>, List<?>> collectedDependencies) throws NodeException {
		for (ReferenceDependency searchReference : searchDependencyInOtherPackagesList) {
			boolean isInOtherPkg = false;

			for (Entry<Class<?>, List<?>> entry : collectedDependencies.entrySet()) {
				Class<?> clazz = entry.getKey();
				if (getImplementationClassForReference(searchReference.getDependencyType()) != clazz) {
					continue;
				}

				List<PackageObject<? extends SynchronizableNodeObject>> packageObjects =
						(List<PackageObject<? extends SynchronizableNodeObject>>) entry.getValue();

				isInOtherPkg = packageObjects.stream()
						.anyMatch(packageObject -> packageObject.getGlobalId().toString().equals(
								searchReference.getGlobalId()));

				if (isInOtherPkg) {
					LOGGER.info(String.format("Found missing dependency for '%s' with id '%s'", clazz,
							searchReference.getGlobalId()));
					break;
				}
			}

			searchReference.withIsInOtherPackage(isInOtherPkg);
		}
	}

	private static Class<? extends SynchronizableNodeObject> getImplementationClassForReference(Type type)
			throws NodeException {
		if (type == Type.CONSTRUCT) {
			return Construct.class;
		} else if (type == Type.TEMPLATE) {
			return Template.class;
		} else if (type == Type.OBJECT_TAG_DEFINITION) {
			return ObjectTagDefinition.class;
		} else if (type == Type.DATASOURCE) {
			return Datasource.class;
		}
		throw new NodeException("No implementation class found for type " + type);
	}

}
