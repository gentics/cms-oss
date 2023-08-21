package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.resource.impl.devtools.resolver.AbstractDependencyResolver;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class PackageDependencyChecker {

  private static final List<Class<? extends SynchronizableNodeObject>> DEPENDENCY_CLASSES = Arrays.asList(
      Construct.class, ObjectTagDefinition.class, Template.class);

  private static final NodeLogger LOGGER = NodeLogger.getNodeLogger(PackageDependencyChecker.class);
  private final PackageSynchronizer packageSynchronizer;
  private String packageName;

  public PackageDependencyChecker(String packageName) {
    this.packageName = packageName;
    this.packageSynchronizer = Synchronizer.getPackage(packageName);
  }

  public static List<PackageDependency> filterMissingDependencies(
      List<PackageDependency> dependencies) {
    removeEmptyDependencyList(dependencies).forEach(
        dependency -> dependency.getReferencedDependencies()
            .removeIf(PackageDependency::getIsInPackage));

    return removeEmptyDependencyList(dependencies);
  }

  private static List<PackageDependency> removeEmptyDependencyList(
      List<PackageDependency> dependencies) {
    return dependencies.stream().filter(
        packageObject -> packageObject.getReferencedDependencies() != null
            && !packageObject.getReferencedDependencies().isEmpty()).collect(
        Collectors.toList());
  }

  public List<PackageDependency> collectDependencies() throws NodeException {
    LOGGER.info("Collecting dependencies for package " + packageName);
    try (Trx trx = ContentNodeHelper.trx()) {
      List<PackageDependency> dependencies = new ArrayList<>();
      for (Class<? extends SynchronizableNodeObject> dependencyClass : DEPENDENCY_CLASSES) {
        AbstractDependencyResolver resolver = new AbstractDependencyResolver
            .Builder(dependencyClass)
            .withSynchronizer(packageSynchronizer)
            .build();

        dependencies.addAll(resolver.resolve());
      }
      return dependencies;
    }
  }

  /**
   * Checks if the package has unmet dependencies
   * @param dependencies dependency list to check
   * @return true if all dependencies are synced to the filesystem
   */
  public boolean isPackageComplete(List<PackageDependency> dependencies) {
    return filterMissingDependencies(dependencies)
        .isEmpty();
  }


  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

}
