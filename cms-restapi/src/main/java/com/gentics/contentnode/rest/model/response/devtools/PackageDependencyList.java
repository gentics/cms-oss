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
   * Checks whether the package is complete.
   * A package is incomplete if any object is neither in this package nor in any other.
   * @return True if the package is complete, false otherwise.
   */
  public boolean checkCompleteness() {
    if(packageIsComplete != null) {
      return packageIsComplete;
    }

    List<PackageDependency> dependencies = this.getItems();
    if (dependencies.isEmpty()) {
      packageIsComplete = true;
      return true;
    }

    packageIsComplete = dependencies.stream()
        .anyMatch(dependency -> !dependency.getIsInPackage() && !dependency.getIsInOtherPackage());

    return packageIsComplete;
  }

}

