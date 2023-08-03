package com.gentics.contentnode.rest.model.response.devtools;


import com.gentics.contentnode.rest.model.response.AbstractListResponse;


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
  private boolean packageIsComplete;

  /**
   * Gets whether the package is complete.
   * @return True if the package is complete, false otherwise.
   */
  public boolean getPackageIsComplete() {
    return packageIsComplete;
  }

  /**
   * Sets whether the package is complete.
   * @param packageIsComplete True if the package is complete, false otherwise.
   */
  public void setPackageIsComplete(boolean packageIsComplete) {
    this.packageIsComplete = packageIsComplete;
  }


}
