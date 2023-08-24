package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.bouncycastle.util.Pack;

/**
 * Service to collect and check packages concurrently
 */
public class ConcurrentPackageDependencyChecker {

  private static final ExecutorService executor = Executors.newFixedThreadPool(5);
  private static final NodeLogger LOGGER = NodeLogger.getNodeLogger(
      ConcurrentPackageDependencyChecker.class);
  private static List<Future<List<PackageDependency>>> dependencyCheckerTasks;

  /**
   * Creates and submits dependency checking task
   *
   * @param excludedPackage the package that has been already checked
   */
  public static void createDependencyCheckerTasks(
      String excludedPackage) {
    dependencyCheckerTasks = new ArrayList<>();
    Set<String> packages = Synchronizer.getPackages();
    // ignore already checked package
    packages.remove(excludedPackage);
    LOGGER.info(String.format("Searching %s packages for missing objects", packages.size()));

    for (String packageName : packages) {
      Callable<List<PackageDependency>> collectDependencyTask = () -> {
        PackageDependencyChecker dependencyChecker = new PackageDependencyChecker(packageName);
        return dependencyChecker.collectDependencies();
      };

      dependencyCheckerTasks.add(executor.submit(collectDependencyTask));
    }
  }

  /**
   * @param searchDependencyInOtherPackagesList the list of dependencies that should be looked for in other packages
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public static void checkAllPackageDependencies(
      List<PackageDependency> searchDependencyInOtherPackagesList)
      throws ExecutionException, InterruptedException {
    for (Future<List<PackageDependency>> task : dependencyCheckerTasks) {
      List<PackageDependency> collectedDependencies = task.get();

      boolean isInOtherPkg = checkOtherPackages(searchDependencyInOtherPackagesList,
          collectedDependencies);
      if (isInOtherPkg) {
        // stop looking for dependency
        dependencyCheckerTasks.forEach(t -> t.cancel(true));
        break;
      }
    }
  }

  /**
   * Check if missing dependency is in other package
   * @param searchDependencyInOtherPackagesList the list of dependencies that should be looked for in other packages
   * @param collectedDependencies package dependencies from other package
   * @return true if dependency was found in other package
   */
  private static boolean checkOtherPackages(
      List<PackageDependency> searchDependencyInOtherPackagesList,
      List<PackageDependency> collectedDependencies) {
    for (PackageDependency searchDependency : searchDependencyInOtherPackagesList) {
      boolean isInOtherPkg = false;
      for (PackageDependency collectedDependency : collectedDependencies) {

        // searching the missing dependency in other package dependency
        Optional<PackageDependency> missingDependency = collectedDependency.getReferencedDependencies().stream().filter(
            referencedDependency ->
              referencedDependency.getGlobalId().equals(searchDependency.getGlobalId()) //todo: compare ref -> top level
        ).findAny();

        searchDependency.setIsInOtherPackage(isInOtherPkg);
        if (isInOtherPkg) {
          return true;
        }
      }
    }
    return false;
  }

  private void searchReferences(PackageDependency dependency){

  }

}

