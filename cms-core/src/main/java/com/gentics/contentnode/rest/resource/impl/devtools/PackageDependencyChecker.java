package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PackageDependencyChecker {

  public final static List<Class<? extends SynchronizableNodeObject>> DEPENDENCY_CLASSES = Arrays.asList(
      Datasource.class, Construct.class, ObjectTagDefinition.class,
      Template.class, CrFragment.class, ContentRepository.class);

  public static NodeLogger logger = NodeLogger.getNodeLogger(PackageDependencyChecker.class);
  private final PackageSynchronizer packageSynchronizer;
  private String packageName;
  private boolean checked = false;

  public PackageDependencyChecker(String packageName) {
    this.packageName = packageName;
    this.packageSynchronizer = Synchronizer.getPackage(packageName);
  }

  public List<PackageDependency> collectDependencies() throws NodeException {
    List<PackageDependency> dependencies = new ArrayList<>();

    try (Trx trx = ContentNodeHelper.trx()) {
      for (Class<? extends SynchronizableNodeObject> dependencyClass : DEPENDENCY_CLASSES) {
        try {
          AbstractDependencyResolver resolver = AbstractDependencyResolver.getResolver(
              dependencyClass);

          dependencies.addAll(
                resolver.resolve(this.packageSynchronizer));

        } catch (NodeException e) {
          logger.error(e);
        }
      }
    }

    return dependencies;
  }

  public boolean performCheck() {
    // check collected dependencies

    this.checked = true;
    return false;
  }


  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

}