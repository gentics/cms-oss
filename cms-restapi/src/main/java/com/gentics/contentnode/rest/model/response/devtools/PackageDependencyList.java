package com.gentics.contentnode.rest.model.response.devtools;


import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import java.util.List;


/**
 * List of dependencies of a package containing consistency information
 */
public class PackageDependencyList extends AbstractListResponse<PackageDependency> {

  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = 7403254217672838794L;


  /**
   * The flag indicating whether the package is complete.
   */
  private Boolean packageIsComplete;

  /**
   * Checks whether the package is complete. A package is incomplete if any object is neither in
   * this package nor in any other.
   * @return True if the package is complete, false otherwise.
   */
  public boolean checkCompleteness() {
    packageIsComplete = false;

    List<PackageDependency> dependencies = this.getItems();
    if (dependencies.isEmpty()) {
      packageIsComplete = true;
      return true;
    }

    for (PackageDependency dependency : dependencies) {
      List<PackageDependency> references = dependency.getReferencedDependencies();
      if (references != null && !references.isEmpty()) {
        if (references.stream().anyMatch(r -> !isSane(r))) {
          packageIsComplete = false;
          return false;
        }
      }
    }
    packageIsComplete= true;

    return true;
  }

  /**
   * Check if the given dependency is consistent (i.e.: contained in a package)
   * @param dependency the dependency to check
   * @return true if all referenced objects are included
   */
  private boolean isSane(PackageDependency dependency) {
    return Boolean.TRUE.equals(dependency.getIsInPackage())
        || Boolean.TRUE.equals(dependency.getIsInOtherPackage());
  }

  /**
   * Gets the package completeness status.
   * @return the package completeness status
   */
  public Boolean getPackageIsComplete() {
    return packageIsComplete;
  }

  /**
   * Sets the package completeness status.
   * @param packageIsComplete the new package completeness status
   */
  public void setPackageIsComplete(Boolean packageIsComplete) {
    this.packageIsComplete = packageIsComplete;
  }

}

