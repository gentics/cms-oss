package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.rest.model.devtools.Package;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
import java.util.ArrayList;
import java.util.List;


public class PackageDependencyChecker {

  private String packageName;

  private boolean checked = false;

  public PackageDependencyChecker(String packageName) {
    this.packageName = packageName;
  }


  public List<PackageDependency> collectDependencies() {
    List<PackageDependency> dependencies = new ArrayList<>();

    PackageDependency packageDependency = new PackageDependency();
    packageDependency.setDependencyTyp(Type.CONSTRUCT);
    packageDependency.setName("Some construct");

    return dependencies;
  }

  public boolean performCheck() {
    // check collected dependencies

    this.checked = true;
    return false;
  }


  //check
  public <T extends SynchronizableNodeObject> boolean isInPackage(PackageObject<T>  packageObject) {
    /*
    if (packageSynchronizer.getObjects(Construct.class).contains(new PackageObject<>(construct))) {

    }
    */

    return false;
  }


  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

}
