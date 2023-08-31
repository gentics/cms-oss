package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
import com.gentics.contentnode.rest.resource.impl.devtools.resolver.ConstructResolver;
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

/**
 * Service to collect and check packages concurrently
 */
public class ConcurrentPackageDependencyChecker {

  private static final ExecutorService executor = Executors.newFixedThreadPool(5);
  private static final NodeLogger LOGGER = NodeLogger.getNodeLogger(
      ConcurrentPackageDependencyChecker.class);
  private static List<Future<List<PackageDependency>>> dependencyCheckerTasks;

  private ConcurrentPackageDependencyChecker() {}

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
   * @param searchDependencyInOtherPackagesList the list of dependencies that should be looked for
   *                                            in other packages
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
   *
   * @param searchDependencyInOtherPackagesList the list of dependencies that should be looked for
   *                                            in other packages
   * @param collectedDependencies               package dependencies from other package
   * @return true if dependency was found in other package
   */
  private static boolean checkOtherPackages(
      List<PackageDependency> searchDependencyInOtherPackagesList,
      List<PackageDependency> collectedDependencies) {
    for (PackageDependency searchDependency : searchDependencyInOtherPackagesList) {
      boolean isInOtherPkg = false;
      for (PackageDependency collectedDependency : collectedDependencies) {

        // searching the missing dependency in other package dependency
        Optional<PackageDependency> missingDependency = findMissingDependency(searchDependency, collectedDependency);

        isInOtherPkg = missingDependency.isPresent();
        searchDependency.setIsInOtherPackage(isInOtherPkg);
        if (isInOtherPkg) {
          return true;
        }
      }
    }
    return false;
  }

  private static Optional<PackageDependency> findMissingDependency(
      PackageDependency searchDependency,
      PackageDependency collectedDependency) {
    if(searchDependency.getDependencyType() == Type.DATASOURCE) {
      return findMissingDatasource(searchDependency, collectedDependency);
    }

    // top level comparison (i.e.: check not reference but dependency itself)
    if(collectedDependency.getGlobalId().equals(searchDependency.getGlobalId())){
      return Optional.of(collectedDependency);
    }

    // check references
    if(collectedDependency.getReferencedDependencies() == null) {
      return Optional.empty();
    }

    // todo: check isInPackage additionally: test in GPU-993
    return collectedDependency.getReferencedDependencies()
        .stream().filter(
            referencedDependency ->
                referencedDependency.getGlobalId().equals(searchDependency.getGlobalId())
        ).findAny();
  }

  private static Optional<PackageDependency> findMissingDatasource(
      PackageDependency searchDependency,
      PackageDependency collectedDependency) {
      try (Trx trx = ContentNodeHelper.trx()) {
        int infoInt = resolvePartInfoInt(searchDependency.getGlobalId());
        String datasourceUuid = ConstructResolver.resolveUuid(infoInt);

        if(collectedDependency.getGlobalId().equals(datasourceUuid)){
          return Optional.of(collectedDependency);
        }
      } catch (NodeException e) {
        return Optional.empty();
      }

    return Optional.empty();
  }


  private static int resolvePartInfoInt(String partUuid) throws NodeException {
    return DBUtils.select("SELECT `info_int` FROM `part` WHERE `uuid` = ?",
        ps -> ps.setString(1, partUuid), resultSet -> {
          if (resultSet.next()) {
            return resultSet.getInt("info_int");
          }
          return -1;
        });
  }

}

